/**
 * Offline pin for issue #45 C2 — the small-crucible CERAMIC row and its raw clay chain.
 * Three faces pinned:
 * <ol>
 * <li>the row exists as data: {@code smeltery_ceramic} rides {@link gregtech6.registry.GT6Crucibles#ROWS}
 *     with the Loader_MultiTileEntities.java:256 verbatim stat pair (hardness 5.0/5.0, the
 *     non-hidden row upstream carries NO crafting pattern — the furnace-hardened raw is its
 *     only acquisition), and {@link gregtech6.registry.GT6Crucibles#CLAY_CRUCIBLE_RAW} is
 *     registered beside it (the upstream IL.Ceramic_Crucible_Raw, meta 989);</li>
 * <li>the bodyTexture Ceramic branch answers for the new row's empty face (the #40-41
 *     rough blockSolid borrow — the branch predated the row, this pin ties them);</li>
 * <li>the recipe chain closes over the committed generated tree: 7 clay → raw (shaped, the
 *     MultiItemRandomTools.java:125 row with the k/R tool marks cut — the GT6MoldDatagen
 *     family form), raw → 7 clay (the :113 reverse), raw → smeltery_ceramic (the Loader
 *     :256 RM.add_smelting tail) — plus the icon borrow, the item model and the lang keys.</li>
 * </ol>
 * Reads the committed generated tree on the classpath (the {@link GT6MoldAssetDatagenTest}
 * form — no datagen run).
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.registry.GT6Crucibles;
import gregtech6.tileentity.GTOfflineTestBase;

public class GT6ClayCrucibleDatagenTest extends GTOfflineTestBase {

    /** The upstream :256 stat pair verbatim. */
    private static final float CERAMIC_HARDNESS = 5.0F;

    private static JsonObject generatedJson(String aPath) throws Exception {
        try (InputStream tStream = GT6ClayCrucibleDatagenTest.class.getClassLoader().getResourceAsStream(aPath)) {
            assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
    }

    private static void assertExists(String aPath) {
        assertNotNull(GT6ClayCrucibleDatagenTest.class.getClassLoader().getResource(aPath),
                "the asset must be on the classpath: " + aPath);
    }

    /** The Ceramic rung exists as data — the :256 verbatim row between stone and bronze. */
    @Test
    void ceramicRowSitsInTheLadder() {
        assertEquals(4, GT6Crucibles.ROWS.size(), "stone/ceramic/bronze/steel — never pass vacuously");
        GT6Crucibles.SmelteryRow tRow = rowByPath("smeltery_ceramic");
        assertNotNull(tRow, "the smeltery_ceramic row must be registered");
        assertEquals(CERAMIC_HARDNESS, tRow.hardness(), "the :256 NBT_HARDNESS/NBT_RESISTANCE pair verbatim");
        assertEquals(1, GT6Crucibles.ROWS.indexOf(tRow), "after the stone family, before the metals (the upstream :251-:265 order)");
        // the RegistryObject handle only (the registry is not booted in the bare JVM —
        // .get() would throw; the id pin is the offline face)
        assertEquals("clay_crucible_raw", GT6Crucibles.CLAY_CRUCIBLE_RAW.getId().getPath(),
                "the raw item (upstream meta 989) is registered");
        // the shell material itself stays unpinned here — MT statics are unbooted in the
        // bare JVM; the generated-model pin below proves the Ceramic branch fired (the
        // bodyTexture Ceramic branch is the ONLY mapping answering the rough borrow)
    }

    private static GT6Crucibles.SmelteryRow rowByPath(String aPath) {
        for (GT6Crucibles.SmelteryRow tRow : GT6Crucibles.ROWS) {
            if (tRow.path().equals(aPath)) return tRow;
        }
        return null;
    }

    /** The bodyTexture Ceramic branch fired for the new row — pinned on the generated output (the bare JVM cannot dereference MT statics). */
    @Test
    void bodyTextureCeramicBranchAnswersForTheNewRow() throws Exception {
        JsonObject tModel = generatedJson("assets/gt6/models/block/smeltery_ceramic_empty.json");
        assertEquals("gt6:block/materialicons/rough/block_solid", tModel.getAsJsonObject("textures").get("all").getAsString(),
                "the generated empty cube rides the Ceramic branch (the rough blockSolid borrow)");
        JsonObject tBlockstate = generatedJson("assets/gt6/blockstates/smeltery_ceramic.json");
        assertEquals(9, tBlockstate.getAsJsonObject("variants").size(),
                "the 9 LIQUID_LEVEL variants like every rung");
    }

    /** The recipe chain closes: 7 clay → raw → (furnace) → the ceramic smeltery item, + the reverse. */
    @Test
    void recipeChainCloses() throws Exception {
        // the shaped raw (MultiItemRandomTools.java:125, the k/R tool marks cut — 7 clay)
        JsonObject tShaped = generatedJson("data/gt6/recipes/clay_crucible_raw.json");
        assertEquals("minecraft:crafting_shaped", tShaped.get("type").getAsString());
        JsonObject tKey = tShaped.getAsJsonObject("key").getAsJsonObject("C");
        assertEquals("minecraft:clay_ball", tKey.get("item").getAsString());
        int tClay = 0;
        for (var tRow : tShaped.getAsJsonArray("pattern")) {
            for (char c : tRow.getAsString().toCharArray()) if (c == 'C') tClay++;
        }
        assertEquals(7, tClay, "the U*7 amount (:113)");
        assertEquals("gt6:clay_crucible_raw", tShaped.getAsJsonObject("result").get("item").getAsString());

        // the reverse (:113 — raw → 7 clay balls)
        JsonObject tReverse = generatedJson("data/gt6/recipes/clay_crucible_raw_reclaim.json");
        assertEquals("minecraft:crafting_shapeless", tReverse.get("type").getAsString());
        assertEquals("gt6:clay_crucible_raw", tReverse.getAsJsonArray("ingredients").get(0).getAsJsonObject().get("item").getAsString());
        JsonObject tReclaim = tReverse.getAsJsonObject("result");
        assertEquals("minecraft:clay_ball", tReclaim.get("item").getAsString());
        assertEquals(7, tReclaim.get("count").getAsInt(), "the 7-clay refund");

        // the furnace hardening tail (Loader :256 RM.add_smelting)
        JsonObject tSmelt = generatedJson("data/gt6/recipes/smelt_smeltery_ceramic.json");
        assertEquals("minecraft:smelting", tSmelt.get("type").getAsString());
        assertEquals("gt6:clay_crucible_raw", tSmelt.getAsJsonObject("ingredient").get("item").getAsString());
        assertEquals("gt6:smeltery_ceramic", tSmelt.get("result").getAsString());
    }

    /** The icon borrow + item model + lang keys ride the family convention. */
    @Test
    void iconModelAndLangRide() throws Exception {
        assertExists("assets/gt6/textures/item/clay_crucible_raw.png");
        JsonObject tItemModel = generatedJson("assets/gt6/models/item/clay_crucible_raw.json");
        assertEquals("minecraft:item/generated", tItemModel.get("parent").getAsString());
        assertEquals("gt6:item/clay_crucible_raw",
                tItemModel.getAsJsonObject("textures").get("layer0").getAsString(), "its own borrowed icon");
        JsonObject tLang = generatedJson("assets/gt6/lang/en_us.json");
        assertEquals("Ceramic Smeltery", tLang.get("gt6.row.crucible.display.smeltery_ceramic").getAsString());
        assertEquals("Ceramic Crucible (Raw)", tLang.get("item.gt6.clay_crucible_raw").getAsString());
    }
}
