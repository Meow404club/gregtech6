package gregtech6.covers.client;

import java.util.List;

import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import gregtech6.client.render.GTRenderModelListener;

/**
 * The client-side wiring of the cover plate models (task p4-cover-core ⑥, ADR-P3-4:
 * card-local {@code @EventBusSubscriber}, GT6Mod/GTModBusListener untouched).
 * Dist.CLIENT — the dedicated server never loads this class, so the client-only
 * {@link CoverPlateModel} chain stays server-safe. Registration runs at mod construct
 * (strictly before the first resource reload that fires ModifyBakingResult —
 * GTRenderModelListener class doc).
 *
 * <p>Targets: the three oven blockstate models (inactive/active/running —
 * GT6BlockStates.addOven) each get wrapped, so every ACTIVE/RUNNING variant renders
 * plates. The ATLAS WIRING is the consumer-side datagen (GT6Atlases → the Forge
 * SpriteSourceProvider): the plate sprite ({@code gt6:item/material_sets/metallic/plate})
 * lands as an explicit single-file source in the block atlas definition
 * (GTRenderModelListener atlas note — the foundation ships zero textures, the consumer
 * card lands them).
 */
@Mod.EventBusSubscriber(modid = GTRenderModelListener.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTCoverClientListener {

	/** The oven blockstate-model ids that carry the dynamic plate model. */
	public static final List<String> TARGET_MODELS = List.of("block/oven", "block/oven_active", "block/oven_running");

	private GTCoverClientListener() {
	}

	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		register();
	}

	/** Idempotent registration (also the test hook — the listener class never loads in tests implicitly). */
	public static void register() {
		for (String tModel : TARGET_MODELS) {
			GTRenderModelListener.registerDynamicModel(new ResourceLocation(GTRenderModelListener.MOD_ID, tModel), CoverPlateModel::new);
		}
	}
}
