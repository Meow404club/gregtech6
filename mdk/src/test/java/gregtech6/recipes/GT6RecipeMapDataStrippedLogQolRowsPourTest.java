package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
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

import gregtech6.block.tree.GT6BeamKind;
import gregtech6.registry.GT6BeamBlocks;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

/**
 * The stripped-log-qol row-stock test (the user ruling 2026-10-01: the stripped logs walk
 * the QoL route, NOT the #gt6:beam_wood tag): the six vanilla stripped logs
 * (minecraft:stripped_oak/spruce/birch/jungle/acacia/dark_oak_log) join
 * <ul>
 * <li><b>sawing.json</b> as 30 tail rows (6 species x the five legs) — every twin a
 *     byte-level mirror of its unstripped Woods:169 log row (same plank x6 + the bark dust
 *     x1 + the identical duration/eut per leg; verbatim values, none invented), each row
 *     carrying a 'qol twin of' comment naming its twin — the census 4274 -> 4304;</li>
 * <li><b>generifier.json</b> as 6 rows into the IL.Beam target gt6:wood_beam (the
 *     already-debarked wood's beam face, the row shape of the Woods:190 beam rows,
 *     duration 1, eut 0) — the census 9117 -> 9123 -> 9187 -> 9123 (the +64 task wire-gt-registration generifier walk re-pour, then the −64 task wiregt-prefix-item-retirement re-pour: the retired wire items left the walk);</li>
 * <li><b>pressurewasher.json</b> as a HEAD-ONLY declaration: the 28 wash rows' explicit
 *     input id set contains NO stripped_* log (the debarking wash strips the bark — an
 *     already-stripped log has no bark face to wash), pinned live by
 *     {@link #noRowWashesAStrippedLog}.</li>
 * </ul>
 * Declared non-faces: the _wood forms inherit the sawing file's _log-only coverage; the
 * mangrove/cherry/crimson/warped (+ bamboo) stripped logs sit outside the GT6 wood
 * dictionary domain — their unstripped logs pour no row in any of the three files either
 * (TRUE NEGATIVE, the LIST_WOODS walk LoaderWoodDictionary.java:51-56); #gt6:beam_wood
 * (the upstream 8-beam OD.beamWood mirror, BlockBaseBeam.java:48) is untouched — the QoL
 * rows are explicit recipe rows, not tag membership. KJS face: datapack-domain JSON,
 * naturally moddable, zero adaptation. RCON: exempt (offline data card, no RCON group).
 */
public class GT6RecipeMapDataStrippedLogQolRowsPourTest extends GTRecipesOfflineTestBase {

	/** The censuses after this card: sawing +30, generifier +6, wash untouched at 28 (the generifier total back at 9123 — the −64 task wiregt-prefix-item-retirement re-pour). */
	private static final int SAWING_CENSUS = 4304, GENERIFIER_CENSUS = 9123, WASH_CENSUS = 28;

