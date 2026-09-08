package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.BiFunction;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.registry.GTMaterialItems;

/**
 * The RM.Extruder plate/rod static pour, offline (task p26-w1-press-extruder-molds): the
 * fixture-seam pour over the offline material universe (the GT6RecipesOreChainTest stub
 * form) and the row-consumption semantics — the MOLD-NOT-CONSUMED pin on the STATIC rows
 * (both the plate AND the rod row, the remember-id478 archaeology: the RM.java:405/:407
 * fifth-boolean F/T axis is aLogErrors, the never-consumed semantic is the upstream
 * size-0 mold input that BOTH rows carry).
 *
 * <p>The mod items are not constructible offline (the intrusive vanilla item registry
 * freezes at bootstrap), so the molds/inputs/outputs ride the loader's resolver seams
 * over distinct vanilla items — the recipe mechanics only compare identities.
 */
class GT6RecipesExtruderTest extends GTRecipesOfflineTestBase {

	private static final Item PLATE_MOLD = Items.IRON_INGOT;
	private static final Item ROD_MOLD = Items.GOLD_INGOT;
	private static final Item BLOCK = Items.IRON_BLOCK;
	private static final Item PLATE_OUT = Items.HEAVY_WEIGHTED_PRESSURE_PLATE;
	private static final Item ROD_OUT = Items.LIGHTNING_ROD;

	private static BiFunction<OreDictPrefix, OreDictMaterial, Item> sDefaultMaterialResolver;
	private static java.util.function.Function<GTMaterialItems.PrefixMaterial, ItemStack> sDefaultBlockResolver;
	private static java.util.function.Supplier<ItemStack> sDefaultPlateMold;
	private static java.util.function.Supplier<ItemStack> sDefaultRodMold;
	private static java.util.function.Predicate<ItemStack> sDefaultNotConsumable;

	@BeforeAll
	static void captureDefaults() {
		GTMaterialItems.initMaterials(); // the offline material universe (MT.init + OP.init)
		sDefaultMaterialResolver = GT6RecipesExtruder.sMaterialItemResolver;
		sDefaultBlockResolver = GT6RecipesExtruder.sBlockResolver;
		sDefaultPlateMold = GT6RecipesExtruder.sPlateMoldResolver;
		sDefaultRodMold = GT6RecipesExtruder.sRodMoldResolver;
		sDefaultNotConsumable = Recipe.sNotConsumable;
	}

	@AfterAll
	static void restoreDefaultsOnly() {
		GT6RecipesExtruder.sMaterialItemResolver = sDefaultMaterialResolver;
		GT6RecipesExtruder.sBlockResolver = sDefaultBlockResolver;
		GT6RecipesExtruder.sPlateMoldResolver = sDefaultPlateMold;
		GT6RecipesExtruder.sRodMoldResolver = sDefaultRodMold;
		Recipe.sNotConsumable = sDefaultNotConsumable;
	}

	@AfterEach
	void restoreSeams() {
		GT6RecipesExtruder.sMaterialItemResolver = sDefaultMaterialResolver;
		GT6RecipesExtruder.sBlockResolver = sDefaultBlockResolver;
		GT6RecipesExtruder.sPlateMoldResolver = sDefaultPlateMold;
		GT6RecipesExtruder.sRodMoldResolver = sDefaultRodMold;
		Recipe.sNotConsumable = sDefaultNotConsumable;
		GT6RecipeMaps.reset();
		GT6RecipesExtruder.resetForTest();
	}

	/** The fixture universe: iron/gold identity pairs over the vanilla item set. */
	private static void installFixtures() {
		GT6RecipesExtruder.sMaterialItemResolver = (aPrefix, aMaterial) -> {
			if (aMaterial != MT.Iron) return null;
			return aPrefix == OP.plate ? PLATE_OUT : aPrefix == OP.stick ? ROD_OUT : null;
		};
		GT6RecipesExtruder.sBlockResolver = aPair -> aPair.material() == MT.Iron ? new ItemStack(BLOCK, 1) : null;
		GT6RecipesExtruder.sPlateMoldResolver = () -> new ItemStack(PLATE_MOLD, 1);
		GT6RecipesExtruder.sRodMoldResolver = () -> new ItemStack(ROD_MOLD, 1);
		// the never-consumed fixture face: the two mold identities are the size-0 markers
		Recipe.sNotConsumable = aStack -> aStack != null && !aStack.isEmpty()
				&& (aStack.getItem() == PLATE_MOLD || aStack.getItem() == ROD_MOLD);
	}

	/** The fixture pour: exactly the iron pair (the resolver gates every other material). */
	private static int pour() {
		GT6RecipeMaps.init();
		GT6RecipesExtruder.load();
		assertNotNull(GT6RecipeMaps.EXTRUDER);
		return GT6RecipeMaps.EXTRUDER.mRecipeList.size();
	}

