package gregtech6.tileentity.multiblocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gregtech6.multiblock.GTMultiBlockPattern;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.Recipe;
import gregtech6.registry.GT6HeatExchangers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

/**
 * The Large Heat Exchanger offline acceptance (task p29-w3-heat-smelter, the
 * LargeBoilerPatternTest form over the offline fixtures):
 *
 * <p>① the bound pattern is the LITERAL 17-cell expectation (the y0 8-ring of Dense
 * Tungsten Walls :92-100 + the y+1 8-ring of Heat Transmitters :102-110 + the centre
 * wall :106); ② the hand-written check writes the part modes — ONLY_ITEM_FLUID_ENERGY_IN
 * on the y0 ring, NOTHING on the y+1 ring and the centre wall — and the pre-placed shell
 * FORMS (the part BEs link to the controller, the 18101-proxy face's prerequisite);
 * ③ the row semantics: rate 16384 re-derives the 163840 L input tank, the fuel row books
 * |power| per litre into the buffer, the emission split hands min(rate/8, energy/8) to
 * EACH of the eight y+2 receivers — the 16384 HU/t emission-rate pin; ④ the overflow
 * tank's stop-refuel gate; ⑤ the buffer floor reset.
 *
 * <p>The live half (the boiler-as-receiver stack, the 18101 proxy through the relay) is
 * the heat_exchanger RCON chain's.
 */
public class GT6HeatExchangerTest extends GTMultiBlocksOfflineTestBase {

	static BlockEntityType<TestHex> sHexType;
	static BlockEntityType<TestReceiver> sReceiverType;

	/** The concrete test BE — the HEX class over a vanilla-block BET, fixture blocks bound. */
	public static final class TestHex extends GT6HeatExchangerBlockEntity {
		public TestHex(BlockPos aPos, BlockState aState) {
			super(sHexType, aPos, aState);
		}
		@Override
		protected net.minecraft.world.level.block.Block getWallBlock() {
			return Blocks.BRICKS;
		}
		@Override
		protected net.minecraft.world.level.block.Block getTransmitterBlock() {
			return Blocks.STONE;
		}
	}

	/**
	 * The scripted y+2 receiver — records the HU pushes (the {@link gregapi.tileentity.energy
	 * .ITileEntityEnergy.Util} dispatch answers {@code doEnergyInjection} directly on any
	 * ITileEntityEnergy, the bare-BE form; the live receiver is the relaying 18101 part).
	 */
	public static final class TestReceiver extends BlockEntity implements gregapi.tileentity.energy.ITileEntityEnergy {
		public long mAccepted = 0;

		public TestReceiver(BlockPos aPos, BlockState aState) {
			super(sReceiverType, aPos, aState);
		}

		@Override public boolean isEnergyType(gregapi.code.TagData aEnergyType, byte aSide, boolean aEmitting) {return !aEmitting && aEnergyType == gregapi.data.TD.Energy.HU;}
		@Override public java.util.Collection<gregapi.code.TagData> getEnergyTypes(byte aSide) {return gregapi.data.TD.Energy.HU.AS_LIST;}
		@Override public boolean isEnergyAcceptingFrom(gregapi.code.TagData aEnergyType, byte aSide, boolean aTheoretical) {return true;}
		@Override public boolean isEnergyEmittingTo(gregapi.code.TagData aEnergyType, byte aSide, boolean aTheoretical) {return false;}
		@Override public long doEnergyInjection(gregapi.code.TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
			if (aDoInject) mAccepted += aAmount * Math.abs(aSize);
			return aAmount;
		}
		@Override public long doEnergyExtraction(gregapi.code.TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoExtract) {return 0;}
		@Override public long getEnergyDemanded(gregapi.code.TagData aEnergyType, byte aSide, long aSize) {return Long.MAX_VALUE;}
		@Override public long getEnergyOffered(gregapi.code.TagData aEnergyType, byte aSide, long aSize) {return 0;}
		@Override public long getEnergySizeInputMin(gregapi.code.TagData aEnergyType, byte aSide) {return 1;}
		@Override public long getEnergySizeInputRecommended(gregapi.code.TagData aEnergyType, byte aSide) {return 1;}
		@Override public long getEnergySizeInputMax(gregapi.code.TagData aEnergyType, byte aSide) {return Long.MAX_VALUE;}
		@Override public long getEnergySizeOutputMin(gregapi.code.TagData aEnergyType, byte aSide) {return 0;}
		@Override public long getEnergySizeOutputRecommended(gregapi.code.TagData aEnergyType, byte aSide) {return 0;}
		@Override public long getEnergySizeOutputMax(gregapi.code.TagData aEnergyType, byte aSide) {return 0;}
	}

