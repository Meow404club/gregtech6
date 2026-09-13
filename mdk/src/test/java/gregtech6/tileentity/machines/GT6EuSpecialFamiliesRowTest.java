package gregtech6.tileentity.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


import org.junit.jupiter.api.Test;


import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.ItemStack;

import gregapi.data.TD;
import gregtech6.block.GTBasicMachineBlock;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.Recipe;
import gregtech6.registry.GTMachines;

/**
 * The eu-special families row acceptance (task p29-w2-eu-special, the OFFLINE half —
 * the {@link GTCannerFamilyRowTest} shape): the fourteen row records pinned to the
 * upstream columns (Loader_MultiTileEntities.java:1497-1501 the Autocrafter EU 5-tier
 * ladder — the WAVE'S FIRST DUAL ENERGY FACE SBIT_U|SBIT_D (the exotic Polarizer
 * later joins with the same U|D face, task p29-w2-exotic-energy) / :1582-1586 the Lightning
 * Processor EU 5-tier ladder with NBT_USE_OUTPUT_TANK T / :1532-1535 the Laminator HU
 * 4-tier Heat_T ladder), the {@link GTMachines#euFiveTierWindow} 5-tier 立行制 resolver
 * (tier 4 = EV_TIER_INPUTS), the 5-row window arithmetic and the output-tank fallback
 * LIVE regression (the first-instance USE_OUTPUT_TANK key: the flag ARMS the
 * :716-732 output-tank recipe fallback — a machine without the flag refuses the same
 * state, a machine with it runs and drains the OUTPUT tank).
 *
 * <p>The lightning-strike attribution (the card discipline): the strike-into-network
 * face is the LightningRod MULTIBLOCK (18104, ported, task p24-lightning-rod) — the
 * Lightning Processor is a PLAIN EU consumer (:1582-1586 registers
 * MultiTileEntityBasicMachineElectric, NBT_ENERGY_ACCEPTED EU); only
 * NBT_USE_OUTPUT_TANK is this machine's porting point. The registration half
 * (14 blocks + 3 family BETs) only resolves on a live server (the runServer/RCON
 * gate, group p29_w2_eu_special).
 */
public class GT6EuSpecialFamiliesRowTest extends TileEntityBasicMachineOfflineTestBase {

	// the :1497 mask columns, verbatim over the :151/:137/:138 read ORs (the row bytes
	// carry the POST-read values, the dryer-row convention)
	/** CS.java:612 — SBIT_U|SBIT_D, the :1497 NBT_ENERGY_ACCEPTED_SIDES dual energy face. */
	static final byte AUTOCRAFTER_ENERGY_MASK = (byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A);
	/** SBIT_U|SBIT_L — the :1497 NBT_INV_SIDE_IN. */
	static final byte AUTOCRAFTER_IN_MASK = (byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A);
	/** SBIT_R|SBIT_D — the :1497 NBT_INV_SIDE_OUT. */
	static final byte AUTOCRAFTER_OUT_MASK = (byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A);
	/** SBIT_B — the :1582 NBT_ENERGY_ACCEPTED_SIDES. */
	static final byte LIGHTNING_ENERGY_MASK = (byte)(GTBasicMachineBlock.SBIT_B | GTBasicMachineBlock.SBIT_A);
	/** SBIT_U|SBIT_L — the :1582 NBT_INV_SIDE_IN == NBT_TANK_SIDE_IN. */
	static final byte LIGHTNING_IN_MASK = (byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A);
	/** SBIT_R|SBIT_D — the :1582 NBT_INV_SIDE_OUT == NBT_TANK_SIDE_OUT. */
	static final byte LIGHTNING_OUT_MASK = (byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A);
	/** SBIT_D — the :1532 NBT_ENERGY_ACCEPTED_SIDES (the p13 burning-box bottom-feed form). */
	static final byte LAMINATOR_ENERGY_MASK = (byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A);
	/** SBIT_L|SBIT_U — the :1532 NBT_INV_SIDE_IN. */
	static final byte LAMINATOR_IN_MASK = (byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A);
	/** SBIT_R — the :1532 NBT_INV_SIDE_OUT. */
	static final byte LAMINATOR_OUT_MASK = (byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A);

