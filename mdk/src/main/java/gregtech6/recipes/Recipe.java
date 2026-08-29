/**
 * Copyright (c) 2025 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

package gregtech6.recipes;

import java.util.Arrays;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/**
 * GT6 Recipe record, minimal Furnace-level port of upstream
 * gregapi/recipes/Recipe.java (:662-683 fields, :796-818 isRecipeInputEqual,
 * :741-771 getOutputs).
 *
 * <p>Porting layer decision (ADR-P4 recipe layering): this record lives on the
 * mdk side because mInputs/mFluidInputs are ItemStack/FluidStack (MC types).
 * The upstream OreDict unification inside {@code checkStacksEqual} is replaced
 * by plain item + tag equality ({@link #isSameItemAndTag}); NBT-lenient matching
 * keeps the upstream {@code mNoNBTChecks || !recipeInput.hasTagCompound()} rule.
 * Output chances are fixed at 100% (Furnace has no probabilistic outputs), so
 * the mChances/mMaxChances arrays are not carried.
 *
 * <p><b>Consumer contract (pinned for p4-machine-oven):</b>
 * {@code RecipeMap.findRecipe} and {@code RecipeMapFurnace.findRecipe} only
 * LOOK UP — they never modify the passed inputs (decrease flag is always false
 * inside the search). Consuming is the machine's job:
 * {@code recipe.isRecipeInputEqual(true, false, aFluidInputs, aInputs)}
 * (upstream MultiTileEntityBasicMachine.java:725/:738 two-stage semantics:
 * aApply=false probes, aApply=true consumes).
 */
public class Recipe {
	/** If you want to change the Output, feel free to modify or even replace the whole ItemStack Array, for Inputs, please add a new Recipe, because of the HashMaps. */
	public ItemStack[] mInputs, mOutputs;
	/** If you want to change the Output, feel free to modify or even replace the whole ItemStack Array, for Inputs, please add a new Recipe, because of the HashMaps. */
	public FluidStack[] mFluidInputs, mFluidOutputs;

	public long mDuration, mEUt, mSpecialValue;

	/** Use this to just disable a specific Recipe. */
	public boolean mEnabled = true;
	/** If this Recipe is Fake and therefore doesn't get found by the findRecipe Function. */
	public boolean mFakeRecipe = false;
	/** If this Recipe can be stored inside a Machine in order to make Recipe searching more Efficient by trying the previously used Recipe first. In case you have a Recipe Map overriding things and returning one time use Recipes, you have to set this to F. */
	public boolean mCanBeBuffered = true;
	/** If this Recipe needs the Output Slots to be completely empty. Needed in case you have randomised Outputs */
	public boolean mNeedsEmptyOutput = false;
	/** If this Recipe is not supposed to check for Input NBT Values. */
	public boolean mNoNBTChecks = false;

	/**
	 * @param aCanBeBuffered if this Recipe may be cached by a Machine as mLastRecipe (upstream :886 passes F for the furnace-generated Recipes at RecipeMapFurnace.java:154).
	 */
	public Recipe(boolean aCanBeBuffered, @Nullable ItemStack[] aInputs, @Nullable ItemStack[] aOutputs, @Nullable FluidStack[] aFluidInputs, @Nullable FluidStack[] aFluidOutputs, long aDuration, long aEUt, long aSpecialValue) {
		mCanBeBuffered = aCanBeBuffered;
		mInputs = withoutTrailingNulls(aInputs);
		mOutputs = withoutTrailingNulls(aOutputs);
		mFluidInputs = aFluidInputs == null ? new FluidStack[0] : aFluidInputs;
		mFluidOutputs = aFluidOutputs == null ? new FluidStack[0] : aFluidOutputs;
		mDuration = aDuration;
		mEUt = aEUt;
		mSpecialValue = aSpecialValue;
	}

	/** Upstream Recipe.setNeedEmptyOut (Recipe.java:709). */
	public Recipe setNeedEmptyOut() {
		mNeedsEmptyOutput = true;
		return this;
	}

	public long getAbsoluteTotalPower() {
		return Math.abs(mEUt * mDuration);
	}

	/** Upstream Recipe.getOutputs (Recipe.java:749) with the random chance branches trimmed away: Furnace-level outputs are always 100%. */
	public ItemStack[] getOutputs() {
		return getOutputs(1);
	}

	/** @param aProcessCount multiplier for parallel processing (upstream ST.mul_ semantics). */
	public ItemStack[] getOutputs(int aProcessCount) {
		ItemStack[] rArray = new ItemStack[mOutputs.length];
		for (int i = 0; i < rArray.length; i++) {
			ItemStack tOutput = mOutputs[i];
			if (tOutput != null && !tOutput.isEmpty()) {
				rArray[i] = tOutput.copy();
				rArray[i].grow(tOutput.getCount() * (Math.max(1, aProcessCount) - 1));
			}
		}
		return rArray;
	}

	/** Upstream Recipe.getFluidOutputs (Recipe.java:735) — no chance processing involved. */
	public FluidStack[] getFluidOutputs() {
		return getFluidOutputs(1);
	}

