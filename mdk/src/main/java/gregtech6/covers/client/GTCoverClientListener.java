package gregtech6.covers.client;

import java.util.List;

import net.minecraft.client.resources.model.ModelResourceLocation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import gregtech6.client.render.GTOvenClientListener;
import gregtech6.client.render.GTRenderModelListener;

/**
 * The client-side wiring of the cover plate models (task p4-cover-core ⑥, ADR-P3-4:
 * card-local {@code @EventBusSubscriber}, GT6Mod/GTModBusListener untouched). Dist.CLIENT —
 * the dedicated server never loads this class, so the client-only {@link CoverPlateModel}
 * chain stays server-safe. Registration runs at mod construct (strictly before the first
 * resource reload that fires ModifyBakingResult — GTRenderModelListener class doc).
 *
 * <p>Targets (task p10-cover-plate-perstate-fix, the P9 erratum landed): the oven's
 * <b>16 per-state</b> ModelResourceLocations ({@code gt6:oven#active=…,facing=…,running=…}),
 * reused read-only from {@link GTOvenClientListener#targetModelIds()} (that file stays
 * zero-diff). Vanilla ModelBakery loads one TOP-LEVEL model per BLOCK STATE
 * (ModelBakery.java:136 {@code loadTopLevel(BlockModelShaper.stateToModelLocation(...))}),
 * so the map ModifyBakingResult exposes is keyed per state and never by the blockstate
 * JSON's model-file paths — the pre-p10 registration ids ({@code block/oven},
 * {@code block/oven_active}, {@code block/oven_running}) never matched any key, and every
 * wrap silently degraded through the absent-target skip (GTRenderModelListener.java:97-99,
 * the mechanism proven by task p9-render-c-oven-overlay).
 *
 * <p>SHARED KEYS (declared, not settled here): the oven overlay listener registers the
 * SAME 16 keys with {@code GTOvenOverlayModel::new} on the same last-wins factory table
 * ({@link GTRenderModelListener#registerDynamicModel} = map replacement), and Forge
 * registers {@code @EventBusSubscriber} classes in annotation-scan order without sorting
 * (forge-1.20.1 AutomaticEventSubscriber.java:32-39 {@code ebsTargets.forEach(...)}) — so
 * per key, whichever listener constructs last owns the entry, and only one dynamic layer
 * (plates or overlay) is live on the shared oven host per session. Composing both layers
 * needs a merged dispatch model and is the follow-up; this card fixes the dead-key shape,
 * verified offline (wrap + parity tests, GTCoverClientListenerTest).
 *
 * <p>The ATLAS WIRING is the consumer-side datagen (GT6Atlases → the Forge
 * SpriteSourceProvider): the plate sprite ({@code gt6:item/material_sets/metallic/plate})
 * lands as an explicit single-file source in the block atlas definition
 * (GTRenderModelListener atlas note — the foundation ships zero textures, the consumer
 * card lands them).
 */
@Mod.EventBusSubscriber(modid = GTRenderModelListener.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTCoverClientListener {

	/**
	 * The oven's 16 per-state keys that carry the dynamic plate model — the same key set
	 * the oven overlay listener registers (see the SHARED KEYS note). Pure-function reuse:
	 * {@link GTOvenClientListener#targetModelIds()} builds a fresh list, touches no
	 * game state, so the class-load-time initialization is safe.
	 */
	public static final List<ModelResourceLocation> TARGET_MODELS = GTOvenClientListener.targetModelIds();

	private GTCoverClientListener() {
	}

	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		register();
	}

	/** Idempotent registration (also the test hook — the listener class never loads in tests implicitly). */
	public static void register() {
		// String-key form: the 1.21.1 ModelResourceLocation record is not a ResourceLocation,
		// the factory table is keyed by toString() (leg-neutral, GTRenderModelListener doc)
		for (ModelResourceLocation tTarget : TARGET_MODELS) {
			GTRenderModelListener.registerDynamicModel(tTarget.toString(), CoverPlateModel::new);
		}
	}
}
