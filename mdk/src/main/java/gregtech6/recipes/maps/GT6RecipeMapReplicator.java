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
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.fluid.FluidBridge;
import gregtech6.fluid.GTFluids;
import gregtech6.items.GT6UsbSticks;
import gregtech6.recipes.Recipe;
import gregtech6.recipes.RecipeMap;
import gregtech6.registry.GTMaterialItems;

/**
 * The Matter Replicator map — the port of upstream
 * {@code gregapi/recipes/maps/RecipeMapReplicator.java} (task p32-qu-scanner-replicator,
 * the GT6RecipeMapCanner judged form: the DYNAMIC replication semantics port IN FULL over
 * the static rows — the static stock pours through the datapack seam and the loaders
 * independently of this arm).
 *
 * <p><b>The runtime arm</b> (:54-86): among the item inputs sits a T3 USB stick whose
 * {@code gt.usb.data} compound carries a {@code gt.replicator.data} material id short
 * (:66/:80-83, through {@link GT6UsbSticks#materialOf}); the stick must carry DATA (the
 * scanner-written material face). The synthesis calls {@link #getReplicatorRecipe}:
 * <ul>
 * <li>the UUM gate (:89) — the material must be UUM-synthesisable and not
 *     antimatter;</li>
 * <li>the matter legs (:90) — {@code neutrons} mB of Neutral Matter plus
 *     {@code protons} mB of Charged Matter (1 mB = 1 nucleon; a {@code <= 0} count
 *     skips its leg, the {@code NF} arm);</li>
 * <li>the power (:91) — {@code (protons + neutrons) × 256} eUt at duration 1 (:94);</li>
 * <li>the room-temperature liquid arm (:92-95) — a material liquid at
 *     {@code DEF_ENV_TEMP} replicates to its fluid (one unit);</li>
 * <li>the item walk (:96-103) — mPriorityPrefix, then gem → plateGem → ingot → plate →
 *     nugget×9 → chunkGt×4 → dust → dustTiny×9 → dustSmall×4 → stick×2;</li>
 * <li>the fluid walk (:104-108) — liquid → gas → plasma, else the material is not
 *     replicable (null).</li>
 * </ul>
 * The recipe is built ON COPIES, never cached ({@code aCanBeBuffered = F}, the upstream
 * {@code setNoBuffering} face) — and the stick input is the upstream
 * {@code ST.amount(0, aUSB)} never-consumed marker: the port carries the stick at count 1
 * and lets {@code Recipe.sNotConsumable} claim data-bearing sticks in the consume pass
 * (the match half is untouched — the machine must HOLD the stick).
 *
 * <p><b>Declared deviations</b> (the canner form):
 * <ul>
 * <li>the USB-CABLE arm (:67-77, {@code OD_USB_CABLES} + {@code ITileEntityUSBPort})
 *     stays POOLED — the port registers no cable items and no USB-port machine face, so
 *     the branch is structurally dead here;</li>
 * <li>the {@code GAPI_POST.mFinishedServerStarted} guard and the
 *     {@code containsInput} wide face (:116-117) ride the same canner-form
 *     deviations;</li>
 * <li>the voltage WINDOW lives in the stored-row scan only ({@code RecipeMap.findRecipe}'s
 *     aSize gate): the synthesis answers BEFORE any window consult (upstream :55-57
 *     verbatim — the override never reads aSize), so a below-recipe rung finds the row and
 *     gates it on the machine-side energy budget instead; the per-machine budget face
 *     rides the machine pool (the upstream :743 bind);</li>
 * <li>the fluid arms resolve through the port's material→fluid seam
 *     ({@link GTFluids#specOf}) at the fixed molten L-per-unit / bucket-per-unit amounts —
 *     the upstream per-material fluid factory and its unit bookkeeping have no wider port
 *     counterpart (the massfab :983-986 pool note); the plasma walk leg has no port
 *     binding at all (only the He/N plasma rows exist, no generic seam).</li>
 * </ul>
 */
public class GT6RecipeMapReplicator extends RecipeMap {

	/** The :94 duration — one tick per replication. */
	public static final long REPLICATION_DURATION = 1;

	/** The :91 power factor — (protons + neutrons) × 256 QU per replication. */
	public static final long REPLICATION_EUT_PER_NUCLEON = 256;

