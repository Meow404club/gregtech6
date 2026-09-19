package gregtech6.worldgen;

import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
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
 * The nether crystal Feature (task p31-nether-lens-end-yield spec ①) — the
 * {@code WorldgenNetherCrystals} port (WorldgenNetherCrystals.java:49-70), shape-verbatim:
 * half the chunks skip (:50 {@code nextBoolean()}), a random column starts at the lava
 * sea level, the ray climbs to the cave ceiling (:53), the ceiling must be natural rock
 * (:55), ten blocks of headroom above the lava line (:56), then the ceiling-adjacent air
 * slot becomes one noise-picked sulfide crystal (:58) and 1500 random-walk iterations
 * grow the stalactite by placing every AIR slot with EXACTLY ONE crystal neighbor
 * (:59-69 — the 1-neighbor rule keeps the shape a thin cluster, not a blob).
 *
 * <p>Deviations (declared): the 50% skip and the walk ride the COORDINATE-SEEDED stream
 * ({@link GT6VeinGenerator#veinRandom} — the strata-lens decision-determinism posture;
 * upstream used the shared per-chunk random, whose placement face is inherently per-boot
 * drifted). The ceiling-rock gate reads {@code #minecraft:base_stone_nether} (netherrack/
 * basalt/blackstone) — the modern form of upstream's {@code Material.rock} test that has
 * no cross-leg carrier (1.20.5 removed getMaterial); nether bricks stay rejected, the
 * :55 intent. The "Crystalline Crag" biome arm of :50 has no modern carrier (a mod
 * biome) and rides the dim-coverage defer. The 12-way crystal identity rides the noise
 * port on {@code (aX/2, 360, aZ/2, 12)} verbatim (:51) into the 12
 * {@code gt6:crystal_<sulfide>} blocks (BlockCrystalOres.java:43 meta order).
 *
 * <p>KJS face (card declaration): zero JSON config; the walk parameters are upstream
 * constants, not a config surface.
 */
public class GT6NetherCrystalFeature extends Feature<NoneFeatureConfiguration> {

    /** The nether lava-sea level (WD.waterLevel nether face): the ray start and the headroom datum. */
    public static final int LAVA_SEA_LEVEL = 31;
    /** The walk iteration count (:59, verbatim). */
    public static final int WALK_ITERATIONS = 1500;
    /** The headroom requirement (:56, {@code --aY - 10 < waterLevel} rejects). */
    public static final int HEADROOM = 10;
    /** The 12 sulfide crystal paths, {@link GT6NetherOres#KEYS} order (BlockCrystalOres.java:43 metas 0..11). */
    private static final String[] CRYSTAL_PATHS = {
            "crystal_arsenopyrite", "crystal_chalcopyrite", "crystal_cinnabar", "crystal_cobaltite",
            "crystal_galena", "crystal_kesterite", "crystal_molybdenite", "crystal_pyrite",
            "crystal_sphalerite", "crystal_stannite", "crystal_stibnite", "crystal_tetrahedrite"};

    public GT6NetherCrystalFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> aContext) {
        WorldGenLevel tLevel = aContext.level();
        ChunkPos tWork = tLevel instanceof WorldGenRegion ? ((WorldGenRegion) tLevel).getCenter()
                : new ChunkPos(aContext.origin());
        Level tConcrete = tLevel instanceof Level tLevel2 ? tLevel2 : ((WorldGenRegion) tLevel).getLevel();
        long tSalt = GT6VeinGenerator.dimensionSalt(tLevel);
        Random tRandom = GT6VeinGenerator.veinRandom(tLevel.getSeed(), tSalt, tWork.x, tWork.z);

        if (tRandom.nextBoolean()) return false; // :50 — half the chunks skip
        int aX = tWork.getMinBlockX() + tRandom.nextInt(16), aZ = tWork.getMinBlockZ() + tRandom.nextInt(16);
        // :51 — the 12-way crystal pick rides the noise, x/z halved verbatim
        GT6WorleyNoise tNoise = new GT6WorleyNoise(tLevel.getSeed(), (int) (512L * tSalt));
        int tMeta = tNoise.get(aX / 2, 360, aZ / 2, CRYSTAL_PATHS.length);
        Block tCrystal = GT6NetherOres.block(CRYSTAL_PATHS[tMeta]);
        if (tCrystal == null) return false;

        // :53 — the ceiling ray: climb from the lava line to the first solid block
        int aY = LAVA_SEA_LEVEL;
        int tMaxBuildY = tLevel.getMaxBuildHeight() - 1;
        while (++aY < tMaxBuildY && tLevel.getBlockState(new BlockPos(aX, aY, aZ)).isAir()) {
            // climb
        }
        Block tCeiling = tLevel.getBlockState(new BlockPos(aX, aY, aZ)).getBlock();
        if (tCeiling == Blocks.NETHER_BRICKS || !tCeiling.defaultBlockState().is(BlockTags.BASE_STONE_NETHER)) {
            return false; // :55 — fortress walls and non-rock ceilings reject
        }
        if (--aY - HEADROOM < LAVA_SEA_LEVEL) return false; // :56 — the headroom gate
        tLevel.setBlock(new BlockPos(aX, aY, aZ), tCrystal.defaultBlockState(), 2); // :58 — the anchor

        // :59-69 — the random walk: air slots with exactly one crystal neighbor join the cluster
        for (int i = 0; i < WALK_ITERATIONS; i++) {
            int tX = aX + tRandom.nextInt(8) - tRandom.nextInt(8);
            int tY = aY - tRandom.nextInt(12);
            int tZ = aZ + tRandom.nextInt(8) - tRandom.nextInt(8);
            if (tY < tLevel.getMinBuildHeight() || tY > tMaxBuildY) continue;
            BlockPos tPos = new BlockPos(tX, tY, tZ);
            if (!tLevel.getBlockState(tPos).isAir()) continue;
            if (countCrystalNeighbors(tLevel, tCrystal, tX, tY, tZ) == 1) {
                tLevel.setBlock(tPos, tCrystal.defaultBlockState(), 2);
            }
        }
        return true; // :70 — T once the anchor placed, upstream verbatim
    }

    /** The 6-face crystal-neighbor count (:62-66 ALL_SIDES_VALID). */
    private static int countCrystalNeighbors(WorldGenLevel aLevel, Block aCrystal, int aX, int aY, int aZ) {
        int tCount = 0;
        BlockPos tPos = new BlockPos(aX, aY, aZ);
        for (Direction tSide : Direction.values()) {
            if (aLevel.getBlockState(tPos.relative(tSide)).getBlock() == aCrystal) tCount++;
        }
        return tCount;
    }
}
