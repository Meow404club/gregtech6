package gregtech6.jei;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;

import gregapi.code.TagData;
import gregtech6.items.tools.GT6MaterialToolRecipe;
import gregtech6.recipes.RecipeMap;

/**
 * The JEI integration plugin (task jei-integration, ADR 2026-09-02-jei-dependency) —
 * the first third-party mod integration of the port.
 *
 * <p>Detection contract: JEI discovers plugins by scanning for the {@link JeiPlugin} annotation
 * and instantiating the class through its NO-ARG constructor ("All {@code IModPlugin} must have
 * this annotation and a constructor with no arguments", JeiPlugin.java — JEI 1.20.1 CommonApi);
 * the implicit default constructor satisfies the second half. Same shape as the GTCEu Modern
 * precedent (GTJEIPlugin.java:43-44: {@code @JeiPlugin} + {@code implements IModPlugin}).
 *
 * <p>Dual-node wiring (task jei-dual-wiring): this single shared source compiles against
 * both pinned JEI stacks — 1.20.1 Forge (mezz.jei:jei-1.20.1-{common-api,forge-api}:15.62.0.216,
 * modCompileOnly/modRuntimeOnly, mdk/build.forge.gradle.kts) and 1.21.1 NeoForge
 * (mezz.jei:jei-1.21.1-{common-api,neoforge-api}:19.52.0.422, plain compileOnly/runtimeOnly —
 * the moddev plugin registers no mod* remapping configurations, live configuration probe
 * 2026-09-04). javap on the two generations shows the consumed faces identical ({@code IModPlugin}
 * 20 methods with {@code getPluginUid() : ResourceLocation} abstract, {@code IRecipeRegistration}
 * including {@code addIngredientInfo(ItemLike, Component...)}), so the plugin faces stay
 * fork-free; the one fork in this file is the extension registration call — the two JEI
 * generations renamed the crafting-category extension API (15.x
 * {@code addCategoryExtension} / 19.x {@code addExtension}, see
 * {@link #registerVanillaCategoryExtensions}). The class stays strictly on the
 * loader-neutral common API — the platform packages ({@code mezz.jei.api.forge} /
 * {@code mezz.jei.api.neoforge}) are deliberately untouched, pinned by the GT6JeiPluginTest
 * bytecode guard (noPlatformSpecificJeiApiInBytecode).
 *
 * <p>Self-contained by card ruling: this class touches nothing in {@code GT6Mod} /
 * {@code GTModBusListener} (frozen) — JEI loads it on its own when present. JEI is a
 * client-only mod, so on a dedicated server the JEI jar sits dormant on the classpath and this
 * class is never loaded (harmless-impl face, runServer gate).
 *
 * <p>Scope history: the original card ruled NO custom {@code IRecipeCategory} and hung the
 * coke oven content on the built-in ingredient info page ({@code addIngredientInfo}) —
 * task multiblock-preview-infra superseded that ruling (the 2026-09-06 user decision to
 * follow the GTCEu convention): the text page is REPLACED by the custom
 * {@link GT6MultiblockPreviewJeiCategory} 3D structure page (one row per
 * {@link GT6MultiblockPreviews} table entry, first version = the Coke Oven), with the
 * description text folded into the page. The (task jei-tool-output-tint) vanilla crafting
 * category EXTENSION for {@code gt6:material_tool} rows stays an extension, not a custom
 * category: the recipes stay in the player's familiar crafting tab with unchanged search
 * behaviour, only the output slot gains the per-material representative stack (the b'
 * ruling, research.issues-r3-tool-jei).
 */
@JeiPlugin
public class GT6JeiPlugin implements IModPlugin {

	/** Stable plugin uid path — the offline test pins both halves against drift. */
	public static final String PLUGIN_UID_PATH = "jei_plugin";

	/** Stable plugin uid — one constant instance, the offline test pins it against drift. */
	public static final ResourceLocation PLUGIN_UID = new ResourceLocation("gt6", PLUGIN_UID_PATH);

	/** The live runtime, stashed by {@link #onRuntimeAvailable} for the jump face below. */
	private static mezz.jei.api.runtime.IJeiRuntime sRuntime;

