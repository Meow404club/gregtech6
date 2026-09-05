package gregtech6.multiblock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import gregtech6.tileentity.multiblocks.ITileEntityMultiBlockController;

/**
 * The shared structure checker (task p16-pattern-checker ② — the ADR
 * 2026-09-05-p16-formation-scoping CHECK capability): walks a bound
 * {@link GTMultiBlockPattern} cell by cell and drives the upstream
 * {@code checkAndSetTarget} equivalent path per forming cell — the block judgement,
 * the part-BE binding, and the controller self-cell arm all come from
 * {@link ITileEntityMultiBlockController.Util#checkAndSetTarget}, NOT re-implemented
 * here. One judgement source, zero per-machine hand-written loops for rigid
 * pattern-bound machines (the "same shape written twice" maintenance the Coke Oven
 * carried is what this kills). Mechanism-level clean-room over kTFRU's CHECK mode +
 * {@code failedPos} diagnostics (StructureContext.java:23-61): the traversal is our
 * own immutable pattern, the verdict is our own value type; no kTFRU code or facing
 * tables are transcribed (AGPL).
 *
 * <p><b>The per-cell semantics (one branch per cell kind):</b>
 * <ul>
 * <li><b>forming part</b> ({@code Cell.forms()}) —
 *     {@code Util.checkAndSetTarget(controller, worldCell, cell.partBlock, cell.design,
 *     cell.usage, ...)}: the upstream :47-77 verbatim path, so the wand auto-place,
 *     the occupation arbitration, the design/mode write and the self-cell pass
 *     (the controller's own cell is always a pass, upstream :48-49) are inherited
 *     behaviours, not copies;</li>
 * <li><b>hollow marker</b> — must-be-empty, fail-not-clear (upstream :52, port
 *     TileEntityCokeOven.java:139-144): a non-air cell fails the check and is left
 *     standing (never removed by a CHECK); the air case is the idempotent no-op the
 *     hand-written loops judged with their {@code removeBlock} pair — this walker
 *     writes nothing;</li>
 * <li><b>declaration-only part</b> (no forming expectation — the P12 calibre) — pure
 *     {@code Cell.matches} judgement, no world write, no binding (the boiler's bound
 *     pattern stays display data; its hand-written check remains the server truth).</li>
 * </ul>
 *
 * <p><b>The unloaded guard is the per-cell superset of the four-corner precedent</b>
 * (boiler :289-290 / cokeoven :134-135): every cell's {@code isLoaded} is probed
 * BEFORE any judgement or binding happens — one probe short-circuits the whole walk
 * into the {@link FormedVerdict#unloaded} verdict and the caller keeps its last
 * verdict (upstream :59/:139 {@code return mStructureOkay}). Unlike the hand-written
 * guards this never lets a mid-structure {@code getBlockState} force-load a chunk:
 * an unloaded cell is reported, not loaded.
 *
 * <p><b>The world position of a cell</b> is {@code controller + cellOffset(facing,
 * cell)} — the {@link GTMultiBlockPattern#cellOffset} pure rotation exactly as the
 * client ghost matcher resolves it, so the server check and the ghost preview walk
 * the same cells through the same arithmetic for every facing.
 */
public final class GTMultiBlockStructureChecker {

	/** One failed cell of a {@link FormedVerdict} — the diagnostics the check command surfaces. */
	public static final class FailedCell {

		/** The cell's index into {@code pattern.cells()} (the declaration order). */
		public final int index;
		/** The cell's structure-centre-relative offset. */
		public final int x;
		/** The cell's structure-centre-relative offset. */
		public final int y;
		/** The cell's structure-centre-relative offset. */
		public final int z;
		/** The world cell that failed. */
		public final BlockPos pos;
		/** The human-readable reason (short, RCON-embeddable). */
		public final String reason;

		FailedCell(int aIndex, GTMultiBlockPattern.Cell aCell, BlockPos aPos, String aReason) {
			index = aIndex;
			x = aCell.x; y = aCell.y; z = aCell.z;
			pos = aPos;
			reason = aReason;
		}

		/** The compact one-line form the command output embeds. */
		@Override
		public String toString() {
			return "#" + index + " (" + x + "," + y + "," + z + ")@" + pos.toShortString() + ": " + reason;
		}
	}

	/**
	 * The immutable check verdict: formed + the failed-cell list (empty on success), plus
	 * the unloaded flag — when it is up, {@link #formed} carries no meaning and the caller
	 * keeps its last verdict (the upstream :59/:139 ruling); the list then holds the
	 * not-loaded cells for diagnostics.
	 */
	public static final class FormedVerdict {

		/** Whether every declared cell passed (meaningless when {@link #unloaded}). */
		public final boolean formed;
		/** True = the walk short-circuited on the isLoaded guard before touching anything. */
		public final boolean unloaded;

