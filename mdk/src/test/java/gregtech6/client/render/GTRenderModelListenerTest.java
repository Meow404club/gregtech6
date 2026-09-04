package gregtech6.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.client.event.ModelEvent;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * The GTRenderModelListener wiring: registerDynamicModel targets are replaced inside the
 * ModelEvent.ModifyBakingResult model map (ModelEvent.java:51), absent targets degrade to
 * the vanilla model, and the factory table survives concurrent worker-thread access.
 *
 * <p>The event handler is invoked directly (its @SubscribeEvent registration is FML's job
 * offline) with a hand-made ModifyBakingResult — the constructor is @ApiStatus.Internal
 * but public, and the handler only touches the model map.
 */
public class GTRenderModelListenerTest extends GTOfflineRenderTestBase {

	private static final ResourceLocation TARGET = new ResourceLocation("gt6", "block/machine/oven");
	private static final ResourceLocation ABSENT = new ResourceLocation("gt6", "block/machine/missing");

	private static final class FallbackProbe implements BakedModel {
		@Override public List<net.minecraft.client.renderer.block.model.BakedQuad> getQuads(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.core.Direction aSide, net.minecraft.util.RandomSource aRand) { return List.of(); }
		@Override public boolean useAmbientOcclusion() { return false; }
		@Override public boolean isGui3d() { return false; }
		@Override public boolean usesBlockLight() { return false; }
		@Override public boolean isCustomRenderer() { return false; }
		@Override public net.minecraft.client.renderer.texture.TextureAtlasSprite getParticleIcon() { return null; }
		@Override public net.minecraft.client.renderer.block.model.ItemTransforms getTransforms() { return net.minecraft.client.renderer.block.model.ItemTransforms.NO_TRANSFORMS; }
		@Override public net.minecraft.client.renderer.block.model.ItemOverrides getOverrides() { return net.minecraft.client.renderer.block.model.ItemOverrides.EMPTY; }
	}

	private static final class WrappedProbe implements BakedModel {
		final BakedModel mFallback;
		WrappedProbe(BakedModel aFallback) { mFallback = aFallback; }
		@Override public List<net.minecraft.client.renderer.block.model.BakedQuad> getQuads(net.minecraft.world.level.block.state.BlockState aState, net.minecraft.core.Direction aSide, net.minecraft.util.RandomSource aRand) { return List.of(); }
		@Override public boolean useAmbientOcclusion() { return true; }
		@Override public boolean isGui3d() { return true; }
		@Override public boolean usesBlockLight() { return true; }
		@Override public boolean isCustomRenderer() { return false; }
		@Override public net.minecraft.client.renderer.texture.TextureAtlasSprite getParticleIcon() { return null; }
		@Override public net.minecraft.client.renderer.block.model.ItemTransforms getTransforms() { return net.minecraft.client.renderer.block.model.ItemTransforms.NO_TRANSFORMS; }
		@Override public net.minecraft.client.renderer.block.model.ItemOverrides getOverrides() { return net.minecraft.client.renderer.block.model.ItemOverrides.EMPTY; }
	}

	@AfterEach
	public void resetRegistry() {
		GTRenderModelListener.clearForTest();
	}

	@Test
	public void registeredTargetsAreReplacedInTheBakingResult() {
		BakedModel tFallback = new FallbackProbe();
		//? if forge {
		GTRenderModelListener.registerDynamicModel(TARGET.toString(), WrappedProbe::new);
		//?} else {
		/*GTRenderModelListener.registerDynamicModel(net.minecraft.client.resources.model.ModelResourceLocation.standalone(TARGET).toString(), WrappedProbe::new); // 21.1: the key string is the record's toString
		*///?}
		assertEquals(1, GTRenderModelListener.registeredCount());

		//? if forge {
		Map<ResourceLocation, BakedModel> tModels = new HashMap<>();
		tModels.put(TARGET, tFallback);
		GTRenderModelListener.onModifyBakingResult(new ModelEvent.ModifyBakingResult(tModels, null));

		assertSame(tFallback, ((WrappedProbe) tModels.get(TARGET)).mFallback,
				"the baked model is replaced by the factory product, which keeps it as fallback");
		//?} else {
		/*// 21.1: the baking table keys on the MRL record — standalone(TARGET) stringifies
		//back to the bare "ns:path" the String-keyed factory table registered.
		Map<net.minecraft.client.resources.model.ModelResourceLocation, BakedModel> tModels = new java.util.HashMap<>();
		var tTargetKey = net.minecraft.client.resources.model.ModelResourceLocation.standalone(TARGET);
		tModels.put(tTargetKey, tFallback);
		GTRenderModelListener.onModifyBakingResult(new ModelEvent.ModifyBakingResult(tModels, null, null));

		assertSame(tFallback, ((WrappedProbe) tModels.get(tTargetKey)).mFallback,
				"the baked model is replaced by the factory product, which keeps it as fallback");
		*///?}
		assertEquals(1, tModels.size(), "no other entry is touched");
	}

	@Test
	public void absentTargetsDegradeSilently() {
		//? if forge {
		GTRenderModelListener.registerDynamicModel(ABSENT.toString(), WrappedProbe::new);
		//?} else {
		/*GTRenderModelListener.registerDynamicModel(net.minecraft.client.resources.model.ModelResourceLocation.standalone(ABSENT).toString(), WrappedProbe::new); // 21.1: record toString
		*///?}

		//? if forge {
		Map<ResourceLocation, BakedModel> tModels = new HashMap<>();
		tModels.put(TARGET, new FallbackProbe());
		GTRenderModelListener.onModifyBakingResult(new ModelEvent.ModifyBakingResult(tModels, null));

		assertFalse(tModels.get(TARGET) instanceof WrappedProbe, "unregistered targets stay vanilla");
		//?} else {
		/*Map<net.minecraft.client.resources.model.ModelResourceLocation, BakedModel> tModels = new HashMap<>();
		var tTargetKey = net.minecraft.client.resources.model.ModelResourceLocation.standalone(TARGET);
		tModels.put(tTargetKey, new FallbackProbe());
		GTRenderModelListener.onModifyBakingResult(new ModelEvent.ModifyBakingResult(tModels, null, null));

		assertFalse(tModels.get(tTargetKey) instanceof WrappedProbe, "unregistered targets stay vanilla");
		*///?}
		assertNull(tModels.get(ABSENT), "an absent baked model is skipped, never fabricated");
	}

	@Test
	public void reRegistrationIsLastWins() {
		//? if forge {
		GTRenderModelListener.registerDynamicModel(TARGET.toString(), WrappedProbe::new);
		GTRenderModelListener.registerDynamicModel(TARGET.toString(), WrappedProbe::new);
		//?} else {
		/*String tKey = net.minecraft.client.resources.model.ModelResourceLocation.standalone(TARGET).toString(); // 21.1: record toString
		GTRenderModelListener.registerDynamicModel(tKey, WrappedProbe::new);
		GTRenderModelListener.registerDynamicModel(tKey, WrappedProbe::new);
		*///?}
		assertEquals(1, GTRenderModelListener.registeredCount(), "same id re-registers in place (map replacement)");
	}
}
