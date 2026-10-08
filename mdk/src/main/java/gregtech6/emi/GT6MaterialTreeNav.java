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
 * centre) — the substitute for a drag/wheel zoom, which EMI 1.1.24 recipe widgets cannot
 * receive (RecipeScreen.mouseScrolled routes to the sidebar/page flip; mouseDragged/
 * mouseReleased belong to the slot drag stack; the vendored MUI RecipeScreenMixin forwards
 * only to its own UIWrapperWidget, not to api widgets).
 */
public final class GT6MaterialTreeNav {

	/** The button/keyboard zoom step — 2x, binary-exact so the clamps freeze without residue. */
	public static final double ZOOM_FACTOR = 2.0;
	/**
	 * One wheel notch's zoom step (the JEI canvas wheel face, task mattree-jei-panzoom):
	 * gentler than the 2x button 档位 because a wheel is continuous — the zoom-anchor card's
	 * 1.25 档 (small steps are the wheel's job; the big jumps stay with the buttons).
	 */
	public static final double WHEEL_STEP = 1.25;
	/** One arrow-key pan = one lane pitch of the shared layout (MaterialTreeLayout.LANE_PITCH). */
	public static final double PAN_STEP = 28.0;

	/** One navigation op, source-agnostic (button, key, or whatever a later card wires). */
	public enum Action { ZOOM_IN, ZOOM_OUT, RESET, PAN_UP, PAN_DOWN, PAN_LEFT, PAN_RIGHT }

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
		}
		return true;
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
