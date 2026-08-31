package gregtech6.tileentity.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.junit.jupiter.api.Test;

import gregtech6.recipes.GT6RecipeMaps;

/**
 * Acceptance 5 (task p7-basicmachine-family ⑦): the offline recipe chains — one per
 * machine, driven end-to-end through the BE dispatcher (findRecipe :712 → canOutput →
 * isRecipeInputEqual consume → getOutputs :758 → doActive progress :813 → :816 output
 * placement), over the representative rows poured by the test base.
 */
public class TileEntityBasicMachineRecipeTest extends TileEntityBasicMachineOfflineTestBase {

	/** Shredder :689 gravel → sand (the pure-vanilla row), full tick chain. */
	@Test
	void shredderChainGravelToSand() {
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.SHREDDER, 1, false);
		tMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.GRAVEL, 1), false);

		drive(tMachine, 4); // eUt 16 × duration 16 = 256 progress = 4 ticks of mInputMax
		ItemStack tOutput = tMachine.getInventory().getStackInSlot(1);
		assertEquals(Items.SAND, tOutput.getItem(), "the :689 output landed in output slot 0 (:816 wrap)");
		assertEquals(1, tOutput.getCount());
		assertTrue(tMachine.getInventory().getStackInSlot(TileEntityBasicMachine.SLOT_INPUT).isEmpty(), "the input was consumed");
	}

	/** Shredder :692 cobblestone → 9× dust(Stone) through the same chain. */
	@Test
	void shredderChainCobblestoneToNineDust() {
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.SHREDDER, 1, false);
		tMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.COBBLESTONE, 1), false);

		drive(tMachine, 4);
		ItemStack tOutput = tMachine.getInventory().getStackInSlot(1);
		assertEquals(sDustStoneItem, tOutput.getItem(), "the :692 nine-dust output (OM.dust(MT.Stone, U*9) shape)");
		assertEquals(9, tOutput.getCount());
	}

	/** Lathe :524 stone → stickLong(Stone), full tick chain with the synthetic output item. */
	@Test
	void latheChainStoneToStickLong() {
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.LATHE, 1, false);
		tMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.STONE, 1), false);

		drive(tMachine, 4); // :524 eUt 16 × duration 16 → 4 ticks
		ItemStack tOutput = tMachine.getInventory().getStackInSlot(1);
		assertEquals(sStickLongStoneItem, tOutput.getItem(), "the :524 lathe output (stickLong of Stone)");
		assertEquals(1, tOutput.getCount());
	}

	/** Crusher gem chain (:72 gem → gemFlawed x2) with the 4-parallel full tick chain. */
	@Test
	void crusherChainGemsToFlawedWithParallel() {
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.CRUSHER, 4, true);
		tMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(sGemItem, 4), false);

		// one parallel cycle: mMaxProgress = 16 × 16 × 4 = 1024 progress = 16 ticks of mInputMax
		drive(tMachine, 18);
		ItemStack tOutput = tMachine.getInventory().getStackInSlot(1);
		assertEquals(sGemFlawedItem, tOutput.getItem(), "the gem-chain output");
		assertEquals(8, tOutput.getCount(), "4 parallel × 2 gemFlawed per gem, one cycle");
		assertTrue(tMachine.getInventory().getStackInSlot(TileEntityBasicMachine.SLOT_INPUT).isEmpty(), "all four inputs eaten in one cycle");
	}

	/** A foreign item in an output slot blocks the recipe (canOutput :637-640) — the family-wide blockage semantics. */
	@Test
	void foreignOutputBlocksTheChain() {
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.SHREDDER, 1, false);
		tMachine.getInventory().insertItem(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.GRAVEL, 1), false);
		tMachine.getInventory().setStackInSlot(1, new ItemStack(Items.DIAMOND, 1));

		assertEquals(TileEntityBasicMachine.FOUND_RECIPE_BUT_DID_NOT_MEET_REQUIREMENTS, tMachine.checkRecipe(true, true),
				":637-640 — a different item in output slot 0 blocks");
		assertEquals(1, tMachine.getInventory().getStackInSlot(TileEntityBasicMachine.SLOT_INPUT).getCount(),
				"the blocked recipe is rejected before the consume (:736 early return)");
	}
}
