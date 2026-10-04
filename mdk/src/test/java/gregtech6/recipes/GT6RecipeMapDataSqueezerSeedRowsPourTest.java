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
 * The recipe-data squeezer row-stock pour test (the cbc-5 leftover (4), grown by the
 * recipe-data-b2b2 replay): the file under the "squeezer" key carries the FOUR VANILLA
 * SEED-OIL LEGS of the {@code Loader_Recipes_Crops.java:256-305} oredict seed fan PLUS the
 * 37 static b2b2 rows (the Vanilla :802-:839 flower/ice/juice faces + the Woods :122-:130
 * rubber-sap faces; the juicer.json rows 4-7 are the frozen folding-ruling twins — upstream
 * pours BOTH machines identically per listener, :273-274/:263-264/:268-269/:278-279). The
 * two fake apple/melon-slice smoke canaries retired with that replay (the true :802 melon
 * row is the real face; the b2b2 REPLACE posture the file head declares).
 *
 * <p>The folding ruling (frozen in juicer.json, the r8-hotfix-juicer precedent): 1.7.10
 * vanilla seeds oredict as seedWheat/seedMelon/seedPumpkin and land on their listener
 * tiers (50/30/60 mB of FL.Oil_Seed); 1.7.10 vanilla had NO beetroot (seedBeet was
 * mod-foreign produce), so minecraft:beetroot_seeds rides the seedRice family tier
 * (:266-269) at 40 mB. Engine semantics: addRecipe1(T, 16, 16, seed, NF, FL.Oil_Seed(N),
 * ZL_IS) = duration 16, eut 16, no item outputs; FL.Oil_Seed is the port fluid gt6:seedoil
 * (the foodB1-spec carrier). The id-universe faces ride the
 * {@link GT6RecipeMapDataB2b2RowsPourTest} posture: the full GTFluids spec-table union for
 * the fluid ids (the b2b2 rows reach the bee-row ice/latex, naming rainbowsap, foodB2
 * poison/golden-carrot and dye_chemical_* families on top of the seed-leg aqua/foodB1
 * faces) and the offline-built gt6 item universe for the gt6 dust/leaves ids.
 */
public class GT6RecipeMapDataSqueezerSeedRowsPourTest extends GTRecipesOfflineTestBase {

	private static final String FILE_KEY = "squeezer";

