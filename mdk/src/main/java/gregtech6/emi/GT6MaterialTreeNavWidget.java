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

import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.Widget;

import gregtech6.recipes.tree.MaterialTreeViewport;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The material-tree page's invisible KEY CANVAS (task nav-m2-emi) — a full-page
 * {@link Widget} that exists only for the keyboard face of the EMI widget 五件套:
 * RecipeScreen.keyPressed (emi 1.1.24, RecipeScreen.java:546-557) forwards keys to every
 * widget whose bounds contain the cursor, and the tree slots' own keyPressed returns false
 * for nav keys — so this canvas, spanning the whole page, is where arrows/+/-/0/R land
 * ({@link GT6MaterialTreeNav#keyToAction}). Unmapped keys fall through to EMI untouched.
 *
 * <p>Mouse clicks pass through too (the default false): the canvas never steals the slots'
 * U/R/resolve presses — {@link GT6RecipeMapEmiRecipe.GearJumpWidget} is the same shape.
 */
public class GT6MaterialTreeNavWidget extends Widget {

	private final MaterialTreeViewport mViewport;
	private final Bounds mBounds;
	private final double mFocusX, mFocusY;

	public GT6MaterialTreeNavWidget(MaterialTreeViewport aViewport, int aX, int aY, int aWidth, int aHeight,
			double aFocusX, double aFocusY) {
		mViewport = aViewport;
		mBounds = new Bounds(aX, aY, aWidth, aHeight);
		mFocusX = aFocusX;
		mFocusY = aFocusY;
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
}
