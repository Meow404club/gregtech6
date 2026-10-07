/**
 * Offline pin for task 40-41-mold-assets (GitHub #40 + #41) — the mold/crucible asset
 * family carries the per-shape borrows and the material smooth bodies. Two live gaps
 * pinned here:
 * <ol>
 * <li>#40 — the 31 raw clay mold items all rode {@code minecraft:block/clay} as layer0
 *     (every shape looked identical). Each raw item model now points at its OWN borrowed
 *     upstream icon ({@code gt6:item/<path>_raw}, the gt.multiitem.randomtools
 *     900-929/991 borrows, assets/README.md) and the PNG must exist on the classpath —
 *     31 distinct icons, zero shared sprites, the vanilla clay reference is gone.</li>
 * <li>#41 — the formed mold/faucet/crucible block bodies all rode the flat
 *     andesite/cobble placeholder. Every body now references the material smooth art per
 *     the upstream getTextureSmooth face (GT6CrucibleDatagen.bodyTexture): the Ceramic
 *     family the rough blockSolid borrow, Bronze the copper set, Steel the metallic set,
 *     the Stone rows the vanilla smooth stone. The 5x5 shape geometry stays the declared
 *     geometry-card defer (this pin guards the TEXTURE face only).</li>
 * </ol>
 * Reads the committed generated tree on the classpath (the
 * {@link GT6RailItemModelDatagenTest} form — no datagen run) and derives the universe
 * from the registration rows, so a new ceramic row joins the pin automatically.
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.registry.GT6Crucibles;
import gregtech6.registry.GT6Molds;
import gregtech6.tileentity.GTOfflineTestBase;

public class GT6MoldAssetDatagenTest extends GTOfflineTestBase {

    /** The material smooth bodies (GT6CrucibleDatagen.bodyTexture) — pinned literally, NOT derived from production code (a tautology pins nothing). */
    private static final String CERAMIC_BODY = "gt6:block/materialicons/rough/block_solid";
    private static final String STONE_BODY = "minecraft:block/smooth_stone";
    private static final String BRONZE_BODY = "gt6:block/materialicons/copper/block_solid";
    private static final String STEEL_BODY = "gt6:block/materialicons/metallic/block_solid";

    private static JsonObject generatedJson(String aPath) throws Exception {
        try (InputStream tStream = GT6MoldAssetDatagenTest.class.getClassLoader().getResourceAsStream(aPath)) {
            assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
    }

    private static void assertExists(String aPath) {
        assertNotNull(GT6MoldAssetDatagenTest.class.getClassLoader().getResource(aPath),
                "the borrowed asset must be on the classpath: " + aPath);
    }

    /** All 31 raw clay items: item/generated over their own borrowed icon, no shared sprite, clay gone. */
    @Test
    void rawItemModelsPointAtTheirOwnBorrowedIcons() throws Exception {
        List<GT6Molds.MoldRow> tRows = new ArrayList<>(GT6Molds.CERAMIC_ROWS);
        tRows.add(GT6Molds.CERAMIC_BLANK_ROW);
        assertEquals(31, tRows.size(), "the ceramic universe walk broke — never pass vacuously");

        Set<String> tLayer0s = new HashSet<>();
        for (GT6Molds.MoldRow tRow : tRows) {
            JsonObject tModel = generatedJson("assets/gt6/models/item/" + tRow.path() + "_raw.json");
            assertEquals("minecraft:item/generated", tModel.get("parent").getAsString(),
                    tRow.path() + "_raw: the vanilla item/generated parent");
            JsonObject tTextures = tModel.getAsJsonObject("textures");
            assertEquals(1, tTextures.size(), tRow.path() + "_raw: exactly the layer0 texture");
            String tLayer0 = tTextures.get("layer0").getAsString();
            assertEquals("gt6:item/" + tRow.path() + "_raw", tLayer0,
                    tRow.path() + "_raw: its own borrowed icon");
            assertFalse(tLayer0.contains("minecraft:block/clay"),
                    tRow.path() + "_raw: the shared vanilla clay sprite must stay dead (#40)");
            assertExists("assets/gt6/textures/" + tLayer0.substring("gt6:".length()) + ".png");
            tLayer0s.add(tLayer0);
        }
        assertEquals(31, tLayer0s.size(), "each raw mold must point at a DISTINCT icon (#40)");
    }

    /** The 32-icon family: the 31 raw mold borrows plus the p38-c1 faucet raw borrow. */
    @Test
    void theBorrowedIconFamilyIsComplete() {
        List<String> tIcons = new ArrayList<>();
        for (GT6Molds.MoldRow tRow : GT6Molds.CERAMIC_ROWS) tIcons.add(tRow.path() + "_raw");
        tIcons.add(GT6Molds.CERAMIC_BLANK_ROW.path() + "_raw");
        tIcons.add("faucet_ceramic_raw");
        assertEquals(32, tIcons.size(), "the icon family walk broke — never pass vacuously");
        for (String tIcon : tIcons) assertExists("assets/gt6/textures/item/" + tIcon + ".png");
    }

    /** The formed ceramic mold bodies: the rough blockSolid borrow on all 31 + the stone rung on vanilla smooth stone. */
    @Test
    void formedMoldBodiesAreTheMaterialSmoothReferences() throws Exception {
        assertFalse(GT6Molds.CERAMIC_ROWS.isEmpty(), "the row walk broke — never pass vacuously");
        for (GT6Molds.MoldRow tRow : GT6Molds.CERAMIC_ROWS) {
            assertBody("assets/gt6/models/block/" + tRow.path() + ".json", CERAMIC_BODY, tRow.path());
        }
        assertBody("assets/gt6/models/block/" + GT6Molds.CERAMIC_BLANK_ROW.path() + ".json",
                CERAMIC_BODY, GT6Molds.CERAMIC_BLANK_ROW.path());
        assertBody("assets/gt6/models/block/mold_stone.json", STONE_BODY, "mold_stone");
    }

    /**
     * The faucet bodies ride their material (task faucet-material-rows: the full 39-row
     * walk — each model's body texture is exactly what {@code GT6CrucibleDatagen
     * .bodyTexture} answers for the row's material, the shared crucible/mold dispatch;
     * stone → vanilla smooth stone, the rest → the set borrow or the shared rough).
     */
    @Test
    void faucetBodiesAreTheMaterialSmoothReferences() throws Exception {
        gregtech6.registry.GT6MaterialTestSupport.materials(); // the row materials resolve their texture sets
        assertFalse(GT6Molds.FAUCET_ROWS.isEmpty(), "the faucet walk broke — never pass vacuously");
        assertEquals(39, GT6Molds.FAUCET_ROWS.size(), "the full Loader:300-341 projection (task faucet-material-rows)");
        for (GT6Molds.FaucetRow tRow : GT6Molds.FAUCET_ROWS) {
            String tExpected = gregtech6.datagen.GT6CrucibleDatagen.bodyTexture(tRow.material().get());
            assertBody("assets/gt6/models/block/" + tRow.path() + ".json", tExpected, tRow.path());
        }
    }

    /** The small crucible empty faces: stone/ceramic/bronze/steel each their material smooth body (#41, +C2). */
    @Test
    void crucibleEmptyBodiesAreTheMaterialSmoothReferences() throws Exception {
        assertEquals(4, GT6Crucibles.ROWS.size(), "the crucible walk broke — never pass vacuously");
        assertBody("assets/gt6/models/block/smeltery_stone_empty.json", STONE_BODY, "smeltery_stone_empty");
        assertBody("assets/gt6/models/block/smeltery_ceramic_empty.json", CERAMIC_BODY, "smeltery_ceramic_empty");
        assertBody("assets/gt6/models/block/smeltery_bronze_empty.json", BRONZE_BODY, "smeltery_bronze_empty");
        assertBody("assets/gt6/models/block/smeltery_steel_empty.json", STEEL_BODY, "smeltery_steel_empty");
    }

    /**
     * The body pin: the model's single body texture key must be exactly the expected
     * reference. Two carrier forms coexist (the mold-geometry rebase seam): the
     * cube_all rows (faucets, crucible empties) pin "all", the 32 bitmap-stamped molds
     * (stone + ceramic, issue #41 — the stone rung retired its flat-cube placeholder and
     * rides the same concave MTE design 1072) pin "body" — the material smooth face and
     * the cobble-stays-dead pin are form-independent.
     */
    private static void assertBody(String aModelPath, String aExpectedBody, String aLabel) throws Exception {
        JsonObject tModel = generatedJson(aModelPath);
        JsonObject tTextures = tModel.getAsJsonObject("textures");
        String tKey = tTextures.has("all") ? "all" : "body";
        assertTrue(tTextures.has(tKey), aLabel + ": the body texture key (cube_all or the bitmap stamp)");
        assertFalse(aExpectedBody.endsWith("/andesite/cobble"),
                aLabel + ": the flat cobble placeholder must stay dead (#41)");
        assertEquals(aExpectedBody, tTextures.get(tKey).getAsString(),
                aLabel + ": the material smooth body");
    }
}
