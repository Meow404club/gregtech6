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

import gregtech6.block.GTOvenBlock;
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
 * <p>Energy (ADR-P4 ruling): option A — a constant full-voltage fake power source. Upstream
 * onTick2 :454-455 fed {@code mEnergy++} per tick (TU trickle); the port instead refills
 * {@code mEnergy = mInputMax} every tick while the machine is not stopped, and doWork drains
 * {@code mInputMax} :791, so every active tick advances progress by exactly mInputMax
 * energy units. Option C — redstone stop: a neighbor signal gates the fake source through a
 * runtime latch ({@link #mRedstoneStopped}) OR'ed at the energy gate, the
 * {@link #setStateOnOff(boolean)} :1027 shape stays on the manual NBT-persisted
 * {@link #mStopped}; the latch is separate so a falling redstone edge cannot release a
 * manual stop. Option D seam — {@link #doInject} keeps the upstream signature :489-508 as a
 * stub so the full energy net replaces the fake source without touching this class again.
 * {@code CONSTANT_ENERGY} (GT_API.java:510, default T) drives the doInactive progress reset
 * :894 verbatim.
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
 * is re-applied from it.
 */
public class TileEntityOven extends TileEntityBase03TicksAndSync implements MenuProvider {

	// checkRecipe result codes (upstream :672-675 verbatim).
	public static final int DID_NOT_FIND_RECIPE = 0;
	public static final int FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS = 1;
	public static final int FOUND_AND_SUCCESSFULLY_USED_RECIPE = 2;
	public static final int FOUND_AND_COULD_HAVE_USED_RECIPE = 3;

	/** GT_API.java:510 CONSTANT_ENERGY default (config field of the same name upstream). */
	public static final boolean CONSTANT_ENERGY = true;

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
			// upstream :446 — checkStructure(T) always passes for the single-block machine (:962-964)
			if (!mActive) checkRecipe(false, mRunning || mStopped);
		}
	}

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		if (aIsServerSide) {
			// option C: redstone stop gate (upstream :453 mBlockUpdated toggleable-source refresh spot)
			mRedstoneStopped = hasLevel() && getLevel().hasNeighborSignal(getBlockPos());
			// option A: constant full-voltage fake power (upstream :454-455 TU trickle replaced; :791 drains it below)
			if (!mStopped && !mRedstoneStopped) mEnergy = mInputMax;

			doWork(aTimer);
			// upstream :459 fluid auto-output and :463 structural re-check :465-466 display refresh are cut
		}
	}

	@Override
	public boolean onTickCheck(long aTimer) {
		// upstream :471-473 verbatim (the visual-data change pair)
		return mActive != oActive || mRunning != oRunning || super.onTickCheck(aTimer);
	}

	@Override
	public void onTickChecked(long aTimer) {
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
	// energy surface (ADR-P4)
	// ---------------------------------------------------------------------------

	/**
	 * Option D seam (ADR-P4): upstream :489-508 signature kept as a stub — the constant
	 * fake power source (option A) bypasses injection, so the full energy net (option D)
	 * replaces the fake source without touching the tick business again.
	 */
	public long doInject(gregapi.code.TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
		return 0;
	}

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
	}

	@Override
	public void load(CompoundTag aNBT) {
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
