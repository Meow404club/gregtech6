package gregtech6.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.world.level.block.state.BlockState;

import gregtech6.multiblock.GTMultiBlockPattern;

/**
 * The ghost preview emission pin (tasks p10-ghost-preview-poc → p12-ghost-pattern-api,
 * offline): the renderer now draws whatever the pattern API declares, and this test
 * pins the drawn result to the POC's literal draw — ① a recording
 * {@link VertexConsumer} captures the exact vertex stream of
 * {@link GTMultiBlockPreviewRenderer#emitPattern} and compares it vertex-by-vertex
 * (positions AND colours, exact float equality) against the POC emission recomputed
 * here from its hardcoded 26-cell table (GTMultiBlockPreviewRenderer.java PATTERN_CELLS
 * at c25ed08) — per facing, formed and unformed: the card's "frame-equal" acceptance;
 * ② the four-facing full-table rotation — every one of the 26 cells plus the air centre
 * maps through the pattern API to the upstream
 * {@code controller - OFF[facing] + (i, j, k)} arithmetic against a test-local mirror of
 * the OFF tables (TileEntityBase01Root.java:174-176 — the specification, independent of
 * the pattern code), with the centre offsets hardcoded per facing; ③ the shell geometry
 * per facing: the pattern bounds always span the 3x3x3 cube one cell BEHIND the
 * controller and level with it.
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

	/** The flat grey of the centre air cell — the POC COLOR_AIR. */
	private static final float COLOR_AIR = 0.5F;

	@Test
	public void emissionIsFrameEqualToThePocDraw() {
		float tRG = 0.3F; // any fixed pulse value — both sides receive the same one
		byte[] tFacings = { 2, 3, 4, 5 };
		GTMultiBlockPattern tPattern = cokeOvenPatternLikePoc();
		PoseStack tPose = new PoseStack();
		int tComparisons = 0;
		for (byte tFacing : tFacings) {
			// the structure centre for a controller at (100, 64, 100)
			int tCx = 100 - OFF_X[tFacing], tCy = 64 - OFF_Y[tFacing], tCz = 100 - OFF_Z[tFacing];
			for (boolean tFormed : new boolean[] { false, true }) {
				Recorder tActual = new Recorder();
				GTMultiBlockPreviewRenderer.emitPattern(tPose.last(), tActual, tCx, tCy, tCz, tPattern, tFormed, tRG);
				Recorder tExpected = new Recorder();
				pocDraw(tExpected, tCx, tCy, tCz, tFormed, tRG);
				String tWhere = "facing " + tFacing + " formed " + tFormed;
				tExpected.assertSameStream(tActual, tWhere);
				tComparisons++;
			}
		}
		assertEquals(8, tComparisons, "four facings x two draw modes, all pinned");
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
	// the POC emission, recomputed literally — the frame-equality spec
	// -------------------------------------------------------------------------

	/** The POC's hardcoded PATTERN_CELLS (the literal upstream loop minus the centre). */
	private static final int[][] POC_CELLS = {
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

	/** The POC renderPreview draw body for an already-centred (cx, cy, cz). */
	private static void pocDraw(Recorder aRecorder, int aCx, int aCy, int aCz, boolean aFormed, float aRG) {
		if (aFormed) {
			// drawBoxEdges(pose, buffer, tCx, tCy, tCz, 1.5F, tRG, 1.0F) — the shell ruling
			pocBox(aRecorder, aCx - 1.5F, aCy - 1.5F, aCz - 1.5F, aCx + 1.5F, aCy + 1.5F, aCz + 1.5F, aRG, 1.0F);
		} else {
			for (int[] tCell : POC_CELLS) {
				pocBox(aRecorder, aCx + tCell[0] - 0.5F, aCy + tCell[1] - 0.5F, aCz + tCell[2] - 0.5F,
						aCx + tCell[0] + 0.5F, aCy + tCell[1] + 0.5F, aCz + tCell[2] + 0.5F, aRG, 1.0F);
			}
			// the centre air cell in flat grey
			pocBox(aRecorder, aCx - 0.5F, aCy - 0.5F, aCz - 0.5F, aCx + 0.5F, aCy + 0.5F, aCz + 0.5F, COLOR_AIR, COLOR_AIR);
		}
	}

	/** The POC drawBoxEdges (center ± half unfolded to min/max), edge order verbatim. */
	private static void pocBox(Recorder aRecorder, float x0, float y0, float z0, float x1, float y1, float z1, float aRG, float aB) {
		// the 4 bottom edges
		aRecorder.edge(x0, y0, z0, x1, y0, z0, aRG, aB);
		aRecorder.edge(x1, y0, z0, x1, y0, z1, aRG, aB);
		aRecorder.edge(x1, y0, z1, x0, y0, z1, aRG, aB);
		aRecorder.edge(x0, y0, z1, x0, y0, z0, aRG, aB);
		// the 4 top edges
		aRecorder.edge(x0, y1, z0, x1, y1, z0, aRG, aB);
		aRecorder.edge(x1, y1, z0, x1, y1, z1, aRG, aB);
		aRecorder.edge(x1, y1, z1, x0, y1, z1, aRG, aB);
		aRecorder.edge(x0, y1, z1, x0, y1, z0, aRG, aB);
		// the 4 vertical edges
		aRecorder.edge(x0, y0, z0, x0, y1, z0, aRG, aB);
		aRecorder.edge(x1, y0, z0, x1, y1, z0, aRG, aB);
		aRecorder.edge(x1, y0, z1, x1, y1, z1, aRG, aB);
		aRecorder.edge(x0, y0, z1, x0, y1, z1, aRG, aB);
	}

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
	 * colours of the LINES emission; normals/uv/lightmap are swallowed (drawLine emits
	 * the face normal per endpoint, identical on both sides by construction).
	 */
	private static final class Recorder implements VertexConsumer {

		private final List<float[]> mVertices = new ArrayList<>();
		private float mX, mY, mZ;

		private void edge(float x0, float y0, float z0, float x1, float y1, float z1, float aRG, float aB) {
			vertexRaw(x0, y0, z0, aRG, aRG, aB, 1.0F);
			vertexRaw(x1, y1, z1, aRG, aRG, aB, 1.0F);
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
		public VertexConsumer vertex(double pX, double pY, double pZ) {
			mX = (float)pX; mY = (float)pY; mZ = (float)pZ;
			return this;
		}

		@Override
		public VertexConsumer vertex(org.joml.Matrix4f pMatrix, float pX, float pY, float pZ) {
			mX = pX; mY = pY; mZ = pZ; // the identity pose — recorded raw
			return this;
		}

		@Override
		public VertexConsumer color(int pR, int pG, int pB, int pA) {
			vertexRaw(mX, mY, mZ, pR / 255.0F, pG / 255.0F, pB / 255.0F, pA / 255.0F);
			return this;
		}

		@Override
		public VertexConsumer color(float pR, float pG, float pB, float pA) {
			vertexRaw(mX, mY, mZ, pR, pG, pB, pA);
			return this;
		}

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

		@Override
		public void defaultColor(int pR, int pG, int pB, int pA) { }

		@Override
		public void unsetDefaultColor() { }
	}
}
