/**
 * The kitchen NEI corner glyph bake pins (task kitchen-nei-corner-jump, the
 * GTOvenOverlayModelTest shape): the patch exists only when a viewer is present (the
 * bake-time GTViewerJump.preferredViewer gate), picks ITS tile from the same predicate
 * (EMI first — what you see is what you jump to), rides the cutout layer (the transparent
 * glyph margins must discard, the r11-oven-solid-layer-fix lesson), carries the upstream
 * pass-6/7 rim-corner geometry derived from the family collision-pool shape, and never
 * perturbs the body quads. The LIVE wrap (listener + atlas stitching + JEI/EMI in a real
 * client) is the field_test.
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

	/** A named atlas stub — the name is the pin's membership key (the GTOvenOverlayModelTest form). */
	private static final class NamedSprite extends TextureAtlasSprite {
		private NamedSprite(ResourceLocation aName) {
			super(aName,
					new net.minecraft.client.renderer.texture.SpriteContents(aName,
							new FrameSize(1, 1),
							new com.mojang.blaze3d.platform.NativeImage(1, 1, false),
							//? if forge {
							net.minecraft.client.resources.metadata.animation.AnimationMetadataSection.EMPTY),
							//?} else {
							/*net.minecraft.server.packs.resources.ResourceMetadata.EMPTY),*/
							//?}
					1, 1, 0, 0);
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

	/** The tile follows the SAME predicate the jump route uses — EMI first (what you see is what you jump to). */
	@Test
	void theTileFollowsTheViewerPredicateEmiFirst() {
		GT6KitchenNeiModel tJei = model(sTub, "jei");
		assertEquals("gt6:block/tools/kitchen_nei_jei", spriteId(quads(tJei, RenderType.cutout()).get(1)).toString(),
				"JEI-only → the JEI tile");
		GT6KitchenNeiModel tEmi = model(sTub, "emi");
		assertEquals("gt6:block/tools/kitchen_nei_emi", spriteId(quads(tEmi, RenderType.cutout()).get(1)).toString(),
				"EMI present → the EMI tile (the dual-install ruling)");
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
