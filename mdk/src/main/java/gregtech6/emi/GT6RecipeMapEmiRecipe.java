package gregtech6.emi;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.TextureWidget;
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
		// the two-layer backdrop FIRST (task 34-viewer-gui-bg, GitHub #34) — render
		// order = add order, so these two TextureWidgets are the bottom of the z stack:
		// the grey machines/NEI.png plate, then the per-map machine GUI band over it
		// (upstream NEI_RecipeMap.drawBackground :629-635). The ctor
		// (texture, x, y, width, height, u, v) defaults to a 256x256 canvas — both the
		// amazawa redraws and the upstream panels are 256x256 (assets/README.md reskin
		// section). The slots below land on the baked-in art via the meta's VIEWER exits
		// (the sOffset(-5,-11) panel fold happened once inside GT6RecipeMapViewerMeta).
		int[] tPlate = GT6RecipeMapViewerMeta.PLATE_CROP, tBand = GT6RecipeMapViewerMeta.BAND_CROP;
		aWidgets.add(new TextureWidget(GT6RecipeMapViewerMeta.PLATE_TEXTURE, 0, 0, tPlate[2], tPlate[3], tPlate[0], tPlate[1]));
		aWidgets.add(new TextureWidget(GT6RecipeMapViewerMeta.guiTexture(mMap), 0, 0, tBand[2], tBand[3], tBand[0], tBand[1]));
		// the representative machine on the plate's baked-in gear spot (task
		// debt-viewer-polish): upstream NEI_RecipeMap.java:278 drew mRecipeMachineList
		// at GUI (152,83) as a bare item — the SlotWidget's drawBack(false) is EMI's
		// no-frame form (bounds stay 18x18 for the hover/tooltip face; the JEI twin draws
		// plain GuiGraphics.renderItem). null = no tabled machine (the four
		// furnace-fallback maps) — the upstream isEmpty() guard, no furnace default.
		net.minecraft.world.item.ItemStack tMachine = GT6RecipeMapViewerMeta.machineIcon(mMap);
		if (tMachine != null) {
			int[] tIconPos = GT6RecipeMapViewerMeta.machineIconPos();
			aWidgets.add(new SlotWidget(EmiStack.of(tMachine), tIconPos[0], tIconPos[1]).drawBack(false));
		}
		for (int i = 0; i < Math.min(mRow.mInputs.length, mMap.mInputItemsCount); i++) {
			if (mRow.mInputs[i] == null || mRow.mInputs[i].isEmpty()) continue;
			int[] tPos = GT6RecipeMapViewerMeta.viewerInputPos(i, mMap);
			if (tPos == null) continue; // past the 12th drawn slot (the meta contract)
			SlotWidget tSlot = aWidgets.add(new SlotWidget(EmiStack.of(mRow.mInputs[i]), tPos[0], tPos[1]));
			if (GT6RecipeMapViewerMeta.notConsumable(mRow.mInputs[i]))
				tSlot.appendTooltip(Component.translatable(GT6RecipeMapViewerMeta.NOT_CONSUMED_KEY));
		}
		for (int i = 0; i < Math.min(mRow.mOutputs.length, mMap.mOutputItemsCount); i++) {
			if (mRow.mOutputs[i] == null || mRow.mOutputs[i].isEmpty()) continue;
			int[] tPos = GT6RecipeMapViewerMeta.viewerOutputPos(i, mMap);
			if (tPos == null) continue; // past the 12th drawn slot (the meta contract)
			// issue #34: NO .large(true) — EMI's large form is a 26x26 box anchored at the
			// passed coordinate (SlotWidget.getBounds output branch), which on the 18px
			// output pitch overlaps each neighbour by 8px and pushes a 3rd slot past the
			// 166-wide category. Upstream NEI drew faithful 18px slots — same as the JEI twin.
			SlotWidget tSlot = aWidgets.add(new SlotWidget(EmiStack.of(mRow.mOutputs[i]), tPos[0], tPos[1]));
			Component tChance = GT6RecipeMapViewerMeta.chanceLine(GT6RecipeMapViewerMeta.outputChance(mRow, i), mRow.mOutputs[i].getCount());
			if (tChance != null) tSlot.appendTooltip(tChance);
		}
		for (int i = 0; i < Math.min(mRow.mFluidInputs.length, mMap.mInputFluidCount); i++) {
			if (mRow.mFluidInputs[i] == null || mRow.mFluidInputs[i].isEmpty()) continue;
			int[] tPos = GT6RecipeMapViewerMeta.viewerFluidInputPos(i);
			aWidgets.add(new SlotWidget(EmiStack.of(mRow.mFluidInputs[i].getFluid(), mRow.mFluidInputs[i].getAmount()), tPos[0], tPos[1]));
		}
		for (int i = 0; i < Math.min(mRow.mFluidOutputs.length, mMap.mOutputFluidCount); i++) {
			if (mRow.mFluidOutputs[i] == null || mRow.mFluidOutputs[i].isEmpty()) continue;
			int[] tPos = GT6RecipeMapViewerMeta.viewerFluidOutputPos(i);
			aWidgets.add(new SlotWidget(EmiStack.of(mRow.mFluidOutputs[i].getFluid(), mRow.mFluidOutputs[i].getAmount()), tPos[0], tPos[1]));
		}
		// drawExtras (:680-717 verbatim arithmetic) at the shared panel-system text band.
		int tY = GT6RecipeMapViewerMeta.TEXT_BASE_Y;
		for (Component tLine : GT6RecipeMapViewerMeta.costLines(mMap, mRow)) {
			aWidgets.addText(tLine, GT6RecipeMapViewerMeta.TEXT_X, tY, 0xFF000000, false);
			tY += GT6RecipeMapViewerMeta.TEXT_LINE_HEIGHT;
		}
	}
}
