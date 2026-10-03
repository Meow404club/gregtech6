package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The technological component families' recipe-parity test (task
 * debt-emitter-sensor-generators): the generated {@code data/gt6/recipes/component/*}
 * files are read verbatim off the classpath (the GT6CircuitsCraftingJsonTest
 * convention) and asserted on their upstream transcription faces — the
 * MultiItemTechnological.java:425-434 FIELD_GENERATORS "WPW","CGC","WPW" / :436-445
 * EMITTERS "SPC","WQP","CWS" / :447-456 SENSORS "P Q","PS ","CPP" grids over the
 * column ladders, plus the POUR MAP: the 20 live rows and the 10 declared CUTs
 * (rungs 7-9 — no {@code #gt6:circuit7..9} tags in the port; the FIELD_GENERATORS
 * ULV rung — no fine-wire carrier).
 */
public class GT6CompactComponentsJsonTest {

	private static JsonObject generated(String aPath) throws Exception {
		String tPath = "/data/gt6/recipes/" + aPath + ".json";
		try (InputStream tStream = GT6CompactComponentsJsonTest.class.getResourceAsStream(tPath)) {
			return tStream == null ? null : JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	private static String row(JsonObject aRow, int aIndex) {
		return aRow.getAsJsonArray("pattern").get(aIndex).getAsString();
	}

	private static JsonObject key(JsonObject aRow, char aLetter) {
		return aRow.getAsJsonObject("key").getAsJsonObject(String.valueOf(aLetter));
	}

	private static String tagOf(JsonObject aRow, char aLetter) {
		return key(aRow, aLetter).get("tag").getAsString();
	}

	private static String itemOf(JsonObject aRow, char aLetter) {
		return key(aRow, aLetter).get("item").getAsString();
	}

	// ------------------------------------------------- the pour map

	/** The 20 live rows exist: FIELD_GENERATORS 1-6, EMITTERS 0-6, SENSORS 0-6. */
	@Test
	public void theTwentyLiveRowsExist() throws Exception {
		String[] tTokens = {"ulv", "lv", "mv", "hv", "ev", "iv", "luv"};
		int tCount = 0;
		for (String tToken : tTokens) {
			assertNotNull(generated("component/signal_emitter_" + tToken), "signal_emitter_" + tToken);
			assertNotNull(generated("component/sensor_" + tToken), "sensor_" + tToken);
			tCount += 2;
			if (!"ulv".equals(tToken)) { // FIELD_GENERATORS ULV is CUT (no fine-wire carrier)
				assertNotNull(generated("component/field_generator_" + tToken), "field_generator_" + tToken);
				tCount++;
			}
		}
		assertEquals(20, tCount);
	}

	/** The 10 declared CUTs stay absent: the FG ULV row + every family's rungs 7-9 (no circuit7-9 tags). */
	@Test
	public void theTenCutRowsStayAbsent() throws Exception {
		assertNull(generated("component/field_generator_ulv"), "the wireFine(Os) column has no port carrier");
		for (String tFamily : new String[]{"field_generator", "signal_emitter", "sensor"}) {
			for (String tToken : new String[]{"zpm", "uv", "puv1"}) {
				assertNull(generated("component/" + tFamily + "_" + tToken), tFamily + "_" + tToken + " — no #gt6:circuit7..9");
			}
		}
	}

	// ------------------------------------------------- the grids + columns

	/** The FIELD_GENERATORS HV rung :428 — "WPW","CGC","WPW", W = wireGt04 Os, P = plateDouble(StainlessSteel), C = circuit3, G = EnderEye. */
	@Test
	public void theFieldGeneratorHvRowCarriesTheGridAndColumns() throws Exception {
		JsonObject tRow = generated("component/field_generator_hv");
		assertEquals("WPW", row(tRow, 0)); // :428 verbatim
		assertEquals("CGC", row(tRow, 1));
		assertEquals("WPW", row(tRow, 2));
		assertEquals("gt6:wire_osmium_elemental_gt04", itemOf(tRow, 'W'), "the :428 wireGt04(Os) column");
		assertEquals("gt6:plate_double_stainless_steel", itemOf(tRow, 'P'), "Electric_T[3] = StainlessSteel (MT.java:3691)");
		assertEquals("gt6:circuit3", tagOf(tRow, 'C'), "the OD_CIRCUITS[3] column");
		assertEquals("forge:gems/ender_eye", tagOf(tRow, 'G'), "the :428 EnderEye gem column");
		assertEquals("gt6:field_generator_hv", tRow.getAsJsonObject("result").get("item").getAsString());
	}

	/** The FIELD_GENERATORS gauge ladder :425-434 — gt01/gt02/gt04/gt06/gt08/gt10 over rungs 1-6. */
	@Test
	public void theFieldGeneratorOsmiumGaugeLadderIsVerbatim() throws Exception {
		String[] tTokens = {"lv", "mv", "hv", "ev", "iv", "luv"};
		String[] tWires = {"gt01", "gt02", "gt04", "gt06", "gt08", "gt10"};
		for (int i = 0; i < 6; i++) {
			assertEquals("gt6:wire_osmium_elemental_" + tWires[i], itemOf(generated("component/field_generator_" + tTokens[i]), 'W'),
					"the :" + (425 + i + 1) + " gauge column");
		}
	}

	/** The EMITTERS ULV rung :436 — "SPC","WQP","CWS" over the Pb rung of every wire ladder. */
	@Test
	public void theSignalEmitterUlvRowCarriesTheGridAndColumns() throws Exception {
		JsonObject tRow = generated("component/signal_emitter_ulv");
		assertEquals("SPC", row(tRow, 0)); // :436 verbatim
		assertEquals("WQP", row(tRow, 1));
		assertEquals("CWS", row(tRow, 2));
		assertEquals("gt6:wire_lead_gt04", itemOf(tRow, 'S'), "WIRES_04[0] = wireGt04(Pb) (MT.java:3613)");
		assertEquals("forge:gems/quartz", tagOf(tRow, 'Q'), "ANY.SiO2 → the quartz gem tag (the :436 column)");
		assertEquals("gt6:plate_curved_tin_alloy", itemOf(tRow, 'P'), "Electric_T[0] = TinAlloy");
		assertEquals("gt6:circuit0", tagOf(tRow, 'C'), "the OD_CIRCUITS[0] column");
		assertEquals("gt6:cable_lead_gt01", itemOf(tRow, 'W'), "CABLES_01[0] = cableGt01(Pb) (MT.java:3631)");
		assertEquals("gt6:signal_emitter_ulv", tRow.getAsJsonObject("result").get("item").getAsString());
	}

	/** The EMITTERS LuV rung :442 — the CABLES_01 Graphene fold: 'W' is the BARE wireGt01, not a cable (:3638). */
	@Test
	public void theSignalEmitterLuvRowFoldsTheGrapheneCableColumn() throws Exception {
		JsonObject tRow = generated("component/signal_emitter_luv");
		assertEquals("gt6:wire_graphene_gt04", itemOf(tRow, 'S'), "WIRES_04[6] = wireGt04(Graphene)");
		assertEquals("forge:gems/nether_star", tagOf(tRow, 'Q'), "the :442 NetherStar column");
		assertEquals("gt6:plate_curved_iridium", itemOf(tRow, 'P'), "Electric_T[6] = Ir");
		assertEquals("gt6:circuit6", tagOf(tRow, 'C'));
		assertEquals("gt6:wire_graphene_gt01", itemOf(tRow, 'W'), "CABLES_01[6] folds to wireGt01(Graphene) — the :3638 wire row");
	}

	/** The SENSORS grid :447-456 — "P Q","PS ","CPP" with the space cells, the HV rung's columns. */
	@Test
	public void theSensorHvRowCarriesTheSpaceyGridAndColumns() throws Exception {
		JsonObject tRow = generated("component/sensor_hv");
		assertEquals("P Q", row(tRow, 0)); // :450 verbatim — the space cell is a real column of the upstream grid
		assertEquals("PS ", row(tRow, 1));
		assertEquals("CPP", row(tRow, 2));
		assertEquals("forge:gems/emerald", tagOf(tRow, 'Q'), "the :450 Emerald column");
		assertEquals("gt6:wire_gold_gt01", itemOf(tRow, 'S'), "WIRES_01[3] = wireGt01(Au) (MT.java:3595)");
		assertEquals("gt6:plate_curved_stainless_steel", itemOf(tRow, 'P'), "Electric_T[3]");
		assertEquals("gt6:circuit3", tagOf(tRow, 'C'));
		assertEquals("gt6:sensor_hv", tRow.getAsJsonObject("result").get("item").getAsString());
	}

	/** The gem ladder :436-445 — SiO2×3/Emerald/EnderPearl/EnderEye/NetherStar×4 over the live rungs. */
	@Test
	public void theEmitterSensorGemLadderIsVerbatim() throws Exception {
		String[] tTokens = {"ulv", "lv", "mv", "hv", "ev", "iv", "luv"};
		String[] tGems = {"quartz", "quartz", "quartz", "emerald", "ender_pearl", "ender_eye", "nether_star"};
		for (int i = 0; i < 7; i++) {
			assertEquals("forge:gems/" + tGems[i], tagOf(generated("component/signal_emitter_" + tTokens[i]), 'Q'), "the :" + (436 + i) + " gem column");
			assertEquals("forge:gems/" + tGems[i], tagOf(generated("component/sensor_" + tTokens[i]), 'Q'), "the :" + (447 + i) + " gem column");
		}
		// the unlock-advancement stop (2026-10-03 user ruling, remember id1359): the row rides alone
		assertNull(getClass().getResourceAsStream("/data/gt6/advancements/recipes/misc/component/signal_emitter_ulv.json"));
	}
}
