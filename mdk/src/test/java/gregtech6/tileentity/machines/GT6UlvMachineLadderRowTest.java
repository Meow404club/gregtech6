package gregtech6.tileentity.machines;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import gregapi.data.TD;
import gregtech6.block.GTBasicMachineBlock;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.registry.GTMachines;

/**
 * The p28-c-ulv-machine-ladder row census (the OFFLINE half — the
 * {@link GT6KineticTrioRowTest} shape): the six V[0] = 8 EU × 1 A rows pinned to the
 * card constants — ULV window {4, 8, 16} ({@link GTMachines#ULV_TIER_INPUTS}, the :126
 * conversion of NBT_INPUT 8), the 1375 K melting gate on EVERY row (the stone-crucible
 * ceiling, decisions.p28-ulv-tier-rulings), the T0 material rungs (Kinetic_T[0] =
 * ANY.Wood / Electric_T[0] = TinAlloy, upstream MT.java:3690-3691 index 0), EU on every
 * row (the EU-variant precedent), menu = null on every row-carrier except the Canner ULV
 * row (which shares the live gt6:canner menu of its family), and the map/texture/metaId
 * columns. NO upstream VN[0] machine exists — the whole ladder is the declared-deviation
 * tier extension, so the metaIds are the family base + 5 convention (documented
 * deviation, data-only).
 */
public class GT6UlvMachineLadderRowTest extends TileEntityBasicMachineOfflineTestBase {

	@Test
	void ulvWindowIsThePacketDomainClosure() {
		// the {4, 8, 16} window: the 8 EU packet lands MID-WINDOW, every TIER_INPUTS
		// machine starts at min 16 — an 8 EU packet is dead below LV (the ULV wall)
		assertArrayEquals(new long[] {4, 8, 16}, GTMachines.ULV_TIER_INPUTS, "the V[0] window (min4/in8/max16)");
		for (long[] tTierInputs : GTMachines.TIER_INPUTS) {
			assertTrue(GTMachines.ULV_TIER_INPUTS[2] <= tTierInputs[0],
					"the ULV max (16) never reaches the smallest legacy min (" + tTierInputs[0]
							+ ") — an 8 EU packet cannot power a single legacy machine (the wall)");
		}
		assertEquals(1375L, GTMachines.ULV_MELTING_GATE_K, "the stone-crucible ceiling (GT6Crucibles.java:86)");
	}

	@Test
	void everyUlvRowCarriesTheGateTheWindowAndEU() {
		for (GTBasicMachineBlock.MachineRow tRow : java.util.List.of(
				GTMachines.SHREDDER_ULV_ROWS.get(0), GTMachines.CRUSHER_ULV_ROWS.get(0),
				GTMachines.SIFTER_ULV_ROWS.get(0), GTMachines.WIREMILL_ULV_ROWS.get(0),
				GTMachines.CANNER_ULV_ROWS.get(0), GTMachines.ROLLINGMILL_ROWS.get(0))) {
			assertEquals(1375L, tRow.maxMeltingPointK(), tRow.path() + ": the 1375 K gate (the 阈值 ULV 行全部 1375K ruling)");
			assertTrue(tRow.ulvVoltage(), tRow.path() + ": the {4,8,16} window marker");
			assertSame(TD.Energy.EU, tRow.energyType(), tRow.path() + ": EU on every ULV row (the EU-variant precedent)");
			assertTrue(tRow.cheapOverclocking(), tRow.path() + ": the :773 loop runs unconditionally");
			assertEquals(0, tRow.tier(), tRow.path() + ": the material rung index 0");
		}
		// the legacy rows stay byte-identical: no gate, no ULV marker (spot pairs per family)
		assertFalse(GTMachines.WIREMILL_ROWS.get(0).ulvVoltage(), "the legacy wiremill T1 keeps the legacy window");
		assertNull(GTMachines.WIREMILL_ROWS.get(0).maxMeltingPointK(), "the legacy wiremill T1 has no gate");
		assertNull(GTMachines.CANNER_ROWS.get(0).maxMeltingPointK(), "the legacy canner T1 has no gate");
		assertNull(GTMachines.DRYER_ROWS.get(0).maxMeltingPointK(), "the legacy dryer T1 has no gate");
		assertFalse(GTMachines.SIFTER_ROWS.get(0).ulvVoltage(), "the legacy sifter T1 keeps the legacy window");
	}

