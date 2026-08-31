package gregtech6.tileentity.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.junit.jupiter.api.Test;

import gregapi.data.TD;
import gregtech6.recipes.GT6RecipeMaps;

/**
 * Acceptance 2 (task p7-basicmachine-family ⑦): the option-A fake-power math — supplyEnergy
 * refills to mInputMax, doWork :791 drains exactly mInputMax, so every active tick advances
 * the progress by exactly mInputMax energy units (:813, the progress unit IS an energy unit);
 * the doInject seam (:489-508) is a stub returning 0; the manual stop gate (mStopped) cuts
 * the supply and parks the machine in doInactive.
 */
public class TileEntityBasicMachineEnergyTest extends TileEntityBasicMachineOfflineTestBase {

	@Test
	void everyActiveTickAdvancesExactlyMInputMax() {
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.SHREDDER, 1, false);
		tMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.GRAVEL, 1), false);

		tMachine.updateEntity(); // tick 1: supply (mEnergy = 64) → doWork → doActive(0, min(64, 64))
		assertEquals(64, tMachine.mProgress, ":813 — one tick of progress = one mInputMax");
		assertEquals(0, tMachine.mEnergy, ":791 — doWork drains the whole supply");
		assertTrue(tMachine.mActive, "the recipe was applied on the first tick");
		assertTrue(tMachine.mRunning, ":783");

		tMachine.updateEntity(); // tick 2: the refill seam feeds the next slice
		assertEquals(128, tMachine.mProgress);
	}

	@Test
	void gravelRowCompletesInFourTicksWithTheOutputPlaced() {
		// :689 gravel → sand, eUt 16, duration 16 → mMaxProgress = 16×16 = 256 (no overclock:
		// :773 needs mMinEnergy < mInputMin, 16 < 16 is false) → 256/64 = 4 ticks
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.SHREDDER, 1, false);
		tMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.GRAVEL, 1), false);
		assertEquals(0, tMachine.mMaxProgress, "sanity: the process has not started");

		drive(tMachine, 4);
		assertEquals(Items.SAND, tMachine.getInventory().getStackInSlot(1).getItem(), ":816 output wrap into output slot 0");
		assertEquals(0, tMachine.mProgress, ":843 carryover — the leftover-energy reset lands on 0 for one process");
		assertTrue(tMachine.mIgnited > 0, ":851 the post-action re-check window opened");
	}

	@Test
	void doInjectIsTheD3Stub() {
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.CRUSHER, 4, true, TD.Energy.KU);
		assertEquals(0, tMachine.doInject(TD.Energy.RU, (byte)2, 32, 10, true), "the ADR ④ seam returns 0 until D3 lands");
		assertEquals(0, tMachine.doInject(TD.Energy.KU, (byte)3, 64, 1, false), "both the simulated and the applied form are stubs");
		assertEquals(TD.Energy.KU, tMachine.mEnergyTypeAccepted, "the row carrier (:1300) rode through the fixture");
	}

	@Test
	void manualStopGatesTheSupplyAndParksTheMachine() {
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.SHREDDER, 1, false);
		tMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.GRAVEL, 1), false);
		assertFalse(tMachine.setStateOnOff(false), "upstream :1027 — stopping returns the new off state");
		assertTrue(tMachine.mStopped);

		tMachine.updateEntity();
		assertEquals(0, tMachine.mProgress, "supplyEnergy skips the stopped machine — doWork takes the inactive branch");
		assertFalse(tMachine.mRunning, ":787");
		assertFalse(tMachine.getStateOnOff(), ":1028");
	}
}
