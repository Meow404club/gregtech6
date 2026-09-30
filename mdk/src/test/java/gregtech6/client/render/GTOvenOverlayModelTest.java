package gregtech6.client.render;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import gregtech6.block.GTOvenBlock;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.data.ModelData;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import gregtech6.client.render.GTOvenRenderSnapshot.OvenOverlayGroup;
import gregtech6.client.render.GTOvenOverlayModel.OvenTextureFace;
import gregtech6.client.render.GTOvenOverlayModel.OverlayPlan;
import gregtech6.covers.GTCoverRenderSnapshot;

/**
 * The oven overlay render-path sentinel tests (task render-c-oven-overlay, the
 * GTFluidPipeFlowModelTest shape): the offline quad-level half of the acceptance — the
 * upstream :1014 overlay-pick truth table (four states, mActive winning over mRunning),
 * the CS.java:528-537 FACING_ROTATIONS face table verbatim, the per-face emission and
 * sprite-id rules, the OVEN_SNAPSHOT dispatch gate (the second ModelProperty, the cover
 * chain's key untouched), and the 16 per-state ModelResourceLocation registrations. The
 * sprite→BakedQuad baker is pinned offline too since uvof-private-copies (the #27
 * GTOreBakedModelSideUvTest form): the overlay PNGs are upright art (the running front's
 * glow window sits in the sprite's bottom half), so the corrected canonical UV walk is
 * asserted per vertex. Since oven-texture-borrow the BODY pass carries the machine
 * paint/material tint (the {@link GTMachineTintModel#tintQuads} product over the
 * {@link GTMachinePaintTint} colour line — the oven ladder is skipped by the
 * GTMachineTintModel wrap, so the tint rides here) and the baked decals ride tintIndex
 * -1 (the P22 uncoloured-decal contract, the vanilla JSON default).
 */
public class GTOvenOverlayModelTest extends GTOfflineRenderTestBase {

	private static final int STRIDE = 8;
	private static final int COLOR_SLOT = 3;

	@org.junit.jupiter.api.BeforeAll
	static void bootMaterials() throws Exception {
		gregtech6.registry.GTMaterialItems.initMaterials();
		// the BLOCK registry write window (the GT6SingleBlockFacingIntegrityTest recipe):
		// the Block ctor registers its intrusive holder and NamespacedWrapper.validateWrite
		// rejects a frozen registry — unfreeze before the offline GTOvenBlock fixtures
		java.lang.reflect.Method tUnfreeze = net.minecraft.core.registries.BuiltInRegistries.BLOCK
				.getClass().getMethod("unfreeze");
		tUnfreeze.setAccessible(true);
		tUnfreeze.invoke(net.minecraft.core.registries.BuiltInRegistries.BLOCK);
	}

	@AfterEach
	void clearRegistration() {
		GTRenderModelListener.clearForTest();
	}

	// ---------------------------------------------------------------------------
	// the upstream :1014 pick — four-state truth table (mActive wins over mRunning)
	// ---------------------------------------------------------------------------

	@Test
	void overlayPickTruthTableMirrorsUpstream1014() {
		// (mActive, mRunning) → group; the quirk: active&&running → ACTIVE (upstream
		// mActive||worldObj==null ? Active : mRunning ? Running : Inactive, :1014)
		assertEquals(OvenOverlayGroup.NONE, new GTOvenRenderSnapshot(false, false).overlayGroup(), "inactive → Inactive (no overlay quad)");
		assertEquals(OvenOverlayGroup.ACTIVE, new GTOvenRenderSnapshot(true, false).overlayGroup(), "active → Active");
		assertEquals(OvenOverlayGroup.RUNNING, new GTOvenRenderSnapshot(false, true).overlayGroup(), "running → Running");
		assertEquals(OvenOverlayGroup.ACTIVE, new GTOvenRenderSnapshot(true, true).overlayGroup(), "the :1014 quirk: active wins over running");
	}

