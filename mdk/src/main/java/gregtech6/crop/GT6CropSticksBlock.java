package gregtech6.crop;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

//? if neoforge {
/*import com.mojang.serialization.MapCodec;
 *///?}

import gregtech6.registry.GT6CropSticks;


/**
 * The crop-stick block  --  the self-owned counterpart of the IC2 blockCrop (GT6 1.7.10 had NO
 * own block; the whole system parasited IC2's, research.crop-breeding q1). Two blockstate
 * faces keyed on {@link #CROSSING}: the single empty stick and the crossingBase double stick
 * (the second stick is right-clicked on, TileEntityCrop.rightClick :414-422); the planted
 * crop's visual stage stays BE-carried (the per-crop/per-size render face is cbc-3's asset
 * census + render dispatch, not a blockstate axis here).
 *
 * <p>Block faces, each the decompiled anchor: zero collision box + the -1px selection box
 * clamped to y0 (getAabbs :399-408  --  the clamp declared), light opacity 0 (:900), PLANT sound
 * (:387), instant break, farmland survival with the pick-then-vanish neighbor reaction
 * (onNeighborChange :1189-1196 verbatim: sticks do NOT drop, seeds do), left-click
 * pick/downgrade (onClicked :376-384), break drops the stick count via {@link #getDrops} +
 * the seeds via the BE {@code pick} (onBlockBreak :392-396), trample through
 * {@code entityInside} (onEntityCollision :485-499  --  1.20.1 fires it for every AABB-overlapped
 * non-air block, Entity.checkInsideBlocks :966-989, so the zero-collision box keeps the walk-
 * through feel AND the trample roll, the GTWireBlock precedent).
 */
public class GT6CropSticksBlock extends BaseEntityBlock {

	/** The crossingBase twin-stick face (the renderState crosscrop :71, the blockstate carrier). */
	public static final BooleanProperty CROSSING = BooleanProperty.create("crossing");

	/** The selection envelope :404  --  box(0.2, -0.0625, 0.2  ->  0.8, 0.85, 0.8), the -1px base clamped to 0 (declared). */
	private static final VoxelShape SHAPE = Block.box(3.2, 0.0, 3.2, 12.8, 13.6, 12.8);

	public GT6CropSticksBlock(Properties aProperties) {
		super(aProperties);
		registerDefaultState(getStateDefinition().any().setValue(CROSSING, false));
	}

