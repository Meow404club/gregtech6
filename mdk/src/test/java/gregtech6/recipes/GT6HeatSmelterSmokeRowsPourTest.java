package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
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

/**
 * The p29-w3-heat-smelter smoke-row pour test (the GT6RollSmokeRowsPourTest fixture
 * posture): the THREE data/gt6/recipe_maps JSON files this card ships (smelter / melter /
 * fuels_hot) are read VERBATIM off the classpath and poured through the real
 * {@link GT6RecipeMapJsonLoader} seam — the new POURABLE keys, the map resolution and the
 * row schema all validated against the shipped bytes. The gt6: hot_water fluid id resolves
 * through the injected resolver seam onto a vanilla stand-in (the synthetic-universe
 * convention — identity is all the recipe mechanics compare); the vanilla ice/water ids
 * are the real registry entries.
 */
public class GT6HeatSmelterSmokeRowsPourTest extends GTRecipesOfflineTestBase {

	private static final Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

	@BeforeEach
	void freshGeneration() {
		GT6RecipeMaps.init();
		GT6RecipeMapJsonLoader.resetForTest();
		// gt6:hot_water is not registered offline — the LAVA stand-in is identity only
		// (the row mechanics never read fluid properties, only equality)
		GT6RecipeMapJsonLoader.sItemResolver = aId -> aId.getPath().equals("ice") ? Items.ICE : Items.AIR;
		GT6RecipeMapJsonLoader.sFluidResolver = aId -> aId.getPath().equals("hot_water") ? Fluids.LAVA : Fluids.WATER;
	}

	@AfterEach
	void teardownGeneration() {
		GT6RecipeMapJsonLoader.sItemResolver = sDefaultItems; // restore the live seams (JUL test order is arbitrary)
		GT6RecipeMapJsonLoader.sFluidResolver = sDefaultFluids;
		GT6RecipeMaps.reset(); // the generation hook retires the JSON tracker WITH the maps
	}

	/** Reads one shipped smoke-row file verbatim and pours it under its map key. */
	private void pourShipped(String aMapKey) throws Exception {
		String tPath = "/data/gt6/recipe_maps/" + aMapKey + ".json";
		try (InputStream tStream = GT6HeatSmelterSmokeRowsPourTest.class.getResourceAsStream(tPath)) {
			assertNotNull(tStream, "the shipped smoke-row file " + tPath + " rides the test classpath");
			String tJson = new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
			GT6RecipeMapJsonLoader.pour(Map.of(new ResourceLocation("gt6", aMapKey), JsonParser.parseString(tJson)));
		}
	}

	@Test
	void theThreeShippedSmokeFilesPourIntoTheirMaps() throws Exception {
		for (String tKey : new String[] {"smelter", "melter", "fuels_hot"}) {
			pourShipped(tKey);
			assertEquals(1, GT6RecipeMapJsonLoader.pouredCount(tKey), tKey + ": the smoke row poured");
			assertNotNull(GT6RecipeMapJsonLoader.mapFor(tKey), tKey + ": the whitelist key resolves its map");
		}
		assertEquals(1, GT6RecipeMaps.SMELTER.mRecipeList.size(), "the smelter map holds the ice smoke row");
		assertEquals(1, GT6RecipeMaps.MELTER.mRecipeList.size(), "the melter map holds the ice smoke row");
		assertEquals(1, GT6RecipeMaps.FUELS_HOT.mRecipeList.size(), "the fuels_hot map holds the hot-water smoke row");
	}

	@Test
	void theIceRowsCarryTheUpstreamChemLoaderParameters() throws Exception {
		pourShipped("smelter");
		pourShipped("melter");
		Recipe tSmelterRow = GT6RecipeMaps.SMELTER.mRecipeList.iterator().next();
		assertEquals(16L, tSmelterRow.mEUt, "the smelter row's eut 16 (Loader_Recipes_Chem.java:501)");
		assertEquals(2000L, tSmelterRow.mDuration, "the smelter row's duration 2000 (= the :501 1000*2)");
		assertEquals(1000, tSmelterRow.mFluidOutputs[0].getAmount(), "the smelter row melts to 1000 L water");
		Recipe tMelterRow = GT6RecipeMaps.MELTER.mRecipeList.iterator().next();
		assertEquals(16L, tMelterRow.mEUt, "the melter row's eut 16 (Loader_Recipes_Chem.java:486)");
		assertEquals(2000L, tMelterRow.mDuration, "the melter row's duration 2000");
	}

	@Test
	void theHotWaterFuelRowIsANegativeEutRowWorthTwoPowerPerLitre() throws Exception {
		pourShipped("fuels_hot");
		Recipe tRow = GT6RecipeMaps.FUELS_HOT.mRecipeList.iterator().next();
		assertEquals(-2L, tRow.mEUt, "the fuel row's NEGATIVE eut (the fuel-map semantics, Loader_Fuels.java:196)");
		assertEquals(1L, tRow.mDuration, "the fuel row's duration 1");
		assertEquals(2L, tRow.getAbsoluteTotalPower(), "the fuel power = |eut x duration| = 2 per fluid unit — the value the HEX books per litre");
		assertTrue(tRow.mFluidInputs.length > 0 && tRow.mFluidOutputs.length > 0, "the row is fluid-in fluid-out");
	}
}
