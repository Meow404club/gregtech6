package gregtech6.multiblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.recipes.GTRecipesOfflineTestBase;

/**
 * The {@link GTMultiBlockPattern} API self-tests (task p12-ghost-pattern-api): builder
 * order preservation and immutability, duplicate/empty rejection, the bounds fold, the
 * pure facing anchor (the OFF-table mirror of TileEntityBase01Root.java:174-176 — the
 * test-local table below is the independent specification), the anchor + cell rotation
 * composition, and the predicate factories against real states.
 */
public class GTMultiBlockPatternTest extends GTRecipesOfflineTestBase {

	// the GT6 side-offset tables, test-local mirror (TileEntityBase01Root.java:174-176;
	// side order DOWN UP NORTH SOUTH WEST EAST = vanilla get3DDataValue)
	private static final int[] OFF_X = { 0, 0, 0, 0, -1, 1 };
	private static final int[] OFF_Y = { 0, 1, 0, 0, 0, 0 };
	private static final int[] OFF_Z = { 0, 0, -1, 1, 0, 0 };

	@Test
	public void builderPreservesDeclarationOrderAndFlags() {
		GTMultiBlockPattern tPattern = GTMultiBlockPattern.builder()
				.part(-1, -1, -1, aState -> true)
				.part(0, -1, 0, aState -> true)
				.hollow(0, 0, 0, aState -> true)
				.part(2, 1, 7, aState -> false)
				.build();
		List<GTMultiBlockPattern.Cell> tCells = tPattern.cells();
		assertEquals(4, tCells.size(), "declaration order preserved");
		assertEquals(-1, tCells.get(0).x); assertEquals(-1, tCells.get(0).y); assertEquals(-1, tCells.get(0).z);
		assertFalse(tCells.get(0).isHollow(), "part flag");
		assertEquals(0, tCells.get(1).x); assertEquals(-1, tCells.get(1).y); assertEquals(0, tCells.get(1).z);
		assertFalse(tCells.get(1).isHollow(), "part flag");
		assertEquals(0, tCells.get(2).x); assertEquals(0, tCells.get(2).y); assertEquals(0, tCells.get(2).z);
		assertTrue(tCells.get(2).isHollow(), "hollow flag");
		assertEquals(2, tCells.get(3).x); assertEquals(1, tCells.get(3).y); assertEquals(7, tCells.get(3).z);
		assertFalse(tCells.get(3).isHollow(), "part flag");
	}

	@Test
	public void cellsAreImmutable() {
		GTMultiBlockPattern tPattern = GTMultiBlockPattern.builder()
				.part(0, 0, 0, aState -> true)
				.build();
		assertThrows(UnsupportedOperationException.class, () -> tPattern.cells().add(null),
				"the cell list must not be mutable");
	}

	@Test
	public void duplicateOffsetsRejected() {
		GTMultiBlockPattern.Builder tBuilder = GTMultiBlockPattern.builder()
				.part(1, 2, 3, aState -> true);
		assertThrows(IllegalArgumentException.class, () -> tBuilder.part(1, 2, 3, aState -> true),
				"a repeated offset is a declaration bug, pattern or not");
		assertThrows(IllegalArgumentException.class, () -> tBuilder.hollow(1, 2, 3, aState -> true),
				"hollow or part — the offset is still taken");
	}

	@Test
	public void emptyPatternRejected() {
		assertThrows(IllegalStateException.class, () -> GTMultiBlockPattern.builder().build(),
				"an empty declaration is a bug, not a pattern");
		assertThrows(IllegalArgumentException.class, () -> GTMultiBlockPattern.builder().part(0, 0, 0, null),
				"a cell without a judgement is a bug");
	}

	@Test
	public void boundsFoldOverAllCellsIncludingHollow() {
		GTMultiBlockPattern tPattern = GTMultiBlockPattern.builder()
				.part(2, -3, 1, aState -> true)
				.hollow(0, 0, 4, aState -> true)
				.part(-5, 1, 1, aState -> true)
				.build();
		assertEquals(-5, tPattern.minX()); assertEquals(2, tPattern.maxX());
		assertEquals(-3, tPattern.minY()); assertEquals(1, tPattern.maxY());
		assertEquals(1, tPattern.minZ()); assertEquals(4, tPattern.maxZ());
	}

