package gregtech6.block.logistics;

import net.minecraft.world.level.block.entity.BlockEntityType;

import gregtech6.block.multiblock.GTMultiBlockControllerBlock;
import gregtech6.registry.GT6Logistics;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import gregtech6.covers.ICoverableTE;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * The Logistics Core controller block (task p32-logistics-lv3, upstream meta 17997,
 * Loader_MultiTileEntities.java:1281, MT.SteelGalvanized, hardness/resistance 6.0F) —
 * the concrete {@link GTMultiBlockControllerBlock} mounting the core BET (the massfab
 * minimal form: everything visual/behavioural is base-owned FACING + FORMED; no use-face
 * — the controller runs headless, the status surface is the /gt6logistics core command
 * and the cover card's display covers).
 */
public class GTLogisticsCoreBlock extends GTMultiBlockControllerBlock {

	public GTLogisticsCoreBlock(Properties aProperties) {
		super(aProperties);
	}
	//? if neoforge {
	/*
	// 21.1 made BaseEntityBlock.codec() abstract — the simpleCodec representative form
	// (the GTMassfabBlock precedent).
	@Override
	protected com.mojang.serialization.MapCodec<? extends GTLogisticsCoreBlock> codec() {
		return simpleCodec(aProperties -> new GTLogisticsCoreBlock(aProperties));
	}
	*///?}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return GT6Logistics.LOGISTICS_CORE_BE.get();
	}

	@Override
	public void stepOn(Level aLevel, BlockPos aPos, BlockState aState, Entity aEntity) {
		super.stepOn(aLevel, aPos, aState, aEntity);
		if (aLevel.getBlockEntity(aPos) instanceof ICoverableTE tCoverable) tCoverable.onCoverWalkOver(aEntity); // MultiTileEntityBlock.java:306 -> 06Covers:428 (p37-covers-crafting-asphalt)
	}
}
