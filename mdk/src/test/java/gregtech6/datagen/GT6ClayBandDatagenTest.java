/**
 * The issue #45 C1 clay-band data-face test: the generated {@code data/gt6/recipes/}
 * rows are read verbatim off the classpath (the GT6CircuitsCraftingJsonTest convention)
 * and asserted on their upstream-transcription faces —
 * <ul>
 * <li>every clay shaped row carries the upstream {@code k=knife}/{@code R=rollingpin}
 *     tool marks as the {@code #gt6:tools/knife}/{@code #gt6:tools/rolling_pin} tag
 *     defines (MultiItemRandomTools.java:123-134; issue #45 C1 re-expansion after the
 *     port had cut them — the p24 tool seams take the 1-damage craft toll),</li>
 * <li>every raw clay item with a declared port source has a live acquisition recipe
 *     (the former dead links: {@code clay_bowl} had only the reverse+smelt faces,
 *     {@code clay_juicer} did not even exist — the Juicer block was creative-only),</li>
 * <li>the reverse shapeless (:119/:118 tails) and the hardening smelts (:2177/:2184)
 *     close both chains.</li>
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
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class GT6ClayBandDatagenTest {

	private static final String KNIFE_TAG = "gt6:tools/knife";
	private static final String ROLLING_PIN_TAG = "gt6:tools/rolling_pin";

	private static JsonObject generated(String aPath) throws Exception {
		String tPath = "/data/gt6/recipes/" + aPath + ".json";
		try (InputStream tStream = GT6ClayBandDatagenTest.class.getResourceAsStream(tPath)) {
			assertNotNull(tStream, tPath + " rides the generated-resources classpath");
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	private static JsonArray pattern(JsonObject aRow) {
		return aRow.getAsJsonArray("pattern");
	}

	private static String tagKey(JsonObject aRow, char aLetter) {
		return aRow.getAsJsonObject("key").getAsJsonObject(String.valueOf(aLetter)).get("tag").getAsString();
	}

	private static String resultItem(JsonObject aRow) {
		return aRow.getAsJsonObject("result").get("item").getAsString();
	}

	private static String resultCount(JsonObject aRow) {
		return aRow.getAsJsonObject("result").has("count") ? aRow.getAsJsonObject("result").get("count").getAsString() : "1";
	}

	// ------------------------------------------------- the tool steps (issue #45 C1 core)

	/**
	 * All four port clay shaped rows carry the knife+rolling-pin tag defines with the
	 * upstream patterns VERBATIM: blank mold :127 "C C","CCC","k R", faucet :128
	 * "C C","kCR", bowl :132 "k R","C C","CCC", juicer :131 "kCR","CCC".
	 */
	@Test
	public void toolShapingRowsCarryKnifeAndRollingPinTags() throws Exception {
		String[][] tRows = {
				{"mold_ceramic_raw", "C C", "CCC", "k R"},
				{"faucet_ceramic_raw", "C C", "kCR"},
				{"clay_bowl", "k R", "C C", "CCC"},
				{"clay_juicer", "kCR", "CCC"}};
		for (String[] tRow : tRows) {
			JsonObject tJson = generated(tRow[0]);
			assertEquals(tRow.length - 1, pattern(tJson).size(), tRow[0] + " pattern rows");
			for (int i = 1; i < tRow.length; i++) {
				assertEquals(tRow[i], pattern(tJson).get(i - 1).getAsString(), tRow[0] + " pattern row " + (i - 1));
			}
			assertEquals(KNIFE_TAG, tagKey(tJson, 'k'), tRow[0] + " knife key rides the tool tag");
			assertEquals(ROLLING_PIN_TAG, tagKey(tJson, 'R'), tRow[0] + " rolling pin key rides the tool tag");
			assertEquals("minecraft:clay_ball", tJson.getAsJsonObject("key").getAsJsonObject("C").get("item").getAsString(),
					tRow[0] + " clay key");
		}
	}

	// ------------------------------------------------- the acquisition faces

	/**
	 * The direct-source raws — each clay raw item with a port crafting source has a live
	 * shaped recipe whose result IS the raw item (issue #45 C1: bowl + juicer were the
	 * dead links; blank mold + faucet ride along as regression pins).
	 */
	@Test
	public void directSourceRawsHaveAcquisitionRecipes() throws Exception {
		String[][] tRaws = {
				{"mold_ceramic_raw", "mold_ceramic_raw"},
				{"faucet_ceramic_raw", "faucet_ceramic_raw"},
				{"clay_bowl", "clay_bowl"},
				{"clay_juicer", "clay_juicer"}};
		for (String[] tRaw : tRaws) {
			assertEquals("gt6:" + tRaw[1], resultItem(generated(tRaw[0])),
					tRaw[0] + " acquires the raw item " + tRaw[1]);
		}
	}

	/** The donation band census on disk: the vanilla exemplar set (:136-153) = 17 rows. */
	@Test
	public void donationBandCensusIsPinned() throws Exception {
		Path tRecipes = mdkRoot().resolve("src").resolve("generated").resolve("resources")
				.resolve("data").resolve("gt6").resolve("recipes");
		assertTrue(Files.isDirectory(tRecipes), "the generated recipes tree exists");
		Set<String> tSeen = new HashSet<>();
		try (Stream<Path> tWalk = Files.walk(tRecipes, 3)) {
			tWalk.filter(p -> p.getFileName().toString().endsWith(".json")).forEach(p -> {
				String tName = p.getFileName().toString().substring(0, p.getFileName().toString().length() - ".json".length());
				if (tName.startsWith("mold_") && tName.contains("_raw_from_")) tSeen.add(tName);
			});
		}
		assertEquals(17, tSeen.size(), "the donation band (blank raw + exemplar → shaped raw) stays 17 rows");
		for (String tId : tSeen) {
			String tShape = tId.substring("mold_".length(), tId.indexOf("_raw_from_"));
			assertEquals("gt6:mold_ceramic_" + tShape + "_raw", resultItem(generated(tId)),
					tId + " donates into its shape's raw");
		}
	}

	/** The reverse tails: bowl → 5 clay (:119), juicer → 4 clay (:118). */
	@Test
	public void reverseTailsReturnTheClayMass() throws Exception {
		JsonObject tBowl = generated("clay_bowl_reverse");
		assertEquals("minecraft:crafting_shapeless", tBowl.get("type").getAsString());
		assertEquals("minecraft:clay_ball", resultItem(tBowl));
		assertEquals("5", resultCount(tBowl));
		assertEquals("gt6:clay_bowl", tBowl.getAsJsonArray("ingredients").get(0).getAsJsonObject().get("item").getAsString());

		JsonObject tJuicer = generated("clay_juicer_reverse");
		assertEquals("minecraft:crafting_shapeless", tJuicer.get("type").getAsString());
		assertEquals("minecraft:clay_ball", resultItem(tJuicer));
		assertEquals("4", resultCount(tJuicer));
		assertEquals("gt6:clay_juicer", tJuicer.getAsJsonArray("ingredients").get(0).getAsJsonObject().get("item").getAsString());
	}

	/** The hardening smelts: bowl raw → mixing bowl (:2177), juicer raw → juicer (:2184). */
	@Test
	public void hardeningSmeltsCloseTheChains() throws Exception {
		JsonObject tBowl = generated("mixing_bowl");
		assertEquals("gt6:clay_bowl", tBowl.getAsJsonObject("ingredient").get("item").getAsString());
		assertEquals("gt6:mixing_bowl", tBowl.get("result").getAsString());

		JsonObject tJuicer = generated("juicer");
		assertEquals("gt6:clay_juicer", tJuicer.getAsJsonObject("ingredient").get("item").getAsString());
		assertEquals("gt6:juicer", tJuicer.get("result").getAsString());
		assertEquals(200, tJuicer.get("cookingtime").getAsInt(), "the vanilla smelt constant");
	}

	// ------------------------------------------------- the juicer raw item face

	/** The clay_juicer item model rides the borrowed 994.png sprite (assets/README.md). */
	@Test
	public void clayJuicerItemFaceIsWired() throws IOException {
		String tModel = readClasspath("/assets/gt6/models/item/clay_juicer.json");
		JsonObject tJson = JsonParser.parseString(tModel).getAsJsonObject();
		assertEquals("gt6:item/clay_juicer", tJson.getAsJsonObject("textures").get("layer0").getAsString());
		assertTrue(Files.isRegularFile(mdkRoot().resolve("src").resolve("main").resolve("resources")
				.resolve("assets").resolve("gt6").resolve("textures").resolve("item").resolve("clay_juicer.png")),
				"the borrowed 994.png sprite ships on disk");
	}

	private static String readClasspath(String aPath) throws IOException {
		try (InputStream tStream = GT6ClayBandDatagenTest.class.getResourceAsStream(aPath)) {
			assertNotNull(tStream, aPath + " rides the generated-resources classpath");
			return new String(tStream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	/** Location of the mdk project root (the GT6StoneSlabCraftingJsonTest.mdkRoot shape). */
	private static Path mdkRoot() {
		for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
			if (Files.isRegularFile(p.resolve("tools").resolve("gen_textures.py"))) return p;
		}
		throw new AssertionError("mdk root (tools/gen_textures.py) not found upward from "
				+ Path.of("").toAbsolutePath());
	}
}
