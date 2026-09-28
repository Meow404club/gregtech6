/**
 * Offline census for task p38-c1-dynamo-bowl-models — the dynamo + caught-item render-wave
 * fixes. The ten GT6DynamoBlock ladder rows (GT6ElectricDynamos T1-T5, GT6FluxDynamos
 * T1-T5) rendered as the magenta-black missing-model checkerboard: zero
 * blockstates/models/item-models in the generated tree. The caught ITEM band — clay_bowl
 * (GT6Kitchen.java:152) plus the six C5-guard catches (zpm, faucet_ceramic_raw, plow,
 * branch_cutter, sense, hand_drill) — held zero item models (the hand-held magenta case).
 * This census pins the fixes end to end (the {@link GT6KitchenRenderDatagenTest} family
 * method):
 * <ul>
 * <li>the 10 generated blockstates carry exactly the TWELVE variants of the SIX-WAY
 *     facing x ACTIVE property (issue #18, task r4-18-converter-tex-facing) over the
 *     output-front rotation map (NORTH→y0 / SOUTH→y180 / WEST→y270 / EAST→y90 /
 *     DOWN→x90 / UP→x270), active=true switching to the {@code _active} overlay shell;
 *     the T0 ULV row keeps its placeholder blockstate;</li>
 * <li>the two generated block models pin the addConverterModel two-layer form: the
 *     tintindex-0 colored body (north front / south back / four sides) + the six untinted
 *     0.01-plate overlay decals, cutout, over face files that actually exist;</li>
 * <li>the 10 generated BlockItem models parent their family block model;</li>
 * <li>the 18 borrowed dynamo PNGs (colored/overlay/overlay_active x front/back/side x
 *     two families) and the seven caught-item borrowed PNGs are grounded in
 *     assets/README.md by basename AND by the actual sha256 of their bytes (the p31
 *     attribution-nail pattern);</li>
 * <li>the seven caught item models pin their parent (generated vs handheld) and their
 *     exact layer stacks over sprites that exist.</li>
 * </ul>
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class GT6DynamoBowlRenderDatagenTest {

    /** The ten fixed ladder rows — family → row paths (the registry id walk, read-only). */
    private static final Map<String, List<String>> LADDERS = Map.of(
            "electric_dynamo", List.of("electric_dynamo", "electric_dynamo_t2",
                    "electric_dynamo_t3", "electric_dynamo_t4", "electric_dynamo_t5"),
            "flux_dynamo", List.of("flux_dynamo", "flux_dynamo_t2", "flux_dynamo_t3",
                    "flux_dynamo_t4", "flux_dynamo_t5"));

    /** facing → the expected rotationY (the output-front band; FRONT = the output face). */
    private static final Map<String, Integer> FACING_ROTATIONS = Map.of(
            "north", 0, "south", 180, "west", 270, "east", 90, "down", 0, "up", 0);

    /** facing → the expected rotationX (the dispenser band, issue #18 — the verticals). */
    private static final Map<String, Integer> FACING_ROTATIONS_X = Map.of(
            "down", 90, "up", 270, "north", 0, "south", 0, "west", 0, "east", 0);

    /** The 18 borrowed face files (family x layer x face), issue #18. */
    private static final List<String> BORROWED_PNGS = List.of(
            "electric_dynamo_colored_front.png", "electric_dynamo_colored_back.png", "electric_dynamo_colored_side.png",
            "electric_dynamo_overlay_front.png", "electric_dynamo_overlay_back.png", "electric_dynamo_overlay_side.png",
            "electric_dynamo_overlay_active_front.png", "electric_dynamo_overlay_active_back.png", "electric_dynamo_overlay_active_side.png",
            "flux_dynamo_colored_front.png", "flux_dynamo_colored_back.png", "flux_dynamo_colored_side.png",
            "flux_dynamo_overlay_front.png", "flux_dynamo_overlay_back.png", "flux_dynamo_overlay_side.png",
            "flux_dynamo_overlay_active_front.png", "flux_dynamo_overlay_active_back.png", "flux_dynamo_overlay_active_side.png");

    /** The seven caught item models — id → (parent, layer texture paths in layer order). */
    private static final Map<String, Map.Entry<String, List<String>>> CAUGHT_ITEMS = Map.of(
            "clay_bowl", Map.entry("minecraft:item/generated",
                    List.of("item/clay_bowl")),
            "faucet_ceramic_raw", Map.entry("minecraft:item/generated",
                    List.of("item/faucet_ceramic_raw")),
            "zpm", Map.entry("minecraft:item/generated",
                    List.of("item/zpm")),
            "plow", Map.entry("minecraft:item/handheld",
                    List.of("item/material_sets/metallic/tool_head_plow", "item/material_sets/metallic/tool_head_plow_overlay",
                            "item/material_sets/wood/stick", "item/material_sets/wood/stick_overlay")),
            "sense", Map.entry("minecraft:item/handheld",
                    List.of("item/material_sets/metallic/tool_head_sense", "item/material_sets/metallic/tool_head_sense_overlay",
                            "item/material_sets/wood/stick", "item/material_sets/wood/stick_overlay")),
            "branch_cutter", Map.entry("minecraft:item/handheld",
                    List.of("item/branch_cutter", "item/branch_cutter_overlay")),
            "hand_drill", Map.entry("minecraft:item/handheld",
                    List.of("item/hand_drill", "item/hand_drill_overlay")));

    /** The seven borrowed PNGs this card lands for the caught items (the plow/sense layers are pre-existing in-repo sprites). */
    private static final List<String> CAUGHT_BORROWED_PNGS = List.of(
            "clay_bowl.png", "faucet_ceramic_raw.png", "zpm.png",
            "branch_cutter.png", "branch_cutter_overlay.png", "hand_drill.png", "hand_drill_overlay.png");

    /** The mdk root (src/main/resources/assets/README.md), walked upward from the leg-dependent test working dir. */
    private static Path mdkRoot() {
        for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
            if (Files.isRegularFile(p.resolve("src/main/resources/assets/README.md"))) return p;
        }
        throw new AssertionError("mdk root (src/main/resources/assets/README.md) not found upward from "
                + Path.of("").toAbsolutePath());
    }

    /** One committed generated-tree JSON as an object (the test classpath carries src/generated/resources). */
    private static JsonObject generatedJson(String aPath) throws Exception {
        try (InputStream tStream = GT6DynamoBowlRenderDatagenTest.class.getClassLoader()
                .getResourceAsStream(aPath)) {
            assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    /** A main-tree texture file under assets/gt6/ (the GT6TextureCensusTest file-system form — texture reads never ride the leg classloader). */
    private static Path assetFile(String aPathUnderAssets) {
        return mdkRoot().resolve(Path.of("src", "main", "resources", "assets", "gt6")).resolve(aPathUnderAssets);
    }

    /** The attribution nail (the p31 wave pattern): the file exists, is named in assets/README.md, and its actual bytes hash to a sha256 the ledger records. */
    private static void assertGroundedInLedger(String aPathUnderAssets, String aBasename) throws Exception {
        Path tPng = assetFile(aPathUnderAssets);
        assertTrue(Files.isRegularFile(tPng), "the borrowed texture must exist: " + tPng);
        String tReadme = Files.readString(mdkRoot().resolve("src/main/resources/assets/README.md"), StandardCharsets.UTF_8);
        String tHex = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(tPng)));
        assertTrue(tReadme.contains(aBasename), aBasename + " — filename absent from assets/README.md");
        assertTrue(tReadme.contains(tHex), aBasename + " — bytes hash to " + tHex + ", not grounded in the ledger");
    }

    /** The ten blockstates: exactly the 12 six-way x active variants, the rotation map + the active shell swap (issue #18). */
    @Test
    void generatedBlockstatesPinTheFacingVariants() throws Exception {
        for (Map.Entry<String, List<String>> tLadder : LADDERS.entrySet()) {
            for (String tRow : tLadder.getValue()) {
                JsonObject tState = generatedJson("assets/gt6/blockstates/" + tRow + ".json");
                assertTrue(tState.has("variants"), tRow + ": the plain-variants form (no multipart)");
                var tVariants = tState.getAsJsonObject("variants");
                assertEquals(12, tVariants.size(), tRow + ": exactly the 12 facing x active variants");
                for (String tFacing : FACING_ROTATIONS.keySet()) {
                    for (boolean tActive : new boolean[] {false, true}) {
                        String tKey = "active=" + tActive + ",facing=" + tFacing;
                        assertTrue(tVariants.has(tKey), tRow + ": the variant key " + tKey);
                        JsonObject tVariant = tVariants.getAsJsonObject(tKey);
                        assertEquals("gt6:block/" + tLadder.getKey() + (tActive ? "_active" : ""),
                                tVariant.get("model").getAsString(),
                                tRow + " " + tKey + ": the family model (active swaps the shell)");
                        // rotation 0 is omitted from the variant (the vanilla default), any other rotation is explicit
                        assertEquals(FACING_ROTATIONS.get(tFacing).intValue(),
                                tVariant.has("y") ? tVariant.get("y").getAsInt() : 0,
                                tRow + " " + tKey + ": the rotationY");
                        assertEquals(FACING_ROTATIONS_X.get(tFacing).intValue(),
                                tVariant.has("x") ? tVariant.get("x").getAsInt() : 0,
                                tRow + " " + tKey + ": the rotationX");
                    }
                }
            }
        }
    }

    /** The T0 ULV placeholder blockstate survives (no regression on the addElectricDynamoUlv row). */
    @Test
    void ulvPlaceholderBlockstateSurvives() throws Exception {
        JsonObject tState = generatedJson("assets/gt6/blockstates/electric_dynamo_ulv.json");
        assertTrue(tState.getAsJsonObject("variants").has(""),
                "electric_dynamo_ulv: the placeholder single-state row remains");
    }

    private static final List<String> FACE_KEYS = List.of("down", "up", "north", "south", "west", "east");

    /** One two-layer converter model: the tinted colored body + six untinted 0.01-plate decals, cutout (the addConverterModel grammar). */
    private static void assertTwoLayerConverterModel(String tFamily, String tModelName, String tOverlayBand) throws Exception {
        JsonObject tModel = generatedJson("assets/gt6/models/block/" + tModelName + ".json");
        if (tModelName.endsWith("_active")) {
            // the ACTIVE child form: the inactive parent with the three overlay overrides
            assertEquals("gt6:block/" + tFamily, tModel.get("parent").getAsString(),
                    tFamily + ": the inactive family model parent");
            assertEquals("minecraft:cutout", tModel.get("render_type").getAsString(),
                    tFamily + ": cutout re-declared on the active shell");
            JsonObject tOverride = tModel.getAsJsonObject("textures");
            assertEquals("gt6:block/" + tFamily + "_" + tOverlayBand + "_front", tOverride.get("overlay_front").getAsString());
            assertEquals("gt6:block/" + tFamily + "_" + tOverlayBand + "_back", tOverride.get("overlay_back").getAsString());
            assertEquals("gt6:block/" + tFamily + "_" + tOverlayBand + "_side", tOverride.get("overlay_side").getAsString());
            assertTrue(!tModel.has("elements"), tFamily + ": the body elements stay inherited");
            return;
        }
        assertEquals("minecraft:block/cube", tModel.get("parent").getAsString(),
                tFamily + ": the vanilla cube parent");
        assertEquals("minecraft:cutout", tModel.get("render_type").getAsString(),
                tFamily + ": the overlay shells demand cutout");
        JsonObject tTextures = tModel.getAsJsonObject("textures");
        assertEquals("gt6:block/" + tFamily + "_colored_front", tTextures.get("north").getAsString(),
                tFamily + ": the north face is the grayscale colored front (the tint seat)");
        assertEquals("gt6:block/" + tFamily + "_colored_back", tTextures.get("south").getAsString(),
                tFamily + ": the south face is the colored back");
        for (String tSide : List.of("east", "west", "up", "down")) {
            assertEquals("gt6:block/" + tFamily + "_colored_side", tTextures.get(tSide).getAsString(),
                    tFamily + ": the " + tSide + " face is the colored side");
        }
        assertEquals("gt6:block/" + tFamily + "_" + tOverlayBand + "_front", tTextures.get("overlay_front").getAsString());
        assertEquals("gt6:block/" + tFamily + "_" + tOverlayBand + "_back", tTextures.get("overlay_back").getAsString());
        assertEquals("gt6:block/" + tFamily + "_" + tOverlayBand + "_side", tTextures.get("overlay_side").getAsString());
        // the element split: body tintindex 0, the six decals untinted (the P22 contract)
        var tElements = tModel.getAsJsonArray("elements");
        assertEquals(7, tElements.size(), tFamily + ": body + 6 decals");
        var tBodyFaces = tElements.get(0).getAsJsonObject().getAsJsonObject("faces");
        for (String tFace : FACE_KEYS) {
            assertTrue(tBodyFaces.get(tFace) != null, tFamily + " body face " + tFace);
            assertEquals(0, tBodyFaces.getAsJsonObject(tFace).get("tintindex").getAsInt(),
                    tFamily + " body face " + tFace + " carries tintindex 0 (the mRGBa seat)");
        }
        for (int i = 1; i < 7; i++) {
            var tDecalFaces = tElements.get(i).getAsJsonObject().getAsJsonObject("faces");
            for (String tFace : tDecalFaces.keySet()) {
                assertTrue(!tDecalFaces.getAsJsonObject(tFace).has("tintindex"),
                        tFamily + " decal " + i + " face " + tFace + " stays untinted");
            }
        }
    }

    /** The two block model pairs (inactive + _active): the two-layer grammar over existing files (issue #18). */
    @Test
    void blockModelsPinTheFacingCubeTextureMap() throws Exception {
        for (String tFamily : LADDERS.keySet()) {
            assertTwoLayerConverterModel(tFamily, tFamily, "overlay");
            assertTwoLayerConverterModel(tFamily, tFamily + "_active", "overlay_active");
        }
        for (String tPng : BORROWED_PNGS) {
            assertTrue(Files.isRegularFile(assetFile("textures/block/" + tPng)), tPng + " must exist");
        }
    }

    /** The ten BlockItem models parent their family block model (the zpm-decharger item form). */
    @Test
    void generatedItemModelsParentTheirBlockModel() throws Exception {
        for (Map.Entry<String, List<String>> tLadder : LADDERS.entrySet()) {
            for (String tRow : tLadder.getValue()) {
                JsonObject tItem = generatedJson("assets/gt6/models/item/" + tRow + ".json");
                assertEquals("gt6:block/" + tLadder.getKey(), tItem.get("parent").getAsString(),
                        tRow + ": the item parents its family block model");
            }
        }
    }

    /** The clay_bowl item model: item/generated, single layer over the borrowed icon. */
    @Test
    void clayBowlItemModelPinsTheBorrow() throws Exception {
        JsonObject tItem = generatedJson("assets/gt6/models/item/clay_bowl.json");
        assertEquals("minecraft:item/generated", tItem.get("parent").getAsString(),
                "clay_bowl: the vanilla generated parent");
        assertEquals("gt6:item/clay_bowl", tItem.getAsJsonObject("textures").get("layer0").getAsString(),
                "clay_bowl: the single layer0 borrow");
        assertGroundedInLedger("textures/item/clay_bowl.png", "clay_bowl.png");
    }

    /** The seven caught items (clay_bowl + the six C5-guard catches): parent + layer stack over existing sprites, the borrows ledger-grounded. */
    @Test
    void caughtItemModelsPinParentsAndLayers() throws Exception {
        for (Map.Entry<String, Map.Entry<String, List<String>>> tRow : CAUGHT_ITEMS.entrySet()) {
            String tId = tRow.getKey();
            JsonObject tItem = generatedJson("assets/gt6/models/item/" + tId + ".json");
            assertEquals(tRow.getValue().getKey(), tItem.get("parent").getAsString(),
                    tId + ": the parent face");
            JsonObject tTextures = tItem.getAsJsonObject("textures");
            List<String> tLayers = tRow.getValue().getValue();
            assertEquals(tLayers.size(), tTextures.size(), tId + ": exactly the declared layers");
            for (int i = 0; i < tLayers.size(); i++) {
                String tRef = tTextures.get("layer" + i).getAsString();
                assertEquals("gt6:" + tLayers.get(i), tRef, tId + ": layer" + i);
                assertTrue(Files.isRegularFile(assetFile("textures/" + tLayers.get(i) + ".png")),
                        tId + ": layer" + i + " sprite must exist: " + tRef);
            }
        }
        List<String> tViolations = new ArrayList<>();
        for (String tPng : CAUGHT_BORROWED_PNGS) {
            try {
                assertGroundedInLedger("textures/item/" + tPng, tPng);
            } catch (AssertionError tMiss) {
                tViolations.add(tMiss.getMessage());
            }
        }
        assertTrue(tViolations.isEmpty(), "caught-item borrow without attribution: " + tViolations);
    }

    /** The 18 borrowed dynamo PNGs are grounded in assets/README.md by basename AND actual sha256 (issue #18). */
    @Test
    void bakedDynamoPngsAreGroundedInTheAssetsLedger() throws Exception {
        List<String> tViolations = new ArrayList<>();
        for (String tPng : BORROWED_PNGS) {
            try {
                assertGroundedInLedger("textures/block/" + tPng, tPng);
            } catch (AssertionError tMiss) {
                tViolations.add(tMiss.getMessage());
            }
        }
        assertTrue(tViolations.isEmpty(), "baked dynamo texture without attribution: " + tViolations);
    }
}
