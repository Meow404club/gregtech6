/**
 * The sifting-table NEI corner glyph bake pins (task sifting-table-family, the
 * GT6AnvilNeiModelTest shape over the SINGLE-SPOT form): the patch exists only when a
 * viewer is present (the bake-time GTViewerJump.preferredViewer gate), picks ITS tile
 * from the same predicate (EMI first — what you see is what you jump to), rides the
 * cutout layer (the transparent glyph margins must discard, the
 * r11-oven-solid-layer-fix lesson), carries the upstream leg-top geometry — one 2x2px
 * tile at Y 13 (MultiTileEntitySiftingTable.java:393 the pass-0 box top, :420 the
 * SIDES_TOP seat), on the UP face — and the body seats resolve the ANY.Steel row through
 * the self-tint arm (the census L3 closure: the Steel gray-white product; the decals
 * pass through untinted). The LIVE wrap (listener + atlas stitching + JEI/EMI in a real
 * client) is the field_test.
 */
package gregtech6.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
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

import gregapi.data.ANY;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class GT6SiftingTableNeiModelTest extends GTOfflineRenderTestBase {

	private static final int STRIDE = 8;

	/** The offline table carrier (the tintMaterialOf dispatch reads its ANY.Steel row). */
	private static gregtech6.block.tools.GT6SiftingTableBlock sTable;

	@BeforeAll
	static void buildOfflineFixtures() {
		// the hermetic material boot first (the GT6HopperFamilyTest r11e house rule) — the
		// carrier's ANY.Steel supplier resolves live; then the BLOCK registry write window
		// (the GT6GrindstoneNeiModelTest recipe) for the carrier ctor.
		gregtech6.registry.GT6MaterialTestSupport.materials();
		try {
			java.lang.reflect.Method tUnfreeze = net.minecraft.core.registries.BuiltInRegistries.BLOCK
					.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(net.minecraft.core.registries.BuiltInRegistries.BLOCK);
		} catch (Exception ignored) {
			// already unfrozen by a sibling fixture
		}
		sTable = new gregtech6.block.tools.GT6SiftingTableBlock(() -> gregapi.data.ANY.Steel, () -> null,
				net.minecraft.world.level.block.state.BlockBehaviour.Properties.of());
	}

	/** A named atlas stub — the name is the pin's membership key (the GT6AnvilNeiModelTest form). */
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

	/** A plain body quad (32 ints, white) — the colored-band seat face (tintindex 0, the datagen seat). */
	private static BakedQuad bodyQuad() {
		int[] tVertices = new int[4 * STRIDE];
		java.util.Arrays.fill(tVertices, 0xFFFFFFFF);
		return new BakedQuad(tVertices, 0, Direction.UP, new NamedSprite(
				ResourceLocation.fromNamespaceAndPath("minecraft", "block/smooth_stone")), true);
	}

	/** An overlay-twin quad (tintindex -1, the P22 untinted decal). */
	private static BakedQuad decalQuad() {
		int[] tVertices = new int[4 * STRIDE];
		java.util.Arrays.fill(tVertices, 0xFFFFFFFF);
		return new BakedQuad(tVertices, -1, Direction.UP, new NamedSprite(
				ResourceLocation.fromNamespaceAndPath("minecraft", "block/smooth_stone")), true);
	}

	/** ARGB → the baked COLOR slot's ABGR byte order (the GTAxleTintArmTest form). */
	private static int abgrOf(int aArgb) {
		return (aArgb & 0xFF00FF00) | ((aArgb & 0xFF) << 16) | ((aArgb >> 16) & 0xFF);
	}

	/** The baked vertex colour slot (stride 8, COLOR = 3). */
	private static int vertexColour(BakedQuad aQuad) {
		return aQuad.getVertices()[3];
	}

	private static GT6SiftingTableNeiModel model(String aViewer) {
		return new GT6SiftingTableNeiModel(fallback(List.of(bodyQuad())), sTable, stubLookup(), () -> aViewer);
	}

	private static List<BakedQuad> quads(GT6SiftingTableNeiModel aModel, RenderType aLayer) {
		return aModel.getQuads(null, null, RandomSource.create(), ModelData.EMPTY, aLayer);
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
		GT6SiftingTableNeiModel tJei = model("jei");
		List<BakedQuad> tCutout = quads(tJei, RenderType.cutout());
		assertEquals(2, tCutout.size(), "the body + the one leg-top glyph");
		assertEquals(1, quads(tJei, RenderType.solid()).size(), "the solid pass carries the body only");
		assertEquals(1, quads(tJei, null).size(), "the unculled pass sees no glyph (the oven convention)");

		GT6SiftingTableNeiModel tNone = model(null);
		assertEquals(1, quads(tNone, RenderType.cutout()).size(), "no viewer → no patch at all (the nei ruling)");
		assertEquals(1, quads(tNone, RenderType.solid()).size(), "no viewer → the plain body everywhere");
	}

	/** The tile follows the SAME predicate the jump route uses — EMI first (what you see is what you jump to). */
	@Test
	void theTileFollowsTheViewerPredicateEmiFirst() {
		assertEquals("gt6:block/tools/kitchen_nei_jei",
				spriteId(quads(model("jei"), RenderType.cutout()).get(1)).toString(), "JEI-only → the JEI tile");
		assertEquals("gt6:block/tools/kitchen_nei_emi",
				spriteId(quads(model("emi"), RenderType.cutout()).get(1)).toString(), "EMI present → the EMI tile (the dual-install ruling)");
	}

	/**
	 * The upstream leg-top seat (MultiTileEntitySiftingTable.java:393 the pass-0 box top,
	 * :420 the SIDES_TOP face): one 2x2px tile in the (0..2)x(0..2) corner at Y 13, on the
	 * UP face. Baked quad vertices live in the 0..1 BLOCK space (the r3 lesson — pin
	 * against the px values divided by 16); the glyph plane protrudes by the LIFT_PX eps
	 * so it never z-fights the leg top.
	 */
	@Test
	void theGlyphIsTheUpstreamLegTopCorner() {
		BakedQuad tGlyph = quads(model("jei"), RenderType.cutout()).get(1);
		assertEquals(Direction.UP, tGlyph.getDirection(), "the SIDES_TOP seat is the UP face");
		assertEquals(-1, tGlyph.getTintIndex(), "the decal is untinted (the yellow is baked in, the P22 contract)");
		AABB tBox = boxOf(tGlyph);
		assertEquals(0.0, tBox.minX, 1e-7, "the glyph starts at the corner (the pass-0 box x 0..2)");
		assertEquals(2.0 / 16.0, tBox.maxX, 1e-7, "2x2px — the upstream glyph box");
		assertEquals(0.0, tBox.minZ, 1e-7, "the corner z 0..2");
		assertEquals(2.0 / 16.0, tBox.maxZ, 1e-7, "2x2px in z");
		assertEquals(13.0 / 16.0 + (double) (GT6SiftingTableNeiModel.LIFT_PX / 16.0F), tBox.minY, 1e-7,
				"the leg-top plane Y 13 protrudes the lift eps");
		assertEquals(tBox.minY, tBox.maxY, 1e-7, "the glyph is the flat UP tile");
	}

	/** The seat: cutout joins the fallback's layers exactly when the glyph exists. */
	@Test
	void theRenderSeatJoinsCutoutOnlyWithAViewer() {
		RandomSource tRand = RandomSource.create();
		ChunkRenderTypeSet tWith = model("jei").getRenderTypes(null, tRand, ModelData.EMPTY);
		assertTrue(tWith.contains(RenderType.solid()), "the body keeps the fallback's solid seat");
		assertTrue(tWith.contains(RenderType.cutout()), "the glyph adds the cutout seat");

		ChunkRenderTypeSet tWithout = model(null).getRenderTypes(null, tRand, ModelData.EMPTY);
		assertTrue(tWithout.contains(RenderType.solid()), "no viewer → the fallback seat unchanged");
		assertFalse(tWithout.contains(RenderType.cutout()), "no viewer → no cutout seat added");
	}

	/**
	 * The body pass resolves the ANY.Steel row through the self-tint arm (task
	 * tint-chain-hopper-grindstone-sifting, the census L3 closure): the tintindex-0 seat
	 * quad comes back as the retinted copy carrying the Steel product (the ANY.Steel row
	 * steals the MT.Steel looks — upstream ANY.java:120 / port ANY.java:200 — so
	 * 130,130,130), while the untinted decal quad passes through as the shared instance
	 * (the P22 contract).
	 */
	@Test
	void theBodySeatTintsAndTheDecalPassesThrough() {
		BakedQuad tSeatQuad = bodyQuad(); // tintindex 0 — the datagen colored-band seat
		BakedQuad tDecalQuad = decalQuad(); // tintindex -1 — the overlay twin
		GT6SiftingTableNeiModel tModel = new GT6SiftingTableNeiModel(fallback(List.of(tSeatQuad, tDecalQuad)),
				sTable, stubLookup(), () -> "jei");
		List<BakedQuad> tOut = tModel.getQuads(null, null, RandomSource.create(), ModelData.EMPTY, RenderType.solid());
		assertEquals(2, tOut.size());
		assertNotSame(tSeatQuad, tOut.get(0), "the seat quad is the retinted copy");
		assertSame(tDecalQuad, tOut.get(1), "the decal quad passes through as the shared instance");
		int tSteelTint = GTMachinePaintTint.tintARGB(ModelData.EMPTY, ANY.Steel, 0);
		assertEquals(abgrOf(tSteelTint), vertexColour(tOut.get(0)), "the body colour IS the ANY.Steel row product");
	}

	/** The blockstate census anchor: the single-variant table ships its generated blockstate JSON (no facing axis). */
	@Test
	void theTableBlockstateShips() throws Exception {
		Path tMdk = locateMdkRoot();
		assertNotNull(tMdk, "mdk root not found from the test working directory");
		Path tJson = tMdk.resolve("src/generated/resources/assets/gt6/blockstates/sifting_table.json");
		assertTrue(Files.isRegularFile(tJson), "sifting_table blockstate ships");
	}

	/** The mdk root from the test working directory (the GT6AnvilNeiModelTest form). */
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
