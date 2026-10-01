/*
 * The task food-bottles-min data-face test: the generated {@code data/gt6/recipes/} rows
 * of the bottles band are read verbatim off the classpath (the GT6ClayBandDatagenTest
 * convention) and asserted on their upstream-transcription faces —
 * <ul>
 * <li>the ketchup ladder :643-:647 — 1 ketchup bottle + 1-5 flat doughs → 1-5 sauced flat
 *     doughs (MultiItemFood.java:643-:647 verbatim),</li>
 * <li>the heavy-cream vanilla cake re-craft :636 — "C"/"Z" over the cream bottle + the T3
 *     cake bottom → minecraft:cake,</li>
 * <li>the BBQ ribs :550 — grilled ribs + BBQ sauce → barbecue ribs,</li>
 * <li>THE PIZZA DEAD-END CLOSURE: the band walk proves the chain ALIVE — dough_flat has a
 *     live producer (the :642 rolling-pin row), the five ladder rows are its ONLY
 *     remaining producers of food_dough_flat_ketchup, and the two already-landed pizza
 *     rows consume it (before this card the ketchup flat had ZERO producers and the pizza
 *     pair was unreachable — the state research.food-crafting-tail 活缺陷 finding).</li>
 * </ul>
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class GT6BottleCraftDatagenTest {

	private static final String KETCHUP = "gt6:food_ketchup";
	private static final String FLAT = "gt6:food_dough_flat";
	private static final String FLAT_KETCHUP = "gt6:food_dough_flat_ketchup";

	private static JsonObject generated(String aPath) throws Exception {
		String tPath = "/data/gt6/recipes/" + aPath + ".json";
		try (InputStream tStream = GT6BottleCraftDatagenTest.class.getResourceAsStream(tPath)) {
			assertNotNull(tStream, tPath + " rides the generated-resources classpath");
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	private static String resultItem(JsonObject aRow) {
		return aRow.getAsJsonObject("result").get("item").getAsString();
	}

	private static String resultCount(JsonObject aRow) {
		return aRow.getAsJsonObject("result").has("count") ? aRow.getAsJsonObject("result").get("count").getAsString() : "1";
	}

	private static List<String> shapelessItems(JsonObject aRow) {
		List<String> rItems = new ArrayList<>();
		for (var tElement : aRow.getAsJsonArray("ingredients")) {
			// the ingredient entries are single objects here (no unions in this band)
			rItems.add(tElement.getAsJsonObject().get("item").getAsString());
		}
		return rItems;
	}

	/**
	 * The ketchup ladder :643-:647 verbatim — rung N = 1 ketchup bottle + N flat doughs →
	 * N sauced flat doughs, the bottle spreads over 1-5 flats.
	 */
	@Test
	public void ketchupLadderRowsAreTheUpstreamRungs() throws Exception {
		for (int i = 1; i <= 5; i++) {
			JsonObject tRow = generated("bake_ketchup_flat_" + i);
			assertEquals("minecraft:crafting_shapeless", tRow.get("type").getAsString(), "rung " + i + " type");
			List<String> tItems = shapelessItems(tRow);
			assertEquals(1 + i, tItems.size(), "rung " + i + " ingredient count");
			assertEquals(KETCHUP, tItems.get(0), "rung " + i + " leads with the bottle");
			for (int j = 1; j <= i; j++) {
				assertEquals(FLAT, tItems.get(j), "rung " + i + " flat dough " + j);
			}
			assertEquals(FLAT_KETCHUP, resultItem(tRow), "rung " + i + " result");
			assertEquals(String.valueOf(i), resultCount(tRow), "rung " + i + " output count");
		}
	}

	/** The :636 vanilla cake re-craft verbatim — "C"/"Z" over the heavy-cream bottle + the cake bottom. */
	@Test
	public void heavyCreamCakeRowIsTheUpstreamShaped() throws Exception {
		JsonObject tRow = generated("bake_cake_heavycream");
		assertEquals("minecraft:crafting_shaped", tRow.get("type").getAsString(), ":636 type");
		JsonArray tPattern = tRow.getAsJsonArray("pattern");
		assertEquals(2, tPattern.size(), ":636 pattern rows");
		assertEquals("C", tPattern.get(0).getAsString(), ":636 cream row");
		assertEquals("Z", tPattern.get(1).getAsString(), ":636 bottom row");
		assertEquals("gt6:food_heavycream", tRow.getAsJsonObject("key").getAsJsonObject("C").get("item").getAsString(),
				":636 C = the foodHeavycream re-registration face, the port cream bottle");
		assertEquals("gt6:food_cakebottom", tRow.getAsJsonObject("key").getAsJsonObject("Z").get("item").getAsString(),
				":636 Z = the T3 bake cake bottom");
		assertEquals("minecraft:cake", resultItem(tRow), ":636 result IS the vanilla cake");
	}

	/** The :550 BBQ ribs row verbatim — grilled ribs + the BBQ sauce bottle → barbecue ribs. */
	@Test
	public void bbqRibsRowIsTheUpstreamShapeless() throws Exception {
		JsonObject tRow = generated("bake_rib_bbq");
		assertEquals("minecraft:crafting_shapeless", tRow.get("type").getAsString(), ":550 type");
		assertEquals(List.of("gt6:food_rib_cooked", "gt6:food_barbecuesauce"), shapelessItems(tRow), ":550 ingredients");
		assertEquals("gt6:food_rib_bbq", resultItem(tRow), ":550 result");
	}

	/**
	 * THE PIZZA CHAIN IS ALIVE (the dead-end closure): walked over the whole generated
	 * crafting band —
	 * <ul>
	 * <li>{@code gt6:food_dough_flat} has a live producer (the :642 rolling-pin row),</li>
	 * <li>{@code gt6:food_dough_flat_ketchup} has EXACTLY the five ladder rungs as its
	 *     producers (before this band: ZERO — the dead end),</li>
	 * <li>the two already-landed bake pizza rows consume the sauced flat → the chain
	 *     dough → flat → flat_ketchup → pizza_raw → (the T3 smelt) pizza is REACHABLE.</li>
	 * </ul>
	 */
	@Test
	public void pizzaChainIsAliveThroughTheKetchupLadder() throws Exception {
		List<String> tFlatProducers = new ArrayList<>();
		List<String> tFlatKetchupProducers = new ArrayList<>();
		List<String> tFlatKetchupConsumers = new ArrayList<>();
		try (Stream<Path> tFiles = Files.list(recipesRoot())) {
			for (Path tFile : tFiles.filter(p -> p.toString().endsWith(".json")).toList()) {
				JsonObject tRow = JsonParser.parseString(Files.readString(tFile, StandardCharsets.UTF_8)).getAsJsonObject();
				// the cooking rows carry "result" as a bare id STRING (the 1.20.1 cooking shape) — only the
				// crafting rows carry the {item: ...} object this walk consumes
				if (!tRow.has("result") || !tRow.get("result").isJsonObject()
						|| !tRow.getAsJsonObject("result").has("item")) continue;
				if (resultItem(tRow).equals(FLAT)) tFlatProducers.add(tFile.getFileName().toString());
				if (resultItem(tRow).equals(FLAT_KETCHUP)) tFlatKetchupProducers.add(tFile.getFileName().toString());
				if (ingredientsContain(tRow, FLAT_KETCHUP)) tFlatKetchupConsumers.add(tFile.getFileName().toString());
			}
		}
		assertTrue(tFlatProducers.contains("bake_dough_flat_rolling.json"),
				"the :642 rolling-pin row still produces the flat dough — the chain ground");
		tFlatKetchupProducers.sort(String::compareTo);
		assertEquals(List.of("bake_ketchup_flat_1.json", "bake_ketchup_flat_2.json", "bake_ketchup_flat_3.json",
				"bake_ketchup_flat_4.json", "bake_ketchup_flat_5.json"), tFlatKetchupProducers,
				"the five ladder rungs are the ONLY producers of the sauced flat (the dead end is closed, not multiplied)");
		assertTrue(tFlatKetchupConsumers.contains("bake_pizza_cheese_raw.json")
				&& tFlatKetchupConsumers.contains("bake_pizza_meat_raw.json"),
				"the already-landed pizza pair consumes the sauced flat: consumers = " + tFlatKetchupConsumers);
	}

	private static boolean ingredientsContain(JsonObject aRow, String aItem) throws IOException {
		if (aRow.has("ingredients")) {
			for (var tElement : aRow.getAsJsonArray("ingredients")) {
				if (tElement.isJsonArray()) {
					for (var tAlt : tElement.getAsJsonArray()) {
						if (isItemEntry(tAlt, aItem)) return true;
					}
				} else if (isItemEntry(tElement, aItem)) return true;
			}
		}
		if (aRow.has("key")) {
			for (var tEntry : aRow.getAsJsonObject("key").entrySet()) {
				if (isItemEntry(tEntry.getValue(), aItem)) return true;
			}
		}
		return false;
	}

	/** The entry carries a plain item id equal to the needle (tag entries carry "tag", not "item"). */
	private static boolean isItemEntry(com.google.gson.JsonElement aEntry, String aItem) {
		return aEntry.isJsonObject() && aEntry.getAsJsonObject().has("item")
				&& aItem.equals(aEntry.getAsJsonObject().get("item").getAsString());
	}

	/**
	 * The generated {@code data/gt6/recipes/} directory (walked from the classpath resource
	 * URL — the GT6CellDatagenTest tree-walk form).
	 */
	private static Path recipesRoot() throws Exception {
		return Path.of(GT6BottleCraftDatagenTest.class.getResource("/data/gt6/recipes").toURI());
	}
}
