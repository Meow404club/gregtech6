package gregtech6.tileentity.multiblocks;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.annotation.Nullable;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;

/**
 * The large-machine behavior constants pinned (task p29-w3-nbtdesign-parts ② — the card
 * ④⑤⑥ semantic predecessors). Every case is an upstream-direct translation of the
 * TileEntityBase10MultiBlockMachine doWork path — i.e. the MultiTileEntityBasicMachine
 * :126-131 NBT keys and the :626-629/:742-745/:761-774 doWork bodies, byte-for-byte:
 * <ul>
 * <li>the two fixed-window forms: the NBT_INPUT derived form (in/2..in*2) and the
 *     512/1/4096-style explicit MIN/MAX overrides landing AFTER it (:126-128);</li>
 * <li>NBT_PARALLEL max(1,·) (:130), NBT_CHEAP_OVERCLOCKING (:122) and
 *     NBT_PARALLEL_DURATION (:131) as row-re-pointable flags;</li>
 * <li>the :773 overclock loop (4x energy, 2x speed) gated on !mCheapOverclocking;</li>
 * <li>the :766-768 vs :770-771 dual form (duration vs energy carries the parallels);</li>
 * <li>the :743 energy bind (skipped for TU and for parallelDuration rows);</li>
 * <li>the :626-629 chain-processing power cap (mInputMax * 600).</li>
 * </ul>
 */
class GTMultiBlockMachineSemanticsTest extends GTMultiBlocksOfflineTestBase {

	private static final BlockPos P1 = new BlockPos(120, 64, 100);

	@org.junit.jupiter.api.BeforeAll
	static void buildMap() {
		sRecipes = new java.util.HashSet<>();
		sMap = new RecipeMap(sRecipes, "gt6.test.semantics", "Semantics Test", null,
				0, 1, "gt6:textures/gui/machines/Oven", 1, 9, 1, 0, 1, 0, 1, 1);
	}

	/** The standalone map for these tests (1 input slot, 9 outputs, power 1) — ONE instance: map names register globally. */
	private static RecipeMap sMap;
	private static java.util.HashSet<Recipe> sRecipes;

	static RecipeMap testMap() {
		sRecipes.clear(); // a fresh row set per test
		return sMap;
	}

	/** The scripted-formed machine over a private map (the TestProcessingOven recipe). */
	static class TestSemanticsMachine extends TileEntityCokeOven {
		TestSemanticsMachine(BlockEntityType<?> aType, BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
			super(aType, aPos, aState);
		}

		@Override
		public boolean checkStructure2(@Nullable BlockPos aCoordinates, @Nullable net.minecraft.world.entity.player.Player aPlayer, @Nullable net.minecraft.world.Container aInventory) {
			return true;
		}

		@Override
		protected net.minecraftforge.fluids.capability.IFluidHandler getFluidOutputTarget(net.minecraft.world.level.material.Fluid aOutput) {
			return null;
		}
	}

	private static TestSemanticsMachine newMachine(RecipeMap aMap) {
		TestSemanticsMachine tMachine = new TestSemanticsMachine(sCokeOvenType, P1, Blocks.BRICKS.defaultBlockState());
		tMachine.mRecipes = aMap;
		return tMachine;
	}

	/** A plain recipe: count coals → count diamonds, the given duration/eUt. */
	private static Recipe recipe(int aCount, long aDuration, long aEUt) {
		return new Recipe(true, new ItemStack[] {new ItemStack(Items.COAL, aCount)},
				new ItemStack[] {new ItemStack(Items.DIAMOND, 1)},
				new FluidStack[0], new FluidStack[0], aDuration, aEUt, 0);
	}

	// ---------------------------------------------------------------------------
	// the two window forms (:126-128)
	// ---------------------------------------------------------------------------

	@Test
	void theDerivedWindowFormIsTheInputHalvingDoubling() {
		TileEntityBase10MultiBlockMachine tMachine = newMachine(testMap());
		tMachine.applyEnergyRowSpec(TileEntityBase10MultiBlockMachine.EnergyRowSpec.ofInput(512));
		assertEquals(256, tMachine.mInputMin, ":126 min = in/2");
		assertEquals(512, tMachine.mInput);
		assertEquals(1024, tMachine.mInputMax, ":126 max = in*2");
	}

