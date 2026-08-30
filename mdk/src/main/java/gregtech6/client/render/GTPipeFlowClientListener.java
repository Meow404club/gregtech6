package gregtech6.client.render;

import java.util.List;

import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * The client-side wiring of the pipe flow-arrow models (task p4-pipe-flow-control
 * spec ④, ADR-P3-4: card-local {@code @EventBusSubscriber}, GT6Mod/GTModBusListener
 * untouched). Dist.CLIENT — the dedicated server never loads this class, so the
 * client-only {@link GTFluidPipeFlowModel} chain stays server-safe. Registration runs
 * at mod construct (strictly before the first resource reload that fires
 * ModifyBakingResult — GTRenderModelListener class doc).
 *
 * <p>Targets: the two pipe blockstate models (one per wood tier — GT6BlockStates
 * addFluidPipe names each model after the block registry path). The ATLAS WIRING is
 * the consumer-side datagen (GT6Atlases → the Forge SpriteSourceProvider): the arrow
 * sprite {@code gt6:block/pipe_flow_arrow} lands as an explicit single-file source in
 * the block atlas definition.
 */
@Mod.EventBusSubscriber(modid = GTRenderModelListener.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTPipeFlowClientListener {

	/** The pipe blockstate-model ids that carry the dynamic flow model. */
	public static final List<String> TARGET_MODELS = List.of("block/wood_fluid_pipe_small", "block/wood_fluid_pipe_medium");

	private GTPipeFlowClientListener() {
	}

	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		register();
	}

	/** Idempotent registration (also the test hook — the listener class never loads in tests implicitly). */
	public static void register() {
		for (String tModel : TARGET_MODELS) {
			GTRenderModelListener.registerDynamicModel(new ResourceLocation(GTRenderModelListener.MOD_ID, tModel), GTFluidPipeFlowModel::new);
		}
	}
}
