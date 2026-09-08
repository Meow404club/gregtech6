package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

/**
 * The tier-b JSON seam offline suite (task p26-tier-b-rm-json-loader acceptance 2): the
 * row-parse happy path, the bad-row WARN+skip contract, the identity-subset replace
 * idempotence (same file applied twice keeps the row count), the FURNACE/FURNACE_FUEL
 * whole-file rejection, the chance<=0 no-output deviation, and the negative-eut fuel-gate
 * semantics. Every registry face rides injected fixtures (the GTEngineFuelsTest
 * {@code sFluidResolver} convention — the vanilla bootstrapped Items constants stand in
 * for the live registry); the live lookup face is the runServer chain's (the log's
 * before/after row counts read from the real maps).
 */
class GT6RecipeMapJsonLoaderTest extends GTRecipesOfflineTestBase {

	/** The offline item fixture: the three ids the suite uses resolve to real vanilla items, everything else is "unregistered". */
	private static final Function<ResourceLocation, Item> ITEM_FIXTURE = aId -> switch (aId.toString()) {
		case "minecraft:dead_bush" -> Items.DEAD_BUSH;
		case "minecraft:stick" -> Items.STICK;
		case "minecraft:stone" -> Items.STONE;
		default -> null;
	};

	/** The offline fluid fixture: every fluid id resolves to water (identities are all the row mechanics compare). */
	private static final Function<ResourceLocation, Fluid> FLUID_FIXTURE = aId -> Fluids.WATER;

	private static final Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

	@BeforeEach
	void freshGeneration() {
		GT6RecipeMaps.init();
		GT6RecipeMapJsonLoader.resetForTest();
	}

	@AfterEach
	void teardownGeneration() {
		GT6RecipeMapJsonLoader.sItemResolver = sDefaultItems; // restore the live seams (JUL test order is arbitrary)
		GT6RecipeMapJsonLoader.sFluidResolver = sDefaultFluids;
		GT6RecipeMaps.reset(); // the generation hook retires the JSON tracker WITH the maps
	}

	private static ResourceLocation rl(String aId) {
		return new ResourceLocation(aId);
	}

	private static JsonElement json(String aJson) {
		return JsonParser.parseString(aJson);
	}

	/** The happy row: the full 7-field schema minus chance keys (deterministic outputs). */
	private static final String HAPPY_ROW = """
			{"inputs":[{"item":"minecraft:dead_bush","count":1}],
			 "outputs":[{"item":"minecraft:stick","count":2}],
			 "duration":16,"eut":16,"specialValue":5}""";

	private void injectFixtures() {
		GT6RecipeMapJsonLoader.sItemResolver = ITEM_FIXTURE;
		GT6RecipeMapJsonLoader.sFluidResolver = FLUID_FIXTURE;
	}

	/** Happy path: a well-formed file pours every row with the schema fields in place, and a live findRecipe consumer finds it. */
	@Test
	void happyPathPoursTheSevenFieldSchemaAndTheMapAnswers() {
		injectFixtures();
		GT6RecipeMapJsonLoader.pour(Map.of(rl("gt6:shredder"), json("""
				{"recipes": [%s, {"inputs":[{"item":"minecraft:stone"}],"outputs":[{"item":"minecraft:stick"}],"duration":8}]
				}""".formatted(HAPPY_ROW))));

		assertEquals(2, GT6RecipeMaps.SHREDDER.mRecipeList.size(), "both rows poured");
		assertEquals(2, GT6RecipeMapJsonLoader.pouredCount("shredder"), "the tracker mirrors the map");
		Recipe tRow = GT6RecipeMaps.SHREDDER.findRecipe(null, 32L, null, new FluidStack[0], new ItemStack(Items.DEAD_BUSH, 1));
		assertNotNull(tRow, "a findRecipe consumer answers for the JSON row's input");
		assertEquals(1, tRow.mInputs[0].getCount(), "count:1 honored");
		assertEquals(Items.DEAD_BUSH, tRow.mInputs[0].getItem());
		assertEquals(Items.STICK, tRow.mOutputs[0].getItem());
		assertEquals(2, tRow.mOutputs[0].getCount());
		assertEquals(16, tRow.mDuration);
		assertEquals(16, tRow.mEUt);
		assertEquals(5, tRow.mSpecialValue);
		assertNull(tRow.mChances, "no chance keys anywhere = the deterministic null-chances row");
		assertTrue(tRow.mCanBeBuffered, "the buffered default (v1: no row flags)");

		// the count default: the second row omitted "count" on both slots
		Recipe tDefault = GT6RecipeMaps.SHREDDER.findRecipe(null, 32L, null, new FluidStack[0], new ItemStack(Items.STONE, 1));
		assertNotNull(tDefault);
		assertEquals(1, tDefault.mInputs[0].getCount(), "omitted count defaults to 1");
		assertEquals(1, tDefault.mOutputs[0].getCount());
		assertEquals(8, tDefault.mDuration);
		assertEquals(0, tDefault.mEUt, "omitted eut defaults to 0 (the machine-row convention)");
		assertEquals(0, tDefault.mSpecialValue, "omitted specialValue defaults to 0");
	}

