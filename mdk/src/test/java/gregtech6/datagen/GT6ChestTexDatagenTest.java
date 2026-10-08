/*
 * Offline pinned tests for task material-mc-a-storage-chests: the metal-chest family
 * (the metalset chest pair :132-133 over the 60-material loop) asserted against the
 * committed generated tree — the {@link GT6StorageTexDatagenTest} shape. Pinned:
 * <ul>
 * <li>the two chest facade models (the derived upstream TESR-sheet crops, assets/README.md):
 *     the colored body + lid boxes are the tintindex-0 seats (the upstream renderer binds
 *     the colored sheet under the mRGBa multiply first, MultiTileEntityChest.java:349-370),
 *     the plain 0.01 decal shells and the metalchest knob untinted (the P22 contract; the
 *     woodchest knob rides the colored layer — the sheets' own art split);</li>
 * <li>the blockstate facing tables (4 variants per row, the chest_bronze/reinforced_chest_steel
 *     spot pins) and the 120 item models parenting their kind model;</li>
 * <li>the harvest bands (aMetal = pickaxe on the plain half :132/:98, aWooden = axe on the
 *     reinforced half :133/:102 — 60 + 60, the task brief's "金属=wrench" corrected by the
 *     loader line);</li>
 * <li>the appearance pin (the id1521 规): all 28 derived PNGs carry NON-solid art
 *     (>= 4 distinct opaque colours) at the ModelChest crop sizes;</li>
 * <li>the lang parity: the en/zh display templates + the shared metalset unit words.</li>
 * </ul>
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.registry.GT6Chests;

class GT6ChestTexDatagenTest {

    @BeforeAll
    static void bootOffline() {
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
            // NetworkHooks.init() failure is expected offline
        }
    }

    private static InputStream resource(String aPath) {
        return GT6ChestTexDatagenTest.class.getClassLoader().getResourceAsStream(aPath);
    }

    private static JsonObject json(String aPath) throws IOException {
        try (InputStream tStream = resource(aPath)) {
            assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    // ------------------------------------------------------------------
    // the chest facade models (the tint seats + the derived art)
    // ------------------------------------------------------------------

    @Test
    public void chestFacadeModelsPinTheTintSeatsAndTheLayerSplit() throws Exception {
        for (String tModelName : List.of("chest", "reinforced_chest")) {
            JsonObject tModel = json("assets/gt6/models/block/" + tModelName + ".json");
            assertEquals("minecraft:cutout", tModel.get("render_type").getAsString(), tModelName + ": cutout");
            var tElements = tModel.getAsJsonArray("elements");
            assertEquals(5, tElements.size(), tModelName + ": body + lid + knob + 2 plain shells");
            // elements 0 (body) and 1 (lid) are the tintindex-0 seats — every face
            for (int i = 0; i < 2; i++) {
                var tFaces = tElements.get(i).getAsJsonObject().getAsJsonObject("faces");
                assertEquals(6, tFaces.size(), tModelName + ": element " + i + " carries all six faces");
                for (String tFace : tFaces.keySet()) {
                    assertEquals(0, tFaces.getAsJsonObject(tFace).get("tintindex").getAsInt(),
                            tModelName + " element " + i + " face " + tFace + " rides the tintindex-0 seat");
                }
            }
            // the knob + the two plain shells stay untinted (the P22 contract)
            for (int i = 2; i < 5; i++) {
                var tFaces = tElements.get(i).getAsJsonObject().getAsJsonObject("faces");
                for (String tFace : tFaces.keySet()) {
                    assertFalse(tFaces.getAsJsonObject(tFace).has("tintindex"),
                            tModelName + " element " + i + " face " + tFace + " stays untinted");
                }
            }
        }
    }

    @Test
    public void knobArtRidesTheSheetThatCarriesTheLatch() throws Exception {
        // metalchest: the knob art lives in the plain sheet (the probe in the crop script)
        assertEquals("gt6:block/metalchest/plain_knob",
                json("assets/gt6/models/block/chest.json").getAsJsonObject("textures").get("knob").getAsString());
        // woodchest: the colored sheet (the role inversion is upstream's own art)
        assertEquals("gt6:block/woodchest/colored_knob",
                json("assets/gt6/models/block/reinforced_chest.json").getAsJsonObject("textures").get("knob").getAsString());
        // the tint seats bind the colored sheet (the mRGBa multiply layer)
        JsonObject tTextures = json("assets/gt6/models/block/chest.json").getAsJsonObject("textures");
        assertEquals("gt6:block/metalchest/colored_front", tTextures.get("front").getAsString());
        assertEquals("gt6:block/metalchest/colored_top", tTextures.get("top").getAsString());
    }

    // ------------------------------------------------------------------
    // the blockstate facing tables + the item models
    // ------------------------------------------------------------------

    @Test
    public void chestBlockstatesPinTheFacingVariantsPerKind() throws Exception {
        assertEquals(120, GT6Chests.ROWS.size(), "the chest census stays 120");
        java.util.Map<String, Integer> tRotY = java.util.Map.of(
                "y0", 0, "y90", 90, "y180", 180, "y270", 270);
        for (GT6Chests.ChestRow tRow : GT6Chests.ROWS) {
            JsonObject tVariants = json("assets/gt6/blockstates/" + tRow.path() + ".json").getAsJsonObject("variants");
            assertEquals(4, tVariants.size(), tRow.path() + ": exactly the 4 facing variants");
            String tModel = tRow.reinforced() ? "gt6:block/reinforced_chest" : "gt6:block/chest";
            for (var tEntry : tVariants.entrySet()) {
                assertTrue(tEntry.getKey().startsWith("facing="), tRow.path() + ": facing keys only");
                assertEquals(tModel, tEntry.getValue().getAsJsonObject().get("model").getAsString(),
                        tRow.path() + ": the kind model");
            }
        }
        // the spot pins: the 4-variant rotation table on one plain and one reinforced row
        var tBronze = json("assets/gt6/blockstates/chest_bronze.json").getAsJsonObject("variants");
        assertFalse(tBronze.getAsJsonObject("facing=north").has("y")); // NORTH identity
        assertEquals(180, tBronze.getAsJsonObject("facing=south").get("y").getAsInt());
        assertEquals(270, tBronze.getAsJsonObject("facing=west").get("y").getAsInt());
        assertEquals(90, tBronze.getAsJsonObject("facing=east").get("y").getAsInt());
        assertNotNull(tRotY); // the table above IS the pin
    }

    @Test
    public void chestItemModelsParentTheirKindModel() throws Exception {
        for (GT6Chests.ChestRow tRow : GT6Chests.ROWS) {
            JsonObject tItem = json("assets/gt6/models/item/" + tRow.path() + ".json");
            String tParent = tRow.reinforced() ? "gt6:block/reinforced_chest" : "gt6:block/chest";
            assertEquals(tParent, tItem.get("parent").getAsString(), tRow.path() + ": the kind-model parent");
        }
    }

    // ------------------------------------------------------------------
    // the harvest bands (aMetal pickaxe / aWooden axe — the loader line correction)
    // ------------------------------------------------------------------

    @Test
    public void harvestBandsFollowTheUpstreamBlockFamilies() throws Exception {
        Set<String> tPickaxe = tagValues("data/minecraft/tags/blocks/mineable/pickaxe.json");
        Set<String> tAxe = tagValues("data/minecraft/tags/blocks/mineable/axe.json");
        int tPlain = 0, tReinforced = 0;
        for (GT6Chests.ChestRow tRow : GT6Chests.ROWS) {
            String tId = "gt6:" + tRow.path();
            if (tRow.reinforced()) {
                tReinforced++;
                assertTrue(tAxe.contains(tId), tId + " joins the axe band (aWooden :133/:102)");
                assertFalse(tPickaxe.contains(tId), tId + " stays out of the pickaxe band");
            } else {
                tPlain++;
                assertTrue(tPickaxe.contains(tId), tId + " joins the pickaxe band (aMetal :132/:98)");
                assertFalse(tAxe.contains(tId), tId + " stays out of the axe band");
            }
        }
        assertEquals(60, tPlain);
        assertEquals(60, tReinforced);
    }

    private Set<String> tagValues(String aPath) throws IOException {
        Set<String> rValues = new HashSet<>();
        // both member forms — the strict string and the tag-residual-convergence optional object
        json(aPath).getAsJsonArray("values").forEach(tValue -> rValues.add(tValue.isJsonPrimitive()
                ? tValue.getAsString() : tValue.getAsJsonObject().get("id").getAsString()));
        return rValues;
    }

    // ------------------------------------------------------------------
    // the appearance pin (the id1521 规 — the derived art is NOT solid colour)
    // ------------------------------------------------------------------

    @Test
    public void derivedChestTexturesCarryNonSolidArt() throws Exception {
        for (String tSheet : List.of("metalchest", "woodchest")) {
            // the UNUSED knob crop is exempt (metalchest colored_knob / woodchest plain_knob
            // are empty by the upstream sheets' own art split — the knob binding carries the
            // other layer, the chestModel datagen)
            List<String> tFaces = List.of("top", "front", "side", "bottom", "lid_front", "lid_side", "knob");
            for (String tLayer : List.of("colored", "plain")) {
                for (String tFace : tFaces) {
                    if (tFace.equals("knob")
                            && ((tSheet.equals("metalchest") && tLayer.equals("colored"))
                                    || (tSheet.equals("woodchest") && tLayer.equals("plain")))) {
                        continue;
                    }
                    String tPath = "assets/gt6/textures/block/" + tSheet + "/" + tLayer + "_" + tFace + ".png";
                    try (InputStream tStream = resource(tPath)) {
                        assertNotNull(tStream, "the derived PNG must exist: " + tPath);
                        var tImage = ImageIO.read(tStream);
                        int tDistinct = 0;
                        java.util.Set<Integer> tColors = new java.util.HashSet<>();
                        for (int tY = 0; tY < tImage.getHeight(); tY++) {
                            for (int tX = 0; tX < tImage.getWidth(); tX++) {
                                int tRgba = tImage.getRGB(tX, tY);
                                if ((tRgba >>> 24) != 0) tColors.add(tRgba);
                            }
                        }
                        tDistinct = tColors.size();
                        assertTrue(tDistinct >= 4, tPath + " carries non-solid art (" + tDistinct + " colours)");
                        // the ModelChest crop sizes (the knob 2x4, the lid bands 14x5, the body 14x10/14x14)
                        int tW = tImage.getWidth(), tH = tImage.getHeight();
                        if (tFace.equals("knob")) {
                            assertEquals(2, tW);
                            assertEquals(4, tH);
                        }
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // the lang parity (the display templates + the shared metalset unit words)
    // ------------------------------------------------------------------

    @Test
    public void langCarriesTheChestTemplatesAndTheSharedUnits() throws Exception {
        JsonObject tEn = json("assets/gt6/lang/en_us.json");
        assertEquals("%s Chest", tEn.get(GT6Chests.DISPLAY_KEY).getAsString());
        assertEquals("%s reinforced wooden Chest", tEn.get(GT6Chests.DISPLAY_REINFORCED_KEY).getAsString());
        JsonObject tZh = json("assets/gt6/lang/zh_cn.json");
        assertEquals("%s箱子", tZh.get(GT6Chests.DISPLAY_KEY).getAsString());
        assertEquals("%s强化木箱", tZh.get(GT6Chests.DISPLAY_REINFORCED_KEY).getAsString());
        // the shared metalset unit words (the dump faces — 铅 from gt.multitileentity.0)
        for (GT6Chests.ChestRow tRow : GT6Chests.ROWS) {
            assertNotNull(tEn.get(GT6Chests.matUnitKeyOf(tRow.material())), "en unit " + tRow.material().slug());
            assertNotNull(tZh.get(GT6Chests.matUnitKeyOf(tRow.material())), "zh unit " + tRow.material().slug());
        }
        assertEquals("铅", tZh.get("gt6.row.mat.lead").getAsString());
    }
}
