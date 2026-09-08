package gregtech6.client.render;

import javax.annotation.Nullable;

import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The client-side wiring of the pipe C-Foam tint (task p25-c-foam-pipe-spray spec ⑤,
 * ADR-P3-4: card-local {@code @EventBusSubscriber}, Dist.CLIENT). The foam quads carry
 * {@link GTFluidPipeFoamModel#FOAM_TINT_INDEX} and this BlockColor resolves it from the
 * BE's {@link GTModelProperties#PAINT} model data — applyFoam paints the pipe the foam
 * colour (the upstream :161/:163 write), so the tint IS the sprayed DYES_INT value, the
 * same grayscale-multiply the machine/barrel paint domains render through (the
 * {@code GTMachinePaintTint} tint pipeline, tint index 1 instead of the machines' 0).
 * No PAINT (a race before the sync lands) is the {@code -1} no-tint sentinel — the
 * grayscale foam shows untinted for one frame at worst.
 *
 * <p>MODEL registration is NOT here: the per-state keys are single-registration
 * (GTRenderModelListener last-wins), so {@code GTPipeFlowClientListener} registers the
 * composed {@link GTFluidPipeFoamModel#chain()} directly.
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = GTRenderModelListener.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTPipeFoamClientListener {

	private GTPipeFoamClientListener() {
	}

	/**
	 * The pure seam the tests drive (the GTMachinePaintTint.tintARGB shape): the opaque
	 * ARGB for one tint index over one snapshot. Index {@link GTFluidPipeFoamModel#FOAM_TINT_INDEX}
	 * reads {@link GTModelProperties#PAINT} (absent = the -1 no-tint sentinel); every other
	 * index is no tint (the arrow quads' index 0 stays untouched).
	 */
	public static int foamTintARGB(@Nullable ModelData aData, int aTintIndex) {
		if (aTintIndex != GTFluidPipeFoamModel.FOAM_TINT_INDEX) return -1;
		Integer tPaint = aData == null ? null : aData.get(GTModelProperties.PAINT);
		return tPaint == null ? -1 : 0xFF000000 | (tPaint.intValue() & 0xFFFFFF);
	}

	/**
	 * The world-side half: registered over the two pipe blocks. The BE-type gate is in the
	 * lambda (a non-pipe position is the -1 sentinel).
	 */
	public static BlockColor foamBlockColor() {
		return (BlockState aState, @Nullable BlockAndTintGetter aLevel, @Nullable BlockPos aPos, int aTintIndex) -> {
			if (aTintIndex != GTFluidPipeFoamModel.FOAM_TINT_INDEX || aLevel == null || aPos == null) return -1;
			BlockEntity tBE = aLevel.getBlockEntity(aPos);
			return tBE instanceof gregtech6.tileentity.connectors.GTFluidPipeBlockEntity
					? foamTintARGB(tBE.getModelData(), aTintIndex) : -1;
		};
	}

	@SubscribeEvent
	public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block aEvent) {
		aEvent.getBlockColors().register(foamBlockColor(),
				gregtech6.registry.GTFluidPipes.WOOD_FLUID_PIPE_SMALL.get(),
				gregtech6.registry.GTFluidPipes.WOOD_FLUID_PIPE_MEDIUM.get());
	}
}
