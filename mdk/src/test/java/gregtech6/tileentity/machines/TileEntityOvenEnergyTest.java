package gregtech6.tileentity.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.junit.jupiter.api.Test;

import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;
import gregtech6.tileentity.machines.GTMachinesOfflineTestBase.MachineLevel;

/**
 * Acceptance 1 (task p4-machine-oven): the energy semantics of the ADR-P4 option-A fake
 * power source, traced against the upstream math (MultiTileEntityBasicMachine.java:761-774
 * energy, :780-793 doWork, :795-887 doActive, :894 CONSTANT_ENERGY reset).
 *
 * <p>Completion timing: :811 and :815 are sequential ifs, not else-if — the tick that steps
 * the progress onto mMaxProgress also places the outputs, so a 256-unit process at 64
 * energy/tick completes on its 4th active tick.
 */
public class TileEntityOvenEnergyTest extends GTMachinesOfflineTestBase {

	@Test
	void fullVoltageDefaultsGiveUnitsMEutTimesMDuration() {
		// Furnace recipe mEUt=16 / mDuration=16 (RecipeMapFurnace :154) at the tier-1 defaults
		// mInputMin=16/mInput=32/mInputMax=64 (upstream :98): mMinEnergy=16, no overclock
		// (16 < 16 false, :773), mMaxProgress = units(16*16, 10000, 10000, T) = 256.
		MachineLevel tLevel = smeltingLevel();
		TileEntityOven tOven = makeOven(tLevel);
		tOven.getInventory().insertItem(TileEntityOven.SLOT_INPUT, new ItemStack(Items.SAND, 8), false);

		drive(tOven, 1); // probe (onTickFirst) + apply (doActive)
		assertEquals(16, tOven.mMinEnergy, "mMinEnergy = max(1, mEUt), TU branch :770");
		assertEquals(256, tOven.mMaxProgress, "mMaxProgress = units(mEUt*mDuration, 10000, 10000, T) :771");
		assertEquals(7, tOven.getInventory().getStackInSlot(TileEntityOven.SLOT_INPUT).getCount(), "the apply consumed one input");
		assertEquals(64, tOven.mProgress, "one active tick advanced the progress by min(mInputMax, mEnergy) = 64 energy units (:813)");
		assertTrue(tOven.mActive, "the machine is processing (:782)");
		assertTrue(tOven.mRunning, ":783");
	}

	@Test
	void progressAdvancesByEnergyUnitsAndCarriesLeftoverOver() {
		// The furnace process (256 units) completes exactly on the 4th active tick (64*4 = 256,
		// zero carryover); a duration-15 recipe (240 units) leaves a real 16-unit overshoot for
		// the next process — the carryover :843. The mIgnited=40 re-check window (:851) drives
		// the next doActive recipe check; without it the :803 reset would wipe the carryover.
		MachineLevel tLevel = smeltingLevel();
		TileEntityOven tOven = makeOven(tLevel);
		RecipeMap tShortMap = new RecipeMap(new HashSet<>(), "gt6.test.short", "Short Test", "shorttest",
				0, 1, "gt6:textures/gui/machines/Oven",
				1, 1, 1, 0, 0, 0, 0, 1);
		tShortMap.addRecipe(new Recipe(true, new ItemStack[] {new ItemStack(Items.SAND)}, new ItemStack[] {new ItemStack(Items.GLASS)}, null, null, 15, 16, 0));
		tOven.mRecipes = tShortMap;
		tOven.getInventory().insertItem(TileEntityOven.SLOT_INPUT, new ItemStack(Items.SAND, 2), false);

		drive(tOven, 3);
		assertEquals(192, tOven.mProgress, "64 per tick for three active ticks");
		assertTrue(tOven.getInventory().getStackInSlot(TileEntityOven.SLOT_OUTPUT).isEmpty(), "not complete yet");

		drive(tOven, 1); // completion tick: 192 <= 240 -> 256 -> place -> carry 256-240 = 16
		assertEquals(1, tOven.getInventory().getStackInSlot(TileEntityOven.SLOT_OUTPUT).getCount(), "glass placed");
		assertEquals(Items.GLASS, tOven.getInventory().getStackInSlot(TileEntityOven.SLOT_OUTPUT).getItem());
		assertEquals(16, tOven.mProgress, "carryover :843 (mProgress -= mMaxProgress)");
		assertEquals(0, tOven.mMaxProgress, "process reset");
		assertTrue(tOven.mSuccessful, "the completion tick is the success state (:850)");
		assertTrue(tOven.mIgnited > 0, "the re-check window opened (:851)");

		drive(tOven, 1); // mIgnited keeps the recipe check alive -> second process starts from 16
		assertEquals(0, tOven.getInventory().getStackInSlot(TileEntityOven.SLOT_INPUT).getCount(), "second input consumed");
		assertEquals(80, tOven.mProgress, "carryover 16 + one active tick 64");
		assertEquals(240, tOven.mMaxProgress, "the next process is underway");
	}

