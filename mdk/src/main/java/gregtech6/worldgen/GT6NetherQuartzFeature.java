package gregtech6.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import gregtech6.registry.GT6NetherOres;

/**
 * The nether quartz Feature (task p31-nether-lens-end-yield spec ①) — the
 * {@code WorldgenNetherQuartz} port (WorldgenNetherQuartz.java:47-56), shape-verbatim:
 * EVERY chunk, EVERY 16x16 column, TWO noise slices —
 * {@code tY = 40 + noise.get(x, 0, z, 200)} and {@code tY = 40 + noise.get(x, 64, z, 200)},
 * the block replaced only where netherrack (:52/:54). The spec's modern clamp rides the
 * slice bound: the noise draw reads {@code 36 + noise.get(x, offset, z, 85)} = the domain
 * clamped to [36, 120] (the card spec's 噪声域钳 36..120 — the 1.7.10 face drew
 * [40, 239] and relied on the 128-tall world silently eating the out-of-range draws; the
 * clamp keeps the same two-slice per-column shape inside the buildable nether).
 *
 * <p><b>The payload</b> is the registered {@code gt6:dense_nether_quartz_ore} (upstream
 * RockOres meta 8, the oreDense NetherQuartz — GT6NetherOres javadoc). The noise is the
 * {@link GT6WorleyNoise} port on the {@code (int) seed} + {@code 512 * dimensionId}
 * offset (NoiseGenerator.java:31-34), so the placement is a PURE coordinate function —
 * decision-level deterministic by construction, the strongest determinism class
 * (decisions.2026-09-18-p31-strata-lens-determinism-acceptance).
 *
 * <p>KJS face (card declaration): the feature carries zero JSON config; the noise
 * parameters are upstream constants, not a config surface.
 */
public class GT6NetherQuartzFeature extends Feature<NoneFeatureConfiguration> {

    /** The clamped slice lower bound (the card spec's 36). */
    public static final int MIN_Y = 36;
    /** The clamped slice upper bound (the card spec's 120). */
    public static final int MAX_Y = 120;
    /** The slice-count span of the clamped draw: nextInt bound = MAX-MIN+1 = 85. */
    public static final int SPAN = MAX_Y - MIN_Y + 1;

    public GT6NetherQuartzFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> aContext) {
        WorldGenLevel tLevel = aContext.level();
        ChunkPos tWork = tLevel instanceof WorldGenRegion ? ((WorldGenRegion) tLevel).getCenter()
                : new ChunkPos(aContext.origin());
        Level tConcrete = tLevel instanceof Level tLevel2 ? tLevel2 : ((WorldGenRegion) tLevel).getLevel();
        GT6WorleyNoise tNoise = new GT6WorleyNoise(tLevel.getSeed(), (int) (512L * GT6NetherLensFeature.dimensionSalt(tLevel)));
        Block tQuartz = GT6NetherOres.block("dense_nether_quartz_ore");
        if (tQuartz == null) return false;
        int tMinBuildY = tLevel.getMinBuildHeight(), tMaxBuildY = tLevel.getMaxBuildHeight() - 1;
        boolean rPlaced = false;
        for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
            int tX = tWork.getMinBlockX() + i, tZ = tWork.getMinBlockZ() + j;
            // the two slices, the offset pair (0, 64) verbatim (WorldgenNetherQuartz.java:51/:53)
            for (int tOffset : new int[] {0, 64}) {
                int tY = MIN_Y + tNoise.get(tX, tOffset, tZ, SPAN);
                if (tY < tMinBuildY || tY > tMaxBuildY) continue; // the mod-dimension /place guard
                BlockPos tPos = new BlockPos(tX, tY, tZ);
                if (tLevel.getBlockState(tPos).getBlock() == Blocks.NETHERRACK) { // :52 — the host face
                    tLevel.setBlock(tPos, tQuartz.defaultBlockState(), 2);
                    rPlaced = true;
                }
            }
        }
        return rPlaced;
    }
}
