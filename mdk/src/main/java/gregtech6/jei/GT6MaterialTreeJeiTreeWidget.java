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
import gregtech6.recipes.tree.MaterialTreeDisplay.Node;
import gregtech6.recipes.tree.MaterialTreeLayout;
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
 * <p><b>The EMI recipe-tree ink</b> (task mattree-emi-tree-rework): white two-segment
 * connectors, the 42x20 process-node box outlines (BoMScreen.java:697-717 outline, no
 * fill), the machine icons in their box cells (:719-728's category-icon seat) and the
 * bare-leaf ore roots — the arrowheads, the edge via-labels and the "+N" overflow markers
 * are retired with the old geometry; the via-info lives in the node tooltips (this class's
 * {@link #getTooltip}, the EMI RecipeTooltipComponent face :615-647).
 *
 * <p><b>The rebuilt affordances</b> (what the invisible slots no longer provide):
 * <ul>
 * <li><b>hover</b>: {@link #getTooltip} self-hits through {@code unapply} (the pointer
 *     mapped back into tree space) and answers the hovered node's stack name + via-label
 *     and the byproducts' source label — {@code getScreenRectangle} stays the API default
 *     {@code null} = own hit-testing (IRecipeWidget javadoc);</li>
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

	/** The header ink (the batch-1 NEI black). */
	private static final int HEADER_INK = 0xFF000000;
	/** The focus outline: one white px ring, one tree-px outside the 18px cell. */
	private static final int FOCUS_INK = 0xFFFFFFFF;

	private final MaterialTreeDisplay mDisplay;
	private final MaterialTreeViewport mView;
	/**
	 * The layout engine's plan (task mattree-r3-nav-unify): the tree geometry authority —
	 * the same {@code Result} the EMI twin and the standalone screen consume; the Display
	 * statics' nominal frame seat is the category frame only.
	 */
	private final MaterialTreeLayout.Result mLayout;
	private final IFocusGroup mFocuses;

	/** Package-private: the category is the production builder (the extras closure). */
	GT6MaterialTreeJeiTreeWidget(MaterialTreeDisplay aDisplay, MaterialTreeViewport aView, IFocusGroup aFocuses) {
		mDisplay = aDisplay;
		mView = aView;
		mFocuses = aFocuses;
		mLayout = aDisplay == null ? null : MaterialTreeLayout.plan(aDisplay);
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
		label(aGui, tFont, MaterialTreeDisplay.materialName(mDisplay.material), 4, MaterialTreeDisplay.HEADER_Y, HEADER_INK);
		if (mLayout.byproductBand()) // the hanging band's header — only when the band exists (the EMI twin's content-driven face)
			label(aGui, tFont, MaterialTreeDisplay.BYPRODUCT_HEADER,
					mLayout.byproductX(0), mLayout.byproductHeaderY(), HEADER_INK);
		// the wires: corner-pair transforms so shared rect edges stay seamless at any scale
		// (the EMI twin's fillTransformed shape — a per-leg render wiring copy, not leg
		// geometry; every coordinate still comes from the shared table)
		for (MaterialTreeLayout.EdgeLayout tLayout : mLayout.edges())
			for (Rect tRect : tLayout.wire()) fillTransformed(aGui, tRect, MaterialTreeLayout.WIRE_INK);
		// the process-node box outlines (the EMI recipe-node form — outline only, no fill)
		for (Node tNode : mDisplay.nodes()) {
			Rect tBox = mLayout.nodeBox(tNode);
			if (tBox != null) boxOutline(aGui, tBox);
		}
		// the machine icons in their box cells (the EMI category-icon seat), then the item
		// icons in their cells at the +1 inset
		for (Node tNode : mDisplay.nodes()) {
			Rect tCell = mLayout.nodeMachineCell(tNode);
			if (tCell == null) continue;
			ItemStack tMachine = producingMachine(tNode);
			if (tMachine != null) item(aGui, tMachine, tCell.x(), tCell.y());
		}
		for (Node tNode : mDisplay.nodes())
			item(aGui, tNode.stack(), mLayout.nodeItemCell(tNode).x(), mLayout.nodeItemCell(tNode).y());
		for (int b = 0; b < mDisplay.byproducts().size(); b++)
			item(aGui, mDisplay.byproducts().get(b).stack(),
					mLayout.byproductX(b) - MaterialTreeLayout.SLOT / 2, mLayout.byproductY() - MaterialTreeLayout.SLOT / 2);
		// the focus outlines (the invisible slots' replacement highlight)
		for (Node tNode : mDisplay.nodes())
			if (isFocused(tNode.stack()))
				focusBox(aGui, mLayout.nodeItemCell(tNode).x(), mLayout.nodeItemCell(tNode).y());
		for (int b = 0; b < mDisplay.byproducts().size(); b++)
			if (isFocused(mDisplay.byproducts().get(b).stack()))
				focusBox(aGui, mLayout.byproductX(b) - MaterialTreeLayout.SLOT / 2, mLayout.byproductY() - MaterialTreeLayout.SLOT / 2);
	}

	/** The producing edge's resolved machine stack for one node, or null (the degrade face). */
	private ItemStack producingMachine(Node aNode) {
		for (var tEdge : mDisplay.edges())
			if (tEdge.to() == aNode.prefix() && !tEdge.machine().isEmpty()) return tEdge.machine();
		return null;
	}

	/** The merged producing-map labels of one node (its incoming edges, display order, dedup). */
	private String producingLabel(Node aNode) {
		java.util.Set<String> tNames = new java.util.LinkedHashSet<>();
		for (var tEdge : mDisplay.edges()) if (tEdge.to() == aNode.prefix()) tNames.addAll(tEdge.mapNames());
		return tNames.isEmpty() ? "" : MaterialTreeDisplay.viaLabel(List.copyOf(tNames));
	}

	/**
	 * The hover affordance: self-hits the pointer through {@code unapply} (the API-default
	 * {@code null} {@code getScreenRectangle} = own hit-testing) — the node's stack name +
	 * via-label, the byproduct's source label.
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

	/** One hover/click answer: a jumpable stack (nodes, byproducts) and/or a text label. */
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
			Rect tCell = mLayout.nodeItemCell(tNode);
			if (tX >= tCell.x() && tX < tCell.x() + tSlot && tY >= tCell.y() && tY < tCell.y() + tSlot) {
				String tLabel = producingLabel(tNode);
				return new Hit(tNode.stack(), tLabel);
			}
			// the machine icon answers its via-label only (no jump face — the EMI category-icon
			// precedent, BoMScreen.java:769-772 answers the category hover)
			Rect tMachineCell = mLayout.nodeMachineCell(tNode);
			if (tMachineCell != null && tX >= tMachineCell.x() && tX < tMachineCell.x() + tSlot
					&& tY >= tMachineCell.y() && tY < tMachineCell.y() + tSlot)
				return new Hit(ItemStack.EMPTY, producingLabel(tNode));
		}
		for (int b = 0; b < mDisplay.byproducts().size(); b++) {
			int tBX = mLayout.byproductX(b) - MaterialTreeLayout.SLOT / 2, tBY = mLayout.byproductY() - MaterialTreeLayout.SLOT / 2;
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

	/** One wire rect through the viewport as a corner pair, then filled (exclusive x2/y2). */
	private void fillTransformed(GuiGraphics aGui, Rect aRect, int aInk) {
		MaterialTreeViewport.Point tA = mView.apply(aRect.x(), aRect.y());
		MaterialTreeViewport.Point tB = mView.apply(aRect.x() + aRect.w(), aRect.y() + aRect.h());
		aGui.fill(
				(int) Math.round(Math.min(tA.x(), tB.x())), (int) Math.round(Math.min(tA.y(), tB.y())),
				(int) Math.round(Math.max(tA.x(), tB.x())), (int) Math.round(Math.max(tA.y(), tB.y())), aInk);
	}

	/** The EMI node box outline: four white fills through the viewport (BoMScreen.java:714-717, no fill inside). */
	private void boxOutline(GuiGraphics aGui, Rect aBox) {
		fillTransformed(aGui, new Rect(aBox.x(), aBox.y(), aBox.w(), 1), MaterialTreeLayout.WIRE_INK);
		fillTransformed(aGui, new Rect(aBox.x(), aBox.y() + aBox.h() - 1, aBox.w(), 1), MaterialTreeLayout.WIRE_INK);
		fillTransformed(aGui, new Rect(aBox.x(), aBox.y(), 1, aBox.h()), MaterialTreeLayout.WIRE_INK);
		fillTransformed(aGui, new Rect(aBox.x() + aBox.w() - 1, aBox.y(), 1, aBox.h()), MaterialTreeLayout.WIRE_INK);
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
