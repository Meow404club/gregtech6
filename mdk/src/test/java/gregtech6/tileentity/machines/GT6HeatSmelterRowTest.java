package gregtech6.tileentity.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
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
import gregtech6.recipes.RecipeMap;
import gregtech6.registry.GTMachines;
import gregtech6.tileentity.machines.TileEntityBasicMachine;

/**
 * The heat-smelter row acceptance (task p29-w3-heat-smelter, the OFFLINE half — the
 * {@link GT6HuTuPiggybackRowTest} shape): the Smelter HU 4-ladder 20241-20244 and the
 * Melter single 22010 pinned to the upstream columns (Loader_MultiTileEntities.java
 * :1431-1434 / :1657), the SHARED mask shape 对拍 (both families carry the IDENTICAL
 * :1431 mask set — inv+tank in SBIT_U auto TOP, inv out SBIT_L auto LEFT, tank out
 * SBIT_R auto RIGHT, energy SBIT_D — split only by map and texture), the NBT_PARALLEL
 * 1000 + NBT_PARALLEL_DURATION T columns on every row, and the parallel-1000 dynamics:
 * the :766-768 linear-duration arm (the bar scales with the count) against the
 * :626-629 chain-processing power cap (a heavy row collapses the count to 1 — the
 * 半程×N 对拍 pairs).
 *
 * <p>The registration half (5 blocks + 5 items + 2 family BETs) only resolves on a live
 * server (the RCON gate — the sweep heat_smelter group).
 */
public class GT6HeatSmelterRowTest extends TileEntityBasicMachineOfflineTestBase {

	/** The :1431 masks, post-read (both heat families share the IDENTICAL row shape). */
	private static final byte HEAT_ENERGY = (byte)(GTBasicMachineBlock.SBIT_D | GTBasicMachineBlock.SBIT_A);
	private static final byte HEAT_IN = (byte)(GTBasicMachineBlock.SBIT_U | GTBasicMachineBlock.SBIT_A);
	private static final byte HEAT_INV_OUT = (byte)(GTBasicMachineBlock.SBIT_L | GTBasicMachineBlock.SBIT_A);
	private static final byte HEAT_TANK_OUT = (byte)(GTBasicMachineBlock.SBIT_R | GTBasicMachineBlock.SBIT_A);
	private static final byte TOP = 1, LEFT = 2, RIGHT = 4;

	// ------------------------------------------------------------------
	// the Smelter ladder: the :1431-1434 columns per tier (ACCEPTANCE ①)
	// ------------------------------------------------------------------

	@Test
	void smelterRowsMatchTheUpstreamColumns() {
		String[] tSlugs = {"steel", "invar", "titanium", "tungsten_carbide"};
		float[] tHardness = {6.0F, 4.0F, 9.0F, 12.5F};
		int[] tInputs = {32, 128, 512, 2048}; // the upstream NBT_INPUT column
		assertEquals(4, GTMachines.SMELTER_ROWS.size(), "the four Smelter rows (:1431-1434)");
		for (int i = 0; i < GTMachines.SMELTER_ROWS.size(); i++) {
			GTBasicMachineBlock.MachineRow tRow = GTMachines.SMELTER_ROWS.get(i);
			assertEquals("smelter" + (i == 0 ? "" : "_t" + (i + 1)), tRow.path());
			assertEquals(20241 + i, tRow.metaId(), "the MultiTile id column of tier " + (i + 1));
			assertEquals(i, tRow.tier(), "the TIER_INPUTS tier index");
			assertEquals(tSlugs[i], tRow.matSlug(), "the Heat_T word set");
			assertEquals(tHardness[i], tRow.hardness(), "the Heat_T hardness ladder");
			assertSame(TD.Energy.HU, tRow.energyType(), "NBT_ENERGY_ACCEPTED TD.Energy.HU");
			assertSame(GT6RecipeMaps.SMELTER, tRow.recipes().get(), "NBT_RECIPEMAP RM.Smelter");
			assertEquals("smelter", tRow.texture(), "NBT_TEXTURE");
			assertEquals(GTMachines.SMELTER_PARALLEL, tRow.parallel(), "NBT_PARALLEL 1000");
			assertTrue(tRow.parallelDuration(), "NBT_PARALLEL_DURATION T");
			assertEquals(GTMachines.MACHINE_SMELTER_UNIT_KEY, tRow.displayKey(), "the material-word one-slot form");
			assertHeatMasks(tRow);
			// the BE half: the TIER_INPUTS window IS the upstream NBT_INPUT column
			TileEntityBasicMachine tMachine = rowMachine(tRow);
			assertEquals(GTMachines.TIER_INPUTS[i][0], tMachine.mInputMin, "the :126 window of tier " + (i + 1));
			assertEquals(tInputs[i], tMachine.mInput, "NBT_INPUT " + tInputs[i] + " == TIER_INPUTS[" + i + "][1]");
			assertEquals(GTMachines.TIER_INPUTS[i][2], tMachine.mInputMax);
		}
	}

