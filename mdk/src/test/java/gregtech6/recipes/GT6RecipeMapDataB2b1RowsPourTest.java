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
 * The recipe-data-b2b1 row-stock pour test (the GT6RecipeMapDataB2aRowsPourTest
 * posture): this card's big-stock map file — fermenter (195 rows = 7 smoke + 188
 * pour) — pours through the real {@link GT6RecipeMapJsonLoader} seam with zero skips,
 * the file-head declaration comments keep naming their declared-out blockers (the TRUE
 * NEGATIVE shape pin, generalized to partial declaration files), the verbatim spot
 * checks read the shipped rows directly, and the walk faces pin their port-universe
 * membership counts (the seven meat/fish prefixes, the 17 WINE members, the 37
 * FRUIT_JUICE members, the 70 potion rows).
 *
 * <p>SCOPE (the seat-IX rebase adjudication, task recipe-data-b2b1): the branch's
 * juicer/roasting twins were DUPLICATES of the already-landed b2-residual-maps walk
 * (GT6RecipeMapDataB2ResidualRowsPourTest owns those two files), and their fluid
 * amounts rode U=144 (the item unit) instead of the fluid U=1000 with full-dust
 * outputs where upstream uses the 7*U2 small-dust ladder (Loader_Recipes_Chem.java:398-440
 * read back). Main's files won wholesale; this test pins fermenter only.
 */
public class GT6RecipeMapDataB2b1RowsPourTest extends GTRecipesOfflineTestBase {

	private static final java.util.function.Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final java.util.function.Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

	/** The per-map census of this card: file key -> expected total rows (smoke + pour, zero skips). */
	private static final Map<String, Integer> CENSUS = Map.of(
			"fermenter", 195); // 7 smoke + 188 pour: the :61-89 meat walk, :611-645 drinks, :647/:649 walks, :357-426 potions, Chem :329

