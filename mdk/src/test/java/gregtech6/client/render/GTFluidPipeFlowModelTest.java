package gregtech6.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.data.ModelData;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * The pipe flow-arrow render-path sentinel tests (task p4-pipe-flow-control acceptance
 * ① render group, the CoverPlateModelTest shape): the planner is pure geometry over the
 * immutable snapshot — the per-face emission rules (null pass + the face's own pass,
 * never culled), the 0.002 Z-fighting slab geometry, the snapshot clamp, and the client
 * registration hook. The sprite→BakedQuad baker itself is the runClient visual check
 * left to the user (the W2 BakedQuad-offline precedent).
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

	@Test
	void clientListenerRegistersBothTierModels() {
		int tBefore = GTRenderModelListener.registeredCount();
		GTPipeFlowClientListener.register();
		assertEquals(tBefore + GTPipeFlowClientListener.TARGET_MODELS.size(), GTRenderModelListener.registeredCount(),
				"both pipe tier blockstate models registered");
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
