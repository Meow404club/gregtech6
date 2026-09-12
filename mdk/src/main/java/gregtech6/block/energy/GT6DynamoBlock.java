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
 * The Dynamo family block (task p28-c-dynamo-family-be) — the facing-cube carrier shared
 * by BOTH dynamo families (Flux and Electric; one block class, ten registrations), upstream
 * MultiTileEntityDynamoFlux / MultiTileEntityDynamoElectric (aClass rows,
 * Loader_MultiTileEntities.java:945-957 — NBT_HARDNESS 4.0F, NBT_RESISTANCE 4.0F, stack
 * 16, no GUI: {@code canDrop} = F, TileEntityBase10EnergyConverter :161).
 *
 * <p>Facing = the placement orientation over the GT6PlacementFacing canon (the
 * GTTransformerRotationBlock verbatim shape): the FRONT face ({@code mFacing}) is the
 * OUTPUT face (upstream isOutput {@code mFacing == aSide}, DynamoFlux :37 — the dynamo is
 * placed facing its consumer), the BACK face the only input (upstream isInput
 * {@code mFacing == OPOS[aSide]}, :36 — the driven axle sits behind). The BE mirror
 * re-syncs from the state each tick (the transformer syncFacingFromState form — the
 * {@code /setblock gt6:flux_dynamo[facing=...]} RCON path, the state is the authority).
 *
 * <p>The row index ({@link #tier}) selects the family ladder column (the GTOvenBlock
 * "block identity IS the config selector" ruling): the Flux rows index
 * {@code MT.FLUX_T[1..5]}, the Electric rows the Electric_T[1..5] word set
 * (Loader :946-957). The family BET rides a supplier (the transformer returned its
 * RegistryObject directly; this shared class cannot name one family, so the registrations
 * hand their own in — resolved at call time, never class-load, the GTWireSpecs:35 ruling).
 *
 * <p>NO use override (no GUI — upstream has none), NO onRemove override (the
 * BaseEntityBlock kill+recreate lesson, remember id59), no ACTIVE property yet (the
 * activity visual rides the W2 render card, the trinary collapsed to the mActive flag).
 */
public class GT6DynamoBlock extends GTEntityBlock {

	/** Facing property (horizontal — FRONT is the output face, BACK the input). */
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	/** The row index of this block in its family ladder (0 = T1 .. 4 = T5). */
	private final int mTier;

	/** The family BET (the GT6FluxDynamos/GT6ElectricDynamos registry object supplier). */
	private final Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> mTickerType;

	public GT6DynamoBlock(Properties aProperties, int aTier,
			Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> aTickerType) {
		super(aProperties);
		mTier = aTier;
		mTickerType = aTickerType;
		registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	/**
	 * The ladder row index of the dynamo block in {@code aState} — 0 for a non-dynamo
	 * block (the offline STONE-state fixtures keep the T1 field defaults, the
	 * GTOvenBlock.tier zero-regression guard).
	 */
	public static int tier(BlockState aState) {
		return aState.getBlock() instanceof GT6DynamoBlock tDynamo ? tDynamo.mTier : 0;
	}
	//? if neoforge {
	/*
	// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
	// dispatch). The simpleCodec representative-value form is the vanilla StairBlock
	// precedent — a parse-time default carrying no live config (tier 0, the family
	// supplier dropped — the GTTransformerRotationBlock codec note); world save/load
	// never runs through this codec (the registry-id + property mapper does).
	@Override
	protected com.mojang.serialization.MapCodec<? extends GT6DynamoBlock> codec() {
		return simpleCodec(aProperties -> new GT6DynamoBlock(aProperties, 0, () -> null));
	}
	*///?}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> aBuilder) {
		aBuilder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext aContext) {
		// the front TOWARDS the placer — you stand on the consumer side, the driven axle
		// sits behind (the GT6PlacementFacing canon, task p28-singleblock-facing-canon)
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
