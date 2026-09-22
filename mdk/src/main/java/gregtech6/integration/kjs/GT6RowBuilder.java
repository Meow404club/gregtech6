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
//? if kjs {
package gregtech6.integration.kjs;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;

/**
 * The fluent row builder behind the {@link GT6Recipes} facade — the script shape is
 * {@code GT6Recipes.map("gt.recipe.shredder").inputs("minecraft:stone")
 * .outputs("minecraft:gravel").eut(16).duration(40).add()}.
 *
 * <p>Zero KubeJS imports by design: the typed ItemStack/FluidStack parameters are
 * what Rhino's type wrappers fill from script values (GT6KJS class doc), so the
 * whole class is leg-neutral and offline-testable. Row semantics align the tier-b
 * JSON loader's 7 fields (GT6RecipeMapJsonLoader: inputs/outputs/fluidInputs/
 * fluidOutputs/duration/eut + the per-output chances): duration &gt; 0 required,
 * eut signed (negative = the fuel-door convention), chances 10000-base per output
 * slot (the port convention; {@code chances(...)} pads/shortens like the Recipe
 * ctor), {@code mCanBeBuffered} = true (production row default).
 */
public final class GT6RowBuilder {

	/** The target map (never null — the facade resolves it via GT6KJS.map). */
	final RecipeMap mMap;

	private ItemStack[] mInputs = new ItemStack[0];
	private ItemStack[] mOutputs = new ItemStack[0];
	private FluidStack[] mFluidInputs = new FluidStack[0];
	private FluidStack[] mFluidOutputs = new FluidStack[0];
	private long mDuration = -1, mEUt = 0, mSpecialValue = 0;
	private long[] mChances;

	GT6RowBuilder(RecipeMap aMap) {
		mMap = aMap;
	}

	/** Item input slots (script values land here pre-wrapped). */
	public GT6RowBuilder inputs(ItemStack... aInputs) {
		mInputs = aInputs == null ? new ItemStack[0] : aInputs;
		return this;
	}

	/** Item output slots. */
	public GT6RowBuilder outputs(ItemStack... aOutputs) {
		mOutputs = aOutputs == null ? new ItemStack[0] : aOutputs;
		return this;
	}

	/** Fluid input slots (mB amounts ride the stacks). */
	public GT6RowBuilder fluidInputs(FluidStack... aInputs) {
		mFluidInputs = aInputs == null ? new FluidStack[0] : aInputs;
		return this;
	}

	/** Fluid output slots (mB amounts ride the stacks). */
	public GT6RowBuilder fluidOutputs(FluidStack... aOutputs) {
		mFluidOutputs = aOutputs == null ? new FluidStack[0] : aOutputs;
		return this;
	}

	/** Duration in ticks — REQUIRED &gt; 0 (the JSON loader's row gate). */
	public GT6RowBuilder duration(long aDuration) {
		mDuration = aDuration;
		return this;
	}

	/** EU/t (signed; negative = the fuel-door convention). */
	public GT6RowBuilder eut(long aEUt) {
		mEUt = aEUt;
		return this;
	}

	/** The special-value carrier (map-dependent; default 0). */
	public GT6RowBuilder specialValue(long aSpecialValue) {
		mSpecialValue = aSpecialValue;
		return this;
	}

	/** Per-output chances, 10000 = 100%, aligned to the output slots (short array = the tail reads 10000). */
	public GT6RowBuilder chances(long... aChances) {
		mChances = aChances;
		return this;
	}

	/** Structural validation (the JSON loader's bad-row gates, fail-loud at the script instead of skip-per-row). */
	public boolean validate() {
		if (mMap == null) return false;
		if (mDuration <= 0) throw new IllegalArgumentException("GT6 KJS row: \"duration\" is required and must be > 0 (map " + mMap.mNameInternal + ")");
		if (mDuration > Integer.MAX_VALUE) throw new IllegalArgumentException("GT6 KJS row: duration overflow (map " + mMap.mNameInternal + ")");
		return true;
	}

	/** Assembles the Recipe (the chances-bearing ctor; trailing-null trim is the ctor's own). */
	public Recipe build() {
		return new Recipe(true, mInputs, mOutputs, mFluidInputs, mFluidOutputs, mDuration, mEUt, mSpecialValue, mChances);
	}

	/** Validates + pours through {@link GT6KJS#addRow} (funnel + FROZEN gate + idempotence ledger).
	 * @return true = poured. */
	public boolean add() {
		return GT6KJS.addRow(this);
	}
}
//?}
