package gregtech6.block.energy;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import gregtech6.block.GTEntityBlock;
import gregtech6.registry.GTBlockEntities;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The Transformer Rotation block (task p12-gearbox-transformer) — the facing-cube carrier
 * over the shared BET (ADR-P3-1), one single wood-row variant for this card (the upstream
 * "Wooden Transformer Gearbox" Loader row :1668 — hardness 6.0 / resistance 6.0; the
 * material family fan-out rides the pool).
 *
 * <p>Facing = the placement orientation, the player's horizontal look direction (the crank
 * {@code getStateForPlacement} precedent): FRONT = the input face, BACK = the output face
 * (MultiTileEntityTransformerRotation :42-45 — input FACE_FRONT, output FACE_BACK). The
 * BE mirror re-syncs from the state each tick ({@code /setblock
 * gt6:transformer_rotation[facing=...]} RCON path — the state is the command-side
 * authority).
 *
 * <p>NO use override (no GUI, no interaction — the monkey-wrench reversal is the pool
 * card), NO onRemove override (the BaseEntityBlock kill+recreate lesson, remember id59).
 */
public class GTTransformerRotationBlock extends GTEntityBlock {

	/** Facing property (horizontal — FRONT is the input face, BACK the output). */
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	public GTTransformerRotationBlock(Properties aProperties) {
		super(aProperties);
		registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}
	//? if neoforge {
	/*
	// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
	// dispatch). The simpleCodec representative-value form is the vanilla StairBlock
	// precedent — a parse-time default carrying no live config; world save/load never
	// runs through this codec (the registry-id + property mapper does).
	@Override
	protected com.mojang.serialization.MapCodec<? extends GTTransformerRotationBlock> codec() {
		return simpleCodec(aProperties -> new GTTransformerRotationBlock(aProperties));
	}
	*///?}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> aBuilder) {
		aBuilder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext aContext) {
		// the front TOWARDS the placer, the driven axle sits behind (the
		// GT6PlacementFacing canon, task p28-singleblock-facing-canon — the old comment
		// declared this intent while the code wrote the raw view direction, the same-disease
		// self-witness; the BE follows via syncFacingFromState :164)
		return defaultBlockState().setValue(FACING, gregtech6.block.GT6PlacementFacing.facingTowardsPlacer(aContext.getHorizontalDirection()));
	}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return GTBlockEntities.TRANSFORMER_BE.get();
	}

	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL; // BaseEntityBlock default INVISIBLE is for BER blocks
	}
}
