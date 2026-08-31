package gregtech6.tileentity.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.item.ItemStack;

import org.junit.jupiter.api.Test;

import gregtech6.recipes.GT6RecipeMaps;

/**
 * Acceptance 3 (task p7-basicmachine-family ⑦): the Crusher parallel math — the restored
 * checkRecipe parallel blocks (:742-745) plus the two energy-math branches (:766-768 the
 * parallelDuration linear-duration form; :770 the non-parallelDuration energy-speedup form)
 * and the overclock loop :773, over the poured gem-chain row (:72, Loader_Recipes_Handlers).
 */
public class TileEntityBasicMachineParallelTest extends TileEntityBasicMachineOfflineTestBase {

	/** A 4-parallel Crusher eats four inputs in ONE process (NBT_PARALLEL 4 + NBT_PARALLEL_DURATION T, :1300). */
	@Test
	void crusherEatsFourInputsInOneCycle() {
		TileEntityBasicMachine tCrusher = makeMachine(GT6RecipeMaps.CRUSHER, 4, true);
		assertEquals(4, tCrusher.mParallel, ":1300 NBT_PARALLEL 4");
		tCrusher.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(sGemItem, 6), false);

		int tResult = tCrusher.checkRecipe(true, true);
		assertEquals(TileEntityBasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tResult, ":777");
		assertEquals(2, tCrusher.getInventory().getStackInSlot(TileEntityBasicMachine.SLOT_INPUT).getCount(),
				":742-745 — four of the six inputs consumed in one check (6 - 4)");
		assertEquals(sGemFlawedItem, tCrusher.mOutputItems[0].getItem(), ":758 getOutputs(count)");
		assertEquals(8, tCrusher.mOutputItems[0].getCount(), ":72 — 4 parallel × 2 gemFlawed per gem");

		// :766-768 — mMinEnergy = max(1, mEUt) stays flat, mMaxProgress = minEnergy × duration × 4
		assertEquals(16, tCrusher.mMinEnergy, ":767 — the per-tick energy draw does NOT scale (mEUt 16, no overclock: 16 < 16 is false)");
		assertEquals(16 * 16 * 4, tCrusher.mMaxProgress, ":768 — the duration scales linearly with the parallel count");
	}

	/** The non-parallelDuration branch: the ENERGY scales (the speedup), the duration stays. */
	@Test
	void nonParallelDurationScalesTheEnergyInstead() {
		// a 2-parallel Shredder-shaped machine (RU): :743 bind(1, 2, mInput/mEUt = 32/16 = 2) keeps 2;
		// the :689 gravel → sand row (1 in, 1 out) is the consumed row
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.SHREDDER, 2, false);
		tMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(net.minecraft.world.item.Items.GRAVEL, 5), false);

		assertEquals(TileEntityBasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tMachine.checkRecipe(true, true), ":777");
		assertEquals(3, tMachine.getInventory().getStackInSlot(TileEntityBasicMachine.SLOT_INPUT).getCount(),
				":744 — two consumed (5 - 2)");
		assertEquals(2, tMachine.mOutputItems[0].getCount(), "the parallel multiplier on the outputs");

		// :770-771 — mMinEnergy = mEUt × 2 = 32; mMaxProgress = minEnergy × duration (NO ×2)
		assertEquals(16 * 2, tMachine.mMinEnergy, ":770 — the energy scales with the parallel count");
		assertEquals(16 * 2 * 16, tMachine.mMaxProgress, ":771 — the duration does not (the speedup form)");
	}

	/** The :743 energy-budget cap: a parallel count larger than mInput / mEUt is bound down. */
	@Test
	void energyBudgetCapsTheParallelCount() {
		// mParallel 8 but mInput 32 / mEUt 16 = 2 affordable parallel processes → bound to 2 (:743)
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.SHREDDER, 8, false);
		tMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(net.minecraft.world.item.Items.GRAVEL, 8), false);

		assertEquals(TileEntityBasicMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tMachine.checkRecipe(true, true), ":777");
		assertEquals(6, tMachine.getInventory().getStackInSlot(TileEntityBasicMachine.SLOT_INPUT).getCount(),
				"the budget bind(1, 8, 2) = 2 caps the consumption at two");
		assertEquals(16 * 2, tMachine.mMinEnergy, ":770 runs with the bound count");
	}

	/** The probe path never consumes and stops before the parallel rebalance (:740 early return). */
	@Test
	void probeStopsBeforeTheParallelConsume() {
		TileEntityBasicMachine tCrusher = makeMachine(GT6RecipeMaps.CRUSHER, 4, true);
		tCrusher.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(sGemItem, 4), false);

		assertEquals(TileEntityBasicMachine.FOUND_AND_COULD_HAVE_USED_RECIPE, tCrusher.checkRecipe(false, true), ":740");
		assertTrue(tCrusher.mCouldUseRecipe, ":739");
		assertEquals(4, tCrusher.getInventory().getStackInSlot(TileEntityBasicMachine.SLOT_INPUT).getCount(), "the probe never consumes");
		assertEquals(0, tCrusher.mMaxProgress, "the probe stops before the energy math");
	}
}
