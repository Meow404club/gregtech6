package gregtech6.block.surface;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The GT6 wild bush (task p30-w6-t2-surface-blocks) — the block-only port of upstream
 * MTE 32759 "Berry Bush" (Loader_MultiTileEntities.java:2030, hardness 0.5 / resistance
 * 0.3, WorldgenBushes.java:86 the growth-stage + facing NBT). The vanilla
 * {@link BushBlock} base carries the ground-attach (mayPlaceOn = the plantableGreens
 * face, WorldgenBushes.java:62).
 *
 * <p>DECLARED DEVIATION (card spec ④): the berry NBT face is CUT — the berry items are
 * the food domain (unported), so the bush is a plain decorative block that drops itself;
 * the MTE growth stages (oStage/mStage), the berry-set right-click and the 5-part cluster
 * shape (core + 4 facing sides, WorldgenBushes.java:71-76) collapse to ONE static block —
 * the placed feature scatters single blocks. The texture is the upstream grayscale
 * {@code bush/colored/bush.png} pre-coloured at borrow time (the grass-card
 * pre-coloured-PNG precedent).
 */
public final class GT6WildBushBlock extends BushBlock {

    /** The inset leaf-ball box (the non-full plant shape; MTE had a full-interact box). */
    protected static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 14.0, 14.0);

    public GT6WildBushBlock(Properties aProperties) {
        super(aProperties);
    }

    //? if neoforge {
    /*
    // 21.1 made BlockBehaviour.codec() abstract (the vanilla 1.21 block-state codec
    // dispatch). The simpleCodec representative-value form is the vanilla StairBlock
    // precedent (the TestMachineBlock fork shape) — world save/load never runs through
    // this codec (the registry-id + property mapper does).
    @Override
    protected com.mojang.serialization.MapCodec<? extends GT6WildBushBlock> codec() {
        return simpleCodec(GT6WildBushBlock::new);
    }
    *///?}

    @Override
    public VoxelShape getShape(BlockState aState, BlockGetter aLevel, BlockPos aPos, CollisionContext aContext) {
        return SHAPE;
    }
}
