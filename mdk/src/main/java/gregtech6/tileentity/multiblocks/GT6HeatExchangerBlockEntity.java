package gregtech6.tileentity.multiblocks;

import java.util.Collection;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
//? if forge {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
//?} else {
/*import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
 *///?}

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.fluid.FluidTankGT;
import gregtech6.multiblock.GTMultiBlockPattern;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;
import gregtech6.registry.GT6HeatExchangers;
import gregtech6.registry.GTMultiBlocks;
import gregtech6.tileentity.energy.GTSteamEngineBlockEntity;

/**
 * 1.20.1 counterpart of the GT6 Large Heat Exchanger multiblock — task
 * p29-w3-heat-smelter, ported from gregtech/tileentity/multiblocks/
 * MultiTileEntityLargeHeatExchanger.java (:54-266) as the W3 FM.Hot consumer and the
 * boiler family's HU source: a 3x3x2 machine whose controller sits at the CENTRE of both
 * layers, burning Hot Fuels into a buffered HU stream pushed UP through the receiver's
 * Heat Transmitter layer.
 *
 * <p><b>The anchor (:88)</b>: {@code tX = xCoord-1, tY = yCoord, tZ = zCoord-1} — the
 * structure is FACING-INDEPENDENT (the controller is the centre cell of both layers, the
 * CokeOven "Main at Bottom-Center" {@link #patternWalkFacing()} zero-offset form):
 * <ul>
 * <li>y0 (:92-100) — the 8-cell ring of Dense Tungsten Walls (upstream part id 18024 =
 *     {@code dense_wall_tungsten}), mode {@link MultiBlockPartBlockEntity
 *     #ONLY_ITEM_FLUID_ENERGY_IN} — the item/fluid/energy intake face;</li>
 * <li>y+1 (:102-110) — the 8-cell ring of Heat Transmitters (18101, mode
 *     {@link MultiBlockPartBlockEntity#NOTHING} — pure structure) around the centre
 *     Dense Tungsten Wall (:106, design 0, NOTHING).</li>
 * </ul>
 * The tooltip pair (:124-127): "3x3 Ring of 8 Heat Transmitters with Dense Tungsten Wall
 * inside" over "3x3 Ring of 8 Dense Tungsten Walls with Main inside".
 *
 * <p><b>The tick (:145-197) verbatim</b>:
 * <ol>
 * <li>the emission half (:148-160) — while the buffer holds ≥ 8 HU, transfer
 *     {@code min(mRate/8, mEnergy/8) * 8} HU split over EIGHT direct
 *     {@link ITileEntityEnergy.Util#insertEnergyInto} pushes into the tiles TWO layers
 *     up (y+2, one above each transmitter, entered from their BOTTOM face — the byte 0
 *     == {@code Direction.DOWN} canon): the receiver's own 18101 parts answer through
 *     their controller relay ({@link HeatTransmitterBlockEntity} → the Large Boiler's
 *     doInject), the 18101 proxy face of the task card. The buffered remainder stays
 *     (partially used fuel is intended upstream :148);</li>
 * <li>the refuel half (:162-191) — while {@code mEnergy < mRate * 2} and the overflow
 *     tank cannot yet hold a whole further output ({@code !mTanks[1].has(mRate * 20)}
 *     upstream, port as the SIMULATE-fill probe): find a FM.Hot row, consume it,
 *     book {@code units(|power|, 10000, mEfficiency, F)} into the buffer and drain its
 *     fluid output into the overflow tank; the while loop keeps consuming until the
 *     buffer reaches {@code mRate * 2} or the input runs dry. A dead fuel type clears
 *     the input tank only after 64 inactive ticks (the mActivity.mData == 0 latch, the
 *     GTDieselEngineBlockEntity inline-trinary form — the behavior class is not
 *     ported);</li>
 * <li>the tails — {@code mEnergy < 8} resets the buffer (:194), and the overflow tank
 *     auto-moves its content to the adjacent tank BELOW the controller (:196, the
 *     capability push).</li>
 * </ol>
 *
 * <p><b>The fields (:55-60)</b>: mEfficiency 10000 (:55 — no Loader row carries
 * NBT_EFFICIENCY), mRate = the NBT_OUTPUT row column (16384, the block carrier; the
 * upstream class default 8 is unreachable behind the single row), mEnergyTypeEmitted =
 * TD.Energy.HU (:57), mRecipes = FM.Hot (:58 —
 * {@link GT6RecipeMaps#FUELS_HOT}, the lazy resolve because the static map generation
 * may not exist offline), the DUAL TANKS (:60) — {@code mTanks[0]} the fuel input whose
 * capacity re-derives {@code mRate * 10} on load (:72 — 163840 L behind the row value,
 * the class-default 10000 L is the upstream pre-row face) and {@code mTanks[1]} the
 * UNBOUNDED overflow tank (the FluidTankGT() MAX_LONG form) whose output gate is the
 * {@code has(mRate * 20)} stop-filling rule, NOT a capacity.
 *
 * <p><b>The energy face (:251-257)</b>: emitting HU only, to the TOP face only
 * (:252 SIDES_TOP — the network-push face), offered/recommended/min/max all mRate
 * (:253-256); never accepting. The fluid face (:222-234 port capability form): fill
 * only fuels the map knows into mTanks[0] (:223 containsInput), drain pulls mTanks[1]
 * (:228), the tank view exposes both (:232).
 *
 * <p><b>NO GUI by census</b> (:140 LH.NO_GUI_FUNNEL_TAP_TO_TANK — the funnel/tap tool
 * faces are the pool, the capability face is the port intake). NO explosion face (the
 * HEX has no overheat semantics upstream, unlike the boiler). The mEfficiency field
 * carries NO live writer (the chisel/calcification arm of the boiler has no HEX
 * counterpart — the field is the :70 read + the :174/:178 units() consumer, both kept).
 */
