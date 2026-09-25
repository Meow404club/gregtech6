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

import gregapi.code.TagData;
import gregapi.data.TD;

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
				0, 1, "gt6:textures/gui/machines/oven",
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
				0, 1, "gt6:textures/gui/machines/oven",
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

	// -------------------------------------------------------------------------
	// grid-fed mode group (task p8-d3 §⑤, HU-rebased by task p34-oven-hu-conversion):
	// ENERGY_FAKE_SOURCE = false — the machine eats HU packets through the
	// ITileEntityEnergy surface (Root gate :717 + oven doInject :489-508; the oven is
	// the upstream 20001-04 shape, NBT_ENERGY_ACCEPTED = TD.Energy.HU,
	// Loader_MultiTileEntities.java:1288-1291). No drive() here: the fake source is
	// off, the energy stays exactly what the injections booked.
	// -------------------------------------------------------------------------

	/** The oven with the fake source OFF (the shipped default semantic). */
	private TileEntityOven netModeOven() {
		TileEntityOven.ENERGY_FAKE_SOURCE = false;
		return makeOven(smeltingLevel());
	}

	@Test
	void netModeInjectChargesUpToTheCapacityClamp() {
		// 32 HU x 2 packets: tInput = min(64-0, 32*2) = 64, tConsumed = min(2, 64/32) = 2,
		// mEnergy += 2*32 = 64 (:501-505) — the tank sits exactly at mInputMax.
		TileEntityOven tOven = netModeOven();

		long tUsed = tOven.doEnergyInjection(TD.Energy.HU, (byte)2, 32, 2, true);
		assertEquals(2, tUsed, "both packets consumed");
		assertEquals(64, tOven.mEnergy, "mEnergy = 2 packets * 32 HU, at the mInputMax clamp");

		tUsed = tOven.doEnergyInjection(TD.Energy.HU, (byte)2, 32, 2, true);
		assertEquals(0, tUsed, "a full tank consumes nothing (tInput = min(64-64, 64) = 0)");
		assertEquals(64, tOven.mEnergy, "the clamp holds");
	}

	@Test
	void netModeExcessPacketsAreClampedToTheFreeTankSpace() {
		// 64 HU x 2 packets into the empty tank: tInput = min(64, 128) = 64,
		// tConsumed = min(2, 64/64) = 1 — only ONE packet is consumed, the second refused.
		TileEntityOven tOven = netModeOven();

		long tUsed = tOven.doEnergyInjection(TD.Energy.HU, (byte)2, 64, 2, true);
		assertEquals(1, tUsed, "only one of the two 64 HU packets fits");
		assertEquals(64, tOven.mEnergy, "the tank is full");
	}

	@Test
	void netModeSimulationDoesNotBookEnergy() {
		// aDoInject = false runs the same math without mutating (:503/:504 guards):
		// tInput = min(64-0, 32*5) = 64, tConsumed = min(5, 64/32) = 2 — the probe reports
		// the two packets that WOULD fit.
		TileEntityOven tOven = netModeOven();

		long tUsed = tOven.doEnergyInjection(TD.Energy.HU, (byte)2, 32, 5, false);
		assertEquals(2, tUsed, "the probe reports what WOULD be consumed");
		assertEquals(0, tOven.mEnergy, "nothing booked");

		tUsed = tOven.doEnergyInjection(TD.Energy.HU, (byte)2, 32, 5, true);
		assertEquals(2, tUsed, "the real injection consumes the same two packets");
		assertEquals(64, tOven.mEnergy, "the tank is at the clamp");
	}

	@Test
	void netModeStoppedMachineRefusesInjection() {
		// :490 — a stopped machine returns 0 before anything else; the Root gate
		// (isEnergyAcceptingFrom without the theoretical flag) bounces first for the
		// doEnergyInjection path, the direct doInject bounce is the :490 line itself.
		TileEntityOven tOven = netModeOven();
		tOven.mStopped = true;

		assertEquals(0, tOven.doEnergyInjection(TD.Energy.HU, (byte)2, 32, 2, true), "the gate refuses (not accepting)");
		assertEquals(0, tOven.doInject(TD.Energy.HU, (byte)2, 32, 2, true), ":490 refuses directly");
		assertEquals(0, tOven.mEnergy, "nothing booked");
		assertFalse(tOven.isEnergyAcceptingFrom(TD.Energy.HU, (byte)2, false), "a stopped machine is not accepting");
		assertTrue(tOven.isEnergyAcceptingFrom(TD.Energy.HU, (byte)2, true), "aTheoretical keeps conductors visually connected (:511)");
	}

	@Test
	void netModeSmallPacketsBookBecauseHUIsSizeIrrelevant() {
		// Root gate :717: HU sits in TD.Energy.ALL_SIZE_IRRELEVANT (TD.java:218), so the
		// below-minimum swallow arm (|8| < InputMin 16) does NOT apply — every packet
		// reaches doInject: tInput = min(64-0, 8*5) = 40, tConsumed = min(5, 40/8) = 5.
		// (The old EU pin asserted the swallow; the type flip moves the oven to the
		// size-irrelevant arm — the upstream 20001-04 semantics.)
		TileEntityOven tOven = netModeOven();

		long tUsed = tOven.doEnergyInjection(TD.Energy.HU, (byte)2, 8, 5, true);
		assertEquals(5, tUsed, "all five small packets consumed");
		assertEquals(40, tOven.mEnergy, "mEnergy = 5 packets * 8 HU booked");
	}

	@Test
	void netModeOvervoltageOverchargesAndSuspendsTheExplosion() {
		// :493-495 — aSize 128 > InputMax 64 overcharges and the WHOLE amount reports as
		// used; mEnergy stays untouched (HU sits in ALL_EXPLODING, so the tierMax curve
		// applies). The oven ticks (mIsTicking), so the explosion is
		// NOT instant from the wire's tick context: it suspends into mExplosionStrength
		// with the tierMax curve (UT6.tierMax(128) = 2) and the machine stays alive until
		// its own tick consumes the flag. The instant/tick-consumption split is covered
		// offline in TileEntityBase01RootEnergyTest (the stub Level cannot take
		// destroyBlock) and live by the RCON overcharge chain (the block really explodes).
		TileEntityOven tOven = netModeOven();
		tOven.getInventory().insertItem(TileEntityOven.SLOT_INPUT, new ItemStack(Items.SAND, 8), false);

		long tUsed = tOven.doEnergyInjection(TD.Energy.HU, (byte)2, 128, 1, true);
		assertEquals(1, tUsed, "the overcharged packet reports as fully used (:495)");
		assertEquals(0, tOven.mEnergy, "no energy booked on the overcharge path");
		assertEquals(2.0F, tOven.mExplosionStrength, 0.0F, "the suspended strength = UT6.tierMax(128) = 2");
		assertFalse(tOven.isDead(), "the machine survives until its own tick consumes the flag");

		// a second over-voltage burst keeps the MAX of the demands (:482).
		tOven.doEnergyInjection(TD.Energy.HU, (byte)2, 512, 1, true);
		assertEquals(3.0F, tOven.mExplosionStrength, 0.0F, "tierMax(512) = 3 wins the max");
	}

	@Test
	void netModeEnergyTypeSurfaceIsHuOnly() {
		// :510/:519 — accepting HU only (the 20001-04 NBT_ENERGY_ACCEPTED, Loader:1288-1291),
		// emitting nothing, sizes 16/32/64 (:513-515 + :133); EU is refused at the gate.
		TileEntityOven tOven = netModeOven();

		assertTrue(tOven.isEnergyType(TD.Energy.HU, (byte)2, false), "accepts HU");
		assertFalse(tOven.isEnergyType(TD.Energy.HU, (byte)2, true), "emits nothing");
		assertFalse(tOven.isEnergyType(TD.Energy.EU, (byte)2, false), "EU is not the accepted type");
		assertEquals(1, tOven.getEnergyTypes((byte)6).size(), "HU.AS_LIST (:519)");
		assertEquals(TD.Energy.HU, tOven.getEnergyTypes((byte)6).iterator().next(), "the single type is HU");
		assertEquals(16, tOven.getEnergySizeInputMin(TD.Energy.HU, (byte)2), "InputMin = mInputMin (:513)");
		assertEquals(32, tOven.getEnergySizeInputRecommended(TD.Energy.HU, (byte)2), "InputRec = mInput (:514)");
		assertEquals(64, tOven.getEnergySizeInputMax(TD.Energy.HU, (byte)2), "InputMax = mInputMax (:515)");
		assertFalse(tOven.isEnergyEmittingTo(TD.Energy.HU, (byte)2, false), "the oven never emits");
		assertTrue(tOven.isSurfaceEnergyAttachable((byte)2), "the Root attachment seam defaults to true (all-sides mask collapsed, :511 seam)");

		// the EU rejection at the Root gate: not accepting -> 0, nothing booked.
		assertEquals(0, tOven.doEnergyInjection(TD.Energy.EU, (byte)2, 32, 2, true), "the gate refuses the EU packet");
		assertEquals(0, tOven.mEnergy, "the refused EU packet booked nothing");
	}
}
