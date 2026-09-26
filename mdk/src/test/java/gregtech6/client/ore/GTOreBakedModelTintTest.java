/**
 * The bake-time overlay tint pins (task p38-issue2-ore-baked-tint): the material colour
 * rides {@link GTOreBakedModel.Params#tintARGB()} and is multiplied into the overlay quads'
 * VERTEX data at bake time (the {@code GTMachineTintModel.retintVertices} product) — the
 * p32 machine-domain migration applied to the ore domain, retiring the runtime BlockColor
 * route that rendered achromatic live. The pins: the overlay quad carries the product with
 * {@code tintIndex -1} (no runtime lookup can double-dye), the base stone stays byte-white
 * with its own {@code -1} (upstream: the BlockTextureMulti mTexture half has no colour
 * argument), and the Params tint agrees byte-for-byte with the block colour seam the
 * retired route used ({@code GTOreClientListener.oreTintARGB}).
 * <p>ISSUE #14: the baked COLOR slot int is ABGR ({@code A<<24|B<<16|G<<8|R} — vanilla
 * putBulkData reads bytes 12/13/14 = R/G/B, QuadTransformers.toABGR the ecosystem's
 * converter); the product pins assert the channel layout, not the raw ARGB tint (the old
 * pin was the swapped convention itself).
 * Offline: a {@code UnitTextureAtlasSprite} stub stands in for the atlas (the
 * GTOreBakedModelLazyBakeTest form — real sprites need a live stitch).
 */
package gregtech6.client.ore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

//? if forge {
import net.minecraftforge.client.textures.UnitTextureAtlasSprite;
//?} else {
/*import net.neoforged.neoforge.client.textures.UnitTextureAtlasSprite;*/
//?}
import net.minecraftforge.client.model.data.ModelData;

import gregtech6.block.ore.GTOreBlock;
import gregtech6.registry.GT6OreBlocks;
import gregtech6.registry.GT6OreBlocks.FormKind;
import gregtech6.registry.GT6OreBlocks.OreKey;
import gregtech6.registry.GTMaterialItems;

public class GTOreBakedModelTintTest {

	private static final int COLOR_SLOT = 3;
	private static final int WHITE = 0xFFFFFFFF;
	/** An arbitrary opaque ore colour (the chalcopyrite-style amber); the product math is value-independent. */
	private static final int TINT = 0xFFA07828;
	/** The same tint as the COLOR SLOT stores it: ABGR, the R/B halves of {@link #TINT} swapped. */
	private static final int TINT_ABGR = (TINT & 0xFF00FF00) | ((TINT >> 16) & 0x000000FF) | ((TINT << 16) & 0x00FF0000);

	@BeforeAll
	static void boot() {
		GTMaterialItems.initMaterials(); // MT/OP must exist before any field dereference
		// the GTOfflineRenderTestBase recipe: the version detect must precede bootStrap — a bare-JVM first boot poisons DataFixers for every later suite in this JVM (the run-order lottery)
		net.minecraft.SharedConstants.tryDetectVersion();
		try {
			net.minecraft.server.Bootstrap.bootStrap();
		} catch (Throwable ignored) {
		}
	}

	/** The model over the unit sprite stub: both layers resolve, the quads bake for real. */
	private static GTOreBakedModel tintedModel(GTOreBakedModel.Params aParams) {
		return new GTOreBakedModel(null, aParams, aMaterial -> UnitTextureAtlasSprite.INSTANCE);
	}

	/** The north pair: quads[0] = the base cube, quads[1] = the overlay shell (the bakeQuads order, per-side filter preserves it). */
	private static List<BakedQuad> northPair(GTOreBakedModel aModel) {
		return aModel.getQuads(null, Direction.NORTH, RandomSource.create(), ModelData.EMPTY, null);
	}

	/** The core pin: the overlay vertices carry colour x tint, the base stays byte-white, both drop tintIndex 0. */
	@Test
	public void overlayBakesTheTintAndTheBaseStaysUntinted() {
		GTOreBakedModel tModel = tintedModel(new GTOreBakedModel.Params(
				new ResourceLocation("gt6", "block/stones/granite/stone"),
				new ResourceLocation("gt6", "block/materialicons/chalcopyrite/ore_small"), TINT));
		List<BakedQuad> tPair = northPair(tModel);
		assertEquals(2, tPair.size(), "one base + one overlay quad per face");

		BakedQuad tBase = tPair.get(0);
		assertEquals(-1, tBase.getTintIndex(), "the base layer keeps the no-tint index");
		assertEquals(WHITE, tBase.getVertices()[COLOR_SLOT], "the base stone stays untinted (white vertices)");

		BakedQuad tOverlay = tPair.get(1);
		assertEquals(-1, tOverlay.getTintIndex(), "the overlay drops tintIndex 0 (no runtime double-dye)");
		assertNotEquals(WHITE, tOverlay.getVertices()[COLOR_SLOT], "the overlay vertices left the white identity");
		assertEquals(TINT_ABGR, tOverlay.getVertices()[COLOR_SLOT], "white x tint = the tint, stored ABGR in the slot");
		// the #14 hue pin: a warm ore colour keeps R > B in the slot layout (copper no longer blue)
		int tSlot = tOverlay.getVertices()[COLOR_SLOT];
		assertTrue((tSlot & 255) > ((tSlot >> 16) & 255), "the tinted overlay is warm: slot R (bits 7-0) > slot B (bits 23-16)");
	}

