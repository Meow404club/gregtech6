package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * The wooden shelf/crate tool-letter parity pin (task storage-tool-char-decode): the
 * "sfr" tool row keys the CR.java table VERBATIM — 's' = saw (:356), 'f' = file (:344),
 * 'r' = softhammer (:355) — over the upstream :181-184 rows (Loader_MultiTileEntities,
 * the wooden Bookshelf "PPP"/"sfr"/"PPP" + Bottlecrate "sfr"/"PGP"/"BPB"). The
 * storage-static-batch landing had decoded 's'/'r' as hard hammer/screwdriver; every
 * generated bookshelf_*.json / bottlecrate_*.json now keys the three letters off the
 * gt6 tool tags, pinned here so a future table drift fails instead of shipping.
 */
public class GT6StorageToolCharParityTest {

	/** The CR.java letter → the gt6 tool tag (the :344/:355/:356 switch arms). */
	private static final String[][] TOOL_LETTERS = {
			{"s", "gt6:tools/saw"         , ":356"},
			{"f", "gt6:tools/file"        , ":344"},
			{"r", "gt6:tools/soft_hammer" , ":355"},
	};

	@Test
	public void theShelfToolLettersKeyTheCrTable() throws Exception {
		for (gregtech6.registry.GT6StaticStorages.Plank tPlank : gregtech6.registry.GT6StaticStorages.PLANKS) {
			JsonObject tJson = generated("bookshelf_" + tPlank.slug());
			assertEquals("PPP", tJson.getAsJsonArray("pattern").get(0).getAsString(), tPlank.slug() + " shelf :181 row 1");
			assertEquals("sfr", tJson.getAsJsonArray("pattern").get(1).getAsString(), tPlank.slug() + " shelf :181 row 2");
			assertEquals("PPP", tJson.getAsJsonArray("pattern").get(2).getAsString(), tPlank.slug() + " shelf :181 row 3");
			assertToolRow(tPlank.slug(), tJson.getAsJsonObject("key"));
			assertEquals("minecraft:" + tPlank.slug() + "_planks", tJson.getAsJsonObject("key").getAsJsonObject("P").get("item").getAsString(), tPlank.slug() + " shelf: the row's plank");
			assertEquals("gt6:bookshelf_" + tPlank.slug(), tJson.getAsJsonObject("result").get("item").getAsString(), tPlank.slug() + " shelf: the result");
		}
	}

	@Test
	public void theCrateToolLettersKeyTheCrTable() throws Exception {
		for (gregtech6.registry.GT6StaticStorages.Plank tPlank : gregtech6.registry.GT6StaticStorages.PLANKS) {
			JsonObject tJson = generated("bottlecrate_" + tPlank.slug());
			assertEquals("sfr", tJson.getAsJsonArray("pattern").get(0).getAsString(), tPlank.slug() + " crate :184 row 1");
			assertEquals("PGP", tJson.getAsJsonArray("pattern").get(1).getAsString(), tPlank.slug() + " crate :184 row 2");
			assertEquals("BPB", tJson.getAsJsonArray("pattern").get(2).getAsString(), tPlank.slug() + " crate :184 row 3");
			assertToolRow(tPlank.slug(), tJson.getAsJsonObject("key"));
			JsonObject tKey = tJson.getAsJsonObject("key");
			assertEquals("minecraft:" + tPlank.slug() + "_planks", tKey.getAsJsonObject("P").get("item").getAsString(), tPlank.slug() + " crate: the row's plank");
			assertEquals("gt6:bolt_wood", tKey.getAsJsonObject("B").get("item").getAsString(), tPlank.slug() + " crate: the wood bolt");
			assertEquals("minecraft:slime_ball", tKey.getAsJsonObject("G").get("item").getAsString(), tPlank.slug() + " crate: the itemGlue fold");
			assertEquals("gt6:bottlecrate_" + tPlank.slug(), tJson.getAsJsonObject("result").get("item").getAsString(), tPlank.slug() + " crate: the result");
		}
	}

	/** The datagen walk covers the whole vanilla-planks subset (a dropped plank row fails here, not silently). */
	@Test
	public void thePlankCensusIsTheTenVanillaSlugs() {
		assertEquals(10, gregtech6.registry.GT6StaticStorages.PLANKS.size(), "the :181-184 ladder fold");
	}

	/** The three shared tool letters: every one keys the CR.java table tag (a swapped letter fails with the arm line). */
	private static void assertToolRow(String aSlug, JsonObject aKey) {
		for (String[] tLetter : TOOL_LETTERS) {
			JsonObject tEntry = aKey.getAsJsonObject(tLetter[0]);
			assertNotNull(tEntry, aSlug + ": the '" + tLetter[0] + "' key ships");
			assertEquals(tLetter[1], tEntry.get("tag").getAsString(), aSlug + ": '" + tLetter[0] + "' = " + tLetter[1] + " (CR.java" + tLetter[2] + ")");
		}
	}

	private static JsonObject generated(String aId) throws Exception {
		byte[] tBytes = resourceOrNull("data/gt6/recipes/" + aId + ".json");
		assertNotNull(tBytes, "data/gt6/recipes/" + aId + ".json rides the generated-resources classpath");
		return JsonParser.parseString(new String(tBytes, StandardCharsets.UTF_8)).getAsJsonObject();
	}

	private static byte[] resourceOrNull(String aPath) throws Exception {
		try (InputStream tStream = GT6StorageToolCharParityTest.class.getClassLoader().getResourceAsStream(aPath)) {
			return tStream == null ? null : tStream.readAllBytes();
		}
	}
}
