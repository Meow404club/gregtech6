package gregtech6.block.pipe;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import gregtech6.block.GTEntityBlock;
import gregtech6.registry.GTFluidPipes;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The GT6 fluid pipe block (task p4-fluid-pipes spec ④) — the block side of the pipe
 * family over the shared BET (ADR-P3-1: one BlockEntityType mounting several blocks,
 * the GT6 "one TE class, many material blocks" counterpart). W1 ships the two wood
 * tiers (the card fixes aStat=50 at 50 L / 300 L per tank; the upstream tiny/small/
 * medium multiplier row MultiTileEntityPipeFluid.java:92-94 is collapsed into the two
 * card-named tiers, other materials/tank-counts are a later card).
 *
 * <p>{@link #CONNECTIONS} is the 6-bit connection mask as a BlockState property — the
 * visual counterpart of {@code TileEntityBase09Connector.mConnections} (upstream
 * getDirectionData :98). The pipe BlockEntity writes it on every connection change
 * (onConnectionChange), and the datagen emits a variant per mask value (GT6 renders
 * its connections from the mask too, getTextureSide :522).
 *
 * <p>The BE does the work: this block only carries the tier capacity and the state.
 */
public class GTFluidPipeBlock extends GTEntityBlock {

	/** The 6-bit connection mask (0..63) — bit i = side i connected (GT6 side order). */
	public static final IntegerProperty CONNECTIONS = IntegerProperty.create("connections", 0, 63);

	private final long mCapacityPerTank;

	public GTFluidPipeBlock(long aCapacityPerTank, Properties aProperties) {
		super(aProperties);
		mCapacityPerTank = aCapacityPerTank;
		registerDefaultState(defaultBlockState().setValue(CONNECTIONS, 0));
	}

	/** Per-tank capacity in Liters (upstream NBT_TANK_CAPACITY :114). */
	public long capacityPerTank() {
		return mCapacityPerTank;
	}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return GTFluidPipes.FLUID_PIPE_BE.get();
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
}
