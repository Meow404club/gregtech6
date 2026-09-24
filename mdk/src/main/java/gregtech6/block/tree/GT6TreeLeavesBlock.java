package gregtech6.block.tree;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.core.BlockPos;

/**
 * The GT6 tree leaves (task p30-w6-t1-trees-nine): one {@link LeavesBlock} per
 * {@link GT6TreeKind} — the vanilla leaves blockstate machine (DISTANCE decay +
 * PERSISTENT) rides untouched. Properties are the vanilla {@code Blocks.leaves(SoundType
 * .GRASS)} row (Blocks.java:7303-7316) with the two private Blocks predicates inlined:
 * {@code ocelotOrParrot} (Blocks.java:7363) and {@code never}.
 *
 * <p>The rainbow face of the Rainbowood leaves (BlockTreeLeavesAB.java:129-139 dynamic
 * RAINBOW_ARRAY tint) is LIVE as of task p38-issue1-4 (GitHub #4): the model carries
 * tintindex 0 over the pixel-true GRAYSCALE PNG and the colour rides the
 * GT6TreeClientListener BlockColor/ItemColor registrations — the former "static
 * rainbow-textured PNG, zero tintindex, pooled" deviation (the GTGrassBlocks
 * pre-coloured-PNG precedent) is superseded there.
 *
 * <p>Loot (sapling/stick/fruit chances, BlockTreeLeavesAB.java:108-127) is the datagen
 * band's face (GT6LootTables addTreeBand); the hazelnut/coconut FRUIT drops defer with the
 * food-item domain (no port item exists to reference — the loot table carries the sapling +
 * stick faces that DO resolve).
 */
public final class GT6TreeLeavesBlock extends LeavesBlock {

    private final GT6TreeKind mKind;

    public GT6TreeLeavesBlock(GT6TreeKind aKind) {
        super(Properties.of().mapColor(MapColor.PLANT).strength(0.2F).randomTicks().sound(SoundType.GRASS)
                .noOcclusion()
                .isValidSpawn(GT6TreeLeavesBlock::ocelotOrParrot)
                .isSuffocating(GT6TreeLeavesBlock::never)
                .isViewBlocking(GT6TreeLeavesBlock::never)
                .ignitedByLava()
                .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY));
        mKind = aKind;
    }

    public GT6TreeKind kind() {
        return mKind;
    }

    /** Blocks.java:7363 ocelotOrParrot inlined (the vanilla leaves spawn face). */
    private static boolean ocelotOrParrot(BlockState aState, BlockGetter aLevel, BlockPos aPos, EntityType<?> aType) {
        return aType == EntityType.OCELOT || aType == EntityType.PARROT;
    }

    /** Blocks.java never (the suffocation/view-blocking face). */
    private static boolean never(BlockState aState, BlockGetter aLevel, BlockPos aPos) {
        return false;
    }
}
