package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

/**
 * The tank-valve tool-letter parity pin (task tank-valve-char-misdecode): the 25
 * Tank Main Valve rows (:1195-1222) key only 'M'/'R' (wood/small) or 'M'/'P' (large)
 * upstream — every lowercase letter rides the CR.java table, 'h' = hammer (:346),
 * 'r' = softhammer (:355), 's' = saw (:356). The w3-tank-valves landing had
 * materialized the wood 'r' as a second lead ring (an extra ring, a missing soft
 * hammer vs upstream); every generated tank_valve/*.json now keys the tool letters
 * off the gt6 tool tags, pinned here so a future table drift fails instead of
 * shipping.
 */
public class GT6TankValveCharParityTest {

	/** The CR.java letter → the gt6 tool tag (the :346/:355/:356 switch arms). */
	private static final String[][] TOOL_LETTERS = {
			{"h", "gt6:tools/hard_hammer" , ":346"},
			{"r", "gt6:tools/soft_hammer" , ":355"},
			{"s", "gt6:tools/saw"         , ":356"},
	};

	private static String slug(String aPath, String aDensePrefix, String aPlainPrefix) {
		return aPath.startsWith(aDensePrefix) ? aPath.substring(aDensePrefix.length()) : aPath.substring(aPlainPrefix.length());
	}

	@BeforeAll
	static void bootRegistries() {
		// the GTMultiBlocksOfflineTestBase posture — GT6Tanks clinit touches the
		// registry keys, an unbootstrapped touch poisons the shared JVM's classes
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
	}

	@Test
	public void theWoodValveKeysTheCrTable() throws Exception {
		for (gregtech6.registry.GT6Tanks.TankValveRow tRow : gregtech6.registry.GT6Tanks.ROWS) {
			if (!tRow.flammable()) continue;
			JsonObject tJson = generated(tRow.path());
			assertEquals(" R ", tJson.getAsJsonArray("pattern").get(0).getAsString(), tRow.path() + " :1195 row 1");
			assertEquals("rMs", tJson.getAsJsonArray("pattern").get(1).getAsString(), tRow.path() + " :1195 row 2");
			assertEquals(" R ", tJson.getAsJsonArray("pattern").get(2).getAsString(), tRow.path() + " :1195 row 3");
			JsonObject tKey = tJson.getAsJsonObject("key");
			assertEquals(4, tKey.size(), tRow.path() + ": the wood key ships exactly M/R/r/s");
			assertEquals("gt6:ring_lead", itemOf(tKey, "R"), tRow.path() + ": 'R' = the lead ring (upstream OP.ring.dat(MT.Pb))");
			assertEquals("gt6:" + tRow.wallPath(), itemOf(tKey, "M"), tRow.path() + ": 'M' = the wood wall");
			assertToolLetter(tRow.path(), tKey, 'r');
			assertToolLetter(tRow.path(), tKey, 's');
			assertEquals("gt6:" + tRow.path(), tJson.getAsJsonObject("result").get("item").getAsString(), tRow.path() + ": the result");
		}
	}

	@Test
	public void theSmallValveToolLettersKeyTheCrTable() throws Exception {
		int tCount = 0;
		for (gregtech6.registry.GT6Tanks.TankValveRow tRow : gregtech6.registry.GT6Tanks.ROWS) {
			if (tRow.flammable() || tRow.size() != 3) continue;
			tCount++;
			JsonObject tJson = generated(tRow.path());
			assertEquals(" R ", tJson.getAsJsonArray("pattern").get(0).getAsString(), tRow.path() + " :1196-1208 row 1");
			assertEquals("hMs", tJson.getAsJsonArray("pattern").get(1).getAsString(), tRow.path() + " :1196-1208 row 2");
			assertEquals(" R ", tJson.getAsJsonArray("pattern").get(2).getAsString(), tRow.path() + " :1196-1208 row 3");
			JsonObject tKey = tJson.getAsJsonObject("key");
			assertEquals(4, tKey.size(), tRow.path() + ": the small key ships exactly M/R/h/s");
			assertEquals("gt6:ring_" + slug(tRow.path(), "tank_small_dense_", "tank_small_"), itemOf(tKey, "R"), tRow.path() + ": 'R' = the row-material ring");
			assertEquals("gt6:" + tRow.wallPath(), itemOf(tKey, "M"), tRow.path() + ": 'M' = the row's wall");
			assertToolLetter(tRow.path(), tKey, 'h');
			assertToolLetter(tRow.path(), tKey, 's');
			assertEquals("gt6:" + tRow.path(), tJson.getAsJsonObject("result").get("item").getAsString(), tRow.path() + ": the result");
		}
		assertEquals(12, tCount, "the :1196-1208 small ladder");
	}