	@Test
	void textureKeysAreLowercasePathTokens() {
		assertEquals("none", OvenOverlayGroup.NONE.textureKey());
		assertEquals("active", OvenOverlayGroup.ACTIVE.textureKey());
		assertEquals("running", OvenOverlayGroup.RUNNING.textureKey());
		for (OvenTextureFace tFace : OvenTextureFace.values()) {
			assertEquals(tFace.name().toLowerCase(java.util.Locale.ROOT), tFace.textureKey());
		}
	}

	// ---------------------------------------------------------------------------
	// the CS.java:528-537 FACING_ROTATIONS table — full 6x6 verbatim
	// ---------------------------------------------------------------------------

	@Test
	void facingRotationsTableIsUpstreamVerbatim() {
		// [facing][side] → texture face; 0=bottom, 1=top, 2=left, 3=front, 4=right, 5=back
		OvenTextureFace[][] tUpstream = {
				{OvenTextureFace.BOTTOM, OvenTextureFace.TOP, OvenTextureFace.LEFT, OvenTextureFace.FRONT, OvenTextureFace.RIGHT, OvenTextureFace.BACK}, // row 0 (DOWN)
				{OvenTextureFace.BOTTOM, OvenTextureFace.TOP, OvenTextureFace.LEFT, OvenTextureFace.FRONT, OvenTextureFace.RIGHT, OvenTextureFace.BACK}, // row 1 (UP)
				{OvenTextureFace.BOTTOM, OvenTextureFace.TOP, OvenTextureFace.FRONT, OvenTextureFace.BACK, OvenTextureFace.RIGHT, OvenTextureFace.LEFT}, // row 2 (NORTH)
				{OvenTextureFace.BOTTOM, OvenTextureFace.TOP, OvenTextureFace.BACK, OvenTextureFace.FRONT, OvenTextureFace.LEFT, OvenTextureFace.RIGHT}, // row 3 (SOUTH)
				{OvenTextureFace.BOTTOM, OvenTextureFace.TOP, OvenTextureFace.LEFT, OvenTextureFace.RIGHT, OvenTextureFace.FRONT, OvenTextureFace.BACK}, // row 4 (WEST)
				{OvenTextureFace.BOTTOM, OvenTextureFace.TOP, OvenTextureFace.RIGHT, OvenTextureFace.LEFT, OvenTextureFace.BACK, OvenTextureFace.FRONT}, // row 5 (EAST)
		};
		int tAssertions = 0;
		for (Direction tFacing : Direction.values()) {
			for (Direction tSide : Direction.values()) {
				assertEquals(tUpstream[tFacing.get3DDataValue()][tSide.get3DDataValue()],
						GTOvenOverlayModel.textureFaceOf(tFacing, tSide),
						"CS.java:528-537 FACING_ROTATIONS[" + tFacing + "][" + tSide + "]");
				tAssertions++;
			}
		}
		assertEquals(36, tAssertions, "the full table is pinned");
		// the horizontal-facing front reads: the front texture sits on the facing side itself
		assertEquals(OvenTextureFace.FRONT, GTOvenOverlayModel.textureFaceOf(Direction.NORTH, Direction.NORTH));
		assertEquals(OvenTextureFace.FRONT, GTOvenOverlayModel.textureFaceOf(Direction.EAST, Direction.EAST));
		assertEquals(OvenTextureFace.FRONT, GTOvenOverlayModel.textureFaceOf(Direction.SOUTH, Direction.SOUTH));
		assertEquals(OvenTextureFace.FRONT, GTOvenOverlayModel.textureFaceOf(Direction.WEST, Direction.WEST));
	}

	// ---------------------------------------------------------------------------
	// the planner — emission rules, sprite ids, slab geometry
	// ---------------------------------------------------------------------------

	@Test
	void inactiveGroupPlansNothing() {
		GTOvenRenderSnapshot tInactive = new GTOvenRenderSnapshot(false, false);
		for (Direction tSide : Direction.values()) {
			assertTrue(GTOvenOverlayModel.planOverlayQuads(tInactive, Direction.NORTH, tSide).isEmpty(),
					"the inactive state adds no overlay quad (A-tier fallback carries the face): " + tSide);
		}
		assertTrue(GTOvenOverlayModel.planOverlayQuads(tInactive, Direction.NORTH, null).isEmpty());
	}

