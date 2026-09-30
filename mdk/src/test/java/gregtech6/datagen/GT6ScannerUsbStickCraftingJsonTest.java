package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The debt-scanner-t3-usb-stick recipe-parity test: the five generated rows are read
 * verbatim off the classpath (the GT6CompactComponentsJsonTest convention) and
 * asserted on their upstream transcription faces —
 * <ul>
 * <li>the Molecular Scanner T3 controller row, Loader_MultiTileEntities.java:1551
 *     ("DXE","FMF","RYS" — the only active scanner rung): the D/E/R/S
 *     Processor_Crystal gem-tag fold (the circuits-c convention) + the F/X/Y HV
 *     component columns (the debt-emitter-sensor-generators items) — the Q1=(b) seam
 *     RESTORE;</li>
 * <li>the four USB Stick rows, MultiItemTechnological.java:796-799 ("xWd","PCP","TCT")
 *     — the usb-peripherals Q3 pool closing: circuit3-6 tags + the WIRES_01[3..6]
 *     Au/Al/Pt/Graphene ladder + the plate/screw Al/StainlessSteel/Cr/Ti ladder + the
 *     wirecutter/screwdriver tool letters.</li>
 * </ul>
 */
public class GT6ScannerUsbStickCraftingJsonTest {

	private static JsonObject generated(String aPath) throws Exception {
		String tPath = "/data/gt6/recipes/" + aPath + ".json";
		try (InputStream tStream = GT6ScannerUsbStickCraftingJsonTest.class.getResourceAsStream(tPath)) {
			assertNotNull(tStream, "the generated file must be committed: " + tPath);
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
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

	// ------------------------------------------------- the Molecular Scanner T3 restore

	/** The :1551 row VERBATIM — "DXE","FMF","RYS" over the gem-tag D/E/R/S fold + the HV F/X/Y columns + the casingSmall(Osmiridium) 'M'. */
	@Test
	public void theMolecularScannerT3RowCarriesTheRestoredGrid() throws Exception {
		JsonObject tRow = generated("molecular_scanner_t3");
		assertEquals("DXE", row(tRow, 0), "the :1551 row 1 verbatim");
		assertEquals("FMF", row(tRow, 1), "the :1551 row 2 verbatim");
		assertEquals("RYS", row(tRow, 2), "the :1551 row 3 verbatim");
		assertEquals("forge:gems/diamond", tagOf(tRow, 'D'), "IL.Processor_Crystal_Diamond → the gem-tag fold");
		assertEquals("forge:gems/emerald", tagOf(tRow, 'E'), "IL.Processor_Crystal_Emerald → the gem-tag fold");
		assertEquals("forge:gems/ruby", tagOf(tRow, 'R'), "IL.Processor_Crystal_Ruby → the gem-tag fold");
		assertEquals("forge:gems/sapphire", tagOf(tRow, 'S'), "IL.Processor_Crystal_Sapphire → the gem-tag fold");
		assertEquals("gt6:field_generator_hv", itemOf(tRow, 'F'), "IL.FIELD_GENERATORS[3] = the HV rung (VN[3])");
		assertEquals("gt6:signal_emitter_hv", itemOf(tRow, 'X'), "IL.EMITTERS[3] = the HV rung");
		assertEquals("gt6:sensor_hv", itemOf(tRow, 'Y'), "IL.SENSORS[3] = the HV rung");
		assertEquals("gt6:casing_small_osmiridium", itemOf(tRow, 'M'), "casingMachine(Osmiridium) → the casingSmall fold");
		assertEquals("gt6:molecular_scanner_t3", tRow.getAsJsonObject("result").get("item").getAsString(), "the machine result path");
	}

	/** The seam restore count pin: exactly one scanner row file (the T1/T2/T4/T5 rungs stay commented out upstream, :1549-1553). */
	@Test
	public void theScannerRestoreIsTheSingleT3Rung() throws Exception {
		assertNotNull(generated("molecular_scanner_t3"), "the restored rung");
	}

	// ------------------------------------------------- the USB Stick rows

	/** The tier ladders (the :796-799 columns). */
	private static final String[] WIRE_COLS = {"wire_gold_gt01", "wire_aluminium_gt01", "wire_platinum_gt01", "wire_graphene_gt01"};
	private static final String[] MAT_SLUGS = {"aluminium", "stainless_steel", "chromium", "titanium"};

	/** The four stick rows carry the :796-799 grid verbatim: "xWd","PCP","TCT" with the circuit3-6 tags, the WIRES_01[3..6] ladder and the plate/screw ladder. */
	@Test
	public void theFourStickRowsCarryTheUpstreamGrids() throws Exception {
		for (int i = 0; i < 4; i++) {
			JsonObject tRow = generated("usb_stick_" + (i + 1));
			assertEquals("xWd", row(tRow, 0), "stick tier " + (i + 1) + ": the :796 row 1");
			assertEquals("PCP", row(tRow, 1), "stick tier " + (i + 1) + ": row 2");
			assertEquals("TCT", row(tRow, 2), "stick tier " + (i + 1) + ": row 3");
			assertEquals("gt6:tools/wire_cutter", key(tRow, 'x').get("tag").getAsString(), "the 'x' wirecutter letter");
			assertEquals("gt6:tools/screwdriver", key(tRow, 'd').get("tag").getAsString(), "the 'd' screwdriver letter");
			assertEquals("gt6:" + WIRE_COLS[i], itemOf(tRow, 'W'), "the WIRES_01[3+i] column (MT.java:3595-3610)");
			assertEquals("gt6:circuit" + (i + 3), tagOf(tRow, 'C'), "the OD_CIRCUITS[3+i] column");
			assertEquals("gt6:plate_" + MAT_SLUGS[i], itemOf(tRow, 'P'), "the plate ladder");
			assertEquals("gt6:screw_" + MAT_SLUGS[i], itemOf(tRow, 'T'), "the screw ladder");
			assertEquals("gt6:usb_stick_" + (i + 1), tRow.getAsJsonObject("result").get("item").getAsString(), "the result path");
		}
	}
}
