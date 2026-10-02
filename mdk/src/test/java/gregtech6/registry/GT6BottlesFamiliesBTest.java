/*
 * Offline census-ratchet tests for task btl-bottles-families-b: the families-B batch of
 * the MultiItemBottles domain (96 new rows over the families-a 75 = 171). Pins the full
 * 171-row order, the kitchen + smoothie segment census (the walk's ACTUAL row output —
 * 69 smoothie rows, the census "~46" headline was an undercount, the row list is the
 * authority), the six new TD.Creative.HIDDEN rows, the stack column, the 13 en desc rows,
 * the 96 sha256-grounded borrows and the bilingual lang faces — the census is hardcoded
 * here INDEPENDENT of the GT6Bottles.ROWS table so the pin cannot drift with the source.
 *
 * <p>The GT6BottlesFamiliesATest posture verbatim: no item construction offline (the
 * intrusive-holder wall), assertion surface = pure registry-wiring data + on-disk assets.
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

public class GT6BottlesFamiliesBTest {

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
	}

	/** The full 171-row order (the empty domain-entry item first, the metas ascending, the families-a landed seats preserved). */
	private static final String[] THE_ORDER = {
			"bottle_empty",
			// the families-a prefix: water :45-:53, grape :56-:68, lemon/potato/reed/hops/wheat/beer :71-:103
			"mnwtr", "seawater", "soda", "mineralsoda", "ice", "waterdirty", "swampwater", "stagnantwater", "spdew",
			"juicewhitegrape", "winewhite", "vinegar", "juiceredgrape", "winered", "whitegrapesmoothie",
			"redgrapesmoothie", "grapesmoothie", "purplegrapesmoothie", "grapejuice", "grc_grapewine0", "wine",
			"ricardosanchez",
			"juicelemon", "lemonade", "limoncello", "alcopops", "cavejohnsonsgrenadejuice", "lemonsmoothie",
			"potatojuice", "vodka", "leninade",
			"reedwater", "rumwhite", "rumdark", "canevinegar", "pina_colada",
			"hopsmash", "darkbeer", "dragonblood",
			"mashwheat", "whiskeywheat", "glenmckenner",
			"wheathopsmash", "beer",
			// the sauce family :106-:111 (805 = the landed BBQ seat)
			"chillysauce", "hotsauce", "diabolosauce", "diablosauce", "diablosauce_strong", "food_barbecuesauce",
			// the apple rows :114-:117 (no 903/904 metas exist upstream)
			"juiceapple", "ciderapple", "applevinegar", "applesmoothie",
			// the cooking oils :120-:127 (the OP.bottle.dat(MT.X) bottle anchors)
			"bottle_olive_oil", "bottle_sunflower_oil", "bottle_nut_oil", "bottle_seed_oil", "bottle_hemp_oil",
			"bottle_lin_oil", "bottle_fish_oil", "bottle_whale_oil",
			// mayo/dressing :134-:135, milk :137-:141 (1101 = the landed cream seat), soy :148-:149
			"mayo", "dressing",
			"bottle_milk", "food_heavycream", "spoiledmilk", "soymilk",
			// honey :156-:162, potion :176-:180, purple/rotten :182-:185, holy :187-:188, rice :190-:192
			"food_honeydrop", "bottle_honeydew", "royal_jelly", "ambrosia", "short_mead", "mead",
			"potion.goldenapplejuice", "potion.goldencider", "potion.idunsapplejuice", "potion.notchesbrew",
			"purpledrink", "rottendrink", "bottle_holy_water",
			"ricewater", "sake", "ricevinegar",
			// the chocolate creams :194-:195
			"chocolatecream", "nutella",
			// the smoothie walk :197-:341 (69 rows; 3101 ketchup rides its landed seat below)
			"strawberryjuice", "strawberrysmoothie", "bananajuice", "bananasmoothie",
			"bottle_slime_green", "bottle_slime_pink", "bottle_slime_blue", "bawls",
			"melonjuice", "melonsmoothie", "juice_juice", "fruitsmoothie",
			"kiwijuice", "kiwismoothie", "raspberryjuice", "raspberrysmoothie",
			"blackberryjuice", "blackberrysmoothie", "blueberryjuice", "blueberrysmoothie",
			"cranberryjuice", "cranberrysmoothie", "gooseberryjuice", "gooseberrysmoothie",
			"juicetomato", "goldencarrotjuice", "juicecarrot", "cactuswater",
			"maplesap", "maplesyrup", "peanutbutter", "rainbowsap",
			"juicecherry", "cherrysmoothie", "juicepineapple", "winepineapple", "pineapplesmoothie",
			"currantjuice", "currantsmoothie", "juiceplum", "plumsmoothie",
			"juicepeach", "peachsmoothie", "juiceelderberry", "elderberrysmoothie",
			"juicegrapefruit", "grapefruitsmoothie", "juicelime", "limesmoothie",
			"juiceorange", "orangesmoothie", "juiceapricot", "apricotsmoothie",
			"juicepear", "pearsmoothie", "pumpkinjuice",
			"persimmonjuice", "persimmonsmoothie", "starfruitjuice", "starfruitsmoothie",
			"figjuice", "figsmoothie", "pomegranatejuice", "pomegranatesmoothie",
			"mangojuice", "mangosmoothie", "papayajuice", "papayasmoothie",
			"coconutmilk", "coconutcream", "coconutsmoothie", "beetjuice",
			// medicine :342-:343, ketchup (the landed seat between 30001 and 32000)
			"medicine.heal", "medicine.laxative", "food_ketchup",
			// ink/indigo :345-:349, tail :357-:408
			"bottle_ink", "bottle_indigo",
			"bottle_poison", "bottle_loot", "bottle_tar", "bottle_blood", "bottle_lubricant", "bottle_mercury",
			"bottle_glue"};

	/** The batch table = 96 rows, the whole table = 171 (the families-a 75 + this batch). */
	@Test
	public void theBatchTableIsExactly96Rows() {
		assertEquals(96, GT6Bottles.FAMILY_B_ROWS.size(), "the families-B batch");
		assertEquals(171, GT6Bottles.ROWS.size(), "75 families-a + 96 families-B");
		assertEquals(THE_ORDER.length, GT6Bottles.ROWS.size(), "the census array covers every row");
	}

	/** The full order pin: ROWS cannot reorder or drift one id (the landed seats included). */
	@Test
	public void rowsRideTheUpstreamMetaOrder() {
		for (int i = 0; i < THE_ORDER.length; i++) {
			assertEquals(THE_ORDER[i], GT6Bottles.ROWS.get(i).id(), "row " + i + " drifts from the census order");
			assertEquals("gt6:" + THE_ORDER[i], GT6Bottles.BOTTLES.get(i).getId().toString(), "holder " + i + " mirrors the row");
		}
	}

	/**
	 * The kitchen + smoothie segment census over the meta bands (the state
	 * research.bottles-census table, walked to the row list): the kitchen segment = 29
	 * rows over ROWS (sauces 6 with the landed BBQ, apples 4 — the census headline said 5
	 * but no 903/904 metas exist upstream, the row list is the authority — oils 8,
	 * mayo/dressing 2, milk 3 with the landed cream, soy 1, chocolate 2, maple 2, peanut
	 * 1); the smoothie walk band 2000-5705 = 70 rows over ROWS (the walk's ACTUAL output
	 * 69 + the landed ketchup).
	 */
	@Test
	public void theSegmentCensusPinsTheKitchenAndSmoothieBands() {
		assertEquals(6, countBand(800, 805), "the sauce family :106-:111 (805 landed)");
		assertEquals(4, countBand(900, 905), "the apple rows :114-:117");
		assertEquals(8, countBand(1000, 1007), "the cooking oils :120-:127");
		assertEquals(2, countBand(1020, 1021), "mayo/dressing :134-:135");
		assertEquals(3, countBand(1100, 1102), "the milk family :137-:141 (1101 landed)");
		assertEquals(1, countBand(1200, 1200), "soy milk :148-:149");
		assertEquals(2, countBand(1900, 1901), "the chocolate creams :194-:195");
		assertEquals(2, countBand(3500, 3501), "maple :265-:266");
		assertEquals(1, countBand(3601, 3601), "peanut butter :273");
		assertEquals(29, countBand(800, 1200) + countBand(1900, 1901) + countBand(3500, 3601),
				"the kitchen segment census total");
		// 73 = the walk's 69 rows + the landed ketchup + the kitchen maple/peanut rows that
		// live numerically inside the band
		assertEquals(73, countBand(2000, 5705), "the smoothie walk band");
		// 72 inside the batch = the walk's 69 rows + the kitchen maple/peanut rows that live
		// numerically inside the band
		assertEquals(72, GT6Bottles.FAMILY_B_ROWS.stream().filter(tRow -> tRow.meta() >= 2000 && tRow.meta() <= 5705).count(),
				"the walk band rows inside the batch");
		assertEquals(69, GT6Bottles.FAMILY_B_ROWS.stream()
						.filter(tRow -> tRow.meta() >= 2000 && tRow.meta() <= 5705
								&& tRow.meta() != 3500 && tRow.meta() != 3501 && tRow.meta() != 3601)
						.count(),
				"the walk's actual output");
		assertEquals(27, GT6Bottles.FAMILY_B_ROWS.size() - 69, "the kitchen rows inside the batch");
	}

	private static long countBand(int aFrom, int aTo) {
		return GT6Bottles.ROWS.stream().filter(tRow -> tRow.meta() >= aFrom && tRow.meta() <= aTo).count();
	}

	/**
	 * The six new TD.Creative.HIDDEN rows (the sauce trio :108-:110, the spoiled milk
	 * :141, the golden carrot juice :259, the rainbow sap :275): registered but
	 * tab-excluded; the tab face = exactly the 151 visible rows.
	 */
	@Test
	public void hiddenRowsRegisterButStayOutOfTheTab() {
		int[] tHiddenMetas = {802, 803, 804, 1102, 3200, 3700};
		for (int tMeta : tHiddenMetas) {
			GT6Bottles.BottleRow tRow = rowByMeta(tMeta);
			assertNotNull(tRow, "hidden meta " + tMeta + " missing from the table");
			assertTrue(tRow.hidden(), "meta " + tMeta + " must carry the HIDDEN flag");
			assertFalse(GT6Bottles.TAB_BOTTLES.stream().anyMatch(tHolder -> tHolder.getId().getPath().equals(tRow.id())),
					"hidden row " + tRow.id() + " must not display in the tab");
		}
		assertEquals(171 - 20, GT6Bottles.TAB_BOTTLES.size(), "tab face = the non-hidden rows (14 A + 6 B hidden)");
	}

	/** Every families-B meta sits below the 32000 split, so the batch stacks 16 across (:423-426). */
	@Test
	public void allBatchBRowsStack16() {
		for (GT6Bottles.BottleRow tRow : GT6Bottles.FAMILY_B_ROWS) {
			assertTrue(tRow.meta() < 32000, "families-B metas stay below the stack split: " + tRow.id());
			assertEquals(16, tRow.stackSize(), "the batch stack column: " + tRow.id());
		}
	}

	/**
	 * The 13 en desc rows verbatim (the registration-row tooltip column): the eight
	 * cooking oils + the coconut milk ride "Cooking Oil" (:120-:127/:336), the three
	 * slime bottles "Can be used as Glue too" (:204/:206/:208), the rainbow sap the
	 * friendship line (:275). The en-empty rows (the sauces, the apples) emit NO tooltip
	 * key (the GT6BakeFoods empty-desc ruling) — 19 desc rows over the whole table.
	 */
	@Test
	public void enDescRowsAreVerbatim() {
		for (int tMeta : new int[] {1000, 1001, 1002, 1003, 1004, 1005, 1006, 1007, 5600}) {
			assertEquals("Cooking Oil", rowByMeta(tMeta).enTooltip(), ":" + tMeta + " cooking oil");
		}
		for (int tMeta : new int[] {2200, 2201, 2202}) {
			assertEquals("Can be used as Glue too", rowByMeta(tMeta).enTooltip(), ":" + tMeta + " slime");
		}
		assertEquals("Friendship in a Bottle, definitely not blood of a Tree", rowByMeta(3700).enTooltip(), ":275 rainbow sap");
		assertEquals("", rowByMeta(800).enTooltip(), ":106 the sauce en tooltip is empty");
		assertNull(rowByMeta(800).tooltipKey(), "the en-empty rows emit no tooltip key");
		long tEmptyDesc = GT6Bottles.ROWS.stream().filter(tRow -> tRow.enTooltip().isEmpty()).count();
		assertEquals(171 - 19, tEmptyDesc, "19 desc rows over the whole table (6 A + 13 B)");
	}

	/**
	 * The 96 families-B borrows: every row's texture exists under item/bottle/ and its
	 * sha256 is grounded in the assets/README.md families-B ledger together with the
	 * upstream source path gt.multiitem.bottles/<meta>.png (the cmp byte-identity was run
	 * at borrow time, 96/96 — the ledger is the committed witness; the oil-family source
	 * note is recorded there).
	 */
	@Test
	public void familiesBBorrowsAreGroundedInTheLedger() throws Exception {
		String tReadme = Files.readString(mdkRoot().resolve("src/main/resources/assets/README.md"), StandardCharsets.UTF_8);
		int tCount = 0;
		for (GT6Bottles.BottleRow tRow : GT6Bottles.FAMILY_B_ROWS) {
			Path tPng = assetFile("textures/item/bottle/" + tRow.texture() + ".png");
			assertTrue(Files.isRegularFile(tPng), "the borrowed texture must exist: " + tPng);
			String tHex = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(tPng)));
			assertTrue(tReadme.contains("`" + tRow.texture() + ".png`"), tRow.texture() + ".png — filename absent from the ledger");
			assertTrue(tReadme.contains("`textures/items/gt.multiitem.bottles/" + tRow.meta() + ".png`"),
					tRow.texture() + ".png — the upstream meta path absent from the ledger");
			assertTrue(tReadme.contains(tHex), tRow.texture() + ".png — bytes hash to " + tHex + ", not grounded in the ledger");
			tCount++;
		}
		assertEquals(96, tCount, "the families-B borrow count");
		assertTrue(tReadme.contains("cmp-verified 2026-10-02,\n  96/96 byte-identical"), "the ledger cmp witness line");
		assertTrue(tReadme.contains("the census \"material prefix texture family\""), "the oil-family source note");
	}

	/** Spot model pins across the segments (the full walk is datagen-side, ROWS-driven). */
	@Test
	public void generatedModelsPinTheSpotRows() {
		assertEquals("minecraft:item/generated", generatedJson("assets/gt6/models/item/chillysauce.json").get("parent").getAsString());
		assertEquals("gt6:item/bottle/chillysauce", generatedJson("assets/gt6/models/item/chillysauce.json").getAsJsonObject("textures").get("layer0").getAsString());
		assertEquals("gt6:item/bottle/bottle_olive_oil",
				generatedJson("assets/gt6/models/item/bottle_olive_oil.json").getAsJsonObject("textures").get("layer0").getAsString());
		assertEquals("gt6:item/bottle/bottle_slime_green",
				generatedJson("assets/gt6/models/item/bottle_slime_green.json").getAsJsonObject("textures").get("layer0").getAsString());
		assertEquals("gt6:item/bottle/beetjuice",
				generatedJson("assets/gt6/models/item/beetjuice.json").getAsJsonObject("textures").get("layer0").getAsString());
	}

	/**
	 * The bilingual faces: en = the registration-row names verbatim, zh = the dump
	 * gt.multiitem.bottles faces verbatim (tmp/gregtech.lang). The seven dump-empty
	 * "Cooking Oil" descs (1002-1007, 5600) are the HAND-COMPOSED 食用油 faces (the 32765
	 * mercury canned-air precedent, the dump word root 热食用油 off
	 * gt.material.FryingOilHot tmp/gregtech.lang:4723); the en-empty sauce/apple rows
	 * carry NO tooltip key on either locale; the Nutella zh face IS the upstream dump
	 * 花生酱 quirk, verbatim.
	 */
	@Test
	public void langFacesCarryTheSegmentAnchorsOnBothLocales() {
		String[][] tPinned = {
				{"item.gt6.chillysauce", "Chili Sauce", "辣酱"},
				{"item.gt6.diablosauce_strong", "There is no Cow Sauce", "老干爹辣椒酱"},
				{"item.gt6.juiceapple", "Apple Juice", "苹果汁"},
				{"item.gt6.ciderapple", "Cider", "苹果酒"},
				{"item.gt6.bottle_olive_oil", "Olive Oil", "橄榄油"},
				{"item.gt6.bottle_olive_oil.tooltip", "Cooking Oil", "健康小帮手♂♂"},
				{"item.gt6.bottle_nut_oil", "Nut Oil", "坚果油"},
				{"item.gt6.bottle_nut_oil.tooltip", "Cooking Oil", "食用油"},
				{"item.gt6.bottle_whale_oil.tooltip", "Cooking Oil", "食用油"},
				{"item.gt6.mayo", "Mayo", "蛋黄酱"},
				{"item.gt6.bottle_milk", "Milk", "牛奶"},
				{"item.gt6.spoiledmilk", "Milk", "牛奶"},
				{"item.gt6.soymilk", "Soy Milk", "豆浆"},
				{"item.gt6.nutella", "Nutella", "花生酱"},
				{"item.gt6.bottle_slime_green", "Green Slime Bottle", "绿色粘液瓶"},
				{"item.gt6.bottle_slime_green.tooltip", "Can be used as Glue too", "可以用来做胶水"},
				{"item.gt6.bawls", "BAWLS", "瓜拿纳"},
				{"item.gt6.cactuswater", "Cactus Water", "仙人掌"},
				{"item.gt6.rainbowsap", "Rainbow Sap", "彩虹汁"},
				{"item.gt6.rainbowsap.tooltip", "Friendship in a Bottle, definitely not blood of a Tree", "一瓶基情, 很明显不是一棵树的血"},
				{"item.gt6.juicepineapple", "Ananas Juice", "菠萝汁"},
				{"item.gt6.winepineapple", "Ananas Cider", "菠萝苹果酒"},
				{"item.gt6.coconutmilk", "Coconut Milk", "椰奶"},
				{"item.gt6.coconutmilk.tooltip", "Cooking Oil", "食用油"},
				{"item.gt6.beetjuice", "Beet Juice", "甜菜汁"}};
		for (String[] tRow : tPinned) {
			assertEquals(tRow[1], langValue("en_us", tRow[0]), tRow[0] + " en face");
			assertEquals(tRow[2], langValue("zh_cn", tRow[0]), tRow[0] + " zh face");
		}
		// the en-empty descs emit NO key on either locale (the mnwtr precedent) — the sauce
		// dump jokes ([Missing No] among them) and the apple lines stay keyless
		for (String tKey : new String[] {"item.gt6.chillysauce.tooltip", "item.gt6.diablosauce_strong.tooltip",
				"item.gt6.juiceapple.tooltip", "item.gt6.applevinegar.tooltip"}) {
			assertFalse(generatedJson("assets/gt6/lang/en_us.json").has(tKey), tKey + " must not exist (en-empty desc)");
			assertFalse(generatedJson("assets/gt6/lang/zh_cn.json").has(tKey), tKey + " must not exist (en-empty desc)");
		}
	}

	private static GT6Bottles.BottleRow rowByMeta(int aMeta) {
		return GT6Bottles.ROWS.stream().filter(tRow -> tRow.meta() == aMeta).findFirst().orElse(null);
	}

	/** The mdk root (src/main/resources/assets/README.md), walked upward from the leg-dependent test working dir. */
	private static Path mdkRoot() {
		for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
			if (Files.isRegularFile(p.resolve("src/main/resources/assets/README.md"))) return p;
		}
		throw new AssertionError("mdk root (src/main/resources/assets/README.md) not found upward from "
				+ Path.of("").toAbsolutePath());
	}

	/** A main-tree texture file under assets/gt6/ (texture reads never ride the leg classloader). */
	private static Path assetFile(String aPathUnderAssets) {
		return mdkRoot().resolve(Path.of("src", "main", "resources", "assets", "gt6")).resolve(aPathUnderAssets);
	}

	/** One committed generated-tree JSON as an object (the test classpath carries src/generated/resources). */
	private static JsonObject generatedJson(String aPath) {
		try (InputStream tStream = GT6BottlesFamiliesBTest.class.getClassLoader().getResourceAsStream(aPath)) {
			assertTrue(tStream != null, "the generated JSON must be on the classpath: " + aPath);
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (Exception aE) {
			throw new AssertionError(aE);
		}
	}

	/** One lang value off the generated-tree lang JSON. */
	private static String langValue(String aLocale, String aKey) {
		JsonObject tLang = generatedJson("assets/gt6/lang/" + aLocale + ".json");
		assertTrue(tLang.has(aKey), aKey + " missing from " + aLocale);
		return tLang.get(aKey).getAsString();
	}
}
