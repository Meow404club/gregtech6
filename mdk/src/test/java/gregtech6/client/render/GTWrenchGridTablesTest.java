package gregtech6.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import gregtech6.client.render.GTWrenchGridTables.GTWrenchGridIcon;
import gregtech6.util.UT6;

/**
 * The wrench grid tables (task p5-wrench-ui-gtceu acceptance — offline, MC-free):
 * ① the full cell→side table 6 faces x 9 cells against a hardcoded expectation
 * (centre = the face, edges = the axis neighbours, corners = OPOS), ② per-cell
 * multi-sample equivalence — any point inside the icon quad maps back to the cell's
 * side via a raw UT6 call assembled with a test-local axis table, proving the drawn
 * cell is constructively the picked cell, ③ the icon table for both modes over the
 * bit x side combinations.
 */
public class GTWrenchGridTablesTest {

	// the in-face axis tables, test-local (axis ids 0=X, 1=Y, 2=Z; GT6 side order
	// DOWN UP NORTH SOUTH WEST EAST) — the specification, independent of the Tables code
	private static final byte[] U_AXIS = { 0, 0, 0, 0, 2, 2 };
	private static final byte[] V_AXIS = { 2, 2, 1, 1, 1, 1 };

	private static byte rawPick(byte aFace, float aU, float aV) {
		float[] tHit = { 0.5F, 0.5F, 0.5F };
		tHit[U_AXIS[aFace]] = aU;
		tHit[V_AXIS[aFace]] = aV;
		return UT6.getSideWrenching(aFace, tHit[0], tHit[1], tHit[2]);
	}

	@Test
	public void inFaceAxisTables() {
		// {0,1}→(X,Z) / {2,3}→(X,Y) / {4,5}→(Z,Y)
		assertEquals(0, GTWrenchGridTables.cellUAxis((byte)0));
		assertEquals(2, GTWrenchGridTables.cellVAxis((byte)0));
		assertEquals(0, GTWrenchGridTables.cellUAxis((byte)1));
		assertEquals(2, GTWrenchGridTables.cellVAxis((byte)1));
		assertEquals(0, GTWrenchGridTables.cellUAxis((byte)2));
		assertEquals(1, GTWrenchGridTables.cellVAxis((byte)2));
		assertEquals(0, GTWrenchGridTables.cellUAxis((byte)3));
		assertEquals(1, GTWrenchGridTables.cellVAxis((byte)3));
		assertEquals(2, GTWrenchGridTables.cellUAxis((byte)4));
		assertEquals(1, GTWrenchGridTables.cellVAxis((byte)4));
		assertEquals(2, GTWrenchGridTables.cellUAxis((byte)5));
		assertEquals(1, GTWrenchGridTables.cellVAxis((byte)5));
	}

	@Test
	public void cellCenterIsSixthGrid() {
		// (2*col+1)/6 → 1/6, 3/6, 5/6
		assertEquals(1 / 6.0F, GTWrenchGridTables.cellCenter(0), 0.0F);
		assertEquals(3 / 6.0F, GTWrenchGridTables.cellCenter(1), 0.0F);
		assertEquals(5 / 6.0F, GTWrenchGridTables.cellCenter(2), 0.0F);
	}

	@Test
	public void cellSideFullTable54Cells() {
		// row-major over (row=v axis, col=u axis): row 0 is the NEAR end of the v axis
		// (v=1/6), row 2 the far end (v=5/6); col 0 the near end of the u axis.
		// face 0/1 (X,Z): col0=WEST, col2=EAST, row0=NORTH, row2=SOUTH
		// face 2/3 (X,Y): col0=WEST, col2=EAST, row0=DOWN,  row2=UP
		// face 4/5 (Z,Y): col0=NORTH, col2=SOUTH, row0=DOWN, row2=UP
		byte[][] tExpected = new byte[6][];
		tExpected[0] = new byte[] { UT6.OPOS[0], 2, UT6.OPOS[0], 4, 0, 5, UT6.OPOS[0], 3, UT6.OPOS[0] };
		tExpected[1] = new byte[] { UT6.OPOS[1], 2, UT6.OPOS[1], 4, 1, 5, UT6.OPOS[1], 3, UT6.OPOS[1] };
		tExpected[2] = new byte[] { UT6.OPOS[2], 0, UT6.OPOS[2], 4, 2, 5, UT6.OPOS[2], 1, UT6.OPOS[2] };
		tExpected[3] = new byte[] { UT6.OPOS[3], 0, UT6.OPOS[3], 4, 3, 5, UT6.OPOS[3], 1, UT6.OPOS[3] };
		tExpected[4] = new byte[] { UT6.OPOS[4], 0, UT6.OPOS[4], 2, 4, 3, UT6.OPOS[4], 1, UT6.OPOS[4] };
		tExpected[5] = new byte[] { UT6.OPOS[5], 0, UT6.OPOS[5], 2, 5, 3, UT6.OPOS[5], 1, UT6.OPOS[5] };

		int tAssertions = 0;
		for (byte tFace = 0; tFace < 6; tFace++) {
			for (int tRow = 0; tRow < 3; tRow++) {
				for (int tCol = 0; tCol < 3; tCol++) {
					byte tExpectedSide = tExpected[tFace][tRow * 3 + tCol];
					assertEquals(tExpectedSide, GTWrenchGridTables.cellSide(tFace, tCol, tRow),
							"face " + tFace + " cell (" + tCol + "," + tRow + ")");
					tAssertions++;
				}
			}
		}
		assertEquals(54, tAssertions, "the full 6x9 table");
	}

