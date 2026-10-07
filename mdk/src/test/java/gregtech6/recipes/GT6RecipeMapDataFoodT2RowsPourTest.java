package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * The food-recipes-t2 row-stock pour test (the b2b1 posture): the four food-domain
 * map files of this card — distillery (21 rows, new file), mixer (223 rows, new
 * file), melter and smelter (1 smoke + 9 pour each) — pour through the real
 * {@link GT6RecipeMapJsonLoader} seam with zero skips, the file-head declarations
 * keep naming the declared-out blockers, the verbatim spot checks read the shipped
 * rows directly, and the walk faces pin their port-universe membership counts
 * (the 11x4 COOKING_OIL x VINEGAR walk, the 37-member FRUIT_JUICE smoothie walk,
 * the 35-row smoothie table, the 3-form milk/peanut/hazelnut/chocolate/chili item
 * walks, the 4-form sugar walk). The fermenter delta is pinned at ZERO: :605-:610
 * are the main smoke rows and :611-:649 the recipe-data-b2b1 pour — only-add-
 * never-duplicate is enforced by this test never touching that file.
 */
public class GT6RecipeMapDataFoodT2RowsPourTest extends GTRecipesOfflineTestBase {

	private static final java.util.function.Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final java.util.function.Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

	/** The per-map census of this card: file key -> expected total rows (smoke + pour, zero skips). */
	private static final Map<String, Integer> CENSUS = Map.of(
			"distillery", 1499, // :559-579 the 21-row food face + the recipe-b2-bath-potion-domain :35-:323 potion replay (1478 rows) — the ratchet bump is the b2 card's pour receipt
			"mixer", 216,     // :727, :739-:744, :746-:828 fluid legs with the item-form and fluid-set walks expanded, minus the unregistered gemChipped Sugar form (new file)
			"melter", 10,     // 1 smoke (Loader_Recipes_Chem.java:486) + :699-707 the 8 oils + the ice melt
			"smelter", 10);   // 1 smoke (Loader_Recipes_Chem.java:501) + :709-717 the same face on the Smelter map

	/** The file-head declarations: every file must keep naming its declared-out blockers. */
	private static final Map<String, String> DECLARATIONS = Map.of(
			"distillery", ":582-:583 coffee rows",
			"mixer", "Sap_Rainbow",
			"melter", "only-add-never-duplicate",
			"smelter", "only-add-never-duplicate");

	@BeforeEach
	void freshGeneration() {
		GT6RecipeMaps.init();
		GT6RecipeMapJsonLoader.resetForTest();
		GT6RecipeMapJsonLoader.sItemResolver = aId ->
				"gt6".equals(aId.getNamespace()) ? Items.IRON_INGOT : Items.BRICK; // identity stand-ins
		GT6RecipeMapJsonLoader.sFluidResolver = aId -> Fluids.WATER;
	}

	@AfterEach
	void teardownGeneration() {
		GT6RecipeMapJsonLoader.sItemResolver = sDefaultItems; // restore the live seams (JUL test order is arbitrary)
		GT6RecipeMapJsonLoader.sFluidResolver = sDefaultFluids;
		GT6RecipeMaps.reset(); // the generation hook retires the JSON tracker WITH the maps
	}

	/** Reads one shipped file verbatim, pours it under its map key, returns its parsed row array. */
	private JsonArray pourShipped(String aKey) throws Exception {
		String tPath = "/data/gt6/recipe_maps/" + aKey + ".json";
		InputStream tStream = GT6RecipeMapDataFoodT2RowsPourTest.class.getResourceAsStream(tPath);
		assertNotNull(tStream, "the shipped file " + tPath + " rides the test classpath");
		String tJson = new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
		JsonObject tDoc = JsonParser.parseString(tJson).getAsJsonObject();
		GT6RecipeMapJsonLoader.pour(Map.of(ResourceLocation.fromNamespaceAndPath("gt6", aKey), tDoc.deepCopy()));
		return tDoc.getAsJsonArray("recipes");
	}

	/** The census: every file pours its full expected row count — no WARN-skipped rows, no truncation. */
	@Test
	public void everyFoodT2FilePoursItsFullCensusWithZeroSkips() throws Exception {
		for (String tKey : CENSUS.keySet()) {
			pourShipped(tKey);
			RecipeMap tMap = GT6RecipeMapJsonLoader.mapFor(tKey);
			assertNotNull(tMap, tKey + " resolves");
			assertEquals(CENSUS.get(tKey).intValue(), tMap.mRecipeList.size(), tKey + ": the map holds the census");
			assertEquals(CENSUS.get(tKey).intValue(), GT6RecipeMapJsonLoader.pouredCount(tKey),
					tKey + ": the tracker mirrors the map (a smaller number = WARN-skipped rows)");
		}
	}

