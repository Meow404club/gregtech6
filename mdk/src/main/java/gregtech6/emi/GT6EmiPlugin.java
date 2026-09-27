package gregtech6.emi;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import net.minecraft.resources.ResourceLocation;

//? if forge {
import net.minecraft.world.item.crafting.Recipe;
//?} else {
/*import net.minecraft.world.item.crafting.RecipeHolder;
*///?}

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiStack;

import gregtech6.items.tools.GT6MaterialToolRecipe;
import gregtech6.jei.GT6RecipeViewerText;
import gregtech6.registry.GTMultiBlocks;

/**
 * The EMI integration plugin (task debt-emi-tier-b, the tier-b ruling of research
 * r-emi-native / decisions.2026-09-26-debt-emi) — the native twin of the two JEI faces,
 * closing the EMI-only (no JEI installed) gap where gt6 previously had zero EMI presence.
 *
 * <p>Detection contract: EMI discovers plugins by scanning the mod jars' bytecode for the
 * {@link EmiEntrypoint} annotation (EmiAgnosForge/EmiAgnosNeoForge.getPluginsAgnos walk
 * ModList.getAllScanData, javap-verified on both emi 1.1.24 artifacts) — no metadata
 * entrypoint, no mods.toml face, the same annotation-only shape as the vendored
 * ModularUIEmiPlugin precedent and GTCEu's GTEMIPlugin. The implicit no-arg constructor
 * satisfies the instantiation half.
 *
 * <p><b>THE JEMI RED LINE</b> (why this class must replicate the JEI faces completely, not
 * partially): with JEI installed alongside EMI, EMI's JEMI bridge (dev.emi.emi.jemi)
 * normally loads every JEI plugin face — but its register() first collects
 * {@code handledNamespaces = EmiAgnos.getPlugins().stream().map(EmiPluginContainer::id)
 * .collect(toSet())} (javap-verified bootstrap #2) and SKIPS every JEI recipe category
 * whose uid namespace is already handled by a native EMI plugin. The moment this class
 * ships, "gt6" enters that set and JEMI stops bridging GT6JeiPlugin's faces — so each
 * JEI face must have its native EMI twin here or EMI+JEI users LOSE it:
 * <table>
 * <tr><th>JEI face (GT6JeiPlugin)</th><th>native EMI twin (this class)</th></tr>
 * <tr><td>registerRecipes: addIngredientInfo(COKE_OVEN_ITEM,
 *     Component.translatable(INFO_KEY_COKE_OVEN))</td>
 *     <td>{@link EmiInfoRecipe} on the same item + the SAME shared text seam
 *     ({@link GT6RecipeViewerText#cokeOvenInfo()})</td></tr>
 * <tr><td>registerVanillaCategoryExtensions: the material_tool crafting extension
 *     (stamped output slot)</td>
 *     <td>the {@link GT6MaterialToolEmiRecipe} replacement rows (stamped output slot,
 *     see that class)</td></tr>
 * </table>
 * The gt6 JEI plugin has exactly these two faces (the r-emi-native census), so the twin
 * set is complete. Tier-c faces (RM recipe categories, multiblock/ByProduct info pages)
 * are ruled to later waves — they exist on neither side yet, so the red line stays
 * balanced.
 *
 * <p>Wiring: EMI stays compile-only on both legs (forge modCompileOnly / neoforge
 * compileOnly, version pinned 1.1.24, maven = terraformers) — zero run-classpath and
 * zero mods.toml contamination. On a dedicated server (or any install without EMI)
 * nothing references this class, so it is never loaded — the same dormant-impl contract
 * as GT6JeiPlugin.
 */
@EmiEntrypoint
public class GT6EmiPlugin implements EmiPlugin {

	/** The info-page recipe id (EmiRecipe ids index EMI's recipe lookups) — pinned literal. */
	public static final ResourceLocation INFO_PAGE_ID_COKE_OVEN = new ResourceLocation("gt6", "info/coke_oven");

	@Override
	public void register(EmiRegistry registry) {
		registerCokeOvenInfo(registry);
		registerMaterialToolRows(registry);
	}

	/**
	 * Face 1 — the coke oven structure info page, the native twin of
	 * GT6JeiPlugin.registerRecipes' addIngredientInfo: the same controller item, the same
	 * shared lang key through the viewer-neutral text seam. EmiInfoRecipe renders under
	 * EMI's INFO category (VanillaEmiRecipeCategories.INFO) attached to the item's page.
	 */
	private static void registerCokeOvenInfo(EmiRegistry registry) {
		registry.addRecipe(new EmiInfoRecipe(
				List.of(EmiStack.of(GTMultiBlocks.COKE_OVEN_ITEM.get())),
				List.of(GT6RecipeViewerText.cokeOvenInfo()),
				INFO_PAGE_ID_COKE_OVEN));
	}

	/**
	 * Face 2 — the gt6:material_tool crafting rows, the native twin of GT6JeiPlugin's
	 * vanilla-crafting extension. EMI's VanillaPlugin auto-wraps every recipe manager
	 * entry (EmiShapedRecipe on the neoforge leg, the generic shapeless-layout
	 * EmiCraftingRecipe on the forge leg — see GT6MaterialToolEmiRecipe's javadoc), all
	 * rendering the BARE result; each wrapper is invalidated by recipe id and the stamped
	 * {@link GT6MaterialToolEmiRecipe} takes its place. The id iteration is the file's one
	 * leg fork: 1.20.1 recipes carry their id, 1.21.1 wraps them in RecipeHolder.
	 */
	private static void registerMaterialToolRows(EmiRegistry registry) {
		Set<ResourceLocation> tMaterialToolIds = new HashSet<>();
		//? if forge {
		for (Recipe<?> tRecipe : registry.getRecipeManager().getRecipes()) {
			if (tRecipe instanceof GT6MaterialToolRecipe tTool) {
				tMaterialToolIds.add(tTool.getId());
				registry.addRecipe(new GT6MaterialToolEmiRecipe(tTool, tTool.getId()));
			}
		}
		//?} else {
		/*for (RecipeHolder<?> tHolder : registry.getRecipeManager().getRecipes()) {
			if (tHolder.value() instanceof GT6MaterialToolRecipe tTool) {
				tMaterialToolIds.add(tHolder.id());
				registry.addRecipe(new GT6MaterialToolEmiRecipe(tTool, tHolder.id()));
			}
		}
		*///?}
		if (!tMaterialToolIds.isEmpty()) {
			// removeRecipes registers a DEFERRED invalidator that filters the FINAL
			// combined recipe list at bake time — it cannot express "remove then re-add",
			// so the predicate must exclude this plugin's own replacements (same recipe
			// id) or they would be invalidated alongside the wrappers they replace.
			registry.removeRecipes(materialToolInvalidator(tMaterialToolIds));
		}
	}

	/**
	 * The invalidator: any recipe carrying a material_tool id EXCEPT this plugin's own
	 * replacement faces dies — that covers both auto-wrapper shapes (EmiShapedRecipe on
	 * neoforge, the generic EmiCraftingRecipe on forge) without naming either internal
	 * class, and leaves unrelated recipes untouched. Package-private static: the offline
	 * guard test drives exactly this seam.
	 */
	static Predicate<EmiRecipe> materialToolInvalidator(Set<ResourceLocation> aMaterialToolIds) {
		return aRecipe -> !(aRecipe instanceof GT6MaterialToolEmiRecipe)
				&& aMaterialToolIds.contains(aRecipe.getId());
	}
}
