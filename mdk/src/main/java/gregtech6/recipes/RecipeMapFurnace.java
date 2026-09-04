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

import javax.annotation.Nullable;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;

/**
 * GT6 RecipeMapFurnace, minimal port of upstream
 * gregapi/recipes/maps/RecipeMapFurnace.java:45-158.
 *
 * <p>{@code findRecipe} does not consult a GT recipe stock at all: it asks the
 * vanilla {@code level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, ...)}
 * and builds a fresh {@link Recipe} on the spot with mEUt=16 / mDuration=16
 * (upstream :54/:154 semantics).
 *
 * <p>Trimmed against upstream: the cooking-oil output boost (:57-61) and the XP
 * fluid calculation (:63-152) are GT6 content (FluidsGT/OD/OM/IL dependencies)
 * and stay in the phase-4 pool; the generated Recipe therefore carries empty
 * fluid arrays. Like upstream :154, the recipe is generated with
 * mCanBeBuffered=false so machines do not cache one-time Recipe objects.
 */
public class RecipeMapFurnace extends RecipeMap {
	public RecipeMapFurnace(Collection<Recipe> aRecipeList, String aNameInternal, String aNameLocal, @Nullable String aNameNEI, long aProgressBarDirection, long aProgressBarAmount, String aNEIGUIPath, long aInputItemsCount, long aOutputItemsCount, long aMinimalInputItems, long aInputFluidCount, long aOutputFluidCount, long aMinimalInputFluids, long aMinimalInputs, long aPower) {
		super(aRecipeList, aNameInternal, aNameLocal, aNameNEI, aProgressBarDirection, aProgressBarAmount, aNEIGUIPath, aInputItemsCount, aOutputItemsCount, aMinimalInputItems, aInputFluidCount, aOutputFluidCount, aMinimalInputFluids, aMinimalInputs, aPower);
	}

	/**
	 * LOOKUP ONLY: never consumes the passed inputs; the machine consumes via
	 * {@code Recipe.isRecipeInputEqual(true, false, aFluids, aInputs)} (pinned
	 * contract for p4-machine-oven, upstream MultiTileEntityBasicMachine.java:725/:738).
	 */
	@Nullable
	public Recipe findRecipe(Level aLevel, @Nullable Recipe aLastRecipe, long aSize, @Nullable ItemStack aSpecialSlot, @Nullable FluidStack[] aFluids, ItemStack... aInputs) {
		if (aInputs == null || aInputs.length <= 0 || aInputs[0] == null || aInputs[0].isEmpty()) return null;

		// Check the Recipe which has been used last time. (upstream :53)
		if (aLastRecipe != null && aLastRecipe.isRecipeInputEqual(false, true, aFluids, aInputs)) return aLastRecipe;

		ItemStack tOutput = getSmeltingResult(aLevel, aInputs[0]);
		if (tOutput == null) return null;

		// return the Recipe (upstream :154 — mCanBeBuffered=F, mEUt=16, mDuration=16, no fluids)
		return new Recipe(false, new ItemStack[] {amount(1, aInputs[0])}, new ItemStack[] {tOutput}, null, null, 16, 16, 0);
	}

	/** Vanilla smelting query (upstream :54 RM.get_smelting). Returns a COPY of the result stack. */
	@Nullable
	public static ItemStack getSmeltingResult(Level aLevel, ItemStack aInput) {
		//? if forge {
		return aLevel.getRecipeManager()
				.getRecipeFor(RecipeType.SMELTING, new SingleSlotContainer(aInput), aLevel)
				.map(tRecipe -> tRecipe.getResultItem(aLevel.registryAccess()).copy())
				.orElse(null);
		//?} else {
		/*// 21.1: getRecipeFor takes a RecipeInput and returns Optional<RecipeHolder<T>>
		//(javap RecipeManager 21.1.249; SingleSlotContainer is forge-side) — unwrap .value().
		return aLevel.getRecipeManager()
				.getRecipeFor(RecipeType.SMELTING, new net.minecraft.world.item.crafting.SingleRecipeInput(aInput), aLevel)
				.map(tRecipe -> tRecipe.value().getResultItem(aLevel.registryAccess()).copy())
				.orElse(null);
		*///?}
	}

	/** Upstream containsInput (RecipeMapFurnace.java:157): is the given stack smeltable at all? */
	public boolean containsInput(Level aLevel, ItemStack aStack) {
		return aStack != null && !aStack.isEmpty() && getSmeltingResult(aLevel, aStack) != null;
	}

	/** Upstream ST.amount: normalize to the given stack size. */
	private static ItemStack amount(long aAmount, ItemStack aStack) {
		ItemStack tStack = aStack.copy();
		tStack.setCount((int)aAmount);
		return tStack;
	}

	/** {@link SmeltingRecipe#matches} tests {@code container.getItem(0)} only (vanilla AbstractCookingRecipe.java:32-34), so a one-slot view of the input suffices. */
	private record SingleSlotContainer(ItemStack mStack) implements Container {
		@Override public int getContainerSize() { return 1; }
		@Override public boolean isEmpty() { return mStack.isEmpty(); }
		@Override public ItemStack getItem(int aSlot) { return mStack; }
		@Override public ItemStack removeItem(int aSlot, int aAmount) { return ItemStack.EMPTY; }
		@Override public ItemStack removeItemNoUpdate(int aSlot) { return ItemStack.EMPTY; }
		@Override public void setItem(int aSlot, ItemStack aStack) {}
		@Override public void setChanged() {}
		@Override public void clearContent() {}
		@Override public boolean stillValid(net.minecraft.world.entity.player.Player aPlayer) { return true; }
	}
}
