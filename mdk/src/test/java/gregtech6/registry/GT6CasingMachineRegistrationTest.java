/**
 * Task casing-machine-register: the four machine-casing prefix families on the port item path.
 *
 * <p>Pins (each against the upstream anchor):
 * <ul>
 * <li>the quartet census — 210 items per family on the port universe (the research estimate was
 *     209/family = 836 over the upstream face; the port PARTS∧SMITHABLE domain measures 210 —
 *     the actual is the pin, the estimate is the declared delta); the conditions are upstream
 *     verbatim and already lived in the port OP (OP.java:1274-1277: casingMachine =
 *     And(PARTS, SMITHABLE), the other three chain setCondition(casingMachine)); upstream itself
 *     holds the family on the BLOCK path (Loader_PrefixBlocks.java:48-51), the item registration
 *     is the declared port deviation (GTMaterialItems.itemPathPrefixes javadoc);</li>
 * <li>the condition-gate decomposition — every casingMachine material contains PARTS
 *     (TD.ItemGenerator.PARTS) and SMITHABLE (TD.Processing.SMITHABLE), the four family domains
 *     are pairwise EQUAL (the chained prefix condition is {@code isTrue → canGenerateItem},
 *     OreDictPrefix.java:349/:291 — no blacklist/force rows exist for the family, upstream
 *     OP.java:594-625 census), and the casingSmall−casingMachine difference is exactly the
 *     non-SMITHABLE/non-PARTS remainder;</li>
 * <li>the id universe — {@code gt6:casing_machine[_dense|_double|_quadruple]_<material_snake}},
 *     pairwise distinct from the fold targets {@code gt6:casing_small_<snake>} (the folds stay,
 *     task boundary: no fold-consumer migration);</li>
 * <li>the fold regression nails — the four fold precedents' target items still register:
 *     casing_small_osmiridium (GT6ScannerUsbStickCraftingJsonTest:72), casing_small_steel_galvanized
 *     (GT6CircuitsCraftingJsonTest:77), the eight diesel slugs (GT6DieselCraftingJsonTest:63,
 *     the casingMachineDouble fold), and the transformer lock still resolves OP.casingSmall
 *     (GT6ElectricTransformers.CASING_LOCK_PREFIX).</li>
 * </ul>
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;

public class GT6CasingMachineRegistrationTest {

    @BeforeAll
    public static void initMaterialSystem() {
        GTMaterialItems.initMaterials();
    }

    private static List<GTMaterialItems.PrefixMaterial> order() {
        return GTMaterialItems.registrationOrder();
    }

    private static Set<String> itemIdsOf(OreDictPrefix aPrefix) {
        Set<String> rIds = new HashSet<>();
        for (GTMaterialItems.PrefixMaterial tPair : order()) {
            if (tPair.prefix() == aPrefix) rIds.add(GTMaterialItems.itemIdOf(aPrefix, tPair.material()));
        }
        return rIds;
    }

    private static boolean itemExists(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
        return order().contains(new GTMaterialItems.PrefixMaterial(aPrefix, aMaterial));
    }

    /** Census pin: 210 per family, 840 total — the measured port face over the verbatim conditions. */
    @Test
    public void theFourFamiliesRegister210ItemsEach() {
        long tMachine = order().stream().filter(p -> p.prefix() == OP.casingMachine).count();
        long tDouble = order().stream().filter(p -> p.prefix() == OP.casingMachineDouble).count();
        long tQuadruple = order().stream().filter(p -> p.prefix() == OP.casingMachineQuadruple).count();
        long tDense = order().stream().filter(p -> p.prefix() == OP.casingMachineDense).count();
        assertEquals(210, tMachine, "casingMachine census (OP.java:1274 And(PARTS, SMITHABLE) over the port universe)");
        assertEquals(210, tDouble, "casingMachineDouble census (OP.java:1275 setCondition(casingMachine))");
        assertEquals(210, tQuadruple, "casingMachineQuadruple census (OP.java:1276 setCondition(casingMachine))");
        assertEquals(210, tDense, "casingMachineDense census (OP.java:1277 setCondition(casingMachine))");
        assertEquals(840, tMachine + tDouble + tQuadruple + tDense, "the quartet total (the research 836 = 4x209 was the upstream-face estimate; the port domain measures 210/family)");
    }

    /** Condition-gate decomposition: the verbatim OP legs, measured on the live domain. */
    @Test
    public void theConditionGateIsPartsAndSmithable() {
        for (GTMaterialItems.PrefixMaterial tPair : order()) {
            if (tPair.prefix() != OP.casingMachine) continue;
            OreDictMaterial tMat = tPair.material();
            assertTrue(tMat.contains(TD.ItemGenerator.PARTS), "the PARTS leg (OP.java:1274): " + tMat.mNameInternal);
            assertTrue(tMat.contains(TD.Processing.SMITHABLE), "the SMITHABLE leg (OP.java:1274): " + tMat.mNameInternal);
        }
    }

    /** The chained conditions make all four domains pairwise equal (OreDictPrefix.isTrue :349 → canGenerateItem :291; no blacklist/force rows upstream OP.java:594-625). */
    @Test
    public void theFourDomainsArePairwiseEqual() {
        Set<OreDictMaterial> tMachine = new HashSet<>(), tDouble = new HashSet<>(),
                tQuadruple = new HashSet<>(), tDense = new HashSet<>();
        for (GTMaterialItems.PrefixMaterial tPair : order()) {
            if (tPair.prefix() == OP.casingMachine) tMachine.add(tPair.material());
            else if (tPair.prefix() == OP.casingMachineDouble) tDouble.add(tPair.material());
            else if (tPair.prefix() == OP.casingMachineQuadruple) tQuadruple.add(tPair.material());
            else if (tPair.prefix() == OP.casingMachineDense) tDense.add(tPair.material());
        }
        assertEquals(tMachine, tDouble, "Double == Machine (the chained condition)");
        assertEquals(tMachine, tQuadruple, "Quadruple == Machine (the chained condition)");
        assertEquals(tMachine, tDense, "Dense == Machine (the chained condition)");
    }

    /** casingMachine ⊆ casingSmall, and the difference is exactly the non-(PARTS∧SMITHABLE) remainder (casingSmall = PARTS alone, OP.java:1273). */
    @Test
    public void theMachineDomainIsTheSmithableSliceOfCasingSmall() {
        Set<OreDictMaterial> tSmall = new HashSet<>(), tMachine = new HashSet<>();
        for (GTMaterialItems.PrefixMaterial tPair : order()) {
            if (tPair.prefix() == OP.casingSmall) tSmall.add(tPair.material());
            else if (tPair.prefix() == OP.casingMachine) tMachine.add(tPair.material());
        }
        assertTrue(tSmall.containsAll(tMachine), "And(PARTS, SMITHABLE) ⊆ PARTS");
        for (OreDictMaterial tMat : tSmall) {
            if (!tMachine.contains(tMat)) {
                boolean tSmithableOrParts = tMat.contains(TD.Processing.SMITHABLE) && tMat.contains(TD.ItemGenerator.PARTS);
                assertFalse(tSmithableOrParts, "outside the machine face ⇒ not (PARTS∧SMITHABLE): " + tMat.mNameInternal);
            }
        }
    }

    /** Id universe: the snake composition and pairwise separation from the casingSmall fold targets. */
    @Test
    public void theQuartetIdsAreTheMachineSnakeShape() {
        assertTrue(itemIdsOf(OP.casingMachine).contains("casing_machine_iron"), "gt6:casing_machine_iron composes");
        assertTrue(itemIdsOf(OP.casingMachineDouble).contains("casing_machine_double_iron"), "gt6:casing_machine_double_iron composes");
        assertTrue(itemIdsOf(OP.casingMachineQuadruple).contains("casing_machine_quadruple_iron"), "gt6:casing_machine_quadruple_iron composes");
        assertTrue(itemIdsOf(OP.casingMachineDense).contains("casing_machine_dense_iron"), "gt6:casing_machine_dense_iron composes");
        // the fold targets stay their OWN ids — no fold-consumer migration (task boundary)
        Set<String> tSmallIds = itemIdsOf(OP.casingSmall);
        for (String tMachineId : itemIdsOf(OP.casingMachine)) {
            assertFalse(tSmallIds.contains(tMachineId), "the machine id is not a fold target id: " + tMachineId);
        }
    }

    /** Fold regression nail ①: the ScannerUsbStick:72 / Cracker:58 / Circuits:77 targets still register. */
    @Test
    public void theFoldTargetItemsStillRegister() {
        assertTrue(itemExists(OP.casingSmall, MT.Osmiridium), "gt6:casing_small_osmiridium — the ScannerUsbStick:72 fold target");
        assertTrue(itemExists(OP.casingSmall, MT.SteelGalvanized), "gt6:casing_small_steel_galvanized — the Circuits:77 fold target");
        assertTrue(itemExists(OP.casingSmall, MT.Iron), "gt6:casing_small_iron — the Cracker:58 fold family head");
        assertSame(OP.casingSmall, GT6ElectricTransformers.CASING_LOCK_PREFIX.get(), "the transformer lock still resolves casingSmall (GT6ElectricTransformers.java:97)");
    }

    /** Fold regression nail ②: the eight diesel rows' casingMachineDouble folds still resolve onto casing_small_<slug> (GT6DieselCraftingJsonTest:63 — the pin rides the slug ids, the DieselSpec.material() strings ARE the material snake faces). */
    @Test
    public void theDieselFoldTargetsStillRegister() {
        assertEquals(8, gregtech6.registry.GT6Kinetics.DIESEL_SPECS.size(), "the eight diesel rows");
        Set<String> tSmallIds = itemIdsOf(OP.casingSmall);
        for (gregtech6.registry.GT6Kinetics.DieselSpec tSpec : gregtech6.registry.GT6Kinetics.DIESEL_SPECS) {
            assertTrue(tSmallIds.contains("casing_small_" + tSpec.material()), "the casingMachineDouble fold target lives: casing_small_" + tSpec.material());
            assertFalse(itemIdsOf(OP.casingMachineDouble).contains("casing_small_" + tSpec.material()), "sanity: the fold target id never collides into the machine family");
        }
    }

    /** The quartet rides no blacklist: upstream carries no disableItemGeneration rows for the family (OP.java:594-625 census) — the registration is the plain condition face. */
    @Test
    public void noBlacklistRowsExistForTheFamily() {
        for (OreDictPrefix tPrefix : OreDictPrefix.VALUES) {
            if (tPrefix != OP.casingMachine && tPrefix != OP.casingMachineDouble
                    && tPrefix != OP.casingMachineQuadruple && tPrefix != OP.casingMachineDense) continue;
            for (GTMaterialItems.PrefixMaterial tPair : order()) {
                if (tPair.prefix() == tPrefix) {
                    assertTrue(tPrefix.canGenerateItem(tPair.material()),
                            "isGeneratingItem == canGenerateItem (no blacklist row): " + tPair.material().mNameInternal);
                }
            }
        }
    }
}
