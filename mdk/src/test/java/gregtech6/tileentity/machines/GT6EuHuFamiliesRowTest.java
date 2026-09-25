package gregtech6.tileentity.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

import gregapi.data.TD;
import gregtech6.block.GTBasicMachineBlock;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.Recipe;
import gregtech6.registry.GTMachines;

/**
 * The eu-hu families row acceptance (task p29-w1-eu-hu-families, the OFFLINE half — the
 * {@link GTCannerFamilyRowTest} shape): the seven families pinned to the upstream columns
 * (Loader_MultiTileEntities.java :1392-1395 Mixer RU / :1504-1508 ElectricMixer /
 * :1511-1515 ElectricLoom / :1518-1522 ElectricSifter / :1635-1639 Boxinator /
 * :1642-1646 Unboxinator / :1654 Fermenter), the shared-map pair semantics (kinetic and
 * electric mixer rows ride the ONE GT6RecipeMaps.MIXER instance; the electric sifter the
 * ONE GT6RecipeMaps.SIFTING instance), the efficiency-direction 共图对拍 (the card-A
 * erratum: units(a, orig, targ) = a × targ/orig — 5000 = 2× the REQUIRED progress =
 * half speed / 2× the energy-time, NOT a 2× speed-up; UT.java:1677 + LH.java:311 double
 * evidence), the TIER_INPUTS windows (the fermenter's upstream explicit 16/32/64 ==
 * TIER_INPUTS[0]), the pooled Unboxinator loot arm (the base-RecipeMap deviation) and
 * the T5-stays-pooled deviation (the Canner T5 ledger).
 *
 * <p>The registration half (25 blocks + 25 items + 7 family BETs) only resolves on a
 * live server (the runServer/RCON gate — the sweep eu_hu group).
 */
public class GT6EuHuFamiliesRowTest extends TileEntityBasicMachineOfflineTestBase {

	/** :1392/:1504 masks, post-read (the port rows carry the OR-SBIT_A form). */
	private static final byte MIXER_ENERGY = (byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A);
	private static final byte MIXER_IN = (byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A);
	private static final byte MIXER_OUT = (byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_A);
	/** :1511/:1518 masks — top in, bottom out, NO tank keys. */
	private static final byte TOP_IN = (byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A);
	private static final byte BOTTOM_OUT = (byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A);
	private static final byte NO_TANK = (byte)127;
	/** :1511 — the ElectricLoom energy face takes BOTH side faces. */
	private static final byte LOOM_ENERGY = (byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A);
	/** :1654 masks — item+tank in back|left, item out right, tank out top. */
	private static final byte FERMENTER_IN = (byte)(GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A);
	private static final byte FERMENTER_ITEM_OUT = (byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A);
	private static final byte FERMENTER_TANK_OUT = (byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A);

	// ------------------------------------------------------------------
	// the registration rows vs the upstream columns
	// ------------------------------------------------------------------

