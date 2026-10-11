package gregtech6.block.tools;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.minecraftforge.items.IItemHandler;

//? if neoforge {
/*import com.mojang.serialization.MapCodec;
 *///?}

import java.util.function.Supplier;

import gregapi.oredict.OreDictMaterial;
import gregtech6.block.GT6PlacementFacing;
import gregtech6.block.GTEntityBlock;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.tools.GT6AnvilBlockEntity;

/**
 * The anvil block carrier — the block side of the stone anvil family (task c-anvil),
 * the GTKitchenBlock carrier pattern: the block carries the registration values upstream
 * wrote into the MTE definition NBT — here the {@code NBT_MATERIAL} (the smash-target
 * scrap rain reads {@code mTargetSmashing.mMaterial} through it, MultiTileEntityAnvil
 * :128), the {@code NBT_DURABILITY} default (Stone 10000 / Blackstone 100000, Loader
 * :2185-2186) and the {@code NBT_HARDNESS}/{@code NBT_RESISTANCE} column (1.0/6.0 both
 * rows, aUtilStone → the STONE sound, :277-282 folds onto it) — plus the family BET this
 * member mounts (one shared BET over both rows, ADR-P3-1).
 *
 * <p>Upstream rows (Loader_MultiTileEntities.java:2185-2219, category "Misc Tool Blocks",
 * 35 rows — registered verbatim in {@link gregtech6.registry.GT6Anvils#ROWS}): the two stone carriers
 * <ul>
 * <li>Stone Anvil — MT.Stone, NBT_DURABILITY 10000, recipe "RRR","hR ","RRR" over stone;</li>
 * <li>Blackstone Anvil — MT.STONES.Blackstone, NBT_DURABILITY 100000, same shape over
 *     OP.stone.dat(Blackstone) (= the vanilla blackstone item).</li>
 * </ul>
 * plus the 33 material-ladder rows (:2187-2219 — task material-mc-e-tool-anvil-rows).
 * The BE/behaviour is material-blind except the two carrier values above — the ladder
 * rows differ in the registration NBT only.
 *
 * <p>{@code use} is the one-click manual-interaction face (the GTKitchenBlock form): the
 * whole upstream onBlockActivated3 (:218-274) + the onToolClick2 hammer strike (:94-141)
 * ride the BE's {@link GT6AnvilBlockEntity#activateChain} — which doubles as the RCON
 * acceptance channel (a null player = the empty-hand report path). The facing is the
 * {@link GT6PlacementFacing} canon (the front TOWARDS the placer — the upstream
 * 09FacingSingle SIDE_FRONT default, getValidSides = SIDES_HORIZONTAL :413).
 */
public class GTAnvilBlock extends GTEntityBlock {

	/** The horizontal facing (the upstream SIDES_VALID = SIDES_HORIZONTAL, :413). */
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	private final Supplier<OreDictMaterial> mMaterial;
	private final long mDurability;
	private final Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> mTickerType;

