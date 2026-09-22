/**
 * Copyright (c) 2025 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

package gregtech6.recipes.maps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.items.bees.GT6BumbleGenes;
import gregtech6.items.bees.GT6Bumbles;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.GT6RecipeMaps;

/**
 * The Bumblelyzer map acceptance (task p34-machines-bumblelyzer-crucible): the two dynamic
 * findRecipe arms (the :58-60 scan arm + the :61 pass-through arm, RecipeMapBumblelyzer.java
 * :50-74), the honey accept set (:55), the DECLARED-empty static stock (the fake display face
 * never joins the findable list), and the display-stock census — the :581-588 make() walk
 * over the {@link GT6Bumbles#SPECIES} table (acceptance ②: the 物种表行数对账 80 × 3 × 4 =
 * 960 rows plus the representative-row content pins).
 *
 * <p>Offline posture: the bee faces ride the vanilla stand-in pair (CLAY_BALL = the
 * unscanned drone, BRICK = the scanned drone — the frozen registry forbids new mod items,
 * the p33 lesson) through the {@code sBumbleType}/{@code sBeeItemResolver} seams, and the
 * paper/fluid-id/honey seams ride the injected fixtures (the
 * {@link GT6RecipeMapCanner#sContainerResolver} convention). The live loop is the RCON
 * chain (acceptance ③).
 */
class GT6BumblelyzerMapTest {

	private static final java.util.function.Supplier<Item> DEFAULT_PAPER = GT6RecipeMapBumblelyzer.sPaperResolver;
	private static final java.util.function.Function<Fluid, String> DEFAULT_FLUID_ID = GT6RecipeMapBumblelyzer.sFluidIdResolver;
	private static final java.util.function.IntFunction<Item> DEFAULT_BEE = GT6Bumbles.sBeeItemResolver;
	private static final java.util.function.Function<String, Fluid> DEFAULT_HONEY = GT6Bumbles.sHoneyFluidResolver;
	private static final java.util.function.Function<ItemStack, Byte> DEFAULT_TYPE = GT6Bumbles.sBumbleType;

