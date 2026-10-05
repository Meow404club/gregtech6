package gregtech6.worldgen;

import java.util.Map;
import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import gregapi.oredict.OreDictMaterial;
import gregtech6.block.ore.GTBedrockOreBlock;
import gregtech6.registry.GT6BedrockOreBlocks;
import gregtech6.registry.GT6OreBlocks;

/**
 * The coltan-contention Feature (task worldgen-coltan) — the per-chunk adapter around
 * {@link GT6ColtanGenerator}, the upstream {@code WorldgenColtan} single-stream order
 * (WorldgenColtan.java:53-81): the seed-derived contention center ({@link GT6ColtanGenerator#center}),
 * the center chunk's bedrock vein ({@link GT6BedrockOreGenerator#generateVein} on the
 * {@link GT6Worldgen#COLTAN_DIMENSION_SALT} coordinate-seeded stream, the :57 face) THEN
 * the ring scatter continuing the SAME stream (:59-81). The placement runs on Count(1)
 * +InSquare+BiomeFilter (one attempt per chunk — the GT6BedrockOreFeature chain shape);
 * the world-seed center + the ring gates live in the generator, no placement modifier can
 * express them.
 *
 * <p><b>The sink</b> is one object serving both callbacks — the bedrock vein's
 * {@link GT6BedrockOreGenerator.BedrockSink} and the scatter's
 * {@link GT6ColtanGenerator.ColtanSink} share the {@code ore} signature, so the
 * host-skin semantics are the GT6BedrockOreFeature shape verbatim (the muffin shell is
 * DEEPSLATE, the muffin/tail and scatter ores ride the host stone's own family; unmapped
 * hosts are skipped, upstream same).
 *
 * <p>KJS face (card declaration): the feature carries zero JSON config (NoneFeatureConfiguration
 * — the constants live in {@link GT6ColtanGenerator}); the registry face is out of KJS scope
 * (GT6Features javadoc clause).
 */
public class GT6ColtanFeature extends Feature<NoneFeatureConfiguration> {

    public GT6ColtanFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> aContext) {
        WorldGenLevel tLevel = aContext.level();
        ChunkPos tWork = tLevel instanceof WorldGenRegion ? ((WorldGenRegion) tLevel).getCenter()
                : new ChunkPos(aContext.origin());
        long tSeed = tLevel.getSeed();
        int[] tCenter = GT6ColtanGenerator.center(tSeed); // :54-55
        int tMinX = tWork.getMinBlockX(), tMinZ = tWork.getMinBlockZ();
        Random tRandom = GT6VeinGenerator.veinRandom(tSeed, GT6Worldgen.COLTAN_DIMENSION_SALT, tWork.x, tWork.z);
        ColtanLevelSink tSink = new ColtanLevelSink(tLevel);
        boolean rPlaced = false;
        if (GT6ColtanGenerator.hitsCenter(tCenter[0], tCenter[1], tMinX, tMinZ)) { // :57
            rPlaced |= GT6BedrockOreGenerator.generateVein(GT6ColtanGenerator.centerVein(), tRandom,
                    tMinX, tMinZ, tLevel.getMinBuildHeight(), tSink);
        }
        rPlaced |= GT6ColtanGenerator.scatter(tCenter[0], tCenter[1], tMinX, tMinZ, tRandom, tSink); // :59-81
        return rPlaced;
    }

    /**
     * ponytail: the level sink mirrors GT6BedrockOreFeature.levelSink 1:1 (bedrock faces +
     * host-family ores) — promote to a shared factory on GT6BedrockOreGenerator if a third
     * caller appears; until then a 30-line duplicate is the smaller diff than touching the
     * bedrock card's shipped files.
     */
    private static final class ColtanLevelSink implements GT6BedrockOreGenerator.BedrockSink,
            GT6ColtanGenerator.ColtanSink {

        private final WorldGenLevel mLevel;
        private final Map<Block, GT6OreBlocks.OreFamily> mHosts;

        ColtanLevelSink(WorldGenLevel aLevel) {
            mLevel = aLevel;
            mHosts = GT6OreBlocks.stoneToOreFamilies();
        }

        @Override
        public boolean isBedrockFace(int aX, int aZ) {
            Block tBlock = mLevel.getBlockState(new BlockPos(aX, GT6BedrockOreGenerator.BEDROCK_Y, aZ)).getBlock();
            return tBlock == Blocks.BEDROCK || tBlock instanceof GTBedrockOreBlock; // :185 (the idempotent re-place face)
        }

        @Override
        public void bedrockOre(int aX, int aY, int aZ, OreDictMaterial aMaterial, boolean aSmall) {
            Block tBlock = GT6BedrockOreBlocks.get(aSmall, aMaterial) == null
                    ? null : GT6BedrockOreBlocks.get(aSmall, aMaterial).get();
            if (tBlock != null) mLevel.setBlock(new BlockPos(aX, aY, aZ), tBlock.defaultBlockState(), 2);
        }

        @Override
        public void shell(int aX, int aY, int aZ) {
            mLevel.setBlock(new BlockPos(aX, aY, aZ), Blocks.DEEPSLATE.defaultBlockState(), 2); // :203, the unconditional face
        }

        @Override
        public void ore(int aX, int aY, int aZ, OreDictMaterial aMaterial, boolean aSmall) {
            BlockPos tPos = new BlockPos(aX, aY, aZ);
            GT6OreBlocks.OreFamily tFamily = mHosts.get(mLevel.getBlockState(tPos).getBlock()); // WD.java:750/:771
            if (tFamily == null) return;
            var tHandle = GT6OreBlocks.get(tFamily,
                    aSmall ? GT6OreBlocks.FormKind.SMALL : GT6OreBlocks.FormKind.NORMAL, aMaterial);
            if (tHandle == null) return;
            mLevel.setBlock(tPos, tHandle.get().defaultBlockState(), 2);
        }
    }
}
