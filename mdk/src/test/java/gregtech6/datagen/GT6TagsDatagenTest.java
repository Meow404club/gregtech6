/**
 * Offline JSON-snapshot tests for task p24-tags-provider-skeleton, extended band by band:
 * the tag batches are pinned against the committed generated tree so a future provider
 * refactor cannot drift the product silently. The block face — mineable/pickaxe over stone272
 * + the whole {@link GTMachines} register + the metal/gem/raw-ore prefix blocks, mineable/axe
 * over exactly the wood barrel, and the p24-tags-prefix-materials rolling batch 1 (the shovel
 * band exactly the blockDust family; the wire universe walk-computed from GTWireSpecs; the
 * barrel closure + the fluid pipes into pickaxe); the item face — the GTCEu
 * {@code defaultTagPath} families (TagPrefix.java:279/:290/:405/:416/:729/:223 plus the
 * rolling batch 2 :456/:502/:267) sampled per material with EXACT expected member lists
 * recomputed from the same registration walks the provider uses (the
 * GT6StoneBlocksRenderDatagenTest mirror discipline: enumeration side walks the offline
 * order, generated side is asserted from the classpath tree; the write side is gated by
 * runData: first run written>0, second written:0), plus the ecosystem-alias twins of the
 * decisions.p24-material-name-normalization ruling (aluminium→aluminum,
 * aluminium_brass→aluminum_brass, direct-member mirrors).
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
import gregtech6.registry.GTBarrels;
import gregtech6.registry.GTMaterialBlocks;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTStoneBlocks;
import gregtech6.registry.GTWireSpecs;

class GT6TagsDatagenTest {

    /**
     * The pickaxe-file member total: 272 stone pairs + 26 machine blocks (oven +
     * shredder/crusher/lathe ladders + 4 dryers + 4 distilleries + the 4 Canner rows +
     * the Advanced Crafting Table — GTMachines.java:58/:118-151/:233-237/:347-351/:415-435,
     * the whole-class {@code GTMachines.BLOCKS} walk means every landed machine row joins
     * the band) + the 1 conscious multiblock join (the Lightning Rod pillar block, task
     * p24-lightning-rod — the wall/coil/controller follow the multiblock-family
     * non-membership convention, the provider javadoc) +
     * the 6 metal/gem/raw prefixes' live pairs (3773 - 1096 blockDust = 2677) + the
     * rolling batch 1 faces (task p24-tags-prefix-materials): 629 wires (the legacy
     * 1x/2x pair + 620 electric + 6 redstone + 1 laser — the whole-class
     * {@code GTWires.BLOCKS} walk) + 2 fluid pipes (the task card names GTFluidPipeBlock
     * into pickaxe) + 15 barrels (plastic canister + bronze drum + logistics tank + 12
     * high-tier drums; the wood barrel stays axe-only). Pinned so any band change is a
     * conscious constant update — the maintenance duty the tags card declared for every
     * future machine card (the four Canner rows 2970 → 2974, the rod block 2974 → 2975,
     * the tags batch 2975 → 3621, then the single-variant ACT row 3621 → 3622, task
     * p24-act-machine — the S4 merge-order reconciliation).
     */
    private static final int PINNED_PICKAXE_TOTAL = 272 + 26 + 1 + 2677 + 629 + 2 + 15;


    /** The 13 tier-ladder machine ids of the first machines card + the ACT single-variant row (the dryer/distillery/canner rows ride the total pin). */
    private static final List<String> PINNED_LADDER_MACHINES = List.of(
            "gt6:oven",
            "gt6:shredder", "gt6:shredder_t2", "gt6:shredder_t3", "gt6:shredder_t4",
            "gt6:crusher", "gt6:crusher_t2", "gt6:crusher_t3", "gt6:crusher_t4",
            "gt6:lathe", "gt6:lathe_t2", "gt6:lathe_t3", "gt6:lathe_t4",
            "gt6:advanced_crafting_table");

    /**
     * The pickaxe-band prefixes — the GT6BlockTags band set mirrored (blockDust excluded).
     * Filled in {@link #initMaterialSystem}: the OP prefix fields are registered by OP.init
     * (a static-field initializer here would read nulls on a solo class run — the whole-suite
     * order only masked it with a preceding test's init).
     */
    private static Set<OreDictPrefix> gPickaxeBlockPrefixes;

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
        gPickaxeBlockPrefixes = Set.of(OP.blockRaw, OP.blockGem,
                OP.blockIngot, OP.blockPlate, OP.blockPlateGem, OP.blockSolid);
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
            if (!gPickaxeBlockPrefixes.contains(tPair.prefix())) continue;
            assertTrue(tMembers.contains("gt6:" + GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material())),
                    "prefix block must ride the pickaxe band: " + tPair);
            tPrefixPairs++;
        }
        assertEquals(2677, tPrefixPairs, "3773 storage pairs - 1096 blockDust pairs");
        // exact product shape: no member outside the census families
        assertEquals(PINNED_PICKAXE_TOTAL, tValues.size(),
                "272 stones + 26 machines + 2677 prefix blocks + 629 wires + 2 pipes + 15 barrels");
        assertTrue(tValues.stream().allMatch(v -> v.startsWith("gt6:")), "mod-face-only members");
    }

    /**
     * The strictness face of the acceptance: no member is optional in any mining tag
     * (rolling batch 1 adds the shovel file to the pin).
     */
    @Test
    void noOptionalEntriesInTheMiningTags() throws Exception {
        tagValues("minecraft/tags/blocks/mineable/pickaxe.json"); // the reader itself asserts primitive-only
        tagValues("minecraft/tags/blocks/mineable/axe.json");
        tagValues("minecraft/tags/blocks/mineable/shovel.json");
    }

    /**
     * The exclusions: blockDust belongs to the shovel band (rolling batch 1), the wood
     * barrel rides axe — while the plastic/metal barrel closure joins pickaxe.
     */
    @Test
    void pickaxeBandExcludesTheDustPrefixAndTheWoodBarrel() throws Exception {
        List<String> tValues = tagValues("minecraft/tags/blocks/mineable/pickaxe.json");
        assertTrue(tValues.stream().noneMatch(v -> v.startsWith("gt6:block_dust_")),
                "blockDust is the shovel band, not the pickaxe band");
        assertTrue(!tValues.contains("gt6:barrel_wood"), "the wood barrel belongs to the axe band");
    }

    /** The axe band: exactly the wood fluid barrel (the census material mapping keeps wood out of pickaxe). */
    @Test
    void axeBandIsExactlyTheWoodBarrel() throws Exception {
        assertEquals(List.of("gt6:barrel_wood"), tagValues("minecraft/tags/blocks/mineable/axe.json"));
    }

    /**
     * The shovel band, rolling batch 1: the blockDust prefix family — walk-exact
     * membership (mirror of the provider's filtered walk), the 3773-2677=1096 census count,
     * all gt6. PIN recomputed at merge order (the conscious-update duty, S3 2045c78d):
     * p24-grass-block landed first and its addGrassBand rides the SAME shovel face — the
     * band is now the blockDust family PLUS the 6 GT grass variants (1096+6=1102).
     */
    @Test
    void shovelBandIsExactlyTheDustPrefixFamily() throws Exception {
        List<String> tValues = tagValues("minecraft/tags/blocks/mineable/shovel.json");
        Set<String> tMembers = Set.copyOf(tValues);
        int tDustPairs = 0;
        for (GTMaterialItems.PrefixMaterial tPair : gStorageWalk) {
            if (tPair.prefix() != OP.blockDust) continue;
            assertTrue(tMembers.contains("gt6:" + GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material())),
                    "blockDust pair must ride the shovel band: " + tPair);
            tDustPairs++;
        }
        assertEquals(1096, tDustPairs, "3773 storage pairs - 2677 pickaxe-band pairs");
        assertEquals(tDustPairs + 6, tValues.size(),
                "the shovel band = the blockDust family + the 6 GT grass variants (p24-grass-block merged first)");
        assertTrue(tValues.stream().allMatch(v -> v.startsWith("gt6:")), "mod-face-only members");
    }

    /**
     * The wire universe, rolling batch 1: every GTWires registry path rides pickaxe —
     * walk-computed from the GTWireSpecs tables (the offline face of the provider's
     * whole-class GTWires.BLOCKS enumeration: legacy pair + electric + redstone + laser).
     */
    @Test
    void pickaxeBandCoversTheWireUniverse() throws Exception {
        List<String> tValues = tagValues("minecraft/tags/blocks/mineable/pickaxe.json");
        Set<String> tMembers = Set.copyOf(tValues);
        List<String> tExpected = new ArrayList<>();
        tExpected.add("wire_electric_1x");
        tExpected.add("wire_electric_2x");
        for (GTWireSpecs.Variant tVariant : GTWireSpecs.variants()) tExpected.add(GTWireSpecs.registryName(tVariant));
        for (GTWireSpecs.Variant tVariant : GTWireSpecs.redstoneVariants()) tExpected.add(GTWireSpecs.registryName(tVariant));
        for (GTWireSpecs.Variant tVariant : GTWireSpecs.laserVariants()) tExpected.add(GTWireSpecs.registryName(tVariant));
        assertEquals(629, tExpected.size(), "2 legacy + 620 electric + 6 redstone + 1 laser");
        for (String tPath : tExpected) {
            assertTrue(tMembers.contains("gt6:" + tPath), "wire must ride the pickaxe band: " + tPath);
        }
    }

    /**
     * The barrel closure + the pipes, rolling batch 1: the plastic/metal/logistics barrels
     * and the twelve high-tier drums ride pickaxe (the census axe/pickaxe/pickaxe material
     * mapping), the two fluid pipes ride the task card's explicit GTFluidPipeBlock ruling.
     */
    @Test
    void pickaxeBandCoversTheBarrelClosureAndThePipes() throws Exception {
        List<String> tValues = tagValues("minecraft/tags/blocks/mineable/pickaxe.json");
        Set<String> tMembers = Set.copyOf(tValues);
        List<String> tBarrels = new ArrayList<>(List.of("barrel_plastic", "barrel_metal", "barrel_logistics"));
        for (GTBarrels.MetalDrumRow tRow : GTBarrels.HIGH_TIER_METAL_DRUMS) tBarrels.add(tRow.path());
        assertEquals(15, tBarrels.size(), "3 standalone rows + 12 high-tier drums");
        for (String tPath : tBarrels) {
            assertTrue(tMembers.contains("gt6:" + tPath), "barrel must ride the pickaxe band: " + tPath);
        }
        assertTrue(tMembers.contains("gt6:wood_fluid_pipe_small"), "the fluid pipes ride the task card's pickaxe ruling");
        assertTrue(tMembers.contains("gt6:wood_fluid_pipe_medium"), "the fluid pipes ride the task card's pickaxe ruling");
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

    /** The classpath stream of one generated tag file, or null when it does not exist. */
    private static InputStream classpathTag(String aDataPath) {
        return GT6TagsDatagenTest.class.getClassLoader().getResourceAsStream("data/" + aDataPath);
    }

    /**
     * The alias-twin face (rolling batch 2): the alias tag exists iff the main-name face
     * does (the same pair gates both), and carries EXACTLY the main-name walk members —
     * the direct-member twin form, so alias and main faces stay walk-derivable from one rule.
     */
    private static void assertAliasFace(String aFamily, Set<OreDictPrefix> aPrefixes, String aMainSnake,
            String aAliasSnake, List<GTMaterialItems.PrefixMaterial> aWalk) throws Exception {
        String tAliasPath = "forge/tags/items/" + aFamily + "/" + aAliasSnake + ".json";
        List<String> tExpected = expectedMembers(aPrefixes, aMainSnake, aWalk);
        try (InputStream tStream = classpathTag(tAliasPath)) {
            if (tExpected.isEmpty()) {
                assertNull(tStream, "no alias face may exist without a main-name face: " + tAliasPath);
                return;
            }
            assertNotNull(tStream, "the alias twin face must exist: " + tAliasPath);
            assertEquals(tExpected, tagValues(tAliasPath),
                    "alias twin must mirror the main-name members: " + tAliasPath);
        }
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

    /** The item families over sample materials — ingots/nuggets/dusts/gems + the rolling batch 2 plates/rods/hot ingots. */
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
        // rolling batch 2 (p24-tags-prefix-materials): TagPrefix.java:456/:502/:267 paths
        assertFamilyFace(GT6ItemTags.PLATES_FAMILY, Set.of(OP.plate), "iron", gItemWalk);
        assertFamilyFace(GT6ItemTags.RODS_FAMILY, Set.of(OP.stick), "iron", gItemWalk);
        assertFamilyFace(GT6ItemTags.HOT_INGOTS_FAMILY, Set.of(OP.ingotHot), "iron", gItemWalk);
        assertFamilyFace(GT6ItemTags.HOT_INGOTS_FAMILY, Set.of(OP.ingotHot), "copper", gItemWalk);
    }

    /**
     * The ecosystem-alias twins (rolling batch 2, the decisions.p24-material-name-normalization
     * ruling): every alias face carries EXACTLY the main-name walk members — the direct-member
     * twin form, and the alias face exists iff the main-name face does (same pair gate).
     */
    @Test
    void ecosystemAliasTwinsCarryTheMainNameMembers() throws Exception {
        // the census-backed double-mount list: aluminium→aluminum, aluminium_brass→aluminum_brass
        assertAliasFace("ingots", Set.of(OP.ingot), "aluminium", "aluminum", gItemWalk);
        assertAliasFace("dusts", Set.of(OP.dust), "aluminium", "aluminum", gItemWalk);
        assertAliasFace("plates", Set.of(OP.plate), "aluminium", "aluminum", gItemWalk);
        assertAliasFace("plates", Set.of(OP.plate), "aluminium_brass", "aluminum_brass", gItemWalk);
        assertAliasFace("storage_blocks", Set.of(OP.blockIngot, OP.blockGem, OP.blockDust),
                "aluminium", "aluminum", gStorageWalk);
        // the oredict synonym aliases stay single-named (no ecosystem tag convention)
        assertNull(classpathTag("forge/tags/items/ingots/gibbsite.json"));
        assertNull(classpathTag("forge/tags/items/ingots/co60.json"));
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
     * The p25 tool face (task p25-tool-hammer-wrench, the PIN evolution duty): the two
     * new self-owned crafting-tool tags carry EXACTLY their one registered member each,
     * and the ecosystem tools band grew 7 → 9 (the hammer + wrench pair appended in the
     * band order). Pinned so any band change is a conscious constant update.
     */
    @Test
    void p25ToolFacesAreTheExactMembers() throws Exception {
        assertEquals(List.of("gt6:hammer"), tagValues("gt6/tags/items/tools/hard_hammer.json"));
        assertEquals(List.of("gt6:wrench"), tagValues("gt6/tags/items/tools/wrench.json"));
        assertEquals(List.of(
                "gt6:file", "gt6:saw", "gt6:crowbar", "gt6:cutter", "gt6:chisel",
                "gt6:builder_wand", "gt6:screwdriver", "gt6:hammer", "gt6:wrench"),
                tagValues("forge/tags/items/tools.json"));
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
        // rolling batch 2 (TagPrefix.java:456/:502/:267)
        assertEquals("plates/%s", GT6ItemTags.PLATES_FAMILY);
        assertEquals("rods/%s", GT6ItemTags.RODS_FAMILY);
        assertEquals("hot_ingots/%s", GT6ItemTags.HOT_INGOTS_FAMILY);
    }
}
