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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import gregapi.oredict.OreDictPrefix;
import gregtech6.recipes.tree.MaterialTreeDisplay.Edge;
import gregtech6.recipes.tree.MaterialTreeDisplay.Node;

/**
 * The material-tree node-graph GEOMETRY (task mattree-v2-nodes; rotated VERTICAL by task
 * mattree-vertical-layout) — the viewer-NEUTRAL helper both legs render from (spec clause ⑦):
 * pure vanilla math over the {@link MaterialTreeDisplay} model, zero JEI/EMI imports, every
 * rect pinned by {@code MaterialTreeLayoutTest}. The JEI category's {@code draw(GuiGraphics)}
 * and the EMI recipe's {@code addDrawable} consumer both translate these rects 1:1 through
 * {@code GuiGraphics.fill(x, y, x+w, y+h, ink)} (1.20.1 GuiGraphics has no diagonal
 * primitive — hLine/vLine/fill only, tmp/vanilla-1.20.1 GuiGraphics.java:121-190 — so all
 * routing is MANHATTAN and the arrowhead is a rasterised solid triangle, spec clause ⑤).
 *
 * <p><b>The rotation (task mattree-vertical-layout)</b> — the coordinate table's semantic
 * map, restated from the layout's point of view (Display javadoc carries the full table):
 * chain STAGES grow DOWNWARD (the user ruling 「最好是向下的，jei/emi是向下空间比较大」),
 * so every pre-rotation left→right hop is now a top→bottom hop. The edge budget rotated
 * with it: the machine icon rides the 22 px gap UNDER the from band (2 px entry wire +
 * 16 px icon + 4 px arrowhead), the wire exits the from slot's bottom-edge centre, runs a
 * shared HORIZONTAL TRUNK along the slot's bottom edge (the fan-out trunk, circuit-diagram
 * style), bends vertically into the machine icon's top edge, crosses under it and bends
 * again at the arrow's back line before entering the target from above. Same-band-hop
 * edges whose midpoint-x collide (same from band AND same midpoint lane) stagger the
 * machine icon and its run 2 px per collision, now HORIZONTALLY. Parallel nodes spread
 * RIGHT within a band at {@link MaterialTreeDisplay#LANE_PITCH} 28 — the old vertical
 * ROW_PITCH 20 (2 px gaps) was the 「挤的太紧」 root cause the rotation retires.
 *
 * <p><b>The three-part edge</b> (spec clause ②): every chain hop renders as
 * 「from slot –wire– [machine icon] –arrow↓ to slot」.
 *
 * <p><b>Degraded edges</b> ({@code Edge.machine()} EMPTY — an untabled map, offline always):
 * same Manhattan path minus the machine box, plus the v1-style via-label position so the
 * machine NAMES survive as text (the 降级纯箭头+文字 clause; live clients never hit it —
 * the census test pins {@link MaterialTreeWorkstations} against 100% of the displayed
 * mapNames).
 *
 * <p>Z-order is the viewers' contract: JEI draws in the category background layer (under
 * slots) and the EMI leg must add the wire widgets BEFORE the slot widgets — a wire crossing
 * an intermediate band's slot hides behind the slot box in both legs.
 *
 * <p><b>The layout engine (task mattree-r2-layout-engine, ADR 2026-10-06-mattree-refactor
 * L1)</b> — the coordinate math above is now a pure function
 * {@code plan(tree) -> Result}: the input is the tree data, the output the node coordinates,
 * the edge plans and the CONTENT BOUNDING BOX (canvas size computed from what the tree
 * uses — the old fixed 202x206 was its worst case, kept as the viewer pages' canvas).
 * The EMI recipe-tree screen is the REFERENCE skeleton, not a replica (the user ruling
 * 「学习 EMI 的配方树…参考级非复刻级」); what was learned and how it was adapted:
 * <ul>
 * <li><b>content-driven extents</b>: EMI aggregates subtree extents bottom-up into
 *     per-depth {@code TreeVolume} left/right pairs, siblings spaced so the extents never
 *     overlap at any shared depth (tmp/harvest/emi-1.20.1-src BoMScreen.java:788-867, the
 *     merge {@code addToRight} at :841-857); our tree is already LAYERED into the semantic
 *     stage bands, so the recursion degenerates into a per-band aggregation —
 *     {@code lanes = max(row+1)} over the chain bands and the byproduct band, aggregated
 *     into the canvas width, the deepest used band into the height.</li>
 * <li><b>spacing constants in one place</b>: EMI pins NODE_WIDTH/NODE_HORIZONTAL_SPACING/
 *     NODE_VERTICAL_SPACING as the screen's layout constants (BoMScreen.java:61-64) — our
 *     constants live in {@link Policy} (the horizontal axis + the canvas margins; the
 *     vertical band pitch stays the edge-budget contract below).</li>
 * <li><b>the content box feeds the pan/zoom clamp</b>: EMI derives the pan bounds from the
 *     computed content size (BoMScreen.java:202-208) — our equivalent seam is
 *     {@code Result.width()/height()} flowing into the
 *     {@link MaterialTreeViewport} fit constructor (the R1 L2 handoff: the standalone
 *     screen now fits the INK box, not the worst-case canvas — the 「墨迹级居中」 leftover
 *     of mattree-viewport-fit).</li>
 * <li><b>what EMI lacks, ours adapts</b>: the byproduct band rides OUTSIDE the main chain
 *     axis (its presence/count never moves a chain node — pinned), and multi-source
 *     coverage chains CONVERGE on the shared target node (EMI forces single-producer
 *     resolution, BoMScreen.java:319-323; we keep N sources merging on one node —
 *     EMI's own same-material merge face, minus the fold).</li>
 * </ul>
 *
 * <p><b>旧钉迁移声明</b>: the coordinate functions on {@link MaterialTreeDisplay}
 * ({@code nodeX/nodeY/byproductX/byproductY/overflowX/overflowY/stageY}) and the fixed
 * 202x206 canvas survive as the VIEWER PAGES' legacy view (the static fixed-grid table,
 * the EMI/JEI page legs' published geometry); the engine face is {@link Result}. Under
 * {@link Policy#DEFAULT} the two are numerically identical — pinned by
 * {@code MaterialTreeLayoutTest}, and {@link #layout(MaterialTreeDisplay)} is now a
 * DEFAULT-policy alias of {@link Result#edges()} so the page legs consume the same math.
 * The standalone screen consumes {@link Result} directly.
 */
