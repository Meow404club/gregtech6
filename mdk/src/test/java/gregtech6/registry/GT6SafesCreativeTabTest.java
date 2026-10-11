package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import gregtech6.tileentity.GTOfflineTestBase;

/**
 * Task storage-tabs-buckets-safes — the {@code gt6:safes} creative-tab pins (the
 * GT6ChestsCreativeTabTest offline discipline): the tab and its items are NOT resolvable in
 * the bootstrapped-and-frozen bare forge JVM (the mod-Item intrusive-holder wall), so the
 * registration pins ride the pre-registration {@code DeferredRegister} view, and the
 * display-walk pins drive {@link GT6StaticStorages#walkDisplayItems} directly through a
 * collector lambda — LIVE on the FML test JVM (the mod bootstraps and binds the holders),
 * assume-skipped on the bare forge leg (the GTOfflineTestBase ASSUME-SKIP posture).
 *
 * <p>The upstream truth this card rehomes: the metalset safe pair registers into the
 * MTE-registry's own "Safes" category tab (tab id 2010, Loader_MultiTileEntities.java
 * :134-135 exactly; one shared CreativeTab per tab id, MultiTileEntityRegistry.java:191).
 * The former MACHINES_TAB join keeps walking the remaining "Storage" rows (:138-144/
 * :177-184, tab id 32751 — that pooling is the declared tabfix-b-energy deviation for the
 * storage split, a later card), so the machines-tab clearance here is STRUCTURAL: both
 * walks branch on the one {@link GT6StaticStorages#isSafe} predicate, and pinning
 * safeRows()=4 / ROWS−safeRows=144 pins the machines walk's safe-free coverage (the
 * CreativeTabJoinCensusTest 455 → 451 retraction rides the same numbers).
 */
public class GT6SafesCreativeTabTest extends GTOfflineTestBase {

	private static ResourceLocation rl(String aPath) {
		return ResourceLocation.fromNamespaceAndPath("gt6", aPath);
	}

	/** The holder-bound face — the FML test JVM binds live, the bare forge JVM does not (the GT6ChestsCreativeTabTest fork). */
	private static boolean safeItemsBound() {
		//? if forge {
		return GT6StaticStorages.ITEMS_BY_PATH.get("safe_mechanical_bronze").isPresent();
		//?} else {
		/*return GT6StaticStorages.ITEMS_BY_PATH.get("safe_mechanical_bronze").isBound(); // 21.1: the DeferredHolder face
		 *///?}
	}

	// ----------------------------------------------------------------- registration pins

	/** The tab DR targets the vanilla creative-tab registry; id gt6:safes; the title key literal. */
	@Test
	public void tabRegistrationIsThePinnedSeat() {
		assertEquals(Registries.CREATIVE_MODE_TAB, GT6StaticStorages.CREATIVE_MODE_TABS.getRegistryKey());
		assertEquals(rl("safes"), GT6StaticStorages.SAFES_TAB.getId());
		assertEquals("itemGroup.gt6.safes", GT6StaticStorages.TAB_TITLE_KEY);
	}

	/** The DR holds exactly the one "safes" tab (the per-family tab discipline, the GT6FoodsTest form). */
	@Test
	public void tabsRegistryHoldsExactlyTheSafesTab() {
		Set<ResourceLocation> tIds = new LinkedHashSet<>();
		//? if forge {
		for (net.minecraftforge.registries.RegistryObject<CreativeModeTab> tTab : GT6StaticStorages.CREATIVE_MODE_TABS.getEntries()) {
		//?} else {
		/*for (net.neoforged.neoforge.registries.DeferredHolder<CreativeModeTab, ? extends CreativeModeTab> tTab : GT6StaticStorages.CREATIVE_MODE_TABS.getEntries()) { // 21.1: wildcard holder
		 *///?}
			tIds.add(tTab.getId());
		}
		assertEquals(Set.of(rl("safes")), tIds);
	}

