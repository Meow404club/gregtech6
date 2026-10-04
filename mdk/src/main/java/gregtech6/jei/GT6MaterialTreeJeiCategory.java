package gregtech6.jei;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotRichTooltipCallback;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

import gregtech6.emi.GT6MaterialTreeEmiRecipe;
import gregtech6.gui.GT6MaterialTreeScreen;
import gregtech6.recipes.tree.MaterialTreeDisplay;
import gregtech6.recipes.tree.MaterialTreeViewport;
import gregtech6.recipes.tree.MaterialTreeDisplay.Byproduct;
import gregtech6.recipes.tree.MaterialTreeDisplay.Edge;
import gregtech6.recipes.tree.MaterialTreeDisplay.Node;
import gregtech6.recipes.tree.MaterialTreeDisplay.Overflow;
import gregtech6.recipes.tree.MaterialTreeLayout;
import gregtech6.recipes.tree.MaterialTreeLayout.EdgeLayout;
import gregtech6.recipes.tree.MaterialTreeLayout.Rect;

/**
 * The material-tree category of the JEI leg (task debt-material-tree-b, ruling
 * 2026-09-26-debt-material-tree; v2 node-graph face task mattree-v2-nodes) — the modern
 * counterpart of the GTCEu ore_processing_diagram category (GTJEIPlugin.java:65-66), built
 * on THIS port's dynamic derivation instead of their static hand-drawn widget: every slot
 * position comes from the shared {@link MaterialTreeDisplay} table-driven layout, every
 * wire/arrow/machine-box rect from the shared {@link MaterialTreeLayout} — the SAME plans
 * the EMI twin renders (the 双 viewer 一坐标表 clause).
 *
 * <p>v2 rendering: the chain edges are no longer floating "via" text (the v1 mush root
 * cause) — each hop draws its Manhattan wire + solid arrowhead in the category background
 * layer (under the slots) and mounts the representative machine stack as a RENDER_ONLY slot
 * on the edge's midpoint box (a slot that shows and tooltips but never joins lookups), with
 * the merged "via A/B" label as its rich tooltip. Degraded edges (EMPTY machine stack,
 * offline-only) keep the v1 text at the shared label spot. Column overflow renders the
 * explicit "+N" markers under their band ({@link MaterialTreeDisplay#overflowY}) — never silently dropped.
 *
 * <p>Slot semantics (the U/R native reachability clause): the {@code ore*} column nodes
 * ride INPUT slots (U on an ore item opens its tree — the 按材质聚合 mounting), every
 * downstream node and the byproduct side column ride OUTPUT slots (R on crushed/dust walks
 * back to the producing tree). Each slot is a native JEI slot, so focus/search/tooltip
 * behaviour is the viewer's own.
 *
 * <p>Consumed faces (JEI 15.x = 1.20.1-forge and 19.x = 1.21.1-neoforge, identical on this
 * surface — the batch-1 dual-node precedent): {@code IRecipeCategory<T>}, the
 * {@code IRecipeLayoutBuilder} slot builders with rich-tooltip callbacks (RENDER_ONLY role
 * exists on both generations), and the {@code draw(...)} text/fill leg — loader-neutral
 * common API only. The single leg fork is the usual ResourceLocation constructor.
 *
 * <p>Nav suite (task nav-m3-jei): the page mounts the lightweight control face of
 * {@link GT6MaterialTreeJeiNavWidget} (zoom in/out/reset strip + canvas keyboard, the EMI
 * twin's semantics riding the same M2 action table) via {@link #createRecipeExtras}, and
 * grows {@link #getHeight()} by the twin's control-strip band. The tree geometry itself
 * stays viewport-frozen — JEI freezes slot coordinates at layout build with no render-time
 * move seam, and self-drawing the tree is ruled out (native hover U/R is the leg's point);
 * the full assessment and the pre-authorized 按钮+键盘 degradation live on the widget's
 * javadoc.
 */
public class GT6MaterialTreeJeiCategory implements IRecipeCategory<MaterialTreeDisplay> {

	public final RecipeType<MaterialTreeDisplay> mRecipeType;

