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
 * The small wall-attachment block (task p12-tap-funnel-attachment spec ①) — the 1.20.1
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
 *     the visual {@link #getShape} is the thin 4/16 plate over the attachment centre
 *     (the upstream render-pass-0 box, MultiTileEntityFluidTap.java:186-189 — the
 *     three-pass faucet/spout stack is the render pool card, one flat plate is the
 *     declared placeholder);</li>
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
		};

		public abstract boolean isValidMount(Direction aFacing);
	}

	private final Family mFamily;
	private final boolean mAcidProof;
	private final java.util.function.Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> mTickerType;

	public GTAttachmentSmallBlock(Family aFamily, boolean aAcidProof,
			java.util.function.Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> aTickerType, Properties aProperties) {
		super(aProperties);
		mFamily = aFamily;
		mAcidProof = aAcidProof;
		mTickerType = aTickerType;
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
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

	/** The thin plate over the attachment centre — the upstream render-pass-0 box (tap :186-189), the declared placeholder. */
	@Override
	public VoxelShape getShape(BlockState aState, BlockGetter aLevel, BlockPos aPos, CollisionContext aContext) {
		Direction tFacing = aState.getValue(FACING);
		return switch (tFacing) {
			case DOWN -> Block.box(6, 12, 6, 10, 16, 10); // the host is ABOVE (facing points at it), plate sits at the cell top
			case UP -> Block.box(6, 0, 6, 10, 4, 10);     // the host is BELOW
			case NORTH -> Block.box(6, 6, 0, 10, 10, 4);  // the host is NORTH, plate at the cell's north edge (tap :186 z 2..4)
			case SOUTH -> Block.box(6, 6, 12, 10, 10, 16);
			case WEST -> Block.box(0, 6, 6, 4, 10, 10);   // tap :188 x 2..4
			case EAST -> Block.box(12, 6, 6, 16, 10, 10);
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
	public InteractionResult use(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, InteractionHand aHand, BlockHitResult aHit) {
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
