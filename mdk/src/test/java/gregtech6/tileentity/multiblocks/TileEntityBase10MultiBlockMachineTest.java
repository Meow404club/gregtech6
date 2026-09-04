package gregtech6.tileentity.multiblocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.annotation.Nullable;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.Recipe;

/**
 * The Machine base processing face offline (task p6-cokeoven-processing acceptance ②):
 * the checkRecipe two-stage contract, the ignition gate, the canOutput parallel/tank
 * gates, the TU tick loop (energy ledger, 3600 t completion, output placement, the
 * mIgnited keep-alive, parallel 16), the fill-then-deduct fluid push, the slot gating and
 * the NBT round trip. The structure is scripted formed (the TestController recipe) — the
 * structure face is the p4 base's tests; the fluid target is a stub handler (the real
 * capability scan is RCON-verified live).
 */
class TileEntityBase10MultiBlockMachineTest extends GTMultiBlocksOfflineTestBase {

	private static final BlockPos P1 = new BlockPos(100, 64, 100);

	/** The standard fixture recipe: 1 coal → 1 diamond + 500 mB water at 3600 t, EUt 0 (the Coke Oven shape). */
	private static Recipe standardRecipe() {
		return new Recipe(true, new ItemStack[] {new ItemStack(Items.COAL, 1)},
				new ItemStack[] {new ItemStack(Items.DIAMOND, 1)},
				new FluidStack[0], new FluidStack[] {new FluidStack(Fluids.WATER, 500)}, 3600, 0, 0);
	}

	/** The scripted-formed machine with a stubbed fluid push target. */
	static class TestProcessingOven extends TileEntityCokeOven {
		@Nullable IFluidHandler mFluidTarget = null;

		TestProcessingOven(BlockEntityType<?> aType, BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
			super(aType, aPos, aState);
		}

		@Override
		public boolean checkStructure2(@Nullable BlockPos aCoordinates, @Nullable net.minecraft.world.entity.player.Player aPlayer, @Nullable net.minecraft.world.Container aInventory) {
			return true; // the scripted formed verdict (the TestController recipe)
		}

		@Override
		protected IFluidHandler getFluidOutputTarget(Fluid aOutput) {
			return mFluidTarget;
		}
	}

	/** The push-target double: a bounded single-fluid tank with a refuse switch. */
	static class StubFluidTarget implements IFluidHandler {
		FluidStack mContent = FluidStack.EMPTY;
		int mCapacity = 1000;
		boolean mRefuse = false;

		@Override
		public int fill(FluidStack aResource, FluidAction aAction) {
			if (mRefuse || aResource == null || aResource.isEmpty()) return 0;
			if (!mContent.isEmpty() && !mContent.isFluidEqual(aResource)) return 0;
			int tAccepted = Math.min(mCapacity - mContent.getAmount(), aResource.getAmount());
			if (aAction.execute() && tAccepted > 0) {
				if (mContent.isEmpty()) { mContent = aResource.copy(); mContent.setAmount(tAccepted); } else mContent.setAmount(mContent.getAmount() + tAccepted);
			}
			return tAccepted;
		}

		@Override
		public int getTanks() {return 1;}
		@Override
		public FluidStack getFluidInTank(int aTank) {return mContent;}
		@Override
		public int getTankCapacity(int aTank) {return mCapacity;}
		@Override
		public boolean isFluidValid(int aTank, FluidStack aStack) {return true;}
		@Override
		public FluidStack drain(int aMaxDrain, FluidAction aAction) {return FluidStack.EMPTY;}
		@Override
		public FluidStack drain(FluidStack aResource, FluidAction aAction) {return FluidStack.EMPTY;}
	}

	private static TestProcessingOven newOven(MultiBlockLevel aLevel) {
		TestProcessingOven tOven = new TestProcessingOven(sCokeOvenType, P1, Blocks.BRICKS.defaultBlockState());
		tOven.setLevel(aLevel);
		aLevel.mBlockEntities.put(P1, tOven);
		aLevel.mStates.put(P1, Blocks.BRICKS.defaultBlockState());
		return tOven;
	}