	/** Per-slot chance: 0 = the LEGAL never-produces slot (WARN, no rewrite), missing keys = 10000, chance-free row = null chances. */
	@Test
	void chanceZeroIsALegalNoOutputSlotWithoutARewrite() {
		injectFixtures();
		GT6RecipeMapJsonLoader.pour(Map.of(rl("gt6:crusher"), json("""
				{"recipes": [
				 {"inputs":[{"item":"minecraft:stone"}],
				  "outputs":[{"item":"minecraft:stick","chance":0},{"item":"minecraft:dead_bush","count":3}],
				  "duration":16},
				 {"inputs":[{"item":"minecraft:dead_bush"}],
				  "outputs":[{"item":"minecraft:stick","chance":5000}],
				  "duration":16}
				]}""")));

		Recipe tZero = GT6RecipeMaps.CRUSHER.findRecipe(null, 32L, null, new FluidStack[0], new ItemStack(Items.STONE, 1));
		assertNotNull(tZero);
		assertNotNull(tZero.mChances, "one explicit chance key materializes the chances array");
		assertEquals(0, tZero.mChances[0], "chance 0 kept verbatim — the declared port deviation (upstream :906 would rewrite to 10000)");
		assertEquals(10000, tZero.mChances[1], "slots without a chance key read the 10000 base");
		ItemStack[] tOutputs = tZero.getOutputs();
		assertNull(tOutputs[0], "chance 0 = NO output (Recipe.getOutputs :220)");
		assertEquals(3, tOutputs[1].getCount(), "the full-chance slot produces");

		Recipe tHalf = GT6RecipeMaps.CRUSHER.findRecipe(null, 32L, null, new FluidStack[0], new ItemStack(Items.DEAD_BUSH, 1));
		assertNotNull(tHalf);
		assertEquals(5000, tHalf.mChances[0], "a fractional chance rides the array verbatim (Bernoulli at consume time)");
	}

	/** Bad rows WARN and skip, never crash: good rows in the same file still pour. */
	@Test
	void badRowsAreSkippedWithTheFileStillPouring() {
		injectFixtures();
		GT6RecipeMapJsonLoader.pour(Map.of(rl("gt6:shredder"), json("""
				{"recipes": [
				 %s,
				 {"inputs":[{"item":"minecraft:not_a_real_item"}],"outputs":[{"item":"minecraft:stick"}],"duration":16},
				 {"inputs":[{"item":"minecraft:dead_bush","count":0}],"outputs":[{"item":"minecraft:stick"}],"duration":16},
				 {"inputs":[{"item":"minecraft:dead_bush"}],"outputs":[{"item":"minecraft:stick"}],"duration":0},
				 {"inputs":[{"tag":"minecraft:logs"}],"outputs":[{"item":"minecraft:stick"}],"duration":16},
				 {"inputs":[{"item":"gt6:missing_prefix"}],"outputs":[],"duration":4},
				 {"inputs":[{"fluid":"minecraft:water"}],"outputs":[],"duration":-3}
				]}""".formatted(HAPPY_ROW))));

		assertEquals(1, GT6RecipeMaps.SHREDDER.mRecipeList.size(), "exactly the one good row poured");
		assertEquals(1, GT6RecipeMapJsonLoader.pouredCount("shredder"));
		Recipe tRow = GT6RecipeMaps.SHREDDER.mRecipeList.iterator().next();
		assertEquals(Items.DEAD_BUSH, tRow.mInputs[0].getItem(), "the survivor is the happy row");
	}

