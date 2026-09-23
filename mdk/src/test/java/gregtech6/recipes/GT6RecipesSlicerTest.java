/**
 * Copyright (c) 2025 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3, or (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see http://www.gnu.org/licenses/lgpl-3.0.txt
 */

package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import gregtech6.registry.GT6SlicerBlades;

/**
 * The RM.Slicer vanilla-face static pour, offline (task p35-slicer-row-domain): the
 * upstream row对照 pins over the fixture-seam pour (the GT6RecipesExtruderTest stub form).
 * The vanilla items ARE constructible offline — only the blades and the tiny-paper output
 * ride the loader's resolver seams over distinct vanilla stand-ins (the recipe mechanics
 * only compare identities).
 *
 * <p>Pins: the :638-:641/:420 constants (EUt 16, duration 16, the leather yield column
 * 1/2/2/1, the 9 tiny-paper output), the blade-not-consumed crown (the size-0 marker
 * port), the W damage wildcard (a worn helmet still slices — the no-tag match half), and
 * the ADR-P18 re-pour face.
 */
class GT6RecipesSlicerTest extends GTRecipesOfflineTestBase {

	private static final Item SPLIT_BLADE = Items.IRON_INGOT;
	private static final Item GRID_BLADE = Items.GOLD_INGOT;
	private static final Item TINY_PAPER_OUT = Items.PAPER; // the stand-in identity for plateTiny.Paper

	private static java.util.function.Supplier<ItemStack> sDefaultSplitBlade;
	private static java.util.function.Supplier<ItemStack> sDefaultGridBlade;
	private static java.util.function.Supplier<ItemStack> sDefaultTinyPaper;
	private static java.util.function.Predicate<ItemStack> sDefaultNotConsumable;
	private static java.util.function.Predicate<ItemStack> sDefaultBladeTest;

	@BeforeAll
	static void captureDefaults() {
		sDefaultSplitBlade = GT6RecipesSlicer.sSplitBladeResolver;
		sDefaultGridBlade = GT6RecipesSlicer.sGridBladeResolver;
		sDefaultTinyPaper = GT6RecipesSlicer.sTinyPaperResolver;
		sDefaultNotConsumable = Recipe.sNotConsumable;
		sDefaultBladeTest = GT6SlicerBlades.sBladeTest;
	}

	@AfterAll
	static void restoreDefaultsOnly() {
		restoreAll();
	}

	@AfterEach
	void restoreSeams() {
		restoreAll();
		GT6RecipeMaps.reset();
		GT6RecipesSlicer.resetForTest();
	}

	private static void restoreAll() {
		GT6RecipesSlicer.sSplitBladeResolver = sDefaultSplitBlade;
		GT6RecipesSlicer.sGridBladeResolver = sDefaultGridBlade;
		GT6RecipesSlicer.sTinyPaperResolver = sDefaultTinyPaper;
		Recipe.sNotConsumable = sDefaultNotConsumable;
		GT6SlicerBlades.sBladeTest = sDefaultBladeTest;
	}

	/** The fixture universe: the two blade stand-ins + the tiny-paper stand-in, production default predicates (the isBlade arm IS the not-consumed face under test). */
	private static void installFixtures() {
		GT6RecipesSlicer.sSplitBladeResolver = () -> new ItemStack(SPLIT_BLADE, 1);
		GT6RecipesSlicer.sGridBladeResolver = () -> new ItemStack(GRID_BLADE, 1);
		GT6RecipesSlicer.sTinyPaperResolver = () -> new ItemStack(TINY_PAPER_OUT, 9);
		// the production predicate shape over the fixture identities: the sBladeTest seam
		// feeds the Recipe.sNotConsumable arm exactly like the live registry does
		GT6SlicerBlades.sBladeTest = aStack -> aStack != null && !aStack.isEmpty()
				&& (aStack.getItem() == SPLIT_BLADE || aStack.getItem() == GRID_BLADE);
		Recipe.sNotConsumable = aStack -> aStack != null && !aStack.isEmpty()
				&& (aStack.getItem() == SPLIT_BLADE || aStack.getItem() == GRID_BLADE);
	}

