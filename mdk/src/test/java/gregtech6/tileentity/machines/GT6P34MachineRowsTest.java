/**
 * Copyright (c) 2025 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

package gregtech6.tileentity.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import gregapi.data.TD;
import gregtech6.block.GTBasicMachineBlock;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.maps.GT6RecipeMapBumblelyzer;
import gregtech6.registry.GTMachines;

/**
 * The p34 machine-row acceptance (task p34-machines-bumblelyzer-crucible, the OFFLINE half —
 * the {@link GT6HuTuPiggybackRowTest} shape): the two families pinned to the upstream columns
 * (Loader_MultiTileEntities.java :1608-1612 Bumblelyzer / :1437-1440 CrystallisationCrucible)
 * — the meta ladders, the masks, the parallel and energy columns, the texture tokens, the RM
 * map wiring, and the RM.java constants rows (:107 / :73). The registration half (9 blocks +
 * 9 items + 2 family BETs) resolves on the live server (the sweep p34_machines_bc group).
 */
public class GT6P34MachineRowsTest extends TileEntityBasicMachineOfflineTestBase {

	// the :1608 masks, post-read (the :137/:138/:143/:151 reads OR SBIT_A)
	private static final byte BUMBLE_ENERGY = (byte)(GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_A);
	private static final byte BUMBLE_TANK_IN = (byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A);
	private static final byte BUMBLE_INV_IN = (byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A);
	private static final byte BUMBLE_INV_OUT = (byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A);
	private static final byte NO_KEY = (byte)127;
	private static final byte UNDEFINED = (byte)-1;
	// the :1437 masks, post-read
	private static final byte CRUC_ENERGY = (byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A);
	private static final byte CRUC_IN = (byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A);
	private static final byte CRUC_INV_OUT = (byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A);

	private static final String[][] VOLTAGE = {{"lv", "LV"}, {"mv", "MV"}, {"hv", "HV"}, {"ev", "EV"}, {"iv", "IV"}};

	@Test
	void bumblelyzerRowsMatchTheUpstreamColumns() {
		assertEquals(5, GTMachines.BUMBLELYZER_ROWS.size(), "the :1608-1612 five-tier ladder");
		assertEquals(GTMachines.BUMBLELYZER_PARALLEL, 64, "the :1608 NBT_PARALLEL column");
		for (int i = 0; i < 5; i++) {
			GTBasicMachineBlock.MachineRow tRow = GTMachines.BUMBLELYZER_ROWS.get(i);
			String tSuffix = i == 0 ? "" : "_t" + (i + 1);
			assertEquals("bumblelyzer" + tSuffix, tRow.path(), "the path ladder");
			assertEquals(20541 + i, tRow.metaId(), "the meta ladder");
			assertEquals(VOLTAGE[i][0], tRow.matSlug(), "the voltage slug");
			assertEquals(VOLTAGE[i][1], tRow.matDisplay(), "the voltage display");
			assertEquals(4.0F, tRow.hardness(), "the hardness constant");
			assertEquals(i, tRow.tier(), "the tier");
			assertEquals(64, tRow.parallel(), "the PARALLEL 64 column");
			assertFalse(tRow.parallelDuration(), "no NBT_PARALLEL_DURATION — the :770 energy-scaling arm");
			assertSame(GT6RecipeMaps.BUMBLELYZER, tRow.recipes().get(), "RM.Bumblelyzer wiring");
			assertSame(TD.Energy.EU, tRow.energyType(), "the EU energy face");
			assertEquals("bumblelyzer", tRow.texture(), "the texture token");
			assertEquals(BUMBLE_ENERGY, tRow.energySides(), "energy bottom");
			assertEquals(BUMBLE_TANK_IN, tRow.fluidIn(), "tank in U|D");
			assertEquals(NO_KEY, tRow.fluidOut(), "no NBT_TANK_SIDE_OUT key");
			assertEquals(BUMBLE_INV_IN, tRow.itemIn(), "item in U|L");
			assertEquals(BUMBLE_INV_OUT, tRow.itemOut(), "item out R|D");
			assertEquals(1, tRow.fluidAutoIn(), "tank auto TOP");
			assertEquals(UNDEFINED, tRow.fluidAutoOut(), "no tank auto-out key");
			assertEquals(2, tRow.itemAutoIn(), "item auto LEFT");
			assertEquals(4, tRow.itemAutoOut(), "item auto RIGHT");
			assertFalse(tRow.ulvVoltage(), "the legacy material-ladder behaviour");
		}
	}

