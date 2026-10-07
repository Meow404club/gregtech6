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

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiStack;

import gregtech6.items.tools.GT6MaterialToolRecipe;
import gregtech6.jei.GT6MultiblockPreviews;

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
 * <tr><td>registerRecipes: the multiblock preview rows ({@link GT6MultiblockPreviewEmiCategory}
 *     — the modern 3D structure page; before task multiblock-preview-infra this row was
 *     {@code addIngredientInfo}/EmiInfoRecipe, the text page both legs dropped)</td>
 *     <td>the {@link GT6MultiblockPreviewEmiCategory} rows — the SAME page through the
 *     shared {@code GT6MultiblockPreviewWidget}, materials as inputs, the controller item
 *     as outputs (the U anchor) + the workstation arm</td></tr>
 * <tr><td>registerVanillaCategoryExtensions: the material_tool crafting extension
 *     (stamped output slot)</td>
 *     <td>the {@link GT6MaterialToolEmiRecipe} replacement rows (stamped output slot,
 *     see that class)</td></tr>
 * <tr><td>registerCategories/registerRecipes + registerRecipeCatalysts: the generic RM
 *     categories (batch1) and the ore-generation distribution page (debt-ore-gen-display,
 *     the {@link GT6OreGenInfoEmiRecipe} rows with the invisible variant mounting)</td>
 *     <td>{@link GT6RecipeMapEmiCategory}/{@link GT6RecipeMapEmiRecipe} and
 *     {@link GT6OreGenInfoEmiCategory} — each JEI face shipped with its twin in the same
 *     card</td></tr>
 * </table>
 * The gt6 JEI plugin's faces each carry their native twin here (multiblock preview = face
 * 1, material_tool rows = face 2, RM categories = face 3, material tree = face 4), so the
 * red line stays balanced.
 *
 * <p>Wiring: EMI stays compile-only on both legs (forge modCompileOnly / neoforge
 * compileOnly, version pinned 1.1.24, maven = terraformers) — zero run-classpath and
 * zero mods.toml contamination. On a dedicated server (or any install without EMI)
 * nothing references this class, so it is never loaded — the same dormant-impl contract
 * as GT6JeiPlugin.
 */
@EmiEntrypoint
public class GT6EmiPlugin implements EmiPlugin {

	@Override
	public void register(EmiRegistry registry) {
		registerMultiblockPreviews(registry);
		registerEnergyInfoPages(registry);
		registerMaterialToolRows(registry);
		registerRecipeMapCategories(registry);
		registerOreGenInfo(registry);
		registerMaterialTree(registry);
	}

	/**
	 * Face 1 (task multiblock-preview-infra) — the multiblock structure preview pages, the
	 * native twin of GT6JeiPlugin's preview category: one {@link GT6MultiblockPreviewEmiCategory}
	 * singleton + one wrapper per {@link GT6MultiblockPreviews} table row (first version =
	 * the Coke Oven; data cards append rows, both legs pick them up). This replaces the
	 * tier-b EmiInfoRecipe text page — the same controller item anchor, the structure
	 * description now folded into the shared preview widget. The workstation arm mirrors
	 * the JEI leg's catalyst registration (the JEMI red line).
	 */
	private static void registerMultiblockPreviews(EmiRegistry registry) {
		registry.addCategory(GT6MultiblockPreviewEmiCategory.CATEGORY);
		for (GT6MultiblockPreviews.Entry tEntry : GT6MultiblockPreviews.entries()) {
			registry.addRecipe(new GT6MultiblockPreviewEmiCategory.PreviewEmiRecipe(tEntry));
			registry.addWorkstation(GT6MultiblockPreviewEmiCategory.CATEGORY,
					EmiStack.of(new net.minecraft.world.item.ItemStack(tEntry.item().get())));
		}
	}

