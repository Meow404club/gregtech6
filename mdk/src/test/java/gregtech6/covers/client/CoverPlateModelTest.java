package gregtech6.covers.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import gregtech6.covers.GTCoverRenderSnapshot;

/**
 * The cover plate render-path sentinel tests (task p4-cover-core acceptance ③, offline
 * half): the planner is pure geometry over the immutable snapshot — the quad emission
 * rules, the GTCEu slab geometry (2px thickness + the 0.002 Z-fighting epsilon), the
 * snapshot freeze and the client registration hook. The sprite→BakedQuad baker itself
 * is the runClient visual check left to the user (the W2 BakedQuad-offline precedent).
 */
public class CoverPlateModelTest {

	private static final ResourceLocation SPRITE_UP = new ResourceLocation("gt6", "block/cover/test_up");
	private static final ResourceLocation SPRITE_NORTH = new ResourceLocation("gt6", "block/cover/test_north");

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
	// the layer table (task p11-render-cover-multilayer)
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
}
