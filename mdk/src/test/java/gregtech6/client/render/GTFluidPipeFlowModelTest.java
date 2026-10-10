package gregtech6.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraftforge.client.model.data.ModelData;

import org.junit.jupiter.api.Test;

import gregtech6.registry.GTFluidPipes;
import gregtech6.registry.GTItemPipes;

/**
 * The pipe flow-arrow render-path sentinel tests (task pipe-flow-control acceptance
 * ① render group, the CoverPlateModelTest shape): the planner is pure geometry over the
 * immutable snapshot — the per-face emission rules (null pass + the face's own pass,
 * never culled), the 0.002 Z-fighting slab geometry, the snapshot clamp — plus the
 * pipe-flow-arrow-render-fix pins: the composed chain lives on the per-state fluid-pipe
 * keys ({@code gt6:<path>#connections=N}, seated by GTRodClientListener), the ModelData
 * forward keeps the paint on the body, and the cutout pass carries arrows and foam.
 * The sprite→BakedQuad baker is pinned offline too since uvof-private-copies (the #27
 * GTOreBakedModelSideUvTest form): the arrow is directional art, so the corrected
 * canonical UV walk is asserted per vertex.
 */
public class GTFluidPipeFlowModelTest extends GTOfflineRenderTestBase {

	@Test
	void snapshotClampsToSixBitsAndResolvesFaces() {
		assertEquals(63, new PipeFlowSnapshot((byte)127, true).outputMask(), "bit0-5 clamp, the mConnections form");
		assertEquals(0, new PipeFlowSnapshot((byte)0, false).outputMask());
		assertFalse(new PipeFlowSnapshot((byte)7).rendersArms(), "the compat form never drives the arm override");
		PipeFlowSnapshot tSnapshot = new PipeFlowSnapshot((byte)(TileEntityBase09ConnectorMask.SBIT[2] | TileEntityBase09ConnectorMask.SBIT[5]), true);
		assertTrue(tSnapshot.hasArrow(Direction.NORTH), "bit 2 == NORTH (GT6 side order == get3DDataValue)");
		assertTrue(tSnapshot.hasArrow(Direction.EAST), "bit 5 == EAST");
		assertFalse(tSnapshot.hasArrow(Direction.UP), "bit 1 unset");
		assertFalse(tSnapshot.hasArrow(Direction.DOWN), "bit 0 unset");
	}

	/** The badge half-inset for the 8px test row (GTFluidPipeFlowModel.halfOf — the arm cross-section). */
	private static final float HALF = GTFluidPipeFlowModel.halfOf(8);

	@Test
	void plannerEmitsOnlyOnTheNullAndOwnFacePasses() {
		PipeFlowSnapshot tSnapshot = new PipeFlowSnapshot((byte)(1 << Direction.UP.get3DDataValue() | 1 << Direction.SOUTH.get3DDataValue()), true);

		// the unculled pass sees every marked face
		List<GTFluidPipeFlowModel.FlowQuad> tNullPass = GTFluidPipeFlowModel.planQuads(tSnapshot, null, HALF);
		assertEquals(2, tNullPass.size());
		assertEquals(Direction.UP, tNullPass.get(0).quadFace());
		assertEquals(Direction.SOUTH, tNullPass.get(1).quadFace());

		// the face's own pass sees exactly its arrow
		assertEquals(1, GTFluidPipeFlowModel.planQuads(tSnapshot, Direction.UP, HALF).size());
		assertEquals(1, GTFluidPipeFlowModel.planQuads(tSnapshot, Direction.SOUTH, HALF).size());

		// other passes see nothing (tangent passes never see the face plane)
		assertTrue(GTFluidPipeFlowModel.planQuads(tSnapshot, Direction.DOWN, HALF).isEmpty(), "unmarked face pass is empty");
		assertTrue(GTFluidPipeFlowModel.planQuads(tSnapshot, Direction.WEST, HALF).isEmpty(), "tangent pass is empty");

		// an empty mask plans nothing at all
		assertTrue(GTFluidPipeFlowModel.planQuads(new PipeFlowSnapshot((byte)0, true), null, HALF).isEmpty());
	}

	@Test
	void plannerSpriteIsTheSingleArrowPlaceholder() {
		List<GTFluidPipeFlowModel.FlowQuad> tPlans = GTFluidPipeFlowModel.planQuads(new PipeFlowSnapshot((byte)63, true), null, HALF);
		assertEquals(6, tPlans.size(), "all six faces marked → six arrow quads");
		for (GTFluidPipeFlowModel.FlowQuad tPlan : tPlans) {
			assertEquals(GTFluidPipeFlowModel.ARROW_SPRITE, tPlan.sprite());
			assertEquals(ResourceLocation.fromNamespaceAndPath("gt6", "block/pipe_flow_arrow"), tPlan.sprite());
		}
	}