	/** Fluid rows: mB amounts ride verbatim, and a no-input-leg row is dropped by the map's ghost guard (WARN, not a crash). */
	@Test
	void fluidAmountsAreMillibucketsAndGhostRowsDrop() {
		injectFixtures();
		GT6RecipeMapJsonLoader.pour(Map.of(rl("gt6:drying"), json("""
				{"recipes": [
				 {"fluidInputs":[{"fluid":"minecraft:water","amount":250}],
				  "fluidOutputs":[{"fluid":"minecraft:water","amount":125}],
				  "duration":32},
				 {"outputs":[{"item":"minecraft:stick"}],"duration":4}
				]}""")));

		assertEquals(1, GT6RecipeMaps.DRYING.mRecipeList.size(), "the input-less ghost row dropped (RecipeMap.java:122)");
		Recipe tRow = GT6RecipeMaps.DRYING.mRecipeList.iterator().next();
		assertEquals(250, tRow.mFluidInputs[0].getAmount(), "amount = mB verbatim, no loader-side conversion");
		assertEquals(125, tRow.mFluidOutputs[0].getAmount());
	}

	/** The fuel face: a negative eut row pours, |eut x duration| powers it, and the findRecipe gate reads the ABSOLUTE value. */
	@Test
	void negativeEutPoursTheFuelGateSemantics() {
		injectFixtures();
		GT6RecipeMapJsonLoader.pour(Map.of(rl("gt6:burn"), json("""
				{"recipes": [
				 {"fluidInputs":[{"fluid":"minecraft:water","amount":100}],"duration":40,"eut":-16}
				]}""")));

		assertEquals(1, GT6RecipeMaps.BURN.mRecipeList.size());
		Recipe tFuel = GT6RecipeMaps.BURN.mRecipeList.iterator().next();
		assertEquals(-16, tFuel.mEUt, "the verbatim negative eut (generator convention)");
		assertEquals(640, tFuel.getAbsoluteTotalPower(), "|eut x duration| (Recipe.java:171-173)");

		// the findRecipe gate is absolute-valued (RecipeMap.absGreaterEqual :154-156): the
		// buffered fast path answers while |size x power| covers |eut|, and declines below it.
		FluidStack[] tTank = {new FluidStack(Fluids.WATER, 100)};
		Recipe tFound = GT6RecipeMaps.BURN.findRecipe(tFuel, 1000L, null, tTank, new ItemStack(Items.STONE, 1));
		assertNotNull(tFound, "1000 covers |−16|");
		assertNull(GT6RecipeMaps.BURN.findRecipe(tFuel, 1L, null, tTank, new ItemStack(Items.STONE, 1)),
				"|1| < |−16| — the gate declines");
	}

	/** Idempotence: the same file applied twice keeps the row count and swaps EVERY instance (identity subset replace). */
	@Test
	void sameFileTwiceKeepsTheRowCountAndSwapsInstances() {
		injectFixtures();
		Map<ResourceLocation, JsonElement> tFile = Map.of(rl("gt6:shredder"), json("""
				{"recipes": [%s, {"inputs":[{"item":"minecraft:stone"}],"outputs":[{"item":"minecraft:stick"}],"duration":8}]}"""
				.formatted(HAPPY_ROW)));
		GT6RecipeMapJsonLoader.pour(tFile);
		Set<Recipe> tFirst = new HashSet<>(GT6RecipeMaps.SHREDDER.mRecipeList);
		assertEquals(2, tFirst.size());

		GT6RecipeMapJsonLoader.pour(tFile); // the /reload replay

		assertEquals(2, GT6RecipeMaps.SHREDDER.mRecipeList.size(), "no stacking: the replace, not an append");
		assertTrue(Collections.disjoint(tFirst, GT6RecipeMaps.SHREDDER.mRecipeList),
				"the previous instances are exactly the removed ones (identity semantics, no equals/hashCode)");
		assertEquals(2, GT6RecipeMapJsonLoader.pouredCount("shredder"));
	}

