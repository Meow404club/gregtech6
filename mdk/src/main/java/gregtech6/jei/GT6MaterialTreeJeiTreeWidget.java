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

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.widgets.IRecipeWidget;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusGroup;

import gregtech6.gui.GT6MaterialTreeScreen;
import gregtech6.recipes.tree.MaterialTreeDisplay;
import gregtech6.recipes.tree.MaterialTreeDisplay.Byproduct;
import gregtech6.recipes.tree.MaterialTreeDisplay.Edge;
import gregtech6.recipes.tree.MaterialTreeDisplay.Node;
import gregtech6.recipes.tree.MaterialTreeDisplay.Overflow;
import gregtech6.recipes.tree.MaterialTreeLayout;
import gregtech6.recipes.tree.MaterialTreeLayout.EdgeLayout;
import gregtech6.recipes.tree.MaterialTreeLayout.Rect;
import gregtech6.recipes.tree.MaterialTreeViewport;

/**
 * The material-tree page's SELF-DRAWN tree body (task mattree-jei-panzoom, the unfreeze
 * ruling a) — one {@link IRecipeWidget} painting the whole diagram through the shared
 * {@link MaterialTreeViewport} at render time, replacing the frozen-coordinate native slots
 * (旧钉迁移声明: the 「JEI freezes slot coordinates, self-drawing is ruled out」 pin of the
 * nav-m3-jei era died here — the ruling was reversed by the mattree-jei-unfreeze-poc
 * research: every event reaches the recipe page, and {@code addInvisibleIngredients} keeps
 * the lookup index, so the native-slot affordance was the only thing lost, and it is
 * rebuilt below). The ink plan is the SAME {@link MaterialTreeLayout} table the EMI twin
 * and the standalone screen render from (the 双 viewer 一坐标表 clause) — zero geometry
 * decided here (the 禁腿内私有数学 red line): every transform is
 * {@link MaterialTreeViewport#apply}/{@code unapply} via {@link MaterialTreeLayout#pose},
 * every fill is a corner-pair of applied points.
 *
 * <p><b>The rebuilt affordances</b> (what the invisible slots no longer provide):
 * <ul>
 * <li><b>hover</b>: {@link #getTooltip} self-hits through {@code unapply} (the pointer
 *     mapped back into tree space) and answers the hovered node's stack name, the machine
 *     icons' via-label and the byproducts' source label — {@code getScreenRectangle} stays
 *     the API default {@code null} = own hit-testing (IRecipeWidget javadoc);</li>
 * <li><b>click</b>: {@link GT6MaterialTreeJeiCanvasHandler#hitAt} (the same hit domain,
 *     shared verbatim) routes a node/byproduct click into the R-axis jump;</li>
 * <li><b>focus highlight</b>: the native visible slots highlighted themselves under a
 *     focus — that died with the slots, so the outline is drawn here from the
 *     {@link IFocusGroup} the category's {@code createRecipeExtras} hands in.</li>
 * </ul>
 *
 * <p><b>Lifecycle</b>: one instance per page layout, built by
 * {@code GT6MaterialTreeJeiCategory.createRecipeExtras} over a fresh viewport (JEI keeps
 * extras alive as long as the layout is on screen — the EMI twin's per-page-open closure).
 * A null display or null focus group is legal (JEI calls extras without a recipe in probe
 * paths; the offline pins ride both) — the widget then draws and hits nothing.
 */
public class GT6MaterialTreeJeiTreeWidget implements IRecipeWidget {

	/** The plain header/overflow ink (the batch-1 NEI black) and the degraded via-label grey. */
	private static final int HEADER_INK = 0xFF000000, VIA_INK = 0xFF555555;
	/** The focus outline: one white px ring, one tree-px outside the 18px cell. */
	private static final int FOCUS_INK = 0xFFFFFFFF;

	private final MaterialTreeDisplay mDisplay;
	private final MaterialTreeViewport mView;
	private final IFocusGroup mFocuses;
	private final List<Edge> mEdges;
	private final List<EdgeLayout> mLayouts;

	/** Package-private: the category is the production builder (the extras closure). */
	GT6MaterialTreeJeiTreeWidget(MaterialTreeDisplay aDisplay, MaterialTreeViewport aView, IFocusGroup aFocuses) {
		mDisplay = aDisplay;
		mView = aView;
		mFocuses = aFocuses;
		mEdges = aDisplay == null ? List.of() : aDisplay.edges();
		mLayouts = aDisplay == null ? List.of() : MaterialTreeLayout.layout(aDisplay);
	}

	@Override
	public ScreenPosition getPosition() {
		return new ScreenPosition(0, 0);
	}

