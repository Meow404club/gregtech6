package gregtech6.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import gregtech6.block.tree.GT6TreeKind;
import gregtech6.registry.GT6TreeBlocks;

/**
 * The GT6 tree grow semantics as a worldgen Feature (task p30-w6-t1-trees-nine) — the
 * <b>two-option ruling, option b</b>: the upstream canopy math is a per-case sequence of
 * irregular layer loops (diamond thresholds {@code Math.abs(i*j)}, non-convex palm arms,
 * a quadratic spruce cone, maple droop rings down to {@code tMaxHeight-7}) that the
 * vanilla TrunkPlacer/FoliagePlacer crown model cannot express (TreeConfiguration's
 * foliage placer places one crown shape, not nine bespoke per-layer rings) — so each
 * {@link GT6TreeKind} carries its grow() case VERBATIM (BlockTreeSaplingAB.java:83-315
 * cases 0-7 + BlockTreeSaplingCD.java:72-102 case 0) into one Feature instance per kind,
 * configured over {@link NoneFeatureConfiguration} (zero new codec surface).
 *
 * <p>One instance per kind (GT6Features.TREE_FEATURES) — the configured-feature JSONs
 * point at the kind's instance, so the sapling grower and the placed feature run the
 * SAME code (the upstream identity: WorldgenTreeRubber.java:58 calls the sapling's grow).
 *
 * <p>Transcription faces:
 * <ul>
 * <li>{@link #canPlaceTree} = BlockBaseSapling.java:128-131 (self/tallgrass/snow/leaves/
 *     canBeReplacedByLeaves) over the modern superset air || canBeReplaced() ||
 *     REPLACEABLE_BY_TREES || LEAVES (the vanilla TreeFeature.validTreePos face,
 *     TreeFeature.java:51-52, widened by the LEAVES tag the upstream leaf-instance arm
 *     needs — GT canopies overlap neighbour trees' leaves).</li>
 * <li>{@link #placeTree} = BlockBaseSapling.java:124-126 (canPlaceTree gate + set, flag 3).</li>
 * <li>{@link #placeLeaf} sets the vanilla DISTANCE explicitly (the
 *     LeavesBlock.getDistanceAt face, LeavesBlock.java:94-102) — GT leaves placed with
 *     flag 3 only get neighbour-update recalcs, so the first write carries the computed
 *     distance (PERSISTENT=false keeps the vanilla decay semantics; a healthy canopy
 *     never exceeds distance 6, so nothing decays).</li>
 * <li>{@link #maxHeight} = BlockBaseSapling.java:117-122.</li>
 * </ul>
 *
 * <p>DECLARED DEVIATION (card spec ④): the rubber resin-hole arm is CUT — the
 * BlockTreeSaplingAB.java:94-101 branch that places MTE 32762 into the trunk is skipped
 * and the trunk carries a plain log at every level; the resin-hole MTE domain is a later
 * card. The blue spruce grass->coarse-dirt skirt (BlockTreeSaplingCD.java:94-99) IS kept.
 */
public final class GT6TreeFeature extends Feature<NoneFeatureConfiguration> {

    private final GT6TreeKind mKind;

    public GT6TreeFeature(GT6TreeKind aKind) {
        super(NoneFeatureConfiguration.CODEC);
        mKind = aKind;
    }

