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

	private static Set<ResourceLocation> itemIds(Iterable<RegistryObject<Item>> aEntries) {
		Set<ResourceLocation> rIds = new LinkedHashSet<>();
		for (RegistryObject<Item> tEntry : aEntries) rIds.add(tEntry.getId());
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
	 * The display table: exactly one row (the crowbar) in display order — the p10 card
	 * pins the cutter-free state; the cutter card appends row 2 and updates this test.
	 */
	@Test
	public void displayTableIsExactlyTheCrowbarRow() {
		assertEquals(1, GT6Tools.TAB_TABLE.size(), "the Tools tab displays exactly the crowbar until the cutter lands");
		assertSame(GT6Tools.CROWBAR, GT6Tools.TAB_TABLE.get(0), "row 0 is the registered crowbar item, not a parallel supplier");
		assertEquals(rl("crowbar"), GT6Tools.TAB_TABLE.get(0).getId());
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
		for (RegistryObject<Item> tRow : GT6Tools.TAB_TABLE) {
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
		for (RegistryObject<CreativeModeTab> tTab : GT6Tools.CREATIVE_MODE_TABS.getEntries()) {
			tIds.add(tTab.getId());
		}
		assertEquals(Set.of(rl("tools")), tIds);
	}
}
