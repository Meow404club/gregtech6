package gregtech6.tileentity.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import gregapi.data.TD;
import gregtech6.block.GTBasicMachineBlock;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.Recipe;
import gregtech6.registry.GTMachines;

/**
 * The P29 W1 process-family row acceptance (task p29-w1-kinetic-process-ladder, the
 * OFFLINE half — the {@link GT6KineticTrioRowTest} shape): the twenty-four row records
 * pinned to the upstream columns (Buzzsaw :1318-1321 ids 20061-20064 / Squeezer
 * :1324-1327 ids 20071-20074 / Centrifuge :1330-1333 ids 20081-20084 / Sluice :1464-1467
 * ids 20291-20294 / Sanding Machine :1589-1592 ids 20511-20514 / Pressure Washer
 * :1615-1618 ids 20551-20554 — the Kinetic_T tier-material name column / NBT_HARDNESS
 * 7.0/6.0/9.0/12.5 / NBT_INPUT through the TIER_INPUTS carrier / NBT_ENERGY_ACCEPTED
 * RU·KU·RU·RU·RU·RU / the card-A recipe maps (CUTTER·SQUEEZER·CENTRIFUGE·SLUICE·
 * SHARPENING·PRESSURE_WASHER) / NBT_TEXTURE per family, "debarker" on the washer
 * verbatim / the parallel columns: PARALLEL_4_32 on the squeezer, the NON-standard
 * CENTRIFUGE_PARALLEL {1,2,4,8} on the centrifuge, no keys → 1 on the other four / the
 * connectivity mask columns / menu = null on every row — the zero-new-MenuType clause).
 *
 * <p>The behavioral half drives the fixture machines through the fake-source regime
 * (the base-suite premise): the buzzsaw coolant gate (no water = no recipe, the
 * MIN-FLUID 1 gate at checkRecipe :709), the squeezer parallel multiplier (PARALLEL_4_32
 * + duration T → outputs ×N), the centrifuge NON-standard parallel differential
 * (T1 = 1 arm vs the T4 = 8 arms, with the Crusher 4/8/16/32 shape as the regression
 * contrast), the sluice dual-minimum-input row (RM.SLUICE MIN 2 = item + fluid legs both),
 * the pressure-washer water consumption (the tank drains one recipe-worth per process)
 * and the :501-type KU/RU mutual reject (the energy-type gate — RU machines refuse KU
 * packets and vice versa).
 */
public class GT6ProcessFamilyRowTest extends TileEntityBasicMachineOfflineTestBase {

	/** The :151 read form + the ORed SBIT_A, per family (the upstream NBT_ENERGY_ACCEPTED_SIDES column). */
	static final byte ENERGY_B = (byte)(GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_A);
	static final byte ENERGY_U = (byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A);
	static final byte ENERGY_D = (byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A);
	/** SBIT_L|SBIT_R faces + the ORed SBIT_A — the left-in/right-out item spine. */
	static final byte ITEM_IN_L = (byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A);
	static final byte ITEM_OUT_R = (byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A);
	/** The no-NBT-key tank face: the upstream field default 127, all relative sides. */
	static final byte TANK_DEFAULT = 127;
	/** SIDE_UNDEFINED — no NBT_*_SIDE_AUTO_* key on those rows. */
	static final byte AUTO_UNDEFINED = (byte)-1;

