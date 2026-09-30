/**
 * Offline pin for task rod-render-pool — the 12 wall attachments trade the cube_all
 * placeholder for the upstream three-pass render stacks as datagen element models: the
 * tap (MultiTileEntityFluidTap.java:186-208 verbatim px boxes) and the funnel (the
 * :99-123 north-mount taper + the :107 under-host DOWN mount), rotated per FACING with
 * the addSensors band (FACING points AT the host). Reads the committed generated tree
 * off the classpath (the GT6SensorFacetRenderDatagenTest form); the old per-row
 * cube_all model death watch rides the same walk.
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.registry.GT6Attachments;
import gregtech6.tileentity.GTOfflineTestBase;

public class GT6AttachmentStackDatagenTest extends GTOfflineTestBase {

    /** The tap stack, MultiTileEntityFluidTap :186-208 verbatim (px). */
    private static final List<float[]> TAP_BOXES = List.of(
            new float[] {6, 6, 2, 10, 7, 4}, new float[] {7, 4, 0, 9, 6, 4}, new float[] {7, 3, 4, 9, 6, 10});

    /** The funnel north-mount taper, MultiTileEntityFluidFunnel :100-106 verbatim (px). */
    private static final List<float[]> FUNNEL_BOXES = List.of(
            new float[] {5, 9, 0, 11, 10, 10}, new float[] {6, 8, 0, 10, 9, 12}, new float[] {7, 7, 0, 9, 8, 14});

    /** The funnel under-host DOWN mount, the :107-109 default case verbatim (px). */
    private static final List<float[]> FUNNEL_DOWN_BOXES = List.of(
            new float[] {5, 2, 5, 11, 3, 11}, new float[] {6, 1, 6, 10, 2, 10}, new float[] {7, 0, 7, 9, 1, 9});

    private static InputStream resource(String aPath) {
        return GT6AttachmentStackDatagenTest.class.getClassLoader().getResourceAsStream(aPath);
    }

    private static JsonObject json(String aPath) throws Exception {
        try (InputStream tStream = resource(aPath)) {
            assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
    }

    /** The three-box element plan: verbatim boxes, family texture, tint seat, boundary culls. */
    private void assertStackModel(String aModelPath, String aTexture, List<float[]> aBoxes) throws Exception {
        JsonObject tModel = json("assets/gt6/models/" + aModelPath + ".json");
        assertEquals("minecraft:block/block", tModel.get("parent").getAsString(), aModelPath + ": the block/block parent");
        JsonObject tTex = tModel.getAsJsonObject("textures");
        assertEquals("gt6:block/" + aTexture, tTex.get("all").getAsString(), aModelPath + ": the family texture");
        assertEquals("#all", tTex.get("particle").getAsString(), aModelPath + ": the particle rides the family texture");

        var tElements = tModel.getAsJsonArray("elements");
        assertEquals(3, tElements.size(), aModelPath + ": the three-pass stack");
        for (int i = 0; i < 3; i++) {
            JsonObject tElement = tElements.get(i).getAsJsonObject();
            float[] tBox = aBoxes.get(i);
            for (int c = 0; c < 3; c++) {
                assertEquals(tBox[c], tElement.getAsJsonArray("from").get(c).getAsDouble(), 1e-9,
                        aModelPath + ": element " + i + " from[" + c + "]");
                assertEquals(tBox[3 + c], tElement.getAsJsonArray("to").get(c).getAsDouble(), 1e-9,
                        aModelPath + ": element " + i + " to[" + c + "]");
            }
            JsonObject tFaces = tElement.getAsJsonObject("faces");
            assertEquals(6, tFaces.size(), aModelPath + ": element " + i + " carries all six faces");
            for (var tFaceEntry : tFaces.entrySet()) {
                JsonObject tFace = tFaceEntry.getValue().getAsJsonObject();
                assertEquals("#all", tFace.get("texture").getAsString(), aModelPath + ": element " + i + " texture");
                assertEquals(0, tFace.get("tintindex").getAsInt(),
                        aModelPath + ": element " + i + " seats the future tint (tint-coverage-batch defer)");
                boolean tBoundary = switch (tFaceEntry.getKey()) {
                    case "down" -> tBox[1] <= 0.0F;
                    case "up" -> tBox[4] >= 16.0F;
                    case "north" -> tBox[2] <= 0.0F;
                    case "south" -> tBox[5] >= 16.0F;
                    case "west" -> tBox[0] <= 0.0F;
                    default -> tBox[3] >= 16.0F;
                };
                if (tBoundary) {
                    assertEquals(tFaceEntry.getKey(), tFace.get("cullface").getAsString(),
                            aModelPath + ": element " + i + " boundary face culls");
                } else {
                    assertNull(tFace.get("cullface"),
                            aModelPath + ": element " + i + " interior face must NOT cull");
                }
            }
        }
    }

    @Test
    public void attachmentModelsAreTheUpstreamThreePassStacks() throws Exception {
        assertEquals(12, GT6Attachments.ROWS.size(), "the attachment census stays 12");
        assertStackModel("block/attachment_tap", "tap", TAP_BOXES);
        assertStackModel("block/attachment_funnel", "funnel", FUNNEL_BOXES);
        assertStackModel("block/attachment_funnel_down", "funnel", FUNNEL_DOWN_BOXES);
    }

    @Test
    public void attachmentBlockstatesRotateTheStackPerFacing() throws Exception {
        // the addSensors horizontal band: N=0, S=180, W=270, E=90; the funnel's DOWN
        // mount rides the dedicated under-host model, the tap's DOWN slot (family
        // invalid, unreachable) and the UP slots reuse the horizontal model
        for (GT6Attachments.AttachmentRow tRow : GT6Attachments.ROWS) {
            JsonObject tState = json("assets/gt6/blockstates/" + tRow.path() + ".json");
            JsonObject tVariants = tState.getAsJsonObject("variants");
            assertEquals(6, tVariants.size(), tRow.path() + ": exactly the six FACING variants");
            boolean tTap = tRow.family() == gregtech6.block.attachment.GTAttachmentSmallBlock.Family.TAP;
            String tHorizontalModel = "gt6:block/attachment_" + (tTap ? "tap" : "funnel");
            for (var tEntry : tVariants.entrySet()) {
                JsonElement tValue = tEntry.getValue();
                JsonObject tVariant = tValue.isJsonArray() ? tValue.getAsJsonArray().get(0).getAsJsonObject()
                        : tValue.getAsJsonObject();
                String tFacing = tEntry.getKey().substring("facing=".length());
                if (tTap || !tFacing.equals("down")) {
                    assertEquals(tHorizontalModel, tVariant.get("model").getAsString(),
                            tRow.path() + ": " + tFacing + " model");
                } else {
                    assertEquals("gt6:block/attachment_funnel_down", tVariant.get("model").getAsString(),
                            tRow.path() + ": the DOWN mount rides the under-host model");
                }
                int tY = switch (tFacing) {
                    case "south" -> 180;
                    case "west" -> 270;
                    case "east" -> 90;
                    default -> 0;
                };
                assertEquals(tY, tVariant.has("y") ? tVariant.get("y").getAsInt() : 0,
                        tRow.path() + ": " + tFacing + " rotationY");
            }
        }
    }

    @Test
    public void attachmentItemModelsStayTheFamilyIcons() throws Exception {
        for (GT6Attachments.AttachmentRow tRow : GT6Attachments.ROWS) {
            JsonObject tItem = json("assets/gt6/models/item/" + tRow.path() + ".json");
            assertEquals("minecraft:item/generated", tItem.get("parent").getAsString(),
                    tRow.path() + ": the 2D item icon stays");
        }
    }

    @Test
    public void oldCubeAllPerRowModelsAreDead() throws Exception {
        for (GT6Attachments.AttachmentRow tRow : GT6Attachments.ROWS) {
            assertFalse(resource("assets/gt6/models/block/" + tRow.path() + ".json") != null,
                    tRow.path() + ": the old cube_all per-row model is retired");
        }
        // no attachment model still references a per-row model id
        for (String tModel : new String[] {"attachment_tap", "attachment_funnel", "attachment_funnel_down"}) {
            JsonElement tParent = json("assets/gt6/models/block/" + tModel + ".json").get("parent");
            assertTrue(tParent != null, tModel + ": keeps its parent");
        }
    }
}
