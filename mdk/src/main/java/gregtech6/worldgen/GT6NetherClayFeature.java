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
 * The nether red-clay Feature (task p31-nether-lens-end-yield spec ①) — the
 * {@code WorldgenNetherClay} port (WorldgenNetherClay.java:47-55), shape-verbatim: every
 * 16x16 column whose noise cell reads 0 ({@code noise.get(x, 42, z, 8) == 0}, the 1/8
 * column gate :52) gets the two-layer band at the lava line —
 * {@code tUpperBound = waterLevel+3 = 34, tLowerBound = waterLevel+2 = 33}, laid top-down
 * over netherrack only (:53). The payload is the registered {@code gt6:nether_red_clay}
 * (upstream BlocksGT.Diggables meta 3 — GT6NetherOres javadoc).
 *
 * <p>The noise is the {@link GT6WorleyNoise} port (the {@code (int) seed} +
 * {@code 512 * dimensionId} offset), so the placement is a PURE coordinate function —
 * decision-level deterministic by construction
 * (decisions.2026-09-18-p31-strata-lens-determinism-acceptance).
 *
 * <p>KJS face (card declaration): zero JSON config; the parameters are upstream
 * constants, not a config surface.
 */
public class GT6NetherClayFeature extends Feature<NoneFeatureConfiguration> {

    /** The nether lava-sea level (WD.waterLevel nether face; the band rides +2/+3 = y33/34). */
    public static final int LAVA_SEA_LEVEL = 31;
    /** The column-gate cell count (:52, {@code get(x, 42, z, 8) == 0}). */
    public static final int GATE_CELLS = 8;
    /** The noise Y probe (:52, verbatim 42). */
    public static final int GATE_PROBE_Y = 42;

    public GT6NetherClayFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> aContext) {
        WorldGenLevel tLevel = aContext.level();
        ChunkPos tWork = tLevel instanceof WorldGenRegion ? ((WorldGenRegion) tLevel).getCenter()
                : new ChunkPos(aContext.origin());
        Level tConcrete = tLevel instanceof Level tLevel2 ? tLevel2 : ((WorldGenRegion) tLevel).getLevel();
        GT6WorleyNoise tNoise = new GT6WorleyNoise(tLevel.getSeed(),
                (int) (512L * GT6VeinGenerator.dimensionSalt(tLevel)));
        Block tClay = GT6NetherOres.block("nether_red_clay");
        if (tClay == null) return false;
        int tUpperBound = LAVA_SEA_LEVEL + 3, tLowerBound = LAVA_SEA_LEVEL + 2; // :49 — y34/y33
        int tMinBuildY = tLevel.getMinBuildHeight(), tMaxBuildY = tLevel.getMaxBuildHeight() - 1;
        boolean rPlaced = false;
        for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
            int tX = tWork.getMinBlockX() + i, tZ = tWork.getMinBlockZ() + j;
            if (tNoise.get(tX, GATE_PROBE_Y, tZ, GATE_CELLS) != 0) continue; // :52 — the 1/8 column gate
            for (int tY = tUpperBound; tY >= tLowerBound; tY--) { // :52 — top-down two-layer band
                if (tY < tMinBuildY || tY > tMaxBuildY) continue; // the mod-dimension /place guard
                BlockPos tPos = new BlockPos(tX, tY, tZ);
                if (tLevel.getBlockState(tPos).getBlock() == Blocks.NETHERRACK) { // :53 — the host face
                    tLevel.setBlock(tPos, tClay.defaultBlockState(), 2);
                    rPlaced = true;
                }
            }
        }
        return rPlaced;
    }
}