	@Test
	public void theLargeValveToolLettersKeyTheCrTable() throws Exception {
		int tCount = 0;
		for (gregtech6.registry.GT6Tanks.TankValveRow tRow : gregtech6.registry.GT6Tanks.ROWS) {
			if (tRow.size() != 5) continue;
			tCount++;
			boolean tDense = tRow.wallPath().startsWith("dense_wall_");
			String tSlug = slug(tRow.path(), "tank_large_dense_", "tank_large_");
			JsonObject tJson = generated(tRow.path());
			assertEquals("PPP", tJson.getAsJsonArray("pattern").get(0).getAsString(), tRow.path() + " :1210-1222 row 1");
			assertEquals("hMs", tJson.getAsJsonArray("pattern").get(1).getAsString(), tRow.path() + " :1210-1222 row 2");
			assertEquals("PPP", tJson.getAsJsonArray("pattern").get(2).getAsString(), tRow.path() + " :1210-1222 row 3");
			JsonObject tKey = tJson.getAsJsonObject("key");
			assertEquals(4, tKey.size(), tRow.path() + ": the large key ships exactly M/P/h/s");
			assertEquals("gt6:" + (tDense ? "plate_dense_" : "plate_") + tSlug, itemOf(tKey, "P"), tRow.path() + ": 'P' = the row plate (plateDense on the dense larges)");
			assertEquals("gt6:" + (tDense ? "tank_small_dense_" : "tank_small_") + tSlug, itemOf(tKey, "M"), tRow.path() + ": 'M' = the SMALL valve");
			assertToolLetter(tRow.path(), tKey, 'h');
			assertToolLetter(tRow.path(), tKey, 's');
			assertEquals("gt6:" + tRow.path(), tJson.getAsJsonObject("result").get("item").getAsString(), tRow.path() + ": the result");
		}
		assertEquals(12, tCount, "the :1210-1222 large ladder");
	}

	/** The datagen walk covers the whole 25-row family (a dropped row fails here, not silently). */
	@Test
	public void theValveCensusIsTheTwentyFiveRows() {
		assertEquals(25, gregtech6.registry.GT6Tanks.ROWS.size(), "the :1195-1222 family fold");
	}

	/** One tool letter: keys the CR.java table tag, never a bare item (a re-fold fails with the arm line). */
	private static void assertToolLetter(String aPath, JsonObject aKey, char aLetter) {
		JsonObject tEntry = aKey.getAsJsonObject(String.valueOf(aLetter));
		String[] tLetter = TOOL_LETTERS['h' == aLetter ? 0 : 'r' == aLetter ? 1 : 2];
		assertNotNull(tEntry, aPath + ": the '" + aLetter + "' key ships");
		assertEquals(tLetter[1], tEntry.get("tag").getAsString(), aPath + ": '" + aLetter + "' = " + tLetter[1] + " (CR.java" + tLetter[2] + ")");
	}

	private static String itemOf(JsonObject aKey, String aLetter) {
		return aKey.getAsJsonObject(aLetter).get("item").getAsString();
	}

	private static JsonObject generated(String aPath) throws Exception {
		String tResource = "data/gt6/recipes/tank_valve/" + aPath + ".json";
		byte[] tBytes = resourceOrNull(tResource);
		assertNotNull(tBytes, tResource + " rides the generated-resources classpath");
		return JsonParser.parseString(new String(tBytes, StandardCharsets.UTF_8)).getAsJsonObject();
	}

	private static byte[] resourceOrNull(String aPath) throws Exception {
		try (InputStream tStream = GT6TankValveCharParityTest.class.getClassLoader().getResourceAsStream(aPath)) {
			return tStream == null ? null : tStream.readAllBytes();
		}
	}
}
