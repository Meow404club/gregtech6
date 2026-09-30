/*
 * Offline pinned-census tests for task 18-converter-tex-facing (GitHub #18): the
 * converter family (9 electric transformer rows + 10 dynamo rows) joins the two-layer
 * tinted grammar — the tintindex-0 grayscale colored body is the mRGBa seat (upstream
 * MultiTileEntityTransformerElectric.java:35-39 BlockTextureMulti(colored x mRGBa,
 * overlay)), the six 0.01-plate overlay decals stay untinted, and ACTIVE switches the
 * shell to the overlay_active art. The former src-over bake painted all tiers the same
 * gray-white (the #18 root cause) — these tests pin the per-tier colours back.
 *
 * <p>Coverage (asserted against the committed generated tree, the
 * {@link GT6ControllerPaintRenderDatagenTest} shape):
 * <ul>
 * <li>the row tables carry the upstream NBT_MATERIAL columns — the transformer rows ride
 *     Electric_T[0..8] (Loader :881-:889, MT.java:3691 TinAlloy..Trinitanium), the
 *     electric dynamos Electric_T[0..5], the flux dynamos FLUX_T[1..5] — and the nine
 *     transformer tier colours are PAIRWISE DISTINCT (the all-gray regression killer);</li>
 * <li>the tint pin: every tier's tintindex-0 ARGB (the GTMachinePaintTint seam, the
 *     unpainted arm) equals 0xFF000000 | getRGBInt(fRGBaSolid) — three literal pins plus
 *     the fRGBaSolid identity walk over all nine rungs;</li>
 * <li>the generated model pairs (electric_transformer / electric_dynamo / flux_dynamo,
 *     each + _active) keep the two-layer shape with the colored/overlay texture split,
 *     the cutout render_type and the untinted decals;</li>
 * <li>the generated blockstates carry exactly the 12 six-way x active variants with the
 *     dispenser-form rotation map (DOWN x=90 / UP x=270, the horizontal y band) — the
 *     FACING face is the FRONT (transformer = the INPUT face, dynamo = the OUTPUT face);
 *     the companion families (the LD endpoint pair, the crystal chargers, the ZPM
 *     dechargers — blocks sharing the converted carrier classes) expose the full six-way
 *     variant set with the x band;</li>
 * <li>the paintable arrays the GTMachineTintModel walk consumes total 9 + 6 + 5 blocks.</li>
 * </ul>
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.registry.GT6ElectricDynamos;
import gregtech6.registry.GT6ElectricTransformers;
import gregtech6.registry.GT6FluxDynamos;
import gregtech6.registry.GTMaterialItems;

class GT6ConverterPaintRenderDatagenTest {

    /** The six body face keys of the block/cube parent. */
    private static final List<String> FACE_KEYS = List.of("down", "up", "north", "south", "west", "east");

    /** facing → rotationY (the output-front band; FRONT on north in the model). */
    private static final java.util.Map<String, Integer> ROT_Y = java.util.Map.of(
            "north", 0, "south", 180, "west", 270, "east", 90, "down", 0, "up", 0);

    /** facing → rotationX (the dispenser band — the verticals). */
    private static final java.util.Map<String, Integer> ROT_X = java.util.Map.of(
            "down", 90, "up", 270, "north", 0, "south", 0, "west", 0, "east", 0);

    @BeforeAll
    static void bootMaterials() {
        // the vanilla bootstrap + material system (the controller census shape)
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
            // NetworkHooks.init() failure is expected offline
        }
        GTMaterialItems.initMaterials();
    }

    private static JsonObject json(String aPath) throws Exception {
        try (InputStream tStream = GT6ConverterPaintRenderDatagenTest.class.getClassLoader()
                .getResourceAsStream(aPath)) {
            assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    /** The upstream Electric_T[0..8] casing ladder, resolved per call (the MT-after-boot rule). */
    private static List<gregapi.oredict.OreDictMaterial> casingLadder() {
        return List.of(gregapi.data.MT.TinAlloy, gregapi.data.MT.SteelGalvanized, gregapi.data.MT.Al,
                gregapi.data.MT.StainlessSteel, gregapi.data.MT.Cr, gregapi.data.MT.Ti,
                gregapi.data.MT.Ir, gregapi.data.MT.Os, gregapi.data.MT.Trinitanium);
    }

    // ------------------------------------------------------------------
    // the row material columns (the NBT_MATERIAL parity)
    // ------------------------------------------------------------------

    @Test
    public void transformerRowsCarryTheCasingLadder() {
        assertEquals(9, GT6ElectricTransformers.ROWS.size(), "the transformer census stays 9");
        assertEquals(9, GT6ElectricTransformers.CASING_LADDER.size(), "the casing ladder stays 9");
        for (int i = 0; i < 9; i++) {
            assertSame(casingLadder().get(i), GT6ElectricTransformers.CASING_LADDER.get(i).get(),
                    "transformer row " + i + " carries Electric_T[" + i + "]");
        }
        // the registry blocks resolve their material through the same ladder column (the
        // registration passes CASING_LADDER.get(tier) in) — the carrier seam itself is
        // pinned in GT6SingleBlockFacingIntegrityTest (the RegistryObject .get() wall
        // keeps the arrays offline-unresolvable, the controller-census ruling)
    }

    @Test
    public void dynamoRowsCarryTheirLadders() {
        assertEquals(6, GT6ElectricDynamos.ELECTRIC_T_LADDER.size(), "the electric dynamo ladder stays 6");
        assertEquals(5, GT6FluxDynamos.FLUX_T_LADDER.size(), "the flux dynamo ladder stays 5");
        // the electric ladder verbatim: [0..5] = TinAlloy..Ti
        List<gregapi.oredict.OreDictMaterial> tElectric = List.of(gregapi.data.MT.TinAlloy, gregapi.data.MT.SteelGalvanized,
                gregapi.data.MT.Al, gregapi.data.MT.StainlessSteel, gregapi.data.MT.Cr, gregapi.data.MT.Ti);
        for (int i = 0; i < 6; i++) {
            assertSame(tElectric.get(i), GT6ElectricDynamos.ELECTRIC_T_LADDER.get(i).get(),
                    "electric dynamo rung " + i + " carries Electric_T[" + i + "]");
        }
        // the flux ladder: FLUX_T[1..5]
        for (int i = 0; i < 5; i++) {
            assertSame(gregapi.data.MT.FLUX_T[i + 1], GT6FluxDynamos.FLUX_T_LADDER.get(i).get(),
                    "flux dynamo rung " + i + " carries FLUX_T[" + (i + 1) + "]");
        }
    }

    // ------------------------------------------------------------------
    // the tint pin (the #18 all-gray regression killer)
    // ------------------------------------------------------------------

    @Test
    public void nineTransformerTiersTintWithDistinctMaterialColours() {
        Set<Integer> tSeen = new HashSet<>();
        for (int i = 0; i < 9; i++) {
            gregapi.oredict.OreDictMaterial tMat = GT6ElectricTransformers.CASING_LADDER.get(i).get();
            int tTint = gregtech6.client.render.GTMachinePaintTint.tintARGB(
                    net.minecraftforge.client.model.data.ModelData.EMPTY, tMat, 0);
            assertEquals(0xFF000000, tTint & 0xFF000000, "tier " + i + " binds full alpha");
            assertEquals(tMat.fRGBaSolid[0], (tTint >> 16) & 0xFF, "tier " + i + " R = fRGBaSolid[0]");
            assertEquals(tMat.fRGBaSolid[1], (tTint >> 8) & 0xFF, "tier " + i + " G = fRGBaSolid[1]");
            assertEquals(tMat.fRGBaSolid[2], tTint & 0xFF, "tier " + i + " B = fRGBaSolid[2]");
            assertTrue(tSeen.add(tTint & 0xFFFFFF),
                    "tier " + i + " colour " + Integer.toHexString(tTint) + " is distinct — the all-gray bake is gone");
        }
    }

    @Test
    public void literalTintPinsAnchorTheLadderEnds() {
        // Electric_T[0] TinAlloy and Electric_T[8] Trinitanium (the ladder ends; the exact
        // fRGBaSolid values the #18 bake flattened to one gray)
        assertEquals(gregtech6.block.GTBasicMachineBlock.materialColor(gregapi.data.MT.TinAlloy) | 0xFF000000,
                gregtech6.client.render.GTMachinePaintTint.tintARGB(null, gregapi.data.MT.TinAlloy, 0),
                "TinAlloy tier tint");
        assertEquals(gregtech6.block.GTBasicMachineBlock.materialColor(gregapi.data.MT.Trinitanium) | 0xFF000000,
                gregtech6.client.render.GTMachinePaintTint.tintARGB(null, gregapi.data.MT.Trinitanium, 0),
                "Trinitanium tier tint");
    }

    // ------------------------------------------------------------------
    // the generated model pairs
    // ------------------------------------------------------------------

    private void assertTwoLayerModel(String aModelName, String aColoredBand, String aOverlayBand) throws Exception {
        JsonObject tModel = json("assets/gt6/models/block/" + aModelName + ".json");
        if (aModelName.endsWith("_active")) {
            // the ACTIVE child form: the inactive parent with the three overlay overrides
            // (render_type does not ride the parent chain — it re-declares cutout)
            assertEquals("gt6:block/" + aModelName.replace("_active", ""), tModel.get("parent").getAsString(),
                    aModelName + ": the inactive family model parent");
            assertEquals("minecraft:cutout", tModel.get("render_type").getAsString(), aModelName + ": cutout re-declared");
            JsonObject tOverride = tModel.getAsJsonObject("textures");
            assertEquals("gt6:block/" + aColoredBand + "_" + aOverlayBand + "_front", tOverride.get("overlay_front").getAsString());
            assertEquals("gt6:block/" + aColoredBand + "_" + aOverlayBand + "_back", tOverride.get("overlay_back").getAsString());
            assertEquals("gt6:block/" + aColoredBand + "_" + aOverlayBand + "_side", tOverride.get("overlay_side").getAsString());
            assertTrue(!tModel.has("elements"), aModelName + ": the body elements stay inherited");
            return;
        }
        assertEquals("minecraft:block/cube", tModel.get("parent").getAsString(), aModelName + ": cube parent");
        assertEquals("minecraft:cutout", tModel.get("render_type").getAsString(), aModelName + ": cutout");
        JsonObject tTextures = tModel.getAsJsonObject("textures");
        assertEquals("gt6:block/" + aColoredBand + "_colored_front", tTextures.get("north").getAsString(),
                aModelName + ": north = the grayscale colored front (the tint seat)");
        assertEquals("gt6:block/" + aColoredBand + "_colored_back", tTextures.get("south").getAsString(),
                aModelName + ": south = the colored back");
        for (String tSide : List.of("east", "west", "up", "down")) {
            assertEquals("gt6:block/" + aColoredBand + "_colored_side", tTextures.get(tSide).getAsString(),
                    aModelName + ": " + tSide + " = the colored side");
        }
        assertEquals("gt6:block/" + aColoredBand + "_" + aOverlayBand + "_front", tTextures.get("overlay_front").getAsString());
        assertEquals("gt6:block/" + aColoredBand + "_" + aOverlayBand + "_back", tTextures.get("overlay_back").getAsString());
        assertEquals("gt6:block/" + aColoredBand + "_" + aOverlayBand + "_side", tTextures.get("overlay_side").getAsString());
        var tElements = tModel.getAsJsonArray("elements");
        assertEquals(7, tElements.size(), aModelName + ": body + 6 decals");
        var tBodyFaces = tElements.get(0).getAsJsonObject().getAsJsonObject("faces");
        for (String tFace : FACE_KEYS) {
            assertEquals(0, tBodyFaces.getAsJsonObject(tFace).get("tintindex").getAsInt(),
                    aModelName + " body face " + tFace + " carries tintindex 0");
        }
        for (int i = 1; i < 7; i++) {
            var tDecalFaces = tElements.get(i).getAsJsonObject().getAsJsonObject("faces");
            for (String tFace : tDecalFaces.keySet()) {
                assertTrue(!tDecalFaces.getAsJsonObject(tFace).has("tintindex"),
                        aModelName + " decal " + i + " face " + tFace + " stays untinted");
            }
        }
    }

    @Test
    public void converterModelPairsKeepTheTwoLayerGrammar() throws Exception {
        assertTwoLayerModel("electric_transformer", "electric_transformer", "overlay");
        assertTwoLayerModel("electric_transformer_active", "electric_transformer", "overlay_active");
        assertTwoLayerModel("electric_dynamo", "electric_dynamo", "overlay");
        assertTwoLayerModel("electric_dynamo_active", "electric_dynamo", "overlay_active");
        assertTwoLayerModel("flux_dynamo", "flux_dynamo", "overlay");
        assertTwoLayerModel("flux_dynamo_active", "flux_dynamo", "overlay_active");
    }

    // ------------------------------------------------------------------
    // the generated blockstates: the 12-variant six-way form
    // ------------------------------------------------------------------

    private void assertTwelveVariants(String aBlockPath, String aInactiveModel, String aActiveModel) throws Exception {
        JsonObject tVariants = json("assets/gt6/blockstates/" + aBlockPath + ".json").getAsJsonObject("variants");
        assertEquals(12, tVariants.size(), aBlockPath + ": exactly the 12 facing x active variants");
        for (String tFacing : ROT_Y.keySet()) {
            for (boolean tActive : new boolean[] {false, true}) {
                String tKey = "active=" + tActive + ",facing=" + tFacing;
                assertTrue(tVariants.has(tKey), aBlockPath + ": the variant key " + tKey);
                JsonObject tVariant = tVariants.getAsJsonObject(tKey);
                assertEquals("gt6:block/" + (tActive ? aActiveModel : aInactiveModel),
                        tVariant.get("model").getAsString(), aBlockPath + " " + tKey + ": the model");
                assertEquals(ROT_Y.get(tFacing).intValue(),
                        tVariant.has("y") ? tVariant.get("y").getAsInt() : 0, aBlockPath + " " + tKey + ": the rotationY");
                assertEquals(ROT_X.get(tFacing).intValue(),
                        tVariant.has("x") ? tVariant.get("x").getAsInt() : 0, aBlockPath + " " + tKey + ": the rotationX");
            }
        }
    }

    @Test
    public void transformerBlockstatesPinTheSixWayTwelveVariants() throws Exception {
        for (GT6ElectricTransformers.TransformerRow tRow : GT6ElectricTransformers.ROWS) {
            assertTwelveVariants(tRow.path(), "electric_transformer", "electric_transformer_active");
        }
    }

    @Test
    public void dynamoBlockstatesPinTheSixWayTwelveVariants() throws Exception {
        for (String tRow : List.of("electric_dynamo", "electric_dynamo_t2", "electric_dynamo_t3",
                "electric_dynamo_t4", "electric_dynamo_t5", "flux_dynamo", "flux_dynamo_t2",
                "flux_dynamo_t3", "flux_dynamo_t4", "flux_dynamo_t5")) {
            assertTwelveVariants(tRow, tRow.startsWith("electric") ? "electric_dynamo" : "flux_dynamo",
                    (tRow.startsWith("electric") ? "electric_dynamo" : "flux_dynamo") + "_active");
        }
    }

    /**
     * The companion families whose blocks SHARE the converted carrier classes: the LD
     * endpoint pair (GT6ElectricTransformerBlock.FACING over the LD model) and the crystal
     * chargers + ZPM dechargers (GT6BatteryBoxBlock.FACING) expose the full six-way
     * variant set — the verticals ride the dispenser x band.
     */
    @Test
    public void companionFamiliesExposeTheSixWayVariants() throws Exception {
        for (String tPath : List.of("longdist_fluid_pipe", "longdist_item_pipe")) {
            // the endpoint blocks ARE GT6ElectricTransformerBlock carriers (tier 0, no
            // material) — 12 variants, both ACTIVE values riding the shared inactive model
            JsonObject tVariants = json("assets/gt6/blockstates/" + tPath + ".json").getAsJsonObject("variants");
            assertEquals(12, tVariants.size(), tPath + ": the six-way x active endpoint variants");
            assertEquals("gt6:block/electric_transformer",
                    tVariants.getAsJsonObject("active=false,facing=down").get("model").getAsString(),
                    tPath + ": the verticals ride the shared transformer model");
            assertEquals("gt6:block/electric_transformer",
                    tVariants.getAsJsonObject("active=true,facing=up").get("model").getAsString(),
                    tPath + ": the shared model has no active art (the inactive shell)");
        }
        List<String> tBattery = List.of("crystal_charger", "crystal_charger_large",
                "zpm_decharger_quantum", "zpm_decharger_electric");
        for (String tPath : tBattery) {
            JsonObject tVariants = json("assets/gt6/blockstates/" + tPath + ".json").getAsJsonObject("variants");
            // tex-composite-family: the battery-box carrier gained the ACTIVE property —
            // the companion blockstates joined the 12-variant form (their own texture rows
            // are the GT6CompositeEnergyTexDatagenTest domain)
            assertEquals(12, tVariants.size(), tPath + ": the six-way x active variants");
        }
    }

    // ------------------------------------------------------------------
    // the walk coverage (the array totals)
    // ------------------------------------------------------------------

    @Test
    public void paintableArraysTotalTheRowTables() {
        // the row tables are the offline face (the RegistryObject-resolving arrays are the
        // runtime faces — the controller-census ruling); the array lengths are derived
        // from these totals by construction
        assertEquals(9, GT6ElectricTransformers.ROWS.size(), "the transformer walk total");
        assertEquals(6, GT6ElectricDynamos.ELECTRIC_T_LADDER.size(), "the electric dynamo walk total");
        assertEquals(5, GT6FluxDynamos.FLUX_T_LADDER.size(), "the flux dynamo walk total");
    }

    /** The item models parent the family model (the creative-tab face of the tint). */
    @Test
    public void transformerItemModelsParentTheFamilyModel() throws Exception {
        for (GT6ElectricTransformers.TransformerRow tRow : GT6ElectricTransformers.ROWS) {
            JsonObject tItem = json("assets/gt6/models/item/" + tRow.path() + ".json");
            assertEquals("gt6:block/electric_transformer", tItem.get("parent").getAsString(),
                    tRow.path() + ": the item parents the inactive family model");
        }
    }
}
