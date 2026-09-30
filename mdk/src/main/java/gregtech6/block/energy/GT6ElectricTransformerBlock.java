package gregtech6.block.energy;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

import gregtech6.block.GTEntityBlock;
import gregtech6.items.tools.GT6ToolActions;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.util.UT6;

/**
 * The Electric Transformer block (task c-ulv-lv-transformer) — the facing-cube
 * carrier of {@link gregtech6.tileentity.energy.GT6ElectricTransformerBlockEntity},
 * the {@code GT6DynamoBlock} shape with the INPUT/OUTPUT sides swapped to the
 * transformer convention (Base11 :63-64): the FRONT face ({@code mFacing}) is the
 * INPUT face (normal mode — the high side, a 32 EU packet source plugs in here), ALL
 * OTHER faces are output (the low side fans out); the reversed mode swaps the sets.
 * Row :881 hardness/resistance 4.0/4.0 (the NBT_HARDNESS/NBT_RESISTANCE columns, the
 * dynamo family block properties).
 *
 * <p>Placement = the GT6PlacementFacing canon over the FULL view (issue #18, task
 * 18-converter-tex-facing): the front TOWARDS the placer, vertical when the look is
 * steep — upstream {@code getSideForPlayerPlacing} (UT.java:1755-1763) folds the pitch
 * into the side over SIDES_VALID = all six (CS.java:699, Base09 :92 does not narrow it
 * and Base10/Base11 do not override). The BE mirror re-syncs from the state each tick
 * (the transformer syncFacingFromState form — the state is the authority). The monkey
 * wrench on any face rotates the FRONT to the clicked sub-face ({@code use()}, the
 * upstream Base09 onToolClick2 :67 getSideWrenching semantics — the GTSensorBlock arm
 * shape; the Base11 :80-88 MODE flip stays the NBT/RCON channel, see the BE crop note).
 * The family BET rides a supplier (the GT6DynamoBlock ruling: resolved at call time,
 * never class-load). NO onRemove override (the BaseEntityBlock kill+recreate lesson),
 * no GUI (upstream has none); the ACTIVE property carries the {@code mActive} activity
 * for the overlay_active texture layer (upstream getTexture2 sOverlays[mActivity.mState]).
 */
public class GT6ElectricTransformerBlock extends GTEntityBlock {

	/** Facing property (six-way, issue #18 — FRONT is the input face, ALL-BUT-FRONT the output; upstream SIDES_VALID = all six). */
	public static final DirectionProperty FACING = BlockStateProperties.FACING;

	/** The activity visual property (the overlay layer selector; the BE drives it off {@code mActive}). */
	public static final net.minecraft.world.level.block.state.properties.BooleanProperty ACTIVE = gregtech6.block.GTBlockProperties.ACTIVE;

	/** The family BET (the GT6ElectricTransformers registry object supplier). */
	private final Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> mTickerType;

	/** The row's ladder index i (the V[i+1]→V[i] pair seat; the :881 row = 0, task p35). */
	private final int mTier;

	/**
	 * The block's upstream {@code NBT_MATERIAL} column (issue #18 — the tint colour
	 * source, the {@link GT6DynamoBlock} lazy-Supplier form): the row's Electric_T[i]
	 * casing material (the GT6ElectricTransformers.CASING_LADDER column, MT.java:3691).
	 */
	@Nullable
	private final Supplier<gregapi.oredict.OreDictMaterial> mMaterial;

	public GT6ElectricTransformerBlock(Properties aProperties,
			Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> aTickerType) {
		this(aProperties, aTickerType, 0);
	}

	/** The tiered constructor (task p35 — the :882-:889 ladder rows; the BatteryBox tier-column form). */
	public GT6ElectricTransformerBlock(Properties aProperties,
			Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> aTickerType, int aTier) {
		this(aProperties, aTickerType, aTier, null);
	}

