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
 * The client-side wiring of the oven overlay model (task p9-render-c-oven-overlay, the
 * {@link GTPipeFlowClientListener} shape: card-local {@code @EventBusSubscriber},
 * GT6Mod/GTModBusListener untouched; Dist.CLIENT — the dedicated server never loads this
 * class). Registration runs at mod construct, strictly before the first resource reload
 * that fires {@code ModelEvent.ModifyBakingResult} (GTRenderModelListener class doc).
 *
 * <p>Dispatch keys are the oven's 16 <b>per-state</b> ModelResourceLocations
 * ({@code gt6:oven#active=…,facing=…,running=…}) — vanilla ModelBakery loads one
 * top-level model per BLOCK STATE (ModelBakery.java:136
 * {@code loadTopLevel(BlockModelShaper.stateToModelLocation(...))}), so the map the
 * ModifyBakingResult event exposes is keyed per state and never by the blockstate JSON's
 * model-file paths ({@code gt6:block/oven} is only an unbaked dependency). The variant
 * string order is the StateDefinition's name-sorted property order
 * ({@code active, facing, running}) — the same strings the generated blockstate JSON
 * uses. Registering the full 16-key set keeps the baked per-state models as fallbacks,
 * so each state's A-tier model (with its own front texture and y rotation) stays the
 * material layer of exactly that state.
 */
@Mod.EventBusSubscriber(modid = GTRenderModelListener.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTOvenClientListener {

	/** The oven block registry path (GTMachines.OVEN — the generated blockstates/oven.json id). */
	public static final String OVEN_BLOCK_PATH = "oven";

	private GTOvenClientListener() {
	}

	/** The 16 per-state target keys (2 active x 4 facing x 2 running), variant string alphabetical. */
	public static List<ModelResourceLocation> targetModelIds() {
		List<ModelResourceLocation> rTargets = new ArrayList<>(16);
		for (boolean tActive : new boolean[] {false, true}) {
			for (String tFacing : new String[] {"north", "south", "west", "east"}) {
				for (boolean tRunning : new boolean[] {false, true}) {
					// 1.20.1: (namespace, path, variant) String triple; 1.21.1: the record
					// ctor is (ResourceLocation id, String variant) — the record no longer
					// extends ResourceLocation (javap compiledWithNeoForge 21.1 jar)
					String tVariant = "active=" + tActive + ",facing=" + tFacing + ",running=" + tRunning;
					//? if forge {
					rTargets.add(new ModelResourceLocation(GTRenderModelListener.MOD_ID, OVEN_BLOCK_PATH, tVariant));
					//? } else {
					/*rTargets.add(new ModelResourceLocation(
							new ResourceLocation(GTRenderModelListener.MOD_ID, OVEN_BLOCK_PATH), tVariant));*/
					//? }
				}
			}
		}
		return rTargets;
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
			GTRenderModelListener.registerDynamicModel(tTarget.toString(), GTOvenOverlayModel::new);
		}
	}
}
