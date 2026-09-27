package gregtech6.jei;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotRichTooltipCallback;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;

/**
 * The generic RM recipe category of the JEI leg (task debt-jei-emi-batch1) — ONE class
 * serving every visible map, the modern counterpart of the upstream single 718-line
 * NEI_RecipeMap handler that served every mNEIAllowed map (NEI_GT_API_Config.java:62
 * registered one handler per map over this same single class). The layout, the cost text
 * and the tooltips all delegate to the shared {@link GT6RecipeMapViewerMeta} seam — the
 * EMI twin renders the same geometry from the same functions, the card's
 * "布局/EU 文案/tooltip 双 viewer 共享" clause.
 *
 * <p>Consumed faces (JEI 15.x = 1.20.1-forge and 19.x = 1.21.1-neoforge, both read from
 * the harvested API sources, identical on this surface — the r3-jei-tool-output-tint
 * dual-node precedent): {@code IRecipeCategory<T>} (getRecipeType/getTitle/getWidth/
 * getHeight/setRecipe/draw), {@code IRecipeLayoutBuilder.addInputSlot(x,y)/addOutputSlot
 * (x,y)}, {@code IRecipeSlotBuilder.addItemStack/addFluidStack(Fluid,long)/
 * addRichTooltipCallback} — the loader-neutral common API only, pinned by the
 * GT6JeiPluginTest bytecode guard's jurisdiction.
 *
 * <p>Slot semantics over the port's {@link Recipe} row (the NEI display fields):
 * item inputs up to the map's mInputItemsCount (NEI probed getRepresentativeInput per
 * slot — the port's mInputs array IS the row's slot list), item outputs with the chance
 * tooltip, fluid inputs/outputs as native fluids (the bucket containerization is the
 * dropped column, GT6RecipeMapViewerMeta class doc), and the never-consumed tooltip
 * riding {@link Recipe#sNotConsumable} (the size-0 marker port).
 */
public class GT6RecipeMapJeiCategory implements IRecipeCategory<Recipe> {

	/** {@code gt6:recipe_map/<internal>} — the JEI-side uid mirrors the EMI category id one-to-one. */
	public final RecipeType<Recipe> mRecipeType;
	public final RecipeMap mMap;

	public GT6RecipeMapJeiCategory(RecipeMap aMap) {
		mMap = aMap;
		//? if forge {
		mRecipeType = new RecipeType<>(new ResourceLocation("gt6", "recipe_map/" + aMap.mNameInternal), Recipe.class);
		//?} else {
		/*mRecipeType = new RecipeType<>(ResourceLocation.fromNamespaceAndPath("gt6", "recipe_map/" + aMap.mNameInternal), Recipe.class);
		 *///?}
	}

	@Override
	public RecipeType<Recipe> getRecipeType() {
		return mRecipeType;
	}

	@Override
	public Component getTitle() {
		return Component.literal(mMap.mNameLocal);
	}

	@Override
	public int getWidth() {
		return GT6RecipeMapViewerMeta.CATEGORY_WIDTH;
	}

	@Override
	public int getHeight() {
		return GT6RecipeMapViewerMeta.CATEGORY_HEIGHT;
	}

	/**
	 * No icon this card: the upstream per-map machine-item face (NEI's
	 * mRecipeMachineList.get(0)) has no port table yet, and JEI explicitly allows null
	 * ("JEI will try to use the first recipe catalyst"). A per-map machine-icon table is
	 * a batch-2 nicety.
	 */
	@Override
	public mezz.jei.api.gui.drawable.IDrawable getIcon() {
		return null;
	}