	/** A second file for the same key replaces the first subset whole (the file = the pack-authoring unit). */
	@Test
	void aNewFileReplacesTheWholePreviousSubset() {
		injectFixtures();
		GT6RecipeMapJsonLoader.pour(Map.of(rl("gt6:shredder"), json("""
				{"recipes": [%s]}""".formatted(HAPPY_ROW))));
		Recipe tFirst = GT6RecipeMaps.SHREDDER.mRecipeList.iterator().next();

		GT6RecipeMapJsonLoader.pour(Map.of(rl("gt6:shredder"), json("""
				{"recipes": [{"inputs":[{"item":"minecraft:stone"}],"outputs":[{"item":"minecraft:stone"}],"duration":8}]}""")));

		assertEquals(1, GT6RecipeMaps.SHREDDER.mRecipeList.size(), "the new file's rows only");
		Recipe tSecond = GT6RecipeMaps.SHREDDER.mRecipeList.iterator().next();
		assertTrue(Collections.disjoint(Set.of(tFirst), GT6RecipeMaps.SHREDDER.mRecipeList), "the old subset fully retired");
		assertEquals(Items.STONE, tSecond.mInputs[0].getItem(), "the new file's content");
	}

	/** The FURNACE/FURNACE_FUEL gate: files for the two zero-stock maps are rejected whole (ERROR, nothing poured anywhere). */
	@Test
	void furnaceAndFurnaceFuelFilesAreRejectedWhole() {
		injectFixtures();
		GT6RecipeMapJsonLoader.pour(Map.of(
				rl("gt6:furnace"), json("""
						{"recipes": [{"inputs":[{"item":"minecraft:stone"}],"outputs":[{"item":"minecraft:stick"}],"duration":16}]}"""),
				rl("gt6:furnace_fuel"), json("""
						{"recipes": [{"inputs":[{"item":"minecraft:stone"}],"outputs":[],"duration":16}]}""")));

		assertEquals(0, GT6RecipeMaps.FURNACE.mRecipeList.size(), "FURNACE rows would never be consumed (it proxies the vanilla RecipeManager)");
		assertEquals(0, GT6RecipeMaps.FURNACE_FUEL.mRecipeList.size(), "FURNACE_FUEL synthesizes on demand — no rows ever");
		assertEquals(0, GT6RecipeMapJsonLoader.pouredCount("furnace"));
		assertEquals(0, GT6RecipeMapJsonLoader.pouredCount("furnace_fuel"));
	}

	/** The namespace and whitelist gates: foreign-namespace squatting of our keys is visible+skipped, unknown keys skip. */
	@Test
	void namespaceAndWhitelistGatesHold() {
		injectFixtures();
		GT6RecipeMapJsonLoader.pour(Map.of(
				rl("othermod:shredder"), json("""
						{"recipes": [%s]}""".formatted(HAPPY_ROW)),
				rl("gt6:not_a_map"), json("""
						{"recipes": [%s]}""".formatted(HAPPY_ROW)),
				rl("gt6:coke_oven"), json("""
						{"recipes": [%s]}""".formatted(HAPPY_ROW))));

		assertEquals(0, GT6RecipeMaps.SHREDDER.mRecipeList.size(), "a foreign namespace never pours (the seam reads data/gt6/recipe_maps/)");
		assertEquals(0, GT6RecipeMaps.LATHE.mRecipeList.size(), "an unknown key is not a map");
		assertEquals(1, GT6RecipeMaps.COKE_OVEN.mRecipeList.size(), "the properly-namespaced file pours");
	}

	/** Malformed file shapes skip whole: top-level array, missing "recipes". */
	@Test
	void malformedFilesSkipWhole() {
		injectFixtures();
		GT6RecipeMapJsonLoader.pour(Map.of(
				rl("gt6:lathe"), json("[1,2,3]"),
				rl("gt6:chisel"), json("{\"something\": true}")));

		assertEquals(0, GT6RecipeMaps.LATHE.mRecipeList.size());
		assertEquals(0, GT6RecipeMaps.CHISEL.mRecipeList.size());
	}