	// ------------------------------------------------------------------
	// the Melter single: the :1657 columns + the shared-mask 对拍 (ACCEPTANCE ②)
	// ------------------------------------------------------------------

	@Test
	void melterRowMatchesTheUpstreamColumnsAndSharesTheSmelterMaskShape() {
		GTBasicMachineBlock.MachineRow tRow = GTMachines.MELTER_ROWS.get(0);
		assertEquals(1, GTMachines.MELTER_ROWS.size(), "the ONE Melter row (:1657)");
		assertEquals("melter", tRow.path());
		assertEquals(22010, tRow.metaId());
		assertEquals("iron", tRow.matSlug(), "the :1657 ANY.Iron housing column");
		assertEquals(6.0F, tRow.hardness(), "NBT_HARDNESS 6.0F");
		assertEquals(0, tRow.tier(), "the TIER_INPUTS tier index");
		assertSame(TD.Energy.HU, tRow.energyType(), "NBT_ENERGY_ACCEPTED TD.Energy.HU");
		assertSame(GT6RecipeMaps.MELTER, tRow.recipes().get(), "NBT_RECIPEMAP RM.Melter");
		assertEquals("melter", tRow.texture(), "NBT_TEXTURE");
		assertEquals(GTMachines.SMELTER_PARALLEL, tRow.parallel(), "NBT_PARALLEL 1000 — the same column as the Smelter");
		assertTrue(tRow.parallelDuration(), "NBT_PARALLEL_DURATION T");
		assertEquals(GTMachines.MELTER_DISPLAY_KEY, tRow.displayKey(), "the atomic no-slot display template");
		// the 对拍 half: the SAME mask shape as every Smelter row — split only by map+texture
		GTBasicMachineBlock.MachineRow tSmelter = GTMachines.SMELTER_ROWS.get(0);
		assertEquals(tSmelter.energySides(), tRow.energySides());
		assertEquals(tSmelter.itemIn(), tRow.itemIn());
		assertEquals(tSmelter.itemOut(), tRow.itemOut());
		assertEquals(tSmelter.fluidIn(), tRow.fluidIn());
		assertEquals(tSmelter.fluidOut(), tRow.fluidOut());
		assertEquals(tSmelter.fluidAutoIn(), tRow.fluidAutoIn());
		assertEquals(tSmelter.fluidAutoOut(), tRow.fluidAutoOut());
		assertEquals(tSmelter.itemAutoIn(), tRow.itemAutoIn());
		assertEquals(tSmelter.itemAutoOut(), tRow.itemAutoOut());
		assertNotSame(GT6RecipeMaps.SMELTER, GT6RecipeMaps.MELTER, "two maps, one mask shape");
		// the BE half: the TIER_INPUTS[0] window = NBT_INPUT 32
		TileEntityBasicMachine tMachine = rowMachine(tRow);
		assertEquals(16L, tMachine.mInputMin, "the :126 window of tier 1");
		assertEquals(32L, tMachine.mInput, "NBT_INPUT 32 == TIER_INPUTS[0][1]");
		assertEquals(64L, tMachine.mInputMax);
	}

