/**
 * The steam-band crafting pin test (task crafting-machines-steam-band): the
 * recipe-bidirectional-census P1 master gate. The machine grids are the upstream
 * registration recipe strings VERBATIM (Loader_MultiTileEntities.java) and the anvil
 * forging ladder is the Loader_Recipes_Handlers.java:175-193 Anvil-row translation —
 * the census pinned the machine family at ZERO vanilla-crafting rows, the "cannot be
 * made" root of the whole production chain.
 *
 * <p>The pins follow the CraftFrom family law (GT6CraftFromDatagenTest): the row counts
 * are the measured item-truth numbers written dead, the universes are SET assertions
 * against the live registrationOrder faces under the verbatim condition legs, and the
 * representative generated-JSON rows get the identity faces (the forge-leg plural tree,
 * the generated-resources classpath).
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregapi.oredict.OreDictMaterial;
import gregtech6.registry.GTMaterialItems;
import gregtech6.tileentity.GTOfflineTestBase;

public class GT6SteamBandCraftingTest extends GTOfflineTestBase {

    @BeforeAll
    static void initMaterialSystem() {
        // the boot order matters: the inherited GTOfflineTestBase boot runs FIRST (the
        // GT6CraftingRecipes class init drags registries → needs the offline Bootstrap)
        GTMaterialItems.initMaterials(); // the force table rides initMaterials (the C2 seam)
    }

    // ------------------------------------------------------------------
    // the anvil forging ladder — the forms and the universes
    // ------------------------------------------------------------------

    private static Set<String> forgeMaterialsOf(String aFormKey) {
        Set<String> rNames = new HashSet<>();
        for (GT6CraftingRecipes.ForgeLadderMaterialRow tRow : GT6CraftingRecipes.forgeLadderMaterialRows()) {
            if (tRow.aForm().aKey().equals(aFormKey)) rNames.add(tRow.aMaterial().mNameInternal);
        }
        return rNames;
    }

    /** Form pin: the six Anvil rows with their upstream prefixes verbatim (:175/:193/:181/:182/:185/:186). */
    @Test
    public void theForgeLadderFormsAreTheUpstreamVerbatim() {
        assertEquals(6, GT6CraftingRecipes.forgeLadderForms().size(), "the six Anvil statements");
        for (GT6CraftingRecipes.ForgeLadderForm tForm : GT6CraftingRecipes.forgeLadderForms()) {
            switch (tForm.aKey()) {
                case "ingot_double" -> { // :175 ingot + ingot → ingotDouble
                    assertEquals(gregapi.data.OP.ingotDouble, tForm.aOutput());
                    assertEquals(gregapi.data.OP.ingot, tForm.aInputA());
                    assertEquals(gregapi.data.OP.ingot, tForm.aInputB(), "the doubled same-prefix pair");
                }
                case "plate" -> { // :193 ingotDouble ×1 → plate — the SINGLE-slot row
                    assertEquals(gregapi.data.OP.plate, tForm.aOutput());
                    assertEquals(gregapi.data.OP.ingotDouble, tForm.aInputA());
                    assertEquals(null, tForm.aInputB(), "the :193 single slot (1 ingotDouble → 1 plate)");
                }
                case "plate_double" -> { // :181 plate + plate → plateDouble
                    assertEquals(gregapi.data.OP.plateDouble, tForm.aOutput());
                    assertEquals(gregapi.data.OP.plate, tForm.aInputA());
                    assertEquals(gregapi.data.OP.plate, tForm.aInputB());
                }
                case "plate_triple" -> { // :182 plate + plateDouble → plateTriple
                    assertEquals(gregapi.data.OP.plateTriple, tForm.aOutput());
                    assertEquals(gregapi.data.OP.plate, tForm.aInputA());
                    assertEquals(gregapi.data.OP.plateDouble, tForm.aInputB());
                }
                case "plate_quadruple" -> { // :185 plateDouble + plateDouble → plateQuadruple
                    assertEquals(gregapi.data.OP.plateQuadruple, tForm.aOutput());
                    assertEquals(gregapi.data.OP.plateDouble, tForm.aInputA());
                    assertEquals(gregapi.data.OP.plateDouble, tForm.aInputB());
                }
                case "plate_quintuple" -> { // :186 plateDouble + plateTriple → plateQuintuple
                    assertEquals(gregapi.data.OP.plateQuintuple, tForm.aOutput());
                    assertEquals(gregapi.data.OP.plateDouble, tForm.aInputA());
                    assertEquals(gregapi.data.OP.plateTriple, tForm.aInputB());
                }
                default -> throw new IllegalArgumentException("unknown form: " + tForm.aKey());
            }
        }
    }

    /** Universe SET pin: every form's rows == the output ∩ input item-truth faces under the verbatim :181 condition fold. */
    @Test
    public void theForgeLadderUniversesAreTheItemTruthSmithFaces() {
        for (GT6CraftingRecipes.ForgeLadderForm tForm : GT6CraftingRecipes.forgeLadderForms()) {
            Set<String> tExpected = new HashSet<>();
            Set<String> tFaceA = new HashSet<>(), tFaceB = new HashSet<>();
            for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
                if (tPair.prefix() == tForm.aInputA()) tFaceA.add(tPair.material().mNameInternal);
                if (tForm.aInputB() != null && tPair.prefix() == tForm.aInputB()) tFaceB.add(tPair.material().mNameInternal);
            }
            for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
                String tName = tPair.material().mNameInternal;
                if (tPair.prefix() != tForm.aOutput() || !tFaceA.contains(tName)) continue;
                if (tForm.aInputB() != null && !tFaceB.contains(tName)) continue;
                OreDictMaterial tMaterial = tPair.material();
                if (!tMaterial.contains(gregapi.data.TD.Processing.SMITHABLE)) continue; // SMITHABLE
                if (tMaterial.contains(gregapi.data.TD.Properties.FLAMMABLE)) continue; // FLAMMABLE.NOT
                if (tMaterial.contains(gregapi.data.TD.Compounds.COATED)) continue; // COATED.NOT
                if (tMaterial.contains(gregapi.data.TD.Atomic.ANTIMATTER)) continue; // ANTIMATTER.NOT
                tExpected.add(tName);
            }
            assertEquals(tExpected, forgeMaterialsOf(tForm.aKey()),
                    "the universe == the item-truth intersection under the verbatim condition: " + tForm.aKey());
        }
    }

    /** Row-count pin (the measured item truth): 6 forms × 302 materials = 1812. */
    @Test
    public void theForgeLadderRowCountIsTheMeasuredItemTruth() {
        assertEquals(1812, GT6CraftingRecipes.forgeLadderMaterialRows().size(), "6 forms x 302 materials");
        for (GT6CraftingRecipes.ForgeLadderForm tForm : GT6CraftingRecipes.forgeLadderForms()) {
            assertEquals(302, forgeMaterialsOf(tForm.aKey()).size(), "the measured face: " + tForm.aKey());
        }
    }

    /** Identity face: the :193 single-slot plate row for Iron — ["A","h"], ONE ingotDouble → ONE plate (the count bug guard). */
    @Test
    public void theIronPlateRowCarriesTheSingleSlotGrid() throws Exception {
        JsonObject tRow = generated("plate/iron");
        assertEquals(2, tRow.getAsJsonArray("pattern").size(), "the 1x2 frame");
        assertEquals("A", tRow.getAsJsonArray("pattern").get(0).getAsString(), "the :193 single slot");
        assertEquals("h", tRow.getAsJsonArray("pattern").get(1).getAsString(), "the hard hammer");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(gregapi.data.OP.ingotDouble, material("Iron")),
                tRow.getAsJsonObject("key").get("A").getAsJsonObject().get("item").getAsString(), "'A' = the ingotDouble");
        assertTrue(tRow.getAsJsonObject("key").get("h").getAsJsonObject().get("tag").getAsString().endsWith("tools/hard_hammer"),
                "'h' = the hard hammer tag");
        assertTrue(tRow.getAsJsonObject("key").get("B") == null, "no phantom B key on the single-slot frame");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(gregapi.data.OP.plate, material("Iron")),
                tRow.getAsJsonObject("result").get("item").getAsString(), "the result item");
    }

    /** Identity face: the :181 plate-double row for Iron — ["AA","h "], plate + plate → plateDouble. */
    @Test
    public void theIronPlateDoubleRowCarriesTheAnvilGrid() throws Exception {
        JsonObject tRow = generated("plate_double/iron");
        assertEquals("AA", tRow.getAsJsonArray("pattern").get(0).getAsString(), "the :181 doubled pair");
        assertEquals("h ", tRow.getAsJsonArray("pattern").get(1).getAsString(), "the hard hammer");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(gregapi.data.OP.plate, material("Iron")),
                tRow.getAsJsonObject("key").get("A").getAsJsonObject().get("item").getAsString(), "'A' = the plate");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(gregapi.data.OP.plateDouble, material("Iron")),
                tRow.getAsJsonObject("result").get("item").getAsString(), "the result item");
    }

    /** Identity face: the :186 plate-quintuple row for Bronze — ["AB","h "], plateDouble + plateTriple → plateQuintuple (the compressor 'P' column). */
    @Test
    public void theBronzePlateQuintupleRowCarriesTheMixedGrid() throws Exception {
        JsonObject tRow = generated("plate_quintuple/bronze");
        assertEquals("AB", tRow.getAsJsonArray("pattern").get(0).getAsString(), "the :186 mixed pair");
        assertEquals("h ", tRow.getAsJsonArray("pattern").get(1).getAsString(), "the hard hammer");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(gregapi.data.OP.plateDouble, material("Bronze")),
                tRow.getAsJsonObject("key").get("A").getAsJsonObject().get("item").getAsString(), "'A' = the plateDouble");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(gregapi.data.OP.plateTriple, material("Bronze")),
                tRow.getAsJsonObject("key").get("B").getAsJsonObject().get("item").getAsString(), "'B' = the plateTriple");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(gregapi.data.OP.plateQuintuple, material("Bronze")),
                tRow.getAsJsonObject("result").get("item").getAsString(), "the result item");
    }

    // ------------------------------------------------------------------
    // the machine grids — the upstream registration strings verbatim
    // ------------------------------------------------------------------

    /** Identity face: the :586 bronze steam engine — "PhP"/"SIS"/"PwP" over plateDouble + stick + springSmall + hammer + wrench. */
    @Test
    public void theBronzeSteamEngineRowCarriesTheUpstreamGrid() throws Exception {
        JsonObject tRow = generated("steam_engine_bronze");
        assertGrid(tRow, "PhP", "SIS", "PwP");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(gregapi.data.OP.plateDouble, material("Bronze")),
                tRow.getAsJsonObject("key").get("P").getAsJsonObject().get("item").getAsString(), "'P' = the plateDouble");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(gregapi.data.OP.stick, material("Bronze")),
                tRow.getAsJsonObject("key").get("S").getAsJsonObject().get("item").getAsString(), "'S' = the stick");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(gregapi.data.OP.springSmall, material("Bronze")),
                tRow.getAsJsonObject("key").get("I").getAsJsonObject().get("item").getAsString(), "'I' = the springSmall");
        assertTrue(tRow.getAsJsonObject("key").get("h").getAsJsonObject().get("tag").getAsString().endsWith("tools/hard_hammer"), "the hammer");
        assertTrue(tRow.getAsJsonObject("key").get("w").getAsJsonObject().get("tag").getAsString().endsWith("tools/wrench"), "the wrench");
        assertEquals("gt6:steam_engine_bronze", tRow.getAsJsonObject("result").get("item").getAsString(), "the result item");
    }

    /** Identity face: the :599 strong lead steam engine — the plateDense + spring columns. */
    @Test
    public void theStrongLeadSteamEngineRowCarriesTheDenseColumn() throws Exception {
        JsonObject tRow = generated("strong_steam_engine_lead");
        assertGrid(tRow, "PhP", "SIS", "PwP");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(gregapi.data.OP.plateDense, material("Lead")),
                tRow.getAsJsonObject("key").get("P").getAsJsonObject().get("item").getAsString(), "'P' = the plateDense");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(gregapi.data.OP.spring, material("Lead")),
                tRow.getAsJsonObject("key").get("I").getAsJsonObject().get("item").getAsString(), "'I' = the spring");
        assertEquals("gt6:strong_steam_engine_lead", tRow.getAsJsonObject("result").get("item").getAsString(), "the result item");
    }

    /** Identity face: the :519 brick burning box — "BBB"/"BBB"/"BFB" over the VANILLA brick item (the oredict ingotBrick bridge) + the firestarter tag. */
    @Test
    public void theBrickBurningBoxRowCarriesTheVanillaBrickBridge() throws Exception {
        JsonObject tRow = generated("brick_burning_box");
        assertGrid(tRow, "BBB", "BBB", "BFB");
        assertEquals("minecraft:brick", tRow.getAsJsonObject("key").get("B").getAsJsonObject().get("item").getAsString(),
                "'B' = the vanilla brick (upstream OP.ingot.dat(MT.Brick) → the ingotBrick oredict face)");
        assertTrue(tRow.getAsJsonObject("key").get("F").getAsJsonObject().get("tag").getAsString().endsWith("tools/flint_and_tinder"),
                "'F' = the firestarter translation (upstream OD.craftingFirestarter)");
        assertEquals("gt6:brick_burning_box", tRow.getAsJsonObject("result").get("item").getAsString(), "the result item");
    }

    /** Identity face: the :524 bronze solid burning box — "PCP"/"PwP"/"BBB" over plate + plateDouble(ANY.Cu→Cu) + the vanilla bricks block + wrench. */
    @Test
    public void theBronzeSolidBurningBoxRowCarriesTheUpstreamGrid() throws Exception {
        JsonObject tRow = generated("burning_box_solid_bronze");
        assertGrid(tRow, "PCP", "PwP", "BBB");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(gregapi.data.OP.plate, material("Bronze")),
                tRow.getAsJsonObject("key").get("P").getAsJsonObject().get("item").getAsString(), "'P' = the row plate");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(gregapi.data.OP.plateDouble, material("Copper")),
                tRow.getAsJsonObject("key").get("C").getAsJsonObject().get("item").getAsString(), "'C' = the ANY.Cu fold → Copper");
        assertEquals("minecraft:bricks", tRow.getAsJsonObject("key").get("B").getAsJsonObject().get("item").getAsString(),
                "'B' = the vanilla bricks block (upstream Blocks.brick_block)");
        assertTrue(tRow.getAsJsonObject("key").get("w").getAsJsonObject().get("tag").getAsString().endsWith("tools/wrench"), "the wrench");
        assertEquals("gt6:burning_box_solid_bronze", tRow.getAsJsonObject("result").get("item").getAsString(), "the result item");
    }

    /** Identity face: the :950 T5 electric dynamo — "TGT"/"CMC"/"TId" over the tier column + the Neodymium magnetic stickLong + the AnnealedCopper fine wires. */
    @Test
    public void theT5DynamoRowCarriesTheUpstreamGrid() throws Exception {
        JsonObject tRow = generated("electric_dynamo_t5");
        assertGrid(tRow, "TGT", "CMC", "TId");
        assertEquals("gt6:casing_machine_double_titanium", tRow.getAsJsonObject("key").get("M").getAsJsonObject().get("item").getAsString(),
                "'M' = the tier housing (the port Electric_T[5] == Ti, the registered ladder)");
        assertEquals("gt6:stick_long_neodymium_magnetic", tRow.getAsJsonObject("key").get("I").getAsJsonObject().get("item").getAsString(),
                "'I' = the T5 magnetic column (upstream :950 NeodymiumMagnetic)");
        assertTrue(tRow.getAsJsonObject("key").get("C").getAsJsonObject().get("tag").getAsString().endsWith("fine_wires/annealed_copper"),
                "'C' = the T3+ wire column folded to the fine-wire tag (upstream wireGt16.dat(MT.AnnealedCopper))");
        assertTrue(tRow.getAsJsonObject("key").get("d").getAsJsonObject().get("tag").getAsString().endsWith("tools/screwdriver"), "the screwdriver");
        assertEquals("gt6:electric_dynamo_t5", tRow.getAsJsonObject("result").get("item").getAsString(), "the result item");
    }

    /** Identity face: the :1312 bronze sifter — "WxW"/"RMR"/"SwS" (x = wire cutter, w = wrench — the SwS row's lowercase letter). */
    @Test
    public void theBronzeSifterRowCarriesTheUpstreamGrid() throws Exception {
        JsonObject tRow = generated("sifter");
        assertGrid(tRow, "WxW", "RMR", "SwS");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(gregapi.data.OP.casingMachineDouble, material("Bronze")),
                tRow.getAsJsonObject("key").get("M").getAsJsonObject().get("item").getAsString(), "'M' = the casing");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(gregapi.data.OP.wireFine, material("Bronze")),
                tRow.getAsJsonObject("key").get("W").getAsJsonObject().get("item").getAsString(), "'W' = the fine wire");
        assertTrue(tRow.getAsJsonObject("key").get("x").getAsJsonObject().get("tag").getAsString().endsWith("tools/wire_cutter"), "the wire cutter");
        assertTrue(tRow.getAsJsonObject("key").get("w").getAsJsonObject().get("tag").getAsString().endsWith("tools/wrench"), "the wrench (the SwS letter)");
        assertEquals("gt6:sifter", tRow.getAsJsonObject("result").get("item").getAsString(), "the result item");
    }

    /** Identity face: the :1343 bronze compressor — "PPR"/"wMS" over plateQuintuple + spring + stick + casing. */
    @Test
    public void theBronzeCompressorRowCarriesTheUpstreamGrid() throws Exception {
        JsonObject tRow = generated("compressor");
        assertGrid(tRow, "PPR", "wMS");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(gregapi.data.OP.plateQuintuple, material("Bronze")),
                tRow.getAsJsonObject("key").get("P").getAsJsonObject().get("item").getAsString(), "'P' = the plateQuintuple");
        assertEquals("gt6:compressor", tRow.getAsJsonObject("result").get("item").getAsString(), "the result item");
    }

    /** Identity face: the :1355 bronze roll bender — "wS "/"GMG"/" Sh" over gear + small gear + casing. */
    @Test
    public void theBronzeRollBenderRowCarriesTheUpstreamGrid() throws Exception {
        JsonObject tRow = generated("rollbender");
        assertGrid(tRow, "wS ", "GMG", " Sh");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(gregapi.data.OP.gearGt, material("Bronze")),
                tRow.getAsJsonObject("key").get("G").getAsJsonObject().get("item").getAsString(), "'G' = the gear");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(gregapi.data.OP.gearGtSmall, material("Bronze")),
                tRow.getAsJsonObject("key").get("S").getAsJsonObject().get("item").getAsString(), "'S' = the small gear");
        assertEquals("gt6:rollbender", tRow.getAsJsonObject("result").get("item").getAsString(), "the result item");
    }

    // ------------------------------------------------------------------
    // the band completeness — the acceptance "逐台有 crafting 配方"
    // ------------------------------------------------------------------

    /** Every registered steam-band machine row has its crafting JSON on the plural tree: 28 engines + 14 burning boxes + 6 dynamos + 4 sifter + 4 compressor + 4 rollbender = 60. */
    @Test
    public void everySteamBandMachineRowHasACraftingFile() {
        int tCount = 0;
        for (gregtech6.registry.GT6Kinetics.SteamEngineRow tRow : gregtech6.registry.GT6Kinetics.STEAM_ENGINES) {
            assertCraftingFile(tRow.path());
            tCount++;
        }
        assertCraftingFile(gregtech6.registry.GT6BurningBoxes.BRICK_ROW.path());
        tCount++;
        for (gregtech6.registry.GT6BurningBoxes.BurningBoxRow tRow : gregtech6.registry.GT6BurningBoxes.SOLID_ROWS) {
            if (tRow.path().startsWith("dense_")) continue; // the Dense ladder → the P1' band
            assertCraftingFile(tRow.path());
            tCount++;
        }
        for (gregtech6.registry.GT6ElectricDynamos.ElectricRow tRow : gregtech6.registry.GT6ElectricDynamos.ROWS) {
            assertCraftingFile(tRow.path());
            tCount++;
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : gregtech6.registry.GTMachines.SIFTER_ROWS) {
            assertCraftingFile(tRow.path());
            tCount++;
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : gregtech6.registry.GTMachines.COMPRESSOR_ROWS) {
            assertCraftingFile(tRow.path());
            tCount++;
        }
        for (gregtech6.block.GTBasicMachineBlock.MachineRow tRow : gregtech6.registry.GTMachines.ROLL_BENDER_ROWS) {
            assertCraftingFile(tRow.path());
            tCount++;
        }
        assertEquals(60, tCount, "the census steam-band face (28 + 14 + 6 + 4x3)");
    }

    private static void assertCraftingFile(String aPath) {
        assertNotNull(GT6SteamBandCraftingTest.class.getResourceAsStream("/data/gt6/recipes/" + aPath + ".json"),
                "the crafting row of " + aPath);
    }

    private static void assertGrid(JsonObject aRow, String... aRows) {
        assertEquals(aRows.length, aRow.getAsJsonArray("pattern").size(), "the pattern height");
        for (int i = 0; i < aRows.length; i++) {
            assertEquals(aRows[i], aRow.getAsJsonArray("pattern").get(i).getAsString(), "the grid row " + i + " verbatim");
        }
    }

    private static JsonObject generated(String aPath) throws Exception {
        String tPath = "/data/gt6/recipes/" + aPath + ".json";
        try (InputStream tStream = GT6SteamBandCraftingTest.class.getResourceAsStream(tPath)) {
            assertNotNull(tStream, tPath + " rides the generated-resources classpath");
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static OreDictMaterial material(String aName) {
        for (OreDictMaterial tMaterial : gregapi.oredict.MaterialRegistry.INSTANCE.MATERIAL_ARRAY) {
            if (tMaterial != null && tMaterial.mID >= 0 && tMaterial.mNameInternal.equals(aName)) return tMaterial;
        }
        throw new IllegalArgumentException("no material: " + aName);
    }
}
