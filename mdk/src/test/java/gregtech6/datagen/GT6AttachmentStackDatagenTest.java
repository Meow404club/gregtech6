/**
 * Offline pin for task rod-render-pool — the 12 wall attachments trade the cube_all
 * placeholder for the upstream three-pass render stacks as datagen element models: the
 * tap (MultiTileEntityFluidTap.java:186-208 verbatim px boxes) and the funnel (the
 * :99-123 north-mount taper + the :107 under-host DOWN mount), rotated per FACING with
 * the addSensors band (FACING points AT the host). Reads the committed generated tree
 * off the classpath (the GT6SensorFacetRenderDatagenTest form); the old per-row
 * cube_all model death watch rides the same walk.
 *
 * <p>Task tap-funnel-model-audit extends the walk: each colored box is twinned by an
 * overlay-pass element (the :204/:124 getTexture2 BlockTextureMulti second layer) over
 * the cutout layer, the borrowed overlay PNGs are sha-pinned, the BlockItems parent the
 * block models (upstream has no dedicated item PNG — the 1.7.10 item renders the 3D
 * stack), the {@code GT6AttachmentTintListener} seam answers the row material's
 * mRGBaSolid, and the two FAUCET rows (the crucible pouring spout, TileEntityFaucet
 * .FaucetBlock — the block family the rod-render-pool walk never reached) rotate the
 * MultiTileEntityFaucet.java:168-200 stack per FACING.
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.registry.GT6Attachments;
import gregtech6.registry.GT6Molds;
import gregtech6.tileentity.GTOfflineTestBase;

public class GT6AttachmentStackDatagenTest extends GTOfflineTestBase {

    /**
     * The tap stack, MultiTileEntityFluidTap :186-208 verbatim (px, PX_N[i]=16-i) —
     * the third box is the :200 pass-2 spout: PX_N[7]=9, PX_N[10]=6, PX_N[10]=6
     * (the PX_N INDEX 10 is a pipe-column index, never a box coordinate — the first
     * pin rode the raw 10 and read the spout tip 4px too long).
     */
    private static final List<float[]> TAP_BOXES = List.of(
            new float[] {6, 6, 2, 10, 7, 4}, new float[] {7, 4, 0, 9, 6, 4}, new float[] {7, 3, 4, 9, 6, 6});

    /**
     * The funnel north-mount taper, MultiTileEntityFluidFunnel :99-127 verbatim (px,
     * PX_N[i]=16-i) — the z-maxes are the :103/:111/:119 PX_N[10/12/14] = 6/4/2 (the
     * raw indices 10/12/14 pinned here once measured the needle 4/8/12px too long).
     */
    private static final List<float[]> FUNNEL_BOXES = List.of(
            new float[] {5, 9, 0, 11, 10, 6}, new float[] {6, 8, 0, 10, 9, 4}, new float[] {7, 7, 0, 9, 8, 2});

    /** The funnel under-host DOWN mount, the :107-109 default case verbatim (px). */
    private static final List<float[]> FUNNEL_DOWN_BOXES = List.of(
            new float[] {5, 2, 5, 11, 3, 11}, new float[] {6, 1, 6, 10, 2, 10}, new float[] {7, 0, 7, 9, 1, 9});

    /**
     * The faucet stack, MultiTileEntityFaucet :168-200 verbatim (px, PX_P[i]=i /
     * PX_N[i]=16-i, the north-mount branch): pass-0 (PX_P[6],PX_P[1],PX_P[0])-
     * (PX_N[6],PX_N[14],PX_N[12]) = (6,1,0)-(10,2,4), pass-1 (5,2,0)-(6,6,4),
     * pass-2 (10,2,0)-(11,6,4).
     */
    private static final List<float[]> FAUCET_BOXES = List.of(
            new float[] {6, 1, 0, 10, 2, 4}, new float[] {5, 2, 0, 6, 6, 4}, new float[] {10, 2, 0, 11, 6, 4});

    /** The upstream overlay side borrow (tap == funnel, byte-identical — assets/README.md). */
    private static final String OVERLAY_SHA256 =
            "02fc1d92864f19600bd1abd5bd455bb2ea8932f340050c913a198736e1eca27d";

    /** The 0.01 outward inflation of an overlay twin (the addConverterModel decal grammar). */
    private static final float EPSILON = 0.01F;

    private static InputStream resource(String aPath) {
        return GT6AttachmentStackDatagenTest.class.getClassLoader().getResourceAsStream(aPath);
    }

    @org.junit.jupiter.api.BeforeAll
    static void bootMaterials() {
        // the row materials resolve through MT.init (the GT6MoldTintDatagenTest bracket —
        // the bare JVM leaves the MT statics null)
        gregtech6.registry.GT6MaterialTestSupport.materials();
    }

    private static JsonObject json(String aPath) throws Exception {
        try (InputStream tStream = resource(aPath)) {
            assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
    }

    private static String sha256(String aPath) throws Exception {
        try (InputStream tStream = resource(aPath)) {
            assertNotNull(tStream, "the texture must be on the classpath: " + aPath);
            byte[] tDigest = MessageDigest.getInstance("SHA-256")
                    .digest(tStream.readAllBytes());
            StringBuilder tHex = new StringBuilder();
            for (byte b : tDigest) tHex.append(String.format("%02x", b));
            return tHex.toString();
        }
    }

    /** One colored element: verbatim box, family texture, tint seat, boundary culls. */
    private void assertColoredElement(JsonObject aModel, String aModelPath, int aIndex, float[] aBox) {
        JsonObject tElement = aModel.getAsJsonArray("elements").get(aIndex).getAsJsonObject();
        for (int c = 0; c < 3; c++) {
            assertEquals(aBox[c], tElement.getAsJsonArray("from").get(c).getAsDouble(), 1e-9,
                    aModelPath + ": element " + aIndex + " from[" + c + "]");
            assertEquals(aBox[3 + c], tElement.getAsJsonArray("to").get(c).getAsDouble(), 1e-9,
                    aModelPath + ": element " + aIndex + " to[" + c + "]");
        }
        JsonObject tFaces = tElement.getAsJsonObject("faces");
        assertEquals(6, tFaces.size(), aModelPath + ": element " + aIndex + " carries all six faces");
        for (var tFaceEntry : tFaces.entrySet()) {
            JsonObject tFace = tFaceEntry.getValue().getAsJsonObject();
            assertEquals("#all", tFace.get("texture").getAsString(), aModelPath + ": element " + aIndex + " texture");
            assertEquals(0, tFace.get("tintindex").getAsInt(),
                    aModelPath + ": element " + aIndex + " seats the material tint");
            boolean tBoundary = isBoundary(aBox, tFaceEntry.getKey());
            if (tBoundary) {
                assertEquals(tFaceEntry.getKey(), tFace.get("cullface").getAsString(),
                        aModelPath + ": element " + aIndex + " boundary face culls");
            } else {
                assertNull(tFace.get("cullface"),
                        aModelPath + ": element " + aIndex + " interior face must NOT cull");
            }
        }
    }

    /** One overlay twin: the box inflated 0.01 outward, overlay art, untinted. */
    private void assertOverlayElement(JsonObject aModel, String aModelPath, int aIndex, float[] aBox) {
        JsonObject tElement = aModel.getAsJsonArray("elements").get(aIndex).getAsJsonObject();
        float[] tOver = {aBox[0] - EPSILON, aBox[1] - EPSILON, aBox[2] - EPSILON,
                aBox[3] + EPSILON, aBox[4] + EPSILON, aBox[5] + EPSILON};
        for (int c = 0; c < 3; c++) {
            assertEquals(tOver[c], tElement.getAsJsonArray("from").get(c).getAsDouble(), 1e-6,
                    aModelPath + ": overlay " + aIndex + " from[" + c + "]");
            assertEquals(tOver[3 + c], tElement.getAsJsonArray("to").get(c).getAsDouble(), 1e-6,
                    aModelPath + ": overlay " + aIndex + " to[" + c + "]");
        }
        for (var tFaceEntry : tElement.getAsJsonObject("faces").entrySet()) {
            JsonObject tFace = tFaceEntry.getValue().getAsJsonObject();
            assertEquals("#overlay", tFace.get("texture").getAsString(),
                    aModelPath + ": overlay " + aIndex + " texture");
            assertNull(tFace.get("tintindex"),
                    aModelPath + ": overlay " + aIndex + " stays untinted (upstream tints the colored pass only)");
            boolean tBoundary = isBoundary(aBox, tFaceEntry.getKey());
            if (tBoundary) {
                assertEquals(tFaceEntry.getKey(), tFace.get("cullface").getAsString(),
                        aModelPath + ": overlay " + aIndex + " boundary face culls");
            } else {
                assertNull(tFace.get("cullface"),
                        aModelPath + ": overlay " + aIndex + " interior face must NOT cull");
            }
        }
    }

    private static boolean isBoundary(float[] aBox, String aFace) {
        return switch (aFace) {
            case "down" -> aBox[1] <= 0.0F;
            case "up" -> aBox[4] >= 16.0F;
            case "north" -> aBox[2] <= 0.0F;
            case "south" -> aBox[5] >= 16.0F;
            case "west" -> aBox[0] <= 0.0F;
            default -> aBox[3] >= 16.0F;
        };
    }

    /** The colored+overlay pair plan: verbatim boxes, texture pair, cutout, tint seat. */
    private void assertStackModel(String aModelPath, String aTexture, List<float[]> aBoxes) throws Exception {
        JsonObject tModel = json("assets/gt6/models/" + aModelPath + ".json");
        assertEquals("minecraft:block/block", tModel.get("parent").getAsString(), aModelPath + ": the block/block parent");
        assertEquals("minecraft:cutout", tModel.get("render_type").getAsString(),
                aModelPath + ": the overlay alpha needs the cutout layer (the r11-oven solid-layer lesson)");
        JsonObject tTex = tModel.getAsJsonObject("textures");
        assertEquals("gt6:block/" + aTexture, tTex.get("all").getAsString(), aModelPath + ": the family texture");
        assertEquals("gt6:block/" + aTexture + "_overlay", tTex.get("overlay").getAsString(),
                aModelPath + ": the family overlay texture");
        assertEquals("#all", tTex.get("particle").getAsString(), aModelPath + ": the particle rides the family texture");

        var tElements = tModel.getAsJsonArray("elements");
        assertEquals(6, tElements.size(), aModelPath + ": the three-pass stack + the three overlay twins");
        for (int i = 0; i < 3; i++) {
            assertColoredElement(tModel, aModelPath, 2 * i, aBoxes.get(i));
            assertOverlayElement(tModel, aModelPath, 2 * i + 1, aBoxes.get(i));
        }
    }

    @Test
    public void attachmentModelsAreTheUpstreamThreePassStacks() throws Exception {
        assertEquals(12, GT6Attachments.ROWS.size(), "the attachment census stays 12");
        assertStackModel("block/attachment_tap", "tap", TAP_BOXES);
        assertStackModel("block/attachment_funnel", "funnel", FUNNEL_BOXES);
        assertStackModel("block/attachment_funnel_down", "funnel", FUNNEL_DOWN_BOXES);
    }

    /** The borrowed overlay art is byte-identical upstream (the sha ledger, assets/README.md). */
    @Test
    public void overlayTexturesAreTheUpstreamBorrows() throws Exception {
        assertEquals(OVERLAY_SHA256, sha256("assets/gt6/textures/block/tap_overlay.png"), "tap_overlay sha256");
        assertEquals(OVERLAY_SHA256, sha256("assets/gt6/textures/block/funnel_overlay.png"), "funnel_overlay sha256");
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

    /**
     * The faucet rows (the crucible pouring spout — the block family the rod-render-pool
     * walk never reached): the upstream three-pass faucet stack, no overlay pass
     * (MultiTileEntityFaucet getTexture2 = the single material blockSolid × mRGBa), the
     * tint seat inert on the finished-texture stone row (the mold listener doctrine).
     */
    @Test
    public void faucetModelsAreTheUpstreamThreePassStacks() throws Exception {
        assertEquals(2, GT6Molds.FAUCET_ROWS.size(), "the faucet census stays 2 (the multi-material expansion is a separate card)");
        for (GT6Molds.FaucetRow tRow : GT6Molds.FAUCET_ROWS) {
            JsonObject tModel = json("assets/gt6/models/block/" + tRow.path() + ".json");
            assertEquals("minecraft:block/block", tModel.get("parent").getAsString(),
                    tRow.path() + ": the block/block parent");
            assertNull(tModel.get("render_type"),
                    tRow.path() + ": no overlay pass, no alpha, the solid layer stays");
            var tElements = tModel.getAsJsonArray("elements");
            assertEquals(3, tElements.size(), tRow.path() + ": the three-pass stack");
            for (int i = 0; i < 3; i++) {
                float[] tBox = FAUCET_BOXES.get(i);
                JsonObject tElement = tElements.get(i).getAsJsonObject();
                for (int c = 0; c < 3; c++) {
                    assertEquals(tBox[c], tElement.getAsJsonArray("from").get(c).getAsDouble(), 1e-9,
                            tRow.path() + ": element " + i + " from[" + c + "]");
                    assertEquals(tBox[3 + c], tElement.getAsJsonArray("to").get(c).getAsDouble(), 1e-9,
                            tRow.path() + ": element " + i + " to[" + c + "]");
                }
                for (var tFaceEntry : tElement.getAsJsonObject("faces").entrySet()) {
                    assertEquals("#all", tFaceEntry.getValue().getAsJsonObject().get("texture").getAsString(),
                            tRow.path() + ": element " + i + " texture");
                    assertEquals(0, tFaceEntry.getValue().getAsJsonObject().get("tintindex").getAsInt(),
                            tRow.path() + ": element " + i + " seats the material tint");
                }
            }
        }
    }

    /** The faucet blockstate rotates the stack with the addAttachments band (all six FACING keys). */
    @Test
    public void faucetBlockstatesRotateTheStackPerFacing() throws Exception {
        for (GT6Molds.FaucetRow tRow : GT6Molds.FAUCET_ROWS) {
            JsonObject tState = json("assets/gt6/blockstates/" + tRow.path() + ".json");
            JsonObject tVariants = tState.getAsJsonObject("variants");
            assertEquals(6, tVariants.size(), tRow.path() + ": exactly the six FACING variants");
            for (var tEntry : tVariants.entrySet()) {
                JsonElement tValue = tEntry.getValue();
                JsonObject tVariant = tValue.isJsonArray() ? tValue.getAsJsonArray().get(0).getAsJsonObject()
                        : tValue.getAsJsonObject();
                assertEquals("gt6:block/" + tRow.path(), tVariant.get("model").getAsString(),
                        tRow.path() + ": " + tEntry.getKey() + " model");
                String tFacing = tEntry.getKey().substring("facing=".length());
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
    public void attachmentItemModelsRideThe3DBlockIcons() throws Exception {
        for (GT6Attachments.AttachmentRow tRow : GT6Attachments.ROWS) {
            JsonObject tItem = json("assets/gt6/models/item/" + tRow.path() + ".json");
            String tExpected = "gt6:block/attachment_" + (tRow.family() == gregtech6.block.attachment.GTAttachmentSmallBlock.Family.TAP ? "tap" : "funnel");
            assertEquals(tExpected, tItem.get("parent").getAsString(),
                    tRow.path() + ": the 3D block icon (upstream has no dedicated item PNG)");
        }
        // the faucet items already parent their (now spout-shaped) block models
        for (GT6Molds.FaucetRow tRow : GT6Molds.FAUCET_ROWS) {
            JsonObject tItem = json("assets/gt6/models/item/" + tRow.path() + ".json");
            assertEquals("gt6:block/" + tRow.path(), tItem.get("parent").getAsString(),
                    tRow.path() + ": the 3D block icon");
        }
    }

    /**
     * The tint seam: every row resolves its upstream material and the mRGBaSolid pack;
     * the faucet's synthetic rows (unknown slug) stay untinted there (the mold listener
     * owns them).
     */
    @Test
    public void attachmentTintSeamResolvesTheRowMaterials() {
        for (GT6Attachments.AttachmentRow tRow : GT6Attachments.ROWS) {
            gregapi.oredict.OreDictMaterial tMaterial = gregtech6.registry.GT6Attachments.materialOf(tRow);
            assertNotNull(tMaterial, tRow.path() + ": the upstream NBT_MATERIAL resolves");
            short[] tRGBa = tMaterial.mRGBaSolid;
            assertEquals(0xFF000000 | (tRGBa[0] << 16) | (tRGBa[1] << 8) | tRGBa[2],
                    gregtech6.client.GT6AttachmentTintListener.rowTintARGB(tRow, 0),
                    tRow.path() + ": the mRGBaSolid pack rides the row material");
            assertEquals(-1, gregtech6.client.GT6AttachmentTintListener.rowTintARGB(tRow, 1),
                    tRow.path() + ": only tintindex 0 is seated");
        }
        // the literal anchor (non-tautological, the GT6MoldTintDatagenTest CERAMIC_TINT value):
        // the first row is tap_ceramic = MT.Ceramic 220/130/70
        assertEquals(0xFFDC8246, gregtech6.client.GT6AttachmentTintListener.rowTintARGB(GT6Attachments.ROWS.get(0), 0),
                "the ceramic pack is the literal mRGBaSolid multiply");
        // the faucet's synthetic TAP-family rows ride the mold listener, not this one
        assertEquals(-1, gregtech6.client.GT6AttachmentTintListener.rowTintARGB(
                new GT6Attachments.AttachmentRow("faucet_stone", "Stone",
                        gregtech6.block.attachment.GTAttachmentSmallBlock.Family.TAP, false, 1.0F, 5.0F,
                        net.minecraft.world.level.block.SoundType.STONE), 0),
                "the faucet synthetic row stays untinted here");
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
