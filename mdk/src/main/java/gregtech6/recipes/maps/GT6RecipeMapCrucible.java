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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.world.item.ItemStack;

import net.minecraftforge.fluids.FluidStack;

import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.oredict.MaterialGraph;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.oredict.OreDictPrefix;
import gregapi.util.CruciblePhysics;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;
import gregtech6.registry.GTMaterialBlocks;
import gregtech6.registry.GTMaterialItems;

import static gregapi.data.CS.U;
import static gregapi.data.CS.U4;
import static gregapi.data.CS.U72;
import static gregapi.data.CS.U9;

/**
 * The Crucible map pair — the port of upstream gregapi/recipes/maps/RecipeMapCrucible.java
 * (task p26-crucible-physics-smeltery). The upstream {@code RM.CrucibleSmelting} is the
 * on-demand {@code RecipeMapSpecialSingleInput} subclass whose {@code getRecipeFor}
 * (RecipeMapCrucible.java:82-97) derives the row from the MATERIAL GRAPH at lookup time —
 * zero static rows, the crucible is a recipe-table machine only in appearance. This port
 * carries the same shape over the {@link gregtech6.recipes.maps.GT6RecipeMapCanner} form
 * (the {@code findRecipe} override precedent, p24-canner-machine): the stored-row scan
 * runs first, and when nothing stored matched the on-demand arm derives a one-time
 * {@link Recipe} from the input's material data.
 *
 * <p><b>The on-demand arm (RecipeMapCrucible.java:82-97 verbatim)</b>: the input resolves
 * to material data, the material must carry {@code TD.Processing.MELTING} and a positive
 * {@code mTargetSmelting.mAmount}, the output is {@code ingotOrDust(mTargetSmelting,
 * units(count * prefix.mAmount, U, mTargetSmelting.mAmount, F))}, duration =
 * {@code mMeltingPoint}, EUt 0 (:96 — the crucible heats with raw HU, the recipe row
 * itself carries no power).
 *
 * <p><b>The stack→material seam</b>: the live resolver is the
 * {@link MaterialPrefixItem} instanceof face (the port's OM.anydata counterpart — the
 * prefix item carries its {@code prefix} and {@code material} as public fields); the
 * offline tests inject a resolver over synthetic stacks.
 *
 * <p><b>CRUCIBLE_ALLOYING</b> (the second map this class serves with its static
 * helpers, RM.java:128): zero static rows like its sibling — the display rows are the
 * {@link #alloyingDisplayRows} synthesis off the material graph (the GT6_Main.java:453-481
 * NEI walk), built only when a display consumer asks.
 *
 * <p><b>Declared deviations</b>:
 * <ul>
 * <li>the NEI/JEI output face of the upstream getNEIRecipes override (:50-79) is the
 * display-consumer pool — the port's JEI integration reads {@link #smeltingDisplayRows}
 * instead (no live consumer this card);</li>
 * <li>the OM.ingot/OM.dust ladders (OM.java:460-475) port in their reduced form —
 * ingot/nugget and dust/dustSmall/dustTiny steps only, the 72x/9x block steps ride the
 * block-universe pool (no blockIngot/blockDust item resolution this card);</li>
 * <li>the GT6_Main Air/C/CaCO3 display arms (:464-469) are CUT — FL.Air.display and the
 * coal/limestone special rows need display items no port card has landed (pool).</li>
 * </ul>
 */
public class GT6RecipeMapCrucible extends RecipeMap {

	/**
	 * The stack→material-data seam (the OM.anydata counterpart). The live default is the
	 * {@link MaterialPrefixItem} face; tests swap in synthetic pairs. Returns null for
	 * stacks without material data (the upstream aData == null → null drop, :84).
	 */
	public static Function<ItemStack, MaterialData> sMaterialResolver = GT6RecipeMapCrucible::prefixItemData;

	/**
	 * The {@code prefix.mat(material, count)} output seam (the OM mat() face — the same
	 * lesson-id224 static-seam discipline as {@link #sMaterialResolver}): the live
	 * default resolves the GTMaterialItems index with the GTMaterialBlocks fallback
	 * (empty offline — the intrusive-holder lesson), so the tests inject over the probe
	 * items. {@link #DEFAULT_MAT_RESOLVER} restores the live form.
	 */
	public static Function<MatRequest, ItemStack> sMatResolver = GT6RecipeMapCrucible::matStackLive;

