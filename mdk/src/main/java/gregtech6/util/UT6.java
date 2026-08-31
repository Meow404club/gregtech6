package gregtech6.util;

/**
 * The root UT port of the wrench side-picking math (task p4-pipe-flow-control spec ①):
 * {@link #getSideWrenching} is the verbatim port of {@code UT.Code.getSideWrenching}
 * (upstream gregapi/util/UT.java:1776-1798) together with its minimal dependencies —
 * the {@link #OPOS} table (upstream gregapi/data/CS.java:620, {@code {1,0,3,2,5,4,6,6}})
 * and {@link #SIDE_INVALID} (CS.java:522, 6).
 *
 * <p>The GT6 side order 0..5 equals {@code Direction.get3DDataValue()} order
 * (DOWN, UP, NORTH, SOUTH, WEST, EAST — the TileEntityBase01Root port doc), and for
 * the first six entries {@code OPOS[i] == i ^ 1}, i.e. exactly
 * {@code Direction.getOpposite().get3DDataValue()}; the table is kept as a table
 * because the two upstream call sites index it directly (onPlaced :84 and the
 * edge/corner fallbacks :1779-1791).
 *
 * <p>Pure static functions, no MC classes — the whole surface is offline-testable
 * against the upstream branch table (edge 0.25/0.75 thresholds, corner fallback onto
 * OPOS[aSide], centre hit = aSide itself, invalid side = SIDE_INVALID).
 */
public final class UT6 {

	/** Upstream CS.java:620 — the opposite-side table (GT6 side order). */
	public static final byte[] OPOS = { 1, 0, 3, 2, 5, 4, 6, 6};

	/** Upstream CS.java:522 — SIDE_INVALID = 6. */
	public static final byte SIDE_INVALID = 6;

	/**
	 * Upstream CS.java:151 verbatim — the EU voltage ladder (V). {@link #tierMax} walks it;
	 * 8 * 4^i per step, sixteen entries up to 2^33.
	 */
	public static final long[] VOLTAGES = { 8, 32, 128,  512, 2048,  8192, 32768, 131072,  524288, 2097152,  8388608, 33554432, 134217728,  536870912, 2147483648L,  8589934592L};

	private UT6() {
	}

	/**
	 * Upstream UT.Code.tierMax (UT.java:1388-1393) verbatim — the first index of
	 * {@link #VOLTAGES} whose value is {@code >= |aSize|}, or the table length when the
	 * size is off the top of the ladder. Pure logic, offline-testable (task p8-d3 §①:
	 * the overcharge explosion-strength curve).
	 */
	public static byte tierMax(long aSize) {
		byte i = -1;
		aSize = Math.abs(aSize);
		while (++i < VOLTAGES.length) if (aSize <= VOLTAGES[i]) return i;
		return i;
	}

	/**
	 * This Function determines the direction a Block gets when being Wrenched.
	 * Upstream UT.java:1776-1798 verbatim — aSide is the clicked face, the hit
	 * coordinates are the 0..1 in-face offsets (1.20.1 caller: the BlockHitResult
	 * location minus the BlockPos components).
	 */
	public static byte getSideWrenching(byte aSide, float aHitX, float aHitY, float aHitZ) {
		switch (aSide) {
		case  0: case  1:
			if (aHitX < 0.25) return aHitZ < 0.25 || aHitZ > 0.75 ? OPOS[aSide] : 4;
			if (aHitX > 0.75) return aHitZ < 0.25 || aHitZ > 0.75 ? OPOS[aSide] : 5;
			if (aHitZ < 0.25) return 2;
			if (aHitZ > 0.75) return 3;
			return aSide;
		case  2: case  3:
			if (aHitX < 0.25) return aHitY < 0.25 || aHitY > 0.75 ? OPOS[aSide] : 4;
			if (aHitX > 0.75) return aHitY < 0.25 || aHitY > 0.75 ? OPOS[aSide] : 5;
			if (aHitY < 0.25) return 0;
			if (aHitY > 0.75) return 1;
			return aSide;
		case  4: case  5:
			if (aHitZ < 0.25) return aHitY < 0.25 || aHitY > 0.75 ? OPOS[aSide] : 2;
			if (aHitZ > 0.75) return aHitY < 0.25 || aHitY > 0.75 ? OPOS[aSide] : 3;
			if (aHitY < 0.25) return 0;
			if (aHitY > 0.75) return 1;
			return aSide;
		}
		return SIDE_INVALID;
	}
}
