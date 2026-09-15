package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import gregtech6.registry.GT6Distillation;

/**
 * The p29-w4-f1-chemicals true-row pour test (the GT6HeatSmelterSmokeRowsPourTest fixture
 * posture): the FOUR data/gt6/recipe_maps JSON files this card ships (steamcracking /
 * catalyticcracking / gas_fuels / burn) are read VERBATIM off the classpath and poured
 * through the real {@link GT6RecipeMapJsonLoader} seam — the smoke rows are GONE, the
 * upstream true rows ride (Loader_Recipes_Chem.java:368-376, Loader_Fuels.java:160-189).
 * Every gt6 fluid id resolves through the injected resolver seam onto a vanilla stand-in
 * (the synthetic-universe convention — the recipe mechanics compare identity only); the
 * resolver RECORDS the requested paths so the id faces assert without a live registry.
 */
public class GT6ChemicalRowsPourTest extends GTRecipesOfflineTestBase {

	private static final Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

	/** The fluid ids the loader asked for during the pour (the id-face assertion set). */
	private final Set<String> mRequestedFluidPaths = new HashSet<>();
	/** The item ids the loader asked for during the pour (the catalyst-face assertion set). */
	private final Set<String> mRequestedItemPaths = new HashSet<>();

	@BeforeEach
	void freshGeneration() {
		GT6RecipeMaps.init();
		GT6RecipeMapJsonLoader.resetForTest();
		mRequestedFluidPaths.clear();
		mRequestedItemPaths.clear();
		// the stand-in resolvers: identity only (the row mechanics never read fluid properties);
		// the sets record WHICH ids the shipped rows actually reference
		GT6RecipeMapJsonLoader.sItemResolver = aId -> {
			if (!"gt6".equals(aId.getNamespace())) return aId.getPath().equals("coal") ? Items.COAL : Items.AIR;
			mRequestedItemPaths.add(aId.getPath());
			return aId.getPath().equals("dust_platinum") ? Items.IRON_INGOT : Items.AIR;
		};
		GT6RecipeMapJsonLoader.sFluidResolver = aId -> {
			if ("minecraft".equals(aId.getNamespace())) return aId.getPath().equals("water") ? Fluids.WATER : Fluids.LAVA;
			mRequestedFluidPaths.add(aId.getPath());
			return Fluids.WATER;
		};
	}

	@AfterEach
	void teardownGeneration() {
		GT6RecipeMapJsonLoader.sItemResolver = sDefaultItems; // restore the live seams (JUL test order is arbitrary)
		GT6RecipeMapJsonLoader.sFluidResolver = sDefaultFluids;
		GT6RecipeMaps.reset(); // the generation hook retires the JSON tracker WITH the maps
	}

	/** Reads one shipped true-row file verbatim and pours it under its map key. */
	private void pourShipped(String aMapKey) throws Exception {
		String tPath = "/data/gt6/recipe_maps/" + aMapKey + ".json";
		try (InputStream tStream = GT6ChemicalRowsPourTest.class.getResourceAsStream(tPath)) {
			assertNotNull(tStream, "the shipped true-row file " + tPath + " rides the test classpath");
			String tJson = new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
			GT6RecipeMapJsonLoader.pour(Map.of(new ResourceLocation("gt6", aMapKey), JsonParser.parseString(tJson)));
		}
	}

	@Test
	public void theFourShippedTrueRowFilesPourIntoTheirMaps() throws Exception {
		pourShipped("steamcracking");
		pourShipped("catalyticcracking");
		pourShipped("gas_fuels");
		pourShipped("burn");
		assertEquals(2, GT6RecipeMaps.STEAM_CRACKING.mRecipeList.size(), "the two SteamCracking rows (:368-369)");
		assertEquals(3, GT6RecipeMaps.CATALYTIC_CRACKING.mRecipeList.size(), "the three CatalyticCracking rows (:373-376, the ethanol double-id merged)");
		assertEquals(6, GT6RecipeMaps.GAS_FUELS.mRecipeList.size(), "natural_gas + methane + butane + propane + ethylene + propylene");
		assertEquals(4, GT6RecipeMaps.BURN.mRecipeList.size(), "butane + propane + ethylene + propylene (Loader_Fuels :179-188)");
	}

