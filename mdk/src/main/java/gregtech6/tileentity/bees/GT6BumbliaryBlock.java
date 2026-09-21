package gregtech6.tileentity.bees;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

import gregtech6.block.GTEntityBlock;
import gregtech6.registry.GT6BeeHives;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The Bumbliary block (task p33-bees-lv3-b-bumbliary) — the block carrier of the MTE
 * 32741 / 32007 ports (Loader_MultiTileEntities.java:2222-2223). Both variants share the
 * class; the mounted BET picks the TE layout ({@link GT6BumbliaryBlockEntity#slotCount}).
 *
 * <p>Wooden-machined physicality (the {@code aWooden} rows): strength 5.0/5.0 on the
 * primary (:2222 {@code NBT_HARDNESS 5.0F / NBT_RESISTANCE 5.0F}), 6.0/6.0 on the
 * advanced (:2223), wood sound. The block drops itself (the standard machine-loot face —
 * the upstream MTE dropped the assembled item) and the contents scatter through the
 * vanilla container {@code onRemove} walk.
 */
public class GT6BumbliaryBlock extends GTEntityBlock {

	/** The facing (the upstream MTE housing carries one; cosmetic here, rides placement
	 *  and the standard rotate/mirror). */
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	private final boolean mAdvanced;

	public GT6BumbliaryBlock(Properties aProperties, boolean aAdvanced) {
		super(aProperties);
		mAdvanced = aAdvanced;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	//? if neoforge {
	/*
	// 21.1 made BaseEntityBlock.codec() abstract (the GT6BumbleHiveBlock fork shape).
	@Override
	protected com.mojang.serialization.MapCodec<? extends GT6BumbliaryBlock> codec() {
		return simpleCodec(aProperties -> new GT6BumbliaryBlock(aProperties, mAdvanced));
	}
	*///?}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> aBuilder) {
		aBuilder.add(FACING);
	}

	@Nullable
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext aContext) {
		return defaultBlockState().setValue(FACING, aContext.getHorizontalDirection().getOpposite());
	}

	/** The standard facing spin (the cosmetic housing face). */
	@Override
	public BlockState rotate(BlockState aState, Rotation aRotation) {
		return aState.setValue(FACING, aRotation.rotate(aState.getValue(FACING)));
	}

	@Override
	public BlockState mirror(BlockState aState, Mirror aMirror) {
		return aState.rotate(aMirror.getRotation(aState.getValue(FACING)));
	}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return mAdvanced ? GT6BeeHives.BUMBLIARY_ADVANCED_BE.get() : GT6BeeHives.BUMBLIARY_BE.get();
	}

	/** The vanilla BaseEntityBlock INVISIBLE default beaten back to MODEL (the census face). */
	@Override
	public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState aState) {
		return net.minecraft.world.level.block.RenderShape.MODEL;
	}

	/** The contents scatter on any structural change (the vanilla container walk). */
	@Override
	public void onRemove(BlockState aState, Level aLevel, BlockPos aPos, BlockState aNewState, boolean aMoved) {
		if (!aState.is(aNewState.getBlock()) && aLevel.getBlockEntity(aPos) instanceof GT6BumbliaryBlockEntity tBumbliary) {
			net.minecraft.world.Containers.dropContents(aLevel, aPos, tBumbliary);
		}
		super.onRemove(aState, aLevel, aPos, aNewState, aMoved);
	}
}
