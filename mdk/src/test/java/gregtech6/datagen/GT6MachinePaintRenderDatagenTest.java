/*
 * Offline pinned-count tests for task p21-paintable-tint-render: the generated machine
 * blockstate/model JSONs carry tintindex 0 on every face of all three models per machine —
 * the generated-JSON half of the 21x3 census, asserted against the committed src/generated
 * tree (the GT6StoneBlocksRenderDatagenTest split: the write side is gated by runData,
 * first run written>0, second run written:0; the datagen-JVM counter half is the
 * GT6BlockStates runData log line).
 *
 * <p>Census ground truth: the machine domain is oven (1) + shredder/crusher/lathe T1-T4
 * (12) + dryer (4) + distillery (4) = 21 blocks (the GTMachines.paintableBlockArray
 * registration census), three models each (inactive/active/running) = 63 block-model
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
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class GT6MachinePaintRenderDatagenTest {

    /** The 21 machine-domain bases, in registration order (the paintableBlockArray census). */
    private static final List<String> MACHINE_BASES = List.of(
            "oven",
            "shredder", "shredder_t2", "shredder_t3", "shredder_t4",
            "crusher", "crusher_t2", "crusher_t3", "crusher_t4",
            "lathe", "lathe_t2", "lathe_t3", "lathe_t4",
            "dryer", "dryer_t2", "dryer_t3", "dryer_t4",
            "distillery", "distillery_t2", "distillery_t3", "distillery_t4");

    /** The addMachine three-model split (inactive/active/running). */
    private static final List<String> MODEL_SUFFIXES = List.of("", "_active", "_running");

    /** The front-texture state suffix per model (the addMachine texture carriers). */
    private static final List<String> FRONT_SUFFIXES = List.of("_front", "_front_active", "_front_running");

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

    /** The census shape: 21 bases x 3 models = 63 tinted block models. */
    @Test
    void pinnedMachinePaintCensus() {
        assertEquals(21, MACHINE_BASES.size(), "the machine block census (paintableBlockArray)");
        assertEquals(21 * 3, MACHINE_BASES.size() * MODEL_SUFFIXES.size(),
                "21 blocks x 3 models — the pinned tinted-model total");
    }

    /** Every machine block model: the block/cube parent, six textures, one element whose six faces all carry tintindex 0. */
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
                assertEquals("gt6:block/" + tFamily + FRONT_SUFFIXES.get(tSuffix),
                        tTextures.get("north").getAsString(),
                        tModelName + ": the family front texture for this state");
                assertEquals(Set.copyOf(FACE_KEYS), tTextures.keySet(),
                        tModelName + ": the six-texture key set");
                var tElements = tModel.getAsJsonArray("elements");
                assertEquals(1, tElements.size(), tModelName + ": one element");
                JsonObject tElement = tElements.get(0).getAsJsonObject();
                assertEquals(0.0, tElement.getAsJsonArray("from").get(0).getAsDouble(),
                        tModelName + ": element from = the full 0..16 cube");
                assertEquals(16.0, tElement.getAsJsonArray("to").get(0).getAsDouble(),
                        tModelName + ": element to = the full 0..16 cube");
                var tFaces = tElement.getAsJsonObject("faces");
                assertEquals(6, tFaces.size(), tModelName + ": six faces");
                for (String tFaceKey : FACE_KEYS) {
                    JsonObject tFace = tFaces.getAsJsonObject(tFaceKey);
                    assertEquals("#" + tFaceKey, tFace.get("texture").getAsString(),
                            tModelName + " face " + tFaceKey + ": its own texture key");
                    assertEquals(0, tFace.get("tintindex").getAsInt(),
                            tModelName + " face " + tFaceKey + ": tintindex 0 — the paint tint seat");
                    assertEquals(tFaceKey, tFace.get("cullface").getAsString(),
                            tModelName + " face " + tFaceKey + ": the vanilla cube cullface");
                }
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