	@Test
	void ulvRowsCarryTheT0MaterialRungs() {
		// Kinetic_T[0] = ANY.Wood ("Any Wood", ANY.java:77) / Electric_T[0] = TinAlloy
		// (upstream MT.java:3690-3691 index 0 — the card's "T0 对应" arm)
		assertSame(gregapi.data.ANY.Wood, GTMachines.KINETIC_T0.get(), "the Kinetic_T[0] rung = ANY.Wood");
		assertSame(gregapi.data.MT.TinAlloy, GTMachines.ELECTRIC_T0.get(), "the Electric_T[0] rung = TinAlloy");
		for (GTBasicMachineBlock.MachineRow tRow : java.util.List.of(
				GTMachines.SHREDDER_ULV_ROWS.get(0), GTMachines.CRUSHER_ULV_ROWS.get(0),
				GTMachines.SIFTER_ULV_ROWS.get(0), GTMachines.WIREMILL_ULV_ROWS.get(0),
				GTMachines.ROLLINGMILL_ROWS.get(0))) {
			assertSame(GTMachines.KINETIC_T0.get(), tRow.material().get(), tRow.path() + ": the Kinetic_T[0] material column");
			assertEquals("any_wood", tRow.matSlug(), tRow.path() + ": the any_wood unit slug");
			assertEquals("Any Wood", tRow.matDisplay(), tRow.path() + ": the material word");
		}
		assertSame(GTMachines.ELECTRIC_T0.get(), GTMachines.CANNER_ULV_ROWS.get(0).material().get(),
				"canner_ulv: the Electric_T[0] material column");
		assertEquals("ulv", GTMachines.CANNER_ULV_ROWS.get(0).matSlug(), "canner_ulv: the VN[0] word slug (CS.java:154)");
		assertEquals("ULV", GTMachines.CANNER_ULV_ROWS.get(0).matDisplay(), "canner_ulv: the VN word display");
	}