	@Test
	void mixerRowsMatchTheUpstreamColumns() {
		assertEquals(4, GTMachines.MIXER_ROWS.size(), "the four Mixer rows (:1392-1395)");
		String[] tSlugs = {"bronze", "steel", "titanium", "tungstensteel"};
		for (int i = 0; i < GTMachines.MIXER_ROWS.size(); i++) {
			GTBasicMachineBlock.MachineRow tRow = GTMachines.MIXER_ROWS.get(i);
			assertEquals("mixer" + (i == 0 ? "" : "_t" + (i + 1)), tRow.path());
			assertEquals(20181 + i, tRow.metaId(), "the MultiTile id column of tier " + (i + 1));
			assertEquals(i, tRow.tier(), "the TIER_INPUTS tier index");
			float[] tHardness = {7.0F, 6.0F, 9.0F, 12.5F};
			assertEquals(tHardness[i], tRow.hardness(), "the kinetic hardness ladder");
			assertSame(TD.Energy.RU, tRow.energyType(), "NBT_ENERGY_ACCEPTED TD.Energy.RU (:1392)");
			assertSame(GT6RecipeMaps.MIXER, tRow.recipes().get(), "NBT_RECIPEMAP RM.Mixer");
			assertEquals("mixer", tRow.texture(), "NBT_TEXTURE mixer");
			assertEquals(GTMachines.PARALLEL_4_32[i], tRow.parallel(), "NBT_PARALLEL 4/8/16/32");
			assertTrue(tRow.parallelDuration(), "NBT_PARALLEL_DURATION T");
			assertNull(tRow.efficiency(), "the kinetic row carries NO NBT_EFFICIENCY key — the :96 10000 identity");
			assertNull(tRow.menu(), "the menu-less carrier");
			assertEquals("gt6.row.machine.mixer", tRow.displayKey(), "the material-word one-slot form");
			assertEquals(tSlugs[i], tRow.matSlug());
			assertEquals(MIXER_ENERGY, tRow.energySides(), "NBT_ENERGY_ACCEPTED_SIDES SBIT_D through the :151 read");
			assertEquals(MIXER_IN, tRow.itemIn(), "NBT_INV_SIDE_IN SBIT_L|SBIT_U");
			assertEquals(MIXER_OUT, tRow.itemOut(), "NBT_INV_SIDE_OUT SBIT_R|SBIT_B");
			assertEquals(MIXER_IN, tRow.fluidIn(), "NBT_TANK_SIDE_IN SBIT_L|SBIT_U");
			assertEquals(MIXER_OUT, tRow.fluidOut(), "NBT_TANK_SIDE_OUT SBIT_R|SBIT_B");
			assertEquals(1, tRow.fluidAutoIn(), "NBT_TANK_SIDE_AUTO_IN SIDE_TOP");
			assertEquals(5, tRow.fluidAutoOut(), "NBT_TANK_SIDE_AUTO_OUT SIDE_BACK");
			assertEquals(2, tRow.itemAutoIn(), "NBT_INV_SIDE_AUTO_IN SIDE_LEFT");
			assertEquals(4, tRow.itemAutoOut(), "NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT");
		}
	}

