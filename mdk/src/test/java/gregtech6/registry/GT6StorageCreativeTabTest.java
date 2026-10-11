package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import gregtech6.tileentity.GTOfflineTestBase;

/**
 * Task storage-tab-completion — the {@code gt6:storage} / {@code gt6:logistics}
 * creative-tab pins (the GT6SafesCreativeTabTest offline discipline): the tabs and their
 * items are NOT resolvable in the bootstrapped-and-frozen bare forge JVM (the mod-Item
 * intrusive-holder wall), so the registration pins ride the pre-registration
 * {@code DeferredRegister} view, and the display-walk pins drive
 * {@link GT6StaticStorages#walkStorageItems} / {@link GT6StaticStorages#walkLogisticsItems}
 * directly through a collector lambda — LIVE on the FML test JVM (the mod bootstraps and
 * binds the holders), assume-skipped on the bare forge leg (the GTOfflineTestBase
 * ASSUME-SKIP posture).
 *
 * <p>The upstream truth this card rehomes: the metalset lines split three ways — the
 * Charging Locker (:139) and the standard Mass Storage (:141) register into the "Storage"
 * category tab (tab id 32751), while the Logistics Mass Storage (:142) registers into
 * "Logistics" (tab id 17997; NOT "Storage" — the pair is split upstream, the port keeps
 * the split rather than merging); one shared CreativeTab per tab id,
 * MultiTileEntityRegistry.java:191. The former MACHINES_TAB join on
 * {@link GT6ChargingLockers} RETIRED with this card (the negative pin rides reflection
 * name-absence, the GT6ChestsCreativeTabTest shape); the {@link GT6StaticStorages#MASS_ROWS}
 * family shipped join-less (creative+JEI blind) until this card.
 */
public class GT6StorageCreativeTabTest extends GTOfflineTestBase {

	private static ResourceLocation rl(String aPath) {
		return ResourceLocation.fromNamespaceAndPath("gt6", aPath);
	}

	/** The holder-bound face — the FML test JVM binds live, the bare forge JVM does not (the GT6SafesCreativeTabTest fork). */
	private static boolean storageItemsBound() {
		//? if forge {
		return GT6ChargingLockers.ITEMS_BY_PATH.get("charging_locker_lead").isPresent();
		//?} else {
		/*return GT6ChargingLockers.ITEMS_BY_PATH.get("charging_locker_lead").isBound(); // 21.1: the DeferredHolder face
		 *///?}
	}

	// ----------------------------------------------------------------- registration pins

	/** The tab DR targets the vanilla creative-tab registry; ids gt6:storage / gt6:logistics; the title key literals. */
	@Test
	public void tabRegistrationIsThePinnedSeat() {
		assertEquals(Registries.CREATIVE_MODE_TAB, GT6StaticStorages.CREATIVE_MODE_TABS.getRegistryKey());
		assertEquals(rl("storage"), GT6StaticStorages.STORAGE_TAB.getId());
		assertEquals(rl("logistics"), GT6StaticStorages.LOGISTICS_TAB.getId());
		assertEquals("itemGroup.gt6.storage", GT6StaticStorages.STORAGE_TAB_TITLE_KEY);
		assertEquals("itemGroup.gt6.logistics", GT6StaticStorages.LOGISTICS_TAB_TITLE_KEY);
	}

	// ----------------------------------------------------------------- the display walks

	/**
	 * The storage family: 120 items in the loader line order — the charging lockers first
	 * (:139), then the standard mass rows (:141), each in registration order (the hopper
	 * metalset walk: lead head, infinity tail). No logistics row leaks in (the :142 family
	 * rides the other tab). FML-leg live (assume-skip on the bare forge leg).
	 */
	@Test
	public void theWalkPinsTheStorageFamilyOneHundredTwenty() {
		assumeTrue(storageItemsBound(), "bare forge leg: the holders are unbound — the walk face rides the FML leg");
		List<ItemStack> tOut = new ArrayList<>();
		GT6StaticStorages.walkStorageItems((aStack, aVisibility) -> tOut.add(aStack));
		assertEquals(120, tOut.size(), "60 charging lockers (:139) + 60 standard mass rows (:141)");
		assertSame(GT6ChargingLockers.ITEMS_BY_PATH.get("charging_locker_lead").get(), tOut.get(0).getItem(),
				"the walk head = the first charging row (the :139 family, lead anchor)");
		assertEquals(GT6ChargingLockers.ITEMS_BY_PATH.get("charging_locker_infinity").get(), tOut.get(59).getItem(),
				"the walk mid = the last charging row (the :139 family tail)");
		assertEquals(GT6StaticStorages.MASS_ITEMS_BY_PATH.get("mass_storage_lead").get(), tOut.get(60).getItem(),
				"the walk second half opens with the first standard mass row (:141)");
		assertEquals(GT6StaticStorages.MASS_ITEMS_BY_PATH.get("mass_storage_infinity").get(), tOut.get(119).getItem(),
				"the walk tail = the last standard mass row");
		for (ItemStack tStack : tOut) {
			assertFalse(tStack.getItem().toString().contains("logistics_mass_storage"),
					"the logistics rows must not leak into the storage walk");
		}
	}

