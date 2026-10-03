package gregtech6.emi;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.WidgetHolder;

import gregtech6.recipes.tree.MaterialTreeDisplay;
import gregtech6.recipes.tree.MaterialTreeDisplay.Byproduct;
import gregtech6.recipes.tree.MaterialTreeDisplay.Edge;
import gregtech6.recipes.tree.MaterialTreeDisplay.Node;
import gregtech6.recipes.tree.MaterialTreeDisplay.Overflow;
import gregtech6.recipes.tree.MaterialTreeLayout;
import gregtech6.recipes.tree.MaterialTreeLayout.EdgeLayout;
import gregtech6.recipes.tree.MaterialTreeLayout.Rect;
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
 */
public class GT6MaterialTreeEmiRecipe implements EmiRecipe {

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
		return MaterialTreeDisplay.HEIGHT;
	}

	/** No recipe tree this card: the transfer/ghost face stays batch 4 (the batch-1 不做 clause). */
	@Override
	public boolean supportsRecipeTree() {
		return false;
	}

	@Override
	public void addWidgets(WidgetHolder aWidgets) {
		List<Edge> tEdges = mDisplay.edges();
		List<EdgeLayout> tLayouts = MaterialTreeLayout.layout(mDisplay);
		// FIRST: the wires + arrowheads, one drawable over the whole canvas ( GuiGraphics.fill
		// per rect — no diagonal primitive on 1.20.1), so they render under the slots
		List<Rect> tWires = new ArrayList<>(), tArrows = new ArrayList<>();
		for (EdgeLayout tLayout : tLayouts) {
			tWires.addAll(tLayout.wire());
			tArrows.addAll(tLayout.arrow());
		}
		aWidgets.addDrawable(0, 0, MaterialTreeDisplay.WIDTH, MaterialTreeDisplay.HEIGHT, (aGuiGraphics, aMouseX, aMouseY, aDelta) -> {
			for (Rect tRect : tWires) aGuiGraphics.fill(tRect.x(), tRect.y(), tRect.x() + tRect.w(), tRect.y() + tRect.h(), MaterialTreeLayout.WIRE_INK);
			for (Rect tRect : tArrows) aGuiGraphics.fill(tRect.x(), tRect.y(), tRect.x() + tRect.w(), tRect.y() + tRect.h(), MaterialTreeLayout.ARROW_INK);
		});
		for (Node tNode : mDisplay.nodes()) {
			aWidgets.add(new SlotWidget(EmiStack.of(tNode.stack()), MaterialTreeDisplay.nodeX(tNode), MaterialTreeDisplay.nodeY(tNode)));
		}
		// the v2 machine-icon nodes: a background-free slot (drawBack false — the bare 16x16 icon
		// face) per machine-resolved edge, hover box one px around the shared helper's icon rect,
		// carrying the via-label tooltip
		for (int i = 0; i < tEdges.size(); i++) {
			EdgeLayout tLayout = tLayouts.get(i);
			if (tLayout.machine() == null) continue;
			aWidgets.add(new SlotWidget(EmiStack.of(tEdges.get(i).machine()),
					tLayout.machine().x() - 1, tLayout.machine().y() - 1)
					.drawBack(false)
					.appendTooltip(Component.literal(tEdges.get(i).viaLabel())));
		}
		int i = 0;
		for (Byproduct tByproduct : mDisplay.byproducts()) {
			aWidgets.add(new SlotWidget(EmiStack.of(tByproduct.stack()),
					MaterialTreeDisplay.byproductX(i), MaterialTreeDisplay.byproductY()))
					.appendTooltip(Component.literal(tByproduct.sourceLabel()));
			i++;
		}
		aWidgets.addText(Component.literal(MaterialTreeDisplay.materialName(mDisplay.material)), 4, 4, 0xFF000000, false);
		aWidgets.addText(Component.literal(MaterialTreeDisplay.BYPRODUCT_HEADER),
				MaterialTreeDisplay.LANE_X0, MaterialTreeDisplay.BYPRODUCT_HEADER_Y, 0xFF000000, false);
		for (int e = 0; e < tEdges.size(); e++) {
			EdgeLayout tLayout = tLayouts.get(e);
			if (tLayout.machine() != null) continue; // the via-label lives on the machine slot's tooltip
			aWidgets.addText(Component.literal(tEdges.get(e).viaLabel()), tLayout.labelX(), tLayout.labelY(), 0xFF555555, false);
		}
		for (Overflow tOverflow : mDisplay.overflow()) {
			aWidgets.addText(Component.literal("+" + tOverflow.hidden()),
					MaterialTreeDisplay.overflowX(), MaterialTreeDisplay.overflowY(tOverflow.column()), 0xFF000000, false);
		}
	}
}