	@Test
	void exactFitFurnaceProcessCompletesWithZeroCarryover() {
		// The vanilla-bridge recipe: 256 units at 64/tick — mProgress lands exactly on
		// mMaxProgress, so the completion fires on the 4th active tick with no leftover.
		TileEntityOven tOven = makeOven(smeltingLevel());
		tOven.getInventory().insertItem(TileEntityOven.SLOT_INPUT, new ItemStack(Items.SAND, 1), false);

		drive(tOven, 3);
		assertEquals(192, tOven.mProgress);
		assertTrue(tOven.getInventory().getStackInSlot(TileEntityOven.SLOT_OUTPUT).isEmpty());

		drive(tOven, 1);
		assertEquals(1, tOven.getInventory().getStackInSlot(TileEntityOven.SLOT_OUTPUT).getCount());
		assertEquals(0, tOven.mProgress, "exact fit: 256 - 256 = 0");
		assertEquals(0, tOven.mMaxProgress);
	}

	@Test
	void energyIsDrainedPerWorkTickAndRefilledByTheFakeSource() {
		// Option A: onTick refills mEnergy = mInputMax while not stopped (:454 replacement),
		// doWork drains mInputMax (:791) — the drained state is observable after every tick.
		TileEntityOven tOven = makeOven(smeltingLevel());
		tOven.getInventory().insertItem(TileEntityOven.SLOT_INPUT, new ItemStack(Items.SAND, 1), false);

		drive(tOven, 1);
		assertEquals(0, tOven.mEnergy, ":791 drained the fake source after the work tick");

		drive(tOven, 1);
		assertEquals(0, tOven.mEnergy, "every work tick ends drained");
	}

	@Test
	void overclockingQuadruplesEnergyAndHalvesTime() {
		// :773 verbatim: while (mMinEnergy < mInputMin && mMinEnergy*4 <= mInputMax) { x4 energy, x2 speed }
		// A 4 EU/t recipe: mMinEnergy 4 -> 16, mMaxProgress units(64,..)=64 -> 128 (one OC step;
		// the second iteration fails on 16 < 16).
		MachineLevel tLevel = smeltingLevel();
		TileEntityOven tOven = makeOven(tLevel);
		RecipeMap tCheapMap = new RecipeMap(new HashSet<>(), "gt6.test.oc", "OC Test", "octest",
				0, 1, "gt6:textures/gui/machines/Oven",
				1, 1, 1, 0, 0, 0, 0, 1);
		tCheapMap.addRecipe(new Recipe(true, new ItemStack[] {new ItemStack(Items.SAND)}, new ItemStack[] {new ItemStack(Items.GLASS)}, null, null, 16, 4, 0));
		tOven.mRecipes = tCheapMap;
		tOven.getInventory().insertItem(TileEntityOven.SLOT_INPUT, new ItemStack(Items.SAND, 4), false);

		drive(tOven, 1);
		assertEquals(16, tOven.mMinEnergy, "overclocked to the voltage floor mInputMin");
		assertEquals(128, tOven.mMaxProgress, "4x energy -> 2x speed: 64 -> 128");
		assertEquals(64, tOven.mProgress);

		drive(tOven, 1); // tick1: 64 <= 128 -> 128 -> complete, carryover 128-128 = 0 (exact fit)
		assertEquals(1, tOven.getInventory().getStackInSlot(TileEntityOven.SLOT_OUTPUT).getCount());
		assertEquals(0, tOven.mProgress, "no leftover on an exact-fit overclocked process");
		assertEquals(0, tOven.mMaxProgress);

		drive(tOven, 1); // tick2: the mIgnited re-check starts the second overclocked process
		assertEquals(2, tOven.getInventory().getStackInSlot(TileEntityOven.SLOT_INPUT).getCount());
		assertEquals(128, tOven.mMaxProgress, "overclock applies to every process of the recipe");
		assertEquals(64, tOven.mProgress);
	}

