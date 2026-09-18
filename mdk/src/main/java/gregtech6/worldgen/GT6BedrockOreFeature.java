package gregtech6.worldgen;

import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import gregapi.oredict.OreDictMaterial;
import gregtech6.block.ore.GTBedrockOreBlock;
import gregtech6.registry.GT6BedrockOreBlocks;
import gregtech6.registry.GT6OreBlocks;

/**
 * The bedrock-ore Feature (task p31-bedrock-ore-worldgen spec ②) — the per-chunk adapter
 * around {@link GT6BedrockOreGenerator}, the {@link GT6StrataLensFeature} isomorphic shape
 * minus the origin grid: the bedrock ore has NO lattice, every chunk rolls its own rows on
 * its own coordinate-seeded stream ({@link GT6VeinGenerator#veinRandom} — the decision-level
 * determinism the strata card pinned; the placed-feature random is NOT used at all).
 *
 * <p><b>The work chunk</b>: the placed feature runs Count(1)+InSquare+BiomeFilter, one
 * attempt per chunk; the work chunk is the REGION CENTER (WorldGenRegion.getCenter(), the
 * strata form) — the /place command path hands a ServerLevel and falls back to the aimed
 * chunk. All writes stay inside that chunk by construction (the patch/muffin bounds are
 * 0..16, the tail walk breaks at the 1..14 ring — the research card's single-chunk face).
 *
 * <p><b>Placement</b> = the WD.setOre/setSmallOre host-skin semantics (WD.java:744-759/
 * :765-780): the muffin/tail ore blocks are the HOST stone's own family from
 * {@link GT6OreBlocks#stoneToOreFamilies()} — the just-written muffin shell is deepslate so
 * the muffin ore rides the deepslate family, the tails ride whatever stone they cross;
 * unmapped hosts (air, caves, the vanilla replaceables granite/diorite/andesite that map to
 * no GT family) are skipped, upstream same. The bedrock-face patch replaces the floor
 * unconditionally within its 6x6 (the upstream placeBlock force, y=0 -> -64).
 *
 * <p>KJS face (card declaration): the 46-row table is datapack JSON (the configured-feature
 * config); this class is the registry face, out of KJS scope (GT6Features javadoc clause).
 */
public class GT6BedrockOreFeature extends Feature<GTBedrockOreConfig.Table> {

    public GT6BedrockOreFeature() {
        super(GTBedrockOreConfig.Table.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<GTBedrockOreConfig.Table> aContext) {
        WorldGenLevel tLevel = aContext.level();
        ChunkPos tWork = tLevel instanceof WorldGenRegion ? ((WorldGenRegion) tLevel).getCenter()
                : new ChunkPos(aContext.origin());
        Random tRandom = GT6VeinGenerator.veinRandom(tLevel.getSeed(), tWork.x, tWork.z);
        List<GTBedrockOreConfig> tHits = GT6BedrockOreGenerator.drawRows(aContext.config(), tRandom);
        if (tHits.isEmpty()) return false;
        Map<Block, GT6OreBlocks.OreFamily> tHosts = GT6OreBlocks.stoneToOreFamilies();
        GT6BedrockOreGenerator.BedrockSink tSink = levelSink(tLevel, tHosts);
        boolean rPlaced = false;
        for (GTBedrockOreConfig tRow : tHits) {
            rPlaced |= GT6BedrockOreGenerator.generateVein(tRow, tRandom, tWork.getMinBlockX(), tWork.getMinBlockZ(),
                    tLevel.getMinBuildHeight(), tSink);
        }
        return rPlaced;
    }

    private GT6BedrockOreGenerator.BedrockSink levelSink(WorldGenLevel aLevel, Map<Block, GT6OreBlocks.OreFamily> aHosts) {
        return new GT6BedrockOreGenerator.BedrockSink() {
            @Override
            public boolean isBedrockFace(int aX, int aZ) {
                Block tBlock = aLevel.getBlockState(new BlockPos(aX, GT6BedrockOreGenerator.BEDROCK_Y, aZ)).getBlock();
                return tBlock == Blocks.BEDROCK || tBlock instanceof GTBedrockOreBlock; // :185 (the idempotent re-place face)
            }

            @Override
            public void bedrockOre(int aX, int aY, int aZ, OreDictMaterial aMaterial, boolean aSmall) {
                Block tBlock = bedrockBlock(aMaterial, aSmall);
                if (tBlock != null) aLevel.setBlock(new BlockPos(aX, aY, aZ), tBlock.defaultBlockState(), 2);
            }

            @Override
            public void shell(int aX, int aY, int aZ) {
                aLevel.setBlock(new BlockPos(aX, aY, aZ), Blocks.DEEPSLATE.defaultBlockState(), 2); // :203, the unconditional face
            }

            @Override
            public void ore(int aX, int aY, int aZ, OreDictMaterial aMaterial, boolean aSmall) {
                BlockPos tPos = new BlockPos(aX, aY, aZ);
                GT6OreBlocks.OreFamily tFamily = aHosts.get(aLevel.getBlockState(tPos).getBlock()); // WD.java:750/:771
                if (tFamily == null) return;
                var tHandle = GT6OreBlocks.get(tFamily,
                        aSmall ? GT6OreBlocks.FormKind.SMALL : GT6OreBlocks.FormKind.NORMAL, aMaterial);
                if (tHandle == null) return;
                aLevel.setBlock(tPos, tHandle.get().defaultBlockState(), 2);
            }
        };
    }

    /** The registered bedrock block of a (material, form) pair, or null (the invalid-row posture). */
    private static Block bedrockBlock(OreDictMaterial aMaterial, boolean aSmall) {
        return GT6BedrockOreBlocks.get(aSmall, aMaterial) == null
                ? null : GT6BedrockOreBlocks.get(aSmall, aMaterial).get();
    }
}