public final class MaterialTreeLayout {

	/** The standard slot square (item nodes ride 18x18 boxes). */
	public static final int SLOT = 18;
	/** The machine-icon node: a bare 16x16 item icon (no slot background — both viewers mount it background-free). */
	public static final int MACHINE = 16;
	/** The wire gap UNDER each band ({@link MaterialTreeDisplay#STAGE_PITCH} - {@link #SLOT}). */
	public static final int GAP = MaterialTreeDisplay.STAGE_PITCH - SLOT;
	/** Arrowhead extent in the travel direction; since the rotation the solid triangle is {@value #ARROW_HEAD} px tall, 7 px wide. */
	public static final int ARROW_HEAD = 4;
	/**
	 * The gap's exact vertical budget (22 px = 2 wire + {@value #MACHINE} machine + 4 arrow):
	 * the machine icon packs TOP against the entry-wire zone so the arrowhead's 4 px fit
	 * flush before the target slot.
	 */
	public static final int ENTRY_WIRE = GAP - MACHINE - ARROW_HEAD;
	/** The wire ink (the v1 via-label gray, NEI's plain 0xFF555555). */
	public static final int WIRE_INK = 0xFF555555;
	/** The arrowhead ink (one step darker so heads read against shared trunks). */
	public static final int ARROW_INK = 0xFF2A2A2A;
	/** Vertical stagger per colliding same-gap midpoint (spec clause ⑥'s 2-3 px, picked 2). */
	public static final int DUP_STAGGER = 2;

	/** One filled rectangle in canvas coordinates ({@code fill(x, y, x+w, y+h, ink)}). */
	public record Rect(int x, int y, int w, int h) {}

	/**
	 * The unified icon/label pose (task mattree-item-zoom-pose, the 三腿统一变换原语 clause):
	 * the translate+scale pair a GuiGraphics pose stack consumes, so an icon or a label
	 * renders at the SAME scale the rect fills do — the origin is the viewport's apply, the
	 * factor its live scale. Pure ({@code java.lang} only, the tree package's viewer-neutrality
	 * holds); the GuiGraphics mounting is the consumers' shared
	 * {@code GT6MaterialTreeScreen.runAtPose} — 1.20.1 GuiGraphics renderItem rides the pose
	 * stack itself (tmp/vanilla-1.20.1 GuiGraphics.java:480-485) and drawString hands it to
	 * drawInBatch (:268).
	 */
	public record Pose(double x, double y, double scale) {}

