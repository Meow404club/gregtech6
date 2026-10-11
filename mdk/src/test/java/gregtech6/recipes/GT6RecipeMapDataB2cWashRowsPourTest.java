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
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregapi.data.MT;
import gregapi.data.OP;
import gregtech6.block.tree.GT6BeamKind;
import gregtech6.block.tree.GT6TreeKind;
import gregtech6.registry.GT6BeamBlocks;
import gregtech6.registry.GT6ForeignMaterialAtlas;
import gregtech6.registry.GT6ModDrivers;
import gregtech6.registry.GT6TreeBlocks;
import gregtech6.registry.GTMaterialItems;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * The recipe-data-b2c-wash row-stock pour test: the pressurewasher.json file — the log
 * debarking walk of Loader_Recipes_Woods.java:165-167 through the RM.java:707 wrapper
 * (eut 16, duration 64, water 200 mB), each row fanned over the four FL.waters legs
 * (FL.java:689), 28 rows — pours through the real {@link GT6RecipeMapJsonLoader} seam
 * with zero skips. RECOMPUTABILITY: the live half of the cross-check walks the port
 * beam registration itself (GT6BeamKind.values() x GT6BeamBlocks.path — the
 * beam-blocks-register card's face) with the per-kind log mapping, and the shipped rows
 * must cover it EXACTLY, both directions, so a registration change (a kind renamed, a
 * log remapped) turns the walk red. The two unpoured faces declared in the file head —
 * the OreDict event listener (:199/:201, gated off by :182/:193 + WoodEntry.java:178)
 * and the @Deprecated debarking alias (:716-718, water 1000 face) — get their own
 * negative pins here so they cannot silently grow rows.
 *
 * <p>RESTORED FIXTURE ROW (task process-cluster-diesel-rig-red): the p29 acceptance
 * smoke row (coarse_dirt + water 100 -&gt; dirt, 16/16) rides again at the file tail.
 * The walk rows WERE declared-out live: the by-product gt6:dust_bark belongs to
 * MT.Bark, a BINNIE-domain foreign material, and the live server seeds the absent-mod
 * ABSENT levels (GT6ModDrivers.seedFromEnvironment), so the bark dust never registered
 * in a real game JVM and the loader's unregistered-id = declared-out seam skipped all
 * 28 walk rows (the offline enumerate/datagen universe is all-PRESENT and cannot see
 * the divergence — exactly why the b2c replacement shipped green while the live map
 * was empty). REVIVED (task material-foreign-domain-registration, user ruling id1745-4):
 * MT.Bark is GT6's own dust() row (upstream MT.java:1243) with a BINNIE
 * attribution-only put (MT.java:2263), upstream item generation has no mod axis
 * (PrefixItem.java:104 + OreDictPrefix.java:364-366), so the item face left the driver —
 * the bark dust registers unconditionally and the 28 walk rows pour live again. The
 * fixture row stays (the vanilla-only acceptance smoke, unique input, zero
 * interference). Its citation prefix "task " is the walk-vs-fixture partition key.
 * The revival pin + the still-dormant negative (the driver-gated FLUID rows, e.g. the
 * RP nikolinealloy_molten crystallisationcrucible rows) live in
 * {@link #theRevivedBarkChainRegistersUnderItsDomainPinAndTheFluidRowsStayDormant}.
 */
public class GT6RecipeMapDataB2cWashRowsPourTest extends GTRecipesOfflineTestBase {

	private static final java.util.function.Function<ResourceLocation, Item> sDefaultItems = GT6RecipeMapJsonLoader.sItemResolver;
	private static final java.util.function.Function<ResourceLocation, Fluid> sDefaultFluids = GT6RecipeMapJsonLoader.sFluidResolver;

	private static final String WASH = "pressurewasher";

	/** The frozen census (the file-head Row census statement): the 28-row walk + the restored p29 fixture row. */
	private static final int CENSUS = 29;

	/** The walk rows only (the fixture row is the CENSUS-CENSUS_WALK tail). */
	private static final int CENSUS_WALK = 28;

	/** The fixture row's citation prefix — the walk-vs-fixture partition key (task process-cluster-diesel-rig-red). */
	private static final String FIXTURE_PREFIX = "task process-cluster-diesel-rig-red";

	private static boolean isFixtureRow(JsonObject aRow) {
		return aRow.has("comment") && aRow.get("comment").getAsString().startsWith(FIXTURE_PREFIX);
	}

	/** The FL.waters leg order (FL.java:689: water/mnwtr/distilled_water/spdew). */
	private static final List<String> LEGS = List.of("minecraft:water", "gt6:mnwtr", "gt6:distilled_water", "gt6:spdew");

	/** The frozen per-source accounting: 7 logs x 4 legs + the declared unpoured seats (the conscious-bump ratchet). */
	private static final Map<String, Integer> SOURCES = new LinkedHashMap<>();
	static {
		for (GT6BeamKind tKind : GT6BeamKind.values()) {
			if (logOf(tKind) != null) SOURCES.put("walk." + tKind.snake() + ":167", LEGS.size());
		}
		SOURCES.put("tn.oredict:199:201", 0); // the event face, gated by :182/:193 + WoodEntry.java:178
		SOURCES.put("tn.woodkind-no-log", 0); // the WOOD kind: DEFAULT_BEAM has no static log-side row
		SOURCES.put("alias.debarking:716-718", 0); // the @Deprecated water-1000 face, skipped by declaration
		SOURCES.put("fixture.smoke-restored", 1); // the p29 acceptance row, restored by process-cluster-diesel-rig-red
	}

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
		String tPath = "/data/gt6/recipe_maps/" + WASH + ".json";
		InputStream tStream = GT6RecipeMapDataB2cWashRowsPourTest.class.getResourceAsStream(tPath);
		assertNotNull(tStream, "the shipped file " + tPath + " rides the test classpath");
		JsonObject tDoc = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		GT6RecipeMapJsonLoader.pour(Map.of(ResourceLocation.fromNamespaceAndPath("gt6", WASH), tDoc.deepCopy()));
		return tDoc.getAsJsonArray("recipes");
	}

	private String head() throws Exception {
		InputStream tStream = GT6RecipeMapDataB2cWashRowsPourTest.class.getResourceAsStream("/data/gt6/recipe_maps/" + WASH + ".json");
		assertNotNull(tStream);
		return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject().get("comment").getAsString();
	}

	/** The census: the file pours its full expected row count — no WARN-skipped rows, no truncation. */
	@Test
	public void theFilePoursItsFullCensusWithZeroSkips() throws Exception {
		pourShipped();
		RecipeMap tMap = GT6RecipeMapJsonLoader.mapFor(WASH);
		assertNotNull(tMap, WASH + " resolves");
		assertEquals(CENSUS, tMap.mRecipeList.size(), WASH + ": the map holds the census");
		assertEquals(CENSUS, GT6RecipeMapJsonLoader.pouredCount(WASH),
				WASH + ": the tracker mirrors the map (a smaller number = WARN-skipped rows)");
	}

	/** The declaration shape pins: the walk + wrapper + waters anchors, the deviation, both TRUE NEGATIVE faces, the PhaseGate posture. */
	@Test
	public void theFileHeadKeepsItsDeclarations() throws Exception {
		String tHead = head(), tLower = tHead.toLowerCase(Locale.ROOT);
		assertTrue(tHead.contains("Loader_Recipes_Woods.java:165-167"), "the Woods walk is cited");
		assertTrue(tHead.contains("RM.java:707") && tHead.contains("water 200"), "the wrapper face (eut 16 / duration 64 / water 200) is cited");
		assertTrue(tHead.contains("FL.java:689") && tHead.contains("mnwtr") && tHead.contains("spdew"), "the four waters legs are cited with their FL anchor");
		assertTrue(tHead.contains("WoodEntry.java:143"), "the default bark output carries its anchor");
		assertTrue(tHead.contains("LoaderWoodDictionary.java:51-56") && tHead.contains(":69"), "the vanilla six + the GT6 rubber tree entries are cited");
		assertTrue(tLower.contains("declared deviation") && tHead.contains("BeamA:0") && tHead.contains("LoaderWoodDictionary.java:175"),
				"the rubber beam-face alignment (BeamA:0 -> the port Beam2:2 face) is declared");
		assertTrue(tLower.contains("no static log-side row") && tHead.contains("GT_API_Post.java:433/565-566"),
				"the WOOD kind no-log face is declared with its mod-wood anchors");
		assertTrue(tLower.contains("true negative") && tHead.contains("Loader_Recipes_OreDict.java:199/:201")
						&& tHead.contains(":182") && tHead.contains(":193") && tHead.contains("WoodEntry.java:178") && tHead.contains("P10"),
				"the OreDict event face TRUE NEGATIVE carries its full gate chain");
		assertTrue(tHead.contains("RM.java:716-718") && tHead.contains("water 1000"),
				"the debarking alias face is skipped by declaration (with its differing water default)");
		assertTrue(tLower.contains("smoke row restored") && tHead.contains("coarse_dirt") && tHead.contains("gt6:dust_bark"),
				"the restored p29 fixture row carries its archaeology (the walk rows are declared-out live: the bark dust never registers in a seeded JVM)");
		assertTrue(tHead.contains("PRESSURE_WASHER") && tHead.contains("pinned 0"), "the PhaseGate zero-bump posture is declared");
		assertTrue(tHead.contains("Row census: 29"), "the row census statement covers the walk + the fixture row");
		// the stripped exclusion + domain-out + tag pin (task stripped-log-qol)
		assertTrue(tHead.contains("STRIPPED EXCLUSION (task stripped-log-qol)"),
				"the stripped-input exclusion is declared (no bark face to wash on an already-stripped log)");
		assertTrue(tLower.contains("no stripped_* log"), "the explicit input id set excludes stripped_*");
		assertTrue(tLower.contains("log tag"), "the tag-migration guard is declared");
		assertTrue(tHead.contains("OUT-OF-DOMAIN") && tHead.contains("mangrove/cherry/crimson/warped"),
				"the out-of-GT6-wood-dictionary stripped logs are declared TRUE NEGATIVE");
		assertTrue(tHead.contains("#gt6:beam_wood"), "the beam tag pin is declared");
	}

	/** Every walk row is a cited :167 expansion (the citation names the wood and the fluid leg); exactly one fixture row rides the tail. */
	@Test
	public void everyRowCarriesItsLineCitation() throws Exception {
		JsonArray tRows = pourShipped();
		int tFixtures = 0;
		for (JsonElement tElement : tRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			assertTrue(tRow.has("comment"), WASH + ": every row carries its citation");
			String tCite = tRow.get("comment").getAsString();
			if (isFixtureRow(tRow)) {tFixtures++; continue;}
			assertTrue(tCite.startsWith("Loader_Recipes_Woods.java:167 ("), WASH + ": the citation names the upstream line");
			assertTrue(tCite.endsWith(")"), WASH + ": the citation names the wood and the leg");
		}
		assertEquals(1, tFixtures, WASH + ": exactly one restored fixture row");
		assertEquals(CENSUS, tRows.size(), WASH + ": the cited stock is the census");
	}

	/** The stripped-input exclusion pin (task stripped-log-qol): no row washes an already-stripped log. */
	@Test
	public void noRowWashesAStrippedLog() throws Exception {
		Set<String> tStripped = new HashSet<>();
		for (JsonElement tElement : pourShipped()) {
			String tIn = tElement.getAsJsonObject().getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString();
			if (tIn.contains("stripped")) tStripped.add(tIn);
		}
		assertTrue(tStripped.isEmpty(),
				"the debarking wash strips the bark — an already-stripped log has no bark face to wash: " + tStripped);
	}

	/**
	 * The revival pin + the dormant negative (task material-foreign-domain-registration,
	 * ruling id1745-4): the bark chain's by-product pair registers under its own domain's
	 * ABSENT pin (the item face is pin-invariant — GT6's own dust row, BINNIE attribution
	 * only), while the still driver-gated FLUID face keeps its rows dormant (the
	 * nikolinealloy_molten crystallisationcrucible rows stay on the loader's
	 * unregistered-id face under an RP pin — the ruling's 真外域专属 half; the fluid
	 * annotation ratchet itself lives in GT6ModDriverClearOutWaveTest).
	 */
	@Test
	public void theRevivedBarkChainRegistersUnderItsDomainPinAndTheFluidRowsStayDormant() throws Exception {
		JsonArray tRows = pourShipped();
		// the shipped walk rows' by-product is exactly the Bark dust pair
		String tBarkId = "gt6:" + GTMaterialItems.itemIdOf(OP.dust, MT.Bark);
		assertEquals("gt6:dust_bark", tBarkId, "the Bark dust id composition (the JSON rows reference this id)");
		long tBarkRows = 0;
		for (JsonElement tElement : tRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (isFixtureRow(tRow)) continue;
			for (JsonElement tOut : tRow.getAsJsonArray("outputs"))
				if (tBarkId.equals(tOut.getAsJsonObject().get("item").getAsString())) tBarkRows++;
		}
		assertEquals(CENSUS_WALK, tBarkRows, "every walk row carries the bark by-product");
		assertEquals("BinnieCore", MT.MD.BINNIE.mID, "the attribution domain (MT.java:2263 put(MD.BINNIE))");
		assertEquals(MT.MD.BINNIE.mID, GT6ForeignMaterialAtlas.domainOf(MT.Bark), "the atlas still answers the domain (fluid/block faces)");

		try {
			// the revival arm: under the full BINNIE ABSENT pin the bark dust STILL registers
			GT6ModDrivers.setDriver(MT.MD.BINNIE.mID, GT6ModDrivers.DriverLevel.ABSENT);
			assertFalse(GT6ModDrivers.isLoaded(MT.MD.BINNIE.mID), "pin precondition");
			assertTrue(GTMaterialItems.registrationOrder().contains(new GTMaterialItems.PrefixMaterial(OP.dust, MT.Bark)),
					"the bark dust pair survives its own domain's ABSENT pin (upstream MT.java:1243 dust() row, no mod axis on the item path — ruling id1745-4)");
		} finally {
			GT6ModDrivers.reset();
		}

		// the dormant negative: the RP-annotated fluid row keeps its domain under the pin — its
		// registration core still shells it live, so the crystallisationcrucible rows consuming
		// gt6:nikolinealloy_molten stay on the loader's unregistered-id face (true foreign sleep)
		try {
			GT6ModDrivers.setDriver(MT.MD.RP.mID, GT6ModDrivers.DriverLevel.ABSENT);
			assertFalse(GT6ModDrivers.isLoaded(MT.MD.RP.mID), "pin precondition");
			assertEquals(MT.MD.RP.mID, GT6ForeignMaterialAtlas.domainOf(MT.NikolineAlloy),
					"the RP PRIMARY attribution still answers (the fluid/block faces' truth)");
			GTMaterialItems.PrefixMaterial tNikolinePair = new GTMaterialItems.PrefixMaterial(OP.ingot, MT.NikolineAlloy);
			assertTrue(GTMaterialItems.registrationOrder().contains(tNikolinePair),
					"the item face stays pin-invariant (the alloy's GT6 ingot registers; only its annotated FLUID row stays dormant)");
		} finally {
			GT6ModDrivers.reset();
		}
	}

	/**
	 * The per-source ratchet: the frozen accounting table (SOURCES) must equal the shipped
	 * row stock partitioned by its wood axis AND the live beam-kind recomputation. A
	 * registration change turns the live check red — re-pour and bump consciously.
	 */
	@Test
	public void theSourceAccountingMatchesTheFrozenPartition() throws Exception {
		Map<String, Integer> tActual = new LinkedHashMap<>();
		for (JsonElement tElement : pourShipped()) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (isFixtureRow(tRow)) { // the fixture row accounts under its own SOURCES seat
				tActual.merge("fixture.smoke-restored", 1, Integer::sum);
				continue;
			}
			String tCite = tRow.get("comment").getAsString();
			String tWood = tCite.substring(tCite.indexOf('(') + 1, tCite.indexOf(';'));
			tActual.merge("walk." + tWood + ":167", 1, Integer::sum);
		}
		List<String> tDrift = new ArrayList<>();
		for (Map.Entry<String, Integer> tPin : SOURCES.entrySet()) {
			Integer tSeen = tActual.get(tPin.getKey());
			if (tPin.getValue().intValue() != (tSeen == null ? 0 : tSeen.intValue())) {
				tDrift.add(tPin.getKey() + " frozen " + tPin.getValue() + " shipped " + tSeen);
			}
		}
		Set<String> tUnaccounted = new HashSet<>(tActual.keySet());
		tUnaccounted.removeAll(SOURCES.keySet());
		assertTrue(tUnaccounted.isEmpty(), WASH + ": shipped rows outside the frozen accounting: " + tUnaccounted);
		assertTrue(tDrift.isEmpty(), WASH + ": the per-source accounting drifted — re-pour and bump SOURCES: " + tDrift);
		assertEquals(CENSUS_WALK, liveWalk().size(), WASH + ": the live beam-kind walk holds the 7x4 shape");
	}

	/**
	 * The live cross-check — recompute the walk over the port beam registration (the
	 * GT6BeamKind universe x the per-kind log mapping) and require the shipped rows to
	 * cover it EXACTLY, both directions.
	 */
	@Test
	public void theWalkMatchesTheLiveBeamKinds() throws Exception {
		Set<String> tExpected = liveWalk();
		Set<String> tShipped = new HashSet<>();
		for (JsonElement tElement : pourShipped()) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (isFixtureRow(tRow)) continue; // the fixture row is outside the beam-kind walk
			tShipped.add(rowKey(tRow));
		}
		Set<String> tMissing = new HashSet<>(tExpected);
		tMissing.removeAll(tShipped);
		Set<String> tStale = new HashSet<>(tShipped);
		tStale.removeAll(tExpected);
		assertTrue(tMissing.isEmpty(), WASH + ": the frozen snapshot trails the live walk (re-pour needed): " + tMissing);
		assertTrue(tStale.isEmpty(), WASH + ": the frozen snapshot holds rows the live walk no longer generates: " + tStale);
		assertEquals(tExpected.size(), tShipped.size(), WASH + ": the walk snapshot size equals the live walk");
	}

	/** Verbatim pins — 7 wood legs across the four waters, the full row shape (input, fluid 200, beam + bark, dur 64, eut 16). */
	@Test
	public void rowsAreUpstreamVerbatim() throws Exception {
		JsonArray tRows = pourShipped();
		assertVerbatim(tRows, "minecraft:oak_log", "minecraft:water", "gt6:oak_beam");
		assertVerbatim(tRows, "minecraft:spruce_log", "gt6:mnwtr", "gt6:spruce_beam");
		assertVerbatim(tRows, "minecraft:birch_log", "gt6:distilled_water", "gt6:birch_beam");
		assertVerbatim(tRows, "minecraft:jungle_log", "gt6:spdew", "gt6:jungle_beam");
		assertVerbatim(tRows, "minecraft:acacia_log", "minecraft:water", "gt6:acacia_beam");
		assertVerbatim(tRows, "minecraft:dark_oak_log", "gt6:spdew", "gt6:dark_oak_beam");
		assertVerbatim(tRows, "gt6:rubber_log", "gt6:mnwtr", "gt6:rubber_wood_beam");
	}

	/**
	 * The id/energy face: every gt6 item id is registered (the material universe for the
	 * bark dust, the live beam registry for the beams), the vanilla logs are the closed
	 * 6-log set, the fluid legs are the FL.waters whitelist at 7 rows each, eut/duration
	 * are the wrapper constants, and the smoke row stays gone.
	 */
	@Test
	public void theStockIsRegisteredFluidWhitelistedAndTyped() throws Exception {
		GTMaterialItems.initMaterials();
		Set<String> tItemUniverse = new HashSet<>();
		for (GTMaterialItems.PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			tItemUniverse.add("gt6:" + GTMaterialItems.itemIdOf(tPair.prefix(), tPair.material()));
		}
		Set<String> tBeamUniverse = new HashSet<>();
		for (GT6BeamKind tKind : GT6BeamKind.values()) {
			tBeamUniverse.add("gt6:" + GT6BeamBlocks.path(tKind));
		}
		Set<String> tTreeLogUniverse = new HashSet<>();
		for (GT6TreeKind tKind : GT6TreeKind.values()) {
			tTreeLogUniverse.add("gt6:" + GT6TreeBlocks.path(tKind, "_log"));
		}
		assertTrue(tItemUniverse.contains("gt6:dust_bark"), "the bark dust is a registered material item (WoodEntry.java:143)");
		Map<String, Integer> tLegCensus = new HashMap<>();
		int tWalkRows = 0, tFixtureRows = 0;
		for (JsonElement tElement : pourShipped()) {
			JsonObject tRow = tElement.getAsJsonObject();
			// the fixture row: the all-vanilla acceptance face (16/16, water 100) — outside the wrapper constants
			if (isFixtureRow(tRow)) {
				tFixtureRows++;
				assertEquals("minecraft:coarse_dirt", tRow.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString(),
						"the fixture row feeds coarse dirt");
				assertEquals("minecraft:dirt", tRow.getAsJsonArray("outputs").get(0).getAsJsonObject().get("item").getAsString(),
						"the fixture row yields dirt");
				assertEquals(100, tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("amount").getAsLong(),
						"the fixture water leg");
				assertEquals(16, tRow.get("eut").getAsLong(), "fixture eut 16");
				assertEquals(16, tRow.get("duration").getAsLong(), "fixture duration 16");
				continue;
			}
			tWalkRows++;
			// items: the gt6 legs resolve against their own universes
			for (String tLeg : new String[] {"inputs", "outputs"}) {
				for (JsonElement tSlot : tRow.getAsJsonArray(tLeg)) {
					String tId = tSlot.getAsJsonObject().get("item").getAsString();
					if (tId.startsWith("gt6:")) {
						assertTrue(tItemUniverse.contains(tId) || tBeamUniverse.contains(tId) || tTreeLogUniverse.contains(tId),
								"unregistered gt6 item id (the row would WARN-skip live): " + tId);
					} else {
						assertTrue(LOGS.contains(tId), "the input is a closed-set log id: " + tId);
					}
				}
			}
			// fluids: the FL.waters whitelist
			for (JsonElement tSlot : tRow.getAsJsonArray("fluidInputs")) {
				String tFluid = tSlot.getAsJsonObject().get("fluid").getAsString();
				assertTrue(LEGS.contains(tFluid), "the fluid leg is a known FL.waters id: " + tFluid);
				tLegCensus.merge(tFluid, 1, Integer::sum);
			}
			// the wrapper constants (RM.java:707)
			assertEquals(16, tRow.get("eut").getAsLong(), "eut 16 on every row");
			assertEquals(64, tRow.get("duration").getAsLong(), "duration 64 on every row");
		}
		assertEquals(CENSUS_WALK, tWalkRows, "the walk stock holds the 28-row census");
		assertEquals(1, tFixtureRows, "exactly one fixture row");
		for (String tLeg : LEGS) {
			assertEquals(7, tLegCensus.getOrDefault(tLeg, 0).intValue(), tLeg + ": exactly 7 rows (one per log)");
		}
	}

	/**
	 * The unpoured faces stay out: no debarking-alias water-1000 leg, no Beam2:3 generic
	 * wood beam output (the WOOD kind face + the :201 event output), and the inputs stay
	 * the closed 7-log set (no mod-log row grew through the event face).
	 */
	@Test
	public void theUnpouredFacesStayOut() throws Exception {
		for (JsonElement tElement : pourShipped()) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (isFixtureRow(tRow)) continue; // the fixture row rides its own water-100 face (documented in the head)
			assertEquals(200, tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("amount").getAsLong(),
					"no water-1000 debarking-alias leg (RM.java:716-718 stays unpoured)");
			assertTrue(LOGS.contains(tRow.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString()),
					"no mod-log event-face input (Loader_Recipes_OreDict.java:199/:201 stays unpoured)");
			for (JsonElement tSlot : tRow.getAsJsonArray("outputs")) {
				assertFalse("gt6:wood_beam".equals(tSlot.getAsJsonObject().get("item").getAsString()),
						"no generic wood-beam output (the WOOD kind face + the :201 Beam2:3 face stay unpoured)");
			}
		}
	}

	// ---------------------------------------------------------------- live walk

	/** The per-kind log mapping (the upstream WoodEntry logs; null = no static log-side row). */
	private static String logOf(GT6BeamKind aKind) {
		switch (aKind) {
		case OAK: return "minecraft:oak_log"; // LoaderWoodDictionary.java:51 log meta 0 -> Beam1:0
		case SPRUCE: return "minecraft:spruce_log"; // :52 -> Beam1:1
		case BIRCH: return "minecraft:birch_log"; // :53 -> Beam1:2
		case JUNGLE: return "minecraft:jungle_log"; // :54 -> Beam1:3
		case ACACIA: return "minecraft:acacia_log"; // :55 log2 meta 0 -> Beam2:0
		case DARK_OAK: return "minecraft:dark_oak_log"; // :56 log2 meta 1 -> Beam2:1
		case RUBBER_WOOD: return "gt6:" + GT6TreeBlocks.path(GT6TreeKind.RUBBER, "_log"); // :69 LogA:0 (beam face aligned to :175)
		case WOOD: return null; // DEFAULT_BEAM: no WoodEntry log upstream
		default: return null;
		}
	}

	private static final List<String> LOGS = List.of("minecraft:oak_log", "minecraft:spruce_log", "minecraft:birch_log",
			"minecraft:jungle_log", "minecraft:acacia_log", "minecraft:dark_oak_log",
			"gt6:" + GT6TreeBlocks.path(GT6TreeKind.RUBBER, "_log"));

	/** The live recomputation: every kind with a log x the four FL.waters legs (the :167 x :707 x :689 shape). */
	private static Set<String> liveWalk() {
		Set<String> rRows = new HashSet<>();
		for (GT6BeamKind tKind : GT6BeamKind.values()) {
			String tLog = logOf(tKind);
			if (tLog == null) continue;
			for (String tLeg : LEGS) {
				rRows.add("in:" + tLog + ":1;fluid:" + tLeg + ":200>out:gt6:" + GT6BeamBlocks.path(tKind)
						+ ":1+gt6:dust_bark:1>dur:64>16");
			}
		}
		return rRows;
	}

	// ---------------------------------------------------------------- helpers

	/** The row identity key: input;fluid > outputs > duration > eut (the magnet rowKey shape). */
	private static String rowKey(JsonObject aRow) {
		StringBuilder tKey = new StringBuilder("in:").append(aRow.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString())
				.append(":1;fluid:").append(aRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("fluid").getAsString())
				.append(':').append(aRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("amount").getAsLong());
		tKey.append(">out:");
		JsonArray tOut = aRow.getAsJsonArray("outputs");
		for (int i = 0; i < tOut.size(); i++) {
			if (i > 0) tKey.append('+');
			JsonObject tSlot = tOut.get(i).getAsJsonObject();
			tKey.append(tSlot.get("item").getAsString()).append(':').append(tSlot.get("count").getAsLong());
		}
		tKey.append(">dur:").append(aRow.get("duration").getAsLong()).append('>').append(aRow.get("eut").getAsLong());
		return tKey.toString();
	}

	/** The verbatim pin: the exact row exists with the wrapper constants and the beam+bark output pair. */
	private static void assertVerbatim(JsonArray aRows, String aLog, String aFluid, String aBeam) {
		for (JsonElement tElement : aRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (aLog.equals(tRow.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString())
					&& aFluid.equals(tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("fluid").getAsString())) {
				assertEquals(200, tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("amount").getAsLong(),
						aLog + " + " + aFluid + ": the :707 water 200 leg");
				JsonArray tOut = tRow.getAsJsonArray("outputs");
				assertEquals(2, tOut.size(), aLog + ": beam + bark");
				assertEquals(aBeam, tOut.get(0).getAsJsonObject().get("item").getAsString(), aLog + ": the beam output");
				assertEquals(1, tOut.get(0).getAsJsonObject().get("count").getAsLong(), aLog + ": beam x1");
				assertEquals("gt6:dust_bark", tOut.get(1).getAsJsonObject().get("item").getAsString(), aLog + ": the bark output");
				assertEquals(1, tOut.get(1).getAsJsonObject().get("count").getAsLong(), aLog + ": bark x1 (WoodEntry.java:143)");
				assertEquals(64, tRow.get("duration").getAsLong(), aLog + ": duration 64");
				assertEquals(16, tRow.get("eut").getAsLong(), aLog + ": eut 16");
				return;
			}
		}
		throw new AssertionError("no verbatim row " + aLog + " + " + aFluid);
	}
}
