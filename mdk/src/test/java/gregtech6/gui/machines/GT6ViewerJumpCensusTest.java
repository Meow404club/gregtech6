/**
 * Offline guard tests for task debt-jei-emi-batch4-transfer: the machine-GUI progress-bar
 * jump face. Pins the click-rect geometry to the {@code ARROW_*} draw cell, the route
 * decision table (JEI first, EMI fallback, neither → no-op), the shared per-map
 * {@code RecipeType} uid formula and the upstream "Recipes" hover literal — the faces the
 * runtime click rides but which are all assertable without a viewer (the EMI runtime
 * cannot run offline, the tier-b GT6EmiPluginTest boundary; the JEI recipes gui likewise).
 */
package gregtech6.gui.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import mezz.jei.api.recipe.RecipeType;

import gregtech6.gui.GTViewerJump;
import gregtech6.jei.GT6RecipeMapJeiCategory;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;

public class GT6ViewerJumpCensusTest {

	/** The minimal vanilla offline bootstrap (the GT6RecipeMapEmiCategoryTest form). */
	@BeforeAll
	static void bootVanillaOffline() {
		gregtech6.registry.GTMaterialItems.initMaterials();
		net.minecraft.SharedConstants.tryDetectVersion();
		try {
			net.minecraft.server.Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
	}

	@AfterEach
	void cleanUp() {
		GT6RecipeMaps.reset();
	}

	/**
	 * The rect IS the arrow draw cell (78,24)-(97,41): corners in, the outer ring out —
	 * upstream covered its arrow with (65,13,36,18); the rect followed the arrow to the
	 * modern 176x166 panel.
	 */
	@Test
	public void jumpRectIsExactlyTheArrowCell() {
		assertTrue(GTBasicMachineScreen.arrowContains(78, 24), "top-left arrow pixel");
		assertTrue(GTBasicMachineScreen.arrowContains(97, 41), "bottom-right arrow pixel");
		assertTrue(GTBasicMachineScreen.arrowContains(87.5, 32.5), "the arrow centre");
		assertFalse(GTBasicMachineScreen.arrowContains(77.9, 24), "one pixel left is out");
		assertFalse(GTBasicMachineScreen.arrowContains(78, 42), "one pixel below is out");
		assertFalse(GTBasicMachineScreen.arrowContains(98, 24), "one pixel right is out");
	}

	/** The route table: no viewer, no map or a JEI runtime that is not ready → false (vanilla click continues). */
	@Test
	public void routeTableFallsBackToNothingWithoutReadyViewers() {
		assertFalse(GTViewerJump.openRecipeMapPage(null, true, true), "no map — nothing to open");
		assertFalse(GTViewerJump.openRecipeMapPage(anyMap(), false, false), "no viewer installed — no-op");
		// jei "loaded" offline but its plugin never saw onRuntimeAvailable → sRuntime null →
		// the JEI arm refuses and the (absent) EMI arm is not reached
		assertFalse(GTViewerJump.openRecipeMapPage(anyMap(), true, false), "JEI without a ready runtime refuses");
	}

	/** The shared uid formula: the click rebuilds exactly the uid the registration used. */
	@Test
	public void recipeTypeOfPinsTheCategoryUidFormula() {
		GT6RecipeMaps.init();
		RecipeType<Recipe> tType = GT6RecipeMapJeiCategory.recipeTypeOf(GT6RecipeMaps.COKE_OVEN);
		assertEquals("gt6:recipe_map/gt.recipe.cokeoven", tType.getUid().toString(),
				"the jump face and the category registration share ONE uid formula");
		assertEquals(Recipe.class, tType.getRecipeClass());
		// RecipeType.equals = uid + recipeClass (RecipeType.java, both pinned generations) —
		// so a rebuilt type resolves to the registered category without stashing instances
		assertEquals(tType, new RecipeType<>(tType.getUid(), Recipe.class), "the equals contract the rebuild relies on");
	}

	/** The hover label is the upstream literal (NEI_RecipeMap.java:420). */
	@Test
	public void rectTooltipIsTheUpstreamLiteral() {
		assertEquals("Recipes", GTViewerJump.RECT_TOOLTIP_TEXT);
	}

	private static RecipeMap anyMap() {
		GT6RecipeMaps.init();
		return GT6RecipeMaps.COKE_OVEN;
	}
}
