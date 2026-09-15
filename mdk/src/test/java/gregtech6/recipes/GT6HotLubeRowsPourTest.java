package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
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

/**
 * The p29-w4-hot-lube true-row pour test (the GT6ChemicalRowsPourTest fixture posture): the
 * TWO data/gt6/recipe_maps JSON files this card ships (fuels_hot / distillationtower) are
 * read VERBATIM off the classpath and poured through the real {@link GT6RecipeMapJsonLoader}
 * seam. Every gt6 fluid id resolves through the injected resolver seam onto a vanilla
 * stand-in (identity only); the resolver RECORDS the requested paths so the acceptance-②
 * closure face asserts without a live registry: the fuel rows' referenced gt6 id set must
 * EQUAL the declared registration set (the dead-row check) — ic2hotcoolant → ic2coolant at
 * EU_PER_COOLANT = 20 pins the live-chain row, and the distillation rows carry SEVEN
 * non-empty fluid product slots each (acceptance ⑤) plus the three dustTiny slots.
 */
public class GT6HotLubeRowsPourTest extends GTRecipesOfflineTestBase {

	private static final Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

	/** The fluid ids the loader asked for during the pour (the id-face assertion set). */
	private final Set<String> mRequestedFluidPaths = new HashSet<>();
	/** The item ids the loader asked for during the pour (the dust-face assertion set). */
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
			if (!"gt6".equals(aId.getNamespace())) return Items.AIR;
			mRequestedItemPaths.add(aId.getPath());
			return Items.IRON_INGOT;
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
		try (InputStream tStream = GT6HotLubeRowsPourTest.class.getResourceAsStream(tPath)) {
			assertNotNull(tStream, "the shipped true-row file " + tPath + " rides the test classpath");
			String tJson = new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
			GT6RecipeMapJsonLoader.pour(Map.of(new ResourceLocation("gt6", aMapKey), JsonParser.parseString(tJson)));
		}
	}

	/** Acceptance ②: the fuels_hot pour — 14 rows (the W3 hot_water row + this card's 13), the EU_PER_* duration ladder pinned, and the coolant ids on the requested face. */
	@Test
	public void fuelsHotRowsPourWithTheCoolantRowAndTheDurationLadder() throws Exception {
		pourShipped("fuels_hot");
		assertEquals(14, GT6RecipeMaps.FUELS_HOT.mRecipeList.size(), "the W3 hot_water row + the 13 FM.Hot rows (:191-211 minus the exists()-guarded twins)");
		// the EU_PER_* ladder — {duration, expected row count} content-matched (the row list is unordered)
		Map<Long, Integer> tByDuration = new HashMap<>();
		for (Recipe tRow : GT6RecipeMaps.FUELS_HOT.mRecipeList) {
			assertTrue(tRow.mEUt < 0, "the fuel-map negative EUt semantics");
			assertEquals(1, tRow.mFluidInputs.length, "every FM.Hot row burns exactly one fuel fluid");
			tByDuration.merge(tRow.mDuration, 1, Integer::sum);
		}
		// CS.java:216-234: blaze 6 / lava 5 / hot waters 1 x3 / coolant 20 / sodium 30 / tin 40 /
		// heavy 50 / semiheavy 40 / tritiated 60 / CO2 20 / helium 30 / licl 15
		assertEquals(1, tByDuration.get(6L), "blaze :191");
		assertEquals(1, tByDuration.get(5L), "lava :192 EU_PER_LAVA/16 = 5");
		assertEquals(3, tByDuration.get(1L), "hot_water + water_boiling + geothermal (:196/:200/:201)");
		assertEquals(2, tByDuration.get(20L), "EU_PER_COOLANT + EU_PER_CO2");
		assertEquals(2, tByDuration.get(30L), "EU_PER_SODIUM + EU_PER_HELIUM");
		assertEquals(2, tByDuration.get(40L), "EU_PER_TIN + EU_PER_SEMI_HEAVY_WATER");
		assertEquals(1, tByDuration.get(50L), "EU_PER_HEAVY_WATER");
		assertEquals(1, tByDuration.get(60L), "EU_PER_TRITIATED_WATER");
		assertEquals(1, tByDuration.get(15L), "EU_PER_LICL");
		// the live-chain row's id face: the :203 pair rode the pour
		assertTrue(mRequestedFluidPaths.contains("ic2hotcoolant"), "the :203 input id");
		assertTrue(mRequestedFluidPaths.contains("ic2coolant"), "the :203 output id");
	}

	/**
	 * Acceptance ② closure half (the dead-row check): the gt6 fluid id set the fuels_hot
	 * rows reference EQUALS the declared registration set — the card's 19 rows + the
	 * already-live aqua/water ids (hot_water, water_boiling, water_geothermal, mnwtr) + the
	 * card-① closure gases (carbondioxide, helium). One reference outside this set would be
	 * an unresolved id (the json loader WARNs and drops the row live).
	 */
	@Test
	public void everyFuelRowReferenceResolvesToADeclaredCarrier() throws Exception {
		pourShipped("fuels_hot");
		Set<String> tExpected = new HashSet<>(java.util.List.of(
				// the hot family (the 12 rows of Loader_Fluids.java:85-99; ic2pahoehoelava arrives via the lava row)
				"ic2coolant", "ic2hotcoolant", "hotmoltensodium", "hotmoltentin", "hotmoltenlicl",
				"hotheavywater", "hotsemiheavywater", "hottritiatedwater",
				"hotcarbondioxide", "hothelium", "ic2pahoehoelava",
				// the closure carriers (blaze is the :191 fuel; the rest are the outputs)
				"blaze", "sodium_molten", "tin_molten", "lithium_chloride_molten",
				"heavywater", "semiheavywater", "tritiatedwater",
				// the card-① closure gases + the aqua/water ids
				"carbondioxide", "helium",
				"hot_water", "water_boiling", "water_geothermal", "mnwtr"));
		// thoriumsalt (:97) is REGISTERED but deliberately NOT referenced by any :191-211 row
		// — its consumer face is the deferred Reactor domain (the card's "不做" list)
		assertTrue(!mRequestedFluidPaths.contains("thoriumsalt"), "the reactor-salt consumption stays deferred");
		Set<String> tRequestedGt6 = new HashSet<>(mRequestedFluidPaths);
		tRequestedGt6.removeIf(aPath -> aPath.startsWith("minecraft:"));
		assertEquals(tExpected, tRequestedGt6, "the fuel-row id face closes over the declared carriers — no dead row");
	}

	/** Acceptance ⑤: the distillation pour — 5 true rows (the W3 smoke row preserved), each with SEVEN non-empty fluid product slots and the three dustTiny item slots at chance 5000. */
	@Test
	public void distillationOilRowsCarrySevenFilledProductSlotsAndTheDustFace() throws Exception {
		pourShipped("distillationtower");
		assertEquals(6, GT6RecipeMaps.DISTILLATION_TOWER.mRecipeList.size(), "the W3 smoke row + the :352-:360 five true rows");
		int tTrueRows = 0;
		Set<Long> tDurations = new HashSet<>();
		for (Recipe tRow : GT6RecipeMaps.DISTILLATION_TOWER.mRecipeList) {
			if (tRow.mFluidInputs.length != 1 || tRow.mFluidInputs[0].getAmount() != 25) continue; // the W3 smoke row (oil 1000 → creosote)
			tTrueRows++;
			assertEquals(64L, tRow.mEUt, "the :352-:360 EUt literal");
			assertEquals(7, tRow.mFluidOutputs.length, "acceptance ⑤: SEVEN non-empty product slots");
			for (int i = 0; i < 7; i++) {
				assertNotNull(tRow.mFluidOutputs[i], "fluid product slot " + i + " non-empty");
				assertTrue(tRow.mFluidOutputs[i].getAmount() > 0, "fluid product slot " + i + " carries amount");
			}
			assertEquals(3, tRow.mOutputs.length, "the WaxParaffin/Asphalt/PetCoke dustTiny slots");
			tDurations.add(tRow.mDuration);
		}
		assertEquals(5, tTrueRows, "the five oil true rows poured");
		assertEquals(Set.of(256L, 196L, 128L, 64L), tDurations, "the duration ladder: :352 256 / :353 196 / :355 128 / :358+:360 share 64");
		Set<String> tExpectedDusts = Set.of("dust_tiny_wax_paraffin", "dust_tiny_asphalt", "dust_tiny_petroleum_coke");
		assertTrue(mRequestedItemPaths.containsAll(tExpectedDusts), "the dust face: " + mRequestedItemPaths);
		Set<String> tExpectedFluids = Set.of("liquid_extra_heavy_oil", "liquid_heavy_oil", "liquid_medium_oil",
				"liquid_light_oil", "soulsandoil",
				"fuel", "diesel", "kerosine", "petrol", "propane", "butane", "lubricant");
		assertTrue(mRequestedFluidPaths.containsAll(tExpectedFluids), "the oil-row id face (7 products + 5 feeds): " + mRequestedFluidPaths);
	}
}
