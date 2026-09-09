
package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

import gregapi.data.ANY;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregtech6.registry.GTMaterialItems;

/**
 * The RM.Bath static-row loader tests (task p26-kitchen-pot-bowl, the
 * GT6RecipesMixerTest seam shape). Three faces:
 * <ol>
 * <li>the MAP shape — the RM.java:80 row verbatim (items 6/6/1, fluids 1/3/1, MIN 2,
 *     "gt.recipe.bath" / "Bath"), the acceptance's "图形态断言（槽容按
 *     mInputFluidCount/mOutputFluidCount）" side (the BE tank arrays read these
 *     constants — the BE half is GT6KitchenBlockEntityTest);</li>
 * <li>the LIVE universe — the wood-oil ladder pours ZERO rows today (no plank item is
 *     an item-path prefix, the plant/fish oil family is port-absent) and the count is
 *     exactly {@code 9 × |ANY.WoodUntreated.mToThis|} (the "首批行 pour 对账");</li>
 * <li>the POUR FACE — with resolvers injected the SAME load() pours the whole ladder
 *     (duration 144 / EUt 0 / the template amounts), proving the face is live code, not
 *     a dead stub.</li>
 * </ol>
 */
class GT6RecipesBathTest extends GTRecipesOfflineTestBase {

	@BeforeAll
	static void bootTheMaterialUniverse() {
		GTMaterialItems.initMaterials(); // the offline material universe (the family-walk prerequisite)
	}

	@BeforeEach
	void armTheMap() {
		GT6RecipeMaps.init();
		GT6RecipeMaps.reset();
		GT6RecipeMaps.init();
		GT6RecipesBath.resetForTest();
	}

	@AfterEach
	void restoreTheLiveSeams() {
		// the live lambdas restored verbatim (the Canner restoreSeams convention)
		GT6RecipesBath.sPlankItemResolver = GT6RecipesMixer::resolveItem;
		GT6RecipesBath.sOilFluidResolver = GT6RecipesBath::resolveOil;
		GT6RecipeMaps.reset();
	}

	/** RM.java:80 verbatim: "gt.recipe.bath", "Bath", items 6/6/1, fluids 1/3/1, MIN 2, AMP 1. */
	@Test
	void bathMapMatchesTheUpstreamRow() {
		RecipeMap tMap = GT6RecipeMaps.BATH;
		assertNotNull(tMap, "the 13th map lives (the 59c34422 declaration)");
		assertEquals("gt.recipe.bath", tMap.mNameInternal, "RM.java:80 — the unlocalized name");
		assertEquals("Bath", tMap.mNameLocal, "RM.java:80 — the localized name");
		assertEquals(6, tMap.mInputItemsCount, "RM.java:80 — items in 6");
		assertEquals(6, tMap.mOutputItemsCount, "RM.java:80 — items out 6");
		assertEquals(1, tMap.mMinimalInputItems, "RM.java:80 — items min 1");
		assertEquals(1, tMap.mInputFluidCount, "RM.java:80 — fluids in 1");
		assertEquals(3, tMap.mOutputFluidCount, "RM.java:80 — fluids out 3");
		assertEquals(1, tMap.mMinimalInputFluids, "RM.java:80 — fluids min 1");
		assertEquals(2, tMap.mMinimalInputs, "RM.java:80 — MIN 2");
		assertNotNull(tMap.mRecipeList, "the row collection is live");
	}

	/**
	 * The live universe pours ZERO rows and the dormant count is exactly
	 * {@code 9 × |untreated woods|} — the "首批行 pour 对账" (every ladder row is
	 * accounted for: plank items and the plant/fish oil family are port-absent).
	 */
	@Test
	void liveUniversePoursZeroRowsWithFullReconciliation() {
		int tWoods = ANY.WoodUntreated.mToThis == null ? 0 : ANY.WoodUntreated.mToThis.size();
		assertTrue(tWoods > 0, "the untreated-wood family is live (the ladder has something to walk)");

		GT6RecipesBath.load();

		assertTrue(GT6RecipeMaps.BATH.mRecipeList.isEmpty(), "RM.Bath has ZERO static rows in the port universe (the dormant band is the declared pool)");
		assertEquals(0, GT6RecipesBath.lastPoured(), "zero rows poured live");
		assertEquals(GT6RecipesBath.WOOD_LADDER_TEMPLATES * tWoods, GT6RecipesBath.lastSkipped(),
				"every ladder row accounted for: 9 templates x " + tWoods + " untreated woods, all dormant on the missing legs");

		// eight of the nine templates are port-absent oils (the Loader_Fluids oil family);
		// creosote (gt6:creosote) is the ONE live leg — its RegistryObject only binds in a
		// mod-registered JVM, so its face is the live server's (the RCON chain drives it)
		for (String tOil : GT6RecipesBath.TREATED_OILS) {
			if (!"creosote".equals(tOil)) {
				assertEquals(null, GT6RecipesBath.resolveOil(new GT6RecipesBath.OilLeg(tOil, 100, false)), tOil + " is port-absent (the Loader_Fluids oil family)");
			}
		}
		assertFalse(GT6RecipesBath.resolveOil(new GT6RecipesBath.OilLeg("fish", 1000, true)) != null, "fish oil is port-absent");
	}

