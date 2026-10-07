/**
 * The convergence pins (task parse-errors-registration-convergence, coordinator ruling
 * 2026-10-07): the committed JSON faces answer the seeded-narrow live registration universe
 * with the ONE atlas predicate — recipes carry the {@code forge:mod_loaded} conditions the
 * RecipeManager patch processes before the serializer (RecipeManager.java.patch:29, the
 * top-level "conditions" key), loot rows skip the gated pairs at datagen (no load-time
 * condition mechanism exists there, ForgeHooks.loadLootTable). The law this class pins:
 * every committed row referencing a would-be-absent gt6 id is either conditioned (recipes)
 * or absent (loot) — the bare install loads clean, the with-mod install loads the row.
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import gregapi.data.MT;
import gregtech6.registry.GTMaterialItems;

public class GT6ForeignRowConvergenceTest {

    @BeforeAll
    public static void initMaterialSystem() {
        // the BuiltInRegistries wall: serializeRecipe() reads the serializer registry — bootstrap
        // offline (the GT6ModDriverClearOutWaveTest BeforeAll form)
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
            // offline init noise; the registries are usable by now
        }
        GTMaterialItems.initMaterials();
    }

    // ---- the one predicate: the four B2 header materials + the B5 face, and the never-gates ----

    @Test
    public void theB2HeaderMaterialsCarryTheirAtlasDomains() {
        assertEquals(MT.MD.RT.mID, GT6ForeignRowConvergence.gatingDomain(MT.SpectreIron), "SpectreIron (upstream MT.java:2611)");
        assertEquals(MT.MD.MET.mID, GT6ForeignRowConvergence.gatingDomain(MT.AstralSilver), "AstralSilver (upstream MT.java:2690)");
        assertEquals(MT.MD.MET.mID, GT6ForeignRowConvergence.gatingDomain(MT.ShadowIron), "ShadowIron (upstream MT.java:2713)");
        assertEquals(MT.MD.TF.mID, GT6ForeignRowConvergence.gatingDomain(MT.MeteoflameRedSteel), "MeteoflameRedSteel (upstream MT.java:2294)");
        assertEquals(MT.MD.MO.mID, GT6ForeignRowConvergence.gatingDomain(MT.Dolamide), "Dolamide (the B5 loot arm, upstream MT.java:2608)");
    }

    @Test
    public void ungatedMaterialsAnswerNull() {
        assertNull(GT6ForeignRowConvergence.gatingDomain(MT.Fe), "unattributed materials gate nowhere");
        assertNull(GT6ForeignRowConvergence.gatingDomain(null), "null material");
        // COMMON_SECONDARY / GT6_SELF never gate (ADR-MDH2): the live seed never hides them,
        // so their rows must load unconditionally
        for (gregtech6.registry.GT6ForeignMaterialAtlas.Row tRow : gregtech6.registry.GT6ForeignMaterialAtlas.rows()) {
            if (tRow.kind() == gregtech6.registry.GT6ForeignMaterialAtlas.AttributionKind.PRIMARY) continue;
            gregapi.oredict.OreDictMaterial tMaterial = tRow.material().get();
            if (tMaterial == null || tMaterial.mID < 0) continue;
            assertNull(GT6ForeignRowConvergence.gatingDomain(tMaterial),
                    tRow.kind() + " row never gates: " + tMaterial.mNameInternal);
        }
    }

    // ---- the recipe form: conditions coverage over the gated ids ----

    @Test
    public void gatedIdRowsCarryTheDomainCondition() {
        JsonObject tRow = new JsonObject();
        tRow.addProperty("type", "minecraft:crafting_shaped");
        JsonObject tIngredient = new JsonObject();
        tIngredient.addProperty("item", "gt6:stick_astral_silver");
        tRow.add("key", tIngredient);
        List<String> tDomains = GT6ForeignRowConvergence.rowDomains(tRow);
        assertEquals(List.of(MT.MD.MET.mID), tDomains, "the stick_astral_silver row requires the MET domain (log:229019 header)");
    }

    @Test
    public void multiDomainRowsRequireEveryReferencedDomain() {
        JsonObject tRow = new JsonObject();
        JsonObject tFirst = new JsonObject();
        tFirst.addProperty("item", "gt6:tool_head_hammer_spectre_iron"); // RT (log:126662 header)
        JsonObject tSecond = new JsonObject();
        tSecond.addProperty("item", "gt6:foil_shadow_iron"); // MET (log:264319 header)
        JsonArray tIngredients = new JsonArray();
        tIngredients.add(tFirst);
        tIngredients.add(tSecond);
        tRow.add("ingredients", tIngredients);
        List<String> tDomains = GT6ForeignRowConvergence.rowDomains(tRow);
        assertEquals(List.of(MT.MD.MET.mID, MT.MD.RT.mID), tDomains, "sorted, distinct — AND semantics");
    }

    @Test
    public void nestedVariantArraysRideTheSameLaw() {
        // the dioptase live finding (the 2026-10-07 bare-boot residual, 169 errors): the
        // any-iron variant lists are arrays INSIDE the ingredients array (Forge grouped
        // ingredients, the alternative-items form) — a walker that only descends into
        // array members' objects misses the gated variant and ships the row unconditioned.
        // The paired id is the registration-universe member (toolHeadPickaxe x IronCompressed
        // rides registrationOrder — the pickaxe/iron_compressed row conditions on it); the
        // toolHeadRawPickaxe twin is OUTSIDE the universe entirely (the walk-gap residual,
        // declared on the card).
        JsonObject tRow = new JsonObject();
        JsonArray tIngredients = new JsonArray();
        JsonObject tGem = new JsonObject();
        tGem.addProperty("item", "gt6:gem_flawed_dioptase");
        tIngredients.add(tGem);
        JsonArray tVariants = new JsonArray();
        JsonObject tIron = new JsonObject();
        tIron.addProperty("item", "gt6:tool_head_pickaxe_iron");
        tVariants.add(tIron);
        JsonObject tCompressed = new JsonObject();
        tCompressed.addProperty("item", "gt6:tool_head_pickaxe_iron_compressed");
        tVariants.add(tCompressed);
        tIngredients.add(tVariants);
        tRow.add("ingredients", tIngredients);
        assertEquals(List.of(MT.MD.PnC.mID), GT6ForeignRowConvergence.rowDomains(tRow),
                "the nested variant array's gated member is seen (the dioptase arm)");
    }

    @Test
    public void vanillaOnlyRowsCarryNoConditions() {
        JsonObject tRow = new JsonObject();
        JsonObject tIngredient = new JsonObject();
        tIngredient.addProperty("item", "minecraft:iron_ingot");
        tRow.add("key", tIngredient);
        assertTrue(GT6ForeignRowConvergence.rowDomains(tRow).isEmpty(), "no gt6 refs, no domains");
        JsonObject tUngated = new JsonObject();
        tUngated.addProperty("item", "gt6:ingot_iron");
        tRow.add("result", tUngated);
        assertTrue(GT6ForeignRowConvergence.rowDomains(tRow).isEmpty(), "ungated gt6 ids stay unconditional");
    }

    // ---- the wrapper (forge leg: the only leg with gt6 recipe rows) ----

    //? if forge {
    @Test
    public void convergedWrapsGatedRowsWithTheConditionsArray() {
        net.minecraft.data.recipes.FinishedRecipe tRow = stub("gt6:test_row", "gt6:tool_head_construction_pickaxe_meteoflame_red_steel");
        net.minecraft.data.recipes.FinishedRecipe tWrapped = GT6ForeignRowConvergence.converged(tRow);
        assertTrue(tWrapped != tRow, "a gated row is wrapped");
        JsonObject tJson = tWrapped.serializeRecipe();
        assertTrue(tJson.has("conditions"), "the wrapper adds the conditions array");
        JsonArray tConditions = tJson.getAsJsonArray("conditions");
        assertEquals(1, tConditions.size(), "one domain");
        JsonObject tCondition = tConditions.get(0).getAsJsonObject();
        assertEquals("forge:mod_loaded", tCondition.get("type").getAsString(), "the condition type the RecipeManager patch processes");
        assertEquals(MT.MD.TF.mID, tCondition.get("modid").getAsString(), "the modid the live seed tests");
        assertEquals("minecraft:crafting_shaped", tJson.get("type").getAsString(), "the recipe body survives the wrap");
    }

    @Test
    public void convergedPassesVanillaRowsThroughUnchanged() {
        net.minecraft.data.recipes.FinishedRecipe tRow = stub("gt6:test_plain", "minecraft:iron_ingot");
        assertSame(tRow, GT6ForeignRowConvergence.converged(tRow), "ungated rows keep the identical instance (zero cost)");
    }

    @Test
    public void theB5BlockFaceRidesTheSamePredicate() {
        // the B5 loot headers are BOTH gated (the atlas attributions explain the observed
        // bare-install errors): rock_gt_dolamide (MO, upstream MT.java:2608) and
        // block_ingot_obsidian_refined (Mek, RefinedObsidian upstream MT.java:2435, the
        // log:20000 header) — the loot faces skip both at datagen.
        assertEquals(MT.MD.Mek.mID, GT6ForeignRowConvergence.gatingDomain(MT.RefinedObsidian), "RefinedObsidian is Mek PRIMARY (the block_ingot_obsidian_refined loot arm)");
        assertEquals(MT.MD.MO.mID, GT6ForeignRowConvergence.gatingDomain(MT.Dolamide), "Dolamide is MO PRIMARY (the rock_gt_dolamide loot arm)");
    }

    /** The minimal FinishedRecipe stub: one "item" member under "result". */
    private net.minecraft.data.recipes.FinishedRecipe stub(String aId, String aItemId) {
        JsonObject tBody = new JsonObject();
        tBody.addProperty("type", "minecraft:crafting_shaped");
        JsonObject tResult = new JsonObject();
        tResult.addProperty("item", aItemId);
        tBody.add("result", tResult);
        return new net.minecraft.data.recipes.FinishedRecipe() {
            @Override
            public void serializeRecipeData(JsonObject aJson) {
                for (java.util.Map.Entry<String, com.google.gson.JsonElement> tEntry : tBody.entrySet())
                    aJson.add(tEntry.getKey(), tEntry.getValue());
            }

            @Override
            public net.minecraft.resources.ResourceLocation getId() {
                return new net.minecraft.resources.ResourceLocation("gt6", aId.substring(4));
            }

            @Override
            public net.minecraft.world.item.crafting.RecipeSerializer<?> getType() {
                return net.minecraft.world.item.crafting.RecipeSerializer.SHAPED_RECIPE;
            }

            @Override
            public JsonObject serializeAdvancement() {
                return null;
            }

            @Override
            public net.minecraft.resources.ResourceLocation getAdvancementId() {
                return null;
            }
        };
    }
    //?} else {
    /*// 21.1: no gt6 recipe rows — the wrapper face is forge-only (FORGE_GATED_ONLY_CANONICAL).
    *///?}

    // ---- the committed-tree law: every shipped JSON answers the seeded-narrow live universe ----

    /** The mdk project root, walking up from the (leg-dependent) test working dir (the boiler-card anchor). */
    private static Path mdkRoot() {
        for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent())
            if (Files.isRegularFile(p.resolve("tools").resolve("gen_textures.py"))) return p;
        throw new AssertionError("mdk root (tools/gen_textures.py) not found upward from "
                + Path.of("").toAbsolutePath());
    }

    /** Every *.json under the gt6 data dir, both the 1.20.1 and the 1.21 directory spellings. */
    private static Stream<Path> shippedJson(String aSubDir) throws IOException {
        Path tData = mdkRoot().resolve("src/generated/resources/data/gt6");
        Stream<Path> rStream = Stream.empty();
        for (String tSpelling : new String[]{aSubDir, aSubDir.endsWith("s") ? aSubDir.substring(0, aSubDir.length() - 1) : aSubDir + "s"})
            if (Files.isDirectory(tData.resolve(tSpelling)))
                rStream = Stream.concat(rStream, Files.walk(tData.resolve(tSpelling))
                        .filter(p -> p.toString().endsWith(".json")));
        return rStream;
    }

    /** The modids a shipped row DECLARES via the two loader condition keys, mod_loaded entries only. */
    private static List<String> declaredDomains(JsonObject aRow) {
        java.util.TreeSet<String> tSet = new java.util.TreeSet<>();
        for (String tKey : new String[]{"conditions", "neoforge:conditions"}) {
            if (!aRow.has(tKey) || !aRow.get(tKey).isJsonArray()) continue;
            for (var tElement : aRow.getAsJsonArray(tKey)) {
                if (!tElement.isJsonObject()) continue;
                JsonObject tCondition = tElement.getAsJsonObject();
                if (tCondition.has("type") && tCondition.get("type").getAsString().contains("mod_loaded")
                        && tCondition.has("modid")) tSet.add(tCondition.get("modid").getAsString());
            }
        }
        return new java.util.ArrayList<>(tSet);
    }

    /**
     * THE row↔condition law over every committed recipe row (task
     * parse-errors-registration-convergence pin ④): a row's declared mod_loaded domains must
     * equal exactly the gated domains of the gt6 ids it references — a missing condition ships
     * a dangling id to the bare install (the 12018 parse errors), a vacuous condition drops a
     * live row for nothing. Run AFTER runData: the pin audits the committed tree.
     */
    @Test
    public void everyShippedRecipeRowDeclaresExactlyItsGatedDomains() throws IOException {
        int tRecipeRows = 0;
        int tConditionedRows = 0;
        for (Path tFile : shippedJson("recipes").toList()) {
            JsonObject tRow = com.google.gson.JsonParser.parseString(Files.readString(tFile)).getAsJsonObject();
            List<String> tNeeded = GT6ForeignRowConvergence.rowDomains(tRow);
            List<String> tDeclared = declaredDomains(tRow);
            assertEquals(tNeeded, tDeclared, tFile.toString());
            tRecipeRows++;
            if (!tDeclared.isEmpty()) tConditionedRows++;
        }
        assertTrue(tRecipeRows > 60000, "the walk saw both directory spellings: " + tRecipeRows);
        assertTrue(tConditionedRows > 1000, "the convergence coverage exists: " + tConditionedRows);
    }

    /**
     * The loot-side law (the B5 arm): loot tables have no load-time condition mechanism, so
     * NO committed loot JSON may reference a gated gt6 id — those rows are skipped at datagen
     * (the upstream bare shape), never shipped dangling.
     */
    @Test
    public void noShippedLootRowReferencesAGatedId() throws IOException {
        int tLootRows = 0;
        for (String tDir : new String[]{"loot_tables", "loot_modifiers"})
            for (Path tFile : shippedJson(tDir).toList()) {
                JsonObject tRow = com.google.gson.JsonParser.parseString(Files.readString(tFile)).getAsJsonObject();
                assertTrue(GT6ForeignRowConvergence.rowDomains(tRow).isEmpty(), tFile.toString());
                tLootRows++;
            }
        assertTrue(tLootRows > 15000, "the loot walk saw the shipped domains: " + tLootRows);
    }
}
