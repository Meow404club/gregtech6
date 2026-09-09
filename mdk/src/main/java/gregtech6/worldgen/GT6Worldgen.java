package gregtech6.worldgen;

import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

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

    private GT6Worldgen() {
    }
}
