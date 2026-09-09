/**
 * The use() MUI dispatch truth table (task p26-mui-row-menu-null-dispatch, the OFFLINE
 * half): {@link GTBasicMachineBlock#opensModularUi} is the dispatch key use() consults
 * before the {@code GT6MuiMachine.tryOpen} / vanilla-menu fork, and the table is two
 * nulls deep —
 *
 * <ul>
 * <li>{@code row == null} → MUI (the row-less families: Shredder/Crusher/Lathe + the
 * Oven, the 2-arg constructor — the p26-mui-a-open-chain arm, unchanged);</li>
 * <li>{@code row != null, menu == null} → MUI (THE flipped arm: the batch-A machine form
 * {@code MachineRow + menu = null} — the Distillery is the live registered carrier
 * today, the W1 five families are the next consumers; before this task these stayed
 * INERT on the {@code menuBound()} gate);</li>
 * <li>{@code row != null, menu != null} → the vanilla menu path (the dryer/canner
 * chains, byte-identical);</li>
 * <li>{@code row == null, menu != null} is UNREPRESENTABLE — a null row carries no menu
 * column at all (the record only exists on row carriers).</li>
 * </ul>
 *
 * <p>The open calls themselves are the live-server surface (the BlockEntityUIFactory /
 * NetworkHooks networks) — the runServer/RCON gate drives them; here the KEY is pinned.
 * The boot is the GTBlockPropertyIdentityTest shape (the registries only matter for the
 * vanilla MenuType sentinel of the bound arm).
 */
package gregtech6.block;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.inventory.MenuType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.TD;
import gregtech6.registry.GTMachines;

public class GTBasicMachineBlockDispatchTest {

	@BeforeAll
	public static void bootOffline() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// Offline bootstrap noise is expected; the dispatch key needs no live network.
		}
	}

	/**
	 * A row fixture in the GTMachines.dryer column form, parameterized only on the menu
	 * column — everything else is inert payload (the suppliers are never resolved by the
	 * dispatch key).
	 */
	private static GTBasicMachineBlock.MachineRow row(
			java.util.function.Supplier<MenuType<gregtech6.gui.machines.GTBasicMachineMenu>> aMenu) {
		return new GTBasicMachineBlock.MachineRow("fixture", "steel", "Steel", "gt6.row.dryer.display", 20311, 6.0F,
				0, 8, true,
				() -> null /*NBT_RECIPEMAP — inert payload*/, TD.Energy.HU, "dryer",
				(byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0,
				aMenu, true);
	}

	/** A live bound-menu sentinel (the ANVIL type under the offline bootstrap) — the supplier reference is all the key reads. */
	@SuppressWarnings("unchecked")
	private static java.util.function.Supplier<MenuType<gregtech6.gui.machines.GTBasicMachineMenu>> boundMenu() {
		java.util.function.Supplier<?> tSentinel = () -> MenuType.ANVIL;
		return (java.util.function.Supplier<MenuType<gregtech6.gui.machines.GTBasicMachineMenu>>) tSentinel;
	}

	@Test
	public void nullRowOpensModularUi() {
		// arm 1 — the row-less families (the 2-arg constructor): the p26-mui-a-open-chain
		// dispatch, byte-identical under the generalized key
		assertTrue(GTBasicMachineBlock.opensModularUi(null), "row == null → the ModularUI chain");
	}

	@Test
	public void menuNullRowOpensModularUi() {
		// arm 2 — THE flipped arm: the menu-less row carrier (MachineRow + menu = null, the
		// batch-A machine form) now dispatches MUI instead of staying inert
		assertTrue(GTBasicMachineBlock.opensModularUi(row(null)), "row != null && menu == null → the ModularUI chain");
	}

	@Test
	public void menuBoundRowKeepsTheVanillaPath() {
		// arm 3 — a bound supplier keeps the vanilla menu path (the dryer/canner chains)
		assertFalse(GTBasicMachineBlock.opensModularUi(row(boundMenu())), "row != null && menu != null → the vanilla menu path");
		// arm 4 is unrepresentable (a null row has no menu column) — the record itself
		// documents it: MachineRow.menu is only reachable through a non-null row
		assertNotNull(MenuType.ANVIL, "the sentinel menu type is live under the offline bootstrap");
	}

	@Test
	public void liveRowCensusRidesTheRightArms() {
		// the live registrations through the SAME key use() consults
		for (GTBasicMachineBlock.MachineRow tRow : GTMachines.DISTILLERY_ROWS) {
			assertNull(tRow.menu(), tRow.path() + " carries the menu-less form");
			assertTrue(GTBasicMachineBlock.opensModularUi(tRow), tRow.path() + " dispatches the ModularUI chain");
		}
		for (GTBasicMachineBlock.MachineRow tRow : GTMachines.DRYER_ROWS) {
			assertNotNull(tRow.menu(), tRow.path() + " keeps its bound gt6:dryer menu");
			assertFalse(GTBasicMachineBlock.opensModularUi(tRow), tRow.path() + " keeps the vanilla menu path");
		}
		for (GTBasicMachineBlock.MachineRow tRow : GTMachines.CANNER_ROWS) {
			assertNotNull(tRow.menu(), tRow.path() + " keeps its bound gt6:canner menu");
			assertFalse(GTBasicMachineBlock.opensModularUi(tRow), tRow.path() + " keeps the vanilla menu path");
		}
	}
}
