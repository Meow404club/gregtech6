package gregtech6.tileentity.multiblocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.fluid.FluidTankGT;
import gregtech6.tileentity.energy.GTEnergySourceBlockEntity;
import gregtech6.multiblock.GTMultiBlockPattern;
import gregtech6.multiblock.GTMultiBlockStructureChecker;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.Recipe;
import gregtech6.registry.GT6DynamoHousings;
import gregtech6.registry.GT6Turbines;
import net.minecraftforge.fluids.FluidStack;

/**
 * The GTMultiBlockConverter base suite (task p29-w3-turbine-dynamo): the row ladders pin
 * the upstream columns (Loader :1254-1257/:1259-1262/:1264-1267), the conversion core pins
 * the Base11 over Converter:61-94 semantics (the door floor, the waste funnel, the waste=F
 * deduction, the proxy emit), the pattern tables pin the two structure usages, and the gas
 * turbine pins the FM.Gas burn/clamp/drain legs (LargeTurbineGas.java:107-139).
 */
public class GT6MultiBlockConverterTest extends GTMultiBlocksOfflineTestBase {

	static BlockEntityType<TestSteamConverter> sSteamType;
	static BlockEntityType<TestGasConverter> sGasType;
	static BlockEntityType<TestDynamoConverter> sDynamoType;
	static BlockEntityType<ReceiverStub> sReceiverType;

	/** The steam-shaped fixture (BRICKS walls, the row injected by the test — the applyTier form). */
	static class TestSteamConverter extends GTMultiBlockConverter {
		TestSteamConverter(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
			super(aType, aPos, aState);
		}

		@Override
		public String getTileEntityName() {
			return "test_steam_converter";
		}

		@Override
		protected Block getWallBlock() {
			return Blocks.BRICKS;
		}

		@Override
		protected Block getCoilBlock() {
			return Blocks.BRICKS; // the offline registry wall — the turbine never consults it
		}

		@Override
		protected boolean fluidColumn() {
			return true;
		}

		@Override
		protected int farPlateDesign() {
			return 3;
		}
	}

	/** The dynamo-shaped fixture (the coil segment over the same BRICKS block offline — the block identity check is the checker's, the fixture pins the table). */
	static class TestDynamoConverter extends GTMultiBlockConverter {
		TestDynamoConverter(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
			super(aType, aPos, aState);
		}

		@Override
		public String getTileEntityName() {
			return "test_dynamo_converter";
		}

		@Override
		protected Block getWallBlock() {
			return Blocks.BRICKS;
		}

		@Override
		protected boolean coilSegment() {
			return true;
		}

		@Override
		protected int farPlateDesign() {
			return 2;
		}
	}

	/** The gas-shaped fixture (the real fuel legs, a fixture map row, no capability face offline). */
	static class TestGasConverter extends GTGasTurbineBlockEntity {
		TestGasConverter(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
			super(aType, aPos, aState);
		}

		@Override
		protected Block getWallBlock() {
			return Blocks.BRICKS;
		}

		@Override
		protected Block getCoilBlock() {
			return Blocks.BRICKS; // the offline registry wall
		}
	}

	/** The recording sink (the RCON receiver stand-in — records every accepted packet). */
	static class ReceiverStub extends BlockEntity implements ITileEntityEnergy {
		final List<Long> mPackets = new ArrayList<>();

		ReceiverStub(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
			super(aType, aPos, aState);
		}

		@Override
		public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
			return !aEmitting && aEnergyType == TD.Energy.RU;
		}

		@Override
		public java.util.Collection<TagData> getEnergyTypes(byte aSide) {
			return TD.Energy.RU.AS_LIST;
		}

		@Override
		public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {
			return aEnergyType == TD.Energy.RU;
		}

		@Override
		public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {
			return false;
		}

		@Override
		public long doEnergyInjection(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
			if (aEnergyType != TD.Energy.RU) return 0;
			if (aDoInject) for (long i = 0; i < aAmount; i++) mPackets.add(aSize);
			return aAmount;
		}