	/**
	 * THE pipe-output-arrows geometry pin: the badge is the connected-arm cross-section
	 * (the seated row diameter) centred on the face, pushed fully OUTSIDE the boundary —
	 * the old full-face slab floated as a big icon over the thin pipe art (the user's
	 * "rotating icon covering the connection texture"). HALF = halfOf(8) → the tangent
	 * bounds [0.25, 0.75]; the outer plane pokes ARROW_EPSILON past the face and the inner
	 * plane sits ARROW_THICKNESS out (the whole badge off the end cap, z-fight-free).
	 */
	@Test
	void markerGeometryRidesTheArmCrossSectionOutsideTheBoundary() {
		double e = GTFluidPipeFlowModel.ARROW_EPSILON, t = GTFluidPipeFlowModel.ARROW_THICKNESS;
		// UP: tangent X/Z clamped to the arm, the slab floats above 1.0
		double[] tUp = GTFluidPipeFlowModel.markerOf(Direction.UP, HALF);
		assertEquals(HALF, tUp[0], 1e-9, "minX = the arm half-inset");
		assertEquals(1 - HALF, tUp[3], 1e-9, "maxX = 1 - the arm half-inset");
		assertEquals(HALF, tUp[2], 1e-9, "minZ = the arm half-inset");
		assertEquals(1 - HALF, tUp[5], 1e-9, "maxZ = 1 - the arm half-inset");
		assertEquals(1 - e - t, tUp[1], 1e-9, "minY = the inner plane, fully outside the block");
		assertEquals(1 + e, tUp[4], 1e-9, "maxY = the outer plane, epsilon past the face");
		// DOWN: mirrored
		double[] tDown = GTFluidPipeFlowModel.markerOf(Direction.DOWN, HALF);
		assertEquals(-e, tDown[1], 1e-9, "minY = -epsilon");
		assertEquals(-e + t, tDown[4], 1e-9, "maxY = the inner plane (1px out)");
		// EAST: the outer plane at 1 + epsilon on X, tangent Y/Z clamped to the arm
		double[] tEast = GTFluidPipeFlowModel.markerOf(Direction.EAST, HALF);
		assertEquals(1 - e - t, tEast[0], 1e-9);
		assertEquals(1 + e, tEast[3], 1e-9);
		assertEquals(HALF, tEast[1], 1e-9);
		assertEquals(1 - HALF, tEast[4], 1e-9);
		// the 2px clamp floor: halfOf(4px tiny rows) still leaves a sane box
		assertEquals((16 - 4) / 32.0, GTFluidPipeFlowModel.halfOf(4), 1e-9);
		assertEquals((16 - 2.0F) / 32.0F, GTFluidPipeFlowModel.halfOf(0), 1e-6, "the readFromNBT2 :64 clamp floor");
	}

	/**
	 * THE pipe-flow-arrow-render-fix killer-1 pin (green form): the flow chain lives on
	 * the per-state bake keys {@code gt6:<path>#connections=0..63} of the FLUID and ITEM
	 * pipe rows (the item rows joined in pipe-output-arrows — the item BE derives its
	 * output arrows onto the same FLOW_SNAPSHOT) — the form vanilla ModelBakery actually
	 * mints (GTRenderModelListener class doc). The historical regression registered the
	 * {@code "block/<path>"} model-file id form, which never matches any bake key and is
	 * skipped silently — the arrows never installed. The old count-only pin was exactly the
	 * blind spot that let it through.
	 */
	@Test
	void flowChainKeysAreExactlyThePerStateFluidPipeKeys() {
		for (String tPath : GTFluidPipes.BLOCKS_BY_PATH.keySet()) {
			for (int tMask = 0; tMask < 64; tMask++) {
				assertTrue(GTRodClientListener.isFlowChainKey("gt6:" + tPath + "#connections=" + tMask),
						tPath + "#connections=" + tMask + " must carry the composed chain");
			}
			assertFalse(GTRodClientListener.isFlowChainKey("gt6:block/" + tPath),
					"the killer-1 phantom form: a model-file id never matches a bake key");
			assertFalse(GTRodClientListener.isFlowChainKey("gt6:" + tPath),
					"the item key renders raw rod (no ModelData in the inventory form)");
			assertFalse(GTRodClientListener.isFlowChainKey("gt6:" + tPath + "#axis=y"),
					"axis is not a pipe variant");
		}
		for (String tPath : GTItemPipes.BLOCKS_BY_PATH.keySet()) {
			for (int tMask = 0; tMask < 64; tMask++) {
				assertTrue(GTRodClientListener.isFlowChainKey("gt6:" + tPath + "#connections=" + tMask),
						tPath + "#connections=" + tMask + " must carry the composed chain (pipe-output-arrows)");
			}
			assertFalse(GTRodClientListener.isFlowChainKey("gt6:" + tPath),
					"the item key renders raw rod (no ModelData in the inventory form)");
		}
		assertFalse(GTRodClientListener.isFlowChainKey("gt6:some_other_block#connections=5"),
				"non-pipe connection-state keys stay raw rod");
	}

