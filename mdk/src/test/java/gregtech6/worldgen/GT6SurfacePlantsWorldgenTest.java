/*
 * Tests for task p30-w6-t2-surface-blocks: the surface-plants + soil worldgen band —
 * the constants transcription and the key-path set (the GT6SurfaceBlocksTest posture).
 *
 * <p>Compile anchors: Loader_Worldgen.java:603-606 (the fallen-log gates), :632-633 (the
 * plant amounts/probabilities), :581-582/:592 (the black sand/turf/clay pit rows),
 * WorldgenPit.java:58 (the 1/320 gate), WorldgenBlackSand.java:48 / WorldgenTurf.java:49
 * (the 1/64 and 1/32 gates), DiskConfiguration.java:15-16 (the radius/half-height caps).
 *
 * <p>Offline-safe by construction: ResourceKey.create is a map intern, the enum walks
 * touch no registry.
 */
package gregtech6.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class GT6SurfacePlantsWorldgenTest {

    /** The WorldgenOnSurface ray gates, transcribed (Loader_Worldgen.java:632-633). */
    @Test
    void plantGatesArePinned() {
        assertEquals(16, GT6Worldgen.GLOWTUS_AMOUNT, "plant.glowtus amount=16");
        assertEquals(2, GT6Worldgen.GLOWTUS_PROBABILITY, "plant.glowtus probability=2");
        assertEquals(1, GT6Worldgen.BUSH_AMOUNT, "plant.bush amount=1");
        assertEquals(4, GT6Worldgen.BUSH_PROBABILITY, "plant.bush probability=4");
    }

    /** The chunk gates + the fallen-log rows (Loader_Worldgen.java:592/:603-606 + WorldgenPit/BlackSand/Turf). */
    @Test
    void soilAndLogGatesArePinned() {
        assertEquals(320, GT6Worldgen.PIT_CLAY_DIVIDER,
                "WorldgenPit.java:58 nextInt(320) > bindInt(1-1)=0 — the 1/320 chunk gate");
        assertEquals(64, GT6Worldgen.BLACKSAND_DIVIDER, "WorldgenBlackSand.java:48 the 1/64 chunk gate");
        assertEquals(32, GT6Worldgen.TURF_DIVIDER, "WorldgenTurf.java:49 the 1/32 chunk gate");
        assertEquals(List.of(8, 3, 8, 8), GT6Worldgen.FALLEN_LOG_PROBABILITY,
                "log.dry/rotten/mossy/frozen probabilities, Loader_Worldgen.java:603-606");
        assertEquals(List.of("log_dry", "log_rotten", "log_mossy", "log_frozen"),
                GT6Worldgen.FALLEN_LOG_PATHS, "the four fallen-log entry paths");
    }

    /** The disk-face constants: the codec caps documented as the areal deviation. */
    @Test
    void diskFacesArePinned() {
        assertEquals(6, GT6Worldgen.SOIL_DISK_RADIUS.getMinValue(), "radius floor 6 (codec cap 8, DiskConfiguration.java:15)");
        assertEquals(8, GT6Worldgen.SOIL_DISK_RADIUS.getMaxValue(), "radius ceiling = the codec cap");
        assertEquals(4, GT6Worldgen.PIT_CLAY_HALF_HEIGHT, "the pit rides the half_height cap (DiskConfiguration.java:16)");
        assertEquals(0, GT6Worldgen.BLACKSAND_HALF_HEIGHT, "the black-sand face is exactly the 2-layer replacement");
        assertEquals(1, GT6Worldgen.TURF_HALF_HEIGHT, "the turf face covers the top soil pair");
    }

    /** The key paths = the dot-flattened upstream config names. */
    @Test
    void keyPathsArePinned() {
        assertEquals("gt6:plant_glowtus", GT6Worldgen.GLOWTUS_CONFIGURED.location().toString());
        assertEquals("gt6:plant_glowtus", GT6Worldgen.GLOWTUS_PLACED.location().toString());
        assertEquals("gt6:plant_bush", GT6Worldgen.BUSH_CONFIGURED.location().toString());
        assertEquals("gt6:river_magnetite", GT6Worldgen.BLACKSAND_CONFIGURED.location().toString());
        assertEquals("gt6:swamp_turf", GT6Worldgen.TURF_PLACED.location().toString());
        assertEquals("gt6:pit_clay_vanilla", GT6Worldgen.PIT_CLAY_CONFIGURED.location().toString());
        assertEquals("gt6:log_dry", GT6Worldgen.FALLEN_LOG_CONFIGURED_KEYS.get(0).location().toString());
        assertEquals("gt6:log_frozen", GT6Worldgen.FALLEN_LOG_PLACED_KEYS.get(3).location().toString());
        assertEquals(4, GT6Worldgen.FALLEN_LOG_CONFIGURED_KEYS.size(), "4 fallen-log configured keys");
        assertEquals(4, GT6Worldgen.FALLEN_LOG_PLACED_KEYS.size(), "4 fallen-log placed keys");
        assertEquals("gt6:surface_glowtus", GT6Worldgen.GLOWTUS_BIOMES.location().toString());
        assertEquals("gt6:surface_log_frozen", GT6Worldgen.LOG_FROZEN_BIOMES.location().toString());
    }
}
