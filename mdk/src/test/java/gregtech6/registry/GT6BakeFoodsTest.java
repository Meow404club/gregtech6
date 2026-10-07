/*
 * Offline tests for task food-bake-items: the GT6BakeFoods registration home — the T3
 * bake-chain subset (60 food rows + 6 food-grade molds of the upstream
 * MultiItemFood/MultiItemTechnological domains), the GT6FoodsTest posture over the
 * table-driven registration + the card acceptance pins: the FoodProperties literals
 * verbatim against the upstream FoodStat anchors (>=10 spot rows), the alwaysEdible
 * walk (T only on the Chum Burger, MultiItemFood.java:693), the byte-identical texture
 * borrows grounded in the assets/README.md sha256 ledger, and the generated item models.
 *
 * <p>The items are NOT constructible in this bootstrapped-and-frozen JVM (the mod-Item
 * intrusive-holder wall), so the assertion surface is the PURE table + registry-wiring
 * data + the FoodProperties construction (a vanilla record, constructible offline) —
 * the GT6FoodsTest posture verbatim.
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.food.FoodProperties;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class GT6BakeFoodsTest {

	/** The offline boot BEFORE the first GT6BakeFoods touch (the GT6FoodsTest.boot shape). */
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
		return ResourceLocation.fromNamespaceAndPath("gt6", aPath);
	}

	/**
	 * The T3 subset is EXACTLY 60 foods + 6 molds — one row per upstream addItem line,
	 * zero skip: the cookies 2000-2006 (MultiItemFood.java:598-621), the cake bottom
	 * 3000/3001 (:632-633), the flat doughs 4000/4002 (:640/:641), the pizzas 4010-4017
	 * (:650-667), the buns 5000-5003 (:675-678), the burgers 5010-5018 (:690-696), the
	 * breads 6000/6002/6003 (:714-717), the sandwiches 6010-6015 (:729-732), the baguettes
	 * 7000-7003 (:744-747), the large sandwiches 7010-7015 (:758-761), the fries
	 * 8000/8010/8011 (:354-356), the toasts 14000-14003 (:774-777), the doughs 32000-32006
	 * (:583-589), the potatoes on sticks 32700/32701 (:341/:347); the molds 10800-10805
	 * (MultiItemTechnological.java:334-342). The four vanilla ALIASES (Food_Bread :715,
	 * Food_Potato :340, Food_Potato_Baked :346, Food_Potato_Poisonous :336) are NOT
	 * registered — the vanilla items carry them (the food-brown-egg-alias precedent).
	 */
	@Test
	public void registeredSubsetIsExactly60FoodsPlus6Molds() {
		assertEquals(60, GT6BakeFoods.BAKE_ROWS.size(), "the T3 subset: 60 food rows, zero skip");
		assertEquals(6, GT6BakeFoods.MOLDS.size(), "the food-grade molds: 6 items (the Cylinder is upstream-real, :341)");
		assertEquals(66, GT6BakeFoods.ITEMS.getEntries().size(), "66 registrations total, zero skip");
	}

	/** The ids + table order — the upstream meta order (the BAKE_ROWS walk). */
	@Test
	public void bakeRowsAreTheUpstreamMetaOrderIds() {
		String[] tIds = {
				// the cookies (MultiItemFood.java:598-621)
				"food_cookie_raw", "food_cookie_raisins_raw", "food_cookie_raisins",
				"food_cookie_chocolate_raisins_raw", "food_cookie_chocolate_raisins", "food_cookie_abyssal_raw",
				// the cake bottom (:632-633)
				"food_cakebottom_raw", "food_cakebottom",
				// the flat doughs (:640/:641)
				"food_dough_flat", "food_dough_flat_ketchup",
				// the pizzas (:650-667)
				"food_pizza_cheese_raw", "food_pizza_cheese", "food_pizza_meat_raw", "food_pizza_meat",
				"food_pizza_veggie_raw", "food_pizza_veggie", "food_pizza_ananas_raw", "food_pizza_ananas",
				// the buns (:675-678)
				"food_bun_raw", "food_bun", "food_bun_sliced", "food_buns_sliced",
				// the burgers (:690-696)
				"food_burger_veggie", "food_burger_cheese", "food_burger_meat", "food_burger_chum",
				"food_burger_tofu", "food_burger_soylent", "food_burger_fish",
				// the breads (:714-717 — the vanilla Bread alias stays unregistered)
				"food_bread_raw", "food_bread_sliced", "food_breads_sliced",
				// the sandwiches (:729-732)
				"food_sandwich_veggie", "food_sandwich_cheese", "food_sandwich_bacon", "food_sandwich_steak",
				// the baguettes (:744-747)
				"food_baguette_raw", "food_baguette", "food_baguette_sliced", "food_baguettes_sliced",
				// the large sandwiches (:758-761)
				"food_large_sandwich_veggie", "food_large_sandwich_cheese", "food_large_sandwich_bacon",
				"food_large_sandwich_steak",
				// the fries (:354-356)
				"food_fries_raw", "food_fries", "food_fries_packaged",
				// the toasts (:774-777)
				"food_toast_raw", "food_toast", "food_toast_sliced", "food_toasted_sliced",
				// the doughs (:583-589)
				"food_dough", "food_dough_sugar", "food_dough_chocolate", "food_dough_egg",
				"food_dough_sugar_raisins", "food_dough_sugar_chocolate_raisins", "food_dough_abyssal",
				// the potatoes on sticks (:341/:347)
				"food_potato_on_stick", "food_potato_on_stick_roasted"};
		assertEquals(60, tIds.length);
		for (int i = 0; i < tIds.length; i++) {
			assertEquals(tIds[i], GT6BakeFoods.BAKE_ROWS.get(i).id(), "row " + i + " rides the upstream meta order");
			assertEquals(rl(tIds[i]), GT6BakeFoods.FOODS.get(i).getId(), "row " + i + " registered under the row id");
		}
	}

	/**
	 * The FoodProperties literals VERBATIM against the upstream FoodStat anchors — the
	 * card's >=10 spot rows (nutrition/saturation per row = the FoodStat (aFoodLevel,
	 * aSaturation) constructor literals of the anchor line): :598 cookie raw, :606 raisin
	 * cookie, :633 cake bottom, :641 ketchup flat dough, :667 pizza hawaii, :677 sliced bun,
	 * :692 hamburger, :693 chum burger, :731 bacon sandwich, :760 large bacon sandwich,
	 * :356 packaged fries, :583 dough, :347 roasted potato on a stick.
	 */
	@Test
	public void foodPropertiesSpotRowsAreTheUpstreamFoodStatLiterals() {
		String[][] tSpot = {
				{"food_cookie_raw", "1", "0.2"},
				{"food_cookie_raisins", "2", "0.2"},
				{"food_cakebottom", "3", "0.2"},
				{"food_dough_flat_ketchup", "2", "0.2"},
				{"food_pizza_ananas", "7", "1.2"},
				{"food_bun_sliced", "1", "1.2"},
				{"food_burger_meat", "6", "1.6"},
				{"food_burger_chum", "6", "1.6"},
				{"food_sandwich_bacon", "10", "1.8"},
				{"food_large_sandwich_bacon", "20", "2.8"},
				{"food_fries_packaged", "7", "1.2"},
				{"food_dough", "1", "1.0"},
				{"food_potato_on_stick_roasted", "5", "1.2"}};
		assertTrue(tSpot.length >= 10, "the card acceptance: >=10 verbatim spot rows");
		for (String[] tExpect : tSpot) {
			GT6BakeFoods.BakeRow tRow = bakeRow(tExpect[0]);
			FoodProperties tProps = GT6BakeFoods.foodProperties(tRow);
			//? if forge {
			assertEquals(Integer.parseInt(tExpect[1]), tProps.getNutrition(), tExpect[0] + " nutrition (the FoodStat aFoodLevel literal)");
			//?} else {
			/*assertEquals(Integer.parseInt(tExpect[1]), tProps.nutrition(), tExpect[0] + " nutrition (the FoodStat aFoodLevel literal)"); // 21.1 record accessor
			 *///?}
			//? if forge {
			assertEquals(Float.parseFloat(tExpect[2]), tProps.getSaturationModifier(), 0.0F, tExpect[0] + " saturation (the FoodStat aSaturation literal)");
			//?} else {
			/*assertEquals(Float.parseFloat(tExpect[2]) * Integer.parseInt(tExpect[1]) * 2.0F, tProps.saturation(), 1.0E-4F, tExpect[0] + " saturation = the FoodStat literal folded at build"); // 21.1: the record carries modifier*nutrition*2 (FoodProperties.java:113-115 FoodConstants.saturationByModifier) — the same literal through the total-energy fold
			 *///?}
		}
	}

	/**
	 * The alwaysEdible walk — F on 59 rows (the upstream 4-flag tail F,T,F,T), T on EXACTLY
	 * the Chum Burger (the T,F,T,T tail of MultiItemFood.java:693 — the T-slot is
	 * alwaysEdible, FoodStat.java:64-67 ctor order; its isRotten + hunger/confusion potion
	 * faces stay POOLED, the modern record has no columns).
	 */
	@Test
	public void alwaysEdibleIsFalseEverywhereExceptTheChumBurger() {
		for (GT6BakeFoods.BakeRow tRow : GT6BakeFoods.BAKE_ROWS) {
			FoodProperties tProps = GT6BakeFoods.foodProperties(tRow);
			assertEquals(tRow.alwaysEdible(), tProps.canAlwaysEat(), tRow.id() + " alwaysEdible rides the row flag");
		}
		assertEquals(1, GT6BakeFoods.BAKE_ROWS.stream().filter(GT6BakeFoods.BakeRow::alwaysEdible).count());
		assertEquals("food_burger_chum", GT6BakeFoods.BAKE_ROWS.stream().filter(GT6BakeFoods.BakeRow::alwaysEdible).findFirst().orElseThrow().id());
	}

	/** The 6 molds in upstream meta order (MultiItemTechnological.java:334/:338-342). */
	@Test
	public void moldsAreTheSixUpstreamMetaOrderIds() {
		String[] tIds = {"shape_foodmold_empty", "shape_foodmold_bun", "shape_foodmold_bread",
				"shape_foodmold_baguette", "shape_foodmold_cylinder", "shape_foodmold_toast"};
		assertEquals(tIds.length, GT6BakeFoods.MOLDS.size());
		for (int i = 0; i < tIds.length; i++) {
			assertEquals(rl(tIds[i]), GT6BakeFoods.MOLDS.get(i).getId(), "mold " + i + " rides the upstream meta order");
		}
	}

	/** The desc tooltip keys — the id-derived face, null on the empty-desc rows (the
	 * raw-key guard: the four upstream "" rows emit no key, review-fix follow). */
	@Test
	public void tooltipKeysAreTheIdDerivedLiterals() {
		for (GT6BakeFoods.BakeRow tRow : GT6BakeFoods.BAKE_ROWS) {
			if (tRow.enTooltip().isEmpty()) {
				assertNull(tRow.tooltipKey(), tRow.id());
			} else {
				assertEquals("item.gt6." + tRow.id() + ".tooltip", tRow.tooltipKey());
			}
		}
	}

	/** The 66 borrowed textures — byte-identical upstream icon renames, in item id form. */
	private static final String[] BORROWED_FOOD_PNGS = {
			"cookie_raw.png", "cookie_raisins_raw.png", "cookie_raisins.png", "cookie_chocolate_raisins_raw.png",
			"cookie_chocolate_raisins.png", "cookie_abyssal_raw.png", "cakebottom_raw.png", "cakebottom.png",
			"dough_flat.png", "dough_flat_ketchup.png", "pizza_cheese_raw.png", "pizza_cheese.png",
			"pizza_meat_raw.png", "pizza_meat.png", "pizza_veggie_raw.png", "pizza_veggie.png",
			"pizza_ananas_raw.png", "pizza_ananas.png", "bun_raw.png", "bun.png", "bun_sliced.png",
			"buns_sliced.png", "burger_veggie.png", "burger_cheese.png", "burger_meat.png", "burger_chum.png",
			"burger_tofu.png", "burger_soylent.png", "burger_fish.png", "bread_raw.png", "bread_sliced.png",
			"breads_sliced.png", "sandwich_veggie.png", "sandwich_cheese.png", "sandwich_bacon.png",
			"sandwich_steak.png", "baguette_raw.png", "baguette.png", "baguette_sliced.png",
			"baguettes_sliced.png", "large_sandwich_veggie.png", "large_sandwich_cheese.png",
			"large_sandwich_bacon.png", "large_sandwich_steak.png", "fries_raw.png", "fries.png",
			"fries_packaged.png", "toast_raw.png", "toast.png", "toast_sliced.png", "toasted_sliced.png",
			"dough.png", "dough_sugar.png", "dough_chocolate.png", "dough_egg.png", "dough_sugar_raisins.png",
			"dough_sugar_chocolate_raisins.png", "dough_abyssal.png", "potato_on_stick.png",
			"potato_on_stick_roasted.png"};
	private static final String[] BORROWED_MOLD_PNGS = {"empty.png", "bun.png", "bread.png", "baguette.png",
			"cylinder.png", "toast.png"};

	/**
	 * Every borrowed texture exists AND its sha256 is grounded in the assets/README.md
	 * ledger (the GT6FoodsTest borrow form — a re-borrow or texture swap without a ledger
	 * row fails here).
	 */
	@Test
	public void borrowedTexturesAreGroundedInTheLedger() throws Exception {
		String tReadme = Files.readString(mdkRoot().resolve("src/main/resources/assets/README.md"), StandardCharsets.UTF_8);
		assertTrue(tReadme.contains("task food-bake-items"), "the T3 ledger section must be present");
		for (String tBasename : BORROWED_FOOD_PNGS) {
			Path tPng = assetFile("textures/item/food/" + tBasename);
			assertTrue(Files.isRegularFile(tPng), "the borrowed texture must exist: " + tPng);
			String tHex = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(tPng)));
			assertTrue(tReadme.contains(tBasename), tBasename + " — filename absent from assets/README.md");
			assertTrue(tReadme.contains(tHex), tBasename + " — bytes hash to " + tHex + ", not grounded in the ledger");
		}
		for (String tBasename : BORROWED_MOLD_PNGS) {
			Path tPng = assetFile("textures/item/shape_foodmold/" + tBasename);
			assertTrue(Files.isRegularFile(tPng), "the borrowed mold texture must exist: " + tPng);
			String tHex = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(tPng)));
			assertTrue(tReadme.contains("shape_foodmold/" + tBasename), tBasename + " — filename absent from assets/README.md");
			assertTrue(tReadme.contains(tHex), tBasename + " — bytes hash to " + tHex + ", not grounded in the ledger");
		}
	}

	/** The 66 generated item models — item/generated over the borrowed layer0s. */
	@Test
	public void generatedItemModelsPinTheBorrowLayers() {
		for (GT6BakeFoods.BakeRow tRow : GT6BakeFoods.BAKE_ROWS) {
			JsonObject tModel = generatedJson("assets/gt6/models/item/" + tRow.id() + ".json");
			assertEquals("minecraft:item/generated", tModel.get("parent").getAsString(), tRow.id() + " — the flat-item parent");
			assertEquals("gt6:item/food/" + tRow.id().substring("food_".length()), tModel.getAsJsonObject("textures").get("layer0").getAsString(),
					tRow.id() + " — the borrowed layer0");
		}
		for (String tMold : new String[] {"empty", "bun", "bread", "baguette", "cylinder", "toast"}) {
			JsonObject tModel = generatedJson("assets/gt6/models/item/shape_foodmold_" + tMold + ".json");
			assertEquals("minecraft:item/generated", tModel.get("parent").getAsString(), tMold + " — the flat-item parent");
			assertEquals("gt6:item/shape_foodmold/" + tMold, tModel.getAsJsonObject("textures").get("layer0").getAsString(),
					tMold + " — the borrowed layer0");
		}
	}

	/** The row lookup by id (the spot-table face). */
	private static GT6BakeFoods.BakeRow bakeRow(String aId) {
		return GT6BakeFoods.BAKE_ROWS.stream().filter(aRow -> aRow.id().equals(aId)).findFirst().orElseThrow();
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
		try (InputStream tStream = GT6BakeFoodsTest.class.getClassLoader().getResourceAsStream(aPath)) {
			assertTrue(tStream != null, "the generated JSON must be on the classpath: " + aPath);
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (Exception aE) {
			throw new AssertionError(aE);
		}
	}
}
