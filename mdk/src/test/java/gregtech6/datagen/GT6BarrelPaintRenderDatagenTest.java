/*
 * Offline pinned-count tests for task p23-barrel-paint-render: the generated barrel
 * blockstate/model JSONs carry tintindex 0 on every face of the single full-cube element —
 * the generated-JSON half of the 16 census, asserted against the committed src/generated
 * tree (the GT6MachinePaintRenderDatagenTest shape: the write side is gated by runData,
 * first run written>0, second run written:0; the datagen-JVM counter half is the
 * GT6BlockStates "16 barrel models tinted" log line).
 *
 * <p>Census ground truth: the barrel domain is wood (1) + plastic (1) + bronze metal (1)
 * + logistics (1) + the twelve high-tier metal drums (Loader_MultiTileEntities.java:2159-2170
 * ladder) = 16 blocks — the GTBarrels.paintableBlockArray() registration census, one model
 * each (barrels have no blockstate properties) = 16 block-model JSONs. Upstream canonical:
 * the barrel renders its colored/ texture multiplied by mRGBa (MultiTileEntityBarrelWood
 * .java:42-55; Plastic:42/Metal:39/Logistics:45 isomorphic) — the tintedCubeAll element
 * form IS that layer over the full-grayscale placeholder PNGs (declared deviation: the
 * upstream overlay/ UNCOLOURED decal layer stays pooled, v1 tints the whole barrel).
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class GT6BarrelPaintRenderDatagenTest {

    /**
     * The 16 barrel-domain bases, in registration order (the GTBarrels.paintableBlockArray
     * census): the four standalone rows, then the twelve high-tier drums in their ladder
     * order (the METAL_DRUM_BLOCKS LinkedHashMap order).
     */
    private static final List<String> BARREL_BASES = List.of(
            "barrel_wood", "barrel_plastic", "barrel_metal", "barrel_logistics",
            "barrel_tungsten_alloy", "barrel_titanium", "barrel_netherite",
            "barrel_tungstensteel", "barrel_tungsten", "barrel_void_metal",
            "barrel_tantalum_hafnium_carbide", "barrel_gaia_spirit",
            "barrel_adamantium", "barrel_draconium", "barrel_awakened_draconium",
            "barrel_infinity");

    /** The twelve high-tier drums share the ONE barrel_metal.png (the p7 shared-PNG form). */
    private static final Set<String> DRUM_BASES = Set.of(
            "barrel_tungsten_alloy", "barrel_titanium", "barrel_netherite",
            "barrel_tungstensteel", "barrel_tungsten", "barrel_void_metal",
            "barrel_tantalum_hafnium_carbide", "barrel_gaia_spirit",
            "barrel_adamantium", "barrel_draconium", "barrel_awakened_draconium",
            "barrel_infinity");

    private static final List<String> FACE_KEYS = List.of("down", "up", "north", "south", "west", "east");

    private static JsonObject json(String aPath) throws Exception {
        try (InputStream tStream = GT6BarrelPaintRenderDatagenTest.class.getClassLoader()
                .getResourceAsStream(aPath)) {
            assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    /** The census shape: 16 barrel blocks, one model each = 16 tinted block models. */
    @Test
    void pinnedBarrelPaintCensus() {
        assertEquals(16, BARREL_BASES.size(), "the barrel block census (paintableBlockArray)");
        assertEquals(12, DRUM_BASES.size(), "the high-tier drum sub-census (the :2159-2170 ladder)");
    }

    /**
     * Every barrel block model: the block/block parent, the two-texture key set (all +
     * particle), ONE full 0..16 element whose six faces each carry #all + tintindex 0 +
     * cullface — the tintedCubeAll form (the p21 machine grammar over the barrel's
     * ONE-texture shortcut). The drums share barrel_metal.png; every other row its own PNG.
     */
    @Test
    void everyBarrelModelCarriesTintIndexZeroOnAllSixFaces() throws Exception {
        for (String tBase : BARREL_BASES) {
            String tTexture = DRUM_BASES.contains(tBase) ? "barrel_metal" : tBase;
            JsonObject tModel = json("assets/gt6/models/block/" + tBase + ".json");
            assertEquals("minecraft:block/block", tModel.get("parent").getAsString(),
                    tBase + ": the block/block parent (display transforms + particle binding)");
            var tTextures = tModel.getAsJsonObject("textures");
            assertEquals("gt6:block/" + tTexture, tTextures.get("all").getAsString(),
                    tBase + ": its (possibly shared, the p7 drum form) placeholder PNG");
            assertEquals("#all", tTextures.get("particle").getAsString(),
                    tBase + ": particle bound to #all");
            assertEquals(Set.of("all", "particle"), tTextures.keySet(),
                    tBase + ": the two-texture key set");
            var tElements = tModel.getAsJsonArray("elements");
            assertEquals(1, tElements.size(), tBase + ": ONE element (the tintedCubeAll shortcut)");

            JsonObject tCube = tElements.get(0).getAsJsonObject();
            assertEquals(0.0, tCube.getAsJsonArray("from").get(0).getAsDouble(),
                    tBase + ": from = the full 0..16 cube");
            assertEquals(16.0, tCube.getAsJsonArray("to").get(0).getAsDouble(),
                    tBase + ": to = the full 0..16 cube");
            var tFaces = tCube.getAsJsonObject("faces");
            assertEquals(6, tFaces.size(), tBase + ": six faces");
            for (String tFaceKey : FACE_KEYS) {
                JsonObject tFace = tFaces.getAsJsonObject(tFaceKey);
                assertEquals("#all", tFace.get("texture").getAsString(),
                        tBase + " face " + tFaceKey + ": the shared #all key");
                assertEquals(0, tFace.get("tintindex").getAsInt(),
                        tBase + " face " + tFaceKey + ": tintindex 0 — the paint tint seat");
                assertEquals(tFaceKey, tFace.get("cullface").getAsString(),
                        tBase + " face " + tFaceKey + ": the vanilla cube cullface");
            }
        }
    }

    /** Every barrel blockstate: the plain single-variant form over exactly its block model. */
    @Test
    void barrelBlockstatesWireExactlyTheirModel() throws Exception {
        for (String tBase : BARREL_BASES) {
            JsonObject tState = json("assets/gt6/blockstates/" + tBase + ".json");
            assertTrue(tState.has("variants"), tBase + ": the plain-variants form (no multipart)");
            var tVariants = tState.getAsJsonObject("variants");
            assertEquals(1, tVariants.size(), tBase + ": barrels have no blockstate properties");
            JsonObject tRow = tVariants.getAsJsonObject("");
            assertNotNull(tRow, tBase + ": the empty-property variant key");
            assertEquals("gt6:block/" + tBase, tRow.get("model").getAsString(),
                    tBase + ": the variant wires exactly the block model");
        }
    }

    /** Every barrel item model parents the block model and carries no tintindex itself (the inventory half is the ItemColor registration, not model data). */
    @Test
    void barrelItemModelsParentBlockModelsWithoutOwnTint() throws Exception {
        for (String tBase : BARREL_BASES) {
            JsonObject tItem = json("assets/gt6/models/item/" + tBase + ".json");
            assertEquals("gt6:block/" + tBase, tItem.get("parent").getAsString(),
                    tBase + ": the item parent");
            assertTrue(!tItem.toString().contains("tintindex"),
                    tBase + ": no own tint — the inventory half is the ItemColor registration");
        }
    }
}
