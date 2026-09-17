package gregtech6.datagen;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
//? if forge {
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ForgeBiomeModifiers;
import net.minecraftforge.registries.ForgeRegistries;
//?} else {
/*import net.minecraft.data.worldgen.BootstrapContext;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
//21.1: the 1.20.5 Bootstap typo fix renamed the class (BootstrapContext), and the
//biome-modifier world classes moved packages — the research card's triple seam.
*///?}
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.data.worldgen.placement.PlacementUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.DiskConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.RuleBasedBlockStateProvider;
import net.minecraft.world.level.levelgen.blockpredicates.BlockPredicate;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.BlockPredicateFilter;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.List;
import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;
import gregtech6.block.stone.StoneVariant;
import gregtech6.block.tree.GT6TreeKind;
import gregtech6.registry.GT6OreBlocks;
import gregtech6.registry.GT6TreeBlocks;
import gregtech6.registry.GTStoneBlocks;
import gregtech6.registry.GT6SurfaceBlocks;
import gregtech6.worldgen.GT6FallenLogFeature;
import gregtech6.worldgen.GT6Features;
import gregtech6.worldgen.GT6Worldgen;
import gregtech6.worldgen.GTLensConfig;
import gregtech6.worldgen.GTVeinConfig;
import gregtech6.worldgen.GTOreWorldgen;

/**
 * The worldgen datagen band (task p26-worldgen-pipeline-skeleton): one
 * {@link RegistrySetBuilder} with three BootstapContexts — configured feature / placed
 * feature / biome modifier — producing 17+17+17 JSONs for the GT6 stone blobs (the
 * {@code WorldgenStone} loop port, Loader_Worldgen.java:654-661; the parameter
 * transcription + key tables live in {@link GT6Worldgen}). Consumed by the
 * {@code DatapackBuiltinEntriesProvider} row appended in GT6DataGenerators (GTCEu
 * data/DataGenerators.java:40 precedent; the provider class import is the other leg
 * seam, handled there).
 *
 * <p>Dual-leg evidence (the biome-modifier triple seam, research card
 * p26-r-worldgen-domain): the registry key is {@code forge:biome_modifier}
 * (ForgeRegistries.java:195) vs {@code neoforge:biome_modifier}
 * (NeoForgeRegistries.java:61-66); the AddFeaturesBiomeModifier holder class is
 * {@code net.minecraftforge.common.world.ForgeBiomeModifiers} (ForgeBiomeModifiers.java:46
 * record, JSON type {@code forge:add_features}) vs
 * {@code net.neoforged.neoforge.common.world.BiomeModifiers} (BiomeModifiers.java:47
 * record, JSON type {@code neoforge:add_features}); the JSON directory follows the
 * registry key namespace — {@code data/gt6/forge/biome_modifier/} (GTCEu 1.20.1
 * generated tree data/gtceu/forge/biome_modifier/ is the production proof) vs
 * {@code data/gt6/neoforge/biome_modifier/} (1.21.1 Registries.elementsDirPath =
 * CommonHooks.prefixNamespace, Registries.java:251-253). Both legs' provider share the
 * 4-arg {@code (PackOutput, HolderLookup.Provider, RegistrySetBuilder, Set)} ctor (forge
 * DatapackBuiltinEntriesProvider.java:51 / neoforge :81), so only the import forks.
 * Everything vanilla in this file (OreConfiguration/FeatureUtils/PlacementUtils + the
 * placement modifiers) is shape-identical on both legs (1.20.1 OreConfiguration.java:10-58
 * vs 1.21.1 :24-42 read back; HeightRangePlacement.uniform / RarityFilter.onAverageOnceEvery
 * same-named same-arg both legs).
 *
 * <p>Coexistence (the card ruling): append-only — the biome modifiers ADD the GT blobs on
 * top of the vanilla ore features (GT6 1.7.10 keeps vanilla ores too); removing vanilla
 * ores is a datapack option ({@code forge:removefeatures} /
 * {@code neoforge:removefeatures}), deliberately not generated.
 */
public final class GT6WorldgenDatagen {

    /**
     * The biome-modifier registry key, per leg ({@code forge:biome_modifier} vs
     * {@code neoforge:biome_modifier} — the research card's triple-seam face).
     */
    public static ResourceKey<Registry<BiomeModifier>> biomeModifierRegistryKey() {
        //? if forge {
        return ForgeRegistries.Keys.BIOME_MODIFIERS;
        //?} else {
        /*return NeoForgeRegistries.Keys.BIOME_MODIFIERS;
        *///?}
    }

    /**
     * The 12 blob biome-modifier keys, BLOB_STONES order — one AddFeaturesBiomeModifier
     * row per stone (the upstream per-object config face,
     * {@code worldgenerator.overworld.stone.<material>}, becomes one datapack JSON each).
     * Task p31-strata-lens: the 5 marker stones (GT6Worldgen.LENS_STONE_SNAKES) ride the
     * ONE strata-lens modifier below instead, their blob rows retired.
     */
    public static final List<ResourceKey<BiomeModifier>> BIOME_MODIFIER_KEYS =
            GT6Worldgen.BLOB_STONES.stream().map(GTStoneBlocks.StoneSpec::snake)
                    .map(GT6WorldgenDatagen::biomeModifierKey).toList();

    private static ResourceKey<BiomeModifier> biomeModifierKey(String aStoneSnake) {
        String tPath = GT6Worldgen.entryPath(aStoneSnake);
        return ResourceKey.create(biomeModifierRegistryKey(), ResourceLocation.fromNamespaceAndPath("gt6", tPath));
    }

    /**
     * The 9 tree biome-modifier keys, GT6TreeBlocks.KINDS order (task p30-w6-t1-trees-nine
     * — one AddFeaturesBiomeModifier row per tree, the upstream per-object config face).
     */
    public static final List<ResourceKey<BiomeModifier>> TREE_BIOME_MODIFIER_KEYS =
            GT6TreeBlocks.KINDS.stream().map(GT6TreeKind::snake)
                    .map(GT6WorldgenDatagen::treeBiomeModifierKey).toList();

    private static ResourceKey<BiomeModifier> treeBiomeModifierKey(String aTreeSnake) {
        String tPath = GT6Worldgen.treeEntryPath(aTreeSnake);
        return ResourceKey.create(biomeModifierRegistryKey(), ResourceLocation.fromNamespaceAndPath("gt6", tPath));
    }

    /**
     * The per-tree chunk-chance column (the upstream Probability), Loader_Worldgen.java
     * :608-616 verbatim: rubber 1/5, maple 1/5, willow 1/4, bluemahoe 1/3, hazel 1/32,
     * cinnamon 1/3, coconut 1/1, rainbowood 1/4, bluespruce 1/32. Amount is 1 for all
     * nine rows (one placement attempt per probability hit — the RarityFilter+default
     * count=1 translation, card spec ③).
     */
    public static final List<Integer> TREE_PROBABILITY = List.of(5, 5, 4, 3, 32, 3, 1, 4, 32);

    /** The single RegistrySetBuilder handed to the DatapackBuiltinEntriesProvider (GT6DataGenerators). */
    public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(Registries.CONFIGURED_FEATURE, GT6WorldgenDatagen::bootstrapConfigured)
            .add(Registries.PLACED_FEATURE, GT6WorldgenDatagen::bootstrapPlaced)
            .add(biomeModifierRegistryKey(), GT6WorldgenDatagen::bootstrapBiomeModifiers);