	@Test
	void stoppedMachineStarvesAndResetsProgressThenResumes() {
		// Manual stop (NBT-persisted mStopped) gates the fake source -> doWork starves -> after
		// tick 40 doInactive resets the progress (CONSTANT_ENERGY = GT_API.java:510 default T, :894).
		MachineLevel tLevel = smeltingLevel();
		TileEntityOven tOven = makeOven(tLevel);
		tOven.getInventory().insertItem(TileEntityOven.SLOT_INPUT, new ItemStack(Items.SAND, 8), false);
		tOven.mStopped = true;

		drive(tOven, 50);
		assertEquals(0, tOven.mEnergy, "the stopped machine receives no energy");
		assertEquals(0, tOven.mProgress, "doInactive reset the progress (:894)");
		assertEquals(8, tOven.getInventory().getStackInSlot(TileEntityOven.SLOT_INPUT).getCount(), "nothing consumed while stopped");
		assertTrue(tOven.getInventory().getStackInSlot(TileEntityOven.SLOT_OUTPUT).isEmpty(), "no output while stopped");
		assertFalse(tOven.getStateOnOff(), "getStateOnOff = !mStopped (:1028)");
		assertFalse(tOven.mRunning);

		assertTrue(tOven.setStateOnOff(true), "setStateOnOff(true) resumes (:1027)");
		assertFalse(tOven.mStopped);

		drive(tOven, 1);
		assertEquals(256, tOven.mMaxProgress, "the resumed machine picks the recipe up again");
		assertEquals(7, tOven.getInventory().getStackInSlot(TileEntityOven.SLOT_INPUT).getCount());
	}

	@Test
	void redstoneStopGatesTheSourceWithoutTouchingTheManualStop() {
		// Option C: a neighbor signal gates the fake source through the runtime latch; the
		// falling edge does not clobber a manual mStopped (the two flags stay independent).
		MachineLevel tLevel = smeltingLevel();
		TileEntityOven tOven = makeOven(tLevel);
		tOven.getInventory().insertItem(TileEntityOven.SLOT_INPUT, new ItemStack(Items.SAND, 8), false);

		tLevel.mNeighborSignal = true;
		drive(tOven, 50);
		assertTrue(tOven.mRedstoneStopped, "the redstone latch engaged");
		assertFalse(tOven.mStopped, "the manual stop flag is untouched");
		assertEquals(0, tOven.mEnergy, "the gated machine receives no energy");
		assertEquals(0, tOven.mProgress, "CONSTANT_ENERGY reset through the redstone path");
		assertEquals(8, tOven.getInventory().getStackInSlot(TileEntityOven.SLOT_INPUT).getCount());

		tLevel.mNeighborSignal = false;
		drive(tOven, 1);
		assertEquals(256, tOven.mMaxProgress, "the machine resumes when the signal drops");
	}
}
