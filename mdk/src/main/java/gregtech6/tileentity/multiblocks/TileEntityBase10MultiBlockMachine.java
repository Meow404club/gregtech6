package gregtech6.tileentity.multiblocks;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.items.IItemHandler;

import gregtech6.fluid.FluidTankGT;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;
import gregtech6.tileentity.GTItemStackHandler;

/**
 * 1.20.1 port of gregapi/tileentity/multiblocks/TileEntityBase10MultiBlockMachine.java —
 * the machine layer between the structure base and the concrete multiblock machines
 * (task p6-cokeoven-processing; the first consumer is TileEntityCokeOven). Upstream this
 * class extends MultiTileEntityBasicMachine and only adds structure defaults; the port
 * carries the BasicMachine business directly because the upstream 1036-line single-block
 * machine is not a separate ported layer (the Oven already proved the trimmed translation,
 * TileEntityOven).
 *
 * <p><b>Trimmed-in business face</b> (upstream MultiTileEntityBasicMachine line refs):
 * <ul>
 * <li>TU self-generation: {@code mEnergy++} per server tick while not stopped (:455) — the
 *     Coke Oven registration is TU/NO_CONSTANT_POWER (Loader_MultiTileEntities.java:1193),
 *     no energy net, no redstone stop (upstream has none); doWork drains mInputMax :791,
 *     so the net progress rate is 1 energy/tick → 3600 t for the standard recipe;</li>
 * <li>{@link #doWork} :780-793 verbatim (energy gate + {@link #doActive}/
 *     {@link #doInactive} + the drain + the mIgnited decrement :792);</li>
 * <li>{@link #doActive} :795-887 trimmed: the recipe re-check (:800), progress += energy
 *     (:813 — the progress unit IS an energy unit), item placement i % outCount (:816),
 *     fluid placement merge-then-empty-tank (:817-835), the keep-alive mIgnited = 40
 *     (:816/:822/:831/:851) and the carryover (:843-851); the alternating-energy gate
 *     (:815) folds away (TU is not alternating) and the neighbor item auto-push block
 *     (:853-858/:867-884) folds away with the auto-IO surface;</li>
 * <li>{@link #doInactive} :889-900 — CONSTANT_ENERGY reuse (the oven static semantics);
 *     the Coke Oven is registered NO_CONSTANT_POWER (:1193) so {@link #mNoConstantEnergy}
 *     defaults true and the :894 progress reset is skipped;</li>
 * <li>{@link #checkRecipe} :683-778 trimmed: no auto-IO (:687 cut), the minimal-input
 *     gates (:708-710), findRecipe with size = mInputMax (:712, the TU branch), the
 *     ignition gate (:737 {@code aApplyRecipe = (!mRequiresIgnition || mIgnited > 0 ||
 *     mActive)}), the two-stage consume (:738), the parallel loop (:742-745 — the TU
 *     branch skips the energy bind; the long-overload consume is a boolean two-stage
 *     loop, the P4 Recipe shell is untouched), the energy math :761-774 verbatim (the
 *     :773 overclock loop stays — with mInputMin = 1 it is a no-op, the zero-overclock
 *     fidelity);</li>
 * <li>{@link #canOutput} :620-668: the output-slot blockage (equal item + capacity,
 *     mNeedsEmptyOutput) and the fluid-tank capacity branch (:650-666) verbatim —
 *     {@code FluidsGT.VOID_OVERFLOW} is carried as an empty set (no fluid this port
 *     produces is void-listed upstream; the FluidsGT list itself is the pool);</li>
 * <li>{@link #doOutputFluids} :994-996 per ADR ruling ①: push each output tank's content
 *     through the target handler — drain(SIMULATE) → target fill(EXECUTE) → drain(EXECUTE)
 *     by the accepted amount (fill-then-deduct, 0 accepted = source untouched), the
 *     upstream SIDE_TOP push maps onto the target capability queried at Direction.UP;</li>
 * <li>the ignition entry {@link #ignite()} = the upstream TOOL_igniter branch :373-379
 *     (mRequiresIgnition → mIgnited = 40); mIgnited keeps the upstream SINGLE field with
 *     its double semantics: the ignition gate :737 AND the post-action re-check window
 *     (:816/:851 set, :792 decrement) — the oven's re-check window is the same mechanism
 *     with mRequiresIgnition = false (the mIgnited_ruling; TileEntityOven stays
 *     untouched);</li>
 * <li>{@link #getFluidOutputTarget(Fluid)} is abstract (upstream :145
 *     {@code getFluidOutputTarget(byte, Fluid)} — the byte side folds away because the
 *     ruling pins the UP face); the item/fluid input+output targets (:98-100 in the Coke
 *     Oven subclass) fold away entirely with the cut auto-IO surface;</li>
 * <li>the inventory is 11 slots = 1 input + 9 outputs + 1 special (upstream
 *     getDefaultInventory :523-531 minus the fluid display slots); the item capability is
 *     exposed through a gating wrapper: insert only the input slot (canInsertItem2
 *     :549-554 trimmed — the mMode/containsInput legs are the pool), extract only the
 *     output slots (canExtractItem2 :556-559).</li>
 * </ul>
 *
 * <p>Fields: mEnergy/mInputMin = 1/mInput = 1/mInputMax = 16 (the :1193 NBT_INPUT triple),
 * mParallel = 16 (NBT_PARALLEL :1193), mProgress/mMaxProgress, the running/active/stopped
 * trio, mRequiresIgnition = true + mIgnited, mLastRecipe/mCurrentRecipe, the pending
 * mOutputItems/mOutputFluids and the single output tank (upstream FluidTankGT default
 * constructor capacity = Long.MAX_VALUE, upstream FluidTankGT.java:49).
 */
