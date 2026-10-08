package gregtech6.jei;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
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
import gregtech6.recipes.tree.MaterialTreeLayout;
import gregtech6.recipes.tree.MaterialTreeLayout.EdgeLayout;

/**
 * The material-tree category of the JEI leg (task debt-material-tree-b, ruling
 * 2026-09-26-debt-material-tree; v2 node-graph face task mattree-v2-nodes) — the modern
 * counterpart of the GTCEu ore_processing_diagram category (GTJEIPlugin.java:65-66), built
 * on THIS port's dynamic derivation instead of their static hand-drawn widget: the whole
 * diagram rides the shared {@link MaterialTreeDisplay} model, the shared
 * {@link MaterialTreeLayout} coordinate table and the shared {@link MaterialTreeViewport}
 * transform — the SAME plans the EMI twin renders (the 双 viewer 一坐标表 clause).
 *
 * <p><b>The unfrozen face (task mattree-jei-panzoom, the mattree-jei-unfreeze-poc ruling
 * a)</b> — the page is a real pan/zoom canvas now:
 * <ul>
 * <li><b>the lookup index</b>: {@link #setRecipe} mounts every ingredient through
 *     {@code addInvisibleIngredients} in its old role — the {@code ore*} column INPUT
 *     (U on an ore item opens its tree), the downstream nodes and byproduct column OUTPUT
 *     (R walks back to the producing tree), the machine icons RENDER_ONLY. The search/@/U/R
 *     buckets keep hitting this category (JEI buckets invisible slots by role exactly like
 *     visible ones — its own IngredientInfoRecipeCategory is the shipped precedent); what
 *     invisible slots do NOT do is render, hover or take clicks, which is exactly what
 *     frees the geometry below.</li>
 * <li><b>the tree body</b>: {@link GT6MaterialTreeJeiTreeWidget} self-draws the whole
 *     diagram through the viewport at render time (wires, arrowheads, labels, icons), so
 *     pan/zoom MOVE the ink — no frozen coordinates anywhere.</li>
 * <li><b>the canvas</b>: {@link GT6MaterialTreeJeiCanvasHandler} rides
 *     {@code createRecipeExtras}' {@code addInputHandler} seam (the whole-canvas
 *     registration) — wheel zooms at the pointer, drag pans (clamped), a click on a node
 *     jumps to its recipe page; hover tooltips and the focus outline are the widget's own
 *     rebuilt affordances (the invisible slots carry none).</li>
 * <li><b>the controls</b>: {@link GT6MaterialTreeJeiNavWidget} keeps the strip + keyboard
 *     face (the nav-m3-jei shape), now driving the LIVE viewport instead of an idle one,
 *     next to the S4 corner entry.</li>
 * </ul>
 *
 * <p><b>旧钉迁移声明</b>: the nav-m3-jei-era pins 「the tree geometry stays viewport-frozen
 * (JEI freezes slot coordinates, no render-time move seam)」 and 「self-drawing the tree is
 * ruled out — native hover U/R is the leg's point」 both died here: the freeze was real but
 * the ruling was reversed once the poc proved the event chain reaches the page and the
 * invisible-slot index keeps the lookups. The replacement pins are the invisible-index
 * fidelity and the pan/zoom/jump behaviour in {@code GT6MaterialTreeJeiPanzoomTest}.
 *
 * <p>Consumed faces (JEI 15.x = 1.20.1-forge and 19.x = 1.21.1-neoforge): the
 * {@code IRecipeCategory} shape, {@code IRecipeLayoutBuilder.addInvisibleIngredients} +
 * the {@code IRecipeExtrasBuilder} registration seams (identical on both pinned
 * generations), loader-neutral common API only. The single leg fork is the usual
 * ResourceLocation constructor.
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

	/**
	 * The invisible lookup index (the unfrozen face): every ingredient of the old visible
	 * slot grid re-homes into {@code addInvisibleIngredients} under its SAME role — the
	 * index side of the poc's verdict (buckets by role, lookups unchanged), with zero
	 * display-side footprint (no render, no hover, no frozen coordinates). The ink those
	 * slots used to carry moved to {@link GT6MaterialTreeJeiTreeWidget}.
	 */
	@Override
	public void setRecipe(IRecipeLayoutBuilder aBuilder, MaterialTreeDisplay aDisplay, IFocusGroup aFocuses) {
		for (Node tNode : aDisplay.nodes()) {
			RecipeIngredientRole tRole = tNode.column() == MaterialTreeDisplay.COL_ORE
					? RecipeIngredientRole.INPUT : RecipeIngredientRole.OUTPUT;
			aBuilder.addInvisibleIngredients(tRole).addItemStack(tNode.stack().copy());
		}
		List<Edge> tEdges = aDisplay.edges();
		List<EdgeLayout> tLayouts = MaterialTreeLayout.layout(aDisplay);
		for (int i = 0; i < tEdges.size(); i++) {
			if (tLayouts.get(i).machine() == null) continue;
			aBuilder.addInvisibleIngredients(RecipeIngredientRole.RENDER_ONLY)
					.addItemStack(tEdges.get(i).machine().copy());
		}
		for (Byproduct tByproduct : aDisplay.byproducts()) {
			aBuilder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT)
					.addItemStack(tByproduct.stack().copy());
		}
	}

	/**
	 * The extras registration: the whole unfrozen page in one closure — one fresh viewport
	 * per layout (JEI keeps extras alive exactly as long as the layout is on screen), the
	 * self-drawn tree widget (draw hat), the canvas input handler (wheel/drag/click), the
	 * nav strip + keyboard face and the S4 corner entry (both wearing the listener hat).
	 * The tree widget receives the focus group so the outline replaces the dead native
	 * slot-highlight. A null display stays legal (the widget draws nothing).
	 */
	@Override
	public void createRecipeExtras(IRecipeExtrasBuilder aBuilder, MaterialTreeDisplay aDisplay, IFocusGroup aFocuses) {
		MaterialTreeViewport tView = new MaterialTreeViewport();
		GT6MaterialTreeJeiTreeWidget tTree = new GT6MaterialTreeJeiTreeWidget(aDisplay, tView, aFocuses);
		aBuilder.addWidget(tTree);
		aBuilder.addInputHandler(new GT6MaterialTreeJeiCanvasHandler(tView, tTree));
		GT6MaterialTreeJeiNavWidget tNav = new GT6MaterialTreeJeiNavWidget(tView);
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

