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

import gregtech6.emi.GT6MaterialTreeEmiRecipe;
import gregtech6.emi.GT6MaterialTreeNav;
import gregtech6.recipes.tree.MaterialTreeDisplay;
import gregtech6.recipes.tree.MaterialTreeViewport;

/**
 * The material-tree page nav face of the JEI leg (task nav-m3-jei, the M3 card of the
 * r11-nav-suite) — one instance wearing both JEI hats: an {@link IRecipeWidget} that draws
 * the zoom in/out/reset strip below the canvas, and an {@link IJeiGuiEventListener} that
 * routes strip clicks and the canvas keyboard into the M2 action table
 * {@link GT6MaterialTreeNav#handle} verbatim (the viewer-neutral action table, zero math
 * copied). The strip rides the EMI twin's band constants
 * ({@link GT6MaterialTreeEmiRecipe#BUTTON_X0}/{@code BUTTON_PITCH}/{@code BUTTON_Y}/
 * {@code CONTROL_STRIP_H} — javac inlines the compile-time ints, so this class carries no
 * EMI bytecode) and sleeps the same buttons at the S1 floor/ceiling.
 *
 * <p><b>槽位变换缝评估 (acceptance ②) — JEI 侧无缝，降级为按钮+键盘缩放，无逐槽 pan。</b>
 * The EMI twin transforms every slot through a {@code getBounds} override because EMI's
 * SlotWidget render/hit/tooltip faces all consume it; JEI has NO equivalent seam:
 * <ul>
 * <li>slot coordinates freeze at layout build — {@code IRecipeSlotBuilder.setPosition}
 *     (an {@code IPlaceable} face) only exists inside {@code setRecipe}; there is no
 *     render-time move API;</li>
 * <li>the slot drawables themselves ({@code RecipeSlot} and friends) live in JEI's
 *     library-internal packages — plugins cannot subclass them, and the api
 *     {@code IRecipeSlotDrawable} face is a read-only view with no bounds override;</li>
 * <li>moving slots with the viewport would mean rebuilding the whole {@code RecipeLayout}
 *     (re-running {@code setRecipe}) — no public API triggers that from a widget click.</li>
 * </ul>
 * The remaining option — self-drawing the whole tree over invisible slots — is REJECTED by
 * the user ruling (the native hover U/R affordance is the point of the JEI leg), so on this
 * leg the tree geometry does NOT consume the viewport at all: the controls drive the shared
 * nav state (slider-free button face, visible sleep states, hover hints) and the full
 * pan/zoom experience stays with the EMI twin and the later S4 independent screen. This is
 * the pre-authorized degradation (按钮+键盘缩放，无逐槽 pan), not a silent no-op.
 *
 * <p><b>Lifecycle</b>: the viewport is created by
 * {@code GT6MaterialTreeJeiCategory.createRecipeExtras} and this instance closes over it;
 * JEI keeps extras "as long as a recipe layout is on screen" (IRecipeExtrasBuilder javadoc)
 * and rebuilds layouts on page updates, which is the same fresh-per-page-open closure the
 * EMI twin gets from {@code addWidgets}.
 *
 * <p><b>The red line (native U/R untouched)</b>: {@link #mouseClicked} consumes ONLY
 * left-clicks inside the three 12px strip cells — the band below the canvas holds no native
 * slots, and every other click returns false and falls through to the native routing
 * (RecipeLayoutInputHandler tries input handlers, then gui event listeners, then the legacy
 * category face). {@link #keyPressed} consumes only the mapped nav keys and mirrors the EMI
 * twin's hover gating: JEI only dispatches keys into the layout while the pointer hovers it
 * ({@code RecipeLayoutInputHandler.handleInput}'s {@code isMouseOver} gate, 15.x :36-39,
 * 19.x same shape). Slot tooltips keep their native priority (IRecipeExtrasBuilder: slots
 * take priority for tooltips), so the hover hints never shadow a slot.
 *
 * <p>Consumed faces (JEI 15.x = 1.20.1-forge 15.62 and 19.x = 1.21.1-neoforge 19.52,
 * identical on this surface): {@code IRecipeWidget} ({@code getPosition}/
 * {@code drawWidget} — 15.20/19.19 floors), {@code IJeiGuiEventListener}
 * ({@code getArea}/mouseClicked/keyPressed — since 15.9/19.6). The scroll/drag defaults are
 * left untouched on purpose: the ruling keeps this leg on EMI-parity button+keyboard
 * semantics. Consumed MC types are the loader-neutral records ScreenPosition/ScreenRectangle
 * (identical on 1.20.1 and 1.21.1) plus GuiGraphics fill/drawString as in the EMI twin.
 */
public class GT6MaterialTreeJeiNavWidget implements IRecipeWidget, IJeiGuiEventListener {

	/** The ink faces: neutral border, two button fills (rest/hover), glyph, and the sleeping greys. */
	private static final int BORDER_INK = 0xFF555555, BORDER_HOVER_INK = 0xFF7F7F7F;
	private static final int FILL_INK = 0xFF181818, FILL_HOVER_INK = 0xFF2A2A2A, FILL_SLEEP_INK = 0xFF101010;
	private static final int GLYPH_INK = 0xFFE0E0E0, GLYPH_SLEEP_INK = 0xFF555555;

	/** The glyphs of the three cells, in {@link GT6MaterialTreeNav.Action} order: in, out, reset. */
	private static final String[] GLYPHS = {"+", "-", "R"};

	/** The live viewport this face closes over (package-private: the nav test drives it). */
	final MaterialTreeViewport mView;
	private final double mFocusX = MaterialTreeDisplay.WIDTH / 2.0, mFocusY = MaterialTreeDisplay.HEIGHT / 2.0;

