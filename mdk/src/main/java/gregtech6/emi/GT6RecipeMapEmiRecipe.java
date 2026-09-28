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

import gregtech6.jei.GT6RecipeMapViewerMeta;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;

/**
 * One RM row's EMI face (task debt-jei-emi-batch1) — the hand-flattened inputs/outputs
 * form of the GTCEu GTEmiRecipe precedent (:22-26 each row wraps its category and
 * flattens the GT ingredient model), here flattening the port's {@link Recipe} arrays:
 * item inputs (with the never-consumed tooltip), item outputs (with the chance tooltip),
 * fluid inputs/outputs as native {@link EmiStack}s. The slots and the cost text land at
 * exactly the coordinates the shared {@link GT6RecipeMapViewerMeta} computes — the same
 * geometry the JEI twin renders (NEI_RecipeMap layout switch translation).
 *
 * <p>Id stability: the port's rows carry no registry id, so the id is the per-map sorted
 * index ({@code gt6:recipe_map/<internal>/<index>}); the sort is a simplified port of the
 * NEI sortRecipes key (NEI_RecipeMap.java:475-514 EUt-first) — EUt, then duration, then
 * the first input's string, then the first output's. ponytail: near-duplicate rows that
 * tie on all four keys may swap ids across sessions; cosmetic (bookmark instability at
 * worst), upgrade to registry-identity keys if a row universe ever grows duplicates.
 */
public class GT6RecipeMapEmiRecipe implements EmiRecipe {

	public final RecipeMap mMap;
	public final Recipe mRow;
	public final GT6RecipeMapEmiCategory mCategory;
	private final ResourceLocation mId;
	private final List<EmiIngredient> mInputs;
	private final List<EmiStack> mOutputs;

	public GT6RecipeMapEmiRecipe(RecipeMap aMap, Recipe aRow, GT6RecipeMapEmiCategory aCategory, int aSortedIndex) {
		mMap = aMap;
		mRow = aRow;
		mCategory = aCategory;
		//? if forge {
		mId = new ResourceLocation("gt6", "recipe_map/" + aMap.mNameInternal + "/" + aSortedIndex);
		//?} else {
		/*mId = ResourceLocation.fromNamespaceAndPath("gt6", "recipe_map/" + aMap.mNameInternal + "/" + aSortedIndex);
		 *///?}
		mInputs = new ArrayList<>();
		for (int i = 0; i < Math.min(aRow.mInputs.length, aMap.mInputItemsCount); i++) {
			if (aRow.mInputs[i] != null && !aRow.mInputs[i].isEmpty()) mInputs.add(EmiStack.of(aRow.mInputs[i]));
		}
		for (int i = 0; i < Math.min(aRow.mFluidInputs.length, aMap.mInputFluidCount); i++) {
			if (aRow.mFluidInputs[i] != null && !aRow.mFluidInputs[i].isEmpty())
				mInputs.add(EmiStack.of(aRow.mFluidInputs[i].getFluid(), aRow.mFluidInputs[i].getAmount()));
		}
		mOutputs = new ArrayList<>();
		for (int i = 0; i < Math.min(aRow.mOutputs.length, aMap.mOutputItemsCount); i++) {
			if (aRow.mOutputs[i] != null && !aRow.mOutputs[i].isEmpty()) mOutputs.add(EmiStack.of(aRow.mOutputs[i]));
		}
		for (int i = 0; i < Math.min(aRow.mFluidOutputs.length, aMap.mOutputFluidCount); i++) {
			if (aRow.mFluidOutputs[i] != null && !aRow.mFluidOutputs[i].isEmpty())
				mOutputs.add(EmiStack.of(aRow.mFluidOutputs[i].getFluid(), aRow.mFluidOutputs[i].getAmount()));
		}
	}

	@Override
	public GT6RecipeMapEmiCategory getCategory() {
		return mCategory;
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
		return GT6RecipeMapViewerMeta.CATEGORY_WIDTH;
	}

