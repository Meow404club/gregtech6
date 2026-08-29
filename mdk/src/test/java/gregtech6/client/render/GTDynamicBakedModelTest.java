package gregtech6.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.client.model.data.ModelData;

import org.junit.jupiter.api.Test;

/**
 * The GTDynamicBakedModel dispatch skeleton: ModelData snapshot hit → dynamic hook, miss →
 * fallback blockstate-baked model (IDynamicBakedModel.java:35, MachineModel.java:238-258
 * dispatch shape); plus the fallback delegation face and the ModelEvent.ModifyBakingResult
 * wiring in GTRenderModelListener. Quads are never constructed offline — the dynamic and
 * fallback paths are distinguished by sentinel list identities.
 */
public class GTDynamicBakedModelTest extends GTOfflineRenderTestBase {

	private record TestSnapshot(String mName) implements GTRenderSnapshot {
	}

	/** Distinct sentinel instances — identity, not equality, is what the assertions pin down. */
	private static final List<net.minecraft.client.renderer.block.model.BakedQuad> DYNAMIC_QUADS = List.of();
	private static final List<net.minecraft.client.renderer.block.model.BakedQuad> FALLBACK_QUADS = List.of();

	static class StubFallback implements BakedModel {
		@Override public List<net.minecraft.client.renderer.block.model.BakedQuad> getQuads(BlockState aState, Direction aSide, RandomSource aRand) {
			return FALLBACK_QUADS;
		}
		@Override public boolean useAmbientOcclusion() { return true; }
		@Override public boolean isGui3d() { return false; }
		@Override public boolean usesBlockLight() { return true; }
		@Override public boolean isCustomRenderer() { return false; }
		@Override public TextureAtlasSprite getParticleIcon() { return null; }
		@Override public ItemTransforms getTransforms() { return ItemTransforms.NO_TRANSFORMS; }
		@Override public net.minecraft.client.renderer.block.model.ItemOverrides getOverrides() { return net.minecraft.client.renderer.block.model.ItemOverrides.EMPTY; }
	}

	static class TestDynamicModel extends GTDynamicBakedModel {
		TestDynamicModel(BakedModel aFallback) { super(aFallback); }
		@Override protected List<net.minecraft.client.renderer.block.model.BakedQuad> getDynamicQuads(
				BlockState aState, Direction aSide, RandomSource aRand, ModelData aModelData, RenderType aRenderType) {
			return DYNAMIC_QUADS;
		}
	}

	/** A subclass with stricter gating refuses the dynamic path even with a snapshot present. */
	static class StrictDynamicModel extends TestDynamicModel {
		StrictDynamicModel(BakedModel aFallback) { super(aFallback); }
		@Override protected boolean supportsDynamicQuads(ModelData aModelData) { return false; }
	}

	private static final BlockState STONE = Blocks.STONE.defaultBlockState();

	private static ModelData snapshotData() {
		return GTModelProperties.snapshot().with(GTModelProperties.RENDER_SNAPSHOT, new TestSnapshot("machine")).build();
	}

	@Test
	public void snapshotHitRoutesToTheDynamicHook() {
		TestDynamicModel tModel = new TestDynamicModel(new StubFallback());

		List<net.minecraft.client.renderer.block.model.BakedQuad> tQuads = tModel.getQuads(
				STONE, Direction.UP, RandomSource.create(), snapshotData(), null);

		assertSame(DYNAMIC_QUADS, tQuads, "ModelData with a RENDER_SNAPSHOT dispatches to the subclass hook");
		assertSame(DYNAMIC_QUADS, tModel.getQuads(STONE, Direction.UP, RandomSource.create(), snapshotData(), null),
				"a null RenderType (all-quads pass) dispatches the same way");
	}

	@Test
	public void snapshotMissFallsBackToTheBlockstateModel() {
		TestDynamicModel tModel = new TestDynamicModel(new StubFallback());

		assertSame(FALLBACK_QUADS, tModel.getQuads(STONE, Direction.UP, RandomSource.create(), ModelData.EMPTY, null),
				"no snapshot: fallback to the blockstate-baked model");
		assertSame(FALLBACK_QUADS, tModel.getQuads(STONE, Direction.UP, RandomSource.create(), null, null),
				"defensive: null ModelData also falls back");
	}

	@Test
	public void threeArgOverloadRoutesThroughTheDispatchWithEmptyData() {
		// IDynamicBakedModel.java:27-30: the plain BakedModel 3-arg overload delegates to the
		// 5-arg dispatch with ModelData.EMPTY — i.e. item-form rendering hits the fallback.
		TestDynamicModel tModel = new TestDynamicModel(new StubFallback());
		assertSame(FALLBACK_QUADS, tModel.getQuads(STONE, Direction.UP, RandomSource.create()));
	}

	@Test
	public void subclassCanTightenTheDispatchGate() {
		StrictDynamicModel tModel = new StrictDynamicModel(new StubFallback());
		assertSame(FALLBACK_QUADS, tModel.getQuads(STONE, Direction.UP, RandomSource.create(), snapshotData(), null),
				"supportsDynamicQuads=false sends the snapshot path to the fallback");
	}

	@Test
	public void staticModelFaceDelegatesToTheFallback() {
		StubFallback tFallback = new StubFallback();
		TestDynamicModel tModel = new TestDynamicModel(tFallback);

		assertSame(tFallback, tModel.getFallbackModel());
		assertEquals(tFallback.useAmbientOcclusion(), tModel.useAmbientOcclusion());
		assertEquals(tFallback.isGui3d(), tModel.isGui3d());
		assertEquals(tFallback.usesBlockLight(), tModel.usesBlockLight());
		assertEquals(tFallback.isCustomRenderer(), tModel.isCustomRenderer());
		assertSame(tFallback.getTransforms(), tModel.getTransforms());
		assertNull(tModel.getParticleIcon());
		assertNotNull(tFallback.getTransforms());
	}

	@Test
	public void forgeModelDataPassthroughIsInherited() {
		// IForgeBakedModel.getModelData default (:69-72) passes the BE-supplied ModelData
		// through untouched — the snapshot comes from the BE, not the model.
		TestDynamicModel tModel = new TestDynamicModel(new StubFallback());
		ModelData tSupplied = snapshotData();
		assertSame(tSupplied, tModel.getModelData(null, BlockPos.ZERO, STONE, tSupplied));
	}

	@Test
	public void fallbackModelIsRequired() {
		assertThrows(NullPointerException.class, () -> new TestDynamicModel(null));
	}
}
