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

package gregtech6.recipes.maps;

import java.util.Collection;

import javax.annotation.Nullable;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;

/**
 * The Canning Machine map — the port of upstream
 * {@code gregapi/recipes/maps/RecipeMapFluidCanner.java} (task p24-canner-machine, ruling
 * R1 of decisions.p24-canner-dyes-rulings: the DYNAMIC fill/empty semantics port IN FULL,
 * not a narrow pinned face — foam cans, Lighter, food-canning pool items all resolve
 * through this one {@code findRecipe} override upstream, and pinning only the 17 refill
 * rows would make the Canner a dead-recipe machine needing a per-card map edit).
 *
 * <p>The upstream ctor (:43-46) sets {@code mMaxFluidInputSize = mMaxFluidOutputSize =
 * 128000} — the port RecipeMap carries no such cap field (RecipeMap.java:38 form), and
 * ruling R6 folds the 128000 map ceiling into the T1 tank capacity (the registration
 * rows' NBT_TANK_CAPACITY 128000..8192000, the GTGeneratorFluidBedBlockEntity:54-55 fold
 * precedent). The per-machine bound IS the tank size here.
 *
 * <p>The three dynamic arms of the upstream {@code findRecipe} override (:48-71), riding
 * the capability translation of the 1.7.10 {@code IFluidContainerItem} (the R1 seam = the
 * modern {@link IFluidHandlerItem}, the lesson-id224 offline-safe item face):
 * <ol>
 * <li><b>the explicit rows first</b> (:50) — {@code super.findRecipe} wins; the dynamic
 *     arms only fire when no stored row matched (and the machine passed real inputs);</li>
 * <li><b>the emptying arm</b> (:52-55) — an input holding fluid becomes a one-time recipe:
 *     consume the filled container, output the drained container, the content rides the
 *     fluid-OUTPUT leg, duration {@code max(amount/64, 16)} at EUt 16;</li>
 * <li><b>the filling arm</b> (:65-67) — with the first input-tank fluid available, a
 *     container input becomes a one-time recipe: consume the container AND the tank's
 *     content copy (the whole post-fill content counts, exactly upstream), output the
 *     filled container, duration {@code max(content/64, 16)} at EUt 16.</li>
 * </ol>
 * Both arms construct their Recipe ON THE RESOLVER COPIES (the upstream FL.fill /
 * ST.container copy semantics — findRecipe is a pure lookup, nothing is consumed here;
 * the machine's two-stage isRecipeInputEqual consume does the real consumption).
 *
 * <p>Declared deviations (both R1 rulings):
 * <ul>
 * <li>the GC oxygen-tank branch (:57-64) is CUT — it sits behind
 *     {@code MD.GC.mLoaded || MD.GC_GALAXYSPACE.mLoaded}, a mod-presence gate that is
 *     structurally dead in this port (no GalactiCraft); a declared-pruned dead branch;</li>
 * <li>the {@code GAPI_POST.mFinishedServerStarted <= 0} guard (:51) has no port
 *     counterpart — the only findRecipe consumer is the ticking machine on a live server,
 *     and the offline tests drive the dynamic arms directly through this method.</li>
 * </ul>
 *
 * <p>The {@code containsInput} wide face (:73-75) ports as the three overloads below —
 * the upstream recipe-FILTER face (the :568 getFluidTankFillable consult) is the pool row
 * the port never wired (RecipeMap.java carries no containsInput surface, the p14
 * declaration), so these are the map-level surface only: any fluid counts as a Canner
 * input (:74-75 verbatim), and an item counts when a fluid handler can be resolved for it
 * with positive capacity (:73, through the same resolver seam).
 */
public class GT6RecipeMapCanner extends RecipeMap {

	/**
	 * The container seam: resolves the {@link IFluidHandlerItem} acting on the given stack
	 * COPY (the live default = {@code FluidUtil.getFluidHandler}; fixtures injected offline —
	 * the lesson-id224 rule forbids probing live-registered container items offline, so the
	 * tests swap in synthetic handlers). The handler mutates the copy in place; callers
	 * always pass a SINGLE-ITEM copy ({@code copy() + setCount(1)} — the upstream
	 * {@code ST.amount(1, tInput)} per-process normalization and the
	 * {@code FluidUtil.getFluidHandler} "stackSize of 1" contract, see findRecipe) and read
	 * {@link IFluidHandlerItem#getContainer()}.
	 * The LazyOptional (forge 1.20.1) vs Optional (neoforge 21.1) return split forks here.
	 */
	public static java.util.function.Function<ItemStack, IFluidHandlerItem> sContainerResolver =
			//? if forge {
			aStack -> net.minecraftforge.fluids.FluidUtil.getFluidHandler(aStack).resolve().orElse(null);
			//?} else {
			/*aStack -> net.neoforged.neoforge.fluids.FluidUtil.getFluidHandler(aStack).orElse(null); // 21.1: Optional<IFluidHandlerItem>
			 *///?}

