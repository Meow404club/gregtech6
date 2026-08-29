package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fluids.FluidStack;
import net.minecraft.world.level.material.Fluids;

/**
 * Upstream Recipe semantics: isRecipeInputEqual probe/consume dual mode
 * (Recipe.java:800-818), two-phase atomicity (:804-815), stack-size and NBT
 * matching rules (:773-793), fixed-100% getOutputs (:749 trim).
 */
class RecipeTest extends GTRecipesOfflineTestBase {

	private static final ItemStack[] SAND = {new ItemStack(Items.SAND, 8)};
	private static final ItemStack[] GLASS = {new ItemStack(Items.GLASS, 1)};

	@Test
	void probeModeLeavesInputsUntouched() {
		Recipe tRecipe = new Recipe(true, SAND, GLASS, null, null, 16, 16, 0);
		ItemStack tInput = new ItemStack(Items.SAND, 8);

		assertTrue(tRecipe.isRecipeInputEqual(false, false, null, tInput));
		assertEquals(8, tInput.getCount(), "probe (aApply=F) must not consume");
	}

	@Test
	void consumeModeShrinksInputs() {
		Recipe tRecipe = new Recipe(true, SAND, GLASS, null, null, 16, 16, 0);
		ItemStack tInput = new ItemStack(Items.SAND, 16);

		assertTrue(tRecipe.isRecipeInputEqual(true, false, null, tInput));
		assertEquals(8, tInput.getCount(), "consume (aApply=T) shrinks by the recipe amount (8)");
	}

	@Test
	void consumeIsAtomicWhenLaterInputFails() {
		// upstream :804-815: matching is fully probed before anything is consumed.
		Recipe tRecipe = new Recipe(true,
				new ItemStack[] {new ItemStack(Items.SAND), new ItemStack(Items.GLASS)}, GLASS,
				new FluidStack[] {new FluidStack(Fluids.LAVA, 100)}, null, 16, 16, 0);
		FluidStack tLava = new FluidStack(Fluids.LAVA, 500);
		ItemStack tSand = new ItemStack(Items.SAND, 4);
		ItemStack tDirt = new ItemStack(Items.DIRT, 4);

		assertFalse(tRecipe.isRecipeInputEqual(true, false, new FluidStack[] {tLava}, tSand, tDirt));
		assertEquals(500, tLava.getAmount(), "fluid must stay untouched when a later item fails");
		assertEquals(4, tSand.getCount(), "item must stay untouched when a later item fails");
	}

	@Test
	void dontCheckStackSizesIgnoresAmounts() {
		// the mLastRecipe fast path probes with aDontCheckStackSizes=T (upstream :487).
		Recipe tRecipe = new Recipe(true, SAND, GLASS, null, null, 16, 16, 0);
		ItemStack tSingle = new ItemStack(Items.SAND, 1);

		assertTrue(tRecipe.isRecipeInputEqual(false, true, null, tSingle));
		assertFalse(tRecipe.isRecipeInputEqual(false, false, null, tSingle), "with size checking, 1 sand cannot cover a 8-sand recipe");
	}

	@Test
	void nbtLessRecipeInputMatchesTaggedMachineInput() {
		// upstream :780 ignoreNBT = mNoNBTChecks || !tInput.hasTagCompound(): an NBT-less
		// recipe input matches any NBT on the machine input.
		Recipe tRecipe = new Recipe(true, SAND, GLASS, null, null, 16, 16, 0);
		ItemStack tTagged = new ItemStack(Items.SAND, 8);
		tTagged.setTag(new CompoundTag());

		assertTrue(tRecipe.isRecipeInputEqual(false, false, null, tTagged));
	}

	@Test
	void taggedRecipeInputRequiresEqualTag() {
		ItemStack tTaggedSand = new ItemStack(Items.SAND, 8);
		CompoundTag tTag = new CompoundTag();
		tTag.putString("foo", "bar");
		tTaggedSand.setTag(tTag);
		Recipe tRecipe = new Recipe(true, new ItemStack[] {tTaggedSand}, GLASS, null, null, 16, 16, 0);

		ItemStack tOtherTag = new ItemStack(Items.SAND, 8);
		CompoundTag tTag2 = new CompoundTag();
		tTag2.putString("foo", "bar");
		tOtherTag.setTag(tTag2);

		assertTrue(tRecipe.isRecipeInputEqual(false, false, null, tOtherTag.copy()), "equal tags match");
		tOtherTag.getTag().putString("foo", "nope");
		assertFalse(tRecipe.isRecipeInputEqual(false, false, null, tOtherTag), "different tags do not match");
	}

