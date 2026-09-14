package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * P1 registry discipline for the RM.java:103 counterpart: init is idempotent
 * within a generation, reset drops the generation, re-registration after reset
 * produces a fresh map under the same name.
 */
class GT6RecipeMapsTest extends GTRecipesOfflineTestBase {

	@BeforeAll
	static void bootMaterials() {
		// the material universe must exist before any expandMaterials pour: the boot used to
		// ride the CLASS ORDER lottery (some earlier-sorting suite initialized it first) —
		// the GT6MultiBlockConverterTest insertion shifted the discovery order and exposed
		// the latent NPE (itemPathPrefixes List.of over a null DATA array) as a full-suite
		// red. Booted here explicitly, the GTEnergyTypeGuardTest @BeforeAll form.
		gregtech6.registry.GTMaterialItems.initMaterials();
	}

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
		assertEquals(60, RecipeMap.RECIPE_MAPS.size(), "furnace + coke oven + shredder + crusher + lathe + chisel + engine fuels + fluid bed + burn + distillery + drying + canner + furnace fuel + mixer + sifter + compressor + wiremill + rollingmill + press + extruder + crucible smelting + crucible alloying + bath + anvil + anvil bend (the p13 RecipeMapFurnaceFuel append, the p14 RM.java:70/:71 pair, the p19-chisel-recipes RM.java:138 append, the p24-canner-machine RM.java:148 append, the p26-c-foam-fluid-refill RM.java:74 append, the p26-w1 RM.java:83/:87/:111 trio, the p26-w1-press-extruder-molds RM.java:99/:136 pair, the p26-crucible-physics-smeltery RM.java:128/:129 pair, the p26-kitchen-pot-bowl RM.java:80 append, the p28-c-ulv-machine-ladder RM.java:113 append, the p28-c-anvil RM.java:118-120 pair with the Small/Big fold) + fermenter + loom + pressurewasher + squeezer + clustermill + rollbender + rollformer + centrifuge + sharpener + cutter + boxinator + unboxinator (the p29-w1-rm-maps-scaffold twelve-map block, RM.java:69/:89/:98/:101/:112-115/:122/:126/:130/:149-150) + sluice (the p29-w1-kinetic-process-ladder batch-C tail-append, RM.java:81 — the Sluice family's map, the card-A enumeration gap) + autocrafter + steamcracking + catalyticcracking + coagulator + cryomixer + magneticseparator + injector + laminator + autoclave + freezer + polarizer + lightning + slicer + laserengraver + welder + electrolyzer + printer + scannervisuals + generifier (the p29-w2-energy-types-5tier nineteen-map block, RM.java:63/:67/:68/:72/:77/:82/:88/:90-94/:96/:116/:117/:123/:141/:142/:151 — the W2 shared-layer card) + gas_fuels (the p29-w3-turbine-dynamo FM.java:42 append — the Gas Turbine fuel face, FM.Hot/Plasma/Turbine/Magic stay the pool bottom) + distillationtower + cryodistillationtower (the p29-w3-distill-crucible RM.java:65/:66 twin pair, the tower consumer card)");
	}

	/** The RM.java:99 Forming Press map constants (task p26-w1-press-extruder-molds). */
	@Test
	void initRegistersPressMapWithUpstreamConstants() {
		GT6RecipeMaps.init();
		assertNotNull(GT6RecipeMaps.PRESS);
		assertSame(GT6RecipeMaps.PRESS, RecipeMap.RECIPE_MAPS.get("gt.recipe.press"));
		assertEquals("Press", GT6RecipeMaps.PRESS.mNameLocal);
		assertEquals("gt.recipe.press", GT6RecipeMaps.PRESS.mNameNEI, "RM.java:99 passes null → the internal name");
		assertEquals(3, GT6RecipeMaps.PRESS.mInputItemsCount, "RM.java:99 IN-OUT-MIN-ITEM 3/1/2");
		assertEquals(1, GT6RecipeMaps.PRESS.mOutputItemsCount);
		assertEquals(2, GT6RecipeMaps.PRESS.mMinimalInputItems, "RM.java:99 MIN-ITEM 2");
		assertEquals(0, GT6RecipeMaps.PRESS.mInputFluidCount);
		assertEquals(0, GT6RecipeMaps.PRESS.mOutputFluidCount);
		assertEquals(0, GT6RecipeMaps.PRESS.mMinimalInputFluids);
		assertEquals(0, GT6RecipeMaps.PRESS.mMinimalInputs);
		assertEquals(1, GT6RecipeMaps.PRESS.mPower);
		assertEquals("gt6:textures/gui/machines/press.png", GT6RecipeMaps.PRESS.mGUIPath, "the RM.java:99 machines/Press row, lowercased");
		assertTrue(GT6RecipeMaps.PRESS instanceof gregtech6.recipes.maps.GT6RecipeMapFormingPress, "RM.Press is the RecipeMapFormingPress subclass upstream");
		assertTrue(GT6RecipeMaps.PRESS.mRecipeList.isEmpty(), "DECLARED-empty static rows: the census pools (food-mold/electrode/compat), the rows ride the dynamic arm");
	}

	/** The RM.java:136 Extruder map constants (task p26-w1-press-extruder-molds). */
	@Test
	void initRegistersExtruderMapWithUpstreamConstants() {
		GT6RecipeMaps.init();
		assertNotNull(GT6RecipeMaps.EXTRUDER);
		assertSame(GT6RecipeMaps.EXTRUDER, RecipeMap.RECIPE_MAPS.get("gt.recipe.extruder"));
		assertEquals("Extruder", GT6RecipeMaps.EXTRUDER.mNameLocal);
		assertEquals("gt.recipe.extruder", GT6RecipeMaps.EXTRUDER.mNameNEI, "RM.java:136 passes null → the internal name");
		assertEquals(2, GT6RecipeMaps.EXTRUDER.mInputItemsCount, "RM.java:136 IN-OUT-MIN-ITEM 2/2/2");
		assertEquals(2, GT6RecipeMaps.EXTRUDER.mOutputItemsCount);
		assertEquals(2, GT6RecipeMaps.EXTRUDER.mMinimalInputItems, "RM.java:136 MIN-ITEM 2");
		assertEquals(0, GT6RecipeMaps.EXTRUDER.mInputFluidCount);
		assertEquals(0, GT6RecipeMaps.EXTRUDER.mOutputFluidCount);
		assertEquals(0, GT6RecipeMaps.EXTRUDER.mMinimalInputFluids);
		assertEquals(0, GT6RecipeMaps.EXTRUDER.mMinimalInputs);
		assertEquals(1, GT6RecipeMaps.EXTRUDER.mPower);
		assertEquals("gt6:textures/gui/machines/extruder.png", GT6RecipeMaps.EXTRUDER.mGUIPath, "the RM.java:136 machines/Extruder row, lowercased");
		assertFalse(GT6RecipeMaps.EXTRUDER instanceof gregtech6.recipes.maps.GT6RecipeMapFormingPress, "RM.Extruder is the BASE map upstream — no subclass");
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
		assertNull(GT6RecipeMaps.PRESS, "reset drops the press entry too (task p26-w1-press-extruder-molds)");
		assertNull(GT6RecipeMaps.EXTRUDER, "reset drops the extruder entry too (task p26-w1-press-extruder-molds)");
		assertFalse(RecipeMap.RECIPE_MAPS.containsKey("mc.recipe.furnace"), "reset drops the registry entry");
		assertFalse(RecipeMap.RECIPE_MAPS.containsKey("gt.recipe.cokeoven"), "reset drops the coke oven entry");
		assertFalse(RecipeMap.RECIPE_MAPS.containsKey("gt.recipe.fuels.engine"), "reset drops the engine fuels entry");
		assertFalse(RecipeMap.RECIPE_MAPS.containsKey("gt.recipe.fuels.fluidbed"), "reset drops the fluid bed entry");
		assertFalse(RecipeMap.RECIPE_MAPS.containsKey("gt.recipe.fuels.burn"), "reset drops the burn entry");
		assertFalse(RecipeMap.RECIPE_MAPS.containsKey("gt.recipe.distillery"), "reset drops the distillery entry");
		assertFalse(RecipeMap.RECIPE_MAPS.containsKey("gt.recipe.drying"), "reset drops the drying entry");
		assertFalse(RecipeMap.RECIPE_MAPS.containsKey("gt.recipe.press"), "reset drops the press entry");
		assertFalse(RecipeMap.RECIPE_MAPS.containsKey("gt.recipe.extruder"), "reset drops the extruder entry");

		GT6RecipeMaps.init();
		assertNotNull(GT6RecipeMaps.FURNACE);
		assertNotNull(GT6RecipeMaps.COKE_OVEN);
		assertNotNull(GT6RecipeMaps.ENGINE_FUELS);
		assertNotNull(GT6RecipeMaps.FLUIDBED);
		assertNotNull(GT6RecipeMaps.BURN);
		assertNotNull(GT6RecipeMaps.DISTILLERY);
		assertNotNull(GT6RecipeMaps.DRYING);
		assertNotNull(GT6RecipeMaps.PRESS);
		assertNotNull(GT6RecipeMaps.EXTRUDER);
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
		assertSame(GT6RecipeMaps.PRESS, RecipeMap.RECIPE_MAPS.get("gt.recipe.press"));
		assertSame(GT6RecipeMaps.EXTRUDER, RecipeMap.RECIPE_MAPS.get("gt.recipe.extruder"));
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

	/** The RM.java:83 Sifting map constants (task p26-w1-sifter-compressor-wiremill). */
	@Test
	void initRegistersSiftingMapWithUpstreamConstants() {
		GT6RecipeMaps.init();
		assertNotNull(GT6RecipeMaps.SIFTING);
		assertSame(GT6RecipeMaps.SIFTING, RecipeMap.RECIPE_MAPS.get("gt.recipe.sifter"));
		assertEquals("Sifter", GT6RecipeMaps.SIFTING.mNameLocal);
		assertEquals("gt.recipe.sifter", GT6RecipeMaps.SIFTING.mNameNEI, "RM.java:83 passes null → the internal name");
		assertEquals(1, GT6RecipeMaps.SIFTING.mInputItemsCount);
		assertEquals(12, GT6RecipeMaps.SIFTING.mOutputItemsCount, "the RM.java:83 1/12 row — the Shredder topology");
		assertEquals(1, GT6RecipeMaps.SIFTING.mMinimalInputItems);
		assertEquals(0, GT6RecipeMaps.SIFTING.mInputFluidCount);
		assertEquals(0, GT6RecipeMaps.SIFTING.mOutputFluidCount);
		assertEquals(0, GT6RecipeMaps.SIFTING.mMinimalInputFluids);
		assertEquals(0, GT6RecipeMaps.SIFTING.mMinimalInputs);
		assertEquals(1, GT6RecipeMaps.SIFTING.mPower);
		assertEquals(2, GT6RecipeMaps.SIFTING.mProgressBarDirection, "the RM.java:83 progress direction 2 — the one non-0 direction in the RM.java:60-115 block");
		assertEquals(1, GT6RecipeMaps.SIFTING.mProgressBarAmount);
		assertEquals("gt6:textures/gui/machines/sifter.png", GT6RecipeMaps.SIFTING.mGUIPath, "the RM.java:83 machines/Sifter row, lowercased");
	}

	/** The RM.java:87 Compressor map constants (task p26-w1-sifter-compressor-wiremill). */
	@Test
	void initRegistersCompressorMapWithUpstreamConstants() {
		GT6RecipeMaps.init();
		assertNotNull(GT6RecipeMaps.COMPRESSOR);
		assertSame(GT6RecipeMaps.COMPRESSOR, RecipeMap.RECIPE_MAPS.get("gt.recipe.compressor"));
		assertEquals("Compressor", GT6RecipeMaps.COMPRESSOR.mNameLocal);
		assertEquals("gt.recipe.compressor", GT6RecipeMaps.COMPRESSOR.mNameNEI, "RM.java:87 passes null → the internal name");
		assertEquals(1, GT6RecipeMaps.COMPRESSOR.mInputItemsCount);
		assertEquals(1, GT6RecipeMaps.COMPRESSOR.mOutputItemsCount, "the RM.java:87 1/1/1 row");
		assertEquals(1, GT6RecipeMaps.COMPRESSOR.mMinimalInputItems);
		assertEquals(0, GT6RecipeMaps.COMPRESSOR.mInputFluidCount);
		assertEquals(0, GT6RecipeMaps.COMPRESSOR.mOutputFluidCount);
		assertEquals(0, GT6RecipeMaps.COMPRESSOR.mMinimalInputFluids);
		assertEquals(0, GT6RecipeMaps.COMPRESSOR.mMinimalInputs);
		assertEquals(1, GT6RecipeMaps.COMPRESSOR.mPower);
		assertEquals(0, GT6RecipeMaps.COMPRESSOR.mProgressBarDirection);
		assertEquals(1, GT6RecipeMaps.COMPRESSOR.mProgressBarAmount);
		assertEquals("gt6:textures/gui/machines/compressor.png", GT6RecipeMaps.COMPRESSOR.mGUIPath, "the RM.java:87 machines/Compressor row, lowercased");
	}

	/** The RM.java:111 Wiremill map constants (task p26-w1-sifter-compressor-wiremill). */
	@Test
	void initRegistersWiremillMapWithUpstreamConstants() {
		GT6RecipeMaps.init();
		assertNotNull(GT6RecipeMaps.WIREMILL);
		assertSame(GT6RecipeMaps.WIREMILL, RecipeMap.RECIPE_MAPS.get("gt.recipe.wiremill"));
		assertEquals("Wiremill", GT6RecipeMaps.WIREMILL.mNameLocal);
		assertEquals("gt.recipe.wiremill", GT6RecipeMaps.WIREMILL.mNameNEI, "RM.java:111 passes null → the internal name");
		assertEquals(1, GT6RecipeMaps.WIREMILL.mInputItemsCount);
		assertEquals(1, GT6RecipeMaps.WIREMILL.mOutputItemsCount, "the RM.java:111 1/1/1 row");
		assertEquals(1, GT6RecipeMaps.WIREMILL.mMinimalInputItems);
		assertEquals(0, GT6RecipeMaps.WIREMILL.mInputFluidCount);
		assertEquals(0, GT6RecipeMaps.WIREMILL.mOutputFluidCount);
		assertEquals(0, GT6RecipeMaps.WIREMILL.mMinimalInputFluids);
		assertEquals(0, GT6RecipeMaps.WIREMILL.mMinimalInputs);
		assertEquals(1, GT6RecipeMaps.WIREMILL.mPower);
		assertEquals(0, GT6RecipeMaps.WIREMILL.mProgressBarDirection);
		assertEquals(1, GT6RecipeMaps.WIREMILL.mProgressBarAmount);
		assertEquals("gt6:textures/gui/machines/wiremill.png", GT6RecipeMaps.WIREMILL.mGUIPath, "the RM.java:111 machines/Wiremill row, lowercased");
	}

	/**
	 * The p26 trio joins the generation lifecycle: reset nulls the fields, drops the
	 * registry names, and the three loader pour flags retire WITH the generation (the
	 * ADR-P18 hook ledger — the loaders' static initializers registered their reset hooks).
	 * The retirement is observed BEHAVIORALLY (a post-reset load() must re-pour, the
	 * {@code oreChainLoadRepoursAfterABareMapReset} shape) — NOT by hook-count growth,
	 * which is order-dependent: a sibling test class in the same JVM may have class-loaded
	 * the trio first, so the sampled baseline would already carry the three hooks.
	 */
	@Test
	void resetRetiresTheKineticTrioGeneration() throws Exception {
		// class-load the three loaders so their static-initializer hooks join the ledger
		// (idempotent; the structural membership pin lives in GT6RecipeGenerationGuardTest)
		Class.forName("gregtech6.recipes.GT6RecipesSifter");
		Class.forName("gregtech6.recipes.GT6RecipesCompressor");
		Class.forName("gregtech6.recipes.GT6RecipesWiremill");
		GT6RecipeMaps.init();
		RecipeMap tFirstSifting = GT6RecipeMaps.SIFTING;
		RecipeMap tFirstCompressor = GT6RecipeMaps.COMPRESSOR;
		RecipeMap tFirstWiremill = GT6RecipeMaps.WIREMILL;
		assertNotNull(tFirstSifting);
		assertNotNull(tFirstCompressor);
		assertNotNull(tFirstWiremill);
		GT6RecipeMaps.reset();
		assertNull(GT6RecipeMaps.SIFTING, "reset drops the sifting entry too");
		assertNull(GT6RecipeMaps.COMPRESSOR, "reset drops the compressor entry too");
		assertNull(GT6RecipeMaps.WIREMILL, "reset drops the wiremill entry too");
		assertFalse(RecipeMap.RECIPE_MAPS.containsKey("gt.recipe.sifter"));
		assertFalse(RecipeMap.RECIPE_MAPS.containsKey("gt.recipe.compressor"));
		assertFalse(RecipeMap.RECIPE_MAPS.containsKey("gt.recipe.wiremill"));
		// the poison-state discriminator: a stuck (non-retired) flag makes load() early-return
		// BEFORE init() — the maps would stay null. The sifter additionally re-pours the
		// :224 grass row0 over the vanilla resolver (the GT6KineticRecipesPourTest pour).
		GT6RecipesSifter.load();
		assertNotNull(GT6RecipeMaps.SIFTING,
				"the sifter pour-flag retired WITH the generation — load() re-inits after a bare reset (ADR-P18)");
		assertTrue(GT6RecipeMaps.SIFTING.mRecipeList.size() >= 1,
				"the :224 grass row0 re-pours after the bare reset");
		GT6RecipesCompressor.load();
		assertNotNull(GT6RecipeMaps.COMPRESSOR, "the compressor pour-flag retired WITH the generation (ADR-P18)");
		GT6RecipesWiremill.load();
		assertNotNull(GT6RecipeMaps.WIREMILL, "the wiremill pour-flag retired WITH the generation (ADR-P18)");
		GT6RecipeMaps.init();
		assertNotSame(tFirstSifting, GT6RecipeMaps.SIFTING, "re-init after reset creates a fresh generation");
		assertNotSame(tFirstCompressor, GT6RecipeMaps.COMPRESSOR);
		assertNotSame(tFirstWiremill, GT6RecipeMaps.WIREMILL);
		GT6RecipeMaps.reset();
	}

	// -----------------------------------------------------------------------
	// the P29 W1 twelve-map block (task p29-w1-rm-maps-scaffold) — the RM.java
	// :69/:89/:98/:101/:112-115/:122/:126/:130/:149-150 transcription pins, all
	// base-RecipeMap, all DECLARED-empty (the row pour is the B/C/D card content).
	// -----------------------------------------------------------------------

	/** The fluid-bearing six of the block: Fermenter :69, PressureWasher :98, Squeezer :101, Centrifuge :122, Cutter :130. */
	@Test
	void initRegistersP29FluidMapsWithUpstreamConstants() {
		GT6RecipeMaps.init();

		assertNotNull(GT6RecipeMaps.FERMENTER);
		assertSame(GT6RecipeMaps.FERMENTER, RecipeMap.RECIPE_MAPS.get("gt.recipe.fermenter"));
		assertEquals("Fermenter", GT6RecipeMaps.FERMENTER.mNameLocal);
		assertEquals(1, GT6RecipeMaps.FERMENTER.mInputItemsCount);
		assertEquals(1, GT6RecipeMaps.FERMENTER.mOutputItemsCount);
		assertEquals(1, GT6RecipeMaps.FERMENTER.mMinimalInputItems, "RM.java:69 MIN-ITEM 1");
		assertEquals(1, GT6RecipeMaps.FERMENTER.mInputFluidCount);
		assertEquals(1, GT6RecipeMaps.FERMENTER.mOutputFluidCount);
		assertEquals(0, GT6RecipeMaps.FERMENTER.mMinimalInputFluids, "RM.java:69 MIN-FLUID 0");
		assertEquals(1, GT6RecipeMaps.FERMENTER.mMinimalInputs, "RM.java:69 MIN 1");
		assertEquals(1, GT6RecipeMaps.FERMENTER.mPower);
		assertEquals("gt6:textures/gui/machines/fermenter.png", GT6RecipeMaps.FERMENTER.mGUIPath);
		assertTrue(GT6RecipeMaps.FERMENTER.mRecipeList.isEmpty(), "DECLARED-empty");

		assertNotNull(GT6RecipeMaps.PRESSURE_WASHER);
		assertSame(GT6RecipeMaps.PRESSURE_WASHER, RecipeMap.RECIPE_MAPS.get("gt.recipe.pressurewasher"));
		assertEquals("Pressure Washer", GT6RecipeMaps.PRESSURE_WASHER.mNameLocal);
		assertEquals(1, GT6RecipeMaps.PRESSURE_WASHER.mInputItemsCount);
		assertEquals(2, GT6RecipeMaps.PRESSURE_WASHER.mOutputItemsCount);
		assertEquals(1, GT6RecipeMaps.PRESSURE_WASHER.mMinimalInputItems, "RM.java:98 MIN-ITEM 1");
		assertEquals(1, GT6RecipeMaps.PRESSURE_WASHER.mInputFluidCount, "RM.java:98 IN-FLUID 1 — the water-consumption leg (no output)");
		assertEquals(0, GT6RecipeMaps.PRESSURE_WASHER.mOutputFluidCount);
		assertEquals(1, GT6RecipeMaps.PRESSURE_WASHER.mMinimalInputFluids, "RM.java:98 MIN-FLUID 1");
		assertEquals(0, GT6RecipeMaps.PRESSURE_WASHER.mMinimalInputs);
		assertEquals("gt6:textures/gui/machines/pressurewasher.png", GT6RecipeMaps.PRESSURE_WASHER.mGUIPath);
		assertTrue(GT6RecipeMaps.PRESSURE_WASHER.mRecipeList.isEmpty(), "DECLARED-empty");

		assertNotNull(GT6RecipeMaps.SQUEEZER);
		assertSame(GT6RecipeMaps.SQUEEZER, RecipeMap.RECIPE_MAPS.get("gt.recipe.squeezer"));
		assertEquals("Squeezer", GT6RecipeMaps.SQUEEZER.mNameLocal);
		assertEquals(1, GT6RecipeMaps.SQUEEZER.mInputItemsCount);
		assertEquals(2, GT6RecipeMaps.SQUEEZER.mOutputItemsCount);
		assertEquals(1, GT6RecipeMaps.SQUEEZER.mMinimalInputItems, "RM.java:101 MIN-ITEM 1");
		assertEquals(0, GT6RecipeMaps.SQUEEZER.mInputFluidCount);
		assertEquals(1, GT6RecipeMaps.SQUEEZER.mOutputFluidCount, "RM.java:101 OUT-FLUID 1 — the juice leg");
		assertEquals(0, GT6RecipeMaps.SQUEEZER.mMinimalInputFluids);
		assertEquals(0, GT6RecipeMaps.SQUEEZER.mMinimalInputs);
		assertEquals("gt6:textures/gui/machines/squeezer.png", GT6RecipeMaps.SQUEEZER.mGUIPath);
		assertTrue(GT6RecipeMaps.SQUEEZER.mRecipeList.isEmpty(), "DECLARED-empty");

		assertNotNull(GT6RecipeMaps.CENTRIFUGE);
		assertSame(GT6RecipeMaps.CENTRIFUGE, RecipeMap.RECIPE_MAPS.get("gt.recipe.centrifuge"));
		assertEquals("Centrifuge", GT6RecipeMaps.CENTRIFUGE.mNameLocal);
		assertEquals(1, GT6RecipeMaps.CENTRIFUGE.mInputItemsCount);
		assertEquals(6, GT6RecipeMaps.CENTRIFUGE.mOutputItemsCount, "RM.java:122 OUT-ITEM 6 — the six-output topology");
		assertEquals(0, GT6RecipeMaps.CENTRIFUGE.mMinimalInputItems);
		assertEquals(1, GT6RecipeMaps.CENTRIFUGE.mInputFluidCount);
		assertEquals(6, GT6RecipeMaps.CENTRIFUGE.mOutputFluidCount, "RM.java:122 OUT-FLUID 6");
		assertEquals(0, GT6RecipeMaps.CENTRIFUGE.mMinimalInputFluids);
		assertEquals(0, GT6RecipeMaps.CENTRIFUGE.mMinimalInputs);
		assertEquals("gt6:textures/gui/machines/centrifuge.png", GT6RecipeMaps.CENTRIFUGE.mGUIPath);
		assertTrue(GT6RecipeMaps.CENTRIFUGE.mRecipeList.isEmpty(), "DECLARED-empty");

		assertNotNull(GT6RecipeMaps.CUTTER);
		assertSame(GT6RecipeMaps.CUTTER, RecipeMap.RECIPE_MAPS.get("gt.recipe.cutter"));
		assertEquals("Cutter", GT6RecipeMaps.CUTTER.mNameLocal);
		assertEquals(1, GT6RecipeMaps.CUTTER.mInputItemsCount);
		assertEquals(3, GT6RecipeMaps.CUTTER.mOutputItemsCount, "RM.java:130 OUT-ITEM 3");
		assertEquals(1, GT6RecipeMaps.CUTTER.mMinimalInputItems, "RM.java:130 MIN-ITEM 1");
		assertEquals(1, GT6RecipeMaps.CUTTER.mInputFluidCount, "RM.java:130 IN-FLUID 1 — the coolant leg");
		assertEquals(0, GT6RecipeMaps.CUTTER.mOutputFluidCount);
		assertEquals(1, GT6RecipeMaps.CUTTER.mMinimalInputFluids, "RM.java:130 MIN-FLUID 1");
		assertEquals(0, GT6RecipeMaps.CUTTER.mMinimalInputs);
		assertEquals("gt6:textures/gui/machines/cutter.png", GT6RecipeMaps.CUTTER.mGUIPath);
		assertTrue(GT6RecipeMaps.CUTTER instanceof RecipeMap, "RM.Cutter IS the base RecipeMap upstream (:130 — the gregapi RecipeMapCutter class is not this field's type)");
		assertTrue(GT6RecipeMaps.CUTTER.mRecipeList.isEmpty(), "DECLARED-empty");
	}

	/** The item-only five of the block: Loom :89, ClusterMill :112, RollBender :114, RollFormer :115, Sharpening :126. */
	@Test
	void initRegistersP29ItemOnlyMapsWithUpstreamConstants() {
		GT6RecipeMaps.init();

		assertNotNull(GT6RecipeMaps.LOOM);
		assertSame(GT6RecipeMaps.LOOM, RecipeMap.RECIPE_MAPS.get("gt.recipe.loom"));
		assertEquals("Loom", GT6RecipeMaps.LOOM.mNameLocal);
		assertEquals(6, GT6RecipeMaps.LOOM.mInputItemsCount, "RM.java:89 IN-ITEM 6 — the six-string topology");
		assertEquals(1, GT6RecipeMaps.LOOM.mOutputItemsCount);
		assertEquals(1, GT6RecipeMaps.LOOM.mMinimalInputItems, "RM.java:89 MIN-ITEM 1");
		assertEquals(0, GT6RecipeMaps.LOOM.mInputFluidCount);
		assertEquals(0, GT6RecipeMaps.LOOM.mOutputFluidCount);
		assertEquals(0, GT6RecipeMaps.LOOM.mMinimalInputs);
		assertEquals("gt6:textures/gui/machines/loom.png", GT6RecipeMaps.LOOM.mGUIPath);
		assertTrue(GT6RecipeMaps.LOOM.mRecipeList.isEmpty(), "DECLARED-empty");

		// the mill trio — three RM.java rows with IDENTICAL constants (1/1/1, 0/0/0, MIN 0,
		// AMP 1), split only by map name and GUI path (RollingMill :113 is the already-landed
		// fourth of the shape)
		String[][] tMills = {
			{"CLUSTER_MILL", "gt.recipe.clustermill", "Cluster Mill", "clustermill", "RM.java:112"},
			{"ROLL_BENDER", "gt.recipe.rollbender", "Roll Bender", "rollbender", "RM.java:114"},
			{"ROLL_FORMER", "gt.recipe.rollformer", "Roll Former", "rollformer", "RM.java:115"}
		};
		for (String[] tMill : tMills) {
			RecipeMap tMap = RecipeMap.RECIPE_MAPS.get(tMill[1]);
			assertNotNull(tMap, tMill[4] + " registered");
			assertSame(field(tMill[0]), tMap, tMill[4] + " field wiring");
			assertEquals(tMill[2], tMap.mNameLocal, tMill[4] + " local name");
			assertEquals(1, tMap.mInputItemsCount, tMill[4] + " IN-ITEM 1");
			assertEquals(1, tMap.mOutputItemsCount, tMill[4] + " OUT-ITEM 1");
			assertEquals(1, tMap.mMinimalInputItems, tMill[4] + " MIN-ITEM 1");
			assertEquals(0, tMap.mInputFluidCount, tMill[4] + " fluids 0/0/0");
			assertEquals(0, tMap.mOutputFluidCount, tMill[4] + " fluids 0/0/0");
			assertEquals(0, tMap.mMinimalInputs, tMill[4] + " MIN 0");
			assertEquals(1, tMap.mPower, tMill[4] + " AMP 1");
			assertEquals("gt6:textures/gui/machines/" + tMill[3] + ".png", tMap.mGUIPath, tMill[4] + " GUI path lowercased");
			assertTrue(tMap.mRecipeList.isEmpty(), tMill[4] + " DECLARED-empty");
		}

		assertNotNull(GT6RecipeMaps.SHARPENING);
		assertSame(GT6RecipeMaps.SHARPENING, RecipeMap.RECIPE_MAPS.get("gt.recipe.sharpener"));
		assertEquals("Sharpener", GT6RecipeMaps.SHARPENING.mNameLocal, "RM.java:126 — the upstream GUI/local word is Sharpener, NOT Sharpening");
		assertEquals(1, GT6RecipeMaps.SHARPENING.mInputItemsCount);
		assertEquals(2, GT6RecipeMaps.SHARPENING.mOutputItemsCount, "RM.java:126 OUT-ITEM 2");
		assertEquals(1, GT6RecipeMaps.SHARPENING.mMinimalInputItems, "RM.java:126 MIN-ITEM 1");
		assertEquals(0, GT6RecipeMaps.SHARPENING.mInputFluidCount);
		assertEquals(0, GT6RecipeMaps.SHARPENING.mOutputFluidCount);
		assertEquals(0, GT6RecipeMaps.SHARPENING.mMinimalInputs);
		assertEquals("gt6:textures/gui/machines/sharpener.png", GT6RecipeMaps.SHARPENING.mGUIPath, "the upstream machines/Sharpener row, lowercased");
		assertTrue(GT6RecipeMaps.SHARPENING.mRecipeList.isEmpty(), "DECLARED-empty");
	}

	/** The boxinator pair: Boxinator :149 (base class) and Unboxinator :150 (the declared base-form deviation). */
	@Test
	void initRegistersP29BoxinatorPairWithUpstreamConstants() {
		GT6RecipeMaps.init();

		assertNotNull(GT6RecipeMaps.BOXINATOR);
		assertSame(GT6RecipeMaps.BOXINATOR, RecipeMap.RECIPE_MAPS.get("gt.recipe.boxinator"));
		assertEquals("Boxinator", GT6RecipeMaps.BOXINATOR.mNameLocal);
		assertEquals(2, GT6RecipeMaps.BOXINATOR.mInputItemsCount, "RM.java:149 IN-ITEM 2");
		assertEquals(1, GT6RecipeMaps.BOXINATOR.mOutputItemsCount);
		assertEquals(2, GT6RecipeMaps.BOXINATOR.mMinimalInputItems, "RM.java:149 MIN-ITEM 2");
		assertEquals(0, GT6RecipeMaps.BOXINATOR.mInputFluidCount);
		assertEquals(0, GT6RecipeMaps.BOXINATOR.mOutputFluidCount);
		assertEquals(0, GT6RecipeMaps.BOXINATOR.mMinimalInputs);
		assertEquals("gt6:textures/gui/machines/boxinator.png", GT6RecipeMaps.BOXINATOR.mGUIPath);
		assertTrue(GT6RecipeMaps.BOXINATOR.mRecipeList.isEmpty(), "DECLARED-empty");

		assertNotNull(GT6RecipeMaps.UNBOXINATOR);
		assertSame(GT6RecipeMaps.UNBOXINATOR, RecipeMap.RECIPE_MAPS.get("gt.recipe.unboxinator"));
		assertEquals("Unboxinator", GT6RecipeMaps.UNBOXINATOR.mNameLocal);
		assertEquals(1, GT6RecipeMaps.UNBOXINATOR.mInputItemsCount);
		assertEquals(12, GT6RecipeMaps.UNBOXINATOR.mOutputItemsCount, "RM.java:150 OUT-ITEM 12 — the Shredder/Sifting topology");
		assertEquals(1, GT6RecipeMaps.UNBOXINATOR.mMinimalInputItems, "RM.java:150 MIN-ITEM 1");
		assertEquals(0, GT6RecipeMaps.UNBOXINATOR.mInputFluidCount);
		assertEquals(0, GT6RecipeMaps.UNBOXINATOR.mOutputFluidCount);
		assertEquals(0, GT6RecipeMaps.UNBOXINATOR.mMinimalInputs);
		assertEquals("gt6:textures/gui/machines/unboxinator.png", GT6RecipeMaps.UNBOXINATOR.mGUIPath);
		assertTrue(GT6RecipeMaps.UNBOXINATOR instanceof RecipeMap && !(GT6RecipeMaps.UNBOXINATOR instanceof RecipeMapFurnace),
				"the declared base-form deviation: the upstream RecipeMapUnboxinator loot arms (RecipeMapUnboxinator.java:43-89) stay POOLED");
		assertTrue(GT6RecipeMaps.UNBOXINATOR.mRecipeList.isEmpty(), "DECLARED-empty — no static row upstream either; a loot-box input finds NO row (the batch-D rejection shape)");
	}

	/** The P29 block joins the generation lifecycle: reset nulls all twelve fields (+ the batch-C sluice tail) and drops the registry names. */
	@Test
	void resetRetiresTheP29TwelveMapGeneration() {
		String[] tNames = {"gt.recipe.fermenter", "gt.recipe.loom", "gt.recipe.pressurewasher", "gt.recipe.squeezer",
				"gt.recipe.clustermill", "gt.recipe.rollbender", "gt.recipe.rollformer", "gt.recipe.centrifuge",
				"gt.recipe.sharpener", "gt.recipe.cutter", "gt.recipe.boxinator", "gt.recipe.unboxinator",
				"gt.recipe.sluice"}; // the batch-C tail-append (task p29-w1-kinetic-process-ladder)
		GT6RecipeMaps.init();
		RecipeMap tFirstCutter = GT6RecipeMaps.CUTTER;
		assertNotNull(tFirstCutter);
		GT6RecipeMaps.reset();
		assertNull(GT6RecipeMaps.FERMENTER, "reset drops the fermenter entry too");
		assertNull(GT6RecipeMaps.LOOM);
		assertNull(GT6RecipeMaps.PRESSURE_WASHER);
		assertNull(GT6RecipeMaps.SQUEEZER);
		assertNull(GT6RecipeMaps.CLUSTER_MILL);
		assertNull(GT6RecipeMaps.ROLL_BENDER);
		assertNull(GT6RecipeMaps.ROLL_FORMER);
		assertNull(GT6RecipeMaps.CENTRIFUGE);
		assertNull(GT6RecipeMaps.SHARPENING);
		assertNull(GT6RecipeMaps.CUTTER);
		assertNull(GT6RecipeMaps.BOXINATOR);
		assertNull(GT6RecipeMaps.UNBOXINATOR);
		assertNull(GT6RecipeMaps.SLUICE);
		for (String tName : tNames) assertFalse(RecipeMap.RECIPE_MAPS.containsKey(tName), "reset drops " + tName);
		GT6RecipeMaps.init();
		assertNotSame(tFirstCutter, GT6RecipeMaps.CUTTER, "re-init after reset creates a fresh generation");
		for (String tName : tNames) assertTrue(RecipeMap.RECIPE_MAPS.containsKey(tName), "re-init re-registers " + tName);
		GT6RecipeMaps.reset();
	}

	/** The batch-C tail-append: Sluice :81 (base class, the fluid-mandatory dual-leg gate). */
	@Test
	void initRegistersSluiceMapWithUpstreamConstants() {
		GT6RecipeMaps.init();

		assertNotNull(GT6RecipeMaps.SLUICE);
		assertSame(GT6RecipeMaps.SLUICE, RecipeMap.RECIPE_MAPS.get("gt.recipe.sluice"));
		assertEquals("Sluice", GT6RecipeMaps.SLUICE.mNameLocal);
		assertEquals(1, GT6RecipeMaps.SLUICE.mInputItemsCount);
		assertEquals(9, GT6RecipeMaps.SLUICE.mOutputItemsCount, "RM.java:81 OUT-ITEM 9 — the Sluice topology");
		assertEquals(1, GT6RecipeMaps.SLUICE.mMinimalInputItems, "RM.java:81 MIN-ITEM 1");
		assertEquals(1, GT6RecipeMaps.SLUICE.mInputFluidCount, "RM.java:81 IN-FLUID 1 — the flowing-water leg lives in the RECIPE domain (the machine carries no world interaction)");
		assertEquals(1, GT6RecipeMaps.SLUICE.mOutputFluidCount, "RM.java:81 OUT-FLUID 1");
		assertEquals(1, GT6RecipeMaps.SLUICE.mMinimalInputFluids, "RM.java:81 MIN-FLUID 1");
		assertEquals(2, GT6RecipeMaps.SLUICE.mMinimalInputs, "RM.java:81 MIN 2 — every row needs BOTH legs (the :708/:709/:710 gate trio)");
		assertEquals(1, GT6RecipeMaps.SLUICE.mPower);
		assertEquals("gt6:textures/gui/machines/sluice.png", GT6RecipeMaps.SLUICE.mGUIPath);
		assertTrue(GT6RecipeMaps.SLUICE instanceof RecipeMap && !(GT6RecipeMaps.SLUICE instanceof RecipeMapFurnace), "the base RecipeMap form");
		assertTrue(GT6RecipeMaps.SLUICE.mRecipeList.isEmpty(), "DECLARED-empty at the scaffold");
	}

	// -----------------------------------------------------------------------
	// the P29 W2 nineteen-map block (task p29-w2-energy-types-5tier) — the RM.java
	// :63/:67/:68/:72/:77/:82/:88/:90-94/:96/:116/:117/:123/:141/:142/:151 transcription
	// pins, all base-RecipeMap, all DECLARED-empty. The table mirrors the upstream rows
	// column-for-column: internal name, local name, GUI word (lowercased), items
	// in/out/min, fluids in/out/min, MIN, and the upstream RM.java line for the message.
	// -----------------------------------------------------------------------

	/** The nineteen rows: {field, internal, local, gui, itemIn, itemOut, itemMin, fluidIn, fluidOut, fluidMin, MIN, rmLine}. */
	private static final String[][] W2_MAPS = {
		{"AUTOCRAFTER", "gt.recipe.autocrafting", "Crafting", "crafting", "9", "12", "1", "0", "0", "0", "1", "RM.java:63"},
		{"STEAM_CRACKING", "gt.recipe.steamcracking", "Steam Cracking", "steamcracking", "1", "3", "0", "2", "9", "1", "2", "RM.java:67"},
		{"CATALYTIC_CRACKING", "gt.recipe.catalyticcracking", "Catalytic Cracking", "catalyticcracking", "1", "3", "0", "2", "9", "1", "2", "RM.java:68"},
		{"COAGULATOR", "gt.recipe.coagulator", "Coagulator", "coagulator", "0", "1", "0", "1", "0", "1", "0", "RM.java:72"},
		{"CRYO_MIXER", "gt.recipe.cryomixer", "Cryo Mixer", "cryomixer", "6", "1", "0", "6", "2", "0", "2", "RM.java:77"},
		{"MAGNETIC_SEPARATOR", "gt.recipe.magneticseparator", "Magnetic Separator", "magneticseparator", "1", "6", "0", "1", "6", "0", "1", "RM.java:82"},
		{"INJECTOR", "gt.recipe.injector", "Injector", "injector", "2", "1", "0", "2", "1", "0", "2", "RM.java:88"},
		{"LAMINATOR", "gt.recipe.laminator", "Laminator", "laminator", "2", "1", "2", "0", "0", "0", "2", "RM.java:90"},
		{"AUTOCLAVE", "gt.recipe.autoclave", "Autoclave", "autoclave", "2", "3", "2", "1", "1", "1", "0", "RM.java:91"},
		{"FREEZER", "gt.recipe.freezer", "Freezer", "freezer", "1", "1", "1", "1", "1", "0", "1", "RM.java:92"},
		{"POLARIZER", "gt.recipe.polarizer", "Polarizer", "polarizer", "1", "1", "1", "0", "0", "0", "0", "RM.java:93"},
		{"LIGHTNING", "gt.recipe.lightning", "Lightning Processor", "lightning", "6", "6", "0", "6", "6", "0", "2", "RM.java:94"},
		{"SLICER", "gt.recipe.slicer", "Slicer", "slicer", "2", "2", "2", "0", "0", "0", "2", "RM.java:96"},
		{"LASER_ENGRAVER", "gt.recipe.laserengraver", "Precision Laser Engraver", "laserengraver", "2", "1", "2", "0", "0", "0", "2", "RM.java:116"},
		{"WELDER", "gt.recipe.welder", "Welding Machine", "welder", "9", "1", "2", "1", "0", "0", "2", "RM.java:117"},
		{"ELECTROLYZER", "gt.recipe.electrolyzer", "Electrolyzer", "electrolyzer", "2", "6", "1", "2", "6", "0", "2", "RM.java:123"},
		{"PRINTER", "gt.recipe.printer", "Printer", "printer", "2", "1", "1", "6", "0", "1", "2", "RM.java:141"},
		{"SCANNER_VISUALS", "gt.recipe.scannervisuals", "Scanner (Visuals)", "scannervisuals", "2", "2", "2", "0", "0", "0", "2", "RM.java:142"},
		{"GENERIFIER", "gt.recipe.generifier", "Generifier", "generifier", "1", "1", "0", "1", "1", "0", "1", "RM.java:151"}
	};

	/** The whole block: every row transcribed column-for-column over the 15-arg port ctor (progress 0/1, AMP 1 everywhere), every map DECLARED-empty, every map the BASE class. */
	@Test
	void initRegistersP29W2NineteenMapsWithUpstreamConstants() {
		GT6RecipeMaps.init();
		for (String[] tRow : W2_MAPS) {
			RecipeMap tMap = field(tRow[0]);
			assertNotNull(tMap, tRow[11] + " " + tRow[0] + " registered");
			assertSame(tMap, RecipeMap.RECIPE_MAPS.get(tRow[1]), tRow[11] + " " + tRow[0] + " registry wiring");
			assertEquals(tRow[2], tMap.mNameLocal, tRow[11] + " local name");
			assertEquals(tRow[1], tMap.mNameNEI, tRow[11] + " NEI name null → the internal name");
			assertEquals(0, tMap.mProgressBarDirection, tRow[11] + " progress direction 0");
			assertEquals(1, tMap.mProgressBarAmount, tRow[11] + " progress amount 1");
			assertEquals(Integer.parseInt(tRow[4]), tMap.mInputItemsCount, tRow[11] + " IN-ITEM");
			assertEquals(Integer.parseInt(tRow[5]), tMap.mOutputItemsCount, tRow[11] + " OUT-ITEM");
			assertEquals(Integer.parseInt(tRow[6]), tMap.mMinimalInputItems, tRow[11] + " MIN-ITEM");
			assertEquals(Integer.parseInt(tRow[7]), tMap.mInputFluidCount, tRow[11] + " IN-FLUID");
			assertEquals(Integer.parseInt(tRow[8]), tMap.mOutputFluidCount, tRow[11] + " OUT-FLUID");
			assertEquals(Integer.parseInt(tRow[9]), tMap.mMinimalInputFluids, tRow[11] + " MIN-FLUID");
			assertEquals(Integer.parseInt(tRow[10]), tMap.mMinimalInputs, tRow[11] + " MIN");
			assertEquals(1, tMap.mPower, tRow[11] + " AMP 1");
			assertEquals("gt6:textures/gui/machines/" + tRow[3] + ".png", tMap.mGUIPath, tRow[11] + " GUI path lowercased");
			assertTrue(tMap.mRecipeList.isEmpty(), tRow[11] + " DECLARED-empty");
			assertEquals(RecipeMap.class, tMap.getClass(), tRow[11] + " the base-RecipeMap form (the subclass arms stay pooled)");
		}
		// the one row where the base-form deviation is NAMED upstream — the AUTOCRAFTER
		// local/GUI word "Crafting" pins the SHARPENING/Sharpener GUI-word convention
		assertEquals("Crafting", GT6RecipeMaps.AUTOCRAFTER.mNameLocal, "RM.java:63 — the upstream word IS Crafting, not Autocrafter");
		// the STEAM/CATALYTIC cracking pair: identical constants rows, split only by name/GUI
		assertEquals(GT6RecipeMaps.STEAM_CRACKING.mInputItemsCount, GT6RecipeMaps.CATALYTIC_CRACKING.mInputItemsCount);
		assertEquals(GT6RecipeMaps.STEAM_CRACKING.mOutputFluidCount, GT6RecipeMaps.CATALYTIC_CRACKING.mOutputFluidCount);
		assertEquals(GT6RecipeMaps.STEAM_CRACKING.mMinimalInputs, GT6RecipeMaps.CATALYTIC_CRACKING.mMinimalInputs);
		assertNotSame(GT6RecipeMaps.STEAM_CRACKING, GT6RecipeMaps.CATALYTIC_CRACKING);
	}

	/** The W2 block joins the generation lifecycle: reset nulls all nineteen fields and drops the registry names; a fresh init re-registers fresh instances. */
	@Test
	void resetRetiresTheP29W2NineteenMapGeneration() {
		GT6RecipeMaps.init();
		RecipeMap tFirstElectrolyzer = GT6RecipeMaps.ELECTROLYZER;
		assertNotNull(tFirstElectrolyzer);
		GT6RecipeMaps.reset();
		for (String[] tRow : W2_MAPS) {
			assertNull(field(tRow[0]), "reset drops " + tRow[0]);
			assertFalse(RecipeMap.RECIPE_MAPS.containsKey(tRow[1]), "reset drops " + tRow[1]);
		}
		GT6RecipeMaps.init();
		assertNotSame(tFirstElectrolyzer, GT6RecipeMaps.ELECTROLYZER, "re-init after reset creates a fresh generation");
		for (String[] tRow : W2_MAPS) {
			assertTrue(RecipeMap.RECIPE_MAPS.containsKey(tRow[1]), "re-init re-registers " + tRow[1]);
		}
		GT6RecipeMaps.reset();
	}

	/** Reflection accessor for the twelve P29 fields (the mill-trio loop above). */
	private static RecipeMap field(String aName) {
		try {
			return (RecipeMap)GT6RecipeMaps.class.getField(aName).get(null);
		} catch (ReflectiveOperationException tError) {
			throw new AssertionError(tError);
		}
	}
}
