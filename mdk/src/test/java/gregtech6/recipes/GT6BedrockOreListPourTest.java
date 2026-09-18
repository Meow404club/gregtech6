/**
 * Tests for task p31-bedrock-ore-worldgen spec ③: the shipped
 * {@code data/gt6/recipe_maps/bedrockorelist.json} pours into RM.BedrockOreList
 * (GT6RecipeMaps.BEDROCK_ORE_LIST, the upstream RM.java:153 NEI fake-recipe display face)
 * — the row count (46 material rows + 1 generic bedrock row), the pour, and the yield-id
 * mapping (broken stone ore for the 53-axis materials, dust for the out-of-axis ones —
 * the declared fallback face).
 *
 * <p>The GT6QuSmokeRowsPourTest posture: the gt6: ids resolve through the injected
 * resolver seam onto vanilla stand-ins (identity is all the recipe mechanics compare);
 * the vanilla bedrock/cobblestone ids are the real registry entries; a miss is LOUD.
 */
package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.registry.GT6OreBlocks;
import gregtech6.registry.GTMaterialItems;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class GT6BedrockOreListPourTest extends GTRecipesOfflineTestBase {

    private static final Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
    private static final Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

    @BeforeEach
    void freshGeneration() {
        GTMaterialItems.initMaterials();
        GT6RecipeMaps.init();
        GT6RecipeMapJsonLoader.resetForTest();
        // the stand-in resolver: EVERY gt6: item id resolves (the display face is valid iff
        // the ROWS parse — the live id existence is the live pour + census's problem, and
        // the yield-mapping test below pins the id FORMS offline against the walk)
        GT6RecipeMapJsonLoader.sItemResolver = aId -> aId.getNamespace().equals("gt6") ? Items.IRON_INGOT
                : switch (aId.getPath()) {
                    case "bedrock" -> Items.BEDROCK;
                    case "cobblestone" -> Items.COBBLESTONE;
                    default -> Items.AIR;
                };
        GT6RecipeMapJsonLoader.sFluidResolver = aId -> switch (aId.getPath()) {
            case "lubricant" -> Fluids.LAVA; // identity stand-in
            default -> Fluids.EMPTY; // a miss is LOUD (the unregistered-id bad row)
        };
    }

    @AfterEach
    void teardownGeneration() {
        GT6RecipeMapJsonLoader.sItemResolver = sDefaultItems; // restore the live seams (test order is arbitrary)
        GT6RecipeMapJsonLoader.sFluidResolver = sDefaultFluids;
        GT6RecipeMaps.reset(); // the generation hook retires the JSON tracker WITH the maps
    }

    /** Reads the shipped file verbatim and pours it under its map key. */
    private JsonObject pourShipped() throws Exception {
        String tPath = "/data/gt6/recipe_maps/bedrockorelist.json";
        try (InputStream tStream = GT6BedrockOreListPourTest.class.getResourceAsStream(tPath)) {
            assertNotNull(tStream, "the shipped display-row file " + tPath + " rides the test classpath");
            String tJson = new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
            JsonObject tDoc = JsonParser.parseString(tJson).getAsJsonObject();
            GT6RecipeMapJsonLoader.pour(Map.of(new ResourceLocation("gt6", "bedrockorelist"), tDoc));
            return tDoc;
        }
    }

    /** The 47 display rows (46 material rows + 1 generic bedrock row) pour clean into the map. */
    @Test
    void theShippedDisplayRowsPourIntoTheBedrockOreListMap() throws Exception {
        JsonObject tDoc = pourShipped();
        assertEquals(47, tDoc.getAsJsonArray("recipes").size(), "46 material rows + the generic bedrock row");
        assertEquals(47, GT6RecipeMapJsonLoader.pouredCount("bedrockorelist"), "the poured row count");
        assertNotNull(GT6RecipeMapJsonLoader.mapFor("bedrockorelist"), "the whitelist key resolves its map");
        assertNotNull(GT6RecipeMaps.BEDROCK_ORE_LIST, "the RM.BedrockOreList face exists");
        assertEquals(47, GT6RecipeMaps.BEDROCK_ORE_LIST.mRecipeList.size(), "the map holds every display row");
    }

    /**
     * The yield-id mapping: every material row's primary output is the broken stone-ore id
     * when the material sits in the 53-axis ore universe, the dust id otherwise (the
     * declared fallback face) — and the input is always the large bedrock ore of the same
     * material snake.
     */
    @Test
    void yieldMappingFollowsTheAxisUniverse() throws Exception {
        String tPath = "/data/gt6/recipe_maps/bedrockorelist.json";
        InputStream tStream = GT6BedrockOreListPourTest.class.getResourceAsStream(tPath);
        assertNotNull(tStream);
        JsonArray tRows = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8))
                .getAsJsonObject().getAsJsonArray("recipes");

        Set<String> tAxis = GT6OreBlocks.materialAxis().stream()
                .map(aMaterial -> GTMaterialItems.snakeCase(aMaterial.mNameInternal))
                .collect(java.util.stream.Collectors.toSet());
        int tBroken = 0, tDust = 0;
        StringBuilder tMismatch = new StringBuilder();
        for (int i = 0; i < tRows.size() - 1; i++) { // the last row is the generic bedrock row
            JsonObject tRow = tRows.get(i).getAsJsonObject();
            String tInput = tRow.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString();
            assertTrue(tInput.startsWith("gt6:ore_bedrock_"), "row " + i + " input is the large bedrock ore: " + tInput);
            String tSnake = tInput.substring("gt6:ore_bedrock_".length());
            String tYield = tRow.getAsJsonArray("outputs").get(0).getAsJsonObject().get("item").getAsString();
            String tExpect = tAxis.contains(tSnake) ? "gt6:ore_broken_stone_" : "gt6:dust_";
            if (!(tYield.equals(tExpect + tSnake))) {
                tMismatch.append(" row").append(i).append('(').append(tSnake).append("): json=").append(tYield)
                        .append(" axis=").append(tAxis.contains(tSnake)).append(";");
            }
            if (tYield.equals("gt6:ore_broken_stone_" + tSnake)) tBroken++; else tDust++;
            // the secondary faces stay the upstream shape: bedrock dust + cobblestone with the upstream chances
            assertEquals("gt6:dust_bedrock", tRow.getAsJsonArray("outputs").get(1).getAsJsonObject().get("item").getAsString());
            assertEquals(10, tRow.getAsJsonArray("outputs").get(1).getAsJsonObject().get("chance").getAsInt());
            assertEquals("minecraft:cobblestone", tRow.getAsJsonArray("outputs").get(2).getAsJsonObject().get("item").getAsString());
            assertEquals(10000, tRow.getAsJsonArray("outputs").get(2).getAsJsonObject().get("chance").getAsInt());
        }
        assertTrue(tMismatch.length() == 0, "yield-mapping mismatches:" + tMismatch);
        assertEquals(22, tBroken, "the 53-axis rows ride the broken stone ore (gold.a/gold.b both carry MT.Au)");
        assertEquals(24, tDust, "the out-of-axis rows ride the dust fallback");
    }
}
