/*
 * Offline pinned tests for task tex-placeholder-audit: the static-storage / hopper /
 * anvil placeholder domains upgrade to the probed upstream art (asserted against the
 * committed generated tree, the {@link GT6BoilerTexDatagenTest} shape). Upstream semantics
 * pinned:
 * <ul>
 * <li>the four hit storage groups (lockers/normal, drawers/quad, safes/mechanical,
 *     safes/keylocked — MultiTileEntityLocker.java:100-110, DrawerQuad:132-142,
 *     SafeMechanical:105-111): the sensorModel two-layer faceted form, the body cube the
 *     tintindex-0 seat since task tint-coverage-batch (the decal plates untinted — the
 *     P22 contract; the former tex-placeholder-audit "NO tintindex" unpaint deviation is
 *     retired), the locker/drawer quad carrying distinct top/bottom art, the
 *     safes folding the side art onto the vertical pair;</li>
 * <li>the two hopper groups (automation/hopper + queuehopper, MultiTileEntityHopper
 *     .java:284-293): the boilerModel TBS form with the tint seat OFF and no front art
 *     (FACES_TBS), one model per kind;</li>
 * <li>the anvils (MultiTileEntityAnvil.java:306 getTextureSmooth): per-row cube_all over
 *     the VANILLA smooth material faces — no gt6 PNG exists for the family;</li>
 * <li>the 44 borrowed PNGs exist and the ten retired placeholder PNGs plus every
 *     generated JSON reference to them are dead.</li>
 * </ul>
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class GT6StorageTexDatagenTest {

    /** facing → rotationY (the storage/anvil horizontal band — NORTH identity). */
    private static final Map<String, Integer> ROT_Y = Map.of(
            "north", 0, "south", 180, "west", 270, "east", 90);

    @BeforeAll
    static void bootOffline() {
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
            // NetworkHooks.init() failure is expected offline
        }
    }

    private static InputStream resource(String aPath) {
        return GT6StorageTexDatagenTest.class.getClassLoader().getResourceAsStream(aPath);
    }

    private static JsonObject json(String aPath) throws IOException {
        try (InputStream tStream = resource(aPath)) {
            assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    // ------------------------------------------------------------------
    // the two-layer faceted storage grammar
    // ------------------------------------------------------------------

    private void assertStorageModel(String aBand, boolean aDistinctTB) throws IOException {
        JsonObject tModel = json("assets/gt6/models/block/" + aBand + ".json");
        assertEquals("minecraft:block/cube", tModel.get("parent").getAsString(), aBand + ": cube parent");
        assertEquals("minecraft:cutout", tModel.get("render_type").getAsString(), aBand + ": cutout");
        JsonObject tTextures = tModel.getAsJsonObject("textures");
        assertEquals("gt6:block/" + aBand + "/colored_front", tTextures.get("north").getAsString(),
                aBand + ": north = the front colored art");
        assertEquals("gt6:block/" + aBand + "/colored_back", tTextures.get("south").getAsString(),
                aBand + ": south = the back colored art");
        for (String tSide : List.of("west", "east")) {
            assertEquals("gt6:block/" + aBand + "/colored_side", tTextures.get(tSide).getAsString(),
                    aBand + ": " + tSide + " = the plain colored side");
        }
        String tDown = aDistinctTB ? "colored_bottom" : "colored_side";
        String tUp = aDistinctTB ? "colored_top" : "colored_side";
        assertEquals("gt6:block/" + aBand + "/" + tDown, tTextures.get("down").getAsString(), aBand + ": down");
        assertEquals("gt6:block/" + aBand + "/" + tUp, tTextures.get("up").getAsString(), aBand + ": up");
        assertEquals("gt6:block/" + aBand + "/overlay_front", tTextures.get("overlay_north").getAsString(),
                aBand + ": overlay north");
        assertEquals("gt6:block/" + aBand + "/overlay_back", tTextures.get("overlay_south").getAsString(),
                aBand + ": overlay south");
        assertEquals("gt6:block/" + aBand + "/" + (aDistinctTB ? "overlay_bottom" : "overlay_side"),
                tTextures.get("overlay_down").getAsString(), aBand + ": overlay down");
        assertEquals("gt6:block/" + aBand + "/" + (aDistinctTB ? "overlay_top" : "overlay_side"),
                tTextures.get("overlay_up").getAsString(), aBand + ": overlay up");
        // the two-layer split: 7 elements — the body cube is the tintindex-0 seat since
        // task tint-coverage-batch (the grayscale colored_* art multiplies the row colour,
        // the GT6StorageBlock.materialOf carrier), the six decal plates stay untinted
        // (the P22 contract; the former "NO tintindex — the unpaint deviation" declaration
        // is retired by the same card)
        var tElements = tModel.getAsJsonArray("elements");
        assertEquals(7, tElements.size(), aBand + ": body + 6 decals");
        for (String tFace : tElements.get(0).getAsJsonObject().getAsJsonObject("faces").keySet()) {
            assertEquals(0, tElements.get(0).getAsJsonObject().getAsJsonObject("faces")
                            .getAsJsonObject(tFace).get("tintindex").getAsInt(),
                    aBand + " body face " + tFace + " rides the tintindex-0 seat");
        }
        for (int i = 1; i < 7; i++) {
            var tFaces = tElements.get(i).getAsJsonObject().getAsJsonObject("faces");
            for (String tFace : tFaces.keySet()) {
                assertTrue(!tFaces.getAsJsonObject(tFace).has("tintindex"),
                        aBand + " decal element " + i + " face " + tFace + " stays untinted (the P22 contract)");
            }
        }
    }

    @Test
    public void lockerAndDrawerKeepTheTwoLayerFacetedGrammarWithOwnTopBottom() throws Exception {
        assertStorageModel("locker", true);
        assertStorageModel("drawer", true);
    }

    @Test
    public void safesKeepTheTwoLayerFacetedGrammarOverTheSideTrio() throws Exception {
        // the upstream getTexture2 trio (front/back/side) serves the vertical pair too
        assertStorageModel("safe_mechanical", false);
        assertStorageModel("safe_keylocked", false);
    }

    // ------------------------------------------------------------------
    // the hopper two-layer TBS grammar (the boilerModel form, tint OFF, no front)
    // ------------------------------------------------------------------

    private void assertHopperModel(String aName, String aBand) throws IOException {
        JsonObject tModel = json("assets/gt6/models/block/" + aName + ".json");
        assertEquals("minecraft:block/cube", tModel.get("parent").getAsString(), aName + ": cube parent");
        assertEquals("minecraft:cutout", tModel.get("render_type").getAsString(), aName + ": cutout");
        JsonObject tTextures = tModel.getAsJsonObject("textures");
        // the FACES_TBS trio: all four horizontals carry the side art, NO front key exists
        for (String tSide : List.of("north", "south", "west", "east")) {
            assertEquals("gt6:block/" + aBand + "/colored_side", tTextures.get(tSide).getAsString(),
                    aName + ": " + tSide + " = the plain colored side (FACES_TBS, no front art)");
        }
        assertEquals("gt6:block/" + aBand + "/colored_bottom", tTextures.get("down").getAsString());
        assertEquals("gt6:block/" + aBand + "/colored_top", tTextures.get("up").getAsString());
        for (String tSide : List.of("north", "south", "west", "east")) {
            assertEquals("gt6:block/" + aBand + "/overlay_side", tTextures.get("overlay_" + tSide).getAsString(),
                    aName + ": overlay " + tSide);
        }
        assertEquals("gt6:block/" + aBand + "/overlay_bottom", tTextures.get("overlay_down").getAsString());
        assertEquals("gt6:block/" + aBand + "/overlay_top", tTextures.get("overlay_up").getAsString());
        // 7 elements, the tint seat OFF (the aTint=false arm — the unpaint deviation)
        var tElements = tModel.getAsJsonArray("elements");
        assertEquals(7, tElements.size(), aName + ": body + 6 decals");
        for (int i = 0; i < 7; i++) {
            var tFaces = tElements.get(i).getAsJsonObject().getAsJsonObject("faces");
            for (String tFace : tFaces.keySet()) {
                assertTrue(!tFaces.getAsJsonObject(tFace).has("tintindex"),
                        aName + " element " + i + " face " + tFace + " stays untinted");
            }
        }
    }

    @Test
    public void hoppersKeepTheTwoLayerTbsGrammarWithoutFrontArt() throws Exception {
        assertHopperModel("gt6_hopper", "hopper");
        assertHopperModel("gt6_queuehopper", "queuehopper");
    }

    // ------------------------------------------------------------------
    // the anvil vanilla material faces
    // ------------------------------------------------------------------

    @Test
    public void anvilsPointAtTheVanillaSmoothMaterialFaces() throws Exception {
        assertEquals(2, gregtech6.registry.GT6Anvils.ROWS.size(), "the anvil census stays 2");
        assertEquals("minecraft:block/smooth_stone",
                json("assets/gt6/models/block/stone_anvil.json").getAsJsonObject("textures").get("all").getAsString(),
                "stone anvil = the SET_STONE smooth face");
        assertEquals("minecraft:block/blackstone",
                json("assets/gt6/models/block/blackstone_anvil.json").getAsJsonObject("textures").get("all").getAsString(),
                "blackstone anvil = the vanilla blackstone face (the crafting item)");
    }

    // ------------------------------------------------------------------
    // the blockstate rotation tables
    // ------------------------------------------------------------------

    @Test
    public void storageBlockstatesPinTheFacingVariantsPerKind() throws Exception {
        assertEquals(28, gregtech6.registry.GT6StaticStorages.ROWS.size(), "the storage census stays 28");
        for (var tRow : gregtech6.registry.GT6StaticStorages.ROWS) {
            JsonObject tVariants = json("assets/gt6/blockstates/" + tRow.path() + ".json").getAsJsonObject("variants");
            assertEquals(4, tVariants.size(), tRow.path() + ": exactly the 4 facing variants");
            String tModel = switch (tRow.kind()) {
                case LOCKER -> "gt6:block/locker";
                case DRAWER -> "gt6:block/drawer";
                case SAFE_MECHANICAL -> "gt6:block/safe_mechanical";
                case SAFE_KEYLOCKED -> "gt6:block/safe_keylocked";
                case BOOKSHELF -> "gt6:block/gt6_bookshelf";
                case BOTTLECRATE -> "gt6:block/gt6_bottlecrate";
            };
            for (String tFacing : ROT_Y.keySet()) {
                JsonObject tVariant = tVariants.getAsJsonObject("facing=" + tFacing);
                assertEquals(tModel, tVariant.get("model").getAsString(),
                        tRow.path() + " facing=" + tFacing + ": the kind model");
                assertEquals(ROT_Y.get(tFacing).intValue(),
                        tVariant.has("y") ? tVariant.get("y").getAsInt() : 0,
                        tRow.path() + " facing=" + tFacing + ": the rotationY table");
            }
        }
    }

    @Test
    public void hopperBlockstatesPinTheSixWayFacingVariants() throws Exception {
        assertEquals(120, gregtech6.registry.GT6Hoppers.ROWS.size(), "the hopper census stays 120 (the 60-material loop x the pair)");
        for (var tRow : gregtech6.registry.GT6Hoppers.ROWS) {
            JsonObject tVariants = json("assets/gt6/blockstates/" + tRow.path() + ".json").getAsJsonObject("variants");
            assertEquals(6, tVariants.size(), tRow.path() + ": exactly the 6 facing variants");
            String tModel = "gt6:block/" + (tRow.queue() ? "gt6_queuehopper" : "gt6_hopper");
            for (String tFacing : List.of("north", "south", "west", "east")) {
                JsonObject tVariant = tVariants.getAsJsonObject("facing=" + tFacing);
                assertEquals(tModel, tVariant.get("model").getAsString(), tRow.path() + " " + tFacing);
                assertEquals(ROT_Y.get(tFacing).intValue(),
                        tVariant.has("y") ? tVariant.get("y").getAsInt() : 0,
                        tRow.path() + " " + tFacing + ": the horizontal y table");
            }
            assertEquals(90, tVariants.getAsJsonObject("facing=down").get("x").getAsInt(),
                    tRow.path() + ": the down x=90 (the Piston convention)");
            assertEquals(270, tVariants.getAsJsonObject("facing=up").get("x").getAsInt(),
                    tRow.path() + ": the up x=270");
        }
    }

    @Test
    public void anvilBlockstatesPinTheFacingVariants() throws Exception {
        for (var tRow : gregtech6.registry.GT6Anvils.ROWS) {
            JsonObject tVariants = json("assets/gt6/blockstates/" + tRow.path() + ".json").getAsJsonObject("variants");
            assertEquals(4, tVariants.size(), tRow.path() + ": exactly the 4 facing variants");
            for (String tFacing : ROT_Y.keySet()) {
                JsonObject tVariant = tVariants.getAsJsonObject("facing=" + tFacing);
                assertEquals("gt6:block/" + tRow.path(), tVariant.get("model").getAsString(),
                        tRow.path() + " " + tFacing + ": the per-row model");
            }
        }
    }

    // ------------------------------------------------------------------
    // the borrowed art + the retired placeholders
    // ------------------------------------------------------------------

    @Test
    public void borrowedStorageAndHopperTexturesExist() {
        for (String tBand : List.of("locker", "drawer")) {
            for (String tFace : List.of("front", "back", "side", "top", "bottom")) {
                for (String tLayer : List.of("colored", "overlay")) {
                    assertNotNull(resource("assets/gt6/textures/block/" + tBand + "/" + tLayer + "_" + tFace + ".png"),
                            tBand + "/" + tLayer + "_" + tFace + ".png must be borrowed");
                }
            }
        }
        for (String tBand : List.of("safe_mechanical", "safe_keylocked")) {
            for (String tFace : List.of("front", "back", "side")) {
                for (String tLayer : List.of("colored", "overlay")) {
                    assertNotNull(resource("assets/gt6/textures/block/" + tBand + "/" + tLayer + "_" + tFace + ".png"),
                            tBand + "/" + tLayer + "_" + tFace + ".png must be borrowed");
                }
            }
        }
        for (String tBand : List.of("hopper", "queuehopper")) {
            for (String tFace : List.of("bottom", "top", "side")) {
                for (String tLayer : List.of("colored", "overlay")) {
                    assertNotNull(resource("assets/gt6/textures/block/" + tBand + "/" + tLayer + "_" + tFace + ".png"),
                            tBand + "/" + tLayer + "_" + tFace + ".png must be borrowed");
                }
            }
        }
    }

    /**
     * The ten retired placeholders are gone from disk AND no generated JSON references the
     * old texture paths (the {@code retiredPlaceholdersAreDeadEverywhere} census walk).
     */
    @Test
    public void retiredPlaceholdersAreDeadEverywhere() throws Exception {
        Path tMdk = mdkRoot();
        for (String tDead : List.of("locker_front.png", "locker_side.png", "drawer_front.png", "drawer_side.png",
                "safe_front.png", "safe_side.png", "hopper_front.png", "hopper_side.png",
                "anvil_top.png", "anvil_side.png")) {
            assertFalse(Files.exists(tMdk.resolve("src/main/resources/assets/gt6/textures/block").resolve(tDead)),
                    "the retired placeholder must be deleted: " + tDead);
        }
        List<String> tDeadRefs = List.of("block/locker_front", "block/locker_side", "block/drawer_front",
                "block/drawer_side", "block/safe_front", "block/safe_side", "block/hopper_front",
                "block/hopper_side", "block/anvil_top", "block/anvil_side");
        try (Stream<Path> tWalk = Files.walk(tMdk.resolve("src/generated/resources/assets/gt6"))) {
            for (Path tFile : tWalk.filter(p -> p.toString().endsWith(".json")).toList()) {
                String tContent = Files.readString(tFile, StandardCharsets.UTF_8);
                for (String tRef : tDeadRefs) {
                    assertFalse(tContent.contains("\"gt6:" + tRef + "\""),
                            tFile.getFileName() + " still references the retired texture " + tRef);
                }
            }
        }
    }

    /** Location of the mdk project root (the census walk). */
    private static Path mdkRoot() {
        for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
            if (Files.isRegularFile(p.resolve("tools").resolve("gen_textures.py"))) {
                return p;
            }
        }
        throw new AssertionError("mdk root (tools/gen_textures.py) not found upward from "
                + Path.of("").toAbsolutePath());
    }
}
