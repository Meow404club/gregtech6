package gregtech6.emi;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;

import gregapi.oredict.OreDictPrefix;
import gregtech6.gui.GT6MaterialTreeScreen;
import gregtech6.recipes.tree.MaterialTreeDisplay;
import gregtech6.recipes.tree.MaterialTreeDisplay.Byproduct;
import gregtech6.recipes.tree.MaterialTreeDisplay.Edge;
import gregtech6.recipes.tree.MaterialTreeDisplay.Node;
import gregtech6.recipes.tree.MaterialTreeLayout;
import gregtech6.recipes.tree.MaterialTreeLayout.EdgeLayout;
import gregtech6.recipes.tree.MaterialTreeLayout.Rect;
import gregtech6.recipes.tree.MaterialTreeViewport;
import gregtech6.registry.GTMaterialItems;

/**
 * One material's tree as an EMI recipe (task debt-material-tree-b; the EMI recipe-tree
 * form task mattree-emi-tree-rework) — wrapping the viewer-neutral {@link MaterialTreeDisplay}
 * and rendering the SAME {@link MaterialTreeLayout} plans as the JEI twin (the 双 viewer 一坐标表
 * clause). The page's look is the EMI recipe tree's own (BoMScreen.java, tmp/harvest/
 * emi-1.20.1-src; 1.21.1 byte-identical on every cited face): depth bands top-down, the
 * process nodes as the 42x20 box outline carrying the machine icon beside the item icon
 * (:697-728), the ore roots as bare-leaf icons, the connectors as the white two-segment
 * Manhattan lines (:675-694) — no arrowheads, no edge labels (the 糊一坨 treatment: the
 * via-info lives in the producing node's tooltip, EMI's own recipe-tooltip face :615-647).
 * The U/R faces mirror the JEI slot split: the band-0 ore roots ride {@link #getInputs()}
 * (U on an ore item finds its tree), the downstream nodes and the byproduct band ride
 * {@link #getOutputs()}.
 *
 * <p>Rendering order is load-bearing, twice: the wire/box/label drawable is added FIRST so
 * the ink renders UNDER the slots (a run crossing a box hides behind the outline in both
 * legs), and — <b>the add-order contract</b> (task mattree-open-fix, the open-screen
 * regression's root chain; javap emi-forge-1.1.24 RecipeScreen.m_6374_ bytecode evidence) —
 * EVERY native click face (the three nav buttons, the S4 corner entry, the slider) must be
 * added BEFORE the {@link GT6MaterialTreeNavWidget} canvas: EMI's RecipeScreen.mouseClicked
 * dispatches forward over the widget list and the first bounds-hit non-SlotWidget that
 * answers true consumes the press, so a full-page canvas added earlier would starve the
 * buttons. The pin lives in {@code GT6MaterialTreeNavTest#everyNativeClickFacePrecedesTheNavCanvas}.
 *
 * <p>Nav suite (task nav-m2-emi, consuming the S1 {@link MaterialTreeViewport}): every page
 * open creates a FRESH viewport and wires it through all the widgets — closing the page
 * drops the widgets, the whole apply-unapply 闭环 (no static state). The pointer seam (task
 * mattree-emi-panzoom): the nav canvas also implements the vendored MUI
 * {@code EmiInteractionSink}, so wheel zooms pointer-anchored and an empty-canvas drag pans
 * — both gated to the tree canvas rect, with slot presses yielded to the native U/R
 * faces (the canvas receives the page's slot list in add order and replays EMI's own
 * bounds test).
 *
 * <p><b>The page IS the engine's content box</b> (task mattree-r3-nav-unify): the display
 * plans ONCE in the constructor and the {@link MaterialTreeLayout.Result} is the single
 * geometry authority — the page dims ({@link #getDisplayWidth}/{@link #getDisplayHeight}),
 * the viewport content box, the canvas rect, the zoom anchor, the strip row and every
 * slot/wire/label coordinate flow from it. Since the EMI rework the content box is
 * UNCAPPED (no per-band caps): a deep tree opens a tall/wide page and the pan/zoom
 * viewport carries it (EMI stacks per-recipe heights natively, RecipeTab.java:41/:62).
 *
 * <p>Id stability: {@code gt6:material_tree/<snake(material)>} — the snake_case face is the
 * registered-item id convention (GTMaterialItems.itemIdOf), so the id survives a material's
 * localisation.
 */
