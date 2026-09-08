/**
 * Offline JSON-snapshot tests for task p24-tags-provider-skeleton: the first tag batch is
 * pinned against the committed generated tree so a future provider refactor cannot drift
 * the product silently. The block face — mineable/pickaxe over stone272 + the whole
 * {@link GTMachines} register + the metal/gem/raw-ore prefix blocks (blockDust excluded,
 * the P1 shovel band), mineable/axe over exactly the wood barrel; the item face — the
 * GTCEu {@code defaultTagPath} families (TagPrefix.java:279/:290/:405/:416/:729/:223)
 * sampled per material with EXACT expected member lists recomputed from the same
 * registration walks the provider uses (the GT6StoneBlocksRenderDatagenTest mirror
 * discipline: enumeration side walks the offline order, generated side is asserted from
 * the classpath tree; the write side is gated by runData: first run written>0, second
 * written:0).
 *
 * <p>Strictness pin: zero {@code optional()} members anywhere — a TagEntry with
 * {@code required:false} serializes as a JSON object, so "values contain only strings"
 * is the machine-checkable form of the TagsProvider.java:85-94 throw contract
 * ("Couldn't define tag %s as it is missing following references"): every reference in
 * the batch resolves against the live registry or runData fails loudly.
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;

import gregapi.data.OP;
import gregapi.oredict.OreDictPrefix;
import gregtech6.registry.GTMaterialBlocks;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTStoneBlocks;

class GT6TagsDatagenTest {

    /**
     * The pickaxe-file member total: 272 stone pairs + 25 machine blocks (oven +
     * shredder/crusher/lathe ladders + 4 dryers + 4 distilleries + the 4 Canner rows —
     * GTMachines.java:58/:118-151/:233-237/:347-351/:328-359, the whole-class
     * {@code GTMachines.BLOCKS} walk means every landed machine row joins the band) +
     * the 1 conscious multiblock join (the Lightning Rod pillar block, task
     * p24-lightning-rod — the wall/coil/controller follow the multiblock-family
     * non-membership convention, the provider javadoc) +
     * the 6 metal/gem/raw prefixes' live pairs (3773 - 1096 blockDust = 2677). Pinned so
     * any band change is a conscious constant update — the maintenance duty the tags
     * card declared for every future machine card (this update rides p24-lightning-rod,
     * the four Canner rows 2970 → 2974 then the rod block 2974 → 2975).
     */
    private static final int PINNED_PICKAXE_TOTAL = 272 + 25 + 1 + 2677;

    /** The 13 tier-ladder machine ids of the first machines card (the dryer/distillery rows ride the total pin). */
    private static final List<String> PINNED_LADDER_MACHINES = List.of(
            "gt6:oven",
            "gt6:shredder", "gt6:shredder_t2", "gt6:shredder_t3", "gt6:shredder_t4",
            "gt6:crusher", "gt6:crusher_t2", "gt6:crusher_t3", "gt6:crusher_t4",
            "gt6:lathe", "gt6:lathe_t2", "gt6:lathe_t3", "gt6:lathe_t4");

    /** The pickaxe-band prefixes — the GT6BlockTags band set mirrored (blockDust excluded). */
    private static final Set<OreDictPrefix> PICKAXE_BLOCK_PREFIXES = Set.of(OP.blockRaw, OP.blockGem,
            OP.blockIngot, OP.blockPlate, OP.blockPlateGem, OP.blockSolid);

    /** The item-path family walks, computed once (mirror of the provider's two walks). */
    private static List<GTMaterialItems.PrefixMaterial> gItemWalk;
    private static List<GTMaterialItems.PrefixMaterial> gStorageWalk;

    @BeforeAll
    static void initMaterialSystem() {
        // the GT6PrefixBlockRenderDatagenTest recipe: bootStrap keeps the headless JVM safe
        // through the provider chain class-load, then the material system fills the walks
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
        }
        GTMaterialItems.initMaterials();
        gItemWalk = GTMaterialItems.registrationOrder();
        gStorageWalk = GTMaterialBlocks.registrationOrder();
    }

    // ------------------------------------------------------------------ the block face

    /** One generated tag JSON's values array (classpath face of the committed tree). */
    private static List<String> tagValues(String aDataPath) throws Exception {
        try (InputStream tStream = GT6TagsDatagenTest.class.getClassLoader().getResourceAsStream("data/" + aDataPath)) {
            assertNotNull(tStream, "the generated tag must be on the classpath: " + aDataPath);
            var tArray = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonArray("values");
            List<String> rValues = new ArrayList<>();
            for (var tEntry : tArray) {
                assertTrue(tEntry.isJsonPrimitive(),
                        "zero optional members — a required:false TagEntry serializes as an object: " + aDataPath);
                rValues.add(tEntry.getAsString());
            }
            return rValues;
        }
    }

    /** The strictness face of the acceptance: no member is optional in either mining tag. */
    @Test
    void noOptionalEntriesInTheMiningTags() throws Exception {
        tagValues("minecraft/tags/blocks/mineable/pickaxe.json"); // the reader itself asserts primitive-only
        tagValues("minecraft/tags/blocks/mineable/axe.json");
    }

    /** The pickaxe band: stone272 full, the ladder machines, the 6-prefix walk — exact total, all gt6. */
    @Test
    void pickaxeBandCoversTheMiningUniverse() throws Exception {
        List<String> tValues = tagValues("minecraft/tags/blocks/mineable/pickaxe.json");
        Set<String> tMembers = Set.copyOf(tValues);
        // stone272, all pairs
        int tStones = 0;
        for (GTStoneBlocks.VariantKey tKey : GTStoneBlocks.registrationOrder()) {
            assertTrue(tMembers.contains("gt6:" + GTStoneBlocks.path(tKey.stone().snake(), tKey.variant())),
                    "stone pair must ride the pickaxe band: " + tKey);
            tStones++;
        }
        assertEquals(272, tStones, "the 17x16 stone census");
        // the machine ladder (the whole-class register rides the total pin below)
        for (String tMachine : PINNED_LADDER_MACHINES) {
            assertTrue(tMembers.contains(tMachine), "machine must ride the pickaxe band: " + tMachine);
        }
        // the metal/gem/raw prefix walk, member-exact
        int tPrefixPairs = 0;
        for (GTMaterialItems.PrefixMaterial tPair : gStorageWalk) {
            if (!PICKAXE_BLOCK_PREFIXES.contains(tPair.prefix())) continue;
            assertTrue(tMembers.contains("gt6:" + GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material())),
                    "prefix block must ride the pickaxe band: " + tPair);
            tPrefixPairs++;
        }
        assertEquals(2677, tPrefixPairs, "3773 storage pairs - 1096 blockDust pairs");
        // exact product shape: no member outside the three census families
        assertEquals(PINNED_PICKAXE_TOTAL, tValues.size(), "272 stones + 21 machines + 2677 prefix blocks");
        assertTrue(tValues.stream().allMatch(v -> v.startsWith("gt6:")), "mod-face-only members");
    }

    /** The exclusions: blockDust is the P1 shovel band, the wood barrel rides axe, not pickaxe. */
    @Test
    void pickaxeBandExcludesTheDustPrefixAndTheBarrel() throws Exception {
        List<String> tValues = tagValues("minecraft/tags/blocks/mineable/pickaxe.json");
        assertTrue(tValues.stream().noneMatch(v -> v.startsWith("gt6:block_dust_")),
                "blockDust is the P1 shovel band, not this batch");
        assertTrue(!tValues.contains("gt6:barrel_wood"), "the wood barrel belongs to the axe band");
    }

    /** The axe band: exactly the wood fluid barrel (first batch — plastic/metal rows are P2). */
    @Test
    void axeBandIsExactlyTheWoodBarrel() throws Exception {
        assertEquals(List.of("gt6:barrel_wood"), tagValues("minecraft/tags/blocks/mineable/axe.json"));
    }

    // ------------------------------------------------------------------ the item face

    /** The walk-expected member list of one family/material face (empty = the tag must NOT exist). */
    private static List<String> expectedMembers(Set<OreDictPrefix> aPrefixes, String aMaterialSnake,
            List<GTMaterialItems.PrefixMaterial> aWalk) {
        List<String> rMembers = new ArrayList<>();
        for (GTMaterialItems.PrefixMaterial tPair : aWalk) {
            if (!aPrefixes.contains(tPair.prefix())) continue;
            if (!GTMaterialItems.snakeCase(tPair.material().mNameInternal).equals(aMaterialSnake)) continue;
            rMembers.add("gt6:" + GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
        }
        return rMembers;
    }

    /** One family/material face asserted EXACT against the walk-computed members (order included). */
    private static void assertFamilyFace(String aFamilyPath, Set<OreDictPrefix> aPrefixes, String aMaterialSnake,
            List<GTMaterialItems.PrefixMaterial> aWalk) throws Exception {
        // ALWAYS the canonical tracked-tree face ("forge" — the forge-leg runData --output
        // both legs' classpaths share). The 21.1 leg's own product lives under data/c/ in
        // its NODE-LOCAL runData output (not on any classpath) and is gated 1:1 against
        // this tree by the datagen_tree_check SEGMENT_MAP c-to-forge band; the namespace
        // constant itself is pinned by namespaceSplitIsTheLegFork.
        String tPath = "forge/tags/items/" + aFamilyPath.formatted(aMaterialSnake) + ".json";
        List<String> tExpected = expectedMembers(aPrefixes, aMaterialSnake, aWalk);
        try (InputStream tStream = GT6TagsDatagenTest.class.getClassLoader().getResourceAsStream("data/" + tPath)) {
        if (tExpected.isEmpty()) {
            assertNull(tStream, "no empty tag files may ship: " + tPath);
            return;
        }
            assertNotNull(tStream, "the family face must exist: " + tPath);
            List<String> tActual = tagValues(tPath);
            assertEquals(tExpected, tActual, "exact snapshot of " + tPath);
        }
    }

    /** The item families over sample materials — ingots/nuggets/dusts/gems, exact per the GTCEu paths. */
    @Test
    void itemFamiliesFollowTheGtceuPaths() throws Exception {
        assertFamilyFace(GT6ItemTags.INGOTS_FAMILY, Set.of(OP.ingot), "iron", gItemWalk);
        assertFamilyFace(GT6ItemTags.INGOTS_FAMILY, Set.of(OP.ingot), "copper", gItemWalk);
        assertFamilyFace(GT6ItemTags.INGOTS_FAMILY, Set.of(OP.ingot), "gold", gItemWalk);
        assertFamilyFace(GT6ItemTags.NUGGETS_FAMILY, Set.of(OP.nugget), "iron", gItemWalk);
        assertFamilyFace(GT6ItemTags.NUGGETS_FAMILY, Set.of(OP.nugget), "gold", gItemWalk);
        assertFamilyFace(GT6ItemTags.DUSTS_FAMILY, Set.of(OP.dust), "redstone", gItemWalk);
        assertFamilyFace(GT6ItemTags.GEMS_FAMILY, Set.of(OP.gem), "diamond", gItemWalk);
        assertFamilyFace(GT6ItemTags.GEMS_FAMILY, Set.of(OP.gem), "coal", gItemWalk);
    }

    /** The storage-block union: blockIngot/blockGem/blockDust share storage_blocks/%s, blockRaw rides raw_%s. */
    @Test
    void storageBlockFacesAreThePrefixUnions() throws Exception {
        Set<OreDictPrefix> tStorage = Set.of(OP.blockIngot, OP.blockGem, OP.blockDust);
        assertFamilyFace(GT6ItemTags.STORAGE_BLOCKS_FAMILY, tStorage, "iron", gStorageWalk);
        assertFamilyFace(GT6ItemTags.STORAGE_BLOCKS_FAMILY, tStorage, "diamond", gStorageWalk);
        assertFamilyFace(GT6ItemTags.STORAGE_BLOCKS_FAMILY, tStorage, "redstone", gStorageWalk);
        assertFamilyFace(GT6ItemTags.STORAGE_BLOCKS_RAW_FAMILY, Set.of(OP.blockRaw), "iron", gStorageWalk);
        assertFamilyFace(GT6ItemTags.STORAGE_BLOCKS_RAW_FAMILY, Set.of(OP.blockRaw), "copper", gStorageWalk);
    }

    /**
     * The namespace split pin: the family faces above read the CANONICAL tracked tree
     * (the forge-leg --output, shared by both legs' classpaths); the namespace itself is
     * the leg fork — forge "forge" / 21.1 "c" (Tags.java:310-312 vs :799/:923) — and the
     * 21.1 leg's data/c produced face is gated 1:1 against this tree by the
     * datagen_tree_check SEGMENT_MAP c-to-forge band.
     */
    @Test
    void namespaceSplitIsTheLegFork() {
        //? if forge {
        assertEquals("forge", GT6ItemTags.MATERIALS_NAMESPACE);
        //?} else {
        /*assertEquals("c", GT6ItemTags.MATERIALS_NAMESPACE);
        *///?}
        // the GTCEu defaultTagPath precedents, verbatim (TagPrefix.java:279/:290/:405/:416/:729/:223)
        assertEquals("ingots/%s", GT6ItemTags.INGOTS_FAMILY);
        assertEquals("dusts/%s", GT6ItemTags.DUSTS_FAMILY);
        assertEquals("gems/%s", GT6ItemTags.GEMS_FAMILY);
        assertEquals("nuggets/%s", GT6ItemTags.NUGGETS_FAMILY);
        assertEquals("storage_blocks/%s", GT6ItemTags.STORAGE_BLOCKS_FAMILY);
        assertEquals("storage_blocks/raw_%s", GT6ItemTags.STORAGE_BLOCKS_RAW_FAMILY);
    }
}
