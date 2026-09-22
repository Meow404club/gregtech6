/**
 * Tests for task p8-prefixblock-registry: the material prefix BLOCK universe census.
 *
 * <p>Pins the upstream seven storage-block prefixes (Loader_PrefixBlocks.java:40-46:
 * blockRaw/blockGem/blockDust/blockIngot/blockPlate/blockPlateGem/blockSolid) with an
 * independent offline walk over MATERIAL_ARRAY x {@link OreDictPrefix#isGeneratingItem}
 * (port OreDictPrefix.java:285-287 = upstream :364-366, the item-side criterion; the block*
 * prefixes carry {@code setCondition(basePrefix)} chains, upstream OP.java:345-351, so a
 * block family mirrors its base prefix family). The pinned counts are the acceptance
 * yardstick for this card and for the render card (p8-prefixblock-render).
 *
 * <p>Measured 2026-08-31 (junit probe over GTMaterialItems.initMaterials): blockRaw 618,
 * blockGem 217, blockDust 1096, blockIngot 483, blockPlate 673, blockPlateGem 207,
 * blockSolid 483 — total 3777 (prefix-material pairs after alias-merge + pair dedup, the
 * same semantics as the production enumeration). Without pair dedup blockRaw walks 625
 * (7 alias-slot materials), which is why the census pins the deduped production walk.
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;

class GTMaterialBlocksRegistrationTest {

    /**
     * The upstream storage-block path spec, transcribed from Loader_PrefixBlocks.java:40-46
     * (file order = OP declaration order). Independent of any production list: if the two
     * ever drift, this test fails and forces a conscious decision.
     */
    private static final List<String> UPSTREAM_BLOCK_PATH = List.of(
        "blockRaw", "blockGem", "blockDust", "blockIngot", "blockPlate", "blockPlateGem", "blockSolid");

    /** The pinned census (2026-08-31, see class javadoc) — the card + render-card yardstick. */
    private static final Map<String, Integer> PINNED_CENSUS = Map.of(
        "blockRaw", 618, "blockGem", 217, "blockDust", 1096, "blockIngot", 483,
        "blockPlate", 673, "blockPlateGem", 207, "blockSolid", 483);
    private static final int PINNED_TOTAL = 3777;

    @BeforeAll
    static void initMaterialSystem() {
        // The single-class JVM probe lesson (class-load cycle): the material system must
        // exist before any OP/MT field is dereferenced (GT6RecipesCokeOvenTest @BeforeAll shape).
        GTMaterialItems.initMaterials();
    }

    /** Independent census walk: VALUES order x MATERIAL_ARRAY x isGeneratingItem, alias-merged, pair-deduped. */
    private static Map<String, Integer> censusByPrefix() {
        Map<String, Integer> rCounts = new LinkedHashMap<>();
        for (OreDictPrefix tPrefix : OreDictPrefix.VALUES) {
            if (!UPSTREAM_BLOCK_PATH.contains(tPrefix.mNameInternal)) continue;
            Set<OreDictMaterial> tSeen = new HashSet<>();
            for (OreDictMaterial tMaterial : MaterialRegistry.INSTANCE.MATERIAL_ARRAY) {
                if (tMaterial == null || tMaterial.mID < 0) continue;
                tMaterial = MaterialRegistry.INSTANCE.get(tMaterial); // alias merge onto the target
                if (tMaterial == null || tMaterial.mID < 0) continue;
                if (!tSeen.add(tMaterial)) continue;
                if (!tPrefix.isGeneratingItem(tMaterial)) continue; // OreDictPrefix.java:285-287 = upstream :364-366
                rCounts.merge(tPrefix.mNameInternal, 1, Integer::sum);
            }
        }
        return rCounts;
    }

    /** The kept materials of one prefix family (alias-merged, deduped), as internal names. */
    private static Set<String> keptMaterialNames(OreDictPrefix aPrefix) {
        Set<String> rNames = new HashSet<>();
        Set<OreDictMaterial> tSeen = new HashSet<>();
        for (OreDictMaterial tMaterial : MaterialRegistry.INSTANCE.MATERIAL_ARRAY) {
            if (tMaterial == null || tMaterial.mID < 0) continue;
            tMaterial = MaterialRegistry.INSTANCE.get(tMaterial);
            if (tMaterial == null || tMaterial.mID < 0) continue;
            if (!tSeen.add(tMaterial)) continue;
            if (!aPrefix.isGeneratingItem(tMaterial)) continue;
            rNames.add(tMaterial.mNameInternal);
        }
        return rNames;
    }

    /** The census: per-prefix exact counts, total, and the upstream prefix order. */
    @Test
    void censusIsPinned() {
        Map<String, Integer> tCensus = censusByPrefix();
        assertEquals(UPSTREAM_BLOCK_PATH, new ArrayList<>(tCensus.keySet()),
                "walk order must be the Loader_PrefixBlocks.java:40-46 spec order");
        for (String tPrefix : UPSTREAM_BLOCK_PATH) {
            assertEquals(PINNED_CENSUS.get(tPrefix), tCensus.get(tPrefix), tPrefix + " census count");
        }
        assertEquals(PINNED_TOTAL, tCensus.values().stream().mapToInt(Integer::intValue).sum(), "total census");
    }

    /** The cokeoven block rows are resolvable: every (prefix, material) pair of the 7 upstream rows is in the census. */
    @Test
    void cokeOvenBlockRowPairsAreInTheCensus() {
        List<OreDictPrefix> tPath = List.of(OP.blockRaw, OP.blockGem, OP.blockDust, OP.blockIngot,
                OP.blockPlate, OP.blockPlateGem, OP.blockSolid);
        assertTrue(tPath.contains(OP.blockSolid) && UPSTREAM_BLOCK_PATH.size() == tPath.size(),
                "the seven OP fields must be exactly the upstream storage path");
        // (prefix, kept-set) over materials — every input and output pair of Loader_Recipes_Other :787-789/:803-805/:815
        Map<OreDictPrefix, Set<String>> tKept = new LinkedHashMap<>();
        for (OreDictPrefix tPrefix : tPath) tKept.put(tPrefix, keptMaterialNames(tPrefix));
        assertTrue(tKept.get(OP.blockRaw).contains("Coal") && tKept.get(OP.blockRaw).contains("Lignite"), ":787/:803 blockRaw Coal/Lignite");
        assertTrue(tKept.get(OP.blockIngot).contains("Coal") && tKept.get(OP.blockIngot).contains("Lignite"), ":788/:804 blockIngot Coal/Lignite");
        assertTrue(tKept.get(OP.blockGem).contains("Coal") && tKept.get(OP.blockGem).contains("Lignite"), ":789/:805 blockGem Coal/Lignite");
        assertTrue(tKept.get(OP.blockDust).contains("OilShale"), ":815 blockDust OilShale (mNameInternal camel; the 'Oilshale' spelling is the mID -1 husk)");
        assertTrue(tKept.get(OP.blockIngot).contains("CoalCoke") && tKept.get(OP.blockIngot).contains("LigniteCoke"),
                "outputs blockIngot CoalCoke/LigniteCoke (:787/:788/:803/:804)");
        assertTrue(tKept.get(OP.blockGem).contains("CoalCoke") && tKept.get(OP.blockGem).contains("LigniteCoke"),
                "outputs blockGem CoalCoke/LigniteCoke (:789/:805)");
        assertTrue(keptMaterialNames(OP.dust).contains("Asphalt"), ":815 output dust Asphalt must exist in the dust family");
    }

    /**
     * The blockSolid condition chain (upstream OP.java:351 {@code setCondition(blockIngot)}):
     * {@code OreDictPrefix.isTrue} = {@code canGenerateItem} (port OreDictPrefix.java:349-351),
     * so {@code blockSolid.isGeneratingItem(m)} must equal {@code blockIngot.canGenerateItem(m)}
     * for every material — the recursion proof — and blockIngot carries no blacklist/forced
     * divergence of its own (measured 2026-08-31: gen == canGen for every material), making the
     * two families identical sets.
     */
    @Test
    void blockSolidConditionChainsToBlockIngot() {
        for (OreDictMaterial tMaterial : MaterialRegistry.INSTANCE.MATERIAL_ARRAY) {
            if (tMaterial == null || tMaterial.mID < 0) continue;
            tMaterial = MaterialRegistry.INSTANCE.get(tMaterial);
            if (tMaterial == null || tMaterial.mID < 0) continue;
            assertEquals(OP.blockIngot.canGenerateItem(tMaterial), OP.blockSolid.isGeneratingItem(tMaterial),
                    "the blockSolid condition must chain to blockIngot (OP.java:351)");
            assertEquals(OP.blockIngot.isGeneratingItem(tMaterial), OP.blockIngot.canGenerateItem(tMaterial),
                    "blockIngot must carry no blacklist/forced divergence");
        }
        assertEquals(keptMaterialNames(OP.blockIngot), keptMaterialNames(OP.blockSolid),
                "blockIngot and blockSolid families are the same material set");
    }

    /** The production block path must equal the upstream seven-name spec, in loader file order. */
    @org.junit.jupiter.api.Test
    void blockPathMatchesUpstreamSpec() {
        List<OreDictPrefix> tProduction = GTMaterialBlocks.blockPathPrefixes();
        List<String> tNames = new ArrayList<>();
        for (OreDictPrefix tPrefix : tProduction) tNames.add(tPrefix.mNameInternal);
        assertEquals(UPSTREAM_BLOCK_PATH, tNames, "block path must be the Loader_PrefixBlocks.java:40-46 spec, in file order");
        assertEquals(7, tProduction.size());
    }

    /** The production enumeration must reproduce the pinned census exactly (per prefix, total, order). */
    @org.junit.jupiter.api.Test
    void productionEnumerationMatchesCensus() {
        List<GTMaterialItems.PrefixMaterial> tOrder = GTMaterialBlocks.registrationOrder();
        assertEquals(PINNED_TOTAL, tOrder.size(), "production walk size = pinned census total");
        Map<String, Integer> tPerPrefix = new LinkedHashMap<>();
        for (GTMaterialItems.PrefixMaterial tPair : tOrder) tPerPrefix.merge(tPair.prefix().mNameInternal, 1, Integer::sum);
        for (String tPrefix : UPSTREAM_BLOCK_PATH) {
            assertEquals(PINNED_CENSUS.get(tPrefix), tPerPrefix.get(tPrefix), tPrefix + " production count");
        }
        // prefix order = OP.VALUES order projected on the block path = the loader spec order
        List<String> tOrderNames = new ArrayList<>();
        for (GTMaterialItems.PrefixMaterial tPair : tOrder) {
            if (!tOrderNames.contains(tPair.prefix().mNameInternal)) tOrderNames.add(tPair.prefix().mNameInternal);
        }
        assertEquals(UPSTREAM_BLOCK_PATH, tOrderNames, "production prefix order must follow the census walk");
    }

    /** The block universe has zero first-wins id drops (measured), and the enumeration bookkeeping says so. */
    @org.junit.jupiter.api.Test
    void blockUniverseHasZeroIdCollisions() {
        GTMaterialBlocks.Enumeration tSet = GTMaterialBlocks.enumerate();
        assertEquals(0, tSet.duplicateIdDrops(), "the block census measured zero id collisions");
        assertEquals(PINNED_TOTAL, tSet.kept().size());
    }

    /** All seven block prefixes are creative-visible (none HIDDEN, all non-empty) in loader order. */
    @org.junit.jupiter.api.Test
    void tabPrefixesAreTheSevenStorageFamilies() {
        List<String> tNames = new ArrayList<>();
        for (OreDictPrefix tPrefix : GTMaterialBlocks.tabPrefixes()) tNames.add(tPrefix.mNameInternal);
        assertEquals(UPSTREAM_BLOCK_PATH, tNames, "7 tabs, one per non-empty non-HIDDEN block prefix (PrefixBlockItem.java:65-67)");
    }

    /**
     * The get seam, per leg (GTOfflineTestBase javadoc :25-33 — the 21.1 test JVM boots through
     * FML itself, so registration has really fired there): on 1.20.1 offline the RegistryObject
     * index is never populated, so the seam returns null (the resolver fallback treats that as
     * absent); on 21.1 that premise is unreachable and the seam hands out the live deferred
     * holder — pinned as the positive proposition (constructed with its id, getId pinned to the
     * GTMaterialItems.itemIdOf composition rule).
     */
    @org.junit.jupiter.api.Test
    void getSeamIsNullBeforeRegistration() {
        //? if forge {
        org.junit.jupiter.api.Assertions.assertNull(GTMaterialBlocks.get(OP.blockIngot, MT.Coal),
                "no RegisterEvent has fired offline — the seam must be null, not a dangling handle");
        //?} else {
        /*var tHandle = GTMaterialBlocks.get(OP.blockIngot, MT.Coal);
        org.junit.jupiter.api.Assertions.assertNotNull(tHandle, "the FML-booted 21.1 JVM registered for real — the seam returns the live deferred holder, not null");
        org.junit.jupiter.api.Assertions.assertEquals(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6", "block_ingot_coal"), tHandle.getId(),
                "the deferred holder id follows the GTMaterialItems.itemIdOf rule (gt6:block_ingot_coal)");
        *///?}
    }
}
