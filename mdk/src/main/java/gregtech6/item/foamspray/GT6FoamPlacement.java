package gregtech6.item.foamspray;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * The air-placement arm of the C-Foam spray — the 1.20.1 counterpart of the mode 0-4 body
 * of upstream {@code Behavior_Spray_Foam.foam()} (gregtech/items/behaviors/Behavior_Spray_Foam
 * .java:132-176), split into the pure arm walk (this class — offline-testable, no Level)
 * and the live sink (the caller's {@link Sink} does the setBlock + the owned/plain choice,
 * :138).
 *
 * <p>Geometry, upstream verbatim:
 * <ul>
 * <li>the arm origin is the clicked face's NEIGHBOUR (:132 {@code aX += OFFX[aSide]}) —
 *     {@code clicked.relative(face)};</li>
 * <li>the line/area orientation rides the PLAYER side (:135 {@code getSideForPlayerPlacing}
 *     — the port passes the caller-resolved direction);</li>
 * <li>mode 0 :137-139 — one block, 10 units;</li>
 * <li>mode 1 :140-145 — a 4-line stepping OPPOSITE the player side (:143
 *     {@code aX -= OFFX[tSide]}), 10 per block, the walk BREAKS on the first failure;</li>
 * <li>mode 2 :146-158 — a 3x3 plane perpendicular to the player side (the origin shifts
 *     one negative step on both non-facing axes :147-149), 10 per block, non-air positions
 *     are SKIPPED (only budget exhaustion breaks);</li>
 * <li>mode 3 :159-161 — one slab, 5 units, the half from the CLICKED face (the upstream
 *     {@code mSlabs[OPOS[aSide]]} — the ruling_slab vanilla mapping UP→BOTTOM/DOWN→TOP,
 *     horizontal faces fall to the hit height);</li>
 * <li>mode 4 :162-174 — the 3x3 slab plane (the mode-2 geometry), 5 per slab, the half
 *     uniform over the PLAYER side ({@code mSlabs[OPOS[tSide]]});</li>
 * <li>the upstream owned ternary exists ONLY on modes 0-2 (:138) — modes 3/4 always place
 *     the plain fresh slab (Behavior_Spray_Foam.java:160/:170 verbatim), and owned cans
 *     never CYCLE into modes 3/4 (switchMode :191 {@code mOwned ? 3 : 5}).</li>
 * </ul>
 *
 * <p>Declared deviations (the ruling_slab record): the upstream vertical slabs
 * (mSlabs[2..5], the GT6 wall-slab forms) degrade to the vanilla bottom/top halves — a
 * horizontal clicked/player face falls back to the hit height (mode 3) or the hit height
 * of the original click (mode 4), which keeps the floor/ceiling cases exact.
 */
public final class GT6FoamPlacement {

	/** The mode 0-2 unit cost (upstream :137/:142/:154 {@code aUses >= 10}). */
	public static final long BLOCK_COST = 10;

	/** The mode 3/4 slab unit cost (upstream :160/:168 {@code aUses >= 5}). */
	public static final long SLAB_COST = 5;

	/** One accepted placement: the position, the slab flag and the half (null = a full block). */
	public record FoamPlacement(BlockPos pos, boolean slab, @Nullable SlabType slabType) {}

	/**
	 * The live half: check + place one candidate, return success (the upstream
	 * {@code WD.air(...) && setBlock(...)} pair, :138/:142/:153-154/:160/:170). The
	 * implementer owns the air gate, the owned/plain choice and the setBlock; a {@code false}
	 * costs nothing and (mode 1) breaks the walk.
	 */
	public interface Sink {

		/** @return true when the placement LANDED (the caller pays its cost). */
		boolean place(FoamPlacement aPlacement);
	}

	private GT6FoamPlacement() {
	}

	/**
	 * The arm walk (upstream :132-176 verbatim semantics): walks the mode's candidate list
	 * over the budget, drives the sink, and returns the total units SPENT.
	 *
	 * @param aMode        0-4 (the upstream switch, :136)
	 * @param aClicked     the clicked position (the origin is its face neighbour, :132)
	 * @param aFace        the clicked face ({@code aSide} of :132)
	 * @param aPlayerSide  the player-look side ({@code tSide} of :135)
	 * @param aHitY        the click's hit height (the horizontal-face half fallback)
	 * @param aBudget      the remaining internal units
	 * @param aSink        the live half
	 * @return the spent units (the caller's payment, upstream :87 {@code tUses -= tFoamed})
	 */
	public static long foamArm(int aMode, BlockPos aClicked, Direction aFace, Direction aPlayerSide, float aHitY,
			long aBudget, Sink aSink) {
		BlockPos tOrigin = aClicked.relative(aFace); // :132 OFFX/OFFY/OFFZ
		long rSpent = 0;
		switch (aMode) {
			case 0 -> { // :137-139 — single block, break-free single shot
				FoamPlacement tPlacement = new FoamPlacement(tOrigin, false, null);
				if (aBudget >= BLOCK_COST && aSink.place(tPlacement)) rSpent += BLOCK_COST;
			}
			case 1 -> { // :140-145 — the 4-line, stepping opposite the player side; ANY failure breaks
				BlockPos tPos = tOrigin;
				for (int i = 0; i < 4; i++) {
					FoamPlacement tPlacement = new FoamPlacement(tPos, false, null);
					if (aBudget < BLOCK_COST || !aSink.place(tPlacement)) break;
					aBudget -= BLOCK_COST;
					rSpent += BLOCK_COST;
					tPos = tPos.relative(aPlayerSide.getOpposite()); // :143 aX -= OFFX[tSide]
				}
			}
			case 2 -> { // :146-158 — the 3x3 plane, 10 per block, non-air skipped, budget-out breaks
				rSpent += planeWalk(tOrigin, aPlayerSide, false, null, aBudget, aSink);
			}
			case 3 -> { // :159-161 — one slab, 5 units, the half from the clicked face
				FoamPlacement tPlacement = new FoamPlacement(tOrigin, true, halfOf(aFace, aHitY));
				if (aBudget >= SLAB_COST && aSink.place(tPlacement)) rSpent += SLAB_COST;
			}
			case 4 -> { // :162-174 — the 3x3 slab plane, 5 per slab, the half uniform over the player side
				rSpent += planeWalk(tOrigin, aPlayerSide, true, halfOf(aPlayerSide, aHitY), aBudget, aSink);
			}
			default -> { /* the upstream switch has no default arm :175-176 */ }
		}
		return rSpent;
	}