	@Test
	void processRowsMatchTheUpstreamColumns() {
		String[][] tFamilies = {
				{"buzzsaw", "gt6.row.machine.buzzsaw", "RU", "buzzsaw", "CUTTER"},
				{"squeezer", "gt6.row.machine.squeezer", "KU", "squeezer", "SQUEEZER"},
				{"centrifuge", "gt6.row.machine.centrifuge", "RU", "centrifuge", "CENTRIFUGE"},
				{"sluice", "gt6.row.machine.sluice", "RU", "sluice", "SLUICE"},
				{"sanding_machine", "gt6.row.machine.sanding_machine", "RU", "sander", "SHARPENING"},
				{"pressure_washer", "gt6.row.machine.pressure_washer", "RU", "debarker", "PRESSURE_WASHER"}};
		int[][] tMetaIds = {{20061, 20062, 20063, 20064}, {20071, 20072, 20073, 20074},
				{20081, 20082, 20083, 20084}, {20291, 20292, 20293, 20294},
				{20511, 20512, 20513, 20514}, {20551, 20552, 20553, 20554}};
		String[] tMatSlugs = {"bronze", "steel", "titanium", "tungstensteel"};
		String[] tMatWords = {"Bronze", "Steel", "Titanium", "Tungstensteel"};
		float[] tHardness = {7.0F, 6.0F, 9.0F, 12.5F};
		for (int f = 0; f < 6; f++) {
			java.util.List<GTBasicMachineBlock.MachineRow> tRows = switch (f) {
				case 0 -> GTMachines.BUZZSAW_ROWS;
				case 1 -> GTMachines.SQUEEZER_ROWS;
				case 2 -> GTMachines.CENTRIFUGE_ROWS;
				case 3 -> GTMachines.SLUICE_ROWS;
				case 4 -> GTMachines.SANDING_ROWS;
				default -> GTMachines.PRESSURE_WASHER_ROWS; };
			assertEquals(4, tRows.size(), "the four " + tFamilies[f][0] + " rows");
			for (int i = 0; i < 4; i++) {
				GTBasicMachineBlock.MachineRow tRow = tRows.get(i);
				int tTier = i + 1;
				String tPath = tFamilies[f][0] + (i == 0 ? "" : "_t" + tTier);
				assertEquals(tPath, tRow.path(), "the registry path ladder");
				assertEquals(tMetaIds[f][i], tRow.metaId(), "the MultiTile id column of " + tPath);
				assertEquals(tTier, tRow.tier() + 1, "the tier index");
				assertEquals(tHardness[i], tRow.hardness(), "NBT_HARDNESS of " + tPath);
				assertSame("KU".equals(tFamilies[f][2]) ? TD.Energy.KU : TD.Energy.RU, tRow.energyType(),
						"NBT_ENERGY_ACCEPTED of " + tPath);
				assertSame(switch (f) {
					case 0 -> GT6RecipeMaps.CUTTER;
					case 1 -> GT6RecipeMaps.SQUEEZER;
					case 2 -> GT6RecipeMaps.CENTRIFUGE;
					case 3 -> GT6RecipeMaps.SLUICE;
					case 4 -> GT6RecipeMaps.SHARPENING;
					default -> GT6RecipeMaps.PRESSURE_WASHER; }, tRow.recipes().get(),
						"NBT_RECIPEMAP through the supplier (" + tPath + ")");
				assertEquals(tFamilies[f][3], tRow.texture(), "NBT_TEXTURE (" + tPath + ") — the debarker fidelity on the washer");
				assertEquals(tFamilies[f][1], tRow.displayKey(), "the family template key (the unit-key carrier)");
				assertEquals(tMatWords[i], tRow.matDisplay(), "the Kinetic_T material word of tier " + tTier);
				assertEquals(tMatSlugs[i], tRow.matSlug(), "the Kinetic_T slug (the gt6.row.mat key tail)");
				assertTrue(tRow.cheapOverclocking(), "the port :773 loop runs unconditionally");
				assertTrue(tRow.efficiency() == null, "no NBT_EFFICIENCY key on " + tPath + " — the 26-arg overload (efficiency = null, the :96 default 10000 face)");
				assertNullMenu(tRow, tPath);
				// the parallel columns
				if (f == 1) {
					assertEquals(GTMachines.PARALLEL_4_32[i], tRow.parallel(), "NBT_PARALLEL of " + tPath + " (:1324-1327 — the {4,8,16,32} table)");
					assertTrue(tRow.parallelDuration(), "NBT_PARALLEL_DURATION T on " + tPath);
				} else if (f == 2) {
					assertEquals(GTMachines.CENTRIFUGE_PARALLEL[i], tRow.parallel(), "NBT_PARALLEL of " + tPath + " (:1330-1333 — the NON-standard {1,2,4,8} table)");
					assertTrue(tRow.parallelDuration(), "NBT_PARALLEL_DURATION T on " + tPath);
				} else {
					assertEquals(1, tRow.parallel(), "NO NBT_PARALLEL on the " + tPath + " rows → 1");
					assertFalse(tRow.parallelDuration(), "NO NBT_PARALLEL_DURATION on the " + tPath + " rows → F");
				}
				// the item spine: left-in/right-out on four families; the centrifuge takes the
				// top (the :1330 SBIT_U row + the SIDE_TOP auto), the sluice the two-sided
				// left|top in / right|bottom out (the :1464 row)
				if (f == 2) {
					assertEquals((byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A), tRow.itemIn(), "NBT_INV_SIDE_IN SBIT_U (" + tPath + ")");
					assertEquals(1, tRow.itemAutoIn(), "NBT_INV_SIDE_AUTO_IN SIDE_TOP (" + tPath + ")");
				} else if (f == 3) {
					assertEquals((byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A), tRow.itemIn(), "NBT_INV_SIDE_IN SBIT_L|SBIT_U (" + tPath + ")");
					assertEquals((byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A), tRow.itemOut(), "NBT_INV_SIDE_OUT SBIT_R|SBIT_D (" + tPath + ")");
					assertEquals(2, tRow.itemAutoIn(), "NBT_INV_SIDE_AUTO_IN SIDE_LEFT (" + tPath + ")");
				} else {
					assertEquals(ITEM_IN_L, tRow.itemIn(), "NBT_INV_SIDE_IN SBIT_L (" + tPath + ")");
					assertEquals(2, tRow.itemAutoIn(), "NBT_INV_SIDE_AUTO_IN SIDE_LEFT (" + tPath + ")");
					assertEquals(ITEM_OUT_R, tRow.itemOut(), "NBT_INV_SIDE_OUT SBIT_R (" + tPath + ")");
				}
				if (f != 3) assertEquals(4, tRow.itemAutoOut(), "NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT (" + tPath + ")");
				// the energy faces: SBIT_B on buzzsaw/sluice/pressure_washer, SBIT_U on
				// squeezer/sanding, SBIT_D on centrifuge (the :151 rows verbatim)
				byte tEnergy = f == 1 || f == 4 ? ENERGY_U : f == 2 ? ENERGY_D : ENERGY_B;
				assertEquals(tEnergy, tRow.energySides(), "NBT_ENERGY_ACCEPTED_SIDES (" + tPath + ")");
			}
		}
	}

