/**
 * The negative-ledger ratchet (task item-pipes-loot-self): the item-pipe family loot face
 * ({@code GT6LootTables.GT6ItemPipeBlockLoot}, the tank-valve dropSelf lane) walks the
 * {@code GTItemPipes.ROWS} universe minus the gated-domain rows — loot tables have NO
 * load-time condition mechanism (ForgeHooks.loadLootTable deserializes straight into
 * LootTable) and registers() is useless at datagen (the datagen JVM seeds all-PRESENT) —
 * so the face skips the {@link GT6ForeignRowConvergence#gatingDomain} rows at datagen
 * instead (the parse-errors-registration-convergence loot shape; the declared cost being
 * that an install carrying the owning mod also misses the row). This class pins the
 * NEGATIVE side forever:
 * <ul>
 * <li>the row-level ledger: the 126-row universe (21 materials x 6 variants,
 *     Loader_MultiTileEntities.java:1823-1843) gates out EXACTLY the three PRIMARY-atlas
 *     slugs (enderium/TE, elementium/BOTA via ElvenElementium, angmallen/MET) x 6 variants
 *     = 18 rows — the other card-cited alloy slugs (manyullyn TiC, electrum TE, constantan
 *     IE, ...) are COMMON_SECONDARY atlas rows and NEVER gate (ADR-MDH2, only PRIMARY is
 *     clearable);</li>
 * <li>the committed-tree ratchet: no gated item-pipe loot JSON may exist in either
 *     directory band (1.20.1 {@code loot_tables/blocks} + the 1.21 {@code loot_table/blocks}
 *     twin), the family total shrinks to exactly 108, and the ungated control rows keep
 *     shipping.</li>
 * </ul>
 *
 * <p>Why row-decisions + tree, not the face itself: the family blocks live in
 * {@code RegistryObject}s that only bind under real registry events, so
 * {@code itemPipeLootBlocks()} cannot dereference in this headless JVM (the
 * GT6FamilyLootConvergencePinTest split verbatim). The face is one
 * {@code gatingDomain(...) != null} continue over the same ROWS this class counts; the
 * runData + datagen_tree_check gate closes the loop on the emitted tree. The
 * {@code longdist_item_pipe.json} table is the long-distance-pipes family, NOT this
 * matrix — the {@code contains("_item_pipe_")} counter below excludes it by shape.
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

import gregtech6.registry.GTItemPipes;
import gregtech6.registry.GTMaterialItems;

public class GT6ItemPipeLootSelfPinTest {

    /** The ledger: 21 loader material lines x 6 addItemPipes variants; the PRIMARY-atlas trio gates out. */
    private static final int UNIVERSE = 126, GATED = 18, SHIPPED = UNIVERSE - GATED;

    /** The three PRIMARY-atlas material slugs of the family (the whole gated set, slug-pinned). */
    private static final List<String> GATED_SLUGS = List.of("enderium", "elementium", "angmallen");

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

    /** The gated-domain predicate over one row (the loot face's skip, mirrored). */
    private static boolean gated(GTItemPipes.ItemPipeRow aRow) {
        return GT6ForeignRowConvergence.gatingDomain(aRow.material().oreDictMaterial()) != null;
    }

    // ---- the row-level ledger: the skip is exactly the PRIMARY-atlas trio x 6 variants ----

    @Test
    public void theFaceSkipsExactlyThePrimaryAtlasTrio() {
        assertEquals(UNIVERSE, GTItemPipes.ROWS.size(), "the item-pipe universe frozen at 21 materials x 6 variants");
        assertEquals(GATED, GTItemPipes.ROWS.stream().filter(GT6ItemPipeLootSelfPinTest::gated).count(),
                "the 3 PRIMARY-atlas slugs x 6 variants gate out");
        for (String tSlug : GATED_SLUGS) {
            List<GTItemPipes.ItemPipeRow> tRows = GTItemPipes.ROWS.stream()
                    .filter(tRow -> tRow.material().slug().equals(tSlug)).toList();
            assertEquals(6, tRows.size(), tSlug + " contributes all six variants");
            assertTrue(tRows.stream().allMatch(GT6ItemPipeLootSelfPinTest::gated), tSlug + " gates every variant");
        }
        // the card-cited COMMON_SECONDARY alloys never gate (ADR-MDH2) — the negative ledger's truth pins
        for (String tSlug : List.of("manyullyn", "electrum", "constantan", "sterling_silver", "rose_gold",
                "black_bronze", "aluminium_brass", "platinum", "osmiridium", "ultimet", "magnalium",
                "vibranium_silver", "cobalt_brass", "germanium", "arsenic_copper", "arsenic_bronze", "brass")) {
            assertTrue(GTItemPipes.ROWS.stream()
                    .filter(tRow -> tRow.material().slug().equals(tSlug))
                    .allMatch(tRow -> !gated(tRow)), tSlug + " is COMMON_SECONDARY/unattributed and never gates");
        }
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

    /** Every loot JSON in one band's blocks dir that belongs to THIS matrix (empty when the band is absent). */
    private static List<String> bandFiles(String aBand) throws IOException {
        Path tDir = mdkRoot().resolve("src/generated/resources/data/gt6").resolve(aBand).resolve("blocks");
        if (!Files.isDirectory(tDir)) return List.of();
        try (Stream<Path> tWalk = Files.walk(tDir)) {
            return tWalk.filter(p -> p.toString().endsWith(".json"))
                    .map(p -> p.getFileName().toString())
                    .filter(f -> f.contains("_item_pipe_")) // excludes the longdist_item_pipe single
                    .toList();
        }
    }

    @Test
    public void noGatedItemPipeLootJsonShipsInEitherBand() throws IOException {
        for (GTItemPipes.ItemPipeRow tRow : GTItemPipes.ROWS) {
            if (!gated(tRow)) continue;
            for (Path tFile : bands(tRow.path()))
                assertFalse(Files.exists(tFile), "the gated table must not ship in either band: " + tFile);
        }
    }

    @Test
    public void theFamilyTotalShrinksToExactlyTheLedger() throws IOException {
        for (String tBand : List.of("loot_tables", "loot_table")) {
            List<String> tFiles = bandFiles(tBand);
            if (tFiles.isEmpty() && "loot_table".equals(tBand)) {
                assertTrue(Files.isDirectory(mdkRoot().resolve("src/generated/resources/data/gt6/loot_tables/blocks")),
                        "the 1.20.1 band must exist");
                continue; // the singular band is optional per run leg (tree_check reconciles the twins)
            }
            assertEquals(SHIPPED, tFiles.size(), tBand + ": item-pipe tables");
        }
    }

    @Test
    public void representativeGatedRowsStayGoneWhileControlsShip() throws IOException {
        // one per gated material: absent in both bands
        for (String tTable : List.of("enderium_item_pipe_medium", "elementium_item_pipe_huge",
                "angmallen_item_pipe_small"))
            for (Path tFile : bands(tTable))
                assertFalse(Files.exists(tFile), "the gated table must not ship: " + tFile);
        // the ungated controls — GT core and the COMMON_SECONDARY alloys named by the research card — still shipping
        Path tBlocks = mdkRoot().resolve("src/generated/resources/data/gt6/loot_tables/blocks");
        for (String tTable : List.of("brass_item_pipe_medium", "manyullyn_item_pipe_medium",
                "electrum_item_pipe_large", "platinum_item_pipe_restrictive_huge"))
            assertTrue(Files.exists(tBlocks.resolve(tTable + ".json")), "the control table must ship: " + tTable);
    }
}