	/** The material-carrier form (issue #18): the row feeds the tint colour source. */
	public GT6ElectricTransformerBlock(Properties aProperties,
			Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> aTickerType, int aTier,
			@Nullable Supplier<gregapi.oredict.OreDictMaterial> aMaterial) {
		super(aProperties);
		mTickerType = aTickerType;
		mTier = aTier;
		mMaterial = aMaterial;
		registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
	}

	/** The row's ladder index (the BE resolveTier seat; the STONE fallback in the BE answers 0). */
	public int tier() {
		return mTier;
	}

	/**
	 * The block's upstream {@code NBT_MATERIAL}, resolved lazily through the Supplier;
	 * null = the white no-tint identity (the offline codec representative).
	 */
	@Nullable
	public gregapi.oredict.OreDictMaterial material() {
		return mMaterial == null ? null : mMaterial.get();
	}

	/**
	 * The transformer material dispatch (issue #18, the {@link GT6DynamoBlock#materialOf}
	 * mirror shape): only the carrier blocks resolve a material — every other block is
	 * null here.
	 */
	@Nullable
	public static gregapi.oredict.OreDictMaterial materialOf(@Nullable net.minecraft.world.level.block.Block aBlock) {
		return aBlock instanceof GT6ElectricTransformerBlock tTransformer ? tTransformer.material() : null;
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
		aBuilder.add(FACING, ACTIVE);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext aContext) {
		// the front TOWARDS the placer over the full look (upstream getSideForPlayerPlacing
		// UT.java:1755-1763 — pitch folds the side vertical; the canon seam, Direction form)
		return defaultBlockState().setValue(FACING, gregtech6.block.GT6PlacementFacing.facingTowardsPlacer(aContext.getNearestLookingDirection()));
	}

	@Override
	//? if forge {
	public InteractionResult use(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, InteractionHand aHand, BlockHitResult aHit) {
	//?} else {
	/*public InteractionResult useWithoutItem(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, BlockHitResult aHit) {
	//21.1: BlockBehaviour.use folded into useWithoutItem (the GTOvenBlock fork).
	InteractionHand aHand = InteractionHand.MAIN_HAND;
	*///?}
		// the wrench arm (issue #18 — upstream Base09 onToolClick2 :67): the wrench-grid
		// sub-face pick sets the FRONT; all six sides valid (CS.java:699 SIDES_VALID, the
		// state is the authority — the BE tick mirror follows). The GTSensorBlock arm shape;
		// the Base11 MODE flip is NOT this arm (the NBT/RCON channel, the BE crop note).
		return wrenchRotate(aState, aLevel, aPos, aPlayer, aHand, aHit, FACING);
	}

	/**
	 * The shared converter-family wrench-rotate arm (the GTSensorBlock :123-145 shape): a
	 * monkey-wrench right-click re-faces the block to the clicked wrench-grid sub-face.
	 * Server-side only, no BE involvement — the state is the facing authority and the BE
	 * mirrors it on its next tick.
	 */
	static InteractionResult wrenchRotate(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, InteractionHand aHand, BlockHitResult aHit, DirectionProperty aFacing) {
		if (aLevel.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		ItemStack tHeld = aPlayer.getItemInHand(aHand);
		if (!tHeld.isEmpty() && tHeld.canPerformAction(GT6ToolActions.WRENCH)) {
			byte tTargetSide = UT6.getSideWrenching((byte) aHit.getDirection().get3DDataValue(),
					(float) (aHit.getLocation().x - aPos.getX()), (float) (aHit.getLocation().y - aPos.getY()),
					(float) (aHit.getLocation().z - aPos.getZ()));
			// all six sides valid (upstream SIDES_VALID; SIDE_INVALID 6 is unreachable for
			// a real face byte) — the vanilla same-block state write keeps the BE
			aLevel.setBlock(aPos, aState.setValue(aFacing, Direction.from3DDataValue(tTargetSide)), 3);
			return InteractionResult.CONSUME;
		}
		return InteractionResult.PASS;
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