	/**
	 * THE killer-2 pin (the composition): on a fluid per-state key the rod replacement
	 * seats Foam(Flow(Rod)) — the foam OUTER (its any-snapshot+PAINT gate), the flow
	 * middle, the SAME rod instance innermost; every other rod key keeps the raw rod. The
	 * old form had two listeners replace the same keys last-wins and the rod won, burying
	 * the whole chain. Pipe-output-arrows: the ITEM per-state keys seat the same chain (the
	 * one arrow system — an extra rod-side arm would double-render the fluid arrows).
	 */
	@Test
	void rodReplacementSeatsFoamOverFlowOverRodOnTheFlowKeys() {
		GTRodBakedModel tRod = new GTRodBakedModel(new GTDynamicBakedModelTest.StubFallback(),
				new GTRodBakedModel.Params(GTFluidPipeFlowModel.ARROW_SPRITE, List.of(), 8), null);
		String tFlowKey = "gt6:" + GTFluidPipes.ROWS.get(0).path() + "#connections=5";
		BakedModel tValue = GTRodClientListener.replacementFor(tFlowKey, tRod);
		assertTrue(tValue instanceof GTFluidPipeFoamModel,
				"foam is the OUTER chain layer (killer-2: the bare-rod last-wins eviction buried it)");
		BakedModel tMid = ((GTFluidPipeFoamModel) tValue).getFallbackModel();
		assertTrue(tMid instanceof GTFluidPipeFlowModel, "flow is the middle layer");
		assertSame(tRod, ((GTFluidPipeFlowModel) tMid).getFallbackModel(), "the rod body is the innermost layer");
		assertSame(tRod, GTRodClientListener.replacementFor("gt6:" + GTFluidPipes.ROWS.get(0).path(), tRod),
				"the item key stays raw rod");
		String tItemKey = "gt6:" + GTItemPipes.ROWS.get(0).path() + "#connections=5";
		BakedModel tItemValue = GTRodClientListener.replacementFor(tItemKey, tRod);
		assertTrue(tItemValue instanceof GTFluidPipeFoamModel,
				"the item per-state key seats the same composed chain (pipe-output-arrows)");
		assertSame(tRod, ((GTFluidPipeFlowModel) ((GTFluidPipeFoamModel) tItemValue).getFallbackModel()).getFallbackModel(),
				"the item chain wraps the SAME rod instance");
		// the badge diameter rides the seat — the flow model sizes its markers to the row's
		// connected-arm cross-section (8px here)
		assertEquals(GTFluidPipeFlowModel.halfOf(8),
				((GTFluidPipeFlowModel) ((GTFluidPipeFoamModel) tValue).getFallbackModel()).half(), 1e-9);
	}

	/**
	 * THE pipe-output-arrows direction pin: the mask the ITEM pipe BE derives
	 * ({@code outputArrowMask} — face 2 emit-disabled leaves the other five emitting)
	 * plans exactly five arrows, and the disabled face (NORTH, bit 2 of the GT6 side order)
	 * carries none; the unconstrained pipe (mask 0) plans nothing.
	 */
	@Test
	void itemPipeDerivedMaskPlansArrowsOnEveryStillEmittingFace() {
		byte tMask = (byte)(63 & ~(1 << Direction.NORTH.get3DDataValue())); // the outputArrowMask(emit-off NORTH) form
		List<GTFluidPipeFlowModel.FlowQuad> tPlans = GTFluidPipeFlowModel.planQuads(new PipeFlowSnapshot(tMask, false), null, HALF);
		assertEquals(5, tPlans.size(), "five still-emitting faces carry arrows");
		for (GTFluidPipeFlowModel.FlowQuad tPlan : tPlans) {
			assertNotEquals(Direction.NORTH, tPlan.quadFace(), "the emit-disabled face carries no arrow");
		}
		assertTrue(GTFluidPipeFlowModel.planQuads(new PipeFlowSnapshot((byte) 0, false), null, HALF).isEmpty(),
				"an unconstrained pipe (the GT6 default) stays silent");
	}

