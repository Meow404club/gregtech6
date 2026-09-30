package gregtech6.client.render;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.data.ModelData;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.client.render.GTBoilerBarometerModel.GaugePlan;
import gregtech6.tileentity.energy.converters.GTBoilerTankBlockEntity;
import gregtech6.tileentity.multiblocks.TileEntityLargeBoiler;

/**
 * The boiler barometer render-path sentinel tests (task boiler-barometer, the
 * GTOvenOverlayModelTest shape): the upstream front-only stack as pure geometry (the
 * :240/:357-361 {@code aSide != mFacing ? null : ...} pick), the zero-padded needle
 * sprite ids, the stacked slab offsets clearing the static 0.01 decal, the dispatch gate
 * (BAROMETER on its own key), the canonical UV bake + the CA_RED_64 vertex retint (the
 * r8-uvof FaceBakePins form), the two BE gauge supplies, and the 144 per-state
 * registrations.
 */
public class GTBoilerBarometerModelTest extends GTOfflineRenderTestBase {

	@BeforeAll
	static void openTheFixtureWindow() throws Exception {
		gregtech6.registry.GTMaterialItems.initMaterials(); // the boiler row material() Suppliers resolve at tint time
		// the BLOCK registry write window (the GTOvenOverlayModelTest recipe): the offline
		// BoilerTankBlock fixture ctor registers its intrusive holder and the frozen
		// registry rejects it — unfreeze before the fixtures
		java.lang.reflect.Method tUnfreeze = net.minecraft.core.registries.BuiltInRegistries.BLOCK
				.getClass().getMethod("unfreeze");
		tUnfreeze.setAccessible(true);
		tUnfreeze.invoke(net.minecraft.core.registries.BuiltInRegistries.BLOCK);
		// the two BET-building tests need the BLOCK_ENTITY_TYPE registry writable —
		// on the neo FML JVM it boots frozen (the tileentity base's established helper,
		// the GT6BoilerProviderTest fixture posture); a no-op on the forge bare JVM
		gregtech6.tileentity.GTOfflineTestBase.unfreezeBlockEntityTypeRegistry();
	}

	@AfterEach
	void clearRegistration() {
		GTRenderModelListener.clearForTest();
	}

	// ---------------------------------------------------------------------------
	// the planner — the upstream front-only two-quad stack
	// ---------------------------------------------------------------------------

	@Test
	void gaugeStackPlansOnlyOnTheFrontFace() {
		for (Direction tFacing : Direction.values()) {
			for (Direction tSide : Direction.values()) {
				List<GaugePlan> tPlans = GTBoilerBarometerModel.planGaugeQuads(tFacing, tSide, 7);
				if (tSide == tFacing) {
					assertEquals(2, tPlans.size(), tFacing + ": dial + needle on the front pass");
				} else {
					assertTrue(tPlans.isEmpty(), tFacing + " vs " + tSide + ": the :240/:357 side gate (aSide != mFacing)");
				}
			}
			assertTrue(GTBoilerBarometerModel.planGaugeQuads(tFacing, null, 7).isEmpty(),
					tFacing + ": the unculled pass sees none (the quads carry a cullface)");
		}
	}

	@Test
	void dialThenNeedleCarryTheUpstreamSpriteIds() {
		List<GaugePlan> tPlans = GTBoilerBarometerModel.planGaugeQuads(Direction.NORTH, Direction.NORTH, 7);
		assertEquals(new ResourceLocation("gt6", "block/barometer/base"), tPlans.get(0).sprite(), "the dial");
		assertFalse(tPlans.get(0).needle());
		assertEquals(new ResourceLocation("gt6", "block/barometer/07"), tPlans.get(1).sprite(), "the zero-padded needle state");
		assertTrue(tPlans.get(1).needle());
		// the needle index binds: 0 and 31 are the upstream BAROMETER_SCALE bounds, out-of-range
		// clamps (the :232 &31 mask family)
		assertEquals(new ResourceLocation("gt6", "block/barometer/00"), GTBoilerBarometerModel.needleSprite(0));
		assertEquals(new ResourceLocation("gt6", "block/barometer/31"), GTBoilerBarometerModel.needleSprite(31));
		assertEquals(new ResourceLocation("gt6", "block/barometer/31"), GTBoilerBarometerModel.needleSprite(42));
		assertEquals(new ResourceLocation("gt6", "block/barometer/00"), GTBoilerBarometerModel.needleSprite(-3));
	}

