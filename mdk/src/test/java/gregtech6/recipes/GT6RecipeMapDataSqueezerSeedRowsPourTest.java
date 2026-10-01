package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.fluid.GTFluids;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * The recipe-data squeezer-seed-legs row-stock pour test (the cbc-5 leftover (4)): the
 * FOUR VANILLA SEED-OIL LEGS of the {@code Loader_Recipes_Crops.java:256-305} oredict
 * seed fan, poured into the SQUEEZER map under the existing "squeezer" key (the tail-append
 * after the two smoke rows; the juicer.json rows 4-7 are the frozen folding-ruling twins —
 * upstream pours BOTH machines identically per listener, :273-274/:263-264/:268-269/:278-279).
 *
 * <p>The folding ruling (frozen in juicer.json, the r8-hotfix-juicer precedent): 1.7.10
 * vanilla seeds oredict as seedWheat/seedMelon/seedPumpkin and land on their listener
 * tiers (50/30/60 mB of FL.Oil_Seed); 1.7.10 vanilla had NO beetroot (seedBeet was
 * mod-foreign produce), so minecraft:beetroot_seeds rides the seedRice family tier
 * (:266-269) at 40 mB. Engine semantics: addRecipe1(T, 16, 16, seed, NF, FL.Oil_Seed(N),
 * ZL_IS) = duration 16, eut 16, no item outputs; FL.Oil_Seed is the port fluid gt6:seedoil
 * (GTFluids.java:1101, the aqua-spec carrier).
 */
public class GT6RecipeMapDataSqueezerSeedRowsPourTest extends GTRecipesOfflineTestBase {

	private static final String FILE_KEY = "squeezer";

	/** The frozen snapshot census: the 2 smoke rows (the earlier phase stock) + the 4 seed legs. */
	private static final int CENSUS = 6;

	/**
	 * The folding-ruling table: the four vanilla seeds → their listener tier amounts
	 * (Loader_Recipes_Crops.java:273 seedWheat 50 / :263 seedMelon 30 / :268 seedRice-beetroot
	 * 40 / :278 seedPumpkin 60). The live-walk cross-check walks THIS table against the
	 * shipped rows, both directions.
	 */
	private static final Map<String, Long> SEED_TIERS = new LinkedHashMap<>();
	static {
		SEED_TIERS.put("minecraft:wheat_seeds", 50L);
		SEED_TIERS.put("minecraft:melon_seeds", 30L);
		SEED_TIERS.put("minecraft:beetroot_seeds", 40L);
		SEED_TIERS.put("minecraft:pumpkin_seeds", 60L);
	}

	/** The vanilla registry keys of the four seed items — the item-universe face, both directions. */
	private static final Map<String, Item> SEED_ITEMS = new LinkedHashMap<>();
	static {
		SEED_ITEMS.put("minecraft:wheat_seeds", Items.WHEAT_SEEDS);
		SEED_ITEMS.put("minecraft:melon_seeds", Items.MELON_SEEDS);
		SEED_ITEMS.put("minecraft:beetroot_seeds", Items.BEETROOT_SEEDS);
		SEED_ITEMS.put("minecraft:pumpkin_seeds", Items.PUMPKIN_SEEDS);
	}

	private static final java.util.function.Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final java.util.function.Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

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

	/** Reads the shipped file verbatim, pours it under its map key, returns its parsed row array. */
	private JsonArray pourShipped() throws Exception {
		String tPath = "/data/gt6/recipe_maps/" + FILE_KEY + ".json";
		InputStream tStream = GT6RecipeMapDataSqueezerSeedRowsPourTest.class.getResourceAsStream(tPath);
		assertNotNull(tStream, "the shipped file " + tPath + " rides the test classpath");
		String tJson = new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
		JsonObject tDoc = JsonParser.parseString(tJson).getAsJsonObject();
		GT6RecipeMapJsonLoader.pour(Map.of(new ResourceLocation("gt6", FILE_KEY), tDoc.deepCopy()));
		return tDoc.getAsJsonArray("recipes");
	}

	/** The census ratchet: the file pours its full expected row count — no WARN-skipped rows, no truncation. */
	@Test
	public void theFilePoursItsFullCensusWithZeroSkips() throws Exception {
		pourShipped();
		RecipeMap tMap = GT6RecipeMapJsonLoader.mapFor(FILE_KEY);
		assertNotNull(tMap, FILE_KEY + " resolves");
		assertEquals(GT6RecipeMaps.SQUEEZER, tMap, FILE_KEY + ": the key pours into the SQUEEZER map");
		assertEquals(CENSUS, tMap.mRecipeList.size(), FILE_KEY + ": the map holds the census (2 smoke + 4 seed legs)");
		assertEquals(CENSUS, GT6RecipeMapJsonLoader.pouredCount(FILE_KEY),
				FILE_KEY + ": the tracker mirrors the map (a smaller number = WARN-skipped rows)");
	}

