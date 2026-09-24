package gregtech6.block.logistics;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import gregtech6.block.GTBlockProperties;
import gregtech6.block.GTEntityBlock;
import gregtech6.registry.GT6Logistics;
import net.minecraft.world.entity.Entity;
import gregtech6.covers.ICoverableTE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * The GT6 logistics wire block (task p32-logistics-lv2) — the block carrier of the
 * single "Logistics Wire" row (upstream meta id 24901, Loader_MultiTileEntities.java:1819),
 * the {@link gregtech6.block.pipe.GTItemPipeBlock} shape minus the interaction layer:
 * the upstream wire carries no tool interactions beyond the cutter facing (MultiTileEntityWireLogistics
 * :57, a declared trim on the BE javadoc), connections form at placement (the BlockItem
 * onPlaced chain) and through the {@link gregtech6.tileentity.connectors.TileEntityBase09Connector}
 * handshake.
 *
 * <p>{@link #CONNECTIONS} is the 6-bit connection mask BlockState — the same visual
 * counterpart contract as the pipe/wire family (the BE writes it in onConnectionChange).
 * NO onRemove override (the id59 red line). Hardness/resistance = the registration NBT
 * pair NBT_HARDNESS 1.0F / NBT_RESISTANCE 2.0F (Loader:1819), carried as the vanilla
 * block properties (the item-pipe registration-carrier precedent).
 */
public class GTLogisticsWireBlock extends GTEntityBlock {

	/** The 6-bit connection mask (0..63) — the GTBlockProperties single instance (ADR-P16-2). */
	public static final IntegerProperty CONNECTIONS = GTBlockProperties.CONNECTIONS;

	public GTLogisticsWireBlock(Properties aProperties) {
		super(aProperties);
		registerDefaultState(defaultBlockState().setValue(CONNECTIONS, 0));
	}
	//? if neoforge {
	/*
	// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
	// dispatch). The simpleCodec representative-value form is the vanilla StairBlock
	// precedent — a parse-time default carrying no live config (GTItemPipeBlock fork verbatim).
	@Override
	protected com.mojang.serialization.MapCodec<? extends GTLogisticsWireBlock> codec() {
		return simpleCodec(GTLogisticsWireBlock::new);
	}
	*///?}

	@Override
	protected BlockEntityType<? extends gregtech6.tileentity.TileEntityBase03TicksAndSync> tickerType() {
		return GT6Logistics.LOGISTICS_WIRE_BE.get();
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> aBuilder) {
		super.createBlockStateDefinition(aBuilder);
		aBuilder.add(CONNECTIONS);
	}

	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL; // BaseEntityBlock default INVISIBLE is for BER blocks
	}

	@Override
	public void stepOn(Level aLevel, BlockPos aPos, BlockState aState, Entity aEntity) {
		super.stepOn(aLevel, aPos, aState, aEntity);
		if (aLevel.getBlockEntity(aPos) instanceof ICoverableTE tCoverable) tCoverable.onCoverWalkOver(aEntity); // MultiTileEntityBlock.java:306 -> 06Covers:428 (p37-covers-crafting-asphalt)
	}
}
