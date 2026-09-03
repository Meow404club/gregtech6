package gregtech6.block.energy;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.block.GTEntityBlock;
import gregtech6.registry.GTBlockEntities;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The GT6 test energy source block (task p8-d4-energy-source spec ②) — a simple cube
 * carrier over the shared BET (ADR-P3-1), the GTWireBlock shape minus the CONNECTIONS
 * property: the source probes its neighbours live on every emit packet
 * (GTEnergySourceBlockEntity.adjacency), so there is no connection mask to carry.
 *
 * <p>NO {@code use} interaction (the /gt6energy command drives the rig headless) and NO
 * onRemove override — the BaseEntityBlock kill+recreate lesson (remember id59).
 */
public class GTEnergySourceBlock extends GTEntityBlock {

	public GTEnergySourceBlock(Properties aProperties) {
		super(aProperties);
	}
	//? if neoforge {
	/*
	// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
	// dispatch). The simpleCodec representative-value form is the vanilla StairBlock
	// precedent — a parse-time default carrying no live config; world save/load never
	// runs through this codec (the registry-id + property mapper does).
	@Override
	protected com.mojang.serialization.MapCodec<? extends GTEnergySourceBlock> codec() {
		return simpleCodec(aProperties -> new GTEnergySourceBlock(aProperties));
	}
	*///?}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return GTBlockEntities.ENERGY_SOURCE_BE.get();
	}

	@Override
	public RenderShape getRenderShape(BlockState aState) {
		return RenderShape.MODEL; // BaseEntityBlock default INVISIBLE is for BER blocks
	}
}
