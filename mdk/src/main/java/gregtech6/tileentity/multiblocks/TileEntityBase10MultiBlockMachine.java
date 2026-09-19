package gregtech6.tileentity.multiblocks;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

//? if forge {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
//?}
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.items.IItemHandler;

import gregtech6.fluid.FluidTankGT;
import gregtech6.gui.machines.GTBasicMachineMenu;
import gregtech6.gui.machines.GTBasicMachinesMenus;
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
 * mOutputItems/mOutputFluids and the output-tank bank (default one tank, re-pointable —
 * constructor capacity = Long.MAX_VALUE, upstream FluidTankGT.java:49).
 *
 * <p>GUI face (task p8-cokeoven-gui-menu ②): the class implements MenuProvider +
 * {@link GTBasicMachineMenu.Host} — the Host methods read the raw slot/field face directly
 * (the upstream ContainerCommonBasicMachine reads the TE inventory the same way; the
 * output no-put rule is the menu's OutputSlot), so the Coke Oven reuses the
 * single-block machine menu and screen without a class of its own. The menu opens
 * through {@link #getMenuType()}, defaulting to the cokeoven MenuType (the only existing
 * subclass — future multiblock machines override it with their own registration).
 */
public abstract class TileEntityBase10MultiBlockMachine extends TileEntityBase10MultiBlockBase implements MenuProvider, GTBasicMachineMenu.Host {

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
	/**
	 * Upstream :92 mCheapOverclocking (task p29-w3-nbtdesign-parts ②) — the
	 * NBT_CHEAP_OVERCLOCKING rows (the Distillation Tower, Loader:1226) gate the :773
	 * overclock loop: T = the row refuses the forced 4x-energy/2x-speed fold, the recipe
	 * runs at its natural eUt inside the window. Registration config (the single-block
	 * carrier pattern: constructor/factory injected, NOT persisted —
	 * loadKeepsTheConstructorInjectedConfig).
	 */
	public boolean mCheapOverclocking = false;
	/**
	 * Upstream :92 mParallelDuration (task p29-w3-nbtdesign-parts ②) — the
	 * NBT_PARALLEL_DURATION rows: T = the duration carries the parallels
	 * (the :766-768 linear-duration form + the :626-629 chain-processing cap), F = the
	 * energy does (the :770-771 form + the :743 bind). Registration config, not persisted.
	 */
	public boolean mParallelDuration = false;
	/**
	 * Upstream :99 mEnergyTypeAccepted (task p29-w3-nbtdesign-parts ②) — the
	 * NBT_ENERGY_ACCEPTED carrier driving the :743/:770 type gates; the base default TU is
	 * the Coke Oven shape (the :770 TU half keeps a constant per-process energy and the
	 * :743 bind folds away). Registration config, not persisted.
	 */
	public gregapi.code.TagData mEnergyTypeAccepted = gregapi.data.TD.Energy.TU;
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
	/**
	 * The output-tank bank — default ONE tank (the Coke Oven shape; the upstream
	 * default-constructor capacity = Long.MAX_VALUE, FluidTankGT.java:49). NOT final since
	 * task p30-distill-output-routing (the 2026-09-16 distill-tower ruling, option a): a
	 * machine whose map carries more fluid-OUT slots re-points the bank to the upstream
	 * readFromNBT2 :161 size ({@code mRecipes.mOutputFluidCount}) — the Distillation Tower
	 * is the first consumer (GT6Distillation, the nine-tank library).
	 */
	public FluidTankGT[] mTanksOutput = {new FluidTankGT()};

	/** Upstream :107 default RM.CokeOven; resolved lazily because the map registers at mod construct. */
	public RecipeMap mRecipes = null;

	protected boolean oActive = false, oRunning = false;

	/** The inventory store (also bound into the root capability slot — see the gated exposure below). */
	protected GTItemStackHandler mInventory;

	//? if forge {
	/** The capability wrapper LazyOptional (built in the constructor, invalidated with the root's own). */
	private final LazyOptional<IItemHandler> mGatedCap;
	//?}

	protected TileEntityBase10MultiBlockMachine(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		// the 10Base constructor already enables ticking (the structure poll + TU generation are tick-driven)
		super(aType, aPos, aState);
		mInventory = new GTItemStackHandler(INVENTORY_SIZE, this::onInventoryChanged);
		setInventory(mInventory); // root binding: marks dirty through onContentsChanged
		//? if forge {
		mGatedCap = LazyOptional.of(() -> new GatedItemHandler());
		//?}
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
	// the row-side energy config (upstream readFromNBT2 :126-131, task p29-w3-nbtdesign-parts ②)
	// ---------------------------------------------------------------------------

	/**
	 * The carrier of the upstream per-row NBT energy keys (MultiTileEntityBasicMachine
	 * readFromNBT2 :126-131) — the fixed-window forms the W3 rows ride:
	 * <ul>
	 * <li>the DERIVED form: {@code nbtInput} alone → {@code mInput = in, mInputMin = in/2,
	 *     mInputMax = in*2} (:126) — the Coke Oven :1193 triple (in 1 → 1/1/16 with the
	 *     upstream (1+1)/2 integer division) and the TIER_INPUTS ladders;</li>
	 * <li>the EXPLICIT form: {@code nbtInputMin}/{@code nbtInputMax} override AFTER the
	 *     derived form (:127-128) — the :1229 LargeCentrifuge shape (512/1/4096:
	 *     NBT_INPUT 512, NBT_INPUT_MIN 1, NBT_INPUT_MAX 4096);</li>
	 * <li>{@code nbtParallel} clamps to ≥ 1 (:130); the two booleans are the :122/:131
	 *     flags.</li>
	 * </ul>
	 * {@code null} = the key absent (the upstream {@code hasKey} gate; the field keeps its
	 * prior value). Registration config is compile-time here — {@link #applyEnergyRowSpec}
	 * re-points the live fields, nothing is persisted (the single-block
	 * loadKeepsTheConstructorInjectedConfig contract).
	 */
	public record EnergyRowSpec(Long nbtInput, Long nbtInputMin, Long nbtInputMax, Integer nbtParallel,
			Boolean cheapOverclocking, Boolean parallelDuration) {

		/** The pure derived-form factory (an NBT_INPUT-only row). */
		public static EnergyRowSpec ofInput(long aInput) {
			return new EnergyRowSpec(aInput, null, null, null, null, null);
		}

		/** All keys absent — the applier then changes nothing (the defaults stand). */
		public static EnergyRowSpec none() {
			return new EnergyRowSpec(null, null, null, null, null, null);
		}
	}

	/**
	 * Upstream :126-131 verbatim, application order included: NBT_INPUT derives
	 * (min, in, max) FIRST, the MIN/MAX overrides land AFTER, the parallel clamps, the
	 * two flags re-point. Returns the applied spec for chaining assertions.
	 */
	public EnergyRowSpec applyEnergyRowSpec(EnergyRowSpec aSpec) {
		if (aSpec.nbtInput() != null) { mInput = aSpec.nbtInput(); mInputMin = aSpec.nbtInput() / 2; mInputMax = aSpec.nbtInput() * 2; } // :126
		if (aSpec.nbtInputMin() != null) mInputMin = aSpec.nbtInputMin();                                                              // :127
		if (aSpec.nbtInputMax() != null) mInputMax = aSpec.nbtInputMax();                                                              // :128
		if (aSpec.nbtParallel() != null) mParallel = Math.max(1, aSpec.nbtParallel());                                                 // :130
		if (aSpec.cheapOverclocking() != null) mCheapOverclocking = aSpec.cheapOverclocking();                                         // :122
		if (aSpec.parallelDuration() != null) mParallelDuration = aSpec.parallelDuration();                                            // :131
		return aSpec;
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
			// :454-455 verbatim — TU self-generation (the mChargeRequirement branch :456 is
			// cut with charging). The upstream gate IS the type check: only TU machines
			// self-generate; QU/EU/RF machines charge from the energy face. The task
			// p31-massfab QU Massfab is the first non-TU consumer (the unconditional port
			// increment leaked 1 energy/t into every registered type).
			if (!mStopped && mEnergyTypeAccepted == gregapi.data.TD.Energy.TU) mEnergy++;
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

	/** Upstream :780-793 verbatim (checkStructure is the real multiblock template).
	 *  The ENERGY_FAKE_SOURCE refill mirrors {@link gregtech6.tileentity.machines.TileEntityBasicMachine
	 *  .supplyEnergy}: the ops regime (gt6machine fakesource, the RCON drive seam) must reach the
	 *  multiblock machines too, else no grid-less acceptance drive exists for them — the refill is
	 *  a no-op while the flag is off (the default), so the grid-fed semantics are untouched. */
	public void doWork(long aTimer) {
		if (gregtech6.tileentity.machines.TileEntityBasicMachine.ENERGY_FAKE_SOURCE && !mStopped) mEnergy = mInputMax;
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
			// :742-745 — the TU branch skips the energy bind (:743 guard mEnergyTypeAccepted != TU;
			// RF half cut with the single-block face). The long-overload consume
			// (1 + isRecipeInputEqual(n-1)) becomes the boolean two-stage loop — the P4 Recipe
			// shell carries no long form and stays untouched.
			if (!mParallelDuration && mEnergyTypeAccepted != gregapi.data.TD.Energy.TU) {
				tMaxProcessCount = (int) gregapi.util.UT.Code.bind(1, tMaxProcessCount, mInput / Math.max(1, tRecipe.mEUt)); // :743/:730
			}
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
			if (mParallelDuration) {
				// :766-768 — the duration carries the parallels: the energy stays at the
				// recipe eUt, the max progress scales with the process count (linear time)
				mMinEnergy = Math.max(1, tRecipe.mEUt);
				mMaxProgress = Math.max(1, units(mMinEnergy * Math.max(1, tRecipe.mDuration) * tMaxProcessCount, 10000, 10000, true));
			} else {
				// :770-771 — the energy carries the parallels; the TU half (:770 ternary)
				// keeps a constant per-process energy (the Coke Oven shape), the RF half is
				// cut with the single-block face; mEfficiency = 10000 → units() is the identity
				mMinEnergy = Math.max(1, mEnergyTypeAccepted == gregapi.data.TD.Energy.TU ? tRecipe.mEUt : tRecipe.mEUt * tMaxProcessCount);
				mMaxProgress = Math.max(1, units(mMinEnergy * Math.max(1, tRecipe.mDuration), 10000, 10000, true));
			}
			// :773 — overclocking: 4x energy, 2x speed. mCheapOverclocking = T (the
			// NBT_CHEAP_OVERCLOCKING rows) refuses the fold; with the Coke Oven window
			// mInputMin = 1 the loop condition mMinEnergy < 1 is never true → zero-overclock
			// fidelity (the ADR ruling ⑤).
			if (!mCheapOverclocking) {
				while (mMinEnergy < mInputMin && mMinEnergy * 4 <= mInputMax) {
					mMinEnergy *= 4;
					mMaxProgress *= 2;
				}
			}
		}

		removeEmptyInputStacks(); // :776 removeAllDroppableNullStacks shape
		return FOUND_AND_SUCCESSFULLY_USED_RECIPE; // :777
	}

	/**
	 * Upstream :620-668 — the output-slot blockage semantics kept (equal item + capacity,
	 * mNeedsEmptyOutput), returns the parallel count (mParallel = 16); the doOutputItems
	 * pre-push (:623) is cut with the auto-IO surface. The mParallelDuration chain-limiting
	 * loop (:626-629) is LIVE since task p29-w3-nbtdesign-parts ② (the Coke Oven shape
	 * keeps mParallelDuration = false, so the oven path is unchanged).
	 */
	public int canOutput(Recipe aRecipe) {
		int rMaxTimes = (int)mParallel; // :621

		if (mParallelDuration) {
			// :626-629 verbatim — chain processing: don't take more than 30..120 seconds
			// worth of input at a time (the total power must stay inside mInputMax * 600)
			while (rMaxTimes > 1 && aRecipe.getAbsoluteTotalPower() * rMaxTimes > mInputMax * 600) rMaxTimes--;
		}

		for (int i = 0, j = SLOT_INPUT + 1; i < recipes().mOutputItemsCount && i < aRecipe.mOutputs.length; i++, j++) { // :631
			ItemStack tOutput = aRecipe.mOutputs[i];
			if (tOutput == null || tOutput.isEmpty()) continue;
			ItemStack tSlot = slot(j);
			if (tSlot != null && !tSlot.isEmpty()) {
				if (aRecipe.mNeedsEmptyOutput) return 0; // :633-636 (the mMode half is cut with mMode)
				//? if forge {
				if (!ItemStack.isSameItemSameTags(tSlot, tOutput)) {mOutputBlocked++; return 0;} // :637-640 — blocked
				//?}
				//? if neoforge {
				/* // 1.21.1: isSameItemSameTags renamed to isSameItemSameComponents
				   // (1.21.1 ItemStack javap).
				if (!ItemStack.isSameItemSameComponents(tSlot, tOutput)) {mOutputBlocked++; return 0;} // :637-640 — blocked
				 *///?}
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
					//? if forge {
					if (tTank.has(Math.max(16000, 1 + (long)aRecipe.mFluidOutputs[j].getAmount() * mParallel)) && !FLUIDS_VOID_OVERFLOW.contains(String.valueOf(net.minecraftforge.registries.ForgeRegistries.FLUIDS.getKey(aRecipe.mFluidOutputs[j].getFluid())))) return 0; // :659
					//?}
					//? if neoforge {
					/* // 1.21.1: the registry handle is BuiltInRegistries.FLUID (the swap's
					   // Registries.FLUID is the ResourceKey, not the Registry — no getKey).
					if (tTank.has(Math.max(16000, 1 + (long)aRecipe.mFluidOutputs[j].getAmount() * mParallel)) && !FLUIDS_VOID_OVERFLOW.contains(String.valueOf(net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(aRecipe.mFluidOutputs[j].getFluid())))) return 0; // :659
					 *///?}
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
		//? if forge {
		if (!ItemStack.isSameItemSameTags(tCurrent, aStack)) return false;
		//?}
		//? if neoforge {
		/* // 1.21.1: isSameItemSameComponents (see checkRecipe fork).
		if (!ItemStack.isSameItemSameComponents(tCurrent, aStack)) return false;
		 *///?}
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
	// GUI (upstream getGUIClient2/getGUIServer2 :1007-1008 → MenuProvider; the Host
	// face is the raw slot/field read — ContainerCommonBasicMachine reads the TE
	// inventory directly the same way, task p8-cokeoven-gui-menu ②)
	// ---------------------------------------------------------------------------

	/** The menu binds this as the SlotItemHandler container (TileEntityBasicMachine :199 same shape). */
	@Override
	public GTItemStackHandler getInventory() {
		return mInventory;
	}

	/** The RecipeMap output slot count (the cokeoven map carries 9 — the output grid's 3x4 branch). */
	@Override
	public int getOutputSlotCount() {
		return recipes().mOutputItemsCount;
	}

	@Override
	public boolean isSuccessful() {
		return mSuccessful;
	}

	@Override
	public long getProgress() {
		return mProgress;
	}

	@Override
	public long getMaxProgress() {
		return mMaxProgress;
	}

	/** The mGUITexture = mRecipes.mGUIPath semantics (MultiTileEntityBasicMachine.java:114). */
	@Override
	public String getGuiTexture() {
		return recipes().mGUIPath;
	}

	@Override
	public AbstractContainerMenu createMenu(int aContainerId, Inventory aPlayerInventory, Player aPlayer) {
		return new GTBasicMachineMenu(getMenuType(), aContainerId, aPlayerInventory, this);
	}

	/**
	 * The MenuType this machine's GUI opens with — the only existing subclass is the Coke
	 * Oven (Loader_MultiTileEntities.java:1193), so the default is the cokeoven registration;
	 * future multiblock machines override this with their own MenuType.
	 */
	protected MenuType<? extends GTBasicMachineMenu> getMenuType() {
		return GTBasicMachinesMenus.cokeoven();
	}

	/** The controller block's translatable name (TileEntityBasicMachine :686 same shape). */
	@Override
	public Component getDisplayName() {
		return getBlockState().getBlock().getName();
	}

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

	//? if forge {
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> aCapability, @Nullable Direction aSide) {
		if (aCapability == ForgeCapabilities.ITEM_HANDLER) {
			return mGatedCap.cast(); // the gated surface shadows the root's raw inventory exposure
		}
		if (aCapability == ForgeCapabilities.FLUID_HANDLER) {
			// spec ③ — the fresh-wrapper-per-call form (TileEntityBase08Barrel:348-351): a
			// stored LazyOptional would memoize the wrapper and freeze the FIRST-queried side
			// into it, breaking the side-aware drain (UP vs the five faces).
			return LazyOptional.of(() -> new MultiBlockFluidHandler(this, aSide)).cast();
		}
		return super.getCapability(aCapability, aSide);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		mGatedCap.invalidate();
	}
	//?}
	//? if neoforge {
	/* // 21.1 face: BlockEntity carries no getCapability/invalidateCaps to override — the
	   // gated item surface (GatedItemHandler) and the per-side MultiBlockFluidHandler
	   // expose through the RegisterCapabilitiesEvent provider wiring
	   // (net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent); the fluid side
	   // keeps the fresh-wrapper-per-call semantics in the provider lambda.
	   //
	   // The seam the wiring delegates to (the TileEntityLargeBoiler:727 form — no @Override,
	   // forge getCapability :704-715 mirrored): the gated item surface is rebuilt fresh per
	   // query here (the forge mGatedCap LazyOptional memoizes, but the wrapper is a stateless
	   // view over mInventory, so fresh-per-call is semantically identical).
	public <T> T getCapability(net.neoforged.neoforge.capabilities.BlockCapability<T, Direction> aCapability, @Nullable Direction aSide) {
		if (aCapability == net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK) {
			return (T) new GatedItemHandler();
		}
		if (aCapability == net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK) {
			return (T) new MultiBlockFluidHandler(this, aSide);
		}
		return null;
	}
	 *///?}

	// ---------------------------------------------------------------------------
	// NBT (the oven readFromNBT2/writeToNBT2 shape)
	// ---------------------------------------------------------------------------

	//? if forge {
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
	//?}
	//? if neoforge {
	/* // 21.1 NBT face: the (CompoundTag) signatures ride the shared chain (the 01Root
	   // fork retains them for the BE tree; the canonical loadAdditional/saveAdditional
	   // delegate in). Only the provider-needing IO calls fork — the serialization
	   // HolderLookup.Provider is the frozen builtin view NBT_ACCESS, the whole-tree
	   // 21.1 contract (ADR-P18): vanilla 1.21.1 loads a BE off the chunk via
	   // BlockEntity.loadStatic BEFORE setLevel, so a level-sourced provider NPEs and
	   // the chunk load silently drops the BE (dead controller) — the W4 NBT wave
	   // (tasks.pool-w4-nbt-provider-threading) threads the vanilla-passed provider
	   // through the chain
	   // (1.21.1 ItemStack/FluidStack/ItemStackHandler javap: all NBT IO takes a provider;
	   // parseOptional keeps the empty-on-garbage semantics of ItemStack.of /
	   // loadFluidStackFromNBT).
	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		net.minecraft.core.HolderLookup.Provider aProvider = NBT_ACCESS; // 21.1: the frozen builtin view (item id lookup only), level-less-safe
		aNBT.put(NBT_INVENTORY, mInventory.serializeNBT(aProvider));
		aNBT.putLong(NBT_ENERGY, mEnergy);
		aNBT.putLong(NBT_MINENERGY, mMinEnergy);
		aNBT.putLong(NBT_PROGRESS, mProgress);
		aNBT.putLong(NBT_MAXPROGRESS, mMaxProgress);
		aNBT.putBoolean(NBT_STOPPED, mStopped);
		aNBT.putByte(NBT_IGNITED, mIgnited);
		aNBT.putBoolean(NBT_ACTIVE, mActive);
		aNBT.putBoolean(NBT_RUNNING, mRunning);
		ListTag tOutputItems = new ListTag();
		for (ItemStack tStack : mOutputItems) if (tStack != null && !tStack.isEmpty()) tOutputItems.add(tStack.save(aProvider, new CompoundTag()));
		aNBT.put(NBT_OUTPUT_ITEMS, tOutputItems);
		ListTag tOutputFluids = new ListTag();
		for (FluidStack tStack : mOutputFluids) if (tStack != null && !tStack.isEmpty()) tOutputFluids.add(tStack.save(aProvider, new CompoundTag()));
		aNBT.put(NBT_OUTPUT_FLUIDS, tOutputFluids);
		mTanksOutput[0].writeToNBT(aNBT, NBT_OUTPUT_TANK);
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		net.minecraft.core.HolderLookup.Provider aProvider = NBT_ACCESS; // 21.1: level-less-safe — loadStatic runs before setLevel (ADR-P18)
		if (aNBT.contains(NBT_INVENTORY, Tag.TAG_COMPOUND)) mInventory.deserializeNBT(aProvider, aNBT.getCompound(NBT_INVENTORY));
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
			for (int i = 0; i < tOutputItems.size(); i++) mOutputItems[i] = ItemStack.parseOptional(aProvider, tOutputItems.getCompound(i));
		}
		if (aNBT.contains(NBT_OUTPUT_FLUIDS, Tag.TAG_LIST)) {
			ListTag tOutputFluids = aNBT.getList(NBT_OUTPUT_FLUIDS, Tag.TAG_COMPOUND);
			mOutputFluids = new FluidStack[tOutputFluids.size()];
			for (int i = 0; i < tOutputFluids.size(); i++) mOutputFluids[i] = FluidStack.parseOptional(aProvider, tOutputFluids.getCompound(i));
		}
		mTanksOutput[0].readFromNBT(aNBT, NBT_OUTPUT_TANK);
	}
	 *///?}

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
