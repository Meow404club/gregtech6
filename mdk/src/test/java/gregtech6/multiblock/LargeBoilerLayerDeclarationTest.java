package gregtech6.multiblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.recipes.GTRecipesOfflineTestBase;
import gregtech6.tileentity.multiblocks.MultiBlockPartBlockEntity;
import gregtech6.tileentity.multiblocks.TileEntityLargeBoiler;

/**
 * The LARGE BOILER re-declared in the layer-sequence DSL — the task p16-pattern-layers
 * acceptance stub: the canonical 4-slab shape (the bottom 3x3 transmitter layer + the
 * middle 3x3 wall layer + the repeatable ring segment) expands, at n = 2, to a cell set
 * EQUAL to the production enumeration — the P12 binding this boiler carries
 * ({@link TileEntityLargeBoiler#getStructurePattern()}, the LargeBoilerPatternTest
 * literal pin) for the display calibre, and the checkStructure2 constants for the FULL
 * forming triple (the ONLY_* usage masks :299/:306/:311/:334, the design-1 pipe holes
 * :124/:334, the :102 keep-hollow cavity top). Also pinned: the n arms (min = 2 == the
 * canonical boiler, max = 3 stretches, out-of-window rejected), the
 * {@code ITileEntityMultiBlockController#getStructurePattern(int)} default, and the
 * production declaration itself is untouched (this file adds a TEST-ONLY family — the
 * no-production-consumer ruling, ADR 2026-09-05-p16-formation-scoping ②).
 *
 * <p>The ring-segment factory encodes the boiler's vertical grammar without knowing n:
 * ring 0's centre is the keep-hollow cavity top above the anchor (upstream :102), every
 * ring's four side-middles carry design 1 ONLY on the first ring (upstream :127/:129/
 * :131/:133), and every ring centre from ring 1 up is a design-1 pipe hole (upstream
 * :124 — at n = 2 exactly the top centre). n = 2 therefore reproduces the canonical
 * boiler cell for cell, triple for triple.
 */
public class LargeBoilerLayerDeclarationTest extends GTRecipesOfflineTestBase {

	static BlockEntityType<LayeredBoiler> sBoilerType;

