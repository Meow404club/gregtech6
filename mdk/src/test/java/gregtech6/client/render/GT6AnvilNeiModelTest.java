/**
 * The anvil NEI legs-band glyph bake pins (task manual-nei-four-family, the
 * GT6KitchenNeiModelTest shape): the patch exists only when a viewer is present (the
 * bake-time GTViewerJump.preferredViewer gate), picks ITS tile from the same predicate
 * (EMI first — what you see is what you jump to), rides the cutout layer (the transparent
 * glyph margins must discard, the r11-oven-solid-layer-fix lesson), carries the upstream
 * pass-5 foot-band geometry per FACING (MultiTileEntityAnvil.java:343-352 the box table:
 * a 2x2px tile at Y 0..2, inset 2px from the facing-side edge, on BOTH along-axis faces),
 * and never perturbs the body quads (the anvil carries no tint — the plain cubeAll body
 * passes through verbatim). The blockstate census anchor: both anvil rows ship their
 * generated blockstate JSONs with the full facing coverage the wrap keys off. The LIVE
 * wrap (listener + atlas stitching + JEI/EMI in a real client) is the field_test.
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.block.tools.GTAnvilBlock;

public class GT6AnvilNeiModelTest extends GTOfflineRenderTestBase {

	private static final int STRIDE = 8;

	private static GTAnvilBlock sAnvil;

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
		// the BET supplier stays null — the model never resolves it (only FACING is read)
		sAnvil = new GTAnvilBlock(() -> gregapi.data.MT.Stone, 10000, () -> null,
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

	/** A plain body quad (32 ints, white) — the anvil's cubeAll face (no tintindex). */
	private static BakedQuad bodyQuad() {
		int[] tVertices = new int[4 * STRIDE];
		java.util.Arrays.fill(tVertices, 0xFFFFFFFF);
		return new BakedQuad(tVertices, -1, Direction.UP, new NamedSprite(
				ResourceLocation.fromNamespaceAndPath("minecraft", "block/smooth_stone")), true);
	}

	private static GT6AnvilNeiModel model(String aViewer) {
		return new GT6AnvilNeiModel(fallback(List.of(bodyQuad())), sAnvil, stubLookup(), () -> aViewer);
	}

	private static BlockState facing(Direction aFacing) {
		return sAnvil.defaultBlockState().setValue(GTAnvilBlock.FACING, aFacing);
	}

	private static List<BakedQuad> quads(GT6AnvilNeiModel aModel, BlockState aState, RenderType aLayer) {
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

	/** The glyphs exist ONLY with a viewer, on the cutout layer alone, and never disturb the body pass. */
	@Test
	void theGlyphsRideCutoutOnlyWhenAViewerIsPresent() {
		GT6AnvilNeiModel tJei = model("jei");
		List<BakedQuad> tCutout = quads(tJei, facing(Direction.NORTH), RenderType.cutout());
		assertEquals(3, tCutout.size(), "the body + the two along-axis face glyphs");
		List<BakedQuad> tSolid = quads(tJei, facing(Direction.NORTH), RenderType.solid());
		assertEquals(1, tSolid.size(), "the solid pass carries the body only");
		assertEquals(1, quads(tJei, facing(Direction.NORTH), null).size(), "the unculled pass sees no glyph (the oven convention)");

		GT6AnvilNeiModel tNone = model(null);
		assertEquals(1, quads(tNone, facing(Direction.NORTH), RenderType.cutout()).size(), "no viewer → no patch at all (the nei ruling)");
		assertEquals(1, quads(tNone, facing(Direction.NORTH), RenderType.solid()).size(), "no viewer → the plain body everywhere");
	}

	/** The tile follows the SAME predicate the jump route uses — EMI first (what you see is what you jump to). */
	@Test
	void theTileFollowsTheViewerPredicateEmiFirst() {
		GT6AnvilNeiModel tJei = model("jei");
		for (BakedQuad tGlyph : quads(tJei, facing(Direction.NORTH), RenderType.cutout()).subList(1, 3)) {
			assertEquals("gt6:block/tools/kitchen_nei_jei", spriteId(tGlyph).toString(), "JEI-only → the JEI tile");
		}
		GT6AnvilNeiModel tEmi = model("emi");
		for (BakedQuad tGlyph : quads(tEmi, facing(Direction.NORTH), RenderType.cutout()).subList(1, 3)) {
			assertEquals("gt6:block/tools/kitchen_nei_emi", spriteId(tGlyph).toString(), "EMI present → the EMI tile (the dual-install ruling)");
		}
	}

	/**
	 * The upstream pass-5 foot band per FACING (MultiTileEntityAnvil.java:343-352): a 2x2px
	 * tile at Y 0..2, inset 2px from the facing-side edge, on BOTH along-axis faces; the
	 * negative facings (NORTH/WEST) ride the PX_P[2] min-edge arm, the positive ones the
	 * PX_P[12] max-edge arm. Baked quad vertices live in the 0..1 BLOCK space (the r3
	 * lesson — pin against the px values divided by 16); the glyph plane protrudes by the
	 * LIFT_PX eps so it never z-fights the cube face (the upstream ±0.0001 expansion).
	 */
	@Test
	void theGlyphsAreTheUpstreamFootBandPerFacing() {
		// NORTH facing (the default): glyphs at x 2..4px on the NORTH plane (z=-eps) and the SOUTH plane (z=16+eps)
		List<BakedQuad> tNorth = quads(model("jei"), facing(Direction.NORTH), RenderType.cutout()).subList(1, 3);
		BakedQuad tFaceN = tNorth.get(0).getDirection() == Direction.NORTH ? tNorth.get(0) : tNorth.get(1);
		BakedQuad tFaceS = tNorth.get(0).getDirection() == Direction.SOUTH ? tNorth.get(0) : tNorth.get(1);
		assertEquals(Direction.NORTH, tFaceN.getDirection());
		assertEquals(Direction.SOUTH, tFaceS.getDirection());
		assertEquals(-1, tFaceN.getTintIndex(), "the decal is untinted (the yellow is baked in, the P22 contract)");
		AABB tBoxN = boxOf(tFaceN);
		assertEquals(2.0 / 16.0, tBoxN.minX, 1e-7, "the glyph insets PX_P[2] from the min edge");
		assertEquals(4.0 / 16.0, tBoxN.maxX, 1e-7, "2x2px — the upstream glyph box");
		assertEquals(0.0, tBoxN.minY, 1e-7, "the foot band starts at the floor");
		assertEquals(2.0 / 16.0, tBoxN.maxY, 1e-7, "the foot band is 2px tall (the upstream box Y 0..2)");
		assertEquals((double)(GT6AnvilNeiModel.LIFT_PX / 16.0F), -tBoxN.minZ, 1e-7, "the NORTH plane protrudes the lift eps");
		AABB tBoxS = boxOf(tFaceS);
		assertEquals(1.0 + (double)(GT6AnvilNeiModel.LIFT_PX / 16.0F), tBoxS.maxZ, 1e-7, "the SOUTH plane protrudes the lift eps");

		// SOUTH facing: the rail mirrors to the max-edge arm (x 12..14, the PX_P[12] variant)
		for (BakedQuad tGlyph : quads(model("jei"), facing(Direction.SOUTH), RenderType.cutout()).subList(1, 3)) {
			AABB tBox = boxOf(tGlyph);
			assertEquals(12.0 / 16.0, tBox.minX, 1e-7, "the SOUTH facing rides the PX_P[12] arm");
			assertEquals(14.0 / 16.0, tBox.maxX, 1e-7, "2x2px");
		}

		// WEST facing: the along-axis faces are WEST/EAST, the rail rides z 2..4 (the min-edge arm)
		List<BakedQuad> tWest = quads(model("jei"), facing(Direction.WEST), RenderType.cutout()).subList(1, 3);
		BakedQuad tFaceW = tWest.get(0).getDirection() == Direction.WEST ? tWest.get(0) : tWest.get(1);
		assertEquals(Direction.WEST, tFaceW.getDirection());
		AABB tBoxW = boxOf(tFaceW);
		assertEquals(2.0 / 16.0, tBoxW.minZ, 1e-7, "the X-facing rail insets PX_P[2] in z");
		assertEquals(4.0 / 16.0, tBoxW.maxZ, 1e-7, "2x2px in z");
		assertEquals((double)(GT6AnvilNeiModel.LIFT_PX / 16.0F), -tBoxW.minX, 1e-7, "the WEST plane protrudes the lift eps");
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

	/** The anvil body carries no tint — the fallback quads pass through verbatim (same instance). */
	@Test
	void theBodyPassIsUntouchedVerbatim() {
		List<BakedQuad> tBody = List.of(bodyQuad());
		GT6AnvilNeiModel tModel = new GT6AnvilNeiModel(fallback(tBody), sAnvil, stubLookup(), () -> "jei");
		assertSame(tBody, tModel.getQuads(facing(Direction.NORTH), null, RandomSource.create(), ModelData.EMPTY, RenderType.solid()),
				"the solid pass IS the fallback's list — no tint rewrap on the anvil (it has none)");
	}

	/**
	 * The blockstate census anchor (the acceptance ① blockstate-existence leg): both anvil
	 * rows ship their generated blockstate JSONs and carry the full FACING variant
	 * coverage the bake wrap keys off.
	 */
	@Test
	void theAnvilBlockstatesShipTheFacingCoverage() throws Exception {
		Path tMdk = locateMdkRoot();
		assertNotNull(tMdk, "mdk root not found from the test working directory");
		for (String tRow : new String[] {"stone_anvil", "blackstone_anvil"}) {
			Path tJson = tMdk.resolve("src/generated/resources/assets/gt6/blockstates/" + tRow + ".json");
			assertTrue(Files.isRegularFile(tJson), tRow + " blockstate ships");
			String tBody = Files.readString(tJson);
			for (String tFacing : new String[] {"facing=north", "facing=south", "facing=west", "facing=east"}) {
				assertTrue(tBody.contains(tFacing), tRow + " covers " + tFacing);
			}
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