	@BeforeEach
	void freshMaps() {
		GT6RecipeMaps.init(); // the GTMachinesOfflineTestBase :73 form — a sibling class's reset() may have retired the generation
	}

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildFixtures() {
		BlockEntityType<TestHex>[] tHolder = (BlockEntityType<TestHex>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(TestHex::new, Blocks.BRICKS, Blocks.STONE).build(null);
		sHexType = tHolder[0];
		BlockEntityType<TestReceiver>[] tRxHolder = (BlockEntityType<TestReceiver>[]) new BlockEntityType<?>[1];
		tRxHolder[0] = BlockEntityType.Builder.of(TestReceiver::new, Blocks.BRICKS, Blocks.STONE).build(null);
		sReceiverType = tRxHolder[0];
	}

	// the literal upstream check order, cells relative to the CONTROLLER (the centre):
	// y0 ring (:92-100), then the y+1 ring (:102-105/:107-110), then the centre wall (:106).
	private static final int[][] LITERAL_PARTS = {
			// :92-100 — the y0 ring (z rows outer, the centre is the CONTROLLER's own cell)
			{ -1, 0, -1 }, { 0, 0, -1 }, { 1, 0, -1 },
			{ -1, 0,  0 },               { 1, 0,  0 },
			{ -1, 0,  1 }, { 0, 0,  1 }, { 1, 0,  1 },
			// :102-105/:107-110 — the y+1 ring
			{ -1, 1, -1 }, { 0, 1, -1 }, { 1, 1, -1 },
			{ -1, 1,  0 },               { 1, 1,  0 },
			{ -1, 1,  1 }, { 0, 1,  1 }, { 1, 1,  1 },
			// :106 — the y+1 centre is a WALL
			{ 0, 1, 0 }
	};

	/** The first 8 entries (the y0 ring) judge the wall block; the y+1 ring the transmitter; the centre a wall. */
	private static final boolean[] IS_WALL = {
			true, true, true, true, true, true, true, true,
			false, false, false, false, false, false, false, false,
			true
	};

	@Test
	public void boundPatternIsTheLiteralUpstreamCheck() {
		TestHex tHex = sHexType.create(C1, Blocks.BRICKS.defaultBlockState());
		GTMultiBlockPattern tPattern = tHex.getStructurePattern();
		assertNotNull(tPattern, "the heat exchanger declares a pattern");
		var tCells = tPattern.cells();
		assertEquals(17, tCells.size(), "the 8+8 rings + the centre wall");
		for (int tCell = 0; tCell < 17; tCell++) {
			GTMultiBlockPattern.Cell tPart = tCells.get(tCell);
			assertEquals(LITERAL_PARTS[tCell][0], tPart.x, "cell " + tCell + " x");
			assertEquals(LITERAL_PARTS[tCell][1], tPart.y, "cell " + tCell + " y");
			assertEquals(LITERAL_PARTS[tCell][2], tPart.z, "cell " + tCell + " z");
			BlockState tWalls = Blocks.BRICKS.defaultBlockState();
			BlockState tTx = Blocks.STONE.defaultBlockState();
			assertTrue(tPart.matches(IS_WALL[tCell] ? tWalls : tTx), "cell " + tCell + " judges " + (IS_WALL[tCell] ? "the wall" : "the transmitter"));
			assertFalse(tPart.matches(IS_WALL[tCell] ? tTx : tWalls), "cell " + tCell + " rejects the other block");
		}
		assertSame(tHex.getStructurePattern(), tHex.getStructurePattern(), "the lazy cache returns the same instance");
		// the bounds fold to the 3x3x2 shell
		assertEquals(-1, tPattern.minX()); assertEquals(1, tPattern.maxX());
		assertEquals(0, tPattern.minY()); assertEquals(1, tPattern.maxY());
		assertEquals(-1, tPattern.minZ()); assertEquals(1, tPattern.maxZ());
		// the facing-independent anchor: isInsideStructure is the controller box y..y+1
		assertTrue(tHex.isInsideStructure(99, 64, 99), "the y0 ring corner is inside");
		assertTrue(tHex.isInsideStructure(101, 65, 101), "the y+1 ring corner is inside");
		assertFalse(tHex.isInsideStructure(99, 63, 100), "below the base layer is outside");
		assertFalse(tHex.isInsideStructure(100, 66, 100), "above the transmitter layer is outside");
	}

	/**
	 * The structure-mode write pin + the form: the y0 ring ONLY_ITEM_FLUID_ENERGY_IN (the
	 * item/fluid/energy intake face), the y+1 ring + the centre wall NOTHING (pure
	 * structure — the transmitter relay is killed in BOTH directions by the mode gate, so
	 * the machine never pushes through its OWN ring: the emission targets the y+2 tiles).
	 */
	@Test
	public void structureModesAreWrittenByTheCheckAndTheShellForms() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		tLevel.mBeFactory = (aPos, aState) -> {
			if (aState.is(Blocks.BRICKS)) return sPartType.create(aPos, aState);
			if (aState.is(Blocks.STONE)) return sTransmitterType().create(aPos, aState);
			return null;
		};
		TestHex tHex = placeHex(tLevel, new BlockPos(100, 64, 100));
		// pre-place the 16 part cells; the centre of y0 is the controller itself
		for (int tDZ = -1; tDZ <= 1; tDZ++) for (int tDX = -1; tDX <= 1; tDX++) {
			if (tDX == 0 && tDZ == 0) continue;
			tLevel.setBlock(tHex.getBlockPos().offset(tDX, 0, tDZ), Blocks.BRICKS.defaultBlockState(), 3);
			tLevel.setBlock(tHex.getBlockPos().offset(tDX, 1, tDZ), Blocks.STONE.defaultBlockState(), 3);
		}
		tLevel.setBlock(tHex.getBlockPos().offset(0, 1, 0), Blocks.BRICKS.defaultBlockState(), 3);
		assertTrue(tHex.checkStructure(true), "the pre-placed shell passes the check and links every part");

		// the y0 ring: ONLY_ITEM_FLUID_ENERGY_IN (the :92-100 call)
		MultiBlockPartBlockEntity tWall = (MultiBlockPartBlockEntity) tLevel.getBlockEntity(tHex.getBlockPos().offset(0, 0, -1));
		assertEquals(MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_ENERGY_IN, tWall.mMode, "the y0 ring admits item+fluid+energy IN only");
		// the y+1 ring: NOTHING (the :102-110 call)
		MultiBlockPartBlockEntity tTx = (MultiBlockPartBlockEntity) tLevel.getBlockEntity(tHex.getBlockPos().offset(0, 1, -1));
		assertEquals(MultiBlockPartBlockEntity.NOTHING, tTx.mMode, "the transmitter ring is pure structure");
		// the y+1 centre wall: NOTHING (:106)
		MultiBlockPartBlockEntity tCentre = (MultiBlockPartBlockEntity) tLevel.getBlockEntity(tHex.getBlockPos().offset(0, 1, 0));
		assertEquals(MultiBlockPartBlockEntity.NOTHING, tCentre.mMode, "the centre wall is pure structure");
	}

