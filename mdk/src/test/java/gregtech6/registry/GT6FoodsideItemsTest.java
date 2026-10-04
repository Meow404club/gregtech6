/*
 * Offline tests for task vanilla-alias-foodside: the GT6FoodsideItems registration home —
 * the census over the six rows, the LoaderItemList dye-alias round trip, the oredict tag
 * faces and the texture/model/lang artifact ratchet (the GT6FoodsTest posture over the
 * table-driven registration + the GT6DynamoBowlRenderDatagenTest borrow form).
 *
 * <p>FAITHFUL-CALIBER DECLARATION (duplicated from GT6FoodsideItems, the card-mandated
 * double statement): the {@code drop_honey}/{@code drop_honeydew} items are NEW-NATIVE —
 * upstream {@code OD.dropHoney}/{@code OD.dropHoneydew} (OD.java:159-160) are Forestry
 * ore-dict names with no GT6 item behind them; the user 2026-10-04 全补 ruling opened the
 * GT6-native pair so the Loader_Recipes_Food.java:534-550 listeners and the :674-679
 * coagulator band have a port face to unlock against. This card registers the items +
 * pins the tags ONLY — the recipe-row backfill rides the downstream food cards.
 *
 * <p>The items are NOT constructible in this bootstrapped-and-frozen JVM (the mod-Item
 * intrusive-holder wall), so the assertion surface is the PURE table + registry-wiring
 * data + the vanilla item handles (Bootstrapped registries answer key lookups offline —
 * the GT6FoodsTest posture).
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;

public class GT6FoodsideItemsTest {

	/** The offline boot BEFORE the first GT6FoodsideItems touch (the GT6FoodsTest.boot shape). */
	@BeforeAll
	static void boot() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
	}

	private static ResourceLocation rl(String aNamespace, String aPath) {
		return new ResourceLocation(aNamespace, aPath);
	}

	private static ResourceLocation rl(String aPath) {
		return rl("gt6", aPath);
	}

	/**
	 * The census — EXACTLY six rows, one per upstream face: the four Remains items ride
	 * their MultiItemFood.java:113-116 metas ascending (names verbatim), the two honey
	 * drops are the new-native pair (no meta, the faithful-caliber flag). The registry
	 * wiring answers id-for-id (the GT6FoodsTest parity form).
	 */
	@Test
	public void theFoodsideBandIsExactlyTheSixRows() {
		Object[][] tAnchors = {
				{"remains_plant", "Plant Remains", 12100, "item_plant_remains", false},
				{"remains_fruit", "Fruit Remains", 12101, "item_plant_remains", false},
				{"remains_veggie", "Vegetable Remains", 12102, "item_plant_remains", false},
				{"remains_nut", "Nut Remains", 12103, "item_plant_remains", false},
				{"drop_honey", "Honey Drop", -1, "drop_honey", true},
				{"drop_honeydew", "Honeydew Drop", -1, "drop_honeydew", true}};
		assertEquals(6, GT6FoodsideItems.ROWS.size(), "the 6-row band (4 remains + 2 new-native drops)");
		assertEquals(6, GT6FoodsideItems.ITEMS.getEntries().size(), "the DeferredRegister holds exactly the six rows");
		for (int i = 0; i < tAnchors.length; i++) {
			GT6FoodsideItems.SideRow tRow = GT6FoodsideItems.ROWS.get(i);
			assertEquals(tAnchors[i][0], tRow.id(), "row " + i + " rides the upstream identity order");
			assertEquals(tAnchors[i][1], tRow.enName(), tRow.id() + " — the display name");
			assertEquals(tAnchors[i][2], tRow.meta(), tRow.id() + " — the MultiItemFood meta (the drops carry none)");
			assertEquals(tAnchors[i][3], tRow.tagPath(), tRow.id() + " — the oredict-translation tag path");
			assertEquals(tAnchors[i][4], tRow.newNative(), tRow.id() + " — the faithful-caliber flag");
			assertEquals(rl((String) tAnchors[i][0]), GT6FoodsideItems.ITEMS_LIST.get(i).getId(), tRow.id() + " — registered under the row id");
			assertEquals("item.gt6." + tRow.id(), tRow.langKey(), tRow.id() + " — the lang key form");
		}
		// the named downstream handles ride the same holders (the Food:534-550 / :674-679 unlock faces)
		assertEquals(GT6FoodsideItems.ITEMS_LIST.get(4), GT6FoodsideItems.DROP_HONEY);
		assertEquals(GT6FoodsideItems.ITEMS_LIST.get(5), GT6FoodsideItems.DROP_HONEYDEW);
	}

	/**
	 * The dye alias round trip — LoaderItemList.java:756 pins {@code IL.Dye_SquidInk} on
	 * vanilla dye meta 0 (= the modern ink_sac) and :757 pins {@code IL.Dye_Cactus} on
	 * meta 2 (= green_dye). Both directions: the handle resolves to THE registry item
	 * under the vanilla id, and the vanilla id answers back the same item (the ST.make
	 * (Items.dye, 1, 0) identity, one register up).
	 */
	@Test
	public void dyeAliasesRoundTripOntoTheVanillaItems() {
		assertEquals(rl("minecraft", "ink_sac"), BuiltInRegistries.ITEM.getKey(GT6FoodsideItems.DYE_SQUID_INK.get()),
				"Dye_SquidInk -> minecraft:ink_sac (LoaderItemList.java:756, dye meta 0)");
		assertEquals(GT6FoodsideItems.DYE_SQUID_INK.get(), BuiltInRegistries.ITEM.get(rl("minecraft", "ink_sac")),
				"minecraft:ink_sac -> the Dye_SquidInk face (the round trip)");
		assertEquals(Items.INK_SAC, GT6FoodsideItems.DYE_SQUID_INK.get(), "the handle IS the vanilla item — no wrapper, no registration");
		assertEquals(rl("minecraft", "green_dye"), BuiltInRegistries.ITEM.getKey(GT6FoodsideItems.DYE_CACTUS.get()),
				"Dye_Cactus -> minecraft:green_dye (LoaderItemList.java:757, dye meta 2 — the b4 Tungstus output face)");
		assertEquals(GT6FoodsideItems.DYE_CACTUS.get(), BuiltInRegistries.ITEM.get(rl("minecraft", "green_dye")),
				"minecraft:green_dye -> the Dye_Cactus face (the round trip)");
		assertEquals(Items.GREEN_DYE, GT6FoodsideItems.DYE_CACTUS.get(), "the handle IS the vanilla item — no wrapper, no registration");
		// the scoped-out dye aliases stay out (the card scope: only the two rows b4 needs)
		assertFalse(GT6FoodsideItems.ROWS.stream().anyMatch(aRow -> aRow.id().startsWith("dye_")),
				"no dye item registers in the foodside band — the aliases are vanilla handles");
	}

	/**
	 * The oredict tag faces — the committed generated tag JSONs pin the translation:
	 * {@code #gt6:item_plant_remains} carries EXACTLY the four remains items (the
	 * OD.itemPlantRemains seat all four MultiItemFood.java:113-116 rows list) and the two
	 * drop tags carry exactly their own item (the OD.dropHoney/dropHoneydew faces —
	 * singleton tags BECAUSE the items are new-native: the tag is the downstream key).
	 */
	@Test
	public void oredictTagFacesCarryTheMembership() {
		JsonArray tRemains = tagJson("item_plant_remains").getAsJsonArray("values");
		assertEquals(4, tRemains.size(), "the shared item_plant_remains tag = the four Remains rows");
		Set<String> tRemainsIds = new LinkedHashSet<>();
		for (int i = 0; i < tRemains.size(); i++) tRemainsIds.add(tRemains.get(i).getAsString());
		assertEquals(Set.of("gt6:remains_plant", "gt6:remains_fruit", "gt6:remains_veggie", "gt6:remains_nut"),
				tRemainsIds, "the OD.itemPlantRemains membership universe");
		assertEquals(java.util.List.of("gt6:drop_honey"), jsonArrayStrings(tagJson("drop_honey").getAsJsonArray("values")),
				"the OD.dropHoney tag carries exactly the new-native item");
		assertEquals(java.util.List.of("gt6:drop_honeydew"), jsonArrayStrings(tagJson("drop_honeydew").getAsJsonArray("values")),
				"the OD.dropHoneydew tag carries exactly the new-native item");
	}

	/**
	 * The six borrowed/composed textures — byte-identical upstream icon borrows or
	 * declared placeholders, grounded in the assets/README.md sha256 ledger (the
	 * GT6FoodsTest borrow form — a re-borrow or texture swap without a ledger row fails
	 * here).
	 */
	@Test
	public void foodsideTexturesAreGroundedInTheLedger() throws Exception {
		String tReadme = Files.readString(mdkRoot().resolve("src/main/resources/assets/README.md"), StandardCharsets.UTF_8);
		for (GT6FoodsideItems.SideRow tRow : GT6FoodsideItems.ROWS) {
			Path tPng = assetFile("textures/item/foodside/" + tRow.id() + ".png");
			assertTrue(Files.isRegularFile(tPng), "the texture must exist: " + tPng);
			String tHex = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(tPng)));
			assertTrue(tReadme.contains(tRow.id() + ".png"), tRow.id() + ".png — filename absent from assets/README.md");
			assertTrue(tReadme.contains(tHex), tRow.id() + ".png — bytes hash to " + tHex + ", not grounded in the ledger");
		}
	}

	/** The six generated item models — item/generated over the item/foodside/<id> layer0. */
	@Test
	public void generatedItemModelsPinTheLayers() {
		for (GT6FoodsideItems.SideRow tRow : GT6FoodsideItems.ROWS) {
			JsonObject tModel = generatedJson("assets/gt6/models/item/" + tRow.id() + ".json");
			assertEquals("minecraft:item/generated", tModel.get("parent").getAsString(), tRow.id() + " — the flat-item parent");
			assertEquals("gt6:item/foodside/" + tRow.id(), tModel.getAsJsonObject("textures").get("layer0").getAsString(),
					tRow.id() + " — the layer0");
		}
	}

	/** The en/zh lang faces carry all six keys (the parity-by-walk face — the values ride the tables). */
	@Test
	public void langFacesCarryTheSixKeys() {
		JsonObject tEn = generatedJson("assets/gt6/lang/en_us.json");
		JsonObject tZh = generatedJson("assets/gt6/lang/zh_cn.json");
		for (GT6FoodsideItems.SideRow tRow : GT6FoodsideItems.ROWS) {
			assertEquals(tRow.enName(), tEn.get(tRow.langKey()).getAsString(), tRow.id() + " — the en face walks the row table");
			assertFalse(tZh.get(tRow.langKey()).getAsString().isBlank(), tRow.id() + " — the zh face is non-blank (the backfill invariant)");
		}
		// the zh remains values are the dump faces verbatim (gt.multiitem.food.12100-12103)
		assertEquals("植物废料", tZh.get("item.gt6.remains_plant").getAsString());
		assertEquals("水果废料", tZh.get("item.gt6.remains_fruit").getAsString());
		assertEquals("蔬菜废料", tZh.get("item.gt6.remains_veggie").getAsString());
		assertEquals("坚果废料", tZh.get("item.gt6.remains_nut").getAsString());
	}

	/** The remains rows carry NO tooltip key (the upstream "" desc column — the dump .tooltip= empty face). */
	@Test
	public void noTooltipKeysExist() {
		JsonObject tEn = generatedJson("assets/gt6/lang/en_us.json");
		for (GT6FoodsideItems.SideRow tRow : GT6FoodsideItems.ROWS) {
			assertNull(tEn.get(tRow.langKey() + ".tooltip"), tRow.id() + " — no desc column upstream, no tooltip key");
		}
	}

	private static java.util.List<String> jsonArrayStrings(JsonArray aArray) {
		java.util.List<String> tList = new java.util.ArrayList<>();
		for (int i = 0; i < aArray.size(); i++) tList.add(aArray.get(i).getAsString());
		return tList;
	}

	/**
	 * The committed generated tag JSON as an object — the tags live under
	 * {@code data/gt6/tags/items/} (the 1.20.1 datapack spelling; the classpath carries
	 * src/generated/resources where BOTH leg spellings are committed, so the path is
	 * leg-neutral).
	 */
	private static JsonObject tagJson(String aPath) {
		return generatedJson("data/gt6/tags/items/" + aPath + ".json");
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
		try (InputStream tStream = GT6FoodsideItemsTest.class.getClassLoader().getResourceAsStream(aPath)) {
			assertTrue(tStream != null, "the generated JSON must be on the classpath: " + aPath);
			return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
		} catch (Exception aE) {
			throw new AssertionError(aE);
		}
	}
}