	@Test
	void electricFamilyRowsMatchTheUpstreamColumns() {
		String[] tWords = {"LV", "MV", "HV", "EV"};
		String[] tSlugs = {"lv", "mv", "hv", "ev"};
		// ElectricMixer :1504-1507 — the SHARED RM.Mixer map, efficiency 5000, parallel 4/8/16/32 + T
		for (int i = 0; i < GTMachines.ELECTRIC_MIXER_ROWS.size(); i++) {
			GTBasicMachineBlock.MachineRow tRow = GTMachines.ELECTRIC_MIXER_ROWS.get(i);
			assertEquals("electricmixer" + (i == 0 ? "" : "_t" + (i + 1)), tRow.path());
			assertEquals(20351 + i, tRow.metaId());
			assertSame(TD.Energy.EU, tRow.energyType(), "NBT_ENERGY_ACCEPTED TD.Energy.EU");
			assertSame(GT6RecipeMaps.MIXER, tRow.recipes().get(), "the SHARED RM.Mixer map — the 共图对 kinetic arm");
			assertEquals("electricmixer", tRow.texture());
			assertEquals(GTMachines.PARALLEL_4_32[i], tRow.parallel(), "NBT_PARALLEL 4/8/16/32 (:1504)");
			assertTrue(tRow.parallelDuration(), "NBT_PARALLEL_DURATION T");
			assertEquals(GTMachines.ELECTRIC_EFFICIENCY, tRow.efficiency().intValue(), "NBT_EFFICIENCY 5000 — the half-speed face");
			assertEquals(4.0F, tRow.hardness(), "NBT_HARDNESS 4.0F on every Electric row");
			assertEquals("gt6.row.electricmixer.display", tRow.displayKey(), "the voltage-word slot");
			assertEquals(tWords[i], tRow.matDisplay());
			assertEquals(tSlugs[i], tRow.matSlug());
			assertEquals(MIXER_ENERGY, tRow.energySides(), "the :1504 masks verbatim");
			assertEquals(MIXER_IN, tRow.itemIn());
			assertEquals(MIXER_OUT, tRow.itemOut());
			assertEquals(MIXER_IN, tRow.fluidIn());
			assertEquals(MIXER_OUT, tRow.fluidOut());
		}
		// ElectricLoom :1511-1514 — RM.Loom, efficiency 5000, no parallel, top-in/bottom-out, BOTH side energy faces
		for (int i = 0; i < GTMachines.ELECTRIC_LOOM_ROWS.size(); i++) {
			GTBasicMachineBlock.MachineRow tRow = GTMachines.ELECTRIC_LOOM_ROWS.get(i);
			assertEquals("electricloom" + (i == 0 ? "" : "_t" + (i + 1)), tRow.path());
			assertEquals(20361 + i, tRow.metaId());
			assertSame(GT6RecipeMaps.LOOM, tRow.recipes().get());
			assertEquals("electricloom", tRow.texture());
			assertEquals(1, tRow.parallel(), "NO NBT_PARALLEL on the :1511 rows → 1");
			assertFalse(tRow.parallelDuration(), "NO NBT_PARALLEL_DURATION → F");
			assertEquals(GTMachines.ELECTRIC_EFFICIENCY, tRow.efficiency().intValue());
			assertEquals(LOOM_ENERGY, tRow.energySides(), "NBT_ENERGY_ACCEPTED_SIDES SBIT_L|SBIT_R — both sides (:1511)");
			assertEquals(TOP_IN, tRow.itemIn(), "NBT_INV_SIDE_IN SBIT_U — top in");
			assertEquals(BOTTOM_OUT, tRow.itemOut(), "NBT_INV_SIDE_OUT SBIT_D — bottom out");
			assertEquals(1, tRow.itemAutoIn(), "NBT_INV_SIDE_AUTO_IN SIDE_TOP");
			assertEquals(0, tRow.itemAutoOut(), "NBT_INV_SIDE_AUTO_OUT SIDE_BOTTOM");
			assertEquals(NO_TANK, tRow.fluidIn(), "no NBT_TANK keys → the 127 field default");
			assertEquals(NO_TANK, tRow.fluidOut());
		}
		// ElectricSifter :1518-1521 — the SHARED RM.Sifting map, the loom masks with energy SBIT_B
		for (int i = 0; i < GTMachines.ELECTRIC_SIFTER_ROWS.size(); i++) {
			GTBasicMachineBlock.MachineRow tRow = GTMachines.ELECTRIC_SIFTER_ROWS.get(i);
			assertEquals("electricsifter" + (i == 0 ? "" : "_t" + (i + 1)), tRow.path());
			assertEquals(20371 + i, tRow.metaId());
			assertSame(GT6RecipeMaps.SIFTING, tRow.recipes().get(), "the SHARED RM.Sifting map — the kinetic sifter rows feed the same instance");
			assertEquals("electricsifter", tRow.texture());
			assertEquals(1, tRow.parallel());
			assertEquals(GTMachines.ELECTRIC_EFFICIENCY, tRow.efficiency().intValue());
			assertEquals((byte)(GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_A), tRow.energySides(), "NBT_ENERGY_ACCEPTED_SIDES SBIT_B (:1518)");
			assertEquals(TOP_IN, tRow.itemIn());
			assertEquals(BOTTOM_OUT, tRow.itemOut());
		}
		// Boxinator :1635-1638 / Unboxinator :1642-1645 — NO efficiency key → the 26-arg overload (null)
		int[] tBoxIds = {20581, 20591};
		for (int f = 0; f < 2; f++) {
			java.util.List<GTBasicMachineBlock.MachineRow> tRows = f == 0 ? GTMachines.BOXINATOR_ROWS : GTMachines.UNBOXINATOR_ROWS;
			String tFamily = f == 0 ? "boxinator" : "unboxinator";
			String tKey = f == 0 ? "gt6.row.boxinator.display" : "gt6.row.unboxinator.display";
			for (int i = 0; i < tRows.size(); i++) {
				GTBasicMachineBlock.MachineRow tRow = tRows.get(i);
				assertEquals(tFamily + (i == 0 ? "" : "_t" + (i + 1)), tRow.path());
				assertEquals(tBoxIds[f] + i, tRow.metaId());
				assertSame(TD.Energy.EU, tRow.energyType());
				assertSame(f == 0 ? GT6RecipeMaps.BOXINATOR : GT6RecipeMaps.UNBOXINATOR, tRow.recipes().get());
				assertEquals(tFamily, tRow.texture());
				assertNull(tRow.efficiency(), "NO NBT_EFFICIENCY key on the :" + (1635 + 7 * f) + " rows — the 10000 identity");
				assertEquals(1, tRow.parallel());
				assertEquals(MIXER_ENERGY, tRow.energySides(), "energy SBIT_D");
				assertEquals(MIXER_IN, tRow.itemIn(), "NBT_INV_SIDE_IN SBIT_L|SBIT_U");
				assertEquals(FERMENTER_ITEM_OUT, tRow.itemOut(), "NBT_INV_SIDE_OUT SBIT_R");
				assertEquals(NO_TANK, tRow.fluidIn(), "no tank keys");
				assertEquals(2, tRow.itemAutoIn(), "SIDE_LEFT");
				assertEquals(4, tRow.itemAutoOut(), "SIDE_RIGHT");
				assertEquals(tKey, tRow.displayKey());
			}
		}
	}

