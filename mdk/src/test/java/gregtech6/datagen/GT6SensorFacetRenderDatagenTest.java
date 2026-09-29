/*
 * Offline pin for task r8-tex-sensors — the 21 sensor families trade the p26/p34/p37
 * src-over front bake (one cube_all texture on all six faces) for the front-bearing
 * two-layer faceted cube: the untinted colored body (front on north = the FACING face,
 * back on south, side on the four flanks — the upstream
 * MultiTileEntitySensor.getTexture2 :218-228 face plan) plus the six 0.01 overlay
 * plates, cutout render type, ZERO tintindex (the rows register NBT-less, Loader
 * :1979-1999, so mRGBa is white = the colored art shows its own colours). Reads the
 * committed generated tree + the borrowed PNGs off the classpath (the
 * {@link GT6RailItemModelDatagenTest} classpath form); the 126-PNG census and the
 * old-flat-bake death watch ride the same walk.
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.registry.GT6Sensors;
import gregtech6.tileentity.GTOfflineTestBase;

public class GT6SensorFacetRenderDatagenTest extends GTOfflineTestBase {

    /** The per-face borrowed layer ids under gt6:block/sensors/<family>/. */
    private static final List<String> LAYER_FACES = List.of(
            "colored_front", "colored_back", "colored_side",
            "overlay_front", "overlay_back", "overlay_side");

    /** The facing → blockstate rotation map (the p26 dispenser band, pinned verbatim). */
    private static final Set<String> FACING_KEYS = Set.of("north", "south", "east", "west", "up", "down");

    private static InputStream resource(String aPath) {
        return GT6SensorFacetRenderDatagenTest.class.getClassLoader().getResourceAsStream(aPath);
    }

    private static JsonObject json(String aPath) throws Exception {
        try (InputStream tStream = resource(aPath)) {
            assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
    }

    /** The png magic head — a non-empty file that is not a PNG must never pass the census. */
    private static void assertPng(String aPath) throws Exception {
        try (InputStream tStream = resource(aPath)) {
            assertNotNull(tStream, "the borrowed PNG must be on the classpath: " + aPath);
            byte[] tHead = tStream.readNBytes(8);
            assertEquals((byte) 0x89, tHead[0], aPath + ": PNG magic");
            assertEquals('P', tHead[1], aPath + ": PNG magic");
            assertEquals('N', tHead[2], aPath + ": PNG magic");
            assertEquals('G', tHead[3], aPath + ": PNG magic");
        }
    }

    private static void scanNoTintindex(JsonElement aElement, String aContext) {
        if (aElement.isJsonObject()) {
            for (var tEntry : aElement.getAsJsonObject().entrySet()) {
                assertFalse(tEntry.getKey().equals("tintindex"),
                        aContext + ": the NBT=null ruling keeps the model tint-free");
                scanNoTintindex(tEntry.getValue(), aContext);
            }
        } else if (aElement.isJsonArray()) {
            for (JsonElement tItem : aElement.getAsJsonArray()) {
                scanNoTintindex(tItem, aContext);
            }
        }
    }

    // ------------------------------------------------------------------
    // the 126-PNG census (21 families x 6 layer-face borrows)
    // ------------------------------------------------------------------

    @Test
    void all21FamiliesBorrowTheSixFacePairs() throws Exception {
        assertEquals(21, GT6Sensors.ROWS.size(), "the sensor census stays 21");
        for (GT6Sensors.SensorRow tRow : GT6Sensors.ROWS) {
            for (String tLayerFace : LAYER_FACES) {
                assertPng("assets/gt6/textures/block/sensors/" + tRow.path() + "/" + tLayerFace + ".png");
            }
        }
    }

    // ------------------------------------------------------------------
    // the two-layer faceted block model
    // ------------------------------------------------------------------

    @Test
    void sensorModelsAreTheTwoLayerFacetCube() throws Exception {
        for (GT6Sensors.SensorRow tRow : GT6Sensors.ROWS) {
            String tBase = "gt6:block/sensors/" + tRow.path();
            JsonObject tModel = json("assets/gt6/models/block/sensors/" + tRow.path() + ".json");
            assertEquals("minecraft:block/cube", tModel.get("parent").getAsString(),
                    tRow.path() + ": the cube parent");
            assertEquals("minecraft:cutout", tModel.get("render_type").getAsString(),
                    tRow.path() + ": cutout (the overlay shells carry transparent texels)");
            scanNoTintindex(tModel, tRow.path());

            JsonObject tTex = tModel.getAsJsonObject("textures");
            assertEquals(tBase + "/colored_front", tTex.get("north").getAsString(),
                    tRow.path() + ": FRONT = the FACING face");
            assertEquals(tBase + "/colored_back", tTex.get("south").getAsString(),
                    tRow.path() + ": BACK = OPOS");
            for (String tFlank : new String[] {"down", "up", "west", "east", "particle"}) {
                assertEquals(tBase + "/colored_side", tTex.get(tFlank).getAsString(),
                        tRow.path() + ": the " + tFlank + " flank = side");
            }
            assertEquals(tBase + "/overlay_front", tTex.get("overlay_front").getAsString(),
                    tRow.path() + ": the front overlay shell");
            assertEquals(tBase + "/overlay_back", tTex.get("overlay_back").getAsString(),
                    tRow.path() + ": the back overlay shell");
            assertEquals(tBase + "/overlay_side", tTex.get("overlay_side").getAsString(),
                    tRow.path() + ": the side overlay shell");
            // the facet references are pairwise distinct (front/back/side never collapse)
            assertFalse(tTex.get("north").getAsString().equals(tTex.get("south").getAsString())
                    || tTex.get("north").getAsString().equals(tTex.get("west").getAsString())
                    || tTex.get("south").getAsString().equals(tTex.get("west").getAsString()),
                    tRow.path() + ": front/back/side references stay distinct");

            // two-layer structure: body cube + six 0.01 overlay plates
            var tElements = tModel.getAsJsonArray("elements");
            assertEquals(7, tElements.size(), tRow.path() + ": body + 6 shells");
            JsonObject tBody = tElements.get(0).getAsJsonObject();
            assertEquals(6, tBody.getAsJsonObject("faces").size(),
                    tRow.path() + ": the body covers all six faces");
            for (int i = 1; i < 7; i++) {
                JsonObject tShell = tElements.get(i).getAsJsonObject();
                assertEquals(1, tShell.getAsJsonObject("faces").size(),
                        tRow.path() + ": shell " + i + " is a single-face plate");
                assertTrue(tShell.getAsJsonArray("from").get(0).getAsDouble() < 0
                        || tShell.getAsJsonArray("to").get(0).getAsDouble() > 16
                        || tShell.getAsJsonArray("from").get(2).getAsDouble() < 0
                        || tShell.getAsJsonArray("to").get(2).getAsDouble() > 16
                        || tShell.getAsJsonArray("from").get(1).getAsDouble() < 0
                        || tShell.getAsJsonArray("to").get(1).getAsDouble() > 16,
                        tRow.path() + ": shell " + i + " rides the 0.01 offset outside the cube");
            }
        }
    }

    // ------------------------------------------------------------------
    // the six-way FACING map (pinned verbatim, the p26 rotation band)
    // ------------------------------------------------------------------

    @Test
    void sensorBlockstatesKeepTheSixWayFacingMap() throws Exception {
        for (GT6Sensors.SensorRow tRow : GT6Sensors.ROWS) {
            JsonObject tState = json("assets/gt6/blockstates/" + tRow.path() + ".json");
            JsonObject tVariants = tState.getAsJsonObject("variants");
            assertEquals(FACING_KEYS.size(), tVariants.size(),
                    tRow.path() + ": exactly the six FACING variants");
            for (String tFacing : FACING_KEYS) {
                JsonObject tVariant = tVariants.getAsJsonObject("facing=" + tFacing);
                assertNotNull(tVariant, tRow.path() + ": the facing=" + tFacing + " variant");
                assertEquals("gt6:block/sensors/" + tRow.path(), tVariant.get("model").getAsString(),
                        tRow.path() + ": the faceted model reference");
                int tX = switch (tFacing) {
                    case "down" -> 90;
                    case "up" -> 270;
                    default -> 0;
                };
                int tY = switch (tFacing) {
                    case "south" -> 180;
                    case "west" -> 270;
                    case "east" -> 90;
                    default -> 0; // north and the two verticals carry the x rotation only
                };
                assertEquals(tX, tVariant.has("x") ? tVariant.get("x").getAsInt() : 0,
                        tRow.path() + ": facing=" + tFacing + " rotationX");
                assertEquals(tY, tVariant.has("y") ? tVariant.get("y").getAsInt() : 0,
                        tRow.path() + ": facing=" + tFacing + " rotationY");
            }
        }
    }

    // ------------------------------------------------------------------
    // the item face (the block IS the held sensor, 1.7.10 form)
    // ------------------------------------------------------------------

    @Test
    void sensorItemModelsParentTheFacetBlockModel() throws Exception {
        for (GT6Sensors.SensorRow tRow : GT6Sensors.ROWS) {
            JsonObject tItem = json("assets/gt6/models/item/" + tRow.path() + ".json");
            assertEquals(1, tItem.size(), tRow.path() + ": the item model is the parent line only");
            assertEquals("gt6:block/sensors/" + tRow.path(), tItem.get("parent").getAsString(),
                    tRow.path() + ": the item parents the faceted block model");
        }
    }

    // ------------------------------------------------------------------
    // the old flat bake death watch
    // ------------------------------------------------------------------

    @Test
    void oldFlatBakeTexturesAreDead() throws Exception {
        Set<String> tOldRefs = new HashSet<>();
        for (GT6Sensors.SensorRow tRow : GT6Sensors.ROWS) {
            // the old baked composite PNG and its cube_all block model are deleted
            assertFalse(resource("assets/gt6/textures/block/" + tRow.path() + ".png") != null,
                    tRow.path() + ": the old flat bake PNG is retired");
            assertFalse(resource("assets/gt6/models/block/" + tRow.path() + ".json") != null,
                    tRow.path() + ": the old cube_all block model is retired");
            tOldRefs.add("gt6:block/" + tRow.path());
        }
        // no new sensor model still references the retired flat sprite ids
        for (GT6Sensors.SensorRow tRow : GT6Sensors.ROWS) {
            JsonObject tTex = json("assets/gt6/models/block/sensors/" + tRow.path() + ".json")
                    .getAsJsonObject("textures");
            for (var tEntry : tTex.entrySet()) {
                assertFalse(tOldRefs.contains(tEntry.getValue().getAsString()),
                        tRow.path() + "." + tEntry.getKey() + ": the retired flat sprite id");
            }
        }
    }
}
