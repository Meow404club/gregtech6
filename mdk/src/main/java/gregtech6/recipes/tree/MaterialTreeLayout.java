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
 * The material-tree node-graph GEOMETRY (task mattree-v2-nodes) — the viewer-NEUTRAL
 * helper both legs render from (spec clause ⑦): pure vanilla math over the
 * {@link MaterialTreeDisplay} model, zero JEI/EMI imports, every rect pinned by
 * {@code MaterialTreeLayoutTest}. The JEI category's {@code draw(GuiGraphics)} and the EMI
 * recipe's {@code addDrawable} consumer both translate these rects 1:1 through
 * {@code GuiGraphics.fill(x, y, x+w, y+h, ink)} (1.20.1 GuiGraphics has no diagonal
 * primitive — hLine/vLine/fill only, tmp/vanilla-1.20.1 GuiGraphics.java:121-190 — so all
 * routing is MANHATTAN and the arrowhead is a rasterised solid triangle, spec clause ⑤).
 *
 * <p><b>The three-part edge</b> (spec clause ②): every chain hop renders as
 * 「from slot –wire– [machine icon] –arrow→ to slot」. The machine icon rides the gap right
 * of the FROM column (the 22 px gap budgets exactly 2 px entry wire + 16 px icon + 4 px
 * arrowhead); the wire exits the from slot's right-edge centre, runs a shared vertical TRUNK
 * along the slot's right edge (spec clause ⑥'s 沿槽右缘走 — fan-outs share the trunk,
 * circuit-diagram style), bends horizontally into the machine icon's left edge, crosses from
 * its right edge and bends again at the arrow's back line before entering the target. Same-
 * gap edges whose midpoints collide (same from row AND same to row — e.g. crushed→dust and
 * crushed→crushedPurified) stagger the machine icon and its run 2 px per collision (spec
 * clause ⑥'s y 错开 2-3 px).
 *
 * <p><b>Degraded edges</b> ({@code Edge.machine()} EMPTY — an untabled map, offline always):
 * same Manhattan path minus the machine box, plus the v1-style via-label position so the
 * machine NAMES survive as text (the 降级纯箭头+文字 clause; live clients never hit it —
 * the census test pins {@link MaterialTreeWorkstations} against 100% of the displayed
 * mapNames).
 *
 * <p>Z-order is the viewers' contract: JEI draws in the category background layer (under
 * slots) and the EMI leg must add the wire widgets BEFORE the slot widgets — a wire crossing
 * an intermediate column's slot hides behind the slot box in both legs.
 */
public final class MaterialTreeLayout {

	/** The standard slot square (item nodes ride 18x18 boxes). */
	public static final int SLOT = 18;
	/** The machine-icon node: a bare 16x16 item icon (no slot background — both viewers mount it background-free). */
	public static final int MACHINE = 16;
	/** The wire gap between columns ({@link MaterialTreeDisplay#COLUMN_PITCH} - {@link #SLOT}). */
	public static final int GAP = MaterialTreeDisplay.COLUMN_PITCH - SLOT;
	/** Arrowhead width in px; the solid triangle is {@value #ARROW_HEAD} wide, 7 px tall. */
	public static final int ARROW_HEAD = 4;
	/**
	 * The gap's exact horizontal budget (22 px = 2 wire + {@value #MACHINE} machine + 4 arrow):
	 * the machine icon packs LEFT against the entry-wire zone so the arrowhead's 4 px fit
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
	 * One edge's render plan, aligned by index with {@link MaterialTreeDisplay#edges()}:
	 * {@code machine} is the 16x16 bare-icon box (null on the degraded face; both viewers
	 * mount the stack background-free ON it — JEI a RENDER_ONLY slot with no background, EMI
	 * a {@code drawBack(false)} SlotWidget — and both hover-boxes extend one px around it),
	 * {@code wire} the Manhattan segments, {@code arrow} the solid-triangle rows with the tip
	 * on the target slot's left edge, {@code labelX/labelY} the degraded via-label spot.
	 */
	public record EdgeLayout(Rect machine, List<Rect> wire, List<Rect> arrow, int labelX, int labelY) {}

