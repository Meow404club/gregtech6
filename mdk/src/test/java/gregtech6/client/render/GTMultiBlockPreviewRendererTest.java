package gregtech6.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.world.level.block.state.BlockState;

import gregtech6.multiblock.GTMultiBlockPattern;

/**
 * The ghost preview emission pin (tasks p10-ghost-preview-poc → p12-ghost-pattern-api →
 * p12-ghost-render-match, offline): ① the FORMED shell draw is STILL the POC's literal
 * draw — a recording {@link VertexConsumer} captures the exact vertex stream of
 * {@link GTMultiBlockPreviewRenderer#emitFormedShell} and compares it vertex-by-vertex
 * (positions AND colours, exact float equality) against the POC shell emission
 * recomputed here (the c25ed08 draw) — per facing, the "FORMED = zero change" ruling;
 * ② the UNFORMED green/red stream — per-cell translucent face quads
 * ({@link RenderType#debugQuads()} calibre: vertex + colour only, face alpha 0.3) plus
 * the opaque same-colour edge wire, GREEN (0.2, 1.0, 0.2) / RED (1.0, 0.2, 0.2),
 * SKIP cells painting nothing, verdict order the declaration order, vertices inset by
 * 0.002 from the block bounds (the z-fight ruling); ③ the four-facing full-table
 * rotation — every one of the 26 cells plus the air centre maps through the pattern
 * API to the upstream {@code controller - OFF[facing] + (i, j, k)} arithmetic against a
 * test-local mirror of the OFF tables (TileEntityBase01Root.java:174-176 — the
 * specification, independent of the pattern code), with the centre offsets hardcoded
 * per facing; ④ the shell geometry per facing: the pattern bounds always span the
 * 3x3x3 cube one cell BEHIND the controller and level with it.
 *
 * <p>Nothing here touches a real GL surface: the recorder swallows the vertex calls,
 * the pose stack is identity math. The pattern must never be derived from
 * {@code checkStructure2} — its centre cell writes the world, TileEntityCokeOven.java:
 * 100-101.
 */
public class GTMultiBlockPreviewRendererTest extends GTOfflineRenderTestBase {

	// the GT6 side-offset tables, test-local mirror (TileEntityBase01Root.java:174-176;
	// side order DOWN UP NORTH SOUTH WEST EAST = vanilla get3DDataValue)
	private static final int[] OFF_X = { 0, 0, 0, 0, -1, 1 };
	private static final int[] OFF_Y = { 0, 1, 0, 0, 0, 0 };
	private static final int[] OFF_Z = { 0, 0, -1, 1, 0, 0 };

	/** The face alpha — the Litematica wrongBlock calibre (#4CFF3333 ≙ alpha 0.30). */
	private static final float FACE_ALPHA = 0.3F;
	/** The green/red triplets — the renderer constants' test-local mirror (the spec). */
	private static final float[] GREEN = { 0.2F, 1.0F, 0.2F };
	private static final float[] RED = { 1.0F, 0.2F, 0.2F };
	/** The z-fight vertex inset — the renderer constant's test-local mirror. */
	private static final float INSET = 0.002F;

	// -------------------------------------------------------------------------
	// ① the FORMED shell — still the POC draw, frame-equal (the zero-change ruling)
	// -------------------------------------------------------------------------

	@Test
	public void formedShellIsFrameEqualToThePocDraw() {
		float tRG = 0.3F; // any fixed pulse value — both sides receive the same one
		byte[] tFacings = { 2, 3, 4, 5 };
		GTMultiBlockPattern tPattern = cokeOvenPatternLikePoc();
		PoseStack tPose = new PoseStack();
		int tComparisons = 0;
		for (byte tFacing : tFacings) {
			// the structure centre for a controller at (100, 64, 100)
			int tCx = 100 - OFF_X[tFacing], tCy = 64 - OFF_Y[tFacing], tCz = 100 - OFF_Z[tFacing];
			Recorder tActual = new Recorder();
			GTMultiBlockPreviewRenderer.emitFormedShell(tPose.last(), tActual, tCx, tCy, tCz, tPattern, tRG);
			Recorder tExpected = new Recorder();
			pocShell(tExpected, tCx, tCy, tCz, tRG);
			tExpected.assertSameStream(tActual, "facing " + tFacing);
			tComparisons++;
		}
		assertEquals(4, tComparisons, "four facings, all pinned");
	}

