package gregtech6.tileentity.sensors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The sensor pure-logic truth tables (task p26-sensors-core offline acceptance — "8 模式
 * 真值表单测+滑动平均数学+键区 mSetNumber 边界+NBT 往返；零 MenuType 断言"). Every case is
 * the upstream branch table replayed against the verbatim ports:
 *
 * <ul>
 * <li>the 8-mode redstone switch (MultiTileEntitySensorTE.java:141-150);</li>
 * <li>the sliding-average math (:126-137, {@code UT.Code.averageInts});</li>
 * <li>the keypad setpoint steps and clamps (:171-194 — the bind16 0..65535 clamp vs the
 *     0..9999 clamp; the floor sticks, no wrap);</li>
 * <li>the averaging-window resize clamps (:217-227, 1..MAX_AVERAGING_VALUES);</li>
 * <li>the display resync gate (MultiTileEntitySensor.java:108-111, |diff| &gt; 49);</li>
 * <li>the screwdriver packing (SensorTE:210/:230-236 — the hex flag across mode cycles).</li>
 * </ul>
 *
 * <p>Zero MenuType surface: the sensor family is NO_GUI (upstream Sensor:91), so this
 * suite never touches a menu class — the structural no-GUI pin is the class set itself
 * (no MenuType row is appended anywhere in this card).
 */
public class GTSensorLogicTest {

	// --------------------------------------------------------------- the mode table

	@Test
	public void modeTableMatchesUpstream() {
		assertEquals(8, GTSensorLogic.MODE_COUNT);
		assertEquals(0, GTSensorLogic.MODE_DISPLAY);
		assertEquals(1, GTSensorLogic.MODE_PERCENT);
		assertEquals(2, GTSensorLogic.MODE_GREATER);
		assertEquals(3, GTSensorLogic.MODE_EQUAL);
		assertEquals(4, GTSensorLogic.MODE_SMALLER);
		assertEquals(5, GTSensorLogic.MODE_SCALE);
		assertEquals(6, GTSensorLogic.MODE_FULL);
		assertEquals(7, GTSensorLogic.MODE_NOT_FULL);
		assertEquals(Short.MAX_VALUE, GTSensorLogic.MAX_AVERAGING_VALUES);
	}

	// --------------------------------------------------------------- the 8-mode redstone truth table

	@Test
	public void displayModeWritesDisplayedKeepsRedstone() {
		GTSensorLogic.Sample tSample = GTSensorLogic.sample(GTSensorLogic.MODE_DISPLAY, 1234, 5000, 7);
		assertTrue(tSample.displayedSet());
		assertEquals(1234, tSample.displayed());
		assertFalse(tSample.redstoneSet()); // the :139 seed keeps the previous output
	}

	@Test
	public void greaterModeTruthTable() {
		assertTrue(GTSensorLogic.sample(GTSensorLogic.MODE_GREATER, 100, 0, 99).redstone() == 15);
		assertTrue(GTSensorLogic.sample(GTSensorLogic.MODE_GREATER, 100, 0, 100).redstone() == 0); // strictly greater
		assertTrue(GTSensorLogic.sample(GTSensorLogic.MODE_GREATER, 50, 0, 99).redstone() == 0);
	}

	@Test
	public void equalModeTruthTable() {
		assertTrue(GTSensorLogic.sample(GTSensorLogic.MODE_EQUAL, 100, 0, 100).redstone() == 15);
		assertTrue(GTSensorLogic.sample(GTSensorLogic.MODE_EQUAL, 99, 0, 100).redstone() == 0);
		assertTrue(GTSensorLogic.sample(GTSensorLogic.MODE_EQUAL, 101, 0, 100).redstone() == 0);
	}

	@Test
	public void smallerModeTruthTable() {
		assertTrue(GTSensorLogic.sample(GTSensorLogic.MODE_SMALLER, 50, 0, 99).redstone() == 15);
		assertTrue(GTSensorLogic.sample(GTSensorLogic.MODE_SMALLER, 100, 0, 100).redstone() == 0); // strictly smaller
		assertTrue(GTSensorLogic.sample(GTSensorLogic.MODE_SMALLER, 150, 0, 99).redstone() == 0);
	}

	@Test
	public void scaleModeIsTheBarometerCurve() {
		// UT.Code.scale(:1534-1536): 0 → 0, max → 15, 1500/5000 → 1 + 1500*14/5000 = 5
		assertEquals(0, GTSensorLogic.sample(GTSensorLogic.MODE_SCALE, 0, 0, 5000).redstone());
		assertEquals(15, GTSensorLogic.sample(GTSensorLogic.MODE_SCALE, 5000, 0, 5000).redstone());
		assertEquals(1 + 1500 * 14 / 5000, GTSensorLogic.sample(GTSensorLogic.MODE_SCALE, 1500, 0, 5000).redstone());
		// above the setpoint saturates at the top step (:1535 aValue >= aMax)
		assertEquals(15, GTSensorLogic.sample(GTSensorLogic.MODE_SCALE, 9000, 0, 5000).redstone());
	}

	@Test
	public void percentModeWritesDisplayedAndScalesIt() {
		GTSensorLogic.Sample tHalf = GTSensorLogic.sample(GTSensorLogic.MODE_PERCENT, 500, 1000, 0);
		assertTrue(tHalf.displayedSet());
		assertEquals(50, tHalf.displayed());
		assertTrue(tHalf.redstoneSet());
		// scale(50, 100, 15) = 1 + 50*14/100 = 8
		assertEquals(1 + 50 * 14 / 100, tHalf.redstone());
		// max == 0 → the displayed percent is 0 and the scale answers 0 (:147 the mCurrentMax > 0 gate)
		GTSensorLogic.Sample tZero = GTSensorLogic.sample(GTSensorLogic.MODE_PERCENT, 500, 0, 0);
		assertEquals(0, tZero.displayed());
		assertEquals(0, tZero.redstone());
		// 100% saturates
		GTSensorLogic.Sample tFull = GTSensorLogic.sample(GTSensorLogic.MODE_PERCENT, 1000, 1000, 0);
		assertEquals(100, tFull.displayed());
		assertEquals(15, tFull.redstone());
	}

	@Test
	public void fullAndNotFullAreTheComplementPair() {
		assertTrue(GTSensorLogic.sample(GTSensorLogic.MODE_FULL, 1000, 1000, 0).redstone() == 15); // >= max
		assertTrue(GTSensorLogic.sample(GTSensorLogic.MODE_FULL, 1200, 1000, 0).redstone() == 15);
		assertTrue(GTSensorLogic.sample(GTSensorLogic.MODE_FULL, 999, 1000, 0).redstone() == 0);
		assertTrue(GTSensorLogic.sample(GTSensorLogic.MODE_NOT_FULL, 1000, 1000, 0).redstone() == 0);
		assertTrue(GTSensorLogic.sample(GTSensorLogic.MODE_NOT_FULL, 999, 1000, 0).redstone() == 15);
	}

	// --------------------------------------------------------------- the sliding average

	@Test
	public void singleSlotWindowIsTheRawValue() {
		assertEquals(1234, GTSensorLogic.average(new int[] {1234})); // upstream :137 the length==1 shortcut
		assertEquals(0, GTSensorLogic.average(new int[0]));
		assertEquals(0, GTSensorLogic.average(null));
	}

	@Test
	public void multiSlotWindowIsTheIntegerMean() {
		assertEquals(2, GTSensorLogic.average(new int[] {1, 2, 3})); // 6/3
		assertEquals(2, GTSensorLogic.average(new int[] {1, 2, 2, 3})); // 8/4
		// the bindInt clamp of averageInts (UT.java:1198) — plain means never hit it
		assertEquals(-1, GTSensorLogic.average(new int[] {-2, 0}));
	}

	@Test
	public void windowRotationStoresAndAverages() {
		// the :126/:136-137 ring walk replayed: a 4-slot window fed 10,20,30,40 then 50
		int[] tValues = new int[4];
		int tIndex = 0;
		long tCurrent = 0;
		int[] tFeed = {10, 20, 30, 40, 50};
		for (int tSample : tFeed) {
			tIndex = (tIndex + 1) % tValues.length;
			tValues[tIndex] = tSample;
			tCurrent = GTSensorLogic.average(tValues);
		}
		// after the 5th feed the window holds {50,20,30,40} → mean 35
		assertEquals(35, tCurrent);
	}

	// --------------------------------------------------------------- the keypad

	@Test
	public void decimalKeypadClampsTo9999And0() {
		// the decimal branch (:184-193): bind(0, 9999)
		assertEquals(9999, GTSensorLogic.keypadStep(false, 0, 1, 9999)); // +100 clamped
		assertEquals(9950, GTSensorLogic.keypadStep(false, 0, 1, 9850)); // +100 below the ceiling — no clamp (:185)
		assertEquals(9999, GTSensorLogic.keypadStep(false, 2, 1, 9999)); // +1 clamped
		assertEquals(0, GTSensorLogic.keypadStep(false, 0, 0, 50)); // -100 clamped to 0
		assertEquals(0, GTSensorLogic.keypadStep(false, 2, 0, 0)); // -1 at the floor
	}

	@Test
	public void hexKeypadWrapsThroughTheUnsignedShortRange() {
		// the hex branch (:171-180): bind16 — the wrap through 65535
		assertEquals(65535, GTSensorLogic.keypadStep(true, 0, 1, 65400)); // +256 clamped at the top
		assertEquals(256, GTSensorLogic.keypadStep(true, 0, 1, 0));
		assertEquals(0, GTSensorLogic.keypadStep(true, 0, 0, 100)); // -256 clamped at the floor
		assertEquals(16, GTSensorLogic.keypadStep(true, 1, 1, 0));
		// the floor is a CLAMP, not a wrap — upstream :179 mSetNumber-- then bind16: 0-1 → bind16(-1) → 0
		assertEquals(0, GTSensorLogic.keypadStep(true, 2, 0, 0));
	}

	@Test
	public void keypadStepTableIsTheUpstreamPixelRows() {
		// rows 0/1/2 = ±256/±16/±1 (hex) and ±100/±10/±1 (decimal); col 0 minus, col 1 plus
		assertEquals(1256, GTSensorLogic.keypadStep(true, 0, 1, 1000));
		assertEquals(1016, GTSensorLogic.keypadStep(true, 1, 1, 1000));
		assertEquals(1001, GTSensorLogic.keypadStep(true, 2, 1, 1000));
		assertEquals(1100, GTSensorLogic.keypadStep(false, 0, 1, 1000));
		assertEquals(1010, GTSensorLogic.keypadStep(false, 1, 1, 1000));
		assertEquals(1001, GTSensorLogic.keypadStep(false, 2, 1, 1000));
		assertEquals(744, GTSensorLogic.keypadStep(true, 0, 0, 1000));
		assertEquals(900, GTSensorLogic.keypadStep(false, 0, 0, 1000));
	}

	// --------------------------------------------------------------- the averaging window

	@Test
	public void averagingWindowClampsToOneAndMax() {
		assertEquals(1, GTSensorLogic.averagingStep(true, 2, 0, 1)); // -1 at the floor = the disabled state
		assertEquals(1, GTSensorLogic.averagingStep(false, 0, 0, 50)); // -100 clamped to 1
		assertEquals(GTSensorLogic.MAX_AVERAGING_VALUES, GTSensorLogic.averagingStep(true, 0, 1, Short.MAX_VALUE)); // +256 clamped
		assertEquals(GTSensorLogic.MAX_AVERAGING_VALUES, GTSensorLogic.averagingStep(false, 0, 1, GTSensorLogic.MAX_AVERAGING_VALUES - 50));
		// the hex/decimal step table (SensorTE:217-222) — row 1 decimal pays 10 (:221)
		assertEquals(1256, GTSensorLogic.averagingStep(true, 0, 1, 1000));
		assertEquals(990, GTSensorLogic.averagingStep(false, 1, 0, 1000));
	}

	// --------------------------------------------------------------- the resync gate

	@Test
	public void displayResyncGateIsTheDiff49() {
		assertFalse(GTSensorLogic.shouldSyncDisplayed(100, 100)); // no change → the stored value rides the open-tick sync only
		assertFalse(GTSensorLogic.shouldSyncDisplayed(149, 100)); // diff 49 == the threshold, NOT greater
		assertTrue(GTSensorLogic.shouldSyncDisplayed(150, 100)); // diff 50 > 49
		assertTrue(GTSensorLogic.shouldSyncDisplayed(50, 100)); // negative diff of the same magnitude
		// the keypad forced-sync arm (:171 oDisplayedNumber = Short.MIN_VALUE) wins through any value
		assertTrue(GTSensorLogic.shouldSyncDisplayed(0, Short.MIN_VALUE));
	}

	// --------------------------------------------------------------- the screwdriver packing

	@Test
	public void modeCycleWalksTheRingAndKeepsTheHexFlag() {
		int tMode = 0;
		for (int i = 1; i < 8; i++) {
			tMode = GTSensorLogic.cycleMode(tMode);
			assertEquals(i, tMode);
		}
		// the wrap (:232 (mode+1) % 8)
		tMode = GTSensorLogic.cycleMode(tMode);
		assertEquals(0, tMode);
		// the hex flag rides across the wrap (:230-236)
		int tHex = GTSensorLogic.cycleMode(GTSensorLogic.MODE_HEX_FLAG | 7);
		assertEquals(GTSensorLogic.MODE_HEX_FLAG, tHex & GTSensorLogic.MODE_HEX_FLAG);
		assertEquals(0, tHex & 127);
	}

	@Test
	public void hexToggleIsTheBitFlip() {
		assertEquals(GTSensorLogic.MODE_HEX_FLAG, GTSensorLogic.toggleHex(0));
		assertEquals(0, GTSensorLogic.toggleHex(GTSensorLogic.MODE_HEX_FLAG));
		assertEquals(GTSensorLogic.MODE_HEX_FLAG | 5, GTSensorLogic.toggleHex(5));
	}

	@Test
	public void thresholdModesAreTheFourSetpointModes() {
		assertFalse(GTSensorLogic.isThresholdMode(GTSensorLogic.MODE_DISPLAY)); // :167 the four refusals
		assertFalse(GTSensorLogic.isThresholdMode(GTSensorLogic.MODE_PERCENT));
		assertFalse(GTSensorLogic.isThresholdMode(GTSensorLogic.MODE_FULL));
		assertFalse(GTSensorLogic.isThresholdMode(GTSensorLogic.MODE_NOT_FULL));
		assertTrue(GTSensorLogic.isThresholdMode(GTSensorLogic.MODE_GREATER));
		assertTrue(GTSensorLogic.isThresholdMode(GTSensorLogic.MODE_EQUAL));
		assertTrue(GTSensorLogic.isThresholdMode(GTSensorLogic.MODE_SMALLER));
		assertTrue(GTSensorLogic.isThresholdMode(GTSensorLogic.MODE_SCALE));
		assertTrue(GTSensorLogic.isThresholdMode(GTSensorLogic.MODE_HEX_FLAG | GTSensorLogic.MODE_GREATER)); // the sign bit never leaks into the index
	}

	// --------------------------------------------------------------- the hit regions

	@Test
	public void keypadHitRegionsAreThePixelWindows() {
		// row bands y 6-8 / 9-11 / 12-14 of 16; col bands x 9-11 / 12-14
		assertArrayEquals(new int[] {0, 0}, GTSensorLogic.keypadHit(9 / 16.0F, 6 / 16.0F));
		assertArrayEquals(new int[] {2, 1}, GTSensorLogic.keypadHit(14 / 16.0F, 14 / 16.0F));
		assertArrayEquals(new int[] {1, 1}, GTSensorLogic.keypadHit(0.8F, 0.625F));
		assertNull(GTSensorLogic.keypadHit(0.5F, 0.5F)); // the centre misses every button
		assertNull(GTSensorLogic.keypadHit(0.0F, 0.0F));
		assertNotNull(GTSensorLogic.keypadHit(0.6F, 0.8125F));
	}

	@Test
	public void displayStripIsTheHexToggleRegion() {
		assertTrue(GTSensorLogic.hasHitDisplay(0.5F, 2 / 16.0F)); // y-pixel 2..4, x 2..14
		assertTrue(GTSensorLogic.hasHitDisplay(13 / 16.0F, 4 / 16.0F));
		assertFalse(GTSensorLogic.hasHitDisplay(0.5F, 5 / 16.0F)); // below the strip
		assertFalse(GTSensorLogic.hasHitDisplay(1 / 16.0F, 3 / 16.0F)); // left of the strip
	}

	// --------------------------------------------------------------- the UT.Code ports

	@Test
	public void bind16UnsignSBindIntMatchUpstream() {
		assertEquals(0, GTSensorLogic.bind16(-1)); // the 0 floor
		assertEquals(65535, GTSensorLogic.bind16(100000)); // the 65535 ceiling
		assertEquals(1234, GTSensorLogic.bind16(1234));
		assertEquals(0, GTSensorLogic.unsignS((short) 0));
		assertEquals(65535, GTSensorLogic.unsignS((short) -1)); // the unsigned fold (:159)
		assertEquals(32768, GTSensorLogic.unsignS((short) -32768));
		assertEquals(Integer.MAX_VALUE, GTSensorLogic.bindInt(Long.MAX_VALUE));
		assertEquals(Integer.MIN_VALUE, GTSensorLogic.bindInt(Long.MIN_VALUE));
	}

	@Test
	public void faceCoordsFoldPerSide() {
		// UT.Code.getFacingCoordsClicked (:1734-1748) — spot the origin flips
		float[] tBottom = GTSensorLogic.getFacingCoordsClicked((byte) 0, 0.25F, 0.5F, 0.75F);
		assertEquals(0.25F, tBottom[0], 1e-6);
		assertEquals(0.25F, tBottom[1], 1e-6); // 1 - 0.75
		float[] tTop = GTSensorLogic.getFacingCoordsClicked((byte) 1, 0.25F, 0.5F, 0.75F);
		assertEquals(0.75F, tTop[1], 1e-6); // no flip on top
		float[] tNorth = GTSensorLogic.getFacingCoordsClicked((byte) 2, 0.25F, 0.75F, 0.5F);
		assertEquals(0.75F, tNorth[0], 1e-6); // 1 - 0.25
		assertEquals(0.25F, tNorth[1], 1e-6); // 1 - 0.75
		float[] tInvalid = GTSensorLogic.getFacingCoordsClicked((byte) 6, 0, 0, 0);
		assertEquals(0.5F, tInvalid[0], 1e-6); // the default centre
	}

	private static void assertArrayEquals(int[] aExpected, int[] aActual) {
		org.junit.jupiter.api.Assertions.assertArrayEquals(aExpected, aActual);
	}
}