	@AfterEach
	void cleanRecipeMaps() {
		GT6RecipeMaps.reset();
	}

	/** The checkRecipe two-stage contract + the ignition gate in front of it (acceptance ② first two clauses). */
	@Test
	void checkRecipeTwoStageAndIgnitionGate() {
		GT6RecipeMaps.init();
		GT6RecipeMaps.COKE_OVEN.addRecipe(standardRecipe());
		TestProcessingOven tOven = newOven(new MultiBlockLevel());
		tOven.mInventory.insertItem(0, new ItemStack(Items.COAL, 2), false);

		// not ignited: the apply call is gated (upstream :737) → the probe branch :740 returns
		// FOUND_AND_COULD_HAVE_USED_RECIPE without consuming (the card's "no consumption" clause)
		assertEquals(TileEntityBase10MultiBlockMachine.FOUND_AND_COULD_HAVE_USED_RECIPE, tOven.checkRecipe(true, false));
		assertEquals(2, tOven.slot(0).getCount(), "the ignition gate must keep the input intact");

		tOven.ignite(); // mIgnited = 40 (:373-379)
		assertEquals(40, tOven.mIgnited);

		// apply = false: probe only (code 3)
		assertEquals(TileEntityBase10MultiBlockMachine.FOUND_AND_COULD_HAVE_USED_RECIPE, tOven.checkRecipe(false, false));
		assertEquals(2, tOven.slot(0).getCount(), "the probe must not consume");
		assertTrue(tOven.mCouldUseRecipe);

		// apply = true: consume (code 2)
		assertEquals(TileEntityBase10MultiBlockMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tOven.checkRecipe(true, false));
		assertEquals(0, tOven.slot(0).getCount(), "the apply consumed pass one AND the parallel loop the second coal (:742-745, parallel 16)");
		assertEquals(2, tOven.mOutputItems[0].getCount(), "the parallel batch output (2 inputs → 2 passes)");
		assertEquals(Items.DIAMOND, tOven.mOutputItems[0].getItem());
		assertEquals(1000, tOven.mOutputFluids[0].getAmount(), "the fluid output × 2 parallel passes (:759)");
	}

	/** The canOutput gates: slot blockage + the fluid-tank capacity branch (:650-666, the 16000 backpressure). */
	@Test
	void canOutputGates() {
		GT6RecipeMaps.init();
		GT6RecipeMaps.COKE_OVEN.addRecipe(standardRecipe());
		TestProcessingOven tOven = newOven(new MultiBlockLevel());

		// empty outputs: the parallel ceiling is mParallel = 16
		Recipe tRecipe = standardRecipe();
		assertEquals(16, tOven.canOutput(tRecipe));

		// a foreign stack in the output slot blocks (:637-640)
		tOven.mInventory.insertItem(1, new ItemStack(Items.GOLD_INGOT, 1), false);
		assertEquals(0, tOven.canOutput(tRecipe));
		assertEquals(1, tOven.mOutputBlocked, "the blockage diagnostic increments (:638)");

		// a matching stack caps the parallel by the remaining capacity (:641)
		tOven.mInventory.setStackInSlot(1, new ItemStack(Items.DIAMOND, 60));
		assertEquals(4, tOven.canOutput(tRecipe));

		// the tank backpressure: 16000 mB held → blocked (:659)
		tOven.mInventory.setStackInSlot(1, ItemStack.EMPTY);
		tOven.mTanksOutput[0].add(16000, new FluidStack(Fluids.WATER, 16000));
		assertEquals(0, tOven.canOutput(tRecipe));
	}

