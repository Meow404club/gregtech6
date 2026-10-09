package gregtech6.covers.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

import net.minecraftforge.client.model.data.ModelData;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import gregtech6.covers.GTCoverRenderSnapshot;
import gregtech6.client.render.GTModelProperties;
import gregtech6.client.render.GTOfflineRenderTestBase;

/**
 * The cover plate render-path sentinel tests (task cover-core acceptance ③, offline
 * half): the planner is pure geometry over the immutable snapshot — the quad emission
 * rules, the GTCEu slab geometry (2px thickness + the 0.002 Z-fighting epsilon), the
 * snapshot freeze and the client registration hook — plus the render-leftovers bake
 * pins (the IdentitySprite fixture form of the in-flight FaceBakePins, self-built here to
 * avoid a cross-branch collision): the outer decal quads bake the canonical full-face UV
 * walk on all six cover faces and the rims bake the 1.7.10 proportional top band.
 */
public class CoverPlateModelTest extends GTOfflineRenderTestBase {

	private static final ResourceLocation SPRITE_UP = ResourceLocation.fromNamespaceAndPath("gt6", "block/cover/test_up");
	private static final ResourceLocation SPRITE_NORTH = ResourceLocation.fromNamespaceAndPath("gt6", "block/cover/test_north");


	@AfterEach
	void clearRegistration() {
		gregtech6.client.render.GTRenderModelListener.clearForTest();
	}

	private static GTCoverRenderSnapshot snapshot(Direction... aFaces) {
		Map<Direction, ResourceLocation> tSprites = new HashMap<>();
		for (Direction tFace : aFaces) tSprites.put(tFace, tFace == Direction.UP ? SPRITE_UP : SPRITE_NORTH);
		return new GTCoverRenderSnapshot(tSprites);
	}

	@Test
	void snapshotFreezesTheSpriteMap() {
		Map<Direction, ResourceLocation> tSprites = new HashMap<>();
		tSprites.put(Direction.UP, SPRITE_UP);
		GTCoverRenderSnapshot tSnapshot = new GTCoverRenderSnapshot(tSprites);
		tSprites.put(Direction.DOWN, SPRITE_NORTH); // the builder froze its copy at construction
		assertFalse(tSnapshot.hasCover(Direction.DOWN), "the record holds a frozen copy");
		assertEquals(SPRITE_UP, tSnapshot.sprite(Direction.UP));
		assertEquals((byte) (1 << Direction.UP.get3DDataValue()), tSnapshot.mask());
	}

	@Test
	void slabGeometryMatchesTheUpstreamCoverPlate() {
		// DOWN: the outer 2px at the bottom face, inflated by the GTCEu COVER_OVERLAY epsilon
		double[] tDown = CoverPlateModel.slabOf(Direction.DOWN);
		assertEquals(-CoverPlateModel.PLATE_EPSILON, tDown[0], 1e-9);
		assertEquals(CoverPlateModel.PLATE_THICKNESS, tDown[4], 1e-9, "maxY = the 2px plate (COVER_OVERLAY.setMaxY(thickness))");
		// UP: the slab hangs from the top face
		double[] tUp = CoverPlateModel.slabOf(Direction.UP);
		assertEquals(1 - CoverPlateModel.PLATE_THICKNESS, tUp[1], 1e-9, "minY = 1 - thickness");
		assertEquals(1 + CoverPlateModel.PLATE_EPSILON, tUp[4], 1e-9, "maxY pokes past the boundary (Z-fighting avoidance)");
		// EAST: the slab hugs the east face
		double[] tEast = CoverPlateModel.slabOf(Direction.EAST);
		assertEquals(1 - CoverPlateModel.PLATE_THICKNESS, tEast[0], 1e-9, "minX = 1 - thickness");
	}

	@Test
	void outerFaceEmittedOnTheCoveredPassOnly() {
		GTCoverRenderSnapshot tSnapshot = snapshot(Direction.UP);
		List<CoverPlateModel.PlateQuad> tUp = CoverPlateModel.planQuads(tSnapshot, Direction.UP);
		assertEquals(1, tUp.size());
		assertEquals(Direction.UP, tUp.get(0).quadFace());
		assertTrue(tUp.get(0).cull(), "the outer face is culled by the neighbour");
		assertEquals(SPRITE_UP, tUp.get(0).sprite());

		List<CoverPlateModel.PlateQuad> tNull = CoverPlateModel.planQuads(tSnapshot, null);
		assertEquals(1, tNull.size(), "the null pass carries the unculled back face");
		assertEquals(Direction.DOWN, tNull.get(0).quadFace(), "the back face of the UP plate faces DOWN");
		assertFalse(tNull.get(0).cull(), "an inner face cannot cull");
	}

