/**
 * The mortar NEI rim-corner glyph bake pins (task mortar-family, the GT6AnvilNeiModelTest
 * shape): the patch exists only when a viewer is present (the bake-time
 * GTViewerJump.preferredViewer gate), picks ITS tile from the same predicate (EMI first —
 * what you see is what you jump to), carries the upstream corner geometry (the (2..4)x
 * (2..4)px rim-corner patch at PX_N[10]+0.001 — the corner-click region, :81/:99/:122),
 * rides the untinted decal contract, and passes the body quads through VERBATIM (no baked
 * tint interplay — the mortar tint is the runtime GT6MortarTint BlockColor). The LIVE wrap
 * (listener + atlas stitching + JEI/EMI in a real client) is the field_test.
 */
package gregtech6.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.block.tools.GT6MortarBlock;

public class GT6MortarNeiModelTest extends GTOfflineRenderTestBase {

	private static final int STRIDE = 8;

	private static GT6MortarBlock sMortar;

	@BeforeAll
	static void buildOfflineFixtures() {
		gregtech6.registry.GTMaterialItems.initMaterials();
		// the BLOCK registry write window (the GT6KitchenNeiModelTest recipe) — the Block
		// ctor registers its intrusive holder
		try {
			java.lang.reflect.Method tUnfreeze = net.minecraft.core.registries.BuiltInRegistries.BLOCK
					.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(net.minecraft.core.registries.BuiltInRegistries.BLOCK);
		} catch (Exception ignored) {
			// already unfrozen by a sibling fixture
		}
		// the BET supplier stays null — the model never resolves it
		sMortar = new GT6MortarBlock(() -> gregapi.data.MT.Steel, () -> null,
				net.minecraft.world.level.block.state.BlockBehaviour.Properties.of());
	}

	/**
	 * A named atlas stub — the name is the pin's membership key (the GTOvenOverlayModelTest
	 * form). The geometry mirrors the REAL block atlas seat (a 128px sheet in a 1024 atlas,
	 * the GT6KitchenNeiModelTest recipe) because FaceBakery shrinks face UVs toward their
	 * centre by {@code uvShrinkRatio() = 4/atlasSize} (FaceBakery.java:45-51) — a 1x1 stub
	 * would ratio-4-mangle the glyph UV pins; at 128/1024 the shrink is the real ~0.4%.
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
			@Override public List<BakedQuad> getQuads(@org.jetbrains.annotations.Nullable BlockState aState,
					@org.jetbrains.annotations.Nullable Direction aSide, RandomSource aRand) { return aQuads; }
			@Override public boolean useAmbientOcclusion() { return false; }
			@Override public boolean isGui3d() { return false; }
			@Override public boolean usesBlockLight() { return false; }
			@Override public boolean isCustomRenderer() { return false; }
			@Override public TextureAtlasSprite getParticleIcon() { return null; }
			@Override public net.minecraft.client.renderer.block.model.ItemTransforms getTransforms() { return net.minecraft.client.renderer.block.model.ItemTransforms.NO_TRANSFORMS; }
			@Override public net.minecraft.client.renderer.block.model.ItemOverrides getOverrides() { return net.minecraft.client.renderer.block.model.ItemOverrides.EMPTY; }
			@Override public ChunkRenderTypeSet getRenderTypes(BlockState aState, RandomSource aRand, ModelData aData) {
				return ChunkRenderTypeSet.of(RenderType.solid());
			}
		};
	}

	/** A tintindex-0 body quad (32 ints, white) — what the mortar models bake on the body faces. */
	private static BakedQuad bodyQuad() {
		int[] tVertices = new int[4 * STRIDE];
		java.util.Arrays.fill(tVertices, 0xFFFFFFFF);
		return new BakedQuad(tVertices, 0, Direction.UP, new NamedSprite(
				ResourceLocation.fromNamespaceAndPath("gt6", "block/tools/mortar/sides")), true);
	}

	private static GT6MortarNeiModel model(String aViewer) {
		return new GT6MortarNeiModel(fallback(List.of(bodyQuad())), stubLookup(), () -> aViewer);
	}

