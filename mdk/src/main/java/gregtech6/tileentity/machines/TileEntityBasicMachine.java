package gregtech6.tileentity.machines;

import java.util.function.Supplier;

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

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.util.UT;
import gregtech6.block.GTBasicMachineBlock;
import gregtech6.gui.machines.GTBasicMachineMenu;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;
import gregtech6.tileentity.GTItemStackHandler;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The shared single-block machine base — direct translation of upstream
 * gregapi/tileentity/machines/MultiTileEntityBasicMachine.java trimmed to the Shredder/
 * Crusher/Lathe shape (task p7-basicmachine-family; registration rows
 * Loader_MultiTileEntities.java:1294-1309, T1 tier = the field defaults kept here).
 * TileEntityOven is the Furnace-shape sibling cut of the same source and stays UNTOUCHED
 * (P6 ADR precedent: no retroactive re-parenting).
 *
 * <p>Field translation (:92-109): mEnergy/mInputMin/mInput/mInputMax = 0/16/32/64 (:98 T1
 * defaults), mProgress/mMaxProgress (:108), mSuccessful/mActive/mRunning (:109), mStopped/
 * mNoConstantEnergy/mCouldUseRecipe (:92), mIgnited (:93, the post-action re-check window
 * — see the field doc), mLastRecipe/mCurrentRecipe (:100), mOutputItems (:102), mParallel
 * (:97), mParallelDuration (:92), mEnergyTypeAccepted (:99, the RU/KU energy-type carrier).
 * The IIconContainer texture sets (:104) become the BlockState ACTIVE/RUNNING properties
 * (upstream getVisualData :1010-1011 = the same two bits); the fluid tanks, side masks,
 * mMode/mOutputBlocked and the fluid auto-IO are cut with their subsystems. Upstream
 * injects mRecipes through the NBT_RECIPEMAP string (getDefaultInventory :525) — the port
 * injects it through the constructor (the registration rows are compile-time constants).
 *
 * <p>Tick business (trimmed verbatim):
 * <ul>
 * <li>{@link #doWork(long)} :780-793 verbatim (single-block {@code checkStructure} :962-964
 *     folded to true; the :792 ignition decrement stays);</li>
 * <li>{@link #doActive(long, long)} :795-887 with the carryover block :843-851 verbatim —
 *     mProgress += min(mInputMax, mEnergy) :813 (the progress unit IS an energy unit),
 *     item output placement wraps i % mOutputItemsCount :816; the :815 gate is the
 *     upstream form VERBATIM (restored p8-machine-tiers-doinject ③, the p11 unwound
 *     fold) —
 *     {@code mStateOld && !mStateNew || !ALL_ALTERNATING.contains(mEnergyTypeAccepted)} with
 *     the :865 {@code mStateOld = mStateNew} shift: the KU/Crusher machine delivers its
 *     outputs only on the injection positive→non-positive transition tick, the AC
 *     half-cycle of the upstream Steam Engine :146 ±alternation (the live source is the
 *     /gt6energy alternating rig); the fluid placement
 *     (:817-835) and the neighbor auto-push
 *     blocks (:853-858/:867-884) are cut (no logistics surface — outputs stay in the slots,
 *     which keeps the upstream canOutput blockage);</li>
 * <li>{@link #checkRecipe(boolean, boolean)} :683-778 with the doInputItems auto-IO (:687)
 *     cut but the PARALLEL blocks (:742-745) RESTORED (the oven cut them with mParallel=1;
 *     the Crusher registers NBT_PARALLEL 4 at :1300) plus the energy math :761-774 verbatim:
 *     mParallelDuration → :766-768 (mMaxProgress × parallel count = linear duration), else
 *     :770-771 (mMinEnergy × parallel count = the speedup; the TU half keeps its constant
 *     per-process energy), then the overclock loop :773 verbatim (4x energy, 2x speed);</li>
 * <li>{@link #canOutput(Recipe)} :620-668 keeps the output-slot blockage semantics and the
 *     parallelDuration chain-processing power cap :626-629, returns the parallel count;
 *     the doOutputItems pre-push :623 and the fluid-tank branch :650-666 are cut;</li>
 * <li>{@link #doInactive(long)} :889-900 — sound interrupt and neighbor push cut,
 *     CONSTANT_ENERGY reset verbatim.</li>
 * </ul>
 *
 * <p>Energy (ADR ruling 2026-08-31-p7-machine-family ④ / 2026-08-31-p7-energy-network D1,
 * closed out by task p8-machine-tiers-doinject, the DEFAULT flipped by task
 * p11-rotor-source-flip): BOTH options stay live behind the {@link #ENERGY_FAKE_SOURCE}
 * regime switch —
 * <table>
 * <tr><th>ENERGY_FAKE_SOURCE</th><th>supply path</th><th>the :815 alternating arm</th></tr>
 * <tr><td>{@code false} (default, the upstream truth)</td><td>the ITileEntityEnergy network
 *     surface only ({@link #doInject} through the Root gate — oven :81-83 shape); the
 *     RU/KU carriers are fed by the rotor-family source rig ({@code /gt6energy type|alt},
 *     the upstream EngineSteam :146 ±alternation form)</td><td>full upstream semantics
 *     (KU outputs only on the transition tick; RU/Lathe/Shredder output on every
 *     completed tick — TD.java:216 ALL_ALTERNATING = (F, KU))</td></tr>
 * <tr><td>{@code true} (the retired M1 port-ism, kept as the offline/regression seam —
 *     the oven :134 switch same shape)</td><td>the A-tier fake source via
 *     {@link #supplyEnergy()} (oven :74-77 semantics verbatim: refill mEnergy = mInputMax
 *     every tick while not stopped; doWork drains mInputMax :791)</td><td>SUSPENDED for the
 *     whole family — the pre-p8 behavior</td></tr>
 * </table>
 * The TRUE default was an M1 port-ism ruling (ADR 2026-08-31-p8-machine-closeout ②(d)):
 * the upstream :501 type-equality gate kept the then-EU-only network out of the RU/KU
 * machines, so FALSE-by-default would have made all three families dead blocks. Task
 * p11-rotor-source-flip shipped the parameterised RU/KU ±alternating source
 * (GTEnergySourceBlockEntity, upstream EngineSteam :146 form) and retired the port-ism:
 * the default is the upstream truth again and the :815 suspension fold is unwound.
 * {@link #doInject} is the upstream :489-508 body minus the charging branch (:497-500,
 * mEnergyTypeCharged/mChargeRequirement are outside the trimmed field set — declared
 * deviation, the oven :492 same shape); the :511 FACE_CONNECTED side mask is not ported
 * (side-gated IO pool) and the receiving gate eats the Root all-sides default. Side
 * configuration and the RU/KU accepted-energy types are carrier fields; no named energy
 * interface is introduced (IEnergyPolicy ownership is D1's).
 *
 * <p>Recipe consumption follows the p4-recipe-core pinned contract: findRecipe only LOOKS UP,
 * consuming is {@code Recipe.isRecipeInputEqual(true, false, fluids, inputs)} (:725/:738
 * two-stage) and the parallel rebalance consumes count-1 (:744).
 *
 * <p>Slots are data-driven from the RecipeMap (getDefaultInventory :524-530): 1 input slot +
 * {@code mRecipes.mOutputItemsCount} output slots — Shredder/Crusher 1+12, Lathe 1+2
 * (RM.java:134/:135/:97). The capability surface is the P6 gated handler: insert only the
 * input slot (canInsertItem2 :549-554 trimmed), extract only the output slots
 * (canExtractItem2 :556-559).
 *
 * <p>Sync: FACING/ACTIVE/RUNNING live on the BlockState (the getVisualData :1010-1011
 * two-bit payload), applied in {@link #onTickChecked(long)} through the vanilla furnace
 * idiom (setBlock(state, 3) — same-block state changes keep the BE). Facing is
 * double-written NBT + BlockState (P4 spec 7): NBT is the persistent authority.
 */
public class TileEntityBasicMachine extends TileEntityBase03TicksAndSync implements MenuProvider {

	// checkRecipe result codes (upstream :672-675 verbatim).
	public static final int DID_NOT_FIND_RECIPE = 0;
	public static final int FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS = 1;
	public static final int FOUND_AND_SUCCESSFULLY_USED_RECIPE = 2;
	public static final int FOUND_AND_COULD_HAVE_USED_RECIPE = 3;

	/** GT_API.java:510 CONSTANT_ENERGY default (config field of the same name upstream). */
	public static final boolean CONSTANT_ENERGY = true;

	/**
	 * The energy regime switch (task p8-machine-tiers-doinject ④, the oven p8-d3 §③ shape;
	 * DEFAULT FLIPPED to false by task p11-rotor-source-flip). {@code false} (shipped
	 * default, the upstream truth — the switch itself is a pure port invention with ZERO
	 * upstream hits) = grid-fed only through {@link #doInject} and the :815 gate runs the
	 * full upstream semantics; the rotor-family source rig (the /gt6energy type|alt dials,
	 * the upstream EngineSteam :146 ±alternation form) feeds the RU/KU carriers. {@code
	 * true} = the A-tier fake source feeds the family through {@link #supplyEnergy()} AND
	 * the :815 alternating arm is suspended — the retired M1 port-ism, kept ONLY as the
	 * offline/regression seam (the oven :134 switch, default false there since p8-d3).
	 */
	public static boolean ENERGY_FAKE_SOURCE = false;

	// NBT keys — plain in-repo form (the oven precedent, TileEntityOven.java:111-121).
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
	/** Upstream NBT_STATE+".new" (:119 read / :233 write) — only the .new half persists; mStateOld is re-derived on the first active tick (:865). */
	public static final String NBT_STATE = "state";

	/** Content slot 0 is the input (upstream :81 RecipeMap order: inputs, then outputs). */
	public static final int SLOT_INPUT = 0;

	// fields :92-109 (trimmed set, see class doc)
	public long mEnergy = 0, mInputMin = 16, mInput = 32, mInputMax = 64, mMinEnergy = 0; // :98
	public long mProgress = 0, mMaxProgress = 0; // :108
	public boolean mSuccessful = false, mActive = false, mRunning = false; // :109
	public boolean mStopped = false, mNoConstantEnergy = false, mCouldUseRecipe = false, mInventoryChanged = false; // :92 + :109-adjacent
	/**
	 * Upstream :93 mIgnited — NOT the ignition-required feature (mRequiresIgnition, cut —
	 * the three machines register no NEEDS_IGNITION) but the post-action re-check window:
	 * every successful output placement sets it to 40 (:816/:851) and doWork decrements it
	 * (:792), which keeps the doActive recipe re-check (:800) firing after each completion —
	 * mInventoryChanged alone cannot (onTickResetChecks clears it every tick end), so without
	 * this counter the carryover :843 would be wiped by the :803 reset before the next
	 * process could start (the oven lesson, TileEntityOven.java:137-145).
	 */
	public byte mIgnited = 0;
	/** Upstream :99 — the accepted-energy carrier (Shredder/Lathe = TD.Energy.RU, Crusher = TD.Energy.KU, :1294/:1300/:1306). */
	public TagData mEnergyTypeAccepted = TD.Energy.TU;
	/**
	 * Upstream :92 mStateNew/mStateOld — the alternating-injection latch pair. mStateNew is
	 * written by {@link #doInject} :502 (the sign of the last packet: positive → true); the
	 * :865 shift moves it into mStateOld at the end of every active tick, so the :815 arm
	 * {@code mStateOld && !mStateNew} fires exactly on the injection positive→non-positive
	 * transition (the AC half-cycle; the upstream Steam Engine :146 alternates the packet
	 * sign by piston phase). Only mStateNew persists (NBT_STATE+".new"); mStateOld
	 * re-derives on the first active tick after a load. The retired
	 * ENERGY_FAKE_SOURCE=TRUE regime suspends the :815 arm's consult (the writes stay
	 * verbatim); the shipped FALSE default runs this pair at full upstream semantics.
	 */
	public boolean mStateNew = false, mStateOld = false;
	/** Upstream :97 — the parallel process cap (Crusher NBT_PARALLEL 4, :1300). */
	public final int mParallel;
	/** Upstream :92 — parallelDuration: T = the duration scales with the parallel count, F = the energy does. */
	public final boolean mParallelDuration;

	/** Upstream :100. */
	public Recipe mLastRecipe = null, mCurrentRecipe = null;
	/** Upstream :102 — the pending outputs of the current process. */
	public ItemStack[] mOutputItems = new ItemStack[0];
	/** Upstream :107 — constructor-injected (the NBT_RECIPEMAP :525 decoupling is a compile-time-constant registration here). */
	public final RecipeMap mRecipes;

	/** Upstream :92 mFacing (byte, GT6 side order == Direction.getIndex()) — oven precedent. */
	protected byte mFacing = 2;
	protected boolean oActive = false, oRunning = false; // :92

	/** Port-owned: which MenuType opens this machine's GUI (one per registered machine). */
	private final Supplier<MenuType<GTBasicMachineMenu>> mMenuType;

	// capability handle (P6 gated-item-handler precedent)
	private final LazyOptional<IItemHandler> mGatedCap = LazyOptional.of(this::newGatedHandler);

	/**
	 * Full constructor — the BET factory entry (Builder.of(...).build(null) works without a
	 * registry, the oven test-fixture precedent). {@code aMenuType} may be null for offline
	 * fixtures (createMenu then throws — menus are a live-server surface).
	 */
	public TileEntityBasicMachine(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState,
			RecipeMap aRecipes, int aParallel, boolean aParallelDuration, @Nullable Supplier<MenuType<GTBasicMachineMenu>> aMenuType) {
		// the machine ticks: the upstream MTE sits on the 03 ticking chain (mIsTicking = true)
		super(true, aType, aPos, aState);
		if (aRecipes == null) throw new IllegalArgumentException("TileEntityBasicMachine requires a RecipeMap (upstream :107/NBT_RECIPEMAP :525)");
		mRecipes = aRecipes;
		mParallel = Math.max(1, aParallel); // upstream NBT_PARALLEL clamp :130
		mParallelDuration = aParallelDuration;
		mMenuType = aMenuType;
		// getDefaultInventory :524-530 — 1 input + mOutputItemsCount outputs (no special/fluid
		// display slots in the port slot shape; the SHCL rows carry mSpecialValue 0)
		setInventory(new GTItemStackHandler(mRecipes.mInputItemsCount + mRecipes.mOutputItemsCount, this::onInventoryChanged));
	}

	@Override
	public String getTileEntityName() {
		// the BET registry path mirrors the RecipeMap internal-name tail ("gt.recipe.shredder" → "shredder")
		return mRecipes.mNameInternal.substring(mRecipes.mNameInternal.lastIndexOf('.') + 1);
	}

	/** The menu binds this as the SlotItemHandler container (oven precedent). */
	public GTItemStackHandler getInventory() {
		return mInventory;
	}

	/** The slot count of the RecipeMap shape (upstream getDefaultInventory :526). */
	public int getInputSlotCount() {
		return mRecipes.mInputItemsCount;
	}

	public int getOutputSlotCount() {
		return mRecipes.mOutputItemsCount;
	}

	// ---------------------------------------------------------------------------
	// tick chain (:443-468 trimmed — the oven shape without the cover/redstone layer)
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
			// the A-tier fake power source through the D3 seam (upstream :454-455 refresh spot)
			supplyEnergy();
			doWork(aTimer);
		}
	}

	/**
	 * The energy seam (ADR 2026-08-31-p7-machine-family ④, regime-gated by task
	 * p8-machine-tiers-doinject ④): option A constant full-voltage fake power — oven :74-77
	 * semantics verbatim (refill to mInputMax every tick while not stopped; doWork :791
	 * drains exactly mInputMax, so every active tick advances progress by mInputMax energy
	 * units) — runs only while {@link #ENERGY_FAKE_SOURCE} is true. With the switch off this
	 * method is a no-op and the machine is grid-fed through {@link #doInject} exclusively.
	 * The protected seam itself stays (the regime flip must not need a tick-chain re-touch).
	 */
	protected void supplyEnergy() {
		if (ENERGY_FAKE_SOURCE && !mStopped) mEnergy = mInputMax;
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

	/** Upstream :780-793 verbatim (checkStructure folded, ignition feature cut with the window kept). */
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

	/** Upstream :795-887 — progress = energy units (:813), output wrap i % outCount (:816), carryover (:843-851). */
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
			// :815 upstream verbatim (the p8 suspension fold UNWOUND by task
			// p11-rotor-source-flip — with ENERGY_FAKE_SOURCE retired to false this is the
			// only live shape; upstream MultiTileEntityBasicMachine.java:815):
			// `mStateOld && !mStateNew || !ALL_ALTERNATING.contains(mEnergyTypeAccepted)`
			// KU is an ALL_ALTERNATING member (root TD.java:216 = (F, KU)) and delivers its
			// outputs only on the injection positive→non-positive transition tick (the AC
			// half-cycle of the upstream Steam Engine :146 ±alternation); RU/Lathe/Shredder
			// output on every completed tick.
			if (mProgress >= mMaxProgress && (mStateOld && !mStateNew || !TD.Energy.ALL_ALTERNATING.contains(mEnergyTypeAccepted))) {
				// :816 — outputs wrap around the output slot range: i % mOutputItemsCount
				for (int i = 0; i < mOutputItems.length; i++) if (mOutputItems[i] != null && addStackToSlot(mRecipes.mInputItemsCount + (i % mRecipes.mOutputItemsCount), mOutputItems[i])) {
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
					mProgress -= mMaxProgress; // :843 verbatim
					mMinEnergy = 0;
					mMaxProgress = 0;
					mOutputItems = new ItemStack[0];
					mSuccessful = true;
					mIgnited = 40; // :851
					// :853-858 neighbor auto-push cut (no logistics surface)
					onProcessFinished();
				}
			}
		}

		// :865 RESTORED — the alternating latch shift at the end of every active tick (the
		// NO-reset form is load-bearing: the transition arm needs mStateNew to PERSIST the
		// last packet's sign across ticks, so mStateOld && !mStateNew can compare them)
		mStateOld = mStateNew;

		// :867-884 output auto-push cut (no logistics surface)
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
	// recipe check (:683-778 — the parallel blocks RESTORED)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream :683-778 — doInputItems auto-IO (:687) cut; the parallel blocks (:742-745)
	 * restored (the oven cut them at mParallel = 1; the Crusher runs 4); the energy math
	 * :761-774 verbatim with the RF halves of :767/:770 cut (no RF conversion in the port
	 * constants). {@code aApplyRecipe=false} probes, {@code true} consumes (the two-stage
	 * isRecipeInputEqual contract).
	 */
	public int checkRecipe(boolean aApplyRecipe, boolean aUseAutoIO) {
		mCouldUseRecipe = false; // :684
		if (mRecipes == null) return DID_NOT_FIND_RECIPE; // :685

		int tInputItemsCount = 0; // :689-694 (no fluid tanks in the machine slot shape)
		ItemStack[] tInputs = new ItemStack[mRecipes.mInputItemsCount];
		for (int i = 0; i < mRecipes.mInputItemsCount; i++) {
			tInputs[i] = slot(i);
			if (tInputs[i] != null && !tInputs[i].isEmpty()) tInputItemsCount++;
		}

		// :696-706 fluid auto-input and tank counting cut (SHCL maps: mMinimalInputFluids = 0)
		if (tInputItemsCount < mRecipes.mMinimalInputItems) return DID_NOT_FIND_RECIPE; // :708
		if (tInputItemsCount < mRecipes.mMinimalInputs) return DID_NOT_FIND_RECIPE; // :710

		// :712 — mInputMax is the voltage (the RF / RF_PER_EU half is cut); the special slot
		// content is EMPTY (the port slot shape has no special slot and the SHCL rows carry
		// mSpecialValue 0 — upstream slot(mInputItemsCount + mOutputItemsCount)).
		Recipe tRecipe = mRecipes.findRecipe(mLastRecipe, mInputMax, ItemStack.EMPTY, null, tInputs);

		int tMaxProcessCount = 0; // :714

		// :716-732 the mCanUseOutputTanks output-tank fallback is cut (no tanks); the found
		// branch :733-746 below is the merged verbatim shape
		if (tRecipe == null) return DID_NOT_FIND_RECIPE; // :719 shape

		if (tRecipe.mCanBeBuffered) mLastRecipe = tRecipe; // :734
		tMaxProcessCount = canOutput(tRecipe); // :735
		if (tMaxProcessCount <= 0) return FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS; // :736

		// :737 — the ignition half (mRequiresIgnition || mIgnited > 0 || mActive) is cut
		if (!tRecipe.isRecipeInputEqual(aApplyRecipe, false, null, tInputs)) return FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS; // :738
		mCouldUseRecipe = true; // :739
		if (!aApplyRecipe) return FOUND_AND_COULD_HAVE_USED_RECIPE; // :740

		if (tMaxProcessCount > 1) { // :742-745 RESTORED (oven cut this; Crusher NBT_PARALLEL 4)
			if (!mParallelDuration && mEnergyTypeAccepted != TD.Energy.TU) { // :743 (RF half cut)
				// UT.Code.bind(aMin, aMax, aBoundValue): the per-tick energy budget caps the count
				tMaxProcessCount = (int)UT.Code.bind(1, tMaxProcessCount, mInput / Math.max(1, tRecipe.mEUt));
			}
			// :744 — 1 + the COUNT consume form (upstream Recipe.isRecipeInputEqual(int, ...) :824;
			// the P4 Recipe shell is frozen, so the count loop is the local helper below)
			tMaxProcessCount = 1 + isRecipeInputEqual(tRecipe, tMaxProcessCount - 1, tInputs);
		}

		// :748-753 adjacent-inventory notify and :755 mSpecialIsStartEnergy cut (auto-IO pool)

		mCurrentRecipe = tRecipe; // :757
		mOutputItems = tRecipe.getOutputs(tMaxProcessCount); // :758
		// :759 fluid outputs cut with the output tanks

		if (tRecipe.mEUt < 0) { // :761-764 — generator rows (no mOutputEnergy in the port field set)
			mMaxProgress = tRecipe.mDuration;
			mMinEnergy = 0;
		} else {
			if (mParallelDuration) { // :766-768 — the duration scales linearly with the parallel count
				mMinEnergy = Math.max(1, tRecipe.mEUt); // :767 (RF half cut)
				mMaxProgress = Math.max(1, units(mMinEnergy * Math.max(1, tRecipe.mDuration) * tMaxProcessCount, 10000, 10000, true)); // :768 (mEfficiency = 10000 → units() is the identity)
			} else { // :770-771 — the energy scales (the speedup); TU keeps its constant per-process energy
				mMinEnergy = Math.max(1, (mEnergyTypeAccepted == TD.Energy.TU ? tRecipe.mEUt : tRecipe.mEUt * tMaxProcessCount));
				mMaxProgress = Math.max(1, units(mMinEnergy * Math.max(1, tRecipe.mDuration), 10000, 10000, true)); // :771
			}
			// :773 verbatim — 4x energy, 2x speed (mCheapOverclocking cut: no config source, always T)
			while (mMinEnergy < mInputMin && mMinEnergy * 4 <= mInputMax) {mMinEnergy *= 4; mMaxProgress *= 2;}
		}

		removeEmptyInputStacks(); // :776 removeAllDroppableNullStacks shape
		return FOUND_AND_SUCCESSFULLY_USED_RECIPE; // :777
	}

	/**
	 * Upstream Recipe.isRecipeInputEqual(int aMaxProcessCount, IFluidTank[], ItemStack...)
	 * (:824-846) — the count form of the consume path, trimmed to the item-only shape (the
	 * P4 Recipe shell is frozen so the overload lands here). Probes one recipe-worth per
	 * iteration and consumes it, returning the count actually consumed; a fluid-bearing
	 * recipe with no fluid source is the upstream :826 early 0.
	 */
	private static int isRecipeInputEqual(Recipe aRecipe, int aMaxProcessCount, ItemStack... aInputs) {
		if (aMaxProcessCount <= 0) return 0; // :825
		if (aRecipe.mFluidInputs.length > 0) return 0; // :826 (no fluid source in the machine slot shape)
		if (aRecipe.mInputs.length > 0 && (aInputs == null || aInputs.length < 1)) return 0; // :827
		int rProcessCount = 0;
		while (rProcessCount < aMaxProcessCount) {
			if (!aRecipe.isRecipeInputEqual(false, false, null, aInputs)) return rProcessCount; // the checkStacksEqual(F, F, ...) probe :835
			aRecipe.isRecipeInputEqual(true, false, null, aInputs); // the checkStacksEqual(T, F, ...) consume :838
			rProcessCount++;
		}
		return rProcessCount; // :845
	}

	/**
	 * Upstream :620-668 — output-slot blockage semantics kept (equal item + capacity,
	 * mNeedsEmptyOutput; the mMode half is cut), returns the parallel count; the
	 * parallelDuration chain-processing power cap :626-629 is verbatim; the doOutputItems
	 * pre-push (:623) and the fluid-tank branch (:650-666) are cut.
	 */
	public int canOutput(Recipe aRecipe) {
		int rMaxTimes = mParallel; // :621

		// :626-629 verbatim — Don't do more than 30 to 120 Seconds worth of Input at a time,
		// when doing Chain Processing.
		if (mParallelDuration) {
			while (rMaxTimes > 1 && aRecipe.getAbsoluteTotalPower() * rMaxTimes > mInputMax * 600) rMaxTimes--;
		}

		for (int i = 0, j = mRecipes.mInputItemsCount; i < mRecipes.mOutputItemsCount && i < aRecipe.mOutputs.length; i++, j++) {
			ItemStack tOutput = aRecipe.mOutputs[i];
			if (tOutput == null || tOutput.isEmpty()) continue; // ST.valid
			ItemStack tSlot = slot(j);
			if (tSlot != null && !tSlot.isEmpty()) { // slotHas
				if (aRecipe.mNeedsEmptyOutput) return 0; // :633-636 (the mMode half is cut)
				if (!ItemStack.isSameItemSameTags(tSlot, tOutput)) return 0; // :637-640 — blocked
				rMaxTimes = Math.min(rMaxTimes, (tSlot.getMaxStackSize() - tSlot.getCount()) / tOutput.getCount()); // :641
				if (rMaxTimes <= 0) return 0; // :642-645
			} else {
				rMaxTimes = Math.min(rMaxTimes, Math.max(1, 64 / tOutput.getCount())); // :647
			}
		}
		// :650-666 fluid-output tanks cut
		return rMaxTimes; // :667
	}

	// ---------------------------------------------------------------------------
	// energy surface (task p8-machine-tiers-doinject ② — the network consumer face,
	// upstream MultiTileEntityBasicMachine :489-519)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream :489-508 verbatim minus the charging branch (:497-500 — mChargeRequirement/
	 * mEnergyTypeCharged are outside the trimmed field set :137, declared deviation, the
	 * oven :492 same shape): a stopped machine refuses (0, :490); an over-voltage packet
	 * overcharges ({@code aSize > mInputMax}) and reports the whole amount as used
	 * (:493-495, the Root overcharge/explode body — D3); an accepted-type packet charges
	 * {@code min(mInputMax - mEnergy, size * amount)} energy, consuming the corresponding
	 * packet count with the rounding-up remainder (:501-505) and latching mStateNew to the
	 * packet sign (:502 — the alternating half-cycle marker). RU/KU carrier note: the :501
	 * type-equality gate admits the accepted type ONLY — the live writers are the
	 * /gt6machine inject rig and the rotor-family source (the /gt6energy type|alt dials
	 * driving the natural tick); the retired ENERGY_FAKE_SOURCE=TRUE A-tier seam is the
	 * only non-network supply left.
	 */
	@Override
	public long doInject(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
		if (mStopped) return 0; // :490
		boolean tPositive = (aSize > 0); // :491
		aSize = Math.abs(aSize); // :492
		if (aSize > getEnergySizeInputMax(aEnergyType, aSide)) { // :493
			if (aDoInject) overcharge(aSize, aEnergyType); // :494 — the Root D3 body (suspend/explode + log)
			return aAmount; // :495
		}
		// :497-500 charging branch cut (mEnergyTypeCharged/mChargeRequirement, declared deviation)
		if (aEnergyType == mEnergyTypeAccepted) { // :501
			if (aDoInject) mStateNew = tPositive; // :502
			long tInput = Math.min(mInputMax - mEnergy, aSize * aAmount), tConsumed = Math.min(aAmount, (tInput/aSize) + (tInput%aSize!=0?1:0)); // :503
			if (aDoInject) mEnergy += tConsumed * aSize; // :504
			return tConsumed; // :505
		}
		return 0; // :507
	}

	/**
	 * Upstream :510 for the family shape — the RECEIVING arm only: the emitting half
	 * (mEnergyTypeEmitted) and the mEnergyTypeCharged half are cut with their subsystems
	 * (declared deviation; the Root default isEnergyEmittingTo consults this with
	 * {@code aEmitting=true} and stays false — the family never emits).
	 */
	@Override public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {return !aEmitting && aEnergyType == mEnergyTypeAccepted;}

	// :511 isEnergyAcceptingFrom NOT overridden — the FACE_CONNECTED rotation mask is the
	// side-gated IO pool item; the receiving gate eats the Root all-sides default. The
	// stopped-machine refusal lives at the :490 line above (and nothing is booked either way).

	/** Upstream :513. */
	@Override public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {return mInputMin;}

	/** Upstream :514. */
	@Override public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {return mInput;}

	/** Upstream :515 — also the overcharge threshold in {@link #doInject}. */
	@Override public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {return mInputMax;}

	/** Upstream :519 — the accepted type's AS_LIST. */
	@Override public java.util.Collection<TagData> getEnergyTypes(byte aSide) {return mEnergyTypeAccepted.AS_LIST;}

	/** Upstream :1027 verbatim minus the adjacent-source refresh (no toggleable sources). */
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
		for (int i = 0; i < mRecipes.mInputItemsCount; i++) {
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
	// capability exposure — the gating IItemHandler (insert input-only, extract output-only)
	// ---------------------------------------------------------------------------

	/**
	 * The gated item surface (P6 TileEntityBase10MultiBlockMachine shape): insert only the
	 * input slot (canInsertItem2 :549-554 trimmed to the slot-range gate — the mMode
	 * empty-slot rule, the same-item dedup and the containsInput legs are the pool), extract
	 * only the output slots (canExtractItem2 :556-559). Storage stays the plain
	 * {@link GTItemStackHandler}.
	 */
	private IItemHandler newGatedHandler() {
		GTItemStackHandler tInventory = mInventory;
		return new IItemHandler() {
			@Override public int getSlots() {return tInventory.getSlots();}
			@Override public ItemStack getStackInSlot(int aSlot) {return tInventory.getStackInSlot(aSlot);}
			@Override public int getSlotLimit(int aSlot) {return tInventory.getSlotLimit(aSlot);}
			@Override public boolean isItemValid(int aSlot, ItemStack aStack) {return canInsertItem2(aSlot);}

			@Override
			public ItemStack insertItem(int aSlot, ItemStack aStack, boolean aSimulate) {
				if (aStack == null || aStack.isEmpty() || !canInsertItem2(aSlot)) return aStack;
				return tInventory.insertItem(aSlot, aStack, aSimulate);
			}

			@Override
			public ItemStack extractItem(int aSlot, int aAmount, boolean aSimulate) {
				if (!canExtractItem2(aSlot)) return ItemStack.EMPTY;
				return tInventory.extractItem(aSlot, aAmount, aSimulate);
			}
		};
	}

	/** Upstream canInsertItem2 :549-554 trimmed to the slot range gate (insert only the input range). */
	public boolean canInsertItem2(int aSlot) {
		return aSlot < mRecipes.mInputItemsCount;
	}

	/** Upstream canExtractItem2 :556-559 verbatim (extract only the output range). */
	public boolean canExtractItem2(int aSlot) {
		return aSlot >= mRecipes.mInputItemsCount && aSlot < mRecipes.mInputItemsCount + mRecipes.mOutputItemsCount;
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
	// facing + visual state (BlockState double-write, P4 spec 7)
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
	 * The GTCEu MetaMachine.setFrontFacing :794-811 counterpart (p6-oven-rotation shape):
	 * only a horizontal side (2..5) different from the current facing rotates — the
	 * same-facing call is the :796 no-op and vertical/invalid sides are rejected.
	 *
	 * @return true when the facing actually changed.
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
		if (!(tState.getBlock() instanceof GTBasicMachineBlock)) return;
		BlockState tNew = tState
				.setValue(GTBasicMachineBlock.FACING, Direction.from3DDataValue(mFacing))
				.setValue(GTBasicMachineBlock.ACTIVE, mActive)
				.setValue(GTBasicMachineBlock.RUNNING, mRunning);
		if (tNew != tState) {
			getLevel().setBlock(getBlockPos(), tNew, 3);
		}
	}

	// ---------------------------------------------------------------------------
	// GUI (upstream getGUIClient2/getGUIServer2 :1007-1008 → MenuProvider)
	// ---------------------------------------------------------------------------

	@Override
	public AbstractContainerMenu createMenu(int aContainerId, Inventory aPlayerInventory, Player aPlayer) {
		if (mMenuType == null) throw new IllegalStateException("This TileEntityBasicMachine has no MenuType bound (offline fixture?)");
		return new GTBasicMachineMenu(mMenuType.get(), aContainerId, aPlayerInventory, this);
	}

	@Override
	public Component getDisplayName() {
		return getBlockState().getBlock().getName();
	}

	// ---------------------------------------------------------------------------
	// NBT (upstream readFromNBT2 :112-155 / writeToNBT2 shape — the oven key set; the
	// mParallel/mParallelDuration/mEnergyTypeAccepted registration config is
	// constructor/factory-injected upstream too (NBT make(...) at :1294-1309), not persisted)
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
		aNBT.putBoolean(NBT_STATE + ".new", mStateNew); // upstream NBT_STATE+".new" :233 — only .new persists; mStateOld re-derives at the first :865 shift
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
		if (aNBT.contains(NBT_STATE + ".new")) mStateNew = aNBT.getBoolean(NBT_STATE + ".new"); // :119 — mStateOld stays false, the first active tick's :865 shift re-derives it
		if (aNBT.contains(NBT_OUTPUT, Tag.TAG_LIST)) {
			ListTag tOutputs = aNBT.getList(NBT_OUTPUT, Tag.TAG_COMPOUND);
			mOutputItems = new ItemStack[tOutputs.size()];
			for (int i = 0; i < tOutputs.size(); i++) mOutputItems[i] = ItemStack.of(tOutputs.getCompound(i));
		}
	}

	// ---------------------------------------------------------------------------
	// upstream UT.Code.units (UT.java:1677-1683) — the root UT port does not carry it yet
	// (same standalone copy as the oven, TileEntityOven.java:700-711)
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