	/** The concrete test BE — the boiler class over a vanilla-block BET, fixture blocks bound. */
	public static final class LayeredBoiler extends TileEntityLargeBoiler {
		public LayeredBoiler(BlockPos aPos, BlockState aState) {
			super(sBoilerType, aPos, aState);
		}
		@Override
		protected Block getWallBlock() {
			return Blocks.BRICKS;
		}
		@Override
		protected Block getTransmitterBlock() {
			return Blocks.STONE;
		}
	}

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void buildFixtures() {
		BlockEntityType<LayeredBoiler>[] tHolder = (BlockEntityType<LayeredBoiler>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(LayeredBoiler::new, Blocks.BRICKS, Blocks.STONE).build(null);
		sBoilerType = tHolder[0];
	}

	/**
	 * The layer-sequence declaration of the boiler (the acceptance stub's subject): slab 0
	 * the bottom 3x3 heat transmitters (ONLY_ENERGY_IN, :104-112), slab 1 the middle 3x3
	 * walls (ONLY_FLUID_IN, :114-122 — the controller's own cell included), then the
	 * repeatable ring segment, window [2, 3] (2 = the canonical boiler).
	 */
	static GTMultiBlockPatternFamily boilerFamily() {
		return GTMultiBlockPatternFamily.of(tBuilder -> tBuilder
				// the structure centre is the CONTROLLER layer (the middle slab) — the bottom
				// transmitter slab sits at y -1, so the 0-based slab sequence is re-anchored
				.originY(-1)
				// :104-112 — the bottom 3x3, ONLY_ENERGY_IN
				.layer(forming3x3(Blocks.STONE, MultiBlockPartBlockEntity.ONLY_ENERGY_IN))
				// :114-122 — the middle 3x3, ONLY_FLUID_IN
				.layer(forming3x3(Blocks.BRICKS, MultiBlockPartBlockEntity.ONLY_FLUID_IN))
				// :124 + :125-135 + :102 — the upper shell, repeatable
				.repeatable(2, 3, LargeBoilerLayerDeclarationTest::ringLayer));
	}

	/** The full 3x3 footprint of ONE forming cell kind, z rows outer (-1, 0, +1) — the upstream double-loop order. */
	private static GTMultiBlockPattern.Layer forming3x3(Block aBlock, int aUsage) {
		GTMultiBlockPattern.Layer.Builder tLayer = GTMultiBlockPattern.Layer.builder();
		for (int tDZ = -1; tDZ <= 1; tDZ++) for (int tDX = -1; tDX <= 1; tDX++) {
			tLayer.formingPart(tDX, tDZ, aBlock, aUsage, 0);
		}
		return tLayer.build();
	}

	/** One ring layer: the 8-cell shell plus the centre — ring 0 hollow (the :102 cavity top), ring i>0 the design-1 pipe hole (:124). */
	private static GTMultiBlockPattern.Layer ringLayer(int aRing) {
		GTMultiBlockPattern.Layer.Builder tLayer = GTMultiBlockPattern.Layer.builder();
		for (int tDZ = -1; tDZ <= 1; tDZ++) for (int tDX = -1; tDX <= 1; tDX++) {
			if (tDX == 0 && tDZ == 0) continue;
			int tDesign = (aRing == 0 && (tDX == 0 || tDZ == 0)) ? TileEntityLargeBoiler.DESIGN_PIPE_HOLE : 0;
			tLayer.formingPart(tDX, tDZ, Blocks.BRICKS, MultiBlockPartBlockEntity.ONLY_FLUID_OUT, tDesign);
		}
		if (aRing == 0) {
			tLayer.hollow(0, 0, GTMultiBlockPattern.AIR); // :102 — the keep-hollow above the anchor
		} else {
			tLayer.formingPart(0, 0, Blocks.BRICKS, MultiBlockPartBlockEntity.ONLY_FLUID_OUT, TileEntityLargeBoiler.DESIGN_PIPE_HOLE); // :124
		}
		return tLayer.build();
	}

	// ---- the display-calibre equivalence: layered expansion == the production P12 binding ----

	@Test
	public void layeredExpansionEqualsTheProductionEnumeration() {
		TileEntityLargeBoiler tBoiler = sBoilerType.create(new BlockPos(100, 64, 100), Blocks.BRICKS.defaultBlockState());
		GTMultiBlockPattern tProduction = tBoiler.getStructurePattern();
		GTMultiBlockPattern tLayered = boilerFamily().forSize(2);

		assertEquals(tProduction.cells().size(), tLayered.cells().size(), "36 cells either way (35 parts + the hollow)");
		Map<Long, GTMultiBlockPattern.Cell> tProd = byOffset(tProduction);
		Map<Long, GTMultiBlockPattern.Cell> tLayeredMap = byOffset(tLayered);
		assertEquals(tProd.keySet(), tLayeredMap.keySet(), "the SAME cell set (order-free — the layer DSL declares by slab, the production enum by check sweep)");

		BlockState tWalls = Blocks.BRICKS.defaultBlockState();
		BlockState tTx = Blocks.STONE.defaultBlockState();
		BlockState tAir = Blocks.AIR.defaultBlockState();
		for (Map.Entry<Long, GTMultiBlockPattern.Cell> tEntry : tProd.entrySet()) {
			GTMultiBlockPattern.Cell tA = tEntry.getValue();
			GTMultiBlockPattern.Cell tB = tLayeredMap.get(tEntry.getKey());
			assertEquals(tA.isHollow(), tB.isHollow(), "the hollow marker at " + tEntry.getKey());
			assertEquals(tA.matches(tTx), tB.matches(tTx), "the transmitter judgement at " + tEntry.getKey());
			assertEquals(tA.matches(tWalls), tB.matches(tWalls), "the wall judgement at " + tEntry.getKey());
			assertEquals(tA.matches(tAir), tB.matches(tAir), "the air judgement at " + tEntry.getKey());
		}
	}

	private static Map<Long, GTMultiBlockPattern.Cell> byOffset(GTMultiBlockPattern aPattern) {
		Map<Long, GTMultiBlockPattern.Cell> tMap = new HashMap<>();
		// the offset key — the same +Short.MAX_VALUE bias as the builder's pack (negative
		// coordinates must not alias the shifted fields)
		for (GTMultiBlockPattern.Cell tCell : aPattern.cells()) {
			tMap.put(key(tCell.x, tCell.y, tCell.z), tCell);
		}
		return tMap;
	}

	// ---- the forming-triple equivalence: the layered declaration vs the checkStructure2 constants ----

	@Test
	public void layeredTripleEqualsTheCheckConstants() {
		GTMultiBlockPattern tLayered = boilerFamily().forSize(2);
		Map<Long, GTMultiBlockPattern.Cell> tMap = byOffset(tLayered);
		assertEquals(36, tMap.size(), "35 forming cells + 1 hollow");

		// the bottom 3x3 (:104-112): STONE transmitters, ONLY_ENERGY_IN, design 0
		for (int tDZ = -1; tDZ <= 1; tDZ++) for (int tDX = -1; tDX <= 1; tDX++) {
			GTMultiBlockPattern.Cell tCell = tMap.get(key(tDX, -1, tDZ));
			assertTrue(tCell.forms(), "the base layer carries the forming expectation at (" + tDX + ",-1," + tDZ + ")");
			assertSame(Blocks.STONE, tCell.partBlock, "the transmitter block");
			assertEquals(MultiBlockPartBlockEntity.ONLY_ENERGY_IN, tCell.usage, "the :299 usage mask");
			assertEquals(0, tCell.design);
			assertFalse(tCell.isHollow());
		}
		// the middle 3x3 (:114-122): walls, ONLY_FLUID_IN, design 0
		for (int tDZ = -1; tDZ <= 1; tDZ++) for (int tDX = -1; tDX <= 1; tDX++) {
			GTMultiBlockPattern.Cell tCell = tMap.get(key(tDX, 0, tDZ));
			assertTrue(tCell.forms());
			assertSame(Blocks.BRICKS, tCell.partBlock, "the wall block");
			assertEquals(MultiBlockPartBlockEntity.ONLY_FLUID_IN, tCell.usage, "the :306 usage mask");
			assertEquals(0, tCell.design);
		}
		// the first ring (y +1, :125-135): ONLY_FLUID_OUT, design 1 on the four side-middles;
		// the centre is the :102 keep-hollow cavity top
		for (int tDZ = -1; tDZ <= 1; tDZ++) for (int tDX = -1; tDX <= 1; tDX++) {
			if (tDX == 0 && tDZ == 0) {
				GTMultiBlockPattern.Cell tCentre = tMap.get(key(0, 1, 0));
				assertTrue(tCentre.isHollow(), "the cavity top above the anchor stays hollow");
				assertFalse(tCentre.forms(), "hollow never forms");
				assertTrue(tCentre.matches(Blocks.AIR.defaultBlockState()), "the hollow judgement is AIR");
				continue;
			}
			GTMultiBlockPattern.Cell tCell = tMap.get(key(tDX, 1, tDZ));
			assertTrue(tCell.forms());
			assertEquals(MultiBlockPartBlockEntity.ONLY_FLUID_OUT, tCell.usage, "the :314-325 usage mask");
			assertEquals((tDX == 0 || tDZ == 0) ? TileEntityLargeBoiler.DESIGN_PIPE_HOLE : 0, tCell.design,
					"the :127/:129/:131/:133 pipe holes are exactly the first ring's side-middles");
		}
		// the top ring (y +2): ONLY_FLUID_OUT, NO holes; the centre is the :124 design-1 pipe hole
		for (int tDZ = -1; tDZ <= 1; tDZ++) for (int tDX = -1; tDX <= 1; tDX++) {
			GTMultiBlockPattern.Cell tCell = tMap.get(key(tDX, 2, tDZ));
			assertTrue(tCell.forms());
			assertSame(Blocks.BRICKS, tCell.partBlock);
			assertEquals(MultiBlockPartBlockEntity.ONLY_FLUID_OUT, tCell.usage, "the :311/:334 usage mask");
			if (tDX == 0 && tDZ == 0) {
				assertEquals(TileEntityLargeBoiler.DESIGN_PIPE_HOLE, tCell.design, "the top centre IS the first pipe hole (:124)");
			} else {
				assertEquals(0, tCell.design, "the second ring carries no holes (:125-135, i == 2)");
			}
		}
	}

	private static long key(int aX, int aY, int aZ) {
		return ((long)(aX + Short.MAX_VALUE) << 34) | ((long)(aY + Short.MAX_VALUE) << 17) | (long)(aZ + Short.MAX_VALUE);
	}

	// ---- the n arms ----

	@Test
	public void sizeArmsMinMaxAndOutOfBounds() {
		GTMultiBlockPatternFamily tFamily = boilerFamily();
		// n = min = 2 is the canonical boiler (both equivalence tests above already pin it)
		assertEquals(36, tFamily.forSize(2).cells().size());
		assertEquals(-1, tFamily.forSize(2).minY()); assertEquals(2, tFamily.forSize(2).maxY(), "the canonical shell");

		// n = max = 3 stretches: one more ring, the shell grows to y +3
		GTMultiBlockPattern tStretched = tFamily.forSize(3);
		assertEquals(45, tStretched.cells().size(), "9 + 9 + 3x9 — one more ring layer");
		assertEquals(-1, tStretched.minY()); assertEquals(3, tStretched.maxY());
		// the stretched top ring keeps the grammar: ONLY_FLUID_OUT shell, design-1 centre, no side holes
		assertTrue(tStretched.cells().stream().filter(tCell -> tCell.y == 3 && tCell.forms())
				.allMatch(tCell -> tCell.usage == MultiBlockPartBlockEntity.ONLY_FLUID_OUT), "the third ring is an ONLY_FLUID_OUT shell");
		assertEquals(TileEntityLargeBoiler.DESIGN_PIPE_HOLE,
				tStretched.cells().stream().filter(tCell -> tCell.y == 3 && tCell.x == 0 && tCell.z == 0).findFirst().get().design,
				"the third ring's centre is a pipe hole (the ring grammar, ring > 0)");
		assertEquals(0, tStretched.cells().stream().filter(tCell -> tCell.y == 3 && !(tCell.x == 0 && tCell.z == 0) && tCell.forms())
				.filter(tCell -> tCell.design != 0).count(), "the third ring's shell carries no holes");

		// out of window
		assertThrows(IllegalArgumentException.class, () -> tFamily.build(1), "below min — a one-ring boiler is not in the declared window");
		assertThrows(IllegalArgumentException.class, () -> tFamily.build(4), "above max");
		assertThrows(IllegalArgumentException.class, () -> tFamily.build(-1), "a negative count is a bug");
	}

	// ---- the controller seam ----

	@Test
	public void sizeParametrisedHookDefaultsToNull() {
		TileEntityLargeBoiler tBoiler = sBoilerType.create(new BlockPos(100, 64, 100), Blocks.BRICKS.defaultBlockState());
		assertNotNull(tBoiler.getStructurePattern(), "the fixed-size declaration is untouched");
		assertEquals(null, tBoiler.getStructurePattern(2), "the size-parametrised default stays null (no consumer machine in this card)");
	}
}
