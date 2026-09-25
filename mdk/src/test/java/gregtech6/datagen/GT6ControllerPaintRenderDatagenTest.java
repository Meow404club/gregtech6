/*
 * Offline pinned-census tests for task p38-c2-controller-tint (the p38 render-gap census
 * C2+C4): the controller/energy-domain model JSONs carry tintindex 0 on every BODY face
 * and NO tintindex on the overlay decals, asserted against the committed src/generated
 * tree (the GT6PartPaintRenderDatagenTest census shape), and the row tables carry the
 * upstream NBT_MATERIAL column (the tint colour source — the
 * TileEntityBase10MultiBlockBase.getTexture2 :193 / MultiTileEntityHeaterElectric :56-59
 * BlockTextureMulti(BlockTextureDefault(colored, mRGBa), overlay) semantics, mRGBa
 * registration-derived from NBT_MATERIAL, ClassContainer.java:51).
 *
 * <p>Coverage (the 39-block wiring): the 12 multiblock mains (4 steam turbine :1254-1257
 * + 4 gas turbine :1264-1267 + 4 dynamo :1259-1262 — the aMat column verbatim
 * StainlessSteel/Ti/TungstenSteel/Ad, the display words are NOT the housing materials),
 * the 15 EU-bridge rungs (heater :817-821 / engine :833-837 / motor :849-853 — the
 * Electric_T[1..5] ladder MT.java:3691), the 10 laser rungs (CO2 laser :930-934 +
 * absorber :976-980, the same Electric_T ladder), the magic absorber (:1005, MT.Pd) and
 * the coke-oven bricks (:1138, MT.Ceramic — the #8 declared deviation retired).
 *
 * <p>EXEMPTION pinned: the quantum energizer rows carry a NULL material column — its
 * amber art is pre-tinted upstream (the derived-look declaration), so the shared tinted
 * builder leaves it at the white identity. The offline fixtures cannot resolve
 * RegistryObjects, so the paintable-block-array walkers are pinned through the row
 * tables they walk (the array size is the row total, 39).
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.registry.GT6DynamoHousings;
import gregtech6.registry.GT6Lasers;
import gregtech6.registry.GT6MagicAbsorbers;
import gregtech6.registry.GT6Turbines;
import gregtech6.registry.GTMachines;

class GT6ControllerPaintRenderDatagenTest {

    /** The six body face keys of the block/cube / orientable parents. */
    private static final List<String> FACE_KEYS = List.of("down", "up", "north", "south", "west", "east");

    /**
     * The Electric_T[1..5] rung ladder (upstream MT.java:3691 members, the bridge/laser
     * face) — resolved per call: the MT fields are null until the BeforeAll material
     * boot (the class-load ordering the part census works around by method-local reads).
     */
    private static List<gregapi.oredict.OreDictMaterial> electricT() {
        return List.of(gregapi.data.MT.SteelGalvanized, gregapi.data.MT.Al, gregapi.data.MT.StainlessSteel,
                gregapi.data.MT.Cr, gregapi.data.MT.Ti);
    }

    @BeforeAll
    public static void bootOfflineThenMaterials() {
        // the vanilla bootstrap first (the GTMachinesMaterialRowTest shape — the row
        // tables/block classes carry BlockProperty/DeferredRegister statics), then the
        // material system
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
            // NetworkHooks.init() failure is expected offline
        }
        gregtech6.registry.GTMaterialItems.initMaterials();
    }

    private static JsonObject json(String aPath) throws Exception {
        try (InputStream tStream = GT6ControllerPaintRenderDatagenTest.class.getClassLoader()
                .getResourceAsStream(aPath)) {
            assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    /** One two-layer model: the body cube tinted on all six faces, the six overlay decals untinted (the turbine form). */
    private static void assertTintedBodyWithDecals(String aPath) throws Exception {
        JsonObject tModel = json("assets/gt6/models/block/" + aPath + ".json");
        assertEquals(7, tModel.getAsJsonArray("elements").size(), aPath + " keeps the two-layer shape (body + 6 decals)");
        JsonObject tBody = tModel.getAsJsonArray("elements").get(0).getAsJsonObject();
        for (Map.Entry<String, JsonElement> tFace : tBody.getAsJsonObject("faces").entrySet()) {
            assertTrue(FACE_KEYS.contains(tFace.getKey()), aPath + " body face key " + tFace.getKey());
            assertEquals(0, tFace.getValue().getAsJsonObject().get("tintindex").getAsInt(),
                    aPath + " body face " + tFace.getKey() + " carries tintindex 0 (the material tint)");
        }
        for (int i = 1; i < 7; i++) {
            JsonObject tDecal = tModel.getAsJsonArray("elements").get(i).getAsJsonObject();
            for (Map.Entry<String, JsonElement> tFace : tDecal.getAsJsonObject("faces").entrySet()) {
                assertNull(tFace.getValue().getAsJsonObject().get("tintindex"),
                        aPath + " decal element " + i + " face " + tFace.getKey() + " stays untinted (the overlay pass)");
            }
        }
    }

    /** One single-element orientable model: every face of the one body element tinted (the bridge/laser form). */
    private static void assertTintedOrientableBody(String aPath) throws Exception {
        JsonObject tModel = json("assets/gt6/models/block/" + aPath + ".json");
        assertEquals(1, tModel.getAsJsonArray("elements").size(), aPath + " keeps the one-element body form");
        JsonObject tBody = tModel.getAsJsonArray("elements").get(0).getAsJsonObject();
        for (Map.Entry<String, JsonElement> tFace : tBody.getAsJsonObject("faces").entrySet()) {
            assertEquals(0, tFace.getValue().getAsJsonObject().get("tintindex").getAsInt(),
                    aPath + " body face " + tFace.getKey() + " carries tintindex 0 (the material tint)");
        }
    }

    /** The upstream row aMat of one tier ladder position, verbatim StainlessSteel/Ti/TungstenSteel/Ad. */
    private static void assertMainTierMaterials(List<?> aRows, String aLabel) {
        gregapi.oredict.OreDictMaterial[] tExpected = {
                gregapi.data.MT.StainlessSteel, gregapi.data.MT.Ti, gregapi.data.MT.TungstenSteel, gregapi.data.MT.Ad};
        for (int i = 0; i < 4; i++) {
            Object tEntry = aRows.get(i);
            gregapi.oredict.OreDictMaterial tMaterial;
            if (tEntry instanceof GT6Turbines.SteamTurbineRow tSteam) tMaterial = tSteam.material().get();
            else if (tEntry instanceof GT6Turbines.GasTurbineRow tGas) tMaterial = tGas.material().get();
            else if (tEntry instanceof GT6DynamoHousings.DynamoRow tDynamo) tMaterial = tDynamo.material().get();
            else tMaterial = null;
            assertSame(tExpected[i], tMaterial, aLabel + " row " + i + " carries the upstream NBT_MATERIAL");
        }
    }

    // ------------------------------------------------------------------
    // the 12 multiblock mains — the turbine/gas/dynamo controller domain
    // ------------------------------------------------------------------

    @Test
    public void mainsCarryTheUpstreamMaterials() {
        assertEquals(4, GT6Turbines.STEAM_ROWS.size(), "the steam turbine census stays 4");
        assertEquals(4, GT6Turbines.GAS_ROWS.size(), "the gas turbine census stays 4");
        assertEquals(4, GT6DynamoHousings.DYNAMO_ROWS.size(), "the dynamo census stays 4");
        assertMainTierMaterials(GT6Turbines.STEAM_ROWS, "steam");
        assertMainTierMaterials(GT6Turbines.GAS_ROWS, "gas");
        assertMainTierMaterials(GT6DynamoHousings.DYNAMO_ROWS, "dynamo");
    }

    @Test
    public void mainModelsCarryTintedBodies() throws Exception {
        assertTintedBodyWithDecals("turbine_main_steam");
        assertTintedBodyWithDecals("turbine_main_gas");
        assertTintedBodyWithDecals("turbine_main_dynamo");
        // the blockstate walk: the FACING x FORMED variants (8) all map to the shared
        // family model (the formed-look visual is the p9 pool posture)
        for (String tPath : List.of("steam_turbine_magnalium", "gas_turbine_vibramantium", "large_dynamo_adamantium")) {
            JsonObject tVariants = json("assets/gt6/blockstates/" + tPath + ".json").getAsJsonObject("variants");
            assertEquals(8, tVariants.size(), tPath + " emits one variant per facing x formed");
            assertTrue(tVariants.getAsJsonObject("facing=north,formed=false").get("model").getAsString().contains("block/turbine_main_"),
                    tPath + " unformed north variant rides the family model");
        }
    }

    // ------------------------------------------------------------------
    // the 15 EU-bridge + 10 laser rungs — the Electric_T ladder domain
    // ------------------------------------------------------------------

    @Test
    public void bridgeRowsCarryTheUpstreamMaterials() {
        for (List<GTMachines.BridgeRow> tFamily : List.of(GTMachines.ELECTRIC_HEATER_ROWS,
                GTMachines.ELECTRIC_ENGINE_ROWS, GTMachines.ELECTRIC_MOTOR_ROWS)) {
            assertEquals(5, tFamily.size(), "the bridge family census stays 5");
            for (int i = 0; i < 5; i++) {
                assertSame(electricT().get(i), tFamily.get(i).material().get(),
                        tFamily.get(i).path() + " carries the Electric_T[" + (i + 1) + "] rung");
                assertSame(tFamily.get(i).material().get(), GTMachines.electricTierMat(i),
                        "the row column and the shared ladder agree at rung " + i);
            }
        }
    }

    @Test
    public void laserRowsCarryTheUpstreamMaterials() {
        for (List<GT6Lasers.LaserRow> tFamily : List.of(GT6Lasers.CO2_LASER_ROWS, GT6Lasers.LASER_ABSORBER_ROWS)) {
            assertEquals(5, tFamily.size(), "the laser family census stays 5");
            for (int i = 0; i < 5; i++) {
                assertSame(electricT().get(i), tFamily.get(i).material().get(),
                        tFamily.get(i).path() + " carries the Electric_T[" + (i + 1) + "] rung");
            }
        }
    }

    @Test
    public void quantumEnergizerRowsStayMaterialless() {
        // the card exemption: the derived pre-tinted amber art — the null column keeps the
        // shared tinted builder at the white identity for this family
        for (GT6Lasers.LaserRow tRow : gregtech6.registry.GT6QuantumEnergizers.QUANTUM_ENERGIZER_ROWS) {
            assertNull(tRow.material(), tRow.path() + " carries no NBT_MATERIAL (the exemption)");
        }
    }

    @Test
    public void bridgeAndLaserModelsCarryTintedBodies() throws Exception {
        assertTintedOrientableBody("bridge_heater");
        assertTintedOrientableBody("bridge_engine");
        assertTintedOrientableBody("bridge_motor");
        // the CO2 Laser rides the upstream NBT_TEXTURE name (the model id laser_electric,
        // the addLaserFamilies aTexture column)
        assertTintedOrientableBody("laser_electric");
        assertTintedOrientableBody("laser_absorber");
        // the shared builder form reaches the exempt family too — the tintindex is the
        // white identity there (no material column, no tint registration)
        assertTintedOrientableBody("quantum_energizer");
    }

    // ------------------------------------------------------------------
    // the magic absorber + the coke-oven bricks — the single-block wirings
    // ------------------------------------------------------------------

    @Test
    public void magicAbsorberCarriesThePalladiumMaterial() {
        assertEquals(1, GT6MagicAbsorbers.MAGIC_ABSORBER_BLOCKS_BY_PATH.size(), "the absorber census stays 1");
        assertSame(gregapi.data.MT.Pd, GT6MagicAbsorbers.MAGIC_ABSORBER_MATERIAL.get(), ":1005 NBT_MATERIAL MT.Pd");
    }

    @Test
    public void magicAbsorberModelCarriesTintedBody() throws Exception {
        assertTintedOrientableBody("magic_absorber");
    }

    @Test
    public void cokeBricksCarryTheCeramicMaterial() {
        assertSame(gregapi.data.MT.Ceramic, gregtech6.registry.GTMultiBlocks.COKE_BRICKS_MATERIAL.get(),
                ":1138 Fire Bricks NBT_MATERIAL MT.Ceramic");
    }

    @Test
    public void cokeBricksModelKeepsTheTintedPartForm() throws Exception {
        // the partModel body tintindex was already in place (the #8 declared deviation);
        // the material column + walk membership are what this card retired
        assertTintedBodyWithDecals("multiblock_coke_oven_bricks");
    }

    // ------------------------------------------------------------------
    // the walker coverage — the array totals the row tables carry
    // (the RegistryObject-resolving arrays themselves are runtime faces)
    // ------------------------------------------------------------------

    @Test
    public void wiringCensusStays39Blocks() {
        int tMains = GT6Turbines.STEAM_ROWS.size() + GT6Turbines.GAS_ROWS.size() + GT6DynamoHousings.DYNAMO_ROWS.size();
        int tBridges = GTMachines.ELECTRIC_HEATER_ROWS.size() + GTMachines.ELECTRIC_ENGINE_ROWS.size()
                + GTMachines.ELECTRIC_MOTOR_ROWS.size();
        int tLasers = GT6Lasers.CO2_LASER_ROWS.size() + GT6Lasers.LASER_ABSORBER_ROWS.size();
        int tMagic = GT6MagicAbsorbers.MAGIC_ABSORBER_BLOCKS_BY_PATH.size();
        assertEquals(39, tMains + tBridges + tLasers + tMagic + 1 /* the coke-oven bricks */,
                "the controller-domain tint wiring census (12 mains + 15 bridges + 10 lasers + 1 absorber + 1 bricks)");
    }
}