	/** The pour lands the two rows per material (plate + rod) with the :405/:407 constants. */
	@Test
	public void pourLandsPlateAndRodRowsWithUpstreamConstants() {
		installFixtures();
		assertEquals(2, pour(), "the iron fixture pair: the :405 plate row + the :407 rod row");
		Recipe tPlateRow = GT6RecipeMaps.EXTRUDER.mRecipeList.stream()
				.filter(aRecipe -> aRecipe.mOutputs[0].getItem() == PLATE_OUT).findFirst().orElse(null);
		Recipe tRodRow = GT6RecipeMaps.EXTRUDER.mRecipeList.stream()
				.filter(aRecipe -> aRecipe.mOutputs[0].getItem() == ROD_OUT).findFirst().orElse(null);
		assertNotNull(tPlateRow);
		assertNotNull(tRodRow);
		assertEquals(9, tPlateRow.mOutputs[0].getCount(), "the RM.java:405 face: 9 plates");
		assertEquals(18, tRodRow.mOutputs[0].getCount(), "the RM.java:407 face: 18 sticks");
		assertEquals(16, tPlateRow.mEUt, "the :405/:407 EUt column");
		assertEquals(32, tPlateRow.mDuration, "the :405/:407 duration column");
		assertEquals(16, tRodRow.mEUt);
		assertEquals(32, tRodRow.mDuration);
		assertTrue(tPlateRow.mCanBeBuffered, "the static rows are buffered (the upstream aCanBeBuffered T)");
		// the mold rides the row at the port's count-1 carrier — the input census is 2
		assertEquals(2, tPlateRow.mInputs.length, "block + mold");
		assertEquals(2, tRodRow.mInputs.length, "block + mold");
	}

	/**
	 * THE STATIC-ROW CROWN: the consume pass eats the block and leaves BOTH molds in their
	 * slot (the size-0 marker port — NOT a per-row boolean axis, the F/T-axis correction).
	 */
	@Test
	public void consumeEatsTheBlockAndLeavesBothMolds() {
		installFixtures();
		pour();
		for (Item tMoldItem : new Item[] {PLATE_MOLD, ROD_MOLD}) {
			Recipe tRow = GT6RecipeMaps.EXTRUDER.mRecipeList.stream()
					.filter(aRecipe -> aRecipe.mInputs.length == 2
							&& aRecipe.mInputs[1].getItem() == tMoldItem)
					.findFirst().orElse(null);
			assertNotNull(tRow, "the row for the " + tMoldItem);
			ItemStack tMold = new ItemStack(tMoldItem, 4);
			ItemStack tBlock = new ItemStack(BLOCK, 7);
			assertTrue(tRow.isRecipeInputEqual(true, false, null, tMold, tBlock), "the consume pass succeeds");
			assertEquals(4, tMold.getCount(), "the MOLD STAYS IN THE SLOT — " + tMoldItem);
			assertEquals(6, tBlock.getCount(), "the block is consumed");
		}
	}

	/** The load() re-pour after a bare map reset (the ADR-P18 canary, the Extruder face). */
	@Test
	public void loadRepoursAfterABareMapReset() {
		installFixtures();
		GT6RecipeMaps.reset();
		GT6RecipesExtruder.resetForTest(); // self-grounding
		assertEquals(2, pour(), "the first pour");
		GT6RecipeMaps.reset(); // the BARE reset — the poison constructor
		GT6RecipesExtruder.load();
		assertEquals(2, GT6RecipeMaps.EXTRUDER.mRecipeList.size(),
				"load() must truly re-pour after a bare reset() — the pour-flag retires WITH the generation");
	}

	/** The probe pass never mutates the inputs (the two-stage contract on the static rows). */
	@Test
	public void probePassLeavesTheInputsUntouched() {
		installFixtures();
		pour();
		Recipe tPlateRow = GT6RecipeMaps.EXTRUDER.mRecipeList.stream()
				.filter(aRecipe -> aRecipe.mOutputs[0].getItem() == PLATE_OUT).findFirst().orElse(null);
		assertNotNull(tPlateRow);
		ItemStack tMold = new ItemStack(PLATE_MOLD, 1);
		ItemStack tBlock = new ItemStack(BLOCK, 1);
		assertTrue(tPlateRow.isRecipeInputEqual(false, false, null, tMold, tBlock));
		assertEquals(1, tMold.getCount());
		assertEquals(1, tBlock.getCount());
		assertFalse(tPlateRow.isRecipeInputEqual(true, false, null, new ItemStack(Items.STICK, 1)),
				"a non-row input set does not match");
	}

	/** A material without both output items skips silently (the upstream mat() null drop). */
	@Test
	public void materialsWithoutOutputsSkipSilently() {
		installFixtures();
		GT6RecipesExtruder.sMaterialItemResolver = (aPrefix, aMaterial) -> aPrefix == OP.plate && aMaterial == MT.Iron ? PLATE_OUT : null;
		assertEquals(0, pour(), "the rod half never resolves → the iron pair skips whole (the +2 skip face)");
	}
}
