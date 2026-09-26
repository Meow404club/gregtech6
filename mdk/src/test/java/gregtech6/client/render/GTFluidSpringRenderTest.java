/**
 * Offline census for task p38-spring-texture-tint — the spring nozzle's borrowed skin +
 * per-fluid tint wave. The pins:
 * <ul>
 * <li>the borrowed FLUID_SPRING dither ({@code block/fluid_spring.png}, the upstream
 *     iconsets PNG byte-copy) is grounded in assets/README.md by name AND by the actual
 *     sha256 of its bytes (the p31 attribution-nail pattern);</li>
 * <li>{@link GTFluidSpringBakedModel#skinOf} resolves the per-fluid colour mapping off the
 *     SAME spec tables the fluid registrations read — three-plus fluids pinned to their
 *     exact tint values (the card's "抽 3 流体断言色值", exceeded);</li>
 * <li>the BE {@code getModelData} carries the spring identity under
 *     {@link GTModelProperties#SPRING_FLUID} (the paint arm's snapshot shape);</li>
 * <li>the baked model emits the TINTED fluid base + the UNTINTED dither overlay per face
 *     (upstream :150 {@code BlockTextureMulti(BlockTextureFluid(mFluid), FLUID_SPRING)} —
 *     the tint lives on the fluid layer, the dither passes white), both colours baked into
 *     the vertex data with {@code tintIndex -1} (the p32 no-double-dye form);</li>
 * <li>all sixteen {@code FLUID_SPRING_TABLE} rows resolve a skin, collapsing onto exactly
 *     the seven distinct spring fluids.</li>
 * </ul>
 * Offline: a {@code UnitTextureAtlasSprite} stub stands in for the atlas (the
 * GTOreBakedModelTintTest form) and a stub fallback stands for the baked JSON model.
 */
package gregtech6.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

//? if forge {
import net.minecraftforge.client.textures.UnitTextureAtlasSprite;
//?} else {
/*import net.neoforged.neoforge.client.textures.UnitTextureAtlasSprite;*/
//?}
import net.minecraftforge.client.model.data.ModelData;

import gregtech6.datagen.GT6WorldgenDatagen;
import gregtech6.tileentity.misc.GTFluidSpringBlockEntity;
import gregtech6.worldgen.GTFluidSpringConfig;

public class GTFluidSpringRenderTest extends GTOfflineRenderTestBase {

	private static final int COLOR_SLOT = 3;
	private static final int WHITE = 0xFFFFFFFF;

	/** The spec-table tints the three-plus colour pins assert (GTFluids CHEMICAL_SPECS/AQUA_SPECS rows verbatim). */
	private static final int MEDIUM_OIL_TINT = 0xFF322814; // liquid_medium_oil, GTFluids.java:2153
	/** The same tint as the baked COLOR SLOT stores it: ABGR, R/B halves swapped (issue #14). */
	private static final int MEDIUM_OIL_TINT_ABGR =
			(MEDIUM_OIL_TINT & 0xFF00FF00) | ((MEDIUM_OIL_TINT >> 16) & 0x000000FF) | ((MEDIUM_OIL_TINT << 16) & 0x00FF0000);
	private static final int EXTRA_HEAVY_OIL_TINT = 0xFF1E140A; // liquid_extra_heavy_oil, :2151
	private static final int GEOTHERMAL_TINT = 0xFF3EC8B4; // water_geothermal, the AQUA_SPECS row
	private static final int NATURAL_GAS_TINT = 0x66FFF2B0; // the p5 NATURAL_GAS_TYPE inline tint

