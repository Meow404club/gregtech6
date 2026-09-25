/*
 * Offline pinned-census tests for task p38-issue8-multipart-tint (GitHub issue #8): the
 * generated part-family model JSONs carry tintindex 0 on every BODY face and NO tintindex
 * on the six overlay decals, asserted against the committed src/generated tree (the
 * GT6MachinePaintRenderDatagenTest census shape), and the row tables carry the upstream
 * NBT_MATERIAL column (the tint colour source — the MultiTileEntityMultiBlockPart
 * .getTexture2 :234-236 BlockTextureMulti(BlockTextureDefault(colored, mRGBa), overlay)
 * semantics, mRGBa registration-derived from NBT_MATERIAL, ClassContainer.java:51).
 *
 * <p>Coverage: the 11 Dense Walls (metalwalldense, WALL_ROWS) x designs 0..7 and the 10
 * new-form Metal Walls (metalwall, METAL_WALL_ROWS minus the Tungsten Wall — that block
 * stays the Lightning Rod family's registration and keeps its UNtinted cube_all borrow,
 * the declared deviation pinned by machineWallTungstenKeepsTheUntintedBorrow) x designs
 * 0..7. The partPaintableBlockArray walk (42 blocks: walls + coils + parts + ventilation
 * + processor units + wood wall + transmitter + the coke-oven bricks, whose Ceramic tint
 * task p38-c2-controller-tint wired) rides the same partModel helper, so the
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

    /** The 10 new-form Metal Wall rows riding the datagen walk (the Tungsten Wall row is the Lightning Rod registration — skipped there, the reuse ruling). */
    private static final List<String> METAL_WALLS = List.of(
            "machine_wall_lead", "machine_wall_bronze", "machine_wall_steel",
            "machine_wall_galvanized_steel", "machine_wall_stainless_steel", "machine_wall_invar",
            "machine_wall_titanium", "machine_wall_tungstensteel",
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

    /** One wall blockstate: 8 design variants over the per-design models. */
    private static void assertDesignVariants(String aPath) throws Exception {
        JsonObject tVariants = json("assets/gt6/blockstates/" + aPath + ".json").getAsJsonObject("variants");
        assertEquals(8, tVariants.size(), aPath + " emits one variant per design 0..7");
        for (int d = 0; d <= DESIGNS; d++) {
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
            assertDesignVariants(tPath);
        }
    }

    @Test
    public void metalWallsCarryTintedBodyModels() throws Exception {
        for (String tPath : METAL_WALLS) {
            for (int d = 0; d <= DESIGNS; d++) assertTintedPartModel(tPath, d);
            assertDesignVariants(tPath);
        }
    }

    @Test
    public void machineWallTungstenKeepsTheUntintedBorrow() throws Exception {
        // the reuse ruling: machine_wall_tungsten is the Lightning Rod family's block — its
        // model stays the untinted cube_all borrow (the declared deviation; wiring it would
        // need the DESIGN property, i.e. a registration change, out of the card's scope).
        // cubeAll emits the PARENT form (no inline elements — the tinted partModel shape
        // above is the negative control).
        JsonObject tModel = json("assets/gt6/models/block/machine_wall_tungsten.json");
        assertTrue(!tModel.has("elements") || tModel.getAsJsonArray("elements").isEmpty(),
                "the cube_all borrow carries no inline elements (nothing to tint at the model level)");
        assertEquals("minecraft:block/cube_all", tModel.get("parent").getAsString(), "the borrowed cube_all parent");
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

    /** The rest of the tinted part family — every row resolves a live material (the walk coverage behind partPaintableBlockArray). */
    @Test
    public void remainingPartRowsResolveLiveMaterials() {
        assertNotNull(materialOf("machine_wall_tungsten")); // the row column exists even though the block keeps the borrow
        assertNotNull(rowMaterial(GTMultiBlocks.TRANSMITTER_ROW.material()));
        for (var tRow : GTMultiBlocks.COIL_ROWS) assertNotNull(rowMaterial(tRow.material()), tRow.path());
        for (var tRow : GTMultiBlocks.PART_ROWS) assertNotNull(rowMaterial(tRow.material()), tRow.path());
        assertNotNull(rowMaterial(GTMultiBlocks.VENTILATION_ROW.material()));
        for (var tRow : GTMultiBlocks.PROCESSOR_UNIT_ROWS) assertNotNull(rowMaterial(tRow.material()), tRow.path());
        assertNotNull(rowMaterial(GTMultiBlocks.WOOD_WALL_ROW.material()));
        for (var tRow : GTMultiBlocks.LIGHTNING_ROD_PART_ROWS) assertNotNull(rowMaterial(tRow.material()), tRow.path());
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
