package gregtech6.block.multiblock;

import net.minecraft.world.level.block.entity.BlockEntityType;

import gregtech6.registry.GTMultiBlocks;
import gregtech6.tileentity.multiblocks.TileEntityCokeOven;

/**
 * The Coke Oven controller block — the concrete {@link GTMultiBlockControllerBlock} of the
 * first multiblock (upstream MultiTileEntityCokeOven, task p4-multiblock-framework spec ④).
 * Everything visual/behavioural is base-owned (FACING + FORMED); this class only mounts the
 * Coke Oven BET.
 */
public class GTCokeOvenBlock extends GTMultiBlockControllerBlock {

	public GTCokeOvenBlock(Properties aProperties) {
		super(aProperties);
	}

	@Override
	protected BlockEntityType<? extends gregtech6.tileentity.TileEntityBase03TicksAndSync> tickerType() {
		return GTMultiBlocks.COKE_OVEN_BE.get();
	}
}
