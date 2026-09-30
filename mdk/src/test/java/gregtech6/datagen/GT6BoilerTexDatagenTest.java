/*
 * Offline pinned tests for task tex-large-boilers: the two boiler domains swap the
 * single-layer placeholders for the two-layer front-bearing tinted grammar (the
 * {@link GT6ConverterPaintRenderDatagenTest} shape — asserted against the committed
 * generated tree). Upstream semantics pinned:
 * <ul>
 * <li>the Base10 default getTexture2 (TileEntityBase10MultiBlockBase.java:192-194) —
 *     the FRONT face renders the colored_front+overlay_front pair, the other five faces
 *     the plain pair; FACES_TBS (CS.java:618) makes the horizontal front a "side" face,
 *     so the model's north carries {@code *_front_side};</li>
 * <li>the boiler tank has NO upstream front art (FACES_TBS={0,1,2,2,2,2}) — all four
 *     sides share the side art, the front placeholder is retired;</li>
 * <li>the five large boiler rows carry the NBT_MATERIAL column (Loader
 *     :1248-1252, the WALL_ROWS aMat mapping) and the colours are pairwise distinct —
 *     the all-gray single-texture placeholder era is gone;</li>
 * <li>the 18 borrowed PNGs (12 largeboiler + 6 boiler_steam) exist and the five retired
 *     placeholder PNGs plus every generated JSON reference to them are dead.</li>
 * </ul>
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.registry.GTMaterialItems;

class GT6BoilerTexDatagenTest {

    /** facing → rotationY (the addBoilers/addLargeBoiler y table — NORTH identity). */
    private static final java.util.Map<String, Integer> ROT_Y = java.util.Map.of(
            "north", 0, "south", 180, "west", 270, "east", 90);

    @BeforeAll
    static void bootMaterials() {
        // the vanilla bootstrap + material system (the converter-census shape)
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
            // NetworkHooks.init() failure is expected offline
        }
        GTMaterialItems.initMaterials();
    }

    private static InputStream resource(String aPath) {
        InputStream tStream = GT6BoilerTexDatagenTest.class.getClassLoader().getResourceAsStream(aPath);
        return tStream;
    }

    private static JsonObject json(String aPath) throws IOException {
        try (InputStream tStream = resource(aPath)) {
            assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    // ------------------------------------------------------------------
    // the two-layer model grammar
    // ------------------------------------------------------------------

    private void assertBoilerModel(String aName, String aBand, boolean aFront) throws IOException {
        JsonObject tModel = json("assets/gt6/models/block/" + aName + ".json");
        assertEquals("minecraft:block/cube", tModel.get("parent").getAsString(), aName + ": cube parent");
        assertEquals("minecraft:cutout", tModel.get("render_type").getAsString(), aName + ": cutout");
        JsonObject tTextures = tModel.getAsJsonObject("textures");
        // the body layer: the front face carries the colored_front art (aFront — the Base10
        // front-face pair), the boiler tank binds plain side art on all four sides
        assertEquals("gt6:block/" + aBand + (aFront ? "/colored_front_side" : "/colored_side"),
                tTextures.get("north").getAsString(), aName + ": north = the front-face colored art");
        for (String tSide : List.of("south", "west", "east")) {
            assertEquals("gt6:block/" + aBand + "/colored_side", tTextures.get(tSide).getAsString(),
                    aName + ": " + tSide + " = the plain colored side");
        }
        assertEquals("gt6:block/" + aBand + "/colored_bottom", tTextures.get("down").getAsString());
        assertEquals("gt6:block/" + aBand + "/colored_top", tTextures.get("up").getAsString());
        // the overlay shell: front face = the overlay_front art, the rest the plain overlay
        assertEquals("gt6:block/" + aBand + (aFront ? "/overlay_front_side" : "/overlay_side"),
                tTextures.get("overlay_north").getAsString(), aName + ": overlay north = the front-face overlay art");
        for (String tSide : List.of("south", "west", "east")) {
            assertEquals("gt6:block/" + aBand + "/overlay_side", tTextures.get("overlay_" + tSide).getAsString(),
                    aName + ": overlay " + tSide + " = the plain overlay side");
        }
        assertEquals("gt6:block/" + aBand + "/overlay_bottom", tTextures.get("overlay_down").getAsString());
        assertEquals("gt6:block/" + aBand + "/overlay_top", tTextures.get("overlay_up").getAsString());
        // the two-layer split: 7 elements, tintindex 0 on every body face, none on the decals
        var tElements = tModel.getAsJsonArray("elements");
        assertEquals(7, tElements.size(), aName + ": body + 6 decals");
        var tBodyFaces = tElements.get(0).getAsJsonObject().getAsJsonObject("faces");
        assertEquals(6, tBodyFaces.size(), aName + ": the body cube is complete");
        for (String tFace : tBodyFaces.keySet()) {
            assertEquals(0, tBodyFaces.getAsJsonObject(tFace).get("tintindex").getAsInt(),
                    aName + " body face " + tFace + " carries tintindex 0 (the mRGBa seat)");
        }
        for (int i = 1; i < 7; i++) {
            var tDecalFaces = tElements.get(i).getAsJsonObject().getAsJsonObject("faces");
            for (String tFace : tDecalFaces.keySet()) {
                assertTrue(!tDecalFaces.getAsJsonObject(tFace).has("tintindex"),
                        aName + " decal " + i + " face " + tFace + " stays untinted (the P22 split)");
            }
        }
    }

    @Test
    public void largeBoilerModelKeepsTheTwoLayerFrontBearingGrammar() throws Exception {
        assertBoilerModel("large_boiler", "large_boiler", true);
    }

    @Test
    public void steamBoilerTankModelKeepsTheTwoLayerGrammarWithoutFrontArt() throws Exception {
        assertBoilerModel("steam_boiler_tank", "boiler_steam", false);
    }

    // ------------------------------------------------------------------
    // the blockstate rotation tables
    // ------------------------------------------------------------------

    @Test
    public void largeBoilerBlockstatesPinTheFacingFormedVariants() throws Exception {
        for (var tRow : gregtech6.registry.GTMultiBlocks.LARGE_BOILER_ROWS) {
            JsonObject tVariants = json("assets/gt6/blockstates/" + tRow.path() + ".json").getAsJsonObject("variants");
            assertEquals(8, tVariants.size(), tRow.path() + ": exactly the 4 facing x 2 formed variants");
            for (String tFacing : ROT_Y.keySet()) {
                for (boolean tFormed : new boolean[] {false, true}) {
                    String tKey = "facing=" + tFacing + ",formed=" + tFormed;
                    assertTrue(tVariants.has(tKey), tRow.path() + ": the variant key " + tKey);
                    JsonObject tVariant = tVariants.getAsJsonObject(tKey);
                    assertEquals("gt6:block/large_boiler", tVariant.get("model").getAsString(),
                            tRow.path() + " " + tKey + ": the shared band model (the FORMED look is the p9 pool)");
                    assertEquals(ROT_Y.get(tFacing).intValue(),
                            tVariant.has("y") ? tVariant.get("y").getAsInt() : 0,
                            tRow.path() + " " + tKey + ": the rotationY table");
                }
            }
        }
    }

    @Test
    public void boilerTankBlockstatesPinTheFacingVariants() throws Exception {
        assertEquals(26, gregtech6.registry.GT6Boilers.allRows().size(), "the boiler tank census stays 26");
        for (var tRow : gregtech6.registry.GT6Boilers.allRows()) {
            JsonObject tVariants = json("assets/gt6/blockstates/" + tRow.path() + ".json").getAsJsonObject("variants");
            assertEquals(4, tVariants.size(), tRow.path() + ": exactly the 4 facing variants");
            for (String tFacing : ROT_Y.keySet()) {
                JsonObject tVariant = tVariants.getAsJsonObject("facing=" + tFacing);
                assertEquals("gt6:block/steam_boiler_tank", tVariant.get("model").getAsString(),
                        tRow.path() + ": the shared tank model");
                assertEquals(ROT_Y.get(tFacing).intValue(),
                        tVariant.has("y") ? tVariant.get("y").getAsInt() : 0,
                        tRow.path() + " facing=" + tFacing + ": the rotationY table");
            }
        }
    }

    // ------------------------------------------------------------------
    // the five NBT_MATERIAL rows + the tint dispatch
    // ------------------------------------------------------------------

    @Test
    public void largeBoilerRowsCarryTheUpstreamMaterialColumn() {
        // the :1248-1252 aMat column, the WALL_ROWS mapping verbatim
        List<gregapi.oredict.OreDictMaterial> tExpected = List.of(gregapi.data.MT.StainlessSteel, gregapi.data.MT.Invar,
                gregapi.data.MT.Ti, gregapi.data.MT.TungstenSteel, gregapi.data.MT.Ad);
        assertEquals(5, gregtech6.registry.GTMultiBlocks.LARGE_BOILER_ROWS.size(), "the large boiler census stays 5");
        for (int i = 0; i < 5; i++) {
            assertSame(tExpected.get(i), gregtech6.registry.GTMultiBlocks.LARGE_BOILER_ROWS.get(i).mat().get(),
                    "large boiler row " + i + " carries the upstream aMat");
        }
    }

    @Test
    public void fiveBoilerRowsTintWithDistinctMaterialColours() {
        // the all-gray single-texture placeholder era is gone: the five rows resolve five
        // distinct tintindex-0 colours through the tintARGB seam (the unpainted arm)
        Set<Integer> tSeen = new HashSet<>();
        for (var tRow : gregtech6.registry.GTMultiBlocks.LARGE_BOILER_ROWS) {
            gregapi.oredict.OreDictMaterial tMat = tRow.mat().get();
            int tTint = gregtech6.client.render.GTMachinePaintTint.tintARGB(
                    net.minecraftforge.client.model.data.ModelData.EMPTY, tMat, 0);
            assertEquals(0xFF000000, tTint & 0xFF000000, tRow.path() + " binds full alpha");
            assertTrue(tSeen.add(tTint & 0xFFFFFF),
                    tRow.path() + " colour " + Integer.toHexString(tTint) + " is distinct");
        }
    }

    // ------------------------------------------------------------------
    // the borrowed art + the retired placeholders
    // ------------------------------------------------------------------

    @Test
    public void borrowedBoilerTexturesExist() {
        for (String tFace : List.of("bottom", "top", "side")) {
            for (String tLayer : List.of("colored", "colored_front", "overlay", "overlay_front")) {
                assertNotNull(resource("assets/gt6/textures/block/large_boiler/" + tLayer + "_" + tFace + ".png"),
                        "large_boiler/" + tLayer + "_" + tFace + ".png must be borrowed");
            }
            for (String tLayer : List.of("colored", "overlay")) {
                assertNotNull(resource("assets/gt6/textures/block/boiler_steam/" + tLayer + "_" + tFace + ".png"),
                        "boiler_steam/" + tLayer + "_" + tFace + ".png must be borrowed");
            }
        }
    }

    /**
     * The five retired placeholders are gone from disk AND no generated JSON references the
     * old texture paths (the {@code boiler_steam} bare grayscale names, the front
     * placeholder, the {@code large_boiler/main} texture) — the census walk over both
     * resource trees (the GT6PaintableRenderTypeCensusTest filesystem shape).
     */
    @Test
    public void retiredPlaceholdersAreDeadEverywhere() throws Exception {
        Path tMdk = mdkRoot();
        for (String tDead : List.of("boiler_steam/bottom.png", "boiler_steam/top.png", "boiler_steam/side.png",
                "boiler_steam/front.png", "large_boiler/main.png")) {
            assertFalse(Files.exists(tMdk.resolve("src/main/resources/assets/gt6/textures/block").resolve(tDead)),
                    "the retired placeholder must be deleted: " + tDead);
        }
        List<String> tDeadRefs = List.of("boiler_steam/bottom", "boiler_steam/top", "boiler_steam/side",
                "boiler_steam/front", "large_boiler/main");
        try (Stream<Path> tWalk = Files.walk(tMdk.resolve("src/generated/resources/assets/gt6"))) {
            for (Path tFile : tWalk.filter(p -> p.toString().endsWith(".json")).toList()) {
                String tContent = Files.readString(tFile, StandardCharsets.UTF_8);
                for (String tRef : tDeadRefs) {
                    assertFalse(tContent.contains("\"gt6:block/" + tRef + "\""),
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