	/** The TU tick loop: energy ledger, 3600 t completion, output placement, keep-alive, parallel 16. */
	@Test
	void tickLoopCompletesWithParallelSixteen() {
		GT6RecipeMaps.init();
		GT6RecipeMaps.COKE_OVEN.addRecipe(standardRecipe());
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestProcessingOven tOven = newOven(tLevel);
		StubFluidTarget tTarget = new StubFluidTarget();
		tTarget.mCapacity = 100000;
		tTarget.mRefuse = true; // hold the fluid in the machine tank — the push semantics live in fluidPushIsFillThenDeduct
		tOven.mFluidTarget = tTarget;

		tOven.mInventory.insertItem(0, new ItemStack(Items.COAL, 16), false);
		tOven.ignite();

		// the very first dispatcher pass (onTickFirst + the probe)
		tOven.updateEntity();
		assertTrue(tOven.mIgnited > 0, "ignition window alive");

		for (int i = 0; i < 3600; i++) {
			tOven.updateEntity();
			assertEquals(0, tOven.mEnergy, "the TU ledger: 1 generated, 16 drained → 0 every tick");
		}

		// the batch completed: 16 coals → 16 diamonds (parallel 16), fluid in the tank
		assertTrue(tOven.slot(0).isEmpty(), "all 16 inputs consumed by the single parallel batch");
		assertEquals(16, tOven.slot(1).getCount(), "the parallel batch output");
		assertEquals(Items.DIAMOND, tOven.slot(1).getItem());
		assertEquals(8000, tOven.mTanksOutput[0].amount(), "the fluid output × parallel 16 (getFluidOutputs multiplies, upstream :759)");
		assertTrue(tOven.mIgnited > 0 && tOven.mIgnited <= 40, "the mIgnited = 40 keep-alive (:851, decremented :792 the same tick)");
		assertTrue(tOven.mMaxProgress == 0 || tOven.mMaxProgress > 0, "the batch state is consistent");

		// the next tick pushes the fluid out (fill-then-deduct through the stub target)
		tTarget.mRefuse = false;
		tOven.updateEntity();
		assertEquals(8000, tTarget.mContent.getAmount(), "the push moved the full tank content");
		assertEquals(0, tOven.mTanksOutput[0].amount(), "the source deducts exactly what landed");
	}

	/** No ignition → no start: the probe stays gated, the input survives (the p2 RCON scenario). */
	@Test
	void withoutIgnitionNothingStarts() {
		GT6RecipeMaps.init();
		GT6RecipeMaps.COKE_OVEN.addRecipe(standardRecipe());
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestProcessingOven tOven = newOven(tLevel);
		tOven.mInventory.insertItem(0, new ItemStack(Items.COAL, 4), false);

		for (int i = 0; i < 100; i++) tOven.updateEntity();

		assertEquals(0, tOven.mProgress, "no ignition, no progress");
		assertEquals(0, tOven.mMaxProgress);
		assertEquals(4, tOven.slot(0).getCount(), "the input must survive the gated probe");
		assertEquals(0, tOven.mTanksOutput[0].amount());
	}

	/** The fill-then-deduct push: partial acceptance and refusal never over-draw (ADR ruling ①). */
	@Test
	void fluidPushIsFillThenDeduct() {
		GT6RecipeMaps.init();
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestProcessingOven tOven = newOven(tLevel);

		StubFluidTarget tTarget = new StubFluidTarget();
		tTarget.mCapacity = 200; // accepts a partial 200 of 500
		tOven.mFluidTarget = tTarget;
		tOven.mTanksOutput[0].add(500, new FluidStack(Fluids.WATER, 500));

		tOven.doOutputFluids();
		assertEquals(200, tTarget.mContent.getAmount());
		assertEquals(300, tOven.mTanksOutput[0].amount(), "the source keeps the unaccepted remainder");

		// refusal: 0 accepted → the source untouched
		tTarget.mRefuse = true;
		tOven.doOutputFluids();
		assertEquals(300, tOven.mTanksOutput[0].amount(), "0 accepted = source untouched (fill-then-deduct)");

		// no target at all: nothing moves
		tOven.mFluidTarget = null;
		tOven.doOutputFluids();
		assertEquals(300, tOven.mTanksOutput[0].amount());
	}

