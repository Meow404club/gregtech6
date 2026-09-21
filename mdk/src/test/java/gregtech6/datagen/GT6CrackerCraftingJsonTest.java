package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The two Cracker crafting-family json test (task p33-cracker-machines spec ③ — the
 * "cracker 机器 crafting pattern 实证" face): the EIGHT generated data/gt6/recipes/
 * {steamcracker,catalyticcracker}[_tN].json files (the Loader_MultiTileEntities.java
 * :1570-1579 grids verbatim) are read off the classpath and asserted on their identity
 * faces — the "IwI"/"PMP"/"ICI" SteamCracker pattern and the "IPI"/"ZMZ"/"ICI"
 * CatalyticCracker pattern per tier, the declared fold keys ('M' casingSmall, 'P'
 * wood_fluid_pipe_medium, 'Z' dust_zeolite, 'w' the wrench tag, the per-tier Invar/Cu
 * plate ladders) and the result = the machine block item.
 */
public class GT6CrackerCraftingJsonTest extends GTOfflineTestBase {

	private static final String[] T1_T4 = {"", "_t2", "_t3", "_t4"};
	private static final String[] PLATES = {"plate_double", "plate_triple", "plate_quadruple", "plate_quintuple"};
	private static final String[] CASINGS = {"steel", "invar", "titanium", "tungstensteel"};

	private static JsonObject generated(String aPath) throws Exception {
		String tPath = "/data/gt6/recipes/" + aPath + ".json";
		try (InputStream tStream = GT6CrackerCraftingJsonTest.class.getResourceAsStream(tPath)) {
			assertNotNull(tStream, tPath + " rides the generated-resources classpath");
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	/** Spec ③ acceptance: the eight rows carry the :1570-1579 patterns and the fold keys. */
	@Test
	public void theEightCrackerRowsCarryTheUpstreamGrids() throws Exception {
		for (int i = 0; i < 4; i++) {
			// the Steam Cracker :1576-1579 — "IwI","PMP","ICI"
			JsonObject tSteam = generated("steamcracker" + T1_T4[i]);
			JsonArray tSPattern = tSteam.getAsJsonArray("pattern");
			assertEquals(3, tSPattern.size(), "steam tier " + i + ": a 3x3 grid");
			assertEquals("IwI", tSPattern.get(0).getAsString(), "steam tier " + i + ": the :1576 row 1");
			assertEquals("PMP", tSPattern.get(1).getAsString(), "steam tier " + i + ": row 2");
			assertEquals("ICI", tSPattern.get(2).getAsString(), "steam tier " + i + ": row 3");
			JsonObject tSKey = tSteam.getAsJsonObject("key");
			assertEquals("gt6:" + PLATES[i] + "_invar", tSKey.getAsJsonObject("I").get("item").getAsString(),
					"steam tier " + i + ": the per-tier Invar plateD/T/Q/Quint");
			assertEquals("gt6:tools/wrench", tSKey.getAsJsonObject("w").get("tag").getAsString(), "steam tier " + i + ": the 'w' wrench tag");
			assertEquals("gt6:wood_fluid_pipe_medium", tSKey.getAsJsonObject("P").get("item").getAsString(),
					"steam tier " + i + ": the 'P' pipe fold (the declared deviation)");
			assertEquals("gt6:casing_small_" + CASINGS[i], tSKey.getAsJsonObject("M").get("item").getAsString(),
					"steam tier " + i + ": the casingMachineDouble fold");
			assertEquals("gt6:" + PLATES[i] + "_copper", tSKey.getAsJsonObject("C").get("item").getAsString(),
					"steam tier " + i + ": the ANY.Cu→Copper fold");
			assertEquals("gt6:steamcracker" + T1_T4[i], tSteam.getAsJsonObject("result").get("item").getAsString(),
					"steam tier " + i + ": the result path");
			// the Catalytic Cracker :1570-1573 — "IPI","ZMZ","ICI"
			JsonObject tCat = generated("catalyticcracker" + T1_T4[i]);
			JsonArray tCPattern = tCat.getAsJsonArray("pattern");
			assertEquals("IPI", tCPattern.get(0).getAsString(), "catalytic tier " + i + ": the :1570 row 1");
			assertEquals("ZMZ", tCPattern.get(1).getAsString(), "catalytic tier " + i + ": row 2");
			assertEquals("ICI", tCPattern.get(2).getAsString(), "catalytic tier " + i + ": row 3");
			JsonObject tCKey = tCat.getAsJsonObject("key");
			assertEquals("gt6:dust_zeolite", tCKey.getAsJsonObject("Z").get("item").getAsString(),
					"catalytic tier " + i + ": the 'Z' Zeolite dust");
			assertEquals("gt6:wood_fluid_pipe_medium", tCKey.getAsJsonObject("P").get("item").getAsString(),
					"catalytic tier " + i + ": the 'P' pipeQuadruple fold (the declared deviation)");
			assertEquals("gt6:catalyticcracker" + T1_T4[i], tCat.getAsJsonObject("result").get("item").getAsString(),
					"catalytic tier " + i + ": the result path");
		}
	}
}