	/** The file-head declarations: every file must keep naming its declared-out blockers. */
	private static final Map<String, String> DECLARATIONS = Map.of(
			"fermenter", "RM.biomass :688-703");

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
		InputStream tStream = GT6RecipeMapDataB2b1RowsPourTest.class.getResourceAsStream(tPath);
		assertNotNull(tStream, "the shipped file " + tPath + " rides the test classpath");
		String tJson = new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
		JsonObject tDoc = JsonParser.parseString(tJson).getAsJsonObject();
		GT6RecipeMapJsonLoader.pour(Map.of(new ResourceLocation("gt6", aKey), tDoc.deepCopy()));
		return tDoc.getAsJsonArray("recipes");
	}

	/** The census: every file pours its full expected row count — no WARN-skipped rows, no truncation. */
	@Test
	public void everyB2b1FilePoursItsFullCensusWithZeroSkips() throws Exception {
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
			InputStream tStream = GT6RecipeMapDataB2b1RowsPourTest.class.getResourceAsStream("/data/gt6/recipe_maps/" + tKey + ".json");
			assertNotNull(tStream);
			JsonObject tDoc = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
			assertTrue(tDoc.has("comment") && tDoc.get("comment").getAsString().contains(DECLARATIONS.get(tKey)),
					tKey + ": the declaration comment names " + DECLARATIONS.get(tKey));
		}
	}

	/**
	 * Spot check 1 — one meat-walk leg (Loader_Recipes_Food.java:63): a raw meat dust
	 * rots into a rotten meat dust at 144 t / 16 EUt, items only.
	 */
	@Test
	public void meatWalkLegIsUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped("fermenter");
		assertEquals(28, countRowsMatching(tRows, "Loader_Recipes_Food\\.java:(6[1-9]|7[0-9]|8[0-9]) "), "the meat/fish walk lands 7 prefixes x 4 source materials");
		JsonObject tRow = findRow(tRows, "Loader_Recipes_Food.java:63 ");
		JsonArray tInputs = tRow.getAsJsonArray("inputs");
		assertEquals("gt6:dust_meat_raw", tInputs.get(0).getAsJsonObject().get("item").getAsString());
		assertEquals("gt6:dust_meat_rotten", tRow.getAsJsonArray("outputs").get(0).getAsJsonObject().get("item").getAsString());
		assertEquals(144, tRow.get("duration").getAsLong());
		assertEquals(16, tRow.get("eut").getAsLong());
	}

	/**
	 * Spot check 2 — the drink legs (Loader_Recipes_Food.java:618/:647): the Scotch
	 * rung and one WINE-walk member.
	 */
	@Test
	public void drinkAndWineWalkLegsAreUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped("fermenter");
		JsonObject tScotch = findRow(tRows, "Loader_Recipes_Food.java:618");
		assertEquals("gt6:whiskeywheat", tScotch.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("fluid").getAsString(), "Whiskey_Scotch = \"binnie.whiskeywheat\"");
		assertEquals("gt6:glenmckenner", tScotch.getAsJsonArray("fluidOutputs").get(0).getAsJsonObject().get("fluid").getAsString());
		assertEquals(10, tScotch.getAsJsonArray("fluidOutputs").get(0).getAsJsonObject().get("amount").getAsInt());
		assertEquals(128, tScotch.get("duration").getAsLong());
		assertEquals(17, countRows(tRows, "Loader_Recipes_Food.java:647"), "the WINE walk = 17 port members (18 upstream minus Wine_Fortified itself)");
		assertEquals(37, countRows(tRows, "Loader_Recipes_Food.java:649"), "the FRUIT_JUICE walk = 37 port members (the B2a list)");
	}

	/**
	 * Spot check 3 — the potion walk (Loader_Recipes_Potions.java): 70 rows, the
	 * Harm->damage id fold and the :402 plain-poison leg.
	 */
	@Test
	public void potionWalkLegsAreUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped("fermenter");
		assertEquals(70, countRows(tRows, "Loader_Recipes_Potions.java:"), "the potion walk = 70 rows (:357-426)");
		JsonObject tRow = findRow(tRows, "Loader_Recipes_Potions.java:360 ");
		assertEquals("gt6:potion.health", tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("fluid").getAsString(), "Potion_Heal_1");
		assertEquals("gt6:potion.damage", tRow.getAsJsonArray("fluidOutputs").get(0).getAsJsonObject().get("fluid").getAsString(), "Potion_Harm_1 = \"potion.damage\"");
		assertEquals(25, tRow.getAsJsonArray("fluidOutputs").get(0).getAsJsonObject().get("amount").getAsInt());
	}

	/** The live-id face, offline: every gt6 id the fermenter file references must be a member of
	 * the registration walks' id universe (GTMaterialItems + GTMaterialBlocks
	 * registrationOrder — the b1/b2a id-universe pattern; vanilla ids resolve live). */
	@Test
	public void everyGt6IdTheB2b1FilesReferenceIsRegistered() throws Exception {
		gregtech6.registry.GTMaterialItems.initMaterials();
		java.util.Set<String> tUniverse = new java.util.HashSet<>();
		for (gregtech6.registry.GTMaterialItems.PrefixMaterial tPair : gregtech6.registry.GTMaterialItems.registrationOrder()) {
			tUniverse.add("gt6:" + gregtech6.registry.GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		}
		for (gregtech6.registry.GTMaterialItems.PrefixMaterial tPair : gregtech6.registry.GTMaterialBlocks.registrationOrder()) {
			tUniverse.add("gt6:" + gregtech6.registry.GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		}
		// the plain (non-material) registered items this card's files reference: the bee combs (GT6BeeCombs.COMB_SPECS)
		for (gregtech6.registry.GT6BeeCombs.CombSpec tComb : gregtech6.registry.GT6BeeCombs.COMB_SPECS) {
			tUniverse.add("gt6:" + tComb.itemId());
		}
		assertTrue(tUniverse.contains("gt6:dust_meat_rotten"), "the id universe built (" + tUniverse.size() + " ids)");

		java.util.Set<String> tMissing = new java.util.TreeSet<>();
		for (String tKey : CENSUS.keySet()) {
			InputStream tStream = GT6RecipeMapDataB2b1RowsPourTest.class.getResourceAsStream("/data/gt6/recipe_maps/" + tKey + ".json");
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
