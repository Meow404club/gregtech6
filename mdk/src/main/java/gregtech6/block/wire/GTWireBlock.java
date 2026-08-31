package gregtech6.block.wire;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

import gregapi.oredict.OreDictMaterial;
import gregtech6.block.GTEntityBlock;
import gregtech6.registry.GTBlockEntities;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The GT6 electric wire block (task p7-d2-cable spec ④) — the block side of the wire
 * family over the shared BET (ADR-P3-1), mirroring the GTFluidPipeBlock shape. The
 * carrier pattern (the GTBarrelBlock registration-carrier precedent): the block carries
 * the wire ratings upstream wrote into the MTE definition NBT — the {@code NBT_PIPESIZE}
 * voltage, the {@code NBT_PIPEBANDWIDTH} amperage and the {@code NBT_PIPELOSS} per-segment
 * loss (MultiTileEntityWireElectric.java:114-116 read them back in readFromNBT2). Since task
 * p9-wire-family-w1 the block additionally carries the row identity the W2 render card consumes:
 * the {@link OreDictMaterial} (may be null on the two material-less p7 legacy blocks), the
 * band size {@code n} (the "1x..16x" multiplier) and the {@code insulated} form flag — the
 * ratings (voltage/amperage/loss/diameter/max-stack) still come from the registration site
 * (GTWireSpecs.Variant, the direct addElectricWires :71-109 transcription).
 *
 * <p>{@link #CONNECTIONS} is the 6-bit connection mask as a BlockState property — the
 * visual counterpart of {@code TileEntityBase09Connector.mConnections}, written by the
 * wire BlockEntity on every connection change (GTWireBlockEntity.onConnectionChange,
 * the pipe twin). Since p9-wire-family-w1 the datagen blockstate maps every mask to the one
 * placeholder model through a single property-less variant (the ModelBakery.java:173 empty-key
 * wildcard) — the connection-aware model picking is the W2 card's BakedModel.
 *
 * <p>{@code use} is a SEMANTIC LOCK (task p9-wire-family-w1 spec ④, promoted from the
 * accidental base default to an explicit override): it always returns
 * {@link InteractionResult#PASS}. Upstream wires have no vanilla right-click action — the
 * connection-management tool is the cutter ({@code getFacingTool() = TOOL_cutter},
 * MultiTileEntityWireElectric.java:245), which travels the IBlockToolable chain
 * (gregapi/block/IBlockToolable.java:81), NOT the vanilla use chain; the vanilla right-click
 * with any held item must fall through ("nothing happens", the upstream
 * TileEntityBase06Covers.onBlockActivated2 :106-130 outcome for GUI-less wires). Never turn
 * this into a connect/disconnect toggle — that is the W3 cutter card's surface, and until it
 * lands {@code /gt6wire connect} is the driver. NO onRemove override — the BaseEntityBlock
 * kill+recreate lesson (remember id59).
 */
public class GTWireBlock extends GTEntityBlock {

	/** The 6-bit connection mask (0..63) — bit i = side i connected (GT6 side order). */
	public static final IntegerProperty CONNECTIONS = IntegerProperty.create("connections", 0, 63);

	private final long mVoltage;
	private final long mAmperage;
	private final long mLoss;
	private final OreDictMaterial mMaterial;
	private final int mSize;
	private final boolean mInsulated;
	private final int mDiameter;

	/**
	 * The p7 legacy form (the two material-less variants) — the vanilla-block fallback ratings
	 * (the upstream field defaults :64) with no row identity.
	 */
	public GTWireBlock(long aVoltage, long aAmperage, long aLoss, Properties aProperties) {
		this(aVoltage, aAmperage, aLoss, null, 1, false, 0, aProperties);
	}

	/**
	 * The full W1 carrier (task p9-wire-family-w1 spec ②).
	 *
	 * @param aVoltage the packet size ceiling in EU (upstream NBT_PIPESIZE)
	 * @param aAmperage the packet count ceiling (upstream NBT_PIPEBANDWIDTH)
	 * @param aLoss the EU lost per segment (upstream NBT_PIPELOSS)
	 * @param aMaterial the row material (the W2 tint anchor; null on the legacy pair)
	 * @param aSize the band multiplier n of "nx" (upstream wireGt01..16 / cableGt01..12)
	 * @param aInsulated false = bare wire (renderType 0), true = insulated cable (renderType 1)
	 * @param aDiameter the upstream PX_P diameter index (NBT_DIAMETER, W2 geometry seed)
	 */
	public GTWireBlock(long aVoltage, long aAmperage, long aLoss, @Nullable OreDictMaterial aMaterial,
			int aSize, boolean aInsulated, int aDiameter, Properties aProperties) {
		super(aProperties);
		mVoltage = aVoltage;
		mAmperage = aAmperage;
		mLoss = aLoss;
		mMaterial = aMaterial;
		mSize = aSize;
		mInsulated = aInsulated;
		mDiameter = aDiameter;
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

	/** The row material (W2 tint anchor; null on the two material-less p7 legacy blocks). */
	@Nullable
	public OreDictMaterial material() {
		return mMaterial;
	}

	/** The band multiplier n of "nx" (upstream wireGt01..16 / cableGt01/02/04/08/12). */
	public int size() {
		return mSize;
	}

	/** False = bare wire (upstream NBT_PIPERENDER 0), true = insulated cable (NBT_PIPERENDER 1). */
	public boolean insulated() {
		return mInsulated;
	}

	/** The upstream PX_P diameter index (NBT_DIAMETER — the W2 geometry seed, W1 data-only). */
	public int diameter() {
		return mDiameter;
	}

	/**
	 * The explicit PASS lock (spec ④): non-tool right-clicks are never consumed — the wire has
	 * no GUI and the connection tool is the cutter on the IBlockToolable chain, not use. See
	 * the class javadoc; do not override again.
	 */
	@Override
	public InteractionResult use(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer,
			InteractionHand aHand, BlockHitResult aHit) {
		return InteractionResult.PASS;
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
