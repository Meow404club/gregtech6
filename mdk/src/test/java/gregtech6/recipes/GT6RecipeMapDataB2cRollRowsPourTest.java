package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.oredict.OreDictMaterial;
import gregtech6.registry.GTMaterialItems;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * The recipe-data-b2c-roll row-stock pour test (the B2a/B2b1 posture): the two
 * handler-expansion map files of this pilot card — rollformer (28 rows) and
 * clustermill (307) — pour through the real {@link GT6RecipeMapJsonLoader} seam
 * with zero skips; the file-head declarations keep naming the handler→JSON
 * deviation (lazy per-material generation frozen to the port-universe snapshot);
 * the verbatim spot checks read the shipped rows directly; and the live-walk
 * cross-check recomputes the handler walk against the live registration
 * (GTMaterialItems.registrationOrder + the TD.Processing/TD.Atomic/TD.Compounds
 * condition gates) and fails the moment this frozen snapshot trails it.
 */
public class GT6RecipeMapDataB2cRollRowsPourTest extends GTRecipesOfflineTestBase {

	private static final java.util.function.Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final java.util.function.Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

	/** The per-map census of this card: file key -> expected total rows (zero skips). */
	private static final Map<String, Integer> CENSUS = Map.of(
			"rollformer", 28,   // 8 easy + 20 hard: the :316/:314 plate->railGt legs over the plate∧railGt registration
			"clustermill", 307); // 87 easy + 220 hard: the :311/:309 plate->foil legs over the plate∧foil registration

	/** The per-leg citation census: the walk rows carry their upstream statement in the row comment. */
	private static final Map<String, Map<String, Integer>> LEG_CENSUS = Map.of(
			"clustermill", Map.of("Loader_Recipes_Handlers.java:311", 87, "Loader_Recipes_Handlers.java:309", 220),
			"rollformer", Map.of("Loader_Recipes_Handlers.java:316", 8, "Loader_Recipes_Handlers.java:314", 20));

