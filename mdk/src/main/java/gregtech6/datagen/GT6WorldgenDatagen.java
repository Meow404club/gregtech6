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
import gregtech6.registry.GTStoneBlocks;
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
     * {@code stone_ore_replaceables -> GTStoneBlocks.<snake>(STONE variant)} at blob size
     * 200 (the GTCEu GTConfiguredFeatures blob form; the 2-arg ctor pins discard-chance 0
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
                            GT6Worldgen.BLOB_SIZE));
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
    }
}
