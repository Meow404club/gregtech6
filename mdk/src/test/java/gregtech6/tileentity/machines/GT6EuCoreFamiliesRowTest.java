package gregtech6.tileentity.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import gregapi.data.MT;
import gregapi.data.TD;
import gregtech6.block.GTBasicMachineBlock;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.Recipe;
import gregtech6.registry.GTMachines;

/**
 * The eu-core families row acceptance (task p29-w2-eu-core-5tier, the OFFLINE half — the
 * {@link GT6EuHuFamiliesRowTest} shape): the five families pinned to the upstream columns
 * (Loader_MultiTileEntities.java :1336-1340 Electrolyzer / :1443-1447 Injector /
 * :1450-1454 Printer / :1457-1461 Scanner (Visuals) / :1525-1529 Slicer), the FIRST
 * 5-tier ladders of the port (Electric_T[1..5], the T5 rung {@link GTMachines#ELECTRIC_T5}
 * = Ti, MT.java:3691), the 5-tier windows ({@link GTMachines#TIER_INPUTS}[0..3] + the
 * {@link GTMachines#EV_TIER_INPUTS} tier-4 arm — the euFiveTierMachine factory (the card-③ landed 5-tier resolver, the merge dedup of the authored euCoreMachine body folded onto it), the
 * T5 packet-domain behavior (4095 dead below min, 8192 mid-window runs), the
 * ELECTROLYZER_PARALLEL {1,2,4,8,16} duration-T ladder 对拍 the Centrifuge non-standard
 * precedent, the EU type gate live against the exotic packets (the cross-card-①
 * MU/LU guard) and the display voltage words VN[1..5] = LV/MV/HV/EV/IV (CS.java:154 —
 * T5 = "IV", NOT "EV", the S9 ruling).
 *
 * <p>The registration half (25 blocks + 25 items + 5 family BETs) only resolves on a
 * live server (the runServer/RCON gate — the sweep p29_w2_eu_core group).
 */
public class GT6EuCoreFamiliesRowTest extends TileEntityBasicMachineOfflineTestBase {

	/** :1336 masks, post-read (the port rows carry the OR-SBIT_A form) — item+tank share every mask. */
	private static final byte ELECTROLYZER_ENERGY = (byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A);
	private static final byte ELECTROLYZER_IN = (byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_F | GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_A);
	private static final byte ELECTROLYZER_OUT = (byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A);
	/** :1443/:1450/:1457/:1525 masks — in U|L (Slicer L|U), out R|D, energy back. */
	private static final byte EU_IN = (byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A);
	private static final byte SLICER_IN = (byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A);
	private static final byte EU_OUT = (byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A);
	private static final byte EU_ENERGY = (byte)(GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_A);
	private static final byte NO_TANK = (byte)127;

	/** The five path ladders (T1 bare, T2-T5 the _tN suffix) — the T5 suffix is NEW to the port. */
	private static String path(String aFamily, int aTier) {
		return aTier == 0 ? aFamily : aFamily + "_t" + (aTier + 1);
	}

	/** The voltage words per tier — VN[1..5] = LV/MV/HV/EV/IV (CS.java:154, the S9 T5 = IV ruling). */
	private static final String[] T5_WORDS = {"LV", "MV", "HV", "EV", "IV"};
	private static final String[] T5_SLUGS = {"lv", "mv", "hv", "ev", "iv"};

	// ------------------------------------------------------------------
	// the registration rows vs the upstream columns
	// ------------------------------------------------------------------

