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
 * <p>CONSUMER TEMPLATE (W3 p4-cover-core; the key shape corrected to the per-state
 * form by p10-cover-plate-perstate-fix — a variant block's registration is its FULL
 * per-state key set, not one entry):
 * <pre>{@code
 * // during mod construction (before the first resource reload):
 * GTRenderModelListener.registerDynamicModel(
 *     new ModelResourceLocation("gt6", "oven", "active=true,facing=north,running=false"),
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
 * {@link GTOvenClientListener} for the oven's 16-key form); a model-file id (the
 * template's pre-p10 form, landed until p10-cover-plate-perstate-fix) silently
 * degrades (the absent-target skip below).
 */
@Mod.EventBusSubscriber(modid = GTRenderModelListener.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTRenderModelListener {

	public static final String MOD_ID = "gt6";

	/**
	 * Target key string → dynamic model factory (input = the freshly baked fallback model).
	 * Concurrent because registration runs on the mod thread while
	 * {@link #onModifyBakingResult} fires on a resource-reload worker thread.
	 *
	 * <p>The key is the target id's {@code toString()} form — leg-neutral by design:
	 * 1.20.1 keys the event map by {@code ResourceLocation} (a supertype of
	 * ModelResourceLocation), 1.21.1 keys it by the ModelResourceLocation <em>record</em>
	 * (which no longer extends ResourceLocation and drops getNamespace/getPath). Both
	 * types stringify to the same {@code ns:path[#variant]} shape on both legs, so the
	 * lookup is a plain String get off the baked key.
	 */
	private static final Map<String, Function<BakedModel, BakedModel>> DYNAMIC_MODEL_FACTORIES =
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
		registerDynamicModel(aTargetModelId.toString(), aFactory);
	}

	/**
	 * The String-key form (the ModelResourceLocation targets arrive here via
	 * {@code toString()} — the record form of 1.21.1 is not a ResourceLocation, so the
	 * overloads keep every caller leg-compilable without per-caller forks).
	 */
	public static void registerDynamicModel(String aTargetModelId, Function<BakedModel, BakedModel> aFactory) {
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
		// (ModelEvent.java:40-43). Iteration runs over the BAKED map (not the factory
		// table): a registered target whose baked model is absent (typo, datagen gap,
		// conditional model) is skipped silently — replacing nothing is the safe
		// degradation, and the blockstate JSON keeps rendering vanilla. The key match is
		// String-based (see DYNAMIC_MODEL_FACTORIES): leg-neutral across the 1.20.1
		// ResourceLocation keys and the 1.21.1 ModelResourceLocation record keys.
		aEvent.getModels().forEach((tBakedKey, tBaked) -> {
			Function<BakedModel, BakedModel> tFactory = DYNAMIC_MODEL_FACTORIES.get(tBakedKey.toString());
			if (tFactory != null) {
				aEvent.getModels().put(tBakedKey, tFactory.apply(tBaked));
			}
		});
	}
}