	/**
	 * THE pipe-output-arrows block B pin (the rod arm-form): a {@code rendersArms} snapshot
	 * ORs its mask into the rod geometry mask — a marked-but-unconnected face grows the
	 * connected-arm form the user ruled ("the specified face looks connected, the same as a
	 * plain-click connect"); a {@code rendersArms=false} snapshot (the item pipe's dense
	 * disable-complement) and a missing snapshot leave the pure state mask.
	 */
	@Test
	void effectiveMaskOrsOnlyTheArmsFacesSnapshots() {
		byte tNorth = (byte)(1 << Direction.NORTH.get3DDataValue());
		assertEquals(GTRodBakedModel.ITEM_MASK | tNorth, GTRodBakedModel.effectiveMask(null,
				GTModelProperties.snapshot().with(GTModelProperties.FLOW_SNAPSHOT, new PipeFlowSnapshot(tNorth, true)).build()),
				"the fluid mark (rendersArms) joins the geometry mask");
		assertEquals(GTRodBakedModel.ITEM_MASK, GTRodBakedModel.effectiveMask(null,
				GTModelProperties.snapshot().with(GTModelProperties.FLOW_SNAPSHOT, new PipeFlowSnapshot((byte) 63, false)).build()),
				"the dense item complement never spawns arms");
		assertEquals(GTRodBakedModel.ITEM_MASK, GTRodBakedModel.effectiveMask(null, ModelData.EMPTY),
				"no snapshot → the pure state mask");
	}