	@Test
	void electrolyzerRowsMatchTheUpstreamColumns() {
		assertEquals(5, GTMachines.ELECTROLYZER_ROWS.size(), "the FIVE Electrolyzer rows (:1336-1340) — the first 5-tier ladder");
		assertArrayRowCheck(GTMachines.ELECTROLYZER_ROWS, 20091, "electrolyzer", "gt6.row.electrolyzer.display",
				GT6RecipeMaps.ELECTROLYZER, "electrolyzer");
		for (int i = 0; i < GTMachines.ELECTROLYZER_ROWS.size(); i++) {
			GTBasicMachineBlock.MachineRow tRow = GTMachines.ELECTROLYZER_ROWS.get(i);
			assertEquals(GTMachines.ELECTROLYZER_PARALLEL[i], tRow.parallel(), "NBT_PARALLEL {1,2,4,8,16} — the FIRST five-rung table");
			assertTrue(tRow.parallelDuration(), "NBT_PARALLEL_DURATION T :1336");
			assertEquals(ELECTROLYZER_ENERGY, tRow.energySides(), "NBT_ENERGY_ACCEPTED_SIDES SBIT_D — the bottom energy face");
			assertEquals(ELECTROLYZER_IN, tRow.itemIn(), "NBT_INV_SIDE_IN SBIT_U|SBIT_F|SBIT_B");
			assertEquals(ELECTROLYZER_OUT, tRow.itemOut(), "NBT_INV_SIDE_OUT SBIT_R|SBIT_L");
			assertEquals(ELECTROLYZER_IN, tRow.fluidIn(), "NBT_TANK_SIDE_IN == the item-in mask (:1336)");
			assertEquals(ELECTROLYZER_OUT, tRow.fluidOut(), "NBT_TANK_SIDE_OUT == the item-out mask");
			assertEquals(1, tRow.fluidAutoIn(), "NBT_TANK_SIDE_AUTO_IN SIDE_TOP");
			assertEquals(2, tRow.fluidAutoOut(), "NBT_TANK_SIDE_AUTO_OUT SIDE_LEFT — the tank/item auto-out divergence");
			assertEquals(1, tRow.itemAutoIn(), "NBT_INV_SIDE_AUTO_IN SIDE_TOP");
			assertEquals(4, tRow.itemAutoOut(), "NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT");
		}
	}

	@Test
	void injectorRowsMatchTheUpstreamColumns() {
		assertEquals(5, GTMachines.INJECTOR_ROWS.size(), "the FIVE Injector rows (:1443-1447)");
		assertArrayRowCheck(GTMachines.INJECTOR_ROWS, 20261, "injector", "gt6.row.injector.display",
				GT6RecipeMaps.INJECTOR, "injector");
		for (int i = 0; i < GTMachines.INJECTOR_ROWS.size(); i++) {
			GTBasicMachineBlock.MachineRow tRow = GTMachines.INJECTOR_ROWS.get(i);
			assertEquals(1, tRow.parallel(), "NO NBT_PARALLEL on the :1443 rows → 1");
			assertFalse(tRow.parallelDuration(), "NO NBT_PARALLEL_DURATION → F");
			assertEquals(EU_ENERGY, tRow.energySides(), "NBT_ENERGY_ACCEPTED_SIDES SBIT_B");
			assertEquals(EU_IN, tRow.itemIn(), "NBT_INV_SIDE_IN SBIT_U|SBIT_L");
			assertEquals(EU_OUT, tRow.itemOut(), "NBT_INV_SIDE_OUT SBIT_R|SBIT_D");
			assertEquals(EU_IN, tRow.fluidIn(), "NBT_TANK_SIDE_IN == the item-in mask");
			assertEquals(EU_OUT, tRow.fluidOut(), "NBT_TANK_SIDE_OUT == the item-out mask");
			assertEquals(1, tRow.fluidAutoIn(), "NBT_TANK_SIDE_AUTO_IN SIDE_TOP");
			assertEquals(0, tRow.fluidAutoOut(), "NBT_TANK_SIDE_AUTO_OUT SIDE_BOTTOM");
			assertEquals(2, tRow.itemAutoIn(), "NBT_INV_SIDE_AUTO_IN SIDE_LEFT");
			assertEquals(4, tRow.itemAutoOut(), "NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT");
		}
	}

