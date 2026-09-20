package gregtech6.tileentity.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import gregtech6.block.GT6PlacementFacing;
import gregtech6.block.GTEntityBlock;
import gregtech6.registry.GT6Placeables;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The Greg o'Lantern block carrier (task p32-placeables) — the upstream row
 * "Greg o'Lantern" (Loader_MultiTileEntities.java:2031, MTE 32758, the aUtilWood row over
 * {@code MultiTileEntityGregOLantern}).
 *
 * <p>Declarative folds (the BE javadoc carries the full map): full cube (the upstream
 * solid-surface trio :47-49) with the vanilla jack_o_lantern material numbers — the
 * upstream copies Blocks.lit_pumpkin hardness/resistance (:43-45), which IS the vanilla
 * {@code strength(1.0F)} — wood sound (the aUtilWood row), {@code lightLevel(15)} (the
 * upstream {@code getLightValue() :41}), horizontal-only facing (the upstream
 * {@code getValidSides SIDES_HORIZONTAL :52}) set TOWARDS the placer (the
 * GT6PlacementFacing canon: the upstream {@code getDefaultSide() SIDE_FRONT :53} is the
 * carved face looking at you, the jack_o_lantern idiom). GTEntityBlock mount: the TE
 * carrier face of the "2 TE" registration assertion; the BE itself is stateless.
 */
public class GT6GregOLanternBlock extends GTEntityBlock {

	/** The carved GREG face — horizontal only (the upstream SIDES_HORIZONTAL valid sides). */
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	/** The full-cube shape (the upstream isSideSolid/isSurfaceOpaque T trio). */
	protected static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

	public GT6GregOLanternBlock(Properties aProperties) {
		super(aProperties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
	}

	//? if neoforge {
	/*
	// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
	// dispatch) — the GTOvenBlock simpleCodec precedent verbatim.
	@Override
	protected com.mojang.serialization.MapCodec<? extends GT6GregOLanternBlock> codec() {
		return simpleCodec(GT6GregOLanternBlock::new);
	}
	*///?}

	/**
	 * The vanilla jack_o_lantern material numbers (upstream :43-45 copies of
	 * Blocks.lit_pumpkin). NOT named {@code properties()} — 21.1 gained a static
	 * {@code BlockBehaviour.properties()} and the name became an override clash.
	 */
	public static Properties newProperties() {
		return Block.Properties.of().strength(1.0F).sound(SoundType.WOOD).lightLevel(aState -> 15);
	}

	/** The vanilla BaseEntityBlock INVISIBLE default beaten back to MODEL (the GT6BumbleHiveBlock form). */
	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL;
	}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return GT6Placeables.GREG_O_LANTERN_BE.get();
	}

	@Override
	public VoxelShape getShape(BlockState aState, BlockGetter aLevel, BlockPos aPos, CollisionContext aContext) {
		return SHAPE;
	}

	/** The carved face TOWARDS the placer (the GT6PlacementFacing canon, the :53 SIDE_FRONT fold). */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext aContext) {
		return defaultBlockState().setValue(FACING, GT6PlacementFacing.facingTowardsPlacer(aContext.getHorizontalDirection()));
	}

	@Override
	public BlockState rotate(BlockState aState, Rotation aRotation) {
		return aState.setValue(FACING, aRotation.rotate(aState.getValue(FACING)));
	}

	@Override
	public BlockState mirror(BlockState aState, Mirror aMirror) {
		return aState.rotate(aMirror.getRotation(aState.getValue(FACING)));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> aBuilder) {
		aBuilder.add(FACING);
	}
}
