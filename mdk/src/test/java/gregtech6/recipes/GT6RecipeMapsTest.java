package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
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

	/** The RM.java:78 Coke Oven map constants (task p6-cokeoven-processing). */
	@Test
	void initRegistersCokeOvenMapWithUpstreamConstants() {
		GT6RecipeMaps.init();
		assertNotNull(GT6RecipeMaps.COKE_OVEN);
		assertSame(GT6RecipeMaps.COKE_OVEN, RecipeMap.RECIPE_MAPS.get("gt.recipe.cokeoven"));
		assertEquals("Coke Oven", GT6RecipeMaps.COKE_OVEN.mNameLocal);
		assertEquals("gt.recipe.cokeoven", GT6RecipeMaps.COKE_OVEN.mNameNEI, "RM.java:78 passes null → the internal name");
		assertEquals(1, GT6RecipeMaps.COKE_OVEN.mInputItemsCount);
		assertEquals(9, GT6RecipeMaps.COKE_OVEN.mOutputItemsCount);
		assertEquals(1, GT6RecipeMaps.COKE_OVEN.mMinimalInputItems);
		assertEquals(0, GT6RecipeMaps.COKE_OVEN.mInputFluidCount);
		assertEquals(1, GT6RecipeMaps.COKE_OVEN.mOutputFluidCount);
		assertEquals(0, GT6RecipeMaps.COKE_OVEN.mMinimalInputFluids);
		assertEquals(1, GT6RecipeMaps.COKE_OVEN.mMinimalInputs);
		assertEquals(1, GT6RecipeMaps.COKE_OVEN.mPower);
		assertEquals(0, GT6RecipeMaps.COKE_OVEN.mProgressBarDirection);
		assertEquals(1, GT6RecipeMaps.COKE_OVEN.mProgressBarAmount);
		assertEquals("gt6:textures/gui/machines/CokeOven.png", GT6RecipeMaps.COKE_OVEN.mGUIPath);
	}

	@Test
	void reinitIsIdempotentWithinAGeneration() {
		GT6RecipeMaps.init();
		RecipeMapFurnace tFirst = GT6RecipeMaps.FURNACE;
		RecipeMap tFirstCoke = GT6RecipeMaps.COKE_OVEN;
		RecipeMap tFirstShredder = GT6RecipeMaps.SHREDDER;
		RecipeMap tFirstCrusher = GT6RecipeMaps.CRUSHER;
		RecipeMap tFirstLathe = GT6RecipeMaps.LATHE;
		GT6RecipeMaps.init();
		assertSame(tFirst, GT6RecipeMaps.FURNACE, "init within one generation must not recreate (upstream :139 would throw on the duplicate name)");
		assertSame(tFirstCoke, GT6RecipeMaps.COKE_OVEN);
		assertSame(tFirstShredder, GT6RecipeMaps.SHREDDER);
		assertSame(tFirstCrusher, GT6RecipeMaps.CRUSHER);
		assertSame(tFirstLathe, GT6RecipeMaps.LATHE);
		assertEquals(5, RecipeMap.RECIPE_MAPS.size(), "furnace + coke oven + shredder + crusher + lathe");
	}

	@Test
	void resetThenReinitProducesFreshRegistration() {
		GT6RecipeMaps.init();
		RecipeMapFurnace tFirst = GT6RecipeMaps.FURNACE;
		RecipeMap tFirstCoke = GT6RecipeMaps.COKE_OVEN;

		GT6RecipeMaps.reset();
		assertNull(GT6RecipeMaps.FURNACE);
		assertNull(GT6RecipeMaps.COKE_OVEN);
		assertFalse(RecipeMap.RECIPE_MAPS.containsKey("mc.recipe.furnace"), "reset drops the registry entry");
		assertFalse(RecipeMap.RECIPE_MAPS.containsKey("gt.recipe.cokeoven"), "reset drops the coke oven entry");

		GT6RecipeMaps.init();
		assertNotNull(GT6RecipeMaps.FURNACE);
		assertNotNull(GT6RecipeMaps.COKE_OVEN);
		assertNotSame(tFirst, GT6RecipeMaps.FURNACE, "re-init after reset creates a fresh generation");
		assertNotSame(tFirstCoke, GT6RecipeMaps.COKE_OVEN);
		assertSame(GT6RecipeMaps.FURNACE, RecipeMap.RECIPE_MAPS.get("mc.recipe.furnace"));
		assertSame(GT6RecipeMaps.COKE_OVEN, RecipeMap.RECIPE_MAPS.get("gt.recipe.cokeoven"));
	}

	/** The duplicate-name guard fires when a second generation is created without a reset (upstream Recipe.java:139). */
	@Test
	void duplicateNameThrowsWithoutReset() {
		GT6RecipeMaps.init();
		assertThrows(IllegalArgumentException.class, () -> new RecipeMap(new HashSet<>(),
				"gt.recipe.cokeoven", "Coke Oven", null,
				0, 1, "gt6:textures/gui/machines/CokeOven",
				1, 9, 1, 0, 1, 0, 1, 1));
	}
}