		private final List<FailedCell> mFailedCells;

		private FormedVerdict(boolean aFormed, boolean aUnloaded, List<FailedCell> aFailedCells) {
			formed = aFormed;
			unloaded = aUnloaded;
			mFailedCells = Collections.unmodifiableList(aFailedCells);
		}

		/** The failed cells in declaration order — immutable, empty when formed. */
		public List<FailedCell> failedCells() {
			return mFailedCells;
		}

		/** The first failure in declaration order, or null when formed/unloaded-clean. */
		@Nullable
		public FailedCell firstFailedCell() {
			return mFailedCells.isEmpty() ? null : mFailedCells.get(0);
		}

		/** The one-line diagnostic the check command embeds after {@code first_failed_cell=}. */
		public String describeFirstFailure() {
			FailedCell tFirst = firstFailedCell();
			return tFirst == null ? "none" : tFirst.toString();
		}
	}

	private GTMultiBlockStructureChecker() {
	}

	/**
	 * The full walk: the controller's bound pattern, the given facing byte (the GT6 side
	 * order — the caller's {@code mFacing}), and the builder-wand triple passed through
	 * to the forming cells verbatim. A controller with no bound pattern is formed by
	 * definition (the interface default — no declaration, no expectation).
	 */
	public static FormedVerdict check(ITileEntityMultiBlockController aController, byte aFacing,
			@Nullable BlockPos aClickedAt, @Nullable Player aPlayer, @Nullable Container aInventory) {
		GTMultiBlockPattern tPattern = aController.getStructurePattern();
		if (tPattern == null) return new FormedVerdict(true, false, new ArrayList<>());

		BlockEntity tSelf = (BlockEntity) aController;
		Level tLevel = tSelf.getLevel();
		if (tLevel == null) return new FormedVerdict(false, false, new ArrayList<>());

		List<GTMultiBlockPattern.Cell> tCells = tPattern.cells();
		BlockPos tControllerPos = tSelf.getBlockPos();

		// the unloaded guard FIRST — no judgement, no binding, no forced chunk load past this point
		List<FailedCell> tFailures = new ArrayList<>();
		for (int i = 0; i < tCells.size(); i++) {
			GTMultiBlockPattern.Cell tCell = tCells.get(i);
			BlockPos tPos = worldCell(tControllerPos, aFacing, tPattern, tCell);
			if (!tLevel.isLoaded(tPos)) {
				tFailures.add(new FailedCell(i, tCell, tPos, "chunk not loaded"));
			}
		}
		if (!tFailures.isEmpty()) return new FormedVerdict(false, true, tFailures);

		// the walk — one branch per cell kind (see the class javadoc)
		for (int i = 0; i < tCells.size(); i++) {
			GTMultiBlockPattern.Cell tCell = tCells.get(i);
			BlockPos tPos = worldCell(tControllerPos, aFacing, tPattern, tCell);
			if (tCell.forms()) {
				// the upstream :47-77 path, verbatim via the Util — wand place, arbitration,
				// design/mode write and the self-cell pass are inherited, not re-coded
				if (!ITileEntityMultiBlockController.Util.checkAndSetTarget(aController,
						tPos.getX(), tPos.getY(), tPos.getZ(),
						tCell.partBlock, tCell.design, tCell.usage, aClickedAt, aPlayer, aInventory)) {
					tFailures.add(new FailedCell(i, tCell, tPos, "part cell failed (missing, wrong block, or foreign claim)"));
				}
			} else if (tCell.isHollow()) {
				// fail-not-clear: a non-air cell fails the check and STAYS (upstream :52); the
				// air case is the no-op the hand-written removeBlock pair judged — no world write
				if (!tLevel.getBlockState(tPos).isAir()) {
					tFailures.add(new FailedCell(i, tCell, tPos, "hollow cell must be air (fail-not-clear)"));
				}
			} else {
				// declaration-only (P12 calibre): predicate judgement, no world write, no binding
				if (!tCell.matches(tLevel.getBlockState(tPos))) {
					tFailures.add(new FailedCell(i, tCell, tPos, "declaration-only cell does not match"));
				}
			}
		}
		return new FormedVerdict(tFailures.isEmpty(), false, tFailures);
	}

	/** The controller-relative world cell — the same {@link GTMultiBlockPattern#cellOffset} arithmetic the ghost matcher walks. */
	private static BlockPos worldCell(BlockPos aControllerPos, byte aFacing, GTMultiBlockPattern aPattern, GTMultiBlockPattern.Cell aCell) {
		int[] tOffset = aPattern.worldOffset(aFacing, aCell);
		return aControllerPos.offset(tOffset[0], tOffset[1], tOffset[2]);
	}
}