public class GT6MaterialTreeEmiRecipe implements EmiRecipe {

	/** The nav control strip below the tree canvas (12px buttons + an 8px track on a 20px band). */
	public static final int CONTROL_STRIP_H = 20;
	/**
	 * The strip's X axis and the JEI page's strip ROW (12px cells, 4px gaps, on the
	 * nominal frame — task mattree-r3-nav-unify: the EMI page derives its own strip row
	 * from the plan height, these Y constants are the JEI category frame's seat, the
	 * category dims being per-category and unable to see a recipe).
	 */
	public static final int BUTTON_X0 = 4, BUTTON_PITCH = 16, BUTTON_Y = MaterialTreeDisplay.HEIGHT + 4;
	/** The click-track zoom slider: a 138x8 track, vertically centred in the strip (the same JEI seat on Y). */
	public static final int SLIDER_X = 56, SLIDER_Y = MaterialTreeDisplay.HEIGHT + 6;
	public static final int SLIDER_W = 138, SLIDER_H = 8;

	public final MaterialTreeDisplay mDisplay;
	/**
	 * The layout engine's plan: the page's single geometry authority — dims, viewport box,
	 * anchor and every coordinate flow from it.
	 */
	public final MaterialTreeLayout.Result mLayout;
	private final ResourceLocation mId;
	private final List<EmiIngredient> mInputs;
	private final List<EmiStack> mOutputs;

	public GT6MaterialTreeEmiRecipe(MaterialTreeDisplay aDisplay) {
		mDisplay = aDisplay;
		mLayout = MaterialTreeLayout.plan(aDisplay);
		//? if forge {
		mId = ResourceLocation.fromNamespaceAndPath("gt6", MaterialTreeDisplay.CATEGORY_UID_PATH + "/" + GTMaterialItems.snakeCase(aDisplay.material.mNameInternal));
		//?} else {
		/*mId = ResourceLocation.fromNamespaceAndPath("gt6", MaterialTreeDisplay.CATEGORY_UID_PATH + "/" + GTMaterialItems.snakeCase(aDisplay.material.mNameInternal));
		 *///?}
		mInputs = new ArrayList<>();
		mOutputs = new ArrayList<>();
		for (Node tNode : aDisplay.nodes()) {
			if (tNode.band() == 0) mInputs.add(EmiStack.of(tNode.stack()));
			else mOutputs.add(EmiStack.of(tNode.stack()));
		}
		for (Byproduct tByproduct : aDisplay.byproducts()) {
			mOutputs.add(EmiStack.of(tByproduct.stack()));
		}
	}

	@Override
	public GT6MaterialTreeEmiCategory getCategory() {
		return GT6MaterialTreeEmiCategory.INSTANCE;
	}

	@Override
	public ResourceLocation getId() {
		return mId;
	}

	@Override
	public List<EmiIngredient> getInputs() {
		return mInputs;
	}

	@Override
	public List<EmiStack> getOutputs() {
		return mOutputs;
	}

	@Override
	public int getDisplayWidth() {
		return mLayout.width();
	}

	@Override
	public int getDisplayHeight() {
		return mLayout.height() + CONTROL_STRIP_H;
	}

	/** No recipe tree this card: the transfer/ghost face stays batch 4 (the batch-1 不做 clause). */
	@Override
	public boolean supportsRecipeTree() {
		return false;
	}