	@Override
	public void setRecipe(IRecipeLayoutBuilder aBuilder, Recipe aRecipe, IFocusGroup aFocuses) {
		int tInputs = Math.min(aRecipe.mInputs.length, mMap.mInputItemsCount);
		for (int i = 0; i < tInputs; i++) {
			ItemStack tStack = aRecipe.mInputs[i];
			if (tStack == null || tStack.isEmpty()) continue;
			int[] tPos = GT6RecipeMapViewerMeta.inputPos(i, mMap);
			if (tPos == null) continue; // past the 12th drawn slot (the meta contract)
			var tSlot = aBuilder.addInputSlot(tPos[0], tPos[1]).addItemStack(tStack.copy());
			if (GT6RecipeMapViewerMeta.notConsumable(tStack)) tSlot.addRichTooltipCallback(notConsumedTooltip());
		}
		int tOutputs = Math.min(aRecipe.mOutputs.length, mMap.mOutputItemsCount);
		for (int i = 0; i < tOutputs; i++) {
			ItemStack tStack = aRecipe.mOutputs[i];
			if (tStack == null || tStack.isEmpty()) continue;
			int[] tPos = GT6RecipeMapViewerMeta.outputPos(i, mMap);
			if (tPos == null) continue; // past the 12th drawn slot (the meta contract)
			String tChance = GT6RecipeMapViewerMeta.chanceLine(GT6RecipeMapViewerMeta.outputChance(aRecipe, i), tStack.getCount());
			var tSlot = aBuilder.addOutputSlot(tPos[0], tPos[1]).addItemStack(tStack.copy());
			if (tChance != null) tSlot.addRichTooltipCallback(staticTooltip(tChance));
		}
		int tFluids = Math.min(aRecipe.mFluidInputs.length, mMap.mInputFluidCount);
		for (int i = 0; i < tFluids; i++) {
			if (aRecipe.mFluidInputs[i] == null || aRecipe.mFluidInputs[i].isEmpty()) continue;
			int[] tPos = GT6RecipeMapViewerMeta.fluidInputPos(i);
			aBuilder.addInputSlot(tPos[0], tPos[1])
					.addFluidStack(aRecipe.mFluidInputs[i].getFluid(), aRecipe.mFluidInputs[i].getAmount());
		}
		int tFluidOuts = Math.min(aRecipe.mFluidOutputs.length, mMap.mOutputFluidCount);
		for (int i = 0; i < tFluidOuts; i++) {
			if (aRecipe.mFluidOutputs[i] == null || aRecipe.mFluidOutputs[i].isEmpty()) continue;
			int[] tPos = GT6RecipeMapViewerMeta.fluidOutputPos(i);
			aBuilder.addOutputSlot(tPos[0], tPos[1])
					.addFluidStack(aRecipe.mFluidOutputs[i].getFluid(), aRecipe.mFluidOutputs[i].getAmount());
		}
	}

	/**
	 * drawExtras, the draw leg: the Costs/Usage/Tier/Power/Time/Special lines from the
	 * shared formatter (NEI_RecipeMap.drawExtras :680-717), at the shared text band —
	 * NEI's fixed 0xFF000000 ink and x10 kept.
	 */
	@Override
	public void draw(Recipe aRecipe, mezz.jei.api.gui.ingredient.IRecipeSlotsView aRecipeSlotsView,
			net.minecraft.client.gui.GuiGraphics aGuiGraphics, double aMouseX, double aMouseY) {
		int tY = GT6RecipeMapViewerMeta.textBaseY(mMap);
		for (String tLine : GT6RecipeMapViewerMeta.costLines(mMap, aRecipe)) {
			aGuiGraphics.drawString(Minecraft.getInstance().font, tLine, GT6RecipeMapViewerMeta.TEXT_X, tY, 0xFF000000);
			tY += GT6RecipeMapViewerMeta.TEXT_LINE_HEIGHT;
		}
	}

	private static IRecipeSlotRichTooltipCallback staticTooltip(String aLine) {
		return (aView, aTooltip) -> aTooltip.add(Component.literal(aLine));
	}

	private static IRecipeSlotRichTooltipCallback notConsumedTooltip() {
		return staticTooltip(GT6RecipeMapViewerMeta.NOT_CONSUMED_TEXT);
	}
}
