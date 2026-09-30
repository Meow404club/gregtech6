package gregtech6.client.render;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * The client-side wiring of the boiler barometer model (task boiler-barometer, the
 * {@link GTOvenClientListener} shape: card-local {@code @EventBusSubscriber},
 * GT6Mod/GTModBusListener untouched; Dist.CLIENT — the dedicated server never loads this
 * class). Registration runs at mod construct, strictly before the first resource reload
 * that fires {@code ModelEvent.ModifyBakingResult} (GTRenderModelListener class doc).
 *
 * <p>Dispatch keys are the boiler ladder's <b>per-state</b> ModelResourceLocations —
 * vanilla ModelBakery loads one top-level model per BLOCK STATE (ModelBakery.java:136
 * {@code loadTopLevel(BlockModelShaper.stateToModelLocation(...))}), so the map the
 * ModifyBakingResult event exposes is keyed per state and never by the blockstate JSON's
 * model-file paths. Variant strings are the StateDefinition name-sorted order:
 * {@code facing=N} for the 26 tank rows (the FACING-only carrier) and
 * {@code facing=N,formed=B} for the 5 large boiler rows (the FACING+FORMED carrier) —
 * the same strings the generated blockstate JSONs use. Registering the full per-state key
 * set keeps the baked per-state models as fallbacks, so each state's two-layer
 * r8-tex-large-boilers model (with its own front art and y rotation) stays the material
 * layer of exactly that state, and the gauge quads stack on top per snapshot.
 */
@Mod.EventBusSubscriber(modid = GTRenderModelListener.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTBoilerClientListener {

	private GTBoilerClientListener() {
	}

	/**
	 * The per-state target keys: 26 tank rows x 4 facing + 5 large rows x 4 facing x 2
	 * formed = 124 (the rows are the static registration tables — no RegistryObject access,
	 * the construct-time-safe path the GTOvenClientListener static-paths form generalizes).
	 */
	public static List<ModelResourceLocation> targetModelIds() {
		List<ModelResourceLocation> rTargets = new ArrayList<>(124);
		for (var tRow : gregtech6.registry.GT6Boilers.allRows()) {
			for (String tFacing : new String[] {"north", "south", "west", "east"}) {
				rTargets.add(modelId(tRow.path(), "facing=" + tFacing));
			}
		}
		for (var tRow : gregtech6.registry.GTMultiBlocks.LARGE_BOILER_ROWS) {
			for (String tFacing : new String[] {"north", "south", "west", "east"}) {
				for (boolean tFormed : new boolean[] {false, true}) {
					rTargets.add(modelId(tRow.path(), "facing=" + tFacing + ",formed=" + tFormed));
				}
			}
		}
		return rTargets;
	}

	/** The per-state key for one block path and variant string (the oven leg-neutral form). */
	private static ModelResourceLocation modelId(String aPath, String aVariant) {
		// 1.20.1: (namespace, path, variant) String triple; 1.21.1: the record ctor is
		// (ResourceLocation id, String variant) — the record no longer extends
		// ResourceLocation (javap compiledWithNeoForge 21.1 jar)
		//? if forge {
		return new ModelResourceLocation(GTRenderModelListener.MOD_ID, aPath, aVariant);
		//? } else {
		/*return new ModelResourceLocation(
				new ResourceLocation(GTRenderModelListener.MOD_ID, aPath), aVariant);*/
		//? }
	}

	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		register();
	}

	/** Idempotent registration (also the test hook — the listener class never loads in tests implicitly). */
	public static void register() {
		// the String-key overload of GTRenderModelListener (the 1.21.1 MRL record is not a
		// ResourceLocation, so the table is keyed by toString() — leg-neutral)
		for (ModelResourceLocation tTarget : targetModelIds()) {
			GTRenderModelListener.registerDynamicModel(tTarget.toString(), GTBoilerBarometerModel::new);
		}
	}
}
