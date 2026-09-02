package gregtech6.block.energy;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import gregtech6.block.GTEntityBlock;
import gregtech6.registry.GT6Kinetics;
import gregtech6.registry.GTBlockEntities;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The Axle block (task p12-axle-family spec ②) — the AXIS-axis carrier over the shared BET
 * (ADR-P3-1), the vanilla-log property shape ({@code RotatedPillarBlock}): the straight
 * line is the whole connectivity model of the port (the declared port-ism — upstream
 * mFacing six-way placement collapses to one three-state axis because the
 * ConnectorStraight base is a line semantics, TileEntityBase11ConnectorStraight.java:63-66
 * "Makes sure the Axles are going actually straight").
 *
 * <p>Placement = the clicked face's axis (the vanilla RotatedPillarBlock.getStateForPlacement
 * :48-50 {@code trySetValue(AXIS, context.getClickedFace().getAxis())} form): an axle placed
 * against a floor/wall/ceiling runs along that face's axis — the RCON path pins it with
 * {@code /setblock gt6:axle_wood_small[axis=x]}.
 *
 * <p>Shape = the upstream straight-connector bounds (TileEntityBase11ConnectorStraight
 * :60: full length on the axis, {@code (1-d)/2} insets on the other two, d = the diameter
 * of the row — PX_P 6/9/12/16 px, CS.java:492), carried as a constructor parameter per
 * material x diameter row.
 *
 * <p>NO use override (no GUI, no interaction — the crank's use IS its drive, the axle has
 * none), NO onRemove override (the BaseEntityBlock kill+recreate lesson, remember id59 —
 * the pop-off break rides {@code level.destroyBlock} from the BE, never the block).
 */
public class GTAxleBlock extends GTEntityBlock {

	/** The axis property — the shared vanilla instance (the property-interning convention). */
	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;

	/** The row diameter in 1/16 block units (PX_P[tier]: 6/9/12/16, CS.java:492). */
	public final int diameterPx;

	/** The Loader kinetic row this block mounts (the NBT_PIPESIZE/NBT_PIPEBANDWIDTH carrier). */
	public final GT6Kinetics.AxleSpec spec;

	/** The diameter index into {@link GT6Kinetics#AXLE_DIAMETERS} (the bandwidth column). */
	public final int sizeIndex;

	public GTAxleBlock(Properties aProperties, GT6Kinetics.AxleSpec aSpec, int aSizeIndex) {
		super(aProperties);
		spec = aSpec;
		sizeIndex = aSizeIndex;
		diameterPx = GT6Kinetics.AXLE_DIAMETERS[aSizeIndex];
		registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> aBuilder) {
		aBuilder.add(AXIS);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext aContext) {
		// the vanilla RotatedPillarBlock :48-50 form
		return defaultBlockState().setValue(AXIS, aContext.getClickedFace().getAxis());
	}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return GTBlockEntities.AXLE_BE.get();
	}

	@Override
	public net.minecraft.world.level.block.RenderShape getRenderShape(BlockState aState) {
		return net.minecraft.world.level.block.RenderShape.MODEL; // BaseEntityBlock default INVISIBLE is for BER blocks
	}

	@Override
	public VoxelShape getShape(BlockState aState, BlockGetter aLevel, BlockPos aPos, CollisionContext aContext) {
		// the ConnectorStraight :60 bounds form: full [0,1] on the axis, (16-d)/32 block insets elsewhere
		double tInset = (16 - diameterPx) / 32.0;
		return switch (aState.getValue(AXIS)) {
			case X -> Shapes.box(0, tInset, tInset, 1, 1 - tInset, 1 - tInset);
			case Y -> Shapes.box(tInset, 0, tInset, 1 - tInset, 1, 1 - tInset);
			case Z -> Shapes.box(tInset, tInset, 0, 1 - tInset, 1 - tInset, 1);
		};
	}
}
