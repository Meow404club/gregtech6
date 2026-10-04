package gregtech6.block.tools;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import gregtech6.block.GT6PlacementFacing;
import gregtech6.block.GTEntityBlock;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.tools.GT6GrindstoneBlockEntity;

/**
 * The Grindstone block carrier (task grindstone-family) — the GTAnvilBlock carrier
 * pattern: the block carries the :2226 registration row (NBT_MATERIAL ANY.Steel +
 * NBT_HARDNESS 1.0 / NBT_RESISTANCE 6.0 through the GT6Grindstones properties, the
 * aUtilMetal METAL sound) and the family BET (single mount — :2226 is single-variant).
 *
 * <h2>The STONE state = the upstream mStone visual face</h2>
 * Upstream keeps the abrasive count in the BE ({@code NBT_STATE}, readFromNBT2 :60) and
 * pushes it to the RENDER through the visual-data channel ({@code setVisualData :195},
 * the {@code onTickCheck mStone != oStone :184-186} re-sync). The port's single source of
 * truth IS the blockstate: {@link #STONE} (0..16 — the upstream {@code mStone--} walks
 * every value, not just the load steps), written on every change through
 * {@link #setStone} — the blockstate update is the client visual sync (the furnace-LIT
 * form). The BE carries only the transient combo counter + last-recipe cache, so the
 * placed-block keeps its abrasive through the ITEM side instead: the break drop writes
 * {@code gt.toolstate} (the upstream writeItemNBT2 :71-74 face, the GT6CellBlock
 * getDrops seam) and placement reads it back in getStateForPlacement — 放块保留磨料.
 *
 * <p>{@code use} is the one-click manual-interaction face (the GTAnvilBlock form): the
 * whole upstream onBlockActivated3 (:88-181) rides the BE's
 * {@link GT6GrindstoneBlockEntity#activateChain} — which doubles as the RCON acceptance
 * channel (a null player = the empty-hand report path). The facing is the
 * {@link GT6PlacementFacing} canon (the front TOWARDS the placer — the upstream
 * 09FacingSingle SIDE_FRONT default, getValidSides = SIDES_HORIZONTAL :201); the valid
 * interaction faces are the TOP and the FRONT (the :89 {@code aSide != mFacing &&
 * !SIDES_TOP[aSide]} gate — everything else passes through, the upstream return F).
 */
public class GT6GrindstoneBlock extends GTEntityBlock {

	/** The horizontal facing (the upstream SIDES_VALID = SIDES_HORIZONTAL, :201). */
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	/**
	 * The abrasive count (the upstream mStone: 0 = empty, the sand ladder loads
	 * 16/16/16/8/4/sandstone 8, each sharpening walk decrements by 1 — MultiTileEntityGrindStone
	 * .java:99-161). Seventeen values ride the blockstate (the visual data the 1.7.10 port
	 * pushed through setVisualData).
	 */
	public static final IntegerProperty STONE = IntegerProperty.create("stone", 0, 16);

	/** The item-NBT key the break drop / placement pair rides (the upstream NBT_STATE face). */
	public static final String NBT_TOOLSTATE = "gt.toolstate";

	/** The upstream collision + selection box (getCollisionBoundingBoxFromPool/getSelectedBoundingBoxFromPool :253-254: PX_P[2]..PX_N[2], Y 0..PX_N[1]). */
	private static final VoxelShape SHAPE = net.minecraft.world.phys.shapes.Shapes.box(
			2.0 / 16.0, 0.0, 2.0 / 16.0, 14.0 / 16.0, 15.0 / 16.0, 14.0 / 16.0);

	private final java.util.function.Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> mTickerType;