	/**
	 * The pose of one tree point through the viewport — the ONE constructor of icon/label
	 * transforms: callers never assemble a translate+scale pair by hand (the 禁腿内私有数学
	 * redline), so a scaled icon cannot drift against the box fills that share its corners.
	 */
	public static Pose pose(MaterialTreeViewport aView, double aTreeX, double aTreeY) {
		MaterialTreeViewport.Point tOrigin = aView.apply(aTreeX, aTreeY);
		return new Pose(tOrigin.x(), tOrigin.y(), aView.scale());
	}

	/**
	 * One edge's render plan, aligned by index with {@link MaterialTreeDisplay#edges()}:
	 * {@code machine} is the 16x16 bare-icon box (null on the degraded face; both viewers
	 * mount the stack background-free ON it — JEI a RENDER_ONLY slot with no background, EMI
	 * a {@code drawBack(false)} SlotWidget — and both hover-boxes extend one px around it),
	 * {@code wire} the Manhattan segments, {@code arrow} the solid-triangle columns with the
	 * tip on the target slot's top edge, {@code labelX/labelY} the degraded via-label spot.
	 */
	public record EdgeLayout(Rect machine, List<Rect> wire, List<Rect> arrow, int labelX, int labelY) {}

	/**
	 * The one edge's geometry between two slot top-lefts. {@code aMachine} false renders the
	 * degraded face; {@code aDup} is the collision index among same-gap same-midpoint edges
	 * (0 for the first). Pure — the pin tests call this directly with hand-picked coords.
	 */
	public static EdgeLayout edge(int aFromX, int aFromY, int aToX, int aToY, boolean aMachine, int aDup) {
		int fcx = aFromX + SLOT / 2, tcx = aToX + SLOT / 2;
		int mid = (aFromX + aToX) / 2;
		int my = aFromY + SLOT + ENTRY_WIRE; // 16px machine icon top-packed in the gap below the from band
		// the machine rides the CENTRE-LINE midpoint (mid + 9 = (fcx + tcx) / 2) so a same-lane
		// edge stays one straight line (mcx == fcx == tcx); the stagger jogs it +2 px per dup
		int mx = mid + SLOT / 2 - MACHINE / 2 + aDup * DUP_STAGGER;
		int mcx = mx + MACHINE / 2;
		int trunkY = aFromY + SLOT + 1; // the shared fan-out trunk along the from slot's bottom edge
		int backY = aToY - ARROW_HEAD;  // the arrow's back line — the target-side bend sits on it

		List<Rect> rWire = new ArrayList<>();
		if (aMachine) {
			if (mcx != fcx) {
				add(rWire, v(fcx, aFromY + SLOT, trunkY));
				add(rWire, h(fcx, mcx, trunkY));
				add(rWire, v(mcx, trunkY, my));
			} else {
				add(rWire, v(fcx, aFromY + SLOT, my));
			}
			if (mcx != tcx) {
				add(rWire, v(mcx, my + MACHINE, backY));
				add(rWire, h(mcx, tcx, backY));
			} else {
				add(rWire, v(mcx, my + MACHINE, backY));
			}
		} else if (fcx != tcx) {
			add(rWire, v(fcx, aFromY + SLOT, trunkY));
			add(rWire, h(fcx, tcx, trunkY));
			add(rWire, v(tcx, trunkY, backY));
		} else {
			add(rWire, v(fcx, aFromY + SLOT, backY));
		}
		return new EdgeLayout(aMachine ? new Rect(mx, my, MACHINE, MACHINE) : null,
				List.copyOf(rWire), arrowhead(tcx, aToY), mid - 4, aFromY + SLOT - 2);
	}

	/**
	 * The whole display's per-edge plans (same order as {@link MaterialTreeDisplay#edges()}),
	 * with the collision indices computed off the display's own deterministic edge order —
	 * the DEFAULT-policy {@link Result#edges()} alias (the viewer pages' entry point).
	 */
	public static List<EdgeLayout> layout(MaterialTreeDisplay aDisplay) {
		return plan(aDisplay).edges();
	}

	// ------------------------------------------------------------------
	// the layout engine (task mattree-r2-layout-engine, ADR L1):
	// tree data in -> node coordinates + edge plans + bounding box out
	// ------------------------------------------------------------------