	public FluidStack[] getFluidOutputs(int aProcessCount) {
		FluidStack[] rArray = new FluidStack[mFluidOutputs.length];
		for (int i = 0; i < rArray.length; i++) {
			FluidStack tOutput = mFluidOutputs[i];
			if (tOutput != null && !tOutput.isEmpty()) {
				rArray[i] = new FluidStack(tOutput, tOutput.getAmount() * Math.max(1, aProcessCount));
			}
		}
		return rArray;
	}

	/** Upstream deprecated two-arg convenience (Recipe.java:796). */
	@Deprecated
	public boolean isRecipeInputEqual(boolean aDecreaseStacksizeBySuccess, @Nullable FluidStack[] aFluidInputs, ItemStack... aInputs) {
		return isRecipeInputEqual(aDecreaseStacksizeBySuccess, false, aFluidInputs, aInputs);
	}

	/**
	 * Upstream Recipe.isRecipeInputEqual (Recipe.java:800-818).
	 *
	 * @param aDecreaseStacksizeBySuccess false = only probe the inputs (findRecipe path), true = consume them (machine apply path)
	 * @param aDontCheckStackSizes true = ignore stack sizes (used by the mLastRecipe fast path, which only checks item identity)
	 */
	public boolean isRecipeInputEqual(boolean aDecreaseStacksizeBySuccess, boolean aDontCheckStackSizes, @Nullable FluidStack[] aFluidInputs, ItemStack... aInputs) {
		if (mFluidInputs.length > 0 && (aFluidInputs == null || aFluidInputs.length < 1)) return false;
		if (mInputs.length > 0 && (aInputs == null || aInputs.length < 1)) return false;

		// Phase 1: probe everything first (upstream :804-810) — the consume phase only
		// runs once ALL inputs verified, so a failing recipe leaves the inputs untouched.
		for (FluidStack tFluid : mFluidInputs) if (tFluid != null && !tFluid.isEmpty()) {
			boolean temp = true;
			if (aFluidInputs != null) for (FluidStack aFluid : aFluidInputs) if (aFluid != null && !aFluid.isEmpty() && aFluid.isFluidEqual(tFluid) && (aDontCheckStackSizes || aFluid.getAmount() >= tFluid.getAmount())) {temp = false; break;}
			if (temp) return false;
		}

		if (!checkStacksEqual(false, aDontCheckStackSizes, aInputs)) return false;

		// Phase 2: consume (upstream :812-815).
		if (aDecreaseStacksizeBySuccess) {
			for (FluidStack tFluid : mFluidInputs) if (tFluid != null && !tFluid.isEmpty()) {
				for (FluidStack aFluid : aFluidInputs) if (aFluid != null && !aFluid.isEmpty() && aFluid.isFluidEqual(tFluid) && aFluid.getAmount() >= tFluid.getAmount()) {aFluid.shrink(tFluid.getAmount()); break;}
			}
			checkStacksEqual(true, false, aInputs);
		}

		return true;
	}

	/**
	 * Upstream Recipe.checkStacksEqual (Recipe.java:773-793). The OreDict manager
	 * equality is replaced by item + tag equality; NBT is skipped when the recipe
	 * input carries no tag (upstream {@code mNoNBTChecks || !tInput.hasTag()}).
	 */
	private boolean checkStacksEqual(boolean aDecreaseStacksizeBySuccess, boolean aDontCheckStackSizes, ItemStack... aInputs) {
		boolean[] tChecked = new boolean[aInputs.length];
		for (ItemStack tInput : mInputs) if (tInput != null && !tInput.isEmpty()) {
			boolean temp = true;
			// upstream: ignoreNBT = mNoNBTChecks || !tInput.hasTag()
			boolean tIgnoreNBT = mNoNBTChecks || !tInput.hasTag();
			for (int i = 0; i < aInputs.length; i++) if (!tChecked[i]) {
				ItemStack aInput = aInputs[i];
				if (aInput != null && !aInput.isEmpty()) {
					if ((aDontCheckStackSizes || aInput.getCount() >= tInput.getCount()) && isSameItemAndTag(aInput, tInput, tIgnoreNBT)) {
						if (aDecreaseStacksizeBySuccess) aInput.shrink(tInput.getCount());
						tChecked[i] = true;
						temp = false;
						break;
					}
				} else {
					tChecked[i] = true;
				}
			}
			if (temp) return false;
		}
		return true;
	}

	/** 1.20.1 equivalent of upstream OreDictManager.equal_(_, _, _, aIgnoreNBT). */
	private static boolean isSameItemAndTag(ItemStack aInput, ItemStack tInput, boolean aIgnoreNBT) {
		if (aIgnoreNBT) return aInput.getItem() == tInput.getItem();
		// ItemStack.isSameItemSameTags compares Objects.equals(tag, tag) (vanilla 1.20.1 :428-433).
		return ItemStack.isSameItemSameTags(aInput, tInput);
	}

	private static ItemStack[] withoutTrailingNulls(@Nullable ItemStack[] aArray) {
		if (aArray == null) return new ItemStack[0];
		int tLength = aArray.length;
		while (tLength > 0 && (aArray[tLength - 1] == null || aArray[tLength - 1].isEmpty())) tLength--;
		return Arrays.copyOf(aArray, tLength);
	}
}
