package gregtech6.block.multiblock;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;

import gregtech6.gui.machines.GT6MuiMachine;
import gregtech6.registry.GTMultiBlocks;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.multiblocks.TileEntityImplosionCompressor;

/**
 * The Implosion Compressor controller block (task implosion) — the concrete
 * {@link GTMultiBlockControllerBlock} mounting the Implosion BET (the GTCokeOvenBlock
 * minimal form: everything visual/behavioural is base-owned FACING + FORMED, this class
 * only mounts the BET).
 *
 * <p>The use face (task machine-openface-gap — the sheet-purge debt closing): the upstream
 * controller opens the machine GUI on a plain right-click (MultiTileEntityBasicMachine
 * .onBlockActivated3 :483-486, inherited through TileEntityBase10MultiBlockMachine — the
 * cokeoven-comment-⑤ semantics: whether or not the structure is formed). The port BE rode
 * the MUI family chain headless (MenuProvider without an open dispatch), so the GUI was
 * unreachable; the open now rides {@link GT6MuiMachine#tryOpen} (the BlockEntityUIFactory
 * network + its native 64-sq range gate — the GTCokeOvenBlock :91-101 form, the user
 * ruling 全部统一).
 */
public class GTImplosionCompressorBlock extends GTMultiBlockControllerBlock {

	public GTImplosionCompressorBlock(Properties aProperties) {
		super(aProperties);
	}
	//? if neoforge {
	/*
	// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
	// dispatch) — the simpleCodec representative-value form (the GTCokeOvenBlock precedent).
	@Override
	protected com.mojang.serialization.MapCodec<? extends GTImplosionCompressorBlock> codec() {
		return simpleCodec(aProperties -> new GTImplosionCompressorBlock(aProperties));
	}
	*///?}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return GTMultiBlocks.IMPLOSION_COMPRESSOR_BE.get();
	}

	@Override
	//? if forge {
	public InteractionResult use(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, InteractionHand aHand, BlockHitResult aHit) {
	//?} else {
	/*public InteractionResult useWithoutItem(BlockState aState, Level aLevel, BlockPos aPos, Player aPlayer, BlockHitResult aHit) {
	//21.1: BlockBehaviour.use folded into useWithoutItem — the InteractionHand param dropped
	//(the GTCokeOvenBlock form; the game loop drives MAIN_HAND first).
	InteractionHand aHand = InteractionHand.MAIN_HAND;
	*///?}
		if (aLevel.getBlockEntity(aPos) instanceof TileEntityImplosionCompressor tCompressor) {
			if (aLevel.isClientSide()) return InteractionResult.SUCCESS;
			if (aPlayer instanceof ServerPlayer tServerPlayer) {
				GT6MuiMachine.tryOpen(tServerPlayer, tCompressor);
			}
			return InteractionResult.CONSUME;
		}
		//? if forge {
		return super.use(aState, aLevel, aPos, aPlayer, aHand, aHit);
		//?} else {
		/*return super.useWithoutItem(aState, aLevel, aPos, aPlayer, aHit);
		*///?}
	}
}
