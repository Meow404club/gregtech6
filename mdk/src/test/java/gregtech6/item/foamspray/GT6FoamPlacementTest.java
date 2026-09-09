package gregtech6.item.foamspray;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.state.properties.SlabType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The mode 0-4 air-placement truth table (task p26-c-foam-block-family SPEC pin ① — the
 * card's biggest uncertainty face, pinned offline): the pure {@link GT6FoamPlacement} arm
 * walk over a recording sink, the upstream Behavior_Spray_Foam.foam() :132-176 semantics
 * transposed — the origin = clicked+face (:132), the player-side orientation (:135), the
 * per-mode geometry (:137/:140/:146/:159/:162), the unit costs (10 blocks / 5 slabs), the
 * owned ternary existing ONLY on the block modes and the budget/skip/break behaviour
 * (:156 budget-out breaks, a failed placement SKIPS in the planes but BREAKS the line).
 */
public class GT6FoamPlacementTest {

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// Offline bootstrap noise is expected; the geometry needs no registries.
		}
	}

	static final BlockPos CLICKED = new BlockPos(10, 20, 30);
	static final Direction FACE = Direction.UP;
	static final BlockPos ORIGIN = CLICKED.relative(FACE); // :132 — (10, 21, 30)

	// ------------------------------------------------------------------ the geometry

	@Test
	void mode0IsOneBlockAtTheFaceNeighbour() {
		List<GT6FoamPlacement.FoamPlacement> tPlan = GT6FoamPlacement.planOf(0, CLICKED, FACE, Direction.NORTH, 0.5F);
		assertEquals(1, tPlan.size(), "mode 0 = the single block (:137)");
		assertEquals(ORIGIN, tPlan.get(0).pos());
		assertTrue(tPlan.get(0).slab() == false && tPlan.get(0).slabType() == null, "a FULL block placement");
	}

	@Test
	void mode1IsTheFourLineSteppingOppositeThePlayerSide() {
		List<GT6FoamPlacement.FoamPlacement> tPlan = GT6FoamPlacement.planOf(1, CLICKED, FACE, Direction.EAST, 0.5F);
		assertEquals(4, tPlan.size(), "mode 1 = four blocks (:140)");
		// the first lands at the origin, then the walk steps OPPOSITE the player side (:143)
		assertEquals(ORIGIN, tPlan.get(0).pos());
		assertEquals(ORIGIN.west(), tPlan.get(1).pos());
		assertEquals(ORIGIN.west(2), tPlan.get(2).pos());
		assertEquals(ORIGIN.west(3), tPlan.get(3).pos());
	}

	@Test
	void mode2IsTheThreeByThreePlanePerpendicularToThePlayerSide() {
		// a DOWN-looking player over the UP face: the plane lives on XZ at the origin's Y
		List<GT6FoamPlacement.FoamPlacement> tPlan = GT6FoamPlacement.planOf(2, CLICKED, FACE, Direction.DOWN, 0.5F);
		assertEquals(9, tPlan.size(), "mode 2 = nine blocks (:146)");
		for (GT6FoamPlacement.FoamPlacement tPlacement : tPlan) {
			assertEquals(ORIGIN.getY(), tPlacement.pos().getY(), "the plane is perpendicular to Y");
			int tDx = tPlacement.pos().getX() - ORIGIN.getX();
			int tDz = tPlacement.pos().getZ() - ORIGIN.getZ();
			assertTrue(tDx >= -1 && tDx <= 1 && tDz >= -1 && tDz <= 1, "the 3x3 span around the origin");
		}
		// the base shifts one NEGATIVE step on BOTH perpendicular axes (:147-149), then +i/+j
		assertEquals(ORIGIN.offset(-1, 0, -1), tPlan.get(0).pos(), "the walk starts at the shifted base");
		// the +j axis (Z/SOUTH) is the INNERMOST walk (the upstream for-i/for-j order of :151:
		// j increments fastest for every i) — tSide DOWN rides x+=i, z+=j
		assertEquals(ORIGIN.offset(-1, 0, 0), tPlan.get(1).pos(), "the second cell steps the +j SOUTH axis");
	}

	@Test
	void mode2PlaneAxisFollowsThePlayerSide() {
		// a SOUTH-facing player: the plane is on XY (the Z axis is the facing)
		List<GT6FoamPlacement.FoamPlacement> tPlan = GT6FoamPlacement.planOf(2, CLICKED, FACE, Direction.SOUTH, 0.5F);
		assertEquals(9, tPlan.size());
		for (GT6FoamPlacement.FoamPlacement tPlacement : tPlan) {
			assertEquals(ORIGIN.getZ(), tPlacement.pos().getZ(), "the plane is perpendicular to Z");
		}
	}

	@Test
	void mode3IsOneSlabWithTheClickedFaceHalf() {
		List<GT6FoamPlacement.FoamPlacement> tPlan = GT6FoamPlacement.planOf(3, CLICKED, FACE, Direction.NORTH, 0.9F);
		assertEquals(1, tPlan.size(), "mode 3 = the single slab (:159)");
		assertEquals(ORIGIN, tPlan.get(0).pos());
		assertTrue(tPlan.get(0).slab(), "a slab placement");
		assertEquals(SlabType.BOTTOM, tPlan.get(0).slabType(), "clicking UP lands the BOTTOM half (OPOS[UP]=BOTTOM — the slab rests on the floor)");
	}

	@Test
	void mode4IsTheThreeByThreeSlabPlaneOverThePlayerSide() {
		List<GT6FoamPlacement.FoamPlacement> tPlan = GT6FoamPlacement.planOf(4, CLICKED, FACE, Direction.DOWN, 0.9F);
		assertEquals(9, tPlan.size(), "mode 4 = nine slabs (:162)");
		for (GT6FoamPlacement.FoamPlacement tPlacement : tPlan) {
			assertTrue(tPlacement.slab(), "every placement is a slab");
			assertEquals(SlabType.TOP, tPlacement.slabType(), "the DOWN player → TOP halves uniformly (OPOS[DOWN]=mSlabs[1])");
		}
		// and the geometry matches the mode-2 walk
		assertEquals(ORIGIN.offset(-1, 0, -1), tPlan.get(0).pos());
	}

	// ------------------------------------------------------------------ the slab-half mapping

	@Test
	void halfOfIsTheOposMappingWithTheHorizontalHitFallback() {
		assertEquals(SlabType.BOTTOM, GT6FoamPlacement.halfOf(Direction.UP, 0.9F), "UP → BOTTOM (rests on the clicked floor)");
		assertEquals(SlabType.TOP, GT6FoamPlacement.halfOf(Direction.DOWN, 0.1F), "DOWN → TOP (hangs under the clicked ceiling)");
		assertEquals(SlabType.BOTTOM, GT6FoamPlacement.halfOf(Direction.NORTH, 0.4F), "a horizontal face falls to the hit height (vanilla rule)");
		assertEquals(SlabType.TOP, GT6FoamPlacement.halfOf(Direction.WEST, 0.6F), "the upper half of the hit block");
	}

	// ------------------------------------------------------------------ the payment arms

	@Test
	void costsAreTenPerBlockAndFivePerSlab() {
		assertEquals(10L, GT6FoamPlacement.foamArm(0, CLICKED, FACE, Direction.NORTH, 0.5F, 256, pos -> true), "mode 0 pays 10 (:137)");
		assertEquals(40L, GT6FoamPlacement.foamArm(1, CLICKED, FACE, Direction.NORTH, 0.5F, 256, pos -> true), "mode 1 pays 4x10 (:142)");
		assertEquals(90L, GT6FoamPlacement.foamArm(2, CLICKED, FACE, Direction.DOWN, 0.5F, 256, pos -> true), "mode 2 pays 9x10 (:154)");
		assertEquals(5L, GT6FoamPlacement.foamArm(3, CLICKED, FACE, Direction.NORTH, 0.5F, 256, pos -> true), "mode 3 pays 5 (:160)");
		assertEquals(45L, GT6FoamPlacement.foamArm(4, CLICKED, FACE, Direction.DOWN, 0.5F, 256, pos -> true), "mode 4 pays 9x5 (:168)");
	}

	@Test
	void budgetExhaustionBreaksTheWalk() {
		assertEquals(0L, GT6FoamPlacement.foamArm(0, CLICKED, FACE, Direction.NORTH, 0.5F, 9, pos -> true),
				"a sub-cost budget places nothing");
		assertEquals(20L, GT6FoamPlacement.foamArm(1, CLICKED, FACE, Direction.NORTH, 0.5F, 25, pos -> true),
				"the line stops when the budget cannot cover the next block (:142 aUses >= 10)");
		assertEquals(80L, GT6FoamPlacement.foamArm(2, CLICKED, FACE, Direction.DOWN, 0.5F, 85, pos -> true),
				"the plane stops at the budget-out (:156 else break)");
		assertEquals(5L, GT6FoamPlacement.foamArm(3, CLICKED, FACE, Direction.NORTH, 0.5F, 7, pos -> true),
				"the slab needs its 5 (:160)");
	}

	/**
	 * The failure semantics asymmetry (:142 vs :154): the LINE breaks on the first failed
	 * placement, the PLANES skip it and keep walking.
	 */
	@Test
	void lineBreaksAndPlanesSkipOnFailure() {
		assertEquals(0L, GT6FoamPlacement.foamArm(1, CLICKED, FACE, Direction.NORTH, 0.5F, 256, pos -> false),
				"the line's first failure breaks (:142 else break)");
		assertEquals(0L, GT6FoamPlacement.foamArm(2, CLICKED, FACE, Direction.DOWN, 0.5F, 256, pos -> false),
				"the plane's failures all skip (:153-155 — the if-wrap, no break)");
		// the plane skips ONE failure and still spends the other eight
		assertEquals(80L, GT6FoamPlacement.foamArm(2, CLICKED, FACE, Direction.DOWN, 0.5F, 256,
				pos -> !pos.pos().equals(ORIGIN)), "nine minus the one skipped origin cell");
	}

	/** The sink's air-gate authority: the placement record carries no live state (the caller decides). */
	@Test
	void planOverAnAlwaysTrueSinkIsTheAcceptedOrder() {
		List<GT6FoamPlacement.FoamPlacement> tPlan = GT6FoamPlacement.planOf(1, CLICKED, FACE, Direction.NORTH, 0.5F);
		assertEquals(4, tPlan.size());
		for (int i = 0; i < 4; i++) {
			assertNull(tPlan.get(i).slabType(), "line blocks carry no half");
		}
	}
}