public abstract class TileEntityBase10MultiBlockMachine extends TileEntityBase10MultiBlockBase {

	// checkRecipe result codes (upstream :672-675 verbatim).
	public static final int DID_NOT_FIND_RECIPE = 0;
	public static final int FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS = 1;
	public static final int FOUND_AND_SUCCESSFULLY_USED_RECIPE = 2;
	public static final int FOUND_AND_COULD_HAVE_USED_RECIPE = 3;

	/** GT_API.java:510 CONSTANT_ENERGY default (the oven precedent). */
	public static final boolean CONSTANT_ENERGY = true;

	/** The FluidsGT.VOID_OVERFLOW port seam (upstream :659 read) — no ported fluid is void-listed; the list itself is the pool. */
	public static final java.util.Set<String> FLUIDS_VOID_OVERFLOW = java.util.Set.of();

	// NBT keys — plain in-repo form (the oven/Base10 precedent: upstream "gt.*" keys carry the same names).
	public static final String NBT_INVENTORY = "inventory";
	public static final String NBT_ENERGY = "energy";
	public static final String NBT_MINENERGY = "minenergy";
	public static final String NBT_PROGRESS = "progress";
	public static final String NBT_MAXPROGRESS = "maxprogress";
	public static final String NBT_STOPPED = "stopped";
	public static final String NBT_IGNITED = "ignited";
	public static final String NBT_ACTIVE = "active";
	public static final String NBT_RUNNING = "running";
	public static final String NBT_OUTPUT_ITEMS = "output_items";
	public static final String NBT_OUTPUT_FLUIDS = "output_fluids";
	public static final String NBT_OUTPUT_TANK = "output_tank";

	/** Slot count — 1 input + 9 outputs + 1 special (no fluid display slots, the card ruling). */
	public static final int INVENTORY_SIZE = 11;

	/** Content slot indices (upstream RecipeMap order :81: inputs, outputs, special — displays cut). */
	public static final int SLOT_INPUT = 0, SLOT_SPECIAL = 10;

	// fields (:92-109 trimmed set, the :1193 registration values)
	public long mEnergy = 0, mInputMin = 1, mInput = 1, mInputMax = 16, mMinEnergy = 0;
	public long mProgress = 0, mMaxProgress = 0;
	public long mParallel = 16;
	public boolean mSuccessful = false, mActive = false, mRunning = false;
	public boolean mStopped = false, mCouldUseRecipe = false, mInventoryChanged = false;
	/** Upstream :1193 NBT_NO_CONSTANT_POWER = T for the Coke Oven — the :894 reset is skipped. */
	public boolean mNoConstantEnergy = true;
	/** The output-blockage counter (upstream :98, the canOutput diagnostic; the :867-884 reader is cut with auto-IO). */
	public long mOutputBlocked = 0;

	/** Upstream :93 mIgnited — the single field with BOTH semantics (see class doc). */
	public byte mIgnited = 0;
	/** Upstream :1193 NBT_NEEDS_IGNITION = T. */
	public boolean mRequiresIgnition = true;

	/** Upstream :100. */
	public Recipe mLastRecipe = null, mCurrentRecipe = null;
	/** Upstream :102 — the pending outputs of the current process. */
	public ItemStack[] mOutputItems = new ItemStack[0];
	public FluidStack[] mOutputFluids = new FluidStack[0];
	/** The single output tank — the upstream default-constructor capacity Long.MAX_VALUE (upstream FluidTankGT.java:49). */
	public final FluidTankGT[] mTanksOutput = {new FluidTankGT()};

