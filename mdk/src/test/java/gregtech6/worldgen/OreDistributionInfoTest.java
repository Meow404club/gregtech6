/**
 * Tests for task debt-ore-gen-data: the per-material aggregation over the three worldgen
 * row tables. The pinned numbers are HAND-RECOMPUTED from the tables (GTOreWorldgen.ROWS
 * 54 rows, GT6WorldgenDatagen.LARGE_VEIN_TABLE 40, BEDROCK_ORE_TABLE 46) — the axis pin
 * decomposes the union so a miscount names its own band:
 *
 * <ul>
 * <li>small face 49 = 54 rows - 1 cinnabar duplicate (redcinnabar :828 + cinnabar :851
 * share MT.OREMATS.Cinnabar) - 3 dim-less rows (dolamide :843 / ambrosium :846 /
 * zanite :847, modded dims only) - 1 placement-gated row (ancientdebris :852);</li>
 * <li>bedrock face 32 = 33 overworld rows - 1 gold.a/b duplicate (both MT.Au);</li>
 * <li>vein face 98 = the distinct materials of the 33 generating rows (overworld || end;
 * the 7 offworld rows :917-925 excluded, their 14 row-unique materials among them);</li>
 * <li>union 118 = 49 + 15 bedrock-only + 54 vein-only.</li>
 * </ul>
 *
 * <p>DISPLAY口径 (the acceptance's required note): the asserted {@code amount} values are
 * the UPSTREAM per-chunk attempt semantics (WorldgenOresSmall.java:61) — this port
 * places at ORE_SIZE=4 whose vein mean is 1.5 blocks, so live density runs ~1.5x these
 * numbers (GTOreWorldgen.java:220). The display card must present them as upstream
 * semantics with that caveat. Bedrock {@code probability} is an independent 1/P roll
 * PER ROW (gold.a and gold.b are two rolls — both asserted kept).
 *
 * <p>Offline-safe by construction: initMaterials + the vanilla bootstrap bracket only.
 */
package gregtech6.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;
import gregtech6.registry.GTMaterialItems;

class OreDistributionInfoTest {

