package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * RecipeMap shell semantics: RECIPE_MAPS registration + reset (P1 registry
 * discipline), slot constants (Recipe.java:129-135), linear findRecipe with
 * the mLastRecipe fast path (upstream :487/:498-501 trim) and lookup-only
 * behavior.
 */
class RecipeMapTest extends GTRecipesOfflineTestBase {

	@AfterEach
	void cleanRegistry() {
		RecipeMap.reset();
	}

	private static RecipeMap newTestMap() {
		return new RecipeMap(new HashSet<>(), "gt.recipe.test", "Test Map", null, 0, 1, "gt6:textures/gui/test", 1, 1, 1, 0, 0, 0, 0, 1);
	}

	@Test
	void slotConstantsApplyMinimalFloors() {
		// upstream :129-135: count = max(count, minimal).
		RecipeMap tMap = new RecipeMap(new HashSet<>(), "gt.recipe.slots", "Slots", null, 2, 3, "gt6:textures/gui/slots",
				/*inItems=*/ 1, /*outItems=*/ 2, /*minInItems=*/ 3,
				/*inFluids=*/ 1, /*outFluids=*/ 1, /*minInFluids=*/ 2,
				/*minInputs=*/ 5, /*power=*/ 1);
		assertEquals(3, tMap.mInputItemsCount, "input item count floors at the minimal");
		assertEquals(2, tMap.mOutputItemsCount);
		assertEquals(3, tMap.mMinimalInputItems);
		assertEquals(2, tMap.mInputFluidCount, "input fluid count floors at the minimal");
		assertEquals(1, tMap.mOutputFluidCount);
		assertEquals(2, tMap.mMinimalInputFluids);
		assertEquals(5, tMap.mMinimalInputs);
		assertEquals(2, tMap.mProgressBarDirection);
		assertEquals(3, tMap.mProgressBarAmount);
		assertEquals("Slots", tMap.mNameLocalUnderscored);
		assertEquals("gt6:textures/gui/slots.png", tMap.mGUIPath, "gui path auto-attaches .png");
		assertEquals("gt.recipe.slots", tMap.mNameNEI, "null NEI name falls back to the internal name");
	}

	@Test
	void registrationAndDuplicateGuard() {
		RecipeMap tMap = newTestMap();
		assertSame(tMap, RecipeMap.RECIPE_MAPS.get("gt.recipe.test"));
		assertThrows(IllegalArgumentException.class, () -> newTestMap(), "upstream :139 duplicate-name guard");
	}

	@Test
	void resetClearsRegistryForReinit() {
		newTestMap();
		assertEquals(1, RecipeMap.RECIPE_MAPS.size());
		RecipeMap.reset();
		assertTrue(RecipeMap.RECIPE_MAPS.isEmpty(), "reset drops the whole generation");
		RecipeMap tAgain = newTestMap();
		assertSame(tAgain, RecipeMap.RECIPE_MAPS.get("gt.recipe.test"), "re-registration after reset is idempotent");
	}

	@Test
	void findRecipeScansLinearlyAndGatesOnPower() {
		// LinkedHashSet keeps insertion order so the first-match semantics are testable
		// (upstream mRecipeList is a HashSet-backed Collection with unspecified order).
		RecipeMap tMap = new RecipeMap(new java.util.LinkedHashSet<>(), "gt.recipe.test", "Test Map", null, 0, 1, "gt6:textures/gui/test", 1, 1, 1, 0, 0, 0, 0, 1);
		Recipe tCheap = new Recipe(true, new ItemStack[] {new ItemStack(Items.SAND)}, new ItemStack[] {new ItemStack(Items.GLASS)}, null, null, 16, 8, 0);
		Recipe tExpensive = new Recipe(true, new ItemStack[] {new ItemStack(Items.SAND)}, new ItemStack[] {new ItemStack(Items.SANDSTONE)}, null, null, 16, 256, 0);
		tMap.addRecipe(tCheap);
		tMap.addRecipe(tExpensive);

		// aSize=64, mPower=1: 64 covers mEUt=8 but not 256. The scan returns the first
		// matching recipe in list order; the gate (upstream :501) only rejects when hit.
		assertSame(tCheap, tMap.findRecipe(null, 64, null, null, new ItemStack(Items.SAND, 4)));

		RecipeMap tOnlyExpensive = new RecipeMap(new java.util.LinkedHashSet<>(), "gt.recipe.test.expensive", "Test Map", null, 0, 1, "gt6:textures/gui/test", 1, 1, 1, 0, 0, 0, 0, 1);
		tOnlyExpensive.addRecipe(tExpensive);
		assertNotNull(tOnlyExpensive.findRecipe(null, 512, null, null, new ItemStack(Items.SAND, 4)), "512 >= 256: covered");
	}