	/**
	 * With injected fixtures the SAME load() pours the WHOLE ladder — duration 144 / EUt 0
	 * / the per-template amounts — the pour face is live code (the moment the plank items
	 * and the oil family land, RM.Bath fills with no code change).
	 */
	@Test
	void thePourFaceIsLiveUnderInjectedFixtures() {
		GT6RecipesBath.sPlankItemResolver = (aPrefix, aMaterial) -> Items.BRICK; // every plank resolves
		GT6RecipesBath.sOilFluidResolver = aLeg -> Fluids.WATER; // every oil resolves

		GT6RecipesBath.resetForTest();
		GT6RecipesBath.load();

		int tWoods = ANY.WoodUntreated.mToThis.size();
		assertEquals(GT6RecipesBath.WOOD_LADDER_TEMPLATES * tWoods, GT6RecipeMaps.BATH.mRecipeList.size(),
				"the whole ladder pours: 9 templates x " + tWoods + " untreated woods");
		assertEquals(GT6RecipesBath.WOOD_LADDER_TEMPLATES * tWoods, GT6RecipesBath.lastPoured(), "all poured, none skipped");
		assertEquals(0, GT6RecipesBath.lastSkipped(), "no dormant rows under fixtures");

		// every poured row carries the shared columns (the :661-669 shape)
		for (Recipe tRow : GT6RecipeMaps.BATH.mRecipeList) {
			assertEquals(GT6RecipesBath.WOOD_LADDER_DURATION, tRow.mDuration, ":661-669 — the shared 144 duration");
			assertEquals(GT6RecipesBath.WOOD_LADDER_EUT, tRow.mEUt, "the manual map runs at EUt 0");
			assertEquals(1, tRow.mFluidInputs.length, "one oil input");
			assertTrue(tRow.mFluidInputs[0].getAmount() == GT6RecipesBath.OIL_TREATED_INPUT
					|| tRow.mFluidInputs[0].getAmount() == GT6RecipesBath.OIL_FISH_INPUT
					|| tRow.mFluidInputs[0].getAmount() == GT6RecipesBath.OIL_WHALE_INPUT,
					"the oil amount is one of the template literals (100 treated / 1000 fish / 500 whale)");
			assertEquals(1, tRow.mInputs.length, "one plank in, one treated plank out");
			assertTrue(tRow.mCanBeBuffered, "addRecipe1(T, …) — buffered");
		}
	}

	/**
	 * The findRecipe consumer half: a fixture row in the map is FOUND through the same
	 * arguments the kitchen BE passes (RECIPE_SIZE 32, slots+tanks) — the pot's map
	 * consumption contract (the BE activation chain itself is the RCON chain's job).
	 */
	@Test
	void bathConsumesRowsThroughTheKitchenArgumentShape() {
		Recipe tRow = new Recipe(true,
				new ItemStack[] {new ItemStack(Items.BRICK, 1)},
				new ItemStack[] {new ItemStack(Items.BRICK, 1)},
				new FluidStack[] {new FluidStack(Fluids.WATER, 100)},
				new FluidStack[0],
				GT6RecipesBath.WOOD_LADDER_DURATION, GT6RecipesBath.WOOD_LADDER_EUT, 0);
		GT6RecipeMaps.BATH.mRecipeList.add(tRow);

		ItemStack[] tSlots = new ItemStack[6];
		tSlots[0] = new ItemStack(Items.BRICK, 4);
		FluidStack[] tTanks = new FluidStack[] {new FluidStack(Fluids.WATER, 1000)};
		Recipe tFound = GT6RecipeMaps.BATH.findRecipe(null, 32, ItemStack.EMPTY, tTanks, tSlots);
		assertNotNull(tFound, "the kitchen BE argument shape (V[1]=32, snapshot+tank arrays) finds the row");
		assertTrue(tFound.isRecipeInputEqual(true, false, tTanks, tSlots), "the row pays (the manual round's isRecipeInputEqual(true, false) form)");
		assertEquals(3, tSlots[0].getCount(), "the brick input is consumed down to 3");
	}
}
