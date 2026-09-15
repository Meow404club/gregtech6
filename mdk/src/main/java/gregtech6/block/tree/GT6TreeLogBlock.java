package gregtech6.block.tree;

import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.material.MapColor;

/**
 * The GT6 tree log (task p30-w6-t1-trees-nine): one {@link RotatedPillarBlock} per
 * {@link GT6TreeKind} — the AXIS pillar face the vanilla log idiom rides (the
 * RotatedPillarBlock.java:14-16 default AXIS=Y the worldgen placement expects). Properties
 * are the GTCEu RubberLogBlock row verbatim ({@code strength(2.0F).sound(WOOD)},
 * GTBlocks.java:703), mapColor WOOD.
 *
 * <p>Function tags (#minecraft:logs block+item, mineable/axe, flammable family) are the
 * datagen band's face (GT6BlockTags/GT6ItemTags addTreeBand) — the decisions
 * .p25-leaves-logs-tags-deferred unlock debt this card clears.
 */
public final class GT6TreeLogBlock extends RotatedPillarBlock {

    private final GT6TreeKind mKind;

    public GT6TreeLogBlock(GT6TreeKind aKind) {
        super(Properties.of().mapColor(MapColor.WOOD).strength(2.0F)
                .sound(net.minecraft.world.level.block.SoundType.WOOD));
        mKind = aKind;
    }

    public GT6TreeKind kind() {
        return mKind;
    }
}