	/** The six vanilla species (LoaderWoodDictionary.java:51-56, the LIST_WOODS vanilla subset). */
	private static final String[] SPECIES = {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak"};

	/** The five leg rows of one RM.sawing call, verbatim (RM.java:725-730: fluid, duration mult, amount mult). */
	private static final Object[][] LEGS = {
			{"minecraft:water", 4L, 4L}, {"gt6:spdew", 4L, 4L}, {"gt6:mnwtr", 4L, 4L},
			{"gt6:distilled_water", 3L, 3L}, {"gt6:lubricant", 1L, 1L}};

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

	/** Reads one shipped recipe_maps file verbatim, pours it under its map key, returns its parsed row array. */
	private JsonArray pourShipped(String aKey) throws Exception {
		String tPath = "/data/gt6/recipe_maps/" + aKey + ".json";
		InputStream tStream = GT6RecipeMapDataStrippedLogQolRowsPourTest.class.getResourceAsStream(tPath);
		assertNotNull(tStream, "the shipped file " + tPath + " rides the test classpath");
		JsonObject tDoc = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		GT6RecipeMapJsonLoader.pour(Map.of(new ResourceLocation("gt6", aKey), tDoc.deepCopy()));
		return tDoc.getAsJsonArray("recipes");
	}

	private String headOf(String aKey) throws Exception {
		InputStream tStream = GT6RecipeMapDataStrippedLogQolRowsPourTest.class.getResourceAsStream("/data/gt6/recipe_maps/" + aKey + ".json");
		assertNotNull(tStream);
		return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject().get("comment").getAsString();
	}

	/** The census: the three touched maps pour their full expected counts — zero WARN-skips. */
	@Test
	public void theFilesPourTheirFullCensusesWithZeroSkips() throws Exception {
		pourShipped("sawing");
		assertEquals(SAWING_CENSUS, GT6RecipeMapJsonLoader.mapFor("sawing").mRecipeList.size(), "sawing: 4274 + 30 twin rows");
		assertEquals(SAWING_CENSUS, GT6RecipeMapJsonLoader.pouredCount("sawing"), "sawing: zero skips");
		pourShipped("generifier");
		assertEquals(GENERIFIER_CENSUS, GT6RecipeMapJsonLoader.mapFor("generifier").mRecipeList.size(), "generifier: 9181 + 6 stripped rows (the −64 wiregt-prefix-item-retirement re-pour nets out the wire-gt walk rows)");
		assertEquals(GENERIFIER_CENSUS, GT6RecipeMapJsonLoader.pouredCount("generifier"), "generifier: zero skips");
		pourShipped("pressurewasher");
		assertEquals(WASH_CENSUS, GT6RecipeMapJsonLoader.mapFor("pressurewasher").mRecipeList.size(), "wash: the head-only card moves no row");
		assertEquals(WASH_CENSUS, GT6RecipeMapJsonLoader.pouredCount("pressurewasher"), "wash: zero skips");
	}

	/** The file-head declaration pins: the QoL increment, the twin ruling, the domain-out, the tag pin. */
	@Test
	public void theFileHeadsCarryTheQolDeclarations() throws Exception {
		String tSawingHead = headOf("sawing"), tLowerSawing = tSawingHead.toLowerCase(Locale.ROOT);
		assertTrue(tSawingHead.contains("QOL INCREMENT (task stripped-log-qol)"), "sawing: the increment is declared");
		assertTrue(tLowerSawing.contains("verbatim") && tLowerSawing.contains("qol twin"), "sawing: the twin-mirror ruling is declared");
		assertTrue(tLowerSawing.contains("_log form only"), "sawing: the _wood-form absence is declared");
		assertTrue(tSawingHead.contains("OUT-OF-DOMAIN") && tSawingHead.contains("mangrove/cherry/crimson/warped"),
				"sawing: the domain-out TRUE NEGATIVE is declared");
		assertTrue(tSawingHead.contains("#gt6:beam_wood"), "sawing: the beam tag pin is declared");

		String tGenHead = headOf("generifier");
		assertTrue(tGenHead.contains("QOL INCREMENT (task stripped-log-qol)"), "generifier: the increment is declared");
		assertTrue(tGenHead.contains("gt6:wood_beam"), "generifier: the IL.Beam target is named");
		assertTrue(tGenHead.contains("OUT-OF-DOMAIN") && tGenHead.contains("mangrove/cherry/crimson/warped"),
				"generifier: the domain-out TRUE NEGATIVE is declared");
		assertTrue(tGenHead.contains("#gt6:beam_wood"), "generifier: the beam tag pin is declared");

		String tWashHead = headOf("pressurewasher");
		assertTrue(tWashHead.contains("STRIPPED EXCLUSION (task stripped-log-qol)"), "wash: the exclusion is declared");
		assertTrue(tWashHead.toLowerCase(Locale.ROOT).contains("no stripped_* log"), "wash: the explicit id set excludes stripped_*");
		assertTrue(tWashHead.contains("OUT-OF-DOMAIN") && tWashHead.contains("mangrove/cherry/crimson/warped"),
				"wash: the domain-out TRUE NEGATIVE is declared");
		assertTrue(tWashHead.contains("#gt6:beam_wood"), "wash: the beam tag pin is declared");
	}

	/**
	 * THE TWIN-MIRROR PIN (acceptance 2): for every species and every leg, the stripped row
	 * equals its unstripped Woods:169 log row field-by-field (outputs, duration, eut, fluid
	 * leg) — the user ruling's verbatim mirror, verified live against the shipped file.
	 */
	@Test
	public void theSawingTwinsMirrorTheirLogRowsFieldByField() throws Exception {
		JsonArray tRows = pourShipped("sawing");
		int tTwinRows = 0;
		for (String tSpecies : SPECIES) {
			for (Object[] tLeg : LEGS) {
				JsonObject tLog = findRow(tRows, "minecraft:" + tSpecies + "_log", (String) tLeg[0]);
				JsonObject tStripped = findRow(tRows, "minecraft:stripped_" + tSpecies + "_log", (String) tLeg[0]);
				assertEquals(tLog.getAsJsonArray("outputs"), tStripped.getAsJsonArray("outputs"),
						tSpecies + ": the twin output tail (planks + bark dust) mirrors its log row");
				assertEquals(tLog.get("duration").getAsLong(), tStripped.get("duration").getAsLong(),
						tSpecies + ": the twin duration mirrors its log row on the " + tLeg[0] + " leg");
				assertEquals(tLog.get("eut").getAsLong(), tStripped.get("eut").getAsLong(), tSpecies + ": the twin eut mirrors");
				assertEquals(tLog.getAsJsonArray("fluidInputs"), tStripped.getAsJsonArray("fluidInputs"),
						tSpecies + ": the twin fluid leg mirrors its log row");
				assertEquals("qol twin of minecraft:" + tSpecies + "_log (task stripped-log-qol)",
						tStripped.get("comment").getAsString(), tSpecies + ": the twin names its log row");
				tTwinRows++;
			}
		}
		assertEquals(30, tTwinRows, "6 species x 5 legs = the 30 twin rows");
	}

	/** The verbatim spot checks (acceptance 2, the literal faces): the oak water leg and the dark_oak distilled-water leg. */
	@Test
	public void theTwinMirrorVerbatimSpotChecks() throws Exception {
		JsonArray tRows = pourShipped("sawing");
		assertRow(findRow(tRows, "minecraft:stripped_oak_log", "minecraft:water"),
				">minecraft:oak_planks:6>gt6:dust_bark:1", 512, 16, 16);
		assertRow(findRow(tRows, "minecraft:stripped_dark_oak_log", "gt6:distilled_water"),
				">minecraft:dark_oak_planks:6>gt6:dust_bark:1", 384, 16, 12);
	}

	/** The input item of a row, or "" for the fluid-only rows (empty inputs array). */
	private static String inputItem(JsonObject aRow) {
		JsonArray tInputs = aRow.getAsJsonArray("inputs");
		return tInputs.size() == 0 ? "" : tInputs.get(0).getAsJsonObject().get("item").getAsString();
	}

	/** The generifier stripped face: exactly 6 rows, each into the IL.Beam target, the Woods:190 row shape. */
	@Test
	public void theGenerifierStrippedFaceIsTheWoodBeam() throws Exception {
		JsonArray tRows = pourShipped("generifier");
		Set<String> tStrippedInputs = new HashSet<>();
		for (JsonElement tElement : tRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			String tIn = inputItem(tRow);
			if (!tIn.contains("stripped")) continue;
			tStrippedInputs.add(tIn);
			assertTrue(tRow.get("comment").getAsString().startsWith("task stripped-log-qol - "),
					tIn + ": the row cites the QoL card");
			assertEquals(">gt6:wood_beam:1", outputsTail(tRow), tIn + ": the IL.Beam generify target");
			assertEquals(1, tRow.get("duration").getAsLong(), tIn + ": the Woods:190 row-shape duration");
			assertEquals(0, tRow.get("eut").getAsLong(), tIn + ": the Woods:190 row-shape eut");
			assertFalse(tRow.has("fluidInputs"), tIn + ": the generify rows carry no fluids");
		}
		Set<String> tExpected = new HashSet<>();
		for (String tSpecies : SPECIES) tExpected.add("minecraft:stripped_" + tSpecies + "_log");
		assertEquals(tExpected, tStrippedInputs, "exactly the six vanilla stripped logs — the mangrove/cherry/crimson/warped"
				+ " (+ bamboo) stripped logs stay outside the GT6 wood dictionary (the TRUE NEGATIVE face, live)");
	}

	/**
	 * The id universe (acceptance 4): the vanilla stripped whitelist is bound to the LIVE
	 * offline vanilla registry (BuiltInRegistries.ITEM.containsKey — a typo'd id would
	 * WARN-skip at runtime), and every minecraft: id the QoL rows touch sits in that exact
	 * six-item set; the gt6 output rides the beam universe.
	 */
	@Test
	public void theVanillaStrippedUniverseIsRegisteredOffline() throws Exception {
		Set<String> tStrippedIds = new HashSet<>();
		for (String tSpecies : SPECIES) tStrippedIds.add("minecraft:stripped_" + tSpecies + "_log");
		Set<String> tUnregistered = new HashSet<>();
		for (String tId : tStrippedIds) {
			if (!BuiltInRegistries.ITEM.containsKey(new ResourceLocation(tId))) tUnregistered.add(tId);
		}
		assertTrue(tUnregistered.isEmpty(), "the vanilla registry must know every stripped id: " + tUnregistered);
		// every minecraft: id in the QoL rows is in the exact whitelist (no stray/typo'd ids)
		Set<String> tExpectedIds = new HashSet<>(tStrippedIds);
		for (String tSpecies : SPECIES) tExpectedIds.add("minecraft:" + tSpecies + "_planks");
		Set<String> tSeen = new HashSet<>();
		for (JsonElement tElement : pourShipped("sawing")) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (!tRow.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString().contains("stripped")) continue;
			for (String tLeg : new String[] {"inputs", "outputs"}) {
				for (JsonElement tSlot : tRow.getAsJsonArray(tLeg)) {
					String tId = tSlot.getAsJsonObject().get("item").getAsString();
					if (tId.startsWith("minecraft:")) tSeen.add(tId);
				}
			}
		}
		assertEquals(tExpectedIds, tSeen, "sawing: exactly the whitelisted minecraft: ids on the twin rows");
		// the generifier output rides the live beam universe (GT6BeamBlocks.path, the beam-blocks-register face)
		Set<String> tBeamUniverse = new HashSet<>();
		for (GT6BeamKind tKind : GT6BeamKind.values()) tBeamUniverse.add("gt6:" + GT6BeamBlocks.path(tKind));
		assertTrue(tBeamUniverse.contains("gt6:wood_beam"), "the generify target gt6:wood_beam lives in the beam universe");
	}