	/** The menu clause split out (assertNull keeps the message shape of the trio test). */
	private static void assertNullMenu(GTBasicMachineBlock.MachineRow aRow, String aPath) {
		org.junit.jupiter.api.Assertions.assertNull(aRow.menu(), "menu = null on " + aPath + " — ZERO new gt6:* MenuType (the card GUI clause)");
	}

	/** The tank mask columns, per family — the buzzsaw/washer coolant tops, the squeezer juice bottom, the centrifuge in-top/out-left, the sluice four-key form, the sander none. */
	@Test
	void tankMaskColumnsMatchTheUpstreamRows() {
		// buzzsaw :1318 — tank IN SBIT_U|SBIT_D auto BOTTOM, no OUT key
		for (GTBasicMachineBlock.MachineRow tRow : GTMachines.BUZZSAW_ROWS) {
			assertEquals((byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A), tRow.fluidIn(), "NBT_TANK_SIDE_IN of " + tRow.path());
			assertEquals(TANK_DEFAULT, tRow.fluidOut(), "no NBT_TANK_SIDE_OUT key on " + tRow.path());
			assertEquals(0, tRow.fluidAutoIn(), "NBT_TANK_SIDE_AUTO_IN SIDE_BOTTOM on " + tRow.path());
			assertEquals(AUTO_UNDEFINED, tRow.fluidAutoOut(), "no NBT_TANK_SIDE_AUTO_OUT key on " + tRow.path());
		}
		// squeezer :1324 — tank OUT SBIT_D auto BOTTOM, no IN key
		for (GTBasicMachineBlock.MachineRow tRow : GTMachines.SQUEEZER_ROWS) {
			assertEquals(TANK_DEFAULT, tRow.fluidIn(), "no NBT_TANK_SIDE_IN key on " + tRow.path());
			assertEquals((byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A), tRow.fluidOut(), "NBT_TANK_SIDE_OUT of " + tRow.path());
			assertEquals(AUTO_UNDEFINED, tRow.fluidAutoIn(), "no NBT_TANK_SIDE_AUTO_IN key on " + tRow.path());
			assertEquals(0, tRow.fluidAutoOut(), "NBT_TANK_SIDE_AUTO_OUT SIDE_BOTTOM on " + tRow.path());
		}
		// centrifuge :1330 — tank IN SBIT_U auto TOP, OUT SBIT_L auto LEFT
		for (GTBasicMachineBlock.MachineRow tRow : GTMachines.CENTRIFUGE_ROWS) {
			assertEquals((byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A), tRow.fluidIn(), "NBT_TANK_SIDE_IN of " + tRow.path());
			assertEquals((byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A), tRow.fluidOut(), "NBT_TANK_SIDE_OUT of " + tRow.path());
			assertEquals(1, tRow.fluidAutoIn(), "NBT_TANK_SIDE_AUTO_IN SIDE_TOP on " + tRow.path());
			assertEquals(2, tRow.fluidAutoOut(), "NBT_TANK_SIDE_AUTO_OUT SIDE_LEFT on " + tRow.path());
		}
		// sluice :1464 — the four-key form: IN SBIT_L|SBIT_U auto TOP, OUT SBIT_R|SBIT_D auto BOTTOM
		for (GTBasicMachineBlock.MachineRow tRow : GTMachines.SLUICE_ROWS) {
			assertEquals((byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A), tRow.fluidIn(), "NBT_TANK_SIDE_IN of " + tRow.path());
			assertEquals((byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A), tRow.fluidOut(), "NBT_TANK_SIDE_OUT of " + tRow.path());
			assertEquals(1, tRow.fluidAutoIn(), "NBT_TANK_SIDE_AUTO_IN SIDE_TOP on " + tRow.path());
			assertEquals(0, tRow.fluidAutoOut(), "NBT_TANK_SIDE_AUTO_OUT SIDE_BOTTOM on " + tRow.path());
			assertEquals((byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A), tRow.itemIn(), "NBT_INV_SIDE_IN SBIT_L|SBIT_U of " + tRow.path());
			assertEquals((byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A), tRow.itemOut(), "NBT_INV_SIDE_OUT SBIT_R|SBIT_D of " + tRow.path());
		}
		// sander :1589 — NO tank keys at all
		for (GTBasicMachineBlock.MachineRow tRow : GTMachines.SANDING_ROWS) {
			assertEquals(TANK_DEFAULT, tRow.fluidIn(), "no NBT_TANK_SIDE_IN key on " + tRow.path());
			assertEquals(TANK_DEFAULT, tRow.fluidOut(), "no NBT_TANK_SIDE_OUT key on " + tRow.path());
			assertEquals(AUTO_UNDEFINED, tRow.fluidAutoIn(), "SIDE_UNDEFINED on " + tRow.path());
			assertEquals(AUTO_UNDEFINED, tRow.fluidAutoOut(), "SIDE_UNDEFINED on " + tRow.path());
		}
		// pressure washer :1615 — tank IN SBIT_U|SBIT_D auto TOP, no OUT key (the water-consumption face)
		for (GTBasicMachineBlock.MachineRow tRow : GTMachines.PRESSURE_WASHER_ROWS) {
			assertEquals((byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A), tRow.fluidIn(), "NBT_TANK_SIDE_IN of " + tRow.path());
			assertEquals(TANK_DEFAULT, tRow.fluidOut(), "no NBT_TANK_SIDE_OUT key on " + tRow.path());
			assertEquals(1, tRow.fluidAutoIn(), "NBT_TANK_SIDE_AUTO_IN SIDE_TOP on " + tRow.path());
			assertEquals(AUTO_UNDEFINED, tRow.fluidAutoOut(), "no NBT_TANK_SIDE_AUTO_OUT key on " + tRow.path());
		}
	}