	/** Acceptance ② in: the SteamCracking rows carry the :368-369 ins/outs (2 in, 4 out, 16 EUt × 64). The map row list is a HashSet — the two rows match by CONTENT pattern, not iteration order. */
	@Test
	public void steamCrackingRowsCarryTheUpstreamInsAndOuts() throws Exception {
		pourShipped("steamcracking");
		assertEquals(2, GT6RecipeMaps.STEAM_CRACKING.mRecipeList.size());
		Set<String> tSeen = new HashSet<>();
		for (Recipe tRow : GT6RecipeMaps.STEAM_CRACKING.mRecipeList) {
			assertEquals(16L, tRow.mEUt, "16 EUt");
			assertEquals(64L, tRow.mDuration, "64 ticks");
			assertEquals(2, tRow.mFluidInputs.length, "steam + the hydrocarbon");
			assertEquals(1000, tRow.mFluidInputs[0].getAmount(), "1000 L steam");
			assertEquals(100, tRow.mFluidInputs[1].getAmount(), "100 L hydrocarbon");
			assertEquals(4, tRow.mFluidOutputs.length, "H2 + CH4 + ethylene + propylene");
			StringBuilder tPattern = new StringBuilder();
			for (int j = 0; j < 4; j++) tPattern.append(tRow.mFluidOutputs[j].getAmount()).append('|');
			tSeen.add(tPattern.toString());
		}
		assertEquals(Set.of("2|27|42|19|", "5|9|78|3|"), tSeen, "the :368/:369 output ladders (propane, butane)");
		Set<String> tExpected = Set.of("steam", "propane", "butane", "hydrogen", "methane", "ethylene", "propylene");
		assertTrue(mRequestedFluidPaths.containsAll(tExpected), "the cracking id face: " + mRequestedFluidPaths);
	}

	/** Acceptance ② catalyst face: each CatalyticCracking row carries the Pt-dust item slot and the :373-376 in/out fluids (content-matched — the row list is unordered). */
	@Test
	public void catalyticRowsCarryTheCatalystSlotAndInOuts() throws Exception {
		pourShipped("catalyticcracking");
		Set<String> tSeen = new HashSet<>();
		for (Recipe tRow : GT6RecipeMaps.CATALYTIC_CRACKING.mRecipeList) {
			assertEquals(16L, tRow.mEUt, "16 EUt");
			assertEquals(64L, tRow.mDuration, "64 ticks");
			assertEquals(1, tRow.mInputs.length, "the catalyst dust slot");
			assertEquals(1, tRow.mInputs[0].getCount(), "one catalyst item");
			assertEquals(2, tRow.mFluidInputs.length, "hydrogen + feedstock");
			assertEquals(100, tRow.mFluidInputs[0].getAmount(), "100 L hydrogen");
			assertEquals(100, tRow.mFluidInputs[1].getAmount(), "100 L feedstock");
			tSeen.add(tRow.mFluidOutputs[0].getAmount() + "|" + tRow.mFluidOutputs[1].getAmount());
		}
		assertEquals(Set.of("20|5", "30|20", "40|10"), tSeen, "the :373+:374/:375/:376 ethylene|propylene ladders");
		assertEquals(Set.of("dust_platinum"), mRequestedItemPaths, "the catalyst face is the Pt dust");
		Set<String> tExpected = Set.of("hydrogen", "ethanol", "petrol", "fuel", "ethylene", "propylene");
		assertTrue(mRequestedFluidPaths.containsAll(tExpected), "the catalytic id face: " + mRequestedFluidPaths);
	}