	/**
	 * The shared 3x3 plane walk (modes 2/4): the origin shifts one negative step on both
	 * non-facing axes (:147-149), then the grid walks +i/+j over the two perpendicular axes
	 * (:151-157). Non-air (a failed sink) SKIPS; only budget exhaustion breaks (:156).
	 */
	private static long planeWalk(BlockPos aOrigin, Direction aPlayerSide, boolean aSlab, @Nullable SlabType aType,
			long aBudget, Sink aSink) {
		Direction.Axis tFacingAxis = aPlayerSide.getAxis();
		Direction tFirst = firstPerpendicular(tFacingAxis); // the +i axis (:153 the AXIS_X gate)
		Direction tSecond = secondPerpendicular(tFacingAxis); // the +j axis (:153 the AXIS_Z gate)
		BlockPos tBase = aOrigin;
		for (Direction.Axis tPerp : perpendiculars(tFacingAxis)) {
			tBase = tBase.relative(tPerp, -1); // :147-149 the -1 shift on both non-facing axes
		}
		long rSpent = 0;
		long tCost = aSlab ? SLAB_COST : BLOCK_COST;
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				if (aBudget < tCost) return rSpent; // :156 the budget-out break
				BlockPos tPos = tBase.relative(tFirst, i).relative(tSecond, j);
				FoamPlacement tPlacement = new FoamPlacement(tPos, aSlab, aType);
				if (aSink.place(tPlacement)) { // non-air/failure = skip (the :153-155 if-wrap)
					aBudget -= tCost;
					rSpent += tCost;
				}
			}
		}
		return rSpent;
	}

	/** The two axes perpendicular to the facing axis, in the upstream X-before-Y-before-Z walk order. */
	private static List<Direction.Axis> perpendiculars(Direction.Axis aFacingAxis) {
		List<Direction.Axis> rAxes = new ArrayList<>(2);
		for (Direction.Axis tAxis : Direction.Axis.values()) { // X, Y, Z — the declaration order
			if (tAxis != aFacingAxis) rAxes.add(tAxis);
		}
		return rAxes;
	}

	/** The +i axis: the first perpendicular in X/Y/Z order (:153 the {@code SIDES_AXIS_X ? 0 : i} gate). */
	private static Direction firstPerpendicular(Direction.Axis aFacingAxis) {
		return directionOf(aFacingAxis == Direction.Axis.X ? Direction.Axis.Y : Direction.Axis.X);
	}

	/** The +j axis: the second perpendicular (:153 the {@code SIDES_AXIS_Z ? j : 0} gate). */
	private static Direction secondPerpendicular(Direction.Axis aFacingAxis) {
		return directionOf(aFacingAxis == Direction.Axis.Z ? Direction.Axis.Y : Direction.Axis.Z);
	}

	private static Direction directionOf(Direction.Axis aAxis) {
		return switch (aAxis) {
			case X -> Direction.EAST;
			case Y -> Direction.UP;
			case Z -> Direction.SOUTH;
		};
	}

	/**
	 * The slab-half mapping (the ruling_slab ruling): the upstream {@code mSlabs[OPOS[side]]}
	 * collapses to the vanilla half — UP→BOTTOM, DOWN→TOP, a horizontal side falls to the
	 * hit height (the vanilla SlabBlock.getStateForPlacement rule).
	 */
	public static SlabType halfOf(Direction aSide, float aHitY) {
		if (aSide == Direction.UP) return SlabType.BOTTOM; // OPOS[UP]=BOTTOM — the slab rests on the clicked floor
		if (aSide == Direction.DOWN) return SlabType.TOP; // OPOS[DOWN]=TOP — the slab hangs under the clicked ceiling
		return aHitY < 0.5F ? SlabType.BOTTOM : SlabType.TOP; // the vanilla horizontal-face rule
	}

	/** The plan of a mode over no sink — the truth-table seam (the accepted-order preview). */
	public static List<FoamPlacement> planOf(int aMode, BlockPos aClicked, Direction aFace, Direction aPlayerSide, float aHitY) {
		List<FoamPlacement> rPlan = new ArrayList<>();
		foamArm(aMode, aClicked, aFace, aPlayerSide, aHitY, Long.MAX_VALUE, aPlacement -> {
			rPlan.add(aPlacement);
			return true; // the recording sink — every candidate lands
		});
		return rPlan;
	}
}