	@Test
	void fermenterRowMatchesTheUpstreamColumns() {
		assertEquals(1, GTMachines.FERMENTER_ROWS.size(), "the SINGLE-variant Fermenter (:1654)");
		GTBasicMachineBlock.MachineRow tRow = GTMachines.FERMENTER_ROWS.get(0);
		assertEquals("fermenter", tRow.path());
		assertEquals(22003, tRow.metaId());
		assertEquals(0, tRow.tier(), "the tier-0 row — the upstream explicit window folds into TIER_INPUTS[0]");
		assertSame(gregapi.data.MT.StainlessSteel, tRow.material().get(), "the StainlessSteel housing (:1654)");
		assertSame(TD.Energy.HU, tRow.energyType(), "NBT_ENERGY_ACCEPTED TD.Energy.HU — the burning box feed");
		assertSame(GT6RecipeMaps.FERMENTER, tRow.recipes().get());
		assertEquals("fermenter", tRow.texture());
		assertEquals(1, tRow.parallel());
		assertNull(tRow.efficiency(), "no NBT_EFFICIENCY key on the :1654 row");
		assertEquals("gt6.row.fermenter.display", tRow.displayKey(), "the atomic single-variant name — NO slot");
		assertEquals(FERMENTER_IN, tRow.itemIn(), "NBT_INV_SIDE_IN SBIT_B|SBIT_L");
		assertEquals(FERMENTER_ITEM_OUT, tRow.itemOut(), "NBT_INV_SIDE_OUT SBIT_R");
		assertEquals(FERMENTER_IN, tRow.fluidIn(), "NBT_TANK_SIDE_IN SBIT_B|SBIT_L");
		assertEquals(FERMENTER_TANK_OUT, tRow.fluidOut(), "NBT_TANK_SIDE_OUT SBIT_U");
		assertEquals(5, tRow.fluidAutoIn(), "NBT_TANK_SIDE_AUTO_IN SIDE_BACK");
		assertEquals(1, tRow.fluidAutoOut(), "NBT_TANK_SIDE_AUTO_OUT SIDE_TOP");
		assertEquals(MIXER_ENERGY, tRow.energySides(), "energy SBIT_D — the burning box emits through its top face");
	}

	/** The T5(EV) rows of the upstream Electric* ladders stay pooled — the Canner T5 ledger, decisions.p29-w1-split-rulings. */
	@Test
	void theT5RowsStayPooled() {
		int[] tPooledT5 = {20355, 20365, 20375, 20585, 20595};
		for (java.util.List<GTBasicMachineBlock.MachineRow> tRows : java.util.List.of(
				GTMachines.ELECTRIC_MIXER_ROWS, GTMachines.ELECTRIC_LOOM_ROWS, GTMachines.ELECTRIC_SIFTER_ROWS,
				GTMachines.BOXINATOR_ROWS, GTMachines.UNBOXINATOR_ROWS)) {
			assertEquals(4, tRows.size(), "four rows — the 4-ladder rule");
			for (GTBasicMachineBlock.MachineRow tRow : tRows) {
				for (int tT5 : tPooledT5) assertTrue(tRow.metaId() != tT5, "the T5 row " + tT5 + " stays pooled");
			}
		}
	}