	@Test
	void findRecipeReturnsNullWhenOnlyOverpoweredRecipesMatch() {
		RecipeMap tMap = newTestMap();
		tMap.addRecipe(new Recipe(true, new ItemStack[] {new ItemStack(Items.SAND)}, new ItemStack[] {new ItemStack(Items.GLASS)}, null, null, 16, 256, 0));
		assertNull(tMap.findRecipe(null, 64, null, null, new ItemStack(Items.SAND, 4)), "64 < 256: recipe matches but the voltage gate rejects it");
	}

	@Test
	void findRecipeSkipsFakeAndDisabledRecipes() {
		RecipeMap tMap = newTestMap();
		Recipe tFake = new Recipe(true, new ItemStack[] {new ItemStack(Items.SAND)}, new ItemStack[] {new ItemStack(Items.GLASS)}, null, null, 16, 8, 0);
		tFake.mFakeRecipe = true;
		tMap.addRecipe(tFake); // addRecipe refuses fakes
		assertNull(tMap.findRecipe(null, Long.MAX_VALUE, null, null, new ItemStack(Items.SAND, 1)));

		Recipe tDisabled = new Recipe(true, new ItemStack[] {new ItemStack(Items.SAND)}, new ItemStack[] {new ItemStack(Items.GLASS)}, null, null, 16, 8, 0);
		tDisabled.mEnabled = false;
		tMap.addRecipe(tDisabled);
		assertNull(tMap.findRecipe(null, Long.MAX_VALUE, null, null, new ItemStack(Items.SAND, 1)), "disabled recipes are found but gated to null (upstream :501)");
	}

	@Test
	void findRecipeIsLookupOnly() {
		RecipeMap tMap = newTestMap();
		tMap.addRecipe(new Recipe(true, new ItemStack[] {new ItemStack(Items.SAND, 1)}, new ItemStack[] {new ItemStack(Items.GLASS)}, null, null, 16, 8, 0));
		ItemStack tSand = new ItemStack(Items.SAND, 8);

		assertNotNull(tMap.findRecipe(null, 64, null, null, tSand));
		assertEquals(8, tSand.getCount(), "findRecipe must never consume");
	}

	@Test
	void lastRecipeFastPathProbesWithoutSizeChecks() {
		RecipeMap tMap = newTestMap();
		Recipe tRecipe = new Recipe(true, new ItemStack[] {new ItemStack(Items.SAND, 8)}, new ItemStack[] {new ItemStack(Items.GLASS)}, null, null, 16, 8, 0);
		tMap.addRecipe(tRecipe);
		ItemStack tSingle = new ItemStack(Items.SAND, 1);

		// upstream :487 probes with aDontCheckStackSizes=T: the cached recipe matches
		// even with a too-small stack, so the machine gets its own recipe back.
		assertSame(tRecipe, tMap.findRecipe(tRecipe, Long.MAX_VALUE, null, null, tSingle));
		assertEquals(1, tSingle.getCount(), "fast path is also lookup-only");
	}
}
