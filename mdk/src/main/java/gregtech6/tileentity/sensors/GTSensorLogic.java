package gregtech6.tileentity.sensors;

/**
 * The sensor pure-logic core (task p26-sensors-core) — the MC-free port of the numeric
 * layer of the upstream double base:
 * <ul>
 * <li>{@code MultiTileEntitySensorTE} (gregapi/tileentity/machines/MultiTileEntitySensorTE
 *     .java:46) — the 8 modes (:47-58), the sliding-average window over
 *     {@code MAX_AVERAGING_VALUES} (:60), the sample-to-redstone switch (:141-150) and
 *     the 3x3 keypad arithmetic (:164-200);</li>
 * <li>{@code MultiTileEntitySensor} (:53) — the display-sync diff gate
 *     {@code Math.abs(mDisplayedNumber - oDisplayedNumber) > 49} (:108-111) and the
 *     screwdriver mode packing (hex flag {@code B[7]} kept across mode cycles,
 *     SensorTE:230-236).</li>
 * </ul>
 *
 * <p>Every numeric helper is the verbatim {@code UT.Code} port with the upstream line
 * anchor in its javadoc ({@code bind16} UT.java:1515, {@code unsignS} :1228,
 * {@code bindInt} :1524, {@code averageInts} :1194-1198, {@code scale} :1534-1536,
 * {@code bind_} :1547-1549, {@code bind} :1544-1546, {@code getFacingCoordsClicked}
 * :1734-1748). Pure static functions over int/byte/float only — no Minecraft classes —
 * so the whole surface runs in the offline truth-table suite (the CruciblePhysics
 * root-layer precedent) while the BE applies it on the live tick.
 */
public final class GTSensorLogic {

	/** Upstream MultiTileEntitySensorTE.java:48. */
	public static final int MODE_COUNT = 8;

	/** Upstream :50-58 — the mode table, index = the byte the "gt.mode" NBT key carries. */
	public static final int
		  MODE_DISPLAY  = 0
		, MODE_PERCENT  = 1
		, MODE_GREATER  = 2
		, MODE_EQUAL    = 3
		, MODE_SMALLER  = 4
		, MODE_SCALE    = 5
		, MODE_FULL     = 6
		, MODE_NOT_FULL = 7
	;

	/** Upstream :60 — the sliding-average window ceiling (a full Short.MAX_VALUE window is legal). */
	public static final int MAX_AVERAGING_VALUES = Short.MAX_VALUE;

	/**
	 * Upstream MultiTileEntitySensor.java:110 — the display-resync threshold: the number
	 * rides the sync channel only when it moved by MORE than 49 since the last sync (or a
	 * keypad/screwdriver arm forced it with the {@code oDisplayedNumber = Short.MIN_VALUE}
	 * write, SensorTE:171-193 — the forced-diff arm is the BE's job, this is the gate).
	 *
	 * <p>DECLARED DEVIATION: 49 pins the upstream {@code SYNC_SECOND ? 0 : 49} FALSE branch
	 * only. Upstream toggles {@code CS.SYNC_SECOND} to true once per second
	 * (CS.java:316 default T; GT_API_Proxy.java:253 {@code SERVER_TIME % 20 == 0}), making
	 * the second-tick sync threshold-0 (every changed value resyncs immediately, visible on
	 * the in-world digital display strip); the port syncs solely on the 49 gate. Unobservable
	 * for now — no digital display-strip render face exists yet, so the per-second cadence
	 * has nothing to show itself on. Revisit at the sensor render pool card.
	 */
	public static final int SYNC_THRESHOLD = 49;

	/** Upstream SensorTE:210 {@code mMode ^= B[7]} — CS.B[7] = -128, the hexadecimal-display flag. */
	public static final int MODE_HEX_FLAG = 0x80;

	/**
	 * The 3x3 keypad rows/cols, upstream PX_P pixel table unfolded: row 0 = the y-pixels
	 * 6..8 band (the ±256 / ±100 row), row 1 = 9..11 (±16 / ±10), row 2 = 12..14 (±1);
	 * col 0 = the x-pixels 9..11 band (minus), col 1 = 12..14 (plus)
	 * (MultiTileEntitySensorTE.java:170-195 — the PX_P[6..14] hit windows).
	 */
	public static final int KEYPAD_ROWS = 3, KEYPAD_COLS = 2;

