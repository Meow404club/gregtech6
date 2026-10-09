/**
 * Offline tests for task tool-creative-tab: the "Tools" creative tab registration
 * (GT6Tools.TAB_TABLE + TOOLS_TAB, the GTWires.ELECTRIC_WIRES_TAB table-driven form).
 *
 * <p>The tab itself (a mod CreativeModeTab) and the items are NOT constructible in this
 * bootstrapped-and-frozen JVM — the mod-Item intrusive-holder wall (CrowbarTest.bootStrap
 * NOTE; the vanilla registries freeze after bootstrap). The offline assertion surface is
 * therefore the PURE table + registry-wiring data:
 * <ul>
 * <li>{@link DeferredRegister#getEntries()} returns the pre-registration view populated at
 *     {@code register()} call time (DeferredRegister.java:334, entriesView) — it proves the
 *     registration wiring without a registry event;</li>
 * <li>{@link RegistryObject#getId()} reads the name field set at construction
 *     (RegistryObject.java:287) — table row ids are assertable without resolving
 *     {@code get()}.</li>
 * </ul>
 * The live faces (tab lands in BuiltInRegistries.CREATIVE_MODE_TAB, the lang key resolves)
 * ride the runServer registration log line and the runData lang gate.
 */
package gregtech6.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.List;
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

public class GT6ToolsCreativeTabTest {

	/**
	 * The offline boot BEFORE the first GT6Tools touch: the DeferredRegister.create
	 * (Registries.*) constants pull Registries.&lt;clinit&gt; → BuiltInRegistries
	 * (ROOT_REGISTRY_NAME), which needs the vanilla bootstrap state; a clinit failure is
	 * sticky for the whole JVM (every later touch = NoClassDefFoundError). The
	 * CrowbarTest.boot shape: tryDetectVersion + bootStrap, the NetworkHooks init
	 * failure swallowed (offline-expected).
	 */
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

	//? if forge {
	private static Set<ResourceLocation> itemIds(Iterable<RegistryObject<Item>> aEntries) {
	//?} else {
	/*private static Set<ResourceLocation> itemIds(Iterable<net.neoforged.neoforge.registries.DeferredHolder<Item, ? extends Item>> aEntries) { // 21.1: getEntries hands the wildcard holder
	*///?}
		Set<ResourceLocation> rIds = new LinkedHashSet<>();
		//? if forge {
		for (RegistryObject<Item> tEntry : aEntries) rIds.add(tEntry.getId());
		//?} else {
		/*for (net.neoforged.neoforge.registries.DeferredHolder<Item, ? extends Item> tEntry : aEntries) rIds.add(tEntry.getId()); // 21.1: wildcard holder
		*///?}
		return rIds;
	}

	/** The tab DR targets the vanilla creative-tab registry (the GTWires.java:58 shape). */
	@Test
	public void tabRegistryKeyIsTheVanillaCreativeTabRegistry() {
		assertEquals(Registries.CREATIVE_MODE_TAB, GT6Tools.CREATIVE_MODE_TABS.getRegistryKey());
	}

	/** Tab id 'tools' (the task-card literal) — gt6:tools. */
	@Test
	public void tabIdIsTools() {
		assertEquals(rl("tools"), GT6Tools.TOOLS_TAB.getId());
	}

	/** The title key literal both the tab builder and the GT6EnUs datagen row consume. */
	@Test
	public void titleKeyIsThePinnedLiteral() {
		assertEquals("itemGroup.gt6.tools", GT6Tools.TAB_TITLE_KEY);
	}

	/**
	 * The display table seam — APPEND-ONLY (decisions.p30-w5-split-rulings): the ten
	 * base rows stay pinned at the HEAD (the asserts below), while the total grows:
	 * this baseline 10 + the 24 armor rows = 34 at the card's own baseline; the W5
	 * tool cards' 54 rows rebase in between and the wave-final census the wave gate
	 * re-measures is 88. The tail alone is fluid (the ArmorSetTest pins the armor
	 * tail; each wave card owns its own row parity in its own test class).
	 */
	@Test
	public void displayTableIsExactlyTheTenToolRows() {
		// task w5-t8-armor-24: the seam is APPEND-ONLY (decisions.p30-w5-split-rulings)
		// — the ten base rows stay pinned at the HEAD (the asserts below), while the total
		// grows: this baseline 10 + 24 armor = 34; the W5 tool cards' 54 rows rebase in
		// between and the wave-final census the wave gate re-measures is 88. The tail alone
		// is fluid (the ArmorSetTest pins it).
		assertTrue(GT6Tools.TAB_TABLE.size() >= 34,
				"the ten base rows + the 24 armor rows are the floor; the wave cards append between");
		assertSame(GT6Tools.CROWBAR, GT6Tools.TAB_TABLE.get(0), "row 0 is the registered crowbar item, not a parallel supplier");
		assertSame(GT6Tools.CUTTER, GT6Tools.TAB_TABLE.get(1), "row 1 is the registered cutter item, not a parallel supplier");
		assertSame(GT6Tools.CHISEL, GT6Tools.TAB_TABLE.get(2), "row 2 is the registered chisel item, not a parallel supplier");
		assertSame(GT6Tools.FILE, GT6Tools.TAB_TABLE.get(3), "row 3 is the registered file item, not a parallel supplier");
		assertSame(GT6Tools.SAW, GT6Tools.TAB_TABLE.get(4), "row 4 is the registered saw item, not a parallel supplier");
		assertSame(GT6Tools.BUILDER_WAND, GT6Tools.TAB_TABLE.get(5), "row 5 is the registered builder wand item, not a parallel supplier");
		assertSame(GT6Tools.SCREWDRIVER, GT6Tools.TAB_TABLE.get(6), "row 6 is the registered screwdriver item, not a parallel supplier");
		assertSame(GT6Tools.HAMMER, GT6Tools.TAB_TABLE.get(7), "row 7 is the registered hammer item, not a parallel supplier");
		assertSame(GT6Tools.WRENCH, GT6Tools.TAB_TABLE.get(8), "row 8 is the registered wrench item, not a parallel supplier");
		assertSame(GT6Tools.BENDING_CYLINDER_SMALL, GT6Tools.TAB_TABLE.get(9), "row 9 is the registered bending cylinder item, not a parallel supplier");
	}

