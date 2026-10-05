/*
 * Tests for task w6-t2-surface-blocks: the surface-plants + soil worldgen band —
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
        assertEquals(7, GT6Worldgen.SOIL_DISK_RADIUS.getMaxValue(),
                "radius 7 constant (codec cap 8, DiskConfiguration.java:15) — constant, NOT uniform: "
                        + "the uniform IntProvider JSON face is leg-forked (1.20.1 value-wrapper)");
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

    /**
     * The four colored-clay pit rows (task worldgen-diggables-pits): the path/key lists in
     * the upstream row order (Loader_Worldgen.java:593/:595-597 — the :594 red row is the
     * nether-only closure, not a gap) and the per-row modifier keys over the SHARED pit
     * biome tag (WorldgenPit.java:58 — all five pit rows carry the identical plains|savanna
     * list, so one tag carries the family).
     */
    @Test
    void coloredClayPitRowsArePinned() {
        assertEquals(List.of("pit_clay_brown", "pit_clay_yellow", "pit_clay_blue", "pit_clay_white"),
                GT6Worldgen.PIT_CLAY_PATHS, "the four colored-clay pit paths, upstream row order");
        for (int i = 0; i < GT6Worldgen.PIT_CLAY_PATHS.size(); i++) {
            String tPath = GT6Worldgen.PIT_CLAY_PATHS.get(i);
            assertEquals("gt6:" + tPath, GT6Worldgen.PIT_CLAY_CONFIGURED_KEYS.get(i).location().toString(),
                    tPath + ": the configured key, path-direct");
            assertEquals("gt6:" + tPath, GT6Worldgen.PIT_CLAY_PLACED_KEYS.get(i).location().toString(),
                    tPath + ": the placed key, path-direct");
        }
        assertEquals("gt6:surface_pit_clay", GT6Worldgen.PIT_CLAY_BIOMES.location().toString(),
                "the shared pit tag");
        assertEquals(9, gregtech6.datagen.GT6WorldgenDatagen.PLANT_BIOME_MODIFIER_KEYS.size(),
                "the plant/soil/log band keeps its nine keys; the clay rows ride PIT_CLAY_MODIFIER_KEYS");
        assertEquals(4, gregtech6.datagen.GT6WorldgenDatagen.PIT_CLAY_MODIFIER_KEYS.size(),
                "4 colored-clay pit modifier keys");
        assertEquals("gt6:pit_clay_brown", gregtech6.datagen.GT6WorldgenDatagen.PIT_CLAY_MODIFIER_KEYS.get(0).location().toString(),
                "the first clay modifier key, path-direct");
    }
}
