package gregtech6.block.multiblock;

import net.minecraft.world.level.block.entity.BlockEntityType;

import gregtech6.registry.GT6Crucibles;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.multiblocks.TileEntityCrucible;

/**
 * The LARGE Steel Crucible controller block (upstream "Large Steel Crucible",
 * Loader_MultiTileEntities.java :1270 — MTE id 17309, hardness == resistance 6.0,
 * NBT_ACIDPROOF F). The concrete {@link GTMultiBlockControllerBlock} of the single-rung
 * material ladder (task crucible-multiblock SPEC ⑤: the 8-material梯 + NBT_DESIGN
 * wall swap defer pool): it mounts the shared MULTIBLOCK_CRUCIBLE_BE and carries the row
 * — the upstream registration NBT (NBT_MATERIAL + NBT_DESIGN wall id + NBT_ACIDPROOF)
 * becomes the row the controller BE reads its wall block and shell material from (the
 * BoilerTankBlock row-carrier form).
 *
 * <p>NO GUI (the upstream census) — the right-click face IS the interaction (issue #20
 * sub-task B): the {@code use} override routes the TOP-face click into
 * {@link TileEntityCrucible#useTop} (the GT6Crucibles.CrucibleBlock.use carrier form,
 * upstream onBlockActivated3 :460-544); every other face and an unformed structure PASS.
 */
public class GTCrucibleControllerBlock extends GTMultiBlockControllerBlock {

	private final GT6Crucibles.CrucibleRow mRow;

	public GTCrucibleControllerBlock(GT6Crucibles.CrucibleRow aRow, Properties aProperties) {
		// task tex-multiblockmains — the row's shell material rides the carrier ctor
		// (the :1270-1277 NBT_MATERIAL columns; the GTLargeBoilerBlock form), so the
		// two-layer controller model's tintindex-0 body resolves the row colour through
		// the GTMultiBlockControllerBlock.materialOf gate (the p38-c2 form)
		super(aProperties, aRow::material);
		mRow = aRow;
	}
	//? if neoforge {
	/*
	// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
	// dispatch). The simpleCodec representative-value form is the vanilla StairBlock
	// precedent — a parse-time default carrying no live config; world save/load never
	// runs through this codec (the registry-id + property mapper does).
	@Override
	protected com.mojang.serialization.MapCodec<? extends GTCrucibleControllerBlock> codec() {
		return simpleCodec(aProperties -> new GTCrucibleControllerBlock(GT6Crucibles.CRUCIBLE_ROWS.get(0), aProperties));
	}
	*///?}

	/** The registration row (the GTLargeBoilerBlock.row carrier read — wall + shell material). */
	public GT6Crucibles.CrucibleRow row() {
		return mRow;
	}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return GT6Crucibles.MULTIBLOCK_CRUCIBLE_BE.get();
	}

	/**
	 * The :460-544 click carrier (the GT6Crucibles.CrucibleBlock.use dual-leg form): only
	 * the top face reacts, the BE answers the structure gate (an unformed crucible
	 * PASSes), the arms run server-side and the click consumes sided.
	 */
	@Override
	//? if forge {
	public net.minecraft.world.InteractionResult use(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.world.level.Level aLevel, net.minecraft.core.BlockPos aPos, net.minecraft.world.entity.player.Player aPlayer, net.minecraft.world.InteractionHand aHand, net.minecraft.world.phys.BlockHitResult aHit) {
	//?} else {
	/*public net.minecraft.world.InteractionResult useWithoutItem(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.world.level.Level aLevel, net.minecraft.core.BlockPos aPos, net.minecraft.world.entity.player.Player aPlayer, net.minecraft.world.phys.BlockHitResult aHit) {
	//21.1: BlockBehaviour.use folded into useWithoutItem (javap 21.1.249) — the
	//InteractionHand param dropped from the signature; the game loop drives the hands
	//in order and MAIN_HAND is the canonical first entry.
	net.minecraft.world.InteractionHand aHand = net.minecraft.world.InteractionHand.MAIN_HAND;
	*///?}
		// the :462 SIDES_TOP gate — only the top opening reacts (the NO_GUI contract)
		if (aHit.getDirection() != net.minecraft.core.Direction.UP) return net.minecraft.world.InteractionResult.PASS;
		if (aLevel.getBlockEntity(aPos) instanceof TileEntityCrucible tCrucible) {
			if (!tCrucible.useTop(aPlayer, aHand)) return net.minecraft.world.InteractionResult.PASS; // :461 the structure gate
			return net.minecraft.world.InteractionResult.sidedSuccess(aLevel.isClientSide);
		}
		return net.minecraft.world.InteractionResult.PASS;
	}

	/**
	 * The composed display name — the vanilla block key keeps the row-less form (the
	 * single-rung ladder has no template to compose; the translation rides the lang
	 * provider, the coke-oven-bricks shape).
	 */
	@Override
	public net.minecraft.network.chat.MutableComponent getName() {
		return net.minecraft.network.chat.Component.translatable("block.gt6." + mRow.path());
	}
}