	/**
	 * The frozen snapshot census: the 4 seed legs (the cbc-5 tail) + the 37 static b2b2
	 * rows (the Vanilla :802-:839 minus the chanced :835-:837/:839 legs = 32, + the Woods
	 * :122-:130 rubber-sap faces = 5); the two fake smoke canaries retired with the b2b2
	 * replay (the GT6RecipeMapDataB2b2RowsPourTest REPLACE posture).
	 */
	private static final int CENSUS = 54; // +13 the recipe-b4 BlockFlowers band (8 A :90-:97 + 5 B :94-:98)

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
		assertEquals(CENSUS, tMap.mRecipeList.size(), FILE_KEY + ": the map holds the census (4 seed legs + 37 b2b2 static rows + 13 b4 flower rows)");
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
		assertTrue(tLower.contains("retires the two fake apple/melon-slice"), "the smoke-canary retirement declaration (the b2b2 replace posture)");
	}

	/**
	 * Verbatim pins — the four seed legs full-row (input, seedoil tier amount, duration 16,
	 * eut 16, NO item outputs — the ZL_IS face). The retired fake smoke canaries stay
	 * retired: the b2b2 replay superseded them with the true :802 melon row, and the
	 * negative walk keeps them from creeping back.
	 */
	@Test
	public void theSeedRowsAreUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped();
		for (Map.Entry<String, Long> tTier : SEED_TIERS.entrySet()) {
			JsonObject tRow = findRow(tRows, tTier.getKey());
			assertEquals("gt6:seedoil", rowFluidOutput(tRow), tTier.getKey() + ": FL.Oil_Seed = gt6:seedoil");
			assertEquals(tTier.getValue().longValue(), rowFluidAmount(tRow), tTier.getKey() + ": the listener tier amount");
			assertEquals(16L, tRow.get("duration").getAsLong(), tTier.getKey() + ": the addRecipe1 duration literal");
			assertEquals(16L, tRow.get("eut").getAsLong(), tTier.getKey() + ": the addRecipe1 eut literal");
			assertTrue(!tRow.has("outputs") || tRow.getAsJsonArray("outputs").size() == 0,
					tTier.getKey() + ": ZL_IS = no item outputs");
			assertTrue(!tRow.has("fluidInputs"), tTier.getKey() + ": NF = no fluid input");
		}
		// the retired smoke canaries stay retired (the b2b2 replace posture: no fake
		// apple/melon-slice stick+water pairs may re-append to the file)
		for (JsonElement tElement : tRows) {
			String tInput = tElement.getAsJsonObject().getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString();
			assertTrue(!tInput.equals("minecraft:apple") && !tInput.equals("minecraft:melon_slice"),
					tInput + ": the retired fake smoke canary stays retired");
		}
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
	 * vanilla registry, every gt6: id resolves in the offline-built gt6 id universe
	 * (GTMaterialItems + GTMaterialBlocks registration order + the tree block items — the
	 * GT6RecipeMapDataB2b2RowsPourTest posture; the b2b2 rows carry the gt6 dust/gem/leaves
	 * faces), and the four seed ids equal the registry keys of their Items constants — a
	 * typo or a tier-row swap cannot pass (the offline boot binds vanilla items).
	 */
	@Test
	public void everyItemIdTheFileReferencesIsRegistered() throws Exception {
		gregtech6.registry.GTMaterialItems.initMaterials();
		Set<String> tUniverse = new HashSet<>();
		for (gregtech6.registry.GTMaterialItems.PrefixMaterial tPair : gregtech6.registry.GTMaterialItems.registrationOrder()) {
			tUniverse.add("gt6:" + gregtech6.registry.GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		}
		for (gregtech6.registry.GTMaterialItems.PrefixMaterial tPair : gregtech6.registry.GTMaterialBlocks.registrationOrder()) {
			tUniverse.add("gt6:" + gregtech6.registry.GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		}
		for (var tItem : gregtech6.registry.GT6TreeBlocks.ITEMS) tUniverse.add("gt6:" + tItem.getId().getPath());
		// the flower band items are plain BlockItems, not material-prefix ids (the b4 BlockFlowers rows)
		for (var tFlower : gregtech6.registry.GT6SurfaceBlocks.FLOWER_ITEMS) tUniverse.add("gt6:" + tFlower.getId().getPath());
		// the vanilla-alias foodside family (the smoke-card remains legs on the seated vanilla rows)
		for (gregtech6.registry.GT6FoodsideItems.SideRow tRow : gregtech6.registry.GT6FoodsideItems.ROWS) {
			tUniverse.add("gt6:" + tRow.id());
		}
		assertTrue(tUniverse.size() > 50000, "the id universe built (" + tUniverse.size() + " ids)");

		Set<String> tSeen = new HashSet<>();
		for (JsonElement tElement : pourShipped()) {
			JsonObject tRow = tElement.getAsJsonObject();
			for (String tLeg : new String[] {"inputs", "outputs"}) {
				if (!tRow.has(tLeg)) continue;
				for (JsonElement tSlot : tRow.getAsJsonArray(tLeg)) {
					String tId = tSlot.getAsJsonObject().get("item").getAsString();
					if (tId.startsWith("minecraft:")) {
						assertTrue(BuiltInRegistries.ITEM.containsKey(new ResourceLocation(tId)), tId + " resolves in the vanilla registry");
					} else {
						assertTrue(tUniverse.contains(tId), tId + " resolves in the gt6 id universe");
					}
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

	/**
	 * The offline fluid-universe lookup: the union of ALL the GTFluids spec-table lookups
	 * (chemical/closure/hot/lubricant/honey/bee/qu/naming/aqua/simple-liquid/food families)
	 * plus the standalone chlorine row — the GT6RecipeMapDataB2b2RowsPourTest union: the
	 * b2b2 rows reach ice/latex (bee-row), rainbowsap (naming), poison/goldencarrotjuice
	 * (foodB2) and the dye_chemical_* family on top of the seed-leg aqua/foodB1 faces.
	 */
	private static boolean fluidRegistered(String aPath) {
		if (aPath.startsWith("dye_watermixed_") || aPath.startsWith("dye_flower_")) return true; // the GTFluids DyeFluid compose walk (the b4 juice rows)
		return GTFluids.chemicalSpec(aPath) != null || GTFluids.closureSpec(aPath) != null || GTFluids.hotSpec(aPath) != null
				|| GTFluids.lubricantSpec(aPath) != null || GTFluids.honeySpec(aPath) != null || GTFluids.beeRowSpec(aPath) != null
				|| GTFluids.quSpec(aPath) != null || GTFluids.namingSpec(aPath) != null
				|| GTFluids.aquaSpec(aPath) != null || GTFluids.simpleLiquidSpec(aPath) != null
				|| GTFluids.foodSpec(aPath) != null || GTFluids.foodB1Spec(aPath) != null || GTFluids.foodB2Spec(aPath) != null
				|| GTFluids.foodTailSpec(aPath) != null || GTFluids.engineSpec(aPath) != null
				|| GTFluids.dyeIndexOf(aPath) >= 0 || aPath.equals("chlorine");
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
