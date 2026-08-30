package gregtech6.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

import net.minecraftforge.common.ToolActions;
import net.minecraftforge.network.NetworkHooks;

import gregtech6.registry.GTMachines;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.machines.TileEntityOven;
import gregtech6.util.UT6;

/**
 * The Oven block (task p4-machine-oven, spec 7/8) — the block-side of the first real
 * processing machine. BlockState carries the machine's whole visual payload: FACING
 * (upstream byte mFacing, the GT6 side order == Direction horizontal subset) plus
 * ACTIVE/RUNNING, the two visual bits of upstream getVisualData :1010-1011
 * ({@code (mActive?1:0)|(mRunning?2:0)}) re-expressed as properties (A-tier rendering:
 * 16 datagen blockstate variants (4 horizontal facings), no dynamic model yet).
 *
 * <p>use() is the chest/open-GUI chain (GTExampleChestBlock.use, upstream
 * onBlockActivated3 :483-486 → openGUI) without the blocked-above guard (a furnace-style
 * machine has no lid). Between the cover consumption and the GUI open sits the GTCEu
 * onWrenchClick :509-525 rotation layer (task p6-oven-rotation): a hoe held with shift
 * rotates the front through the grid edge cells (shift never opens the GUI), everything
 * else opens the GUI unchanged. setPlacedBy mirrors onPlaced (:128-131): the BE's facing
 * becomes the player's horizontal look direction, double-written NBT + BlockState.
 */
public class GTOvenBlock extends GTEntityBlock {

	/** Facing property (horizontal — the upstream SIDES_HORIZONTAL valid sides). */
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	/** Actually processing (upstream mActive, visual bit 0). */
	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

	/** Powered / has work (upstream mRunning, visual bit 1). */
	public static final BooleanProperty RUNNING = BooleanProperty.create("running");

	public GTOvenBlock(Properties aProperties) {
		super(aProperties);
		registerDefaultState(this.stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH).setValue(ACTIVE, false).setValue(RUNNING, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> aBuilder) {
		aBuilder.add(FACING, ACTIVE, RUNNING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext aContext) {
		// the player's horizontal look direction — the same side setFacingFromPlacement writes
		// (chest precedent onPlaced :128-131, UT.Code.getSideForPlayerPlacing SIDES_HORIZONTAL)
		return defaultBlockState().setValue(FACING, aContext.getHorizontalDirection());
	}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return GTMachines.OVEN_BE.get();
	}

	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL; // BaseEntityBlock.java:19-21 default is INVISIBLE (BER assumption)
	}

	@Override
	public InteractionResult use(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, InteractionHand aHand, BlockHitResult aHit) {
		// AbstractFurnaceBlock.use :39-45 shape, upstream onBlockActivated3 :483-486
		if (aLevel.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		BlockEntity tBlockEntity = aLevel.getBlockEntity(aPos);
		if (tBlockEntity instanceof TileEntityOven tOven && aPlayer instanceof ServerPlayer tServerPlayer) {
			// upstream onBlockActivated2 :106-133 — the cover machinery consumes the click first
			// (the covered-face intercepts, then the attachCoversFirst install branch :118-123);
			// false falls through to the GUI open (upstream onBlockActivated3 = :124)
			if (tOven.onCoverUse(aPlayer, (byte) aHit.getDirection().get3DDataValue(), aPlayer.getItemInHand(aHand),
					(float) (aHit.getLocation().x - aPos.getX()), (float) (aHit.getLocation().y - aPos.getY()), (float) (aHit.getLocation().z - aPos.getZ()))) {
				return InteractionResult.CONSUME;
			}
			// task p6-oven-rotation — the GTCEu onWrenchClick :509-525 layer, between the
			// cover consumption above and the GUI open below (upstream onBlockActivated3
			// :124): a hoe held with shift rotates the machine through the grid edge cells.
			// The side resolution is the same UT6.getSideWrenching pick the wrench grid
			// overlay draws (GTFluidPipeBlock.use:109-114 shape — not the bare hit
			// direction), so the cell clicked is constructively the cell shown. A valid
			// side rotates; an invalid one (vertical / the front / SIDE_INVALID) is the
			// GTCEu FAIL :518-519 no-op — shift never opens the GUI; without shift the
			// GUI path is unchanged (PASS :524).
			ItemStack tHeld = aPlayer.getItemInHand(aHand);
			if (!tHeld.isEmpty() && tHeld.canPerformAction(ToolActions.HOE_DIG) && aPlayer.isShiftKeyDown()) {
				tOven.setFrontFacing(UT6.getSideWrenching((byte) aHit.getDirection().get3DDataValue(),
						(float) (aHit.getLocation().x - aPos.getX()), (float) (aHit.getLocation().y - aPos.getY()),
						(float) (aHit.getLocation().z - aPos.getZ())));
				return InteractionResult.CONSUME;
			}
			NetworkHooks.openScreen(tServerPlayer, tOven, aPos); // upstream openGUI
		}
		return InteractionResult.CONSUME;
	}

	@Override
	public void setPlacedBy(Level aLevel, BlockPos aPos, BlockState aState, LivingEntity aPlacer, ItemStack aStack) {
		super.setPlacedBy(aLevel, aPos, aState, aPlacer, aStack);
		if (aPlacer instanceof Player tPlayer && aLevel.getBlockEntity(aPos) instanceof TileEntityOven tOven) {
			tOven.setFacingFromPlacement(tPlayer); // upstream onPlaced :128-131
		}
	}
}
