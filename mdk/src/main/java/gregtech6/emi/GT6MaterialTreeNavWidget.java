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

import java.util.List;

import brachy.modularui.integration.emi.recipe.EmiInteractionSink;

import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.Widget;

import gregtech6.recipes.tree.MaterialTreeDisplay;
import gregtech6.recipes.tree.MaterialTreeViewport;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The material-tree page's invisible NAV CANVAS (task nav-m2-emi; pan/zoom seam task
 * mattree-emi-panzoom) — a full-page {@link Widget} carrying two faces:
 *
 * <ul>
 * <li><b>the keyboard face</b>: RecipeScreen.keyPressed (emi 1.1.24, RecipeScreen.java
 *     :539-560) forwards keys to every widget whose bounds contain the cursor, and the
 *     tree slots' own keyPressed returns false for nav keys — so this canvas, spanning
 *     the whole page, is where arrows/+/-/0/R land
 *     ({@link GT6MaterialTreeNav#keyToAction}). Unmapped keys fall through to EMI
 *     untouched.</li>
 * <li><b>the pointer seam face</b> ({@link EmiInteractionSink}): scroll/drag/release are
 *     events EMI 1.1.24 never dispatches to api widgets (scroll → sidebar/page flip,
 *     drag/release → the pressed-slot drag stack), so the vendored MUI
 *     {@code RecipeScreenMixin} HEAD injections are the only delivery path — its gate
 *     widened from its own UIWrapperWidget to the {@code EmiInteractionSink} interface,
 *     which this canvas implements. Gating is entirely OURS here (the MUI wrapper keeps
 *     its screen-level semantics): wheel consumes only INSIDE the tree canvas
 *     ({@code [0,0,WIDTH,HEIGHT)}, never the control strip) and zooms pointer-anchored
 *     through the {@link GT6MaterialTreeNav} table; outside it the forward returns false
 *     so sidebar scroll / page flip keep their EMI behaviour. A press on empty canvas
 *     opens a pan gesture (left/middle button — the interaction shape of EMI's built-in
 *     standalone-screen precedent, not a copy of any upstream page); a press on a tree
 *     slot yields ({@link GT6MaterialTreeTransformSlot#getBounds} contains = the same
 *     test EMI's pressedSlot uses, same order) so the slots' native U/R and drag-stack
 *     affordances stay live. Drag pans by the forwarded delta ({@code Viewport.pan} is
 *     screen-space — EMI 1.1.24 pages are unscaled, so delta IS the pane delta); release
 *     is consumed only while the gesture is live, so the slots' pressed-slot release
 *     (the U/R jump) is never suppressed by us.
 * </ul>
 *
 * <p>Mouse clicks outside those two faces pass through (the default false): the canvas
 * never steals the slots' presses — {@link GT6RecipeMapEmiRecipe.GearJumpWidget} is the
 * same shape.
 */
public class GT6MaterialTreeNavWidget extends Widget implements EmiInteractionSink {

	private final MaterialTreeViewport mViewport;
	private final Bounds mBounds;
	/** The tree canvas inside the page — the ONLY rect where wheel/pan gestures live (the strip below is control chrome). */
	private final Bounds mCanvas;
	private final double mFocusX, mFocusY;
	/** The page's slots in add order — the yield set (press hits = native slot affordance wins). */
	private final List<GT6MaterialTreeTransformSlot> mSlots;
	/** The live pan gesture (empty-canvas press → drag → release); not live = every forward returns false. */
	private boolean mPanning;
	private int mPanButton = -1;

	public GT6MaterialTreeNavWidget(MaterialTreeViewport aViewport, int aX, int aY, int aWidth, int aHeight,
			double aFocusX, double aFocusY, List<GT6MaterialTreeTransformSlot> aSlots) {
		mViewport = aViewport;
		mBounds = new Bounds(aX, aY, aWidth, aHeight);
		mCanvas = new Bounds(aX, aY, MaterialTreeDisplay.WIDTH, MaterialTreeDisplay.HEIGHT);
		mFocusX = aFocusX;
		mFocusY = aFocusY;
		mSlots = aSlots;
	}

	@Override
	public Bounds getBounds() {
		return mBounds;
	}

	/** Nothing to draw — the tree renders under this widget (added last, drawn over, paints nil). */
	@Override
	public void render(GuiGraphics aDraw, int aMouseX, int aMouseY, float aDelta) {}

	@Override
	public boolean keyPressed(int aKeyCode, int aScanCode, int aModifiers) {
		return GT6MaterialTreeNav.handle(mViewport, GT6MaterialTreeNav.keyToAction(aKeyCode), mFocusX, mFocusY);
	}

	@Override
	public boolean mouseClicked(int aMouseX, int aMouseY, int aButton) {
		// left/middle open the pan gesture; right stays with EMI. Out of the tree canvas
		// (strip/负坐标) and onto any tree slot = pass-through, native faces win.
		if ((aButton != 0 && aButton != 2) || !mCanvas.contains(aMouseX, aMouseY)) return false;
		for (GT6MaterialTreeTransformSlot tSlot : mSlots) {
			if (tSlot.getBounds().contains(aMouseX, aMouseY)) return false;
		}
		mPanning = true;
		mPanButton = aButton;
		return true;
	}

	@Override
	public boolean mouseDragged(int aButton, double aDeltaX, double aDeltaY) {
		if (!mPanning || aButton != mPanButton) return false;
		mViewport.pan(aDeltaX, aDeltaY);
		return true;
	}

	@Override
	public boolean mouseReleased(int aButton) {
		if (!mPanning || aButton != mPanButton) return false;
		mPanning = false;
		return true; // 手势活跃期才消费——pressedSlot 的 U/R release 永不被我们抑制
	}

	//? if forge {
	@Override
	public boolean mouseScrolled(double aMouseX, double aMouseY, double aAmount) {
		return scrollImpl(aMouseX, aMouseY, aAmount);
	}
	//?} else {
	/*@Override
	public boolean mouseScrolled(double aMouseX, double aMouseY, double aScrollX, double aScrollY) {
		return scrollImpl(aMouseX, aMouseY, aScrollY);
	}
	*///?}

	/**
	 * The wheel face: consume (and zoom pointer-anchored) ONLY inside the tree canvas —
	 * the floored int keeps a pixel left/above the canvas out (conservative: EMI's own
	 * click dispatch truncates, we refuse to swallow a scroll it would not have gated).
	 * The anchor stays the raw double coords — the zoom must not quantise the pointer.
	 */
	private boolean scrollImpl(double aMouseX, double aMouseY, double aAmount) {
		if (!mCanvas.contains((int)Math.floor(aMouseX), (int)Math.floor(aMouseY))) return false;
		GT6MaterialTreeNav.handle(mViewport, aAmount > 0
				? GT6MaterialTreeNav.Action.WHEEL_IN : GT6MaterialTreeNav.Action.WHEEL_OUT, aMouseX, aMouseY);
		return true;
	}
}
