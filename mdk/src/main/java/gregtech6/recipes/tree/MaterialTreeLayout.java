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
 * The material-tree node-graph GEOMETRY — the viewer-NEUTRAL helper both legs render from:
 * pure vanilla math over the {@link MaterialTreeDisplay} model, zero JEI/EMI imports, every
 * rect pinned by {@code MaterialTreeLayoutTest}.
 *
 * <p><b>The EMI recipe-tree form</b> (task mattree-emi-tree-rework, the user ruling
 * 「排版参考EMI的配方树」): the geometry is item-for-item the EMI recipe tree's, taken off the
 * harvest sources (tmp/harvest/emi-1.20.1-src BoMScreen.java; the 1.21.1 generation is
 * byte-identical on every face cited, BoMScreen.java:63-65/:844):
 * <ul>
 * <li><b>depth-band layering</b>: band = the BFS layer, y = band * pitch (EMI's
 *     {@code depth * NODE_VERTICAL_SPACING}, BoMScreen.java:334/:337 — the addNewNodes
 *     recursion lays children one row below the parent);</li>
 * <li><b>lane spread</b>: siblings on a band step right one lane (EMI's sibling spread,
 *     BoMScreen.java:841-857 {@code addToRight} — extents never overlap; ours spaces
 *     uniform lane centres, the layered-DAG reading);</li>
 * <li><b>the node form</b>: process nodes are the EMI recipe-node BOX — a 42x20 outline
 *     carrying the machine icon (EMI's category icon, BoMScreen.java:719-723) beside the
 *     item icon (:728); the ore roots are EMI's LEAF form — the bare item icon, no box
 *     (EMI :662-663 {@code width = 16});</li>
 * <li><b>the edge routing</b>: two MANHATTAN segments, EMI-exact — a vertical drop at the
 *     target's centre x from the rail into its box top, and one horizontal rail just below
 *     the source box's bottom edge (EMI Node.render :675-694: {@code drawLine(nx, ny-11,
 *     nx, py+off)} + {@code drawLine(px, py+off, nx, py+off)}), white ink (EMI drawLine
 *     fills {@code 0xFFFFFFFF}, :340-350); same-band hops route as one horizontal between
 *     the facing box edges (no EMI analog — ADR: our DAG grows sideways where EMI's tree
 *     cannot);</li>
 * <li><b>what EMI has and ours deliberately does not</b>: arrowheads and edge-mounted
 *     machine icons and via-labels are RETIRED — EMI marks flow with the bare two-segment
 *     line and carries the producing recipe as the box's icon + tooltip (:615-647 the
 *     RecipeTooltipComponent face); our via-labels ride the producing node's tooltip the
 *     same way.</li>
 * </ul>
 *
 * <p><b>The viewport seam</b>: the content box ({@code Result.width/height}) feeds the
 * {@link MaterialTreeViewport} fit constructor (the EMI pan-clamp analog, BoMScreen.java
 * :202-208 derives its bounds from the content); the fit/reset/zoom semantics stay the R1
 * viewport's — EMI's integer window-relative steps (:352-361) are its full-screen form,
 * ours the bounded-page one (ADR, unchanged this task).
 *
 * <p><b>旧钉迁移声明</b> (task mattree-emi-tree-rework): the v2 three-part edge (machine icon
 * on the wire + rasterised arrowhead + via-label spot), the per-band {@code MAX_ROWS} caps,
 * the {@code Overflow} markers, the collision stagger and the closed {@code COLUMN_TABLE}
 * stage map all died with the EMI-form rework — the replacement pins are the two-segment
 * wire plans, the node-box/cell geometry and the depth-band coordinate map in
 * {@code MaterialTreeLayoutTest}.
 */
public final class MaterialTreeLayout {

