package gregtech6.tileentity.multiblocks;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.IntSupplier;

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
import net.minecraft.world.level.material.Fluids;

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
import gregtech6.block.multiblock.GTLargeBoilerBlock;
import gregtech6.fluid.FluidTankGT;
import gregtech6.fluid.GTFluids;
import gregtech6.multiblock.GTMultiBlockPattern;
import gregtech6.registry.GTMultiBlocks;
import gregtech6.tileentity.energy.GTSteamEngineBlockEntity;

/**
 * 1.20.1 counterpart of the GT6 Large Boiler multiblock — task p13-large-boiler, ported
 * from gregtech/tileentity/multiblocks/MultiTileEntityLargeBoiler.java (:65-393) as the
 * W4 controller closing the steam family: a 3x3 Heat-Transmitter base, a 3x3x3 hollow of
 * Dense Walls with FIVE pipe holes, five-target load-balanced steam output and the
 * break-any-wall explosion (HIGH risk).
 *
 * <p><b>The anchor (:98)</b>: {@code tX = getOffsetXN(mFacing), tY = yCoord, tZ =
 * getOffsetZN(mFacing)} — the structure centre sits ONE cell in FRONT of the controller at
 * the SAME layer (no {@code getOffsetYN}: the CokeOven {@code tY} offset folds away, this
 * machine's core is front-and-level, tooltip :145 "Main centered on Side-Bottom of Boiler
 * facing outwards").
 *
 * <p><b>The structure (:97-140 verbatim, per-cell)</b>:
 * <ul>
 * <li>(tX, tY+1, tZ) — the front-upper cell must ALREADY be air (:102 {@code getAir} —
 *     a non-air cell is a check FAILURE, not a silent clear; the {@code setBlockToAir}
 *     on the air cell is the idempotent no-op kept by judging {@code isAir} only);</li>
 * <li>the bottom 3x3 (y-1) — Heat Transmitters, design 0, mode {@link
 *     MultiBlockPartBlockEntity#ONLY_ENERGY_IN} (:104-112, upstream part id 18101);</li>
 * <li>the middle 3x3 (y) — the Boiler Walls, design 0, mode {@link
 *     MultiBlockPartBlockEntity#ONLY_FLUID_IN} (:114-122; the controller's own cell is
 *     inside this 3x3 and passes through the checkAndSetTarget :48-49 self-cell arm);</li>
 * <li>the top centre (y+2) — a wall, design 1, ONLY_FLUID_OUT (:124) — design 1 IS the
 *     pipe-hole marker;</li>
 * <li>the two upper rings (y+1, y+2) — walls, ONLY_FLUID_OUT (:125-135), with the FOUR
 *     middle-layer side cells (N/W/E/S of the ring at y+1, :127/:129/:131/:133) marked
 *     design 1 = the other four pipe holes. The five design-1 cells are exactly the five
 *     output ports of the :213-218 delegator table.</li>
 * </ul>
 * The mode/design face is the {@link ITileEntityMultiBlockController.Util#checkAndSetTarget}
 * write (the part-mode carrier, the CokeOven shape) — the {@link GTMultiBlockPattern}
 * binding below is display-only data whose predicates cannot express modes (the pattern
 * seam ruling: structure judgment rides THIS hand-written check, the predicate seam stays
 * frozen).
 *
 * <p><b>The fields (:66-71)</b>: water tank 128000 L + steam tank, the :80 double
 * assignment {@code mTanks[1].setCapacity(mCapacity = mOutput * 10000)} — the steam tank
 * AND the overheat ceiling re-derive from mOutput together (the lesson-227 double-assign
 * discipline: both variables verified against :80). The class defaults (mOutput 2048,
 * mCapacity 20480000, walls 18002) are the defensive pre-row values; every Loader row
 * (:1248-1252, re-read verbatim) specifies its own NBT_DESIGN wall and NBT_OUTPUT_SU,
 * riding the block carrier ({@link GTMultiBlocks.LargeBoilerRow}, the BoilerTankBlock
 * form) — the upstream NBT_DESIGN/NBT_OUTPUT_SU registration NBT becomes the row.
 *
 * <p><b>The tick (:176-265)</b>: the conversion (:180-190) is the W3 verbatim shape with
 * the LARGE-BOILER constants — the same triple min, the same rng(10) scale decrement with
 * the 5000 floor and the distilled-water immunity (:183-186), the yield {@code units(
 * conversions, 10000, mEfficiency * 160, F)} (the GLOBAL constant, CS.java:242). The
 * cooldown (:193-201) bleeds {@code (mOutput * 64) / STEAM_PER_EU} HU and trashes
 * {@code mOutput * 64} L of steam per tick after 128 quiet ticks. The barometer (:259) is
 * the 5-bit visual. The explosion verdict (:262-264): {@code (mBarometer > 4 &&
 * !checkStructure(false)) || mEnergy > mCapacity || mTanks[1].isFull()} — the structural
 * probe runs HERE in the tick (upstream has no onBlockUpdate arm; breaking any structure
 * wall flips mStructureChanged through the part-block playerWillDestroy chain and the
 * next tick's checkStructure detonates). NO dry-burn explosion (the W3 negative ruling).
 *
 * <p><b>The five-target load-balanced output (:203-256, verbatim five-Delegator
 * algorithm)</b>: above HALF tank the drainable offer is {@code bindInt(min(tAmount >
 * capacity/4 ? mOutput * 2 : mOutput, tAmount))} (:207 — the 3/4 double rate). The five
 * targets (:213-218) sit one cell OUTSIDE each pipe hole, consulted through the face
 * pointing back into the hole (SIDE_Y_NEG / X_POS / X_NEG / Z_POS / Z_NEG — the GT6 side
 * byte IS the {@code Direction.get3DDataValue()}, so the delegator table maps 1:1 onto
 * capability queries at DOWN/EAST/WEST/SOUTH/NORTH). Then the upstream algorithm verbatim:
 * one target → the whole offer (:225-229); several targets with a total acceptance above
 * the offer (:231) → the small-target sweep at the per-target quota (:233-239), the
 * single-survivor arm (:240-244), the even split of the remainder (:245-249); a total
 * acceptance below the offer → everyone drains to their simulated maximum (:251-253).
 * The offer that stays un-moved stays in the tank (upstream FL.move_ semantics: the
 * return value is what actually moved).
 *
 * <p><b>The explosion family (HIGH risk)</b>: the strength is the upstream :326 formula
 * {@code 2 + max(1, sqrt(steam litres) / 1000)} (the Large-Boiler override of the
 * strength-4 default — note the W3 single-block boiler divides by 100 with no base
 * offset; the two formulas are NOT interchangeable). The pressurised dismantle
 * (:313-316 removedByPlayer → barometer &gt; 4, non-creative → explode(T)) rides the
 * controller block's playerWillDestroy + the /gt6multiblock boiler dismantle arm; the
 * caught-in-a-neighbour's-explosion second blast (:318-322) rides the block's
 * onBlockExploded; the chisel detonation (:282-283, barometer &gt; 15) rides
 * {@link #chisel}. The execution is the Root vanilla bridge (p8-d3, the W3 reuse ruling).
 *
 * <p><b>The energy face (:367-378)</b>: HU ONLY, accepting from EVERY side, never
 * emitting (the Root :338 delegation carries the accepting gate); the doInject door
 * (:370) accumulates and re-arms the cooldown to max(, 32); demand/recommended = mOutput
 * / 2 (:371-372), min 1 / max Long.MAX_VALUE (:373-374). The stored/capacity reporting
 * (:375-376) rides the stat/thermometer surface — the capacitor half is the cut ADR-D1
 * subsystem (the W3 ruling).
 *
 * <p><b>The fluid face (:380-385)</b>: the tank VIEW exposes BOTH tanks on every face
 * (the :382/:385 getFluidTanks — the pipe-connect handshake queries it); fill = the
 * FL.water gate into the water tank (:380/:383 — NOTE: no face gate here, unlike the W3
 * single-block boiler's SIDES_BOTTOM_HORIZONTAL mask; the large boiler's intake is gated
 * by the PART modes — only the ONLY_FLUID_IN middle-ring walls relay fluid upstream, and
 * the 1.20.1 part capability relay (task p4) carries no mode gate, the port-consistent
 * shape); drain = the steam tank (:381/:384 — steam IS drainable through the capability,
 * unlike the W3 machine whose drain stays null).
 *
 * <p><b>NO GUI by census</b> (:150-166 addToolTips has no GUI line, the multiblock
 * controller tooltip is the contract); the Gibbl display face (:387-388) is pooled (the
 * W3 declaration); the contact-heat-damage half of the chisel arm needs a live player
 * (the W3 live-only ruling); the barometer texture overlay (:357-361) is the render pool.
 */
