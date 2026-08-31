package gregtech6.block;

import java.util.function.Supplier;

import javax.annotation.Nullable;

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

import net.minecraftforge.network.NetworkHooks;

import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.machines.TileEntityBasicMachine;

/**
 * The basic-machine block (task p7-basicmachine-family ③) — the GTOvenBlock shape with the
 * cover machinery and the wrench-rotation layer cut (the machine family registers no cover
 * surface and the side-configuration stays a carrier concern): BlockState carries the whole
 * visual payload — FACING (the upstream byte mFacing, horizontal subset) plus ACTIVE/RUNNING,
 * the two visual bits of upstream getVisualData :1010-1011 re-expressed as properties
 * (16 datagen blockstate variants per machine, the A-tier furnace idiom). use() is the
 * pure open-GUI chain (upstream onBlockActivated3 :483-486 → openGUI), setPlacedBy mirrors
 * onPlaced (:128-131). No onRemove override (id59).
 *
 * <p>One class, three registered blocks (Shredder/Crusher/Lathe) — each instance resolves
 * its own BlockEntityType through the supplier captured at registration (ADR-P3-1: the
 * ticker asks the live instance every tick).
 */
public class GTBasicMachineBlock extends GTEntityBlock {

	/** Facing property (horizontal — the upstream SIDES_HORIZONTAL valid sides). */
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	/**
	 * Actually processing (upstream mActive, visual bit 0). Interning note: BooleanProperty
	 * .create caches by name (BooleanProperty.java BY_NAME), so this is the SAME instance as
	 * GTOvenBlock.ACTIVE — the shared datagen helper reads either interchangeably.
	 */
	public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

	/** Powered / has work (upstream mRunning, visual bit 1). */
	public static final BooleanProperty RUNNING = BooleanProperty.create("running");

	private final Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> mTickerType;

	public GTBasicMachineBlock(Properties aProperties, Supplier<BlockEntityType<? extends TileEntityBase03TicksAndSync>> aTickerType) {
		super(aProperties);
		mTickerType = aTickerType;
		registerDefaultState(this.stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH).setValue(ACTIVE, false).setValue(RUNNING, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> aBuilder) {
		aBuilder.add(FACING, ACTIVE, RUNNING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext aContext) {
		// the player's horizontal look direction — the same side setFacingFromPlacement writes
		return defaultBlockState().setValue(FACING, aContext.getHorizontalDirection());
	}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return mTickerType.get();
	}

	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL; // BaseEntityBlock.java:19-21 default is INVISIBLE (BER assumption)
	}

	@Override
	public InteractionResult use(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, InteractionHand aHand, BlockHitResult aHit) {
		// AbstractFurnaceBlock.use :39-45 shape, upstream onBlockActivated3 :483-486
		// (the cover consumption and the wrench rotation of GTOvenBlock.use are cut —
		// the machine family registers no covers)
		if (aLevel.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		BlockEntity tBlockEntity = aLevel.getBlockEntity(aPos);
		if (tBlockEntity instanceof TileEntityBasicMachine tMachine && aPlayer instanceof ServerPlayer tServerPlayer) {
			NetworkHooks.openScreen(tServerPlayer, tMachine, aPos); // upstream openGUI
		}
		return InteractionResult.CONSUME;
	}

	@Override
	public void setPlacedBy(Level aLevel, BlockPos aPos, BlockState aState, LivingEntity aPlacer, ItemStack aStack) {
		super.setPlacedBy(aLevel, aPos, aState, aPlacer, aStack);
		if (aPlacer instanceof Player tPlayer && aLevel.getBlockEntity(aPos) instanceof TileEntityBasicMachine tMachine) {
			tMachine.setFacingFromPlacement(tPlayer); // upstream onPlaced :128-131
		}
	}
}
