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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

/**
 * GT6 RecipeMap shell, minimal port of the inner class upstream
 * gregapi/recipes/Recipe.java:49-140.
 *
 * <p>Trimmed against upstream (documented deviations for the Furnace-level port):
 * <ul>
 * <li>no mRecipeItemMap/mRecipeFluidMap/mMinInputTankSizes — {@code mRecipeList}
 * is scanned linearly by {@link #findRecipe} (upstream findRecipeInternal :469-551
 * uses three hash indexes; the Furnace map keeps no recipe stock at all and future
 * recipe-bearing maps are fine at this scale);</li>
 * <li>no MineTweaker detect/reInit (:491-494), no RecipeMapHandlers (:526-542),
 * no config file, no NEI fields, no GUIPath registration into the localizer;</li>
 * <li>{@code RECIPE_MAPS} gained a {@link #reset()} (P1 registry discipline:
 * registries must be re-initializable per JVM generation — upstream has no reset
 * because 1.7.10 never reloads the mod class space).</li>
 * </ul>
 */
public class RecipeMap {
	/** RecipeMap-HashMap so that Machines can store their corresponding Recipe Lists as String NBT. (upstream :51) */
	public static final Map<String, RecipeMap> RECIPE_MAPS = new HashMap<>();

	/** Port-only reset hook (P1 registry discipline): drops every registered map so a fresh init re-registers cleanly. */
	public static void reset() {
		RECIPE_MAPS.clear();
	}

	/** The List of all Recipes. (upstream :68) */
	public final Collection<Recipe> mRecipeList;
	/** String used as an unlocalised Name. (upstream :72) */
	public final String mNameInternal;
	/** String used as a localised Name. (upstream :74) */
	public final String mNameLocal, mNameLocalUnderscored;
	/** String used for the Recipe Lists; if null it falls back to the unlocalised Name. (upstream :76/:123) */
	public final String mNameNEI;
	/** GUI used for the Recipe Display, usually the GUI of the Machine itself, auto-attaches ".png". (upstream :78/:124) */
	public final String mGUIPath;
	public final byte mProgressBarDirection, mProgressBarAmount;
	/** Slot constants (upstream :81). */
	public final int mInputItemsCount, mOutputItemsCount, mInputFluidCount, mOutputFluidCount, mMinimalInputItems, mMinimalInputFluids, mMinimalInputs;
	public final long mPower;

	public RecipeMap(@Nullable Collection<Recipe> aRecipeList, String aNameInternal, String aNameLocal, @Nullable String aNameNEI, long aProgressBarDirection, long aProgressBarAmount, String aNEIGUIPath, long aInputItemsCount, long aOutputItemsCount, long aMinimalInputItems, long aInputFluidCount, long aOutputFluidCount, long aMinimalInputFluids, long aMinimalInputs, long aPower) {
		mRecipeList = (aRecipeList == null ? new HashSet<>() : aRecipeList);
		mNameInternal = aNameInternal;
		mNameLocal = aNameLocal;
		StringBuilder tBuilder = new StringBuilder(mNameLocal.length());
		for (char tChar : mNameLocal.toCharArray()) {
			if (tChar == '(' || tChar == ')' || tChar == '[' || tChar == ']' || tChar == '{' || tChar == '}' || tChar == '"' || tChar == '\'' || tChar == '<' || tChar == '>' || tChar == '°' || tChar == '~' || tChar == '$' || tChar == '%' || tChar == '#' || tChar == '+' || tChar == '*' || tChar == '§' || tChar == '!' || tChar == '?' || tChar == '.' || tChar == ',' || tChar == ':' || tChar == ';') continue;
			if (tChar == ' ' || tChar == '-' || tChar == '=' || tChar == '&' || tChar == '^' || tChar == '|' || tChar == '/' || tChar == '\\') tBuilder.append('_'); else tBuilder.append(tChar);
		}
		mNameLocalUnderscored = tBuilder.toString();
		mNameNEI = aNameNEI == null ? mNameInternal : aNameNEI;
		mGUIPath = aNEIGUIPath.endsWith(".png") ? aNEIGUIPath : aNEIGUIPath + ".png";
		mProgressBarDirection = (byte)aProgressBarDirection;
		mProgressBarAmount = (byte)aProgressBarAmount;
		mPower = aPower;
		mMinimalInputItems = (int)aMinimalInputItems;
		mInputItemsCount = (int)Math.max(aInputItemsCount, mMinimalInputItems);
		mOutputItemsCount = (int)aOutputItemsCount;
		mMinimalInputFluids = (int)aMinimalInputFluids;
		mInputFluidCount = (int)Math.max(aInputFluidCount, mMinimalInputFluids);
		mOutputFluidCount = (int)aOutputFluidCount;
		mMinimalInputs = (int)aMinimalInputs;
		if (RECIPE_MAPS.containsKey(mNameInternal)) throw new IllegalArgumentException("Recipe Map Name already exists: " + mNameInternal);
		RECIPE_MAPS.put(mNameInternal, this);
	}