	/** The standard slot square (item nodes ride 18x18 boxes). */
	public static final int SLOT = 18;
	/** The machine-icon node: a bare 16x16 item icon (EMI's in-box category icon size, BoMScreen.java:719-728). */
	public static final int MACHINE = 16;
	/** The wire ink — EMI's white line (BoMScreen.java:340-350 {@code drawLine} fills 0xFFFFFFFF). */
	public static final int WIRE_INK = 0xFFFFFFFF;

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
	 * the EMI two-segment wire (or the single same-band horizontal). EMI carries no
	 * arrowhead, no edge machine box and no edge label — the flow marking lives in the
	 * node form (BoMScreen.java:675-694 is the whole connector).
	 */
	public record EdgeLayout(List<Rect> wire) {}

	/**
	 * The spacing constants in ONE place (the EMI screen pins its three spacing constants
	 * as BoMScreen.java:61-64 — the same seat): the lane axis, the band axis and the canvas
	 * margins. Changing one field re-flows the whole tree.
	 *
	 * @param laneX0       x CENTRE of the first lane (the box's left edge lands on 4)
	 * @param lanePitch    the lane pitch; the no-overlap contract needs {@code >= BOX_WIDTH}
	 * @param bandPitch    the depth-band pitch (the EMI NODE_VERTICAL_SPACING seat; ADR: 28,
	 *                     not EMI's 20 — see the class javadoc)
	 * @param rightMargin  canvas margin right of the last box
	 * @param bottomMargin canvas margin below the last band's box
	 */
	public record Policy(int laneX0, int lanePitch, int bandPitch, int rightMargin, int bottomMargin) {
		/** The shipped values — the {@link MaterialTreeDisplay} constants. */
		public static final Policy DEFAULT = new Policy(
				MaterialTreeDisplay.LANE_X0, MaterialTreeDisplay.LANE_PITCH, MaterialTreeDisplay.BAND_PITCH, 12, 12);
	}

	/**
	 * The byproduct band's VIEW STATE (task mattree-r2-byproduct-per-step, ADR 2026-10-06
	 * mattree-refactor R2 rider): the 逐步/逐带 REVEAL face over {@link MaterialTreeDisplay.Byproduct#step()}.
	 * Pure order-preserving filter — the same state over the same tree always yields the same
	 * visible set (the determinism pin), the filter never touches the chain axis (the root
	 * card's 副产不挪主轴钉 holds under EVERY state), and the hanging band's content follows
	 * the visible set alone:
	 * <ul>
	 * <li>{@link #ALL} — the aggregate face, the full band (the EMI/JEI page legs' face);</li>
	 * <li>{@link #step(int)} — 「只看某一步的副产」: exactly the {@code step == n} slots;</li>
	 * <li>{@link #cumulative(int)} — 「累积到某步」: every {@code 0 <= step <= n} slot, the
	 *     reveal growing monotonically with n (the content-box monotone pin).</li>
	 * </ul>
	 */
	public record ByproductView(Mode mode, int step) {
		/** The reveal modes. */
		public enum Mode { ALL, STEP, CUMULATIVE }

		/** The aggregate face (also the default — {@link #plan(MaterialTreeDisplay)} rides it). */
		public static final ByproductView ALL = new ByproductView(Mode.ALL, -1);

		/** 「只看某一步的副产」: the step-n slots alone. */
		public static ByproductView step(int aStep) { return new ByproductView(Mode.STEP, aStep); }

		/** 「累积到某步」: every slot of step 0..aStep. */
		public static ByproductView cumulative(int aStep) { return new ByproductView(Mode.CUMULATIVE, aStep); }

		/** Whether this state shows the slot. */
		public boolean shows(MaterialTreeDisplay.Byproduct aByproduct) {
			return switch (mode) {
				case ALL -> true;
				case STEP -> aByproduct.step() == step;
				case CUMULATIVE -> aByproduct.step() >= 0 && aByproduct.step() <= step;
			};
		}

