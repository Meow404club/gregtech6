package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.registry.GT6Kinetics;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The Diesel Engine crafting-row json test (task p29-w4-hot-lube acceptance ④ — the
 * "lubricant 行+item crafting 位" face): the EIGHT generated data/gt6/recipes/
 * diesel_engine_*.json files (the Loader_MultiTileEntities.java:722-729 grids) are read
 * verbatim off the classpath and asserted on their IDENTITY faces — the "PLP"/"SMS"/"GPC"
 * pattern, the 'L' key = {@code gt6:lubricant_bucket} (the OD.itemLubricant port carrier),
 * the five per-material keys, and the result = the engine block item. The json files ride
 * the test classpath through the generated-resources source root (the GTGrassBlockTest
 * {@code generated()} convention).
 */
public class GT6DieselCraftingJsonTest extends GTOfflineTestBase {

	/** The material slugs in DIESEL_SPECS order (GT6Kinetics.DIESEL_SPECS). */
	private static List<String> sSlugs;

	@BeforeAll
	static void walkSpecs() {
		sSlugs = GT6Kinetics.DIESEL_SPECS.stream().map(GT6Kinetics.DieselSpec::material).toList();
		assertEquals(8, sSlugs.size(), "the eight :722-729 rows");
	}

	private static JsonObject generated(String aPath) throws Exception {
		String tPath = "/data/gt6/recipes/" + aPath + ".json";
		try (InputStream tStream = GT6DieselCraftingJsonTest.class.getResourceAsStream(tPath)) {
			assertNotNull(tStream, tPath + " rides the generated-resources classpath");
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	/** Acceptance ④: every row carries the :722-729 shape and the 'L' = lubricant_bucket face. */
	@Test
	public void theEightDieselRowsCarryTheGridAndTheLubricantSlot() throws Exception {
		for (String tSlug : sSlugs) {
			JsonObject tRow = generated(GT6Kinetics.dieselName(tSlug));
			JsonArray tPattern = tRow.getAsJsonArray("pattern");
			assertEquals(3, tPattern.size(), tSlug + ": a 3x3 grid");
			assertEquals("PLP", tPattern.get(0).getAsString(), tSlug + ": the :722-729 row 1");
			assertEquals("SMS", tPattern.get(1).getAsString(), tSlug + ": row 2");
			assertEquals("GPC", tPattern.get(2).getAsString(), tSlug + ": row 3");
			JsonObject tKey = tRow.getAsJsonObject("key");
			assertEquals("gt6:lubricant_bucket", tKey.getAsJsonObject("L").get("item").getAsString(),
					tSlug + ": the 'L' slot = OD.itemLubricant → the port single-item carrier");
			// the per-material id faces (the GTMaterialItems composition: prefix + "_" + material snake)
			assertEquals("gt6:casing_small_" + tSlug, tKey.getAsJsonObject("M").get("item").getAsString(), tSlug + ": the casingMachineDouble fold");
			assertEquals("gt6:plate_curved_" + tSlug, tKey.getAsJsonObject("P").get("item").getAsString(), tSlug + ": plateCurved");
			assertEquals("gt6:stick_" + tSlug, tKey.getAsJsonObject("S").get("item").getAsString(), tSlug + ": stick");
			assertEquals("gt6:gear_gt_" + tSlug, tKey.getAsJsonObject("G").get("item").getAsString(), tSlug + ": gearGt");
			assertEquals("gt6:gear_gt_small_" + tSlug, tKey.getAsJsonObject("C").get("item").getAsString(), tSlug + ": gearGtSmall");
			JsonObject tResult = tRow.getAsJsonObject("result");
			assertEquals("gt6:" + GT6Kinetics.dieselName(tSlug), tResult.get("item").getAsString(), tSlug + ": the result path");
		}
	}

	/** The eight row ids match the DIESEL_SPECS slug ladder (the result-path convention). Note: DIESEL_ITEMS itself fills at runtime registration — offline the map is empty, so the result face rides the json files above. */
	@Test
	public void theSlugLadderCoversEveryUpstreamRow() {
		assertEquals(List.of("bronze", "arsenic_copper", "arsenic_bronze", "steel", "invar", "titanium", "tungstensteel", "iridium"), sSlugs,
				"the :721-729 declaration order");
		assertEquals(8, GT6Kinetics.DIESEL_SPECS.size(), "the eight rows (the live item map fills at registry time)");
	}
}
