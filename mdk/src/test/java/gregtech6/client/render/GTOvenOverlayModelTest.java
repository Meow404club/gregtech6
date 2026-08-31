package gregtech6.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.model.data.ModelData;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import gregtech6.client.render.GTOvenRenderSnapshot.OvenOverlayGroup;
import gregtech6.client.render.GTOvenOverlayModel.OvenTextureFace;
import gregtech6.client.render.GTOvenOverlayModel.OverlayPlan;
import gregtech6.covers.GTCoverRenderSnapshot;

/**
 * The oven overlay render-path sentinel tests (task p9-render-c-oven-overlay, the
 * GTFluidPipeFlowModelTest shape): the offline quad-level half of the acceptance — the
 * upstream :1014 overlay-pick truth table (four states, mActive winning over mRunning),
 * the CS.java:528-537 FACING_ROTATIONS face table verbatim, the per-face emission and
 * sprite-id rules, the OVEN_SNAPSHOT dispatch gate (the second ModelProperty, the cover
 * chain's key untouched), and the 16 per-state ModelResourceLocation registrations. The
 * sprite→BakedQuad baker itself is the runClient visual check left to the user.
 */
public class GTOvenOverlayModelTest extends GTOfflineRenderTestBase {

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
				assertEquals(new ResourceLocation("gt6", "block/oven_overlay_" + tGroup.textureKey() + "_" + tPlan.textureFace().textureKey()),
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
	// the 16 per-state registrations
	// ---------------------------------------------------------------------------

	@Test
	void listenerRegistersTheSixteenPerStateKeys() {
		List<ModelResourceLocation> tTargets = GTOvenClientListener.targetModelIds();
		assertEquals(16, tTargets.size(), "2 active x 4 facing x 2 running");
		assertEquals(16, new HashSet<>(tTargets).size(), "all keys distinct");
		// variant string = the StateDefinition name-sorted property order (active, facing, running)
		assertTrue(tTargets.contains(new ModelResourceLocation("gt6", "oven", "active=false,facing=north,running=false")));
		assertTrue(tTargets.contains(new ModelResourceLocation("gt6", "oven", "active=true,facing=east,running=true")));
		assertFalse(tTargets.contains(new ModelResourceLocation("gt6", "oven", "facing=north,active=false,running=false")),
				"the misordered variant string is NOT a key (StateDefinition sorts by name)");
		for (ModelResourceLocation tTarget : tTargets) {
			assertEquals("gt6", tTarget.getNamespace());
			assertEquals("oven", tTarget.getPath());
		}
	}

	@Test
	void registrationWrapsTheBakedPerStateModels() {
		int tBefore = GTRenderModelListener.registeredCount();
		GTOvenClientListener.register();
		assertEquals(tBefore + 16, GTRenderModelListener.registeredCount(), "all 16 per-state keys registered");

		ModelResourceLocation tKey = new ModelResourceLocation("gt6", "oven", "active=true,facing=north,running=false");
		StubFallback tBaked = new StubFallback();
		java.util.Map<ResourceLocation, net.minecraft.client.resources.model.BakedModel> tModels = new java.util.HashMap<>();
		tModels.put(tKey, tBaked);
		GTRenderModelListener.onModifyBakingResult(new ModelEvent.ModifyBakingResult(tModels, null));

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
