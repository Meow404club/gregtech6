package gregtech6.block.energy;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
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
 * The Magic Field Absorber block carrier (task p32-magic-absorber) — the dynamo-carrier
 * form over the SIX-WAY facing (upstream :128-129 getValidSides = SIDES_BOTTOM_HORIZONTAL
 * with the default {@code getDefaultSide() = SIDE_BOTTOM}): the FACING face is the OUTPUT
 * face (upstream isEnergyEmittingTo :117 {@code aSide == mFacing}), the TOP face is the
 * trophy seat (the :84 {@code getBlockAtSide(SIDE_TOP)} probe).
 *
 * <p>The upstream placement walk (:73, the horizontal-click side form) is not reproduced:
 * placement always lands DOWN (the canonical posture — the absorber sits on the consumer,
 * emitting into it; declared in the BE class doc). {@code /setblock} rotates; a
 * neighbor update on any face re-arms the BE's trophy probe (the GTWireBlock
 * markBlockUpdated form — the trophy placement on top is a neighbor update).
 */
public class GT6MagicAbsorberBlock extends GTEntityBlock {

	/** Six-way facing (DOWN default — the upstream SIDE_BOTTOM). */
	public static final DirectionProperty FACING = BlockStateProperties.FACING;

	/** The family BET (the GT6MagicAbsorbers registry object supplier, the dynamo-carrier form). */
	private final Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> mTickerType;

	public GT6MagicAbsorberBlock(Properties aProperties,
			Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> aTickerType) {
		super(aProperties);
		mTickerType = aTickerType;
		registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.DOWN));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> aBuilder) {
		aBuilder.add(FACING);
	}

	//? if neoforge {
	/*
	// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
	// dispatch) — the GT6DynamoBlock representative-value form: a parse-time default
	// carrying no live config; world save/load never runs through this codec.
	@Override
	protected com.mojang.serialization.MapCodec<? extends GT6MagicAbsorberBlock> codec() {
		return simpleCodec(aProperties -> new GT6MagicAbsorberBlock(aProperties, () -> null));
	}
	*///?}

	@Override
	public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext aContext) {
		return defaultBlockState(); // the canonical posture: the output face DOWN (the BE doc's declared placement cut)
	}

	@Override
	public void neighborChanged(BlockState aState, Level aLevel, BlockPos aPos, Block aBlock, BlockPos aFromPos, boolean aIsMoving) {
		// the trophy placement/removal on the TOP face is a neighbor update — re-arm the
		// probe (upstream :79 mBlockUpdated; the GTWireBlock :349 markBlockUpdated form)
		if (aLevel.getBlockEntity(aPos) instanceof TileEntityBase03TicksAndSync tTile) {
			tTile.markBlockUpdated();
		}
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
