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

//? if forge {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
//?} else {
/*import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
 *///?}
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.items.IItemHandler;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.util.UT;
import gregtech6.block.GTBasicMachineBlock;
import gregtech6.fluid.FluidTankGT;
import gregtech6.gui.machines.GTBasicMachineMenu;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;
import gregtech6.tileentity.GTItemStackHandler;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.util.GTSideTables;

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
 * (upstream getVisualData :1010-1011 = the same two bits); the fluid tanks and side masks
 * are LIVE since task p14-machine-fluid-face (:101/:95/:93 — the per-side FLUID_HANDLER
 * face and the :511 energy gate), while mMode/mOutputBlocked and the fluid auto-IO stay
 * cut with their subsystems. Upstream injects mRecipes through the NBT_RECIPEMAP string
 * (getDefaultInventory :525) — the port injects it through the constructor (the
 * registration rows are compile-time constants).
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
 *     /gt6energy alternating rig); the fluid placement (:817-835) is LIVE since task
 *     p14-machine-fluid-face (pending outputs land in mTanksOutput, containing tank
 *     first then empty — the minimal tank half; the neighbor auto-push blocks
 *     :853-858/:867-884 stay cut — no logistics surface — outputs stay in the slots and
 *     tanks, which keeps the upstream canOutput blockage);</li>
 * <li>{@link #checkRecipe(boolean, boolean)} :683-778 with the doInputItems auto-IO (:687)
 *     cut but the fluid legs LIVE since task p14-machine-fluid-face (the :706 tank census,
 *     the :709/:710 minimal-fluid gates, the :712 findRecipe tank argument and the
 *     :738/:744 consume through the tank snapshot mirror) and the PARALLEL blocks
 *     (:742-745) RESTORED (the oven cut them with mParallel=1;
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
 * deviation, the oven :492 same shape); the :511 FACE_CONNECTED receiving gate IS ported
 * since task p14-machine-fluid-face ({@link #mEnergyInputs} + the rotation lookup, default
 * 127 = the former Root all-sides behaviour bit-for-bit). Side configuration and the RU/KU
 * accepted-energy types are carrier fields; no named energy interface is introduced
 * (IEnergyPolicy ownership is D1's).
 *
 * <p>Recipe consumption follows the p4-recipe-core pinned contract: findRecipe only LOOKS UP,
 * consuming is {@code Recipe.isRecipeInputEqual(true, false, fluids, inputs)} (:725/:738
 * two-stage) and the parallel rebalance consumes count-1 (:744). Since task
 * p14-machine-fluid-face the {@code fluids} argument is the REAL input-tank snapshot
 * (upstream :712/:738 pass the tanks themselves; the frozen P4 Recipe surface takes
 * FluidStack[], so the consume mirrors the drained amounts back onto the tanks).
 *
 * <p>Slots are data-driven from the RecipeMap (getDefaultInventory :524-530): 1 input slot +
 * {@code mRecipes.mOutputItemsCount} output slots — Shredder/Crusher 1+12, Lathe 1+2
 * (RM.java:134/:135/:97). The capability surface is the P6 gated handler: insert only the
 * input slot (canInsertItem2 :549-554 trimmed), extract only the output slots
 * (canExtractItem2 :556-559), plus the p14 per-side FLUID_HANDLER over
 * {@link #mTanksInput}/{@link #mTanksOutput} (the side masks
 * {@link #mFluidInputs}/{@link #mFluidOutputs}).
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
	/** Upstream NBT_TANK — the per-tank content keys {@code tanks.in.<i>} / {@code tanks.out.<i>} (:160/:162), plain in-repo form. */
	public static final String NBT_TANK = "tanks";
	/** Upstream NBT_TANK_OUT+".<i>" (:164-165 FL.load per-index keys) — the port list form, the mOutputItems NBT_OUTPUT precedent. */
	public static final String NBT_OUTPUT_FLUIDS = "output_fluids";

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
	 * Upstream :93 — the ACCEPTING-face connectivity mask, in machine-relative side bits
	 * (bit 0 = bottom, 1 = top, 2..5 = left/front/right/back, 6 = undefined), read through
	 * the :511 rotation gate. The default 127 = every relative side connected = the
	 * pre-p14 behaviour bit-for-bit (FACE_CONNECTED[any][127] is {@code true} for relative
	 * sides 0-6). Registration rows re-point it post-construction (the carrier pattern of
	 * mParallel/mEnergyTypeAccepted — upstream NBT make(...) at :1294-1309 writes
	 * NBT_ENERGY_ACCEPTED_SIDES; the port keeps registration config constructor/factory
	 * injected and NOT persisted, the loadKeepsTheConstructorInjectedConfig contract).
	 */
	public byte mEnergyInputs = 127;
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
	/** Upstream :103 — the pending fluid outputs (filled at checkRecipe :759, placed into mTanksOutput at doActive :817-835). */
	public FluidStack[] mOutputFluids = new FluidStack[0];
	/**
	 * Upstream :101 — the recipe-input fluid tanks. Constructed at :157-160: the 1000
	 * default capacity, and the upstream {@code setCapacity(mRecipes, mParallel * 2L)}
	 * adjustable-map leg (:160 → FluidTankGT:304-343) collapses for the port — the port
	 * RecipeMap carries no mMinInputTankSizes (RecipeMap.java:38 documented omission), so
	 * the map-less upstream capacity() IS the constructed default (max(mAmount, 1000),
	 * FluidTankGT.java:340-342). NBT_TANK_CAPACITY stays a pool item. Restored on demand
	 * together with the RecipeMap map.
	 */
	public final FluidTankGT[] mTanksInput;
	/** Upstream :101/:162 — the output tanks, the no-arg default capacity (Long.MAX_VALUE upstream; the port FluidTankGT() form). */
	public final FluidTankGT[] mTanksOutput;
	/** Upstream :107 — constructor-injected (the NBT_RECIPEMAP :525 decoupling is a compile-time-constant registration here). */
	public final RecipeMap mRecipes;

	/** Upstream :92 mFacing (byte, GT6 side order == Direction.getIndex()) — oven precedent. */
	protected byte mFacing = 2;
	protected boolean oActive = false, oRunning = false; // :92

	/** Port-owned: which MenuType opens this machine's GUI (one per registered machine). */
	private final Supplier<MenuType<GTBasicMachineMenu>> mMenuType;

	// capability handle (P6 gated-item-handler precedent)
	//? if forge {
	private final LazyOptional<IItemHandler> mGatedCap = LazyOptional.of(this::newGatedHandler);
	//?} else {
	/*private IItemHandler mGatedHandler; // (1.21.1) the LazyOptional.of lazy semantics kept — created at the first query; 21.1 has no invalidation surface
	 *///?}

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
		// :157-162 — the tanks (see the field docs for the capacity rulings)
		mTanksInput = new FluidTankGT[mRecipes.mInputFluidCount];
		for (int i = 0; i < mTanksInput.length; i++) mTanksInput[i] = new FluidTankGT(1000); // :159-160 (1000 default, the map leg collapsed)
		mTanksOutput = new FluidTankGT[mRecipes.mOutputFluidCount];
		for (int i = 0; i < mTanksOutput.length; i++) mTanksOutput[i] = new FluidTankGT(); // :162 (default capacity)
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
			// :817-826 RESTORED (task p14-machine-fluid-face ④, the doActive tank-placement
			// half; the auto-push halves :853-858/:867-884 stay cut) — a pending output joins
			// a CONTAINING output tank first; the :819 updateInventory beat lives in THIS
			// loop only (upstream shape)
			for (int i = 0; i < mOutputFluids.length; i++) if (mOutputFluids[i] != null && !mOutputFluids[i].isEmpty()) for (int j = 0; j < mTanksOutput.length; j++) {
				if (mTanksOutput[j].contains(mOutputFluids[i])) {
					onInventoryChanged(); // :819 updateInventory
					mTanksOutput[j].add(mOutputFluids[i].getAmount()); // :820
					mSuccessful = true;
					mIgnited = 40;
					mOutputFluids[i] = null;
					break;
				}
			}
			// :827-835 — then an EMPTY output tank adopts the output (upstream :829 setFluid;
			// the port lands it through the add(long, FluidStack) adoption primitive)
			for (int i = 0; i < mOutputFluids.length; i++) if (mOutputFluids[i] != null && !mOutputFluids[i].isEmpty()) for (int j = 0; j < mTanksOutput.length; j++) {
				if (mTanksOutput[j].isEmpty()) {
					mTanksOutput[j].setEmpty();
					mTanksOutput[j].add(mOutputFluids[i].getAmount(), mOutputFluids[i]); // :829
					mSuccessful = true;
					mIgnited = 40;
					mOutputFluids[i] = null;
					break;
				}
			}

			if (containsSomething(mOutputItems) || containsSomething(mOutputFluids)) { // :837 verbatim
					// :837-841 — outputs blocked: park at max progress and retry the placement next tick
					mMinEnergy = 0;
					mProgress = mMaxProgress;
				} else {
				// :843-861 — all outputs placed: carry the leftover energy into the next process
				mProgress -= mMaxProgress; // :843 verbatim
				mMinEnergy = 0;
				mMaxProgress = 0;
				mOutputItems = new ItemStack[0];
				mOutputFluids = new FluidStack[0]; // :849
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

		int tInputItemsCount = 0; // :689-694
		ItemStack[] tInputs = new ItemStack[mRecipes.mInputItemsCount];
		for (int i = 0; i < mRecipes.mInputItemsCount; i++) {
			tInputs[i] = slot(i);
			if (tInputs[i] != null && !tInputs[i].isEmpty()) tInputItemsCount++;
		}

		// :696-705 fluid auto-input pull cut (auto-IO pool); :706 the input-tank census
		// RESTORED (task p14-machine-fluid-face ③)
		int tInputFluidsCount = 0;
		for (FluidTankGT tTank : mTanksInput) if (tTank.has()) tInputFluidsCount++;

		if (tInputItemsCount < mRecipes.mMinimalInputItems) return DID_NOT_FIND_RECIPE; // :708
		if (tInputFluidsCount < mRecipes.mMinimalInputFluids) return DID_NOT_FIND_RECIPE; // :709 RESTORED
		if (tInputItemsCount + tInputFluidsCount < mRecipes.mMinimalInputs) return DID_NOT_FIND_RECIPE; // :710 verbatim

		// :712 — mInputMax is the voltage (the RF / RF_PER_EU half is cut); the special slot
		// content is EMPTY (the port slot shape has no special slot and the SHCL rows carry
		// mSpecialValue 0 — upstream slot(mInputItemsCount + mOutputItemsCount)); the fluid
		// argument now carries the REAL input-tank snapshot (upstream passes mTanksInput).
		Recipe tRecipe = mRecipes.findRecipe(mLastRecipe, mInputMax, ItemStack.EMPTY, tankSnapshot(mTanksInput), tInputs);

		int tMaxProcessCount = 0; // :714

		// :716-732 the mCanUseOutputTanks output-tank fallback is cut (no tanks); the found
		// branch :733-746 below is the merged verbatim shape
		if (tRecipe == null) return DID_NOT_FIND_RECIPE; // :719 shape

		if (tRecipe.mCanBeBuffered) mLastRecipe = tRecipe; // :734
		tMaxProcessCount = canOutput(tRecipe); // :735
		if (tMaxProcessCount <= 0) return FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS; // :736

		// :737 — the ignition half (mRequiresIgnition || mIgnited > 0 || mActive) is cut.
		// :738 — the two-stage consume runs on the tank SNAPSHOT (the frozen P4 Recipe takes
		// FluidStack[], upstream Recipe.java:800 takes the IFluidTank[] directly and drains
		// it in place at :833); an applied consume mirrors the exact drained amounts onto the
		// real input tanks (the applyTankConsumption adapter, :833's tank.drain leg).
		FluidStack[] tFluids = tankSnapshot(mTanksInput);
		long[] tFluidBaseline = snapshotAmounts(mTanksInput);
		if (!tRecipe.isRecipeInputEqual(aApplyRecipe, false, tFluids, tInputs)) return FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS; // :738
		if (aApplyRecipe) applyTankConsumption(tFluids, tFluidBaseline);
		mCouldUseRecipe = true; // :739
		if (!aApplyRecipe) return FOUND_AND_COULD_HAVE_USED_RECIPE; // :740

		if (tMaxProcessCount > 1) { // :742-745 RESTORED (oven cut this; Crusher NBT_PARALLEL 4)
			if (!mParallelDuration && mEnergyTypeAccepted != TD.Energy.TU) { // :743 (RF half cut)
				// UT.Code.bind(aMin, aMax, aBoundValue): the per-tick energy budget caps the count
				tMaxProcessCount = (int)UT.Code.bind(1, tMaxProcessCount, mInput / Math.max(1, tRecipe.mEUt));
			}
			// :744 — 1 + the COUNT consume form (upstream Recipe.isRecipeInputEqual(int,
			// IFluidTank[], ...) :840-852 with the per-iteration fluid pre-check :847-851;
			// the P4 Recipe shell is frozen, so the count loop is the local helper below)
			tMaxProcessCount = 1 + isRecipeInputEqual(tRecipe, tMaxProcessCount - 1, tFluids, tInputs);
			if (aApplyRecipe) applyTankConsumption(tFluids, tFluidBaseline);
		}

		// :748-753 adjacent-inventory notify and :755 mSpecialIsStartEnergy cut (auto-IO pool)

		mCurrentRecipe = tRecipe; // :757
		mOutputItems = tRecipe.getOutputs(tMaxProcessCount); // :758
		mOutputFluids = tRecipe.getFluidOutputs(tMaxProcessCount); // :759 RESTORED (task p14-machine-fluid-face ③)

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
	 * (:840-852+) — the count form of the consume path (the P4 Recipe shell is frozen so
	 * the overload lands here). Probes one recipe-worth per iteration and consumes it,
	 * returning the count actually consumed; the per-iteration fluid availability
	 * pre-check (:847-851, amount-CHECKED unlike the findRecipe probe) runs on the
	 * snapshot array, a fluid-bearing recipe with no fluid source is the :842 early 0.
	 */
	private static int isRecipeInputEqual(Recipe aRecipe, int aMaxProcessCount, @Nullable FluidStack[] aFluids, ItemStack... aInputs) {
		if (aMaxProcessCount <= 0) return 0; // :841
		if (aRecipe.mFluidInputs.length > 0 && (aFluids == null || aFluids.length < 1)) return 0; // :842
		if (aRecipe.mInputs.length > 0 && (aInputs == null || aInputs.length < 1)) return 0; // :843
		int rProcessCount = 0;
		while (rProcessCount < aMaxProcessCount) {
			for (FluidStack tFluid : aRecipe.mFluidInputs) if (tFluid != null && !tFluid.isEmpty()) { // :847-851
				boolean temp = true;
				if (aFluids != null) for (FluidStack aFluid : aFluids) if (aFluid != null && !aFluid.isEmpty() && aFluid.isFluidEqual(tFluid) && aFluid.getAmount() >= tFluid.getAmount()) {temp = false; break;}
				if (temp) return rProcessCount;
			}
			if (!aRecipe.isRecipeInputEqual(false, false, aFluids, aInputs)) return rProcessCount; // the checkStacksEqual(F, F, ...) probe :835
			aRecipe.isRecipeInputEqual(true, false, aFluids, aInputs); // the checkStacksEqual(T, F, ...) consume :838
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

	/**
	 * Upstream :95 — the fluid-face connectivity masks, in machine-relative side bits, read
	 * through the rotation gate ({@code FACE_CONNECTED[FACING_ROTATIONS[mFacing][aSide]]
	 * [mask]}, CS.java:528/:598). The default 127 = every relative side open for both
	 * directions, the upstream field default (:95). The fluid auto-IO sides
	 * (mFluidAutoInput/mFluidAutoOutput, upstream :95 too) stay cut with the auto-IO pool.
	 */
	public byte mFluidInputs = 127, mFluidOutputs = 127;
	/**
	 * Upstream :511 VERBATIM (task p14-machine-fluid-face ②): the receiving gate is the
	 * rotated connectivity mask — {@code FACE_CONNECTED[FACING_ROTATIONS[mFacing][aSide]]
	 * [mEnergyInputs]} over the {@code aTheoretical || !mStopped} arm — ANDed with the
	 * super arm (the Root isEnergyType/isSurfaceEnergyAttachable chain,
	 * TileEntityBase01Root.java:338). The default mEnergyInputs = 127 keeps every relative
	 * side connected, so the pre-p14 all-sides behaviour is bit-for-bit (the
	 * defaultMaskAcceptsEverywhere truth table); registration rows that set the mask
	 * (upstream NBT_ENERGY_ACCEPTED_SIDES, e.g. the Dryer SBIT_D bottom-face rows,
	 * Loader_MultiTileEntities.java:1477-1480) get the exact upstream face geometry.
	 * The stopped-machine refusal of {@link #doInject} (:490) stays at the doInject line;
	 * this gate is the theoretical/mode probe the network and conductors consult.
	 */
	@Override
	public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {
		return (aTheoretical || !mStopped) && GTSideTables.faceConnected(mFacing, aSide, mEnergyInputs)
				&& super.isEnergyAcceptingFrom(aEnergyType, aSide, aTheoretical);
	}

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

	/** Upstream UT.Code.containsSomething over the pending fluids (upstream :837 second arm, 1.20.1 empty-flag aware). */
	private static boolean containsSomething(FluidStack[] aArray) {
		if (aArray == null) return false;
		for (FluidStack tFluid : aArray) if (tFluid != null && !tFluid.isEmpty()) return true;
		return false;
	}

	// ---------------------------------------------------------------------------
	// the tank↔recipe adapters (the frozen P4 Recipe takes FluidStack[], upstream
	// Recipe.isRecipeInputEqual :800/:833 drains the IFluidTank[] in place — the
	// snapshot+mirror pair is the behaviour-equal translation)
	// ---------------------------------------------------------------------------

	/** Per-tank COPIES (null = empty tank, upstream tank.getFluid() null = empty); the probe/consume runs mutate the copies, never the tanks. */
	private static FluidStack[] tankSnapshot(FluidTankGT[] aTanks) {
		FluidStack[] rSnapshot = new FluidStack[aTanks.length];
		for (int i = 0; i < aTanks.length; i++) {
			FluidStack tFluid = aTanks[i].fluid();
			if (tFluid != null && !tFluid.isEmpty()) rSnapshot[i] = new FluidStack(tFluid, FluidTankGT.bindInt(aTanks[i].amount()));
		}
		return rSnapshot;
	}

	private static long[] snapshotAmounts(FluidTankGT[] aTanks) {
		long[] rAmounts = new long[aTanks.length];
		for (int i = 0; i < aTanks.length; i++) rAmounts[i] = aTanks[i].amount();
		return rAmounts;
	}

	/**
	 * The :833 tank.drain mirror: drains from each tank exactly what the consume step
	 * removed from its snapshot copy (the first-matching-entry semantics of
	 * Recipe.java:250 align 1:1 with the upstream tank order), then re-baselines the
	 * amounts array so a second call after the :744 count loop drains only the delta.
	 */
	private void applyTankConsumption(FluidStack[] aSnapshot, long[] aBaseline) {
		boolean tChanged = false;
		for (int i = 0; i < mTanksInput.length && i < aSnapshot.length; i++) {
			if (aBaseline[i] <= 0) continue;
			long tNow = (aSnapshot[i] == null || aSnapshot[i].isEmpty()) ? 0 : aSnapshot[i].getAmount();
			long tConsumed = aBaseline[i] - tNow;
			if (tConsumed > 0) {
				mTanksInput[i].remove(tConsumed);
				tChanged = true;
			}
			aBaseline[i] = tNow;
		}
		if (tChanged) onInventoryChanged(); // the upstream updateInventory beat (05Inventories.java:103)
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
	// fluid tank face (upstream :561-597, task p14-machine-fluid-face ⑤)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream getFluidTankFillable2 :564-571 — containing input tank first, then an empty
	 * one. The :565 auto-output-face refuse leg is cut with the auto-IO pool
	 * (mFluidAutoOutput is out of the field set), and the :568 containsInput recipe filter
	 * is cut with its pool item — any fluid may claim an EMPTY input tank (declared
	 * deviation, containsInput is the same TODO.md pool row as the auto-IO).
	 */
	@Nullable
	public FluidTankGT getFluidTankFillable(byte aWorldSide, FluidStack aFluidToFill) {
		if (!GTSideTables.faceConnected(mFacing, aWorldSide, mFluidInputs)) return null; // :566
		for (int i = 0; i < mTanksInput.length; i++) if (mTanksInput[i].contains(aFluidToFill)) return mTanksInput[i]; // :567
		for (int i = 0; i < mTanksInput.length; i++) if (mTanksInput[i].isEmpty()) return mTanksInput[i]; // :569
		return null;
	}

	/**
	 * Upstream getFluidTankDrainable2 :574-582 — containing output tank for a named
	 * resource, any non-empty one for the resource-less form. The :577 SERVER_TIME
	 * round-robin degrades to first-has order (no server-time port; zero return for
	 * single-tank maps — declared deviation, restored with the SERVER_TIME carrier).
	 */
	@Nullable
	public FluidTankGT getFluidTankDrainable(byte aWorldSide, @Nullable FluidStack aFluidToDrain) {
		if (!GTSideTables.faceConnected(mFacing, aWorldSide, mFluidOutputs)) return null; // :575
		return drainableOutput(aFluidToDrain);
	}

	/** The side-less all-open drain body — the side-less query has no upstream form and follows the P5 barrel ruling (GTBarrelCommand.java:106/:165). */
	@Nullable
	public FluidTankGT getFluidTankDrainableAny(@Nullable FluidStack aFluidToDrain) {
		return drainableOutput(aFluidToDrain);
	}

	@Nullable
	private FluidTankGT drainableOutput(@Nullable FluidStack aFluidToDrain) {
		if (aFluidToDrain == null) {
			for (int i = 0; i < mTanksOutput.length; i++) if (mTanksOutput[i].has()) return mTanksOutput[i]; // :576-577
		} else {
			for (int i = 0; i < mTanksOutput.length; i++) if (mTanksOutput[i].contains(aFluidToDrain)) return mTanksOutput[i]; // :579
		}
		return null;
	}

	/** The static fill rule (the MultiBlockFluidHandler.fillAllowedBySide seam shape): the side-less probe is refused, a world face reads the rotated mask. */
	public static boolean fillAllowedBySide(byte aFacing, @Nullable Direction aSide, byte aMask) {
		return aSide != null && GTSideTables.faceConnected(aFacing, (byte)aSide.get3DDataValue(), aMask);
	}

	/** The static drain rule: the side-less probe is all-open (the P5 ruling), a world face reads the rotated mask. */
	public static boolean drainAllowedBySide(byte aFacing, @Nullable Direction aSide, byte aMask) {
		return aSide == null || GTSideTables.faceConnected(aFacing, (byte)aSide.get3DDataValue(), aMask);
	}

	/** The executed tank-IO dirty mark (the upstream updateInventory beat, 05Inventories.java:103 — the tapDrain :919 mirror). */
	protected void onFluidIO() {
		onInventoryChanged();
	}

	// ---------------------------------------------------------------------------
	// capability exposure — the gating IItemHandler (insert input-only, extract output-only)
	// plus the per-side fluid handler (task p14-machine-fluid-face ⑤)
	// ---------------------------------------------------------------------------

	/**
	 * The per-side fluid surface (the P4 SideFluidHandler form over the machine: the
	 * 1.20.1 IFluidHandler carries no Direction, so the side travels through the
	 * {@code getCapability(FLUID_HANDLER, Direction)} wrapper). fill routes through
	 * {@link #getFluidTankFillable} (the mFluidInputs rotation mask), drain through
	 * {@link #getFluidTankDrainable} (mFluidOutputs); the side-less query drains
	 * all-open and refuses fill (the P5 barrel ruling). The tank VIEW
	 * (getTanks/getFluidInTank) is side-blind — the p13-boiler-tank lesson: the pipe
	 * canConnect handshake probes {@code handler.getTanks() > 0} on the neighbor's face
	 * (GTFluidPipeBlockEntity.java:281-287) and a masked-to-zero view would dead-end it.
	 */
	private static final class BasicMachineFluidHandler implements IFluidHandler {
		private final TileEntityBasicMachine mMachine;
		/** The face this handler fronts; null = the side-less query. */
		@Nullable
		private final Direction mSide;

		BasicMachineFluidHandler(TileEntityBasicMachine aMachine, @Nullable Direction aSide) {
			mMachine = aMachine;
			mSide = aSide;
		}

		private int tankCount() {
			return mMachine.mTanksInput.length + mMachine.mTanksOutput.length;
		}

		@Nullable
		private FluidTankGT tank(int aTank) {
			if (aTank < 0 || aTank >= tankCount()) return null;
			return aTank < mMachine.mTanksInput.length
					? mMachine.mTanksInput[aTank]
					: mMachine.mTanksOutput[aTank - mMachine.mTanksInput.length];
		}

		@Override
		public int getTanks() {
			return tankCount();
		}

		@Override
		public FluidStack getFluidInTank(int aTank) {
			FluidTankGT tTank = tank(aTank);
			FluidStack tFluid = tTank == null ? null : tTank.fluid();
			return tFluid == null ? FluidStack.EMPTY : tFluid;
		}

		@Override
		public int getTankCapacity(int aTank) {
			FluidTankGT tTank = tank(aTank);
			return tTank == null ? 0 : tTank.getCapacity();
		}

		@Override
		public boolean isFluidValid(int aTank, FluidStack aStack) {
			FluidTankGT tTank = tank(aTank);
			return tTank != null && tTank.isFluidValid(aStack);
		}

		@Override
		public int fill(FluidStack aResource, FluidAction aAction) {
			if (aResource == null || aResource.isEmpty()) return 0;
			if (!fillAllowedBySide(mMachine.mFacing, mSide, mMachine.mFluidInputs)) return 0; // :566 via the static seam
			FluidTankGT tTank = mMachine.getFluidTankFillable((byte)mSide.get3DDataValue(), aResource); // :567/:569
			if (tTank == null) return 0;
			int rFilled = tTank.fill(aResource, aAction);
			if (aAction.execute() && rFilled > 0) mMachine.onFluidIO();
			return rFilled;
		}

		@Override
		public FluidStack drain(FluidStack aResource, FluidAction aAction) {
			if (aResource == null || aResource.isEmpty()) return FluidStack.EMPTY;
			FluidTankGT tTank = drainable(aResource); // :579 containing tank
			if (tTank == null) return FluidStack.EMPTY;
			FluidStack rFluid = tTank.drain(FluidTankGT.bindInt(Math.min(tTank.amount(), aResource.getAmount())), aAction);
			if (aAction.execute() && rFluid != null && !rFluid.isEmpty()) mMachine.onFluidIO(); // the tapDrain :919 mirror
			return rFluid;
		}

		@Override
		public FluidStack drain(int aMaxDrain, FluidAction aAction) {
			if (aMaxDrain <= 0) return FluidStack.EMPTY;
			FluidTankGT tTank = drainable(null); // :576-577 resource-less arm
			if (tTank == null) return FluidStack.EMPTY;
			FluidStack rFluid = tTank.drain(FluidTankGT.bindInt(Math.min(tTank.amount(), aMaxDrain)), aAction);
			if (aAction.execute() && rFluid != null && !rFluid.isEmpty()) mMachine.onFluidIO();
			return rFluid;
		}

		@Nullable
		private FluidTankGT drainable(@Nullable FluidStack aResource) {
			if (mSide == null) return mMachine.getFluidTankDrainableAny(aResource);
			return mMachine.getFluidTankDrainable((byte)mSide.get3DDataValue(), aResource);
		}
	}

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

	/**
	 * The per-call wrapper factory — the {@code getCapability(FLUID_HANDLER, aSide)} seam.
	 * Package-private so the offline tests drive the wrapper directly (the ForgeCapabilities
	 * tokens are transformer-resolved and unresolvable offline, the
	 * GT6MultiBlockFluidTest direct-construction precedent).
	 */
	IFluidHandler newFluidHandler(@Nullable Direction aSide) {
		return new BasicMachineFluidHandler(this, aSide);
	}

	//? if forge {
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> aCapability, @Nullable Direction aSide) {
		if (aCapability == ForgeCapabilities.ITEM_HANDLER) {
			return mGatedCap.cast(); // the gated surface shadows the root's raw inventory exposure
		}
		if (aCapability == ForgeCapabilities.FLUID_HANDLER) {
			// the fresh-wrapper-per-call form (TileEntityBase10MultiBlockMachine:681-686): a
			// stored LazyOptional would memoize the wrapper and freeze the FIRST-queried side
			// into the stateless per-side handler
			return LazyOptional.of(() -> newFluidHandler(aSide)).cast();
		}
		return super.getCapability(aCapability, aSide);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		mGatedCap.invalidate();
	}
	//?} else {
	/*// (1.21.1 seam: NeoForge 21.1 removed BlockEntity#getCapability/LazyOptional — this member
	// is the provider seam; W4's RegisterCapabilitiesEvent.registerBlockEntity delegates to it.
	// No @Override: the parent method does not exist on 21.1.)
	public <T> T getCapability(BlockCapability<T, Direction> aCapability, @Nullable Direction aSide) {
		if (aCapability == Capabilities.ItemHandler.BLOCK) {
			if (mGatedHandler == null) mGatedHandler = newGatedHandler();
			return (T) mGatedHandler; // the gated surface shadows the root's raw inventory exposure
		}
		if (aCapability == Capabilities.FluidHandler.BLOCK) {
			// the fresh-wrapper-per-call form: the side is part of the handler identity
			return (T) newFluidHandler(aSide);
		}
		return null;
	}
	 *///?}

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
		for (int i = 0; i < mTanksInput.length; i++) mTanksInput[i].writeToNBT(aNBT, NBT_TANK + ".in." + i); // :160
		for (int i = 0; i < mTanksOutput.length; i++) mTanksOutput[i].writeToNBT(aNBT, NBT_TANK + ".out." + i); // :162
		ListTag tOutputFluids = new ListTag();
		for (FluidStack tFluid : mOutputFluids) if (tFluid != null && !tFluid.isEmpty()) tOutputFluids.add(tFluid.writeToNBT(new CompoundTag()));
		aNBT.put(NBT_OUTPUT_FLUIDS, tOutputFluids); // upstream NBT_TANK_OUT.i :164-165 (list form)
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
		// :160/:162 — the tank CONTENT loads into the constructor-sized tanks (the capacity
		// is the constructor's 1000/default ruling, never NBT-driven — NBT_TANK_CAPACITY is
		// the pool item)
		for (int i = 0; i < mTanksInput.length; i++) mTanksInput[i].readFromNBT(aNBT, NBT_TANK + ".in." + i);
		for (int i = 0; i < mTanksOutput.length; i++) mTanksOutput[i].readFromNBT(aNBT, NBT_TANK + ".out." + i);
		if (aNBT.contains(NBT_OUTPUT_FLUIDS, Tag.TAG_LIST)) {
			ListTag tOutputFluids = aNBT.getList(NBT_OUTPUT_FLUIDS, Tag.TAG_COMPOUND);
			mOutputFluids = new FluidStack[tOutputFluids.size()];
			for (int i = 0; i < tOutputFluids.size(); i++) mOutputFluids[i] = FluidStack.loadFluidStackFromNBT(tOutputFluids.getCompound(i));
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