	/**
	 * The one edge's geometry between two slot top-lefts. {@code aMachine} false renders the
	 * degraded face; {@code aDup} is the collision index among same-gap same-midpoint edges
	 * (0 for the first). Pure — the pin tests call this directly with hand-picked coords.
	 */
	public static EdgeLayout edge(int aFromX, int aFromY, int aToX, int aToY, boolean aMachine, int aDup) {
		int fcy = aFromY + SLOT / 2, tcy = aToY + SLOT / 2;
		int mid = (aFromY + aToY) / 2;
		int mx = aFromX + SLOT + ENTRY_WIRE; // 16px machine icon left-packed in the gap
		// the machine rides the CENTRE-LINE midpoint (mid + 9 = (fcy + tcy) / 2) so a same-row
		// edge stays one straight line (mcy == fcy == tcy); the stagger jogs it +2 px per dup
		int my = mid + SLOT / 2 - MACHINE / 2 + aDup * DUP_STAGGER;
		int mcy = my + MACHINE / 2;
		int trunkX = aFromX + SLOT + 1; // the shared fan-out trunk along the from slot's right edge
		int backX = aToX - ARROW_HEAD;  // the arrow's back line — the target-side bend sits on it

		List<Rect> rWire = new ArrayList<>();
		if (aMachine) {
			if (mcy != fcy) {
				add(rWire, h(aFromX + SLOT, trunkX, fcy));
				add(rWire, v(trunkX, fcy, mcy));
				add(rWire, h(trunkX, mx, mcy));
			} else {
				add(rWire, h(aFromX + SLOT, mx, fcy));
			}
			if (mcy != tcy) {
				add(rWire, h(mx + MACHINE, backX, mcy));
				add(rWire, v(backX, mcy, tcy));
			} else {
				add(rWire, h(mx + MACHINE, backX, mcy));
			}
		} else if (fcy != tcy) {
			add(rWire, h(aFromX + SLOT, trunkX, fcy));
			add(rWire, v(trunkX, fcy, tcy));
			add(rWire, h(trunkX, backX, tcy));
		} else {
			add(rWire, h(aFromX + SLOT, backX, fcy));
		}
		return new EdgeLayout(aMachine ? new Rect(mx, my, MACHINE, MACHINE) : null,
				List.copyOf(rWire), arrowhead(aToX, tcy), aFromX + SLOT - 2, mid - 4);
	}

	/**
	 * The whole display's per-edge plans (same order as {@link MaterialTreeDisplay#edges()}),
	 * with the collision indices computed off the display's own deterministic edge order.
	 */
	public static List<EdgeLayout> layout(MaterialTreeDisplay aDisplay) {
		Map<OreDictPrefix, Node> tNodes = new LinkedHashMap<>();
		for (Node tNode : aDisplay.nodes()) tNodes.put(tNode.prefix(), tNode);
		Map<Long, Integer> tDups = new LinkedHashMap<>();
		List<EdgeLayout> rLayouts = new ArrayList<>();
		for (Edge tEdge : aDisplay.edges()) {
			Node tFrom = tNodes.get(tEdge.from()), tTo = tNodes.get(tEdge.to());
			if (tFrom == null || tTo == null) continue; // defensive; the assembly only emits displayed endpoints
			int tFromX = MaterialTreeDisplay.nodeX(tFrom), tFromY = MaterialTreeDisplay.nodeY(tFrom);
			int tToX = MaterialTreeDisplay.nodeX(tTo), tToY = MaterialTreeDisplay.nodeY(tTo);
			long tKey = ((long) tFromX << 32) | ((tFromY + tToY) / 2);
			int tDup = tDups.getOrDefault(tKey, 0);
			tDups.put(tKey, tDup + 1);
			rLayouts.add(edge(tFromX, tFromY, tToX, tToY, !tEdge.machine().isEmpty(), tDup));
		}
		return rLayouts;
	}

	/** The solid right-pointing triangle: tip on the target slot's left edge centre, 4x7 rasterised as fill rows. */
	public static List<Rect> arrowhead(int aTipX, int aTipY) {
		List<Rect> rRows = new ArrayList<>(7);
		for (int dy = -(ARROW_HEAD - 1); dy < ARROW_HEAD; dy++) {
			int tWidth = ARROW_HEAD - Math.abs(dy);
			rRows.add(new Rect(aTipX - tWidth, aTipY + dy, tWidth, 1));
		}
		return rRows;
	}

	private static Rect h(int aX1, int aX2, int aY) { return aX2 > aX1 ? new Rect(aX1, aY, aX2 - aX1, 1) : null; }
	private static Rect v(int aX, int aY1, int aY2) { return aY2 != aY1 ? new Rect(aX, Math.min(aY1, aY2), 1, Math.abs(aY2 - aY1)) : null; }
	private static void add(List<Rect> aRects, Rect aRect) { if (aRect != null) aRects.add(aRect); }

	private MaterialTreeLayout() {
	}
}
