package gregtech6.tileentity.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.junit.jupiter.api.Test;

import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;

/**
 * Acceptance 1 (task p4-machine-oven): the recipe consumption contract — findRecipe only
 * LOOKS UP, the machine consumes through Recipe.isRecipeInputEqual(true, false, ...) in the
 * two-stage checkRecipe (upstream MultiTileEntityBasicMachine.java:725/:738), and canOutput
 * (:620-668) keeps the output blockage semantics.
 */
public class TileEntityOvenRecipeTest extends GTMachinesOfflineTestBase {

	private TileEntityOven ovenWithSand(int aCount) {
		TileEntityOven tOven = makeOven(smeltingLevel());
		tOven.getInventory().insertItem(TileEntityOven.SLOT_INPUT, new ItemStack(Items.SAND, aCount), false);
		return tOven;
	}

	@Test
	void probeFindsRecipeWithoutConsuming() {
		TileEntityOven tOven = ovenWithSand(8);
		int tResult = tOven.checkRecipe(false, true); // upstream onTickFirst :446 / doInactive :897 shape
		assertEquals(TileEntityOven.FOUND_AND_COULD_HAVE_USED_RECIPE, tResult, ":740 probe result");
		assertTrue(tOven.mCouldUseRecipe, ":739");
		assertEquals(8, tOven.getInventory().getStackInSlot(TileEntityOven.SLOT_INPUT).getCount(), "the probe never consumes");
		assertEquals(0, tOven.mMaxProgress, "the probe stops before the energy math (:740 early return)");
		assertNull(tOven.mCurrentRecipe, ":757 not reached on the probe path");
		assertNull(tOven.mLastRecipe, "Furnace recipes are mCanBeBuffered=false (:154) — nothing cached");
	}

	@Test
	void applyConsumesAndComputesTheEnergyMath() {
		TileEntityOven tOven = ovenWithSand(8);
		int tResult = tOven.checkRecipe(true, true); // upstream doActive :800 shape
		assertEquals(TileEntityOven.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tResult, ":777");
		assertEquals(7, tOven.getInventory().getStackInSlot(TileEntityOven.SLOT_INPUT).getCount(), "exactly one input consumed");
		assertEquals(256, tOven.mMaxProgress);
		assertEquals(16, tOven.mMinEnergy);
		assertEquals(1, tOven.mOutputItems.length, ":758");
		assertEquals(Items.GLASS, tOven.mOutputItems[0].getItem());
		assertNotNull(tOven.mCurrentRecipe, ":757");
	}

	@Test
	void applyOnEmptyInputFindsNothing() {
		TileEntityOven tOven = makeOven(smeltingLevel());
		assertEquals(TileEntityOven.DID_NOT_FIND_RECIPE, tOven.checkRecipe(true, true), ":708 minimal-input gate");
	}

	@Test
	void foreignOutputBlocksTheRecipe() {
		TileEntityOven tOven = ovenWithSand(8);
		tOven.getInventory().setStackInSlot(TileEntityOven.SLOT_OUTPUT, new ItemStack(Items.DIAMOND, 4));
		assertEquals(TileEntityOven.FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS,
				tOven.checkRecipe(true, true), ":637-640 — a different item in the output slot blocks");
		assertEquals(8, tOven.getInventory().getStackInSlot(TileEntityOven.SLOT_INPUT).getCount(),
				"the blocked recipe is rejected before isRecipeInputEqual consumes (:736 early return)");
		assertEquals(0, tOven.mMaxProgress);
	}

	@Test
	void fullOutputSlotBlocksTheRecipe() {
		TileEntityOven tOven = ovenWithSand(8);
		tOven.getInventory().setStackInSlot(TileEntityOven.SLOT_OUTPUT, new ItemStack(Items.GLASS, 64));
		assertEquals(TileEntityOven.FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS,
				tOven.checkRecipe(true, true), ":641-645 — no capacity left for the +1 output");
	}

	@Test
	void matchingOutputWithCapacityPasses() {
		TileEntityOven tOven = ovenWithSand(8);
		tOven.getInventory().setStackInSlot(TileEntityOven.SLOT_OUTPUT, new ItemStack(Items.GLASS, 10));
		assertEquals(TileEntityOven.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tOven.checkRecipe(true, true),
				"same item + capacity: canOutput computes the parallel count :641");
	}

	@Test
	void needsEmptyOutputRecipeRespectsTheOccupiedSlot() {
		TileEntityOven tOven = ovenWithSand(8);
		tOven.getInventory().setStackInSlot(TileEntityOven.SLOT_OUTPUT, new ItemStack(Items.GLASS, 10));
		// :633-636 — mNeedsEmptyOutput blocks even a matching occupied output slot; the vanilla
		// FURNACE bridge has no recipe stock, so a plain RecipeMap carries the recipe.
		RecipeMap tMap = new RecipeMap(new java.util.HashSet<>(), "gt6.test.needsempty", "NeedsEmpty", "needsempty",
				0, 1, "gt6:textures/gui/machines/oven", 1, 1, 1, 0, 0, 0, 0, 1);
		tMap.addRecipe(new Recipe(true, new ItemStack[] {new ItemStack(Items.SAND)}, new ItemStack[] {new ItemStack(Items.GLASS)}, null, null, 16, 16, 0).setNeedEmptyOut());
		tOven.mRecipes = tMap;
		assertEquals(TileEntityOven.FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS,
				tOven.checkRecipe(true, true), "mNeedsEmptyOutput + occupied output slot = blocked");
	}

	@Test
	void specialSlotContentRidesAlongWithoutAffectingTheLookup() {
		TileEntityOven tOven = ovenWithSand(8);
		tOven.getInventory().setStackInSlot(TileEntityOven.SLOT_SPECIAL, new ItemStack(Items.GOLD_NUGGET));
		assertEquals(TileEntityOven.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tOven.checkRecipe(true, true),
				"the Furnace map ignores the special slot content (upstream findRecipe :463)");
		assertEquals(7, tOven.getInventory().getStackInSlot(TileEntityOven.SLOT_INPUT).getCount());
		assertEquals(Items.GOLD_NUGGET, tOven.getInventory().getStackInSlot(TileEntityOven.SLOT_SPECIAL).getItem(),
				"the special slot is not consumed");
	}
}
