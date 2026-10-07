/**
 * The non-steam machine-body crafting pin test (task machines-crafting-pour): the
 * machine-port-prereq-census P1' card. The band pours every GTMachines family's
 * crafting-table housing frame the P1 steam band left (the grids are the ACTIVE
 * upstream registration rows VERBATIM, Loader_MultiTileEntities.java machines1
 * :1288 - machines4 :1657) and CUT ledger pins the families whose inputs have no
 * port items (the gap ledger, no item fabricated).
 *
 * <p>The pins follow the P1 steam-band law (GT6SteamBandCraftingTest): the row counts
 * are the measured family-truth numbers written dead, the representative generated-JSON
 * rows get the identity faces (the forge-leg plural tree, the generated-resources
 * classpath), and the CUT faces get the negative file pins.
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregapi.oredict.OreDictMaterial;
import gregtech6.block.GTBasicMachineBlock;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTMachines;
import gregtech6.tileentity.GTOfflineTestBase;

public class GT6MachinesCraftingPourTest extends GTOfflineTestBase {

    @BeforeAll
    static void initMaterialSystem() {
        // the boot order matters: the inherited GTOfflineTestBase boot runs FIRST (the
        // GTMachines class init drags registries → needs the offline Bootstrap)
        GTMaterialItems.initMaterials(); // the force table rides initMaterials (the C2 seam)
    }

    // ------------------------------------------------------------------
    // the band completeness — the family count reconciliation (家数对账)
    // ------------------------------------------------------------------

    /** Every poured family row has its crafting JSON on the plural tree. The walk mirrors {@code GT6CraftingRecipes.machinePourBand()} family-for-family. */
    @Test
    public void everyPouredMachineFamilyRowHasACraftingFile() {
        int tCount = 0;
        // machines1, the Kinetic_T frames (the flat-constant trio + the row-list families)
        tCount += assertFamily(4, "shredder", "shredder_t2", "shredder_t3", "shredder_t4");
        tCount += assertFamily(4, "crusher", "crusher_t2", "crusher_t3", "crusher_t4");
        tCount += assertFamily(4, "lathe", "lathe_t2", "lathe_t3", "lathe_t4");
        tCount += assertRows(GTMachines.BUZZSAW_ROWS, GTMachines.BUZZSAW_ITEMS_BY_PATH);
        tCount += assertRows(GTMachines.SQUEEZER_ROWS, GTMachines.SQUEEZER_ITEMS_BY_PATH);
        tCount += assertRows(GTMachines.CENTRIFUGE_ROWS, GTMachines.CENTRIFUGE_ITEMS_BY_PATH);
        // machines1, the Electric + Kinetic singles
        tCount += assertRows(GTMachines.ELECTROLYZER_ROWS, GTMachines.ELECTROLYZER_ITEMS_BY_PATH);
        tCount += assertRows(GTMachines.ROLLINGMILL_RU_ROWS, GTMachines.ROLLINGMILL_ITEMS_BY_PATH);
        tCount += assertRows(GTMachines.ROLL_FORMER_ROWS, GTMachines.ROLLFORMER_ITEMS_BY_PATH);
        tCount += assertRows(GTMachines.CLUSTER_MILL_ROWS, GTMachines.CLUSTERMILL_ITEMS_BY_PATH);
        tCount += assertRows(GTMachines.WIREMILL_ROWS, GTMachines.WIREMILL_ITEMS_BY_PATH);
        // the Heat_T frames
        tCount += assertFamily(4, "oven", "oven_t2", "oven_t3", "oven_t4");
        tCount += assertRows(GTMachines.ROASTING_ROWS, GTMachines.ROASTING_ITEMS_BY_PATH);
        tCount += assertRows(GTMachines.MIXER_ROWS, GTMachines.MIXER_ITEMS_BY_PATH);
        tCount += assertRows(GTMachines.DISTILLERY_ROWS, GTMachines.DISTILLERY_ITEMS_BY_PATH);
        tCount += assertRows(GTMachines.EXTRUDER_ROWS, GTMachines.EXTRUDER_ITEMS_BY_PATH);
        tCount += assertRows(GTMachines.LOOM_ROWS, GTMachines.LOOM_ITEMS_BY_PATH);
        // machines2
        tCount += assertRows(GTMachines.POLARIZER_ROWS, GTMachines.POLARIZER_ITEMS_BY_PATH);
        tCount += assertRows(GTMachines.PRESS_ROWS, GTMachines.PRESS_ITEMS_BY_PATH);
        tCount += assertRows(GTMachines.INJECTOR_ROWS, GTMachines.INJECTOR_ITEMS_BY_PATH);
        tCount += assertRows(GTMachines.PRINTER_ROWS, GTMachines.PRINTER_ITEMS_BY_PATH);
        tCount += assertRows(GTMachines.SCANNER_VISUALS_ROWS, GTMachines.SCANNER_VISUALS_ITEMS_BY_PATH);
        tCount += assertRows(GTMachines.SLUICE_ROWS, GTMachines.SLUICE_ITEMS_BY_PATH);
        tCount += assertRows(GTMachines.MAGNETIC_SEPARATOR_ROWS, GTMachines.MAGNETIC_SEPARATOR_ITEMS_BY_PATH);
        tCount += assertRows(GTMachines.DRYER_ROWS, GTMachines.DRYER_ITEMS_BY_PATH);
        tCount += assertRows(GTMachines.LASER_ENGRAVER_ROWS, GTMachines.LASER_ENGRAVER_ITEMS_BY_PATH);
        tCount += assertRows(GTMachines.AUTOCRAFTER_ROWS, GTMachines.AUTOCRAFTER_ITEMS_BY_PATH);
        tCount += assertRows(GTMachines.ELECTRIC_MIXER_ROWS, GTMachines.ELECTRIC_MIXER_ITEMS_BY_PATH);
        tCount += assertRows(GTMachines.ELECTRIC_LOOM_ROWS, GTMachines.ELECTRIC_LOOM_ITEMS_BY_PATH);
        tCount += assertRows(GTMachines.ELECTRIC_SIFTER_ROWS, GTMachines.ELECTRIC_SIFTER_ITEMS_BY_PATH);
        tCount += assertRows(GTMachines.SLICER_ROWS, GTMachines.SLICER_ITEMS_BY_PATH);
        tCount += assertRows(GTMachines.LAMINATOR_ROWS, GTMachines.LAMINATOR_ITEMS_BY_PATH);
        // machines3, the exotic rungs
        tCount += assertRows(GTMachines.MASSFAB_SMALL_ROWS, GTMachines.MASSFAB_SMALL_ITEMS_BY_PATH);
        tCount += assertRows(GTMachines.REPLICATOR_ROWS, GTMachines.REPLICATOR_ITEMS_BY_PATH);
        tCount += assertRows(GTMachines.SANDING_ROWS, GTMachines.SANDING_ITEMS_BY_PATH);
        tCount += assertRows(GTMachines.PLANTALYZER_ROWS, GTMachines.PLANTALYZER_ITEMS_BY_PATH);
        tCount += assertRows(GTMachines.BUMBLELYZER_ROWS, GTMachines.BUMBLELYZER_ITEMS_BY_PATH);
        tCount += assertRows(GTMachines.PRESSURE_WASHER_ROWS, GTMachines.PRESSURE_WASHER_ITEMS_BY_PATH);
        tCount += assertRows(GTMachines.FREEZER_ROWS, GTMachines.FREEZER_ITEMS_BY_PATH);
        tCount += assertRows(GTMachines.CRYO_MIXER_ROWS, GTMachines.CRYO_MIXER_ITEMS_BY_PATH);
        tCount += assertRows(GTMachines.BOXINATOR_ROWS, GTMachines.BOXINATOR_ITEMS_BY_PATH);
        tCount += assertRows(GTMachines.UNBOXINATOR_ROWS, GTMachines.UNBOXINATOR_ITEMS_BY_PATH);
        // machines4, the single-variant rungs
        tCount += assertRows(GTMachines.COAGULATOR_ROWS, GTMachines.COAGULATOR_ITEMS_BY_PATH);
        tCount += assertRows(GTMachines.GENERIFIER_ROWS, GTMachines.GENERIFIER_ITEMS_BY_PATH);
        tCount += assertRows(GTMachines.BATH_ROWS, GTMachines.BATH_ITEMS_BY_PATH);
        tCount += assertRows(GTMachines.FERMENTER_ROWS, GTMachines.FERMENTER_ITEMS_BY_PATH);
        tCount += assertRows(GTMachines.AUTOCLAVE_ROWS, GTMachines.AUTOCLAVE_ITEMS_BY_PATH);
        tCount += assertRows(GTMachines.MELTER_ROWS, GTMachines.MELTER_ITEMS_BY_PATH);
        // the measured walk total (the port row lists ARE the truth — 187 = 12 flat-trio
        // + 43 row-list families over the 4/5/3-rung port ladders + 6 single-variant
        // rungs; the replicator carries 3 rungs, the port registration face)
        assertEquals(187, tCount, "the measured family face of the pour walk");
    }

    /** The walk of one MachineRow family: every row's item registered AND its crafting file on the tree; returns the row count. */
    private static int assertRows(java.util.List<GTBasicMachineBlock.MachineRow> aRows, java.util.Map<String, ? extends java.util.function.Supplier<net.minecraft.world.item.Item>> aItems) {
        for (GTBasicMachineBlock.MachineRow tRow : aRows) {
            assertNotNull(aItems.get(tRow.path()), "the family item of " + tRow.path());
            assertCraftingFile(tRow.path());
        }
        return aRows.size();
    }

    /** The walk of one flat-constant family (the OvenRow shape): the paths dead-listed. */
    private static int assertFamily(int aExpected, String... aPaths) {
        assertEquals(aExpected, aPaths.length, "the dead list size");
        for (String tPath : aPaths) assertCraftingFile(tPath);
        return aPaths.length;
    }

    // ------------------------------------------------------------------
    // the CUT ledger — the gap-list negative pins (勿硬造)
    // ------------------------------------------------------------------

    /** The Canner family ships NO crafting rows (IL.PUMPS[t] has no port item — the gap ledger; the machine items themselves exist). */
    @Test
    public void theCannerFamilyShipsNoCraftingRows() {
        for (GTBasicMachineBlock.MachineRow tRow : GTMachines.CANNER_ROWS) {
            assertNotNull(GTMachines.CANNER_ITEMS_BY_PATH.get(tRow.path()), "the canner machine item exists: " + tRow.path());
            assertNull(craftingFileOrNull(tRow.path()), "no crafting row for the unregistered-input family: " + tRow.path());
        }
    }

    /** The Smelter and Crystallisation Crucible families ship NO crafting rows (the tungsten/graphite/Ta4HfC5/quartz/iridium small crucibles have no port items). */
    @Test
    public void theCrucibleCoreFamiliesShipNoCraftingRows() {
        for (GTBasicMachineBlock.MachineRow tRow : GTMachines.SMELTER_ROWS) assertNull(craftingFileOrNull(tRow.path()), "no crafting row: " + tRow.path());
        for (GTBasicMachineBlock.MachineRow tRow : GTMachines.CRYSTALLISATION_ROWS) assertNull(craftingFileOrNull(tRow.path()), "no crafting row: " + tRow.path());
        // the Melter sibling POURS — its ceramic crucible is the one small rung the port registers
        assertCraftingFile("melter");
    }

    /** The Laser Welder family ships NO crafting rows (the DYE_OREDICTS_LENS[Yellow] lens family is pooled, GTMachines:2032) and the Lightning Processor ships none ('X' = wireGt01 over ANY.Iron, no iron wire rung — the upstream input itself is dead). */
    @Test
    public void theLensAndIronWireFamiliesShipNoCraftingRows() {
        for (GTBasicMachineBlock.MachineRow tRow : GTMachines.LASER_WELDER_ROWS) assertNull(craftingFileOrNull(tRow.path()), "no crafting row: " + tRow.path());
        for (GTBasicMachineBlock.MachineRow tRow : GTMachines.LIGHTNING_ROWS) assertNull(craftingFileOrNull(tRow.path()), "no crafting row: " + tRow.path());
    }

    // ------------------------------------------------------------------
    // the identity faces — the representative generated rows
    // ------------------------------------------------------------------

    /** Identity face: the :1294 bronze shredder — "GDG"/"hMw" over gear + plateGem(Diamond) + casingMachineDouble + hammer + wrench. */
    @Test
    public void theBronzeShredderRowCarriesTheUpstreamGrid() throws Exception {
        JsonObject tRow = generated("shredder");
        assertGrid(tRow, "GDG", "hMw");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(gregapi.data.OP.gearGt, material("Bronze")),
                tRow.getAsJsonObject("key").get("G").getAsJsonObject().get("item").getAsString(), "'G' = the gear");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(gregapi.data.OP.plateGem, material("Diamond")),
                tRow.getAsJsonObject("key").get("D").getAsJsonObject().get("item").getAsString(), "'D' = the plateGem (the ANY.Diamond fold)");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(gregapi.data.OP.casingMachineDouble, material("Bronze")),
                tRow.getAsJsonObject("key").get("M").getAsJsonObject().get("item").getAsString(), "'M' = the casing");
        assertEquals("gt6:shredder", tRow.getAsJsonObject("result").get("item").getAsString(), "the result item");
    }

    /** Identity face: the :1288 steel oven — "wMh"/"BCB" over casingMachine + plateDouble(ANY.Cu→Cu) + the vanilla bricks. */
    @Test
    public void theSteelOvenRowCarriesTheUpstreamGrid() throws Exception {
        JsonObject tRow = generated("oven");
        assertGrid(tRow, "wMh", "BCB");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(gregapi.data.OP.casingMachine, material("Steel")),
                tRow.getAsJsonObject("key").get("M").getAsJsonObject().get("item").getAsString(), "'M' = the casing (the REAL casing_machine item)");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(gregapi.data.OP.plateDouble, material("Copper")),
                tRow.getAsJsonObject("key").get("C").getAsJsonObject().get("item").getAsString(), "'C' = the ANY.Cu fold");
        assertEquals("minecraft:bricks", tRow.getAsJsonObject("key").get("B").getAsJsonObject().get("item").getAsString(),
                "'B' = the vanilla bricks block (upstream Blocks.brick_block)");
        assertEquals("gt6:oven", tRow.getAsJsonObject("result").get("item").getAsString(), "the result item");
    }

    /** Identity face: the :1336 LV electrolyzer — "SMS"/"WwW" over the fixed Pt wireGt01 + the tier cable column (CABLES_01[1] = Sn). */
    @Test
    public void theLvElectrolyzerRowCarriesTheWireColumns() throws Exception {
        JsonObject tRow = generated("electrolyzer");
        assertGrid(tRow, "SMS", "WwW");
        assertEquals("gt6:wire_platinum_gt01", tRow.getAsJsonObject("key").get("S").getAsJsonObject().get("item").getAsString(),
                "'S' = the fixed Pt wire (upstream wireGt01.dat(MT.Pt), the wire-family item face)");
        assertEquals("gt6:cable_tin_gt01", tRow.getAsJsonObject("key").get("W").getAsJsonObject().get("item").getAsString(),
                "'W' = the CABLES_01[1] column (the WIRE_TOKENS tier ladder)");
        assertEquals("gt6:electrolyzer", tRow.getAsJsonObject("result").get("item").getAsString(), "the result item");
    }

    /** Identity face: the :1621 T1 freezer and the :1625 T5 rung — "hPw"/"PMP"/"PSP" over the Si plate ladder rungs. */
    @Test
    public void theFreezerRowsCarryTheSiliconPlateLadder() throws Exception {
        JsonObject tRow = generated("freezer");
        assertGrid(tRow, "hPw", "PMP", "PSP");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(gregapi.data.OP.plate, material("Silicon")),
                tRow.getAsJsonObject("key").get("S").getAsJsonObject().get("item").getAsString(), "T1 'S' = the single plate");
        JsonObject tT5 = generated("freezer_t5");
        assertEquals("gt6:" + GTMaterialItems.itemIdOf(gregapi.data.OP.plateQuintuple, material("Silicon")),
                tT5.getAsJsonObject("key").get("S").getAsJsonObject().get("item").getAsString(), "T5 'S' = the plateQuintuple");
        assertEquals("gt6:freezer_t5", tT5.getAsJsonObject("result").get("item").getAsString(), "the T5 result item");
    }

    /** Identity face: the :1542 T1 mass fabricator — "RFS"/"FMF"/"RFS" over the ruby/sapphire gem tags + the fixed LV field generator. */
    @Test
    public void theMassfabRowCarriesTheCrystalComponentColumns() throws Exception {
        JsonObject tRow = generated("massfab");
        assertGrid(tRow, "RFS", "FMF", "RFS");
        assertTrue(tRow.getAsJsonObject("key").get("R").getAsJsonObject().get("tag").getAsString().endsWith("gems/ruby"),
                "'R' = the ruby gem tag (the molecularScannerRow fold of IL.Processor_Crystal_Ruby)");
        assertTrue(tRow.getAsJsonObject("key").get("S").getAsJsonObject().get("tag").getAsString().endsWith("gems/sapphire"), "'S' = the sapphire gem tag");
        assertEquals("gt6:field_generator_lv", tRow.getAsJsonObject("key").get("F").getAsJsonObject().get("item").getAsString(),
                "'F' = the fixed FIELD_GENERATORS[1] column");
        assertEquals("gt6:massfab", tRow.getAsJsonObject("result").get("item").getAsString(), "the result item");
    }

    /** Identity face: the :1657 melter — "wUh"/"PMP"/"BCB" with 'U' = the port smeltery_ceramic item (the one registered small-crucible rung). */
    @Test
    public void theMelterRowCarriesTheCeramicCrucibleCore() throws Exception {
        JsonObject tRow = generated("melter");
        assertGrid(tRow, "wUh", "PMP", "BCB");
        assertEquals("gt6:smeltery_ceramic", tRow.getAsJsonObject("key").get("U").getAsJsonObject().get("item").getAsString(),
                "'U' = the upstream aRegistry.getItem(1005) Ceramic crucible → the port smeltery rung");
        assertEquals("gt6:melter", tRow.getAsJsonObject("result").get("item").getAsString(), "the result item");
    }

    /** Identity face: the :1400 T3 distillery — "GPG"/"WMW"/"hCw" with 'W' = wireGt08 Nichrome and 'G' = the vanilla glass bridge. */
    @Test
    public void theDistilleryRowsCarryThePerTierWireColumns() throws Exception {
        JsonObject tRow = generated("distillery_t3");
        assertGrid(tRow, "GPG", "WMW", "hCw");
        assertEquals("gt6:wire_nichrome_gt08", tRow.getAsJsonObject("key").get("W").getAsJsonObject().get("item").getAsString(),
                "T3 'W' = wireGt08.dat(MT.Nichrome)");
        assertEquals("minecraft:glass", tRow.getAsJsonObject("key").get("G").getAsJsonObject().get("item").getAsString(),
                "'G' = the vanilla glass (upstream OD.blockGlassColorless)");
        JsonObject tT4 = generated("distillery_t4");
        assertEquals("gt6:wire_carborundum_gt16", tT4.getAsJsonObject("key").get("W").getAsJsonObject().get("item").getAsString(),
                "T4 'W' = wireGt16.dat(MT.SiC) → the carborundum token (GTWireSpecs:76)");
    }

    /** Identity face: the :1610 T2 bumblelyzer — "WXW"/"ZMP"/"CYC" over the cable/emitter/sensor/circuit columns + the vanilla saplings tag. */
    @Test
    public void theBumblelyzerRowCarriesTheComponentColumns() throws Exception {
        JsonObject tRow = generated("bumblelyzer_t2");
        assertGrid(tRow, "WXW", "ZMP", "CYC");
        assertEquals("gt6:cable_copper_gt01", tRow.getAsJsonObject("key").get("W").getAsJsonObject().get("item").getAsString(),
                "'W' = the CABLES_01[2] column");
        assertEquals("gt6:signal_emitter_mv", tRow.getAsJsonObject("key").get("X").getAsJsonObject().get("item").getAsString(),
                "'X' = the EMITTERS[2] column (the GT6Emitters path face)");
        assertEquals("gt6:sensor_mv", tRow.getAsJsonObject("key").get("Y").getAsJsonObject().get("item").getAsString(), "'Y' = the SENSORS[2] column");
        assertEquals("minecraft:honey_bottle", tRow.getAsJsonObject("key").get("Z").getAsJsonObject().get("item").getAsString(),
                "'Z' = the honey container (upstream OD.container1000honey → the bumbliary-band vanilla fold; the plantalyzer 'Z' stays the saplings tag)");
        assertTrue(tRow.getAsJsonObject("key").get("C").getAsJsonObject().get("tag").getAsString().endsWith("circuit2"), "'C' = the OD_CIRCUITS[2] tag");
        assertEquals("gt6:bumblelyzer_t2", tRow.getAsJsonObject("result").get("item").getAsString(), "the result item");
    }

    /** Identity face: the :1499 T3 autocrafter — "WRW"/"RwR"/"CMC" with 'R' = the T3 compact robot arm (the IL.ROBOT_ARMS[3] column). */
    @Test
    public void theAutocrafterRowCarriesTheRobotArmColumn() throws Exception {
        JsonObject tRow = generated("autocrafter_t3");
        assertGrid(tRow, "WRW", "RwR", "CMC");
        assertEquals("gt6:compact_robot_arm_hv", tRow.getAsJsonObject("key").get("R").getAsJsonObject().get("item").getAsString(),
                "'R' = the IL.ROBOT_ARMS[3] column (the GT6Robotics ladder)");
        assertEquals("gt6:autocrafter_t3", tRow.getAsJsonObject("result").get("item").getAsString(), "the result item");
    }

    // ------------------------------------------------------------------
    // the helpers (the steam-band test shape)
    // ------------------------------------------------------------------

    private static void assertCraftingFile(String aPath) {
        assertNotNull(craftingFileOrNull(aPath), "the crafting row of " + aPath);
    }

    private static InputStream craftingFileOrNull(String aPath) {
        return GT6MachinesCraftingPourTest.class.getResourceAsStream("/data/gt6/recipes/" + aPath + ".json");
    }

    private static void assertGrid(JsonObject aRow, String... aRows) {
        assertEquals(aRows.length, aRow.getAsJsonArray("pattern").size(), "the pattern height");
        for (int i = 0; i < aRows.length; i++) {
            assertEquals(aRows[i], aRow.getAsJsonArray("pattern").get(i).getAsString(), "the grid row " + i + " verbatim");
        }
    }

    private static JsonObject generated(String aPath) throws Exception {
        try (InputStream tStream = craftingFileOrNull(aPath)) {
            assertNotNull(tStream, "/data/gt6/recipes/" + aPath + ".json rides the generated-resources classpath");
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
