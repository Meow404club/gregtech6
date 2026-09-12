package gregtech6.block.energy;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import gregtech6.block.GTEntityBlock;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The Electric Transformer block (task p28-c-ulv-lv-transformer) — the facing-cube
 * carrier of {@link gregtech6.tileentity.energy.GT6ElectricTransformerBlockEntity},
 * the {@code GT6DynamoBlock} shape with the INPUT/OUTPUT sides swapped to the
 * transformer convention (Base11 :63-64): the FRONT face ({@code mFacing}) is the
 * INPUT face (normal mode — the high side, a 32 EU packet source plugs in here), ALL
 * OTHER faces are output (the low side fans out); the reversed mode swaps the sets.
 * Row :881 hardness/resistance 4.0/4.0 (the NBT_HARDNESS/NBT_RESISTANCE columns, the
 * dynamo family block properties).
 *
 * <p>Placement = the GT6PlacementFacing canon (the dynamo form: the front TOWARDS the
 * placer — you stand on the SOURCE side, the consumers sit behind). The BE mirror
 * re-syncs from the state each tick (the transformer syncFacingFromState form — the
 * state is the authority). The family BET rides a supplier (the GT6DynamoBlock ruling:
 * resolved at call time, never class-load). NO use override (no GUI — upstream has
 * none; the mode flip is the NBT face, see the BE crop note), NO onRemove override
 * (the BaseEntityBlock kill+recreate lesson), no ACTIVE property (the W2 render card).
 */
public class GT6ElectricTransformerBlock extends GTEntityBlock {

	/** Facing property (horizontal — FRONT is the input face, ALL-BUT-FRONT the output). */
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	/** The family BET (the GT6ElectricTransformers registry object supplier). */
	private final Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> mTickerType;

	public GT6ElectricTransformerBlock(Properties aProperties,
			Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> aTickerType) {
		super(aProperties);
		mTickerType = aTickerType;
		registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	//? if neoforge {
	/*
	// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
	// dispatch) — the GT6DynamoBlock simpleCodec representative-value form (tier 0,
	// the family supplier dropped — the GTTransformerRotationBlock codec note).
	@Override
	protected com.mojang.serialization.MapCodec<? extends GT6ElectricTransformerBlock> codec() {
		return simpleCodec(aProperties -> new GT6ElectricTransformerBlock(aProperties, () -> null));
	}
	*///?}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> aBuilder) {
		aBuilder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext aContext) {
		// the front TOWARDS the placer — you stand on the source side (the GT6PlacementFacing
		// canon; the FRONT is the INPUT face here, the dynamo's output-front mirrored)
		return defaultBlockState().setValue(FACING, gregtech6.block.GT6PlacementFacing.facingTowardsPlacer(aContext.getHorizontalDirection()));
	}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return mTickerType.get();
	}

	@Override
	@Nullable
	public BlockEntity newBlockEntity(BlockPos aPos, BlockState aState) {
		BlockEntityType<? extends TileEntityBase03TicksAndSync> tType = tickerType();
		return tType == null ? null : tType.create(aPos, aState); // the GTEntityBlock form, null-guarded for the offline codec representative
	}

	@Override
	@Nullable
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level aLevel, BlockState aState, BlockEntityType<T> aType) {
		BlockEntityType<? extends TileEntityBase03TicksAndSync> tType = tickerType();
		if (tType == null || aType != tType) {
			return null;
		}
		return (aTickerLevel, aPos, aTickerState, aTile) -> {
			if (aTile instanceof TileEntityBase03TicksAndSync tTile && tTile.canUpdate() && !tTile.isRemoved()) {
				tTile.updateEntity();
			}
		};
	}

	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL; // BaseEntityBlock default INVISIBLE is for BER blocks
	}
}