    @BeforeAll
    static void boot() {
        GTMaterialItems.initMaterials();
        // the GTOfflineRenderTestBase recipe: the version detect must precede bootStrap (run-order lottery)
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
        }
    }

    // ---------------------------------------------------------------- the axis pin

    /** THE entry-count pin — the union of the three generating faces, hand-decomposed above. */
    @Test
    void materialAxisIsPinned() {
        List<OreDistributionInfo.Entry> tEntries = OreDistributionInfo.entries();
        assertEquals(118, tEntries.size(), "49 small + 15 bedrock-only + 54 vein-only (the class javadoc decomposition)");
        assertEquals(49, tEntries.stream().filter(e -> !e.smallOres().isEmpty()).count(), "small-ore materials");
        assertEquals(98, tEntries.stream().filter(e -> !e.veins().isEmpty()).count(), "large-vein materials (generating rows only)");
        assertEquals(32, tEntries.stream().filter(e -> !e.bedrockOres().isEmpty()).count(), "overworld bedrock-ore materials");

        // first-appearance order: the first table row's material leads
        assertEquals(MT.Cu, tEntries.get(0).material());

        // ghosts — table materials with NO generating face in any vanilla dim — have no entry
        for (OreDictMaterial tGhost : List.of(
                MT.AncientDebris,                       // small :852 placement-gated + bedrock :764 offworld
                MT.Dolamide, MT.Ambrosium, MT.Zanite,   // dim-less small rows (+ dolamide vein/bedrock rows offworld)
                MT.Adamantine, MT.Desh, MT.Syrmorite, MT.Octine, // vein offworld rows only (+ offworld bedrock rows)
                MT.Cheese, MT.VoidQuartz)) {            // vein offworld row / bedrock offworld row only
            assertNull(OreDistributionInfo.of(tGhost), tGhost::toString);
        }
    }

    /** Full-table cross-audits: the face totals re-derive the source walks row by row. */
    @Test
    void faceTotalsCrossAuditTheTables() {
        List<OreDistributionInfo.Entry> tEntries = OreDistributionInfo.entries();
        // every placement pair lands on some entry (91 = overworld 38 + nether 20 + end 33)
        assertEquals(GTOreWorldgen.placementPairs().size(),
                tEntries.stream().mapToInt(e -> e.smallOres().size()).sum());
        // 121 vein faces: per generating row (distinct slots) x (its dims) — e.g. lignite 2, platinum 4x2, naquadah 1
        assertEquals(121, tEntries.stream().mapToInt(e -> e.veins().size()).sum());
        // all 33 overworld bedrock rows kept as faces (gold.a and gold.b BOTH)
        assertEquals(33, tEntries.stream().mapToInt(e -> e.bedrockOres().size()).sum());
    }

    // ---------------------------------------------------------------- the spot recomputes (>=4, all fields vs the tables)

    /** Cassiterite: small in ALL THREE dims (:804) + the veinEnd vein row :906 (ow+end) + bedrock :756. */
    @Test
    void cassiteriteFullTripleFaceRecomputed() {
        OreDistributionInfo.Entry tEntry = OreDistributionInfo.of(MT.OREMATS.Cassiterite);
        assertNotNull(tEntry);
        assertEquals(List.of(
                new OreDistributionInfo.SmallOre(GTOreWorldgen.Dim.OVERWORLD, "ore.small.cassiterite", 60, 120, 16),
                new OreDistributionInfo.SmallOre(GTOreWorldgen.Dim.NETHER,    "ore.small.cassiterite", 60, 120, 16),
                new OreDistributionInfo.SmallOre(GTOreWorldgen.Dim.END,       "ore.small.cassiterite", 60, 120, 16)),
                tEntry.smallOres());
        assertEquals(List.of(
                new OreDistributionInfo.Vein(GTOreWorldgen.Dim.OVERWORLD, "ore.large.cassiterite", 40, 90, 170, 24),
                new OreDistributionInfo.Vein(GTOreWorldgen.Dim.END,       "ore.large.cassiterite", 40, 90, 170, 24)),
                tEntry.veins());
        assertEquals(List.of(new OreDistributionInfo.BedrockOre("ore.bedrock.cassiterite", 2000)), tEntry.bedrockOres());
    }

    /** Cinnabar: TWO nether small rows (:828 redcinnabar + :851 cinnabar) + the redstone vein's spread slot :913. */
    @Test
    void cinnabarTwoNetherRowsRecomputed() {
        OreDistributionInfo.Entry tEntry = OreDistributionInfo.of(MT.OREMATS.Cinnabar);
        assertEquals(List.of(
                new OreDistributionInfo.SmallOre(GTOreWorldgen.Dim.OVERWORLD, "ore.small.redcinnabar", 5, 20, 4),
                new OreDistributionInfo.SmallOre(GTOreWorldgen.Dim.NETHER,    "ore.small.redcinnabar", 5, 20, 4),
                new OreDistributionInfo.SmallOre(GTOreWorldgen.Dim.NETHER,    "ore.small.cinnabar",   5, 250, 16)),
                tEntry.smallOres());
        assertEquals(List.of(
                new OreDistributionInfo.Vein(GTOreWorldgen.Dim.OVERWORLD, "ore.large.redstone", 10, 40, 60, 24)),
                tEntry.veins());
        assertTrue(tEntry.bedrockOres().isEmpty());
    }

    /** Fe2O3 (Hematite): small :815 (ow+END) + two overworld veins (iron :915 between, copper :916 bottom) — the offworld adamantium row :917 (:between) must NOT appear; the gold row :903 carries Arsenopyrite/Au, NOT Fe2O3. */
    @Test
    void hematiteThreeVeinsRecomputed() {
        OreDistributionInfo.Entry tEntry = OreDistributionInfo.of(MT.Fe2O3);
        assertEquals(List.of(
                new OreDistributionInfo.SmallOre(GTOreWorldgen.Dim.OVERWORLD, "ore.small.hematite", 40, 80, 24),
                new OreDistributionInfo.SmallOre(GTOreWorldgen.Dim.END,       "ore.small.hematite", 40, 80, 24)),
                tEntry.smallOres());
        assertEquals(List.of(
                new OreDistributionInfo.Vein(GTOreWorldgen.Dim.OVERWORLD, "ore.large.iron",   10, 40, 120, 24),
                new OreDistributionInfo.Vein(GTOreWorldgen.Dim.OVERWORLD, "ore.large.copper", 10, 30,  80, 24)),
                tEntry.veins());
        assertEquals(List.of(new OreDistributionInfo.BedrockOre("ore.bedrock.hematite", 4000)), tEntry.bedrockOres());
    }

    /** Ferberite: bedrock-ONLY (:727, P=96000) — no small row, no vein slot. */
    @Test
    void ferberiteBedrockOnlyRecomputed() {
        OreDistributionInfo.Entry tEntry = OreDistributionInfo.of(MT.OREMATS.Ferberite);
        assertNotNull(tEntry);
        assertTrue(tEntry.smallOres().isEmpty());
        assertTrue(tEntry.veins().isEmpty());
        assertEquals(List.of(new OreDistributionInfo.BedrockOre("ore.bedrock.ferberite", 96000)), tEntry.bedrockOres());
    }

    /** Gold: the bedrock pair is TWO independent rolls (:736/:737) — both rows kept; small is three-dim :813, vein spread slot :903. */
    @Test
    void goldBedrockDoubleRollPreserved() {
        OreDistributionInfo.Entry tEntry = OreDistributionInfo.of(MT.Au);
        assertEquals(List.of(
                new OreDistributionInfo.BedrockOre("ore.bedrock.gold.a", 32000),
                new OreDistributionInfo.BedrockOre("ore.bedrock.gold.b", 32000)),
                tEntry.bedrockOres());
        assertEquals(3, tEntry.smallOres().size(), "ow+nether+end :813");
        assertEquals(List.of(
                new OreDistributionInfo.SmallOre(GTOreWorldgen.Dim.OVERWORLD, "ore.small.gold", 20, 40, 4),
                new OreDistributionInfo.SmallOre(GTOreWorldgen.Dim.NETHER,    "ore.small.gold", 20, 40, 4),
                new OreDistributionInfo.SmallOre(GTOreWorldgen.Dim.END,       "ore.small.gold", 20, 40, 4)),
                tEntry.smallOres());
        assertEquals(List.of(new OreDistributionInfo.Vein(GTOreWorldgen.Dim.OVERWORLD, "ore.large.gold", 20, 30, 5, 16)), tEntry.veins());
    }

    /** Naquadah: end-only on both faces (:841 small / :918 vein — four Nq slots dedup to ONE vein face). */
    @Test
    void naquadahEndFacesDedupRecomputed() {
        OreDistributionInfo.Entry tEntry = OreDistributionInfo.of(MT.Nq);
        assertEquals(List.of(
                new OreDistributionInfo.SmallOre(GTOreWorldgen.Dim.END, "ore.small.naquadah", 10, 80, 6)),
                tEntry.smallOres());
        assertEquals(List.of(
                new OreDistributionInfo.Vein(GTOreWorldgen.Dim.END, "ore.large.naquadah", 10, 60, 10, 32)),
                tEntry.veins());
        assertTrue(tEntry.bedrockOres().isEmpty());
    }
}