		/** The visible subset in the band's own order (ALL returns the list itself — the zero-filter face). */
		public List<MaterialTreeDisplay.Byproduct> apply(List<MaterialTreeDisplay.Byproduct> aByproducts) {
			if (mode == Mode.ALL) return aByproducts;
			List<MaterialTreeDisplay.Byproduct> rVisible = new ArrayList<>(aByproducts.size());
			for (MaterialTreeDisplay.Byproduct tByproduct : aByproducts) if (shows(tByproduct)) rVisible.add(tByproduct);
			return List.copyOf(rVisible);
		}
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
	 * The one display's layout under a byproduct VIEW STATE (task mattree-r2-byproduct-per-step):
	 * the view filters only the hanging band's slots — the chain nodes and edge plans are the
	 * ALL face's, byte for byte, under every state.
	 */
	public static Result plan(MaterialTreeDisplay aDisplay, ByproductView aView) {
		return plan(aDisplay.nodes(), aDisplay.edges(), aView.apply(aDisplay.byproducts()));
	}

	/**
	 * The primitive plan over the tree data's raw lists (the degenerate-case seam: tests
	 * can lay out a hand-built tree without a registry pour). Guarded: an empty tree lays
	 * out as a single empty band rather than a negative-width canvas.
	 */
	public static Result plan(List<MaterialTreeDisplay.Node> aNodes, List<MaterialTreeDisplay.Edge> aEdges, List<MaterialTreeDisplay.Byproduct> aByproducts) {
		return plan(aNodes, aEdges, aByproducts, Policy.DEFAULT);
	}

	/** The {@link #plan(List, List, List) primitive plan} under an explicit {@link Policy} — the 间距改一处全树生效 seam. */
	public static Result plan(List<MaterialTreeDisplay.Node> aNodes, List<MaterialTreeDisplay.Edge> aEdges, List<MaterialTreeDisplay.Byproduct> aByproducts, Policy aPolicy) {
		return new Result(aPolicy, aNodes, aEdges, aByproducts);
	}

	/**
	 * One tree's geometry: the bounding box, the node-box/cell coordinate functions and the
	 * per-edge wire plans. Immutable snapshot — the consumers (the standalone screen's
	 * render and hit test, the R1 viewport's content box) read it freely.
	 */
	public static final class Result {
		private final Policy mPolicy;
		private final List<MaterialTreeDisplay.Node> mNodes;
		private final List<MaterialTreeDisplay.Edge> mEdges;
		private final int mLanes;
		private final int mLastBand;
		private final int mByproductY;
		private final int mWidth, mHeight;
		private final List<EdgeLayout> mEdgeLayouts;
		private final Map<OreDictPrefix, Rect> mMachineCells;