	@Test
	public void everyPointInsideIconQuadPicksTheCellSide() {
		// the icon quad of cell (col,row) spans [origin+m, origin+CELL-m] on both
		// in-face axes in 1/16 units; sample its 4 corners + centre (margin-inset) and
		// assert a raw UT6 pick — assembled with the test-local axis tables — equals
		// the cell table value, i.e. the drawn cell is constructively the picked cell
		int tAssertions = 0;
		for (byte tFace = 0; tFace < 6; tFace++) {
			for (int tRow = 0; tRow < 3; tRow++) {
				for (int tCol = 0; tCol < 3; tCol++) {
					byte tCellSide = GTWrenchGridTables.cellSide(tFace, tCol, tRow);
					float tU0 = (GTWrenchGridTables.CELL_ORIGIN_16[tCol] + GTWrenchGridTables.MARGIN_16) / 16.0F;
					float tU1 = (GTWrenchGridTables.CELL_ORIGIN_16[tCol] + GTWrenchGridTables.CELL_16 - GTWrenchGridTables.MARGIN_16) / 16.0F;
					float tV0 = (GTWrenchGridTables.CELL_ORIGIN_16[tRow] + GTWrenchGridTables.MARGIN_16) / 16.0F;
					float tV1 = (GTWrenchGridTables.CELL_ORIGIN_16[tRow] + GTWrenchGridTables.CELL_16 - GTWrenchGridTables.MARGIN_16) / 16.0F;
					float[][] tSamples = { {tU0, tV0}, {tU0, tV1}, {tU1, tV0}, {tU1, tV1}, { (tU0 + tU1) / 2, (tV0 + tV1) / 2 } };
					for (float[] tSample : tSamples) {
						assertEquals(tCellSide, rawPick(tFace, tSample[0], tSample[1]),
								"face " + tFace + " cell (" + tCol + "," + tRow + ") sample u=" + tSample[0] + " v=" + tSample[1]);
						tAssertions++;
					}
				}
			}
		}
		assertEquals(270, tAssertions, "54 cells x 5 samples");
	}

	@Test
	public void iconTableBothModesAllBitSideCombinations() {
		// NORMAL: connected bit → PIPE_CONNECT else PIPE_BLOCK
		// SHIFT:  ioMask bit → IO_FACING_ROTATION else PIPE_BLOCK
		// (the ioMask bit displays without a connection — display semantics)
		for (byte tSide = 0; tSide < 6; tSide++) {
			int tBit = 1 << tSide;
			assertEquals(GTWrenchGridIcon.PIPE_CONNECT, GTWrenchGridTables.iconFor(false, tSide, (byte)tBit, (byte)0), "normal connected " + tSide);
			assertEquals(GTWrenchGridIcon.PIPE_BLOCK, GTWrenchGridTables.iconFor(false, tSide, (byte)0, (byte)0), "normal disconnected " + tSide);
			assertEquals(GTWrenchGridIcon.IO_FACING_ROTATION, GTWrenchGridTables.iconFor(true, tSide, (byte)0, (byte)tBit), "shift marked " + tSide);
			assertEquals(GTWrenchGridIcon.PIPE_BLOCK, GTWrenchGridTables.iconFor(true, tSide, (byte)0, (byte)0), "shift unmarked " + tSide);
			// neighbour bits of the same mask never leak into this side's icon
			byte tOthers = (byte)(tBit ^ 63);
			assertEquals(GTWrenchGridIcon.PIPE_BLOCK, GTWrenchGridTables.iconFor(false, tSide, tOthers, (byte)0), "normal others-connected " + tSide);
			assertEquals(GTWrenchGridIcon.PIPE_BLOCK, GTWrenchGridTables.iconFor(true, tSide, (byte)0, tOthers), "shift others-marked " + tSide);
		}
	}

	@Test
	public void iconTexturePaths() {
		assertEquals("textures/gui/overlay/tool_pipe_connect.png", GTWrenchGridIcon.PIPE_CONNECT.texturePath);
		assertEquals("textures/gui/overlay/tool_pipe_block.png", GTWrenchGridIcon.PIPE_BLOCK.texturePath);
		assertEquals("textures/gui/overlay/tool_io_facing_rotation.png", GTWrenchGridIcon.IO_FACING_ROTATION.texturePath);
	}
}
