/*
 * Offline pinned tests for task p38-issue1-4 (GitHub #1 + #4): the surface deco band
 * geometry fix and the Rainbowood leaves tint wiring, asserted against the committed
 * src/generated tree (the GT6BarrelPaintRenderDatagenTest shape).
 *
 * <p>#1 ground geometry: the stick is the upstream 14x2x2 lying bar (MultiTileEntityStick
 * .java:53/:58-68, default centered-in-Z pose; the FACING blockstate still emits both
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

    /** #1 stick: the 14x2x2 ground bar over the vanilla oak-log side, NO tintindex (the untinted borrow). */
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
                .map(e -> e.getAsInt()).toList(), "stick to = 14x2x2, not the old 12x2x12 slab");
        var tFaces = tBox.getAsJsonObject("faces");
        assertEquals(6, tFaces.size());
        for (String tFaceKey : FACE_KEYS) {
            JsonObject tFace = tFaces.getAsJsonObject(tFaceKey);
            assertEquals("#slab", tFace.get("texture").getAsString());
            assertFalse(tFace.has("tintindex"), "stick face " + tFaceKey + ": untinted");
        }
    }

    /** #1 stick blockstate: all six FACINGS, the two rotationY bar orientations among them. */
    @Test
    void stickBlockstateKeepsBothBarOrientations() throws Exception {
        JsonObject tState = json("assets/gt6/blockstates/surface_stick.json");
        var tVariants = tState.getAsJsonObject("variants");
        assertEquals(6, tVariants.size(), "the full FACING table");
        assertEquals("gt6:block/surface_stick", tVariants.getAsJsonObject("facing=down").get("model").getAsString());
        assertFalse(tVariants.getAsJsonObject("facing=down").has("y"), "down: the floor form");
        assertEquals(90, tVariants.getAsJsonObject("facing=east").get("y").getAsInt(),
                "east: the Z-long bar arm, rotationY variant 1");
        assertEquals(270, tVariants.getAsJsonObject("facing=west").get("y").getAsInt(),
                "west: the Z-long bar arm, rotationY variant 2");
        assertEquals(180, tVariants.getAsJsonObject("facing=up").get("x").getAsInt(), "up: ceiling form");
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