public class TileEntityLargeBoiler extends TileEntityBase10MultiBlockBase implements ITileEntityEnergy {

	/** The NBT keys (the W3 generator spelling convention; visual = the synced gauge byte). */
	public static final String NBT_ENERGY = "gt.energy";
	public static final String NBT_VISUAL = "gt.visual";
	public static final String NBT_OUTPUT = "gt.output";
	public static final String NBT_EFFICIENCY = "gt.efficiency";
	public static final String NBT_TANK0 = "gt.tank0";
	public static final String NBT_TANK1 = "gt.tank1";

	/** The pipe-hole design marker (upstream :124/:127/:129/:131/:133 — the third checkAndSetTarget argument). */
	public static final int DESIGN_PIPE_HOLE = 1;

	/** The five output faces of the delegator table (:214-218), in upstream order. */
	static final Direction[] STEAM_TARGET_SIDES = {
			Direction.DOWN, // SIDE_Y_NEG — above the top-centre hole
			Direction.EAST, // SIDE_X_POS — west of the west hole
			Direction.WEST, // SIDE_X_NEG — east of the east hole
			Direction.SOUTH, // SIDE_Z_POS — north of the north hole
			Direction.NORTH  // SIDE_Z_NEG — south of the south hole
	};

	/** The barometer (:67, the synced 5-bit pressure gauge). */
	public byte mBarometer = 0;
	/** The o-pair of the :330-333 change check. */
	private byte oBarometer = 0;
	/** The scale/calcification state (:68, ten-thousandths, the 5000 floor). */
	public short mEfficiency = 10000;
	/** The cooldown timer (:68; 128 set per conversion :189, max(,32) per injection :370). */
	public short mCoolDownResetTimer = 128;
	/** The stored heat (:69, NBT_ENERGY :76). */
	public long mEnergy = 0;
	/** The heat ceiling (:69 default 20480000 — re-derived with the row, the :80 double assignment). */
	public long mCapacity = 20480000;
	/** The nominal steam output (:69 default 2048; the raw NBT_OUTPUT_SU row value, :79). */
	public long mOutput = 2048;
	/** The accepted energy type (:70 — every Loader large-boiler row is HU). */
	public TagData mEnergyTypeAccepted = TD.Energy.HU;
	/** The two tanks (:71): [0] water 128000 L, [1] steam capacity mOutput * 10000 (:80). */
	public final FluidTankGT[] mTanks = new FluidTankGT[] {new FluidTankGT(128000), new FluidTankGT(2048000)};

