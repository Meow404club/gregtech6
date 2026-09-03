package gregtech6.block.energy;

import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.block.GTEntityBlock;
import gregtech6.registry.GTBlockEntities;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The GearBox block (task p12-gearbox-transformer) — the full-cube carrier over the
 * shared BET (ADR-P3-1), one single wood-row variant for this card (the upstream
 * "Custom Wooden Gearbox" Loader row :1669 — hardness 6.0 / resistance 6.0, the wooden
 * NBT_FLAMMABILITY half is a later card; the material family fan-out rides the pool).
 *
 * <p>NO FACING property — the gearbox is side-symmetric, the connectivity model is the
 * BE connection mask ({@code mAxleGear}, set through the {@code /gt6engine gearbox}
 * acceptance channel; the wrench/monkey-wrench installation is the p12-gear-items pool
 * card). NO use override (no GUI, no interaction) and NO onRemove override (the
 * BaseEntityBlock kill+recreate lesson, remember id59 — the gear explosion keeps the
 * block alive and drops through the ItemEntity path from the BE, never the block).
 */
public class GTGearBoxBlock extends GTEntityBlock {

	public GTGearBoxBlock(Properties aProperties) {
		super(aProperties);
	}
	//? if neoforge {
	/*
	// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
	// dispatch). The simpleCodec representative-value form is the vanilla StairBlock
	// precedent — a parse-time default carrying no live config; world save/load never
	// runs through this codec (the registry-id + property mapper does).
	@Override
	protected com.mojang.serialization.MapCodec<? extends GTGearBoxBlock> codec() {
		return simpleCodec(aProperties -> new GTGearBoxBlock(aProperties));
	}
	*///?}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return GTBlockEntities.GEARBOX_BE.get();
	}

	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL; // BaseEntityBlock default INVISIBLE is for BER blocks
	}
}
