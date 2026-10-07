package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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

import gregapi.data.TD;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;

import gregtech6.registry.GTFluidPipes;
import gregtech6.registry.GTItemPipes;
import gregtech6.registry.GTMaterialBlocks;
import gregtech6.registry.GTMaterialItems;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * The recipe-data-b2c-magnet row-stock pour test (the B2c-weld posture, two maps): the
 * magneticseparator.json file (the RM.MagneticSeparator fixed rows :51-54/:58-59/:61-64 + the
 * polarity-asymmetry walk :454-457 of Loader_Recipes_Ores.java, 176 rows) and the sluice.json
 * file (the RM.Sluice ore walk :385-420 over the four FL.waters legs + the two Petrotheum
 * RecipeMapHandlerPrefix statements of Loader_Recipes_Handlers.java:672-673, 4930 rows) pour
 * through the real {@link GT6RecipeMapJsonLoader} seam with zero skips. RECOMPUTABILITY: the
 * rows were generated from a one-shot live registration dump (an uncommitted JUnit walking
 * GTMaterialItems.registrationOrder + the condition tags, folded by a /tmp generator — the
 * B2c zero-transcription method), and THIS test is the durable half: the live-walk
 * cross-checks recompute all three walk families against the live registration
 * (OreDictMaterial.mByProducts — the debt-byproducts-data face — plus the magnetic tags and
 * the UT.Code.select fallback) and fail the moment the frozen snapshot trails or outruns it.
 * The walk rows carry NO per-row comment (the cross-check IS their provenance); the 10 fixed
 * rows keep their per-line citations. The two TRUE NEGATIVE faces (the RH sand row
 * Loader_Recipes_Temporary.java:601, the Tropic sand row Compat_Recipes_Tropicraft.java:52)
 * are declared in the file heads, not poured. R2 (the review reversal): the :54/:59/:64
 * blockDust faces pour too — the r1 BLOCKED claim looked only at the GTMaterialItems item
 * path, but OP.blockDust lives on the GTMaterialBlocks block path
 * (blockPathPrefixes + the GTMaterialPrefixBlockItem same-id ITEM bridge, the weld r2
 * blockSolid precedent), and the id-universe check below runs the weld r2 THREE-universe form
 * so this class of miss cannot recur.
 */
public class GT6RecipeMapDataB2cMagnetRowsPourTest extends GTRecipesOfflineTestBase {

	private static final java.util.function.Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final java.util.function.Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

	/** The two map keys and the frozen census (the file-head Row census statements). */
	private static final String MAGNET = "magneticseparator";
	private static final String SLUICE = "sluice";
	private static final Map<String, Integer> CENSUS = Map.of(MAGNET, 176, SLUICE, 4930);

	/** The frozen per-source accounting (the generation-time face; the conscious-bump ratchet). */
	private static final Map<String, Integer> SOURCES = new LinkedHashMap<>();
	static {
		SOURCES.put(MAGNET + ".fixed:51", 1);
		SOURCES.put(MAGNET + ".fixed:52", 1);
		SOURCES.put(MAGNET + ".fixed:53", 1);
		SOURCES.put(MAGNET + ".fixed:54", 1);
		SOURCES.put(MAGNET + ".fixed:58", 1);
		SOURCES.put(MAGNET + ".fixed:59", 1);
		SOURCES.put(MAGNET + ".fixed:61", 1);
		SOURCES.put(MAGNET + ".fixed:62", 1);
		SOURCES.put(MAGNET + ".fixed:63", 1);
		SOURCES.put(MAGNET + ".fixed:64", 1);
		SOURCES.put(MAGNET + ".walk:454-457", 166);
		SOURCES.put(MAGNET + ".tn:rh601+tropic52", 0);
		SOURCES.put(SLUICE + ".walk:385-420", 3944);
		SOURCES.put(SLUICE + ".petro:672", 493);
		SOURCES.put(SLUICE + ".petro:673", 493);
	}

	/** The FL.waters leg order (FL.java:689) and the Petrotheum amounts (FL.java:449, 250 per Unit). */
	private static final String[][] WATERS = {{"minecraft:water", "900"}, {"gt6:mnwtr", "900"}, {"gt6:distilled_water", "900"}, {"gt6:spdew", "900"},
			{"minecraft:water", "100"}, {"gt6:mnwtr", "100"}, {"gt6:distilled_water", "100"}, {"gt6:spdew", "100"}};
	private static final Set<String> FLUID_UNIVERSE = Set.of("minecraft:water", "gt6:mnwtr", "gt6:distilled_water", "gt6:spdew", "gt6:sluicejuice", "gt6:petrotheum");

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
	private JsonArray pourShipped(String aKey) throws Exception {
		String tPath = "/data/gt6/recipe_maps/" + aKey + ".json";
		InputStream tStream = GT6RecipeMapDataB2cMagnetRowsPourTest.class.getResourceAsStream(tPath);
		assertNotNull(tStream, "the shipped file " + tPath + " rides the test classpath");
		JsonObject tDoc = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		GT6RecipeMapJsonLoader.pour(Map.of(ResourceLocation.fromNamespaceAndPath("gt6", aKey), tDoc.deepCopy()));
		return tDoc.getAsJsonArray("recipes");
	}

	private String head(String aKey) throws Exception {
		InputStream tStream = GT6RecipeMapDataB2cMagnetRowsPourTest.class.getResourceAsStream("/data/gt6/recipe_maps/" + aKey + ".json");
		assertNotNull(tStream);
		return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject().get("comment").getAsString();
	}

