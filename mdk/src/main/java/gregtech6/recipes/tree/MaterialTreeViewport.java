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

/**
 * The material-tree VIEWPORT (task nav-s1-viewport) — the shared pan/zoom transform state of
 * the r11-nav-suite, the S1 foundation card: the M2 EMI leg (TransformSlot
 * {@code getBounds} overrides, nav buttons, keyboard pan), the M3 JEI leg (canvas listener
 * drag/scroll/keys) and the S4 standalone screen all drive this ONE class, so all three
 * consumers navigate identically by construction.
 *
 * <p><b>Pure math, viewer-neutral</b> (the {@link MaterialTreeLayout} precedent): zero
 * JEI/EMI/MC imports — only {@code java.lang}. Rendering and input WIRING are the consumer
 * cards' business; this class only owns the state and its invariants, pinned by
 * {@code MaterialTreeViewportTest}.
 *
 * <p><b>The model</b>: screen = tree * scale + offset, i.e. tree = (screen - offset) / scale.
 * {@code apply}/{@code unapply} are that pair ({@code unapply} is the click-hit inverse);
 * {@code pan} accumulates translation; {@code zoomAt(focusX, focusY, factor)} scales about a
 * SCREEN-space anchor so the tree point under the mouse/button stays under it (the
 * wheel/button standard). {@code reset()} is the exact unit transform (the 「回到单位变换」
 * clause) — which is also the fit pose, because {@link MaterialTreeDisplay}'s 202x206 canvas
 * is BOTH the content and the pane in the default constructor (the r11-nav-suite 基准).
 *
 * <p><b>Clamping (the 防树飞出视野找不回 clause)</b>: scale is held in
 * {@link #MIN_SCALE}..{@link #MAX_SCALE} — the floor is the fit pose itself, so the tree
 * never shrinks below fully visible. The offsets are held in
 * {@code [min(0, pane - content*scale), max(0, pane - content*scale)]} per axis: at fit the
 * pose is pinned to (0,0); zoomed in, one content edge always remains reachable on the pane
 * (S4's larger pane gets the same guarantee for free from the same formula). A zoom factor
 * past a limit FREEZES the whole pose (the 档位幂等 clause — the clamped zoom is a no-op,
 * it does not drift the view), and degenerate factors (NaN/Infinite/0/negative — a hostile
 * wheel delta) are no-ops.
 *
 * <p><b>Shape</b>: mutable with a {@link #copy()} snapshot — a viewer page holds one
 * instance, its input handlers mutate it, its render pass reads it; tests freeze poses via
 * {@code copy()}. Methods are void (no fluent chaining demand), accessors are record-style.
 */
public final class MaterialTreeViewport {

	/** The zoom-out floor — the fit pose itself: the tree never renders smaller than 1:1. */
	public static final double MIN_SCALE = 1.0;
	/** The zoom-in ceiling: an 18 px slot tops out at 72 px, comfortably past reading size. */
	public static final double MAX_SCALE = 4.0;

	private final int contentWidth, contentHeight, paneWidth, paneHeight;
	private double offsetX, offsetY, scale = 1.0;

	/** The viewer-page default: the material-tree canvas ({@link MaterialTreeDisplay} 202x206) as both content and pane. */
	public MaterialTreeViewport() {
		this(MaterialTreeDisplay.WIDTH, MaterialTreeDisplay.HEIGHT, MaterialTreeDisplay.WIDTH, MaterialTreeDisplay.HEIGHT);
	}

	/** The explicit form (the S4 standalone screen's larger-pane escape hatch). */
	public MaterialTreeViewport(int aContentWidth, int aContentHeight, int aPaneWidth, int aPaneHeight) {
		contentWidth = aContentWidth;
		contentHeight = aContentHeight;
		paneWidth = aPaneWidth;
		paneHeight = aPaneHeight;
	}

	/** One 2D point in whichever space the caller asked for. */
	public record Point(double x, double y) {}

	public double offsetX() { return offsetX; }

	public double offsetY() { return offsetY; }

	public double scale() { return scale; }

	/** Accumulates the translation by the given screen-space delta, then clamps. */
	public void pan(double aDx, double aDy) {
		offsetX += aDx;
		offsetY += aDy;
		clampOffsets();
	}

	/**
	 * Scales by {@code aFactor} about the SCREEN-space anchor {@code (aFocusX, aFocusY)}:
	 * the tree point under the focus stays under it. Past a scale limit the pose freezes
	 * entirely; degenerate factors are no-ops.
	 */
	public void zoomAt(double aFocusX, double aFocusY, double aFactor) {
		if (!(aFactor > 0.0) || Double.isInfinite(aFactor)) return; // catches NaN, 0, negatives and infinities
		double tNewScale = Math.min(MAX_SCALE, Math.max(MIN_SCALE, scale * aFactor));
		double tEffective = tNewScale / scale;
		if (tEffective == 1.0) return; // clamped at a limit: the 档位幂等 freeze, nothing moves
		offsetX = aFocusX - (aFocusX - offsetX) * tEffective;
		offsetY = aFocusY - (aFocusY - offsetY) * tEffective;
		scale = tNewScale;
		clampOffsets();
	}

	/** Returns to the exact unit transform (the fit pose). */
	public void reset() {
		offsetX = 0.0;
		offsetY = 0.0;
		scale = 1.0;
	}

	/** tree -> screen. Unclamped by design: this is a query, only the state ops clamp. */
	public Point apply(double aTreeX, double aTreeY) {
		return new Point(aTreeX * scale + offsetX, aTreeY * scale + offsetY);
	}

	/** screen -> tree (the click-hit inverse). Safe: the scale floor keeps the divisor ≥ {@link #MIN_SCALE}. */
	public Point unapply(double aScreenX, double aScreenY) {
		return new Point((aScreenX - offsetX) / scale, (aScreenY - offsetY) / scale);
	}

	/** An independent snapshot of the whole pose (tests freeze poses; callers can stash pre-drag states). */
	public MaterialTreeViewport copy() {
		MaterialTreeViewport rCopy = new MaterialTreeViewport(contentWidth, contentHeight, paneWidth, paneHeight);
		rCopy.offsetX = offsetX;
		rCopy.offsetY = offsetY;
		rCopy.scale = scale;
		return rCopy;
	}

	/**
	 * The 防树飞出视野 clamp: per axis the offset stays within
	 * {@code [min(0, pane - content*scale), max(0, pane - content*scale)]} — at fit that
	 * range is exactly {0}; zoomed in, one content edge always remains on the pane.
	 */
	private void clampOffsets() {
		double tMinX = Math.min(0, paneWidth - contentWidth * scale);
		double tMaxX = Math.max(0, paneWidth - contentWidth * scale);
		double tMinY = Math.min(0, paneHeight - contentHeight * scale);
		double tMaxY = Math.max(0, paneHeight - contentHeight * scale);
		offsetX = Math.min(tMaxX, Math.max(tMinX, offsetX));
		offsetY = Math.min(tMaxY, Math.max(tMinY, offsetY));
	}
}
