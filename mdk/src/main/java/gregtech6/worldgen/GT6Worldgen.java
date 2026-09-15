package gregtech6.worldgen;

import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import gregtech6.block.tree.GT6TreeKind;
import gregtech6.registry.GT6TreeBlocks;
import gregtech6.registry.GTStoneBlocks;

/**
 * The worldgen constant table + the datagen key face (task p26-worldgen-pipeline-skeleton).
 * Pure data — offline-safe by construction (no registry access: {@code ResourceKey.create}
 * is a map intern, the GTStoneBlocksRegistrationTest posture), the datagen walks and the
 * offline test both consume this class.
 *
 * <p>Upstream anchor: the 17 stone blobs are the {@code WorldgenStone} loop rows
 * (Loader_Worldgen.java:654-661); the overworld row is
 * {@code new WorldgenStone("overworld.stone.<material>", F, stone, 0, 1, 200, 100, 0, 120,
 * null, F, GEN_OVERWORLD, ...)} — WorldgenStone.java:41 ctor order
 * {@code (name, default, block, meta, amount, size, probability, minY, maxY, biomeList,
 * void, dims)} bound into the constants below. The upstream per-object config face
 * (WorldgenObject.java:50 mEnabled + :59-66 per-dimension switches, the
 * {@code worldgenerator.overworld.stone.<material>} / {@code .dim.<DimensionName>}
 * config categories) maps onto the generated biome-modifier JSONs in the modern
 * pipeline: the JSON is the datapack-native per-feature enable/limit face (players can
 * delete or condition an entry; the RemoveFeaturesBiomeModifier "de-vanilla" option is a
 * datapack choice, NOT code — the card's append-only coexistence ruling).
 *
 * <p>Documented deviation (card-pinned): upstream EXCLUDES the two prismarines from the
 * blob loop (Loader_Worldgen.java:654 {@code if (tStone != PrismarineDark && !=
 * PrismarineLight)}) — 15 blobs. This port generates all 17 STONES rows verbatim: the
 * card spec pins the 17-stone key set aligned with GTStoneBlocks.STONES (the acceptance
 * key-order audit), and the prismarine exclusion rides the deferred dim-coverage card
 * (nether/twilight/erebus/atum/tropics rows of Loader_Worldgen.java:656-661 are deferred
 * with it).
 */
public final class GT6Worldgen {

    /** WorldgenBlob.java:56 bind(1, 16, config) default 1 — one blob per probability hit. */
    public static final int BLOB_AMOUNT = 1;
    /** WorldgenBlob.java:57 bind(4, 250, config) default 200 — the blob size (OreConfiguration "size"). */
    public static final int BLOB_SIZE = 200;
    /**
     * The vanilla OreConfiguration "size" codec cap — {@code Codec.intRange(0, 64)} both legs
     * (1.20.1 OreConfiguration.java:14, 1.21.1 :15). Documented deviation: the upstream 200
     * block blob is NOT representable over the vanilla OreFeature (the datagen run errors
     * "Value 200 outside of range [0:64]" and the JSON is undecodable at datapack load), so
     * the L0 blob rides {@link #oreBlobSize()} = min(200, 64) = 64 — expected density drops
     * from ~2.0 to ~0.64 blocks/chunk at the same 1/100 rarity. The full-200 blob needs the
     * L1 custom Feature (the GTCEu production precedent: GTFeatures.STONE_BLOB +
     * StoneBlobConfiguration UniformInt.of(20, 30), GTConfiguredFeatures.java:45-55) —
     * deferred with the vein pipeline per the card's defer clause.
     */
    public static final int ORE_SIZE_CODEC_CAP = 64;

    /** The effective OreConfiguration size: the upstream blob size clamped to the vanilla codec cap. */
    public static int oreBlobSize() {
        return Math.min(BLOB_SIZE, ORE_SIZE_CODEC_CAP);
    }
    /** WorldgenBlob.java:55 max(1, config) default 100 — chunk-attempt chance 1/100 (RarityFilter). */
    public static final int BLOB_PROBABILITY = 100;
    /** Loader_Worldgen.java:655 overworld row MinHeight 0 (HeightRangePlacement uniform low anchor). */
    public static final int OVERWORLD_MIN_Y = 0;
    /** Loader_Worldgen.java:655 overworld row MaxHeight 120 (uniform high anchor; upstream picks Y in [min, max)). */
    public static final int OVERWORLD_MAX_Y = 120;