	/** The live {@link #sMatResolver} (kept for the test restore). */
	public static final Function<MatRequest, ItemStack> DEFAULT_MAT_RESOLVER = GT6RecipeMapCrucible::matStackLive;

	/** One {@code prefix.mat(material, count)} call, the seam key. */
	public record MatRequest(OreDictPrefix prefix, OreDictMaterial material, long count) {}

	/** The resolved material face of one input stack (the OreDictItemData projection). */
	public record MaterialData(OreDictPrefix prefix, OreDictMaterial material) {}

	/** The live resolver: {@link MaterialPrefixItem} carries its pair as public fields. */
	@Nullable
	public static MaterialData prefixItemData(ItemStack aStack) {
		if (aStack == null || aStack.isEmpty() || !(aStack.getItem() instanceof MaterialPrefixItem tItem)) return null;
		return new MaterialData(tItem.prefix, tItem.material);
	}

	public GT6RecipeMapCrucible(@Nullable java.util.Collection<Recipe> aRecipeList, String aNameInternal, String aNameLocal, @Nullable String aNameNEI,
			long aProgressBarDirection, long aProgressBarAmount, String aNEIGUIPath,
			long aInputItemsCount, long aOutputItemsCount, long aMinimalInputItems,
			long aInputFluidCount, long aOutputFluidCount, long aMinimalInputFluids, long aMinimalInputs, long aPower) {
		super(aRecipeList, aNameInternal, aNameLocal, aNameNEI, aProgressBarDirection, aProgressBarAmount, aNEIGUIPath,
				aInputItemsCount, aOutputItemsCount, aMinimalInputItems, aInputFluidCount, aOutputFluidCount,
				aMinimalInputFluids, aMinimalInputs, aPower);
	}

	// ------------------------------------------------------------------------------------
	// the on-demand smelting row (RecipeMapCrucible.java:82-97)
	// ------------------------------------------------------------------------------------

	@Override
	@Nullable
	public Recipe findRecipe(@Nullable Recipe aLastRecipe, long aSize, @Nullable ItemStack aSpecialSlot, @Nullable FluidStack[] aFluids, ItemStack... aInputs) {
		// the explicit rows win (RecipeMapFluidCanner.java:50 order); the map has zero
		// stored rows, so today this returns null and the dynamic arm always answers
		Recipe tStored = super.findRecipe(aLastRecipe, aSize, aSpecialSlot, aFluids, aInputs);
		if (tStored != null || aInputs == null || aInputs.length <= 0) return tStored;

		for (ItemStack tInput : aInputs) {
			if (tInput == null || tInput.isEmpty() || tInput.getCount() <= 0) continue;
			MaterialData tData = sMaterialResolver.apply(tInput);
			if (tData == null) continue; // :84 — no material data, no row
			OreDictMaterial tMaterial = tData.material();
			// :87 — MELTING tag + a positive smelting target, else the input is not crucible food
			if (!tMaterial.contains(TD.Processing.MELTING) || tMaterial.mTargetSmelting.mAmount <= 0) continue;
			long tAmount = CruciblePhysics.units((long)tInput.getCount() * tData.prefix().mAmount, U, tMaterial.mTargetSmelting.mAmount, false);
			OreDictMaterialStack tSmelted = new OreDictMaterialStack(tMaterial.mTargetSmelting.mMaterial, tAmount);
			ItemStack tOutput = ingotOrDust(tSmelted.mMaterial, tSmelted.mAmount); // :91
			if (tOutput == null || tOutput.isEmpty()) continue; // :94 — no representable output, no row

			ItemStack tSingle = tInput.copy();
			tSingle.setCount(1); // :96 ST.amount(1, aInput) — the row consumes ONE item per process
			// :96 — EUt 0, duration = mMeltingPoint, no fluids, mCanBeBuffered F
			return new Recipe(false, new ItemStack[] {tSingle}, new ItemStack[] {tOutput}, null, null, tMaterial.mMeltingPoint, 0, 0);
		}
		return null;
	}