	@Test
	void rimSuppressedWhenThePerpendicularFaceIsCovered() {
		GTCoverRenderSnapshot tBoth = snapshot(Direction.UP, Direction.NORTH);
		List<CoverPlateModel.PlateQuad> tEast = CoverPlateModel.planQuads(tBoth, Direction.EAST);
		assertEquals(2, tEast.size(), "EAST uncovered: both plates put a rim on the EAST pass");
		assertTrue(tEast.stream().allMatch(t -> t.quadFace() == Direction.EAST && t.cull()));
		assertTrue(tEast.stream().anyMatch(t -> t.sprite() == SPRITE_UP) && tEast.stream().anyMatch(t -> t.sprite() == SPRITE_NORTH),
				"each rim carries its own plate's sprite");

		List<CoverPlateModel.PlateQuad> tNorth = CoverPlateModel.planQuads(tBoth, Direction.NORTH);
		assertEquals(1, tNorth.size(), "the NORTH pass gets its own plate's outer face only");
		assertEquals(Direction.NORTH, tNorth.get(0).quadFace());

		assertEquals(2, CoverPlateModel.planQuads(tBoth, null).size(), "both back faces on the null pass");

		// with NORTH uncovered, the UP plate would emit a NORTH-facing rim there instead
		List<CoverPlateModel.PlateQuad> tNorthOnUpOnly = CoverPlateModel.planQuads(snapshot(Direction.UP), Direction.NORTH);
		assertEquals(1, tNorthOnUpOnly.size());
		assertEquals(Direction.NORTH, tNorthOnUpOnly.get(0).quadFace());
		assertEquals(SPRITE_UP, tNorthOnUpOnly.get(0).sprite(), "the rim carries the UP plate's sprite");
	}

	@Test
	void noQuadsWithoutCovers() {
		assertTrue(CoverPlateModel.planQuads(new GTCoverRenderSnapshot(Map.of()), Direction.UP).isEmpty());
		assertTrue(CoverPlateModel.planQuads(new GTCoverRenderSnapshot(Map.of()), null).isEmpty());
	}

	// ---------------------------------------------------------------------------
	// the layer table (task render-cover-multilayer)
	// ---------------------------------------------------------------------------

	/**
	 * Zero-behavior-change face: an unmapped (single-layer) sprite plans EXACTLY the
	 * pre-p11 structure — one quad per emission rule, the unmoved slab box, the face's
	 * own sprite, layer index 0.
	 */
	@Test
	void singleLayerPlansAreByteIdenticalToTheLegacyPlanner() {
		GTCoverRenderSnapshot tSnapshot = snapshot(Direction.UP);
		double e = CoverPlateModel.PLATE_EPSILON, t = CoverPlateModel.PLATE_THICKNESS;
		double[] tLegacyUpSlab = {-e, 1 - t, -e, 1 + e, 1 + e, 1 + e};

		List<CoverPlateModel.PlateQuad> tUp = CoverPlateModel.planQuads(tSnapshot, Direction.UP);
		assertEquals(1, tUp.size());
		assertLegacyQuad(tUp.get(0), Direction.UP, true, SPRITE_UP, tLegacyUpSlab);

		List<CoverPlateModel.PlateQuad> tNull = CoverPlateModel.planQuads(tSnapshot, null);
		assertEquals(1, tNull.size());
		assertLegacyQuad(tNull.get(0), Direction.DOWN, false, SPRITE_UP, tLegacyUpSlab);

		List<CoverPlateModel.PlateQuad> tRim = CoverPlateModel.planQuads(tSnapshot, Direction.NORTH);
		assertEquals(1, tRim.size());
		assertLegacyQuad(tRim.get(0), Direction.NORTH, true, SPRITE_UP, tLegacyUpSlab);
	}

