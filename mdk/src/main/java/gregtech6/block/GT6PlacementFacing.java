package gregtech6.block;

import net.minecraft.core.Direction;

/**
 * The PLACEMENT-FACING CANON (task p28-singleblock-facing-canon): a machine's FRONT at
 * placement points TOWARDS the placer — the OPPOSITE of the player's view direction.
 * The single source both the BlockState seam ({@code getStateForPlacement}) and the
 * BlockEntity seam ({@code setFacingFromPlacement}) of every singleblock family consume,
 * so the client prediction and the server pair-write can never disagree (the P27
 * client-prediction lesson: state and BE must flip through the SAME seam).
 *
 * <p>Upstream evidence chain (task p27-cokeoven-facing-fix, now generalised):
 * {@code getSideForPlayerPlacing} → {@code getHorizontalForPlayerPlacing}
 * (UT.java:1751-1753) over {@code COMPASS_DIRECTIONS = {SIDE_NORTH, SIDE_EAST,
 * SIDE_SOUTH, SIDE_WEST}} (CS.java:638-639): yaw 180 (the view NORTH,
 * {@code Direction.fromYRot} Direction.java:285-287) yields SIDE_SOUTH — the front
 * faces the player standing south. The vanilla furnace idiom
 * ({@code getHorizontalDirection().getOpposite()}, GT6StaticStorages.java:262-265 is
 * the in-repo precedent) is the same mapping. The raw view direction is exactly what
 * {@code getStateForPlacement} used to receive: {@code UseOnContext.getHorizontalDirection}
 * (UseOnContext.java:70-72) is {@code player.getDirection()} — hence the disease this
 * canon replaces was "the front looks WITH the player".
 */
public final class GT6PlacementFacing {

	private GT6PlacementFacing() {}

	/**
	 * The canonical mapping, Direction form — for the BlockState mirrors
	 * ({@code getStateForPlacement}): the front TOWARDS the placer.
	 */
	public static Direction facingTowardsPlacer(Direction aViewDirection) {
		return aViewDirection.getOpposite();
	}

	/**
	 * The canonical mapping, GT6-side byte form (GT6 side order ==
	 * {@code Direction.get3DDataValue()}, Direction.java:119) — for the BlockEntity
	 * NBT-authority mirrors ({@code setFacingFromPlacement}).
	 */
	public static byte placementFacing(Direction aViewDirection) {
		return (byte) facingTowardsPlacer(aViewDirection).get3DDataValue();
	}
}
