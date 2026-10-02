/*
 * Offline census-ratchet tests for task btl-bottles-families-a: the families-A batch of
 * the MultiItemBottles domain (71 new rows over the food-bottles-min 4 = 75). Pins the
 * full 75-row order, the segment census (water 9 / alcohol 38 / honey-potion 15 / tail 9
 * + the 4 landed), the TD.Creative.HIDDEN set (registered, tab-excluded), the tab icon
 * meta 1600, the stackLimit column split (:423-426), the 71 sha256-grounded borrows and
 * the bilingual lang faces — the census is hardcoded here INDEPENDENT of the
 * GT6Bottles.ROWS table so the pin cannot drift with the source.
 *
 * <p>The GT6BottlesTest posture verbatim: no item construction offline (the intrusive-
 * holder wall), assertion surface = pure registry-wiring data + on-disk assets.
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

public class GT6BottlesFamiliesATest {

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
	}

	/** The full 75-row order (the empty domain-entry item first, then upstream metas ascending). */
	private static final String[] THE_ORDER = {
			"bottle_empty",
			// the water family, MultiItemBottles.java:45-:53 (6/7/8 HIDDEN)
			"mnwtr", "seawater", "soda", "mineralsoda", "ice", "waterdirty", "swampwater", "stagnantwater", "spdew",
			// the grape family, :56-:68 (13)
			"juicewhitegrape", "winewhite", "vinegar", "juiceredgrape", "winered", "whitegrapesmoothie",
			"redgrapesmoothie", "grapesmoothie", "purplegrapesmoothie", "grapejuice", "grc_grapewine0", "wine",
			"ricardosanchez",
			// lemon :71-:76 (6), potato :79-:81 (3)
			"juicelemon", "lemonade", "limoncello", "alcopops", "cavejohnsonsgrenadejuice", "lemonsmoothie",
			"potatojuice", "vodka", "leninade",
			// reed :84-:88 (5), hops :91-:93 (3), wheat :96-:98 (3), beer :101/:103 (2)
			"reedwater", "rumwhite", "rumdark", "canevinegar", "pina_colada",
			"hopsmash", "darkbeer", "dragonblood",
			"mashwheat", "whiskeywheat", "glenmckenner",
			"wheathopsmash", "beer",
			// the two landed kitchen rows (food-bottles-min), metas 805/1101
			"food_barbecuesauce", "food_heavycream",
			// honey :156-:162 (6), potion :176-:180 (4, HIDDEN), purple/rotten :182-:185, holy :187-:188
			"food_honeydrop", "bottle_honeydew", "royal_jelly", "ambrosia", "short_mead", "mead",
			"potion.goldenapplejuice", "potion.goldencider", "potion.idunsapplejuice", "potion.notchesbrew",
			"purpledrink", "rottendrink", "bottle_holy_water",
			// rice :190-:192 (3), medicine :342-:343 (2), ketchup :257 (the landed row, meta 3101)
			"ricewater", "sake", "ricevinegar",
			"medicine.heal", "medicine.laxative",
			"food_ketchup",
			// ink/indigo :345-:349, tail :357-:408
			"bottle_ink", "bottle_indigo",
			"bottle_poison", "bottle_loot", "bottle_tar", "bottle_blood", "bottle_lubricant", "bottle_mercury",
			"bottle_glue"};

	/**
	 * The census segment counts over the meta bands (the state research.bottles-census
	 * table). Task btl-bottles-families-b ratchet: the table grew 75 -> 171 (the 96
	 * families-B rows interleave), so the A pin rides as a subsequence (see the order
	 * test) and the counts here are position-independent.
	 */
	@Test
	public void segmentCensusMatchesTheBatchPlan() {
		// task btl-dye-bottles ratchet: 219 rows (4 landed + 71 A + 96 B + 48 dye)
		assertEquals(219, GT6Bottles.ROWS.size(), "4 landed + 71 families-A + 96 families-B + 48 dye");
		assertEquals(9, countBand(0, 8), "water family :45-:53");
		assertEquals(38, countBand(100, 701) + countBand(1800, 1802), "alcohol families 38 bottles");
		assertEquals(15, countBand(1300, 1700) + countBand(30000, 30001), "honey/potion segment 15");
		assertEquals(2, countBand(32000, 32001), "ink/indigo");
		assertEquals(7, countBand(32760, 32766), "tail 32760-32766");
		assertEquals(1, countBand(-1, -1), "the empty domain-entry item");
		assertEquals(3, countBand(805, 805) + countBand(1101, 1101) + countBand(3101, 3101), "the landed BBQ + cream + ketchup rows");
	}

	private static long countBand(int aFrom, int aTo) {
		return GT6Bottles.ROWS.stream().filter(tRow -> tRow.meta() >= aFrom && tRow.meta() <= aTo).count();
	}

	/** The full order pin: the families-A ids keep their relative order inside ROWS (the families-B rows interleave, the families-B test pins the full 171 sequence). */
	@Test
	public void rowsRideTheUpstreamMetaOrder() {
		int tCursor = 0;
		for (String tId : THE_ORDER) {
			while (tCursor < GT6Bottles.ROWS.size() && !GT6Bottles.ROWS.get(tCursor).id().equals(tId)) tCursor++;
			assertTrue(tCursor < GT6Bottles.ROWS.size(), "families-A id " + tId + " lost from ROWS");
			assertEquals("gt6:" + tId, GT6Bottles.BOTTLES.get(tCursor).getId().toString(), "holder mirrors the row at " + tCursor);
			tCursor++;
		}
	}

	/** The ids are unique (the record convention never collides across the 71 + 4 rows). */
	@Test
	public void rowIdsAreUnique() {
		assertEquals(GT6Bottles.ROWS.size(), GT6Bottles.ROWS.stream().map(GT6Bottles.BottleRow::id).distinct().count(),
				"no duplicate bottle ids");
	}

	/**
	 * The TD.Creative.HIDDEN set (upstream verbatim): water 6/7/8, alcopops 203, grenade
	 * juice 204, leninade 302, pirate brew 402, dragon blood 502, glen mckenner 602, the
	 * four potion bottles, rotten drink 1601 — registered but tab-excluded.
	 */
	@Test
	public void hiddenRowsRegisterButStayOutOfTheTab() {
		int[] tHiddenMetas = {6, 7, 8, 203, 204, 302, 402, 502, 602, 1400, 1401, 1500, 1501, 1601};
		for (int tMeta : tHiddenMetas) {
			GT6Bottles.BottleRow tRow = rowByMeta(tMeta);
			assertNotNull(tRow, "hidden meta " + tMeta + " missing from the table");
			assertTrue(tRow.hidden(), "meta " + tMeta + " must carry the HIDDEN flag");
			assertFalse(GT6Bottles.TAB_BOTTLES.stream().anyMatch(tHolder -> tHolder.getId().getPath().equals(tRow.id())),
					"hidden row " + tRow.id() + " must not display in the tab");
		}
		java.util.Set<String> tVisible = new java.util.HashSet<>();
		for (GT6Bottles.BottleRow tRow : GT6Bottles.ROWS) {
			if (!tRow.hidden()) tVisible.add(tRow.id());
		}
		java.util.Set<String> tTab = new java.util.HashSet<>();
		GT6Bottles.TAB_BOTTLES.forEach(tHolder -> tTab.add(tHolder.getId().getPath()));
		// task btl-dye-bottles ratchet: 219 rows, 20 hidden (14 A + 6 B, none dye)
		assertEquals(219 - 20, GT6Bottles.TAB_BOTTLES.size(), "tab face = the non-hidden rows");
		assertEquals(tVisible, tTab, "the tab face is EXACTLY the non-hidden rows, no swap");
	}

	/** The tab icon = meta 1600 "Purple Drink" (MultiItemBottles.java:40, the reclaimed deviation). */
	@Test
	public void tabIconIsTheMeta1600PurpleDrink() {
		GT6Bottles.BottleRow tIcon = rowById(GT6Bottles.TAB_ICON_ID);
		assertEquals(1600, tIcon.meta(), "the icon row is meta 1600");
		assertEquals("Purple Drink", tIcon.enName(), "the icon row is the upstream verbatim Purple Drink");
		assertFalse(tIcon.hidden(), "the icon row displays");
	}

	/**
	 * The stack column = the getDefaultStackLimit split (MultiItemBottles.java:423-426):
	 * meta >= 32000 stacks 64, everything else OP.bottle.mDefaultStackSize 16 (OP.java:229
	 * .setStacksize(16)). Boundary pins: 30000/30001 (medicine) ride the 16 side, 32000/
	 * 32001 (ink/indigo) start the 64 side — meta 32100+ (the dye bottles, the dye card)
	 * inherits the same rule, so the column contract is pinned at the rows this card owns.
	 */
	@Test
	public void stackColumnFollowsThe32000Split() {
		for (GT6Bottles.BottleRow tRow : GT6Bottles.ROWS) {
			assertEquals(tRow.meta() >= 32000 ? 64 : 16, tRow.stackSize(), "stack split violation at " + tRow.id());
		}
		assertEquals(16, rowByMeta(30000).stackSize(), "medicine meta 30000 rides the 16 side");
		assertEquals(16, rowByMeta(30001).stackSize(), "medicine meta 30001 rides the 16 side");
		assertEquals(64, rowByMeta(32000).stackSize(), "ink meta 32000 starts the 64 side");
		assertEquals(64, rowByMeta(32001).stackSize(), "indigo meta 32001 rides the 64 side");
	}

	/** The six en desc rows verbatim (the registration-row tooltip column, MultiItemBottles). */
	@Test
	public void enDescRowsAreVerbatim() {
		assertEquals("Why does this Bottle look like a Bear and not a Bee?", rowByMeta(1300).enTooltip(), ":157 honey");
		assertEquals("Color: Black", rowByMeta(32000).enTooltip(), ":346 ink");
		assertEquals("Color: Blue", rowByMeta(32001).enTooltip(), ":349 indigo");
		assertEquals("Loot: A random Bottle", rowByMeta(32761).enTooltip(), ":368 loot");
		assertEquals("Can be used as Glue too", rowByMeta(32762).enTooltip(), ":371 tar");
		assertEquals("Also called Quicksilver", rowByMeta(32765).enTooltip(), ":399 mercury");
		long tEmptyDesc = GT6Bottles.ROWS.stream().filter(tRow -> tRow.enTooltip().isEmpty()).count();
		// task btl-dye-bottles ratchet: 219 rows, 67 descs (6 A + 13 B + 48 dye)
		assertEquals(219 - 67, tEmptyDesc, "exactly sixty-seven rows carry a desc");
	}

	/**
	 * The 71 families-A borrows: every row's texture exists under item/bottle/ and its
	 * sha256 is grounded in the assets/README.md families-A ledger together with the
	 * upstream source path gt.multiitem.bottles/<meta>.png (the cmp byte-identity was run
	 * at borrow time, 71/71 — the ledger is the committed witness).
	 */
	@Test
	public void familiesABorrowsAreGroundedInTheLedger() throws Exception {
		String tReadme = Files.readString(mdkRoot().resolve("src/main/resources/assets/README.md"), StandardCharsets.UTF_8);
		int tCount = 0;
		for (GT6Bottles.BottleRow tRow : GT6Bottles.FAMILY_A_ROWS) {
			Path tPng = assetFile("textures/item/bottle/" + tRow.texture() + ".png");
			assertTrue(Files.isRegularFile(tPng), "the borrowed texture must exist: " + tPng);
			String tHex = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(tPng)));
			assertTrue(tReadme.contains("`" + tRow.texture() + ".png`"), tRow.texture() + ".png — filename absent from the ledger");
			assertTrue(tReadme.contains("`textures/items/gt.multiitem.bottles/" + tRow.meta() + ".png`"),
					tRow.texture() + ".png — the upstream meta path absent from the ledger");
			assertTrue(tReadme.contains(tHex), tRow.texture() + ".png — bytes hash to " + tHex + ", not grounded in the ledger");
			tCount++;
		}
		assertEquals(71, tCount, "the families-A borrow count");
		assertTrue(tReadme.contains("cmp-verified 2026-10-02,\n  71/71 byte-identical"), "the ledger cmp witness line");
	}

	/** Spot model pins across the four segments (the full walk is datagen-side, ROWS-driven). */
	@Test
	public void generatedModelsPinTheSpotRows() {
		assertEquals("minecraft:item/generated", generatedJson("assets/gt6/models/item/mnwtr.json").get("parent").getAsString());
		assertEquals("gt6:item/bottle/mnwtr", generatedJson("assets/gt6/models/item/mnwtr.json").getAsJsonObject("textures").get("layer0").getAsString());
		assertEquals("gt6:item/bottle/potion.goldenapplejuice",
				generatedJson("assets/gt6/models/item/potion.goldenapplejuice.json").getAsJsonObject("textures").get("layer0").getAsString());
		assertEquals("gt6:item/bottle/honeydrop",
				generatedJson("assets/gt6/models/item/food_honeydrop.json").getAsJsonObject("textures").get("layer0").getAsString(),
				"the food_ prefix strips in the texture name (the ketchup.png precedent)");
		assertEquals("gt6:item/bottle/bottle_tar",
				generatedJson("assets/gt6/models/item/bottle_tar.json").getAsJsonObject("textures").get("layer0").getAsString());
	}

	/**
	 * The bilingual faces: en = the registration-row names verbatim, zh = the dump
	 * gt.multiitem.bottles faces verbatim (tmp/gregtech.lang). The mercury desc keeps NO
	 * zh key (the dump face is the empty string — en falls back at runtime, the upstream
	 * zh shape); the poison/loot zh faces ARE the untranslated English (dump verbatim).
	 */
	@Test
	public void langFacesCarryTheSegmentAnchorsOnBothLocales() {
		String[][] tPinned = {
				{"item.gt6.mnwtr", "Mineral Water", "矿泉水"},
				{"item.gt6.cavejohnsonsgrenadejuice", "Cave Johnson's Grenade Juice", "凯夫·约翰逊的炸弹柠檬汁"},
				{"item.gt6.pina_colada", "Piña Colada", "凤梨可乐达"},
				{"item.gt6.food_honeydrop", "Honey", "蜂蜜"},
				{"item.gt6.potion.notchesbrew", "Notches Brew", "Notch汤"},
				{"item.gt6.medicine.laxative", "Laxative", "泻药"},
				{"item.gt6.bottle_ink", "Ink Bottle", "墨水瓶"},
				{"item.gt6.bottle_indigo", "Bottled Indigo Dye", "罐装靛蓝色染料"},
				{"item.gt6.bottle_tar", "Tar Bottle", "焦油瓶"},
				{"item.gt6.bottle_poison", "Bottle of Poison", "Bottle of Poison"},
				{"item.gt6.food_honeydrop.tooltip", "Why does this Bottle look like a Bear and not a Bee?", "为什么这个罐子看起来像熊而不是蜜蜂?"},
				{"item.gt6.bottle_ink.tooltip", "Color: Black", "颜色: 黑色"},
				{"item.gt6.bottle_tar.tooltip", "Can be used as Glue too", "也可以用作胶水"}};
		for (String[] tRow : tPinned) {
			assertEquals(tRow[1], langValue("en_us", tRow[0]), tRow[0] + " en face");
			assertEquals(tRow[2], langValue("zh_cn", tRow[0]), tRow[0] + " zh face");
		}
		assertEquals("Also called Quicksilver", langValue("en_us", "item.gt6.bottle_mercury.tooltip"), "mercury en desc");
		// the mercury zh desc = HAND-COMPOSED (the dump gt.multiitem.bottles.32765.tooltip
		// face is the empty string — no verbatim face to take; the canned-air trio precedent:
		// 又叫 + the dump word 水银(汞), gt.material.Quicksilver=水银(汞) tmp/gregtech.lang:5331;
		// the zh cardinality stays aligned with en, the zero-debt invariant)
		assertEquals("又叫水银(汞)", langValue("zh_cn", "item.gt6.bottle_mercury.tooltip"), "mercury zh desc hand-composed");
	}

	private static GT6Bottles.BottleRow rowByMeta(int aMeta) {
		return GT6Bottles.ROWS.stream().filter(tRow -> tRow.meta() == aMeta).findFirst().orElse(null);
	}

	private static GT6Bottles.BottleRow rowById(String aId) {
		return GT6Bottles.ROWS.stream().filter(tRow -> tRow.id().equals(aId)).findFirst().orElseThrow();
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
		try (InputStream tStream = GT6BottlesFamiliesATest.class.getClassLoader().getResourceAsStream(aPath)) {
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