	/**
	 * The end-to-end B pin at the rod instance: with a {@code rendersArms} snapshot the
	 * baked quad set is the plan of the OR'd mask (the DOWN arm appears on the unconnected
	 * item-form state), not the plan of the bare state mask.
	 */
	@Test
	void rodInstanceBakesTheArmFormFromTheSnapshot() {
		GTRodBakedModel tRod = new GTRodBakedModel(new GTDynamicBakedModelTest.StubFallback(),
				new GTRodBakedModel.Params(GTFluidPipeFlowModel.ARROW_SPRITE, List.of(), 8), null,
				aSpriteId -> FaceBakePins.IdentitySprite.INSTANCE);
		byte tDown = (byte)(1 << Direction.DOWN.get3DDataValue());
		ModelData tArms = GTModelProperties.snapshot()
				.with(GTModelProperties.FLOW_SNAPSHOT, new PipeFlowSnapshot(tDown, true)).build();
		List<BakedQuad> tWithArm = tRod.getQuads(null, null, RandomSource.create(), tArms, null);
		List<BakedQuad> tPlain = tRod.getQuads(null, null, RandomSource.create(), ModelData.EMPTY, null);
		int tExpected = GTRodBakedModel.planShapes(8, GTRodBakedModel.ITEM_MASK | tDown, 0).size();
		assertEquals(tExpected, tWithArm.size(), "the OR'd mask plans the DOWN arm (the pipe mouth exists)");
		assertEquals(GTRodBakedModel.planShapes(8, GTRodBakedModel.ITEM_MASK, 0).size(), tPlain.size(),
				"no snapshot → the bare N-S segment");
		assertTrue(tWithArm.size() > tPlain.size(), "the arm-form adds quads");
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

	/** The composed chain over a recording body — the live GTRodClientListener shape (8px test row). */
	private static GTFluidPipeFoamModel chainOver(RecordingBody aBody) {
		java.util.function.Function<ResourceLocation, net.minecraft.client.renderer.texture.TextureAtlasSprite> tLookup =
				aSpriteId -> FaceBakePins.IdentitySprite.INSTANCE;
		return new GTFluidPipeFoamModel(new GTFluidPipeFlowModel(aBody, 8, tLookup), tLookup);
	}

	private static ModelData dataWithFlowAndPaint() {
		return GTModelProperties.snapshot()
				.with(GTModelProperties.FLOW_SNAPSHOT, new PipeFlowSnapshot((byte) 1, true))
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
		GTFluidPipeFlowModel tFlow = new GTFluidPipeFlowModel(new RecordingBody(), 8, aSpriteId -> FaceBakePins.IdentitySprite.INSTANCE);
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
		// the fixture mask 1 marks DOWN — query the DOWN pass
		List<BakedQuad> tArrows = tChain.getQuads(null, Direction.DOWN, RandomSource.create(),
				dataWithFlowAndPaint(), net.minecraft.client.renderer.RenderType.cutout());
		assertEquals(1, tArrows.size() - RecordingBody.BODY_QUADS.size(), "one arrow on the DOWN pass, cutout layer");
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
		GTFluidPipeFlowModel tModel = new GTFluidPipeFlowModel(new GTDynamicBakedModelTest.StubFallback(), 8,
				aSpriteId -> FaceBakePins.IdentitySprite.INSTANCE);
		ModelData tData = GTModelProperties.snapshot()
				.with(GTModelProperties.FLOW_SNAPSHOT, new PipeFlowSnapshot((byte) 63, true)).build();
		for (Direction tFace : Direction.values()) {
			List<BakedQuad> tQuads = tModel.getQuads(null, tFace, RandomSource.create(), tData, null);
			assertEquals(1, tQuads.size(), tFace + ": the face's own pass carries exactly its arrow");
			FaceBakePins.assertCanonicalFullFaceUv(tQuads.get(0), tFace);
		}
	}

	@Test
	void flowGatePassesEveryChainSnapshotThroughToTheBody() {
		// the pipe-flow-arrow-render-fix pass-through: the flow model sits INSIDE the
		// composed chain — a closed gate would drop the ModelData on the base-class 3-arg
		// miss (paint loss), so the gate admits everything the outer foam gate admits; the
		// arrows themselves still key on FLOW_SNAPSHOT only (getDynamicQuads)
		GTFluidPipeFlowModel tModel = new GTFluidPipeFlowModel(new GTDynamicBakedModelTest.StubFallback(), 8, aSpriteId -> null);
		assertTrue(tModel.supportsDynamicQuads(GTModelProperties.snapshot()
				.with(GTModelProperties.FLOW_SNAPSHOT, new PipeFlowSnapshot((byte) 1, true)).build()),
				"the flow snapshot alone dispatches the flow model");
		assertTrue(tModel.supportsDynamicQuads(GTModelProperties.snapshot()
				.with(GTModelProperties.RENDER_SNAPSHOT, new gregtech6.covers.GTCoverRenderSnapshot(java.util.Map.of())).build()),
				"a covered pipe passes through (the body data stays alive)");
		assertTrue(tModel.supportsDynamicQuads(GTModelProperties.snapshot()
				.with(GTModelProperties.PAINT, 0x00FF00).build()), "paint-only passes through (the spray is kept)");
		assertFalse(tModel.supportsDynamicQuads(ModelData.EMPTY), "a plain pipe falls through");
	}

	@Test
	void foamWrapperDispatchesEveryInnerSnapshotFamily() {
		// the foam model is the outer chain wrapper — its gate must admit all three keys:
		// an arrow-only pipe (FLOW), a covered+foamed pipe (RENDER+FOAM, the pre-split shape
		// that RENDER alone satisfied) and a foam-only pipe (FOAM)
		GTFluidPipeFoamModel tFoam = new GTFluidPipeFoamModel(new GTDynamicBakedModelTest.StubFallback(), aSpriteId -> null);
		ModelData tFlowOnly = GTModelProperties.snapshot()
				.with(GTModelProperties.FLOW_SNAPSHOT, new PipeFlowSnapshot((byte) 1, true)).build();
		assertTrue(tFoam.supportsDynamicQuads(tFlowOnly), "an arrow-only pipe still dispatches the composed chain");
		ModelData tCoverFoam = GTModelProperties.snapshot()
				.with(GTModelProperties.RENDER_SNAPSHOT, new gregtech6.covers.GTCoverRenderSnapshot(java.util.Map.of()))
				.with(GTModelProperties.FOAM_SNAPSHOT, new PipeFoamSnapshot(false, false)).build();
		assertTrue(tFoam.supportsDynamicQuads(tCoverFoam), "a covered+foamed pipe dispatches the composed chain");
		ModelData tFoamOnly = GTModelProperties.snapshot()
				.with(GTModelProperties.FOAM_SNAPSHOT, new PipeFoamSnapshot(false, false)).build();
		assertTrue(tFoam.supportsDynamicQuads(tFoamOnly), "a foam-only pipe dispatches the composed chain");
		assertTrue(tFoam.supportsDynamicQuads(GTModelProperties.snapshot()
				.with(GTModelProperties.PAINT, 0x00FF00).build()),
				"a painted plain pipe dispatches the chain (the paint must reach the body)");
		assertFalse(tFoam.supportsDynamicQuads(ModelData.EMPTY), "a plain pipe falls back");
	}

	/** The SBIT values (kept literal here so the test does not need the connector BE boot). */
	private static final class TileEntityBase09ConnectorMask {
		static final byte[] SBIT = {1, 2, 4, 8, 16, 32};
	}
}