	// ------------------------------------------------------------------
	// the 共图对拍: same MIXER row, kinetic 1× vs electric 2× REQUIRED progress
	// ------------------------------------------------------------------

	/** The 2-input mixer probe row (eUt 16, duration 16 — the :773 loop inert at the T1 window). */
	private static void pourMixerProbeRow() {
		GT6RecipeMaps.MIXER.addRecipe(new Recipe(true,
				new ItemStack[] {new ItemStack(Items.GRAVEL, 1), new ItemStack(Items.SAND, 1)},
				new ItemStack[] {new ItemStack(Items.GLASS_PANE, 1)},
				null, null, 16, 16, 0));
	}

	/** Builds a row-config fixture machine: the kineticMachine body contract (windows + applyRow) against the offline fixture. */
	private static TileEntityBasicMachine rowMachine(GTBasicMachineBlock.MachineRow aRow) {
		TileEntityBasicMachine tMachine = makeMachine(aRow.recipes().get(), aRow.parallel(), aRow.parallelDuration(), aRow.energyType());
		long[] tInputs = GTMachines.TIER_INPUTS[aRow.tier()]; // the machine() factory assignment (the kineticMachine body)
		tMachine.mInputMin = tInputs[0];
		tMachine.mInput = tInputs[1];
		tMachine.mInputMax = tInputs[2];
		return GTMachines.applyRow(tMachine, aRow);
	}

	private static void feedProbeSet(TileEntityBasicMachine aMachine) {
		// the explicit per-slot form: the handler insertItem(0, ...) scan does not hop
		// across the first occupied slot for a DIFFERENT item, so slot 1 is targeted directly
		aMachine.getInventory().insertItem(0, new ItemStack(Items.GRAVEL, 1), false);
		aMachine.getInventory().insertItem(1, new ItemStack(Items.SAND, 1), false);
	}

	/** Injects {@code aTicks} packets of {@code aSize} through the grid rig (one doInject + one tick per iteration, the /gt6machine inject order). */
	private static void inject(TileEntityBasicMachine aMachine, int aTicks, long aSize) {
		for (int i = 0; i < aTicks; i++) {
			aMachine.doInject(aMachine.mEnergyTypeAccepted, (byte)0, aSize, 1, true);
			aMachine.updateEntity();
		}
	}

