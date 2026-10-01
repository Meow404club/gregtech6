/**
 * Offline pin for task bush-growth-blockstate — the generated-tree faces of the wild
 * bush's blockstate form. The blockstate emits the FULL 16-state grid (AGE_3 x KIND)
 * over the one tinted cube model (the colour rides the GT6BushTintListener per-state
 * arm, not the JSON), and the configured feature composes the vanilla
 * RandomizedIntStateProvider (the birth-stage randomization, the card acceptance 5) over
 * the WeightedStateProvider quartet (the kind spread — the upstream NoiseGenerator patch
 * index and the FIXED stage-3 birth, WorldgenBushes.java:66/:86, are the declared
 * collapses in GT6WorldgenDatagen).
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

	private static JsonObject generatedJson(String aPath) throws Exception {
		try (InputStream tStream = GT6BushBlockstateDatagenTest.class.getClassLoader()
				.getResourceAsStream(aPath)) {
			assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8))
					.getAsJsonObject();
		}
	}

	/** The blockstate grid: every AGE x KIND state points at the one tinted cube model. */
	@Test
	void theBlockstateEmitsTheFullSixteenStateGrid() throws Exception {
		JsonObject tVariants = generatedJson("assets/gt6/blockstates/berry_bush.json")
				.getAsJsonObject("variants");
		assertEquals(16, tVariants.size(), "4 ages x 4 kinds, every state seated");
		for (int tAge = 0; tAge <= 3; tAge++) {
			for (String tKind : List.of("blueberry", "candleberry", "cranberry", "cotton")) {
				String tKey = "age=" + tAge + ",kind=" + tKind;
				assertTrue(tVariants.has(tKey), "the variant key " + tKey + " exists");
				assertEquals("gt6:block/berry_bush", tVariants.getAsJsonObject(tKey).get("model").getAsString(),
						"the " + tKey + " state rides the one tinted cube model");
			}
		}
	}

	/**
	 * The configured feature: the AGE randomizer (property "age", uniform 0-3) over the
	 * weighted KIND quartet — the plant_bush.json face of the birth-stage randomization.
	 */
	@Test
	void theConfiguredFeatureRandomizesStageAndKind() throws Exception {
		JsonObject tToPlace = generatedJson("data/gt6/worldgen/configured_feature/plant_bush.json")
				.getAsJsonObject("config").getAsJsonObject("to_place");
		assertEquals("minecraft:randomized_int_state_provider", tToPlace.get("type").getAsString(),
				"the vanilla AGE randomizer seat");
		assertEquals("age", tToPlace.get("property").getAsString(), "the randomizer drives AGE");
		JsonObject tValues = tToPlace.getAsJsonObject("values");
		assertEquals("minecraft:uniform", tValues.get("type").getAsString(), "uniform over the stage range");
		assertEquals(0, tValues.getAsJsonObject("value").get("min_inclusive").getAsInt(), "birth stage floor 0");
		assertEquals(3, tValues.getAsJsonObject("value").get("max_inclusive").getAsInt(), "birth stage cap 3");
		JsonObject tSource = tToPlace.getAsJsonObject("source");
		assertEquals("minecraft:weighted_state_provider", tSource.get("type").getAsString(),
				"the weighted KIND quartet seat");
		JsonArray tEntries = tSource.getAsJsonArray("entries");
		assertEquals(4, tEntries.size(), "the card ruling quartet, one entry per kind");
		java.util.HashSet<String> tKinds = new java.util.HashSet<>();
		for (int i = 0; i < tEntries.size(); i++) {
			JsonObject tEntry = tEntries.get(i).getAsJsonObject();
			assertEquals(1, tEntry.get("weight").getAsInt(), "uniform kind weights");
			JsonObject tState = tEntry.getAsJsonObject("data");
			assertEquals("gt6:berry_bush", tState.get("Name").getAsString(), "the bush block");
			tKinds.add(tState.getAsJsonObject("Properties").get("kind").getAsString());
		}
		assertEquals(java.util.Set.of("blueberry", "candleberry", "cranberry", "cotton"), tKinds,
				"the four kinds cover the quartet");
	}
}