	/**
	 * The energy-source pages (task energy-page-emi-twin, the approved energy-source-page
	 * design; the JEMI red line's arm on the info face): one native category
	 * ({@link GT6EnergyInfoEmiCategory}) + one page recipe per carrier chunk-run — the
	 * ten carriers (the nine accepted-energy ones + STEAM) off
	 * {@link gregtech6.jei.GT6EnergyCensus#carriers()}. This RETIRES the task
	 * viewer-energy-jump-gear {@code EmiInfoRecipe} text pages in the same change (no
	 * double-category window): the gear widget ({@link GT6RecipeMapEmiRecipe.GearJumpWidget})
	 * still lands via {@link #displayEnergyCarrierInfo} unchanged, because the new page
	 * recipes double-hook the pseudo {@link GT6EnergyCarrierEmiStack} into getInputs AND
	 * getOutputs (the E1 red-proof fix — displayRecipes reads byOutput only).
	 */
	private static void registerEnergyInfoPages(EmiRegistry registry) {
		GT6EnergyInfoEmiCategory.register(registry);
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
	 * Face 3 (task debt-jei-emi-batch1) — the generic RM recipe categories, the native
	 * twin of GT6JeiPlugin's registerCategories/registerRecipes loop over the same
	 * visible-map set (the shared {@link gregtech6.jei.GT6RecipeMapViewerMeta}, batch 2's
	 * full eligible-set opening): one {@link GT6RecipeMapEmiCategory} per map (the
	 * memoize factory, the GTCEu GTEMIPlugin.java:47-51 loop shape) and one
	 * {@link GT6RecipeMapEmiRecipe} per
	 * row, id'd by the map-sorted index (rows carry no registry id — that class's doc).
	 * The rows ride the shared {@link gregtech6.jei.GT6RecipeMapViewerMeta#rowsOf} seam
	 * (crucible-viewer-page): the live list for every stored-row map, the material-graph
	 * synthesis for the crucible pair — registration after the datapack pour sees every
	 * static-loader and JSON row AND the registered graph. The fake display rows
	 * (mFakeRecipe) need no wrapper special-casing: GT6RecipeMapEmiRecipe flattens the
	 * raw arrays, which is exactly what a display row is. THE JEMI RED LINE's new arm:
	 * this face and its JEI twin ship through the SAME seam, so the gt6 namespace stays
	 * balanced on both sides.
	 */
	private static void registerRecipeMapCategories(EmiRegistry registry) {
		for (gregtech6.recipes.RecipeMap tMap : gregtech6.jei.GT6RecipeMapViewerMeta.visibleMaps()) {
			GT6RecipeMapEmiCategory tCategory = GT6RecipeMapEmiCategory.CATEGORIES.apply(tMap);
			registry.addCategory(tCategory);
			// task issues #29/#34a + r11-emi-workstation-full — the workstation twin of the
			// JEI leg's RM catalysts (the JEMI red line): EVERY machine of the map's reverse
			// index registers (the EmiRecipes.java:107-109 per-category list, the upstream
			// mRecipeMachineList whole-list face); the first entry is iconOf's representative
			for (gregtech6.jei.GT6RecipeMapIcons.Workstation tWs : gregtech6.jei.GT6RecipeMapIcons.workstationsOf(tMap)) {
				registry.addWorkstation(tCategory, EmiStack.of(gregtech6.jei.GT6RecipeMapIcons.stackOf(tWs)));
			}
			List<gregtech6.recipes.Recipe> tRows = gregtech6.jei.GT6RecipeMapViewerMeta.rowsOf(tMap);
			tRows.sort(ROW_ORDER);
			for (int i = 0; i < tRows.size(); i++) {
				registry.addRecipe(new GT6RecipeMapEmiRecipe(tMap, tRows.get(i), tCategory, i));
			}
		}
	}

	/**
	 * Face 4 (task debt-ore-gen-display) — the ore-generation distribution page, the native
	 * twin of GT6JeiPlugin's registerOreGenInfoRows/registerRecipeCatalysts: one
	 * {@link GT6OreGenInfoEmiCategory}, one {@link GT6OreGenInfoEmiRecipe} per data-layer
	 * entry (the invisible variant mounting rides getOutputs), the rockGt pebble
	 * workstation. THE JEMI RED LINE's arm count moves to four — both sides ship the face
	 * in the same card, the gt6 namespace stays balanced.
	 */
	private static void registerOreGenInfo(EmiRegistry registry) {
		GT6OreGenInfoEmiCategory.register(registry);
	}

	/**
	 * The deterministic row order behind the EMI ids: the simplified NEI sortRecipes key
	 * (NEI_RecipeMap.java:477-513 EUt-first — fluid/input-count and oredict material keys
	 * collapse into the first-input string), the GT6RecipeMapEmiRecipe doc's contract.
	 */
	static final java.util.Comparator<gregtech6.recipes.Recipe> ROW_ORDER =
			java.util.Comparator.comparingLong((gregtech6.recipes.Recipe r) -> r.mEUt)
					.thenComparingLong(r -> r.mDuration)
					.thenComparing(r -> firstStackString(r.mInputs))
					.thenComparing(r -> firstStackString(r.mOutputs));

	private static String firstStackString(net.minecraft.world.item.ItemStack[] aStacks) {
		for (net.minecraft.world.item.ItemStack tStack : aStacks) if (tStack != null && !tStack.isEmpty()) return tStack.toString();
		return "";
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

	/**
	 * The machine-GUI progress-bar jump face (task debt-jei-emi-batch4), the native EMI
	 * twin of {@link gregtech6.jei.GT6JeiPlugin#openRecipeMapPage}: opens this map's
	 * category page via {@code EmiApi.displayRecipeCategory} (emi 1.1.24 xplat
	 * EmiApi.java:123 — builds the page from the registered category's live recipe list).
	 * Upstream opened the SAME handler page for both click arms (NEI_RecipeMap.java:75-76
	 * dual registration), so there is no use/recipe split to mirror. The category instance
	 * comes from the same {@link GT6RecipeMapEmiCategory#CATEGORIES} memoize the
	 * registration used, so it is the registered instance by construction. Never call
	 * unguarded: the class loads EMI API — only the ModList-gated
	 * {@link gregtech6.gui.GTViewerJump} router may reach it.
	 */
	public static boolean openRecipeMapPage(gregtech6.recipes.RecipeMap aMap) {
		if (aMap == null) return false;
		EmiApi.displayRecipeCategory(GT6RecipeMapEmiCategory.CATEGORIES.apply(aMap));
		return true;
	}

	/**
	 * The gear-port jump face (task viewer-energy-jump-gear), the native EMI twin of
	 * {@link gregtech6.jei.GT6JeiPlugin#openEnergyCarrierInfo}: opens the carrier's page
	 * via {@code EmiApi.displayRecipes} (EmiApi.java:131 — the recipes-for-stack page;
	 * since task energy-page-emi-twin the energy-source pages index under the pseudo
	 * stack through their getOutputs double hook, so the energy_info category surfaces).
	 * Never call unguarded: only the EMI-side gear widget reaches it (EMI present by
	 * construction there). The method reference is what the widget carries, so the
	 * offline click pin can substitute its own consumer.
	 */
	public static void displayEnergyCarrierInfo(gregapi.code.TagData aCarrier) {
		EmiApi.displayRecipes(GT6EnergyCarrierEmiStack.of(aCarrier));
	}

	/**
	 * The item jump face (task nav-s4-tree-screen), the native EMI twin of
	 * {@link gregtech6.jei.GT6JeiPlugin#openItemPage}: {@code EmiApi.displayRecipes} is the
	 * R axis (recipes-for-stack — the {@link #displayEnergyCarrierInfo} precedent, EmiApi
	 * :131). Never call unguarded: the class loads EMI API — only the preferredViewer
	 * routing in {@link gregtech6.gui.GT6MaterialTreeScreen#openInViewer} may reach it.
	 *
	 * @return false (no-op) when the stack is null/empty — offline and pre-init safe.
	 */
	public static boolean openItemPage(net.minecraft.world.item.ItemStack aStack) {
		if (aStack == null || aStack.isEmpty()) return false;
		EmiApi.displayRecipes(EmiStack.of(aStack));
		return true;
	}

	/**
	 * Face 4 (task debt-material-tree-b) — the per-material processing-tree displays, the
	 * native twin of GT6JeiPlugin's registerMaterialTreeRows (same builder sweep, same
	 * shared {@link gregtech6.recipes.tree.MaterialTreeDisplay} model, same {@code SHOWN}
	 * switch — the hideOreProcessingDiagrams precedent). One category (the singleton) +
	 * one recipe per ore-universe material. THE JEMI RED LINE's arm: this face and its JEI
	 * twin ship in the same card, so the gt6 namespace stays balanced on both sides.
	 *
	 * <p>Workstation arm (task debt-material-tree-c): the machines displayed on the tree's
	 * edges ride {@code addWorkstation} on the SAME builder sweep — clicking a
	 * Shredder/Sifter/Anvil/Crusher opens the category, the GTCEu registerWorkstation
	 * shape on the EMI leg (GTRecipeEMICategory's machines).
	 */
	private static void registerMaterialTree(EmiRegistry registry) {
		if (!gregtech6.recipes.tree.MaterialTreeDisplay.SHOWN) return;
		gregtech6.recipes.tree.MaterialTreeBuilder tTree = gregtech6.recipes.tree.MaterialTreeBuilder.build();
		registry.addCategory(GT6MaterialTreeEmiCategory.INSTANCE);
		java.util.List<gregtech6.recipes.tree.MaterialTreeDisplay> tDisplays =
				gregtech6.recipes.tree.MaterialTreeDisplay.buildAll(tTree);
		for (gregtech6.recipes.tree.MaterialTreeDisplay tDisplay : tDisplays) {
			registry.addRecipe(new GT6MaterialTreeEmiRecipe(tDisplay));
		}
		for (net.minecraft.world.item.ItemStack tStack : gregtech6.recipes.tree.MaterialTreeWorkstations.workstationStacks(tDisplays)) {
			registry.addWorkstation(GT6MaterialTreeEmiCategory.INSTANCE, EmiStack.of(tStack));
		}
	}
}
