/*
 * Offline pinned-census tests for task issue8-multipart-tint (GitHub issue #8): the
 * generated part-family model JSONs carry tintindex 0 on every BODY face and NO tintindex
 * on the six overlay decals, asserted against the committed src/generated tree (the
 * GT6MachinePaintRenderDatagenTest census shape), and the row tables carry the upstream
 * NBT_MATERIAL column (the tint colour source — the MultiTileEntityMultiBlockPart
 * .getTexture2 :234-236 BlockTextureMulti(BlockTextureDefault(colored, mRGBa), overlay)
 * semantics, mRGBa registration-derived from NBT_MATERIAL, ClassContainer.java:51).
 *
 * <p>Coverage: the 11 Dense Walls (metalwalldense, WALL_ROWS) x designs 0..7 and the 11
 * new-form Metal Walls (metalwall, METAL_WALL_ROWS) x designs 0..7 — machine_wall_tungsten
 * joined the ladder in task debt-tungsten-wall-designs (the Lightning Rod registration
 * now carries the :1151 NBT_DESIGNS 7; its former single-variant pin is retired), and
 * niobium_titanium_coil joined in task debt-coil-design (the :1168 NBT_DESIGNS 1; the
 * same reuse shape). The
 * partPaintableBlockArray walk (44 blocks: walls +
 * coils + parts + ventilation + processor units + wood wall + the tungsten wall + the
 * niobium-titanium coil +
 * transmitter + the coke-oven bricks, whose Ceramic tint
 * task c2-controller-tint wired) rides the same partModel helper, so the
 * body/decal split asserted here covers them; the material columns of those rows are
 * pinned non-null (the upstream aMat verbatim mapping, ANY.Steel→MT.Steel / ANY.W→MT.W,
 * the GT6Crucibles CrucibleRow precedent).
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

import gregtech6.registry.GTMultiBlocks;

class GT6PartPaintRenderDatagenTest {

    /** The 11 Dense Wall rows (metalwalldense family, DESIGNS 7 — the design range 0..7 inclusive). */
    private static final List<String> DENSE_WALLS = List.of(
            "dense_wall_stainless_steel", "dense_wall_invar", "dense_wall_titanium",
            "dense_wall_tungstensteel", "dense_wall_adamantium", "dense_wall_lead",
            "dense_wall_bronze", "dense_wall_steel", "dense_wall_galvanized_steel",
            "dense_wall_tungsten", "dense_wall_tantalum_hafnium_carbide");

    /** The 11 new-form Metal Wall rows riding the datagen walk (machine_wall_tungsten included — task debt-tungsten-wall-designs gave the Lightning Rod registration the :1151 NBT_DESIGNS 7, so it walks the design ladder like its siblings). */
    private static final List<String> METAL_WALLS = List.of(
            "machine_wall_lead", "machine_wall_bronze", "machine_wall_steel",
            "machine_wall_galvanized_steel", "machine_wall_stainless_steel", "machine_wall_invar",
            "machine_wall_titanium", "machine_wall_tungstensteel", "machine_wall_tungsten",
            "machine_wall_tantalum_hafnium_carbide", "machine_wall_adamantium");

    /** The DESIGNS-7 design range (0..N inclusive — IntegerProperty demands min&lt;max). */
    private static final int DESIGNS = 7;

    /** The six body face keys of the block/cube parent. */
    private static final List<String> FACE_KEYS = List.of("down", "up", "north", "south", "west", "east");

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
        try (InputStream tStream = GT6PartPaintRenderDatagenTest.class.getClassLoader()
                .getResourceAsStream(aPath)) {
            assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    /** One wall model: 7 elements, the body cube tinted on all six faces, the six overlay decals untinted. */
    private static void assertTintedPartModel(String aPath, int aDesign) throws Exception {
        JsonObject tModel = json("assets/gt6/models/block/" + aPath + "_design_" + aDesign + ".json");
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
                        aPath + " decal element " + i + " face " + tFace.getKey() + " stays untinted (the BlockTextureMulti overlay pass)");
            }
        }
    }

    /** One part blockstate: designs+1 variants over the per-design models (the row's NBT_DESIGNS range). */
    private static void assertDesignVariants(String aPath, int aDesigns) throws Exception {
        JsonObject tVariants = json("assets/gt6/blockstates/" + aPath + ".json").getAsJsonObject("variants");
        assertEquals(aDesigns + 1, tVariants.size(), aPath + " emits one variant per design 0.." + aDesigns);
        for (int d = 0; d <= aDesigns; d++) {
            JsonObject tEntry = tVariants.getAsJsonObject("design=" + d);
            assertNotNull(tEntry, aPath + " variant design=" + d);
            assertTrue(tEntry.get("model").getAsString().endsWith("block/" + aPath + "_design_" + d),
                    aPath + " design=" + d + " maps to its per-design model");
        }
    }

    @Test
    public void denseWallsCarryTintedBodyModels() throws Exception {
        assertEquals(11, GTMultiBlocks.WALL_ROWS.size(), "the Dense Wall census stays 11 (issue #8's wall family)");
        for (String tPath : DENSE_WALLS) {
            for (int d = 0; d <= DESIGNS; d++) assertTintedPartModel(tPath, d);
            assertDesignVariants(tPath, DESIGNS);
        }
    }

    @Test
    public void metalWallsCarryTintedBodyModels() throws Exception {
        for (String tPath : METAL_WALLS) {
            for (int d = 0; d <= DESIGNS; d++) assertTintedPartModel(tPath, d);
            assertDesignVariants(tPath, DESIGNS);
        }
    }

    /**
     * The Niobium-Titanium Coil (task debt-coil-design): the :1168 row — NBT_DESIGNS 1,
     * line-identical on the column with its five coil siblings (:1167-1172) — walks the
     * two-step design ladder over the Lightning Rod family's registration (the
     * machine_wall_tungsten reuse shape). A revert of the ternary to maxDesign 0
     * collapses the blockstate to the single-state form and goes red on the variant
     * count; a lost row normalization goes red on the tint/texture pins (the former
     * cube_all borrow carried neither).
     */
    @Test
    public void niobiumTitaniumCoilCarriesTheDesignLadder() throws Exception {
        assertEquals(1, GTMultiBlocks.COIL_ROWS.stream()
                .filter(r -> r.path().equals("niobium_titanium_coil")).findFirst().orElseThrow()
                .designs(), "the :1168 row rides the COIL_ROWS census with NBT_DESIGNS 1");
        assertSame(gregapi.data.MT.NiobiumTitanium, materialOf("niobium_titanium_coil"),
                "the row column (the upstream aMat verbatim, the tint source)");
        for (int d = 0; d <= 1; d++) assertTintedPartModel("niobium_titanium_coil", d);
        assertDesignVariants("niobium_titanium_coil", 1);
    }

    // ------------------------------------------------------------------
    // the NBT_MATERIAL columns — the upstream aMat of each Loader row verbatim
    // (Loader_MultiTileEntities.java:1143-1165; ANY.Steel→MT.Steel / ANY.W→MT.W,
    // the GT6Crucibles CrucibleRow mapping)
    // ------------------------------------------------------------------

    @Test
    public void denseWallRowsCarryTheUpstreamMaterials() {
        assertSame(gregapi.data.MT.StainlessSteel  , materialOf("dense_wall_stainless_steel"));
        assertSame(gregapi.data.MT.Invar           , materialOf("dense_wall_invar"));
        assertSame(gregapi.data.MT.Ti              , materialOf("dense_wall_titanium"));
        assertSame(gregapi.data.MT.TungstenSteel   , materialOf("dense_wall_tungstensteel"));
        assertSame(gregapi.data.MT.Ad              , materialOf("dense_wall_adamantium"));
        assertSame(gregapi.data.MT.Pb              , materialOf("dense_wall_lead"));
        assertSame(gregapi.data.MT.Bronze          , materialOf("dense_wall_bronze"));
        assertSame(gregapi.data.MT.Steel           , materialOf("dense_wall_steel"));
        assertSame(gregapi.data.MT.SteelGalvanized , materialOf("dense_wall_galvanized_steel"));
        assertSame(gregapi.data.MT.W               , materialOf("dense_wall_tungsten"));
        assertSame(gregapi.data.MT.Ta4HfC5         , materialOf("dense_wall_tantalum_hafnium_carbide"));
    }

    @Test
    public void metalWallRowsCarryTheUpstreamMaterials() {
        assertSame(gregapi.data.MT.Pb              , materialOf("machine_wall_lead"));
        assertSame(gregapi.data.MT.Bronze          , materialOf("machine_wall_bronze"));
        assertSame(gregapi.data.MT.Steel           , materialOf("machine_wall_steel"));
        assertSame(gregapi.data.MT.SteelGalvanized , materialOf("machine_wall_galvanized_steel"));
        assertSame(gregapi.data.MT.StainlessSteel  , materialOf("machine_wall_stainless_steel"));
        assertSame(gregapi.data.MT.Invar           , materialOf("machine_wall_invar"));
        assertSame(gregapi.data.MT.Ti              , materialOf("machine_wall_titanium"));
        assertSame(gregapi.data.MT.TungstenSteel   , materialOf("machine_wall_tungstensteel"));
        assertSame(gregapi.data.MT.W               , materialOf("machine_wall_tungsten"));
        assertSame(gregapi.data.MT.Ta4HfC5         , materialOf("machine_wall_tantalum_hafnium_carbide"));
        assertSame(gregapi.data.MT.Ad              , materialOf("machine_wall_adamantium"));
    }

    /**
     * The Lightning Rod pillar part (task r11-mains-tint-wrap): the :1179 row —
     * NBT_TEXTURE "lightningrod", NBT_DESIGNS 0, upstream aMat MT.SteelGalvanized —
     * walks the addParts single-model branch over the Lightning Rod family's
     * registration (the machine_wall_tungsten/niobium_titanium_coil reuse shape). The
     * former cube_all {@code lightningrod/rod} borrow (light-gray, ZERO overlay layer —
     * the user-facing "pure white bare pillar" report) is retired: the family two-layer
     * partModel over the {@code parts/lightningrod/0} borrow carries the tintindex-0
     * seat (the SteelGalvanized column multiplies the grayscale wall) + the overlay
     * cap-plate decals (the dark top/bottom art the upstream rod pillar shows). A lost
     * row normalization goes red on the material/texture pins; a revert to the cube_all
     * form goes red on the two-layer shape.
     */
    @Test
    public void lightningRodPillarJoinsTheFamilyForm() throws Exception {
        assertSame(gregapi.data.MT.SteelGalvanized, materialOf("lightning_rod"),
                "the :1179 row column (the upstream aMat verbatim, the tint source)");
        JsonObject tModel = json("assets/gt6/models/block/lightning_rod.json");
        assertEquals(7, tModel.getAsJsonArray("elements").size(), "lightning_rod keeps the two-layer shape (body + 6 decals)");
        JsonObject tTextures = tModel.getAsJsonObject("textures");
        assertEquals("gt6:block/parts/lightningrod/0/colored/bottom", tTextures.get("down").getAsString(),
                "lightning_rod down: the borrowed colored layer");
        assertEquals("gt6:block/parts/lightningrod/0/colored/top", tTextures.get("up").getAsString(),
                "lightning_rod up: the borrowed colored layer");
        for (String tFace : List.of("north", "south", "west", "east")) {
            assertEquals("gt6:block/parts/lightningrod/0/colored/side", tTextures.get(tFace).getAsString(),
                    "lightning_rod " + tFace + ": the borrowed colored side (the :1179 texture key)");
        }
        for (String tFace : List.of("overlay_bottom", "overlay_top", "overlay_side")) {
            assertTrue(tTextures.get(tFace).getAsString().startsWith("gt6:block/parts/lightningrod/0/overlay/"),
                    "lightning_rod " + tFace + ": the overlay cap-plate decals");
        }
        JsonObject tBody = tModel.getAsJsonArray("elements").get(0).getAsJsonObject();
        for (Map.Entry<String, JsonElement> tFace : tBody.getAsJsonObject("faces").entrySet()) {
            assertEquals(0, tFace.getValue().getAsJsonObject().get("tintindex").getAsInt(),
                    "lightning_rod body face " + tFace.getKey() + " carries tintindex 0 (the SteelGalvanized tint)");
        }
        for (int i = 1; i < 7; i++) {
            JsonObject tDecal = tModel.getAsJsonArray("elements").get(i).getAsJsonObject();
            for (Map.Entry<String, JsonElement> tFace : tDecal.getAsJsonObject("faces").entrySet()) {
                assertNull(tFace.getValue().getAsJsonObject().get("tintindex"),
                        "lightning_rod decal " + i + " face " + tFace.getKey() + " stays untinted (the overlay pass)");
            }
        }
        JsonObject tVariants = json("assets/gt6/blockstates/lightning_rod.json").getAsJsonObject("variants");
        assertEquals(1, tVariants.size(), "lightning_rod: the single-state blockstate (NBT_DESIGNS 0)");
        assertEquals("gt6:block/lightning_rod", tVariants.getAsJsonObject(tVariants.keySet().iterator().next())
                .get("model").getAsString(), "lightning_rod state maps to the family two-layer model");
    }

    /** The rest of the tinted part family — every row resolves a live material (the walk coverage behind partPaintableBlockArray). */
    @Test
    public void remainingPartRowsResolveLiveMaterials() {
        assertNotNull(materialOf("machine_wall_tungsten")); // the row column (the block keeps the Lightning Rod registration)
        assertNotNull(rowMaterial(GTMultiBlocks.TRANSMITTER_ROW.material()));
        for (var tRow : GTMultiBlocks.COIL_ROWS) assertNotNull(rowMaterial(tRow.material()), tRow.path());
        for (var tRow : GTMultiBlocks.PART_ROWS) assertNotNull(rowMaterial(tRow.material()), tRow.path());
        assertNotNull(rowMaterial(GTMultiBlocks.VENTILATION_ROW.material()));
        for (var tRow : GTMultiBlocks.PROCESSOR_UNIT_ROWS) assertNotNull(rowMaterial(tRow.material()), tRow.path());
        assertNotNull(rowMaterial(GTMultiBlocks.WOOD_WALL_ROW.material()));
        assertNotNull(rowMaterial(GTMultiBlocks.LIGHTNING_ROD_PILLAR_ROW.material()));
        for (var tRow : GTMultiBlocks.LIGHTNING_ROD_PART_ROWS) assertNotNull(rowMaterial(tRow.material()), tRow.path());
    }

    // ------------------------------------------------------------------
    // task issue8-residual — the #8 stragglers: the 25 tank valve controllers
    // (addTanks, the two shared tintedCube models) and the 8 crucible walls
    // (addLargeCrucible, the dedicated GTCrucibleWallBlock over the metalwall borrow)
    // ------------------------------------------------------------------

    /**
     * The valve family model mapping: a wood-row and a metal-row blockstate both map
     * their FACING×FORMED variants onto the family model. (Task tex-tank-family
     * rewire: the two shared models left the woodwall/metalwall part borrows for the
     * dedicated tank_valves two-layer front-pair art — the shape pins live in
     * {@link GT6TankFamilyPaintRenderDatagenTest#valveModelsKeepTheFrontLayerPair}.)
     */
    @Test
    public void tankValvesCarryTintedBodyModels() throws Exception {
        // the wood valve is the flammable row (tank_wood), every metal valve the tank_metal model
        assertAllVariantsMapToModel("tank_wood", "block/tank_wood");
        assertAllVariantsMapToModel("tank_small_tungstensteel", "block/tank_metal");
    }

    /** The 25 valve rows carry the upstream NBT_MATERIAL column (Loader :1195-1222 verbatim; ANY.W→MT.W). */
    @Test
    public void tankValveRowsCarryTheUpstreamMaterials() {
        var tRows = gregtech6.registry.GT6Tanks.ROWS;
        assertEquals(25, tRows.size(), "the tank valve census stays 25 (the w3-tank-valves family)");
        for (var tRow : tRows) assertNotNull(rowMaterial(tRow.material()), tRow.path());
        assertSame(gregapi.data.MT.WoodTreated    , valveMaterial(tRows, "tank_wood"));
        assertSame(gregapi.data.MT.StainlessSteel , valveMaterial(tRows, "tank_small_stainless_steel"));
        assertSame(gregapi.data.MT.Invar          , valveMaterial(tRows, "tank_small_invar"));
        assertSame(gregapi.data.MT.Ti             , valveMaterial(tRows, "tank_small_titanium"));
        assertSame(gregapi.data.MT.TungstenSteel  , valveMaterial(tRows, "tank_small_tungstensteel"));
        assertSame(gregapi.data.MT.W              , valveMaterial(tRows, "tank_small_tungsten"));
        assertSame(gregapi.data.MT.Ad             , valveMaterial(tRows, "tank_small_adamantium"));
        assertSame(gregapi.data.MT.StainlessSteel , valveMaterial(tRows, "tank_large_dense_stainless_steel"));
        assertSame(gregapi.data.MT.Ad             , valveMaterial(tRows, "tank_large_dense_adamantium"));
    }

    /**
     * The 8 crucible walls ride the metalwall DESIGN ladder (task mb-formed-crucible-wall):
     * the same partModel two-layer form as the machine_wall_* siblings over the per-design
     * borrowed textures (the upstream metalwall NBT_DESIGNS 7 — Loader:1143-1153; the formed
     * crucible repaints design 4, MultiTileEntityCrucible.java:124-128), one variant per
     * design 0..7. A ctor revert to maxDesign 0 collapses the blockstate to the singleton
     * form and goes red on the variant count (the niobiumTitaniumCoil revert shape).
     */
    @Test
    public void crucibleWallsCarryTheDesignLadderModels() throws Exception {
        for (String tPath : crucibleWallPaths()) {
            for (int d = 0; d <= DESIGNS; d++) assertTintedPartModel(tPath, d);
            assertDesignVariants(tPath, DESIGNS);
        }
    }

    /** The 8 dedicated crucible wall paths (the steel rung + the seven ladder rows). */
    private static java.util.List<String> crucibleWallPaths() {
        java.util.List<String> rPaths = new java.util.ArrayList<>(java.util.List.of("crucible_steel_wall"));
        rPaths.addAll(gregtech6.registry.GT6Crucibles.CRUCIBLE_WALL_BLOCKS_BY_PATH.keySet());
        return rPaths;
    }

    /**
     * The FORMED design write resolves END TO END (task crucible-render-followup, the
     * symptom-A nail): the pattern's {@code FORMED_WALL_DESIGN} value has a blockstate
     * variant whose model file exists and whose every texture resolves to a PNG on disk.
     * The structure test pins the runtime write (design 4 lands in the state); this pin
     * closes the static half — a variant pointing at a missing model/texture is the exact
     * "formed wall renders bare" surface (the vanilla missing-variant fallback,
     * ModelBakery.java:346-348, bakes the purple MISSING_MODEL there).
     */
    @Test
    public void formedWallDesignResolvesToAModelWithTextures() throws Exception {
        int tFormed = gregtech6.tileentity.multiblocks.TileEntityCrucible.formedWallDesign();
        assertTrue(tFormed >= 0 && tFormed <= DESIGNS, "the formed design lands inside the wall ladder 0.." + DESIGNS);
        for (String tPath : crucibleWallPaths()) {
            JsonObject tVariants = json("assets/gt6/blockstates/" + tPath + ".json").getAsJsonObject("variants");
            JsonObject tVariant = tVariants.getAsJsonObject("design=" + tFormed);
            assertNotNull(tVariant, tPath + " blockstate carries the design=" + tFormed + " (FORMED_WALL_DESIGN) variant");
            String tModelRef = tVariant.get("model").getAsString();
            int tColon = tModelRef.indexOf(':');
            JsonObject tModel = json("assets/" + tModelRef.substring(0, tColon) + "/models/"
                    + tModelRef.substring(tColon + 1) + ".json");
            for (Map.Entry<String, JsonElement> tTexture : tModel.getAsJsonObject("textures").entrySet()) {
                String tTexRef = tTexture.getValue().getAsString();
                int tTexColon = tTexRef.indexOf(':');
                String tTexPath = "assets/" + tTexRef.substring(0, tTexColon) + "/textures/"
                        + tTexRef.substring(tTexColon + 1) + ".png";
                assertNotNull(getClass().getClassLoader().getResourceAsStream(tTexPath),
                        tPath + " design=" + tFormed + " texture " + tTexture.getKey() + " -> " + tTexRef + " exists on disk");
            }
        }
    }

    /** The 8 crucible wall rows carry the upstream NBT_MATERIAL column (Loader :1145/:1270-1277 verbatim; ANY.W→MT.W). */
    @Test
    public void crucibleWallRowsCarryTheUpstreamMaterials() {
        // the ROW tables, not the RegistryObjects (the offline test never runs the mod-bus
        // registration — the GT6Tanks ROWS reading shape)
        assertSame(gregapi.data.MT.Steel          , gregtech6.registry.GT6Crucibles.STEEL_WALL_ROW.material());
        assertSame(gregapi.data.MT.StainlessSteel , gregtech6.registry.GT6Crucibles.STAINLESS_STEEL_ROW.material());
        assertSame(gregapi.data.MT.Invar          , gregtech6.registry.GT6Crucibles.INVAR_ROW.material());
        assertSame(gregapi.data.MT.Ti             , gregtech6.registry.GT6Crucibles.TITANIUM_ROW.material());
        assertSame(gregapi.data.MT.TungstenSteel  , gregtech6.registry.GT6Crucibles.TUNGSTENSTEEL_ROW.material());
        assertSame(gregapi.data.MT.W              , gregtech6.registry.GT6Crucibles.TUNGSTEN_ROW.material());
        assertSame(gregapi.data.MT.Ta4HfC5        , gregtech6.registry.GT6Crucibles.TANTALUM_HAFNIUM_CARBIDE_ROW.material());
        assertSame(gregapi.data.MT.Ad             , gregtech6.registry.GT6Crucibles.ADAMANTIUM_ROW.material());
    }

    /** One FACING×FORMED blockstate: every variant maps onto the family model. */
    private static void assertAllVariantsMapToModel(String aPath, String aModel) throws Exception {
        JsonObject tVariants = json("assets/gt6/blockstates/" + aPath + ".json").getAsJsonObject("variants");
        assertEquals(8, tVariants.size(), aPath + " emits the 4 FACING x 2 FORMED variants");
        for (Map.Entry<String, JsonElement> tVariant : tVariants.entrySet()) {
            assertEquals("gt6:" + aModel, tVariant.getValue().getAsJsonObject().get("model").getAsString(),
                    aPath + " variant " + tVariant.getKey());
        }
    }

    private static gregapi.oredict.OreDictMaterial valveMaterial(java.util.List<gregtech6.registry.GT6Tanks.TankValveRow> aRows, String aPath) {
        for (var tRow : aRows) if (tRow.path().equals(aPath)) return rowMaterial(tRow.material());
        return null;
    }

    private static gregapi.oredict.OreDictMaterial rowMaterial(
            java.util.function.Supplier<gregapi.oredict.OreDictMaterial> aMaterial) {
        return aMaterial.get();
    }

    private static gregapi.oredict.OreDictMaterial materialOf(String aPath) {
        for (var tRow : GTMultiBlocks.WALL_ROWS) if (tRow.path().equals(aPath)) return rowMaterial(tRow.material());
        for (var tRow : GTMultiBlocks.METAL_WALL_ROWS) if (tRow.path().equals(aPath)) return rowMaterial(tRow.material());
        for (var tRow : GTMultiBlocks.LIGHTNING_ROD_PART_ROWS) if (tRow.path().equals(aPath)) return rowMaterial(tRow.material());
        return null;
    }
}
