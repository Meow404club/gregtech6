package gregtech6.block.multiblock;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.registry.GTMultiBlocks;
import gregtech6.tileentity.multiblocks.HeatTransmitterBlockEntity;

/**
 * The Heat Transmitter part block (upstream MTE id 18101, Loader_MultiTileEntities.java
 * :1176 re-read verbatim — Invar, "heatacceptor", the 3x3 Large-Boiler base layer, the
 * ONLY_ENERGY_IN part mode). The {@link GTMultiBlockPartBlock} shape with the one
 * difference that matters: the block-entity factory mounts the
 * {@link HeatTransmitterBlockEntity} (the energy-relaying part subclass) instead of the
 * plain shared part BE — everything else (onPlace/playerWillDestroy propagation, the
 * no-onRemove red line, RenderShape.MODEL) is inherited untouched.
 */
public class GTHeatTransmitterBlock extends GTMultiBlockPartBlock {

	public GTHeatTransmitterBlock(Properties aProperties) {
		super(aProperties);
	}

	/** The tinted form (task p38-issue8-multipart-tint): the row's Invar column rides along (the partPaintableBlockArray walk). */
	public GTHeatTransmitterBlock(Properties aProperties, @Nullable java.util.function.Supplier<gregapi.oredict.OreDictMaterial> aMaterial) {
		super(aProperties, 0, null, aMaterial);
	}

	@Override
	@Nullable
	public BlockEntity newBlockEntity(BlockPos aPos, BlockState aState) {
		return GTMultiBlocks.HEAT_TRANSMITTER_BE.get().create(aPos, aState);
	}
}
