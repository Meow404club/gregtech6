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
 * <p>Task p28-b-port-overlay-render completes the six-face form: each model is SEVEN
 * elements — the tinted body cube now bound to the family's OWN colored six-set
 * ({@code <family>_colored_bottom/top/front/back/left/right}, the A-card borrow) plus six
 * thin untinted state decals (the p22 front decal generalized to all six faces), one per
 * face keyed by the upstream art token and mapped per the upstream FACING_ROTATIONS table
 * (CS.java:528-537 — model-space front at north: west carries the RIGHT art, east the
 * LEFT art; the blockstate y rotations reproduce the remaining facings). The decal state
 * trio ("" / _active / _running) switches per blockstate variant exactly as the upstream
 * :1014 pick — the p28 static-art ruling (no BE read).
 *
 * <p>Census ground truth: the machine domain is the oven Heat_T ladder (4, task
 * p27-oven-heat-t-ladder) + shredder/crusher/lathe T1-T4
 * (12) + dryer (4) + distillery (4) + canner (4, task p24-canner-machine) + sifter/
 * compressor/wiremill (12, task p26-w1-sifter-compressor-wiremill) + press (4) +
 * extruder (4, both rows of the last task p26-w1-press-extruder-molds) + the six ULV
 * rows (task p28-c-ulv-machine-ladder) + the four roll-ladder RU families (16, task
 * p29-w1-kinetic-roll-ladder — the RU rollingmill rungs ride tier-suffixed bases while
 * sharing the ULV rung's family texture set) + the six P29 W1 process families (24,
 * task p29-w1-kinetic-process-ladder) + the seven eu-hu families (25, task
 * p29-w1-eu-hu-families) = 119 blocks (the
 * GTMachines.paintableBlockArray
 * registration census), three models each (inactive/active/running) = 357 block-model
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
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class GT6MachinePaintRenderDatagenTest {

    /** The 133 machine-domain bases (the paintableBlockArray census). */
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
            "extruder", "extruder_t2", "extruder_t3", "extruder_t4", // task p26-w1-press-extruder-molds
            "shredder_ulv", "crusher_ulv", "canner_ulv", "sifter_ulv", "wiremill_ulv", "rollingmill", // task p28-c-ulv-machine-ladder
            "rollingmill_t1", "rollingmill_t2", "rollingmill_t3", "rollingmill_t4", // task p29-w1-kinetic-roll-ladder — the RU rungs
            "rollbender", "rollbender_t2", "rollbender_t3", "rollbender_t4",
            "rollformer", "rollformer_t2", "rollformer_t3", "rollformer_t4",
            "clustermill", "clustermill_t2", "clustermill_t3", "clustermill_t4",
            "buzzsaw", "buzzsaw_t2", "buzzsaw_t3", "buzzsaw_t4", // task p29-w1-kinetic-process-ladder
            "squeezer", "squeezer_t2", "squeezer_t3", "squeezer_t4",
            "centrifuge", "centrifuge_t2", "centrifuge_t3", "centrifuge_t4",
            "sluice", "sluice_t2", "sluice_t3", "sluice_t4",
            "sanding_machine", "sanding_machine_t2", "sanding_machine_t3", "sanding_machine_t4",
            "pressure_washer", "pressure_washer_t2", "pressure_washer_t3", "pressure_washer_t4",
            "mixer", "mixer_t2", "mixer_t3", "mixer_t4", // task p29-w1-eu-hu-families
            "electricmixer", "electricmixer_t2", "electricmixer_t3", "electricmixer_t4", // task p29-w1-eu-hu-families
            "electricloom", "electricloom_t2", "electricloom_t3", "electricloom_t4", // task p29-w1-eu-hu-families
            "electricsifter", "electricsifter_t2", "electricsifter_t3", "electricsifter_t4", // task p29-w1-eu-hu-families
            "boxinator", "boxinator_t2", "boxinator_t3", "boxinator_t4", // task p29-w1-eu-hu-families
            "unboxinator", "unboxinator_t2", "unboxinator_t3", "unboxinator_t4", // task p29-w1-eu-hu-families
            "fermenter", // task p29-w1-eu-hu-families
            "autocrafter", "autocrafter_t2", "autocrafter_t3", "autocrafter_t4", "autocrafter_t5", // task p29-w2-eu-special
            "lightning", "lightning_t2", "lightning_t3", "lightning_t4", "lightning_t5", // task p29-w2-eu-special
            "laminator", "laminator_t2", "laminator_t3", "laminator_t4"); // task p29-w2-eu-special
    /** The addMachine three-model split (inactive/active/running). */
    private static final List<String> MODEL_SUFFIXES = List.of("", "_active", "_running");

    /** The decal state suffix per model — the upstream :1014 pick (the p28 static-art ruling). */
    private static final List<String> STATE_SUFFIXES = List.of("", "_active", "_running");

    /** The six body face keys of the block/cube parent. */
    private static final List<String> FACE_KEYS = List.of("down", "up", "north", "south", "west", "east");

    /**
     * Body face → upstream colored art token — the FACING_ROTATIONS row for a north-facing
     * machine (CS.java:528-537 row 2, [side]→index over [bottom,top,left,front,right,back]):
     * west→4=right art, east→2=left art; the blockstate y rotations reproduce the rest.
     */
    private static final Map<String, String> BODY_FACE_ART = Map.of(
            "down", "bottom", "up", "top",
            "north", "front", "south", "back",
            "west", "right", "east", "left");

    /** The upstream overlay art tokens (the :176-203 six-entry array order). */
    private static final List<String> OVERLAY_TOKENS = List.of("front", "back", "left", "right", "top", "bottom");

    /** One expected decal element: the model face, the art-token texture key, the slab box. */
    private record DecalSpec(String face, String key, double[] from, double[] to) {}

    /**
     * The six decals in model element order (1..6), the p22 front-decal geometry
     * (16x16x0.01, floating 0.01 outside the body plane) generalized to every face.
     */
    private static final List<DecalSpec> DECALS = List.of(
            new DecalSpec("north", "overlay_front",
                    new double[] {0.0, 0.0, -0.01}, new double[] {16.0, 16.0, 0.0}),
            new DecalSpec("south", "overlay_back",
                    new double[] {0.0, 0.0, 16.0}, new double[] {16.0, 16.0, 16.01}),
            new DecalSpec("east", "overlay_left", // FACING_ROTATIONS[north][east]=2=left
                    new double[] {16.0, 0.0, 0.0}, new double[] {16.01, 16.0, 16.0}),
            new DecalSpec("west", "overlay_right", // FACING_ROTATIONS[north][west]=4=right
                    new double[] {-0.01, 0.0, 0.0}, new double[] {0.0, 16.0, 16.0}),
            new DecalSpec("down", "overlay_bottom",
                    new double[] {0.0, -0.01, 0.0}, new double[] {16.0, 0.0, 16.0}),
            new DecalSpec("up", "overlay_top",
                    new double[] {0.0, 16.0, 0.0}, new double[] {16.0, 16.01, 16.0}));

    private static JsonObject json(String aPath) throws Exception {
        try (InputStream tStream = GT6MachinePaintRenderDatagenTest.class.getClassLoader()
                .getResourceAsStream(aPath)) {
            assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    /** The tier rows keep the family textures (the p8 texture-base overload); the p28 ULV rows and the p29 W2 _t5 rungs likewise (the addUlvLadder/addEuSpecialFamilies family tokens). */
    private static String familyOf(String aBase) {
        if (aBase.startsWith("rollingmill_t")) return "rollingmill"; // task p29-w1-kinetic-roll-ladder — the RU rungs share the ULV rung's family set
        for (String tTier : new String[] {"_t2", "_t3", "_t4", "_t5"}) {
            if (aBase.endsWith(tTier)) return artTokenOf(aBase.substring(0, aBase.length() - tTier.length()));
        }
        if (aBase.endsWith("_ulv")) return aBase.substring(0, aBase.length() - "_ulv".length()); // task p28-c-ulv-machine-ladder
        return artTokenOf(aBase);
    }

    /**
     * The art token overrides: two p29 process families carry upstream NBT_TEXTURE
     * tokens distinct from their registry path (task p29-w1-kinetic-process-ladder —
     * the Sanding Machine rows ride "sander", the Pressure Washer rows "debarker",
     * the row.texture() column verbatim).
     */
    private static String artTokenOf(String aBase) {
        return switch (aBase) {
            case "sanding_machine" -> "sander";
            case "pressure_washer" -> "debarker";
            default -> aBase;
        };
    }

    private static void assertCoord(JsonObject aElement, String aKey, double[] aExpected, String aName) {
        var tArray = aElement.getAsJsonArray(aKey);
        for (int i = 0; i < aExpected.length; i++) {
            assertEquals(aExpected[i], tArray.get(i).getAsDouble(),
                    aName + " " + aKey + "[" + i + "]");
        }
    }

    /** The census shape: 133 bases x 3 models = 399 tinted block models. */
    @Test
    void pinnedMachinePaintCensus() {
        assertEquals(133, MACHINE_BASES.size(), "the machine block census (paintableBlockArray)");
        assertEquals(133 * 3, MACHINE_BASES.size() * MODEL_SUFFIXES.size(),
                "133 blocks x 3 models — the pinned tinted-model total");
    }

    /**
     * Every machine block model: the block/cube parent, the twelve-texture key set (six
     * body keys + the six overlay art tokens), the full tinted body cube over the family's
     * own colored art, and the six thin untinted state decals (task p28-b-port-overlay-render).
     */
    @Test
    void everyMachineModelCarriesTintIndexZeroOnAllSixFaces() throws Exception {
        for (String tBase : MACHINE_BASES) {
            String tFamily = familyOf(tBase);
            for (int tSuffix = 0; tSuffix < MODEL_SUFFIXES.size(); tSuffix++) {
                String tModelName = tBase + MODEL_SUFFIXES.get(tSuffix);
                String tStateSuffix = STATE_SUFFIXES.get(tSuffix);
                JsonObject tModel = json("assets/gt6/models/block/" + tModelName + ".json");
                assertEquals("minecraft:block/cube", tModel.get("parent").getAsString(),
                        tModelName + ": the block/cube parent (display transforms + particle binding kept)");
                var tTextures = tModel.getAsJsonObject("textures");

                // the body keys — the family's OWN colored art, mapped per FACING_ROTATIONS
                for (String tFaceKey : FACE_KEYS) {
                    assertEquals("gt6:block/" + tFamily + "_colored_" + BODY_FACE_ART.get(tFaceKey),
                            tTextures.get(tFaceKey).getAsString(),
                            tModelName + " body face " + tFaceKey + ": the family colored "
                                    + BODY_FACE_ART.get(tFaceKey) + " art (the FACING_ROTATIONS mapping)");
                }
                // the overlay keys — the family state decal trio for this state
                for (String tToken : OVERLAY_TOKENS) {
                    assertEquals("gt6:block/" + tFamily + "_overlay_" + tToken + tStateSuffix,
                            tTextures.get("overlay_" + tToken).getAsString(),
                            tModelName + ": the family " + tToken + " state decal for this state");
                }
                Set<String> tExpectedKeys = new HashSet<>(FACE_KEYS); // the six body keys…
                for (String tToken : OVERLAY_TOKENS) tExpectedKeys.add("overlay_" + tToken); // …plus the six decals
                assertEquals(tExpectedKeys, tTextures.keySet(),
                        tModelName + ": the twelve-texture key set (six body keys + the six art tokens)");

                var tElements = tModel.getAsJsonArray("elements");
                assertEquals(7, tElements.size(),
                        tModelName + ": body cube + six state decals (the p28 six-face split)");

                // element 0 — the body cube, unchanged from p21: full 0..16, six faces, tintindex 0 each.
                JsonObject tBody = tElements.get(0).getAsJsonObject();
                assertCoord(tBody, "from", new double[] {0.0, 0.0, 0.0}, tModelName + ": body");
                assertCoord(tBody, "to", new double[] {16.0, 16.0, 16.0}, tModelName + ": body");
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

                // elements 1-6 — the state decals: the p22 front-decal form on every face,
                // each a single quad with NO tintindex (the upstream UNCOLOURED overlay
                // layer) and the cullface synced with the body's own face.
                for (int tDecal = 0; tDecal < DECALS.size(); tDecal++) {
                    DecalSpec tSpec = DECALS.get(tDecal);
                    JsonObject tElement = tElements.get(tDecal + 1).getAsJsonObject();
                    String tName = tModelName + " decal " + tSpec.face();
                    assertCoord(tElement, "from", tSpec.from(), tName);
                    assertCoord(tElement, "to", tSpec.to(), tName);
                    var tDecalFaces = tElement.getAsJsonObject("faces");
                    assertEquals(1, tDecalFaces.size(), tName + ": the decal is a single quad");
                    JsonObject tFace = tDecalFaces.getAsJsonObject(tSpec.face());
                    assertEquals("#" + tSpec.key(), tFace.get("texture").getAsString(),
                            tName + ": decal face texture = the " + tSpec.key() + " state art");
                    assertTrue(!tFace.has("tintindex"),
                            tName + ": decal face has NO tintindex — the untinted overlay layer "
                                    + "(BlockTextureDefault(IIcon,boolean) = UNCOLOURED)");
                    assertEquals(tSpec.face(), tFace.get("cullface").getAsString(),
                            tName + ": decal face cullface — syncs its cull with the body");
                }
            }
        }
    }

    /**
     * Every machine blockstate: 16 variants (4 facings x 2 active x 2 running), each wired
     * to exactly the state model the upstream :1014 pick demands and the y rotation the
     * FACING property demands (north 0 = omitted, east 90, south 180, west 270).
     */
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
                String tVariantKey = tEntry.getKey();
                JsonObject tRow = tEntry.getValue().getAsJsonObject();
                tReferenced.add(tRow.get("model").getAsString());
                // the variant key: "active=<b>,facing=<dir>,running=<b>" (the name-sorted order)
                String[] tParts = tVariantKey.split(",");
                assertEquals(3, tParts.length, tBase + " variant " + tVariantKey + ": the three axes");
                boolean tActive = Boolean.parseBoolean(tParts[0].substring("active=".length()));
                String tFacing = tParts[1].substring("facing=".length());
                boolean tRunning = Boolean.parseBoolean(tParts[2].substring("running=".length()));
                // the :1014 state pick — active wins over running, inactive is the bare model
                String tExpectedModel = "gt6:block/" + tBase
                        + (tActive ? "_active" : tRunning ? "_running" : "");
                assertEquals(tExpectedModel, tRow.get("model").getAsString(),
                        tBase + " variant " + tVariantKey + ": the state model");
                // the FACING rotation — the vanilla serializer omits y = 0
                int tExpectedY = switch (tFacing) {
                    case "east" -> 90;
                    case "south" -> 180;
                    case "west" -> 270;
                    default -> 0; // north
                };
                if (tExpectedY == 0) {
                    assertTrue(!tRow.has("y") || tRow.get("y").getAsInt() == 0,
                            tBase + " variant " + tVariantKey + ": north needs no y rotation");
                } else {
                    assertEquals(tExpectedY, tRow.get("y").getAsInt(),
                            tBase + " variant " + tVariantKey + ": the facing y rotation");
                }
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
