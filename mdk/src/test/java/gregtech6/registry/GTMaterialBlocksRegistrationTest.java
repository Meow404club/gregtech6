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
 * blockGem 217, blockDust 1096, blockIngot 483, blockPlate 673, blockPlateGem 203,
 * blockSolid 483 — total 3773 (prefix-material pairs after alias-merge + pair dedup, the
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
        "blockPlate", 673, "blockPlateGem", 203, "blockSolid", 483);
    private static final int PINNED_TOTAL = 3773;

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
}