	/**
	 * The centrifuge NON-standard parallel ruling: the card-A constant is the ONLY table
	 * the rows consume, the T1 = 1 arm differs from every 4/8/16/32 shape, and the
	 * Crusher/sifter/compressor contrast stays on PARALLEL_4_32 (the regression face).
	 */
	@Test
	void centrifugeParallelIsTheNonStandardLadder() {
		int[] tUpstream = {1, 2, 4, 8}; // :1330-1333
		assertEquals(4, GTMachines.CENTRIFUGE_PARALLEL.length);
		for (int i = 0; i < 4; i++) {
			assertEquals(tUpstream[i], GTMachines.CENTRIFUGE_PARALLEL[i], "the NBT_PARALLEL column of the centrifuge tier " + (i + 1));
			assertEquals(GTMachines.CENTRIFUGE_PARALLEL[i], GTMachines.CENTRIFUGE_ROWS.get(i).parallel(), "the centrifuge rows consume the card-A constant");
			assertTrue(GTMachines.CENTRIFUGE_PARALLEL[i] != GTMachines.PARALLEL_4_32[i],
					"the T" + (i + 1) + " arm differs from the 4/8/16/32 shape — the NON-standard ladder");
		}
		// the crusher contrast rows stay on the shared table (the regression control)
		assertSame(GTMachines.PARALLEL_4_32, GTMachines.CRUSHER_PARALLEL, "the W1 merge ruling untouched");
		assertEquals(4, GTMachines.CRUSHER_PARALLEL[0], "the crusher T1 arm is 4, NOT 1");
	}

