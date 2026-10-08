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

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.SlotWidget;

import gregtech6.gui.GT6MaterialTreeScreen;
import gregtech6.recipes.tree.MaterialTreeLayout;
import gregtech6.recipes.tree.MaterialTreeViewport;
import gregtech6.recipes.tree.MaterialTreeViewport.Point;
import net.minecraft.client.gui.GuiGraphics;

/**
 * A material-tree slot that rides the page viewport (task nav-m2-emi, the M2 真槽位 pan/zoom
 * seam). The whole integration is ONE override: emi 1.1.24's {@code SlotWidget.getBounds}
 * is a non-final {@code @Override} (SlotWidget.java:146-155) and every face that matters
 * consumes it — the three render segments (background :170, stack :188, hover overlay
 * :195/:210), the RecipeScreen click hit (:426) and the tooltip/keyboard hover gates
 * (:552) — so the transformed bounds pan/zoom the slot, its U/R/resolve interactions and
 * its hover face together, with zero render overrides.
 *
 * <p>At the fit pose the viewport is the identity, so the bounds are the layout
 * coordinates verbatim and the page renders exactly as before the nav suite.
 *
 * <p><b>The icon face</b> (task mattree-r3-nav-unify, closing the mattree-item-zoom-pose
 * audit): the bounds transform pans/zooms the slot BOX, but EMI's own
 * {@code SlotWidget.drawStack} (xplat SlotWidget.java:187-192, byte-identical on the
 * 1.20.1 and 1.21.1 generations) centres a FIXED 16 px icon inside it —
 * {@code (bounds.width() - 16) / 2} then {@code EmiIngredient.render} — so a zoomed box
 * grew around a 16 px icon (the scale lag). The {@link #drawStack} override mounts the
 * stack through the unified {@link MaterialTreeLayout#pose} primitive (the same
 * {@code runAtPose} mount the standalone screen's icons and both pages' labels ride): the
 * icon renders at {@code 16 * scale} px, centred in the transformed box, and at the fit
 * pose the mount degenerates to EMI's own coordinates verbatim.
 */
public class GT6MaterialTreeTransformSlot extends SlotWidget {

	/** The page's live viewport — public so an offline pin can drive the instance the page shares. */
	public final MaterialTreeViewport mViewport;

	public GT6MaterialTreeTransformSlot(EmiIngredient aStack, int aX, int aY, MaterialTreeViewport aViewport) {
		super(aStack, aX, aY);
		mViewport = aViewport;
	}

	@Override
	public Bounds getBounds() {
		// the slot families this page uses are all square (18px slot, 26px large) — the
		// custom face is mirrored for completeness, a rectangular custom slot would need
		// customHeight here too (ponytail: the tree page has none)
		return transformedBounds(mViewport, x, y, custom ? customWidth : output ? 26 : 18);
	}

	/**
	 * The scaled icon mount: the pose-stack mount replaces EMI's fixed-16px drawStack —
	 * the 16 px icon renders at the viewport scale, still centred in the box (the +inset
	 * is the box's own icon cell, {@link #iconPose}). Empty stacks render nothing either
	 * way, so the degraded-slot faces are unaffected.
	 */
	@Override
	public void drawStack(GuiGraphics aDraw, int aMouseX, int aMouseY, float aDelta) {
		GT6MaterialTreeScreen.runAtPose(aDraw, iconPose(mViewport, x, y, custom ? customWidth : output ? 26 : 18),
				() -> getStack().render(aDraw, 0, 0, aDelta));
	}

	/**
	 * The pure icon-mount math, offline-pinnable: the pose of the centred 16 px icon cell
	 * inside the slot at ({@code aX, aY}) of the given square size — the same +inset EMI's
	 * own {@code (size - 16) / 2} centring picks, expressed in tree space so the pose
	 * carries the scale.
	 */
	static MaterialTreeLayout.Pose iconPose(MaterialTreeViewport aViewport, int aX, int aY, int aSize) {
		return MaterialTreeLayout.pose(aViewport, aX + (aSize - 16) / 2, aY + (aSize - 16) / 2);
	}

	/**
	 * The pure math, offline-pinnable: the slot rect through the viewport as a corner pair
	 * (both corners through {@code apply}, then min/max — never a per-field width scale,
	 * which would drift against the wire rects that share these corners).
	 */
	static Bounds transformedBounds(MaterialTreeViewport aViewport, int aX, int aY, int aSize) {
		Point tA = aViewport.apply(aX, aY);
		Point tB = aViewport.apply(aX + aSize, aY + aSize);
		return new Bounds(
				(int)Math.round(Math.min(tA.x(), tB.x())),
				(int)Math.round(Math.min(tA.y(), tB.y())),
				(int)Math.round(Math.abs(tB.x() - tA.x())),
				(int)Math.round(Math.abs(tB.y() - tA.y())));
	}
}
