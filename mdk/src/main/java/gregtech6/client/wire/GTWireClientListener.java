package gregtech6.client.wire;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.common.Mod;

import gregtech6.client.render.GTRenderModelListener;
import gregtech6.registry.GTWireSpecs;

/**
 * The client-side wiring of the wire family models (task p9-wire-family-w2, the
 * {@code GTPipeFlowClientListener} shape: card-local {@code @EventBusSubscriber},
 * GT6Mod/GTModBusListener untouched; Dist.CLIENT — the dedicated server never loads this
 * class). Two jobs:
 *
 * <p>① PARAM TABLE (main-thread, at {@link FMLClientSetupEvent} — after registration, before
 * the first resource reload): every wire block registry path → its immutable
 * {@link GTWireBakedModel.Params} (borrowed texture set, insulated form, PX_P diameter).
 * The paths come from the {@link GTWireSpecs} table (the W1 registration is driven by the
 * same rows, so path drift is structurally impossible) plus the two legacy p7 anchors —
 * and, since task p11-wire-fiber-texture, the laser row ({@code wire_laser}, the fixed
 * FIBER_WIRE+OVERLAY pair of MultiTileEntityWireLaser :121-122) and, since task
 * p11-wire-brightness, the six redstone rows (the electric form: the row's set sprite —
 * all three materials resolve to the borrowed copper set — with the insulation layers on
 * the cable form).
 *
 * <p>② BAKE DISPATCH ({@link ModelEvent.ModifyBakingResult}, the only Forge hook whose
 * model map is still modifiable after baking — the {@link GTRenderModelListener} discipline):
 * replaces the baked model of every wire key with one cached {@link GTWireBakedModel} per
 * block. The keys are the per-state ModelResourceLocations
 * ({@code gt6:<path>#connections=<0..63>}) AND the plain item-model id
 * ({@code gt6:<path>}) — vanilla ModelBakery loads one top-level model per BLOCK STATE
 * (ModelBakery.java:136) and a separate one per item, and the blockstate JSON's model-file
 * paths never appear as keys (the p9-render-c-oven-overlay dispatch-key finding). Wrapping
 * the item key too gives the inventory form the upstream {@code worldObj == null} N-S
 * segment (GTWireBakedModel.ITEM_MASK).
 */
