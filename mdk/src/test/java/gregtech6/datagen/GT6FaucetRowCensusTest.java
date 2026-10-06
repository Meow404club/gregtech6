package gregtech6.datagen;

import com.google.gson.JsonObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.OP;
import gregtech6.registry.GT6MaterialTestSupport;
import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GT6Molds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The faucet 39-row reconciliation pin (task faucet-material-rows spec 1 — the
 * "参数化族禁代表性移植" lesson as a test): every row carries its upstream meta (the
 * Loader_MultiTileEntities.java:300-341 / zh-dump mte 1700-1749 key), en local word,
 * acid/resistance pair, hidden flag and craft face, and the generated tree carries the
 * matching recipes/lang. Column ground truth = the {@link GT6Molds.FaucetRow} javadoc.
 */
public class GT6FaucetRowCensusTest extends GTOfflineTestBase {

	@BeforeAll
	static void bootMaterials() {
		GT6MaterialTestSupport.materials();
	}

	private static JsonObject generatedJson(String aPath) throws Exception {
		try (InputStream tStream = GT6FaucetRowCensusTest.class.getClassLoader().getResourceAsStream(aPath)) {
			assertNotNull(tStream, "the generated JSON must be on the classpath: " + aPath);
			return com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(tStream,
					java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}

	// -------------------------------------------------------------------
	// the row table against the upstream projection
	// -------------------------------------------------------------------

	/** 39 rows, each meta unique, exactly the upstream ids (1710-1717/1745-1747 are upstream gaps too). */
	@Test
	public void metasAreTheUpstreamProjection() {
		assertEquals(39, GT6Molds.FAUCET_ROWS.size(), "the full Loader:300-341 projection");
		Set<Integer> tMetas = new TreeSet<>();
		for (GT6Molds.FaucetRow tRow : GT6Molds.FAUCET_ROWS) tMetas.add(tRow.meta());
		assertEquals(39, tMetas.size(), "metas unique");
		for (int m = 1700; m <= 1709; m++) assertTrue(tMetas.contains(m), "meta " + m);
		for (int m = 1718; m <= 1744; m++) assertTrue(tMetas.contains(m), "meta " + m);
		assertTrue(tMetas.contains(1748) && tMetas.contains(1749), "the bedrock/adamantium tail");
		for (int m = 1710; m <= 1717; m++) assertFalse(tMetas.contains(m), "upstream gap " + m);
		for (int m = 1745; m <= 1747; m++) assertFalse(tMetas.contains(m), "upstream gap " + m);
	}

	/** NBT_ACIDPROOF T = the 8 metal rows (Loader:318/:322/:326/:328/:333/:336/:338/:341). */
	@Test
	public void acidProofIsTheUpstreamEightRows() {
		Set<String> tAcid = new TreeSet<>();
		for (GT6Molds.FaucetRow tRow : GT6Molds.FAUCET_ROWS) if (tRow.acidProof()) tAcid.add(tRow.path());
		assertEquals(Set.of("faucet_stainless_steel", "faucet_netherite", "faucet_thaumium", "faucet_chromium",
				"faucet_iridium", "faucet_tungsten", "faucet_void_metal", "faucet_adamantium"), tAcid);
	}

	/** NBT_RESISTANCE: 5.0 = the stone family + quartz (11 rows), 6.0 = carbon + the metals (28) — carbon is F/6.0, NOT derivable from acidProof. */
	@Test
	public void resistanceSplitsAtTheCarbonRow() {
		int tFive = 0, tSix = 0;
		for (GT6Molds.FaucetRow tRow : GT6Molds.FAUCET_ROWS) {
			if (tRow.resistance() == 5.0F) {
				tFive++;
				assertTrue(tRow.meta() <= 1709 || tRow.meta() == 1718, tRow.path() + ": 5.0 = the stone family + quartz");
			} else if (tRow.resistance() == 6.0F) {
				tSix++;
				assertTrue(tRow.meta() >= 1719, tRow.path() + ": 6.0 = carbon + metals");
			} else {
				assertNull(tRow, tRow.path() + ": no other resistance value upstream");
			}
		}
		assertEquals(11, tFive, "the 5.0 rung");
		assertEquals(28, tSix, "the 6.0 rung");
	}

	/** NBT_HIDDEN = the 8 stone-family rows (Loader:301-304 unconditional, :306-309 the mod gates resolved hidden in the single-mod port). */
	@Test
	public void hiddenIsTheStoneFamily() {
		Set<String> tHidden = new TreeSet<>();
		for (GT6Molds.FaucetRow tRow : GT6Molds.FAUCET_ROWS) if (tRow.hidden()) tHidden.add(tRow.path());
		assertEquals(Set.of("faucet_basalt", "faucet_black_granite", "faucet_red_granite", "faucet_nether_brick",
				"faucet_umber", "faucet_livingrock", "faucet_holystone", "faucet_betweenstone"), tHidden);
	}

	/** The craft face: 26 self-plate metals + graphene carbon + the declared cuts (8 stone-block, 2 ANY-material) + the 2 special rows. */
	@Test
	public void craftFacesMatchTheUpstreamIngredients() {
		Map<GT6Molds.FaucetCraft, Integer> tCounts = new java.util.EnumMap<>(GT6Molds.FaucetCraft.class);
		for (GT6Molds.FaucetRow tRow : GT6Molds.FAUCET_ROWS) tCounts.merge(tRow.craft(), 1, Integer::sum);
		assertEquals(26, tCounts.getOrDefault(GT6Molds.FaucetCraft.PLATE_SELF, 0), "the metal plate rows");
		assertEquals(1, tCounts.getOrDefault(GT6Molds.FaucetCraft.PLATE_GRAPHENE, 0), "the carbon row (:312)");
		assertEquals(8, tCounts.getOrDefault(GT6Molds.FaucetCraft.CUT_STONE_BLOCK, 0), "the OP.stone rows");
		assertEquals(2, tCounts.getOrDefault(GT6Molds.FaucetCraft.CUT_ANY_MATERIAL, 0), "quartz + tungsten (ANY pool)");
		assertEquals(2, tCounts.getOrDefault(GT6Molds.FaucetCraft.NONE, 0), "stone craft + ceramic smelt");
	}

	/** The en local words are the upstream getLocal faces verbatim (MT.java: the setLocal overrides included). */
	@Test
	public void enWordsAreTheUpstreamLocals() {
		Map<String, String> tWords = new java.util.HashMap<>();
		for (GT6Molds.FaucetRow tRow : GT6Molds.FAUCET_ROWS) tWords.put(tRow.path(), tRow.matDisplay());
		assertEquals("Umberstone", tWords.get("faucet_umber"), "MT.java:4537 setLocal");
		assertEquals("Black Granite", tWords.get("faucet_black_granite"), "MT.java:4554 setLocal");
		assertEquals("Red Granite", tWords.get("faucet_red_granite"), "MT.java:4553 setLocal");
		assertEquals("Nether Brick", tWords.get("faucet_nether_brick"), "MT.java:2393");
		assertEquals("Quartz", tWords.get("faucet_quartz"), "ANY.java:211 setLocal");
		assertEquals("Carbon", tWords.get("faucet_carbon"), "MT.java:967 carbon()");
		assertEquals("HSLA-Steel", tWords.get("faucet_hsla"), "MT.java:2467");
		assertEquals("Meteoric Steel", tWords.get("faucet_meteoric_steel"), "MT.java MeteoricSteel");
		assertEquals("Tungsten", tWords.get("faucet_tungsten"), "ANY.java:213 setLocal");
		assertEquals("Tantalum Hafnium Carbide", tWords.get("faucet_tantalum_hafnium_carbide"), "MT.java:2482");
		assertEquals("Bedrock-HSLA-Alloy", tWords.get("faucet_bedrock_hsla_alloy"), "MT.java:2552");
		assertEquals("Adamantium", tWords.get("faucet_adamantium"), "MT.java:1671");
	}

	// -------------------------------------------------------------------
	// the generated tree: recipes + lang
	// -------------------------------------------------------------------

	/** Every PLATE row has its shaped "P P"/" P " craft over gt6:plate_<mat>; CUT rows have NO recipe; the two specials keep theirs. */
	@Test
	public void recipesFollowTheRowTable() throws Exception {
		for (GT6Molds.FaucetRow tRow : GT6Molds.FAUCET_ROWS) {
			String tRecipePath = "data/gt6/recipes/" + tRow.path() + ".json";
			switch (tRow.craft()) {
				case PLATE_SELF, PLATE_GRAPHENE -> {
					JsonObject tRecipe = generatedJson(tRecipePath);
					assertEquals("minecraft:crafting_shaped", tRecipe.get("type").getAsString(), tRow.path() + ": shaped");
					assertEquals(List.of("P P", " P "), toList(tRecipe.getAsJsonArray("pattern")),
							tRow.path() + ": the three-point diagonal (:312/:314-341)");
					gregapi.oredict.OreDictMaterial tPlateMat = tRow.craft() == GT6Molds.FaucetCraft.PLATE_GRAPHENE
							? gregapi.data.MT.Graphene : tRow.material().get();
					String tPlateId = "gt6:" + GTMaterialItems.itemIdOf(OP.plate, tPlateMat);
					assertEquals(tPlateId, tRecipe.getAsJsonObject("key").getAsJsonObject("P").get("item").getAsString(),
							tRow.path() + ": the plate ingredient");
					assertEquals("gt6:" + tRow.path(), tRecipe.getAsJsonObject("result").get("item").getAsString(),
							tRow.path() + ": the faucet result (the 1.20.1 forge save face)");
				}
				case NONE -> {
					// stone keeps its own :300 handcraft; ceramic rides the raw-item pair
					// (faucet_ceramic_raw + smelt_faucet_ceramic, asserted below)
					if (tRow.path().equals("faucet_stone")) {
						assertNotNull(generatedJsonOrNull(tRecipePath), "the :300 stone craft");
					}
				}
				default -> assertNull(generatedJsonOrNull(tRecipePath),
						tRow.path() + ": the declared cut keeps no craft row");
			}
		}
		// the two NONE specials (the pre-existing rows this card walked with)
		assertNotNull(generatedJsonOrNull("data/gt6/recipes/faucet_stone.json"), "the :300 stone craft");
		assertNotNull(generatedJsonOrNull("data/gt6/recipes/faucet_ceramic_raw.json"), "the :305 raw clay craft");
		assertNotNull(generatedJsonOrNull("data/gt6/recipes/smelt_faucet_ceramic.json"), "the :305 furnace row");
	}

	/** The 39 composed names carry BOTH faces, zh verbatim off the dump (mte 1700-1749). */
	@Test
	public void langCarriesBothFacesForAll39() throws Exception {
		JsonObject tEn = generatedJson("assets/gt6/lang/en_us.json");
		JsonObject tZh = generatedJson("assets/gt6/lang/zh_cn.json");
		for (GT6Molds.FaucetRow tRow : GT6Molds.FAUCET_ROWS) {
			String tKey = GT6Molds.faucetMatUnitKeyOf(tRow);
			assertEquals(tRow.matDisplay(), tEn.get(tKey).getAsString(), tKey + ": the en face = the upstream local");
			assertTrue(tZh.has(tKey), tKey + ": the zh face exists");
			assertFalse(tZh.get(tKey).getAsString().isEmpty(), tKey + ": the zh face is non-empty");
		}
		// dump-verbatim spot pins (mte meta → word)
		assertEquals("艾德曼合金", tZh.get("gt6.row.faucet.mat.adamantium").getAsString(), "dump mte 1749");
		assertEquals("棕石", tZh.get("gt6.row.faucet.mat.umber").getAsString(), "dump mte 1706");
		assertEquals("HSLA钢", tZh.get("gt6.row.faucet.mat.hsla").getAsString(), "dump mte 1741");
		assertEquals("碳化钽铪", tZh.get("gt6.row.faucet.mat.tantalum_hafnium_carbide").getAsString(), "dump mte 1743");
		assertEquals("基岩合金", tZh.get("gt6.row.faucet.mat.bedrock_hsla_alloy").getAsString(), "dump mte 1748");
	}

	private static List<String> toList(com.google.gson.JsonArray aArray) {
		List<String> r = new ArrayList<>();
		aArray.forEach(e -> r.add(e.getAsString()));
		return r;
	}

	private static JsonObject generatedJsonOrNull(String aPath) throws Exception {
		try (InputStream tStream = GT6FaucetRowCensusTest.class.getClassLoader().getResourceAsStream(aPath)) {
			return tStream == null ? null : com.google.gson.JsonParser.parseReader(
					new java.io.InputStreamReader(tStream, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
		}
	}
}