	@Test
	void ulvRowColumnsMatchTheFamilyShapes() {
		// paths + metaIds (family base + 5, the documented invented convention — data-only)
		assertEquals("shredder_ulv", GTMachines.SHREDDER_ULV_ROWS.get(0).path());
		assertEquals("crusher_ulv", GTMachines.CRUSHER_ULV_ROWS.get(0).path());
		assertEquals("sifter_ulv", GTMachines.SIFTER_ULV_ROWS.get(0).path());
		assertEquals("wiremill_ulv", GTMachines.WIREMILL_ULV_ROWS.get(0).path());
		assertEquals("canner_ulv", GTMachines.CANNER_ULV_ROWS.get(0).path());
		assertEquals("rollingmill", GTMachines.ROLLINGMILL_ROWS.get(0).path());
		assertEquals(20015, GTMachines.SHREDDER_ULV_ROWS.get(0).metaId());
		assertEquals(20025, GTMachines.CRUSHER_ULV_ROWS.get(0).metaId());
		assertEquals(20055, GTMachines.SIFTER_ULV_ROWS.get(0).metaId());
		assertEquals(20155, GTMachines.WIREMILL_ULV_ROWS.get(0).metaId());
		assertEquals(20166, GTMachines.CANNER_ULV_ROWS.get(0).metaId());
		assertEquals(20115, GTMachines.ROLLINGMILL_ROWS.get(0).metaId());
		// family constants: texture + map + parallel
		assertEquals("shredder", GTMachines.SHREDDER_ULV_ROWS.get(0).texture());
		assertSame(GT6RecipeMaps.SHREDDER, GTMachines.SHREDDER_ULV_ROWS.get(0).recipes().get());
		assertEquals("crusher", GTMachines.CRUSHER_ULV_ROWS.get(0).texture());
		assertSame(GT6RecipeMaps.CRUSHER, GTMachines.CRUSHER_ULV_ROWS.get(0).recipes().get());
		assertEquals(4, GTMachines.CRUSHER_ULV_ROWS.get(0).parallel(), "the :1300 NBT_PARALLEL 4 family shape");
		assertTrue(GTMachines.CRUSHER_ULV_ROWS.get(0).parallelDuration(), "the :1300 NBT_PARALLEL_DURATION T family shape");
		assertSame(GT6RecipeMaps.SIFTING, GTMachines.SIFTER_ULV_ROWS.get(0).recipes().get());
		assertEquals(GTMachines.PARALLEL_4_32[0], GTMachines.SIFTER_ULV_ROWS.get(0).parallel(), "the :1312 family parallel shape");
		assertTrue(GTMachines.SIFTER_ULV_ROWS.get(0).parallelDuration(), "the :1312 NBT_PARALLEL_DURATION T family shape");
		assertEquals("wiremill", GTMachines.WIREMILL_ULV_ROWS.get(0).texture());
		assertSame(GT6RecipeMaps.WIREMILL, GTMachines.WIREMILL_ULV_ROWS.get(0).recipes().get());
		assertEquals(1, GTMachines.WIREMILL_ULV_ROWS.get(0).parallel(), "NO NBT_PARALLEL on the :1373 shape → 1");
		assertFalse(GTMachines.WIREMILL_ULV_ROWS.get(0).parallelDuration(), "NO NBT_PARALLEL_DURATION on the :1373 shape");
		assertEquals("rollingmill", GTMachines.ROLLINGMILL_ROWS.get(0).texture());
		assertSame(GT6RecipeMaps.ROLLING_MILL, GTMachines.ROLLINGMILL_ROWS.get(0).recipes().get());
		assertEquals("canner", GTMachines.CANNER_ULV_ROWS.get(0).texture());
		assertSame(GT6RecipeMaps.CANNER, GTMachines.CANNER_ULV_ROWS.get(0).recipes().get());
		// the display-key faces: the row-carrier compose templates
		assertEquals(GTMachines.MACHINE_SHREDDER_DISPLAY_KEY, GTMachines.SHREDDER_ULV_ROWS.get(0).displayKey());
		assertEquals(GTMachines.MACHINE_CRUSHER_DISPLAY_KEY, GTMachines.CRUSHER_ULV_ROWS.get(0).displayKey());
		assertEquals(GTMachines.MACHINE_SIFTER_UNIT_KEY, GTMachines.SIFTER_ULV_ROWS.get(0).displayKey());
		assertEquals(GTMachines.MACHINE_WIREMILL_UNIT_KEY, GTMachines.WIREMILL_ULV_ROWS.get(0).displayKey());
		assertEquals(GTMachines.CANNER_DISPLAY_KEY, GTMachines.CANNER_ULV_ROWS.get(0).displayKey());
		assertEquals(GTMachines.MACHINE_ROLLING_MILL_UNIT_KEY, GTMachines.ROLLINGMILL_ROWS.get(0).displayKey());
		// menus: null everywhere except the canner ULV row (the family's live gt6:canner)
		assertNull(GTMachines.SHREDDER_ULV_ROWS.get(0).menu(), "zero new gt6:* MenuType");
		assertNull(GTMachines.CRUSHER_ULV_ROWS.get(0).menu(), "zero new gt6:* MenuType");
		assertNull(GTMachines.SIFTER_ULV_ROWS.get(0).menu(), "zero new gt6:* MenuType");
		assertNull(GTMachines.WIREMILL_ULV_ROWS.get(0).menu(), "zero new gt6:* MenuType");
		assertNull(GTMachines.ROLLINGMILL_ROWS.get(0).menu(), "zero new gt6:* MenuType");
		assertNotNull(GTMachines.CANNER_ULV_ROWS.get(0).menu(), "the canner_ulv row shares the family gt6:canner menu");
	}

	/** The applyRow seam carries the gate onto the BE (the mask-carrier form). */
	@Test
	void applyRowCarriesTheMeltingGate() {
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.SHREDDER, 1, false);
		GTMachines.applyRow(tMachine, GTMachines.SHREDDER_ULV_ROWS.get(0));
		assertEquals(1375L, tMachine.mMaxMeltingPointK, "the gate column rides applyRow");
		// the legacy row clears it (the byte-identical contract)
		GTMachines.applyRow(tMachine, GTMachines.WIREMILL_ROWS.get(0));
		assertNull(tMachine.mMaxMeltingPointK, "the legacy rows ride null (no gate)");
	}
}