	@Test
	void nullPassSeesNoOverlayQuads() {
		// cube-faithful emission: the quads carry a cullface, so the unculled pass sees none
		assertTrue(GTOvenOverlayModel.planOverlayQuads(new GTOvenRenderSnapshot(true, false), Direction.NORTH, null).isEmpty());
		assertTrue(GTOvenOverlayModel.planOverlayQuads(new GTOvenRenderSnapshot(false, true), Direction.EAST, null).isEmpty());
		assertTrue(GTOvenOverlayModel.planOverlayQuads(new GTOvenRenderSnapshot(true, true), Direction.SOUTH, null).isEmpty());
	}

	@Test
	void activeAndRunningPlansCoverEveryFaceOnce() {
		for (GTOvenRenderSnapshot tSnapshot : List.of(new GTOvenRenderSnapshot(true, false), new GTOvenRenderSnapshot(false, true))) {
			Set<OvenTextureFace> tFaces = new HashSet<>();
			for (Direction tSide : Direction.values()) {
				List<OverlayPlan> tPlans = GTOvenOverlayModel.planOverlayQuads(tSnapshot, Direction.NORTH, tSide);
				assertEquals(1, tPlans.size(), "exactly one overlay quad per face pass: " + tSide);
				assertEquals(tSide, tPlans.get(0).quadFace(), "the quad sits on its own culling face");
				tFaces.add(tPlans.get(0).textureFace());
			}
			assertEquals(Set.of(OvenTextureFace.values()), tFaces, "all six upstream texture faces are drawn");
		}
	}

	@Test
	void plansCarryTheQuirkAndTheGroupSprites() {
		// (true, true) plans the ACTIVE sprites — the :1014 quirk at the planner level
		List<OverlayPlan> tQuirk = GTOvenOverlayModel.planOverlayQuads(new GTOvenRenderSnapshot(true, true), Direction.NORTH, Direction.NORTH);
		assertEquals(1, tQuirk.size());
		assertEquals(GTOvenOverlayModel.spriteOf(OvenOverlayGroup.ACTIVE, OvenTextureFace.FRONT), tQuirk.get(0).sprite());
		assertEquals(new ResourceLocation("gt6", "block/oven_overlay_active_front"), tQuirk.get(0).sprite());

		// every sprite id has the gt6:block/oven_overlay_<group>_<face> shape
		for (OvenOverlayGroup tGroup : List.of(OvenOverlayGroup.ACTIVE, OvenOverlayGroup.RUNNING)) {
			for (Direction tSide : Direction.values()) {
				OverlayPlan tPlan = GTOvenOverlayModel.planOverlayQuads(new GTOvenRenderSnapshot(tGroup == OvenOverlayGroup.ACTIVE, tGroup == OvenOverlayGroup.RUNNING),
						Direction.NORTH, tSide).get(0);
				//? if forge {
				assertEquals(new ResourceLocation("gt6", "block/oven_overlay_" + tGroup.textureKey() + "_" + tPlan.textureFace().textureKey()),
				//?} else {
				/*assertEquals(ResourceLocation.fromNamespaceAndPath("gt6", "block/oven_overlay_" + tGroup.textureKey() + "_" + tPlan.textureFace().textureKey()),
				*///?}
						tPlan.sprite(), "the sprite id mirrors the upstream NBT_TEXTURE name form");
			}
		}
	}

	@Test
	void slabGeometryCarriesTheZfightingEpsilon() {
		double e = GTOvenOverlayModel.OVERLAY_EPSILON, t = GTOvenOverlayModel.OVERLAY_THICKNESS;
		double[] tUp = GTOvenOverlayModel.slabOf(Direction.UP);
		assertEquals(1 + e, tUp[4], 1e-9, "maxY = 1 + epsilon (COVER_OVERLAY form)");
		assertEquals(1 - t, tUp[1], 1e-9, "minY = the inner plane, 1px inside");
		double[] tDown = GTOvenOverlayModel.slabOf(Direction.DOWN);
		assertEquals(-e, tDown[1], 1e-9, "minY = -epsilon");
		double[] tEast = GTOvenOverlayModel.slabOf(Direction.EAST);
		assertEquals(1 + e, tEast[3], 1e-9, "maxX = 1 + epsilon");
		assertEquals(1 - t, tEast[0], 1e-9, "minX = the inner plane");
	}

