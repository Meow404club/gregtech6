package gregtech6.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

//? if forge {
import net.minecraftforge.client.textures.UnitTextureAtlasSprite;
//?} else {
/*import net.neoforged.neoforge.client.textures.UnitTextureAtlasSprite;*/
//?}
import net.minecraftforge.client.model.data.ModelData;

import gregapi.oredict.OreDictMaterial;
import gregtech6.registry.GTMaterialItems;

/**
 * The wrapper-path offline tests (task p32-render-embeddium-tint rework): the reviewer
 * verification upgrade asserts THE VERTEX DATA COLOUR CHANGES WITH THE MODEL DATA —
 * the full getQuads seam (model data in → retinted quads out) over real constructed
 * {@link BakedQuad}s, not just the multiplier math. The wrapper instance itself needs a
 * live Block for {@code materialOf}, so the tests drive the pure
 * {@link GTMachineTintModel#tintQuads} seam with the {@code tintARGB} outputs the
 * registered {@code getDynamicQuads} feeds it (the one-line wiring the
 * {@code GTMachinePaintTintTest} colours pin).
 */
class GTMachineTintModelTest extends GTOfflineRenderTestBase {

	private static final int STRIDE = 8;
	private static final int COLOR_SLOT = 3;

	@BeforeAll
	static void bootMaterials() {
		GTMaterialItems.initMaterials();
	}

	/** A baked-format body quad (32 ints, white vertex colours, tintIndex 0). */
	private static BakedQuad bodyQuad() {
		int[] tVertices = new int[4 * STRIDE];
		Arrays.fill(tVertices, 0xFFFFFFFF);
		return new BakedQuad(tVertices, 0, Direction.NORTH, UnitTextureAtlasSprite.INSTANCE, true);
	}

	/** A baked-format decal quad (untinted, the P22 overlay form). */
	private static BakedQuad decalQuad() {
		int[] tVertices = new int[4 * STRIDE];
		Arrays.fill(tVertices, 0xFF333333);
		return new BakedQuad(tVertices, -1, Direction.NORTH, UnitTextureAtlasSprite.INSTANCE, true);
	}

	/** The barest fallback model: returns exactly the quads the tests hand it. */
	private static BakedModel stubModel(final List<BakedQuad> aQuads) {
		return new BakedModel() {
			@Override
			public List<BakedQuad> getQuads(@org.jetbrains.annotations.Nullable BlockState aState,
					@org.jetbrains.annotations.Nullable Direction aSide,
					net.minecraft.util.RandomSource aRand) {
				return aQuads;
			}

			@Override
			public boolean useAmbientOcclusion() {
				return true;
			}

			@Override
			public boolean isGui3d() {
				return false;
			}

			@Override
			public boolean usesBlockLight() {
				return true;
			}

			@Override
			public boolean isCustomRenderer() {
				return false;
			}

			@Override
			public net.minecraft.client.renderer.texture.TextureAtlasSprite getParticleIcon() {
				return UnitTextureAtlasSprite.INSTANCE;
			}

			@Override
			public net.minecraft.client.renderer.block.model.ItemTransforms getTransforms() {
				return net.minecraft.client.renderer.block.model.ItemTransforms.NO_TRANSFORMS;
			}

			@Override
			public net.minecraft.client.renderer.block.model.ItemOverrides getOverrides() {
				return net.minecraft.client.renderer.block.model.ItemOverrides.EMPTY;
			}
		};
	}

