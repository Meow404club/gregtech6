package gregtech6.tileentity.multiblocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;

import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;
import gregtech6.registry.GT6LargeMachines.GTLargeMachineBlockEntity;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The live doWork-path semantics of the twelve large machines (task p29-w3-large-12 —
 * the acceptance ②③④⑤⑥ offline legs), driven on the REAL
 * {@link GTLargeMachineBlockEntity} (row=null fixtures: the declared-pattern arm is
 * null-formed, the row config fields ride direct — the GTMultiBlockMachineSemanticsTest
 * shape):
 * <ul>
 * <li>acceptance ② — parallel 16 + PARALLEL_DURATION: the duration carries the parallels
 *     (mMinEnergy stays the recipe eUt while the outputs multiply; the :766-768 vs
 *     :770-771 pair bit-exact);</li>
 * <li>acceptance ④ — eff 5000 = half speed (units(x, 5000, 10000) = 2x the bar);</li>
 * <li>acceptance ③ — the NO_CONSTANT_POWER power-gap retention (the W1 kinetic ruling
 *     extended to the multiblock TU/RU shapes) against a constant-power control;</li>
 * <li>acceptance ⑥ — the 512-window lower bound refuses a 511-energy tick;</li>
 * <li>the fluid-input half — the coagulator water row consumes and drains the input
 *     tank;</li>
 * <li>acceptance ⑤ (offline half) — the energy-type gate: the doInject door refuses the
 *     wrong type, pays the right one, and overcharge swallows.</li>
 * </ul>
 */
class GT6LargeMachineSemanticsTest extends GTMultiBlocksOfflineTestBase {

	private static final BlockPos P1 = new BlockPos(300, 64, 100);

	static BlockEntityType<GTLargeMachineBlockEntity> sLargeType;

	@BeforeAll
	static void buildFixture() {
		// the vanilla boot chain already ran in the parent @BeforeAll — the frozen-registry
		// reopen is the only extra face the fixture BET needs (the selfHolder recipe)
		GTOfflineTestBase.unfreezeBlockEntityTypeRegistry();
		@SuppressWarnings("unchecked")
		BlockEntityType<GTLargeMachineBlockEntity>[] tHolder = (BlockEntityType<GTLargeMachineBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTLargeMachineBlockEntity(tHolder[0], aPos, aState, null),
				Blocks.BRICKS).build(null);
		sLargeType = tHolder[0];
	}
	static RecipeMap sMap;
	static java.util.HashSet<Recipe> sRecipes;

	static RecipeMap testMap() {
		if (sMap == null) {
			sRecipes = new java.util.HashSet<>();
			sMap = new RecipeMap(sRecipes, "gt6.test.large", "Large Test", null,
					0, 1, "gt6:textures/gui/machines/oven", 1, 9, 1, 0, 1, 0, 1, 1);
		}
		sRecipes.clear(); // a fresh row set per test
		return sMap;
	}

	static Recipe itemRecipe(int aCount, int aDuration, long aEUt) {
		return new Recipe(true, new ItemStack[] {new ItemStack(Items.COAL, aCount)},
				new ItemStack[] {new ItemStack(Items.DIAMOND, 1)},
				new FluidStack[0], new FluidStack[0], aDuration, aEUt, 0);
	}

	/** The centrifuge row config over a null-row fixture (the EnergyRowSpec card-① carrier). */
	private static GTLargeMachineBlockEntity centrifuge(RecipeMap aMap) {
		GTLargeMachineBlockEntity tMachine = new GTLargeMachineBlockEntity(sLargeType, P1, Blocks.BRICKS.defaultBlockState(), null);
		tMachine.mRecipes = aMap;
		tMachine.applyEnergyRowSpec(new TileEntityBase10MultiBlockMachine.EnergyRowSpec(512L, 512L, 4096L, 16, true, true));
		tMachine.mEnergyTypeAccepted = gregapi.data.TD.Energy.RU;
		tMachine.mEfficiency = 5000;
		return tMachine;
	}

	// -------------------------------------------------------------------------
	// acceptance ② — parallel 16 + PARALLEL_DURATION: speed, not energy
	// -------------------------------------------------------------------------

