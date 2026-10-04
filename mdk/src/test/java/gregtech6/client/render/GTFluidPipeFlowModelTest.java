package gregtech6.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraftforge.client.model.data.ModelData;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * The pipe flow-arrow render-path sentinel tests (task pipe-flow-control acceptance
 * ① render group, the CoverPlateModelTest shape): the planner is pure geometry over the
 * immutable snapshot — the per-face emission rules (null pass + the face's own pass,
 * never culled), the 0.002 Z-fighting slab geometry, the snapshot clamp, and the client
 * registration hook. The sprite→BakedQuad baker is pinned offline too since
 * uvof-private-copies (the #27 GTOreBakedModelSideUvTest form): the arrow is
 * directional art, so the corrected canonical UV walk is asserted per vertex.
 */
public class GTFluidPipeFlowModelTest extends GTOfflineRenderTestBase {

	@AfterEach
	void clearRegistration() {
		GTRenderModelListener.clearForTest();
	}

	@Test
	void snapshotClampsToSixBitsAndResolvesFaces() {
		assertEquals(63, new PipeFlowSnapshot((byte)127).outputMask(), "bit0-5 clamp, the mConnections form");
		assertEquals(0, new PipeFlowSnapshot((byte)0).outputMask());
		PipeFlowSnapshot tSnapshot = new PipeFlowSnapshot((byte)(TileEntityBase09ConnectorMask.SBIT[2] | TileEntityBase09ConnectorMask.SBIT[5]));
		assertTrue(tSnapshot.hasArrow(Direction.NORTH), "bit 2 == NORTH (GT6 side order == get3DDataValue)");
		assertTrue(tSnapshot.hasArrow(Direction.EAST), "bit 5 == EAST");
		assertFalse(tSnapshot.hasArrow(Direction.UP), "bit 1 unset");
		assertFalse(tSnapshot.hasArrow(Direction.DOWN), "bit 0 unset");
	}

	@Test
	void plannerEmitsOnlyOnTheNullAndOwnFacePasses() {
		PipeFlowSnapshot tSnapshot = new PipeFlowSnapshot((byte)(1 << Direction.UP.get3DDataValue() | 1 << Direction.SOUTH.get3DDataValue()));

		// the unculled pass sees every marked face
		List<GTFluidPipeFlowModel.FlowQuad> tNullPass = GTFluidPipeFlowModel.planQuads(tSnapshot, null);
		assertEquals(2, tNullPass.size());
		assertEquals(Direction.UP, tNullPass.get(0).quadFace());
		assertEquals(Direction.SOUTH, tNullPass.get(1).quadFace());

		// the face's own pass sees exactly its arrow
		assertEquals(1, GTFluidPipeFlowModel.planQuads(tSnapshot, Direction.UP).size());
		assertEquals(1, GTFluidPipeFlowModel.planQuads(tSnapshot, Direction.SOUTH).size());

		// other passes see nothing (tangent passes never see the face plane)
		assertTrue(GTFluidPipeFlowModel.planQuads(tSnapshot, Direction.DOWN).isEmpty(), "unmarked face pass is empty");
		assertTrue(GTFluidPipeFlowModel.planQuads(tSnapshot, Direction.WEST).isEmpty(), "tangent pass is empty");

		// an empty mask plans nothing at all
		assertTrue(GTFluidPipeFlowModel.planQuads(new PipeFlowSnapshot((byte)0), null).isEmpty());
	}

	@Test
	void plannerSpriteIsTheSingleArrowPlaceholder() {
		List<GTFluidPipeFlowModel.FlowQuad> tPlans = GTFluidPipeFlowModel.planQuads(new PipeFlowSnapshot((byte)63), null);
		assertEquals(6, tPlans.size(), "all six faces marked → six arrow quads");
		for (GTFluidPipeFlowModel.FlowQuad tPlan : tPlans) {
			assertEquals(GTFluidPipeFlowModel.ARROW_SPRITE, tPlan.sprite());
			assertEquals(new ResourceLocation("gt6", "block/pipe_flow_arrow"), tPlan.sprite());
		}
	}

	@Test
	void slabGeometryCarriesTheZfightingEpsilon() {
		double e = GTFluidPipeFlowModel.ARROW_EPSILON, t = GTFluidPipeFlowModel.ARROW_THICKNESS;
		// UP: the slab hangs from the top face, outer plane pokes past 1.0
		double[] tUp = GTFluidPipeFlowModel.slabOf(Direction.UP);
		assertEquals(1 + e, tUp[4], 1e-9, "maxY = 1 + epsilon (COVER_OVERLAY form)");
		assertEquals(1 - t, tUp[1], 1e-9, "minY = the inner plane, 1px inside");
		// DOWN: mirrored — outer plane pokes past 0.0
		double[] tDown = GTFluidPipeFlowModel.slabOf(Direction.DOWN);
		assertEquals(-e, tDown[1], 1e-9, "minY = -epsilon");
		assertEquals(t, tDown[4], 1e-9, "maxY = the inner plane");
		// EAST: the outer plane at 1 + epsilon on X
		double[] tEast = GTFluidPipeFlowModel.slabOf(Direction.EAST);
		assertEquals(1 + e, tEast[3], 1e-9);
		assertEquals(1 - t, tEast[0], 1e-9);
	}

	/**
	 * THE pipe-flow-arrow-render-fix killer-1 pin: a bake-table key is the per-state
	 * ModelResourceLocation {@code gt6:<path>#connections=N} (GTRenderModelListener class
	 * doc — model-file ids are NEVER keys and mismatched registrations are silently
	 * skipped), so a live registration id must be the per-state form. The historical
	 * regression: the {@code "block/<path>"} model-file ids were registered, every one
	 * skipped silently, and the arrow chain never installed. The old count-only pin
	 * ({@code registeredCount}) was exactly the blind spot that let it through.
	 */
	@Test
	void registeredFlowTargetsAreAllRealPerStateBakedKeys() {
		for (String tTarget : GTPipeFlowClientListener.TARGET_MODELS) {
			assertTrue(tTarget.contains("#connections="),
					tTarget + ": a model-file id never matches a per-state bake key");
		}
	}

	/** A body model that records the Forge 5-arg dispatch (the ModelData forward pin). */
	private static final class RecordingBody implements net.minecraft.client.resources.model.BakedModel {
		static final List<BakedQuad> BODY_QUADS = List.of();
		ModelData mSeenVia5Arg;
		@Override public List<BakedQuad> getQuads(net.minecraft.world.level.block.state.BlockState aState,
				Direction aSide, RandomSource aRand) { return BODY_QUADS; }
		@Override public List<BakedQuad> getQuads(net.minecraft.world.level.block.state.BlockState aState,
				Direction aSide, RandomSource aRand, ModelData aData, net.minecraft.client.renderer.RenderType aRenderType) {
			mSeenVia5Arg = aData;
			return BODY_QUADS;
		}
		@Override public boolean useAmbientOcclusion() { return false; }
		@Override public boolean isGui3d() { return false; }
		@Override public boolean usesBlockLight() { return true; }
		@Override public boolean isCustomRenderer() { return false; }
		@Override public net.minecraft.client.renderer.texture.TextureAtlasSprite getParticleIcon() { return null; }
		@Override public net.minecraft.client.renderer.block.model.ItemTransforms getTransforms() {
			return net.minecraft.client.renderer.block.model.ItemTransforms.NO_TRANSFORMS; }
		@Override public net.minecraft.client.renderer.block.model.ItemOverrides getOverrides() {
			return net.minecraft.client.renderer.block.model.ItemOverrides.EMPTY; }
	}

	/** The composed chain over a recording body — the live GTRodClientListener shape. */
	private static GTFluidPipeFoamModel chainOver(RecordingBody aBody) {
		java.util.function.Function<ResourceLocation, net.minecraft.client.renderer.texture.TextureAtlasSprite> tLookup =
				aSpriteId -> FaceBakePins.IdentitySprite.INSTANCE;
		return new GTFluidPipeFoamModel(new GTFluidPipeFlowModel(aBody, tLookup), tLookup);
	}

	private static ModelData dataWithFlowAndPaint() {
		return GTModelProperties.snapshot()
				.with(GTModelProperties.FLOW_SNAPSHOT, new PipeFlowSnapshot((byte) 1))
				.with(GTModelProperties.PAINT, 0x00FF00).build();
	}

	/**
	 * THE killer-2/5-arg pin: the composed chain must hand the BE's ModelData down to the
	 * rod body — the body's paint tint resolves inside tintARGB off the PAINT property, so
	 * a 3-arg body call (ModelData dropped) sprays an arrowed pipe back to its material
	 * colour (the 掉漆 bug).
	 */
	@Test
	void chainForwardsTheSnapshotToTheBodyModel() {
		RecordingBody tBody = new RecordingBody();
		GTFluidPipeFoamModel tChain = chainOver(tBody);
		ModelData tData = dataWithFlowAndPaint();
		tChain.getQuads(null, Direction.UP, RandomSource.create(), tData, null);
		assertSame(tData, tBody.mSeenVia5Arg, "the body must see the BE ModelData (5-arg forward, paint kept)");
	}

	/** The acceptance pin: one FLOW dispatch serves the arrow quads BESIDE the pipe body quads. */
	@Test
	void flowDispatchServesArrowsBesideTheBodyQuads() {
		GTFluidPipeFoamModel tChain = chainOver(new RecordingBody());
		List<BakedQuad> tQuads = tChain.getQuads(null, null, RandomSource.create(), dataWithFlowAndPaint(), null);
		assertTrue(tQuads.containsAll(RecordingBody.BODY_QUADS), "the body quads ride along");
		assertTrue(tQuads.size() > RecordingBody.BODY_QUADS.size(), "the arrows are appended");
	}

	/**
	 * The spray-keep pin: a PAINTED plain pipe carries PAINT-only ModelData — the chain
	 * must still dispatch down to the body (both gates) or the paint drops to the material
	 * colour on the very first frame.
	 */
	@Test
	void paintOnlyModelDataStillDispatchesTheChain() {
		ModelData tPaintOnly = GTModelProperties.snapshot().with(GTModelProperties.PAINT, 0x00FF00).build();
		GTFluidPipeFoamModel tFoam = chainOver(new RecordingBody());
		assertTrue(tFoam.supportsDynamicQuads(tPaintOnly), "the outer gate must open for paint-only data");
		GTFluidPipeFlowModel tFlow = new GTFluidPipeFlowModel(new RecordingBody(), aSpriteId -> FaceBakePins.IdentitySprite.INSTANCE);
		assertTrue(tFlow.supportsDynamicQuads(tPaintOnly), "the inner gate must pass paint-only data through to the body");
	}

	/** A foamed pipe paints the body too (applyFoam) — the foam-only forward must keep the data. */
	@Test
	void foamOnlyPipeKeepsItsPaintOnTheBodyForward() {
		RecordingBody tBody = new RecordingBody();
		GTFluidPipeFoamModel tChain = chainOver(tBody);
		ModelData tData = GTModelProperties.snapshot()
				.with(GTModelProperties.FOAM_SNAPSHOT, new PipeFoamSnapshot(false, false))
				.with(GTModelProperties.PAINT, 0x00FF00).build();
		tChain.getQuads(null, Direction.UP, RandomSource.create(), tData, null);
		assertSame(tData, tBody.mSeenVia5Arg, "foam-only: the body still sees the ModelData (fresh overlay pass)");
	}

	/**
	 * The layer-gate pin: the pipe rows declare {@code render_type: cutout} (the r8-tex
	 * shared models), so the chunk bake runs ONLY the cutout pass — arrows and foam gated
	 * to the solid layer are never baked at all.
	 */
	@Test
	void cutoutPassCarriesTheArrowsAndTheFoam() {
		GTFluidPipeFoamModel tChain = chainOver(new RecordingBody());
		List<BakedQuad> tArrows = tChain.getQuads(null, Direction.UP, RandomSource.create(),
				dataWithFlowAndPaint(), net.minecraft.client.renderer.RenderType.cutout());
		assertEquals(1, tArrows.size() - RecordingBody.BODY_QUADS.size(), "one arrow on the UP pass, cutout layer");
		List<BakedQuad> tFoam = tChain.getQuads(null, Direction.UP, RandomSource.create(),
				GTModelProperties.snapshot().with(GTModelProperties.FOAM_SNAPSHOT, new PipeFoamSnapshot(false, false)).build(),
				net.minecraft.client.renderer.RenderType.cutout());
		assertTrue(tFoam.size() > RecordingBody.BODY_QUADS.size(), "the fresh foam overlay rides the cutout layer");
	}

	/**
	 * THE uvof-private-copies PIN: the arrow bakes the canonical full-face UV walk on
	 * all six faces — the sprite top (V=0, the arrow head) on the side faces' top corners
	 * (upright, the orientation the art was drawn for). The old GTCEu cubeUV table hung
	 * the arrow upside down on every side (plus a U mirror on NORTH/EAST).
	 */
	@Test
	void arrowQuadsBakeTheCanonicalFullFaceUvWalk() {
		GTFluidPipeFlowModel tModel = new GTFluidPipeFlowModel(new GTDynamicBakedModelTest.StubFallback(),
				aSpriteId -> FaceBakePins.IdentitySprite.INSTANCE);
		ModelData tData = GTModelProperties.snapshot()
				.with(GTModelProperties.FLOW_SNAPSHOT, new PipeFlowSnapshot((byte) 63)).build();
		for (Direction tFace : Direction.values()) {
			List<BakedQuad> tQuads = tModel.getQuads(null, tFace, RandomSource.create(), tData, null);
			assertEquals(1, tQuads.size(), tFace + ": the face's own pass carries exactly its arrow");
			FaceBakePins.assertCanonicalFullFaceUv(tQuads.get(0), tFace);
		}
	}

	@Test
	void dispatchKeysOnTheFlowSnapshotProperty() {
		// the p35 split: the flow model keys on the dedicated FLOW_SNAPSHOT — the cover
		// chain's RENDER_SNAPSHOT no longer dispatches it (and no longer gets evicted by it)
		GTFluidPipeFlowModel tModel = new GTFluidPipeFlowModel(new GTDynamicBakedModelTest.StubFallback(), aSpriteId -> null);
		ModelData tFlowOnly = GTModelProperties.snapshot()
				.with(GTModelProperties.FLOW_SNAPSHOT, new PipeFlowSnapshot((byte) 1)).build();
		ModelData tCoverOnly = GTModelProperties.snapshot()
				.with(GTModelProperties.RENDER_SNAPSHOT, new gregtech6.covers.GTCoverRenderSnapshot(java.util.Map.of())).build();
		assertTrue(tModel.supportsDynamicQuads(tFlowOnly), "the flow snapshot alone dispatches the flow model");
		assertFalse(tModel.supportsDynamicQuads(tCoverOnly), "a covers-only ModelData does not dispatch the flow model");
		assertFalse(tModel.supportsDynamicQuads(ModelData.EMPTY), "a plain pipe falls back");
	}

	@Test
	void foamWrapperDispatchesEveryInnerSnapshotFamily() {
		// the foam model is the outer chain wrapper — its gate must admit all three keys:
		// an arrow-only pipe (FLOW), a covered+foamed pipe (RENDER+FOAM, the pre-split shape
		// that RENDER alone satisfied) and a foam-only pipe (FOAM)
		GTFluidPipeFoamModel tFoam = new GTFluidPipeFoamModel(new GTDynamicBakedModelTest.StubFallback(), aSpriteId -> null);
		ModelData tFlowOnly = GTModelProperties.snapshot()
				.with(GTModelProperties.FLOW_SNAPSHOT, new PipeFlowSnapshot((byte) 1)).build();
		assertTrue(tFoam.supportsDynamicQuads(tFlowOnly), "an arrow-only pipe still dispatches the composed chain");
		ModelData tCoverFoam = GTModelProperties.snapshot()
				.with(GTModelProperties.RENDER_SNAPSHOT, new gregtech6.covers.GTCoverRenderSnapshot(java.util.Map.of()))
				.with(GTModelProperties.FOAM_SNAPSHOT, new PipeFoamSnapshot(false, false)).build();
		assertTrue(tFoam.supportsDynamicQuads(tCoverFoam), "a covered+foamed pipe dispatches the composed chain");
		ModelData tFoamOnly = GTModelProperties.snapshot()
				.with(GTModelProperties.FOAM_SNAPSHOT, new PipeFoamSnapshot(false, false)).build();
		assertTrue(tFoam.supportsDynamicQuads(tFoamOnly), "a foam-only pipe dispatches the composed chain");
		assertFalse(tFoam.supportsDynamicQuads(ModelData.EMPTY), "a plain pipe falls back");
	}

	/** The SBIT values (kept literal here so the test does not need the connector BE boot). */
	private static final class TileEntityBase09ConnectorMask {
		static final byte[] SBIT = {1, 2, 4, 8, 16, 32};
	}
}