	@Test
	void crystallisationRowsMatchTheUpstreamColumns() {
		String[] tSlugs = {"steel", "invar", "titanium", "tungsten_carbide"};
		String[] tDisplays = {"Steel", "Invar", "Titanium", "Tungsten Carbide"};
		float[] tHardness = {6.0F, 4.0F, 9.0F, 12.5F};
		assertEquals(4, GTMachines.CRYSTALLISATION_ROWS.size(), "the :1437-1440 Heat_T 4-ladder");
		for (int i = 0; i < 4; i++) {
			GTBasicMachineBlock.MachineRow tRow = GTMachines.CRYSTALLISATION_ROWS.get(i);
			String tSuffix = i == 0 ? "" : "_t" + (i + 1);
			assertEquals("crystallisationcrucible" + tSuffix, tRow.path(), "the path ladder");
			assertEquals(20251 + i, tRow.metaId(), "the meta ladder");
			assertEquals(tSlugs[i], tRow.matSlug(), "the Heat_T material word");
			assertEquals(tDisplays[i], tRow.matDisplay(), "the Heat_T display word");
			assertEquals(tHardness[i], tRow.hardness(), "the hardness ladder");
			assertEquals(i, tRow.tier(), "the tier");
			assertEquals(1, tRow.parallel(), "NO NBT_PARALLEL keys");
			assertFalse(tRow.parallelDuration(), "no NBT_PARALLEL_DURATION key");
			assertSame(GT6RecipeMaps.CRYSTALLISATION_CRUCIBLE, tRow.recipes().get(), "RM.CrystallisationCrucible wiring");
			assertSame(TD.Energy.HU, tRow.energyType(), "the HU energy face (the crucible-chain temperature consumer)");
			assertEquals("crystallisationcrucible", tRow.texture(), "the texture token");
			assertEquals(CRUC_ENERGY, tRow.energySides(), "energy bottom");
			assertEquals(CRUC_IN, tRow.fluidIn(), "tank in L|B|U");
			assertEquals(NO_KEY, tRow.fluidOut(), "no NBT_TANK_SIDE_OUT key — the fluids 3/0/1 face");
			assertEquals(CRUC_IN, tRow.itemIn(), "item in L|B|U");
			assertEquals(CRUC_INV_OUT, tRow.itemOut(), "item out R");
			assertEquals(2, tRow.fluidAutoIn(), "tank auto LEFT");
			assertEquals(UNDEFINED, tRow.fluidAutoOut(), "no tank auto-out key");
			assertEquals(1, tRow.itemAutoIn(), "item auto TOP");
			assertEquals(4, tRow.itemAutoOut(), "item auto RIGHT");
		}
	}

	/** The RM.java:73/:107 constants rows over the port ctor. */
	@Test
	void theTwoMapsCarryTheRMJavaConstantsRows() {
		GT6RecipeMaps.init();
		// RM.java:73 — items 1/1/1, fluids 3/0/1, MIN 1, AMP 1
		assertEquals("gt.recipe.crystallisationcrucible", GT6RecipeMaps.CRYSTALLISATION_CRUCIBLE.mNameInternal, "the :73 unlocalized name");
		assertEquals("Crystallisation Crucible", GT6RecipeMaps.CRYSTALLISATION_CRUCIBLE.mNameLocal, "the :73 local name");
		assertEquals(1, GT6RecipeMaps.CRYSTALLISATION_CRUCIBLE.mInputItemsCount, "items 1/x");
		assertEquals(1, GT6RecipeMaps.CRYSTALLISATION_CRUCIBLE.mMinimalInputItems, "the MIN items column");
		assertEquals(3, GT6RecipeMaps.CRYSTALLISATION_CRUCIBLE.mInputFluidCount, "fluids 3/x");
		assertEquals(1, GT6RecipeMaps.CRYSTALLISATION_CRUCIBLE.mMinimalInputs, "the MIN 1 column");
		// RM.java:107 — the subclass identity (the dynamic scan arm lives on the class)
		assertTrue(GT6RecipeMaps.BUMBLELYZER instanceof GT6RecipeMapBumblelyzer, "RM.Bumblelyzer IS a RecipeMapBumblelyzer upstream");
		assertEquals(0, GT6RecipeMaps.BUMBLELYZER.mRecipeList.size(), "the Bumblelyzer stock is DECLARED-empty (the dynamic arm)");
	}
}