	/** The census: both files pour their full expected row counts — no WARN-skipped rows, no truncation. */
	@Test
	public void theFilesPourTheirFullCensusWithZeroSkips() throws Exception {
		for (Map.Entry<String, Integer> tPin : CENSUS.entrySet()) {
			pourShipped(tPin.getKey());
			RecipeMap tMap = GT6RecipeMapJsonLoader.mapFor(tPin.getKey());
			assertNotNull(tMap, tPin.getKey() + " resolves");
			assertEquals(tPin.getValue().longValue(), tMap.mRecipeList.size(), tPin.getKey() + ": the map holds the census");
			assertEquals(tPin.getValue().longValue(), GT6RecipeMapJsonLoader.pouredCount(tPin.getKey()),
					tPin.getKey() + ": the tracker mirrors the map (a smaller number = WARN-skipped rows)");
		}
	}

	/** The declaration shape pins: sources + deviations + the foreign-judgment chains + the blocked faces. */
	@Test
	public void theFileHeadsKeepTheirDeclarations() throws Exception {
		String tMag = head(MAGNET), tSlu = head(SLUICE);
		String tLower = tMag.toLowerCase(java.util.Locale.ROOT);
		assertTrue(tMag.contains("Loader_Recipes_Ores.java"), "magnet: the Ores row source is cited");
		assertTrue(tMag.contains(":51-54") && tMag.contains(":58-59") && tMag.contains(":61-64") && tMag.contains(":454-457"),
				"magnet: all four fixed-row families and the walk are cited");
		assertTrue(tMag.toLowerCase(java.util.Locale.ROOT).contains("declared deviation"), "magnet: the deviation class is declared");
		assertTrue(tMag.toLowerCase(java.util.Locale.ROOT).contains("frozen"), "magnet: the frozen-universe semantics are declared");
		assertTrue(tMag.toLowerCase(java.util.Locale.ROOT).contains("walk row provenance"), "magnet: the walk-row provenance ruling is declared");
		assertTrue(tMag.toLowerCase(java.util.Locale.ROOT).contains("compacted null slots"), "magnet: the null-slot compaction is declared");
		assertTrue(tMag.toLowerCase(java.util.Locale.ROOT).contains("smoke row"), "magnet: the replaced smoke row is declared");
		assertTrue(tMag.contains("Loader_Recipes_Temporary.java:601") && tMag.contains("RH_Sand_Magnetite"),
				"magnet: the RH-sand TRUE NEGATIVE is declared with its input item");
		assertTrue(tMag.contains("Compat_Recipes_Tropicraft.java:52"), "magnet: the Tropic compat TRUE NEGATIVE is declared");
		assertTrue(tMag.contains("GTMaterialBlocks.blockPathPrefixes()") && tMag.contains("GTMaterialPrefixBlockItem"),
				"magnet: the block-path bridge (the r2 un-blocked :54/:59/:64 faces) is declared");
		assertTrue(tMag.contains("MAGNETIC_SEPARATOR") && tMag.contains("pinned 0"), "magnet: the PhaseGate zero-bump posture is declared");
		assertTrue(tSlu.contains("Loader_Recipes_Ores.java:385-420"), "sluice: the ore walk is cited");
		assertTrue(tSlu.contains("Loader_Recipes_Handlers.java:672-673"), "sluice: the Petrotheum statements are cited");
		assertTrue(tSlu.contains("gt6:petrotheum") && tSlu.contains("GTFluids.java:2780") && tSlu.contains("MT.java:2403"),
				"sluice: the Petrotheum judgment chain names the port-native fluid id and the MT anchor");
		assertTrue(tSlu.contains("FL.java:449") && tSlu.contains("250 per Unit"), "sluice: the 45/5 mB amounts carry their upstream anchor");
		assertTrue(tSlu.contains(":415-416") && tSlu.contains("mByProducts"), "sluice: the byproduct walk cites its accessor semantics");
		assertTrue(tSlu.contains("sluicejuice"), "sluice: the fluid output id is anchored");
		assertTrue(tSlu.toLowerCase(java.util.Locale.ROOT).contains("declared deviation"), "sluice: the deviation class is declared");
		assertTrue(tSlu.toLowerCase(java.util.Locale.ROOT).contains("declared over-cap"), "sluice: the over-cap ruling is declared");
		assertTrue(tSlu.toLowerCase(java.util.Locale.ROOT).contains("compacted null slots"), "sluice: the null-slot compaction is declared");
		assertTrue(tSlu.toLowerCase(java.util.Locale.ROOT).contains("smoke row"), "sluice: the replaced smoke rows are declared");
	}

	/** The 7 fixed rows keep their per-line citations; all walk rows are uncommented (the scale ruling). */
	@Test
	public void fixedRowsAreCitedAndWalkRowsAreUncommented() throws Exception {
		JsonArray tMagRows = pourShipped(MAGNET);
		int tCited = 0;
		for (JsonElement tElement : tMagRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (tRow.has("comment")) {
				tCited++;
				assertTrue(tRow.get("comment").getAsString().startsWith("Loader_Recipes_Ores.java:"),
						MAGNET + ": the citation names the upstream line");
			}
		}
		assertEquals(10, tCited, MAGNET + ": exactly the 10 fixed rows carry citations");
		assertEquals(166, tMagRows.size() - tCited, MAGNET + ": the walk stock is uncommented");
		for (JsonElement tElement : pourShipped(SLUICE)) {
			assertFalse(tElement.getAsJsonObject().has("comment"), SLUICE + ": no per-row comments (the provenance IS the live-walk cross-check)");
		}
	}

