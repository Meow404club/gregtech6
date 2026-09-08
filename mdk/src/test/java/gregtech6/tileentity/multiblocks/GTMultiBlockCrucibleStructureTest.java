package gregtech6.tileentity.multiblocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import static gregtech6.tileentity.multiblocks.TileEntityCrucible.DEF_ENV_TEMP;
import static gregtech6.tileentity.multiblocks.TileEntityCrucible.FLAME_RANGE;
import static gregtech6.tileentity.multiblocks.TileEntityCrucible.GAS_RANGE;
import static gregtech6.tileentity.multiblocks.TileEntityCrucible.HEAT_RESISTANCE_BONUS;
import static gregtech6.tileentity.multiblocks.TileEntityCrucible.KG_PER_ENERGY;
import static gregtech6.tileentity.multiblocks.TileEntityCrucible.MAX_AMOUNT;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregapi.data.CS;
import gregtech6.multiblock.GTMultiBlockPattern;

/**
 * The LARGE CRUCIBLE offline structure suite (task p26-crucible-multiblock acceptance ②):
 * the 3x3x3 three-layer pattern forms, a missing wall fails it, the centre column is the
 * fail-not-clear air constraint, a structure loss cools the stored heat toward the
 * environment, and the upstream :78-80 constant face is pinned verbatim
 * (432U / 1.10 / 5 / 5 / 100).
 *
 * <p>All fixtures ride the {@link GTMultiBlocksOfflineTestBase} stub world: the wall block
 * is BRICKS (the frozen-registry TestCokeOven recipe — the offline tests override
 * {@code getWallBlock()}), the environment temperature is an injectable static. The
 * controller fixtures drive the check through the base template's flag: every world edit
 * arms {@code onStructureChange()} before the recheck (the flag is consumed either way).
 */
public class GTMultiBlockCrucibleStructureTest extends GTMultiBlocksOfflineTestBase {

	static BlockEntityType<TestCrucible> sCrucibleType;

	/** The concrete test BE — the crucible over a vanilla-block BET, wall and env bound. */
	public static final class TestCrucible extends TileEntityCrucible {
		public TestCrucible(BlockPos aPos, BlockState aState) {
			super(sCrucibleType, aPos, aState);
		}
		@Override
		protected Block getWallBlock() {
			return Blocks.BRICKS;
		}
		@Override
		protected long envTemperature() {
			return sEnvTemp;
		}
	}

	/** The injectable environment temperature (the WD.envTemp seam). */
	static long sEnvTemp = DEF_ENV_TEMP;

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildCrucibleFixtures() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		gregtech6.tileentity.GTOfflineTestBase.unfreezeBlockEntityTypeRegistry();
		BlockEntityType<TestCrucible>[] tHolder = (BlockEntityType<TestCrucible>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(TestCrucible::new, Blocks.BRICKS).build(null);
		sCrucibleType = tHolder[0];
	}

	// ------------------------------------------------------------------
	// the constant face (upstream :78-80 verbatim)
	// ------------------------------------------------------------------

	@Test
	public void constantsAreTheUpstreamLargeCrucibleFace() {
		assertEquals(16L*3*3*3*CS.U, MAX_AMOUNT, "MAX_AMOUNT = 432U (upstream :79)");
		assertEquals(432L * CS.U, MAX_AMOUNT, "the 432U reading of the same product");
		assertEquals(1.10, HEAT_RESISTANCE_BONUS, 0.0, "the LARGE bonus 1.10 (upstream :80 — NOT the small 1.25)");
		assertEquals(5, GAS_RANGE, "GAS_RANGE 5 (upstream :78)");
		assertEquals(5, FLAME_RANGE, "FLAME_RANGE 5 (upstream :78)");
		assertEquals(100, KG_PER_ENERGY, "KG_PER_ENERGY 100 (upstream :79)");
	}

	// ------------------------------------------------------------------
	// the pattern declaration (24 walls + 2 hollow centres)
	// ------------------------------------------------------------------

