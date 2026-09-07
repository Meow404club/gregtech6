package gregtech6.block;

import net.minecraft.core.BlockPos;
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

import gregtech6.registry.GTMachines;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.machines.TileEntityAdvancedCraftingTable;

/**
 * The Advanced Crafting Table block (task p24-act-machine) — the GTOvenBlock shape over
 * the facing axis ONLY: the ACT is zero-energy and player-click driven, so there is no
 * ACTIVE/RUNNING visual payload (the upstream machine carries no state decals — the
 * texture set has no overlay_active/overlay_running layers, the craftingtables/advanced
 * group). FACING is the shared BlockStateProperties.HORIZONTAL_FACING instance (the
 * ADR-P16-2 single-owner alias — the datagen variant builder reads it through the same
 * GTOvenBlock.FACING reference).
 *
 * <p>use() stays inert through the C1 review (the card's phased commits: the BE is the
 * independently-verifiable unit — the RCON chain drives everything through /gt6act);
 * C2 wires the ModularUI open chain here (NetworkHooks.openScreen over the MenuProvider
 * BE, the oven :199 shape) when the menu lands.
 */
public class GTAdvancedCraftingTableBlock extends GTEntityBlock {

	/** Facing property (horizontal — the shared single instance, the ADR-P16-2 alias). */
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

	public GTAdvancedCraftingTableBlock(Properties aProperties) {
		super(aProperties);
		registerDefaultState(this.stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH));
	}
	//? if neoforge {
	/*
	// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
	// dispatch) — the GTOvenBlock simpleCodec precedent verbatim.
	@Override
	protected com.mojang.serialization.MapCodec<? extends GTAdvancedCraftingTableBlock> codec() {
		return simpleCodec(aProperties -> new GTAdvancedCraftingTableBlock(aProperties));
	}
	*///?}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> aBuilder) {
		aBuilder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext aContext) {
		// the player's horizontal look direction — the setFacingFromPlacement twin (the oven :88 shape)
		return defaultBlockState().setValue(FACING, aContext.getHorizontalDirection());
	}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return GTMachines.ADVANCED_CRAFTING_TABLE_BE.get();
	}

	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL; // BaseEntityBlock default is INVISIBLE (the oven :98 note)
	}

	@Override
	//? if forge {
	public InteractionResult use(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, InteractionHand aHand, net.minecraft.world.phys.BlockHitResult aHit) {
	//?} else {
	/*public InteractionResult useWithoutItem(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, net.minecraft.world.phys.BlockHitResult aHit) {
	//21.1: BlockBehaviour.use folded into useWithoutItem — the InteractionHand param dropped
	//(the GTOvenBlock fork shape).
	InteractionHand aHand = InteractionHand.MAIN_HAND;
	*///?}
		// C1: the GUI open chain lands with the C2 ModularUI menu (the phase ruling — the
		// BE is the independently-verifiable C1 unit, the RCON chain drives /gt6act).
		return InteractionResult.CONSUME;
	}

	@Override
	public void setPlacedBy(Level aLevel, BlockPos aPos, BlockState aState, LivingEntity aPlacer, ItemStack aStack) {
		super.setPlacedBy(aLevel, aPos, aState, aPlacer, aStack);
		if (aPlacer instanceof Player tPlayer && aLevel.getBlockEntity(aPos) instanceof TileEntityAdvancedCraftingTable tTable) {
			tTable.setFacingFromPlacement(tPlayer); // upstream onPlaced :128-131 (the oven :210 shape)
		}
	}
}
