/**
 * The grindstone NEI corner glyph bake pins (task grindstone-family, the
 * GT6AnvilNeiModelTest shape): the patch exists only when a viewer is present (the
 * bake-time GTViewerJump.preferredViewer gate) AND the state's STONE is in (the upstream
 * {@code mStone!=0 &&} conjunction, MultiTileEntityGrindStone.java:244 — the empty stone
 * shows no glyph), picks ITS tile from the same predicate (EMI first), rides the cutout
 * layer, carries the upstream pass-0 post-top geometry per FACING AXIS (:211 — the 2x2px
 * corner at (6..8, 2..4) Z-form / (2..4, 6..8) X-form, on the 15px plate shoulder), and
 * never perturbs the body quads (the dispatch-less grindstone resolves the -1 no-tint
 * sentinel — the body list passes through verbatim). The LIVE wrap (listener + atlas
 * stitching + JEI/EMI in a real client) is the field_test.
 */
package gregtech6.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
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

import gregtech6.block.tools.GT6GrindstoneBlock;

public class GT6GrindstoneNeiModelTest extends GTOfflineRenderTestBase {

	private static final int STRIDE = 8;

	private static GT6GrindstoneBlock sGrindstone;

	@BeforeAll
	static void buildOfflineFixtures() {
		// the BLOCK registry write window (the GT6AnvilNeiModelTest recipe) — the Block
		// ctor registers its intrusive holder
		try {
			java.lang.reflect.Method tUnfreeze = net.minecraft.core.registries.BuiltInRegistries.BLOCK
					.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(net.minecraft.core.registries.BuiltInRegistries.BLOCK);
		} catch (Exception ignored) {
			// already unfrozen by a sibling fixture
		}
		// the BET supplier stays null — the model never resolves it (only FACING/STONE are read)
		sGrindstone = new GT6GrindstoneBlock(() -> null,
				net.minecraft.world.level.block.state.BlockBehaviour.Properties.of());
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

	/** A plain body quad (32 ints, white, tintIndex -1) — the decal-free body face. */
	private static BakedQuad bodyQuad() {
		int[] tVertices = new int[4 * STRIDE];
		java.util.Arrays.fill(tVertices, 0xFFFFFFFF);
		return new BakedQuad(tVertices, -1, Direction.UP, new NamedSprite(
				ResourceLocation.fromNamespaceAndPath("minecraft", "block/smooth_stone")), true);
	}

	private static GT6GrindstoneNeiModel model(String aViewer) {
		return new GT6GrindstoneNeiModel(fallback(List.of(bodyQuad())), sGrindstone, stubLookup(), () -> aViewer);
	}

	private static BlockState state(Direction aFacing, int aStone) {
		return sGrindstone.defaultBlockState().setValue(GT6GrindstoneBlock.FACING, aFacing)
				.setValue(GT6GrindstoneBlock.STONE, aStone);
	}

	private static List<BakedQuad> quads(GT6GrindstoneNeiModel aModel, BlockState aState, RenderType aLayer) {
		return aModel.getQuads(aState, null, RandomSource.create(), ModelData.EMPTY, aLayer);
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

	/** The glyph exists ONLY with a viewer, on cutout, AND only on a loaded state (the mStone gate). */
	@Test
	void theGlyphsRideCutoutOnlyWhenAViewerAndTheStoneArePresent() {
		GT6GrindstoneNeiModel tJei = model("jei");
		List<BakedQuad> tCutout = quads(tJei, state(Direction.NORTH, 16), RenderType.cutout());
		assertEquals(2, tCutout.size(), "the body + the one top-corner glyph");
		List<BakedQuad> tSolid = quads(tJei, state(Direction.NORTH, 16), RenderType.solid());
		assertEquals(1, tSolid.size(), "the solid pass carries the body only");
		assertEquals(1, quads(tJei, state(Direction.NORTH, 0), RenderType.cutout()).size(),
				":244 — the EMPTY stone carries no glyph");
		assertEquals(1, quads(tJei, state(Direction.NORTH, 16), null).size(), "the unculled pass sees no glyph");
		assertEquals(2, quads(tJei, state(Direction.NORTH, 8), RenderType.cutout()).size(),
				"any loaded value (8) shows the glyph — the upstream mStone!=0 form");

		GT6GrindstoneNeiModel tNone = model(null);
		assertEquals(1, quads(tNone, state(Direction.NORTH, 16), RenderType.cutout()).size(), "no viewer → no patch at all");
	}

	/** The tile follows the SAME predicate the jump route uses — EMI first (what you see is what you jump to). */
	@Test
	void theTileFollowsTheViewerPredicateEmiFirst() {
		for (BakedQuad tGlyph : quads(model("jei"), state(Direction.NORTH, 16), RenderType.cutout()).subList(1, 2)) {
			assertEquals("gt6:block/tools/kitchen_nei_jei", spriteId(tGlyph).toString(), "JEI-only → the JEI tile");
		}
		for (BakedQuad tGlyph : quads(model("emi"), state(Direction.NORTH, 16), RenderType.cutout()).subList(1, 2)) {
			assertEquals("gt6:block/tools/kitchen_nei_emi", spriteId(tGlyph).toString(), "EMI present → the EMI tile");
		}
	}

	/**
	 * The upstream pass-0 post-top corner per FACING AXIS (:211 — the Z-form post at
	 * (6,3,2)-(8,15,4), the X-form at (2,3,6)-(4,15,8); the glyph is the post's top face).
	 * Baked quad vertices live in the 0..1 BLOCK space (the r3 lesson — pin against the px
	 * values divided by 16); the glyph plane protrudes the lift eps off the 15px shoulder.
	 */
	@Test
	void theGlyphIsTheUpstreamPostTopCornerPerFacingAxis() {
		// NORTH (Z-axis): the glyph at x 6..8px, z 2..4px, ON the 15px plate top
		List<BakedQuad> tNorth = quads(model("jei"), state(Direction.NORTH, 16), RenderType.cutout()).subList(1, 2);
		BakedQuad tGlyphN = tNorth.get(0);
		assertEquals(Direction.UP, tGlyphN.getDirection(), "the post TOP face");
		assertEquals(-1, tGlyphN.getTintIndex(), "the decal is untinted (the yellow is baked in, the P22 contract)");
		AABB tBoxN = boxOf(tGlyphN);
		assertEquals(6.0 / 16.0, tBoxN.minX, 1e-7, "the Z-form post top starts at x6");
		assertEquals(8.0 / 16.0, tBoxN.maxX, 1e-7, "2x2px");
		// the quad is PLANAR on the lifted shoulder — (15px + lift)/16 (the kitchen form:
		// only the lifted plane is pinned, GT6KitchenNeiModelTest "PX_N[8]+0.001 on the rim")
		assertEquals((15.0 + GT6GrindstoneNeiModel.NEI_LIFT) / 16.0, tBoxN.maxY, 1e-7, "the lifted 15px plate shoulder");
		assertEquals(tBoxN.minY, tBoxN.maxY, 1e-7, "the UP quad is planar on the lifted shoulder");
		assertEquals(2.0 / 16.0, tBoxN.minZ, 1e-7, "the Z-form post top starts at z2");
		assertEquals(4.0 / 16.0, tBoxN.maxZ, 1e-7, "2x2px in z");

		// SOUTH shares the Z-form (the upstream geometry is axis-signed-blind) — same spot
		AABB tBoxS = boxOf(quads(model("jei"), state(Direction.SOUTH, 16), RenderType.cutout()).get(1));
		assertEquals(tBoxN, tBoxS, "SOUTH renders the identical Z-form corner");

		// EAST (X-axis): the coordinate-swap corner at x 2..4px, z 6..8px
		AABB tBoxE = boxOf(quads(model("jei"), state(Direction.EAST, 16), RenderType.cutout()).get(1));
		assertEquals(2.0 / 16.0, tBoxE.minX, 1e-7, "the X-form post top starts at x2");
		assertEquals(4.0 / 16.0, tBoxE.maxX, 1e-7, "2x2px");
		assertEquals(6.0 / 16.0, tBoxE.minZ, 1e-7, "the X-form post top starts at z6");
		assertEquals(8.0 / 16.0, tBoxE.maxZ, 1e-7, "2x2px in z");
		assertFalse(tBoxE.equals(tBoxN), "the two axis forms differ (the swap)");
	}

	/** The seat: cutout joins the fallback's layers exactly when the glyph exists. */
	@Test
	void theRenderSeatJoinsCutoutOnlyWithAViewer() {
		RandomSource tRand = RandomSource.create();
		ChunkRenderTypeSet tWith = model("jei").getRenderTypes(null, tRand, ModelData.EMPTY);
		assertTrue(tWith.contains(RenderType.solid()), "the body keeps the fallback's solid seat");
		assertTrue(tWith.contains(RenderType.cutout()), "the glyphs add the cutout seat");

		ChunkRenderTypeSet tWithout = model(null).getRenderTypes(null, tRand, ModelData.EMPTY);
		assertTrue(tWithout.contains(RenderType.solid()), "no viewer → the fallback seat unchanged");
		assertFalse(tWithout.contains(RenderType.cutout()), "no viewer → no cutout seat added");
	}

	/** The body pass passes through verbatim today (the dispatch-less grindstone resolves the -1 sentinel). */
	@Test
	void theBodyPassIsUntouchedVerbatim() {
		List<BakedQuad> tBody = List.of(bodyQuad());
		GT6GrindstoneNeiModel tModel = new GT6GrindstoneNeiModel(fallback(tBody), sGrindstone, stubLookup(), () -> "jei");
		assertSame(tBody, tModel.getQuads(state(Direction.NORTH, 16), null, RandomSource.create(), ModelData.EMPTY, RenderType.solid()),
				"the solid pass IS the fallback's list — the white identity is the vanilla -1 no-tint sentinel");
	}

	/**
	 * The blockstate census anchor (the acceptance ① blockstate-existence leg): the
	 * grindstone ships its generated blockstate JSON with the FULL FACING×STONE coverage
	 * the wrap keys off (68 variants).
	 */
	@Test
	void theGrindstoneBlockstateShipsTheFullCoverage() throws Exception {
		Path tMdk = locateMdkRoot();
		assertNotNull(tMdk, "mdk root not found from the test working directory");
		Path tJson = tMdk.resolve("src/generated/resources/assets/gt6/blockstates/grindstone.json");
		assertTrue(Files.isRegularFile(tJson), "the grindstone blockstate ships");
		String tBody = Files.readString(tJson);
		int tVariants = tBody.split("\"model\"").length - 1;
		assertEquals(68, tVariants, "4 FACING x 17 STONE variants");
		for (String tForm : new String[] {"grindstone_z", "grindstone_z_stone", "grindstone_x", "grindstone_x_stone"}) {
			assertTrue(tBody.contains("gt6:block/" + tForm), "the model seat " + tForm + " is wired");
		}
	}

	/** The mdk root from the test working directory (the GT6KitchenNeiTextureCensusTest form). */
	private static Path locateMdkRoot() {
		Path tDir = Path.of("").toAbsolutePath();
		for (int i = 0; i < 8 && tDir != null; i++, tDir = tDir.getParent()) {
			if (Files.isRegularFile(tDir.resolve("src/main/java/gregtech6/datagen/GT6BlockStates.java"))) {
				return tDir;
			}
		}
		return null;
	}
}
