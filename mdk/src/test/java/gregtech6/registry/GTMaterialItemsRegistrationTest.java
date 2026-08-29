/**
 * Tests for task p3-fullprefix-creativetab: the full-prefix registration expansion.
 *
 * <p>Pins the upstream item-path universe (Loader_Items.java:57-171, 105 prefixes) as a name
 * spec independent of the production list, recomputes the registration set with an independent
 * walk (acceptance: independent recount), pins the phase-2 per-prefix counts as regression
 * anchors, and exercises the first-wins id collision rule on the compressed/Compressed pair.
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;

public class GTMaterialItemsRegistrationTest {

    /**
     * The upstream item-path spec, transcribed from Loader_Items.java:57-171 (every OP prefix
     * upstream constructs a PrefixItem for, in file order). Independent of the production list:
     * if the two ever drift, this test fails and forces a conscious decision.
     */
    private static final List<String> UPSTREAM_ITEM_PATH = List.of(
        "dust", "dustSmall", "dustTiny", "dustDiv72", "dustImpure",
        "crushed", "crushedTiny", "crushedPurified", "crushedPurifiedTiny", "crushedCentrifuged", "crushedCentrifugedTiny",
        "gemChipped", "gemFlawed", "gem", "gemFlawless", "gemExquisite", "gemLegendary", "bouleGt",
        "nugget", "chunkGt", "billet", "ingot", "ingotHot", "ingotDouble", "ingotTriple", "ingotQuadruple", "ingotQuintuple",
        "plateGemTiny", "plateGem", "plateTiny", "plate", "plateDouble", "plateTriple", "plateQuadruple", "plateQuintuple", "plateDense", "plateCurved",
        "scrapGt", "rockGt", "oreRaw",
        "gearGtSmall", "gearGt", "rotor", "stick", "stickLong", "springSmall", "spring",
        "lens", "round", "bolt", "screw", "ring", "chain", "foil", "casingSmall", "wireFine", "minecartWheels", "railGt",
        "plantGtBerry", "plantGtBlossom", "plantGtFiber", "plantGtTwig", "plantGtWart", "chemtube",
        "toolHeadRawSword", "toolHeadSword", "toolHeadRawPickaxe", "toolHeadPickaxe", "toolHeadPickaxeGem",
        "toolHeadConstructionPickaxe", "toolHeadBuilderwand", "toolHeadRawShovel", "toolHeadShovel",
        "toolHeadRawSpade", "toolHeadSpade", "toolHeadRawAxe", "toolHeadAxe", "toolHeadRawAxeDouble",
        "toolHeadAxeDouble", "toolHeadRawHoe", "toolHeadHoe", "toolHeadHammer", "toolHeadFile",
        "toolHeadRawChisel", "toolHeadChisel", "toolHeadRawSaw", "toolHeadSaw", "toolHeadDrill",
        "toolHeadChainsaw", "toolHeadWrench", "toolHeadScrewdriver", "toolHeadRawUniversalSpade",
        "toolHeadUniversalSpade", "toolHeadRawSense", "toolHeadSense", "toolHeadRawPlow", "toolHeadPlow",
        "toolHeadBuzzSaw", "toolHeadRawArrow", "toolHeadArrow",
        "arrowGtWood", "arrowGtPlastic", "bulletGtSmall", "bulletGtMedium", "bulletGtLarge");

    @BeforeAll
    public static void initMaterialSystem() {
        GTMaterialItems.initMaterials();
    }

    @Test
    public void itemPathMatchesUpstreamSpec() {
        List<OreDictPrefix> tProduction = GTMaterialItems.itemPathPrefixes();
        Set<String> tProductionNames = new TreeSet<>();
        for (OreDictPrefix tPrefix : tProduction) tProductionNames.add(tPrefix.mNameInternal);
        assertEquals(new TreeSet<>(UPSTREAM_ITEM_PATH), tProductionNames, "production item path must equal the upstream Loader_Items.java:57-171 spec");
        assertEquals(105, tProduction.size(), "upstream constructs exactly 105 PrefixItems");
    }

    @Test
    public void independentRecountMatchesRegistrationOrder() {
        // Independent walk: own code, same semantics (VALUES order, alias merge, isGeneratingItem,
        // pair dedup, first-wins by id) — recomputed from the name spec, not from production helpers.
        Set<String> tSpec = new HashSet<>(UPSTREAM_ITEM_PATH);
        Map<String, Integer> tPerPrefix = new LinkedHashMap<>();
        Set<String> tSeenIds = new HashSet<>();
        int tDrops = 0;
        for (OreDictPrefix tPrefix : OreDictPrefix.VALUES) {
            if (!tSpec.contains(tPrefix.mNameInternal)) continue;
            Set<OreDictMaterial> tSeenMaterials = new HashSet<>();
            for (OreDictMaterial tMaterial : MaterialRegistry.INSTANCE.MATERIAL_ARRAY) {
                if (tMaterial == null || tMaterial.mID < 0) continue;
                tMaterial = MaterialRegistry.INSTANCE.get(tMaterial);
                if (tMaterial == null || tMaterial.mID < 0) continue;
                if (!tSeenMaterials.add(tMaterial)) continue;
                if (!tPrefix.isGeneratingItem(tMaterial)) continue;
                String tId = GTMaterialItems.itemIdOf(tPrefix, tMaterial);
                if (!tSeenIds.add(tId)) {tDrops++; continue;} // first-wins
                tPerPrefix.merge(tPrefix.mNameInternal, 1, Integer::sum);
            }
        }
        List<GTMaterialItems.PrefixMaterial> tOrder = GTMaterialItems.registrationOrder();
        assertEquals(tPerPrefix.values().stream().mapToInt(Integer::intValue).sum(), tOrder.size(), "independent recount must match the enumeration size");
        assertEquals(0, tDrops, "the item universe census has zero id collisions");
        // per-prefix totals must agree between the two walks
        Map<String, Integer> tProductionPerPrefix = new LinkedHashMap<>();
        for (GTMaterialItems.PrefixMaterial tPair : tOrder) tProductionPerPrefix.merge(tPair.prefix().mNameInternal, 1, Integer::sum);
        assertEquals(tPerPrefix, tProductionPerPrefix);
    }

    @Test
    public void phaseTwoPrefixCountsAreRegressionAnchors() {
        Map<String, Integer> tCounts = new LinkedHashMap<>();
        for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) tCounts.merge(tPair.prefix().mNameInternal, 1, Integer::sum);
        assertEquals(483, tCounts.get("ingot"), "phase-2 registered 483 ingots (p2-registration-bridge handoff)");
        assertEquals(1096, tCounts.get("dust"), "phase-2 registered 1096 dusts");
        assertEquals(217, tCounts.get("gem"), "phase-2 registered 217 gems");
        assertEquals(673, tCounts.get("plate"), "phase-2 registered 673 plates");
    }

    @Test
    public void registrationIdsAreUnique() {
        Set<String> tIds = new HashSet<>();
        for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
            assertTrue(tIds.add(GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material())), "duplicate id " + tIds);
        }
    }

    @Test
    public void snakeCaseAlgorithmIsPinned() {
        // Edge cases: camel boundaries, digits, acronym runs — keeps the registration-side
        // algorithm identical to MaterialPrefixItem.snakeCase (the item-class copy serves the name keys).
        assertEquals("ingot_iron", GTMaterialItems.snakeCase("ingotIron"));
        assertEquals("wire_gt01", GTMaterialItems.snakeCase("wireGt01"));
        assertEquals("tool_head_raw_universal_spade", GTMaterialItems.snakeCase("toolHeadRawUniversalSpade"));
        assertEquals("compressed", GTMaterialItems.snakeCase("Compressed"));
        assertEquals("compressed", GTMaterialItems.snakeCase("compressed"));
    }

    @Test
    public void firstWinsIdCollisionRule() {
        // The only snake_case collision pair in all of OP: "compressed" (OP.compressed, condition PLATES)
        // and "Compressed" (identical-name alias of compressed via addIdenticalNames, PREFIX_UNUSED).
        OreDictPrefix tAlias = null;
        for (OreDictPrefix tPrefix : OreDictPrefix.VALUES) {
            if ("Compressed".equals(tPrefix.mNameInternal)) tAlias = tPrefix;
        }
        OreDictPrefix tCompressed = OP.compressed;
        assertTrue(tCompressed != null && tAlias != null, "both collision parties must exist in OP");
        assertEquals(GTMaterialItems.itemIdOf(tCompressed, MT.Iron), GTMaterialItems.itemIdOf(tAlias, MT.Iron), "compressed/Compressed must snake-collide");
        assertFalse(Arrays.asList(GTMaterialItems.itemPathPrefixes()).contains(tCompressed), "census: compressed is outside the item universe");
        assertFalse(Arrays.asList(GTMaterialItems.itemPathPrefixes()).contains(tAlias), "census: Compressed alias is outside the item universe");
        // first-wins: the earlier pair in iteration order keeps the id
        List<GTMaterialItems.PrefixMaterial> tPairs = List.of(
            new GTMaterialItems.PrefixMaterial(tCompressed, MT.Iron),
            new GTMaterialItems.PrefixMaterial(tAlias, MT.Iron));
        GTMaterialItems.FirstWins tResult = GTMaterialItems.firstWinsById(new ArrayList<>(tPairs));
        assertEquals(1, tResult.drops());
        assertEquals(1, tResult.kept().size());
        assertSame(tCompressed, tResult.kept().get(0).prefix(), "first-wins keeps the earlier prefix");
    }

    @Test
    public void hiddenPrefixesRegisterItemsButGetNoTab() {
        Set<OreDictPrefix> tTabs = new HashSet<>(GTMaterialItems.tabPrefixes());
        for (OreDictPrefix tPrefix : GTMaterialItems.tabPrefixes()) {
            assertFalse(tPrefix.contains(TD.Creative.HIDDEN), "no HIDDEN prefix may own a tab: " + tPrefix.mNameInternal);
        }
        List<String> tHiddenOnItemPath = List.of("ingotHot", "scrapGt", "plantGtBerry", "plantGtBlossom", "plantGtFiber", "plantGtTwig", "plantGtWart");
        Set<String> tTabNames = new HashSet<>();
        for (OreDictPrefix tPrefix : tTabs) tTabNames.add(tPrefix.mNameInternal);
        for (String tName : tHiddenOnItemPath) {
            assertFalse(tTabNames.contains(tName), tName + " is TD.Creative.HIDDEN and must have no tab");
            assertTrue(tCounts().containsKey(tName), tName + " still registers items (upstream PrefixItem.java:114 leaves them invisible)");
        }
    }

    @Test
    public void tabSetMatchesIndependentRecount() {
        Set<String> tSpec = new HashSet<>(UPSTREAM_ITEM_PATH);
        Map<String, Integer> tCounts = tCounts();
        Set<String> tExpected = new HashSet<>();
        for (OreDictPrefix tPrefix : OreDictPrefix.VALUES) {
            if (!tSpec.contains(tPrefix.mNameInternal)) continue;
            if (tPrefix.contains(TD.Creative.HIDDEN)) continue;
            Integer tCount = tCounts.get(tPrefix.mNameInternal);
            if (tCount == null || tCount == 0) continue; // empty families get no tab
            tExpected.add(tPrefix.mNameInternal);
        }
        Set<String> tActual = new HashSet<>();
        for (OreDictPrefix tPrefix : GTMaterialItems.tabPrefixes()) tActual.add(tPrefix.mNameInternal);
        assertEquals(tExpected, tActual, "tab set = item path minus HIDDEN minus empty families");
    }

    private static Map<String, Integer> tCounts() {
        Map<String, Integer> rCounts = new LinkedHashMap<>();
        for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) rCounts.merge(tPair.prefix().mNameInternal, 1, Integer::sum);
        return rCounts;
    }
}