	@Override
	public void addWidgets(WidgetHolder aWidgets) {
		// the nav viewport: FRESH per page-open (the apply-unapply 闭环). The content box is
		// the ENGINE'S: content == pane on the plan's own box — fit 1:1.
		int tCanvasW = mLayout.width(), tCanvasH = mLayout.height();
		MaterialTreeViewport tView = new MaterialTreeViewport(tCanvasW, tCanvasH, tCanvasW, tCanvasH);
		// the shared zoom anchor: the canvas centre (buttons, keys and slider all focus it)
		double tCx = tCanvasW / 2.0, tCy = tCanvasH / 2.0;
		// the strip row: the page's own band below the plan box (the statics above are the JEI frame's seat)
		int tButtonY = tCanvasH + 4, tSliderY = tCanvasH + 6;

		// the nav canvas's slot yield set: every TransformSlot in add order, so a canvas press
		// can replay EMI's own first-hit bounds test and yield to the native slot faces
		List<GT6MaterialTreeTransformSlot> tSlots = new ArrayList<>();

		// FIRST: the ink layer — wires, box outlines and the two headers, one drawable over
		// the whole page (GuiGraphics.fill per rect — no diagonal primitive on 1.20.1), so it
		// renders under the slots. The labels live here because their positions must follow
		// the viewport at RENDER time (a TextWidget freezes its coordinate at add time).
		List<EdgeLayout> tLayouts = mLayout.edges();
		List<Rect> tWires = new ArrayList<>(), tBoxes = new ArrayList<>();
		for (EdgeLayout tLayout : tLayouts) tWires.addAll(tLayout.wire());
		for (Node tNode : mDisplay.nodes()) {
			Rect tBox = mLayout.nodeBox(tNode);
			if (tBox != null) tBoxes.add(tBox);
		}
		record Label(String text, int x, int y, int color) {}
		List<Label> tLabels = new ArrayList<>();
		tLabels.add(new Label(MaterialTreeDisplay.materialName(mDisplay.material), 4, MaterialTreeDisplay.HEADER_Y, 0xFF000000));
		if (mLayout.byproductBand()) // the hanging band's header — only when the band exists
			tLabels.add(new Label(MaterialTreeDisplay.BYPRODUCT_HEADER, mLayout.byproductX(0), mLayout.byproductHeaderY(), 0xFF000000));
		aWidgets.addDrawable(0, 0, tCanvasW, tCanvasH + CONTROL_STRIP_H, (aGuiGraphics, aMouseX, aMouseY, aDelta) -> {
			// corner-pair transforms: shared rect edges stay seamless at any scale
			for (Rect tRect : tWires) fillTransformed(aGuiGraphics, tView, tRect, MaterialTreeLayout.WIRE_INK);
			for (Rect tRect : tBoxes) drawBox(aGuiGraphics, tView, tRect);
			for (Label tLabel : tLabels) {
				GT6MaterialTreeScreen.runAtPose(aGuiGraphics, MaterialTreeLayout.pose(tView, tLabel.x(), tLabel.y()),
						() -> aGuiGraphics.drawString(Minecraft.getInstance().font, tLabel.text(), 0, 0, tLabel.color(), false));
			}
		});

		// the node slots: the item cell of every node (the EMI leaf/recipe-node icon face),
		// carrying the merged producing-maps tooltip on the process nodes; then the machine
		// icons in their box cells (drawBack false — the bare 16x16 icon face, EMI's
		// category-icon seat), tooltip = the same via-label
		java.util.Map<OreDictPrefix, List<String>> tByNode = producingMaps();
		for (Node tNode : mDisplay.nodes()) {
			Rect tCell = mLayout.nodeItemCell(tNode);
			GT6MaterialTreeTransformSlot tSlot = new GT6MaterialTreeTransformSlot(EmiStack.of(tNode.stack()), tCell.x(), tCell.y(), tView);
			tSlots.add(tSlot);
			List<String> tMaps = tByNode.get(tNode.prefix());
			if (tMaps != null) aWidgets.add(tSlot).appendTooltip(Component.literal(MaterialTreeDisplay.viaLabel(tMaps)));
			else aWidgets.add(tSlot);
		}
		for (Node tNode : mDisplay.nodes()) {
			Rect tCell = mLayout.nodeMachineCell(tNode);
			if (tCell == null) continue;
			ItemStackMachine tMachine = machineOf(tNode);
			if (tMachine == null || tMachine.stack().isEmpty()) continue;
			GT6MaterialTreeTransformSlot tSlot = new GT6MaterialTreeTransformSlot(EmiStack.of(tMachine.stack()), tCell.x(), tCell.y(), tView);
			tSlots.add(tSlot);
			aWidgets.add(tSlot.drawBack(false))
					.appendTooltip(Component.literal(tMachine.label()));
		}
		int b = 0;
		for (Byproduct tByproduct : mDisplay.byproducts()) {
			GT6MaterialTreeTransformSlot tByproductSlot = new GT6MaterialTreeTransformSlot(EmiStack.of(tByproduct.stack()),
					mLayout.byproductX(b) - MaterialTreeLayout.SLOT / 2,
					mLayout.byproductY() - MaterialTreeLayout.SLOT / 2, tView);
			tSlots.add(tByproductSlot);
			aWidgets.add(tByproductSlot)
					.appendTooltip(Component.literal(tByproduct.sourceLabel()));
			b++;
		}

		// the nav control strip (the EMI 1.1.24 native button seam — WidgetHolder.addButton onto
		// the blank cell u=72 v=0 of emi buttons.png): the zoom buttons sleep at the S1
		// floor/ceiling, reset never does. ADD-ORDER CONTRACT: these native click faces — and
		// the S4 corner entry below — go in BEFORE the NavWidget canvas (mattree-open-fix).
		aWidgets.addButton(BUTTON_X0, tButtonY, 12, 12, 72, 0,
				() -> tView.scale() < MaterialTreeViewport.MAX_SCALE,
				(aMx, aMy, aBtn) -> GT6MaterialTreeNav.handle(tView, GT6MaterialTreeNav.Action.ZOOM_IN, tCx, tCy));
		aWidgets.addButton(BUTTON_X0 + BUTTON_PITCH, tButtonY, 12, 12, 72, 0,
				() -> tView.scale() > MaterialTreeViewport.MIN_SCALE,
				(aMx, aMy, aBtn) -> GT6MaterialTreeNav.handle(tView, GT6MaterialTreeNav.Action.ZOOM_OUT, tCx, tCy));
		aWidgets.addButton(BUTTON_X0 + 2 * BUTTON_PITCH, tButtonY, 12, 12, 72, 0,
				() -> true,
				(aMx, aMy, aBtn) -> GT6MaterialTreeNav.handle(tView, GT6MaterialTreeNav.Action.RESET, tCx, tCy));
		aWidgets.add(new GT6MaterialTreeSliderWidget(tView, SLIDER_X, tSliderY, SLIDER_W, SLIDER_H, tCx, tCy));
		// the S4 escape hatch (task nav-s4-tree-screen): the canvas's top-right corner cell
		// opens the standalone full-screen tree (GT6MaterialTreeScreen), the r11 大树走独立屏
		// ruling's face. The blank native cell (u=72 v=0, the strip convention) carries a "T"
		// glyph drawable above it; the click replaces the EMI page with the screen (setScreen
		// has no barrier from a widget click). The corner is the page's own top-right (the
		// static SCREEN_BUTTON_X is the JEI frame's).
		//
		// ADD ORDER IS LOAD-BEARING HERE (the mattree-open-fix root cause): EMI 1.1.24
		// dispatches widget clicks FORWARD in add order — the first containing non-SlotWidget
		// whose mouseClicked returns true consumes the press (RecipeScreen.mouseClicked, javap
		// emi-forge-1.1.24+1.20.1: the group widget loop, the SlotWidget branch only records
		// pressedSlot) — so every native click face MUST precede the nav canvas below.
		// Tail-appended after it (the nav-s4 form, written while the canvas still had no press
		// face), the cell went dead when task mattree-emi-panzoom gave the canvas its
		// empty-canvas pan gesture: the gesture swallowed the corner press and the standalone
		// fullscreen entry died — the reported 开屏 regression.
		int tScreenX = tCanvasW - 16;
		aWidgets.addButton(tScreenX, GT6MaterialTreeScreen.SCREEN_BUTTON_Y, 12, 12, 72, 0,
				() -> true, (aMx, aMy, aBtn) -> GT6MaterialTreeScreen.open(mDisplay));
		aWidgets.addDrawable(tScreenX, GT6MaterialTreeScreen.SCREEN_BUTTON_Y, 12, 12,
				(aGuiGraphics, aMx, aMy, aDelta) -> aGuiGraphics.drawString(Minecraft.getInstance().font, "T", 3, 2, 0xFFE0E0E0, false));
		// the key canvas LAST among the click faces: it covers the page for the keyboard face
		// but paints nil, and its mouse faces gate to the tree canvas (the pointer seam above)
		// — a press reaches it only when no EARLIER-ADDED native face (the slots' replay, the
		// strip buttons, the corner cell above) claims the pointer; that forward-dispatch
		// contract is what keeps the slots' U/R faces AND the fullscreen entry alive beside
		// the pan gesture
		aWidgets.add(new GT6MaterialTreeNavWidget(tView, 0, 0, tCanvasW, tCanvasH + CONTROL_STRIP_H, tCx, tCy, tSlots));
		// hover hints — invisible tooltip widgets, they never consume clicks
		aWidgets.addTooltipText(List.of(Component.literal("Zoom in (+)")), BUTTON_X0, tButtonY, 12, 12);
		aWidgets.addTooltipText(List.of(Component.literal("Zoom out (-)")), BUTTON_X0 + BUTTON_PITCH, tButtonY, 12, 12);
		aWidgets.addTooltipText(List.of(Component.literal("Reset view (R/0)")), BUTTON_X0 + 2 * BUTTON_PITCH, tButtonY, 12, 12);
		aWidgets.addTooltipText(List.of(Component.literal("Click to set zoom")), SLIDER_X, tSliderY, SLIDER_W, SLIDER_H);
		aWidgets.addTooltipText(List.of(Component.literal("Open full tree view")),
				tScreenX, GT6MaterialTreeScreen.SCREEN_BUTTON_Y, 12, 12);
	}

