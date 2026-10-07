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

package gregtech6.gui;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import gregtech6.emi.GT6EmiPlugin;
import gregtech6.emi.GT6MaterialTreeNav;
import gregtech6.jei.GT6JeiPlugin;
import gregtech6.recipes.tree.MaterialTreeDisplay;
import gregtech6.recipes.tree.MaterialTreeDisplay.Byproduct;
import gregtech6.recipes.tree.MaterialTreeDisplay.Edge;
import gregtech6.recipes.tree.MaterialTreeDisplay.Overflow;
import gregtech6.recipes.tree.MaterialTreeLayout;
import gregtech6.recipes.tree.MaterialTreeLayout.EdgeLayout;
import gregtech6.recipes.tree.MaterialTreeLayout.Rect;
import gregtech6.recipes.tree.MaterialTreeViewport;

/**
 * The material-tree STANDALONE FULL SCREEN (task nav-s4-tree-screen, the S4 close-out card
 * of the r11-nav-suite) — the escape hatch the user ruling named (「大树走独立屏」): one
 * material's tree rendered by the shared geometry, over the WHOLE window, with real
 * wheel/drag/keys — the EMI page's drag/scroll hard-unreachability and the JEI slot
 * freeze both end here.
 *
 * <p><b>Zero math rewritten</b>: the tree geometry is the same
 * {@link MaterialTreeLayout} plans the two viewer legs render, the pan/zoom state
 * is the S1 {@link MaterialTreeViewport} driven through its four-arg constructor (content
 * = the layout engine's CONTENT BOUNDING BOX, pane = the REAL window — task mattree-viewport-fit,
 * the former 「pane clamped into the content box」 pin died with the base-translation hack it
 * existed for), and every input gesture funnels into the M2 {@link GT6MaterialTreeNav}
 * action table verbatim, so all three consumers navigate identically by construction. Only
 * the shell is new: this class paints the layout's rects through {@link GuiGraphics} and
 * routes the window's events into the table.
 *
 * <p><b>The content box is the engine's, not a constant</b> (task mattree-r2-layout-engine,
 * ADR L1): the screen plans its display once ({@link MaterialTreeLayout#plan}) and feeds
 * the {@code Result} bounding box into the viewport's fit — a tree with few lanes opens on
 * a tight, correctly centred INK box instead of the worst-case 202x206 canvas (the
 * 「墨迹级居中」 leftover the mattree-viewport-fit handoff assigned here). <b>旧钉迁移声明</b>:
 * the former 「content = the fixed {@code MaterialTreeDisplay.WIDTH/HEIGHT} canvas」 wiring
 * died here — the replacement face is {@code Result.width()/height()}; the fixed canvas
 * survives as the viewer PAGES' geometry (the EMI/JEI legs) and the fit/anchor/resize
 * semantics below are the R1 viewport's, untouched.
 *
 * <p><b>The fit pose lives in the viewport now</b> (task mattree-viewport-fit): the
 * viewport's dynamic fit scale magnifies the 202x206 canvas into the real window, centred,
 * and the screen adds NO translation of its own — <b>旧钉迁移声明</b>: the former constant
 * base translation outside the viewport ({@code mBaseX/mBaseY}, compensated separately in
 * the wheel zoom, the hit test and nothing in the render pass — the render/input split that
 * pinned the tree top-left) died here; every coordinate now passes through the viewport's
 * apply/unapply pair alone. The tree layer renders inside a pane-sized scissor (the pane is
 * the visible region, the L2 contract the three legs share).
 *
 * <p><b>Icons and labels ride the viewport's scale</b> (task mattree-item-zoom-pose, the
 * L3 统一 pose 变换原语 clause): every icon and label mounts the GuiGraphics pose stack
 * through {@code MaterialTreeLayout.pose} + {@link #runAtPose} — the ONE primitive the EMI
 * page's labels consume too, so the ink scales with the boxes the corner-pair fills draw
 * (<b>旧钉迁移声明</b>: the former translate-only 16 px icon — the symptom23 ③ zoom 错位 —
 * died here; the replacement pins are the zoom-2x centring in {@code MaterialTreeLayoutTest}
 * and the rig screenshot).
 *
 * <p><b>Node clicks jump to the viewer</b> (所见即所跳): the release-within-slop click
 * resolves through the viewport's unapply inverse and routes by the shared
 * {@link GTViewerJump#preferredViewer()} predicate — the kitchen-NEI seam, EMI 双装优先 —
 * to the R axis (recipes-for-item) of whichever viewer the user actually sees. A drag
 * that travelled past the slop is a pan, never a jump.
 *
 * <p>Lifecycle: the first {@code init} creates the viewport on the fit pose; a re-init
 * (every window resize re-runs {@code init}) hands the live viewport to
 * {@link MaterialTreeViewport#resizeTo} — the anchor survives the resize — and
 * {@link #open} simply replaces the current screen ({@code Minecraft.setScreen} has no
 * barrier from a widget click, the r11 bridge finding).
 *
 * <p>Consumed faces are the loader-neutral common core on both pinned stacks (1.20.1
 * forge / 1.21.1 neoforge): the two scroll generations differ and fork once (the 1.21.1
 * {@code mouseScrolled} carries the horizontal axis), as does {@code renderBackground}
 * (the {@link GTGuiScreen} fork shape). Never loaded on a dedicated server: only the
 * viewer plugins reach this class (the dormant-impl contract).
 */
