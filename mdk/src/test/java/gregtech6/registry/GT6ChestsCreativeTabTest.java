package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import gregtech6.registry.GT6Chests.ChestRow;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * Task chests-tab-rehome — the {@code gt6:chests} creative-tab pins (the
 * GT6CropsCreativeTabTest offline discipline): the tab and its items are NOT resolvable in
 * the bootstrapped-and-frozen bare forge JVM (the mod-Item intrusive-holder wall), so the
 * registration pins ride the pre-registration {@code DeferredRegister} view, and the
 * display-walk pins drive {@link GT6Chests#walkDisplayItems} /
 * {@link GTBlockEntities#displayChests} directly through a collector lambda — LIVE on the
 * FML test JVM (the mod bootstraps and binds the holders), assume-skipped on the bare forge
 * leg (the GTOfflineTestBase ASSUME-SKIP posture).
 *
 * <p>The upstream truth this card rehomes: the metalset chest pair registers into the
 * MTE-registry's own "Chests" category tab (tab id 32745, Loader_MultiTileEntities.java
 * :132-133; one shared CreativeTab per tab id, MultiTileEntityRegistry.java:191) — the
 * former MACHINES_TAB join ({@code onBuildTabContents}, task tabfix-b-energy) is RETIRED,
 * which is also the MACHINES_TAB negative: the event join was the ONLY mechanism that put
 * chests into the machines tab, so the name-absence pin + the CreativeTabJoinCensusTest
 * retraction (575 → 455) are the machines-tab clearance.
 */
public class GT6ChestsCreativeTabTest extends GTOfflineTestBase {

	private static ResourceLocation rl(String aPath) {
		return ResourceLocation.fromNamespaceAndPath("gt6", aPath);
	}

	/** The holder-bound face — the FML test JVM binds live, the bare forge JVM does not (the GT6CropsCreativeTabTest seedItemsBound fork). */
	private static boolean chestItemsBound() {
		//? if forge {
		return GT6Chests.ITEMS_BY_PATH.get("chest_lead").isPresent();
		//?} else {
		/*return GT6Chests.ITEMS_BY_PATH.get("chest_lead").isBound(); // 21.1: the DeferredHolder face
		 *///?}
	}

	// ----------------------------------------------------------------- registration pins

	/** The tab DR targets the vanilla creative-tab registry; id gt6:chests; the title key literal (the GT6CropSticks.TAB_TITLE_KEY form). */
	@Test
	public void tabRegistrationIsThePinnedSeat() {
		assertEquals(Registries.CREATIVE_MODE_TAB, GTBlockEntities.CREATIVE_MODE_TABS.getRegistryKey());
		assertEquals(rl("chests"), GTBlockEntities.CHESTS_TAB.getId());
		assertEquals("itemGroup.gt6.chests", GTBlockEntities.TAB_TITLE_KEY);
	}

	/** The DR holds exactly the one "chests" tab (the per-family tab discipline, the GT6FoodsTest form). */
	@Test
	public void tabsRegistryHoldsExactlyTheChestsTab() {
		Set<ResourceLocation> tIds = new LinkedHashSet<>();
		//? if forge {
		for (net.minecraftforge.registries.RegistryObject<CreativeModeTab> tTab : GTBlockEntities.CREATIVE_MODE_TABS.getEntries()) {
		//?} else {
		/*for (net.neoforged.neoforge.registries.DeferredHolder<CreativeModeTab, ? extends CreativeModeTab> tTab : GTBlockEntities.CREATIVE_MODE_TABS.getEntries()) { // 21.1: wildcard holder
		 *///?}
			tIds.add(tTab.getId());
		}
		assertEquals(Set.of(rl("chests")), tIds);
	}

	/**
	 * The MACHINES_TAB ride is RETIRED: the registration home declares no
	 * {@code onBuildTabContents} join handler anymore (the name-absence face — the
	 * GT6CropsCreativeTabTest retirement discipline). The join WAS the only chest path
	 * into the machines tab; its absence + the census retraction (575 → 455) clear
	 * MACHINES_TAB of the family.
	 */
	@Test
	public void theMachinesTabRideIsRetired() {
		for (Method tMethod : GT6Chests.class.getDeclaredMethods()) {
			assertNotEquals("onBuildTabContents", tMethod.getName(),
					"GT6Chests still declares the retired MACHINES_TAB join");
		}
	}

	// ----------------------------------------------------------------- the display walk

	/**
	 * The full-family walk: 120 stacks in {@link GT6Chests#ROWS} registration order — per
	 * loader line the plain chest (:132) then the reinforced chest (:133) — head
	 * chest_lead, tail reinforced_chest_infinity, every walk row the ROWS item. FML-leg
	 * live (assume-skip on the bare forge leg).
	 */
	@Test
	public void theWalkPinsTheFamilyHundredTwenty() {
		assumeTrue(chestItemsBound(), "bare forge leg: the holders are unbound — the walk face rides the FML leg");
		List<ItemStack> tOut = new ArrayList<>();
		GT6Chests.walkDisplayItems((aStack, aVisibility) -> tOut.add(aStack));
		assertEquals(120, tOut.size(), "the whole family rides the chests tab walk (60 loader lines x the chest pair :132-133)");
		List<String> tPaths = new ArrayList<>();
		for (ChestRow tRow : GT6Chests.ROWS) tPaths.add(tRow.path());
		assertEquals("chest_lead", tPaths.get(0), "the walk head = the first metalset line's plain chest");
		assertEquals("reinforced_chest_infinity", tPaths.get(tPaths.size() - 1), "the walk tail = the last line's reinforced chest");
		for (int i = 0; i < tPaths.size(); i++) {
			assertSame(GT6Chests.ITEMS_BY_PATH.get(tPaths.get(i)).get(), tOut.get(i).getItem(),
					"walk order = ROWS registration order, row " + i);
		}
	}

	/**
	 * The tab seam ({@code GTBlockEntities.displayChests}, the builder lambda body): 121 =
	 * the 120-row family + the port-native example chest TAIL (the declared fold — the
	 * reference implementation stays reachable in creative + JEI, upstream has no such
	 * row). FML-leg live (assume-skip on the bare forge leg).
	 */
	@Test
	public void theTabSeamAppendsTheExampleChestTail() {
		assumeTrue(chestItemsBound(), "bare forge leg: the holders are unbound — the walk face rides the FML leg");
		List<ItemStack> tOut = new ArrayList<>();
		GTBlockEntities.displayChests((aStack, aVisibility) -> tOut.add(aStack));
		assertEquals(121, tOut.size(), "120 family rows + the example chest tail (the declared port-native fold)");
		assertSame(GT6Chests.ITEMS_BY_PATH.get("reinforced_chest_infinity").get(), tOut.get(119).getItem(),
				"row 119 = the family tail, the example chest rides AFTER the family");
		assertSame(GTBlockEntities.EXAMPLE_CHEST_ITEM.get(), tOut.get(120).getItem(), "the tail = the example chest");
	}

	/**
	 * The tab icon = the family head item (chest_lead — the upstream lazy-tab icon face,
	 * MultiTileEntityRegistry.java:191: the tab icon = the first registration's block
	 * item). FML-leg live (assume-skip on the bare forge leg).
	 */
	@Test
	public void theTabIconIsTheFamilyHead() {
		assumeTrue(chestItemsBound(), "bare forge leg: the holders are unbound — the walk face rides the FML leg");
		assertSame(GT6Chests.ITEMS_BY_PATH.get("chest_lead").get(),
				GTBlockEntities.CHESTS_TAB.get().getIconItem().getItem(),
				"the icon = the family head (the upstream :191 first-registration face)");
	}
}
