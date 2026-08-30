package gregtech6.block.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import gregtech6.registry.GTMultiBlocks;
import gregtech6.tileentity.multiblocks.TileEntityCokeOven;

/**
 * The Coke Oven controller block — the concrete {@link GTMultiBlockControllerBlock} of the
 * first multiblock (upstream MultiTileEntityCokeOven, task p4-multiblock-framework spec ④).
 * Everything visual/behavioural is base-owned (FACING + FORMED); this class only mounts the
 * Coke Oven BET.
 *
 * <p>p6-cokeoven-processing adds the block-side ignition hook (the card block_hook ruling):
 * a main-hand flint-and-steel click on the controller ignites it server-side
 * ({@link TileEntityCokeOven#ignite()} = the upstream TOOL_igniter branch
 * MultiTileEntityBasicMachine:373-379) and consumes one durability point — the flint is the
 * igniter stand-in (the hoe = crowbar substitution precedent). Every other interaction
 * keeps the base behaviour.
 */
public class GTCokeOvenBlock extends GTMultiBlockControllerBlock {

	public GTCokeOvenBlock(Properties aProperties) {
		super(aProperties);
	}

	@Override
	protected BlockEntityType<? extends gregtech6.tileentity.TileEntityBase03TicksAndSync> tickerType() {
		return GTMultiBlocks.COKE_OVEN_BE.get();
	}

	@Override
	public InteractionResult use(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, InteractionHand aHand, BlockHitResult aHit) {
		ItemStack tHeld = aPlayer.getItemInHand(aHand);
		if (tHeld.is(Items.FLINT_AND_STEEL) && aLevel.getBlockEntity(aPos) instanceof TileEntityCokeOven tOven) {
			if (aLevel.isClientSide()) return InteractionResult.SUCCESS;
			tOven.ignite(); // no-op while !mRequiresIgnition (the upstream :377 return-0 shape)
			if (!tOven.mRequiresIgnition) return InteractionResult.PASS;
			tHeld.hurtAndBreak(1, aPlayer, p -> p.broadcastBreakEvent(aHand == InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND)); // CONSUME the tool
			return InteractionResult.CONSUME;
		}
		return super.use(aState, aLevel, aPos, aPlayer, aHand, aHit);
	}
}
