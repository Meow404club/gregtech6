/**
 * Tests for task p34-machines-bumblelyzer-crucible: the GTMaterialItems force-table slice —
 * the upstream OP.java:616/:619/:624 forceItemGeneration rows the port OP defers with the
 * MT/ANY-dependent block, landed mdk-side (the coordinator ruling C: the minimal boule
 * registration takes the 39-crystallisation-row output material set as the real count).
 *
 * <p>Pins: the eleven bouleGt items (the quartet + the seven sapphires), the plateTiny
 * Paper item (the Bumblelyzer scan leg), the KNOWN plateGem cascade (the Or(gem, bouleGt)
 * condition face — upstream-faithful, the Crystalline Silicon/Germanium/Alloy plates), and
 * the negative face (no boule items beyond the forced set).
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;

public class GTMaterialItemsForceTest {

    /** The eleven crystallisation-row output materials (Loader_Recipes_Other.java:683-706, the ruling's 实数 form). */
    private static final String[] BOULE_MATERIALS = {
        "Silicon", "Germanium", "RedstoneAlloy", "NikolineAlloy",
        "Sapphire", "BlueSapphire", "GreenSapphire", "YellowSapphire", "OrangeSapphire", "PurpleSapphire", "Ruby"};

    @BeforeAll
    public static void initMaterialSystem() {
        GTMaterialItems.initMaterials(); // the force table rides initMaterials (the C2 seam)
    }

    private static boolean generates(OreDictPrefix aPrefix, String aMaterialField) {
        OreDictMaterial tMaterial = MaterialLookup.byField(aMaterialField);
        return tMaterial != null && aPrefix.isGeneratingItem(tMaterial);
    }

    /** Minimal reflective-free lookup: the MT fields the pin names, resolved through the live registry by internal name. */
    private static final class MaterialLookup {
        static OreDictMaterial byField(String aInternalName) {
            for (OreDictMaterial tMaterial : gregapi.oredict.MaterialRegistry.INSTANCE.MATERIAL_ARRAY) {
                if (tMaterial != null && tMaterial.mID >= 0 && tMaterial.mNameInternal.equals(aInternalName)) return tMaterial;
            }
            return null;
        }
    }

    @Test
    public void theElevenBouleMaterialsGenerate() {
        for (String tName : BOULE_MATERIALS) {
            assertTrue(generates(OP.bouleGt, tName), "bouleGt." + tName + " generates (the forced row-output set)");
        }
    }

    @Test
    public void paperTinyGenerates() {
        assertTrue(generates(OP.plateTiny, "Paper"), "plateTiny.Paper generates (the upstream :619 force, the scan leg)");
    }

    @Test
    public void thePlateGemCascadeIsTheKnownUpstreamFaithfulFace() {
        // plateGem's condition is Or(gem, bouleGt) && PLATES — the forced boule materials with
        // tool stats yield the Crystalline plates upstream carries (the sapphires already
        // generate gem, so the DELTA is the quartet)
        for (String tName : new String[] {"Silicon", "Germanium", "RedstoneAlloy", "NikolineAlloy"}) {
            assertTrue(generates(OP.plateGem, tName), "plateGem." + tName + " — the Or(gem, bouleGt) cascade face");
        }
    }

    @Test
    public void noBouleItemsBeyondTheForcedSet() {
        Set<String> tForced = new HashSet<>(java.util.Arrays.asList(BOULE_MATERIALS));
        for (OreDictMaterial tMaterial : gregapi.oredict.MaterialRegistry.INSTANCE.MATERIAL_ARRAY) {
            if (tMaterial == null || tMaterial.mID < 0) continue;
            if (!tForced.contains(tMaterial.mNameInternal)) {
                assertFalse(OP.bouleGt.isGeneratingItem(tMaterial), "bouleGt." + tMaterial.mNameInternal + " must NOT generate (no row consumes it)");
            }
        }
    }
}
