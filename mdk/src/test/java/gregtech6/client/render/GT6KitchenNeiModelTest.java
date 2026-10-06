/**
 * The kitchen NEI corner glyph bake pins (task kitchen-nei-corner-jump, corrected by task
 * mixingbowl-bathingpot-fidelity, the GTOvenOverlayModelTest shape): the patch exists only
 * when a viewer is present (the bake-time GTViewerJump.preferredViewer gate), rides the
 * BYTE-IDENTICAL upstream nei.png sheet for ANY viewer (the fidelity correction — the
 * glyph is upstream's NEI mark, not a viewer logo), samples exactly ONE word cell of the
 * 8x8 sheet (the UV = the quad footprint — the upstream render-bounds interpolation,
 * ITexture.java:295-298; the full-sheet UV was the small-letter-pile bug), carries the
 * upstream CA_YELLOW_255 render tint in the vertex colours (the shipped PNG stays
 * untouched), rides the cutout layer (the transparent glyph margins must discard, the
 * r11-oven-solid-layer-fix lesson), carries the upstream pass-6/7 rim-corner geometry
 * derived from the family collision-pool shape, and never perturbs the body quads. The
 * LIVE wrap (listener + atlas stitching + JEI/EMI in a real client) is the field_test.
 */
package gregtech6.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.Function;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;

import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.block.tools.GTKitchenBlock;

public class GT6KitchenNeiModelTest extends GTOfflineRenderTestBase {

	private static final int STRIDE = 8;

	private static GTKitchenBlock sTub;
	private static GTKitchenBlock sJuicer;

	@BeforeAll
	static void buildOfflineFixtures() {
		gregtech6.registry.GTMaterialItems.initMaterials();
		// the BLOCK registry write window (the GTOvenOverlayModelTest recipe) — the Block
		// ctor registers its intrusive holder
		try {
			java.lang.reflect.Method tUnfreeze = net.minecraft.core.registries.BuiltInRegistries.BLOCK
					.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(net.minecraft.core.registries.BuiltInRegistries.BLOCK);
		} catch (Exception ignored) {
			// already unfrozen by a sibling fixture
		}
		net.minecraft.world.level.block.state.BlockBehaviour.Properties tProps =
				net.minecraft.world.level.block.state.BlockBehaviour.Properties.of();
		// a REAL material — the null-material fixture would ride the P23 -1 tint identity
		// and silently skip the retint arm this pin drives
		sTub = new GTKitchenBlock(8000, () -> gregapi.data.MT.Ceramic, GTKitchenBlock.SHAPE_TUB, () -> null, tProps);
		sJuicer = new GTKitchenBlock(1000000, () -> gregapi.data.MT.Ceramic, GTKitchenBlock.SHAPE_JUICER, () -> null, tProps);
	}

	/**
	 * A named atlas stub — the name is the pin's membership key (the GTOvenOverlayModelTest
	 * form). The geometry mirrors the REAL block atlas seat (a 128px sheet in a 1024 atlas)
	 * because FaceBakery shrinks face UVs toward their centre by
	 * {@code uvShrinkRatio() = 4/atlasSize} (FaceBakery.java:47-53) — a 1x1 stub would
	 * ratio-4-mangle the glyph UV pins; at 128/1024 the shrink is the real ~0.4%.
	 */
	private static final class NamedSprite extends TextureAtlasSprite {
		private static final int SHEET_PX = 128;
		private static final int ATLAS_PX = 1024;

		private NamedSprite(ResourceLocation aName) {
			super(aName,
					new net.minecraft.client.renderer.texture.SpriteContents(aName,
							new FrameSize(SHEET_PX, SHEET_PX),
							new com.mojang.blaze3d.platform.NativeImage(SHEET_PX, SHEET_PX, false),
							//? if forge {
							net.minecraft.client.resources.metadata.animation.AnimationMetadataSection.EMPTY),
							//?} else {
							/*net.minecraft.server.packs.resources.ResourceMetadata.EMPTY),*/
							//?}
					ATLAS_PX, ATLAS_PX, 0, 0);
		}
	}

	private static Function<ResourceLocation, TextureAtlasSprite> stubLookup() {
		return aId -> new NamedSprite(aId);
	}

