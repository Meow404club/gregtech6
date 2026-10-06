/**
 * Offline tests for task debt-ore-gen-display: the shared layout seam of the ore-generation
 * distribution page — the census through the DISPLAY walk (acceptance 1: the 170-axis pin
 * re-derived here + the two named materials full-field), the line formats, and the
 * invisible-mounting walk.
 *
 * <p>The pins are HAND-RECOMPUTED from the data card (OreDistributionInfo, merged
 * 69c45ad66; the tables GTOreWorldgen.ROWS / LARGE_VEIN_TABLE / BEDROCK_ORE_TABLE):
 * <ul>
 * <li>Cassiterite — the triple-face material: small in ALL THREE dims (:804), the
 *     veinEnd vein row (:906, ow+end, w170 s24), the bedrock row (:756, 1/2000). Its
 *     block universe = 74 small-ore-axis paths + 2 bedrock forms;</li>
 * <li>Ferberite — bedrock-ONLY (:727, 1/96000): one face line, one dim, and the 2
 *     bedrock paths are its ENTIRE mounting (no small-ore axis membership);</li>
 * <li>the mounting totals: 125 small-ore-axis entries carry 74 paths each, the 45-material
 *     bedrock axis members carry +2, the union walk yields 76 for the axis-intersection
 *     materials (74+2).</li>
 * </ul>
 *
 * <p>Offline-safe: the walks are pure key arithmetic; only the ItemStack faces degrade to
 * EMPTY (registries unfired) — the stack faces are pinned at the path layer instead.
 */
package gregtech6.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregtech6.registry.GTMaterialItems;

//? if neoforge {
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
//?}

class GT6OreGenInfoLayoutTest {

