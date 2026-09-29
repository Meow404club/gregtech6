/*
 * Offline pinned tests for task r8-tex-multiblockmains: the four multiblockmains
 * controller families (the eight large crucibles, the large heat exchanger, the
 * lightning rod, the logistics core) swap the borrowed-wall/composite placeholders for
 * the two-layer front-bearing tinted grammar (the {@link GT6BoilerTexDatagenTest} shape
 * — asserted against the committed generated tree). Upstream semantics pinned:
 * <ul>
 * <li>the Base10 default getTexture2 (TileEntityBase10MultiBlockBase.java:192-194; the
 *     crucible rides the same aSide==mFacing form, MultiTileEntityCrucible.java:643-650)
 *     — the FRONT face renders the colored_front+overlay_front pair, the other five
 *     faces the plain pair; FACES_TBS (CS.java:618) makes the horizontal front a "side"
 *     face, so the model's north carries {@code *_front_side};</li>
 * <li>the eleven controller rows carry the NBT_MATERIAL column (Loader
 *     :1270-1277 crucible eight, :1245 heat exchanger, :1281 logistics core, :1282
 *     lightning rod) and the colours resolve pairwise distinct — the borrowed-wall
 *     untinted-placeholder era is gone;</li>
 * <li>the 48 borrowed PNGs exist and the retired {@code large_boiler/wall} placeholder
 *     plus every generated JSON reference to it are dead, while the two borrow-time
 *     composites stay on disk as the itemform-B item layer0 reservation.</li>
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

import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;
import gregtech6.block.multiblock.GTMultiBlockControllerBlock;
import gregtech6.client.render.GTMachinePaintTint;
import gregtech6.registry.GTMaterialItems;

import net.minecraft.world.level.block.Block;

class GT6MultiblockMainsTexDatagenTest {

    /** facing → rotationY (the addLargeBoiler/addLogisticsCore y table — NORTH identity). */
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
        // the BLOCK write window (the GT6CFoamFamilyTest recipe via
        // GT6SingleBlockFacingIntegrityTest): the carrier-block fixtures construct
        // post-freeze, and the Forge-patched Block ctor registers its intrusive holder
        // — reopen the registry so `new` stays legal offline
        try {
            java.lang.reflect.Method tUnfreeze = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                    .getClass().getMethod("unfreeze");
            tUnfreeze.setAccessible(true);
            tUnfreeze.invoke(net.minecraft.core.registries.BuiltInRegistries.BLOCK);
        } catch (Exception aE) {
            throw new IllegalStateException("could not unfreeze the offline block registry", aE);
        }
    }

    private static InputStream resource(String aPath) {
        return GT6MultiblockMainsTexDatagenTest.class.getClassLoader().getResourceAsStream(aPath);
    }

    private static JsonObject json(String aPath) throws IOException {
        try (InputStream tStream = resource(aPath)) {
            assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    // ------------------------------------------------------------------
    // the two-layer front-bearing model grammar (the boilerModel shared product)
    // ------------------------------------------------------------------

    private void assertFrontBearingModel(String aName, String aBand) throws IOException {
        JsonObject tModel = json("assets/gt6/models/block/" + aName + ".json");
        assertEquals("minecraft:block/cube", tModel.get("parent").getAsString(), aName + ": cube parent");
        assertEquals("minecraft:cutout", tModel.get("render_type").getAsString(), aName + ": cutout");
        JsonObject tTextures = tModel.getAsJsonObject("textures");
        // the body layer: the north face carries the colored_front art (the Base10
        // front-face pair), the other five the plain colored art
        assertEquals("gt6:block/" + aBand + "/colored_front_side", tTextures.get("north").getAsString(),
                aName + ": north = the front-face colored art");
        for (String tSide : List.of("south", "west", "east")) {
            assertEquals("gt6:block/" + aBand + "/colored_side", tTextures.get(tSide).getAsString(),
                    aName + ": " + tSide + " = the plain colored side");
        }
        assertEquals("gt6:block/" + aBand + "/colored_bottom", tTextures.get("down").getAsString());
        assertEquals("gt6:block/" + aBand + "/colored_top", tTextures.get("up").getAsString());
        // the overlay shell: front face = the overlay_front art, the rest the plain overlay
        assertEquals("gt6:block/" + aBand + "/overlay_front_side", tTextures.get("overlay_north").getAsString(),
                aName + ": overlay north = the front-face overlay art");
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
    public void fourControllerModelsKeepTheTwoLayerFrontBearingGrammar() throws Exception {
        assertFrontBearingModel("large_crucible", "crucible");
        assertFrontBearingModel("logistics_core", "logistics_core");
        assertFrontBearingModel("multiblock_lightning_rod", "lightningrod");
        assertFrontBearingModel("large_heat_exchanger", "large_heat_exchanger");
    }

    // ------------------------------------------------------------------
    // the blockstate rotation tables
    // ------------------------------------------------------------------

    @Test
    public void crucibleBlockstatesPinTheFacingFormedVariants() throws Exception {
        assertEquals(8, gregtech6.registry.GT6Crucibles.CRUCIBLE_ROWS.size(), "the crucible census stays 8");
        for (var tRow : gregtech6.registry.GT6Crucibles.CRUCIBLE_ROWS) {
            JsonObject tVariants = json("assets/gt6/blockstates/" + tRow.path() + ".json").getAsJsonObject("variants");
            assertEquals(8, tVariants.size(), tRow.path() + ": exactly the 4 facing x 2 formed variants");
            for (String tFacing : ROT_Y.keySet()) {
                for (boolean tFormed : new boolean[] {false, true}) {
                    JsonObject tVariant = tVariants.getAsJsonObject("facing=" + tFacing + ",formed=" + tFormed);
                    assertEquals("gt6:block/large_crucible", tVariant.get("model").getAsString(),
                            tRow.path() + ": the shared band model (the FORMED look is the p9 pool)");
                    assertEquals(ROT_Y.get(tFacing).intValue(),
                            tVariant.has("y") ? tVariant.get("y").getAsInt() : 0,
                            tRow.path() + " facing=" + tFacing + ": the rotationY table");
                }
            }
        }
    }

    @Test
    public void logisticsCoreBlockstateKeepsTheRotationTable() throws Exception {
        JsonObject tVariants = json("assets/gt6/blockstates/logistics_core.json").getAsJsonObject("variants");
        assertEquals(8, tVariants.size(), "logistics_core: exactly the 4 facing x 2 formed variants");
        for (String tFacing : ROT_Y.keySet()) {
            for (boolean tFormed : new boolean[] {false, true}) {
                JsonObject tVariant = tVariants.getAsJsonObject("facing=" + tFacing + ",formed=" + tFormed);
                assertEquals("gt6:block/logistics_core", tVariant.get("model").getAsString(),
                        "logistics_core " + tFacing + ": the shared band model");
                assertEquals(ROT_Y.get(tFacing).intValue(),
                        tVariant.has("y") ? tVariant.get("y").getAsInt() : 0,
                        "logistics_core facing=" + tFacing + ": the rotationY table");
            }
        }
    }

    @Test
    public void lightningRodBlockstateStaysUnrotated() throws Exception {
        // the facing is structurally meaningless (the rod is vertical) — every state maps
        // to the same model with no rotation (the front pair lands on north)
        JsonObject tVariants = json("assets/gt6/blockstates/multiblock_lightning_rod.json").getAsJsonObject("variants");
        assertEquals(8, tVariants.size(), "multiblock_lightning_rod: the 4 facing x 2 formed variants");
        for (String tKey : tVariants.keySet()) {
            JsonObject tVariant = tVariants.getAsJsonObject(tKey);
            assertEquals("gt6:block/multiblock_lightning_rod", tVariant.get("model").getAsString(),
                    "multiblock_lightning_rod " + tKey + ": the shared band model");
            assertFalse(tVariant.has("y"), "multiblock_lightning_rod " + tKey + ": no rotation");
        }
    }

    @Test
    public void heatExchangerBlockstatePinsFormedOnly() throws Exception {
        // FORMED is the only state (no facing — the structure is facing-independent)
        JsonObject tVariants = json("assets/gt6/blockstates/large_heat_exchanger.json").getAsJsonObject("variants");
        assertEquals(2, tVariants.size(), "large_heat_exchanger: exactly the 2 formed variants");
        for (String tKey : tVariants.keySet()) {
            JsonObject tVariant = tVariants.getAsJsonObject(tKey);
            assertEquals("gt6:block/large_heat_exchanger", tVariant.get("model").getAsString(),
                    "large_heat_exchanger " + tKey + ": the shared band model");
            assertFalse(tVariant.has("y"), "large_heat_exchanger " + tKey + ": no rotation");
        }
    }

    // ------------------------------------------------------------------
    // the eleven NBT_MATERIAL rows + the tint dispatch
    // ------------------------------------------------------------------

    /** The upstream aMat columns verbatim (:1270-1277 crucible eight, the row order). */
    private static List<OreDictMaterial> expectedCrucibleMaterials() {
        return List.of(MT.Steel, MT.StainlessSteel, MT.Invar, MT.Ti, MT.TungstenSteel, MT.W, MT.Ta4HfC5, MT.Ad);
    }

    @Test
    public void elevenControllersCarryTheUpstreamMaterialColumn() {
        // the crucible eight through the GTCrucibleControllerBlock row carrier, the
        // logistics core + lightning rod through the GTMultiBlockControllerBlock carrier
        // ctor, the heat exchanger through its own materialOf (the GT6DynamoBlock shape).
        // Offline the DeferredRegister maps are empty (the itemform-B DIESEL_BLOCKS
        // lesson), so the carrier blocks are instantiated directly — the ctor/body wiring
        // under test is instance-level, the registry only mounts it.
        var tProps = net.minecraft.world.level.block.state.BlockBehaviour.Properties.of();
        List<OreDictMaterial> tExpected = expectedCrucibleMaterials();
        List<gregtech6.registry.GT6Crucibles.CrucibleRow> tRows = gregtech6.registry.GT6Crucibles.CRUCIBLE_ROWS;
        for (int i = 0; i < tRows.size(); i++) {
            assertSame(tExpected.get(i), tRows.get(i).material(),
                    "crucible row " + tRows.get(i).path() + " carries the upstream aMat");
            assertSame(tExpected.get(i), GTMultiBlockControllerBlock.materialOf(
                            new gregtech6.block.multiblock.GTCrucibleControllerBlock(tRows.get(i), tProps)),
                    "crucible row " + tRows.get(i).path() + " block rides the row carrier ctor");
        }
        assertSame(MT.SteelGalvanized, GTMultiBlockControllerBlock.materialOf(
                        new gregtech6.block.logistics.GTLogisticsCoreBlock(tProps)),
                "the logistics core resolves the :1281 SteelGalvanized column");
        assertSame(MT.W, GTMultiBlockControllerBlock.materialOf(
                        new gregtech6.block.multiblock.GTLightningRodBlock(tProps)),
                "the lightning rod resolves the :1282 ANY.W column");
        assertSame(MT.W, gregtech6.registry.GT6HeatExchangers.HeatExchangerBlock.materialOf(
                        new gregtech6.registry.GT6HeatExchangers.HeatExchangerBlock(tProps)),
                "the heat exchanger resolves the :1245 ANY.W column");
    }

    @Test
    public void elevenControllersTintWithPairwiseDistinctColours() {
        // the untinted-placeholder era is gone: the eleven rows resolve their tintindex-0
        // colour through the tintARGB seam (the unpainted arm) as the material's own
        // fRGBaSolid — Adamantium is genuinely white (upstream MT.java:794 element
        // 255,255,255,255), so the pin is materialColor equality, not non-whiteness —
        // and pairwise distinct within the crucible eight
        Set<Integer> tSeen = new HashSet<>();
        var tProps = net.minecraft.world.level.block.state.BlockBehaviour.Properties.of();
        for (var tRow : gregtech6.registry.GT6Crucibles.CRUCIBLE_ROWS) {
            int tTint = GTMachinePaintTint.tintARGB(
                    net.minecraftforge.client.model.data.ModelData.EMPTY, tRow.material(), 0);
            assertEquals(gregtech6.block.GTBasicMachineBlock.materialColor(tRow.material()), tTint & 0xFFFFFF,
                    tRow.path() + " tints with its own material colour through the seam");
            assertTrue(tSeen.add(tTint & 0xFFFFFF),
                    tRow.path() + " colour " + Integer.toHexString(tTint) + " is distinct");
        }
        List<Block> tCarriers = List.of(
                new gregtech6.block.logistics.GTLogisticsCoreBlock(tProps),
                new gregtech6.block.multiblock.GTLightningRodBlock(tProps),
                new gregtech6.registry.GT6HeatExchangers.HeatExchangerBlock(tProps));
        for (Block tCarrier : tCarriers) {
            OreDictMaterial tMat = tCarrier instanceof gregtech6.registry.GT6HeatExchangers.HeatExchangerBlock
                    ? gregtech6.registry.GT6HeatExchangers.HeatExchangerBlock.materialOf(tCarrier)
                    : GTMultiBlockControllerBlock.materialOf(tCarrier);
            assertNotNull(tMat, "the " + tCarrier.getClass().getSimpleName() + " carrier resolves its material");
            int tTint = GTMachinePaintTint.tintARGB(
                    net.minecraftforge.client.model.data.ModelData.EMPTY, tMat, 0);
            assertEquals(gregtech6.block.GTBasicMachineBlock.materialColor(tMat), tTint & 0xFFFFFF,
                    "the " + tCarrier.getClass().getSimpleName() + " tint seat reads its material");
        }
    }

    // ------------------------------------------------------------------
    // the borrowed art + the retired placeholders
    // ------------------------------------------------------------------

    @Test
    public void borrowedMultiblockmainsTexturesExist() {
        for (String tBand : List.of("crucible", "logistics_core", "lightningrod", "large_heat_exchanger")) {
            for (String tFace : List.of("bottom", "top", "side")) {
                for (String tLayer : List.of("colored", "colored_front", "overlay", "overlay_front")) {
                    assertNotNull(resource("assets/gt6/textures/block/" + tBand + "/" + tLayer + "_" + tFace + ".png"),
                            tBand + "/" + tLayer + "_" + tFace + ".png must be borrowed");
                }
            }
        }
    }

    /**
     * The retired large_boiler/wall placeholder is gone from disk AND no generated JSON
     * references it (its last consumer, the crucible controller borrow, died with this
     * task — the census walk over the generated tree, the boiler-test filesystem shape).
     * The two borrow-time composites STAY on disk: they are the r8-tex-itemform-b item
     * layer0 reservation (README-declared, zero block-model consumers).
     */
    @Test
    public void retiredWallPlaceholderIsDeadEverywhereAndCompositesStay() throws Exception {
        Path tMdk = mdkRoot();
        assertFalse(Files.exists(tMdk.resolve("src/main/resources/assets/gt6/textures/block/large_boiler/wall.png")),
                "the retired wall placeholder must be deleted");
        for (String tKept : List.of("lightningrod/main.png", "large_heat_exchanger/main.png")) {
            assertTrue(Files.exists(tMdk.resolve("src/main/resources/assets/gt6/textures/block").resolve(tKept)),
                    "the itemform-B item sprite must stay on disk: " + tKept);
        }
        try (Stream<Path> tWalk = Files.walk(tMdk.resolve("src/generated/resources/assets/gt6"))) {
            for (Path tFile : tWalk.filter(p -> p.toString().endsWith(".json")).toList()) {
                String tContent = Files.readString(tFile, StandardCharsets.UTF_8);
                assertFalse(tContent.contains("gt6:block/large_boiler/wall"),
                        tFile.getFileName() + " still references the retired wall placeholder");
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
