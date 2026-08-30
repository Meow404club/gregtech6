package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

/**
 * The tag-driven log expansion (the 2026-08-30 coordinator amendment, acceptance ①): the
 * pure expansion function fed synthetic tag content offline, plus the subset-replace
 * idempotence of the listener bookkeeping (identity-based, no stacking on repeated tag
 * updates). The live tag wiring (TagsUpdatedEvent → rebuild) is RCON-verified — the oak_log
 * acceptance step walks the real #minecraft:logs path.
 */
class GT6CokeOvenLogExpansionTest extends GTRecipesOfflineTestBase {

	/** The gem.Charcoal stand-in — a vanilla item (new Items cannot be created offline, intrusive registry holders). */
	private static final Item CHARCOAL_GEM = Items.CHARCOAL;

	@AfterEach
	void cleanMaps() {
		GT6RecipeMaps.reset();
	}

	/** One recipe per log item, the Woods:176-180 shape at the WoodEntry 2-arg rates (1 charcoal / 250 mB). */
	@Test
	void expandProducesOneRecipePerLog() {
		List<Recipe> tRecipes = GT6CokeOvenLogExpansion.expand(
				List.of(Items.OAK_LOG, Items.SPRUCE_LOG, Items.BIRCH_LOG, Items.JUNGLE_LOG, Items.ACACIA_LOG, Items.DARK_OAK_LOG),
				CHARCOAL_GEM, Fluids.WATER);
		assertEquals(6, tRecipes.size(), "one recipe per vanilla log (the upstream per-wood registration)");

		for (Recipe tRecipe : tRecipes) {
			assertEquals(1, tRecipe.mInputs.length);
			assertEquals(1, tRecipe.mInputs[0].getCount());
			assertEquals(1, tRecipe.mOutputs.length);
			assertEquals(CHARCOAL_GEM, tRecipe.mOutputs[0].getItem());
			assertEquals(1, tRecipe.mOutputs[0].getCount());
			assertEquals(1, tRecipe.mFluidOutputs.length);
			assertEquals(GT6CokeOvenLogExpansion.CREOSOTE_MB_PER_LOG, tRecipe.mFluidOutputs[0].getAmount());
			assertEquals(GT6CokeOvenLogExpansion.DURATION, tRecipe.mDuration);
			assertEquals(0, tRecipe.mEUt);
		}
		// per-log distinctness: the oak recipe matches only oak
		assertTrue(tRecipes.get(0).isRecipeInputEqual(false, false, null, new ItemStack(Items.OAK_LOG, 4)));
		assertFalse(tRecipes.get(0).isRecipeInputEqual(false, false, null, new ItemStack(Items.SPRUCE_LOG, 4)));
	}

	/** Null guards: no charcoal gem → nothing expands; a null creosote keeps the item recipes (the NF branch). */
	@Test
	void expandGuards() {
		assertEquals(0, GT6CokeOvenLogExpansion.expand(List.of(Items.OAK_LOG), null, Fluids.WATER).size());
		List<Recipe> tNoFluid = GT6CokeOvenLogExpansion.expand(List.of(Items.OAK_LOG), CHARCOAL_GEM, null);
		assertEquals(1, tNoFluid.size());
		assertEquals(0, tNoFluid.get(0).mFluidOutputs.length, "the creosote <= 0 ? NF branch (Woods.java:179)");
		assertTrue(GT6CokeOvenLogExpansion.expand(Set.of(), CHARCOAL_GEM, Fluids.WATER).isEmpty());
	}

	/** The end-to-end findRecipe over the tag subset: an oak_log stack matches its own recipe. */
	@Test
	void tagSubsetIsFindableAndConsumable() {
		GT6RecipeMaps.init();
		List<Recipe> tRecipes = GT6CokeOvenLogExpansion.expand(
				List.of(Items.OAK_LOG, Items.SPRUCE_LOG), CHARCOAL_GEM, Fluids.WATER);
		GT6CokeOvenTagListener.replaceLogRecipes(tRecipes);
		assertEquals(2, GT6RecipeMaps.COKE_OVEN.mRecipeList.size());

		Recipe tFound = GT6RecipeMaps.COKE_OVEN.findRecipe(null, 16, ItemStack.EMPTY, null, new ItemStack(Items.OAK_LOG, 16));
		assertNotNull(tFound);
		assertTrue(tFound.isRecipeInputEqual(true, false, null, new ItemStack(Items.OAK_LOG, 2)));
		FluidStack[] tFluids = tFound.getFluidOutputs(1);
		assertEquals(GT6CokeOvenLogExpansion.CREOSOTE_MB_PER_LOG, tFluids[0].getAmount());
	}

	/** Repeated rebuilds replace the subset — no stacking (the coordinator ruling #4 idempotence). */
	@Test
	void rebuildReplacesInsteadOfStacking() {
		GT6RecipeMaps.init();

		GT6CokeOvenTagListener.replaceLogRecipes(GT6CokeOvenLogExpansion.expand(List.of(Items.OAK_LOG), CHARCOAL_GEM, Fluids.WATER));
		assertEquals(1, GT6RecipeMaps.COKE_OVEN.mRecipeList.size());
		assertEquals(1, GT6CokeOvenTagListener.logRecipeCount());

		// the tag grew (e.g. a mod added a log): the rebuild replaces, the static rows would sit beside it
		GT6CokeOvenTagListener.replaceLogRecipes(GT6CokeOvenLogExpansion.expand(List.of(Items.OAK_LOG, Items.SPRUCE_LOG, Items.BIRCH_LOG), CHARCOAL_GEM, Fluids.WATER));
		assertEquals(3, GT6RecipeMaps.COKE_OVEN.mRecipeList.size(), "the fresh subset fully replaces the old one");

		// the same content again (a re-fire on the same tag data): still no stacking
		GT6CokeOvenTagListener.replaceLogRecipes(GT6CokeOvenLogExpansion.expand(List.of(Items.OAK_LOG, Items.SPRUCE_LOG, Items.BIRCH_LOG), CHARCOAL_GEM, Fluids.WATER));
		assertEquals(3, GT6RecipeMaps.COKE_OVEN.mRecipeList.size());

		// the tag shrank: the removed recipes are gone
		GT6CokeOvenTagListener.replaceLogRecipes(GT6CokeOvenLogExpansion.expand(List.of(Items.SPRUCE_LOG), CHARCOAL_GEM, Fluids.WATER));
		assertEquals(1, GT6RecipeMaps.COKE_OVEN.mRecipeList.size());
		Recipe tSpruce = GT6RecipeMaps.COKE_OVEN.findRecipe(null, 16, ItemStack.EMPTY, null, new ItemStack(Items.SPRUCE_LOG, 1));
		assertNotNull(tSpruce);
		assertNull(GT6RecipeMaps.COKE_OVEN.findRecipe(null, 16, ItemStack.EMPTY, null, new ItemStack(Items.OAK_LOG, 1)));
	}

	/** rebuild() with no map generation early-returns (the offline-guard shape, no registry touches). */
	@Test
	void rebuildWithoutMapGenerationIsSafe() {
		assertTrue(RecipeMap.RECIPE_MAPS.isEmpty());
		assertDoesNotThrow(GT6CokeOvenTagListener::rebuild);
		assertEquals(0, GT6CokeOvenTagListener.logRecipeCount());
	}
}