	// ------------------------------------------------------------------------------------
	// the display-row synthesis (the GT6_Main.java:453-481 NEI walk, per-alloy form)
	// ------------------------------------------------------------------------------------

	/**
	 * The CRUCIBLE_SMELTING display rows of one output material (the upstream
	 * getNEIRecipes :50-79 reduced to the material-graph query): every registered material
	 * that smelts INTO aMaterial shows its dust/ore row. Display-consumer pool face.
	 */
	public static List<Recipe> smeltingDisplayRows(OreDictMaterial aOutput) {
		if (aOutput == null) return Collections.emptyList();
		List<Recipe> rList = new ArrayList<>();
		for (OreDictMaterial tMat : MaterialGraph.targeting(aOutput, MaterialGraph.Process.SMELTING).keySet()) {
			Recipe tRow = displayRow(OP.dust, tMat);
			if (tRow != null) rList.add(tRow);
		}
		return rList;
	}

	/**
	 * The CRUCIBLE_ALLOYING display rows of one alloy (GT6_Main.java:453-481): one fake
	 * row over the DUST inputs and one over the INGOT inputs, output =
	 * {@code commonDivider * U} of the alloy, SpecialValue = the temperature display
	 * (the second-highest component melting point, at least the alloy's own — :478-481).
	 * The Air/C/CaCO3 special arms (:464-469) are CUT (declared deviation, class doc).
	 */
	public static List<Recipe> alloyingDisplayRows(OreDictMaterial aAlloy) {
		if (aAlloy == null || aAlloy.mComponents == null || aAlloy.mAlloyCreationRecipes.isEmpty()) return Collections.emptyList();
		List<OreDictMaterialStack> tComponents = aAlloy.mComponents.getUndividedComponents();
		if (tComponents.isEmpty()) return Collections.emptyList();

		// :477 — the sorted melting points; the special value is the SECOND-highest at least
		List<Long> tMeltingPoints = new ArrayList<>();
		List<ItemStack> tDusts = new ArrayList<>(), tIngots = new ArrayList<>();
		for (OreDictMaterialStack tComponent : tComponents) {
			tMeltingPoints.add(tComponent.mMaterial.mMeltingPoint);
			ItemStack tDust = dustOrIngot(tComponent.mMaterial, tComponent.mAmount);
			ItemStack tIngot = ingotOrDust(tComponent.mMaterial, tComponent.mAmount);
			if (tDust == null || tIngot == null) return Collections.emptyList(); // the :472 tDusts.add null-guard family
			tDusts.add(tDust);
			tIngots.add(tIngot);
		}
		Collections.sort(tMeltingPoints);
		long tSpecial = tMeltingPoints.size() > 1
				? Math.max(tMeltingPoints.get(tMeltingPoints.size() - 2), aAlloy.mMeltingPoint)
				: aAlloy.mMeltingPoint;

		ItemStack tOutput = ingotOrDust(aAlloy, aAlloy.mComponents.getCommonDivider() * U); // :478
		if (tOutput == null) return Collections.emptyList();

		List<Recipe> rList = new ArrayList<>();
		rList.add(fakeRow(tDusts, tOutput, tSpecial)); // :478 the dust row
		rList.add(fakeRow(tIngots, tOutput, tSpecial)); // :479 the ingot row
		return rList;
	}

	/** One fake display row (the addFakeRecipe(F, ...) shape, :478-481): not findable, display only. */
	private static Recipe fakeRow(List<ItemStack> aInputs, ItemStack aOutput, long aSpecialValue) {
		Recipe rRecipe = new Recipe(false, aInputs.toArray(new ItemStack[0]), new ItemStack[] {aOutput}, null, null, 0, 0, aSpecialValue);
		rRecipe.mFakeRecipe = true;
		return rRecipe;
	}

	// ------------------------------------------------------------------------------------
	// the OM projections (OM.java:411-475, the reduced ladders of the class doc)
	// ------------------------------------------------------------------------------------