	@Test
	void theExplicitOverridesLandAfterTheDerivedForm() {
		TileEntityBase10MultiBlockMachine tMachine = newMachine(testMap());
		// the :1229 LargeCentrifuge shape: NBT_INPUT 512, NBT_INPUT_MIN 1, NBT_INPUT_MAX 4096
		tMachine.applyEnergyRowSpec(new TileEntityBase10MultiBlockMachine.EnergyRowSpec(512L, 1L, 4096L, 16, null, null));
		assertEquals(1, tMachine.mInputMin, "the :127 MIN override beats the derived in/2");
		assertEquals(512, tMachine.mInput);
		assertEquals(4096, tMachine.mInputMax, "the :128 MAX override beats the derived in*2");
		assertEquals(16, tMachine.mParallel, ":130 the parallel column lands alongside");

		// the :1193 Coke Oven triple 16/1/16 → the 1/1/16 shape (16 derives 8/16/32 first)
		TileEntityBase10MultiBlockMachine tOven = newMachine(testMap());
		tOven.applyEnergyRowSpec(new TileEntityBase10MultiBlockMachine.EnergyRowSpec(16L, 1L, 16L, null, null, null));
		assertEquals(1, tOven.mInputMin);
		assertEquals(16, tOven.mInput);
		assertEquals(16, tOven.mInputMax);
	}

	@Test
	void absentKeysKeepThePriorValues() {
		TileEntityBase10MultiBlockMachine tMachine = newMachine(testMap());
		tMachine.applyEnergyRowSpec(TileEntityBase10MultiBlockMachine.EnergyRowSpec.ofInput(128));
		tMachine.applyEnergyRowSpec(TileEntityBase10MultiBlockMachine.EnergyRowSpec.none()); // the hasKey-false pass
		assertEquals(64, tMachine.mInputMin, "the absent-key pass changes nothing");
		assertEquals(128, tMachine.mInput);
		assertEquals(256, tMachine.mInputMax);
	}

	@Test
	void parallelClampsToOne() {
		TileEntityBase10MultiBlockMachine tMachine = newMachine(testMap());
		tMachine.applyEnergyRowSpec(new TileEntityBase10MultiBlockMachine.EnergyRowSpec(null, null, null, 0, null, null));
		assertEquals(1, tMachine.mParallel, ":130 Math.max(1, n)");
		tMachine.applyEnergyRowSpec(new TileEntityBase10MultiBlockMachine.EnergyRowSpec(null, null, null, -7, null, null));
		assertEquals(1, tMachine.mParallel, ":130 Math.max(1, negative)");
	}

	// ---------------------------------------------------------------------------
	// the :773 overclock gate (NBT_CHEAP_OVERCLOCKING)
	// ---------------------------------------------------------------------------

	@Test
	void theOverclockFoldRunsUntilTheWindowMinimumWithoutCheapOverclocking() {
		RecipeMap tMap = testMap();
		tMap.addRecipe(recipe(1, 100, 16)); // eUt 16 duration 100
		TileEntityBase10MultiBlockMachine tMachine = newMachine(tMap);
		// a window whose MINIMUM sits above the recipe eUt — the fold fuel
		tMachine.applyEnergyRowSpec(new TileEntityBase10MultiBlockMachine.EnergyRowSpec(null, 512L, 4096L, null, null, null));
		tMachine.mInventory.setStackInSlot(0, new ItemStack(Items.COAL, 1));
		tMachine.mIgnited = 40; // open the :737 gate
		tMachine.mEnergyTypeAccepted = gregapi.data.TD.Energy.RU; // a non-TU shape, parallel 1 → count 1

		assertEquals(TileEntityBase10MultiBlockMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tMachine.checkRecipe(true, false));
		// :773 — 16 -> 64 -> 256 -> 1024, then 1024 < 512 fails: three folds
		assertEquals(1024, tMachine.mMinEnergy, "mMinEnergy folds x4 until it passes the window minimum");
		// the progress unit IS an energy unit: 1600 base (16 x 100) x2 per fold — 12800 at 1024/tick = 12.5 t
		assertEquals(12800, tMachine.mMaxProgress, "each fold halves the RUN TIME: 1600/16=100t -> 3200/64=50t -> 6400/256=25t -> 12800/1024=12.5t");
	}

	@Test
	void cheapOverclockingRefusesTheForcedFold() {
		RecipeMap tMap = testMap();
		tMap.addRecipe(recipe(1, 100, 16));
		TileEntityBase10MultiBlockMachine tMachine = newMachine(tMap);
		tMachine.applyEnergyRowSpec(new TileEntityBase10MultiBlockMachine.EnergyRowSpec(null, 512L, 4096L, null, true, null)); // NBT_CHEAP_OVERCLOCKING T (window min 512 > eUt 16 >= mInputMax floor)
		tMachine.mInventory.setStackInSlot(0, new ItemStack(Items.COAL, 1));
		tMachine.mIgnited = 40;
		tMachine.mEnergyTypeAccepted = gregapi.data.TD.Energy.RU;

		assertEquals(TileEntityBase10MultiBlockMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tMachine.checkRecipe(true, false));
		assertEquals(16, tMachine.mMinEnergy, "the :773 fold never fires — the recipe runs at its natural eUt");
		assertEquals(1600, tMachine.mMaxProgress, "natural duration: 16 x 100");
	}

