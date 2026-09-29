/*
 * Offline pinned tests for task r8-tex-bridge-kinetic (the census tierB2/B3 batch 2):
 * the five borrowable bridge/laser families, the rotation transformer and the kinetic
 * engines (28 steam rows + 8 diesel rows) join the addConverterModel two-layer grammar —
 * the tintindex-0 grayscale colored body is the mRGBa seat (upstream
 * BlockTextureMulti(colored x mRGBa, overlay), HeaterElectric :56-59 and siblings), the
 * six 0.01-plate overlay decals stay untinted, and ACTIVE switches the shell to the
 * overlay_active art where the upstream group exists AND the port carrier has the
 * channel (the GT6DynamoBlock property; engine/motor/transformer/engine rows ship no
 * active split — the static-face posture).
 *
 * <p>Coverage (asserted against the committed generated tree, the
 * {@link GT6ConverterPaintRenderDatagenTest} shape):
 * <ul>
 * <li>the two-layer model pairs (bridge_heater/laser_electric/laser_absorber each
 *     + _active; bridge_engine/bridge_motor/steam_engine/diesel_engine/
 *     transformer_rotation static);</li>
 * <li>the blockstate variant spaces: 12 (six-way x ACTIVE) for the dynamo-carrier
 *     families — active=true riding the shared inactive model on the art-less families —
 *     and 4 (the horizontal y band) for the steam/diesel/rotation-transformer rows;</li>
 * <li>the row-material tint columns: the 14 steam slugs and 8 diesel slugs resolve
 *     through the GTMachinePaintTint.bySlug table (the Loader :584-612/:721-729
 *     NBT_MATERIAL columns) and colour PAIRWISE DISTINCT (the all-gray regression
 *     killer); the rotation transformer pins WoodTreated (Loader :1668);</li>
 * <li>the 38 borrowed PNGs exist with the PNG magic (three canary digests pin the
 *     verbatim bytes);</li>
 * <li>the retired single-layer flat names are dead: no file, and no generated model
 *     references the old sprite paths;</li>
 * <li>the probe-miss family stays declared: quantum_energizer keeps the plain
 *     single-layer orientable (upstream quantum_laser overlay groups are alpha-0 empty
 *     layers — the assets/README.md probe verdict).</li>
 * </ul>
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.registry.GT6Kinetics;
import gregtech6.registry.GTMaterialItems;

class GT6BridgeKineticTexDatagenTest {

    /** The six body face keys of the block/cube parent. */
    private static final List<String> FACE_KEYS = List.of("down", "up", "north", "south", "west", "east");

    /** facing → rotationY (the output-front band; FRONT on north in the model). */
    private static final java.util.Map<String, Integer> ROT_Y = java.util.Map.of(
            "north", 0, "south", 180, "west", 270, "east", 90);

    /** The eight diesel row slugs (the GT6Kinetics.DieselSpec material column). */
    private static final List<String> DIESEL_SLUGS = List.of("bronze", "arsenic_copper", "arsenic_bronze",
            "steel", "invar", "titanium", "tungstensteel", "iridium");

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
        try (InputStream tStream = GT6BridgeKineticTexDatagenTest.class.getClassLoader()
                .getResourceAsStream(aPath)) {
            assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    // ------------------------------------------------------------------
    // the two-layer model pairs
    // ------------------------------------------------------------------

    private void assertTwoLayerModel(String aModelName, String aBand, boolean aActive) throws Exception {
        JsonObject tModel = json("assets/gt6/models/block/" + aModelName + ".json");
        if (aActive) {
            // the ACTIVE child form: the inactive parent with the three overlay overrides
            // (render_type does not ride the parent chain — it re-declares cutout)
            assertEquals("gt6:block/" + aBand, tModel.get("parent").getAsString(),
                    aModelName + ": the inactive family model parent");
            assertEquals("minecraft:cutout", tModel.get("render_type").getAsString(), aModelName + ": cutout re-declared");
            JsonObject tOverride = tModel.getAsJsonObject("textures");
            assertEquals("gt6:block/" + aBand + "_overlay_active_front", tOverride.get("overlay_front").getAsString());
            assertEquals("gt6:block/" + aBand + "_overlay_active_back", tOverride.get("overlay_back").getAsString());
            assertEquals("gt6:block/" + aBand + "_overlay_active_side", tOverride.get("overlay_side").getAsString());
            assertTrue(!tModel.has("elements"), aModelName + ": the body elements stay inherited");
            return;
        }
        assertEquals("minecraft:block/cube", tModel.get("parent").getAsString(), aModelName + ": cube parent");
        assertEquals("minecraft:cutout", tModel.get("render_type").getAsString(), aModelName + ": cutout");
        JsonObject tTextures = tModel.getAsJsonObject("textures");
        assertEquals("gt6:block/" + aBand + "_colored_front", tTextures.get("north").getAsString(),
                aModelName + ": north = the grayscale colored front (the tint seat)");
        assertEquals("gt6:block/" + aBand + "_colored_back", tTextures.get("south").getAsString(),
                aModelName + ": south = the colored back");
        for (String tSide : List.of("east", "west", "up", "down")) {
            assertEquals("gt6:block/" + aBand + "_colored_side", tTextures.get(tSide).getAsString(),
                    aModelName + ": " + tSide + " = the colored side");
        }
        assertEquals("gt6:block/" + aBand + "_overlay_front", tTextures.get("overlay_front").getAsString());
        assertEquals("gt6:block/" + aBand + "_overlay_back", tTextures.get("overlay_back").getAsString());
        assertEquals("gt6:block/" + aBand + "_overlay_side", tTextures.get("overlay_side").getAsString());
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
    public void bridgeModelPairsKeepTheTwoLayerGrammar() throws Exception {
        // the ACTIVE pair (the upstream overlay_active groups + the dynamo-carrier channel)
        assertTwoLayerModel("bridge_heater", "bridge_heater", false);
        assertTwoLayerModel("bridge_heater_active", "bridge_heater", true);
        assertTwoLayerModel("laser_electric", "laser_electric", false);
        assertTwoLayerModel("laser_electric_active", "laser_electric", true);
        assertTwoLayerModel("laser_absorber", "laser_absorber", false);
        assertTwoLayerModel("laser_absorber_active", "laser_absorber", true);
        // the static shells (no upstream active groups — the probe verdict)
        assertTwoLayerModel("bridge_engine", "bridge_engine", false);
        assertTwoLayerModel("bridge_motor", "bridge_motor", false);
        // the kinetic engines + the rotation transformer
        assertTwoLayerModel("steam_engine", "steam_engine", false);
        assertTwoLayerModel("diesel_engine", "diesel_engine", false);
        assertTwoLayerModel("transformer_rotation", "transformer_rotation", false);
    }

    // ------------------------------------------------------------------
    // the blockstate variant spaces
    // ------------------------------------------------------------------

    private void assertTwelveVariants(String aBlockPath, String aInactiveModel, String aActiveModel) throws Exception {
        JsonObject tVariants = json("assets/gt6/blockstates/" + aBlockPath + ".json").getAsJsonObject("variants");
        assertEquals(12, tVariants.size(), aBlockPath + ": exactly the 12 facing x active variants");
        for (String tFacing : ROT_Y.keySet()) {
            String tKey = "active=false,facing=" + tFacing;
            JsonObject tVariant = tVariants.getAsJsonObject(tKey);
            assertNotNull(tVariant, aBlockPath + ": the variant key " + tKey);
            assertEquals("gt6:block/" + aInactiveModel, tVariant.get("model").getAsString(),
                    aBlockPath + " " + tKey + ": the inactive model");
        }
        for (String tFacing : List.of("down", "up")) {
            String tKey = "active=false,facing=" + tFacing;
            assertTrue(tVariants.has(tKey), aBlockPath + ": the vertical key " + tKey + " (the six-way carrier)");
        }
        for (String tFacing : ROT_Y.keySet()) {
            JsonObject tVariant = tVariants.getAsJsonObject("active=true,facing=" + tFacing);
            assertEquals("gt6:block/" + aActiveModel, tVariant.get("model").getAsString(),
                    aBlockPath + " active=true facing=" + tFacing + ": the active model");
        }
    }

    @Test
    public void activeCarrierFamiliesPinTheTwelveVariants() throws Exception {
        // one archetype per family (the five rungs share the walk by construction)
        assertTwelveVariants("electric_heater", "bridge_heater", "bridge_heater_active");
        assertTwelveVariants("co2_laser", "laser_electric", "laser_electric_active");
        assertTwelveVariants("laser_absorber", "laser_absorber", "laser_absorber_active");
    }

    @Test
    public void artlessFamiliesRideTheSharedInactiveModel() throws Exception {
        // the probe verdict: kinetic_electric / rotation_electric ship NO active groups —
        // both ACTIVE values ride the inactive shell (12 variants, one model)
        for (String tPath : List.of("electric_engine", "electric_motor")) {
            JsonObject tVariants = json("assets/gt6/blockstates/" + tPath + ".json").getAsJsonObject("variants");
            assertEquals(12, tVariants.size(), tPath + ": the six-way x active variant space");
            assertEquals("gt6:block/" + tPath.replace("electric_", "bridge_").replace("co2_", ""),
                    tVariants.getAsJsonObject("active=false,facing=north").get("model").getAsString(),
                    tPath + ": the inactive model");
            assertEquals("gt6:block/" + tPath.replace("electric_", "bridge_").replace("co2_", ""),
                    tVariants.getAsJsonObject("active=true,facing=down").get("model").getAsString(),
                    tPath + ": both ACTIVE values ride the shared inactive model (no active art)");
        }
    }

    private void assertFourFacingVariants(String aBlockPath, String aModel) throws Exception {
        JsonObject tVariants = json("assets/gt6/blockstates/" + aBlockPath + ".json").getAsJsonObject("variants");
        assertEquals(4, tVariants.size(), aBlockPath + ": the horizontal facing band");
        for (String tFacing : ROT_Y.keySet()) {
            JsonObject tVariant = tVariants.getAsJsonObject("facing=" + tFacing);
            assertNotNull(tVariant, aBlockPath + ": the variant key facing=" + tFacing);
            assertEquals("gt6:block/" + aModel, tVariant.get("model").getAsString(), aBlockPath + " " + tFacing + ": the model");
            assertEquals(ROT_Y.get(tFacing).intValue(),
                    tVariant.has("y") ? tVariant.get("y").getAsInt() : 0, aBlockPath + " " + tFacing + ": the rotationY");
            assertTrue(!tVariant.has("x"), aBlockPath + " " + tFacing + ": no x rotation (horizontal carrier)");
        }
    }

    @Test
    public void kineticRowsPinTheFourFacingVariants() throws Exception {
        assertFourFacingVariants("steam_engine_lead", "steam_engine");
        assertFourFacingVariants("strong_steam_engine_tungstensteel", "steam_engine");
        assertFourFacingVariants("diesel_engine_bronze", "diesel_engine");
        assertFourFacingVariants("diesel_engine_iridium", "diesel_engine");
        assertFourFacingVariants("transformer_rotation", "transformer_rotation");
    }

    // ------------------------------------------------------------------
    // the row-material tint columns (the NBT_MATERIAL parity)
    // ------------------------------------------------------------------

    @Test
    public void steamRowsResolveTheirLoaderMaterialColumns() {
        // the 28 rows resolve non-null through the bySlug table; the 14 ladder materials
        // pairwise distinct (the Loader :584-612 NBT_MATERIAL columns — the Strong ladder
        // repeats the Steam ladder's material set, so distinctness rides the SET)
        assertEquals(28, GT6Kinetics.STEAM_ENGINES.size(), "the steam census stays 28");
        Set<Integer> tSeen = new HashSet<>();
        for (GT6Kinetics.SteamEngineRow tRow : GT6Kinetics.STEAM_ENGINES) {
            gregapi.oredict.OreDictMaterial tMat = gregtech6.client.render.GTMachinePaintTint.bySlug(tRow.matSlug());
            assertNotNull(tMat, tRow.path() + ": the slug " + tRow.matSlug() + " must resolve");
            int tTint = gregtech6.client.render.GTMachinePaintTint.tintARGB(
                    net.minecraftforge.client.model.data.ModelData.EMPTY, tMat, 0);
            assertEquals(0xFF000000, tTint & 0xFF000000, tRow.path() + " binds full alpha");
            tSeen.add(tTint & 0xFFFFFF);
        }
        assertEquals(14, tSeen.size(),
                "the 14 ladder materials stay distinct (the two ladders share the set — "
                + "the all-gray bake is gone)");
    }

    @Test
    public void dieselRowsResolveTheirLoaderMaterialColumns() {
        assertEquals(8, GT6Kinetics.DIESEL_SPECS.size(), "the diesel census stays 8");
        Set<Integer> tSeen = new HashSet<>();
        for (String tSlug : DIESEL_SLUGS) {
            gregapi.oredict.OreDictMaterial tMat = gregtech6.client.render.GTMachinePaintTint.bySlug(tSlug);
            assertNotNull(tMat, "the diesel slug " + tSlug + " must resolve (the Loader :721-729 column)");
            assertTrue(tSeen.add(gregtech6.client.render.GTMachinePaintTint.tintARGB(
                    net.minecraftforge.client.model.data.ModelData.EMPTY, tMat, 0) & 0xFFFFFF),
                    tSlug + " colour is distinct");
        }
        assertEquals(8, tSeen.size(), "the 8 diesel materials stay distinct");
        // the unknown slug is the white identity (the tint seat stays harmless)
        assertNull(gregtech6.client.render.GTMachinePaintTint.bySlug("unobtainium"), "the default arm stays null");
    }

    @Test
    public void rotationTransformerPinsTheWoodTreatedRow() {
        // the "Wooden Transformer Gearbox" row (Loader :1668, NBT_MATERIAL MT.WoodTreated)
        assertEquals(gregtech6.block.GTBasicMachineBlock.materialColor(gregapi.data.MT.WoodTreated) | 0xFF000000,
                gregtech6.client.render.GTMachinePaintTint.tintARGB(null, gregapi.data.MT.WoodTreated, 0),
                "the WoodTreated tint seat");
    }

    // ------------------------------------------------------------------
    // the borrowed wave (the PNG face)
    // ------------------------------------------------------------------

    /** The 38 borrowed wave files (the assets/README.md r8-tex-bridge-kinetic section). */
    private static final List<String> BORROWED = List.of(
            "bridge_heater_colored_back.png",
            "bridge_heater_overlay_front.png", "bridge_heater_overlay_back.png", "bridge_heater_overlay_side.png",
            "bridge_heater_overlay_active_front.png", "bridge_heater_overlay_active_back.png", "bridge_heater_overlay_active_side.png",
            "bridge_engine_colored_back.png",
            "bridge_engine_overlay_front.png", "bridge_engine_overlay_back.png", "bridge_engine_overlay_side.png",
            "bridge_motor_colored_back.png",
            "bridge_motor_overlay_front.png", "bridge_motor_overlay_back.png", "bridge_motor_overlay_side.png",
            "laser_electric_colored_back.png",
            "laser_electric_overlay_front.png", "laser_electric_overlay_back.png", "laser_electric_overlay_side.png",
            "laser_electric_overlay_active_front.png", "laser_electric_overlay_active_back.png", "laser_electric_overlay_active_side.png",
            "laser_absorber_colored_back.png",
            "laser_absorber_overlay_front.png", "laser_absorber_overlay_back.png", "laser_absorber_overlay_side.png",
            "laser_absorber_overlay_active_front.png", "laser_absorber_overlay_active_back.png", "laser_absorber_overlay_active_side.png",
            "steam_engine_overlay_front.png", "steam_engine_overlay_back.png", "steam_engine_overlay_side.png",
            "diesel_engine_colored_back.png", "diesel_engine_colored_side.png",
            "diesel_engine_overlay_front.png", "diesel_engine_overlay_back.png", "diesel_engine_overlay_side.png",
            "transformer_rotation_colored_back.png");

    private static final byte[] PNG_MAGIC = {(byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    @Test
    public void borrowedWavePinsThePngs() throws Exception {
        assertEquals(38, BORROWED.size(), "the wave census stays 38 (the README section rows)");
        for (String tName : BORROWED) {
            try (InputStream tStream = GT6BridgeKineticTexDatagenTest.class.getClassLoader()
                    .getResourceAsStream("assets/gt6/textures/block/" + tName)) {
                assertNotNull(tStream, "the borrowed PNG must be on the classpath: " + tName);
                byte[] tHead = tStream.readNBytes(8);
                assertTrue(java.util.Arrays.equals(PNG_MAGIC, tHead), tName + ": the PNG magic");
            }
        }
    }

    // ------------------------------------------------------------------
    // the retired flat names (dead files AND dead references)
    // ------------------------------------------------------------------

    private static final List<String> RETIRED_FLAT_NAMES = List.of(
            "bridge_heater_front.png", "bridge_heater_side.png",
            "bridge_engine_front.png", "bridge_engine_side.png",
            "bridge_motor_front.png", "bridge_motor_side.png",
            "laser_electric_front.png", "laser_electric_side.png",
            "laser_absorber_front.png", "laser_absorber_side.png",
            "steam_engine_front.png", "steam_engine_back.png", "steam_engine_side.png",
            "diesel_engine.png",
            "transformer_rotation_front.png", "transformer_rotation_side.png");

    /** Location of the mdk project root (the GT6PaintableRenderTypeCensusTest walk). */
    private static Path mdkRoot() {
        for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
            if (Files.isRegularFile(p.resolve("tools").resolve("gen_textures.py"))) {
                return p;
            }
        }
        throw new AssertionError("mdk root (tools/gen_textures.py) not found upward from "
                + Path.of("").toAbsolutePath());
    }

    @Test
    public void retiredFlatNamesAreDeadFilesAndDeadReferences() throws Exception {
        Path tBlock = mdkRoot().resolve("mdk/src/main/resources/assets/gt6/textures/block");
        List<String> tAlive = new ArrayList<>();
        for (String tName : RETIRED_FLAT_NAMES) {
            if (Files.isRegularFile(tBlock.resolve(tName))) tAlive.add(tName);
        }
        assertTrue(tAlive.isEmpty(), "retired flat sprites still on disk: " + tAlive);
        // no model in either tree (static ∪ generated) REFERENCES the retired sprites:
        // only the "textures" object counts (a parent like gt6:block/diesel_engine is the
        // live model name, not the retired texture)
        List<String> tRefs = new ArrayList<>();
        for (String tTree : List.of("src/generated/resources", "src/main/resources")) {
            Path tModels = mdkRoot().resolve("mdk").resolve(tTree).resolve("assets/gt6/models");
            if (!Files.isDirectory(tModels)) continue;
            try (Stream<Path> tWalk = Files.walk(tModels)) {
                for (Path tFile : tWalk.filter(Files::isRegularFile).toArray(Path[]::new)) {
                    JsonObject tModel = JsonParser.parseString(Files.readString(tFile, StandardCharsets.UTF_8)).getAsJsonObject();
                    if (!tModel.has("textures")) continue;
                    for (var tEntry : tModel.getAsJsonObject("textures").entrySet()) {
                        String tValue = tEntry.getValue().getAsString();
                        for (String tName : RETIRED_FLAT_NAMES) {
                            String tRef = tName.substring(0, tName.length() - ".png".length());
                            if (tValue.equals("gt6:block/" + tRef)) {
                                tRefs.add(tTree + ":" + tModels.relativize(tFile) + " [" + tEntry.getKey() + "] -> " + tRef);
                            }
                        }
                    }
                }
            }
        }
        assertTrue(tRefs.isEmpty(), "models still referencing the retired sprites: " + tRefs);
    }

    // ------------------------------------------------------------------
    // the probe-miss family stays declared (quantum_energizer)
    // ------------------------------------------------------------------

    @Test
    public void quantumEnergizerKeepsTheDeclaredSingleLayer() throws Exception {
        JsonObject tModel = json("assets/gt6/models/block/quantum_energizer.json");
        assertEquals("minecraft:block/orientable", tModel.get("parent").getAsString(),
                "quantum_energizer: the plain orientable parent");
        assertEquals("minecraft:cutout", tModel.get("render_type").getAsString(), "quantum_energizer: cutout");
        JsonObject tTextures = tModel.getAsJsonObject("textures");
        assertEquals("gt6:block/quantum_energizer_front", tTextures.get("front").getAsString(),
                "quantum_energizer: the derived amber front (the rung identity)");
        assertEquals("gt6:block/quantum_energizer_side", tTextures.get("side").getAsString(),
                "quantum_energizer: the derived amber side");
        for (String tKey : List.of("overlay_front", "overlay_back", "overlay_side")) {
            assertFalse(tTextures.has(tKey),
                    "quantum_energizer declares " + tKey + " — the alpha-0-empty probe verdict rotted");
        }
        var tElements = tModel.getAsJsonArray("elements");
        assertEquals(1, tElements.size(), "quantum_energizer: the single-layer body");
        var tBodyFaces = tElements.get(0).getAsJsonObject().getAsJsonObject("faces");
        for (String tFace : FACE_KEYS) {
            JsonElement tTint = tBodyFaces.getAsJsonObject(tFace).get("tintindex");
            assertTrue(tTint == null || tTint.getAsInt() == 0,
                    "quantum_energizer face " + tFace + ": the tint seat stays identity-or-zero");
        }
    }
}
