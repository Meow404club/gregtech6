package gregtech6.block.surface;

import net.minecraft.world.level.block.FallingBlock;

/**
 * The GT6 magnetite black sand (task p30-w6-t2-surface-blocks) — the WorldgenBlackSand
 * river-bed soil block. The vanilla sand family rides {@link FallingBlock} gravity; the
 * 1.20.1 {@code SandBlock} is the 21.1-removed face (the SandBlock-into-FallingBlock
 * merge made the parent abstract there), so the shared face is this one concrete
 * subclass — the vanilla-sand properties ride the registration (strength 0.5 + SAND
 * sound), the dust tint is the FallingBlock default black.
 */
public final class GT6BlackSandBlock extends FallingBlock {

    public GT6BlackSandBlock(Properties aProperties) {
        super(aProperties);
    }

    //? if neoforge {
    /*
    // 21.1 made BlockBehaviour.codec() abstract (the vanilla 1.21 block-state codec
    // dispatch). The simpleCodec representative-value form is the vanilla StairBlock
    // precedent (the TestMachineBlock fork shape).
    @Override
    protected com.mojang.serialization.MapCodec<? extends GT6BlackSandBlock> codec() {
        return simpleCodec(GT6BlackSandBlock::new);
    }
    *///?}
}