	private static int pour() {
		GT6RecipeMaps.init();
		GT6RecipesSlicer.load();
		assertNotNull(GT6RecipeMaps.SLICER);
		return GT6RecipeMaps.SLICER.mRecipeList.size();
	}

	/** The pour lands exactly the five vanilla rows (no food, no fur, no melon — the CUT faces). */
	@Test
	public void pourLandsExactlyTheFiveVanillaRows() {
		installFixtures();
		assertEquals(5, pour(), "the leather quartet + the paper row");
		long tSplitRows = GT6RecipeMaps.SLICER.mRecipeList.stream()
				.filter(aRecipe -> aRecipe.mInputs[1].getItem() == SPLIT_BLADE).count();
		long tGridRows = GT6RecipeMaps.SLICER.mRecipeList.stream()
				.filter(aRecipe -> aRecipe.mInputs[1].getItem() == GRID_BLADE).count();
		assertEquals(4, tSplitRows, "the :638-:641 leather quartet rides the split blade");
		assertEquals(1, tGridRows, "the :420 paper row rides the grid blade");
	}

	/** The upstream constants pin: EUt 16, duration 16, the leather yield column 1/2/2/1, buffered rows. */
	@Test
	public void rowsCarryTheUpstreamConstants() {
		installFixtures();
		pour();
		record Expectation(Item aArmor, int aYield) {}
		for (Expectation tExp : new Expectation[] {
				new Expectation(Items.LEATHER_HELMET, 1),     // :638
				new Expectation(Items.LEATHER_CHESTPLATE, 2), // :639
				new Expectation(Items.LEATHER_LEGGINGS, 2),   // :640
				new Expectation(Items.LEATHER_BOOTS, 1)}) {   // :641
			Recipe tRow = GT6RecipeMaps.SLICER.mRecipeList.stream()
					.filter(aRecipe -> aRecipe.mInputs[0].getItem() == tExp.aArmor).findFirst().orElse(null);
			assertNotNull(tRow, "the row for " + tExp.aArmor);
			assertEquals(GT6RecipesSlicer.SLICER_EUT, tRow.mEUt, "the :638-:641 EUt column");
			assertEquals(GT6RecipesSlicer.SLICER_DURATION, tRow.mDuration, "the :638-:641 duration column");
			assertEquals(tExp.aYield, tRow.mOutputs[0].getCount(), "the leather yield column");
			assertEquals(Items.LEATHER, tRow.mOutputs[0].getItem(), "the leather output identity");
			assertTrue(tRow.mCanBeBuffered, "the static rows are buffered (the upstream aCanBeBuffered T)");
			assertEquals(2, tRow.mInputs.length, "armor + blade");
		}
		Recipe tPaperRow = GT6RecipeMaps.SLICER.mRecipeList.stream()
				.filter(aRecipe -> aRecipe.mInputs[0].getItem() == Items.PAPER).findFirst().orElse(null);
		assertNotNull(tPaperRow, "the :420 paper row");
		assertEquals(GT6RecipesSlicer.SLICER_EUT, tPaperRow.mEUt, "the :420 EUt column");
		assertEquals(GT6RecipesSlicer.SLICER_DURATION, tPaperRow.mDuration, "the :420 duration column");
		assertEquals(9, tPaperRow.mOutputs[0].getCount(), "the :420 face: 9 tiny paper plates");
	}

	/** THE STATIC-ROW CROWN: the consume pass eats the armor/paper and leaves the blade in its slot (the size-0 marker port). */
	@Test
	public void consumeEatsTheInputAndLeavesTheBlade() {
		installFixtures();
		pour();
		// the rows live in a HashSet-ordered list — pair each row with ITS OWN primary
		// input (the blade-filtered first row may be any of the five)
		for (Item tPrimary : new Item[] {Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS, Items.PAPER}) {
			Recipe tRow = GT6RecipeMaps.SLICER.mRecipeList.stream()
					.filter(aRecipe -> aRecipe.mInputs[0].getItem() == tPrimary).findFirst().orElse(null);
			assertNotNull(tRow, "the row for the " + tPrimary);
			ItemStack tBlade = tRow.mInputs[1].copy();
			tBlade.setCount(4);
			ItemStack tInput = new ItemStack(tPrimary, 7);
			assertTrue(tRow.isRecipeInputEqual(true, false, null, tBlade, tInput), "the consume pass succeeds");
			assertEquals(4, tBlade.getCount(), "THE BLADE STAYS IN THE SLOT — " + tPrimary);
			assertEquals(6, tInput.getCount(), "the armor/paper input is consumed");
		}
	}

