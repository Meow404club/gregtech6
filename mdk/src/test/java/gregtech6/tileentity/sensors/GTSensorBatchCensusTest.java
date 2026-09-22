package gregtech6.tileentity.sensors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.registry.GT6Sensors;
import gregtech6.tileentity.GTOfflineTestBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * The sensor batch census (task p34-sensors-trivial-14, ACCEPTANCE ①) — the ROWS 钉测:
 * 18 live rows (3 pioneers + the 15-row batch), ids AND order pinned to the upstream
 * anchor (Loader_MultiTileEntities.java:1979-1999 read line by line — the CENSUS
 * ERRATUM: the upstream sensors() method registers 21 rows, not the 19 the P26/P34
 * census ledgers carried; the appended subsequence here IS the anchor's row sequence).
 * The three pooled rows (tachometer 31019 / geigercounter 31020 / laserometer 31021)
 * are pinned ABSENT — the 缺缝留池 declaration as an executable assertion. The batch
 * arithmetic pins ride the pure statics and fresh BE instances over a shared fixture
 * type (the {@link GT6ChronometerBlockEntity#minutesOfDay} day-cycle points, the
 * constant max faces, the Gibbl divisors) — no Level needed.
 */
public class GTSensorBatchCensusTest extends GTOfflineTestBase {

	private static final BlockPos PROBE_POS = new BlockPos(1, 2, 3);

	/** One throwaway fixture BET serves every instance assertion (the ctor only stores it). */
	static BlockEntityType<?> sFixtureType;

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

	@Test
	public void pooledRowsStayAbsent() {
		for (String tPooled : new String[] {"tachometer", "geigercounter", "laserometer"}) {
			assertTrue(GT6Sensors.BLOCKS_BY_PATH.keySet().stream().noneMatch(tPooled::equals)
					&& GT6Sensors.ROWS.stream().noneMatch(aRow -> tPooled.equals(aRow.path())),
					"the pooled row '" + tPooled + "' stays unregistered (缺缝留池, never implemented outside a seam)");
		}
		// the block carrier map walks ROWS one-to-one (the static-block form)
		assertEquals(GT6Sensors.ROWS.size(), GT6Sensors.BLOCKS_BY_PATH.size(), "the block carrier map walks ROWS");
	}

	@Test
	public void chronometerMinutesOfDayPinsTheDayCycle() {
		// upstream Chronometer:34 ((time+6000)%24000*60)/1000 — the anchor points
		assertEquals(720, GT6ChronometerBlockEntity.minutesOfDay(0));            // the +6000 noon anchor → 12:00
		assertEquals(0, GT6ChronometerBlockEntity.minutesOfDay(18000));          // midnight
		assertEquals(1410, GT6ChronometerBlockEntity.minutesOfDay(23000));       // 23:30
		assertEquals(600, GT6ChronometerBlockEntity.minutesOfDay(6000));         // sunset → 18:00
		assertEquals(720, GT6ChronometerBlockEntity.minutesOfDay(24000 + 0));    // the day rollover wraps
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
