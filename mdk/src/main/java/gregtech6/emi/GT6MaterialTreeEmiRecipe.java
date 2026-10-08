package gregtech6.emi;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;

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
import gregtech6.registry.GTMaterialItems;

/**
 * One material's tree as an EMI recipe (task debt-material-tree-b; v2 node-graph face task
 * mattree-v2-nodes) — wrapping the viewer-neutral {@link MaterialTreeDisplay} and
 * rendering the SAME {@link MaterialTreeLayout} plans as the JEI twin (the 双 viewer 一坐标表
 * clause): every slot lands at the shared layout table's coordinates, every wire/arrow/
 * machine box at the shared helper's. The U/R faces mirror the JEI slot split: the
 * {@code ore*} column rides {@link #getInputs()} (U on an ore item finds its tree — the
 * 按材质聚合 mounting), the downstream nodes and the byproduct side column ride
 * {@link #getOutputs()}.
 *
 * <p>v2 rendering order is load-bearing: the wire drawable widget is added FIRST so the
 * Manhattan segments and arrowheads render UNDER the slots (a run crossing an intermediate
 * column's slot hides behind the box, matching the JEI background-layer z-order). The
 * machine-icon nodes are plain SlotWidgets carrying the merged "via A/B" tooltip; degraded
 * edges (EMPTY machine stack — offline-only) fall back to the v1-style via-label text; the
 * "+N" overflow markers render under their band ({@link MaterialTreeDisplay#overflowY}).
 *
 * <p>Id stability: {@code gt6:material_tree/<snake(material)>} — the snake_case face is the
 * registered-item id convention (GTMaterialItems.itemIdOf), so the id survives a material's
 * localisation.
 *
 * <p>Nav suite (task nav-m2-emi, consuming the S1 {@link MaterialTreeViewport}): every page
 * open creates a FRESH viewport and wires it through all the widgets — the wires/labels
 * drawable and the {@link GT6MaterialTreeTransformSlot}s transform through it at render
 * time, the control strip (zoom in/out/reset buttons, click-track slider) and the invisible
 * key canvas drive it — and closing the page drops the widgets, which is the whole
 * apply-unapply 闭环 (no static state, nothing leaks between opens). The control strip
 * rides a band BELOW the canvas ({@link #CONTROL_STRIP_H}) — the recipe height is the EMI
 * page-size source (RecipeDisplay.height = getDisplayHeight), and EMI stacks each recipe
 * at its own height, so the extra 20px cost nothing.
 *
 * <p>The pointer seam (task mattree-emi-panzoom): the nav canvas also implements the
 * vendored MUI {@code EmiInteractionSink}, so wheel zooms pointer-anchored and an
 * empty-canvas drag pans — both gated to the tree canvas rect, with slot presses yielded
 * to the native U/R/drag-stack faces (the canvas receives the page's slot list in add
 * order and replays EMI's own bounds test). The interaction shape is the standalone-screen
 * precedent's (wheel = anchored zoom, drag = follow-the-cursor pan), brought onto the
 * recipe page through the one seam that can deliver these events.
 *
 * <p><b>The icon-scale audit</b> (task mattree-item-zoom-pose) — <b>RESOLVED</b> by task
 * mattree-r3-nav-unify: the page's labels ride the unified
 * {@link MaterialTreeLayout#pose} primitive at render time and the
 * {@link GT6MaterialTreeTransformSlot} bounds pan/zoom every slot face with the viewport;
 * EMI 1.1.24's {@code SlotWidget.drawStack} used to centre a fixed 16 px icon inside the
 * transformed bounds (javap emi-forge-1.1.24: {@code (width - 16) / 2} then
 * {@code EmiIngredient.render} — a scale lag, centred so never 错位), and the slot's
 * {@code drawStack} pose override now renders the icon at the viewport scale, centred in
 * the box (the pin lives in {@code GT6MaterialTreeNavTest}); the bounds math stays pinned
 * in {@code GT6MaterialTreeNavTest#transformSlotBoundsFollowTheViewport}.
 */
public class GT6MaterialTreeEmiRecipe implements EmiRecipe {

