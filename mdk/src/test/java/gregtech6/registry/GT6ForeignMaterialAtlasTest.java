/**
 * Tests for the foreign-material attribution table (mdh-2 batch 1 + mdh-atlas-batch2 batch 2).
 *
 * <p>Pins, in acceptance order: the append-only ratchet (batch 1 is a verbatim 164-row
 * prefix — eight domains, exact counts, exact seedable modid set, all mdh-2 numbers
 * untouched), the batch-2 census (56 domains against the upstream MT.java:2060-2721 re-read
 * account, with the one deferral — NikolineAlloy — accounted), the
 * secondary-attribution census with the never-hide contract (SPEC pairs present as
 * COMMON_SECONDARY, no batch-2 domain seedable, CS rows answer no domain and survive an
 * ABSENT pin of their own domain), the three-kind contract of ADR-MDH2 with the port's
 * carried attribution strings ({@code setOriginalMod}, MT.java:3099+) cross-checking every
 * row's domain, and table integrity (unique material per domain, domains ⊆ MD.MD ids).
 *
 * <p>The offline JVM has no FML and the neo junit-fml JVM is kept out of seeding by the
 * GT6ModDrivers test-JVM guard, so every ABSENT state here is explicit seam injection.
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregtech6.registry.GT6ForeignMaterialAtlas.AttributionKind;
import gregtech6.registry.GT6ForeignMaterialAtlas.Row;

public class GT6ForeignMaterialAtlasTest {

    /** The mdh-2 batch-1 ratchet: the table's first 164 rows are the batch-1 transcription, never edited. */
    private static final int BATCH1_ROWS = 164;
    /** 164 + 276: batch 2 appends 56 domains / 276 rows (1 upstream row deferred — see batchTwoCensusPins). */
    private static final int TOTAL_ROWS = 440;

    @BeforeAll
    public static void initMaterialSystem() {
        GTMaterialItems.initMaterials();
    }

    @AfterEach
    public void restorePristineDriver() {
        GT6ModDrivers.reset();
    }

    private static OreDictMaterial material(Row aRow) {
        OreDictMaterial rMaterial = aRow.material().get();
        assertNotNull(rMaterial, "atlas material must resolve: " + aRow);
        return rMaterial;
    }

    private static long countRegistered(String aInternalName) {
        return GTMaterialItems.registrationOrder().stream().filter(tPair -> tPair.material().mNameInternal.equals(aInternalName)).count();
    }

    private static Map<String, Long> kindCensus(AttributionKind aKind) {
        return GT6ForeignMaterialAtlas.rows().stream().filter(tRow -> tRow.kind() == aKind)
                .collect(java.util.stream.Collectors.groupingBy(Row::domain, java.util.stream.Collectors.counting()));
    }

    @Test
    public void batchOneCensusPins() {
        List<Row> tRows = GT6ForeignMaterialAtlas.rows();
        // The append-only ratchet: batch 1 is still exactly the mdh-2 prefix, unedited.
        assertEquals(TOTAL_ROWS, tRows.size(), "total table: batch-1 164 + batch-2 276");
        assertEquals(BATCH1_ROWS, countKind(tRows.subList(0, BATCH1_ROWS), null), "batch-1 prefix length");

        // Per-domain totals, upstream re-read order (each domain's block range in the atlas javadoc).
        assertEquals(19, tRows.stream().filter(tRow -> tRow.domain().equals(MT.MD.HaC.mID)).count(), "harvestcraft :2089-2107");
        assertEquals(17, tRows.stream().filter(tRow -> tRow.domain().equals(MT.MD.IC2.mID)).count(), "IC2 :2207-2223");
        assertEquals(16, tRows.stream().filter(tRow -> tRow.domain().equals(MT.MD.TE.mID)).count(), "ThermalExpansion :2318-2333");
        assertEquals(16, tRows.stream().filter(tRow -> tRow.domain().equals(MT.MD.EIO.mID)).count(), "EnderIO :2361-2376");
        assertEquals(18, tRows.stream().filter(tRow -> tRow.domain().equals(MT.MD.HBM.mID)).count(), "hbm :2391-2408");
        assertEquals(17, tRows.stream().filter(tRow -> tRow.domain().equals(MT.MD.BOTA.mID)).count(), "Botania :2455-2471");
        assertEquals(27, tRows.stream().filter(tRow -> tRow.domain().equals(MT.MD.GC_EXTRAPLANETS.mID)).count(), "ExtraPlanets :2566-2592");
        assertEquals(34, tRows.stream().filter(tRow -> tRow.domain().equals(MT.MD.MET.mID)).count(), "Metallurgy :2683-2716");

        // Three-value account of the batch-1 prefix: 137 PRIMARY / 23 COMMON_SECONDARY / 4 GT6_SELF.
        assertEquals(137, countKind(tRows.subList(0, BATCH1_ROWS), AttributionKind.PRIMARY), "batch-1 PRIMARY total");
        assertEquals(23, countKind(tRows.subList(0, BATCH1_ROWS), AttributionKind.COMMON_SECONDARY), "batch-1 COMMON_SECONDARY total");
        assertEquals(4, countKind(tRows.subList(0, BATCH1_ROWS), AttributionKind.GT6_SELF), "batch-1 GT6_SELF total: the theum quartet only");

        // The seed walks the PRIMARY-bearing domains of the WHOLE table (52: the batch-1 eight
        // first in row order, then the batch-2 PRIMARY domains in row order) — the activation
        // derivation, table-driven with the GT5U "gregtech"-modid rows excluded (mdh-clearout-batch2).
        List<String> tSeed = GT6ForeignMaterialAtlas.seedableDomains();
        assertEquals(MT.MD.HaC.mID, tSeed.get(0), "the batch-1 row order is the seed prefix");
        assertEquals(8, (int) tSeed.stream().filter(tDomain -> List.of(MT.MD.HaC.mID, MT.MD.IC2.mID,
                MT.MD.TE.mID, MT.MD.EIO.mID, MT.MD.HBM.mID, MT.MD.BOTA.mID, MT.MD.GC_EXTRAPLANETS.mID, MT.MD.MET.mID)
                .contains(tDomain)).count(), "all eight batch-1 domains seed");
        assertEquals(52, tSeed.size(), "the seed = 8 batch-1 + 44 batch-2 PRIMARY domains (56 batch-2 domains minus the 12 CS-only ones)");
        assertFalse(tSeed.contains(MT.MD.GT5U.mID), "our own modid (gregtech) never seeds");
        assertEquals(new HashSet<>(tSeed).size(), tSeed.size(), "no duplicate domains in the seed walk");
    }

    @Test
    public void batchTwoCensusPins() {
        List<Row> tRows = GT6ForeignMaterialAtlas.rows();
        List<Row> tBatch2 = tRows.subList(BATCH1_ROWS, tRows.size());
        assertEquals(276, tBatch2.size(), "batch-2 total: 56 domains, 1 upstream row deferred (NikolineAlloy)");

        // Per-domain totals against the upstream MT.java re-read account (javadoc batch list).
        assertEquals(7, countDomain(tBatch2, MT.MD.EtFu.mID), "etfuturum :2080-2086");
        assertEquals(1, countDomain(tBatch2, MT.MD.Salt.mID), "SaltMod :2110");
        assertEquals(1, countDomain(tBatch2, MT.MD.GrC.mID), "Growthcraft :2113");
        assertEquals(3, countDomain(tBatch2, MT.MD.NePl.mID), "netheriteplus :2116-2118 — AncientDebris is COMMON_SECONDARY per its :2118 COMMON_ORE flag (sibling of Netherite :2116)");
        assertEquals(6, countDomain(tBatch2, MT.MD.NeLi.mID), "netherlicious :2121-2126");
        assertEquals(3, countDomain(tBatch2, MT.MD.EnLi.mID), "enderlicious :2129-2131");
        assertEquals(4, countDomain(tBatch2, MT.MD.GT5U.mID), "GT5U :2195-2198 (modid gregtech = own, never seedable)");
        assertEquals(9, countDomain(tBatch2, MT.MD.IHL.mID), "ihl :2226-2234");
        assertEquals(2, countDomain(tBatch2, MT.MD.BC.mID), "BuildCraft :2237-2238");
        assertEquals(15, countDomain(tBatch2, MT.MD.FR.mID), "Forestry :2241-2255");
        assertEquals(3, countDomain(tBatch2, MT.MD.FRMB.mID), "MagicBees :2258-2260");
        assertEquals(2, countDomain(tBatch2, MT.MD.BINNIE.mID), "BinnieCore :2263-2264");
        assertEquals(13, countDomain(tBatch2, MT.MD.TFC.mID), "terrafirmacraft :2267-2279");
        assertEquals(14, countDomain(tBatch2, MT.MD.TF.mID), "TwilightForest :2282-2295");
        assertEquals(4, countDomain(tBatch2, MT.MD.ERE.mID), "erebus :2298-2301");
        assertEquals(8, countDomain(tBatch2, MT.MD.RC.mID), "Railcraft :2304-2311");
        assertEquals(2, countDomain(tBatch2, MT.MD.IE.mID), "ImmersiveEngineering :2314-2315");
        assertEquals(5, countDomain(tBatch2, MT.MD.AE.mID), "appliedenergistics2 :2336-2340");
        assertEquals(1, countDomain(tBatch2, MT.MD.PnC.mID), "PneumaticCraft :2343");
        assertEquals(2, countDomain(tBatch2, MT.MD.SC2.mID), "steamcraft2 :2346-2347");
        assertEquals(6, countDomain(tBatch2, MT.MD.TiC.mID), "TConstruct :2350-2355");
        assertEquals(1, countDomain(tBatch2, MT.MD.AA.mID), "ActuallyAdditions :2358");
        assertEquals(3, countDomain(tBatch2, MT.MD.MFR.mID), "MineFactoryReloaded :2379-2381");
        assertEquals(5, countDomain(tBatch2, MT.MD.BR.mID), "BigReactors :2384-2388");
        assertEquals(2, countDomain(tBatch2, MT.MD.ReC.mID), "ReactorCraft :2411-2412");
        assertEquals(15, countDomain(tBatch2, MT.MD.RoC.mID), "RotaryCraft :2415-2431 — upstream 17 statements, Prismane/Lonsdaleite duplicated :2423/:2424");
        assertEquals(5, countDomain(tBatch2, MT.MD.Mek.mID), "Mekanism :2434-2438");
        assertEquals(9, countDomain(tBatch2, MT.MD.TC.mID), "Thaumcraft :2441-2449");
        assertEquals(1, countDomain(tBatch2, MT.MD.TCTE.mID), "ThaumcraftExtras :2452");
        assertEquals(5, countDomain(tBatch2, MT.MD.ALF.mID), "alfheim :2474-2478");
        assertEquals(4, countDomain(tBatch2, MT.MD.CANDY.mID), "candycraftmod :2481-2484");
        assertEquals(2, countDomain(tBatch2, MT.MD.GC_ADV_ROCKETRY.mID), "advancedRocketry :2487-2488");
        assertEquals(2, countDomain(tBatch2, MT.MD.HEE.mID), "HardcoreEnderExpansion :2491-2492");
        assertEquals(6, countDomain(tBatch2, MT.MD.MaCu.mID), "Mariculture :2495-2500");
        assertEquals(4, countDomain(tBatch2, MT.MD.ABYSSAL.mID), "abyssalcraft :2503-2506");
        assertEquals(1, countDomain(tBatch2, MT.MD.Fossil.mID), "fossil :2509");
        assertEquals(2, countDomain(tBatch2, MT.MD.DE.mID), "DraconicEvolution :2512-2513");
        assertEquals(3, countDomain(tBatch2, MT.MD.AV.mID), "Avaritia :2516-2518");
        assertEquals(2, countDomain(tBatch2, MT.MD.PE.mID), "ProjectE :2521-2522");
        assertEquals(4, countDomain(tBatch2, MT.MD.TROPIC.mID), "tropicraft :2525-2528");
        assertEquals(4, countDomain(tBatch2, MT.MD.BoP.mID), "BiomesOPlenty :2531-2534");
        assertEquals(5, countDomain(tBatch2, MT.MD.FM.mID), "meteors :2537-2541");
        assertEquals(8, countDomain(tBatch2, MT.MD.ARS.mID), "arsmagica2 :2544-2551");
        assertEquals(10, countDomain(tBatch2, MT.MD.GC.mID), "GalacticraftCore :2554-2563");
        assertEquals(8, countDomain(tBatch2, MT.MD.GC_GALAXYSPACE.mID), "GalaxySpace :2595-2602");
        assertEquals(4, countDomain(tBatch2, MT.MD.MO.mID), "mo :2605-2608");
        assertEquals(2, countDomain(tBatch2, MT.MD.RT.mID), "RandomThings :2611-2612");
        assertEquals(2, countDomain(tBatch2, MT.MD.ExU.mID), "ExtraUtilities :2615-2616");
        assertEquals(13, countDomain(tBatch2, MT.MD.BTL.mID), "thebetweenlands :2619-2631");
        assertEquals(7, countDomain(tBatch2, MT.MD.AETHER.mID), "aether :2634-2640");
        assertEquals(13, countDomain(tBatch2, MT.MD.RP.mID), "Redpower :2643-2656 — upstream 14, :2655 NikolineAlloy deferred (GTFluids:2277 ratchet), :2657 EnergiumCyan has no put()");
        assertEquals(1, countDomain(tBatch2, MT.MD.PR.mID), "ProjRed|Core :2660");
        assertEquals(1, countDomain(tBatch2, MT.MD.BP.mID), "bluepower :2663");
        assertEquals(5, countDomain(tBatch2, MT.MD.FZ.mID), "factorization :2666-2670");
        assertEquals(5, countDomain(tBatch2, MT.MD.PFAA.mID), "PFAAGeologica :2673-2677");
        assertEquals(1, countDomain(tBatch2, MT.MD.UB.mID), "UndergroundBiomes :2680");

        // Batch-2 three-value account: 144 PRIMARY / 132 COMMON_SECONDARY / 0 GT6_SELF.
        assertEquals(144, countKind(tBatch2, AttributionKind.PRIMARY), "batch-2 PRIMARY total");
        assertEquals(132, countKind(tBatch2, AttributionKind.COMMON_SECONDARY), "batch-2 COMMON_SECONDARY total");
        assertEquals(0, countKind(tBatch2, AttributionKind.GT6_SELF), "batch-2 GT6_SELF total: no theum-shaped reversal found (review account in atlas javadoc)");
    }

    @Test
    public void batchTwoSecondaryCensusAndNeverHideContract() {
        List<Row> tBatch2 = GT6ForeignMaterialAtlas.rows().subList(BATCH1_ROWS, TOTAL_ROWS);

        // The SPEC secondary pairs are all present as COMMON_SECONDARY rows.
        Map<String, String> tPairs = Map.of(
                MT.MD.EtFu.mID, "Cu", MT.MD.RP.mID, "W", MT.MD.TC.mID, "Hg", MT.MD.AE.mID, "Si",
                MT.MD.Mek.mID, "Ge", MT.MD.FZ.mID, "Pb", MT.MD.TFC.mID, "Bi", MT.MD.MaCu.mID, "Ti", MT.MD.RC.mID, "Steel");
        for (Map.Entry<String, String> tPair : tPairs.entrySet()) {
            Row tRow = tBatch2.stream().filter(t -> t.domain().equals(tPair.getKey())
                    && t.kind() == AttributionKind.COMMON_SECONDARY
                    && material(t) == fieldMaterial(tPair.getValue())).findFirst().orElse(null);
            assertNotNull(tRow, "SPEC secondary pair must be COMMON_SECONDARY: " + tPair.getValue() + " → " + tPair.getKey());
        }

        // The activation form (mdh-clearout-batch2): every batch-2 domain with a PRIMARY row
        // seeds; the CS-only domains have nothing to hide and stay out.
        Set<String> tSeed = new HashSet<>(GT6ForeignMaterialAtlas.seedableDomains());
        Set<String> tBatch2PrimaryDomains = new HashSet<>();
        for (Row tRow : tBatch2) if (tRow.kind() == AttributionKind.PRIMARY) tBatch2PrimaryDomains.add(tRow.domain());
        for (Row tRow : tBatch2) {
            if (tRow.kind() != AttributionKind.PRIMARY) continue;
            if (tRow.domain().equals(MT.MD.GT5U.mID)) continue; // our own modid — excluded by design
            assertTrue(tSeed.contains(tRow.domain()), "a PRIMARY-bearing batch-2 domain seeds: " + tRow.domain());
        }
        assertEquals(45, tBatch2PrimaryDomains.size(), "the batch-2 PRIMARY-domain census (44 seedable + GT5U)");
        assertEquals(52, tSeed.size(), "the seed = the batch-1 eight + the 44 batch-2 PRIMARY domains");

        // A batch-2 COMMON_SECONDARY row answers no domain and survives its own domain's ABSENT pin,
        // while the domain's PRIMARY rows hide (ADR-MDH2 over batch-2 data; Thaumcraft has both kinds).
        assertTrue(countRegistered("Thaumium") > 0, "TC precondition: Thaumium (COMMON_SECONDARY flag) registers");
        assertTrue(countRegistered("VoidMetal") > 0, "TC precondition: VoidMetal (PRIMARY) registers");
        assertEquals(null, GT6ForeignMaterialAtlas.domainOf(material(rowOf(tBatch2, "Thaumium"))), "CS row answers no domain");
        GT6ModDrivers.setDriver(MT.MD.TC.mID, GT6ModDrivers.DriverLevel.ABSENT);
        assertEquals(0, countRegistered("VoidMetal"), "TC PRIMARY rows hide under the ABSENT pin");
        assertTrue(countRegistered("Thaumium") > 0, "TC COMMON_SECONDARY rows survive the ABSENT pin");
        GT6ModDrivers.reset();
        assertTrue(countRegistered("VoidMetal") > 0 && countRegistered("Thaumium") > 0, "reset restores both");
    }

    @Test
    public void tableIntegrityUniqueRowsAndLegalDomains() {
        // Every row's domain must be one of the port's MD.MD constant ids (reflection over the MD class).
        Set<String> tLegal = new HashSet<>();
        for (java.lang.reflect.Field tField : MT.MD.class.getFields()) {
            try {
                tLegal.add(((MT.MDRef) tField.get(null)).mID);
            } catch (IllegalAccessException aE) {
                throw new AssertionError(aE);
            }
        }
        // No (domain, material) pair twice — the upstream duplicate statements (RoC Prismane/Lonsdaleite)
        // were collapsed to one row each; every other material appears at most once per domain.
        Set<String> tSeen = new HashSet<>();
        for (Row tRow : GT6ForeignMaterialAtlas.rows()) {
            assertTrue(tLegal.contains(tRow.domain()), "domain id must be an MD.MD constant: " + tRow.domain());
            OreDictMaterial tMaterial = material(tRow);
            assertTrue(tSeen.add(tRow.domain() + "/" + tMaterial.mNameInternal), "duplicate (domain, material) row: " + tMaterial.mNameInternal);
        }
        assertEquals(TOTAL_ROWS, tSeen.size(), "every row is unique");
    }

    // ---- helpers ----

    private static Row rowOf(List<Row> aRows, String aInternalName) {
        return aRows.stream().filter(t -> material(t).mNameInternal.equals(aInternalName)).findFirst().orElse(null);
    }

    /** The live MT field behind a pair name — object identity, robust against alias-hosted internal names (Ni→"Nickel", Cu→"Copper"). */
    private static OreDictMaterial fieldMaterial(String aField) {
        try {
            return (OreDictMaterial) MT.class.getField(aField).get(null);
        } catch (ReflectiveOperationException aE) {
            throw new AssertionError(aE);
        }
    }

    private static long countDomain(List<Row> aRows, String aDomain) {
        return aRows.stream().filter(tRow -> tRow.domain().equals(aDomain)).count();
    }

    private static long countKind(List<Row> aRows, AttributionKind aKind) {
        return aRows.stream().filter(tRow -> aKind == null || tRow.kind() == aKind).count();
    }

    @Test
    public void threeKindContractWithPortAttributionCrossCheck() {
        for (Row tRow : GT6ForeignMaterialAtlas.rows()) {
            OreDictMaterial tMaterial = material(tRow);
            // Port-side corroboration: the transcription must agree with the attribution the
            // port actually carried (MT.java:3099+ setOriginalMod section, 599 rows).
            assertEquals(tRow.domain(), tMaterial.mOriginalMod, "mOriginalMod cross-check: " + tMaterial.mNameInternal);
            // ADR-MDH2: only PRIMARY rows answer their domain; CS/SELF rows stay unattributed (never hide).
            if (tRow.kind() == AttributionKind.PRIMARY) {
                if (tMaterial.mID >= 0) {
                    assertEquals(tRow.domain(), GT6ForeignMaterialAtlas.domainOf(tMaterial), "PRIMARY answers its domain: " + tMaterial.mNameInternal);
                } else {
                    // The port hosts some upstream PRIMARY materials as technical aliases
                    // (mID < 0, e.g. Advanced) — no own items, never indexed, nothing to hide.
                    assertEquals(null, GT6ForeignMaterialAtlas.domainOf(tMaterial), "alias-hosted PRIMARY has no index entry: " + tMaterial.mNameInternal);
                }
            } else {
                // COMMON_SECONDARY/GT6_SELF never enter the index — including technical alias
                // rows like TECH.RefinedIron (mID < 0, re-registered onto WroughtIron).
                assertEquals(null, GT6ForeignMaterialAtlas.domainOf(tMaterial), tRow.kind() + " never answers a domain: " + tMaterial.mNameInternal);
            }
        }
    }

    @Test
    public void secondaryAndSelfRowsSurviveAbsentInjection() {
        int tBaseline = GTMaterialItems.registrationOrder().size();
        assertTrue(countRegistered("Enderium") > 0, "TE precondition: Enderium (PRIMARY) registers");
        assertTrue(countRegistered("Nickel") > 0, "TE precondition: Ni (COMMON_SECONDARY flag) registers");
        assertTrue(countRegistered("Invar") > 0, "TE precondition: Invar (COMMON_SECONDARY conservative) registers");
        assertTrue(countRegistered("Pyrotheum") > 0, "TE precondition: Pyrotheum (GT6_SELF) registers");
        assertTrue(countRegistered("Angmallen") > 0, "MET precondition");

        // The production lookup is live (reset restored the atlas default): one ABSENT pin,
        // no fixture attribution — PRIMARY rows drop, COMMON_SECONDARY and GT6_SELF survive.
        GT6ModDrivers.setDriver(MT.MD.TE.mID, GT6ModDrivers.DriverLevel.ABSENT);
        assertEquals(0, countRegistered("Enderium"), "TE PRIMARY rows hide under the ABSENT pin");
        assertTrue(countRegistered("Nickel") > 0, "COMMON_SECONDARY (COMMON_ORE flag row) still registers");
        assertTrue(countRegistered("Invar") > 0, "COMMON_SECONDARY (conservative shared alloy) still registers");
        assertTrue(countRegistered("Pyrotheum") > 0, "GT6_SELF (theum quartet, lesson id1218) still registers");

        GT6ModDrivers.setDriver(MT.MD.MET.mID, GT6ModDrivers.DriverLevel.ABSENT);
        assertEquals(0, countRegistered("Angmallen"), "MET (all-PRIMARY domain) hides under the ABSENT pin");

        GT6ModDrivers.reset();
        assertEquals(tBaseline, GTMaterialItems.registrationOrder().size(), "reset restores the full universe");
        assertTrue(countRegistered("Enderium") > 0 && countRegistered("Angmallen") > 0, "hidden PRIMARY rows come back");
    }

    @Test
    public void defaultGateLeavesTheUniverseUnchanged() {
        int tBaseline = GTMaterialItems.registrationOrder().size();
        for (String tDomain : GT6ForeignMaterialAtlas.seedableDomains()) {
            assertTrue(GT6ModDrivers.isLoaded(tDomain), "no seeded state offline/at-test: every atlas domain answers loaded");
        }
        assertEquals(tBaseline, GTMaterialItems.registrationOrder().size(), "the wired production lookup hides nothing without an ABSENT pin");
        for (OreDictMaterial tMaterial : MaterialRegistry.INSTANCE.MATERIAL_ARRAY) {
            if (tMaterial == null || tMaterial.mID < 0) continue;
            if (GT6ForeignMaterialAtlas.domainOf(tMaterial) != null) assertTrue(GT6ModDrivers.isVisible(tMaterial), "unpinned attribution stays visible: " + tMaterial.mNameInternal);
        }
    }
}
