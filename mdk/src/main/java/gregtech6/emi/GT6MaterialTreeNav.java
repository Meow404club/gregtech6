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

package gregtech6.emi;

import gregtech6.recipes.tree.MaterialTreeViewport;

/**
 * The material-tree nav ACTION TABLE (task nav-m2-emi, the M2 EMI card's pure seam) —
 * every input source (ButtonWidget clicks, the canvas keyboard, the click-track slider, and
 * since task mattree-jei-panzoom the JEI canvas wheel/drag) funnels into
 * {@link #handle}/{@code zoomToFraction}/{@link #wheelZoom}/{@link #dragPan} so the page has
 * exactly one nav behaviour, all of it riding the S1 {@link MaterialTreeViewport} math.
 *
 * <p><b>Pure</b>: only {@code java.lang} + the viewport import — no EMI, no MC (pinned by
 * {@code GT6MaterialTreeNavTest}), so the whole table is offline-testable and the M3 JEI /
 * S4 screen cards can consume it verbatim if they ever need the same buttons.
 *
 * <p><b>The 档位 feel</b>: zoom buttons step the exact power-of-two 2x (1→2→4 and back) —
 * binary-exact products mean the S1 floor/ceiling freezes land without float residue.
 * The slider carries the fine grain ({@code MIN + f*(MAX-MIN)}, anchored at the canvas
 * centre); the wheel carries the pointer-anchored fine grain ({@link #WHEEL_STEP} per
 * notch, task mattree-emi-panzoom riding the vendored MUI {@code RecipeScreenMixin}
 * seam — EMI 1.1.24 dispatches scroll/drag/release to no api widget itself, the mixin's
 * HEAD injections are the only delivery path, gated to {@code EmiInteractionSink}).
 * Drag pan needs no table entry of its own: it is the raw screen-space delta straight
 * into {@link MaterialTreeViewport#pan}.
 *
 * <p><b>The anchor policy (task mattree-zoom-anchor, the 指针锚默认化 clause)</b>: a zoom
 * gesture that carries the pointer anchors at the pointer — {@link #wheelZoom} is that
 * face, the wheel being the one zoom gesture the standalone screen receives directly.
 * The EMI page keeps the centre-anchored degraded face BY DESIGN: its buttons/keys/slider
 * declare the canvas centre because the page leg cannot see the pointer for zoom; the
 * standalone screen's wheel is the pointer face.
 * <b>旧钉迁移声明</b>: the screen leg's inline wheel-factor choice
 * ({@code delta > 0 ? ZOOM_FACTOR : 1/ZOOM_FACTOR} — the 2x quantum that jumped any fit
 * past 2x straight onto the 4x ceiling, frozen there) died in task mattree-zoom-anchor;
 * the wheel now takes the fine {@link #WHEEL_STEP}, and the double-click re-fit rides
 * {@link #isDoubleClick}.
 */
public final class GT6MaterialTreeNav {

	/** The button/keyboard zoom step — 2x, binary-exact so the clamps freeze without residue. */
	public static final double ZOOM_FACTOR = 2.0;
	/**
	 * One wheel notch's zoom step (the JEI canvas wheel face, task mattree-jei-panzoom):
	 * gentler than the 2x button 档位 because a wheel is continuous — the zoom-anchor card's
	 * 1.25 档 (small steps are the wheel's job; the big jumps stay with the buttons). A fit
	 * past 2x reaches the 4x ceiling in graded notches instead of one 2x jump (the
	 * mattree-viewport-fit handoff's 「从 >2x 的 fit 一档直达 4x 天花板」 complaint, cured by
	 * task mattree-zoom-anchor's screen routing).
	 */
	public static final double WHEEL_STEP = 1.25;
	/** The double-click window — the vanilla list idiom's 250 ms (ServerSelectionList.mouseClicked). */
	public static final long DOUBLE_CLICK_MS = 250;
	/** The double-click radius: the second click lands within this many px of the first on both axes (vanilla bounds the idiom by entry identity; a bare canvas needs the spatial bound). */
	public static final double DOUBLE_CLICK_RADIUS = 4.0;
	/** One arrow-key pan = one lane pitch of the shared layout (MaterialTreeLayout.LANE_PITCH). */
	public static final double PAN_STEP = 28.0;
	// (the EMI leg's own WHEEL_STEP seat folded into the declaration above — the rebase
	// union of task mattree-jei-panzoom and task mattree-emi-panzoom, same name/value)

	/** One navigation op, source-agnostic (button, key, or whatever a later card wires). */
	public enum Action { ZOOM_IN, ZOOM_OUT, RESET, PAN_UP, PAN_DOWN, PAN_LEFT, PAN_RIGHT, WHEEL_IN, WHEEL_OUT }

	private GT6MaterialTreeNav() {}