	@Test
	void printerRowsMatchTheUpstreamColumns() {
		assertEquals(5, GTMachines.PRINTER_ROWS.size(), "the FIVE Printer rows (:1450-1454)");
		assertArrayRowCheck(GTMachines.PRINTER_ROWS, 20271, "printer", "gt6.row.printer.display",
				GT6RecipeMaps.PRINTER, "printer");
		for (int i = 0; i < GTMachines.PRINTER_ROWS.size(); i++) {
			GTBasicMachineBlock.MachineRow tRow = GTMachines.PRINTER_ROWS.get(i);
			assertEquals(1, tRow.parallel(), "NO NBT_PARALLEL on the :1450 rows → 1");
			assertEquals(EU_ENERGY, tRow.energySides(), "NBT_ENERGY_ACCEPTED_SIDES SBIT_B");
			assertEquals(EU_IN, tRow.itemIn());
			assertEquals(EU_OUT, tRow.itemOut());
			assertEquals(EU_IN, tRow.fluidIn(), "NBT_TANK_SIDE_IN SBIT_U|SBIT_L (:1450)");
			assertEquals(NO_TANK, tRow.fluidOut(), "NO NBT_TANK_SIDE_OUT key → the 127 field default");
			assertEquals(1, tRow.fluidAutoIn(), "NBT_TANK_SIDE_AUTO_IN SIDE_TOP");
			assertEquals(-1, tRow.fluidAutoOut(), "NO NBT_TANK_SIDE_AUTO_OUT key → SIDE_UNDEFINED");
			assertEquals(2, tRow.itemAutoIn(), "NBT_INV_SIDE_AUTO_IN SIDE_LEFT");
			assertEquals(4, tRow.itemAutoOut(), "NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT");
			assertNull(tRow.menu(), "the menu-less carrier");
		}
	}

	@Test
	void scannerVisualsRowsMatchTheUpstreamColumns() {
		assertEquals(5, GTMachines.SCANNER_VISUALS_ROWS.size(), "the FIVE Scanner (Visuals) rows (:1457-1461)");
		assertArrayRowCheck(GTMachines.SCANNER_VISUALS_ROWS, 20281, "scannervisuals", "gt6.row.scannervisuals.display",
				GT6RecipeMaps.SCANNER_VISUALS, "scannervisuals");
		for (int i = 0; i < GTMachines.SCANNER_VISUALS_ROWS.size(); i++) {
			GTBasicMachineBlock.MachineRow tRow = GTMachines.SCANNER_VISUALS_ROWS.get(i);
			assertEquals(1, tRow.parallel(), "NO NBT_PARALLEL on the :1457 rows → 1");
			assertEquals(EU_ENERGY, tRow.energySides(), "NBT_ENERGY_ACCEPTED_SIDES SBIT_B");
			assertEquals(EU_IN, tRow.itemIn());
			assertEquals(EU_OUT, tRow.itemOut());
			assertEquals(NO_TANK, tRow.fluidIn(), "NO NBT_TANK keys on the :1457 rows — the zero-fluid face");
			assertEquals(NO_TANK, tRow.fluidOut());
			assertEquals(-1, tRow.fluidAutoIn(), "NO NBT_TANK_SIDE_AUTO_IN key → SIDE_UNDEFINED");
			assertEquals(-1, tRow.fluidAutoOut(), "NO NBT_TANK_SIDE_AUTO_OUT key → SIDE_UNDEFINED");
			assertEquals(2, tRow.itemAutoIn(), "NBT_INV_SIDE_AUTO_IN SIDE_LEFT");
			assertEquals(4, tRow.itemAutoOut(), "NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT");
		}
	}

