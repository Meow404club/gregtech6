/**
 * Copyright (c) 2026 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

package gregtech6.recipes.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import gregapi.oredict.OreDictPrefix;
import gregtech6.recipes.tree.MaterialTreeDisplay.Edge;
import gregtech6.recipes.tree.MaterialTreeDisplay.Node;

/**
 * The material-tree COVERAGE face (task mattree-r2-coverage-display, ADR 2026-10-06
 * mattree-refactor R2 rider) — a pure read over the {@link MaterialTreeDisplay} model: which
 * of the displayed nodes have a KNOWN process path, which are only REACHABLE, and which sit
 * on the tree with NO visible path at all. Zero layout coupling (never calls
 * {@link MaterialTreeLayout#plan} — the plan-independence pin), zero viewer imports, and a
 * determinism contract: same display in, same states out (the display's lists are
 * deterministic order, so the maps/bands iterate identically every time).
 *
 * <p><b>The three states</b> (the ADR names the coverage card but leaves the state set
 * undetailed; this is the minimal DETERMINED three-state design, per-node over the DISPLAYED
 * tree — a {@code prefix} not on the tree has no state at all, {@link #node} returns null):
 * <ul>
 * <li>{@link State#COVERED} — the node has a known process path: at least one DISPLAYED
 *     incoming edge carries its machine ({@code Edge.machine()} non-empty — the producing
 *     map is workstation-tabled and renders the machine icon), or the node is an ENTRY node
 *     ({@code columnOf(prefix) == COL_ORE}, the mined start of the walk — its path is
 *     「mine the world」, no producing edge can exist);</li>
 * <li>{@link State#REACHABLE} — the node is on the tree through displayed incoming edge(s),
 *     but EVERY producing edge is degraded (machine EMPTY — the r11 sluice face: the hop is
 *     real recipe data, the workstation face is not active, the display shows a plain
 *     arrow);</li>
 * <li>{@link State#UNREACHABLE} — the node is displayed but no displayed incoming edge and
 *     no entry column leads to it (its only producers were filtered out of the display — a
 *     visible gap, not a data truth).</li>
 * </ul>
 * The state is LOCAL (the node's own incoming displayed edges), not a transitive
 * reachability analysis — the minimal version; a path-walk face can extend this class
 * without changing the contract. The rule is MONOTONE under the ordering
 * COVERED &gt; REACHABLE &gt; UNREACHABLE: adding a path (an edge, or a node with its
 * edges) can only UPGRADE states, never downgrade — the coverage monotonicity pin.
 *
 * <p><b>Per band</b>: the {@link Band} aggregate counts the band's nodes per state; a
 * band's own {@link Band#state()} is the STRONGEST node state in it (the 「这一带是否存在
 * 已知工艺路径」 reading: green = some node has a machined path, amber = hops exist but all
 * degraded, red = nothing visible) — the aggregate that keeps the monotonicity pin honest
 * when a new node joins a band (worst-of would let a fresh degraded hop DOWNGRADE a covered
 * band; best-of can only rise). The counts carry the finer face. Bands with no nodes are
 * absent (no invented empty-band state). The byproduct band is not a chain band of this
 * material's nodes and carries no coverage (rider① mattree-r2-byproduct-per-step owns that
 * band's face).
 *
 * <p><b>Consumption</b>: the standalone screen computes it once beside the plan
 * ({@code GT6MaterialTreeScreen}) and renders the state as a corner-badge colour per node
 * (the minimal 视觉标注 — interaction/filters are R3's); the query face
 * ({@link #node}, {@link #bands()}) is viewer-neutral for the R3 host legs.
 */
public final class MaterialTreeCoverage {

	/** The coverage states, ordered COVERED &gt; REACHABLE &gt; UNREACHABLE (the monotonicity pin's ranking). */
	public enum State { COVERED, REACHABLE, UNREACHABLE }

	/**
	 * One chain band's coverage: the per-state node counts and the derived
	 * {@link #state()} (the strongest node state in the band).
	 *
	 * @param column      the band's semantic stage ({@link MaterialTreeDisplay#COL_ORE}..{@link MaterialTreeDisplay#COL_DUST})
	 * @param covered     nodes with a known process path
	 * @param reachable   nodes on a degraded path (real data, workstation face inactive)
	 * @param unreachable nodes with no visible path
	 */
	public record Band(int column, int covered, int reachable, int unreachable) {
		/** The band's own state: the STRONGEST node state (monotone — adding paths or nodes never downgrades it). */
		public State state() {
			if (covered > 0) return State.COVERED;
			if (reachable > 0) return State.REACHABLE;
			return State.UNREACHABLE;
		}
	}