	/**
	 * Applies one nav op to the viewport and reports whether it was consumed. The pan
	 * direction is the image-viewer standard: the ARROW names the way the VIEW moves, so
	 * PAN_UP slides the content down (the offset grows toward its ceiling). Degenerate
	 * arguments (null viewport/action) are an unconsumed false.
	 */
	public static boolean handle(MaterialTreeViewport aView, Action aAction, double aFocusX, double aFocusY) {
		if (aView == null || aAction == null) return false;
		switch (aAction) {
			case ZOOM_IN -> aView.zoomAt(aFocusX, aFocusY, ZOOM_FACTOR);
			case ZOOM_OUT -> aView.zoomAt(aFocusX, aFocusY, 1.0 / ZOOM_FACTOR);
			case RESET -> aView.reset();
			case PAN_UP -> aView.pan(0, PAN_STEP);
			case PAN_DOWN -> aView.pan(0, -PAN_STEP);
			case PAN_LEFT -> aView.pan(PAN_STEP, 0);
			case PAN_RIGHT -> aView.pan(-PAN_STEP, 0);
			case WHEEL_IN -> aView.zoomAt(aFocusX, aFocusY, WHEEL_STEP);
			case WHEEL_OUT -> aView.zoomAt(aFocusX, aFocusY, 1.0 / WHEEL_STEP);
		}
		return true;
	}

	/**
	 * The double-click idiom (the vanilla list shape — ServerSelectionList.mouseClicked's
	 * 250 ms window) made spatial: this click is a double when the previous CLEAN click is
	 * under {@link #DOUBLE_CLICK_MS} old and within {@link #DOUBLE_CLICK_RADIUS} px on both
	 * axes. {@code aLastMs == 0} is the no-previous-click fresh state.
	 */
	public static boolean isDoubleClick(long aNowMs, long aLastMs, double aX, double aLastX, double aY, double aLastY) {
		return aLastMs > 0 && aNowMs - aLastMs < DOUBLE_CLICK_MS
				&& Math.abs(aX - aLastX) <= DOUBLE_CLICK_RADIUS && Math.abs(aY - aLastY) <= DOUBLE_CLICK_RADIUS;
	}

	/**

	 * GLFW key code to action (the canvas keyboard face); unknown keys return null so they
	 * fall through to EMI (search, page flip, ...). The literals are the GLFW constants
	 * (verified javap lwjgl-glfw 3.3.1) inlined by javac — no GLFW class load at runtime.
	 */
	public static Action keyToAction(int aKeyCode) {
		return switch (aKeyCode) {
			case 263 -> Action.PAN_LEFT; // GLFW_KEY_LEFT
			case 262 -> Action.PAN_RIGHT; // GLFW_KEY_RIGHT
			case 265 -> Action.PAN_UP; // GLFW_KEY_UP
			case 264 -> Action.PAN_DOWN; // GLFW_KEY_DOWN
			case 61, 334 -> Action.ZOOM_IN; // GLFW_KEY_EQUAL (+), GLFW_KEY_KP_ADD
			case 45, 333 -> Action.ZOOM_OUT; // GLFW_KEY_MINUS, GLFW_KEY_KP_SUBTRACT
			case 48, 82 -> Action.RESET; // GLFW_KEY_0, GLFW_KEY_R
			default -> null;
		};
	}

	/**
	 * The wheel op (the JEI canvas face): one notch of {@code aDelta} = one
	 * {@link #WHEEL_STEP} about the given anchor — the pointer, so the tree point under it
	 * stays under it (the zoom-at-cursor standard). The sign carries the direction (some
	 * platforms send fractional deltas — the signum is the whole read); a zero/NaN delta is
	 * a no-op. All the math stays in {@link MaterialTreeViewport#zoomAt}.
	 */
	public static void wheelZoom(MaterialTreeViewport aView, double aDelta, double aFocusX, double aFocusY) {
		if (aDelta == 0.0 || Double.isNaN(aDelta)) return; // a hostile delta cannot move the pose
		aView.zoomAt(aFocusX, aFocusY, aDelta > 0 ? WHEEL_STEP : 1.0 / WHEEL_STEP);
	}

	/**
	 * The drag-pan op (the JEI canvas face): one drag tick's screen-space delta. A
	 * pass-through to {@link MaterialTreeViewport#pan} (which clamps) — named in the table so
	 * no consumer leg carries a pan verb of its own.
	 */
	public static void dragPan(MaterialTreeViewport aView, double aDx, double aDy) {
		aView.pan(aDx, aDy);
	}

	/**
	 * The click-track slider op: a click at track fraction {@code aFraction} zooms to
	 * {@code MIN + fraction*(MAX-MIN)} about the given anchor. Out-of-range fractions clamp
	 * onto the ends; clicking the handle's own position is a no-op (the S1 档位幂等 through
	 * the factor-1 short-circuit).
	 */
	public static void zoomToFraction(MaterialTreeViewport aView, double aFraction, double aFocusX, double aFocusY) {
		double tTarget = MaterialTreeViewport.MIN_SCALE + clamp01(aFraction)
				* (MaterialTreeViewport.MAX_SCALE - MaterialTreeViewport.MIN_SCALE);
		aView.zoomAt(aFocusX, aFocusY, tTarget / aView.scale());
	}

	/** The slider handle position (0..1 of the track) for the current scale, clamped. */
	public static double fractionOfScale(double aScale) {
		return clamp01((aScale - MaterialTreeViewport.MIN_SCALE)
				/ (MaterialTreeViewport.MAX_SCALE - MaterialTreeViewport.MIN_SCALE));
	}

	/** A click x inside the track as a clamped 0..1 fraction (degenerate tracks are safe 0). */
	public static double trackFraction(double aMouseX, double aTrackX, double aTrackWidth) {
		if (!(aTrackWidth > 0)) return 0.0; // also catches NaN width
		return clamp01((aMouseX - aTrackX) / aTrackWidth);
	}

	private static double clamp01(double aValue) {
		return Math.min(1.0, Math.max(0.0, aValue));
	}
}