	private GTSensorLogic() {
	}

	// -------------------------------------------------------------------------
	// the UT.Code numeric layer (verbatim ports)
	// -------------------------------------------------------------------------

	/** Upstream UT.Code.bind16 (UT.java:1515) — the 0..65535 unsigned-short clamp. */
	public static int bind16(long aBoundValue) {
		return (int) Math.max(0, Math.min(65535, aBoundValue));
	}

	/** Upstream UT.Code.unsignS (UT.java:1228-1230). */
	public static int unsignS(short aShort) {
		return aShort < 0 ? aShort + 65536 : aShort;
	}

	/** Upstream UT.Code.bindInt (UT.java:1524). */
	public static int bindInt(long aBoundValue) {
		return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, aBoundValue));
	}

	/** Upstream UT.Code.bind_ (UT.java:1547-1549) — the ordered clamp. */
	public static long bind_(long aMin, long aMax, long aBoundValue) {
		return Math.max(aMin, Math.min(aMax, aBoundValue));
	}

	/** Upstream UT.Code.bind (UT.java:1544-1546) — the either-order clamp. */
	public static long bind(long aMin, long aMax, long aBoundValue) {
		return aMin > aMax ? Math.max(aMax, Math.min(aMin, aBoundValue)) : Math.max(aMin, Math.min(aMax, aBoundValue));
	}

	/**
	 * Upstream UT.Code.scale (UT.java:1534-1536) verbatim — "a Value for a Scale between
	 * 0 and aMax with aScale+1 possible Steps" (the MODE_SCALE / MODE_PERCENT redstone
	 * curve; aInvert is always F on the sensor call sites, carried for parity).
	 */
	public static long scale(long aValue, long aMax, long aScale, boolean aInvert) {
		long rScale = (aValue <= 0 ? 0 : aValue >= aMax ? aScale : aScale <= 2 ? 1 : 1 + (aValue * (aScale - 1)) / aMax);
		return aInvert ? aScale - rScale : rScale;
	}

	/** Upstream UT.Code.averageInts (UT.java:1194-1198) — the plain integer mean, bindInt-clamped. */
	public static int averageInts(int... aInts) {
		if (aInts == null || aInts.length <= 0) return 0;
		long tSum = 0;
		for (int tInt : aInts) tSum += tInt;
		return bindInt(tSum / aInts.length);
	}

	/**
	 * Upstream UT.Code.getFacingCoordsClicked (UT.java:1734-1748) verbatim — the clicked
	 * point in face coordinates with the top-left origin ("like on the Texture Sheet"),
	 * the axis folding the 1.7.10 side order 0..5 == Direction.get3DDataValue() order
	 * carries over (TileEntityBase01Root port doc).
	 */
	public static float[] getFacingCoordsClicked(byte aSide, float aHitX, float aHitY, float aHitZ) {
		switch (aSide) {
		case  0: return new float[] {Math.min(0.99F, Math.max(0,  aHitX)), Math.min(0.99F, Math.max(0, 1 - aHitZ))};
		case  1: return new float[] {Math.min(0.99F, Math.max(0,  aHitX)), Math.min(0.99F, Math.max(0,   aHitZ))};
		case  2: return new float[] {Math.min(0.99F, Math.max(0, 1 - aHitX)), Math.min(0.99F, Math.max(0, 1 - aHitY))};
		case  3: return new float[] {Math.min(0.99F, Math.max(0,  aHitX)), Math.min(0.99F, Math.max(0, 1 - aHitY))};
		case  4: return new float[] {Math.min(0.99F, Math.max(0,  aHitZ)), Math.min(0.99F, Math.max(0, 1 - aHitY))};
		case  5: return new float[] {Math.min(0.99F, Math.max(0, 1 - aHitZ)), Math.min(0.99F, Math.max(0, 1 - aHitY))};
		default: return new float[] {0.5F, 0.5F};
		}
	}

	// -------------------------------------------------------------------------
	// the mode layer
	// -------------------------------------------------------------------------

	/** The display mode of a packed mode byte (upstream {@code mMode & 127}, SensorTE:141). */
	public static int modeOf(int aPackedMode) {
		return aPackedMode & 127;
	}

	/** The hexadecimal-display flag of a packed mode byte (upstream {@code mMode < 0}, SensorTE:169). */
	public static boolean isHexMode(int aPackedMode) {
		return (aPackedMode & MODE_HEX_FLAG) != 0;
	}

	/**
	 * Upstream SensorTE:230-236 verbatim — the screwdriver mode cycle: (mode+1) % 8 with
	 * the hex flag preserved across the wrap.
	 */
	public static int cycleMode(int aPackedMode) {
		int tMode = ((aPackedMode & 127) + 1) % MODE_COUNT;
		return (aPackedMode & ~127) | tMode;
	}

	/** Upstream SensorTE:210 — the display-hit hex toggle. */
	public static int toggleHex(int aPackedMode) {
		return aPackedMode ^ MODE_HEX_FLAG;
	}

	/**
	 * Upstream SensorTE:167 — the keypad (and the averaging-window and the setpoint data
	 * structure) only exist on the four threshold modes; DISPLAY/PERCENT/FULL/NOT_FULL
	 * have no setpoint to type.
	 */
	public static boolean isThresholdMode(int aPackedMode) {
		int tMode = modeOf(aPackedMode);
		return tMode != MODE_DISPLAY && tMode != MODE_PERCENT && tMode != MODE_FULL && tMode != MODE_NOT_FULL;
	}

	// -------------------------------------------------------------------------
	// the sample switch (upstream :124-160, the numeric half)
	// -------------------------------------------------------------------------

	/** One sampled tick's outcome: the display write (when {@code displayedSet}) and the raw redstone (when {@code redstoneSet}). */
	public record Sample(int displayed, boolean displayedSet, long redstone, boolean redstoneSet) {}

	/**
	 * The upstream :141-150 switch verbatim, lifted out of the tick:
	 * <ul>
	 * <li>DISPLAY — {@code mDisplayedNumber = bindInt(mCurrentValue)}; the redstone keeps
	 *     its previous value (the :139 {@code tRedstone = mRedstone} seed);</li>
	 * <li>GREATER / EQUAL / SMALLER — 15 : 0 against {@code mSetNumber};</li>
	 * <li>SCALE — {@code scale(mCurrentValue, mSetNumber, 15, F)};</li>
	 * <li>PERCENT — the displayed number becomes {@code current * 100 / max} (0 when max
	 *     is 0) and the redstone scales THAT over 100;</li>
	 * <li>FULL / NOT_FULL — the {@code >= max} complement pair.</li>
	 * </ul>
	 * The caller bind4-clamps the redstone and applies the unsigned-short display fold
	 * (:152/:159) after the change check.
	 */
	public static Sample sample(int aPackedMode, long aCurrentValue, long aCurrentMax, long aSetNumber) {
		return switch (modeOf(aPackedMode)) {
			case MODE_DISPLAY -> new Sample(bindInt(aCurrentValue), true, 0, false);
			case MODE_GREATER -> new Sample(0, false, aCurrentValue > aSetNumber ? 15 : 0, true);
			case MODE_EQUAL -> new Sample(0, false, aCurrentValue == aSetNumber ? 15 : 0, true);
			case MODE_SMALLER -> new Sample(0, false, aCurrentValue < aSetNumber ? 15 : 0, true);
			case MODE_SCALE -> new Sample(0, false, scale(aCurrentValue, aSetNumber, 15, false), true);
			case MODE_PERCENT -> {
				long tPercent = aCurrentMax > 0 ? bindInt(aCurrentValue * 100L / aCurrentMax) : 0;
				yield new Sample(bindInt(tPercent), true, scale(tPercent, 100L, 15, false), true);
			}
			case MODE_FULL -> new Sample(0, false, aCurrentValue >= aCurrentMax ? 15 : 0, true);
			case MODE_NOT_FULL -> new Sample(0, false, aCurrentValue >= aCurrentMax ? 0 : 15, true);
			default -> new Sample(0, false, 0, false);
		};
	}

	/**
	 * The sliding-average step (upstream :126/:136-137): rotate the ring index, store the
	 * bound sample, and read back the mean — a one-slot window IS the raw value
	 * ({@code mValues.length == 1 ? mValues[0] : averageInts(mValues)}, :137).
	 */
	public static long average(int[] aValues) {
		if (aValues == null || aValues.length == 0) return 0;
		if (aValues.length == 1) return aValues[0];
		return averageInts(aValues);
	}

	/** Upstream Sensor.java:110 — the display-resync gate (the 49-diff threshold). */
	public static boolean shouldSyncDisplayed(int aDisplayed, int aOldDisplayed) {
		return Math.abs(aDisplayed - aOldDisplayed) > SYNC_THRESHOLD;
	}

	// -------------------------------------------------------------------------
	// the keypad arithmetic (upstream :164-200 the setpoint, :217-227 the averaging)
	// -------------------------------------------------------------------------