	public GT6MaterialTreeJeiNavWidget(MaterialTreeViewport aView) {
		mView = aView;
	}

	// ------------------------------------------------------------------
	// the IRecipeWidget hat: the draw origin is the page origin, so the
	// strip cells below are page coordinates, same as the input face sees
	// ------------------------------------------------------------------

	@Override
	public ScreenPosition getPosition() {
		return new ScreenPosition(0, 0);
	}

	/** Null: the face carries no tooltip of its own — the hover hints ride the category's tooltip areas. */
	@Override
	public ScreenRectangle getScreenRectangle() {
		return null;
	}

	@Override
	public void drawWidget(GuiGraphics aGuiGraphics, double aMouseX, double aMouseY) {
		for (int i = 0; i < GLYPHS.length; i++) {
			int tX = GT6MaterialTreeEmiRecipe.BUTTON_X0 + i * GT6MaterialTreeEmiRecipe.BUTTON_PITCH;
			int tY = GT6MaterialTreeEmiRecipe.BUTTON_Y;
			boolean tSleeping = isSleeping(i);
			boolean tHovered = !tSleeping && aMouseX >= tX && aMouseX < tX + 12 && aMouseY >= tY && aMouseY < tY + 12;
			aGuiGraphics.fill(tX, tY, tX + 12, tY + 12, tSleeping ? FILL_SLEEP_INK : tHovered ? FILL_HOVER_INK : FILL_INK);
			aGuiGraphics.fill(tX, tY, tX + 12, tY + 1, tSleeping ? FILL_SLEEP_INK : tHovered ? BORDER_HOVER_INK : BORDER_INK);
			aGuiGraphics.fill(tX, tY + 11, tX + 12, tY + 12, tSleeping ? FILL_SLEEP_INK : tHovered ? BORDER_HOVER_INK : BORDER_INK);
			aGuiGraphics.fill(tX, tY, tX + 1, tY + 12, tSleeping ? FILL_SLEEP_INK : tHovered ? BORDER_HOVER_INK : BORDER_INK);
			aGuiGraphics.fill(tX + 11, tY, tX + 12, tY + 12, tSleeping ? FILL_SLEEP_INK : tHovered ? BORDER_HOVER_INK : BORDER_INK);
			String tGlyph = GLYPHS[i];
			var tFont = Minecraft.getInstance().font;
			aGuiGraphics.drawString(tFont, tGlyph, tX + (12 - tFont.width(tGlyph)) / 2, tY + 2,
					tSleeping ? GLYPH_SLEEP_INK : GLYPH_INK, false);
		}
	}

	/** The EMI twin's active faces: zoom-out sleeps at the fit pose, zoom-in at the ceiling; reset never does. */
	private boolean isSleeping(int aCell) {
		return switch (aCell) {
			case 0 -> mView.scale() >= MaterialTreeViewport.MAX_SCALE;
			case 1 -> mView.scale() <= MaterialTreeViewport.MIN_SCALE;
			default -> false;
		};
	}

	// ------------------------------------------------------------------
	// the IJeiGuiEventListener hat: the area is the whole page, so JEI hands
	// the events back in page coordinates (the area origin is (0,0))
	// ------------------------------------------------------------------

	@Override
	public ScreenRectangle getArea() {
		return new ScreenRectangle(0, 0, MaterialTreeDisplay.WIDTH,
				MaterialTreeDisplay.HEIGHT + GT6MaterialTreeEmiRecipe.CONTROL_STRIP_H);
	}

	@Override
	public boolean mouseClicked(double aMouseX, double aMouseY, int aButton) {
		if (aButton != 0) return false;
		int tCell = cellAt(aMouseX, aMouseY);
		if (tCell < 0) return false; // canvas clicks (native slot territory) fall through — the red line
		return GT6MaterialTreeNav.handle(mView, actionOf(tCell), mFocusX, mFocusY);
	}

	@Override
	public boolean keyPressed(double aMouseX, double aMouseY, int aKeyCode, int aScanCode, int aModifiers) {
		GT6MaterialTreeNav.Action tAction = GT6MaterialTreeNav.keyToAction(aKeyCode);
		if (tAction == null) return false; // typing keys stay with JEI (search etc.)
		return GT6MaterialTreeNav.handle(mView, tAction, mFocusX, mFocusY);
	}

	/** The strip cell under the page point, or -1 (the band below the canvas holds no native slots). */
	private static int cellAt(double aMouseX, double aMouseY) {
		if (aMouseX < GT6MaterialTreeEmiRecipe.BUTTON_X0 || aMouseY < GT6MaterialTreeEmiRecipe.BUTTON_Y
				|| aMouseY >= GT6MaterialTreeEmiRecipe.BUTTON_Y + 12) return -1;
		int tCell = (int)((aMouseX - GT6MaterialTreeEmiRecipe.BUTTON_X0) / GT6MaterialTreeEmiRecipe.BUTTON_PITCH);
		return tCell >= 0 && tCell < GLYPHS.length
				&& aMouseX < GT6MaterialTreeEmiRecipe.BUTTON_X0 + tCell * GT6MaterialTreeEmiRecipe.BUTTON_PITCH + 12
						? tCell : -1;
	}

	private static GT6MaterialTreeNav.Action actionOf(int aCell) {
		return switch (aCell) {
			case 0 -> GT6MaterialTreeNav.Action.ZOOM_IN;
			case 1 -> GT6MaterialTreeNav.Action.ZOOM_OUT;
			default -> GT6MaterialTreeNav.Action.RESET;
		};
	}
}
