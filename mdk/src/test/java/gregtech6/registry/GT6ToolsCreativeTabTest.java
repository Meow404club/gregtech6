/**
 * Offline tests for task p10-tool-creative-tab: the "Tools" creative tab registration
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
		return new ResourceLocation("gt6", aPath);
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
	 * The display table: exactly sixty-four rows in display order — the p10-tool-cutter
	 * card appended row 2, the p16-chisel-decalcify card appended row 3, the
	 * p24-tool-system card appended rows 3/4, the p24-builder-wand card appended row 5,
	 * the p24-screwdriver-item card appended row 6, the p25-tool-hammer-wrench card
	 * appended rows 7/8, the p25-food-can-row0 card appended row 9, task
	 * p29-w5-t1-dig-six appended rows 10-15 (the six dig tools), task
	 * p29-w5-t2-blade-six appended rows 16-21 (the six blade tools — sword, knife,
	 * butchery_knife, club, axe, axe_double; the blade-six card owns the blade-row
	 * parity in BladeSixTest), task p29-w5-t3-machine-face-four appended rows 22-25
	 * (the machine-face four — the soft hammer, the monkey wrench, the magnifying
	 * glass, the pincers), task p29-w5-t4-field-five appended rows 26-30 (the five
	 * field tools — hoe, branch_cutter, sense, plow, hand_drill; the field-five card
	 * owns the field-row parity in FieldFiveTest), task p29-w5-t5-scene-six appended
	 * rows 31-36 (the six scene tools — scissors, scoop, plunger, flint_and_tinder,
	 * rolling_pin, bending_cylinder; the scene-six card owns the scene-row parity in
	 * SceneSixTest), task p29-w5-t6-electric-nineteen appended rows 37-55 (the nineteen
	 * electric tools; the electric card owns the electric-row parity in
	 * ElectricNineteenTest) and task p29-w5-t7-pocket-eight appended rows 56-63 (the
	 * eight pocket forms — the POCKET_FORMS order; the pocket card owns the pocket-row
	 * parity in PocketEightTest).
	 */
	@Test
	public void displayTableIsExactlyTheThirtyOneToolRows() {
		assertEquals(64, GT6Tools.TAB_TABLE.size(), "the Tools tab displays the 56 prior tools plus the eight pocket forms");
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
		assertEquals(rl("crowbar"), GT6Tools.TAB_TABLE.get(0).getId());
		assertEquals(rl("cutter"), GT6Tools.TAB_TABLE.get(1).getId());
		assertEquals(rl("chisel"), GT6Tools.TAB_TABLE.get(2).getId());
		assertEquals(rl("file"), GT6Tools.TAB_TABLE.get(3).getId());
		assertEquals(rl("saw"), GT6Tools.TAB_TABLE.get(4).getId());
		assertEquals(rl("builder_wand"), GT6Tools.TAB_TABLE.get(5).getId());
		assertEquals(rl("screwdriver"), GT6Tools.TAB_TABLE.get(6).getId());
		assertEquals(rl("hammer"), GT6Tools.TAB_TABLE.get(7).getId());
		assertEquals(rl("wrench"), GT6Tools.TAB_TABLE.get(8).getId());
		assertEquals(rl("bending_cylinder_small"), GT6Tools.TAB_TABLE.get(9).getId());
		assertSame(GT6Tools.PICKAXE, GT6Tools.TAB_TABLE.get(10), "row 10 is the registered pickaxe item, not a parallel supplier");
		assertSame(GT6Tools.PICKAXE_GEM, GT6Tools.TAB_TABLE.get(11), "row 11 is the registered gem pickaxe item, not a parallel supplier");
		assertSame(GT6Tools.PICKAXE_CONSTRUCTION, GT6Tools.TAB_TABLE.get(12), "row 12 is the registered construction pickaxe item, not a parallel supplier");
		assertSame(GT6Tools.SHOVEL, GT6Tools.TAB_TABLE.get(13), "row 13 is the registered shovel item, not a parallel supplier");
		assertSame(GT6Tools.SPADE, GT6Tools.TAB_TABLE.get(14), "row 14 is the registered spade item, not a parallel supplier");
		assertSame(GT6Tools.UNIVERSAL_SPADE, GT6Tools.TAB_TABLE.get(15), "row 15 is the registered universal spade item, not a parallel supplier");
		assertEquals(rl("pickaxe"), GT6Tools.TAB_TABLE.get(10).getId());
		assertEquals(rl("pickaxe_gem"), GT6Tools.TAB_TABLE.get(11).getId());
		assertEquals(rl("pickaxe_construction"), GT6Tools.TAB_TABLE.get(12).getId());
		assertEquals(rl("shovel"), GT6Tools.TAB_TABLE.get(13).getId());
		assertEquals(rl("spade"), GT6Tools.TAB_TABLE.get(14).getId());
		assertEquals(rl("universal_spade"), GT6Tools.TAB_TABLE.get(15).getId());
		assertSame(GT6Tools.SOFT_HAMMER, GT6Tools.TAB_TABLE.get(22), "row 22 is the registered soft hammer item, not a parallel supplier");
		assertSame(GT6Tools.MONKEY_WRENCH, GT6Tools.TAB_TABLE.get(23), "row 23 is the registered monkey wrench item, not a parallel supplier");
		assertSame(GT6Tools.MAGNIFYING_GLASS, GT6Tools.TAB_TABLE.get(24), "row 24 is the registered magnifying glass item, not a parallel supplier");
		assertSame(GT6Tools.PINCERS, GT6Tools.TAB_TABLE.get(25), "row 25 is the registered pincers item, not a parallel supplier");
		assertEquals(rl("soft_hammer"), GT6Tools.TAB_TABLE.get(22).getId());
		assertEquals(rl("monkey_wrench"), GT6Tools.TAB_TABLE.get(23).getId());
		assertEquals(rl("magnifying_glass"), GT6Tools.TAB_TABLE.get(24).getId());
		assertEquals(rl("pincers"), GT6Tools.TAB_TABLE.get(25).getId());
		assertSame(GT6Tools.HOE, GT6Tools.TAB_TABLE.get(26), "row 26 is the registered hoe item, not a parallel supplier");
		assertSame(GT6Tools.BRANCH_CUTTER, GT6Tools.TAB_TABLE.get(27), "row 27 is the registered branch cutter item, not a parallel supplier");
		assertSame(GT6Tools.SENSE, GT6Tools.TAB_TABLE.get(28), "row 28 is the registered sense item, not a parallel supplier");
		assertSame(GT6Tools.PLOW, GT6Tools.TAB_TABLE.get(29), "row 29 is the registered plow item, not a parallel supplier");
		assertSame(GT6Tools.HAND_DRILL, GT6Tools.TAB_TABLE.get(30), "row 30 is the registered hand drill item, not a parallel supplier");
		assertEquals(rl("hoe"), GT6Tools.TAB_TABLE.get(26).getId());
		assertEquals(rl("branch_cutter"), GT6Tools.TAB_TABLE.get(27).getId());
		assertEquals(rl("sense"), GT6Tools.TAB_TABLE.get(28).getId());
		assertEquals(rl("plow"), GT6Tools.TAB_TABLE.get(29).getId());
		assertEquals(rl("hand_drill"), GT6Tools.TAB_TABLE.get(30).getId());
		assertSame(GT6Tools.SCISSORS, GT6Tools.TAB_TABLE.get(31), "row 31 is the registered scissors item, not a parallel supplier");
		assertSame(GT6Tools.SCOOP, GT6Tools.TAB_TABLE.get(32), "row 32 is the registered scoop item, not a parallel supplier");
		assertSame(GT6Tools.PLUNGER, GT6Tools.TAB_TABLE.get(33), "row 33 is the registered plunger item, not a parallel supplier");
		assertSame(GT6Tools.FLINT_AND_TINDER, GT6Tools.TAB_TABLE.get(34), "row 34 is the registered flint and tinder item, not a parallel supplier");
		assertSame(GT6Tools.ROLLING_PIN, GT6Tools.TAB_TABLE.get(35), "row 35 is the registered rolling pin item, not a parallel supplier");
		assertSame(GT6Tools.BENDING_CYLINDER, GT6Tools.TAB_TABLE.get(36), "row 36 is the registered bending cylinder item, not a parallel supplier");
		assertEquals(rl("scissors"), GT6Tools.TAB_TABLE.get(31).getId());
		assertEquals(rl("scoop"), GT6Tools.TAB_TABLE.get(32).getId());
		assertEquals(rl("plunger"), GT6Tools.TAB_TABLE.get(33).getId());
		assertEquals(rl("flint_and_tinder"), GT6Tools.TAB_TABLE.get(34).getId());
		assertEquals(rl("rolling_pin"), GT6Tools.TAB_TABLE.get(35).getId());
		assertEquals(rl("bending_cylinder"), GT6Tools.TAB_TABLE.get(36).getId());
		assertSame(GT6Tools.POCKET_MULTITOOL, GT6Tools.TAB_TABLE.get(56), "row 56 is the registered pocket multitool item, not a parallel supplier");
		assertSame(GT6Tools.POCKET_MULTITOOL_KNIFE, GT6Tools.TAB_TABLE.get(57), "row 57 is the pocket knife form");
		assertSame(GT6Tools.POCKET_MULTITOOL_SAW, GT6Tools.TAB_TABLE.get(58), "row 58 is the pocket saw form");
		assertSame(GT6Tools.POCKET_MULTITOOL_FILE, GT6Tools.TAB_TABLE.get(59), "row 59 is the pocket file form");
		assertSame(GT6Tools.POCKET_MULTITOOL_SCREWDRIVER, GT6Tools.TAB_TABLE.get(60), "row 60 is the pocket screwdriver form");
		assertSame(GT6Tools.POCKET_MULTITOOL_WIRE_CUTTER, GT6Tools.TAB_TABLE.get(61), "row 61 is the pocket wire cutter form");
		assertSame(GT6Tools.POCKET_MULTITOOL_SCISSORS, GT6Tools.TAB_TABLE.get(62), "row 62 is the pocket scissors form");
		assertSame(GT6Tools.POCKET_MULTITOOL_CHISEL, GT6Tools.TAB_TABLE.get(63), "row 63 is the pocket chisel form");
		assertEquals(rl("pocket_multitool"), GT6Tools.TAB_TABLE.get(56).getId());
		assertEquals(rl("pocket_multitool_knife"), GT6Tools.TAB_TABLE.get(57).getId());
		assertEquals(rl("pocket_multitool_saw"), GT6Tools.TAB_TABLE.get(58).getId());
		assertEquals(rl("pocket_multitool_file"), GT6Tools.TAB_TABLE.get(59).getId());
		assertEquals(rl("pocket_multitool_screwdriver"), GT6Tools.TAB_TABLE.get(60).getId());
		assertEquals(rl("pocket_multitool_wire_cutter"), GT6Tools.TAB_TABLE.get(61).getId());
		assertEquals(rl("pocket_multitool_scissors"), GT6Tools.TAB_TABLE.get(62).getId());
		assertEquals(rl("pocket_multitool_chisel"), GT6Tools.TAB_TABLE.get(63).getId());
	}

	/**
	 * Registration smoke + bidirectional parity: every table row must be a registered
	 * ITEMS entry (a table row referencing an unregistered item would crash the
	 * displayItems generator at runtime), and every registered ITEMS entry must appear
	 * in the table (no invisible tools — the crowbar was /give-only once, never again).
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
		assertTrue(tRegisteredIds.containsAll(tTableIds), "every table row must be a registered item");
		assertTrue(tTableIds.containsAll(tRegisteredIds), "every registered tool item must be displayed (no orphans)");
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
