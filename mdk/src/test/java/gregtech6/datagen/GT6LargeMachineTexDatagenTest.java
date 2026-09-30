/*
 * Offline pinned tests for task tex-large-machines: the 17 large-controller domains
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
import java.util.ArrayList;
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

    /** facing → rotationY (the addLargeMachines y table — NORTH identity). */
    private static final java.util.Map<String, Integer> ROT_Y = java.util.Map.of(
            "north", 0, "south", 180, "west", 270, "east", 90);

    /** The three mains that join the row trio (block path → texture family). */
    private static final List<String[]> TRIO_MAINS = List.of(
            new String[] {"large_massfab", "largemassfab"},
            new String[] {"fusion_reactor", "fusionreactor"},
            new String[] {"implosion_compressor", "implosioncompressor"});

    /** The two front-pair mains (block path → band dir; no upstream active group). */
    private static final List<String[]> FRONT_PAIR_MAINS = List.of(
            new String[] {"von_da_graagg", "vondagraagg"},
            new String[] {"bedrock_drill", "bedrockdrill"});

    /**
     * The 17 controller blocks, offline-safe: the RegistryObjects never turn present in
     * the test JVM (no registry event — the blockArray() walk would throw), so the test
     * constructs the plain instances under the block-registry latch (see
     * {@link #buildControllerBlocks()}), after the @BeforeAll bootstrap. The order pairs
     * {@link #expectedMaterials()}: the twelve rows in ROWS order + massfab, fusion,
     * implosion, graagg, drill (Loader :1241/:1242/:1228/:1280/:1283).
     */
    private static List<net.minecraft.world.level.block.Block> sControllers;

    /**
     * The {@code gregtech6.tileentity.GTOfflineTestBase} ItemLatch shape over the BLOCK
     * registry: the plain {@code Block} ctor binds its intrusive holder, so the write
     * window opens for the fixture builds and closes again (the latch class-init stays
     * lazy — touching BuiltInRegistries before the bootstrap fails the registry class).
     */
    private static final class BlockLatch {
        static final sun.misc.Unsafe UNSAFE;
        static final long LOCKED_OFFSET;
        static final long FROZEN_OFFSET;
        static final boolean ARMED;
        static {
            sun.misc.Unsafe tUnsafe = null;
            long tLocked = 0, tFrozen = 0;
            boolean tArmed = true;
            try {
                java.lang.reflect.Field tUnsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
                tUnsafeField.setAccessible(true);
                tUnsafe = (sun.misc.Unsafe) tUnsafeField.get(null);
                Class<?> tClass = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getClass();
                tLocked = tUnsafe.objectFieldOffset(walkNestedField(tClass, "locked"));
                tFrozen = tUnsafe.objectFieldOffset(walkNestedField(tClass, "frozen"));
            } catch (Throwable ignored) {
                tArmed = false; // the fallback leg (no constructed fixtures)
            }
            UNSAFE = tUnsafe;
            LOCKED_OFFSET = tLocked;
            FROZEN_OFFSET = tFrozen;
            ARMED = tArmed;
        }

        /** The latch fields live on wrapper superclasses — walk up (getDeclaredField sees one class only). */
        private static java.lang.reflect.Field walkNestedField(Class<?> aClass, String aName)
                throws NoSuchFieldException {
            for (Class<?> tWalk = aClass; tWalk != null; tWalk = tWalk.getSuperclass()) {
                try {
                    return tWalk.getDeclaredField(aName);
                } catch (NoSuchFieldException ignored) {
                    // the superclass carries it
                }
            }
            throw new NoSuchFieldException(aName);
        }
    }

    private static List<net.minecraft.world.level.block.Block> buildControllerBlocks() {
        var tProps = net.minecraft.world.level.block.state.BlockBehaviour.Properties.of();
        List<net.minecraft.world.level.block.Block> rBlocks = new ArrayList<>();
        for (var tRow : gregtech6.registry.GT6LargeMachines.ROWS) {
            rBlocks.add(new gregtech6.registry.GT6LargeMachines.GTLargeMachineBlock(tRow, tProps));
        }
        rBlocks.add(new gregtech6.block.multiblock.GTMassfabBlock(tProps));
        rBlocks.add(new gregtech6.block.multiblock.GTFusionReactorBlock(tProps));
        rBlocks.add(new gregtech6.block.multiblock.GTImplosionCompressorBlock(tProps));
        rBlocks.add(new gregtech6.block.multiblock.GTVonDaGraaggBlock(tProps));
        rBlocks.add(new gregtech6.block.multiblock.GTBedrockDrillBlock(tProps));
        return rBlocks;
    }

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
        // the fixture blocks build under the opened write window (no-op when the latch
        // is unreachable — the dispatch test then skips itself, the telemetry face)
        if (BlockLatch.ARMED) {
            BlockLatch.UNSAFE.putBoolean(net.minecraft.core.registries.BuiltInRegistries.BLOCK,
                    BlockLatch.FROZEN_OFFSET, false);
            BlockLatch.UNSAFE.putBoolean(net.minecraft.core.registries.BuiltInRegistries.BLOCK,
                    BlockLatch.LOCKED_OFFSET, false);
            try {
                sControllers = buildControllerBlocks();
            } finally {
                BlockLatch.UNSAFE.putBoolean(net.minecraft.core.registries.BuiltInRegistries.BLOCK,
                        BlockLatch.FROZEN_OFFSET, true);
                BlockLatch.UNSAFE.putBoolean(net.minecraft.core.registries.BuiltInRegistries.BLOCK,
                        BlockLatch.LOCKED_OFFSET, true);
            }
        }
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
        assertEquals(12, gregtech6.registry.GT6LargeMachines.ROWS.size(), "the large-12 row census stays 12");
        for (var tRow : gregtech6.registry.GT6LargeMachines.ROWS) {
            assertTrioModel(tRow.path(), tRow.texture(), "");
            assertTrioModel(tRow.path() + "_active", tRow.texture(), "_active");
            assertTrioModel(tRow.path() + "_running", tRow.texture(), "_running");
        }
        assertEquals(3, TRIO_MAINS.size(), "the trio-main census stays 3");
        for (String[] tMain : TRIO_MAINS) {
            assertTrioModel(tMain[0], tMain[1], "");
            assertTrioModel(tMain[0] + "_active", tMain[1], "_active");
            assertTrioModel(tMain[0] + "_running", tMain[1], "_running");
        }
    }

    /**
     * The defer declaration pinned: the 15 trio blockstates wire ONLY the inactive model
     * (8 FACING x FORMED variants, the addLargeMachines y table) while the _active/
     * _running models exist on the classpath — the property seat is the follow-up card's.
     */
    @Test
    public void trioBlockstatesStayStaticOnTheInactiveModel() throws Exception {
        List<String> tTrioBlocks = new ArrayList<>();
        for (var tRow : gregtech6.registry.GT6LargeMachines.ROWS) tTrioBlocks.add(tRow.path());
        for (String[] tMain : TRIO_MAINS) tTrioBlocks.add(tMain[0]);
        assertEquals(15, tTrioBlocks.size(), "the trio block census stays 15");
        for (String tBlock : tTrioBlocks) {
            String tModel = "gt6:block/" + tBlock;
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
        for (String[] tMain : FRONT_PAIR_MAINS) {
            assertFrontPairModel(tMain[0], tMain[1]);
        }
        // no upstream active group — ONE static band each (the disprobe pin)
        for (String[] tMain : FRONT_PAIR_MAINS) {
            for (String tState : List.of("_active", "_running")) {
                assertFalse(resource("assets/gt6/models/block/" + tMain[0] + tState + ".json") != null,
                        tMain[0] + " ships no upstream active group — no state band");
            }
        }
    }

    @Test
    public void frontPairBlockstatesPinTheFacingFormedVariants() throws Exception {
        for (String[] tMain : FRONT_PAIR_MAINS) {
            JsonObject tVariants = json("assets/gt6/blockstates/" + tMain[0] + ".json").getAsJsonObject("variants");
            assertEquals(8, tVariants.size(), tMain[0] + ": exactly the 4 facing x 2 formed variants");
            for (String tFacing : ROT_Y.keySet()) {
                for (boolean tFormed : new boolean[] {false, true}) {
                    JsonObject tVariant = tVariants.getAsJsonObject("facing=" + tFacing + ",formed=" + tFormed);
                    assertEquals("gt6:block/" + tMain[0], tVariant.get("model").getAsString(),
                            tMain[0] + ": the shared band model");
                    assertEquals(ROT_Y.get(tFacing).intValue(),
                            tVariant.has("y") ? tVariant.get("y").getAsInt() : 0,
                            tMain[0] + " facing=" + tFacing + ": the rotationY table");
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // the 17 NBT_MATERIAL rows + the tint dispatch
    // ------------------------------------------------------------------

    /** The upstream aMat column, Loader :1228-1283 verbatim (order = CONTROLLER_BLOCKS). */
    private static List<gregapi.oredict.OreDictMaterial> expectedMaterials() {
        return List.of(
                gregapi.data.MT.TungstenSteel, gregapi.data.MT.StainlessSteel, gregapi.data.MT.StainlessSteel,
                gregapi.data.MT.StainlessSteel, gregapi.data.MT.StainlessSteel, gregapi.data.MT.StainlessSteel,
                gregapi.data.MT.StainlessSteel, gregapi.data.MT.Invar, gregapi.data.MT.Ti,
                gregapi.data.MT.TungstenSteel, gregapi.data.MT.TungstenSteel, gregapi.data.ANY.Steel,
                gregapi.data.MT.Pb, gregapi.data.MT.SteelGalvanized, gregapi.data.MT.TungstenSteel,
                gregapi.data.MT.SteelGalvanized, gregapi.data.MT.Ti);
    }

    /**
     * The dispatch arm pinned reflectively (the CreativeTabJoinCensusTest discipline —
     * tintMaterialOf is the package-private pure seam): every controller resolves its
     * upstream NBT_MATERIAL (assertSame — SS x6 / TS x4 / Ti x2 repeat BY DESIGN, the
     * row table is the pin), full-alpha over each, and the DISTINCT materials render
     * pairwise distinct colours (the all-gray single-texture placeholder era is gone).
     */
    @Test
    public void seventeenRowsTintWithDistinctUpstreamMaterials() throws Exception {
        List<gregapi.oredict.OreDictMaterial> tExpected = expectedMaterials();
        // the GTOfflineTestBase.registerItemFixture telemetry face (ItemLatch is designed
        // for exactly this: "the latch fields may be unreachable (module access)") — the
        // armed leg carries the 17-material pin, the unarmed leg skips itself
        org.junit.jupiter.api.Assumptions.assumeTrue(BlockLatch.ARMED,
                "the offline registry latch is unreachable on this JVM");
        assertNotNull(sControllers, "the fixture controllers must build (the offline latch arm)");
        assertEquals(17, sControllers.size(), "the large-controller census stays 17");
        var tMethod = Class.forName("gregtech6.client.render.GTMachinePaintTint")
                .getDeclaredMethod("tintMaterialOf", net.minecraft.world.level.block.Block.class);
        tMethod.setAccessible(true);
        Set<gregapi.oredict.OreDictMaterial> tResolved = new HashSet<>();
        for (int i = 0; i < 17; i++) {
            gregapi.oredict.OreDictMaterial tMat =
                    (gregapi.oredict.OreDictMaterial) tMethod.invoke(null, sControllers.get(i));
            assertSame(tExpected.get(i), tMat, "controller " + i + " carries the upstream aMat");
            int tTint = gregtech6.client.render.GTMachinePaintTint.tintARGB(
                    net.minecraftforge.client.model.data.ModelData.EMPTY, tMat, 0);
            assertEquals(0xFF000000, tTint & 0xFF000000, "controller " + i + " binds full alpha");
            tResolved.add(tMat);
        }
        // the seven distinct columns each carry their own colour
        assertEquals(7, tResolved.size(), "the distinct-material census stays 7");
        Set<Integer> tSeen = new HashSet<>();
        for (gregapi.oredict.OreDictMaterial tMat : tResolved) {
            int tTint = gregtech6.client.render.GTMachinePaintTint.tintARGB(
                    net.minecraftforge.client.model.data.ModelData.EMPTY, tMat, 0);
            assertTrue(tSeen.add(tTint & 0xFFFFFF),
                    "material " + tMat + " colour " + Integer.toHexString(tTint) + " is distinct");
        }
        assertEquals(7, tSeen.size(), "the seven columns stay pairwise distinct");
    }

    // ------------------------------------------------------------------
    // the borrowed art + the retired spread
    // ------------------------------------------------------------------

    @Test
    public void borrowedLargeMachineTexturesExist() {
        // the twelve rows' state trios: 6 faces x (overlay + active + running)
        for (var tRow : gregtech6.registry.GT6LargeMachines.ROWS) {
            for (String tFace : List.of("bottom", "top", "front", "back", "left", "right")) {
                for (String tState : List.of("", "_active", "_running")) {
                    assertNotNull(resource("assets/gt6/textures/block/" + tRow.texture() + "_overlay_" + tFace + tState + ".png"),
                            tRow.texture() + "_overlay_" + tFace + tState + ".png must be borrowed");
                }
            }
        }
        // the three mains' trios
        for (String[] tMain : TRIO_MAINS) {
            for (String tFace : List.of("bottom", "top", "front", "back", "left", "right")) {
                for (String tState : List.of("", "_active", "_running")) {
                    assertNotNull(resource("assets/gt6/textures/block/" + tMain[1] + "_overlay_" + tFace + tState + ".png"),
                            tMain[1] + "_overlay_" + tFace + tState + ".png must be borrowed");
                }
            }
        }
        // the three animation sidecars
        for (String tSidecar : List.of("largebath_overlay_front_active.png.mcmeta",
                "largemixer_overlay_front_active.png.mcmeta", "largemixer_overlay_front_running.png.mcmeta")) {
            assertNotNull(resource("assets/gt6/textures/block/" + tSidecar), tSidecar + " must be borrowed");
        }
        // the two front-pair band dirs: 8 faces each
        for (String[] tMain : FRONT_PAIR_MAINS) {
            for (String tFace : List.of(
                    "colored_bottom", "colored_top", "colored_side", "colored_front_side",
                    "overlay_bottom", "overlay_top", "overlay_side", "overlay_front_side")) {
                assertNotNull(resource("assets/gt6/textures/block/" + tMain[1] + "/" + tFace + ".png"),
                        tMain[1] + "/" + tFace + ".png must be borrowed");
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
                "vondagraagg_colored_bottom.png", "vondagraagg_colored_top.png", "vondagraagg_colored_front.png",
                "vondagraagg_colored_back.png", "vondagraagg_colored_left.png", "vondagraagg_colored_right.png",
                "bedrockdrill_colored_bottom.png", "bedrockdrill_colored_top.png", "bedrockdrill_colored_front.png",
                "bedrockdrill_colored_back.png", "bedrockdrill_colored_left.png", "bedrockdrill_colored_right.png");
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