	private BlockEntityType<HeatTransmitterBlockEntity> sTransmitterFixtureType;

	@SuppressWarnings("unchecked")
	private BlockEntityType<HeatTransmitterBlockEntity> sTransmitterType() {
		if (sTransmitterFixtureType == null) {
			BlockEntityType<HeatTransmitterBlockEntity>[] tTxHolder = (BlockEntityType<HeatTransmitterBlockEntity>[]) new BlockEntityType<?>[1];
			tTxHolder[0] = BlockEntityType.Builder.of((aPos, aState) -> new HeatTransmitterBlockEntity(tTxHolder[0], aPos, aState), Blocks.BRICKS, Blocks.STONE).build(null);
			sTransmitterFixtureType = tTxHolder[0];
		}
		return sTransmitterFixtureType;
	}

	/** Places a fixture HEX controller into the stub world (the placeController shape). */
	private static TestHex placeHex(MultiBlockLevel aLevel, BlockPos aPos) {
		TestHex tHex = sHexType.create(aPos, Blocks.BRICKS.defaultBlockState());
		tHex.setLevel(aLevel);
		aLevel.mStates.put(aPos, Blocks.BRICKS.defaultBlockState());
		aLevel.mBlockEntities.put(aPos, tHex);
		return tHex;
	}

