/*
 * Offline tests for task p25-food-can-row0: the GT6FoodCans registration home — the
 * row0 MINIMAL subset (the empty can + the CANS_ROTTEN 6-tier family + the Cookie Tin
 * output), the GT6SprayCansCreativeTabTest face over the table-driven tab.
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
	 * The row0 subset is EXACTLY 8 items: the empty can (upstream meta 998,
	 * MultiItemRandomTools.java:234) + the CANS_ROTTEN family (6 tiers, IL.java:508) +
	 * the tier-6 cookies can (MultiItemCans.java:107). The other 43 census metas stay
	 * POOLED (acceptance 4: the Undefined/Veggie/Fruit/Bread/Meat/Fish/Chum families,
	 * the other Cookies tiers and the three air cans produce zero registrations here).
	 */
	@Test
	public void registeredSubsetIsExactlyTheEightRow0Items() {
		assertEquals(8, GT6FoodCans.ITEMS.getEntries().size(), "the row0 subset: 1 empty + 6 rotten + 1 cookies tin");
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

	/** The two standalone can ids — the empty can and the Cookie Tin output. */
	@Test
	public void standaloneCanIdsAreTheUpstreamSnakes() {
		assertEquals(rl("food_can_empty"), GT6FoodCans.FOOD_CAN_EMPTY.getId(), "upstream meta 998");
		assertEquals(rl("food_can_cookies_huge"), GT6FoodCans.FOOD_CAN_COOKIES_HUGE.getId(), "upstream meta 86");
	}

	/**
	 * The display table: exactly 8 rows — the empty can, then the rotten ladder in tier
	 * order, then the cookies tin — each row the SAME RegistryObject the register call
	 * produced (the assertSame discipline).
	 */
	@Test
	public void displayTableIsTheRow0Order() {
		assertEquals(8, GT6FoodCans.TAB_TABLE.size());
		assertSame(GT6FoodCans.FOOD_CAN_EMPTY, GT6FoodCans.TAB_TABLE.get(0), "row 0 is the empty can");
		for (int i = 0; i < 6; i++) {
			assertSame(GT6FoodCans.FOOD_CAN_ROTTEN.get(i), GT6FoodCans.TAB_TABLE.get(1 + i),
					"row " + (1 + i) + " is rotten tier " + i);
		}
		assertSame(GT6FoodCans.FOOD_CAN_COOKIES_HUGE, GT6FoodCans.TAB_TABLE.get(7), "row 7 is the cookies tin");
	}

	/**
	 * Registration smoke + bidirectional parity (the GT6ToolsCreativeTabTest form): every
	 * table row is a registered ITEMS entry and every registered entry is displayed.
	 */
	@Test
	public void tableAndItemsRegistryAreInParity() {
		Set<ResourceLocation> tTableIds = new LinkedHashSet<>();
		for (RegistryObject<Item> tRow : GT6FoodCans.TAB_TABLE) {
			tTableIds.add(tRow.getId());
		}
		Set<ResourceLocation> tRegisteredIds = new LinkedHashSet<>();
		for (RegistryObject<Item> tEntry : GT6FoodCans.ITEMS.getEntries()) {
			tRegisteredIds.add(tEntry.getId());
		}
		assertTrue(tRegisteredIds.containsAll(tTableIds), "every table row must be a registered item");
		assertTrue(tTableIds.containsAll(tRegisteredIds), "every registered can must be displayed (no orphans)");
	}

	/** The tab DR holds exactly the one "food_cans" tab (per-family tab discipline). */
	@Test
	public void tabsRegistryHoldsExactlyTheFoodCansTab() {
		Set<ResourceLocation> tIds = new LinkedHashSet<>();
		for (RegistryObject<CreativeModeTab> tTab : GT6FoodCans.CREATIVE_MODE_TABS.getEntries()) {
			tIds.add(tTab.getId());
		}
		assertEquals(Set.of(rl("food_cans")), tIds);
	}
}