	/** Upstream :107 default RM.CokeOven; resolved lazily because the map registers at mod construct. */
	public RecipeMap mRecipes = null;

	protected boolean oActive = false, oRunning = false;

	/** The inventory store (also bound into the root capability slot — see the gated exposure below). */
	protected GTItemStackHandler mInventory;

	/** The capability wrapper LazyOptional (built in the constructor, invalidated with the root's own). */
	private final LazyOptional<IItemHandler> mGatedCap;

	protected TileEntityBase10MultiBlockMachine(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		// the 10Base constructor already enables ticking (the structure poll + TU generation are tick-driven)
		super(aType, aPos, aState);
		mInventory = new GTItemStackHandler(INVENTORY_SIZE, this::onInventoryChanged);
		setInventory(mInventory); // root binding: marks dirty through onContentsChanged
		mGatedCap = LazyOptional.of(() -> new GatedItemHandler());
	}

	/** Lazy RM.CokeOven resolution (the oven :204-211 precedent; upstream :525 resolves via NBT_RECIPEMAP). */
	public RecipeMap recipes() {
		RecipeMap tMap = mRecipes;
		if (tMap == null) {
			tMap = GT6RecipeMaps.COKE_OVEN;
			mRecipes = tMap;
		}
		return tMap;
	}

	// ---------------------------------------------------------------------------
	// tick chain (:443-468 trimmed)
	// ---------------------------------------------------------------------------

