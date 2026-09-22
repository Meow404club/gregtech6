package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.registry.GT6BeeCombs;
import gregtech6.registry.GTMaterialItems;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The Bumbliary pair crafting-row json test (task p34-bumbliary-recipes acceptance ① —
 * the "两配方 json 资源钉测（形状/键/结果逐参）" face): the TWO generated
 * data/gt6/recipes/ files are read verbatim off the classpath and asserted on their
 * IDENTITY faces against the upstream registration-line varargs
 * (Loader_MultiTileEntities.java:2222 {@code "PPP","PBP","TdT"} / :2223
 * {@code "PRP","HBH","PCP"}). The json files ride the test classpath through the
 * generated-resources source root (the GT6DieselCraftingJsonTest convention); the tag
 * faces pin the forge-leg plural tree (the canonical 1.20.1-forge producer, the
 * GT6DualDirectoryFacesTest read form).
 */
public class GT6BumbliaryCraftingJsonTest extends GTOfflineTestBase {

	@BeforeAll
	static void boot() {
		GTMaterialItems.initMaterials();
	}

	private static JsonObject generated(String aPath) throws Exception {
		String tPath = "/data/gt6/recipes/" + aPath + ".json";
		try (InputStream tStream = GT6BumbliaryCraftingJsonTest.class.getResourceAsStream(tPath)) {
			assertNotNull(tStream, tPath + " rides the generated-resources classpath");
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	private static JsonElement key(JsonObject aRow, char aChar) {
		return aRow.getAsJsonObject("key").get(String.valueOf(aChar));
	}

	/** Acceptance ① row one: the :2222 Bumbliary varargs. */
	@Test
	public void theBumbliaryRowCarriesTheUpstreamGrid() throws Exception {
		JsonObject tRow = generated("bumbliary");
		JsonArray tPattern = tRow.getAsJsonArray("pattern");
		assertEquals(3, tPattern.size(), "a 3x3 grid");
		assertEquals("PPP", tPattern.get(0).getAsString(), "the :2222 row 1");
		assertEquals("PBP", tPattern.get(1).getAsString(), "row 2");
		assertEquals("TdT", tPattern.get(2).getAsString(), "row 3");
		assertTrue(key(tRow, 'P').getAsJsonObject().has("tag")
				&& key(tRow, 'P').getAsJsonObject().get("tag").getAsString().endsWith("plates/wood_treated"),
				"'P' = OP.plate.dat(MT.WoodTreated) → the plate material tag");
		assertEquals("gt6:bumble_hive", key(tRow, 'B').getAsJsonObject().get("item").getAsString(),
				"'B' = aRegistry.getItem(32755) → the R2 hive BlockItem");
		assertEquals("gt6:screw_iron", key(tRow, 'T').getAsJsonObject().get("item").getAsString(),
				"'T' = OP.screw.dat(ANY.Iron) → the iron-member fold (the scissors precedent)");
		assertTrue(key(tRow, 'd').getAsJsonObject().has("tag")
				&& key(tRow, 'd').getAsJsonObject().get("tag").getAsString().endsWith("tools/screwdriver"),
				"'d' = the screwdriver tool letter (the drawer-row shape)");
		assertEquals("gt6:bumbliary", tRow.getAsJsonObject("result").get("item").getAsString(),
				"the result = the 32741 port block item");
	}

	/** Acceptance ① row two: the :2223 Advanced Bumbliary varargs. */
	@Test
	public void theAdvancedRowCarriesTheUpstreamGrid() throws Exception {
		JsonObject tRow = generated("bumbliary_advanced");
		JsonArray tPattern = tRow.getAsJsonArray("pattern");
		assertEquals(3, tPattern.size(), "a 3x3 grid");
		assertEquals("PRP", tPattern.get(0).getAsString(), "the :2223 row 1");
		assertEquals("HBH", tPattern.get(1).getAsString(), "row 2");
		assertEquals("PCP", tPattern.get(2).getAsString(), "row 3");
		assertTrue(key(tRow, 'P').getAsJsonObject().has("tag")
				&& key(tRow, 'P').getAsJsonObject().get("tag").getAsString().endsWith("plates/stainless_steel"),
				"'P' = OP.plate.dat(MT.StainlessSteel) → the plate material tag");
		assertEquals("gt6:bumbliary", key(tRow, 'B').getAsJsonObject().get("item").getAsString(),
				"'B' = aRegistry.getItem(32741) → the Bumbliary BlockItem");
		assertTrue(key(tRow, 'C').getAsJsonObject().has("tag")
				&& key(tRow, 'C').getAsJsonObject().get("tag").getAsString().endsWith("chests"),
				"'C' = OD.craftingChest → the platform chest tag (the LOCKER precedent)");
		assertTrue(key(tRow, 'R').getAsJsonObject().has("tag")
				&& key(tRow, 'R').getAsJsonObject().get("tag").getAsString().endsWith("combs/crossbred"),
				"'R' = OD.beeCombCrossbred → the crossbred-comb tag");
		assertEquals("minecraft:honey_bottle", key(tRow, 'H').getAsJsonObject().get("item").getAsString(),
				"'H' = OD.container1000honey → the vanilla-native container fold (the oLantern torch precedent)");
		assertEquals("gt6:bumbliary_advanced", tRow.getAsJsonObject("result").get("item").getAsString(),
				"the result = the 32007 port block item");
	}

	/** The 'R' tag face carries exactly the ten upstream OD.beeCombCrossbred combs (:237-246). */
	@Test
	public void theCrossbredCombTagIsTheUpstreamTen() throws Exception {
		String tPath = "/data/gt6/tags/items/combs/crossbred.json";
		try (InputStream tStream = GT6BumbliaryCraftingJsonTest.class.getResourceAsStream(tPath)) {
			assertNotNull(tStream, tPath + " rides the generated-resources classpath");
			JsonObject tTag = JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
			List<String> tMembers = tTag.getAsJsonArray("values").asList().stream().map(JsonElement::getAsString).toList();
			assertEquals(10, tMembers.size(), "the ten :237-246 tagged combs");
			for (GT6BeeCombs.CombSpec tSpec : GT6BeeCombs.COMB_SPECS) {
				assertTrue(tSpec.meta() < 30100 || tMembers.contains("gt6:" + tSpec.itemId()),
						tSpec.itemId() + " rides the tag (the meta >= 30100 walk)");
			}
			assertEquals("gt6:comb_clay", tMembers.get(0), "the declaration-order walk anchors the head");
			assertEquals("gt6:comb_tera", tMembers.get(9), "the tail is the Tera comb");
		}
	}
}
