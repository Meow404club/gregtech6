/**
 * Offline tests for task p8-prefixblock-render: the model-merging census, the SET_NONE
 * fallback rule, and the borrowed-PNG coverage — everything checkable without the MC
 * registries (the datagen JVM gates the generated-JSON counts themselves: runData first
 * run written>0 / second run written:0, and the loot-table file count == 3773).
 *
 * <p>Pins measured 2026-08-31 over the card-A census walk
 * ({@link GTMaterialBlocks#registrationOrder()}, 3773 pairs): the per-prefix live
 * texture-set lists (upstream TextureSet.java:188-228 names, resolved through
 * {@code material.mTextureSetsBlock}, OreDictMaterial.java:252 / MT.java:210-215) sum to
 * 175 shared (prefix x set) block models — NOT one model per material pair (the anti-bloat
 * red line). The SET_NONE fallback (empty/blank set list -> "none", the
 * GT6ItemModels.iconsetOf mirror on the block list) fires for ZERO pairs today, but the
 * generator dereferences nothing blindly.
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictPrefix;
import gregtech6.registry.GTMaterialBlocks;
import gregtech6.registry.GTMaterialItems;

class GT6PrefixBlockRenderDatagenTest {

    /** The card-A census yardstick (GTMaterialBlocksRegistrationTest, re-pinned here for the render card). */
    private static final int PINNED_TOTAL = 3773;

    /**
     * The live BLOCK texture-set list per storage prefix, measured 2026-08-31 (probe over the
     * production walk). 29+22+36+22+23+21+22 = 175 shared models. If the census or the
     * MT.setTextures assignments ever change, this pins the delta consciously.
     */
    private static final Map<String, Set<String>> PINNED_LIVE_SETS = new LinkedHashMap<>();
    static {
        PINNED_LIVE_SETS.put("blockRaw", sets("COPPER", "CUBE", "CUBE_SHINY", "DIAMOND", "DULL", "EMERALD", "FINE",
                "FLINT", "GEM_HORIZONTAL", "GEM_VERTICAL", "GLASS", "HEX", "LAPIS", "LIGNITE", "METALLIC", "NETHERSTAR",
                "OPAL", "POWDER", "PRISMARINE", "QUARTZ", "RAD", "REDSTONE", "ROUGH", "RUBY", "SAND", "SHARDS", "SHINY",
                "SPACE", "STONE"));
        PINNED_LIVE_SETS.put("blockGem", sets("CUBE_SHINY", "DIAMOND", "DULL", "EMERALD", "FINE", "FLINT",
                "GEM_HORIZONTAL", "GEM_VERTICAL", "GLASS", "HEX", "LAPIS", "LIGNITE", "METALLIC", "NETHERSTAR", "OPAL",
                "PRISMARINE", "QUARTZ", "REDSTONE", "ROUGH", "RUBY", "SHARDS", "SHINY"));
        PINNED_LIVE_SETS.put("blockDust", sets("BRICK", "COPPER", "CUBE", "CUBE_SHINY", "DIAMOND", "DULL", "EMERALD",
                "FIERY", "FINE", "FLINT", "FOOD", "GEM_HORIZONTAL", "GEM_VERTICAL", "GLASS", "HEX", "LAPIS", "LEAF",
                "LIGNITE", "METALLIC", "NETHERSTAR", "OPAL", "PAPER", "POWDER", "PRISMARINE", "QUARTZ", "RAD",
                "REDSTONE", "ROUGH", "RUBBER", "RUBY", "SAND", "SHARDS", "SHINY", "SPACE", "STONE", "WOOD"));
        PINNED_LIVE_SETS.put("blockIngot", sets("BRICK", "COPPER", "CUBE", "DIAMOND", "DULL", "FIERY", "FINE", "FOOD",
                "LAPIS", "LEAF", "LIGNITE", "MAGNETIC", "METALLIC", "QUARTZ", "RAD", "REDSTONE", "ROUGH", "RUBBER",
                "RUBY", "SHINY", "SPACE", "WOOD"));
        PINNED_LIVE_SETS.put("blockPlate", sets("BRICK", "COPPER", "DIAMOND", "DULL", "FIERY", "FINE", "FOOD",
                "LAPIS", "LEAF", "LIGNITE", "MAGNETIC", "METALLIC", "POWDER", "QUARTZ", "RAD", "REDSTONE", "ROUGH",
                "RUBBER", "RUBY", "SHINY", "SPACE", "STONE", "WOOD"));
        PINNED_LIVE_SETS.put("blockPlateGem", sets("CUBE_SHINY", "DIAMOND", "DULL", "EMERALD", "FINE", "FLINT",
                "GEM_HORIZONTAL", "GEM_VERTICAL", "GLASS", "HEX", "LAPIS", "LIGNITE", "METALLIC", "NETHERSTAR", "OPAL",
                "PRISMARINE", "QUARTZ", "REDSTONE", "RUBY", "SHARDS", "SHINY"));
        PINNED_LIVE_SETS.put("blockSolid", sets("BRICK", "COPPER", "CUBE", "DIAMOND", "DULL", "FIERY", "FINE", "FOOD",
                "LAPIS", "LEAF", "LIGNITE", "MAGNETIC", "METALLIC", "QUARTZ", "RAD", "REDSTONE", "ROUGH", "RUBBER",
                "RUBY", "SHINY", "SPACE", "WOOD"));
    }

    /** The walk order = the upstream seven storage prefixes (same base list as the census test). */
    private static final List<String> UPSTREAM_BLOCK_PATH = List.of(
        "blockRaw", "blockGem", "blockDust", "blockIngot", "blockPlate", "blockPlateGem", "blockSolid");

    /** Pinned set names are written in the upstream TextureSet casing and compared lower-snake (the datagen-facing form). */
    private static Set<String> sets(String... aNames) {
        Set<String> rSets = new TreeSet<>();
        for (String tName : aNames) rSets.add(tName.toLowerCase(java.util.Locale.ROOT));
        return rSets;
    }

    @BeforeAll
    static void initMaterialSystem() {
        // GT6BlockStates is a BlockStateProvider subclass: touching its statics class-loads the
        // provider chain (BlockStateProperties -> BuiltInRegistries), which needs the vanilla
        // bootstrap in a headless JVM. The GTOfflineRenderTestBase recipe: bootStrap, then
        // swallow the expected offline NetworkHooks.init() failure (registries are ready by then).
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
        }
        GTMaterialItems.initMaterials();
    }

    /** The live set of one prefix family, in the production walk. */
    private static TreeSet<String> liveSets(OreDictPrefix aPrefix) {
        TreeSet<String> rSets = new TreeSet<>();
        for (GTMaterialItems.PrefixMaterial tPair : GTMaterialBlocks.registrationOrder()) {
            if (tPair.prefix() != aPrefix) continue;
            rSets.add(GT6BlockStates.blockSetOf(tPair.material()));
        }
        return rSets;
    }

    /** The live set lists are pinned exactly (membership + size), and their pair-sum is 175 shared models. */
    @Test
    void liveSetsArePinned() {
        int tTotalModels = 0;
        Set<String> tUnion = new TreeSet<>();
        for (OreDictPrefix tPrefix : GTMaterialBlocks.blockPathPrefixes()) {
            TreeSet<String> tLive = liveSets(tPrefix);
            Set<String> tPinned = PINNED_LIVE_SETS.get(tPrefix.mNameInternal);
            assertNotNull(tPinned, tPrefix.mNameInternal + " must be pinned");
            assertEquals(tPinned, tLive, tPrefix.mNameInternal + " live texture sets (TextureSet.java:188-228 names)");
            tTotalModels += tLive.size();
            tUnion.addAll(tLive);
        }
        assertEquals(175, tTotalModels, "shared (prefix x set) block models = the anti-bloat model count");
        assertEquals(37, tUnion.size(), "distinct texture-set names across the seven prefixes");
    }

    /**
     * The SET_NONE fallback rule: an empty/blank mTextureSetsBlock resolves to "none"
     * (upstream TextureSet.java:188 SET_NONE — never a blind dereference), and over the
     * registered 3773 pairs it fires ZERO times (measured 2026-08-31) — every pair borrows
     * its real set texture.
     */
    @Test
    void fallbackIsNoneAndNeverFiresToday() {
        // MT.NULL (mID -1, registered at MT class load) is never run through MT.setTextures —
        // its mTextureSetsBlock is the empty-list field default, the upstream SET_NONE fallback
        // target (upstream OreDictMaterial.java:252 defaults the list to SET_NONE[0].mList).
        assertEquals("none", GT6BlockStates.blockSetOf(MT.NULL), "an untextured material must fall back to SET_NONE semantics");
        int tFallbackFires = 0;
        for (GTMaterialItems.PrefixMaterial tPair : GTMaterialBlocks.registrationOrder()) {
            if ("none".equals(GT6BlockStates.blockSetOf(tPair.material()))) tFallbackFires++;
        }
        assertEquals(0, tFallbackFires, "fallback fires for zero registered pairs (measured 2026-08-31)");
    }

    /**
     * Every registered pair's texture path exists among the borrowed PNGs — the borrow
     * intersection is complete for ALL 3773 blocks, not just the 175 distinct models, and
     * every path satisfies the 1.20.1 ResourceLocation charset (the declared lowercase
     * deviation from the upstream CamelCase file names).
     */
    @Test
    void everyPairReferencesABorrowedLowercasePng() {
        for (GTMaterialItems.PrefixMaterial tPair : GTMaterialBlocks.registrationOrder()) {
            String tSetSnake = GT6BlockStates.blockSetOf(tPair.material());
            String tPrefixSnake = GTMaterialItems.snakeCase(tPair.prefix().mNameInternal);
            assertTrue(tSetSnake.matches("[a-z0-9_]+") && tPrefixSnake.matches("[a-z0-9_]+"),
                    "lowercase ResourceLocation charset: " + tSetSnake + "/" + tPrefixSnake);
            String tPath = "/assets/gt6/textures/block/materialicons/" + tSetSnake + "/" + tPrefixSnake + ".png";
            assertNotNull(GT6PrefixBlockRenderDatagenTest.class.getResource(tPath),
                    "borrowed grayscale PNG missing: " + tPath);
        }
    }

    /** The borrowed PNG inventory is exactly the 175 referenced (prefix x set) files — no strays. */
    @Test
    void borrowedPngCountMatchesModelCount() throws Exception {
        String tRoot = "/assets/gt6/textures/block/materialicons";
        var tUrl = GT6PrefixBlockRenderDatagenTest.class.getResource(tRoot);
        assertNotNull(tUrl, "the materialicons resource root must exist");
        int tCount = 0;
        try (var tWalk = java.nio.file.Files.walk(java.nio.file.Path.of(tUrl.toURI()))) {
            tCount = (int)tWalk.filter(p -> p.toString().endsWith(".png")).count();
        }
        assertEquals(175, tCount, "one borrowed PNG per referenced (prefix x set) model");
    }

    /**
     * The loot 1:1 rule: the provider's generate() maps dropSelf over
     * {@link GT6LootTables#lootBlocks()} = the block array = the census walk, so the table
     * count is the block count (3773). Offline the registries never fire, so this pins the
     * enumeration side of the equality (3773 pairs); the generated-file side is gated by
     * runData (loot_tables/blocks/*.json == 3773, written>0 then written:0).
     */
    @Test
    void lootTableCountEqualsBlockCountByConstruction() {
        assertEquals(PINNED_TOTAL, GTMaterialBlocks.registrationOrder().size(), "the census walk the blocks and tables both derive from");
        assertEquals(GTMaterialBlocks.blockArray().length, GT6LootTables.lootBlocks().size(),
                "lootBlocks is a snapshot of the block array (both empty offline, both 3773 in the datagen JVM)");
        assertEquals("block_ingot_coal", GTMaterialItems.itemIdOf(OP.blockIngot, MT.Coal),
                "the vanilla default table location: gt6:blocks/block_ingot_coal (Block.getLootTable default, zero block code)");
    }
}