	/** The barest fallback: the fixed quad list + the solid-only layer seat. */
	private static BakedModel fallback(List<BakedQuad> aQuads) {
		return new BakedModel() {
			@Override public List<BakedQuad> getQuads(@org.jetbrains.annotations.Nullable net.minecraft.world.level.block.state.BlockState aState,
					@org.jetbrains.annotations.Nullable Direction aSide, RandomSource aRand) { return aQuads; }
			@Override public boolean useAmbientOcclusion() { return false; }
			@Override public boolean isGui3d() { return false; }
			@Override public boolean usesBlockLight() { return false; }
			@Override public boolean isCustomRenderer() { return false; }
			@Override public TextureAtlasSprite getParticleIcon() { return null; }
			@Override public net.minecraft.client.renderer.block.model.ItemTransforms getTransforms() { return net.minecraft.client.renderer.block.model.ItemTransforms.NO_TRANSFORMS; }
			@Override public net.minecraft.client.renderer.block.model.ItemOverrides getOverrides() { return net.minecraft.client.renderer.block.model.ItemOverrides.EMPTY; }
			@Override public ChunkRenderTypeSet getRenderTypes(net.minecraft.world.level.block.state.BlockState aState, RandomSource aRand, ModelData aData) {
				return ChunkRenderTypeSet.of(RenderType.solid());
			}
		};
	}

	/** A tintindex-0 body quad (32 ints, white) — what the family models bake on every face. */
	private static BakedQuad bodyQuad() {
		int[] tVertices = new int[4 * STRIDE];
		java.util.Arrays.fill(tVertices, 0xFFFFFFFF);
		return new BakedQuad(tVertices, 0, Direction.NORTH, new NamedSprite(
				ResourceLocation.fromNamespaceAndPath("gt6", "block/tools/mixing_bowl/sides")), true);
	}

	private static GT6KitchenNeiModel model(Block aBlock, String aViewer) {
		// the same shape derivation the production ctor drives (the GTKitchenBlock carrier
		// answers state-free)
		net.minecraft.world.phys.shapes.VoxelShape tShape = ((GTKitchenBlock) aBlock).getShape(null, null, null, null);
		return new GT6KitchenNeiModel(fallback(List.of(bodyQuad())), aBlock, tShape, stubLookup(), () -> aViewer);
	}

	private static List<BakedQuad> quads(GT6KitchenNeiModel aModel, RenderType aLayer) {
		return aModel.getQuads(null, Direction.UP, RandomSource.create(), ModelData.EMPTY, aLayer);
	}

	/** The quad's sprite id (the offline membership probe). */
	private static ResourceLocation spriteId(BakedQuad aQuad) {
		return aQuad.getSprite().contents().name();
	}

	/** The quad's corner positions in model space (px) — min/max over the 4 vertices. */
	private static AABB boxOf(BakedQuad aQuad) {
		int[] tV = aQuad.getVertices();
		double tMinX = 99, tMinY = 99, tMinZ = 99, tMaxX = -99, tMaxY = -99, tMaxZ = -99;
		for (int v = 0; v * STRIDE < tV.length; v++) {
			double tX = Float.intBitsToFloat(tV[v * STRIDE]);
			double tY = Float.intBitsToFloat(tV[v * STRIDE + 1]);
			double tZ = Float.intBitsToFloat(tV[v * STRIDE + 2]);
			tMinX = Math.min(tMinX, tX); tMaxX = Math.max(tMaxX, tX);
			tMinY = Math.min(tMinY, tY); tMaxY = Math.max(tMaxY, tY);
			tMinZ = Math.min(tMinZ, tZ); tMaxZ = Math.max(tMaxZ, tZ);
		}
		return new AABB(tMinX, tMinY, tMinZ, tMaxX, tMaxY, tMaxZ);
	}

	/** The quad's {u, v} extents in normalized sprite space (min/max over the 4 vertices) — {umin, umax}, {vmin, vmax}. */
	private static double[][] uvOf(BakedQuad aQuad) {
		int[] tV = aQuad.getVertices();
		double tMinU = 99, tMinV = 99, tMaxU = -99, tMaxV = -99;
		for (int v = 0; v * STRIDE < tV.length; v++) {
			double tU = Float.intBitsToFloat(tV[v * STRIDE + 4]);
			double tVv = Float.intBitsToFloat(tV[v * STRIDE + 5]);
			tMinU = Math.min(tMinU, tU); tMaxU = Math.max(tMaxU, tU);
			tMinV = Math.min(tMinV, tVv); tMaxV = Math.max(tMaxV, tVv);
		}
		return new double[][] {{tMinU, tMaxU}, {tMinV, tMaxV}};
	}