	// -------------------------------------------------------------------------
	// ② the UNFORMED green/red stream
	// -------------------------------------------------------------------------

	/** All-SKIP verdicts paint nothing at all (the matched hollow centre, the Coke Oven formed-centre case). */
	@Test
	public void allSkipPaintsNothing() {
		GTMultiBlockPattern tPattern = singleCellPattern(0, 0, 0);
		Recorder tFaces = new Recorder(), tLines = new Recorder();
		PoseStack tPose = new PoseStack();
		GTMultiBlockPreviewRenderer.emitUnformed(tPose.last(), tFaces, tLines, tPattern,
				verdicts(GTMultiBlockGhostMatcher.Verdict.SKIP), 0, 0, 0);
		assertEquals(0, tFaces.mVertices.size(), "no face vertices for a skipped cell");
		assertEquals(0, tLines.mVertices.size(), "no edge vertices for a skipped cell");
	}

	/** One GREEN cell: 24 face vertices (6 quads x 4, debugQuads calibre, alpha 0.3) + 24 opaque edge vertices, inset by 0.002. */
	@Test
	public void greenCellPaintsTranslucentFacesAndOpaqueEdges() {
		assertCellStream(GTMultiBlockGhostMatcher.Verdict.GREEN, GREEN, "green");
		assertCellStream(GTMultiBlockGhostMatcher.Verdict.RED, RED, "red");
	}

	private static void assertCellStream(GTMultiBlockGhostMatcher.Verdict aVerdict, float[] aRGB, String aLabel) {
		GTMultiBlockPattern tPattern = singleCellPattern(0, 0, 1);
		Recorder tFaces = new Recorder(), tLines = new Recorder();
		PoseStack tPose = new PoseStack();
		// structure centre at the origin → the cell (0,0,1) spans x/y in [-0.5+.., 0.5-..], z in [0.5+.., 1.5-..]
		GTMultiBlockPreviewRenderer.emitUnformed(tPose.last(), tFaces, tLines, tPattern,
				verdicts(aVerdict), 0, 0, 0);
		// the same arithmetic as emitGhostCell, in the same evaluation order (bit-equality)
		float tX0 = -0.5F + INSET, tY0 = -0.5F + INSET, tZ0 = 0.5F + INSET;
		float tX1 = 0.5F - INSET, tY1 = 0.5F - INSET, tZ1 = 1.5F - INSET;
		// the face stream: 6 quads x 4 vertices, colour (r, g, b, 0.3)
		Recorder tExpectedFaces = new Recorder();
		expectedCellFaces(tExpectedFaces, tX0, tY0, tZ0, tX1, tY1, tZ1, aRGB);
		tExpectedFaces.assertSameStream(tFaces, aLabel + " faces");
		// the edge stream: 12 edges x 2 vertices, same colour opaque
		Recorder tExpectedLines = new Recorder();
		edgeLoop(tExpectedLines, tX0, tY0, tZ0, tX1, tY1, tZ1, aRGB[0], aRGB[1], aRGB[2]);
		tExpectedLines.assertSameStream(tLines, aLabel + " edges");
	}

