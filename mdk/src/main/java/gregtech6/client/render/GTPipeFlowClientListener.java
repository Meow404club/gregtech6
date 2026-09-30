package gregtech6.client.render;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * The client-side wiring of the pipe flow-arrow models (task pipe-flow-control
 * spec ④, ADR-P3-4: card-local {@code @EventBusSubscriber}, GT6Mod/GTModBusListener
 * untouched). Dist.CLIENT — the dedicated server never loads this class, so the
 * client-only {@link GTFluidPipeFlowModel} chain stays server-safe. Registration runs
 * at mod construct (strictly before the first resource reload that fires
 * ModifyBakingResult — GTRenderModelListener class doc).
 *
 * <p>Targets: the pipe blockstate models, one per row (the full 280-row matrix —
 * {@link #TARGET_MODELS}; GT6BlockStates pipeBlockstate names each blockstate after the
 * block registry path). The ATLAS WIRING is
 * the consumer-side datagen (GT6Atlases → the Forge SpriteSourceProvider): the arrow
 * sprite {@code gt6:block/pipe_flow_arrow} lands as an explicit single-file source in
 * the block atlas definition.
 */
@Mod.EventBusSubscriber(modid = GTRenderModelListener.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTPipeFlowClientListener {

	/**
	 * The pipe blockstate-model ids that carry the dynamic flow model — the full matrix
	 * walk (task fluid-pipe-matrix; the W1 pair only covered the two wood rows, leaving
	 * the other rows' output arrows unrendered). Same key form per row (the baked
	 * per-state key — the r8-tex-pipe-textures note).
	 */
	public static final List<String> TARGET_MODELS;

	static {
		List<String> tModels = new ArrayList<>();
		for (String tPath : gregtech6.registry.GTFluidPipes.BLOCKS_BY_PATH.keySet()) {
			tModels.add("block/" + tPath);
		}
		TARGET_MODELS = List.copyOf(tModels);
	}

	private GTPipeFlowClientListener() {
	}

	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		register();
	}

	/** Idempotent registration (also the test hook — the listener class never loads in tests implicitly). */
	public static void register() {
		// String-key form (toString) — the 1.21.1 event map keys by the ModelResourceLocation
		// record, so the factory table is keyed leg-neutrally by the id string.
		// Task c-foam-pipe-spray: the factory is the COMPOSED chain — foam(outer) →
		// flow(inner) → baked blockstate model (GTFluidPipeFoamModel.chain()). One
		// registration per per-state key (GTRenderModelListener last-wins), so the foam
		// wrapper rides HERE instead of a second competing registration of the same keys.
		for (String tModel : TARGET_MODELS) {
			GTRenderModelListener.registerDynamicModel(
					new ResourceLocation(GTRenderModelListener.MOD_ID, tModel).toString(), GTFluidPipeFoamModel.chain());
		}
	}
}
