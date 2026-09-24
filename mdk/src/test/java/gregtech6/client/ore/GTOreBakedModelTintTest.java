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
 * Offline: a {@code UnitTextureAtlasSprite} stub stands in for the atlas (the
 * GTOreBakedModelLazyBakeTest form — real sprites need a live stitch).
 */
package gregtech6.client.ore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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

	@BeforeAll
	static void boot() {
		GTMaterialItems.initMaterials(); // MT/OP must exist before any field dereference
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
		assertEquals(TINT, tOverlay.getVertices()[COLOR_SLOT], "white x tint = the tint itself");
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

	/** The Params tint is the block colour seam's own value (no drift between the bake and the retired route's encoding). */
	@Test
	public void paramsTintAgreesWithTheBlockColourSeam() {
		// the small form pins the OP.oreSmall prefix leg (fRGBa[oreSmall.mState]); any axis material agrees
		OreKey tKey = new OreKey(GT6OreBlocks.TAB_FAMILY, FormKind.SMALL, GT6OreBlocks.materialAxis().get(0));
		GTOreBakedModel.Params tParams = GTOreBakedModel.paramsOf(tKey);
		GTOreBlock tBlock = new GTOreBlock(tKey.family(), tKey.kind(), tKey.family().form(tKey.kind()),
				tKey.family().prefix(tKey.kind()), tKey.material());
		assertEquals(GTOreClientListener.oreTintARGB(tBlock.defaultBlockState(), 0), tParams.tintARGB(),
				"the bake tint = fRGBa[oreSmall.mState], the same value the BlockColor route resolved");
		assertEquals(0xFF, tParams.tintARGB() >>> 24, "opaque ARGB");
	}
}
