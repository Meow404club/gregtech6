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
		// the fuels_hot per-key poured count ratcheted 1 → 14 (the card-④ FM.Hot true rows
		// appended behind the :196 hot_water row, itself upgraded to the true-row framing;
		// smelter/melter keep their single smoke rows)
		java.util.Map<String, Integer> tExpected = java.util.Map.of("smelter", 1, "melter", 1, "fuels_hot", 14);
		for (String tKey : new String[] {"smelter", "melter", "fuels_hot"}) {
			pourShipped(tKey);
			assertEquals(tExpected.get(tKey), GT6RecipeMapJsonLoader.pouredCount(tKey), tKey + ": the poured row count");
			assertNotNull(GT6RecipeMapJsonLoader.mapFor(tKey), tKey + ": the whitelist key resolves its map");
		}
		assertEquals(1, GT6RecipeMaps.SMELTER.mRecipeList.size(), "the smelter map holds the ice smoke row");
		assertEquals(1, GT6RecipeMaps.MELTER.mRecipeList.size(), "the melter map holds the ice smoke row");
		// task p29-w4-hot-lube appended the 13 FM.Hot true rows behind the W3 smoke row —
		// the ratchet: 14 rows now, the smoke row still among them (pinned by content below)
		assertEquals(14, GT6RecipeMaps.FUELS_HOT.mRecipeList.size(), "the fuels_hot map: the :196 hot-water TRUE row (the review-round upgrade from the smoke-row framing) + the 13 card-④ FM.Hot rows");
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
		// the :196 hot_water TRUE row is matched BY CONTENT now — the card-④ rows ride beside it
		// (the row list is unordered, so the hot_water 1→1 pair is picked by its duration-1 face;
		// both duration-1 rows carry eut -2 and |power| 2, so the content face pins them equally)
		Recipe tRow = null;
		for (Recipe tCandidate : GT6RecipeMaps.FUELS_HOT.mRecipeList) {
			if (tCandidate.mDuration == 1 && tCandidate.mFluidInputs.length == 1 && tCandidate.mFluidInputs[0].getAmount() == 1) tRow = tCandidate;
		}
		assertNotNull(tRow, "the W3 hot_water row still pours (content-matched)");
		assertEquals(-2L, tRow.mEUt, "the fuel row's NEGATIVE eut (the fuel-map semantics, Loader_Fuels.java:196)");
		assertEquals(2L, tRow.getAbsoluteTotalPower(), "the fuel power = |eut x duration| = 2 per fluid unit — the value the HEX books per litre");
		assertTrue(tRow.mFluidInputs.length > 0 && tRow.mFluidOutputs.length > 0, "the row is fluid-in fluid-out");
	}
}