	@Override
	public int getDisplayHeight() {
		return GT6RecipeMapViewerMeta.CATEGORY_HEIGHT;
	}

	/** No recipe tree this card: the transfer/ghost face is batch 4 (the card's 不做 clause). */
	@Override
	public boolean supportsRecipeTree() {
		return false;
	}

	@Override
	public void addWidgets(WidgetHolder aWidgets) {
		for (int i = 0; i < Math.min(mRow.mInputs.length, mMap.mInputItemsCount); i++) {
			if (mRow.mInputs[i] == null || mRow.mInputs[i].isEmpty()) continue;
			int[] tPos = GT6RecipeMapViewerMeta.inputPos(i, mMap);
			if (tPos == null) continue; // past the 12th drawn slot (the meta contract)
			SlotWidget tSlot = aWidgets.add(new SlotWidget(EmiStack.of(mRow.mInputs[i]), tPos[0], tPos[1]));
			if (GT6RecipeMapViewerMeta.notConsumable(mRow.mInputs[i]))
				tSlot.appendTooltip(Component.translatable(GT6RecipeMapViewerMeta.NOT_CONSUMED_KEY));
		}
		for (int i = 0; i < Math.min(mRow.mOutputs.length, mMap.mOutputItemsCount); i++) {
			if (mRow.mOutputs[i] == null || mRow.mOutputs[i].isEmpty()) continue;
			int[] tPos = GT6RecipeMapViewerMeta.outputPos(i, mMap);
			if (tPos == null) continue; // past the 12th drawn slot (the meta contract)
			// issue #34: NO .large(true) — EMI's large form is a 26x26 box anchored at the
			// passed coordinate (SlotWidget.getBounds output branch), which on the meta's
			// 18px output pitch (107/125/143) overlaps each neighbour by 8px and pushes a
			// 3rd slot to x169 past the 166-wide category. Upstream NEI drew faithful 18px
			// slots — same as the JEI twin.
			SlotWidget tSlot = aWidgets.add(new SlotWidget(EmiStack.of(mRow.mOutputs[i]), tPos[0], tPos[1]));
			Component tChance = GT6RecipeMapViewerMeta.chanceLine(GT6RecipeMapViewerMeta.outputChance(mRow, i), mRow.mOutputs[i].getCount());
			if (tChance != null) tSlot.appendTooltip(tChance);
		}
		for (int i = 0; i < Math.min(mRow.mFluidInputs.length, mMap.mInputFluidCount); i++) {
			if (mRow.mFluidInputs[i] == null || mRow.mFluidInputs[i].isEmpty()) continue;
			int[] tPos = GT6RecipeMapViewerMeta.fluidInputPos(i);
			aWidgets.add(new SlotWidget(EmiStack.of(mRow.mFluidInputs[i].getFluid(), mRow.mFluidInputs[i].getAmount()), tPos[0], tPos[1]));
		}
		for (int i = 0; i < Math.min(mRow.mFluidOutputs.length, mMap.mOutputFluidCount); i++) {
			if (mRow.mFluidOutputs[i] == null || mRow.mFluidOutputs[i].isEmpty()) continue;
			int[] tPos = GT6RecipeMapViewerMeta.fluidOutputPos(i);
			aWidgets.add(new SlotWidget(EmiStack.of(mRow.mFluidOutputs[i].getFluid(), mRow.mFluidOutputs[i].getAmount()), tPos[0], tPos[1]));
		}
		int tY = GT6RecipeMapViewerMeta.textBaseY(mMap);
		for (Component tLine : GT6RecipeMapViewerMeta.costLines(mMap, mRow)) {
			aWidgets.addText(tLine, GT6RecipeMapViewerMeta.TEXT_X, tY, 0xFF000000, false);
			tY += GT6RecipeMapViewerMeta.TEXT_LINE_HEIGHT;
		}
	}
}