	// ---------------------------------------------------------------- the pins

	/** The glyph exists ONLY with a viewer, on the cutout layer alone, and never disturbs the body pass. */
	@Test
	void theGlyphRidesCutoutOnlyWhenAViewerIsPresent() {
		GT6KitchenNeiModel tJei = model(sTub, "jei");
		List<BakedQuad> tCutout = quads(tJei, RenderType.cutout());
		assertEquals(2, tCutout.size(), "the tinted body + the glyph");
		List<BakedQuad> tSolid = quads(tJei, RenderType.solid());
		assertEquals(1, tSolid.size(), "the solid pass carries the body only");
		assertEquals(1, quads(tJei, null).size(), "the unculled pass sees no glyph (the oven convention)");

		GT6KitchenNeiModel tNone = model(sTub, null);
		assertEquals(1, quads(tNone, RenderType.cutout()).size(), "no viewer → no patch at all (the nei ruling)");
		assertEquals(1, quads(tNone, RenderType.solid()).size(), "no viewer → the plain body everywhere");
	}

	/** The glyph is the byte-identical upstream NEI sheet for ANY viewer (the fidelity correction — no viewer lettering). */
	@Test
	void theGlyphIsTheUpstreamNeiSheetForAnyViewer() {
		assertEquals("gt6:" + GT6KitchenNeiModel.SPRITE_NEI,
				spriteId(quads(model(sTub, "jei"), RenderType.cutout()).get(1)).toString(),
				"JEI-only → the upstream nei.png sheet");
		assertEquals("gt6:" + GT6KitchenNeiModel.SPRITE_NEI,
				spriteId(quads(model(sTub, "emi"), RenderType.cutout()).get(1)).toString(),
				"EMI present → the SAME upstream sheet (the jump target is EMI, the mark is upstream's)");
	}

	/**
	 * The UV is the quad's own footprint — ONE word cell of the 8x8 sheet (the upstream
	 * render-bounds interpolation, ITexture.java:295-298); the full-sheet UV squeezed all
	 * 64 words onto the corner (the small-letter-pile bug). The stored values carry the
	 * vanilla face-UV shrink toward the centre (uvShrinkRatio = 4/1024 on the 128-in-1024
	 * stub, FaceBakery.java:47-53) — ~0.4%, inside the pin tolerance; the OLD full-sheet
	 * UV lands at u ≈ 0.125 (the whole 128px sheet) and fails this pin outright.
	 */
	@Test
	void theGlyphUvIsTheQuadFootprintOneSheetCell() {
		double tEps = 0.0005;
		double tSheet = 128.0; // the stub sprite's native px — normalized u = sheet px / 128
		// the tub pair: corner (0,0) 2x2px → the sheet's top-left cell
		double[][] tTubUv = uvOf(quads(model(sTub, "jei"), RenderType.cutout()).get(1));
		assertEquals(0.0, tTubUv[0][0], tEps, "the tub glyph u starts at the sheet edge");
		assertEquals(0.0, tTubUv[1][0], tEps, "the tub glyph v starts at the sheet edge");
		assertEquals(2.0 / tSheet, tTubUv[0][1], tEps, "the tub glyph u spans the 2px crop — one cell");
		assertEquals(2.0 / tSheet, tTubUv[1][1], tEps, "the tub glyph v spans the 2px crop — one cell");
		// the Juicer: corner (2,2) 2x2px → the SECOND cell (the same word, upstream's own crop)
		double[][] tJuicerUv = uvOf(quads(model(sJuicer, "jei"), RenderType.cutout()).get(1));
		assertEquals(2.0 / tSheet, tJuicerUv[0][0], tEps, "the Juicer glyph u rides the 2px inset");
		assertEquals(2.0 / tSheet, tJuicerUv[1][0], tEps, "the Juicer glyph v rides the 2px inset");
		assertEquals(4.0 / tSheet, tJuicerUv[0][1], tEps, "the Juicer glyph u ends at the 4px bound");
		assertEquals(4.0 / tSheet, tJuicerUv[1][1], tEps, "the Juicer glyph v ends at the 4px bound");
	}

