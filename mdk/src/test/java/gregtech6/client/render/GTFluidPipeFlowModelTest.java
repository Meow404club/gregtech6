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
 * ① render group, the CoverPlateModelTest shape), form reworked by
 * pipe-flow-indicator-rework: the planner is pure geometry over the immutable snapshot —
 * THE USER RULING under test: four arrow patches on the marked arm's own side faces,
 * each inside the arm's outer segment so a fully-connected neighbourhood can hide
 * nothing (the retired pipe-output-arrows end-cap slab floated OUTSIDE the block
 * boundary and sat on the marked face itself), the per-patch emission rules (null pass +
 * the patch face's own pass, never culled), and the per-(marked, patch) UV rotation
 * table pointing every sprite head at the marked face. Plus the pipe-flow-arrow-render-fix
 * pins: the composed chain lives on the per-state fluid-pipe keys
 * ({@code gt6:<path>#connections=N}, seated by GTRodClientListener), the ModelData
 * forward keeps the paint on the body, and the cutout pass carries arrows and foam.
 * The sprite→BakedQuad baker is pinned offline since uvof-private-copies (the #27
 * GTOreBakedModelSideUvTest form): directional art, so every baked patch's vertex UVs
 * and positions are asserted.
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

	/** The half-inset of the 8px test row (GTFluidPipeFlowModel.halfOf — the arm cross-section). */
	private static final float HALF = GTFluidPipeFlowModel.halfOf(8);

	/** The patch edge length of the 8px test row (min(h, 1-2h) = min(0.25, 0.5)). */
	private static final float SIZE = GTFluidPipeFlowModel.patchSize(HALF);

	/**
	 * The pass bucketing (pipe-flow-indicator-rework form): a patch rides its own patch
	 * face's pass plus the null pass — for marks {UP, SOUTH}: the UP arm paints N/S/W/E,
	 * the SOUTH arm paints U/D/W/E, so the side passes see 1/1/2/2 and the U/D passes one
	 * each (the SOUTH arm's up/down patches); tangent passes with no patch stay empty.
	 */
	@Test
	void plannerEmitsPatchesOnTheirOwnSidePasses() {
		PipeFlowSnapshot tSnapshot = new PipeFlowSnapshot((byte)(1 << Direction.UP.get3DDataValue() | 1 << Direction.SOUTH.get3DDataValue()), true);

		// the unculled pass sees every patch of every marked face
		assertEquals(8, GTFluidPipeFlowModel.planQuads(tSnapshot, null, HALF).size(), "two marked faces x four side patches");

		// the per-side pass buckets (1 = only one marked arm has that side, 2 = both do)
		assertEquals(1, GTFluidPipeFlowModel.planQuads(tSnapshot, Direction.UP, HALF).size(), "the SOUTH arm's up patch");
		assertEquals(1, GTFluidPipeFlowModel.planQuads(tSnapshot, Direction.DOWN, HALF).size(), "the SOUTH arm's down patch");
		assertEquals(1, GTFluidPipeFlowModel.planQuads(tSnapshot, Direction.NORTH, HALF).size(), "the UP arm's north patch");
		assertEquals(1, GTFluidPipeFlowModel.planQuads(tSnapshot, Direction.SOUTH, HALF).size(), "the UP arm's south patch");
		assertEquals(2, GTFluidPipeFlowModel.planQuads(tSnapshot, Direction.WEST, HALF).size(), "the west patches of both arms");
		assertEquals(2, GTFluidPipeFlowModel.planQuads(tSnapshot, Direction.EAST, HALF).size(), "the east patches of both arms");

		// an empty mask plans nothing at all
		assertTrue(GTFluidPipeFlowModel.planQuads(new PipeFlowSnapshot((byte)0, true), null, HALF).isEmpty());
	}

	@Test
	void plannerSpriteIsTheSingleArrowPlaceholder() {
		List<GTFluidPipeFlowModel.FlowQuad> tPlans = GTFluidPipeFlowModel.planQuads(new PipeFlowSnapshot((byte)63, true), null, HALF);
		assertEquals(24, tPlans.size(), "all six faces marked → six arms x four side patches");
		for (GTFluidPipeFlowModel.FlowQuad tPlan : tPlans) {
			assertEquals(GTFluidPipeFlowModel.ARROW_SPRITE, tPlan.sprite());
			assertEquals(ResourceLocation.fromNamespaceAndPath("gt6", "block/pipe_flow_arrow"), tPlan.sprite());
		}
	}

	/**
	 * The patch size table ({@code min(h, 1 - 2h)} — the outer-segment depth vs the arm
	 * face width) and the full-block consequence: d=16 rows have h=0 → an EMPTY outer
	 * segment → nothing satisfiable, nothing planned (the class doc derivation).
	 */
	@Test
	void patchSizeAdaptsToTheRowDiameterAndFullBlockRowsPlanNothing() {
		// halfOf clamps kept from pipe-output-arrows
		assertEquals((16 - 4) / 32.0, GTFluidPipeFlowModel.halfOf(4), 1e-9);
		assertEquals((16 - 2.0F) / 32.0F, GTFluidPipeFlowModel.halfOf(0), 1e-6, "the readFromNBT2 :64 clamp floor");
		// sizes in px: bounded by the segment (h) and the face (1-2h), the 2px floor at d=12
		assertEquals(4.0 / 16, GTFluidPipeFlowModel.patchSize(GTFluidPipeFlowModel.halfOf(4)), 1e-9, "tiny: the face width binds (4px)");
		assertEquals(5.0 / 16, GTFluidPipeFlowModel.patchSize(GTFluidPipeFlowModel.halfOf(6)), 1e-9, "small: the segment binds (5px)");
		assertEquals(4.0 / 16, GTFluidPipeFlowModel.patchSize(GTFluidPipeFlowModel.halfOf(8)), 1e-9, "normal: the segment binds (4px)");
		assertEquals(2.0 / 16, GTFluidPipeFlowModel.patchSize(GTFluidPipeFlowModel.halfOf(12)), 1e-9, "large: the segment binds (2px — the coarse floor)");
		assertEquals(0, GTFluidPipeFlowModel.patchSize(GTFluidPipeFlowModel.halfOf(16)), 1e-9, "full block: no outer segment");
		assertTrue(GTFluidPipeFlowModel.planQuads(new PipeFlowSnapshot((byte)63, true), null, GTFluidPipeFlowModel.halfOf(16)).isEmpty(),
				"a fully marked full-block row still plans nothing (nothing can satisfy the visibility ruling)");
	}

	/**
	 * THE pipe-flow-indicator-rework position pin (the user ruling 「保证其余面全部连接时，
	 * 这 4 枚箭头仍全部可见」): against the WORST-case neighbourhood — every face marked or
	 * connected, i.e. every perpendicular arm at full cross-section [h, 1-h] — every patch
	 * of every real row diameter must (a) sit entirely in the F arm's OUTER SEGMENT along
	 * the F axis (F-span ∩ [h, 1-h] = ∅, flush to the block face, inside the boundary),
	 * (b) lie ON the arm's own side plane (± the epsilon) within the arm face width, and
	 * (c) be centred on the arm cross-section. armBox reference: GTRodBakedModel:453-461.
	 */
	@Test
	void everyPatchSitsInTheOuterSegmentOfEveryRealRowDiameter() {
		List<Integer> tDiameters = GTFluidPipes.ROWS.stream().map(tRow -> tRow.variant().diameterPx).distinct().sorted().toList();
		assertTrue(tDiameters.contains(4) && tDiameters.contains(12), "the real diameter spread is exercised: " + tDiameters);
		for (int tDiameter : tDiameters) {
			float tHalf = GTFluidPipeFlowModel.halfOf(tDiameter);
			List<GTFluidPipeFlowModel.FlowQuad> tPlans = GTFluidPipeFlowModel.planQuads(new PipeFlowSnapshot((byte)63, true), null, tHalf);
			assertEquals(tDiameter >= 16 ? 0 : 24, tPlans.size(), "d=" + tDiameter + ": full-block rows plan nothing, the rest 6x4");
			for (GTFluidPipeFlowModel.FlowQuad tPlan : tPlans) {
				for (Direction tMarked : Direction.values()) {
					if (tMarked.getAxis() == tPlan.quadFace().getAxis()) continue; // not this plan's marked face
					if (isPatchOf(tPlan, tMarked)) {
						assertPatchGeometry(tPlan, tMarked, tHalf, GTFluidPipeFlowModel.patchSize(tHalf), "d=" + tDiameter);
					}
				}
			}
		}
	}

	/** Whether the plan is one of the marked face's four side patches (its rotation table entry matches). */
	private static boolean isPatchOf(GTFluidPipeFlowModel.FlowQuad aPlan, Direction aMarked) {
		Direction tQuadFace = aPlan.quadFace();
		if (tQuadFace.getAxis() == aMarked.getAxis()) return false;
		// the patch's F-span hugs the block face on the marked axis — the arm-side tells
		// (the box layout is the interleaved [x0,y0,z0,x1,y1,z1])
		double[] tBox = aPlan.box();
		int tF = aMarked.getAxis().ordinal();
		boolean tFlushPositive = Math.abs(tBox[tF + 3] - 1) < 1e-9;
		boolean tFlushNegative = Math.abs(tBox[tF]) < 1e-9;
		return (aMarked.getAxisDirection() == Direction.AxisDirection.POSITIVE) == tFlushPositive && (tFlushPositive || tFlushNegative);
	}

	/** The per-patch geometry assertions (a): outer segment, (b) on the arm plane, (c) centred. */
	private static void assertPatchGeometry(GTFluidPipeFlowModel.FlowQuad aPlan, Direction aMarked, float aHalf, float aSize, String aLabel) {
		double[] tBox = aPlan.box();
		Direction tPatchFace = aPlan.quadFace();
		// (a) the F span: flush to the block face, entirely outside [h, 1-h]
		int tF = aMarked.getAxis().ordinal();
		if (aMarked.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
			assertEquals(1 - aSize, tBox[tF], 1e-9, aLabel + " " + aMarked + " patch F-min = the outer-segment end");
			assertEquals(1, tBox[tF + 3], 1e-9, aLabel + " " + aMarked + " patch F-max = the block face (inside the boundary)");
		} else {
			assertEquals(0, tBox[tF], 1e-9, aLabel + " " + aMarked + " patch F-min = the block face");
			assertEquals(aSize, tBox[tF + 3], 1e-9, aLabel + " " + aMarked + " patch F-max = the outer-segment end");
		}
		// (b) the patch plane sits one epsilon off the arm's own side face (the arm face is at h or 1-h)
		int tS = tPatchFace.getAxis().ordinal();
		boolean tPositive = tPatchFace.getAxisDirection() == Direction.AxisDirection.POSITIVE;
		assertEquals(tPositive ? 1 - aHalf + GTFluidPipeFlowModel.ARROW_EPSILON : aHalf - GTFluidPipeFlowModel.ARROW_EPSILON,
				tBox[tS + (tPositive ? 3 : 0)], 1e-9, aLabel + " " + aMarked + "/" + tPatchFace + " plane = the arm face ± epsilon");
		// (c) the third axis is centred on the arm cross-section and inside the arm face
		Direction.Axis tTAxis = Direction.Axis.X;
		for (Direction.Axis tAxis : Direction.Axis.values()) {
			if (tAxis != tPatchFace.getAxis() && tAxis != aMarked.getAxis()) tTAxis = tAxis;
		}
		int tT = tTAxis.ordinal();
		assertEquals(0.5 - aSize / 2, tBox[tT], 1e-9, aLabel + " " + aMarked + "/" + tPatchFace + " centred min");
		assertEquals(0.5 + aSize / 2, tBox[tT + 3], 1e-9, aLabel + " " + aMarked + "/" + tPatchFace + " centred max");
		assertTrue(tBox[tT] >= aHalf - 1e-9 && tBox[tT + 3] <= 1 - aHalf + 1e-9, aLabel + ": the patch lies within the arm face width");
	}

	/**
	 * THE direction pin: all 24 (marked F, patch face S) combinations bake ONE quad whose
	 * (a) UVs walk the rotation table's corner sequence (BlockFaceUV.java:40-42: rotation
	 * k maps vertex i to corner (i+k)%4 of (0,0),(0,16),(16,16),(16,0)) and (b) sprite-top
	 * edge centre minus sprite-bottom edge centre is exactly the marked face's normal x
	 * the patch size — the arrow HEAD points along the flow.
	 */
	@Test
	void everyRotationEntryBakesAPatchPointingAtTheMarkedFace() {
		GTFluidPipeFlowModel tModel = new GTFluidPipeFlowModel(new GTDynamicBakedModelTest.StubFallback(), 8,
				aSpriteId -> FaceBakePins.IdentitySprite.INSTANCE);
		for (Direction tMarked : Direction.values()) {
			ModelData tData = GTModelProperties.snapshot()
					.with(GTModelProperties.FLOW_SNAPSHOT, new PipeFlowSnapshot((byte)(1 << tMarked.get3DDataValue()), true)).build();
			for (Direction tPatchFace : Direction.values()) {
				if (tPatchFace.getAxis() == tMarked.getAxis()) continue;
				List<BakedQuad> tQuads = tModel.getQuads(null, tPatchFace, RandomSource.create(), tData, null);
				assertEquals(1, tQuads.size(), tMarked + "/" + tPatchFace + ": the patch pass carries exactly its patch");
				assertBakedPatch(tQuads.get(0), tMarked, tPatchFace);
			}
		}
	}

	/** The baked-quad form: FaceInfo vertex walk + rotation table + the F-pointing head. */
	private static void assertBakedPatch(BakedQuad aQuad, Direction aMarked, Direction aPatchFace) {
		int[] tV = aQuad.getVertices();
		float[] tX = new float[4], tY = new float[4], tZ = new float[4], tU = new float[4], tVv = new float[4];
		for (int i = 0; i < 4; i++) {
			tX[i] = Float.intBitsToFloat(tV[i * 8]);
			tY[i] = Float.intBitsToFloat(tV[i * 8 + 1]);
			tZ[i] = Float.intBitsToFloat(tV[i * 8 + 2]);
			tU[i] = Float.intBitsToFloat(tV[i * 8 + 4]);
			tVv[i] = Float.intBitsToFloat(tV[i * 8 + 5]);
		}
		// (a) the UV walk: vertex i carries corner (i + k) % 4 of (0,0),(0,16),(16,16),(16,0)
		int tK = GTFluidPipeFlowModel.arrowRotation(aMarked, aPatchFace) / 90;
		float[][] tCorners = { {0, 0}, {0, 16}, {16, 16}, {16, 0} };
		for (int i = 0; i < 4; i++) {
			float[] tCorner = tCorners[(i + tK) % 4];
			assertEquals(tCorner[0], tU[i], 0.1F, aMarked + "/" + aPatchFace + " vertex " + i + " U");
			assertEquals(tCorner[1], tVv[i], 0.1F, aMarked + "/" + aPatchFace + " vertex " + i + " V");
		}
		// (b) the head (V≈0 vertices) centre sits along the marked normal from the tail centre
		float tTopX = 0, tTopY = 0, tTopZ = 0, tBotX = 0, tBotY = 0, tBotZ = 0;
		int tTop = 0, tBot = 0;
		for (int i = 0; i < 4; i++) {
			if (tVv[i] < 8) { tTopX += tX[i]; tTopY += tY[i]; tTopZ += tZ[i]; tTop++; }
			else { tBotX += tX[i]; tBotY += tY[i]; tBotZ += tZ[i]; tBot++; }
		}
		assertEquals(2, tTop, "the sprite-top edge is two vertices");
		// vertex POSITIONS are 0..1 block units (FaceBakery.setupShape divides by 16), UVs are 0..16
		float tDx = (tTopX - tBotX) / 2, tDy = (tTopY - tBotY) / 2, tDz = (tTopZ - tBotZ) / 2;
		float tLen = SIZE;
		assertEquals(tLen * aMarked.getStepX(), tDx, 0.01F, aMarked + "/" + aPatchFace + " arrow ΔX");
		assertEquals(tLen * aMarked.getStepY(), tDy, 0.01F, aMarked + "/" + aPatchFace + " arrow ΔY");
		assertEquals(tLen * aMarked.getStepZ(), tDz, 0.01F, aMarked + "/" + aPatchFace + " arrow ΔZ");
		// (c) the quad sits on the arm plane, one epsilon off the arm face (h or 1-h)
		float tPlane = aPatchFace.getAxisDirection() == Direction.AxisDirection.POSITIVE
				? (1 - HALF + (float)GTFluidPipeFlowModel.ARROW_EPSILON)
				: (HALF - (float)GTFluidPipeFlowModel.ARROW_EPSILON);
		int tS = aPatchFace.getAxis().ordinal();
		for (int i = 0; i < 4; i++) {
			float tCoord = switch (tS) { case 0 -> tX[i]; case 1 -> tY[i]; default -> tZ[i]; };
			assertEquals(tPlane, tCoord, 0.005F, aMarked + "/" + aPatchFace + " plane coordinate on " + aPatchFace.getAxis());
		}
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
		// the patch geometry rides the seat — the flow model sizes its patches to the row's
		// connected-arm cross-section (8px here)
		assertEquals(GTFluidPipeFlowModel.halfOf(8),
				((GTFluidPipeFlowModel) ((GTFluidPipeFoamModel) tValue).getFallbackModel()).half(), 1e-9);
	}

	/**
	 * THE pipe-output-arrows direction pin, item form: the mask the ITEM pipe BE derives
	 * ({@code outputArrowMask} — one emit-disabled face leaves the other five emitting)
	 * plans four patches per still-emitting face = twenty regardless of WHICH face is off;
	 * the unconstrained pipe (mask 0) plans nothing.
	 */
	@Test
	void itemPipeDerivedMaskPlansArrowsOnEveryStillEmittingFace() {
		for (Direction tFace : Direction.values()) {
			byte tMask = (byte)(63 & ~(1 << tFace.get3DDataValue())); // the outputArrowMask(emit-off tFace) form
			List<GTFluidPipeFlowModel.FlowQuad> tPlans = GTFluidPipeFlowModel.planQuads(new PipeFlowSnapshot(tMask, false), null, HALF);
			assertEquals(20, tPlans.size(), tFace + " emit-disabled: five emitting faces x four patches");
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
	 * to the solid layer are never baked at all. pipe-flow-indicator-rework form: the
	 * fixture mask 1 marks DOWN, and the DOWN arm paints its four SIDE faces, so the
	 * NORTH pass carries the one patch.
	 */
	@Test
	void cutoutPassCarriesTheArrowsAndTheFoam() {
		GTFluidPipeFoamModel tChain = chainOver(new RecordingBody());
		// the fixture mask 1 marks DOWN — its patches ride the N/S/W/E passes
		List<BakedQuad> tArrows = tChain.getQuads(null, Direction.NORTH, RandomSource.create(),
				dataWithFlowAndPaint(), net.minecraft.client.renderer.RenderType.cutout());
		assertEquals(1, tArrows.size() - RecordingBody.BODY_QUADS.size(), "one patch on the NORTH pass, cutout layer");
		List<BakedQuad> tFoam = tChain.getQuads(null, Direction.UP, RandomSource.create(),
				GTModelProperties.snapshot().with(GTModelProperties.FOAM_SNAPSHOT, new PipeFoamSnapshot(false, false)).build(),
				net.minecraft.client.renderer.RenderType.cutout());
		assertTrue(tFoam.size() > RecordingBody.BODY_QUADS.size(), "the fresh foam overlay rides the cutout layer");
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
