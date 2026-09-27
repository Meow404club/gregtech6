package gregtech6.jei;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotRichTooltipCallback;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

import gregtech6.recipes.tree.MaterialTreeDisplay;
import gregtech6.recipes.tree.MaterialTreeDisplay.Byproduct;
import gregtech6.recipes.tree.MaterialTreeDisplay.Edge;
import gregtech6.recipes.tree.MaterialTreeDisplay.Node;

/**
 * The material-tree category of the JEI leg (task debt-material-tree-b, ruling
 * 2026-09-26-debt-material-tree: the display card of the A/B/C split) — the modern
 * counterpart of the GTCEu ore_processing_diagram category (GTJEIPlugin.java:65-66), built
 * on THIS port's dynamic derivation instead of their static hand-drawn widget: every slot
 * position comes from the shared {@link MaterialTreeDisplay} table-driven layout, the same
 * functions the EMI twin renders from.
 *
 * <p>Slot semantics (the U/R native reachability clause): the {@code ore*} column nodes
 * ride INPUT slots (U on an ore item opens its tree — the 按材质聚合 mounting), every
 * downstream node and the byproduct side column ride OUTPUT slots (R on crushed/dust walks
 * back to the producing tree). Each slot is a native JEI slot, so focus/search/tooltip
 * behaviour is the viewer's own.
 *
 * <p>Consumed faces (JEI 15.x = 1.20.1-forge and 19.x = 1.21.1-neoforge, identical on this
 * surface — the batch-1 dual-node precedent): {@code IRecipeCategory<T>}, the
 * {@code IRecipeLayoutBuilder} slot builders with rich-tooltip callbacks, and the
 * {@code draw(...)} text leg — loader-neutral common API only. The single leg fork is the
 * usual ResourceLocation constructor (see the batch-1 category).
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
		return MaterialTreeDisplay.HEIGHT;
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
		int i = 0;
		for (Byproduct tByproduct : aDisplay.byproducts()) {
			aBuilder.addOutputSlot(MaterialTreeDisplay.columnX(MaterialTreeDisplay.COL_BYPRODUCT), MaterialTreeDisplay.byproductY(i))
					.addItemStack(tByproduct.stack().copy())
					.addRichTooltipCallback(staticTooltip(tByproduct.sourceLabel()));
			i++;
		}
	}

	/**
	 * The text leg: the material header, the byproduct column header and the machine labels
	 *贴边 ("via Shredder") at each edge's midpoint — the 机器名标签 clause. Plain literal
	 * ink like the batch-1 cost band (NEI's fixed 0xFF000000).
	 */
	@Override
	public void draw(MaterialTreeDisplay aDisplay, IRecipeSlotsView aRecipeSlotsView,
			GuiGraphics aGuiGraphics, double aMouseX, double aMouseY) {
		var tFont = Minecraft.getInstance().font;
		aGuiGraphics.drawString(tFont, MaterialTreeDisplay.materialName(aDisplay.material), 4, 4, 0xFF000000);
		aGuiGraphics.drawString(tFont, MaterialTreeDisplay.BYPRODUCT_HEADER,
				MaterialTreeDisplay.columnX(MaterialTreeDisplay.COL_BYPRODUCT), 4, 0xFF000000);
		for (Edge tEdge : aDisplay.edges()) {
			Node tFrom = nodeOf(aDisplay, tEdge.from()), tTo = nodeOf(aDisplay, tEdge.to());
			if (tFrom == null || tTo == null) continue;
			StringBuilder tLabel = new StringBuilder(MaterialTreeDisplay.VIA_PREFIX);
			for (int m = 0; m < tEdge.mapNames().size(); m++) {
				if (m > 0) tLabel.append('/');
				tLabel.append(MaterialTreeDisplay.mapLabel(tEdge.mapNames().get(m)));
			}
			int tX = MaterialTreeDisplay.nodeX(tFrom) + 16;
			int tY = (MaterialTreeDisplay.nodeY(tFrom) + MaterialTreeDisplay.nodeY(tTo)) / 2 - 4;
			aGuiGraphics.drawString(tFont, tLabel.toString(), tX, tY, 0xFF555555);
		}
	}

	private static Node nodeOf(MaterialTreeDisplay aDisplay, gregapi.oredict.OreDictPrefix aPrefix) {
		for (Node tNode : aDisplay.nodes()) if (tNode.prefix() == aPrefix) return tNode;
		return null;
	}

	private static IRecipeSlotRichTooltipCallback staticTooltip(String aLine) {
		return (aView, aTooltip) -> aTooltip.add(Component.literal(aLine));
	}
}