public class GT6HeatExchangerBlockEntity extends TileEntityBase10MultiBlockBase implements ITileEntityEnergy {

	/** The NBT keys (the W3 generator spelling convention; the boiler row form). */
	public static final String NBT_ENERGY = "gt.energy";
	public static final String NBT_OUTPUT = "gt.output";
	public static final String NBT_EFFICIENCY = "gt.efficiency";
	public static final String NBT_TANK0 = "gt.tank0";
	public static final String NBT_TANK1 = "gt.tank1";
	/** The activity history NBT (the trinary inline form, the diesel carrier). */
	public static final String NBT_ACTIVITY = "gt.activity";

	/** The GT6 side byte of the receiver face (:152-159 SIDE_BOTTOM — Direction.DOWN.get3DDataValue()). */
	public static final byte RECEIVER_SIDE = 0;

	/** The efficiency (:55 — the 10000 identity; no Loader row re-binds it). */
	public short mEfficiency = 10000;
	/** The buffered heat (:56) and the emission rate (:56 — NBT_OUTPUT, the row column). */
	public long mEnergy = 0, mRate = 16384;
	/** The emitted energy type (:57). */
	public TagData mEnergyTypeEmitted = TD.Energy.HU;
	/** The fuel map (:58 — FM.Hot; the lazy resolve keeps the offline JVM map-free). */
	@Nullable
	public RecipeMap mRecipes = null;
	/** The last recipe (the findRecipe fast path). */
	@Nullable
	public Recipe mLastRecipe = null;
	/** The DUAL TANKS (:60): [0] the fuel input (capacity mRate * 10, :72), [1] the unbounded overflow. */
	public final FluidTankGT[] mTanks = {new FluidTankGT(163840), new FluidTankGT()};

	// the TE_Behavior_Active_Trinary fields (inline, the GTDieselEngineBlockEntity form —
	// the behavior class is not ported): the 64-bit activity history + the running flag
	/** The 64 bit per-tick activity history (mActivity.mData; 0 = 64 inactive ticks). */
	public long mActivityData = 0;
	/** The running flag of the tick (mActivity.mActive). */
	public boolean mActive = false;

	/** The map resolve (:58 — the FM.Hot generation, lazy so the offline JVM boots map-free). */
	public RecipeMap recipes() {
		if (mRecipes == null) mRecipes = GT6RecipeMaps.FUELS_HOT;
		return mRecipes;
	}

	// ---------------------------------------------------------------------------
	// construction (the BET-factory + block-carrier row shape)
	// ---------------------------------------------------------------------------