	@Test
	void sharedMapPairRunsTheSameMixerRowWithTheTwoToOneProgressRatio() {
		pourMixerProbeRow();
		TileEntityBasicMachine.ENERGY_FAKE_SOURCE = false; // the grid-fed regime (the rig semantics)

		TileEntityBasicMachine tKinetic = rowMachine(GTMachines.MIXER_ROWS.get(0));
		assertEquals(10000, tKinetic.mEfficiency, "the null efficiency column → the :96 10000 identity");
		TileEntityBasicMachine tElectric = rowMachine(GTMachines.ELECTRIC_MIXER_ROWS.get(0));
		assertEquals(5000, tElectric.mEfficiency, "the Electric* NBT_EFFICIENCY 5000 column (:1504)");

		// bind the SAME row on both machines (ONE map, one findRecipe — the 共图 premise)
		feedProbeSet(tKinetic);
		feedProbeSet(tElectric);
		assertEquals(TileEntityBasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tKinetic.checkRecipe(true, true));
		assertEquals(TileEntityBasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tElectric.checkRecipe(true, true));

		// the units() direction: maxProgress ratio EXACTLY 2:1 — 256 vs 512 (the card-A erratum direction:
		// NOT a 2x speed-up, 2x the REQUIRED progress — half speed / 2x the energy-time)
		assertEquals(256L, tKinetic.mMaxProgress, "units(16 × 16 × 1, 10000, 10000, T) = 256 — the kinetic 1x bar");
		assertEquals(512L, tElectric.mMaxProgress, "units(16 × 16 × 1, 5000, 10000, T) = 512 — the electric 2x bar");
		assertEquals(2 * tKinetic.mMaxProgress, tElectric.mMaxProgress, "the maxProgress ratio 2:1");

		// two wall-clock ticks: the kinetic bar sits at half width; the electric consumes the
		// same 64 EU/tick but its bar is only a quarter in (the bar is progress/maxProgress —
		// the maxProgress ratio 2:1 IS the half-speed face)
		for (int i = 0; i < 2; i++) {
			inject(tKinetic, 1, 64);
			inject(tElectric, 1, 64);
		}
		assertEquals(128L, tKinetic.mProgress, "128/256 — the kinetic bar at half width after 2 ticks");
		assertEquals(128L, tElectric.mProgress, "the electric machine consumes the same 64 EU/tick");
		assertEquals(0.5, (double)tKinetic.mProgress / tKinetic.mMaxProgress, "the kinetic wall-clock fraction");

		// the same wall-clock the kinetic process COMPLETES in (4 ticks), the electric sits at
		// EXACTLY half the bar — 同墙钟半程: the electric bar NOW shows the same width the
		// kinetic bar showed at ITS half-time (the bar fraction == the wall-clock fraction)
		for (int i = 0; i < 2; i++) {
			inject(tKinetic, 1, 64);
			inject(tElectric, 1, 64);
		}
		assertEquals(0L, tKinetic.mMaxProgress, "the kinetic process done at 4 ticks (:843 mMaxProgress = 0)");
		assertTrue(tKinetic.getInventory().getStackInSlot(tKinetic.getInputSlotCount()).getCount() >= 1,
				"the kinetic output landed (GLASS_PANE in the first output slot)");
		assertEquals(256L, tElectric.mProgress, "256/512 — the electric bar at half width, the same wall-clock");
		assertEquals(512L, tElectric.mMaxProgress);
		assertEquals(0.5, (double)tElectric.mProgress / tElectric.mMaxProgress,
				"同墙钟进度条同宽: the electric bar at ITS 4-tick half-time == the kinetic bar at ITS 2-tick half-time");

		// and it needs the FULL doubled wall-clock (8 ticks, 2× the energy) to finish
		for (int i = 0; i < 4; i++) inject(tElectric, 1, 64);
		assertEquals(0L, tElectric.mMaxProgress, "the electric process completes only after 8 ticks — 2× the kinetic wall-clock");
		assertTrue(tElectric.getInventory().getStackInSlot(tElectric.getInputSlotCount()).getCount() >= 1,
				"the electric output landed at the doubled wall-clock");
	}

	// ------------------------------------------------------------------
	// the EU window + the 8 EU packet wall (the ULV regression face)
	// ------------------------------------------------------------------

	@Test
	void electricMixerWindowIsTierInputsAndAnEightEuPacketIsDeadBelowIt() {
		pourMixerProbeRow();
		TileEntityBasicMachine.ENERGY_FAKE_SOURCE = false;

		// the LV window = TIER_INPUTS[0] = {16, 32, 64} — an 8 EU packet sits BELOW the wall
		TileEntityBasicMachine tT1 = rowMachine(GTMachines.ELECTRIC_MIXER_ROWS.get(0));
		assertEquals(16, tT1.mInputMin, "TIER_INPUTS[0] min — the LV window (the :126 conversion)");
		assertEquals(32, tT1.mInput);
		assertEquals(64, tT1.mInputMax);
		// the T2 ramp {64, 128, 256} (the :1505 row)
		TileEntityBasicMachine tT2 = rowMachine(GTMachines.ELECTRIC_MIXER_ROWS.get(1));
		assertEquals(64, tT2.mInputMin, "TIER_INPUTS[1]");
		assertEquals(128, tT2.mInput);
		assertEquals(256, tT2.mInputMax);

		// 8 EU packets never cross the wall: each rig tick +8 then the doWork drain resets —
		// mEnergy stays below mInputMin, no recipe ever binds (the p28 ULV-wall semantics)
		feedProbeSet(tT1);
		inject(tT1, 40, 8);
		assertEquals(0, tT1.mProgress, "progress 0 — the 8 EU packet is dead below the LV wall");
		assertEquals(0, tT1.mMaxProgress);

		// the control: an in-window packet fires the same machine
		assertEquals(TileEntityBasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tT1.checkRecipe(true, true));
		inject(tT1, 1, 32);
		assertTrue(tT1.mProgress > 0, "an in-window 32 EU packet advances the process");
	}

