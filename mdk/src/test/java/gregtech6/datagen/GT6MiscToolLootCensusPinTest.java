/**
 * The misc-tool-block loot census pin (task misc-toolblocks-loot): the registered-but-
 * table-less census tail of the "Misc Tool Blocks"/small-tank pool, closed — and the
 * already-covered members pinned as the negative ledger so a future regression (a table
 * dropped, a registration landing without its face) goes red here.
 *
 * <p>The census (the card's first step, now structural): every block id the six candidate
 * registration classes ({@code GT6Mortars}/{@code GT6Grindstones}/{@code GT6SiftingTables}/
 * {@code GT6Cups}/{@code GT6Hoppers}/{@code GT6MeasuringPot}) register must have its
 * {@code gt6:blocks/<id>} table in BOTH committed bands. Pre-card gap (the verdict the card
 * closed): mortar=0, grindstone=0, porcelain_cup=0, measuring_pot=0. Already-covered
 * negative ledger: sifting_table (GT6SiftingTableBlockLoot), the hopper matrix
 * (GT6HopperBlockLoot, the driver-domain skip face — 90 tables over the 120-row universe,
 * the loot-unported-census shape), anvils (GT6AnvilBlockLoot), sap_bag/plant_pot (the 32xxx
 * domain provider, non-candidate pool members listed in the card report only).
 *
 * <p>Per-member upstream verdicts (each read in tmp/gt6-1.7.10, the provider javadoc is the
 * full chain): mortars/grindstone/cup ship the pure dropSelf shape (mortar = the MTE-default
 * self-drop with zero item NBT, Loader:2179-2183 + TileEntityBase04MultiTileEntities
 * .getDrops:166-171; grindstone/cup = the BE-less fallback face whose live content carry is
 * the port block getDrops override — a table-side copy could never fire where the override
 * does not). The Measuring Pot is the one content-carrying table ({@code copy_nbt} tank
 * + mode → {@code BlockEntityTag.*}, the behavior pin below forbids the dropSelf shape
 * for it forever).
 *
 * <p>Why tree files, not the face itself: the family blocks live in {@code RegistryObject}s
 * that only bind under real registry events, so the datagen-side lists cannot dereference in
 * this headless JVM (the GT6KitchenLootDropFacePinTest split verbatim; the census walks the
 * registration classes' row tables and DeferredRegister entries for the PATHS only). The
 * runData + datagen_tree_check gate closes the loop on the emitted tree.
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import gregtech6.registry.GT6Cups;
import gregtech6.registry.GT6Grindstones;
import gregtech6.registry.GT6MeasuringPot;
import gregtech6.registry.GT6Mortars;
import gregtech6.registry.GT6SiftingTables;
import gregtech6.registry.GTMaterialItems;

public class GT6MiscToolLootCensusPinTest {

    /** The pre-card gap, now shipped: the five mortar rows (GT6Mortars.ROWS order = Loader :2179-2183). */
    private static final List<String> MORTAR_SLUGS = List.of(
            "mortar_steel", "mortar_netherite", "mortar_sapphire", "mortar_diamond", "mortar_amethyst");

    /** The single-row gap members: the Grindstone (:2226) and the Porcelain Cup (:2094) — dropSelf shape. */
    private static final List<String> DROPSELF_SLUGS = List.of("grindstone", "porcelain_cup");

    /** The content-carrying row: the Ceramic Measuring Pot (:2096) — the tankSelfTable copy shape. */
    private static final String MEASURING_POT_SLUG = "measuring_pot";

    /** The negative-ledger single: the Sifting Table (:2227) — GT6SiftingTableBlockLoot's, not this card's provider. */
    private static final String SIFTING_TABLE_SLUG = "sifting_table";

    /** The committed band tree total (plural + singular twins): 17071 + the 8 new tables. */
    // 17079 -> 17199: +120 task storage-massstorage (the 120 mass-storage dropSelf loot rows, merge cedf692692) —
    // the card-2 gate domain missed this pin (it walked FamilyLootConvergence/StoneBlockCensus, not this tree walk);
    // review-seat 54 re-pin on the measured tree (the +120 rows are the only delta, shapeless/special faces moved by zero).
    private static final int TREE_RATCHET = 17199;

    /** The mdk project root, walking up from the (leg-dependent) test working dir (the convergence-test anchor). */
    private static Path mdkRoot() {
        for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent())
            if (Files.isRegularFile(p.resolve("tools").resolve("gen_textures.py"))) return p;
        throw new AssertionError("mdk root (tools/gen_textures.py) not found upward from "
                + Path.of("").toAbsolutePath());
    }

    /** The committed band dirs for the blocks-loot domain (1.20.1 plural + 1.21 singular twin). */
    private static List<Path> bands() {
        Path tData = mdkRoot().resolve("src/generated/resources/data/gt6");
        return List.of(tData.resolve("loot_tables").resolve("blocks"), tData.resolve("loot_table").resolve("blocks"));
    }

    @BeforeAll
    public static void initMaterialSystem() {
        SharedConstants.tryDetectVersion();
        try {
            Bootstrap.bootStrap();
        } catch (Throwable ignored) {
            // offline init noise; the registries are usable by now
        }
        GTMaterialItems.initMaterials();
    }

    /** The candidate classes' registered block paths — the census universe (headless: paths only, no .get()). */
    private static List<String> candidatePaths() {
        List<String> rPaths = new ArrayList<>(GT6Mortars.ROWS.stream().map(r -> r.path()).toList());
        rPaths.add(single(GT6Grindstones.BLOCKS)); // the :2226 row
        rPaths.add(single(GT6SiftingTables.BLOCKS)); // the :2227 row
        rPaths.add(single(GT6Cups.BLOCKS)); // the :2094 row
        rPaths.add(single(GT6MeasuringPot.BLOCKS)); // the :2096 row
        for (gregtech6.registry.GT6Hoppers.HopperRow tRow : gregtech6.registry.GT6Hoppers.ROWS)
            rPaths.add(tRow.path());
        return rPaths;
    }

    /** The lone registration of a single-row class (the DeferredRegister entries walk; paths only). */
    private static String single(net.minecraftforge.registries.DeferredRegister<net.minecraft.world.level.block.Block> aBlocks) {
        assertEquals(1, aBlocks.getEntries().size(), aBlocks + ": the single-row census class");
        return aBlocks.getEntries().iterator().next().getId().getPath();
    }

    /** The hopper tables the provider ships (the driver-domain skip face — the loot-unported-census shape). */
    private static List<String> hopperTablePaths() {
        return gregtech6.registry.GT6Hoppers.ROWS.stream()
                .filter(tRow -> tRow.material().driverDomain() == null)
                .map(tRow -> tRow.path()).toList();
    }

    // ---- the census: registered ids <-> shipped tables, both bands ----

    @Test
    public void everyCandidateRegistrationHasItsTable() throws IOException {
        for (Path tBand : bands()) {
            for (String tPath : candidatePaths()) {
                if (tPath.startsWith("hopper_") || tPath.startsWith("queue_hopper_")) continue; // gated face, own pin
                assertTrue(Files.isRegularFile(tBand.resolve(tPath + ".json")),
                        tBand.getFileName() + ": the census gap must ship its table: " + tPath);
            }
        }
    }

    @Test
    public void theHopperMatrixShipsItsGatedFace() throws IOException {
        List<String> tHopperPaths = hopperTablePaths();
        assertEquals(90, tHopperPaths.size(), "the shipped hopper face (120 rows minus the 30 driver-domain skips)");
        for (Path tBand : bands())
            for (String tPath : tHopperPaths)
                assertTrue(Files.isRegularFile(tBand.resolve(tPath + ".json")),
                        tBand.getFileName() + ": the covered hopper row must stay covered: " + tPath);
    }

    // ---- the tree ratchet: 17071 -> 17079 -> 17199, exactly, both bands ----

    @Test
    public void theTreeTotalIsExactlyTheRatchet() throws IOException {
        for (Path tBand : bands()) {
            try (Stream<Path> tWalk = Files.walk(tBand)) {
                long tTotal = tWalk.filter(p -> p.toString().endsWith(".json")).count();
                assertEquals(TREE_RATCHET, tTotal, tBand + ": the blocks-loot tree ratchet");
            }
        }
    }

    // ---- the behavior pins: dropSelf for the gap tail, the tank copy for the pot ----

    @Test
    public void everyGapDropSelfTableIsThePureSelfShape() throws IOException {
        Path tBand = bands().get(0);
        for (String tSlug : MORTAR_SLUGS) assertPureDropSelf(tBand, tSlug);
        for (String tSlug : DROPSELF_SLUGS) assertPureDropSelf(tBand, tSlug);
    }

    /** The kitchen shape-b pins (the GT6KitchenLootDropFacePinTest form verbatim). */
    private static void assertPureDropSelf(Path tBand, String aSlug) throws IOException {
        String tJson = Files.readString(tBand.resolve(aSlug + ".json"));
        assertTrue(tJson.contains("\"type\": \"minecraft:block\""), aSlug + ": a block table");
        assertTrue(tJson.contains("\"name\": \"gt6:" + aSlug + "\""), aSlug + ": drops its own item");
        assertTrue(tJson.contains("minecraft:survives_explosion"), aSlug + ": the explosion gate");
        assertTrue(tJson.contains("\"rolls\": 1.0") && tJson.contains("\"bonus_rolls\": 0.0"),
                aSlug + ": one roll");
        // the mortar verdict: the MTE-default self-drop carries no item NBT (the design rides
        // the row id); the grindstone/cup verdict: the table is the BE-less fallback where a
        // copy source is null — the live content carry is the port block getDrops override.
        assertFalse(tJson.contains("\"functions\""), aSlug + ": no loot functions");
        assertFalse(tJson.contains("copy_nbt") || tJson.contains("copy_custom_data"),
                aSlug + ": no NBT carry");
    }

    @Test
    public void theMeasuringPotTableCarriesItsTank() throws IOException {
        Path tBand = bands().get(0);
        String tJson = Files.readString(tBand.resolve(MEASURING_POT_SLUG + ".json"));
        assertTrue(tJson.contains("\"type\": \"minecraft:block\""), MEASURING_POT_SLUG + ": a block table");
        assertTrue(tJson.contains("\"name\": \"gt6:" + MEASURING_POT_SLUG + "\""),
                MEASURING_POT_SLUG + ": drops its own item");
        assertTrue(tJson.contains("minecraft:survives_explosion"), MEASURING_POT_SLUG + ": the explosion gate");
        // the :2096 verdict: upstream carries the tank into the dropped stack
        // (TileEntityBase08FluidContainer.writeItemNBT2:92-95) plus the capacity re-bind
        // (MultiTileEntityMeasuringPot.writeItemNBT2:61-63, NBT_MODE off the default) and
        // the port block has no getDrops override — the table IS the live face, so
        // dropSelf would lose content AND the re-bound capacity.
        assertTrue(tJson.contains("\"function\": \"minecraft:copy_nbt\"")
                || tJson.contains("minecraft:copy_custom_data"), MEASURING_POT_SLUG + ": the tank copy function");
        assertTrue(tJson.contains("\"source\": \"block_entity\""), MEASURING_POT_SLUG + ": the BE source");
        assertTrue(tJson.contains("\"source\": \"tank\"") && tJson.contains("\"target\": \"BlockEntityTag.tank\"")
                && tJson.contains("\"op\": \"replace\""),
                MEASURING_POT_SLUG + ": the tank -> BlockEntityTag.tank REPLACE op");
        // the mode verdict: the re-bind rides NBT_MODE (GT6MeasuringPotBlockEntity
        // .saveAdditional:158 off-default guard / load:151 restore) — the pot re-bound by
        // the empty-hand click keeps its capacity through break/place only if the key rides.
        assertTrue(tJson.contains("\"source\": \"mode\"") && tJson.contains("\"target\": \"BlockEntityTag.mode\"")
                && tJson.contains("\"op\": \"replace\""),
                MEASURING_POT_SLUG + ": the mode -> BlockEntityTag.mode REPLACE op");
    }
}
