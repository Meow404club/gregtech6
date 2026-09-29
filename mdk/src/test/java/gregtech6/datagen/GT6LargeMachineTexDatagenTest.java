/*
 * Offline pinned tests for task r8-tex-large-machines: the 17 large-controller domains
 * swap the single-layer tintless cubes for the two-layer grammar (the
 * {@link GT6BoilerTexDatagenTest} shape — asserted against the committed generated
 * tree). Upstream semantics pinned:
 * <ul>
 * <li>the 15 basicmachines families (the twelve large-12 rows + the massfab/fusion/
 *     implosion controllers) render the familyMachineModel state trio — the upstream
 *     :1014 pick (mActive ? Active : mRunning ? Running : Inactive) over the six-face
 *     arrays, the body cube tintindex 0 (the mRGBa seat), the six decals untinted;</li>
 * <li>the blockstate stays static on the inactive model (the controller block carries
 *     no ACTIVE property — the GTMultiBlockControllerBlock FACING+FORMED pair), the
 *     _active/_running models ride the tree with the switch declared defer;</li>
 * <li>the two front-pair mains (von_da_graagg, bedrock_drill) render the Base10 default
 *     getTexture2 (TileEntityBase10MultiBlockBase.java:192-194): the FRONT face the
 *     colored_front+overlay_front pair, the other five the plain pair — no upstream
 *     active group (the census card's "bedrockdrill overlay_active" reading disprobed);</li>
 * <li>all 17 upstream rows carry NBT_MATERIAL (Loader :1228-1283) and the unpainted
 *     tints resolve pairwise distinct through the GTMachinePaintTint dispatch arm;</li>
 * <li>the 286 borrowed PNGs (+3 animation sidecars) exist, and the retired flat
 *     vondagraagg/bedrockdrill colored spread is dead on disk and in the JSON refs.</li>
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

class GT6LargeMachineTexDatagenTest {

    /** The 15 familyMachineModel families: the twelve rows' texture tokens + the three mains. */
    private static final List<String> TRIO_FAMILIES = List.of(
            "largecentrifuge", "largeelectrolyzer", "largecoagulator", "largeautoclave",
            "largebath", "largemixer", "largefermenter", "largeoven", "largesluice",
            "largecrusher", "largeshredder", "largesqueezer", "largemassfab",
            "fusionreactor", "implosioncompressor");

    /** The blockstate model name per trio family (the row paths + the three mains). */
    private static final List<String> TRIO_BLOCKS = List.of(
            "large_centrifuge", "large_electrolyzer", "large_coagulator", "large_autoclave",
            "large_bath", "large_batch_mixer", "large_fermenter", "large_electric_oven",
            "large_sluice", "large_crusher", "large_shredder", "large_squeezer",
            "large_massfab", "fusion_reactor", "implosion_compressor");

    /** facing → rotationY (the addLargeMachines y table — NORTH identity). */
    private static final java.util.Map<String, Integer> ROT_Y = java.util.Map.of(
            "north", 0, "south", 180, "west", 270, "east", 90);

    @BeforeAll
    static void bootMaterials() {
        // the vanilla bootstrap + material system (the boiler-census shape)
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
            // NetworkHooks.init() failure is expected offline
        }
        GTMaterialItems.initMaterials();
    }

    private static InputStream resource(String aPath) {
        return GT6LargeMachineTexDatagenTest.class.getClassLoader().getResourceAsStream(aPath);
    }

    private static JsonObject json(String aPath) throws IOException {
        try (InputStream tStream = resource(aPath)) {
            assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    // ------------------------------------------------------------------
    // the two-layer state-trio grammar
    // ------------------------------------------------------------------

    private void assertTrioModel(String aName, String aFamily, String aSuffix) throws IOException {
        JsonObject tModel = json("assets/gt6/models/block/" + aName + ".json");
        assertEquals("minecraft:block/cube", tModel.get("parent").getAsString(), aName + ": cube parent");
        assertEquals("minecraft:cutout", tModel.get("render_type").getAsString(), aName + ": cutout");
        JsonObject tTextures = tModel.getAsJsonObject("textures");
        // the body layer: the familyMachineModel face mapping (front on north, the
        // FACING_ROTATIONS west/right + east/left mirror)
        assertEquals("gt6:block/" + aFamily + "_colored_front", tTextures.get("north").getAsString(),
                aName + ": north = the colored front");
        assertEquals("gt6:block/" + aFamily + "_colored_back", tTextures.get("south").getAsString(),
                aName + ": south = the colored back");
        assertEquals("gt6:block/" + aFamily + "_colored_right", tTextures.get("west").getAsString(),
                aName + ": west carries the right art");
        assertEquals("gt6:block/" + aFamily + "_colored_left", tTextures.get("east").getAsString(),
                aName + ": east carries the left art");
        assertEquals("gt6:block/" + aFamily + "_colored_bottom", tTextures.get("down").getAsString());
        assertEquals("gt6:block/" + aFamily + "_colored_top", tTextures.get("up").getAsString());
        // the state decal layer: the six art tokens with the state suffix
        for (String tArt : List.of("front", "back", "left", "right", "top", "bottom")) {
            assertEquals("gt6:block/" + aFamily + "_overlay_" + tArt + aSuffix,
                    tTextures.get("overlay_" + tArt).getAsString(),
                    aName + ": overlay_" + tArt + " = the " + (aSuffix.isEmpty() ? "inactive" : aSuffix) + " decal");
        }
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
    public void trioFamiliesKeepTheTwoLayerStateTrio() throws Exception {
        assertEquals(15, TRIO_FAMILIES.size(), "the state-trio census stays 15");
        for (String tFamily : TRIO_FAMILIES) {
            assertTrioModel(tFamily, tFamily, "");
            assertTrioModel(tFamily + "_active", tFamily, "_active");
            assertTrioModel(tFamily + "_running", tFamily, "_running");
        }
    }

    /**
     * The defer declaration pinned: the 15 trio blockstates wire ONLY the inactive model
     * (8 FACING x FORMED variants, the addLargeMachines y table) while the _active/
     * _running models exist on the classpath — the property seat is the follow-up card's.
     */
    @Test
    public void trioBlockstatesStayStaticOnTheInactiveModel() throws Exception {
        assertEquals(15, TRIO_BLOCKS.size(), "the trio block census stays 15");
        for (int i = 0; i < 15; i++) {
            String tBlock = TRIO_BLOCKS.get(i);
            String tModel = "gt6:block/" + TRIO_FAMILIES.get(i);
            JsonObject tVariants = json("assets/gt6/blockstates/" + tBlock + ".json").getAsJsonObject("variants");
            assertEquals(8, tVariants.size(), tBlock + ": exactly the 4 facing x 2 formed variants");
            for (String tFacing : ROT_Y.keySet()) {
                for (boolean tFormed : new boolean[] {false, true}) {
                    String tKey = "facing=" + tFacing + ",formed=" + tFormed;
                    assertTrue(tVariants.has(tKey), tBlock + ": the variant key " + tKey);
                    JsonObject tVariant = tVariants.getAsJsonObject(tKey);
                    assertEquals(tModel, tVariant.get("model").getAsString(),
                            tBlock + " " + tKey + ": the static inactive model (the defer declaration)");
                    assertEquals(ROT_Y.get(tFacing).intValue(),
                            tVariant.has("y") ? tVariant.get("y").getAsInt() : 0,
                            tBlock + " " + tKey + ": the rotationY table");
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // the two front-pair mains
    // ------------------------------------------------------------------

    private void assertFrontPairModel(String aName, String aBand) throws IOException {
        JsonObject tModel = json("assets/gt6/models/block/" + aName + ".json");
        assertEquals("minecraft:block/cube", tModel.get("parent").getAsString(), aName + ": cube parent");
        assertEquals("minecraft:cutout", tModel.get("render_type").getAsString(), aName + ": cutout");
        JsonObject tTextures = tModel.getAsJsonObject("textures");
        // the boilerModel front pair: north = the front art, the other five the plain side
        assertEquals("gt6:block/" + aBand + "/colored_front_side", tTextures.get("north").getAsString(),
                aName + ": north = the front-face colored art");
        for (String tSide : List.of("south", "west", "east")) {
            assertEquals("gt6:block/" + aBand + "/colored_side", tTextures.get(tSide).getAsString(),
                    aName + ": " + tSide + " = the plain colored side");
        }
        assertEquals("gt6:block/" + aBand + "/colored_bottom", tTextures.get("down").getAsString());
        assertEquals("gt6:block/" + aBand + "/colored_top", tTextures.get("up").getAsString());
        assertEquals("gt6:block/" + aBand + "/overlay_front_side", tTextures.get("overlay_north").getAsString(),
                aName + ": overlay north = the front-face overlay art");
        for (String tSide : List.of("south", "west", "east")) {
            assertEquals("gt6:block/" + aBand + "/overlay_side", tTextures.get("overlay_" + tSide).getAsString(),
                    aName + ": overlay " + tSide + " = the plain overlay side");
        }
        assertEquals("gt6:block/" + aBand + "/overlay_bottom", tTextures.get("overlay_down").getAsString());
        assertEquals("gt6:block/" + aBand + "/overlay_top", tTextures.get("overlay_up").getAsString());
        // the two-layer split: tintindex 0 body, untinted decals
        var tElements = tModel.getAsJsonArray("elements");
        assertEquals(7, tElements.size(), aName + ": body + 6 decals");
        var tBodyFaces = tElements.get(0).getAsJsonObject().getAsJsonObject("faces");
        for (String tFace : tBodyFaces.keySet()) {
            assertEquals(0, tBodyFaces.getAsJsonObject(tFace).get("tintindex").getAsInt(),
                    aName + " body face " + tFace + " carries tintindex 0");
        }
    }

    @Test
    public void frontPairMainsKeepTheBoilerModelGrammar() throws Exception {
        assertFrontPairModel("von_da_graagg", "vondagraagg");
        assertFrontPairModel("bedrock_drill", "bedrockdrill");
        // no upstream active group — ONE static band each (the disprobe pin)
        for (String tState : List.of("_active", "_running")) {
            assertFalse(resource("assets/gt6/models/block/von_da_graagg" + tState + ".json") != null,
                    "von_da_graagg ships no upstream active group — no state band");
            assertFalse(resource("assets/gt6/models/block/bedrock_drill" + tState + ".json") != null,
                    "bedrock_drill ships no upstream active group — no state band");
        }
    }

    @Test
    public void frontPairBlockstatesPinTheFacingFormedVariants() throws Exception {
        for (String tBlock : List.of("von_da_graagg", "bedrock_drill")) {
            JsonObject tVariants = json("assets/gt6/blockstates/" + tBlock + ".json").getAsJsonObject("variants");
            assertEquals(8, tVariants.size(), tBlock + ": exactly the 4 facing x 2 formed variants");
            for (String tFacing : ROT_Y.keySet()) {
                JsonObject tVariant = tVariants.getAsJsonObject("facing=" + tFacing + ",formed=false");
                assertEquals("gt6:block/" + tBlock, tVariant.get("model").getAsString(),
                        tBlock + ": the shared band model");
                assertEquals(ROT_Y.get(tFacing).intValue(),
                        tVariant.has("y") ? tVariant.get("y").getAsInt() : 0,
                        tBlock + " facing=" + tFacing + ": the rotationY table");
            }
        }
    }

    // ------------------------------------------------------------------
    // the 17 NBT_MATERIAL rows + the tint dispatch
    // ------------------------------------------------------------------

    /** The upstream aMat column, Loader :1228-1283 verbatim (row order = TRIO_BLOCKS then the two mains). */
    private static List<gregapi.oredict.OreDictMaterial> expectedMaterials() {
        return List.of(
                gregapi.data.MT.TungstenSteel, gregapi.data.MT.StainlessSteel, gregapi.data.MT.StainlessSteel,
                gregapi.data.MT.StainlessSteel, gregapi.data.MT.StainlessSteel, gregapi.data.MT.StainlessSteel,
                gregapi.data.MT.StainlessSteel, gregapi.data.MT.Invar, gregapi.data.MT.Ti,
                gregapi.data.MT.TungstenSteel, gregapi.data.MT.TungstenSteel, gregapi.data.ANY.Steel,
                gregapi.data.MT.Pb, gregapi.data.MT.SteelGalvanized, gregapi.data.MT.TungstenSteel,
                gregapi.data.MT.SteelGalvanized, gregapi.data.MT.Ti);
    }

    /** The 17 controller blocks (the twelve rows + the five mains), registration order. */
    private static List<net.minecraft.world.level.block.Block> controllerBlocks() {
        List<net.minecraft.world.level.block.Block> rBlocks = new java.util.ArrayList<>();
        for (var tBlock : gregtech6.registry.GT6LargeMachines.blockArray()) rBlocks.add(tBlock);
        rBlocks.add(gregtech6.registry.GTMultiBlocks.IMPLOSION_COMPRESSOR.get());
        rBlocks.add(gregtech6.registry.GTMultiBlocks.VON_DA_GRAAGG.get());
        rBlocks.add(gregtech6.registry.GTMultiBlocks.MASSFAB.get());
        rBlocks.add(gregtech6.registry.GTMultiBlocks.FUSION_REACTOR.get());
        rBlocks.add(gregtech6.registry.GTMultiBlocks.BEDROCK_DRILL.get());
        return rBlocks;
    }

    /**
     * The dispatch arm pinned reflectively (the CreativeTabJoinCensusTest discipline —
     * tintMaterialOf is the package-private pure seam): every controller resolves its
     * upstream NBT_MATERIAL, and the unpainted tint colours are pairwise distinct over
     * the 17 (the all-gray single-texture placeholder era is gone).
     */
    @Test
    public void seventeenRowsTintWithDistinctUpstreamMaterials() throws Exception {
        List<gregapi.oredict.OreDictMaterial> tExpected = expectedMaterials();
        List<net.minecraft.world.level.block.Block> tBlocks = controllerBlocks();
        assertEquals(17, tBlocks.size(), "the large-controller census stays 17");
        var tMethod = Class.forName("gregtech6.client.render.GTMachinePaintTint")
                .getDeclaredMethod("tintMaterialOf", net.minecraft.world.level.block.Block.class);
        tMethod.setAccessible(true);
        Set<Integer> tSeen = new HashSet<>();
        for (int i = 0; i < 17; i++) {
            gregapi.oredict.OreDictMaterial tMat =
                    (gregapi.oredict.OreDictMaterial) tMethod.invoke(null, tBlocks.get(i));
            assertSame(tExpected.get(i), tMat, "controller " + i + " carries the upstream aMat");
            int tTint = gregtech6.client.render.GTMachinePaintTint.tintARGB(
                    net.minecraftforge.client.model.data.ModelData.EMPTY, tMat, 0);
            assertEquals(0xFF000000, tTint & 0xFF000000, tBlocks.get(i) + " binds full alpha");
            assertTrue(tSeen.add(tTint & 0xFFFFFF),
                    tBlocks.get(i) + " colour " + Integer.toHexString(tTint) + " is distinct");
        }
    }

    // ------------------------------------------------------------------
    // the borrowed art + the retired spread
    // ------------------------------------------------------------------

    @Test
    public void borrowedLargeMachineTexturesExist() {
        // the 15 state trios: 6 faces x (overlay + active + running)
        for (String tFamily : TRIO_FAMILIES) {
            for (String tFace : List.of("bottom", "top", "front", "back", "left", "right")) {
                for (String tState : List.of("", "_active", "_running")) {
                    assertNotNull(resource("assets/gt6/textures/block/" + tFamily + "_overlay_" + tFace + tState + ".png"),
                            tFamily + "_overlay_" + tFace + tState + ".png must be borrowed");
                }
            }
        }
        // the three animation sidecars
        for (String tSidecar : List.of("largebath_overlay_front_active.png.mcmeta",
                "largemixer_overlay_front_active.png.mcmeta", "largemixer_overlay_front_running.png.mcmeta")) {
            assertNotNull(resource("assets/gt6/textures/block/" + tSidecar), tSidecar + " must be borrowed");
        }
        // the two front-pair band dirs: 8 faces each
        for (String tBand : List.of("vondagraagg", "bedrockdrill")) {
            for (String tFace : List.of(
                    "colored_bottom", "colored_top", "colored_side", "colored_front_side",
                    "overlay_bottom", "overlay_top", "overlay_side", "overlay_front_side")) {
                assertNotNull(resource("assets/gt6/textures/block/" + tBand + "/" + tFace + ".png"),
                        tBand + "/" + tFace + ".png must be borrowed");
            }
        }
    }

    /**
     * The retired six-face flat spread is dead on disk AND no generated JSON references
     * the old texture paths (the GT6BoilerTexDatagenTest census-walk shape).
     */
    @Test
    public void retiredFlatSpreadIsDeadEverywhere() throws Exception {
        Path tMdk = mdkRoot();
        List<String> tDead = List.of(
                "vondagraagg_colored_bottom.png", "vondagraagg_colored_top.png", "vondagraagg_colored_side.png",
                "vondagraagg_colored_front.png", "vondagraagg_colored_back.png", "vondagraagg_colored_left.png",
                "vondagraagg_colored_right.png",
                "bedrockdrill_colored_bottom.png", "bedrockdrill_colored_top.png", "bedrockdrill_colored_side.png",
                "bedrockdrill_colored_front.png", "bedrockdrill_colored_back.png", "bedrockdrill_colored_left.png",
                "bedrockdrill_colored_right.png");
        for (String tPath : tDead) {
            assertFalse(Files.exists(tMdk.resolve("src/main/resources/assets/gt6/textures/block").resolve(tPath)),
                    "the retired flat texture must be deleted: " + tPath);
        }
        List<String> tDeadRefs = List.of(
                "vondagraagg_colored_bottom", "vondagraagg_colored_top", "vondagraagg_colored_front",
                "vondagraagg_colored_back", "vondagraagg_colored_left", "vondagraagg_colored_right",
                "bedrockdrill_colored_bottom", "bedrockdrill_colored_top", "bedrockdrill_colored_front",
                "bedrockdrill_colored_back", "bedrockdrill_colored_left", "bedrockdrill_colored_right");
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
