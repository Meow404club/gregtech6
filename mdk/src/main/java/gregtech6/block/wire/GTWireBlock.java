package gregtech6.block.wire;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

import gregtech6.block.GTEntityBlock;
import gregtech6.registry.GTBlockEntities;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The GT6 electric wire block (task p7-d2-cable spec ④) — the block side of the wire
 * family over the shared BET (ADR-P3-1), mirroring the GTFluidPipeBlock shape. The
 * carrier pattern (the GTBarrelBlock registration-carrier precedent): the block carries
 * the wire ratings upstream wrote into the MTE definition NBT — the {@code NBT_PIPESIZE}
 * voltage, the {@code NBT_PIPEBANDWIDTH} amperage and the {@code NBT_PIPELOSS} per-segment
 * loss (MultiTileEntityWireElectric.java:114-116 read them back in readFromNBT2). W1
 * ships two variants: 1x (32 EU / 1 A / 1 loss — the upstream field defaults :64) and
 * 2x (32 EU / 2 A / 1 loss, the upstream "2x" bandwidth doubling :73); the 16-wire
 * 5-cable material spectrum is a pool item.
 *
 * <p>{@link #CONNECTIONS} is the 6-bit connection mask as a BlockState property — the
 * visual counterpart of {@code TileEntityBase09Connector.mConnections}, written by the
 * wire BlockEntity on every connection change (GTWireBlockEntity.onConnectionChange,
 * the pipe twin) and rendered as one variant per mask value in datagen.
 *
 * <p>No {@code use} interaction: W1 wires have no wrench/cover layer (the card fixes
 * {@code use} = none; the base default PASS stands, connection work goes through
 * {@code /gt6wire connect} until the tool system reaches wires). NO onRemove override —
 * the BaseEntityBlock kill+recreate lesson (remember id59).
 */
public class GTWireBlock extends GTEntityBlock {

	/** The 6-bit connection mask (0..63) — bit i = side i connected (GT6 side order). */
	public static final IntegerProperty CONNECTIONS = IntegerProperty.create("connections", 0, 63);

	private final long mVoltage;
	private final long mAmperage;
	private final long mLoss;

	/**
	 * @param aVoltage the packet size ceiling in EU (upstream NBT_PIPESIZE, 32 for W1)
	 * @param aAmperage the packet count ceiling (upstream NBT_PIPEBANDWIDTH, 1x/2x → 1/2)
	 * @param aLoss the EU lost per segment (upstream NBT_PIPELOSS, 1 for W1)
	 */
	public GTWireBlock(long aVoltage, long aAmperage, long aLoss, Properties aProperties) {
		super(aProperties);
		mVoltage = aVoltage;
		mAmperage = aAmperage;
		mLoss = aLoss;
		registerDefaultState(defaultBlockState().setValue(CONNECTIONS, 0));
	}

	/** The voltage ceiling in EU (upstream NBT_PIPESIZE). */
	public long voltageL() {
		return mVoltage;
	}

	/** The amperage ceiling (upstream NBT_PIPEBANDWIDTH). */
	public long amperageL() {
		return mAmperage;
	}

	/** The per-segment loss in EU (upstream NBT_PIPELOSS). */
	public long lossL() {
		return mLoss;
	}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return GTBlockEntities.WIRE_ELECTRIC_BE.get();
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