public class GT6MaterialTreeScreen extends Screen {

	/** The canvas's top-right corner entry cell both viewer legs mount (their button draws the same glyph). */
	public static final int SCREEN_BUTTON_X = MaterialTreeDisplay.WIDTH - 16, SCREEN_BUTTON_Y = 4;

	/** The in-screen nav strip: three 12px cells on a 16px pitch at the window's top-left (the M3 cell language). */
	public static final int BUTTON_X0 = 4, BUTTON_Y0 = 4, BUTTON_CELL = 12, BUTTON_PITCH = 16;
	/** A release within this many travelled px of the press is a click (a jump); past it, a pan. */
	private static final double CLICK_SLOP = 4.0;

	/** The ink faces (the M3 cell language: neutral border, dark fills, bright glyphs, sleeping greys). */
	private static final int BORDER_INK = 0xFF555555, BORDER_HOVER_INK = 0xFF7F7F7F;
	private static final int FILL_INK = 0xFF181818, FILL_HOVER_INK = 0xFF2A2A2A, FILL_SLEEP_INK = 0xFF101010;
	private static final int GLYPH_INK = 0xFFE0E0E0, GLYPH_SLEEP_INK = 0xFF555555;
	private static final int SLOT_BORDER_INK = 0xFF8B8B8B, SLOT_WELL_INK = 0xFF373737;

	final MaterialTreeDisplay mDisplay;
	/** The layout engine's plan of {@link #mDisplay}: the coordinates, the edge plans and the content box (task mattree-r2-layout-engine). */
	final MaterialTreeLayout.Result mLayout;
	MaterialTreeViewport mView;
	private int mPaneWidth, mPaneHeight;
	private boolean mPressed;
	private double mDragDistance;
	/** The jump seam (package-private: the offline test substitutes a recorder). */
	Consumer<ItemStack> mJump = GT6MaterialTreeScreen::openInViewerLive;

	public GT6MaterialTreeScreen(MaterialTreeDisplay aDisplay) {
		super(Component.literal(MaterialTreeDisplay.CATEGORY_TITLE + " - " + MaterialTreeDisplay.materialName(aDisplay.material)));
		mDisplay = aDisplay;
		mLayout = MaterialTreeLayout.plan(aDisplay);
	}

	@Override
	protected void init() {
		super.init();
		navInit(this.width, this.height);
	}

