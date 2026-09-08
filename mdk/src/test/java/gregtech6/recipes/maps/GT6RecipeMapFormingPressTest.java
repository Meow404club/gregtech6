package gregtech6.recipes.maps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Function;

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
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.GTRecipesOfflineTestBase;
import gregtech6.recipes.Recipe;

/**
 * The Forming Press map, offline (task p26-w1-press-extruder-molds): the findRecipe
 * override's arms over fixture seams — the stored-rows-first priority, the two-slot gate,
 * the row0 FORMING arm ([mold, blockIngot] → 9 plates / 18 sticks, either slot order) and
 * the MOLD-NOT-CONSUMED crown (the two-stage isRecipeInputEqual consume skips the mold
 * through {@code Recipe.sNotConsumable} — the archaeology conclusion's offline pin: the
 * upstream size-0 marker rides BOTH the plate and the rod rows, the RM.java:405/:407
 * fifth-boolean F/T axis being aLogErrors, NOT a not-consumable flag).
 *
 * <p>Fixtures are EXISTING vanilla items (mod items are not constructible offline — the
 * intrusive vanilla item registry freezes at bootstrap, the GT6RecipesShCLTest
 * convention); the three seams ({@code sMoldShape}/{@code sBlockMaterial}/
 * {@code sOutputResolver}) are swapped and restored per test.
 */
class GT6RecipeMapFormingPressTest extends GTRecipesOfflineTestBase {

	private static final Item PLATE_MOLD = Items.IRON_INGOT;
	private static final Item ROD_MOLD = Items.GOLD_INGOT;
	private static final Item BLOCK = Items.IRON_BLOCK;
	private static final Item PLATE_OUT = Items.HEAVY_WEIGHTED_PRESSURE_PLATE;
	private static final Item ROD_OUT = Items.LIGHTNING_ROD;
	private static final Item NON_MOLD = Items.STICK;

	private static Function<ItemStack, OreDictPrefix> sDefaultMoldShape;
	private static Function<ItemStack, OreDictMaterial> sDefaultBlockMaterial;
	private static java.util.function.BiFunction<OreDictPrefix, OreDictMaterial, Item> sDefaultOutput;
	private static java.util.function.Predicate<ItemStack> sDefaultNotConsumable;

	@BeforeAll
	static void captureDefaults() {
		GTMaterialItems.initMaterials(); // the offline material universe (MT.init + OP.init)
		sDefaultMoldShape = GT6RecipeMapFormingPress.sMoldShape;
		sDefaultBlockMaterial = GT6RecipeMapFormingPress.sBlockMaterial;
		sDefaultOutput = GT6RecipeMapFormingPress.sOutputResolver;
		sDefaultNotConsumable = Recipe.sNotConsumable;
	}

	@AfterAll
	static void restoreDefaultsOnly() {
		// the production defaults back on the map seams (the per-test restores keep them
		// through the suite; this is the belt to the suspenders)
		GT6RecipeMapFormingPress.sMoldShape = sDefaultMoldShape;
		GT6RecipeMapFormingPress.sBlockMaterial = sDefaultBlockMaterial;
		GT6RecipeMapFormingPress.sOutputResolver = sDefaultOutput;
		Recipe.sNotConsumable = sDefaultNotConsumable;
	}

	@AfterEach
	void restoreSeams() {
		GT6RecipeMapFormingPress.sMoldShape = sDefaultMoldShape;
		GT6RecipeMapFormingPress.sBlockMaterial = sDefaultBlockMaterial;
		GT6RecipeMapFormingPress.sOutputResolver = sDefaultOutput;
		Recipe.sNotConsumable = sDefaultNotConsumable;
		GT6RecipeMaps.reset();
	}

