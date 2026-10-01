/**
 * Tests for task mdh-2-atlas-mapping: the batch-1 foreign-material attribution table.
 *
 * <p>Pins, in acceptance order: the row census (eight domains, exact counts, exact
 * seedable modid set), the three-kind contract of ADR-MDH2 (PRIMARY answers its domain,
 * COMMON_SECONDARY/GT6_SELF never do — with the port's own carried attribution strings
 * ({@code setOriginalMod}, MT.java:3099+) cross-checking every row's domain), and the
 * secondary-attribution protection: under an ABSENT pin the domain's PRIMARY rows drop
 * from the registration universe while its COMMON_SECONDARY and GT6_SELF rows survive.
 *
 * <p>The offline JVM has no FML and the neo junit-fml JVM is kept out of seeding by the
 * GT6ModDrivers test-JVM guard, so every ABSENT state here is explicit seam injection.
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregtech6.registry.GT6ForeignMaterialAtlas.AttributionKind;
import gregtech6.registry.GT6ForeignMaterialAtlas.Row;

public class GT6ForeignMaterialAtlasTest {

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
        assertEquals(164, tRows.size(), "batch-1 total: the eight-domain transcription ratchet");

        // Per-domain totals, upstream re-read order (each domain's block range in the atlas javadoc).
        assertEquals(19, tRows.stream().filter(tRow -> tRow.domain().equals(MT.MD.HaC.mID)).count(), "harvestcraft :2089-2107");
        assertEquals(17, tRows.stream().filter(tRow -> tRow.domain().equals(MT.MD.IC2.mID)).count(), "IC2 :2207-2223");
        assertEquals(16, tRows.stream().filter(tRow -> tRow.domain().equals(MT.MD.TE.mID)).count(), "ThermalExpansion :2318-2333");
        assertEquals(16, tRows.stream().filter(tRow -> tRow.domain().equals(MT.MD.EIO.mID)).count(), "EnderIO :2361-2376");
        assertEquals(18, tRows.stream().filter(tRow -> tRow.domain().equals(MT.MD.HBM.mID)).count(), "hbm :2391-2408");
        assertEquals(17, tRows.stream().filter(tRow -> tRow.domain().equals(MT.MD.BOTA.mID)).count(), "Botania :2455-2471");
        assertEquals(27, tRows.stream().filter(tRow -> tRow.domain().equals(MT.MD.GC_EXTRAPLANETS.mID)).count(), "ExtraPlanets :2566-2592");
        assertEquals(34, tRows.stream().filter(tRow -> tRow.domain().equals(MT.MD.MET.mID)).count(), "Metallurgy :2683-2716");

        // Three-value account: 137 PRIMARY / 23 COMMON_SECONDARY / 4 GT6_SELF.
        assertEquals(137, tRows.stream().filter(tRow -> tRow.kind() == AttributionKind.PRIMARY).count(), "PRIMARY total");
        assertEquals(23, tRows.stream().filter(tRow -> tRow.kind() == AttributionKind.COMMON_SECONDARY).count(), "COMMON_SECONDARY total");
        assertEquals(4, tRows.stream().filter(tRow -> tRow.kind() == AttributionKind.GT6_SELF).count(), "GT6_SELF total: the theum quartet only");

        // The seed walks exactly the eight PRIMARY-bearing domains, first-seen order.
        assertEquals(List.of(MT.MD.HaC.mID, MT.MD.IC2.mID, MT.MD.TE.mID, MT.MD.EIO.mID,
                MT.MD.HBM.mID, MT.MD.BOTA.mID, MT.MD.GC_EXTRAPLANETS.mID, MT.MD.MET.mID),
                GT6ForeignMaterialAtlas.seedableDomains(), "SEEDED_DOMAINS source: distinct PRIMARY domains in row order");
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
