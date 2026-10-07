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

import gregapi.util.UT;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import net.minecraftforge.fluids.FluidStack;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.oredict.MaterialGraph;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.oredict.OreDictPrefix;
import gregapi.oredict.configurations.IOreDictConfigurationComponent;
import gregapi.util.CruciblePhysics;
import gregtech6.fluid.GTFluids;
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
 * (task crucible-physics-smeltery). The upstream {@code RM.CrucibleSmelting} is the
 * on-demand {@code RecipeMapSpecialSingleInput} subclass whose {@code getRecipeFor}
 * (RecipeMapCrucible.java:82-97) derives the row from the MATERIAL GRAPH at lookup time —
 * zero static rows, the crucible is a recipe-table machine only in appearance. This port
 * carries the same shape over the {@link gregtech6.recipes.maps.GT6RecipeMapCanner} form
 * (the {@code findRecipe} override precedent, canner-machine): the stored-row scan
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
 * {@link #alloyingDisplayRows} synthesis off the alloy-creation configurations (the
 * GT6_Main.java:453-484 NEI walk, hidden components skipping their configuration per
 * :460), built only when a display consumer asks; {@link #allAlloyingDisplayRows} is the
 * full-universe walk the viewer pages register.
 *
 * <p><b>Declared deviations</b>:
 * <ul>
 * <li>the NEI/JEI output face of the upstream getNEIRecipes override (:50-79) is the
 * display-consumer pool — the port's JEI integration reads {@link #smeltingDisplayRows}
 * instead; the dynamic per-output NEI query vs the flat registered page is the
 * id1353-accepted semantics gap;</li>
 * <li>the OM.ingot/OM.dust OUTPUT ladders (OM.java:460-475) port in their reduced form —
 * ingot/nugget and dust/dustSmall/dustTiny steps only; the 72x/9x block steps the upstream
 * ladders lead with (:462/:471) are not ladder steps here — a direct prefix.mat ask
 * resolves them through the GTMaterialBlocks fallback in {@link #matStackLive}, which is
 * also what puts the :58-67 block forms on the display face;</li>
 * <li>the GT6_Main Air/C/CaCO3 display arms (:461-469) — RESTORED by task
 * crucible-alloying-flux-rows: the C→Coal / CaCO3→Limestone flux third row (:467-468,
 * gated :480-481) and the Air component's fluid-stack face (:461-466, the mB
 * UT.Code.units form — the modern viewers render FluidStack natively per the
 * ViewerMeta doc folded ruling), replacing the upstream FL.display container items.
 * The air FLUID itself is not registered in the port yet (the census precheck miss) —
 * {@link #sAirDisplayFluid} answers null until a fluids card lands gt6:air — and the
 * alloying map still declares zero fluid slots (the RM.java:128 row verbatim,
 * GT6RecipeMaps.init), so the slot face rides a later map-face decision.</li>
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

	/**
	 * The GT6_Main.java:461-465 Air display fluid (the upstream {@code FL.Air} face) for
	 * the alloying walk. The port has NO air fluid registered yet (the census precheck
	 * expected one — only the netherair/enderair dimension airs exist, GTFluids
	 * NAMING_FLUID_SPECS): the live walk answers null, the Air component then renders no
	 * slot at all but never kills its pair, and the fluid slot materializes the moment a
	 * fluids card registers the gt6:air fluid. Tests swap in a stand-in (the id224
	 * static-seam discipline); {@link #DEFAULT_AIR_DISPLAY_FLUID} restores the live form.
	 */
	public static Supplier<Fluid> sAirDisplayFluid = GT6RecipeMapCrucible::airDisplayFluidLive;

	/** The live {@link #sAirDisplayFluid} (kept for the test restore). */
	public static final Supplier<Fluid> DEFAULT_AIR_DISPLAY_FLUID = GT6RecipeMapCrucible::airDisplayFluidLive;

	/** The live air-fluid walk (the GT6RecipesCanner.liveAirFluid tolerance: unbound offline registries answer null, never throw). */
	@Nullable
	static Fluid airDisplayFluidLive() {
		try {return GTFluids.liveFluidSource("air");} catch (RuntimeException tOffline) {return null;}
	}

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
			long tAmount = UT.Code.units((long)tInput.getCount() * tData.prefix().mAmount, U, tMaterial.mTargetSmelting.mAmount, false);
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
	 * The upstream smelting-input prefixes (RecipeMapCrucible.java:58-67), verbatim order:
	 * the four {@code :58-61} forms a CROSS-SOURCE material additionally shows, then the
	 * five {@code :63-67} forms every source shows. A METHOD, not a static-final capture,
	 * ON PURPOSE: {@code OP.init()} regenerates every prefix object (OP.java:737 nulls,
	 * :1214 recreates), and the neoforge junit-fml leg boots the mod — and class-initializes
	 * this class — BEFORE the test flood, so a static capture rides a dead OP generation
	 * and every prefix-identity lookup nulls out. The remaining eight upstream prefixes
	 * (:68-75, chunk … reduced) have no items upstream either (Loader_Items.java:57-171) —
	 * getRecipeFor null-drops them there, the {@link #displayRow} null-gate does here.
	 */
	private static OreDictPrefix[] crossSourcePrefixes() {
		return new OreDictPrefix[] {OP.ingot, OP.blockIngot, OP.gem, OP.blockGem, OP.dust, OP.blockDust, OP.crushed, OP.crushedPurified, OP.crushedCentrifuged};
	}

	/** The {@code :63-67} family — the forms the SELF arm shows too (upstream :62 keeps them outside the {@code tMat != self} branch); per-call for the OP-regeneration reason of {@link #crossSourcePrefixes()}. */
	private static OreDictPrefix[] selfPrefixes() {
		return new OreDictPrefix[] {OP.dust, OP.blockDust, OP.crushed, OP.crushedPurified, OP.crushedCentrifuged};
	}

	/**
	 * The CRUCIBLE_SMELTING display rows of one output material (the upstream
	 * getNEIRecipes :50-79 reduced to the material-graph query): every registered material
	 * that smelts INTO aMaterial shows its cross-source prefix family (RecipeMapCrucible.java:58-67 —
	 * {@link MaterialGraph#targeting} excludes the output itself, so this walk IS the
	 * upstream :57 {@code tMat != self} branch). Prefixes without a representable item
	 * drop out per-row (the {@link #displayRow} null-gate — the upstream getRecipeFor
	 * null shape). Upstream NEI dynamic per-output query vs this flat-page pool is the
	 * id1353-accepted semantics gap. Display-consumer pool face.
	 */
	public static List<Recipe> smeltingDisplayRows(OreDictMaterial aOutput) {
		if (aOutput == null) return Collections.emptyList();
		List<Recipe> rList = new ArrayList<>();
		for (OreDictMaterial tMat : MaterialGraph.targeting(aOutput, MaterialGraph.Process.SMELTING).keySet()) {
			for (OreDictPrefix tPrefix : crossSourcePrefixes()) { // RecipeMapCrucible.java:58-67 verbatim order
				Recipe tRow = displayRow(tPrefix, tMat);
				if (tRow != null) rList.add(tRow);
			}
		}
		return rList;
	}

	/**
	 * The CRUCIBLE_ALLOYING display rows of one alloy (GT6_Main.java:453-484): per
	 * alloy-creation configuration, one fake row over the DUST inputs and one over the
	 * INGOT inputs, output = {@code commonDivider * U} of the alloy, SpecialValue = the
	 * temperature display (the second-highest component melting point, at least the
	 * alloy's own — :478-481), plus the flux third row when a C/CaCO3 component rides the
	 * configuration (:467-468, gated :480-481). Alloys carry their simple composition AND
	 * any explicit creation configurations (Fe's flux groups, the Steel Air pair) — each
	 * configuration shows its own rows, exactly like the upstream client walk.
	 */
	public static List<Recipe> alloyingDisplayRows(OreDictMaterial aAlloy) {
		if (aAlloy == null || aAlloy.mAlloyCreationRecipes.isEmpty()) return Collections.emptyList();
		List<Recipe> rList = new ArrayList<>();
		for (IOreDictConfigurationComponent tConfig : aAlloy.mAlloyCreationRecipes) rList.addAll(alloyConfigRows(aAlloy, tConfig));
		return rList;
	}

	/**
	 * The display rows of ONE alloy-creation configuration — the GT6_Main.java:455-482
	 * inner walk verbatim, translated off the upstream {@code ArrayListNoNulls}: the null
	 * semantics ride explicit guards at the same anchors (a null dust kills THIS
	 * configuration :472, a null ingot is silently skipped :473, a null flux fails the
	 * {@code tAddedSpecial} flag :467-468), and the old port-wide kill arm (:211) is
	 * narrowed accordingly.
	 */
	private static List<Recipe> alloyConfigRows(OreDictMaterial aAlloy, IOreDictConfigurationComponent aConfig) {
		boolean tAddSpecial = false; // :455
		List<Long> tMeltingPoints = new ArrayList<>(); // :457
		List<ItemStack> tDusts = new ArrayList<>(), tIngots = new ArrayList<>(), tSpecial = new ArrayList<>(); // :456
		List<FluidStack> tAirFluids = new ArrayList<>(); // the :461-465 Air displays, the modern FluidStack form
		for (OreDictMaterialStack tComponent : aConfig.getUndividedComponents()) { // :458
			boolean tAddedSpecial = false; // :459
			// :460 — a hidden component skips THIS configuration's rows
			// ({@code if (tMaterial.mMaterial.mHidden) {temp = F; break;}} verbatim)
			if (tComponent.mMaterial.mHidden) return Collections.emptyList();
			// :461-466 — an Air component rides the Air fluid stack (mB) on every row face,
			// adds no item input and no melting point, and never kills the pair (the :211
			// drop arm narrows to the non-Air components it can actually hit)
			if (tComponent.mMaterial == MT.Air) {
				Fluid tAir = sAirDisplayFluid.get();
				if (tAir != null) tAirFluids.add(new FluidStack(tAir, (int)UT.Code.units(tComponent.mAmount, U, 1000, true)));
				continue;
			}
			// :467-468 — C/CaCO3 display their flux at twice the amount on the special row
			// INSTEAD of their own dust (the :474 else-arm skips)
			if (tComponent.mMaterial == MT.C    ) {ItemStack tFlux = dustOrIngot(MT.Coal            , tComponent.mAmount * 2); tAddedSpecial = tFlux != null && tSpecial.add(tFlux);}
			if (tComponent.mMaterial == MT.CaCO3) {ItemStack tFlux = dustOrIngot(MT.STONES.Limestone, tComponent.mAmount * 2); tAddedSpecial = tFlux != null && tSpecial.add(tFlux);}
			tMeltingPoints.add(tComponent.mMaterial.mMeltingPoint); // :470
			ItemStack tDust = dustOrIngot(tComponent.mMaterial, tComponent.mAmount);
			if (tDust == null) return Collections.emptyList(); // :472 — the NoNulls.add null-guard kills THIS configuration
			tDusts.add(tDust);
			ItemStack tIngot = ingotOrDust(tComponent.mMaterial, tComponent.mAmount);
			if (tIngot != null) tIngots.add(tIngot); // :473 — upstream NoNulls skips a null silently, no kill
			if (tAddedSpecial) tAddSpecial = true; else tSpecial.add(tDust); // :474
		}
		Collections.sort(tMeltingPoints); // :476
		// :478-481 — the special value is the SECOND-highest component melting point at least
		long tSpecialValue = tMeltingPoints.size() > 1
				? Math.max(tMeltingPoints.get(tMeltingPoints.size() - 2), aAlloy.mMeltingPoint)
				: aAlloy.mMeltingPoint;
		ItemStack tOutput = ingotOrDust(aAlloy, aConfig.getCommonDivider() * U); // :478 — the CONFIGURATION's divider
		if (tOutput == null) return Collections.emptyList();
		List<Recipe> rList = new ArrayList<>();
		rList.add(fakeRow(tDusts, tAirFluids, tOutput, tSpecialValue)); // :478 the dust row
		rList.add(fakeRow(tIngots, tAirFluids, tOutput, tSpecialValue)); // :479 the ingot row
		if (tAddSpecial) rList.add(fakeRow(tSpecial, tAirFluids, tOutput, tSpecialValue)); // :480-481 the flux row
		return rList;
	}

	/** One fake display row (the addFakeRecipe(F, ...) shape, :478-481): not findable, display only. */
	private static Recipe fakeRow(List<ItemStack> aInputs, List<FluidStack> aFluidInputs, ItemStack aOutput, long aSpecialValue) {
		Recipe rRecipe = new Recipe(false, aInputs.toArray(new ItemStack[0]), new ItemStack[] {aOutput},
				aFluidInputs.isEmpty() ? null : aFluidInputs.toArray(new FluidStack[0]), null, 0, 0, aSpecialValue);
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
		//? if forge {
		net.minecraftforge.registries.RegistryObject<net.minecraft.world.item.Item> tHandle = GTMaterialItems.get(aRequest.prefix(), aRequest.material());
		if (tHandle == null) tHandle = GTMaterialBlocks.get(aRequest.prefix(), aRequest.material());
		if (tHandle == null || !tHandle.isPresent()) return null;
		//?} else {
		/*net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.item.Item, net.minecraft.world.item.Item> tHandle = GTMaterialItems.get(aRequest.prefix(), aRequest.material());
		if (tHandle == null) tHandle = GTMaterialBlocks.get(aRequest.prefix(), aRequest.material());
		if (tHandle == null || !tHandle.isBound()) return null;
		//21.1: the GTMaterialItems.get swap returns DeferredHolder on this leg (the
		//GTMultiBlockCommand input-arm idiom; isBound is the 21.1 bound probe).
		*///?}
		return new ItemStack(tHandle.get(), (int)gregapi.util.UT.Code.bind(1, 64, aRequest.count())); // UT.Code.bindStack
	}

	/** A display row helper for the smelting display face (the :58 getRecipeFor projection over ONE material). */
	@Nullable
	private static Recipe displayRow(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		ItemStack tInput = matStack(aPrefix, aMaterial, 1);
		if (tInput == null) return null;
		Recipe tLive = findRecipeFor(tInput);
		if (tLive == null) return null;
		// :96 — the NEI face pair re-pinned at the DISPLAY layer only: SpecialValue =
		// mMeltingPoint (renders "Temperature: N K") and duration 0, while the live
		// findRecipe arm keeps duration = mMeltingPoint for the machine side (risk ②
		// ruling: findRecipe's live semantics untouched — this wrapper is the display fork).
		Recipe rRow = new Recipe(false, tLive.mInputs, tLive.mOutputs, null, null, 0, 0, aMaterial.mMeltingPoint);
		rRow.mFakeRecipe = true;
		return rRow;
	}

	/**
	 * The full CRUCIBLE_SMELTING display face — the enumeration the viewer page registers:
	 * the SELF rows of every registered material over the RecipeMapCrucible.java:63-67 dust
	 * family (the upstream getNEIRecipes self arm sits OUTSIDE its {@code tMat != self}
	 * skip, so "dust iron → ingot iron" is the page's core row) plus every cross-source
	 * row family of {@link #smeltingDisplayRows}. The hidden gate rides the SELF arm:
	 * upstream self rows only surfaced on the material's own NEI page — unreachable for a
	 * hidden material — while this global walk would surface them on the public page.
	 * Materials without representable items drop out through the {@link #displayRow}
	 * null-gates. Linear in the registry size (the ViewerMeta registration-cost ruling's
	 * shape).
	 */
	public static List<Recipe> allSmeltingDisplayRows() {
		List<Recipe> rList = new ArrayList<>();
		for (OreDictMaterial tMat : MaterialRegistry.INSTANCE.MATERIAL_MAP.values()) {
			if (tMat.mHidden) continue; // :460 spirit — hidden materials have no public page to mirror
			for (OreDictPrefix tPrefix : selfPrefixes()) { // RecipeMapCrucible.java:63-67 verbatim order
				Recipe tSelf = displayRow(tPrefix, tMat);
				if (tSelf != null) rList.add(tSelf);
			}
			rList.addAll(smeltingDisplayRows(tMat));
		}
		return rList;
	}

	/**
	 * The full CRUCIBLE_ALLOYING display face — the GT6_Main.java:453-484 client walk
	 * verbatim: every registered alloy × its creation configurations, hidden components
	 * skipping their configuration (the :460 gate inside {@link #alloyingDisplayRows}).
	 */
	public static List<Recipe> allAlloyingDisplayRows() {
		List<Recipe> rList = new ArrayList<>();
		for (OreDictMaterial tAlloy : OreDictMaterial.ALLOYS) rList.addAll(alloyingDisplayRows(tAlloy));
		return rList;
	}

	/** The public single-stack face of the on-demand arm (the upstream getRecipeFor(:82) name). */
	@Nullable
	public static Recipe findRecipeFor(ItemStack aInput) {
		GT6RecipeMapCrucible tMap = gregtech6.recipes.GT6RecipeMaps.CRUCIBLE_SMELTING;
		return tMap == null ? null : tMap.findRecipe(null, Long.MAX_VALUE, ItemStack.EMPTY, new FluidStack[0], aInput);
	}
}