	/**
	 * The W damage wildcard: the upstream {@code ST.make(armor, 1, W)} row input carries no
	 * tag, so the match half compares item identity alone — a DAMAGED helmet still slices.
	 */
	@Test
	public void damagedArmorStillSlices() {
		installFixtures();
		pour();
		Recipe tRow = GT6RecipeMaps.SLICER.mRecipeList.stream()
				.filter(aRecipe -> aRecipe.mInputs[0].getItem() == Items.LEATHER_HELMET).findFirst().orElse(null);
		assertNotNull(tRow);
		ItemStack tWorn = new ItemStack(Items.LEATHER_HELMET, 1);
		tWorn.setDamageValue(100);
		assertTrue(tRow.isRecipeInputEqual(false, false, null, new ItemStack(SPLIT_BLADE, 1), tWorn),
				"the damaged helmet matches (the no-tag identity match half)");
	}

	/** The map-contract pin: a single-input probe NEVER matches (mMinimalInputItems == 2, TileEntityBasicMachine.java:626). */
	@Test
	public void singleInputProbesNeverMatch() {
		installFixtures();
		pour();
		Recipe tRow = GT6RecipeMaps.SLICER.mRecipeList.stream()
				.filter(aRecipe -> aRecipe.mInputs[0].getItem() == Items.LEATHER_HELMET).findFirst().orElse(null);
		assertNotNull(tRow);
		assertFalse(tRow.isRecipeInputEqual(true, false, null, new ItemStack(Items.LEATHER_HELMET, 1)),
				"the blade is a HARD prerequisite — the armor alone does not match");
	}

	/** A non-row input set does not match (the probe-pass negative face). */
	@Test
	public void nonRowInputsDoNotMatch() {
		installFixtures();
		pour();
		Recipe tPaperRow = GT6RecipeMaps.SLICER.mRecipeList.stream()
				.filter(aRecipe -> aRecipe.mInputs[0].getItem() == Items.PAPER).findFirst().orElse(null);
		assertNotNull(tPaperRow);
		assertFalse(tPaperRow.isRecipeInputEqual(true, false, null,
				new ItemStack(GRID_BLADE, 1), new ItemStack(Items.IRON_INGOT, 1)),
				"paper vs iron ingot: no match");
	}

	/** The load() re-pour after a bare map reset (the ADR-P18 canary, the Extruder face). */
	@Test
	public void loadRepoursAfterABareMapReset() {
		installFixtures();
		GT6RecipeMaps.reset();
		GT6RecipesSlicer.resetForTest(); // self-grounding
		assertEquals(5, pour(), "the first pour");
		GT6RecipeMaps.reset(); // the BARE reset — the poison constructor
		GT6RecipesSlicer.load();
		assertEquals(5, GT6RecipeMaps.SLICER.mRecipeList.size(),
				"load() must truly re-pour after a bare reset() — the pour-flag retires WITH the generation");
	}

	/** The probe pass never mutates the inputs (the two-stage contract on the static rows). */
	@Test
	public void probePassLeavesTheInputsUntouched() {
		installFixtures();
		pour();
		Recipe tRow = GT6RecipeMaps.SLICER.mRecipeList.stream()
				.filter(aRecipe -> aRecipe.mInputs[0].getItem() == Items.LEATHER_BOOTS).findFirst().orElse(null);
		assertNotNull(tRow);
		ItemStack tBlade = new ItemStack(SPLIT_BLADE, 1);
		ItemStack tBoots = new ItemStack(Items.LEATHER_BOOTS, 1);
		assertTrue(tRow.isRecipeInputEqual(false, false, null, tBlade, tBoots));
		assertEquals(1, tBlade.getCount());
		assertEquals(1, tBoots.getCount());
	}
}