	/**
	 * The viewport wiring (package-private: the offline test drives it with a synthetic
	 * pane) — the S1 four-arg form with the pane being the REAL window: the viewport's
	 * dynamic fit scale magnifies the ENGINE'S CONTENT BOX ({@code mLayout.width/height} —
	 * task mattree-r2-layout-engine) to fill it, centred. The FIRST init creates the
	 * viewport (fit pose); a re-init is a window resize and re-binds the live viewport
	 * through {@link MaterialTreeViewport#resizeTo} — the anchor survives. A degenerate
	 * pane (a minimized window's 0) keeps the last pane: one clamp, no NaN.
	 */
	void navInit(int aPaneWidth, int aPaneHeight) {
		if (aPaneWidth <= 0 || aPaneHeight <= 0) return;
		mPaneWidth = aPaneWidth;
		mPaneHeight = aPaneHeight;
		if (mView == null) mView = new MaterialTreeViewport(mLayout.width(), mLayout.height(), aPaneWidth, aPaneHeight);
		else mView.resizeTo(aPaneWidth, aPaneHeight);
	}

	// ------------------------------------------------------------------
	// input: wheel, drag pan, click-to-jump, keyboard — all through the M2 table
	// ------------------------------------------------------------------

	/** The wheel core (leg-neutral, the two {@code mouseScrolled} generations fork onto it): one notch = the 2x 档位 about the pointer. */
	boolean scroll(double aMouseX, double aMouseY, double aDelta) {
		if (mView == null) return false;
		double tFactor = aDelta > 0 ? GT6MaterialTreeNav.ZOOM_FACTOR : 1.0 / GT6MaterialTreeNav.ZOOM_FACTOR;
		mView.zoomAt(aMouseX, aMouseY, tFactor);
		return true;
	}

	//? if forge {
	@Override
	public boolean mouseScrolled(double aMouseX, double aMouseY, double aDelta) {
		return scroll(aMouseX, aMouseY, aDelta);
	}
	//?} else {
	/*@Override
	public boolean mouseScrolled(double aMouseX, double aMouseY, double aDeltaX, double aDeltaY) {
		return scroll(aMouseX, aMouseY, aDeltaY);
	}
	*///?}

	@Override
	public boolean mouseClicked(double aMouseX, double aMouseY, int aButton) {
		if (mView == null) return false;
		if (aButton != 0) return false;
		GT6MaterialTreeNav.Action tAction = buttonAt(aMouseX, aMouseY);
		if (tAction != null) {
			GT6MaterialTreeNav.handle(mView, tAction, mPaneWidth / 2.0, mPaneHeight / 2.0);
			return true;
		}
		mPressed = true;
		mDragDistance = 0;
		return true;
	}

	@Override
	public boolean mouseDragged(double aMouseX, double aMouseY, int aButton, double aDragX, double aDragY) {
		if (mView == null || !mPressed || aButton != 0) return false;
		mDragDistance += Math.abs(aDragX) + Math.abs(aDragY);
		mView.pan(aDragX, aDragY);
		return true;
	}

	@Override
	public boolean mouseReleased(double aMouseX, double aMouseY, int aButton) {
		boolean tWasPress = mPressed;
		mPressed = false;
		if (mView == null || !tWasPress || aButton != 0) return false;
		if (mDragDistance <= CLICK_SLOP) {
			ItemStack tStack = itemAt(aMouseX, aMouseY);
			if (!tStack.isEmpty()) mJump.accept(tStack);
		}
		return true;
	}

	@Override
	public boolean keyPressed(int aKeyCode, int aScanCode, int aModifiers) {
		if (mView != null) {
			GT6MaterialTreeNav.Action tAction = GT6MaterialTreeNav.keyToAction(aKeyCode);
			if (tAction != null) return GT6MaterialTreeNav.handle(mView, tAction, mPaneWidth / 2.0, mPaneHeight / 2.0);
		}
		return super.keyPressed(aKeyCode, aScanCode, aModifiers);
	}

	@Override
	public boolean isPauseScreen() {
		return false; // a viewer, not a pause menu
	}

	// ------------------------------------------------------------------
	// the seams: strip cells, slot hit, open, viewer jump
	// ------------------------------------------------------------------