	/** The vanilla stand-in pair: CLAY_BALL plays the unscanned drone, BRICK the scanned drone. */
	private static final Item BEE = Items.CLAY_BALL, SCANNED = Items.BRICK;

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
		GT6RecipeMaps.init();
		GT6RecipeMapBumblelyzer.sPaperResolver = () -> Items.PAPER;
		GT6RecipeMapBumblelyzer.sFluidIdResolver = aFluid -> "honey"; // the identity fixture — every probe fluid reads as the honey face
		GT6Bumbles.sBeeItemResolver = aIndex -> aIndex == 0 ? BEE : SCANNED; // FACES 0 = the drone, 4 = the scanned drone
		GT6Bumbles.sHoneyFluidResolver = aId -> Fluids.WATER;
		GT6Bumbles.sBumbleType = aStack -> { // CLAY_BALL = the type-0 unscanned drone, BRICK = the type-5 scanned drone
			if (aStack.is(BEE)) return Byte.valueOf((byte)0);
			if (aStack.is(SCANNED)) return Byte.valueOf((byte)5);
			return null;
		};
	}

	@AfterAll
	static void restoreSeams() {
		GT6RecipeMapBumblelyzer.sPaperResolver = DEFAULT_PAPER;
		GT6RecipeMapBumblelyzer.sFluidIdResolver = DEFAULT_FLUID_ID;
		GT6Bumbles.sBeeItemResolver = DEFAULT_BEE;
		GT6Bumbles.sHoneyFluidResolver = DEFAULT_HONEY;
		GT6Bumbles.sBumbleType = DEFAULT_TYPE;
		GT6RecipeMaps.reset(); // the generation hook retires the display stock WITH the maps
	}

	@AfterEach
	void clearStock() {
		GT6RecipeMapBumblelyzer.sFakeRecipes = java.util.List.of(); // each test fills its own census
	}

	// ------------------------------------------------- the map declaration row

	@Test
	void theMapCarriesTheRMJava107ConstantsRow() {
		GT6RecipeMapBumblelyzer tMap = GT6RecipeMaps.BUMBLELYZER;
		assertNotNull(tMap, "the BUMBLELYZER map registers with init()");
		assertEquals("gt.recipe.bumblelyzer", tMap.mNameInternal, "the :107 unlocalized name");
		assertEquals("Bumblelyzer", tMap.mNameLocal, "the :107 local name");
		assertEquals(2, tMap.mInputItemsCount, "items 2/2/0");
		assertEquals(2, tMap.mOutputItemsCount, "the output column");
		assertEquals(0, tMap.mMinimalInputItems, "the MIN items column");
		assertEquals(1, tMap.mInputFluidCount, "fluids 1/0/0");
		assertEquals(0, tMap.mMinimalInputFluids, "the MIN fluids column");
		assertEquals(2, tMap.mMinimalInputs, "the :107 MIN 2 column");
	}

	// ------------------------------------------------- the dynamic findRecipe arms

	/** :58-60 — the scan arm: bee + paper tiny → the scanned form, 10 L of the tank fluid, 64 @ 16. */
	@Test
	void theScanArmScansAnUnscannedBee() {
		ItemStack tBee = new ItemStack(BEE, 1);
		Recipe tRow = GT6RecipeMaps.BUMBLELYZER.findRecipe(null, Long.MAX_VALUE, null,
				new FluidStack[] {new FluidStack(Fluids.WATER, 1000)}, tBee);
		assertNotNull(tRow, "the honey tank + the bee resolves the scan arm");
		assertEquals(2, tRow.mInputs.length, "the bee + the paper tiny");
		assertTrue(ItemStack.isSameItemSameTags(tRow.mInputs[0], tBee), "the bee input is the single-copy");
		assertTrue(ItemStack.isSameItemSameTags(tRow.mInputs[1], new ItemStack(Items.PAPER, 1)), "the paper leg");
		assertEquals(1, tRow.mOutputs.length, "one output");
		assertEquals(SCANNED, tRow.mOutputs[0].getItem(), "the scanned face");
		assertFalse(tRow.mCanBeBuffered, "one-time recipes never cache (the upstream F, F, F prefix)");
		assertEquals(10, tRow.mFluidInputs[0].getAmount(), "the 10 L scan charge (:60)");
		assertEquals(64L, tRow.mDuration, "the :60 duration");
		assertEquals(16L, tRow.mEUt, "the :60 EUt");
	}

	/** :564 — the scan output carries the input stack's NBT (the gt.bumble genes ride the meta+5 copy). */
	@Test
	void theScanArmKeepsTheGenes() {
		ItemStack tBee = new ItemStack(BEE, 1);
		GT6BumbleGenes.setCode(tBee, 20330); // the tera row
		Recipe tRow = GT6RecipeMaps.BUMBLELYZER.findRecipe(null, Long.MAX_VALUE, null,
				new FluidStack[] {new FluidStack(Fluids.WATER, 1000)}, tBee);
		assertNotNull(tRow);
		assertEquals(20330, GT6BumbleGenes.codeOf(tRow.mOutputs[0]), "the code tag rides the scan");
	}

	/** :61 — the pass-through arm: a scanned bee copies itself, no paper, no fluid, 1 @ 16. */
	@Test
	void thePassThroughArmAutoSkipsAScannedBee() {
		ItemStack tScanned = new ItemStack(SCANNED, 1);
		Recipe tRow = GT6RecipeMaps.BUMBLELYZER.findRecipe(null, Long.MAX_VALUE, null,
				new FluidStack[] {new FluidStack(Fluids.WATER, 1000)}, tScanned);
		assertNotNull(tRow, "a scanned bee still routes (auto-skip)");
		assertEquals(1, tRow.mInputs.length, "no paper leg");
		assertEquals(0, tRow.mFluidInputs.length, "no fluid leg");
		assertEquals(1L, tRow.mDuration, "the :61 duration");
		assertEquals(16L, tRow.mEUt, "the :61 EUt");
		assertTrue(ItemStack.isSameItemSameTags(tRow.mInputs[0], tScanned), "in == out (the copy)");
	}

	/** :53/:55 — the guards: no tank, or a non-honey first tank → no dynamic row (the seam restores before returning). */
	@Test
	void theGuardsRefuseNonHoneyAndEmptyTanks() {
		GT6RecipeMapBumblelyzer.sFluidIdResolver = aFluid -> "water"; // the non-honey face
		assertNull(GT6RecipeMaps.BUMBLELYZER.findRecipe(null, Long.MAX_VALUE, null,
				new FluidStack[] {new FluidStack(Fluids.WATER, 1000)}, new ItemStack(BEE, 1)), "a non-honey tank refuses");
		assertNull(GT6RecipeMaps.BUMBLELYZER.findRecipe(null, Long.MAX_VALUE, null,
				null, new ItemStack(BEE, 1)), "no tank at all refuses");
		GT6RecipeMapBumblelyzer.sFluidIdResolver = aFluid -> "honey"; // restore the honey face (the JUnit method order is arbitrary)
	}

	// ------------------------------------------------- the display-stock census (acceptance ②)

	/** The :581-588 census: SPECIES × {drone, princess, dead} × ({honey, honeydew} + pass-through pair). */
	@Test
	void theDisplayStockCensusReconcilesWithTheSpeciesTable() {
		GT6Bumbles.addScanFakeRecipes();
		assertEquals(GT6Bumbles.SPECIES.size() * 3 * 4, GT6RecipeMapBumblelyzer.sFakeRecipes.size(),
				"80 species x 3 type faces x (2 diluents + 2 pass-throughs) = 960 display rows");
		assertEquals(GT6RecipeMapBumblelyzer.fakeRowCensus(GT6Bumbles.SPECIES.size()), GT6RecipeMapBumblelyzer.sFakeRecipes.size(),
				"the map-side census constant agrees");
		assertEquals(0, GT6RecipeMaps.BUMBLELYZER.mRecipeList.size(),
				"the fake rows NEVER join the findable stock (the :293 upstream face, the port addRecipe :177)");
	}

	/** The representative rows: the first species/drone row content and the per-species code tags. */
	@Test
	void theRepresentativeRowsCarryTheUpstreamContent() {
		GT6Bumbles.addScanFakeRecipes();
		Recipe tFirst = GT6RecipeMapBumblelyzer.sFakeRecipes.get(0);
		assertEquals(2, tFirst.mInputs.length, "bee + paper");
		assertEquals(0, GT6BumbleGenes.codeOf(tFirst.mInputs[0]), "row 0 = the Wild Bumblebee (code 0)");
		assertEquals(64L, tFirst.mDuration, "the scan clock");
		assertEquals(16L, tFirst.mEUt, "the scan EUt");
		assertEquals(10, tFirst.mFluidInputs[0].getAmount(), "the 10 L diluent");

		// the per-species code face: the LAST row is the tera pass-through (code 20330)
		Recipe tLast = GT6RecipeMapBumblelyzer.sFakeRecipes.get(GT6RecipeMapBumblelyzer.sFakeRecipes.size() - 1);
		assertEquals(20330, GT6BumbleGenes.codeOf(tLast.mInputs[0]), "the tera species row closes the walk");
		assertEquals(0, tLast.mFluidInputs.length, "the pass-through form");
		assertEquals(1L, tLast.mDuration, "the pass-through clock");

		// the per-face structure: 2 scan rows + 2 pass-through rows per face — count the clocks
		int tScan = 0, tPass = 0;
		for (Recipe tRow : GT6RecipeMapBumblelyzer.sFakeRecipes) {
			if (tRow.mDuration == 64L) tScan++;
			else if (tRow.mDuration == 1L) tPass++;
		}
		assertEquals(GT6Bumbles.SPECIES.size() * 3 * 2, tScan, "the diluent scan rows");
		assertEquals(GT6Bumbles.SPECIES.size() * 3 * 2, tPass, "the pass-through pair rows");

		// the diluent face: every scan row charges exactly 10 L
		Set<Long> tFluids = new HashSet<>();
		for (Recipe tRow : GT6RecipeMapBumblelyzer.sFakeRecipes) if (tRow.mFluidInputs.length > 0) tFluids.add((long) tRow.mFluidInputs[0].getAmount());
		assertEquals(Set.of(10L), tFluids, "every scan row charges 10 L");
	}
}