	/**
	 * The spacing constants in ONE place (the 间距常量 the EMI reference screen pins as
	 * NODE_WIDTH/NODE_HORIZONTAL_SPACING/NODE_VERTICAL_SPACING, BoMScreen.java:61-64): the
	 * horizontal lane axis and the canvas margins. Changing one field re-flows the whole
	 * tree (the ADR acceptance 间距改一处全树生效). The VERTICAL band pitch is deliberately
	 * NOT a policy knob: the gap under each band is budgeted byte-exact
	 * (2 wire + 16 machine + 4 arrow, {@link #ENTRY_WIRE}) — it is an edge-geometry
	 * contract carried by {@link MaterialTreeDisplay#STAGE_PITCH}, not a free value.
	 *
	 * @param laneX0       x of the first lane (parallel nodes spread RIGHT from here)
	 * @param lanePitch    the lane pitch; the no-overlap contract needs {@code >= SLOT}
	 * @param rightMargin  canvas margin right of the last lane's slot
	 * @param bottomMargin canvas margin below the last band's slot row
	 */
	public record Policy(int laneX0, int lanePitch, int rightMargin, int bottomMargin) {
		/** The shipped values — numerically identical to the legacy fixed-grid statics. */
		public static final Policy DEFAULT = new Policy(
				MaterialTreeDisplay.LANE_X0, MaterialTreeDisplay.LANE_PITCH, 12, 12);
	}

	/**
	 * The one display's layout under {@link Policy#DEFAULT}: node coordinates, edge plans
	 * and the content bounding box, all pure functions of the tree data (same input, same
	 * output — the display's lists are deterministic order).
	 */
	public static Result plan(MaterialTreeDisplay aDisplay) {
		return plan(aDisplay.nodes(), aDisplay.edges(), aDisplay.byproducts());
	}

	/**
	 * The primitive plan over the tree data's raw lists (the degenerate-case seam: tests
	 * can lay out a hand-built tree without a registry pour). Guarded: an empty tree lays
	 * out as a single empty lane rather than a negative-width canvas.
	 */
	public static Result plan(List<MaterialTreeDisplay.Node> aNodes, List<MaterialTreeDisplay.Edge> aEdges, List<MaterialTreeDisplay.Byproduct> aByproducts) {
		return plan(aNodes, aEdges, aByproducts, Policy.DEFAULT);
	}

	/** The {@link #plan(List, List, List) primitive plan} under an explicit {@link Policy} — the 间距改一处全树生效 seam. */
	public static Result plan(List<MaterialTreeDisplay.Node> aNodes, List<MaterialTreeDisplay.Edge> aEdges, List<MaterialTreeDisplay.Byproduct> aByproducts, Policy aPolicy) {
		return new Result(aPolicy, aNodes, aEdges, aByproducts);
	}

	/**
	 * One tree's geometry: the bounding box, the node/byproduct/overflow coordinate
	 * functions and the per-edge plans. Immutable snapshot — the consumers (the standalone
	 * screen's render and hit test, the R1 viewport's content box) read it freely.
	 */
	public static final class Result {
		private final Policy mPolicy;
		private final List<MaterialTreeDisplay.Node> mNodes;
		private final List<MaterialTreeDisplay.Edge> mEdges;
		private final int mLanes;
		private final int mLastBand;
		private final boolean mByproductBand;
		private final int mWidth, mHeight;
		private final List<EdgeLayout> mEdgeLayouts;

		private Result(Policy aPolicy, List<MaterialTreeDisplay.Node> aNodes, List<MaterialTreeDisplay.Edge> aEdges, List<MaterialTreeDisplay.Byproduct> aByproducts) {
			mPolicy = aPolicy;
			mNodes = aNodes;
			mEdges = aEdges;
			// the extent aggregation (the EMI TreeVolume face, degenerated to the band grid):
			// lanes = max(row+1) over the chain bands, then the byproduct band's slots — the
			// widest extent wins the canvas width
			int tLanes = 1;
			int tLastBand = -1;
			for (MaterialTreeDisplay.Node tNode : aNodes) {
				tLanes = Math.max(tLanes, tNode.row() + 1);
				tLastBand = Math.max(tLastBand, tNode.column());
			}
			tLanes = Math.max(tLanes, aByproducts.size());
			mByproductBand = !aByproducts.isEmpty();
			if (tLastBand < 0) tLastBand = 0; // the empty-tree guard: one empty band
			mLanes = tLanes;
			mLastBand = tLastBand;
			mWidth = aPolicy.laneX0() + (mLanes - 1) * aPolicy.lanePitch() + SLOT + aPolicy.rightMargin();
			mHeight = byproductBand() ? stageY(MaterialTreeDisplay.COL_BYPRODUCT) + SLOT + aPolicy.bottomMargin()
					: stageY(tLastBand) + SLOT + aPolicy.bottomMargin();
			// the per-edge plans off this result's own coordinates (the deterministic edge
			// order the dup-key collision rule rides)
			Map<OreDictPrefix, MaterialTreeDisplay.Node> tByPrefix = new LinkedHashMap<>();
			for (MaterialTreeDisplay.Node tNode : aNodes) tByPrefix.put(tNode.prefix(), tNode);
			Map<Long, Integer> tDups = new LinkedHashMap<>();
			List<EdgeLayout> rLayouts = new ArrayList<>();
			for (MaterialTreeDisplay.Edge tEdge : aEdges) {
				MaterialTreeDisplay.Node tFrom = tByPrefix.get(tEdge.from()), tTo = tByPrefix.get(tEdge.to());
				if (tFrom == null || tTo == null) continue; // defensive; the assembly only emits displayed endpoints
				long tKey = ((long) nodeY(tFrom) << 32) | (nodeX(tFrom) + nodeX(tTo)) / 2;
				int tDup = tDups.getOrDefault(tKey, 0);
				tDups.put(tKey, tDup + 1);
				rLayouts.add(edge(nodeX(tFrom), nodeY(tFrom), nodeX(tTo), nodeY(tTo), !tEdge.machine().isEmpty(), tDup));
			}
			mEdgeLayouts = List.copyOf(rLayouts);
		}

