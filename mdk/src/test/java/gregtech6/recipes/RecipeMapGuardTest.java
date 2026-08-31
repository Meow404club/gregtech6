package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fluids.FluidStack;
import net.minecraft.world.level.material.Fluids;

/**
 * The RecipeMap.addRecipe double-empty guard (task p8-recipe-chances-orechain ②): a recipe
 * with neither an item leg nor a fluid leg is rejected — not stored, no exception, null
 * returned. Upstream is immune at the index layer (addToItemMap, Recipe.java:632-640);
 * this port's linear scan + vacuously-true checkStacksEqual (upstream :775) would make an
 * empty-input row match EVERY lookup (the p7 id102 ghost recipe), so the invariant lives
 * at the registration layer.
 */
class RecipeMapGuardTest {

	private RecipeMap mMap;

	@BeforeEach
	void buildMap() {
		mMap = new RecipeMap(null, "gt.recipe.guardtest", "Guard Test", null, 0, 1, "gt6:textures/gui/machines/crusher", 1, 1, 1, 0, 0, 0, 0, 1);
	}

	@AfterEach
	void dropMap() {
		RecipeMap.reset();
	}

	@Test
	void doubleEmptyRecipeIsRejected() {
		Recipe tGhost = new Recipe(true, null, new ItemStack[] {new ItemStack(Items.GLASS)}, null, null, 16, 16, 0);
		assertEquals(0, tGhost.mInputs.length, "precondition: the ghost row has no item leg");
		assertNull(mMap.addRecipe(tGhost), "the guard returns null");
		assertEquals(0, mMap.mRecipeList.size(), "the ghost row is not stored");
		assertFalse(mMap.mRecipeList.contains(tGhost));
	}

	@Test
	void allAirInputsTrimToTheRejectedShape() {
		// the p7 ghost shape: AIR stacks trim away inside the ctor → empty inputs
		Recipe tAirGhost = new Recipe(true, new ItemStack[] {new ItemStack(Items.AIR, 4)}, new ItemStack[] {new ItemStack(Items.GLASS)}, null, null, 16, 16, 0);
		assertEquals(0, tAirGhost.mInputs.length, "precondition: AIR inputs trim to an empty leg");
		assertNull(mMap.addRecipe(tAirGhost));
		assertEquals(0, mMap.mRecipeList.size());
	}

	@Test
	void emptyInputsCanBeStillFluidOnlyLegs() {
		Recipe tFluidOnly = new Recipe(true, null, new ItemStack[] {new ItemStack(Items.GLASS)}, new FluidStack[] {new FluidStack(Fluids.LAVA, 100)}, null, 16, 16, 0);
		assertEquals(0, tFluidOnly.mInputs.length);
		assertSame(tFluidOnly, mMap.addRecipe(tFluidOnly), "a fluid leg alone is a valid recipe");
		assertEquals(1, mMap.mRecipeList.size());
	}

	@Test
	void itemLegRecipesPassUnchanged() {
		Recipe tNormal = new Recipe(true, new ItemStack[] {new ItemStack(Items.SAND)}, new ItemStack[] {new ItemStack(Items.GLASS)}, null, null, 16, 16, 0);
		assertSame(tNormal, mMap.addRecipe(tNormal));
		assertEquals(1, mMap.mRecipeList.size());
		assertNotNull(mMap.findRecipe(null, 16, ItemStack.EMPTY, null, new ItemStack(Items.SAND, 1)), "the stored row stays findable");
	}

	@Test
	void nullArgumentStaysNull() {
		assertNull(mMap.addRecipe(null));
		assertEquals(0, mMap.mRecipeList.size());
	}

	@Test
	void noExceptionIsEverThrownForDegenerateShapes() {
		assertDoesNotThrow(() -> mMap.addRecipe(new Recipe(true, new ItemStack[0], new ItemStack[0], new FluidStack[0], new FluidStack[0], 16, 16, 0)));
		assertDoesNotThrow(() -> mMap.addRecipe(new Recipe(false, null, null, null, null, 16, 16, 0)));
		assertEquals(0, mMap.mRecipeList.size());
	}

	@Test
	void fakeAndDisabledRecipesKeepTheirExistingHandling() {
		Recipe tFake = new Recipe(true, new ItemStack[] {new ItemStack(Items.SAND)}, new ItemStack[] {new ItemStack(Items.GLASS)}, null, null, 16, 16, 0);
		tFake.mFakeRecipe = true;
		assertSame(tFake, mMap.addRecipe(tFake), "the guard must not change the fake/disabled return contract");
		assertEquals(0, mMap.mRecipeList.size(), "fakes still do not enter the list");
	}

	@Test
	void theGuardIsTheGhostProofForLookups() {
		// behavioural capstone: pre-guard, a stored empty-input row matched every lookup;
		// post-guard the row never enters, so an unrelated lookup finds nothing.
		mMap.addRecipe(new Recipe(true, new ItemStack[] {new ItemStack(Items.AIR, 1)}, new ItemStack[] {new ItemStack(Items.DIAMOND)}, null, null, 16, 16, 0));
		assertNull(mMap.findRecipe(null, 16, ItemStack.EMPTY, null, new ItemStack(Items.SAND, 1)),
				"an AIR-input ghost row must neither be stored nor match a lookup");
	}
}
