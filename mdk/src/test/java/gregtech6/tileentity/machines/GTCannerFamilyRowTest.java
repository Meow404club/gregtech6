package gregtech6.tileentity.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import gregapi.data.TD;
import gregtech6.block.GTBasicMachineBlock;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.registry.GTMachines;

/**
 * The Canner-family row acceptance (task p24-canner-machine, the OFFLINE half — the
 * {@link GTDryerFamilyRowTest} shape): the four row records pinned to the upstream columns
 * (Loader_MultiTileEntities.java:1379-1382 — the "Canning Machine ("+VN[tier]+")" name
 * column / MultiTileEntity id 20161-20164 / NBT_HARDNESS 4.0F / NBT_INPUT 32/128/512/2048
 * through the TIER_INPUTS carrier / NBT_ENERGY_ACCEPTED TD.Energy.EU / NBT_RECIPEMAP
 * RM.Canner / NBT_TEXTURE "canner" / NO NBT_PARALLEL → 1 / NO NBT_PARALLEL_DURATION → F /
 * NBT_USE_OUTPUT_TANK T / NBT_TANK_CAPACITY 128000/512000/2048000/8192000 / the
 * connectivity mask columns), the {@link GTMachines#cannerMachine} factory columns (the
 * two registration keys the MachineRow record does not carry), the R2 T5-stays-pooled
 * ruling and the R6 128000 map-ceiling fold. The registration half (4 blocks + 4 items +
 * the 1 family BET) only resolves on a live server (the runServer/RCON gate).
 */
public class GTCannerFamilyRowTest extends TileEntityBasicMachineOfflineTestBase {

	/** CS.java:612 — SBIT_B, the :151 NBT_ENERGY_ACCEPTED_SIDES read form (:1379 verbatim). */
	static final byte ENERGY_IN_MASK = (byte)(GTBasicMachineBlock.SBIT_B);
	/** CS.java:612 — SBIT_U|SBIT_L, the :143/:137 NBT_TANK_SIDE_IN / NBT_INV_SIDE_IN read form. */
	static final byte IN_MASK = (byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_L);
	/** CS.java:612 — SBIT_R|SBIT_D, the :144/:138 NBT_TANK_SIDE_OUT / NBT_INV_SIDE_OUT read form. */
	static final byte OUT_MASK = (byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_D);

