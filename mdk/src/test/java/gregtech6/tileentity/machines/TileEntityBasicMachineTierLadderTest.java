package gregtech6.tileentity.machines;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.junit.jupiter.api.Test;

import gregapi.data.TD;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.registry.GTMachines;

/**
 * The T2-T4 full-ladder acceptance (task p8-machine-tiers-doinject ①, the OFFLINE half):
 * the tier tables pinned to the upstream rows (Loader_MultiTileEntities.java:1294-1309
 * NBT_INPUT 32/128/512/2048 through the MultiTileEntityBasicMachine :126 conversion
 * min = in/2 / max = in*2; the Crusher NBT_PARALLEL 4/8/16/32 + NBT_PARALLEL_DURATION T
 * rows) and fixture machines built at every tier config — energy three values, the
 * parallel shape, the voltage-scaled recipe math and the shared slot shape. The registry
 * half (9 blocks + 9 items + the 3 family BETs with T1-T4 validBlocks multi-attach) only
 * resolves on a live server: covered by the runServer/RCON gates (the RO pattern, the D4
 * precedent — the registration itself never runs in offline tests).
 */
public class TileEntityBasicMachineTierLadderTest extends TileEntityBasicMachineOfflineTestBase {

	@Test
	void tierTablesMatchTheUpstreamRows() {
		// NBT_INPUT 32/128/512/2048 (:1294-1309) through the :126 conversion.
		assertArrayEquals(new long[] {16, 32, 64}, GTMachines.TIER_INPUTS[0], "T1 = the :98 field defaults (in = 32)");
		assertArrayEquals(new long[] {64, 128, 256}, GTMachines.TIER_INPUTS[1], "T2 = 128/2, 128, 128*2");
		assertArrayEquals(new long[] {256, 512, 1024}, GTMachines.TIER_INPUTS[2], "T3 = 512/2, 512, 512*2");
		assertArrayEquals(new long[] {1024, 2048, 4096}, GTMachines.TIER_INPUTS[3], "T4 = 2048/2, 2048, 2048*2");
		assertArrayEquals(new int[] {4, 8, 16, 32}, GTMachines.CRUSHER_PARALLEL, "the Crusher NBT_PARALLEL row (:1300-1303)");
	}

	/** A fixture built at a tier config — the same three-value assignment the BET factory body does. */
	private static TileEntityBasicMachine tierMachine(gregtech6.recipes.RecipeMap aMap, int aParallel, boolean aParallelDuration, gregapi.code.TagData aType, int aTier) {
		TileEntityBasicMachine tMachine = makeMachine(aMap, aParallel, aParallelDuration, aType);
		long[] tInputs = GTMachines.TIER_INPUTS[aTier];
		tMachine.mInputMin = tInputs[0];
		tMachine.mInput = tInputs[1];
		tMachine.mInputMax = tInputs[2];
		return tMachine;
	}

	@Test
	void everyTierCarriesItsOwnEnergyThreeValueShape() {
		// one machine per family x tier: the three values land, the carrier and the
		// data-driven slot shape stay the family constants (Shredder/Crusher 1+12,
		// Lathe 1+2 — the RecipeMap tail, TileEntityBasicMachine :98-102 javadoc).
		for (int tTier = 0; tTier < GTMachines.TIER_INPUTS.length; tTier++) {
			long[] tInputs = GTMachines.TIER_INPUTS[tTier];

			TileEntityBasicMachine tShredder = tierMachine(GT6RecipeMaps.SHREDDER, 1, false, TD.Energy.RU, tTier);
			assertEquals(tInputs[0], tShredder.mInputMin, "Shredder T" + (tTier + 1) + " InputMin");
			assertEquals(tInputs[1], tShredder.mInput, "Shredder T" + (tTier + 1) + " InputRec");
			assertEquals(tInputs[2], tShredder.mInputMax, "Shredder T" + (tTier + 1) + " InputMax");
			assertEquals(TD.Energy.RU, tShredder.mEnergyTypeAccepted, "Shredder carrier :1294");
			assertEquals(1, tShredder.mParallel, "Shredder registers no NBT_PARALLEL key");
			assertFalse(tShredder.mParallelDuration);
			assertEquals(13, tShredder.getInventory().getSlots(), "the 1+12 Shredder slot shape is tier-invariant");

			TileEntityBasicMachine tCrusher = tierMachine(GT6RecipeMaps.CRUSHER, GTMachines.CRUSHER_PARALLEL[tTier], true, TD.Energy.KU, tTier);
			assertEquals(tInputs[0], tCrusher.mInputMin, "Crusher T" + (tTier + 1) + " InputMin");
			assertEquals(GTMachines.CRUSHER_PARALLEL[tTier], tCrusher.mParallel, "the Crusher NBT_PARALLEL row");
			assertTrue(tCrusher.mParallelDuration, "the Crusher NBT_PARALLEL_DURATION T row");
			assertEquals(TD.Energy.KU, tCrusher.mEnergyTypeAccepted, "Crusher carrier :1300");
			assertEquals(13, tCrusher.getInventory().getSlots(), "the 1+12 Crusher slot shape is tier-invariant");

			TileEntityBasicMachine tLathe = tierMachine(GT6RecipeMaps.LATHE, 1, false, TD.Energy.RU, tTier);
			assertEquals(tInputs[2], tLathe.mInputMax, "Lathe T" + (tTier + 1) + " InputMax");
			assertEquals(TD.Energy.RU, tLathe.mEnergyTypeAccepted, "Lathe carrier :1306");
			assertEquals(3, tLathe.getInventory().getSlots(), "the 1+2 Lathe slot shape is tier-invariant");
		}
	}

