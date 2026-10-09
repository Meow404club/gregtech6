package gregtech6.emi;

import java.util.function.Function;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;

import gregtech6.jei.GT6RecipeMapIcons;
import gregtech6.recipes.RecipeMap;

/**
 * The generic RM recipe category of the EMI leg (task debt-jei-emi-batch1) — the
 * memoize-factory + one-category-per-map shape of the GTCEu precedent
 * (GTRecipeEMICategory.java:27-28 {@code Util.memoize(GTRecipeEMICategory::new)}, the
 * loop registration at GTEMIPlugin.java:47-51), carrying THIS port's map face instead of
 * the GTCEu GTRecipeCategory face. The geometry/text/tooltip all come from the shared
 * {@link gregtech6.jei.GT6RecipeMapViewerMeta} seam — the same functions the JEI twin
 * renders from, the card's 双 viewer 共享 clause.
 *
 * <p>The icon (task issues #29/#34a, GitHub #29a) is the map's representative machine item from
 * the shared {@link GT6RecipeMapIcons} table — the batch-2 adjudication (task
 * debt-jei-emi-batch2) ruled the table DEFERRED, this card landed it. The furnace stack
 * survives ONLY as that table's whitelist fallback for the four DECLARED-empty maps —
 * the upstream NEI_RecipeMap.init() lit-furnace default whenever a map's
 * mRecipeMachineList was empty (:82 {@code Blocks.lit_furnace}), kept faithful.
 */
public final class GT6RecipeMapEmiCategory extends EmiRecipeCategory {

	/** One category instance per map (the memoize factory — GTRecipeEMICategory.CATEGORIES form). */
	public static final Function<RecipeMap, GT6RecipeMapEmiCategory> CATEGORIES = Util.memoize(GT6RecipeMapEmiCategory::new);

	public final RecipeMap mMap;

	private GT6RecipeMapEmiCategory(RecipeMap aMap) {
		super(idOf(aMap), EmiStack.of(GT6RecipeMapIcons.iconOf(aMap)));
		mMap = aMap;
	}

	/** {@code gt6:recipe_map/<internal>} — mirrors the JEI category uid one-to-one (the JEMI skip key). */
	public static ResourceLocation idOf(RecipeMap aMap) {
		//? if forge {
		return ResourceLocation.fromNamespaceAndPath("gt6", "recipe_map/" + aMap.mNameInternal);
		//?} else {
		/*return ResourceLocation.fromNamespaceAndPath("gt6", "recipe_map/" + aMap.mNameInternal);
		 *///?}
	}

	/**
	 * The per-map category title (task issues #29/#34a, GitHub #29b) — the shared
	 * {@link gregtech6.jei.GT6RecipeMapViewerMeta#titleKey} formula, the EMI twin of the
	 * JEI leg's translatable getTitle.
	 */
	@Override
	public Component getName() {
		return Component.translatable(gregtech6.jei.GT6RecipeMapViewerMeta.titleKey(mMap));
	}
}
