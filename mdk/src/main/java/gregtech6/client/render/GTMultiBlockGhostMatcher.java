package gregtech6.client.render;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import gregtech6.multiblock.GTMultiBlockPattern;

/**
 * The green/red per-cell match classifier of the ghost preview (task
 * p12-ghost-render-match, ADR 2026-09-02-p12-ghost-render-translucent): a pure function
 * from ({@link GTMultiBlockPattern} read-only structure + {@link Level} + controller
 * position + facing byte) to a per-cell three-valued verdict.
 *
 * <p><b>The verdict semantics — the P12 minimal set (the ADR ruling).</b> Per declared
 * cell, one world block is read and pushed through the cell's own predicate — the
 * {@code Cell.matches} seam, the only judgement channel (this class never hardcodes a
 * block id or an {@code isAir} check of its own):
 * <ul>
 * <li>{@link Verdict#RED} — the predicate does not match. Missing blocks and wrong
 *     blocks merge into one red (Litematica's wrongBlock red #4CFF3333 / GTNH MSHP
 *     missing-red family; the P12 ruling deliberately has no separate blue "missing").</li>
 * <li>{@link Verdict#GREEN} — the predicate matches, a structural part in place.</li>
 * <li>{@link Verdict#SKIP} — the predicate matches on a <b>hollow</b> marker (the
 *     must-stay-hollow air pocket, the Coke Oven centre): world air = draw nothing. The
 *     hollow cell declares {@link GTMultiBlockPattern#AIR} as its predicate
 *     (TileEntityCokeOven binding), so riding the predicate reproduces the upstream
 *     "a non-air centre is a check FAILURE" rule exactly (upstream
 *     MultiTileEntityCokeOven :52, port TileEntityCokeOven.java:99-104) — and a non-air
 *     centre classifies RED below, which is why the ghost must paint it.</li>
 * </ul>
 *
 * <p><b>The equality calibre is predicate-level, not full-state.</b> The Coke Oven brick
 * judgement binds {@code state.is(partBlock)} — the upstream checkAndSetTarget
 * part-id+mode pair, never a full BlockState equality (the "wrong state of the right
 * block" yellow grade is cut pool until the first property-level structure lands, the
 * P12 ruling). Extra world blocks the pattern never declared are not classified at all —
 * the verdict list spans exactly {@code pattern.cells()}, never judge-more.
 *
 * <p><b>Unloaded chunks read as VOID_AIR, safely.</b> The per-frame
 * {@code getBlockState} on an ungenerated chunk returns VOID_AIR through
 * {@code ClientChunkCache} :75 (a pure read, proven) — VOID_AIR is an
 * {@code AirBlock} (vanilla Blocks.java:5832), so a part cell classifies RED (the
 * missing semantics) and the hollow centre's AIR predicate matches (SKIP). Computed
 * fresh every frame — 27..125 reads is nothing, no caching (the P12 ruling), and no
 * static mutable field exists here (the P5+P10 constraints, structural in this class:
 * pure static functions over per-frame inputs only). Fusion-grade patterns (~600-1000
 * cells, p12-ghost-upstream-census) are the noted future consumer of a cached mesh on
 * top — not an API change, and not this card.
 */
@OnlyIn(Dist.CLIENT)
public final class GTMultiBlockGhostMatcher {

	/** GREEN = predicate matched (a part in place) / RED = not matched (missing or wrong) / SKIP = matched hollow (draw nothing). */
	public enum Verdict { GREEN, RED, SKIP }

	private GTMultiBlockGhostMatcher() {
	}

	/**
	 * The single-cell primitive: the world block at {@code aWorldPos} through the cell's
	 * predicate. {@code aWorldPos} = controller + {@code pattern.worldOffset(facing, cell)}
	 * (the caller folds the anchor — the renderer already holds it).
	 */
	public static Verdict classifyCell(GTMultiBlockPattern.Cell aCell, Level aLevel, BlockPos aWorldPos) {
		BlockState tState = aLevel.getBlockState(aWorldPos);
		if (!aCell.matches(tState)) return Verdict.RED; // missing and wrong merge — the P12 minimal set
		return aCell.isHollow() ? Verdict.SKIP : Verdict.GREEN;
	}

	/**
	 * The card-shaped aggregate: one verdict per declared cell, aligned with
	 * {@code pattern.cells()} by index (declaration order preserved — the draw order).
	 * Fresh per call, allocation per call, zero statics.
	 */
	public static List<Verdict> classify(GTMultiBlockPattern aPattern, Level aLevel, BlockPos aControllerPos, byte aFacing) {
		List<Verdict> tVerdicts = new ArrayList<>(aPattern.cells().size());
		for (GTMultiBlockPattern.Cell tCell : aPattern.cells()) {
			int[] tOffset = aPattern.worldOffset(aFacing, tCell);
			tVerdicts.add(classifyCell(tCell, aLevel,
					aControllerPos.offset(tOffset[0], tOffset[1], tOffset[2])));
		}
		return tVerdicts;
	}
}
