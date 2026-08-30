package gregtech6.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * The getSideWrenching parse table (task p4-pipe-flow-control acceptance ①): every
 * branch of the upstream UT.java:1776-1798 switch exercised per face — the 0.25/0.75
 * edge thresholds, the corner fallback onto OPOS[aSide], the centre hit returning the
 * face itself, and the SIDE_INVALID fall-through.
 */
public class UT6SideWrenchingTest {

	@Test
	public void oposTableIsUpstreamVerbatim() {
		// CS.java:620 — OPOS[i] == i^1 for the valid sides, 6 for the invalid tail
		byte[] tExpected = { 1, 0, 3, 2, 5, 4, 6, 6 };
		assertEquals(8, UT6.OPOS.length);
		for (int i = 0; i < 8; i++) assertEquals(tExpected[i], UT6.OPOS[i], "OPOS[" + i + "]");
	}

	@Test
	public void bottomAndTopFacesSplitOnXZ() {
		// face 0 (DOWN) / face 1 (UP): the X thresholds pick X, the Z thresholds pick Z
		for (byte tFace : new byte[] {0, 1}) {
			// centre hit = the face itself (:1783)
			assertEquals(tFace, UT6.getSideWrenching(tFace, 0.5F, 0.5F, 0.5F));
			// X edge with Z centre → WEST/EAST (:1779/:1780)
			assertEquals(4, UT6.getSideWrenching(tFace, 0.1F, 0.5F, 0.5F), "face " + tFace + " x<0.25");
			assertEquals(5, UT6.getSideWrenching(tFace, 0.9F, 0.5F, 0.5F), "face " + tFace + " x>0.75");
			// X centre, Z edge → NORTH/SOUTH (:1781/:1782)
			assertEquals(2, UT6.getSideWrenching(tFace, 0.5F, 0.5F, 0.1F), "face " + tFace + " z<0.25");
			assertEquals(3, UT6.getSideWrenching(tFace, 0.5F, 0.5F, 0.9F), "face " + tFace + " z>0.75");
			// X edge AND Z edge → corner fallback onto OPOS (:1779/:1780)
			assertEquals(UT6.OPOS[tFace], UT6.getSideWrenching(tFace, 0.1F, 0.5F, 0.1F), "face " + tFace + " corner -,-");
			assertEquals(UT6.OPOS[tFace], UT6.getSideWrenching(tFace, 0.9F, 0.5F, 0.9F), "face " + tFace + " corner +,+");
			// threshold boundaries are strict: 0.25 and 0.75 themselves are inside the centre band
			assertEquals(tFace, UT6.getSideWrenching(tFace, 0.25F, 0.5F, 0.75F));
			// both X thresholds hit at once is impossible; both Z thresholds hit at once is
			// impossible too — but the Z edge inside the X corner band picks Z before falling back
			// (the OR-corner only fires when BOTH tangent axes are outside)
		}
	}

	@Test
	public void northAndSouthFacesSplitOnXY() {
		// face 2 (NORTH) / face 3 (SOUTH): X thresholds pick X, Y thresholds pick Y
		for (byte tFace : new byte[] {2, 3}) {
			assertEquals(tFace, UT6.getSideWrenching(tFace, 0.5F, 0.5F, 0.5F));
			assertEquals(4, UT6.getSideWrenching(tFace, 0.1F, 0.5F, 0.5F), "face " + tFace + " x<0.25");
			assertEquals(5, UT6.getSideWrenching(tFace, 0.9F, 0.5F, 0.5F), "face " + tFace + " x>0.75");
			assertEquals(0, UT6.getSideWrenching(tFace, 0.5F, 0.1F, 0.5F), "face " + tFace + " y<0.25");
			assertEquals(1, UT6.getSideWrenching(tFace, 0.5F, 0.9F, 0.5F), "face " + tFace + " y>0.75");
			assertEquals(UT6.OPOS[tFace], UT6.getSideWrenching(tFace, 0.1F, 0.1F, 0.5F), "face " + tFace + " corner -,-");
			assertEquals(UT6.OPOS[tFace], UT6.getSideWrenching(tFace, 0.9F, 0.9F, 0.5F), "face " + tFace + " corner +,+");
		}
	}

	@Test
	public void westAndEastFacesSplitOnZY() {
		// face 4 (WEST) / face 5 (EAST): Z thresholds pick Z, Y thresholds pick Y
		for (byte tFace : new byte[] {4, 5}) {
			assertEquals(tFace, UT6.getSideWrenching(tFace, 0.5F, 0.5F, 0.5F));
			assertEquals(2, UT6.getSideWrenching(tFace, 0.5F, 0.5F, 0.1F), "face " + tFace + " z<0.25");
			assertEquals(3, UT6.getSideWrenching(tFace, 0.5F, 0.5F, 0.9F), "face " + tFace + " z>0.75");
			assertEquals(0, UT6.getSideWrenching(tFace, 0.5F, 0.1F, 0.5F), "face " + tFace + " y<0.25");
			assertEquals(1, UT6.getSideWrenching(tFace, 0.5F, 0.9F, 0.5F), "face " + tFace + " y>0.75");
			assertEquals(UT6.OPOS[tFace], UT6.getSideWrenching(tFace, 0.5F, 0.1F, 0.1F), "face " + tFace + " corner -,-");
			assertEquals(UT6.OPOS[tFace], UT6.getSideWrenching(tFace, 0.5F, 0.9F, 0.9F), "face " + tFace + " corner +,+");
		}
	}

	@Test
	public void invalidSideFallsThrough() {
		// :1797 — anything outside 0..5 returns SIDE_INVALID (6)
		assertEquals(UT6.SIDE_INVALID, UT6.getSideWrenching((byte)6, 0.5F, 0.5F, 0.5F));
		assertEquals(UT6.SIDE_INVALID, UT6.getSideWrenching((byte)-1, 0.5F, 0.5F, 0.5F));
		assertEquals(6, UT6.SIDE_INVALID);
	}
}
