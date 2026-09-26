package gregtech6.emi;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;

import dev.emi.emi.api.recipe.EmiPatternCraftingRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;

import gregtech6.items.tools.GT6MaterialToolRecipe;

/**
 * The EMI-native face of one {@code gt6:material_tool} crafting row (task debt-emi-tier-b)
 * — the port of the JEI extension seam (gregtech6.jei.GT6MaterialToolJeiExtension, task
 * r3-jei-tool-output-tint) to EMI's own API. Why a replacement at all:
 * <ul>
 * <li><b>1.21.1 neoforge leg</b> — GT6MaterialToolRecipe extends ShapedRecipe, so EMI's
 *     VanillaPlugin auto-wraps every row as an {@code EmiShapedRecipe} rendering the BARE
 *     {@code getResultItem} stack (Steel fallback) — the same #6 symptom JEI had.</li>
 * <li><b>1.20.1 forge leg</b> — the recipe is a CraftingRecipe wrapper, NOT a
 *     ShapedRecipe, so VanillaPlugin's shaped/shapeless instanceof dispatch misses and the
 *     generic tail wraps it as a shapeless-layout {@code EmiCraftingRecipe} — still the
 *     bare output, plus the wrong (patternless) grid.</li>
 * </ul>
 * This class replaces both wrappers (see GT6EmiPlugin.register: the auto-wrappers are
 * invalidated by recipe id and this face is added in their place): same vanilla crafting
 * category (inherited {@code getCategory()} = VanillaEmiRecipeCategories.CRAFTING — the
 * recipes stay in the player's familiar crafting tab, the b' ruling's EMI twin), the
 * actual pattern layout through {@link EmiPatternCraftingRecipe}'s 3x3 slot grid, and the
 * output slot carrying the row's stamped representative stack
 * ({@link GT6MaterialToolRecipe#stampedDisplayResult()} — the same display seam the JEI
 * extension consumes, one accessor both viewers).
 *
 * <p>The id rides in explicitly from the caller: on 1.20.1 the recipe carries it
 * ({@code getId()}), on 1.21.1 it lives on the surrounding {@code RecipeHolder} — the
 * plugin's iteration fork hands it over either way, keeping this class fork-free except
 * the pattern-dims accessor below.
 *
 * <p>The padded input list mirrors EMI's own EmiShapedRecipe.padIngredients: row-major
 * pattern cells become {@link EmiIngredient#of(Ingredient)}, out-of-pattern cells become
 * {@link EmiStack#EMPTY} — a full 9-entry 3x3 grid.
 */
public class GT6MaterialToolEmiRecipe extends EmiPatternCraftingRecipe {

	public GT6MaterialToolEmiRecipe(GT6MaterialToolRecipe aRecipe, ResourceLocation aId) {
		super(padInputs(aRecipe), EmiStack.of(aRecipe.stampedDisplayResult()), aId);
	}

	private static List<EmiIngredient> padInputs(GT6MaterialToolRecipe aRecipe) {
		List<Ingredient> tPattern = aRecipe.getIngredients();
		// The pattern dims — the file's one leg fork: the forge leg exposes them through
		// the wrapper accessors (the wrapper is not an IShapedRecipe there), the neoforge
		// leg reads them straight off the ShapedRecipe superclass.
		//? if forge {
		int tWidth = aRecipe.recipeWidth();
		int tHeight = aRecipe.recipeHeight();
		//?} else {
		/*int tWidth = aRecipe.getWidth();
		int tHeight = aRecipe.getHeight();
		*///?}
		List<EmiIngredient> tInputs = new ArrayList<>(9);
		for (int tRow = 0; tRow < 3; tRow++) {
			for (int tCol = 0; tCol < 3; tCol++) {
				int tIndex = tRow * tWidth + tCol;
				if (tRow < tHeight && tCol < tWidth && tIndex < tPattern.size()) {
					tInputs.add(EmiIngredient.of(tPattern.get(tIndex)));
				} else {
					tInputs.add(EmiStack.EMPTY);
				}
			}
		}
		return tInputs;
	}

	@Override
	public SlotWidget getInputWidget(int aIndex, int aX, int aY) {
		// The 3x3 grid cell: in-pattern cells carry the ingredient, padding stays empty
		// (EmiPatternCraftingRecipe.addWidgets calls this for all 9 cells).
		if (aIndex < input.size() && !input.get(aIndex).isEmpty()) {
			return new SlotWidget(input.get(aIndex), aX, aY);
		}
		return new SlotWidget(EmiStack.EMPTY, aX, aY);
	}

	@Override
	public SlotWidget getOutputWidget(int aX, int aY) {
		// The base addWidgets already chains .large(true).recipeContext(this) on this slot
		// — the stamped output stack itself was frozen into the super ctor's output field.
		return new SlotWidget(output, aX, aY);
	}
}