	/**
	 * @param aTickerType the family BET (the GTAnvilBlock supplier form — the RegistryObject
	 *        resolves at registration time, after the block ctor)
	 */
	public GT6GrindstoneBlock(java.util.function.Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> aTickerType,
			net.minecraft.world.level.block.state.BlockBehaviour.Properties aProperties) {
		super(aProperties);
		mTickerType = aTickerType;
		registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(STONE, 0));
	}

	//? if neoforge {
	/*// (1.21.1: BlockBehaviour.codec() is abstract — the GTBarrelBlock carrier precedent.)
	@Override
	protected com.mojang.serialization.MapCodec<GT6GrindstoneBlock> codec() {
		return simpleCodec(aProperties -> new GT6GrindstoneBlock(
				() -> gregtech6.registry.GT6Grindstones.GRINDSTONE_BE.get(), aProperties));
	}
	 *///?}

	/** The upstream NBT_MATERIAL row (ANY.Steel) — the paint dispatch's row colour face. */
	public static gregapi.oredict.OreDictMaterial materialOf(@Nullable net.minecraft.world.level.block.Block aBlock) {
		return aBlock instanceof GT6GrindstoneBlock ? gregapi.data.ANY.Steel : null;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> aBuilder) {
		aBuilder.add(FACING, STONE);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext aContext) {
		// the placement canon: the front TOWARDS the placer (the 09FacingSingle SIDE_FRONT
		// default) + the item's abrasive back (the writeItemNBT2 round trip — 放块保留磨料)
		byte tStone = 0;
		//? if forge {
		CompoundTag tTag = aContext.getItemInHand().getTag();
		if (tTag != null && tTag.contains(NBT_TOOLSTATE)) tStone = (byte) Math.min(16, Math.max(0, tTag.getByte(NBT_TOOLSTATE)));
		//?} else {
		/*net.minecraft.world.item.component.CustomData tData =
				aContext.getItemInHand().get(gregtech6.registry.GT6DataComponents.TOOLSTATE);
		if (tData != null && tData.copyTag().contains(NBT_TOOLSTATE))
				tStone = (byte) Math.min(16, Math.max(0, tData.copyTag().getByte(NBT_TOOLSTATE)));
		 *///?}
		return defaultBlockState().setValue(FACING, GT6PlacementFacing.facingTowardsPlacer(aContext.getHorizontalDirection()))
				.setValue(STONE, (int) tStone);
	}

	@Override
	public void setPlacedBy(Level aLevel, BlockPos aPos, BlockState aState, @Nullable LivingEntity aPlacer, ItemStack aStack) {
		super.setPlacedBy(aLevel, aPos, aState, aPlacer, aStack);
		// the BE reads its abrasive off the placed state (the ctor) — nothing to copy here
	}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return mTickerType.get();
	}

	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL; // BaseEntityBlock default INVISIBLE is for BER blocks
	}

	/** The upstream collision + selection box (:253-255, the PX_P[2]..PX_N[2] floor slab). */
	@Override
	public VoxelShape getShape(BlockState aState, BlockGetter aLevel, BlockPos aPos, CollisionContext aContext) {
		return SHAPE;
	}

	/**
	 * The break drop — the upstream writeItemNBT2 (:71-74) item face through the
	 * GT6CellBlock.getDrops seam: exactly the self stack, carrying {@code gt.toolstate}
	 * when the abrasive is in (the empty grindstone drops tag-less and stacks clean).
	 */
	@Override
	public List<ItemStack> getDrops(BlockState aState, LootParams.Builder aBuilder) {
		BlockEntity tBE = aBuilder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
		int tStone = aState.hasProperty(STONE) ? aState.getValue(STONE) : 0;
		if (tBE instanceof GT6GrindstoneBlockEntity tGrindstone) tStone = tGrindstone.stone();
		ItemStack tStack = new ItemStack(this.asItem());
		if (tStone > 0) {
			CompoundTag tTag = new CompoundTag();
			tTag.putByte(NBT_TOOLSTATE, (byte) tStone);
			//? if forge {
			tStack.setTag(tTag);
			//?} else {
			/*net.minecraft.world.item.component.CustomData.set(gregtech6.registry.GT6DataComponents.TOOLSTATE, tStack, tTag);
			 *///?}
		}
		return List.of(tStack);
	}

	/**
	 * The mStone write-through — the BE's single visual-sync arm (the upstream
	 * setVisualData/onTickCheck pair :184-197): one blockstate update carries the new
	 * abrasive to every client. No-op without a level (the offline report channel).
	 */
	public void setStone(Level aLevel, BlockPos aPos, BlockState aState, int aStone) {
		if (aLevel != null && aStone != aState.getValue(STONE)) {
			aLevel.setBlock(aPos, aState.setValue(STONE, aStone), 3);
		}
	}

	/** The placed-state abrasive read (the BE ctor's carrier default, the anvil form). */
	public static int stoneOf(BlockState aState) {
		return aState.hasProperty(STONE) ? aState.getValue(STONE) : 0;
	}

	//? if forge {
	@Override
	public InteractionResult use(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, InteractionHand aHand, BlockHitResult aHit) {
		if (aLevel.getBlockEntity(aPos) instanceof GT6GrindstoneBlockEntity tGrindstone) {
			Direction tFace = aHit.getDirection();
			// :89 — only the TOP and the FRONT face interact; everything else passes through
			if (tFace != Direction.UP && tFace != aState.getValue(FACING)) return InteractionResult.PASS;
			tGrindstone.activateChain(aPlayer, (byte) tFace.get3DDataValue(), aPlayer.getItemInHand(aHand),
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
		if (aLevel.getBlockEntity(aPos) instanceof GT6GrindstoneBlockEntity tGrindstone) {
			Direction tFace = aHit.getDirection();
			// :89 — only the TOP and the FRONT face interact; everything else passes through
			if (tFace != Direction.UP && tFace != aState.getValue(FACING)) return InteractionResult.PASS;
			tGrindstone.activateChain(aPlayer, (byte) tFace.get3DDataValue(), aPlayer.getItemInHand(InteractionHand.MAIN_HAND),
					(float) (aHit.getLocation().x - aPos.getX()), (float) (aHit.getLocation().y - aPos.getY()),
					(float) (aHit.getLocation().z - aPos.getZ()));
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}
	 *///?}
}