	// ------------------------------------------------------------------
	// the draw hat: page coordinates in (the draw origin is the page origin),
	// tree coordinates out through the viewport
	// ------------------------------------------------------------------

	@Override
	public void drawWidget(GuiGraphics aGui, double aMouseX, double aMouseY) {
		if (mDisplay == null) return;
		Font tFont = Minecraft.getInstance().font;
		// the headers
		label(aGui, tFont, MaterialTreeDisplay.materialName(mDisplay.material), 4, 4, HEADER_INK);
		label(aGui, tFont, MaterialTreeDisplay.BYPRODUCT_HEADER,
				MaterialTreeDisplay.LANE_X0, MaterialTreeDisplay.BYPRODUCT_HEADER_Y, HEADER_INK);
		// the wires + arrowheads: corner-pair transforms so shared rect edges stay seamless
		// at any scale (the EMI twin's fillTransformed shape — a per-leg render wiring copy,
		// not leg geometry; every coordinate still comes from the shared table)
		for (EdgeLayout tLayout : mLayouts) {
			for (Rect tRect : tLayout.wire()) fillTransformed(aGui, tRect, MaterialTreeLayout.WIRE_INK);
			for (Rect tRect : tLayout.arrow()) fillTransformed(aGui, tRect, MaterialTreeLayout.ARROW_INK);
		}
		// the degraded edges' via-labels (the machine-resolved ones carry theirs as hover text)
		for (int i = 0; i < mEdges.size(); i++) {
			EdgeLayout tLayout = mLayouts.get(i);
			if (tLayout.machine() != null) continue;
			label(aGui, tFont, mEdges.get(i).viaLabel(), tLayout.labelX(), tLayout.labelY(), VIA_INK);
		}
		// the "+N" overflow markers (the explicit-not-silent clause)
		for (Overflow tOverflow : mDisplay.overflow())
			label(aGui, tFont, "+" + tOverflow.hidden(),
					MaterialTreeDisplay.overflowX(), MaterialTreeDisplay.overflowY(tOverflow.column()), HEADER_INK);
		// the icons: nodes and byproducts at their +1 cell inset, machine icons bare on the
		// helper's 16px rect (the -1 mount is the old hover-box pad — icon lands on the rect)
		for (Node tNode : mDisplay.nodes())
			item(aGui, tNode.stack(), MaterialTreeDisplay.nodeX(tNode), MaterialTreeDisplay.nodeY(tNode));
		for (int i = 0; i < mEdges.size(); i++) {
			EdgeLayout tLayout = mLayouts.get(i);
			if (tLayout.machine() == null) continue;
			item(aGui, mEdges.get(i).machine(), tLayout.machine().x() - 1, tLayout.machine().y() - 1);
		}
		for (int b = 0; b < mDisplay.byproducts().size(); b++)
			item(aGui, mDisplay.byproducts().get(b).stack(),
					MaterialTreeDisplay.byproductX(b), MaterialTreeDisplay.byproductY());
		// the focus outlines (the invisible slots' replacement highlight)
		for (Node tNode : mDisplay.nodes())
			if (isFocused(tNode.stack()))
				focusBox(aGui, MaterialTreeDisplay.nodeX(tNode), MaterialTreeDisplay.nodeY(tNode));
		for (int b = 0; b < mDisplay.byproducts().size(); b++)
			if (isFocused(mDisplay.byproducts().get(b).stack()))
				focusBox(aGui, MaterialTreeDisplay.byproductX(b), MaterialTreeDisplay.byproductY());
	}

	/**
	 * The hover affordance: self-hits the pointer through {@code unapply} (the API-default
	 * {@code null} {@code getScreenRectangle} = own hit-testing) — the node's stack name,
	 * the machine's via-label, the byproduct's source label.
	 */
	@Override
	public void getTooltip(ITooltipBuilder aTooltip, double aMouseX, double aMouseY) {
		Hit tHit = hitAt(aMouseX, aMouseY);
		if (!tHit.stack().isEmpty()) aTooltip.add(tHit.stack().getHoverName());
		if (!tHit.label().isEmpty()) aTooltip.add(Component.literal(tHit.label()));
	}

	// ------------------------------------------------------------------
	// the one hit domain: shared by this tooltip, the canvas handler's click
	// jump and the offline pins — unapply the pointer, scan the shared table's cells
	// ------------------------------------------------------------------

	/** One hover/click answer: a jumpable stack (nodes, byproducts) and/or a text label (machines). */
	record Hit(ItemStack stack, String label) {
		static final Hit MISS = new Hit(ItemStack.EMPTY, "");
	}

