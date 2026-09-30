package gregtech6.block.multiblock;

import net.minecraft.world.level.block.entity.BlockEntityType;

import gregtech6.registry.GTMultiBlocks;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The Bedrock Mining Drill controller block (task bedrock-drill) — the concrete
 * {@link GTMultiBlockControllerBlock} mounting the drill BET (the GTVonDaGraaggBlock
 * minimal form: everything visual/behavioural is base-owned FACING + FORMED, this class
 * only mounts the BET; the machine runs headless — the lubricant/fluid/output faces ride
 * the capabilities and the drill has no GUI).
 */
public class GTBedrockDrillBlock extends GTMultiBlockControllerBlock {

	public GTBedrockDrillBlock(Properties aProperties) {
		super(aProperties);
	}
	//? if neoforge {
	/*
	// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
	// dispatch) — the simpleCodec representative-value form (the GTCokeOvenBlock precedent).
	@Override
	protected com.mojang.serialization.MapCodec<? extends GTBedrockDrillBlock> codec() {
		return simpleCodec(GTBedrockDrillBlock::new);
	}
	*///?}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return GTMultiBlocks.BEDROCK_DRILL_BE.get();
	}
}