	/** The merged producing-map label lists per prefix (its incoming edges, display order, dedup). */
	private java.util.Map<OreDictPrefix, List<String>> producingMaps() {
		java.util.Map<OreDictPrefix, List<String>> rMap = new java.util.LinkedHashMap<>();
		for (Edge tEdge : mDisplay.edges()) rMap.computeIfAbsent(tEdge.to(), k -> new ArrayList<>());
		java.util.Map<OreDictPrefix, Set<String>> tSeen = new java.util.LinkedHashMap<>();
		for (Edge tEdge : mDisplay.edges()) {
			Set<String> tDedup = tSeen.computeIfAbsent(tEdge.to(), k -> new LinkedHashSet<>());
			List<String> tList = rMap.get(tEdge.to());
			for (String tName : tEdge.mapNames()) if (tDedup.add(tName)) tList.add(tName);
		}
		return rMap;
	}

	/** The producing edge's resolved machine stack + its via-label for one node, or null. */
	private ItemStackMachine machineOf(Node aNode) {
		for (Edge tEdge : mDisplay.edges()) {
			if (tEdge.to() != aNode.prefix()) continue;
			if (!tEdge.machine().isEmpty()) return new ItemStackMachine(tEdge.machine(), tEdge.viaLabel());
		}
		return null;
	}

