package gregtech6.tileentity.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.junit.jupiter.api.Test;

import gregapi.data.TD;
import gregtech6.recipes.GT6RecipeMaps;

/**
 * The p29-w2-energy-types-5tier ③ TU supply dynamics (the OFFLINE half, the W1
 * retained-vs-reset 数值自洽 method — assertions describe BEHAVIOR, never a type
 * attribution):
 *
 * <ul>
 * <li>NO_CONSTANT_POWER (the :123 NBT_NO_CONSTANT_POWER = T bind) RETAINS progress
 *     through a power gap; a constant-power machine RESETS it. The gate is the upstream
 *     :894 line {@code if (CONSTANT_ENERGY && !mNoConstantEnergy) mProgress = 0;} —
 *     verbatim in the port doInactive, and W1 established the gate carries NO energy-type
 *     term, so the reset face is pinned on BOTH a TU-carrier and an EU-carrier machine
 *     (behavior, not attribution).</li>
 * <li>The TU parallel arm of :770: {@code mEnergyTypeAccepted == TD.Energy.TU ?
 *     tRecipe.mEUt : tRecipe.mEUt * tMaxProcessCount} — a TU machine with N machines'
 *     worth of inputs still binds mMinEnergy = mEUt (the parallel loop multiplies the
 *     OUTPUTS, not the energy), with the EU/RU carrier as the ×N contrast.</li>
 * </ul>
 *
 * <p>The live (RCON) half of the resume face rides the consumer cards' chains (no W2
 * machine exists on main yet) — this class is the binding offline form.
 */
public class GT6EnergyDynamicsTest extends TileEntityBasicMachineOfflineTestBase {