	/** OM.ingotOrDust(:411-416): the ingot ladder first, the dust ladder second. */
	@Nullable
	public static ItemStack ingotOrDust(OreDictMaterial aMaterial, long aMaterialAmount) {
		if (aMaterialAmount <= 0 || aMaterial == null) return null;
		ItemStack rStack = ingot(aMaterial, aMaterialAmount);
		return rStack != null ? rStack : dust(aMaterial, aMaterialAmount);
	}

	/** OM.dustOrIngot(:422-426): the dust ladder first, the ingot ladder second. */
	@Nullable
	public static ItemStack dustOrIngot(OreDictMaterial aMaterial, long aMaterialAmount) {
		if (aMaterialAmount <= 0 || aMaterial == null) return null;
		ItemStack rStack = dust(aMaterial, aMaterialAmount);
		return rStack != null ? rStack : ingot(aMaterial, aMaterialAmount);
	}

	/** OM.ingot(:469-474) reduced: ingot → chunk-free → nugget. */
	@Nullable
	public static ItemStack ingot(OreDictMaterial aMaterial, long aMaterialAmount) {
		if (aMaterialAmount < U9 || aMaterial == null) return null;
		if (aMaterialAmount >= U) {
			ItemStack tStack = matStack(OP.ingot, aMaterial, aMaterialAmount / U);
			if (tStack != null) return tStack;
		}
		return matStack(OP.nugget, aMaterial, (aMaterialAmount * 9) / U);
	}

	/** OM.dust(:460-466) reduced: dust → dustSmall → dustTiny. */
	@Nullable
	public static ItemStack dust(OreDictMaterial aMaterial, long aMaterialAmount) {
		if (aMaterialAmount < U72 || aMaterial == null) return null;
		if (aMaterialAmount >= U) {
			ItemStack tStack = matStack(OP.dust, aMaterial, aMaterialAmount / U);
			if (tStack != null) return tStack;
		}
		if (aMaterialAmount >= U4) {
			ItemStack tStack = matStack(OP.dustSmall, aMaterial, (aMaterialAmount * 4) / U);
			if (tStack != null) return tStack;
		}
		return matStack(OP.dustTiny, aMaterial, (aMaterialAmount * 9) / U);
	}

	/**
	 * The {@code prefix.mat(material, count)} face (upstream OreDictItemData mat()): the
	 * GTMaterialItems index with the GTMaterialBlocks fallback, bindStack-capped, null
	 * when the pair has no item (the upstream silent-drop semantics). The Mold BE
	 * consumes this for its solidified output too.
	 */
	@Nullable
	public static ItemStack matStack(OreDictPrefix aPrefix, OreDictMaterial aMaterial, long aCount) {
		if (aCount < 1 || aPrefix == null || aMaterial == null) return null;
		return sMatResolver.apply(new MatRequest(aPrefix, aMaterial, aCount));
	}

	/** The live {@code prefix.mat} resolution (the GTMaterialItems index + the block universe). */
	@Nullable
	static ItemStack matStackLive(MatRequest aRequest) {
		net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item> tHandle = GTMaterialItems.get(aRequest.prefix(), aRequest.material());
		if (tHandle == null) tHandle = GTMaterialBlocks.get(aRequest.prefix(), aRequest.material());
		if (tHandle == null || !tHandle.isPresent()) return null;
		return new ItemStack(tHandle.get(), (int)gregapi.util.UT.Code.bind(1, 64, aRequest.count())); // UT.Code.bindStack
	}

	/** A display row helper for the smelting display face (the :58 getRecipeFor projection over ONE material). */
	@Nullable
	private static Recipe displayRow(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		ItemStack tInput = matStack(aPrefix, aMaterial, 1);
		if (tInput == null) return null;
		return findRecipeFor(tInput);
	}

	/** The public single-stack face of the on-demand arm (the upstream getRecipeFor(:82) name). */
	@Nullable
	public static Recipe findRecipeFor(ItemStack aInput) {
		GT6RecipeMapCrucible tMap = gregtech6.recipes.GT6RecipeMaps.CRUCIBLE_SMELTING;
		return tMap == null ? null : tMap.findRecipe(null, Long.MAX_VALUE, ItemStack.EMPTY, new FluidStack[0], aInput);
	}
}
