package gregtech6.block.multiblock;

import net.minecraft.world.level.block.entity.BlockEntityType;

import gregtech6.registry.GTMultiBlocks;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The Implosion Compressor controller block (task p31-implosion) — the concrete
 * {@link GTMultiBlockControllerBlock} mounting the Implosion BET (the GTCokeOvenBlock
 * minimal form: everything visual/behavioural is base-owned FACING + FORMED, this class
 * only mounts the BET; no use-face — the controller runs headless, the W2 menu-null
 * form the twelve large machines ride).
 */
public class GTImplosionCompressorBlock extends GTMultiBlockControllerBlock {

	public GTImplosionCompressorBlock(Properties aProperties) {
		super(aProperties);
	}
	//? if neoforge {
	/*
	// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
	// dispatch) — the simpleCodec representative-value form (the GTCokeOvenBlock precedent).
	@Override
	protected com.mojang.serialization.MapCodec<? extends GTImplosionCompressorBlock> codec() {
		return simpleCodec(aProperties -> new GTImplosionCompressorBlock(aProperties));
	}
	*///?}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return GTMultiBlocks.IMPLOSION_COMPRESSOR_BE.get();
	}
}