	/** The tag-membership pin (acceptance 3): the beam mirror stays the registration walk, the QoL rows are explicit rows. */
	@Test
	public void theBeamTagMirrorStaysTheRegistrationWalk() throws Exception {
		// task beam-fireproof-closeout: 8 -> 21 kinds (Beam3/A/B/C residual families join) —
		// the beam_wood tag rides the live registration walk (BLOCKS + FIREPROOF_BLOCKS), still
		// NO stripped member joins (a stripped log is a log face, never a beam face)
		assertEquals(21, GT6BeamKind.values().length,
				"#gt6:beam_wood mirrors the upstream beam block set (BlockBaseBeam.java:48 OD.beamWood, now incl. the Beam3/A/B/C families + the fireproof twins) — no stripped member joins");
		// the beam-input segments of the two sawing/generifier faces keep their exact pre-card shape
		int tSawingBeamRows = 0;
		for (JsonElement tElement : pourShipped("sawing")) {
			String tIn = inputItem(tElement.getAsJsonObject());
			if (tIn.endsWith("_beam") && !tIn.contains("stripped")) tSawingBeamRows++;
		}
		assertEquals(40, tSawingBeamRows, "sawing: the beam segment stays 7 BeamEntry faces x5 + the :66 row x5");
		int tGenBeamRows = 0;
		for (JsonElement tElement : pourShipped("generifier")) {
			if (inputItem(tElement.getAsJsonObject()).endsWith("_beam")) tGenBeamRows++;
		}
		assertEquals(7, tGenBeamRows, "generifier: the beam segment stays the 7 non-degenerate faces");
	}