	/**
	 * THE uvof-private-copies PIN: the overlay bakes the canonical full-face UV walk on
	 * all six faces — the sprite top (V=0) on the side faces' top corners (upright; the
	 * running front's glow window keeps its bottom-of-the-door position). The old GTCEu
	 * cubeUV table baked every side face upside down (plus a U mirror on NORTH/EAST).
	 */
	@Test
	void overlayQuadsBakeTheCanonicalFullFaceUvWalk() {
		GTOvenOverlayModel tModel = new GTOvenOverlayModel(new StubFallback(),
				aSpriteId -> FaceBakePins.IdentitySprite.INSTANCE);
		ModelData tData = ModelData.builder()
				.with(GTModelProperties.OVEN_SNAPSHOT, new GTOvenRenderSnapshot(true, false)).build();
		for (Direction tFace : Direction.values()) {
			List<BakedQuad> tQuads = tModel.getQuads(null, tFace, RandomSource.create(), tData, null);
			assertEquals(1, tQuads.size(), tFace + ": the overlay face rides its own culling pass");
			FaceBakePins.assertCanonicalFullFaceUv(tQuads.get(0), tFace);
		}
	}

	// ---------------------------------------------------------------------------
	// the body tint (oven-texture-borrow) — the solid pass retints, the decals don't
	// ---------------------------------------------------------------------------

	/** A baked-format body quad (32 ints, white vertex colours, tintIndex 0). */
	private static BakedQuad bodyQuad() {
		int[] tVertices = new int[4 * STRIDE];
		java.util.Arrays.fill(tVertices, 0xFFFFFFFF);
		return new BakedQuad(tVertices, 0, Direction.NORTH, FaceBakePins.IdentitySprite.INSTANCE, true);
	}

	/** A baked-format decal quad (untinted, the P22 overlay form). */
	private static BakedQuad decalQuad() {
		int[] tVertices = new int[4 * STRIDE];
		java.util.Arrays.fill(tVertices, 0xFF333333);
		return new BakedQuad(tVertices, -1, Direction.NORTH, FaceBakePins.IdentitySprite.INSTANCE, true);
	}

	/** The barest fallback: returns exactly the quads the tint arms hand it. */
	private static net.minecraft.client.resources.model.BakedModel quadsFallback(List<BakedQuad> aQuads) {
		return new net.minecraft.client.resources.model.BakedModel() {
			@Override public List<BakedQuad> getQuads(@org.jetbrains.annotations.Nullable net.minecraft.world.level.block.state.BlockState aState,
					@org.jetbrains.annotations.Nullable Direction aSide, net.minecraft.util.RandomSource aRand) { return aQuads; }
			@Override public boolean useAmbientOcclusion() { return false; }
			@Override public boolean isGui3d() { return false; }
			@Override public boolean usesBlockLight() { return false; }
			@Override public boolean isCustomRenderer() { return false; }
			@Override public net.minecraft.client.renderer.texture.TextureAtlasSprite getParticleIcon() { return null; }
			@Override public net.minecraft.client.renderer.block.model.ItemTransforms getTransforms() { return net.minecraft.client.renderer.block.model.ItemTransforms.NO_TRANSFORMS; }
			@Override public net.minecraft.client.renderer.block.model.ItemOverrides getOverrides() { return net.minecraft.client.renderer.block.model.ItemOverrides.EMPTY; }
		};
	}

