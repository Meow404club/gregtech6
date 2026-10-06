package gregtech6.emi;

import java.util.List;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;

import gregtech6.worldgen.GT6OreGenInfoLayout;
import gregtech6.worldgen.OreDistributionInfo;

/**
 * One material's EMI face of the ore-generation distribution page (task
 * debt-ore-gen-display) — the hand-flattened form of the batch1 row precedent
 * (GT6RecipeMapEmiRecipe), rendering the shared {@link GT6OreGenInfoLayout} geometry the
 * JEI twin draws.
 *
 * <p>The invisible arm lives in {@link #getOutputs()}: every ore block item the material
 * owns rides the output list while {@link #addWidgets} draws only the representative slot
 * — EMI's focus matching keys on getOutputs, so U on any ore block item opens this row
 * (the JEI twin's addInvisibleIngredients(OUTPUT) form of the same GTCEu trick).
 *
 * <p>Id: {@code gt6:ore_gen_info/<index>} — the index is the data layer's deterministic
 * first-appearance position, so ids are stable across sessions.
 */
public class GT6OreGenInfoEmiRecipe implements EmiRecipe {

	public final OreDistributionInfo.Entry mEntry;
	private final ResourceLocation mId;
	private final List<EmiStack> mOutputs;

	public GT6OreGenInfoEmiRecipe(OreDistributionInfo.Entry aEntry, int aIndex) {
		mEntry = aEntry;
		//? if forge {
		mId = new ResourceLocation("gt6", "ore_gen_info/" + aIndex);
		//?} else {
		/*mId = ResourceLocation.fromNamespaceAndPath("gt6", "ore_gen_info/" + aIndex);
		 *///?}
		List<EmiStack> tOutputs = new java.util.ArrayList<>();
		for (ItemStack tStack : GT6OreGenInfoLayout.variantStacks(aEntry)) {
			tOutputs.add(EmiStack.of(tStack));
		}
		mOutputs = List.copyOf(tOutputs);
	}

	@Override
	public EmiRecipeCategory getCategory() {
		return GT6OreGenInfoEmiCategory.CATEGORY;
	}

	@Override
	public ResourceLocation getId() {
		return mId;
	}

	@Override
	public List<EmiIngredient> getInputs() {
		return List.of();
	}

	/** The invisible mounting: every registered ore block item of the material (offline: empty). */
	@Override
	public List<EmiStack> getOutputs() {
		return mOutputs;
	}

	@Override
	public int getDisplayWidth() {
		return GT6OreGenInfoLayout.WIDTH;
	}

	@Override
	public int getDisplayHeight() {
		return GT6OreGenInfoLayout.categoryHeight();
	}

	/** No recipe tree: the page is an information face, not a production step (the batch1 ruling). */
	@Override
	public boolean supportsRecipeTree() {
		return false;
	}

	@Override
	public void addWidgets(WidgetHolder aWidgets) {
		ItemStack tRep = GT6OreGenInfoLayout.representative(mEntry);
		if (!tRep.isEmpty()) {
			aWidgets.add(new dev.emi.emi.api.widget.SlotWidget(EmiStack.of(tRep),
					GT6OreGenInfoLayout.SLOT_X, GT6OreGenInfoLayout.SLOT_Y)).large(true);
		}
		aWidgets.addText(GT6OreGenInfoLayout.dimsRow(mEntry),
				GT6OreGenInfoLayout.DIMS_X, GT6OreGenInfoLayout.DIMS_Y, 0xFF000000, false);
		for (GT6OreGenInfoLayout.Row tRow : GT6OreGenInfoLayout.rows(mEntry)) {
			aWidgets.addText(tRow.component(), tRow.x(), tRow.y(), 0xFF000000, false);
		}
	}
}
