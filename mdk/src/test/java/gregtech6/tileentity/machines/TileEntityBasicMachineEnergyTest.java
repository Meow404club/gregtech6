package gregtech6.tileentity.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.junit.jupiter.api.Test;

import gregapi.data.TD;
import gregtech6.recipes.GT6RecipeMaps;

/**
 * Acceptance 2 (task p7-basicmachine-family ⑦, regime-extended by task
 * p8-machine-tiers-doinject ②③④): the option-A fake-power math under the TRUE regime
 * (supplyEnergy refills to mInputMax, doWork :791 drains exactly mInputMax — every active
 * tick advances the progress by exactly mInputMax energy units :813) plus the FALSE-regime
 * group: the restored doInject :489-508 math, the KU transition dual-state (continuous
 * positive injection never delivers; the positive→non-positive transition tick does — the
 * upstream Steam Engine :146 ±alternation) and the TRUE-mode zero-regression pair.
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
	void doInjectRowCarrierRodeThroughTheFixture() {
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.CRUSHER, 4, true, TD.Energy.KU);
		assertEquals(TD.Energy.KU, tMachine.mEnergyTypeAccepted, "the row carrier (:1300) rode through the fixture");
	}

	// -------------------------------------------------------------------------
	// grid-fed regime group (task p8-machine-tiers-doinject ②④): ENERGY_FAKE_SOURCE =
	// false — the machine eats packets of its accepted type through the ITileEntityEnergy
	// surface (Root gate :717 + the restored doInject :489-508). doInject math first.
	// -------------------------------------------------------------------------

	/** The machine with the fake source OFF (the shipped-default regime flip). */
	private TileEntityBasicMachine netMachine(gregtech6.recipes.RecipeMap aMap, int aParallel, boolean aParallelDuration, gregapi.code.TagData aType) {
		TileEntityBasicMachine.ENERGY_FAKE_SOURCE = false;
		return makeMachine(aMap, aParallel, aParallelDuration, aType);
	}

	@Test
	void doInjectChargesUpToTheCapacityClamp() {
		// 32 RU x 2 packets into the T1 tank (16/32/64): tInput = min(64-0, 64) = 64,
		// tConsumed = min(2, 64/32) = 2, mEnergy += 2*32 = 64 (:503-504) — then a full tank
		// consumes nothing (tInput = min(64-64, 64) = 0).
		TileEntityBasicMachine tMachine = netMachine(GT6RecipeMaps.SHREDDER, 1, false, TD.Energy.RU);

		assertEquals(2, tMachine.doInject(TD.Energy.RU, (byte)2, 32, 2, true), "both packets consumed");
		assertEquals(64, tMachine.mEnergy, "the tank sits at the mInputMax clamp");
		assertEquals(0, tMachine.doInject(TD.Energy.RU, (byte)2, 32, 2, true), "a full tank consumes nothing");
		assertTrue(tMachine.mStateNew, ":502 latched the positive packet sign");
	}

	@Test
	void doInjectWrongTypeStoppedAndSimulationRefuse() {
		TileEntityBasicMachine tMachine = netMachine(GT6RecipeMaps.SHREDDER, 1, false, TD.Energy.RU);

		assertEquals(0, tMachine.doInject(TD.Energy.KU, (byte)2, 32, 2, true), "the :501 type-equality gate keeps the RU machine KU-blind");
		assertEquals(0, tMachine.mEnergy, "nothing booked");
		assertFalse(tMachine.mStateNew, ":502 never ran for a foreign type");

		assertEquals(2, tMachine.doInject(TD.Energy.RU, (byte)2, 32, 5, false), "the :503 probe reports what WOULD fit");
		assertEquals(0, tMachine.mEnergy, "the simulation booked nothing (:504 guard)");
		assertFalse(tMachine.mStateNew, "the :502 latch is behind the aDoInject guard too");

		tMachine.mStopped = true;
		assertEquals(0, tMachine.doInject(TD.Energy.RU, (byte)2, 32, 2, true), ":490 — a stopped machine refuses");
		assertEquals(0, tMachine.mEnergy);
	}

	@Test
	void doInjectOvervoltageSuspendsTheExplosion() {
		// :493-495 — aSize 128 > InputMax 64 overcharges and the WHOLE amount reports as
		// used; mEnergy untouched. The machine ticks (mIsTicking), so the Root D3 body
		// suspends into mExplosionStrength (UT6.tierMax(128) = 2) instead of destroying
		// offline; the real explosion is RCON/live-covered (the oven p8-d3 precedent).
		TileEntityBasicMachine tMachine = netMachine(GT6RecipeMaps.SHREDDER, 1, false, TD.Energy.RU);

		assertEquals(1, tMachine.doInject(TD.Energy.RU, (byte)2, 128, 1, true), "the overcharged packet reports fully used (:495)");
		assertEquals(0, tMachine.mEnergy, "no energy booked on the overcharge path");
		assertEquals(2.0F, tMachine.mExplosionStrength, 0.0F, "the suspended strength = UT6.tierMax(128) = 2");
		assertFalse(tMachine.isDead(), "the machine survives until its own tick consumes the flag");
	}

	@Test
	void netModeMachineStallsWithoutInjection() {
		// FALSE regime, no writer on doInject: the tank stays empty, doWork never reaches
		// the active branch, the input is untouched — the three families are NOT self-fed.
		TileEntityBasicMachine tMachine = netMachine(GT6RecipeMaps.SHREDDER, 1, false, TD.Energy.RU);
		tMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.GRAVEL, 2), false);

		drive(tMachine, 50);
		assertEquals(0, tMachine.mEnergy, "no fake source in the FALSE regime");
		assertEquals(0, tMachine.mProgress, "no injection — no progress");
		assertEquals(2, tMachine.getInventory().getStackInSlot(TileEntityBasicMachine.SLOT_INPUT).getCount(), "nothing consumed");
	}

	@Test
	void ruShredderDeliversOnEveryCompletedInjectedTick() {
		// RU is NOT an ALL_ALTERNATING member (root TD.java:216 = (F, KU)): the :815
		// non-alternating arm fires on the completing tick even though the packet sign
		// never flipped (mStateNew stays true through the whole positive train).
		TileEntityBasicMachine tMachine = netMachine(GT6RecipeMaps.SHREDDER, 1, false, TD.Energy.RU);
		tMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.GRAVEL, 1), false);

		// gravel → sand, 256 units at 64/injected tick = 4 inject+tick pairs
		for (int i = 0; i < 4; i++) {
			assertEquals(1, tMachine.doInject(TD.Energy.RU, (byte)2, 64, 1, true), "one 64 RU packet per tick");
			drive(tMachine, 1);
		}
		assertEquals(Items.SAND, tMachine.getInventory().getStackInSlot(1).getItem(), "the non-alternating machine delivered on the completing tick");
	}

	@Test
	void kuCrusherDeliversOnlyOnThePositiveToNonPositiveTransition() {
		// THE KU pulse semantics (task p8-machine-tiers-doinject ③): KU IS an
		// ALL_ALTERNATING member, so a continuous POSITIVE injection must NOT deliver even
		// after the progress completes (mStateOld && !mStateNew stays false — mStateNew
		// persists the last packet sign, the :865 shift is a no-reset form); the delivery
		// fires on the transition tick driven by a NON-POSITIVE packet — the AC half-cycle
		// of the upstream Steam Engine :146 piston-phase ±alternation.
		TileEntityBasicMachine tMachine = netMachine(GT6RecipeMaps.CRUSHER, 4, true, TD.Energy.KU);
		tMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(sGemItem, 4), false);

		// mMaxProgress = 16 eUt * 16 duration * 4 parallel = 1024; 64 per injected tick →
		// 16 pairs to complete (the first pair also arms the recipe via mInventoryChanged).
		for (int i = 0; i < 18; i++) {
			assertEquals(1, tMachine.doInject(TD.Energy.KU, (byte)2, 64, 1, true), "the positive half-cycle packet");
			drive(tMachine, 1);
			assertTrue(tMachine.mStateOld, "the latch shifted (:865)");
			assertTrue(tMachine.getInventory().getStackInSlot(1).isEmpty(), "continuous positive injection NEVER delivers (pair " + i + ")");
		}
		assertTrue(tMachine.mProgress >= tMachine.mMaxProgress, "the positive train ran the process past its completion point (pairs 17-18 overshoot)");

		// the negative half-cycle: :502 latches mStateNew = false, :503-504 still charge
		// |size| (the machine stores direction-agnostic KU) — the :815 transition arm opens.
		assertTrue(tMachine.mStateOld && tMachine.mStateNew, "sanity: both latch halves are true after the positive train");
		assertEquals(1, tMachine.doInject(TD.Energy.KU, (byte)2, -64, 1, true), "the negative packet is accepted with |size|");
		assertFalse(tMachine.mStateNew, ":502 latched the non-positive sign");
		drive(tMachine, 1);
		assertEquals(8, tMachine.getInventory().getStackInSlot(1).getCount(), "the transition tick delivered the 4-parallel output (2x gemFlawed each)");
	}

	@Test
	void fakeSourceRegimeStillDeliversKuImmediately() {
		// The TRUE-mode zero-regression assertion (task p8-machine-tiers-doinject ④): the
		// :815 alternating arm is suspended for the whole family, so the same KU crusher
		// under the A-tier fake source delivers on the completing tick like any other
		// machine (the pre-p8 behavior; the latch pair stays write-only-consulted-never).
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.CRUSHER, 4, true, TD.Energy.KU); // ENERGY_FAKE_SOURCE = true (the base per-test restore)
		tMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(sGemItem, 4), false);

		drive(tMachine, 16);
		assertTrue(tMachine.mProgress >= tMachine.mMaxProgress || tMachine.mMaxProgress == 0, "the fake source completes the process window");
		assertFalse(tMachine.getInventory().getStackInSlot(1).isEmpty(), "the suspended arm delivers on the completing tick (zero regression)");
	}

	@Test
	void stateNewPersistsWhileStateOldReDerives() {
		// NBT_STATE+".new" (:119/:233 — the port persists ONLY .new): the latch survives a
		// save/load, mStateOld comes back false and re-derives on the first active :865.
		TileEntityBasicMachine tMachine = netMachine(GT6RecipeMaps.SHREDDER, 1, false, TD.Energy.RU);
		assertEquals(1, tMachine.doInject(TD.Energy.RU, (byte)2, 64, 1, true), "one packet");
		assertTrue(tMachine.mStateNew);

		CompoundTag tTag = new CompoundTag();
		tMachine.saveAdditional(tTag);

		TileEntityBasicMachine tRestored = netMachine(GT6RecipeMaps.SHREDDER, 1, false, TD.Energy.RU);
		tRestored.load(tTag);
		assertTrue(tRestored.mStateNew, "the .new half persisted");
		assertFalse(tRestored.mStateOld, "the .old half is not persisted (:865 re-derives)");

		tRestored.doInject(TD.Energy.RU, (byte)2, 64, 1, true);
		drive(tRestored, 1);
		assertTrue(tRestored.mStateOld, "the first active tick shifted mStateNew into mStateOld");
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