	/** The fixture universe: the two mold identities + the blockIngot carrier + the outputs. */
	private static void installFixtures() {
		GT6RecipeMapFormingPress.sMoldShape = aStack -> {
			if (aStack == null || aStack.isEmpty()) return null;
			if (aStack.getItem() == PLATE_MOLD) return OP.plate;
			if (aStack.getItem() == ROD_MOLD) return OP.stick;
			return null;
		};
		GT6RecipeMapFormingPress.sBlockMaterial = aStack -> aStack != null && !aStack.isEmpty() && aStack.getItem() == BLOCK ? MT.Iron : null;
		GT6RecipeMapFormingPress.sOutputResolver = (aShape, aMaterial) -> {
			if (aMaterial != MT.Iron) return null;
			return aShape == OP.plate ? PLATE_OUT : aShape == OP.stick ? ROD_OUT : null;
		};
		// the never-consumed fixture face: the two mold identities are the size-0 markers
		Recipe.sNotConsumable = aStack -> aStack != null && !aStack.isEmpty()
				&& (aStack.getItem() == PLATE_MOLD || aStack.getItem() == ROD_MOLD);
	}

	/** The PRESS map instance (the generation form — the test never constructs the map directly). */
	private static GT6RecipeMapFormingPress press() {
		GT6RecipeMaps.init();
		return GT6RecipeMaps.PRESS;
	}

	/** The row0 FORMING arm: [plate mold, block] → 9 plates at EUt 16, duration 32, one-time. */
	@Test
	public void formingArmSynthesizesThePlateRow() {
		installFixtures();
		Recipe tRecipe = press().findRecipe(null, Long.MAX_VALUE, null, null,
				new ItemStack(PLATE_MOLD, 1), new ItemStack(BLOCK, 1));
		assertNotNull(tRecipe, "the arm synthesizes the plate forming row");
		assertEquals(1, tRecipe.mOutputs.length, "single-output row");
		assertEquals(PLATE_OUT, tRecipe.mOutputs[0].getItem());
		assertEquals(9, tRecipe.mOutputs[0].getCount(), "the RM.java:405 plate row face: 9 plates");
		assertEquals(16, tRecipe.mEUt, "the :405 EUt column");
		assertEquals(32, tRecipe.mDuration, "the :405 duration column");
		assertFalse(tRecipe.mCanBeBuffered, "the one-time synthesis (the upstream :52 aRecipe(F, F, F) form)");
	}

	/** The arm is slot-order symmetric (the upstream :49/:54 pair). */
	@Test
	public void formingArmIsSlotOrderSymmetric() {
		installFixtures();
		Recipe tPlateFirst = press().findRecipe(null, Long.MAX_VALUE, null, null,
				new ItemStack(PLATE_MOLD, 1), new ItemStack(BLOCK, 1));
		Recipe tBlockFirst = press().findRecipe(null, Long.MAX_VALUE, null, null,
				new ItemStack(BLOCK, 1), new ItemStack(PLATE_MOLD, 1));
		assertNotNull(tPlateFirst);
		assertNotNull(tBlockFirst);
		assertEquals(PLATE_OUT, tBlockFirst.mOutputs[0].getItem(), "the same row either way");
		assertEquals(9, tBlockFirst.mOutputs[0].getCount());
	}

	/** The rod arm: [rod mold, block] → 18 sticks (the RM.java:407 face). */
	@Test
	public void formingArmSynthesizesTheRodRow() {
		installFixtures();
		Recipe tRecipe = press().findRecipe(null, Long.MAX_VALUE, null, null,
				new ItemStack(ROD_MOLD, 1), new ItemStack(BLOCK, 1));
		assertNotNull(tRecipe, "the arm synthesizes the rod forming row");
		assertEquals(ROD_OUT, tRecipe.mOutputs[0].getItem());
		assertEquals(18, tRecipe.mOutputs[0].getCount(), "the RM.java:407 rod row face: 18 sticks");
	}

	/**
	 * THE CROWN (the archaeology conclusion's offline pin): the synthesized recipe's
	 * consume pass eats the BLOCK and leaves the MOLD in its slot — both the plate AND the
	 * rod rows (the upstream size-0 marker semantics, NOT a per-row boolean axis).
	 */
	@Test
	public void consumeEatsTheBlockAndLeavesBothMoldsInTheSlot() {
		installFixtures();
		for (Item tMoldItem : new Item[] {PLATE_MOLD, ROD_MOLD}) {
			ItemStack tMold = new ItemStack(tMoldItem, 3);
			ItemStack tBlock = new ItemStack(BLOCK, 5);
			Recipe tRecipe = press().findRecipe(null, Long.MAX_VALUE, null, null,
					tMold.copy(), tBlock.copy());
			assertNotNull(tRecipe);
			assertTrue(tRecipe.isRecipeInputEqual(true, false, null, tMold, tBlock),
					"the consume pass succeeds");
			assertEquals(3, tMold.getCount(), "the MOLD STAYS IN THE SLOT (the size-0 marker port) — " + tMoldItem);
			assertEquals(4, tBlock.getCount(), "the block is consumed (the :47 consumed slot)");
		}
	}