	/** The strip cell under the pointer ({@code null} = none): +/−/R at the window's top-left. */
	private GT6MaterialTreeNav.Action buttonAt(double aMouseX, double aMouseY) {
		if (aMouseY < BUTTON_Y0 || aMouseY >= BUTTON_Y0 + BUTTON_CELL) return null;
		if (aMouseX >= BUTTON_X0 && aMouseX < BUTTON_X0 + BUTTON_CELL) return GT6MaterialTreeNav.Action.ZOOM_IN;
		if (aMouseX >= BUTTON_X0 + BUTTON_PITCH && aMouseX < BUTTON_X0 + BUTTON_PITCH + BUTTON_CELL)
			return GT6MaterialTreeNav.Action.ZOOM_OUT;
		if (aMouseX >= BUTTON_X0 + 2 * BUTTON_PITCH && aMouseX < BUTTON_X0 + 2 * BUTTON_PITCH + BUTTON_CELL)
			return GT6MaterialTreeNav.Action.RESET;
		return null;
	}

	/** The node/byproduct slot under the pointer through the unapply inverse ({@code EMPTY} = a miss). */
	ItemStack itemAt(double aMouseX, double aMouseY) {
		MaterialTreeViewport.Point tTree = mView.unapply(aMouseX, aMouseY);
		for (MaterialTreeDisplay.Node tNode : mDisplay.nodes()) {
			double tX = mLayout.nodeX(tNode), tY = mLayout.nodeY(tNode);
			if (tTree.x() >= tX && tTree.x() < tX + MaterialTreeLayout.SLOT
					&& tTree.y() >= tY && tTree.y() < tY + MaterialTreeLayout.SLOT) return tNode.stack();
		}
		for (int i = 0; i < mDisplay.byproducts().size(); i++) {
			double tX = mLayout.byproductX(i), tY = mLayout.byproductY();
			if (tTree.x() >= tX && tTree.x() < tX + MaterialTreeLayout.SLOT
					&& tTree.y() >= tY && tTree.y() < tY + MaterialTreeLayout.SLOT) return mDisplay.byproducts().get(i).stack();
		}
		return ItemStack.EMPTY;
	}

	/** The entry face both viewer legs' corner cells call — replaces the current screen with the standalone tree. */
	public static void open(MaterialTreeDisplay aDisplay) {
		Minecraft.getInstance().setScreen(new GT6MaterialTreeScreen(aDisplay));
	}

	/** The live arm — the ONE predicate ({@link GTViewerJump#preferredViewer()}, EMI 双装优先) routes the jump. */
	static void openInViewerLive(ItemStack aStack) {
		openInViewer(aStack, GTViewerJump.preferredViewer());
	}

	/**
	 * The pure arm — the routing table over a preferred-viewer answer ({@code "emi"} /
	 * {@code "jei"} / {@code null}): the same shape as {@link GTViewerJump}'s own table,
	 * item-level. The R axis (recipes-for-item) both legs, the single-destination precedent.
	 */
	static boolean openInViewer(ItemStack aStack, String aPreferredViewer) {
		if (aStack == null || aStack.isEmpty() || aPreferredViewer == null) return false;
		if ("emi".equals(aPreferredViewer)) return GT6EmiPlugin.openItemPage(aStack);
		if ("jei".equals(aPreferredViewer)) return GT6JeiPlugin.openItemPage(aStack);
		return false;
	}

	// ------------------------------------------------------------------
	// render: the shared layout plans through the viewport
	// ------------------------------------------------------------------