	/** The nav control strip below the tree canvas (12px buttons + an 8px track on a 20px band). */
	public static final int CONTROL_STRIP_H = 20;
	/** The strip buttons' column (zoom in / zoom out / reset, 12px cells, 4px gaps), on the band's top row. */
	public static final int BUTTON_X0 = 4, BUTTON_PITCH = 16, BUTTON_Y = MaterialTreeDisplay.HEIGHT + 4;
	/** The click-track zoom slider: a 138x8 track, vertically centred in the strip. */
	public static final int SLIDER_X = 56, SLIDER_Y = MaterialTreeDisplay.HEIGHT + 6;
	public static final int SLIDER_W = 138, SLIDER_H = 8;

	public final MaterialTreeDisplay mDisplay;
	private final ResourceLocation mId;
	private final List<EmiIngredient> mInputs;
	private final List<EmiStack> mOutputs;

	public GT6MaterialTreeEmiRecipe(MaterialTreeDisplay aDisplay) {
		mDisplay = aDisplay;
		//? if forge {
		mId = new ResourceLocation("gt6", MaterialTreeDisplay.CATEGORY_UID_PATH + "/" + GTMaterialItems.snakeCase(aDisplay.material.mNameInternal));
		//?} else {
		/*mId = ResourceLocation.fromNamespaceAndPath("gt6", MaterialTreeDisplay.CATEGORY_UID_PATH + "/" + GTMaterialItems.snakeCase(aDisplay.material.mNameInternal));
		 *///?}
		mInputs = new ArrayList<>();
		mOutputs = new ArrayList<>();
		for (Node tNode : aDisplay.nodes()) {
			if (tNode.column() == MaterialTreeDisplay.COL_ORE) mInputs.add(EmiStack.of(tNode.stack()));
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
		return MaterialTreeDisplay.WIDTH;
	}

	@Override
	public int getDisplayHeight() {
		return MaterialTreeDisplay.HEIGHT + CONTROL_STRIP_H;
	}

	/** No recipe tree this card: the transfer/ghost face stays batch 4 (the batch-1 不做 clause). */
	@Override
	public boolean supportsRecipeTree() {
		return false;
	}

	@Override
	public void addWidgets(WidgetHolder aWidgets) {
		// the nav viewport: FRESH per page-open (the apply-unapply 闭环 — entering applies it
		// to every widget below, leaving drops the widgets, nothing static survives)
		MaterialTreeViewport tView = new MaterialTreeViewport();
		// the shared zoom anchor: the canvas centre (buttons, keys and slider all focus it)
		double tCx = MaterialTreeDisplay.WIDTH / 2.0, tCy = MaterialTreeDisplay.HEIGHT / 2.0;

		List<Edge> tEdges = mDisplay.edges();
		List<EdgeLayout> tLayouts = MaterialTreeLayout.layout(mDisplay);
		// the nav canvas's slot yield set: every TransformSlot in add order, so a canvas press
		// can replay EMI's own first-hit bounds test and yield to the native slot faces
		List<GT6MaterialTreeTransformSlot> tSlots = new ArrayList<>();
		// FIRST: the wires + arrowheads + labels, one drawable over the whole page ( GuiGraphics.fill
		// per rect — no diagonal primitive on 1.20.1), so they render under the slots. The
		// labels moved in here with the nav suite: their positions must follow the viewport
		// at RENDER time, and a TextWidget freezes its coordinate at add time.
		List<Rect> tWires = new ArrayList<>(), tArrows = new ArrayList<>();
		for (EdgeLayout tLayout : tLayouts) {
			tWires.addAll(tLayout.wire());
			tArrows.addAll(tLayout.arrow());
		}
		record Label(Component text, int x, int y, int color) {}
		List<Label> tLabels = new ArrayList<>();
		tLabels.add(new Label(Component.literal(MaterialTreeDisplay.materialName(mDisplay.material)), 4, 4, 0xFF000000));
		tLabels.add(new Label(Component.literal(MaterialTreeDisplay.BYPRODUCT_HEADER),
				MaterialTreeDisplay.LANE_X0, MaterialTreeDisplay.BYPRODUCT_HEADER_Y, 0xFF000000));
		for (int e = 0; e < tEdges.size(); e++) {
			EdgeLayout tLayout = tLayouts.get(e);
			if (tLayout.machine() != null) continue; // the via-label lives on the machine slot's tooltip
			tLabels.add(new Label(Component.literal(tEdges.get(e).viaLabel()), tLayout.labelX(), tLayout.labelY(), 0xFF555555));
		}
		for (Overflow tOverflow : mDisplay.overflow()) {
			tLabels.add(new Label(Component.literal("+" + tOverflow.hidden()),
					MaterialTreeDisplay.overflowX(), MaterialTreeDisplay.overflowY(tOverflow.column()), 0xFF000000));
		}
		aWidgets.addDrawable(0, 0, MaterialTreeDisplay.WIDTH, MaterialTreeDisplay.HEIGHT + CONTROL_STRIP_H, (aGuiGraphics, aMouseX, aMouseY, aDelta) -> {
			// corner-pair transforms: shared rect edges stay seamless at any scale
			for (Rect tRect : tWires) fillTransformed(aGuiGraphics, tView, tRect, MaterialTreeLayout.WIRE_INK);
			for (Rect tRect : tArrows) fillTransformed(aGuiGraphics, tView, tRect, MaterialTreeLayout.ARROW_INK);
			for (Label tLabel : tLabels) {
				GT6MaterialTreeScreen.runAtPose(aGuiGraphics, MaterialTreeLayout.pose(tView, tLabel.x(), tLabel.y()),
						() -> aGuiGraphics.drawString(Minecraft.getInstance().font, tLabel.text(), 0, 0, tLabel.color(), false));
			}
		});
		for (Node tNode : mDisplay.nodes()) {
			tSlots.add(aWidgets.add(new GT6MaterialTreeTransformSlot(EmiStack.of(tNode.stack()),
					MaterialTreeDisplay.nodeX(tNode), MaterialTreeDisplay.nodeY(tNode), tView)));
		}
		// the v2 machine-icon nodes: a background-free slot (drawBack false — the bare 16x16 icon
		// face) per machine-resolved edge, hover box one px around the shared helper's icon rect,
		// carrying the via-label tooltip
		for (int i = 0; i < tEdges.size(); i++) {
			EdgeLayout tLayout = tLayouts.get(i);
			if (tLayout.machine() == null) continue;
			// drawBack/appendTooltip return the SlotWidget supertype — mutate on the typed
			// local, the chain result is the same instance
			GT6MaterialTreeTransformSlot tMachine = new GT6MaterialTreeTransformSlot(EmiStack.of(tEdges.get(i).machine()),
					tLayout.machine().x() - 1, tLayout.machine().y() - 1, tView);
			tSlots.add(tMachine);
			aWidgets.add(tMachine.drawBack(false)
					.appendTooltip(Component.literal(tEdges.get(i).viaLabel())));
		}
		int i = 0;
		for (Byproduct tByproduct : mDisplay.byproducts()) {
			GT6MaterialTreeTransformSlot tByproductSlot = new GT6MaterialTreeTransformSlot(EmiStack.of(tByproduct.stack()),
					MaterialTreeDisplay.byproductX(i), MaterialTreeDisplay.byproductY(), tView);
			tSlots.add(tByproductSlot);
			aWidgets.add(tByproductSlot)
					.appendTooltip(Component.literal(tByproduct.sourceLabel()));
			i++;
		}
		// the control strip (the EMI 1.1.24 native button seam — WidgetHolder.addButton onto the
		// blank cell u=72 v=0 of emi buttons.png, whose hover/inactive rows ButtonWidget picks by
		// itself): the zoom buttons sleep at the S1 floor/ceiling, reset never does
		aWidgets.addButton(BUTTON_X0, BUTTON_Y, 12, 12, 72, 0,
				() -> tView.scale() < MaterialTreeViewport.MAX_SCALE,
				(aMx, aMy, aBtn) -> GT6MaterialTreeNav.handle(tView, GT6MaterialTreeNav.Action.ZOOM_IN, tCx, tCy));
		aWidgets.addButton(BUTTON_X0 + BUTTON_PITCH, BUTTON_Y, 12, 12, 72, 0,
				() -> tView.scale() > MaterialTreeViewport.MIN_SCALE,
				(aMx, aMy, aBtn) -> GT6MaterialTreeNav.handle(tView, GT6MaterialTreeNav.Action.ZOOM_OUT, tCx, tCy));
		aWidgets.addButton(BUTTON_X0 + 2 * BUTTON_PITCH, BUTTON_Y, 12, 12, 72, 0,
				() -> true,
				(aMx, aMy, aBtn) -> GT6MaterialTreeNav.handle(tView, GT6MaterialTreeNav.Action.RESET, tCx, tCy));
		aWidgets.add(new GT6MaterialTreeSliderWidget(tView, SLIDER_X, SLIDER_Y, SLIDER_W, SLIDER_H, tCx, tCy));
		// the key canvas LAST: it covers the page for the keyboard face but paints nil, and its
		// mouse faces gate to the tree canvas (the pointer seam above) — clicks on slots pass
		// through, so the slots' U/R faces are untouched
		aWidgets.add(new GT6MaterialTreeNavWidget(tView, 0, 0,
				MaterialTreeDisplay.WIDTH, MaterialTreeDisplay.HEIGHT + CONTROL_STRIP_H, tCx, tCy, tSlots));
		// hover hints — invisible tooltip widgets, they never consume clicks
		aWidgets.addTooltipText(List.of(Component.literal("Zoom in (+)")), BUTTON_X0, BUTTON_Y, 12, 12);
		aWidgets.addTooltipText(List.of(Component.literal("Zoom out (-)")), BUTTON_X0 + BUTTON_PITCH, BUTTON_Y, 12, 12);
		aWidgets.addTooltipText(List.of(Component.literal("Reset view (R/0)")), BUTTON_X0 + 2 * BUTTON_PITCH, BUTTON_Y, 12, 12);
		aWidgets.addTooltipText(List.of(Component.literal("Click to set zoom")), SLIDER_X, SLIDER_Y, SLIDER_W, SLIDER_H);
		// the S4 escape hatch (task nav-s4-tree-screen) — tail-appended after the M2 nav face:
		// the canvas's top-right corner cell opens the standalone full-screen tree
		// (GT6MaterialTreeScreen), the r11 大树走独立屏 ruling's face. The blank native cell
		// (u=72 v=0, the strip convention) carries a "T" glyph drawable above it; the click
		// replaces the EMI page with the screen (setScreen has no barrier from a widget click).
		aWidgets.addButton(GT6MaterialTreeScreen.SCREEN_BUTTON_X, GT6MaterialTreeScreen.SCREEN_BUTTON_Y, 12, 12, 72, 0,
				() -> true, (aMx, aMy, aBtn) -> GT6MaterialTreeScreen.open(mDisplay));
		aWidgets.addDrawable(GT6MaterialTreeScreen.SCREEN_BUTTON_X, GT6MaterialTreeScreen.SCREEN_BUTTON_Y, 12, 12,
				(aGuiGraphics, aMx, aMy, aDelta) -> aGuiGraphics.drawString(Minecraft.getInstance().font, "T", 3, 2, 0xFFE0E0E0, false));
		aWidgets.addTooltipText(List.of(Component.literal("Open full tree view")),
				GT6MaterialTreeScreen.SCREEN_BUTTON_X, GT6MaterialTreeScreen.SCREEN_BUTTON_Y, 12, 12);
	}

	/** One wire/arrow rect through the viewport as a corner pair, then filled (exclusive x2/y2). */
	private static void fillTransformed(GuiGraphics aGuiGraphics, MaterialTreeViewport aView, Rect aRect, int aInk) {
		MaterialTreeViewport.Point tA = aView.apply(aRect.x(), aRect.y());
		MaterialTreeViewport.Point tB = aView.apply(aRect.x() + aRect.w(), aRect.y() + aRect.h());
		aGuiGraphics.fill(
				(int)Math.round(Math.min(tA.x(), tB.x())), (int)Math.round(Math.min(tA.y(), tB.y())),
				(int)Math.round(Math.max(tA.x(), tB.x())), (int)Math.round(Math.max(tA.y(), tB.y())), aInk);
	}
}
