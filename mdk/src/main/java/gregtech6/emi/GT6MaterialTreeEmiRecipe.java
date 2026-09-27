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
import gregtech6.registry.GTMaterialItems;

/**
 * One material's tree as an EMI recipe (task debt-material-tree-b) — the hand-flattened
 * form of the batch-1 precedent (GT6RecipeMapEmiRecipe), wrapping the viewer-neutral
 * {@link MaterialTreeDisplay} instead of a Recipe row: the slots and the machine labels
 * land at exactly the coordinates the shared layout table computes, the same geometry the
 * JEI twin renders. The U/R faces mirror the JEI slot split: the {@code ore*} column rides
 * {@link #getInputs()} (U on an ore item finds its tree — the 按材质聚合 mounting), the
 * downstream nodes and the byproduct side column ride {@link #getOutputs()}.
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
		for (Node tNode : mDisplay.nodes()) {
			aWidgets.add(new SlotWidget(EmiStack.of(tNode.stack()), MaterialTreeDisplay.nodeX(tNode), MaterialTreeDisplay.nodeY(tNode)));
		}
		int i = 0;
		for (Byproduct tByproduct : mDisplay.byproducts()) {
			aWidgets.add(new SlotWidget(EmiStack.of(tByproduct.stack()),
					MaterialTreeDisplay.columnX(MaterialTreeDisplay.COL_BYPRODUCT), MaterialTreeDisplay.byproductY(i)))
					.appendTooltip(Component.literal(tByproduct.sourceLabel()));
			i++;
		}
		aWidgets.addText(Component.literal(MaterialTreeDisplay.materialName(mDisplay.material)), 4, 4, 0xFF000000, false);
		aWidgets.addText(Component.literal(MaterialTreeDisplay.BYPRODUCT_HEADER),
				MaterialTreeDisplay.columnX(MaterialTreeDisplay.COL_BYPRODUCT), 4, 0xFF000000, false);
		for (Edge tEdge : mDisplay.edges()) {
			Node tFrom = nodeOf(tEdge.from()), tTo = nodeOf(tEdge.to());
			if (tFrom == null || tTo == null) continue;
			StringBuilder tLabel = new StringBuilder(MaterialTreeDisplay.VIA_PREFIX);
			for (int m = 0; m < tEdge.mapNames().size(); m++) {
				if (m > 0) tLabel.append('/');
				tLabel.append(MaterialTreeDisplay.mapLabel(tEdge.mapNames().get(m)));
			}
			aWidgets.addText(Component.literal(tLabel.toString()),
					MaterialTreeDisplay.nodeX(tFrom) + 16,
					(MaterialTreeDisplay.nodeY(tFrom) + MaterialTreeDisplay.nodeY(tTo)) / 2 - 4, 0xFF555555, false);
		}
	}

	private Node nodeOf(gregapi.oredict.OreDictPrefix aPrefix) {
		for (Node tNode : mDisplay.nodes()) if (tNode.prefix() == aPrefix) return tNode;
		return null;
	}
}