	@Test
	void slicerRowsMatchTheUpstreamColumns() {
		assertEquals(5, GTMachines.SLICER_ROWS.size(), "the FIVE Slicer rows (:1525-1529)");
		assertArrayRowCheck(GTMachines.SLICER_ROWS, 20381, "slicer", "gt6.row.slicer.display",
				GT6RecipeMaps.SLICER, "slicer");
		for (int i = 0; i < GTMachines.SLICER_ROWS.size(); i++) {
			GTBasicMachineBlock.MachineRow tRow = GTMachines.SLICER_ROWS.get(i);
			assertEquals(1, tRow.parallel(), "NO NBT_PARALLEL on the :1525 rows → 1");
			assertEquals(EU_ENERGY, tRow.energySides(), "NBT_ENERGY_ACCEPTED_SIDES SBIT_B");
			assertEquals(SLICER_IN, tRow.itemIn(), "NBT_INV_SIDE_IN SBIT_L|SBIT_U (:1525) — the Slicer word order");
			assertEquals(EU_OUT, tRow.itemOut(), "NBT_INV_SIDE_OUT SBIT_R|SBIT_D");
			assertEquals(NO_TANK, tRow.fluidIn(), "NO NBT_TANK keys on the :1525 rows");
			assertEquals(NO_TANK, tRow.fluidOut());
			assertEquals(-1, tRow.fluidAutoIn(), "NO NBT_TANK_SIDE_AUTO_IN key → SIDE_UNDEFINED");
			assertEquals(-1, tRow.fluidAutoOut(), "NO NBT_TANK_SIDE_AUTO_OUT key → SIDE_UNDEFINED");
			assertEquals(2, tRow.itemAutoIn(), "NBT_INV_SIDE_AUTO_IN SIDE_LEFT");
			assertEquals(4, tRow.itemAutoOut(), "NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT");
		}
	}

	/** The shared row-column columns: paths/ids/tiers/hardness/materials/words/energy/map/texture/no-efficiency. */
	private static void assertArrayRowCheck(java.util.List<GTBasicMachineBlock.MachineRow> aRows, int aBaseId,
			String aFamily, String aDisplayKey, gregtech6.recipes.RecipeMap aMap, String aTexture) {
		for (int i = 0; i < aRows.size(); i++) {
			GTBasicMachineBlock.MachineRow tRow = aRows.get(i);
			assertEquals(path(aFamily, i), tRow.path(), "the tier path (the _t5 suffix is NEW to the port)");
			assertEquals(aBaseId + i, tRow.metaId(), "the MultiTile id column of tier " + (i + 1));
			assertEquals(i, tRow.tier(), "the tier index 0..4");
			assertEquals(4.0F, tRow.hardness(), "NBT_HARDNESS 4.0F on EVERY row (NBT_RESISTANCE == hardness)");
			assertSame(TD.Energy.EU, tRow.energyType(), "NBT_ENERGY_ACCEPTED TD.Energy.EU — the Electric carrier");
			assertSame(aMap, tRow.recipes().get(), "NBT_RECIPEMAP");
			assertEquals(aTexture, tRow.texture(), "NBT_TEXTURE");
			assertNull(tRow.efficiency(), "NO NBT_EFFICIENCY key — the :96 10000 identity");
			assertNull(tRow.menu(), "the menu-less carrier");
			assertEquals(aDisplayKey, tRow.displayKey(), "the voltage-word slot template");
			assertEquals(T5_WORDS[i], tRow.matDisplay(), "VN[" + (i + 1) + "] (CS.java:154) — T5 is IV, NOT EV");
			assertEquals(T5_SLUGS[i], tRow.matSlug(), "the gt6.row.mat slug");
			// the material column: Electric_T[1..4] through the ladder, the T5 rung Ti
			assertSame(i < 4
					? GTMachines.ELECTRIC_T_LADDER.get(i).get()
					: GTMachines.ELECTRIC_T5.get(), tRow.material().get(),
					aFamily + " row " + i + " carries its Electric_T material column (MT.java:3691)");
		}
	}

	// ------------------------------------------------------------------
	// the 5-tier windows: TIER_INPUTS[0..3] + the EV_TIER_INPUTS T5 arm
	// ------------------------------------------------------------------

	/** Injects {@code aTicks} packets of {@code aSize} through the grid rig (one doInject + one tick per iteration, the /gt6machine inject order). */
	private static void inject(TileEntityBasicMachine aMachine, int aTicks, long aSize) {
		for (int i = 0; i < aTicks; i++) {
			aMachine.doInject(aMachine.mEnergyTypeAccepted, (byte)0, aSize, 1, true);
			aMachine.updateEntity();
		}
	}