	/**
	 * The upstream CA_YELLOW_255 render tint (CS.java:389, {255,255,0,255}) rides the
	 * vertex colours: white sheet x yellow = ABGR {@code 0xFF00FFFF} on every vertex —
	 * while the tint index stays -1 (the P22 decal contract, never re-tinted at runtime).
	 */
	@Test
	void theGlyphCarriesTheUpstreamYellowInVertexColours() {
		BakedQuad tGlyph = quads(model(sTub, "jei"), RenderType.cutout()).get(1);
		assertEquals(-1, tGlyph.getTintIndex(), "the decal is untinted at runtime (the P22 contract)");
		int[] tV = tGlyph.getVertices();
		for (int v = 0; v * STRIDE < tV.length; v++) {
			assertEquals(0xFF00FFFF, tV[v * STRIDE + 3],
					"vertex " + v + " carries white-sheet-x-CA_YELLOW_255 (ABGR) in its colour slot");
		}
	}

	/** The patch is the upstream pass-6/7 rim-corner box, derived from the collision-pool shape. */
	@Test
	void thePatchIsTheRimCornerBoxFromTheFamilyShape() {
		// baked quad vertices live in the 0..1 BLOCK space (the r3-stick-shape lesson —
		// pin against the px values divided by 16)
		// the tub pair: walls y 2..8 at the full footprint → the (0,8,0)-(2,8.001,2) corner
		BakedQuad tTub = quads(model(sTub, "jei"), RenderType.cutout()).get(1);
		assertEquals(Direction.UP, tTub.getDirection(), "the glyph is an UP quad (the pass-6 SIDE_TOP face)");
		assertEquals(-1, tTub.getTintIndex(), "the decal is untinted (the yellow is baked in, the P22 contract)");
		AABB tBox = boxOf(tTub);
		assertEquals(0.0, tBox.minX, 0.0001, "the tub corner rides the footprint's min x");
		assertEquals(0.0, tBox.minZ, 0.0001, "the tub corner rides the footprint's min z");
		assertEquals(8.001 / 16.0, tBox.maxY, 0.0001, "the glyph sits PX_N[8]+0.001 on the rim");
		assertEquals(2.0 / 16.0, tBox.maxX, 0.0001, "2x2px — the upstream glyph box width");
		assertEquals(2.0 / 16.0, tBox.maxZ, 0.0001, "2x2px — the upstream glyph box depth");

		// the Juicer (the 4px variant family): walls y 0..4 at the 2px inset → (2,4,2)-(4,4.001,4)
		BakedQuad tJuicer = quads(model(sJuicer, "emi"), RenderType.cutout()).get(1);
		AABB tJBox = boxOf(tJuicer);
		assertEquals(2.0 / 16.0, tJBox.minX, 0.0001, "the Juicer corner rides the 2px inset (PX_P[2])");
		assertEquals(2.0 / 16.0, tJBox.minZ, 0.0001, "the Juicer corner rides the 2px inset (PX_P[2])");
		assertEquals(4.001 / 16.0, tJBox.maxY, 0.0001, "the Juicer glyph sits PX_N[12]+0.001 on its rim");
	}

	/** The seat: cutout joins the fallback's layers exactly when the glyph exists. */
	@Test
	void theRenderSeatJoinsCutoutOnlyWithAViewer() {
		RandomSource tRand = RandomSource.create();
		ChunkRenderTypeSet tWith = model(sTub, "jei").getRenderTypes(null, tRand, ModelData.EMPTY);
		assertTrue(tWith.contains(RenderType.solid()), "the body keeps the fallback's solid seat");
		assertTrue(tWith.contains(RenderType.cutout()), "the glyph adds the cutout seat");

		ChunkRenderTypeSet tWithout = model(sTub, null).getRenderTypes(null, tRand, ModelData.EMPTY);
		assertTrue(tWithout.contains(RenderType.solid()), "no viewer → the fallback seat unchanged");
		assertFalse(tWithout.contains(RenderType.cutout()), "no viewer → no cutout seat added");
	}

	/** The body pass off a RAW fallback is the family tint (the order-first wrap case) — retinted, not dropped. */
	@Test
	void theBodyOffARawFallbackCarriesTheFamilyTint() {
		GT6KitchenNeiModel tModel = model(sTub, "jei");
		List<BakedQuad> tSolid = quads(tModel, RenderType.solid());
		assertNotNull(tSolid);
		assertEquals(1, tSolid.size());
		assertEquals(-1, tSolid.get(0).getTintIndex(),
				"the raw fallback's tintindex-0 body came out retinted (the tintQuads product) — the order-first case tints exactly once");
	}
}