	/** The per-node states in the display's node order (insertion-ordered by prefix). */
	private final Map<OreDictPrefix, State> mNodes;
	/** The band aggregates in column order (only bands with at least one node). */
	private final List<Band> mBands;

	private MaterialTreeCoverage(Map<OreDictPrefix, State> aNodes, List<Band> aBands) {
		mNodes = aNodes;
		mBands = aBands;
	}

	/** The one display's coverage (the production face — same as the primitive over its lists). */
	public static MaterialTreeCoverage of(MaterialTreeDisplay aDisplay) {
		return of(aDisplay.nodes(), aDisplay.edges());
	}

	/**
	 * The primitive over the raw node/edge lists (the layout engine's list-trio seam
	 * precedent: tests hand-build degenerate trees without a registry pour). Only edges
	 * whose BOTH endpoints are in {@code aNodes} count — an undisplayed endpoint is not a
	 * visible path.
	 */
	public static MaterialTreeCoverage of(List<Node> aNodes, List<Edge> aEdges) {
		Map<OreDictPrefix, Node> tDisplayed = new LinkedHashMap<>();
		for (Node tNode : aNodes) tDisplayed.put(tNode.prefix(), tNode);
		// the incoming-edge tally: any displayed producer vs a machine-carrying one
		Map<OreDictPrefix, Boolean> tReached = new LinkedHashMap<>();
		Map<OreDictPrefix, Boolean> tMachined = new LinkedHashMap<>();
		for (Edge tEdge : aEdges) {
			if (!tDisplayed.containsKey(tEdge.from()) || !tDisplayed.containsKey(tEdge.to())) continue;
			tReached.put(tEdge.to(), Boolean.TRUE);
			if (!tEdge.machine().isEmpty()) tMachined.put(tEdge.to(), Boolean.TRUE);
		}
		Map<OreDictPrefix, State> rStates = new LinkedHashMap<>();
		// the band tally: [covered, reachable, unreachable] per column, ascending column order (the TreeMap)
		Map<Integer, int[]> tBandCounts = new java.util.TreeMap<>();
		for (Node tNode : aNodes) {
			State tState;
			if (tMachined.containsKey(tNode.prefix())) tState = State.COVERED;
			else if (tReached.containsKey(tNode.prefix())) tState = State.REACHABLE;
			else if (tNode.column() == MaterialTreeDisplay.COL_ORE) tState = State.COVERED; // the mined entry
			else tState = State.UNREACHABLE;
			rStates.put(tNode.prefix(), tState);
			int[] tCounts = tBandCounts.computeIfAbsent(tNode.column(), k -> new int[3]);
			tCounts[tState == State.COVERED ? 0 : tState == State.REACHABLE ? 1 : 2]++;
		}
		List<Band> rBands = new ArrayList<>();
		for (Map.Entry<Integer, int[]> tBand : tBandCounts.entrySet())
			rBands.add(new Band(tBand.getKey(), tBand.getValue()[0], tBand.getValue()[1], tBand.getValue()[2]));
		return new MaterialTreeCoverage(Collections.unmodifiableMap(rStates), List.copyOf(rBands));
	}

	/**
	 * The one node's state ({@code null} = the prefix is not part of this tree — absence is
	 * not {@link State#UNREACHABLE}, it is no state at all).
	 */
	public State node(OreDictPrefix aPrefix) {
		return mNodes.get(aPrefix);
	}

	/** The per-node states, display node order (read-only). */
	public Map<OreDictPrefix, State> nodes() {
		return mNodes;
	}

	/** The band aggregates, column order (read-only; bands without nodes are absent). */
	public List<Band> bands() {
		return mBands;
	}

	/** The one band's aggregate ({@code null} = the band has no nodes on this tree). */
	public Band band(int aColumn) {
		for (Band tBand : mBands) if (tBand.column() == aColumn) return tBand;
		return null;
	}
}
