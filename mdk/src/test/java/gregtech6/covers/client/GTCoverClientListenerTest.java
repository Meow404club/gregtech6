package gregtech6.covers.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.client.event.ModelEvent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import gregtech6.client.render.GTOvenClientListener;
import gregtech6.client.render.GTRenderModelListener;

/**
 * The cover plate registration sentinel tests (task p10-cover-plate-perstate-fix, the
 * {@code GTOvenOverlayModelTest.registrationWrapsTheBakedPerStateModels} shape): the
 * listener's targets are the oven's 16 per-state ModelResourceLocations (read-only reuse
 * of {@link GTOvenClientListener#targetModelIds()} — vanilla ModelBakery.java:136 keys
 * the baking map per BLOCK STATE, so the pre-p10 model-file ids
 * {@code block/oven[,_active,_running]} never matched anything and every wrap silently
 * degraded through the GTRenderModelListener.java:97-99 absent-target skip). Pins: the
 * key set, the full 16-key wrap with the fallback preserved, the dead keys staying
 * unwrapped, and the registeredCount parity with the oven listener on the shared
 * last-wins table.
 */
public class GTCoverClientListenerTest {

	/** The pre-p10 dead keys (blockstate JSON model-file paths — never baking-map keys). */
	private static final List<String> DEAD_FILE_IDS = List.of("block/oven", "block/oven_active", "block/oven_running");

	@AfterEach
	void clearRegistration() {
		GTRenderModelListener.clearForTest();
	}

	// ---------------------------------------------------------------------------
	// the key set — read-only reuse of the oven listener's 16 per-state keys
	// ---------------------------------------------------------------------------

	@Test
	void targetsAreTheOvenPerStateKeys() {
		List<ModelResourceLocation> tTargets = GTCoverClientListener.TARGET_MODELS;
		assertEquals(16, tTargets.size(), "2 active x 4 facing x 2 running");
		assertEquals(16, new HashSet<>(tTargets).size(), "all keys distinct");
		assertEquals(GTOvenClientListener.targetModelIds(), tTargets,
				"the cover targets ARE the oven overlay's per-state keys (read-only reuse, zero drift)");
		// variant string = the StateDefinition name-sorted property order (active, facing, running)
		assertTrue(tTargets.contains(new ModelResourceLocation("gt6", "oven", "active=false,facing=north,running=false")));
		assertTrue(tTargets.contains(new ModelResourceLocation("gt6", "oven", "active=true,facing=east,running=true")));
		assertFalse(tTargets.contains(new ModelResourceLocation("gt6", "oven", "facing=north,active=false,running=false")),
				"the misordered variant string is NOT a key (StateDefinition sorts by name)");
		for (ModelResourceLocation tTarget : tTargets) {
			//? if forge {
			assertEquals("gt6", tTarget.getNamespace());
			assertEquals("oven", tTarget.getPath(),
			//?} else {
			/*assertEquals("gt6", tTarget.id().getNamespace()); // 21.1: MRL is a record over an RL id
			assertEquals("oven", tTarget.id().getPath(),
			*///?}
					"per-state keys address the BLOCK (gt6:oven#variants), not the blockstate JSON's model files (block/oven…)");
		}
	}

	// ---------------------------------------------------------------------------
	// the wrap — all 16 per-state keys wrap their baked model, dead keys stay vanilla
	// ---------------------------------------------------------------------------