	/** The verdict order is the declaration order; SKIP cells are passed over, colours follow the verdicts. */
	@Test
	public void verdictOrderFollowsDeclarationOrder() {
		GTMultiBlockPattern.Builder tBuilder = GTMultiBlockPattern.builder();
		tBuilder.part(0, 0, 0, (BlockState aState) -> true);
		tBuilder.part(1, 0, 0, (BlockState aState) -> true);
		tBuilder.part(2, 0, 0, (BlockState aState) -> true);
		GTMultiBlockPattern tPattern = tBuilder.build();
		Recorder tFaces = new Recorder(), tLines = new Recorder();
		PoseStack tPose = new PoseStack();
		GTMultiBlockPreviewRenderer.emitUnformed(tPose.last(), tFaces, tLines, tPattern, Arrays.asList(
				GTMultiBlockGhostMatcher.Verdict.GREEN,
				GTMultiBlockGhostMatcher.Verdict.SKIP,
				GTMultiBlockGhostMatcher.Verdict.RED), 0, 0, 0);
		// 2 painted cells x 24 vertices; the first 24 green, the last 24 red
		assertEquals(48, tFaces.mVertices.size(), "the skipped cell paints no faces");
		assertEquals(48, tLines.mVertices.size(), "the skipped cell paints no edges");
		for (int i = 0; i < 24; i++) assertVertexColour(tFaces.mVertices.get(i), GREEN, FACE_ALPHA, "face " + i + " green half");
		for (int i = 24; i < 48; i++) assertVertexColour(tFaces.mVertices.get(i), RED, FACE_ALPHA, "face " + i + " red half");
		for (int i = 0; i < 24; i++) assertVertexColour(tLines.mVertices.get(i), GREEN, 1.0F, "edge " + i + " green half");
		for (int i = 24; i < 48; i++) assertVertexColour(tLines.mVertices.get(i), RED, 1.0F, "edge " + i + " red half");
		// the painted cells are cell 0 (centre 0,0,0 → x in [-0.498, 0.498]) and cell 2 (centre 2,0,0 → x in [1.502, 2.498])
		assertFaceSpan(tFaces, 0, 24, -0.5F + INSET, 0.5F - INSET, -0.5F + INSET, 0.5F - INSET, -0.5F + INSET, 0.5F - INSET);
		assertFaceSpan(tFaces, 24, 48, 1.5F + INSET, 2.5F - INSET, -0.5F + INSET, 0.5F - INSET, -0.5F + INSET, 0.5F - INSET);
	}

	private static void assertVertexColour(float[] aVertex, float[] aRGB, float aAlpha, String aWhere) {
		assertEquals(aRGB[0], aVertex[3], 0.0F, aWhere + " r");
		assertEquals(aRGB[1], aVertex[4], 0.0F, aWhere + " g");
		assertEquals(aRGB[2], aVertex[5], 0.0F, aWhere + " b");
		assertEquals(aAlpha, aVertex[6], 0.0F, aWhere + " a");
	}

	/** Every vertex in [aFrom, aTo) of the recorder lies exactly on the given inset box surface. */
	private static void assertFaceSpan(Recorder aRecorder, int aFrom, int aTo,
			float aMinX, float aMaxX, float aMinY, float aMaxY, float aMinZ, float aMaxZ) {
		for (int i = aFrom; i < aTo; i++) {
			float[] tV = aRecorder.mVertices.get(i);
			assertTrue(on(tV[0], aMinX, aMaxX), "vertex " + i + " x on the inset surface");
			assertTrue(on(tV[1], aMinY, aMaxY), "vertex " + i + " y on the inset surface");
			assertTrue(on(tV[2], aMinZ, aMaxZ), "vertex " + i + " z on the inset surface");
		}
	}

	private static boolean on(float aValue, float aMin, float aMax) {
		return aValue == aMin || aValue == aMax;
	}

