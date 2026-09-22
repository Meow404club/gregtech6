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

import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import gregtech6.items.bees.GT6Bumbles;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;
import gregtech6.registry.GTMaterialItems;

/**
 * The Bumblelyzer map — the port of upstream {@code gregapi/recipes/maps/RecipeMapBumblelyzer.java}
 * (task p34-machines-bumblelyzer-crucible). The map itself is DECLARED-empty as a static stock:
 * the live scan semantics are the DYNAMIC {@code findRecipe} arm (:50-74), the
 * {@link GT6RecipeMapCanner} R1 ruling's second consumer — every bee of every species resolves
 * through this one override upstream, and the static stock is only the NEI display face.
 *
 * <p>The upstream display stock ({@code MultiItemBumbles.make} :581-588, addFakeRecipe rows)
 * ports as {@link #sFakeRecipes} — a display-only list OUTSIDE {@code mRecipeList}, because the
 * port {@code RecipeMap.addRecipe} (:177, the :293 upstream javadoc "findRecipe wont find fake
 * Recipes") keeps fake rows unfindable by construction. The rows are built by
 * {@link gregtech6.items.bees.GT6Bumbles#addScanFakeRecipes} (the card's only legal writer of
 * that file) and consumed by the census pins (acceptance ②); the JEI display bridge stays pooled
 * (the P12 category bridge), so the list is the map's whole fake face.
 *
 * <p>The two dynamic arms of the upstream {@code findRecipe} (:55-62), riding the first input
 * tank (:53 {@code aFluids[0]} verbatim):
 * <ol>
 * <li><b>the scan arm</b> (:58-60) — an UNSCANNED bee ({@code bumbleType < 5}) + a paper tiny
 *     becomes a one-time recipe: consume the bee and the paper, output the scanned form (the
 *     {@code bumbleScan} :564 meta+5 copy — the stack NBT, the gt.bumble genes, rides along), the
 *     first tank fluid rides the input leg at 10 L, duration 64 @ 16 EUt;</li>
 * <li><b>the pass-through arm</b> (:61) — an already-scanned bee copies itself through with no
 *     fluid and no paper, duration 1 @ 16 EUt ("Was already scanned, auto-skipping").</li>
 * </ol>
 * The accept set (:55) is the honey family — upstream {@code FluidsGT.HONEY} (the FL HONEY-tag
 * members that exist in a clean GT6 env: exactly the one "for.honey" row) plus the separate
 * {@code FL.Honeydew} check; the port honey faces are the p31 rows gt6:honey and gt6:honeydew
 * (the {@code GTFluids.HONEY_FLUID_SPECS} rows, whose class doc names this very accept set), so
 * the set is those two ids.
 *
 * <p>Declared deviations (the Canner precedents):
 * <ul>
 * <li>the Forestry bee arms (:63-69, {@code IL.FR_Bee_*} + the AlleleRegistry analyze walk)
 *     are CUT — a mod-presence gate structurally dead in this port (no Forestry);</li>
 * <li>the {@code GAPI_POST.mFinishedServerStarted <= 0} guard (:53) has no port counterpart —
 *     the only findRecipe consumer is the ticking machine on a live server.</li>
 * </ul>
 * Both arms build their Recipe ON COPIES (the {@code ST.amount(1, ...)} per-process
 * normalization; findRecipe is a pure lookup — the machine's two-stage isRecipeInputEqual does
 * the real consumption). The paper leg rides {@code aInput.stackSize} on the ONE-count local
 * (:60 — the upstream local is normalized to 1 before the read, so the leg is a single tiny
 * plate per process) = {@code OP.plateTiny} Paper ×1, the upstream :619
 * {@code plateTiny.forceItemGeneration(MT.Paper)} item.
 */
public class GT6RecipeMapBumblelyzer extends RecipeMap {

	/**
	 * :55 — the honey accept set (the p31 honey-family scan diluents, the "honey"/"honeydew"
	 * port ids; the FluidsGT.HONEY one-live-member form + the Honeydew check folded in).
	 */
	public static final Set<String> HONEY_ACCEPT = Set.of("honey", "honeydew");

	/** The 10 L scan diluent charge (:60 {@code FL.amount(aFluids[0], 10)}). */
	public static final int SCAN_FLUID_L = 10;

	/** The scan row clock columns (:60 — duration 64, 16 EUt) and the pass-through pair (:61 — duration 1, 16 EUt). */
	public static final long SCAN_DURATION = 64, SCAN_EUT = 16, PASS_DURATION = 1, PASS_EUT = 16;

	/**
	 * The display stock (the upstream addFakeRecipe face, :581-588): the per-species
	 * {@code {drone, princess, dead} × {honey, honeydew, pass-through ×2}} rows in build order.
	 * Display/census only — never in {@code mRecipeList}, never findable. Volatile + rebuilt per
	 * generation (the ADR-P18 form: the reset hook clears it with the maps).
	 */
	public static volatile List<Recipe> sFakeRecipes = List.of();