	/** Builds a row-config fixture machine: the euFiveTierMachine body contract (window + applyRow) against the offline fixture. */
	private static TileEntityBasicMachine rowMachine(GTBasicMachineBlock.MachineRow aRow) {
		TileEntityBasicMachine tMachine = makeMachine(aRow.recipes().get(), aRow.parallel(), aRow.parallelDuration(), aRow.energyType());
		long[] tInputs = aRow.tier() < GTMachines.TIER_INPUTS.length
				? GTMachines.TIER_INPUTS[aRow.tier()]
				: GTMachines.EV_TIER_INPUTS; // the euFiveTierMachine T5 arm (the machineUlv override form)
		tMachine.mInputMin = tInputs[0];
		tMachine.mInput = tInputs[1];
		tMachine.mInputMax = tInputs[2];
		return GTMachines.applyRow(tMachine, aRow);
	}

	@Test
	void theFiveTierWindowsRampThroughEvTierInputs() {
		// the per-row windows: TIER_INPUTS[0..3] for T1-T4 and EV_TIER_INPUTS for T5 —
		// read through the euFiveTierMachine body contract on EVERY family
		for (java.util.List<GTBasicMachineBlock.MachineRow> tRows : java.util.List.of(
				GTMachines.ELECTROLYZER_ROWS, GTMachines.INJECTOR_ROWS, GTMachines.PRINTER_ROWS,
				GTMachines.SCANNER_VISUALS_ROWS, GTMachines.SLICER_ROWS)) {
			for (int i = 0; i < tRows.size(); i++) {
				TileEntityBasicMachine tMachine = rowMachine(tRows.get(i));
				long[] tWindow = i < 4 ? GTMachines.TIER_INPUTS[i] : GTMachines.EV_TIER_INPUTS;
				assertEquals(tWindow[0], tMachine.mInputMin, "min of tier " + (i + 1));
				assertEquals(tWindow[1], tMachine.mInput, "in of tier " + (i + 1));
				assertEquals(tWindow[2], tMachine.mInputMax, "max of tier " + (i + 1));
			}
		}
		// the gapless doubling chain across the 5-tier seam (the T4 max == the T5 min)
		assertEquals(4096L, rowMachine(GTMachines.ELECTROLYZER_ROWS.get(4)).mInputMin, "the T5 window min 4096 = the :126 conversion over 8192");
	}

	@Test
	void theT5WindowRunsMidPacketsAndRefusesBelowMin() {
		// the electrolyzer smoke-row shape (2 clay → 1 brick, eUt 16, duration 16 — the
		// TWO-entry form: each recipe input binds one slot, the count gate is per slot)
		GT6RecipeMaps.ELECTROLYZER.addRecipe(new Recipe(true,
				new ItemStack[] {new ItemStack(Items.CLAY_BALL, 1), new ItemStack(Items.CLAY_BALL, 1)},
				new ItemStack[] {new ItemStack(Items.BRICK, 1)},
				null, null, 16, 16, 0));
		TileEntityBasicMachine.ENERGY_FAKE_SOURCE = false;

		TileEntityBasicMachine tT5 = rowMachine(GTMachines.ELECTROLYZER_ROWS.get(4));
		assertEquals(4096, tT5.mInputMin, "the T5 window floor");
		assertEquals(16384, tT5.mInputMax, "the T5 window ceiling");
		// the overclock loop: eUt 16 folds 4x/2x up to the min → mMinEnergy 4096 / bar 4096.
		// the probe feeds TWO slots (the MIN-2 maps count occupied input SOURCES, :708-710 —
		// a single 4-stack slot reads one)
		tT5.getInventory().insertItem(0, new ItemStack(Items.CLAY_BALL, 1), false);
		tT5.getInventory().insertItem(1, new ItemStack(Items.CLAY_BALL, 1), false);
		assertEquals(TileEntityBasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tT5.checkRecipe(true, true));
		assertEquals(4096L, tT5.mMinEnergy, "the :773 loop lands mMinEnergy on the window floor");
		assertEquals(4096L, tT5.mMaxProgress, "units(16 x 16 x 1, 10000, 10000, T) = 256 folded x16 by the 4x/2x loop (ONE input set binds — the parallel 16 cannot exceed the stock)");

		// BELOW the window: 4095 packets are refused outright — the buffer never fills
		for (int i = 0; i < 40; i++) inject(tT5, 1, 4095);
		assertEquals(0, tT5.mProgress, "progress 0 — the 4095 packet is dead below the T5 floor (拒 arm)");

		// MID-window: the 8192 packet runs (跑通 arm) — one tick consumes the folded
		// 4096/tick and completes the bar (4096/4096), the brick lands in the output slot.
		// the bind CONSUMED the first pair, so re-feed before the second bind
		tT5.getInventory().insertItem(0, new ItemStack(Items.CLAY_BALL, 1), false);
		tT5.getInventory().insertItem(1, new ItemStack(Items.CLAY_BALL, 1), false);
		assertEquals(TileEntityBasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tT5.checkRecipe(true, true));
		inject(tT5, 1, 8192);
		assertTrue(tT5.getInventory().getStackInSlot(tT5.getInputSlotCount()).getCount() >= 1,
				"the brick output landed on the in-window 8192 packet");
	}