	// -------------------------------------------------------------------------
	// ③/④ the pattern API rotation + shell geometry (unchanged from the pattern-api card)
	// -------------------------------------------------------------------------

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
		GTMultiBlockPattern tPattern = cokeOvenPatternLikePoc();
		GTMultiBlockPattern.Cell tCentre = tPattern.cells().get(26); // appended last, the POC draw order
		int tAssertions = 0;
		for (int tFacingIndex = 0; tFacingIndex < 4; tFacingIndex++) {
			byte tFacing = tFacings[tFacingIndex];
			// the air centre maps to the hardcoded structure centre
			assertArray("centre", tHardcodedCentres[tFacingIndex],
					GTMultiBlockPattern.cellOffset(tFacing, 0, 0, 0));
			tAssertions++;
			// all 26 brick cells: controller - OFF[facing] + (i, j, k) — the upstream
			// checkStructure2 arithmetic (TileEntityCokeOven.java:92/:97-111), through both
			// the static rotation and the instance path
			for (int tCell = 0; tCell < 26; tCell++) {
				GTMultiBlockPattern.Cell tBrick = tPattern.cells().get(tCell);
				int[] tExpected = {
						tBrick.x - OFF_X[tFacing],
						tBrick.y - OFF_Y[tFacing],
						tBrick.z - OFF_Z[tFacing]
				};
				assertArray("facing " + tFacing + " cell " + tCell, tExpected,
						GTMultiBlockPattern.cellOffset(tFacing, tBrick.x, tBrick.y, tBrick.z));
				assertArray("facing " + tFacing + " cell " + tCell + " (instance)", tExpected,
						tPattern.worldOffset(tFacing, tBrick));
				tAssertions += 2;
			}
			assertTrue(!tCentre.isHollow() == false, "cell 26 is the hollow centre marker");
			tAssertions++;
		}
		assertEquals(216, tAssertions, "26 cells x2 paths + centre + hollow check, four facings");
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
		GTMultiBlockPattern tPattern = cokeOvenPatternLikePoc();
		assertEquals(-1, tPattern.minX()); assertEquals(1, tPattern.maxX());
		assertEquals(-1, tPattern.minY()); assertEquals(1, tPattern.maxY());
		assertEquals(-1, tPattern.minZ()); assertEquals(1, tPattern.maxZ());
		for (int tFacingIndex = 0; tFacingIndex < 4; tFacingIndex++) {
			byte tFacing = tFacings[tFacingIndex];
			int[] tAnchor = GTMultiBlockPattern.anchorOffset(tFacing);
			int[] tExpected = tBounds[tFacingIndex];
			assertEquals(tExpected[0], tAnchor[0] + tPattern.minX(), "facing " + tFacing + " minX");
			assertEquals(tExpected[1], tAnchor[0] + tPattern.maxX(), "facing " + tFacing + " maxX");
			assertEquals(tExpected[2], tAnchor[1] + tPattern.minY(), "facing " + tFacing + " minY (level shell)");
			assertEquals(tExpected[3], tAnchor[1] + tPattern.maxY(), "facing " + tFacing + " maxY (level shell)");
			assertEquals(tExpected[4], tAnchor[2] + tPattern.minZ(), "facing " + tFacing + " minZ");
			assertEquals(tExpected[5], tAnchor[2] + tPattern.maxZ(), "facing " + tFacing + " maxZ");
			// the controller's own cell (the world offset (0,0,0) — the tTileEntity ==
			// aController pass, ITileEntityMultiBlockController.java:88) is one of the 26
			// brick positions in every facing
			Set<Long> tCells = new HashSet<>();
			for (GTMultiBlockPattern.Cell tBrick : tPattern.cells()) {
				if (!tBrick.isHollow()) {
					int[] tOffset = tPattern.worldOffset(tFacing, tBrick);
					tCells.add(pack(tOffset));
				}
			}
			assertTrue(tCells.contains(pack(new int[] { 0, 0, 0 })), "controller cell inside shell, facing " + tFacing);
			assertEquals(26, tCells.size(), "26 distinct world cells, facing " + tFacing);
		}
	}

	// -------------------------------------------------------------------------
	// the expected emissions, recomputed literally — the stream specs
	// -------------------------------------------------------------------------

	/** The POC's formed-shell draw for an already-centred (cx, cy, cz) — the shell ruling, bit-equal target. */
	private static void pocShell(Recorder aRecorder, int aCx, int aCy, int aCz, float aRG) {
		pocBox(aRecorder, aCx - 1.5F, aCy - 1.5F, aCz - 1.5F, aCx + 1.5F, aCy + 1.5F, aCz + 1.5F, aRG, 1.0F);
	}

	/** The POC drawBoxEdges (center ± half unfolded to min/max), edge order verbatim. */
	private static void pocBox(Recorder aRecorder, float x0, float y0, float z0, float x1, float y1, float z1, float aRG, float aB) {
		// the 4 bottom edges
		aRecorder.edge(x0, y0, z0, x1, y0, z0, aRG, aRG, aB);
		aRecorder.edge(x1, y0, z0, x1, y0, z1, aRG, aRG, aB);
		aRecorder.edge(x1, y0, z1, x0, y0, z1, aRG, aRG, aB);
		aRecorder.edge(x0, y0, z1, x0, y0, z0, aRG, aRG, aB);
		// the 4 top edges
		aRecorder.edge(x0, y1, z0, x1, y1, z0, aRG, aRG, aB);
		aRecorder.edge(x1, y1, z0, x1, y1, z1, aRG, aRG, aB);
		aRecorder.edge(x1, y1, z1, x0, y1, z1, aRG, aRG, aB);
		aRecorder.edge(x0, y1, z1, x0, y1, z0, aRG, aRG, aB);
		// the 4 vertical edges
		aRecorder.edge(x0, y0, z0, x0, y1, z0, aRG, aRG, aB);
		aRecorder.edge(x1, y0, z0, x1, y1, z0, aRG, aRG, aB);
		aRecorder.edge(x1, y0, z1, x1, y1, z1, aRG, aRG, aB);
		aRecorder.edge(x0, y0, z1, x0, y1, z1, aRG, aRG, aB);
	}

	/**
	 * The expected face stream of one ghost cell spanning (x0..x1, y0..y1, z0..z1) —
	 * the {@code drawCellFaces} emission in its exact order: bottom, top, north, south,
	 * west, east, one quad each, colour (r, g, b, 0.3).
	 */
	private static void expectedCellFaces(Recorder aRecorder, float x0, float y0, float z0, float x1, float y1, float z1,
			float aR, float aG, float aB) {
		// bottom / top
		aRecorder.quad(x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, aR, aG, aB);
		aRecorder.quad(x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, aR, aG, aB);
		// north / south
		aRecorder.quad(x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, aR, aG, aB);
		aRecorder.quad(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, aR, aG, aB);
		// west / east
		aRecorder.quad(x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, aR, aG, aB);
		aRecorder.quad(x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, aR, aG, aB);
	}

	private static void expectedCellFaces(Recorder aRecorder, float x0, float y0, float z0, float x1, float y1, float z1, float[] aRGB) {
		expectedCellFaces(aRecorder, x0, y0, z0, x1, y1, z1, aRGB[0], aRGB[1], aRGB[2]);
	}

	/**
	 * The expected edge stream of one ghost cell — the {@code drawBoxEdges} order
	 * (the POC's bottom-4/top-4/vertical-4), same colour opaque.
	 */
	private static void expectedCellEdges(Recorder aRecorder, float x0, float y0, float z0, float x1, float y1, float z1, float[] aRGB) {
		edgeLoop(aRecorder, x0, y0, z0, x1, y1, z1, aRGB[0], aRGB[1], aRGB[2]);
	}

	private static void edgeLoop(Recorder aRecorder, float x0, float y0, float z0, float x1, float y1, float z1, float aR, float aG, float aB) {
		// the 4 bottom edges
		aRecorder.edge(x0, y0, z0, x1, y0, z0, aR, aG, aB);
		aRecorder.edge(x1, y0, z0, x1, y0, z1, aR, aG, aB);
		aRecorder.edge(x1, y0, z1, x0, y0, z1, aR, aG, aB);
		aRecorder.edge(x0, y0, z1, x0, y0, z0, aR, aG, aB);
		// the 4 top edges
		aRecorder.edge(x0, y1, z0, x1, y1, z0, aR, aG, aB);
		aRecorder.edge(x1, y1, z0, x1, y1, z1, aR, aG, aB);
		aRecorder.edge(x1, y1, z1, x0, y1, z1, aR, aG, aB);
		aRecorder.edge(x0, y1, z1, x0, y1, z0, aR, aG, aB);
		// the 4 vertical edges
		aRecorder.edge(x0, y0, z0, x0, y1, z0, aR, aG, aB);
		aRecorder.edge(x1, y0, z0, x1, y1, z0, aR, aG, aB);
		aRecorder.edge(x1, y0, z1, x1, y1, z1, aR, aG, aB);
		aRecorder.edge(x0, y0, z1, x0, y1, z1, aR, aG, aB);
	}

	// -------------------------------------------------------------------------
	// fixtures
	// -------------------------------------------------------------------------

	/** The declared Coke Oven shape, as TileEntityCokeOven.getStructurePattern builds it. */
	private static GTMultiBlockPattern cokeOvenPatternLikePoc() {
		GTMultiBlockPattern.Builder tBuilder = GTMultiBlockPattern.builder();
		for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) for (int k = -1; k <= 1; k++) {
			if (i == 0 && j == 0 && k == 0) continue;
			tBuilder.part(i, j, k, (BlockState aState) -> true);
		}
		tBuilder.hollow(0, 0, 0, (BlockState aState) -> true);
		return tBuilder.build();
	}

	private static GTMultiBlockPattern singleCellPattern(int aX, int aY, int aZ) {
		GTMultiBlockPattern.Builder tBuilder = GTMultiBlockPattern.builder();
		tBuilder.part(aX, aY, aZ, (BlockState aState) -> true);
		return tBuilder.build();
	}

	private static List<GTMultiBlockGhostMatcher.Verdict> verdicts(GTMultiBlockGhostMatcher.Verdict aVerdict) {
		List<GTMultiBlockGhostMatcher.Verdict> tVerdicts = new ArrayList<>();
		tVerdicts.add(aVerdict);
		return tVerdicts;
	}

	private static long pack(int[] aCell) {
		return ((long)(aCell[0] + 128) << 16) | ((long)(aCell[1] + 128) << 8) | (long)(aCell[2] + 128);
	}

	private static void assertArray(String aMessage, int[] aExpected, int[] aActual) {
		assertEquals(aExpected[0], aActual[0], aMessage + " x");
		assertEquals(aExpected[1], aActual[1], aMessage + " y");
		assertEquals(aExpected[2], aActual[2], aMessage + " z");
	}

	/**
	 * The recording vertex consumer — captures the exact per-vertex positions and
	 * colours of both the LINES and the debugQuads emission; normals/uv/lightmap are
	 * swallowed (drawLine emits the face normal per endpoint, identical on both sides by
	 * construction).
	 */
	private static final class Recorder implements VertexConsumer {

		private final List<float[]> mVertices = new ArrayList<>();
		private float mX, mY, mZ;

		private void edge(float x0, float y0, float z0, float x1, float y1, float z1, float aR, float aG, float aB) {
			vertexRaw(x0, y0, z0, aR, aG, aB, 1.0F);
			vertexRaw(x1, y1, z1, aR, aG, aB, 1.0F);
		}

		private void quad(float aX0, float aY0, float aZ0, float aX1, float aY1, float aZ1,
				float aX2, float aY2, float aZ2, float aX3, float aY3, float aZ3, float aR, float aG, float aB) {
			vertexRaw(aX0, aY0, aZ0, aR, aG, aB, FACE_ALPHA);
			vertexRaw(aX1, aY1, aZ1, aR, aG, aB, FACE_ALPHA);
			vertexRaw(aX2, aY2, aZ2, aR, aG, aB, FACE_ALPHA);
			vertexRaw(aX3, aY3, aZ3, aR, aG, aB, FACE_ALPHA);
		}

		private void vertexRaw(float aX, float aY, float aZ, float aR, float aG, float aB, float aA) {
			mVertices.add(new float[] { aX, aY, aZ, aR, aG, aB, aA });
		}

		private void assertSameStream(Recorder aOther, String aWhere) {
			assertEquals(mVertices.size(), aOther.mVertices.size(), aWhere + " — vertex count");
			for (int i = 0; i < mVertices.size(); i++) {
				float[] tExpected = mVertices.get(i);
				float[] tActual = aOther.mVertices.get(i);
				for (int a = 0; a < 7; a++) {
					assertEquals(tExpected[a], tActual[a], 0.0F,
							aWhere + " — component " + a + " of vertex " + i + " must be bit-equal");
				}
			}
		}

		@Override
		//? if forge {
		public VertexConsumer vertex(double pX, double pY, double pZ) {
		//?} else {
		/*public VertexConsumer addVertex(float pX, float pY, float pZ) { // 21.1: the double variant is gone, the float triple is the abstract
		*///?}
			mX = (float)pX; mY = (float)pY; mZ = (float)pZ;
			return this;
		}

		@Override
		//? if forge {
		public VertexConsumer vertex(org.joml.Matrix4f pMatrix, float pX, float pY, float pZ) {
		//?} else {
		/*public VertexConsumer addVertex(org.joml.Matrix4f pMatrix, float pX, float pY, float pZ) {
		*///?}
			mX = pX; mY = pY; mZ = pZ; // the identity pose — recorded raw
			return this;
		}

		@Override
		//? if forge {
		public VertexConsumer color(int pR, int pG, int pB, int pA) {
		//?} else {
		/*public VertexConsumer setColor(int pR, int pG, int pB, int pA) {
		*///?}
			vertexRaw(mX, mY, mZ, pR / 255.0F, pG / 255.0F, pB / 255.0F, pA / 255.0F);
			return this;
		}

		@Override
		//? if forge {
		public VertexConsumer color(float pR, float pG, float pB, float pA) {
		//?} else {
		/*public VertexConsumer setColor(float pR, float pG, float pB, float pA) {
		*///?}
			vertexRaw(mX, mY, mZ, pR, pG, pB, pA);
			return this;
		}

		//? if forge {
		@Override
		public VertexConsumer uv(float pU, float pV) { return this; }

		@Override
		public VertexConsumer overlayCoords(int pU, int pV) { return this; }

		@Override
		public VertexConsumer uv2(int pU, int pV) { return this; }

		@Override
		public VertexConsumer normal(float pX, float pY, float pZ) { return this; }

		@Override
		public void endVertex() { }
		//?} else {
		/*@Override
		public VertexConsumer setUv(float pU, float pV) { return this; }

		@Override
		public VertexConsumer setUv1(int pU, int pV) { return this; } // 21.1: overlayCoords → setUv1

		@Override
		public VertexConsumer setUv2(int pU, int pV) { return this; }

		@Override
		public VertexConsumer setLight(int pLight) { return this; }

		@Override
		public VertexConsumer setNormal(float pX, float pY, float pZ) { return this; }
		// 21.1: endVertex is gone (the 1.21 writer flushes per call) — nothing to stub.
		*///?}

		//? if forge {
		@Override
		public void defaultColor(int pR, int pG, int pB, int pA) { }

		@Override
		public void unsetDefaultColor() { }
		//?} else {
		/*// 21.1: the defaultColor face left VertexConsumer — nothing to stub.
		*///?}
	}
}