	@Test
	public void anchorOffsetIsTheOffTableMirror() {
		// all six side bytes — the tables are total even though HORIZONTAL_FACING only
		// reaches 2..5; the 0/1 entries pin the port convention (UP = +1)
		for (byte tFacing = 0; tFacing < 6; tFacing++) {
			int[] tAnchor = GTMultiBlockPattern.anchorOffset(tFacing);
			assertEquals(-OFF_X[tFacing], tAnchor[0], "anchor x, facing " + tFacing);
			assertEquals(-OFF_Y[tFacing], tAnchor[1], "anchor y, facing " + tFacing);
			assertEquals(-OFF_Z[tFacing], tAnchor[2], "anchor z, facing " + tFacing);
		}
	}

	@Test
	public void cellOffsetAndWorldOffsetComposeTheAnchor() {
		GTMultiBlockPattern tPattern = GTMultiBlockPattern.builder().part(3, -2, 5, aState -> true).build();
		GTMultiBlockPattern.Cell tCell = tPattern.cells().get(0);
		for (byte tFacing = 0; tFacing < 6; tFacing++) {
			int[] tStatic = GTMultiBlockPattern.cellOffset(tFacing, 3, -2, 5);
			assertEquals(3 - OFF_X[tFacing], tStatic[0], "static x, facing " + tFacing);
			assertEquals(-2 - OFF_Y[tFacing], tStatic[1], "static y, facing " + tFacing);
			assertEquals(5 - OFF_Z[tFacing], tStatic[2], "static z, facing " + tFacing);
			int[] tInstance = tPattern.worldOffset(tFacing, tCell);
			assertEquals(tStatic[0], tInstance[0], "instance == static x, facing " + tFacing);
			assertEquals(tStatic[1], tInstance[1], "instance == static y, facing " + tFacing);
			assertEquals(tStatic[2], tInstance[2], "instance == static z, facing " + tFacing);
			// anchor + centre-relative == cell offset relative to the controller
			int[] tAnchor = GTMultiBlockPattern.anchorOffset(tFacing);
			assertEquals(tAnchor[0] + 3, tStatic[0], "anchor composition x, facing " + tFacing);
			assertEquals(tAnchor[1] - 2, tStatic[1], "anchor composition y, facing " + tFacing);
			assertEquals(tAnchor[2] + 5, tStatic[2], "anchor composition z, facing " + tFacing);
		}
		assertFalse(tCell.isHollow(), "the reference cell is a part");
	}

	@Test
	public void predicateFactories() {
		BlockState tBricks = Blocks.BRICKS.defaultBlockState();
		BlockState tStone = Blocks.STONE.defaultBlockState();
		BlockState tAir = Blocks.AIR.defaultBlockState();
		assertTrue(GTMultiBlockPattern.is(Blocks.BRICKS).test(tBricks), "is() matches its block");
		assertFalse(GTMultiBlockPattern.is(Blocks.BRICKS).test(tStone), "is() rejects others");
		assertTrue(GTMultiBlockPattern.anyOf(Blocks.BRICKS, Blocks.STONE).test(tBricks), "anyOf() first member");
		assertTrue(GTMultiBlockPattern.anyOf(Blocks.BRICKS, Blocks.STONE).test(tStone), "anyOf() second member");
		assertFalse(GTMultiBlockPattern.anyOf(Blocks.BRICKS, Blocks.STONE).test(tAir), "anyOf() reject");
		assertTrue(GTMultiBlockPattern.AIR.test(tAir), "AIR matches air");
		assertFalse(GTMultiBlockPattern.AIR.test(tBricks), "AIR rejects solids");
		assertFalse(GTMultiBlockPattern.is(Blocks.BRICKS).test(tAir), "is() rejects air");
	}
}
