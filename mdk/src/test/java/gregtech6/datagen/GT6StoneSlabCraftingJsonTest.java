/**
 * The stone-slab data-face parity test (task debt-slab-gap acceptance): the generated
 * {@code data/gt6/recipes/slab_*} rows are read verbatim off the classpath (the
 * GT6CircuitsCraftingJsonTest convention) and asserted on their upstream-transcription
 * faces — the BlockStones.java:269/:327 stone rows and the BlockMetaType.java:89/:93
 * generic conversion rows — plus the on-disk census of the whole band (578 rows) and one
 * blockstate/loot face each.
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

import gregtech6.registry.GTStoneBlocks;
import gregtech6.block.stone.StoneVariant;

public class GT6StoneSlabCraftingJsonTest {

	private static JsonObject generated(String aPath) throws Exception {
		String tPath = "/data/gt6/recipes/" + aPath + ".json";
		try (InputStream tStream = GT6StoneSlabCraftingJsonTest.class.getResourceAsStream(tPath)) {
			assertNotNull(tStream, tPath + " rides the generated-resources classpath");
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	private static JsonObject generatedAt(String aDirPrefix, String aPath) throws Exception {
		String tPath = "/" + aDirPrefix + "/" + aPath + ".json";
		try (InputStream tStream = GT6StoneSlabCraftingJsonTest.class.getResourceAsStream(tPath)) {
			assertNotNull(tStream, tPath + " rides the generated-resources classpath");
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	private static JsonArray pattern(JsonObject aRow) {
		return aRow.getAsJsonArray("pattern");
	}

	private static JsonObject key(JsonObject aRow, char aLetter) {
		return aRow.getAsJsonObject("key").getAsJsonObject(String.valueOf(aLetter));
	}

	private static String resultItem(JsonObject aRow) {
		return aRow.getAsJsonObject("result").get("item").getAsString();
	}

	private static int resultCount(JsonObject aRow) {
		return aRow.getAsJsonObject("result").has("count") ? aRow.getAsJsonObject("result").get("count").getAsInt() : 1;
	}

	// ------------------------------------------------- the census (disk face)

	/** The 578-row census: 272 slab_to_block + 272 slab_saw + 17 slab_rock + 17 slab_cobble. */
	@Test
	public void slabBandCensusIsPinned() throws IOException {
		Path tRecipes = mdkRoot().resolve("src").resolve("generated").resolve("resources")
				.resolve("data").resolve("gt6").resolve("recipes");
		assertTrue(Files.isDirectory(tRecipes), "the generated recipes tree exists");
		Set<String> tSeen = new HashSet<>();
		try (Stream<Path> tWalk = Files.walk(tRecipes, 3)) {
			tWalk.filter(p -> p.getFileName().toString().endsWith(".json")).forEach(p -> {
				String tRel = tRecipes.relativize(p).toString().replace('\\', '/');
				if (tRel.startsWith("slab_")) tSeen.add(tRel.substring(0, tRel.length() - ".json".length()));
			});
		}
		assertEquals(578, tSeen.size(), "272 + 272 + 17 + 17 slab rows exactly");
		// the id set is derived from the registration walk — no orphan, no gap
		Set<String> tExpected = new HashSet<>();
		for (GTStoneBlocks.VariantKey tKey : GTStoneBlocks.registrationOrder()) {
			String tPath = GTStoneBlocks.path(tKey.stone().snake(), tKey.variant());
			tExpected.add("slab_to_block/" + tPath);
			tExpected.add("slab_saw/" + tPath);
		}
		for (GTStoneBlocks.StoneSpec tStone : GTStoneBlocks.STONES) {
			tExpected.add("slab_rock/" + tStone.snake());
			tExpected.add("slab_cobble/" + tStone.snake());
		}
		assertEquals(tExpected, tSeen, "the band is exactly the 272-pair + 17-family walk");
	}

	/** Location of the mdk project root (the GT6AssetCoverageGuardTest.mdkRoot shape). */
	private static Path mdkRoot() {
		for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
			if (Files.isRegularFile(p.resolve("tools").resolve("gen_textures.py"))) return p;
		}
		throw new AssertionError("mdk root (tools/gen_textures.py) not found upward from "
				+ Path.of("").toAbsolutePath());
	}

	// ------------------------------------------------- the BlockStones rows

	/**
	 * BlockStones.java:269 — {@code CR.shaped(mSlabs[0] x1 COBBL, "  ","XX", 'X' =
	 * OP.rockGt.dat(mMaterial))}: two small rocks make ONE COBBL-variant slab, granite row.
	 */
	@Test
	public void rocksRowIsThe269Transcription() throws Exception {
		JsonObject tRow = generated("slab_rock/granite");
		assertEquals("  ", pattern(tRow).get(0).getAsString(), "the :269 two-row pattern verbatim (leading empty row)");
		assertEquals("XX", pattern(tRow).get(1).getAsString());
		assertEquals("gt6:rock_gt_granite", key(tRow, 'X').get("item").getAsString());
		assertEquals("gt6:granite_cobble_slab", resultItem(tRow));
		assertEquals(1, resultCount(tRow));
	}

	/** :269, the PrismarineLight family — the rock id rides the MATERIAL internal name (rock_gt_prismarine, MT.java:2397). */
	@Test
	public void rocksRowPrismarineCarriesTheMaterialInternalName() throws Exception {
		JsonObject tRow = generated("slab_rock/prismarine_light");
		assertEquals("gt6:rock_gt_prismarine", key(tRow, 'X').get("item").getAsString(),
				"PrismarineLight.mNameInternal = \"Prismarine\" — the stairs-card quirk pin");
		assertEquals("gt6:prismarine_light_cobble_slab", resultItem(tRow));
	}

	/**
	 * BlockStones.java:327 — the mEqualBlocks[COBBL] row: two family cobblestones make
	 * FOUR COBBL-variant slabs (the 1:2 sawmill ratio), marble row.
	 */
	@Test
	public void cobbleRowIsThe327Transcription() throws Exception {
		JsonObject tRow = generated("slab_cobble/marble");
		assertEquals("  ", pattern(tRow).get(0).getAsString());
		assertEquals("XX", pattern(tRow).get(1).getAsString());
		assertEquals("gt6:marble_cobble", key(tRow, 'X').get("item").getAsString());
		assertEquals("gt6:marble_cobble_slab", resultItem(tRow));
		assertEquals(4, resultCount(tRow), "the :327 x4 output");
	}

	// ------------------------------------------------- the BlockMetaType generic rows

	/** BlockMetaType.java:89 — two slabs stacked make the full block, variant-0 granite pair. */
	@Test
	public void slabToBlockRowIsThe89Transcription() throws Exception {
		JsonObject tRow = generated("slab_to_block/granite");
		assertEquals("X", pattern(tRow).get(0).getAsString());
		assertEquals("X", pattern(tRow).get(1).getAsString());
		assertEquals("gt6:granite_slab", key(tRow, 'X').get("item").getAsString());
		assertEquals("gt6:granite", resultItem(tRow));
		assertEquals(1, resultCount(tRow));
	}

	/** :89, a non-zero variant pair — each pair converts to its OWN variant on both legs. */
	@Test
	public void slabToBlockRowCarriesThePairIdentity() throws Exception {
		JsonObject tRow = generated("slab_to_block/marble_bricks_chiseled");
		assertEquals("gt6:marble_bricks_chiseled_slab", key(tRow, 'X').get("item").getAsString());
		assertEquals("gt6:marble_bricks_chiseled", resultItem(tRow));
	}

	/**
	 * BlockMetaType.java:93 — block plus saw make TWO slabs (the crafting-table face of
	 * the :92 sawmill row; the machine row itself is the sawing-map deferral).
	 */
	@Test
	public void slabSawRowIsThe93Transcription() throws Exception {
		JsonObject tRow = generated("slab_saw/basalt");
		assertEquals("sX", pattern(tRow).get(0).getAsString());
		assertEquals("gt6:tools/saw", key(tRow, 's').get("tag").getAsString(),
				"'s' keys the port saw tool tag (the spray-can row precedent)");
		assertEquals("gt6:basalt", key(tRow, 'X').get("item").getAsString());
		assertEquals("gt6:basalt_slab", resultItem(tRow));
		assertEquals(2, resultCount(tRow), "the :93 x2 output");
	}

	// ------------------------------------------------- the blockstate/loot faces

	/**
	 * The blockstate triad (the addStoneSlabs band): BOTTOM/TOP over the own
	 * {@code minecraft:block/slab[_top]} parents' models, DOUBLE over the paired full
	 * cube model — granite variant-0.
	 */
	@Test
	public void blockstateTriadPointsDoubleAtTheFullCube() throws Exception {
		JsonObject tState = generatedAt("assets/gt6/blockstates", "granite_slab");
		JsonObject tVariants = tState.getAsJsonObject("variants");
		assertEquals("gt6:block/stones/granite/stone_slab",
				tVariants.getAsJsonObject("type=bottom").get("model").getAsString());
		assertEquals("gt6:block/stones/granite/stone_slab_top",
				tVariants.getAsJsonObject("type=top").get("model").getAsString());
		assertEquals("gt6:block/stones/granite/stone",
				tVariants.getAsJsonObject("type=double").get("model").getAsString(),
				"the DOUBLE state IS the upstream full-block double (the :142-153 merge face)");
	}

	/**
	 * The loot face (the GT6StoneSlabLoot band): the vanilla createSlabItemTable shape —
	 * self-drop with {@code set_count 2} gated on {@code type=double} +
	 * {@code explosion_decay}.
	 */
	@Test
	public void lootTableIsTheVanillaSlabIdiom() throws Exception {
		JsonObject tLoot = generatedAt("data/gt6/loot_tables/blocks", "granite_slab");
		JsonObject tEntry = tLoot.getAsJsonArray("pools").get(0).getAsJsonObject()
				.getAsJsonArray("entries").get(0).getAsJsonObject();
		assertEquals("minecraft:item", tEntry.get("type").getAsString());
		assertEquals("gt6:granite_slab", tEntry.get("name").getAsString(),
				"upstream :178 — the slab drops the slab (mSlabs[0])");
		JsonArray tFunctions = tEntry.getAsJsonArray("functions");
		assertEquals(2, tFunctions.size());
		JsonObject tCount = tFunctions.get(0).getAsJsonObject();
		assertEquals("minecraft:set_count", tCount.get("function").getAsString());
		assertEquals(2.0F, tCount.get("count").getAsFloat(), "double = two slabs' worth");
		JsonObject tCond = tCount.getAsJsonArray("conditions").get(0).getAsJsonObject();
		assertEquals("minecraft:block_state_property", tCond.get("condition").getAsString());
		assertEquals("double", tCond.getAsJsonObject("properties").get("type").getAsString());
		assertEquals("minecraft:explosion_decay", tFunctions.get(1).getAsJsonObject().get("function").getAsString());
	}

	// ------------------------------------------------- the lang ratchet face

	/** The ONE slab template lands on both lang faces (the GT6LangParityTest slot pin's data twin). */
	@Test
	public void slabTemplateLandsOnBothLangFaces() throws Exception {
		for (String tLocale : new String[] {"en_us", "zh_cn"}) {
			JsonObject tLang = generatedAt("assets/gt6/lang", tLocale);
			assertTrue(tLang.has("gt6.stone.slab"), tLocale + " carries the slab template");
			assertEquals(1, tLang.get("gt6.stone.slab").getAsString().split("%s", -1).length - 1,
					tLocale + " slab template has exactly one slot");
		}
		assertEquals("%s Slab", generatedAt("assets/gt6/lang", "en_us").get("gt6.stone.slab").getAsString());
		assertEquals("%s半砖", generatedAt("assets/gt6/lang", "zh_cn").get("gt6.stone.slab").getAsString(),
				"dump :15977-15982 — the variant name + 半砖 suffix");
		// the composed example: Granite Cobblestone Slab == the granite variant-1 template over the granite material unit
		JsonObject tEn = generatedAt("assets/gt6/lang", "en_us");
		String tComposed = tEn.get(StoneVariant.COBBL.key()).getAsString()
				.replace("%s", tEn.get("gt6.material.granite").getAsString());
		assertEquals("Granite Cobblestone", tComposed);
		assertEquals("Granite Cobblestone Slab",
				tEn.get("gt6.stone.slab").getAsString().replace("%s", tComposed));
	}
}
