package gregtech6.datagen;

import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySetBuilder;
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
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.RarityFilter;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

import java.util.List;
import gregtech6.block.stone.StoneVariant;
import gregtech6.block.tree.GT6TreeKind;
import gregtech6.registry.GT6TreeBlocks;
import gregtech6.registry.GTStoneBlocks;
import gregtech6.worldgen.GT6Features;
import gregtech6.worldgen.GT6Worldgen;

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
     * The 17 biome-modifier keys, GTStoneBlocks.STONES order — one AddFeaturesBiomeModifier
     * row per stone (the upstream per-object config face,
     * {@code worldgenerator.overworld.stone.<material>}, becomes one datapack JSON each).
     */
    public static final List<ResourceKey<BiomeModifier>> BIOME_MODIFIER_KEYS =
            GTStoneBlocks.STONES.stream().map(GTStoneBlocks.StoneSpec::snake)
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
            String tSnake = GTStoneBlocks.STONES.get(i).snake();
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
    }
}