	// ----------------------------------------------------------------- the display walk

	/**
	 * The safe family: 4 rows in {@link GT6StaticStorages#ROWS} registration order — per
	 * loader line the Mechanical Safe (:134) then the Key Locked Safe (:135), Bronze anchor
	 * ladder first (upstream storages() metalset order, :191 before :202) — head
	 * safe_mechanical_bronze, tail safe_keylocked_steel, every walk row the ROWS item. FML-leg
	 * live (assume-skip on the bare forge leg).
	 */
	@Test
	public void theWalkPinsTheSafeFamilyFour() {
		assumeTrue(safeItemsBound(), "bare forge leg: the holders are unbound — the walk face rides the FML leg");
		List<GT6StaticStorages.StaticRow> tExpected = GT6StaticStorages.safeRows();
		assertEquals(4, tExpected.size(), "two Safe loader lines :134-135 x the Bronze/Steel anchors");
		assertEquals("safe_mechanical_bronze", tExpected.get(0).path(), "the walk head = the Bronze anchor's Mechanical Safe");
		assertEquals("safe_keylocked_steel", tExpected.get(tExpected.size() - 1).path(), "the walk tail = the Steel anchor's Key Locked Safe");
		for (GT6StaticStorages.StaticRow tRow : tExpected) {
			assertTrue(GT6StaticStorages.isSafe(tRow), tRow.path() + " must be a safe-kind row");
		}
		List<ItemStack> tOut = new ArrayList<>();
		GT6StaticStorages.walkDisplayItems((aStack, aVisibility) -> tOut.add(aStack));
		assertEquals(4, tOut.size(), "the whole safe family rides the safes tab walk");
		for (int i = 0; i < tExpected.size(); i++) {
			assertSame(GT6StaticStorages.ITEMS_BY_PATH.get(tExpected.get(i).path()).get(), tOut.get(i).getItem(),
					"walk order = ROWS registration order, row " + i);
		}
	}

	/**
	 * The machines-tab clearance (the structural face): the walk split is the one
	 * {@link GT6StaticStorages#isSafe} predicate — the safe rows answer true, every other
	 * row answers false, so the machines join's {@code continue} clears exactly the 4 safes
	 * and the pool keeps 144 (the census total 455 → 451 rides these numbers).
	 */
	@Test
	public void theMachinesTabPoolIsSafeFree() {
		int tSafes = 0;
		for (GT6StaticStorages.StaticRow tRow : GT6StaticStorages.ROWS) {
			if (GT6StaticStorages.isSafe(tRow)) {
				tSafes++;
				assertTrue(tRow.path().startsWith("safe_"), tRow.path() + " safe-kind rows keep the safe_ path prefix");
			} else {
				assertFalse(tRow.path().startsWith("safe_"), tRow.path() + " leaked the safe_ prefix outside the safe kinds");
			}
		}
		assertEquals(4, tSafes, "the rehomed safe rows");
		assertEquals(148, GT6StaticStorages.ROWS.size(), "the family total is unchanged — only the tab home moved");
		assertEquals(144, GT6StaticStorages.ROWS.size() - tSafes, "the machines-tab pool keeps the Storage rows");
	}

	/**
	 * The tab icon = the family head item (safe_mechanical_bronze — the upstream lazy-tab
	 * icon face, MultiTileEntityRegistry.java:191: the tab icon = the first registration's
	 * block item). FML-leg live (assume-skip on the bare forge leg).
	 */
	@Test
	public void theTabIconIsTheFamilyHead() {
		assumeTrue(safeItemsBound(), "bare forge leg: the holders are unbound — the walk face rides the FML leg");
		assertSame(GT6StaticStorages.ITEMS_BY_PATH.get("safe_mechanical_bronze").get(),
				GT6StaticStorages.SAFES_TAB.get().getIconItem().getItem(),
				"the icon = the family head (the upstream :191 first-registration face)");
	}
}
