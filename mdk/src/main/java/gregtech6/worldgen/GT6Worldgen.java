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

    // ------------------------------------------------------------------
    // The strata-lens band (task p31-strata-lens) — the option-b ruling
    // (decisions.2026-09-17-p30-strata-ruling): the 5 marker stones generate as
    // mountain-scale flattened-blob lenses through the ONE gt6:strata_lenses
    // feature (the GT6VeinGenerator isomorphic core + row table); the other 12
    // stones keep the L0 blob above. Registration increment zero (the 272
    // GTStoneBlocks universe already carries all five).
    // ------------------------------------------------------------------

    /**
     * The 5 marker stones (the card spec's settled list, spec order): their per-stone blob
     * rows are RETIRED (a blob + a lens would double-generate the stone), so the
     * biome-modifier band ships 12 blob rows + the one lens row.
     */
    public static final List<String> LENS_STONE_SNAKES =
            List.of("marble", "basalt", "kimberlite", "granite_red", "komatiite");

    /** The 12 blob stones: STONES minus the lens band (the option-b "其余 12 石维持 blob" face). */
    public static final List<GTStoneBlocks.StoneSpec> BLOB_STONES = GTStoneBlocks.STONES.stream()
            .filter(tStone -> !LENS_STONE_SNAKES.contains(tStone.snake())).toList();

    /** The 12 blob configured keys, BLOB_STONES order (the acceptance key-order audit unit). */
    public static final List<ResourceKey<ConfiguredFeature<?, ?>>> CONFIGURED_KEYS = BLOB_STONES.stream()
            .map(GTStoneBlocks.StoneSpec::snake).map(GT6Worldgen::configuredKey).toList();

    /** The 12 blob placed keys, same order (placed[i] hangs off configured[i]). */
    public static final List<ResourceKey<PlacedFeature>> PLACED_KEYS = BLOB_STONES.stream()
            .map(GTStoneBlocks.StoneSpec::snake).map(GT6Worldgen::placedKey).toList();

    /** The strata-lens configured feature (the GT6StrataLensFeature instance + the 5-row lens table). */
    public static final ResourceKey<ConfiguredFeature<?, ?>> STRATA_LENSES_CONFIGURED = configKey("strata_lenses");
    /** The strata-lens placed feature (Count 1 constant + InSquare + BiomeFilter — one attempt per chunk). */
    public static final ResourceKey<PlacedFeature> STRATA_LENSES_PLACED = placedKeyOf("strata_lenses");

    // ------------------------------------------------------------------
    // The nether stone-lens band (task p31-nether-lens-end-yield). Upstream:
    // the WorldgenStone loop's nether row per stone —
    // {@code new WorldgenStone("nether.stone.<mat>", F, stone, 0, 1, 200, 200,
    // 0, 120, null, F, GEN_NETHER)} (Loader_Worldgen.java:656; the ctor order
    // binds meta=0, amount=1, size=200, probability=200, MinHeight=0,
    // MaxHeight=120) — default F, this port ships it DEFAULT ON (the declared
    // deviation, decisions.2026-09-17-p30-nether-end-rulings "nether-lens-default-on").
    // All 17 stones ride ONE lens feature (the strata-lens isomorph) with the
    // INDEPENDENT per-row 1/200 rolls (WorldgenBlob.java:68 shape).
    // ------------------------------------------------------------------

    /** Loader_Worldgen.java:656 nether row probability 200 — the per-row 1/200 chunk gate. */
    public static final int NETHER_LENS_PROBABILITY = 200;
    /** Loader_Worldgen.java:656 nether row MinHeight 0. */
    public static final int NETHER_LENS_MIN_Y = 0;
    /** Loader_Worldgen.java:656 nether row MaxHeight 120 (the lens center draw reads it inclusively — the strata-lens table convention). */
    public static final int NETHER_LENS_MAX_Y = 120;
    /** The clean-calibration radius stand-in for the upstream size-200 sausage blob (see NETHER_LENS_TABLE). */
    public static final int NETHER_LENS_RADIUS = 40;
    /** The clean-calibration vertical half-extent (the upstream blob's ~13-block vertical reach). */
    public static final int NETHER_LENS_HALF_HEIGHT = 12;

    /** The nether-lens configured feature (the GT6NetherLensFeature instance + the 17-row table). */
    public static final ResourceKey<ConfiguredFeature<?, ?>> NETHER_LENSES_CONFIGURED = configKey("nether_lenses");
    /** The nether-lens placed feature (Count 1 constant + InSquare + BiomeFilter — one attempt per chunk; the per-chunk row rolls live in the Feature). */
    public static final ResourceKey<PlacedFeature> NETHER_LENSES_PLACED = placedKeyOf("nether_lenses");

    // ------------------------------------------------------------------
    // The nether three-form band (task p31-nether-lens-end-yield spec ①):
    // WorldgenNetherQuartz (:600)/WorldgenNetherCrystals (:601)/WorldgenNetherClay
    // (:599), all default T upstream and all GEN_NETHER — three NoneFeatureConfiguration
    // features over the GT6WorleyNoise port. The payloads are the GT6NetherOres
    // band blocks (the minimal-carrier ruling).
    // ------------------------------------------------------------------

    public static final ResourceKey<ConfiguredFeature<?, ?>> NETHER_QUARTZ_CONFIGURED = configKey("nether_quartz");
    public static final ResourceKey<PlacedFeature> NETHER_QUARTZ_PLACED = placedKeyOf("nether_quartz");
    public static final ResourceKey<ConfiguredFeature<?, ?>> NETHER_CRYSTALS_CONFIGURED = configKey("nether_crystals");
    public static final ResourceKey<PlacedFeature> NETHER_CRYSTALS_PLACED = placedKeyOf("nether_crystals");
    public static final ResourceKey<ConfiguredFeature<?, ?>> NETHER_CLAY_CONFIGURED = configKey("nether_clay");
    public static final ResourceKey<PlacedFeature> NETHER_CLAY_PLACED = placedKeyOf("nether_clay");

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

    // ------------------------------------------------------------------
    // The surface-plants + soil band (task p30-w6-t2-surface-blocks). The
    // WorldgenOnSurface ray gates (Amount targets x the nextInt(Probability)
    // gate, WorldgenOnSurface.java:49-76) translate to Count+RarityFilter; the
    // WorldgenPit/BlackSand/Turf chunk gates (nextInt(divider)) translate to
    // RarityFilter verbatim.
    // ------------------------------------------------------------------

    /** Loader_Worldgen.java:632 plant.glowtus amount=16 (the per-chunk ray targets). */
    public static final int GLOWTUS_AMOUNT = 16;
    /** Loader_Worldgen.java:632 plant.glowtus probability=2. */
    public static final int GLOWTUS_PROBABILITY = 2;
    /** Loader_Worldgen.java:633 plant.bush amount=1. */
    public static final int BUSH_AMOUNT = 1;
    /** Loader_Worldgen.java:633 plant.bush probability=4. */
    public static final int BUSH_PROBABILITY = 4;
    /** WorldgenBlackSand.java:48 {@code nextInt(64) > 0} — the 1/64 chunk gate. */
    public static final int BLACKSAND_DIVIDER = 64;
    /** WorldgenTurf.java:49 {@code nextInt(32) > 0} — the 1/32 chunk gate. */
    public static final int TURF_DIVIDER = 32;
    /**
     * WorldgenPit.java:58 {@code nextInt(mDivider) > mChance} with mChance=bindInt(1-1)=0
     * (the pit ctor's aChance=1, Loader_Worldgen.java:592) — the 1/320 chunk gate.
     */
    public static final int PIT_CLAY_DIVIDER = 320;
    /** Loader_Worldgen.java:603-606 — the four fallen-log gates, dry/rotten/mossy/frozen (amount=1 all four). */
    public static final List<Integer> FALLEN_LOG_PROBABILITY = List.of(8, 3, 8, 8);

    /**
     * The vanilla DiskConfiguration radius for the three soil disks (black sand /
     * turf / the clay pit): the upstream 48x48 SHAPE is a radius ~23 disk (WorldgenPit
     * .java:83-131), but the DiskConfiguration codec caps radius at 8
     * (DiskConfiguration.java:15 {@code Codec.intRange(0, 8)}) — the declared areal
     * deviation; the chunk gates stay verbatim. CONSTANT 7 (not a uniform band): the
     * IntProvider JSON face is leg-forked for uniform (1.20.1 wraps {@code value},
     * 1.21.1 flat) while the constant form is shape-identical — the datagen_tree_check
     * byte-equality gate holds without a declared-fork entry.
     */
    public static final net.minecraft.util.valueproviders.ConstantInt SOIL_DISK_RADIUS =
            net.minecraft.util.valueproviders.ConstantInt.of(7);
    /** The pit's vertical face: half_height 4 (the codec cap, DiskConfiguration.java:16) — 5 replaced layers vs the upstream 7. */
    public static final int PIT_CLAY_HALF_HEIGHT = 4;
    /** The black-sand face: half_height 0 = exactly the 2-layer replacement (WorldgenBlackSand.java:57 {@code tGenerated < 2}). */
    public static final int BLACKSAND_HALF_HEIGHT = 0;
    /** The turf face: half_height 1 = the top soil pair plus the surface block (WorldgenTurf.java:57 {@code tGenerated < 2}). */
    public static final int TURF_HALF_HEIGHT = 1;

    /** The modern entry ids (the dot-flattened upstream config names, the {@link #entryPath} rule). */
    public static final String GLOWTUS_PATH = "plant_glowtus";
    public static final String BUSH_PATH = "plant_bush";
    public static final String BLACKSAND_PATH = "river_magnetite";
    public static final String TURF_PATH = "swamp_turf";
    public static final String PIT_CLAY_PATH = "pit_clay_vanilla";
    /** The four fallen-log paths, upstream log.dry/rotten/mossy/frozen (Loader_Worldgen.java:603-606). */
    public static final List<String> FALLEN_LOG_PATHS = List.of("log_dry", "log_rotten", "log_mossy", "log_frozen");

    /** The configured/placed keys of this band (path-direct, the {@link #configKey} form). */
    public static final ResourceKey<ConfiguredFeature<?, ?>> GLOWTUS_CONFIGURED = configKey(GLOWTUS_PATH);
    public static final ResourceKey<PlacedFeature> GLOWTUS_PLACED = placedKeyOf(GLOWTUS_PATH);
    public static final ResourceKey<ConfiguredFeature<?, ?>> BUSH_CONFIGURED = configKey(BUSH_PATH);
    public static final ResourceKey<PlacedFeature> BUSH_PLACED = placedKeyOf(BUSH_PATH);
    public static final ResourceKey<ConfiguredFeature<?, ?>> BLACKSAND_CONFIGURED = configKey(BLACKSAND_PATH);
    public static final ResourceKey<PlacedFeature> BLACKSAND_PLACED = placedKeyOf(BLACKSAND_PATH);
    public static final ResourceKey<ConfiguredFeature<?, ?>> TURF_CONFIGURED = configKey(TURF_PATH);
    public static final ResourceKey<PlacedFeature> TURF_PLACED = placedKeyOf(TURF_PATH);
    public static final ResourceKey<ConfiguredFeature<?, ?>> PIT_CLAY_CONFIGURED = configKey(PIT_CLAY_PATH);
    public static final ResourceKey<PlacedFeature> PIT_CLAY_PLACED = placedKeyOf(PIT_CLAY_PATH);

    /** The 4 fallen-log configured keys, {@link #FALLEN_LOG_PATHS} order. */
    public static final List<ResourceKey<ConfiguredFeature<?, ?>>> FALLEN_LOG_CONFIGURED_KEYS =
            FALLEN_LOG_PATHS.stream().map(GT6Worldgen::configKey).toList();
    /** The 4 fallen-log placed keys, same order. */
    public static final List<ResourceKey<PlacedFeature>> FALLEN_LOG_PLACED_KEYS =
            FALLEN_LOG_PATHS.stream().map(GT6Worldgen::placedKeyOf).toList();

    /** The biome tags this band's biome modifiers hang off (gt6 biome tag datagen, the t1-card form). */
    public static final TagKey<Biome> GLOWTUS_BIOMES = biomeTag("surface_glowtus");
    /** WorldgenBushes.java:56 plains|woods minus the frozen set. */
    public static final TagKey<Biome> BUSH_BIOMES = biomeTag("surface_bush");
    /** WorldgenBlackSand.java:51 river minus ocean/beach/swamp — the river tag is the vanilla face. */
    public static final TagKey<Biome> BLACKSAND_BIOMES = biomeTag("surface_blacksand");
    /** WorldgenTurf.java:51 swamp. */
    public static final TagKey<Biome> TURF_BIOMES = biomeTag("surface_turf");
    /** WorldgenPit.java:58 plains|savanna at the chunk centre. */
    public static final TagKey<Biome> PIT_CLAY_BIOMES = biomeTag("surface_pit_clay");
    /** WorldgenLogDry.java:50 plains|woods|savanna|desert|mesa|wastelands. */
    public static final TagKey<Biome> LOG_DRY_BIOMES = biomeTag("surface_log_dry");
    /** WorldgenLogRotten.java:49 swamp|jungle. */
    public static final TagKey<Biome> LOG_ROTTEN_BIOMES = biomeTag("surface_log_rotten");
    /** WorldgenLogMossy.java:52 plains|woods|swamp. */
    public static final TagKey<Biome> LOG_MOSSY_BIOMES = biomeTag("surface_log_mossy");
    /** WorldgenLogFrozen.java:50 frozen. */
    public static final TagKey<Biome> LOG_FROZEN_BIOMES = biomeTag("surface_log_frozen");

    private static TagKey<Biome> biomeTag(String aPath) {
        return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("gt6", aPath));
    }

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

    // ------------------------------------------------------------------
    // The large-vein band (task p30-w6-t3-large-veins) — the single Feature over the
    // 40-row JSON vein table; key form = the surface-band path-direct shape.
    // ------------------------------------------------------------------

    /** The large-vein configured feature (the GT6LargeVeinFeature instance + the 40-row vein table). */
    public static final ResourceKey<ConfiguredFeature<?, ?>> LARGE_VEINS_CONFIGURED = configKey("large_veins");
    /** The large-vein placed feature (one attempt per chunk — the per-chunk origin-grid scan lives in the Feature). */
    public static final ResourceKey<PlacedFeature> LARGE_VEINS_PLACED = placedKeyOf("large_veins");

    // ------------------------------------------------------------------
    // The bedrock-ore band (task p31-bedrock-ore-worldgen) — the single Feature over the
    // 46-row bedrock-ore table; key form = the large-vein band's path-direct shape.
    // ------------------------------------------------------------------

    /** The bedrock-ore configured feature (the GT6BedrockOreFeature instance + the 46-row table). */
    public static final ResourceKey<ConfiguredFeature<?, ?>> BEDROCK_ORES_CONFIGURED = configKey("bedrock_ores");
    /**
     * The bedrock-ore placed feature (Count 1 constant + InSquare + BiomeFilter — one
     * attempt per chunk; the per-chunk row rolls live in the Feature on the coordinate-
     * seeded stream, the conflict-audit Count-constant/InSquare/BiomeFilter posture).
     */
    public static final ResourceKey<PlacedFeature> BEDROCK_ORES_PLACED = placedKeyOf("bedrock_ores");

    private GT6Worldgen() {
    }
}