	@Test
	void parallelDurationCarriesTheParallelsAtRecipeEnergy() {
		RecipeMap tMap = testMap();
		tMap.addRecipe(itemRecipe(1, 16, 16)); // coal -> diamond, duration 16, eUt 16
		GTLargeMachineBlockEntity tMachine = centrifuge(tMap);
		tMachine.mInventory.setStackInSlot(0, new ItemStack(Items.COAL, 16));
		tMachine.mIgnited = 40;

		assertEquals(TileEntityBase10MultiBlockMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tMachine.checkRecipe(true, false));
		// :766-768 — the energy stays at the recipe eUt (16), the bar carries the parallels
		// AND the eff-5000 divisor: units(16*16*16, 5000, 10000, T) = 8192
		assertEquals(16, tMachine.mMinEnergy, "PARALLEL_DURATION rows keep mMinEnergy at the recipe eUt — parallel = speed, not energy");
		assertEquals(8192, tMachine.mMaxProgress, "the bar is eUt*duration*parallel x2 (eff 5000)");
		assertEquals(16, tMachine.mOutputItems[0].getCount(), "the 16 parallels multiply the OUTPUTS");
	}

	@Test
	void theEnergyFormMultipliesTheEnergyInstead() {
		// the same 16 parallels with mParallelDuration F: mMinEnergy = eUt * count (the
		// :770-771 form) — the side-by-side the "parallel = speed not energy" ruling pins
		RecipeMap tMap = testMap();
		tMap.addRecipe(itemRecipe(1, 16, 16));
		GTLargeMachineBlockEntity tMachine = centrifuge(tMap);
		tMachine.mParallelDuration = false; // flip the row flag only
		tMachine.mInventory.setStackInSlot(0, new ItemStack(Items.COAL, 16));
		tMachine.mIgnited = 40;

		assertEquals(TileEntityBase10MultiBlockMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tMachine.checkRecipe(true, false));
		assertEquals(256, tMachine.mMinEnergy, "the energy carries the parallels: 16 x 16");
		assertEquals(8192, tMachine.mMaxProgress, "units(256*16, 5000, 10000, T) = 8192 — the same 8192 progress-units as the duration form, burned at 256/t over 32 ticks");
	}

	// -------------------------------------------------------------------------
	// acceptance ④ — eff 5000 = half speed
	// -------------------------------------------------------------------------

	@Test
	void efficiencyFiveThousandDoublesTheBar() {
		RecipeMap tMap = testMap();
		tMap.addRecipe(itemRecipe(1, 100, 16));
		GTLargeMachineBlockEntity tHalf = centrifuge(tMap); // eff 5000, parallel 1 leg
		tHalf.mInventory.setStackInSlot(0, new ItemStack(Items.COAL, 1));
		tHalf.mIgnited = 40;
		assertEquals(TileEntityBase10MultiBlockMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tHalf.checkRecipe(true, false));
		assertEquals(3200, tHalf.mMaxProgress, "units(16*100, 5000, 10000, T) = 3200 — the W1 units() half-speed bar (2x the 1600 natural bar)");

		GTLargeMachineBlockEntity tFull = centrifuge(tMap);
		tFull.mEfficiency = 10000; // the identity control
		tFull.mInventory.setStackInSlot(0, new ItemStack(Items.COAL, 1));
		tFull.mIgnited = 40;
		assertEquals(TileEntityBase10MultiBlockMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tFull.checkRecipe(true, false));
		assertEquals(1600, tFull.mMaxProgress, "eff 10000 = the identity (the same natural 16*100 bar)");

		// the Oven's 2500 = 4x the bar
		GTLargeMachineBlockEntity tQuarter = centrifuge(tMap);
		tQuarter.mEfficiency = 2500;
		tQuarter.mInventory.setStackInSlot(0, new ItemStack(Items.COAL, 1));
		tQuarter.mIgnited = 40;
		assertEquals(TileEntityBase10MultiBlockMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tQuarter.checkRecipe(true, false));
		assertEquals(6400, tQuarter.mMaxProgress, "units(1600, 2500, 10000) = 6400");
	}

	// -------------------------------------------------------------------------
	// acceptance ③ — NO_CONSTANT_POWER retention vs the constant-power reset
	// -------------------------------------------------------------------------