	/** The bronze row colour (the unpainted ModelData arm) through the wrapper seam. */
	@Test
	void unpaintedModelDataTintsTheBodyQuadsThroughTheSeam() {
		OreDictMaterial tBronze = gregapi.data.MT.Bronze;
		int tTint = GTMachinePaintTint.tintARGB(ModelData.EMPTY, tBronze, 0);
		BakedQuad tBody = bodyQuad();
		BakedQuad tDecal = decalQuad();
		List<BakedQuad> tOut = GTMachineTintModel.tintQuads(List.of(tBody, tDecal), tTint,
				new ConcurrentHashMap<>());

		assertEquals(2, tOut.size(), "the quad count is preserved");
		BakedQuad tRetinted = tOut.get(0);
		assertNotEquals(tBody, tRetinted, "the body quad is a retinted copy");
		assertEquals(-1, tRetinted.getTintIndex(), "the retinted copy drops the tint index (no double tint)");
		int tColour = tRetinted.getVertices()[COLOR_SLOT];
		// the slot stores ABGR (issue #14): the tint's R lands at bits 7-0, B at 23-16
		assertEquals((tTint >> 16) & 255, tColour & 255, "red channel (ABGR slot 0)");
		assertEquals((tTint >> 8) & 255, (tColour >> 8) & 255, "green channel");
		assertEquals(tTint & 255, (tColour >> 16) & 255, "blue channel (ABGR slot 2)");
		assertSame(tDecal, tOut.get(1), "the untinted decal passes through as the shared instance");
	}

	/** THE reviewer verification upgrade: the vertex data colour CHANGES with the ModelData. */
	@Test
	void vertexColourFollowsTheModelData() {
		int tUnpainted = GTMachinePaintTint.tintARGB(ModelData.EMPTY, gregapi.data.MT.Bronze, 0);
		int tPaintedRed = GTMachinePaintTint.tintARGB(
				GTModelProperties.derive(ModelData.EMPTY).with(GTModelProperties.PAINT, 0xFF0000).build(),
				gregapi.data.MT.Bronze, 0);
		assertNotEquals(tUnpainted, tPaintedRed, "the two ModelData arms resolve different tints");

		Map<Integer, Map<BakedQuad, BakedQuad>> tCache = new ConcurrentHashMap<>();
		int[] tUnpaintedVertices = GTMachineTintModel.tintQuads(List.of(bodyQuad()), tUnpainted, tCache)
				.get(0).getVertices();
		int[] tPaintedVertices = GTMachineTintModel.tintQuads(List.of(bodyQuad()), tPaintedRed, tCache)
				.get(0).getVertices();

		int tUnpaintedColour = tUnpaintedVertices[COLOR_SLOT];
		int tPaintedColour = tPaintedVertices[COLOR_SLOT];
		assertNotEquals(tUnpaintedColour, tPaintedColour, "the vertex data colour follows the ModelData");
		// the unpainted arm = the bronze row colour, the painted arm = pure red; the slot
		// is ABGR (issue #14): R at bits 7-0, G at 15-8, B at 23-16
		assertEquals((tUnpainted >> 16) & 255, tUnpaintedColour & 255, "unpainted R (bronze)");
		assertEquals(255, tPaintedColour & 255, "painted R (red)");
		assertEquals(0, (tPaintedColour >> 8) & 255, "painted G (red)");
		assertEquals(0, (tPaintedColour >> 16) & 255, "painted B (red)");
	}

	/** The -1 identity returns the input list instance (the P23 barrel byte-identity). */
	@Test
	void noTintIdentityReturnsTheInputList() {
		List<BakedQuad> tInput = List.of(bodyQuad(), decalQuad());
		assertSame(tInput, GTMachineTintModel.tintQuads(tInput, -1, new ConcurrentHashMap<>()),
				"the -1 no-tint sentinel is the byte-identity passthrough");
	}

	/** The cache: one retinted copy per (tint, source quad), reused across calls. */
	@Test
	void cacheReusesTheRetintedCopyPerTint() {
		Map<Integer, Map<BakedQuad, BakedQuad>> tCache = new ConcurrentHashMap<>();
		BakedQuad tBody = bodyQuad();
		List<BakedQuad> tFirst = GTMachineTintModel.tintQuads(List.of(tBody), 0xFF00FF00, tCache);
		List<BakedQuad> tSecond = GTMachineTintModel.tintQuads(List.of(tBody), 0xFF00FF00, tCache);
		assertSame(tFirst.get(0), tSecond.get(0), "the same (tint, source) reuses the retinted copy");
		// and the copy is actually green-tinted (0xFF00FF00: G=255, R/B=0)
		assertEquals(255, (tFirst.get(0).getVertices()[COLOR_SLOT] >> 8) & 255, "the copy is green-tinted");
		assertTrue(tCache.containsKey(0xFF00FF00), "the cache holds the tint table");
	}
}