	/** The generation-reset discipline: reset() retires the tracker WITH the maps (the ADR-P18 recheck point). */
	@Test
	void generationResetRetiresTheTracker() {
		injectFixtures();
		GT6RecipeMapJsonLoader.pour(Map.of(rl("gt6:shredder"), json("""
				{"recipes": [%s]}""".formatted(HAPPY_ROW))));
		assertEquals(1, GT6RecipeMapJsonLoader.pouredCount("shredder"));

		GT6RecipeMaps.reset();
		assertEquals(0, GT6RecipeMapJsonLoader.pouredCount("shredder"), "the generation hook cleared the tracker with the maps");

		// the broken-lifecycle gate: pouring with no map instances skips cleanly (no NPE)
		assertDoesNotThrow(() -> GT6RecipeMapJsonLoader.pour(Map.of(rl("gt6:shredder"), json("""
				{"recipes": [%s]}""".formatted(HAPPY_ROW)))));
		assertEquals(0, GT6RecipeMapJsonLoader.pouredCount("shredder"));

		// and a fresh generation re-pours exactly once
		GT6RecipeMaps.init();
		GT6RecipeMapJsonLoader.pour(Map.of(rl("gt6:shredder"), json("""
				{"recipes": [%s]}""".formatted(HAPPY_ROW))));
		assertEquals(1, GT6RecipeMaps.SHREDDER.mRecipeList.size(), "fresh generation: one row, no tracker residue");
	}

	private static void assertDoesNotThrow(Runnable aRunnable) {
		try {
			aRunnable.run();
		} catch (Throwable t) {
			throw new AssertionError("threw " + t, t);
		}
	}

	/** The map-key resolution table covers exactly the 11 pourable keys (and nothing else). */
	@Test
	void mapForResolvesExactlyTheElevenPourableKeys() {
		for (String tKey : new String[] {"coke_oven", "shredder", "crusher", "lathe", "chisel", "engine_fuels",
				"fluidbed", "burn", "distillery", "drying", "canner"}) {
			assertNotNull(GT6RecipeMapJsonLoader.mapFor(tKey), tKey + " resolves");
			assertFalse(tKey.equals("furnace") || tKey.equals("furnace_fuel"), "the forbidden pair stays outside the table");
		}
		assertNull(GT6RecipeMapJsonLoader.mapFor("furnace"), "FURNACE is not pourable");
		assertNull(GT6RecipeMapJsonLoader.mapFor("furnace_fuel"), "FURNACE_FUEL is not pourable");
		assertNull(GT6RecipeMapJsonLoader.mapFor("unknown"), "unknown keys do not resolve");
	}

	/** The live-lookup default bindings resolve vanilla ids offline, and the missing-entry placeholder is a bad row (the live face). */
	@Test
	void defaultResolversAnswerVanillaIdsOffline() {
		assertEquals(Items.STONE, GT6RecipeMapJsonLoader.sItemResolver.apply(rl("minecraft:stone")),
				"the default item seam is the live registry lookup (ForgeRegistries/BuiltInRegistries per leg)");
		assertNotNull(GT6RecipeMapJsonLoader.sFluidResolver.apply(rl("minecraft:water")),
				"the default fluid seam answers vanilla water offline");

		// the live registry lookup answers the MISSING-ENTRY placeholders (air / the empty
		// fluid), not null — the loader must turn them into bad rows (the runServer live face)
		assertEquals(Items.AIR, GT6RecipeMapJsonLoader.sItemResolver.apply(rl("minecraft:gt6_does_not_exist")));
		GT6RecipeMapJsonLoader.pour(Map.of(rl("gt6:shredder"), json("""
				{"recipes": [
				 {"inputs":[{"item":"minecraft:gt6_does_not_exist"}],"outputs":[{"item":"minecraft:stick"}],"duration":16},
				 {"inputs":[{"item":"minecraft:stone"}],"outputs":[{"item":"minecraft:stick"}],"duration":16}
				]}""")));
		assertEquals(1, GT6RecipeMaps.SHREDDER.mRecipeList.size(), "the placeholder-id row skipped as a bad row, the good row poured");
	}
}