	@Test
	void noNBTChecksFlagSkipsTagComparison() {
		ItemStack tTaggedSand = new ItemStack(Items.SAND, 8);
		CompoundTag tTag = new CompoundTag();
		tTag.putString("foo", "bar");
		tTaggedSand.setTag(tTag);
		Recipe tRecipe = new Recipe(true, new ItemStack[] {tTaggedSand}, GLASS, null, null, 16, 16, 0);
		tRecipe.mNoNBTChecks = true;

		ItemStack tUntagged = new ItemStack(Items.SAND, 8);
		assertTrue(tRecipe.isRecipeInputEqual(false, false, null, tUntagged), "mNoNBTChecks matches item identity only");
	}

	@Test
	void fluidMatchingUsesIsFluidEqualAndAmount() {
		Recipe tRecipe = new Recipe(true, new ItemStack[] {new ItemStack(Items.SAND)}, GLASS,
				new FluidStack[] {new FluidStack(Fluids.WATER, 100)}, null, 16, 16, 0);

		assertTrue(tRecipe.isRecipeInputEqual(false, false, new FluidStack[] {new FluidStack(Fluids.WATER, 100)}, new ItemStack(Items.SAND, 1)));
		assertTrue(tRecipe.isRecipeInputEqual(false, false, new FluidStack[] {new FluidStack(Fluids.WATER, 1000)}, new ItemStack(Items.SAND, 1)), "more than enough is fine");
		assertFalse(tRecipe.isRecipeInputEqual(false, false, new FluidStack[] {new FluidStack(Fluids.WATER, 99)}, new ItemStack(Items.SAND, 1)), "not enough is not");
		assertFalse(tRecipe.isRecipeInputEqual(false, false, new FluidStack[] {new FluidStack(Fluids.LAVA, 100)}, new ItemStack(Items.SAND, 1)), "wrong fluid is not");
		assertFalse(tRecipe.isRecipeInputEqual(false, false, null, new ItemStack(Items.SAND, 1)), "no fluids at all is not");
	}

	@Test
	void itemInputsGuardAgainstEmptyCallers() {
		Recipe tRecipe = new Recipe(true, SAND, GLASS, null, null, 16, 16, 0);
		assertFalse(tRecipe.isRecipeInputEqual(false, false, null), "empty caller inputs");
		assertTrue(new Recipe(true, null, null, new FluidStack[] {new FluidStack(Fluids.WATER, 100)}, null, 16, 16, 0)
				.isRecipeInputEqual(false, false, new FluidStack[] {new FluidStack(Fluids.WATER, 100)}), "fluid-only recipe without item inputs");
	}

	@Test
	void trailingNullInputsAreTrimmed() {
		Recipe tRecipe = new Recipe(true, new ItemStack[] {new ItemStack(Items.SAND), null, null}, new ItemStack[] {new ItemStack(Items.GLASS), null}, null, null, 16, 16, 0);
		assertEquals(1, tRecipe.mInputs.length);
		assertEquals(1, tRecipe.mOutputs.length);
		assertTrue(tRecipe.isRecipeInputEqual(false, false, null, new ItemStack(Items.SAND)));
	}

	@Test
	void getOutputsAreFixedHundredPercent() {
		Recipe tRecipe = new Recipe(true, SAND, new ItemStack[] {new ItemStack(Items.GLASS, 2)}, null, null, 16, 16, 0);

		ItemStack[] tOutputs = tRecipe.getOutputs();
		assertEquals(1, tOutputs.length);
		assertEquals(2, tOutputs[0].getCount());
		assertEquals(Items.GLASS, tOutputs[0].getItem());
		assertNotSame(GLASS[0], tOutputs[0], "outputs must be copies");

		assertEquals(6, tRecipe.getOutputs(3)[0].getCount(), "process count multiplies the output size");
		assertEquals(0, tRecipe.getFluidOutputs().length);
	}

	@Test
	void absoluteTotalPower() {
		assertEquals(256, new Recipe(true, SAND, GLASS, null, null, 16, 16, 0).getAbsoluteTotalPower());
	}
}