	/** The probe pass (aDecreaseStacksizeBySuccess false) never mutates anything. */
	@Test
	public void probePassLeavesTheInputsUntouched() {
		installFixtures();
		ItemStack tMold = new ItemStack(PLATE_MOLD, 2);
		ItemStack tBlock = new ItemStack(BLOCK, 2);
		Recipe tRecipe = press().findRecipe(null, Long.MAX_VALUE, null, null, tMold, tBlock);
		assertNotNull(tRecipe);
		assertTrue(tRecipe.isRecipeInputEqual(false, false, null, tMold, tBlock));
		assertEquals(2, tMold.getCount());
		assertEquals(2, tBlock.getCount());
	}

	/** The stored-rows-first priority: a static row wins over the dynamic arm (the upstream :46). */
	@Test
	public void storedRowsWinOverTheDynamicArm() {
		installFixtures();
		GT6RecipeMapFormingPress tMap = press();
		Recipe tStored = new Recipe(true,
				new ItemStack[] {new ItemStack(NON_MOLD, 1)},
				new ItemStack[] {new ItemStack(Items.BRICK, 1)},
				null, null, 64, 16, 0);
		tMap.addRecipe(tStored);
		Recipe tFound = tMap.findRecipe(null, Long.MAX_VALUE, null, null, new ItemStack(NON_MOLD, 1));
		assertNotNull(tFound);
		assertEquals(Items.BRICK, tFound.mOutputs[0].getItem(), "the stored row matched (not a dynamic synthesis)");
	}

	/** The two-slot gate: the arm reads exactly the first two inputs (the upstream :47 form). */
	@Test
	public void twoSlotGateReadsOnlyTheFirstTwoInputs() {
		installFixtures();
		GT6RecipeMapFormingPress tMap = press();
		// mold and block in slots 2 and 3 (index 1 and 2): only ONE of them is in the arm's
		// read window, so the gate fails and findRecipe answers null
		assertNull(tMap.findRecipe(null, Long.MAX_VALUE, null, null,
				new ItemStack(NON_MOLD, 1), new ItemStack(PLATE_MOLD, 1), new ItemStack(BLOCK, 1)),
				"the arm reads inputs[0]/[1] only — a mold at index 1 with a non-mold at index 0 gates out");
		// a single input gates out too
		assertNull(tMap.findRecipe(null, Long.MAX_VALUE, null, null, new ItemStack(PLATE_MOLD, 1)));
		assertNull(tMap.findRecipe(null, Long.MAX_VALUE, null, null));
	}

	/** Non-mold inputs and non-blockIngot carriers gate out (no synthesis). */
	@Test
	public void nonMoldAndNonBlockInputsGateOut() {
		installFixtures();
		GT6RecipeMapFormingPress tMap = press();
		// two non-mold stacks: no mold identity → no synthesis
		assertNull(tMap.findRecipe(null, Long.MAX_VALUE, null, null,
				new ItemStack(NON_MOLD, 1), new ItemStack(Items.BRICK, 1)));
		// a mold with a non-block carrier (STICK is not a blockIngot under the fixture)
		assertNull(tMap.findRecipe(null, Long.MAX_VALUE, null, null,
				new ItemStack(PLATE_MOLD, 1), new ItemStack(NON_MOLD, 1)));
	}

	/** An unregistered (shape, material) output gates out (the upstream mat() null drop). */
	@Test
	public void unregisteredOutputGatesOut() {
		installFixtures();
		GT6RecipeMapFormingPress.sOutputResolver = (aShape, aMaterial) -> null;
		assertNull(press().findRecipe(null, Long.MAX_VALUE, null, null,
				new ItemStack(PLATE_MOLD, 1), new ItemStack(BLOCK, 1)));
	}
}