    @BeforeAll
    static void boot() {
        GTMaterialItems.initMaterials();
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
        }
    }

    // ---------------------------------------------------------------- the census through the display walk

    /** The data card's axis pin, re-derived through the layout walk — the display row list IS entries(). */
    @Test
    void displayRowsAreThe170MaterialAggregates() {
        assertEquals(170, OreDistributionInfo.entries().size(), "125 small + 12 bedrock-only + 33 vein-only (r7-b pool + b2 boundary + batch2 anchor rows joined; Oilshale the one brand-new entry)");
        long tWithSmall = OreDistributionInfo.entries().stream().filter(e -> !e.smallOres().isEmpty()).count();
        long tWithVein = OreDistributionInfo.entries().stream().filter(e -> !e.veins().isEmpty()).count();
        long tWithBedrock = OreDistributionInfo.entries().stream().filter(e -> !e.bedrockOres().isEmpty()).count();
        assertEquals(125, tWithSmall, "small-ore materials");
        assertEquals(98, tWithVein, "large-vein materials");
        assertEquals(32, tWithBedrock, "overworld bedrock-ore materials");
        // every row renders at least one section (header + face line) — no blank pages in the category
        for (OreDistributionInfo.Entry tEntry : OreDistributionInfo.entries()) {
            assertTrue(GT6OreGenInfoLayout.rows(tEntry).size() >= 2, tEntry.material()::toString);
        }
    }

    /**
     * The sectioned structure (task oregen-info-relayout — the pre-relayout page was a flat
     * wall of same-style strings, the "一坨字糊一起" complaint): Cassiterite renders THREE
     * sections in the data layer's column order (small, vein, bedrock), each = header at
     * {@code TEXT_X} + members indented by {@code INDENT}, {@code LINE_HEIGHT} within a
     * block and {@code LINE_HEIGHT + SECTION_GAP} between blocks (the GTCEu drawUI rhythm,
     * OreVeinRecipeWidget.java:103-134).
     */
    @Test
    void cassiteriteRendersThreeSectionedBlocks() {
        OreDistributionInfo.Entry tCassiterite = OreDistributionInfo.of(MT.OREMATS.Cassiterite);
        List<GT6OreGenInfoLayout.Row> tRows = GT6OreGenInfoLayout.rows(tCassiterite);
        // review-seat rebase union: the atum small face (task atum-dim-adaptation) rides the
        // sectioned structure as the 4th small line — 10 rows, not the authored 9
        assertEquals(10, tRows.size(), "3 headers + 4 small (Atum rides the atum dim) + 2 vein + 1 bedrock");
        assertEquals(GT6OreGenInfoLayout.SECTION_SMALL_KEY, tRows.get(0).key());
        assertEquals(GT6OreGenInfoLayout.LINE_SMALL_KEY, tRows.get(1).key());
        assertEquals(GT6OreGenInfoLayout.LINE_SMALL_KEY, tRows.get(2).key());
        assertEquals(GT6OreGenInfoLayout.LINE_SMALL_KEY, tRows.get(3).key());
        assertEquals(GT6OreGenInfoLayout.LINE_SMALL_KEY, tRows.get(4).key());
        assertEquals(GT6OreGenInfoLayout.SECTION_VEIN_KEY, tRows.get(5).key());
        assertEquals(GT6OreGenInfoLayout.LINE_VEIN_KEY, tRows.get(6).key());
        assertEquals(GT6OreGenInfoLayout.LINE_VEIN_KEY, tRows.get(7).key());
        assertEquals(GT6OreGenInfoLayout.SECTION_BEDROCK_KEY, tRows.get(8).key());
        assertEquals(GT6OreGenInfoLayout.LINE_BEDROCK_KEY, tRows.get(9).key());
        // headers sit at TEXT_X, members one INDENT in — the sectioning is visible geometry, not string prefixes
        assertEquals(GT6OreGenInfoLayout.TEXT_X, tRows.get(0).x());
        assertEquals(GT6OreGenInfoLayout.TEXT_X + GT6OreGenInfoLayout.INDENT, tRows.get(1).x());
        assertEquals(GT6OreGenInfoLayout.TEXT_X + GT6OreGenInfoLayout.INDENT, tRows.get(9).x());
        // the y rhythm: 10px line steps, +SECTION_GAP before each following header
        assertEquals(GT6OreGenInfoLayout.FACE_BASE_Y, tRows.get(0).y());
        assertEquals(GT6OreGenInfoLayout.FACE_BASE_Y + 5 * GT6OreGenInfoLayout.LINE_HEIGHT + GT6OreGenInfoLayout.SECTION_GAP,
                tRows.get(5).y(), "the vein header clears the small block (header + 4 faces incl Atum) by a gap");
        assertEquals(tRows.get(5).y() + 3 * GT6OreGenInfoLayout.LINE_HEIGHT + GT6OreGenInfoLayout.SECTION_GAP,
                tRows.get(8).y(), "the bedrock header clears the vein block (header + 2 faces) by a gap");
        // every visible word rides a lang key — no literal display strings on the seam
        for (GT6OreGenInfoLayout.Row tRow : tRows) {
            assertTrue(tRow.key().startsWith(GT6OreGenInfoLayout.TITLE_KEY + "."), tRow.key());
        }
    }

    /** The 1.5x-口径 contract the display must NOT violate: the args are the UPSTREAM table values verbatim. */
    @Test
    void rowsCarryUpstreamSemanticsVerbatim() {
        OreDistributionInfo.Entry tCassiterite = OreDistributionInfo.of(MT.OREMATS.Cassiterite);
        List<GT6OreGenInfoLayout.Row> tRows = GT6OreGenInfoLayout.rows(tCassiterite);
        // small face args: [dim component, minY, maxY, amount] — Y 60-120, 16/chunk
        assertEquals(GT6OreGenInfoLayout.DIM_OVERWORLD_KEY,
                ((net.minecraft.network.chat.contents.TranslatableContents) ((net.minecraft.network.chat.Component) tRows.get(1).args().get(0)).getContents()).getKey());
        assertEquals(60, tRows.get(1).args().get(1));
        assertEquals(120, tRows.get(1).args().get(2));
        assertEquals(16, tRows.get(1).args().get(3));
        // vein face args: [dim, minY, maxY, weight, size] — w170 s24 (the old soup, now labeled);
        // the vein LINE sits at row 6 (the atum small line occupies row 4, review-seat rebase union)
        assertEquals(40, tRows.get(6).args().get(1));
        assertEquals(90, tRows.get(6).args().get(2));
        assertEquals(170, tRows.get(6).args().get(3));
        assertEquals(24, tRows.get(6).args().get(4));
        // the dims row: the four-dim union of translatable siblings (en proper nouns pinned by
        // key, zh rides the dump faces; Atum joined at task atum-dim-adaptation)
        List<net.minecraft.network.chat.Component> tSiblings = GT6OreGenInfoLayout.dimsRow(tCassiterite).getSiblings();
        assertEquals(7, tSiblings.size(), "dim, sep, dim, sep, dim, sep, atum");
        assertEquals(GT6OreGenInfoLayout.DIM_OVERWORLD_KEY, dimKey(tSiblings.get(0)));
        assertEquals(GT6OreGenInfoLayout.DIM_NETHER_KEY, dimKey(tSiblings.get(2)));
        assertEquals(GT6OreGenInfoLayout.DIM_END_KEY, dimKey(tSiblings.get(4)));
    }

    /** Ferberite: bedrock-only — one section (header + line), one dim, and the bedrock row's P verbatim. */
    @Test
    void ferberiteRendersOneBedrockLine() {
        OreDistributionInfo.Entry tFerberite = OreDistributionInfo.of(MT.OREMATS.Ferberite);
        assertNotNull(tFerberite);
        List<GT6OreGenInfoLayout.Row> tRows = GT6OreGenInfoLayout.rows(tFerberite);
        assertEquals(2, tRows.size(), "the bedrock header + the one face line");
        assertEquals(GT6OreGenInfoLayout.SECTION_BEDROCK_KEY, tRows.get(0).key());
        assertEquals(GT6OreGenInfoLayout.LINE_BEDROCK_KEY, tRows.get(1).key());
        assertEquals("ferberite", tRows.get(1).args().get(0), "the row-name suffix is DATA (the row id), not copy");
        assertEquals(96000, tRows.get(1).args().get(1), "1/96000 per chunk, the upstream P verbatim");
        List<net.minecraft.network.chat.Component> tDims = GT6OreGenInfoLayout.dimsRow(tFerberite).getSiblings();
        assertEquals(1, tDims.size(), "bedrock rows are overworld-only by construction");
        assertEquals(GT6OreGenInfoLayout.DIM_OVERWORLD_KEY, dimKey(tDims.get(0)));
    }

    /** The gold pair: TWO independent rolls, both rendered (the data card's double-roll pin, display face). */
    @Test
    void goldBedrockPairRendersBothRolls() {
        OreDistributionInfo.Entry tGold = OreDistributionInfo.of(MT.Au);
        List<GT6OreGenInfoLayout.Row> tRows = GT6OreGenInfoLayout.rows(tGold);
        assertEquals(2, tRows.stream().filter(tRow -> tRow.key().equals(GT6OreGenInfoLayout.LINE_BEDROCK_KEY)
                && String.valueOf(tRow.args().get(0)).startsWith("gold.")).count(),
                "gold.a + gold.b, both rolls kept");
        for (GT6OreGenInfoLayout.Row tRow : tRows) {
            if (tRow.key().equals(GT6OreGenInfoLayout.LINE_BEDROCK_KEY)
                    && String.valueOf(tRow.args().get(0)).startsWith("gold.")) {
                assertEquals(32000, tRow.args().get(1), "the independent 1/P roll, verbatim");
            }
        }
    }

    // ---------------------------------------------------------------- the invisible mounting walk

    /** Cassiterite's mounting = 74 small-ore-axis paths + 2 bedrock forms = 76 — with the registry contracts named. */
    @Test
    void cassiteriteMountingCoversEveryBlockItemVariant() {
        OreDistributionInfo.Entry tCassiterite = OreDistributionInfo.of(MT.OREMATS.Cassiterite);
        List<String> tPaths = GT6OreGenInfoLayout.variantPaths(tCassiterite);
        assertEquals(74 + 2, tPaths.size(), "26 families x forms (74) + the 2 bedrock forms");
        assertTrue(tPaths.contains("ore_stone_cassiterite"), "the stone NORMAL form (the representative)");
        assertTrue(tPaths.contains("ore_small_stone_cassiterite"), "the small form of the same family");
        assertTrue(tPaths.contains("ore_bedrock_cassiterite"), "the large bedrock form (:756 axis member)");
        assertTrue(tPaths.contains("ore_small_bedrock_cassiterite"), "the small bedrock form");
        // the representative: Cassiterite IS in the small-ore block axis -> the stone NORMAL path
        assertEquals("ore_stone_cassiterite", GT6OreGenInfoLayout.representativePath(tCassiterite));
    }

    /** Ferberite's mounting = the 2 bedrock paths ONLY — no small-ore axis membership, the representative falls to the large bedrock form. */
    @Test
    void ferberiteMountingIsTheBedrockPair() {
        OreDistributionInfo.Entry tFerberite = OreDistributionInfo.of(MT.OREMATS.Ferberite);
        assertEquals(List.of("ore_bedrock_ferberite", "ore_small_bedrock_ferberite"),
                GT6OreGenInfoLayout.variantPaths(tFerberite));
        assertEquals("ore_bedrock_ferberite", GT6OreGenInfoLayout.representativePath(tFerberite));
    }

    /**
     * The stack faces ride the leg's registry state: the 1.20.1-forge offline harness
     * never fires RegisterEvents, so every resolve degrades to EMPTY; the 1.21.1-neoforge
     * harness DOES fire them, so the walk resolves the FULL mounting — every stack's
     * registry id lands in the path walk (the end-to-end pin the forge leg can't run).
     */
    @Test
    void stackFacesMatchTheLegRegistryState() {
        OreDistributionInfo.Entry tCassiterite = OreDistributionInfo.of(MT.OREMATS.Cassiterite);
        //? if forge {
        assertTrue(GT6OreGenInfoLayout.variantStacks(tCassiterite).isEmpty(), "no registries offline");
        assertTrue(GT6OreGenInfoLayout.representative(tCassiterite).isEmpty(), "no registries offline");
        assertTrue(GT6OreGenInfoLayout.catalystStack().isEmpty(), "rockGt Stone unregistered offline");
        //?} else {
        /*java.util.List<ItemStack> tStacks = GT6OreGenInfoLayout.variantStacks(tCassiterite);
        assertEquals(GT6OreGenInfoLayout.variantPaths(tCassiterite).size(), tStacks.size(),
                "the mounting resolves in full when the registries are live");
        for (ItemStack tStack : tStacks) {
            assertTrue(GT6OreGenInfoLayout.variantPaths(tCassiterite)
                    .contains(BuiltInRegistries.ITEM.getKey(tStack.getItem()).getPath()),
                    tStack.getItem()::toString);
        }
        assertEquals("ore_stone_cassiterite",
                BuiltInRegistries.ITEM.getKey(GT6OreGenInfoLayout.representative(tCassiterite).getItem()).getPath(),
                "the representative resolves to the stone NORMAL form");
        assertFalse(GT6OreGenInfoLayout.catalystStack().isEmpty(), "the rockGt Stone pebble is registered");
         *///?}
    }

    // ---------------------------------------------------------------- geometry

    /** The category height is the WORST entry's — no row clips, and the constant set is the shared seam's. */
    @Test
    void categoryHeightCoversTheWorstEntry() {
        int tWorst = 0;
        for (OreDistributionInfo.Entry tEntry : OreDistributionInfo.entries()) {
            tWorst = Math.max(tWorst, GT6OreGenInfoLayout.height(tEntry));
        }
        assertEquals(tWorst, GT6OreGenInfoLayout.categoryHeight(), "category height = the max per-entry height");
        // the formula: Cassiterite's 10 rows (3 headers + 7 faces incl the atum small line,
        // review-seat rebase union), + line height + pad — the authored 9-row 140 grew by one
        // LINE_HEIGHT when the atum dim joined (task atum-dim-adaptation)
        List<GT6OreGenInfoLayout.Row> tRows = GT6OreGenInfoLayout.rows(OreDistributionInfo.of(MT.OREMATS.Cassiterite));
        assertEquals(tRows.get(tRows.size() - 1).y() + GT6OreGenInfoLayout.LINE_HEIGHT + 4,
                GT6OreGenInfoLayout.height(OreDistributionInfo.of(MT.OREMATS.Cassiterite)));
        assertEquals(146, GT6OreGenInfoLayout.height(OreDistributionInfo.of(MT.OREMATS.Cassiterite)),
                "38 base + 10 rows x 10 + 2 section gaps x 4 + 4 pad");
        // wide enough for the widest pinned en face line: "Overworld Y 40-90 · Weight 170 · Size 24" = 40 chars x ~6px/char
        assertTrue(GT6OreGenInfoLayout.WIDTH >= 250, "wide enough for the longest pinned line");
    }

    /** The translatable-key extraction for component pins (offline-safe: never resolves through Language). */
    private static String dimKey(net.minecraft.network.chat.Component aComponent) {
        assertTrue(aComponent.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents,
                "the dim rides the translatable seam");
        return ((net.minecraft.network.chat.contents.TranslatableContents) aComponent.getContents()).getKey();
    }
}
