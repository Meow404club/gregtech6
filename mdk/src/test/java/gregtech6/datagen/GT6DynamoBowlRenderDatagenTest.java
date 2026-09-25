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
 * <li>the 10 generated blockstates carry exactly the four HORIZONTAL_FACING variants with
 *     the output-front rotation map (NORTH→0 / SOUTH→180 / WEST→270 / EAST→90) and the
 *     shared family block model; the T0 ULV row keeps its placeholder blockstate;</li>
 * <li>the two generated block models pin the facing-cube texture map (north = the OUTPUT
 *     front face, south = the INPUT back face, the other four the side art) over face
 *     files that actually exist;</li>
 * <li>the 10 generated BlockItem models parent their family block model;</li>
 * <li>the 6 baked dynamo PNGs and the seven caught-item borrowed PNGs are grounded in
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

    /** facing variant → the expected rotationY (the addZpmDechargers map; FRONT = the output face). */
    private static final Map<String, Integer> FACING_ROTATIONS = Map.of(
            "facing=north", 0, "facing=south", 180, "facing=west", 270, "facing=east", 90);

    /** The six baked face files (family face → texture path under textures/block/). */
    private static final List<String> BAKED_PNGS = List.of(
            "electric_dynamo_front.png", "electric_dynamo_back.png", "electric_dynamo_side.png",
            "flux_dynamo_front.png", "flux_dynamo_back.png", "flux_dynamo_side.png");

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

    /** The ten blockstates: exactly the four facing variants, all pointing at the family model with the rotation map. */
    @Test
    void generatedBlockstatesPinTheFacingVariants() throws Exception {
        for (Map.Entry<String, List<String>> tLadder : LADDERS.entrySet()) {
            for (String tRow : tLadder.getValue()) {
                JsonObject tState = generatedJson("assets/gt6/blockstates/" + tRow + ".json");
                assertTrue(tState.has("variants"), tRow + ": the plain-variants form (no multipart)");
                var tVariants = tState.getAsJsonObject("variants");
                assertEquals(FACING_ROTATIONS.keySet(), tVariants.keySet(),
                        tRow + ": exactly the four HORIZONTAL_FACING variants");
                for (String tFacing : FACING_ROTATIONS.keySet()) {
                    JsonObject tVariant = tVariants.getAsJsonObject(tFacing);
                    assertEquals("gt6:block/" + tLadder.getKey(), tVariant.get("model").getAsString(),
                            tRow + " " + tFacing + ": the shared family block model");
                    // rotationY 0 is omitted from the variant (the vanilla default), any other rotation is explicit
                    assertEquals(FACING_ROTATIONS.get(tFacing).intValue(),
                            tVariant.has("y") ? tVariant.get("y").getAsInt() : 0,
                            tRow + " " + tFacing + ": the rotationY");
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

    /** The two block models: the facing-cube texture map (north front / south back / side elsewhere) over existing files. */
    @Test
    void blockModelsPinTheFacingCubeTextureMap() throws Exception {
        for (String tFamily : LADDERS.keySet()) {
            JsonObject tModel = generatedJson("assets/gt6/models/block/" + tFamily + ".json");
            assertEquals("minecraft:block/cube", tModel.get("parent").getAsString(),
                    tFamily + ": the vanilla cube parent");
            JsonObject tTextures = tModel.getAsJsonObject("textures");
            assertEquals("gt6:block/" + tFamily + "_front", tTextures.get("north").getAsString(),
                    tFamily + ": the north face is the OUTPUT front art");
            assertEquals("gt6:block/" + tFamily + "_back", tTextures.get("south").getAsString(),
                    tFamily + ": the south face is the INPUT back art");
            for (String tSide : List.of("east", "west", "up", "down")) {
                assertEquals("gt6:block/" + tFamily + "_side", tTextures.get(tSide).getAsString(),
                        tFamily + ": the " + tSide + " face is the side art");
            }
            for (String tFace : List.of("front", "back", "side")) {
                assertTrue(Files.isRegularFile(assetFile("textures/block/" + tFamily + "_" + tFace + ".png")),
                        tFamily + "_" + tFace + ".png must exist");
            }
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

    /** The six baked dynamo PNGs are grounded in assets/README.md by basename AND actual sha256. */
    @Test
    void bakedDynamoPngsAreGroundedInTheAssetsLedger() throws Exception {
        List<String> tViolations = new ArrayList<>();
        for (String tPng : BAKED_PNGS) {
            try {
                assertGroundedInLedger("textures/block/" + tPng, tPng);
            } catch (AssertionError tMiss) {
                tViolations.add(tMiss.getMessage());
            }
        }
        assertTrue(tViolations.isEmpty(), "baked dynamo texture without attribution: " + tViolations);
    }
}