	@Test
	void registrationWrapsThePerStateKeysNotTheFileIds() {
		GTCoverClientListener.register();
		//? if forge {
		Map<ResourceLocation, BakedModel> tStubs = new HashMap<>();
		Map<ResourceLocation, BakedModel> tModels = new HashMap<>();
		//?} else {
		/*// 21.1: the baking-result table keys on the MRL record (javap ModelEvent$ModifyBakingResult).
		Map<ModelResourceLocation, BakedModel> tStubs = new HashMap<>();
		Map<ModelResourceLocation, BakedModel> tModels = new HashMap<>();
		*///?}
		for (ModelResourceLocation tKey : GTCoverClientListener.TARGET_MODELS) {
			BakedModel tStub = new StubFallback();
			tStubs.put(tKey, tStub);
			tModels.put(tKey, tStub);
		}
		for (String tDead : DEAD_FILE_IDS) {
			BakedModel tStub = new StubFallback();
			//? if forge {
			tStubs.put(new ResourceLocation("gt6", tDead), tStub);
			tModels.put(new ResourceLocation("gt6", tDead), tStub);
			//?} else {
			/*tStubs.put(ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("gt6", tDead)), tStub);
			tModels.put(ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath("gt6", tDead)), tStub);
			*///?}
		}
		assertEquals(19, tModels.size());

		//? if forge {
		GTRenderModelListener.onModifyBakingResult(new ModelEvent.ModifyBakingResult(tModels, null));
		//?} else {
		/*GTRenderModelListener.onModifyBakingResult(new ModelEvent.ModifyBakingResult(tModels, null, null)); // 21.1: +ModelBakery
		*///?}

		for (ModelResourceLocation tKey : GTCoverClientListener.TARGET_MODELS) {
			assertTrue(tModels.get(tKey) instanceof CoverPlateModel, "every per-state baked model is wrapped: " + tKey);
			assertSame(tStubs.get(tKey), ((CoverPlateModel) tModels.get(tKey)).getFallbackModel(),
					"the baked model stays as the fallback (A-tier material layer): " + tKey);
		}
		for (String tDead : DEAD_FILE_IDS) {
			assertTrue(tModels.get(new ResourceLocation("gt6", tDead)) instanceof StubFallback,
					"the pre-p10 file-id key is not a registration key — its stub stays unwrapped: " + tDead);
		}
	}

	// ---------------------------------------------------------------------------
	// the parity — the cover keys land ON the oven keys (last-wins table, no dead key)
	// ---------------------------------------------------------------------------

	@Test
	void registeredCountHasParityWithTheOvenListener() {
		GTOvenClientListener.register();
		int tAfterOven = GTRenderModelListener.registeredCount();
		assertEquals(16, tAfterOven, "the oven overlay's 16 per-state keys");

		GTCoverClientListener.register();
		assertEquals(tAfterOven, GTRenderModelListener.registeredCount(),
				"parity: the cover keys coincide with the oven's 16 per-state keys — no dead key survives on the shared table (the pre-p10 file-id ids would make this 19)");

		// last-wins on the shared keys (GTCoverClientListener SHARED KEYS note): the
		// cover registration replaced the oven factory per key — offline the call order
		// decides, in-game Forge's unsorted @EventBusSubscriber scan order
		// (AutomaticEventSubscriber.java:32-39) does.
		ModelResourceLocation tKey = new ModelResourceLocation("gt6", "oven", "active=true,facing=north,running=false");
		//? if forge {
		Map<ResourceLocation, BakedModel> tModels = new HashMap<>();
		//?} else {
		/*Map<ModelResourceLocation, BakedModel> tModels = new HashMap<>();
		*///?}
		BakedModel tStub = new StubFallback();
		tModels.put(tKey, tStub);
		//? if forge {
		GTRenderModelListener.onModifyBakingResult(new ModelEvent.ModifyBakingResult(tModels, null));
		//?} else {
		/*GTRenderModelListener.onModifyBakingResult(new ModelEvent.ModifyBakingResult(tModels, null, null)); // 21.1: +ModelBakery
		*///?}
		assertTrue(tModels.get(tKey) instanceof CoverPlateModel, "the last registration owns the shared key");
		assertSame(tStub, ((CoverPlateModel) tModels.get(tKey)).getFallbackModel());
	}

	/** Minimal BakedModel stub (the GTOvenOverlayModelTest probe shape). */
	public static final class StubFallback implements BakedModel {
		@Override public List<net.minecraft.client.renderer.block.model.BakedQuad> getQuads(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.core.Direction aSide, net.minecraft.util.RandomSource aRand) { return List.of(); }
		@Override public boolean useAmbientOcclusion() { return false; }
		@Override public boolean isGui3d() { return false; }
		@Override public boolean usesBlockLight() { return false; }
		@Override public boolean isCustomRenderer() { return false; }
		@Override public net.minecraft.client.renderer.texture.TextureAtlasSprite getParticleIcon() { return null; }
		@Override public net.minecraft.client.renderer.block.model.ItemTransforms getTransforms() { return net.minecraft.client.renderer.block.model.ItemTransforms.NO_TRANSFORMS; }
		@Override public net.minecraft.client.renderer.block.model.ItemOverrides getOverrides() { return net.minecraft.client.renderer.block.model.ItemOverrides.EMPTY; }
	}
}
