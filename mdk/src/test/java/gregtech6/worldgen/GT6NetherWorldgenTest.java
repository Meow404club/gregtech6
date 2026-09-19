package gregtech6.worldgen;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.datagen.GT6WorldgenDatagen;
import gregtech6.registry.GTStoneBlocks;

/**
 * The nether worldgen band pins (task p31-nether-lens-end-yield): the 17-stone lens
 * table census + the independent per-row 1/200 draw semantics + the coordinate-seeded
 * decision determinism + the End five-row draw filter + the conditions-key JSON
 * snapshots (the acceptance's 双腿各自家品牌 face). The JSON reads go off the
 * CLASSPATH (src/generated/resources is a test resource dir, the
 * GT6DualDirectoryFacesTest posture).
 */
public class GT6NetherWorldgenTest {

    private static final long SEED = 6131000569321125127L; // the S31 family scan seed

    @org.junit.jupiter.api.BeforeAll
    static void bootstrap() {
        // the ResourceKey.create static-init face needs the vanilla bootstrap (the
        // GTGrassBlockTest posture) — order-robust under --tests filters
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
            // NetworkHooks.init() failure is expected offline; registries are ready by now.
        }
        // the draw validity gate reads the GT6OreBlocks material axis (MT/OP must exist,
        // the GT6BedrockOreBlocksRegistrationTest posture)
        gregtech6.registry.GTMaterialItems.initMaterials();
    }

    // ---------------------------------------------------------------- the 17-stone lens table

    @Test
    public void netherLensTableIsTheWholeStoneUniverseInUniformUpstreamColumns() {
        List<GTLensConfig> tTable = GT6WorldgenDatagen.NETHER_LENS_TABLE;
        assertEquals(17, tTable.size(), "the upstream nether loop emits one row per stone (Loader_Worldgen.java:656)");
        assertEquals(GTStoneBlocks.STONES.stream().map(GTStoneBlocks.StoneSpec::snake).toList(),
                tTable.stream().map(GTLensConfig::stone).toList(), "GTStoneBlocks.STONES order");
        for (GTLensConfig tLens : tTable) {
            assertEquals(200, tLens.rarity(), "the per-row 1/200 roll denominator, the upstream Probability");
            assertEquals(0, tLens.minY(), "upstream MinHeight 0");
            assertEquals(120, tLens.maxY(), "upstream MaxHeight 120");
            assertEquals(40, tLens.radius(), "the clean-calibration radius (GT6Worldgen.NETHER_LENS_RADIUS)");
            assertEquals(12, tLens.halfHeight(), "the clean-calibration half-height");
            // the block-resolution face rides GTStoneBlocksRegistrationTest (the registry
            // handles bind only under the registration tests, not this headless census)
        }
    }

    // ---------------------------------------------------------------- the independent draw

    @Test
    public void independentDrawRollsEveryDrawableRowInTableOrder() {
        // a 3-row table: rarity 1 rows always hit; the dead-Y row (maxY<=minY) consumes NO draw
        List<GTLensConfig> tRows = List.of(
                new GTLensConfig("marble", 1, 0, 120, 40, 12),
                new GTLensConfig("basalt", 200, 0, 0, 40, 12), // dead Y: skipped pre-roll
                new GTLensConfig("kimberlite", 1, 0, 120, 40, 12));
        List<GTLensConfig> tHits = GT6LensGenerator.drawIndependentLenses(new GTLensConfig.Table(tRows), new Random(42));
        assertEquals(2, tHits.size(), "both rarity-1 rows hit; the dead-Y row rolled no draw");
        assertEquals("marble", tHits.get(0).stone());
        assertEquals("kimberlite", tHits.get(1).stone(), "table order preserved");
    }

    @Test
    public void independentDrawIsCoordinateSeedDeterministic() {
        GTLensConfig.Table tTable = new GTLensConfig.Table(GT6WorldgenDatagen.NETHER_LENS_TABLE);
        // replay: the same (seed, salt, origin) derives the identical hit list
        for (int tOrigin = 0; tOrigin < 40; tOrigin++) {
            List<GTLensConfig> tFirst = GT6LensGenerator.drawIndependentLenses(tTable,
                    GT6VeinGenerator.veinRandom(SEED, GT6VeinGenerator.NETHER_DIMENSION_SALT, tOrigin, tOrigin * 3));
            List<GTLensConfig> tSecond = GT6LensGenerator.drawIndependentLenses(tTable,
                    GT6VeinGenerator.veinRandom(SEED, GT6VeinGenerator.NETHER_DIMENSION_SALT, tOrigin, tOrigin * 3));
            assertEquals(tFirst, tSecond, "decision-level determinism at origin " + tOrigin);
        }
        // the dimension salt separates the streams: over 40 origins the hit pattern under
        // a different world seed diverges somewhere (per-origin lists may coincide — both
        // empty is the ~84% case — so the assertion is over the window, not one origin)
        boolean tDiverged = false;
        for (int tOrigin = 0; tOrigin < 40 && !tDiverged; tOrigin++) {
            tDiverged = !GT6LensGenerator.drawIndependentLenses(tTable,
                    GT6VeinGenerator.veinRandom(SEED, -1, tOrigin, tOrigin)).equals(
                    GT6LensGenerator.drawIndependentLenses(tTable,
                            GT6VeinGenerator.veinRandom(SEED ^ 2, -1, tOrigin, tOrigin)));
        }
        assertTrue(tDiverged, "a different world seed changes the draw pattern across the window");
    }

    @Test
    public void netherLensRateMatchesTheUpstreamSeventeenOverTwoHundred() {
        // 200 rolling origins: the expected total hits = 17 rows x 1 = 17 (one full
        // expectation window); the band is generous against binomial tails (>=5, <=40)
        GTLensConfig.Table tTable = new GTLensConfig.Table(GT6WorldgenDatagen.NETHER_LENS_TABLE);
        int tHits = 0;
        Set<String> tStones = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            for (GTLensConfig tLens : GT6LensGenerator.drawIndependentLenses(tTable,
                    GT6VeinGenerator.veinRandom(SEED, -1, i, -i))) {
                tHits++;
                tStones.add(tLens.stone());
            }
        }
        assertTrue(tHits >= 5 && tHits <= 40, "200 origins x 17 rows x 1/200 expects ~17 hits, got " + tHits);
        assertTrue(tStones.size() >= 1, "at least one distinct stone in the window");
    }

    // ---------------------------------------------------------------- the End five-row draw

    @Test
    public void endDrawSumsOnlyTheFiveOreEndRows() {
        // the drawable End set = the axis-valid subset: molybdenum carries all four slots
        // outside the 53-material registration axis (the p30-t3 declared mapping), so the
        // drawn set is platinum(5)/cassiterite(170)/naquadah(10)/trinium(100) = weight 285
        Set<String> tEndNames = Set.of("ore.large.platinum", "ore.large.molybdenum",
                "ore.large.cassiterite", "ore.large.naquadah", "ore.large.trinium");
        List<GTVeinConfig> tEndRows = GT6WorldgenDatagen.LARGE_VEIN_TABLE.stream()
                .filter(GTVeinConfig::end).toList();
        assertEquals(5, tEndRows.size(), "exactly the five ORE_END rows, Loader_Worldgen.java:904-919");
        for (GTVeinConfig tVein : tEndRows) assertTrue(tEndNames.contains(tVein.name()), tVein.name());
        // the overworld flag set stays the p30 face (31 rows)
        assertEquals(31, GT6WorldgenDatagen.LARGE_VEIN_TABLE.stream().filter(GTVeinConfig::overworld).count());
    }

    @Test
    public void endDrawNeverPicksAnOverworldOnlyRowAndReplaysIdentically() {
        List<GTVeinConfig> tTable = GT6WorldgenDatagen.LARGE_VEIN_TABLE;
        Set<String> tDrawn = new HashSet<>();
        for (int i = 0; i < 5000; i++) {
            GTVeinConfig tVein = GT6VeinGenerator.drawVein(tTable, new Random(i), true);
            assertNotNull(tVein, "the End draw always lands (weight 285 > 0)");
            assertTrue(tVein.end(), "only ORE_END rows: " + tVein.name());
            tDrawn.add(tVein.name());
        }
        assertEquals(Set.of("ore.large.platinum", "ore.large.cassiterite", "ore.large.naquadah", "ore.large.trinium"),
                tDrawn, "four drawable rows; molybdenum rides the axis-posture exclusion");
        // the zero-salt overworld overload replay: the 2-arg and the false-overload agree
        GTVeinConfig tA = GT6VeinGenerator.drawVein(tTable, new Random(7));
        GTVeinConfig tB = GT6VeinGenerator.drawVein(tTable, new Random(7), false);
        assertEquals(tA, tB, "the 2-arg overworld overload is the false face");
    }

    // ---------------------------------------------------------------- the Worley noise port

    @Test
    public void worleyNoiseStaysInContractAndReplays() {
        GT6WorleyNoise tNoise = new GT6WorleyNoise(SEED, -512);
        // the domain contract: get(x,y,z,count) in [0, count-1]
        for (int i = -64; i < 64; i += 7) {
            int tV = tNoise.get(i * 3, 0, i * 5, 85);
            assertTrue(tV >= 0 && tV < 85, "the clamped quartz slice domain [0,84]: " + tV);
            int tM = tNoise.get(i / 2, 360, i / 2, 12);
            assertTrue(tM >= 0 && tM < 12, "the 12-way crystal pick: " + tM);
            int tC = tNoise.get(i, 42, i, 8);
            assertTrue(tC >= 0 && tC < 8, "the 1/8 clay gate: " + tC);
        }
        // replay determinism: a fresh instance reproduces the draws bit-exactly
        GT6WorleyNoise tReplay = new GT6WorleyNoise(SEED, -512);
        for (int i = 0; i < 100; i++) {
            assertEquals(tNoise.get(i, 0, i, 200), tReplay.get(i, 0, i, 200), "slice replay at " + i);
        }
        // the dimension offset participates: the overworld-offset noise differs in-band
        GT6WorleyNoise tOverworld = new GT6WorleyNoise(SEED, 0);
        boolean tAnyDiff = false;
        for (int i = 0; i < 64 && !tAnyDiff; i++) {
            tAnyDiff = tNoise.get(i * 9, 0, i * 9, 200) != tOverworld.get(i * 9, 0, i * 9, 200);
        }
        assertTrue(tAnyDiff, "the 512*dimId offset separates the dimension streams");
    }

    // ---------------------------------------------------------------- the JSON snapshots

    private static JsonObject resourceJson(String aPath) throws Exception {
        try (InputStream tStream = GT6NetherWorldgenTest.class.getClassLoader().getResourceAsStream(aPath)) {
            assertNotNull(tStream, aPath + " must ship on the classpath");
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static String conditionsModId(JsonArray aConditions) {
        for (JsonElement tElement : aConditions) {
            JsonObject tCondition = tElement.getAsJsonObject();
            if (!tCondition.get("type").getAsString().endsWith(":not")) continue;
            JsonObject tValue = tCondition.getAsJsonObject("value");
            if (tValue.get("type").getAsString().endsWith(":mod_loaded")) {
                return tValue.get("modid").getAsString();
            }
        }
        return null;
    }

    /** The acceptance face: the End-yield row carries the conditions key in EACH leg's brand. */
    @Test
    public void endYieldRowShipsTheConditionsKeyInBothBrands() throws Exception {
        for (String tBrand : new String[] {"forge", "neoforge"}) {
            JsonObject tRow = resourceJson("data/gt6/" + tBrand + "/biome_modifier/large_veins_end.json");
            assertEquals(tBrand + ":add_features", tRow.get("type").getAsString(), tBrand + " type brand");
            assertEquals("#minecraft:is_end", tRow.get("biomes").getAsString(), "the End biome gate");
            assertEquals("gt6:large_veins", tRow.get("features").getAsString(), "the shared placed feature");
            JsonArray tConditions = tRow.getAsJsonArray(tBrand + ":conditions");
            assertNotNull(tConditions, "the conditions key in the " + tBrand + " brand");
            assertEquals(1, tConditions.size(), "exactly the yield inversion");
            JsonObject tNot = tConditions.get(0).getAsJsonObject();
            assertEquals(tBrand + ":not", tNot.get("type").getAsString());
            JsonObject tModLoaded = tNot.getAsJsonObject("value");
            assertEquals(tBrand + ":mod_loaded", tModLoaded.get("type").getAsString());
            assertEquals("galacticraft", tModLoaded.get("modid").getAsString(),
                    "the single-point trigger constant (GT6WorldgenDatagen.PLANET_VEIN_TRIGGER_MODID)");
            assertNotNull(conditionsModId(tConditions));
        }
    }

    /** The other 44 modifier rows ship NO conditions key (the yield inversion is the one row). */
    @Test
    public void noOtherBiomeModifierRowCarriesAConditionsKey() throws Exception {
        for (String tBrand : new String[] {"forge", "neoforge"}) {
            // spot rows across every band, including the shared nether band
            for (String tPath : new String[] {"large_veins", "strata_lenses", "bedrock_ores",
                    "ore_small_nether", "nether_lenses", "nether_quartz", "nether_crystals", "nether_clay"}) {
                JsonObject tRow = resourceJson("data/gt6/" + tBrand + "/biome_modifier/" + tPath + ".json");
                for (String tKey : tRow.keySet()) {
                    assertFalse(tKey.endsWith(":conditions"), tPath + " must ship unconditioned (got " + tKey + ")");
                }
            }
        }
    }

    /** The nether band ships complete: 4 configured + 4 placed + 4 modifiers per brand. */
    @Test
    public void netherBandShipsComplete() throws Exception {
        for (String tPath : new String[] {"nether_lenses", "nether_quartz", "nether_crystals", "nether_clay"}) {
            assertNotNull(resourceJson("data/gt6/worldgen/configured_feature/" + tPath + ".json"));
            JsonObject tPlaced = resourceJson("data/gt6/worldgen/placed_feature/" + tPath + ".json");
            for (JsonElement tModifier : tPlaced.getAsJsonArray("placement")) {
                assertNotEquals("minecraft:rarity_filter", tModifier.getAsJsonObject().get("type").getAsString(),
                        "the placed chain carries NO rarity filter (the per-chunk rolls live in the Feature): " + tPath);
            }
            for (String tBrand : new String[] {"forge", "neoforge"}) {
                JsonObject tModifier = resourceJson("data/gt6/" + tBrand + "/biome_modifier/" + tPath + ".json");
                assertEquals("#minecraft:is_nether", tModifier.get("biomes").getAsString(), tPath);
            }
        }
    }

    /** The 40-row configured table ships the end column; the five rows are true, the rest false. */
    @Test
    public void largeVeinTableShipsTheEndColumnOnExactlyFiveRows() throws Exception {
        JsonObject tConfigured = resourceJson("data/gt6/worldgen/configured_feature/large_veins.json");
        List<JsonElement> tVeins = tConfigured.getAsJsonObject("config").getAsJsonArray("veins").asList();
        assertEquals(40, tVeins.size());
        Set<String> tEndTrue = new HashSet<>();
        for (JsonElement tElement : tVeins) {
            JsonObject tRow = tElement.getAsJsonObject();
            assertTrue(tRow.has("end"), "every row carries the end column");
            if (tRow.get("end").getAsBoolean()) tEndTrue.add(tRow.get("name").getAsString());
        }
        assertEquals(Set.of("ore.large.platinum", "ore.large.molybdenum", "ore.large.cassiterite",
                "ore.large.naquadah", "ore.large.trinium"), tEndTrue);
    }
}
