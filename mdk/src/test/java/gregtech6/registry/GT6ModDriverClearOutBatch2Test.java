/**
 * Tests for task mdh-clearout-batch2: the batch-2 clear-out wave — the activation of the
 * remaining 44 PRIMARY domains (the mdh-3 家法 over the batch-2 atlas rows; the batch-1
 * eight already live in {@link GT6ModDriverClearOutWaveTest}).
 *
 * <p>The kept ledger (the card report's 对账表, probe-measured 2026-10-02, forge offline
 * leg — the mdh-3 probe-first discipline): per-domain registration-order drops reconciled
 * against the atlas PRIMARY row counts (45 ledger domains, live 7285 — the WOOD-gate OP.plank wave 2ef11c4dc scattered plank items into the wood-carrying domains), the batch-1 + batch-2
 * joint pin (15521 = 8237 + 7285 exactly — no cross-domain bleed), and the universe/tab
 * before-after (57243 → 41721; 49964 → 36165 creative-visible pairs, 101 prefix families,
 * none empties out).
 *
 * <p>The five census faces re-sign here over a batch-2 domain arm (all five walk the ONE
 * shared root — {@link GTMaterialItems#registrationOrder()}); the SEEDED_DOMAINS activation
 * itself is pinned by the seed derivation census in GT6ForeignMaterialAtlasTest (52
 * table-driven domains) and by the runData byte-identity rerun (the committed tree stays
 * default-mode — the datagen short-circuit, GT6ModDriversTest).
 *
 * <p>The offline JVM has no FML and the neo junit-fml JVM is kept out of seeding by the
 * GT6ModDrivers test-JVM guard, so every ABSENT state here is explicit seam injection and
 * the pristine default is the zero-change proof.
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;
import gregtech6.fluid.GTFluids;
import gregtech6.integration.kjs.GT6KJS;
import gregtech6.registry.GT6ForeignMaterialAtlas.AttributionKind;
import gregtech6.registry.GT6ForeignMaterialAtlas.Row;
import gregtech6.registry.GT6ModDrivers.DriverLevel;

public class GT6ModDriverClearOutBatch2Test {

    /** The default registration universe (the mdh-3 post-casing census 57113 + 130 GT6-core
     * pairs the post-probe main merges registered — wood-planks/concrete/beam/small-tank —
     * seat-IX re-baseline; unchanged by this card's default mode — the zero-change proof). */
    private static final int BASELINE = 57243;
    /** The batch-2 per-domain kept ledger, probe-measured (the card report quotes these).
     * 44 PRIMARY domains; sum = 7234. The GT5U "gregtech" rows are not seedable (our own
     * modid) and carry no ledger line. */
    private static final Map<String, Integer> PER_DOMAIN_DROPS = Map.ofEntries(
            Map.entry(MT.MD.EtFu.mID, 114), // the Bamboo plank is NOT in the WOOD gate (the live recompute kept 114)
            Map.entry(MT.MD.Salt.mID, 19),
            Map.entry(MT.MD.GrC.mID, 6),
            Map.entry(MT.MD.NePl.mID, 91),
            Map.entry(MT.MD.NeLi.mID, 11),
            Map.entry(MT.MD.EnLi.mID, 22),
            Map.entry(MT.MD.IHL.mID, 221),
            Map.entry(MT.MD.FR.mID, 136),
            Map.entry(MT.MD.BINNIE.mID, 23), // +1 the binnie wood plank (the WOOD-gate wave)
            Map.entry(MT.MD.TFC.mID, 528),
            Map.entry(MT.MD.TF.mID, 895), // +4 the twilight woods' planks (the WOOD-gate wave)
            Map.entry(MT.MD.ERE.mID, 135), // +1 the erebus wood plank (the WOOD-gate wave)
            Map.entry(MT.MD.RC.mID, 44),
            Map.entry(MT.MD.PnC.mID, 79),
            Map.entry(MT.MD.SC2.mID, 79),
            Map.entry(MT.MD.TiC.mID, 185),
            Map.entry(MT.MD.AA.mID, 57),
            Map.entry(MT.MD.MFR.mID, 30),
            Map.entry(MT.MD.ReC.mID, 133),
            Map.entry(MT.MD.RoC.mID, 752),
            Map.entry(MT.MD.Mek.mID, 167),
            Map.entry(MT.MD.TC.mID, 287), // +2 Greatwood/Silverwood planks (the WOOD-gate wave)
            Map.entry(MT.MD.TCTE.mID, 88),
            Map.entry(MT.MD.ALF.mID, 387),
            Map.entry(MT.MD.CANDY.mID, 208), // +1 post-probe main registration (the live recompute's verdict, seat-IX re-baseline)
            Map.entry(MT.MD.GC_ADV_ROCKETRY.mID, 176),
            Map.entry(MT.MD.MaCu.mID, 45),
            Map.entry(MT.MD.DE.mID, 88),
            Map.entry(MT.MD.AV.mID, 264),
            Map.entry(MT.MD.PE.mID, 134),
            Map.entry(MT.MD.FM.mID, 324),
            Map.entry(MT.MD.TROPIC.mID, 246),
            Map.entry(MT.MD.ARS.mID, 95),
            Map.entry(MT.MD.GC.mID, 89),
            Map.entry(MT.MD.GC_GALAXYSPACE.mID, 175),
            Map.entry(MT.MD.MO.mID, 73),
            Map.entry(MT.MD.RT.mID, 99),
            Map.entry(MT.MD.RP.mID, 40), // the NikolineAlloy row (mdh-clearout-batch2 segment 4) made RP a PRIMARY domain
            Map.entry(MT.MD.ExU.mID, 96),
            Map.entry(MT.MD.BTL.mID, 303), // +1 post-probe main registration (same batch as the CANDY +1)
            Map.entry(MT.MD.AETHER.mID, 53), // +1 the Skyroot plank (the WOOD-gate wave)
            Map.entry(MT.MD.PR.mID, 38),
            Map.entry(MT.MD.BP.mID, 38),
            Map.entry(MT.MD.FZ.mID, 108),
            Map.entry(MT.MD.PFAA.mID, 104));
    /** The batch-1 ledger (GT6ModDriverClearOutWaveTest) — the joint pin's other half (8234 + 3 the BOTA planks, seat XVII). */
    private static final int BATCH1_TOTAL = 8237;
    /** 8237 + 7285 = 15521; 57243 − 15522 = 41721 (the batch-2 total includes the RP 40 from the NikolineAlloy row and the post-probe CANDY +1). */
    private static final int BATCH2_TOTAL = PER_DOMAIN_DROPS.values().stream().mapToInt(Integer::intValue).sum();
    private static final int JOINT_DROP = BATCH1_TOTAL + BATCH2_TOTAL;
    /** The batch-1 eight, the seed prefix (the wave test's ledger keys). */
    private static final List<String> BATCH1 = List.of(MT.MD.HaC.mID, MT.MD.IC2.mID, MT.MD.TE.mID,
            MT.MD.EIO.mID, MT.MD.HBM.mID, MT.MD.BOTA.mID, MT.MD.GC_EXTRAPLANETS.mID, MT.MD.MET.mID);
    /** Creative-visible pairs before/after the joint pin (the tab face's before/after numbers). */
    private static final int TAB_DEFAULT = 49964;
    private static final int TAB_AFTER = 36165;

    @BeforeAll
    public static void initMaterialSystem() {
        // the fluid-ratchet arm touches GTFluids (ForgeRegistries at clinit) — bootstrap offline
        // instead of relying on a same-fork neighbour to have done it (the GTOfflineTestBase form)
        SharedConstants.tryDetectVersion();
        try {
            Bootstrap.bootStrap();
        } catch (Throwable ignored) {
            // offline init noise; the registries are usable by now
        }
        GTMaterialItems.initMaterials();
    }

    @AfterEach
    public void restorePristineDriver() {
        GT6ModDrivers.reset();
    }

    // ---- the per-domain kept ledger (card acceptance 2: 逐域对账 + joint 无渗漏) ----

    @Test
    public void perDomainKeptLedgerMatchesTheAtlas() {
        assertEquals(BASELINE, GTMaterialItems.registrationOrder().size(), "default universe unchanged (the zero-change proof)");
        assertEquals(7285, BATCH2_TOTAL, "the batch-2 drop total (incl. the NikolineAlloy RP 40)");

        Set<String> allDroppedNames = new java.util.HashSet<>();
        for (Map.Entry<String, Integer> tEntry : PER_DOMAIN_DROPS.entrySet()) {
            String tDomain = tEntry.getKey();
            Set<String> tPrimaryNames = primaryNamesOf(tDomain);
            assertFalse(tPrimaryNames.isEmpty(), "every ledger domain owns PRIMARY materials: " + tDomain);

            GT6ModDrivers.setDriver(tDomain, DriverLevel.ABSENT);
            List<GTMaterialItems.PrefixMaterial> tShrunk = GTMaterialItems.registrationOrder();
            assertEquals(BASELINE - tEntry.getValue(), tShrunk.size(), "kept count after pinning " + tDomain + " ABSENT");
            for (GTMaterialItems.PrefixMaterial tPair : tShrunk)
                assertFalse(tPrimaryNames.contains(tPair.material().mNameInternal),
                        tPair.material().mNameInternal + " is " + tDomain + " PRIMARY and must drop entirely");
            // the domain's COMMON_SECONDARY/GT6_SELF members survive the same pin (ADR-MDH2)
            for (Row tRow : GT6ForeignMaterialAtlas.rows()) {
                if (!tRow.domain().equals(tDomain) || tRow.kind() == AttributionKind.PRIMARY) continue;
                OreDictMaterial tMaterial = tRow.material().get();
                if (tMaterial == null || tMaterial.mID < 0) continue;
                assertTrue(GT6ModDrivers.isVisible(tMaterial), tRow.kind() + " row survives its own domain's pin: " + tMaterial.mNameInternal);
                assertTrue(tShrunk.stream().anyMatch(tPair -> tPair.material() == tMaterial),
                        tRow.kind() + " row keeps its registration pairs: " + tMaterial.mNameInternal);
            }
            allDroppedNames.addAll(tPrimaryNames);
            GT6ModDrivers.reset();
            assertEquals(BASELINE, GTMaterialItems.registrationOrder().size(), "reset between domains");
        }

        // the joint pin: batch-1 eight + all 44 batch-2 domains — exactly the union's pairs leave
        for (String tDomain : BATCH1) GT6ModDrivers.setDriver(tDomain, DriverLevel.ABSENT);
        for (String tDomain : PER_DOMAIN_DROPS.keySet()) GT6ModDrivers.setDriver(tDomain, DriverLevel.ABSENT);
        assertEquals(BASELINE - JOINT_DROP, GTMaterialItems.registrationOrder().size(),
                "the joint pin keeps 41721 (15522 = 8237 + 7285 exactly — no cross-domain bleed)");
        for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder())
            assertFalse(allDroppedNames.contains(tPair.material().mNameInternal),
                    "no batch-2 PRIMARY material survives the joint pin: " + tPair.material().mNameInternal);
    }

    // ---- the five census faces re-signed over a batch-2 arm (ADR-MDH4, the mdh-3 家法) ----

    @Test
    public void assetGuardFaceIdsShrinkWithTheRoot() {
        Set<String> tDefaultIds = new TreeSet<>();
        for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) tDefaultIds.add(GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
        assertEquals(BASELINE, tDefaultIds.size(), "the asset-guard item channel ids are the full universe by default");

        GT6ModDrivers.setDriver(MT.MD.TiC.mID, DriverLevel.ABSENT);
        Set<String> tShrunkIds = new TreeSet<>();
        for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) tShrunkIds.add(GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
        assertEquals(BASELINE - PER_DOMAIN_DROPS.get(MT.MD.TiC.mID), tShrunkIds.size(), "the guard's item channel shrinks to the kept count");
        Set<String> tDroppedIds = new TreeSet<>(tDefaultIds);
        tDroppedIds.removeAll(tShrunkIds);
        assertFalse(tDroppedIds.isEmpty(), "the TiC pin drops ids from the guard universe (the model-id face)");
        String tSampleDropped = tDroppedIds.iterator().next();
        assertTrue(tDefaultIds.contains(tSampleDropped), "precondition: a dropped id existed by default");
    }

    @Test
    public void langFaceNameKeysExcludeHiddenPairs() {
        Set<String> tDefaultKeys = new TreeSet<>();
        for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) tDefaultKeys.add(GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));

        GT6ModDrivers.setDriver(MT.MD.TiC.mID, DriverLevel.ABSENT);
        Set<String> tShrunkKeys = new TreeSet<>();
        for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) tShrunkKeys.add(GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
        Set<String> tDroppedKeys = new TreeSet<>(tDefaultKeys);
        tDroppedKeys.removeAll(tShrunkKeys);
        assertFalse(tDroppedKeys.isEmpty(), "hidden materials leave the computed name-key walk (the lang parity source shrinks)");
        assertTrue(tShrunkKeys.contains("ingot_enderium"), "unpinned batch-1 domains keep their keys");
        assertTrue(tDefaultKeys.contains("ingot_enderium"), "precondition: the enderium key exists by default");
    }

    @Test
    public void datagenFaceConvergenceSourceShrinksWithTheRoot() {
        // GT6DatagenItemsConvergenceTest consumes registrationOrder() directly; the committed
        // tree is datagen'd in default mode (the seed short-circuits in datagen JVMs), so the
        // convergence pin itself is untouched — this arm pins that the SOURCE shrinks.
        assertEquals(BASELINE, GTMaterialItems.registrationOrder().size());
        GT6ModDrivers.setDriver(MT.MD.RoC.mID, DriverLevel.ABSENT);
        assertEquals(BASELINE - PER_DOMAIN_DROPS.get(MT.MD.RoC.mID), GTMaterialItems.registrationOrder().size(),
                "the convergence walk shrinks exactly by the RoC ledger line (the biggest batch-2 drop)");
    }

    @Test
    public void tabFaceBurdenBeforeAfter() {
        Set<String> tDefaultTabs = new TreeSet<>();
        for (var tPrefix : GTMaterialItems.tabPrefixes()) tDefaultTabs.add(tPrefix.mNameInternal);
        int tDefaultTabItems = registrationOrderTabItems();
        int tJointEligible = tabEligibleJointDrop(tDefaultTabItems); // resets the driver after itself

        for (String tDomain : BATCH1) GT6ModDrivers.setDriver(tDomain, DriverLevel.ABSENT);
        for (String tDomain : PER_DOMAIN_DROPS.keySet()) GT6ModDrivers.setDriver(tDomain, DriverLevel.ABSENT);
        Set<String> tAbsentTabs = new TreeSet<>();
        for (var tPrefix : GTMaterialItems.tabPrefixes()) tAbsentTabs.add(tPrefix.mNameInternal);
        int tAbsentTabItems = registrationOrderTabItems();
        GT6ModDrivers.reset();
        assertEquals(tDefaultTabs, tAbsentTabs, "no prefix family empties out — the tab set is unchanged (101 families)");
        assertEquals(TAB_AFTER, tAbsentTabItems, "the creative-visible tab pairs land at 36165 after the joint pin");
        assertEquals(TAB_DEFAULT - TAB_AFTER, tJointEligible, "the tab drop is exactly the tab-eligible share of the 15522 dropped pairs");
    }

    @Test
    public void kjsBindingSurfaceIsClassLevelAndSelfAligning() {
        Map<String, Object> tDefault = GT6KJS.bindingClasses();
        assertFalse(tDefault.isEmpty(), "binding census precondition");
        for (String tDomain : PER_DOMAIN_DROPS.keySet()) GT6ModDrivers.setDriver(tDomain, DriverLevel.ABSENT);
        assertEquals(tDefault.keySet(), GT6KJS.bindingClasses().keySet(),
                "the KJS binding surface is class-level — the registration universe shrinks beneath it, keys unchanged");
        assertEquals(GTMaterialItems.class, GT6KJS.bindingClasses().get("GTMaterialItems"), "the script item face binds the gated walk's owner");
    }

    // ---- the activation face: seed derivation + the fluid data-face ratchet carries over ----

    @Test
    public void theActivationSeedIsTheTableDrivenDomainSet() {
        List<String> tSeed = GT6ForeignMaterialAtlas.seedableDomains();
        assertEquals(53, tSeed.size(), "8 batch-1 + 45 batch-2 PRIMARY domains (RP joined with the NikolineAlloy row)");
        assertEquals(BATCH1, tSeed.subList(0, 8), "the batch-1 eight are the seed prefix in row order");
        for (String tDomain : PER_DOMAIN_DROPS.keySet()) assertTrue(tSeed.contains(tDomain), "every ledger domain seeds: " + tDomain);
        assertFalse(tSeed.contains(MT.MD.GT5U.mID), "GT5U (modid gregtech = self) never seeds");
        assertEquals(new java.util.HashSet<>(tSeed).size(), tSeed.size(), "the seed list is distinct");
    }

    @Test
    public void fluidFaceAnnotationsStayExhaustiveOverTheGrownTable() {
        // the mdh-3 fluid ratchet re-walked: every PRIMARY material-fluid row must carry its
        // atlas domain — over the batch-2 rows too (NikolineAlloy lands in the same card, the
        // ratchet below holds AFTER segment 4 as well).
        Map<String, GTFluids.ChemicalFluidSpec> tById = new HashMap<>();
        for (GTFluids.ChemicalFluidSpec tSpec : allChemicalSpecs()) tById.putIfAbsent(tSpec.name(), tSpec);
        for (Row tRow : GT6ForeignMaterialAtlas.rows()) {
            OreDictMaterial tMaterial = tRow.material().get();
            if (tMaterial == null || tMaterial.mID < 0) continue;
            String tBase = tMaterial.mNameInternal.toLowerCase();
            for (String tCandidate : List.of(tBase, tBase + "_molten")) {
                GTFluids.ChemicalFluidSpec tSpec = tById.get(tCandidate);
                if (tSpec == null) continue;
                if (tRow.kind() == AttributionKind.PRIMARY) {
                    assertEquals(tRow.domain(), tSpec.driverDomain(),
                            "a PRIMARY material-fluid row must carry its atlas domain: " + tCandidate);
                } else {
                    org.junit.jupiter.api.Assertions.assertNull(tSpec.driverDomain(), tRow.kind() + " material-fluid row must never hide: " + tCandidate);
                }
            }
        }
    }

    // ---- helpers ----

    /** The PRIMARY row materials of one domain, by internal name (mID<0 rows excluded). */
    private static Set<String> primaryNamesOf(String aDomain) {
        Set<String> rNames = new java.util.HashSet<>();
        for (Row tRow : GT6ForeignMaterialAtlas.rows()) {
            if (tRow.kind() != AttributionKind.PRIMARY || !tRow.domain().equals(aDomain)) continue;
            OreDictMaterial tMaterial = tRow.material().get();
            if (tMaterial != null && tMaterial.mID >= 0) rNames.add(tMaterial.mNameInternal);
        }
        return rNames;
    }

    /** Registration pairs that sit on a creative-visible prefix. */
    private static int registrationOrderTabItems() {
        Set<String> tTabPrefixes = new TreeSet<>();
        for (var tPrefix : GTMaterialItems.tabPrefixes()) tTabPrefixes.add(tPrefix.mNameInternal);
        int rCount = 0;
        for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder())
            if (tTabPrefixes.contains(tPair.prefix().mNameInternal)) rCount++;
        return rCount;
    }

    /** The tab-eligible share of the JOINT drop (batch-1 + batch-2), the tab arm's before/after bridge. */
    private static int tabEligibleJointDrop(int aDefaultTabItems) {
        Set<String> tTabPrefixes = new TreeSet<>();
        for (var tPrefix : GTMaterialItems.tabPrefixes()) tTabPrefixes.add(tPrefix.mNameInternal);
        for (String tDomain : BATCH1) GT6ModDrivers.setDriver(tDomain, DriverLevel.ABSENT);
        for (String tDomain : PER_DOMAIN_DROPS.keySet()) GT6ModDrivers.setDriver(tDomain, DriverLevel.ABSENT);
        int tKeptTabItems = 0;
        for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder())
            if (tTabPrefixes.contains(tPair.prefix().mNameInternal)) tKeptTabItems++;
        GT6ModDrivers.reset();
        return aDefaultTabItems - tKeptTabItems;
    }

    private static List<GTFluids.ChemicalFluidSpec> allChemicalSpecs() {
        List<GTFluids.ChemicalFluidSpec> rSpecs = new java.util.ArrayList<>();
        rSpecs.addAll(GTFluids.CHEMICAL_SPECS);
        rSpecs.addAll(GTFluids.HOT_FLUID_SPECS);
        rSpecs.addAll(GTFluids.CLOSURE_FLUID_SPECS);
        rSpecs.addAll(GTFluids.LUBRICANT_FLUID_SPECS);
        rSpecs.addAll(GTFluids.HONEY_FLUID_SPECS);
        rSpecs.addAll(GTFluids.BEE_ROW_FLUID_SPECS);
        rSpecs.addAll(GTFluids.QU_FLUID_SPECS);
        rSpecs.addAll(GTFluids.NAMING_FLUID_SPECS);
        return rSpecs;
    }
}