	@Test
	void cannerRowsMatchTheUpstreamColumns() {
		assertEquals(4, GTMachines.CANNER_ROWS.size(), "the four Canner rows (:1379-1382) — the R2 T5-stays-pooled ruling");
		String[] tVoltageWords = {"LV", "MV", "HV", "EV"};
		String[] tVoltageSlugs = {"lv", "mv", "hv", "ev"};
		for (int i = 0; i < GTMachines.CANNER_ROWS.size(); i++) {
			GTBasicMachineBlock.MachineRow tRow = GTMachines.CANNER_ROWS.get(i);
			int tTier = i + 1; // the upstream tier index, 1-based in the messages
			assertEquals("canner" + (i == 0 ? "" : "_t" + tTier), tRow.path(), "the registry path ladder");
			assertEquals(20161 + i, tRow.metaId(), "the MultiTile id column of tier " + tTier);
			assertEquals(tTier, tRow.tier() + 1, "the tier index");
			assertEquals(4.0F, tRow.hardness(), "NBT_HARDNESS 4.0F on every row (NBT_RESISTANCE == hardness)");
			assertSame(TD.Energy.EU, tRow.energyType(), "NBT_ENERGY_ACCEPTED TD.Energy.EU on every row");
			assertSame(GT6RecipeMaps.CANNER, tRow.recipes().get(), "NBT_RECIPEMAP RM.Canner through the supplier");
			assertEquals("canner", tRow.texture(), "NBT_TEXTURE canner on every row (the ladder shares the fronts)");
			assertEquals(1, tRow.parallel(), "NO NBT_PARALLEL on the :1379 rows → 1");
			assertFalse(tRow.parallelDuration(), "NO NBT_PARALLEL_DURATION on the :1379 rows → F");
			assertTrue(tRow.cheapOverclocking(), "the port :773 loop runs unconditionally (no config source)");
			assertNotNull(tRow.menu(), "the gt6:canner menu supplier is bound on every row (the offline .get() would touch the unbound RegistryObject)");
			// the VN voltage name column (CS.java:154) — NOT a material word
			assertEquals("gt6.row.canner.display", tRow.displayKey(), "the family template key");
			assertEquals(tVoltageWords[i], tRow.matDisplay(), "the voltage word, VN[" + tTier + "]");
			assertEquals(tVoltageSlugs[i], tRow.matSlug(), "the voltage slug (the gt6.row.mat key tail)");
			// the connectivity + auto-side columns (:1379 verbatim, identical on all four rows)
			assertEquals(ENERGY_IN_MASK, tRow.energySides(), "NBT_ENERGY_ACCEPTED_SIDES SBIT_B through the :151 read");
			assertEquals(IN_MASK, tRow.fluidIn(), "NBT_TANK_SIDE_IN SBIT_U|SBIT_L through the :143 read");
			assertEquals(OUT_MASK, tRow.fluidOut(), "NBT_TANK_SIDE_OUT SBIT_R|SBIT_D through the :144 read");
			assertEquals(IN_MASK, tRow.itemIn(), "NBT_INV_SIDE_IN SBIT_U|SBIT_L through the :137 read");
			assertEquals(OUT_MASK, tRow.itemOut(), "NBT_INV_SIDE_OUT SBIT_R|SBIT_D through the :138 read");
			assertEquals(1, tRow.fluidAutoIn(), "NBT_TANK_SIDE_AUTO_IN SIDE_TOP");
			assertEquals(0, tRow.fluidAutoOut(), "NBT_TANK_SIDE_AUTO_OUT SIDE_BOTTOM");
			assertEquals(2, tRow.itemAutoIn(), "NBT_INV_SIDE_AUTO_IN SIDE_LEFT");
			assertEquals(4, tRow.itemAutoOut(), "NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT");
		}
	}

	/** The factory columns the MachineRow record does not carry: the tank capacity ladder + the use-output-tank key. */
	@Test
	void factoryAppliesTheTankAndOutputTankColumns() {
		long[] tCapacities = {128000L, 512000L, 2048000L, 8192000L}; // NBT_TANK_CAPACITY :1379-1382
		assertEquals(4, GTMachines.CANNER_TANK_CAPACITY.length, "the capacity ladder is tier-indexed");
		for (int i = 0; i < 4; i++) {
			assertEquals(tCapacities[i], GTMachines.CANNER_TANK_CAPACITY[i], "NBT_TANK_CAPACITY of tier " + (i + 1) + " (:1379-1382 verbatim)");
		}
		// the T5 row (:1383) stays pooled WITH its capacity — the R2 ruling covers both columns
		for (long tCapacity : GTMachines.CANNER_TANK_CAPACITY) {
			assertTrue(tCapacity != 32768000L, "the T5 capacity 32768000 (:1383) is NOT in the landed ladder");
		}
		// the R6 fold: the upstream map-level mMaxFluid*Size 128000 ceiling == the T1 capacity
		assertEquals(GTMachines.CANNER_TANK_CAPACITY[0], 128000L, "the R6 fold — mMaxFluid*Size 128000 rides the T1 capacity");
	}

