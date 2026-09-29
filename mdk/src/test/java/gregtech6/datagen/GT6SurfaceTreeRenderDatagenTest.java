/*
 * Offline pinned tests for task p38-issue1-4 (GitHub #1 + #4): the surface deco band
 * geometry fix and the Rainbowood leaves tint wiring, asserted against the committed
 * src/generated tree (the GT6BarrelPaintRenderDatagenTest shape).
 *
 * <p>#1 ground geometry: the stick is the upstream 12x2x2 lying bar (MultiTileEntityStick
 * .java:53 default bounds 2..14 x 7..9, endpoint notation, default centered-in-Z pose;
 * the FACING blockstate still emits both
 * rotationY bar orientations), the rock the 8x3x8 fixed representative of the upstream
 * 2..8px random micro box (MultiTileEntityRock.java:58-67, tintindex 0 kept).
 *
 * <p>#4 rainbow leaves: the grayscale rainbowood_leaves model gains tintindex 0 on every
 * face (the tintedCubeAll grammar over the ONLY untinted-canvas tree kind), the world/
 * inventory tint tables are the GT6TreeClientListener RAINBOW registrations — the color
 * table is pinned here against transcription drift (CS.java:330-355).
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
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.client.render.GT6TreeClientListener;

class GT6SurfaceTreeRenderDatagenTest {

    private static final List<String> FACE_KEYS = List.of("down", "up", "north", "south", "west", "east");

    private static JsonObject json(String aPath) throws Exception {
        try (InputStream tStream = GT6SurfaceTreeRenderDatagenTest.class.getClassLoader()
                .getResourceAsStream(aPath)) {
            assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    /** #1 stick: the 12x2x2 ground bar over the vanilla oak-log side, NO tintindex (the untinted borrow). */
    @Test
    void stickModelIsTheUpstreamLyingBar() throws Exception {
        JsonObject tModel = json("assets/gt6/models/block/surface_stick.json");
        assertEquals("minecraft:block/block", tModel.get("parent").getAsString());
        var tTextures = tModel.getAsJsonObject("textures");
        assertEquals("minecraft:block/oak_log", tTextures.get("slab").getAsString());
        assertEquals("#slab", tTextures.get("particle").getAsString());
        var tElements = tModel.getAsJsonArray("elements");
        assertEquals(1, tElements.size());
        JsonObject tBox = tElements.get(0).getAsJsonObject();
        assertEquals(List.of(2, 0, 7), tBox.getAsJsonArray("from").asList().stream()
                .map(e -> e.getAsInt()).toList(), "stick from = the bar's ground pose");
        assertEquals(List.of(14, 2, 9), tBox.getAsJsonArray("to").asList().stream()
                .map(e -> e.getAsInt()).toList(), "stick to = the 2..14 endpoint bar (12px), not the old 12x2x12 slab");
        var tFaces = tBox.getAsJsonObject("faces");
        assertEquals(6, tFaces.size());
        for (String tFaceKey : FACE_KEYS) {
            JsonObject tFace = tFaces.getAsJsonObject(tFaceKey);
            assertEquals("#slab", tFace.get("texture").getAsString());
            assertFalse(tFace.has("tintindex"), "stick face " + tFaceKey + ": untinted");
        }
    }

    /**
     * #12 stick blockstate (r3-stick-shape-random + debt-issue12-shape-follow-tilt): all
     * six FACINGS, each a weighted EIGHT-variant list — the shared-table order
     * (GT6SurfaceVariants.Stick declaration order == JSON array order == the
     * WeightedRandom scan order the selection box replays): centered arms x2, one slide
     * tier per arm, the two tilt models on both arms. First entry of every facing =
     * the unrotated default arm.
     */
    @Test
    void stickBlockstateIsTheWeightedVariantBand() throws Exception {
        JsonObject tState = json("assets/gt6/blockstates/surface_stick.json");
        var tVariants = tState.getAsJsonObject("variants");
        assertEquals(6, tVariants.size(), "the full FACING table");
        // the pool pinned in emission order (GT6SurfaceVariants.Stick): model id + arm y
        List<String> tPoolModels = List.of("gt6:block/surface_stick", "gt6:block/surface_stick",
                "gt6:block/surface_stick_a", "gt6:block/surface_stick_b",
                "gt6:block/surface_stick_t22", "gt6:block/surface_stick_t22",
                "gt6:block/surface_stick_t45", "gt6:block/surface_stick_t45");
        List<Integer> tPoolArms = List.of(0, 90, 0, 90, 0, 90, 0, 90);
        for (String tFacing : List.of("down", "up", "north", "south", "west", "east")) {
            var tList = tVariants.getAsJsonArray("facing=" + tFacing);
            assertEquals(8, tList.size(), tFacing + ": 2 centered arms + 2 slide + 2x2 tilt");
            for (int i = 0; i < tList.size(); i++) {
                JsonObject tEntry = tList.get(i).getAsJsonObject();
                assertEquals(tPoolModels.get(i), tEntry.get("model").getAsString(),
                        tFacing + " variant " + i + ": pool model order");
                assertFalse(tEntry.has("weight"), tFacing + " variant " + i + ": uniform band, weight 1 omitted");
            }
        }
        // the arm composition: down = no facing rotation, so y alternates 0(omitted)/90
        var tDown = tVariants.getAsJsonArray("facing=down");
        for (int i = 0; i < 8; i++) {
            boolean tHasY = tDown.get(i).getAsJsonObject().has("y");
            assertEquals(tPoolArms.get(i) != 0, tHasY, "down variant " + i + ": arm " + tPoolArms.get(i));
            if (tHasY) assertEquals(tPoolArms.get(i), tDown.get(i).getAsJsonObject().get("y").getAsInt(),
                    "down variant " + i + ": arm y");
        }
        assertFalse(tDown.get(0).getAsJsonObject().has("x"), "down default arm: the floor form, no x");
        // east = facing y 90; the arms stack: 90 / (90+90)%360=180
        assertEquals(90, tVariants.getAsJsonArray("facing=east").get(0).getAsJsonObject().get("y").getAsInt(),
                "east default arm keeps the facing rotationY");
        assertEquals(180, tVariants.getAsJsonArray("facing=east").get(1).getAsJsonObject().get("y").getAsInt(),
                "east twin arm = facing + 90");
        assertFalse(tVariants.getAsJsonArray("facing=west").get(1).getAsJsonObject().has("y"),
                "west twin arm = (270+90)%360 = 0, the omitted JSON default");
        assertEquals(180, tVariants.getAsJsonArray("facing=up").get(0).getAsJsonObject().get("x").getAsInt(),
                "up: ceiling form rotation carried");
    }

    /**
     * Issue #47 follow-up: the sneak-placed stick (shift+stickGt / the vanilla Items.STICK
     * alias, the p32 dispatch) rides the SAME weighted variant band as the worldgen stick —
     * upstream StickPlaced 32073 extends the worldgen stick (placeables/MultiTileEntityStickPlaced
     * over placeables/MultiTileEntityStick, only getDefaultStick overridden), one look. The
     * floor pose is the worldgen facing=down band VERBATIM (the deep-equality pin — the
     * isomorphism the card accepts), the old full-footprint 16x16x2 oak plank model is dead.
     */
    @Test
    void placedStickBlockstateRidesTheSurfaceStickBand() throws Exception {
        JsonObject tState = json("assets/gt6/blockstates/placed_stick.json");
        var tVariants = tState.getAsJsonObject("variants");
        assertEquals(1, tVariants.size(), "the placed pile is stateless — the one '' key");
        var tPlaced = tVariants.getAsJsonArray("");
        var tWorld = json("assets/gt6/blockstates/surface_stick.json").getAsJsonObject("variants")
                .getAsJsonArray("facing=down");
        assertEquals(tWorld, tPlaced, "the placed stick IS the worldgen floor band, entry for entry");
        // the band pinned directly too (the pool in emission order): 2 centered arms,
        // one slide tier per arm, the two tilt models on both arms — uniform weights
        List<String> tModels = List.of("gt6:block/surface_stick", "gt6:block/surface_stick",
                "gt6:block/surface_stick_a", "gt6:block/surface_stick_b",
                "gt6:block/surface_stick_t22", "gt6:block/surface_stick_t22",
                "gt6:block/surface_stick_t45", "gt6:block/surface_stick_t45");
        List<Integer> tArms = List.of(0, 90, 0, 90, 0, 90, 0, 90);
        assertEquals(8, tPlaced.size(), "the full eight-entry stick band");
        for (int i = 0; i < tPlaced.size(); i++) {
            JsonObject tEntry = tPlaced.get(i).getAsJsonObject();
            assertEquals(tModels.get(i), tEntry.get("model").getAsString(), "variant " + i + ": the shared stick model");
            if (tArms.get(i) != 0) {
                assertEquals(tArms.get(i), tEntry.get("y").getAsInt(), "variant " + i + ": the arm rotation");
            } else {
                assertFalse(tEntry.has("y"), "variant " + i + ": arm 0 = the omitted JSON default");
            }
            assertFalse(tEntry.has("x"), "variant " + i + ": the floor pose, no x rotation");
            assertFalse(tEntry.has("weight"), "variant " + i + ": uniform band, weight 1 omitted");
        }
        // the referenced models resolve on the classpath (the offline blockstate->model walk)
        for (String tModel : tModels.stream().distinct().toList()) {
            assertNotNull(json("assets/gt6/models/block/" + tModel.substring("gt6:block/".length()) + ".json"),
                    tModel + " must exist");
        }
        // the old full-footprint plank is retired from the generated tree
        try (InputStream tOld = GT6SurfaceTreeRenderDatagenTest.class.getClassLoader()
                .getResourceAsStream("assets/gt6/models/block/placed_stick.json")) {
            assertNull(tOld, "the 16x16x2 plank model must be gone from the generated tree");
        }
    }

    /**
     * #12 tilt models (debt-issue12-shape-follow-tilt): the default bar with an ELEMENT
     * rotation about its own centre — origin [8,1,8], axis y, angle 22.5/45 (the only
     * angles the grammar validates, BlockElement.java:100), untinted oak borrow kept.
     */
    @Test
    void stickTiltModelsAreTheRotatedBars() throws Exception {
        for (String tPair : List.of("surface_stick_t22:22.5", "surface_stick_t45:45.0")) {
            String tName = tPair.substring(0, tPair.indexOf(':'));
            JsonObject tModel = json("assets/gt6/models/block/" + tName + ".json");
            assertEquals("minecraft:block/oak_log", tModel.getAsJsonObject("textures").get("slab").getAsString(),
                    tName + ": the vanilla oak borrow");
            JsonObject tBox = tModel.getAsJsonArray("elements").get(0).getAsJsonObject();
            assertEquals(List.of(2, 0, 7), tBox.getAsJsonArray("from").asList().stream()
                    .map(e -> e.getAsInt()).toList(), tName + " from = the default centered bar");
            assertEquals(List.of(14, 2, 9), tBox.getAsJsonArray("to").asList().stream()
                    .map(e -> e.getAsInt()).toList(), tName + " to = the default centered bar");
            JsonObject tRotation = tBox.getAsJsonObject("rotation");
            assertEquals("y", tRotation.get("axis").getAsString(), tName + ": the local-Y tilt axis");
            assertEquals(Double.parseDouble(tPair.substring(tPair.indexOf(':') + 1)),
                    tRotation.get("angle").getAsDouble(), 1e-6, tName + ": tilt angle");
            assertEquals(3, tRotation.getAsJsonArray("origin").size(), tName + ": the bar-centre origin");
            assertEquals(8.0, tRotation.getAsJsonArray("origin").get(0).getAsDouble(), 1e-6, tName + " origin x");
            assertEquals(1.0, tRotation.getAsJsonArray("origin").get(1).getAsDouble(), 1e-6, tName + " origin y");
            assertEquals(8.0, tRotation.getAsJsonArray("origin").get(2).getAsDouble(), 1e-6, tName + " origin z");
            assertFalse(tModel.toString().contains("tintindex"), tName + ": untinted");
        }
    }

    /**
     * #12 stick slide tiers: the three bar models are the same 12x2x2 bar slid across Z
     * (centered 7..9 / -2px 5..7 / +2px 9..11 — the upstream readFromNBT2 0..14px
     * perpendicular slide downsampled), untinted oak-log borrow kept.
     */
    @Test
    void stickSlideTierModelsAreTheSameBarSlid() throws Exception {
        List<Integer> tSlideFrom = List.of(7, 5, 9);
        List<String> tTierFiles = List.of("surface_stick", "surface_stick_a", "surface_stick_b");
        for (int i = 0; i < tTierFiles.size(); i++) {
            JsonObject tModel = json("assets/gt6/models/block/" + tTierFiles.get(i) + ".json");
            assertEquals("minecraft:block/oak_log", tModel.getAsJsonObject("textures").get("slab").getAsString(),
                    tTierFiles.get(i) + ": the vanilla oak borrow");
            JsonObject tBox = tModel.getAsJsonArray("elements").get(0).getAsJsonObject();
            assertEquals(List.of(2, 0, tSlideFrom.get(i)), tBox.getAsJsonArray("from").asList().stream()
                    .map(e -> e.getAsInt()).toList(), tTierFiles.get(i) + " from");
            assertEquals(List.of(14, 2, tSlideFrom.get(i) + 2), tBox.getAsJsonArray("to").asList().stream()
                    .map(e -> e.getAsInt()).toList(), tTierFiles.get(i) + " to = same bar, slid");
            assertFalse(tModel.toString().contains("tintindex"), tTierFiles.get(i) + ": untinted");
        }
    }

    /**
     * #12 rock variant band (the same weighted mechanism, the R2 suggestion): three size
     * tiers of the upstream 2..8px-wide x 1..4px-high random micro box, weights 3/2/1
     * (weight 1 omitted = the JSON default), every tier box inside the 8x3x8 envelope
     * 4..12 x 0..3 x 4..12 — the null-pos fallback shape (debt-issue12-shape-follow-tilt:
     * a live position's selection box follows its tier exactly now).
     */
    @Test
    void rockBlockstateIsTheSizedVariantBand() throws Exception {
        JsonObject tState = json("assets/gt6/blockstates/surface_rock_stone.json");
        var tList = tState.getAsJsonObject("variants").getAsJsonArray("facing=down");
        assertEquals(3, tList.size(), "three size tiers");
        assertEquals("gt6:block/surface_rock", tList.get(0).getAsJsonObject().get("model").getAsString());
        assertEquals(3, tList.get(0).getAsJsonObject().get("weight").getAsInt(), "the representative tier heaviest");
        assertEquals("gt6:block/surface_rock_a", tList.get(1).getAsJsonObject().get("model").getAsString());
        assertEquals(2, tList.get(1).getAsJsonObject().get("weight").getAsInt());
        assertEquals("gt6:block/surface_rock_b", tList.get(2).getAsJsonObject().get("model").getAsString());
        assertFalse(tList.get(2).getAsJsonObject().has("weight"), "weight 1 = the omitted JSON default");
        List<Integer> tEnvMin = List.of(4, 0, 4), tEnvMax = List.of(12, 3, 12);
        for (String tTier : List.of("surface_rock", "surface_rock_a", "surface_rock_b")) {
            JsonObject tBox = json("assets/gt6/models/block/" + tTier + ".json")
                    .getAsJsonArray("elements").get(0).getAsJsonObject();
            var tFrom = tBox.getAsJsonArray("from").asList().stream().map(e -> e.getAsInt()).toList();
            var tTo = tBox.getAsJsonArray("to").asList().stream().map(e -> e.getAsInt()).toList();
            for (int a = 0; a < 3; a++) {
                assertTrue(tFrom.get(a) >= tEnvMin.get(a) && tTo.get(a) <= tEnvMax.get(a),
                        tTier + " axis " + a + ": the tier box sits inside the 8x3x8 selection envelope");
            }
        }
    }

    /** #1 rock: the 8x3x8 fixed representative box, tintindex 0 on every face, vanilla stone. */
    @Test
    void rockModelIsTheShrunkTintedBox() throws Exception {
        JsonObject tModel = json("assets/gt6/models/block/surface_rock.json");
        assertEquals("minecraft:block/block", tModel.get("parent").getAsString());
        var tTextures = tModel.getAsJsonObject("textures");
        assertEquals("minecraft:block/stone", tTextures.get("slab").getAsString());
        var tElements = tModel.getAsJsonArray("elements");
        assertEquals(1, tElements.size());
        JsonObject tBox = tElements.get(0).getAsJsonObject();
        assertEquals(List.of(4, 0, 4), tBox.getAsJsonArray("from").asList().stream()
                .map(e -> e.getAsInt()).toList(), "rock from = inside the old 12x3x12 pelt");
        assertEquals(List.of(12, 3, 12), tBox.getAsJsonArray("to").asList().stream()
                .map(e -> e.getAsInt()).toList(), "rock to = the 8x3x8 representative box");
        var tFaces = tBox.getAsJsonObject("faces");
        assertEquals(6, tFaces.size());
        for (String tFaceKey : FACE_KEYS) {
            JsonObject tFace = tFaces.getAsJsonObject(tFaceKey);
            assertEquals("#slab", tFace.get("texture").getAsString());
            assertEquals(0, tFace.get("tintindex").getAsInt(),
                    "rock face " + tFaceKey + ": tintindex 0 — the material tint seat");
        }
    }

    /**
     * Issue #47: the sneak-placed rock pile (shift+rockGt, the p32 dispatch) rides the SAME
     * weighted pebble band as the worldgen rocks — upstream RockPlaced 32074 extends the
     * worldgen rock (placeables/MultiTileEntityRockPlaced over misc/MultiTileEntityRock),
     * one look. The single-state blockstate maps "" to the three shared tiers 3/2/1 with
     * NO rotations, and the old full-footprint 16x16x3 slab model is dead.
     */
    @Test
    void placedRockBlockstateRidesTheSurfaceRockBand() throws Exception {
        JsonObject tState = json("assets/gt6/blockstates/placed_rock.json");
        var tVariants = tState.getAsJsonObject("variants");
        assertEquals(1, tVariants.size(), "the placed pile is stateless — the one '' key");
        var tList = tVariants.getAsJsonArray("");
        assertEquals(3, tList.size(), "the shared three size tiers");
        List<String> tModels = List.of("gt6:block/surface_rock", "gt6:block/surface_rock_a", "gt6:block/surface_rock_b");
        List<Integer> tWeights = List.of(3, 2, 1);
        for (int i = 0; i < tList.size(); i++) {
            JsonObject tEntry = tList.get(i).getAsJsonObject();
            assertEquals(tModels.get(i), tEntry.get("model").getAsString(), "tier " + i + ": the shared rock model");
            if (tWeights.get(i) != 1) {
                assertEquals(tWeights.get(i), tEntry.get("weight").getAsInt(), "tier " + i + ": the band weight");
            } else {
                assertFalse(tEntry.has("weight"), "tier " + i + ": weight 1 = the omitted JSON default");
            }
            assertFalse(tEntry.has("x") || tEntry.has("y"), "tier " + i + ": the floor pose, no rotations");
        }
        // the referenced models resolve on the classpath (the offline blockstate->model walk)
        for (String tModel : tModels) {
            assertNotNull(json("assets/gt6/models/block/" + tModel.substring("gt6:block/".length()) + ".json"),
                    tModel + " must exist");
        }
        // the old full-footprint slab is retired from the generated tree
        try (InputStream tOld = GT6SurfaceTreeRenderDatagenTest.class.getClassLoader()
                .getResourceAsStream("assets/gt6/models/block/placed_rock.json")) {
            assertNull(tOld, "the 16x16x3 slab model must be gone from the generated tree");
        }
    }

    /** #4 rainbowood leaves: every face tintindex 0 + cutout_mipped over the grayscale PNG. */
    @Test
    void rainbowoodLeavesModelCarriesTintIndexZero() throws Exception {
        JsonObject tModel = json("assets/gt6/models/block/rainbowood_leaves.json");
        assertEquals("minecraft:block/block", tModel.get("parent").getAsString());
        assertEquals("minecraft:cutout_mipped", tModel.get("render_type").getAsString(),
                "the vanilla leaves render layer kept");
        assertEquals("gt6:block/tree/leaves_rainbowood", tModel.getAsJsonObject("textures").get("all").getAsString(),
                "the grayscale canvas PNG");
        var tElements = tModel.getAsJsonArray("elements");
        assertEquals(1, tElements.size());
        JsonObject tCube = tElements.get(0).getAsJsonObject();
        var tFaces = tCube.getAsJsonObject("faces");
        assertEquals(6, tFaces.size());
        for (String tFaceKey : FACE_KEYS) {
            JsonObject tFace = tFaces.getAsJsonObject(tFaceKey);
            assertEquals("#all", tFace.get("texture").getAsString());
            assertEquals(0, tFace.get("tintindex").getAsInt(),
                    "rainbowood face " + tFaceKey + ": tintindex 0 — the RAINBOW seat");
        }
    }

    /** #4 the pre-coloured control: a non-rainbow kind keeps its untinted cube_all (untouched band). */
    @Test
    void otherTreeLeavesStayUntinted() throws Exception {
        JsonObject tModel = json("assets/gt6/models/block/maple_leaves.json");
        assertFalse(tModel.toString().contains("tintindex"), "maple: the pre-coloured-PNG form");
        assertEquals("minecraft:block/cube_all", tModel.get("parent").getAsString());
    }

    /** #4 wiring: the rainbowood blockstate/item model still wire exactly the tinted block model. */
    @Test
    void rainbowoodLeavesWiring() throws Exception {
        JsonObject tState = json("assets/gt6/blockstates/rainbowood_leaves.json");
        assertEquals("gt6:block/rainbowood_leaves",
                tState.getAsJsonObject("variants").getAsJsonObject("").get("model").getAsString());
        JsonObject tItem = json("assets/gt6/models/item/rainbowood_leaves.json");
        assertEquals("gt6:block/rainbowood_leaves", tItem.get("parent").getAsString());
    }

    /** The CS.java:330-355 RAINBOW_ARRAY transcription: 24 entries, ARGB-opaque, spot rows. */
    @Test
    void rainbowTableTranscription() {
        assertEquals(24, GT6TreeClientListener.RAINBOW.length, "the upstream table length");
        assertEquals(0xFFFF0000, GT6TreeClientListener.RAINBOW[0], "red head (CS.java:331)");
        assertEquals(0xFF00FF00, GT6TreeClientListener.RAINBOW[8], "pure green mid (CS.java:339)");
        assertEquals(0xFF0000FF, GT6TreeClientListener.RAINBOW[16], "blue (CS.java:347)");
        assertEquals(0xFFFF0040, GT6TreeClientListener.RAINBOW[23], "rose tail (CS.java:354)");
        for (int tColor : GT6TreeClientListener.RAINBOW)
            assertEquals(0xFF000000, tColor & 0xFF000000, "every entry ARGB-opaque (the modern tint face)");
    }
}
