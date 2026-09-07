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
		// lowercase since p8-cokeoven-gui-menu ⑥ — the 1.20.1 ResourceLocation charset makes
		// the upstream-style "CokeOven" spelling unparseable for GTBasicMachineScreen.backgroundOf
		assertEquals("gt6:textures/gui/machines/cokeoven.png", GT6RecipeMaps.COKE_OVEN.mGUIPath);
	}

	/** The FM.java:45 Engine Fuels map constants (task p12-engine-fuel-fluids). */
	@Test
	void initRegistersEngineFuelsMapWithUpstreamConstants() {
		GT6RecipeMaps.init();
		assertNotNull(GT6RecipeMaps.ENGINE_FUELS);
		assertSame(GT6RecipeMaps.ENGINE_FUELS, RecipeMap.RECIPE_MAPS.get("gt.recipe.fuels.engine"));
		assertEquals("Engine Fuels", GT6RecipeMaps.ENGINE_FUELS.mNameLocal);
		assertEquals("gt.recipe.fuels.engine", GT6RecipeMaps.ENGINE_FUELS.mNameNEI, "FM.java:45 passes null → the internal name");
		assertEquals(1, GT6RecipeMaps.ENGINE_FUELS.mInputItemsCount);
		assertEquals(2, GT6RecipeMaps.ENGINE_FUELS.mOutputItemsCount);
		assertEquals(0, GT6RecipeMaps.ENGINE_FUELS.mMinimalInputItems, "MIN-ITEMS 0 — the upstream fluid-only lookup switch (Recipe.java:518-523)");
		assertEquals(1, GT6RecipeMaps.ENGINE_FUELS.mInputFluidCount);
		assertEquals(2, GT6RecipeMaps.ENGINE_FUELS.mOutputFluidCount);
		assertEquals(0, GT6RecipeMaps.ENGINE_FUELS.mMinimalInputFluids);
		assertEquals(1, GT6RecipeMaps.ENGINE_FUELS.mMinimalInputs);
		assertEquals(1, GT6RecipeMaps.ENGINE_FUELS.mPower);
		assertEquals(0, GT6RecipeMaps.ENGINE_FUELS.mProgressBarDirection);
		assertEquals(1, GT6RecipeMaps.ENGINE_FUELS.mProgressBarAmount);
		assertEquals("gt6:textures/gui/machines/default.png", GT6RecipeMaps.ENGINE_FUELS.mGUIPath, "the FM.java:45 machines/Default row, lowercased");
	}

	/** The FM.java:40 Fluidized Bed Fuels map constants (task p13-hu-steam-foundation). */
	@Test
	void initRegistersFluidBedMapWithUpstreamConstants() {
		GT6RecipeMaps.init();
		assertNotNull(GT6RecipeMaps.FLUIDBED);
		assertSame(GT6RecipeMaps.FLUIDBED, RecipeMap.RECIPE_MAPS.get("gt.recipe.fuels.fluidbed"));
		assertEquals("Fluidized Bed Fuels", GT6RecipeMaps.FLUIDBED.mNameLocal);
		assertEquals("gt.recipe.fuels.fluidbed", GT6RecipeMaps.FLUIDBED.mNameNEI, "FM.java:40 passes null → the internal name");
		assertEquals(1, GT6RecipeMaps.FLUIDBED.mInputItemsCount);
		assertEquals(2, GT6RecipeMaps.FLUIDBED.mOutputItemsCount);
		assertEquals(1, GT6RecipeMaps.FLUIDBED.mMinimalInputItems, "FM.java:40 MIN-ITEM 1 — the column that differs from the Burn row");
		assertEquals(1, GT6RecipeMaps.FLUIDBED.mInputFluidCount);
		assertEquals(2, GT6RecipeMaps.FLUIDBED.mOutputFluidCount);
		assertEquals(1, GT6RecipeMaps.FLUIDBED.mMinimalInputFluids, "FM.java:40 MIN-FLUID 1 — the column that differs from the Burn row");
		assertEquals(2, GT6RecipeMaps.FLUIDBED.mMinimalInputs, "FM.java:40 MIN 2 — the column that differs from the Burn row");
		assertEquals(1, GT6RecipeMaps.FLUIDBED.mPower);
		assertEquals(0, GT6RecipeMaps.FLUIDBED.mProgressBarDirection);
		assertEquals(1, GT6RecipeMaps.FLUIDBED.mProgressBarAmount);
		assertEquals("gt6:textures/gui/machines/default.png", GT6RecipeMaps.FLUIDBED.mGUIPath, "the FM.java:40 machines/Default row, lowercased");
	}

	/** The FM.java:41 Burnable Fuels map constants (task p13-hu-steam-foundation). */
	@Test
	void initRegistersBurnMapWithUpstreamConstants() {
		GT6RecipeMaps.init();
		assertNotNull(GT6RecipeMaps.BURN);
		assertSame(GT6RecipeMaps.BURN, RecipeMap.RECIPE_MAPS.get("gt.recipe.fuels.burn"));
		assertEquals("Burnable Fuels", GT6RecipeMaps.BURN.mNameLocal);
		assertEquals("gt.recipe.fuels.burn", GT6RecipeMaps.BURN.mNameNEI, "FM.java:41 passes null → the internal name");
		assertEquals(1, GT6RecipeMaps.BURN.mInputItemsCount);
		assertEquals(2, GT6RecipeMaps.BURN.mOutputItemsCount);
		assertEquals(0, GT6RecipeMaps.BURN.mMinimalInputItems, "FM.java:41 MIN-ITEM 0 — same shape as the FM.java:45 Engine row");
		assertEquals(1, GT6RecipeMaps.BURN.mInputFluidCount);
		assertEquals(2, GT6RecipeMaps.BURN.mOutputFluidCount);
		assertEquals(0, GT6RecipeMaps.BURN.mMinimalInputFluids, "FM.java:41 MIN-FLUID 0");
		assertEquals(1, GT6RecipeMaps.BURN.mMinimalInputs, "FM.java:41 MIN 1");
		assertEquals(1, GT6RecipeMaps.BURN.mPower);
		assertEquals(0, GT6RecipeMaps.BURN.mProgressBarDirection);
		assertEquals(1, GT6RecipeMaps.BURN.mProgressBarAmount);
		assertEquals("gt6:textures/gui/machines/default.png", GT6RecipeMaps.BURN.mGUIPath, "the FM.java:41 machines/Default row, lowercased");
	}

	/** The RM.java:70 Distillery map constants (task p14-drying-distillery-maps). */
	@Test
	void initRegistersDistilleryMapWithUpstreamConstants() {
		GT6RecipeMaps.init();
		assertNotNull(GT6RecipeMaps.DISTILLERY);
		assertSame(GT6RecipeMaps.DISTILLERY, RecipeMap.RECIPE_MAPS.get("gt.recipe.distillery"));
		assertEquals("Distillery", GT6RecipeMaps.DISTILLERY.mNameLocal);
		assertEquals("gt.recipe.distillery", GT6RecipeMaps.DISTILLERY.mNameNEI, "RM.java:70 passes null → the internal name");
		assertEquals(1, GT6RecipeMaps.DISTILLERY.mInputItemsCount);
		assertEquals(2, GT6RecipeMaps.DISTILLERY.mOutputItemsCount);
		assertEquals(1, GT6RecipeMaps.DISTILLERY.mMinimalInputItems, "RM.java:70 MIN-ITEM 1");
		assertEquals(1, GT6RecipeMaps.DISTILLERY.mInputFluidCount);
		assertEquals(2, GT6RecipeMaps.DISTILLERY.mOutputFluidCount);
		assertEquals(1, GT6RecipeMaps.DISTILLERY.mMinimalInputFluids, "RM.java:70 MIN-FLUID 1");
		assertEquals(2, GT6RecipeMaps.DISTILLERY.mMinimalInputs, "RM.java:70 MIN 2 — item AND fluid minimum 1 each, the FLUIDBED column shape");
		assertEquals(1, GT6RecipeMaps.DISTILLERY.mPower);
		assertEquals(0, GT6RecipeMaps.DISTILLERY.mProgressBarDirection);
		assertEquals(1, GT6RecipeMaps.DISTILLERY.mProgressBarAmount);
		assertEquals("gt6:textures/gui/machines/distillery.png", GT6RecipeMaps.DISTILLERY.mGUIPath, "the RM.java:70 machines/Distillery row, lowercased");
		assertTrue(GT6RecipeMaps.DISTILLERY.mRecipeList.isEmpty(), "DECLARED-empty: rows are pooled behind the circuit-selector item system");
	}

	/** The RM.java:71 Drying map constants (task p14-drying-distillery-maps). */
	@Test
	void initRegistersDryingMapWithUpstreamConstants() {
		GT6RecipeMaps.init();
		assertNotNull(GT6RecipeMaps.DRYING);
		assertSame(GT6RecipeMaps.DRYING, RecipeMap.RECIPE_MAPS.get("gt.recipe.drying"));
		assertEquals("Dryer", GT6RecipeMaps.DRYING.mNameLocal);
		assertEquals("gt.recipe.drying", GT6RecipeMaps.DRYING.mNameNEI, "RM.java:71 passes null → the internal name");
		assertEquals(1, GT6RecipeMaps.DRYING.mInputItemsCount);
		assertEquals(1, GT6RecipeMaps.DRYING.mOutputItemsCount);
		assertEquals(0, GT6RecipeMaps.DRYING.mMinimalInputItems, "RM.java:71 MIN-ITEM 0 — the Water→DistW row is item-input-free");
		assertEquals(1, GT6RecipeMaps.DRYING.mInputFluidCount);
		assertEquals(3, GT6RecipeMaps.DRYING.mOutputFluidCount);
		assertEquals(0, GT6RecipeMaps.DRYING.mMinimalInputFluids, "RM.java:71 MIN-FLUID 0");
		assertEquals(1, GT6RecipeMaps.DRYING.mMinimalInputs, "RM.java:71 MIN 1");
		assertEquals(1, GT6RecipeMaps.DRYING.mPower);
		assertEquals(0, GT6RecipeMaps.DRYING.mProgressBarDirection);
		assertEquals(1, GT6RecipeMaps.DRYING.mProgressBarAmount);
		assertEquals("gt6:textures/gui/machines/dryer.png", GT6RecipeMaps.DRYING.mGUIPath, "the RM.java:71 machines/Dryer row, lowercased");
		// the declared-empty assertion on mRecipeList lived here until the W3 pour (task
		// p14-loop-closure-chain) landed the :525 Water row — the row-level contract now
		// lives in GT6RecipesDryingTest
	}

	@Test
	void reinitIsIdempotentWithinAGeneration() {
		GT6RecipeMaps.init();
		RecipeMapFurnace tFirst = GT6RecipeMaps.FURNACE;
		RecipeMap tFirstCoke = GT6RecipeMaps.COKE_OVEN;
		RecipeMap tFirstShredder = GT6RecipeMaps.SHREDDER;
		RecipeMap tFirstCrusher = GT6RecipeMaps.CRUSHER;
		RecipeMap tFirstLathe = GT6RecipeMaps.LATHE;
		RecipeMap tFirstEngine = GT6RecipeMaps.ENGINE_FUELS;
		RecipeMap tFirstFluidBed = GT6RecipeMaps.FLUIDBED;
		RecipeMap tFirstBurn = GT6RecipeMaps.BURN;
		RecipeMap tFirstDistillery = GT6RecipeMaps.DISTILLERY;
		RecipeMap tFirstDrying = GT6RecipeMaps.DRYING;
		GT6RecipeMaps.init();
		assertSame(tFirst, GT6RecipeMaps.FURNACE, "init within one generation must not recreate (upstream :139 would throw on the duplicate name)");
		assertSame(tFirstCoke, GT6RecipeMaps.COKE_OVEN);
		assertSame(tFirstShredder, GT6RecipeMaps.SHREDDER);
		assertSame(tFirstCrusher, GT6RecipeMaps.CRUSHER);
		assertSame(tFirstLathe, GT6RecipeMaps.LATHE);
		assertSame(tFirstEngine, GT6RecipeMaps.ENGINE_FUELS);
		assertSame(tFirstFluidBed, GT6RecipeMaps.FLUIDBED);
		assertSame(tFirstBurn, GT6RecipeMaps.BURN);
		assertSame(tFirstDistillery, GT6RecipeMaps.DISTILLERY);
		assertSame(tFirstDrying, GT6RecipeMaps.DRYING);
		assertEquals(13, RecipeMap.RECIPE_MAPS.size(), "furnace + coke oven + shredder + crusher + lathe + chisel + engine fuels + fluid bed + burn + distillery + drying + canner + furnace fuel (the p13 RecipeMapFurnaceFuel append, the p14 RM.java:70/:71 pair, the p19-chisel-recipes RM.java:138 append, the p24-canner-machine RM.java:148 append)");
	}

	@Test
	void resetThenReinitProducesFreshRegistration() {
		GT6RecipeMaps.init();
		RecipeMapFurnace tFirst = GT6RecipeMaps.FURNACE;
		RecipeMap tFirstCoke = GT6RecipeMaps.COKE_OVEN;
		RecipeMap tFirstEngine = GT6RecipeMaps.ENGINE_FUELS;
		RecipeMap tFirstFluidBed = GT6RecipeMaps.FLUIDBED;
		RecipeMap tFirstBurn = GT6RecipeMaps.BURN;
		RecipeMap tFirstDistillery = GT6RecipeMaps.DISTILLERY;
		RecipeMap tFirstDrying = GT6RecipeMaps.DRYING;

		GT6RecipeMaps.reset();
		assertNull(GT6RecipeMaps.FURNACE);
		assertNull(GT6RecipeMaps.COKE_OVEN);
		assertNull(GT6RecipeMaps.ENGINE_FUELS);
		assertNull(GT6RecipeMaps.FLUIDBED, "reset drops the fluid bed entry too");
		assertNull(GT6RecipeMaps.BURN, "reset drops the burn entry too");
		assertNull(GT6RecipeMaps.DISTILLERY, "reset drops the distillery entry too");
		assertNull(GT6RecipeMaps.DRYING, "reset drops the drying entry too");
		assertFalse(RecipeMap.RECIPE_MAPS.containsKey("mc.recipe.furnace"), "reset drops the registry entry");
		assertFalse(RecipeMap.RECIPE_MAPS.containsKey("gt.recipe.cokeoven"), "reset drops the coke oven entry");
		assertFalse(RecipeMap.RECIPE_MAPS.containsKey("gt.recipe.fuels.engine"), "reset drops the engine fuels entry");
		assertFalse(RecipeMap.RECIPE_MAPS.containsKey("gt.recipe.fuels.fluidbed"), "reset drops the fluid bed entry");
		assertFalse(RecipeMap.RECIPE_MAPS.containsKey("gt.recipe.fuels.burn"), "reset drops the burn entry");
		assertFalse(RecipeMap.RECIPE_MAPS.containsKey("gt.recipe.distillery"), "reset drops the distillery entry");
		assertFalse(RecipeMap.RECIPE_MAPS.containsKey("gt.recipe.drying"), "reset drops the drying entry");

		GT6RecipeMaps.init();
		assertNotNull(GT6RecipeMaps.FURNACE);
		assertNotNull(GT6RecipeMaps.COKE_OVEN);
		assertNotNull(GT6RecipeMaps.ENGINE_FUELS);
		assertNotNull(GT6RecipeMaps.FLUIDBED);
		assertNotNull(GT6RecipeMaps.BURN);
		assertNotNull(GT6RecipeMaps.DISTILLERY);
		assertNotNull(GT6RecipeMaps.DRYING);
		assertNotSame(tFirst, GT6RecipeMaps.FURNACE, "re-init after reset creates a fresh generation");
		assertNotSame(tFirstCoke, GT6RecipeMaps.COKE_OVEN);
		assertNotSame(tFirstEngine, GT6RecipeMaps.ENGINE_FUELS);
		assertNotSame(tFirstFluidBed, GT6RecipeMaps.FLUIDBED);
		assertNotSame(tFirstBurn, GT6RecipeMaps.BURN);
		assertNotSame(tFirstDistillery, GT6RecipeMaps.DISTILLERY);
		assertNotSame(tFirstDrying, GT6RecipeMaps.DRYING);
		assertSame(GT6RecipeMaps.FURNACE, RecipeMap.RECIPE_MAPS.get("mc.recipe.furnace"));
		assertSame(GT6RecipeMaps.COKE_OVEN, RecipeMap.RECIPE_MAPS.get("gt.recipe.cokeoven"));
		assertSame(GT6RecipeMaps.ENGINE_FUELS, RecipeMap.RECIPE_MAPS.get("gt.recipe.fuels.engine"));
		assertSame(GT6RecipeMaps.FLUIDBED, RecipeMap.RECIPE_MAPS.get("gt.recipe.fuels.fluidbed"));
		assertSame(GT6RecipeMaps.BURN, RecipeMap.RECIPE_MAPS.get("gt.recipe.fuels.burn"));
		assertSame(GT6RecipeMaps.DISTILLERY, RecipeMap.RECIPE_MAPS.get("gt.recipe.distillery"));
		assertSame(GT6RecipeMaps.DRYING, RecipeMap.RECIPE_MAPS.get("gt.recipe.drying"));
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