	/**
	 * The logistics family: the 60 logistics mass rows (the :142 line) in the
	 * {@link GT6StaticStorages#MASS_ROWS} registration order — lead head, infinity tail,
	 * every row the logistics kind. FML-leg live (assume-skip on the bare forge leg).
	 */
	@Test
	public void theWalkPinsTheLogisticsFamilySixty() {
		assumeTrue(storageItemsBound(), "bare forge leg: the holders are unbound — the walk face rides the FML leg");
		List<ItemStack> tOut = new ArrayList<>();
		GT6StaticStorages.walkLogisticsItems((aStack, aVisibility) -> tOut.add(aStack));
		assertEquals(60, tOut.size(), "the :142 logistics line over the 60-material metalset");
		assertEquals(GT6StaticStorages.MASS_ITEMS_BY_PATH.get("logistics_mass_storage_lead").get(), tOut.get(0).getItem(),
				"the walk head = the first logistics mass row");
		assertEquals(GT6StaticStorages.MASS_ITEMS_BY_PATH.get("logistics_mass_storage_infinity").get(), tOut.get(59).getItem(),
				"the walk tail = the last logistics mass row");
		assertEquals(120, GT6StaticStorages.MASS_ROWS.size(), "the mass family total is unchanged — only the tab homes moved");
	}

	/**
	 * The charging-locker MACHINES_TAB join is retired (the structural face): no
	 * {@code onBuildTabContents} handler remains on the family (the reflection
	 * name-absence pin, the GT6ChestsCreativeTabTest shape), while the walk seams exist
	 * for the storage tab.
	 */
	@Test
	public void theChargingLockerMachinesJoinIsRetired() throws Exception {
		for (Method tMethod : GT6ChargingLockers.class.getDeclaredMethods()) {
			assertFalse(tMethod.getName().equals("onBuildTabContents"),
					"the machines-tab pool join must stay retired (the family rides gt6:storage)");
		}
		Method tStorageWalk = GT6StaticStorages.class.getDeclaredMethod("walkStorageItems", CreativeModeTab.Output.class);
		assertTrue(java.lang.reflect.Modifier.isStatic(tStorageWalk.getModifiers()), "the storage walk seam");
		Method tLogisticsWalk = GT6StaticStorages.class.getDeclaredMethod("walkLogisticsItems", CreativeModeTab.Output.class);
		assertTrue(java.lang.reflect.Modifier.isStatic(tLogisticsWalk.getModifiers()), "the logistics walk seam");
	}

	/**
	 * The tab icons = the upstream first-registration faces (MultiTileEntityRegistry
	 * .java:191: the tab icon = the first registration's block item): gt6:storage's true
	 * head (:153 Storage Inserter) and the barrel/box block (:155-172) are unported, so the
	 * first PORTED registration is the wooden bookshelf ladder head (:181, i=0) —
	 * bookshelf_oak; gt6:logistics' head is the first logistics mass row (:142, the lead
	 * anchor — storages() at Loader :114 precedes the machinesN block :120-128, so :142
	 * precedes the Logistics Core :1281). FML-leg live (assume-skip on the bare forge leg).
	 */
	@Test
	public void theTabIconsAreTheUpstreamFirstPortedRows() {
		assumeTrue(storageItemsBound(), "bare forge leg: the holders are unbound — the walk face rides the FML leg");
		assertSame(GT6StaticStorages.ITEMS_BY_PATH.get("bookshelf_oak").get(),
				GT6StaticStorages.STORAGE_TAB.get().getIconItem().getItem(),
				"the storage icon = the first ported Storage-32751 registration (the :181 ladder head)");
		assertSame(GT6StaticStorages.MASS_ITEMS_BY_PATH.get("logistics_mass_storage_lead").get(),
				GT6StaticStorages.LOGISTICS_TAB.get().getIconItem().getItem(),
				"the logistics icon = the Logistics-17997 family head (the :142 lead anchor)");
	}
}