	// ---------------------------------------------------------------------------
	// the offline identity seams (the W3 form: the gt6 fluids do not exist offline)
	// ---------------------------------------------------------------------------

	/** The steam identity (the conversion product + the output offer identity, upstream FL.Steam.make). */
	public Function<Long, FluidStack> mSteamMake = aAmount -> new FluidStack(GTFluids.STEAM.source.get(), (int)Math.min(Integer.MAX_VALUE, Math.max(1, aAmount.longValue())));

	/** The distilled-water identity (the :183 scale immunity, upstream FL.distw). */
	public Function<Fluid, Boolean> mDistwMatch = f -> f == GTFluids.DISTILLED_WATER.source.get();

	/** The water identity (upstream FL.water — vanilla water, offline-safe). */
	public Function<Fluid, Boolean> mWaterMatch = f -> f == Fluids.WATER;

	/** The live rng (:183 — the world Random; the generator rng seam pattern). */
	protected int rng(int aRange) {
		if (aRange <= 0) return 0;
		if (mRngOverride != null) return mRngOverride.getAsInt();
		return hasLevel() ? getLevel().random.nextInt(aRange) : 0;
	}

	@Nullable
	private IntSupplier mRngOverride = null;

	/** The offline rng seam. */
	public void setRngOverride(@Nullable IntSupplier aRng) {
		mRngOverride = aRng;
	}

	/** The offline fluid seam setter (the W3 setFluidSeams form). */
	public void setFluidSeams(@Nullable Function<Long, FluidStack> aSteamMake, @Nullable Function<Fluid, Boolean> aDistwMatch, @Nullable Function<Fluid, Boolean> aWaterMatch) {
		if (aSteamMake != null) mSteamMake = aSteamMake;
		if (aDistwMatch != null) mDistwMatch = aDistwMatch;
		if (aWaterMatch != null) mWaterMatch = aWaterMatch;
	}

	// ---------------------------------------------------------------------------
	// construction (the BET-factory + block-carrier row shape)
	// ---------------------------------------------------------------------------

	/** BET factory for BlockEntityType.Builder.of — resolves the shared type through the registry at runtime. */
	public TileEntityLargeBoiler(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/**
	 * Full constructor — also the offline (test) entry point: a null type falls back to the
	 * shared registry type at runtime. A placed variant block injects its registration row
	 * (the upstream :79 NBT_OUTPUT_SU + :77 NBT_DESIGN wall read); foreign blocks (the
	 * offline fixtures) keep the class defaults.
	 */
	public TileEntityLargeBoiler(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType != null ? aType : GTMultiBlocks.LARGE_BOILER_BE.get(), aPos, aState);
		if (aState.getBlock() instanceof GTLargeBoilerBlock tBlock) {
			setOutput(tBlock.row().outputSteamPerTick()); // :79-80
		}
	}

	@Override
	public String getTileEntityName() {
		return "multiblock_large_boiler"; // BET registry path mirrors it
	}

	/**
	 * The Boiler Wall block this variant is built from (upstream :66 mBoilerWalls + :77
	 * NBT_DESIGN — the wall MTE id pair becomes the Block identity carried by the variant
	 * row; the class default 18002 is the upstream defensive value no Loader row relies on).
	 * A hook so the offline tests bind a fixture block without the frozen-registry dance.
	 */
	protected Block getWallBlock() {
		BlockState tState = getBlockState();
		if (tState.getBlock() instanceof GTLargeBoilerBlock tBlock) {
			return tBlock.wallBlock();
		}
		return GTMultiBlocks.WALL_BLOCKS_BY_PATH.get(GTMultiBlocks.WALL_ROWS.get(0).path()).get(); // the SS Dense Wall defensive default
	}

	/** The Heat Transmitter part block (upstream part id 18101, :104-112). */
	protected Block getTransmitterBlock() {
		return GTMultiBlocks.HEAT_TRANSMITTER.get();
	}

	/** The :80 double assignment — the steam tank AND the overheat ceiling pair with mOutput. */
	public long steamTankCapacity() {
		return mOutput * 10000;
	}

	/** The output setter (the row load + the offline tests) — the upstream :80 verbatim. */
	public void setOutput(long aOutput) {
		mOutput = Math.max(1, aOutput);
		mCapacity = steamTankCapacity(); // :80 — mCapacity = mOutput * 10000 (the double assignment, lesson 227②)
		mTanks[1].setCapacity(mCapacity);
	}

	// ---------------------------------------------------------------------------
	// the structure (:97-173 verbatim)
	// ---------------------------------------------------------------------------