	/** The offline oven block fixture (the GT6SingleBlockFacingIntegrityTest :365 shape). */
	private static GTOvenBlock ovenBlock() {
		return new GTOvenBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of());
	}

	private static ModelData ovenData(boolean aActive, boolean aRunning) {
		return ModelData.builder().with(GTModelProperties.OVEN_SNAPSHOT, new GTOvenRenderSnapshot(aActive, aRunning)).build();
	}

	@Test
	void solidPassRetintsTheBodyWithTheRowMaterial() {
		GTOvenBlock tBlock = ovenBlock();
		BakedQuad tBody = bodyQuad(), tDecal = decalQuad();
		GTOvenOverlayModel tModel = new GTOvenOverlayModel(quadsFallback(List.of(tBody, tDecal)),
				aSpriteId -> FaceBakePins.IdentitySprite.INSTANCE);
		List<BakedQuad> tOut = tModel.getQuads(tBlock.defaultBlockState(), Direction.NORTH, RandomSource.create(),
				ovenData(false, false), null); // the inactive oven: the unpainted body IS the field complaint
		assertEquals(2, tOut.size(), "body + decal, the inactive state plans no overlay");
		int tTint = GTMachinePaintTint.tintARGB(ModelData.EMPTY,
				GTMachinePaintTint.tintMaterialOf(tBlock), 0);
		assertNotEquals(0xFFFFFFFF, tTint, "the Heat_T row material actually colours (not the white identity)");
		assertArrayEquals(GTMachineTintModel.retintVertices(tBody.getVertices(), tTint),
				tOut.get(0).getVertices(), "the body quad is the tintQuads product of the seam colour");
		assertEquals(-1, tOut.get(0).getTintIndex(), "the retinted copy rides tintIndex -1 (no second multiply)");
		assertSame(tDecal, tOut.get(1), "the decal passes through as the shared instance (P22)");
	}

	@Test
	void paintedSnapshotWinsOverTheRowMaterial() {
		GTOvenBlock tBlock = ovenBlock();
		GTOvenOverlayModel tModel = new GTOvenOverlayModel(quadsFallback(List.of(bodyQuad())),
				aSpriteId -> FaceBakePins.IdentitySprite.INSTANCE);
		ModelData tPainted = ModelData.builder()
				.with(GTModelProperties.OVEN_SNAPSHOT, new GTOvenRenderSnapshot(false, false))
				.with(GTModelProperties.PAINT, 0x00FF00)
				.build();
		List<BakedQuad> tOut = tModel.getQuads(tBlock.defaultBlockState(), Direction.NORTH, RandomSource.create(), tPainted, null);
		assertEquals(1, tOut.size());
		assertArrayEquals(GTMachineTintModel.retintVertices(bodyQuad().getVertices(), 0xFF00FF00),
				tOut.get(0).getVertices(), "the spray-paint colour wins (upstream Paintable:85)");
	}

	@Test
	void dynamicOverlayQuadsRideNoTintIndex() {
		// the P22 alignment pin: the baked decals carry tintIndex -1 (the vanilla JSON
		// default), so no tint route can ever multiply the door art
		GTOvenOverlayModel tModel = new GTOvenOverlayModel(new StubFallback(),
				aSpriteId -> FaceBakePins.IdentitySprite.INSTANCE);
		for (Direction tFace : Direction.values()) {
			List<BakedQuad> tQuads = tModel.getQuads(null, tFace, RandomSource.create(), ovenData(true, false), null);
			assertEquals(1, tQuads.size());
			assertEquals(-1, tQuads.get(0).getTintIndex(), tFace + ": the state decal is UNCOLOURED (upstream :179-180)");
		}
	}

	// ---------------------------------------------------------------------------
	// the dispatch gate — the second ModelProperty, the cover key untouched
	// ---------------------------------------------------------------------------

	@Test
	void dispatchGatesOnTheOvenSnapshotOnly() {
		GTOvenOverlayModel tModel = new GTOvenOverlayModel(new StubFallback());
		// the oven snapshot → dynamic dispatch
		assertTrue(tModel.supportsDynamicQuads(ModelData.builder().with(GTModelProperties.OVEN_SNAPSHOT, new GTOvenRenderSnapshot(true, false)).build()));
		// a cover-only snapshot (the p4 chain's RENDER_SNAPSHOT) does NOT route to the oven model…
		assertFalse(tModel.supportsDynamicQuads(ModelData.builder().with(GTModelProperties.RENDER_SNAPSHOT, GTCoverTestProbe.coverSnapshot()).build()),
				"the cover chain's key does not dispatch the oven overlay model");
		// …and an empty ModelData falls back
		assertFalse(tModel.supportsDynamicQuads(ModelData.EMPTY));
	}

	// ---------------------------------------------------------------------------
	// the 64 per-state registrations (4 ladder rows x 16, task oven-heat-t-ladder)
	// ---------------------------------------------------------------------------

	@Test
	void listenerRegistersTheLadderPerStateKeys() {
		List<ModelResourceLocation> tTargets = GTOvenClientListener.targetModelIds();
		assertEquals(64, tTargets.size(), "4 ladder rows x 2 active x 4 facing x 2 running");
		assertEquals(64, new HashSet<>(tTargets).size(), "all keys distinct");
		// variant string = the StateDefinition name-sorted property order (active, facing, running)
		assertTrue(tTargets.contains(new ModelResourceLocation("gt6", "oven", "active=false,facing=north,running=false")));
		assertTrue(tTargets.contains(new ModelResourceLocation("gt6", "oven", "active=true,facing=east,running=true")));
		assertFalse(tTargets.contains(new ModelResourceLocation("gt6", "oven", "facing=north,active=false,running=false")),
				"the misordered variant string is NOT a key (StateDefinition sorts by name)");
		for (ModelResourceLocation tTarget : tTargets) {
			//? if forge {
			assertEquals("gt6", tTarget.getNamespace());
			assertTrue(GTOvenClientListener.OVEN_BLOCK_PATHS.contains(tTarget.getPath()),
					tTarget.getPath() + ": every ladder row path is a key");
			//?} else {
			/*assertEquals("gt6", tTarget.id().getNamespace()); // 21.1: MRL is a record over an RL id
			assertTrue(GTOvenClientListener.OVEN_BLOCK_PATHS.contains(tTarget.id().getPath()),
					tTarget.id().getPath() + ": every ladder row path is a key");
			*///?}
		}
	}

	@Test
	void registrationWrapsTheBakedPerStateModels() {
		int tBefore = GTRenderModelListener.registeredCount();
		GTOvenClientListener.register();
		assertEquals(tBefore + 64, GTRenderModelListener.registeredCount(), "all 64 per-state keys registered");

		ModelResourceLocation tKey = new ModelResourceLocation("gt6", "oven", "active=true,facing=north,running=false");
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

		assertTrue(tModels.get(tKey) instanceof GTOvenOverlayModel, "the per-state baked model is replaced by the overlay model");
		assertSame(tBaked, ((GTOvenOverlayModel) tModels.get(tKey)).getFallbackModel(), "the baked model stays as the fallback (A-tier material layer)");
	}

	/** Minimal BakedModel stub (the GTRenderModelListenerTest probe shape). */
	public static final class StubFallback implements net.minecraft.client.resources.model.BakedModel {
		@Override public List<net.minecraft.client.renderer.block.model.BakedQuad> getQuads(net.minecraft.world.level.block.state.BlockState aState, Direction aSide, net.minecraft.util.RandomSource aRand) { return List.of(); }
		@Override public boolean useAmbientOcclusion() { return false; }
		@Override public boolean isGui3d() { return false; }
		@Override public boolean usesBlockLight() { return false; }
		@Override public boolean isCustomRenderer() { return false; }
		@Override public net.minecraft.client.renderer.texture.TextureAtlasSprite getParticleIcon() { return null; }
		@Override public net.minecraft.client.renderer.block.model.ItemTransforms getTransforms() { return net.minecraft.client.renderer.block.model.ItemTransforms.NO_TRANSFORMS; }
		@Override public net.minecraft.client.renderer.block.model.ItemOverrides getOverrides() { return net.minecraft.client.renderer.block.model.ItemOverrides.EMPTY; }
	}

	/** Cover-snapshot fixture without touching the covers test package state. */
	private static final class GTCoverTestProbe {
		static GTCoverRenderSnapshot coverSnapshot() {
			return new GTCoverRenderSnapshot(java.util.Map.of(Direction.DOWN, new ResourceLocation("gt6", "block/cover/test_plate")));
		}
	}
}