	/** The hit under the page point, or {@link Hit#MISS} — package-private (the handler + the pins). */
	Hit hitAt(double aMouseX, double aMouseY) {
		if (mDisplay == null) return Hit.MISS;
		MaterialTreeViewport.Point tPoint = mView.unapply(aMouseX, aMouseY);
		double tX = tPoint.x(), tY = tPoint.y();
		int tSlot = MaterialTreeLayout.SLOT;
		for (Node tNode : mDisplay.nodes()) {
			int tNX = MaterialTreeDisplay.nodeX(tNode), tNY = MaterialTreeDisplay.nodeY(tNode);
			if (tX >= tNX && tX < tNX + tSlot && tY >= tNY && tY < tNY + tSlot) return new Hit(tNode.stack(), "");
		}
		for (int i = 0; i < mEdges.size(); i++) {
			EdgeLayout tLayout = mLayouts.get(i);
			if (tLayout.machine() == null) continue;
			Rect tBox = tLayout.machine();
			if (tX >= tBox.x() - 1 && tX < tBox.x() + tBox.w() + 1
					&& tY >= tBox.y() - 1 && tY < tBox.y() + tBox.h() + 1)
				return new Hit(ItemStack.EMPTY, mEdges.get(i).viaLabel()); // label-only: no jump face
		}
		for (int b = 0; b < mDisplay.byproducts().size(); b++) {
			int tBX = MaterialTreeDisplay.byproductX(b), tBY = MaterialTreeDisplay.byproductY();
			if (tX >= tBX && tX < tBX + tSlot && tY >= tBY && tY < tBY + tSlot)
				return new Hit(mDisplay.byproducts().get(b).stack(), mDisplay.byproducts().get(b).sourceLabel());
		}
		return Hit.MISS;
	}

	/** Whether any item-stack focus in the group carries the same item (package-private: the pins drive it). */
	boolean isFocused(ItemStack aStack) {
		if (mFocuses == null) return false;
		return mFocuses.getFocuses(VanillaTypes.ITEM_STACK)
				.anyMatch(tFocus -> ItemStack.isSameItem(aStack, tFocus.getTypedValue().getIngredient()));
	}

	// ------------------------------------------------------------------
	// the paint primitives (all coordinates through the viewport — none decided here)
	// ------------------------------------------------------------------

	/** One wire/arrow rect through the viewport as a corner pair, then filled (exclusive x2/y2). */
	private void fillTransformed(GuiGraphics aGui, Rect aRect, int aInk) {
		MaterialTreeViewport.Point tA = mView.apply(aRect.x(), aRect.y());
		MaterialTreeViewport.Point tB = mView.apply(aRect.x() + aRect.w(), aRect.y() + aRect.h());
		aGui.fill(
				(int) Math.round(Math.min(tA.x(), tB.x())), (int) Math.round(Math.min(tA.y(), tB.y())),
				(int) Math.round(Math.max(tA.x(), tB.x())), (int) Math.round(Math.max(tA.y(), tB.y())), aInk);
	}

	/** One label at the pose scale (the unified {@link MaterialTreeLayout#pose} mount). */
	private void label(GuiGraphics aGui, Font aFont, String aText, int aTreeX, int aTreeY, int aInk) {
		GT6MaterialTreeScreen.runAtPose(aGui, MaterialTreeLayout.pose(mView, aTreeX, aTreeY),
				() -> aGui.drawString(aFont, aText, 0, 0, aInk, false));
	}

	/** One 16px item icon centred in its 18px cell at the pose scale (the +1 inset). */
	private void item(GuiGraphics aGui, ItemStack aStack, int aTreeX, int aTreeY) {
		GT6MaterialTreeScreen.runAtPose(aGui, MaterialTreeLayout.pose(mView, aTreeX + 1, aTreeY + 1),
				() -> aGui.renderItem(aStack, 0, 0));
	}

	/** The focus outline: a 1px ring one tree-px outside the cell, corner-pair transformed. */
	private void focusBox(GuiGraphics aGui, int aTreeX, int aTreeY) {
		MaterialTreeViewport.Point tA = mView.apply(aTreeX - 1, aTreeY - 1);
		MaterialTreeViewport.Point tB = mView.apply(aTreeX + MaterialTreeLayout.SLOT + 1, aTreeY + MaterialTreeLayout.SLOT + 1);
		int tX0 = (int) Math.round(tA.x()), tY0 = (int) Math.round(tA.y());
		int tX1 = (int) Math.round(tB.x()), tY1 = (int) Math.round(tB.y());
		aGui.fill(tX0, tY0, tX1, tY0 + 1, FOCUS_INK);
		aGui.fill(tX0, tY1 - 1, tX1, tY1, FOCUS_INK);
		aGui.fill(tX0, tY0, tX0 + 1, tY1, FOCUS_INK);
		aGui.fill(tX1 - 1, tY0, tX1, tY1, FOCUS_INK);
	}
}