	/** Upstream :97-140 verbatim, per cell. */
	@Override
	public boolean checkStructure2(@Nullable BlockPos aCoordinates, @Nullable Player aPlayer, @Nullable Container aInventory) {
		int tX = getOffsetXN(mFacing), tY = getBlockPos().getY(), tZ = getOffsetZN(mFacing); // :98 — NO getOffsetYN
		if (!hasLevel()) return mStructureOkay;
		if (getLevel().isLoaded(new BlockPos(tX - 1, tY, tZ - 1)) && getLevel().isLoaded(new BlockPos(tX + 1, tY, tZ - 1))
				&& getLevel().isLoaded(new BlockPos(tX - 1, tY, tZ + 1)) && getLevel().isLoaded(new BlockPos(tX + 1, tY, tZ + 1))) { // :99
			boolean tSuccess = true;

			// :102 — the front-upper cell must ALREADY be air (a non-air cell FAILS, not clears;
			// the upstream setBlockToAir on an air cell is the idempotent no-op this judges)
			if (!getLevel().getBlockState(new BlockPos(tX, tY + 1, tZ)).isAir()) tSuccess = false;

			// :104-112 — the bottom 3x3, Heat Transmitters, ONLY_ENERGY_IN
			for (int tDZ = -1; tDZ <= 1; tDZ++) for (int tDX = -1; tDX <= 1; tDX++) {
				if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX + tDX, tY - 1, tZ + tDZ,
						getTransmitterBlock(), 0, MultiBlockPartBlockEntity.ONLY_ENERGY_IN, aCoordinates, aPlayer, aInventory)) tSuccess = false;
			}