	// ------------------------------------------------------------------
	// the row semantics: the rate -> tank derivation, the burn, the split
	// ------------------------------------------------------------------

	@Test
	public void theRowRateReDerivesTheFuelTankAndTheEnergyFace() {
		TestHex tHex = sHexType.create(C1, Blocks.BRICKS.defaultBlockState());
		assertEquals(16384, tHex.mRate, "the :1245 NBT_OUTPUT column (the block carrier)");
		assertEquals(163840, tHex.mTanks[0].capacity(), "the :72 mRate * 10 input-tank derivation");
		assertEquals(10000, tHex.mEfficiency, "the :55 efficiency identity");
		assertSame(GT6RecipeMaps.FUELS_HOT, tHex.recipes(), "the :58 FM.Hot fuel map");
		assertEquals(0, tHex.mTanks[1].capacity() == Long.MAX_VALUE ? 0 : 1, "the overflow tank is the unbounded form");
		tHex.mEnergy = 123456;
		assertEquals(16384, tHex.getEnergyOffered(tHex.mEnergyTypeEmitted, (byte)1, 16384), "the :253 offered = min(mRate, mEnergy)");
		assertEquals(16384, tHex.getEnergySizeOutputRecommended(tHex.mEnergyTypeEmitted, (byte)1), "the :254 recommended = mRate");
		assertEquals(16384, tHex.getEnergySizeOutputMin(tHex.mEnergyTypeEmitted, (byte)1), "the :255 min = mRate (the Root default would differ)");
		assertEquals(16384, tHex.getEnergySizeOutputMax(tHex.mEnergyTypeEmitted, (byte)1), "the :256 max = mRate");
		assertTrue(tHex.isEnergyType(tHex.mEnergyTypeEmitted, (byte)1, true), "the :251 emitting-HU face");
		assertFalse(tHex.isEnergyType(tHex.mEnergyTypeEmitted, (byte)1, false), "never accepting");
	}

	@Test
	public void theHotFuelRowBooksBufferAndOverflowAndSplitsTheEmission() {
		// fuels_hot.json: hot_water 1 -> water 1, eut -2, duration 1 (the LAVA stand-in identity)
		GT6RecipeMaps.FUELS_HOT.addRecipe(new Recipe(true,
				new net.minecraft.world.item.ItemStack[0],
				new net.minecraft.world.item.ItemStack[0],
				new FluidStack[] {new FluidStack(Fluids.LAVA, 1)},
				new FluidStack[] {new FluidStack(Fluids.WATER, 1)}, 1, -2, 0));

		TestHex tHex = sHexType.create(new BlockPos(100, 64, 100), Blocks.BRICKS.defaultBlockState());
		MultiBlockLevel tLevel = new MultiBlockLevel();
		tHex.setLevel(tLevel);
		tLevel.mStates.put(tHex.getBlockPos(), Blocks.BRICKS.defaultBlockState());

		// 1000 L of the stand-in fuel: the refuel loop runs while mEnergy < mRate * 2 —
		// 2 HU per litre books the whole 1000 (2000 < 32768), the overflow takes the water
		tHex.mTanks[0].fill(new FluidStack(Fluids.LAVA, 1000), FluidAction.EXECUTE);
		Map<BlockPos, BlockEntity> tTargets = new java.util.HashMap<>();
		TestReceiver[] tReceivers = new TestReceiver[8];
		int tIndex = 0;
		for (int tDZ = -1; tDZ <= 1; tDZ++) for (int tDX = -1; tDX <= 1; tDX++) {
			if (tDX == 0 && tDZ == 0) continue;
			tReceivers[tIndex] = sReceiverType.create(tHex.getBlockPos().offset(tDX, 2, tDZ), Blocks.BRICKS.defaultBlockState());
			tTargets.put(new BlockPos(tDX, 2, tDZ), tReceivers[tIndex]);
			tIndex++;
		}
		tHex.setEmissionTargetsOverride(tTargets);
		tHex.setOutputTargetOverride(new NoTank());

		assertTrue(tHex.mEnergy < 8, "the buffer starts empty (the < 8 reset)");
		tHex.onTick(1, true);
		assertEquals(2000, tHex.mEnergy, "the buffer holds 1000 L x 2 HU (partially used fuel stays buffered, :148)");
		assertEquals(1000, tHex.mTanks[1].amount(), "the overflow tank took the row's water output");
		assertEquals(0, tHex.mTanks[0].amount(), "the input tank drained dry — the :176-181 loop kept up the power per tick");
		assertTrue(tHex.mActive, "the refuel half set the running flag");

		// the emission rate pin: fill the buffer to 2x rate — the per-tick split hands
		// min(rate/8, energy/8) = 2048 to EACH of the 8 receivers = 16384 HU/t total
		tHex.mEnergy = tHex.mRate * 2;
		tHex.onTick(2, true);
		long tTotal = 0;
		for (TestReceiver tReceiver : tReceivers) tTotal += tReceiver.mAccepted;
		assertEquals(16384, tTotal, "the :148-160 emission split — the 16384 HU/t rate over the 8 y+2 receivers");
	}