	/**
	 * @param aMaterial the upstream {@code NBT_MATERIAL} (resolved lazily — class-load
	 *        precedes MT.init); the smash-target scrap rain consumes its mTargetSmashing
	 * @param aDurability the upstream {@code NBT_DURABILITY} default (10000 / 100000)
	 * @param aTickerType the family BET (both rows share one)
	 */
	public GTAnvilBlock(Supplier<OreDictMaterial> aMaterial, long aDurability,
			Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> aTickerType,
			net.minecraft.world.level.block.state.BlockBehaviour.Properties aProperties) {
		super(aProperties);
		mMaterial = aMaterial;
		mDurability = aDurability;
		mTickerType = aTickerType;
		registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	//? if neoforge {
	/*// (1.21.1: BlockBehaviour.codec() is abstract — the GTBarrelBlock carrier precedent.)
	@Override
	protected MapCodec<GTAnvilBlock> codec() {
		return simpleCodec(aProperties -> new GTAnvilBlock(mMaterial, mDurability, mTickerType, aProperties));
	}
	 *///?}

	/** The upstream NBT_MATERIAL — the smash-target hop reads it (MultiTileEntityAnvil :128). */
	public OreDictMaterial material() {
		return mMaterial.get();
	}

	/** The upstream NBT_DURABILITY default (the BE's fresh-placement durability). */
	public long durability() {
		return mDurability;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> aBuilder) {
		aBuilder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext aContext) {
		// the placement canon: the front TOWARDS the placer (the 09FacingSingle SIDE_FRONT default)
		return defaultBlockState().setValue(FACING, GT6PlacementFacing.facingTowardsPlacer(aContext.getHorizontalDirection()));
	}

	@Override
	public void setPlacedBy(Level aLevel, BlockPos aPos, BlockState aState, @Nullable LivingEntity aPlacer, ItemStack aStack) {
		super.setPlacedBy(aLevel, aPos, aState, aPlacer, aStack);
		// keep the item-NBT durability with the placed stack (upstream writeItemNBT2 :82-85
		// carries it — the port item form is the fresh default; a worn-anvil ITEM universe
		// is the pool cut, so the BE keeps the carrier default)
	}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return mTickerType.get();
	}

	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL; // BaseEntityBlock default INVISIBLE is for BER blocks
	}

	/** The :397-398 box for the X-axis facings (EAST/WEST): x 4..12px waist, z 0..16px band, y 0..12px. */
	private static final VoxelShape SHAPE_FACING_X = Block.box(4, 0, 0, 12, 12, 16);
	/** The :397-398 box for the Z-axis facings (NORTH/SOUTH): the X/Z legs swap. */
	private static final VoxelShape SHAPE_FACING_Z = Block.box(0, 0, 4, 16, 12, 12);

	/**
	 * The upstream collision/selection box — ONE box for both ({@code
	 * getCollisionBoundingBoxFromPool} == {@code getSelectedBoundingBoxFromPool},
	 * MultiTileEntityAnvil.java:397-398; the setBlockBoundsBasedOnState :399 folds onto the
	 * same box), transcribed as two cached forms picked by the facing axis (the
	 * {@code SIDES_AXIS_X[mFacing]} leg): the long axis runs PERPENDICULAR to the facing
	 * (the anvil-shape-models rotation canon) — the facing axis carries the
	 * {@code PX_P[4]..PX_N[4]} = 4..12px waist, the cross axis the {@code PX_P[0]..PX_N[0]}
	 * = 0..16px full band, y = {@code PX_P[0]..PX_N[4]} = 0..12px. Symptom pinned here:
	 * without the override the collision box was the default FULL CUBE, disagreeing with
	 * the six-box model.
	 */
	private static VoxelShape shape(BlockState aState) {
		return aState.getValue(FACING).getAxis() == Direction.Axis.X ? SHAPE_FACING_X : SHAPE_FACING_Z;
	}

	@Override
	public VoxelShape getShape(BlockState aState, BlockGetter aLevel, BlockPos aPos, CollisionContext aContext) {
		return shape(aState);
	}

	@Override
	public VoxelShape getCollisionShape(BlockState aState, BlockGetter aLevel, BlockPos aPos, CollisionContext aContext) {
		return shape(aState);
	}

	/**
	 * The break drop contract (upstream canDrop :417 = T per slot) — the pops ride the
	 * {@link GTEntityBlock} fallback; this bridge hands over the anvil BE's differently
	 * spelled {@code inventory()} accessor (the {@code getInventory()} census default
	 * finds nothing here).
	 */
	@Override
	protected IItemHandler dropInventory(BlockEntity aTile) {
		return aTile instanceof GT6AnvilBlockEntity tAnvil ? tAnvil.inventory() : super.dropInventory(aTile);
	}

	//? if forge {
	@Override
	public InteractionResult use(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, InteractionHand aHand, BlockHitResult aHit) {
		if (aLevel.getBlockEntity(aPos) instanceof GT6AnvilBlockEntity tAnvil) {
			tAnvil.activateChain(aPlayer, (byte) aHit.getDirection().get3DDataValue(), aPlayer.getItemInHand(aHand),
					(float) (aHit.getLocation().x - aPos.getX()), (float) (aHit.getLocation().y - aPos.getY()),
					(float) (aHit.getLocation().z - aPos.getZ()));
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}
	//?} else {
	/*// (1.21.1: Block.use is gone — useWithoutItem receives both empty-hand and item-hand
	// right clicks exactly like the 1.20.1 use(); the hand-degradation deviation is the
	// GTBarrelBlock 1.21.1 form verbatim — MAIN_HAND stands in.)
	@Override
	protected InteractionResult useWithoutItem(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, BlockHitResult aHit) {
		if (aLevel.getBlockEntity(aPos) instanceof GT6AnvilBlockEntity tAnvil) {
			tAnvil.activateChain(aPlayer, (byte) aHit.getDirection().get3DDataValue(), aPlayer.getItemInHand(InteractionHand.MAIN_HAND),
					(float) (aHit.getLocation().x - aPos.getX()), (float) (aHit.getLocation().y - aPos.getY()),
					(float) (aHit.getLocation().z - aPos.getZ()));
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}
	 *///?}
}