	public GT6RecipeMapCanner(@Nullable Collection<Recipe> aRecipeList, String aNameInternal, String aNameLocal, @Nullable String aNameNEI,
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
		// :50 — the explicit rows (the 17 refill rows) win over the dynamic arms
		Recipe rRecipe = super.findRecipe(aLastRecipe, aSize, aSpecialSlot, aFluids, aInputs);
		if (aInputs == null || aInputs.length <= 0 || rRecipe != null) return rRecipe;

		for (ItemStack tInput : aInputs) if (tInput != null && !tInput.isEmpty()) {
			// the 1.20.1 slot census: an EMPTY-looking but non-empty-count stack (the in-place
			// consumption leftover) is not a canning input
			if (tInput.getCount() <= 0) continue;
			// upstream :55 ST.amount(1, tInput) — the dynamic arms process ONE container per
			// recipe. The SINGLE copy is also the resolver's contract face (FluidUtil
			// .getFluidHandler: "the itemStack MUST have a stackSize of 1 if you want to fill
			// or drain it") — a stacked-slot probe would multiply or destroy liquid. The
			// consume semantics ride the recipe's mInputs count 1 (isRecipeInputEqual removes
			// exactly one item per process), so a stacked slot only ever loses one container.
			ItemStack tSingle = tInput.copy();
			tSingle.setCount(1);
			// the resolver works on a COPY — findRecipe is lookup-only (the two-stage
			// isRecipeInputEqual consume in the machine does the real consumption)
			IFluidHandlerItem tHandler = sContainerResolver.apply(tSingle.copy());
			if (tHandler == null) continue;

			// :52-55 — the emptying arm: a container HOLDING fluid drains onto the output leg
			FluidStack tDrained = tHandler.drain(Integer.MAX_VALUE, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
			if (tDrained != null && !tDrained.isEmpty() && tDrained.getAmount() > 0) {
				tHandler.drain(Integer.MAX_VALUE, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE); // the copy only
				ItemStack tEmptyContainer = tHandler.getContainer(); // ST.container(ST.amount(1, tInput), T) — the drained shape, count 1
				return new Recipe(false, // aCanBeBuffered F — upstream :55 (one-time recipes never cache)
						new ItemStack[] {tSingle}, new ItemStack[] {tEmptyContainer},
						null, new FluidStack[] {tDrained},
						Math.max(tDrained.getAmount() / 64, 16), 16, 0);
			}

			// :65-67 — the filling arm (the GC oxygen branch :57-64 CUT, the R1 dead-code ruling):
			// the first input-tank fluid fills the container copy; the whole post-fill content
			// counts as the fluid-input leg (upstream tFluid = FL.getFluid(tOutput, T))
			if (aFluids != null && aFluids.length > 0 && aFluids[0] != null && !aFluids[0].isEmpty()) {
				IFluidHandlerItem tFiller = sContainerResolver.apply(tSingle.copy());
				if (tFiller == null) continue;
				//? if forge {
				int tFilled = tFiller.fill(new FluidStack(aFluids[0], aFluids[0].getAmount()), net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE); // FL.fill(..., aRemoveFluidDirectly=F, ...) — the source is NOT debited here
				//?} else {
				/*int tFilled = tFiller.fill(aFluids[0].copyWithAmount(aFluids[0].getAmount()), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE); // 21.1: no copy ctor — copyWithAmount(int)
				 *///?}
				if (tFilled <= 0) continue;
				ItemStack tFilledContainer = tFiller.getContainer();
				FluidStack tContent = tFiller.drain(Integer.MAX_VALUE, net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE); // FL.getFluid(tOutput, T) — the whole content
				if (tContent == null || tContent.isEmpty()) continue;
				return new Recipe(false, // aCanBeBuffered F — upstream :67
						new ItemStack[] {tSingle}, new ItemStack[] {tFilledContainer},
						new FluidStack[] {tContent}, null,
						Math.max(tContent.getAmount() / 64, 16), 16, 0);
			}
		}
		return null;
	}

	// -------------------------------------------------------------------------
	// the containsInput wide face (:73-75)
	// -------------------------------------------------------------------------

	/**
	 * :73 — an item counts as a Canner input when the stored rows match it OR a fluid
	 * handler with positive capacity resolves for it (the upstream
	 * {@code IFluidContainerItem.getCapacity(aStack) > 0} arm over the resolver seam).
	 */
	public boolean containsInput(ItemStack aStack) {
		if (aStack == null || aStack.isEmpty()) return false;
		for (Recipe tRecipe : mRecipeList) for (ItemStack tInput : tRecipe.mInputs) {
			if (tInput != null && !tInput.isEmpty() && ItemStack.isSameItemSameTags(tInput, aStack)) return true;
		}
		ItemStack tSingle = aStack.copy();
		tSingle.setCount(1); // the resolver's stackSize-of-1 contract (see findRecipe)
		IFluidHandlerItem tHandler = sContainerResolver.apply(tSingle);
		return tHandler != null && tHandler.getTanks() > 0 && tHandler.getTankCapacity(0) > 0;
	}

	/** :74 — ANY fluid stack counts (the Canner accepts everything into its tanks). */
	public boolean containsInput(FluidStack aFluid) {return true;}

	/** :75 — ANY fluid counts (the side-less form). */
	public boolean containsInput(Fluid aFluid) {return true;}
}