		private Result(Policy aPolicy, List<MaterialTreeDisplay.Node> aNodes, List<MaterialTreeDisplay.Edge> aEdges, List<MaterialTreeDisplay.Byproduct> aByproducts) {
			mPolicy = aPolicy;
			mNodes = aNodes;
			mEdges = aEdges;
			// the extent aggregation (the EMI TreeVolume face, degenerated to the band grid):
			// lanes = max(row+1) over the bands, aggregated into the canvas width; the deepest
			// used band, into the height (plus the hanging byproduct band when present)
			int tLanes = 1;
			int tLastBand = -1;
			for (MaterialTreeDisplay.Node tNode : aNodes) {
				tLanes = Math.max(tLanes, tNode.row() + 1);
				tLastBand = Math.max(tLastBand, tNode.band());
			}
			tLanes = Math.max(tLanes, aByproducts.size()); // the hanging band's slots are lanes too
			if (tLastBand < 0) tLastBand = 0; // the empty-tree guard: one empty band
			mLanes = tLanes;
			mLastBand = tLastBand;
			mByproductY = aByproducts.isEmpty() ? -1 : bandY(tLastBand + 1);
			mWidth = aPolicy.laneX0() + (mLanes - 1) * aPolicy.lanePitch() + MaterialTreeDisplay.BOX_WIDTH / 2 + aPolicy.rightMargin();
			mHeight = (mByproductY >= 0 ? mByproductY : bandY(tLastBand)) + MaterialTreeDisplay.BOX_HEIGHT / 2 + aPolicy.bottomMargin();

			// the node-coordinate face the edge plans ride (insertion order = the display's)
			Map<OreDictPrefix, MaterialTreeDisplay.Node> tByPrefix = new LinkedHashMap<>();
			for (MaterialTreeDisplay.Node tNode : aNodes) tByPrefix.put(tNode.prefix(), tNode);

			// the machine cell per node: the EMI recipe-node's category-icon seat — the FIRST
			// producing edge (the display's edge order) that resolved a machine stack
			mMachineCells = new LinkedHashMap<>();
			for (MaterialTreeDisplay.Edge tEdge : aEdges) {
				MaterialTreeDisplay.Node tTo = tByPrefix.get(tEdge.to());
				if (tTo == null || tEdge.machine().isEmpty() || mMachineCells.containsKey(tTo.prefix())) continue;
				mMachineCells.put(tTo.prefix(), machineCell(tTo));
			}

			// the per-edge two-segment wire plans (the EMI connector, same edge order)
			List<EdgeLayout> rLayouts = new ArrayList<>();
			for (MaterialTreeDisplay.Edge tEdge : aEdges) {
				MaterialTreeDisplay.Node tFrom = tByPrefix.get(tEdge.from()), tTo = tByPrefix.get(tEdge.to());
				if (tFrom == null || tTo == null) continue; // defensive; the assembly only emits displayed endpoints
				rLayouts.add(new EdgeLayout(edgeWire(tFrom, tTo)));
			}
			mEdgeLayouts = List.copyOf(rLayouts);
		}

		/** The content bounding box width (lane centres at the policy pitch + one box + the right margin). */
		public int width() { return mWidth; }

		/** The content bounding box height (the deepest band's box + the bottom margin; the byproduct band, when present, is the deepest). */
		public int height() { return mHeight; }

		/** Whether the byproduct band (the hanging legend face OUTSIDE the main chain axis) is part of this layout. */
		public boolean byproductBand() { return mByproductY >= 0; }

		/** y centre of a depth band (the EMI layer axis: y = bandY0 + band * pitch). */
		public int bandY(int aBand) { return MaterialTreeDisplay.BAND_Y0 + aBand * mPolicy.bandPitch(); }

		/** x CENTRE of a node (the lane axis: lanes grow RIGHT). */
		public int nodeX(MaterialTreeDisplay.Node aNode) { return mPolicy.laneX0() + aNode.row() * mPolicy.lanePitch(); }

		/** y CENTRE of a node (its depth band). */
		public int nodeY(MaterialTreeDisplay.Node aNode) { return bandY(aNode.band()); }

		/**
		 * The process-node BOX (the EMI recipe-node outline, BoMScreen.java:697-717), or
		 * {@code null} for the band-0 ore roots — those are EMI's LEAF form (the bare icon,
		 * BoMScreen.java:662-663/:728).
		 */
		public Rect nodeBox(MaterialTreeDisplay.Node aNode) {
			return aNode.band() == 0 ? null
					: new Rect(nodeX(aNode) - MaterialTreeDisplay.BOX_WIDTH / 2, nodeY(aNode) - MaterialTreeDisplay.BOX_HEIGHT / 2,
							MaterialTreeDisplay.BOX_WIDTH, MaterialTreeDisplay.BOX_HEIGHT);
		}

		/** The node's item-icon 18x18 cell: right of centre inside the box (band > 0), centred on the leaf (band 0). */
		public Rect nodeItemCell(MaterialTreeDisplay.Node aNode) {
			return aNode.band() == 0
					? new Rect(nodeX(aNode) - SLOT / 2, nodeY(aNode) - SLOT / 2, SLOT, SLOT)
					: new Rect(nodeX(aNode) + 1, nodeY(aNode) - SLOT / 2, SLOT, SLOT);
		}

