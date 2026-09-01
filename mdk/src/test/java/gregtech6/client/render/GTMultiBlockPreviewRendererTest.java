package gregtech6.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * The multiblock ghost preview pattern (task p10-ghost-preview-poc acceptance —
 * offline, MC-free): ① the pattern table is the literal transcription of the
 * upstream Coke Oven loop (TileEntityCokeOven.java:97-111, {@code i} outer /
 * {@code j} middle / {@code k} inner, minus the air centre), ② the four-facing
 * full-table rotation — every one of the 26 cells plus the air centre maps through
 * {@link GTMultiBlockPreviewRenderer#cellOffset} to the upstream
 * {@code controller - OFF[facing] + (i, j, k)} arithmetic against a test-local mirror
 * of the OFF tables (TileEntityBase01Root.java:174-176 — the specification,
 * independent of the renderer code), with the centre offsets hardcoded per facing,
 * ③ the shell geometry per facing: the 27 cells always span the 3x3x3 cube one cell
 * BEHIND the controller and level with it.
 *
 * <p>The renderer class is {@code @OnlyIn(Dist.CLIENT)} and carries the draw code;
 * only these pure statics are touched here, so the test never loads a rendering
 * surface (the pattern must never be derived from {@code checkStructure2} — its
 * centre cell writes the world, TileEntityCokeOven.java:100-101).
 */
public class GTMultiBlockPreviewRendererTest {

	// the GT6 side-offset tables, test-local mirror (TileEntityBase01Root.java:174-176;
	// side order DOWN UP NORTH SOUTH WEST EAST = vanilla get3DDataValue)
	private static final int[] OFF_X = { 0, 0, 0, 0, -1, 1 };
	private static final int[] OFF_Y = { 0, 1, 0, 0, 0, 0 };
	private static final int[] OFF_Z = { 0, 0, -1, 1, 0, 0 };

	@Test
	public void patternTableIsTheUpstreamLoopMinusCentre() {
		// the literal upstream loop order (i outer, j middle, k inner), (0,0,0) excluded —
		// this hardcoded array IS the acceptance copy of TileEntityCokeOven.java:97-111
		int[][] tExpected = new int[][] {
				{ -1, -1, -1 }, { -1, -1, 0 }, { -1, -1, 1 },
				{ -1, 0, -1 }, { -1, 0, 0 }, { -1, 0, 1 },
				{ -1, 1, -1 }, { -1, 1, 0 }, { -1, 1, 1 },
				{ 0, -1, -1 }, { 0, -1, 0 }, { 0, -1, 1 },
				{ 0, 0, -1 }, { 0, 0, 1 },
				{ 0, 1, -1 }, { 0, 1, 0 }, { 0, 1, 1 },
				{ 1, -1, -1 }, { 1, -1, 0 }, { 1, -1, 1 },
				{ 1, 0, -1 }, { 1, 0, 0 }, { 1, 0, 1 },
				{ 1, 1, -1 }, { 1, 1, 0 }, { 1, 1, 1 }
		};
		int tAssertions = 0;
		assertEquals(26, GTMultiBlockPreviewRenderer.PATTERN_CELLS.length, "26 brick cells");
		tAssertions++;
		assertEquals(26, tExpected.length, "the expectation itself is 26 cells");
		tAssertions++;
		Set<Long> tSeen = new HashSet<>();
		for (int tCell = 0; tCell < 26; tCell++) {
			for (int tAxis = 0; tAxis < 3; tAxis++) {
				int tCoordinate = GTMultiBlockPreviewRenderer.PATTERN_CELLS[tCell][tAxis];
				assertEquals(tExpected[tCell][tAxis], tCoordinate,
						"cell " + tCell + " axis " + tAxis + " — literal transcription, upstream loop order");
				tAssertions++;
				assertTrue(tCoordinate >= -1 && tCoordinate <= 1, "canonical coordinate in -1..1");
				tAssertions++;
			}
			tSeen.add(pack(GTMultiBlockPreviewRenderer.PATTERN_CELLS[tCell]));
		}
		assertEquals(26, tSeen.size(), "all 26 cells distinct");
		tAssertions++;
		assertTrue(!tSeen.contains(0L), "the air centre (0,0,0) is not a brick cell");
		tAssertions++;
	}

	@Test
	public void fourFacingsFullTable() {
		// facings: NORTH=2, SOUTH=3, WEST=4, EAST=5 (HORIZONTAL_FACING, get3DDataValue);
		// the centre offsets hardcoded: one cell BEHIND the facing (getOffsetXN/YN/ZN =
		// controller - OFF, TileEntityBase01Root.java:194-206)
		byte[] tFacings = { 2, 3, 4, 5 };
		int[][] tHardcodedCentres = {
				{ 0, 0, 1 }, // NORTH — front looks -Z, the shell sits at +Z
				{ 0, 0, -1 }, // SOUTH — shell at -Z
				{ 1, 0, 0 }, // WEST — shell at +X
				{ -1, 0, 0 } // EAST — shell at -X
		};
		int tAssertions = 0;
		for (int tFacingIndex = 0; tFacingIndex < 4; tFacingIndex++) {
			byte tFacing = tFacings[tFacingIndex];
			// the air centre maps to the hardcoded structure centre
			assertArray("centre", tHardcodedCentres[tFacingIndex],
					GTMultiBlockPreviewRenderer.cellOffset(tFacing, 0, 0, 0));
			tAssertions++;
			// all 26 brick cells: controller - OFF[facing] + (i, j, k) — the upstream
			// checkStructure2 arithmetic (TileEntityCokeOven.java:92/:97-111)
			for (int tCell = 0; tCell < 26; tCell++) {
				int[] tPattern = GTMultiBlockPreviewRenderer.PATTERN_CELLS[tCell];
				int[] tExpected = {
						tPattern[0] - OFF_X[tFacing],
						tPattern[1] - OFF_Y[tFacing],
						tPattern[2] - OFF_Z[tFacing]
				};
				assertArray("facing " + tFacing + " cell " + tCell, tExpected,
						GTMultiBlockPreviewRenderer.cellOffset(tFacing, tPattern[0], tPattern[1], tPattern[2]));
				tAssertions++;
			}
		}
		assertEquals(108, tAssertions, "26 cells + centre, four facings");
	}

	@Test
	public void shellGeometryPerFacing() {
		// the 27 cells (26 bricks + air centre) always span the 3x3x3 cube one cell behind
		// the controller, level with it — hardcoded per-facing bounds (min, max) per axis
		byte[] tFacings = { 2, 3, 4, 5 };
		int[][] tBounds = {
				{ -1, 1, -1, 1, 0, 2 }, // NORTH — x, y full span, z from the controller cell to +2
				{ -1, 1, -1, 1, -2, 0 }, // SOUTH
				{ 0, 2, -1, 1, -1, 1 }, // WEST
				{ -2, 0, -1, 1, -1, 1 } // EAST
		};
		for (int tFacingIndex = 0; tFacingIndex < 4; tFacingIndex++) {
			byte tFacing = tFacings[tFacingIndex];
			int tMinX = Integer.MAX_VALUE, tMaxX = Integer.MIN_VALUE;
			int tMinY = Integer.MAX_VALUE, tMaxY = Integer.MIN_VALUE;
			int tMinZ = Integer.MAX_VALUE, tMaxZ = Integer.MIN_VALUE;
			Set<Long> tCells = new HashSet<>();
			for (int tCell = 0; tCell < 26; tCell++) {
				int[] tPattern = GTMultiBlockPreviewRenderer.PATTERN_CELLS[tCell];
				int[] tOffset = GTMultiBlockPreviewRenderer.cellOffset(tFacing, tPattern[0], tPattern[1], tPattern[2]);
				tMinX = Math.min(tMinX, tOffset[0]); tMaxX = Math.max(tMaxX, tOffset[0]);
				tMinY = Math.min(tMinY, tOffset[1]); tMaxY = Math.max(tMaxY, tOffset[1]);
				tMinZ = Math.min(tMinZ, tOffset[2]); tMaxZ = Math.max(tMaxZ, tOffset[2]);
				tCells.add(pack(tOffset));
			}
			int[] tExpected = tBounds[tFacingIndex];
			assertEquals(tExpected[0], tMinX, "facing " + tFacing + " minX");
			assertEquals(tExpected[1], tMaxX, "facing " + tFacing + " maxX");
			assertEquals(tExpected[2], tMinY, "facing " + tFacing + " minY (level shell)");
			assertEquals(tExpected[3], tMaxY, "facing " + tFacing + " maxY (level shell)");
			assertEquals(tExpected[4], tMinZ, "facing " + tFacing + " minZ");
			assertEquals(tExpected[5], tMaxZ, "facing " + tFacing + " maxZ");
			// the air centre is inside the shell, and the controller's own cell (the world
			// offset (0,0,0) — the tTileEntity == aController pass,
			// ITileEntityMultiBlockController.java:88) is one of the 26 brick positions in
			// every facing
			assertTrue(tCells.contains(pack(new int[] { 0, 0, 0 })), "controller cell inside shell, facing " + tFacing);
			assertEquals(26, tCells.size(), "26 distinct world cells, facing " + tFacing);
		}
	}

	private static long pack(int[] aCell) {
		return ((long)(aCell[0] + 128) << 16) | ((long)(aCell[1] + 128) << 8) | (long)(aCell[2] + 128);
	}

	private static void assertArray(String aMessage, int[] aExpected, int[] aActual) {
		assertEquals(aExpected[0], aActual[0], aMessage + " x");
		assertEquals(aExpected[1], aActual[1], aMessage + " y");
		assertEquals(aExpected[2], aActual[2], aMessage + " z");
	}
}
