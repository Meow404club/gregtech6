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
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.WeightedPlacedFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.RandomFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.DiskConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.WeightedStateProvider;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.block.surface.GT6WildBushBlock;
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
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregtech6.block.stone.StoneVariant;
import gregtech6.block.tree.GT6TreeKind;
import gregtech6.registry.GT6OreBlocks;
import gregtech6.registry.GT6TreeBlocks;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTStoneBlocks;
import gregtech6.registry.GT6SurfaceBlocks;
import gregtech6.worldgen.GT6FallenLogFeature;
import gregtech6.worldgen.GT6Features;
import gregtech6.worldgen.GT6WaterReplaceConfig;
import gregtech6.worldgen.GT6Worldgen;
import gregtech6.worldgen.GTBedrockOreConfig;
import gregtech6.worldgen.GTFluidSpringConfig;
import gregtech6.worldgen.GTLensConfig;
import gregtech6.worldgen.GTVeinConfig;
import gregtech6.worldgen.GTOreWorldgen;

/**
 * The worldgen datagen band (task worldgen-pipeline-skeleton): one
 * {@link RegistrySetBuilder} with three BootstapContexts — configured feature / placed
 * feature / biome modifier — producing 17+17+17 JSONs for the GT6 stone blobs (the
 * {@code WorldgenStone} loop port, Loader_Worldgen.java:654-661; the parameter
 * transcription + key tables live in {@link GT6Worldgen}). Consumed by the
 * {@code DatapackBuiltinEntriesProvider} row appended in GT6DataGenerators (GTCEu
 * data/DataGenerators.java:40 precedent; the provider class import is the other leg
 * seam, handled there).
 *
 * <p>Dual-leg evidence (the biome-modifier triple seam, research card
 * r-worldgen-domain): the registry key is {@code forge:biome_modifier}
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
 * <p>Coexistence + de-vanilla (issue #32, decisions.2026-09-28-devanilla-default — the p26
 * "deliberately not generated" ruling reversed): the biome modifiers still ADD the GT blobs
 * on top, AND the two {@code remove_features} rows below (issue #32 vanilla-deblob) strip
 * the vanilla blobs the user named — the full ore family (the upstream GenerateMinable
 * seven, GT6_Main.java:112 DisableVanillaOres default-T, plus the modern extensions),
 * the stone blobs (granite/diorite/andesite/tuff), dirt/gravel and the lava lakes (the
 * upstream DisableVanillaLakes precedent, GT6_Main.java:112). The amethyst geode was on
 * this list only while the #33 GT geode band lived; the band is reverted (geode-revert,
 * decisions.r8-geode-revert) and the vanilla geode generates again. Per-key vanilla
 * placed-feature names ONLY — the ADD phase runs
 * before REMOVE (ForgeBiomeModifiers Phase :80/:84), and disk/clay/infested/magma stay
 * (the upstream does not suppress them; disks are surface bands, not blobs). The keys
 * were verified against BOTH legs' jars (the 1.20.1 client extract and the 1.21.1
 * neoformruntime client jar, data/minecraft/worldgen/placed_feature/): the ONE leg delta
 * is {@code ore_diamond_medium} (the 1.21.1 diamond split, BiomeDefaultFeatures.java:67)
 * — the {@code //?} fork below, mirrored by the datagen_tree_check normalizer
 * (remove-features-diamond-medium(1.21.1)).
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
     * Task strata-lens: the 5 marker stones (GT6Worldgen.LENS_STONE_SNAKES) ride the
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
     * The 9 tree biome-modifier keys, GT6TreeBlocks.KINDS order (task w6-t1-trees-nine
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
            .add(biomeModifierRegistryKey(), GT6WorldgenDatagen::bootstrapBiomeModifiers)
            // task dungeon-framework: the shelter dungeon rides the same provider —
            // the structure + its random_spread structure_set (the 11-chunk grid JSON).
            .add(Registries.STRUCTURE, GT6WorldgenDatagen::bootstrapStructure)
            .add(Registries.STRUCTURE_SET, GT6WorldgenDatagen::bootstrapStructureSet);

    /**
     * The three builder bands as a key table, BUILDER order — the acceptance ctx-key audit
     * unit. The mirror exists because 1.20.1 RegistrySetBuilder has NO key accessor (the
     * public {@code getEntryKeys()} is a 1.21.1 addition, vanilla-mc 1.21.1
     * RegistrySetBuilder.java:75), so the offline test can only introspect the builder on
     * the neoforge leg and pins the forge leg through this constant the BUILDER rows are
     * textually bound to.
     */
    public static final List<ResourceKey<? extends Registry<?>>> BUILDER_KEYS = List.of(
            Registries.CONFIGURED_FEATURE, Registries.PLACED_FEATURE, biomeModifierRegistryKey(),
            Registries.STRUCTURE, Registries.STRUCTURE_SET);

    // --------------------------------------------------------------- the dungeon band

    /** The structure key — {@code gt6:dungeon} (the JSON at data/gt6/worldgen/structure/dungeon.json). */
    public static final ResourceKey<Structure> DUNGEON_STRUCTURE =
            ResourceKey.create(Registries.STRUCTURE, ResourceLocation.fromNamespaceAndPath("gt6", "dungeon"));

    /** The structure-set key — {@code gt6:dungeon} (data/gt6/worldgen/structure_set/dungeon.json). */
    public static final ResourceKey<StructureSet> DUNGEON_STRUCTURE_SET =
            ResourceKey.create(Registries.STRUCTURE_SET, ResourceLocation.fromNamespaceAndPath("gt6", "dungeon"));

    /**
     * The placement constants (task dungeon-framework): spacing 11 = the upstream
     * chunk-grid period {@code maxSize + 4} (WorldgenDungeonGT.java:144), separation 5
     * keeps the candidate at least mid-cell (the upstream FIXED offset 5 becomes the
     * salt-uniform offset in {@code [0, spacing - separation]} — the documented
     * distribution-equivalent deviation), the salt is arbitrary (vanilla salts are too).
     */
    public static final int DUNGEON_SPACING = 11, DUNGEON_SEPARATION = 5, DUNGEON_SALT = 371266954;

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
     * The remove seam — the issue-#32 twin of {@link #addFeatures}, the
     * {@code <loader>:remove_features} JSON type. The {@code allSteps} factory (present on
     * both legs: ForgeBiomeModifiers.java:91 / BiomeModifiers.java:89) omits the optional
     * {@code steps} field (both codecs default it to every Decoration step:
     * ForgeMod.java:203 / NeoForgeMod.java:245) — the named keys are removed wherever the
     * biome carries them.
     */
    private static BiomeModifier removeFeatures(HolderSet<Biome> aBiomes, HolderSet<PlacedFeature> aFeatures) {
        //? if forge {
        return ForgeBiomeModifiers.RemoveFeaturesBiomeModifier.allSteps(aBiomes, aFeatures);
        //?} else {
        /*return BiomeModifiers.RemoveFeaturesBiomeModifier.allSteps(aBiomes, aFeatures);
        *///?}
    }

    // ------------------------------------------------------------------
    // The vanilla de-blob band (issue #32, decisions.2026-09-28-devanilla-default —
    // the p26 "deliberately not generated" ruling reversed). TWO remove_features
    // rows naming the vanilla placed features the user listed, per key, never per
    // step (disks share the underground_ores step and stay). Key spellings were
    // verified against BOTH legs' vanilla jars (data/minecraft/worldgen/placed_
    // feature/): ore_debris_small (not ore_ancient_debris_small), the lake pair
    // lake_lava_surface / lake_lava_underground (1.18+ ships NO lake_water — the
    // upstream DENY has no modern target). amethyst_geode is deliberately NOT here
    // (geode-revert): the #33 GT geode band that justified its suppression is
    // reverted, so the vanilla geode generates again. The ONE leg delta is
    // ore_diamond_medium — the 1.21.1 diamond split
    // (BiomeDefaultFeatures.java:67): the 1.20.1 registry lacks the key (a
    // getOrThrow would trip the forge datagen), and the 1.21.1 list without it
    // would leave the split's diamonds generating. The leg delta is folded by the
    // datagen_tree_check normalizer remove-features-diamond-medium(1.21.1).
    // ------------------------------------------------------------------

    /** The overworld de-blob biome-modifier key — the {@code gt6:vanilla_deblob_overworld} row. */
    public static final ResourceKey<BiomeModifier> VANILLA_DEBLOB_OVERWORLD_KEY =
            biomeModifierKeyOf("vanilla_deblob_overworld");

    /** The nether de-blob biome-modifier key — the {@code gt6:vanilla_deblob_nether} row. */
    public static final ResourceKey<BiomeModifier> VANILLA_DEBLOB_NETHER_KEY =
            biomeModifierKeyOf("vanilla_deblob_nether");

    /**
     * The overworld suppress list — the vanilla placed-feature PATHS under the
     * {@code minecraft} namespace, jar-listing order (the lake pair, then the
     * ore blobs alphabetically; 28 keys on 1.20.1, 29 on 1.21.1). NOT suppressed on
     * purpose (the card ruling): ore_infested/ore_clay/ore_magma/ore_soul_sand/
     * ore_blackstone/ore_emerald/ore_gravel_nether (the upstream GenerateMinable set
     * never carried them) and the disk family (surface bands, not blobs).
     */
    public static final List<String> VANILLA_DEBLOB_OVERWORLD = List.of(
            "lake_lava_surface",
            "lake_lava_underground",
            "ore_andesite_lower",
            "ore_andesite_upper",
            "ore_coal_lower",
            "ore_coal_upper",
            "ore_copper",
            "ore_copper_large",
            "ore_diamond",
            "ore_diamond_buried",
            "ore_diamond_large",
            //? if neoforge {
            /*"ore_diamond_medium", // the 1.21.1 diamond split (BiomeDefaultFeatures.java:67); absent from the 1.20.1 jar
            *///?}
            "ore_diorite_lower",
            "ore_diorite_upper",
            "ore_dirt",
            "ore_gold",
            "ore_gold_extra",
            "ore_gold_lower",
            "ore_granite_lower",
            "ore_granite_upper",
            "ore_gravel",
            "ore_iron_middle",
            "ore_iron_small",
            "ore_iron_upper",
            "ore_lapis",
            "ore_lapis_buried",
            "ore_redstone",
            "ore_redstone_lower",
            "ore_tuff");

    /**
     * The nether suppress list — the five nether ore blobs (the quartz pair, the nether
     * gold, the ancient debris pair; jar-listing order). ore_gravel_nether stays (the
     * card ruling: the upstream never suppressed gravel).
     */
    public static final List<String> VANILLA_DEBLOB_NETHER = List.of(
            "ore_ancient_debris_large",
            "ore_debris_small",
            "ore_gold_nether",
            "ore_quartz_deltas",
            "ore_quartz_nether");

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
        // task w6-t1-trees-nine — the 9 tree configured features: the registered
        // GT6TreeFeature instance per kind over NoneFeatureConfiguration (zero JSON
        // config face; the Feature carries the shape). The double casts bind the
        // wildcard key/feature pair to the FC-typed register overload.
        for (int i = 0; i < GT6Worldgen.TREE_CONFIGURED_KEYS.size(); i++) {
            @SuppressWarnings("unchecked")
            Feature<NoneFeatureConfiguration> tFeature = (Feature<NoneFeatureConfiguration>) GT6Features.TREE_FEATURES.get(i);
            FeatureUtils.register(ctx, GT6Worldgen.TREE_CONFIGURED_KEYS.get(i), tFeature,
                    NoneFeatureConfiguration.INSTANCE);
        }
        bootstrapSurfaceConfigured(ctx); // task w6-rocks-sticks — tail-append
        bootstrapPlantsConfigured(ctx); // task w6-t2-surface-blocks — tail-append
        // task w6-t3-large-veins — the ONE large-vein configured feature: the registered
        // GT6LargeVeinFeature instance over the 40-row vein table ({@link #LARGE_VEIN_TABLE};
        // the table rides the config JSON — the card spec ② tier-a face).
        FeatureUtils.register(ctx, GT6Worldgen.LARGE_VEINS_CONFIGURED, GT6Features.LARGE_VEINS,
                new GTVeinConfig.Table(LARGE_VEIN_TABLE));
        // task c2-deep-band — the deep-band mirror table under its OWN configured key
        // (the SAME GT6LargeVeinFeature instance — a configured feature is a (feature,
        // config) pair): a separate table keeps the surface draw mass verbatim (the
        // weighted draw sums every drawable row of its own table — mirror rows inside
        // LARGE_VEIN_TABLE would dilute the surface draw).
        FeatureUtils.register(ctx, GT6Worldgen.LARGE_VEINS_DEEP_CONFIGURED, GT6Features.LARGE_VEINS,
                new GTVeinConfig.Table(DEEP_VEIN_TABLE));
        // task strata-lens — the ONE strata-lens configured feature: the registered
        // GT6StrataLensFeature instance over the 5-row marker-stone lens table
        // ({@link #STRATA_LENS_TABLE}; the table rides the config JSON — the same tier-a
        // face as the vein table).
        FeatureUtils.register(ctx, GT6Worldgen.STRATA_LENSES_CONFIGURED, GT6Features.STRATA_LENSES,
                new GTLensConfig.Table(STRATA_LENS_TABLE));
        // task nether-lens-end-yield — the ONE nether-lens configured feature: the
        // registered GT6NetherLensFeature instance over the 17-stone nether lens table
        // ({@link #NETHER_LENS_TABLE}; the same tier-a face).
        FeatureUtils.register(ctx, GT6Worldgen.NETHER_LENSES_CONFIGURED, GT6Features.NETHER_LENSES,
                new GTLensConfig.Table(NETHER_LENS_TABLE));
        // task nether-lens-end-yield — the three nether surface forms: quartz/crystals/
        // clay, NoneFeatureConfiguration each (the upstream constants live in the classes).
        FeatureUtils.register(ctx, GT6Worldgen.NETHER_QUARTZ_CONFIGURED, GT6Features.NETHER_QUARTZ,
                NoneFeatureConfiguration.INSTANCE);
        FeatureUtils.register(ctx, GT6Worldgen.NETHER_CRYSTALS_CONFIGURED, GT6Features.NETHER_CRYSTALS,
                NoneFeatureConfiguration.INSTANCE);
        FeatureUtils.register(ctx, GT6Worldgen.NETHER_CLAY_CONFIGURED, GT6Features.NETHER_CLAY,
                NoneFeatureConfiguration.INSTANCE);
        // task worldgen-racks — the nether rack feature: the registered GT6RacksFeature
        // instance, NoneFeatureConfiguration (the nether-form shape — the gate/scan/
        // lottery constants live in the class).
        FeatureUtils.register(ctx, GT6Worldgen.NETHER_RACKS_CONFIGURED, GT6Features.NETHER_RACKS,
                NoneFeatureConfiguration.INSTANCE);
        // task worldgen-deepocean-corals — the ONE deep-ocean pylon configured feature: the
        // registered GT6DeepOceanFeature instance, NoneFeatureConfiguration (the nether-form
        // shape — the WorldgenDeepOcean constants live in the class, Loader_Worldgen.java:580).
        FeatureUtils.register(ctx, GT6Worldgen.DEEP_OCEAN_CONFIGURED, GT6Features.DEEP_OCEAN,
                NoneFeatureConfiguration.INSTANCE);
        // task bees-lv2 — the ONE bumble-hive configured feature: the registered
        // GT6HiveFeature instance, NoneFeatureConfiguration (the nether-form shape — the
        // WorldgenHives constants live in the class, not a config surface). One Feature
        // over the three dimensions (upstream WorldgenHives.java:48-193 was three rows
        // over one generator body).
        FeatureUtils.register(ctx, GT6Worldgen.BUMBLE_HIVES_CONFIGURED, GT6Features.BUMBLE_HIVES,
                NoneFeatureConfiguration.INSTANCE);
        // task worldgen-coltan — the ONE coltan-contention configured feature: the
        // registered GT6ColtanFeature instance, NoneFeatureConfiguration (the nether-form
        // shape — the upstream WorldgenColtan constants live in GT6ColtanGenerator).
        FeatureUtils.register(ctx, GT6Worldgen.COLTAN_CONFIGURED, GT6Features.COLTAN,
                NoneFeatureConfiguration.INSTANCE);
        // task worldgen-center-nexus — the ONE world-origin center configured feature: the
        // registered GT6CenterFeature instance, NoneFeatureConfiguration (the trio Nexus/
        // Streets/Beacon dispatches on chunk coordinates inside the Feature — the upstream
        // three WorldgenObject rows, Loader_Worldgen.java:647-649, over one generator body,
        // the WorldgenHives one-Feature-three-rows precedent).
        FeatureUtils.register(ctx, GT6Worldgen.CENTER_CONFIGURED, GT6Features.CENTER,
                NoneFeatureConfiguration.INSTANCE);
        // task bedrock-ore-worldgen — the ONE bedrock-ore configured feature: the
        // registered GT6BedrockOreFeature instance over the 46-row table (the same tier-a
        // face as the vein/lens tables).
        FeatureUtils.register(ctx, GT6Worldgen.BEDROCK_ORES_CONFIGURED, GT6Features.BEDROCK_ORES,
                new GTBedrockOreConfig.Table(BEDROCK_ORE_TABLE));
        // task fluid-spring — the ONE bedrock-spring configured feature: the registered
        // GT6FluidSpringFeature instance over the 16-row table (textually after the bedrock
        // feature = the upstream "Has to be after Bedrock Ores" source order, :781).
        FeatureUtils.register(ctx, GT6Worldgen.FLUID_SPRINGS_CONFIGURED, GT6Features.FLUID_SPRINGS,
                new GTFluidSpringConfig.Table(FLUID_SPRING_TABLE));
        // task worldgen-water-replace — the ONE vanilla-water configured feature: the
        // registered GT6WaterReplaceFeature instance over the 3-row table (the row order
        // IS the Loader_Worldgen.java:575-578 OCEAN→RIVER→SWAMP hard constraint).
        FeatureUtils.register(ctx, GT6Worldgen.WATER_REPLACE_CONFIGURED, GT6Features.WATER_REPLACE,
                new GT6WaterReplaceConfig.Table(WATER_REPLACE_TABLE));
        bootstrapOreConfigured(ctx); // task w6-small-ore-datagen — tail-append
        bootstrapLensOreConfigured(ctx); // task c3-lens-ores — tail-append
        bootstrapTwilightOreConfigured(ctx); // task twilight-adaptation-pilot — tail-append
        bootstrapTwilightStoneConfigured(ctx); // task twilight-stone-rows — tail-append
        bootstrapAtumStoneConfigured(ctx); // task atum-dim-adaptation — tail-append
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
        // task w6-t1-trees-nine — the 9 tree placed features: the GTCEu tree modifier
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
        bootstrapSurfacePlaced(ctx, tFeatures); // task w6-rocks-sticks — tail-append
        bootstrapPlantsPlaced(ctx, tFeatures); // task w6-t2-surface-blocks — tail-append
        // task w6-t3-large-veins — the large-vein placed feature: ONE attempt per chunk
        // (the default count — the per-chunk origin-grid 5x5 scan lives in the Feature,
        // GT6WorldGenerator.java:95-105), square spread + biome filter as the form.
        PlacementUtils.register(ctx, GT6Worldgen.LARGE_VEINS_PLACED,
                tFeatures.getOrThrow(GT6Worldgen.LARGE_VEINS_CONFIGURED),
                InSquarePlacement.spread(), BiomeFilter.biome());
        // task c2-deep-band — the deep-mirror placed feature: the surface chain shape
        // (InSquare + BiomeFilter; the per-chunk origin-grid scan lives in the Feature).
        // The Y domain rides the shifted row bands (no HeightRangePlacement — the surface
        // row's own comment face).
        PlacementUtils.register(ctx, GT6Worldgen.LARGE_VEINS_DEEP_PLACED,
                tFeatures.getOrThrow(GT6Worldgen.LARGE_VEINS_DEEP_CONFIGURED),
                InSquarePlacement.spread(), BiomeFilter.biome());
        // task strata-lens — the strata-lens placed feature: Count 1 CONSTANT +
        // InSquare + BiomeFilter, one attempt per chunk (the per-chunk ±3-chunk origin
        // scan lives in the Feature). The conflict-audit UniformInt TRAP: the count is a
        // CONSTANT integer (CountPlacement.of(int) = ConstantInt both legs,
        // CountPlacement.java:21/:21) — a uniform provider would fork the legs at JSON
        // level (DFU6 wraps {"value":{min,max}}, DFU8 inlines). Y rides the lens row
        // table (the center draw), so no HeightRangePlacement.
        PlacementUtils.register(ctx, GT6Worldgen.STRATA_LENSES_PLACED,
                tFeatures.getOrThrow(GT6Worldgen.STRATA_LENSES_CONFIGURED),
                CountPlacement.of(1), InSquarePlacement.spread(), BiomeFilter.biome());
        // task bedrock-ore-worldgen — the bedrock-ore placed feature: Count 1 CONSTANT +
        // InSquare + BiomeFilter (the conflict-audit posture; the 1/P row rolls ride the
        // Feature's coordinate-seeded stream, no Y placement — the rows carry their own
        // bedrock-anchored bands).
        PlacementUtils.register(ctx, GT6Worldgen.BEDROCK_ORES_PLACED,
                tFeatures.getOrThrow(GT6Worldgen.BEDROCK_ORES_CONFIGURED),
                CountPlacement.of(1), InSquarePlacement.spread(), BiomeFilter.biome());
        // task fluid-spring — the bedrock-spring placed feature: Count 1 CONSTANT +
        // InSquare + BiomeFilter (the bedrock-ore chain shape; the 1/P row rolls + the
        // exclusion replay live in the Feature's coordinate-seeded streams, no Y placement
        // — the rows carry their own bedrock-anchored bands).
        PlacementUtils.register(ctx, GT6Worldgen.FLUID_SPRINGS_PLACED,
                tFeatures.getOrThrow(GT6Worldgen.FLUID_SPRINGS_CONFIGURED),
                CountPlacement.of(1), InSquarePlacement.spread(), BiomeFilter.biome());
        // task worldgen-water-replace — the vanilla-water placed feature: Count 1 CONSTANT +
        // InSquare + BiomeFilter (the bedrock-ore chain shape; the row gates + the ordered
        // scan live in the Feature — the whole-chunk column walk, deterministic, no Y
        // placement and no randomness, the WorldgenOcean.java:58-82 shape).
        PlacementUtils.register(ctx, GT6Worldgen.WATER_REPLACE_PLACED,
                tFeatures.getOrThrow(GT6Worldgen.WATER_REPLACE_CONFIGURED),
                CountPlacement.of(1), InSquarePlacement.spread(), BiomeFilter.biome());
        // task nether-lens-end-yield — the nether-lens placed feature: Count 1 CONSTANT +
        // InSquare + BiomeFilter (the strata-lens chain shape; the per-chunk 1/200 row rolls
        // live in the Feature's coordinate-seeded stream, no Y placement — the rows carry
        // their own Y domains).
        PlacementUtils.register(ctx, GT6Worldgen.NETHER_LENSES_PLACED,
                tFeatures.getOrThrow(GT6Worldgen.NETHER_LENSES_CONFIGURED),
                CountPlacement.of(1), InSquarePlacement.spread(), BiomeFilter.biome());
        // task nether-lens-end-yield — the three form placed features: Count 1 CONSTANT +
        // InSquare + BiomeFilter (the conflict-audit posture; each Feature walks its own
        // 16x16 columns / walk on the coordinate-seeded stream — no Y placement).
        PlacementUtils.register(ctx, GT6Worldgen.NETHER_QUARTZ_PLACED,
                tFeatures.getOrThrow(GT6Worldgen.NETHER_QUARTZ_CONFIGURED),
                CountPlacement.of(1), InSquarePlacement.spread(), BiomeFilter.biome());
        PlacementUtils.register(ctx, GT6Worldgen.NETHER_CRYSTALS_PLACED,
                tFeatures.getOrThrow(GT6Worldgen.NETHER_CRYSTALS_CONFIGURED),
                CountPlacement.of(1), InSquarePlacement.spread(), BiomeFilter.biome());
        // task bees-lv2 — the bumble-hive placed feature: Count 1 CONSTANT + InSquare
        // + BiomeFilter (the conflict-audit posture; the column pick + the dimension rolls
        // ride the Feature's coordinate-seeded stream, no Y placement — the forms carry
        // their own Y domains). The hive hangs off THREE biome modifiers (the next method).
        PlacementUtils.register(ctx, GT6Worldgen.BUMBLE_HIVES_PLACED,
                tFeatures.getOrThrow(GT6Worldgen.BUMBLE_HIVES_CONFIGURED),
                CountPlacement.of(1), InSquarePlacement.spread(), BiomeFilter.biome());
        // task worldgen-coltan — the coltan placed feature: Count 1 CONSTANT + InSquare
        // + BiomeFilter (the bedrock-ore chain shape; the seed-derived center + the ring
        // gates live in the Feature's coordinate-seeded stream, no Y placement — the
        // scatter carries its own 20..40 band).
        PlacementUtils.register(ctx, GT6Worldgen.COLTAN_PLACED,
                tFeatures.getOrThrow(GT6Worldgen.COLTAN_CONFIGURED),
                CountPlacement.of(1), InSquarePlacement.spread(), BiomeFilter.biome());
        PlacementUtils.register(ctx, GT6Worldgen.NETHER_CLAY_PLACED,
                tFeatures.getOrThrow(GT6Worldgen.NETHER_CLAY_CONFIGURED),
                CountPlacement.of(1), InSquarePlacement.spread(), BiomeFilter.biome());
        // task worldgen-racks — the nether rack placed feature: RarityFilter(2) = the
        // upstream :54 aRandom.nextBoolean() chunk gate (the same 50% draw, the tier-a
        // datapack face) + Count 1 CONSTANT + InSquare + BiomeFilter (the nether-form
        // chain shape; the 16 column attempts live in the Feature).
        PlacementUtils.register(ctx, GT6Worldgen.NETHER_RACKS_PLACED,
                tFeatures.getOrThrow(GT6Worldgen.NETHER_RACKS_CONFIGURED),
                RarityFilter.onAverageOnceEvery(2), CountPlacement.of(1), InSquarePlacement.spread(),
                BiomeFilter.biome());

        // task worldgen-center-nexus — the center placed feature: Count 1 CONSTANT +
        // InSquare + BiomeFilter (the conflict-audit posture; the fixed world-origin chunk
        // gates live in the Feature's coordinate dispatch, no Y placement — the bodies
        // carry their own HEIGHT-anchored bands).
        PlacementUtils.register(ctx, GT6Worldgen.CENTER_PLACED,
                tFeatures.getOrThrow(GT6Worldgen.CENTER_CONFIGURED),
                CountPlacement.of(1), InSquarePlacement.spread(), BiomeFilter.biome());
        // task worldgen-deepocean-corals — the deep-ocean pylon placed feature: Count 1
        // CONSTANT + InSquare + BiomeFilter (the nether-form chain shape; the column pick +
        // noise gate ride the Feature's coordinate-seeded stream, no Y placement — the
        // pylon carries its own y30..38 domain).
        PlacementUtils.register(ctx, GT6Worldgen.DEEP_OCEAN_PLACED,
                tFeatures.getOrThrow(GT6Worldgen.DEEP_OCEAN_CONFIGURED),
                CountPlacement.of(1), InSquarePlacement.spread(), BiomeFilter.biome());
        bootstrapOrePlaced(ctx, tFeatures); // task w6-small-ore-datagen — tail-append
        bootstrapLensOrePlaced(ctx, tFeatures); // task c3-lens-ores — tail-append
        bootstrapTwilightOrePlaced(ctx, tFeatures); // task twilight-adaptation-pilot — tail-append
        bootstrapTwilightStonePlaced(ctx, tFeatures); // task twilight-stone-rows — tail-append
        bootstrapAtumStonePlaced(ctx, tFeatures); // task atum-dim-adaptation — tail-append
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
        // task w6-t1-trees-nine — the 9 tree biome modifiers: one per kind, keyed on
        // the #gt6:trees/<snake> biome tag (GT6BiomeTags; the tag IS the datapack
        // per-feature biome face) at the VEGETAL_DECORATION step (the vanilla tree pass —
        // the enum has no TREES member, the upstream surface-tree surface pass rides it)
        for (int i = 0; i < TREE_BIOME_MODIFIER_KEYS.size(); i++) {
            ctx.register(TREE_BIOME_MODIFIER_KEYS.get(i), addFeatures(
                    tBiomes.getOrThrow(GT6BiomeTags.treeTag(GT6TreeBlocks.KINDS.get(i).snake())),
                    HolderSet.direct(tPlaced.getOrThrow(GT6Worldgen.TREE_PLACED_KEYS.get(i))),
                    GenerationStep.Decoration.VEGETAL_DECORATION));
        }
        bootstrapSurfaceBiomeModifiers(ctx, tBiomes, tPlaced); // task w6-rocks-sticks — tail-append
        bootstrapPlantsBiomeModifiers(ctx, tBiomes, tPlaced); // task w6-t2-surface-blocks — tail-append
        // task w6-t3-large-veins — the large-vein biome modifier: EVERY overworld biome
        // (the research.p30-w6-vein-boundary impl note: upstream large veins have NO biome
        // gate — the generate signature has no biome parameter — and a per-biome split would
        // carve holes into any vein crossing a biome border), at the UNDERGROUND_ORES step.
        ctx.register(biomeModifierKeyOf("large_veins"), addFeatures(tOverworld,
                HolderSet.direct(tPlaced.getOrThrow(GT6Worldgen.LARGE_VEINS_PLACED)),
                GenerationStep.Decoration.UNDERGROUND_ORES));
        // task c2-deep-band — the deep-mirror biome modifier: EVERY overworld biome at
        // the ore pass (the large-vein no-biome-gate note carried over). The modifier set
        // is OVERWORLD-ONLY — the End draw (the END_YIELD modifier above) never sees the
        // deep table, the ORE_END rows stay the only End veins.
        ctx.register(biomeModifierKeyOf("large_veins_deep"), addFeatures(tOverworld,
                HolderSet.direct(tPlaced.getOrThrow(GT6Worldgen.LARGE_VEINS_DEEP_PLACED)),
                GenerationStep.Decoration.UNDERGROUND_ORES));
        // task strata-lens + c3-lens-ores — the strata-lens biome modifier: EVERY
        // overworld biome (the research.p30-w6-vein-boundary impl note carried over: a
        // per-biome split would carve holes into any lens crossing a biome border), at the
        // UNDERGROUND_ORES step. The companion-ore placed features (task c3-lens-ores)
        // ride the SAME modifier AFTER the lens feature: the appended list order is the
        // biome feature-list order, and vanilla executes a step's features along the
        // FeatureSorter chain edges (FeatureSorter.buildFeaturesPerStep:52-57 consecutive
        // pairs → ChunkGenerator.applyBiomeDecoration:319) — so every companion feature
        // scans host blocks the lens has already placed, independent of where this
        // modifier lands among the other ore-pass modifiers.
        //
        // task lens-ore-base-order — the ORDINARY small-ore band (the overworld surface
        // pairs + the deep-band mirrors, overworldSmallOrePlaced) rides this SAME list
        // AFTER the companions. It used to hang off its own ore_small_overworld modifier:
        // the cross-modifier landing order is the uncontracted datapack load order, so
        // the band could run BEFORE the lens — the ore replaced plain stone first, then
        // the lens replaced the surrounding stone but never the ore block (ores are not
        // in stone_ore_replaceables), leaving stone-based small ore embedded in marble.
        // One list, one chain: lens → companions → small ores, under ANY landing.
        List<Holder<PlacedFeature>> tLensBand = new ArrayList<>();
        tLensBand.add(tPlaced.getOrThrow(GT6Worldgen.STRATA_LENSES_PLACED));
        for (LensOreRow tRow : lensOreRows()) tLensBand.add(tPlaced.getOrThrow(lensOrePlacedKey(tRow)));
        tLensBand.addAll(overworldSmallOrePlaced(tPlaced));
        ctx.register(biomeModifierKeyOf("strata_lenses"), addFeatures(tOverworld,
                HolderSet.direct(tLensBand),
                GenerationStep.Decoration.UNDERGROUND_ORES));
        // task bedrock-ore-worldgen — the bedrock-ore biome modifier: EVERY overworld
        // biome (the no-biome-gate impl note carried over: upstream WorldgenOresBedrock has
        // no biome parameter), at the UNDERGROUND_ORES step.
        ctx.register(biomeModifierKeyOf("bedrock_ores"), addFeatures(tOverworld,
                HolderSet.direct(tPlaced.getOrThrow(GT6Worldgen.BEDROCK_ORES_PLACED)),
                GenerationStep.Decoration.UNDERGROUND_ORES));
        // task fluid-spring — the bedrock-spring biome modifier: EVERY overworld biome,
        // at the UNDERGROUND_ORES step, registered textually AFTER the bedrock modifier (the
        // :781 "Has to be after Bedrock Ores" order). The GT6FluidSpringFeature replay seam
        // keeps the one-bedrock-event-per-chunk exclusion correct under ANY modifier
        // application order — this ordering is the declared source-order semantics.
        ctx.register(biomeModifierKeyOf("fluid_springs"), addFeatures(tOverworld,
                HolderSet.direct(tPlaced.getOrThrow(GT6Worldgen.FLUID_SPRINGS_PLACED)),
                GenerationStep.Decoration.UNDERGROUND_ORES));
        // task worldgen-nether-bedrock-lava — the TWO is_nether legs over the SAME placed
        // features (the END_YIELD / nether_bumble_hives same-placed-key convention): the
        // seven GEN_NETHER bedrock rows (:758-764) and the :797 nether lava dome ride the
        // nether's own modifier rows, textually after their overworld twins (the source
        // order: the nether ore rows follow the GEN_FLOOR rows, the spring row is last,
        // Loader_Worldgen.java:758-764/:797). The row-level nether columns (the configs)
        // keep the roll mass per dimension — the feature walks only its own dimension's rows.
        ctx.register(biomeModifierKeyOf("nether_bedrock_ores"), addFeatures(
                tBiomes.getOrThrow(BiomeTags.IS_NETHER),
                HolderSet.direct(tPlaced.getOrThrow(GT6Worldgen.BEDROCK_ORES_PLACED)),
                GenerationStep.Decoration.UNDERGROUND_ORES));
        ctx.register(biomeModifierKeyOf("nether_fluid_springs"), addFeatures(
                tBiomes.getOrThrow(BiomeTags.IS_NETHER),
                HolderSet.direct(tPlaced.getOrThrow(GT6Worldgen.FLUID_SPRINGS_PLACED)),
                GenerationStep.Decoration.UNDERGROUND_ORES));
        // task atum-dim-adaptation — the atum spring modifier: the :789-792 oil band over
        // OUR OWN #gt6:atum_biomes tag (Atum's #forge:is_atum fill covers 2 of the 11
        // biomes — the 11-id fallback IS the coverage), at the ore pass, textually after
        // the nether row (the source order: the atum band follows the OW band, :789 after
        // :782-788). The atum rows roll with no ore-replay opponent (no GT bedrock ores
        // carry GEN_ATUM); the mod_loaded condition rides the emission registry.
        ctx.register(ATUM_FLUID_SPRINGS_MODIFIER_KEY, addFeatures(
                tBiomes.getOrThrow(GT6Worldgen.ATUM_BIOMES),
                HolderSet.direct(tPlaced.getOrThrow(GT6Worldgen.FLUID_SPRINGS_PLACED)),
                GenerationStep.Decoration.UNDERGROUND_ORES));
        // task atum-dim-adaptation — the atum large-vein modifier: the SAME
        // gt6:large_veins placed feature over #gt6:atum_biomes (the END_YIELD
        // same-placed-key convention), at the ore step; the Feature's biome probe picks
        // the ORE_ATUM rows (:886-916, 31 rows) and rides the ATUM_DIMENSION_SALT stream
        // there. The atum host face is the #gt6:atum_base_stone tag (the stone-family
        // ore skin — atum's limestone/karst are the dimension's vein ground).
        ctx.register(ATUM_LARGE_VEINS_MODIFIER_KEY, addFeatures(
                tBiomes.getOrThrow(GT6Worldgen.ATUM_BIOMES),
                HolderSet.direct(tPlaced.getOrThrow(GT6Worldgen.LARGE_VEINS_PLACED)),
                GenerationStep.Decoration.UNDERGROUND_ORES));
        // task atum-dim-adaptation — the atum stone modifier: the 17-stone row over
        // #gt6:atum_biomes at the ore step — the 12 blob stones REUSE the overworld
        // placed features (the atum row numbers ARE the overworld row numbers,
        // Loader_Worldgen.java:659 vs :655), the 5 marker stones ride their own
        // atum_stone_<snake> placed variants. ONE modifier over the whole band (the
        // twilight_stones row's shape); the mod_loaded condition rides the emission
        // registry.
        List<Holder<PlacedFeature>> tAtumStones = new ArrayList<>(17);
        for (ResourceKey<PlacedFeature> tKey : GT6Worldgen.PLACED_KEYS) tAtumStones.add(tPlaced.getOrThrow(tKey));
        for (String tSnake : GT6Worldgen.LENS_STONE_SNAKES) tAtumStones.add(tPlaced.getOrThrow(GT6Worldgen.atumStonePlacedKey(tSnake)));
        ctx.register(ATUM_STONES_MODIFIER_KEY, addFeatures(
                tBiomes.getOrThrow(GT6Worldgen.ATUM_BIOMES),
                HolderSet.direct(tAtumStones),
                GenerationStep.Decoration.UNDERGROUND_ORES));

        // task worldgen-water-replace — the vanilla-water biome modifier: EVERY overworld
        // biome (the row gates do the filtering in-feature — the chunk-granular
        // aBiomeNames face), at the TOP_LAYER_MODIFICATION step, the LAST decoration pass:
        // AFTER kelp/seagrass (strict Blocks.WATER gates, SeagrassFeature.java:30/
        // KelpFeature.java:26 — replacing earlier would sterilize ocean vegetation) and
        // AFTER freeze_top_layer (strict fluid==Fluids.WATER, Biome.java:141 — replacing
        // earlier would end frozen-river ice; vanilla inline features precede
        // modifier-appended ones in the same step's list). The row ORDER (the
        // :575-578 hard constraint) rides the table inside ONE configured feature —
        // immune to modifier application order.
        ctx.register(biomeModifierKeyOf("water_replace"), addFeatures(tOverworld,
                HolderSet.direct(tPlaced.getOrThrow(GT6Worldgen.WATER_REPLACE_PLACED)),
                GenerationStep.Decoration.TOP_LAYER_MODIFICATION));
        // task nether-lens-end-yield — the nether-lens biome modifier: EVERY nether
        // biome (upstream GEN_NETHER, Loader_Worldgen.java:656 — the dim flag is the modern
        // biome-tag face, the small-ore band's IS_NETHER convention), at the
        // UNDERGROUND_ORES step (the stone pass the blob/lens rows ride).
        ctx.register(biomeModifierKeyOf("nether_lenses"), addFeatures(
                tBiomes.getOrThrow(BiomeTags.IS_NETHER),
                HolderSet.direct(tPlaced.getOrThrow(GT6Worldgen.NETHER_LENSES_PLACED)),
                GenerationStep.Decoration.UNDERGROUND_ORES));
        // task nether-lens-end-yield — the three form biome modifiers (GEN_NETHER,
        // Loader_Worldgen.java:599-601): quartz rides the ore pass (a netherrack
        // replacement, the blob convention), the crystals too (ore blocks in caves), the
        // clay rides LOCAL_MODIFICATIONS (the soil-band disk-pass convention — a surface
        // band, not an ore).
        ctx.register(biomeModifierKeyOf("nether_quartz"), addFeatures(
                tBiomes.getOrThrow(BiomeTags.IS_NETHER),
                HolderSet.direct(tPlaced.getOrThrow(GT6Worldgen.NETHER_QUARTZ_PLACED)),
                GenerationStep.Decoration.UNDERGROUND_ORES));
        ctx.register(biomeModifierKeyOf("nether_crystals"), addFeatures(
                tBiomes.getOrThrow(BiomeTags.IS_NETHER),
                HolderSet.direct(tPlaced.getOrThrow(GT6Worldgen.NETHER_CRYSTALS_PLACED)),
                GenerationStep.Decoration.UNDERGROUND_ORES));
        ctx.register(biomeModifierKeyOf("nether_clay"), addFeatures(
                tBiomes.getOrThrow(BiomeTags.IS_NETHER),
                HolderSet.direct(tPlaced.getOrThrow(GT6Worldgen.NETHER_CLAY_PLACED)),
                GenerationStep.Decoration.LOCAL_MODIFICATIONS));
        // task worldgen-racks — the nether rack biome modifier: EVERY nether biome
        // (GEN_NETHER, Loader_Worldgen.java:619), at the VEGETAL_DECORATION step (the
        // surface-deco band convention — the racks are the nether sister of the
        // overworld_surface_rocks band).
        ctx.register(biomeModifierKeyOf("nether_racks"), addFeatures(
                tBiomes.getOrThrow(BiomeTags.IS_NETHER),
                HolderSet.direct(tPlaced.getOrThrow(GT6Worldgen.NETHER_RACKS_PLACED)),
                GenerationStep.Decoration.VEGETAL_DECORATION));
        // task worldgen-deepocean-corals — the deep-ocean pylon biome modifier
        // (GEN_OVERWORLD, Loader_Worldgen.java:580): the upstream chunk-biome-name
        // deepOcean probe rides the modern tag face #minecraft:is_deep_ocean (the
        // small-ore-band dim-flag convention — the tag covers the deep_ocean/deep_cold/
        // deep_lukewarm/deep_frozen family, the 1.7.10 single-biome face's modern
        // translation), at the TOP_LAYER_MODIFICATION step (the seabed face — after the
        // vegetal pass, the pylons overwrite kelp/seagrass the way the upstream WD.set
        // did; the same step the water-body band rides, keeping the upstream
        // ocean-then-corals ordering seam per-step).
        ctx.register(biomeModifierKeyOf("deep_ocean"), addFeatures(
                tBiomes.getOrThrow(BiomeTags.IS_DEEP_OCEAN),
                HolderSet.direct(tPlaced.getOrThrow(GT6Worldgen.DEEP_OCEAN_PLACED)),
                GenerationStep.Decoration.TOP_LAYER_MODIFICATION));
        // task bees-lv2 — the THREE bumble-hive biome modifiers over the SAME placed
        // feature (upstream Loader_Worldgen.java:635-637: overworld.bumblehives /
        // nether.bumblehives / end.bumblehives; the END_YIELD same-placed-key modifier
        // precedent). The overworld row rides #minecraft:is_overworld (the small-ore band
        // convention; upstream also listed the overworld-like mod dims — the Feature routes
        // every non-nether/end dimension to the overworld shape, the DIM_UNKNOWN face), at
        // the UNDERGROUND_ORES step (the surface exists pre-decoration; the underground
        // form is the native rider — one step for both forms).
        ctx.register(biomeModifierKeyOf("bumble_hives"), addFeatures(tOverworld,
                HolderSet.direct(tPlaced.getOrThrow(GT6Worldgen.BUMBLE_HIVES_PLACED)),
                GenerationStep.Decoration.UNDERGROUND_ORES));
        ctx.register(biomeModifierKeyOf("nether_bumble_hives"), addFeatures(
                tBiomes.getOrThrow(BiomeTags.IS_NETHER),
                HolderSet.direct(tPlaced.getOrThrow(GT6Worldgen.BUMBLE_HIVES_PLACED)),
                GenerationStep.Decoration.UNDERGROUND_ORES));
        ctx.register(biomeModifierKeyOf("end_bumble_hives"), addFeatures(
                tBiomes.getOrThrow(BiomeTags.IS_END),
                HolderSet.direct(tPlaced.getOrThrow(GT6Worldgen.BUMBLE_HIVES_PLACED)),
                GenerationStep.Decoration.UNDERGROUND_ORES));
        // task atum-dim-adaptation — the atum hive modifier (the FOURTH vanilla-family
        // row + the first mod-dim hive): the SAME gt6:bumble_hives placed feature over
        // #gt6:atum_biomes at the underground step; the Feature's atum arm
        // (WorldgenHives.java:79-87) scans y16-79 for the #gt6:atum_base_stone wall.
        // The mod_loaded condition rides the emission registry.
        ctx.register(ATUM_BUMBLE_HIVES_MODIFIER_KEY, addFeatures(
                tBiomes.getOrThrow(GT6Worldgen.ATUM_BIOMES),
                HolderSet.direct(tPlaced.getOrThrow(GT6Worldgen.BUMBLE_HIVES_PLACED)),
                GenerationStep.Decoration.UNDERGROUND_ORES));
        // task worldgen-coltan — the coltan biome modifier: EVERY overworld biome
        // (upstream GEN_OVERWORLD, Loader_Worldgen.java:779 — the small-ore band's
        // IS_OVERWORLD convention; the Feature routes by the seed-derived center, not by
        // biome), at the UNDERGROUND_ORES step (the small-ore pass the rings ride).
        ctx.register(biomeModifierKeyOf("coltan"), addFeatures(tOverworld,
                HolderSet.direct(tPlaced.getOrThrow(GT6Worldgen.COLTAN_PLACED)),
                GenerationStep.Decoration.UNDERGROUND_ORES));

        // task worldgen-center-nexus — the center biome modifier: EVERY overworld biome
        // (upstream Loader_Worldgen.java:647-649 gates on DIM_OVERWORLD, not biomes), at
        // the TOP_LAYER_MODIFICATION step (the last decoration pass — the hand-built
        // terrain overwrites whatever generated before, the upstream
        // WorldgenCenterBiomes.reset :63-65 GENERATING_SPECIAL suppression face becomes
        // plain overwriting; the water-replace frozen-row step precedent).
        ctx.register(biomeModifierKeyOf("center"), addFeatures(tOverworld,
                HolderSet.direct(tPlaced.getOrThrow(GT6Worldgen.CENTER_PLACED)),
                GenerationStep.Decoration.TOP_LAYER_MODIFICATION));
        // task nether-lens-end-yield — the END large-vein modifier over the SAME
        // gt6:large_veins placed feature (the Feature's biome probe picks the ORE_END
        // rows there), at the ore step. The CONDITIONS ride the emission providers
        // (the neo leg's native conditions map / the forge leg's GT6BiomeModifierConditions
        // injection) — the bootstrap itself is condition-free, the loader brand keys are
        // added at emission time.
        ctx.register(END_YIELD_MODIFIER_KEY, addFeatures(tBiomes.getOrThrow(BiomeTags.IS_END),
                HolderSet.direct(tPlaced.getOrThrow(GT6Worldgen.LARGE_VEINS_PLACED)),
                GenerationStep.Decoration.UNDERGROUND_ORES));
        bootstrapOreBiomeModifiers(ctx, tBiomes, tPlaced); // task w6-small-ore-datagen — tail-append
        bootstrapVanillaDeblob(ctx, tBiomes, tPlaced); // issue #32 vanilla-deblob — tail-append
        // task twilight-adaptation-pilot — the ONE mod-dimension biome modifier (the
        // adaptation skeleton's first tenant): the 3 axis-valid RockOres rows over TF's own
        // tag, at the ore step. The tag lookup resolves EMPTY at datagen (RegistrySetBuilder
        // .EmptyTagLookup — any tag key serializes as the "#..." string) and the ROW's
        // mod_loaded condition gates the entry before the tag resolves at runtime — the
        // bootstrap itself is condition-free (the END_YIELD division of labor), the loader
        // brand keys are added by the emission registry (GT6BiomeModifierConditions).
        ctx.register(TWILIGHT_ORES_MODIFIER_KEY, addFeatures(
                tBiomes.getOrThrow(GTOreWorldgen.twilightBiomeTag()),
                HolderSet.direct(GTOreWorldgen.twilightOnAxisRows().stream()
                        .map(tRow -> tPlaced.getOrThrow(GTOreWorldgen.twilightPlacedKey(tRow))).toList()),
                GenerationStep.Decoration.UNDERGROUND_ORES));
        // task twilight-stone-rows — the SECOND mod-dimension biome modifier: the 17
        // twilight stone rows (GTStoneBlocks.STONES order, the placed-variant band —
        // Loader_Worldgen.java:657) over TF's own tag, at the ore step (the blob rows'
        // pass). ONE modifier over the whole band (the twilight_ores row's shape — the
        // per-stone upstream config categories become the per-placed JSONs + the ONE
        // datapack-editable modifier row); the row's mod_loaded condition rides the
        // emission registry (the twilight_ores division of labor).
        ctx.register(TWILIGHT_STONES_MODIFIER_KEY, addFeatures(
                tBiomes.getOrThrow(GTOreWorldgen.twilightBiomeTag()),
                HolderSet.direct(GT6Worldgen.TWILIGHT_STONE_PLACED_KEYS.stream()
                        .map(tPlaced::getOrThrow).toList()),
                GenerationStep.Decoration.UNDERGROUND_ORES));
        // task twilight-hives-springs — the FOURTH hive modifier over TF's own tag at the
        // ore step (the twilight_ores row's division of labor: condition-free bootstrap,
        // the loader brand keys ride the emission registry). The mount half only — the
        // Feature's default routing IS the upstream DIM_TWILIGHT-over-OVERWORLD-case face
        // (WorldgenHives.java:126), zero Feature changes.
        ctx.register(TWILIGHT_HIVES_MODIFIER_KEY, addFeatures(
                tBiomes.getOrThrow(GTOreWorldgen.twilightBiomeTag()),
                HolderSet.direct(tPlaced.getOrThrow(GT6Worldgen.BUMBLE_HIVES_PLACED)),
                GenerationStep.Decoration.UNDERGROUND_ORES));
        // task twilight-hives-springs — the twilight FLUID-SPRING mount over TF's own tag at
        // the ore step, textually AFTER the hive mount (the upstream source order: the hive
        // row :639 precedes the spring rows :795-796). The ROW masks (the twilight column,
        // the :795-796 rows) keep the roll mass per dimension — the Feature walks only its
        // own dimension's rows, the nether_fluid_springs convention; the conditions ride the
        // emission registry (the positive mod_loaded, the twilight_ores row's division of
        // labor).
        ctx.register(TWILIGHT_FLUID_SPRINGS_MODIFIER_KEY, addFeatures(
                tBiomes.getOrThrow(GTOreWorldgen.twilightBiomeTag()),
                HolderSet.direct(tPlaced.getOrThrow(GT6Worldgen.FLUID_SPRINGS_PLACED)),
                GenerationStep.Decoration.UNDERGROUND_ORES));
    }

    /**
     * The two de-blob biome modifiers (issue #32, decisions.2026-09-28-devanilla-default):
     * the remove_features rows over the vanilla dimension tags (#minecraft:is_overworld /
     * #minecraft:is_nether, the small-ore band's convention). Phase REMOVE runs after ADD
     * (ForgeBiomeModifiers Phase :80/:84), so the GT blobs added above survive; the
     * per-key names only — a future vanilla rename makes a row member a harmless no-op,
     * never a step-wide wipe.
     */
    private static void bootstrapVanillaDeblob(
        //? if forge {
        BootstapContext<BiomeModifier> ctx
        //?} else {
        /*BootstrapContext<BiomeModifier> ctx
        *///?}
        , HolderGetter<Biome> aBiomes, HolderGetter<PlacedFeature> aPlaced
    ) {
        ctx.register(VANILLA_DEBLOB_OVERWORLD_KEY, removeFeatures(aBiomes.getOrThrow(BiomeTags.IS_OVERWORLD),
                placedSet(aPlaced, VANILLA_DEBLOB_OVERWORLD)));
        ctx.register(VANILLA_DEBLOB_NETHER_KEY, removeFeatures(aBiomes.getOrThrow(BiomeTags.IS_NETHER),
                placedSet(aPlaced, VANILLA_DEBLOB_NETHER)));
    }

    /** The vanilla placed-feature holder set for one suppress list (getOrThrow = the leg jar is the key validator). */
    private static HolderSet<PlacedFeature> placedSet(HolderGetter<PlacedFeature> aPlaced, List<String> aPaths) {
        List<Holder<PlacedFeature>> tHolders = new ArrayList<>(aPaths.size());
        for (String tPath : aPaths) {
            tHolders.add(aPlaced.getOrThrow(ResourceKey.create(Registries.PLACED_FEATURE,
                    ResourceLocation.fromNamespaceAndPath("minecraft", tPath))));
        }
        return HolderSet.direct(tHolders);
    }

    // ------------------------------------------------------------------
    // The surface deco band (task w6-rocks-sticks). Structure:
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

    /**
     * The twilight rocks' biome-modifier key (task twilight-vegetation): the one
     * mod-dimension SURFACE row — biomes {@code #gt6:surface_rocks_twilight} (our own
     * tag, the 12 TF members optional), features = the twilight placed twin (count 4),
     * the mod_loaded condition rides the emission registry
     * (GT6BiomeModifierConditions.CONDITION_ROWS, the twilight_ores second-row face).
     */
    public static final ResourceKey<BiomeModifier> TWILIGHT_SURFACE_ROCKS_MODIFIER_KEY =
            biomeModifierKeyOf("twilight_surface_rocks");

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
        // task twilight-vegetation — the twilight.rocks placed twin over the SAME configured
        // lottery (Loader_Worldgen.java:621 Amount=4 vs the overworld row :618 Amount=2, the
        // same Probability=3): the outer chain shape above, count 4, no square/heightmap
        // (the inline inner chains carry the positions).
        PlacementUtils.register(ctx, GT6Worldgen.TWILIGHT_SURFACE_ROCKS_PLACED,
                aFeatures.getOrThrow(GT6Worldgen.SURFACE_ROCKS_CONFIGURED),
                RarityFilter.onAverageOnceEvery(GT6Worldgen.SURFACE_ROCKS_PROBABILITY),
                CountPlacement.of(GT6Worldgen.TWILIGHT_SURFACE_ROCKS_AMOUNT),
        // task atum-dim-adaptation — the atum surface-rocks placed twin (the
        // twilight-vegetation form): atum.rocks is 3,3 (Loader_Worldgen.java:625
        // amount 3 / probability 3) over the SHARED overworld_surface_rocks lottery —
        // the anchor-less outer chain, the overworld twin's exact shape at atum numbers.
        PlacementUtils.register(ctx, GT6Worldgen.ATUM_SURFACE_ROCKS_PLACED,
                aFeatures.getOrThrow(GT6Worldgen.SURFACE_ROCKS_CONFIGURED),
                RarityFilter.onAverageOnceEvery(GT6Worldgen.ATUM_SURFACE_ROCKS_PROBABILITY),
                CountPlacement.of(GT6Worldgen.ATUM_SURFACE_ROCKS_AMOUNT),
                BiomeFilter.biome());
    }

    // ------------------------------------------------------------------
    // The small-ore band (task w6-small-ore-datagen; the pool loop joined in
    // b-gem-pool-extension). Structure: 152
    // (row, dim) placement pairs (GTOreWorldgen.placementPairs, the upstream
    // GEN-flag walk) x {configured = vanilla Feature.ORE size=4 over the
    // WD.setSmallOre host targets, placed = Count(veinCount constant)+InSquare+
    // HeightRange uniform+BiomeFilter}. The OVERWORLD face (surface pairs + deep
    // mirrors) rides the one strata_lenses biome modifier AFTER the lens+companion
    // head (task lens-ore-base-order — the FeatureSorter chain puts the band's
    // marble-family target arm after the lens); nether/end keep their own
    // IS_NETHER/IS_END modifiers at the ore pass. Zero new blocks/features —
    // pure JSON consumption of the ore-1 ore_small universe.
    // ------------------------------------------------------------------

    /**
     * The 2 small-ore biome-modifier keys (nether/end — the dims with no lens chain).
     * The OVERWORLD face rides the one strata_lenses modifier after the lens+companion
     * head (task lens-ore-base-order): a standalone overworld modifier's landing order
     * was the uncontracted datapack load order, so the band could run before the lens
     * and ship stone-based ore inside the lens stone.
     */
    public static final List<ResourceKey<BiomeModifier>> ORE_BIOME_MODIFIER_KEYS = List.of(
            biomeModifierKeyOf("ore_small_nether"),
            biomeModifierKeyOf("ore_small_end"));

    /**
     * The End large-vein biome-modifier key (task nether-lens-end-yield): the ONE
     * row carrying the has_planet_veins yield condition — no planet mod: the modifier
     * applies and the five ORE_END rows generate in the End (the ruling B semantics:
     * GT6 self-sufficient); planet mod present: the conditions fail and the modifier
     * entry is skipped at datapack load (the RegistryDataLoader patch's debug-level
     * skip), yielding the End (the declared deviation from upstream's union-dual-mount,
     * decisions.2026-09-17-p30-nether-end-rulings).
     */
    public static final ResourceKey<BiomeModifier> END_YIELD_MODIFIER_KEY = biomeModifierKeyOf("large_veins_end");

    /**
     * The twilight RockOres biome-modifier key (task twilight-adaptation-pilot): the ONE
     * mod-dimension row — biomes {@code #twilightforest:in_twilight_forest} (TF's own tag,
     * GTOreWorldgen.twilightBiomeTag), features = the axis-valid twilight rows, the
     * conditions ride the emission registry (GT6BiomeModifierConditions, the positive
     * {@code [mod_loaded twilightforest]} = the TF-absence skip semantics). The bootstrap
     * itself is condition-free, the loader brand keys are added at emission time (the
     * END_YIELD row's division of labor).
     */
    public static final ResourceKey<BiomeModifier> TWILIGHT_ORES_MODIFIER_KEY = biomeModifierKeyOf("twilight_ores");

    /**
     * The twilight stone-rows biome-modifier key (task twilight-stone-rows): the SECOND
     * mod-dimension row, the twilight_ores shape — biomes {@code #twilightforest:
     * in_twilight_forest}, features = the 17 twilight stone placed variants (GTStoneBlocks
     * .STONES order), the conditions ride the emission registry (the positive
     * {@code [mod_loaded twilightforest]}).
     */
    public static final ResourceKey<BiomeModifier> TWILIGHT_STONES_MODIFIER_KEY = biomeModifierKeyOf("twilight_stones");

    /**
     * The twilight bumble-hive biome-modifier key (task twilight-hives-springs): the FOURTH
     * hive row — biomes {@code #twilightforest:in_twilight_forest} (the same TF tag gate as
     * the twilight_ores row), features = the ONE {@code gt6:bumble_hives} placed feature
     * (the nether_bumble_hives same-placed-key convention), the conditions ride the emission
     * registry (the positive {@code [mod_loaded twilightforest]}). The upstream
     * {@code twilight.bumblehives} row (Loader_Worldgen.java:639) routed DIM_TWILIGHT into
     * the OVERWORLD case (WorldgenHives.java:126 — DIM_TWILIGHT in the same case group), and
     * the Feature's default routing already sends every non-nether/end dimension to that
     * overworld shape — this row is purely the MOUNT. The surface family chain picks the
     * hive colour per biome: TF's magical biomes ride {@code #gt6:bumble_hives/magical}
     * (GT6BiomeTags, the required:false pack-extension face).
     */
    public static final ResourceKey<BiomeModifier> TWILIGHT_HIVES_MODIFIER_KEY = biomeModifierKeyOf("twilight_bumble_hives");

    /**
     * The twilight fluid-spring biome-modifier key (task twilight-hives-springs): biomes
     * {@code #twilightforest:in_twilight_forest} (the same TF tag gate as the twilight_ores
     * row), features = the ONE {@code gt6:fluid_springs} placed feature (the nether_fluid_
     * springs same-placed-key convention), the conditions ride the emission registry (the
     * positive {@code [mod_loaded twilightforest]}). The upstream :795-796 rows (natural gas
     * 1/200, geothermal water 1/100) routed via the GEN_TWILIGHT dim-type list; the port's
     * Feature routes by the row's {@code twilight} mask column behind the THREE-STATE
     * dimension face (the hasCeiling binary alone would roll the OW oil/gas band in TF —
     * the trap the twilight column exists for).
     */
    public static final ResourceKey<BiomeModifier> TWILIGHT_FLUID_SPRINGS_MODIFIER_KEY = biomeModifierKeyOf("twilight_fluid_springs");

     * The atum spring biome-modifier key (task atum-dim-adaptation): the FIRST atum row —
     * biomes {@code #gt6:atum_biomes} (the 11-member fallback tag), features = the SAME
     * gt6:fluid_springs placed feature (the nether_fluid_springs same-placed-key
     * convention; the atum rows are selected by the config's atum mask in-feature), the
     * conditions ride the emission registry (the positive {@code [mod_loaded atum]}).
     */
    public static final ResourceKey<BiomeModifier> ATUM_FLUID_SPRINGS_MODIFIER_KEY = biomeModifierKeyOf("atum_fluid_springs");

    /**
     * The atum large-vein biome-modifier key (task atum-dim-adaptation): the SAME
     * gt6:large_veins placed feature over {@code #gt6:atum_biomes} (the END_YIELD
     * same-placed-key convention; the Feature's biome probe picks the ORE_ATUM rows
     * there — :886-916, 31 rows) at the ore pass, the conditions riding the emission
     * registry (the positive {@code [mod_loaded atum]}).
     */
    public static final ResourceKey<BiomeModifier> ATUM_LARGE_VEINS_MODIFIER_KEY = biomeModifierKeyOf("atum_large_veins");

    /**
     * The atum small-ore biome-modifier key (task atum-dim-adaptation): the 35 atum
     * placement pairs (the Dim.ATUM projection of the shared rows :800-848) over
     * {@code #gt6:atum_biomes} at the ore pass, the hosts riding the
     * {@code #gt6:atum_base_stone} tag arm (the oreTargets ATUM face), the conditions
     * riding the emission registry.
     */
    public static final ResourceKey<BiomeModifier> ATUM_ORES_MODIFIER_KEY = biomeModifierKeyOf("atum_ores");

    /**
     * The atum stone-rows biome-modifier key (task atum-dim-adaptation): the 17-stone
     * band over {@code #gt6:atum_biomes} at the ore step — 12 reused overworld blob
     * placed features + 5 own atum lens-stone placed variants, the twilight_stones
     * row's shape; the conditions ride the emission registry.
     */
    public static final ResourceKey<BiomeModifier> ATUM_STONES_MODIFIER_KEY = biomeModifierKeyOf("atum_stones");

    /**
     * The atum surface-rocks biome-modifier key (task atum-dim-adaptation): the 3,3
     * placed twin over {@code #gt6:atum_biomes} at the vegetal step (the
     * twilight_surface_rocks row's shape, atum numbers), the conditions riding the
     * emission registry.
     */
    public static final ResourceKey<BiomeModifier> ATUM_SURFACE_ROCKS_MODIFIER_KEY = biomeModifierKeyOf("atum_surface_rocks");

    /**
     * The atum bumble-hive biome-modifier key (task atum-dim-adaptation): the SAME
     * gt6:bumble_hives placed feature over {@code #gt6:atum_biomes} (the
     * end_bumble_hives same-placed-key convention; the Feature's atum arm rides the
     * WorldgenHives.java:79-87 case — y16-79, the #gt6:atum_base_stone wall, the
     * yellow/900 hive), the conditions riding the emission registry.
     */
    public static final ResourceKey<BiomeModifier> ATUM_BUMBLE_HIVES_MODIFIER_KEY = biomeModifierKeyOf("atum_bumble_hives");

    /**
     * The planet-mod id of the yield inversion, the SINGLE flip point — the trigger card
     * MUST verify the target planet mod's actual modern modid before shipping the flip
     * (coordinator ruling 2026-09-18; "galacticraft" = the GT6 1.7.10 planet-domain
     * lineage, MD.GC = the ORE_PLANETS flags' original carrier).
     *
     * <p>Driver-face interlink (mdh-4 closeout): the unified registry-side mod-driver face
     * is GT6ModDrivers (mdh series; isLoaded/visibilityGate) — the MD.GC lineage note here
     * is static history and GC is an mdh-2 atlas (GT6ForeignMaterialAtlas) candidate,
     * while the JSON not(mod_loaded) face built on this id STAYS datapack-domain (the
     * KEEP ruling, see GT6BiomeModifierConditions).
     */
    public static final String PLANET_VEIN_TRIGGER_MODID = "galacticraft";

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
                    // size=4 (GTOreWorldgen.ORE_SIZE): sizes 1-2 are mathematically
                    // inert — the walk sphere never reaches a block center from an
                    // integer origin (live census 0/20, 0/8; the b19bcfc6d 2→4 flip)
                    new OreConfiguration(oreTargets(tMaterial, tPair.dim()), GTOreWorldgen.ORE_SIZE));
        }
    }

    /**
     * The WD.setSmallOre host face per dimension (WD.java:765-780), in
     * {@link GTOreWorldgen#hostPaths} rule order — the 3 vanilla granite/diorite/andesite
     * block rows FIRST (they sit inside #stone_ore_replaceables, the tag JSON's 4 entries,
     * vanilla 1.20.1 tags/blocks/stone_ore_replaceables.json:3-6 — a tag-first order would
     * flatten them onto the stone base; the modern-complement fix, research.issues-r3-ore
     * appendix B: upstream 1.7.10 has no vanilla three-stones, the GT same-name families
     * only ever fired inside GT blobs there), then the stone/deepslate tags (the
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
        if (aDim == GTOreWorldgen.Dim.ATUM) {
            // task atum-dim-adaptation — the single #gt6:atum_base_stone tag arm (the
            // deadrock red-line indirection: atum:limestone + atum:karst ride OUR tag file
            // required:false; atum absent = empty tag = the feature replaces nothing), the
            // state is the STONE family's small-ore block (the host-skin declaration).
            return List.of(OreConfiguration.target(new TagMatchTest(GT6Worldgen.ATUM_BASE_STONE), smallState("stone", aMaterial)));
        }
        List<OreConfiguration.TargetBlockState> rTargets = new ArrayList<>(24);
        rTargets.add(OreConfiguration.target(new BlockMatchTest(Blocks.GRANITE), smallState("granite", aMaterial)));
        rTargets.add(OreConfiguration.target(new BlockMatchTest(Blocks.DIORITE), smallState("diorite", aMaterial)));
        rTargets.add(OreConfiguration.target(new BlockMatchTest(Blocks.ANDESITE), smallState("andesite", aMaterial)));
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
        // task c2-deep-band — the deep-band mirrors: PLACED-only (the row's OVERWORLD
        // configured feature is reused verbatim — its [4] target is the
        // #deepslate_ore_replaceables tag arm, so the deep band resolves the deepslate
        // family block), the band = the upstream band shifted DEEP_SHIFT down (the
        // GTOreWorldgen.DEEP_SHIFT rule). Same count chain — the deep twin keeps the
        // surface twin's density.
        for (GTOreWorldgen.SmallOreRow tRow : GTOreWorldgen.deepMirrorRows()) {
            PlacementUtils.register(ctx, GTOreWorldgen.deepPlacedKey(tRow),
                    aFeatures.getOrThrow(GTOreWorldgen.configuredKey(tRow, GTOreWorldgen.Dim.OVERWORLD)),
                    CountPlacement.of(GTOreWorldgen.veinCount(tRow)),
                    InSquarePlacement.spread(),
                    HeightRangePlacement.uniform(VerticalAnchor.absolute(GTOreWorldgen.deepMinY(tRow)),
                            VerticalAnchor.absolute(GTOreWorldgen.deepMaxY(tRow))),
                    BiomeFilter.biome());
        }
    }

    /**
     * The overworld small-ore placed face in modifier order: the surface pairs
     * (placementPairs OVERWORLD, table order) then the deep-band mirrors (task
     * c2-deep-band order). The strata_lenses biome modifier appends this AFTER the
     * lens+companion head — the one-modifier list order is the FeatureSorter chain
     * (lens → companions → small ores), so the band's marble-family target arm always
     * sees lens stone the lens already placed (task lens-ore-base-order).
     */
    private static List<Holder<PlacedFeature>> overworldSmallOrePlaced(HolderGetter<PlacedFeature> aPlaced) {
        List<Holder<PlacedFeature>> tHolders = new ArrayList<>(GTOreWorldgen.placementPairs().size());
        for (GTOreWorldgen.Placement tPair : GTOreWorldgen.placementPairs()) {
            if (tPair.dim() == GTOreWorldgen.Dim.OVERWORLD) tHolders.add(aPlaced.getOrThrow(GTOreWorldgen.placedKey(tPair.row(), tPair.dim())));
        }
        // task c2-deep-band — the deep-band mirrors, appended after the surface pairs
        // ("deep" is a key directory, not a Dim — the deepslate band is overworld content).
        for (GTOreWorldgen.SmallOreRow tRow : GTOreWorldgen.deepMirrorRows()) {
            tHolders.add(aPlaced.getOrThrow(GTOreWorldgen.deepPlacedKey(tRow)));
        }
        return tHolders;
    }

    private static void bootstrapOreBiomeModifiers(
        //? if forge {
        BootstapContext<BiomeModifier> ctx, HolderGetter<Biome> aBiomes, HolderGetter<PlacedFeature> aPlaced
        //?} else {
        /*BootstrapContext<BiomeModifier> ctx, HolderGetter<Biome> aBiomes, HolderGetter<PlacedFeature> aPlaced
        *///?}
    ) {
        // two modifiers, the nether/end faces at the ore pass (the overworld face rides
        // the strata_lenses chain — task lens-ore-base-order); keys follow Dim order.
        TagKey<Biome>[] tDimTags = new TagKey[] {BiomeTags.IS_NETHER, BiomeTags.IS_END};
        GTOreWorldgen.Dim[] tDims = new GTOreWorldgen.Dim[] {GTOreWorldgen.Dim.NETHER, GTOreWorldgen.Dim.END};
        for (int i = 0; i < ORE_BIOME_MODIFIER_KEYS.size(); i++) {
            List<Holder<PlacedFeature>> tHolders = new ArrayList<>(54);
            for (GTOreWorldgen.Placement tPair : GTOreWorldgen.placementPairs()) {
                if (tPair.dim() == tDims[i]) tHolders.add(aPlaced.getOrThrow(GTOreWorldgen.placedKey(tPair.row(), tPair.dim())));
            }
            ctx.register(ORE_BIOME_MODIFIER_KEYS.get(i), addFeatures(aBiomes.getOrThrow(tDimTags[i]),
                    HolderSet.direct(tHolders),
                    GenerationStep.Decoration.UNDERGROUND_ORES));
        }
        // task atum-dim-adaptation — the THIRD small-ore modifier: the 35 atum pairs over
        // OUR OWN #gt6:atum_biomes tag (the ore_small_nether/end shape; no lens chain in
        // atum — the strata lenses are overworld strata), at the ore pass. The atum hosts
        // ride the #gt6:atum_base_stone tag arm (the oreTargets ATUM face); the mod_loaded
        // condition rides the emission registry.
        List<Holder<PlacedFeature>> tAtumHolders = new ArrayList<>(35);
        for (GTOreWorldgen.Placement tPair : GTOreWorldgen.placementPairs()) {
            if (tPair.dim() == GTOreWorldgen.Dim.ATUM) tAtumHolders.add(aPlaced.getOrThrow(GTOreWorldgen.placedKey(tPair.row(), tPair.dim())));
        }
        ctx.register(ATUM_ORES_MODIFIER_KEY, addFeatures(aBiomes.getOrThrow(GT6Worldgen.ATUM_BIOMES),
                HolderSet.direct(tAtumHolders),
                GenerationStep.Decoration.UNDERGROUND_ORES));
    }

    // ------------------------------------------------------------------
    // The lens companion-ore band (task c3-lens-ores) — the C3 bone completion:
    // the upstream stone-LAYER companion-ore table (Loader_Worldgen.java:61-640, the
    // StoneLayerOres ctor rows — the card brief's "WorldgenStoneLayers.java:220-340"
    // was the placement logic; the table itself lives here) bound to the port's
    // 5 marker-stone lenses (the strata-lens compromise form; the per-column layer
    // mode itself stays the KG-deferred custom-Feature card). One row per upstream
    // StoneLayerOres line, verbatim Y band + chance denominator (CS.U48 = CS.U/48,
    // StoneLayerOres.check:90 {@code nextInt(U) < chance} = the 1/N per layer-stone
    // -block face). The two biome-gated gem rows (spinel MOUNTAINS / balasruby
    // JUNGLE) ride the port's dimension-level-only convention (the small-ore band
    // face). Host = the lens stone ONLY (the upstream layer-stone host face:
    // BlockMatchTest on the family anchor). Execution order = the ONE strata_lenses
    // biome modifier AFTER the lens feature (see bootstrapBiomeModifiers).
    //
    // The 53-material registration axis (GT6OreBlocks.WORLDGEN_ORES) is the block
    // universe and the card rules out new ore blocks: rows outside it stay TABLE
    // data only — the GT6VeinGenerator.valid gate face (the molybdenum precedent),
    // 13 of 22 today. The gate is LIVE data, not a frozen list: the axis-extension
    // card (a-ore-axis-extension, B plan ruled — coordinator notice 2026-09-28)
    // widens the axis and these rows start generating with zero edits here (the
    // basalt row set is entirely gated today, so basalt generates no companion ore
    // until then).
    // ------------------------------------------------------------------

    /**
     * One upstream {@code StoneLayerOres} companion row (Loader_Worldgen ctor order
     * material / chance / minY / maxY, plus the lens snake binding it to this port's
     * lens stone). The material rides a supplier (the GTOreWorldgen.SmallOreRow
     * offline posture — this class loads before OP.init).
     *
     * @param lens        the lens stone snake (LENS_STONE_SNAKES member, the ore family snake)
     * @param minY/maxY  the upstream Y band, verbatim
     * @param denominator the N of the upstream 1/N per-layer-stone-block chance (U48 → 48)
     */
    public record LensOreRow(String lens, int minY, int maxY, long denominator,
            java.util.function.Supplier<OreDictMaterial> material) {

        /** The inclusive Y-band width in blocks (the count-formula input). */
        public int bandWidth() { return maxY - minY + 1; }
    }

    /** The MT.* supplier behind the row literals (the GTOreWorldgen.row form). */
    private static LensOreRow lensOre(String aLens, int aMinY, int aMaxY, long aDenominator,
            java.util.function.Supplier<OreDictMaterial> aMaterial) {
        return new LensOreRow(aLens, aMinY, aMaxY, aDenominator, aMaterial);
    }

    /**
     * The 22 upstream companion rows over the 5 lenses, upstream order
     * (Loader_Worldgen.java line cites inline): kimberlite 3 (:225-229), basalt 4
     * (:247-252), marble 6 (:288-295), granite_red 5 (:359-365), komatiite 4 (:217-222).
     * The granite_red tantalite/columbite/coltan rows are the {@code !MD.HBM.mLoaded}
     * arm (:362-364) — no HBM on this port, they are the active upstream face.
     *
     * <p>Driver-face interlink (mdh-4 closeout): the unified mod-driver face is
     * GT6ModDrivers (mdh series; isLoaded/visibilityGate) — this port-time static ruling
     * ships as history; HBM is an mdh-2 atlas (GT6ForeignMaterialAtlas) takeover candidate.
     *
     * <p>mdh-5 axis-takeover census (task mdh-5-block-worldgen-axis, CLOSED — ruling (a),
     * static terminal state): the lens face carries ZERO atlas-PRIMARY rows — the HBM
     * triple-intersect candidate (Columbite: STONE_LAYER_ORES axis member + the granite_red
     * arm above + the atlas hbm row) is COMMON_SECONDARY (upstream MT.java:2398 COMMON_ORE,
     * never hidden). The large-vein ({@code LARGE_VEIN_TABLE}, :1433) and bedrock
     * ({@code BEDROCK_ORE_TABLE}, :1591) tables carry atlas-PRIMARY slots only in DEAD or
     * OFFWORLD positions (apatite/lapis/monazite/titanium/adamantium/dolamide veins;
     * bedrock dolamide/adamantine), and the lapis vein's live Azurite spread slot stays
     * guarded at its consumers — no dead-reference face, no driver gate. Cross-table +
     * per-face rulings: GT6AxisTakeoverCensusTest, decisions.mdh-5-axis-rulings.
     */
    public static final List<LensOreRow> LENS_ORE_TABLE = List.of(
        // -- kimberlite, :225-229 — the diamond-pipe bone ----------------------------
        lensOre("kimberlite",  0, 12, 48, () -> MT.Diamond),
        lensOre("kimberlite", 24, 48, 48, () -> MT.Spinel),                     // + BIOMES_MOUNTAINS (dropped, dim-level-only)
        lensOre("kimberlite", 24, 48, 48, () -> MT.BalasRuby),                  // + BIOMES_JUNGLE (dropped, dim-level-only)
        // -- basalt, :247-252 — ALL FOUR outside the 53-axis (table data only today) --
        lensOre("basalt",  0, 32, 32, () -> MT.Peridot),
        lensOre("basalt",  8, 40, 32, () -> MT.Uvarovite),
        lensOre("basalt", 16, 48, 32, () -> MT.Grossular),
        lensOre("basalt", 32, 64,  8, () -> MT.OREMATS.Chromite),
        // -- marble, :288-295 — the cassiterite lens ---------------------------------
        lensOre("marble", 20, 80, 16, () -> MT.OREMATS.Cassiterite),
        lensOre("marble", 38, 82, 16, () -> MT.OREMATS.Stannite),
        lensOre("marble", 38, 82, 16, () -> MT.OREMATS.Kesterite),
        lensOre("marble", 10, 30,  8, () -> MT.OREMATS.Sphalerite),
        lensOre("marble",  0, 20,  8, () -> MT.OREMATS.Chalcopyrite),
        lensOre("marble",  0, 30, 12, () -> MT.Pyrite),
        // -- granite_red, :359-365 — the !MD.HBM arm rows ship ------------------------
        lensOre("granite_red",  0, 18, 32, () -> MT.OREMATS.Pitchblende),
        lensOre("granite_red",  0, 16, 32, () -> MT.OREMATS.Uraninite),
        lensOre("granite_red", 30, 40, 64, () -> MT.OREMATS.Tantalite),
        lensOre("granite_red", 30, 40, 64, () -> MT.OREMATS.Columbite),
        lensOre("granite_red", 20, 50, 16, () -> MT.OREMATS.Coltan),
        // -- komatiite, :217-222 ------------------------------------------------------
        lensOre("komatiite", 20, 50, 16, () -> MT.MgCO3),
        lensOre("komatiite",  0, 32, 12, () -> MT.OREMATS.Cinnabar),
        lensOre("komatiite",  0, 30,  8, () -> MT.Redstone),
        lensOre("komatiite",  0, 30, 12, () -> MT.Pyrite));

    /** The alias-resolved row material (the GTOreWorldgen.resolve walk, LensOreRow shape). */
    public static OreDictMaterial lensOreResolve(LensOreRow aRow) {
        OreDictMaterial tMaterial = aRow.material().get();
        if (tMaterial == null || tMaterial.mID < 0) return null;
        return MaterialRegistry.INSTANCE.get(tMaterial); // alias slot -> target (MaterialRegistry.java:182-185)
    }

    /** The registration-universe validity (the GT6VeinGenerator.valid face: 53-axis membership). */
    public static boolean lensOreValid(LensOreRow aRow) {
        OreDictMaterial tMaterial = lensOreResolve(aRow);
        return tMaterial != null && GT6OreBlocks.materialAxis().contains(tMaterial);
    }

    /** The generated rows: LENS_ORE_TABLE minus the axis-gated rows, table order. */
    public static List<LensOreRow> lensOreRows() {
        return LENS_ORE_TABLE.stream().filter(GT6WorldgenDatagen::lensOreValid).toList();
    }

    /**
     * The per-chunk Count constant: {@code max(1, round(bandWidth / (denominator × 1.5)))}.
     * The upstream face is a 1/N chance per layer-stone block in band; the vanilla
     * Feature.ORE proxy knows nothing of host counts, so the projection anchors on the
     * nominal one-block-band slab (16×16 columns × 1 block) divided by N and by the
     * size-4 walk mean 1.5 blocks per attempt (GTOreWorldgen.ORE_SIZE javadoc). The
     * RELATIVE frequencies stay upstream (bandWidth/N — cassiterite outmasses diamond
     * ~14:1, chromite outmasses peridot 4:1); the absolute scale is the declared
     * lens-form compromise (the layer mode's whole-world columns do not exist under
     * the lens form — the ponytail calibration knob, one formula to retune).
     */
    public static int lensOreCount(LensOreRow aRow) {
        return Math.max(1, Math.round(aRow.bandWidth() / (aRow.denominator() * 1.5f)));
    }

    /** The configured-feature key of a row ({@code gt6:ore_lens/<lens>_<material-snake>}). */
    public static ResourceKey<ConfiguredFeature<?, ?>> lensOreConfiguredKey(LensOreRow aRow) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, lensOreLocation(aRow));
    }

    /** The placed-feature key of a row (same path as its configured sibling). */
    public static ResourceKey<PlacedFeature> lensOrePlacedKey(LensOreRow aRow) {
        return ResourceKey.create(Registries.PLACED_FEATURE, lensOreLocation(aRow));
    }

    private static ResourceLocation lensOreLocation(LensOreRow aRow) {
        return ResourceLocation.fromNamespaceAndPath("gt6", "ore_lens/" + aRow.lens() + "_"
                + GTMaterialItems.snakeCase(lensOreResolve(aRow).mNameInternal));
    }

    /**
     * The lens stone snake → the ore family snake (the GT6OreBlocks.stoneBlockSnake
     * naming splits reversed — GTStoneBlocks "granite_red" carries the "redgranite"
     * family; every other lens snake is identical).
     */
    private static String lensOreFamilySnake(String aLens) {
        return "granite_red".equals(aLens) ? "redgranite" : aLens;
    }

    /** The lens companion configured features: vanilla Feature.ORE over the lens stone anchor ONLY. */
    private static void bootstrapLensOreConfigured(
        //? if forge {
        BootstapContext<ConfiguredFeature<?, ?>> ctx
        //?} else {
        /*BootstrapContext<ConfiguredFeature<?, ?>> ctx
        *///?}
    ) {
        for (LensOreRow tRow : lensOreRows()) {
            String tFamily = lensOreFamilySnake(tRow.lens());
            FeatureUtils.register(ctx, lensOreConfiguredKey(tRow), Feature.ORE,
                    // single host target — the lens stone itself (the upstream layer-stone
                    // host face; no vanilla-stone targets: the companion ore is the lens's
                    // own bone, not a world-scatter)
                    new OreConfiguration(List.of(OreConfiguration.target(
                            new BlockMatchTest(GTOreWorldgen.oreFamily(tFamily).stoneAnchor().get()),
                            smallState(tFamily, lensOreResolve(tRow)))), GTOreWorldgen.ORE_SIZE));
        }
    }

    /** The lens companion placed features: Count(constant)+InSquare+HeightRange(uniform band)+BiomeFilter. */
    private static void bootstrapLensOrePlaced(
        //? if forge {
        BootstapContext<PlacedFeature> ctx, HolderGetter<ConfiguredFeature<?, ?>> aFeatures
        //?} else {
        /*BootstrapContext<PlacedFeature> ctx, HolderGetter<ConfiguredFeature<?, ?>> aFeatures
        *///?}
    ) {
        for (LensOreRow tRow : lensOreRows()) {
            PlacementUtils.register(ctx, lensOrePlacedKey(tRow),
                    aFeatures.getOrThrow(lensOreConfiguredKey(tRow)),
                    CountPlacement.of(lensOreCount(tRow)),
                    InSquarePlacement.spread(),
                    // the upstream [minY, maxY] band verbatim (the small-ore overworld face;
                    // every band fits the modern heights as-is)
                    HeightRangePlacement.uniform(VerticalAnchor.absolute(tRow.minY()),
                            VerticalAnchor.absolute(tRow.maxY())),
                    BiomeFilter.biome());
        }
    }

    // ------------------------------------------------------------------
    // The twilight RockOres band (task twilight-adaptation-pilot — the mod-dimension
    // adaptation skeleton's first tenant). The 8-row census table lives in GTOreWorldgen
    // (TWILIGHT_ORE_ROWS, Loader_Worldgen.java:666-673 verbatim); ONLY the axis-valid rows
    // emit (the axis gate — a registrable ore block universe is the emission precondition,
    // the GTVeinConfig validity face), the rest stay table data for the axis extension to
    // revive. All faces below hang off the ONE twilight_ores biome modifier
    // ({@link #TWILIGHT_ORES_MODIFIER_KEY}); the mod_loaded conditions ride the emission
    // registry (GT6BiomeModifierConditions.CONDITION_ROWS), never the bootstrap.
    // ------------------------------------------------------------------

    /**
     * The axis-valid twilight configured features: vanilla {@code Feature.ORE} at the row's
     * verbatim size (WorldgenBlob.java:55 bind — every twilight size is under the 64 codec
     * cap) over the row's host face. The {@code hostTag == null} rows keep the upstream
     * {@code replaceBlock=null} face: the vanilla default {@code target == Blocks.stone}
     * (WorldgenOresVanilla.java:53 {@code isReplaceableOreGen}) and the material's NORMAL
     * ore-stone state (the dense-block translation: upstream places BlocksGT.RockOres/
     * VanillaOresA — OP.oreDense/oreVanillastone carriers, this port has no dense form,
     * NORMAL is the full-block near kin — the declared deviation, coordinator-approved
     * 2026-10-05; the dense prefix is a future axis-extension-family card). The netherite
     * row is tag-hosted: {@code TagMatchTest(#gt6:tf_deadrock)} over the vanilla
     * ancient-debris state — the foreign block id lives only in the tag file, never here
     * (the configured-feature JSON red line).
     */
    private static void bootstrapTwilightOreConfigured(
        //? if forge {
        BootstapContext<ConfiguredFeature<?, ?>> ctx
        //?} else {
        /*BootstrapContext<ConfiguredFeature<?, ?>> ctx
        *///?}
    ) {
        for (GTOreWorldgen.TwilightOreRow tRow : GTOreWorldgen.twilightOnAxisRows()) {
            OreConfiguration.TargetBlockState tTarget;
            if (tRow.hostTag() == null) {
                tTarget = OreConfiguration.target(new BlockMatchTest(Blocks.STONE),
                        GT6OreBlocks.get(GTOreWorldgen.oreFamily("stone"), GT6OreBlocks.FormKind.NORMAL,
                                resolve(tRow)).get().defaultBlockState());
            } else {
                tTarget = OreConfiguration.target(
                        new TagMatchTest(GTOreWorldgen.twilightHostTag(tRow)),
                        Blocks.ANCIENT_DEBRIS.defaultBlockState());
            }
            FeatureUtils.register(ctx, GTOreWorldgen.twilightConfiguredKey(tRow), Feature.ORE,
                    new OreConfiguration(List.of(tTarget), tRow.size()));
        }
    }

    /**
     * The axis-valid twilight placed features: [rarity?][count?] + InSquare + the row's
     * verbatim uniform band + BiomeFilter. Rarity only when probability &gt; 1 (the 1/N
     * chunk gate — probability 1 = every chunk = no filter, the old band's 1/100 shape
     * unchanged); count only when amount &gt; 1 (Amount=1 IS the default count, the blob
     * band convention; the WorldgenBlob amount loop is a plain for, mirrored 1:1 by the
     * count attempts).
     */
    private static void bootstrapTwilightOrePlaced(
        //? if forge {
        BootstapContext<PlacedFeature> ctx, HolderGetter<ConfiguredFeature<?, ?>> aFeatures
        //?} else {
        /*BootstrapContext<PlacedFeature> ctx, HolderGetter<ConfiguredFeature<?, ?>> aFeatures
        *///?}
    ) {
        for (GTOreWorldgen.TwilightOreRow tRow : GTOreWorldgen.twilightOnAxisRows()) {
            List<PlacementModifier> tChain = new ArrayList<>(5);
            if (tRow.probability() > 1) tChain.add(RarityFilter.onAverageOnceEvery(tRow.probability()));
            if (tRow.amount() > 1) tChain.add(CountPlacement.of(tRow.amount()));
            tChain.add(InSquarePlacement.spread());
            tChain.add(HeightRangePlacement.uniform(VerticalAnchor.absolute(tRow.minY()),
                    VerticalAnchor.absolute(tRow.maxY())));
            tChain.add(BiomeFilter.biome());
            PlacementUtils.register(ctx, GTOreWorldgen.twilightPlacedKey(tRow),
                    aFeatures.getOrThrow(GTOreWorldgen.twilightConfiguredKey(tRow)),
                    tChain.toArray(new PlacementModifier[0]));
        }
    }

    /** The row's resolved registration material — the {@link GTOreWorldgen#resolve} alias walk over a twilight supplier. */
    private static OreDictMaterial resolve(GTOreWorldgen.TwilightOreRow aRow) {
        OreDictMaterial tMaterial = aRow.material().get();
        if (tMaterial == null || tMaterial.mID < 0) return null;
        return MaterialRegistry.INSTANCE.get(tMaterial); // alias slot -> target (MaterialRegistry.java:182-185)
    }

    // ------------------------------------------------------------------
    // The twilight stone-row band (task twilight-stone-rows) — the WorldgenStone
    // loop's twilight row per stone (Loader_Worldgen.java:657; the constants +
    // erratum javadoc live in {@link GT6Worldgen}). 17 placed variants hanging off
    // the reuse-or-own configured face, ALL mounted by the ONE
    // {@link #TWILIGHT_STONES_MODIFIER_KEY} row; the mod_loaded condition rides the
    // emission registry (GT6BiomeModifierConditions.CONDITION_ROWS), never the
    // bootstrap.
    // ------------------------------------------------------------------

    /**
     * The 5 twilight-only blob configured features (the lens marker stones — their
     * overworld blob rows are retired, so no configured to reuse): vanilla
     * {@code Feature.ORE} byte-shape-identical to the 12-blob band (GT6WorldgenDatagen
     * .bootstrapConfigured head loop) — the single {@code stone_ore_replaceables ->
     * GTStoneBlocks.<snake>(STONE)} target at {@link GT6Worldgen#oreBlobSize()} (the
     * upstream twilight size 100 clamps to the same 64 the overworld 200 clamps to).
     */
    private static void bootstrapTwilightStoneConfigured(
        //? if forge {
        BootstapContext<ConfiguredFeature<?, ?>> ctx
        //?} else {
        /*BootstrapContext<ConfiguredFeature<?, ?>> ctx
        *///?}
    ) {
        for (String tSnake : GT6Worldgen.LENS_STONE_SNAKES) {
            FeatureUtils.register(ctx, GT6Worldgen.twilightStoneConfiguredKey(tSnake), Feature.ORE,
                    new OreConfiguration(
                            List.of(OreConfiguration.target(new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES),
                                    GTStoneBlocks.block(tSnake, StoneVariant.STONE).get().defaultBlockState())),
                            GT6Worldgen.oreBlobSize()));
        }
    }

    /**
     * The 17 twilight placed variants (STONES order — the acceptance 17-row audit unit):
     * RarityFilter 1/200 (Loader_Worldgen.java:657 probability 200 — the WorldgenBlob
     * .java:55 {@code nextInt(mProbability) == 0} chunk gate) + InSquare + BiomeFilter +
     * the verbatim uniform Y [0, 40] band (the overworld blob chain shape at the twilight
     * numbers; NO count modifier — Amount=1 IS the default count).
     */
    private static void bootstrapTwilightStonePlaced(
        //? if forge {
        BootstapContext<PlacedFeature> ctx, HolderGetter<ConfiguredFeature<?, ?>> aFeatures
        //?} else {
        /*BootstrapContext<PlacedFeature> ctx, HolderGetter<ConfiguredFeature<?, ?>> aFeatures
        *///?}
    ) {
        for (int i = 0; i < GT6Worldgen.TWILIGHT_STONE_PLACED_KEYS.size(); i++) {
            String tSnake = GTStoneBlocks.STONES.get(i).snake();
            PlacementUtils.register(ctx, GT6Worldgen.TWILIGHT_STONE_PLACED_KEYS.get(i),
                    aFeatures.getOrThrow(GT6Worldgen.twilightStoneConfiguredKey(tSnake)),
                    RarityFilter.onAverageOnceEvery(GT6Worldgen.TWILIGHT_STONE_PROBABILITY),
                    InSquarePlacement.spread(),
                    BiomeFilter.biome(),
                    HeightRangePlacement.uniform(VerticalAnchor.absolute(GT6Worldgen.TWILIGHT_STONE_MIN_Y),
                            VerticalAnchor.absolute(GT6Worldgen.TWILIGHT_STONE_MAX_Y)));
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
        // task twilight-vegetation — the twilight.rocks row (upstream GEN_TWILIGHT,
        // Loader_Worldgen.java:621): our own 12-member optional tag over the placed twin,
        // at the same VEGETAL_DECORATION step; the mod_loaded condition rides the
        // emission registry (the twilight_ores division of labor).
        ctx.register(TWILIGHT_SURFACE_ROCKS_MODIFIER_KEY, addFeatures(
                aBiomes.getOrThrow(GT6Worldgen.SURFACE_ROCKS_TWILIGHT_BIOMES),
                HolderSet.direct(aPlaced.getOrThrow(GT6Worldgen.TWILIGHT_SURFACE_ROCKS_PLACED)),
                GenerationStep.Decoration.VEGETAL_DECORATION));
        // task atum-dim-adaptation — the atum rocks modifier: the 3,3 placed twin over
        // #gt6:atum_biomes at the vegetal step (the overworld rocks row's step), the
        // mod_loaded condition riding the emission registry.
        ctx.register(ATUM_SURFACE_ROCKS_MODIFIER_KEY, addFeatures(aBiomes.getOrThrow(GT6Worldgen.ATUM_BIOMES),
                HolderSet.direct(aPlaced.getOrThrow(GT6Worldgen.ATUM_SURFACE_ROCKS_PLACED)),
                GenerationStep.Decoration.VEGETAL_DECORATION));
    }

    // ------------------------------------------------------------------
    // The atum stone-row band (task atum-dim-adaptation) — the WorldgenStone loop's
    // atum row per stone (Loader_Worldgen.java:659: amount 1 / size 200 / probability
    // 100 / Y 0-120 — the OVERWORLD row numbers verbatim). The 12 blob stones REUSE the
    // overworld placed features outright (identical rarity/Y chains — the modifier is
    // the only new face); the 5 marker stones get own configured/placed (their overworld
    // blob rows are retired to the strata lenses, the twilight-stone-rows reuse-or-own
    // form at atum numbers). ALL mounted by the ONE {@link #ATUM_STONES_MODIFIER_KEY}
    // row; the mod_loaded condition rides the emission registry.
    // ------------------------------------------------------------------

    /**
     * The 5 atum-only blob configured features (the lens marker stones): vanilla
     * {@code Feature.ORE} byte-shape-identical to the 12-blob band — the single
     * {@code stone_ore_replaceables -> GTStoneBlocks.<snake>(STONE)} target at
     * {@link GT6Worldgen#oreBlobSize()} (the upstream atum size 200 clamps to the same
     * 64 the overworld 200 clamps to).
     */
    private static void bootstrapAtumStoneConfigured(
        //? if forge {
        BootstapContext<ConfiguredFeature<?, ?>> ctx
        //?} else {
        /*BootstrapContext<ConfiguredFeature<?, ?>> ctx
        *///?}
    ) {
        for (String tSnake : GT6Worldgen.LENS_STONE_SNAKES) {
            FeatureUtils.register(ctx, GT6Worldgen.atumStoneConfiguredKey(tSnake), Feature.ORE,
                    new OreConfiguration(
                            List.of(OreConfiguration.target(new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES),
                                    GTStoneBlocks.block(tSnake, StoneVariant.STONE).get().defaultBlockState())),
                            GT6Worldgen.oreBlobSize()));
        }
    }

    /**
     * The 5 atum lens-stone placed variants: RarityFilter 1/100 (Loader_Worldgen.java:659
     * probability 100 — the WorldgenBlob.java:55 chunk gate) + InSquare + BiomeFilter +
     * the verbatim uniform Y [0, 120] band (NO count modifier — Amount=1 IS the default
     * count).
     */
    private static void bootstrapAtumStonePlaced(
        //? if forge {
        BootstapContext<PlacedFeature> ctx, HolderGetter<ConfiguredFeature<?, ?>> aFeatures
        //?} else {
        /*BootstrapContext<PlacedFeature> ctx, HolderGetter<ConfiguredFeature<?, ?>> aFeatures
        *///?}
    ) {
        for (String tSnake : GT6Worldgen.LENS_STONE_SNAKES) {
            PlacementUtils.register(ctx, GT6Worldgen.atumStonePlacedKey(tSnake),
                    aFeatures.getOrThrow(GT6Worldgen.atumStoneConfiguredKey(tSnake)),
                    RarityFilter.onAverageOnceEvery(GT6Worldgen.BLOB_PROBABILITY),
                    InSquarePlacement.spread(),
                    BiomeFilter.biome(),
                    HeightRangePlacement.uniform(VerticalAnchor.absolute(GT6Worldgen.OVERWORLD_MIN_Y),
                            VerticalAnchor.absolute(GT6Worldgen.OVERWORLD_MAX_Y)));
        }
    }

    // ------------------------------------------------------------------
    // The surface-plants + soil band (task w6-t2-surface-blocks):
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

    /**
     * The band's nine biome-modifier keys, worldgen order (glowtus/bush/blacksand/turf/pit + the 4 logs); the
     * four colored-clay pit rows (task worldgen-diggables-pits) ride {@link #PIT_CLAY_MODIFIER_KEYS} — one key
     * per row, the per-row modifier family face (the PLANT list shape), on the SHARED pit biome tag.
     */
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

    /** The 4 colored-clay pit modifier keys, {@link GT6Worldgen#PIT_CLAY_PATHS} order (all on the shared gt6:surface_pit_clay tag). */
    public static final List<ResourceKey<BiomeModifier>> PIT_CLAY_MODIFIER_KEYS =
            GT6Worldgen.PIT_CLAY_PATHS.stream().map(GT6WorldgenDatagen::biomeModifierKeyOf).toList();

    private static void bootstrapPlantsConfigured(
        //? if forge {
        BootstapContext<ConfiguredFeature<?, ?>> ctx
        //?} else {
        /*BootstrapContext<ConfiguredFeature<?, ?>> ctx
        *///?}
    ) {
        FeatureUtils.register(ctx, GT6Worldgen.GLOWTUS_CONFIGURED, Feature.SIMPLE_BLOCK,
                new SimpleBlockConfiguration(BlockStateProvider.simple(GT6SurfaceBlocks.GLOWTUS.get().defaultBlockState())));
        // The bush (task bush-growth-blockstate): the KIND nine weighted-uniform, each
        // state at the upstream birth stage 3 VERBATIM (placeBushCore :86 NBT_STATE 3 —
        // the 2026-10-01 coordinator ruling: the birth stage follows the upstream; only
        // the growth/harvest mechanics ride the vanilla homolog). DECLARED: the upstream
        // kind pick is the position-noise index over BushesGT.MAP (WorldgenBushes.java:66
        // NoiseGenerator aX/2,300,aZ/2) — the port collapses the noise patching to
        // per-placement uniform over the MAP full set (the nine kinds, gooseberry has no
        // MAP put upstream either).
        BlockState tBushRipe = GT6SurfaceBlocks.BERRY_BUSH.get().defaultBlockState()
                .setValue(GT6WildBushBlock.AGE, GT6WildBushBlock.MAX_AGE);
        FeatureUtils.register(ctx, GT6Worldgen.BUSH_CONFIGURED, Feature.SIMPLE_BLOCK,
                new SimpleBlockConfiguration(new WeightedStateProvider(
                        new SimpleWeightedRandomList.Builder<BlockState>()
                                .add(tBushRipe.setValue(GT6WildBushBlock.KIND, GT6WildBushBlock.Kind.BLUEBERRY), 1)
                                .add(tBushRipe.setValue(GT6WildBushBlock.KIND, GT6WildBushBlock.Kind.CANDLEBERRY), 1)
                                .add(tBushRipe.setValue(GT6WildBushBlock.KIND, GT6WildBushBlock.Kind.CRANBERRY), 1)
                                .add(tBushRipe.setValue(GT6WildBushBlock.KIND, GT6WildBushBlock.Kind.CURRANTS_BLACK), 1)
                                .add(tBushRipe.setValue(GT6WildBushBlock.KIND, GT6WildBushBlock.Kind.CURRANTS_WHITE), 1)
                                .add(tBushRipe.setValue(GT6WildBushBlock.KIND, GT6WildBushBlock.Kind.CURRANTS_RED), 1)
                                .add(tBushRipe.setValue(GT6WildBushBlock.KIND, GT6WildBushBlock.Kind.BLACKBERRY), 1)
                                .add(tBushRipe.setValue(GT6WildBushBlock.KIND, GT6WildBushBlock.Kind.RASPBERRY), 1)
                                .add(tBushRipe.setValue(GT6WildBushBlock.KIND, GT6WildBushBlock.Kind.COTTON), 1))));
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
        // The four colored-clay pits (task worldgen-diggables-pits, Loader_Worldgen.java
        // :593/:595-597): the pit_clay_vanilla clone family — same DISK face over the same
        // soil target (WorldgenPit.java:67-69), the pit fills its own colored-clay block
        // (the Diggables meta stand-in). The :594 red row stays OUT (nether-only closure).
        for (int i = 0; i < GT6Worldgen.PIT_CLAY_CONFIGURED_KEYS.size(); i++) {
            FeatureUtils.register(ctx, GT6Worldgen.PIT_CLAY_CONFIGURED_KEYS.get(i), Feature.DISK, new DiskConfiguration(
                    RuleBasedBlockStateProvider.simple(GT6SurfaceBlocks.CLAY_BAND.get(i).get()), tSoil,
                    GT6Worldgen.SOIL_DISK_RADIUS, GT6Worldgen.PIT_CLAY_HALF_HEIGHT));
        }
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
        // The four colored-clay pits (task worldgen-diggables-pits): the pit_clay_vanilla
        // placed chain verbatim — the 1/320 chunk gate is the SHARED divider (all five pit
        // rows carry tChance=320, Loader_Worldgen.java:584).
        for (int i = 0; i < GT6Worldgen.PIT_CLAY_PLACED_KEYS.size(); i++) {
            PlacementUtils.register(ctx, GT6Worldgen.PIT_CLAY_PLACED_KEYS.get(i),
                    aFeatures.getOrThrow(GT6Worldgen.PIT_CLAY_CONFIGURED_KEYS.get(i)),
                    RarityFilter.onAverageOnceEvery(GT6Worldgen.PIT_CLAY_DIVIDER),
                    InSquarePlacement.spread(),
                    PlacementUtils.HEIGHTMAP,
                    BiomeFilter.biome());
        }
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
        // The four colored-clay pit modifiers (task worldgen-diggables-pits): per-row keys
        // over the SHARED pit biome tag (WorldgenPit.java:58 plains|savanna — all five pit
        // rows carry the identical biome list), the disk step.
        for (int i = 0; i < PIT_CLAY_MODIFIER_KEYS.size(); i++) {
            ctx.register(PIT_CLAY_MODIFIER_KEYS.get(i), addFeatures(aBiomes.getOrThrow(GT6Worldgen.PIT_CLAY_BIOMES),
                    HolderSet.direct(aPlaced.getOrThrow(GT6Worldgen.PIT_CLAY_PLACED_KEYS.get(i))),
                    GenerationStep.Decoration.LOCAL_MODIFICATIONS));
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
    // The large-vein band (task w6-t3-large-veins) — the 40-row vein table,
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
        return new GTVeinConfig(aName, aMinY, aMaxY, aWeight, aDensity, aSize, 0, true, true, false, false, aTop, aBottom, aBetween, aSpread);
    }

    /** The row helper for the offworld rows (:917-925 — never drawn overworld, kept for the table census). */
    private static GTVeinConfig veinOffworld(String aName, int aMinY, int aMaxY, int aWeight, int aDensity, int aSize,
            OreDictMaterial aTop, OreDictMaterial aBottom, OreDictMaterial aBetween, OreDictMaterial aSpread) {
        return new GTVeinConfig(aName, aMinY, aMaxY, aWeight, aDensity, aSize, 0, true, false, false, false, aTop, aBottom, aBetween, aSpread);
    }

    /**
     * The row helper for the ORE_END+ORE_OVERWORLD rows (task nether-lens-end-yield:
     * platinum :904 / molybdenum :905 / cassiterite :906 — drawn in BOTH dimensions).
     */
    private static GTVeinConfig veinEnd(String aName, int aMinY, int aMaxY, int aWeight, int aDensity, int aSize,
            OreDictMaterial aTop, OreDictMaterial aBottom, OreDictMaterial aBetween, OreDictMaterial aSpread) {
        return new GTVeinConfig(aName, aMinY, aMaxY, aWeight, aDensity, aSize, 0, true, true, true, false, aTop, aBottom, aBetween, aSpread);
    }

    /**
     * The row helper for the pure-alien ORE_END rows (:918 naquadah / :919 trinium —
     * MARS+PLANETS+ASTEROIDS+END, no overworld; the END flag IS their vanilla+-pack
     * lifeline, the yield ruling's subject matter).
     */
    private static GTVeinConfig veinOffworldEnd(String aName, int aMinY, int aMaxY, int aWeight, int aDensity, int aSize,
            OreDictMaterial aTop, OreDictMaterial aBottom, OreDictMaterial aBetween, OreDictMaterial aSpread) {
        return new GTVeinConfig(aName, aMinY, aMaxY, aWeight, aDensity, aSize, 0, true, false, true, false, aTop, aBottom, aBetween, aSpread);
    }

    /**
     * The row helper for the ORE_ATUM+ORE_OVERWORLD rows (task atum-dim-adaptation:
     * the :886-916 block minus the three ORE_END rows — 28 rows, drawn in BOTH the
     * overworld and atum; the atum arm rides the {@link GT6Worldgen#ATUM_DIMENSION_SALT}
     * stream and the #gt6:atum_base_stone host face).
     */
    private static GTVeinConfig veinAtum(String aName, int aMinY, int aMaxY, int aWeight, int aDensity, int aSize,
            OreDictMaterial aTop, OreDictMaterial aBottom, OreDictMaterial aBetween, OreDictMaterial aSpread) {
        return new GTVeinConfig(aName, aMinY, aMaxY, aWeight, aDensity, aSize, 0, true, true, false, true, aTop, aBottom, aBetween, aSpread);
    }

    /** The row helper for the ORE_ATUM+ORE_END+ORE_OVERWORLD triple (:904-906 — the three-row intersection). */
    private static GTVeinConfig veinEndAtum(String aName, int aMinY, int aMaxY, int aWeight, int aDensity, int aSize,
            OreDictMaterial aTop, OreDictMaterial aBottom, OreDictMaterial aBetween, OreDictMaterial aSpread) {
        return new GTVeinConfig(aName, aMinY, aMaxY, aWeight, aDensity, aSize, 0, true, true, true, true, aTop, aBottom, aBetween, aSpread);
    }

    /** The ONE 40-row large-vein table — the card spec ② "40 脉合一张 JSON 脉表". */
    public static final List<GTVeinConfig> LARGE_VEIN_TABLE = List.of(
        veinAtum("ore.large.lignite"     , 50, 130, 160, 8, 32, MT.Lignite                     , MT.Lignite                     , MT.Lignite                     , MT.Coal               ), // :886
        veinAtum("ore.large.coal"        , 50,  80,  80, 6, 32, MT.Coal                         , MT.Coal                         , MT.Coal                         , MT.Lignite            ), // :887
        veinAtum("ore.large.apatite"     , 40,  60,  60, 3, 16, MT.Apatite                      , MT.Apatite                      , MT.PhosphorusBlue               , MT.PO4                ), // :888
        veinAtum("ore.large.lapis"       , 20,  50,  40, 5, 16, MT.Lazurite                     , MT.Sodalite                     , MT.Lapis                        , MT.Azurite            ), // :889
        veinAtum("ore.large.bauxite"     , 50,  90,  80, 4, 24, MT.OREMATS.Bauxite              , MT.OREMATS.Bauxite              , MT.OREMATS.Bauxite              , MT.OREMATS.Ilmenite   ), // :890
        veinAtum("ore.large.iodinesalt"  , 50,  60,  30, 3, 24, MT.KIO3                         , MT.NaCl                         , MT.OREMATS.Borax                , MT.OREMATS.Zeolite    ), // :891
        veinAtum("ore.large.rocksalt"    , 50,  60,  30, 3, 24, MT.KCl                          , MT.OREMATS.Coltan               , MT.OREMATS.Lepidolite           , MT.OREMATS.Spodumene  ), // :892
        veinAtum("ore.large.asbestos"    , 10,  40,  30, 3, 16, MT.OREMATS.Chromite             , MT.Talc                         , MT.Gypsum                       , MT.Asbestos           ), // :893
        veinAtum("ore.large.sapphire"    , 10,  40,  30, 3, 16, MT.BlueSapphire                 , MT.OrangeSapphire               , MT.YellowSapphire               , MT.Ruby               ), // :894
        veinAtum("ore.large.sapphire2"   , 10,  40,  30, 3, 16, MT.GreenSapphire                , MT.Ruby                         , MT.BlueSapphire                 , MT.PurpleSapphire     ), // :895
        veinAtum("ore.large.garnet"      , 10,  40,  60, 3, 16, MT.Almandine                    , MT.Pyrope                       , MT.Andradite                    , MT.Uvarovite          ), // :896
        veinAtum("ore.large.pitchblende" , 10,  40,  40, 3, 16, MT.OREMATS.Pitchblende          , MT.OREMATS.Pitchblende          , MT.OREMATS.Uraninite            , MT.OREMATS.Uraninite  ), // :897
        veinAtum("ore.large.monazite"    , 10,  40,  30, 3, 16, MT.OREMATS.Bastnasite           , MT.OREMATS.Bastnasite           , MT.Monazite                     , MT.Nd                 ), // :898
        veinAtum("ore.large.diamond"     ,  5,  20,  40, 2, 16, MT.Graphite                     , MT.Graphite                     , MT.Diamond                      , MT.Graphite           ), // :899
        veinAtum("ore.large.galena"      , 30,  60,  40, 5, 16, MT.OREMATS.Galena               , MT.OREMATS.Galena               , MT.Ag                           , MT.Pb                 ), // :900
        veinAtum("ore.large.quartz"      , 40,  80,  60, 3, 16, MT.MilkyQuartz                  , MT.OREMATS.Barite               , MT.CertusQuartz                 , MT.CertusQuartz       ), // :901
        veinAtum("ore.large.peridot"     , 10,  40,  60, 3, 16, MT.OREMATS.Kyanite              , MT.MgCO3                        , MT.Peridot                      , MT.OREMATS.Glauconite ), // :902
        veinAtum("ore.large.gold"        , 20,  30,   5, 3, 16, MT.Pyrite                       , MT.OREMATS.Chalcopyrite         , MT.OREMATS.Arsenopyrite         , MT.Au                 ), // :903
        veinEndAtum("ore.large.platinum"    , 40,  50,   5, 3, 16, MT.OREMATS.Cooperite            , MT.Pd                           , MT.OREMATS.Sperrylite           , MT.Ir                 ), // :904 ORE_END
        veinEndAtum("ore.large.molybdenum"  , 20,  50,   5, 3, 16, MT.OREMATS.Wulfenite            , MT.OREMATS.Molybdenite          , MT.Mo                           , MT.OREMATS.Powellite  ), // :905 ORE_END
        veinEndAtum("ore.large.cassiterite" , 40,  90, 170, 5, 24, MT.OREMATS.Stannite             , MT.OREMATS.Kesterite            , MT.OREMATS.Huebnerite           , MT.OREMATS.Cassiterite), // :906 ORE_END
        veinAtum("ore.large.tungstate"   , 20,  50,  10, 3, 16, MT.OREMATS.Scheelite            , MT.OREMATS.Russellite           , MT.OREMATS.Tungstate            , MT.OREMATS.Pinalite   ), // :907
        veinAtum("ore.large.manganese"   , 20,  30,  20, 3, 16, MT.Grossular                    , MT.Spessartine                  , MT.MnO2                         , MT.OREMATS.Coltan     ), // :908
        veinAtum("ore.large.beryllium"   ,  5,  30,  15, 3, 16, MT.Aquamarine                   , MT.Maxixe                       , MT.Emerald                      , MT.Th                 ), // :909
        veinAtum("ore.large.beryllium2"  ,  5,  30,  15, 3, 16, MT.Bixbite                      , MT.Goshenite                    , MT.Heliodor                     , MT.Morganite          ), // :910
        veinAtum("ore.large.titanium"    , 10,  40,  40, 3, 16, MT.TiO2                         , MT.TiO2                         , MT.Zircon                       , MT.OREMATS.Ilmenite   ), // :911
        veinAtum("ore.large.nickel"      , 10,  40,  40, 3, 16, MT.OREMATS.Garnierite           , MT.Ni                           , MT.OREMATS.Cobaltite            , MT.OREMATS.Pentlandite), // :912
        veinAtum("ore.large.redstone"    , 10,  40,  60, 3, 24, MT.Redstone                     , MT.Redstone                     , MT.Ruby                         , MT.OREMATS.Cinnabar   ), // :913
        veinAtum("ore.large.tetrahedrite", 70, 120, 150, 4, 24, MT.OREMATS.Tetrahedrite         , MT.OREMATS.Tetrahedrite         , MT.Cu                           , MT.OREMATS.Stibnite   ), // :914
        veinAtum("ore.large.iron"        , 10,  40, 120, 4, 24, MT.OREMATS.BrownLimonite        , MT.OREMATS.YellowLimonite       , MT.Fe2O3                        , MT.OREMATS.Malachite  ), // :915
        veinAtum("ore.large.copper"      , 10,  30,  80, 4, 24, MT.OREMATS.Chalcopyrite         , MT.Fe2O3                        , MT.Pyrite                       , MT.Cu                 ), // :916
        veinOffworld("ore.large.adamantium", 10, 120,   5, 2, 16, MT.OREMATS.BrownLimonite  , MT.OREMATS.YellowLimonite       , MT.Fe2O3                        , MT.Adamantine         ), // :917
        veinOffworldEnd("ore.large.naquadah"  , 10,  60,  10, 4, 32, MT.Nq                     , MT.Nq                           , MT.Nq                           , MT.Nq                 ), // :918 ORE_END
        veinOffworldEnd("ore.large.trinium"   , 10,  90, 100, 1, 12, MT.Ke                     , MT.Ke                           , MT.Ke                           , MT.Ke                 ), // :919 ORE_END
        veinOffworld("ore.large.dolamide"  ,  5,  60,  40, 3, 16, MT.OREMATS.DuraniumHexaiodide, MT.OREMATS.DuraniumHexafluoride, MT.OREMATS.DuraniumHexachloride, MT.Dolamide), // :920
        veinOffworld("ore.large.moonmars"  , 10,  90, 240, 1,  8, MT.MgCO3                  , MT.MnO2                         , MT.Al2O3                        , MT.TiO2               ), // :921
        veinOffworld("ore.large.cheese"    , 10,  90, 100, 3, 16, MT.Cheese                 , MT.Cheese                       , MT.Cheese                       , MT.Se                 ), // :922
        veinOffworld("ore.large.desh"      , 10,  90, 100, 3, 16, MT.OREMATS.TritaniumHexafluoride, MT.OREMATS.DuraniumHexaastatide, MT.OREMATS.DuraniumHexabromide, MT.Desh), // :923
        veinOffworld("ore.large.syrmorite" , 30,  45, 160, 2, 32, MT.Syrmorite              , MT.Syrmorite                    , MT.Syrmorite                    , MT.Syrmorite          ), // :924
        veinOffworld("ore.large.octine"    , 10,  25,  40, 1, 32, MT.Octine                 , MT.Octine                       , MT.Octine                       , MT.Octine             )  // :925
    );

    /**
     * The ONE 20-row deep-band mirror vein table (task c2-deep-band spec ①) — the
     * selected overworld rows of {@link #LARGE_VEIN_TABLE} with the band translated
     * {@code y - 64} (the {@link GTOreWorldgen#DEEP_SHIFT} rule: the 1.7.10 column [0, 128]
     * rides the bedrock-anchored translation onto modern [-64, +64], so the mirrored band
     * keeps its exact thickness and lands wholly in the modern deepslate band — the modern
     * face of the upstream mNoDeep deep-slate protection layer, WorldgenStoneLayers.java
     * :77/:196 y&lt;24 forces DEEPSLATE). The band-comment on each row cites the twin's
     * upstream line + band.
     *
     * <p>SELECTION (the card rule, each exclusion declared): major METAL + GEM veins whose
     * upstream band sits in the upstream LOWER half ({@code maxY <= 64}). NOT mirrored —
     * fuels (lignite/coal :886/:887; the 1.7.10 semantics keep carbon near the surface),
     * the evaporite/industrial rows (apatite :888, iodinesalt :891, rocksalt :892,
     * asbestos :893), the upper-half bands (bauxite 50-90, quartz 40-80, cassiterite 40-90,
     * tetrahedrite 70-120), the then-dead molybdenum row (:905 — all four slots outside
     * the registration axis at the c2 selection freeze, the twin never drew; the
     * worldgen-edge-ores-b1 axis extension lit the SURFACE row later, the mirror
     * selection stays frozen — no deep twin) and the offworld rows (:917-925). Every
     * mirrored row keeps its twin's weight/density/size/indicator/material slots VERBATIM
     * (only name + band differ — "ore.large.deep.&lt;tail&gt;"), so the deep draw
     * distribution = the surface distribution one band lower; the row-for-row shift
     * correspondence is pinned by GT6LargeVeinTest. The rows that fell to the
     * registration-axis validity gate at the freeze (sapphire/garnet/peridot/monazite/
     * pitchblende/beryllium/titanium slots) stayed mirrored STRUCTURALLY: they ride the
     * same gate as their surface twins and lit up together as the axis extended
     * (r7-a/r7-b, then the B1 card lit the rest).
     */
    public static final List<GTVeinConfig> DEEP_VEIN_TABLE = List.of(
        vein("ore.large.deep.lapis"     , -44, -14,  40, 5, 16, MT.Lazurite                     , MT.Sodalite                     , MT.Lapis                        , MT.Azurite            ), // :889 lapis 20-50
        vein("ore.large.deep.sapphire"  , -54, -24,  30, 3, 16, MT.BlueSapphire                 , MT.OrangeSapphire               , MT.YellowSapphire               , MT.Ruby               ), // :894 sapphire 10-40
        vein("ore.large.deep.sapphire2" , -54, -24,  30, 3, 16, MT.GreenSapphire                , MT.Ruby                         , MT.BlueSapphire                 , MT.PurpleSapphire     ), // :895 sapphire2 10-40
        vein("ore.large.deep.garnet"    , -54, -24,  60, 3, 16, MT.Almandine                    , MT.Pyrope                       , MT.Andradite                    , MT.Uvarovite          ), // :896 garnet 10-40
        vein("ore.large.deep.pitchblende", -54, -24, 40, 3, 16, MT.OREMATS.Pitchblende          , MT.OREMATS.Pitchblende          , MT.OREMATS.Uraninite            , MT.OREMATS.Uraninite  ), // :897 pitchblende 10-40
        vein("ore.large.deep.monazite"  , -54, -24,  30, 3, 16, MT.OREMATS.Bastnasite           , MT.OREMATS.Bastnasite           , MT.Monazite                     , MT.Nd                 ), // :898 monazite 10-40
        vein("ore.large.deep.diamond"   , -59, -44,  40, 2, 16, MT.Graphite                     , MT.Graphite                     , MT.Diamond                      , MT.Graphite           ), // :899 diamond 5-20
        vein("ore.large.deep.galena"    , -34,  -4,  40, 5, 16, MT.OREMATS.Galena               , MT.OREMATS.Galena               , MT.Ag                           , MT.Pb                 ), // :900 galena 30-60
        vein("ore.large.deep.peridot"   , -54, -24,  60, 3, 16, MT.OREMATS.Kyanite              , MT.MgCO3                        , MT.Peridot                      , MT.OREMATS.Glauconite ), // :902 peridot 10-40
        vein("ore.large.deep.gold"      , -44, -34,   5, 3, 16, MT.Pyrite                       , MT.OREMATS.Chalcopyrite         , MT.OREMATS.Arsenopyrite         , MT.Au                 ), // :903 gold 20-30
        vein("ore.large.deep.platinum"  , -24, -14,   5, 3, 16, MT.OREMATS.Cooperite            , MT.Pd                           , MT.OREMATS.Sperrylite           , MT.Ir                 ), // :904 platinum 40-50
        vein("ore.large.deep.tungstate" , -44, -14,  10, 3, 16, MT.OREMATS.Scheelite            , MT.OREMATS.Russellite           , MT.OREMATS.Tungstate            , MT.OREMATS.Pinalite   ), // :907 tungstate 20-50
        vein("ore.large.deep.manganese" , -44, -34,  20, 3, 16, MT.Grossular                    , MT.Spessartine                  , MT.MnO2                         , MT.OREMATS.Coltan     ), // :908 manganese 20-30
        vein("ore.large.deep.beryllium" , -59, -34,  15, 3, 16, MT.Aquamarine                   , MT.Maxixe                       , MT.Emerald                      , MT.Th                 ), // :909 beryllium 5-30
        vein("ore.large.deep.beryllium2", -59, -34,  15, 3, 16, MT.Bixbite                      , MT.Goshenite                    , MT.Heliodor                     , MT.Morganite          ), // :910 beryllium2 5-30
        vein("ore.large.deep.titanium"  , -54, -24,  40, 3, 16, MT.TiO2                         , MT.TiO2                         , MT.Zircon                       , MT.OREMATS.Ilmenite   ), // :911 titanium 10-40
        vein("ore.large.deep.nickel"    , -54, -24,  40, 3, 16, MT.OREMATS.Garnierite           , MT.Ni                           , MT.OREMATS.Cobaltite            , MT.OREMATS.Pentlandite), // :912 nickel 10-40
        vein("ore.large.deep.redstone"  , -54, -24,  60, 3, 24, MT.Redstone                     , MT.Redstone                     , MT.Ruby                         , MT.OREMATS.Cinnabar   ), // :913 redstone 10-40
        vein("ore.large.deep.iron"      , -54, -24, 120, 4, 24, MT.OREMATS.BrownLimonite        , MT.OREMATS.YellowLimonite       , MT.Fe2O3                        , MT.OREMATS.Malachite  ), // :915 iron 10-40
        vein("ore.large.deep.copper"    , -54, -34,  80, 4, 24, MT.OREMATS.Chalcopyrite         , MT.Fe2O3                        , MT.Pyrite                       , MT.Cu                 )  // :916 copper 10-30
    );

    /**
     * The ONE 5-row strata-lens table (task strata-lens) — the card spec's settled
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

    /**
     * The ONE 17-stone nether-lens table (task nether-lens-end-yield) — every stone of
     * the upstream nether loop, GTStoneBlocks.STONES order (Loader_Worldgen.java:656 emits
     * one nether row per loop stone; the overworld prismarine-exclusion note rides the
     * GT6Worldgen javadoc, the 17-stone port universe). UNIFORM columns, the upstream
     * uniform ctor face: rarity = probability 200 ({@link GT6Worldgen#NETHER_LENS_PROBABILITY},
     * the independent per-row 1/200 roll denominator — NOT a weight), Y domain 0..120
     * (MinHeight/MaxHeight; the center draw reads it inclusively — the strata-lens table
     * convention, so the band is [0,120] not the upstream [0,119)), radius/halfHeight =
     * the CLEAN CALIBRATION stand-ins ({@link GT6Worldgen#NETHER_LENS_RADIUS} 40 /
     * {@link GT6Worldgen#NETHER_LENS_HALF_HEIGHT} 12) for the upstream size-200 sausage
     * blob (~25-block axis + ~12.5-block random radius, ~13 vertical) — no P30 calibration
     * curve reused, the radius rides the 48 cap so the Feature's ±3-chunk window stays
     * mathematically closed.
     */
    public static final List<GTLensConfig> NETHER_LENS_TABLE = GTStoneBlocks.STONES.stream()
            .map(GTStoneBlocks.StoneSpec::snake)
            .map(tSnake -> new GTLensConfig(tSnake,
                    GT6Worldgen.NETHER_LENS_PROBABILITY, GT6Worldgen.NETHER_LENS_MIN_Y,
                    GT6Worldgen.NETHER_LENS_MAX_Y, GT6Worldgen.NETHER_LENS_RADIUS,
                    GT6Worldgen.NETHER_LENS_HALF_HEIGHT))
            .toList();

    // ------------------------------------------------------------------
    // The bedrock-ore band (task bedrock-ore-worldgen) — the 46-row table,
    // Loader_Worldgen.java:725-770 row-for-row (the :772 hexorium row rides the MD.HEX
    // mod-gated compat pool, the 53-axis ruling face). Column order
    // (WorldgenOresBedrock.java:61-65): name / probability / material; the "overworld"
    // column = the row listed GEN_FLOOR (:725-757 true; the nether/mars/BL rows :758-770
    // false — the per-chunk independent rolls walk the dimension's own rows only, and the
    // offworld rows stay table-census data until the dim cards hang modifiers). The rows
    // roll INDEPENDENTLY (one WorldgenObject per row upstream, the :142 gate each) — NOT a
    // weighted exactly-one draw. The indicator columns (mIndicatorRocks=T on every row +
    // the flower pairs) are the declared spec deviation — the GTBedrockOreConfig javadoc.
    // Driver-face interlink (mdh-4 closeout): unified mod-driver face = GT6ModDrivers (mdh
    // series) — the MD.HEX row cut is port-time static history; HEX is an mdh-2 atlas
    // (GT6ForeignMaterialAtlas) takeover candidate.
    // ------------------------------------------------------------------

    /**
     * The row helper: an overworld (GEN_FLOOR) row with its indicator flower (task
     * worldgen-flower-arm) — {@code aFlowerSpec} is the GT6SurfaceBlocks.FLOWER_SPECS index
     * (upstream FlowersA meta m = spec m, FlowersB meta m = {@link #specB} m), resolved
     * here so the JSON always carries a REGISTERED flower id (Loader_Worldgen :725-757).
     */
    private static GTBedrockOreConfig bedrock(String aName, int aProbability, OreDictMaterial aMaterial, int aFlowerSpec) {
        return new GTBedrockOreConfig(aName, aMaterial, aProbability, true, false, GT6SurfaceBlocks.FLOWER_SPECS.get(aFlowerSpec).snake());
    }

    /** The FlowersB meta m = FLOWER_SPECS[10+m] (the A group is the identity, no helper). */
    private static int specB(int aMeta) {
        return 10 + aMeta;
    }

    /** The row helper for the offworld census rows (:765-770 mars/BL — no dim card hangs their modifiers yet). */
    private static GTBedrockOreConfig bedrockOffworld(String aName, int aProbability, OreDictMaterial aMaterial) {
        return new GTBedrockOreConfig(aName, aMaterial, aProbability, false, false, "");
    }

    /** The row helper for the GEN_NETHER rows (:758-764, task worldgen-nether-bedrock-lava). */
    private static GTBedrockOreConfig bedrockNether(String aName, int aProbability, OreDictMaterial aMaterial) {
        return new GTBedrockOreConfig(aName, aMaterial, aProbability, false, true, "");
    }

    /** The ONE 46-row bedrock-ore table — the card spec ② "上游行表→Feature 形", plus the
     * indicator-flower columns (task worldgen-flower-arm, Loader_Worldgen.java:725-757:
     * the last ctor pair, A meta direct / B meta via specB). */
    public static final List<GTBedrockOreConfig> BEDROCK_ORE_TABLE = List.of(
        bedrock("ore.bedrock.diamond"     , 128000, MT.Diamond                    , specB(6)), // :725
        bedrock("ore.bedrock.tungstate"   ,  96000, MT.OREMATS.Tungstate          , specB(7)), // :726
        bedrock("ore.bedrock.ferberite"   ,  96000, MT.OREMATS.Ferberite          , specB(7)), // :727
        bedrock("ore.bedrock.wolframite"  ,  96000, MT.OREMATS.Wolframite         , specB(7)), // :728
        bedrock("ore.bedrock.stolzite"    ,  96000, MT.OREMATS.Stolzite           , specB(7)), // :729
        bedrock("ore.bedrock.scheelite"   ,  96000, MT.OREMATS.Scheelite          , specB(7)), // :730
        bedrock("ore.bedrock.huebnerite"  ,  96000, MT.OREMATS.Huebnerite         , specB(7)), // :731
        bedrock("ore.bedrock.russellite"  ,  96000, MT.OREMATS.Russellite         , specB(7)), // :732
        bedrock("ore.bedrock.pinalite"    ,  96000, MT.OREMATS.Pinalite           , specB(7)), // :733
        bedrock("ore.bedrock.uraninite"   ,  60000, MT.OREMATS.Uraninite          ,         5), // :734
        bedrock("ore.bedrock.pitchblende" ,  60000, MT.OREMATS.Pitchblende        , specB(5)), // :735
        bedrock("ore.bedrock.gold.a"      ,  32000, MT.Au                         ,         0), // :736
        bedrock("ore.bedrock.gold.b"      ,  32000, MT.Au                         , specB(2)), // :737
        bedrock("ore.bedrock.cooperite"   ,  16000, MT.OREMATS.Cooperite          ,         6), // :738
        bedrock("ore.bedrock.copper"      ,  16000, MT.Cu                         , specB(3)), // :739
        bedrock("ore.bedrock.monazite"    ,  16000, MT.Monazite                   ,         9), // :740
        bedrock("ore.bedrock.powellite"   ,  14000, MT.OREMATS.Powellite          ,         7), // :741 (Orechid — the TODO Molybdenum face)
        bedrock("ore.bedrock.bastnasite"  ,   8000, MT.OREMATS.Bastnasite         ,         9), // :742
        bedrock("ore.bedrock.stibnite"    ,   8000, MT.OREMATS.Arsenopyrite       , specB(0)), // :743 (the arsenopyrite-material row)
        bedrock("ore.bedrock.redstone"    ,   7000, MT.Redstone                   , specB(4)), // :744
        bedrock("ore.bedrock.vanadium"    ,   6000, MT.V2O5                       ,         7), // :745 (Orechid — the TODO Vanadium face)
        bedrock("ore.bedrock.galena"      ,   6000, MT.OREMATS.Galena             ,         1), // :746
        bedrock("ore.bedrock.coal"        ,   5000, MT.Coal                       ,         7), // :747 (Orechid — the TODO Carbon face)
        bedrock("ore.bedrock.graphite"    ,   5000, MT.Graphite                   ,         7), // :748 (Orechid — the TODO Carbon face)
        bedrock("ore.bedrock.stibnite"    ,   4000, MT.OREMATS.Stibnite           , specB(1)), // :749
        bedrock("ore.bedrock.hematite"    ,   4000, MT.Fe2O3                      ,         7), // :750 (Orechid — the TODO Iron face)
        bedrock("ore.bedrock.sphalerite"  ,   3000, MT.OREMATS.Sphalerite         ,         3), // :751
        bedrock("ore.bedrock.smithsonite" ,   3000, MT.OREMATS.Smithsonite        ,         3), // :752
        bedrock("ore.bedrock.pentlandite" ,   3000, MT.OREMATS.Pentlandite        ,         4), // :753
        bedrock("ore.bedrock.saltpeter"   ,   3000, MT.Niter                      ,         7), // :754 (Orechid — the TODO Niter face)
        bedrock("ore.bedrock.bauxite"     ,   2000, MT.OREMATS.Bauxite            ,         7), // :755 (Orechid — the TODO Aluminium face)
        bedrock("ore.bedrock.cassiterite" ,   2000, MT.OREMATS.Cassiterite        ,         7), // :756 (Orechid — the TODO Tin face)
        bedrock("ore.bedrock.chalcopyrite",   2000, MT.OREMATS.Chalcopyrite       ,         2), // :757
        bedrockNether("ore.bedrock.voidquartz", 4000, MT.VoidQuartz), // :758
        bedrockNether("ore.bedrock.glowstone", 4000, MT.Glowstone), // :759
        bedrockNether("ore.bedrock.gloomstone", 4000, MT.Gloomstone), // :760
        bedrockNether("ore.bedrock.efrine", 2000, MT.Efrine), // :761
        bedrockNether("ore.bedrock.netherquartz", 2000, MT.NetherQuartz), // :762
        bedrockNether("ore.bedrock.firestone", 8000, MT.Firestone), // :763
        bedrockNether("ore.bedrock.ancientdebris", 4000, MT.AncientDebris), // :764
        bedrockOffworld("ore.bedrock.naquadah"     , 10000, MT.Nq             ), // :765
        bedrockOffworld("ore.bedrock.desh"         ,  2000, MT.Desh           ), // :766
        bedrockOffworld("ore.bedrock.dolamide"     ,  5000, MT.Dolamide       ), // :767
        bedrockOffworld("ore.bedrock.adamantine"   , 10000, MT.Adamantine     ), // :768
        bedrockOffworld("ore.bedrock.octine"       ,  5000, MT.Octine         ), // :769
        bedrockOffworld("ore.bedrock.syrmorite"    ,  2000, MT.Syrmorite      )  // :770
    );

    // ------------------------------------------------------------------
    // The bedrock-spring band (task fluid-spring; the springFluid column
    // returned by task issue5-fluid-spring-nozzle) — the 16-row table,
    // Loader_Worldgen.java:782-797 row-for-row. Column order (WorldgenFluidSpring
    // .java:50): name / block / probability / overworld / springFluid; the
    // indicatorType column (:782-788 literals 2/2/2/2/1/3/1) stays the declared
    // spec ③ deferral. springFluid = the upstream mSpringFluid amount (the nozzle
    // arm's 1/amount divisor; null = upstream NF) at the loader values verbatim —
    // the upstream tInfiniteOil/tInfiniteGas=false config gates do not exist in
    // this port, the infinite springs ARE the issue #5 fix. The block ids are
    // single-sourced: the six GT rows over GTFluids.springBlockId (the fluid id +
    // _block; natural_gas its existing p5 block face), the two lava rows the bare
    // minecraft:lava. The rows roll FIRST-HIT-WINS in table order (the
    // WorldgenFluidSpring.java:64 claim blocks every later spring row — at most one
    // spring per chunk).
    // ------------------------------------------------------------------

    /** The row helper: an overworld spring row (the GT fluid id through {@code springBlockId} — the single-source face) + the nozzle amount. */
    private static GTFluidSpringConfig spring(String aName, String aFluidName, int aProbability, int aSpringAmount) {
        return new GTFluidSpringConfig(aName, gregtech6.fluid.GTFluids.springBlockId(aFluidName), aProbability, true, aSpringAmount);
    }

    /**
     * The shelter-dungeon structure (task dungeon-framework, the upstream
     * Loader_Worldgen.java:652 registration row): overworld-only biomes (the
     * {@code #minecraft:is_overworld} tag — the structure JSON biome gate the vanilla
     * findValidGenerationPoint applies at the Y20 stub), no spawn overrides, the
     * {@code underground_structures} step (the vanilla dungeon-family step; the upstream
     * rides the 1.7.10 per-chunk worldgen hook), no terrain adaptation (the pieces build
     * their own shell and the entrance shaft opens its own cap).
     */
    public static void bootstrapStructure(
        //? if forge {
        BootstapContext<Structure> ctx
        //?} else {
        /*BootstrapContext<Structure> ctx
        *///?}
    ) {
        HolderGetter<Biome> tBiomes = ctx.lookup(Registries.BIOME);
        ctx.register(DUNGEON_STRUCTURE, new gregtech6.worldgen.dungeon.GT6DungeonStructure(
                new Structure.StructureSettings(tBiomes.getOrThrow(BiomeTags.IS_OVERWORLD),
                        java.util.Map.of(), GenerationStep.Decoration.UNDERGROUND_STRUCTURES, TerrainAdjustment.NONE)));
    }

    /** The dungeon structure-set — the random_spread face of the upstream 11-chunk grid (the constant javadoc). */
    public static void bootstrapStructureSet(
        //? if forge {
        BootstapContext<StructureSet> ctx
        //?} else {
        /*BootstrapContext<StructureSet> ctx
        *///?}
    ) {
        ctx.register(DUNGEON_STRUCTURE_SET, new StructureSet(
                ctx.lookup(Registries.STRUCTURE).getOrThrow(DUNGEON_STRUCTURE),
                new RandomSpreadStructurePlacement(DUNGEON_SPACING, DUNGEON_SEPARATION, RandomSpreadType.LINEAR, DUNGEON_SALT)));
    }

    /** The row helper for the offworld rows (:789-796 — never drawn overworld, kept for the table census). */
    private static GTFluidSpringConfig springOffworld(String aName, String aBlockId, int aProbability, int aSpringAmount) {
        return new GTFluidSpringConfig(aName, aBlockId, aProbability, false, aSpringAmount);
    }

    /** The twilight-row helper (:795-796 — the task twilight-hives-springs activation, the twilight mask column). */
    private static GTFluidSpringConfig springTwilight(String aName, String aBlockId, int aProbability, int aSpringAmount) {
        return new GTFluidSpringConfig(aName, aBlockId, aProbability, false, true, false, aSpringAmount);
    }

    /**
     * The row helper for the atum oil band (:789-792, task atum-dim-adaptation): the atum
     * mask arm — {@code atum=true}, never overworld (the springOffworld posture plus the
     * atum column; 1/200 · 2000mB, half the OW band's 1/400 · 6000).
     */
    private static GTFluidSpringConfig springAtum(String aName, String aBlockId, int aProbability, int aSpringAmount) {
        return new GTFluidSpringConfig(aName, aBlockId, aProbability, false, false, true, aSpringAmount);
    }

    /** The lava-row helper (:788/:797 — the vanilla block face, no GT fluid id to single-source). */
    private static GTFluidSpringConfig springLava(String aName, int aProbability, boolean aOverworld, boolean aNether, int aSpringAmount) {
        return new GTFluidSpringConfig(aName, "minecraft:lava", aProbability, aOverworld, aNether, aSpringAmount);
    }

    /** The ONE 16-row bedrock-spring table — the card spec ② "上游行表→Feature 形". */
    public static final List<GTFluidSpringConfig> FLUID_SPRING_TABLE = List.of(
        spring       ("overworld.fluid.oil.extraheavy", "liquid_extra_heavy_oil", 400, 6000), // :782
        spring       ("overworld.fluid.oil.heavy"     , "liquid_heavy_oil"     , 400, 6000), // :783
        spring       ("overworld.fluid.oil.medium"    , "liquid_medium_oil"    , 400, 6000), // :784
        spring       ("overworld.fluid.oil.light"     , "liquid_light_oil"     , 400, 6000), // :785
        spring       ("overworld.fluid.gas.natural"   , "natural_gas"          , 200, 3000), // :786
        spring       ("overworld.fluid.water"         , "water_geothermal"     , 100,  500), // :787
        springLava   ("overworld.fluid.lava"          , 200, true , false,     1000), // :788 — the OW lava dome, the vanilla block face
        springAtum   ("atum.fluid.oil.extraheavy"     , "gt6:liquid_extra_heavy_oil_block", 200, 2000), // :789 — the GEN_ATUM band (task atum-dim-adaptation)
        springAtum   ("atum.fluid.oil.heavy"          , "gt6:liquid_heavy_oil_block"     , 200, 2000), // :790
        springAtum   ("atum.fluid.oil.medium"         , "gt6:liquid_medium_oil_block"    , 200, 2000), // :791
        springAtum   ("atum.fluid.oil.light"          , "gt6:liquid_light_oil_block"     , 200, 2000), // :792
        springOffworld("erebus.fluid.gas.natural"     , "gt6:natural_gas_block"          , 200, 1000), // :793
        springOffworld("betweenlands.fluid.gas.natural", "gt6:natural_gas_block"         , 200, 1000), // :794
        springTwilight("twilight.fluid.gas.natural"   , "gt6:natural_gas_block"          , 200, 1000), // :795 — the twilight mask (task twilight-hives-springs)
        springTwilight("twilight.fluid.water"         , "gt6:water_geothermal_block"     , 100,  250), // :796 — the twilight mask (task twilight-hives-springs)
        springLava   ("nether.fluid.lava"             , 100, false, true ,      500)  // :797 — the GEN_NETHER dome (task worldgen-nether-bedrock-lava)
    );

    // ------------------------------------------------------------------
    // The vanilla-water replacement band (task worldgen-water-replace) — the 3-row
    // table, Loader_Worldgen.java:576-578 row-for-row (the row names are the upstream
    // WorldgenObject names verbatim). Column order (GT6WaterReplaceConfig): name /
    // block / gate / scanTop — scanTop rides the 62 default (the upstream "Height"
    // config default, WorldgenOcean.java:48 over WD.waterLevel()=62), so the column
    // is ABSENT from the JSON (optionalFieldOf). The block ids are single-sourced over
    // GTFluids.springBlockId (the fluid id + _block; the task's WATER_REPLACE_BLOCK_IDS
    // block face). The rows REPLACE in table order — the OCEAN→RIVER→SWAMP hard
    // constraint (Loader_Worldgen.java:575-578 source comment) IS this table order.
    // ------------------------------------------------------------------

    /** The row helper: a vanilla-water row (the GT fluid id through {@code springBlockId} — the single-source face). */
    private static GT6WaterReplaceConfig waterRow(String aName, String aFluidName, GT6WaterReplaceConfig.Gate aGate) {
        return new GT6WaterReplaceConfig(aName, gregtech6.fluid.GTFluids.springBlockId(aFluidName), aGate, 62);
    }

    /** The ONE 3-row vanilla-water table — the card spec's "替换范围语义按上游三类 verbatim". */
    public static final List<GT6WaterReplaceConfig> WATER_REPLACE_TABLE = List.of(
        waterRow("ocean.seawater" , "seawater"  , GT6WaterReplaceConfig.Gate.OCEAN), // :576
        waterRow("river.riverwater", "riverwater", GT6WaterReplaceConfig.Gate.RIVER), // :577
        waterRow("swamp.dirtywater", "waterdirty", GT6WaterReplaceConfig.Gate.SWAMP)  // :578
    );

    // ------------------------------------------------------------------
    // REVERTED: the gem-geode band (33-geode, GitHub #33; budding row 3b6878913,
    // IntProvider pins c7b3c44d3) came out here — 4 gem materials x configured+placed+
    // biome-modifier, vanilla Feature.GEODE as pure JSON. User ruling 2026-09-28
    // (decisions.r8-geode-revert): "先r了，后续再看" — reverted for now, revisit later
    // (re-tune the parameters or swap the implementation; the git history above carries
    // the full band). KEPT on purpose: the budding amethyst blocks/items/recipes
    // (unrelated registry face) and the lens companion ores (the C3 system, independent).
    // The vanilla amethyst geode is un-suppressed (VANILLA_DEBLOB_OVERWORLD above).
    // ------------------------------------------------------------------
}