	private static List<BakedQuad> quads(GT6MortarNeiModel aModel, RenderType aLayer) {
		return aModel.getQuads(sMortar.defaultBlockState(), Direction.UP, RandomSource.create(), ModelData.EMPTY, aLayer);
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

	/** The glyph exists ONLY with a viewer, on the cutout layer alone. */
	@Test
	void theGlyphRidesCutoutOnlyWhenAViewerIsPresent() {
		GT6MortarNeiModel tJei = model("jei");
		assertEquals(2, quads(tJei, RenderType.cutout()).size(), "the body + the glyph");
		assertEquals(1, quads(tJei, RenderType.solid()).size(), "the solid pass carries the body only");
		assertEquals(1, quads(tJei, null).size(), "the unculled pass sees no glyph (the oven convention)");

		GT6MortarNeiModel tNone = model(null);
		assertEquals(1, quads(tNone, RenderType.cutout()).size(), "no viewer -> no patch at all (the nei ruling)");
		assertEquals(1, quads(tNone, RenderType.solid()).size(), "no viewer -> the plain body everywhere");
	}

	/** The tile follows the SAME predicate the jump route uses — EMI first (what you see is what you jump to). */
	@Test
	void theTileFollowsTheViewerPredicateEmiFirst() {
		GT6MortarNeiModel tJei = model("jei");
		assertEquals("gt6:block/tools/kitchen_nei_jei", spriteId(quads(tJei, RenderType.cutout()).get(1)).toString(),
				"JEI-only -> the JEI tile (the family pair, zero new assets)");
		GT6MortarNeiModel tEmi = model("emi");
		assertEquals("gt6:block/tools/kitchen_nei_emi", spriteId(quads(tEmi, RenderType.cutout()).get(1)).toString(),
				"EMI present -> the EMI tile (the dual-install ruling)");
	}

	/** The patch is the (2..4)x(2..4)px rim-corner box at PX_N[10]+0.001 (the corner-click region, :81/:99). */
	@Test
	void thePatchIsTheRimCornerBox() {
		// baked quad vertices live in the 0..1 BLOCK space (the r3-stick-shape lesson —
		// pin against the px values divided by 16)
		BakedQuad tGlyph = quads(model("jei"), RenderType.cutout()).get(1);
		assertEquals(Direction.UP, tGlyph.getDirection(), "the glyph is an UP quad (the pass-6 SIDE_TOP face)");
		assertEquals(-1, tGlyph.getTintIndex(), "the decal is untinted (the P22 contract)");
		AABB tBox = boxOf(tGlyph);
		assertEquals(2.0 / 16.0, tBox.minX, 0.0001, "the corner rides the envelope's 2px inset (PX_P[2])");
		assertEquals(2.0 / 16.0, tBox.minZ, 0.0001, "the corner rides the envelope's 2px inset (PX_P[2])");
		assertEquals(6.001 / 16.0, tBox.maxY, 0.0001, "the glyph sits PX_N[10]+0.001 on the rim (the :122 plate)");
		assertEquals(4.0 / 16.0, tBox.maxX, 0.0001, "the 2px remainder up to PX_P[4] — the clickable corner region");
		assertEquals(4.0 / 16.0, tBox.maxZ, 0.0001, "the 2px remainder up to PX_P[4]");
	}

	/**
	 * The UV is the quad's own footprint — ONE word cell of the 8x8 sheet (the kitchen
	 * {@code bakeNeiQuad} correction; the upstream render-bounds interpolation,
	 * ITexture.java:295-298). The corner sits at (2,2)px → the SECOND cell, face px 2..4 on
	 * both axes. The stored values carry the vanilla face-UV shrink toward the centre
	 * (uvShrinkRatio = 4/1024 on the 128-in-1024 stub, FaceBakery.java:45-51) — ~0.4%,
	 * inside the pin tolerance; the OLD full-sheet {@code [0,0,16,16]} UV lands at
	 * u ≈ 0.125 (the whole 128px sheet squeezed onto the corner — the 「整张字模表」 bug)
	 * and fails this pin outright.
	 */
	@Test
	void theGlyphUvIsTheQuadFootprintOneSheetCell() {
		double tEps = 0.0005;
		double tSheet = 128.0; // the stub sprite's native px — normalized u = sheet px / 128
		// the corner (2..4)x(2..4)px → the cell one in from the sheet's top-left edge
		double[][] tUv = uvOf(quads(model("jei"), RenderType.cutout()).get(1));
		assertEquals(2.0 / tSheet, tUv[0][0], tEps, "the glyph u rides the 2px envelope inset");
		assertEquals(2.0 / tSheet, tUv[1][0], tEps, "the glyph v rides the 2px envelope inset");
		assertEquals(4.0 / tSheet, tUv[0][1], tEps, "the glyph u ends at the 4px bound — one cell");
		assertEquals(4.0 / tSheet, tUv[1][1], tEps, "the glyph v ends at the 4px bound — one cell");
	}

	/** The seat: cutout joins the fallback's layers exactly when the glyph exists. */
	@Test
	void theRenderSeatJoinsCutoutOnlyWithAViewer() {
		RandomSource tRand = RandomSource.create();
		ChunkRenderTypeSet tWith = model("jei").getRenderTypes(sMortar.defaultBlockState(), tRand, ModelData.EMPTY);
		assertTrue(tWith.contains(RenderType.solid()), "the body keeps the fallback's solid seat");
		assertTrue(tWith.contains(RenderType.cutout()), "the glyph adds the cutout seat");

		ChunkRenderTypeSet tWithout = model(null).getRenderTypes(sMortar.defaultBlockState(), tRand, ModelData.EMPTY);
		assertTrue(tWithout.contains(RenderType.solid()), "no viewer -> the fallback seat unchanged");
		assertFalse(tWithout.contains(RenderType.cutout()), "no viewer -> no cutout seat added");
	}

	/** The body pass is the fallback's list VERBATIM — the tint rides the runtime BlockColor, not a bake wrap. */
	@Test
	void theBodyPassIsUntouchedVerbatim() {
		List<BakedQuad> tBody = List.of(bodyQuad());
		GT6MortarNeiModel tModel = new GT6MortarNeiModel(fallback(tBody), stubLookup(), () -> "jei");
		assertSame(tBody, tModel.getQuads(sMortar.defaultBlockState(), null, RandomSource.create(), ModelData.EMPTY, RenderType.solid()),
				"the solid pass IS the fallback's list — no tint rewrap (the GT6MortarTint dispatch is runtime)");
	}
}