	/** The mdk root (src/main/resources/assets/README.md), walked upward from the leg-dependent test working dir. */
	private static Path mdkRoot() {
		for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
			if (Files.isRegularFile(p.resolve("src/main/resources/assets/README.md"))) return p;
		}
		throw new AssertionError("mdk root (src/main/resources/assets/README.md) not found upward from "
				+ Path.of("").toAbsolutePath());
	}

	/** The model over the unit-sprite stub + a stub fallback JSON model (the lazy-bake face needs a live fallback). */
	private static GTFluidSpringBakedModel springModel() {
		return new GTFluidSpringBakedModel(stubFallback(), aMaterial -> UnitTextureAtlasSprite.INSTANCE);
	}

	/** The barest fallback: the plain JSON-model stand-in with a fixed solid render type. */
	private static BakedModel stubFallback() {
		return new BakedModel() {
			@Override
			public java.util.List<BakedQuad> getQuads(@org.jetbrains.annotations.Nullable BlockState aState,
					@org.jetbrains.annotations.Nullable Direction aSide, RandomSource aRand) {
				return java.util.List.of();
			}

			@Override
			public boolean useAmbientOcclusion() {return false;}

			@Override
			public boolean isGui3d() {return false;}

			@Override
			public boolean usesBlockLight() {return false;}

			@Override
			public boolean isCustomRenderer() {return false;}

			@Override
			public net.minecraft.client.renderer.block.model.ItemTransforms getTransforms() {
				return net.minecraft.client.renderer.block.model.ItemTransforms.NO_TRANSFORMS;
			}

			@Override
			public net.minecraft.client.renderer.block.model.ItemOverrides getOverrides() {
				return net.minecraft.client.renderer.block.model.ItemOverrides.EMPTY;
			}

			@Override
			public net.minecraft.client.renderer.texture.TextureAtlasSprite getParticleIcon() {
				return UnitTextureAtlasSprite.INSTANCE;
			}

			@Override
			public net.minecraftforge.client.ChunkRenderTypeSet getRenderTypes(BlockState aState,
					RandomSource aRand, ModelData aData) {
				return net.minecraftforge.client.ChunkRenderTypeSet.of(RenderType.solid());
			}
		};
	}

	/** The snapshot the chunk build would hand in (the BE's getModelData product, built directly). */
	private static ModelData springData(String aBlockId) {
		return GTModelProperties.snapshot()
				.with(GTModelProperties.SPRING_FLUID, aBlockId)
				.build();
	}

	// ---------------------------------------------------------------------------
	// the borrow: the ledger nail
	// ---------------------------------------------------------------------------

	/** The borrowed dither exists and its bytes hash to the sha256 the ledger records. */
	@Test
	public void borrowedDitherIsGroundedInTheAssetsLedger() throws Exception {
		InputStream tStream = GTFluidSpringRenderTest.class.getResourceAsStream("/assets/gt6/textures/block/fluid_spring.png");
		assertNotNull(tStream, "the FLUID_SPRING borrow must live at textures/block/fluid_spring.png");
		String tHex;
		try (tStream) {
			tHex = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(tStream.readAllBytes()));
		}
		String tReadme = Files.readString(mdkRoot().resolve("src/main/resources/assets/README.md"), StandardCharsets.UTF_8);
		assertTrue(tReadme.contains("fluid_spring.png"), "the borrow filename must be in assets/README.md");
		assertTrue(tReadme.contains(tHex), "the borrowed bytes hash to " + tHex + " — the ledger must record them");
	}

	// ---------------------------------------------------------------------------
	// the colour source: the per-fluid skin mapping (the card's 3-fluid pin, exceeded)
	// ---------------------------------------------------------------------------

	/** The colour mapping rides the SAME spec tables the registrations read — exact tints per fluid. */
	@Test
	public void skinOfPinsThePerFluidColourMapping() {
		ResourceLocation tWaterStill = ResourceLocation.withDefaultNamespace("block/water_still");
		ResourceLocation tLavaStill = ResourceLocation.withDefaultNamespace("block/lava_still");

		GTFluidSpringBakedModel.SpringSkin tMedium = GTFluidSpringBakedModel.skinOf("gt6:liquid_medium_oil_block");
		assertEquals(tWaterStill, tMedium.stillSprite(), "the gt6 fluids' shared still carrier");
		assertEquals(MEDIUM_OIL_TINT, tMedium.tintARGB(), "raw oil = the CHEMICAL_SPECS row tint");

		assertEquals(EXTRA_HEAVY_OIL_TINT, GTFluidSpringBakedModel.skinOf("gt6:liquid_extra_heavy_oil_block").tintARGB(),
				"extra-heavy oil = its own ramp row — the oils must NOT collapse to one colour");

		GTFluidSpringBakedModel.SpringSkin tGeo = GTFluidSpringBakedModel.skinOf("gt6:water_geothermal_block");
		assertEquals(GEOTHERMAL_TINT, tGeo.tintARGB(), "geothermal water = the AQUA_SPECS row tint");

		GTFluidSpringBakedModel.SpringSkin tGas = GTFluidSpringBakedModel.skinOf("gt6:natural_gas_block");
		assertEquals(NATURAL_GAS_TINT, tGas.tintARGB(), "natural gas = the p5 inline tint (alpha rides along)");

		GTFluidSpringBakedModel.SpringSkin tLava = GTFluidSpringBakedModel.skinOf("minecraft:lava");
		assertEquals(tLavaStill, tLava.stillSprite(), "lava keeps its own coloured still");
		assertEquals(-1, tLava.tintARGB(), "lava = the white no-tint identity (the -1 sentinel)");

		// the loud-refusal faces: an unknown block id, a bare path, a null — all unresolvable
		assertNull(GTFluidSpringBakedModel.skinOf("gt6:not_a_fluid_block"), "an unknown gt6 block id");
		assertNull(GTFluidSpringBakedModel.skinOf("garbage"), "a non-id string");
		assertNull(GTFluidSpringBakedModel.skinOf(null), "no spring");
	}

	// ---------------------------------------------------------------------------
	// the ModelData seam: the BE carries the spring identity
	// ---------------------------------------------------------------------------

	/**
	 * The BE's getModelData carries the spring block id exactly while a spring is set (the
	 * paint arm's shape). The pure {@code springModelData} seam runs on BOTH legs; the
	 * bare-BE end-to-end face rides the forge leg only — the 1.21.1 BlockEntity ctor
	 * validates its state against the type (BlockEntity.java:46-57), so the null-type
	 * bare face cannot be CONSTRUCTED there, unlike 1.20.1.
	 */
	@Test
	public void modelDataCarriesTheSpringIdentity() {
		assertFalse(GTFluidSpringBlockEntity.springModelData(ModelData.EMPTY, null).has(GTModelProperties.SPRING_FLUID),
				"no spring -> the absent-property face (the fallback JSON)");
		var tSeam = GTFluidSpringBlockEntity.springModelData(ModelData.EMPTY, "gt6:liquid_medium_oil_block");
		assertTrue(tSeam.has(GTModelProperties.SPRING_FLUID), "a spring set -> the property present");
		assertEquals("gt6:liquid_medium_oil_block", tSeam.get(GTModelProperties.SPRING_FLUID),
				"the property value IS the spring block id the model resolves off");

		//? if forge {
		GTFluidSpringBlockEntity tBe = new GTFluidSpringBlockEntity();
		assertFalse(tBe.getModelData().has(GTModelProperties.SPRING_FLUID),
				"the bare BE: absent property before a spring is set");
		tBe.setSpring("gt6:liquid_medium_oil_block", 6000);
		assertTrue(tBe.getModelData().has(GTModelProperties.SPRING_FLUID), "the override wires the seam");
		assertEquals("gt6:liquid_medium_oil_block", tBe.getModelData().get(GTModelProperties.SPRING_FLUID),
				"the override value = the seam value");
		//?}
	}

	// ---------------------------------------------------------------------------
	// the baked quads: tinted fluid base + untinted dither, both layers per face
	// ---------------------------------------------------------------------------

	/** The north pair: quads[0] = the tinted fluid base, quads[1] = the white dither shell (the bakeQuads order). */
	@Test
	public void bakedModelEmitsTheTintedBaseAndTheUntintedDither() {
		GTFluidSpringBakedModel tModel = springModel();
		ModelData tData = springData("gt6:liquid_medium_oil_block");
		RandomSource tRand = RandomSource.create();

		assertEquals(12, tModel.getQuads(null, null, tRand, tData, null).size(),
				"six base + six overlay quads over the six faces (the null pass = all, IForgeBakedModel)");
		assertEquals(2, tModel.getQuads(null, Direction.UP, tRand, tData, null).size(),
				"one base + one overlay per culled face");
		assertEquals(0, tModel.getQuads(null, null, tRand, tData, RenderType.translucent()).size(),
				"no translucent emission — the spring is a solid+cutout stack");
		// the #16 cull sync (the GTOreBakedModel ruling): the null-SIDE chunk pass (the
		// unconditional one, ModelBlockRenderer.java:81-85/:106-110) receives nothing —
		// the quads flow through the per-direction, neighbour-culled passes only
		assertEquals(0, tModel.getQuads(null, null, tRand, tData, RenderType.solid()).size(),
				"the null-side solid pass is empty (cullface-synced)");
		assertEquals(0, tModel.getQuads(null, null, tRand, tData, RenderType.cutout()).size(),
				"the null-side cutout pass is empty (no uncullfaced dither hairlines)");

		// the issue #2 same-type partition: the tinted fluid body rides solid ALONE, the
		// dither shell cutout ALONE — on the alpha-less solid shader the shell's 116
		// transparent holes would paint opaque gray over the fluid body
		List<BakedQuad> tSolid = tModel.getQuads(null, Direction.NORTH, tRand, tData, RenderType.solid());
		assertEquals(1, tSolid.size(), "the solid pass receives the fluid body alone");
		assertEquals(MEDIUM_OIL_TINT_ABGR, tSolid.get(0).getVertices()[COLOR_SLOT], "the solid quad is the tinted base (ABGR slot)");

		List<BakedQuad> tCutout = tModel.getQuads(null, Direction.NORTH, tRand, tData, RenderType.cutout());
		assertEquals(1, tCutout.size(), "the cutout pass receives the dither shell alone");
		assertEquals(WHITE, tCutout.get(0).getVertices()[COLOR_SLOT],
				"the FLUID_SPRING dither is UNTINTED — upstream's BlockTextureDefault half is white; the fluid colour lives on the layer beneath");

		var tPair = tModel.getQuads(null, Direction.NORTH, tRand, tData, null);
		BakedQuad tBase = tPair.get(0);
		assertEquals(-1, tBase.getTintIndex(), "the base drops tintIndex 0 (no runtime lookup can double-dye)");
		assertEquals(MEDIUM_OIL_TINT_ABGR, tBase.getVertices()[COLOR_SLOT], "white x tint = the tint, stored ABGR in the slot (issue #14)");

		BakedQuad tOverlay = tPair.get(1);
		assertEquals(-1, tOverlay.getTintIndex(), "the dither shell drops the tint index too");
		assertEquals(WHITE, tOverlay.getVertices()[COLOR_SLOT],
				"the dither passes white on the null pass too");
	}

	/** The white identity face: the lava row's -1 sentinel bakes the raw quad (no retint pass). */
	@Test
	public void lavaSkinBakesTheWhiteIdentity() {
		GTFluidSpringBakedModel tModel = springModel();
		ModelData tData = springData("minecraft:lava");
		BakedQuad tBase = tModel.getQuads(null, Direction.NORTH, RandomSource.create(), tData, null).get(0);
		assertEquals(WHITE, tBase.getVertices()[COLOR_SLOT], "the -1 sentinel bakes the white identity");
	}

	/** An unresolvable id (registry drift face) renders the dither shell only — visible, never invisible. */
	@Test
	public void unresolvableIdRendersTheDitherOnly() {
		GTFluidSpringBakedModel tModel = springModel();
		ModelData tData = springData("gt6:not_a_fluid_block");
		var tQuads = tModel.getQuads(null, null, RandomSource.create(), tData, null);
		assertEquals(6, tQuads.size(), "the overlay shell alone, six faces");
		for (BakedQuad tQuad : tQuads) {
			assertEquals(WHITE, tQuad.getVertices()[COLOR_SLOT], "the dither carries no colour in either edition");
		}
	}

	/** Render types: the dynamic face is the solid+cutout set, the miss delegates to the fallback (the JSON solid face). */
	@Test
	public void renderTypesFollowTheDynamicDispatch() {
		GTFluidSpringBakedModel tModel = springModel();
		var tDynamic = tModel.getRenderTypes(null, RandomSource.create(), springData("gt6:liquid_medium_oil_block"));
		assertTrue(tDynamic.contains(RenderType.solid()) && tDynamic.contains(RenderType.cutout()),
				"the tinted base needs solid, the alpha dither needs cutout");
		var tFallback = tModel.getRenderTypes(null, RandomSource.create(), ModelData.EMPTY);
		assertTrue(tFallback.contains(RenderType.solid()), "the fallback JSON face stays on solid");
	}

	// ---------------------------------------------------------------------------
	// the sixteen-row census: every row a skin, seven distinct fluids
	// ---------------------------------------------------------------------------

	/** Every FLUID_SPRING_TABLE row's blockId resolves; the rows collapse onto exactly 7 distinct skins. */
	@Test
	public void everySpringTableRowResolvesASkinAndCollapsesOntoSevenFluids() {
		assertEquals(16, GT6WorldgenDatagen.FLUID_SPRING_TABLE.size(), "the one 16-row table");
		for (GTFluidSpringConfig tRow : GT6WorldgenDatagen.FLUID_SPRING_TABLE) {
			assertNotNull(GTFluidSpringBakedModel.skinOf(tRow.blockId()),
					tRow.name() + " (" + tRow.blockId() + ") must resolve a skin — no row renders the invisible base");
		}
		long tDistinct = GT6WorldgenDatagen.FLUID_SPRING_TABLE.stream()
				.map(tRow -> GTFluidSpringBakedModel.skinOf(tRow.blockId()))
				.distinct().count();
		assertEquals(7, tDistinct, "4 oils + natural gas + geothermal water + lava — the distinct spring fluids");
		assertNotEquals(1, tDistinct, "the all-gray regression the card bans: the rows must NOT collapse to one face");
	}
}