	public GT6MaterialTreeJeiCategory() {
		//? if forge {
		mRecipeType = new RecipeType<>(new ResourceLocation("gt6", MaterialTreeDisplay.CATEGORY_UID_PATH), MaterialTreeDisplay.class);
		//?} else {
		/*mRecipeType = new RecipeType<>(ResourceLocation.fromNamespaceAndPath("gt6", MaterialTreeDisplay.CATEGORY_UID_PATH), MaterialTreeDisplay.class);
		 *///?}
	}

	@Override
	public RecipeType<MaterialTreeDisplay> getRecipeType() {
		return mRecipeType;
	}

	@Override
	public Component getTitle() {
		return Component.literal(MaterialTreeDisplay.CATEGORY_TITLE);
	}

	@Override
	public int getWidth() {
		return MaterialTreeDisplay.WIDTH;
	}

	@Override
	public int getHeight() {
		return MaterialTreeDisplay.HEIGHT + GT6MaterialTreeEmiRecipe.CONTROL_STRIP_H;
	}

	/** No icon this card (the batch-1 precedent: JEI falls back; a machine-item icon table is a later nicety). */
	@Override
	public mezz.jei.api.gui.drawable.IDrawable getIcon() {
		return null;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder aBuilder, MaterialTreeDisplay aDisplay, IFocusGroup aFocuses) {
		for (Node tNode : aDisplay.nodes()) {
			if (tNode.column() == MaterialTreeDisplay.COL_ORE) {
				aBuilder.addInputSlot(MaterialTreeDisplay.nodeX(tNode), MaterialTreeDisplay.nodeY(tNode))
						.addItemStack(tNode.stack().copy());
			} else {
				aBuilder.addOutputSlot(MaterialTreeDisplay.nodeX(tNode), MaterialTreeDisplay.nodeY(tNode))
						.addItemStack(tNode.stack().copy());
			}
		}
		// the v2 machine-icon nodes: RENDER_ONLY with NO background (setStandardSlotBackground is
		// opt-in on both generations — the bare 16x16 machine icon face), one per machine-resolved
		// edge, hover box one px around the shared helper's icon rect
		List<Edge> tEdges = aDisplay.edges();
		List<EdgeLayout> tLayouts = MaterialTreeLayout.layout(aDisplay);
		for (int i = 0; i < tEdges.size(); i++) {
			EdgeLayout tLayout = tLayouts.get(i);
			if (tLayout.machine() == null) continue;
			IRecipeSlotBuilder tSlot = aBuilder.addSlot(RecipeIngredientRole.RENDER_ONLY,
					tLayout.machine().x() - 1, tLayout.machine().y() - 1)
					.addItemStack(tEdges.get(i).machine().copy());
			tSlot.addRichTooltipCallback(staticTooltip(tEdges.get(i).viaLabel()));
		}
		int i = 0;
		for (Byproduct tByproduct : aDisplay.byproducts()) {
			aBuilder.addOutputSlot(MaterialTreeDisplay.byproductX(i), MaterialTreeDisplay.byproductY())
					.addItemStack(tByproduct.stack().copy())
					.addRichTooltipCallback(staticTooltip(tByproduct.sourceLabel()));
			i++;
		}
	}