			// :114-122 — the middle 3x3, Boiler Walls, ONLY_FLUID_IN (the controller's own cell
			// is inside this 3x3 and passes via the checkAndSetTarget self-cell arm :48-49)
			for (int tDZ = -1; tDZ <= 1; tDZ++) for (int tDX = -1; tDX <= 1; tDX++) {
				if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX + tDX, tY, tZ + tDZ,
						getWallBlock(), 0, MultiBlockPartBlockEntity.ONLY_FLUID_IN, aCoordinates, aPlayer, aInventory)) tSuccess = false;
			}

			// :124 — the top centre, design 1 (THE pipe hole), ONLY_FLUID_OUT
			if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(this, tX, tY + 2, tZ,
					getWallBlock(), DESIGN_PIPE_HOLE, MultiBlockPartBlockEntity.ONLY_FLUID_OUT, aCoordinates, aPlayer, aInventory)) tSuccess = false;

			// :125-135 — the two upper rings, ONLY_FLUID_OUT; the four side-middles of the FIRST
			// ring (:127/:129/:131/:133) carry design 1 (the other four pipe holes)
			for (int i = 1; i < 3; i++) {
				if (!checkWall(this, tX - 1, tY + i, tZ - 1, i, aCoordinates, aPlayer, aInventory)) tSuccess = false; // :126/:132 corner
				if (!checkWall(this, tX    , tY + i, tZ - 1, i, aCoordinates, aPlayer, aInventory)) tSuccess = false; // :127/:133 hole at i==1
				if (!checkWall(this, tX + 1, tY + i, tZ - 1, i, aCoordinates, aPlayer, aInventory)) tSuccess = false; // :128/:134 corner
				if (!checkWall(this, tX - 1, tY + i, tZ    , i, aCoordinates, aPlayer, aInventory)) tSuccess = false; // :129 hole at i==1
				if (!checkWall(this, tX + 1, tY + i, tZ    , i, aCoordinates, aPlayer, aInventory)) tSuccess = false; // :131 hole at i==1
				if (!checkWall(this, tX - 1, tY + i, tZ + 1, i, aCoordinates, aPlayer, aInventory)) tSuccess = false; // corner
				if (!checkWall(this, tX    , tY + i, tZ + 1, i, aCoordinates, aPlayer, aInventory)) tSuccess = false; // hole at i==1
				if (!checkWall(this, tX + 1, tY + i, tZ + 1, i, aCoordinates, aPlayer, aInventory)) tSuccess = false; // corner
			}

			return tSuccess; // :137
		}
		return mStructureOkay; // :139 — unloaded corners keep the last verdict
	}

	/** One :126-135 ring cell — design 1 exactly on the four side-middles of the first ring. */
	private boolean checkWall(TileEntityLargeBoiler aBoiler, int aX, int aY, int aZ, int aRing, @Nullable BlockPos aCoordinates, @Nullable Player aPlayer, @Nullable Container aInventory) {
		int tDesign = (aRing == 1 && (aX == getOffsetXN(mFacing) || aZ == getOffsetZN(mFacing))) ? DESIGN_PIPE_HOLE : 0;
		return ITileEntityMultiBlockController.Util.checkAndSetTarget(aBoiler, aX, aY, aZ, getWallBlock(), tDesign,
				MultiBlockPartBlockEntity.ONLY_FLUID_OUT, aCoordinates, aPlayer, aInventory);
	}

	/** Upstream :170-173 verbatim — the box around the anchor, y from -1 to +2. */
	@Override
	public boolean isInsideStructure(int aX, int aY, int aZ) {
		int tX = getOffsetXN(mFacing), tY = getBlockPos().getY(), tZ = getOffsetZN(mFacing);
		return aX >= tX - 1 && aY >= tY - 1 && aZ >= tZ - 1 && aX <= tX + 1 && aY <= tY + 2 && aZ <= tZ + 1;
	}

	/**
	 * The declared structure pattern (the p12 binding, display data): the 34 wall +
	 * transmitter cells in the :104-135 check order plus the hollow air cell above the
	 * anchor (:102). The predicates judge BLOCK IDENTITY only — the ONLY_* part modes and
	 * the design-1 pipe-hole markers are the checkAndSetTarget write, not pattern data (the
	 * pattern-seam ruling: structure judgment rides checkStructure2, the seam stays frozen).
	 */
	@Override
	@Nullable
	public GTMultiBlockPattern getStructurePattern() {
		if (mStructurePattern == null) {
			GTMultiBlockPattern.Builder tBuilder = GTMultiBlockPattern.builder();
			java.util.function.Predicate<BlockState> tWall = GTMultiBlockPattern.is(getWallBlock());
			java.util.function.Predicate<BlockState> tTransmitter = GTMultiBlockPattern.is(getTransmitterBlock());
			// :104-112 — the bottom 3x3 (y -1)
			for (int tDZ = -1; tDZ <= 1; tDZ++) for (int tDX = -1; tDX <= 1; tDX++) tBuilder.part(tDX, -1, tDZ, tTransmitter);
			// :114-122 — the middle 3x3 (y 0)
			for (int tDZ = -1; tDZ <= 1; tDZ++) for (int tDX = -1; tDX <= 1; tDX++) tBuilder.part(tDX, 0, tDZ, tWall);
			// :124 + :125-135 — the top centre and the two rings (y +1..+2)
			tBuilder.part(0, 2, 0, tWall);
			for (int i = 1; i < 3; i++) {
				tBuilder.part(-1, i, -1, tWall); tBuilder.part(0, i, -1, tWall); tBuilder.part(1, i, -1, tWall);
				tBuilder.part(-1, i, 0, tWall);  tBuilder.part(1, i, 0, tWall);
				tBuilder.part(-1, i, 1, tWall);  tBuilder.part(0, i, 1, tWall); tBuilder.part(1, i, 1, tWall);
			}
			// :102 — the hollow air cell above the anchor, appended LAST (the CokeOven draw order)
			tBuilder.hollow(0, 1, 0, GTMultiBlockPattern.AIR);
			mStructurePattern = tBuilder.build();
		}
		return mStructurePattern;
	}

	@Nullable
	private GTMultiBlockPattern mStructurePattern = null;

	// ---------------------------------------------------------------------------
	// the tick (:176-265 verbatim)
	// ---------------------------------------------------------------------------

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		super.onTick(aTimer, aIsServerSide); // :177 — the 600-tick structure poll rides the base
		if (!aIsServerSide) return;

		// Convert Water to Steam — :180-190
		long tConversions = Math.min(mTanks[1].capacity() / 2560, Math.min(mEnergy / GTFluids.EU_PER_WATER, mTanks[0].amount()));
		if (tConversions > 0) {
			mTanks[0].remove(tConversions); // :182
			if (rng(10) == 0 && mEfficiency > 5000 && mTanks[0].has() && !mDistwMatch.apply(mTanks[0].getFluid().getFluid())) { // :183
				mEfficiency -= tConversions; // :184
				if (mEfficiency < 5000) mEfficiency = 5000; // :185
			}
			// :187 — the steam goes IN; the add clamps at capacity (the W3 ruling: the upstream
			// setFluid could overshoot, the isFull() explosion verdict below is the same either way)
			mTanks[1].add(GTSteamEngineBlockEntity.units(tConversions, 10000, (long)mEfficiency * GTFluids.STEAM_PER_WATER_GLOBAL, false), mSteamMake.apply(tConversions));
			mEnergy -= tConversions * GTFluids.EU_PER_WATER; // :188
			mCoolDownResetTimer = 128; // :189
		}

		// Remove Steam and Heat during the process of cooling down — :193-201
		if (mCoolDownResetTimer-- <= 0) {
			mCoolDownResetTimer = 0; // :194
			mEnergy -= (mOutput * 64) / GTFluids.STEAM_PER_EU; // :195
			mTanks[1].remove(mOutput * 64); // :196 — the GarbageGT.trash direct disposal (no world emission)
			if (mEnergy <= 0) { // :197
				mEnergy = 0; // :198
				mCoolDownResetTimer = 128; // :199
			}
		}

		long tAmount = mTanks[1].amount() - mTanks[1].capacity() / 2; // :203

		// Emit Steam — :205-256 (the five-target load balance)
		if (tAmount > 0) {
			emitSteam(tAmount); // the :207-255 verbatim body, factored for the offline truth table
		}

		// Set Barometer — :259
		mBarometer = (byte)GTSteamEngineBlockEntity.scale(mTanks[1].amount(), mTanks[1].capacity(), 31, false);

		// Well the Boiler gets structural Damage when being too hot, or when being too full of Steam — :262-264
		if ((mBarometer > 4 && !checkStructure(false)) || mEnergy > mCapacity || mTanks[1].isFull()) {
			explode(false);
		}
	}

	/** The :206-255 emit body — package-visible for the offline truth-table fixture. */
	void emitSteam(long tAmount) {
		// :207 — the offer: the 3/4 double rate, clamped to the amount above half, int-bound
		int tOfferAmount = FluidTankGT.bindInt(Math.min(tAmount > mTanks[1].capacity() / 4 ? mOutput * 2 : mOutput, tAmount));
		if (tOfferAmount <= 0) return;
		FluidStack tDrainableSteam = mSteamMake.apply((long)tOfferAmount);
		if (tDrainableSteam == null || tDrainableSteam.isEmpty()) return;

		// :213-218 — the five targets, one cell outside each pipe hole, consulted through the
		// face pointing back INTO the hole (the GT6 side byte == Direction.get3DDataValue())
		IFluidHandler[] tDelegators = new IFluidHandler[5];
		long[] tTargetAmounts = new long[5];
		int tTargets = 0;
		for (int i = 0; i < 5; i++) {
			IFluidHandler tHandler = steamTargetAt(i);
			// :223 — the SIMULATE probe: a target that would take nothing is dropped
			if (tHandler != null && (tTargetAmounts[i] = tHandler.fill(copyOf(tDrainableSteam, tOfferAmount), FluidAction.SIMULATE)) > 0) {
				tDelegators[i] = tHandler;
				tTargets++;
			} else {
				tDelegators[i] = null; // :223 else-arm
			}
		}

		if (tTargets == 1) {
			// :225-229 — the single target takes the whole offer
			for (int i = 0; i < 5; i++) if (tDelegators[i] != null) {
				moveSteam(tDelegators[i], tOfferAmount);
				break;
			}
		} else if (tTargets > 1 && tOfferAmount >= tTargets) { // :230
			if (sum(tTargetAmounts) > tOfferAmount) { // :231
				// :232-250 — the acceptance-fair split
				int tMoveable = tOfferAmount, tOriginalTargets = tTargets; // :232
				for (int i = 0; i < 5; i++) if (tDelegators[i] != null) { // :233
					if (tTargetAmounts[i] <= tOfferAmount / tOriginalTargets) { // :234 — the small-target sweep
						tMoveable -= moveSteam(tDelegators[i], tOfferAmount / tOriginalTargets); // :235
						tDelegators[i] = null; // :236
						if (--tTargets < 2) break; // :237
					}
				}
				if (tTargets == 1) { // :240 — the single survivor takes the remainder
					for (int i = 0; i < 5; i++) if (tDelegators[i] != null) {
						moveSteam(tDelegators[i], tMoveable); // :242
						break;
					}
				} else if (tTargets > 1 && tMoveable >= tTargets) { // :245 — the even split of the remainder
					for (int i = 0; i < 5; i++) if (tDelegators[i] != null) {
						tMoveable -= moveSteam(tDelegators[i], tMoveable / tTargets); // :247
						if (--tTargets < 1) break; // :248
					}
				}
			} else {
				// :251-253 — the total acceptance does not exceed the offer: everyone drains out
				for (int i = 0; i < 5; i++) if (tDelegators[i] != null) moveSteam(tDelegators[i], tTargetAmounts[i]);
			}
		}
	}

	/** The :223/:235/:242/:247/:252 {@code FL.move_(mTanks[1], target, amount)} — returns what actually moved. */
	private long moveSteam(IFluidHandler aTarget, long aAmount) {
		if (aTarget == null || aAmount <= 0) return 0;
		int tOfferInt = FluidTankGT.bindInt(aAmount);
		if (tOfferInt <= 0) return 0;
		int tAccepted = aTarget.fill(copyOf(mSteamMake.apply((long)tOfferInt), tOfferInt), FluidAction.EXECUTE);
		if (tAccepted > 0) mTanks[1].remove(tAccepted);
		return tAccepted;
	}

	/** A fresh stack copy at a bound amount (the FluidStack immutability discipline). */
	private FluidStack copyOf(FluidStack aTemplate, int aAmount) {
		return new FluidStack(aTemplate.getFluid(), aAmount);
	}

	private static long sum(long[] aValues) {
		long rSum = 0;
		for (long tValue : aValues) rSum += tValue;
		return rSum; // UT.Code.sum :231
	}

	/**
	 * The per-tick target resolver (:213-218): the block one cell outside pipe hole
	 * {@code aIndex}, consulted through the face pointing back INTO the hole. Offline-
	 * replaceable via {@link #setSteamTargetsOverride} (the W3 mFluidAdjacency seam shape).
	 */
	protected IFluidHandler steamTargetAt(int aIndex) {
		if (mSteamTargetsOverride != null) return mSteamTargetsOverride[aIndex];
		if (!hasLevel()) return null;
		int tX = getOffsetXN(mFacing), tY = getBlockPos().getY(), tZ = getOffsetZN(mFacing);
		BlockPos tPos = switch (aIndex) {
			case 0 -> new BlockPos(tX, tY + 3, tZ);      // :214 — above the top-centre hole
			case 1 -> new BlockPos(tX - 2, tY + 1, tZ);  // :215 — west of the west hole
			case 2 -> new BlockPos(tX + 2, tY + 1, tZ);  // :216 — east of the east hole
			case 3 -> new BlockPos(tX, tY + 1, tZ - 2);  // :217 — north of the north hole
			default -> new BlockPos(tX, tY + 1, tZ + 2); // :218 — south of the south hole
		};
		BlockEntity tNeighbor = getLevel().getBlockEntity(tPos);
		if (tNeighbor == null || tNeighbor.isRemoved()) return null;
		//? if forge {
		return tNeighbor.getCapability(ForgeCapabilities.FLUID_HANDLER, STEAM_TARGET_SIDES[aIndex]).orElse(null);
		//?} else {
		/*return getLevel().getCapability(Capabilities.FluidHandler.BLOCK, tPos, STEAM_TARGET_SIDES[aIndex]);
		 *///?}
	}

	@Nullable
	private IFluidHandler[] mSteamTargetsOverride = null;

	/** The offline seam: the five handlers in :213-218 order (a null slot = no target). */
	public void setSteamTargetsOverride(@Nullable IFluidHandler[] aHandlers) {
		mSteamTargetsOverride = aHandlers;
	}

	// ---------------------------------------------------------------------------
	// the tool faces (:268-300 — the RCON command arms call these)
	// ---------------------------------------------------------------------------

	/** The TOOL_plunger arm (:275-278) — the water tank first, else the steam tank. */
	public long plunger() {
		if (mTanks[0].has()) { // :276
			long tTrashed = mTanks[0].amount();
			mTanks[0].setEmpty();
			return tTrashed;
		}
		long tTrashed = mTanks[1].amount(); // :277
		mTanks[1].setEmpty();
		return tTrashed;
	}

	/**
	 * The TOOL_chisel arm (:279-293). Returns the repair value (0 = nothing to do / the
	 * explosion branch). Above 15/31 pressure the chisel DETONATES the boiler (:282-283,
	 * deferred explode(F)); otherwise the tank vents and the scale and the heat reset
	 * (:286-288) and the :285 heat damage lands on a LIVE player (the RCON path passes null
	 * and skips the damage arm — the W3 ruling).
	 */
	public int chisel(@Nullable Player aPlayer) {
		int rResult = 10000 - mEfficiency; // :280
		if (rResult > 0) { // :281
			if (mBarometer > 15) { // :282
				explode(false); // :283
			} else {
				if (mEnergy + mTanks[1].amount() / GTFluids.STEAM_PER_EU > 2000 && aPlayer != null) { // :285
					aPlayer.hurt(aPlayer.damageSources().onFire(), (mEnergy + mTanks[1].amount() / 2) / 2000.0F);
				}
				mTanks[1].setEmpty(); // :286
				mEfficiency = 10000; // :287
				mEnergy = 0; // :288
				return rResult; // :289
			}
		}
		return 0; // :292
	}

	/** The TOOL_thermometer arm (:295-298) — the stored-heat readout line. */
	public String thermometer() {
		return "Stored Heat Units: " + mEnergy + " / " + mCapacity + " HU"; // :296
	}

	/** The onMagnifyingGlass2 arm (:303-310) — the calcification + water-warning lines. */
	public java.util.List<String> magnifyingglass() {
		String tWater = mTanks[0].has() ? "Water: " + mTanks[0].amount() + "/" + mTanks[0].capacity() + " L" : "WARNING: NO WATER!!!"; // :309
		if (mEfficiency < 10000) { // :304
			return java.util.List.of("Calcification: " + (10000 - mEfficiency) / 100 + "%", tWater); // :305
		}
		return java.util.List.of("No Calcification in this Boiler", tWater); // :307
	}

	// ---------------------------------------------------------------------------
	// the explosion family (HIGH risk — the W3 form over the Root vanilla bridge)
	// ---------------------------------------------------------------------------

	/** Upstream :325-327 — the strength is {@code 2 + max(1, sqrt(steam litres) / 1000)}. */
	@Override
	public void explode(boolean aInstant) {
		explode(aInstant, 2 + Math.max(1, Math.sqrt(mTanks[1].amount()) / 1000.0)); // :326
	}

	/**
	 * Upstream :318-322 — the caught-in-an-explosion second blast (barometer &gt; 4, server
	 * side, instant). Called from the controller block's onBlockExploded BEFORE the air swap
	 * removes this BE (the W3 ruling).
	 */
	public void onExploded() {
		if (isServerSide() && mBarometer > 4) explode(true); // :321
	}

	/**
	 * Upstream :313-316 removedByPlayer — the pressurised-dismantle arm. {@code aPlayer}
	 * null = the RCON acceptance channel (the non-creative counterfactual). Returns whether
	 * the boiler exploded; the caller removes the block afterwards either way (the :315
	 * setBlockToAir).
	 */
	public boolean dismantle(@Nullable Player aPlayer) {
		boolean tCreative = aPlayer != null && aPlayer.isCreative(); // :314 UT.Entities.isCreative
		if (isServerSide() && !tCreative && mBarometer > 4) {
			explode(true); // :314 — the T instant form
			return true;
		}
		return false;
	}

	// ---------------------------------------------------------------------------
	// the sync checks (:330-344 — the barometer byte is the sync payload)
	// ---------------------------------------------------------------------------

	@Override
	public boolean onTickCheck(long aTimer) {
		mBarometer = bind5(mBarometer); // :331
		return mBarometer != oBarometer || super.onTickCheck(aTimer); // :332
	}

	@Override
	public void onTickResetChecks(long aTimer, boolean aIsServerSide) {
		super.onTickResetChecks(aTimer, aIsServerSide);
		oBarometer = mBarometer; // :338
	}

	/** UT.Code.bind5 (the :331 local, the W3 static re-call form). */
	public static byte bind5(byte aValue) {
		return (byte)Math.max(0, Math.min(31, aValue));
	}

	// ---------------------------------------------------------------------------
	// the energy face (:367-378 verbatim)
	// ---------------------------------------------------------------------------

	@Override
	public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
		return !aEmitting && aEnergyType == mEnergyTypeAccepted; // :367 — HU only, never emitting
	}

	/**
	 * Upstream :370 — the injection door (the Root :323 extension hook). Accumulation ONLY:
	 * no world writes, no explosion — the overheat reaction is the tick's buffered explode.
	 */
	@Override
	public long doInject(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
		if (aDoInject) { // :370
			mEnergy += Math.abs(aAmount * aSize);
			mCoolDownResetTimer = (short)Math.max(mCoolDownResetTimer, 32);
		}
		return aAmount;
	}

	@Override
	public long getEnergyDemanded(TagData aEnergyType, byte aSide, long aSize) {
		return mOutput / 2; // :371
	}

	@Override
	public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {
		return mOutput / 2; // :372
	}

	@Override
	public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {
		return 1; // :373 — the Root default Rec/2 would differ
	}

	@Override
	public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {
		return Long.MAX_VALUE; // :374 — the Root default Rec*2 would differ
	}

	@Override
	public Collection<TagData> getEnergyTypes(byte aSide) {
		return mEnergyTypeAccepted.AS_LIST; // :377
	}

	// the :375-376 stored/capacity reporting rides the stat/thermometer surface — the
	// capacitor interface half is the cut ADR-D1 subsystem, no port surface (class doc).

	// ---------------------------------------------------------------------------
	// the fluid capability door (:380-385 — the tank view exposes BOTH tanks on every
	// face, fill = the FL.water gate (:380 has NO face half), drain = the steam tank (:381))
	// ---------------------------------------------------------------------------

	//? if forge {
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> aCapability, @Nullable Direction aSide) {
		if (aCapability == ForgeCapabilities.FLUID_HANDLER) {
			// the FRESH per-call side wrapper (the W3 form; the side is accepted unused — the
			// :380 fillable gate has NO face half on this machine, the intake gating lives on
			// the part modes upstream and on the door's water-only half here)
			return LazyOptional.of(LargeBoilerFluidHandler::new).cast();
		}
		return super.getCapability(aCapability, aSide);
	}
	//?} else {
	/*// (1.21.1 seam: NeoForge 21.1 removed BlockEntity#getCapability/LazyOptional — W4's
	// RegisterCapabilitiesEvent.registerBlockEntity delegates to this member; no @Override.
	// The FRESH per-call side wrapper form is kept: the side is accepted unused — the :380
	// fillable gate has NO face half on this machine, the intake gating lives on the part
	// modes upstream and on the door's water-only half here.)
	public <T> T getCapability(BlockCapability<T, Direction> aCapability, @Nullable Direction aSide) {
		if (aCapability == Capabilities.FluidHandler.BLOCK) {
			return (T) new LargeBoilerFluidHandler();
		}
		return null;
	}
	 *///?}

	/**
	 * The door behind the capability: fill = the :380 water-only gate into the water tank
	 * (every face — the upstream fillable has no side arm); drain = the :381 steam tank
	 * (steam IS capability-drainable, unlike the W3 machine's :263 null); the tank view
	 * exposes both (:382). A non-water fluid is REFUSED, not destroyed.
	 */
	private class LargeBoilerFluidHandler implements IFluidHandler {

		@Override public int getTanks() {return mTanks.length;} // :382
		@Override public FluidStack getFluidInTank(int aTank) {
			FluidStack tStack = (aTank >= 0 && aTank < mTanks.length) ? mTanks[aTank].get() : null;
			return tStack == null ? FluidStack.EMPTY : tStack;
		}
		@Override public int getTankCapacity(int aTank) {return (aTank >= 0 && aTank < mTanks.length) ? mTanks[aTank].getCapacity() : 0;}
		@Override public boolean isFluidValid(int aTank, FluidStack aStack) {
			return aTank == 0 && aStack != null && !aStack.isEmpty() && mWaterMatch.apply(aStack.getFluid());
		}

		@Override
		public int fill(FluidStack aResource, FluidAction aAction) {
			if (aResource == null || aResource.isEmpty()) return 0;
			if (!mWaterMatch.apply(aResource.getFluid())) return 0; // :380 the FL.water half (no face half)
			return mTanks[0].fill(aResource, aAction);
		}

		@Override
		public FluidStack drain(FluidStack aResource, FluidAction aAction) { // :381
			if (aResource == null || aResource.isEmpty()) return FluidStack.EMPTY;
			return mTanks[1].drain(aResource.getAmount(), aAction);
		}

		@Override
		public FluidStack drain(int aMaxDrain, FluidAction aAction) { // :381
			return mTanks[1].drain(aMaxDrain, aAction);
		}
	}

	// the placement facing mirror (the FRONT carries the barometer :360, getDefaultSide
	// SIDE_FRONT :364 over SIDES_HORIZONTAL :365) rides the base
	// TileEntityBase10MultiBlockBase.setFacingFromPlacement — the controller block calls it.

	// ---------------------------------------------------------------------------
	// NBT (the upstream :74-94 set; the row config re-derives from the block carrier)
	// ---------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putLong(NBT_ENERGY, mEnergy); // :91
		if (mEfficiency != 10000) aNBT.putShort(NBT_EFFICIENCY, mEfficiency); // :92
		aNBT.putLong(NBT_OUTPUT, mOutput); // the row value rides NBT for a clean round trip (:79)
		aNBT.putByte(NBT_VISUAL, mBarometer); // the synced gauge byte (:78)
		mTanks[0].writeToNBT(aNBT, NBT_TANK0); // :93
		mTanks[1].writeToNBT(aNBT, NBT_TANK1); // :93
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_ENERGY, Tag.TAG_ANY_NUMERIC)) mEnergy = aNBT.getLong(NBT_ENERGY); // :76
		if (aNBT.contains(NBT_VISUAL, Tag.TAG_ANY_NUMERIC)) mBarometer = bind5(aNBT.getByte(NBT_VISUAL)); // :78 + :343 mask
		if (aNBT.contains(NBT_OUTPUT, Tag.TAG_ANY_NUMERIC)) setOutput(aNBT.getLong(NBT_OUTPUT)); // :79-80
		if (aNBT.contains(NBT_EFFICIENCY, Tag.TAG_ANY_NUMERIC)) {
			mEfficiency = (short)Math.max(0, Math.min(10000, aNBT.getShort(NBT_EFFICIENCY))); // :83 UT.Code.bind_
		}
		mTanks[0].readFromNBT(aNBT, NBT_TANK0); // :85
		mTanks[1].readFromNBT(aNBT, NBT_TANK1); // :85
		mTanks[1].setCapacity(steamTankCapacity()); // :80 half two
	}
}
