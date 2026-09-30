/*
 * Offline tests for task food-items-core: the GT6Foods registration home — the T1
 * MINIMUM subset (9 food rows of the upstream MultiItemFood domain), the GT6FoodCansTest
 * posture over the table-driven tab, plus the two domain pins the card acceptance adds:
 * the FoodProperties literals verbatim against the upstream FoodStat anchors and the
 * byte-identical texture borrows grounded in the assets/README.md sha256 ledger
 * (the GT6DynamoBowlRenderDatagenTest borrow form).
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
	 * The T1 subset is EXACTLY 9 items — one row per upstream addItem line, zero skip:
	 * cheese 1000 + cheese slice 1001 (MultiItemFood.java:490-491), boiled eggs
	 * 1060/1061 (:497-498), potato chips 9010 (:365) + chili chips 9020 (:374), plain ice
	 * cream 13000 (:809), butters 32117/32119 (:933-934). Everything else of the
	 * MultiItemFood domain stays POOLED (the T3-T5 waves + the flavour families).
	 */
	@Test
	public void registeredSubsetIsExactlyTheNineT1Items() {
		assertEquals(9, GT6Foods.ITEMS.getEntries().size(), "the T1 subset: 9 rows, zero skip");
	}

	/** The ids + table order — the upstream meta order (the FOOD_ROWS walk). */
	@Test
	public void foodRowsAreTheNineUpstreamMetaOrderIds() {
		String[] tIds = {"food_cheese", "food_cheese_sliced", "food_brown_egg_boiled", "food_white_egg_boiled",
				"food_potato_chips", "food_chili_chips", "food_ice_cream", "food_butter", "food_butter_salted"};
		assertEquals(tIds.length, GT6Foods.FOOD_ROWS.size());
		for (int i = 0; i < tIds.length; i++) {
			assertEquals(tIds[i], GT6Foods.FOOD_ROWS.get(i).id(), "row " + i + " rides the upstream meta order");
			assertEquals(rl(tIds[i]), GT6Foods.FOODS.get(i).getId(), "row " + i + " registered under the row id");
		}
	}

	/**
	 * The FoodProperties literals VERBATIM against the upstream FoodStat anchors —
	 * nutrition/saturationModifier per row = the FoodStat (aFoodLevel, aSaturation)
	 * constructor literals of the anchor line, canAlwaysEat = F (the 4-flag tail
	 * F,T,F,T = alwaysEdible/... , FoodStat.java:64-67). The total-energy face
	 * (nutrition + saturationModifier x nutrition x 2) is a pure function of these two
	 * pinned inputs. Anchors per index: :490/:491/:497/:498/:365/:374/:809/:933/:934.
	 */
	@Test
	public void foodPropertiesAreTheUpstreamFoodStatLiterals() {
		int[] tNutrition = {2, 1, 2, 2, 7, 7, 1, 1, 1};
		float[] tSaturation = {1.2F, 0.6F, 1.2F, 1.2F, 1.2F, 1.2F, 0.6F, 4.0F, 4.0F};
		for (int i = 0; i < GT6Foods.FOOD_ROWS.size(); i++) {
			FoodProperties tProps = GT6Foods.foodProperties(GT6Foods.FOOD_ROWS.get(i));
			//? if forge {
			assertEquals(tNutrition[i], tProps.getNutrition(), GT6Foods.FOOD_ROWS.get(i).id() + " nutrition (the FoodStat aFoodLevel literal)");
			//?} else {
			/*assertEquals(tNutrition[i], tProps.nutrition(), GT6Foods.FOOD_ROWS.get(i).id() + " nutrition (the FoodStat aFoodLevel literal)"); // 21.1 record accessor
			 *///?}
			//? if forge {
			assertEquals(tSaturation[i], tProps.getSaturationModifier(), 0.0F, GT6Foods.FOOD_ROWS.get(i).id() + " saturation (the FoodStat aSaturation literal)");
			//?} else {
			/*assertEquals(tSaturation[i] * tNutrition[i] * 2.0F, tProps.saturation(), 1.0E-4F, GT6Foods.FOOD_ROWS.get(i).id() + " saturation = the FoodStat literal folded at build"); // 21.1: the record carries modifier*nutrition*2 (FoodProperties.java:113-115 FoodConstants.saturationByModifier) — the same literal through the total-energy fold
			 *///?}
			assertFalse(tProps.canAlwaysEat(), GT6Foods.FOOD_ROWS.get(i).id() + " alwaysEdible=F (the upstream flag tail)");
		}
	}

	/** The desc tooltip keys — the GT6LaserGas tooltip-key face, derivable from the id. */
	@Test
	public void tooltipKeysAreTheIdDerivedLiterals() {
		for (gregtech6.registry.GT6Foods.FoodRow tRow : GT6Foods.FOOD_ROWS) {
			assertEquals("item.gt6." + tRow.id() + ".tooltip", tRow.tooltipKey());
		}
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

	/** The 9 borrowed textures — byte-identical upstream icon renames, in item id form. */
	private static final String[] BORROWED_PNGS = {"cheese.png", "cheese_sliced.png", "brown_egg_boiled.png",
			"white_egg_boiled.png", "potato_chips.png", "chili_chips.png", "ice_cream.png", "butter.png",
			"butter_salted.png"};

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

	/** The 9 generated item models — item/generated over the item/food/<id-sans-prefix> layer0. */
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