	@Test
	void noConstantPowerKeepsProgressThroughTheGap() {
		RecipeMap tMap = testMap();
		tMap.addRecipe(itemRecipe(1, 3200, 16)); // a long bar: 16 eUt x 3200 dur = 51200 progress
		GTLargeMachineBlockEntity tNcp = centrifuge(tMap);
		tNcp.mInventory.setStackInSlot(0, new ItemStack(Items.COAL, 1));
		tNcp.mIgnited = 40;
		assertEquals(TileEntityBase10MultiBlockMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tNcp.checkRecipe(true, false));
		// two powered ticks: progress climbs by min(mInputMax, mEnergy) per tick
		tNcp.mStructureOkay = true; // the scripted-formed arm (offline: no level)
		tNcp.mEnergy = 4096;
		tNcp.doWork(10);
		tNcp.mEnergy = 4096;
		tNcp.doWork(11);
		long tProgressed = tNcp.mProgress;
		assertTrue(tProgressed > 0, "the work chain runs while fed");
		tNcp.mStructureOkay = true; // the scripted-formed arm (see theWindowLowerBoundRefusesTheUnderfedTick)
		// the power gap: mEnergy drained to zero (the :791 floor) → doInactive
		tNcp.mEnergy = 0;
		tNcp.doWork(50); // aTimer > 40
		assertEquals(tProgressed, tNcp.mProgress, "the NCP row keeps its progress through the gap (no :894 reset)");

		// the constant-power control: the same gap RESETS
		GTLargeMachineBlockEntity tCp = centrifuge(tMap);
		tCp.mNoConstantEnergy = false; // the six non-NCP rows
		tCp.mInventory.setStackInSlot(0, new ItemStack(Items.COAL, 1));
		tCp.mIgnited = 40;
		assertEquals(TileEntityBase10MultiBlockMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tCp.checkRecipe(true, false));
		tCp.mStructureOkay = true;
		tCp.mEnergy = 4096;
		tCp.doWork(10);
		assertTrue(tCp.mProgress > 0);
		tCp.mEnergy = 0;
		tCp.doWork(50); // aTimer > 40 — the :782 doInactive entry the :894 reset rides
		assertEquals(0, tCp.mProgress, "the constant-power rows reset on the gap (the :894 half)");
	}

	// -------------------------------------------------------------------------
	// acceptance ⑥ — the 512-window lower bound refuses 511
	// -------------------------------------------------------------------------

	@Test
	void theWindowLowerBoundRefusesTheUnderfedTick() {
		RecipeMap tMap = testMap();
		tMap.addRecipe(itemRecipe(1, 16, 16));
		GTLargeMachineBlockEntity tMachine = centrifuge(tMap);
		tMachine.mInventory.setStackInSlot(0, new ItemStack(Items.COAL, 1));
		tMachine.mIgnited = 40;
		assertEquals(TileEntityBase10MultiBlockMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tMachine.checkRecipe(true, false));
		tMachine.mStructureOkay = true; // the scripted-formed arm (offline: no level — the doWork :780 gate reads the cached verdict)
		tMachine.mEnergy = 511; // one under the :1229 NBT_INPUT_MIN 512
		tMachine.doWork(10);
		assertFalse(tMachine.mRunning, "511 < mInputMin 512 — the :780 gate refuses, no work");
		assertEquals(0, tMachine.mProgress);
		tMachine.mEnergy = 512;
		tMachine.doWork(11);
		assertTrue(tMachine.mRunning, "512 passes the window minimum");
	}

	// -------------------------------------------------------------------------
	// the fluid-input half — the coagulator water row
	// -------------------------------------------------------------------------

	static RecipeMap sFluidMap;
	static java.util.HashSet<Recipe> sFluidRecipes;

	/** The fluid-row map: mMinimalInputItems 0 (the :708 gate releases the itemless rows). */
	static RecipeMap fluidMap() {
		if (sFluidMap == null) {
			sFluidRecipes = new java.util.HashSet<>();
			sFluidMap = new RecipeMap(sFluidRecipes, "gt6.test.large.fluid", "Large Fluid Test", null,
					0, 1, "gt6:textures/gui/machines/oven", 1, 9, 0, 1, 1, 0, 1, 1);
		}
		sFluidRecipes.clear();
		return sFluidMap;
	}