	@Override
	public void render(GuiGraphics aGui, int aMouseX, int aMouseY, float aPartialTick) {
		//? if forge {
		renderBackground(aGui);
		//?} else {
		/*renderBackground(aGui, aMouseX, aMouseY, aPartialTick);
		*///?}
		if (mView == null) return;
		Font tFont = Minecraft.getInstance().font;
		List<Edge> tEdges = mDisplay.edges();
		List<EdgeLayout> tLayouts = mLayout.edges();
		// the tree layer renders inside the pane-sized scissor (the pane is the visible
		// region — the L2 contract; the strip and the tooltip stay outside it)
		aGui.enableScissor(0, 0, this.width, this.height);
		// the wire layer leads (under the slots — the viewers' z-order contract)
		for (EdgeLayout tLayout : tLayouts) {
			for (Rect tRect : tLayout.wire()) fillTransformed(aGui, tRect, MaterialTreeLayout.WIRE_INK);
			for (Rect tRect : tLayout.arrow()) fillTransformed(aGui, tRect, MaterialTreeLayout.ARROW_INK);
		}
		// the labels (the EMI twin's render-time-transform form — the same label set)
		drawLabel(aGui, tFont, MaterialTreeDisplay.materialName(mDisplay.material), 4, 4, 0xFF000000);
		if (mLayout.byproductBand()) // the hanging band's header — only when the band exists (the content-driven canvas has no room for a floating label)
			drawLabel(aGui, tFont, MaterialTreeDisplay.BYPRODUCT_HEADER,
					mLayout.overflowX(), mLayout.byproductHeaderY(), 0xFF000000);
		for (int i = 0; i < tEdges.size(); i++) {
			EdgeLayout tLayout = tLayouts.get(i);
			if (tLayout.machine() != null) continue; // the via-label lives on the machine tooltip
			drawLabel(aGui, tFont, tEdges.get(i).viaLabel(), tLayout.labelX(), tLayout.labelY(), 0xFF555555);
		}
		for (Overflow tOverflow : mDisplay.overflow())
			drawLabel(aGui, tFont, "+" + tOverflow.hidden(),
					mLayout.overflowX(), mLayout.overflowY(tOverflow.column()), 0xFF000000);
		// the slots: nodes and byproducts in cells, machine icons bare (the drawBack(false) face)
		for (MaterialTreeDisplay.Node tNode : mDisplay.nodes())
			drawSlot(aGui, tNode.stack(), mLayout.nodeX(tNode), mLayout.nodeY(tNode));
		for (int i = 0; i < tEdges.size(); i++) {
			EdgeLayout tLayout = tLayouts.get(i);
			if (tLayout.machine() != null)
				drawItem(aGui, tEdges.get(i).machine(), tLayout.machine().x() - 1, tLayout.machine().y() - 1);
		}
		int i = 0;
		for (Byproduct tByproduct : mDisplay.byproducts())
			drawSlot(aGui, tByproduct.stack(), mLayout.byproductX(i++), mLayout.byproductY());
		aGui.disableScissor();
		drawStrip(aGui, tFont, aMouseX, aMouseY);
		// the hovered slot's own tooltip (the native face)
		ItemStack tHovered = itemAt(aMouseX, aMouseY);
		if (!tHovered.isEmpty()) aGui.renderTooltip(tFont, tHovered, aMouseX, aMouseY);
	}

	/** One wire/arrow rect through the viewport as a corner pair, then filled (the M2 {@code fillTransformed}). */
	private void fillTransformed(GuiGraphics aGui, Rect aRect, int aInk) {
		MaterialTreeViewport.Point tA = mView.apply(aRect.x(), aRect.y());
		MaterialTreeViewport.Point tB = mView.apply(aRect.x() + aRect.w(), aRect.y() + aRect.h());
		aGui.fill(
				(int) Math.round(Math.min(tA.x(), tB.x())), (int) Math.round(Math.min(tA.y(), tB.y())),
				(int) Math.round(Math.max(tA.x(), tB.x())), (int) Math.round(Math.max(tA.y(), tB.y())), aInk);
	}

	/** One label through the viewport at the pose scale (render-time transform — a frozen coordinate would not follow the pan). */
	private void drawLabel(GuiGraphics aGui, Font aFont, String aText, int aTreeX, int aTreeY, int aInk) {
		runAtPose(aGui, MaterialTreeLayout.pose(mView, aTreeX, aTreeY), () -> aGui.drawString(aFont, aText, 0, 0, aInk, false));
	}