		/**
		 * The node's machine-icon 18x18 cell (the box's left half, the EMI category-icon
		 * seat), or {@code null} when no producing edge resolved a machine stack (the
		 * degrade face — the box renders the item icon alone).
		 */
		public Rect nodeMachineCell(MaterialTreeDisplay.Node aNode) {
			return mMachineCells.get(aNode.prefix());
		}

		private Rect machineCell(MaterialTreeDisplay.Node aNode) {
			return new Rect(nodeX(aNode) - MaterialTreeDisplay.BOX_WIDTH / 2 + 2, nodeY(aNode) - SLOT / 2, SLOT, SLOT);
		}

		/** x centre of a byproduct slot (lane aIndex of the hanging band). */
		public int byproductX(int aIndex) { return mPolicy.laneX0() + aIndex * mPolicy.lanePitch(); }

		/** y centre of the byproduct band's slot row (one band below the deepest chain band). */
		public int byproductY() { return mByproductY; }

		/** y of the byproduct band header (the wire-free gap just above the band — EMI's legend-text seat, BoMScreen.java:222). */
		public int byproductHeaderY() { return byproductY() - 11; }

		/** The per-edge render plans (same order as the edge list this result was planned from). */
		public List<EdgeLayout> edges() { return mEdgeLayouts; }

		/** The node list this result was planned from (the consumers iterate it for slots). */
		public List<MaterialTreeDisplay.Node> nodes() { return mNodes; }

		/** The edge list this result was planned from. */
		public List<MaterialTreeDisplay.Edge> treeEdges() { return mEdges; }

		/**
		 * The EMI connector for one hop (BoMScreen.java:675-694, translated): a same-band hop
		 * is ONE horizontal between the facing box edges; a cross-band hop is the vertical
		 * drop at the target's centre x into its box top, plus the horizontal rail 4px below
		 * the source box's bottom edge (EMI's {@code off = NODE_VERTICAL_SPACING - 1} seat,
		 * adapted to the 8px gap our 28 pitch buys — the rail stays OUTSIDE both boxes).
		 */
		private List<Rect> edgeWire(MaterialTreeDisplay.Node aFrom, MaterialTreeDisplay.Node aTo) {
			int tFromX = nodeX(aFrom), tFromY = nodeY(aFrom);
			int tToX = nodeX(aTo), tToY = nodeY(aTo);
			if (tFromY == tToY) {
				// same-band: between the facing edges (the source may be an unboxed root on
				// band 0 — its icon edge is the anchor then)
				int tRight = tFromX + (aFrom.band() == 0 ? SLOT / 2 : MaterialTreeDisplay.BOX_WIDTH / 2);
				int tLeft = tToX - MaterialTreeDisplay.BOX_WIDTH / 2;
				List<Rect> rWire = new ArrayList<>(1);
				if (tRight != tLeft) rWire.add(new Rect(Math.min(tRight, tLeft), tFromY, Math.abs(tLeft - tRight), 1));
				return List.copyOf(rWire);
			}
			int tRailY = tFromY + MaterialTreeDisplay.BOX_HEIGHT / 2 + 4; // mid-gap: 4 below the source box
			List<Rect> rWire = new ArrayList<>(2);
			Rect tDrop = v(tToX, tRailY, tToY - MaterialTreeDisplay.BOX_HEIGHT / 2);
			if (tDrop != null) rWire.add(tDrop);
			Rect tRail = tFromX != tToX ? new Rect(Math.min(tFromX, tToX), tRailY, Math.abs(tToX - tFromX), 1) : null;
			if (tRail != null) rWire.add(tRail);
			return List.copyOf(rWire);
		}
	}

	private static Rect v(int aX, int aY1, int aY2) { return aY2 != aY1 ? new Rect(aX, Math.min(aY1, aY2), 1, Math.abs(aY2 - aY1)) : null; }

	private MaterialTreeLayout() {
	}
}