	// ------------------------------------------------------------------
	// the behavioral half — the fake-source drives (the fluid-round-trip drive form)
	// ------------------------------------------------------------------

	private static void pourCutterRow() {
		GT6RecipeMaps.CUTTER.addRecipe(new Recipe(true, new ItemStack[] {new ItemStack(Items.OAK_PLANKS, 1)},
				new ItemStack[] {new ItemStack(Items.STICK, 4)},
				new FluidStack[] {new FluidStack(Fluids.WATER, 10)}, null, 16, 16, 0));
	}

	/** The buzzsaw coolant gate (acceptance ①): no water = no recipe and zero consume; with water the row completes 4x stick. */
	@Test
	void buzzsawRefusesWithoutCoolantAndRunsWithIt() {
		pourCutterRow();
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.CUTTER, 1, false, TD.Energy.RU);
		GTMachines.applyRow(tMachine, GTMachines.BUZZSAW_ROWS.get(0));
		assertEquals(1, tMachine.mTanksInput.length, "the CUTTER map carries exactly one input-tank slot (the coolant leg)");
		assertEquals(0, tMachine.mTanksOutput.length, "the CUTTER map is OUT-FLUID 0 — no output tank");
		tMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.OAK_PLANKS, 1), false);

		// the negative: item present, coolant absent — the :709 MIN-FLUID gate refuses before any consume
		drive(tMachine, 40);
		assertEquals(0, tMachine.mProgress, "no progress without the coolant");
		assertEquals(1, countInput(tMachine), "the plank was NOT consumed");
		assertEquals(0, tMachine.mOutputItems.length, "no pending outputs without the coolant");

		// the control: coolant in through the real capability face (the wrapper's onFluidIO
		// re-opens the :476 checkRecipe arm a closed mRunning latch shut — the direct tank
		// fill would sit unnoticed) → the row completes (4x stick, the durable verdict —
		// mSuccessful is a single-tick face the idle ticks reset)
		assertEquals(1000, fluidFill(tMachine, new FluidStack(Fluids.WATER, 1000)));
		drive(tMachine, 40);
		assertEquals(4, countOutput(tMachine, 0), "the 4x stick row landed in the first output slot");
		assertEquals(0, countInput(tMachine), "the plank was consumed");
		assertEquals(990, tMachine.mTanksInput[0].amount(), "the coolant drained one recipe-worth");
	}

	private static int countInput(TileEntityBasicMachine aMachine) {
		ItemStack tStack = aMachine.getInventory().getStackInSlot(TileEntityBasicMachine.SLOT_INPUT);
		return tStack.isEmpty() ? 0 : tStack.getCount();
	}

	/** The capability-face fill (the TileEntityBasicMachineFluidFaceTest wrapper form — the onFluidIO beat books the inventory change). */
	private static int fluidFill(TileEntityBasicMachine aMachine, FluidStack aStack) {
		return aMachine.newFluidHandler(net.minecraft.core.Direction.DOWN).fill(aStack, FluidAction.EXECUTE);
	}

	/** The output-slot read (the :816 placement — slot = mInputItemsCount + i). */
	private static int countOutput(TileEntityBasicMachine aMachine, int aOutputIndex) {
		ItemStack tStack = aMachine.getInventory().getStackInSlot(aMachine.getInputSlotCount() + aOutputIndex);
		return tStack.isEmpty() ? 0 : tStack.getCount();
	}

	private static void pourSqueezerRow() {
		GT6RecipeMaps.SQUEEZER.addRecipe(new Recipe(true, new ItemStack[] {new ItemStack(Items.APPLE, 1)},
				new ItemStack[] {new ItemStack(Items.STICK, 1)}, null,
				new FluidStack[] {new FluidStack(Fluids.WATER, 50)}, 16, 16, 0));
	}

	/** The squeezer parallel multiplier (acceptance ②): T1 parallel 4 + duration T consumes 4 apples in ONE completion → 4x stick + 200 L. */
	@Test
	void squeezerParallelFourMultipliesTheOutputs() {
		pourSqueezerRow();
		// KU is an ALL_ALTERNATING carrier — the completion verdict rides the net regime
		// and the positive→non-positive transition (the kuCrusher pulse-cycle form)
		TileEntityBasicMachine.ENERGY_FAKE_SOURCE = false;
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.SQUEEZER, GTMachines.PARALLEL_4_32[0], true, TD.Energy.KU);
		GTMachines.applyRow(tMachine, GTMachines.SQUEEZER_ROWS.get(0));
		assertEquals(0, tMachine.mTanksInput.length, "the SQUEEZER map is IN-FLUID 0");
		assertEquals(1, tMachine.mTanksOutput.length, "the SQUEEZER map carries the one juice output tank");
		for (int i = 0; i < 4; i++) tMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.APPLE, 1), false);

		// progress 16x16x4 = 1024 at 64/injected tick = 16 pairs; the margin pairs overshoot harmlessly
		for (int i = 0; i < 20; i++) {
			assertEquals(1, tMachine.doInject(TD.Energy.KU, (byte)2, 64, 1, true), "the positive half-cycle packet");
			drive(tMachine, 1);
		}
		assertTrue(tMachine.mProgress >= tMachine.mMaxProgress, "the positive train ran the process past completion");
		// the negative half-cycle opens the :815 transition arm → ALL FOUR apples placed by the ONE completion
		assertEquals(1, tMachine.doInject(TD.Energy.KU, (byte)2, -64, 1, true), "the negative packet is accepted with |size|");
		drive(tMachine, 1);
		assertEquals(0, countInput(tMachine), "ALL FOUR apples consumed by the ONE completion (parallel 4 + duration T)");
		assertEquals(4, countOutput(tMachine, 0), "outputs xN — the parallel multiplier on the item leg");
		assertEquals(200, tMachine.mTanksOutput[0].amount(), "outputs xN — the parallel multiplier on the juice leg");
	}

	private static void pourCentrifugeRow() {
		GT6RecipeMaps.CENTRIFUGE.addRecipe(new Recipe(true, new ItemStack[] {new ItemStack(Items.SNOWBALL, 2)},
				new ItemStack[] {new ItemStack(Items.CLAY_BALL, 1), new ItemStack(Items.FLINT, 1)}, null, null, 16, 16, 0));
	}

	/** The centrifuge NON-standard differential (acceptance ③): the T1 = 1 arm processes ONE row per completion (2 snowballs), the T4 = 8 arm processes EIGHT (16 snowballs) — the crusher 4/8/16/32 shape as the contrast. */
	@Test
	void centrifugeNonStandardParallelDifferential() {
		pourCentrifugeRow();

		// the T1 arm: parallel 1 — exactly one process per completion
		TileEntityBasicMachine tT1 = makeMachine(GT6RecipeMaps.CENTRIFUGE, GTMachines.CENTRIFUGE_PARALLEL[0], true, TD.Energy.RU);
		GTMachines.applyRow(tT1, GTMachines.CENTRIFUGE_ROWS.get(0));
		tT1.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.SNOWBALL, 2), false);
		drive(tT1, 40);
		assertEquals(0, countInput(tT1), "the T1 arm consumed exactly ONE row (2 snowballs) — parallel 1");
		assertEquals(1, countOutput(tT1, 0), "one row's worth of clay");
		assertEquals(1, countOutput(tT1, 1), "one row's worth of flint");

		// the T4 arm: parallel 8 — ONE completion consumes eight rows
		TileEntityBasicMachine tT4 = makeMachine(GT6RecipeMaps.CENTRIFUGE, GTMachines.CENTRIFUGE_PARALLEL[3], true, TD.Energy.RU);
		GTMachines.applyRow(tT4, GTMachines.CENTRIFUGE_ROWS.get(3));
		tT4.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.SNOWBALL, 16), false);
		drive(tT4, 40); // progress 16x16x8 = 2048 → 32 ticks at the 64/t fake drain
		assertEquals(0, countInput(tT4), "the T4 arm consumed EIGHT rows (16 snowballs) in ONE completion — parallel 8");
		assertEquals(8, countOutput(tT4, 0), "eight rows' worth of clay — the x8 multiplier");
		assertEquals(8, countOutput(tT4, 1), "eight rows' worth of flint");
	}

	private static void pourSluiceRow() {
		GT6RecipeMaps.SLUICE.addRecipe(new Recipe(true, new ItemStack[] {new ItemStack(Items.SAND, 1)},
				new ItemStack[] {new ItemStack(Items.CLAY_BALL, 1), new ItemStack(Items.GOLD_NUGGET, 1)},
				new FluidStack[] {new FluidStack(Fluids.WATER, 100)}, null, 16, 16, 0));
	}

	/** The sluice dual-minimum-input row (acceptance ④): RM.SLUICE MIN 2 — the fluid alone does nothing, the item+fluid pair completes (the 流水 lives in the recipe domain). */
	@Test
	void sluiceDualMinimumInputsRun() {
		pourSluiceRow();
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.SLUICE, 1, false, TD.Energy.RU);
		GTMachines.applyRow(tMachine, GTMachines.SLUICE_ROWS.get(0));
		assertEquals(1, tMachine.mTanksInput.length, "the SLUICE map carries the one input-tank slot (the flowing-water leg)");
		// the negative: water only — the :708 MIN-ITEM gate refuses
		assertEquals(1000, tMachine.mTanksInput[0].fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE));
		drive(tMachine, 40);
		assertEquals(0, tMachine.mProgress, "no progress on the fluid leg alone");
		assertEquals(1000, tMachine.mTanksInput[0].amount(), "nothing consumed on the fluid leg alone");
		// the control: both minimum legs → the row completes
		tMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.SAND, 1), false);
		drive(tMachine, 40);
		assertEquals(1, countOutput(tMachine, 0), "the clay output landed — the dual-leg row completed");
		assertEquals(1, countOutput(tMachine, 1), "the gold-nugget output landed");
		assertEquals(900, tMachine.mTanksInput[0].amount(), "the water leg drained one recipe-worth");
	}

	private static void pourSharpeningRow() {
		GT6RecipeMaps.SHARPENING.addRecipe(new Recipe(true, new ItemStack[] {new ItemStack(Items.STONE_BRICKS, 1)},
				new ItemStack[] {new ItemStack(Items.STONE, 2)}, null, null, 16, 16, 0));
	}

	/** The sander smoke face: left-in/right-out, no tanks, one row one process. */
	@Test
	void sanderRunsTheSharpeningRow() {
		pourSharpeningRow();
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.SHARPENING, 1, false, TD.Energy.RU);
		GTMachines.applyRow(tMachine, GTMachines.SANDING_ROWS.get(0));
		assertEquals(0, tMachine.mTanksInput.length + tMachine.mTanksOutput.length, "the SHARPENING map is 0/0 fluids — no tanks at all");
		tMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.STONE_BRICKS, 1), false);
		drive(tMachine, 40);
		assertEquals(2, countOutput(tMachine, 0), "the 2x stone row landed — the sharpening row completed");
	}

	private static void pourWasherRow() {
		GT6RecipeMaps.PRESSURE_WASHER.addRecipe(new Recipe(true, new ItemStack[] {new ItemStack(Items.COARSE_DIRT, 1)},
				new ItemStack[] {new ItemStack(Items.DIRT, 1)},
				new FluidStack[] {new FluidStack(Fluids.WATER, 100)}, null, 16, 16, 0));
	}

	/** The pressure-washer water consumption (acceptance ⑤): the tank drains exactly one recipe-worth per process, no output tank. */
	@Test
	void pressureWasherConsumesItsWater() {
		pourWasherRow();
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.PRESSURE_WASHER, 1, false, TD.Energy.RU);
		GTMachines.applyRow(tMachine, GTMachines.PRESSURE_WASHER_ROWS.get(0));
		assertEquals(1, tMachine.mTanksInput.length, "the washer input tank (the water-consumption leg)");
		assertEquals(0, tMachine.mTanksOutput.length, "the washer map is OUT-FLUID 0 — no output tank");
		assertEquals(1000, tMachine.mTanksInput[0].fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE));
		tMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.COARSE_DIRT, 2), false);


		// two sequential cycles (parallel 1) inside one drive: each drains 100 L
		drive(tMachine, 40);
		assertEquals(0, countInput(tMachine), "both items washed");
		assertEquals(2, countOutput(tMachine, 0), "two washed dirt");
		assertEquals(800, tMachine.mTanksInput[0].amount(), "1000 - 2x100: the water consumption is booked per process");
	}

	/** The :501-type gate (acceptance ⑥): the KU machine and the RU machine REJECT each other's carrier (reference equality, the shared TD.Energy constants). */
	@Test
	void energyTypeGateRejectsTheForeignCarrier() {
		// the buzzsaw fixture is the RU carrier
		TileEntityBasicMachine tRu = makeMachine(GT6RecipeMaps.CUTTER, 1, false, TD.Energy.RU);
		assertTrue(tRu.isEnergyAcceptingFrom(TD.Energy.RU, (byte)0, true), "the RU machine takes the RU carrier");
		assertFalse(tRu.isEnergyAcceptingFrom(TD.Energy.KU, (byte)0, true), "the RU machine REFUSES the KU carrier — the :501 reference-equality gate");
		assertFalse(tRu.isEnergyAcceptingFrom(TD.Energy.EU, (byte)0, true), "the RU machine REFUSES EU");
		assertFalse(tRu.isEnergyAcceptingFrom(TD.Energy.HU, (byte)0, true), "the RU machine REFUSES HU");
		// the squeezer fixture is the KU carrier (the first KU consumer family)
		TileEntityBasicMachine tKu = makeMachine(GT6RecipeMaps.SQUEEZER, 1, false, TD.Energy.KU);
		assertTrue(tKu.isEnergyAcceptingFrom(TD.Energy.KU, (byte)0, true), "the KU machine takes the KU carrier");
		assertFalse(tKu.isEnergyAcceptingFrom(TD.Energy.RU, (byte)0, true), "the KU machine REFUSES the RU carrier — the :501 reference-equality gate");
		assertFalse(tKu.isEnergyAcceptingFrom(TD.Energy.EU, (byte)0, true), "the KU machine REFUSES EU");
	}
}
