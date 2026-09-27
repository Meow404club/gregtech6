package gregtech6.emi;

import java.util.function.Function;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;

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
 * <p>The icon is the upstream NEI fallback: NEI_RecipeMap.init() drew the lit furnace
 * whenever a map's mRecipeMachineList was empty (:82 {@code Blocks.lit_furnace}) — and
 * the batch-2 adjudication (task debt-jei-emi-batch2) rules the per-map icon table
 * DEFERRED: the port's folded 15-arg RecipeMap carries no mRecipeMachineList, and the
 * machine registries are forward-keyed (machine row → RecipeMapSupplier, many machines
 * share one map), so a per-map item table would be new reverse-index infrastructure.
 * FURNACE stays the fallback form on every category until such a table exists.
 */
public final class GT6RecipeMapEmiCategory extends EmiRecipeCategory {

	/** One category instance per map (the memoize factory — GTRecipeEMICategory.CATEGORIES form). */
	public static final Function<RecipeMap, GT6RecipeMapEmiCategory> CATEGORIES = Util.memoize(GT6RecipeMapEmiCategory::new);

	public final RecipeMap mMap;

	private GT6RecipeMapEmiCategory(RecipeMap aMap) {
		super(idOf(aMap), EmiStack.of(Items.FURNACE));
		mMap = aMap;
	}

	/** {@code gt6:recipe_map/<internal>} — mirrors the JEI category uid one-to-one (the JEMI skip key). */
	public static ResourceLocation idOf(RecipeMap aMap) {
		//? if forge {
		return new ResourceLocation("gt6", "recipe_map/" + aMap.mNameInternal);
		//?} else {
		/*return ResourceLocation.fromNamespaceAndPath("gt6", "recipe_map/" + aMap.mNameInternal);
		 *///?}
	}

	@Override
	public Component getName() {
		return Component.literal(mMap.mNameLocal);
	}
}