/**
 * One keypad click on the setpoint (upstream :171-194 verbatim): row 0 pays ±256/±100,
 * row 1 ±16/±10, row 2 ±1; a hex-mode sensor clamps through {@code bind16} (the 0..65535
 * unsigned-short range — a CLAMP, the floor sticks at 0, no wrap), a decimal-mode one
 * clamps to 0..9999 ({@code bind(0, 9999)}).
	 *
	 * @param aRow 0..2 top-to-bottom, {@param aCol} 0 = minus, 1 = plus
	 * @return the new setpoint
	 */
	public static int keypadStep(boolean aHexMode, int aRow, int aCol, int aSetNumber) {
		int tSign = aCol == 0 ? -1 : 1;
		int tStep = switch (aRow) {
			case 0 -> aHexMode ? 256 : 100;
			case 1 -> aHexMode ? 16 : 10;
			default -> 1;
		};
		if (aHexMode) return bind16((long) aSetNumber + (long) tSign * tStep);
		return (int) bind(0, 9999, (long) aSetNumber + (long) tSign * tStep);
	}

	/**
	 * One screwdriver click on the averaging-window buttons (upstream :217-227 verbatim):
	 * the same row/col step table as the keypad, clamped 1..MAX_AVERAGING_VALUES
	 * ({@code bind_}). The average is "disabled" at length 1 (the :225 chat verdict).
	 */
	public static int averagingStep(boolean aHexMode, int aRow, int aCol, int aCurrentLength) {
		int tSign = aCol == 0 ? -1 : 1;
		int tStep = switch (aRow) {
			case 0 -> aHexMode ? 256 : 100;
			case 1 -> aHexMode ? 16 : 10;
			default -> 1;
		};
		return (int) bind_(1, MAX_AVERAGING_VALUES, (long) aCurrentLength + (long) tSign * tStep);
	}

	/**
	 * The keypad hit region test on the front face (upstream the :170-194 PX_P windows,
	 * unfolded): the in-face coordinates land on one of the 3x2 buttons.
	 *
	 * @return the {@code [row, col]} of the button, or null when the click missed every button
	 */
	public static int[] keypadHit(float aCoordX, float aCoordY) {
		int tRow = rowHit(aCoordY);
		int tCol = colHit(aCoordX);
		if (tRow < 0 || tCol < 0) return null;
		return new int[] {tRow, tCol};
	}

	/** The row band test (y-pixels 6-8 / 9-11 / 12-14 of 16). */
	public static int rowHit(float aCoordY) {
		if (aCoordY >= 6 / 16.0F && aCoordY <= 8 / 16.0F) return 0;
		if (aCoordY >= 9 / 16.0F && aCoordY <= 11 / 16.0F) return 1;
		if (aCoordY >= 12 / 16.0F && aCoordY <= 14 / 16.0F) return 2;
		return -1;
	}

	/** The column band test (x-pixels 9-11 / 12-14 of 16). */
	public static int colHit(float aCoordX) {
		if (aCoordX >= 9 / 16.0F && aCoordX <= 11 / 16.0F) return 0;
		if (aCoordX >= 12 / 16.0F && aCoordX <= 14 / 16.0F) return 1;
		return -1;
	}

	/**
	 * Upstream MultiTileEntitySensor.java:230-233 — the display strip hit (the hex-toggle
	 * region): x 2..14, y 2..4 of 16.
	 */
	public static boolean hasHitDisplay(float aCoordX, float aCoordY) {
		return aCoordX >= 2 / 16.0F && aCoordX <= 14 / 16.0F && aCoordY >= 2 / 16.0F && aCoordY <= 4 / 16.0F;
	}
}