	@Test
	void theFluidRowConsumesAndDrainsTheInputTank() {
		RecipeMap tMap = fluidMap();
		// the coagulator.json smoke row: water 1000 -> snowball, duration 64, eUt 1
		tMap.addRecipe(new Recipe(true, new ItemStack[0],
				new ItemStack[] {new ItemStack(Items.SNOWBALL, 1)},
				new FluidStack[] {new FluidStack(Fluids.WATER, 1000)}, new FluidStack[0], 64, 1, 0));
		GTLargeMachineBlockEntity tMachine = new GTLargeMachineBlockEntity(sLargeType, P1, Blocks.BRICKS.defaultBlockState(), null);
		tMachine.mRecipes = tMap;
		tMachine.applyEnergyRowSpec(new TileEntityBase10MultiBlockMachine.EnergyRowSpec(1L, 1L, 16L, 64, false, false));
		tMachine.mEnergyTypeAccepted = gregapi.data.TD.Energy.TU;
		tMachine.mIgnited = 40;

		assertEquals(1000, tMachine.mTanksInput[0].fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE));
		assertEquals(TileEntityBase10MultiBlockMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tMachine.checkRecipe(true, false));
		assertEquals(0, tMachine.mTanksInput[0].amount(), "the apply drains the 1000 L water from the input tank");
		assertEquals(64, tMachine.mMaxProgress, "the TU window keeps the constant per-process energy: eUt 1 x dur 64");
	}

	@Test
	void theFluidRowRefusesWithoutTheFluid() {
		RecipeMap tMap = fluidMap();
		tMap.addRecipe(new Recipe(true, new ItemStack[0],
				new ItemStack[] {new ItemStack(Items.SNOWBALL, 1)},
				new FluidStack[] {new FluidStack(Fluids.WATER, 1000)}, new FluidStack[0], 64, 1, 0));
		GTLargeMachineBlockEntity tMachine = new GTLargeMachineBlockEntity(sLargeType, P1, Blocks.BRICKS.defaultBlockState(), null);
		tMachine.mRecipes = tMap;
		tMachine.applyEnergyRowSpec(new TileEntityBase10MultiBlockMachine.EnergyRowSpec(1L, 1L, 16L, 64, false, false));
		tMachine.mIgnited = 40;
		assertEquals(TileEntityBase10MultiBlockMachine.DID_NOT_FIND_RECIPE, tMachine.checkRecipe(true, false),
				"the empty input tank refuses the fluid row (the :283 null-fluids gate via the restored :689-706 read)");
	}

	// -------------------------------------------------------------------------
	// acceptance ⑤ (offline half) — the energy-type gate on the doInject door
	// -------------------------------------------------------------------------

	@Test
	void theEnergyDoorGatesTheTypeAndTheWindow() {
		RecipeMap tMap = testMap();
		tMap.addRecipe(itemRecipe(1, 16, 16));
		GTLargeMachineBlockEntity tMachine = centrifuge(tMap); // RU, window 512..4096

		// the wrong type pays nothing
		assertEquals(0, tMachine.doInject(gregapi.data.TD.Energy.EU, (byte) 2, 512, 1, true), "an EU packet on an RU row pays nothing");
		assertEquals(0, tMachine.mEnergy);
		// the right type charges min(mInputMax - mEnergy, size * amount)
		assertEquals(1, tMachine.doInject(gregapi.data.TD.Energy.RU, (byte) 2, 512, 1, true), "an in-window RU packet pays one amp");
		assertEquals(512, tMachine.mEnergy);
		// the overcharge wall: a 4097-size packet is refused whole (the :493-495 body)
		assertEquals(3, tMachine.doInject(gregapi.data.TD.Energy.RU, (byte) 2, 4097, 3, false), "the overcharge probe reports the whole amount");
		// the stopped machine refuses everything (the :490 line)
		tMachine.setStateOnOff(false);
		assertTrue(tMachine.mStopped);
		assertEquals(0, tMachine.doInject(gregapi.data.TD.Energy.RU, (byte) 2, 512, 1, true), "the stopped machine refuses the door");
	}
}