	@Override
	public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime aRuntime) {
		sRuntime = aRuntime;
	}

	@Override
	public void onRuntimeUnavailable() {
		sRuntime = null;
	}

	@Override
	public ResourceLocation getPluginUid() {
		return PLUGIN_UID;
	}

	@Override
	public void registerRecipes(IRecipeRegistration registration) {
		registerMultiblockPreviewRows(registration);
		registerEnergyInfoPages(registration);
		registerRecipeMapCategoriesRows(registration);
		registerOreGenInfoRows(registration);
		registerMaterialTreeRows(registration);
	}

	/**
	 * Face 1 (task multiblock-preview-infra) — the multiblock structure preview pages: one
	 * {@link GT6MultiblockPreviewJeiCategory.Wrapper} per {@link GT6MultiblockPreviews}
	 * table row (first version = the Coke Oven; data cards append rows). This replaces the
	 * addIngredientInfo text page this plugin carried since jei-integration — the same
	 * controller-item anchor, the structure description folded into the 3D page. The EMI
	 * plugin registers the same rows natively — the JEMI red line stays balanced.
	 */
	private static void registerMultiblockPreviewRows(IRecipeRegistration registration) {
		List<GT6MultiblockPreviewJeiCategory.Wrapper> tRows = new ArrayList<>();
		for (GT6MultiblockPreviews.Entry tEntry : GT6MultiblockPreviews.entries()) {
			tRows.add(GT6MultiblockPreviewJeiCategory.wrapperOf(tEntry));
		}
		registration.addRecipes(GT6MultiblockPreviewJeiCategory.RECIPE_TYPE, tRows);
	}

	/**
	 * The per-carrier energy-source pages (task energy-page-jei-leg, the approved 形态A —
	 * this REPLACES the nine addIngredientInfo text pages the viewer-energy-jump-gear card
	 * hung here, the same-commit retirement clause: a transition period with both
	 * categories would double-hit the same focus). One {@link GT6EnergyInfoJeiCategory}
	 * recipe per page over {@link GT6EnergyCensus#carriers()} — the nine pinned carriers
	 * plus STEAM as the tenth; overflowing families continue on further page recipes of
	 * the SAME carrier (the packer's cut, JEI's native page arrows). Every page mounts its
	 * carrier as the INPUT slot, so {@link #openEnergyCarrierInfo}'s focus lands here
	 * unconditionally (the E1 POC green light) — the gear-port arm below is untouched.
	 * The EMI leg keeps its EmiInfoRecipe text pages until its own twin card (the JEMI red
	 * line stays balanced per-leg).
	 */
	private static void registerEnergyInfoPages(IRecipeRegistration registration) {
		GT6EnergyInfoJeiCategory tCategory = new GT6EnergyInfoJeiCategory();
		List<GT6EnergyInfoJeiCategory.Page> tPages = new ArrayList<>();
		for (TagData tCarrier : GT6EnergyCensus.carriers()) tPages.addAll(GT6EnergyInfoJeiCategory.pagesOf(tCarrier));
		registration.addRecipes(tCategory.getRecipeType(), tPages);
	}

	/**
	 * The energy-carrier pseudo ingredient (task viewer-energy-jump-gear, the r6-30
	 * phase-2 design): the nine accepted-energy carriers register with an EMPTY
	 * {@code allIngredients} list — never in the ingredient list / search index (the
	 * r6-30-flagged POC face). The per-carrier info pages themselves ride
	 * {@link #registerRecipes} (the addIngredientInfo overload lives on
	 * IRecipeRegistration, not here). Both pinned generations expose the same
	 * {@code IModPlugin.registerIngredients} hook.
	 */
	@Override
	public void registerIngredients(mezz.jei.api.registration.IModIngredientRegistration registration) {
		registration.register(GT6EnergyCarrierJei.TYPE, java.util.List.of(), GT6EnergyCarrierJei.HELPER, GT6EnergyCarrierJei.RENDERER);
	}

	/**
	 * The generic RM category rows (task debt-jei-emi-batch1, batch 2 full opening): every
	 * map the shared {@link GT6RecipeMapViewerMeta} exposes (since batch 2: the whole
	 * eligible set) gets its {@link GT6RecipeMapJeiCategory} rows — the modern counterpart
	 * of the upstream one-NEI_RecipeMap-per-map registration (NEI_GT_API_Config.java:62).
	 * The rows ride the shared {@link GT6RecipeMapViewerMeta#rowsOf} seam: the live list as
	 * a defensive copy for every stored-row map, the material-graph synthesis for the
	 * crucible pair (crucible-viewer-page — registration runs client-side after the server
	 * datapack pour, so the graph and the JSON smoke rows are already in). The EMI plugin
	 * registers the same maps natively through the same seam — the JEMI red line
	 * (GT6EmiPlugin class doc) stays balanced: every gt6 uid namespace face this plugin
	 * adds has its native EMI twin.
	 */
	private static void registerRecipeMapCategoriesRows(IRecipeRegistration registration) {
		for (RecipeMap tMap : GT6RecipeMapViewerMeta.visibleMaps()) {
			registration.addRecipes(GT6RecipeMapJeiCategory.recipeTypeOf(tMap), GT6RecipeMapViewerMeta.rowsOf(tMap));
		}
	}

	/**
	 * Face 3 (task debt-ore-gen-display) — the ore-generation distribution page: ONE
	 * {@link GT6OreGenInfoJeiCategory}, one row per {@code OreDistributionInfo.Entry} (the
	 * data card's 118 material aggregates). The rows carry the invisible variant mounting
	 * (the category's setRecipe), so U on any ore block item reaches the page. The EMI
	 * plugin registers the same entries natively — the JEMI red line stays balanced.
	 */
	private static void registerOreGenInfoRows(IRecipeRegistration registration) {
		GT6OreGenInfoJeiCategory tCategory = new GT6OreGenInfoJeiCategory();
		registration.addRecipes(tCategory.getRecipeType(),
				new ArrayList<>(gregtech6.worldgen.OreDistributionInfo.entries()));
	}

	@Override
	public void registerCategories(IRecipeCategoryRegistration registration) {
		// task multiblock-preview-infra — the 3D structure page (the category builds its
		// controller-item icon internally, the GTCEu MultiblockInfoJeiCategory shape)
		registration.addRecipeCategories(new GT6MultiblockPreviewJeiCategory(
				registration.getJeiHelpers().getGuiHelper()));
		for (RecipeMap tMap : GT6RecipeMapViewerMeta.visibleMaps()) {
			// task issues #29/#34a — the per-map machine icon (GitHub #29a), the shared
			// GT6RecipeMapIcons table drawn through JEI's ingredient-drawable face
			mezz.jei.api.gui.drawable.IDrawable tIcon =
					registration.getJeiHelpers().getGuiHelper().createDrawableItemStack(GT6RecipeMapIcons.iconOf(tMap));
			registration.addRecipeCategories(new GT6RecipeMapJeiCategory(tMap, tIcon));
		}
		registration.addRecipeCategories(new GT6OreGenInfoJeiCategory());
		registration.addRecipeCategories(new GT6EnergyInfoJeiCategory());
		if (gregtech6.recipes.tree.MaterialTreeDisplay.SHOWN) {
			registration.addRecipeCategories(new GT6MaterialTreeJeiCategory());
		}
	}

	/**
	 * The ore page's catalyst: the surface-rock's collected rockGt pebble — the port's
	 * stand-in for GTCEu's prospector catalysts (GTOreVeinInfoCategory.java:55-59). JEI
	 * also uses it as the category icon (the category draws none). Second arm (task
	 * debt-material-tree-c): the material tree's own chain machines — clicking a
	 * Shredder/Sifter/Anvil/Crusher opens the {@code gt6:material_tree} page, the GTCEu
	 * registerWorkstation shape on the JEI leg.
	 *
	 * <p>Third arm (task issues #29/#34a + r11-emi-workstation-full): every RM category
	 * carries ALL of its map's machines as catalysts (the reverse index's full list — the
	 * upstream mRecipeMachineList face, JEI catalysts are a list too) — so U on any tier of
	 * any machine family reaches its recipes. The EMI leg mirrors the same list through
	 * addWorkstation (the JEMI red line).
	 */
	@Override
	public void registerRecipeCatalysts(mezz.jei.api.registration.IRecipeCatalystRegistration registration) {
		// task multiblock-preview-infra — the controller items are the preview page's U
		// anchors (the EMI leg mirrors this through addWorkstation)
		registration.addRecipeCatalysts(GT6MultiblockPreviewJeiCategory.RECIPE_TYPE,
				GT6MultiblockPreviews.entries().stream()
						.map(tEntry -> new ItemStack(tEntry.item().get()))
						.toArray(ItemStack[]::new));
		for (RecipeMap tMap : GT6RecipeMapViewerMeta.visibleMaps()) {
			List<ItemStack> tCatalysts = new ArrayList<>();
			for (GT6RecipeMapIcons.Workstation tWs : GT6RecipeMapIcons.workstationsOf(tMap)) {
				tCatalysts.add(GT6RecipeMapIcons.stackOf(tWs));
			}
			registration.addRecipeCatalysts(GT6RecipeMapJeiCategory.recipeTypeOf(tMap), tCatalysts.toArray(new ItemStack[0]));
		}
		GT6OreGenInfoJeiCategory tCategory = new GT6OreGenInfoJeiCategory();
		ItemStack tCatalyst = gregtech6.worldgen.GT6OreGenInfoLayout.catalystStack();
		if (!tCatalyst.isEmpty()) registration.addRecipeCatalysts(tCategory.getRecipeType(), tCatalyst);
		registerMaterialTreeCatalysts(registration);
	}

	/**
	 * The material-tree catalysts (task debt-material-tree-c): the machines displayed on
	 * the tree's edges ({@code MaterialTreeWorkstations.workstationStacks} over the same
	 * displays {@link #registerMaterialTreeRows} assembles). ponytail: this re-assembles
	 * the displays — one extra hash-read walk + builder sweep per session on the JEI leg;
	 * share the instances via a small memo only if a profiler ever cares.
	 */
	private static void registerMaterialTreeCatalysts(mezz.jei.api.registration.IRecipeCatalystRegistration registration) {
		if (!gregtech6.recipes.tree.MaterialTreeDisplay.SHOWN) return;
		java.util.List<gregtech6.recipes.tree.MaterialTreeDisplay> tDisplays =
				gregtech6.recipes.tree.MaterialTreeDisplay.buildAll(gregtech6.recipes.tree.MaterialTreeBuilder.build());
		java.util.List<ItemStack> tStacks = gregtech6.recipes.tree.MaterialTreeWorkstations.workstationStacks(tDisplays);
		if (!tStacks.isEmpty())
			registration.addRecipeCatalysts(new GT6MaterialTreeJeiCategory().getRecipeType(), tStacks.toArray(new ItemStack[0]));
	}

	/**
	 * The material-tree displays (task debt-material-tree-b, ruling 2026-09-26-debt-material-tree
	 * card B): one aggregated per-material processing-tree display per ore-universe material,
	 * derived from ONE {@code MaterialTreeBuilder.build()} sweep (the hash-index red line —
	 * startup-only, the EMI twin runs its own). The {@code SHOWN} switch is the
	 * hideOreProcessingDiagrams precedent (GTCEu ConfigHolder.java:221).
	 */
	private static void registerMaterialTreeRows(IRecipeRegistration registration) {
		if (!gregtech6.recipes.tree.MaterialTreeDisplay.SHOWN) return;
		GT6MaterialTreeJeiCategory tCategory = new GT6MaterialTreeJeiCategory();
		java.util.List<gregtech6.recipes.tree.MaterialTreeDisplay> tDisplays =
				gregtech6.recipes.tree.MaterialTreeDisplay.buildAll(gregtech6.recipes.tree.MaterialTreeBuilder.build());
		if (!tDisplays.isEmpty()) registration.addRecipes(tCategory.getRecipeType(), tDisplays);
	}

	/**
	 * The gt6:material_tool display seam (task jei-tool-output-tint, GitHub #6 round 3):
	 * without an extension JEI renders the crafting category's output slot from the bare
	 * {@code getResultItem} stack, so every material row shows the Steel fallback colour
	 * while real crafts stamp the identity. The registered {@link GT6MaterialToolJeiExtension}
	 * swaps in the row's stamped representative stack — same category, same search
	 * behaviour, zero crafting-semantics change. The registration call itself is the file's
	 * one leg fork: the two JEI generations renamed the extension API (see the extension
	 * class javadoc for the lineage).
	 */
	@Override
	public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
		//? if forge {
		// 15.x: the extension wraps the recipe instance (IExtendableRecipeCategory.addCategoryExtension).
		registration.getCraftingCategory().addCategoryExtension(GT6MaterialToolRecipe.class, GT6MaterialToolJeiExtension::new);
		//?} else {
		/*// 19.x: a stateless singleton receives the recipe in each call (since 16.0.0).
		registration.getCraftingCategory().addExtension(GT6MaterialToolRecipe.class, GT6MaterialToolJeiExtension.INSTANCE);
		*///?}
	}

	/**
	 * The machine-GUI progress-bar jump face (task debt-jei-emi-batch4): opens this map's
	 * category page in the JEI recipes gui — the modern counterpart of upstream
	 * GT_RectHandler's {@code GuiCraftingRecipe/GuiUsageRecipe.openRecipeGui(mNEI)}
	 * (NEI_RecipeMap.java:399-426). Upstream registered the SAME handler instance as
	 * crafting AND usage handler (NEI_RecipeMap.java:75-76 {@code API.registerRecipeHandler}
	 * + {@code registerUsageHandler}), so the left/right click arms opened the identical
	 * page — this port keeps that single-destination behaviour, no use/recipe split.
	 * <p>
	 * {@code IRecipesGui.showTypes(List)} exists verbatim on both pinned generations
	 * (15.x CommonApi IRecipesGui.java:48, 19.x Common IRecipesGui.java:49), and
	 * {@code RecipeType.equals} is uid+class (RecipeType.java), so the type rebuilt through
	 * {@link GT6RecipeMapJeiCategory#recipeTypeOf} — the same formula the registration used
	 * — resolves to the registered category. Never call unguarded: the class loads JEI API,
	 * so only the ModList-gated {@link gregtech6.gui.GTViewerJump} router may reach it.
	 *
	 * @return false (no-op) when the runtime is not available — the router then falls
	 *         through to the EMI leg.
	 */
	public static boolean openRecipeMapPage(RecipeMap aMap) {
		if (aMap == null || sRuntime == null) return false;
		sRuntime.getRecipesGui().showTypes(java.util.List.of(GT6RecipeMapJeiCategory.recipeTypeOf(aMap)));
		return true;
	}

	/**
	 * The gear-port jump face (task viewer-energy-jump-gear): opens the energy carrier's
	 * info page — the info recipes ride under the carrier as an INPUT focus ingredient
	 * (the internal IngredientInfoRecipeCategory mounts each recipe's ingredients under
	 * BOTH roles: addInputSlot + addInvisibleIngredients(OUTPUT), read off the harvested
	 * 15.x Library source :41-51), so {@code show(focus)} lands on the Information tab.
	 * The focus comes from {@code IJeiRuntime.getJeiHelpers().getFocusFactory()}
	 * (IJeiHelpers.java:42, both legs) — no stash beyond {@link #sRuntime} needed.
	 * Never call unguarded: only the JEI-side gear handler reaches it (JEI present by
	 * construction there).
	 *
	 * @return false (no-op) when the runtime is not available — offline and pre-init safe.
	 */
	public static boolean openEnergyCarrierInfo(TagData aCarrier) {
		if (aCarrier == null || sRuntime == null) return false;
		mezz.jei.api.recipe.IFocus<TagData> tFocus = sRuntime.getJeiHelpers().getFocusFactory()
				.createFocus(mezz.jei.api.recipe.RecipeIngredientRole.INPUT, GT6EnergyCarrierJei.TYPE, aCarrier);
		sRuntime.getRecipesGui().show(tFocus);
		return true;
	}

	/**
	 * The item jump face (task nav-s4-tree-screen): opens the recipes-for-item page — the R
	 * axis (how to obtain it), the screen's node-click destination on the JEI leg. The focus
	 * comes from the same {@code getFocusFactory()} seam {@link #openEnergyCarrierInfo}
	 * rides ({@code createFocus(role, type, ingredient)}, identical on both pinned
	 * generations), with {@code VanillaTypes.ITEM_STACK} as the type. Never call unguarded:
	 * the class loads JEI API — only the ModList-gated routing in
	 * {@link gregtech6.gui.GT6MaterialTreeScreen#openInViewer} may reach it.
	 *
	 * @return false (no-op) when the runtime is not available or the stack is empty.
	 */
	public static boolean openItemPage(ItemStack aStack) {
		if (aStack == null || aStack.isEmpty() || sRuntime == null) return false;
		mezz.jei.api.recipe.IFocus<ItemStack> tFocus = sRuntime.getJeiHelpers().getFocusFactory()
				.createFocus(mezz.jei.api.recipe.RecipeIngredientRole.OUTPUT,
						mezz.jei.api.constants.VanillaTypes.ITEM_STACK, aStack);
		sRuntime.getRecipesGui().show(tFocus);
		return true;
	}
}