	static {GT6RecipeMaps.registerGenerationResetHook(GT6RecipeMapBumblelyzer::resetForTest);}

	/** The generation hook: drops the display stock with the maps (the GT6RecipeMapJsonLoader form). */
	static void resetForTest() {
		sFakeRecipes = List.of();
	}

	/** The fill seam (called by {@code GT6Bumbles.addScanFakeRecipes}): replaces the display stock wholesale. */
	public static void setFakeRecipes(List<Recipe> aRows) {
		sFakeRecipes = List.copyOf(aRows);
	}

	/**
	 * The paper seam (:60 {@code OP.plateTiny.mat(MT.Paper)}): the live {@code GTMaterialItems}
	 * lookup by default (the {@code plateTiny.forceItemGeneration(MT.Paper)} item), fixtures
	 * injected offline — the {@link GT6RecipeMapCanner#sContainerResolver} convention. Null when
	 * the item is absent (the upstream mat() null-drop form).
	 */
	public static java.util.function.Supplier<net.minecraft.world.item.Item> sPaperResolver = () -> {
		net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item> tHandle = GTMaterialItems.get(gregapi.data.OP.plateTiny, gregapi.data.MT.Paper);
		return tHandle == null ? null : tHandle.get();
	};

	/**
	 * The fluid-id seam (:55 {@code aFluids[0].getFluid().getName()}): the live
	 * {@code ForgeRegistries.FLUIDS} path by default, the identity fixture offline (the
	 * {@link #sPaperResolver} convention — the bare JVM carries no Forge registry).
	 */
	public static java.util.function.Function<net.minecraft.world.level.material.Fluid, String> sFluidIdResolver =
			aFluid -> {
				net.minecraft.resources.ResourceLocation tId = net.minecraftforge.registries.ForgeRegistries.FLUIDS.getKey(aFluid);
				return tId == null ? null : tId.getPath();
			};

	public GT6RecipeMapBumblelyzer(@Nullable java.util.Collection<Recipe> aRecipeList, String aNameInternal, String aNameLocal, @Nullable String aNameNEI,
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
		// :52 — the explicit rows win (the map stock is empty today; the JSON reload seam could pour)
		Recipe rRecipe = super.findRecipe(aLastRecipe, aSize, aSpecialSlot, aFluids, aInputs);
		if (rRecipe != null) return rRecipe;
		// :53 — the null-guard face (inputs/tanks present)
		if (aInputs == null || aFluids == null || aFluids.length < 1 || aFluids[0] == null || aFluids[0].isEmpty()) return null;
		// :55 — the honey accept set over the first tank
		String tFluidName = sFluidIdResolver.apply(aFluids[0].getFluid());
		if (tFluidName == null || !HONEY_ACCEPT.contains(tFluidName)) return null;

		for (ItemStack tInput : aInputs) {
			if (tInput == null || tInput.isEmpty()) continue;
			// :56-58 — the IItemBumbleBee instanceof walk rides the {@link GT6Bumbles#sBumbleType} seam
			Byte tType = GT6Bumbles.sBumbleType.apply(tInput);
			if (tType == null) continue;
			// :57 ST.amount(1, ...) — the dynamic arms process ONE bee per recipe; the copies keep
			// findRecipe lookup-only (the consume is the machine's isRecipeInputEqual two-stage)
			ItemStack tBee = tInput.copy();
			tBee.setCount(1);
			if (tType < 5) {
				// :58-60 — the scan arm: bee + paper tiny → the bumbleScan output, 10 L of the tank fluid
				net.minecraft.world.item.Item tPaper = sPaperResolver.get();
				if (tPaper == null) return null;
				return new Recipe(false, new ItemStack[] {tBee, new ItemStack(tPaper, 1)},
						new ItemStack[] {GT6Bumbles.bumbleScan(tBee)},
						new FluidStack[] {new FluidStack(aFluids[0].getFluid(), SCAN_FLUID_L)}, null, SCAN_DURATION, SCAN_EUT, 0);
			}
			// :61 — the pass-through arm: a scanned bee auto-skips
			return new Recipe(false, new ItemStack[] {tBee.copy()}, new ItemStack[] {tBee.copy()},
					null, null, PASS_DURATION, PASS_EUT, 0);
		}
		return null;
	}

	/** The per-face census constant (the :581-588 make() walk): the diluent rows + the pass-through pair. */
	public static int fakeRowsPerFace() {
		return HONEY_ACCEPT.size() + 2;
	}

	/** The full-stock census constant: the species count × the {drone, princess, dead} faces × per-face rows. */
	public static int fakeRowCensus(int aSpeciesCount) {
		return aSpeciesCount * 3 * fakeRowsPerFace();
	}
}