	/** The double-layer cover: the plate background at the unmoved slab, the surface sprite one epsilon outward, front face only. */
	@Test
	void doubleLayerCoverPlansBasePlusOffsetForeground() {
		// the redstone machine switch is a census hit: [covers/base, redstone_switch/circuit]
		ResourceLocation tFg = gregtech6.covers.covers.CoverControllerRedstone.sprite();
		Map<Direction, ResourceLocation> tSprites = new HashMap<>();
		tSprites.put(Direction.UP, tFg);
		GTCoverRenderSnapshot tSnapshot = new GTCoverRenderSnapshot(tSprites);
		double e = CoverPlateModel.PLATE_EPSILON, t = CoverPlateModel.PLATE_THICKNESS;
		double[] tBaseSlab = {-e, 1 - t, -e, 1 + e, 1 + e, 1 + e};

		List<CoverPlateModel.PlateQuad> tUp = CoverPlateModel.planQuads(tSnapshot, Direction.UP);
		assertEquals(2, tUp.size(), "the background slab + one foreground decal");
		assertLegacyQuad(tUp.get(0), Direction.UP, true, GTCoverRenderSnapshot.SPRITE_PLATE_BASE, tBaseSlab);
		CoverPlateModel.PlateQuad tFgQuad = tUp.get(1);
		assertEquals(1, tFgQuad.layer());
		assertEquals(tFg, tFgQuad.sprite(), "the surface sprite rides the offset layer");
		assertEquals(Direction.UP, tFgQuad.quadFace());
		assertTrue(tFgQuad.cull());
		assertEquals(1 - t + e, tFgQuad.box()[1], 1e-12, "the foreground slab shifts outward by epsilon * layer");
		assertEquals(1 + 2 * e, tFgQuad.box()[4], 1e-12, "the foreground's visible face sits one epsilon past the background's");
		assertEquals(tBaseSlab[0], tFgQuad.box()[0], 1e-12, "tangential axes unmoved …");
		assertEquals(tBaseSlab[2], tFgQuad.box()[2], 1e-12);
		assertEquals(tBaseSlab[3], tFgQuad.box()[3], 1e-12);
		assertEquals(tBaseSlab[5], tFgQuad.box()[5], 1e-12, "… so the front face's UV extents are identical: pixel-aligned layers");

		// the foreground is a face decal: the null pass (back face) and the rim pass stay background-only
		List<CoverPlateModel.PlateQuad> tNull = CoverPlateModel.planQuads(tSnapshot, null);
		assertEquals(1, tNull.size(), "the unculled back face is the background layer only");
		assertLegacyQuad(tNull.get(0), Direction.DOWN, false, GTCoverRenderSnapshot.SPRITE_PLATE_BASE, tBaseSlab);
		List<CoverPlateModel.PlateQuad> tRim = CoverPlateModel.planQuads(tSnapshot, Direction.NORTH);
		assertEquals(1, tRim.size(), "the rim is the background layer only");
		assertLegacyQuad(tRim.get(0), Direction.NORTH, true, GTCoverRenderSnapshot.SPRITE_PLATE_BASE, tBaseSlab);
	}

	/** The layer shift runs along each face's own normal (north = -Z here), the front face always one epsilon outward per layer. */
	@Test
	void doubleLayerShiftFollowsTheCoverNormal() {
		ResourceLocation tFg = gregtech6.covers.covers.CoverControllerRedstone.sprite();
		Map<Direction, ResourceLocation> tSprites = new HashMap<>();
		tSprites.put(Direction.NORTH, tFg);
		GTCoverRenderSnapshot tSnapshot = new GTCoverRenderSnapshot(tSprites);
		double e = CoverPlateModel.PLATE_EPSILON, t = CoverPlateModel.PLATE_THICKNESS;

		List<CoverPlateModel.PlateQuad> tNorth = CoverPlateModel.planQuads(tSnapshot, Direction.NORTH);
		assertEquals(2, tNorth.size());
		assertEquals(-e, tNorth.get(0).box()[2], 1e-12, "the background's north face at the legacy position");
		assertEquals(-2 * e, tNorth.get(1).box()[2], 1e-12, "the foreground's north face one epsilon further out");
		assertEquals(t, tNorth.get(0).box()[5], 1e-12);
		assertEquals(t - e, tNorth.get(1).box()[5], 1e-12);
	}