	/** The registry-path constructor (the shared-BET factory form). */
	public GT6HeatExchangerBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/**
	 * Full constructor — also the offline (test) entry point. A placed variant block
	 * injects the registration row (the NBT_OUTPUT 16384 column); foreign blocks (the
	 * offline fixtures) keep the class defaults.
	 */
	public GT6HeatExchangerBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType != null ? aType : GT6HeatExchangers.HEAT_EXCHANGER_BE.get(), aPos, aState);
		if (aState.getBlock() instanceof GT6HeatExchangers.HeatExchangerBlock tBlock) {
			setRate(tBlock.row().outputHUPerTick());
		}
	}

	@Override
	public String getTileEntityName() {
		return "multiblock_heat_exchanger"; // BET registry path mirrors it
	}

	/**
	 * The wall block this machine is built from (the 18024 Dense Tungsten Wall, :92-100/:106).
	 * A foreign carrier block (the offline fixtures) answers BRICKS — the fixture BET mounts
	 * it (the frozen-registry form).
	 */
	protected Block getWallBlock() {
		BlockState tState = getBlockState();
		if (tState.getBlock() instanceof GT6HeatExchangers.HeatExchangerBlock) {
			return GTMultiBlocks.WALL_BLOCKS_BY_PATH.get("dense_wall_tungsten").get();
		}
		return net.minecraft.world.level.block.Blocks.BRICKS;
	}

	/** The Heat Transmitter part block (the 18101 ring, :102-110). */
	protected Block getTransmitterBlock() {
		return GTMultiBlocks.HEAT_TRANSMITTER.get();
	}

	/** The rate setter (the row load + the offline tests) — re-derives the input-tank capacity (:72). */
	public void setRate(long aRate) {
		mRate = Math.max(1, aRate);
		mTanks[0].setCapacity(mRate * 10); // :72 — the input tank rides the rate
	}

	// ---------------------------------------------------------------------------
	// the structure (:87-121 verbatim, the facing-independent centre anchor)
	// ---------------------------------------------------------------------------

	@Override
	public boolean checkStructure2(@Nullable BlockPos aCoordinates, @Nullable Player aPlayer, @Nullable Container aInventory) {
		int tX = getBlockPos().getX() - 1, tY = getBlockPos().getY(), tZ = getBlockPos().getZ() - 1; // :88
		if (!hasLevel()) return mStructureOkay;
		if (getLevel().isLoaded(new BlockPos(tX, tY, tZ)) && getLevel().isLoaded(new BlockPos(tX + 2, tY, tZ))
				&& getLevel().isLoaded(new BlockPos(tX, tY, tZ + 2)) && getLevel().isLoaded(new BlockPos(tX + 2, tY, tZ + 2))) { // :89
			boolean tSuccess = true;

			// :92-100 — the y0 ring, Dense Tungsten Walls, ONLY_ITEM_FLUID_ENERGY_IN
			// (the centre cell is the CONTROLLER — the checkAndSetTarget self-cell arm)
			for (int tDZ = 0; tDZ <= 2; tDZ++) for (int tDX = 0; tDX <= 2; tDX++) {
				if (tDX == 1 && tDZ == 1) continue;
				if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX + tDX, tY, tZ + tDZ,
						getWallBlock(), 0, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_ENERGY_IN, aCoordinates, aPlayer, aInventory)) tSuccess = false;
			}

			// :102-110 — the y+1 ring, Heat Transmitters, NOTHING; the centre is a wall (:106)
			for (int tDZ = 0; tDZ <= 2; tDZ++) for (int tDX = 0; tDX <= 2; tDX++) {
				if (tDX == 1 && tDZ == 1) {
					if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX + tDX, tY + 1, tZ + tDZ,
							getWallBlock(), 0, MultiBlockPartBlockEntity.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = false; // :106
					continue;
				}
				if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX + tDX, tY + 1, tZ + tDZ,
						getTransmitterBlock(), 0, MultiBlockPartBlockEntity.NOTHING, aCoordinates, aPlayer, aInventory)) tSuccess = false;
			}

			return tSuccess; // :112
		}
		return mStructureOkay; // :114 — unloaded corners keep the last verdict
	}

	/** Upstream :118-121 verbatim — the 3x3x2 box around the controller. */
	@Override
	public boolean isInsideStructure(int aX, int aY, int aZ) {
		int tX = getBlockPos().getX(), tY = getBlockPos().getY(), tZ = getBlockPos().getZ();
		return aX >= tX - 1 && aY >= tY && aZ >= tZ - 1 && aX <= tX + 1 && aY <= tY + 1 && aZ <= tZ + 1;
	}

	/**
	 * The zero-offset facing (the crucible form, the base doc): the HEX controller is the
	 * CENTRE of both layers — the declared cells are CONTROLLER-relative and must NOT be
	 * displaced through any horizontal facing's side-offset table.
	 */
	@Override
	public byte patternWalkFacing() {
		return 0;
	}

	/**
	 * The declared structure pattern (the p12 binding, display data): the 17 cells in the
	 * :92-110 check order. The predicates judge BLOCK IDENTITY only — the ONLY_ITEM_FLUID_
	 * ENERGY_IN / NOTHING modes are the checkAndSetTarget write (the pattern-seam ruling).
	 */
	@Override
	@Nullable
	public GTMultiBlockPattern getStructurePattern() {
		if (mStructurePattern == null) {
			GTMultiBlockPattern.Builder tBuilder = GTMultiBlockPattern.builder();
			java.util.function.Predicate<BlockState> tWall = GTMultiBlockPattern.is(getWallBlock());
			java.util.function.Predicate<BlockState> tTransmitter = GTMultiBlockPattern.is(getTransmitterBlock());
			// :92-100 — the y0 ring
			for (int tDZ = -1; tDZ <= 1; tDZ++) for (int tDX = -1; tDX <= 1; tDX++) {
				if (tDX == 0 && tDZ == 0) continue;
				tBuilder.part(tDX, 0, tDZ, tWall);
			}
			// :102-110 — the y+1 ring + the centre wall (:106)
			for (int tDZ = -1; tDZ <= 1; tDZ++) for (int tDX = -1; tDX <= 1; tDX++) {
				if (tDX == 0 && tDZ == 0) continue;
				tBuilder.part(tDX, 1, tDZ, tTransmitter);
			}
			tBuilder.part(0, 1, 0, tWall);
			mStructurePattern = tBuilder.build();
		}
		return mStructurePattern;
	}

	@Nullable
	private GTMultiBlockPattern mStructurePattern = null;

	// ---------------------------------------------------------------------------
	// the tick (:145-197 verbatim)
	// ---------------------------------------------------------------------------

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		super.onTick(aTimer, aIsServerSide); // the 600-tick structure poll rides the base
		if (!aIsServerSide) return;

		// Emit buffered Energy (:148-160). And yes if you use a strong enough Fuel, that
		// Energy would stay buffered even while the Box is Off. This is very intended and
		// represents partially used Fuel.
		if (mEnergy >= 8) { // :149
			long tTransferred = Math.min(mRate / 8, mEnergy / 8); // :150
			mEnergy -= tTransferred * 8; // :151
			for (int tDZ = -1; tDZ <= 1; tDZ++) for (int tDX = -1; tDX <= 1; tDX++) {
				if (tDX == 0 && tDZ == 0) continue;
				ITileEntityEnergy.Util.insertEnergyInto(mEnergyTypeEmitted, RECEIVER_SIDE, 1, tTransferred, this, tileAt(new BlockPos(tDX, 2, tDZ))); // :152-159
			}
		}

		// Check if it needs to use more Fuel, or if the buffered Energy is enough (:162).
		if (mEnergy < mRate * 2) {
			// Will be set back to true if the Recipe finds enough Fuel (:164).
			mActive = false;
			// Output isn't allowed to be completely filled (:166 — the upstream has(mRate*20)
			// stop rule; the port probe is the SIMULATE fill, the canFillAll cut restored).
			if (!overflowHolds(mRate * 20)) {
				// Find and apply fitting Recipe (:168).
				Recipe tRecipe = recipes().findRecipe(mLastRecipe, Long.MAX_VALUE, null, fuelSnapshot());
				if (tRecipe != null) {
					if (overflowAccepts(tRecipe)) { // :170 — the canFillAll face
						if (consumeFuel(tRecipe)) { // :171 — the probe+consume pair (the frozen-Recipe adapter, the machine's applyTankConsumption form)
							mActive = true; // :172
							mLastRecipe = tRecipe; // :173
							mEnergy += GTSteamEngineBlockEntity.units(tRecipe.getAbsoluteTotalPower(), 10000, mEfficiency, false); // :174
							fillOverflow(tRecipe); // :175
							// Use as much as needed to keep up the Power per Tick (:176-181).
							while (mEnergy < mRate * 2 && overflowAccepts(tRecipe) && consumeFuel(tRecipe)) {
								mEnergy += GTSteamEngineBlockEntity.units(tRecipe.getAbsoluteTotalPower(), 10000, mEfficiency, false); // :178
								fillOverflow(tRecipe); // :179
								if (mTanks[0].isEmpty()) break; // :180
							}
						} else {
							// set remaining Fluid to null, in case the Fuel Type needs to be swapped
							// out. But only if it was inactive for 64 ticks (:183-184).
							if (mActivityData == 0) mTanks[0].setEmpty();
						}
					}
				} else {
					// set remaining Fluid to null, because it is not valid Fuel anymore for
					// whatever reason (:188-189).
					mTanks[0].setEmpty();
				}
			}
		}
		// Out of Fuel I guess (:194).
		if (mEnergy < 8) mEnergy = 0;
		// Output used Liquid to the Front (:196 — the controller's BOTTOM face).
		if (mTanks[1].has()) moveOverflowDown();

		// the activity history shift (the trinary check :48-53, the diesel inline form)
		mActivityData <<= 1;
		if (mActive) mActivityData |= 1;
	}

	/**
	 * The :152-159 target resolver — the tile TWO layers up, entered from its BOTTOM face.
	 * The upstream {@code WD.te(..., SIDE_BOTTOM, T)} DelegatorTileEntity folds to the
	 * plain BlockEntity + the fixed side byte (the {@link ITileEntityEnergy.Util} port
	 * dispatch). Offline-replaceable via {@link #setEmissionTargetsOverride}.
	 */
	protected BlockEntity tileAt(BlockPos aOffset) {
		if (mEmissionTargetsOverride != null) return mEmissionTargetsOverride.get(aOffset);
		if (!hasLevel()) return null;
		return getLevel().getBlockEntity(getBlockPos().offset(aOffset));
	}

	@Nullable
	private java.util.Map<BlockPos, BlockEntity> mEmissionTargetsOverride = null;

	/** The offline seam: the :152-159 targets keyed by their ring offset (a null entry = no target). */
	public void setEmissionTargetsOverride(@Nullable java.util.Map<BlockPos, BlockEntity> aTargets) {
		mEmissionTargetsOverride = aTargets;
	}

	/**
	 * The :168/:171 tank argument — the FluidStack snapshot of the fuel tank (the machine's
	 * tankSnapshot form: the frozen P4 Recipe takes FluidStack[], the upstream
	 * IFluidTank[] drains in place).
	 */
	private FluidStack[] fuelSnapshot() {
		FluidStack tFluid = mTanks[0].getFluid();
		//? if forge {
		return tFluid != null && !tFluid.isEmpty() ? new FluidStack[] {new FluidStack(tFluid, FluidTankGT.bindInt(mTanks[0].amount()))} : new FluidStack[0];
		//?} else {
		/*return tFluid != null && !tFluid.isEmpty() ? new FluidStack[] {tFluid.copyWithAmount(FluidTankGT.bindInt(mTanks[0].amount()))} : new FluidStack[0]; // 21.1: no copy ctor
		 *///?}
	}

	/**
	 * The :171/:177 consume — probe then drain the REAL tank (the upstream
	 * {@code isRecipeInputEqual(T, F, mTanks[0].AS_ARRAY, ZL_IS)} drained in place; the
	 * port adapter mirrors the machine's applyTankConsumption: the snapshot consumes,
	 * the verified amounts drain the source).
	 */
	private boolean consumeFuel(Recipe aRecipe) {
		FluidStack[] tSnapshot = fuelSnapshot();
		if (!aRecipe.isRecipeInputEqual(true, false, tSnapshot)) return false;
		for (FluidStack tInput : aRecipe.mFluidInputs) {
			if (tInput != null && !tInput.isEmpty()) mTanks[0].drain(tInput.getAmount(), FluidAction.EXECUTE);
		}
		return true;
	}

	/** The :166 gate — {@code !mTanks[1].has(mRate * 20)}: the overflow must not yet hold a full further share. */
	private boolean overflowHolds(long aAmount) {
		return mTanks[1].has(aAmount);
	}

	/** The :170 gate — the overflow can take the WHOLE row output (the canFillAll face via the SIMULATE probe). */
	private boolean overflowAccepts(Recipe aRecipe) {
		if (aRecipe.mFluidOutputs.length <= 0) return true; // :170 — an output-less row passes
		FluidStack tOutput = aRecipe.mFluidOutputs[0];
		return mTanks[1].fill(tOutput, FluidAction.SIMULATE) >= tOutput.getAmount();
	}

	/** The :175/:179 — the row's first fluid output into the overflow tank. */
	private void fillOverflow(Recipe aRecipe) {
		if (aRecipe.mFluidOutputs.length > 0) mTanks[1].fill(aRecipe.mFluidOutputs[0], FluidAction.EXECUTE);
	}

	/** The :196 — the overflow auto-moves to the adjacent tank below the controller. */
	private void moveOverflowDown() {
		IFluidHandler tTarget = outputTargetAt();
		if (tTarget == null) return;
		FluidStack tDrainable = mTanks[1].drain(Integer.MAX_VALUE, FluidAction.SIMULATE);
		if (tDrainable == null || tDrainable.isEmpty()) return;
		int tMoved = tTarget.fill(tDrainable, FluidAction.EXECUTE);
		if (tMoved > 0) mTanks[1].drain(tMoved, FluidAction.EXECUTE);
	}

	/** The :196 target — the block BELOW the controller, its fluid handler seen from the top. Offline-replaceable. */
	@Nullable
	protected IFluidHandler outputTargetAt() {
		if (mOutputTargetOverride != null) return mOutputTargetOverride;
		if (!hasLevel()) return null;
		BlockEntity tNeighbor = getLevel().getBlockEntity(getBlockPos().below());
		if (tNeighbor == null || tNeighbor.isRemoved()) return null;
		//? if forge {
		return tNeighbor.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP).orElse(null);
		//?} else {
		/*return getLevel().getCapability(Capabilities.FluidHandler.BLOCK, getBlockPos().below(), Direction.UP);
		 *///?}
	}

	@Nullable
	private IFluidHandler mOutputTargetOverride = null;

	/** The offline seam: the :196 output target (a null = no adjacent tank). */
	public void setOutputTargetOverride(@Nullable IFluidHandler aTarget) {
		mOutputTargetOverride = aTarget;
	}

	// ---------------------------------------------------------------------------
	// the tool faces (:201-205 — the RCON command arms call these)
	// ---------------------------------------------------------------------------

	/** The onMagnifyingGlass2 arm (:201-205) — the formed verdict + the two tank contents. */
	public List<String> magnifyingglass() {
		return List.of("Structure is formed already!", "Input: " + mTanks[0].amount() + " L", "Output: " + mTanks[1].amount() + " L");
	}

	// ---------------------------------------------------------------------------
	// the energy face (:251-257 verbatim — emitting HU, top face, mRate everywhere)
	// ---------------------------------------------------------------------------

	@Override
	public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
		return aEmitting && aEnergyType == mEnergyTypeEmitted; // :251
	}

	@Override
	public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {
		return aSide == Direction.UP.get3DDataValue() && super.isEnergyEmittingTo(aEnergyType, aSide, aTheoretical); // :252 SIDES_TOP
	}

	@Override
	public long getEnergyOffered(TagData aEnergyType, byte aSide, long aSize) {
		return Math.min(mRate, mEnergy); // :253
	}

	@Override
	public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {
		return mRate; // :254
	}

	@Override
	public long getEnergySizeOutputMin(TagData aEnergyType, byte aSide) {
		return mRate; // :255 — the Root default Rec/2 would differ
	}

	@Override
	public long getEnergySizeOutputMax(TagData aEnergyType, byte aSide) {
		return mRate; // :256 — the Root default Rec*2 would differ
	}

	@Override
	public Collection<TagData> getEnergyTypes(byte aSide) {
		return mEnergyTypeEmitted.AS_LIST; // :257
	}

	// the :259-261 getStateRunningActively triple has no port face (the
	// ITileEntityRunningActively interface is the unported running-visual pool; the
	// mActive flag stays the BE field the stat/command faces read)

	// ---------------------------------------------------------------------------
	// the fluid capability door (:222-234 — fill = the fuel gate into tank0, drain =
	// the overflow tank, the tank view exposes both; the funnel/tap tool faces are the
	// pool, the capability is the port intake)
	// ---------------------------------------------------------------------------

	/** The :223/:238 containsInput face — does the fuel map know this fluid? */
	public boolean isFuel(FluidStack aFluid) {
		if (aFluid == null || aFluid.isEmpty()) return false;
		for (Recipe tRecipe : recipes().mRecipeList) {
			for (FluidStack tInput : tRecipe.mFluidInputs) {
				if (tInput != null && !tInput.isEmpty() && tInput.isFluidEqual(aFluid)) return true;
			}
		}
		return false;
	}

	//? if forge {
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> aCapability, @Nullable Direction aSide) {
		if (aCapability == ForgeCapabilities.FLUID_HANDLER) {
			// the FRESH per-call side wrapper (the boiler form; the :222 fillable gate has
			// no side half upstream — the intake gating lives on the ONLY_ITEM_FLUID_ENERGY_IN
			// part modes and the door's fuel-only half here)
			return LazyOptional.of(HeatExchangerFluidHandler::new).cast();
		}
		return super.getCapability(aCapability, aSide);
	}
	//?} else {
	/*// 21.1 seam: RegisterCapabilitiesEvent provider wiring delegates to this member; no @Override.
	public <T> T getCapability(BlockCapability<T, Direction> aCapability, @Nullable Direction aSide) {
		if (aCapability == Capabilities.FluidHandler.BLOCK) {
			return (T) new HeatExchangerFluidHandler();
		}
		return null;
	}
	 *///?}

	/**
	 * The door behind the capability: fill = the :223 fuel-only gate into mTanks[0] (a
	 * non-fuel fluid is REFUSED, not destroyed); drain = the :228 overflow tank; the tank
	 * view exposes both (:232).
	 */
	private class HeatExchangerFluidHandler implements IFluidHandler {

		@Override public int getTanks() {return mTanks.length;} // :232
		@Override public FluidStack getFluidInTank(int aTank) {
			FluidStack tStack = (aTank >= 0 && aTank < mTanks.length) ? mTanks[aTank].get() : null;
			return tStack == null ? FluidStack.EMPTY : tStack;
		}
		@Override public int getTankCapacity(int aTank) {return (aTank >= 0 && aTank < mTanks.length) ? mTanks[aTank].getCapacity() : 0;}
		@Override public boolean isFluidValid(int aTank, FluidStack aStack) {
			return aTank == 0 && isFuel(aStack);
		}

		@Override
		public int fill(FluidStack aResource, FluidAction aAction) {
			if (!isFuel(aResource)) return 0; // :223 the containsInput half
			return mTanks[0].fill(aResource, aAction);
		}

		@Override
		public FluidStack drain(FluidStack aResource, FluidAction aAction) { // :228
			if (aResource == null || aResource.isEmpty()) return FluidStack.EMPTY;
			return mTanks[1].drain(aResource.getAmount(), aAction);
		}

		@Override
		public FluidStack drain(int aMaxDrain, FluidAction aAction) { // :228
			return mTanks[1].drain(aMaxDrain, aAction);
		}
	}

	// ---------------------------------------------------------------------------
	// NBT (the upstream :64-84 set; the row config re-derives from the block carrier)
	// ---------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putLong(NBT_ENERGY, mEnergy); // :80
		aNBT.putLong(NBT_OUTPUT, mRate); // the row value rides NBT for a clean round trip
		aNBT.putLong(NBT_ACTIVITY, mActivityData);
		if (mEfficiency != 10000) aNBT.putShort(NBT_EFFICIENCY, mEfficiency); // :92 (no live writer, the load face)
		mTanks[0].writeToNBT(aNBT, NBT_TANK0); // :82-83
		mTanks[1].writeToNBT(aNBT, NBT_TANK1);
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_ENERGY, Tag.TAG_ANY_NUMERIC)) mEnergy = aNBT.getLong(NBT_ENERGY); // :66
		if (aNBT.contains(NBT_OUTPUT, Tag.TAG_ANY_NUMERIC)) setRate(aNBT.getLong(NBT_OUTPUT)); // :68 + :72
		if (aNBT.contains(NBT_EFFICIENCY, Tag.TAG_ANY_NUMERIC)) {
			mEfficiency = (short)Math.max(0, Math.min(10000, aNBT.getShort(NBT_EFFICIENCY))); // :70 UT.Code.bind_
		}
		if (aNBT.contains(NBT_ACTIVITY, Tag.TAG_ANY_NUMERIC)) mActivityData = aNBT.getLong(NBT_ACTIVITY);
		mTanks[0].readFromNBT(aNBT, NBT_TANK0); // :73
		mTanks[1].readFromNBT(aNBT, NBT_TANK1); // :74
		mTanks[0].setCapacity(mRate * 10); // :72 half two (the setCapacity AFTER the read, the boiler :802 form)
	}
}
