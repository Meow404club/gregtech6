package gregtech6.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.datagen.GT6BiomeModifierConditions;
import gregtech6.datagen.GT6BiomeTags;
import gregtech6.datagen.GT6WorldgenDatagen;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GT6TreeBlocks;

/**
 * The twilight vegetation pins (task twilight-vegetation — the mod-dimension adaptation
 * skeleton's surface band): the PER-FEATURE TAG reconciliation table
 * ({@link GT6BiomeTags#TWILIGHT_MEMBERS}, the main-session ruling — the TF-wide mount
 * premise was overturned, every WorldgenOnSurface subclass overrides canGenerate with a
 * BiomeNameSet name check) read back from the emitted tag JSONs off the classpath, the
 * twilight.rocks rider (Loader_Worldgen.java:621 amount=4 ≠ the overworld row :618
 * amount=2 → the independent placed twin + the one new biome modifier + the third
 * CONDITION_ROWS entry), and the features-count parity (the tag extension widens NO
 * feature list — every touched modifier keeps a singleton features array).
 *
 * <p>The reconciliation table (upstream name set ∩ the TF 1.20.1 universe,
 * TFBiomes.java:19-48 + the en_us lang display names; CS.java:263-289):
 * <pre>
 * tree.rubber   :608 BIOMES_RUBBER(:267)      → highlands, snowy_forest          #gt6:trees/rubber
 * tree.maple    :609 BIOMES_MAPLE(:273)       → firefly_forest                   #gt6:trees/maple
 * tree.willow   :610 BIOMES_WILLOW(:264)      → swamp                            #gt6:trees/willow
 * tree.hazel    :612 BIOMES_HAZEL(:277)       → clearing                         #gt6:trees/hazel
 * tree.rainbowood:615 BIOMES_RAINBOWOOD(:289) → enchanted_forest                 #gt6:trees/rainbowood
 * tree.bluespruce:616 BIOMES_BLUESPRUCE(:282) → highlands, thornlands            #gt6:trees/blue_spruce
 * sticks        :630 WorldgenSticks:53-55     → dense{forest,dense_forest*,firefly_forest,
 *                                                dark_forest,dark_forest_center,swamp,fire_swamp}
 *                                                moderate{stream,clearing,oak_savannah*}
 *                                                sparse{highlands,snowy_forest}
 * glowtus       :632 WorldgenGlowtus:49       → fire_swamp                       #gt6:surface_glowtus
 * bush          :633 WorldgenBushes:56        → the plains|woods six             #gt6:surface_bush
 * black sand    :581 WorldgenBlackSand:49-51  → stream                           #gt6:surface_blacksand
 * log.dry       :603 WorldgenLogDry:50        → plains|woods|SAVANNA seven       #gt6:surface_log_dry
 * log.rotten    :604 WorldgenLogRotten:49     → swamp, fire_swamp                #gt6:surface_log_rotten
 * log.mossy     :605 WorldgenLogMossy:52      → plains|woods|swamp eight         #gt6:surface_log_mossy
 * log.frozen    :606 WorldgenLogFrozen:50     → snowy_forest, glacier            #gt6:surface_log_frozen
 * twilight.rocks:621 WorldgenRocks:54         → the untouched-twelve             #gt6:surface_rocks_twilight
 * </pre>
 * (*) the declared name drift: TF 1.20.1 renamed "Dense Twilight Forest" → "Dense
 * Forest" (twilightforest:dense_forest) — the new display name is in NO upstream
 * name set, so dense_forest rides the WOODS/FOREST rows BY DECLARATION (the same
 * biome renamed; strict display-name matching would exclude it). oak_savannah
 * displays "Oak Savanna" and is caught ONLY by BIOMES_SAVANNA (:261) — it rides
 * exactly the savanna-flavored rows (sticks moderate, log dry, twilight rocks)
 * and NO tree tag or plains/woods-only row; clearing displays "Twilight Clearing",
 * matched verbatim by BIOMES_PLAINS (:276) and BIOMES_HAZEL (:277).
 *
 * <p>THE TF-ABSENCE SEMANTICS: every appended member is {@code required:false}
 * (TagsProvider.addOptional, TagsProvider.java:152) — TF absent, the unknown members
 * drop at datapack load, the tags resolve to their vanilla part, zero mounts, zero
 * errors; the rocks modifier additionally carries the mod_loaded condition (the
 * twilight_ores detection face). The KJS face: everything here is datapack-domain
 * (the tag + biome-modifier + placed JSONs are player-editable), no other surface.
 */