	/**
	 * The facet family (task cover-underlay-census, upstream CoverVent :78-79): the vent's
	 * outer face keeps the two-layer census stack [base, front], while the null-pass back
	 * quad and the rim quads swap the background for the vent's dedicated facet sprites —
	 * single sprites, no base behind a facet (upstream returns ONE texture per attachment
	 * face). The borrowed back/sides PNGs live in textures/block/vent/ (assets README).
	 */
	@Test
	void facetCoverPlansItsBackAndRimSprites() {
		ResourceLocation tVentFront = new gregtech6.covers.covers.CoverVent().getCoverTextureSurface((byte) 0, null);
		ResourceLocation tVentBack = ResourceLocation.fromNamespaceAndPath("gt6", "block/vent/back");
		ResourceLocation tVentSides = ResourceLocation.fromNamespaceAndPath("gt6", "block/vent/sides");
		Map<Direction, ResourceLocation> tSprites = new HashMap<>();
		tSprites.put(Direction.UP, tVentFront);
		GTCoverRenderSnapshot tSnapshot = new GTCoverRenderSnapshot(tSprites);
		double e = CoverPlateModel.PLATE_EPSILON, t = CoverPlateModel.PLATE_THICKNESS;
		double[] tBaseSlab = {-e, 1 - t, -e, 1 + e, 1 + e, 1 + e};

		// the outer face: the ordinary double-layer census stack — the facet only touches back/rim
		List<CoverPlateModel.PlateQuad> tUp = CoverPlateModel.planQuads(tSnapshot, Direction.UP);
		assertEquals(2, tUp.size());
		assertLegacyQuad(tUp.get(0), Direction.UP, true, GTCoverRenderSnapshot.SPRITE_PLATE_BASE, tBaseSlab);
		assertEquals(tVentFront, tUp.get(1).sprite());
		assertEquals(1, tUp.get(1).layer(), "the vent front rides the offset foreground layer");

		// the null pass: the unculled back face carries the vent's back art, not the base
		List<CoverPlateModel.PlateQuad> tNull = CoverPlateModel.planQuads(tSnapshot, null);
		assertEquals(1, tNull.size());
		assertLegacyQuad(tNull.get(0), Direction.DOWN, false, tVentBack, tBaseSlab);

		// the rim pass: the vent's sides art, not the base
		List<CoverPlateModel.PlateQuad> tRim = CoverPlateModel.planQuads(tSnapshot, Direction.NORTH);
		assertEquals(1, tRim.size());
		assertLegacyQuad(tRim.get(0), Direction.NORTH, true, tVentSides, tBaseSlab);
	}

	/** The pre-p11 quad contract: the legacy emission rule, the unmoved slab, the face sprite, layer 0. */
	private static void assertLegacyQuad(CoverPlateModel.PlateQuad aQuad, Direction aFace, boolean aCull,
			ResourceLocation aSprite, double[] aBox) {
		assertEquals(aFace, aQuad.quadFace());
		assertEquals(aCull, aQuad.cull());
		assertEquals(aSprite, aQuad.sprite());
		assertEquals(0, aQuad.layer(), "the legacy plan carries only layer 0");
		assertTrue(java.util.Arrays.equals(aBox, aQuad.box()), "the slab box is byte-identical to the pre-p11 planner: " + java.util.Arrays.toString(aQuad.box()));
	}

	@Test
	void clientRegistrationHooksTheOvenModels() {
		assertEquals(0, gregtech6.client.render.GTRenderModelListener.registeredCount());
		GTCoverClientListener.register();
		assertEquals(GTCoverClientListener.TARGET_MODELS.size(), gregtech6.client.render.GTRenderModelListener.registeredCount(),
				"the oven's 16 per-state models carry the dynamic plate model");
		GTCoverClientListener.register(); // idempotent
		assertEquals(GTCoverClientListener.TARGET_MODELS.size(), gregtech6.client.render.GTRenderModelListener.registeredCount());
	}

	// ---------------------------------------------------------------------------
	// the render-leftovers bake pins (the FaceBakePins fixture form)
	// ---------------------------------------------------------------------------

	/** The int[] vertex stride (FaceBakery.VERTEX_INT_SIZE) and UV slot offsets. */
	private static final int STRIDE = 8, U_SLOT = 4, V_SLOT = 5;

	/** The forge FaceBakery anti-bleed nudge headroom (uv*0.999 + opposite*0.001 → ≤0.016 at 0..16, plus the 0.032 epsilon slack). */
	private static final float TOLERANCE = 0.1F;

