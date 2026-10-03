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
 * The material-tree page's CLICK-TRACK zoom slider (task nav-m2-emi) — a track where a
 * click JUMPS the zoom to the clicked level ({@code MIN + fraction*(MAX-MIN)} anchored at
 * the canvas centre, {@link GT6MaterialTreeNav#zoomToFraction}). Click-track, not drag:
 * EMI 1.1.24 recipe widgets never see mouseDragged/mouseScrolled (RecipeScreen routes them
 * to the sidebar and the slot drag stack), so the track click through
 * {@code mouseClicked} (:420-446: every non-SlotWidget under the cursor gets the press) is
 * the reachable substitute.
 *
 * <p>The render face is plain {@code GuiGraphics.fill} — track, filled span (the current
 * scale's handle position via {@link GT6MaterialTreeNav#fractionOfScale}) and a 3px handle
 * — no textures, no EMI render internals.
 */
public class GT6MaterialTreeSliderWidget extends Widget {

	private static final int TRACK_INK = 0xFF555555;
	private static final int FILL_INK = 0xFF8B8B8B;
	private static final int HANDLE_INK = 0xFFF0F0F0;
	private static final int HANDLE_W = 3;

	private final MaterialTreeViewport mViewport;
	private final Bounds mBounds;
	private final double mFocusX, mFocusY;

	public GT6MaterialTreeSliderWidget(MaterialTreeViewport aViewport, int aX, int aY, int aWidth, int aHeight,
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

	@Override
	public void render(GuiGraphics aDraw, int aMouseX, int aMouseY, float aDelta) {
		int tX = mBounds.x(), tY = mBounds.y(), tW = mBounds.width(), tH = mBounds.height();
		aDraw.fill(tX, tY, tX + tW, tY + tH, TRACK_INK);
		int tFill = (int)Math.round(GT6MaterialTreeNav.fractionOfScale(mViewport.scale()) * tW);
		if (tFill > 0) aDraw.fill(tX, tY, tX + tFill, tY + tH, FILL_INK);
		int tHandle = Math.min(tW - HANDLE_W, Math.max(0, tFill - 1));
		aDraw.fill(tX + tHandle, tY - 1, tX + tHandle + HANDLE_W, tY + tH + 1, HANDLE_INK);
	}

	@Override
	public boolean mouseClicked(int aMouseX, int aMouseY, int aButton) {
		GT6MaterialTreeNav.zoomToFraction(mViewport,
				GT6MaterialTreeNav.trackFraction(aMouseX, mBounds.x(), mBounds.width()), mFocusX, mFocusY);
		return true;
	}
}
