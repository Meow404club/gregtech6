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