	//? if neoforge {
	/*// 1.21.1: BlockBehaviour.codec() is abstract (the GTBarrelBlock carrier precedent).
	@Override
	protected MapCodec<GT6CropSticksBlock> codec() {
		return simpleCodec(GT6CropSticksBlock::new);
	}
	 *///?}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> aBuilder) {
		aBuilder.add(CROSSING);
	}

	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL; // BaseEntityBlock default INVISIBLE is for BER blocks
	}

	@Override
	public VoxelShape getShape(BlockState aState, BlockGetter aLevel, BlockPos aPos, CollisionContext aContext) {
		return SHAPE;
	}

	//? if forge {
	@Override
	public InteractionResult use(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, InteractionHand aHand, BlockHitResult aHit) {
		return useCore(aState, aLevel, aPos, aPlayer, aPlayer.getItemInHand(aHand));
	}
	//?} else {
	/*// 1.21.1: Block.use is gone and the default useItemOn routes every click through
	// PASS_TO_DEFAULT_BLOCK_INTERACTION, so useWithoutItem receives both empty-hand and
	// item-hand right clicks exactly like the 1.20.1 use() (the GTBarrelBlock 1.21.1 form;
	// MAIN_HAND stands in -- the declared deviation).
	@Override
	protected InteractionResult useWithoutItem(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, BlockHitResult aHit) {
		return useCore(aState, aLevel, aPos, aPlayer, aPlayer.getItemInHand(InteractionHand.MAIN_HAND));
	}
	 *///?}

	/** The shared interaction body  --  the rightClick :410-462 delegation over the BE. */
	private InteractionResult useCore(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, ItemStack aHeld) {
		if (aLevel.getBlockEntity(aPos) instanceof GT6CropBlockEntity tCrop) {
			if (!aLevel.isClientSide) {
				tCrop.rightClick(aLevel, aPos, aHeld.copy(), aPlayer.getAbilities().instabuild);
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	/** The left-click face  --  onClicked :376-384 (pick / crossing downgrade + stick spill). */
	@Override
	public void attack(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer) {
		if (!aLevel.isClientSide && aLevel.getBlockEntity(aPos) instanceof GT6CropBlockEntity tCrop) {
			tCrop.leftClick(aLevel, aPos);
		}
	}

	/** The trample face  --  the zero-collision box keeps this a walk-through (the class doc). */
	@Override
	public void entityInside(BlockState aState, Level aLevel, BlockPos aPos, Entity aEntity) {
		if (!aLevel.isClientSide && aLevel.getBlockEntity(aPos) instanceof GT6CropBlockEntity tCrop) {
			tCrop.onEntityCollision(aLevel, aPos, aEntity);
		}
	}

	/** :1192  --  the farmland survival gate. */
	@Override
	public boolean canSurvive(BlockState aState, LevelReader aLevel, BlockPos aPos) {
		return aLevel.getBlockState(aPos.below()).is(net.minecraft.world.level.block.Blocks.FARMLAND);
	}

	/** onPlace + neighborChanged = the :1189-1196 pick-then-vanish (sticks do NOT drop). */
	@Override
	public void onPlace(BlockState aState, Level aLevel, BlockPos aPos, BlockState aOldState, boolean aIsMoving) {
		super.onPlace(aState, aLevel, aPos, aOldState, aIsMoving);
		vanishWithoutSupport(aLevel, aPos);
	}

	@Override
	public void neighborChanged(BlockState aState, Level aLevel, BlockPos aPos, Block aBlock, BlockPos aFromPos, boolean aIsMoving) {
		super.neighborChanged(aState, aLevel, aPos, aBlock, aFromPos, aIsMoving);
		vanishWithoutSupport(aLevel, aPos);
	}

	private void vanishWithoutSupport(Level aLevel, BlockPos aPos) {
		if (!aLevel.isClientSide && !canSurvive(aLevel.getBlockState(aPos), aLevel, aPos)) {
			if (aLevel.getBlockEntity(aPos) instanceof GT6CropBlockEntity tCrop) {
				tCrop.pick(aLevel, aPos); // the seeds fall, the sticks vanish (:1193-1194)
			}
			aLevel.removeBlock(aPos, false);
		}
	}

	/** The stick count drop  --  1 single, 2 crossing (the blockCrop loot face; seeds ride the BE pick). */
	@Override
	public List<ItemStack> getDrops(BlockState aState, net.minecraft.world.level.storage.loot.LootParams.Builder aBuilder) {
		return List.of(GT6CropBlockEntity.stickStackWithCount(aState.getValue(CROSSING) ? 2 : 1));
	}

	/** The seed spill on any real removal  --  onBlockBreak :392-396. */
	@Override
	public void onRemove(BlockState aOldState, Level aLevel, BlockPos aPos, BlockState aNewState, boolean aIsMoving) {
		if (!aOldState.is(aNewState.getBlock()) && !aLevel.isClientSide
				&& aLevel.getBlockEntity(aPos) instanceof GT6CropBlockEntity tCrop && tCrop.getCrop() != null) {
			tCrop.pick(aLevel, aPos);
		}
		super.onRemove(aOldState, aLevel, aPos, aNewState, aIsMoving);
	}

	// -------------------------------------------------------------- the BE wiring

	@Override
	@Nullable
	public BlockEntity newBlockEntity(BlockPos aPos, BlockState aState) {
		return GT6CropSticks.CROP_STICKS_BE.get().create(aPos, aState);
	}

	@Override
	@Nullable
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level aLevel, BlockState aState, BlockEntityType<T> aType) {
		if (aType != GT6CropSticks.CROP_STICKS_BE.get()) return null;
		return (aTickerLevel, aPos, aTickerState, aTile) ->
				GT6CropBlockEntity.tick(aTickerLevel, aPos, aTickerState, (GT6CropBlockEntity) aTile);
	}

	/** The registration-side properties (instant break, walk-through, no occlusion). */
	public static net.minecraft.world.level.block.state.BlockBehaviour.Properties cropProperties() {
		return net.minecraft.world.level.block.state.BlockBehaviour.Properties.of()
				.instabreak()
				.sound(SoundType.GRASS) // the PLANT sound group (getBlockSound :387-389 -- 1.20.1 mojmap spells it GRASS)
				.noCollission() // the zero collision box :401-402
				.noOcclusion()
				.isViewBlocking((aState, aLevel, aPos) -> false)
				.lightLevel(aState -> 0); // getLightOpacity :900-902
	}
}
