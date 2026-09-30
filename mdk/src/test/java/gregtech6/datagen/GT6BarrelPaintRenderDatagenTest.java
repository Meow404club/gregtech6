/*
 * Offline pinned-count tests for task barrel-paint-render: the generated barrel
 * blockstate/model JSONs carry the tintindex-0 paint seat — the generated-JSON half of
 * the 16 census, asserted against the committed src/generated tree (the
 * GT6MachinePaintRenderDatagenTest shape: the write side is gated by runData, first run
 * written>0, second run written:0; the datagen-JVM counter half is the GT6BlockStates
 * "16 barrel models tinted" log line).
 *
 * <p>Census ground truth: the barrel domain is wood (1) + plastic (1) + bronze metal (1)
 * + logistics (1) + the twelve high-tier metal drums (Loader_MultiTileEntities.java:2159-2170
 * ladder) = 16 blocks — the GTBarrels.paintableBlockArray() registration census, one model
 * each (barrels have no blockstate properties) = 16 block-model JSONs. Upstream canonical:
 * the barrel renders its colored/ texture multiplied by mRGBa (MultiTileEntityBarrelWood
 * .java:42-55; Plastic:42/Metal:39/Logistics:45 isomorphic). Task tex-tank-family:
 * the 15 borrowable rows ride the two-layer per-face barrel_parts grammar (the p23
 * "overlay stays pooled" deviation is RETIRED — the overlay decal shells landed); the
 * p12 logistics row keeps the tintedCubeAll single-element form (its borrow deferred).
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
     * Every barrel block model carries the tintindex-0 seat on its body faces. Task
     * tex-tank-family: the 15 borrowable rows ride the two-layer per-face
     * barrel_parts form (body + six decal shells — the body faces tinted, the decals
     * untinted; the drums share the drum family set, every other two-layer row its own);
     * the p12 logistics row keeps the tintedCubeAll single-element form. The full
     * two-layer shape pins live in {@link GT6TankFamilyPaintRenderDatagenTest}.
     */
    @Test
    void everyBarrelModelCarriesTintIndexZeroOnAllSixFaces() throws Exception {
        for (String tBase : BARREL_BASES) {
            JsonObject tModel = json("assets/gt6/models/block/" + tBase + ".json");
            var tElements = tModel.getAsJsonArray("elements");
            if (tBase.equals("barrel_logistics")) {
                // the declared single-layer exemption (the p12 row, out of the r8 borrow's scope)
                assertEquals(1, tElements.size(), tBase + ": ONE element (the tintedCubeAll shortcut)");
            } else {
                String tFamily = DRUM_BASES.contains(tBase) ? "drum"
                        : "barrel_wood".equals(tBase) ? "barrel"
                        : "barrel_metal".equals(tBase) ? "drum" : "plasticcan";
                var tTextures = tModel.getAsJsonObject("textures");
                String tBand = "gt6:block/barrel_parts/" + tFamily + "/";
                assertEquals(tBand + "colored_bottom", tTextures.get("down").getAsString(), tBase + ": the bottom borrow");
                assertEquals(tBand + "colored_top", tTextures.get("up").getAsString(), tBase + ": the top borrow");
                assertEquals(tBand + "colored_side", tTextures.get("north").getAsString(), tBase + ": the side borrow");
                assertEquals(7, tElements.size(), tBase + ": body + 6 decal shells (the two-layer form)");
            }
            var tFaces = tElements.get(0).getAsJsonObject().getAsJsonObject("faces");
            for (String tFaceKey : FACE_KEYS) {
                assertEquals(0, tFaces.getAsJsonObject(tFaceKey).get("tintindex").getAsInt(),
                        tBase + " face " + tFaceKey + ": tintindex 0 — the paint tint seat");
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
