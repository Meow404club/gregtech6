/**
 * The negative-ledger ratchet (task loot-unported-census): the five family loot faces
 * (fluid pipes / metal chests / storage hoppers / ACT+CCT / charging lockers) walk their
 * mdh-6-gated registration universes, but loot tables have NO load-time condition
 * mechanism (ForgeHooks.loadLootTable deserializes straight into LootTable) and
 * registers() is useless at datagen (the datagen JVM seeds all-PRESENT) — so the faces
 * skip the {@code driverDomain() != null} rows at datagen instead (the
 * parse-errors-registration-convergence loot shape, the declared cost being that an
 * install carrying the owning mod also misses the row). This class pins the NEGATIVE
 * side forever:
 * <ul>
 * <li>the row-level ledger: exactly 91 + 30 + 30 + 30 + 15 = 196 rows gate out of the
 *     five faces (the charter arithmetic, closed against the family censuses
 *     GT6ModDriverFamilyGateTest DRUMS12/CELLS40/HOPPER120/PIPE280/TABLE120);</li>
 * <li>the committed-tree ratchet: no gated family loot JSON may exist in either
 *     directory band (1.20.1 {@code loot_tables/blocks} + the 1.21 {@code loot_table/blocks}
 *     twin), the family totals shrink by exactly those counts, and the ungated control
 *     rows keep shipping.</li>
 * </ul>
 *
 * <p>Why row-decisions + tree, not the faces themselves: the family blocks live in
 * {@code RegistryObject}s that only bind under real registry events, so
 * {@code pipeLootBlocks()} &amp; co cannot dereference in this headless JVM (the
 * GT6BatteryBoxLootDatagenTest split verbatim). The faces are one
 * {@code driverDomain() != null} continue each over the same ROWS this class counts; the
 * runData + datagen_tree_check gate closes the loop on the emitted tree.
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import gregtech6.registry.GT6ChargingLockers;
import gregtech6.registry.GT6Chests;
import gregtech6.registry.GT6DriverTestSupport;
import gregtech6.registry.GT6Hoppers;
import gregtech6.registry.GTFluidPipes;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTMachines;

public class GT6FamilyLootConvergencePinTest {

    /** The charter ledger: pipes 13 slugs x 7 variants, the HopperMaterial/CraftingTableMaterial 15-slug pairs, the locker singleton. */
    private static final int PIPES = 91, CHESTS = 30, HOPPERS = 30, TABLES = 30, LOCKERS = 15;

    @BeforeAll
    public static void initMaterialSystem() {
        SharedConstants.tryDetectVersion();
        try {
            Bootstrap.bootStrap();
        } catch (Throwable ignored) {
            // offline init noise; the registries are usable by now
        }
        GTMaterialItems.initMaterials();
        GT6DriverTestSupport.loadFamiliesPristine(); // freeze the family lists pristine BEFORE any count
    }

    // ---- the row-level ledger: the five skip counts are exactly the charter buckets ----

    @Test
    public void theFiveFacesSkipExactlyTheCharterBuckets() {
        assertEquals(280, GTFluidPipes.ROWS.size(), "the pipe universe frozen at the full 280");
        assertEquals(PIPES, GTFluidPipes.ROWS.stream()
                .filter(tRow -> tRow.material().driverDomain() != null).count(), "the 13 foreign slugs x 7 variants");
        assertEquals(120, GT6Chests.ROWS.size(), "the chest universe frozen at the full 120");
        assertEquals(CHESTS, GT6Chests.ROWS.stream()
                .filter(tRow -> tRow.material().driverDomain() != null).count(), "15 slugs x chest+reinforced");
        assertEquals(120, GT6Hoppers.ROWS.size(), "the hopper universe frozen at the full 120");
        assertEquals(HOPPERS, GT6Hoppers.ROWS.stream()
                .filter(tRow -> tRow.material().driverDomain() != null).count(), "15 slugs x hopper+queue_hopper");
        assertEquals(120, GTMachines.CRAFTING_TABLE_ROWS.size(), "the crafting-table universe frozen at the full 120");
        assertEquals(TABLES, GTMachines.CRAFTING_TABLE_ROWS.stream()
                .filter(tRow -> tRow.material().driverDomain() != null).count(), "15 slugs x advanced+charging");
        assertEquals(60, GT6ChargingLockers.ROWS.size(), "the locker universe frozen at the full 60");
        assertEquals(LOCKERS, GT6ChargingLockers.ROWS.stream()
                .filter(tRow -> tRow.material().driverDomain() != null).count(), "15 slugs x one locker");
        assertEquals(196, PIPES + CHESTS + HOPPERS + TABLES + LOCKERS, "the closed negative ledger");
    }

    // ---- the committed-tree ratchet: the gated rows ship NO loot JSON in either band ----

    /** The mdk project root, walking up from the (leg-dependent) test working dir (the convergence-test anchor). */
    private static Path mdkRoot() {
        for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent())
            if (Files.isRegularFile(p.resolve("tools").resolve("gen_textures.py"))) return p;
        throw new AssertionError("mdk root (tools/gen_textures.py) not found upward from "
                + Path.of("").toAbsolutePath());
    }

    /** The two committed bands for one table name: the 1.20.1 plural + the 1.21 singular block-loot dirs. */
    private static List<Path> bands(String aTable) {
        Path tRoot = mdkRoot().resolve("src/generated/resources/data/gt6");
        return List.of(tRoot.resolve("loot_tables").resolve("blocks").resolve(aTable + ".json"),
                tRoot.resolve("loot_table").resolve("blocks").resolve(aTable + ".json"));
    }

    /** Every loot JSON in one band's blocks dir (empty when the band is absent). */
    private static List<String> bandFiles(String aBand) throws IOException {
        Path tDir = mdkRoot().resolve("src/generated/resources/data/gt6").resolve(aBand).resolve("blocks");
        if (!Files.isDirectory(tDir)) return List.of();
        try (Stream<Path> tWalk = Files.walk(tDir)) {
            return tWalk.filter(p -> p.toString().endsWith(".json")).map(p -> p.getFileName().toString()).toList();
        }
    }

    /** The gated table names of one family bucket (the exact files the faces stop emitting). */
    private static List<String> gatedTables(String aFamily) {
        List<String> rNames = new java.util.ArrayList<>();
        if ("pipe".equals(aFamily)) {
            for (var tRow : GTFluidPipes.ROWS)
                if (tRow.material().driverDomain() != null) rNames.add(tRow.path());
        } else if ("chest".equals(aFamily)) {
            for (var tRow : GT6Chests.ROWS)
                if (tRow.material().driverDomain() != null) rNames.add(tRow.path());
        } else if ("hopper".equals(aFamily)) {
            for (var tRow : GT6Hoppers.ROWS)
                if (tRow.material().driverDomain() != null) rNames.add(tRow.path());
        } else if ("table".equals(aFamily)) {
            for (var tRow : GTMachines.CRAFTING_TABLE_ROWS)
                if (tRow.material().driverDomain() != null) rNames.add(tRow.path());
        } else {
            for (var tRow : GT6ChargingLockers.ROWS)
                if (tRow.material().driverDomain() != null) rNames.add(tRow.path());
        }
        return rNames;
    }

    @Test
    public void noGatedFamilyLootJsonShipsInEitherBand() throws IOException {
        int tGated = 0;
        for (String tFamily : List.of("pipe", "chest", "hopper", "table", "locker"))
            for (String tTable : gatedTables(tFamily)) {
                for (Path tFile : bands(tTable))
                    assertFalse(Files.exists(tFile), "the gated table must not ship in either band: " + tFile);
                tGated++;
            }
        assertEquals(196, tGated, "the ratchet walks exactly the closed ledger");
    }

    @Test
    public void theFamilyTotalsShrinkByExactlyTheLedger() throws IOException {
        // per band: family files present = universe total - the gated count
        for (String tBand : List.of("loot_tables", "loot_table")) {
            List<String> tFiles = bandFiles(tBand);
            if (tFiles.isEmpty() && "loot_table".equals(tBand)) {
                assertTrue(Files.isDirectory(mdkRoot().resolve("src/generated/resources/data/gt6/loot_tables/blocks")),
                        "the 1.20.1 band must exist");
                continue; // the singular band is optional per run leg (tree_check reconciles the twins)
            }
            assertEquals(280 - PIPES, tFiles.stream().filter(f -> f.contains("_fluid_pipe_")).count(),
                    tBand + ": pipe tables");
            assertEquals(120 - CHESTS, tFiles.stream().filter(f -> f.startsWith("chest_") || f.startsWith("reinforced_chest_")).count(),
                    tBand + ": chest tables");
            assertEquals(120 - HOPPERS, tFiles.stream().filter(f -> f.startsWith("hopper_") || f.startsWith("queue_hopper_")).count(),
                    tBand + ": hopper tables");
            assertEquals(120 - TABLES, tFiles.stream().filter(f -> f.contains("_crafting_table_")).count(),
                    tBand + ": crafting-table tables");
            assertEquals(60 - LOCKERS, tFiles.stream().filter(f -> f.startsWith("charging_locker_")).count(),
                    tBand + ": locker tables");
        }
    }

    @Test
    public void representativeForeignRowsStayGoneWhileControlsShip() throws IOException {
        // the charter evidence row + one per family: absent in both bands
        for (String tTable : List.of("aluminium_fluid_pipe_small", "infinity_fluid_pipe_nonuple",
                "chest_manasteel", "reinforced_chest_terrasteel",
                "hopper_knightmetal", "queue_hopper_lumium",
                "advanced_crafting_table_void_metal", "charging_crafting_table_desh_alloy",
                "charging_locker_awakened_draconium"))
            for (Path tFile : bands(tTable))
                assertFalse(Files.exists(tFile), "the gated table must not ship: " + tFile);
        // the ungated controls: same families, GT core materials — still shipping in the plural band
        Path tBlocks = mdkRoot().resolve("src/generated/resources/data/gt6/loot_tables/blocks");
        for (String tTable : List.of("wood_fluid_pipe_small", "steel_fluid_pipe_small",
                "chest_steel", "reinforced_chest_steel",
                "hopper_lead", "queue_hopper_lead",
                "advanced_crafting_table_steel", "charging_crafting_table_steel",
                "charging_locker_steel"))
            assertTrue(Files.exists(tBlocks.resolve(tTable + ".json")), "the control table must ship: " + tTable);
    }
}
