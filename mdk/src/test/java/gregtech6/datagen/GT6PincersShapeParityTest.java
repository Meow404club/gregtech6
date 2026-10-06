package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregapi.data.OP;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregtech6.registry.GTMaterialItems;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The pincers shape parity pin (task pincers-bottom-row-shape): the upstream row is
 * {@code {"XhX"," T ","SdS"}} (Loader_Tools.java:316, the :455-473 explicit alphabet +
 * the CR tool letters) — the w5-era steel anchor had re-arranged it to
 * {@code "XhX"/" d "/"S S"}, dropping the bottom-row 'd' screwdriver tool (CR.java:342)
 * and reusing the letter for the screw ('T' = screw, Loader_Tools.java:463). Both faces
 * are pinned here VERBATIM so a future shape drift fails instead of shipping:
 *
 * <ul>
 * <li>the Steel anchor {@code gt6:recipes/pincers.json} (pincersBuilder, the
 * GT6StorageToolCharParityTest precedent form);</li>
 * <li>the machine-ladder family rows ({@link GT6CraftingRecipes#MACHINE_LADDER_FORMS}
 * "pincers" :5359 — already verbatim, pinned so the two faces cannot drift apart
 * again).</li>
 * </ul>
 */
public class GT6PincersShapeParityTest extends GTOfflineTestBase {

	/** The upstream :316 shape, both faces. */
	private static final List<String> UPSTREAM_SHAPE = List.of("XhX", " T ", "SdS");

	@BeforeAll
	static void boot() {
		GTMaterialItems.initMaterials();
	}

	private static JsonObject generated(String aPath) throws Exception {
		String tPath = "/data/gt6/" + aPath + ".json";
		try (InputStream tStream = GT6PincersShapeParityTest.class.getResourceAsStream(tPath)) {
			assertNotNull(tStream, tPath + " rides the generated-resources classpath");
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	private static JsonElement key(JsonObject aRow, char aChar) {
		return aRow.getAsJsonObject("key").get(String.valueOf(aChar));
	}

	private static void assertShape(JsonObject aRow, String aFace) {
		List<String> tPattern = aRow.getAsJsonArray("pattern").asList().stream().map(JsonElement::getAsString).toList();
		assertEquals(UPSTREAM_SHAPE, tPattern, aFace + ": the Loader_Tools.java:316 shape verbatim");
	}

	/** The shared three tool/prefix keys: 'h'/'d' ride the CR tags, 'T' the row material's screw (Loader_Tools.java:463). */
	private static void assertToolKeys(JsonObject aRow, String aSnake, String aFace) {
		assertEquals("gt6:tools/hard_hammer", key(aRow, 'h').getAsJsonObject().get("tag").getAsString(),
				aFace + ": 'h' = the hammer tool letter (CR.java:346)");
		assertEquals("gt6:tools/screwdriver", key(aRow, 'd').getAsJsonObject().get("tag").getAsString(),
				aFace + ": 'd' = the screwdriver tool letter (CR.java:342) — the restored bottom row");
		assertEquals("gt6:screw_" + aSnake, key(aRow, 'T').getAsJsonObject().get("item").getAsString(),
				aFace + ": 'T' = the row material's screw (Loader_Tools.java:463)");
	}

	/** The steel anchor (pincersBuilder) carries the upstream shape verbatim. */
	@Test
	public void theSteelAnchorCarriesTheUpstreamShape() throws Exception {
		JsonObject tRow = generated("recipes/pincers");
		assertEquals("minecraft:crafting_shaped", tRow.get("type").getAsString(), "the anchor rides the vanilla serializer");
		assertShape(tRow, "steel anchor");
		assertToolKeys(tRow, "steel", "steel anchor");
		assertEquals("gt6:plate_curved_steel", key(tRow, 'X').getAsJsonObject().get("item").getAsString(),
				"steel anchor: 'X' = the steel curved plate (the :470 plateCurved letter)");
		assertEquals("minecraft:stick", key(tRow, 'S').getAsJsonObject().get("item").getAsString(),
				"steel anchor: 'S' = the vanilla stick (the w5 seat)");
		assertEquals("gt6:pincers", tRow.getAsJsonObject("result").get("item").getAsString(), "the anchor output");
	}

	/**
	 * The ladder family rows (the per-material gt6:material_tool face) ride the SAME
	 * shape and keys — the walk mirrors machineLadderRows (alias merge, the Steel
	 * exclusion, machineLadderAxis, the X/T/S letter item truth); the count is the
	 * measured dead number. Steel itself is NEGATIVE: its tool face is the anchor's.
	 */
	@Test
	public void theLadderFamilyRowsCarryTheUpstreamShape() throws Exception {
		GT6CraftingRecipes.MachineLadderForm tPincers = null;
		for (GT6CraftingRecipes.MachineLadderForm tForm : GT6CraftingRecipes.MACHINE_LADDER_FORMS) {
			if (tForm.aId().equals("pincers")) tPincers = tForm;
		}
		assertNotNull(tPincers, "the :5359 form ships");
		assertFalse(tPincers.aIncludeSteel(), "the steel anchors own the tool face (the :5326 ruling)");

		List<String> tSnakes = new ArrayList<>();
		Set<String> tSeen = new HashSet<>();
		for (OreDictMaterial tMaterial : MaterialRegistry.INSTANCE.MATERIAL_ARRAY) {
			if (tMaterial == null || tMaterial.mID < 0) continue;
			tMaterial = MaterialRegistry.INSTANCE.get(tMaterial); // the alias merge
			if (tMaterial == null || tMaterial.mID < 0 || !tSeen.add(tMaterial.mNameInternal)) continue;
			if (tMaterial == gregapi.data.MT.Steel) continue; // the :5765 exclusion
			if (!GT6CraftingRecipes.machineLadderAxis(tMaterial, tPincers)) continue;
			boolean tResolvable = OP.plateCurved.isGeneratingItem(tMaterial) // 'X'
					&& OP.screw.isGeneratingItem(tMaterial) // 'T'
					&& OP.stick.isGeneratingItem(tMaterial); // 'S' (h/d = the tool tags, no material truth)
			if (!tResolvable) continue;
			String tSnake = GTMaterialItems.snakeCase(tMaterial.mNameInternal);
			JsonObject tRow;
			try {
				tRow = generated("recipes/pincers/" + tSnake);
			} catch (AssertionError tMiss) {
				throw new AssertionError("pincers/" + tSnake + ": the walk says a row ships, the tree disagrees", tMiss);
			}
			assertEquals("gt6:material_tool", tRow.get("type").getAsString(), tSnake + ": the stamped serializer");
			assertEquals(tSnake, tRow.get("material").getAsString(), tSnake + ": the stamp material field");
			assertEquals("gt6:pincers", tRow.getAsJsonObject("result").get("item").getAsString(), tSnake + ": the tool output");
			assertShape(tRow, tSnake);
			assertToolKeys(tRow, tSnake, tSnake);
			assertEquals("gt6:plate_curved_" + tSnake, key(tRow, 'X').getAsJsonObject().get("item").getAsString(),
					tSnake + ": 'X' = the row material's curved plate");
			assertEquals("gt6:stick_" + tSnake, key(tRow, 'S').getAsJsonObject().get("item").getAsString(),
					tSnake + ": 'S' = the row material's stick");
			tSnakes.add(tSnake);
		}
		assertEquals(202, tSnakes.size(), "the measured pincers ladder universe (X/T/S item truth ∩ the :316 axis)");
		assertNull(generatedOrNull("recipes/pincers/steel"), "no ghost steel ladder row — the anchor owns Steel");
	}

	private static JsonObject generatedOrNull(String aPath) throws Exception {
		String tPath = "/data/gt6/" + aPath + ".json";
		try (InputStream tStream = GT6PincersShapeParityTest.class.getResourceAsStream(tPath)) {
			assertTrue(tStream == null, tPath + " must NOT ship (the :5765 steel exclusion)");
			return null;
		}
	}
}
