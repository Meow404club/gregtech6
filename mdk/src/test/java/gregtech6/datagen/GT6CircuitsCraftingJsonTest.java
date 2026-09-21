package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The circuits C-column recipe-parity test (task p33-circuits-crafting-c acceptance ②):
 * the generated data/gt6/recipes/ files are read verbatim off the classpath (the
 * GT6DieselCraftingJsonTest generated-resources convention) and asserted on their
 * upstream-transcription faces — the Loader_MultiTileEntities.java:1184-1189 six machine
 * rows (pattern + the 'C' circuit tag + the ruling-B substitution columns) and the
 * ItemIntegratedCircuit.java:58-85 26-row self-crafting block (the base/reset/24
 * programming rows, the configuration field per row).
 */
public class GT6CircuitsCraftingJsonTest {

	private static JsonObject generated(String aPath) throws Exception {
		String tPath = "/data/gt6/recipes/" + aPath + ".json";
		try (InputStream tStream = GT6CircuitsCraftingJsonTest.class.getResourceAsStream(tPath)) {
			assertNotNull(tStream, tPath + " rides the generated-resources classpath");
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	private static JsonArray pattern(JsonObject aRow) {
		return aRow.getAsJsonArray("pattern");
	}

	private static JsonObject key(JsonObject aRow, char aLetter) {
		return aRow.getAsJsonObject("key").getAsJsonObject(String.valueOf(aLetter));
	}

	/** The row-1 letter pattern of the six machine rows (the :1184-1189 transcription). */
	private static String row1(JsonObject aRow) {
		return pattern(aRow).get(0).getAsString();
	}

	// ------------------------------------------------- the six machine rows

	/** The Ventilation Unit :1184 — "FwF"/"CMC"/"EdE", C = #gt6:circuit3 (the OD_CIRCUITS[3] column), F/E = the ruling-B substitutions. */
	@Test
	public void theVentilationUnitRowCarriesTheGridAndTheCircuit3Tag() throws Exception {
		JsonObject tRow = generated("part_circuit/ventilation_unit");
		assertEquals("FFF", row1(tRow));
		assertEquals("wCw", pattern(tRow).get(1).getAsString());
		assertEquals("EEE", pattern(tRow).get(2).getAsString());
		JsonObject tC = key(tRow, 'C');
		assertEquals("gt6:circuit3", tC.get("tag").getAsString(), "the OD_CIRCUITS[3] column → the #gt6:circuit3 tag (the battery 'X' precedent)");
		JsonObject tE = key(tRow, 'E');
		assertEquals("gt6:electric_motor_t2", tE.get("item").getAsString(), "DEV: IL.MOTORS[1] → the T2 bridge motor");
		assertEquals("gt6:ventilation_unit", tRow.getAsJsonObject("result").get("item").getAsString());
	}

	/** The Versatile PU :1185 — "DCS"/"CMC"/"RCE" over the four gem tags + #gt6:circuit6. */
	@Test
	public void theVersatileProcessorRowCarriesTheFourGemTagsAndCircuit6() throws Exception {
		JsonObject tRow = generated("part_circuit/processor_unit_versatile");
		assertEquals("DCS", row1(tRow));
		assertEquals("CMC", pattern(tRow).get(1).getAsString());
		assertEquals("RCE", pattern(tRow).get(2).getAsString());
		assertEquals("forge:gems/diamond", key(tRow, 'D').get("tag").getAsString(), "DEV: IL.Processor_Crystal_Diamond → #forge:gems/diamond");
		assertEquals("forge:gems/sapphire", key(tRow, 'S').get("tag").getAsString(), "DEV: IL.Processor_Crystal_Sapphire");
		assertEquals("forge:gems/ruby", key(tRow, 'R').get("tag").getAsString(), "DEV: IL.Processor_Crystal_Ruby");
		assertEquals("forge:gems/emerald", key(tRow, 'E').get("tag").getAsString(), "DEV: IL.Processor_Crystal_Emerald");
		assertEquals("gt6:circuit6", key(tRow, 'C').get("tag").getAsString(), "the OD_CIRCUITS[6] column");
		assertEquals("gt6:casing_small_steel_galvanized", key(tRow, 'M').get("item").getAsString(), "the casingMachine fold (the Locker precedent)");
		assertEquals("gt6:processor_unit_versatile", tRow.getAsJsonObject("result").get("item").getAsString());
	}

	/** The four ladder PUs :1186-1189 — "PCP"/"CMC"/"PCP" over the row's crystal gem tag. */
	@Test
	public void theFourLadderProcessorRowsCarryTheirCrystalGemTag() throws Exception {
		List<String> tPaths = List.of(
				"part_circuit/processor_unit_logic",        // :1186 Diamond
				"part_circuit/processor_unit_control",      // :1187 Ruby
				"part_circuit/processor_unit_storage",      // :1188 Emerald
				"part_circuit/processor_unit_conversion");  // :1189 Sapphire
		List<String> tGems = List.of("diamond", "ruby", "emerald", "sapphire");
		for (int i = 0; i < tPaths.size(); i++) {
			JsonObject tRow = generated(tPaths.get(i));
			assertEquals("PCP", row1(tRow), tPaths.get(i) + ": row 1");
			assertEquals("CMC", pattern(tRow).get(1).getAsString(), tPaths.get(i) + ": row 2");
			assertEquals("PCP", pattern(tRow).get(2).getAsString(), tPaths.get(i) + ": row 3");
			assertEquals("forge:gems/" + tGems.get(i), key(tRow, 'P').get("tag").getAsString(),
					tPaths.get(i) + ": DEV the crystal column → the gem tag");
			assertEquals("gt6:circuit6", key(tRow, 'C').get("tag").getAsString(), tPaths.get(i) + ": the OD_CIRCUITS[6] column");
			assertEquals("gt6:" + tPaths.get(i).substring("part_circuit/".length()), tRow.getAsJsonObject("result").get("item").getAsString(),
					tPaths.get(i) + ": the result path");
		}
	}

	// ------------------------------------------------- the 26 circuit rows

	/** The base row :58 — "GhG"/"SSS"/"GwG" over the small gears + rods + h/w, configuration 0. */
	@Test
	public void theBaseRowCarriesTheGearGridAndConfigurationZero() throws Exception {
		JsonObject tRow = generated("integrated_circuit");
		assertEquals("GhG", row1(tRow));
		assertEquals("SSS", pattern(tRow).get(1).getAsString());
		assertEquals("GwG", pattern(tRow).get(2).getAsString());
		assertEquals("forge:small_gears/iron", key(tRow, 'G').get("tag").getAsString(), "the OP.gearGtSmall.dat(ANY.Iron) column");
		assertEquals("forge:rods/iron", key(tRow, 'S').get("tag").getAsString(), "the OP.stick.dat(ANY.Iron) column");
		assertEquals("gt6:tools/hard_hammer", key(tRow, 'h').get("tag").getAsString(), "the CR 'h' letter");
		assertEquals("gt6:tools/wrench", key(tRow, 'w').get("tag").getAsString(), "the CR 'w' letter");
		assertEquals(0, tRow.get("configuration").getAsInt(), "the base result = configuration 0 (upstream :58 ST.make(this, 1, 0))");
		assertEquals("gt6:integrated_circuit", tRow.getAsJsonObject("result").get("item").getAsString());
		assertEquals("gt6:circuit_program", tRow.get("type").getAsString(), "the shared serializer type");
	}

	/** The reset row :59 — the circuit back to configuration 0 (the 1x1 shaped re-expression: one circuit anywhere in the grid, the 21.1 shared-codec ruling). */
	@Test
	public void theResetRowTakesAnyCircuitBackToZero() throws Exception {
		JsonObject tRow = generated("integrated_circuit_reset");
		assertEquals("P", tRow.getAsJsonArray("pattern").get(0).getAsString(), "the 1x1 shaped form");
		assertEquals(1, tRow.getAsJsonObject("key").size(), "the single circuit key");
		assertEquals("gt6:integrated_circuit", tRow.getAsJsonObject("key").getAsJsonObject("P").get("item").getAsString());
		assertEquals(0, tRow.get("configuration").getAsInt());
	}

	/** All 24 programming rows exist, each carries its configuration number and the P/d alphabet (:61-85). */
	@Test
	public void theTwentyFourProgrammingRowsCarryTheirConfigurationAndAlphabet() throws Exception {
		// the upstream :61-85 shapes per configuration (the 2-line rows pad with a blank third line)
		List<String> tShapes = List.of(
				"d / P", "d /P ", " d/P ", "Pd/  ", "P / d", "P /d ", " P/d ", "dP/  ", "P d",
				"P  /  d", "P  /   /  d", "P  /   / d ", "  P/   /  d", "  P/   / d ", "  P/   /d  ", "  P/d  /   ",
				"   /   /d P", "   /d  /  P", "d  /   /  P", " d /   /  P", "d  /   /P  ", " d /   /P  ",
				"  d/   /P  ", "   /  d/P  ");
		for (int i = 1; i <= 24; i++) {
			JsonObject tRow = generated("integrated_circuit/config_" + i);
			assertEquals(i, tRow.get("configuration").getAsInt(), "config_" + i + ": the fixed result configuration");
			assertEquals("gt6:integrated_circuit", tRow.getAsJsonObject("result").get("item").getAsString(), "config_" + i + ": the circuit result");
			assertEquals("gt6:circuit_program", tRow.get("type").getAsString(), "config_" + i + ": the shared serializer");
			JsonObject tKey = tRow.getAsJsonObject("key");
			assertEquals("gt6:integrated_circuit", tKey.getAsJsonObject("P").get("item").getAsString(), "config_" + i + ": the 'P' base-circuit column");
			assertEquals("gt6:tools/screwdriver", tKey.getAsJsonObject("d").get("tag").getAsString(), "config_" + i + ": the 'd' screwdriver letter");
			// the shape faces: the flattened pattern matches the upstream sparse grid
			JsonArray tPattern = tRow.getAsJsonArray("pattern");
			StringBuilder tFlat = new StringBuilder();
			for (int r = 0; r < tPattern.size(); r++) {
				if (r > 0) tFlat.append('/');
				tFlat.append(tPattern.get(r).getAsString());
			}
			String tExpect = tShapes.get(i - 1);
			while (tFlat.length() < tExpect.length() && tFlat.charAt(tFlat.length() - 1) == '/') tFlat.setLength(tFlat.length() - 1);
			assertEquals(tExpect, tFlat.toString(), "config_" + i + ": the :61-85 grid transcription");
		}
	}

	/** The band census: 6 machine rows + 26 circuit rows = 32 new recipe files. */
	@Test
	public void theBandCensus() throws Exception {
		for (String tPath : List.of(
				"part_circuit/ventilation_unit", "part_circuit/processor_unit_versatile",
				"part_circuit/processor_unit_logic", "part_circuit/processor_unit_control",
				"part_circuit/processor_unit_storage", "part_circuit/processor_unit_conversion")) {
			assertNotNull(generated(tPath));
		}
		assertNotNull(generated("integrated_circuit"));
		assertNotNull(generated("integrated_circuit_reset"));
		for (int i = 1; i <= 24; i++) {
			assertNotNull(generated("integrated_circuit/config_" + i));
		}
	}
}
