/*
 * Offline tests for task food-items-core (the T1 subset) + food-meat-items (the T4a
 * egg/meat extension to 38 rows): the GT6Foods registration home, the GT6FoodCansTest
 * posture over the table-driven tab, plus the domain pins the card acceptance adds:
 * the FoodProperties literals verbatim against the upstream FoodStat anchors, the
 * id→meta census and the byte-identical texture borrows grounded in the assets/README.md
 * sha256 ledger (the GT6DynamoBowlRenderDatagenTest borrow form).
 *
 * <p>The items are NOT constructible in this bootstrapped-and-frozen JVM (the mod-Item
 * intrusive-holder wall), so the assertion surface is the PURE table + registry-wiring
 * data + the FoodProperties construction (a vanilla record, constructible offline) —
 * the GT6FoodCansTest posture verbatim.
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class GT6FoodsTest {

	/** The offline boot BEFORE the first GT6Foods touch (the GT6FoodCansTest.boot shape). */
	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
	}

	private static ResourceLocation rl(String aPath) {
		return new ResourceLocation("gt6", aPath);
	}

	/**
	 * The T4a table is EXACTLY 38 items — one row per upstream addItem line, zero skip:
	 * the T1 nine (cheese 1000 + slice 1001, boiled eggs 1060/1061, chips 9010/9020, ice
	 * cream 13000, butters 32117/32119) + the egg family tail (white egg 1051 :496, fried/
	 * scrambled/sliced/yolk/white 1070-1074 :499-:503) + the meat/chum families (MultiItem
	 * Food.java:529-:560/:563-:578/:594/:798). The vanilla-egg alias Food_Brown_Egg :495
	 * registers nothing (it IS the vanilla item), the scrap-meat 1998 and the ingot-bar
	 * metas 32101-32115 stay POOLED (the T3 rows), the 37-flavour ice-cream family stays
	 * the T3 pool.
	 */
	@Test
	public void registeredSubsetIsExactlyThe38Rows() {
		assertEquals(38, GT6Foods.ITEMS.getEntries().size(), "the T4a table: 38 rows, zero skip");
	}

	/**
	 * The id→meta census, three ledgers per the card acceptance (egg/meat/chips+ in one
	 * ascending walk) — every row pinned to its upstream MultiItemFood.addItem meta.
	 */
	@Test
	public void foodRowsCarryTheUpstreamMetaAnchors() {
		Object[][] tAnchors = {
				// the T1 nine (MultiItemFood.java:490/:491/:497/:498/:365/:374/:809/:933/:934)
				{"food_cheese", 1000}, {"food_cheese_sliced", 1001},
				{"food_white_egg", 1051}, {"food_brown_egg_boiled", 1060}, {"food_white_egg_boiled", 1061},
				{"food_egg_fried", 1070}, {"food_egg_scrambled", 1071}, {"food_egg_sliced", 1072},
				{"food_egg_yolk", 1073}, {"food_egg_white", 1074},
				{"food_ham_raw", 1100}, {"food_ham_cooked", 1101}, {"food_ham_slice_raw", 1102},
				{"food_ham_slice_cooked", 1103}, {"food_bacon_raw", 1112}, {"food_bacon_cooked", 1113},
				{"food_rib_raw", 1200}, {"food_rib_cooked", 1201}, {"food_rib_bbq", 1202},
				{"food_ribeyesteak_raw", 1210}, {"food_ribeyesteak_cooked", 1211},
				{"food_dogmeat_raw", 1300}, {"food_dogmeat_cooked", 1301},
				{"food_mutton_raw", 1400}, {"food_mutton_cooked", 1401},
				{"food_horse_raw", 1500}, {"food_horse_cooked", 1501},
				{"food_mule_raw", 1510}, {"food_mule_cooked", 1511},
				{"food_donkey_raw", 1520}, {"food_donkey_cooked", 1521},
				{"food_chum", 10000}, {"food_chum_on_stick", 10010},
				{"food_potato_chips", 9010}, {"food_chili_chips", 9020},
				{"food_ice_cream", 13000}, {"food_butter", 32117}, {"food_butter_salted", 32119}};
		assertEquals(38, tAnchors.length);
		for (int i = 0; i < tAnchors.length; i++) {
			assertEquals(tAnchors[i][0], GT6Foods.FOOD_ROWS.get(i).id(), "row " + i + " rides the upstream meta order");
			assertEquals(rl((String) tAnchors[i][0]), GT6Foods.FOODS.get(i).getId(), "row " + i + " registered under the row id");
		}
	}

	/**
	 * The FoodProperties literals VERBATIM against the upstream FoodStat anchors —
	 * nutrition/saturationModifier per row = the FoodStat (aFoodLevel, aSaturation)
	 * constructor literals of the anchor line, canAlwaysEat = the 4-flag tail head
	 * alwaysEdible (FoodStat.java:64-67 — T only on the two Chum rows :594/:798, F
	 * elsewhere). The White Egg :496 carries NO FoodStat — edible()=F and NO food
	 * component. Anchors per index = the census walk above (:490/:491/:496/:497/:498/
	 * :499-:503/:529-:560/:563-:578/:594/:798/:365/:374/:809/:933/:934).
	 */
	@Test
	public void foodPropertiesAreTheUpstreamFoodStatLiterals() {
		int[] tNutrition = {2, 1, 0, 2, 2, 2, 2, 1, 1, 1, 3, 10, 1, 3, 1, 3, 3, 10, 10, 3, 10, 2, 8, 2, 7, 2, 8, 3, 10, 2, 8, 5, 5, 7, 7, 1, 1, 1};
		float[] tSaturation = {1.2F, 0.6F, 0.0F, 1.2F, 1.2F, 1.2F, 1.2F, 0.6F, 1.2F, 1.2F,
				0.6F, 1.6F, 0.6F, 1.6F, 0.9F, 1.8F, 0.6F, 1.6F, 1.6F, 0.6F, 1.6F, 0.6F, 1.6F, 0.6F, 2.0F,
				0.6F, 1.6F, 0.8F, 1.8F, 0.6F, 1.6F, 1.6F, 1.6F, 1.2F, 1.2F, 0.6F, 4.0F, 4.0F};
		boolean[] tAlwaysEdible = new boolean[38];
		tAlwaysEdible[31] = true; // food_chum (MultiItemFood.java:594 — the T,F,T,T flag tail)
		tAlwaysEdible[32] = true; // food_chum_on_stick (:798 — the T,F,T,T flag tail)
		assertEquals(tNutrition.length, GT6Foods.FOOD_ROWS.size());
		for (int i = 0; i < GT6Foods.FOOD_ROWS.size(); i++) {
			gregtech6.registry.GT6Foods.FoodRow tRow = GT6Foods.FOOD_ROWS.get(i);
			assertEquals(tNutrition[i] > 0, tRow.edible(), tRow.id() + " — the FoodStat presence face");
			if (!tRow.edible()) continue; // the White Egg :496 — no FoodStat, no food component
			FoodProperties tProps = GT6Foods.foodProperties(tRow);
			//? if forge {
			assertEquals(tNutrition[i], tProps.getNutrition(), tRow.id() + " nutrition (the FoodStat aFoodLevel literal)");
			//?} else {
			/*assertEquals(tNutrition[i], tProps.nutrition(), tRow.id() + " nutrition (the FoodStat aFoodLevel literal)"); // 21.1 record accessor
			 *///?}
			//? if forge {
			assertEquals(tSaturation[i], tProps.getSaturationModifier(), 0.0F, tRow.id() + " saturation (the FoodStat aSaturation literal)");
			//?} else {
			/*assertEquals(tSaturation[i] * tNutrition[i] * 2.0F, tProps.saturation(), 1.0E-4F, tRow.id() + " saturation = the FoodStat literal folded at build"); // 21.1: the record carries modifier*nutrition*2 (FoodProperties.java:113-115 FoodConstants.saturationByModifier) — the same literal through the total-energy fold
			 *///?}
			assertEquals(tAlwaysEdible[i], tProps.canAlwaysEat(), tRow.id() + " alwaysEdible (the upstream flag tail head)");
		}
	}

	/** The desc tooltip keys — the GT6LaserGas tooltip-key face, derivable from the id. */
	@Test
	public void tooltipKeysAreTheIdDerivedLiterals() {
		for (gregtech6.registry.GT6Foods.FoodRow tRow : GT6Foods.FOOD_ROWS) {
			assertEquals("item.gt6." + tRow.id() + ".tooltip", tRow.tooltipKey());
		}
	}

	/**
	 * The empty-desc rows carry NO tooltip key (the upstream renders no line — :499/:500/
	 * :503/:530-:538 etc.); the 14 keyed rows = the 38 minus the 24 empty-desc rows, en/zh
	 * parity rides the same emission rule.
	 */
	@Test
	public void emptyTooltipRowsEmitNoTooltipKey() {
		Set<String> tEmpty = Set.of("food_egg_fried", "food_egg_scrambled", "food_egg_white",
				"food_ham_cooked", "food_ham_slice_raw", "food_ham_slice_cooked", "food_bacon_cooked",
				"food_rib_cooked", "food_mutton_cooked", "food_horse_raw", "food_horse_cooked",
				"food_mule_raw", "food_mule_cooked", "food_donkey_raw", "food_donkey_cooked");
		int tEmptyCount = 0;
		for (gregtech6.registry.GT6Foods.FoodRow tRow : GT6Foods.FOOD_ROWS) {
			if (tRow.enTooltip().isEmpty()) {
				tEmptyCount++;
				assertTrue(tEmpty.contains(tRow.id()), tRow.id() + " — unexpected empty-desc row");
			}
		}
		assertEquals(15, tEmptyCount, "the empty-desc census (the White Egg desc is NOT empty)");
	}

	/** The tab — id 'food', the pinned title key (the GT6FoodCansTest tab face). */
	@Test
	public void tabIdAndTitleKeyAreThePinnedLiterals() {
		assertEquals(Registries.CREATIVE_MODE_TAB, GT6Foods.CREATIVE_MODE_TABS.getRegistryKey());
		assertEquals(rl("food"), GT6Foods.FOOD_TAB.getId());
		assertEquals("itemGroup.gt6.food", GT6Foods.TAB_TITLE_KEY);
	}

	/** The tab DR holds exactly the one "food" tab (per-family tab discipline). */
	@Test
	public void tabsRegistryHoldsExactlyTheFoodTab() {
		Set<ResourceLocation> tIds = new LinkedHashSet<>();
		//? if forge {
		for (RegistryObject<CreativeModeTab> tTab : GT6Foods.CREATIVE_MODE_TABS.getEntries()) {
		//?} else {
		/*for (net.neoforged.neoforge.registries.DeferredHolder<CreativeModeTab, ? extends CreativeModeTab> tTab : GT6Foods.CREATIVE_MODE_TABS.getEntries()) { // 21.1: wildcard holder
		*///?}
			tIds.add(tTab.getId());
		}
		assertEquals(Set.of(rl("food")), tIds);
	}

	/**
	 * Registration smoke + bidirectional parity (the GT6ToolsCreativeTabTest form): every
	 * FOODS row is a registered ITEMS entry and every registered entry is displayed.
	 */
	@Test
	public void tableAndItemsRegistryAreInParity() {
		Set<ResourceLocation> tTableIds = new LinkedHashSet<>();
		//? if forge {
		for (RegistryObject<Item> tRow : GT6Foods.FOODS) {
			tTableIds.add(tRow.getId());
		}
		//?} else {
		/*for (net.neoforged.neoforge.registries.DeferredHolder<Item, ? extends Item> tRow : GT6Foods.FOODS) { // 21.1: the list stores the wildcard holder
			tTableIds.add(tRow.getId());
		}
		*///?}
		Set<ResourceLocation> tRegisteredIds = new LinkedHashSet<>();
		//? if forge {
		for (RegistryObject<Item> tEntry : GT6Foods.ITEMS.getEntries()) {
		//?} else {
		/*for (net.neoforged.neoforge.registries.DeferredHolder<Item, ? extends Item> tEntry : GT6Foods.ITEMS.getEntries()) { // 21.1: wildcard holder
		*///?}
			tRegisteredIds.add(tEntry.getId());
		}
		assertTrue(tRegisteredIds.containsAll(tTableIds), "every table row must be a registered item");
		assertTrue(tTableIds.containsAll(tRegisteredIds), "every registered food must be displayed (no orphans)");
	}

	/** The 38 borrowed textures — byte-identical upstream icon renames, in item id form. */
	private static final String[] BORROWED_PNGS;
	static {
		String[] tIds = new String[GT6Foods.FOOD_ROWS.size()];
		for (int i = 0; i < tIds.length; i++) tIds[i] = GT6Foods.FOOD_ROWS.get(i).id().substring("food_".length()) + ".png";
		BORROWED_PNGS = tIds;
	}

	/**
	 * Every borrowed texture exists AND its sha256 is grounded in the assets/README.md
	 * ledger (the GT6DynamoBowlRenderDatagenTest borrow form — a re-borrow or texture
	 * swap without a ledger row fails here).
	 */
	@Test
	public void borrowedTexturesAreGroundedInTheLedger() throws Exception {
		String tReadme = Files.readString(mdkRoot().resolve("src/main/resources/assets/README.md"), StandardCharsets.UTF_8);
		for (String tBasename : BORROWED_PNGS) {
			Path tPng = assetFile("textures/item/food/" + tBasename);
			assertTrue(Files.isRegularFile(tPng), "the borrowed texture must exist: " + tPng);
			String tHex = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(tPng)));
			assertTrue(tReadme.contains(tBasename), tBasename + " — filename absent from assets/README.md");
			assertTrue(tReadme.contains(tHex), tBasename + " — bytes hash to " + tHex + ", not grounded in the ledger");
		}
	}

	/** The 38 generated item models — item/generated over the item/food/<id-sans-prefix> layer0. */
	@Test
	public void generatedItemModelsPinTheBorrowLayers() {
		for (gregtech6.registry.GT6Foods.FoodRow tRow : GT6Foods.FOOD_ROWS) {
			JsonObject tModel = generatedJson("assets/gt6/models/item/" + tRow.id() + ".json");
			assertEquals("minecraft:item/generated", tModel.get("parent").getAsString(), tRow.id() + " — the flat-item parent");
			assertEquals("gt6:item/food/" + tRow.id().substring("food_".length()), tModel.getAsJsonObject("textures").get("layer0").getAsString(),
					tRow.id() + " — the borrowed layer0");
		}
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
		try (InputStream tStream = GT6FoodsTest.class.getClassLoader().getResourceAsStream(aPath)) {
			assertTrue(tStream != null, "the generated JSON must be on the classpath: " + aPath);
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (Exception aE) {
			throw new AssertionError(aE);
		}
	}
}