	// ---------------------------------------------------------------------------
	// the :766-768 vs :770-771 dual form (NBT_PARALLEL_DURATION)
	// ---------------------------------------------------------------------------

	@Test
	void parallelDurationCarriesTheParallelsInTheDuration() {
		RecipeMap tMap = testMap();
		tMap.addRecipe(recipe(1, 100, 32));
		TileEntityBase10MultiBlockMachine tMachine = newMachine(tMap);
		tMachine.applyEnergyRowSpec(new TileEntityBase10MultiBlockMachine.EnergyRowSpec(512L, 1L, 4096L, 4, null, true)); // NBT_PARALLEL_DURATION T
		tMachine.mInventory.setStackInSlot(0, new ItemStack(Items.COAL, 4));
		tMachine.mIgnited = 40;
		tMachine.mEnergyTypeAccepted = gregapi.data.TD.Energy.RU;

		assertEquals(TileEntityBase10MultiBlockMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tMachine.checkRecipe(true, false));
		assertEquals(32, tMachine.mMinEnergy, ":766-768 — the energy stays at the recipe eUt");
		assertEquals(12800, tMachine.mMaxProgress, ":768 — eUt x duration x parallel count (32 x 100 x 4)");
	}

	@Test
	void plainParallelCarriesTheParallelsInTheEnergy() {
		RecipeMap tMap = testMap();
		tMap.addRecipe(recipe(1, 100, 32));
		TileEntityBase10MultiBlockMachine tMachine = newMachine(tMap);
		tMachine.applyEnergyRowSpec(new TileEntityBase10MultiBlockMachine.EnergyRowSpec(512L, 1L, 4096L, 4, null, false));
		tMachine.mInventory.setStackInSlot(0, new ItemStack(Items.COAL, 4));
		tMachine.mIgnited = 40;
		tMachine.mEnergyTypeAccepted = gregapi.data.TD.Energy.RU;

		assertEquals(TileEntityBase10MultiBlockMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tMachine.checkRecipe(true, false));
		assertEquals(128, tMachine.mMinEnergy, ":770 non-TU half — the energy scales with the count (32 x 4)");
		assertEquals(12800, tMachine.mMaxProgress, ":771 — the RUN TIME stays at the recipe time (128 x 100)");
	}

	@Test
	void tuMachinesKeepAConstantPerProcessEnergyWhenParalleling() {
		RecipeMap tMap = testMap();
		tMap.addRecipe(recipe(1, 100, 32));
		TileEntityBase10MultiBlockMachine tMachine = newMachine(tMap);
		tMachine.applyEnergyRowSpec(new TileEntityBase10MultiBlockMachine.EnergyRowSpec(512L, 1L, 4096L, 4, null, false));
		tMachine.mInventory.setStackInSlot(0, new ItemStack(Items.COAL, 4));
		tMachine.mIgnited = 40;
		// mEnergyTypeAccepted stays TU (the Coke Oven shape)

		assertEquals(TileEntityBase10MultiBlockMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tMachine.checkRecipe(true, false));
		assertEquals(32, tMachine.mMinEnergy, ":770 TU ternary — no per-parallel energy multiply");
		assertEquals(3200, tMachine.mMaxProgress, ":771 — 32 x 100");
	}

	// ---------------------------------------------------------------------------
	// the :743 energy bind
	// ---------------------------------------------------------------------------

	@Test
	void theEnergyBindCapsParallelsByTheNominalInput() {
		RecipeMap tMap = testMap();
		tMap.addRecipe(recipe(4, 100, 128)); // eUt 128 — the bind budget is mInput / eUt
		TileEntityBase10MultiBlockMachine tMachine = newMachine(tMap);
		tMachine.applyEnergyRowSpec(new TileEntityBase10MultiBlockMachine.EnergyRowSpec(512L, null, null, 16, null, false));
		tMachine.mInventory.setStackInSlot(0, new ItemStack(Items.COAL, 16)); // fuel for 16 parallels
		tMachine.mIgnited = 40;
		tMachine.mEnergyTypeAccepted = gregapi.data.TD.Energy.RU;

		assertEquals(TileEntityBase10MultiBlockMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tMachine.checkRecipe(true, false));
		// :743 — bind(1, 16, 512/128 = 4): the machine only feeds 4 eUt-128 parallels
		assertEquals(4, tMachine.mOutputItems[0].getCount(), "the outputs carry the bound parallel count (not the row's 16)");
		assertEquals(512, tMachine.mMinEnergy, ":770 — 4 x 128 = the bound parallel count in the energy");
	}

