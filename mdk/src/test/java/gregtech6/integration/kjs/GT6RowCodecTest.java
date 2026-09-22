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
//? if kjs {
package gregtech6.integration.kjs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.GTRecipesOfflineTestBase;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;

/**
 * The RM row script-write path offline codec pins (task p34-kjs-bindings acceptance
 * ③): builder → {@link Recipe} row → {@link RecipeMap#addRecipe} single funnel, the
 * re-run idempotence ledger, the remove half and the FROZEN-gate semantics
 * (restart-to-apply, GT6KJS class doc). Per-test fresh generation = the
 * GT6RecipeMapPhaseGateTest discipline (reset in before/after, the P18 ledger).
 */
class GT6RowCodecTest extends GTRecipesOfflineTestBase {

	/** Unique fixture name per test (the RecipeMap ctor registers into the shared RECIPE_MAPS — shared names collide, the TileEntityBasicMachineSideIOTest counter convention). */
	private static int sFixtureCounter = 0;

	private String mMapName;

	@BeforeEach
	void freshGeneration() {
		GT6RecipeMaps.reset();
		GT6KJS.resetLedger(); // the kjs ledger must never outlive its generation
		mMapName = "gt.recipe.kjs.codec." + (sFixtureCounter++);
		new RecipeMap(new HashSet<>(), mMapName, "KJS Codec", null,
				0, 1, "gt6:textures/gui/machines/fixture", 2, 2, 0, 1, 1, 0, 0, 1);
	}

	@AfterEach
	void dropGeneration() {
		GT6KJS.resetLedger();
		GT6RecipeMaps.reset(); // rewinds the phase with the generation (the P18 discipline)
	}

	@Test
	void rowPoursThroughTheFunnelAndIsFindable() {
		assertTrue(GT6Recipes.instance().map(mMapName)
				.inputs(new ItemStack(Items.BRICK))
				.outputs(new ItemStack(Items.IRON_INGOT))
				.duration(40).eut(16).add(), "a well-formed row pours");
		RecipeMap tMap = GT6KJS.map(mMapName);
		assertEquals(1, tMap.mRecipeList.size());
		Recipe tRow = GT6KJS.ledgerRows(mMapName)[0];
		assertEquals(40, tRow.mDuration);
		assertEquals(16, tRow.mEUt);
		// The hash-index read path finds the script row like any other (the funnel indexed it).
		assertNotNull(tMap.findRecipe(null, Long.MAX_VALUE, null,
				new net.minecraftforge.fluids.FluidStack[0], new ItemStack(Items.BRICK)));
	}

	@Test
	void rerunIsIdempotentViaTheLedger() {
		for (int i = 0; i < 2; i++) {
			assertTrue(GT6Recipes.instance().map(mMapName)
					.inputs(new ItemStack(Items.BRICK))
					.outputs(new ItemStack(Items.IRON_INGOT))
					.duration(40).eut(16).add(), "re-run pour #" + i);
		}
		assertEquals(1, GT6KJS.map(mMapName).mRecipeList.size(), "the re-pour replaces the same-signature instance (1.20.1 stack identity-equals string keys)");
		assertEquals(1, GT6KJS.ledgerRows(mMapName).length, "the ledger tracks exactly the live script rows");
	}

	@Test
	void removeHalfRemovesAndUntracks() {
		assertTrue(GT6Recipes.instance().map(mMapName)
				.inputs(new ItemStack(Items.BRICK)).outputs(new ItemStack(Items.IRON_INGOT))
				.duration(40).eut(16).add());
		assertTrue(GT6Recipes.instance().map(mMapName)
				.inputs(new ItemStack(Items.SAND)).outputs(new ItemStack(Items.GLASS))
				.duration(20).eut(8).add());
		assertEquals(2, GT6KJS.map(mMapName).mRecipeList.size());
		assertEquals(1, GT6Recipes.instance().remove(mMapName, r -> r.mEUt == 16), "the predicate picks one row");
		assertEquals(1, GT6KJS.map(mMapName).mRecipeList.size());
		assertEquals(1, GT6KJS.ledgerRows(mMapName).length, "the removed row is also untracked");
	}

	@Test
	void structuralGatesMirrorTheJsonLoader() {
		// duration > 0 required (the JSON loader's "duration is required" bad-row gate).
		assertThrows(IllegalArgumentException.class, () -> GT6Recipes.instance().map(mMapName)
				.inputs(new ItemStack(Items.BRICK)).duration(0).add());
		// The ghost-recipe guard: no inputs at all = the funnel's silent null (add() false).
		assertFalse(GT6Recipes.instance().map(mMapName)
				.outputs(new ItemStack(Items.IRON_INGOT)).duration(40).add(), "input-less row = ghost, rejected");
		// Unknown map name = null builder (the script typo surfaces as a null, not a crash).
		assertEquals(null, GT6Recipes.instance().map("gt.recipe.no.such.map.kjs"));
	}

	@Test
	void frozenGenerationKeepsRowsAndDegradesGracefully() {
		assertTrue(GT6Recipes.instance().map(mMapName)
				.inputs(new ItemStack(Items.BRICK)).outputs(new ItemStack(Items.IRON_INGOT))
				.duration(40).eut(16).add(), "boot-window pour is legal (OPEN)");
		GT6RecipeMaps.freeze();
		assertFalse(GT6Recipes.instance().map(mMapName)
				.inputs(new ItemStack(Items.SAND)).outputs(new ItemStack(Items.GLASS))
				.duration(20).eut(8).add(), "FROZEN = the addRow catch degrades to false (restart-to-apply)");
		assertEquals(1, GT6KJS.map(mMapName).mRecipeList.size(), "existing rows kept — no reload crash, no partial pour");
		assertEquals(1, GT6KJS.ledgerRows(mMapName).length, "the ledger still matches the live rows");
		// Removal is a read-path repair — legal while FROZEN (the CokeOven direct-write seam).
		assertEquals(1, GT6Recipes.instance().remove(mMapName, r -> true), "remove works while frozen");
		assertEquals(0, GT6KJS.map(mMapName).mRecipeList.size());
	}

	@Test
	void chancesRideTheRow() {
		assertTrue(GT6Recipes.instance().map(mMapName)
				.inputs(new ItemStack(Items.BRICK))
				.outputs(new ItemStack(Items.IRON_INGOT), new ItemStack(Items.GOLD_INGOT))
				.chances(10000, 5000)
				.duration(40).eut(16).add());
		Recipe tRow = GT6KJS.ledgerRows(mMapName)[0];
		assertEquals(2, tRow.mChances.length);
		assertEquals(10000, tRow.mChances[0]);
		assertEquals(5000, tRow.mChances[1]);
	}
}
//?}