	/** The stripped logs are consumers only: no row of the three files OUTPUTS a stripped id. */
	@Test
	public void noStrippedRowIsAnOutput() throws Exception {
		for (String tKey : new String[] {"sawing", "generifier", "pressurewasher"}) {
			for (JsonElement tElement : pourShipped(tKey)) {
				JsonObject tRow = tElement.getAsJsonObject();
				if (!tRow.has("outputs")) continue; // the fluid-output rows ride fluidOutputs only
				for (JsonElement tSlot : tRow.getAsJsonArray("outputs")) {
					assertFalse(tSlot.getAsJsonObject().get("item").getAsString().contains("stripped"),
							tKey + ": a stripped log is never an output");
				}
			}
		}
	}

	// ------------------------------------------------------------------ helpers

	private static JsonObject findRow(JsonArray aRows, String aInputItem, String aFluid) {
		for (JsonElement tElement : aRows) {
			JsonObject tRow = tElement.getAsJsonObject();
			if (aInputItem.equals(tRow.getAsJsonArray("inputs").get(0).getAsJsonObject().get("item").getAsString())
					&& aFluid.equals(tRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("fluid").getAsString())) return tRow;
		}
		throw new AssertionError("no row " + aInputItem + " on " + aFluid);
	}

	/** Full-row pin: outputs tail (">id:count" segments), duration, eut, fluid amount. */
	private static void assertRow(JsonObject aRow, String aOutputsTail, long aDuration, long aEut, long aFluidAmount) {
		StringBuilder tOutputs = new StringBuilder();
		for (JsonElement tOut : aRow.getAsJsonArray("outputs")) {
			JsonObject tSlot = tOut.getAsJsonObject();
			tOutputs.append('>').append(tSlot.get("item").getAsString()).append(':').append(tSlot.get("count").getAsInt());
		}
		assertEquals(aOutputsTail, tOutputs.toString(), "the output slot tail");
		assertEquals(aDuration, aRow.get("duration").getAsLong(), "the leg duration");
		assertEquals(aEut, aRow.get("eut").getAsLong(), "the call eut");
		assertEquals(aFluidAmount, aRow.getAsJsonArray("fluidInputs").get(0).getAsJsonObject().get("amount").getAsInt(),
				"the leg fluid amount");
	}

	private static String outputsTail(JsonObject aRow) {
		StringBuilder tOutputs = new StringBuilder();
		for (JsonElement tOut : aRow.getAsJsonArray("outputs")) {
			JsonObject tSlot = tOut.getAsJsonObject();
			tOutputs.append('>').append(tSlot.get("item").getAsString()).append(':').append(tSlot.get("count").getAsInt());
		}
		return tOutputs.toString();
	}
}
