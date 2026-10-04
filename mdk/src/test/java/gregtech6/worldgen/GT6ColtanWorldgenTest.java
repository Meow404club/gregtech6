/**
 * Tests for task worldgen-coltan: the WorldgenColtan special generator port (the r12
 * coverage audit gap ②) — the seed-derived contention center, the center-chunk bedrock
 * vein routing, the 480-block small-ore ring, the 64-block large-ore inner ring, and the
 * committed datagen row quad (configured + placed + the two leg biome modifiers).
 *
 * <p>Offline-safe by construction: initMaterials + the vanilla bootstrap bracket only; the
 * Feature class is never touched (the GT6BedrockOreWorldgenTest posture — the offline legs
 * drive GT6ColtanGenerator + GT6BedrockOreGenerator, the same calls the Feature adapter
 * makes in the same order).
 */
package gregtech6.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;
import gregtech6.registry.GT6BedrockOreBlocks;
import gregtech6.registry.GTMaterialItems;
import gregtech6.worldgen.GT6BedrockOreGenerator.BedrockSink;
import gregtech6.worldgen.GT6ColtanGenerator.ColtanSink;

class GT6ColtanWorldgenTest {

    /** The card's fixed probe seed — the GT6BedrockOreWorldgenTest RCON seed, one constant. */
    private static final long SEED = 6131000569321125127L;