	/** The room-temperature liquid gate (:92) — upstream CS.java:135 DEF_ENV_TEMP = C + 20 = 293. */
	public static final long DEF_ENV_TEMP = 293;

	/** The resolution seam for the item walk (the massfab/fusion resolver form; fixtures inject offline). */
	public static BiFunction<OreDictPrefix, OreDictMaterial, Item> sMaterialItemResolver = GT6RecipeMapReplicator::resolveItem;

	/** The matter-fluid seam: "charged"/"neutral" → the QU_FLUIDS source (the massfab form). */
	public static Function<String, Fluid> sMatterFluidResolver = GT6RecipeMapReplicator::resolveMatterFluid;

	public GT6RecipeMapReplicator(@Nullable Collection<Recipe> aRecipeList, String aNameInternal, String aNameLocal, @Nullable String aNameNEI,
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
		// :55-57 — the stored rows win (the :929 Ender row + the redstone six)
		Recipe rRecipe = super.findRecipe(aLastRecipe, aSize, aSpecialSlot, aFluids, aInputs);
		if (rRecipe != null || aInputs == null || aFluids == null) return rRecipe;

		// :59-84 — the T3 stick with DATA picks its material; the cable arm :67-77 stays
		// POOLED (the class doc). The first stick decides (upstream :62's tData == null
		// selector); a data-less stick just falls through (:80's return rRecipe arm).
		for (ItemStack aInput : aInputs) {
			if (aInput == null || aInput.isEmpty() || !GT6RecipeMapScannerMolecular.isScannerStick(aInput)) continue;
			OreDictMaterial tMaterial = GT6UsbSticks.materialOf(aInput); // :81-83 — the id short + the exists gate
			if (tMaterial == null) return rRecipe;
			Recipe tRecipe = getReplicatorRecipe(tMaterial, aInput);
			return tRecipe != null ? tRecipe : rRecipe;
		}
		return rRecipe;
	}

	/**
	 * The :88-114 replication synthesis, or null when the material is not replicable. The
	 * USB input rides at count 1 with the never-consumed claim ({@code Recipe.sNotConsumable}
	 * — the upstream {@code ST.amount(0, aUSB)} zero-consume marker, the port count-0
	 * incompatibility).
	 */
	@Nullable
	public static Recipe getReplicatorRecipe(OreDictMaterial aMaterial, ItemStack aUSB) {
		// :89 — the UUM gate
		if (aMaterial == null || !aMaterial.contains(TD.Processing.UUM) || aMaterial.contains(TD.Atomic.ANTIMATTER)) return null;
		// :90 — the two matter legs, the NF arms skipped
		Fluid tNeutral = sMatterFluidResolver.apply("neutral");
		Fluid tCharged = sMatterFluidResolver.apply("charged");
		if (tNeutral == null || tCharged == null) return null; // the fluids land before their consumers — a null here is a broken lifecycle
		List<FluidStack> tMatters = new ArrayList<>(2);
		if (aMaterial.mNeutrons > 0) tMatters.add(new FluidStack(tNeutral, (int)aMaterial.mNeutrons));
		if (aMaterial.mProtons > 0) tMatters.add(new FluidStack(tCharged, (int)aMaterial.mProtons));
		long tPower = (aMaterial.mProtons + aMaterial.mNeutrons) * REPLICATION_EUT_PER_NUCLEON; // :91
		// the stick input — the never-consumed marker at count 1 (the sNotConsumable claim)
		ItemStack tStick = aUSB.copy();
		tStick.setCount(1);
		// :92-95 — the room-temperature liquid arm (the molten carrier, one L-unit)
		if (aMaterial.mMeltingPoint <= DEF_ENV_TEMP) {
			FluidStack tFluidOutput = materialFluid(aMaterial, true);
			if (tFluidOutput != null) return replicatorRecipe(tStick, null, tMatters.toArray(new FluidStack[0]), tFluidOutput, tPower);
		}
		// :96-103 — the item walk, upstream prefix order
		ItemStack tOutput = materialItem(aMaterial.mPriorityPrefix, aMaterial, 1);
		if (tOutput == null) tOutput = materialItem(OP.gem, aMaterial, 1);
		if (tOutput == null) tOutput = materialItem(OP.plateGem, aMaterial, 1);
		if (tOutput == null) tOutput = materialItem(OP.ingot, aMaterial, 1);
		if (tOutput == null) tOutput = materialItem(OP.plate, aMaterial, 1);
		if (tOutput == null) tOutput = materialItem(OP.nugget, aMaterial, 9);
		if (tOutput == null) tOutput = materialItem(OP.chunkGt, aMaterial, 4);
		if (tOutput == null) tOutput = materialItem(OP.dust, aMaterial, 1);
		if (tOutput == null) tOutput = materialItem(OP.dustTiny, aMaterial, 9);
		if (tOutput == null) tOutput = materialItem(OP.dustSmall, aMaterial, 4);
		if (tOutput == null) tOutput = materialItem(OP.stick, aMaterial, 2);
		if (tOutput != null) return replicatorRecipe(tStick, tOutput, tMatters.toArray(new FluidStack[0]), null, tPower);
		// :104-108 — the fluid walk: liquid (the molten arm above) → gas; the plasma leg has
		// no port binding (the class doc). One bucket per unit (the port gas-row carrier).
		FluidStack tFluidOutput = materialFluid(aMaterial, false);
		if (tFluidOutput == null) return null; // :107 — not replicable
		return replicatorRecipe(tStick, null, tMatters.toArray(new FluidStack[0]), tFluidOutput, tPower);
	}

