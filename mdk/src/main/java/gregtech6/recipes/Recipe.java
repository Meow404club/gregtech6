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
import java.util.Random;

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
 * Output chances are carried since p8-recipe-chances-orechain: {@link #mChances} is a
 * 10000-based per-output-slot chance array aligned to {@link #mOutputs} (upstream
 * Recipe.java:666 carries {@code mChances, mMaxChances}; {@code mMaxChances} is NOT
 * ported — its every read defaults to 10000, upstream Recipe.java:687, so the port
 * folds that constant into the chance semantics, declared deviation).
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
	/**
	 * Per-output-slot chance, 10000 = 100% (upstream Recipe.java:665-666). {@code null} or
	 * an all-10000 array is behaviourally identical to the pre-chances port (every
	 * deterministic row of the existing loaders). Array length is aligned to {@link #mOutputs}
	 * by the chances-bearing constructor; indices past a shorter array read as 10000
	 * (upstream getOutputChance :686 out-of-bounds → getMaxChance :687 → 10000, with
	 * mMaxChances folded to the constant). A chance of 0 yields NO output — declared
	 * deviation: upstream Recipe.java:765-767 passes chance==0 through as an unconditional
	 * output (masked in practice by the ctor's {@code chances[i] <= 0 → 10000} rewrite,
	 * upstream Recipe.java:906, which the port does NOT replicate).
	 */
	@Nullable
	public long[] mChances;

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
		this(aCanBeBuffered, aInputs, aOutputs, aFluidInputs, aFluidOutputs, aDuration, aEUt, aSpecialValue, null);
	}

	/**
	 * Chances-bearing constructor, the 8-param form with {@code aChances} appended at the
	 * tail (upstream carries chances as the 7th parameter of the 12-arg ctor, the shape the
	 * RecipeMapHandlerCrushing.java:137 call site uses; the port appends instead of
	 * re-matching positions). Upstream Recipe.java:893 pads a short chances array up to the
	 * output length and Recipe.java:906 rewrites every {@code chance <= 0} to 10000 — the
	 * port does neither: a short array keeps reading 10000 past its end (same observable
	 * result via the :686/:687 default) while a 0 chance stays 0 and yields NO output (the
	 * declared deviation documented on {@link #mChances}).
	 *
	 * @param aChances per-output chances, 10000 = 100%; aligned (trimmed) to the trimmed
	 *        output array length; {@code null} = the deterministic pre-chances behaviour.
	 */
	public Recipe(boolean aCanBeBuffered, @Nullable ItemStack[] aInputs, @Nullable ItemStack[] aOutputs, @Nullable FluidStack[] aFluidInputs, @Nullable FluidStack[] aFluidOutputs, long aDuration, long aEUt, long aSpecialValue, @Nullable long[] aChances) {
		mCanBeBuffered = aCanBeBuffered;
		mInputs = withoutTrailingNulls(aInputs);
		mOutputs = withoutTrailingNulls(aOutputs);
		mFluidInputs = aFluidInputs == null ? new FluidStack[0] : aFluidInputs;
		mFluidOutputs = aFluidOutputs == null ? new FluidStack[0] : aFluidOutputs;
		mDuration = aDuration;
		mEUt = aEUt;
		mSpecialValue = aSpecialValue;
		if (aChances == null) {
			mChances = null;
		} else {
			// align to the trailing-null-trimmed outputs ("same length as mOutputs"); a
			// shorter array stays short — reads past its end default to 10000 (upstream :686).
			mChances = Arrays.copyOf(aChances, Math.min(aChances.length, mOutputs.length));
		}
	}

	/** Upstream Recipe.setNeedEmptyOut (Recipe.java:709). */
	public Recipe setNeedEmptyOut() {
		mNeedsEmptyOutput = true;
		return this;
	}

	public long getAbsoluteTotalPower() {
		return Math.abs(mEUt * mDuration);
	}

	/**
	 * Deterministic convenience overload (upstream Recipe.java:741 — the pre-chances port
	 * shape). Kept for the existing machine call sites (compile-zero-change); on a
	 * chances-bearing row it resolves probabilistic slots at FULL certainty and chance-0
	 * slots to nothing — use {@link #getOutputs(Random, int)} for real chance semantics.
	 */
	public ItemStack[] getOutputs() {
		return getOutputs(1);
	}

	/** Deterministic convenience overload (upstream Recipe.java:745-747 shape), see {@link #getOutputs()}. */
	public ItemStack[] getOutputs(int aProcessCount) {
		ItemStack[] rArray = new ItemStack[mOutputs.length];
		for (int i = 0; i < rArray.length; i++) {
			ItemStack tOutput = mOutputs[i];
			if (tOutput == null || tOutput.isEmpty()) continue;
			long tChance = outputChance(i);
			if (tChance <= 0) continue; // declared deviation: upstream :765-767 would pass through
			// >= 10000 = the deterministic whole-stack branch; 0 < chance < 10000 = the
			// deterministic convenience reading (every Bernoulli trial succeeds).
			rArray[i] = tOutput.copy();
			rArray[i].grow(tOutput.getCount() * (Math.max(1, aProcessCount) - 1));
		}
		return rArray;
	}

	/**
	 * Upstream Recipe.getOutputs(Random, int) (Recipe.java:749-771). For every output slot:
	 * chance &gt;= 10000 → the deterministic whole stack × processCount (:758-759, the
	 * behaviour of every pre-chances row); 0 &lt; chance &lt; 10000 → per-unit Bernoulli
	 * sampling, {@code random.nextInt(10000) < chance} per unit of
	 * {@code stackSize × processCount} with unit-by-unit accumulation (:761-763); chance
	 * &lt;= 0 → NO output (declared deviation — upstream :765-767 passes the slot through,
	 * a bug the ctor's :906 rewrite used to mask; the port keeps 0 = never).
	 *
	 * @param aRandom injected RNG; {@code null} → a fresh {@code Random} (upstream :751).
	 *        The upstream RNGSUS singleton is not ported.
	 */
	public ItemStack[] getOutputs(@Nullable Random aRandom, int aProcessCount) {
		Random tRandom = aRandom == null ? new Random() : aRandom; // upstream :751
		ItemStack[] rArray = new ItemStack[mOutputs.length];
		for (int i = 0; i < rArray.length; i++) {
			ItemStack tOutput = mOutputs[i];
			if (tOutput == null || tOutput.isEmpty()) continue;
			long tChance = outputChance(i);
			if (tChance <= 0) continue; // declared deviation: upstream :765-767 would pass through
			if (tChance >= 10000) {
				rArray[i] = tOutput.copy();
				rArray[i].grow(tOutput.getCount() * (Math.max(1, aProcessCount) - 1)); // upstream :758-759 ST.mul_
			} else {
				for (int j = 0, k = tOutput.getCount() * Math.max(1, aProcessCount); j < k; j++) {
					if (tRandom.nextInt(10000) < tChance) { // upstream :761, tMax folded to 10000
						if (rArray[i] == null) rArray[i] = tOutput.copyWithCount(1); else rArray[i].grow(1); // upstream :762 unit accumulation
					}
				}
			}
		}
		return rArray;
	}

	/**
	 * Upstream getOutputChance :686 with {@code mMaxChances} folded to the constant 10000
	 * (upstream getMaxChance :687 returns 10000 for every out-of-bounds/default read —
	 * mMaxChances is not ported, declared deviation).
	 */
	private long outputChance(int aIndex) {
		if (mChances == null || aIndex < 0 || aIndex >= mChances.length) return 10000;
		return mChances[aIndex];
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
			//? if forge {
			boolean tIgnoreNBT = mNoNBTChecks || !tInput.hasTag();
			//?} else {
			/*boolean tIgnoreNBT = mNoNBTChecks || tInput.getComponentsPatch().isEmpty();
			 *///?}
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
		//? if forge {
		return ItemStack.isSameItemSameTags(aInput, tInput);
		//?} else {
		/*return ItemStack.isSameItemSameComponents(aInput, tInput);
		 *///?}
	}

	private static ItemStack[] withoutTrailingNulls(@Nullable ItemStack[] aArray) {
		if (aArray == null) return new ItemStack[0];
		int tLength = aArray.length;
		while (tLength > 0 && (aArray[tLength - 1] == null || aArray[tLength - 1].isEmpty())) tLength--;
		return Arrays.copyOf(aArray, tLength);
	}
}
