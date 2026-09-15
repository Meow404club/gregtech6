package gregtech6.block.tree;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
//? if forge {
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.grower.AbstractTreeGrower;
//?} else {
/*import net.minecraft.world.level.block.grower.TreeGrower;
//21.1: AbstractTreeGrower died with the TreeGrower record rework — the sapling ctor now
//takes a named TreeGrower over the configured-feature key (TreeGrower.java:36-52, the
//(String, Optional mega, Optional tree, Optional flowers) public ctor).
*///?}

import net.minecraft.world.level.material.MapColor;

/**
 * The GT6 tree sapling (task p30-w6-t1-trees-nine): one block per {@link GT6TreeKind},
 * extending the vanilla {@link SaplingBlock} so the grow semantics ride the platform —
 * randomTick light>=9 + 1/7 + the STAGE 0->1 two-tick gate (SaplingBlock.java:39-50) IS
 * the upstream BlockBaseSapling.updateTick2 face (BlockBaseSapling.java:100-104, the
 * TREE_GROWTH_TIME config default 1 = CS.java:882 disables the extra gate), the bonemeal
 * 45% = BlockBaseSapling.java:151 verbatim, and the ground-loss pop = the BushBlock
 * canSurvive face instead of the upstream checkAndDropBlock.
 *
 * <p>The grower hands the kind's CONFIGURED FEATURE key (GT6Worldgen tree band) to the
 * vanilla {@code AbstractTreeGrower}/{@code TreeGrower} plumbing — the canopy math itself
 * lives in the gt6 TreeFeature the key points at (the GTCEu GTBlocks.java:685
 * SaplingBlock+AbstractTreeGrower precedent).
 *
 * <p>The one behavioural override is the coconut sand face ({@link GT6TreeKind#canGrowOnSand},
 * BlockTreeSaplingAB.java:74-76): {@code mayPlaceOn} additionally admits
 * {@code #minecraft:sand}, the modern form of the upstream canSustainPlant(cactus) arm.
 * Declared deviation: the oxygen→deadbush faces (BlockBaseSapling.java:92-107) are the
 * Galacticraft-compat domain and stay unported.
 */
public final class GT6TreeSaplingBlock extends SaplingBlock {

    private final GT6TreeKind mKind;

    public GT6TreeSaplingBlock(GT6TreeKind aKind, ResourceKey<ConfiguredFeature<?, ?>> aFeature) {
        super(
            //? if forge {
            new AbstractTreeGrower() {
                @Override
                protected ResourceKey<ConfiguredFeature<?, ?>> getConfiguredFeature(RandomSource aRandom,
                        boolean aHasFlowers) {
                    return aFeature;
                }
            },
            //?} else {
            /*new TreeGrower("gt6_" + aKind.snake(), java.util.Optional.empty(),
                java.util.Optional.of(aFeature), java.util.Optional.empty()),
            *///?}
            Properties.of().mapColor(MapColor.PLANT).noCollission().instabreak().randomTicks()
                    .sound(net.minecraft.world.level.block.SoundType.GRASS).noOcclusion());
        mKind = aKind;
    }

    public GT6TreeKind kind() {
        return mKind;
    }

    @Override
    protected boolean mayPlaceOn(BlockState aState, BlockGetter aLevel, BlockPos aPos) {
        return super.mayPlaceOn(aState, aLevel, aPos) || (mKind.canGrowOnSand() && aState.is(BlockTags.SAND));
    }
}
