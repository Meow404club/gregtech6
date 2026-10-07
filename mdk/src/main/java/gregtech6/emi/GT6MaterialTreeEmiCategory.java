package gregtech6.emi;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;

import gregtech6.recipes.tree.MaterialTreeDisplay;

/**
 * The material-tree category of the EMI leg (task debt-material-tree-b) — the native twin
 * of GT6MaterialTreeJeiCategory, the one-category-instance shape of the batch-1 precedent
 * (GT6RecipeMapEmiCategory's memoize form collapsed to a singleton: this card has exactly
 * one category, not one per map). The id mirrors the JEI RecipeType one-to-one
 * ({@code gt6:material_tree}) — the JEMI skip key stays balanced because both twins ship in
 * the same card (THE JEMI RED LINE, GT6EmiPlugin class doc). The icon is vanilla raw iron:
 * the ore-chain face without a GT item dependency (offline-constructible, the guard tests
 * build this instance).
 */
public final class GT6MaterialTreeEmiCategory extends EmiRecipeCategory {

	/** The single instance (the batch-1 memoize form, degenerated to one category). */
	public static final GT6MaterialTreeEmiCategory INSTANCE = new GT6MaterialTreeEmiCategory();

	private GT6MaterialTreeEmiCategory() {
		super(id(), EmiStack.of(Items.RAW_IRON));
	}

	/** {@code gt6:material_tree} — mirrors the JEI category uid one-to-one (the JEMI skip key). */
	public static ResourceLocation id() {
		//? if forge {
		return ResourceLocation.fromNamespaceAndPath("gt6", MaterialTreeDisplay.CATEGORY_UID_PATH);
		//?} else {
		/*return ResourceLocation.fromNamespaceAndPath("gt6", MaterialTreeDisplay.CATEGORY_UID_PATH);
		 *///?}
	}

	@Override
	public Component getName() {
		return Component.literal(MaterialTreeDisplay.CATEGORY_TITLE);
	}
}