	/** One 18x18 slot: the cell as a corner-pair rect (already box-scaled), the item at the pose scale. */
	private void drawSlot(GuiGraphics aGui, ItemStack aStack, int aTreeX, int aTreeY) {
		MaterialTreeViewport.Point tA = mView.apply(aTreeX, aTreeY);
		MaterialTreeViewport.Point tB = mView.apply(aTreeX + MaterialTreeLayout.SLOT, aTreeY + MaterialTreeLayout.SLOT);
		int tX = (int) Math.round(tA.x()), tY = (int) Math.round(tA.y());
		int tX2 = (int) Math.round(tB.x()), tY2 = (int) Math.round(tB.y());
		aGui.fill(tX, tY, tX2, tY2, SLOT_BORDER_INK);
		if (tX2 - tX > 2 && tY2 - tY > 2) aGui.fill(tX + 1, tY + 1, tX2 - 1, tY2 - 1, SLOT_WELL_INK);
		drawItem(aGui, aStack, aTreeX, aTreeY);
	}

	/**
	 * One item icon through the viewport at the POSE scale (the unified
	 * {@link MaterialTreeLayout#pose} primitive): the +1 inset centres the 16 px icon in the
	 * 18 px box in TREE space, and the pose scale grows the icon with the box.
	 * <b>旧钉迁移声明</b>: the former translate-only drawItem (16 px icon against the
	 * 18×scale slot box — the symptom23 ③ zoom 错位) died here; the replacement pins are the
	 * zoom-2x centring in {@code MaterialTreeLayoutTest} and the rig screenshot
	 * (the ADR §6 观感硬闸).
	 */
	private void drawItem(GuiGraphics aGui, ItemStack aStack, int aTreeX, int aTreeY) {
		runAtPose(aGui, MaterialTreeLayout.pose(mView, aTreeX + 1, aTreeY + 1), () -> aGui.renderItem(aStack, 0, 0));
	}

	/**
	 * The GuiGraphics mount of the unified pose (the 三腿统一变换原语 wiring, shared with the
	 * EMI page's labels drawable): translate to the pose origin, scale by its factor, draw in
	 * tree-local coordinates. The float casts are the common-denominator overload on both
	 * pinned stacks (1.20.1 / 1.21.1 {@code PoseStack.translate(float..)/scale(float..)}).
	 */
	public static void runAtPose(GuiGraphics aGui, MaterialTreeLayout.Pose aPose, Runnable aDraw) {
		aGui.pose().pushPose();
		aGui.pose().translate((float) aPose.x(), (float) aPose.y(), 0.0F);
		float tScale = (float) aPose.scale();
		aGui.pose().scale(tScale, tScale, 1.0F);
		aDraw.run();
		aGui.pose().popPose();
	}

	/** The nav strip (the M3 cell language): +/−/R, the zoom cells sleeping at the live fit floor / ceiling. */
	private void drawStrip(GuiGraphics aGui, Font aFont, int aMouseX, int aMouseY) {
		drawCell(aGui, aFont, "+", BUTTON_X0, aMouseX, aMouseY, mView.scale() < MaterialTreeViewport.MAX_SCALE);
		drawCell(aGui, aFont, "-", BUTTON_X0 + BUTTON_PITCH, aMouseX, aMouseY, mView.scale() > mView.minScale());
		drawCell(aGui, aFont, "R", BUTTON_X0 + 2 * BUTTON_PITCH, aMouseX, aMouseY, true);
	}

	private void drawCell(GuiGraphics aGui, Font aFont, String aGlyph, int aX, int aMouseX, int aMouseY, boolean aLive) {
		boolean tHover = aLive && aMouseX >= aX && aMouseX < aX + BUTTON_CELL
				&& aMouseY >= BUTTON_Y0 && aMouseY < BUTTON_Y0 + BUTTON_CELL;
		aGui.fill(aX, BUTTON_Y0, aX + BUTTON_CELL, BUTTON_Y0 + BUTTON_CELL, tHover ? BORDER_HOVER_INK : BORDER_INK);
		aGui.fill(aX + 1, BUTTON_Y0 + 1, aX + BUTTON_CELL - 1, BUTTON_Y0 + BUTTON_CELL - 1,
				tHover ? FILL_HOVER_INK : aLive ? FILL_INK : FILL_SLEEP_INK);
		aGui.drawString(aFont, aGlyph, aX + 3, BUTTON_Y0 + 2, aLive ? GLYPH_INK : GLYPH_SLEEP_INK, false);
	}
}
