/*
 * Offline tests for task food-crop-items: the GT6CropFoods registration home — the T5a
 * berry/nut/fruit band + the fodder family (49 food rows + 12 inedibles of the upstream
 * MultiItemFood domain), the GT6FoodsTest posture over the table-driven tab, plus the
 * domain pins the card acceptance adds: the FoodProperties literals verbatim against the
 * upstream FoodStat anchors, the byte-identical texture borrows grounded in the
 * assets/README.md sha256 ledger, and the meta anchors of the three census ledgers
 * (band/fodder/inedible).
 *
 * <p>The items are NOT constructible in this bootstrapped-and-frozen JVM (the mod-Item
 * intrusive-holder wall), so the assertion surface is the PURE table + registry-wiring
 * data + the FoodProperties construction (a vanilla record, constructible offline) —
 * the GT6FoodsTest posture verbatim. The inedible face is pinned at the table level: the
 * the 14 PLAIN_ROWS register through the no-food Properties lambda (the registration code),
 * never through {@link GT6CropFoods#foodProperties}.
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class GT6CropFoodsTest {

	/** The offline boot BEFORE the first GT6CropFoods touch (the GT6FoodsTest.boot shape). */
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

	/** The band census — EXACTLY 49 food rows + 14 inedibles = 63 registrations, zero skip. */
	@Test
	public void theBandIsExactly49FoodsPlus12Inedibles() {
		assertEquals(49, GT6CropFoods.FOOD_ROWS.size(), "the T5a food band: 49 rows");
		assertEquals(14, GT6CropFoods.PLAIN_ROWS.size(), "the inedibles: 4 apple cores + 8 fodder + 2 magic flowers (cbc-6)");
		assertEquals(63, GT6CropFoods.ITEMS.getEntries().size(), "63 registrations total, zero skip");
	}

	/**
	 * The food ids + table order — the upstream meta order. Spot anchors per family (the
	 * meta 三账): lemon 0/:273, lemon slice 1/:274, tomato 10/:279, maxim tomato 20/:285,
	 * onion 30/:290, cucumber 40/:296, pickle 42/:298, chili 50/:307, green grapes 60/:311,
	 * raisins green 61/:312, grapes purple 70/:323, chocolate raisins 72/:327, carrot slice
	 * 81/:331, banana 90/:384, pomegranate 100/:390, pomeraisins 101/:391, blueberry
	 * 110/:395, strawberry 200/:431, green apple 210/:436, yellow apple 220/:443, red slice
	 * 231/:451, dark-red apple 240/:457, peanut 250/:467, hazelnut 260/:472, ananas
	 * 270/:476, cinnamon 280/:482, coconut 290/:486.
	 */
	@Test
	public void foodIdsRideTheUpstreamMetaOrder() {
		String[] tIds = {"food_lemon", "food_lemon_sliced", "food_tomato", "food_tomato_sliced", "food_mtomato",
				"food_onion", "food_onion_sliced", "food_cucumber", "food_cucumber_sliced", "food_pickle",
				"food_pickle_sliced", "food_chili_pepper", "food_grapes_green", "food_raisins_green",
				"food_grapes_white", "food_raisins_white", "food_grapes_red", "food_raisins_red",
				"food_grapes_purple", "food_raisins_purple", "food_raisins_chocolate", "food_carrot_sliced",
				"food_banana", "food_banana_sliced", "food_pomegranate", "food_pomeraisins", "food_blueberry",
				"food_gooseberry", "food_candleberry", "food_cranberry", "food_currants_black",
				"food_currants_white", "food_currants_red", "food_blackberry", "food_raspberry",
				"food_strawberry", "food_apple_green", "food_apple_green_sliced", "food_apple_yellow",
				"food_apple_yellow_sliced", "food_apple_red_sliced", "food_apple_darkred",
				"food_apple_darkred_sliced", "food_peanut", "food_hazelnut", "food_ananas",
				"food_ananas_sliced", "food_cinnamon", "food_coconut"};
		assertEquals(tIds.length, GT6CropFoods.FOOD_ROWS.size());
		for (int i = 0; i < tIds.length; i++) {
			assertEquals(tIds[i], GT6CropFoods.FOOD_ROWS.get(i).id(), "row " + i + " rides the upstream meta order");
			assertEquals(rl(tIds[i]), GT6CropFoods.FOODS.get(i).getId(), "row " + i + " registered under the row id");
		}
		// the family spot anchors (id -> upstream meta), sampled over the band
		assertEquals("food_lemon", GT6CropFoods.FOOD_ROWS.get(0).id()); // meta 0, :273
		assertEquals("food_grapes_green", GT6CropFoods.FOOD_ROWS.get(12).id()); // meta 60, :311
		assertEquals("food_blueberry", GT6CropFoods.FOOD_ROWS.get(26).id()); // meta 110, :395
		assertEquals("food_apple_green", GT6CropFoods.FOOD_ROWS.get(36).id()); // meta 210, :436
		assertEquals("food_apple_red_sliced", GT6CropFoods.FOOD_ROWS.get(40).id()); // meta 231, :451
		assertEquals("food_coconut", GT6CropFoods.FOOD_ROWS.get(48).id()); // meta 290, :486
	}

	/**
	 * The inedible ids + table order — the 4 apple cores, the 8 fodder rows, then the 2
	 * magic flowers (the upstream order: cores :438-:459, grass states :53-56, crops :57-60,
	 * flowers meta 12010-12011 :98-99). The cores carry the mod-confusion tooltip, the
	 * grass/crop rows carry no key (the upstream "" rows), the flowers the magic tooltip.
	 */
	@Test
	public void plainIdsRideTheUpstreamMetaOrder() {
		String[] tIds = {"food_apple_green_core", "food_apple_yellow_core", "food_apple_red_core",
				"food_apple_darkred_core", "food_grass", "food_grass_dry", "food_grass_moldy",
				"food_grass_rotten", "food_crop_rye", "food_crop_oats", "food_crop_barley", "food_crop_rice",
				"food_cerublossom", "food_desertnova"};
		assertEquals(tIds.length, GT6CropFoods.PLAIN_ROWS.size());
		for (int i = 0; i < tIds.length; i++) {
			assertEquals(tIds[i], GT6CropFoods.PLAIN_ROWS.get(i).id(), "row " + i + " rides the upstream meta order");
			assertEquals(rl(tIds[i]), GT6CropFoods.PLAINS.get(i).getId(), "row " + i + " registered under the row id");
		}
		assertEquals("Not to be confused with the Mod", GT6CropFoods.PLAIN_ROWS.get(0).enTooltip()); // :438
		assertEquals("Make 9 of this into a Bale in order to dry it", GT6CropFoods.PLAIN_ROWS.get(4).enTooltip()); // :53
		assertEquals("Useful for making a simple Fire Starter", GT6CropFoods.PLAIN_ROWS.get(5).enTooltip()); // :54
		assertEquals("", GT6CropFoods.PLAIN_ROWS.get(8).enTooltip()); // the rye crop, :57
		// the cbc-6 magic-flower face — the addItem lines verbatim (MultiItemFood.java:98-99)
		assertEquals("Cerublossom", GT6CropFoods.PLAIN_ROWS.get(12).enName());
		assertEquals("Used for magical Purposes", GT6CropFoods.PLAIN_ROWS.get(12).enTooltip());
		assertEquals("Desert Nova", GT6CropFoods.PLAIN_ROWS.get(13).enName());
		assertEquals("Used for magical Purposes", GT6CropFoods.PLAIN_ROWS.get(13).enTooltip());
	}

	/**
	 * The FoodProperties literals VERBATIM against the upstream FoodStat anchors — the
	 * full-table walk (49 rows, the card's >=10 spot requirement exceeded): nutrition/
	 * saturationModifier per row = the FoodStat (aFoodLevel, aSaturation) constructor
	 * literals of the anchor line, canAlwaysEat = F on every row (the 4-flag tail
	 * F,T,F,T = alwaysEdible/..., FoodStat.java:64-67). Anchors per index: :273/:274/
	 * :279/:280/:285/:290/:291/:296/:297/:298/:299/:307/:311/:312/:315/:316/:319/:320/
	 * :323/:324/:327/:331/:384/:385/:390/:391/:395/:399/:403/:407/:411/:415/:419/:423/
	 * :427/:431/:436/:437/:443/:444/:451/:457/:458/:467/:472/:476/:477/:482/:486.
	 */
	@Test
	public void foodPropertiesAreTheUpstreamFoodStatLiterals() {
		int[] tNutrition = {1, 0, 1, 0, 9, 1, 0, 1, 0, 1, 0, 1, 1, 2, 1, 2, 1, 2, 1, 2, 3, 0, 1, 0, 1, 2,
				1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 4, 1, 5, 1, 1, 5, 1, 2, 2, 4, 1, 2, 2};
		float[] tSaturation = {0.6F, 0.15F, 0.6F, 0.15F, 1.0F, 1.2F, 0.3F, 1.2F, 0.3F, 1.2F, 0.3F, 1.2F,
				0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 1.2F, 0.3F, 0.6F, 0.15F, 0.6F, 0.6F,
				0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.6F, 0.4F, 0.4F, 0.3F, 0.3F,
				0.3F, 0.4F, 0.4F, 0.3F, 0.3F, 0.3F, 0.3F, 0.3F, 0.3F};
		assertEquals(tNutrition.length, GT6CropFoods.FOOD_ROWS.size());
		for (int i = 0; i < GT6CropFoods.FOOD_ROWS.size(); i++) {
			FoodProperties tProps = GT6CropFoods.foodProperties(GT6CropFoods.FOOD_ROWS.get(i));
			//? if forge {
			assertEquals(tNutrition[i], tProps.getNutrition(), GT6CropFoods.FOOD_ROWS.get(i).id() + " nutrition (the FoodStat aFoodLevel literal)");
			//?} else {
			/*assertEquals(tNutrition[i], tProps.nutrition(), GT6CropFoods.FOOD_ROWS.get(i).id() + " nutrition (the FoodStat aFoodLevel literal)"); // 21.1 record accessor
			 *///?}
			//? if forge {
			assertEquals(tSaturation[i], tProps.getSaturationModifier(), 0.0F, GT6CropFoods.FOOD_ROWS.get(i).id() + " saturation (the FoodStat aSaturation literal)");
			//?} else {
			/*assertEquals(tSaturation[i] * tNutrition[i] * 2.0F, tProps.saturation(), 1.0E-4F, GT6CropFoods.FOOD_ROWS.get(i).id() + " saturation = the FoodStat literal folded at build"); // 21.1: the record carries modifier*nutrition*2 (FoodProperties.java:113-115 FoodConstants.saturationByModifier)
			 *///?}
			assertFalse(tProps.canAlwaysEat(), GT6CropFoods.FOOD_ROWS.get(i).id() + " alwaysEdible=F (the upstream flag tail)");
		}
	}

	/**
	 * The tooltip keys — derivable from the id, NULL on the empty-desc rows (the raw-key
	 * guard: 18 empty food rows + 6 empty plain rows emit no key on either locale, the
	 * upstream "" registration displays nothing).
	 */
	@Test
	public void tooltipKeysAreIdDerivedAndNullOnTheEmptyRows() {
		int tKeys = 0;
		for (GT6CropFoods.CropFoodRow tRow : GT6CropFoods.FOOD_ROWS) {
			if (tRow.enTooltip().isEmpty()) {
				assertNull(tRow.tooltipKey(), tRow.id() + " — the empty desc must not emit a key");
			} else {
				assertEquals("item.gt6." + tRow.id() + ".tooltip", tRow.tooltipKey());
				tKeys++;
			}
		}
		assertEquals(31, tKeys, "the band's non-empty desc rows (49 foods - 18 empty: the 10 berries, 7 apple rows, hazelnut)");
	}

	/** The plain-row tooltip face: 8 non-empty keys (the cores + the 2 described grasses + the 2 magic flowers). */
	@Test
	public void plainTooltipKeysMatchTheUpstreamDescRows() {
		int tKeys = 0;
		for (GT6CropFoods.CropPlainRow tRow : GT6CropFoods.PLAIN_ROWS) {
			if (tRow.enTooltip().isEmpty()) {
				assertNull(tRow.tooltipKey(), tRow.id() + " — the empty desc must not emit a key");
			} else {
				assertEquals("item.gt6." + tRow.id() + ".tooltip", tRow.tooltipKey());
				tKeys++;
			}
		}
		assertEquals(8, tKeys, "4 apple cores + grass + dry grass (MultiItemFood.java:438-:459/:53-:54) + 2 flowers (:98-99)");
	}

	/**
	 * Registration smoke + bidirectional parity over BOTH tables (the GT6FoodsTest form):
	 * every FOODS/PLAINS row is a registered ITEMS entry and every registered entry is
	 * displayed (no orphans) — the food-tab displayItems walk cannot drift.
	 */
	@Test
	public void tablesAndItemsRegistryAreInParity() {
		Set<ResourceLocation> tTableIds = new LinkedHashSet<>();
		//? if forge {
		for (RegistryObject<Item> tRow : GT6CropFoods.FOODS) tTableIds.add(tRow.getId());
		for (RegistryObject<Item> tRow : GT6CropFoods.PLAINS) tTableIds.add(tRow.getId());
		//?} else {
		/*for (net.neoforged.neoforge.registries.DeferredHolder<Item, ? extends Item> tRow : GT6CropFoods.FOODS) tTableIds.add(tRow.getId()); // 21.1: the list stores the wildcard holder
		for (net.neoforged.neoforge.registries.DeferredHolder<Item, ? extends Item> tRow : GT6CropFoods.PLAINS) tTableIds.add(tRow.getId());
		 *///?}
		Set<ResourceLocation> tRegisteredIds = new LinkedHashSet<>();
		//? if forge {
		for (RegistryObject<Item> tEntry : GT6CropFoods.ITEMS.getEntries()) {
		//?} else {
		/*for (net.neoforged.neoforge.registries.DeferredHolder<Item, ? extends Item> tEntry : GT6CropFoods.ITEMS.getEntries()) { // 21.1: wildcard holder
		 *///?}
			tRegisteredIds.add(tEntry.getId());
		}
		assertTrue(tRegisteredIds.containsAll(tTableIds), "every table row must be a registered item");
		assertTrue(tTableIds.containsAll(tRegisteredIds), "every registered crop item must be displayed (no orphans)");
	}

	/** The 61 borrowed textures — byte-identical upstream icon renames, in item id form. */
	private static final String[] BORROWED_PNGS = {"lemon.png", "lemon_sliced.png", "tomato.png", "tomato_sliced.png",
			"mtomato.png", "onion.png", "onion_sliced.png", "cucumber.png", "cucumber_sliced.png", "pickle.png",
			"pickle_sliced.png", "chili_pepper.png", "grapes_green.png", "raisins_green.png", "grapes_white.png",
			"raisins_white.png", "grapes_red.png", "raisins_red.png", "grapes_purple.png", "raisins_purple.png",
			"raisins_chocolate.png", "carrot_sliced.png", "banana.png", "banana_sliced.png", "pomegranate.png",
			"pomeraisins.png", "blueberry.png", "gooseberry.png", "candleberry.png", "cranberry.png",
			"currants_black.png", "currants_white.png", "currants_red.png", "blackberry.png", "raspberry.png",
			"strawberry.png", "apple_green.png", "apple_green_sliced.png", "apple_green_core.png",
			"apple_yellow.png", "apple_yellow_sliced.png", "apple_yellow_core.png", "apple_red_sliced.png",
			"apple_red_core.png", "apple_darkred.png", "apple_darkred_sliced.png", "apple_darkred_core.png",
			"peanut.png", "hazelnut.png", "ananas.png", "ananas_sliced.png", "cinnamon.png", "coconut.png",
			"grass.png", "grass_dry.png", "grass_moldy.png", "grass_rotten.png", "crop_rye.png", "crop_oats.png",
			"crop_barley.png", "crop_rice.png"};

	/**
	 * Every borrowed texture exists AND its sha256 is grounded in the assets/README.md
	 * ledger (the GT6FoodsTest borrow form — a re-borrow or texture swap without a ledger
	 * row fails here).
	 */
	@Test
	public void borrowedTexturesAreGroundedInTheLedger() throws Exception {
		assertEquals(61, BORROWED_PNGS.length);
		String tReadme = Files.readString(mdkRoot().resolve("src/main/resources/assets/README.md"), StandardCharsets.UTF_8);
		for (String tBasename : BORROWED_PNGS) {
			Path tPng = assetFile("textures/item/food/" + tBasename);
			assertTrue(Files.isRegularFile(tPng), "the borrowed texture must exist: " + tPng);
			String tHex = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(tPng)));
			assertTrue(tReadme.contains(tBasename), tBasename + " — filename absent from assets/README.md");
			assertTrue(tReadme.contains(tHex), tBasename + " — bytes hash to " + tHex + ", not grounded in the ledger");
		}
	}

	/**
	 * The 2 magic-flower placeholders (task cbc-6) — present, 16x16 PNG, and sha256-grounded
	 * in the assets/README.md cbc-6 band (the borrow-grounding form; NOT byte-identical to
	 * upstream, the declared P20 placeholder: re-drawing or re-encoding turns this red).
	 */
	@Test
	public void magicFlowerPlaceholdersAreGroundedInTheLedger() throws Exception {
		String[] tBasenames = {"cerublossom.png", "desertnova.png"};
		String tReadme = Files.readString(mdkRoot().resolve("src/main/resources/assets/README.md"), StandardCharsets.UTF_8);
		for (String tBasename : tBasenames) {
			Path tPng = assetFile("textures/item/food/" + tBasename);
			assertTrue(Files.isRegularFile(tPng), "the placeholder must exist: " + tPng);
			byte[] tBytes = Files.readAllBytes(tPng);
			assertTrue(tBytes.length > 24 && tBytes[0] == (byte)0x89 && tBytes[1] == 'P' && tBytes[2] == 'N' && tBytes[3] == 'G',
					"PNG magic: " + tPng);
			// the big-endian IHDR dimensions (the CropCardsTest ihdr face)
			assertEquals(16, ((tBytes[16] & 0xFF) << 24) | ((tBytes[17] & 0xFF) << 16) | ((tBytes[18] & 0xFF) << 8) | (tBytes[19] & 0xFF),
					"16px wide: " + tPng);
			assertEquals(16, ((tBytes[20] & 0xFF) << 24) | ((tBytes[21] & 0xFF) << 16) | ((tBytes[22] & 0xFF) << 8) | (tBytes[23] & 0xFF),
					"16px tall: " + tPng);
			String tHex = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(tBytes));
			assertTrue(tReadme.contains(tBasename), tBasename + " — filename absent from assets/README.md");
			assertTrue(tReadme.contains(tHex), tBasename + " — bytes hash to " + tHex + ", not grounded in the ledger");
		}
	}

	/** The 61 generated item models — item/generated over the item/food/<id-sans-prefix> layer0. */
	@Test
	public void generatedItemModelsPinTheBorrowLayers() {
		for (GT6CropFoods.CropFoodRow tRow : GT6CropFoods.FOOD_ROWS) {
			JsonObject tModel = generatedJson("assets/gt6/models/item/" + tRow.id() + ".json");
			assertEquals("minecraft:item/generated", tModel.get("parent").getAsString(), tRow.id() + " — the flat-item parent");
			assertEquals("gt6:item/food/" + tRow.id().substring("food_".length()), tModel.getAsJsonObject("textures").get("layer0").getAsString(),
					tRow.id() + " — the borrowed layer0");
		}
		for (GT6CropFoods.CropPlainRow tRow : GT6CropFoods.PLAIN_ROWS) {
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
		try (InputStream tStream = GT6CropFoodsTest.class.getClassLoader().getResourceAsStream(aPath)) {
			assertTrue(tStream != null, "the generated JSON must be on the classpath: " + aPath);
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (Exception aE) {
			throw new AssertionError(aE);
		}
	}
}