		/** The content bounding box width (lanes at the policy pitch + one slot + the right margin). */
		public int width() { return mWidth; }

		/** The content bounding box height (the deepest used band's slot row + one slot + the bottom margin; the byproduct band, when present, is the deepest). */
		public int height() { return mHeight; }

		/** Whether the byproduct band (the hanging face OUTSIDE the main chain axis) is part of this layout. */
		public boolean byproductBand() { return mByproductBand; }

		/** x of a node's slot (the policy lane axis: lanes grow RIGHT). */
		public int nodeX(MaterialTreeDisplay.Node aNode) { return mPolicy.laneX0() + aNode.row() * mPolicy.lanePitch(); }

		/** y of a node's slot (the semantic stage band: stages grow DOWN — the model's rotation table). */
		public int nodeY(MaterialTreeDisplay.Node aNode) { return stageY(aNode.column()); }

		/** y of a stage band's slot row. */
		public int stageY(int aStage) { return MaterialTreeDisplay.STAGE_Y[aStage]; }

		/** x of a byproduct slot (lane aIndex of the hanging bottom band). */
		public int byproductX(int aIndex) { return mPolicy.laneX0() + aIndex * mPolicy.lanePitch(); }

		/** y of the byproduct band's slot row. */
		public int byproductY() { return stageY(MaterialTreeDisplay.COL_BYPRODUCT); }

		/** y of the byproduct band header (the wire-free gap just above the band). */
		public int byproductHeaderY() { return byproductY() - 11; }

		/** x of a stage band's "+N" overflow marker (under the band's first lane). */
		public int overflowX() { return mPolicy.laneX0(); }

		/** y of a stage band's "+N" overflow marker (the explicit-not-silent clause, per band). */
		public int overflowY(int aStage) { return stageY(aStage) + 20; }

		/** The per-edge render plans (same order as the edge list this result was planned from). */
		public List<EdgeLayout> edges() { return mEdgeLayouts; }

		/** The node list this result was planned from (the consumers iterate it for slots). */
		public List<MaterialTreeDisplay.Node> nodes() { return mNodes; }

		/** The edge list this result was planned from. */
		public List<MaterialTreeDisplay.Edge> treeEdges() { return mEdges; }
	}

	/** The solid down-pointing triangle: tip on the target slot's top edge centre, 7 px wide, {@value #ARROW_HEAD} px tall, rasterised as fill columns. */
	public static List<Rect> arrowhead(int aTipX, int aTipY) {
		List<Rect> rCols = new ArrayList<>(7);
		for (int dx = -(ARROW_HEAD - 1); dx < ARROW_HEAD; dx++) {
			int tHeight = ARROW_HEAD - Math.abs(dx);
			rCols.add(new Rect(aTipX + dx, aTipY - tHeight, 1, tHeight));
		}
		return rCols;
	}

	private static Rect h(int aX1, int aX2, int aY) { return aX2 != aX1 ? new Rect(Math.min(aX1, aX2), aY, Math.abs(aX2 - aX1), 1) : null; }
	private static Rect v(int aX, int aY1, int aY2) { return aY2 != aY1 ? new Rect(aX, Math.min(aY1, aY2), 1, Math.abs(aY2 - aY1)) : null; }
	private static void add(List<Rect> aRects, Rect aRect) { if (aRect != null) aRects.add(aRect); }

	private MaterialTreeLayout() {
	}
}
