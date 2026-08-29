package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * P1 registry discipline for the RM.java:103 counterpart: init is idempotent
 * within a generation, reset drops the generation, re-registration after reset
 * produces a fresh map under the same name.
 */
class GT6RecipeMapsTest extends GTRecipesOfflineTestBase {

	@AfterEach
	void cleanUp() {
		GT6RecipeMaps.reset();
	}

	@Test
	void initRegistersFurnaceMapWithUpstreamConstants() {
		GT6RecipeMaps.init();
		assertNotNull(GT6RecipeMaps.FURNACE);
		assertSame(GT6RecipeMaps.FURNACE, RecipeMap.RECIPE_MAPS.get("mc.recipe.furnace"));
		assertEquals("Furnace", GT6RecipeMaps.FURNACE.mNameLocal);
		assertEquals("smelting", GT6RecipeMaps.FURNACE.mNameNEI, "RM.java:103 passes the vanilla type name as NEI name");
		assertEquals(1, GT6RecipeMaps.FURNACE.mInputItemsCount);
		assertEquals(1, GT6RecipeMaps.FURNACE.mOutputItemsCount);
		assertEquals(1, GT6RecipeMaps.FURNACE.mMinimalInputItems);
		assertEquals(1, GT6RecipeMaps.FURNACE.mInputFluidCount);
		assertEquals(1, GT6RecipeMaps.FURNACE.mOutputFluidCount);
		assertEquals(0, GT6RecipeMaps.FURNACE.mMinimalInputFluids);
		assertEquals(0, GT6RecipeMaps.FURNACE.mMinimalInputs);
		assertEquals(1, GT6RecipeMaps.FURNACE.mPower);
		assertEquals(0, GT6RecipeMaps.FURNACE.mProgressBarDirection);
		assertEquals(1, GT6RecipeMaps.FURNACE.mProgressBarAmount);
		assertTrue(GT6RecipeMaps.FURNACE.mGUIPath.endsWith(".png"));
		assertTrue(GT6RecipeMaps.FURNACE instanceof RecipeMapFurnace);
	}

	@Test
	void reinitIsIdempotentWithinAGeneration() {
		GT6RecipeMaps.init();
		RecipeMapFurnace tFirst = GT6RecipeMaps.FURNACE;
		GT6RecipeMaps.init();
		assertSame(tFirst, GT6RecipeMaps.FURNACE, "init within one generation must not recreate (upstream :139 would throw on the duplicate name)");
		assertEquals(1, RecipeMap.RECIPE_MAPS.size());
	}

	@Test
	void resetThenReinitProducesFreshRegistration() {
		GT6RecipeMaps.init();
		RecipeMapFurnace tFirst = GT6RecipeMaps.FURNACE;

		GT6RecipeMaps.reset();
		assertNull(GT6RecipeMaps.FURNACE);
		assertFalse(RecipeMap.RECIPE_MAPS.containsKey("mc.recipe.furnace"), "reset drops the registry entry");

		GT6RecipeMaps.init();
		assertNotNull(GT6RecipeMaps.FURNACE);
		assertNotSame(tFirst, GT6RecipeMaps.FURNACE, "re-init after reset creates a fresh generation");
		assertSame(GT6RecipeMaps.FURNACE, RecipeMap.RECIPE_MAPS.get("mc.recipe.furnace"));
	}
}
