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

import javax.annotation.Nullable;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
//? if forge {
import net.minecraftforge.common.ForgeHooks;
//?}
import net.minecraftforge.fluids.FluidStack;

/**
 * GT6 RecipeMapFurnaceFuel, the FM.Furnace ("mc.recipe.furnacefuel", FM.java:38) port —
 * the SOLID Burning Box fuel map (task p13-burning-box-family spec ①).
 *
 * <p><b>The port form ruling</b>: upstream this map carries ZERO static rows —
 * {@code findRecipe} synthesizes a fuel Recipe ON DEMAND from the vanilla furnace fuel
 * value (upstream RecipeMapFurnaceFuel.java:48-92: {@code ST.fuel(aInputs[0])} →
 * {@code new Recipe(..., tFuelValue * EU_PER_FURNACE_TICK, -1, 0)}). The 1.20.1
 * counterpart of "the vanilla furnace fuel value" is the ForgeHooks.getBurnTime bridge
 * (forge-1.20.1 ForgeHooks.java:1130 {@code getBurnTime(ItemStack, RecipeType<?>)},
 * vanilla {@code VANILLA_BURNS} + the FurnaceFuelBurnTimeEvent chain — the FuelValues
 * semantics), so the port synthesizes through that: duration {@code = burnTime ×
 * EU_PER_FURNACE_TICK} and EUt {@code -1} exactly like the upstream row (the
 * {@code getAbsoluteTotalPower() = |EUt × duration| = burnTime × 25} HU per item,
 * Recipe.java:723-725 — a 1600-tick coal is therefore 40000 HU raw, and the Brick box
 * at efficiency 2500 charges 10000 HU = 625 ticks at 16 HU/t). A static pour would be
 * the WRONG shape: mod fuel rows would freeze at pour time instead of following the
 * live fuel registry.
 *
 * <p><b>Trimmed against upstream (declared)</b>: the ash-synthesis branch
 * (:56-83 — {@code OM.anydata_}/{@code mTargetBurning} dust-output chain) and the
 * EXPLOSIVE-material exclusion (:55) ride the material-data pool card — the port's
 * {@link OreDictMaterial} carries neither {@code mTargetBurning} nor explosivity, so the
 * no-container case yields an OUTPUT-LESS recipe (upstream with no ore data and no
 * container reaches the very same shape, :73-74). The Lava-container exclusion
 * (:55 {@code !FL.contains(aInputs[0], FL.Lake)}) is likewise pooled — vanilla itself
 * burns lava buckets (20000 ticks, bucket back), so the port follows the vanilla
 * behaviour instead of the upstream veto. The dynamic-cache arm (:87-88 adds the
 * synthesized Recipe back into the pool) is not ported — the fresh-per-find shape is
 * the port {@code RecipeMapFurnace} precedent (mCanBeBuffered=false, "LOOKUP ONLY").
 */
public class RecipeMapFurnaceFuel extends RecipeMap {

	/** Upstream CS.java:212 — GU per vanilla furnace tick (1 Smelt = 200 ticks = 5000 GU, the "(1 Smelt = 5000 GU)" RecipeMapFurnaceFuel.java:45 name suffix). */
	public static final long EU_PER_FURNACE_TICK = 25;

	public RecipeMapFurnaceFuel() {
		super(new java.util.HashSet<>(),
				"mc.recipe.furnacefuel", "Furnace Fuels", "smelting",
				0, 1,
				"gt6:textures/gui/machines/oven",
				/*IN-OUT-MIN-ITEM=*/ 1, 1, 1,
				/*IN-OUT-MIN-FLUID=*/ 0, 0, 0,
				/*MIN=*/ 0,
				/*AMP=*/ 1);
	}

	/**
	 * The on-demand fuel synthesis (upstream RecipeMapFurnaceFuel.java:48-92, the
	 * ForgeHooks bridge form): burnTime &gt; 0 → a fresh Recipe with the item ×1 as the
	 * sole input, the crafting container item (bucket et al) as the sole output when it
	 * exists, duration {@code burnTime × 25}, EUt {@code -1}, never cached.
	 */
	@Nullable
	public Recipe findFuelRecipe(ItemStack aInput) {
		if (aInput == null || aInput.isEmpty()) return null;
		//? if forge {
		int tBurnTime = ForgeHooks.getBurnTime(aInput, RecipeType.SMELTING); // the :53 ST.fuel counterpart
		//?} else {
		/*int tBurnTime = aInput.getBurnTime(RecipeType.SMELTING); // 21.1: ForgeHooks.getBurnTime deleted — IItemStackExtension.getBurnTime(RecipeType) is the same query (javap 21.1.249)
		*///?}
		if (tBurnTime <= 0) return null;
		ItemStack tContainer = aInput.hasCraftingRemainingItem() ? aInput.getCraftingRemainingItem() : null; // the :56 ST.container counterpart
		ItemStack tOne = aInput.copy();
		tOne.setCount(1); // the :74 ST.amount(1, aInputs[0]) form — the full item+tag identity at amount 1
		return new Recipe(false, // mCanBeBuffered=false — the fresh-per-find port RecipeMapFurnace precedent
				new ItemStack[] {tOne},
				tContainer == null || tContainer.isEmpty() ? new ItemStack[0] : new ItemStack[] {tContainer},
				new FluidStack[0], new FluidStack[0],
				tBurnTime * EU_PER_FURNACE_TICK, // :74/:79/:85 — the fuel value in GU
				-1, // :74 — the negative-EUt generator convention; |EUt × duration| = the raw heat
				0);
	}

	/** Upstream containsInput (RecipeMapFurnaceFuel.java:94) — "burnable" = the synthesis answers. */
	public boolean containsFuelInput(@Nullable ItemStack aStack) {
		return findFuelRecipe(aStack) != null;
	}
}
