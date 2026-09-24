package gregtech6.tileentity.sensors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.registry.GT6Kinetics;
import gregtech6.registry.GT6Sensors;
import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.tileentity.connectors.GTWireBlockEntity;
import gregtech6.tileentity.energy.GTAxleBlockEntity;
import gregtech6.tileentity.energy.GTGearBoxBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The sensor batch census (task p34-sensors-trivial-14 ACCEPTANCE ①, pool closure task
 * p37-sensors-3) — the ROWS 钉测: 21 live rows (3 pioneers + the 15-row batch + the
 * 3-row pool closure), ids AND order pinned to the upstream anchor
 * (Loader_MultiTileEntities.java:1979-1999 read line by line — the CENSUS ERRATUM: the
 * upstream sensors() method registers 21 rows, not the 19 the P26/P34 census ledgers
 * carried; the appended subsequences ARE the anchor's row sequence). The three former
 * pooled rows (tachometer 31019 / geigercounter 31020 / laserometer 31021) joined LIVE
 * (task p37-sensors-3): the p34 缺缝 notes went stale — P28 built the kinetics carriers
 * and P32 revived the LU carrier; the geiger's reactor arm stays declared (the s2
 * true-gap pool) with its non-reactor 0 read pinned as the upstream-faithful behaviour.
 * The batch arithmetic pins ride the pure statics and fresh BE instances over a shared
 * fixture type (the {@link GT6ChronometerBlockEntity#minutesOfDay} day-cycle points, the
 * constant max faces, the Gibbl divisors) — no Level needed.
 */
public class GTSensorBatchCensusTest extends GTOfflineTestBase {

	private static final BlockPos PROBE_POS = new BlockPos(1, 2, 3);

	/** One throwaway fixture BET serves every instance assertion (the ctor only stores it). */
	static BlockEntityType<?> sFixtureType;

	/**
	 * The laser-carrier fixture: the sensor's read face is the {@code isLaser()} gate
	 * (the port's stand-in for the upstream {@code instanceof MultiTileEntityWireLaser}
	 * :42/:48), so the offline pin overrides that gate directly — constructing real
	 * {@code GTWireBlock}s is impossible past the offline boot (the frozen-registry
	 * intrusive-holder write, the GTOfflineTestBase javadoc rule); the family latch
	 * behind the gate is the wire family's own pin (GTWireContactDamageTest).
	 */
	static final class LaserWireFixture extends GTWireBlockEntity {
		LaserWireFixture(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
			super(aType, aPos, aState);
		}

		@Override
		public boolean isLaser() {
			return true;
		}
	}

	@BeforeAll
	static void buildFixture() {
		// the boot + BET-unfreeze rode the superclass @BeforeAll
		sFixtureType = BlockEntityType.Builder.of((aPos, aState) -> null, Blocks.BRICKS).build(null);
	}

	/** The upstream anchor sequence: (path, legacyId) in Loader_MultiTileEntities.java order. */
	private static final String[][] ANCHOR = {
			// the pioneers kept their P26 head-of-list order (:1995 → :1986 → :1997)
			{"progressmeter", "31018"},          // :1995
			{"fluidometer", "31006"},            // :1986
			{"electrometer", "31015"},           // :1997
			// the p34 batch = the anchor's remaining rows in :1979 → :1994 sequence
			{"thermometer", "31000"},            // :1979
			{"luminometer", "31002"},            // :1980
			{"chronometer", "31003"},            // :1981
			{"gibblometer", "31001"},            // :1982
			{"kilogibblometer", "31023"},        // :1983
			{"itemometer", "31004"},             // :1984
			{"stackometer", "31005"},            // :1985
			{"bucketometer", "31007"},           // :1987
			{"kilobucketometer", "31022"},       // :1988
			{"lightweightometer", "31010"},      // :1989
			{"mediumweightometer", "31011"},     // :1990
			{"heavyweightometer", "31012"},      // :1991
			{"superheavyweightometer", "31013"}, // :1992
			{"tpsmeter", "31016"},               // :1993
			{"playercounter", "31017"},          // :1994
			// the p37 pool closure = the anchor's remaining rows in :1996 → :1998 → :1999 order
			{"geigercounter", "31020"},          // :1996
			{"tachometer", "31019"},             // :1998
			{"laserometer", "31021"},            // :1999
	};

	@Test
	public void rowsCensusPinsTheUpstreamAnchor() {
		assertEquals(ANCHOR.length, GT6Sensors.ROWS.size(), "the live row count (the census 钉测)");
		for (int i = 0; i < ANCHOR.length; i++) {
			GT6Sensors.SensorRow tRow = GT6Sensors.ROWS.get(i);
			assertEquals(ANCHOR[i][0], tRow.path(), "row " + i + " path (order pinned)");
			assertEquals(Long.parseLong(ANCHOR[i][1]), tRow.legacyId(), "row " + i + " id (order pinned)");
			assertEquals("block.gt6." + ANCHOR[i][0], tRow.displayKey(), "row " + i + " display key");
		}
	}

	/**
	 * The pool closure (task p37-sensors-3): the three former pooled rows joined the walk —
	 * registered, order-pinned by {@link #rowsCensusPinsTheUpstreamAnchor}, and the block
	 * carrier map walks ROWS one-to-one (the static-block form).
	 */
	@Test
	public void poolRowsJoinedLive() {
		for (String tJoined : new String[] {"tachometer", "geigercounter", "laserometer"}) {
			assertTrue(GT6Sensors.BLOCKS_BY_PATH.containsKey(tJoined),
					"the former pooled row '" + tJoined + "' is registered (the 21/21 closure)");
		}
		assertEquals(GT6Sensors.ROWS.size(), GT6Sensors.BLOCKS_BY_PATH.size(), "the block carrier map walks ROWS");
	}

	/**
	 * The Tachometer read faces (upstream MultiTileEntityTachometer.java:42-53): the axle
	 * answers its {@code mTransferredLast} against the {@code mPower * mSpeed} rating, the
	 * gearbox its {@code mTransferredLast} against {@code mMaxThroughPut * 16}, everything
	 * else 0 — over the live P28 carriers.
	 */
	@Test
	public void tachometerReadsTheRuCarriers() {
		GT6TachometerBlockEntity tMeter = new GT6TachometerBlockEntity(sFixtureType, PROBE_POS, Blocks.BRICKS.defaultBlockState());
		GTAxleBlockEntity tAxle = new GTAxleBlockEntity(sFixtureType, PROBE_POS, Blocks.BRICKS.defaultBlockState()); // the fixture state keeps the :56 defaults mSpeed=32/mPower=1
		tAxle.mTransferredLast = 5000;
		assertEquals(5000, tMeter.getCurrentValue(tAxle));  // upstream :43
		assertEquals(32, tMeter.getCurrentMax(tAxle));      // upstream :50 — 1 * 32, the fixture defaults
		tAxle.mPower = 8; tAxle.mSpeed = 64;
		assertEquals(512, tMeter.getCurrentMax(tAxle));     // upstream :50 — the product rating
		GTGearBoxBlockEntity tBox = new GTGearBoxBlockEntity(sFixtureType, PROBE_POS, Blocks.BRICKS.defaultBlockState());
		tBox.mTransferredLast = 250;
		assertEquals(250, tMeter.getCurrentValue(tBox));    // upstream :44
		assertEquals(GT6Kinetics.GEARBOX_MAX_THROUGHPUT * 16, tMeter.getCurrentMax(tBox)); // upstream :51
		assertEquals(0, tMeter.getCurrentValue(null));      // upstream :45
		assertEquals(0, tMeter.getCurrentMax(null));        // upstream :52
	}

	/**
	 * The Geiger Counter non-reactor arm (upstream MultiTileEntityGeigerCounter.java
	 * :45-66): every neighbour that is not the reactor core reads 0/0 (:49/:65 verbatim)
	 * — with the reactor system unported (the s2 true-gap pool) that is EVERY neighbour,
	 * the declared 100%-faithful posture; the reactor arm cites this test as the seam.
	 */
	@Test
	public void geigerCounterAnswersZeroOffTheReactorSeam() {
		GT6GeigerCounterBlockEntity tMeter = new GT6GeigerCounterBlockEntity(sFixtureType, PROBE_POS, Blocks.BRICKS.defaultBlockState());
		assertEquals(0, tMeter.getCurrentValue(null));
		assertEquals(0, tMeter.getCurrentMax(null));
		assertEquals(0, tMeter.getCurrentValue(new GTAxleBlockEntity(sFixtureType, PROBE_POS, Blocks.BRICKS.defaultBlockState())));
		assertEquals(0, tMeter.getCurrentMax(new GTAxleBlockEntity(sFixtureType, PROBE_POS, Blocks.BRICKS.defaultBlockState())));
	}

	/**
	 * The Laser-O-Meter read faces (upstream MultiTileEntityLaserometer.java:40-50): the
	 * laser-family wire answers its {@code mTransferredLast} against the constant 65535
	 * ceiling (:48), a non-laser wire and any other neighbour 0 — over the live P32 LU
	 * carrier.
	 */
	@Test
	public void laserometerReadsTheLuCarrier() {
		GT6LaserometerBlockEntity tMeter = new GT6LaserometerBlockEntity(sFixtureType, PROBE_POS, Blocks.BRICKS.defaultBlockState());
		LaserWireFixture tLaser = new LaserWireFixture(sFixtureType, PROBE_POS, Blocks.BRICKS.defaultBlockState());
		tLaser.mTransferredLast = 1234;
		assertEquals(1234, tMeter.getCurrentValue(tLaser)); // upstream :42
		assertEquals(65535, tMeter.getCurrentMax(tLaser));  // upstream :48 verbatim
		// the non-laser wire: a bare GTWireBlockEntity over the fixture state — mLaserFamily false
		GTWireBlockEntity tElectric = new GTWireBlockEntity(sFixtureType, PROBE_POS, Blocks.BRICKS.defaultBlockState());
		assertEquals(0, tMeter.getCurrentValue(tElectric)); // upstream :43 — the non-laser wire family
		assertEquals(0, tMeter.getCurrentMax(tElectric));   // upstream :49
		assertEquals(0, tMeter.getCurrentValue(null));
		assertEquals(0, tMeter.getCurrentMax(null));
	}

	@Test
	public void chronometerMinutesOfDayPinsTheDayCycle() {
		// upstream Chronometer:34 ((time+6000)%24000*60)/1000 — the anchor points.
		// The +6000 offset puts tick 0 at minute 360 (the upstream meter reads 360 on a
		// fresh world — minute 0 = tick 18000, the midnight fold).
		assertEquals(360, GT6ChronometerBlockEntity.minutesOfDay(0));         // the +6000 offset → 06:00 slot
		assertEquals(0, GT6ChronometerBlockEntity.minutesOfDay(18000));       // the midnight fold
		assertEquals(300, GT6ChronometerBlockEntity.minutesOfDay(23000));     // 5000 ticks past the fold → 05:00 slot
		assertEquals(720, GT6ChronometerBlockEntity.minutesOfDay(6000));      // sunset → 18:00 slot
		assertEquals(360, GT6ChronometerBlockEntity.minutesOfDay(24000 + 0)); // the day rollover wraps
	}

	@Test
	public void declaredBaselineReadsPinTheirScales() {
		// the declared-zero families keep their upstream scale constants
		assertEquals(1000, GT6GibblometerBlockEntity.SCALE);        // upstream Gibblometer:33
		assertEquals(1000000, GT6KiloGibblometerBlockEntity.SCALE); // upstream KiloGibblometer:32
		assertEquals(65535, GT6LightWeightometerBlockEntity.MAX_WEIGHT_READING); // upstream WeightometerLight:65 B[16]-1
		// the constant-max faces through fresh instances (the fixture type; no target)
		assertEquals(65535, new GT6LightWeightometerBlockEntity(sFixtureType, PROBE_POS, Blocks.BRICKS.defaultBlockState()).getCurrentMax(null));
		assertEquals(65535, new GT6MediumWeightometerBlockEntity(sFixtureType, PROBE_POS, Blocks.BRICKS.defaultBlockState()).getCurrentMax(null));
		assertEquals(65535, new GT6HeavyWeightometerBlockEntity(sFixtureType, PROBE_POS, Blocks.BRICKS.defaultBlockState()).getCurrentMax(null));
		assertEquals(65535, new GT6SuperHeavyWeightometerBlockEntity(sFixtureType, PROBE_POS, Blocks.BRICKS.defaultBlockState()).getCurrentMax(null));
		assertEquals(2000, new GT6TpsmeterBlockEntity(sFixtureType, PROBE_POS, Blocks.BRICKS.defaultBlockState()).getCurrentMax(null)); // upstream TPSmeter:50
		assertEquals(15, new GT6LuminometerBlockEntity(sFixtureType, PROBE_POS, Blocks.BRICKS.defaultBlockState()).getCurrentMax(null)); // upstream Luminometer:40
		assertEquals(1440, new GT6ChronometerBlockEntity(sFixtureType, PROBE_POS, Blocks.BRICKS.defaultBlockState()).getCurrentMax(null)); // upstream Chronometer:37
		// the declared-zero bodies answer 0 with no target and no level (the Electrometer non-wire form)
		assertEquals(0, new GT6GibblometerBlockEntity(sFixtureType, PROBE_POS, Blocks.BRICKS.defaultBlockState()).getCurrentValue(null));
		assertEquals(0, new GT6KiloGibblometerBlockEntity(sFixtureType, PROBE_POS, Blocks.BRICKS.defaultBlockState()).getCurrentValue(null));
		assertEquals(0, new GT6LightWeightometerBlockEntity(sFixtureType, PROBE_POS, Blocks.BRICKS.defaultBlockState()).getCurrentValue(null));
		assertEquals(0, new GT6BucketometerBlockEntity(sFixtureType, PROBE_POS, Blocks.BRICKS.defaultBlockState()).getCurrentValue(null)); // no level → the source arm is dead
		assertEquals(0, new GT6KiloBucketometerBlockEntity(sFixtureType, PROBE_POS, Blocks.BRICKS.defaultBlockState()).getCurrentValue(null));
		assertEquals(0, new GT6ItemometerBlockEntity(sFixtureType, PROBE_POS, Blocks.BRICKS.defaultBlockState()).getCurrentValue(null));
		assertEquals(0, new GT6StackometerBlockEntity(sFixtureType, PROBE_POS, Blocks.BRICKS.defaultBlockState()).getCurrentValue(null));
		assertEquals(0, new GT6PlayerCounterBlockEntity(sFixtureType, PROBE_POS, Blocks.BRICKS.defaultBlockState()).getCurrentValue(null));
		// the TPS fold itself: upstream :43 — (20 * 100000) / 1000ms = 2000 (the 20 TPS reference)
		GT6TpsmeterBlockEntity tTps = new GT6TpsmeterBlockEntity(sFixtureType, PROBE_POS, Blocks.BRICKS.defaultBlockState());
		tTps.mTime = System.currentTimeMillis() - 1000;
		tTps.sampleTick();
		assertEquals(2000, tTps.mCurrentTime, "a perfect 1000 ms window folds to the 20 TPS reference");
	}

	@Test
	public void rowPathsStayOneWord() {
		// the registry path IS the lang key tail and the RCON place id — no dots, no caps
		for (GT6Sensors.SensorRow tRow : GT6Sensors.ROWS) {
			assertEquals(tRow.path(), tRow.path().toLowerCase(java.util.Locale.ROOT));
			assertFalse(tRow.path().contains("."), "path stays dot-free");
		}
		List<String> tPaths = GT6Sensors.ROWS.stream().map(GT6Sensors.SensorRow::path).toList();
		assertEquals(tPaths.stream().distinct().count(), tPaths.size(), "no duplicate paths");
	}
}