    @BeforeAll
    static void boot() {
        GTMaterialItems.initMaterials();
        // the GTOfflineRenderTestBase recipe: the version detect must precede bootStrap — a bare-JVM first boot poisons DataFixers for every later suite in this JVM (the run-order lottery)
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
        }
    }

    // ---------------------------------------------------------------- the contention center

    /**
     * THE center pin: WorldgenColtan.java:54-55 — new Random(seed+5), two nextGaussian
     * draws scaled by 1500, cast to int. The exact projection of the probe seed is a
     * literal (replay-stable — java.util.Random is spec-frozen).
     */
    @Test
    void contentionCenterIsDeterministicAndPinned() {
        int[] tCenter = GT6ColtanGenerator.center(SEED);
        assertEquals(-1463, tCenter[0], "the probe seed's center X (the :55 first gaussian draw)");
        assertEquals(-15, tCenter[1], "the probe seed's center Z (the :55 second gaussian draw)");
        // determinism: a second derivation replays identically
        assertEquals(tCenter[0], GT6ColtanGenerator.center(SEED)[0], "X replay");
        assertEquals(tCenter[1], GT6ColtanGenerator.center(SEED)[1], "Z replay");
    }

    /** The center moves with the seed (two arbitrary seeds almost surely differ). */
    @Test
    void contentionCenterMovesWithTheSeed() {
        int[] tA = GT6ColtanGenerator.center(SEED), tB = GT6ColtanGenerator.center(SEED + 1);
        assertNotEquals(tA[0] + "," + tA[1], tB[0] + "," + tB[1], "seed+1 moves the contention point");
    }

    /** WorldgenColtan.java:57 — the center BLOCK's chunk (>>4) equals the work chunk. */
    @Test
    void centerChunkMatchTruthTable() {
        assertTrue(GT6ColtanGenerator.hitsCenter(100, 200, 96, 192), "the center's own chunk");
        assertFalse(GT6ColtanGenerator.hitsCenter(100, 200, 112, 192), "the x-neighbor chunk");
        assertFalse(GT6ColtanGenerator.hitsCenter(100, 200, 96, 208), "the z-neighbor chunk");
        assertTrue(GT6ColtanGenerator.hitsCenter(-1, -1, -16, -16), "negative coords: -1>>4 == -1");
    }

    // ---------------------------------------------------------------- the ring gates

    /** :62 — a chunk beyond the 480 ring places nothing and reports false. */
    @Test
    void farChunkOutsideRingPlacesNothing() {
        RecordingSink tSink = new RecordingSink();
        assertFalse(GT6ColtanGenerator.scatter(0, 0, 10000, 10000, new Random(1), tSink), "out of ring -> false");
        assertEquals(0, tSink.ores.size(), "no ore outside the ring");
    }

    /**
     * The long-gate deviation pin: at extreme coordinates the upstream :59 int square
     * overflows NEGATIVE and the {@code > mRange*mRange} gate passes (coltan everywhere);
     * the port's long math keeps the authored ring — the far field stays empty.
     */
    @Test
    void extremeDistanceDoesNotOverflowTheRingGate() {
        RecordingSink tSink = new RecordingSink();
        assertFalse(GT6ColtanGenerator.scatter(0, 0, -29999984, 29999984, new Random(1), tSink),
                "the world-border chunk is outside the ring");
        assertEquals(0, tSink.ores.size(), "no overflow leakage at extreme distance");
    }

    /**
     * The ring face (:62-69): inside 480 but outside 64, the small-ore loop only — count
     * within the upstream :63 range, materials the 3:1:1 switch face, y inside [20, 40),
     * x/z inside the chunk. The exact count/histogram of the pinned stream is a literal.
     */
    @Test
    void ringChunkScattersSmallOresOnly() {
        RecordingSink tSink = new RecordingSink();
        // chunk min (300, 0): dX=300 -> 90000 <= 230400 in the ring, > 4096 out of the inner ring
        assertTrue(GT6ColtanGenerator.scatter(0, 0, 300, 0, new Random(20261004L), tSink), "in ring -> true");
        assertTrue(tSink.ores.size() >= 16 && tSink.ores.size() <= 32,
                "the :63 count range 16..32 at Amount 32, got " + tSink.ores.size());
        assertEquals(0, tSink.largeCount(), "no normal-form ore outside the 64 ring");
        int tColtan = 0, tColumbite = 0, tTantalite = 0;
        for (SinkOre tOre : tSink.ores) {
            assertTrue(tOre.small(), "ring ores are the small form");
            assertTrue(tOre.y() >= GT6ColtanGenerator.MIN_Y && tOre.y() < GT6ColtanGenerator.MAX_Y,
                    "y inside [20, 40), got " + tOre.y());
            assertTrue(tOre.x() >= 300 && tOre.x() < 316 && tOre.z() >= 0 && tOre.z() < 16, "x/z inside the chunk");
            if (tOre.material() == MT.OREMATS.Coltan) tColtan++;
            else if (tOre.material() == MT.OREMATS.Columbite) tColumbite++;
            else if (tOre.material() == MT.OREMATS.Tantalite) tTantalite++;
            else throw new AssertionError("stray material " + tOre.material());
        }
        assertEquals(10, tColtan, "the pinned coltan count (the switch default, 3/5 mass)");
        assertEquals(3, tColumbite, "the pinned columbite count (switch case 0)");
        assertEquals(4, tTantalite, "the pinned tantalite count (switch case 1)");
    }

    /**
     * The inner ring (:72-79): within 64 blocks the second loop re-draws its own count and
     * places the normal form. The exact pair of counts of the pinned stream is a literal.
     */
    @Test
    void innerRingAddsNormalFormOres() {
        RecordingSink tSink = new RecordingSink();
        // chunk min (16, 32): dX=16, dZ=32 -> 1280 <= 4096 inside the inner ring
        assertTrue(GT6ColtanGenerator.scatter(0, 0, 16, 32, new Random(20261004L), tSink), "inner ring -> true");
        assertEquals(17, tSink.smallCount(), "the pinned small count (the :63 loop)");
        assertEquals(20, tSink.largeCount(), "the pinned large count (the :73 re-drawn loop)");
    }

    /** The ring material switch (WorldgenColtan.java:64-68) — roll 0/1/default. */
    @Test
    void ringMaterialSwitchIsPinned() {
        assertEquals(MT.OREMATS.Columbite, GT6ColtanGenerator.material(0), ":66");
        assertEquals(MT.OREMATS.Tantalite, GT6ColtanGenerator.material(1), ":67");
        assertEquals(MT.OREMATS.Coltan, GT6ColtanGenerator.material(2), ":65 default");
        assertEquals(MT.OREMATS.Coltan, GT6ColtanGenerator.material(4), ":65 default");
    }

    // ---------------------------------------------------------------- the center-chunk flow

    /**
     * THE flow pin: the Feature adapter's exact call order on the center chunk — the :57
     * vein (the shared GT6BedrockOreGenerator shape on the coordinate-seeded stream) THEN
     * the :59-79 scatter continuing the same stream. The vein's bedrock-face rows are
     * expected EMPTY (the declared gap: no Coltan bedrock blocks — the sink null-guard),
     * the muffin/tail ores are Coltan-only.
     */
    @Test
    void centerChunkFlowReplayVeinThenScatter() {
        int tChunkX = GT6ColtanGenerator.center(SEED)[0] >> 4, tChunkZ = GT6ColtanGenerator.center(SEED)[1] >> 4;
        assertTrue(GT6ColtanGenerator.hitsCenter(GT6ColtanGenerator.center(SEED)[0], GT6ColtanGenerator.center(SEED)[1],
                tChunkX << 4, tChunkZ << 4), "the center chunk self-match");

        Random tRandom = GT6VeinGenerator.veinRandom(SEED, GT6Worldgen.COLTAN_DIMENSION_SALT, tChunkX, tChunkZ);
        RecordingBedrockSink tVein = new RecordingBedrockSink();
        assertTrue(GT6BedrockOreGenerator.generateVein(GT6ColtanGenerator.centerVein(), tRandom,
                tChunkX << 4, tChunkZ << 4, -64, tVein), "the vein places (the bedrock face exists)");
        assertEquals(0, tVein.bedrockOres.size(), "no bedrock-face ore (the declared axis gap)");
        assertTrue(tVein.shells.size() > 0, "the muffin shell places");
        for (SinkOre tOre : tVein.ores) assertEquals(MT.OREMATS.Coltan, tOre.material(), "vein ores are Coltan-only");

        RecordingSink tScatter = new RecordingSink();
        assertTrue(GT6ColtanGenerator.scatter(GT6ColtanGenerator.center(SEED)[0], GT6ColtanGenerator.center(SEED)[1],
                tChunkX << 4, tChunkZ << 4, tRandom, tScatter), "the center chunk is inside its own ring");
        assertTrue(tScatter.smallCount() >= 16, "the center chunk small ring: got " + tScatter.smallCount());
        assertTrue(tScatter.largeCount() >= 16, "the center chunk inner ring: got " + tScatter.largeCount());
        // determinism: the whole flow replays identically from a fresh stream
        Random tReplay = GT6VeinGenerator.veinRandom(SEED, GT6Worldgen.COLTAN_DIMENSION_SALT, tChunkX, tChunkZ);
        RecordingBedrockSink tVeinB = new RecordingBedrockSink();
        GT6BedrockOreGenerator.generateVein(GT6ColtanGenerator.centerVein(), tReplay, tChunkX << 4, tChunkZ << 4, -64, tVeinB);
        RecordingSink tScatterB = new RecordingSink();
        GT6ColtanGenerator.scatter(GT6ColtanGenerator.center(SEED)[0], GT6ColtanGenerator.center(SEED)[1],
                tChunkX << 4, tChunkZ << 4, tReplay, tScatterB);
        assertEquals(tVein.ores, tVeinB.ores, "the vein replay");
        assertEquals(tScatter.ores, tScatterB.ores, "the scatter replay");
    }

    /** The vein row identity: the :57 name face, the inert 1/P, the overworld row. */
    @Test
    void centerVeinRowIsPinned() {
        assertEquals("ore.special.coltan", GT6ColtanGenerator.centerVein().name(), "the :779 config name");
        assertSameMT(MT.OREMATS.Coltan, GT6ColtanGenerator.centerVein().material());
        assertTrue(GT6ColtanGenerator.centerVein().overworld(), "the overworld row face");
    }

    private static void assertSameMT(OreDictMaterial aExpected, OreDictMaterial aActual) {
        assertEquals(aExpected.mNameInternal, aActual.mNameInternal, "the vein material identity");
    }

    // ---------------------------------------------------------------- the datagen rows (THE red pin)

    /** The mdk project root, walking up from the (leg-dependent) test working dir (the boiler-card anchor). */
    private static Path mdkRoot() {
        for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
            if (Files.isRegularFile(p.resolve("tools").resolve("gen_textures.py"))) return p;
        }
        throw new AssertionError("mdk root (tools/gen_textures.py) not found upward from "
                + Path.of("").toAbsolutePath());
    }

    private static Path generated(String aRelative) {
        return mdkRoot().resolve("src/generated/resources/data/gt6").resolve(aRelative);
    }

    /**
     * THE row pin: the coltan feature chain is committed — the shared configured/placed
     * pair and the per-leg biome modifiers. RED until the rows are灌入 (the acceptance's
     * "Coltan 行缺席钉红 -> 灌入转绿").
     */
    @Test
    void datagenRowsAreCommitted() throws Exception {
        Path tConfigured = generated("worldgen/configured_feature/coltan.json");
        assertTrue(Files.isRegularFile(tConfigured), "the configured row must be committed: " + tConfigured);
        JsonObject tConfiguredJson = JsonParser.parseString(Files.readString(tConfigured, StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals("gt6:coltan", tConfiguredJson.get("type").getAsString(), "the coltan Feature type");
        assertEquals(0, tConfiguredJson.getAsJsonObject("config").entrySet().size(), "NoneFeatureConfiguration = empty config");

        Path tPlaced = generated("worldgen/placed_feature/coltan.json");
        assertTrue(Files.isRegularFile(tPlaced), "the placed row must be committed: " + tPlaced);
        JsonObject tPlacedJson = JsonParser.parseString(Files.readString(tPlaced, StandardCharsets.UTF_8)).getAsJsonObject();
        assertEquals("gt6:coltan", tPlacedJson.get("feature").getAsString(), "the placed feature reference");
        var tPlacement = tPlacedJson.getAsJsonArray("placement");
        assertEquals(3, tPlacement.size(), "Count(1) + InSquare + BiomeFilter");
        assertEquals("minecraft:count", tPlacement.get(0).getAsJsonObject().get("type").getAsString(), "the count step");
        assertEquals(1, tPlacement.get(0).getAsJsonObject().get("count").getAsInt(), "one attempt per chunk");
        assertEquals("minecraft:in_square", tPlacement.get(1).getAsJsonObject().get("type").getAsString(), "the square spread");
        assertEquals("minecraft:biome", tPlacement.get(2).getAsJsonObject().get("type").getAsString(), "the biome filter");

        for (String tLeg : new String[] {"forge", "neoforge"}) {
            Path tModifier = generated(tLeg + "/biome_modifier/coltan.json");
            assertTrue(Files.isRegularFile(tModifier), "the " + tLeg + " modifier must be committed: " + tModifier);
            JsonObject tJson = JsonParser.parseString(Files.readString(tModifier, StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals(tLeg + ":add_features", tJson.get("type").getAsString(), "the leg's add_features type");
            assertEquals("#minecraft:is_overworld", tJson.get("biomes").getAsString(), "the overworld tag");
            assertEquals("gt6:coltan", tJson.get("features").getAsString(), "the coltan placed feature");
            assertEquals("underground_ores", tJson.get("step").getAsString(), "the ore-pass step");
        }
    }

    /** The key face: both keys live on the shared path-direct shape. */
    @Test
    void keyPathsArePinned() {
        assertEquals("gt6:coltan", GT6Worldgen.COLTAN_CONFIGURED.location().toString(), "the configured key path");
        assertEquals("gt6:coltan", GT6Worldgen.COLTAN_PLACED.location().toString(), "the placed key path");
        assertEquals(GT6Worldgen.configKey("coltan"), GT6Worldgen.COLTAN_CONFIGURED, "the path-direct form");
    }

    // ---------------------------------------------------------------- recording sinks

    /** One placed ore. */
    private record SinkOre(int x, int y, int z, OreDictMaterial material, boolean small) {}

    /** The offline ColtanSink. */
    private static final class RecordingSink implements ColtanSink {
        final List<SinkOre> ores = new ArrayList<>();

        @Override
        public void ore(int aX, int aY, int aZ, OreDictMaterial aMaterial, boolean aSmall) {
            ores.add(new SinkOre(aX, aY, aZ, aMaterial, aSmall));
        }

        int smallCount() {
            return (int) ores.stream().filter(SinkOre::small).count();
        }

        int largeCount() {
            return (int) ores.stream().filter(tOre -> !tOre.small()).count();
        }
    }

    /** The offline BedrockSink (the GT6BedrockOreWorldgenTest shape: alive bedrock face, recording lists). */
    private static final class RecordingBedrockSink implements BedrockSink {
        final List<SinkOre> bedrockOres = new ArrayList<>(), ores = new ArrayList<>();
        final List<int[]> shells = new ArrayList<>();

        @Override
        public boolean isBedrockFace(int aX, int aZ) {
            return true;
        }

        @Override
        public void bedrockOre(int aX, int aY, int aZ, OreDictMaterial aMaterial, boolean aSmall) {
            // the Feature's level-sink null-guard mirrored: unregistered (material, form)
            // pairs place NOTHING (the declared gap — Coltan has no bedrock-face blocks),
            // so only the placed rows are recorded
            if (GT6BedrockOreBlocks.get(aSmall, aMaterial) == null) return;
            bedrockOres.add(new SinkOre(aX, aY, aZ, aMaterial, aSmall));
        }

        @Override
        public void shell(int aX, int aY, int aZ) {
            shells.add(new int[] {aX, aY, aZ});
        }

        @Override
        public void ore(int aX, int aY, int aZ, OreDictMaterial aMaterial, boolean aSmall) {
            ores.add(new SinkOre(aX, aY, aZ, aMaterial, aSmall));
        }
    }
}
