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
import net.minecraft.world.phys.BlockHitResult;
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
 * The Dynamo family block (task c-dynamo-family-be) — the facing-cube carrier shared
 * by BOTH dynamo families (Flux and Electric; one block class, ten registrations), upstream
 * MultiTileEntityDynamoFlux / MultiTileEntityDynamoElectric (aClass rows,
 * Loader_MultiTileEntities.java:945-957 — NBT_HARDNESS 4.0F, NBT_RESISTANCE 4.0F, stack
 * 16, no GUI: {@code canDrop} = F, TileEntityBase10EnergyConverter :161).
 *
 * <p>Facing = the placement orientation over the GT6PlacementFacing canon (issue #18,
 * task 18-converter-tex-facing): the FRONT face ({@code mFacing}) is the OUTPUT face
 * (upstream isOutput {@code mFacing == aSide}, DynamoFlux :37 — the dynamo is placed
 * facing its consumer), the BACK face the only input (upstream isInput
 * {@code mFacing == OPOS[aSide]}, :36 — the driven axle sits behind). SIX-WAY now —
 * upstream SIDES_VALID = all six (CS.java:699, Base09 :92) and Base10 does not narrow
 * it, so placement folds the look's pitch vertical ({@code getSideForPlayerPlacing}
 * UT.java:1755-1763) and the monkey wrench re-faces to the clicked sub-face (the
 * {@code GT6ElectricTransformerBlock.wrenchRotate} arm, Base09 onToolClick2 :67). The BE
 * mirror re-syncs from the state each tick (the transformer syncFacingFromState form —
 * the {@code /setblock gt6:flux_dynamo[facing=...]} RCON path, the state is the
 * authority); the ACTIVE property carries {@code mActive} for the overlay_active layer.
 *
 * <p>The row index ({@link #tier}) selects the family ladder column (the GTOvenBlock
 * "block identity IS the config selector" ruling): the Flux rows index
 * {@code MT.FLUX_T[1..5]}, the Electric rows the Electric_T[1..5] word set
 * (Loader :946-957). The family BET rides a supplier (the transformer returned its
 * RegistryObject directly; this shared class cannot name one family, so the registrations
 * hand their own in — resolved at call time, never class-load, the GTWireSpecs:35 ruling).
 *
 * <p>No GUI (upstream has none), NO onRemove override (the BaseEntityBlock kill+recreate
 * lesson, remember id59); the activity visual rides the ACTIVE property (issue #18 —
 * the upstream trinary stays collapsed to the {@code mActive} flag, the blinking state
 * remains the defer).
 */
public class GT6DynamoBlock extends GTEntityBlock {

	/** Facing property (six-way, issue #18 — FRONT is the output face, BACK the input; upstream SIDES_VALID = all six). */
	public static final DirectionProperty FACING = BlockStateProperties.FACING;

	/** The activity visual property (the overlay_active layer selector; the BE drives it off {@code mActive}). */
	public static final net.minecraft.world.level.block.state.properties.BooleanProperty ACTIVE = gregtech6.block.GTBlockProperties.ACTIVE;

	/** The row index of this block in its family ladder (0 = T1 .. 4 = T5). */
	private final int mTier;

	/** The family BET (the GT6FluxDynamos/GT6ElectricDynamos registry object supplier). */
	private final Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> mTickerType;

	/**
	 * The block's upstream {@code NBT_MATERIAL} column (task c2-controller-tint — the
	 * tint colour source, the lazy-Supplier GTBarrels form): the EU-bridge and laser
	 * families hand their Electric_T rung material in. Null = the material-less
	 * registrations (the Flux/Electric dynamos, the quantum energizer) — the white
	 * no-tint identity.
	 */
	@Nullable
	private final Supplier<gregapi.oredict.OreDictMaterial> mMaterial;

	public GT6DynamoBlock(Properties aProperties, int aTier,
			Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> aTickerType) {
		this(aProperties, aTier, aTickerType, null);
	}

	/** The material-carrier form (task c2-controller-tint): the row feeds the tint colour source. */
	public GT6DynamoBlock(Properties aProperties, int aTier,
			Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> aTickerType,
			@Nullable Supplier<gregapi.oredict.OreDictMaterial> aMaterial) {
		super(aProperties);
		mTier = aTier;
		mTickerType = aTickerType;
		mMaterial = aMaterial;
		registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
	}

	/**
	 * The block's upstream {@code NBT_MATERIAL}, resolved lazily through the Supplier;
	 * null = the material-less registrations (the white identity, upstream UNCOLORED
	 * CS.java:327).
	 */
	@Nullable
	public gregapi.oredict.OreDictMaterial material() {
		return mMaterial == null ? null : mMaterial.get();
	}

	/**
	 * The dynamo-carrier material dispatch (task c2-controller-tint, the
	 * {@code GTMultiBlockPartBlock.materialOf} mirror shape): only the carrier blocks
	 * resolve a material — every other block is null here.
	 */
	@Nullable
	public static gregapi.oredict.OreDictMaterial materialOf(@Nullable net.minecraft.world.level.block.Block aBlock) {
		return aBlock instanceof GT6DynamoBlock tDynamo ? tDynamo.material() : null;
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
		aBuilder.add(FACING, ACTIVE);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext aContext) {
		// the front TOWARDS the placer over the full look — you stand on the consumer
		// side, the driven axle sits behind (the canon, the vertical fold per issue #18)
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
		// the wrench arm (issue #18 — upstream Base09 onToolClick2 :67): the clicked
		// wrench-grid sub-face becomes the FRONT (the output face); the state is the
		// authority, the BE tick mirror follows
		return GT6ElectricTransformerBlock.wrenchRotate(aState, aLevel, aPos, aPlayer, aHand, aHit, FACING);
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
