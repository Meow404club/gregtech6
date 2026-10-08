package gregtech6.block.attachment;

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
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Supplier;

import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.attachment.GTAttachmentSmallBlockEntity;

/**
 * The small wall-attachment block (task tap-funnel-attachment spec ①) — the 1.20.1
 * block side of {@code TileEntityBase11AttachmentSmall}: a thin plate MOUNTED ON one
 * face of a sturdy neighbour, carrying the facing as the blockstate ({@code FACING},
 * six directions — the attachment looks AT its host, {@code mFacing} == clicked face)
 * and re-syncing the BE mirror at the tick head (the crank pattern).
 *
 * <p>Mount semantics:
 * <ul>
 * <li>{@link #canSurvive} — the OPPOSITE side of the faced neighbour must be sturdy
 *     ("对面须可贴", the upstream placement obstruction face —
 *     {@code isSurfaceSolid/isSurfaceOpaque2} of the host, TileEntityBase10Attachment
 *     surface defaults); breaking the host unmounts the attachment through
 *     {@link #updateShape} → air (the vanilla torch idiom, the upstream
 *     {@code canDrop} semantics);</li>
 * <li>{@link #getStateForPlacement} — the clicked face becomes the facing;</li>
 * <li>{@code ignorePlayerCollisionWhenPlacing} (11AttachmentSmall :29-30) — FREE in
 *     1.20.1: {@code BlockItem.place → canPlace} checks {@code canSurvive} only and
 *     never intersects the player AABB, no port needed (declared in the class doc
 *     because the upstream interface name is the spec line);</li>
 * <li>{@link #getCollisionShape} → empty (upstream
 *     {@code getCollisionBoundingBoxFromPool() == null}, TileEntityBase10Attachment:35);
 *     the selection {@link #getShape} is the upstream taper/spout stack envelope
 *     (MultiTileEntityFluidTap :226-233 / FluidFunnel :156-163) — since task
 *     rod-render-pool the VISUAL is the upstream three-pass stack too (the datagen
 *     element models in GT6BlockStates.addAttachments), the per-face top/bottom art
 *     borrow still declared (the borrowed side icons texture every face);</li>
 * <li>valid mount faces per family: the tap takes the four horizontals (the 10Attachment
 *     default {@code SIDES_HORIZONTAL}, TileEntityBase10Attachment.java:51 — the tap
 *     class carries no override), the funnel the horizontals + DOWN
 *     ({@code SIDES_BOTTOM_HORIZONTAL}, MultiTileEntityFluidFunnel.java:168).</li>
 * </ul>
 *
 * <p>NO onRemove override (the red line): the unmount is {@link #updateShape}, the BE
 * lifecycle is vanilla's.
 */
public class GTAttachmentSmallBlock extends Block implements EntityBlock {

	public static final DirectionProperty FACING = BlockStateProperties.FACING;

	/** The mount-validity per family (the upstream getValidSides sets). */
	public enum Family {
		/** The fluid tap — four horizontals (TileEntityBase10Attachment.java:51 default, no tap override). */
		TAP {
			@Override
			public boolean isValidMount(Direction aFacing) {
				return aFacing.getAxis() != Direction.Axis.Y;
			}
		},
		/** The fluid funnel — horizontals + down (MultiTileEntityFluidFunnel.java:168 SIDES_BOTTOM_HORIZONTAL). */
		FUNNEL {
			@Override
			public boolean isValidMount(Direction aFacing) {
				return aFacing != Direction.UP;
			}
		},
		/** The fluid nozzle (task material-mc-f-attachment-rows) — four horizontals (MultiTileEntityFluidNozzle carries no getValidSides override, the 10Attachment:51 default like the tap). */
		NOZZLE {
			@Override
			public boolean isValidMount(Direction aFacing) {
				return aFacing.getAxis() != Direction.Axis.Y;
			}
		},
		/** The fluid cap nozzle — four horizontals (MultiTileEntityFluidCapNozzle carries no getValidSides override, the 10Attachment:51 default like the tap). */
		CAP_NOZZLE {
			@Override
			public boolean isValidMount(Direction aFacing) {
				return aFacing.getAxis() != Direction.Axis.Y;
			}
		};