	/** The file-head declarations: the deviation class + the replaced smoke rows must stay named. */
	private static final Map<String, String> DECLARATIONS = Map.of(
			"rollformer", "DECLARED DEVIATION",
			"clustermill", "DECLARED DEVIATION");

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
		InputStream tStream = GT6RecipeMapDataB2cRollRowsPourTest.class.getResourceAsStream(tPath);
		assertNotNull(tStream, "the shipped file " + tPath + " rides the test classpath");
		String tJson = new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
		JsonObject tDoc = JsonParser.parseString(tJson).getAsJsonObject();
		GT6RecipeMapJsonLoader.pour(Map.of(new ResourceLocation("gt6", aKey), tDoc.deepCopy()));
		return tDoc.getAsJsonArray("recipes");
	}

	/** The census: every file pours its full expected row count — no WARN-skipped rows, no truncation. */
	@Test
	public void everyB2cRollFilePoursItsFullCensusWithZeroSkips() throws Exception {
		for (String tKey : CENSUS.keySet()) {
			pourShipped(tKey);
			RecipeMap tMap = GT6RecipeMapJsonLoader.mapFor(tKey);
			assertNotNull(tMap, tKey + " resolves");
			assertEquals(CENSUS.get(tKey).intValue(), tMap.mRecipeList.size(), tKey + ": the map holds the census");
			assertEquals(CENSUS.get(tKey).intValue(), GT6RecipeMapJsonLoader.pouredCount(tKey),
					tKey + ": the tracker mirrors the map (a smaller number = WARN-skipped rows)");
		}
	}

	/** The declaration shape pin: the file heads keep naming the handler→JSON deviation. */
	@Test
	public void theFileHeadsKeepTheirDeclarationComments() throws Exception {
		for (String tKey : DECLARATIONS.keySet()) {
			InputStream tStream = GT6RecipeMapDataB2cRollRowsPourTest.class.getResourceAsStream("/data/gt6/recipe_maps/" + tKey + ".json");
			assertNotNull(tStream);
			JsonObject tDoc = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
			assertTrue(tDoc.has("comment"), tKey + ": the file head carries a comment member");
			String tComment = tDoc.get("comment").getAsString();
			String tLower = tComment.toLowerCase(java.util.Locale.ROOT);
			assertTrue(tLower.contains(DECLARATIONS.get(tKey).toLowerCase(java.util.Locale.ROOT)),
					tKey + ": the declaration names " + DECLARATIONS.get(tKey));
			assertTrue(tLower.contains("frozen"), tKey + ": the declaration names the frozen-universe semantics");
			assertTrue(tComment.contains("Loader_Recipes_Handlers.java:3"), tKey + ": the declaration cites the handler rows");
			assertTrue(tLower.contains("smoke row"), tKey + ": the declaration names the replaced smoke row");
		}
	}

	/** The per-leg citation census: each upstream statement's walk lands its expected row count. */
	@Test
	public void theLegCitationsCarryTheirCensus() throws Exception {
		for (Map.Entry<String, Map<String, Integer>> tFile : LEG_CENSUS.entrySet()) {
			JsonArray tRows = pourShipped(tFile.getKey());
			for (Map.Entry<String, Integer> tLeg : tFile.getValue().entrySet()) {
				int tCount = 0;
				for (JsonElement tElement : tRows) {
					JsonObject tRow = tElement.getAsJsonObject();
					if (tRow.has("comment") && tRow.get("comment").getAsString().startsWith(tLeg.getKey() + " ")) tCount++;
				}
				assertEquals(tLeg.getValue().intValue(), tCount, tFile.getKey() + ": the " + tLeg.getKey() + " leg census");
			}
		}
	}

	/**
	 * Spot check 1 — the iron pair on both maps is HARD leg (Iron is NEVER_FURNACE and not
	 * SOFT upstream, MT.java:414): duration = multiplier*(toolQuality+1) = 768/192, count 4.
	 */
	@Test
	public void ironHardLegsAreUpstreamVerbatim() throws Exception {
		JsonObject tCm = findRow(pourShipped("clustermill"), "gt6:foil_iron");
		assertEquals("gt6:plate_iron", tCm.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString());
		assertEquals(4, tCm.getAsJsonArray("outputs").get(0).getAsJsonObject().get("count").getAsInt());
		assertEquals(768, tCm.get("duration").getAsLong(), "clustermill hard: 256*(q+1), iron q=2");
		assertEquals(16, tCm.get("eut").getAsLong());
		JsonObject tRf = findRow(pourShipped("rollformer"), "gt6:rail_gt_iron");
		assertEquals(192, tRf.get("duration").getAsLong(), "rollformer hard: 64*(q+1), iron q=2");
		assertTrue(tRf.get("comment").getAsString().startsWith("Loader_Recipes_Handlers.java:314"),
				"the iron rail row cites the hard-leg statement");
	}

	/**
	 * Spot check 2 — the easy legs: copper (SOFT+FURNACE, MT.java:418) rolls foil at the
	 * fixed 16; aluminium (SOFT+FURNACE) rides both maps' easy legs.
	 */
	@Test
	public void easyLegsAreUpstreamVerbatim() throws Exception {
		JsonObject tCopper = findRow(pourShipped("clustermill"), "gt6:foil_copper");
		assertEquals(16, tCopper.get("duration").getAsLong(), "clustermill easy: mDuration 16 fixed");
		assertTrue(tCopper.get("comment").getAsString().startsWith("Loader_Recipes_Handlers.java:311"),
				"the copper foil row cites the easy-leg statement");
		JsonObject tAlu = findRow(pourShipped("rollformer"), "gt6:rail_gt_aluminium");
		assertEquals(16, tAlu.get("duration").getAsLong(), "rollformer easy: 16 fixed");
	}

	/** Spot check 3 — the tungstensteel pair (q=4): the largest common hard-leg durations. */
	@Test
	public void tungstensteelHardLegsScaleWithToolQuality() throws Exception {
		assertEquals(1280, findRow(pourShipped("clustermill"), "gt6:foil_tungstensteel").get("duration").getAsLong());
		assertEquals(320, findRow(pourShipped("rollformer"), "gt6:rail_gt_tungstensteel").get("duration").getAsLong());
	}

	/**
	 * The live-walk cross-check — the heart of the frozen-snapshot deviation: recompute the
	 * handler walk against the LIVE registration (the RecipeMapHandlerPrefix.addRecipeForMaterial
	 * gates: condition :205, INVALID_MATERIAL :205, input/output item existence :209/:214; the
	 * addAllRecipesInternal universe :170-178 = the input-prefix registration) and require the
	 * shipped file to cover it EXACTLY (same material set, same leg split, same durations).
	 */
	@Test
	public void theShippedWalkMatchesTheLiveRegistrationWalk() throws Exception {
		GTMaterialItems.initMaterials();
		Set<String> tPlates = new HashSet<>(), tFoils = new HashSet<>(), tRails = new HashSet<>();
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			if (tPair.prefix() == OP.plate) tPlates.add(tPair.material().mNameInternal);
			else if (tPair.prefix() == OP.foil) tFoils.add(tPair.material().mNameInternal);
			else if (tPair.prefix() == OP.railGt) tRails.add(tPair.material().mNameInternal);
		}
		assertTrue(tPlates.size() >= 300, "the plate universe exists (" + tPlates.size() + " materials)");
		checkLiveWalk("clustermill", tPlates, tFoils, 256, false);
		checkLiveWalk("rollformer", tPlates, tRails, 64, true);
	}

	/** One map's live walk vs its shipped rows, exact both ways. */
	private void checkLiveWalk(String aKey, Set<String> aPlates, Set<String> aOutputs, long aMultiplier, boolean aNoCoated) throws Exception {
		List<String> tExpected = new ArrayList<>();
		for (String tMaterialName : aPlates) {
			if (!aOutputs.contains(tMaterialName)) continue; // the :214 output-existence gate
			OreDictMaterial tMaterial = materialByName(tMaterialName);
			if (tMaterial.contains(TD.Atomic.ANTIMATTER)) continue; // ANTIMATTER.NOT
			if (aNoCoated && tMaterial.contains(TD.Compounds.COATED)) continue; // COATED.NOT
			if (!tMaterial.contains(TD.Processing.SMITHABLE)) continue; // SMITHABLE
			if (tMaterial.contains(TD.Properties.INVALID_MATERIAL)) continue; // the :205 invalid gate
			boolean tEasy = tMaterial.contains(TD.Processing.FURNACE) || tMaterial.contains(TD.Properties.SOFT);
			long tDuration = tEasy ? 16 : aMultiplier * (tMaterial.mToolQuality + 1);
			String tSnake = GTMaterialItems.snakeCase(tMaterial.mNameInternal);
			tExpected.add("gt6:plate_" + tSnake + ">" + "gt6:" + (aKey.equals("clustermill") ? "foil" : "rail_gt") + "_" + tSnake + ">" + tDuration + ">16");
		}
		Set<String> tExpectedSet = new HashSet<>(tExpected);

		JsonArray tRows = pourShipped(aKey);
		Set<String> tShippedSet = new HashSet<>();
		for (JsonElement tElement : tRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			String tIn = tRow.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString();
			String tOut = tRow.getAsJsonArray("outputs").get(0).getAsJsonObject().get("item").getAsString();
			tShippedSet.add(tIn + ">" + tOut + ">" + tRow.get("duration").getAsLong() + ">" + tRow.get("eut").getAsLong());
		}
		Set<String> tMissing = new HashSet<>(tExpectedSet);
		tMissing.removeAll(tShippedSet);
		Set<String> tStale = new HashSet<>(tShippedSet);
		tStale.removeAll(tExpectedSet);
		assertTrue(tMissing.isEmpty(), aKey + ": the frozen snapshot trails the live walk (re-pour needed): " + tMissing);
		assertTrue(tStale.isEmpty(), aKey + ": the frozen snapshot holds rows the live walk no longer generates: " + tStale);
		assertEquals(tExpected.size(), tRows.size(), aKey + ": the snapshot size equals the live walk");
	}

	/** The id-universe face: every gt6 id the two files reference is a live registration id. */
	@Test
	public void everyGt6IdTheB2cRollFilesReferenceIsRegistered() throws Exception {
		GTMaterialItems.initMaterials();
		Set<String> tUniverse = new HashSet<>();
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			tUniverse.add("gt6:" + GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		}
		assertTrue(tUniverse.contains("gt6:plate_iron"), "the id universe built (" + tUniverse.size() + " ids)");
		Set<String> tMissing = new HashSet<>();
		for (String tKey : CENSUS.keySet()) {
			InputStream tStream = GT6RecipeMapDataB2cRollRowsPourTest.class.getResourceAsStream("/data/gt6/recipe_maps/" + tKey + ".json");
			assertNotNull(tStream);
			JsonObject tDoc = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
			for (JsonElement tElement : tDoc.getAsJsonArray("recipes")) {
				JsonObject tRow = tElement.getAsJsonObject();
				for (String tLeg : new String[] {"inputs", "outputs"}) {
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

	/** Finds the first row whose output slot carries the given item id. */
	private static JsonObject findRow(JsonArray aRows, String aOutputItem) {
		for (JsonElement tElement : aRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (aOutputItem.equals(tRow.getAsJsonArray("outputs").get(0).getAsJsonObject().get("item").getAsString())) return tRow;
		}
		throw new AssertionError("no row outputs " + aOutputItem);
	}

	/** Resolves a live material by its internal name (the registration is the single source). */
	private static OreDictMaterial materialByName(String aNameInternal) {
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			if (tPair.material().mNameInternal.equals(aNameInternal)) return tPair.material();
		}
		throw new AssertionError("material not in the registration: " + aNameInternal);
	}
}