	/**
	 * The {@code cannerMachine} factory seam (the applyRow public-test-seam form): the
	 * two extra registration columns land on a fixture machine — the capacity through
	 * {@code applyTankCapacity} (the :157-160 re-arm) and the output-tank fallback flag.
	 */
	@Test
	void applyFactoryColumnsOnAFixture() {
		GTBasicMachineBlock.MachineRow tRow = GTMachines.CANNER_ROWS.get(2); // T3
		// the machine(aType,...) half, fixture-driven (the makeMachine helper)
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.CANNER, tRow.parallel(), tRow.parallelDuration(), TD.Energy.EU);
		GTMachines.applyRow(tMachine, tRow); // the mask half (the W1a carrier, shared with the Dryer)
		// the cannerMachine-tail columns, applied exactly like the factory body
		tMachine.mCanUseOutputTanks = true; // NBT_USE_OUTPUT_TANK T (:1379)
		tMachine.mTankCapacity = GTMachines.CANNER_TANK_CAPACITY[tRow.tier()];
		tMachine.applyTankCapacity();

		assertEquals(GTMachines.CANNER_TANK_CAPACITY[2], tMachine.mTankCapacity, "the T3 tank capacity landed");
		assertEquals(GTMachines.CANNER_TANK_CAPACITY[2], tMachine.mTanksInput[0].getCapacity(), "the input tank re-armed AT the row capacity (:157-160)");
		// the OUTPUT tanks keep the constructed default (upstream :162 the no-capacity form;
		// NBT_TANK_CAPACITY is the INPUT-tank key upstream, :157-160 only)
		assertTrue(tMachine.mTanksOutput[0].getCapacity() != GTMachines.CANNER_TANK_CAPACITY[2],
				"the output tank is NOT re-armed by applyTankCapacity (the :162 default form)");
		assertTrue(tMachine.mCanUseOutputTanks, "the :716-732 output-tank fallback is armed");
		assertEquals(ENERGY_IN_MASK, tMachine.mEnergyInputs, "the mask half via applyRow");
		assertEquals(IN_MASK, tMachine.mFluidInputs, "the tank-in mask via applyRow");
		assertEquals(OUT_MASK, tMachine.mFluidOutputs, "the tank-out mask via applyRow");
		// the CANNER slot/tank shape (RM.java:148: items 2,2,1 / fluids 1,1,0) — the R7 BE half
		assertEquals(4, tMachine.getInventory().getSlots(), tRow.path() + " the 2+2 slot shape (the second input slot, R7)");
		assertEquals(1, tMachine.mTanksInput.length, tRow.path() + " the 1 input tank");
		assertEquals(1, tMachine.mTanksOutput.length, tRow.path() + " the 1 output tank");
		assertEquals(2, tMachine.getInputSlotCount(), "the data-driven input count the R7 menu rides");
	}

	/** The RecipeMap declaration columns (RM.java:148 — the 2/2 item + 1/1 fluid shape). */
	@Test
	void cannerMapCarriesTheTwoTwoDeclaration() {
		GT6RecipeMaps.init();
		try {
			gregtech6.recipes.maps.GT6RecipeMapCanner tMap = GT6RecipeMaps.CANNER;
			assertNotNull(tMap, "the CANNER map init");
			assertEquals("gt.recipe.canner", tMap.mNameInternal, "the RM.java:148 internal name");
			assertEquals("Canning Machine", tMap.mNameLocal, "the local name column");
			assertEquals(2, tMap.mInputItemsCount, "IN-ITEMS 2 — the R7 two-slot shape");
			assertEquals(2, tMap.mOutputItemsCount, "OUT-ITEMS 2");
			assertEquals(1, tMap.mMinimalInputItems, "MIN-ITEMS 1");
			assertEquals(1, tMap.mInputFluidCount, "IN-FLUIDS 1");
			assertEquals(1, tMap.mOutputFluidCount, "OUT-FLUIDS 1");
			assertEquals(0, tMap.mMinimalInputFluids, "MIN-FLUIDS 0");
			assertEquals(1, tMap.mMinimalInputs, "MIN 1");
			assertEquals("gt6:textures/gui/machines/canner.png", tMap.mGUIPath, "the GUI path column, lowercased");
		} finally {
			GT6RecipeMaps.reset();
		}
	}
}