	/** The common recipe tail — duration 1, never buffered (the :94/:111 setNoBuffering face). */
	private static Recipe replicatorRecipe(ItemStack aStick, @Nullable ItemStack aItemOutput, FluidStack[] aMatters, @Nullable FluidStack aFluidOutput, long aPower) {
		return new Recipe(false,
				new ItemStack[] {aStick},
				aItemOutput == null ? new ItemStack[0] : new ItemStack[] {aItemOutput},
				aMatters, aFluidOutput == null ? null : new FluidStack[] {aFluidOutput},
				REPLICATION_DURATION, aPower, 0);
	}

	/** One arm of the :96-103 walk, or null when the pair has no item (the upstream {@code mat() → null} drop). */
	@Nullable
	private static ItemStack materialItem(@Nullable OreDictPrefix aPrefix, OreDictMaterial aMaterial, int aCount) {
		if (aPrefix == null) return null;
		Item tItem = sMaterialItemResolver.apply(aPrefix, aMaterial);
		return tItem == null ? null : new ItemStack(tItem, aCount);
	}

	/**
	 * The :92-95/:104-108 fluid arms over the port material→fluid seam: {@code aMolten} =
	 * the {@code <name>_molten} carrier at the TCon L-per-unit convention (144 mB, the
	 * {@link FluidBridge#L_PER_MOLTEN_UNIT}); the gas walk the bare {@code <name>} carrier
	 * at one bucket. Null when the carrier is not registered (the upstream {@code FL.zero}
	 * miss face).
	 */
	@Nullable
	private static FluidStack materialFluid(OreDictMaterial aMaterial, boolean aMolten) {
		GTFluids.ChemicalFluidSpec tSpec = GTFluids.specOf(aMaterial, aMolten);
		if (tSpec == null) return null;
		for (GTFluids.ChemicalFluid tChemical : GTFluids.CHEMICALS) {
			if (tChemical.spec.equals(tSpec)) return new FluidStack(tChemical.source.get(),
					aMolten ? (int)FluidBridge.L_PER_MOLTEN_UNIT : 1000);
		}
		return null;
	}

	/** The live item lookup (the massfab resolveItem form). */
	@Nullable
	private static Item resolveItem(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		var tHandle = GTMaterialItems.get(aPrefix, aMaterial); // RegistryObject (forge) / DeferredHolder (neo) — the var cross-leg form
		return tHandle == null ? null : tHandle.get();
	}

	/** The live matter-fluid lookup over {@link GTFluids#QU_FLUIDS} (the massfab resolveMatterFluid form). */
	@Nullable
	private static Fluid resolveMatterFluid(String aHalf) {
		for (GTFluids.ChemicalFluid tFluid : GTFluids.QU_FLUIDS) {
			if (tFluid.spec.name().equals(aHalf.equals("charged") ? "chargedmatter" : "neutralmatter")) return tFluid.source.get();
		}
		return null;
	}
}
