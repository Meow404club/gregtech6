package gregtech6.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

//? if forge {
import net.minecraftforge.network.NetworkHooks;
//?}

import gregtech6.registry.GTBlockEntities;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.example.GTExampleChestBlockEntity;

/**
 * The example chest block (task p3-example-machine) — the block-side of the playable chain:
 * right-click opens the chest BE's menu through NetworkHooks.openScreen, the 1.20.1 mapping of
 * upstream onBlockActivated2 → openGUI (MultiTileEntityChest.java:234-240,
 * TileEntityBase01Root.java:180). openScreen(player, provider, pos) writes the BlockPos
 * payload (NetworkHooks.java:176) that the client-side menu factory resolves the BE from.
 *
 * <p>Upstream guard carried over: the blocked-above check (:235,
 * {@code worldObj.isSideSolid(x, y+1, z, DOWN)}) in its modern idiom
 * (ChestBlock.isBlockedChestByBlock, ChestBlock.java:323-326 — the cat check is vanilla-chest
 * behaviour, not GT6's, and stays out). The open-time isUseableByPlayerGUI distance/owner
 * gate maps to the menu's stillValid (GTExampleChestMenu) instead.
 *
 * <p>Placement mirrors onPlaced (:128-131): mFacing becomes the player's horizontal look
 * direction (UT.Code.getSideForPlayerPlacing with SIDES_HORIZONTAL, UT.java:1755/:1751-1753).
 * getRenderShape is overridden back to MODEL — BaseEntityBlock defaults INVISIBLE
 * (BaseEntityBlock.java:19-21) for BER blocks, and this chest renders from its blockstate
 * model while the TESR is deferred (feature layer).
 */
public class GTExampleChestBlock extends GTEntityBlock {

	public GTExampleChestBlock(Properties aProperties) {
		super(aProperties);
	}
	//? if neoforge {
	/*
	// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
	// dispatch). The simpleCodec representative-value form is the vanilla StairBlock
	// precedent — a parse-time default carrying no live config; world save/load never
	// runs through this codec (the registry-id + property mapper does).
	@Override
	protected com.mojang.serialization.MapCodec<? extends GTExampleChestBlock> codec() {
		return simpleCodec(aProperties -> new GTExampleChestBlock(aProperties));
	}
	*///?}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return GTBlockEntities.EXAMPLE_CHEST_BE.get();
	}

	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL; // BaseEntityBlock.java:19-21 default is INVISIBLE (BER assumption)
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
		// AbstractFurnaceBlock.use :39-45 shape
		if (aLevel.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		BlockEntity tBlockEntity = aLevel.getBlockEntity(aPos);
		if (tBlockEntity instanceof GTExampleChestBlockEntity tChest && aPlayer instanceof ServerPlayer tServerPlayer && !isBlockedAbove(aLevel, aPos)) {
			//? if forge {
			NetworkHooks.openScreen(tServerPlayer, tChest, aPos); // upstream openGUI (TileEntityBase01Root.java:180)
			//?} else {
			/*tServerPlayer.openMenu(tChest, tBuf -> tBuf.writeBlockPos(aPos)); // 21.1: NetworkHooks deleted — ServerPlayer.openMenu(MenuProvider, buf) carries the pos payload (the command-file precedent)
			*///?}
		}
		return InteractionResult.CONSUME;
	}

	/** Upstream :235 blocked-above check in the ChestBlock.java:323-326 idiom. */
	private static boolean isBlockedAbove(Level aLevel, BlockPos aPos) {
		BlockPos tAbove = aPos.above();
		return aLevel.getBlockState(tAbove).isRedstoneConductor(aLevel, tAbove);
	}

	@Override
	public void setPlacedBy(Level aLevel, BlockPos aPos, BlockState aState, LivingEntity aPlacer, ItemStack aStack) {
		super.setPlacedBy(aLevel, aPos, aState, aPlacer, aStack);
		if (aPlacer instanceof Player tPlayer && aLevel.getBlockEntity(aPos) instanceof GTExampleChestBlockEntity tChest) {
			tChest.setFacingFromPlacement(tPlayer); // upstream onPlaced :128-131
		}
	}
}