	/** The declaration shape pin: the file-head comments keep naming the declared-out blockers. */
	@Test
	public void theFileHeadsKeepTheirDeclarationComments() throws Exception {
		for (String tKey : DECLARATIONS.keySet()) {
			InputStream tStream = GT6RecipeMapDataFoodT2RowsPourTest.class.getResourceAsStream("/data/gt6/recipe_maps/" + tKey + ".json");
			assertNotNull(tStream);
			JsonObject tDoc = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
			assertTrue(tDoc.has("comment") && tDoc.get("comment").getAsString().contains(DECLARATIONS.get(tKey)),
					tKey + ": the declaration comment names " + DECLARATIONS.get(tKey));
		}
	}

	/**
	 * Spot check 1 — the distillery face (Loader_Recipes_Food.java:559/:567/:578):
	 * the royal-jelly lead, the two-fluid-output maple row and the Diablo ladder.
	 */
	@Test
	public void distilleryLegsAreUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped("distillery");
		int tFoodFace = 0;
		for (JsonElement tElement : tRows) {
			if (tElement.getAsJsonObject().get("comment").getAsString().startsWith("Loader_Recipes_Food.java:")) tFoodFace++;
		}
		assertEquals(21, tFoodFace, "the whole :559-579 face lands (the recipe-b2-bath-potion-domain potion replay joined the file behind it)");
		JsonObject tJelly = findRow(tRows, "Loader_Recipes_Food.java:559 ");
		assertEquals("gt6:royal_jelly", tJelly.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("fluid").getAsString(), "RoyalJelly = the p31 \"royal_jelly\" normalization");
		assertEquals("gt6:distilled_water", tJelly.getAsJsonArray("fluidOutputs").get(0).getAsJsonObject().get("fluid").getAsString(), "DistW = \"ic2distilledwater\" -> the distilled_water carrier");
		assertEquals("gt6:dust_sugar", tJelly.getAsJsonArray("outputs").get(0).getAsJsonObject().get("item").getAsString());
		assertEquals(1, tJelly.getAsJsonArray("outputs").get(0).getAsJsonObject().get("count").getAsInt());
		JsonObject tMaple = findRow(tRows, "Loader_Recipes_Food.java:567 ");
		assertEquals(2, tMaple.getAsJsonArray("fluidOutputs").size(), "Sap_Maple -> Syrup_Maple + DistW, both fluid slots");
		assertEquals("gt6:maplesyrup", tMaple.getAsJsonArray("fluidOutputs").get(0).getAsJsonObject().get("fluid").getAsString());
		assertEquals(50, tMaple.getAsJsonArray("fluidOutputs").get(0).getAsJsonObject().get("amount").getAsInt());
		JsonObject tCowLevel = findRow(tRows, "Loader_Recipes_Food.java:578 ");
		assertEquals("gt6:dust_gunpowder", tCowLevel.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString());
		assertEquals("gt6:diablosauce_strong", tCowLevel.getAsJsonArray("fluidOutputs").get(0).getAsJsonObject().get("fluid").getAsString(), "Sauce_Cow_Level = \"potion.diablosauce.strong\" -> the collision-suffix carrier");
		assertEquals(64, tCowLevel.get("eut").getAsLong());
	}

	/**
	 * Spot check 2 — the oil/ice faces (Loader_Recipes_Food.java:699-:707/:709-:717):
	 * the olive carrier fold and the ice melt, on both maps (the Smelter anchors sit
	 * eight lines lower than the Melter ones).
	 */
	@Test
	public void oilAndIceLegsAreUpstreamVerbatim() throws Exception {
		JsonArray tMelter = pourShipped("melter");
		assertEquals(10, tMelter.size(), "melter: 1 smoke + 9 pour");
		assertEquals(8, countRowsMatching(tMelter, "Oil_"), "melter: the eight cooking oils");
		JsonObject tOlive = findRow(tMelter, "Loader_Recipes_Food.java:700 ");
		assertEquals("gt6:juiceolive", tOlive.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("fluid").getAsString(), "Oil_Olive = \"binnie.juiceolive\"");
		assertEquals("gt6:hotfryingoil", tOlive.getAsJsonArray("fluidOutputs").get(0).getAsJsonObject().get("fluid").getAsString(), "Oil_Frying = \"hotfryingoil\"");
		assertEquals(1, tOlive.get("duration").getAsLong());
		JsonObject tIce = findRow(tMelter, "Loader_Recipes_Food.java:707 ");
		assertEquals("gt6:ice", tIce.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("fluid").getAsString());
		assertEquals("minecraft:water", tIce.getAsJsonArray("fluidOutputs").get(0).getAsJsonObject().get("fluid").getAsString());
		JsonArray tSmelter = pourShipped("smelter");
		assertEquals(10, tSmelter.size(), "smelter: 1 smoke + 9 pour");
		assertEquals(8, countRowsMatching(tSmelter, "Oil_"), "smelter: the eight cooking oils");
		JsonObject tOliveS = findRow(tSmelter, "Loader_Recipes_Food.java:710 ");
		assertEquals("gt6:juiceolive", tOliveS.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("fluid").getAsString());
		JsonObject tIceS = findRow(tSmelter, "Loader_Recipes_Food.java:717 ");
		assertEquals("gt6:ice", tIceS.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("fluid").getAsString());
	}

	/**
	 * Spot check 3 — the mixer band leads (Loader_Recipes_Food.java:727/:740/:747/:759):
	 * the mash blend, the pina colada triple, one milk-walk leg and the chocolate
	 * cream leg (the four chocolate-fluid rows ride the same :755 walk).
	 */
	@Test
	public void mixerBandLeadsAreUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped("mixer");
		JsonObject tMash = findRow(tRows, "Loader_Recipes_Food.java:727 ");
		assertEquals("gt6:mashwheat", tMash.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("fluid").getAsString(), "Mash_Wheat = \"binnie.mashwheat\"");
		assertEquals("gt6:wheathopsmash", tMash.getAsJsonArray("fluidOutputs").get(0).getAsJsonObject().get("fluid").getAsString());
		JsonObject tColada = findRow(tRows, "Loader_Recipes_Food.java:740 ");
		assertEquals(3, tColada.getAsJsonArray("fluidInputs").size(), "Juice_Ananas + Cream_Coconut + Rum_White");
		assertEquals("gt6:coconutcream", tColada.getAsJsonArray("fluidInputs").get(1).getAsJsonObject().get("fluid").getAsString());
		assertEquals("gt6:pina_colada", tColada.getAsJsonArray("fluidOutputs").get(0).getAsJsonObject().get("fluid").getAsString(), "Pina_Colada = \"pina.colada\"");
		JsonObject tMilkWalk = findRow(tRows, "Loader_Recipes_Food.java:746 — Milk dust walk (dust_small_milk)");
		assertEquals("gt6:dust_small_milk", tMilkWalk.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString());
		assertEquals(4, tMilkWalk.getAsJsonArray("inputs").get(0).getAsJsonObject().get("count").getAsInt());
		assertEquals(3, countRows(tRows, "Loader_Recipes_Food.java:746 "), "the milk dust walk = 3 forms (dust/dustSmall x4/dustTiny x9)");
		JsonObject tChocoCream = findRow(tRows, "Loader_Recipes_Food.java:759 ");
		assertEquals("gt6:chocolatecream", tChocoCream.getAsJsonArray("fluidOutputs").get(0).getAsJsonObject().get("fluid").getAsString());
		assertEquals(12, countRowsMatching(tRows, "Loader_Recipes_Food\\.java:75[5-9] "), "the chocolate walk = 3 forms x 4 fluid rows");
		assertEquals(21, countRowsMatching(tRows, "Loader_Recipes_Food\\.java:76[2-8] "), "the sugar walk = 3 registered forms x 7 rows (the gemChipped form stays out)");
	}

	/**
	 * Spot check 4 — the mixer walks (Loader_Recipes_Food.java:775/:776/:828):
	 * the 11x4 COOKING_OIL x VINEGAR expansion and the 37-member FRUIT_JUICE
	 * smoothie walk.
	 */
	@Test
	public void mixerWalksPinTheirPortUniverseCounts() throws Exception {
		JsonArray tRows = pourShipped("mixer");
		assertEquals(44, countRows(tRows, "Loader_Recipes_Food.java:775 "), "the Dressing walk = 11 COOKING_OIL x 4 VINEGAR");
		assertEquals(44, countRows(tRows, "Loader_Recipes_Food.java:776 "), "the Sauce_BBQ walk = 11 COOKING_OIL x 4 VINEGAR");
		JsonObject tDressing = findRow(tRows, "Loader_Recipes_Food.java:775 — COOKING_OIL x VINEGAR walk (coconutmilk + vinegar)");
		assertEquals("gt6:dust_salt", tDressing.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString(), "MT.NaCl = the \"Salt\" internal name");
		assertEquals("gt6:coconutmilk", tDressing.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("fluid").getAsString(), "Juice_Coconut carries the COOKING_OIL flag too");
		assertEquals("gt6:dressing", tDressing.getAsJsonArray("fluidOutputs").get(0).getAsJsonObject().get("fluid").getAsString(), "Dressing = \"potion.dressing\"");
		assertEquals(37, countRows(tRows, "Loader_Recipes_Food.java:828 "), "the FRUIT_JUICE smoothie walk = 37 port members (the B2a list)");
		assertEquals(35, countRowsMatching(tRows, "Ice 125 \\+ .* 125 -> Smoothie "), "the :792-826 smoothie table = 35 rows");
		assertEquals(8, countRowsMatching(tRows, "Loader_Recipes_Food\\.java:7(79|8[0-7]) "), "the laxative pair table = 8 rows (:779-787)");
		assertEquals(0, countRows(tRows, "Loader_Recipes_Food.java:789"), "the Sap_Rainbow Med_Heal row stays declared-out");
	}

	/**
	 * The fermenter reconciliation face: the :605-649 segment of the T2 list is fully
	 * covered elsewhere — :605-:610 ride the main smoke rows, :611-:649 landed with the
	 * recipe-data-b2b1 pour (the branch's fermenter only-add, never duplicate) — so
	 * THIS card ships zero fermenter rows, and the pin rides the landed b2b1 census.
	 */
	@Test
	public void theFermenterDeltaStaysEmptyAgainstTheB2b1Pour() throws Exception {
		InputStream tStream = GT6RecipeMapDataFoodT2RowsPourTest.class.getResourceAsStream("/data/gt6/recipe_maps/fermenter.json");
		assertNotNull(tStream);
		JsonObject tDoc = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		assertEquals(195, tDoc.getAsJsonArray("recipes").size(), "fermenter.json holds the landed b2b1 pour (195 = 7 smoke + 188; the :611-649 drinks/walks landed with recipe-data-b2b1 — the seat-IX reconciliation bump)");
	}

	/** The live-id face, offline: every gt6 item id the four files reference must be a member
	 * of the registration walks' id universe (GTMaterialItems + GTMaterialBlocks
	 * registrationOrder — the b1/b2a/b2b1 id-universe pattern; vanilla ids resolve live). */
	@Test
	public void everyGt6ItemIdTheFoodT2FilesReferenceIsRegistered() throws Exception {
		gregtech6.registry.GTMaterialItems.initMaterials();
		java.util.Set<String> tUniverse = new java.util.HashSet<>();
		for (gregtech6.registry.GTMaterialItems.PrefixMaterial tPair : gregtech6.registry.GTMaterialItems.registrationOrder()) {
			tUniverse.add("gt6:" + gregtech6.registry.GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		}
		for (gregtech6.registry.GTMaterialItems.PrefixMaterial tPair : gregtech6.registry.GTMaterialBlocks.registrationOrder()) {
			tUniverse.add("gt6:" + gregtech6.registry.GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		}
		assertTrue(tUniverse.contains("gt6:dust_sugar"), "the id universe built (" + tUniverse.size() + " ids)");

		java.util.Set<String> tMissing = new java.util.TreeSet<>();
		for (String tKey : CENSUS.keySet()) {
			InputStream tStream = GT6RecipeMapDataFoodT2RowsPourTest.class.getResourceAsStream("/data/gt6/recipe_maps/" + tKey + ".json");
			assertNotNull(tStream);
			JsonObject tDoc = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
			for (JsonElement tElement : tDoc.getAsJsonArray("recipes")) {
				JsonObject tRow = tElement.getAsJsonObject();
				for (String tLeg : new String[] {"inputs", "outputs"}) {
					if (!tRow.has(tLeg)) continue;
					for (JsonElement tSlot : tRow.getAsJsonArray(tLeg)) {
						String tId = tSlot.getAsJsonObject().get("item").getAsString();
						if (tId.startsWith("gt6:") && !tUniverse.contains(tId)) tMissing.add(tKey + ": " + tId);
					}
				}
			}
		}
		assertTrue(tMissing.isEmpty(), "unregistered gt6 ids (these rows would WARN-skip live): " + tMissing);
	}

	// ------------------------------------------------------------------ helpers

	private static int countRows(JsonArray aRows, String aCommentPrefix) {
		int rCount = 0;
		for (JsonElement tElement : aRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (tRow.has("comment") && tRow.get("comment").getAsString().startsWith(aCommentPrefix)) rCount++;
		}
		return rCount;
	}

	/** Counts rows whose comment matches the given regex (for the multi-line walk faces). */
	private static int countRowsMatching(JsonArray aRows, String aRegex) {
		int rCount = 0;
		for (JsonElement tElement : aRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (tRow.has("comment") && tRow.get("comment").getAsString().matches(".*" + aRegex + ".*")) rCount++;
		}
		return rCount;
	}

	/** Finds the first row whose comment starts with the given source citation. */
	private static JsonObject findRow(JsonArray aRows, String aCommentPrefix) {
		for (JsonElement tElement : aRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (tRow.has("comment") && tRow.get("comment").getAsString().startsWith(aCommentPrefix)) return tRow;
		}
		throw new AssertionError("no row cited " + aCommentPrefix);
	}
}
