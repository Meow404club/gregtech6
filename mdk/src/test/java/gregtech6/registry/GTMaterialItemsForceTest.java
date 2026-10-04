/**
 * Tests for task machines-bumblelyzer-crucible: the GTMaterialItems force-table slice —
 * the upstream OP.java:616/:619/:624 forceItemGeneration rows the port OP defers with the
 * MT/ANY-dependent block, landed mdk-side (the coordinator ruling C: the minimal boule
 * registration takes the 39-crystallisation-row output material set as the real count).
 *
 * <p>Pins: the eleven bouleGt items (the quartet + the seven sapphires), the plateTiny
 * Paper item (the Bumblelyzer scan leg), the KNOWN plateGem cascade (the Or(gem, bouleGt)
 * condition face — upstream-faithful, the Crystalline Silicon/Germanium/Alloy plates), and
 * the negative face (no boule items beyond the forced set).
 *
 * <p>Task dye-item-axis extension: the dye item axis — MT.DATA.Dye_Materials (upstream
 * MT.java:3687, the 16 vanilla-index dye materials) and the OP.dust×16 + OP.plantGtFiber×16
 * item face (the b4 recipe band's prerequisite). EMPIRICAL NOTE (the probe this card ran
 * before landing the rows): the 32 items are ALREADY enumerated without any forcing — the
 * dye() factory routes through dust() which stamps put(G_DUST, MORTAR) (MT.java:528 =
 * upstream MT.java:164 verbatim) and TD.java:587 G_DUST = {DUSTS, PLANTS} satisfies both
 * the dust gate Or(DUSTS, DIRTY_DUSTS) (OP.java:1199) and the fiber gate PLANTS
 * (OP.java:1417). Upstream is the same (PrefixItem.run generates on the condition face; no
 * mat() lazy path involved — the research Q2 premise was falsified by the probe). The two
 * force rows are therefore belt-and-suspenders: they pin the axis against future condition
 * refactors at the established seam (the bouleGt/plateTiny precedent), and the forced-set
 * reflection pins below are what verifies they actually fired.
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;

public class GTMaterialItemsForceTest {

    /** The eleven crystallisation-row output materials (Loader_Recipes_Other.java:683-706, the ruling's 实数 form). */
    private static final String[] BOULE_MATERIALS = {
        "Silicon", "Germanium", "RedstoneAlloy", "NikolineAlloy",
        "Sapphire", "BlueSapphire", "GreenSapphire", "YellowSapphire", "OrangeSapphire", "PurpleSapphire", "Ruby"};

    /** The dye axis in vanilla dye-index order (upstream MT.java:3687 DATA.Dye_Materials verbatim). */
    private static final String[] DYE_MATERIALS = {
        "Black", "Red", "Green", "Brown", "Blue", "Purple", "Cyan", "LightGray",
        "Gray", "Pink", "Lime", "Yellow", "LightBlue", "Magenta", "Orange", "White"};

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

    // -----------------------------------------------------------------------------------------------------------------
    // Task dye-item-axis: the dye item axis (MT.DATA.Dye_Materials + the 32-item OP.dust/plantGtFiber face).
    // -----------------------------------------------------------------------------------------------------------------

    /** The forced set behind OreDictPrefix.forceItemGeneration (private, hence the reflection pin). */
    @SuppressWarnings("unchecked")
    private static Set<OreDictMaterial> forcedSet(OreDictPrefix aPrefix) throws Exception {
        Field tField = OreDictPrefix.class.getDeclaredField("mItemGeneratorForced");
        tField.setAccessible(true);
        return (Set<OreDictMaterial>)tField.get(aPrefix);
    }

    /** MT.DATA.Dye_Materials exists and holds the 16 dyes in vanilla dye-index order (upstream MT.java:3687). */
    @Test
    public void theDyeMaterialsArrayIsTheVanillaIndexAxis() throws Exception {
        Field tField = MT.DATA.class.getField("Dye_Materials"); // red until the DATA row lands
        OreDictMaterial[] tAxis = (OreDictMaterial[])tField.get(null);
        assertNotNull(tAxis, "MT.DATA.Dye_Materials must be bound by MT.init (the FLUX_T late-binding face)");
        assertEquals(16, tAxis.length, "the dye axis is the 16 vanilla dye materials");
        for (int i = 0; i < 16; i++) {
            assertNotNull(tAxis[i], "Dye_Materials[" + i + "] bound");
            assertEquals(DYE_MATERIALS[i], tAxis[i].mNameInternal, "Dye_Materials[" + i + "] in vanilla dye-index order (upstream MT.java:3687)");
            assertEquals(8250 + i, tAxis[i].mID, "dye index " + i + " is material id 8250+" + i);
        }
    }

    /** The force rows fired: both prefixes hold all 16 dyes in their forced set (the two mdk-side rows). */
    @Test
    public void theDyeForceRowsFired() throws Exception {
        for (String tName : DYE_MATERIALS) {
            OreDictMaterial tMaterial = MaterialLookup.byField(tName);
            assertNotNull(tMaterial, tName + " resolves");
            assertTrue(forcedSet(OP.dust).contains(tMaterial), "dust force-row holds " + tName);
            assertTrue(forcedSet(OP.plantGtFiber).contains(tMaterial), "plantGtFiber force-row holds " + tName);
        }
    }

    /** The 32-item face: every dye carries a dust AND a plantGtFiber pair in the enumeration (the single source). */
    @Test
    public void allThirtyTwoDyeAxisItemsAreEnumerated() {
        Set<String> tOrder = new HashSet<>();
        for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
            tOrder.add(tPair.prefix().mNameInternal + "/" + tPair.material().mNameInternal);
        }
        for (String tName : DYE_MATERIALS) {
            assertTrue(tOrder.contains("dust/" + tName), "dust/" + tName + " enumerated (the b4 dust leg)");
            assertTrue(tOrder.contains("plantGtFiber/" + tName), "plantGtFiber/" + tName + " enumerated (the b4 fiber leg)");
        }
    }

    /**
     * The gate semantics (the card's 门语义 pins): dust gate = Or(DUSTS, DIRTY_DUSTS) satisfied
     * via the ITEMGENERATOR.DUSTS tag, fiber gate = PLANTS via ITEMGENERATOR.PLANTS — both
     * stamped by the dust() factory's put(G_DUST, MORTAR) — and the dye() = dust(SET_FOOD)
     * face (the FOOD iconset, no extra flags).
     */
    @Test
    public void theDyeGatesPassThroughTheG_DUSTStamp() {
        for (String tName : DYE_MATERIALS) {
            OreDictMaterial tMaterial = MaterialLookup.byField(tName);
            assertNotNull(tMaterial, tName + " resolves");
            assertTrue(tMaterial.contains(TD.ItemGenerator.DUSTS), tName + " carries ITEMGENERATOR.DUSTS (the dust() G_DUST stamp, MT.java:528)");
            assertTrue(tMaterial.contains(TD.ItemGenerator.PLANTS), tName + " carries ITEMGENERATOR.PLANTS (the dust() G_DUST stamp, TD.java:587)");
            assertTrue(OP.dust.isGeneratingItem(tMaterial), "dust gate Or(DUSTS, DIRTY_DUSTS) passes for " + tName + " (OP.java:1199)");
            assertTrue(OP.plantGtFiber.isGeneratingItem(tMaterial), "fiber gate PLANTS passes for " + tName + " (OP.java:1417)");
            // The SET_FOOD face pinned at the source (mTextureSetsItems[0]); GT6ItemModels.iconsetOf
            // reads exactly this list and would drag vanilla registry statics into the plain JVM.
            assertEquals("FOOD", tMaterial.mTextureSetsItems.get(0), tName + " item set is FOOD (dye() = dust(SET_FOOD), MT.java:564)");
        }
        List<GTMaterialItems.PrefixMaterial> tOrder = GTMaterialItems.registrationOrder();
        long tDustDyes = tOrder.stream().filter(p -> p.prefix() == OP.dust && p.material().mID >= 8250 && p.material().mID <= 8265).count();
        long tFiberDyes = tOrder.stream().filter(p -> p.prefix() == OP.plantGtFiber && p.material().mID >= 8250 && p.material().mID <= 8265).count();
        assertEquals(16, tDustDyes, "exactly 16 dye dusts (no PLANTS-domain flood beyond the dyes)");
        assertEquals(16, tFiberDyes, "exactly 16 dye fibers (the plantGtFiber walk stays dye-bounded)");
    }
}