	@Test
	void gaugePlatesStackClearOfTheStaticDecal() {
		double e = GTBoilerBarometerModel.OVERLAY_EPSILON;
		// the static boilerModel front decal plate sits 0.01 outside the face — the dial is
		// one epsilon clear of it, the needle one more
		double[] tDial = GTBoilerBarometerModel.slabOf(Direction.NORTH, GTBoilerBarometerModel.DIAL_OFFSET);
		assertEquals(-GTBoilerBarometerModel.DIAL_OFFSET, tDial[2], 1e-9, "the dial outer plane");
		assertEquals(GTBoilerBarometerModel.GAUGE_THICKNESS - GTBoilerBarometerModel.DIAL_OFFSET, tDial[5], 1e-9,
				"the dial inner plane, 1px inside");
		double[] tNeedle = GTBoilerBarometerModel.slabOf(Direction.NORTH, GTBoilerBarometerModel.NEEDLE_OFFSET);
		assertEquals(-(GTBoilerBarometerModel.DIAL_OFFSET + e), tNeedle[2], 1e-9, "the needle outer plane clears the dial");
		// the tangent axes inflate (the oven slab form) and the opposite horizontal faces mirror
		double[] tSouth = GTBoilerBarometerModel.slabOf(Direction.SOUTH, GTBoilerBarometerModel.DIAL_OFFSET);
		assertEquals(1 + GTBoilerBarometerModel.DIAL_OFFSET, tSouth[5], 1e-9, "the SOUTH outer plane");
		assertEquals(1 - GTBoilerBarometerModel.GAUGE_THICKNESS + GTBoilerBarometerModel.DIAL_OFFSET, tSouth[2], 1e-9,
				"the SOUTH inner plane");
		assertEquals(-e, tDial[0], 1e-9, "the tangent inflation");
		assertEquals(1 + e, tDial[3], 1e-9, "the tangent inflation");
	}