	/** The file-head declaration pins: the seed-fan anchor, the folding ruling, the declared absence, provenance. */
	@Test
	public void theFileHeadKeepsItsDeclarations() throws Exception {
		InputStream tStream = GT6RecipeMapDataSqueezerSeedRowsPourTest.class.getResourceAsStream("/data/gt6/recipe_maps/" + FILE_KEY + ".json");
		assertNotNull(tStream);
		JsonObject tDoc = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		assertTrue(tDoc.has("comment"), FILE_KEY + ": the file head carries a comment member");
		String tComment = tDoc.get("comment").getAsString();
		String tLower = tComment.toLowerCase(java.util.Locale.ROOT);
		// the card anchor
		assertTrue(tComment.contains("Loader_Recipes_Crops.java:256-305"), "the oredict seed fan anchor");
		// the four tier rows, each cited
		assertTrue(tComment.contains(":271-274"), "the seedWheat family tier (50 mB)");
		assertTrue(tComment.contains(":261-264"), "the seedMelon family tier (30 mB)");
		assertTrue(tComment.contains(":266-269"), "the seedRice family tier beetroot rides (40 mB)");
		assertTrue(tComment.contains(":276-279"), "the seedPumpkin family tier (60 mB)");
		// the method declarations
		assertTrue(tLower.contains("folding ruling"), "the beetroot tier mapping is declared a ruling, not an upstream literal");
		assertTrue(tLower.contains("no beetroot"), "the 1.7.10 vanilla-absence ground for the ruling");
		assertTrue(tLower.contains("juicer.json"), "the twin-file provenance (rows 4-7)");
		assertTrue(tLower.contains("gt6:seedoil"), "the FL.Oil_Seed carrier is declared");
		assertTrue(tLower.contains("declared absence"), "the mod-seed rest of the fan is declared (the b2_backlog seed-crop-items card)");
		assertTrue(tLower.contains("pinned per-file"), "the provenance ruling names this test");
	}

