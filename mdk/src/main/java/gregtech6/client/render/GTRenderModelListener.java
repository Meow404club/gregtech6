package gregtech6.client.render;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * The self-contained mod-bus wiring of the C-grade render foundation (ADR-P3-4: card-local
 * {@code @EventBusSubscriber}, GT6Mod.java/GTModBusListener.java untouched; Dist.CLIENT —
 * skipped entirely on the dedicated server): replaces the baked model of a target
 * blockstate with a {@link GTDynamicBakedModel} at resource-reload time.
 *
 * <p>The event is {@link ModelEvent.ModifyBakingResult} (ModelEvent.java:51) — the only
 * Forge hook whose model map is still modifiable after baking. It fires on a WORKER THREAD
 * (ModelEvent.java:40-43: "fired from a worker thread and it is therefore not safe to
 * access anything outside the model registry"), which is why the factory table is a
 * {@link ConcurrentHashMap} and factories must not touch anything but the baked model they
 * receive. BakingCompleted (ModelEvent.java:91) is read-only and deliberately unused.
 *
 * <p>CONSUMER TEMPLATE (W3 p4-cover-core):
 * <pre>{@code
 * // during mod construction (before the first resource reload):
 * GTRenderModelListener.registerDynamicModel(
 *     new ResourceLocation("gt6", "block/machine/oven"),
 *     baked -> new OvenDynamicModel(baked)); // OvenDynamicModel extends GTDynamicBakedModel
 * }</pre>
 *
 * <p>ATLAS WIRING (card note ⑤ — the foundation ships ZERO textures; the consumer card
 * lands the sources): every sprite a dynamic model stitches in runtime-built quads must be
 * present in the block atlas. Datagen side — add the sprite ids as a source in the
 * generated {@code assets/gt6/atlases/blocks.json} (a datagen provider output; hand-written
 * JSON is forbidden; SpriteResourceLoader merges sources across namespaces — atlas
 * sources list, forge-docs). Runtime side — resolve the sprite through
 * {@code Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(spriteId)}
 * (Minecraft.java:2386) and bake the quad's UVs from it. Textures living under
 * {@code textures/block/} are additionally always stitched by vanilla's cross-namespace
 * {@code directory("block")} atlas source (vanilla blocks.json) — the explicit source is
 * belt-and-suspenders.
 *
 * <p>DISPATCH-KEY SHAPE (clarified by task p9-render-c-oven-overlay): vanilla ModelBakery
 * loads one TOP-LEVEL model per BLOCK STATE (ModelBakery.java:136
 * {@code loadTopLevel(BlockModelShaper.stateToModelLocation(block, state))}), so the map
 * exposed here is keyed by per-state {@link net.minecraft.resources.ModelResourceLocation}s
 * ({@code gt6:oven#active=…,facing=…,running=…}) — the blockstate JSON's model-file paths
 * are only unbaked dependencies and never appear as keys. A consumer targeting a
 * variant-based block must therefore register its per-state keys (see
 * {@link GTOvenClientListener} for the oven's 16-key form); a model-file id in the
 * template above silently degrades (the absent-target skip below).
 */
@Mod.EventBusSubscriber(modid = GTRenderModelListener.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTRenderModelListener {

	public static final String MOD_ID = "gt6";

	/**
	 * Target blockstate-model id → dynamic model factory (input = the freshly baked
	 * fallback model). Concurrent because registration runs on the mod thread while
	 * {@link #onModifyBakingResult} fires on a resource-reload worker thread.
	 */
	private static final Map<ResourceLocation, Function<BakedModel, BakedModel>> DYNAMIC_MODEL_FACTORIES =
			new ConcurrentHashMap<>();

	private GTRenderModelListener() {
	}

	/**
	 * Registers the replacement of the baked model at {@code aTargetModelId} with the
	 * factory's dynamic model (the factory receives the original baked model and must
	 * wrap it as its fallback). Idempotent per id — the last registration wins, matching
	 * the map-replacement semantics of the baking result.
	 */
	public static void registerDynamicModel(ResourceLocation aTargetModelId, Function<BakedModel, BakedModel> aFactory) {
		DYNAMIC_MODEL_FACTORIES.put(aTargetModelId, aFactory);
	}

	/** Registration count (smoke tests / diagnostics). */
	public static int registeredCount() {
		return DYNAMIC_MODEL_FACTORIES.size();
	}

	/** Test seam: clears the factory table (registry is JVM-global across tests). */
	public static void clearForTest() {
		DYNAMIC_MODEL_FACTORIES.clear();
	}

	@SubscribeEvent
	public static void onModifyBakingResult(ModelEvent.ModifyBakingResult aEvent) {
		// Worker thread — only the model registry map is legal to touch here
		// (ModelEvent.java:40-43). A target whose baked model is absent (typo, datagen
		// gap, conditional model) is skipped silently: replacing nothing is the safe
		// degradation, and the blockstate JSON keeps rendering vanilla.
		DYNAMIC_MODEL_FACTORIES.forEach((tTargetModelId, tFactory) -> {
			BakedModel tBaked = aEvent.getModels().get(tTargetModelId);
			if (tBaked != null) {
				aEvent.getModels().put(tTargetModelId, tFactory.apply(tBaked));
			}
		});
	}
}