		@Override
		public long doEnergyExtraction(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoExtract) {
			return 0;
		}

		@Override
		public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {
			return 1;
		}

		@Override
		public long getEnergyOffered(TagData aEnergyType, byte aSide, long aSize) {
			return 0;
		}

		@Override
		public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {
			return 1;
		}

		@Override
		public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {
			return 1;
		}

		@Override
		public long getEnergySizeOutputMin(TagData aEnergyType, byte aSide) {
			return 0;
		}

		@Override
		public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {
			return 0;
		}

		@Override
		public long getEnergySizeOutputMax(TagData aEnergyType, byte aSide) {
			return 0;
		}

		@Override
		public long getEnergyDemanded(TagData aEnergyType, byte aSide, long aSize) {
			return 0;
		}
	}

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildConverterFixtures() {
		sSteamType = holder((BlockEntityType<TestSteamConverter>[] t) -> t[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new TestSteamConverter(t[0], aPos, aState), Blocks.BRICKS).build(null));
		sGasType = holder((BlockEntityType<TestGasConverter>[] t) -> t[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new TestGasConverter(t[0], aPos, aState), Blocks.BRICKS).build(null));
		sDynamoType = holder((BlockEntityType<TestDynamoConverter>[] t) -> t[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new TestDynamoConverter(t[0], aPos, aState), Blocks.BRICKS).build(null));
		sReceiverType = holder((BlockEntityType<ReceiverStub>[] t) -> t[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new ReceiverStub(t[0], aPos, aState), Blocks.BRICKS).build(null));
	}

	/** The self-referencing BET holder (the base's private selfHolder form, re-declared for the four fixture types). */
	private static <T extends BlockEntity> BlockEntityType<T> holder(java.util.function.Consumer<BlockEntityType<T>[]> aBuilder) {
		@SuppressWarnings("unchecked")
		BlockEntityType<T>[] tHolder = (BlockEntityType<T>[]) new BlockEntityType<?>[1];
		aBuilder.accept(tHolder);
		return tHolder[0];
	}

	// ---------------------------------------------------------------------------
	// the row ladders (Loader :1254-1257 / :1259-1262 / :1264-1267)
	// ---------------------------------------------------------------------------

	@Test
	public void rowTablesPinTheUpstreamColumns() {
		// steam: 17211-17214, in = raw × STEAM_PER_EU(2), waste T, wall = the dense set
		assertEquals(4, GT6Turbines.STEAM_ROWS.size());
		long[] tSteamIds = {17211, 17212, 17213, 17214};
		long[] tSteamIn = {12288, 24576, 49152, 393216}; // 6144/12288/24576/196608 × 2
		long[] tSteamOut = {4096, 8192, 16384, 131072};
		String[] tSteamWalls = {"dense_wall_stainless_steel", "dense_wall_titanium", "dense_wall_tungstensteel", "dense_wall_adamantium"};
		for (int i = 0; i < 4; i++) {
			GT6Turbines.SteamTurbineRow tRow = GT6Turbines.STEAM_ROWS.get(i);
			assertEquals(tSteamIds[i], tRow.metaId(), "steam row " + i + " id");
			assertEquals(tSteamIn[i], tRow.input(), "steam row " + i + " input");
			assertEquals(tSteamOut[i], tRow.output(), "steam row " + i + " output");
			assertEquals(tSteamWalls[i], tRow.wallPath(), "steam row " + i + " wall");
		}
		// dynamo: 17221-17224, the exact 75% ratio, RU in / EU out
		assertEquals(4, GT6DynamoHousings.DYNAMO_ROWS.size());
		long[] tDynIds = {17221, 17222, 17223, 17224};
		long[] tDynIn = {4096, 8192, 16384, 131072};
		long[] tDynOut = {3072, 6144, 12288, 98304};
		for (int i = 0; i < 4; i++) {
			GT6DynamoHousings.DynamoRow tRow = GT6DynamoHousings.DYNAMO_ROWS.get(i);
			assertEquals(tDynIds[i], tRow.metaId(), "dynamo row " + i + " id");
			assertEquals(tDynIn[i], tRow.input(), "dynamo row " + i + " input");
			assertEquals(tDynOut[i], tRow.output(), "dynamo row " + i + " output");
			assertEquals(tDynIn[i] * 3 / 4, tRow.output(), "dynamo row " + i + " is exactly 75%");
		}
		// gas: 17231-17234, HU-scale inputs (no STEAM_PER_EU), the same output ladder
		assertEquals(4, GT6Turbines.GAS_ROWS.size());
		long[] tGasIds = {17231, 17232, 17233, 17234};
		long[] tGasIn = {6144, 12288, 24576, 196608};
		long[] tGasOut = {4096, 8192, 16384, 131072};
		for (int i = 0; i < 4; i++) {
			GT6Turbines.GasTurbineRow tRow = GT6Turbines.GAS_ROWS.get(i);
			assertEquals(tGasIds[i], tRow.metaId(), "gas row " + i + " id");
			assertEquals(tGasIn[i], tRow.input(), "gas row " + i + " input");
			assertEquals(tGasOut[i], tRow.output(), "gas row " + i + " output");
			assertEquals(tSteamWalls[i], tRow.wallPath(), "gas row " + i + " wall");
		}
	}

	@Test
	public void steamDialResolvesTheSharedSteamInstance() {
		assertSame(TD.Energy.STEAM, GTEnergySourceBlockEntity.resolveEnergyType("STEAM"));
		assertSame(TD.Energy.STEAM, GTEnergySourceBlockEntity.resolveEnergyType("steam"), "case-insensitive, shared instance");
		assertSame(TD.Energy.STEAM, GTEnergySourceBlockEntity.resolveEnergyType("ENERGY.STEAM"), "the registered mName resolves too");
		assertNull(GTEnergySourceBlockEntity.resolveEnergyType("STEAM "), "no sloppy matches");
	}

	// ---------------------------------------------------------------------------
	// the structure tables
	// ---------------------------------------------------------------------------

	@Test
	public void turbinePatternCarriesTheUpstreamUsageTable() {
		GTMultiBlockPattern tPattern = GTMultiBlockConverter.structurePattern(
				(byte)2, Blocks.BRICKS, Blocks.BRICKS, true, false, 3); // facing NORTH(2), the fluid column, far design 3
		assertEquals(36, tPattern.cells().size(), "3x3x4 = 36 cells (35 walls + the controller cell)");
		// facing 2: OFF = (0,0,-1); the far centre cell = (1-3)*front = (0,0,+2), world offset
		// = cell - OFF = (0,0,+3) — three BEHIND the north-facing front
		GTMultiBlockPattern.Cell tFar = cellAt(tPattern, 0, 0, 2);
		assertTrue(tFar.forms());
		assertEquals(MultiBlockPartBlockEntity.ONLY_ENERGY_OUT, tFar.usage);
		assertEquals(3, tFar.design);
		// the cell convention (facing 2): cell = (p1, p2, d-1) — the controller layer at
		// z = -1, the far plate at z = +2, y the cross-section height
		assertEquals(MultiBlockPartBlockEntity.ONLY_FLUID, cellAt(tPattern, 0, -1, -1).usage, "the intake layer bottom (ONLY_FLUID)");
		assertEquals(MultiBlockPartBlockEntity.ONLY_FLUID_IN, cellAt(tPattern, 0, 0, -1).usage, "the intake layer centre = the controller cell (self-pass at check time)");
		assertEquals(MultiBlockPartBlockEntity.ONLY_FLUID_IN, cellAt(tPattern, 0, 1, -1).usage, "the intake layer top");
		assertEquals(MultiBlockPartBlockEntity.ONLY_FLUID_OUT, cellAt(tPattern, 0, -1, 0).usage, "depth +1 bottom = an outer bottom row (:93)");
		assertEquals(MultiBlockPartBlockEntity.ONLY_FLUID_OUT, cellAt(tPattern, 0, -1, 1).usage, "depth +2 bottom = an outer bottom row too");
		assertEquals(MultiBlockPartBlockEntity.ONLY_FLUID_IN, cellAt(tPattern, 1, 1, -1).usage, "the intake layer corner");
		// the outer walls: bottom row ONLY_FLUID_OUT, the rest NOTHING
		assertEquals(MultiBlockPartBlockEntity.ONLY_FLUID_OUT, cellAt(tPattern, 0, -1, 2).usage, "the far bottom row");
		assertEquals(MultiBlockPartBlockEntity.NOTHING, cellAt(tPattern, 1, 0, 2).usage, "the far ring");
		assertEquals(MultiBlockPartBlockEntity.ONLY_FLUID_IN, cellAt(tPattern, -1, 1, -1).usage, "the whole frontal layer intakes (the tooltip: input only possible at frontal 3x3)");
		assertEquals(MultiBlockPartBlockEntity.NOTHING, cellAt(tPattern, -1, 0, 0).usage, "the mid ring");
	}

	@Test
	public void dynamoPatternCarriesTheCoilSegment() {
		GTMultiBlockPattern tPattern = GTMultiBlockConverter.structurePattern(
				(byte)2, Blocks.BRICKS, Blocks.COBBLESTONE, false, true, 2); // the coil segment, far design 2
		assertEquals(36, tPattern.cells().size());
		// the coil layers: d = 1..2 -> cell z in {0, 1} (the two middle layers), the full
		// 3x3 cross-section each
		int tCoils = 0;
		for (GTMultiBlockPattern.Cell tCell : tPattern.cells()) {
			if (tCell.partBlock == Blocks.COBBLESTONE) {
				tCoils++;
				assertEquals(MultiBlockPartBlockEntity.NOTHING, tCell.usage, "coil cells are NOTHING");
				assertEquals(0, tCell.design);
			}
		}
		assertEquals(18, tCoils, "3x3x2 = 18 Large Copper Coils");
		// the far plate: design 2 + ONLY_ENERGY_OUT
		GTMultiBlockPattern.Cell tFar = cellAt(tPattern, 0, 0, 2);
		assertEquals(MultiBlockPartBlockEntity.ONLY_ENERGY_OUT, tFar.usage);
		assertEquals(2, tFar.design);
		// the end-plate walls: everything NOTHING (no fluid column on the dynamo)
		assertEquals(MultiBlockPartBlockEntity.NOTHING, cellAt(tPattern, -1, -1, -1).usage, "the controller-layer bottom");
		assertEquals(MultiBlockPartBlockEntity.NOTHING, cellAt(tPattern, 0, -1, 2).usage, "the far bottom row");
	}

	@Test
	public void patternResolvesTheFarPlateThreeBehindForEveryFacing() {
		for (byte tFacing = 2; tFacing <= 5; tFacing++) {
			GTMultiBlockPattern tPattern = GTMultiBlockConverter.structurePattern(
					tFacing, Blocks.BRICKS, Blocks.BRICKS, true, false, 3);
			Direction tFront = Direction.from3DDataValue(tFacing);
			GTMultiBlockPattern.Cell tFar = cellAt(tPattern, -2 * tFront.getStepX(), -2 * tFront.getStepY(), -2 * tFront.getStepZ());
			// the far centre cell sits at (1-3)*front = -2*front; its world offset = cell - OFF = -3*front
			int[] tOffset = tPattern.cellOffset(tFacing, tFar.x, tFar.y, tFar.z);
			assertEquals(-3 * tFront.getStepX(), tOffset[0], "facing " + tFacing + " X");
			assertEquals(-3 * tFront.getStepY(), tOffset[1], "facing " + tFacing + " Y");
			assertEquals(-3 * tFront.getStepZ(), tOffset[2], "facing " + tFacing + " Z");
			assertEquals(MultiBlockPartBlockEntity.ONLY_ENERGY_OUT, tFar.usage, "facing " + tFacing);
		}
	}

	private static GTMultiBlockPattern.Cell cellAt(GTMultiBlockPattern aPattern, int aX, int aY, int aZ) {
		for (GTMultiBlockPattern.Cell tCell : aPattern.cells()) {
			if (tCell.x == aX && tCell.y == aY && tCell.z == aZ) return tCell;
		}
		throw new AssertionError("no cell at (" + aX + "," + aY + "," + aZ + ")");
	}

	// ---------------------------------------------------------------------------
	// the conversion core (Base11 over Converter:61-94)
	// ---------------------------------------------------------------------------

	/** A fresh steam fixture with the row applied (in 100 / out 50 / waste T) — small numbers, the math is scale-free. */
	private static TestSteamConverter steamConverter(BlockPos aPos) {
		TestSteamConverter tConverter = new TestSteamConverter(sSteamType, aPos, Blocks.BRICKS.defaultBlockState());
		tConverter.applyRow(100, 50, TD.Energy.STEAM, TD.Energy.RU, true, false);
		return tConverter;
	}

	@Test
	public void injectionGateConsumesWholePacketsWithinTheWindow() {
		TestSteamConverter tConverter = steamConverter(C1);
		// the window [50, 200]: a whole packet of 100 enters
		assertEquals(1, tConverter.doInject(TD.Energy.STEAM, tConverter.mFacing, 100, 1, true));
		assertEquals(100, tConverter.mStorage);
		// a second packet fills to the capacity 200
		assertEquals(1, tConverter.doInject(TD.Energy.STEAM, tConverter.mFacing, 100, 1, true));
		assertEquals(200, tConverter.mStorage);
		// the full gate: 0 — the source refunds
		assertEquals(0, tConverter.doInject(TD.Energy.STEAM, tConverter.mFacing, 100, 1, true));
	}

	@Test
	public void oversizePacketConsumesAllAndStrikesTheLadder() {
		TestSteamConverter tConverter = steamConverter(C1);
		assertEquals(2, tConverter.doInject(TD.Energy.STEAM, tConverter.mFacing, 201, 2, true), "the whole offer");
		assertEquals(0, tConverter.mStorage, "the strike cleared the capacitor");
		assertEquals(1, tConverter.mExplosionPrevention, "the first soft strike");
	}

	@Test
	public void conversionDoorFloorsTheRatioAndTheWasteVents() {
		TestSteamConverter tConverter = steamConverter(C1);
		tConverter.doEnergyInjection(TD.Energy.STEAM, tConverter.mFacing, 100, 1, true); // the gated entry, storage 100
		tConverter.doConversion(3); // tOutput = 50, >= out/2 = 25 -> converted; waste vents 2*in = 200 -> 0
		assertEquals(50, tConverter.mLastConverted, "the door: floor(100 * 50 / 100)");
		assertEquals(0, tConverter.mStorage, "the waste funnel emptied the bucket");
		// a sub-minimum packet: STEAM rides ALL_SIZE_IRRELEVANT, so the Root gate
		// (EnergyGate.gateInjection) skips the input-min check and it ENTERS — but the door
		// floors it under the out/2 minimum, nothing converts, and the waste funnel vents it
		TestSteamConverter tSmall = steamConverter(C1);
		tSmall.doEnergyInjection(TD.Energy.STEAM, tSmall.mFacing, 49, 1, true); // below in/2 = 50
		assertEquals(49, tSmall.mStorage, "size-irrelevant input: the gate admits the small packet");
		tSmall.doConversion(3);
		assertEquals(24, tSmall.mLastConverted, "the door readout is the raw floor — but under the out/2 minimum");
		assertFalse(tSmall.mCanEmitEnergy, "the emit door stays shut");
		assertEquals(0, tSmall.mLastOut, "nothing delivered — no steam ever becomes RU");
		assertEquals(0, tSmall.mStorage, "and the waste funnel vents it");
	}

	@Test
	public void wasteFalseDeductsTheConvertedAmount() {
		TestSteamConverter tConverter = steamConverter(C1);
		tConverter.applyRow(100, 50, TD.Energy.RU, TD.Energy.EU, false, false); // the waste=F shape
		tConverter.doEnergyInjection(TD.Energy.RU, tConverter.mFacing, 100, 1, true); // storage 100
		tConverter.doConversion(3); // converts 50, no receiver -> used 0 -> NO deduction, NO vent
		assertEquals(100, tConverter.mStorage, "waste=F keeps the banked energy on a dead emit");
		assertEquals(50, tConverter.mLastConverted);
		assertEquals(0, tConverter.mLastOut, "no receiver -> nothing delivered");
	}

	@Test
	public void emitWalksThroughTheFarPlateProxy() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestSteamConverter tConverter = steamConverter(C1); // facing 2 (NORTH): far plate at +3 SOUTH
		tConverter.setLevel(tLevel);
		tLevel.mBlockEntities.put(C1, tConverter);
		BlockPos tPlate = new BlockPos(100, 64, 103);
		MultiBlockPartBlockEntity tPart = placePart(tLevel, tPlate);
		ReceiverStub tReceiver = new ReceiverStub(sReceiverType, new BlockPos(100, 64, 104), Blocks.BRICKS.defaultBlockState());
		tReceiver.setLevel(tLevel);
		tLevel.mBlockEntities.put(tReceiver.getBlockPos(), tReceiver);

		assertSame(tPart, tConverter.getEmittingTileEntity(), "the proxy is the far-plate part");
		assertEquals((byte) Direction.SOUTH.get3DDataValue(), tConverter.getEmittingSide(), "the emit side is the BACK");

		tConverter.doEnergyInjection(TD.Energy.STEAM, tConverter.mFacing, 100, 1, true);
		tConverter.doConversion(3);
		assertEquals(1, tReceiver.mPackets.size(), "one packet delivered");
		assertEquals(50L, tReceiver.mPackets.get(0), "the size-carrying branch: ONE packet of tOutput");
		assertTrue(tConverter.mActive);
		assertEquals(50, tConverter.mLastOut);
		// the fallback: a dead plate resolves back to the controller
		tLevel.mBlockEntities.remove(tPlate);
		assertSame(tConverter, tConverter.getEmittingTileEntity());
	}

	@Test
	public void checkerFormsTheTurbineShellAndRejectsAWrongWall() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestSteamConverter tConverter = steamConverter(C1);
		tConverter.setLevel(tLevel);
		tLevel.mBlockEntities.put(C1, tConverter);
		GTMultiBlockPattern tPattern = tConverter.getStructurePattern();
		for (GTMultiBlockPattern.Cell tCell : tPattern.cells()) {
			BlockPos tPos = C1.offset(tPattern.cellOffset((byte) 2, tCell.x, tCell.y, tCell.z)[0],
					tPattern.cellOffset((byte) 2, tCell.x, tCell.y, tCell.z)[1],
					tPattern.cellOffset((byte) 2, tCell.x, tCell.y, tCell.z)[2]);
			if (tPos.equals(C1)) continue; // the self-cell
			placePart(tLevel, tPos);
		}
		GTMultiBlockStructureChecker.FormedVerdict tVerdict = GTMultiBlockStructureChecker.check(
				tConverter, tConverter.mFacing, null, null, null);
		assertTrue(tVerdict.formed, "the full shell forms — first failure: " + tVerdict.describeFirstFailure());
		// the far plate got its design 3 + ONLY_ENERGY_OUT write
		MultiBlockPartBlockEntity tFar = (MultiBlockPartBlockEntity) tLevel.getBlockEntity(new BlockPos(100, 64, 103));
		assertEquals(3, tFar.mDesign);
		assertEquals(MultiBlockPartBlockEntity.ONLY_ENERGY_OUT, tFar.mMode);
		// the wrong wall breaks the check
		BlockPos tProbe = new BlockPos(100, 64, 102);
		tLevel.mStates.put(tProbe, Blocks.DIAMOND_BLOCK.defaultBlockState());
		GTMultiBlockStructureChecker.FormedVerdict tBroken = GTMultiBlockStructureChecker.check(
				tConverter, tConverter.mFacing, null, null, null);
		assertFalse(tBroken.formed, "a wrong wall cell rejects the form");
	}

	// ---------------------------------------------------------------------------
	// the gas turbine (LargeTurbineGas.java:107-139)
	// ---------------------------------------------------------------------------

	/** The FM.Gas fixture row: 5 water in, water 6 out, -64 x 30 (the natural-gas shape over vanilla fluids). */
	private static final long GAS_ROW_POWER = 64L * 30;

	private static void pourFixtureGasRow() {
		GT6RecipeMaps.init();
		GT6RecipeMaps.GAS_FUELS.addRecipe(new Recipe(true,
				new net.minecraft.world.item.ItemStack[0], new net.minecraft.world.item.ItemStack[0],
				new FluidStack[] {new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 5)},
				new FluidStack[] {new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 6)},
				30, -64, 0));
	}

	private static TestGasConverter gasConverter(BlockPos aPos) {
		TestGasConverter tConverter = new TestGasConverter(sGasType, aPos, Blocks.BRICKS.defaultBlockState());
		tConverter.applyRow(6144, 4096, TD.Energy.HU, TD.Energy.RU, false, true);
		tConverter.rederiveTankCapacities();
		return tConverter;
	}

	@Test
	public void gasTurbineBurnsTheGasRowIntoTheCapacitor() {
		pourFixtureGasRow();
		TestGasConverter tConverter = gasConverter(C1);
		assertTrue(tConverter.mLimitConsumption, "the gas rows carry LIMIT_CONSUMPTION");
		assertFalse(tConverter.mWasteEnergy, "the gas rows carry WASTE_ENERGY F");
		tConverter.mInputTank.add(6144, new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1));
		// the deficit ceiling: ceil(12288 / 1920) = 7; the tank holds 6144/5 = 1228 units -> parallel 7
		tConverter.doConversion(3);
		assertEquals(7 * GAS_ROW_POWER, tConverter.mStorage, "parallel x |EUt| x duration credited");
		assertEquals(6144 - 35, tConverter.mInputTank.amount(), "7 x 5 L consumed");
		assertEquals(42, tConverter.mTanksOutput[0].amount(), "7 x 6 L exhaust");
		// the ceil overshoot (7 x 1920 = 13440 > the 12288 window) banks past the capacitor:
		// the door rounds to 8960, past outMax 8192 — the LIVE LIMIT_CONSUMPTION cap (:70)
		assertEquals(8192, tConverter.mLastConverted, "the limit cap holds the packet at outMax");
		// waste=F: no vent, the emit found no receiver, the bank stays
		assertEquals(7 * GAS_ROW_POWER, tConverter.mStorage);
	}

	@Test
	public void gasClampLegConvertsOneWindowPerTickFromAStockpile() {
		pourFixtureGasRow();
		TestGasConverter tConverter = gasConverter(C1);
		tConverter.mStorage = 30000; // above 2x in = 12288
		tConverter.doConversion(3);
		assertEquals(30000 - 12288, tConverter.mStorage, "one full window burned, the rest banks");
		assertEquals(8192, tConverter.mLastConverted, "units(12288, 6144, 4096, F) — exactly outMax, no cap needed");
	}

	@Test
	public void gasIdleDrainsWithoutFuel() {
		pourFixtureGasRow();
		TestGasConverter tConverter = gasConverter(C1);
		tConverter.mStorage = 5000;
		tConverter.doConversion(3); // no tank content -> the :136 drain floors it, then converts 0
		assertEquals(0, tConverter.mStorage, "the idle drain (the exhaust-penalty semantics)");
		assertEquals(0, tConverter.mLastConverted);
	}

	@Test
	public void gasFillGateRejectsNonFuelFluids() {
		pourFixtureGasRow();
		// lava is no gas row -> the containment probe misses
		assertNull(GTGasTurbineBlockEntity.findFuelRecipeFor(
				new FluidStack(net.minecraft.world.level.material.Fluids.LAVA, 100)));
		assertNotNull(GTGasTurbineBlockEntity.findFuelRecipeFor(
				new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 100)), "water is the fixture fuel");
	}
}