	/** The lightning probe row (the smoke row's data face poured DIRECTLY — eUt 16, duration 64). */
	private static void pourLightningProbeRow() {
		GT6RecipeMaps.LIGHTNING.addRecipe(new Recipe(true,
				new ItemStack[] {new ItemStack(Items.QUARTZ, 1), new ItemStack(Items.GLOWSTONE_DUST, 1)},
				new ItemStack[] {new ItemStack(Items.PRISMARINE_CRYSTALS, 1)},
				new FluidStack[] {new FluidStack(Fluids.WATER, 1000)}, null, 64, 16, 0));
	}

	/** The autocrafter probe row (4 planks → crafting_table — the vanilla-shape smoke row, eUt 16, duration 32). */
	private static void pourAutocrafterProbeRow() {
		GT6RecipeMaps.AUTOCRAFTER.addRecipe(new Recipe(true,
				new ItemStack[] {new ItemStack(Items.OAK_PLANKS, 4)},
				new ItemStack[] {new ItemStack(Items.CRAFTING_TABLE, 1)},
				null, null, 32, 16, 0));
	}

	/** The laminator probe row (piston + slime_ball → sticky_piston — the :190 listener row, eUt 16, duration 16). */
	private static void pourLaminatorProbeRow() {
		GT6RecipeMaps.LAMINATOR.addRecipe(new Recipe(true,
				new ItemStack[] {new ItemStack(Items.PISTON, 1), new ItemStack(Items.SLIME_BALL, 1)},
				new ItemStack[] {new ItemStack(Items.STICKY_PISTON, 1)},
				null, null, 16, 16, 0));
	}

	// ------------------------------------------------------------------
	// the 5-tier 立行制 resolver (the shared tier→window arithmetic)
	// ------------------------------------------------------------------

	/** TIER_INPUTS[0..3] + EV_TIER_INPUTS[0] — the resolver is the ONLY window source for the two 5-tier ladders. */
	@Test
	void euFiveTierWindowResolverPinned() {
		long[][] tExpected = {
				{16, 32, 64}, {64, 128, 256}, {256, 512, 1024}, {1024, 2048, 4096}, // TIER_INPUTS[0..3]
				{4096, 8192, 16384}}; // EV_TIER_INPUTS[0] — the T5/IV rung (:126 over NBT_INPUT 8192)
		assertEquals(5, tExpected.length);
		for (int i = 0; i < 5; i++) {
			org.junit.jupiter.api.Assertions.assertArrayEquals(tExpected[i], GTMachines.euFiveTierWindow(i),
					"the tier-" + (i + 1) + " window (min = in/2, max = in*2, the :126 conversion)");
		}
		// the tier-4 rung IS the card-① constant, not a new array
		org.junit.jupiter.api.Assertions.assertSame(GTMachines.EV_TIER_INPUTS, GTMachines.euFiveTierWindow(4),
				"tier 4 rides EV_TIER_INPUTS by IDENTITY — no window drift");
		org.junit.jupiter.api.Assertions.assertSame(GTMachines.TIER_INPUTS[2], GTMachines.euFiveTierWindow(2),
				"tiers 0..3 ride TIER_INPUTS by identity");
	}

	// ------------------------------------------------------------------
	// the Autocrafter ladder (:1497-1501)
	// ------------------------------------------------------------------