@Mod.EventBusSubscriber(modid = GTRenderModelListener.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTWireClientListener {

	/** Registry path → render params (built once, read on bake worker threads). */
	private static final Map<String, GTWireBakedModel.Params> PARAMS = new ConcurrentHashMap<>();

	private static volatile boolean sBuilt = false;

	private GTWireClientListener() {
	}

	/** The two legacy p7 anchors (material-less, the p7 placeholder texture, upstream-default thin form). */
	public static final String LEGACY_1X = "wire_electric_1x";
	public static final String LEGACY_2X = "wire_electric_2x";

	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent aEvent) {
		buildParams();
	}

	@SubscribeEvent
	public static void onModifyBakingResult(ModelEvent.ModifyBakingResult aEvent) {
		if (!sBuilt) buildParams(); // ordering guard — client setup always ran first; belt and suspenders
		Map<String, GTWireBakedModel> tModels = new ConcurrentHashMap<>();
		// var for the entry: 1.20.1 keys the map by ResourceLocation, 1.21.1 by the
		// ModelResourceLocation record (no longer a ResourceLocation — no getNamespace/
		// getPath). The key is read through its toString ("ns:path[#variant]" on BOTH
		// legs), path = the segment before the optional '#' variant separator.
		for (var tEntry : aEvent.getModels().entrySet()) {
			String tKeyString = tEntry.getKey().toString();
			if (!tKeyString.startsWith(GTRenderModelListener.MOD_ID + ":")) continue;
			String tPath = tKeyString.substring(GTRenderModelListener.MOD_ID.length() + 1).split("#", 2)[0];
			GTWireBakedModel.Params tParams = PARAMS.get(tPath);
			if (tParams == null) continue;
			// one model instance per block — the per-state keys and the item key share the
			// same fallback ancestry (both descend from the same shared set model), so the
			// static-property delegation is identical whichever key built it first
			tEntry.setValue(tModels.computeIfAbsent(tPath,
					tP -> new GTWireBakedModel(tEntry.getValue(), tParams)));
		}
	}

	/**
	 * The param table (idempotent, test-callable). Materials dereference here is safe:
	 * MT.init ran during mod loading, long before client setup / the first bake.
	 *
	 * <p>Task p11-wire-fiber-texture: the LASER rows join the table (the p10 placeholder
	 * card left them off — the {@code wire_laser} blockstate fell back to the shared JSON
	 * cube). Task p11-wire-brightness: the SIX REDSTONE rows join too (the last family on
	 * the JSON fallback) — every per-state key {@code gt6:wire_red_alloy#connections=0..63}
	 * et al AND the item keys now bake into the {@link GTWireBakedModel} electric form:
	 * the row's set sprite (all three materials land on the borrowed copper set, the same
	 * source the datagen shared model used), the insulation layer set on the cable form
	 * (tint index 1 — the redstone jacket colour rides {@link GTWireTint} now). The
	 * upstream bare-wire fullbright flag {@code mState > 0} (WireRedstone :81-82) is a
	 * DECLARED DEVIATION — see the model javadoc.
	 */
	public static synchronized void buildParams() {
		if (sBuilt) return;
		PARAMS.put(LEGACY_1X, new GTWireBakedModel.Params(GTWireTextures.legacySprite(), false, 0));
		PARAMS.put(LEGACY_2X, new GTWireBakedModel.Params(GTWireTextures.legacySprite(), false, 0));
		for (GTWireSpecs.Variant tVariant : GTWireSpecs.variants()) {
			PARAMS.put(GTWireSpecs.registryName(tVariant), new GTWireBakedModel.Params(
					GTWireTextures.wireSprite(GTWireTextures.blockSetOf(tVariant.row().material().get())),
					tVariant.insulated(), tVariant.diameter()));
		}
		// task p11 — the laser pair: base FIBER_WIRE (tint 0 = the mRGBa dye, the row
		// material MT.NULL) + untinted FIBER_WIRE_OVERLAY (WireLaser :121-122, no glow).
		for (GTWireSpecs.Variant tVariant : GTWireSpecs.laserVariants()) {
			PARAMS.put(GTWireSpecs.registryName(tVariant), new GTWireBakedModel.Params(
					GTWireTextures.fiberSprite(), GTWireTextures.fiberOverlaySprite(),
					tVariant.insulated(), tVariant.diameter()));
		}
		// task p11 — the redstone family: the same electric form over the row's set sprite
		// (the upstream texture picks are the material wire icon :81-82 bare / the
		// INSULATION_FULL + tier jacket :184-185 cable — both the port's electric planner
		// shape; the jacket COLOUR is the per-family tint, GTWireTint's business).
		for (GTWireSpecs.Variant tVariant : GTWireSpecs.redstoneVariants()) {
			PARAMS.put(GTWireSpecs.registryName(tVariant), new GTWireBakedModel.Params(
					GTWireTextures.wireSprite(GTWireTextures.blockSetOf(tVariant.row().material().get())),
					tVariant.insulated(), tVariant.diameter()));
		}
		sBuilt = true;
	}

	/** The params of one registry path (tests / diagnostics). */
	@Nullable
	public static GTWireBakedModel.Params paramsFor(String aRegistryPath) {
		return PARAMS.get(aRegistryPath);
	}

	/** The table size — 620 electric rows + 6 redstone rows + 1 laser row + 2 legacy anchors (smoke assertion). */
	public static int paramsCount() {
		return PARAMS.size();
	}

	/** Test seam: clears the table (registry is JVM-global across tests). */
	public static void clearForTest() {
		PARAMS.clear();
		sBuilt = false;
	}
}
