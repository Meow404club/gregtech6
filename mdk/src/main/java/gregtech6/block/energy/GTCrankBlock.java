package gregtech6.block.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

import gregtech6.block.GTEntityBlock;
import gregtech6.registry.GTBlockEntities;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.energy.GTCrankBlockEntity;

/**
 * The Hand Crank block (task p12-engine-crank spec ②) — the facing cube carrier over the
 * shared BET (ADR-P3-1), the GTOvenBlock facing shape minus the GUI/cover/rotation layers:
 * the crank has NO GUI (the upstream {@code NO_GUI_CLICK_TO_INTERACT} tooltip
 * MultiTileEntityCrank.java:68) and {@code use} IS the drive (upstream
 * {@code onBlockActivated3} :103-112 always-consume arm).
 *
 * <p>Facing = the placement orientation, the player's horizontal look direction (the
 * oven/GTBasicMachineBlock {@code getStateForPlacement} precedent) — the crank's emit side
 * ({@code mFacing}) is then the side AWAY from the player, where the driven machine sits;
 * the handle side the player cranks from is the placement-opposite side. The BE mirror is
 * double-written by {@link #setPlacedBy} (the oven :184-189 form) and re-synced from the
 * state each tick (the {@code /setblock gt6:crank[facing=...]} RCON path — the state is
 * the command-side authority).
 *
 * <p>NO onRemove override — the BaseEntityBlock kill+recreate lesson (remember id59), the
 * GTEnergySourceBlock form.
 */
public class GTCrankBlock extends GTEntityBlock {

	/** Facing property (horizontal — the placement orientation, the machine-side is this Direction). */
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	public GTCrankBlock(Properties aProperties) {
		super(aProperties);
		registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}
	//? if neoforge {
	/*
	// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
	// dispatch). The simpleCodec representative-value form is the vanilla StairBlock
	// precedent — a parse-time default carrying no live config; world save/load never
	// runs through this codec (the registry-id + property mapper does).
	@Override
	protected com.mojang.serialization.MapCodec<? extends GTCrankBlock> codec() {
		return simpleCodec(aProperties -> new GTCrankBlock(aProperties));
	}
	*///?}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> aBuilder) {
		aBuilder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext aContext) {
		// the front TOWARDS the placer (the GT6PlacementFacing canon, task
		// p28-singleblock-facing-canon) — the same side setFacingFromPlacement
		// mirrors into the BE (the GTOvenBlock.getStateForPlacement :72-76 precedent)
		return defaultBlockState().setValue(FACING, gregtech6.block.GT6PlacementFacing.facingTowardsPlacer(aContext.getHorizontalDirection()));
	}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return GTBlockEntities.CRANK_BE.get();
	}

	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL; // BaseEntityBlock default INVISIBLE is for BER blocks
	}

	@Override
	//? if forge {
	public InteractionResult use(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, InteractionHand aHand, BlockHitResult aHit) {
	//?} else {
	/*public InteractionResult useWithoutItem(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, BlockHitResult aHit) {
	//21.1: BlockBehaviour.use folded into useWithoutItem — the InteractionHand param dropped
	//(javap BlockBehaviour 21.1.249); the game loop drives MAIN_HAND first.
	InteractionHand aHand = InteractionHand.MAIN_HAND;
	*///?}
		// the GTOvenBlock.use client/server split shape, the body = upstream
		// onBlockActivated3 :103-112 (server arm only, always consume — no GUI)
		if (aLevel.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (aLevel.getBlockEntity(aPos) instanceof GTCrankBlockEntity tCrank) {
			tCrank.onPlayerCrank(aPlayer);
			return InteractionResult.CONSUME;
		}
		return InteractionResult.PASS;
	}

	@Override
	public void setPlacedBy(Level aLevel, BlockPos aPos, BlockState aState, LivingEntity aPlacer, ItemStack aStack) {
		super.setPlacedBy(aLevel, aPos, aState, aPlacer, aStack);
		if (aPlacer instanceof Player tPlayer && aLevel.getBlockEntity(aPos) instanceof GTCrankBlockEntity tCrank) {
			tCrank.setFacingFromPlacement(tPlayer); // upstream onPlaced :128-131 (the oven precedent)
		}
	}
}
