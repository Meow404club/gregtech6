package gregtech6.tileentity.multiblocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregapi.data.TD;
import gregtech6.multiblock.GTMultiBlockPattern;

/**
 * The Von da Graagg offline acceptance (task p31-graagg): the pattern declaration pins
 * (the 64 forming cells — the cornerless 5x5x2 base of Dense Galvanized Steel Walls with
 * the controller self-cell, the 5-coil pole, the 17 Dense Steel Walls of the top box —
 * in the upstream :66-93 loop order), the controller-anchored walk (the crucible
 * patternWalkFacing == 0), the :114-117 isInsideStructure box, the :167-174 EU input
 * face, the :149-150 range/drain formula (the UT.java:1560 bind8 ceiling = 255 — "256"
 * is display-only), and the suppression arms (in-range fresh spawn suppressed,
 * chunk-reload passes, mossy cobblestone exemption both vanilla and the GT MCOBL
 * variant).
 */
public class TileEntityVonDaGraaggTest extends GTMultiBlocksOfflineTestBase {

	/** The controller cell for this suite — away from the shared C1/C2 arbitration fixtures. */
	static final BlockPos GRAAGG_POS = new BlockPos(240, 64, 240);

	static BlockEntityType<TestGraagg> sGraaggType;

	/**
	 * The concrete test BE — the Graagg controller over a vanilla-block BET, the three
	 * part hooks bound to distinct vanilla blocks (the implosion TestImplosion form).
	 */
	public static final class TestGraagg extends TileEntityVonDaGraagg {
		public TestGraagg(BlockPos aPos, BlockState aState) {
			super(sGraaggType, aPos, aState);
		}
		public TestGraagg(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
			super(aType, aPos, aState);
		}
		@Override
		protected Block getBaseWallBlock() {
			return Blocks.BRICKS;
		}
		@Override
		protected Block getCoilBlock() {
			return Blocks.IRON_BLOCK;
		}
		@Override
		protected Block getTopWallBlock() {
			return Blocks.GOLD_BLOCK;
		}
	}

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildGraaggFixtures() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		gregtech6.tileentity.GTOfflineTestBase.unfreezeBlockEntityTypeRegistry();
		BlockEntityType<TestGraagg>[] tHolder = (BlockEntityType<TestGraagg>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(TestGraagg::new, Blocks.BRICKS, Blocks.IRON_BLOCK, Blocks.GOLD_BLOCK).build(null);
		sGraaggType = tHolder[0];
	}

	// ------------------------------------------------------------------
	// the pattern declaration — the :66-93 geometry
	// ------------------------------------------------------------------