	@Test
	void tierTwoCrusherRunsItsOwnVoltageThroughRecipeMathAndInjection() {
		// T2 crusher (64/128/256, parallel 8, duration T): the gem row 16/16 overclocks to
		// the 64 InputMin floor (one :773 step, x2 progress); 8 parallel → mMaxProgress =
		// 16*16*8*2 = 4096; the fake source refills 256/tick (:813 = min(256, mEnergy)).
		TileEntityBasicMachine tCrusher = tierMachine(GT6RecipeMaps.CRUSHER, GTMachines.CRUSHER_PARALLEL[1], true, TD.Energy.KU, 1);
		tCrusher.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(sGemItem, 8), false);

		drive(tCrusher, 1);
		assertEquals(64, tCrusher.mMinEnergy, "the overclock loop stopped at the T2 mInputMin floor");
		assertEquals(4096, tCrusher.mMaxProgress, "16 eUt * 16 duration * 8 parallel * 2 overclock");
		assertEquals(256, tCrusher.mProgress, "one active tick at the T2 voltage");
		assertEquals(0, tCrusher.getInventory().getStackInSlot(0).getCount(), "the apply + the :744 count loop consumed all 8 gems of the 8-parallel process");

		// the T2 clamp: 2 x 128 KU packets fill the 256 tank exactly (:503-504)
		assertEquals(2, tCrusher.doInject(TD.Energy.KU, (byte)2, 128, 2, true), "both T2-voltage packets consumed");
		assertEquals(256, tCrusher.mEnergy, "the T2 tank clamp");
		// the T2 overcharge threshold: 512 > 256 suspends tierMax(512) = 3
		assertEquals(1, tCrusher.doInject(TD.Energy.KU, (byte)2, 512, 1, true), "the overcharged packet reports fully used");
		assertEquals(3.0F, tCrusher.mExplosionStrength, 0.0F, "tierMax(512) = 3");
	}

	@Test
	void tierFourCrusherOverclocksToTheGigavoltFloor() {
		// T4 crusher (1024/2048/4096, parallel 32): the :773 loop 16→64→256→1024 (3 steps,
		// x8 progress); the fake source refills 4096/tick. mMaxProgress = 16*16*32 * 8 =
		// 8192 * 8 = 65536.
		TileEntityBasicMachine tCrusher = tierMachine(GT6RecipeMaps.CRUSHER, GTMachines.CRUSHER_PARALLEL[3], true, TD.Energy.KU, 3);
		tCrusher.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(sGemItem, 32), false);

		drive(tCrusher, 1);
		assertEquals(1024, tCrusher.mMinEnergy, "the overclock loop stopped at the T4 mInputMin floor");
		assertEquals(65536, tCrusher.mMaxProgress, "16 eUt * 16 duration * 32 parallel (= 8192) * 8 overclock");
		assertEquals(4096, tCrusher.mProgress, "one active tick at the T4 voltage");

		// the T4 clamp via the Root gate path (doEnergyInjection, the real network entry):
		// 2 x 2048 packets fill the 4096 tank
		assertEquals(2, tCrusher.doEnergyInjection(TD.Energy.KU, (byte)2, 2048, 2, true), "both T4-voltage packets through the Root gate");
		assertEquals(4096, tCrusher.mEnergy, "the T4 tank clamp");
	}

	@Test
	void tierThreeValuesRideThroughNbtLikeTheRestOfTheState() {
		// the three-value assignment is tick-independent config; the NBT round trip must
		// preserve mEnergy at a T3-scale tank (the values themselves are factory-injected
		// upstream too — NBT make(...) :1294-1309 — and the port keeps that shape).
		TileEntityBasicMachine tLathe = tierMachine(GT6RecipeMaps.LATHE, 1, false, TD.Energy.RU, 2);
		assertEquals(1, tLathe.doInject(TD.Energy.RU, (byte)2, 512, 1, true), "one T3-voltage packet");
		assertEquals(512, tLathe.mEnergy, "the T3 clamp");

		CompoundTag tTag = new CompoundTag();
		tLathe.saveAdditional(tTag);
		TileEntityBasicMachine tRestored = tierMachine(GT6RecipeMaps.LATHE, 1, false, TD.Energy.RU, 2);
		tRestored.load(tTag);
		assertEquals(256, tRestored.mInputMin, "the T3 floor re-derived from the fixture config");
		assertEquals(512, tRestored.mEnergy, "the T3-scale tank persisted");
		assertTrue(tRestored.mStateNew, "the :502 latch rode through the round trip");
	}
}