	/**
	 * The per-source ratchet: the frozen accounting table (SOURCES) must equal the shipped
	 * row stock partitioned by its identification face (the cited line for the fixed rows,
	 * the fluid leg for the sluice families) AND the live recomputation totals. A
	 * registration change turns the live checks red — re-pour and bump consciously.
	 */
	@Test
	public void theSourceAccountingMatchesTheFrozenPartition() throws Exception {
		Map<String, Integer> tActual = new LinkedHashMap<>();
		for (JsonElement tElement : pourShipped(MAGNET)) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (tRow.has("comment")) {
				String tCite = tRow.get("comment").getAsString();
				tActual.merge(MAGNET + ".fixed:" + tCite.substring(tCite.indexOf(':') + 1, tCite.indexOf(' ')), 1, Integer::sum);
			} else {
				tActual.merge(MAGNET + ".walk:454-457", 1, Integer::sum);
			}
		}
		for (JsonElement tElement : pourShipped(SLUICE)) {
			JsonObject tRow = tElement.getAsJsonObject();
			String tFluid = tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("fluid").getAsString();
			String tAmount = tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("amount").getAsString();
			if ("gt6:petrotheum".equals(tFluid)) {
				tActual.merge(SLUICE + ".petro:" + ("45".equals(tAmount) ? "672" : "673"), 1, Integer::sum);
			} else {
				tActual.merge(SLUICE + ".walk:385-420", 1, Integer::sum);
			}
		}
		List<String> tDrift = new ArrayList<>();
		for (Map.Entry<String, Integer> tPin : SOURCES.entrySet()) {
			Integer tSeen = tActual.get(tPin.getKey());
			int tLive = 0;
			if (tPin.getKey().endsWith(":454-457")) tLive = liveMagnetWalk().size();
			if (tPin.getKey().endsWith(":385-420")) tLive = liveSluiceWalk().size();
			if (tPin.getKey().endsWith(":672")) tLive = livePetroRows().size() / 2;
			if (tPin.getKey().endsWith(":673")) tLive = livePetroRows().size() / 2;
			if (tPin.getValue().intValue() != (tSeen == null ? 0 : tSeen.intValue()) || (tLive > 0 && tLive != tPin.getValue())) {
				tDrift.add(tPin.getKey() + " frozen " + tPin.getValue() + " shipped " + tSeen + " live " + tLive);
			}
		}
		assertTrue(tDrift.isEmpty(), MAGNET + "/" + SLUICE + ": the per-source accounting drifted — re-pour and bump SOURCES: " + tDrift);
	}

	/**
	 * The magneticseparator walk cross-check — recompute the :454-457 walk over the LIVE
	 * registration (the polarity gate :389-394, the select fallback, the chances tMagnet
	 * {10000,600x5}, durations 144/16) and require the shipped uncommented rows to cover it
	 * EXACTLY, both directions.
	 */
	@Test
	public void theMagneticseparatorWalkMatchesTheLiveRegistration() throws Exception {
		Set<String> tExpected = liveMagnetWalk();
		Set<String> tShipped = new HashSet<>();
		int tWalkRows = 0;
		for (JsonElement tElement : pourShipped(MAGNET)) {
			if (tElement.getAsJsonObject().has("comment")) continue;
			tWalkRows++;
			tShipped.add(rowKey(tElement.getAsJsonObject()));
		}
		Set<String> tMissing = new HashSet<>(tExpected);
		tMissing.removeAll(tShipped);
		Set<String> tStale = new HashSet<>(tShipped);
		tStale.removeAll(tExpected);
		assertTrue(tMissing.isEmpty(), MAGNET + ": the frozen snapshot trails the live walk (re-pour needed): " + tMissing);
		assertTrue(tStale.isEmpty(), MAGNET + ": the frozen snapshot holds rows the live walk no longer generates: " + tStale);
		assertEquals(tExpected.size(), tWalkRows, MAGNET + ": the walk snapshot size equals the live walk");
	}

	/** The sluice ore-walk cross-check — the :385-420 walk x the four FL.waters legs, both directions. */
	@Test
	public void theSluiceWalkMatchesTheLiveRegistration() throws Exception {
		Set<String> tExpected = liveSluiceWalk();
		Set<String> tShipped = new HashSet<>();
		for (JsonElement tElement : pourShipped(SLUICE)) {
			JsonObject tRow = tElement.getAsJsonObject();
			String tFluid = tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("fluid").getAsString();
			if ("gt6:petrotheum".equals(tFluid)) continue;
			tShipped.add(rowKey(tRow));
		}
		Set<String> tMissing = new HashSet<>(tExpected);
		tMissing.removeAll(tShipped);
		Set<String> tStale = new HashSet<>(tShipped);
		tStale.removeAll(tExpected);
		assertTrue(tMissing.isEmpty(), SLUICE + ": the frozen snapshot trails the live walk (re-pour needed): " + tMissing);
		assertTrue(tStale.isEmpty(), SLUICE + ": the frozen snapshot holds rows the live walk no longer generates: " + tStale);
		assertEquals(tExpected.size(), tShipped.size(), SLUICE + ": the walk snapshot size equals the live walk");
	}

	/**
	 * The Petrotheum statement cross-check + the verbatim pin (the approved reversal): the
	 * two handler statements expand to 500+500 rows over the LIVE crushed universe and the
	 * iron row shape is exactly crushed x1 + petrotheum 45/5 mB -> crushedPurified x2 +
	 * crushedPurifiedTiny x9 + SluiceSand dust, chances 10000/5000/10000.
	 */
	@Test
	public void thePetrotheumStatementsMatchTheLiveRegistration() throws Exception {
		Map<String, JsonObject> tExpected = livePetroRows();
		Set<String> tShipped = new HashSet<>();
		Set<String> tExpectedKeys = tExpected.keySet();
		for (JsonElement tElement : pourShipped(SLUICE)) {
			JsonObject tRow = tElement.getAsJsonObject();
			String tFluid = tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("fluid").getAsString();
			if (!"gt6:petrotheum".equals(tFluid)) continue;
			String tKey = rowKey(tRow);
			tShipped.add(tKey);
			if ("gt6:crushed_iron".equals(tRow.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString())
					&& tRow.get("duration").getAsLong() == 144) {
				// the coordinator-mandated verbatim pin on the :672 iron row
				assertEquals(45, tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("amount").getAsLong(), ":672 fluid = 9*U50 = 45 mB");
				JsonArray tOut = tRow.getAsJsonArray("outputs");
				assertEquals(3, tOut.size(), ":672 three output slots");
				assertEquals("gt6:crushed_purified_iron", tOut.get(0).getAsJsonObject().get("item").getAsString());
				assertEquals(2, tOut.get(0).getAsJsonObject().get("count").getAsLong(), ":672 crushedPurified x2");
				assertEquals("gt6:crushed_purified_tiny_iron", tOut.get(1).getAsJsonObject().get("item").getAsString());
				assertEquals(9, tOut.get(1).getAsJsonObject().get("count").getAsLong(), ":672 crushedPurifiedTiny x9");
				assertEquals(5000, tOut.get(1).getAsJsonObject().get("chance").getAsLong(), ":672 chances(10000,5000,10000)");
				assertEquals("gt6:dust_sluice_sand", tOut.get(2).getAsJsonObject().get("item").getAsString(), ":672 the SluiceSand additional output");
			}
			if ("gt6:crushed_tiny_iron".equals(tRow.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString())) {
				assertEquals(5, tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("amount").getAsLong(), ":673 fluid = U50 = 5 mB");
				assertEquals(16, tRow.get("duration").getAsLong(), ":673 duration 16");
			}
		}
		Set<String> tMissing = new HashSet<>(tExpectedKeys);
		tMissing.removeAll(tShipped);
		Set<String> tStale = new HashSet<>(tShipped);
		tStale.removeAll(tExpectedKeys);
		assertTrue(tMissing.isEmpty(), SLUICE + ": the Petrotheum snapshot trails the live walk: " + tMissing);
		assertTrue(tStale.isEmpty(), SLUICE + ": the Petrotheum snapshot holds rows the live walk no longer generates: " + tStale);
		assertEquals(986, tShipped.size(), SLUICE + ": 493 + 493 Petrotheum rows over the resolved crushed universe");
	}

	/** Spot checks — the 7 fixed rows verbatim (chances, counts, durations, eut) and walk-row twins. */
	@Test
	public void fixedRowsAreUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped(MAGNET);
		// :51 — dustTiny SluiceSand, chances {9640,72x5}, dustTiny Fe x2
		JsonObject tRow = findRow(tRows, "gt6:dust_tiny_sluice_sand", "gt6:dust_tiny_stone", 16);
		assertChanceRow(tRow, new long[] {9640, 72, 72, 72, 72, 72});
		assertEquals("gt6:dust_tiny_iron", tRow.getAsJsonArray("outputs").get(1).getAsJsonObject().get("item").getAsString(), ":51 Fe dustTiny");
		assertEquals(2, tRow.getAsJsonArray("outputs").get(1).getAsJsonObject().get("count").getAsLong(), ":51 Fe x2");
		// :52/:53 — the scale ladder 162/648 over dustSmall/dust
		assertChanceRow(findRow(tRows, "gt6:dust_small_sluice_sand", "gt6:dust_small_stone", 36), new long[] {9640, 162, 162, 162, 162, 162});
		assertChanceRow(findRow(tRows, "gt6:dust_sluice_sand", "gt6:dust_stone", 144), new long[] {9640, 648, 648, 648, 648, 648});
		// :58 — Bedrock, eut 64, the RareEarth/Nd/V2O5 tail counts
		JsonObject tBedrock = findRow(tRows, "gt6:dust_bedrock", "gt6:dust_deepslate", 144);
		assertChanceRow(tBedrock, new long[] {7000, 3000, 3000, 3000, 3000, 3000});
		assertEquals(64, tBedrock.get("eut").getAsLong(), ":58 eut 64");
		assertEquals("gt6:dust_tiny_rare_earth", tBedrock.getAsJsonArray("outputs").get(3).getAsJsonObject().get("item").getAsString(), ":58 RareEarth");
		assertEquals(2, tBedrock.getAsJsonArray("outputs").get(3).getAsJsonObject().get("count").getAsLong(), ":58 RareEarth x2");
		assertEquals("gt6:dust_tiny_neodymium", tBedrock.getAsJsonArray("outputs").get(4).getAsJsonObject().get("item").getAsString(), ":58 Nd");
		assertEquals(3, tBedrock.getAsJsonArray("outputs").get(4).getAsJsonObject().get("count").getAsLong(), ":58 Nd x3");
		assertEquals("gt6:dust_tiny_vanadium_pentoxide", tBedrock.getAsJsonArray("outputs").get(5).getAsJsonObject().get("item").getAsString(), ":58 V2O5");
		// :54/:59/:64 — the blockDust faces (r2), eut 16/64/16 over duration 1296
		JsonObject tS54 = findRow(tRows, "gt6:block_dust_sluice_sand", "gt6:dust_stone", 1296);
		assertChanceRow(tS54, new long[] {9640, 5832, 5832, 5832, 5832, 5832});
		assertEquals("gt6:dust_tiny_iron", tS54.getAsJsonArray("outputs").get(1).getAsJsonObject().get("item").getAsString(), ":54 Fe dustTiny");
		assertEquals(9, tS54.getAsJsonArray("outputs").get(0).getAsJsonObject().get("count").getAsLong(), ":54 Stone dust x9");
		JsonObject tS59 = findRow(tRows, "gt6:block_dust_bedrock", "gt6:dust_deepslate", 1296);
		assertChanceRow(tS59, new long[] {7000, 3000, 3000, 3000, 3000, 3000});
		assertEquals(64, tS59.get("eut").getAsLong(), ":59 eut 64");
		assertEquals(9, tS59.getAsJsonArray("outputs").get(0).getAsJsonObject().get("count").getAsLong(), ":59 Deepslate x9");
		assertEquals("gt6:dust_tiny_adamantine", tS59.getAsJsonArray("outputs").get(1).getAsJsonObject().get("item").getAsString(), ":59 Adamantine");
		assertEquals("gt6:dust_tiny_rare_earth", tS59.getAsJsonArray("outputs").get(3).getAsJsonObject().get("item").getAsString(), ":59 RareEarth");
		assertEquals(18, tS59.getAsJsonArray("outputs").get(3).getAsJsonObject().get("count").getAsLong(), ":59 RareEarth x18");
		assertEquals("gt6:dust_tiny_neodymium", tS59.getAsJsonArray("outputs").get(4).getAsJsonObject().get("item").getAsString(), ":59 Nd");
		assertEquals(27, tS59.getAsJsonArray("outputs").get(4).getAsJsonObject().get("count").getAsLong(), ":59 Nd x27");
		assertEquals("gt6:dust_tiny_vanadium_pentoxide", tS59.getAsJsonArray("outputs").get(5).getAsJsonObject().get("item").getAsString(), ":59 V2O5");
		assertEquals(27, tS59.getAsJsonArray("outputs").get(5).getAsJsonObject().get("count").getAsLong(), ":59 V2O5 x27");
		JsonObject tS64 = findRow(tRows, "gt6:block_dust_moon_turf", "gt6:dust_basalt", 1296);
		assertChanceRow(tS64, new long[] {3000, 972, 972, 972, 972, 972});
		assertEquals("gt6:dust_tiny_meteoric_iron", tS64.getAsJsonArray("outputs").get(1).getAsJsonObject().get("item").getAsString(), ":64 MeteoricIron");
		assertEquals(9, tS64.getAsJsonArray("outputs").get(1).getAsJsonObject().get("count").getAsLong(), ":64 MeteoricIron x9");
		// :61-63 — MoonTurf, the MeteoricIron x9 leg and the 12/27/108 ladder
		JsonObject tMoon = findRow(tRows, "gt6:dust_tiny_moon_turf", "gt6:dust_tiny_basalt", 16);
		assertChanceRow(tMoon, new long[] {3000, 12, 12, 12, 12, 12});
		assertEquals("gt6:dust_tiny_meteoric_iron", tMoon.getAsJsonArray("outputs").get(1).getAsJsonObject().get("item").getAsString(), ":61 MeteoricIron");
		assertEquals(9, tMoon.getAsJsonArray("outputs").get(1).getAsJsonObject().get("count").getAsLong(), ":61 MeteoricIron x9");
		assertChanceRow(findRow(tRows, "gt6:dust_small_moon_turf", "gt6:dust_small_basalt", 36), new long[] {3000, 27, 27, 27, 27, 27});
		assertChanceRow(findRow(tRows, "gt6:dust_moon_turf", "gt6:dust_basalt", 144), new long[] {3000, 108, 108, 108, 108, 108});
		// the walk twin: the Pyrite purified row (a magnetic self over non-magnetic byproducts; computed live)
		Walk tWalk = walkOnce();
		JsonObject tMagIron = findRow(tRows, "gt6:crushed_purified_pyrite", "gt6:crushed_centrifuged_pyrite", 144);
		assertEquals(tWalk.magnetKey, rowKey(tMagIron), "the Pyrite :455 row equals the live recomputation verbatim");
		// the sluice + petro twins on the same live shape
		JsonArray tSluRows = pourShipped(SLUICE);
		JsonObject tSluIron = findFluidRow(tSluRows, "gt6:crushed_iron", "minecraft:water", 900, "gt6:crushed_purified_iron", 144);
		assertEquals(tWalk.sluiceKey, rowKey(tSluIron), "the Iron :418 row equals the live recomputation verbatim");
		JsonObject tSluIronDist = findFluidRow(tSluRows, "gt6:crushed_iron", "gt6:distilled_water", 900, "gt6:crushed_purified_iron", 144);
		assertNotNull(tSluIronDist, "the DistW leg twin exists");
		JsonObject tSluIronTiny = findFluidRow(tSluRows, "gt6:crushed_tiny_iron", "gt6:spdew", 100, "gt6:crushed_purified_tiny_iron", 16);
		assertNotNull(tSluIronTiny, "the SpDew tiny leg exists");
		JsonObject tPetroIronTiny = findFluidRow(tSluRows, "gt6:crushed_tiny_iron", "gt6:petrotheum", 5, "gt6:crushed_purified_tiny_iron", 16);
		assertEquals("gt6:dust_tiny_sluice_sand", tPetroIronTiny.getAsJsonArray("outputs").get(2).getAsJsonObject().get("item").getAsString(),
				":673 the dustTiny SluiceSand additional output");
	}

	/** The id/energy face over the THREE universes (the weld r2 form): every gt6 item id lives in GTMaterialItems ∪ the GTMaterialBlocks storage blocks ∪ the pipe rows; eut/duration are typed; the smoke rows stay gone. */
	@Test
	public void theStockIsRegisteredEutTypedAndSmokeFree() throws Exception {
		GTMaterialItems.initMaterials();
		Set<String> tItemUniverse = new HashSet<>();
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			tItemUniverse.add("gt6:" + GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		}
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialBlocks.registrationOrder()) {
			tItemUniverse.add("gt6:" + GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		}
		for (GTFluidPipes.FluidPipeRow tRow : GTFluidPipes.ROWS) {
			tItemUniverse.add("gt6:" + tRow.path());
		}
		for (GTItemPipes.ItemPipeRow tRow : GTItemPipes.ROWS) {
			tItemUniverse.add("gt6:" + tRow.path());
		}
		Set<String> tMissing = new HashSet<>();
		int tForeignItems = 0;
		for (String tKey : new String[] {MAGNET, SLUICE}) {
			for (JsonElement tElement : pourShipped(tKey)) {
				JsonObject tRow = tElement.getAsJsonObject();
				for (String tLeg : new String[] {"inputs", "outputs"}) {
					for (JsonElement tSlot : tRow.getAsJsonArray(tLeg)) {
						String tId = tSlot.getAsJsonObject().get("item").getAsString();
						if (!tId.startsWith("gt6:")) {tForeignItems++; continue;}
						if (!tItemUniverse.contains(tId)) tMissing.add(tId);
					}
				}
				for (String tLeg : new String[] {"fluidInputs", "fluidOutputs"}) {
					if (!tRow.has(tLeg)) continue; // the magnet rows carry no fluid legs
					for (JsonElement tSlot : tRow.getAsJsonArray(tLeg)) {
						assertTrue(FLUID_UNIVERSE.contains(tSlot.getAsJsonObject().get("fluid").getAsString()),
								"the fluid leg is a known FL.waters/sluicejuice/petrotheum id");
					}
				}
				long tEut = tRow.get("eut").getAsLong();
				if (MAGNET.equals(tKey) && tRow.has("comment") && tRow.get("comment").getAsString().contains("eut 64")) {
					assertEquals(64, tEut, ":58-59 carry eut 64");
				} else {
					assertEquals(16, tEut, "eut 16 on every other row");
				}
				assertTrue(tRow.get("duration").getAsLong() > 0, "the JSON loader rejects duration <= 0");
			}
		}
		assertTrue(tMissing.isEmpty(), "unregistered gt6 item ids (these rows would WARN-skip live): " + tMissing);
		assertEquals(0, tForeignItems, "no vanilla-namespace item ids (the p29 smoke rows were replaced)");
	}

	// ---------------------------------------------------------------- live walks

	private static final class Walk {
		final Set<String> rows = new HashSet<>();
		String magnetKey, sluiceKey;
	}

	/** Walks all three live recomputations in one pass (the shared pair-map setup). */
	private Walk walkOnce() {
		GTMaterialItems.initMaterials();
		Map<String, GTMaterialItems.PrefixMaterial> tPairs = new HashMap<>();
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			tPairs.put(tPair.prefix().mNameInternal + "|" + tPair.material().mNameInternal, tPair);
		}
		Walk rWalk = new Walk();
		Set<OreDictMaterial> tSeen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
		for (OreDictMaterial tMat0 : OreDictMaterial.MATERIAL_ARRAY) {
			if (tMat0 == null || tMat0.mID < 0) continue;
			OreDictMaterial tMat = MaterialRegistry.INSTANCE.get(tMat0); // the enumerate() alias resolution
			if (tMat == null || tMat.mID < 0 || !tSeen.add(tMat)) continue;
			if (tMat.contains(TD.Atomic.ANTIMATTER) || tMat.contains(TD.Properties.INVALID_MATERIAL)) continue;
			List<OreDictMaterial> tBy = tMat.mByProducts;
			// the magnetic walk :454-457 — inside the purified gate :449, the polarity gate :389-394
			String tMatName = tMat.mNameInternal;
			GTMaterialItems.PrefixMaterial tPur = tPairs.get("crushedPurified|" + tMatName);
			GTMaterialItems.PrefixMaterial tPurT = tPairs.get("crushedPurifiedTiny|" + tMatName);
			if (tPur != null && tPurT != null) {
				boolean tSelfMag = tMat.containsAny(TD.Properties.MAGNETIC_PASSIVE, TD.Properties.MAGNETIC_ACTIVE);
				List<OreDictMaterial> tMagnetList = new ArrayList<>();
				for (OreDictMaterial tB : tBy) if (tB.containsAny(TD.Properties.MAGNETIC_PASSIVE, TD.Properties.MAGNETIC_ACTIVE) != tSelfMag) tMagnetList.add(tB);
				if (!tMagnetList.isEmpty()) {
					for (int tForm = 0; tForm < 2; tForm++) {
						long tDur = tForm == 0 ? 144 : 16;
						long tCnt = tForm == 0 ? 18 : 2;
						String tIn = tForm == 0 ? "crushedPurified" : "crushedPurifiedTiny";
						String tOutMain = tForm == 0 ? "crushedCentrifuged" : "crushedCentrifugedTiny";
						StringBuilder tKey = new StringBuilder("in:").append(id(tPairs, tIn, tMat)).append(":1");
						tKey.append(">out:").append(id(tPairs, tOutMain, tMat)).append(":1");
						for (int i = 0; i < 5; i++) {
							OreDictMaterial tSel = select(i, tMat, tMagnetList);
							if (tPairs.get("crushedCentrifugedTiny|" + tSel.mNameInternal) == null) continue; // compacted null slot
							tKey.append('+').append(id(tPairs, "crushedCentrifugedTiny", tSel)).append(':').append(tCnt).append(":600");
						}
						tKey.append(">dur:").append(tDur).append(">16");
						rWalk.rows.add(tKey.toString());
						if ("Pyrite".equals(tMatName) && tForm == 0) rWalk.magnetKey = tKey.toString();
					}
				}
			}
			// the sluice walk :385-420 — the dust gate :386-387, the crushed family gate :397, Bedrock excluded :385
			if ("Bedrock".equals(tMatName)) continue;
			if (tPairs.get("dust|" + tMatName) == null) continue;
			if (tPairs.get("crushed|" + tMatName) == null || tPairs.get("crushedTiny|" + tMatName) == null) continue;
			if (tPur == null || tPurT == null) continue;
			for (String[] tLeg : WATERS) {
				boolean tBig = "900".equals(tLeg[1]);
				String tIn = tBig ? "crushed" : "crushedTiny";
				String tMain = tBig ? "crushedPurified" : "crushedPurifiedTiny";
				long tCnt = tBig ? 9 : 1, tDur = tBig ? 144 : 16;
				StringBuilder tKey = new StringBuilder("in:").append(id(tPairs, tIn, tMat)).append(":1;fluid:").append(tLeg[0]).append(':').append(tLeg[1]);
				tKey.append(">out:").append(id(tPairs, tMain, tMat)).append(":1");
				for (int i = 0; i < 8; i++) {
					OreDictMaterial tProd = i < tBy.size() ? tBy.get(i) : tMat; // tSluiceProducts :415-416
					if (tPairs.get("crushedPurifiedTiny|" + tProd.mNameInternal) == null) continue; // compacted null slot
					tKey.append('+').append(id(tPairs, "crushedPurifiedTiny", tProd)).append(':').append(tCnt).append(":300");
				}
				tKey.append(";fout:gt6:sluicejuice:").append(tLeg[1]).append(">dur:").append(tDur).append(">16");
				rWalk.rows.add(tKey.toString());
				if ("Iron".equals(tMatName) && tBig && "minecraft:water".equals(tLeg[0])) rWalk.sluiceKey = tKey.toString();
			}
		}
		return rWalk;
	}

	private Set<String> liveMagnetWalk() {
		return walkOnce().rows.stream().filter(aKey -> !aKey.contains(";fluid:")).collect(java.util.stream.Collectors.toSet());
	}

	private Set<String> liveSluiceWalk() {
		return walkOnce().rows.stream().filter(aKey -> aKey.contains(";fluid:") && !aKey.contains("gt6:petrotheum")).collect(java.util.stream.Collectors.toSet());
	}

	/** The two Petrotheum statement walks (Handlers:672-673) — keys only; the verbatim pin rides its own test. */
	private Map<String, JsonObject> livePetroRows() {
		walkOnce(); // warm the registration (idempotent)
		GTMaterialItems.initMaterials();
		Map<String, GTMaterialItems.PrefixMaterial> tPairs = new HashMap<>();
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			tPairs.put(tPair.prefix().mNameInternal + "|" + tPair.material().mNameInternal, tPair);
		}
		Map<String, JsonObject> rRows = new LinkedHashMap<>();
		Set<OreDictMaterial> tSeen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
		for (OreDictMaterial tMat0 : OreDictMaterial.MATERIAL_ARRAY) {
			if (tMat0 == null || tMat0.mID < 0) continue;
			OreDictMaterial tMat = MaterialRegistry.INSTANCE.get(tMat0); // the enumerate() alias resolution
			if (tMat == null || tMat.mID < 0 || !tSeen.add(tMat)) continue;
			if (tMat.contains(TD.Atomic.ANTIMATTER) || tMat.contains(TD.Properties.INVALID_MATERIAL)) continue;
			String tMatName = tMat.mNameInternal;
			if (tPairs.get("crushed|" + tMatName) != null && tPairs.get("crushedPurified|" + tMatName) != null && tPairs.get("crushedPurifiedTiny|" + tMatName) != null) {
				JsonObject tRow = new JsonObject();
				tRow.addProperty("k", "in:gt6:" + GTMaterialItems.itemIdOf(tPairs.get("crushed|" + tMatName).prefix(), tMat)
						+ ":1;fluid:gt6:petrotheum:45>out:gt6:" + GTMaterialItems.itemIdOf(tPairs.get("crushedPurified|" + tMatName).prefix(), tMat)
						+ ":2+gt6:" + GTMaterialItems.itemIdOf(tPairs.get("crushedPurifiedTiny|" + tMatName).prefix(), tMat) + ":9:5000"
						+ "+gt6:dust_sluice_sand:1>dur:144>16");
				rRows.put(tRow.get("k").getAsString(), tRow);
			}
			if (tPairs.get("crushedTiny|" + tMatName) != null && tPairs.get("crushedPurifiedTiny|" + tMatName) != null) {
				JsonObject tRow = new JsonObject();
				tRow.addProperty("k", "in:gt6:" + GTMaterialItems.itemIdOf(tPairs.get("crushedTiny|" + tMatName).prefix(), tMat)
						+ ":1;fluid:gt6:petrotheum:5>out:gt6:" + GTMaterialItems.itemIdOf(tPairs.get("crushedPurifiedTiny|" + tMatName).prefix(), tMat)
						+ ":2+gt6:" + GTMaterialItems.itemIdOf(tPairs.get("crushedPurifiedTiny|" + tMatName).prefix(), tMat) + ":1:5000"
						+ "+gt6:dust_tiny_sluice_sand:1>dur:16>16");
				rRows.put(tRow.get("k").getAsString(), tRow);
			}
		}
		return rRows;
	}

	/** The row identity key: inputs (+fluid) > outputs with their non-10000 chances > duration > eut. */
	private static String rowKey(JsonObject aRow) {
		StringBuilder tKey = new StringBuilder("in:");
		JsonArray tIn = aRow.getAsJsonArray("inputs");
		for (int i = 0; i < tIn.size(); i++) {
			if (i > 0) tKey.append('+');
			tKey.append(tIn.get(i).getAsJsonObject().get("item").getAsString()).append(':').append(tIn.get(i).getAsJsonObject().get("count").getAsLong());
		}
		if (aRow.has("fluidInputs") && aRow.getAsJsonArray("fluidInputs").size() > 0) {
			JsonObject tFluid = aRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject();
			tKey.append(";fluid:").append(tFluid.get("fluid").getAsString()).append(':').append(tFluid.get("amount").getAsLong());
		}
		tKey.append(">out:");
		JsonArray tOut = aRow.getAsJsonArray("outputs");
		for (int i = 0; i < tOut.size(); i++) {
			if (i > 0) tKey.append('+');
			JsonObject tSlot = tOut.get(i).getAsJsonObject();
			tKey.append(tSlot.get("item").getAsString()).append(':').append(tSlot.get("count").getAsLong());
			if (tSlot.has("chance")) tKey.append(':').append(tSlot.get("chance").getAsLong());
		}
		if (aRow.has("fluidOutputs") && aRow.getAsJsonArray("fluidOutputs").size() > 0) {
			JsonObject tFluid = aRow.getAsJsonArray("fluidOutputs").get(0).getAsJsonObject();
			tKey.append(";fout:").append(tFluid.get("fluid").getAsString()).append(':').append(tFluid.get("amount").getAsLong());
		}
		tKey.append(">dur:").append(aRow.get("duration").getAsLong()).append(">").append(aRow.get("eut").getAsLong());
		return tKey.toString();
	}

	/** UT.Code.select(long, E, List) with a non-null replacement (UT.java:1458-1462). */
	private static OreDictMaterial select(long aIndex, OreDictMaterial aReplacement, List<OreDictMaterial> aList) {
		if (aList.isEmpty() || aIndex >= aList.size()) return aReplacement;
		return aList.get((int) aIndex);
	}

	private static String id(Map<String, GTMaterialItems.PrefixMaterial> aPairs, String aPrefix, OreDictMaterial aMat) {
		return "gt6:" + GTMaterialItems.itemIdOf(aPairs.get(aPrefix + "|" + aMat.mNameInternal).prefix(), aMat);
	}

	// ---------------------------------------------------------------- helpers

	private static void assertChanceRow(JsonObject aRow, long[] aChances) {
		JsonArray tOut = aRow.getAsJsonArray("outputs");
		assertEquals(aChances.length, tOut.size(), "the output slot count");
		for (int i = 0; i < aChances.length; i++) {
			JsonObject tSlot = tOut.get(i).getAsJsonObject();
			if (aChances[i] == 10000) {
				assertFalse(tSlot.has("chance"), "slot " + i + " deterministic (no chance key)");
			} else {
				assertEquals(aChances[i], tSlot.get("chance").getAsLong(), "slot " + i + " chance");
			}
		}
	}

	private static JsonObject findRow(JsonArray aRows, String aInputItem, String aOutputItem, long aDuration) {
		for (JsonElement tElement : aRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			JsonArray tIn = tRow.getAsJsonArray("inputs"), tOut = tRow.getAsJsonArray("outputs");
			if (aInputItem.equals(tIn.get(0).getAsJsonObject().get("item").getAsString())
					&& aOutputItem.equals(tOut.get(0).getAsJsonObject().get("item").getAsString())
					&& aDuration == tRow.get("duration").getAsLong()) return tRow;
		}
		throw new AssertionError("no row " + aInputItem + " -> " + aOutputItem + " @ " + aDuration);
	}

	private static JsonObject findFluidRow(JsonArray aRows, String aInputItem, String aFluid, long aAmount, String aOutputItem, long aDuration) {
		for (JsonElement tElement : aRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			JsonArray tFin = tRow.getAsJsonArray("fluidInputs");
			JsonObject tHead = tFin.size() > 0 ? tFin.get(0).getAsJsonObject() : null;
			if (tHead != null && aFluid.equals(tHead.get("fluid").getAsString()) && aAmount == tHead.get("amount").getAsLong()
					&& aInputItem.equals(tRow.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString())
					&& aOutputItem.equals(tRow.getAsJsonArray("outputs").get(0).getAsJsonObject().get("item").getAsString())
					&& aDuration == tRow.get("duration").getAsLong()) return tRow;
		}
		throw new AssertionError("no row " + aInputItem + " + " + aFluid + " " + aAmount + " -> " + aOutputItem + " @ " + aDuration);
	}
}