		public abstract boolean isValidMount(Direction aFacing);
	}

	private final Family mFamily;
	private final boolean mAcidProof;
	/** The carried registration row (task i18n-compose-rows) — feeds the composed tap/funnel name. */
	private final gregtech6.registry.GT6Attachments.AttachmentRow mRow;
	private final java.util.function.Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> mTickerType;

	public GTAttachmentSmallBlock(gregtech6.registry.GT6Attachments.AttachmentRow aRow, boolean aAcidProof,
			java.util.function.Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> aTickerType, Properties aProperties) {
		super(aProperties);
		mFamily = aRow.family();
		mAcidProof = aAcidProof;
		mRow = aRow;
		mTickerType = aTickerType;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	/** The registration row (the GTBarrelBlock registration-carrier pattern). */
	public gregtech6.registry.GT6Attachments.AttachmentRow row() {
		return mRow;
	}

	/**
	 * The composed attachment name (task i18n-compose-rows): the family template over
	 * the gt6.row.attachment.mat small unit (the four-family seam, task
	 * material-mc-f-attachment-rows — nozzle/cap-nozzle compose the same way).
	 */
	@Override
	public net.minecraft.network.chat.MutableComponent getName() {
		return gregtech6.registry.GT6Attachments.displayOf(mRow);
	}

	public Family family() {
		return mFamily;
	}

	/** The registration row's NBT_ACIDPROOF (Loader_MultiTileEntities.java:2108-2120, the block-carrier pattern). */
	public boolean acidProof() {
		return mAcidProof;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> aBuilder) {
		aBuilder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext aContext) {
		// the clicked face is the mount: the attachment looks AT the host (mFacing == clicked face)
		return defaultBlockState().setValue(FACING, aContext.getClickedFace());
	}

	@Override
	public boolean canSurvive(BlockState aState, LevelReader aLevel, BlockPos aPos) {
		Direction tFacing = aState.getValue(FACING);
		if (!mFamily.isValidMount(tFacing)) return false;
		BlockPos tAttached = aPos.relative(tFacing);
		return aLevel.getBlockState(tAttached).isFaceSturdy(aLevel, tAttached, tFacing.getOpposite());
	}

	@Override
	public BlockState updateShape(BlockState aState, Direction aDirection, BlockState aNeighborState,
			LevelAccessor aLevel, BlockPos aPos, BlockPos aNeighborPos) {
		// the host broke → unmount (the torch idiom; canSurvive re-checks family validity too)
		if (aDirection == aState.getValue(FACING) && !aState.canSurvive(aLevel, aPos)) {
			return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
		}
		return super.updateShape(aState, aDirection, aNeighborState, aLevel, aPos, aNeighborPos);
	}

	/** Upstream TileEntityBase10Attachment:35 — attachments block nothing. */
	@Override
	public VoxelShape getCollisionShape(BlockState aState, BlockGetter aLevel, BlockPos aPos, CollisionContext aContext) {
		return Shapes.empty();
	}

	/**
	 * The upstream selection envelopes (task rod-render-pool) — the tap
	 * getSelectedBoundingBoxFromPool (MultiTileEntityFluidTap.java:226-233) and the
	 * funnel (MultiTileEntityFluidFunnel.java:156-163) verbatim: the full taper/spout
	 * stack envelope against the host edge (FACING points AT the host). The nozzle and
	 * cap-nozzle families (task material-mc-f-attachment-rows) share the tap envelope —
	 * their getSelectedBoundingBoxFromPool (MultiTileEntityFluidNozzle.java:172-178 /
	 * MultiTileEntityFluidCapNozzle.java:136-142) projects to the same
	 * (6,3,0)-(10,7,6) north box. The previous
	 * 4x4x4 placeholder predates the render geometry landing; collision stays
	 * {@link #getCollisionShape empty} (upstream 10Attachment:35).
	 */
	@Override
	public VoxelShape getShape(BlockState aState, BlockGetter aLevel, BlockPos aPos, CollisionContext aContext) {
		Direction tFacing = aState.getValue(FACING);
		if (mFamily != Family.FUNNEL) {
			// MultiTileEntityFluidTap :226-233 (PX_P/PX_N px: (6,3,0)-(10,7,6) on north) —
			// verbatim shared by the tap/nozzle/cap-nozzle trio (the nozzle pair's own
			// projection is byte-equal, see the javadoc)
			return switch (tFacing) {
				case NORTH -> Block.box(6, 3, 0, 10, 7, 6);
				case SOUTH -> Block.box(6, 3, 10, 10, 7, 16);
				case WEST -> Block.box(0, 3, 6, 6, 7, 10);
				default -> Block.box(10, 3, 6, 16, 7, 10); // EAST (+ the unreachable verticals)
			};
		}
		// MultiTileEntityFluidFunnel :156-163 (px: (5,7,0)-(11,10,6) north / (5,0,5)-(11,3,11) down)
		return switch (tFacing) {
			case NORTH -> Block.box(5, 7, 0, 11, 10, 6);
			case SOUTH -> Block.box(5, 7, 10, 11, 10, 16);
			case WEST -> Block.box(0, 7, 5, 6, 10, 11);
			case EAST -> Block.box(10, 7, 5, 16, 10, 11);
			case DOWN -> Block.box(5, 0, 5, 11, 3, 11); // the under-host mount (:163 default)
			default -> Block.box(5, 7, 0, 11, 10, 6);   // UP is family-invalid, never placed
		};
	}

	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL; // BaseEntityBlock default INVISIBLE is for BER blocks
	}