	@Test
	public void patternIsTheLiteralUpstreamLoop() {
		TestGraagg tTower = sGraaggType.create(GRAAGG_POS, Blocks.BRICKS.defaultBlockState());
		GTMultiBlockPattern tPattern = tTower.getStructurePattern();
		assertNotNull(tPattern, "the graagg declares a pattern");

		var tCells = tPattern.cells();
		assertEquals(64, tCells.size(), "42 base walls (incl. the controller self-cell) + 5 coils + 17 top walls");

		// the upstream :71-89 loop order, replayed — every offset with its exact (block, usage, design)
		int tIndex = 0;
		for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) if (Math.abs(i * j) < 4) {
			for (int dy = 0; dy <= 1; dy++) {
				GTMultiBlockPattern.Cell tCell = tCells.get(tIndex++);
				assertEquals(i, tCell.x, "base x");
				assertEquals(dy, tCell.y, "base y — the dy 0/1 interleave");
				assertEquals(j, tCell.z, "base z");
				assertSame(Blocks.BRICKS, tCell.partBlock, "the base wall (production: dense_wall_galvanized_steel 18028)");
				assertEquals(MultiBlockPartBlockEntity.ONLY_ENERGY_IN, tCell.usage, "the :72-73 ONLY_ENERGY_IN mask");
				assertEquals(0, tCell.design, "design 0");
			}
		}
		for (int h = 2; h <= 6; h++) {
			GTMultiBlockPattern.Cell tCell = tCells.get(tIndex++);
			assertEquals(0, tCell.x); assertEquals(h, tCell.y); assertEquals(0, tCell.z);
			assertSame(Blocks.IRON_BLOCK, tCell.partBlock, "the coil pole (production: large_copper_coil 18040)");
			assertEquals(MultiBlockPartBlockEntity.NOTHING, tCell.usage, "the :75-79 NOTHING mask");
		}
		{
			GTMultiBlockPattern.Cell tCell = tCells.get(tIndex++);
			assertEquals(0, tCell.x); assertEquals(7, tCell.y); assertEquals(0, tCell.z);
			assertSame(Blocks.GOLD_BLOCK, tCell.partBlock, "the box top centre (production: dense_wall_steel 18029)");
		}
		for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) if (i != 0 || j != 0) {
			GTMultiBlockPattern.Cell tRing = tCells.get(tIndex++);
			assertEquals(i, tRing.x); assertEquals(6, tRing.y); assertEquals(j, tRing.z);
			assertSame(Blocks.GOLD_BLOCK, tRing.partBlock);
			assertEquals(MultiBlockPartBlockEntity.NOTHING, tRing.usage, "the :83 NOTHING mask");
			if (i * j == 0) {
				for (int dy : new int[] {5, 7}) {
					GTMultiBlockPattern.Cell tCross = tCells.get(tIndex++);
					assertEquals(i, tCross.x); assertEquals(dy, tCross.y); assertEquals(j, tCross.z);
					assertSame(Blocks.GOLD_BLOCK, tCross.partBlock);
					assertEquals(MultiBlockPartBlockEntity.NOTHING, tCross.usage, "the :85-86 NOTHING mask");
				}
			}
		}
		assertEquals(tIndex, tCells.size(), "every cell accounted for");
	}

	@Test
	public void boundsFoldToTheControllerAnchoredTower() {
		TestGraagg tTower = sGraaggType.create(GRAAGG_POS, Blocks.BRICKS.defaultBlockState());
		GTMultiBlockPattern tPattern = tTower.getStructurePattern();
		assertEquals(-2, tPattern.minX()); assertEquals(2, tPattern.maxX());
		assertEquals(0, tPattern.minY());  assertEquals(7, tPattern.maxY(), "the tower spans controller Y..Y+7");
		assertEquals(-2, tPattern.minZ()); assertEquals(2, tPattern.maxZ());
	}

	@Test
	public void walkIsControllerAnchoredForEveryLiveFacing() {
		// the upstream walk has NO facing displacement (straight xCoord/yCoord/zCoord) —
		// the crucible zero-offset anchor: patternWalkFacing() == 0, so the shared checker
		// walks EVERY cell as controller + cell, facing-independent (the facing byte only
		// drives the BlockState visual). cellOffset(0, cell) == cell is the anchor identity.
		TestGraagg tTower = sGraaggType.create(GRAAGG_POS, Blocks.BRICKS.defaultBlockState());
		assertEquals(0, tTower.patternWalkFacing(), "the crucible zero-offset anchor");
		for (int[] tCell : new int[][] {{-2, 0, -2}, {0, 0, 0}, {2, 1, 2}, {0, 2, 0}, {1, 6, 1}, {0, 7, 0}, {-1, 5, 0}}) {
			int[] tWorld = GTMultiBlockPattern.cellOffset((byte)0, tCell[0], tCell[1], tCell[2]);
			assertEquals(tCell[0], tWorld[0], "cell x identity at the zero facing");
			assertEquals(tCell[1], tWorld[1], "cell y identity at the zero facing");
			assertEquals(tCell[2], tWorld[2], "cell z identity at the zero facing");
		}
	}

	// ------------------------------------------------------------------
	// the :114-117 box and the registration name
	// ------------------------------------------------------------------

	@Test
	public void isInsideStructureIsTheUpstreamBox() {
		TestGraagg tTower = sGraaggType.create(GRAAGG_POS, Blocks.BRICKS.defaultBlockState());
		int tX = GRAAGG_POS.getX(), tY = GRAAGG_POS.getY(), tZ = GRAAGG_POS.getZ();
		assertTrue(tTower.isInsideStructure(tX, tY, tZ), "the controller cell is inside its own box");
		assertTrue(tTower.isInsideStructure(tX + 2, tY + 6, tZ + 2), "the far base corner is inside");
		assertTrue(tTower.isInsideStructure(tX, tY + 8, tZ), "the box top is inside");
		assertFalse(tTower.isInsideStructure(tX + 3, tY, tZ), "three across is outside");
		assertFalse(tTower.isInsideStructure(tX, tY + 9, tZ), "nine up is outside");
		assertFalse(tTower.isInsideStructure(tX, tY - 1, tZ), "below the controller is outside");
	}

	@Test
	public void tileEntityNameMirrorsTheBetPath() {
		TestGraagg tTower = sGraaggType.create(GRAAGG_POS, Blocks.BRICKS.defaultBlockState());
		assertEquals("multiblock_von_da_graagg", tTower.getTileEntityName());
	}

	// ------------------------------------------------------------------
	// the :167-174 energy face
	// ------------------------------------------------------------------

	@Test
	public void energyFaceIsTheEuInputCapacitorDemand() {
		TestGraagg tTower = sGraaggType.create(GRAAGG_POS, Blocks.BRICKS.defaultBlockState());
		assertTrue(tTower.isEnergyType(TD.Energy.EU, (byte)2, false), "accepts EU (the :167 !emitting arm)");
		assertFalse(tTower.isEnergyType(TD.Energy.EU, (byte)2, true), "never emits (the :167 emitting arm)");
		assertFalse(tTower.isEnergyType(TD.Energy.HU, (byte)2, false), "EU only (the :1280 NBT_ENERGY_ACCEPTED column)");
		assertSame(TD.Energy.EU, tTower.mEnergyTypeAccepted);

		// the :170 doInject — accumulates the absolute packet train, reports everything used
		assertEquals(2, tTower.doInject(TD.Energy.EU, (byte)2, 2048, 2, true), "the whole packet train reported used");
		assertEquals(4096, tTower.mEnergy, "mEnergy += |aAmount * aSize|");
		// the :170 form returns aAmount REGARDLESS of aDoInject (the upstream quirk) — only
		// the accumulation is gated; the simulation pass flows through EnergyGate unchanged
		assertEquals(2, tTower.doInject(TD.Energy.EU, (byte)2, 2048, 2, false), "the simulation return mirrors :170 verbatim");
		assertEquals(4096, tTower.mEnergy, "the simulation pass leaves the store untouched");
		assertEquals(0, tTower.getEnergyDemanded(TD.Energy.EU, (byte)2, 2048), "the :171 demand = 4096 - mEnergy");

		assertEquals(2048, tTower.getEnergySizeInputRecommended(TD.Energy.EU, (byte)2), ":172");
		assertEquals(256, tTower.getEnergySizeInputMin(TD.Energy.EU, (byte)2), ":173 — the LITERAL min, not Rec/2");
		assertEquals(4096, tTower.getEnergySizeInputMax(TD.Energy.EU, (byte)2), ":174");
	}

	// ------------------------------------------------------------------
	// the :149-150 range/drain formula (the bind8 ceiling = 255, UT.java:1560)
	// ------------------------------------------------------------------

	@Test
	public void rangeFormulaIsMinEnergyOverSixteenByteBound() {
		assertEquals(255, TileEntityVonDaGraagg.updateRange(4096, true), "min(4096,4096)/16 = 256 -> bind8 caps at 255 (the '256m' face is display-only)");
		assertEquals(255, TileEntityVonDaGraagg.updateRange(Long.MAX_VALUE / 2, true), "over-fed stores still cap at 255");
		assertEquals(128, TileEntityVonDaGraagg.updateRange(2048, true), "2048/16 = 128");
		assertEquals(16, TileEntityVonDaGraagg.updateRange(256, true), "256/16 = 16");
		assertEquals(0, TileEntityVonDaGraagg.updateRange(0, true), "empty store -> no range");
		assertEquals(0, TileEntityVonDaGraagg.updateRange(4096, false), "the :149 unformed arm zeroes the range");
		assertEquals(0, TileEntityVonDaGraagg.updateRange(-5, true), "a negative store never goes negative");
	}

	@Test
	public void drainIsFourZeroNineSixPerTickFlooredAtZero() {
		assertEquals(0, TileEntityVonDaGraagg.drain(4096), "the full drain");
		assertEquals(904, TileEntityVonDaGraagg.drain(5000), "the remainder survives");
		assertEquals(0, TileEntityVonDaGraagg.drain(0), "floored at zero (the :150 guard)");
	}

	@Test
	public void serverTickDerivesRangeThenDrains() {
		TestGraagg tTower = sGraaggType.create(GRAAGG_POS, Blocks.BRICKS.defaultBlockState());
		tTower.mStructureOkay = true;
		tTower.mEnergy = 2 * 4096;
		tTower.onTick(1, true);
		assertEquals(255, tTower.mCurrentRange, "the first tick reads the range off the store");
		assertEquals(4096, tTower.mEnergy, "then drains 4096");
		tTower.onTick(2, true);
		assertEquals(255, tTower.mCurrentRange);
		assertEquals(0, tTower.mEnergy);
		tTower.onTick(3, true);
		assertEquals(0, tTower.mCurrentRange, "the drained store loses the range on the next tick");
	}

	@Test
	public void serverTickBleedsAnUnformedTowerDry() {
		// the :149 ternary + the :150 unconditional drain — an unformed tower still drains
		TestGraagg tTower = sGraaggType.create(GRAAGG_POS, Blocks.BRICKS.defaultBlockState());
		tTower.mStructureOkay = false;
		tTower.mEnergy = 4096;
		tTower.onTick(1, true);
		assertEquals(0, tTower.mCurrentRange, "unformed -> range 0");
		assertEquals(0, tTower.mEnergy, "the drain runs regardless");
	}

	// ------------------------------------------------------------------
	// the suppression arms (:130-138 + the EntityJoinLevelEvent adapter face)
	// ------------------------------------------------------------------

	private TestGraagg poweredTower(GTMultiBlocksOfflineTestBase.MultiBlockLevel aLevel) {
		TestGraagg tTower = sGraaggType.create(GRAAGG_POS, Blocks.BRICKS.defaultBlockState());
		tTower.setLevel(aLevel);
		tTower.mStructureOkay = true;
		tTower.mCurrentRange = 8;
		return tTower;
	}

	@Test
	public void suppressionArms() {
		GTMultiBlocksOfflineTestBase.MultiBlockLevel tLevel = new GTMultiBlocksOfflineTestBase.MultiBlockLevel();
		TestGraagg tTower = poweredTower(tLevel);
		TileEntityVonDaGraagg.ALL_GRAAGGS.add(tTower);
		try {
			BlockPos tCenter = GRAAGG_POS.above();
			BlockPos tFar = GRAAGG_POS.offset(64, 0, 0);

			assertTrue(tTower.inhibitsSpawn(tLevel, tCenter), "in range, mossy-free ground -> suppressed");
			assertTrue(TileEntityVonDaGraagg.shouldSuppress(tLevel, tCenter, false), "the fresh-spawn arm suppresses");

			assertFalse(TileEntityVonDaGraagg.shouldSuppress(tLevel, tCenter, true),
					"the chunk-reload arm PASSES — loadedFromDisk mobs are never suppressed (no reload apocalypse)");

			assertFalse(tTower.inhibitsSpawn(tLevel, tFar), "outside the Chebyshev radius -> pass");

			assertFalse(tTower.inhibitsSpawn(new GTMultiBlocksOfflineTestBase.MultiBlockLevel(), tCenter),
					"a different level -> pass (the :131 dimension gate)");

			tTower.mCurrentRange = 0;
			assertFalse(tTower.inhibitsSpawn(tLevel, tCenter), "range 0 -> pass (the :131 gate)");
			tTower.mCurrentRange = 8;

			// the exemption column — the ±5 vertical scan at the spawn column (:132-137)
			tLevel.setBlock(tCenter.below(), Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 3);
			assertFalse(tTower.inhibitsSpawn(tLevel, tCenter), "vanilla mossy cobblestone one below exempts the spot");
			assertFalse(TileEntityVonDaGraagg.shouldSuppress(tLevel, tCenter, false), "the exemption wins over the range gate");

			tLevel.setBlock(tCenter.below(), Blocks.STONE.defaultBlockState(), 3);
			tLevel.setBlock(tCenter.above(5), Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 3);
			assertFalse(tTower.inhibitsSpawn(tLevel, tCenter), "the scan reaches i=+5 (the :133 edge)");

			tLevel.setBlock(tCenter.above(5), Blocks.STONE.defaultBlockState(), 3);
			tLevel.setBlock(tCenter.below(6), Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 3);
			assertTrue(tTower.inhibitsSpawn(tLevel, tCenter), "i=-6 is OUTSIDE the scan window — suppression holds");

			tLevel.setBlock(tCenter.below(6), Blocks.STONE.defaultBlockState(), 3);
			assertTrue(tTower.inhibitsSpawn(tLevel, tCenter), "stone ground -> suppression holds");
			// the GT stone MCOBL variant arm (the BlockStones.MCOBL meta-2 port) is the
			// RCON chain's live face — a GT block cannot be constructed offline (the
			// frozen-registry lesson, GTStoneBlocksRegistrationTest header)
		} finally {
			TileEntityVonDaGraagg.ALL_GRAAGGS.remove(tTower);
		}
		assertTrue(TileEntityVonDaGraagg.ALL_GRAAGGS.isEmpty(), "the table is clean after the suite (the other suites see no ghosts)");
	}

	@Test
	public void emptyTableFastExits() {
		assertTrue(TileEntityVonDaGraagg.ALL_GRAAGGS.isEmpty(), "precondition: no towers registered");
		assertFalse(TileEntityVonDaGraagg.shouldSuppress(new GTMultiBlocksOfflineTestBase.MultiBlockLevel(), GRAAGG_POS, false),
				"a tower-free world never suppresses (the listener fast exit)");
	}

	@Test
	public void unregisteredTowerNeverSuppresses() {
		GTMultiBlocksOfflineTestBase.MultiBlockLevel tLevel = new GTMultiBlocksOfflineTestBase.MultiBlockLevel();
		TestGraagg tTower = poweredTower(tLevel); // NOT added to ALL_GRAAGGS
		assertFalse(TileEntityVonDaGraagg.shouldSuppress(tLevel, GRAAGG_POS.above(), false),
				"the table, not the world, is the authority — an unlisted tower suppresses nothing");
		assertSame(tLevel, tTower.getLevel(), "fixture sanity");
	}
}
