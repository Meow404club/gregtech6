/*
 * Offline tests for task food-bottles-min: the GT6Bottles registration home (the
 * bottles-domain minimum 4 rows) — the census + the registry parity + the borrowed
 * textures grounded in the assets/README.md sha256 ledger + the item-model pins, the
 * GT6FoodsTest posture verbatim.
 *
 * <p>The items are NOT constructible in this bootstrapped-and-frozen JVM (the mod-Item
 * intrusive-holder wall), so the assertion surface is the pure registry-wiring data +
 * the on-disk assets — the GT6FoodCansTest posture.
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;

public class GT6BottlesTest {

	/** The offline boot BEFORE the first GT6Bottles touch (the GT6FoodCansTest.boot shape). */
	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
	}

	/**
	 * The minimum subset is EXACTLY 4 items — one row per upstream identity, zero skip:
	 * bottle_empty (OP.bottle.dat(MT.Empty), OP.java:229), food_ketchup (meta 3101,
	 * MultiItemBottles.java:257), food_barbecuesauce (meta 805, :111) and food_heavycream
	 * (meta 1101, :139 — oredict bottleCream re-registered to foodHeavycream by
	 * LoaderOreDictReRegistrations.java:870, the port id IS the final crafting name).
	 * Every other bottle of the domain (~150 + the 48 dye bottles) stays the pool card.
	 */
	@Test
	public void registeredSubsetIsExactlyThe4Rows() {
		assertEquals(4, GT6Bottles.ITEMS.getEntries().size(), "the bottles minimum: 4 rows, zero skip");
	}

	/** The BOTTLES table walks the 4 ids in upstream identity order (empty, ketchup, BBQ, cream). */
	@Test
	public void bottlesTableWalksTheUpstreamIdentityOrder() {
		String[] tExpected = {"bottle_empty", "food_ketchup", "food_barbecuesauce", "food_heavycream"};
		assertEquals(tExpected.length, GT6Bottles.BOTTLES.size());
		for (int i = 0; i < tExpected.length; i++) {
			assertEquals("gt6:" + tExpected[i], GT6Bottles.BOTTLES.get(i).getId().toString(),
					"row " + i + " rides the upstream identity order");
		}
	}

	/**
	 * Registration smoke + bidirectional parity (the GT6FoodsTest form): every BOTTLES row
	 * is a registered ITEMS entry and every registered entry is displayed.
	 */
	@Test
	public void tableAndItemsRegistryAreInParity() {
		Set<ResourceLocation> tTableIds = new LinkedHashSet<>();
		//? if forge {
		for (net.minecraftforge.registries.RegistryObject<Item> tRow : GT6Bottles.BOTTLES) {
			tTableIds.add(tRow.getId());
		}
		Set<ResourceLocation> tRegisteredIds = new LinkedHashSet<>();
		for (net.minecraftforge.registries.RegistryObject<Item> tEntry : GT6Bottles.ITEMS.getEntries()) {
			tRegisteredIds.add(tEntry.getId());
		}
		//?} else {
		/*for (net.neoforged.neoforge.registries.DeferredHolder<Item, ? extends Item> tRow : GT6Bottles.BOTTLES) { // 21.1: the list stores the wildcard holder
			tTableIds.add(tRow.getId());
		}
		Set<ResourceLocation> tRegisteredIds = new LinkedHashSet<>();
		for (net.neoforged.neoforge.registries.DeferredHolder<Item, ? extends Item> tEntry : GT6Bottles.ITEMS.getEntries()) { // 21.1: wildcard holder
			tRegisteredIds.add(tEntry.getId());
		}
		*///?}
		assertTrue(tRegisteredIds.containsAll(tTableIds), "every table row must be a registered item");
		assertTrue(tTableIds.containsAll(tRegisteredIds), "every registered bottle must be displayed (no orphans)");
	}

	/** The 3 borrowed textures — byte-identical upstream icon renames, in item id form. */
	private static final String[] BORROWED_PNGS = {"ketchup.png", "barbecuesauce.png", "heavycream.png"};

	/**
	 * Every borrowed texture exists AND its sha256 is grounded in the assets/README.md
	 * ledger (the GT6FoodsTest borrow form — a re-borrow or texture swap without a ledger
	 * row fails here).
	 */
	@Test
	public void borrowedTexturesAreGroundedInTheLedger() throws Exception {
		String tReadme = Files.readString(mdkRoot().resolve("src/main/resources/assets/README.md"), StandardCharsets.UTF_8);
		for (String tBasename : BORROWED_PNGS) {
			Path tPng = assetFile("textures/item/bottle/" + tBasename);
			assertTrue(Files.isRegularFile(tPng), "the borrowed texture must exist: " + tPng);
			String tHex = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(tPng)));
			assertTrue(tReadme.contains(tBasename), tBasename + " — filename absent from assets/README.md");
			assertTrue(tReadme.contains(tHex), tBasename + " — bytes hash to " + tHex + ", not grounded in the ledger");
		}
	}

	/**
	 * The 4 generated item models: the 3 borrowed faces are item/generated over the
	 * item/bottle/<id-sans-food_> layer0; bottle_empty parents the VANILLA glass_bottle
	 * model (upstream OP.bottle.dat(MT.Empty) has no own sprite — the declared deviation).
	 */
	@Test
	public void generatedItemModelsPinTheBorrowLayers() {
		JsonObject tEmpty = generatedJson("assets/gt6/models/item/bottle_empty.json");
		assertEquals("minecraft:item/glass_bottle", tEmpty.get("parent").getAsString(),
				"bottle_empty — the declared vanilla glass_bottle parent");
		for (String tBorrow : BORROWED_PNGS) {
			String tId = "food_" + tBorrow.substring(0, tBorrow.length() - ".png".length());
			JsonObject tModel = generatedJson("assets/gt6/models/item/" + tId + ".json");
			assertEquals("minecraft:item/generated", tModel.get("parent").getAsString(), tId + " — the flat-item parent");
			assertEquals("gt6:item/bottle/" + tBasename(tBorrow), tModel.getAsJsonObject("textures").get("layer0").getAsString(),
					tId + " — the borrowed layer0");
		}
	}

	private static String tBasename(String aPng) {
		return aPng.substring(0, aPng.length() - ".png".length());
	}

	/**
	 * The lang 四落 parity (the generated-tree lang JSONs): all 4 item keys + the tab key
	 * exist on BOTH locales with the upstream/verbatim values (en = the registration-row
	 * name column + the "GregTech: Bottles" tab literal, MultiItemBottles.java:257/:111/
	 * :139/:40; zh = the dump gt.multiitem.bottles.{3101,805,1101} faces + the composed
	 * 空+瓶 + the itemGroup.gt.multiitem.bottles tab face).
	 */
	@Test
	public void langFacesCarryTheBottleKeysOnBothLocales() {
		String[][] tPinned = {
				{"item.gt6.bottle_empty", "Empty Bottle", "空瓶"},
				{"item.gt6.food_ketchup", "Tomato Ketchup", "番茄酱"},
				{"item.gt6.food_barbecuesauce", "Barbecue Sauce", "烧烤酱"},
				{"item.gt6.food_heavycream", "Heavy Cream", "鲜奶油"},
				{"itemGroup.gt6.bottles", "GregTech: Bottles", "格雷牌饮料"}};
		for (String[] tRow : tPinned) {
			assertEquals(tRow[1], langValue("en_us", tRow[0]), tRow[0] + " en face");
			assertEquals(tRow[2], langValue("zh_cn", tRow[0]), tRow[0] + " zh face");
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
		try (InputStream tStream = GT6BottlesTest.class.getClassLoader().getResourceAsStream(aPath)) {
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