    public GT6TreeKind kind() {
        return mKind;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> aCtx) {
        WorldGenLevel tLevel = aCtx.level();
        BlockPos tPos = aCtx.origin();
        RandomSource tRandom = aCtx.random();
        int tX = tPos.getX(), tY = tPos.getY(), tZ = tPos.getZ();
        int tMaxHeight;
        switch (mKind) {
            case RUBBER: {
                tMaxHeight = maxHeight(tLevel, tPos, 9);
                if (tMaxHeight < 7) return false;
                tMaxHeight = tY + 7 + tRandom.nextInt(tMaxHeight - 6);
                for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) if (i != 0 || j != 0)
                    if (!canPlaceTree(tLevel, tPos.offset(i, tMaxHeight - tY - 5, j))) return false;
                // spec ④: the upstream :94-101 resin-hole branch is cut — plain trunk
                placeTree(tLevel, tPos, logState());
                for (int tY2 = tY + 1; tY2 < tMaxHeight; tY2++) placeTree(tLevel, new BlockPos(tX, tY2, tZ), logState());
                placeLeaf(tLevel, new BlockPos(tX, tMaxHeight, tZ));
                placeLeaf(tLevel, new BlockPos(tX, tMaxHeight + 1, tZ));
                for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) if (i != 0 || j != 0) {
                    placeLeaf(tLevel, tPos.offset(i, tMaxHeight - tY - 1, j));
                }
                for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) if (i != 0 || j != 0) {
                    if (Math.abs(i * j) < 2) placeLeaf(tLevel, tPos.offset(i, tMaxHeight - tY - 2, j));
                    if (Math.abs(i * j) < 4) {
                        placeLeaf(tLevel, tPos.offset(i, tMaxHeight - tY - 3, j));
                        placeLeaf(tLevel, tPos.offset(i, tMaxHeight - tY - 4, j));
                    }
                    placeLeaf(tLevel, tPos.offset(i, tMaxHeight - tY - 5, j));
                }
                return true;
            }
            case MAPLE: {
                tMaxHeight = maxHeight(tLevel, tPos, 11);
                if (tMaxHeight < 9) return false;
                tMaxHeight = tY + 9 + tRandom.nextInt(tMaxHeight - 8);
                for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) if (i != 0 || j != 0)
                    if (!canPlaceTree(tLevel, tPos.offset(i, tMaxHeight - tY - 4, j))) return false;
                placeTree(tLevel, tPos, logState());
                for (int tY2 = tY + 1; tY2 < tMaxHeight; tY2++) placeTree(tLevel, new BlockPos(tX, tY2, tZ), logState());
                for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) {
                    placeLeaf(tLevel, tPos.offset(i, tMaxHeight - tY + 1, j));
                }
                for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) {
                    placeLeaf(tLevel, tPos.offset(i, tMaxHeight - tY, j));
                    if (i != 0 || j != 0) {
                        if (Math.abs(i * j) < 4) placeLeaf(tLevel, tPos.offset(i, tMaxHeight - tY - 7, j));
                        placeLeaf(tLevel, tPos.offset(i, tMaxHeight - tY - 1, j));
                    }
                }
                for (int i = -3; i <= 3; i++) for (int j = -3; j <= 3; j++) if (i != 0 || j != 0) {
                    if (Math.abs(i * j) < 9) {
                        placeLeaf(tLevel, tPos.offset(i, tMaxHeight - tY - 2, j));
                        placeLeaf(tLevel, tPos.offset(i, tMaxHeight - tY - 3, j));
                        placeLeaf(tLevel, tPos.offset(i, tMaxHeight - tY - 6, j));
                    }
                    placeLeaf(tLevel, tPos.offset(i, tMaxHeight - tY - 4, j));
                    placeLeaf(tLevel, tPos.offset(i, tMaxHeight - tY - 5, j));
                }
                return true;
            }
            case WILLOW: {
                tMaxHeight = maxHeight(tLevel, tPos, 7);
                if (tMaxHeight < 5) return false;
                tMaxHeight = tY + 5 + tRandom.nextInt(tMaxHeight - 4);
                for (int i = -3; i <= 3; i++) for (int j = -3; j <= 3; j++) if (i != 0 || j != 0)
                    if (!canPlaceTree(tLevel, tPos.offset(i, tMaxHeight - tY - 2, j))) return false;
                placeTree(tLevel, tPos, logState());
                for (int tY2 = tY + 1; tY2 < tMaxHeight; tY2++) placeTree(tLevel, new BlockPos(tX, tY2, tZ), logState());
                for (int i = -3; i <= 3; i++) for (int j = -3; j <= 3; j++) {
                    if (Math.abs(i * j) < 9) {
                        placeLeaf(tLevel, tPos.offset(i, tMaxHeight - tY + 1, j));
                        if (i != 0 || j != 0) placeLeaf(tLevel, tPos.offset(i, tMaxHeight - tY - 2, j));
                    }
                    placeLeaf(tLevel, tPos.offset(i, tMaxHeight - tY, j));
                }
                for (int i = -4; i <= 4; i++) for (int j = -4; j <= 4; j++) if (i != 0 || j != 0) {
                    if (Math.abs(i * j) < 10) {
                        placeLeaf(tLevel, tPos.offset(i, tMaxHeight - tY - 1, j));
                        if (tMaxHeight - 2 <= tY) continue;
                        if (Math.abs(i * j) > 6) {
                            placeLeaf(tLevel, tPos.offset(i, tMaxHeight - tY - 2, j));
                            if (tMaxHeight - 3 <= tY) continue;
                            placeLeaf(tLevel, tPos.offset(i, tMaxHeight - tY - 3, j));
                            if (tMaxHeight - 4 <= tY) continue;
                            placeLeaf(tLevel, tPos.offset(i, tMaxHeight - tY - 4, j));
                            if (tMaxHeight - 5 <= tY) continue;
                            placeLeaf(tLevel, tPos.offset(i, tMaxHeight - tY - 5, j));
                        }
                    }
                }
                return true;
            }
            case BLUE_MAHOE: {
                tMaxHeight = maxHeight(tLevel, tPos, 5);
                if (tMaxHeight < 4) return false;
                tMaxHeight = tY + 4 + tRandom.nextInt(tMaxHeight - 3);
                for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) if (i != 0 || j != 0)
                    if (!canPlaceTree(tLevel, tPos.offset(i, tMaxHeight - tY - 2, j))) return false;
                placeTree(tLevel, tPos, logState());
                for (int tY2 = tY + 1; tY2 < tMaxHeight; tY2++) placeTree(tLevel, new BlockPos(tX, tY2, tZ), logState());
                for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) {
                    placeLeaf(tLevel, tPos.offset(i, tMaxHeight - tY + 3, j));
                }
                for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) if (Math.abs(i * j) < 4) {
                    placeLeaf(tLevel, tPos.offset(i, tMaxHeight - tY + 2, j));
                    placeLeaf(tLevel, tPos.offset(i, tMaxHeight - tY + 1, j));
                    if (i != 0 || j != 0) {
                        placeLeaf(tLevel, tPos.offset(i, tMaxHeight - tY, j));
                        placeLeaf(tLevel, tPos.offset(i, tMaxHeight - tY - 1, j));
                    }
                }
                return true;
            }
            case HAZEL: {
                if (maxHeight(tLevel, tPos, 4) < 4) return false;
                for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) if (i != 0 || j != 0)
                    if (!canPlaceTree(tLevel, tPos.offset(i, 2, j))) return false;
                placeTree(tLevel, tPos, logState());
                placeTree(tLevel, tPos.above(1), logState());
                placeTree(tLevel, tPos.above(2), logState());
                for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) {
                    placeLeaf(tLevel, tPos.offset(i, 4, j));
                }
                for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) {
                    if (i != 0 || j != 0) placeLeaf(tLevel, tPos.offset(i, 2, j));
                    if (Math.abs(i * j) < 4) placeLeaf(tLevel, tPos.offset(i, 3, j));
                }
                for (int i = -3; i <= 3; i++) for (int j = -3; j <= 3; j++) {
                    if (Math.abs(i * j) < 9 && (i != 0 || j != 0)) placeLeaf(tLevel, tPos.offset(i, 1, j));
                }
                return true;
            }
            case CINNAMON: {
                return placeCinnamonShape(tLevel, tPos, tRandom, 8, 6);
            }
            case COCONUT: {
                tMaxHeight = maxHeight(tLevel, tPos, 12);
                if (tMaxHeight < 8) return false;
                tMaxHeight = tY + 8 + tRandom.nextInt(tMaxHeight - 7);
                for (int i = -3; i <= 3; i++) for (int j = -3; j <= 3; j++) if (i != 0 || j != 0)
                    if (!canPlaceTree(tLevel, tPos.offset(i, tMaxHeight - tY, j))) return false;
                placeTree(tLevel, tPos, logState());
                for (int tY2 = tY + 1; tY2 < tMaxHeight; tY2++) placeTree(tLevel, new BlockPos(tX, tY2, tZ), logState());
                for (int i = -3; i <= 3; i++) for (int j = -3; j <= 3; j++) if (i == j || i == -j) {
                    if (Math.abs(i) == 3 || Math.abs(j) == 3) {
                        placeLeaf(tLevel, tPos.offset(i, tMaxHeight - tY - 1, j));
                        placeLeaf(tLevel, tPos.offset(i, tMaxHeight - tY - 2, j));
                    } else if (Math.abs(i) == 2 || Math.abs(j) == 2) {
                        placeLeaf(tLevel, tPos.offset(i, tMaxHeight - tY, j));
                        placeLeaf(tLevel, tPos.offset(i, tMaxHeight - tY - 1, j));
                    } else {
                        placeLeaf(tLevel, tPos.offset(i, tMaxHeight - tY, j));
                    }
                }
                for (int i = -4; i <= 4; i++) for (int j = -4; j <= 4; j++) if (i == 0 || j == 0) {
                    if (Math.abs(i) == 4 || Math.abs(j) == 4) {
                        placeLeaf(tLevel, tPos.offset(i, tMaxHeight - tY - 1, j));
                        placeLeaf(tLevel, tPos.offset(i, tMaxHeight - tY - 2, j));
                    } else if (Math.abs(i) == 3 || Math.abs(j) == 3) {
                        placeLeaf(tLevel, tPos.offset(i, tMaxHeight - tY, j));
                        placeLeaf(tLevel, tPos.offset(i, tMaxHeight - tY - 1, j));
                    } else {
                        placeLeaf(tLevel, tPos.offset(i, tMaxHeight - tY, j));
                    }
                }
                return true;
            }
            case RAINBOWOOD: {
                // the CINNAMON canopy shape over rainbow leaves (BlockTreeSaplingAB.java:288-315)
                return placeCinnamonShape(tLevel, tPos, tRandom, 9, 7);
            }
            case BLUE_SPRUCE: {
                tMaxHeight = maxHeight(tLevel, tPos, 16);
                if (tMaxHeight < 16) return false;
                tMaxHeight = tY + tMaxHeight - tRandom.nextInt(3);
                for (int i = -3; i <= 3; i++) for (int j = -3; j <= 3; j++) if (i != 0 || j != 0)
                    if (!canPlaceTree(tLevel, tPos.offset(i, tMaxHeight - tY - 5, j))) return false;
                placeTree(tLevel, tPos, logState());
                for (int tY2 = tY + 1; tY2 < tMaxHeight; tY2++) placeTree(tLevel, new BlockPos(tX, tY2, tZ), logState());
                placeLeaf(tLevel, new BlockPos(tX, tMaxHeight, tZ));
                placeLeaf(tLevel, new BlockPos(tX, tMaxHeight + 1, tZ));
                placeLeaf(tLevel, tPos.offset(1, tMaxHeight - tY - 1, 0));
                placeLeaf(tLevel, tPos.offset(-1, tMaxHeight - tY - 1, 0));
                placeLeaf(tLevel, tPos.offset(0, tMaxHeight - tY - 1, 1));
                placeLeaf(tLevel, tPos.offset(0, tMaxHeight - tY - 1, -1));
                for (int i = -6; i <= 6; i++) for (int j = -6; j <= 6; j++) if (i != 0 || j != 0) {
                    for (int k = 1; k <= 14; k++) if (i * i + j * j < k * k * 0.2) {
                        placeLeaf(tLevel, tPos.offset(i, tMaxHeight - tY + 1 - k, j));
                    }
                    // the coarse-dirt skirt (BlockTreeSaplingCD.java:94-99)
                    if (i * i + j * j <= 30) for (int k = 0; k <= 3; k++) {
                        BlockPos tBelow = new BlockPos(tX + i, tY - k, tZ + j);
                        BlockState tState = tLevel.getBlockState(tBelow);
                        if (tState.isAir() || tState.canBeReplaced()) continue;
                        if (tState.is(Blocks.DIRT) || tState.is(Blocks.GRASS_BLOCK)) {
                            tLevel.setBlock(tBelow, Blocks.COARSE_DIRT.defaultBlockState(), 3);
                        }
                        break;
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * The CINNAMON shape shared by cases 5 and 7 (BlockTreeSaplingAB.java:228-254 vs
     * :288-315 — byte-identical loops, only the max-height band and the leaves shift).
     */
    private boolean placeCinnamonShape(WorldGenLevel aLevel, BlockPos aPos, RandomSource aRandom,
            int aMaxBand, int aMinBand) {
        int tMaxHeight = maxHeight(aLevel, aPos, aMaxBand);
        if (tMaxHeight < aMinBand) return false;
        tMaxHeight = aPos.getY() + aMinBand + aRandom.nextInt(tMaxHeight - aMinBand + 1);
        for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) if (i != 0 || j != 0)
            if (!canPlaceTree(aLevel, aPos.offset(i, tMaxHeight - aPos.getY() - 4, j))) return false;
        placeTree(aLevel, aPos, logState());
        for (int tY2 = aPos.getY() + 1; tY2 < tMaxHeight; tY2++) placeTree(aLevel, new BlockPos(aPos.getX(), tY2, aPos.getZ()), logState());
        for (int i = -2; i <= 2; i++) for (int j = -2; j <= 2; j++) {
            placeLeaf(aLevel, aPos.offset(i, tMaxHeight - aPos.getY() + 2, j));
            if (i != 0 || j != 0) {
                placeLeaf(aLevel, aPos.offset(i, tMaxHeight - aPos.getY() - 4, j));
            }
        }
        for (int i = -3; i <= 3; i++) for (int j = -3; j <= 3; j++) if (Math.abs(i * j) < 9) {
            if (i != 0 || j != 0) {
                placeLeaf(aLevel, aPos.offset(i, tMaxHeight - aPos.getY() - 1, j));
                placeLeaf(aLevel, aPos.offset(i, tMaxHeight - aPos.getY() - 2, j));
                placeLeaf(aLevel, aPos.offset(i, tMaxHeight - aPos.getY() - 3, j));
            }
            placeLeaf(aLevel, aPos.offset(i, tMaxHeight - aPos.getY(), j));
            placeLeaf(aLevel, aPos.offset(i, tMaxHeight - aPos.getY() + 1, j));
        }
        return true;
    }

    // ------------------------------------------------------------------ the BlockBaseSapling faces

    /** BlockBaseSapling.java:117-122 — the clear-column scan (upstream getHeight() face). */
    private static int maxHeight(WorldGenLevel aLevel, BlockPos aPos, int aMaxTreeHeight) {
        aMaxTreeHeight--;
        int rMaxHeight = 0;
        while (rMaxHeight++ < aMaxTreeHeight) {
            if (aPos.getY() + rMaxHeight >= aLevel.getHeight() || !canPlaceTree(aLevel, aPos.above(rMaxHeight)))
                return rMaxHeight - 1;
        }
        return rMaxHeight;
    }

    /**
     * BlockBaseSapling.java:128-131 over the modern superset (the javadoc face): air /
     * replaceable / REPLACEABLE_BY_TREES (TreeFeature.java:51-52) / LEAVES (the upstream
     * leaf-instance arm — GT canopies overlap neighbouring trees).
     */
    private static boolean canPlaceTree(WorldGenLevel aLevel, BlockPos aPos) {
        BlockState tState = aLevel.getBlockState(aPos);
        return tState.isAir() || tState.canBeReplaced() || tState.is(BlockTags.REPLACEABLE_BY_TREES)
                || tState.is(BlockTags.LEAVES);
    }

    /** BlockBaseSapling.java:124-126 — the gated set (flag 3, the upstream WD.set face). */
    private boolean placeTree(WorldGenLevel aLevel, BlockPos aPos, BlockState aState) {
        return canPlaceTree(aLevel, aPos) && aLevel.setBlock(aPos, aState, 3);
    }

    /**
     * The canopy write: the LEAVES state with the vanilla distance computed at first write
     * (the LeavesBlock.getDistanceAt face, LeavesBlock.java:94-102 — logs count 0, leaves
     * count their distance, everything else 7; the +1 minimum clamps at 1..7).
     */
    private boolean placeLeaf(WorldGenLevel aLevel, BlockPos aPos) {
        if (!canPlaceTree(aLevel, aPos)) return false;
        int tDistance = 7;
        for (Direction tDir : Direction.values()) {
            BlockState tNeighbor = aLevel.getBlockState(aPos.relative(tDir));
            int tCandidate;
            if (tNeighbor.is(BlockTags.LOGS)) tCandidate = 0;
            else if (tNeighbor.getBlock() instanceof LeavesBlock) tCandidate = tNeighbor.getValue(LeavesBlock.DISTANCE);
            else tCandidate = 6;
            tDistance = Math.min(tDistance, tCandidate + 1);
        }
        if (tDistance < 1) tDistance = 1;
        if (tDistance > 7) tDistance = 7;
        BlockState tState = GT6TreeBlocks.LEAVES.get(mKind.ordinal()).get().defaultBlockState()
                .setValue(LeavesBlock.DISTANCE, tDistance)
                .setValue(LeavesBlock.PERSISTENT, Boolean.FALSE);
        return aLevel.setBlock(aPos, tState, 3);
    }

    /** The kind's log blockstate (LOGS list is KINDS-ordered). */
    private BlockState logState() {
        return GT6TreeBlocks.LOGS.get(mKind.ordinal()).get().defaultBlockState();
    }
}