	/**
	 * Verbatim pins — the four seed legs full-row (input, seedoil tier amount, duration 16,
	 * eut 16, NO item outputs — the ZL_IS face), plus the two pre-existing smoke rows so the
	 * tail-append is proven not to have disturbed the earlier phase stock.
	 */
	@Test
	public void theSeedRowsAreUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped();
		for (Map.Entry<String, Long> tTier : SEED_TIERS.entrySet()) {
			JsonObject tRow = findRow(tRows, tTier.getKey());
			assertEquals("gt6:seedoil", rowFluidOutput(tRow), tTier.getKey() + ": FL.Oil_Seed = gt6:seedoil (GTFluids.java:1101)");
			assertEquals(tTier.getValue().longValue(), rowFluidAmount(tRow), tTier.getKey() + ": the listener tier amount");
			assertEquals(16L, tRow.get("duration").getAsLong(), tTier.getKey() + ": the addRecipe1 duration literal");
			assertEquals(16L, tRow.get("eut").getAsLong(), tTier.getKey() + ": the addRecipe1 eut literal");
			assertTrue(!tRow.has("outputs") || tRow.getAsJsonArray("outputs").size() == 0,
					tTier.getKey() + ": ZL_IS = no item outputs");
			assertTrue(!tRow.has("fluidInputs"), tTier.getKey() + ": NF = no fluid input");
		}
		// the smoke rows, pinned against tail-append disturbance
		JsonObject tApple = findRow(tRows, "minecraft:apple");
		assertEquals("minecraft:stick", tApple.getAsJsonArray("outputs").get(0).getAsJsonObject().get("item").getAsString());
		assertEquals(50, rowFluidAmount(tApple), "the apple smoke row amount");
		assertEquals(16L, tApple.get("duration").getAsLong(), "the apple smoke row duration");
		JsonObject tMelon = findRow(tRows, "minecraft:melon_slice");
		assertEquals(100, rowFluidAmount(tMelon), "the melon_slice smoke row amount");
		assertEquals(512L, tMelon.get("duration").getAsLong(), "the melon_slice smoke row duration");
	}

	/**
	 * The live-walk cross-check: the shipped seed rows match the folding table EXACTLY, both
	 * directions — a missing leg, a stale leg, or a swapped tier fails here before it fails live.
	 */
	@Test
	public void theShippedSeedRowsMatchTheFoldingTable() throws Exception {
		Map<String, Long> tShipped = new LinkedHashMap<>();
		for (JsonElement tElement : pourShipped()) {
			JsonObject tRow = tElement.getAsJsonObject();
			String tItem = tRow.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString();
			if (SEED_TIERS.containsKey(tItem)) {
				tShipped.put(tItem, rowFluidAmount(tRow));
			}
		}
		assertEquals(SEED_TIERS, tShipped, "the shipped seed legs equal the folding table, order and amounts");
	}

	/** The fluid-universe face: gt6:seedoil resolves in the GTFluids spec tables (the cut-test shape). */
	@Test
	public void everyFluidIdTheFileReferencesIsRegistered() throws Exception {
		Set<String> tMissing = new HashSet<>();
		for (JsonElement tElement : pourShipped()) {
			JsonObject tRow = tElement.getAsJsonObject();
			for (JsonElement tSlot : tRow.getAsJsonArray("fluidOutputs")) {
				String tId = tSlot.getAsJsonObject().get("fluid").getAsString();
				if (tId.equals("minecraft:water")) continue; // the vanilla carrier resolves live (the generify precedent)
				if (!fluidRegistered(tId.substring("gt6:".length()))) tMissing.add(tId);
			}
		}
		assertTrue(tMissing.isEmpty(), "unregistered gt6 fluid ids (these rows would WARN-skip live): " + tMissing);
	}

	/**
	 * The item-universe face: every minecraft: item id in the file resolves in the live
	 * vanilla registry, and the four seed ids equal the registry keys of their Items
	 * constants — a typo or a tier-row swap cannot pass (the offline boot binds vanilla items).
	 */
	@Test
	public void everyItemIdTheFileReferencesIsRegistered() throws Exception {
		Set<String> tSeen = new HashSet<>();
		for (JsonElement tElement : pourShipped()) {
			JsonObject tRow = tElement.getAsJsonObject();
			for (String tLeg : new String[] {"inputs", "outputs"}) {
				if (!tRow.has(tLeg)) continue;
				for (JsonElement tSlot : tRow.getAsJsonArray(tLeg)) {
					String tId = tSlot.getAsJsonObject().get("item").getAsString();
					assertTrue(tId.startsWith("minecraft:"), tId + ": this file's stock is all-vanilla on the item face");
					assertTrue(BuiltInRegistries.ITEM.containsKey(new ResourceLocation(tId)), tId + " resolves in the vanilla registry");
					tSeen.add(tId);
				}
			}
		}
		for (Map.Entry<String, Item> tSeed : SEED_ITEMS.entrySet()) {
			ResourceLocation tKey = BuiltInRegistries.ITEM.getKey(tSeed.getValue());
			assertEquals(tSeed.getKey(), tKey.toString(), tSeed.getValue() + ": the folding table pins the registry key");
			assertTrue(tSeen.contains(tSeed.getKey()), tSeed.getKey() + " is shipped in the file");
		}
	}

	/** The offline fluid-universe lookup: the union of the GTFluids spec-table lookups (the cut-test shape). */
	private static boolean fluidRegistered(String aPath) {
		return GTFluids.aquaSpec(aPath) != null || GTFluids.engineSpec(aPath) != null
				|| GTFluids.chemicalSpec(aPath) != null || GTFluids.simpleLiquidSpec(aPath) != null
				|| GTFluids.lubricantSpec(aPath) != null || GTFluids.foodSpec(aPath) != null
				|| GTFluids.foodB1Spec(aPath) != null; // gt6:seedoil lives HERE (the :1101 foodB1 spec row)
	}

	// ------------------------------------------------------------- the row lookups

	private static JsonObject findRow(JsonArray aRows, String aInputItem) {
		for (JsonElement tElement : aRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (aInputItem.equals(tRow.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString())) return tRow;
		}
		throw new AssertionError("no row with input " + aInputItem);
	}

	private static String rowFluidOutput(JsonObject aRow) {
		return aRow.getAsJsonArray("fluidOutputs").get(0).getAsJsonObject().get("fluid").getAsString();
	}

	private static long rowFluidAmount(JsonObject aRow) {
		return aRow.getAsJsonArray("fluidOutputs").get(0).getAsJsonObject().get("amount").getAsLong();
	}
}
