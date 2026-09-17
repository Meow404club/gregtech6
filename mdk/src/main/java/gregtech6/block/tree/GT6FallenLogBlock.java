package gregtech6.block.tree;

import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.material.MapColor;

/**
 * One GT6 fallen-log wood (task p30-w6-t2-surface-blocks): Dead/Rotten/Mossy/Frozen —
 * the upstream special-wood faces of the shared tree-log block. VERIFICATION ANCHOR (the
 * card's "独立块还是原木变体" ruling): Loader_Worldgen.java:603-606 places
 * {@code BlocksGT.Log1} at {@code PILLARS_Y/X/Z[0..3]} (CS.java:765 = meta variant 0-3 x
 * axis bits) — NOT independent blocks and NOT one of the nine t1 species; the four
 * variants are the special wood types Dead/Rotten/Mossy/Frozen (BlockTreeLog1.java:46-62
 * display names, LoaderWoodDictionary.java:165-172 planks/bark rows). The t1 per-pair log
 * universe has no such rows, so the P8 per-pair ADR expands to four plain
 * {@link RotatedPillarBlock} ids ({@code dead_log} etc.) — the axis property is the
 * pillar face the fallen shapes ride (the t1 GT6TreeLogBlock properties verbatim:
 * strength 2.0 + WOOD sound + mapColor WOOD).
 */
public final class GT6FallenLogBlock extends RotatedPillarBlock {

    public GT6FallenLogBlock() {
        super(Properties.of().mapColor(MapColor.WOOD).strength(2.0F)
                .sound(net.minecraft.world.level.block.SoundType.WOOD));
    }
}
