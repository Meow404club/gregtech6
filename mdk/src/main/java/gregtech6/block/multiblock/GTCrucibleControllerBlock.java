package gregtech6.block.multiblock;

import net.minecraft.world.level.block.entity.BlockEntityType;

import gregtech6.registry.GT6Crucibles;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.multiblocks.TileEntityCrucible;

/**
 * The LARGE Steel Crucible controller block (upstream "Large Steel Crucible",
 * Loader_MultiTileEntities.java :1270 — MTE id 17309, hardness == resistance 6.0,
 * NBT_ACIDPROOF F). The concrete {@link GTMultiBlockControllerBlock} of the single-rung
 * material ladder (task p26-crucible-multiblock SPEC ⑤: the 8-material梯 + NBT_DESIGN
 * wall swap defer pool): it mounts the shared MULTIBLOCK_CRUCIBLE_BE and carries the row
 * — the upstream registration NBT (NBT_MATERIAL + NBT_DESIGN wall id + NBT_ACIDPROOF)
 * becomes the row the controller BE reads its wall block and shell material from (the
 * BoilerTankBlock row-carrier form).
 *
 * <p>NO use override: the crucible has NO GUI (the explicit defer, SPEC ⑥ — MUI 批 A
 * merged first), a right-click does nothing.
 */
public class GTCrucibleControllerBlock extends GTMultiBlockControllerBlock {

	private final GT6Crucibles.CrucibleRow mRow;

	public GTCrucibleControllerBlock(GT6Crucibles.CrucibleRow aRow, Properties aProperties) {
		super(aProperties);
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
	 * The composed display name — the vanilla block key keeps the row-less form (the
	 * single-rung ladder has no template to compose; the translation rides the lang
	 * provider, the coke-oven-bricks shape).
	 */
	@Override
	public net.minecraft.network.chat.MutableComponent getName() {
		return net.minecraft.network.chat.Component.translatable("block.gt6." + mRow.path());
	}
}