	// ------------------------------------------------------------------
	// the electric sifter eats the EXISTING SIFTING row (the shared map, live)
	// ------------------------------------------------------------------

	@Test
	void electricSifterEatsTheExistingSiftingRow() {
		// the :224 grass row0 (the same row the kinetic sifter chain drives live)
		GT6RecipeMaps.SIFTING.addRecipe(new Recipe(true,
				new ItemStack[] {new ItemStack(Items.GRASS_BLOCK, 1)},
				new ItemStack[] {new ItemStack(Items.COARSE_DIRT, 1)},
				null, null, 144, 16, 0));
		TileEntityBasicMachine.ENERGY_FAKE_SOURCE = false;

		TileEntityBasicMachine tMachine = rowMachine(GTMachines.ELECTRIC_SIFTER_ROWS.get(0));
		assertEquals(5000, tMachine.mEfficiency);
		tMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.GRASS_BLOCK, 1), false);
		assertEquals(TileEntityBasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tMachine.checkRecipe(true, true),
				"the electric sifter finds the STATIC row of the shared map — no JSON needed for SIFTING");
		assertEquals(4608L, tMachine.mMaxProgress, "units(16 × 144 × 1, 5000, 10000, T) = 4608 — the efficiency divisor live");
		inject(tMachine, 72, 64);
		assertTrue(tMachine.getInventory().getStackInSlot(tMachine.getInputSlotCount()).getCount() >= 1,
				"the coarse-dirt output landed after the doubled 72-tick wall-clock");
	}

	// ------------------------------------------------------------------
	// the fermenter HU window (the upstream explicit 16/32/64 == TIER_INPUTS[0])
	// ------------------------------------------------------------------

	@Test
	void fermenterWindowRefusesBelowMinAndRunsAtMidWindow() {
		// the smoke-row shape (item + fluid input, eUt 16, duration 128 — the RM.java:688 EUt=16 semantics)
		GT6RecipeMaps.FERMENTER.addRecipe(new Recipe(true,
				new ItemStack[] {new ItemStack(Items.WHEAT, 1)},
				new ItemStack[] {new ItemStack(Items.SUGAR, 1)},
				new FluidStack[] {new FluidStack(Fluids.WATER, 1000)}, null, 128, 16, 0));
		TileEntityBasicMachine.ENERGY_FAKE_SOURCE = false;

		TileEntityBasicMachine tMachine = rowMachine(GTMachines.FERMENTER_ROWS.get(0));
		assertEquals(16, tMachine.mInputMin, "NBT_INPUT_MIN 16 (:1654) == TIER_INPUTS[0][0]");
		assertEquals(32, tMachine.mInput, "NBT_INPUT 32 (:1654) == TIER_INPUTS[0][1]");
		assertEquals(64, tMachine.mInputMax, "NBT_INPUT_MAX 64 (:1654) == TIER_INPUTS[0][2]");
		assertSame(TD.Energy.HU, tMachine.mEnergyTypeAccepted);

		// stock the inputs
		tMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.WHEAT, 1), false);
		tMachine.mTanksInput[0].fill(new FluidStack(Fluids.WATER, 1000), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);

		// BELOW the window: 8 HU/tick never crosses 16 — no progress (the <16 拒 arm)
		inject(tMachine, 40, 8);
		assertEquals(0, tMachine.mProgress, "progress 0 below the 16 HU window floor");

		// MID-window: 32 HU/tick runs (the 32 跑通 arm); budget = units(16×128×1, 10000, 10000, T) = 2048
		assertEquals(TileEntityBasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tMachine.checkRecipe(true, true));
		assertEquals(2048L, tMachine.mMaxProgress);
		inject(tMachine, 4, 32);
		assertTrue(tMachine.mProgress > 0, "the mid-window HU train advances the fermenter");

		// ABOVE the window: the :493 gate routes to overcharge (the >64 拒 arm — the RCON face
		// drives the live explosion; offline pins the gate constant)
		assertEquals(64, tMachine.getEnergySizeInputMax(TD.Energy.HU, (byte)0), "the :493 packet ceiling — an 80 HU packet is REFUSED into overcharge");
	}

	// ------------------------------------------------------------------
	// the pooled Unboxinator loot arm (the base-RecipeMap deviation)
	// ------------------------------------------------------------------

	@Test
	void unboxinatorLootArmIsPooledAndRefusesUnknownInputs() {
		// the upstream RecipeMapUnboxinator.java:43-89 loot runtime-synthesis arm is POOLED:
		// the port map is the BASE RecipeMap — no findRecipe override exists to synthesize
		// Crate_Loot/Drop_Loot/Scrapbox/lootbag rows at lookup time (the SHREDDER/CHISEL
		// subclass judgement, the GT6RecipeMaps.UNBOXINATOR field doc)
		assertSame(gregtech6.recipes.RecipeMap.class, GT6RecipeMaps.UNBOXINATOR.getClass(),
				"the port UNBOXINATOR is the base RecipeMap — the loot arm does not exist");

		// a loot-bag-ish input with NO poured row is REFUSED (the loot arm 不存在即拒)
		TileEntityBasicMachine.ENERGY_FAKE_SOURCE = false;
		TileEntityBasicMachine tMachine = rowMachine(GTMachines.UNBOXINATOR_ROWS.get(0));
		tMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.BUNDLE, 1), false);
		assertTrue(tMachine.checkRecipe(true, true) != TileEntityBasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE,
				"no loot synthesis — the unknown input refuses");

		// the static smoke row itself runs (the boxinator/unboxinator smoke 行进出 arm)
		GT6RecipeMaps.UNBOXINATOR.addRecipe(new Recipe(true,
				new ItemStack[] {new ItemStack(Items.HEART_OF_THE_SEA, 1)},
				new ItemStack[] {new ItemStack(Items.PAPER, 8), new ItemStack(Items.COMPASS, 1)},
				null, null, 16, 16, 0));
		tMachine.getInventory().extractItem(TileEntityBasicMachine.SLOT_INPUT, 1, false); // clear the refused bundle (max-stack-1 → slot 0)
		tMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.HEART_OF_THE_SEA, 1), false);
		assertEquals(TileEntityBasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tMachine.checkRecipe(true, true));
		inject(tMachine, 4, 64);
		assertTrue(tMachine.getInventory().getStackInSlot(tMachine.getInputSlotCount()).getCount() >= 8,
				"8x paper in the first output slot");
		assertTrue(tMachine.getInventory().getStackInSlot(tMachine.getInputSlotCount() + 1).getCount() >= 1,
				"1x compass in the second output slot");
	}

	// ------------------------------------------------------------------
	// the boxinator smoke row runs with the NO-efficiency identity (offline)
	// ------------------------------------------------------------------

	@Test
	void boxinatorRunsAtTheIdentityEfficiency() {
		GT6RecipeMaps.BOXINATOR.addRecipe(new Recipe(true,
				new ItemStack[] {new ItemStack(Items.PAPER, 8), new ItemStack(Items.COMPASS, 1)},
				new ItemStack[] {new ItemStack(Items.MAP, 1)},
				null, null, 16, 16, 0));
		TileEntityBasicMachine.ENERGY_FAKE_SOURCE = false;
		TileEntityBasicMachine tMachine = rowMachine(GTMachines.BOXINATOR_ROWS.get(0));
		assertEquals(10000, tMachine.mEfficiency, "NO efficiency key → the :96 default — the zero-drift face");
		tMachine.getInventory().insertItem(0, new ItemStack(Items.PAPER, 8), false);
		tMachine.getInventory().insertItem(1, new ItemStack(Items.COMPASS, 1), false);
		assertEquals(TileEntityBasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tMachine.checkRecipe(true, true));
		assertEquals(256L, tMachine.mMaxProgress, "units(16 × 16 × 1, 10000, 10000, T) = 256 — the identity bar");
		inject(tMachine, 4, 64);
		assertTrue(tMachine.getInventory().getStackInSlot(tMachine.getInputSlotCount()).getCount() >= 1,
				"the map landed after 4 ticks (256 @ 64/tick)");
	}
}
