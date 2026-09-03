package gregtech6.tileentity.machines;

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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

//? if forge {
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
//?} else {
/*import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
 *///?}
import net.minecraftforge.items.IItemHandler;

import gregapi.code.TagData;
import gregapi.data.TD;

import gregtech6.block.GTOvenBlock;
import gregtech6.client.render.GTModelProperties;
import gregtech6.client.render.GTOvenRenderSnapshot;
import gregtech6.client.render.GTRenderUpdates;
import gregtech6.covers.CoverData;
import gregtech6.covers.GTCoverRenderSnapshot;
import gregtech6.covers.ICoverableTE;
import gregtech6.gui.machines.GTOvenMenu;
import gregtech6.gui.machines.GTOvenMenus;
import gregtech6.registry.GTMachines;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;
import gregtech6.recipes.RecipeMapFurnace;
import gregtech6.tileentity.GTItemStackHandler;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * 1.20.1 port of the first real processing machine, the Oven (RM.Furnace tier) — direct
 * translation of upstream gregapi/tileentity/machines/MultiTileEntityBasicMachine.java
 * trimmed to the Furnace shape (task p4-machine-oven; upstream self-certification
 * Example_Mod.java:167-167 and Loader_MultiTileEntities.java:1288-1291 "Oven" NBT_INPUT
 * 32..2048, tier 1 = the field defaults kept here).
 *
 * <p>Field translation (:92-109): mEnergy/mInputMin/mInput/mInputMax = 0/16/32/64 (:98
 * defaults), mProgress/mMaxProgress (:108), mSuccessful/mActive/mRunning (:109), mStopped
 * (:92), mIgnited (:93), mCouldUseRecipe, mOutputBlocked dropped with the neighbor auto-IO,
 * mOutputItems (:102), mLastRecipe/mCurrentRecipe (:100). The IIconContainer texture sets
 * (:104) become the BlockState ACTIVE/RUNNING properties (upstream getVisualData :1010-1011
 * = the same two bits); mParallel/fluid tanks are cut with their subsystems.
 *
 * <p>Tick business (trimmed verbatim):
 * <ul>
 * <li>{@link #doWork(long)} :780-793 verbatim (single-block {@code checkStructure} :962-964
 *     folded to true; the :792 ignition decrement stays — mIgnited is the post-action
 *     re-check window, see the field doc);</li>
 * <li>{@link #doActive(long, long)} :795-887 with mProgress += min(mInputMax, mEnergy) :813
 *     — the progress unit IS an energy unit (ADR-P4), item output placement i % outCount
 *     :816, and the carryover :843 (mProgress -= mMaxProgress when the outputs cleared)
 *     kept; the parallel and alternating-energy (:815) branches are cut, as is the fluid
 *     output placement (:817-835) and the neighbor auto-push block :867-884 (no logistics
 *     surface yet — outputs stay in the slot, which keeps the upstream canOutput
 *     blockage). The mRequiresIgnition feature is cut with it (aApplyRecipe passes
 *     through unchanged, :737).</li>
 * <li>{@link #checkRecipe(boolean, boolean)} :683-778 with the doInputItems auto-IO (:687)
 *     and the parallel-count blocks (:729-732/:742-745) cut; the energy math :761-774 is
 *     verbatim including the overclock {@code while (mMinEnergy < mInputMin && mMinEnergy *
 *     4 <= mInputMax) {mMinEnergy *= 4; mMaxProgress *= 2;}} :773 (4x energy, 2x speed);</li>
 * <li>{@link #canOutput(Recipe)} :620-668 keeps the output-slot blockage semantics (equal
 *     item + capacity, mNeedsEmptyOutput) and returns the parallel count (always 1 here);
 *     the doOutputItems pre-push :623 and the fluid-tank branch :650-666 are cut;</li>
 * <li>{@link #onTickFirst(boolean)} :443-448 / {@link #onTick(long, boolean)} :451-468 —
 *     the single-block structure check always passes, the fluid auto-IO and the display
 *     tank refresh (:459, :465-466) are cut.</li>
 * </ul>
 *
 * <p>Energy (ADR-P4 ruling, degraded by task p8-d3 §③): option A — the constant
 * full-voltage fake power source — is now gated behind the {@link #ENERGY_FAKE_SOURCE}
 * static test switch, default {@code false} = the machine is grid-fed only through the
 * real energy network surface (the ITileEntityEnergy default block on
 * TileEntityBase01Root, upstream :489-519): doInject :489-508 verbatim (see the method),
 * the EU-only face/type/sizes :510-519 and the Root gate :717 (Min=16=Rec/2: packets
 * below it are swallowed, aSize &gt; 64=Max overcharges). Upstream onTick2 :454-455 fed
 * {@code mEnergy++} per tick (TU trickle); the port instead refills
 * {@code mEnergy = mInputMax} every tick while the machine is not stopped — but only
 * when ENERGY_FAKE_SOURCE is on (the offline test fixtures set it, p4/p6 semantics).
 * doWork drains {@code mInputMax} :791 regardless of the source. Option C — redstone
 * stop: a neighbor signal gates the fake source through a runtime latch
 * ({@link #mRedstoneStopped}) OR'ed at the energy gate, the {@link #setStateOnOff(boolean)}
 * :1027 shape stays on the manual NBT-persisted {@link #mStopped}; the latch is separate
 * so a falling redstone edge cannot release a manual stop. Note: with the default
 * grid-fed mode the p4/p6 smelting RCON chains lose their power premise — they must be
 * driven over the energy network (/gt6wire inject) from p8-d3 on.
 * {@code CONSTANT_ENERGY} (GT_API.java:510, default T) drives the doInactive progress
 * reset :894 verbatim.
 *
 * <p>Recipe consumption follows the p4-recipe-core pinned contract: findRecipe only LOOKS UP
 * (RecipeMapFurnace.findRecipe), consuming is
 * {@code Recipe.isRecipeInputEqual(true, false, fluids, inputs)} (:725/:738 two-stage).
 *
 * <p>Sync: FACING/ACTIVE/RUNNING live on the BlockState (upstream getVisualData :1010-1011
 * is the same two-bit payload), applied in {@link #onTickChecked(long)} through the vanilla
 * furnace idiom (AbstractFurnaceBlockEntity.serverTick: {@code level.setBlock(pos, state,
 * 3)} — same-block state changes keep the BE, LevelChunk.setBlockState:292). Facing is
 * double-written NBT + BlockState (spec 7): NBT is the persistent authority, the BlockState
 * is re-applied from it. Since p9-render-c-oven-overlay the same two fields additionally
 * project into the C-grade {@link GTOvenRenderSnapshot} ({@code getModelData()}); the
 * client arm of the render pair rides {@link #load} (both sync channels converge there).
 */
public class TileEntityOven extends TileEntityBase03TicksAndSync implements MenuProvider, ICoverableTE, ITileEntitySwitchableOnOff {

	// checkRecipe result codes (upstream :672-675 verbatim).
	public static final int DID_NOT_FIND_RECIPE = 0;
	public static final int FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS = 1;
	public static final int FOUND_AND_SUCCESSFULLY_USED_RECIPE = 2;
	public static final int FOUND_AND_COULD_HAVE_USED_RECIPE = 3;

	/** GT_API.java:510 CONSTANT_ENERGY default (config field of the same name upstream). */
	public static final boolean CONSTANT_ENERGY = true;

	/**
	 * Option A degradation switch (task p8-d3 §③): the constant full-voltage fake power
	 * source of ADR-P4. {@code false} (default, the shipped semantic) = grid-fed only —
	 * the machine accepts EU through the ITileEntityEnergy network surface
	 * ({@link #doInject}); {@code true} = the p4/p6 fake source refills
	 * {@code mEnergy = mInputMax} every tick. The offline test fixtures turn it on
	 * (GTMachinesOfflineTestBase) so the p4 acceptance stays regression-covered.
	 */
	public static boolean ENERGY_FAKE_SOURCE = false;

	// NBT keys — plain in-repo form (the chest precedent: upstream "gt.*" keys carry the
	// same names; the vanilla "id" slot collision that forced the gt. prefix is gone).
	public static final String NBT_FACING = "facing";
	public static final String NBT_INVENTORY = "inventory";
	public static final String NBT_ENERGY = "energy";
	public static final String NBT_MINENERGY = "minenergy";
	public static final String NBT_PROGRESS = "progress";
	public static final String NBT_MAXPROGRESS = "maxprogress";
	public static final String NBT_STOPPED = "stopped";
	public static final String NBT_IGNITED = "ignited";
	public static final String NBT_ACTIVE = "active";
	public static final String NBT_RUNNING = "running";
	public static final String NBT_OUTPUT = "output";

	/** Slot count — the RM.Furnace 5-slot shape: 1 input + 1 output + 1 special + 2 fluid displays. */
	public static final int INVENTORY_SIZE = 5;

	/** Content slot indices (upstream RecipeMap order :81: inputs, outputs, special, fluid displays). */
	public static final int SLOT_INPUT = 0, SLOT_OUTPUT = 1, SLOT_SPECIAL = 2, SLOT_FLUID_IN_DISPLAY = 3, SLOT_FLUID_OUT_DISPLAY = 4;

	/** RM.Furnace slot constants (GT6RecipeMaps.java:49-50). */
	public static final int INPUT_ITEMS_COUNT = 1, OUTPUT_ITEMS_COUNT = 1;

	// fields :92-109 (trimmed set, see class doc)
	public long mEnergy = 0, mInputMin = 16, mInput = 32, mInputMax = 64, mMinEnergy = 0;
	public long mProgress = 0, mMaxProgress = 0;
	public boolean mSuccessful = false, mActive = false, mRunning = false;
	public boolean mStopped = false, mNoConstantEnergy = false, mCouldUseRecipe = false, mInventoryChanged = false;
	/**
	 * Upstream :93 mIgnited — NOT the ignition-required feature (mRequiresIgnition, cut) but
	 * the post-action re-check window: every successful output placement sets it to 40 (:816/
	 * :851) and doWork decrements it (:792), which keeps the doActive recipe re-check (:800)
	 * firing after each completion — mInventoryChanged alone cannot (onTickResetChecks clears
	 * it every tick end), so without this counter the carryover :843 would be wiped by the
	 * :803 reset before the next process could start.
	 */
	public byte mIgnited = 0;

	/** Option C runtime latch (ADR-P4): set by a neighbor redstone signal, never persisted. */
	public boolean mRedstoneStopped = false;

	/** Upstream :100. */
	public Recipe mLastRecipe = null, mCurrentRecipe = null;
	/** Upstream :102 — the pending outputs of the current process. */
	public ItemStack[] mOutputItems = new ItemStack[0];
	/** Upstream :107 default RM.Furnace; resolved lazily because the map registers at mod construct. */
	public RecipeMap mRecipes = null;

	/** Upstream :92 mFacing (byte, GT6 side order == Direction.getIndex()) — chest precedent. */
	protected byte mFacing = 2;
	protected boolean oActive = false, oRunning = false;
	/**
	 * Client-side render-dirty flag (task p9-render-c-oven-overlay): set when the client
	 * copy of {@code mActive}/{@code mRunning} changes through {@link #load} (both sync
	 * channels converge there) and consumed by {@link #scheduleRenderRefresh} — the client
	 * arm of the scheduleRenderUpdate pair (requestModelDataUpdate alone never triggers a
	 * chunk rebuild, GTRenderUpdates class doc).
	 */
	private boolean mOvenVisualDirty = false;

	// ---------------------------------------------------------------------------
	// covers (task p4-cover-core ⑤ — composition: the store lives here, the 06Covers
	// behaviour comes from the ICoverableTE defaults; the base-class chain is untouched)
	// ---------------------------------------------------------------------------

	/** Upstream 06Covers :63 mCovers — {@code null} while no face carries a cover. */
	public CoverData mCovers = null;

	@Override
	public CoverData getCovers() {
		return mCovers;
	}

	@Override
	public void setCovers(CoverData aCoverData) {
		mCovers = aCoverData;
	}

	/**
	 * BET factory for BlockEntityType.Builder.of — resolves the shared type through the registry at runtime.
	 */
	public TileEntityOven(BlockPos aPos, BlockState aState) {
		this(GTMachines.OVEN_BE.get(), aPos, aState);
	}

	/** Full constructor — also the offline (test) entry point: Builder.of(...).build(null) works without a registry. */
	public TileEntityOven(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		// the oven ticks: the upstream MTE sits on the 03 ticking chain (mIsTicking = true)
		super(true, aType, aPos, aState);
		setInventory(new GTItemStackHandler(INVENTORY_SIZE, this::onInventoryChanged));
	}

	@Override
	public String getTileEntityName() {
		return "oven"; // Loader_MultiTileEntities.java:1288-1291, BET registry path mirrors it
	}

	/** The menu binds this as the SlotItemHandler container (chest precedent). */
	public GTItemStackHandler getInventory() {
		return mInventory;
	}

	/** Lazy RM.Furnace resolution (upstream :107 field initializer {@code = RM.Furnace}). */
	public RecipeMap recipes() {
		RecipeMap tMap = mRecipes;
		if (tMap == null) {
			tMap = GT6RecipeMaps.FURNACE;
			mRecipes = tMap;
		}
		return tMap;
	}

	// ---------------------------------------------------------------------------
	// tick chain (:443-468 trimmed)
	// ---------------------------------------------------------------------------

	@Override
	public void onTickFirst(boolean aIsServerSide) {
		if (aIsServerSide) {
			// upstream 06Covers :191 — the validity sweep rides onTickFirst before the machine business
			checkCoverValidity();
			// upstream :446 — checkStructure(T) always passes for the single-block machine (:962-964)
			if (!mActive) checkRecipe(false, mRunning || mStopped);
		}
	}

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		// upstream 06Covers :200 — the cover tick precedes the machine business
		if (hasCovers()) getCovers().tickPre(aTimer, aIsServerSide, mBlockUpdated, mInventoryChanged);
		if (aIsServerSide) {
			// option C: redstone stop gate (upstream :453 mBlockUpdated toggleable-source refresh spot)
			mRedstoneStopped = hasLevel() && getLevel().hasNeighborSignal(getBlockPos());
			// option A: constant full-voltage fake power (upstream :454-455 TU trickle replaced; :791 drains it below)
			// — now gated behind ENERGY_FAKE_SOURCE (task p8-d3 §③): default false = grid-fed via doInject only.
			if (ENERGY_FAKE_SOURCE && !mStopped && !mRedstoneStopped) mEnergy = mInputMax;

			doWork(aTimer);
			// upstream :459 fluid auto-output and :463 structural re-check :465-466 display refresh are cut
		}
		// upstream 06Covers :202 — the cover tick follows the machine business
		if (hasCovers()) getCovers().tickPost(aTimer, aIsServerSide, mBlockUpdated, mInventoryChanged);
	}

	@Override
	public boolean onTickCheck(long aTimer) {
		// upstream :471-473 verbatim (the visual-data change pair) + 06Covers :184-186 (the cover visual sync)
		return (hasCovers() && getCovers().requiresSync()) || mActive != oActive || mRunning != oRunning || super.onTickCheck(aTimer);
	}

	@Override
	public void onTickChecked(long aTimer) {
		// upstream 06Covers :178-181 — the visual sync flags reset after the sync window
		if (hasCovers()) getCovers().resetSync();
		applyVisualState();
	}

	@Override
	public void onTickResetChecks(long aTimer, boolean aIsServerSide) {
		super.onTickResetChecks(aTimer, aIsServerSide);
		// upstream :476-480 (oActive/oRunning pair) + 05Inventories.java:86-89 (mInventoryChanged)
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
	// work (:780-887 trimmed verbatim)
	// ---------------------------------------------------------------------------

	/** Upstream :780-793 verbatim (checkStructure folded, ignition cut). */
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
		mEnergy -= mInputMax;
		if (mEnergy < 0) mEnergy = 0; // :791
		if (mIgnited > 0) mIgnited--; // :792
	}

	/** Upstream :795-887 — progress = energy units (:813), output wrap i % outCount (:816), carryover (:843). */
	public boolean doActive(long aTimer, long aEnergy) {
		boolean rActive = false;

		if (mMaxProgress <= 0) {
			// :798-805 verbatim — mIgnited is the post-action re-check window (see field doc)
			if ((mIgnited > 0 || mInventoryChanged || !mRunning || aTimer % 1200 == 5) && checkRecipe(!mStopped, true) == FOUND_AND_SUCCESSFULLY_USED_RECIPE) {
				onProcessStarted();
			} else {
				mProgress = 0;
			}
		}

		mSuccessful = false; // :807

		if (mMaxProgress > 0) {
			rActive = true; // :810 (mSpecialIsStartEnergy cut with the special-start-energy path)
			if (mProgress <= mMaxProgress) {
				mProgress += aEnergy; // :813 — the progress unit IS an energy unit (ADR-P4)
			}
			// :815 — the alternating-energy half (mStateOld && !mStateNew) is cut; TU is not alternating
			if (mProgress >= mMaxProgress) {
				// :816 — outputs wrap around the output slot range: i % mOutputItemsCount
				for (int i = 0; i < mOutputItems.length; i++) if (mOutputItems[i] != null && addStackToSlot(SLOT_OUTPUT + (i % OUTPUT_ITEMS_COUNT), mOutputItems[i])) {
					mSuccessful = true;
					mIgnited = 40; // :816
					mOutputItems[i] = null;
					continue;
				}
				// :817-835 fluid output placement cut (no output tanks)

				if (containsSomething(mOutputItems)) {
					// :837-841 — outputs blocked: park at max progress and retry the placement next tick
					mMinEnergy = 0;
					mProgress = mMaxProgress;
				} else {
					// :843-861 — all outputs placed: carry the leftover energy into the next process
					mProgress -= mMaxProgress;
					mMinEnergy = 0;
					mMaxProgress = 0;
					mOutputItems = new ItemStack[0];
					mSuccessful = true;
					mIgnited = 40; // :851
					onProcessFinished();
				}
			}
		}

		// :867-884 neighbor auto-push cut (no logistics surface; canOutput keeps the blockage)
		return rActive;
	}

	/** Upstream :889-900 — sound interrupt and neighbor output push cut, CONSTANT_ENERGY reset verbatim. */
	public boolean doInactive(long aTimer) {
		if (CONSTANT_ENERGY && !mNoConstantEnergy) mProgress = 0; // :894
		if (mRunning || mIgnited > 0 || mInventoryChanged || aTimer % 1200 == 5) { // :895
			checkRecipe(false, true); // :897 (the checkStructure(T) call :896 always passes)
		}
		return false;
	}

	// ---------------------------------------------------------------------------
	// recipe check (:683-778 trimmed)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream :683-778 — doInputItems auto-IO (:687) and the parallel blocks (:729-732 /
	 * :742-745) cut; the energy math :761-774 is verbatim. {@code aApplyRecipe=false} probes,
	 * {@code true} consumes (the two-stage isRecipeInputEqual contract).
	 */
	public int checkRecipe(boolean aApplyRecipe, boolean aUseAutoIO) {
		mCouldUseRecipe = false; // :684
		RecipeMap tRecipes = recipes();
		if (tRecipes == null) return DID_NOT_FIND_RECIPE; // :685

		int tInputItemsCount = 0; // :689 (fluids have no tanks in the Furnace shape)
		ItemStack[] tInputs = new ItemStack[tRecipes.mInputItemsCount];
		for (int i = 0; i < tRecipes.mInputItemsCount; i++) {
			tInputs[i] = slot(i);
			if (tInputs[i] != null && !tInputs[i].isEmpty()) tInputItemsCount++;
		}

		// :696-706 fluid auto-input and tank counting cut (Furnace map: mMinimalInputFluids = 0)
		if (tInputItemsCount < tRecipes.mMinimalInputItems) return DID_NOT_FIND_RECIPE; // :708
		if (tInputItemsCount < tRecipes.mMinimalInputs) return DID_NOT_FIND_RECIPE; // :710

		// :712 — mInputMax is the voltage (TU carries no RF conversion); the special slot content rides along.
		// The Furnace bridge queries the vanilla RecipeManager and therefore takes the Level; plain
		// RecipeMaps keep the base signature (W1 contract, RecipeMap.java:118).
		Recipe tRecipe;
		if (tRecipes instanceof RecipeMapFurnace tFurnace) {
			tRecipe = tFurnace.findRecipe(getLevel(), mLastRecipe, mInputMax, slot(SLOT_SPECIAL), null, tInputs);
		} else {
			tRecipe = tRecipes.findRecipe(mLastRecipe, mInputMax, slot(SLOT_SPECIAL), null, tInputs);
		}
		if (tRecipe == null) return DID_NOT_FIND_RECIPE; // :719 shape (the mCanUseOutputTanks fallback :717-718 is cut)

		if (tRecipe.mCanBeBuffered) mLastRecipe = tRecipe; // :734
		int tMaxProcessCount = canOutput(tRecipe); // :735 (mParallel is 1)
		if (tMaxProcessCount <= 0) return FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS; // :736

		// :737 — the ignition half (mRequiresIgnition || mIgnited > 0 || mActive) is cut
		if (!tRecipe.isRecipeInputEqual(aApplyRecipe, false, null, tInputs)) return FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS; // :738
		mCouldUseRecipe = true; // :739
		if (!aApplyRecipe) return FOUND_AND_COULD_HAVE_USED_RECIPE; // :740

		// :748-755 adjacent-inventory notify, mSpecialIsStartEnergy and the parallel rebalance cut

		mCurrentRecipe = tRecipe; // :757
		mOutputItems = tRecipe.getOutputs(tMaxProcessCount); // :758
		// :759 fluid outputs cut with the output tanks

		if (tRecipe.mEUt < 0) { // :761-764 — generator recipes
			mMaxProgress = tRecipe.mDuration;
			mMinEnergy = 0;
		} else {
			// :770-771 verbatim (TU branch of :770, mEfficiency = 10000 → units() is the identity)
			mMinEnergy = Math.max(1, tRecipe.mEUt);
			mMaxProgress = Math.max(1, units(mMinEnergy * Math.max(1, tRecipe.mDuration), 10000, 10000, true));
			// :773 verbatim — overclocking: 4x energy, 2x speed
			while (mMinEnergy < mInputMin && mMinEnergy * 4 <= mInputMax) {
				mMinEnergy *= 4;
				mMaxProgress *= 2;
			}
		}

		removeEmptyInputStacks(); // :776 removeAllDroppableNullStacks shape
		return FOUND_AND_SUCCESSFULLY_USED_RECIPE; // :777
	}

	/**
	 * Upstream :620-668 — output-slot blockage semantics kept (equal item + capacity,
	 * mNeedsEmptyOutput), returns the parallel count; the doOutputItems pre-push (:623) and
	 * the fluid-tank branch (:650-666) are cut.
	 */
	public int canOutput(Recipe aRecipe) {
		int rMaxTimes = 1; // :621 (mParallel = 1)
		for (int i = 0, j = SLOT_OUTPUT; i < recipes().mOutputItemsCount && i < aRecipe.mOutputs.length; i++, j++) {
			ItemStack tOutput = aRecipe.mOutputs[i];
			if (tOutput == null || tOutput.isEmpty()) continue;
			ItemStack tSlot = slot(j);
			if (tSlot != null && !tSlot.isEmpty()) {
				if (aRecipe.mNeedsEmptyOutput) return 0; // :633-636 (the mMode half is cut)
				if (!ItemStack.isSameItemSameTags(tSlot, tOutput)) return 0; // :637-640 — blocked
				rMaxTimes = Math.min(rMaxTimes, (tSlot.getMaxStackSize() - tSlot.getCount()) / tOutput.getCount()); // :641
				if (rMaxTimes <= 0) return 0; // :642-645
			} else {
				rMaxTimes = Math.min(rMaxTimes, Math.max(1, 64 / tOutput.getCount())); // :647
			}
		}
		return rMaxTimes; // :667
	}

	// ---------------------------------------------------------------------------
	// energy surface (task p8-d3 §② — the network consumer face, upstream
	// MultiTileEntityBasicMachine :489-519)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream :489-508 verbatim minus the charging branch (:497-500 — mChargeRequirement/
	 * mEnergyTypeCharged are outside the oven trimmed field set :133, declared deviation):
	 * a stopped machine refuses (0, :490); an over-voltage packet overcharges
	 * ({@code aSize > mInputMax = 64}) and reports the whole amount as used (:493-495);
	 * accepted EU packets charge {@code min(mInputMax - mEnergy, size * amount)} energy,
	 * consuming the corresponding packet count with the rounding-up remainder
	 * (:501-505). Called through the Root gate (:717) — so simulation calls
	 * ({@code aDoInject = false}) and below-minimum packets (Min = 16, swallowed) never
	 * reach this body, and the overcharge flag is consumed by the machine's own next tick.
	 */
	@Override
	public long doInject(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
		if (mStopped) return 0;
		boolean tPositive = (aSize > 0);
		aSize = Math.abs(aSize);
		if (aSize > getEnergySizeInputMax(aEnergyType, aSide)) {
			if (aDoInject) overcharge(aSize, aEnergyType);
			return aAmount;
		}
		// :497-500 charging branch cut (mChargeRequirement/mEnergyTypeCharged, declared deviation)
		if (aEnergyType == TD.Energy.EU) { // :501 mEnergyTypeAccepted == EU for the oven
			if (aDoInject) mStateNew = tPositive;
			long tInput = Math.min(mInputMax - mEnergy, aSize * aAmount), tConsumed = Math.min(aAmount, (tInput/aSize) + (tInput%aSize!=0?1:0));
			if (aDoInject) mEnergy += tConsumed * aSize;
			return tConsumed;
		}
		return 0;
	}

	/**
	 * Upstream :510 for the oven shape: accepting EU only, emitting nothing
	 * (mEnergyTypeEmitted is null) and the charging pair is cut with the charging branch.
	 */
	@Override public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {return !aEmitting && aEnergyType == TD.Energy.EU;}

	/**
	 * Upstream :511 minus the FACE_CONNECTED rotation-index mask item — the mask
	 * semantics (the 6-bit energy-input sides mask indexed through the facing rotation)
	 * collapse to the all-sides constant for the oven (SIDES full 1) and hang on the
	 * {@link #isEnergyInputSide(byte)} seam; the doInject body is orthogonal to the mask.
	 * The {@code (aTheoretical || !mStopped)} prefix keeps conductors visually connected
	 * to a stopped machine.
	 */
	@Override public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {return (aTheoretical || !mStopped) && isEnergyInputSide(aSide) && super.isEnergyAcceptingFrom(aEnergyType, aSide, aTheoretical);}

	/**
	 * The :511 input-mask seam — all six sides for the oven (upstream mEnergyInputs all-1
	 * shape). Overridable seam for machines with real per-side masks.
	 */
	public boolean isEnergyInputSide(byte aSide) {return true;}

	/** Upstream :513 + the oven trimmed field set :133. */
	@Override public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {return mInputMin;}

	/** Upstream :514 + the oven trimmed field set :133. */
	@Override public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {return mInput;}

	/** Upstream :515 + the oven trimmed field set :133 — also the overcharge threshold in {@link #doInject}. */
	@Override public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {return mInputMax;}

	/** Upstream :519 (mEnergyTypeAccepted.AS_LIST) — EU only. */
	@Override public java.util.Collection<TagData> getEnergyTypes(byte aSide) {return TD.Energy.EU.AS_LIST;}

	/** Upstream :92 mStateNew — the alternating-state latch written by doInject :502. The
	 * :815 alternating consumer is cut (the machine-family ADR keeps the alternating
	 * half-maintain in the pool) and EU is not an ALL_ALTERNATING member (TD.java:219 =
	 * (F, KU)), so the latch stays write-only here; kept for the verbatim :501-505 shape. */
	public boolean mStateNew = false;

	/** Upstream :1027 verbatim (manual stop toggle; the redstone latch is separate). */
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

	// ---------------------------------------------------------------------------
	// hooks (:1002-1003)
	// ---------------------------------------------------------------------------

	public void onProcessStarted() {/**/}

	public void onProcessFinished() {/**/}

	/** Upstream :962-964 — the basic machine is a single block; the structure check always passes. */
	public boolean checkStructure(boolean aForceReset) {
		return true;
	}

	// ---------------------------------------------------------------------------
	// facing + visual state (BlockState double-write, spec 7)
	// ---------------------------------------------------------------------------

	/** Chest precedent onPlaced :128-131 — GT6 side order == Direction.getIndex() (get3DDataValue). */
	public void setFacingFromPlacement(Player aPlayer) {
		mFacing = (byte) aPlayer.getDirection().get3DDataValue();
		applyVisualState(); // facing changes write NBT (persistence) + BlockState (visuals) immediately
	}

	public byte getFacing() {
		return mFacing;
	}

	/**
	 * GTCEu MetaMachine.setFrontFacing :794-811 counterpart (task p6-oven-rotation): the
	 * shift-click edge-cell rotation of the wrench grid. Only a horizontal side (2..5, the
	 * HORIZONTAL_FACING domain) different from the current facing rotates — the same-facing
	 * call is the :796 no-op and vertical/invalid sides are rejected (the
	 * allowExtendedFacing=false shape; the callers exclude the front side as well, the
	 * GTCEu isFacingValid :770-778 double guard). Persistence keeps the spec-7 double
	 * write: mFacing + NBT stays the persistent authority, applyVisualState re-applies the
	 * BlockState (setBlock(state, 3) — the client display authority).
	 *
	 * @return true when the facing actually changed (the rotation-consumer/command feedback).
	 */
	public boolean setFrontFacing(byte aSide) {
		if (aSide < 2 || aSide > 5 || aSide == mFacing) return false; // :796 no-op + the horizontal domain
		mFacing = aSide;
		setChanged();
		applyVisualState();
		return true;
	}

	/**
	 * Applies FACING/ACTIVE/RUNNING onto the BlockState (upstream getVisualData :1010-1011
	 * two-bit payload) — the vanilla furnace setBlock(state, 3) idiom; same-block state
	 * changes keep the BE (LevelChunk.setBlockState:292).
	 */
	public void applyVisualState() {
		if (!hasLevel() || isClientSide()) return;
		BlockState tState = getLevel().getBlockState(getBlockPos());
		if (!(tState.getBlock() instanceof GTOvenBlock)) return;
		BlockState tNew = tState
				.setValue(GTOvenBlock.FACING, Direction.from3DDataValue(mFacing))
				.setValue(GTOvenBlock.ACTIVE, mActive)
				.setValue(GTOvenBlock.RUNNING, mRunning);
		if (tNew != tState) {
			getLevel().setBlock(getBlockPos(), tNew, 3);
		}
	}

	// ---------------------------------------------------------------------------
	// GUI (upstream getGUIClient2/getGUIServer2 :1007-1008 → MenuProvider)
	// ---------------------------------------------------------------------------

	@Override
	public AbstractContainerMenu createMenu(int aContainerId, Inventory aPlayerInventory, Player aPlayer) {
		return new GTOvenMenu(GTOvenMenus.oven(), aContainerId, aPlayerInventory, this);
	}

	@Override
	public Component getDisplayName() {
		return getBlockState().getBlock().getName();
	}

	// ---------------------------------------------------------------------------
	// NBT (upstream readFromNBT2 :112-155 / writeToNBT2 shape)
	// ---------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putByte(NBT_FACING, mFacing);
		aNBT.put(NBT_INVENTORY, mInventory.serializeNBT());
		aNBT.putLong(NBT_ENERGY, mEnergy); // upstream NBT_ENERGY :115
		aNBT.putLong(NBT_MINENERGY, mMinEnergy); // upstream NBT_MINENERGY :129
		aNBT.putLong(NBT_PROGRESS, mProgress); // upstream NBT_PROGRESS :133
		aNBT.putLong(NBT_MAXPROGRESS, mMaxProgress); // upstream NBT_MAXPROGRESS :134
		aNBT.putBoolean(NBT_STOPPED, mStopped); // upstream NBT_STOPPED :117
		aNBT.putByte(NBT_IGNITED, mIgnited); // upstream NBT_IGNITION :136
		aNBT.putBoolean(NBT_ACTIVE, mActive); // upstream NBT_ACTIVE :116
		aNBT.putBoolean(NBT_RUNNING, mRunning); // upstream NBT_RUNNING :118
		ListTag tOutputs = new ListTag();
		for (ItemStack tStack : mOutputItems) if (tStack != null && !tStack.isEmpty()) tOutputs.add(tStack.save(new CompoundTag()));
		aNBT.put(NBT_OUTPUT, tOutputs); // upstream NBT_INV_OUT.i :166-167 (list form)
		writeCoversToNBT(aNBT); // upstream 06Covers :74
	}

	@Override
	public void load(CompoundTag aNBT) {
		// the client visual-field change detector: mActive/mRunning are rewritten below,
		// so the pre-load values must be captured before super.load/the field reads
		boolean tWasActive = mActive, tWasRunning = mRunning;
		super.load(aNBT);
		if (aNBT.contains(NBT_FACING, Tag.TAG_ANY_NUMERIC)) mFacing = aNBT.getByte(NBT_FACING);
		if (aNBT.contains(NBT_INVENTORY, Tag.TAG_COMPOUND)) mInventory.deserializeNBT(aNBT.getCompound(NBT_INVENTORY));
		mEnergy = aNBT.getLong(NBT_ENERGY); // :115
		mMinEnergy = aNBT.getLong(NBT_MINENERGY); // :129
		mProgress = aNBT.getLong(NBT_PROGRESS); // :133
		mMaxProgress = aNBT.getLong(NBT_MAXPROGRESS); // :134
		if (aNBT.contains(NBT_STOPPED)) mStopped = aNBT.getBoolean(NBT_STOPPED); // :117
		if (aNBT.contains(NBT_IGNITED, Tag.TAG_ANY_NUMERIC)) mIgnited = aNBT.getByte(NBT_IGNITED); // :136
		if (aNBT.contains(NBT_ACTIVE)) mActive = aNBT.getBoolean(NBT_ACTIVE); // :116
		if (aNBT.contains(NBT_RUNNING)) mRunning = aNBT.getBoolean(NBT_RUNNING); // :118
		if (aNBT.contains(NBT_OUTPUT, Tag.TAG_LIST)) {
			ListTag tOutputs = aNBT.getList(NBT_OUTPUT, Tag.TAG_COMPOUND);
			mOutputItems = new ItemStack[tOutputs.size()];
			for (int i = 0; i < tOutputs.size(); i++) mOutputItems[i] = ItemStack.of(tOutputs.getCompound(i));
		}
		readCoversFromNBT(aNBT); // upstream 06Covers :68
		// task p9-render-c-oven-overlay: the client write point of mActive/mRunning — both
		// sync channels (chunk data + block update) converge on this load; flag the render
		// pair, scheduleRenderRefresh consumes it
		if (hasLevel() && isClientSide() && (mActive != tWasActive || mRunning != tWasRunning)) mOvenVisualDirty = true;
	}

	// ---------------------------------------------------------------------------
	// cover sync + render refresh (task p4-cover-core ⑦/⑥)
	// ---------------------------------------------------------------------------

	/**
	 * The covers ride the two sync channels through {@code saveAdditional/load} (both
	 * converge on {@link #load}). After either channel lands, the client schedules the
	 * render refresh pair — the server side triggers it implicitly: a cover change flags
	 * the sync window ({@link #syncCoverClientData} → sendClientData → sendBlockUpdated),
	 * the client BE data packet lands here, and the pair (sendBlockUpdated +
	 * requestModelDataUpdate) pushes the fresh snapshot into the ModelDataManager before
	 * the rebuild task reads it. The server blockEvent forward (GTRenderUpdates template)
	 * is the alternative once the Block side enters this card's scope.
	 */
	@Override
	public void onLoad() {
		super.onLoad();
		scheduleRenderRefresh();
	}

	@Override
	public void handleUpdateTag(CompoundTag aTag) {
		super.handleUpdateTag(aTag);
		scheduleRenderRefresh(); // chunk-data channel (login/chunk load)
	}

	@Override
	public void onDataPacket(net.minecraft.network.Connection aNet, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket aPacket) {
		super.onDataPacket(aNet, aPacket);
		scheduleRenderRefresh(); // block-update channel (cover changes, machine visuals)
	}

	/**
	 * The client arm of the scheduleRenderUpdate pair (task p4-cover-core ⑦ + task
	 * p9-render-c-oven-overlay): fires when the covers changed OR the oven visual fields
	 * changed through {@link #load} — the pair (sendBlockUpdated + requestModelDataUpdate)
	 * pushes the fresh snapshot into the ModelDataManager before the rebuild task reads
	 * it. The server side needs no blockEvent forward here: the machine's visual writes
	 * already ride the vanilla channels (applyVisualState setBlock(state, 3) + the
	 * onTickCheck sync window), and both client channels land in {@link #load}, which
	 * flags {@link #mOvenVisualDirty}.
	 */
	private void scheduleRenderRefresh() {
		if (!hasLevel() || !isClientSide()) return;
		if (!mOvenVisualDirty && !hasCovers()) return;
		mOvenVisualDirty = false;
		GTRenderUpdates.scheduleRenderUpdate(this);
	}

	/**
	 * The C-grade render hook (IForgeBlockEntity.java:174), rebuilt by task
	 * p9-render-c-oven-overlay as a two-snapshot payload over disjoint properties:
	 * <ul>
	 * <li>{@link GTModelProperties#OVEN_SNAPSHOT} — always present: the read-only
	 *     projection of {@code mActive}/{@code mRunning} taken at this exact moment
	 *     (single-writer: those fields are the only source; the blockstate ACTIVE/RUNNING
	 *     properties and this snapshot are both readers of them, never a second writer).</li>
	 * <li>{@link GTModelProperties#RENDER_SNAPSHOT} — the cover chain (p4-cover-core),
	 *     present exactly when a face carries a cover; untouched.</li>
	 * </ul>
	 * The {@link GTOvenOverlayModel} keys on the oven property, the cover plate model on
	 * the cover property — the second ModelProperty exists precisely because
	 * RENDER_SNAPSHOT is single-valued and the cover value must not be overwritten.
	 */
	@Override
	public net.minecraftforge.client.model.data.ModelData getModelData() {
		GTOvenRenderSnapshot tOven = new GTOvenRenderSnapshot(mActive, mRunning);
		CoverData tCovers = mCovers;
		if (tCovers == null) {
			return GTModelProperties.derive(super.getModelData())
					.with(GTModelProperties.OVEN_SNAPSHOT, tOven)
					.build();
		}
		java.util.Map<net.minecraft.core.Direction, net.minecraft.resources.ResourceLocation> tSprites = new java.util.EnumMap<>(net.minecraft.core.Direction.class);
		for (byte tSide = 0; tSide < 6; tSide++) {
			if (tCovers.mBehaviours[tSide] == null) continue;
			net.minecraft.resources.ResourceLocation tSprite = tCovers.mBehaviours[tSide].getCoverTextureSurface(tSide, tCovers);
			if (tSprite != null) tSprites.put(net.minecraft.core.Direction.from3DDataValue(tSide), tSprite);
		}
		if (tSprites.isEmpty()) {
			return GTModelProperties.derive(super.getModelData())
					.with(GTModelProperties.OVEN_SNAPSHOT, tOven)
					.build();
		}
		return GTModelProperties.derive(super.getModelData())
				.with(GTModelProperties.RENDER_SNAPSHOT, new GTCoverRenderSnapshot(tSprites))
				.with(GTModelProperties.OVEN_SNAPSHOT, tOven)
				.build();
	}

	// ---------------------------------------------------------------------------
	// capability exposure — the side-aware cover-gated IItemHandler
	// (task p10-cover-item-intercept, ADR 2026-09-01-p10-cover-item-intercept)
	// ---------------------------------------------------------------------------

	/**
	 * The per-face capability handles (BasicMachine {@code mGatedCap} shape, TileEntityBasicMachine:222,
	 * indexed by {@code Direction.get3DDataValue()} — the GT6 side order). Lazily created at the
	 * capability request that captures the {@link Direction}, invalidated with the BE.
	 */
	//? if forge {
	@SuppressWarnings("unchecked")
	private final LazyOptional<IItemHandler>[] mCoverGatedCaps = new LazyOptional[6];
	//?} else {
	/*private final IItemHandler[] mCoverGatedHandlers = new IItemHandler[6]; // (1.21.1) the per-face lazy cache kept — live cover state, cover removal still immediate; no invalidation surface
	 *///?}

	/**
	 * Upstream the oven sat on the 04Covers host, whose final ISidedInventory dispatch
	 * (TileEntityBase04Covers:343-365) consulted the queried face's cover before every
	 * insert/extract/slot query reached the inventory. The 1.20.1 counterpart shadows the
	 * root's raw item-handler exposure (TileEntityBase01Root:407-415) with a side-aware
	 * decorator: the capability request captures the face, the decorator runs the
	 * {@link ICoverableTE} item gates for that face BEFORE the inner inventory is touched.
	 * The side-less query (null direction, e.g. GTMultiBlockCommand:366) keeps the raw
	 * handler — the upstream SIDES_INVALID face never consults a cover either (:355).
	 */
	//? if forge {
	@Override
	public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> aCapability, @Nullable Direction aSide) {
		if (aCapability == ForgeCapabilities.ITEM_HANDLER && aSide != null) {
			int tIndex = aSide.get3DDataValue();
			LazyOptional<IItemHandler> tCap = mCoverGatedCaps[tIndex];
			if (tCap == null) {
				tCap = LazyOptional.of(() -> newCoverGatedHandler(aSide)); // the request captures the face
				mCoverGatedCaps[tIndex] = tCap;
			}
			return tCap.cast();
		}
		return super.getCapability(aCapability, aSide);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		for (LazyOptional<IItemHandler> tCap : mCoverGatedCaps) if (tCap != null) tCap.invalidate();
	}
	//?} else {
	/*// (1.21.1 seam: NeoForge 21.1 removed BlockEntity#getCapability/LazyOptional — W4's
	// RegisterCapabilitiesEvent.registerBlockEntity delegates to this member; no @Override.
	// The per-face lazy cache keeps the p10 semantics: the wrapper consults the LIVE cover
	// state, so cover removal still takes effect immediately.)
	public <T> T getCapability(BlockCapability<T, Direction> aCapability, @Nullable Direction aSide) {
		if (aCapability == Capabilities.ItemHandler.BLOCK && aSide != null) {
			int tIndex = aSide.get3DDataValue();
			IItemHandler tHandler = mCoverGatedHandlers[tIndex];
			if (tHandler == null) {
				tHandler = newCoverGatedHandler(aSide); // the request captures the face
				mCoverGatedHandlers[tIndex] = tHandler;
			}
			return (T) tHandler;
		}
		return null;
	}
	 *///?}

	/**
	 * The side-aware decorator over {@link #getInventory()} (the BasicMachine anonymous
	 * gated-handler shape, TileEntityBasicMachine:696-716). Insert/extract consult the
	 * captured face's cover gates first — an intercept hit refuses, an override hit lets
	 * the cover answer AND the inner inventory still applies its own admission (the
	 * upstream {@code && canInsertItem2} :353/:362 host half), the slot-visibility gate
	 * narrows automation to the cover's accessible slots when claimed. Public: the
	 * offline test seam (ForgeCapabilities cannot class-init offline, the
	 * TestMachineBlockEntityNBTTest:71 precedent) and the future machine-card reuse face.
	 *
	 * @param aSide the face the capability was requested on — the gates key on it
	 */
	public IItemHandler newCoverGatedHandler(Direction aSide) {
		GTItemStackHandler tInventory = getInventory();
		byte tCoverSide = (byte) aSide.get3DDataValue();
		int[] tAllSlots = new int[tInventory.getSlots()];
		for (int i = 0; i < tAllSlots.length; i++) tAllSlots[i] = i;
		return new IItemHandler() {
			@Override public int getSlots() {return tInventory.getSlots();}
			@Override public ItemStack getStackInSlot(int aSlot) {return tInventory.getStackInSlot(aSlot);}
			@Override public int getSlotLimit(int aSlot) {return tInventory.getSlotLimit(aSlot);}

			@Override
			public boolean isItemValid(int aSlot, ItemStack aStack) {
				return slotVisible(aSlot) && canInsertItem(tCoverSide, aSlot, aStack) && tInventory.isItemValid(aSlot, aStack);
			}

			@Override
			public ItemStack insertItem(int aSlot, ItemStack aStack, boolean aSimulate) {
				if (aStack == null || aStack.isEmpty()) return aStack;
				if (!slotVisible(aSlot) || !canInsertItem(tCoverSide, aSlot, aStack)) return aStack; // the refused stack returns untouched
				return tInventory.insertItem(aSlot, aStack, aSimulate);
			}

			@Override
			public ItemStack extractItem(int aSlot, int aAmount, boolean aSimulate) {
				// the 06Covers :336 form: the extract query carries no stack, the slot's content answers
				if (!slotVisible(aSlot) || !canExtractItem(tCoverSide, aSlot, tInventory.getStackInSlot(aSlot))) return ItemStack.EMPTY;
				return tInventory.extractItem(aSlot, aAmount, aSimulate);
			}

			/** The :344-347 slot-visibility gate — only the cover's accessible slots when it claims the override. */
			private boolean slotVisible(int aSlot) {
				int[] tAccessible = getAccessibleSlotsFromSide(tCoverSide, tAllSlots);
				for (int tVisible : tAccessible) if (tVisible == aSlot) return true;
				return false;
			}
		};
	}

	// ---------------------------------------------------------------------------
	// upstream UT.Code.units (UT.java:1677-1683) — the root UT port does not carry it yet
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
