/**
 * Offline pin for task bush-growth-blockstate — the generated-tree faces of the wild
 * bush's blockstate form, remapped by berry-overlay: the 36-state grid (AGE_3 x KIND
 * nine) maps per AGE onto the three bush models (age0 = berry_bush, age1 = stage1,
 * age2|3 = stage2 — the stage colour rides the GT6BushTintListener arm, not the JSON),
 * and the configured feature is the weighted KIND nine at the upstream birth stage 3
 * VERBATIM (placeBushCore :86 NBT_STATE 3 — the 2026-10-01 coordinator ruling: the birth
 * stage follows the upstream; the growth/harvest mechanics alone ride the vanilla
 * homolog). The kind spread collapses the upstream NoiseGenerator patch index
 * (WorldgenBushes.java:66) to per-placement uniform — declared in GT6WorldgenDatagen.
 *
 * <p>Reads the committed generated tree on the classpath (the GT6BushTintDatagenTest
 * form — no datagen run).
 */
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

public class GT6BushBlockstateDatagenTest {

	private static final List<String> KINDS = List.of("blueberry", "candleberry", "cranberry",
			"currants_black", "currants_white", "currants_red", "blackberry", "raspberry", "cotton");

	private static JsonObject generatedJson(String aPath) throws Exception {
		try (InputStream tStream = GT6BushBlockstateDatagenTest.class.getClassLoader()
				.getResourceAsStream(aPath)) {
			assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8))
					.getAsJsonObject();
		}
	}

	/**
	 * The blockstate grid (task berry-overlay): every AGE x KIND state rides its per-AGE
	 * model — age 0 = berry_bush, age 1 = berry_bush_stage1, ages 2|3 share
	 * berry_bush_stage2 (their colours differ only in the GT6BushTintListener stage arm) —
	 * 36 variants collapsing to exactly 3 distinct model names, KIND-independent.
	 */
	@Test
	void theBlockstateEmitsTheFullThirtySixStateGrid() throws Exception {
		JsonObject tVariants = generatedJson("assets/gt6/blockstates/berry_bush.json")
				.getAsJsonObject("variants");
		assertEquals(36, tVariants.size(), "4 ages x 9 kinds, every state seated");
		java.util.Set<String> tModelNames = new java.util.HashSet<>();
		String[] tModelsByAge = {"gt6:block/berry_bush", "gt6:block/berry_bush_stage1",
				"gt6:block/berry_bush_stage2", "gt6:block/berry_bush_stage2"};
		for (int tAge = 0; tAge <= 3; tAge++) {
			for (String tKind : KINDS) {
				String tKey = "age=" + tAge + ",kind=" + tKind;
				assertTrue(tVariants.has(tKey), "the variant key " + tKey + " exists");
				String tModel = tVariants.getAsJsonObject(tKey).get("model").getAsString();
				assertEquals(tModelsByAge[tAge], tModel,
						"the " + tKey + " state rides its per-AGE model");
				tModelNames.add(tModel);
			}
		}
		assertEquals(3, tModelNames.size(), "the 36 variants collapse to exactly the bush trio");
	}

	/**
	 * The configured feature: the weighted KIND nine, every entry at the upstream birth
	 * stage 3 (the placeBushCore :86 verbatim face — no randomizer; the vanilla homolog
	 * covers the growth/harvest mechanics only).
	 */
	@Test
	void theConfiguredFeatureSpawnsTheNineKindsAtStageThree() throws Exception {
		JsonObject tToPlace = generatedJson("data/gt6/worldgen/configured_feature/plant_bush.json")
				.getAsJsonObject("config").getAsJsonObject("to_place");
		assertEquals("minecraft:weighted_state_provider", tToPlace.get("type").getAsString(),
				"the weighted provider IS the provider — no randomizer seat");
		JsonArray tEntries = tToPlace.getAsJsonArray("entries");
		assertEquals(9, tEntries.size(), "the BushesGT.MAP full set, one entry per kind");
		java.util.HashSet<String> tKinds = new java.util.HashSet<>();
		for (int i = 0; i < tEntries.size(); i++) {
			JsonObject tEntry = tEntries.get(i).getAsJsonObject();
			assertEquals(1, tEntry.get("weight").getAsInt(), "uniform kind weights");
			JsonObject tState = tEntry.getAsJsonObject("data");
			assertEquals("gt6:berry_bush", tState.get("Name").getAsString(), "the bush block");
			JsonObject tProps = tState.getAsJsonObject("Properties");
			assertEquals("3", tProps.get("age").getAsString(),
					"the birth stage = the upstream fixed 3 (placeBushCore :86 verbatim)");
			tKinds.add(tProps.get("kind").getAsString());
		}
		assertEquals(new java.util.HashSet<>(KINDS), tKinds, "the nine kinds cover the full set");
	}
}
