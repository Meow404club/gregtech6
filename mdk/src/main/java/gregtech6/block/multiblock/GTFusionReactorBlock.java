package gregtech6.block.multiblock;

import net.minecraft.world.level.block.entity.BlockEntityType;

import gregtech6.registry.GTMultiBlocks;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The Fusion Reactor controller block (task p31-fusion) — the concrete
 * {@link GTMultiBlockControllerBlock} mounting the Fusion BET (the GTMassfabBlock minimal
 * form: everything visual/behavioural is base-owned FACING + FORMED, this class only
 * mounts the BET; no use-face — the controller runs headless, the W2 menu-null form).
 */
public class GTFusionReactorBlock extends GTMultiBlockControllerBlock {

	public GTFusionReactorBlock(Properties aProperties) {
		super(aProperties);
	}
	//? if neoforge {
	/*
	// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
	// dispatch) — the simpleCodec representative-value form (the GTCokeOvenBlock precedent).
	@Override
	protected com.mojang.serialization.MapCodec<? extends GTFusionReactorBlock> codec() {
		return simpleCodec(aProperties -> new GTFusionReactorBlock(aProperties));
	}
	*///?}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return GTMultiBlocks.FUSION_REACTOR_BE.get();
	}
}