	/** Starts the gravel→sand chain (eUt 16, duration 16 → the 256 bar) and runs {@code aTicks} at 32 EU/tick. */
	private static long runTicks(TileEntityBasicMachine aMachine, int aTicks) {
		aMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.GRAVEL, 1), false);
		assertEquals(TileEntityBasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, aMachine.checkRecipe(true, true));
		for (int i = 0; i < aTicks; i++) {
			aMachine.doInject(aMachine.mEnergyTypeAccepted, (byte)0, 32, 1, true);
			aMachine.updateEntity();
		}
		return aMachine.mProgress;
	}

	/** Drives the dispatcher WITHOUT injection until {@code doInactive} fires (aTimer > 40 gate, the doWork :459 form). */
	private static void powerGap(TileEntityBasicMachine aMachine, int aTicks) {
		for (int i = 0; i < aTicks; i++) aMachine.updateEntity();
	}

	/** The NO_CONSTANT_POWER machine retains its exact mid-flight progress across the gap, and the retained tail shortens the resume. */
	@Test
	void noConstantPowerRetainsProgressAcrossAPowerGap() {
		TileEntityBasicMachine.ENERGY_FAKE_SOURCE = false; // the grid-fed regime — the gap starves the machine
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.SHREDDER, 1, false);
		tMachine.mNoConstantEnergy = true; // the :123 NBT_NO_CONSTANT_POWER = T bind form (the Loader:1651-1655 TU four / the Coke Oven :1193 judged row)

		long tProgress = runTicks(tMachine, 2);
		assertTrue(tProgress > 0, "the process is mid-flight (32 EU × 2 ticks into the 256 bar)");

		powerGap(tMachine, 60);
		assertEquals(tProgress, tMachine.mProgress,
				"the :894 gate skipped — mNoConstantEnergy keeps the parked progress EXACTLY");

		// the resume: the retained tail means the process completes in FEWER ticks than a
		// fresh run (the 数值自洽 half — same rig, same packets)
		int tResumeTicks = 0;
		while (tMachine.mMaxProgress != 0 && tResumeTicks < 64) {
			tMachine.doInject(tMachine.mEnergyTypeAccepted, (byte)0, 32, 1, true);
			tMachine.updateEntity();
			tResumeTicks++;
		}
		assertEquals(0L, tMachine.mMaxProgress, "the process completed after the gap");
		assertTrue(tResumeTicks < 8, "the resume needed " + tResumeTicks
				+ " ticks < the 8-tick fresh bar — the retained progress WAS the work");
		assertTrue(tMachine.getInventory().getStackInSlot(tMachine.getInputSlotCount()).getCount() >= 1,
				"the output landed");
	}

	/** The constant-power twin resets to 0 in the SAME gap — the retained-vs-reset pair on one rig shape. */
	@Test
	void constantPowerMachineIdleResetsProgress() {
		TileEntityBasicMachine.ENERGY_FAKE_SOURCE = false;
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.SHREDDER, 1, false); // mNoConstantEnergy default F

		long tProgress = runTicks(tMachine, 2);
		assertTrue(tProgress > 0, "the process is mid-flight");

		powerGap(tMachine, 60);
		assertEquals(0L, tMachine.mProgress,
				"the :894 gate fired — CONSTANT_ENERGY && !mNoConstantEnergy resets the parked progress");

		// the restart: the full 8-tick bar again — the reset LOST the work (the pair's reset face)
		int tRestartTicks = 0;
		while (tMachine.mMaxProgress != 0 && tRestartTicks < 64) {
			tMachine.doInject(tMachine.mEnergyTypeAccepted, (byte)0, 32, 1, true);
			tMachine.updateEntity();
			tRestartTicks++;
		}
		assertEquals(0L, tMachine.mMaxProgress, "the process completed from zero");
		assertEquals(8, tRestartTicks, "exactly the full fresh bar — nothing was retained");
	}

	/**
	 * The :894 gate is type-blind (W1's verified reading — the line has no energy-type
	 * term): a TU-carrier and an EU-carrier machine at the DEFAULT mNoConstantEnergy both
	 * reset in the same gap. Behavior only — the carrier type does NOT save the progress.
	 */
	@Test
	void theIdleResetGateIsTypeBlind() {
		TileEntityBasicMachine.ENERGY_FAKE_SOURCE = false;
		TileEntityBasicMachine tTu = makeMachine(GT6RecipeMaps.SHREDDER, 1, false, TD.Energy.TU);
		TileEntityBasicMachine tEu = makeMachine(GT6RecipeMaps.SHREDDER, 1, false, TD.Energy.EU);
		assertSame(TD.Energy.TU, tTu.mEnergyTypeAccepted);
		assertSame(TD.Energy.EU, tEu.mEnergyTypeAccepted);

		assertTrue(runTicks(tTu, 2) > 0, "the TU carrier is mid-flight");
		assertTrue(runTicks(tEu, 2) > 0, "the EU carrier is mid-flight");

		powerGap(tTu, 60);
		powerGap(tEu, 60);
		assertEquals(0L, tTu.mProgress, "the TU carrier resets (the gate has no type term)");
		assertEquals(0L, tEu.mProgress, "the EU carrier resets identically");
	}

	/** The :770 TU arm: N inputs bind mMinEnergy = mEUt — the parallel count multiplies the OUTPUTS, not the energy. */
	@Test
	void tuParallelMultipliesOutputsNotTheEnergy() {
		// parallel 4, non-parallelDuration (the :770 arm), 4 inputs in stock; the window
		// carries the TU-four EXPLICIT-override form (min 1 / in 128 / max 256 — the min
		// decoupled from the :126 relation exactly like the Loader:1651-1655 rows do) so
		// the :743 per-tick energy budget (mInput/mEUt = 8) never caps the 4-process count
		// AND the :773 fold stays dead (mMinEnergy < 1 is impossible)
		TileEntityBasicMachine tTu = makeMachine(GT6RecipeMaps.SHREDDER, 4, false, TD.Energy.TU);
		tTu.mInputMin = 1;
		tTu.mInput = 128;
		tTu.mInputMax = 256;
		tTu.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.GRAVEL, 4), false);
		assertEquals(TileEntityBasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tTu.checkRecipe(true, true));
		assertEquals(16L, tTu.mMinEnergy, "mMinEnergy = mEUt 16 — the TU arm does NOT multiply by the 4-process count");
		assertEquals(256L, tTu.mMaxProgress, "16 EUt × 16 duration — the raw single-process bar");

		// the RU contrast: the same rig binds mEUt × 4 (the :770 non-TU branch)
		TileEntityBasicMachine tRu = makeMachine(GT6RecipeMaps.SHREDDER, 4, false, TD.Energy.RU);
		tRu.mInputMin = 1;
		tRu.mInput = 128;
		tRu.mInputMax = 256;
		tRu.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.GRAVEL, 4), false);
		assertEquals(TileEntityBasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tRu.checkRecipe(true, true));
		assertEquals(64L, tRu.mMinEnergy, "mMinEnergy = 16 × 4 — the non-TU carriers multiply (the :770 else branch)");
		// and neither rung overclocks: mMinEnergy < mInputMin 1 is impossible — the :773 loop is inert
	}
}