	/**
	 * The background layer: the material header, the byproduct column header, the Manhattan
	 * wires + solid arrowheads (plain {@code fill} — no diagonal primitive on 1.20.1), the
	 * degraded edges' via-labels and the "+N" overflow markers. Plain literal ink like the
	 * batch-1 cost band (NEI's fixed 0xFF000000).
	 */
	@Override
	public void draw(MaterialTreeDisplay aDisplay, IRecipeSlotsView aRecipeSlotsView,
			GuiGraphics aGuiGraphics, double aMouseX, double aMouseY) {
		var tFont = Minecraft.getInstance().font;
		aGuiGraphics.drawString(tFont, MaterialTreeDisplay.materialName(aDisplay.material), 4, 4, 0xFF000000);
		aGuiGraphics.drawString(tFont, MaterialTreeDisplay.BYPRODUCT_HEADER,
				MaterialTreeDisplay.LANE_X0, MaterialTreeDisplay.BYPRODUCT_HEADER_Y, 0xFF000000);
		List<Edge> tEdges = aDisplay.edges();
		List<EdgeLayout> tLayouts = MaterialTreeLayout.layout(aDisplay);
		for (EdgeLayout tLayout : tLayouts) {
			for (Rect tRect : tLayout.wire()) aGuiGraphics.fill(tRect.x(), tRect.y(), tRect.x() + tRect.w(), tRect.y() + tRect.h(), MaterialTreeLayout.WIRE_INK);
			for (Rect tRect : tLayout.arrow()) aGuiGraphics.fill(tRect.x(), tRect.y(), tRect.x() + tRect.w(), tRect.y() + tRect.h(), MaterialTreeLayout.ARROW_INK);
		}
		for (int i = 0; i < tEdges.size(); i++) {
			EdgeLayout tLayout = tLayouts.get(i);
			if (tLayout.machine() != null) continue; // the via-label lives on the machine slot's tooltip
			aGuiGraphics.drawString(tFont, tEdges.get(i).viaLabel(), tLayout.labelX(), tLayout.labelY(), 0xFF555555);
		}
		for (Overflow tOverflow : aDisplay.overflow()) {
			aGuiGraphics.drawString(tFont, "+" + tOverflow.hidden(),
					MaterialTreeDisplay.overflowX(), MaterialTreeDisplay.overflowY(tOverflow.column()), 0xFF000000);
		}
	}

	private static IRecipeSlotRichTooltipCallback staticTooltip(String aLine) {
		return (aView, aTooltip) -> aTooltip.add(Component.literal(aLine));
	}

	/**
	 * The extras registration: the M3 nav face (task nav-m3-jei) + the S4 corner entry
	 * (task nav-s4-tree-screen), one override mounting both — one fresh viewport per
	 * layout (JEI keeps extras alive exactly as long as the layout is on screen — the EMI
	 * twin's per-page-open closure), each face BOTH the draw widget and the input listener
	 * (the one-instance-two-hats shape), plus the four hover hints (the three EMI-parity
	 * strip cells + the corner cell). The recipe is not touched here (the viewport is
	 * display-agnostic), so a null display is as good as any.
	 */
	@Override
	public void createRecipeExtras(IRecipeExtrasBuilder aBuilder, MaterialTreeDisplay aDisplay, IFocusGroup aFocuses) {
		GT6MaterialTreeJeiNavWidget tNav = new GT6MaterialTreeJeiNavWidget(new MaterialTreeViewport());
		aBuilder.addWidget(tNav);
		aBuilder.addGuiEventListener(tNav);
		GT6MaterialTreeJeiScreenButton tButton = new GT6MaterialTreeJeiScreenButton(aDisplay);
		aBuilder.addWidget(tButton);
		aBuilder.addGuiEventListener(tButton);
		aBuilder.addTooltipArea(GT6MaterialTreeEmiRecipe.BUTTON_X0, GT6MaterialTreeEmiRecipe.BUTTON_Y, 12, 12)
				.setTooltip(Component.literal("Zoom in (+)"));
		aBuilder.addTooltipArea(GT6MaterialTreeEmiRecipe.BUTTON_X0 + GT6MaterialTreeEmiRecipe.BUTTON_PITCH,
				GT6MaterialTreeEmiRecipe.BUTTON_Y, 12, 12).setTooltip(Component.literal("Zoom out (-)"));
		aBuilder.addTooltipArea(GT6MaterialTreeEmiRecipe.BUTTON_X0 + 2 * GT6MaterialTreeEmiRecipe.BUTTON_PITCH,
				GT6MaterialTreeEmiRecipe.BUTTON_Y, 12, 12).setTooltip(Component.literal("Reset view (R/0)"));
		aBuilder.addTooltipArea(GT6MaterialTreeScreen.SCREEN_BUTTON_X, GT6MaterialTreeScreen.SCREEN_BUTTON_Y, 12, 12)
				.setTooltip(Component.literal("Open full tree view"));
	}
}