    /** The upstream config category prefix (WorldgenObject.java:49 "worldgenerator." + name), the naming evidence. */
    public static final String UPSTREAM_CATEGORY_PREFIX = "overworld.stone.";

    /**
     * The modern entry id per stone: {@code overworld_stone_<snake>} — the upstream config
     * name {@code overworld.stone.<material>} with the dots flattened (the ResourceLocation
     * path charset keeps dots, but a flat segment matches the sibling datagen file naming).
     */
    public static String entryPath(String aStoneSnake) {
        return "overworld_stone_" + aStoneSnake;
    }

    /** The configured-feature key of a stone (Registries.CONFIGURED_FEATURE, gt6 namespace). */
    public static ResourceKey<ConfiguredFeature<?, ?>> configuredKey(String aStoneSnake) {
        String tPath = entryPath(aStoneSnake);
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath("gt6", tPath));
    }

    /** The placed-feature key of a stone (Registries.PLACED_FEATURE, same path as the configured key — GTCEu blob form). */
    public static ResourceKey<PlacedFeature> placedKey(String aStoneSnake) {
        String tPath = entryPath(aStoneSnake);
        return ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath("gt6", tPath));
    }

    /** The 17 configured keys, GTStoneBlocks.STONES order (the acceptance key-order audit unit). */
    public static final List<ResourceKey<ConfiguredFeature<?, ?>>> CONFIGURED_KEYS = GTStoneBlocks.STONES.stream()
            .map(GTStoneBlocks.StoneSpec::snake).map(GT6Worldgen::configuredKey).toList();

    /** The 17 placed keys, same order (placed[i] hangs off configured[i]). */
    public static final List<ResourceKey<PlacedFeature>> PLACED_KEYS = GTStoneBlocks.STONES.stream()
            .map(GTStoneBlocks.StoneSpec::snake).map(GT6Worldgen::placedKey).toList();

    // ------------------------------------------------------------------ the tree band (task p30-w6-t1-trees-nine)

    /**
     * The modern entry id per tree: {@code tree_<snake>} — the upstream config name {@code
     * tree.rubber} .. {@code tree.bluespruce} (Loader_Worldgen.java:608-616) with the dot
     * flattened ({@link #entryPath} rule).
     */
    public static String treeEntryPath(String aTreeSnake) {
        return "tree_" + aTreeSnake;
    }

    /** The configured-feature key of a tree ({@link #treeEntryPath}; the Feature points at the gt6 TreeFeature). */
    public static ResourceKey<ConfiguredFeature<?, ?>> treeConfiguredKey(String aTreeSnake) {
        String tPath = treeEntryPath(aTreeSnake);
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath("gt6", tPath));
    }

    /** The placed-feature key of a tree (same path as the configured key — the GTCEu blob form). */
    public static ResourceKey<PlacedFeature> treePlacedKey(String aTreeSnake) {
        String tPath = treeEntryPath(aTreeSnake);
        return ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath("gt6", tPath));
    }

    /** The 9 tree configured keys, GT6TreeBlocks.KINDS order (the sapling grower reads treeConfiguredKey(kind)). */
    public static final List<ResourceKey<ConfiguredFeature<?, ?>>> TREE_CONFIGURED_KEYS = GT6TreeBlocks.KINDS.stream()
            .map(GT6TreeKind::snake).map(GT6Worldgen::treeConfiguredKey).toList();

    /** The 9 tree placed keys, same order (placed[i] hangs off configured[i]). */
    public static final List<ResourceKey<PlacedFeature>> TREE_PLACED_KEYS = GT6TreeBlocks.KINDS.stream()
            .map(GT6TreeKind::snake).map(GT6Worldgen::treePlacedKey).toList();

    // ------------------------------------------------------------------
    // The surface deco band (task p30-w6-rocks-sticks) — WorldgenOnSurface
    // ray-cast semantics (WorldgenOnSurface.java:49-76) translated into the
    // vanilla placed-feature modifiers: Amount -> Count, the per-ray
    // nextInt(mProbability) gate -> RarityFilter, the sky ray ->
    // InSquare+HEIGHTMAP, the ground contact -> BlockPredicateFilter.
    // ------------------------------------------------------------------

    /** Loader_Worldgen.java:618 overworld.rocks amount=2 (the per-chunk ray targets). */
    public static final int SURFACE_ROCKS_AMOUNT = 2;
    /** Loader_Worldgen.java:618 overworld.rocks probability=3 (nextInt(3)==0 per ray, WorldgenOnSurface.java:60). */
    public static final int SURFACE_ROCKS_PROBABILITY = 3;
    /** Loader_Worldgen.java:630 sticks probability=2. */
    public static final int STICKS_PROBABILITY = 2;

    /** WorldgenSticks.java:53 woods/swamp = mAmount*3 = 6 rays. */
    public static final int STICKS_DENSE_COUNT = 6;
    /** WorldgenSticks.java:54 river/plains/savanna = mAmount*2 = 4 rays. */
    public static final int STICKS_MODERATE_COUNT = 4;
    /** WorldgenSticks.java:55 taiga/mesa/wasteland = mAmount*1 = 2 rays. */
    public static final int STICKS_SPARSE_COUNT = 2;

    /**
     * The rock-type lottery of WorldgenRocks.java:63, as the RANDOM_SELECTOR chances:
     * the NBT-less half (nextInt(2)!=0) is the stone default rock; of the NBT half
     * 11/12 carry a flint item and 1/12 carry MeteoricIron. Sequential chances:
     * stone if r<0.5, else flint if r<11/12, else meteorite — the joint distribution
     * is 12/24 stone, 11/24 flint, 1/24 meteorite (the SPEC's 12:11:1 weights).
     */
    public static final float SURFACE_ROCK_CHANCE_STONE = 0.5F;
    public static final float SURFACE_ROCK_CHANCE_FLINT = 11.0F / 12.0F;

    /** The generic configured-feature key (the stone-blob key form, path-direct). */
    public static ResourceKey<ConfiguredFeature<?, ?>> configKey(String aPath) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, ResourceLocation.fromNamespaceAndPath("gt6", aPath));
    }

    /** The generic placed-feature key, same path as its configured sibling. */
    public static ResourceKey<PlacedFeature> placedKeyOf(String aPath) {
        return ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.fromNamespaceAndPath("gt6", aPath));
    }

    /** The rocks' outer configured feature (the RANDOM_SELECTOR lottery). */
    public static final ResourceKey<ConfiguredFeature<?, ?>> SURFACE_ROCKS_CONFIGURED = configKey("overworld_surface_rocks");
    /** The rocks' outer placed feature (rarity 3 + count 2, hangs off {@link #SURFACE_ROCKS_CONFIGURED}). */
    public static final ResourceKey<PlacedFeature> SURFACE_ROCKS_PLACED = placedKeyOf("overworld_surface_rocks");
    /** The stick's configured feature (the bare SIMPLE_BLOCK). */
    public static final ResourceKey<ConfiguredFeature<?, ?>> SURFACE_STICK_CONFIGURED = configKey("overworld_surface_stick");

    /** The three stick-group placed paths (WorldgenSticks.java:53-55, dense/moderate/sparse). */
    public static final List<String> STICKS_GROUP_PATHS =
            List.of("overworld_surface_sticks_dense", "overworld_surface_sticks_moderate", "overworld_surface_sticks_sparse");

    /** The biome tags the biome modifiers hang off (gt6 biome tag datagen, the t1-card form). */
    public static final TagKey<Biome> SURFACE_ROCKS_BIOMES = TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("gt6", "surface_rocks"));
    /** WorldgenSticks.java:53 woods|swamp biomes. */
    public static final TagKey<Biome> STICKS_DENSE_BIOMES = TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("gt6", "sticks_dense"));
    /** WorldgenSticks.java:54 river|plains|savanna biomes. */
    public static final TagKey<Biome> STICKS_MODERATE_BIOMES = TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("gt6", "sticks_moderate"));
    /** WorldgenSticks.java:55 taiga|mesa|wasteland biomes (the BoP wastelands have no vanilla tag equivalent — declared skip). */
    public static final TagKey<Biome> STICKS_SPARSE_BIOMES = TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("gt6", "sticks_sparse"));

    private GT6Worldgen() {
    }
}