	@Test
	void autocrafterRowsMatchTheUpstreamColumns() {
		assertEquals(5, GTMachines.AUTOCRAFTER_ROWS.size(), "the FIVE Autocrafter rows (:1497-1501) — the 5-tier 立行制");
		String[] tVoltageWords = {"LV", "MV", "HV", "EV", "IV"};
		String[] tVoltageSlugs = {"lv", "mv", "hv", "ev", "iv"};
		for (int i = 0; i < GTMachines.AUTOCRAFTER_ROWS.size(); i++) {
			GTBasicMachineBlock.MachineRow tRow = GTMachines.AUTOCRAFTER_ROWS.get(i);
			int tTier = i + 1; // the upstream tier index, 1-based in the messages
			assertEquals("autocrafter" + (i == 0 ? "" : "_t" + tTier), tRow.path(), "the registry path ladder — _t5 is the FIRST 5-tier rung");
			assertEquals(20341 + i, tRow.metaId(), "the MultiTile id column of tier " + tTier);
			assertEquals(tTier, tRow.tier() + 1, "the tier index");
			assertEquals(4.0F, tRow.hardness(), "NBT_HARDNESS 4.0F on every row (NBT_RESISTANCE == hardness)");
			assertSame(TD.Energy.EU, tRow.energyType(), "NBT_ENERGY_ACCEPTED TD.Energy.EU on every row");
			assertSame(GT6RecipeMaps.AUTOCRAFTER, tRow.recipes().get(), "NBT_RECIPEMAP RM.AUTOCRAFTER through the supplier");
			assertEquals("autocrafter", tRow.texture(), "NBT_TEXTURE autocrafter on every row (the ladder shares the fronts)");
			assertEquals(1, tRow.parallel(), "NO NBT_PARALLEL on the :1497 rows → 1");
			assertFalse(tRow.parallelDuration(), "NO NBT_PARALLEL_DURATION on the :1497 rows → F");
			assertTrue(tRow.cheapOverclocking(), "the port :773 loop runs unconditionally (no config source)");
			assertNull(tRow.menu(), "the menu-less carrier (the null-menu convention — the crafting-grid GUI arm is the pool card's surface)");
			assertEquals("gt6.row.autocrafter.display", tRow.displayKey(), "the family template key");
			assertEquals(tVoltageWords[i], tRow.matDisplay(), "the voltage word VN[" + tTier + "] (CS.java:154, T5 = IV)");
			assertEquals(tVoltageSlugs[i], tRow.matSlug(), "the voltage slug (the gt6.row.mat key tail)");
			// the material column: Electric_T[1..4] + the T5 rung
			assertSame(i < 4 ? GTMachines.ELECTRIC_T_LADDER.get(i) : GTMachines.ELECTRIC_T5, tRow.material(),
					"the Electric_T material ladder of tier " + tTier + " (MT.java:3691)");
			// the connectivity + auto-side columns (:1497 verbatim)
			assertEquals(AUTOCRAFTER_ENERGY_MASK, tRow.energySides(), "NBT_ENERGY_ACCEPTED_SIDES SBIT_U|SBIT_D — the WAVE'S FIRST DUAL ENERGY FACE");
			assertEquals((byte)127, tRow.fluidIn(), "NO NBT_TANK_SIDE_IN key → the 127 default (the zero-fluid map)");
			assertEquals((byte)127, tRow.fluidOut(), "NO NBT_TANK_SIDE_OUT key");
			assertEquals((byte)-1, tRow.fluidAutoIn(), "SIDE_UNDEFINED tank auto in");
			assertEquals((byte)-1, tRow.fluidAutoOut(), "SIDE_UNDEFINED tank auto out");
			assertEquals(AUTOCRAFTER_IN_MASK, tRow.itemIn(), "NBT_INV_SIDE_IN SBIT_U|SBIT_L");
			assertEquals(AUTOCRAFTER_OUT_MASK, tRow.itemOut(), "NBT_INV_SIDE_OUT SBIT_R|SBIT_D");
			assertEquals(2, tRow.itemAutoIn(), "NBT_INV_SIDE_AUTO_IN SIDE_LEFT");
			assertEquals(4, tRow.itemAutoOut(), "NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT");
		}
	}

