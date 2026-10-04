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

package gregtech6.jei;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;

import mezz.jei.api.gui.inputs.IJeiGuiEventListener;
import mezz.jei.api.gui.widgets.IRecipeWidget;

import gregtech6.gui.GT6MaterialTreeScreen;
import gregtech6.recipes.tree.MaterialTreeDisplay;

/**
 * The material-tree page's S4 ENTRY CELL (task nav-s4-tree-screen) — one instance wearing
 * both JEI hats (the nav-m3-jei mounting shape): an {@link IRecipeWidget} drawing a 12px
 * ink cell with the "T" glyph at the canvas's top-right corner, and an
 * {@link IJeiGuiEventListener} consuming ONLY the left clicks inside that cell — each one
 * opens the standalone full-screen tree ({@link GT6MaterialTreeScreen#open}). In-page
 * pan/zoom stays nav-m3-jei's business; this cell is the escape hatch (the r11 ruling
 * 大树走独立屏), so the two faces share the page with zero semantic overlap. Every other
 * event returns false and falls through to the native routing (the slots' U/R untouched).
 *
 * <p>Merged-page note: nav-m3-jei is in review; the category mounts this cell from its
 * {@code createRecipeExtras} tail — the two bodies append (page widgets, different
 * semantics), the rebase is the review seat's. The consumed faces are the loader-neutral
 * records {@link ScreenPosition}/{@link ScreenRectangle} (identical on 1.20.1 and 1.21.1)
 * plus the {@code getPosition}/{@code drawWidget} ({@code 15.20}/{@code 19.19} floors) and
 * {@code getArea}/{@code mouseClicked} ({@code since 15.9/19.6}) widget faces.
 */
public class GT6MaterialTreeJeiScreenButton implements IRecipeWidget, IJeiGuiEventListener {

	/** The ink faces (the M3 cell language). */
	private static final int BORDER_INK = 0xFF555555, BORDER_HOVER_INK = 0xFF7F7F7F;
	private static final int FILL_INK = 0xFF181818, FILL_HOVER_INK = 0xFF2A2A2A;
	private static final int GLYPH_INK = 0xFFE0E0E0;

	/** The cell is 12px (the strip-cell convention both legs share). */
	private static final int CELL = 12;

	private final MaterialTreeDisplay mDisplay;
	/** The action seam (package-private: the offline test substitutes a recorder). */
	Runnable mAction;

	public GT6MaterialTreeJeiScreenButton(MaterialTreeDisplay aDisplay) {
		mDisplay = aDisplay;
		mAction = () -> GT6MaterialTreeScreen.open(mDisplay);
	}

	// ------------------------------------------------------------------
	// the IRecipeWidget hat: page coordinates, so draw and input see one space
	// ------------------------------------------------------------------

	@Override
	public ScreenPosition getPosition() {
		return new ScreenPosition(0, 0);
	}

	@Override
	public void drawWidget(GuiGraphics aGui, double aMouseX, double aMouseY) {
		int tX = GT6MaterialTreeScreen.SCREEN_BUTTON_X, tY = GT6MaterialTreeScreen.SCREEN_BUTTON_Y;
		boolean tHover = aMouseX >= tX && aMouseX < tX + CELL && aMouseY >= tY && aMouseY < tY + CELL;
		aGui.fill(tX, tY, tX + CELL, tY + CELL, tHover ? BORDER_HOVER_INK : BORDER_INK);
		aGui.fill(tX + 1, tY + 1, tX + CELL - 1, tY + CELL - 1, tHover ? FILL_HOVER_INK : FILL_INK);
		aGui.drawString(Minecraft.getInstance().font, "T", tX + 3, tY + 2, GLYPH_INK, false);
	}

	// ------------------------------------------------------------------
	// the IJeiGuiEventListener hat: only the cell's left clicks are ours
	// ------------------------------------------------------------------

	@Override
	public ScreenRectangle getArea() {
		return new ScreenRectangle(new ScreenPosition(GT6MaterialTreeScreen.SCREEN_BUTTON_X, GT6MaterialTreeScreen.SCREEN_BUTTON_Y),
				CELL, CELL);
	}

	@Override
	public boolean mouseClicked(double aMouseX, double aMouseY, int aButton) {
		int tX = GT6MaterialTreeScreen.SCREEN_BUTTON_X, tY = GT6MaterialTreeScreen.SCREEN_BUTTON_Y;
		if (aButton != 0 || aMouseX < tX || aMouseX >= tX + CELL || aMouseY < tY || aMouseY >= tY + CELL) return false;
		mAction.run();
		return true;
	}
}