    /**
     * The three builder bands as a key table, BUILDER order — the acceptance ctx-key audit
     * unit. The mirror exists because 1.20.1 RegistrySetBuilder has NO key accessor (the
     * public {@code getEntryKeys()} is a 1.21.1 addition, vanilla-mc 1.21.1
     * RegistrySetBuilder.java:75), so the offline test can only introspect the builder on
     * the neoforge leg and pins the forge leg through this constant the BUILDER rows are
     * textually bound to.
     */
    public static final List<ResourceKey<? extends Registry<?>>> BUILDER_KEYS = List.of(
            Registries.CONFIGURED_FEATURE, Registries.PLACED_FEATURE, biomeModifierRegistryKey());

    private GT6WorldgenDatagen() {
    }

    /**
     * The one holder-class-name seam (ForgeBiomeModifiers vs BiomeModifiers), extracted so
     * the bootstrap loop below stays leg-free.
     */
    private static BiomeModifier addFeatures(HolderSet<Biome> aBiomes, HolderSet<PlacedFeature> aFeatures,
            GenerationStep.Decoration aStep) {
        //? if forge {
        return new ForgeBiomeModifiers.AddFeaturesBiomeModifier(aBiomes, aFeatures, aStep);
        //?} else {
        /*return new BiomeModifiers.AddFeaturesBiomeModifier(aBiomes, aFeatures, aStep);
        *///?}
    }

