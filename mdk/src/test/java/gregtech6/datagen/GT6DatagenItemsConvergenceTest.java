/**
 * Tests for task p3-fullprefix-creativetab: dual-criterion convergence.
 *
 * <p>GT6DatagenItems must be a pure adapter over the single enumeration source in
 * GTMaterialItems — in a headless JVM (no RegisterEvent) the datagen fallback walk and the
 * registration bridge walk must produce the identical (prefix, material, id) sequence.
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.registry.GTMaterialItems;

public class GT6DatagenItemsConvergenceTest {

    @BeforeAll
    public static void initMaterialSystem() {
        GTMaterialItems.initMaterials();
    }

    @Test
    public void datagenFallbackEqualsRegistrationOrder() {
        // headless JVM: INDEX is empty (no RegisterEvent fired), so collect() exercises the fallback
        List<GT6DatagenItems.Entry> tEntries = GT6DatagenItems.collect();
        List<GTMaterialItems.PrefixMaterial> tOrder = GTMaterialItems.registrationOrder();
        assertEquals(tOrder.size(), tEntries.size(), "datagen fallback and registration walk must agree on the item count");
        for (int i = 0; i < tOrder.size(); i++) {
            GTMaterialItems.PrefixMaterial tPair = tOrder.get(i);
            GT6DatagenItems.Entry tEntry = tEntries.get(i);
            assertEquals(tPair.prefix(), tEntry.prefix(), "sequence divergence at " + i);
            assertEquals(tPair.material(), tEntry.material(), "sequence divergence at " + i);
            assertEquals(GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()), tEntry.itemId(), "id divergence at " + i);
        }
    }

    @Test
    public void expansionOrderOfMagnitude() {
        // The expansion lands in the GTCEu-precedent band (ADR-P2-3: "1.20.1 with 40k+ items is feasible"):
        // 4 phase-2 prefixes were 2469 items; the 105-prefix item path is ~58k pairs (jshell census 2026-08-30).
        int tCount = GTMaterialItems.registrationOrder().size();
        org.junit.jupiter.api.Assertions.assertTrue(tCount > 40_000, "full-prefix expansion must register the full item path, got " + tCount);
        org.junit.jupiter.api.Assertions.assertTrue(tCount < 100_000, "expansion must stay in the upstream item universe, got " + tCount);
        assertEquals(tCount, new ArrayList<>(GTMaterialItems.registrationOrder()).size());
    }
}
