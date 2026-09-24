package gregtech6.block;

import net.minecraft.world.level.block.entity.BlockEntityType;

import gregtech6.registry.GTBlockEntities;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The bedrock fluid-spring nozzle block (task p38-issue5-fluid-spring-nozzle) — the modern
 * carrier of the upstream MultiTileEntityFluidSpring block face (id 32763, placed by
 * WorldgenFluidSpring.java:77-79 under the spring dome), mounted with the
 * {@link gregtech6.tileentity.misc.GTFluidSpringBlockEntity}. Worldgen-only: no BlockItem,
 * no loot ({@code noLootTable} = the upstream Drops_None face — the block is player-
 * unmineable, upstream getBlockHardness -1) and bedrock-grade blast resistance (upstream
 * getExplosionResistance2 = the vanilla bedrock value), the vanilla bedrock
 * {@code strength(-1, 3600000)} property face.
 *
 * <p>Blockstate/model datagen: the fluid-spring band of GT6OreBlockStates (the nether
 * surface-form band shape — a worldgen carrier, no item models, no authored JSON here).
 */
public class GTFluidSpringBlock extends GTEntityBlock {

	public GTFluidSpringBlock(Properties aProperties) {
		super(aProperties);
	}
	//? if neoforge {
	/*
	// 21.1 made BaseEntityBlock.codec() abstract (the vanilla 1.21 block-state codec
	// dispatch). The simpleCodec representative-value form is the TestMachineBlock
	// precedent — a parse-time default carrying no live config; world save/load never
	// runs through this codec (the registry-id + property mapper does).
	@Override
	protected com.mojang.serialization.MapCodec<? extends GTFluidSpringBlock> codec() {
		return simpleCodec(GTFluidSpringBlock::new);
	}
	*///?}

	@Override
	protected BlockEntityType<? extends TileEntityBase03TicksAndSync> tickerType() {
		return GTBlockEntities.FLUID_SPRING_BE.get();
	}
}