public class GT6TwilightVegetationWorldgenTest {

    @org.junit.jupiter.api.BeforeAll
    static void bootstrap() {
        // the residual_rule: any test touching the worldgen table classes boots materials
        // FIRST (forge-test-hygiene 42c9ad4e2 discipline), then the vanilla bootstrap
        // bracket for the TagKey/ResourceKey static-init face (offline throwables ignored).
        GTMaterialItems.initMaterials();
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
        }
    }

    private static JsonObject resourceJson(String aPath) throws Exception {
        try (InputStream tStream = GT6TwilightVegetationWorldgenTest.class.getClassLoader()
                .getResourceAsStream(aPath)) {
            assertNotNull(tStream, aPath + " must ship on the classpath");
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
    }

    /** The expected tag values: the vanilla part (strings/tag refs) then the optional TF objects. */
    private static JsonArray expectedValues(List<String> aVanilla, List<String> aTwilight) {
        JsonArray rValues = new JsonArray();
        for (String tVanilla : aVanilla) rValues.add(tVanilla);
        for (String tMember : aTwilight) {
            JsonObject tEntry = new JsonObject();
            tEntry.addProperty("id", "twilightforest:" + tMember);
            tEntry.addProperty("required", false);
            rValues.add(tEntry);
        }
        return rValues;
    }

    /**
     * THE RECONCILIATION TABLE, pinned as full values-array equality per tag: the vanilla
     * subset transcribed verbatim (GT6BiomeTags.addTags emission order) followed by the
     * required:false twilight members in TWILIGHT_MEMBERS order. Any drift — a lost
     * vanilla member, a misnamed TF id, a required:true slip, an order change — fails.
     */
    @Test
    public void vegetationTagReconciliationIsPinned() throws Exception {
        Map<String, List<String>> tTable = new java.util.LinkedHashMap<>();
        tTable.put("trees/rubber", List.of("minecraft:taiga", "minecraft:snowy_taiga",
                "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga"));
        tTable.put("trees/maple", List.of("minecraft:forest"));
        tTable.put("trees/willow", List.of("minecraft:swamp"));
        tTable.put("trees/hazel", List.of("minecraft:plains"));
        tTable.put("trees/rainbowood", List.of());
        tTable.put("trees/blue_spruce", List.of("minecraft:windswept_hills", "minecraft:windswept_forest",
                "minecraft:windswept_gravelly_hills", "minecraft:stony_shore"));
        tTable.put("sticks_dense", List.of("minecraft:swamp", "minecraft:mangrove_swamp", "#minecraft:is_forest"));
        tTable.put("sticks_moderate", List.of("minecraft:plains", "minecraft:snowy_plains",
                "minecraft:sunflower_plains", "#minecraft:is_river", "#minecraft:is_savanna"));
        tTable.put("sticks_sparse", List.of("#minecraft:is_taiga", "#minecraft:is_badlands"));
        tTable.put("surface_glowtus", List.of("minecraft:jungle", "minecraft:sparse_jungle", "minecraft:bamboo_jungle"));
        tTable.put("surface_bush", List.of("minecraft:plains", "minecraft:sunflower_plains", "#minecraft:is_forest"));
        tTable.put("surface_blacksand", List.of("#minecraft:is_river"));
        tTable.put("surface_log_dry", List.of("minecraft:plains", "minecraft:sunflower_plains", "minecraft:desert",
                "#minecraft:is_forest", "#minecraft:is_savanna", "#minecraft:is_badlands"));
        tTable.put("surface_log_rotten", List.of("minecraft:swamp", "minecraft:mangrove_swamp", "#minecraft:is_jungle"));
        tTable.put("surface_log_mossy", List.of("minecraft:plains", "minecraft:sunflower_plains", "minecraft:swamp",
                "minecraft:mangrove_swamp", "#minecraft:is_forest"));
        tTable.put("surface_log_frozen", List.of("minecraft:snowy_plains", "minecraft:ice_spikes", "minecraft:snowy_taiga"));
        tTable.put("surface_rocks_twilight", List.of());
        assertEquals(tTable.keySet(), GT6BiomeTags.TWILIGHT_MEMBERS.keySet(),
            "the emission walk and the pin table must carry the SAME 17 tag rows");
        for (Map.Entry<String, List<String>> tRow : GT6BiomeTags.TWILIGHT_MEMBERS.entrySet()) {
            JsonObject tTag = resourceJson("data/gt6/tags/worldgen/biome/" + tRow.getKey() + ".json");
            assertEquals(expectedValues(tTable.get(tRow.getKey()), tRow.getValue()), tTag.getAsJsonArray("values"),
                "tag " + tRow.getKey() + ": the vanilla part + the optional twilight members, exact order");
        }
    }

    /**
     * The negative face of the reconciliation: the three non-TF trees (upstream
     * BIOMES_BLUEMAHOE/BIOMES_CINNAMON = the jungle family, BIOMES_COCONUT = the beach
     * family — no TF member in either, CS.java:255-256/:279) stay vanilla-only, and no
     * TF id leaks anywhere with required:true.
     */
    @Test
    public void nonTwilightTreesStayVanillaOnly() throws Exception {
        for (String tSnake : List.of("blue_mahoe", "cinnamon", "coconut")) {
            JsonArray tValues = resourceJson("data/gt6/tags/worldgen/biome/trees/" + tSnake + ".json")
                    .getAsJsonArray("values");
            for (JsonElement tElement : tValues) {
                if (tElement.isJsonPrimitive()) continue;
                // (review-seat rebase union, task atum-dim-adaptation): the coconut gate
                // admits Atum's Oasis by display name (BIOMES_COCONUT CS.java:285 — the
                // atum:oasis required:false rider); the negative face stays scoped to the
                // twilight dim — the ONLY allowed non-primitive is that declared rider.
                com.google.gson.JsonObject tObj = tElement.getAsJsonObject();
                assertEquals("atum:oasis", tObj.get("id").getAsString(),
                    "trees/" + tSnake + " non-primitive member must be the declared atum rider (got " + tElement + ")");
                assertEquals(false, tObj.get("required").getAsBoolean(),
                    "trees/" + tSnake + " the atum rider stays required:false");
            }
        }
    }

    /**
     * The twilight.rocks rider (Loader_Worldgen.java:621 {@code ("twilight.rocks", T, 4, 3,
     * GEN_TWILIGHT)}): Amount=4 — NOT the overworld row's :618 Amount=2, hence the placed
     * twin over the SAME configured lottery — and the shared Probability=3.
     */
    @Test
    public void twilightRocksRowIsPinned() throws Exception {
        assertEquals(4, GT6Worldgen.TWILIGHT_SURFACE_ROCKS_AMOUNT, ":621 Amount=4 (overworld :618 = 2)");
        assertEquals(2, GT6Worldgen.SURFACE_ROCKS_AMOUNT, "the overworld row stays 2 (the 4≠2 premise)");
        assertEquals(3, GT6Worldgen.SURFACE_ROCKS_PROBABILITY, "the shared :621 Probability=3");
        assertEquals("gt6:twilight_surface_rocks", GT6Worldgen.TWILIGHT_SURFACE_ROCKS_PLACED.location().toString());
        assertEquals("gt6:surface_rocks_twilight", GT6Worldgen.SURFACE_ROCKS_TWILIGHT_BIOMES.location().toString());
        assertEquals("gt6:twilight_surface_rocks",
                GT6WorldgenDatagen.TWILIGHT_SURFACE_ROCKS_MODIFIER_KEY.location().toString());

        // the placed JSON: the SAME configured lottery pointer, rarity 3, count 4.
        JsonObject tPlaced = resourceJson("data/gt6/worldgen/placed_feature/twilight_surface_rocks.json");
        assertEquals("gt6:overworld_surface_rocks", tPlaced.get("feature").getAsString(),
            "the placed twin hangs off the shared RANDOM_SELECTOR lottery");
        JsonArray tPlacement = tPlaced.getAsJsonArray("placement");
        JsonObject tRarity = tPlacement.get(0).getAsJsonObject();
        assertEquals("minecraft:rarity_filter", tRarity.get("type").getAsString());
        assertEquals(3, tRarity.get("chance").getAsInt(), "the shared 1/3 gate");
        JsonObject tCount = tPlacement.get(1).getAsJsonObject();
        assertEquals("minecraft:count", tCount.get("type").getAsString());
        assertEquals(4, tCount.get("count").getAsInt(), "the twilight ray mass");
        assertEquals("minecraft:biome", tPlacement.get(2).getAsJsonObject().get("type").getAsString());

        // the modifier row in BOTH leg brands: our own 12-member tag, singleton features,
        // the vegetal step, the mod_loaded condition (the twilight_ores detection face).
        for (String tBrand : List.of("forge", "neoforge")) {
            JsonObject tModifier = resourceJson("data/gt6/" + tBrand + "/biome_modifier/twilight_surface_rocks.json");
            assertEquals(tBrand + ":add_features", tModifier.get("type").getAsString());
            assertEquals("#gt6:surface_rocks_twilight", tModifier.get("biomes").getAsString());
            // the single-holder set serializes as the bare string (the tree_rubber face).
            assertEquals("gt6:twilight_surface_rocks", tModifier.get("features").getAsString(),
                "the singleton feature (the parity face)");
            assertEquals("vegetal_decoration", tModifier.get("step").getAsString(),
                "the surface-deco step (the overworld rocks row's step)");
            JsonArray tConditions = tModifier.getAsJsonArray(tBrand + ":conditions");
            assertEquals(1, tConditions.size(), "exactly the positive mod_loaded");
            JsonObject tCondition = tConditions.get(0).getAsJsonObject();
            assertEquals(tBrand + ":mod_loaded", tCondition.get("type").getAsString());
            assertEquals("twilightforest", tCondition.get("modid").getAsString());
        }
        // the conditions registry row (the fourth tenant — the twilight_stones row of
        // task twilight-stone-rows landed first, this is the rebase-union seam; the
        // twilight-hives-springs rows 5-6 landed after, the next ratchet step).
        assertEquals(12, GT6BiomeModifierConditions.CONDITION_ROWS.size(),
                "end_yield + twilight_ores + twilight_stones + this row + hives + springs"
                        + " (the review-seat rebase union appends the SIX atum rows, task"
                        + " atum-dim-adaptation — 6 + 6 = 12, the twilight prefix order"
                        + " unchanged)");
        assertEquals("twilight_surface_rocks",
                GT6BiomeModifierConditions.CONDITION_ROWS.get(3).rowPath());
    }

    /**
     * The features-count parity: the tag extension widens NO feature list — the 9 tree
     * biome-modifier rows stay keyed 1:1 on the 9 kinds, every TF-touched tree modifier
     * keeps a singleton features array (a TF-wide mount would have shown up as features
     * duplicated across tags), and the surface band's four-key list is untouched (the
     * twilight rocks row rides its own key).
     */
    @Test
    public void featuresCountParityIsPinned() throws Exception {
        assertEquals(GT6TreeBlocks.KINDS.size(), GT6WorldgenDatagen.TREE_BIOME_MODIFIER_KEYS.size(),
            "one tree modifier per kind, unchanged");
        for (int i = 0; i < GT6TreeBlocks.KINDS.size(); i++) {
            String tPath = GT6WorldgenDatagen.TREE_BIOME_MODIFIER_KEYS.get(i).location().getPath();
            JsonObject tModifier = resourceJson("data/gt6/forge/biome_modifier/" + tPath + ".json");
            // the single-holder set serializes as the bare string; the multi as an array
            // (the twilight_ores 23-row face) — both shapes stay single-feature here.
            JsonElement tFeatures = tModifier.get("features");
            assertTrue(tFeatures.isJsonPrimitive() || tFeatures.getAsJsonArray().size() == 1,
                tPath + " keeps a single-feature list (the tag-extension-only face), got " + tFeatures);
        }
        assertEquals(4, GT6WorldgenDatagen.SURFACE_BIOME_MODIFIER_KEYS.size(),
            "the overworld surface band stays four rows; the twilight rocks row rides its own key");
        // the trees/rubber modifier still hangs off the per-tree tag (not TF-wide).
        assertEquals("#gt6:trees/rubber",
                resourceJson("data/gt6/forge/biome_modifier/tree_rubber.json").get("biomes").getAsString());
    }

    /** The sticks tiers keep their TF disjointness (upstream WorldgenSticks.java:53-55 takes the max tier per chunk). */
    @Test
    public void sticksTiersStayDisjoint() {
        List<String> tDense = GT6BiomeTags.TWILIGHT_MEMBERS.get("sticks_dense");
        List<String> tModerate = GT6BiomeTags.TWILIGHT_MEMBERS.get("sticks_moderate");
        List<String> tSparse = GT6BiomeTags.TWILIGHT_MEMBERS.get("sticks_sparse");
        for (List<String> tTier : List.of(tModerate, tSparse)) {
            for (String tMember : tTier) {
                assertTrue(!tDense.contains(tMember), "dense tier must not swallow " + tMember);
            }
        }
        for (String tMember : tSparse) {
            assertTrue(!tModerate.contains(tMember), "moderate tier must not swallow " + tMember);
        }
        // the tier walk carries exactly 12 distinct TF members (7 dense + 3 moderate + 2 sparse).
        List<String> tAll = new ArrayList<>();
        tAll.addAll(tDense);
        tAll.addAll(tModerate);
        tAll.addAll(tSparse);
        assertEquals(12, tAll.size());
    }

    /**
     * The whole-TF-universe accounting: the band touches 15 of TF 1.20.1's 22 biomes
     * (TFBiomes.java:19-48) — the 7 untouched ones are exactly the biomes no card name
     * set ever matched (the two mushroom pair, spooky_forest, lake — BIOMES_RIVER_LAKE
     * only, final_plateau, and the two underground cave biomes).
     */
    @Test
    public void twilightUniverseAccountingIsPinned() {
        java.util.Set<String> tTouched = new java.util.HashSet<>();
        GT6BiomeTags.TWILIGHT_MEMBERS.values().forEach(tTouched::addAll);
        assertEquals(15, tTouched.size(), "15 distinct TF members across the band");
        List<String> tUntouched = List.of("mushroom_forest", "dense_mushroom_forest", "spooky_forest",
                "lake", "final_plateau", "underground", "highlands_underground");
        for (String tBiome : tUntouched) {
            assertTrue(!tTouched.contains(tBiome), tBiome + " must stay untouched (no upstream name-set match)");
        }
        assertEquals(22, tTouched.size() + tUntouched.size(), "TF 1.20.1 = 22 biomes (TFBiomes.java:19-48)");
    }

    /**
     * The oak_savannah drift-guard: its display name "Oak Savanna" (the generated
     * en_us lang) is caught ONLY by BIOMES_SAVANNA (CS.java:261) — so it rides exactly
     * the savanna-flavored rows (sticks moderate via the :54 tier, log dry via the :50
     * group list, twilight rocks via the :54 nine-group union) and NO plains/woods-only
     * row (hazel :277, bush WorldgenBushes.java:56, log mossy WorldgenLogMossy.java:52)
     * and NO tree tag at all. Guards the crashed predecessor's "clearing-family" slip.
     */
    @Test
    public void oakSavannahRidesOnlyTheSavannaFlavoredRows() {
        List<String> tAllowed = List.of("sticks_moderate", "surface_log_dry", "surface_rocks_twilight");
        for (Map.Entry<String, List<String>> tRow : GT6BiomeTags.TWILIGHT_MEMBERS.entrySet()) {
            boolean tHas = tRow.getValue().contains("oak_savannah");
            assertEquals(tAllowed.contains(tRow.getKey()), tHas,
                "oak_savannah membership wrong in " + tRow.getKey() + " (savanna-only display catch)");
        }
    }
}