	@Test
	void frontOfReadsTheHorizontalFacing() {
		// both carriers share BlockStateProperties.HORIZONTAL_FACING (BoilerTankBlock.FACING ==
		// TileEntityBase10MultiBlockBase.FACING) — a vanilla furnace state exercises the pick
		BlockState tFurnace = Blocks.FURNACE.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.EAST);
		assertEquals(Direction.EAST, GTBoilerBarometerModel.frontOf(tFurnace));
		assertEquals(Direction.NORTH, GTBoilerBarometerModel.frontOf(Blocks.STONE.defaultBlockState()),
				"the no-facing default (defensive arm)");
	}

	// ---------------------------------------------------------------------------
	// the dispatch gate + the bake
	// ---------------------------------------------------------------------------

	@Test
	void dispatchGatesOnTheBarometerKeyOnly() {
		GTBoilerBarometerModel tModel = new GTBoilerBarometerModel(new GTBoilerBarometerModelTest.StubFallback());
		assertTrue(tModel.supportsDynamicQuads(ModelData.builder().with(GTModelProperties.BAROMETER, 7).build()));
		// the paint-only / cover-only snapshots do NOT route to the boiler model
		assertFalse(tModel.supportsDynamicQuads(ModelData.EMPTY));
		assertFalse(tModel.supportsDynamicQuads(ModelData.builder()
				.with(GTModelProperties.PAINT, 0xFF404040).build()), "the PAINT key alone is not the gauge");
	}

	@Test
	void gaugeQuadsRideTheCutoutPassOnTheFrontOnly() {
		GTBoilerBarometerModel tModel = new GTBoilerBarometerModel(new GTBoilerBarometerModelTest.StubFallback(),
				aSpriteId -> FaceBakePins.IdentitySprite.INSTANCE);
		ModelData tData = ModelData.builder().with(GTModelProperties.BAROMETER, 7).build();
		BlockState tFurnace = Blocks.FURNACE.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
		assertEquals(2, tModel.getQuads(tFurnace, Direction.NORTH, RandomSource.create(), tData, RenderType.cutout()).size(),
				"dial + needle on the cutout pass (the fallback boilerModel is cutout-declared)");
		assertEquals(2, tModel.getQuads(tFurnace, Direction.NORTH, RandomSource.create(), tData, null).size(),
				"the null all-layers pass");
		assertTrue(tModel.getQuads(tFurnace, Direction.NORTH, RandomSource.create(), tData, RenderType.solid()).isEmpty(),
				"the solid pass draws nothing (no ChunkRenderTypeSet extension needed)");
		assertTrue(tModel.getQuads(tFurnace, Direction.EAST, RandomSource.create(), tData, RenderType.cutout()).isEmpty(),
				"the non-front culling passes see none");
	}

	/**
	 * THE bake pin (the r8-uvof form): both gauge quads bake the canonical full-face UV
	 * walk (the upright dial/needle art, the #27 ruling), tintIndex stays -1, and the
	 * needle carries the CA_RED_64 {64,0,0,255} modulation in the vertex colours — the
	 * ABGR colour slot reads R=64, G=0, B=0, A=255 per vertex.
	 */
	@Test
	void gaugeQuadsBakeCanonicalUvAndTheNeedleRetintsRed() {
		GTBoilerBarometerModel tModel = new GTBoilerBarometerModel(new GTBoilerBarometerModelTest.StubFallback(),
				aSpriteId -> FaceBakePins.IdentitySprite.INSTANCE);
		ModelData tData = ModelData.builder().with(GTModelProperties.BAROMETER, 15).build();
		BlockState tFurnace = Blocks.FURNACE.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
		List<BakedQuad> tQuads = tModel.getQuads(tFurnace, Direction.NORTH, RandomSource.create(), tData, null);
		assertEquals(2, tQuads.size());
		BakedQuad tDial = tQuads.get(0), tNeedle = tQuads.get(1);
		FaceBakePins.assertCanonicalFullFaceUv(tDial, Direction.NORTH);
		FaceBakePins.assertCanonicalFullFaceUv(tNeedle, Direction.NORTH);
		assertEquals(-1, tDial.getTintIndex(), "the dial stays untinted by the BlockColors chain");
		assertEquals(-1, tNeedle.getTintIndex(), "the needle red is baked, not a tint index");
		assertRedNeedleVertices(tNeedle);
		// the dial vertices stay full-white in RGB (bakeQuad default, no retint)
		for (int v = 0; v < 4; v++) {
			int tColor = tDial.getVertices()[v * 8 + 3];
			assertEquals(255, tColor & 0xFF, "dial R " + v);
			assertEquals(255, (tColor >> 8) & 0xFF, "dial G " + v);
			assertEquals(255, (tColor >> 16) & 0xFF, "dial B " + v);
		}
	}

	/** The needle colour slot (ABGR): per-channel (255 * tint + 255) >> 8 of {64,0,0,255}. */
	private static void assertRedNeedleVertices(BakedQuad aNeedle) {
		for (int v = 0; v < 4; v++) {
			int tColor = aNeedle.getVertices()[v * 8 + 3];
			assertEquals(64, tColor & 0xFF, "vertex " + v + " R = the CA_RED_64 red");
			assertEquals(0, (tColor >> 8) & 0xFF, "vertex " + v + " G");
			assertEquals(0, (tColor >> 16) & 0xFF, "vertex " + v + " B");
			assertEquals(255, (tColor >>> 24), "vertex " + v + " A");
		}
	}

	// ---------------------------------------------------------------------------
	// the body tint (the review fix) — the fallback retints, the gauge plates don't
	// ---------------------------------------------------------------------------

	/** A baked-format body quad (32 ints, white vertex colours, tintIndex 0). */
	private static BakedQuad bodyQuad() {
		int[] tVertices = new int[4 * 8];
		java.util.Arrays.fill(tVertices, 0xFFFFFFFF);
		return new BakedQuad(tVertices, 0, Direction.NORTH, FaceBakePins.IdentitySprite.INSTANCE, true);
	}

	/** A baked-format fallback decal quad (untinted, the P22 overlay form). */
	private static BakedQuad decalQuad() {
		int[] tVertices = new int[4 * 8];
		java.util.Arrays.fill(tVertices, 0xFF333333);
		return new BakedQuad(tVertices, -1, Direction.NORTH, FaceBakePins.IdentitySprite.INSTANCE, true);
	}

	/** The barest fallback: returns exactly the quads the tint arms hand it. */
	private static net.minecraft.client.resources.model.BakedModel quadsFallback(List<BakedQuad> aQuads) {
		return new net.minecraft.client.resources.model.BakedModel() {
			@Override public List<BakedQuad> getQuads(net.minecraft.world.level.block.state.BlockState aState,
					Direction aSide, RandomSource aRand) { return aQuads; }
			@Override public boolean useAmbientOcclusion() { return false; }
			@Override public boolean isGui3d() { return false; }
			@Override public boolean usesBlockLight() { return false; }
			@Override public boolean isCustomRenderer() { return false; }
			@Override public net.minecraft.client.renderer.texture.TextureAtlasSprite getParticleIcon() { return null; }
			@Override public net.minecraft.client.renderer.block.model.ItemTransforms getTransforms() { return net.minecraft.client.renderer.block.model.ItemTransforms.NO_TRANSFORMS; }
			@Override public net.minecraft.client.renderer.block.model.ItemOverrides getOverrides() { return net.minecraft.client.renderer.block.model.ItemOverrides.EMPTY; }
		};
	}

	/** The offline boiler tank fixture — the Lead row (no registry, the oven fixture form). */
	private static gregtech6.registry.GT6Boilers.BoilerTankBlock leadTankBlock() {
		return new gregtech6.registry.GT6Boilers.BoilerTankBlock(
				gregtech6.registry.GT6Boilers.BOILER_ROWS.get(0),
				net.minecraft.world.level.block.state.BlockBehaviour.Properties.of());
	}

	/**
	 * THE white-body regression pin: the GTMachineTintModel wrap skips the boiler ladder
	 * (the dynamic model occupies the per-state seat first), so the body tint MUST ride
	 * the model itself — the cutout pass AND the raw non-cutout branch both retint the
	 * tintindex-0 body with the {@link GTMachinePaintTint} row colour, the decal passes
	 * through as the shared instance, and the gauge plates ride tintIndex -1.
	 */
	@Test
	void bodyPassRetintsWithTheRowMaterial() {
		gregtech6.registry.GT6Boilers.BoilerTankBlock tBlock = leadTankBlock();
		BakedQuad tBody = bodyQuad(), tDecal = decalQuad();
		GTBoilerBarometerModel tModel = new GTBoilerBarometerModel(quadsFallback(List.of(tBody, tDecal)),
				aSpriteId -> FaceBakePins.IdentitySprite.INSTANCE);
		ModelData tData = ModelData.builder().with(GTModelProperties.BAROMETER, 0).build();
		List<BakedQuad> tOut = tModel.getQuads(tBlock.defaultBlockState(), Direction.NORTH, RandomSource.create(),
				tData, RenderType.cutout());
		assertEquals(4, tOut.size(), "body + decal + dial + needle");
		int tTint = GTMachinePaintTint.tintARGB(ModelData.EMPTY, GTMachinePaintTint.tintMaterialOf(tBlock), 0);
		assertNotEquals(0xFFFFFFFF, tTint, "the Lead row material actually colours (not the white identity)");
		assertArrayEquals(GTMachineTintModel.retintVertices(tBody.getVertices(), tTint),
				tOut.get(0).getVertices(), "the body quad is the tintQuads product of the seam colour");
		assertEquals(-1, tOut.get(0).getTintIndex(), "the retinted copy rides tintIndex -1 (no second multiply)");
		assertSame(tDecal, tOut.get(1), "the fallback decal passes through as the shared instance (P22)");
		assertEquals(-1, tOut.get(2).getTintIndex(), "the dial stays untinted");
		assertEquals(-1, tOut.get(3).getTintIndex(), "the needle red is baked, not a tint index");
		// the raw non-cutout branch carries the same tint (wrap-semantics parity)
		List<BakedQuad> tSolid = tModel.getQuads(tBlock.defaultBlockState(), Direction.NORTH, RandomSource.create(),
				tData, RenderType.solid());
		assertEquals(2, tSolid.size());
		assertArrayEquals(GTMachineTintModel.retintVertices(tBody.getVertices(), tTint),
				tSolid.get(0).getVertices(), "the solid-branch fallback retints identically");
	}

	/** PAINT wins over the row material (the spray-paint snapshot co-hosted on the BE ModelData). */
	@Test
	void paintedSnapshotWinsOverTheRowMaterial() {
		gregtech6.registry.GT6Boilers.BoilerTankBlock tBlock = leadTankBlock();
		GTBoilerBarometerModel tModel = new GTBoilerBarometerModel(quadsFallback(List.of(bodyQuad())),
				aSpriteId -> FaceBakePins.IdentitySprite.INSTANCE);
		ModelData tPainted = ModelData.builder()
				.with(GTModelProperties.BAROMETER, 7)
				.with(GTModelProperties.PAINT, 0x00FF00)
				.build();
		List<BakedQuad> tOut = tModel.getQuads(tBlock.defaultBlockState(), Direction.NORTH, RandomSource.create(),
				tPainted, RenderType.cutout());
		assertEquals(3, tOut.size(), "body + dial + needle");
		assertArrayEquals(GTMachineTintModel.retintVertices(bodyQuad().getVertices(), 0xFF00FF00),
				tOut.get(0).getVertices(), "the spray-paint colour wins (upstream Paintable:85)");
	}

	// ---------------------------------------------------------------------------
	// the BE gauge supplies (the NBT_VISUAL byte → ModelData contract)
	// ---------------------------------------------------------------------------

	@Test
	@SuppressWarnings("unchecked")
	void boilerTankBESuppliesTheGaugeSnapshot() {
		// the offline fixture BET (the GTBoilerTankBlockEntityTest holder form — a null type
		// would dereference the runtime registry)
		BlockEntityType<GTBoilerTankBlockEntity>[] tHolder = (BlockEntityType<GTBoilerTankBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTBoilerTankBlockEntity(tHolder[0], aPos, aState), Blocks.STONE).build(null);
		GTBoilerTankBlockEntity tBoiler = new GTBoilerTankBlockEntity(tHolder[0], BlockPos.ZERO, Blocks.STONE.defaultBlockState());
		ModelData tData = tBoiler.getModelData();
		assertTrue(tData.has(GTModelProperties.BAROMETER), "the gauge is ALWAYS present (gauge 0 = a real reading)");
		assertEquals(0, tData.get(GTModelProperties.BAROMETER).intValue(), "the empty gauge");
		tBoiler.mBarometer = 17;
		assertEquals(17, tBoiler.getModelData().get(GTModelProperties.BAROMETER).intValue(), "the fresh reading");
	}

	@Test
	@SuppressWarnings("unchecked")
	void largeBoilerBESuppliesTheGaugeSnapshot() {
		BlockEntityType<TileEntityLargeBoiler>[] tHolder = (BlockEntityType<TileEntityLargeBoiler>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new TileEntityLargeBoiler(tHolder[0], aPos, aState), Blocks.STONE).build(null);
		TileEntityLargeBoiler tBoiler = new TileEntityLargeBoiler(tHolder[0], BlockPos.ZERO, Blocks.STONE.defaultBlockState());
		tBoiler.mBarometer = 31;
		ModelData tData = tBoiler.getModelData();
		assertTrue(tData.has(GTModelProperties.BAROMETER), "the large boiler gauge rides the same property");
		assertEquals(31, tData.get(GTModelProperties.BAROMETER).intValue(), "the full gauge");
	}

	// ---------------------------------------------------------------------------
	// the 144 per-state registrations
	// ---------------------------------------------------------------------------

	@Test
	void listenerRegistersTheBoilerPerStateKeys() {
		List<ModelResourceLocation> tTargets = GTBoilerClientListener.targetModelIds();
		assertEquals(144, tTargets.size(), "26 tank rows x 4 facing + 5 large rows x 4 facing x 2 formed");
		assertEquals(144, new HashSet<>(tTargets).size(), "all keys distinct");
		// the tank rows carry the FACING-only variant, the large rows the name-sorted
		// facing,formed pair (the row path literal: allRows().get(0) = the Lead row,
		// GT6Boilers.java:171 — the stonecutter MRL swap needs literal arguments)
		assertTrue(tTargets.contains(new ModelResourceLocation("gt6", "steam_boiler_tank_lead", "facing=north")),
				"the Lead tank row's facing-only key");
		assertTrue(tTargets.contains(new ModelResourceLocation("gt6", "large_boiler_stainless_steel", "facing=east,formed=true")),
				"the large row's facing,formed key");
		assertFalse(tTargets.contains(new ModelResourceLocation("gt6", "large_boiler_stainless_steel", "formed=true,facing=east")),
				"the misordered variant string is NOT a key (StateDefinition sorts by name)");
		for (ModelResourceLocation tTarget : tTargets) {
			//? if forge {
			assertEquals("gt6", tTarget.getNamespace());
			//?} else {
			/*assertEquals("gt6", tTarget.id().getNamespace()); // 21.1: MRL is a record over an RL id
			*///?}
		}
	}

	@Test
	void registrationWrapsTheBakedPerStateModels() {
		int tBefore = GTRenderModelListener.registeredCount();
		GTBoilerClientListener.register();
		assertEquals(tBefore + 144, GTRenderModelListener.registeredCount(), "all 144 per-state keys registered");

		String tTankPath = gregtech6.registry.GT6Boilers.allRows().get(0).path();
		assertEquals("steam_boiler_tank_lead", tTankPath, "the census head (the literal pin above stays honest)");
		ModelResourceLocation tKey = new ModelResourceLocation("gt6", "steam_boiler_tank_lead", "facing=south");
		StubFallback tBaked = new StubFallback();
		//? if forge {
		java.util.Map<ResourceLocation, net.minecraft.client.resources.model.BakedModel> tModels = new java.util.HashMap<>();
		//?} else {
		/*java.util.Map<ModelResourceLocation, net.minecraft.client.resources.model.BakedModel> tModels = new java.util.HashMap<>(); // 21.1: the baking table keys on the MRL record
		*///?}
		tModels.put(tKey, tBaked);
		//? if forge {
		GTRenderModelListener.onModifyBakingResult(new ModelEvent.ModifyBakingResult(tModels, null));
		//?} else {
		/*GTRenderModelListener.onModifyBakingResult(new ModelEvent.ModifyBakingResult(tModels, null, null)); // 21.1: +ModelBakery
		*///?}

		assertTrue(tModels.get(tKey) instanceof GTBoilerBarometerModel, "the per-state baked model is replaced by the gauge model");
		assertSame(tBaked, ((GTBoilerBarometerModel) tModels.get(tKey)).getFallbackModel(),
				"the baked model stays as the fallback (the static two-layer material layer)");
	}

	/** Minimal BakedModel stub (the GTOvenOverlayModelTest probe shape). */
	public static final class StubFallback implements net.minecraft.client.resources.model.BakedModel {
		@Override public List<BakedQuad> getQuads(BlockState aState, Direction aSide, RandomSource aRand) { return List.of(); }
		@Override public boolean useAmbientOcclusion() { return false; }
		@Override public boolean isGui3d() { return false; }
		@Override public boolean usesBlockLight() { return false; }
		@Override public boolean isCustomRenderer() { return false; }
		@Override public net.minecraft.client.renderer.texture.TextureAtlasSprite getParticleIcon() { return null; }
		@Override public net.minecraft.client.renderer.block.model.ItemTransforms getTransforms() { return net.minecraft.client.renderer.block.model.ItemTransforms.NO_TRANSFORMS; }
		@Override public net.minecraft.client.renderer.block.model.ItemOverrides getOverrides() { return net.minecraft.client.renderer.block.model.ItemOverrides.EMPTY; }
	}
}
