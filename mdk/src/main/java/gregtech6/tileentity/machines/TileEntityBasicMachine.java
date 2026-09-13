package gregtech6.tileentity.machines;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.PanelSyncManager;

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
import net.minecraft.world.level.block.entity.BlockEntity;
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
import gregtech6.gui.machines.GTBasicMachineMUI;
import gregtech6.gui.machines.GTBasicMachineMenu;
import gregtech6.gui.machines.GT6MuiMachine;
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
 *     first then empty — the minimal tank half; the neighbor ITEM auto-push blocks
 *     :853-858/:867-884 stay cut — no item logistics surface — output ITEM slots stay
 *     blocked, while the FLUID auto-output push (:459 → {@link #doOutputFluids}) is LIVE
 *     since task p16-machine-side-io ② and drains a configured mFluidAutoOutput face);</li>
 * <li>{@link #checkRecipe(boolean, boolean)} :683-778 with the :687 doInputItems ITEM
 *     auto-IO cut (the item pool) but the fluid legs LIVE — the :706 tank census since
 *     task p14-machine-fluid-face, the :696-705 auto-input PULL and the :716-732
 *     mCanUseOutputTanks output-tank fallback since task p16-machine-side-io ②③, plus the
 *     :709/:710 minimal-fluid gates, the :712 findRecipe tank argument, the
 *     :738/:744 consume through the tank snapshot mirror, and the PARALLEL blocks
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
 *
 * <p>GUI: the row-less families (Shredder/Crusher/Lathe) open the ModularUI chain since
 * task p26-mui-a-open-chain — the BE implements {@link GT6MuiMachine} and
 * {@link #buildUI} delegates to the {@link GTBasicMachineMUI} panel factory over the
 * {@link GTBasicMachineMenu#hostOf} projection; the row families (dryer/canner/
 * distillery) keep the vanilla MenuProvider path ({@link #createMenu}) byte-identical.
 */
public class TileEntityBasicMachine extends TileEntityBase03TicksAndSync implements MenuProvider, GT6MuiMachine {

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
	/**
	 * Upstream NBT_TANK_CAPACITY (:157-158 — read, NEVER written, the registration-config
	 * family; the port re-feeds it through {@code /data merge} the same way the upstream
	 * registry re-feeds the placement NBT). Task p16-machine-side-io ③.
	 */
	public static final String NBT_TANK_CAPACITY = "tank_capacity";
	/** Upstream NBT_USE_OUTPUT_TANK (:132) — the {@link #mCanUseOutputTanks} registration key (read-only, same family). */
	public static final String NBT_USE_OUTPUT_TANK = "use_output_tank";
	/** Upstream NBT_INV_SIDE_IN / NBT_INV_SIDE_OUT (:137-138, OR {@link #SBIT_A} on load) — the item ACCESS masks. */
	public static final String NBT_ITEM_SIDE_IN = "item_sides_in";
	public static final String NBT_ITEM_SIDE_OUT = "item_sides_out";
	/** Upstream NBT_TANK_SIDE_IN / NBT_TANK_SIDE_OUT (:143-144, OR {@link #SBIT_A} on load) — the fluid-face masks (the p14 carriers). */
	public static final String NBT_TANK_SIDE_IN = "fluid_sides_in";
	public static final String NBT_TANK_SIDE_OUT = "fluid_sides_out";
	/** Upstream NBT_TANK_SIDE_AUTO_IN / NBT_TANK_SIDE_AUTO_OUT (:145-146, no SBIT_A OR) — the fluid auto-IO sides. */
	public static final String NBT_TANK_SIDE_AUTO_IN = "fluid_sides_auto_in";
	public static final String NBT_TANK_SIDE_AUTO_OUT = "fluid_sides_auto_out";
	/** Upstream CS SBIT_A — the SIDE_ANY bit the NBT mask loads OR in (:137-144), so a configured mask keeps the side-less probe open. */
	public static final byte SBIT_A = 64;
	/** Upstream SIDE_UNDEFINED — the auto-IO side off value (:94-95 defaults; SIDES_VALID[-1+1] = F, CS.java:561-568 form). */
	public static final byte SIDE_UNDEFINED = -1;

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

	/**
	 * Port-owned: which MenuType opens this machine's GUI (one per registered machine).
	 * Null semantics (task p26-mui-a-open-chain): an offline test fixture, OR a
	 * ModularUI-family machine — the row-less families open through
	 * {@link GT6MuiMachine#tryOpen} (the BlockEntityUIFactory chain), so their vanilla
	 * menu is unreachable and {@link #createMenu} keeps its documented throw; the row
	 * families (dryer/canner/distillery) keep their live supplier path unchanged.
	 */
	private final Supplier<MenuType<GTBasicMachineMenu>> mMenuType;

	// capability handle (P6 gated-item-handler precedent)
	//? if forge {
	private final LazyOptional<IItemHandler> mGatedCap = LazyOptional.of(() -> newGatedHandler(null));
	//?} else {
	/*private IItemHandler mGatedHandler; // (1.21.1) the LazyOptional.of lazy semantics kept — created at the first query; 21.1 has no invalidation surface
	 *///?}

	/**
	 * Full constructor — the BET factory entry (Builder.of(...).build(null) works without a
	 * registry, the oven test-fixture precedent). {@code aMenuType} may be null for offline
	 * fixtures (createMenu then throws — menus are a live-server surface) and for
	 * ModularUI-family machines (the open chain rides {@link GT6MuiMachine#tryOpen},
	 * createMenu unreachable — the field doc's null semantics).
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
		// :157-162 — the tanks (see the field docs for the capacity rulings); mTankCapacity
		// is the field-initializer 1000 here, the registration rows re-arm it through
		// applyTankCapacity() and load() applies NBT_TANK_CAPACITY before the content read
		mTanksInput = new FluidTankGT[mRecipes.mInputFluidCount];
		for (int i = 0; i < mTanksInput.length; i++) mTanksInput[i] = new FluidTankGT(mTankCapacity); // :159-160 (the 1000 default; the map leg collapsed)
		mTanksOutput = new FluidTankGT[mRecipes.mOutputFluidCount];
		for (int i = 0; i < mTanksOutput.length; i++) mTanksOutput[i] = new FluidTankGT(); // :162 (default capacity)
		// :217 — the ACCESSIBLE table must exist before the first capability query
		// (the upstream readFromNBT2 tail runs it after every load too)
		updateAccessibleSlots();
	}

	/**
	 * Re-arms {@link #mTankCapacity} onto the input tanks (the registration-config seam:
	 * upstream constructs them AT the :157-158 capacity, the port tanks are constructor-built
	 * so the rows set the carrier and call this).
	 */
	public void applyTankCapacity() {
		for (FluidTankGT tTank : mTanksInput) tTank.setCapacity(mTankCapacity);
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
			// upstream :459 — the fluid auto-output push arm ahead of the work chain (the
			// mDisabledFluidOutput half is cut with the disabled family; the undefined side
			// folds to a no-op so the registered machines keep their pre-p16 behaviour)
			if (mFluidAutoOutput != SIDE_UNDEFINED) doOutputFluids();
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
	 * Upstream :683-778 — the :687 doInputItems ITEM auto-IO stays cut (the item auto-IO pool
	 * item; the FLUID pull arm :696-705 is LIVE since task p16-machine-side-io ②); the
	 * parallel blocks (:742-745) restored (the oven cut them at mParallel = 1; the Crusher
	 * runs 4); the energy math :761-774 verbatim with the RF halves of :767/:770 cut (no RF
	 * conversion in the port constants). {@code aApplyRecipe=false} probes, {@code true}
	 * consumes (the two-stage isRecipeInputEqual contract).
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

		// :696-705 RESTORED (task p16-machine-side-io ②, the capability translation) — the
		// fluid auto-input PULL: the getTankInfo walk (:700) becomes the getTanks/getFluidInTank
		// census, the FL.move_ beat (:701-702) the drain-SIMULATE → own-fill → drain-EXECUTE
		// three-beat (the P6 push form mirrored for pull; the :697 mDisabledFluidInput half is
		// cut with the disabled family). The :568 containsInput filter stays cut (the pool
		// row's remaining item — the port RecipeMap carries no containsInput surface).
		if (aUseAutoIO && mFluidAutoInput != SIDE_UNDEFINED) {
			byte tAutoInput = worldSideOfRelative(mFluidAutoInput); // FACING_TO_SIDE (:696)
			if (tAutoInput != SIDE_UNDEFINED) {
				IFluidHandler tSource = getFluidInputTarget(tAutoInput); // :698
				if (tSource != null) for (int i = 0; i < tSource.getTanks(); i++) {
					FluidStack tInfo = tSource.getFluidInTank(i); // :700 tank info walk
					if (tInfo == null || tInfo.isEmpty()) continue;
					FluidTankGT tTank = fillableAny(tInfo); // :701 getFluidTankFillable(SIDE_ANY, ...) — no mask gate
					if (tTank == null) continue;
					int tFit = tTank.fill(copyOf(tInfo, tInfo.getAmount()), FluidAction.SIMULATE);
					if (tFit <= 0) continue;
					FluidStack tDrained = tSource.drain(tFit, FluidAction.EXECUTE);
					if (tDrained == null || tDrained.isEmpty()) continue;
					if (tDrained.getAmount() < tFit) tFit = tDrained.getAmount(); // the source under-delivered → bound the fill
					if (tTank.fill(copyOf(tDrained, tFit), FluidAction.EXECUTE) > 0) onFluidIO(); // :702 updateInventory on move > 0
				}
			}
		}
		// :706 the input-tank census RESTORED (task p14-machine-fluid-face ③) — the pulled
		// fluids above land here in the SAME pass
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

		// :716-732 RESTORED (task p16-machine-side-io ③) — the mCanUseOutputTanks fallback:
		// a failed input-tank lookup re-runs against the OUTPUT tanks and the whole consume
		// chain below drains THEM (upstream :718/:725/:731 pass mTanksOutput). The two
		// upstream branches differ ONLY in the tank array, so the port keeps the merged
		// branch shape with the source array selected here.
		boolean tUseOutputTanks = false;
		if (tRecipe == null && mCanUseOutputTanks) { // :717
			tRecipe = mRecipes.findRecipe(mLastRecipe, mInputMax, ItemStack.EMPTY, tankSnapshot(mTanksOutput), tInputs); // :718
			tUseOutputTanks = tRecipe != null;
		}

		int tMaxProcessCount = 0; // :714

		if (tRecipe == null) return DID_NOT_FIND_RECIPE; // :719/:719-shape

		// the p28-c-ulv-machine-ladder melting gate (NO upstream :72x line — the container
		// semantics of Smeltery :194 / Mold :189 re-expressed as a machine recipe gate; the
		// research.p28-r-ulv-tier-design rejected-arms ledger covers the RM whitelist and
		// the BE-subclass forms): findRecipe HIT but a consumed input material melts above
		// the row ceiling → the recipe is refused BEFORE any consume (the probe arm
		// aApplyRecipe=false refuses identically, so the machine never starts). Unresolvable
		// inputs (vanilla items, non-GT stacks) pass — the "no material data = no gate" arm
		// is what keeps the vanilla-compat recipes runnable.
		if (mMaxMeltingPointK != null && meltingGateBlocks(mMaxMeltingPointK, tInputs)) return FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS; // :736/:723-shape

		if (tRecipe.mCanBeBuffered) mLastRecipe = tRecipe; // :734/:721
		tMaxProcessCount = canOutput(tRecipe); // :735/:722
		if (tMaxProcessCount <= 0) return FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS; // :736/:723

		// :737 — the ignition half (mRequiresIgnition || mIgnited > 0 || mActive) is cut.
		// :738/:725 — the two-stage consume runs on the tank SNAPSHOT (the frozen P4 Recipe
		// takes FluidStack[], upstream Recipe.java:800 takes the IFluidTank[] directly and
		// drains it in place at :833); an applied consume mirrors the exact drained amounts
		// onto the real SOURCE tanks (the applyTankConsumption adapter, :833's tank.drain leg).
		FluidTankGT[] tSourceTanks = tUseOutputTanks ? mTanksOutput : mTanksInput;
		FluidStack[] tFluids = tankSnapshot(tSourceTanks);
		long[] tFluidBaseline = snapshotAmounts(tSourceTanks);
		if (!tRecipe.isRecipeInputEqual(aApplyRecipe, false, tFluids, tInputs)) return FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS; // :738/:725
		if (aApplyRecipe) applyTankConsumption(tSourceTanks, tFluids, tFluidBaseline);
		mCouldUseRecipe = true; // :739
		if (!aApplyRecipe) return FOUND_AND_COULD_HAVE_USED_RECIPE; // :740

		if (tMaxProcessCount > 1) { // :742-745 RESTORED (oven cut this; Crusher NBT_PARALLEL 4)
			if (!mParallelDuration && mEnergyTypeAccepted != TD.Energy.TU) { // :743/:730 (RF half cut)
				// UT.Code.bind(aMin, aMax, aBoundValue): the per-tick energy budget caps the count
				tMaxProcessCount = (int)UT.Code.bind(1, tMaxProcessCount, mInput / Math.max(1, tRecipe.mEUt));
			}
			// :744/:731 — 1 + the COUNT consume form (upstream Recipe.isRecipeInputEqual(int,
			// IFluidTank[], ...) :840-852 with the per-iteration fluid pre-check :847-851;
			// the P4 Recipe shell is frozen, so the count loop is the local helper below)
			tMaxProcessCount = 1 + isRecipeInputEqual(tRecipe, tMaxProcessCount - 1, tFluids, tInputs);
			if (aApplyRecipe) applyTankConsumption(tSourceTanks, tFluids, tFluidBaseline);
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
				mMaxProgress = Math.max(1, units(mMinEnergy * Math.max(1, tRecipe.mDuration) * tMaxProcessCount, mEfficiency, 10000, true)); // :768 (efficiency 10000 = the units() identity, the historical folded form)
			} else { // :770-771 — the energy scales (the speedup); TU keeps its constant per-process energy
				mMinEnergy = Math.max(1, (mEnergyTypeAccepted == TD.Energy.TU ? tRecipe.mEUt : tRecipe.mEUt * tMaxProcessCount));
				mMaxProgress = Math.max(1, units(mMinEnergy * Math.max(1, tRecipe.mDuration), mEfficiency, 10000, true)); // :771
			}
			// :773 verbatim — 4x energy, 2x speed (mCheapOverclocking cut: no config source, always T)
			while (mMinEnergy < mInputMin && mMinEnergy * 4 <= mInputMax) {mMinEnergy *= 4; mMaxProgress *= 2;}
		}

		removeEmptyInputStacks(); // :776 removeAllDroppableNullStacks shape
		return FOUND_AND_SUCCESSFULLY_USED_RECIPE; // :777
	}

	/**
	 * The p28-c-ulv-machine-ladder melting gate — the PURE decision function behind the
	 * {@link #checkRecipe} hook (offline-testable: no world, no BE state). {@code true} =
	 * BLOCKED. Any ONE input stack whose resolved material carries
	 * {@code mMeltingPoint > aMaxMeltingPointK} blocks the recipe (the 任一超即拒 ruling);
	 * stacks with NO material data (vanilla items, non-GT stacks, empty slots) pass. The
	 * material resolution is the port's item→material seam — {@code
	 * MaterialPrefixItem.material} (the same public field the tint/paint faces read); the
	 * upstream OM.materialstack(ItemStack) walk has no wider port counterpart yet, so
	 * non-{@code MaterialPrefixItem} stacks are unresolvable and pass.
	 */
	public static boolean meltingGateBlocks(long aMaxMeltingPointK, @Nullable ItemStack... aInputs) {
		if (aInputs == null) return false;
		for (ItemStack tStack : aInputs) {
			if (tStack == null || tStack.isEmpty()) continue;
			if (tStack.getItem() instanceof gregtech6.item.MaterialPrefixItem tMaterialItem
					&& tMaterialItem.material != null
					&& tMaterialItem.material.mMeltingPoint > aMaxMeltingPointK) return true;
		}
		return false;
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
	 * directions, the upstream field default (:95). The NBT load leg (:143-144, OR
	 * {@link #SBIT_A}) is the same hasKey-guarded registration-config family as the item
	 * masks below — absent keys keep the constructor/applyRow value (the p14
	 * loadKeepsTheConstructorInjectedConfig contract).
	 */
	public byte mFluidInputs = 127, mFluidOutputs = 127;
	/**
	 * Upstream :94 — the per-side item ACCESS masks (machine-relative side bits, default 127
	 * = every relative side, the upstream field default). Task p16-machine-side-io ①: they
	 * feed {@link #updateAccessibleSlots()} (upstream :533-541) — the per-world-side
	 * accessible-slot table every insert/extract consults. Registration rows re-point them
	 * post-construction (the carrier pattern of mEnergyInputs; the upstream rows write
	 * NBT_INV_SIDE_IN/OUT at :1294-1309) and the hasKey-guarded load legs (:137-138, OR
	 * {@link #SBIT_A}) keep NBT overrides working without persisting the config (the
	 * upstream writeToNBT2 never writes these keys either).
	 */
	public byte mItemInputs = 127, mItemOutputs = 127;
	/**
	 * Upstream :95 — the fluid auto-IO sides (machine-relative, {@link #SIDE_UNDEFINED} =
	 * off, the upstream default). Task p16-machine-side-io ②: {@link #mFluidAutoInput} is
	 * the PULL face the :696-705 checkRecipe arm drains from, {@link #mFluidAutoOutput} the
	 * PUSH face the :459/:994-996 tick arm fills through — both fold to no-ops while
	 * undefined, so the registered machines keep their exact pre-p16 behaviour. The item
	 * auto-IO pair (mItemAutoInput/mItemAutoOutput, upstream :94) stays CUT with its pool
	 * item (doInputItems/doOutputItems are not ported). The NBT load legs (:145-146) are
	 * registration-config, never persisted.
	 */
	public byte mFluidAutoInput = SIDE_UNDEFINED, mFluidAutoOutput = SIDE_UNDEFINED;
	/**
	 * Upstream :92 mCanUseOutputTanks (NBT_USE_OUTPUT_TANK :132) — when the primary recipe
	 * lookup over the input tanks fails, the checkRecipe :716-732 fallback re-runs the
	 * lookup AND the consume against the OUTPUT tanks, so a product sitting in the output
	 * tank can directly feed a follow-up recipe. Default false, the upstream field default;
	 * registration rows re-point it post-construction.
	 */
	public boolean mCanUseOutputTanks = false;
	/**
	 * Upstream :157-158 — the input-tank capacity (NBT_TANK_CAPACITY, default 1000). The
	 * carrier form of the upstream load-time local: {@link #applyTankCapacity()} re-arms the
	 * constructed tanks (the upstream :160 constructs them AT capacity; the port tanks are
	 * constructor-built, so the carrier + re-apply seam is the same behaviour), and
	 * {@link #load} applies the NBT key before the content read. The upstream
	 * {@code setCapacity(mRecipes, mParallel * 2L)} adjustable-map leg (:160) stays collapsed
	 * with the RecipeMap mMinInputTankSizes omission (RecipeMap.java:38, the p14 ruling).
	 */
	public long mTankCapacity = 1000;
	/**
	 * Port-owned (task p28-c-ulv-machine-ladder, NO upstream counterpart — the ULV tier
	 * extension is the declared-deviation new machine face): the row's
	 * {@code maxMeltingPointK} gate column carried onto the BE by
	 * {@link GTMachines#applyRow} the same way the side masks ride. {@code null} = the
	 * machine sets no melting gate (every non-ULV row). A non-null value turns the
	 * {@link #checkRecipe} hook on: a recipe whose INPUT material stacks carry an
	 * {@code mMeltingPoint} above the threshold is refused
	 * (FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS) before any consume — the GT6
	 * container-by-melting-point semantics (MultiTileEntitySmeltery.java:194
	 * {@code mTemperature >= mMeltingPoint} / MultiTileEntityMold.java:189 temperature
	 * refusal) re-expressed as a machine recipe gate. Every ULV row carries 1375 K — the
	 * stone-crucible ceiling (GT6Crucibles.java:86, TileEntitySmelteryOfflineTest :281):
	 * "meltable in the stone crucible = processable in ULV". Registration-config carrier,
	 * NOT persisted (the mEnergyInputs load-keeps-the-constructor-config contract).
	 */
	public Long mMaxMeltingPointK = null;
	/**
	 * Upstream :96 mEfficiency — the progress-division divisor of the :768/:771 rows
	 * ({@code units(minEnergy × duration [× parallelCount], mEfficiency, 10000, T)};
	 * upstream UT.Code.units(a, orig, targ) = a × targ/orig — the LH.java:334 efficiency
	 * tooltip is the same direction): the FRACTION OF ENERGY-TIME THAT DOES WORK, scaled
	 * against the 10000 perfection. 10000 = the identity (every unit of energy-time
	 * counts — every row before task p29-w1-rm-maps-scaffold, which is why the port could
	 * FOLD the divisor to the constant 10000 until this card); 5000 = 2× the REQUIRED
	 * progress per process (the Electric* rows :1504-1522 NBT_EFFICIENCY 5000 — the
	 * plug-in convenience burns 2× the energy-time; in the same wall-clock the bar sits
	 * at exactly half). The registration-config carrier (the mMaxMeltingPointK shape):
	 * {@code GTMachines.applyRow} writes the row's {@code efficiency} column through the
	 * upstream :125 bind form {@code bind(0, 10000, value)} — NOT persisted (the
	 * loadKeepsTheConstructorInjectedConfig contract). The upstream :125 bind floor of 0
	 * is kept verbatim; efficiency 0 hits the UT.Code.units aOriginalUnit==0 arm
	 * (UT.java:1679) which IS the identity, the same no-penalty net effect as 10000
	 * (upstream quirk preserved).
	 */
	public short mEfficiency = 10000;
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
			//? if forge {
			if (tFluid != null && !tFluid.isEmpty()) rSnapshot[i] = new FluidStack(tFluid, FluidTankGT.bindInt(aTanks[i].amount()));
			//?} else {
			/*if (tFluid != null && !tFluid.isEmpty()) rSnapshot[i] = tFluid.copyWithAmount(FluidTankGT.bindInt(aTanks[i].amount())); // 21.1: no copy ctor — copyWithAmount(int)
			*///?}
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
	 * The tank array is a parameter since task p16-machine-side-io ③ — the
	 * mCanUseOutputTanks fallback consumes from mTanksOutput the same way.
	 */
	private void applyTankConsumption(FluidTankGT[] aTanks, FluidStack[] aSnapshot, long[] aBaseline) {
		boolean tChanged = false;
		for (int i = 0; i < aTanks.length && i < aSnapshot.length; i++) {
			if (aBaseline[i] <= 0) continue;
			long tNow = (aSnapshot[i] == null || aSnapshot[i].isEmpty()) ? 0 : aSnapshot[i].getAmount();
			long tConsumed = aBaseline[i] - tNow;
			if (tConsumed > 0) {
				aTanks[i].remove(tConsumed);
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
	// fluid auto-IO (upstream :459/:696-705/:994-996, task p16-machine-side-io ②)
	// ---------------------------------------------------------------------------

	/**
	 * The CS.java:543-552 {@code FACING_TO_SIDE} table — the machine-relative → world side
	 * inverse of {@link GTSideTables#FACING_ROTATIONS} (row = the world facing, column = the
	 * relative side, value = the world side). Local to this class until a shared-table card
	 * moves it beside {@link GTSideTables} (the util file is outside this task's scope).
	 */
	private static final byte[][] FACING_TO_SIDE = {
		{0,1,2,3,4,5,6,6},
		{0,1,2,3,4,5,6,6},
		{0,1,5,2,4,3,6,6},
		{0,1,4,3,5,2,6,6},
		{0,1,2,4,3,5,6,6},
		{0,1,3,5,2,4,6,6},
		{0,1,2,3,4,5,6,6},
		{0,1,2,3,4,5,6,6}
	};

	/** The upstream {@code FACING_TO_SIDE[mFacing][aRelativeSide]} lookup (both operands masked into the table domain). */
	public byte worldSideOfRelative(byte aRelativeSide) {
		return FACING_TO_SIDE[mFacing & 7][aRelativeSide & 7];
	}

	/**
	 * The load-leg domain normalizer for the auto-IO sides (upstream SIDES_VALID semantics,
	 * CS.java:561-568): only a valid relative face 0..5 or the {@link #SIDE_UNDEFINED} off
	 * value is accepted — any other byte folds to {@link #SIDE_UNDEFINED}. A raw out-of-domain
	 * value (e.g. a {@code /data merge} 99) would otherwise pass the {@code != SIDE_UNDEFINED}
	 * gates (:383/:577) and alias onto a real face through the {@code & 7} in
	 * {@link #worldSideOfRelative} (99 &amp; 7 = 3 = front), turning the off state into live IO.
	 */
	private static byte normalizeAutoIOSide(byte aSide) {
		return (aSide >= 0 && aSide <= 5) || aSide == SIDE_UNDEFINED ? aSide : SIDE_UNDEFINED;
	}

	/** The auto-input adjacency seam (upstream :974-976 getFluidInputTarget(byte)) — the neighbor FLUID_HANDLER on that world face. */
	@Nullable
	protected IFluidHandler getFluidInputTarget(byte aWorldSide) {
		return fluidHandlerAt(aWorldSide);
	}

	/**
	 * The auto-output adjacency seam (upstream :978-980 getFluidOutputTarget(byte, Fluid)) —
	 * the Fluid argument folds away: the port capability query is content-blind.
	 */
	@Nullable
	protected IFluidHandler getFluidOutputTarget(byte aWorldSide) {
		return fluidHandlerAt(aWorldSide);
	}

	/** The neighbor FLUID_HANDLER resolve (the TileEntityCokeOven.fluidHandlerAt :192-203 form, side parameterised; opposite face — the handler fronts ITS face toward us). */
	@Nullable
	private IFluidHandler fluidHandlerAt(byte aWorldSide) {
		if (!hasLevel() || isClientSide()) return null;
		Direction tSide = Direction.from3DDataValue(aWorldSide & 7);
		BlockPos tPos = getBlockPos().relative(tSide);
		//? if forge {
		BlockEntity tNeighbor = getLevel().getBlockEntity(tPos);
		if (tNeighbor == null) return null;
		return tNeighbor.getCapability(ForgeCapabilities.FLUID_HANDLER, tSide.getOpposite()).resolve().orElse(null);
		//?} else {
		/*// 21.1: the query goes through the level (ILevelExtension.getCapability returns the
		//handler directly, null when absent; the TileEntityBase08Barrel:300 precedent).
		return getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, tPos, tSide.getOpposite());
		 *///?}
	}

	/**
	 * Upstream :994-996 doOutputFluids — for each non-empty output tank, push through the
	 * auto-output face's neighbor handler with the P6 three-beat (drain SIMULATE → target
	 * fill EXECUTE → drain EXECUTE exactly what landed; 0 accepted = the source keeps
	 * everything). The upstream {@code FL.move(tank, delegator)} is the same three-beat;
	 * the {@code > 0} move fires the :995 updateInventory beat.
	 */
	public void doOutputFluids() {
		byte tAutoOutput = worldSideOfRelative(mFluidAutoOutput);
		if (tAutoOutput == SIDE_UNDEFINED) return;
		for (FluidTankGT tCheck : mTanksOutput) if (tCheck.has()) { // :995
			FluidStack tContent = tCheck.fluid();
			if (tContent == null || tContent.isEmpty()) continue;
			IFluidHandler tTarget = getFluidOutputTarget(tAutoOutput); // :995 per-tank target call
			if (tTarget == null) continue;
			FluidStack tAvailable = tCheck.drain(Integer.MAX_VALUE, FluidAction.SIMULATE);
			if (tAvailable == null || tAvailable.isEmpty()) continue;
			int tFilled = tTarget.fill(tAvailable, FluidAction.EXECUTE);
			if (tFilled <= 0) continue; // the target refuses → the source keeps everything (fill-then-deduct)
			FluidStack tDrained = tCheck.drain(tFilled, FluidAction.EXECUTE);
			if (tDrained != null && !tDrained.isEmpty()) onFluidIO(); // :995 updateInventory on move > 0
		}
	}

	/** Per-amount COPIES for the auto-IO hand-offs (the tankSnapshot copy form, forge/21.1 split). */
	private static FluidStack copyOf(FluidStack aFluid, int aAmount) {
		//? if forge {
		return new FluidStack(aFluid, aAmount);
		//?} else {
		/*return aFluid.copyWithAmount(aAmount); // 21.1: no copy ctor
		 *///?}
	}

	// ---------------------------------------------------------------------------
	// fluid tank face (upstream :561-597, task p14-machine-fluid-face ⑤)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream getFluidTankFillable2 :564-571 — the :565 auto-output-face refuse leg is LIVE
	 * since task p16-machine-side-io ② (a configured mFluidAutoOutput makes that world face
	 * push-only: external fill is refused there), then the :566 mask gate, then the tank
	 * walk. The :568 containsInput recipe filter is cut with its pool item — any fluid may
	 * claim an EMPTY input tank (declared deviation; the port RecipeMap carries no
	 * containsInput surface, the same TODO.md pool row as the auto-IO's remaining item).
	 */
	@Nullable
	public FluidTankGT getFluidTankFillable(byte aWorldSide, FluidStack aFluidToFill) {
		if (mFluidAutoOutput != SIDE_UNDEFINED && worldSideOfRelative(mFluidAutoOutput) == aWorldSide) return null; // :565 (the mDisabledFluidOutput half is cut)
		if (!GTSideTables.faceConnected(mFacing, aWorldSide, mFluidInputs)) return null; // :566
		return fillableAny(aFluidToFill);
	}

	/**
	 * Upstream :701 getFluidTankFillable(SIDE_ANY, ...) — the side-less fillable the
	 * auto-input pull consults: containing tank first (:567), then an empty one (:569); NO
	 * mask gate (the SIDE_ANY row is all-open) and no auto-output refuse (the pull is the
	 * machine's own initiative, not a face interaction).
	 */
	@Nullable
	public FluidTankGT fillableAny(FluidStack aFluidToFill) {
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
	// the per-side item ACCESS table (upstream :533-545, task p16-machine-side-io ①)
	// ---------------------------------------------------------------------------

	/** Upstream :543 — the per-WORLD-side accessible slot table (row 6 = the SIDE_ANY entry, built like upstream). */
	private final int[][] mAccessible = new int[7][];
	/** Upstream :544 — the three shared slot lists (rebuilt by {@link #updateAccessibleSlots()}). */
	private int[] mAccessibleSlots, mAccessibleInputs, mAccessibleOutputs;

	/**
	 * Upstream :533-541 updateAccessibleSlots — rebuilds the per-world-side slot table from
	 * the rotated {@link #mItemInputs}/{@link #mItemOutputs} masks: a side with both masks
	 * touches EVERY slot (ACCESSIBLE_SLOTS), an input-only side the input range
	 * (ACCESSIBLE_INPUTS), an output-only side the output range (ACCESSIBLE_OUTPUTS), a
	 * side with neither touches nothing (ZL_INTEGER). The direction gates
	 * ({@link #canInsertItem2}/{@link #canExtractItem2}) stay ON TOP of the table, exactly
	 * like upstream — an output-only side still cannot INSERT into the accessible output
	 * slots. Rebuilt at construction, after every load (:217) and on every facing change
	 * (:1005 onFacingChange).
	 */
	public void updateAccessibleSlots() {
		int tInputCount = mRecipes.mInputItemsCount, tOutputCount = mRecipes.mOutputItemsCount;
		mAccessibleSlots = new int[tInputCount + tOutputCount];
		for (int i = 0; i < mAccessibleSlots.length; i++) mAccessibleSlots[i] = i; // UT.Code.getAscendingArray :526
		mAccessibleInputs = new int[tInputCount];
		for (int i = 0; i < tInputCount; i++) mAccessibleInputs[i] = i; // :527
		mAccessibleOutputs = new int[tOutputCount];
		for (int i = 0; i < tOutputCount; i++) mAccessibleOutputs[i] = tInputCount + i; // :528-529
		for (byte i = 0; i < 7; i++) { // upstream :534 — all seven rows (6 = the SIDE_ANY entry)
			if (GTSideTables.faceConnected(mFacing, i, mItemInputs)) { // :535 — the row byte IS a world side, the rotation is inside faceConnected
				if (GTSideTables.faceConnected(mFacing, i, mItemOutputs)) mAccessible[i] = mAccessibleSlots; else mAccessible[i] = mAccessibleInputs; // :536
			} else {
				if (GTSideTables.faceConnected(mFacing, i, mItemOutputs)) mAccessible[i] = mAccessibleOutputs; else mAccessible[i] = null; // :538 (ZL_INTEGER)
			}
		}
	}

	/**
	 * Upstream :545 getAccessibleSlotsFromSide2 — the table row (a defensive copy: the
	 * array is shared state), empty for a side with no access.
	 */
	public int[] getAccessibleSlotsFromSide(byte aWorldSide) {
		int[] tRow = mAccessible[aWorldSide & 7];
		return tRow == null ? new int[0] : tRow.clone();
	}

	/** The upstream touch-gate: the side's ACCESSIBLE row must list the slot before any insert/extract is even considered. */
	public boolean isSlotAccessible(byte aWorldSide, int aSlot) {
		int[] tRow = mAccessible[aWorldSide & 7];
		if (tRow == null) return false;
		for (int tSlot : tRow) if (tSlot == aSlot) return true;
		return false;
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
	 * input slot (canInsertItem2 :549-554 trimmed — the mMode empty-slot rule, the same-item
	 * dedup and the containsInput legs are the pool), extract only the output slots
	 * (canExtractItem2 :556-559). Storage stays the plain {@link GTItemStackHandler}.
	 * Task p16-machine-side-io ①: the {@code aSide} face first consults the ACCESSIBLE
	 * table (upstream getAccessibleSlotsFromSide2 :545 — a side may only touch the slots
	 * its mask row lists, in BOTH directions); {@code null} = the side-less probe, which
	 * keeps the pre-p16 all-sides behaviour (the P5 barrel side-less ruling form).
	 */
	private IItemHandler newGatedHandler(@Nullable Direction aSide) {
		GTItemStackHandler tInventory = mInventory;
		return new IItemHandler() {
			@Override public int getSlots() {return tInventory.getSlots();}
			@Override public ItemStack getStackInSlot(int aSlot) {return tInventory.getStackInSlot(aSlot);}
			@Override public int getSlotLimit(int aSlot) {return tInventory.getSlotLimit(aSlot);}
			@Override public boolean isItemValid(int aSlot, ItemStack aStack) {return canInsertItem2(aSlot);}

			@Override
			public ItemStack insertItem(int aSlot, ItemStack aStack, boolean aSimulate) {
				if (aStack == null || aStack.isEmpty() || !canInsertItem2(aSlot) || !touchable(aSide, aSlot)) return aStack;
				return tInventory.insertItem(aSlot, aStack, aSimulate);
			}

			@Override
			public ItemStack extractItem(int aSlot, int aAmount, boolean aSimulate) {
				if (!canExtractItem2(aSlot) || !touchable(aSide, aSlot)) return ItemStack.EMPTY;
				return tInventory.extractItem(aSlot, aAmount, aSimulate);
			}
		};
	}

	/** The touch-gate seam: null side = all-open (the side-less probe), a world face reads the ACCESSIBLE row. */
	private boolean touchable(@Nullable Direction aSide, int aSlot) {
		return aSide == null || isSlotAccessible((byte)aSide.get3DDataValue(), aSlot);
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
	 * The per-call wrapper factories — the {@code getCapability(<CAP>, aSide)} seams.
	 * Package-private so the offline tests drive the wrappers directly (the
	 * ForgeCapabilities tokens are transformer-resolved and unresolvable offline, the
	 * GT6MultiBlockFluidTest direct-construction precedent).
	 */
	IFluidHandler newFluidHandler(@Nullable Direction aSide) {
		return new BasicMachineFluidHandler(this, aSide);
	}

	/** The per-side gated item surface (task p16-machine-side-io ①) — null side = the all-sides form. */
	IItemHandler newItemHandler(@Nullable Direction aSide) {
		return newGatedHandler(aSide);
	}

	//? if forge {
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> aCapability, @Nullable Direction aSide) {
		if (aCapability == ForgeCapabilities.ITEM_HANDLER) {
			if (aSide == null) return mGatedCap.cast(); // the side-less probe keeps the cached all-sides surface
			// the fresh-wrapper-per-call form (the FLUID_HANDLER comment below): the side is
			// part of the wrapper identity — a stored LazyOptional would freeze the
			// FIRST-queried side into the ACCESSIBLE row
			return LazyOptional.of(() -> newGatedHandler(aSide)).cast();
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
			if (aSide == null) {
				if (mGatedHandler == null) mGatedHandler = newGatedHandler(null); // the side-less cached surface
				return (T) mGatedHandler;
			}
			// the fresh-per-call form: the side is part of the handler identity
			return (T) newGatedHandler(aSide);
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

	/**
	 * Chest precedent onPlaced :128-131 — GT6 side order == Direction.getIndex()
	 * (get3DDataValue). Task p28-singleblock-facing-canon: the front TOWARDS the placer —
	 * the GT6PlacementFacing canon (view OPPOSITE), not the raw view direction.
	 */
	public void setFacingFromPlacement(Player aPlayer) {
		mFacing = gregtech6.block.GT6PlacementFacing.placementFacing(aPlayer.getDirection());
		updateAccessibleSlots(); // the upstream onFacingChange :1005 beat
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
		updateAccessibleSlots(); // the upstream onFacingChange :1005 beat
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
	// GUI (upstream getGUIClient2/getGUIServer2 :1007-1008 → MenuProvider; the
	// ModularUI chain since task p26-mui-a-open-chain)
	// ---------------------------------------------------------------------------

	/**
	 * Server+CLIENT panel build — the GTBasicMachineMUI delegation (the ACT buildUI shape,
	 * TileEntityAdvancedCraftingTable :836-840): the gui-domain {@link GTBasicMachineMenu#hostOf}
	 * adapter projects this BE onto the panel factory, which runs on both sides (the sync
	 * handlers register there). The client screen is the inherited {@link GT6MuiMachine}
	 * default — no override needed.
	 */
	@Override
	public ModularPanel<?> buildUI(PosGuiData aData, PanelSyncManager aSyncManager, UISettings aSettings) {
		return GTBasicMachineMUI.buildPanel(GTBasicMachineMenu.hostOf(this), aSyncManager);
	}

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
		//? if forge {
		aNBT.put(NBT_INVENTORY, mInventory.serializeNBT());
		//?} else {
		/*aNBT.put(NBT_INVENTORY, mInventory.serializeNBT(NBT_ACCESS)); // 21.1: ItemStackHandler NBT takes the registries
		*///?}
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
		//? if forge {
		for (ItemStack tStack : mOutputItems) if (tStack != null && !tStack.isEmpty()) tOutputs.add(tStack.save(new CompoundTag()));
		//?} else {
		/*for (ItemStack tStack : mOutputItems) if (tStack != null && !tStack.isEmpty()) tOutputs.add(tStack.save(NBT_ACCESS, new CompoundTag())); // 21.1: provider-first save
		*///?}
		aNBT.put(NBT_OUTPUT, tOutputs); // upstream NBT_INV_OUT.i :166-167 (list form)
		for (int i = 0; i < mTanksInput.length; i++) mTanksInput[i].writeToNBT(aNBT, NBT_TANK + ".in." + i); // :160
		for (int i = 0; i < mTanksOutput.length; i++) mTanksOutput[i].writeToNBT(aNBT, NBT_TANK + ".out." + i); // :162
		ListTag tOutputFluids = new ListTag();
		//? if forge {
		for (FluidStack tFluid : mOutputFluids) if (tFluid != null && !tFluid.isEmpty()) tOutputFluids.add(tFluid.writeToNBT(new CompoundTag()));
		//?} else {
		/*for (FluidStack tFluid : mOutputFluids) if (tFluid != null && !tFluid.isEmpty()) tOutputFluids.add(tFluid.save(NBT_ACCESS, new CompoundTag())); // 21.1: the codec save face
		*///?}
		aNBT.put(NBT_OUTPUT_FLUIDS, tOutputFluids); // upstream NBT_TANK_OUT.i :164-165 (list form)
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_FACING, Tag.TAG_ANY_NUMERIC)) mFacing = aNBT.getByte(NBT_FACING);
		//? if forge {
		if (aNBT.contains(NBT_INVENTORY, Tag.TAG_COMPOUND)) mInventory.deserializeNBT(aNBT.getCompound(NBT_INVENTORY));
		//?} else {
		/*if (aNBT.contains(NBT_INVENTORY, Tag.TAG_COMPOUND)) mInventory.deserializeNBT(NBT_ACCESS, aNBT.getCompound(NBT_INVENTORY)); // 21.1: provider-first
		*///?}
		mEnergy = aNBT.getLong(NBT_ENERGY); // :115
		mMinEnergy = aNBT.getLong(NBT_MINENERGY); // :129
		mProgress = aNBT.getLong(NBT_PROGRESS); // :133
		mMaxProgress = aNBT.getLong(NBT_MAXPROGRESS); // :134
		if (aNBT.contains(NBT_STOPPED)) mStopped = aNBT.getBoolean(NBT_STOPPED); // :117
		if (aNBT.contains(NBT_IGNITED, Tag.TAG_ANY_NUMERIC)) mIgnited = aNBT.getByte(NBT_IGNITED); // :136
		if (aNBT.contains(NBT_ACTIVE)) mActive = aNBT.getBoolean(NBT_ACTIVE); // :116
		if (aNBT.contains(NBT_RUNNING)) mRunning = aNBT.getBoolean(NBT_RUNNING); // :118
		if (aNBT.contains(NBT_STATE + ".new")) mStateNew = aNBT.getBoolean(NBT_STATE + ".new"); // :119 — mStateOld stays false, the first active tick's :865 shift re-derives it
		// the registration-config family (task p16-machine-side-io ①②③) — hasKey-guarded
		// legs over NEVER-persisted keys (the upstream writeToNBT2 writes none of them), so
		// absent keys keep the constructor/applyRow values (the p14
		// loadKeepsTheConstructorInjectedConfig contract) while /data merge (the port form of
		// the upstream registry's placement-NBT re-feed) can override live
		if (aNBT.contains(NBT_ITEM_SIDE_IN, Tag.TAG_ANY_NUMERIC)) mItemInputs = (byte)(aNBT.getByte(NBT_ITEM_SIDE_IN) | SBIT_A); // :137
		if (aNBT.contains(NBT_ITEM_SIDE_OUT, Tag.TAG_ANY_NUMERIC)) mItemOutputs = (byte)(aNBT.getByte(NBT_ITEM_SIDE_OUT) | SBIT_A); // :138
		if (aNBT.contains(NBT_TANK_SIDE_IN, Tag.TAG_ANY_NUMERIC)) mFluidInputs = (byte)(aNBT.getByte(NBT_TANK_SIDE_IN) | SBIT_A); // :143
		if (aNBT.contains(NBT_TANK_SIDE_OUT, Tag.TAG_ANY_NUMERIC)) mFluidOutputs = (byte)(aNBT.getByte(NBT_TANK_SIDE_OUT) | SBIT_A); // :144
		if (aNBT.contains(NBT_TANK_SIDE_AUTO_IN, Tag.TAG_ANY_NUMERIC)) mFluidAutoInput = normalizeAutoIOSide(aNBT.getByte(NBT_TANK_SIDE_AUTO_IN)); // :145 (no SBIT_A OR; out-of-domain bytes fold to SIDE_UNDEFINED)
		if (aNBT.contains(NBT_TANK_SIDE_AUTO_OUT, Tag.TAG_ANY_NUMERIC)) mFluidAutoOutput = normalizeAutoIOSide(aNBT.getByte(NBT_TANK_SIDE_AUTO_OUT)); // :146 (same domain guard)
		if (aNBT.contains(NBT_USE_OUTPUT_TANK)) mCanUseOutputTanks = aNBT.getBoolean(NBT_USE_OUTPUT_TANK); // :132
		if (aNBT.contains(NBT_TANK_CAPACITY, Tag.TAG_ANY_NUMERIC)) mTankCapacity = FluidTankGT.bindInt(aNBT.getLong(NBT_TANK_CAPACITY)); // :157-158 (UT.Code.bindInt form)
		applyTankCapacity(); // :160 — the tanks are constructed AT capacity before the content read below
		if (aNBT.contains(NBT_OUTPUT, Tag.TAG_LIST)) {
			ListTag tOutputs = aNBT.getList(NBT_OUTPUT, Tag.TAG_COMPOUND);
			mOutputItems = new ItemStack[tOutputs.size()];
			//? if forge {
			for (int i = 0; i < tOutputs.size(); i++) mOutputItems[i] = ItemStack.of(tOutputs.getCompound(i));
			//?} else {
			/*for (int i = 0; i < tOutputs.size(); i++) mOutputItems[i] = ItemStack.parseOptional(NBT_ACCESS, tOutputs.getCompound(i)); // 21.1: the codec parse face
			*///?}
		}
		// :160/:162 — the tank CONTENT loads into the constructor-sized tanks; the capacity
		// was re-armed above (NBT_TANK_CAPACITY / applyTankCapacity, the :157-160 form)
		for (int i = 0; i < mTanksInput.length; i++) mTanksInput[i].readFromNBT(aNBT, NBT_TANK + ".in." + i);
		for (int i = 0; i < mTanksOutput.length; i++) mTanksOutput[i].readFromNBT(aNBT, NBT_TANK + ".out." + i);
		if (aNBT.contains(NBT_OUTPUT_FLUIDS, Tag.TAG_LIST)) {
			ListTag tOutputFluids = aNBT.getList(NBT_OUTPUT_FLUIDS, Tag.TAG_COMPOUND);
			mOutputFluids = new FluidStack[tOutputFluids.size()];
			//? if forge {
			for (int i = 0; i < tOutputFluids.size(); i++) mOutputFluids[i] = FluidStack.loadFluidStackFromNBT(tOutputFluids.getCompound(i));
			//?} else {
			/*for (int i = 0; i < tOutputFluids.size(); i++) mOutputFluids[i] = FluidStack.parseOptional(NBT_ACCESS, tOutputFluids.getCompound(i)); // 21.1: a failed parse lands EMPTY (the isEmpty consumers skip it like null)
			 *///?}
		}
		updateAccessibleSlots(); // :217 — the upstream readFromNBT2 tail (covers an NBT-carried facing too)
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