	/** The no-tint identity (the -1 sentinel) bakes the raw quad, no retint pass. */
	@Test
	public void minusOneTintBakesTheIdentity() {
		GTOreBakedModel tModel = tintedModel(new GTOreBakedModel.Params(
				new ResourceLocation("gt6", "block/stones/granite/stone"),
				new ResourceLocation("gt6", "block/materialicons/chalcopyrite/ore_small"), -1));
		List<BakedQuad> tPair = northPair(tModel);
		assertEquals(WHITE, tPair.get(1).getVertices()[COLOR_SLOT], "the -1 sentinel bakes the white identity");
	}

	/**
	 * The issue #2 RenderType partition: solid = exactly the base set (white vertices),
	 * cutout = exactly the overlay set (tinted vertices), null = both, any other chunk
	 * layer = nothing. Before the fix both chunk passes received all 12 quads and the
	 * overlay's transparent-to-white PNG painted an opaque ore plate on the alpha-less
	 * solid shader (RenderType.java:26-38 vs the :52-64 cutout discard), dyeing the whole
	 * stone surface.
	 */
	@Test
	public void getQuadsPartitionsByRenderType() {
		GTOreBakedModel tModel = tintedModel(new GTOreBakedModel.Params(
				new ResourceLocation("gt6", "block/stones/granite/stone"),
				new ResourceLocation("gt6", "block/materialicons/chalcopyrite/ore_small"), TINT));
		RandomSource tRand = RandomSource.create();

		List<BakedQuad> tSolid = tModel.getQuads(null, null, tRand, ModelData.EMPTY, RenderType.solid());
		assertEquals(6, tSolid.size(), "solid = exactly the 6 base-cube quads");
		for (BakedQuad tQuad : tSolid) {
			assertEquals(WHITE, tQuad.getVertices()[COLOR_SLOT], "every solid quad is an untinted base quad");
		}

		List<BakedQuad> tCutout = tModel.getQuads(null, null, tRand, ModelData.EMPTY, RenderType.cutout());
		assertEquals(6, tCutout.size(), "cutout = exactly the 6 overlay-shell quads");
		for (BakedQuad tQuad : tCutout) {
			assertEquals(TINT_ABGR, tQuad.getVertices()[COLOR_SLOT], "every cutout quad is the tinted overlay");
			assertEquals(-1, tQuad.getTintIndex(), "the overlay keeps the no-runtime-lookup index");
		}

		List<BakedQuad> tNull = tModel.getQuads(null, null, tRand, ModelData.EMPTY, null);
		assertEquals(12, tNull.size(), "the null pass (item render, breaking overlays) = all quads");
		assertEquals(6, countWhite(tNull), "the null pass carries the 6 white base quads");
		assertEquals(6, countTinted(tNull), "the null pass carries the 6 tinted overlay quads");

		assertEquals(List.of(), tModel.getQuads(null, null, tRand, ModelData.EMPTY, RenderType.translucent()),
				"no other chunk layer receives anything");

		// per-side filtering rides the partitioned sets too
		assertEquals(1, tModel.getQuads(null, Direction.NORTH, tRand, ModelData.EMPTY, RenderType.solid()).size(),
				"solid north = the one base quad");
		assertEquals(1, tModel.getQuads(null, Direction.NORTH, tRand, ModelData.EMPTY, RenderType.cutout()).size(),
				"cutout north = the one overlay quad");
	}

	private static int countWhite(List<BakedQuad> aQuads) {
		int rCount = 0;
		for (BakedQuad tQuad : aQuads) if (tQuad.getVertices()[COLOR_SLOT] == WHITE) rCount++;
		return rCount;
	}

	private static int countTinted(List<BakedQuad> aQuads) {
		int rCount = 0;
		for (BakedQuad tQuad : aQuads) if (tQuad.getVertices()[COLOR_SLOT] == TINT_ABGR) rCount++;
		return rCount;
	}

	/** The Params tint is the block colour seam's own value (no drift between the bake and the retired route's encoding). */
	@Test
	public void paramsTintAgreesWithTheBlockColourSeam() {
		// the small form pins the OP.oreSmall prefix leg (fRGBa[oreSmall.mState]); any axis material agrees
		OreKey tKey = new OreKey(GT6OreBlocks.TAB_FAMILY, FormKind.SMALL, GT6OreBlocks.materialAxis().get(0));
		// Block.<init> creates its intrusive holder past the bootstrap freeze — carry our own
		// write window (the GT6CFoamFamilyTest.buildFamilyFixtures shape, no re-freeze): the
		// class previously rode an earlier-alphabetical class's open window, which fork
		// partitioning (maxParallelForks, task maint-ci-perf) can't guarantee.
		try {
			java.lang.reflect.Method tUnfreeze = net.minecraft.core.registries.BuiltInRegistries.BLOCK
					.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(net.minecraft.core.registries.BuiltInRegistries.BLOCK);
		} catch (Exception aE) {
			throw new IllegalStateException("could not unfreeze the offline block registry", aE);
		}
		GTOreBakedModel.Params tParams = GTOreBakedModel.paramsOf(tKey);
		GTOreBlock tBlock = new GTOreBlock(tKey.family(), tKey.kind(), tKey.family().form(tKey.kind()),
				tKey.family().prefix(tKey.kind()), tKey.material());
		assertEquals(GTOreClientListener.oreTintARGB(tBlock.defaultBlockState(), 0), tParams.tintARGB(),
				"the bake tint = fRGBa[oreSmall.mState], the same value the BlockColor route resolved");
		assertEquals(0xFF, tParams.tintARGB() >>> 24, "opaque ARGB");
	}
}
