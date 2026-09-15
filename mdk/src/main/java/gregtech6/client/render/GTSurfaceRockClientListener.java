package gregtech6.client.render;

import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import gregtech6.block.surface.GT6SurfaceRockBlock;
import gregtech6.registry.GT6SurfaceBlocks;

/**
 * The client wiring of the surface-rock material tint (task p30-w6-rocks-sticks) — the
 * {@link GTPipeFoamClientListener} card-local {@code @EventBusSubscriber(Dist.CLIENT)}
 * form. The shared rock model carries {@code tintindex 0} on every face (the machineModel
 * p21-paintable-tint-render form) and this BlockColor resolves it to the material's solid
 * RGB ({@link GT6SurfaceRockBlock#tintARGB}, the GTCEu tintedBlockColor
 * SurfaceRockBlock.java:154-161 form) — the grayscale shared texture paints per material,
 * the same grayscale-multiply pipeline the machine/barrel paint domains render through.
 * The stick block's model carries NO tint index, so its handler entry is never consulted.
 */
@Mod.EventBusSubscriber(modid = GTRenderModelListener.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTSurfaceRockClientListener {

	private GTSurfaceRockClientListener() {
	}

	/** The pure tint face: tint index 0 -> the block's material ARGB (the GTMachinePaintTint sentinel form). */
	public static BlockColor surfaceRockColor() {
		return (BlockState aState, BlockAndTintGetter aLevel, BlockPos aPos, int aTintIndex) -> {
			if (aTintIndex != 0) return -1;
			Block tBlock = aState.getBlock();
			return tBlock instanceof GT6SurfaceRockBlock tRock ? tRock.tintARGB() : -1;
		};
	}

	@SubscribeEvent
	public static void onRegisterBlockColors(RegisterColorHandlersEvent.Block aEvent) {
		aEvent.getBlockColors().register(surfaceRockColor(), GT6SurfaceBlocks.ALL.stream().map(tRow -> tRow.get()).toArray(Block[]::new));
	}
}