	@Test
	void parallelDurationRowsSkipTheEnergyBind() {
		RecipeMap tMap = testMap();
		tMap.addRecipe(recipe(4, 100, 128));
		TileEntityBase10MultiBlockMachine tMachine = newMachine(tMap);
		tMachine.applyEnergyRowSpec(new TileEntityBase10MultiBlockMachine.EnergyRowSpec(512L, 1L, 4096L, 16, null, true));
		tMachine.mInventory.setStackInSlot(0, new ItemStack(Items.COAL, 64)); // the row-count fuel: 16 processes x 4 coals
		tMachine.mIgnited = 40;
		tMachine.mEnergyTypeAccepted = gregapi.data.TD.Energy.RU;

		assertEquals(TileEntityBase10MultiBlockMachine.FOUND_AND_SUCCESSFULLY_USED_RECIPE, tMachine.checkRecipe(true, false));
		assertEquals(128, tMachine.mMinEnergy, "no bind on a parallelDuration row — the full 16 parallels run at the recipe eUt");
		assertEquals(204800, tMachine.mMaxProgress, ":768 — 32 x 100 x 16");
	}

	// ---------------------------------------------------------------------------
	// the :626-629 chain-processing cap
	// ---------------------------------------------------------------------------

	@Test
	void theChainCapLimitsParallelsToTwoMinutesOfInput() {
		RecipeMap tMap = testMap();
		// total power = 60000 eUt*t; the cap window = mInputMax * 600 = 4096 * 600 = 2457600 → cap 40
		tMap.addRecipe(recipe(1, 60000, 1));
		TileEntityBase10MultiBlockMachine tMachine = newMachine(tMap);
		tMachine.applyEnergyRowSpec(new TileEntityBase10MultiBlockMachine.EnergyRowSpec(null, null, 4096L, 64, null, true));
		tMachine.mParallel = 64;

		assertEquals(40, tMachine.canOutput(tMap.mRecipeList.iterator().next()),
				":628 — the parallel count backs off until power x n <= mInputMax x 600");
	}

	@Test
	void theChainCapStaysOffForPlainParallelRows() {
		RecipeMap tMap = testMap();
		tMap.addRecipe(recipe(1, 60000, 1));
		TileEntityBase10MultiBlockMachine tMachine = newMachine(tMap);
		tMachine.applyEnergyRowSpec(new TileEntityBase10MultiBlockMachine.EnergyRowSpec(null, null, 4096L, 64, null, false));
		tMachine.mParallel = 64;

		assertEquals(64, tMachine.canOutput(tMap.mRecipeList.iterator().next()),
				"no :626-629 cap without NBT_PARALLEL_DURATION — the parallel column stands");
	}

	// ---------------------------------------------------------------------------
	// config is not persisted (the loadKeepsTheConstructorInjectedConfig contract)
	// ---------------------------------------------------------------------------

	@Test
	void theRowConfigDoesNotRideNBT() {
		RecipeMap tMap = testMap();
		TileEntityBase10MultiBlockMachine tMachine = newMachine(tMap);
		tMachine.applyEnergyRowSpec(new TileEntityBase10MultiBlockMachine.EnergyRowSpec(512L, 1L, 4096L, 8, true, true));
		net.minecraft.nbt.CompoundTag tTag = newTag();
		tMachine.saveAdditional(tTag);
		assertFalse(tTag.contains("cheapoverclocking") || tTag.contains("parallelduration"),
				"the flags are registration config, not persisted state");
		TileEntityBase10MultiBlockMachine tReloaded = newMachine(tMap);
		tReloaded.load(tTag);
		assertFalse(tReloaded.mCheapOverclocking, "a fresh BE starts at the upstream default F");
		assertFalse(tReloaded.mParallelDuration, "a fresh BE starts at the upstream default F");
		assertTrue(tReloaded.mEnergyTypeAccepted == gregapi.data.TD.Energy.TU, "a fresh BE starts at the TU carrier");
	}

	/** CompoundTag alias (the save/load test above). */
	private static net.minecraft.nbt.CompoundTag newTag() {
		return new net.minecraft.nbt.CompoundTag();
	}
}