	// ------------------------------------------------------------------
	// the ELECTROLYZER_PARALLEL duration-T ladder 对拍 the Centrifuge precedent
	// ------------------------------------------------------------------

	@Test
	void electrolyzerParallelLadderMatchesTheCentrifugeDurationPrecedent() {
		// the SAME probe row on BOTH maps: 2 clay → 1 brick, eUt 16, duration 16 (the
		// Centrifuge's live-chain row shape: 2 snowballs → clay + flint, NBT_PARALLEL_DURATION T)
		GT6RecipeMaps.ELECTROLYZER.addRecipe(new Recipe(true,
				new ItemStack[] {new ItemStack(Items.CLAY_BALL, 1), new ItemStack(Items.CLAY_BALL, 1)},
				new ItemStack[] {new ItemStack(Items.BRICK, 1)},
				null, null, 16, 16, 0));
		GT6RecipeMaps.CENTRIFUGE.addRecipe(new Recipe(true,
				new ItemStack[] {new ItemStack(Items.CLAY_BALL, 2)},
				new ItemStack[] {new ItemStack(Items.CLAY_BALL, 1), new ItemStack(Items.FLINT, 1)},
				null, null, 16, 16, 0));
		TileEntityBasicMachine.ENERGY_FAKE_SOURCE = false;

		// T1 (parallel 1) is the baseline bar
		TileEntityBasicMachine tT1 = rowMachine(GTMachines.ELECTROLYZER_ROWS.get(0));
		tT1.getInventory().insertItem(0, new ItemStack(Items.CLAY_BALL, 1), false);
		tT1.getInventory().insertItem(1, new ItemStack(Items.CLAY_BALL, 1), false);
		assertEquals(TileEntityBasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tT1.checkRecipe(true, true));
		assertEquals(256L, tT1.mMaxProgress, "units(16 x 16 x 1, 10000, 10000, T) = 256 — the parallel-1 bar");
		assertEquals(1, tT1.mParallel, "the T1 = 1 arm — the NON-standard face (no 4/8/16/32 base)");

		// the {1,2,4,8,16} ladder read back row by row
		for (int i = 0; i < GTMachines.ELECTROLYZER_ROWS.size(); i++) {
			assertEquals(GTMachines.ELECTROLYZER_PARALLEL[i], GTMachines.ELECTROLYZER_ROWS.get(i).parallel(),
					"the parallel column of tier " + (i + 1));
		}

		// 对拍 the Centrifuge T2 (parallel 2, duration T): the SAME duration-linear face —
		// with 4 clay in the slot both machines bind tMaxProcessCount = 2, the bar doubles
		// (2 x 256) and the outputs double (2 rows consumed, 2x outputs per completion)
		TileEntityBasicMachine tElecT2 = rowMachine(GTMachines.ELECTROLYZER_ROWS.get(1));
		TileEntityBasicMachine tCentT2 = rowMachine(GTMachines.CENTRIFUGE_ROWS.get(1));
		assertEquals(2, tElecT2.mParallel, "the electrolyzer T2 parallel 2 (:1337)");
		assertEquals(2, tCentT2.mParallel, "the centrifuge T2 parallel 2 (CENTRIFUGE_PARALLEL[1]) — the precedent");
		tElecT2.getInventory().insertItem(0, new ItemStack(Items.CLAY_BALL, 2), false);
		tElecT2.getInventory().insertItem(1, new ItemStack(Items.CLAY_BALL, 2), false);
		// the centrifuge map carries ONE input slot (mInputItemsCount 1) — its row is the
		// single-slot 2-clay form, the stock 4 covers the same 2 parallel sets
		tCentT2.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.CLAY_BALL, 4), false);
		assertEquals(TileEntityBasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tElecT2.checkRecipe(true, true));
		assertEquals(TileEntityBasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tCentT2.checkRecipe(true, true));
		assertEquals(tCentT2.mMaxProgress, tElecT2.mMaxProgress, "the 对拍: units(16 x 16 x 2, 10000, 10000, T) folded by the :773 loop on BOTH duration-T ladders");
		assertEquals(1024L, tElecT2.mMaxProgress, "2 x 256 = 512 pre-fold, x2 by the 4x/2x loop (mMinEnergy 16 → 64 = the T2 window floor)");

		// drive to done: the bar 1024 at mMinEnergy 64/tick — 16 ticks of in-window 128
		// packets; the ONE completion drops 2x brick
		for (int i = 0; i < 24; i++) inject(tElecT2, 1, 128);
		assertTrue(tElecT2.getInventory().getStackInSlot(tElecT2.getInputSlotCount()).getCount() >= 2,
				"2x brick from the parallel-2 completion (the outputs xN face)");
	}

	// ------------------------------------------------------------------
	// the EU type gate: the exotic packets are refused (cross-card-① guard)
	// ------------------------------------------------------------------

	@Test
	void euMachineRefusesTheExoticPackets() {
		GT6RecipeMaps.ELECTROLYZER.addRecipe(new Recipe(true,
				new ItemStack[] {new ItemStack(Items.CLAY_BALL, 1), new ItemStack(Items.CLAY_BALL, 1)},
				new ItemStack[] {new ItemStack(Items.BRICK, 1)},
				null, null, 16, 16, 0));
		TileEntityBasicMachine.ENERGY_FAKE_SOURCE = false;

		TileEntityBasicMachine tMachine = rowMachine(GTMachines.ELECTROLYZER_ROWS.get(0));
		assertSame(TD.Energy.EU, tMachine.mEnergyTypeAccepted, "the EU carrier");
		tMachine.getInventory().insertItem(0, new ItemStack(Items.CLAY_BALL, 1), false);
		tMachine.getInventory().insertItem(1, new ItemStack(Items.CLAY_BALL, 1), false);

		// MU and LU trains (the card-① source-dial types) are REFUSED by the :501
		// reference-equality gate — the buffer never fills, the machine never starts
		for (int i = 0; i < 40; i++) tMachine.doInject(TD.Energy.MU, (byte)0, 32, 1, true);
		for (int i = 0; i < 40; i++) tMachine.doInject(TD.Energy.LU, (byte)0, 32, 1, true);
		assertEquals(0, tMachine.mEnergy, "the exotic packets never crossed the type gate");
		assertEquals(0, tMachine.mProgress, "no progress on a refused carrier");

		// the control: the SAME machine runs on its own EU carrier
		assertEquals(TileEntityBasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tMachine.checkRecipe(true, true));
		inject(tMachine, 1, 32);
		assertTrue(tMachine.mProgress > 0, "the EU packet advances the EU machine");
	}
}