	private void assertHeatMasks(GTBasicMachineBlock.MachineRow aRow) {
		assertEquals(HEAT_ENERGY, aRow.energySides(), "NBT_ENERGY_ACCEPTED_SIDES SBIT_D through the :151 read");
		assertEquals(HEAT_IN, aRow.itemIn(), "NBT_INV_SIDE_IN SBIT_U");
		assertEquals(HEAT_INV_OUT, aRow.itemOut(), "NBT_INV_SIDE_OUT SBIT_L");
		assertEquals(HEAT_IN, aRow.fluidIn(), "NBT_TANK_SIDE_IN SBIT_U");
		assertEquals(HEAT_TANK_OUT, aRow.fluidOut(), "NBT_TANK_SIDE_OUT SBIT_R");
		assertEquals(TOP, aRow.fluidAutoIn(), "NBT_TANK_SIDE_AUTO_IN SIDE_TOP");
		assertEquals(RIGHT, aRow.fluidAutoOut(), "NBT_TANK_SIDE_AUTO_OUT SIDE_RIGHT");
		assertEquals(TOP, aRow.itemAutoIn(), "NBT_INV_SIDE_AUTO_IN SIDE_TOP");
		assertEquals(LEFT, aRow.itemAutoOut(), "NBT_INV_SIDE_AUTO_OUT SIDE_LEFT");
	}

	// ------------------------------------------------------------------
	// the two maps carry the IDENTICAL RM.java:131/:132 constants row
	// ------------------------------------------------------------------

	@Test
	void theMelterAndSmelterMapsShareTheIdenticalConstantsRow() {
		for (RecipeMap tMap : new RecipeMap[] {GT6RecipeMaps.MELTER, GT6RecipeMaps.SMELTER}) {
			assertEquals(1, tMap.mInputItemsCount, "1 item input slot (RM.java:131/:132)");
			assertEquals(1, tMap.mOutputItemsCount, "1 item output slot");
			assertEquals(1, tMap.mInputFluidCount, "1 fluid input slot");
			assertEquals(1, tMap.mOutputFluidCount, "1 fluid output slot");
			assertEquals(1, tMap.mMinimalInputs, "MIN 1 — the melting maps' minimum ingredient count");
		}
	}

	// ------------------------------------------------------------------
	// the ice smoke row RUNS on both machines (the datapack face, offline) and the
	// parallelDuration bar scales linearly with the parallel count (ACCEPTANCE ①
	// 半程×N 对拍, leg A: the count-N bar)
	// ------------------------------------------------------------------