    /**
     * 17 configured features: vanilla {@code Feature.ORE} with a single target
     * {@code stone_ore_replaceables -> GTStoneBlocks.<snake>(STONE variant)} at the upstream
     * blob size clamped to the vanilla codec cap ({@link GT6Worldgen#oreBlobSize()} — the
     * raw 200 trips {@code Codec.intRange(0, 64)}, OreConfiguration.java:14/:15 both legs;
     * the GTCEu GTConfiguredFeatures blob form; the 2-arg ctor pins discard-chance 0
     * — the stone blob never carries buried-ore logic).
     */
    public static void bootstrapConfigured(
        //? if forge {
        BootstapContext<ConfiguredFeature<?, ?>> ctx
        //?} else {
        /*BootstrapContext<ConfiguredFeature<?, ?>> ctx
        *///?}
    ) {
        for (int i = 0; i < GT6Worldgen.CONFIGURED_KEYS.size(); i++) {
            String tSnake = GT6Worldgen.BLOB_STONES.get(i).snake();
            FeatureUtils.register(ctx, GT6Worldgen.CONFIGURED_KEYS.get(i), Feature.ORE,
                    new OreConfiguration(
                            List.of(OreConfiguration.target(new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES),
                                    GTStoneBlocks.block(tSnake, StoneVariant.STONE).get().defaultBlockState())),
                            GT6Worldgen.oreBlobSize()));
        }
        // task p30-w6-t1-trees-nine — the 9 tree configured features: the registered
        // GT6TreeFeature instance per kind over NoneFeatureConfiguration (zero JSON
        // config face; the Feature carries the shape). The double casts bind the
        // wildcard key/feature pair to the FC-typed register overload.
        for (int i = 0; i < GT6Worldgen.TREE_CONFIGURED_KEYS.size(); i++) {
            @SuppressWarnings("unchecked")
            Feature<NoneFeatureConfiguration> tFeature = (Feature<NoneFeatureConfiguration>) GT6Features.TREE_FEATURES.get(i);
            FeatureUtils.register(ctx, GT6Worldgen.TREE_CONFIGURED_KEYS.get(i), tFeature,
                    NoneFeatureConfiguration.INSTANCE);
        }
        bootstrapSurfaceConfigured(ctx); // task p30-w6-rocks-sticks — tail-append
        bootstrapPlantsConfigured(ctx); // task p30-w6-t2-surface-blocks — tail-append
        // task p30-w6-t3-large-veins — the ONE large-vein configured feature: the registered
        // GT6LargeVeinFeature instance over the 40-row vein table ({@link #LARGE_VEIN_TABLE};
        // the table rides the config JSON — the card spec ② tier-a face).
        FeatureUtils.register(ctx, GT6Worldgen.LARGE_VEINS_CONFIGURED, GT6Features.LARGE_VEINS,
                new GTVeinConfig.Table(LARGE_VEIN_TABLE));
        // task p31-strata-lens — the ONE strata-lens configured feature: the registered
        // GT6StrataLensFeature instance over the 5-row marker-stone lens table
        // ({@link #STRATA_LENS_TABLE}; the table rides the config JSON — the same tier-a
        // face as the vein table).
        FeatureUtils.register(ctx, GT6Worldgen.STRATA_LENSES_CONFIGURED, GT6Features.STRATA_LENSES,
                new GTLensConfig.Table(STRATA_LENS_TABLE));
        bootstrapOreConfigured(ctx); // task p30-w6-small-ore-datagen — tail-append
    }

    /**
     * 17 placed features: 1/100 chunk attempts (upstream probability 100 = WorldgenBlob
     * .java:55 {@code nextInt(mProbability) == 0}), square-spread XZ, biome filter, uniform
     * Y in [0, 120] (Loader_Worldgen.java:655 overworld row) — the GTCEu GTPlacedFeatures
     * RED_GRANITE_BLOB modifier chain with the upstream numbers.
     */
    public static void bootstrapPlaced(
        //? if forge {
        BootstapContext<PlacedFeature> ctx
        //?} else {
        /*BootstrapContext<PlacedFeature> ctx
        *///?}
    ) {
        HolderGetter<ConfiguredFeature<?, ?>> tFeatures = ctx.lookup(Registries.CONFIGURED_FEATURE);
        for (int i = 0; i < GT6Worldgen.PLACED_KEYS.size(); i++) {
            PlacementUtils.register(ctx, GT6Worldgen.PLACED_KEYS.get(i),
                    tFeatures.getOrThrow(GT6Worldgen.CONFIGURED_KEYS.get(i)),
                    RarityFilter.onAverageOnceEvery(GT6Worldgen.BLOB_PROBABILITY),
                    InSquarePlacement.spread(),
                    BiomeFilter.biome(),
                    HeightRangePlacement.uniform(VerticalAnchor.absolute(GT6Worldgen.OVERWORLD_MIN_Y),
                            VerticalAnchor.absolute(GT6Worldgen.OVERWORLD_MAX_Y)));
        }
        // task p30-w6-t1-trees-nine — the 9 tree placed features: the GTCEu tree modifier
        // chain (GTPlacedFeatures.java:31-43 RUBBER_CHECKED: spread + SurfaceWaterDepth(0)
        // + HEIGHTMAP_TOP_SOLID + BiomeFilter + filteredByBlockSurvival) with the upstream
        // 1/N chunk chance as the RarityFilter (count stays the default 1 = Amount 1)
        for (int i = 0; i < GT6Worldgen.TREE_PLACED_KEYS.size(); i++) {
            PlacementUtils.register(ctx, GT6Worldgen.TREE_PLACED_KEYS.get(i),
                    tFeatures.getOrThrow(GT6Worldgen.TREE_CONFIGURED_KEYS.get(i)),
                    RarityFilter.onAverageOnceEvery(TREE_PROBABILITY.get(i)),
                    InSquarePlacement.spread(),
                    net.minecraft.world.level.levelgen.placement.SurfaceWaterDepthFilter.forMaxDepth(0),
                    PlacementUtils.HEIGHTMAP_TOP_SOLID,
                    BiomeFilter.biome(),
                    PlacementUtils.filteredByBlockSurvival(
                            GT6TreeBlocks.SAPLINGS.get(i).get()));
        }
        bootstrapSurfacePlaced(ctx, tFeatures); // task p30-w6-rocks-sticks — tail-append
        bootstrapPlantsPlaced(ctx, tFeatures); // task p30-w6-t2-surface-blocks — tail-append
        // task p30-w6-t3-large-veins — the large-vein placed feature: ONE attempt per chunk
        // (the default count — the per-chunk origin-grid 5x5 scan lives in the Feature,
        // GT6WorldGenerator.java:95-105), square spread + biome filter as the form.
        PlacementUtils.register(ctx, GT6Worldgen.LARGE_VEINS_PLACED,
                tFeatures.getOrThrow(GT6Worldgen.LARGE_VEINS_CONFIGURED),
                InSquarePlacement.spread(), BiomeFilter.biome());
        // task p31-strata-lens — the strata-lens placed feature: Count 1 CONSTANT +
        // InSquare + BiomeFilter, one attempt per chunk (the per-chunk ±3-chunk origin
        // scan lives in the Feature). The conflict-audit UniformInt TRAP: the count is a
        // CONSTANT integer (CountPlacement.of(int) = ConstantInt both legs,
        // CountPlacement.java:21/:21) — a uniform provider would fork the legs at JSON
        // level (DFU6 wraps {"value":{min,max}}, DFU8 inlines). Y rides the lens row
        // table (the center draw), so no HeightRangePlacement.
        PlacementUtils.register(ctx, GT6Worldgen.STRATA_LENSES_PLACED,
                tFeatures.getOrThrow(GT6Worldgen.STRATA_LENSES_CONFIGURED),
                CountPlacement.of(1), InSquarePlacement.spread(), BiomeFilter.biome());
        bootstrapOrePlaced(ctx, tFeatures); // task p30-w6-small-ore-datagen — tail-append
    }

    /**
     * 17 biome modifiers: one AddFeaturesBiomeModifier per stone, appending the placed
     * feature to every overworld biome at the UNDERGROUND_ORES step (the GTCEu STONE_BLOB
     * step; the upstream stone blob rides the same per-chunk ore pass).
     */
    public static void bootstrapBiomeModifiers(
        //? if forge {
        BootstapContext<BiomeModifier> ctx
        //?} else {
        /*BootstrapContext<BiomeModifier> ctx
        *///?}
    ) {
        HolderGetter<Biome> tBiomes = ctx.lookup(Registries.BIOME);
        HolderGetter<PlacedFeature> tPlaced = ctx.lookup(Registries.PLACED_FEATURE);
        HolderSet<Biome> tOverworld = tBiomes.getOrThrow(BiomeTags.IS_OVERWORLD);
        for (int i = 0; i < BIOME_MODIFIER_KEYS.size(); i++) {
            ctx.register(BIOME_MODIFIER_KEYS.get(i), addFeatures(tOverworld,
                    HolderSet.direct(tPlaced.getOrThrow(GT6Worldgen.PLACED_KEYS.get(i))),
                    GenerationStep.Decoration.UNDERGROUND_ORES));
        }
        // task p30-w6-t1-trees-nine — the 9 tree biome modifiers: one per kind, keyed on
        // the #gt6:trees/<snake> biome tag (GT6BiomeTags; the tag IS the datapack
        // per-feature biome face) at the VEGETAL_DECORATION step (the vanilla tree pass —
        // the enum has no TREES member, the upstream surface-tree surface pass rides it)
        for (int i = 0; i < TREE_BIOME_MODIFIER_KEYS.size(); i++) {
            ctx.register(TREE_BIOME_MODIFIER_KEYS.get(i), addFeatures(
                    tBiomes.getOrThrow(GT6BiomeTags.treeTag(GT6TreeBlocks.KINDS.get(i).snake())),
                    HolderSet.direct(tPlaced.getOrThrow(GT6Worldgen.TREE_PLACED_KEYS.get(i))),
                    GenerationStep.Decoration.VEGETAL_DECORATION));
        }
        bootstrapSurfaceBiomeModifiers(ctx, tBiomes, tPlaced); // task p30-w6-rocks-sticks — tail-append
        bootstrapPlantsBiomeModifiers(ctx, tBiomes, tPlaced); // task p30-w6-t2-surface-blocks — tail-append
        // task p30-w6-t3-large-veins — the large-vein biome modifier: EVERY overworld biome
        // (the research.p30-w6-vein-boundary impl note: upstream large veins have NO biome
        // gate — the generate signature has no biome parameter — and a per-biome split would
        // carve holes into any vein crossing a biome border), at the UNDERGROUND_ORES step.
        ctx.register(biomeModifierKeyOf("large_veins"), addFeatures(tOverworld,
                HolderSet.direct(tPlaced.getOrThrow(GT6Worldgen.LARGE_VEINS_PLACED)),
                GenerationStep.Decoration.UNDERGROUND_ORES));
        // task p31-strata-lens — the strata-lens biome modifier: EVERY overworld biome
        // (the research.p30-w6-vein-boundary impl note carried over: a per-biome split
        // would carve holes into any lens crossing a biome border), at the
        // UNDERGROUND_ORES step.
        ctx.register(biomeModifierKeyOf("strata_lenses"), addFeatures(tOverworld,
                HolderSet.direct(tPlaced.getOrThrow(GT6Worldgen.STRATA_LENSES_PLACED)),
                GenerationStep.Decoration.UNDERGROUND_ORES));
        bootstrapOreBiomeModifiers(ctx, tBiomes, tPlaced); // task p30-w6-small-ore-datagen — tail-append
    }

    // ------------------------------------------------------------------
    // The surface deco band (task p30-w6-rocks-sticks). Structure:
    // - configured overworld_surface_rocks = Feature.RANDOM_SELECTOR over three
    //   INLINE placed features (PlacementUtils.inlinePlaced form — the vanilla
    //   patch-feature posture; the inner chains never enter any biome's feature
    //   list, so they carry NO BiomeFilter) wrapping SIMPLE_BLOCK configured
    //   features, one per rock, with the WorldgenRocks.java:63 lottery chances;
    // - configured overworld_surface_stick = the bare SIMPLE_BLOCK;
    // - placed overworld_surface_rocks = RarityFilter(3)+Count(2)+BiomeFilter
    //   (the WorldgenOnSurface ray gates: amount=2/probability=3,
    //   Loader_Worldgen.java:618); placed overworld_surface_sticks_*= the three
    //   WorldgenSticks.java:53-55 groups (rarity 2, count 6/4/2);
    // - four biome modifiers over the gt6 biome tags at VEGETAL_DECORATION.
    // ------------------------------------------------------------------

    /** The surface band's four biome-modifier keys, worldgen order (rocks + the three stick groups). */
    public static final List<ResourceKey<BiomeModifier>> SURFACE_BIOME_MODIFIER_KEYS = List.of(
            biomeModifierKeyOf("overworld_surface_rocks"),
            biomeModifierKeyOf("overworld_surface_sticks_dense"),
            biomeModifierKeyOf("overworld_surface_sticks_moderate"),
            biomeModifierKeyOf("overworld_surface_sticks_sparse"));

    private static ResourceKey<BiomeModifier> biomeModifierKeyOf(String aPath) {
        return ResourceKey.create(biomeModifierRegistryKey(), ResourceLocation.fromNamespaceAndPath("gt6", aPath));
    }

    /** The ground-contact predicate (WorldgenRocks.java:60 grass|ground|sand; WorldgenSticks.java:62 grass|ground — gravel is the modern ground arm). */
    private static BlockPredicate groundContact(boolean aRocks) {
        return BlockPredicate.allOf(
                BlockPredicate.replaceable(), // WD.easyRep (WorldgenRocks.java:63) — the tall-grass slot is replaceable
                aRocks
                        ? BlockPredicate.anyOf(
                                BlockPredicate.matchesTag(new Vec3i(0, -1, 0), BlockTags.DIRT),
                                BlockPredicate.matchesBlocks(new Vec3i(0, -1, 0), Blocks.SAND, Blocks.RED_SAND, Blocks.GRAVEL))
                        : BlockPredicate.anyOf(
                                BlockPredicate.matchesTag(new Vec3i(0, -1, 0), BlockTags.DIRT),
                                BlockPredicate.matchesBlocks(new Vec3i(0, -1, 0), Blocks.GRAVEL)));
    }

    /** The sky-ray position chain (square spread + surface heightmap + ground contact) — shared by the sticks' outer chains and the rocks' inline inner chains. */
    private static PlacementModifier[] surfaceRayChain(boolean aRocks) {
        return new PlacementModifier[] {InSquarePlacement.spread(), PlacementUtils.HEIGHTMAP,
                BlockPredicateFilter.forPredicate(groundContact(aRocks))};
    }

    /** One inline placed SIMPLE_BLOCK for the rocks lottery: the sky-ray chain over the bare block. */
    private static Holder<PlacedFeature> inlineSurfaceFeature(Block aBlock, boolean aRocks) {
        return PlacementUtils.inlinePlaced(
                Holder.direct(new ConfiguredFeature<>(Feature.SIMPLE_BLOCK,
                        new SimpleBlockConfiguration(BlockStateProvider.simple(aBlock.defaultBlockState())))),
                surfaceRayChain(aRocks));
    }

    private static void bootstrapSurfaceConfigured(
        //? if forge {
        BootstapContext<ConfiguredFeature<?, ?>> ctx
        //?} else {
        /*BootstrapContext<ConfiguredFeature<?, ?>> ctx
        *///?}
    ) {
        FeatureUtils.register(ctx, GT6Worldgen.SURFACE_ROCKS_CONFIGURED, Feature.RANDOM_SELECTOR,
                new RandomFeatureConfiguration(
                        List.of(new WeightedPlacedFeature(inlineSurfaceFeature(GT6SurfaceBlocks.SURFACE_ROCK_STONE.get(), true),
                                        GT6Worldgen.SURFACE_ROCK_CHANCE_STONE),
                                new WeightedPlacedFeature(inlineSurfaceFeature(GT6SurfaceBlocks.SURFACE_ROCK_FLINT.get(), true),
                                        GT6Worldgen.SURFACE_ROCK_CHANCE_FLINT)),
                        inlineSurfaceFeature(GT6SurfaceBlocks.SURFACE_ROCK_METEORITE.get(), true))); // the default: 1/12 of the NBT half = 1/24 joint
        FeatureUtils.register(ctx, GT6Worldgen.SURFACE_STICK_CONFIGURED, Feature.SIMPLE_BLOCK,
                new SimpleBlockConfiguration(BlockStateProvider.simple(GT6SurfaceBlocks.SURFACE_STICK.get().defaultBlockState())));
    }

    private static void bootstrapSurfacePlaced(
        //? if forge {
        BootstapContext<PlacedFeature> ctx, HolderGetter<ConfiguredFeature<?, ?>> aFeatures
        //?} else {
        /*BootstrapContext<PlacedFeature> ctx, HolderGetter<ConfiguredFeature<?, ?>> aFeatures
        *///?}
    ) {
        // The rocks: 2 ray targets (amount) x the 1/3 gate (probability), the lottery
        // lives in the configured RANDOM_SELECTOR. The outer chain needs NO square/
        // heightmap — the inline inner chains do that per attempt.
        PlacementUtils.register(ctx, GT6Worldgen.SURFACE_ROCKS_PLACED,
                aFeatures.getOrThrow(GT6Worldgen.SURFACE_ROCKS_CONFIGURED),
                RarityFilter.onAverageOnceEvery(GT6Worldgen.SURFACE_ROCKS_PROBABILITY),
                CountPlacement.of(GT6Worldgen.SURFACE_ROCKS_AMOUNT),
                BiomeFilter.biome());
        // The sticks: the three biome-group ray counts (x3/x2/x1 on mAmount=2), each 1/2 gated.
        // The POSITION ANCHORS (square spread + surface heightmap + ground contact) ride the
        // outer chain — without them the decoration origin (chunk corner, y = build-bottom)
        // reaches the feature and the SIMPLE_BLOCK never lands (the live-scan finding, the
        // probe6 T1/T2 split: anchored chain 210 chunk hits, anchor-less chain 0).
        for (int i = 0; i < GT6Worldgen.STICKS_GROUP_PATHS.size(); i++) {
            int tCount = new int[] {GT6Worldgen.STICKS_DENSE_COUNT, GT6Worldgen.STICKS_MODERATE_COUNT,
                    GT6Worldgen.STICKS_SPARSE_COUNT}[i];
            PlacementModifier[] tRay = surfaceRayChain(false);
            PlacementUtils.register(ctx, GT6Worldgen.placedKeyOf(GT6Worldgen.STICKS_GROUP_PATHS.get(i)),
                    aFeatures.getOrThrow(GT6Worldgen.SURFACE_STICK_CONFIGURED),
                    RarityFilter.onAverageOnceEvery(GT6Worldgen.STICKS_PROBABILITY),
                    CountPlacement.of(tCount),
                    tRay[0], tRay[1], tRay[2],
                    BiomeFilter.biome());
        }
    }

    // ------------------------------------------------------------------
    // The small-ore band (task p30-w6-small-ore-datagen). Structure: 91
    // (row, dim) placement pairs (GTOreWorldgen.placementPairs, the upstream
    // GEN-flag walk) x {configured = vanilla Feature.ORE size=1 over the
    // WD.setSmallOre host targets, placed = Count(UniformInt)+InSquare+
    // HeightRange uniform+BiomeFilter}, then 3 biome modifiers
    // (IS_OVERWORLD/IS_NETHER/IS_END at UNDERGROUND_ORES) hanging the per-dim
    // placed sets off the vanilla dimension tags. Zero new blocks/features —
    // pure JSON consumption of the ore-1 ore_small universe.
    // ------------------------------------------------------------------

    /** The 3 small-ore biome-modifier keys, Dim order (overworld/nether/end). */
    public static final List<ResourceKey<BiomeModifier>> ORE_BIOME_MODIFIER_KEYS = List.of(
            biomeModifierKeyOf("ore_small_overworld"),
            biomeModifierKeyOf("ore_small_nether"),
            biomeModifierKeyOf("ore_small_end"));

    private static void bootstrapOreConfigured(
        //? if forge {
        BootstapContext<ConfiguredFeature<?, ?>> ctx
        //?} else {
        /*BootstrapContext<ConfiguredFeature<?, ?>> ctx
        *///?}
    ) {
        for (GTOreWorldgen.Placement tPair : GTOreWorldgen.placementPairs()) {
            OreDictMaterial tMaterial = GTOreWorldgen.resolve(tPair.row());
            FeatureUtils.register(ctx, GTOreWorldgen.configuredKey(tPair.row(), tPair.dim()), Feature.ORE,
                    // size=2 (GTOreWorldgen.ORE_SIZE): size=1 is mathematically inert — the walk
                    // sphere never reaches a block center from an integer origin (live-run 0/20)
                    new OreConfiguration(oreTargets(tMaterial, tPair.dim()), GTOreWorldgen.ORE_SIZE));
        }
    }

    /**
     * The WD.setSmallOre host face per dimension (WD.java:765-780), in
     * {@link GTOreWorldgen#hostPaths} rule order — the stone/deepslate tags first (the
     * vanilla OreFeatures.java:49-60 dual-target canon carries the y<0 deepslate split
     * on the HOST tag, no per-y feature pairs), the 17 GT stone blob anchors, the
     * gravel/sand fallbacks; nether the base_stone_nether tag; end end_stone.
     */
    private static List<OreConfiguration.TargetBlockState> oreTargets(OreDictMaterial aMaterial, GTOreWorldgen.Dim aDim) {
        if (aDim == GTOreWorldgen.Dim.NETHER) {
            return List.of(OreConfiguration.target(new TagMatchTest(BlockTags.BASE_STONE_NETHER), smallState("netherrack", aMaterial)));
        }
        if (aDim == GTOreWorldgen.Dim.END) {
            return List.of(OreConfiguration.target(new BlockMatchTest(Blocks.END_STONE), smallState("endstone", aMaterial)));
        }
        List<OreConfiguration.TargetBlockState> rTargets = new ArrayList<>(21);
        rTargets.add(OreConfiguration.target(new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES), smallState("stone", aMaterial)));
        rTargets.add(OreConfiguration.target(new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES), smallState("deepslate", aMaterial)));
        for (int i = GTOreWorldgen.GT_STONE_FAMILY_START; i < GT6OreBlocks.FAMILIES.size(); i++) {
            GT6OreBlocks.OreFamily tFamily = GT6OreBlocks.FAMILIES.get(i);
            rTargets.add(OreConfiguration.target(new BlockMatchTest(tFamily.stoneAnchor().get()), smallState(tFamily.snake(), aMaterial)));
        }
        rTargets.add(OreConfiguration.target(new BlockMatchTest(Blocks.GRAVEL), smallState("gravel", aMaterial)));
        rTargets.add(OreConfiguration.target(new BlockMatchTest(Blocks.SAND), smallState("sand", aMaterial)));
        return rTargets;
    }

    /** The registered small-ore block of a (family, material) pair (the ore-1 universe — zero new blocks). */
    private static net.minecraft.world.level.block.state.BlockState smallState(String aFamilySnake, OreDictMaterial aMaterial) {
        return GT6OreBlocks.get(GTOreWorldgen.oreFamily(aFamilySnake), GT6OreBlocks.FormKind.SMALL, aMaterial)
                .get().defaultBlockState();
    }

    private static void bootstrapOrePlaced(
        //? if forge {
        BootstapContext<PlacedFeature> ctx, HolderGetter<ConfiguredFeature<?, ?>> aFeatures
        //?} else {
        /*BootstrapContext<PlacedFeature> ctx, HolderGetter<ConfiguredFeature<?, ?>> aFeatures
        *///?}
    ) {
        for (GTOreWorldgen.Placement tPair : GTOreWorldgen.placementPairs()) {
            GTOreWorldgen.SmallOreRow tRow = tPair.row();
            PlacementUtils.register(ctx, GTOreWorldgen.placedKey(tRow, tPair.dim()),
                    aFeatures.getOrThrow(GTOreWorldgen.configuredKey(tRow, tPair.dim())),
                    // count = the constant max(1, amount/2) — the :61 range lower bound, the
                    // density-exact cross-leg face (UniformInt is dispatch-divergent per leg,
                    // GTOreWorldgen.veinCount javadoc)
                    CountPlacement.of(GTOreWorldgen.veinCount(tRow)),
                    InSquarePlacement.spread(),
                    HeightRangePlacement.uniform(VerticalAnchor.absolute(tRow.minY()),
                            VerticalAnchor.absolute(GTOreWorldgen.placedMaxY(tRow, tPair.dim()))),
                    BiomeFilter.biome());
        }
    }

    private static void bootstrapOreBiomeModifiers(
        //? if forge {
        BootstapContext<BiomeModifier> ctx, HolderGetter<Biome> aBiomes, HolderGetter<PlacedFeature> aPlaced
        //?} else {
        /*BootstrapContext<BiomeModifier> ctx, HolderGetter<Biome> aBiomes, HolderGetter<PlacedFeature> aPlaced
        *///?}
    ) {
        // one modifier per vanilla dimension tag, the dim's whole placed set at the ore
        // pass (spec ④); IS_OVERWORLD/IS_NETHER/IS_END ride Dim ordinal order.
        TagKey<Biome>[] tDimTags = new TagKey[] {BiomeTags.IS_OVERWORLD, BiomeTags.IS_NETHER, BiomeTags.IS_END};
        for (int i = 0; i < ORE_BIOME_MODIFIER_KEYS.size(); i++) {
            GTOreWorldgen.Dim tDim = GTOreWorldgen.Dim.values()[i];
            List<Holder<PlacedFeature>> tHolders = new ArrayList<>(38);
            for (GTOreWorldgen.Placement tPair : GTOreWorldgen.placementPairs()) {
                if (tPair.dim() == tDim) tHolders.add(aPlaced.getOrThrow(GTOreWorldgen.placedKey(tPair.row(), tPair.dim())));
            }
            ctx.register(ORE_BIOME_MODIFIER_KEYS.get(i), addFeatures(aBiomes.getOrThrow(tDimTags[i]),
                    HolderSet.direct(tHolders),
                    GenerationStep.Decoration.UNDERGROUND_ORES));
        }
    }

    private static void bootstrapSurfaceBiomeModifiers(
        //? if forge {
        BootstapContext<BiomeModifier> ctx, HolderGetter<Biome> aBiomes, HolderGetter<PlacedFeature> aPlaced
        //?} else {
        /*BootstrapContext<BiomeModifier> ctx, HolderGetter<Biome> aBiomes, HolderGetter<PlacedFeature> aPlaced
        *///?}
    ) {        // VEGETAL_DECORATION — the surface deco step (the upstream surface pass rides
        // the per-chunk decoration, not the ore pass the blobs use).
        ctx.register(SURFACE_BIOME_MODIFIER_KEYS.get(0), addFeatures(aBiomes.getOrThrow(GT6Worldgen.SURFACE_ROCKS_BIOMES),
                HolderSet.direct(aPlaced.getOrThrow(GT6Worldgen.SURFACE_ROCKS_PLACED)),
                GenerationStep.Decoration.VEGETAL_DECORATION));
        for (int i = 0; i < GT6Worldgen.STICKS_GROUP_PATHS.size(); i++) {
            TagKey<Biome> tTag = new TagKey[] {GT6Worldgen.STICKS_DENSE_BIOMES, GT6Worldgen.STICKS_MODERATE_BIOMES,
                    GT6Worldgen.STICKS_SPARSE_BIOMES}[i];
            ctx.register(SURFACE_BIOME_MODIFIER_KEYS.get(i + 1), addFeatures(aBiomes.getOrThrow(tTag),
                    HolderSet.direct(aPlaced.getOrThrow(GT6Worldgen.placedKeyOf(GT6Worldgen.STICKS_GROUP_PATHS.get(i)))),
                    GenerationStep.Decoration.VEGETAL_DECORATION));
        }
    }

    // ------------------------------------------------------------------
    // The surface-plants + soil band (task p30-w6-t2-surface-blocks):
    // - glowtus / bush = SIMPLE_BLOCK over the WorldgenOnSurface ray gates
    //   (amount x probability -> Count+RarityFilter); glowtus anchors on the
    //   water surface (HEIGHTMAP + the below-is-water predicate, the
    //   WorldgenGlowtus.java:55 anywater face), the bush on the ground
    //   contact (the sticks surfaceRayChain(false) shape);
    // - black sand / turf / the clay pit = Feature.DISK over the vanilla
    //   DISK_CLAY idiom (MiscOverworldFeatures.java:69-75 + the OCEAN_FLOOR
    //   anchor + matchesFluids filter, MiscOverworldPlacements.java:113-122),
    //   the upstream nextInt(divider) chunk gates -> RarityFilter verbatim;
    // - the 4 fallen logs = the GT6FallenLogFeature instances over the
    //   Loader_Worldgen.java:603-606 gates (the ground/water/snow faces ride
    //   INSIDE the feature — the multi-block shapes own them);
    // - 9 biome modifiers: the plant+log rows at VEGETAL_DECORATION, the soil
    //   disks at LOCAL_MODIFICATIONS (the vanilla disk pass).
    // ------------------------------------------------------------------

    /** The band's nine biome-modifier keys, worldgen order (glowtus/bush/blacksand/turf/pit + the 4 logs). */
    public static final List<ResourceKey<BiomeModifier>> PLANT_BIOME_MODIFIER_KEYS = List.of(
            biomeModifierKeyOf(GT6Worldgen.GLOWTUS_PATH),
            biomeModifierKeyOf(GT6Worldgen.BUSH_PATH),
            biomeModifierKeyOf(GT6Worldgen.BLACKSAND_PATH),
            biomeModifierKeyOf(GT6Worldgen.TURF_PATH),
            biomeModifierKeyOf(GT6Worldgen.PIT_CLAY_PATH),
            biomeModifierKeyOf("log_dry"),
            biomeModifierKeyOf("log_rotten"),
            biomeModifierKeyOf("log_mossy"),
            biomeModifierKeyOf("log_frozen"));

    private static void bootstrapPlantsConfigured(
        //? if forge {
        BootstapContext<ConfiguredFeature<?, ?>> ctx
        //?} else {
        /*BootstrapContext<ConfiguredFeature<?, ?>> ctx
        *///?}
    ) {
        FeatureUtils.register(ctx, GT6Worldgen.GLOWTUS_CONFIGURED, Feature.SIMPLE_BLOCK,
                new SimpleBlockConfiguration(BlockStateProvider.simple(GT6SurfaceBlocks.GLOWTUS.get().defaultBlockState())));
        FeatureUtils.register(ctx, GT6Worldgen.BUSH_CONFIGURED, Feature.SIMPLE_BLOCK,
                new SimpleBlockConfiguration(BlockStateProvider.simple(GT6SurfaceBlocks.BERRY_BUSH.get().defaultBlockState())));
        BlockPredicate tSoil = BlockPredicate.matchesBlocks(Blocks.DIRT, Blocks.SAND, Blocks.RED_SAND,
                Blocks.GRAVEL, Blocks.CLAY); // the WorldgenBlackSand.java:62 / WorldgenPit.java:67-69 replaceable set
        FeatureUtils.register(ctx, GT6Worldgen.BLACKSAND_CONFIGURED, Feature.DISK, new DiskConfiguration(
                RuleBasedBlockStateProvider.simple(GT6SurfaceBlocks.BLACK_SAND.get()), tSoil,
                GT6Worldgen.SOIL_DISK_RADIUS, GT6Worldgen.BLACKSAND_HALF_HEIGHT));
        FeatureUtils.register(ctx, GT6Worldgen.TURF_CONFIGURED, Feature.DISK, new DiskConfiguration(
                RuleBasedBlockStateProvider.simple(GT6SurfaceBlocks.TURF.get()),
                BlockPredicate.matchesBlocks(Blocks.DIRT, Blocks.GRASS_BLOCK), // the grass top joins the pair (declared)
                GT6Worldgen.SOIL_DISK_RADIUS, GT6Worldgen.TURF_HALF_HEIGHT));
        FeatureUtils.register(ctx, GT6Worldgen.PIT_CLAY_CONFIGURED, Feature.DISK, new DiskConfiguration(
                RuleBasedBlockStateProvider.simple(Blocks.CLAY), tSoil, // the upstream pit places Blocks.clay verbatim
                GT6Worldgen.SOIL_DISK_RADIUS, GT6Worldgen.PIT_CLAY_HALF_HEIGHT));
        for (int i = 0; i < GT6Worldgen.FALLEN_LOG_CONFIGURED_KEYS.size(); i++) {
            @SuppressWarnings("unchecked")
            Feature<NoneFeatureConfiguration> tFeature =
                    (Feature<NoneFeatureConfiguration>) GT6Features.FALLEN_LOG_FEATURES.get(i);
            FeatureUtils.register(ctx, GT6Worldgen.FALLEN_LOG_CONFIGURED_KEYS.get(i), tFeature,
                    NoneFeatureConfiguration.INSTANCE);
        }
    }

    private static void bootstrapPlantsPlaced(
        //? if forge {
        BootstapContext<PlacedFeature> ctx, HolderGetter<ConfiguredFeature<?, ?>> aFeatures
        //?} else {
        /*BootstrapContext<PlacedFeature> ctx, HolderGetter<ConfiguredFeature<?, ?>> aFeatures
        *///?}
    ) {
        // The glowtus: 16 ray targets x the 1/2 gate; the anchor is the free slot
        // above the WATER surface (MOTION_BLOCKING counts water) and the below-is-water
        // predicate is the WorldgenGlowtus.java:55 anywater face.
        PlacementUtils.register(ctx, GT6Worldgen.GLOWTUS_PLACED,
                aFeatures.getOrThrow(GT6Worldgen.GLOWTUS_CONFIGURED),
                RarityFilter.onAverageOnceEvery(GT6Worldgen.GLOWTUS_PROBABILITY),
                CountPlacement.of(GT6Worldgen.GLOWTUS_AMOUNT),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP,
                BlockPredicateFilter.forPredicate(
                        BlockPredicate.matchesBlocks(new Vec3i(0, -1, 0), Blocks.WATER)),
                BiomeFilter.biome());
        // The bush: 1 target x the 1/4 gate over the ground contact (the sticks chain shape).
        PlacementModifier[] tRay = surfaceRayChain(false);
        PlacementUtils.register(ctx, GT6Worldgen.BUSH_PLACED,
                aFeatures.getOrThrow(GT6Worldgen.BUSH_CONFIGURED),
                RarityFilter.onAverageOnceEvery(GT6Worldgen.BUSH_PROBABILITY),
                CountPlacement.of(GT6Worldgen.BUSH_AMOUNT),
                tRay[0], tRay[1], tRay[2],
                BiomeFilter.biome());
        // The three soil disks: the chunk gates verbatim, the vanilla DISK_CLAY anchor
        // chain (square + OCEAN_FLOOR + the water filter where the upstream scan starts
        // at the fluid surface).
        PlacementUtils.register(ctx, GT6Worldgen.BLACKSAND_PLACED,
                aFeatures.getOrThrow(GT6Worldgen.BLACKSAND_CONFIGURED),
                RarityFilter.onAverageOnceEvery(GT6Worldgen.BLACKSAND_DIVIDER),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_TOP_SOLID,
                BlockPredicateFilter.forPredicate(BlockPredicate.matchesFluids(Fluids.WATER)),
                BiomeFilter.biome());
        PlacementUtils.register(ctx, GT6Worldgen.TURF_PLACED,
                aFeatures.getOrThrow(GT6Worldgen.TURF_CONFIGURED),
                RarityFilter.onAverageOnceEvery(GT6Worldgen.TURF_DIVIDER),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP_TOP_SOLID,
                BiomeFilter.biome());
        PlacementUtils.register(ctx, GT6Worldgen.PIT_CLAY_PLACED,
                aFeatures.getOrThrow(GT6Worldgen.PIT_CLAY_CONFIGURED),
                RarityFilter.onAverageOnceEvery(GT6Worldgen.PIT_CLAY_DIVIDER),
                InSquarePlacement.spread(),
                PlacementUtils.HEIGHTMAP,
                BiomeFilter.biome());
        // The fallen logs: the four gates (Loader_Worldgen.java:603-606); the ground
        // checks ride INSIDE the feature (the shapes need the water/snow arms).
        for (int i = 0; i < GT6Worldgen.FALLEN_LOG_PLACED_KEYS.size(); i++) {
            PlacementUtils.register(ctx, GT6Worldgen.FALLEN_LOG_PLACED_KEYS.get(i),
                    aFeatures.getOrThrow(GT6Worldgen.FALLEN_LOG_CONFIGURED_KEYS.get(i)),
                    RarityFilter.onAverageOnceEvery(GT6Worldgen.FALLEN_LOG_PROBABILITY.get(i)),
                    CountPlacement.of(1),
                    InSquarePlacement.spread(),
                    PlacementUtils.HEIGHTMAP,
                    BiomeFilter.biome());
        }
    }

    private static void bootstrapPlantsBiomeModifiers(
        //? if forge {
        BootstapContext<BiomeModifier> ctx, HolderGetter<Biome> aBiomes, HolderGetter<PlacedFeature> aPlaced
        //?} else {
        /*BootstrapContext<BiomeModifier> ctx, HolderGetter<Biome> aBiomes, HolderGetter<PlacedFeature> aPlaced
        *///?}
    ) {
        HolderSet<PlacedFeature>[] tPlaced = new HolderSet[] {
                HolderSet.direct(aPlaced.getOrThrow(GT6Worldgen.GLOWTUS_PLACED)),
                HolderSet.direct(aPlaced.getOrThrow(GT6Worldgen.BUSH_PLACED)),
                HolderSet.direct(aPlaced.getOrThrow(GT6Worldgen.BLACKSAND_PLACED)),
                HolderSet.direct(aPlaced.getOrThrow(GT6Worldgen.TURF_PLACED)),
                HolderSet.direct(aPlaced.getOrThrow(GT6Worldgen.PIT_CLAY_PLACED))};
        TagKey<Biome>[] tTags = new TagKey[] {GT6Worldgen.GLOWTUS_BIOMES, GT6Worldgen.BUSH_BIOMES,
                GT6Worldgen.BLACKSAND_BIOMES, GT6Worldgen.TURF_BIOMES, GT6Worldgen.PIT_CLAY_BIOMES};
        for (int i = 0; i < 5; i++) {
            ctx.register(PLANT_BIOME_MODIFIER_KEYS.get(i), addFeatures(aBiomes.getOrThrow(tTags[i]), tPlaced[i],
                    i < 2 ? GenerationStep.Decoration.VEGETAL_DECORATION // the plant rows ride the deco pass
                          : GenerationStep.Decoration.LOCAL_MODIFICATIONS)); // the soil disks ride the vanilla disk pass
        }
        for (int i = 0; i < GT6Worldgen.FALLEN_LOG_PLACED_KEYS.size(); i++) {
            TagKey<Biome> tLogTag = new TagKey[] {GT6Worldgen.LOG_DRY_BIOMES, GT6Worldgen.LOG_ROTTEN_BIOMES,
                    GT6Worldgen.LOG_MOSSY_BIOMES, GT6Worldgen.LOG_FROZEN_BIOMES}[i];
            // the frozen row rides TOP_LAYER_MODIFICATION (appends AFTER the vanilla
            // FREEZE_TOP_LAYER in the same step list): its contact face needs the snow
            // layer ALREADY on the ground — the vegetal step runs before the freeze and
            // the snow arm could never fire (the forge-leg live-scan finding, 0 hits).
            GenerationStep.Decoration tStep = mKindOf(i) == GT6FallenLogFeature.Kind.FROZEN
                    ? GenerationStep.Decoration.TOP_LAYER_MODIFICATION
                    : GenerationStep.Decoration.VEGETAL_DECORATION;
            ctx.register(PLANT_BIOME_MODIFIER_KEYS.get(5 + i), addFeatures(aBiomes.getOrThrow(tLogTag),
                    HolderSet.direct(aPlaced.getOrThrow(GT6Worldgen.FALLEN_LOG_PLACED_KEYS.get(i))),
                    tStep));
        }
    }

    /** The fallen-log kind of a FALLEN_LOG_PATHS index (the step face above). */
    private static GT6FallenLogFeature.Kind mKindOf(int aIndex) {
        return GT6FallenLogFeature.Kind.values()[aIndex];
    }
    // The large-vein band (task p30-w6-t3-large-veins) — the 40-row vein table,
    // Loader_Worldgen.java:886-925 row-for-row. Column order (WorldgenOresLarge.java:54
    // ctor): name / minY / maxY / weight / density / size / OreTop / OreBottom /
    // OreBetween / OreSpread. The "overworld" column = the row listed ORE_OVERWORLD
    // (:886-916 true; the :917-925 Mars/End/Moon/Betweenlands rows false — the draw
    // must sum over the dimension's own rows, GT6WorldGenerator.java:93). spawnDistance
    // is 0 on every row (no row passes the DistanceFromSpawn arg) and the indicator
    // flag is true on every row (the :886 ctor's first T). The materials reference the
    // MT constants directly, so the JSON carries the canonical mNameInternal strings.
    // ------------------------------------------------------------------

    /** The row helper: an overworld row (indicator on, distance 0). */
    private static GTVeinConfig vein(String aName, int aMinY, int aMaxY, int aWeight, int aDensity, int aSize,
            OreDictMaterial aTop, OreDictMaterial aBottom, OreDictMaterial aBetween, OreDictMaterial aSpread) {
        return new GTVeinConfig(aName, aMinY, aMaxY, aWeight, aDensity, aSize, 0, true, true, aTop, aBottom, aBetween, aSpread);
    }

    /** The row helper for the offworld rows (:917-925 — never drawn overworld, kept for the table census). */
    private static GTVeinConfig veinOffworld(String aName, int aMinY, int aMaxY, int aWeight, int aDensity, int aSize,
            OreDictMaterial aTop, OreDictMaterial aBottom, OreDictMaterial aBetween, OreDictMaterial aSpread) {
        return new GTVeinConfig(aName, aMinY, aMaxY, aWeight, aDensity, aSize, 0, true, false, aTop, aBottom, aBetween, aSpread);
    }

    /** The ONE 40-row large-vein table — the card spec ② "40 脉合一张 JSON 脉表". */
    public static final List<GTVeinConfig> LARGE_VEIN_TABLE = List.of(
        vein("ore.large.lignite"     , 50, 130, 160, 8, 32, MT.Lignite                     , MT.Lignite                     , MT.Lignite                     , MT.Coal               ), // :886
        vein("ore.large.coal"        , 50,  80,  80, 6, 32, MT.Coal                         , MT.Coal                         , MT.Coal                         , MT.Lignite            ), // :887
        vein("ore.large.apatite"     , 40,  60,  60, 3, 16, MT.Apatite                      , MT.Apatite                      , MT.PhosphorusBlue               , MT.PO4                ), // :888
        vein("ore.large.lapis"       , 20,  50,  40, 5, 16, MT.Lazurite                     , MT.Sodalite                     , MT.Lapis                        , MT.Azurite            ), // :889
        vein("ore.large.bauxite"     , 50,  90,  80, 4, 24, MT.OREMATS.Bauxite              , MT.OREMATS.Bauxite              , MT.OREMATS.Bauxite              , MT.OREMATS.Ilmenite   ), // :890
        vein("ore.large.iodinesalt"  , 50,  60,  30, 3, 24, MT.KIO3                         , MT.NaCl                         , MT.OREMATS.Borax                , MT.OREMATS.Zeolite    ), // :891
        vein("ore.large.rocksalt"    , 50,  60,  30, 3, 24, MT.KCl                          , MT.OREMATS.Coltan               , MT.OREMATS.Lepidolite           , MT.OREMATS.Spodumene  ), // :892
        vein("ore.large.asbestos"    , 10,  40,  30, 3, 16, MT.OREMATS.Chromite             , MT.Talc                         , MT.Gypsum                       , MT.Asbestos           ), // :893
        vein("ore.large.sapphire"    , 10,  40,  30, 3, 16, MT.BlueSapphire                 , MT.OrangeSapphire               , MT.YellowSapphire               , MT.Ruby               ), // :894
        vein("ore.large.sapphire2"   , 10,  40,  30, 3, 16, MT.GreenSapphire                , MT.Ruby                         , MT.BlueSapphire                 , MT.PurpleSapphire     ), // :895
        vein("ore.large.garnet"      , 10,  40,  60, 3, 16, MT.Almandine                    , MT.Pyrope                       , MT.Andradite                    , MT.Uvarovite          ), // :896
        vein("ore.large.pitchblende" , 10,  40,  40, 3, 16, MT.OREMATS.Pitchblende          , MT.OREMATS.Pitchblende          , MT.OREMATS.Uraninite            , MT.OREMATS.Uraninite  ), // :897
        vein("ore.large.monazite"    , 10,  40,  30, 3, 16, MT.OREMATS.Bastnasite           , MT.OREMATS.Bastnasite           , MT.Monazite                     , MT.Nd                 ), // :898
        vein("ore.large.diamond"     ,  5,  20,  40, 2, 16, MT.Graphite                     , MT.Graphite                     , MT.Diamond                      , MT.Graphite           ), // :899
        vein("ore.large.galena"      , 30,  60,  40, 5, 16, MT.OREMATS.Galena               , MT.OREMATS.Galena               , MT.Ag                           , MT.Pb                 ), // :900
        vein("ore.large.quartz"      , 40,  80,  60, 3, 16, MT.MilkyQuartz                  , MT.OREMATS.Barite               , MT.CertusQuartz                 , MT.CertusQuartz       ), // :901
        vein("ore.large.peridot"     , 10,  40,  60, 3, 16, MT.OREMATS.Kyanite              , MT.MgCO3                        , MT.Peridot                      , MT.OREMATS.Glauconite ), // :902
        vein("ore.large.gold"        , 20,  30,   5, 3, 16, MT.Pyrite                       , MT.OREMATS.Chalcopyrite         , MT.OREMATS.Arsenopyrite         , MT.Au                 ), // :903
        vein("ore.large.platinum"    , 40,  50,   5, 3, 16, MT.OREMATS.Cooperite            , MT.Pd                           , MT.OREMATS.Sperrylite           , MT.Ir                 ), // :904
        vein("ore.large.molybdenum"  , 20,  50,   5, 3, 16, MT.OREMATS.Wulfenite            , MT.OREMATS.Molybdenite          , MT.Mo                           , MT.OREMATS.Powellite  ), // :905
        vein("ore.large.cassiterite" , 40,  90, 170, 5, 24, MT.OREMATS.Stannite             , MT.OREMATS.Kesterite            , MT.OREMATS.Huebnerite           , MT.OREMATS.Cassiterite), // :906
        vein("ore.large.tungstate"   , 20,  50,  10, 3, 16, MT.OREMATS.Scheelite            , MT.OREMATS.Russellite           , MT.OREMATS.Tungstate            , MT.OREMATS.Pinalite   ), // :907
        vein("ore.large.manganese"   , 20,  30,  20, 3, 16, MT.Grossular                    , MT.Spessartine                  , MT.MnO2                         , MT.OREMATS.Coltan     ), // :908
        vein("ore.large.beryllium"   ,  5,  30,  15, 3, 16, MT.Aquamarine                   , MT.Maxixe                       , MT.Emerald                      , MT.Th                 ), // :909
        vein("ore.large.beryllium2"  ,  5,  30,  15, 3, 16, MT.Bixbite                      , MT.Goshenite                    , MT.Heliodor                     , MT.Morganite          ), // :910
        vein("ore.large.titanium"    , 10,  40,  40, 3, 16, MT.TiO2                         , MT.TiO2                         , MT.Zircon                       , MT.OREMATS.Ilmenite   ), // :911
        vein("ore.large.nickel"      , 10,  40,  40, 3, 16, MT.OREMATS.Garnierite           , MT.Ni                           , MT.OREMATS.Cobaltite            , MT.OREMATS.Pentlandite), // :912
        vein("ore.large.redstone"    , 10,  40,  60, 3, 24, MT.Redstone                     , MT.Redstone                     , MT.Ruby                         , MT.OREMATS.Cinnabar   ), // :913
        vein("ore.large.tetrahedrite", 70, 120, 150, 4, 24, MT.OREMATS.Tetrahedrite         , MT.OREMATS.Tetrahedrite         , MT.Cu                           , MT.OREMATS.Stibnite   ), // :914
        vein("ore.large.iron"        , 10,  40, 120, 4, 24, MT.OREMATS.BrownLimonite        , MT.OREMATS.YellowLimonite       , MT.Fe2O3                        , MT.OREMATS.Malachite  ), // :915
        vein("ore.large.copper"      , 10,  30,  80, 4, 24, MT.OREMATS.Chalcopyrite         , MT.Fe2O3                        , MT.Pyrite                       , MT.Cu                 ), // :916
        veinOffworld("ore.large.adamantium", 10, 120,   5, 2, 16, MT.OREMATS.BrownLimonite  , MT.OREMATS.YellowLimonite       , MT.Fe2O3                        , MT.Adamantine         ), // :917
        veinOffworld("ore.large.naquadah"  , 10,  60,  10, 4, 32, MT.Nq                     , MT.Nq                           , MT.Nq                           , MT.Nq                 ), // :918
        veinOffworld("ore.large.trinium"   , 10,  90, 100, 1, 12, MT.Ke                     , MT.Ke                           , MT.Ke                           , MT.Ke                 ), // :919
        veinOffworld("ore.large.dolamide"  ,  5,  60,  40, 3, 16, MT.OREMATS.DuraniumHexaiodide, MT.OREMATS.DuraniumHexafluoride, MT.OREMATS.DuraniumHexachloride, MT.Dolamide), // :920
        veinOffworld("ore.large.moonmars"  , 10,  90, 240, 1,  8, MT.MgCO3                  , MT.MnO2                         , MT.Al2O3                        , MT.TiO2               ), // :921
        veinOffworld("ore.large.cheese"    , 10,  90, 100, 3, 16, MT.Cheese                 , MT.Cheese                       , MT.Cheese                       , MT.Se                 ), // :922
        veinOffworld("ore.large.desh"      , 10,  90, 100, 3, 16, MT.OREMATS.TritaniumHexafluoride, MT.OREMATS.DuraniumHexaastatide, MT.OREMATS.DuraniumHexabromide, MT.Desh), // :923
        veinOffworld("ore.large.syrmorite" , 30,  45, 160, 2, 32, MT.Syrmorite              , MT.Syrmorite                    , MT.Syrmorite                    , MT.Syrmorite          ), // :924
        veinOffworld("ore.large.octine"    , 10,  25,  40, 1, 32, MT.Octine                 , MT.Octine                       , MT.Octine                       , MT.Octine             )  // :925
    );

    /**
     * The ONE 5-row strata-lens table (task p31-strata-lens) — the card spec's settled
     * marker-stone list, spec order. CLEAN CALIBRATION (the conflict-audit ORE_SIZE
     * lesson): rarity/shape/Y chosen fresh for the mountain-scale lens face, no P30
     * blob/small-ore curve reused. rarity = the weight of the exactly-one origin draw
     * (sum 21); Y = the lens CENTER domain, nextInt(maxY-minY+1); radius rides the
     * {@link GTLensConfig#RADIUS_CODEC_CAP} 48 (the ±3-chunk scan-safety rail);
     * halfHeight is the flattened-blob vertical half-extent. kimberlite is the deepest
     * and most bulbous (its real-world pipe face), granite_red the widest and shallowest.
     */
    public static final List<GTLensConfig> STRATA_LENS_TABLE = List.of(
        new GTLensConfig("marble"     , 5, 32,  96, 44, 10),
        new GTLensConfig("basalt"     , 5, 16,  80, 40,  8),
        new GTLensConfig("kimberlite" , 3,  8,  48, 32, 14),
        new GTLensConfig("granite_red", 4, 32, 104, 48,  9),
        new GTLensConfig("komatiite"  , 4,  8,  64, 36, 12));
}