	@Test
	public void patternDeclaresThreeRingLayersAndTheHollowColumn() {
		TileEntityCrucible tCrucible = sCrucibleType.create(new BlockPos(100, 64, 100), Blocks.BRICKS.defaultBlockState());
		GTMultiBlockPattern tPattern = tCrucible.getStructurePattern();
		assertNotNull(tPattern);
		assertEquals(26, tPattern.cells().size(), "24 wall cells + 2 hollow centre cells");

		// the per-layer usage mask column (upstream :119-121)
		for (GTMultiBlockPattern.Cell tCell : tPattern.cells()) {
			if (tCell.isHollow()) {
				assertTrue(tCell.y == 1 || tCell.y == 2, "the hollow pair sits at y +1/+2 only");
				assertEquals(0, tCell.x + tCell.z, "the hollow cells are the centre column");
				continue;
			}
			assertTrue(tCell.forms(), "every wall cell carries the forming expectation");
			assertSame(Blocks.BRICKS, tCell.partBlock, "the fixture wall block");
			switch (tCell.y) {
				case 0 -> assertEquals(MultiBlockPartBlockEntity.ONLY_ENERGY_IN, tCell.usage, "the y+0 ring (:119)");
				case 1 -> assertEquals(MultiBlockPartBlockEntity.ONLY_CRUCIBLE, tCell.usage, "the y+1 ring (:120)");
				case 2 -> assertEquals(MultiBlockPartBlockEntity.ONLY_ITEM_FLUID, tCell.usage, "the y+2 ring (:121)");
				default -> fail("no wall cell outside y 0..2");
			}
			assertTrue(tCell.x != 0 || tCell.z != 0, "the wall rings exclude the centre column");
		}
		assertEquals(8, tPattern.cells().stream().filter(t -> t.y == 0 && t.forms()).count(), "8 walls at y+0");
		assertEquals(8, tPattern.cells().stream().filter(t -> t.y == 1 && t.forms()).count(), "8 walls at y+1");
		assertEquals(8, tPattern.cells().stream().filter(t -> t.y == 2 && t.forms()).count(), "8 walls at y+2");
	}

	// ------------------------------------------------------------------
	// the forming: full ring set forms, one missing wall fails
	// ------------------------------------------------------------------

	/** Places the 24-wall ring set around the controller at (100, 64, 100). */
	private static MultiBlockLevel placeFullWalls(MultiBlockLevel aLevel) {
		for (int tDZ = -1; tDZ <= 1; tDZ++) for (int tDX = -1; tDX <= 1; tDX++) {
			if (tDX == 0 && tDZ == 0) continue;
			for (int tY = 0; tY <= 2; tY++) placePart(aLevel, new BlockPos(100 + tDX, 64 + tY, 100 + tDZ));
		}
		return aLevel;
	}

	/** Removes one placed wall and arms the controller recheck. */
	private static void removeWall(MultiBlockLevel aLevel, TileEntityCrucible aCrucible, BlockPos aPos) {
		aLevel.mBlockEntities.remove(aPos);
		aLevel.mStates.put(aPos, Blocks.AIR.defaultBlockState());
		aCrucible.onStructureChange();
	}

