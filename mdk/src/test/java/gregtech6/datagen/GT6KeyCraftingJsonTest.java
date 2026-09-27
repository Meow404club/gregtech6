package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The dungeon-key crafting pins (task debt-dungeon-keys-recipes — the P38 defer closure,
 * the r-dungeon-key-trace research): the ten rows of MultiItemRandomTools.java:589-598
 * ship verbatim — {@code CR.shaped(Key.get(3), CR.DEF_NCC, "fPx", 'P', OP.plate.dat(mat))}
 * per material (iron/gold/copper/tin/bronze/brass/silver/platinum/lead/plastic, ids
 * 30000-30009) = 1 plate → 3 keys, the single-row grid [file | plate | wire cutter].
 * The JSON-ship face only (the offline JVM holds no registry — the GT6StoneStairsWall
 * lesson): row count, row shape, quantity and the three key columns.
 */
public class GT6KeyCraftingJsonTest {

	/** The ten rows in upstream registration order (:589-:598) — the output path suffix = the plate tag snake. */
	private static final String[] MATERIALS = {
			"iron", "gold", "copper", "tin", "bronze", "brass", "silver", "platinum", "lead", "plastic"};

	@Test
	public void theTenUpstreamRowsShip() throws Exception {
		assertEquals(10, MATERIALS.length, "the :589-:598 census — one key per material, ids 30000-30009");
		for (String tMat : MATERIALS) {
			assertNotNull(generated("key_" + tMat), "data/gt6/recipes/key_" + tMat + ".json rides the generated-resources classpath");
		}
	}

	@Test
	public void theGridsAreTheUpstreamFpxFace() throws Exception {
		for (String tMat : MATERIALS) {
			JsonObject tJson = generated("key_" + tMat);
			// the row shape — "fPx", the single-row grid [file | plate | wire cutter]
			assertEquals(1, tJson.getAsJsonArray("pattern").size(), "key_" + tMat + ": the single-row grid");
			assertEquals("fPx", tJson.getAsJsonArray("pattern").get(0).getAsString(), "key_" + tMat + ": the :589-:598 row verbatim");
			// the quantity — Key.get(3): 1 plate → 3 keys
			JsonObject tResult = tJson.getAsJsonObject("result");
			assertEquals("gt6:key_" + tMat, tResult.get("item").getAsString(), "key_" + tMat + ": the result is the material key");
			assertEquals(3, tResult.get("count").getAsInt(), "key_" + tMat + ": the .get(3) quantity");
			// the three key columns — 'f' file tag, 'P' the material plate family tag, 'x' wire-cutter tag
			JsonObject tKey = tJson.getAsJsonObject("key");
			assertEquals("gt6:tools/file", tKey.getAsJsonObject("f").get("tag").getAsString(), "key_" + tMat + ": the 'f' FILE key (CR.java:200)");
			assertTrue(tKey.getAsJsonObject("P").get("tag").getAsString().endsWith("plates/" + tMat),
					"key_" + tMat + ": the 'P' column rides the material plate family tag");
			assertEquals("gt6:tools/wire_cutter", tKey.getAsJsonObject("x").get("tag").getAsString(), "key_" + tMat + ": the 'x' WIRE_CUTTER key (CR.java:214)");
			assertEquals(3, tKey.size(), "key_" + tMat + ": exactly the f/P/x columns");
		}
	}

	private static JsonObject generated(String aId) throws Exception {
		byte[] tBytes = resourceOrNull("data/gt6/recipes/" + aId + ".json");
		if (tBytes == null) return null;
		return JsonParser.parseString(new String(tBytes, StandardCharsets.UTF_8)).getAsJsonObject();
	}

	private static byte[] resourceOrNull(String aPath) throws Exception {
		try (InputStream tStream = GT6KeyCraftingJsonTest.class.getClassLoader().getResourceAsStream(aPath)) {
			return tStream == null ? null : tStream.readAllBytes();
		}
	}
}
