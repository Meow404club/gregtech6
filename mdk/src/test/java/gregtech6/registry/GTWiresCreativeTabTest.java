/**
 * Offline tests for task flat-redstone-tab: the per-category wire tab split — the 6
 * redstone wire items move from the flat ELECTRIC_WIRES_TAB into their own "Redstone Wires"
 * tab and the laser fiber wire into its own "Laser Wires" tab, the upstream MTE-registry
 * categories.
 *
 * <p>Upstream evidence (the card's archaeology): every Loader_MultiTileEntities redstone
 * registration row (Loader:1895-1902) carries the category string "Redstone Wires" with the
 * tab id 27050, while the laser fiber wire registers under "Laser Wires" with the tab id
 * 24900 (Loader:1815) — a SINGLE-member category. The MTE registry lazily creates one
 * CreativeTab per category with the tab-item icon at meta = the tab id
 * (MultiTileEntityRegistry.java:191: {@code new CreativeTab(name + "." + tabID,
 * aCategoricalName, Item.getItemFromBlock(mBlock), tabID)}), the title registered as
 * {@code itemGroup.<name> = aCategoricalName} (CreativeTab.java:35). No tab texture exists
 * upstream — the icon is an item render, so nothing is borrowed byte-wise.
 *
 * <p>The offline assertion surface follows the GT6ToolsCreativeTabTest discipline: the mod
 * CreativeModeTab objects and the items are NOT constructible in this bootstrapped-and-frozen
 * JVM, but the membership is TABLE-DRIVEN ({@code GTWires.*_TAB_TABLE}, the GT6Tools
 * TAB_TABLE form) over {@link RegistryObject} placeholders whose ids are readable without
 * resolving {@code get()} (RegistryObject.getId, RegistryObject.java:287).
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraftforge.registries.RegistryObject;

public class GTWiresCreativeTabTest {

	/** The offline boot before the first GTWires touch (the GT6ToolsCreativeTabTest.boot shape). */
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

	private static List<ResourceLocation> ids(List<RegistryObject<net.minecraft.world.item.Item>> aTable) {
		List<ResourceLocation> rIds = new ArrayList<>();
		for (RegistryObject<net.minecraft.world.item.Item> tRow : aTable) rIds.add(tRow.getId());
		return rIds;
	}

	/** The wire tab DR targets the vanilla creative-tab registry (the GT6Tools.java shape). */
	@Test
	public void wireTabRegistryKeyIsTheVanillaCreativeTabRegistry() {
		assertEquals(Registries.CREATIVE_MODE_TAB, GTWires.CREATIVE_MODE_TABS.getRegistryKey());
	}

	/** The wire tab DR holds exactly the three category tabs (electric / redstone / laser). */
	@Test
	public void wireTabsAreExactlyTheThreeCategoryTabs() {
		Set<ResourceLocation> tIds = new LinkedHashSet<>();
		//? if forge {
		for (RegistryObject<net.minecraft.world.item.CreativeModeTab> tTab : GTWires.CREATIVE_MODE_TABS.getEntries()) {
		//?} else {
		/*for (net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.item.CreativeModeTab, ? extends net.minecraft.world.item.CreativeModeTab> tTab : GTWires.CREATIVE_MODE_TABS.getEntries()) { // 21.1: getEntries hands the wildcard holder
		*///?}
			tIds.add(tTab.getId());
		}
		assertEquals(Set.of(rl("electric_wires"), rl("redstone_wires"), rl("laser_wires")), tIds);
	}

	/**
	 * The Redstone Wires tab: EXACTLY the 6 redstone family items in the upstream Loader row
	 * order — one bare wire + one insulated cable per material row
	 * (red_alloy :1895-1896, signalum :1898-1899, lumium :1901-1902; no size ladder).
	 */
	@Test
	public void redstoneTabMembersAreExactlyTheSixUpstreamRowsInOrder() {
		assertEquals(List.of(
				rl("wire_red_alloy"), rl("cable_red_alloy"),
				rl("wire_signalum"), rl("cable_signalum"),
				rl("wire_lumium"), rl("cable_lumium")),
				ids(GTWires.REDSTONE_WIRES_TAB_TABLE));
		assertEquals(6, GTWires.REDSTONE_WIRES_TAB_TABLE.size());
	}

	/** The Laser Wires tab: EXACTLY the single fiber wire (Loader:1815, single registration). */
	@Test
	public void laserTabMemberIsExactlyTheSingleFiberWire() {
		assertEquals(List.of(rl("wire_laser")), ids(GTWires.LASER_WIRES_TAB_TABLE));
		assertEquals(1, GTWires.LASER_WIRES_TAB_TABLE.size());
	}

	/**
	 * The Electric Wires tab: EXACTLY the 620 family (620 rows) — the 7 migrated members
	 * (6 redstone + 1 laser) are gone from it, and so are the p7 legacy 1x/2x placeholder
	 * rows (wiregt-legacy-anchor-removal — upstream registers no material-less electric
	 * wire, the pair was a port-built P8 RCON anchor).
	 */
	@Test
	public void electricTabHoldsExactlyThe620Family() {
		List<ResourceLocation> tIds = ids(GTWires.ELECTRIC_WIRES_TAB_TABLE);
		assertEquals(GTWires.FAMILY_ITEMS.size(), tIds.size(), "the tab IS the family list");
		assertEquals(620, tIds.size(), "620 family rows are census-pinned (GTWireSpecsCensusTest)");
		assertEquals(rl("wire_tin_gt01"), tIds.get(0), "row 0 is the first upstream electric row (Loader:1914 tin)");
		for (RegistryObject<net.minecraft.world.item.Item> tMigrated : GTWires.REDSTONE_WIRES_TAB_TABLE) {
			assertFalse(tIds.contains(tMigrated.getId()), "redstone row " + tMigrated.getId() + " must not ride the electric tab");
		}
		for (RegistryObject<net.minecraft.world.item.Item> tMigrated : GTWires.LASER_WIRES_TAB_TABLE) {
			assertFalse(tIds.contains(tMigrated.getId()), "laser row " + tMigrated.getId() + " must not ride the electric tab");
		}
	}

	/** No item is a member of two wire tabs (the 620 + 6 + 1 split is disjoint, union 627). */
	@Test
	public void theThreeTabTablesArePairwiseDisjoint() {
		Set<ResourceLocation> tSeen = new LinkedHashSet<>();
		List<List<RegistryObject<net.minecraft.world.item.Item>>> tTables = List.of(
				GTWires.ELECTRIC_WIRES_TAB_TABLE, GTWires.REDSTONE_WIRES_TAB_TABLE, GTWires.LASER_WIRES_TAB_TABLE);
		for (List<RegistryObject<net.minecraft.world.item.Item>> tTable : tTables) {
			for (RegistryObject<net.minecraft.world.item.Item> tRow : tTable) {
				assertTrue(tSeen.add(tRow.getId()), "duplicate tab member across wire tabs: " + tRow.getId());
			}
		}
		assertEquals(627, tSeen.size(), "620 electric + 6 redstone + 1 laser, no overlaps");
	}

	/**
	 * The Electric Wires tab icon slot: upstream the icon is the MTE block item at meta =
	 * the tab id 28366 (MultiTileEntityRegistry.java:191 icon rule) = the FIRST registered
	 * electric row (Loader:1914 tin). Ported: index 0 of the family list — the p7 2x
	 * placeholder icon retired with the pair (wiregt-legacy-anchor-removal).
	 */
	@Test
	public void electricTabIconSlotIsTheFirstUpstreamRow() {
		assertEquals(rl("wire_tin_gt01"), GTWires.ELECTRIC_WIRES_TAB_TABLE.get(0).getId());
	}

	/**
	 * The Redstone Wires tab icon slot: upstream the icon is the MTE block item at meta =
	 * the tab id 27050 (MultiTileEntityRegistry.java:191 icon rule) = the Signalum bare wire
	 * (Loader:1898 registers id 27050). Ported: index 2 of the redstone table must stay the
	 * signalum wire — reordering the table silently moves the icon.
	 */
	@Test
	public void redstoneTabIconSlotIsTheUpstreamSignalumWire() {
		assertEquals(rl("wire_signalum"), GTWires.REDSTONE_WIRES_TAB_TABLE.get(2).getId());
	}

	/**
	 * The Laser Wires tab icon slot: upstream meta 24900 = the one registered id = the fiber
	 * wire itself (Loader:1815).
	 */
	@Test
	public void laserTabIconSlotIsTheFiberWireItself() {
		assertEquals(rl("wire_laser"), GTWires.LASER_WIRES_TAB_TABLE.get(0).getId());
	}

	/** The title keys, pinned literals shared by the tab builders and the GT6EnUs datagen rows. */
	@Test
	public void titleKeysAreThePinnedLiterals() {
		assertEquals("itemGroup.gt6.redstone_wires", GTWires.REDSTONE_TAB_TITLE_KEY);
		assertEquals("itemGroup.gt6.laser_wires", GTWires.LASER_TAB_TITLE_KEY);
	}
}
