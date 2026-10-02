/*
 * Offline census-ratchet tests for task btl-dye-bottles: the dye batch of the
 * MultiItemBottles domain (48 new rows over the families 171 = 219, MultiItemBottles
 * .java:351-355). Pins the batch census (three 16-colour families, the vanilla dye
 * order 0=Black..15=White), the zero-collision table pin, the 48/48 fluid-id mapping
 * against the baseline GTFluids dye families, the stack-64 boundary face, the 48 sha256
 * grounded borrows and the bilingual lang faces — the census is composed from the
 * GTFluids registration seams (DYE_WATERMIXED/DYE_FLOWER/dyeChemicalName) so a bottle
 * id that drifts from its content fluid fails here.
 *
 * <p>The GT6BottlesFamiliesBTest posture verbatim: no item construction offline (the
 * intrusive-holder wall), assertion surface = pure registry-wiring data + on-disk assets.
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
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.fluid.GTFluids;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

public class GT6BottlesDyeTest {

	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
	}

	private static final String[] THE_FAMILY_IDS = {"watermixed", "chemical", "flower"};
	private static final int[] THE_FAMILY_BASES = {32100, 32116, 32132};

	/** The batch table = 48 rows, the whole table = 219 (171 + this batch). */
	@Test
	public void theBatchTableIsExactly48Rows() {
		assertEquals(48, GT6Bottles.FAMILY_DYE_ROWS.size(), "the dye batch");
		assertEquals(219, GT6Bottles.ROWS.size(), "171 families + 48 dye");
	}

	/**
	 * The three-family segment census (MultiItemBottles.java:352/:353/:354): 16 rows per
	 * family over the metas 32100-32115 / 32116-32131 / 32132-32147, in the vanilla dye
	 * order 0=Black..15=White inside each family, and the whole band sits in ROWS between
	 * indigo 32001 and the tail 32760 (contiguous, no interleave).
	 */
	@Test
	public void theSegmentCensusPinsTheThreeFamilies() {
		for (int tFamily = 0; tFamily < 3; tFamily++) {
			int tBase = THE_FAMILY_BASES[tFamily];
			assertEquals(16, countBand(tBase, tBase + 15), "family " + THE_FAMILY_IDS[tFamily] + " = 16 colours");
			for (int i = 0; i < 16; i++) {
				GT6Bottles.BottleRow tRow = rowByMeta(tBase + i);
				assertNotNull(tRow, "meta " + (tBase + i) + " missing");
				assertTrue(tRow.id().startsWith("dye_" + THE_FAMILY_IDS[tFamily] + "_"),
						tRow.id() + " rides the " + THE_FAMILY_IDS[tFamily] + " family");
			}
		}
		assertEquals(48, countBand(32100, 32147), "the dye band total");
		// contiguity: the dye rows are exactly ROWS positions 164..211 (bottle_indigo at 163, bottle_poison at 212)
		assertEquals("bottle_indigo", GT6Bottles.ROWS.get(163).id(), "indigo directly precedes the dye walk");
		assertEquals("bottle_poison", GT6Bottles.ROWS.get(212).id(), "the tail directly follows the dye walk");
		assertEquals("dye_watermixed_black", GT6Bottles.ROWS.get(164).id(), "the dye walk starts at 164");
		assertEquals("dye_flower_white", GT6Bottles.ROWS.get(211).id(), "the dye walk ends at 211");
	}

	private static long countBand(int aFrom, int aTo) {
		return GT6Bottles.ROWS.stream().filter(tRow -> tRow.meta() >= aFrom && tRow.meta() <= aTo).count();
	}

	/**
	 * The zero-collision pin: all 219 ROWS ids are distinct and all 219 registered holder
	 * ids match the table (the families-a/b id conventions never coined a {@code dye_}
	 * prefix, and this batch is the only one that does).
	 */
	@Test
	public void theTableHasZeroIdCollisions() {
		Set<String> tRowIds = new HashSet<>();
		Set<String> tHolderIds = new HashSet<>();
		for (GT6Bottles.BottleRow tRow : GT6Bottles.ROWS) {
			assertTrue(tRowIds.add(tRow.id()), "duplicate row id: " + tRow.id());
		}
		for (var tHolder : GT6Bottles.BOTTLES) {
			assertTrue(tHolderIds.add(tHolder.getId().getPath()), "duplicate holder id: " + tHolder.getId());
		}
		assertEquals(219, tRowIds.size(), "the table ids");
		assertEquals(tRowIds, tHolderIds, "the holder ids mirror the table");
	}

	/**
	 * The 48/48 fluid-mapping pin: every bottle id IS the baseline-registered content
	 * fluid id (the convention (3) rule, structural via the shared GTSprayCanItem.DYE_IDS
	 * compose) — watermixed against {@link GTFluids#DYE_WATERMIXED}, chemical against
	 * {@link GTFluids#dyeChemicalName}, flower against {@link GTFluids#DYE_FLOWER}.
	 */
	@Test
	public void everyDyeBottleRidesItsBaselineFluidId() {
		assertEquals(16, GTFluids.DYE_WATERMIXED.size(), "the prereq watermixed family");
		assertEquals(16, GTFluids.DYE_FLOWER.size(), "the prereq flower family");
		for (int i = 0; i < 16; i++) {
			assertEquals(GTFluids.DYE_WATERMIXED.get(i).name(), rowByMeta(32100 + i).id(),
					":352 meta " + (32100 + i) + " rides the watermixed fluid");
			assertEquals(GTFluids.dyeChemicalName(i), rowByMeta(32116 + i).id(),
					":353 meta " + (32116 + i) + " rides the chemical fluid");
			assertEquals(GTFluids.DYE_FLOWER.get(i).name(), rowByMeta(32132 + i).id(),
					":354 meta " + (32132 + i) + " rides the flower fluid");
		}
	}

	/**
	 * The HIDDEN/stack boundary face: the :351-355 walk rows carry no TD.Creative.HIDDEN
	 * (all 48 display, the tab face grows 151 -> 199) and all 48 sit on the meta
	 * {@code >= 32000} stack-64 side (:423-426).
	 */
	@Test
	public void dyeRowsDisplayAndStack64() {
		for (GT6Bottles.BottleRow tRow : GT6Bottles.FAMILY_DYE_ROWS) {
			assertFalse(tRow.hidden(), "the dye walk carries no HIDDEN: " + tRow.id());
			assertEquals(64, tRow.stackSize(), "the meta >= 32000 stack side: " + tRow.id());
		}
		assertEquals(219 - 20, GT6Bottles.TAB_BOTTLES.size(), "tab face = the non-hidden rows (14 A + 6 B hidden)");
	}

	/**
	 * The 48 en desc rows are the {@code "Color: " + DYE_NAMES[i]} composes verbatim
	 * (:352-:354), all non-empty — the empty-desc census grows 19 -> 67 desc rows over
	 * the whole table (219 - 67 empty).
	 */
	@Test
	public void enDescRowsAreTheColorComposes() {
		assertEquals("Color: Black", rowByMeta(32100).enTooltip(), ":352 watermixed black");
		assertEquals("Color: White", rowByMeta(32115).enTooltip(), ":352 watermixed white");
		assertEquals("Color: Light Gray", rowByMeta(32123).enTooltip(), ":353 chemical light gray (DYE_NAMES spacing)");
		assertEquals("Color: White", rowByMeta(32147).enTooltip(), ":354 flower white");
		for (int tBase : THE_FAMILY_BASES) {
			for (int i = 0; i < 16; i++) {
				GT6Bottles.BottleRow tRow = rowByMeta(tBase + i);
				assertNotNull(tRow.tooltipKey(), tRow.id() + " carries a tooltip key");
				assertTrue(tRow.enTooltip().startsWith("Color: "), tRow.id() + " tooltip compose");
			}
		}
		long tEmptyDesc = GT6Bottles.ROWS.stream().filter(tRow -> tRow.enTooltip().isEmpty()).count();
		assertEquals(219 - 67, tEmptyDesc, "67 desc rows over the whole table (6 A + 13 B + 48 dye)");
	}

	/**
	 * The 48 dye borrows: every row's texture exists under item/bottle/ and its sha256 is
	 * grounded in the assets/README.md dye-batch ledger together with the upstream source
	 * path gt.multiitem.bottles/<meta>.png (the cmp byte-identity was run at borrow time,
	 * 48/48 — the ledger is the committed witness).
	 */
	@Test
	public void dyeBorrowsAreGroundedInTheLedger() throws Exception {
		String tReadme = Files.readString(mdkRoot().resolve("src/main/resources/assets/README.md"), StandardCharsets.UTF_8);
		int tCount = 0;
		for (GT6Bottles.BottleRow tRow : GT6Bottles.FAMILY_DYE_ROWS) {
			Path tPng = assetFile("textures/item/bottle/" + tRow.texture() + ".png");
			assertTrue(Files.isRegularFile(tPng), "the borrowed texture must exist: " + tPng);
			String tHex = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(tPng)));
			assertTrue(tReadme.contains("`" + tRow.texture() + ".png`"), tRow.texture() + ".png — filename absent from the ledger");
			assertTrue(tReadme.contains("`textures/items/gt.multiitem.bottles/" + tRow.meta() + ".png`"),
					tRow.texture() + ".png — the upstream meta path absent from the ledger");
			assertTrue(tReadme.contains(tHex), tRow.texture() + ".png — bytes hash to " + tHex + ", not grounded in the ledger");
			tCount++;
		}
		assertEquals(48, tCount, "the dye borrow count");
		assertTrue(tReadme.contains("cmp-verified 2026-10-02, 48/48 byte-identical"), "the ledger cmp witness line");
	}

	/** Spot model pins (the full walk is datagen-side, ROWS-driven). */
	@Test
	public void generatedModelsPinTheSpotRows() {
		assertEquals("gt6:item/bottle/dye_watermixed_black",
				generatedJson("assets/gt6/models/item/dye_watermixed_black.json").getAsJsonObject("textures").get("layer0").getAsString());
		assertEquals("gt6:item/bottle/dye_chemical_light_gray",
				generatedJson("assets/gt6/models/item/dye_chemical_light_gray.json").getAsJsonObject("textures").get("layer0").getAsString());
		assertEquals("gt6:item/bottle/dye_flower_white",
				generatedJson("assets/gt6/models/item/dye_flower_white.json").getAsJsonObject("textures").get("layer0").getAsString());
	}

	/**
	 * The bilingual faces: en = the :351-355 name/tooltip composes verbatim, zh = the dump
	 * gt.multiitem.bottles faces verbatim (tmp/gregtech.lang:7080-7175, 罐装水性/化学/植物染料
	 * + 颜色: <色名>).
	 */
	@Test
	public void langFacesCarryTheDyeAnchorsOnBothLocales() {
		String[][] tPinned = {
				{"item.gt6.dye_watermixed_black", "Bottled Water Dye", "Color: Black", "罐装水性染料", "颜色: 黑色"},
				{"item.gt6.dye_watermixed_light_gray", "Bottled Water Dye", "Color: Light Gray", "罐装水性染料", "颜色: 淡灰色"},
				{"item.gt6.dye_chemical_black", "Bottled Chemical Dye", "Color: Black", "罐装化学染料", "颜色: 黑色"},
				{"item.gt6.dye_chemical_light_gray", "Bottled Chemical Dye", "Color: Light Gray", "罐装化学染料", "颜色: 淡灰色"},
				{"item.gt6.dye_flower_black", "Bottled Flower Dye", "Color: Black", "罐装植物染料", "颜色: 黑色"},
				{"item.gt6.dye_flower_white", "Bottled Flower Dye", "Color: White", "罐装植物染料", "颜色: 白色"}};
		for (String[] tRow : tPinned) {
			assertEquals(tRow[1], langValue("en_us", tRow[0]), tRow[0] + " en name");
			assertEquals(tRow[2], langValue("en_us", tRow[0] + ".tooltip"), tRow[0] + " en tooltip");
			assertEquals(tRow[3], langValue("zh_cn", tRow[0]), tRow[0] + " zh name");
			assertEquals(tRow[4], langValue("zh_cn", tRow[0] + ".tooltip"), tRow[0] + " zh tooltip");
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
		try (InputStream tStream = GT6BottlesDyeTest.class.getClassLoader().getResourceAsStream(aPath)) {
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
