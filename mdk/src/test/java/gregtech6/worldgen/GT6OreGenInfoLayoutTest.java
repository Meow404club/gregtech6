/**
 * Offline tests for task debt-ore-gen-display: the shared layout seam of the ore-generation
 * distribution page — the census through the DISPLAY walk (acceptance 1: the 118-axis pin
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
 * <li>the mounting totals: 49 small-ore-axis entries carry 74 paths each, the 45-material
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
    void displayRowsAreThe118MaterialAggregates() {
        assertEquals(118, OreDistributionInfo.entries().size(), "49 small + 15 bedrock-only + 54 vein-only");
        long tWithSmall = OreDistributionInfo.entries().stream().filter(e -> !e.smallOres().isEmpty()).count();
        long tWithVein = OreDistributionInfo.entries().stream().filter(e -> !e.veins().isEmpty()).count();
        long tWithBedrock = OreDistributionInfo.entries().stream().filter(e -> !e.bedrockOres().isEmpty()).count();
        assertEquals(49, tWithSmall, "small-ore materials");
        assertEquals(98, tWithVein, "large-vein materials");
        assertEquals(32, tWithBedrock, "overworld bedrock-ore materials");
        // every row renders at least one face line — no blank pages in the category
        for (OreDistributionInfo.Entry tEntry : OreDistributionInfo.entries()) {
            assertTrue(!GT6OreGenInfoLayout.faceLines(tEntry).isEmpty(), tEntry.material()::toString);
        }
    }

    /** The 1.5x-口径 contract the display must NOT violate: the seam never rewrites amounts. */
    @Test
    void faceLinesCarryUpstreamSemanticsVerbatim() {
        OreDistributionInfo.Entry tCassiterite = OreDistributionInfo.of(MT.OREMATS.Cassiterite);
        assertEquals(List.of(
                "Small Overworld Y 60-120 16/chunk",
                "Small Nether Y 60-120 16/chunk",
                "Small End Y 60-120 16/chunk",
                "Vein Overworld Y 40-90 w170 s24",
                "Vein End Y 40-90 w170 s24",
                "Bedrock cassiterite 1/2000/chunk"),
                GT6OreGenInfoLayout.faceLines(tCassiterite));
        assertEquals("Overworld, Nether, End", GT6OreGenInfoLayout.dimsLine(tCassiterite));
    }

    /** Ferberite: bedrock-only — one dim, one line, and the bedrock row's P verbatim. */
    @Test
    void ferberiteRendersOneBedrockLine() {
        OreDistributionInfo.Entry tFerberite = OreDistributionInfo.of(MT.OREMATS.Ferberite);
        assertNotNull(tFerberite);
        assertEquals(List.of("Bedrock ferberite 1/96000/chunk"), GT6OreGenInfoLayout.faceLines(tFerberite));
        assertEquals("Overworld", GT6OreGenInfoLayout.dimsLine(tFerberite));
    }

    /** The gold pair: TWO independent rolls, both rendered (the data card's double-roll pin, display face). */
    @Test
    void goldBedrockPairRendersBothRolls() {
        OreDistributionInfo.Entry tGold = OreDistributionInfo.of(MT.Au);
        List<String> tLines = GT6OreGenInfoLayout.faceLines(tGold);
        assertTrue(tLines.contains("Bedrock gold.a 1/32000/chunk"), () -> String.join(" | ", tLines));
        assertTrue(tLines.contains("Bedrock gold.b 1/32000/chunk"), () -> String.join(" | ", tLines));
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
        // the formula: face band base (38) + 4 pad + lines x 10 — Cassiterite's 6 lines pin it
        assertEquals(38 + 4 + 6 * 10, GT6OreGenInfoLayout.height(OreDistributionInfo.of(MT.OREMATS.Cassiterite)));
        assertTrue(GT6OreGenInfoLayout.WIDTH >= 200, "wide enough for the longest pinned line");
    }
}
