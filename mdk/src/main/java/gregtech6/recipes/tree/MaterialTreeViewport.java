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
 * The material-tree VIEWPORT — the shared pan/zoom transform state: the M2 EMI leg
 * (TransformSlot {@code getBounds} overrides, nav buttons, keyboard pan), the M3 JEI leg
 * (canvas listener drag/scroll/keys) and the S4 standalone screen all drive this ONE
 * class, so all three consumers navigate identically by construction.
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
 * wheel/button standard).
 *
 * <p><b>The fit pose (task mattree-viewport-fit, ADR 2026-10-06-mattree-refactor L2)</b>:
 * the fit scale is dynamic — {@code min(pane/content)} capped at {@link #MAX_SCALE} — so a
 * pane larger than the content MAGNIFIES the tree to fill it, centred, instead of leaving
 * a small top-left block (the symptom23 ② root cause). {@code reset()} returns to that fit
 * pose: fully visible, centred. A fresh viewport IS the fit pose. <b>旧钉迁移声明</b>: the
 * former 「fit = 1:1 恒等、reset = 单位变换」 pin (MIN_SCALE as the fit) died here — the
 * replacement pins are {@link #minScale()} (the live dynamic floor) and the reset-is-fit
 * semantics above; the static {@link #MIN_SCALE} survives ONLY as the default page's fit
 * value (content == pane) and the {@code GT6MaterialTreeNav} slider-range floor — it is
 * NOT the live zoom floor any more.
 *
 * <p><b>Clamping (the 防树飞出视野找不回 clause)</b>: scale is held in
 * {@link #minScale()}..{@link #MAX_SCALE} — the floor is the fit scale itself, so the tree
 * never shrinks below fully visible (a pane smaller than the content fits by shrinking,
 * the same formula — the former 「never below 1:1」 reading retired with the fit rework).
 * The offsets are held in {@code [min(0, pane - content*scale), max(0, pane - content*scale)]}
 * per axis: at fit the limiting axis pins to (0) and the other axis centres the tree
 * inside its slack; zoomed in, one content edge always remains reachable on the pane. A
 * zoom factor past a limit FREEZES the whole pose (the 档位幂等 clause — the clamped zoom
 * is a no-op, it does not drift the view), and degenerate factors (NaN/Infinite/0/negative
 * — a hostile wheel delta) are no-ops.
 *
 * <p><b>Resize (the 保锚 clause)</b>: {@link #resizeTo} re-binds the pane, keeping the tree
 * point of the old pane's centre under the new pane's centre at the kept zoom level (within
 * the new floor) — a window resize no longer throws the navigation pose away.
 *
 * <p><b>Shape</b>: mutable with a {@link #copy()} snapshot — a viewer page holds one
 * instance, its input handlers mutate it, its render pass reads it; tests freeze poses via
 * {@code copy()}. Methods are void (no fluent chaining demand), accessors are record-style.
 */
public final class MaterialTreeViewport {

	/**
	 * The legacy 1:1 floor — kept as a constant for the nav slider range and the viewer
	 * legs' sleeping-cell tests (see the class javadoc 旧钉迁移声明): the LIVE zoom-out
	 * floor is the dynamic fit, {@link #minScale()}.
	 */
	public static final double MIN_SCALE = 1.0;
	/** The zoom-in ceiling: an 18 px slot tops out at 72 px, comfortably past reading size. Also caps the fit scale. */
	public static final double MAX_SCALE = 4.0;

	private final int contentWidth, contentHeight;
	private int paneWidth, paneHeight;
	private double mFitScale;
	private double offsetX, offsetY, scale;

	/** The viewer-page default: the material-tree canvas ({@link MaterialTreeDisplay} 202x206) as both content and pane — fit 1:1, centred by definition. */
	public MaterialTreeViewport() {
		this(MaterialTreeDisplay.WIDTH, MaterialTreeDisplay.HEIGHT, MaterialTreeDisplay.WIDTH, MaterialTreeDisplay.HEIGHT);
	}

	/** The explicit form: content = the shared canvas, pane = whatever the consumer shows (the standalone screen passes the real window). */
	public MaterialTreeViewport(int aContentWidth, int aContentHeight, int aPaneWidth, int aPaneHeight) {
		contentWidth = aContentWidth;
		contentHeight = aContentHeight;
		paneWidth = Math.max(1, aPaneWidth);
		paneHeight = Math.max(1, aPaneHeight);
		refit();
	}

	/** One 2D point in whichever space the caller asked for. */
	public record Point(double x, double y) {}

	public double offsetX() { return offsetX; }

	public double offsetY() { return offsetY; }

	public double scale() { return scale; }

	/** The live zoom-out floor = the fit scale: {@code min(pane/content)} capped at {@link #MAX_SCALE} — the tree never renders smaller than fully visible. */
	public double minScale() { return mFitScale; }

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
		double tNewScale = Math.min(MAX_SCALE, Math.max(mFitScale, scale * aFactor));
		double tEffective = tNewScale / scale;
		if (tEffective == 1.0) return; // clamped at a limit: the 档位幂等 freeze, nothing moves
		offsetX = aFocusX - (aFocusX - offsetX) * tEffective;
		offsetY = aFocusY - (aFocusY - offsetY) * tEffective;
		scale = tNewScale;
		clampOffsets();
	}

	/** Returns to the fit pose: the whole tree fully visible, centred on the pane. */
	public void reset() {
		refit();
	}

	/**
	 * Re-binds the pane (the window-resize seam), keeping the tree point of the old pane's
	 * centre under the new pane's centre and the zoom level within the new
	 * {@link #minScale()}..{@link #MAX_SCALE} range.
	 */
	public void resizeTo(int aPaneWidth, int aPaneHeight) {
		Point tAnchor = unapply(paneWidth / 2.0, paneHeight / 2.0);
		paneWidth = Math.max(1, aPaneWidth);
		paneHeight = Math.max(1, aPaneHeight);
		mFitScale = computeFit();
		scale = Math.min(MAX_SCALE, Math.max(mFitScale, scale));
		offsetX = paneWidth / 2.0 - tAnchor.x() * scale;
		offsetY = paneHeight / 2.0 - tAnchor.y() * scale;
		clampOffsets();
	}

	/** tree -> screen. Unclamped by design: this is a query, only the state ops clamp. */
	public Point apply(double aTreeX, double aTreeY) {
		return new Point(aTreeX * scale + offsetX, aTreeY * scale + offsetY);
	}

	/** screen -> tree (the click-hit inverse). Safe: the scale floor keeps the divisor ≥ {@link #minScale()} > 0. */
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

	/** The fit scale of the CURRENT pane. */
	private double computeFit() {
		return Math.min(MAX_SCALE, Math.min((double) paneWidth / contentWidth, (double) paneHeight / contentHeight));
	}

	/** Re-lands on the fit pose of the current pane: fit scale, both axes centred. */
	private void refit() {
		mFitScale = computeFit();
		scale = mFitScale;
		offsetX = (paneWidth - contentWidth * mFitScale) / 2.0;
		offsetY = (paneHeight - contentHeight * mFitScale) / 2.0;
	}

	/**
	 * The 防树飞出视野 clamp: per axis the offset stays within
	 * {@code [min(0, pane - content*scale), max(0, pane - content*scale)]} — at fit the
	 * limiting axis pins to 0 (the other centres), zoomed in one content edge always
	 * remains on the pane.
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
