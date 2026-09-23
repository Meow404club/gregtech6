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

import gregapi.code.TagData;

import gregtech6.block.GTEntityBlock;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The BatteryBox block (task p29-w4-battery-storage ④) — the facing-cube carrier of
 * {@link gregtech6.tileentity.energy.GT6BatteryBoxBlockEntity}, the
 * GT6ElectricTransformerBlock shape with the BatteryBox convention (Base10EnergyBatBox
 * :232-233): the FRONT face ({@code mFacing}) is the OUTPUT face (the emit side), ALL
 * OTHER faces are input. Row :894-:895 hardness/resistance 4.0/4.0, stack 16.
 *
 * <p>Placement = the GT6PlacementFacing canon (the transformer form: the front TOWARDS
 * the placer — you stand on the CONSUMER side, the sources plug in behind). The BE
 * mirror re-syncs from the state each tick (the state is the authority). The block
 * carries the family's slot count (4 or 16 — the NBT_INV_SIZE column) as data; the two
 * sizes share the BE class over two BETs. NO use override (the GUI is the pool —
 * upstream opens a ContainerCommonDefault; the port face is the item slot NBT + the
 * capability item handler), NO onRemove override (the BaseEntityBlock kill+recreate
 * lesson), no ACTIVE property (the W2 render card).
 */
public class GT6BatteryBoxBlock extends GTEntityBlock {

	/** Facing property (horizontal — FRONT is the output face, ALL-BUT-FRONT the input). */
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	/** The NBT_INV_SIZE column: 4 (the small box, :894) or 16 (the Large box, :895). */
	private final int mSlots;

	/** The tier ladder index (the VN ordinal — V[tier] rides mInput/mOutput, the NBT_INPUT/OUTPUT columns). */
	private final int mTier;

	/** The family BET (the GT6ElectricTransformers registry object supplier). */
	private final Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> mTickerType;

	/** The energy domain of the box (task p35: EU = the battery boxes, LU = the Crystal Chargers — the NBT_ENERGY_ACCEPTED/EMITTED columns). */
	private final Supplier<TagData> mEnergyType;

	public GT6BatteryBoxBlock(Properties aProperties, int aTier, int aSlots,
			Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> aTickerType) {
		this(aProperties, aTier, aSlots, aTickerType, () -> gregapi.data.TD.Energy.EU);
	}

	/** The typed constructor (task p35 — the Crystal Charger LU family). */
	public GT6BatteryBoxBlock(Properties aProperties, int aTier, int aSlots,
			Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> aTickerType, Supplier<TagData> aEnergyType) {
		super(aProperties);
		mTier = aTier;
		mSlots = aSlots;
		mTickerType = aTickerType;
		mEnergyType = aEnergyType;
		registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	/** The box's energy domain (the BE resolveEnergyType seat; lazy like every MT/TD read). */
	public Supplier<TagData> energyType() {
		return mEnergyType;
	}

	/** The block's own family BET supplier (the BE resolveBet seat — the charger blocks resolve THEIR BET, not the battery-box one). */
	public Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> betSupplier() {
		return mTickerType;
	}

	/** The family slot count (the BE reads it off its block state — the NBT_INV_SIZE column). */
	public int slots() {
		return mSlots;
	}

	/** The tier ladder index (the BE reads V[tier] off it — the NBT_INPUT/NBT_OUTPUT columns). */
	public int tier() {
		return mTier;
	}

	//? if neoforge {
	/*
	// 21.1 made BaseEntityBlock.codec() abstract — the GT6ElectricTransformerBlock
	// simpleCodec representative-value form (slot count 4, the family supplier dropped).
	@Override
	protected com.mojang.serialization.MapCodec<? extends GT6BatteryBoxBlock> codec() {
		return simpleCodec(aProperties -> new GT6BatteryBoxBlock(aProperties, 0, 4, () -> null));
	}
	*///?}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> aBuilder) {
		aBuilder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext aContext) {
		// the front TOWARDS the placer — the FRONT is the OUTPUT face here (the consumer
		// side; the GT6PlacementFacing canon)
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
		return tType == null ? null : tType.create(aPos, aState); // the GTEntityBlock form
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
		return RenderShape.MODEL;
	}
}
