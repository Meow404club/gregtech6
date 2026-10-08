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

import java.util.function.Predicate;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.world.item.ItemStack;

import mezz.jei.api.gui.inputs.IJeiInputHandler;
import mezz.jei.api.gui.inputs.IJeiUserInput;

import gregtech6.emi.GT6MaterialTreeNav;
import gregtech6.recipes.tree.MaterialTreeDisplay;
import gregtech6.recipes.tree.MaterialTreeViewport;

/**
 * The material-tree page's CANVAS input face (task mattree-jei-panzoom) — a whole-canvas
 * {@link IJeiInputHandler} registered through {@code IRecipeExtrasBuilder.addInputHandler}
 * (the seam since 15.9/19.6; the official whole-canvas precedent is DebugRecipeCategory's
 * {@code addInputHandler(new ScreenRectangle(0, 0, W, H))} face, and the in-repo precedent
 * is the nav widget's whole-page listener area). Owns the three pan/zoom/jump events over
 * the tree canvas; every op is a named entry of the {@link GT6MaterialTreeNav} table over
 * the shared {@link MaterialTreeViewport} — zero math decided here (the 禁腿内私有数学
 * red line).
 *
 * <ul>
 * <li><b>wheel</b> → {@link GT6MaterialTreeNav#wheelZoom} about the POINTER (the
 *     zoom-at-cursor anchor). The two JEI generations fork on the scroll signature — 15.x
 *     hands one vertical delta, 19.x hands dual-axis {@code scrollDeltaX/Y} (the //? fork
 *     below, the vertical axis zooms) — the divergence stops at this one method.</li>
 * <li><b>drag</b> → {@link GT6MaterialTreeNav#dragPan}: pure delta pan (the viewport
 *     clamps), so no button state and no drag-session state live here — a drag released
 *     off-canvas leaves nothing behind (the 拖出画布外释放无残留 face).</li>
 * <li><b>click</b> → the JEI click protocol (IJeiInputHandler javadoc): mouse-down is
 *     {@code isSimulate()} — report whether a jumpable tree cell is under the pointer,
 *     EXECUTE nothing (the 防吞分页键 clause — a half-click that slides off the page must
 *     not eat the input); mouse-up EXECUTEs the R-axis jump through
 *     {@link GT6JeiPlugin#openItemPage}. Non-mouse inputs (keys) return false so the page
 *     keeps its keyboard face (page flip, transfer).</li>
 * </ul>
 *
 * <p><b>Area</b>: the tree canvas only (0, 0, WIDTH, HEIGHT) — the control strip below
 * stays with the nav widget's listener (its cells) and JEI's default wheel (page flip), so
 * the zoom never fights the strip and the strip never blocks the zoom.
 */
public class GT6MaterialTreeJeiCanvasHandler implements IJeiInputHandler {

	/** The left mouse button's InputConstants value (GLFW_MOUSE_BUTTON_LEFT = 0). */
	private static final int LEFT_BUTTON = 0;

	private final MaterialTreeViewport mView;
	private final GT6MaterialTreeJeiTreeWidget mTree;
	/** The jump seam: production rides {@link GT6JeiPlugin#openItemPage}, the offline pins inject a recorder. */
	private final Predicate<ItemStack> mJump;

	GT6MaterialTreeJeiCanvasHandler(MaterialTreeViewport aView, GT6MaterialTreeJeiTreeWidget aTree) {
		this(aView, aTree, GT6JeiPlugin::openItemPage);
	}

	/** The test seam (offline has no JEI runtime to jump with). */
	GT6MaterialTreeJeiCanvasHandler(MaterialTreeViewport aView, GT6MaterialTreeJeiTreeWidget aTree, Predicate<ItemStack> aJump) {
		mView = aView;
		mTree = aTree;
		mJump = aJump;
	}

	@Override
	public ScreenRectangle getArea() {
		return new ScreenRectangle(0, 0, MaterialTreeDisplay.WIDTH, MaterialTreeDisplay.HEIGHT);
	}

	@Override
	public boolean handleInput(double aMouseX, double aMouseY, IJeiUserInput aInput) {
		if (aInput.getKey().getType() != InputConstants.Type.MOUSE) return false; // keys stay with JEI
		if (aInput.getKey().getValue() != LEFT_BUTTON) return false; // the canvas pans/jumps on the left hand only
		ItemStack tTarget = mTree.hitAt(aMouseX, aMouseY).stack();
		if (aInput.isSimulate()) return !tTarget.isEmpty(); // mouse-down: report only, execute nothing
		if (tTarget.isEmpty()) return false;
		return mJump.test(tTarget); // mouse-up EXECUTE: the R-axis jump
	}

	//? if forge {
	@Override
	public boolean handleMouseScrolled(double aMouseX, double aMouseY, double aScrollDelta) {
		return wheel(aMouseX, aMouseY, aScrollDelta);
	}
	//?} else {
	/*@Override
	public boolean handleMouseScrolled(double aMouseX, double aMouseY, double aScrollDeltaX, double aScrollDeltaY) {
		return wheel(aMouseX, aMouseY, aScrollDeltaY); // the vertical axis zooms; horizontal trackpad pan is ignored
	}
	*///?}

	private boolean wheel(double aMouseX, double aMouseY, double aDelta) {
		if (aDelta == 0.0 || Double.isNaN(aDelta)) return false; // a hostile delta is unconsumed and inert
		GT6MaterialTreeNav.wheelZoom(mView, aDelta, aMouseX, aMouseY);
		return true;
	}

	@Override
	public boolean handleMouseDragged(double aMouseX, double aMouseY, InputConstants.Key aMouseKey, double aDragX, double aDragY) {
		if (aMouseKey == null || aMouseKey.getType() != InputConstants.Type.MOUSE
				|| aMouseKey.getValue() != LEFT_BUTTON) return false;
		GT6MaterialTreeNav.dragPan(mView, aDragX, aDragY);
		return true;
	}
}
