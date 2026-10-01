/*
 * Offline tests for task food-can-row0 + food-meat-items: the GT6FoodCans registration
 * home — the full 58-can census (the empty can + the nine 6-tier families + the three
 * air cans; the row0 minimal subset preceded it), the GT6SprayCansCreativeTabTest face
 * over the table-driven tab.
 *
 * <p>The items are NOT constructible in this bootstrapped-and-frozen JVM (the mod-Item
 * intrusive-holder wall), so the assertion surface is the PURE table + registry-wiring
 * data: {@code DeferredRegister.getEntries()} (the pre-registration entriesView) and
 * {@code RegistryObject.getId()} — the GT6ToolsCreativeTabTest posture verbatim.
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

public class GT6FoodCansTest {

	/** The offline boot BEFORE the first GT6FoodCans touch (the GT6ToolsCreativeTabTest.boot shape). */
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

	/** The tab DR targets the vanilla creative-tab registry (the GT6SprayCans shape). */
	@Test
	public void tabRegistryKeyIsTheVanillaCreativeTabRegistry() {
		assertEquals(Registries.CREATIVE_MODE_TAB, GT6FoodCans.CREATIVE_MODE_TABS.getRegistryKey());
	}

	/** Tab id 'food_cans' — gt6:food_cans (the per-family tab discipline). */
	@Test
	public void tabIdIsFoodCans() {
		assertEquals(rl("food_cans"), GT6FoodCans.FOOD_CANS_TAB.getId());
	}

	/** The title key literal both the tab builder and the GT6EnUs datagen row consume. */
	@Test
	public void titleKeyIsThePinnedLiteral() {
		assertEquals("itemGroup.gt6.food_cans", GT6FoodCans.TAB_TITLE_KEY);
	}

	/**
	 * The census is EXACTLY 58 items — the full IL.java:287-296 namespace (task
	 * food-meat-items completed the row0 subset): the empty can (upstream meta 998,
	 * MultiItemRandomTools.java:234) + the nine 6-tier families (unknown 1-6 :46-51,
	 * rotten 11-16 :53-58, veggie 21-26 :60-65, fruit 31-36 :67-72, bread 41-46 :74-79,
	 * meat 51-56 :81-86, fish 61-66 :88-93, chum 71-76 :95-100, cookies 81-86 :102-107) +
	 * the three air cans (32764-32766 :109-:111). Zero POOLED members remain.
	 */
	@Test
	public void registeredSubsetIsTheFullCensus58() {
		assertEquals(58, GT6FoodCans.ITEMS.getEntries().size(), "1 empty + 9x6 tiers + 3 air cans");
	}

	/** The CANS_ROTTEN family order — tier 0 = tiny .. tier 5 = huge (the IL.java:508 array order). */
	@Test
	public void rottenFamilyIsTheSixTierLadder() {
		assertEquals(6, GT6FoodCans.FOOD_CAN_ROTTEN.size(), "the dispatch indexes tiers 0..5 (RM.java:743-753)");
		String[] tSizes = {"tiny", "small", "tall", "wide", "large", "huge"};
		for (int i = 0; i < 6; i++) {
			assertEquals(rl("food_can_rotten_" + tSizes[i]), GT6FoodCans.FOOD_CAN_ROTTEN.get(i).getId(),
					"tier " + i + " rides the upstream size-adjective snake");
		}
	}

	/**
	 * The new families (task food-meat-items) — every 6-tier list rides the
	 * size-adjective snake under its family word, in upstream meta order.
	 */
	@Test
	public void newFamiliesAreTheSizeLadders() {
		String[] tSizes = {"tiny", "small", "tall", "wide", "large", "huge"};
		Object[][] tFamilies = {
				{GT6FoodCans.FOOD_CAN_UNKNOWN, "unknown", 1}, {GT6FoodCans.FOOD_CAN_VEGGIE, "veggie", 21},
				{GT6FoodCans.FOOD_CAN_FRUIT, "fruit", 31}, {GT6FoodCans.FOOD_CAN_BREAD, "bread", 41},
				{GT6FoodCans.FOOD_CAN_MEAT, "meat", 51}, {GT6FoodCans.FOOD_CAN_FISH, "fish", 61},
				{GT6FoodCans.FOOD_CAN_CHUM, "chum", 71}};
		for (Object[] tEntry : tFamilies) {
			@SuppressWarnings("unchecked")
			java.util.List<RegistryObject<Item>> tList = (java.util.List<RegistryObject<Item>>) tEntry[0];
			String tWord = (String) tEntry[1];
			int tMeta = (Integer) tEntry[2];
			assertEquals(6, tList.size(), tWord + " family: 6 tiers");
			for (int i = 0; i < 6; i++) {
				assertEquals(rl("food_can_" + tWord + "_" + tSizes[i]), tList.get(i).getId(),
						tWord + " tier " + i + " (upstream meta " + (tMeta + i) + ")");
			}
		}
		// the cookies tiers 1-5 + the huge standalone + the air trio
		for (int i = 0; i < 5; i++) {
			assertEquals(rl("food_can_cookies_" + tSizes[i]), GT6FoodCans.FOOD_CAN_COOKIES.get(i).getId(),
					"cookies tier " + i + " (upstream meta " + (81 + i) + ")");
		}
		assertEquals(rl("food_can_air"), GT6FoodCans.FOOD_CAN_AIR.getId(), "upstream meta 32766");
		assertEquals(rl("food_can_air_nether"), GT6FoodCans.FOOD_CAN_AIR_NETHER.getId(), "upstream meta 32765");
		assertEquals(rl("food_can_air_end"), GT6FoodCans.FOOD_CAN_AIR_END.getId(), "upstream meta 32764");
	}

	/** The two standalone can ids — the empty can and the Cookie Tin output. */
	@Test
	public void standaloneCanIdsAreTheUpstreamSnakes() {
		assertEquals(rl("food_can_empty"), GT6FoodCans.FOOD_CAN_EMPTY.getId(), "upstream meta 998");
		assertEquals(rl("food_can_cookies_huge"), GT6FoodCans.FOOD_CAN_COOKIES_HUGE.getId(), "upstream meta 86");
	}

	/**
	 * The display table: the FULL 58 rows — the empty can head (the tab icon), then the
	 * nine families in upstream meta order (unknown/rotten/veggie/fruit/bread/meat/fish/
	 * chum/cookies), cookies huge closing its family, then the air trio in registration
	 * order — each row the SAME RegistryObject the register call produced (the assertSame
	 * discipline).
	 */
	@Test
	public void displayTableIsTheCensusOrder() {
		assertEquals(58, GT6FoodCans.TAB_TABLE.size());
		assertSame(GT6FoodCans.FOOD_CAN_EMPTY, GT6FoodCans.TAB_TABLE.get(0), "row 0 is the empty can");
		Object[][] tFamilies = {
				{GT6FoodCans.FOOD_CAN_UNKNOWN, 1}, {GT6FoodCans.FOOD_CAN_ROTTEN, 7},
				{GT6FoodCans.FOOD_CAN_VEGGIE, 13}, {GT6FoodCans.FOOD_CAN_FRUIT, 19},
				{GT6FoodCans.FOOD_CAN_BREAD, 25}, {GT6FoodCans.FOOD_CAN_MEAT, 31},
				{GT6FoodCans.FOOD_CAN_FISH, 37}, {GT6FoodCans.FOOD_CAN_CHUM, 43}};
		for (Object[] tEntry : tFamilies) {
			@SuppressWarnings("unchecked")
			java.util.List<RegistryObject<Item>> tList = (java.util.List<RegistryObject<Item>>) tEntry[0];
			int tBase = (Integer) tEntry[1];
			for (int i = 0; i < 6; i++) {
				assertSame(tList.get(i), GT6FoodCans.TAB_TABLE.get(tBase + i), "table row " + (tBase + i));
			}
		}
		for (int i = 0; i < 5; i++) {
			assertSame(GT6FoodCans.FOOD_CAN_COOKIES.get(i), GT6FoodCans.TAB_TABLE.get(49 + i), "cookies tier " + i);
		}
		assertSame(GT6FoodCans.FOOD_CAN_COOKIES_HUGE, GT6FoodCans.TAB_TABLE.get(54), "row 54 is the cookies tin");
		assertSame(GT6FoodCans.FOOD_CAN_AIR_END, GT6FoodCans.TAB_TABLE.get(55), "row 55 is the End air");
		assertSame(GT6FoodCans.FOOD_CAN_AIR_NETHER, GT6FoodCans.TAB_TABLE.get(56), "row 56 is the Nether air");
		assertSame(GT6FoodCans.FOOD_CAN_AIR, GT6FoodCans.TAB_TABLE.get(57), "row 57 is the plain air");
	}

	/**
	 * Registration smoke + bidirectional parity (the GT6ToolsCreativeTabTest form): every
	 * table row is a registered ITEMS entry and every registered entry is displayed.
	 */
	@Test
	public void tableAndItemsRegistryAreInParity() {
		Set<ResourceLocation> tTableIds = new LinkedHashSet<>();
		//? if forge {
		for (RegistryObject<Item> tRow : GT6FoodCans.TAB_TABLE) {
			tTableIds.add(tRow.getId());
		}
		//?} else {
		/*for (net.neoforged.neoforge.registries.DeferredHolder<Item, ? extends Item> tRow : GT6FoodCans.TAB_TABLE) { // 21.1: the table stores the wildcard holder
			tTableIds.add(tRow.getId());
		}
		*///?}
		Set<ResourceLocation> tRegisteredIds = new LinkedHashSet<>();
		//? if forge {
		for (RegistryObject<Item> tEntry : GT6FoodCans.ITEMS.getEntries()) {
		//?} else {
		/*for (net.neoforged.neoforge.registries.DeferredHolder<Item, ? extends Item> tEntry : GT6FoodCans.ITEMS.getEntries()) { // 21.1: wildcard holder
		*///?}
			tRegisteredIds.add(tEntry.getId());
		}
		assertTrue(tRegisteredIds.containsAll(tTableIds), "every table row must be a registered item");
		assertTrue(tTableIds.containsAll(tRegisteredIds), "every registered can must be displayed (no orphans)");
	}

	/** The tab DR holds exactly the one "food_cans" tab (per-family tab discipline). */
	@Test
	public void tabsRegistryHoldsExactlyTheFoodCansTab() {
		Set<ResourceLocation> tIds = new LinkedHashSet<>();
		//? if forge {
		for (RegistryObject<CreativeModeTab> tTab : GT6FoodCans.CREATIVE_MODE_TABS.getEntries()) {
		//?} else {
		/*for (net.neoforged.neoforge.registries.DeferredHolder<CreativeModeTab, ? extends CreativeModeTab> tTab : GT6FoodCans.CREATIVE_MODE_TABS.getEntries()) { // 21.1: wildcard holder
		*///?}
			tIds.add(tTab.getId());
		}
		assertEquals(Set.of(rl("food_cans")), tIds);
	}
}