	/** The machine stack + its via-label pair (the tooltip face of the box's machine icon). */
	private record ItemStackMachine(net.minecraft.world.item.ItemStack stack, String label) {}

	/** One wire rect through the viewport as a corner pair, then filled (exclusive x2/y2). */
	private static void fillTransformed(GuiGraphics aGuiGraphics, MaterialTreeViewport aView, Rect aRect, int aInk) {
		MaterialTreeViewport.Point tA = aView.apply(aRect.x(), aRect.y());
		MaterialTreeViewport.Point tB = aView.apply(aRect.x() + aRect.w(), aRect.y() + aRect.h());
		aGuiGraphics.fill(
				(int)Math.round(Math.min(tA.x(), tB.x())), (int)Math.round(Math.min(tA.y(), tB.y())),
				(int)Math.round(Math.max(tA.x(), tB.x())), (int)Math.round(Math.max(tA.y(), tB.y())), aInk);
	}

	/** The EMI node box: a white outline (four fills) through the viewport — no fill inside (BoMScreen.java:714-717). */
	private static void drawBox(GuiGraphics aGuiGraphics, MaterialTreeViewport aView, Rect aBox) {
		int tInk = MaterialTreeLayout.WIRE_INK;
		fillTransformed(aGuiGraphics, aView, new Rect(aBox.x(), aBox.y(), aBox.w(), 1), tInk);
		fillTransformed(aGuiGraphics, aView, new Rect(aBox.x(), aBox.y() + aBox.h() - 1, aBox.w(), 1), tInk);
		fillTransformed(aGuiGraphics, aView, new Rect(aBox.x(), aBox.y(), 1, aBox.h()), tInk);
		fillTransformed(aGuiGraphics, aView, new Rect(aBox.x() + aBox.w() - 1, aBox.y(), 1, aBox.h()), tInk);
	}
}
