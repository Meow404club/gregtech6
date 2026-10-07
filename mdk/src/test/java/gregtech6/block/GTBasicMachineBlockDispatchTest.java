/**
 * The use() MUI dispatch truth table (task mui-row-menu-null-dispatch, the OFFLINE
 * half): {@link GTBasicMachineBlock#opensModularUi} is the dispatch key use() consults
 * before the {@code GT6MuiMachine.tryOpen} / vanilla-menu fork, and the table is two
 * nulls deep —
 *
 * <ul>
 * <li>{@code row == null} → MUI (the row-less families: Shredder/Crusher/Lathe + the
 * Oven, the 2-arg constructor — the mui-a-open-chain arm, unchanged);</li>
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

import static org.junit.jupiter.api.Assertions.assertEquals;
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
		return new GTBasicMachineBlock.MachineRow("fixture", "steel", "Steel",
				() -> null /*NBT_MATERIAL — inert payload, the dispatch key never resolves it (task machine-material-tint-fidelity column)*/,
				"gt6.row.dryer.display", 20311, 6.0F,
				0, 8, true,
				() -> null /*NBT_RECIPEMAP — inert payload*/, TD.Energy.HU, "dryer",
				(byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0, (byte)0,
				aMenu, true, null, false);
	}

	/** A live bound-menu sentinel (the ANVIL type under the offline bootstrap) — the supplier reference is all the key reads. */
	@SuppressWarnings("unchecked")
	private static java.util.function.Supplier<MenuType<gregtech6.gui.machines.GTBasicMachineMenu>> boundMenu() {
		java.util.function.Supplier<?> tSentinel = () -> MenuType.ANVIL;
		return (java.util.function.Supplier<MenuType<gregtech6.gui.machines.GTBasicMachineMenu>>) tSentinel;
	}

	@Test
	public void nullRowOpensModularUi() {
		// arm 1 — the row-less families (the 2-arg constructor): the mui-a-open-chain
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

	/**
	 * The whole-registry census (task mui-dispatch-seam): every {@code List<MachineRow>}
	 * family GTMachines declares walks the SAME key use() consults — a menu-less row
	 * dispatches the ModularUI chain, a menu-bound row keeps the vanilla path. The walk is
	 * reflection over the public static fields, so a family added tomorrow is pinned the
	 * day it lands (the family-count ratchet below forces the pin update consciously).
	 *
	 * <p>Erratum face (task mui-dispatch-seam archaeology): the pre-p26 registration
	 * comments claimed the menu-null rows sat INERT ("stays inert until the seam-① micro
	 * card") — the seam-① micro card IS task mui-row-menu-null-dispatch (merge f1b634d8,
	 * landed one day after the Press family registered): NO family is inert, and this
	 * census pins that exhaustively.
	 */
	@Test
	public void everyMachineRowFamilyRidesTheDispatch() throws ReflectiveOperationException {
		java.util.List<String> tMenuBoundFamilies = new java.util.ArrayList<>();
		int tMenuNullRows = 0, tMenuBoundRows = 0, tFamilies = 0;
		for (java.lang.reflect.Field tField : GTMachines.class.getFields()) {
			if (!java.util.List.class.isAssignableFrom(tField.getType())) continue;
			Object tValue = tField.get(null);
			if (!(tValue instanceof java.util.List<?> tList) || tList.isEmpty()
					|| !(tList.get(0) instanceof GTBasicMachineBlock.MachineRow)) continue;
			tFamilies++;
			boolean tMenuNull = true;
			for (Object tEntry : tList) {
				GTBasicMachineBlock.MachineRow tRow = (GTBasicMachineBlock.MachineRow) tEntry;
				if (tRow.menu() == null) {
					// arm 2 — the menu-less carrier: the MUI dispatch (the batch-A form)
					assertTrue(GTBasicMachineBlock.opensModularUi(tRow),
							tField.getName() + "/" + tRow.path() + " menu == null → the ModularUI chain");
					tMenuNullRows++;
				} else {
					// arm 3 — a bound supplier: the vanilla menu path
					assertFalse(GTBasicMachineBlock.opensModularUi(tRow),
							tField.getName() + "/" + tRow.path() + " menu bound → the vanilla menu path");
					tMenuBoundRows++;
					tMenuNull = false;
				}
			}
			if (!tMenuNull) tMenuBoundFamilies.add(tField.getName());
		}
		// the family-count ratchet — a family added or removed must update this pin
		// consciously (62 families at task mui-dispatch-seam)
		assertEquals(62, tFamilies, "the MachineRow family count — update this pin when a family lands");
		// the bound-arm roster is CLOSED: only the five documented shared carriers ride a
		// gt6:* MenuType (a new bound family must be named here on purpose)
		assertEquals(java.util.List.of("DRYER_ROWS", "CANNER_ROWS", "CANNER_ULV_ROWS",
				"BURNER_MIXER_ROWS", "PLANTALYZER_ROWS"), tMenuBoundFamilies,
				"the menu-bound roster — a new gt6:* MenuType carrier is a conscious decision");
		// the row-count ratchets (each ladder's length pinned by family sum)
		assertEquals(206, tMenuNullRows, "the menu-null row count");
		assertEquals(18, tMenuBoundRows, "the menu-bound row count (dryer 4 + canner 4 + canner_ulv 1 + burner_mixer 4 + plantalyzer 5)");
	}
}