	/**
	 * THE render-leftovers PIN (outer decal face): every cover face's own pass bakes the
	 * canonical rotation-0 full-face walk (0,0),(0,16),(16,16),(16,0) — the vanilla cube
	 * JSON form, sprite top (V=0) on the side faces' top corners (upright). The old GTCEu
	 * cubeUV table flipped every non-UP outer face (V mirror on DOWN/SOUTH/WEST, 180° on
	 * NORTH/EAST) — the redstone_emitter digits and the top-left-anchored circuit icons
	 * made that visible, hence the fix.
	 */
	@Test
	void outerDecalQuadsBakeTheCanonicalFullFaceUvWalk() {
		for (Direction tFace : Direction.values()) {
			CoverPlateModel tModel = new CoverPlateModel(new StubFallback(), aSpriteId -> IdentitySprite.INSTANCE);
			ModelData tData = ModelData.builder()
					.with(GTModelProperties.RENDER_SNAPSHOT, snapshot(tFace)).build();
			List<BakedQuad> tQuads = tModel.getQuads(null, tFace, RandomSource.create(), tData, null);
			assertEquals(1, tQuads.size(), tFace + ": the cover's own pass carries its outer decal quad");
			assertCanonicalFullFaceUv(tQuads.get(0), tFace);
		}
	}

	/**
	 * The unculled back face (null pass) and the 2px rim both carry the background plate:
	 * the back face walks the canonical form of its own quad face; the rim samples the
	 * 1.7.10 proportional TOP band (V ≈ 0 at the top corners, V ≈ 2 at the bottom — the
	 * 2px strip), not a full-sprite squeeze and not the old table's upside-down bottom band.
	 */
	@Test
	void backFaceAndRimBakeTheVanillaForms() {
		CoverPlateModel tModel = new CoverPlateModel(new StubFallback(), aSpriteId -> IdentitySprite.INSTANCE);
		ModelData tData = ModelData.builder()
				.with(GTModelProperties.RENDER_SNAPSHOT, snapshot(Direction.UP)).build();

		List<BakedQuad> tBack = tModel.getQuads(null, null, RandomSource.create(), tData, null);
		assertEquals(1, tBack.size(), "the null pass carries the unculled back face");
		assertCanonicalFullFaceUv(tBack.get(0), Direction.DOWN);

		List<BakedQuad> tRim = tModel.getQuads(null, Direction.NORTH, RandomSource.create(), tData, null);
		assertEquals(1, tRim.size(), "the NORTH pass carries the UP plate's rim");
		int[] tV = tRim.get(0).getVertices();
		for (int i = 0; i < 4; i++) {
			float tU = Float.intBitsToFloat(tV[i * STRIDE + U_SLOT]), tVv = Float.intBitsToFloat(tV[i * STRIDE + V_SLOT]);
			assertTrue(Math.abs(tU) < TOLERANCE || Math.abs(tU - 16) < TOLERANCE, "rim vertex " + i + " U spans the full width: " + tU);
		}
		assertTrue(Math.abs(Float.intBitsToFloat(tV[V_SLOT])) < TOLERANCE
				&& Math.abs(Float.intBitsToFloat(tV[3 * STRIDE + V_SLOT])) < TOLERANCE,
				"the rim's top corners (vertices 0/3) carry the sprite-top V=0 (the 1.7.10 proportional band)");
		assertTrue(Math.abs(Float.intBitsToFloat(tV[STRIDE + V_SLOT]) - 2.0F) < TOLERANCE
				&& Math.abs(Float.intBitsToFloat(tV[2 * STRIDE + V_SLOT]) - 2.0F) < TOLERANCE,
				"the rim's bottom corners sit at V=2 (the 2px band depth 16 - minY*16)");
	}