	/**
	 * Registration smoke + bidirectional parity: every table row must be a registered
	 * ITEMS entry (a table row referencing an unregistered item would crash the
	 * displayItems generator at runtime), and every registered ITEMS entry must appear
	 * in the table (no invisible tools — the crowbar was /give-only once, never again).
	 *
	 * <p>Since task disposable-tools-tab-rehome the tail rows register OUT of
	 * {@link GT6Tools#ITEMS}: the ten Single Use tools stay in their registration home
	 * {@link GT6Robotics#ITEMS} (one DR per family, the multiitem flattening) while their
	 * DISPLAY home is this table (the upstream Equipment tab, MultiItemRandomTools.java:59).
	 * The forward check therefore unions the two DRs.
	 */
	@Test
	public void tableAndItemsRegistryAreInParity() {
		Set<ResourceLocation> tTableIds = new LinkedHashSet<>();
		//? if forge {
		for (RegistryObject<Item> tRow : GT6Tools.TAB_TABLE) {
		//?} else {
		/*for (net.neoforged.neoforge.registries.DeferredHolder<Item, ? extends Item> tRow : GT6Tools.TAB_TABLE) { // 21.1: the table stores the wildcard holder
		*///?}
			tTableIds.add(tRow.getId());
		}
		Set<ResourceLocation> tRegisteredIds = itemIds(GT6Tools.ITEMS.getEntries());
		tRegisteredIds.addAll(itemIds(GT6Robotics.ITEMS.getEntries()));
		assertTrue(tRegisteredIds.containsAll(tTableIds), "every table row must be a registered item");
		assertTrue(tTableIds.containsAll(itemIds(GT6Tools.ITEMS.getEntries())), "every registered tool item must be displayed (no orphans)");
	}

	/**
	 * The rehome pin (task disposable-tools-tab-rehome): the ten Single Use tools are the
	 * TAB_TABLE tail rows 91-100, in the upstream registration order (the metas 8500-8509,
	 * MultiItemRandomTools.java:492-501), and none of them registers in GT6Tools.ITEMS (the
	 * registration home stays GT6Robotics).
	 */
	@Test
	public void theSingleUseTokenTailRidesTheToolsTab() {
		List<String> tExpected = List.of("wrench", "screwdriver", "saw", "hammer", "cutter", "chisel", "rubber", "blade", "drill", "file");
		assertEquals(101, GT6Tools.TAB_TABLE.size(), "the 88 prior rows + the three gun rows + the ten token rows");
		for (int i = 0; i < 10; i++) {
			ResourceLocation tId = GT6Tools.TAB_TABLE.get(91 + i).getId();
			assertEquals(rl("single_use_" + tExpected.get(i)), tId, "tail row " + (91 + i) + " in upstream meta order (8500+" + i + ")");
		}
		for (RegistryObject<Item> tToken : GT6Robotics.TOOL_TOKENS) {
			assertTrue(tTableContains(tToken.getId()), "every single-use tool is displayed in the tools tab");
			assertTrue(GT6Robotics.MACHINE_TAB_ITEMS.stream().noneMatch(tRow -> tRow.getId().equals(tToken.getId())),
					"zero single-use rows left in the machines-tab walk (the rehome is total)");
		}
	}

	private static boolean tTableContains(ResourceLocation aId) {
		//? if forge {
		for (RegistryObject<Item> tRow : GT6Tools.TAB_TABLE) if (tRow.getId().equals(aId)) return true;
		//?} else {
		/*for (net.neoforged.neoforge.registries.DeferredHolder<Item, ? extends Item> tRow : GT6Tools.TAB_TABLE) if (tRow.getId().equals(aId)) return true; // 21.1
		*///?}
		return false;
	}

	/** The tab DR holds exactly the one "tools" tab (per-family tab discipline). */
	@Test
	public void tabsRegistryHoldsExactlyTheToolsTab() {
		Set<ResourceLocation> tIds = new LinkedHashSet<>();
		//? if forge {
		for (RegistryObject<CreativeModeTab> tTab : GT6Tools.CREATIVE_MODE_TABS.getEntries()) {
		//?} else {
		/*for (net.neoforged.neoforge.registries.DeferredHolder<CreativeModeTab, ? extends CreativeModeTab> tTab : GT6Tools.CREATIVE_MODE_TABS.getEntries()) { // 21.1: wildcard holder
		*///?}
			tIds.add(tTab.getId());
		}
		assertEquals(Set.of(rl("tools")), tIds);
	}
}