	@Override
	public void onTickFirst(boolean aIsServerSide) {
		super.onTickFirst(aIsServerSide); // the 10Base structure check (upstream onTickFirst2 :444 super call)
		if (aIsServerSide) {
			// :446 — the recipe probe before the first doWork (checkStructure(true) is the
			// cached verdict at this point; the base already forced the full check above)
			if (checkStructure(true) && !mActive) checkRecipe(false, mRunning || mStopped);
		}
	}

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		super.onTick(aTimer, aIsServerSide); // the 10Base 600-tick structure poll (:121-124, includes :463)
		if (aIsServerSide) {
			// :454-455 — TU self-generation (the mChargeRequirement branch :456 is cut with charging)
			if (!mStopped) mEnergy++;
			// :459 — fluid auto-output (the mDisabledFluidOutput/SIDES_VALID gate folds away with the flags)
			doOutputFluids();
			// :461
			doWork(aTimer);
			// :465-466 fluid display slots cut
		}
	}

	@Override
	public boolean onTickCheck(long aTimer) {
		// :471-473 verbatim (the visual-data change pair)
		return mActive != oActive || mRunning != oRunning || super.onTickCheck(aTimer);
	}

	@Override
	public void onTickResetChecks(long aTimer, boolean aIsServerSide) {
		super.onTickResetChecks(aTimer, aIsServerSide);
		// :476-480 + 05Inventories.java:86-89 (mInventoryChanged)
		oRunning = mRunning;
		oActive = mActive;
		mInventoryChanged = false;
	}

	/** Content-change hook bound into the GTItemStackHandler (upstream updateInventory 05Inventories.java:103). */
	protected void onInventoryChanged() {
		setChanged();
		mInventoryChanged = true;
	}

	// ---------------------------------------------------------------------------
	// work (:780-900)
	// ---------------------------------------------------------------------------

	/** Upstream :780-793 verbatim (checkStructure is the real multiblock template). */
	public void doWork(long aTimer) {
		if (mEnergy >= mInputMin && mEnergy >= mMinEnergy && checkStructure(false)) {
			mActive = doActive(aTimer, Math.min(mInputMax, mEnergy));
			mRunning = true;
		} else {
			if (aTimer > 40) {
				mActive = doInactive(aTimer);
				mRunning = false;
			}
			mSuccessful = false;
		}
		mEnergy -= mInputMax; // :791
		if (mEnergy < 0) mEnergy = 0;
		if (mIgnited > 0) mIgnited--; // :792
	}

	/** Upstream :795-887 — progress = energy units (:813), output wrap i % outCount (:816), carryover (:843-851). */
	public boolean doActive(long aTimer, long aEnergy) {
		boolean rActive = false;

		if (mMaxProgress <= 0) {
			// :798-805 verbatim — mIgnited is the ignition gate AND the post-action re-check window
			if ((mIgnited > 0 || mInventoryChanged || !mRunning || aTimer % 1200 == 5) && checkRecipe(!mStopped, true) == FOUND_AND_SUCCESSFULLY_USED_RECIPE) {
				onProcessStarted();
			} else {
				mProgress = 0;
			}
		}

		mSuccessful = false; // :807

		if (mMaxProgress > 0) {
			rActive = true; // :810 (the mSpecialIsStartEnergy half is cut with special-start-energy)
			if (mProgress <= mMaxProgress) {
				mProgress += aEnergy; // :813 — the progress unit IS an energy unit
			}
			// :815 — the alternating-energy half (mStateOld && !mStateNew) folds away; TU is not alternating
			if (mProgress >= mMaxProgress) {
				// :816 — outputs wrap around the output slot range: i % mOutputItemsCount
				for (int i = 0; i < mOutputItems.length; i++) if (mOutputItems[i] != null && addStackToSlot(recipes().mInputItemsCount + (i % recipes().mOutputItemsCount), mOutputItems[i])) {
					mSuccessful = true;
					mIgnited = 40; // :816
					mOutputItems[i] = null;
					continue;
				}
				// :817-835 — fluid placement: merge into a tank holding the fluid, then an empty tank
				for (int i = 0; i < mOutputFluids.length; i++) if (mOutputFluids[i] != null) for (FluidTankGT tTank : mTanksOutput) {
					if (tTank.contains(mOutputFluids[i])) {
						tTank.add(mOutputFluids[i].getAmount()); // :820
						mSuccessful = true;
						mIgnited = 40; // :822
						mOutputFluids[i] = null;
						break;
					}
				}
				for (int i = 0; i < mOutputFluids.length; i++) if (mOutputFluids[i] != null) for (FluidTankGT tTank : mTanksOutput) {
					if (tTank.isEmpty()) {
						tTank.add(mOutputFluids[i].getAmount(), mOutputFluids[i]); // :829 — the empty tank adopts a copy (FluidTankGT :170-179)
						mSuccessful = true;
						mIgnited = 40; // :831
						mOutputFluids[i] = null;
						break;
					}
				}

				if (containsSomething(mOutputItems) || containsSomething(mOutputFluids)) {
					// :837-841 — outputs blocked: park at max progress and retry the placement next tick
					mMinEnergy = 0;
					mProgress = mMaxProgress;
				} else {
					// :843-861 — all outputs placed: carry the leftover energy into the next process
					mProgress -= mMaxProgress;
					mMinEnergy = 0;
					mMaxProgress = 0;
					mOutputItems = new ItemStack[0]; // :848 ZL_IS
					mOutputFluids = new FluidStack[0]; // :849 ZL_FS
					mSuccessful = true;
					mIgnited = 40; // :851
					// :853-858 adjacent-inventory notify cut with the auto-IO surface
					onProcessFinished();
				}
			}
		}

		// :867-884 neighbor auto-push cut (getItemOutputTarget is null for the Coke Oven; canOutput keeps the blockage)
		return rActive;
	}

	/** Upstream :889-900 — sound interrupt and the item push cut; CONSTANT_ENERGY reset verbatim. */
	public boolean doInactive(long aTimer) {
		if (CONSTANT_ENERGY && !mNoConstantEnergy) mProgress = 0; // :894 (NO_CONSTANT_POWER = T for the Coke Oven :1193)
		if (mRunning || mIgnited > 0 || mInventoryChanged || aTimer % 1200 == 5) { // :895
			if (!checkStructure(false)) checkStructure(true); // :896
			checkRecipe(false, true); // :897
		}
		return false;
	}

	// ---------------------------------------------------------------------------
	// recipe check (:683-778 trimmed)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream :683-778 — the auto-IO (:687), the output-tank fallback (:717-718) and the
	 * adjacent-notify (:748-753) are cut. {@code aApplyRecipe=false} probes, {@code true}
	 * consumes (the two-stage isRecipeInputEqual contract).
	 */
	public int checkRecipe(boolean aApplyRecipe, boolean aUseAutoIO) {
		mCouldUseRecipe = false; // :684
		RecipeMap tRecipes = recipes();
		if (tRecipes == null) return DID_NOT_FIND_RECIPE; // :685

		int tInputItemsCount = 0; // :689 (the map carries no fluid inputs — mInputFluidCount = 0)
		ItemStack[] tInputs = new ItemStack[tRecipes.mInputItemsCount];
		for (int i = 0; i < tRecipes.mInputItemsCount; i++) {
			tInputs[i] = slot(i);
			if (tInputs[i] != null && !tInputs[i].isEmpty()) tInputItemsCount++;
		}
		// :696-706 fluid auto-input and tank counting cut (mMinimalInputFluids = 0)

		if (tInputItemsCount                     < tRecipes.mMinimalInputItems ) return DID_NOT_FIND_RECIPE; // :708
		if (tInputItemsCount                     < tRecipes.mMinimalInputs     ) return DID_NOT_FIND_RECIPE; // :710

		// :712 — mInputMax is the voltage (the TU branch of the RF ternary); the special slot rides along.
		Recipe tRecipe = tRecipes.findRecipe(mLastRecipe, mInputMax, slot(SLOT_SPECIAL), null, tInputs);
		if (tRecipe == null) return DID_NOT_FIND_RECIPE; // :719 shape (the mCanUseOutputTanks fallback :717-718 is the pool)

		if (tRecipe.mCanBeBuffered) mLastRecipe = tRecipe; // :734
		int tMaxProcessCount = canOutput(tRecipe); // :735
		if (tMaxProcessCount <= 0) return FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS; // :736

		// :737 — the ignition gate, verbatim
		if (aApplyRecipe) aApplyRecipe = !mRequiresIgnition || mIgnited > 0 || mActive;
		if (!tRecipe.isRecipeInputEqual(aApplyRecipe, false, null, tInputs)) return FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS; // :738
		mCouldUseRecipe = true; // :739
		if (!aApplyRecipe) return FOUND_AND_COULD_HAVE_USED_RECIPE; // :740

		if (tMaxProcessCount > 1) {
			// :742-745 — the TU branch skips the energy bind (:743 guard mEnergyTypeAccepted != TU).
			// The long-overload consume (1 + isRecipeInputEqual(n-1)) becomes the boolean
			// two-stage loop — the P4 Recipe shell carries no long form and stays untouched.
			int tExtra = 0;
			while (tExtra < tMaxProcessCount - 1 && tRecipe.isRecipeInputEqual(true, false, null, tInputs)) tExtra++;
			tMaxProcessCount = 1 + tExtra;
		}

		// :748-755 adjacent-inventory notify and mSpecialIsStartEnergy cut

		mCurrentRecipe = tRecipe; // :757
		mOutputItems = tRecipe.getOutputs(tMaxProcessCount); // :758
		mOutputFluids = tRecipe.getFluidOutputs(tMaxProcessCount); // :759

		if (tRecipe.mEUt < 0) { // :761-764 — generator recipes
			mMaxProgress = tRecipe.mDuration;
			mMinEnergy = 0;
		} else {
			// :770-771 verbatim (the TU branch of :770, mEfficiency = 10000 → units() is the identity)
			mMinEnergy = Math.max(1, tRecipe.mEUt);
			mMaxProgress = Math.max(1, units(mMinEnergy * Math.max(1, tRecipe.mDuration), 10000, 10000, true));
			// :773 verbatim — overclocking: 4x energy, 2x speed. With mInputMin = 1 the loop
			// condition mMinEnergy < 1 is never true → zero-overclock fidelity (the ADR ruling ⑤).
			while (mMinEnergy < mInputMin && mMinEnergy * 4 <= mInputMax) {
				mMinEnergy *= 4;
				mMaxProgress *= 2;
			}
		}

		removeEmptyInputStacks(); // :776 removeAllDroppableNullStacks shape
		return FOUND_AND_SUCCESSFULLY_USED_RECIPE; // :777
	}

	/**
	 * Upstream :620-668 — the output-slot blockage semantics kept (equal item + capacity,
	 * mNeedsEmptyOutput), returns the parallel count (mParallel = 16); the doOutputItems
	 * pre-push (:623) is cut with the auto-IO surface; the mParallelDuration chain-limiting
	 * loop (:626-629) is cut (mParallelDuration = false for the Coke Oven shape).
	 */
	public int canOutput(Recipe aRecipe) {
		int rMaxTimes = (int)mParallel; // :621

		for (int i = 0, j = SLOT_INPUT + 1; i < recipes().mOutputItemsCount && i < aRecipe.mOutputs.length; i++, j++) { // :631
			ItemStack tOutput = aRecipe.mOutputs[i];
			if (tOutput == null || tOutput.isEmpty()) continue;
			ItemStack tSlot = slot(j);
			if (tSlot != null && !tSlot.isEmpty()) {
				if (aRecipe.mNeedsEmptyOutput) return 0; // :633-636 (the mMode half is cut with mMode)
				if (!ItemStack.isSameItemSameTags(tSlot, tOutput)) {mOutputBlocked++; return 0;} // :637-640 — blocked
				rMaxTimes = Math.min(rMaxTimes, (tSlot.getMaxStackSize() - tSlot.getCount()) / tOutput.getCount()); // :641
				if (rMaxTimes <= 0) {mOutputBlocked++; return 0;} // :642-645
			} else {
				rMaxTimes = Math.min(rMaxTimes, Math.max(1, 64 / tOutput.getCount())); // :647
			}
		}
		if (aRecipe.mFluidOutputs.length > 0) { // :650-666 verbatim
			int tEmptyOutputTanks = 0, tRequiredEmptyTanks = aRecipe.mFluidOutputs.length;
			for (FluidTankGT tTank : mTanksOutput) if (tTank.isEmpty()) tEmptyOutputTanks++;
			for (int j = 0; j < aRecipe.mFluidOutputs.length; j++) {
				if (aRecipe.mFluidOutputs[j] == null) {
					tRequiredEmptyTanks--;
				} else for (FluidTankGT tTank : mTanksOutput) if (tTank.contains(aRecipe.mFluidOutputs[j])) {
					if (tTank.has(Math.max(16000, 1 + (long)aRecipe.mFluidOutputs[j].getAmount() * mParallel)) && !FLUIDS_VOID_OVERFLOW.contains(String.valueOf(net.minecraftforge.registries.ForgeRegistries.FLUIDS.getKey(aRecipe.mFluidOutputs[j].getFluid())))) return 0; // :659
					tRequiredEmptyTanks--;
					break;
				}
			}
			if (tRequiredEmptyTanks > tEmptyOutputTanks) return 0; // :664
		}
		return rMaxTimes; // :667
	}

	// ---------------------------------------------------------------------------
	// fluid push (ADR ruling ① — the :994-996 semantics over capabilities)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream :994-996 — for each non-empty output tank: probe the full content
	 * (drain SIMULATE), push through the target handler (fill EXECUTE), then deduct exactly
	 * what landed (drain EXECUTE). 0 accepted = source untouched (fill-then-deduct, never
	 * over-draws). The upstream {@code FL.move(tank, delegator)} is the same three-beat.
	 */
	public void doOutputFluids() {
		for (FluidTankGT tCheck : mTanksOutput) if (tCheck.has()) {
			FluidStack tContent = tCheck.fluid();
			if (tContent == null) continue;
			IFluidHandler tTarget = getFluidOutputTarget(tContent.getFluid());
			if (tTarget == null) continue;
			FluidStack tAvailable = tCheck.drain(Integer.MAX_VALUE, FluidAction.SIMULATE);
			if (tAvailable == null || tAvailable.isEmpty()) continue;
			int tFilled = tTarget.fill(tAvailable, FluidAction.EXECUTE);
			if (tFilled <= 0) continue; // the target refuses → the source keeps everything
			FluidStack tDrained = tCheck.drain(tFilled, FluidAction.EXECUTE);
			if (tDrained != null && !tDrained.isEmpty()) mInventoryChanged = true; // the upstream updateInventory() on move > 0
		}
	}

	/**
	 * The reusable output-target seam (upstream :145 abstract {@code getFluidOutputTarget(byte, Fluid)};
	 * the byte side folds away — the ruling pins the target query at Direction.UP, the
	 * upstream SIDE_TOP 1:1). Returns the handler to push into, or null (no target →
	 * {@link #doOutputFluids} skips).
	 */
	protected abstract IFluidHandler getFluidOutputTarget(Fluid aOutput);

	// ---------------------------------------------------------------------------
	// ignition (:373-379)
	// ---------------------------------------------------------------------------

	/** The TOOL_igniter branch verbatim: an ignited machine keeps its recipe re-check window alive for 40 ticks. */
	public void ignite() {
		if (mRequiresIgnition) mIgnited = 40;
	}

	// ---------------------------------------------------------------------------
	// energy surface stubs (no energy net, no redstone — the ADR ruling ⑤)
	// ---------------------------------------------------------------------------

	/** Upstream :1027-1028 — the manual stop toggle (no updateAdjacentToggleableEnergySources surface). */
	public boolean setStateOnOff(boolean aOnOff) {
		if (mStopped == aOnOff) mStopped = !aOnOff;
		return !mStopped;
	}

	/** Upstream :1028. */
	public boolean getStateOnOff() {
		return !mStopped;
	}

	// ---------------------------------------------------------------------------
	// inventory helpers
	// ---------------------------------------------------------------------------

	/** Upstream slot(i) — the live stack reference (consumption shrinks it in place, :690-693). */
	public ItemStack slot(int aIndex) {
		return mInventory.getStackInSlot(aIndex);
	}

	/**
	 * Upstream 05Inventories.addStackToSlot: adds what fits and returns true only when the
	 * whole stack landed (a partial add leaves the remainder pending — the blocked-output
	 * path of doActive keeps it in mOutputItems). {@code aSlot} content index.
	 */
	protected boolean addStackToSlot(int aSlot, ItemStack aStack) {
		if (aStack == null || aStack.isEmpty()) return false;
		ItemStack tCurrent = mInventory.getStackInSlot(aSlot);
		if (tCurrent.isEmpty()) {
			mInventory.setStackInSlot(aSlot, aStack.copy());
			aStack.setCount(0);
			mInventoryChanged = true;
			return true;
		}
		if (!ItemStack.isSameItemSameTags(tCurrent, aStack)) return false;
		int tLimit = Math.min(mInventory.getSlotLimit(aSlot), tCurrent.getMaxStackSize());
		if (tLimit - tCurrent.getCount() < aStack.getCount()) return false;
		tCurrent.grow(aStack.getCount()); // direct mutation — flag the change like upstream updateInventory
		aStack.setCount(0);
		mInventoryChanged = true;
		return true;
	}

	/** Upstream :776 removeAllDroppableNullStacks shape — zero-count stacks left by the in-place consumption become EMPTY. */
	protected void removeEmptyInputStacks() {
		for (int i = 0; i < recipes().mInputItemsCount; i++) {
			ItemStack tStack = mInventory.getStackInSlot(i);
			if (!tStack.isEmpty() && tStack.getCount() <= 0) mInventory.setStackInSlot(i, ItemStack.EMPTY);
		}
	}

	private static boolean containsSomething(ItemStack[] aArray) {
		if (aArray == null) return false;
		for (ItemStack tStack : aArray) if (tStack != null && !tStack.isEmpty()) return true;
		return false;
	}

	private static boolean containsSomething(FluidStack[] aArray) {
		if (aArray == null) return false;
		for (FluidStack tStack : aArray) if (tStack != null && !tStack.isEmpty()) return true;
		return false;
	}

	// ---------------------------------------------------------------------------
	// hooks (:1002-1003)
	// ---------------------------------------------------------------------------

	public void onProcessStarted() {/**/}

	public void onProcessFinished() {/**/}

	// ---------------------------------------------------------------------------
	// capability exposure — the gating IItemHandler (insert input-only, extract output-only)
	// ---------------------------------------------------------------------------

	/**
	 * The gated item surface: insert only the input slot (canInsertItem2 :549-554 with the
	 * mMode empty-slot rule, the same-item dedup and the containsInput legs cut = the pool),
	 * extract only the output slots (canExtractItem2 :556-559). Storage stays the plain
	 * {@link GTItemStackHandler}.
	 */
	private class GatedItemHandler implements IItemHandler {
		@Override public int getSlots() {return mInventory.getSlots();}
		@Override public ItemStack getStackInSlot(int aSlot) {return mInventory.getStackInSlot(aSlot);}
		@Override public int getSlotLimit(int aSlot) {return mInventory.getSlotLimit(aSlot);}
		@Override public boolean isItemValid(int aSlot, ItemStack aStack) {return canInsertItem2(aSlot);}

		@Override
		public ItemStack insertItem(int aSlot, ItemStack aStack, boolean aSimulate) {
			if (aStack == null || aStack.isEmpty() || !canInsertItem2(aSlot)) return aStack;
			return mInventory.insertItem(aSlot, aStack, aSimulate);
		}

		@Override
		public ItemStack extractItem(int aSlot, int aAmount, boolean aSimulate) {
			if (!canExtractItem2(aSlot)) return ItemStack.EMPTY;
			return mInventory.extractItem(aSlot, aAmount, aSimulate);
		}
	}

	/** Upstream canInsertItem2 :549-554 trimmed to the slot range gate (insert only the input range). */
	public boolean canInsertItem2(int aSlot) {
		return aSlot < recipes().mInputItemsCount;
	}

	/** Upstream canExtractItem2 :556-559 verbatim (extract only the output range). */
	public boolean canExtractItem2(int aSlot) {
		return aSlot >= recipes().mInputItemsCount && aSlot < recipes().mInputItemsCount + recipes().mOutputItemsCount;
	}

	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> aCapability, @Nullable Direction aSide) {
		if (aCapability == ForgeCapabilities.ITEM_HANDLER) {
			return mGatedCap.cast(); // the gated surface shadows the root's raw inventory exposure
		}
		return super.getCapability(aCapability, aSide);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		mGatedCap.invalidate();
	}

	// ---------------------------------------------------------------------------
	// NBT (the oven readFromNBT2/writeToNBT2 shape)
	// ---------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.put(NBT_INVENTORY, mInventory.serializeNBT());
		aNBT.putLong(NBT_ENERGY, mEnergy);
		aNBT.putLong(NBT_MINENERGY, mMinEnergy);
		aNBT.putLong(NBT_PROGRESS, mProgress);
		aNBT.putLong(NBT_MAXPROGRESS, mMaxProgress);
		aNBT.putBoolean(NBT_STOPPED, mStopped);
		aNBT.putByte(NBT_IGNITED, mIgnited);
		aNBT.putBoolean(NBT_ACTIVE, mActive);
		aNBT.putBoolean(NBT_RUNNING, mRunning);
		ListTag tOutputItems = new ListTag();
		for (ItemStack tStack : mOutputItems) if (tStack != null && !tStack.isEmpty()) tOutputItems.add(tStack.save(new CompoundTag()));
		aNBT.put(NBT_OUTPUT_ITEMS, tOutputItems);
		ListTag tOutputFluids = new ListTag();
		for (FluidStack tStack : mOutputFluids) if (tStack != null && !tStack.isEmpty()) tOutputFluids.add(tStack.writeToNBT(new CompoundTag()));
		aNBT.put(NBT_OUTPUT_FLUIDS, tOutputFluids);
		mTanksOutput[0].writeToNBT(aNBT, NBT_OUTPUT_TANK);
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_INVENTORY, Tag.TAG_COMPOUND)) mInventory.deserializeNBT(aNBT.getCompound(NBT_INVENTORY));
		mEnergy = aNBT.getLong(NBT_ENERGY);
		mMinEnergy = aNBT.getLong(NBT_MINENERGY);
		mProgress = aNBT.getLong(NBT_PROGRESS);
		mMaxProgress = aNBT.getLong(NBT_MAXPROGRESS);
		if (aNBT.contains(NBT_STOPPED)) mStopped = aNBT.getBoolean(NBT_STOPPED);
		if (aNBT.contains(NBT_IGNITED, Tag.TAG_ANY_NUMERIC)) mIgnited = aNBT.getByte(NBT_IGNITED);
		if (aNBT.contains(NBT_ACTIVE)) mActive = aNBT.getBoolean(NBT_ACTIVE);
		if (aNBT.contains(NBT_RUNNING)) mRunning = aNBT.getBoolean(NBT_RUNNING);
		if (aNBT.contains(NBT_OUTPUT_ITEMS, Tag.TAG_LIST)) {
			ListTag tOutputItems = aNBT.getList(NBT_OUTPUT_ITEMS, Tag.TAG_COMPOUND);
			mOutputItems = new ItemStack[tOutputItems.size()];
			for (int i = 0; i < tOutputItems.size(); i++) mOutputItems[i] = ItemStack.of(tOutputItems.getCompound(i));
		}
		if (aNBT.contains(NBT_OUTPUT_FLUIDS, Tag.TAG_LIST)) {
			ListTag tOutputFluids = aNBT.getList(NBT_OUTPUT_FLUIDS, Tag.TAG_COMPOUND);
			mOutputFluids = new FluidStack[tOutputFluids.size()];
			for (int i = 0; i < tOutputFluids.size(); i++) mOutputFluids[i] = FluidStack.loadFluidStackFromNBT(tOutputFluids.getCompound(i));
		}
		mTanksOutput[0].readFromNBT(aNBT, NBT_OUTPUT_TANK);
	}

	// ---------------------------------------------------------------------------
	// upstream UT.Code.units (UT.java:1677-1683) — the TileEntityOven copy
	// ---------------------------------------------------------------------------

	public static long units(long aAmount, long aOriginalUnit, long aTargetUnit, boolean aRoundUp) {
		if (aTargetUnit == 0) return 0;
		if (aOriginalUnit == aTargetUnit || aOriginalUnit == 0) return aAmount;
		if (aOriginalUnit % aTargetUnit == 0) {
			aOriginalUnit /= aTargetUnit;
			aTargetUnit = 1;
		} else if (aTargetUnit % aOriginalUnit == 0) {
			aTargetUnit /= aOriginalUnit;
			aOriginalUnit = 1;
		}
		return Math.max(0, ((aAmount * aTargetUnit) / aOriginalUnit) + (aRoundUp && (aAmount * aTargetUnit) % aOriginalUnit > 0 ? 1 : 0));
	}
}