	@Test
	public void fullWallSetForms() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TileEntityCrucible tCrucible = placeController(tLevel, sCrucibleType, new BlockPos(100, 64, 100), (byte)0);
		placeFullWalls(tLevel);
		tCrucible.onStructureChange();
		// the centre column stands open (air by default)
		assertTrue(tCrucible.checkStructure(false), "the 24-wall hollow forms");
	}

	@Test
	public void missingWallFails() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TileEntityCrucible tCrucible = placeController(tLevel, sCrucibleType, new BlockPos(100, 64, 100), (byte)0);
		placeFullWalls(tLevel);
		tCrucible.onStructureChange();
		assertTrue(tCrucible.checkStructure(false));
		// knock out one wall of the TOP ring
		removeWall(tLevel, tCrucible, new BlockPos(101, 66, 100));
		assertFalse(tCrucible.checkStructure(false), "one missing wall breaks the structure");
	}

	@Test
	public void centreColumnMustBeAirFailNotClear() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TileEntityCrucible tCrucible = placeController(tLevel, sCrucibleType, new BlockPos(100, 64, 100), (byte)0);
		placeFullWalls(tLevel);
		tCrucible.onStructureChange();
		assertTrue(tCrucible.checkStructure(false));
		// a standing block in the centre column (upstream :115-116 getAir gate)
		BlockPos tPlug = new BlockPos(100, 65, 100);
		tLevel.mStates.put(tPlug, Blocks.STONE.defaultBlockState());
		tCrucible.onStructureChange();
		assertFalse(tCrucible.checkStructure(false), "a plugged centre column fails the check");
		// fail-NOT-clear: the offending block stays standing (the checker writes nothing)
		assertSame(Blocks.STONE, tLevel.getBlockState(tPlug).getBlock(), "the plug is not cleared by the check");
		// removing it re-forms (the flag re-armed)
		tLevel.mStates.put(tPlug, Blocks.AIR.defaultBlockState());
		tCrucible.onStructureChange();
		assertTrue(tCrucible.checkStructure(false), "the unplug re-forms");
	}

	// ------------------------------------------------------------------
	// the structure-loss slow cool-down (:187-195)
	// ------------------------------------------------------------------

	@Test
	public void structureLossCoolsTowardTheEnvironment() {
		sEnvTemp = 300;
		try {
			MultiBlockLevel tLevel = new MultiBlockLevel();
			TileEntityCrucible tCrucible = placeController(tLevel, sCrucibleType, new BlockPos(100, 64, 100), (byte)0);
			placeFullWalls(tLevel);
			tCrucible.onStructureChange();
			assertTrue(tCrucible.checkStructure(false));
			tCrucible.mTemperature = 5000;

			// break the structure: one wall of the BOTTOM ring
			removeWall(tLevel, tCrucible, new BlockPos(99, 64, 100));

			// the decay: 1 K per 10 ticks toward env, BOTH directions (upstream :192 — the
			// stored heat relaxes to the environment, cooling above it, warming below it)
			for (long t = 1; t <= 100; t++) tCrucible.onTick(t, true);
			assertEquals(4990, tCrucible.mTemperature, "ten decay windows fired: 5000 - 10");

			// the warming arm: below env the relaxation climbs (205 + ten windows)
			tCrucible.mTemperature = 205;
			for (long t = 101; t <= 200; t++) tCrucible.onTick(t, true);
			assertEquals(215, tCrucible.mTemperature, "the relaxation warms toward env = 300");

			// the floor: min(200, env) — a cold-soaked chunk (150 K) is pulled up to 200 at once
			tCrucible.mTemperature = 150;
			tCrucible.onTick(201, true);
			assertEquals(200, tCrucible.mTemperature, "the floor max(mTemperature, min(200, env)) holds (:193)");
		} finally {
			sEnvTemp = DEF_ENV_TEMP;
		}
	}

	@Test
	public void formedStructureSkipsTheCoolDown() {
		sEnvTemp = 300;
		try {
			MultiBlockLevel tLevel = new MultiBlockLevel();
			TileEntityCrucible tCrucible = placeController(tLevel, sCrucibleType, new BlockPos(100, 64, 100), (byte)0);
			placeFullWalls(tLevel);
			tCrucible.onStructureChange();
			assertTrue(tCrucible.checkStructure(false));
			tCrucible.mTemperature = 5000;
			// the formed structure skips the :187-195 loss arm; the IN-TICK supply cooldown
			// (the :353-365 grace) still owns the temperature — 100 ticks of grace, no decay
			for (long t = 1; t <= 99; t++) tCrucible.onTick(t, true);
			assertEquals(5000, tCrucible.mTemperature, "the formed structure holds its heat through the grace window");
		} finally {
			sEnvTemp = DEF_ENV_TEMP;
		}
	}

	// ------------------------------------------------------------------
	// NBT round-trip (the temperature half, upstream :96/:106)
	// ------------------------------------------------------------------

	@Test
	public void temperatureRoundTrips() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TileEntityCrucible tCrucible = placeController(tLevel, sCrucibleType, new BlockPos(100, 64, 100), (byte)0);
		tCrucible.mTemperature = 1773;
		net.minecraft.nbt.CompoundTag tTag = tCrucible.saveWithoutMetadata();
		TileEntityCrucible tRestored = sCrucibleType.create(new BlockPos(100, 64, 100), Blocks.BRICKS.defaultBlockState());
		tRestored.load(tTag);
		assertEquals(1773, tRestored.mTemperature, "the stored heat survives save/load");
	}
}
