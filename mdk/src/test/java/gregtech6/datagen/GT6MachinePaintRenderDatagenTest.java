/*
 * Offline pinned-count tests for task p21-paintable-tint-render: the generated machine
 * blockstate/model JSONs carry tintindex 0 on every BODY face of all three models per
 * machine — the generated-JSON half of the 21x3 census, asserted against the committed
 * src/generated tree (the GT6StoneBlocksRenderDatagenTest split: the write side is gated
 * by runData, first run written>0, second run written:0; the datagen-JVM counter half is
 * the GT6BlockStates runData log line).
 *
 * <p>Task p22-paint-front-overlay-split extends the shape: each model is TWO elements —
 * the tinted body cube (p21, unchanged) plus a thin front decal with NO tintindex
 * (the upstream two-layer getTexture2 form: the state overlay layer is UNCOLOURED,
 * MultiTileEntityBasicMachine.java:1014 + BlockTextureDefault.java:179-180), so a painted
 * machine no longer re-tints its active/running state decal. The p20 baked-front
 * composites are retired for the separate colored/_colored_front + _overlay_front*
 * borrows (assets/README.md).
 *
 * <p>Census ground truth: the machine domain is the oven Heat_T ladder (4, task
 * p27-oven-heat-t-ladder) + shredder/crusher/lathe T1-T4
 * (12) + dryer (4) + distillery (4) + canner (4, task p24-canner-machine) + sifter/
 * compressor/wiremill (12, task p26-w1-sifter-compressor-wiremill) + press (4) +
 * extruder (4, both rows of the last task p26-w1-press-extruder-molds) = 48 blocks (the
 * GTMachines.paintableBlockArray
 * registration census), three models each (inactive/active/running) = 144 block-model
 * JSONs. Upstream canonical: every faced face multiplies the grayscale texture by mRGBa
 * (MultiTileEntityBasicMachine.java:1014), so all three models tint identically.
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class GT6MachinePaintRenderDatagenTest {

    /** The 48 machine-domain bases, in registration order (the paintableBlockArray census). */
    private static final List<String> MACHINE_BASES = List.of(
            "oven", "oven_t2", "oven_t3", "oven_t4", // task p27-oven-heat-t-ladder
            "shredder", "shredder_t2", "shredder_t3", "shredder_t4",
            "crusher", "crusher_t2", "crusher_t3", "crusher_t4",
            "lathe", "lathe_t2", "lathe_t3", "lathe_t4",
            "dryer", "dryer_t2", "dryer_t3", "dryer_t4",
            "distillery", "distillery_t2", "distillery_t3", "distillery_t4",
            "canner", "canner_t2", "canner_t3", "canner_t4",
            "sifter", "sifter_t2", "sifter_t3", "sifter_t4",
            "compressor", "compressor_t2", "compressor_t3", "compressor_t4",
            "wiremill", "wiremill_t2", "wiremill_t3", "wiremill_t4",
            "press", "press_t2", "press_t3", "press_t4", // task p26-w1-press-extruder-molds
            "extruder", "extruder_t2", "extruder_t3", "extruder_t4"); // task p26-w1-press-extruder-molds

    /** The addMachine three-model split (inactive/active/running). */
    private static final List<String> MODEL_SUFFIXES = List.of("", "_active", "_running");

    /** The overlay-texture state suffix per model (the p22 split-front decal carriers). */
    private static final List<String> OVERLAY_SUFFIXES =
            List.of("_overlay_front", "_overlay_front_active", "_overlay_front_running");

    private static final List<String> FACE_KEYS = List.of("down", "up", "north", "south", "west", "east");

    private static JsonObject json(String aPath) throws Exception {
        try (InputStream tStream = GT6MachinePaintRenderDatagenTest.class.getClassLoader()
                .getResourceAsStream(aPath)) {
            assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    /** The tier rows keep the family front textures (the p8 texture-base overload). */
    private static String familyOf(String aBase) {
        for (String tTier : new String[] {"_t2", "_t3", "_t4"}) {
            if (aBase.endsWith(tTier)) return aBase.substring(0, aBase.length() - tTier.length());
        }
        return aBase;
    }

    /** The census shape: 48 bases x 3 models = 144 tinted block models. */
    @Test
    void pinnedMachinePaintCensus() {
        assertEquals(48, MACHINE_BASES.size(), "the machine block census (paintableBlockArray)");
        assertEquals(48 * 3, MACHINE_BASES.size() * MODEL_SUFFIXES.size(),
                "48 blocks x 3 models — the pinned tinted-model total");
    }

    /** Every machine block model: the block/cube parent, the seven-texture key set, the full tinted body cube + the thin untinted front decal (task p22-paint-front-overlay-split). */
    @Test
    void everyMachineModelCarriesTintIndexZeroOnAllSixFaces() throws Exception {
        for (String tBase : MACHINE_BASES) {
            String tFamily = familyOf(tBase);
            for (int tSuffix = 0; tSuffix < MODEL_SUFFIXES.size(); tSuffix++) {
                String tModelName = tBase + MODEL_SUFFIXES.get(tSuffix);
                JsonObject tModel = json("assets/gt6/models/block/" + tModelName + ".json");
                assertEquals("minecraft:block/cube", tModel.get("parent").getAsString(),
                        tModelName + ": the block/cube parent (display transforms + particle binding kept)");
                var tTextures = tModel.getAsJsonObject("textures");
                assertEquals("gt6:block/" + tFamily + "_colored_front",
                        tTextures.get("north").getAsString(),
                        tModelName + ": the plain grayscale family front (the P22 colored/ borrow)");
                assertEquals("gt6:block/" + tFamily + OVERLAY_SUFFIXES.get(tSuffix),
                        tTextures.get("overlay").getAsString(),
                        tModelName + ": the family state decal for this state");
                Set<String> tExpectedKeys = new HashSet<>(FACE_KEYS); // the six body keys…
                tExpectedKeys.add("overlay"); // …plus the decal key
                assertEquals(tExpectedKeys, tTextures.keySet(),
                        tModelName + ": the seven-texture key set (six body keys + the decal)");
                var tElements = tModel.getAsJsonArray("elements");
                assertEquals(2, tElements.size(), tModelName + ": body cube + front decal (the p22 split)");

                // element 0 — the body cube, unchanged from p21: full 0..16, six faces, tintindex 0 each.
                JsonObject tBody = tElements.get(0).getAsJsonObject();
                assertEquals(0.0, tBody.getAsJsonArray("from").get(0).getAsDouble(),
                        tModelName + ": body from = the full 0..16 cube");
                assertEquals(16.0, tBody.getAsJsonArray("to").get(0).getAsDouble(),
                        tModelName + ": body to = the full 0..16 cube");
                var tFaces = tBody.getAsJsonObject("faces");
                assertEquals(6, tFaces.size(), tModelName + ": six body faces");
                for (String tFaceKey : FACE_KEYS) {
                    JsonObject tFace = tFaces.getAsJsonObject(tFaceKey);
                    assertEquals("#" + tFaceKey, tFace.get("texture").getAsString(),
                            tModelName + " face " + tFaceKey + ": its own texture key");
                    assertEquals(0, tFace.get("tintindex").getAsInt(),
                            tModelName + " face " + tFaceKey + ": tintindex 0 — the paint tint seat");
                    assertEquals(tFaceKey, tFace.get("cullface").getAsString(),
                            tModelName + " face " + tFaceKey + ": the vanilla cube cullface");
                }

                // element 1 — the front decal: 16x16x0.01 floating 0.01 north of the body
                // plane, one north face, NO tintindex (the upstream UNCOLOURED overlay layer),
                // cullface north syncing its cull with the body's own north face.
                JsonObject tDecal = tElements.get(1).getAsJsonObject();
                assertEquals(0.0, tDecal.getAsJsonArray("from").get(0).getAsDouble(),
                        tModelName + ": decal from x = full width");
                assertEquals(-0.01, tDecal.getAsJsonArray("from").get(2).getAsDouble(),
                        tModelName + ": decal from z = 0.01 north of the body plane (anti z-fight)");
                assertEquals(16.0, tDecal.getAsJsonArray("to").get(0).getAsDouble(),
                        tModelName + ": decal to x = full width");
                assertEquals(0.0, tDecal.getAsJsonArray("to").get(2).getAsDouble(),
                        tModelName + ": decal to z = flush with the body plane");
                var tDecalFaces = tDecal.getAsJsonObject("faces");
                assertEquals(1, tDecalFaces.size(), tModelName + ": the decal is a single north quad");
                JsonObject tDecalNorth = tDecalFaces.getAsJsonObject("north");
                assertEquals("#overlay", tDecalNorth.get("texture").getAsString(),
                        tModelName + ": decal face texture = the state decal");
                assertTrue(!tDecalNorth.has("tintindex"),
                        tModelName + ": decal face has NO tintindex — the untinted overlay layer "
                                + "(BlockTextureDefault(IIcon,boolean) = UNCOLOURED)");
                assertEquals("north", tDecalNorth.get("cullface").getAsString(),
                        tModelName + ": decal face cullface north — syncs its cull with the body");
            }
        }
    }

    /** Every machine blockstate: 16 variants (4 facings x 2 active x 2 running) over exactly the three tinted models. */
    @Test
    void machineBlockstatesWireExactlyTheThreeTintedModels() throws Exception {
        for (String tBase : MACHINE_BASES) {
            JsonObject tState = json("assets/gt6/blockstates/" + tBase + ".json");
            assertTrue(tState.has("variants"), tBase + ": the plain-variants form (no multipart)");
            var tVariants = tState.getAsJsonObject("variants");
            assertEquals(16, tVariants.size(), tBase + ": 4 facings x 2 active x 2 running");
            Set<String> tReferenced = new TreeSet<>();
            List<String> tModelKeys = new ArrayList<>();
            for (String tSuffix : MODEL_SUFFIXES) tModelKeys.add("gt6:block/" + tBase + tSuffix);
            for (var tEntry : tVariants.entrySet()) {
                JsonObject tRow = tEntry.getValue().getAsJsonObject();
                tReferenced.add(tRow.get("model").getAsString());
            }
            assertEquals(new TreeSet<>(tModelKeys), tReferenced,
                    tBase + ": the variant table wires exactly the three tinted models");
        }
    }

    /** The machine item models parent the block models and carry no tintindex themselves (a BlockColor does not colour the BlockItem). */
    @Test
    void machineItemModelsParentBlockModelsWithoutOwnTint() throws Exception {
        for (String tBase : MACHINE_BASES) {
            JsonObject tItem = json("assets/gt6/models/item/" + tBase + ".json");
            assertEquals("gt6:block/" + tBase, tItem.get("parent").getAsString(),
                    tBase + ": the item parent");
            assertTrue(!tItem.toString().contains("tintindex"),
                    tBase + ": no own tint — the inventory half is the pooled item-domain card");
        }
    }
}
