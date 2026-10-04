package gregtech6.worldgen;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.world.level.block.Blocks;

/**
 * The deep-ocean prismarine-pylon pins (task worldgen-deepocean-corals): the stepped
 * four-band pyramid shape symmetric around j (WorldgenDeepOcean.java:59-101), the
 * exactly-one-draw-per-body 1/8 ore roll on the UPPER face only (:62/:67/:72/:77 + the
 * light twins), the noise-cell gate wiring (:50, cells 12-13 dark / 14-15 light) and the
 * configured/placed/biome-modifier JSON snapshots. The JSON reads go off the CLASSPATH
 * (the GT6NetherWorldgenTest posture).
 */
public class GT6DeepOceanWorldgenTest {

    private static final long SEED = 6131000569321125127L; // the S31 family scan seed

    @org.junit.jupiter.api.BeforeAll
    static void bootstrap() {
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
            // NetworkHooks.init() failure is expected offline; registries are ready by now.
        }
    }

    /** The recording sink: (x, y, z) -> block, last write wins (the WD.set semantics). */
    private static final class RecordingSink implements GT6DeepOceanFeature.Sink {
        final Map<Long, Integer> blocks = new HashMap<>();
        int writes = 0;

        @Override
        public void put(int aX, int aY, int aZ, net.minecraft.world.level.block.Block aBlock) {
            long tKey = ((long) aX & 0x3FFFFFF) << 38 | ((long) (aY + 512) & 0xFFF) << 26 | ((long) aZ & 0x3FFFFFF);
            blocks.put(tKey, System.identityHashCode(aBlock));
            writes++;
        }
    }

    // ---------------------------------------------------------------- the pylon shape

    @Test
    public void pylonIsTheFourBandSteppedPyramidSymmetricAroundJ() {
        RecordingSink tSink = new RecordingSink();
        GT6DeepOceanFeature.placePylons(100, 34, 200, new Random(42), Blocks.STONE, Blocks.GLASS, tSink);
        // distinct positions: 203 = 49+49 |dy|<=1, +25*3 |dy| 2..4, +9*3 |dy| 5..7, +1*3 |dy| 8..10
        assertEquals(203, tSink.blocks.size(), "the stepped pyramid distinct positions");
        Map<Integer, Integer> tByLayer = new HashMap<>();
        // replay the (x,z) frame: every |dy| layer keeps the band radius (7x7/5x5/3x3/1x1)
        Set<Long> tSeen = tSink.blocks.keySet();
        int tDarkStone = System.identityHashCode(Blocks.STONE), tOre = System.identityHashCode(Blocks.GLASS);
        for (long tKey : tSeen) {
            int tDy = (int) ((tKey >> 26) & 0xFFF) - 512 - 34; // stored = y+512, y = 34+dy
            int tAbs = Math.abs(tDy);
            int tExpectedSpan = tAbs <= 1 ? 7 : tAbs <= 4 ? 5 : tAbs <= 7 ? 3 : 1;
            int tSpan = spanAt(tSeen, tKey, tDy, 34);
            assertEquals(tExpectedSpan, tSpan, "the band span at dy " + tDy);
            tByLayer.merge(tAbs, 1, Integer::sum);
        }
        for (int tAbs = 0; tAbs <= 10; tAbs++) {
            assertTrue(tByLayer.containsKey(tAbs), "every |dy| 0..10 present, missing " + tAbs);
        }
        assertTrue(tSink.writes > tSink.blocks.size(), "the l=0 body double-write collapses (writes " + tSink.writes
                + " > distinct " + tSink.blocks.size() + ")");
        assertTrue(tSink.blocks.containsValue(tDarkStone), "the prismarine payload present");
        assertTrue(tSink.blocks.containsValue(tOre), "the ore payload present");
    }

    /** The x/z span of the |dy| layer holding aKey (the band radius face). */
    private static int spanAt(Set<Long> aSeen, long aKey, int aDy, int aCenterY) {
        int tX = (int) (aKey >> 38), tZ = (int) (aKey & 0x3FFFFFF);
        int tDyAbs = Math.abs(aDy);
        int tRadius = tDyAbs <= 1 ? 3 : tDyAbs <= 4 ? 2 : tDyAbs <= 7 ? 1 : 0;
        int tCount = 0;
        for (int m = -tRadius; m <= tRadius; m++) for (int n = -tRadius; n <= tRadius; n++) {
            long tProbe = ((long) (tX + m) & 0x3FFFFFF) << 38
                    | ((long) (aCenterY + aDy + 512) & 0xFFF) << 26
                    | ((long) (tZ + n) & 0x3FFFFFF);
            if (aSeen.contains(tProbe)) tCount++;
        }
        return tCount;
    }

    // ---------------------------------------------------------------- the ore roll

    @Test
    public void oreRollIsExactlyOneDrawPerBodyOnTheUpperFaceOnly() {
        RecordingSink tSink = new RecordingSink();
        Random tRandom = new Random(42);
        GT6DeepOceanFeature.placePylons(100, 34, 200, tRandom, Blocks.STONE, Blocks.GLASS, tSink);
        // the replay: 203 bodies (l 8..10 r0, l 5..7 r1, l 2..4 r2, l 0..1 r3), ONE nextInt(8)
        // draw per body in (l, m outer, n inner) order — ore replaces the UPPER set when 0
        List<int[]> tExpected = new ArrayList<>();
        Random tReplay = new Random(42);
        int tBodies = 0;
        for (int tBand = 0; tBand < 4; tBand++) {
            int tL = new int[] {8, 5, 2, 0}[tBand], tRows = tBand == 3 ? 2 : 3, tRadius = tBand;
            for (int tRow = 0; tRow < tRows; tRow++, tL++) {
                for (int m = -tRadius; m <= tRadius; m++) for (int n = -tRadius; n <= tRadius; n++) {
                    tBodies++;
                    if (tReplay.nextInt(8) == 0) tExpected.add(new int[] {100 + m, 34 + tL, 200 + n});
                }
            }
        }
        assertEquals(203, tBodies, "the upstream body census (3+27+75+98)");
        // collect the actual ore positions (the GLASS writes)
        Set<Long> tActual = new HashSet<>(), tExpect = new HashSet<>();
        int tOreId = System.identityHashCode(Blocks.GLASS);
        for (Map.Entry<Long, Integer> tEntry : tSink.blocks.entrySet()) {
            if (tEntry.getValue() == tOreId) {
                int tY = (int) ((tEntry.getKey() >> 26) & 0xFFF) - 512;
                tActual.add(tEntry.getKey());
                assertTrue(tY >= 34, "the ore rides the UPPER face only, found dy " + (tY - 34));
            }
        }
        for (int[] tPos : tExpected) {
            tExpect.add(((long) (tPos[0] & 0x3FFFFFF) << 38) | ((long) (tPos[1] + 512) & 0xFFF) << 26
                    | ((long) (tPos[2] & 0x3FFFFFF)));
        }
        assertEquals(tExpect, tActual, "the ore positions replay bit-exactly (same draw order)");
        assertFalse(tExpect.isEmpty(), "the window carries ore (7/8 miss chance over 203 bodies)");
    }

    @Test
    public void pylonReplayIsDeterministicAndSeedSensitive() {
        RecordingSink tA = new RecordingSink();
        GT6DeepOceanFeature.placePylons(100, 34, 200, new Random(42), Blocks.STONE, Blocks.GLASS, tA);
        RecordingSink tB = new RecordingSink();
        GT6DeepOceanFeature.placePylons(100, 34, 200, new Random(42), Blocks.STONE, Blocks.GLASS, tB);
        assertEquals(tA.blocks, tB.blocks, "decision-level determinism: same seed, same pylon");
        boolean tDiverged = false;
        for (int tOrigin = 0; tOrigin < 30 && !tDiverged; tOrigin++) {
            RecordingSink tOne = new RecordingSink();
            RecordingSink tTwo = new RecordingSink();
            GT6DeepOceanFeature.placePylons(tOrigin * 16, 34, -tOrigin * 16, new Random(42), Blocks.STONE, Blocks.GLASS, tOne);
            GT6DeepOceanFeature.placePylons(tOrigin * 16, 34, -tOrigin * 16, new Random(43), Blocks.STONE, Blocks.GLASS, tTwo);
            tDiverged = !tOne.blocks.equals(tTwo.blocks);
        }
        assertTrue(tDiverged, "a different seed changes the pylon across the window");
    }

    // ---------------------------------------------------------------- the noise gate wiring

    @Test
    public void pylonCellsAreTheSixOfSixteenNoiseCells() {
        assertEquals(16, GT6DeepOceanFeature.GATE_CELLS, "the 16-cell noise draw (WorldgenDeepOcean.java:50)");
        assertEquals(32, GT6DeepOceanFeature.GATE_PROBE_Y, "the fixed probe Y :50");
        assertEquals(8, GT6DeepOceanFeature.GATE_PROBE_XZ, "the chunk-center probe offset aMinX+8 :50");
        // the noise face: over 4096 chunk origins the 6/16 pylon band fires with a generous
        // band (the cell placement is world-geometry, not a uniform die — the nether 1/8
        // gate posture)
        GT6WorleyNoise tNoise = new GT6WorleyNoise(SEED, 0);
        int tPylons = 0;
        final int tWindow = 4096;
        for (int c = 0; c < tWindow; c++) {
            int tCell = tNoise.get(c * 16 + 8, GT6DeepOceanFeature.GATE_PROBE_Y, -c * 16 + 8,
                    GT6DeepOceanFeature.GATE_CELLS);
            assertTrue(tCell >= 0 && tCell < 16, "the cell domain [0,15]: " + tCell);
            if (tCell >= 12) tPylons++;
        }
        assertTrue(tPylons > tWindow / 8 && tPylons < tWindow / 2,
                "the 6/16 pylon band over " + tWindow + " origins: " + tPylons);
    }

    // ---------------------------------------------------------------- the JSON snapshots

    private static JsonObject resourceJson(String aPath) throws Exception {
        try (InputStream tStream = GT6DeepOceanWorldgenTest.class.getClassLoader().getResourceAsStream(aPath)) {
            assertNotNull(tStream, aPath + " must ship on the classpath");
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    @Test
    public void deepOceanShipsConfiguredPlacedAndBothBrandModifiers() throws Exception {
        JsonObject tConfigured = resourceJson("data/gt6/worldgen/configured_feature/deep_ocean.json");
        assertEquals("gt6:deep_ocean", tConfigured.get("type").getAsString(), "the registered feature id");
        assertEquals("{}", tConfigured.getAsJsonObject("config").toString(), "the NoneFeatureConfiguration face");

        JsonObject tPlaced = resourceJson("data/gt6/worldgen/placed_feature/deep_ocean.json");
        assertEquals("gt6:deep_ocean", tPlaced.get("feature").getAsString());
        List<JsonElement> tChain = tPlaced.getAsJsonArray("placement").asList();
        assertEquals(3, tChain.size(), "count+in_square+biome, the nether-form shape");
        assertEquals("minecraft:count", tChain.get(0).getAsJsonObject().get("type").getAsString());
        assertEquals(1, tChain.get(0).getAsJsonObject().get("count").getAsInt());
        assertEquals("minecraft:in_square", tChain.get(1).getAsJsonObject().get("type").getAsString());
        assertEquals("minecraft:biome", tChain.get(2).getAsJsonObject().get("type").getAsString());
        for (JsonElement tStep : tChain) {
            assertNotEquals("minecraft:rarity_filter", tStep.getAsJsonObject().get("type").getAsString(),
                    "no rarity filter — the gate lives in the Feature's noise cell");
        }

        for (String tBrand : new String[] {"forge", "neoforge"}) {
            JsonObject tModifier = resourceJson("data/gt6/" + tBrand + "/biome_modifier/deep_ocean.json");
            assertEquals(tBrand + ":add_features", tModifier.get("type").getAsString(), tBrand + " type brand");
            assertEquals("#minecraft:is_deep_ocean", tModifier.get("biomes").getAsString(),
                    "the deep-ocean tag face (upstream BiomeGenBase.deepOcean, the modern family face)");
            assertEquals("gt6:deep_ocean", tModifier.get("features").getAsString());
            assertEquals("top_layer_modification", tModifier.get("step").getAsString(),
                    "the seabed face — after the vegetal pass, pylons win over kelp/seagrass");
            for (String tKey : tModifier.keySet()) {
                assertFalse(tKey.endsWith(":conditions"), "unconditioned row");
            }
        }
    }
}