	@Test
	public void theOverflowStopGateAndTheBufferFloorReset() {
		GT6RecipeMaps.FUELS_HOT.addRecipe(new Recipe(true,
				new net.minecraft.world.item.ItemStack[0],
				new net.minecraft.world.item.ItemStack[0],
				new FluidStack[] {new FluidStack(Fluids.LAVA, 1)},
				new FluidStack[] {new FluidStack(Fluids.WATER, 1)}, 1, -2, 0));
		TestHex tHex = sHexType.create(new BlockPos(100, 64, 100), Blocks.BRICKS.defaultBlockState());
		MultiBlockLevel tLevel = new MultiBlockLevel();
		tHex.setLevel(tLevel);
		tLevel.mStates.put(tHex.getBlockPos(), Blocks.BRICKS.defaultBlockState());
		tHex.setOutputTargetOverride(new NoTank());
		tHex.setEmissionTargetsOverride(Map.of());

		// the :166 stop rule: an overflow holding >= rate * 20 refuses new fuel
		tHex.mTanks[1].fill(new FluidStack(Fluids.WATER, (int)tHex.mRate * 20), FluidAction.EXECUTE);
		tHex.mTanks[0].fill(new FluidStack(Fluids.LAVA, 500), FluidAction.EXECUTE);
		tHex.onTick(1, true);
		assertEquals(0, tHex.mEnergy, "the stop-filled overflow gates the refuel half — no HU booked");

		// the :194 floor reset: a sub-8 buffer collapses to zero
		tHex.mEnergy = 7;
		tHex.onTick(2, true);
		assertEquals(0, tHex.mEnergy, "mEnergy < 8 resets the buffer (the out-of-fuel verdict)");
	}

	/** The scripted no-adjacent-tank target (the :196 arm drains into nothing). */
	private static final class NoTank implements net.minecraftforge.fluids.capability.IFluidHandler {
		@Override public int getTanks() {return 0;}
		@Override public net.minecraftforge.fluids.FluidStack getFluidInTank(int aTank) {return net.minecraftforge.fluids.FluidStack.EMPTY;}
		@Override public int getTankCapacity(int aTank) {return 0;}
		@Override public boolean isFluidValid(int aTank, net.minecraftforge.fluids.FluidStack aStack) {return false;}
		@Override public int fill(net.minecraftforge.fluids.FluidStack aResource, FluidAction aAction) {return 0;}
		@Override public net.minecraftforge.fluids.FluidStack drain(net.minecraftforge.fluids.FluidStack aResource, FluidAction aAction) {return net.minecraftforge.fluids.FluidStack.EMPTY;}
		@Override public net.minecraftforge.fluids.FluidStack drain(int aMaxDrain, FluidAction aAction) {return net.minecraftforge.fluids.FluidStack.EMPTY;}
	}
}