	@Test
	void theIceRowRunsAndTheBarScalesLinearlyWithTheCount() {
		TileEntityBasicMachine.ENERGY_FAKE_SOURCE = false;
		// smelter.json / melter.json: 1 ice -> 1000 L water, eUt 16, duration 2000
		Recipe tIceRow = new Recipe(true,
				new ItemStack[] {new ItemStack(Items.ICE, 1)},
				new ItemStack[0], null,
				new FluidStack[] {new FluidStack(Fluids.WATER, 1000)}, 2000, 16, 0);
		for (java.util.List<GTBasicMachineBlock.MachineRow> tFamily : java.util.List.of(GTMachines.SMELTER_ROWS, GTMachines.MELTER_ROWS)) {
			tFamily.get(0).recipes().get().addRecipe(tIceRow);
			TileEntityBasicMachine tMachine = rowMachine(tFamily.get(0));
			tMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.ICE, 1), false);
			assertEquals(TileEntityBasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tMachine.checkRecipe(true, true),
					"the ice row binds (the item leg alone; the map has MIN 1, item 1/1/0)");
			// the parallelDuration arm with count 1: units(16 × 2000 × 1, 10000, 10000, T) = 32000;
			// the :773 overclock loop is inert at T1 (mMinEnergy 16 == mInputMin 16)
			assertEquals(32000L, tMachine.mMaxProgress, "the linear-duration bar of ONE process");
			inject(tMachine, 1000, 32);
			assertEquals(1000, tMachine.mTanksOutput[0].getFluidAmount(), "1000 L water landed in the output tank (1000 ticks @ 32/tick)");
		}
	}

	@Test
	void aCheapRowBanksCountTimesTheWorkThroughTheThousandParallel() {
		TileEntityBasicMachine.ENERGY_FAKE_SOURCE = false;
		// a CHEAP row (eut 16, duration 2 → power 32) — the :626-629 cap floor(64 × 600 / 32)
		// = 1200 leaves the full 1000 parallel reachable, so 64 items ride ONE bar
		GT6RecipeMaps.SMELTER.addRecipe(new Recipe(true,
				new ItemStack[] {new ItemStack(Items.BRICK, 1)},
				new ItemStack[0], null,
				new FluidStack[] {new FluidStack(Fluids.WATER, 1)}, 2, 16, 0));
		TileEntityBasicMachine tMachine = rowMachine(GTMachines.SMELTER_ROWS.get(0));
		tMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.BRICK, 64), false);
		assertEquals(TileEntityBasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tMachine.checkRecipe(true, true));
		// the :766-768 linear-duration face of the THOUSAND-parallel machine: the bar is
		// units(16 × 2 × 64, 10000, 10000) = 2048 — count × the single bar (a parallel-1
		// machine would bank 32)
		assertEquals(2048L, tMachine.mMaxProgress, "the bar scaled by the 64-process count (N × the half-way 对拍 arm)");
		inject(tMachine, 64, 32);
		assertEquals(64, tMachine.mTanksOutput[0].getFluidAmount(), "64 processes completed in ONE bar — the parallel-1000 count at work");
	}

	@Test
	void aHeavyRowCollapsesTheThousandParallelToThePowerCap() {
		TileEntityBasicMachine.ENERGY_FAKE_SOURCE = false;
		// the ICE row again (power 32000): the :626-629 chain-processing cap on the T1
		// window (max 64) is floor(64 × 600 / 32000) = 1 — the count collapses below ONE
		// even with a full stack in (the linear bar then carries count 1 only)
		GT6RecipeMaps.SMELTER.addRecipe(new Recipe(true,
				new ItemStack[] {new ItemStack(Items.ICE, 1)},
				new ItemStack[0], null,
				new FluidStack[] {new FluidStack(Fluids.WATER, 1000)}, 2000, 16, 0));
		TileEntityBasicMachine tMachine = rowMachine(GTMachines.SMELTER_ROWS.get(0));
		tMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.ICE, 64), false);
		assertEquals(TileEntityBasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tMachine.checkRecipe(true, true));
		assertEquals(32000L, tMachine.mMaxProgress, "the bar is ONE process — the power cap pins the count to 1");
		inject(tMachine, 1000, 32);
		assertEquals(1000, tMachine.mTanksOutput[0].getFluidAmount(), "exactly ONE process ran to the output tank");
	}

	// ------------------------------------------------------------------
	// the fixtures (the GT6HuTuPiggybackRowTest shape — the kineticMachine body)
	// ------------------------------------------------------------------

	/** The BET-factory fixture: the machine() half + applyRow (the kineticMachine body). */
	private static TileEntityBasicMachine rowMachine(GTBasicMachineBlock.MachineRow aRow) {
		TileEntityBasicMachine tMachine = makeMachine(aRow.recipes().get(), aRow.parallel(), aRow.parallelDuration(), aRow.energyType());
		long[] tInputs = GTMachines.TIER_INPUTS[aRow.tier()]; // the machine() factory assignment (the kineticMachine body)
		tMachine.mInputMin = tInputs[0];
		tMachine.mInput = tInputs[1];
		tMachine.mInputMax = tInputs[2];
		return GTMachines.applyRow(tMachine, aRow);
	}

	/** The direct-inject driver: one doInject + one dispatcher tick per iteration. */
	private static void inject(TileEntityBasicMachine aMachine, int aTicks, long aSize) {
		for (int i = 0; i < aTicks; i++) {
			aMachine.doInject(aMachine.mEnergyTypeAccepted, (byte)0, aSize, 1, true);
			aMachine.updateEntity();
		}
	}
}
