/**
 * The Grindstone crafting-row json pin (task grindstone-family, the
 * GT6BumbliaryCraftingJsonTest form): the generated data/gt6/recipes/grindstone.json is
 * read verbatim off the classpath and asserted on its IDENTITY faces against the upstream
 * registration-line varargs (Loader_MultiTileEntities.java:2226
 * {@code "SAS","SwS","PPP"}, 'S' = stickLong ANY.Iron, 'A' = stick ANY.Iron, 'P' =
 * plateDouble ANY.Iron, 'w' = the wrench tool letter). The json rides the test classpath
 * through the generated-resources source root.
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.registry.GT6Grindstones;
import gregtech6.tileentity.GTOfflineTestBase;

public class GT6GrindstoneCraftingJsonTest extends GTOfflineTestBase {

	private static JsonObject generated(String aPath) throws Exception {
		String tPath = "/data/gt6/recipes/" + aPath + ".json";
		try (InputStream tStream = GT6GrindstoneCraftingJsonTest.class.getResourceAsStream(tPath)) {
			assertNotNull(tStream, tPath + " rides the generated-resources classpath");
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	private static JsonElement key(JsonObject aRow, char aChar) {
		return aRow.getAsJsonObject("key").get(String.valueOf(aChar));
	}

	/** The :2226 row — the "SAS"/"SwS"/"PPP" grid over the iron carriers + the wrench mark. */
	@Test
	public void theGrindstoneRowCarriesTheUpstreamGrid() throws Exception {
		JsonObject tRow = generated("grindstone");
		assertEquals("minecraft:crafting_shaped", tRow.get("type").getAsString());
		JsonArray tPattern = tRow.getAsJsonArray("pattern");
		assertEquals(3, tPattern.size(), "a 3x3 grid");
		assertEquals("SAS", tPattern.get(0).getAsString(), "the :2226 row 1");
		assertEquals("SwS", tPattern.get(1).getAsString(), "row 2 — the wrench mark centre");
		assertEquals("PPP", tPattern.get(2).getAsString(), "row 3 — the plate-double base");
		assertEquals("gt6:stick_long_iron", key(tRow, 'S').getAsJsonObject().get("item").getAsString(),
				"'S' = OP.stickLong.dat(ANY.Iron) → the iron-member fold (the bumbliary screw_iron precedent)");
		assertEquals("gt6:stick_iron", key(tRow, 'A').getAsJsonObject().get("item").getAsString(),
				"'A' = OP.stick.dat(ANY.Iron) → the same fold");
		assertTrue(key(tRow, 'P').getAsJsonObject().has("tag")
				&& key(tRow, 'P').getAsJsonObject().get("tag").getAsString().endsWith("double_plates/iron"),
				"'P' = OP.plateDouble.dat(ANY.Iron) → the double-plate material tag (the transformer-row form)");
		assertTrue(key(tRow, 'w').getAsJsonObject().has("tag")
				&& key(tRow, 'w').getAsJsonObject().get("tag").getAsString().endsWith("tools/wrench"),
				"'w' = the wrench tool letter (not consumed)");
		assertEquals("gt6:grindstone", tRow.getAsJsonObject("result").get("item").getAsString(),
				"the result = the 32703 port block item");
		assertTrue(!tRow.getAsJsonObject("result").has("count"),
				"count 1 rides the vanilla omission convention (the stairs-wall pin form)");
	}

	/** The registration face: block + BET + the item ride their ids (the getId() read, no .get()). */
	@Test
	public void theRegistrationFaceCarriesTheIds() {
		assertEquals("grindstone", GT6Grindstones.GRINDSTONE.getId().getPath(), "the block id");
		assertEquals("grindstone", GT6Grindstones.GRINDSTONE_BE.getId().getPath(), "the BET id");
		assertEquals("grindstone", GT6Grindstones.GRINDSTONE_ITEM.getId().getPath(), "the BlockItem id");
	}

	/** The lang faces: the en VERBATIM row name + the zh dump row (打磨石, meta 32703). */
	@Test
	public void theLangFacesCarryTheUpstreamNames() throws Exception {
		JsonObject tEn = generatedJsonAsset("/assets/gt6/lang/en_us.json");
		assertEquals("Grindstone", tEn.get("block.gt6.grindstone").getAsString(), "the :2226 en name");
		JsonObject tZh = generatedJsonAsset("/assets/gt6/lang/zh_cn.json");
		assertEquals("打磨石", tZh.get("block.gt6.grindstone").getAsString(), "the zh dump row (tmp/gregtech.lang 32703)");
	}

	private static JsonObject generatedJsonAsset(String aPath) throws Exception {
		try (InputStream tStream = GT6GrindstoneCraftingJsonTest.class.getResourceAsStream(aPath)) {
			assertNotNull(tStream, aPath + " rides the generated-resources classpath");
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}
}