	/**
	 * The DUAL ENERGY FACE truth table — the :511 rotated connectivity gate over the
	 * :1497 mask: the up AND bottom relative faces accept (facing-invariant — every
	 * FACING_ROTATIONS row keeps bottom→bottom and top→top), every horizontal face
	 * refuses. This IS the acceptance-① offline half ("能量双面 U|D 供能贯通+其余面拒");
	 * the live half drives a real adjacent emitter per face (the RCON chain).
	 */
	@Test
	void autocrafterEnergyFaceTruthTable() {
		for (int i = 0; i < GTMachines.AUTOCRAFTER_ROWS.size(); i++) {
			GTBasicMachineBlock.MachineRow tRow = GTMachines.AUTOCRAFTER_ROWS.get(i);
			byte tMask = tRow.energySides();
			// the machine-relative verdicts (the faces themselves, facing-independent)
			assertTrue(GTSideTables_Top(tMask), "the TOP face accepts (SBIT_U bit)");
			assertTrue(GTSideTables_Bottom(tMask), "the BOTTOM face accepts (SBIT_D bit)");
			for (byte tSide = 2; tSide <= 5; tSide++) {
				assertFalse(GTSideTables_faceConnected(tSide, tMask),
						"the world side " + tSide + " refuses on every horizontal facing — the single-face rows reject (row " + tRow.path() + ")");
			}
			// the full isEnergyAcceptingFrom chain (BE-level, the fixture machine carries the row mask)
			TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.AUTOCRAFTER, 1, false, TD.Energy.EU);
			tMachine.mEnergyInputs = tMask;
			assertTrue(tMachine.isEnergyAcceptingFrom(TD.Energy.EU, (byte)1, true), "the machine accepts EU from the TOP face");
			assertTrue(tMachine.isEnergyAcceptingFrom(TD.Energy.EU, (byte)0, true), "the machine accepts EU from the BOTTOM face");
			for (byte tSide = 2; tSide <= 5; tSide++) {
				assertFalse(tMachine.isEnergyAcceptingFrom(TD.Energy.EU, tSide, true),
						"the machine REFUSES EU from the world side " + tSide + " (the other-faces-reject arm)");
			}
			assertFalse(tMachine.isEnergyAcceptingFrom(TD.Energy.RU, (byte)1, true), "the :501 type gate keeps the RU carrier out");
		}
	}

	/** The relative-TOP face verdict (faceConnected is facing-invariant for the vertical pair). */
	private static boolean GTSideTables_Top(byte aMask) {
		return gregtech6.util.GTSideTables.faceConnected((byte)2, (byte)1, aMask);
	}

	/** The relative-BOTTOM face verdict. */
	private static boolean GTSideTables_Bottom(byte aMask) {
		return gregtech6.util.GTSideTables.faceConnected((byte)2, (byte)0, aMask);
	}

	/** Every horizontal facing x the given world side (2..5) — the dual-face gate must refuse them all. */
	private static boolean GTSideTables_faceConnected(byte aWorldSide, byte aMask) {
		boolean tAny = false;
		for (byte tFacing = 2; tFacing <= 5; tFacing++) {
			tAny |= gregtech6.util.GTSideTables.faceConnected(tFacing, aWorldSide, aMask);
		}
		return tAny;
	}

	// ------------------------------------------------------------------
	// the Lightning Processor ladder (:1582-1586)
	// ------------------------------------------------------------------

	@Test
	void lightningRowsMatchTheUpstreamColumns() {
		assertEquals(5, GTMachines.LIGHTNING_ROWS.size(), "the FIVE Lightning rows (:1582-1586)");
		String[] tVoltageWords = {"LV", "MV", "HV", "EV", "IV"};
		for (int i = 0; i < GTMachines.LIGHTNING_ROWS.size(); i++) {
			GTBasicMachineBlock.MachineRow tRow = GTMachines.LIGHTNING_ROWS.get(i);
			int tTier = i + 1;
			assertEquals("lightning" + (i == 0 ? "" : "_t" + tTier), tRow.path(), "the registry path ladder (the NBT_TEXTURE token IS 'lightning')");
			assertEquals(20501 + i, tRow.metaId(), "the MultiTile id column of tier " + tTier);
			assertEquals(tTier, tRow.tier() + 1, "the tier index");
			assertEquals(4.0F, tRow.hardness(), "NBT_HARDNESS 4.0F on every row");
			assertSame(TD.Energy.EU, tRow.energyType(), "NBT_ENERGY_ACCEPTED EU — a PLAIN EU consumer, NO lightning-strike mechanism (the strike face is the LightningRod multiblock 18104)");
			assertSame(GT6RecipeMaps.LIGHTNING, tRow.recipes().get(), "NBT_RECIPEMAP RM.LIGHTNING");
			assertEquals("lightning", tRow.texture(), "NBT_TEXTURE lightning (the upstream token verbatim)");
			assertEquals(1, tRow.parallel(), "NO NBT_PARALLEL → 1");
			assertFalse(tRow.parallelDuration(), "NO NBT_PARALLEL_DURATION → F");
			assertNull(tRow.menu(), "the menu-less carrier");
			assertEquals("gt6.row.lightningprocessor.display", tRow.displayKey(), "the family template key (upstream 'Lightning Processor ('+VN[tier]+')')");
			assertEquals(tVoltageWords[i], tRow.matDisplay(), "the voltage word VN[" + tTier + "]");
			assertSame(i < 4 ? GTMachines.ELECTRIC_T_LADDER.get(i) : GTMachines.ELECTRIC_T5, tRow.material(), "the Electric_T material ladder");
			// the :1582 masks
			assertEquals(LIGHTNING_ENERGY_MASK, tRow.energySides(), "NBT_ENERGY_ACCEPTED_SIDES SBIT_B");
			assertEquals(LIGHTNING_IN_MASK, tRow.fluidIn(), "NBT_TANK_SIDE_IN SBIT_U|SBIT_L");
			assertEquals(LIGHTNING_OUT_MASK, tRow.fluidOut(), "NBT_TANK_SIDE_OUT SBIT_R|SBIT_D");
			assertEquals(LIGHTNING_IN_MASK, tRow.itemIn(), "NBT_INV_SIDE_IN SBIT_U|SBIT_L");
			assertEquals(LIGHTNING_OUT_MASK, tRow.itemOut(), "NBT_INV_SIDE_OUT SBIT_R|SBIT_D");
			assertEquals(1, tRow.fluidAutoIn(), "NBT_TANK_SIDE_AUTO_IN SIDE_TOP");
			assertEquals(0, tRow.fluidAutoOut(), "NBT_TANK_SIDE_AUTO_OUT SIDE_BOTTOM");
			assertEquals(2, tRow.itemAutoIn(), "NBT_INV_SIDE_AUTO_IN SIDE_LEFT");
			assertEquals(4, tRow.itemAutoOut(), "NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT");
		}
	}

	/**
	 * THE first-instance key regression (the acceptance-② core): NBT_USE_OUTPUT_TANK T
	 * arms the :716-732 output-tank recipe fallback — a recipe whose fluid input sits in
	 * the OUTPUT tank (never the input tank) runs and drains the output tank WITH the
	 * flag, and the SAME state refuses WITHOUT it. The flag is the cannerMachine
	 * factory-column form (capacity-free — the :1582 rows carry no NBT_TANK_CAPACITY).
	 */
	@Test
	void lightningOutputTankFallbackRunsAndDrainsTheOutputTank() {
		pourLightningProbeRow();
		assertEquals(1, GT6RecipeMaps.LIGHTNING.mRecipeList.size(), "the lightning probe row poured");

		// the fallback ARM: the fluid input lives ONLY in the OUTPUT tank
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.LIGHTNING, 1, false, TD.Energy.EU);
		tMachine.mCanUseOutputTanks = true; // NBT_USE_OUTPUT_TANK T (:1582) — the factory arm
		tMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.QUARTZ, 1), false);
		tMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT + 1, new ItemStack(Items.GLOWSTONE_DUST, 1), false);
		assertTrue(tMachine.mTanksInput[0].isEmpty(), "the INPUT tank stays empty — the fallback is the only fluid source");
		assertEquals(1000, tMachine.mTanksOutput[0].fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE),
				"the OUTPUT tank carries the water");

		drive(tMachine, 72); // eUt 16 x duration 64 → mMaxProgress 1024 = 16 ticks of mInputMax, 4x headroom

		ItemStack tOutput = tMachine.getInventory().getStackInSlot(tMachine.getInputSlotCount());
		assertEquals(Items.PRISMARINE_CRYSTALS, tOutput.getItem(), "the process RAN off the output tank (the :717/:718 fallback re-lookup)");
		assertEquals(1, tOutput.getCount());
		assertTrue(tMachine.mTanksOutput[0].isEmpty(), "the output tank DRAINED — the consume chain pulled the fluid from the OUTPUT tanks (:718)");
	}

	/** The flag IS the gate: the same output-tank-only state refuses on a machine without NBT_USE_OUTPUT_TANK. */
	@Test
	void withoutTheFlagTheOutputTankStateRefuses() {
		pourLightningProbeRow();
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.LIGHTNING, 1, false, TD.Energy.EU);
		assertFalse(tMachine.mCanUseOutputTanks, "the constructed default is FALSE (the :92 upstream default) — the flag is per-family, not ambient");
		tMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.QUARTZ, 1), false);
		tMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT + 1, new ItemStack(Items.GLOWSTONE_DUST, 1), false);
		tMachine.mTanksOutput[0].fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);

		drive(tMachine, 72);

		assertEquals(ItemStack.EMPTY, tMachine.getInventory().getStackInSlot(tMachine.getInputSlotCount()),
				"NO output — the input-tank lookup failed and the :717 fallback is disarmed");
		assertEquals(1000, tMachine.mTanksOutput[0].amount(), "the output tank is UNTOUCHED");

		// the ordinary leg on a FRESH machine: the water in the INPUT tank runs regardless of
		// the flag (the :712 primary lookup is the every-legacy-machine behaviour)
		TileEntityBasicMachine tPlain = makeMachine(GT6RecipeMaps.LIGHTNING, 1, false, TD.Energy.EU);
		tPlain.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.QUARTZ, 1), false);
		tPlain.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT + 1, new ItemStack(Items.GLOWSTONE_DUST, 1), false);
		tPlain.mTanksInput[0].fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
		drive(tPlain, 72);
		assertEquals(Items.PRISMARINE_CRYSTALS, tPlain.getInventory().getStackInSlot(tPlain.getInputSlotCount()).getItem(),
				"the input-tank leg is unaffected by the flag (the :712 primary lookup)");
		assertTrue(tPlain.mTanksInput[0].isEmpty(), "the input tank drained (the ordinary consume)");
	}

	// ------------------------------------------------------------------
	// the Laminator ladder (:1532-1535)
	// ------------------------------------------------------------------

	@Test
	void laminatorRowsMatchTheUpstreamColumns() {
		assertEquals(4, GTMachines.LAMINATOR_ROWS.size(), "the FOUR Laminator rows (:1532-1535) — the HU 4-tier Heat_T ladder");
		String[] tMatSlugs = {"steel", "invar", "titanium", "tungsten_carbide"};
		String[] tMatWords = {"Steel", "Invar", "Titanium", "Tungsten Carbide"};
		float[] tHardness = {6.0F, 4.0F, 9.0F, 12.5F};
		for (int i = 0; i < GTMachines.LAMINATOR_ROWS.size(); i++) {
			GTBasicMachineBlock.MachineRow tRow = GTMachines.LAMINATOR_ROWS.get(i);
			int tTier = i + 1;
			assertEquals("laminator" + (i == 0 ? "" : "_t" + tTier), tRow.path(), "the registry path ladder");
			assertEquals(20391 + i, tRow.metaId(), "the MultiTile id column of tier " + tTier);
			assertEquals(tTier, tRow.tier() + 1, "the tier index");
			assertEquals(tHardness[i], tRow.hardness(), "NBT_HARDNESS " + tHardness[i] + "F of tier " + tTier + " (NBT_RESISTANCE == hardness)");
			assertSame(TD.Energy.HU, tRow.energyType(), "NBT_ENERGY_ACCEPTED TD.Energy.HU");
			assertSame(GT6RecipeMaps.LAMINATOR, tRow.recipes().get(), "NBT_RECIPEMAP RM.LAMINATOR");
			assertEquals("laminator", tRow.texture(), "NBT_TEXTURE laminator");
			assertEquals(1, tRow.parallel(), "NO NBT_PARALLEL → 1");
			assertFalse(tRow.parallelDuration(), "NO NBT_PARALLEL_DURATION → F");
			assertNull(tRow.menu(), "the menu-less carrier — the upstream NBT_GUI machines/Laminator.png path stays a DECLARED fidelity record (the section doc), zero MenuType");
			assertEquals("gt6.row.laminator.display", tRow.displayKey(), "the family template key (upstream 'Laminator ('+aMat.getLocal()+')')");
			assertEquals(tMatWords[i], tRow.matDisplay(), "the Heat_T material word (the oven-family form, NOT a voltage word)");
			assertEquals(tMatSlugs[i], tRow.matSlug(), "the material slug");
			assertSame(GTMachines.HEAT_T_LADDER.get(i), tRow.material(), "the Heat_T[1..4] material ladder");
			// the :1532 masks
			assertEquals(LAMINATOR_ENERGY_MASK, tRow.energySides(), "NBT_ENERGY_ACCEPTED_SIDES SBIT_D — the p13 bottom-feed form");
			assertEquals((byte)127, tRow.fluidIn(), "NO NBT_TANK_SIDE_IN key → the 127 default");
			assertEquals((byte)127, tRow.fluidOut(), "NO NBT_TANK_SIDE_OUT key");
			assertEquals((byte)-1, tRow.fluidAutoIn(), "SIDE_UNDEFINED tank auto in");
			assertEquals((byte)-1, tRow.fluidAutoOut(), "SIDE_UNDEFINED tank auto out");
			assertEquals(LAMINATOR_IN_MASK, tRow.itemIn(), "NBT_INV_SIDE_IN SBIT_L|SBIT_U");
			assertEquals(LAMINATOR_OUT_MASK, tRow.itemOut(), "NBT_INV_SIDE_OUT SBIT_R");
			assertEquals(2, tRow.itemAutoIn(), "NBT_INV_SIDE_AUTO_IN SIDE_LEFT");
			assertEquals(4, tRow.itemAutoOut(), "NBT_INV_SIDE_AUTO_OUT SIDE_RIGHT");
			// the HU window of the tier — the shared TIER_INPUTS table through the :126 conversion
			org.junit.jupiter.api.Assertions.assertArrayEquals(GTMachines.TIER_INPUTS[i], GTMachines.euFiveTierWindow(tRow.tier()),
					"the tier-" + tTier + " HU window rides the SHARED TIER_INPUTS row");
		}
	}

	/** The 5-tier ladders' row tiers land on the resolver the same way (the acceptance-③ "逐行" face over BOTH ladders). */
	@Test
	void fiveTierRowsLandOnTheirWindows() {
		for (java.util.List<GTBasicMachineBlock.MachineRow> tLadder : java.util.List.of(GTMachines.AUTOCRAFTER_ROWS, GTMachines.LIGHTNING_ROWS)) {
			for (int i = 0; i < tLadder.size(); i++) {
				GTBasicMachineBlock.MachineRow tRow = tLadder.get(i);
				long[] tWindow = GTMachines.euFiveTierWindow(tRow.tier());
				if (tRow.tier() < 4) {
					assertSame(GTMachines.TIER_INPUTS[tRow.tier()], tWindow, tRow.path() + " rides the shared TIER_INPUTS row");
				} else {
					assertSame(GTMachines.EV_TIER_INPUTS, tWindow, tRow.path() + " rides EV_TIER_INPUTS (the T5 rung)");
					assertEquals(4096L, tWindow[0], "the T5 min (= T4 max — the seamless wall)");
					assertEquals(16384L, tWindow[2], "the T5 max");
				}
			}
		}
	}


	/** The other two smoke rows drive their machines e2e (the autocrafter/laminator legs of acceptance-④). */
	@Test
	void autocrafterAndLaminatorSmokeRowsRunTheirMachines() {
		pourAutocrafterProbeRow();
		pourLaminatorProbeRow();

		// the autocrafter leg: 4 planks → crafting_table (the vanilla-shape smoke row)
		TileEntityBasicMachine tAutocrafter = makeMachine(GT6RecipeMaps.AUTOCRAFTER, 1, false, TD.Energy.EU);
		tAutocrafter.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.OAK_PLANKS, 4), false);
		drive(tAutocrafter, 40); // eUt 16 x duration 32 → mMaxProgress 512 = 8 ticks of mInputMax, 4x headroom
		assertEquals(Items.CRAFTING_TABLE, tAutocrafter.getInventory().getStackInSlot(tAutocrafter.getInputSlotCount()).getItem(),
				"the autocrafter smoke row runs the machine (the crafting-grid arm stays pooled — the map consumes the JSON row)");

		// the laminator leg: piston + slime_ball → sticky_piston (the :190 listener row verbatim in port ids)
		TileEntityBasicMachine tLaminator = makeMachine(GT6RecipeMaps.LAMINATOR, 1, false, TD.Energy.HU);
		tLaminator.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.PISTON, 1), false);
		tLaminator.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT + 1, new ItemStack(Items.SLIME_BALL, 1), false);
		drive(tLaminator, 20); // eUt 16 x duration 16 → mMaxProgress 256 = 4 ticks of mInputMax, 4x headroom
		assertEquals(Items.STICKY_PISTON, tLaminator.getInventory().getStackInSlot(tLaminator.getInputSlotCount()).getItem(),
				"the laminator smoke row runs the HU machine");
	}
}