	/** The BET this block mounts (the GTEntityBlock field form; Block itself carries no such override). */
	public BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return mTickerType.get();
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos aPos, BlockState aState) {
		return tickerType().create(aPos, aState);
	}

	/**
	 * The attachment activation face (the upstream {@code onBlockActivated3} :77/:68 —
	 * server branch only, always consumed): the BE runs the tap/funnel chain with the
	 * player's MAIN HAND (the upstream {@code getCurrentEquippedItem}).
	 */
	@Override
	//? if forge {
	public InteractionResult use(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, InteractionHand aHand, BlockHitResult aHit) {
	//?} else {
	/*public InteractionResult useWithoutItem(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, BlockHitResult aHit) {
	//21.1: BlockBehaviour.use folded into useWithoutItem — the InteractionHand param dropped
	//(javap BlockBehaviour 21.1.249); the game loop drives MAIN_HAND first.
	InteractionHand aHand = InteractionHand.MAIN_HAND;
	*///?}
		if (aLevel.isClientSide()) return InteractionResult.SUCCESS;
		if (aLevel.getBlockEntity(aPos) instanceof GTAttachmentSmallBlockEntity tAttachment) {
			tAttachment.onPlayerUse(aPlayer, (byte)aHit.getDirection().get3DDataValue());
			return InteractionResult.CONSUME;
		}
		return InteractionResult.PASS;
	}

	/** The BE facing mirror (the crank setPlacedBy form): placement writes the state, the mirror re-syncs at the tick head. */
	@Override
	public void setPlacedBy(Level aLevel, BlockPos aPos, BlockState aState, @Nullable LivingEntity aPlacer, ItemStack aStack) {
		super.setPlacedBy(aLevel, aPos, aState, aPlacer, aStack);
		if (aLevel.getBlockEntity(aPos) instanceof GTAttachmentSmallBlockEntity tAttachment) {
			tAttachment.mFacing = (byte)aState.getValue(FACING).get3DDataValue();
			tAttachment.setChanged();
		}
	}

	/** Safe cast helper for the command/RCON surfaces. */
	public @Nullable GTAttachmentSmallBlockEntity attachment(@Nullable BlockEntity aBE) {
		return aBE instanceof GTAttachmentSmallBlockEntity tAttachment ? tAttachment : null;
	}
}