	/** Acceptance ③: the FM.Gas rows are the negative-EUt fuel semantics over the closure fluids, the natural_gas CO2 leg restored (content-matched — the row list is unordered). */
	@Test
	public void gasFuelRowsCarryTheFuelSemanticsAndTheRestoredByproduct() throws Exception {
		pourShipped("gas_fuels");
		assertEquals(6, GT6RecipeMaps.GAS_FUELS.mRecipeList.size());
		Set<String> tSeen = new HashSet<>();
		for (Recipe tRow : GT6RecipeMaps.GAS_FUELS.mRecipeList) {
			assertEquals(-64L, tRow.mEUt, "the NEGATIVE fuel EUt");
			assertEquals(1, tRow.mFluidInputs.length, "one fuel fluid");
			assertEquals(2, tRow.mFluidOutputs.length, "water + carbon dioxide");
			assertEquals(64L * tRow.mDuration, tRow.getAbsoluteTotalPower(), "|EUt × duration|");
			tSeen.add(tRow.mFluidInputs[0].getAmount() + "x" + tRow.mDuration);
		}
		// the natural_gas and methane rows share the 5 L × 30 column — five DISTINCT column pairs over six rows
		assertEquals(Set.of("5x30", "7x56", "5x40", "1x5", "1x4"), tSeen,
				"the litres×duration columns of :162/:161/:180/:183/:186/:189");
		Set<String> tExpected = Set.of("natural_gas", "methane", "butane", "propane", "ethylene", "propylene");
		assertTrue(mRequestedFluidPaths.containsAll(tExpected), "the FM.Gas id face: " + mRequestedFluidPaths);
	}

	/** The FM.Burn face: the same four gases over the Burnable map, the :179-188 durations (content-matched — the row list is unordered). */
	@Test
	public void burnRowsCarryTheFuelSemantics() throws Exception {
		pourShipped("burn");
		assertEquals(4, GT6RecipeMaps.BURN.mRecipeList.size());
		Set<String> tSeen = new HashSet<>();
		for (Recipe tRow : GT6RecipeMaps.BURN.mRecipeList) {
			assertEquals(-64L, tRow.mEUt, "the NEGATIVE fuel EUt");
			tSeen.add(tRow.mFluidInputs[0].getAmount() + "x" + tRow.mDuration);
		}
		assertEquals(Set.of("7x42", "5x30", "1x4", "1x3"), tSeen, "the litres×duration columns of :179/:182/:185/:188");
		Set<String> tExpected = Set.of("butane", "propane", "ethylene", "propylene");
		assertTrue(mRequestedFluidPaths.containsAll(tExpected), "the FM.Burn id face: " + mRequestedFluidPaths);
	}

	/**
	 * Acceptance ④: the tower routing layers the closure gases ride — propane/methane to
	 * y+7, butane y+6 (the GT6Distillation :568-576 class table over the port ids; the
	 * kerosene/bioethanol cover words stay harmless).
	 */
	@Test
	public void towerRoutingLayersPinTheClosureGasIds() {
		assertEquals(7, GT6Distillation.TileEntityDistillationTower.routingLayerByName("gt6:propane"), ":569 propane to y+7");
		assertEquals(7, GT6Distillation.TileEntityDistillationTower.routingLayerByName("gt6:methane"), ":569 methane to y+7");
		assertEquals(6, GT6Distillation.TileEntityDistillationTower.routingLayerByName("gt6:butane"), ":570 butane to y+6");
		assertEquals(1, GT6Distillation.TileEntityDistillationTower.routingLayerByName("gt6:ethylene"), "no ethylene class in :571-574 — the default leg");
		assertEquals(1, GT6Distillation.TileEntityDistillationTower.routingLayerByName("gt6:hydrogen"), "hydrogen rides the default y+1");
		assertEquals(1, GT6Distillation.TileEntityDistillationTower.routingLayerByName("gt6:carbondioxide"), "CO2 rides the default y+1");
	}
}
