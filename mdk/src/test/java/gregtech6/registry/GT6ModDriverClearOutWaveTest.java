/**
 * Tests for task mdh-3-clear-out-wave: the batch-1 clear-out wave — the data face that
 * makes an ABSENT domain actually shrink the port's registration universe, plus the five
 * census-face re-signs (ADR-MDH4: asset/lang/tab/datagen/KJS re-signed in the same card).
 *
 * <p>What mdh-3 owns here: the per-domain kept ledger (the ratchet the card report quotes),
 * the census arms (all five faces walk the ONE shared root —
 * {@link GTMaterialItems#registrationOrder()} — so each arm pins its face's own derivation:
 * id strings for the asset guard, name keys for lang, the convergence source list for
 * datagen, per-tab totals for the tab face, and the class-level binding surface for KJS),
 * and the fluid data-face ratchet (the three annotated material-fluid rows plus the
 * no-unannotated-PRIMARY-row pin — the family LISTS freeze at GTFluids class-init, so the
 * registration-core gate itself stays mdh-1's mechanism and the real-install face is
 * field_test).
 *
 * <p>The offline JVM has no FML and the neo junit-fml JVM is kept out of seeding by the
 * GT6ModDrivers test-JVM guard, so every ABSENT state here is explicit seam injection and
 * the pristine default is the zero-change proof (acceptance 2).
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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

public class GT6ModDriverClearOutWaveTest {

    /** The default registration universe (the mdh-2 census 56273 + the casing-machine-register
     * 840 = 4 casing prefixes × 210 materials, which landed on main AFTER this card's freeze —
     * ratchet protocol ADR-MDH4, histogram-verified pair-for-pair: 104 → 108 families, every
     * other family's count byte-identical; REVIEW FIX merge seat XVI) + 130 task
     * wood-planks-register (the plank WOOD domain; +1 creative-visible prefix family,
     * TAB_PREFIX_COUNT 101 → 102; REVIEW FIX seat XVII — the rebase exposed this
     * post-freeze pin to the card's +130). */
    private static final int BASELINE = 57243;
    /** The per-domain kept-drop ledger (the card report's 对账表, offline-measured 2026-10-01),
     * re-measured at review on the post-casing universe (probe run, forge offline leg): the
     * casingMachine 210-material axis overlaps five domains' PRIMARY materials, whose casing
     * pairs drop with their domain pin — TE +12, EIO +28, HBM +16, BOTA +16, MET +136
     * (total 8026 → 8234; HaC/IC2/EP untouched, sum = joint = no cross-domain bleed).
     * Review seat XVI. +3 BOTA (1129 → 1132) task wood-planks-register REVIEW FIX seat XVII:
     * Livingwood/Dreamwood/Shimmerwood are wood()-stamped WOOD materials (MT.java:2107-2109)
     * AND BOTA PRIMARY (atlas :203/:205/:206) — the plank registration put a plank item on
     * each, which joins BOTA's absent-pin drop. */
    private static final Map<String, Integer> PER_DOMAIN_DROPS = Map.of(
            MT.MD.HaC.mID, 211,
            MT.MD.IC2.mID, 75,
            MT.MD.TE.mID, 336,
            MT.MD.EIO.mID, 873,
            MT.MD.HBM.mID, 908,
            MT.MD.BOTA.mID, 1132,
            MT.MD.GC_EXTRAPLANETS.mID, 1510,
            MT.MD.MET.mID, 3192);
    /** 57243 − 8237 = 49006: the eight-domain joint pin keeps the ledger arithmetic honest. */
    private static final int TOTAL_DROP = PER_DOMAIN_DROPS.values().stream().mapToInt(Integer::intValue).sum();
    /** Creative-visible prefix families before and after the wave — no family empties out
     * (97 + the 4 casingMachine families that landed post-freeze + plank, task
     * wood-planks-register, see BASELINE). */
    private static final int TAB_PREFIX_COUNT = 102;

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

    // ---- the per-domain kept ledger (card acceptance 3) ----

    @Test
    public void perDomainKeptLedgerMatchesTheAtlas() {
        assertEquals(BASELINE, GTMaterialItems.registrationOrder().size(), "default universe unchanged (acceptance 2, zero-change proof)");
        assertEquals(8237, TOTAL_DROP, "the eight-domain drop total (57243 → 49006)");

        Set<String> allDroppedNames = new HashSet<>();
        for (Map.Entry<String, Integer> tEntry : PER_DOMAIN_DROPS.entrySet()) {
            String tDomain = tEntry.getKey();
            Set<String> tPrimaryNames = primaryNamesOf(tDomain);
            assertFalse(tPrimaryNames.isEmpty(), "every ledger domain owns PRIMARY materials: " + tDomain);

            GT6ModDrivers.setDriver(tDomain, DriverLevel.ABSENT);
            List<GTMaterialItems.PrefixMaterial> tShrunk = GTMaterialItems.registrationOrder();
            assertEquals(BASELINE - tEntry.getValue(), tShrunk.size(), "kept count after pinning " + tDomain + " ABSENT");
            // every dropped pair belongs to the pinned domain's PRIMARY rows, and they drop entirely
            for (GTMaterialItems.PrefixMaterial tPair : tShrunk)
                assertFalse(tPrimaryNames.contains(tPair.material().mNameInternal),
                        tPair.material().mNameInternal + " is " + tDomain + " PRIMARY and must drop entirely");
            // and the domain's COMMON_SECONDARY/GT6_SELF members survive the same pin (ADR-MDH2, the fluid-side ruling too)
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

        // the eight-domain joint pin: exactly the 137 PRIMARY materials' pairs leave (no cross-domain bleed)
        for (String tDomain : PER_DOMAIN_DROPS.keySet()) GT6ModDrivers.setDriver(tDomain, DriverLevel.ABSENT);
        assertEquals(BASELINE - TOTAL_DROP, GTMaterialItems.registrationOrder().size(), "all-eight ABSENT keeps 49006");
        for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder())
            assertFalse(allDroppedNames.contains(tPair.material().mNameInternal),
                    "no PRIMARY material of any pinned domain survives the joint pin: " + tPair.material().mNameInternal);
    }

    // ---- the five census faces, re-signed in-card (ADR-MDH4); all walk the shared root ----

    @Test
    public void assetGuardFaceIdsShrinkWithTheRoot() {
        Set<String> tDefaultIds = new TreeSet<>();
        for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) tDefaultIds.add(GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
        assertEquals(BASELINE, tDefaultIds.size(), "the asset-guard item channel ids are the full universe by default");

        GT6ModDrivers.setDriver(MT.MD.TE.mID, DriverLevel.ABSENT);
        Set<String> tShrunkIds = new TreeSet<>();
        for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) tShrunkIds.add(GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
        assertEquals(BASELINE - PER_DOMAIN_DROPS.get(MT.MD.TE.mID), tShrunkIds.size(), "the guard's item channel shrinks to the kept count");
        assertFalse(tShrunkIds.contains("ingot_enderium"), "a hidden material's model id leaves the guard universe");
        assertTrue(tDefaultIds.contains("ingot_enderium"), "precondition: the id exists in the default universe");
    }

    @Test
    public void langFaceNameKeysExcludeHiddenPairs() {
        List<String> tDefaultKeys = new ArrayList<>();
        for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) tDefaultKeys.add(GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
        assertTrue(tDefaultKeys.contains("ingot_enderium"), "lang parity source carries the default key set");
        assertTrue(tDefaultKeys.contains("ingot_manasteel"), "lang parity source carries Botania keys too");

        GT6ModDrivers.setDriver(MT.MD.BOTA.mID, DriverLevel.ABSENT);
        List<String> tShrunkKeys = new ArrayList<>();
        for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) tShrunkKeys.add(GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
        assertFalse(tShrunkKeys.contains("ingot_manasteel"), "hidden materials leave the computed name-key walk (extra committed keys are inert datagen artifacts)");
        assertTrue(tShrunkKeys.contains("ingot_enderium"), "unpinned domains keep their keys");
    }

    @Test
    public void datagenFaceConvergenceSourceShrinksWithTheRoot() {
        // GT6DatagenItemsConvergenceTest consumes registrationOrder() directly; the committed
        // tree is datagen'd in default mode, so the convergence pin itself is untouched
        // (acceptance 2) — this arm pins that the SOURCE the convergence test walks shrinks.
        assertEquals(BASELINE, GTMaterialItems.registrationOrder().size());
        GT6ModDrivers.setDriver(MT.MD.GC_EXTRAPLANETS.mID, DriverLevel.ABSENT);
        assertEquals(BASELINE - PER_DOMAIN_DROPS.get(MT.MD.GC_EXTRAPLANETS.mID), GTMaterialItems.registrationOrder().size(),
                "the convergence walk shrinks exactly by the domain's ledger line");
        assertEquals(BASELINE - PER_DOMAIN_DROPS.get(MT.MD.GC_EXTRAPLANETS.mID),
                GTMaterialItems.registrationOrder().stream().mapToInt(tPair -> 1).sum(), "the source list is the single walk (no second census)");
    }

    @Test
    public void tabFaceBurdenBeforeAfter() {
        Set<String> tDefaultTabs = new TreeSet<>();
        for (var tPrefix : GTMaterialItems.tabPrefixes()) tDefaultTabs.add(tPrefix.mNameInternal);
        assertEquals(TAB_PREFIX_COUNT, tDefaultTabs.size(), "default creative-visible prefix families");
        int tDefaultTabItems = registrationOrderTabItems();
        int tEligible = tabEligibleDrop(tDefaultTabItems); // resets the driver after itself

        for (String tDomain : PER_DOMAIN_DROPS.keySet()) GT6ModDrivers.setDriver(tDomain, DriverLevel.ABSENT);
        Set<String> tAbsentTabs = new TreeSet<>();
        for (var tPrefix : GTMaterialItems.tabPrefixes()) tAbsentTabs.add(tPrefix.mNameInternal);
        int tAbsentTabItems = registrationOrderTabItems();
        GT6ModDrivers.reset();
        assertEquals(tDefaultTabs, tAbsentTabs, "no prefix family empties out — the tab set is unchanged");
        assertEquals(tDefaultTabItems - tEligible, tAbsentTabItems,
                "the tab burden shrinks exactly by the tab-eligible share of the 8234 dropped pairs");
    }

    @Test
    public void kjsBindingSurfaceIsClassLevelAndSelfAligning() {
        Map<String, Object> tDefault = GT6KJS.bindingClasses();
        assertFalse(tDefault.isEmpty(), "binding census precondition");
        for (String tDomain : PER_DOMAIN_DROPS.keySet()) GT6ModDrivers.setDriver(tDomain, DriverLevel.ABSENT);
        assertEquals(tDefault.keySet(), GT6KJS.bindingClasses().keySet(),
                "the KJS binding surface is class-level — the registration universe shrinks beneath it (script faces call the same gated walks), keys unchanged");
        assertEquals(GTMaterialItems.class, GT6KJS.bindingClasses().get("GTMaterialItems"), "the script item face binds the gated walk's owner");
    }

    // ---- the fluid data-face ratchet (the GTFluids half of the data face) ----

    @Test
    public void fluidDataFaceThreeAnnotatedRowsAndNoUnannotatedPrimaryRow() {
        // 1. the annotation census: exactly SEVEN material-fluid rows carry a domain —
        // mdh-3's three + the chem-blocker three (chem-fluids-unlock review fix:
        // H2O2/IHL, H2SO4/FZ, AquaRegia/FZ) + the nikolinealloy_molten RP row the
        // mdh-clearout-batch2 deferral landed (row + annotation one commit).
        Map<String, String> tAnnotated = new HashMap<>();
        for (GTFluids.ChemicalFluidSpec tSpec : allChemicalSpecs())
            if (tSpec.driverDomain() != null) tAnnotated.put(tSpec.name(), tSpec.driverDomain());
        assertEquals(Map.of("chocolate_molten", MT.MD.HaC.mID, "waxplant_molten", MT.MD.HaC.mID, "ic2uumatter", MT.MD.IC2.mID,
                        "hydrogenperoxide", MT.MD.IHL.mID, "sulfuricacid", MT.MD.FZ.mID, "aquaregia", MT.MD.FZ.mID,
                        "nikolinealloy_molten", MT.MD.RP.mID),
                tAnnotated, "the fluid annotation census (the seven PRIMARY material-fluid faces; the seat-IX rebase union of the mdh-3/chem-blocker census and this card's RP closeout row)");
        for (GTFluids.AquaFluidSpec tSpec : allAquaSpecs())
            assertNull(tSpec.driverDomain(), "no aqua-family row is driver-annotated: " + tSpec.name());
        for (GTFluids.EngineFluidSpec tSpec : GTFluids.ENGINE_SPECS)
            assertNull(tSpec.driverDomain(), "no engine-family row is driver-annotated: " + tSpec.name());

        // 2/3. the candidate ratchets, over every chemical-family table (the lookup seam covers
        // only CHEMICAL_SPECS, the annotated rows live in BEE_ROW/NAMING): a PRIMARY material's
        // would-be fluid ids (the createGas/createMolten naming faces) must not exist UNannotated
        // — future batch rows have to carry their domain; a COMMON_SECONDARY/GT6_SELF material's
        // must never carry one (rubber_molten, the theum quartet — the fluid-side ADR-MDH2 ruling).
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
                            "a PRIMARY material-fluid row must carry its atlas domain (mdh-3 ratchet): " + tCandidate);
                } else {
                    assertNull(tSpec.driverDomain(), tRow.kind() + " material-fluid row must never hide: " + tCandidate);
                }
            }
        }
    }

    // ---- helpers ----

    /** The PRIMARY row materials of one domain, by internal name (alias-hosted mID<0 rows excluded — nothing to hide). */
    private static Set<String> primaryNamesOf(String aDomain) {
        Set<String> rNames = new HashSet<>();
        for (Row tRow : GT6ForeignMaterialAtlas.rows()) {
            if (tRow.kind() != AttributionKind.PRIMARY || !tRow.domain().equals(aDomain)) continue;
            OreDictMaterial tMaterial = tRow.material().get();
            if (tMaterial != null && tMaterial.mID >= 0) rNames.add(tMaterial.mNameInternal);
        }
        return rNames;
    }

    /** Registration pairs that sit on a creative-visible prefix (the tab universe the tab face counts). */
    private static int registrationOrderTabItems() {
        Set<String> tTabPrefixes = new HashSet<>();
        for (var tPrefix : GTMaterialItems.tabPrefixes()) tTabPrefixes.add(tPrefix.mNameInternal);
        int rCount = 0;
        for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder())
            if (tTabPrefixes.contains(tPair.prefix().mNameInternal)) rCount++;
        return rCount;
    }

    /** The tab-eligible share of the ledger drop: per domain, the tab pairs present by default minus the pairs the pin keeps. */
    private static int tabEligibleDrop(int aDefaultTabItems) {
        Set<String> tTabPrefixes = new HashSet<>();
        for (var tPrefix : GTMaterialItems.tabPrefixes()) tTabPrefixes.add(tPrefix.mNameInternal);
        int rCount = 0;
        for (String tDomain : PER_DOMAIN_DROPS.keySet()) {
            GT6ModDrivers.setDriver(tDomain, DriverLevel.ABSENT);
            int tKeptTabItems = 0;
            for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder())
                if (tTabPrefixes.contains(tPair.prefix().mNameInternal)) tKeptTabItems++;
            GT6ModDrivers.reset();
            rCount += aDefaultTabItems - tKeptTabItems;
        }
        return rCount;
    }

    private static List<GTFluids.ChemicalFluidSpec> allChemicalSpecs() {
        List<GTFluids.ChemicalFluidSpec> rSpecs = new ArrayList<>();
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

    private static List<GTFluids.AquaFluidSpec> allAquaSpecs() {
        List<GTFluids.AquaFluidSpec> rSpecs = new ArrayList<>();
        rSpecs.addAll(GTFluids.AQUA_SPECS);
        rSpecs.addAll(GTFluids.SIMPLE_LIQUID_SPECS);
        rSpecs.addAll(GTFluids.FOOD_FLUID_SPECS);
        rSpecs.addAll(GTFluids.FOOD_B1_SPECS);
        rSpecs.addAll(GTFluids.FOOD_B2_SPECS);
        rSpecs.addAll(GTFluids.FOOD_TAIL_SPECS);
        return rSpecs;
    }
}
