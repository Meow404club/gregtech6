/**
 * Copyright (c) 2026 GregTech-6 Team
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

package gregtech6.recipes.maps;

import java.util.Collection;

import javax.annotation.Nullable;

import net.minecraft.world.item.ItemStack;

import net.minecraftforge.fluids.FluidStack;

import gregapi.data.TD;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.items.GT6UsbSticks;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;

/**
 * The Molecular Scanner map — the port of upstream
 * {@code gregapi/recipes/maps/RecipeMapScannerMolecular.java} (task
 * p32-qu-scanner-replicator, the GT6RecipeMapCanner judged form: the DYNAMIC scan
 * semantics port IN FULL, not a narrow pinned face — the upstream map carries zero static
 * rows, the runtime synthesis IS the map).
 *
 * <p><b>The runtime arm</b> (RecipeMapScannerMolecular.java:46-67): a T3 USB stick plus a
 * SCANNABLE prefix item with material data synthesizes a one-time row — the scanned item
 * and the stick are consumed, the stick comes back carrying
 * {@code gt.usb.data = {gt.replicator.data: <material id short>}} and the
 * {@code gt.usb.tier = 3} byte (:58-60, through {@link GT6UsbSticks#writeMaterialData}),
 * power {@code (protons + neutrons) × 512} eUt at duration 512 (:57). The recipe is
 * built ON COPIES (findRecipe is a pure lookup; the machine's two-stage
 * isRecipeInputEqual consume does the real consumption) and never cached
 * ({@code aCanBeBuffered = F} — upstream :57's first arg, one-time rows don't ride
 * mLastRecipe).
 *
 * <p><b>Declared deviations</b> (both canner-form):
 * <ul>
 * <li>the {@code GAPI_POST.mFinishedServerStarted <= 0} guard (:48) has no port
 *     counterpart — the only findRecipe consumer is the ticking machine on a live
 *     server, and the offline tests drive the arm directly;</li>
 * <li>the {@code containsInput} wide face (:69) stays unwired — the port RecipeMap
 *     carries no containsInput surface (the p14 declaration, the canner note);</li>
 * <li>the voltage WINDOW lives in the stored-row scan only: the synthesis answers BEFORE
 *     any window consult (upstream :48/:55 verbatim — the override never reads aSize), so
 *     the scan row rides the machine-side energy budget at a below-recipe rung (the
 *     replicator map carries the same note).</li>
 * </ul>
 *
 * <p>The material resolution rides the port's item→material seam
 * ({@code MaterialPrefixItem.prefix/material}, the melting-gate seam face) — the upstream
 * {@code OM.anydata_} walk has no wider port counterpart yet, so non-GT stacks are
 * unresolvable and just fall through (upstream :63's {@code return rRecipe} arm).
 */
public class GT6RecipeMapScannerMolecular extends RecipeMap {

	/** The :57 scan duration — a constant 512 ticks regardless of the scanned material. */
	public static final long SCAN_DURATION = 512;

	/** The :57 power factor — (protons + neutrons) × 512 QU per scan. */
	public static final long SCAN_EUT_PER_NUCLEON = 512;

	public GT6RecipeMapScannerMolecular(@Nullable Collection<Recipe> aRecipeList, String aNameInternal, String aNameLocal, @Nullable String aNameNEI,
			long aProgressBarDirection, long aProgressBarAmount, String aNEIGUIPath,
			long aInputItemsCount, long aOutputItemsCount, long aMinimalInputItems,
			long aInputFluidCount, long aOutputFluidCount, long aMinimalInputFluids, long aMinimalInputs, long aPower) {
		super(aRecipeList, aNameInternal, aNameLocal, aNameNEI, aProgressBarDirection, aProgressBarAmount, aNEIGUIPath,
				aInputItemsCount, aOutputItemsCount, aMinimalInputItems, aInputFluidCount, aOutputFluidCount,
				aMinimalInputFluids, aMinimalInputs, aPower);
	}

	@Override
	@Nullable
	public Recipe findRecipe(@Nullable Recipe aLastRecipe, long aSize, @Nullable ItemStack aSpecialSlot, @Nullable FluidStack[] aFluids, ItemStack... aInputs) {
		// :47-48 — the stored rows win (the shipped stand-in row); the arm fires only when
		// nothing matched and TWO real inputs sit in the machine (:48's null/length gate)
		Recipe rRecipe = super.findRecipe(aLastRecipe, aSize, aSpecialSlot, aFluids, aInputs);
		if (rRecipe != null || aInputs == null || aInputs.length < 2) return rRecipe;

		// :50-53 — the first T3 stick is the medium, the first non-stick stack the scanned item
		ItemStack tUSB = null, tScanned = null;
		for (ItemStack aInput : aInputs) {
			if (aInput == null || aInput.isEmpty()) continue;
			if (tUSB == null && isScannerStick(aInput)) {
				tUSB = aInput;
			} else if (tScanned == null) {
				tScanned = aInput;
			}
			if (tUSB != null && tScanned != null && !tScanned.isEmpty()) {
				// :55-56 — the (prefix, material) resolution + the SCANNABLE gate
				if (!(tScanned.getItem() instanceof MaterialPrefixItem tItem) || tItem.material == null
						|| tItem.material.mID < 1 || !tItem.prefix.contains(TD.Prefix.SCANNABLE)) return rRecipe;
				// :57-61 — the one-time row: both inputs consumed, the stick back WITH the data
				ItemStack tOutput = tUSB.copy();
				tOutput.setCount(1);
				GT6UsbSticks.writeMaterialData(tOutput, tItem.material);
				return new Recipe(false, // :57 aCanBeBuffered F — one-time rows never cache
						new ItemStack[] {tScanned.copy(), tUSB.copy()},
						new ItemStack[] {tOutput},
						null, null,
						SCAN_DURATION, (tItem.material.mProtons + tItem.material.mNeutrons) * SCAN_EUT_PER_NUCLEON, 0);
			}
		}
		return rRecipe;
	}

	/** The OD_USB_STICKS[3] membership face — the port item identity (the :63 gate; the stick carries the tier byte). */
	public static boolean isScannerStick(ItemStack aStack) {
		return aStack != null && !aStack.isEmpty() && aStack.getItem() instanceof GT6UsbSticks.GT6UsbStickItem tStick
				&& tStick.mTier == GT6UsbSticks.TIER_SCANNER_WRITE;
	}
}