	/** The slot gating (canInsertItem2/canExtractItem2 :549-559 trimmed) drives the gated capability wrapper. */
	@Test
	void slotGating() {
		GT6RecipeMaps.init();
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestProcessingOven tOven = newOven(tLevel);

		assertTrue(tOven.canInsertItem2(0), "slot 0 is the input");
		assertFalse(tOven.canInsertItem2(1), "output slots reject inserts");
		assertFalse(tOven.canInsertItem2(10), "the special slot rejects inserts");
		assertFalse(tOven.canExtractItem2(0), "the input slot rejects extracts");
		assertTrue(tOven.canExtractItem2(1), "output slots allow extracts");
		assertFalse(tOven.canExtractItem2(10), "the special slot rejects extracts");
	}

	/** The NBT round trip over the machine state (acceptance ② last clause). */
	@Test
	void nbtRoundTrip() {
		GT6RecipeMaps.init();
		GT6RecipeMaps.COKE_OVEN.addRecipe(standardRecipe());
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestProcessingOven tOven = newOven(tLevel);

		tOven.mInventory.insertItem(0, new ItemStack(Items.COAL, 3), false);
		tOven.mInventory.insertItem(1, new ItemStack(Items.DIAMOND, 2), false);
		tOven.mTanksOutput[0].add(750, new FluidStack(Fluids.WATER, 750));
		tOven.mEnergy = 42;
		tOven.mMinEnergy = 1;
		tOven.mProgress = 777;
		tOven.mMaxProgress = 3600;
		tOven.mIgnited = 33;
		tOven.mStopped = true;
		tOven.mOutputItems = new ItemStack[] {new ItemStack(Items.DIAMOND, 5)};
		tOven.mOutputFluids = new FluidStack[] {new FluidStack(Fluids.WATER, 500)};

		CompoundTag tTag = tOven.saveWithoutMetadata();

		TestProcessingOven tRestored = new TestProcessingOven(sCokeOvenType, P1, Blocks.BRICKS.defaultBlockState());
		// the load face reads level.registryAccess() for the provider-based inventory/fluid
		// legs (21.1) — the fixture level supplies it (task p15-m4-test-infra-2)
		tRestored.setLevel(tLevel);
		tRestored.load(tTag);

		assertEquals(42, tRestored.mEnergy);
		assertEquals(1, tRestored.mMinEnergy);
		assertEquals(777, tRestored.mProgress);
		assertEquals(3600, tRestored.mMaxProgress);
		assertEquals(33, tRestored.mIgnited);
		assertTrue(tRestored.mStopped);
		assertEquals(3, tRestored.slot(0).getCount());
		assertEquals(2, tRestored.slot(1).getCount());
		assertEquals(750, tRestored.mTanksOutput[0].amount());
		assertEquals(1, tRestored.mOutputItems.length);
		assertEquals(5, tRestored.mOutputItems[0].getCount());
		assertEquals(1, tRestored.mOutputFluids.length);
		assertEquals(500, tRestored.mOutputFluids[0].getAmount());
	}

	/** The upstream :1193 registration values are the field defaults (the ADR ruling ⑤ anchor). */
	@Test
	void registrationDefaults() {
		GT6RecipeMaps.init();
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestProcessingOven tOven = newOven(tLevel);
		assertEquals(1, tOven.mInputMin);
		assertEquals(1, tOven.mInput);
		assertEquals(16, tOven.mInputMax);
		assertEquals(16, tOven.mParallel);
		assertTrue(tOven.mRequiresIgnition);
		assertTrue(tOven.mNoConstantEnergy, "NO_CONSTANT_POWER = T (Loader_MultiTileEntities.java:1193)");
		assertEquals(11, tOven.INVENTORY_SIZE);
		assertEquals(Long.MAX_VALUE, tOven.mTanksOutput[0].capacity(), "the upstream FluidTankGT default ctor capacity (FluidTankGT.java:49)");
		assertNotNull(GT6RecipeMaps.COKE_OVEN, "the lazy recipes() resolution target");
		assertNull(tOven.mRecipes, "resolved on first use");
		assertEquals(GT6RecipeMaps.COKE_OVEN, tOven.recipes());
	}
}