	/**
	 * Registers a Recipe into this Map. Upstream Recipe.add() feeds three hash
	 * indexes; the linear-scan port appends to {@code mRecipeList} directly.
	 *
	 * <p><b>Double-empty guard (p8-recipe-chances-orechain)</b>: a recipe with neither item
	 * nor fluid inputs is REJECTED — not added to {@code mRecipeList}, no exception thrown,
	 * {@code null} returned. Upstream is structurally immune to these ghost recipes at the
	 * index layer: {@code addToItemMap} (upstream Recipe.java:632-640) never buckets a
	 * recipe without inputs, so {@code findRecipeInternal} can never match one. This port
	 * scans {@code mRecipeList} linearly and {@code Recipe.checkStacksEqual} passes vacuously
	 * over an empty {@code mInputs} (Recipe.java:775 upstream — no required inputs = every
	 * caller-side input set matches), so an empty-input row would match EVERY lookup. The
	 * invariant "every stored recipe has at least one input leg" must therefore be enforced
	 * at the registration layer here — the upstream-layering equivalent. Rejections are not
	 * logged here (the map carries no logger; the loaders' skip-and-count pour statistics
	 * cover the audit trail, GT6RecipesCokeOven/GT6RecipesShCL precedent). Returning
	 * {@code null} is compatible with the existing call chain (loaders treat a null
	 * {@code addRecipe} result as a drop, the upstream {@code null != addRecipe(...)}
	 * idiom).
	 */
	@Nullable
	public Recipe addRecipe(@Nullable Recipe aRecipe) {
		if (aRecipe == null) return null;
		if (aRecipe.mInputs.length == 0 && aRecipe.mFluidInputs.length == 0) return null; // ghost-recipe guard, see javadoc
		if (aRecipe.mEnabled && !aRecipe.mFakeRecipe) mRecipeList.add(aRecipe);
		return aRecipe;
	}

	/**
	 * {@code aSize} = Voltage of the Machine or Long.MAX_VALUE if it has no Voltage;
	 * {@code aSpecialSlot} is the content of the Special Slot (regular Maps ignore it,
	 * upstream findRecipe :463 javadoc).
	 *
	 * <p>LOOKUP ONLY: never consumes the passed inputs. The mLastRecipe fast path and
	 * the linear scan both probe with {@code isRecipeInputEqual(F, T, ...)}
	 * (upstream :487/:501).
	 */
	@Nullable
	public Recipe findRecipe(@Nullable Recipe aLastRecipe, long aSize, @Nullable ItemStack aSpecialSlot, @Nullable FluidStack[] aFluids, ItemStack... aInputs) {
		if (aInputs == null || aInputs.length <= 0) return null;

		// Check the Recipe which has been used last time in order to not have to search for it again, if possible. (upstream :487)
		if (aLastRecipe != null && !aLastRecipe.mFakeRecipe && aLastRecipe.mCanBeBuffered && aLastRecipe.isRecipeInputEqual(false, true, aFluids, aInputs)) {
			return aLastRecipe.mEnabled && absGreaterEqual(aSize * mPower, aLastRecipe.mEUt) ? aLastRecipe : null;
		}

		// Linear scan of mRecipeList (upstream :498-501 minus the hash indexes).
		for (Recipe tRecipe : mRecipeList) {
			if (tRecipe.mFakeRecipe || !tRecipe.isRecipeInputEqual(false, true, aFluids, aInputs)) continue;
			return tRecipe.mEnabled && absGreaterEqual(aSize * mPower, tRecipe.mEUt) ? tRecipe : null;
		}
		return null;
	}

	/** Upstream UT.Code.abs_greater_equal (UT.java:1727). */
	protected static boolean absGreaterEqual(long aAmount1, long aAmount2) {
		return Math.abs(aAmount1) >= Math.abs(aAmount2);
	}
}
