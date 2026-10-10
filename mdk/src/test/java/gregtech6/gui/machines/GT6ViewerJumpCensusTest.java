/**
 * Offline guard tests for task debt-jei-emi-batch4-transfer: the machine-GUI progress-bar
 * jump face. Pins the click-rect geometry to the {@code ARROW_*} draw cell, the route
 * decision table (EMI first, JEI fallback, neither → no-op — the r11-nei-corner-jump
 * priority flip, known_bugs.r11-batch2-render.viewer_priority_emi), the shared per-map
 * {@code RecipeType} uid formula and the upstream "Recipes" hover literal — the faces the
 * runtime click rides but which are all assertable without a viewer (the EMI runtime
 * cannot run offline, the tier-b GT6EmiPluginTest boundary; the JEI recipes gui likewise).
 */
package gregtech6.gui.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
	 * upstream covered its arrow with (65,13,36,18). Re-based on the MUI panel (task
	 * basic-machines-mui-migration — the vanilla screen retired): the jump click rides the
	 * ProgressWidget's bounds through the fork isMouseAbove gate, so the rect is exactly
	 * the widget's (x, y, w, h) = the family seat (78, 24, 20x18).
	 */
	@Test
	public void jumpRectIsExactlyTheArrowCell() throws Exception {
		brachy.modularui.screen.ModularPanel<?> tPanel = GTBasicMachineMUI.buildPanel(
				new MinimalHost(),
				new brachy.modularui.value.sync.PanelSyncManager(new brachy.modularui.value.sync.ModularSyncManager(false), true));
		brachy.modularui.widgets.ProgressWidget tArrow = (brachy.modularui.widgets.ProgressWidget) allWidgets(tPanel)
				.stream().filter(w -> w instanceof brachy.modularui.widgets.ProgressWidget).findFirst().orElseThrow();
		int tX = posOf(tArrow, true), tY = posOf(tArrow, false);
		int tW = widgetSize(tArrow, true), tH = widgetSize(tArrow, false);

		assertTrue(contains(tX, tY, tW, tH, 78, 24), "top-left arrow pixel");
		assertTrue(contains(tX, tY, tW, tH, 97, 41), "bottom-right arrow pixel");
		assertTrue(contains(tX, tY, tW, tH, 87.5, 32.5), "the arrow centre");
		assertFalse(contains(tX, tY, tW, tH, 77.9, 24), "one pixel left is out");
		assertFalse(contains(tX, tY, tW, tH, 78, 42), "one pixel below is out");
		assertFalse(contains(tX, tY, tW, tH, 98, 24), "one pixel right is out");
	}

	/** The retired vanilla screen's rect predicate, over the pinned panel geometry. */
	private static boolean contains(int aX, int aY, int aW, int aH, double aRelX, double aRelY) {
		return aRelX >= aX && aRelX < aX + aW && aRelY >= aY && aRelY < aY + aH;
	}

	/** A bare Host fake — the panel builds on the interface defaults (no map, no banks). */
	private static final class MinimalHost implements GTBasicMachineMenu.Host {
		private final gregtech6.tileentity.GTItemStackHandler mInventory = new gregtech6.tileentity.GTItemStackHandler(16);
		@Override public gregtech6.tileentity.GTItemStackHandler getInventory() { return mInventory; }
		@Override public int getOutputSlotCount() { return 1; }
		@Override public boolean isSuccessful() { return false; }
		@Override public long getProgress() { return 0; }
		@Override public long getMaxProgress() { return 0; }
	}

	private static int widgetSize(brachy.modularui.api.widget.IWidget aWidget, boolean aWidth) throws Exception {
		// the size face mirrors posOf: the resizer's axis DimensionSizer holds the Unit set
		// by .size(...) in its "size" field (the .pos(...) Units ride "start")
		brachy.modularui.api.widget.IPositioned<?> tPositioned = (brachy.modularui.api.widget.IPositioned<?>) aWidget;
		java.lang.reflect.Field tAxis = tPositioned.resizer().getClass().getDeclaredField(aWidth ? "x" : "y");
		tAxis.setAccessible(true);
		Object tSizer = tAxis.get(tPositioned.resizer());
		java.lang.reflect.Field tSize = tSizer.getClass().getDeclaredField("size");
		tSize.setAccessible(true);
		Object tUnit = tSize.get(tSizer);
		return (int) ((brachy.modularui.widget.sizer.Unit) tUnit).getValue();
	}

	private static java.util.List<brachy.modularui.api.widget.IWidget> allWidgets(brachy.modularui.screen.ModularPanel<?> aPanel) {
		java.util.List<brachy.modularui.api.widget.IWidget> tAll = new java.util.ArrayList<>();
		collectAll(aPanel, tAll);
		return tAll;
	}

	private static void collectAll(brachy.modularui.api.widget.IWidget aWidget, java.util.List<brachy.modularui.api.widget.IWidget> aSink) {
		aSink.add(aWidget);
		if (aWidget instanceof brachy.modularui.widget.ParentWidget<?> tParent) {
			for (brachy.modularui.api.widget.IWidget tChild : tParent.getChildren()) {
				collectAll(tChild, aSink);
			}
		}
	}

	private static int posOf(brachy.modularui.api.widget.IWidget aWidget, boolean aX) throws Exception {
		brachy.modularui.api.widget.IPositioned<?> tPositioned = (brachy.modularui.api.widget.IPositioned<?>) aWidget;
		java.lang.reflect.Field tAxis = tPositioned.resizer().getClass().getDeclaredField(aX ? "x" : "y");
		tAxis.setAccessible(true);
		Object tSizer = tAxis.get(tPositioned.resizer());
		java.lang.reflect.Field tStart = tSizer.getClass().getDeclaredField("start");
		tStart.setAccessible(true);
		Object tUnit = tStart.get(tSizer);
		return (int) ((brachy.modularui.widget.sizer.Unit) tUnit).getValue();
	}

	/** The route table: no viewer, no map or a JEI runtime that is not ready → false (vanilla click continues). */
	@Test
	public void routeTableFallsBackToNothingWithoutReadyViewers() {
		assertFalse(GTViewerJump.openRecipeMapPage(null, true, true), "no map — nothing to open");
		assertFalse(GTViewerJump.openRecipeMapPage(anyMap(), false, false), "no viewer installed — no-op");
		// (jei only): the JEI arm refuses offline — its plugin never saw onRuntimeAvailable → sRuntime null
		assertFalse(GTViewerJump.openRecipeMapPage(anyMap(), true, false), "JEI without a ready runtime refuses");
	}

	/**
	 * The priority flip (r11-nei-corner-jump): EMI first in BOTH the jump route and the
	 * shared {@code preferredViewer} predicate the kitchen NEI glyph gate consumes — what
	 * you see is what you jump to.
	 */
	@Test
	public void preferredViewerIsEmiFirst() {
		assertEquals("emi", GTViewerJump.preferredViewer(true, true), "dual install → EMI (the ruling)");
		assertEquals("emi", GTViewerJump.preferredViewer(true, false), "EMI only → EMI");
		assertEquals("jei", GTViewerJump.preferredViewer(false, true), "JEI only → JEI");
		assertNull(GTViewerJump.preferredViewer(false, false), "neither → null (no glyph, no jump)");
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