	/**
	 * The canonical rotation-0 full-face walk (0,0),(0,16),(16,16),(16,0) in FaceInfo vertex
	 * order, cross-checked against the quad's own positions (side top corners carry V=0;
	 * UP reads V=0 at MIN_Z, DOWN at MAX_Z — the renderFixedNegativeYFacing form).
	 */
	private static void assertCanonicalFullFaceUv(BakedQuad aQuad, Direction aFace) {
		int[] tV = aQuad.getVertices();
		float[][] tWalk = {{0, 0}, {0, 16}, {16, 16}, {16, 0}};
		for (int i = 0; i < 4; i++) {
			assertEquals(tWalk[i][0], Float.intBitsToFloat(tV[i * STRIDE + U_SLOT]), TOLERANCE, aFace + " vertex " + i + " U");
			assertEquals(tWalk[i][1], Float.intBitsToFloat(tV[i * STRIDE + V_SLOT]), TOLERANCE, aFace + " vertex " + i + " V");
		}
		float tY0 = Float.intBitsToFloat(tV[1]), tY1 = Float.intBitsToFloat(tV[1 + STRIDE]),
				tY2 = Float.intBitsToFloat(tV[2 * STRIDE + 1]), tY3 = Float.intBitsToFloat(tV[3 * STRIDE + 1]);
		if (aFace.getAxis().isHorizontal()) {
			assertTrue(tY0 > tY1 && tY0 > tY2 && tY3 > tY1 && tY3 > tY2, aFace + ": vertices 0/3 are the top corners");
		} else if (aFace == Direction.UP) {
			assertTrue(Float.intBitsToFloat(tV[2]) < Float.intBitsToFloat(tV[2 + STRIDE]),
					aFace + ": vertex 0 sits at the MIN_Z edge (V=0 there)");
		} else {
			assertTrue(Float.intBitsToFloat(tV[2]) > Float.intBitsToFloat(tV[2 + STRIDE]),
					aFace + ": vertex 0 sits at the MAX_Z edge (V=0 there, the renderFixedNegativeYFacing form)");
		}
	}

	/** Empty baked model — the dynamic quads never touch it on a snapshot hit. */
	public static final class StubFallback implements BakedModel {
		@Override public List<BakedQuad> getQuads(net.minecraft.world.level.block.state.BlockState aState, Direction aSide, RandomSource aRand) { return List.of(); }
		@Override public boolean useAmbientOcclusion() { return false; }
		@Override public boolean isGui3d() { return false; }
		@Override public boolean usesBlockLight() { return false; }
		@Override public boolean isCustomRenderer() { return false; }
		@Override public TextureAtlasSprite getParticleIcon() { return null; }
		@Override public net.minecraft.client.renderer.block.model.ItemTransforms getTransforms() { return net.minecraft.client.renderer.block.model.ItemTransforms.NO_TRANSFORMS; }
		@Override public net.minecraft.client.renderer.block.model.ItemOverrides getOverrides() { return net.minecraft.client.renderer.block.model.ItemOverrides.EMPTY; }
	}

	/**
	 * The leg-uniform identity atlas stub (the FaceBakePins form, self-built to avoid the
	 * cross-branch name collision): getU/getV return the input unchanged so the decoded
	 * vertex floats ARE the 0..16 model-space values, and a zero uvShrinkRatio kills
	 * FaceBakery's shrink lerp. The legs diverge in sprite coordinate conventions — forge
	 * 1.20.1 passes 0..16 into {@code getU(double)}, 1.21.1 passes 0..1 into
	 * {@code getU(float)} (FaceBakery divides by 16 first) — hence the per-leg overrides.
	 */
	static final class IdentitySprite extends TextureAtlasSprite {
		static final IdentitySprite INSTANCE = new IdentitySprite();

		private IdentitySprite() {
			super(ResourceLocation.fromNamespaceAndPath("gt6", "unit_uv"),
					new net.minecraft.client.renderer.texture.SpriteContents(ResourceLocation.fromNamespaceAndPath("gt6", "unit_uv"),
							new FrameSize(1, 1),
							new NativeImage(1, 1, false),
							//? if forge {
							net.minecraft.client.resources.metadata.animation.AnimationMetadataSection.EMPTY),
							//?} else {
							/*net.minecraft.server.packs.resources.ResourceMetadata.EMPTY),*/
							//?}
					1, 1, 0, 0);
		}

		//? if forge {
		@Override
		public float getU(double aU) { return (float) aU; }

		@Override
		public float getV(double aV) { return (float) aV; }
		//?} else {
		/*@Override
		public float getU(float aU) { return aU * 16.0F; }

		@Override
		public float getV(float aV) { return aV * 16.0F; }*/
		//?}

		@Override
		public float uvShrinkRatio() { return 0.0F; }
	}
}
