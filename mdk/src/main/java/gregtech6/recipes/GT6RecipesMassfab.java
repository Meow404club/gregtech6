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

package gregtech6.recipes;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.fluid.GTFluids;
import gregtech6.registry.GTMaterialItems;

/**
 * The Matter Fabricator recipe book (task p31-massfab) — the element-disintegration walk
 * of Loader_Recipes_Other.java:969-987, poured into {@link GT6RecipeMaps#MASSFAB}.
 *
 * <p><b>The upstream loop</b> (:969): every material of {@code MATERIAL_ARRAY} with
 * {@code mNeutrons + mProtons > 0 && contains(TD.Atomic.ELEMENT) && !contains(TD.Atomic.
 * ANTIMATTER)} — the :971-975 unit arms (dust/ingot/plate/plateGem/gem, one unit in →
 * {@code mProtons} mB MatterCharged + {@code mNeutrons} mB MatterNeutral, 1 mB = 1
 * proton/neutron) and the :977-981 block arms (blockDust/blockIngot/blockPlate/
 * blockPlateGem/blockGem, ×9 everything). Duration {@code (p+n) × 131072} (:971, the
 * constants the qu-a smoke row pinned: Fe 26p/30n → 7340032), eUt 1. The
 * {@code mProtons < 1}/{@code mNeutrons < 1} arms skip the respective fluid output
 * (upstream {@code tMaterial.mProtons<1?NF:...}).
 *
 * <p><b>The :983-986 fluid half stays pooled</b>: the liquid/gas/plasma addRecipe0 arms
 * ride {@code tMaterial.liquid/gas/plasma(U, T)} — the port's material↔fluid binding is
 * the shallow P29 w4 nominal-spec form, not the upstream per-material fluid factory, so
 * the {@code FL.nonzero} guard would drop most rows anyway; the card scope is the item
 * walk (the spec enumeration). The pool note rides the class doc only.
 *
 * <p><b>The Ender rows are NOT here</b>: :915-928 (Dilithium/AncientDebris → FL.Ender)
 * ship as the {@code massfab.json} smoke rows (the card ruling — static content on the
 * datapack seam, pack-author-editable; the upstream ST.tag family-select circuit is
 * vestigial with the Ender_TE half :897-914 unregistered, so the JSON rows drop the
 * selector — the declared deviation).
 *
 * <p><b>The resolution seam</b>: prefix×material items resolve through
 * {@link GTMaterialItems#get} (null = the upstream {@code mat() → null} silent drop,
 * the implosion convention); the matter fluids resolve through
 * {@link #sMatterFluidResolver} onto the {@link GTFluids#QU_FLUIDS} registrations
 * (fixtures injected offline).
 *
 * <p><b>Load timing</b>: the {@link gregtech6.recipes.GT6RecipesImplosion} form —
 * self-contained MOD-bus listener at FMLCommonSetup.enqueueWork, lazily built table,
 * generation-tracked pour flag (ADR-P18).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6RecipesMassfab {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** The :971 duration constant — (neutrons + protons) × 131072 ticks per unit. */
	public static final long DURATION_PER_NUCLEON = 131072;

	/** The :971-975 unit-prefix arms, upstream order. */
	public static final List<OreDictPrefix> UNIT_PREFIXES = List.of(OP.dust, OP.ingot, OP.plate, OP.plateGem, OP.gem);

	/** The :977-981 block-prefix arms (×9), upstream order. */
	public static final List<OreDictPrefix> BLOCK_PREFIXES = List.of(OP.blockDust, OP.blockIngot, OP.blockPlate, OP.blockPlateGem, OP.blockGem);

	/** The resolution seam: the live registry lookups by default, fixtures injected offline (the implosion form). */
	public static BiFunction<OreDictPrefix, OreDictMaterial, Item> sMaterialItemResolver = GT6RecipesMassfab::resolveItem;

	/** The matter-fluid seam: "charged"/"neutral" → the QU_FLUIDS source (offline stand-ins injectable). */
	public static Function<String, Fluid> sMatterFluidResolver = GT6RecipesMassfab::resolveMatterFluid;

	/** The lazily built element table (MT.init at class-load lesson — the a9027ac form). */
	private static volatile List<OreDictMaterial> sElements = null;

	/**
	 * The transcribed :969 filter walk, captured on first use (one material generation):
	 * {@code mNeutrons + mProtons > 0 && contains(TD.Atomic.ELEMENT) && !contains(TD.Atomic.ANTIMATTER)}.
	 */
	public static List<OreDictMaterial> elements() {
		List<OreDictMaterial> tTable = sElements;
		if (tTable == null) {
			List<OreDictMaterial> tRows = new ArrayList<>();
			for (OreDictMaterial tMaterial : OreDictMaterial.MATERIAL_ARRAY) {
				if (tMaterial == null) continue; // :969
				if (tMaterial.mNeutrons + tMaterial.mProtons <= 0) continue; // :969
				if (!tMaterial.contains(TD.Atomic.ELEMENT)) continue; // :969
				if (tMaterial.contains(TD.Atomic.ANTIMATTER)) continue; // :969
				tRows.add(tMaterial);
			}
			sElements = tTable = List.copyOf(tRows);
		}
		return tTable;
	}

	/** Poured flag — one generation, one pour (upstream loaders run once per JVM). */
	private static boolean sLoaded = false;

	// ADR-P18: the pour-flag joins the GT6RecipeMaps generation (the implosion form).
	static {GT6RecipeMaps.registerGenerationResetHook(GT6RecipesMassfab::resetForTest);}

	/** FMLCommonSetup.enqueueWork — items are registered by this point (unlike ConstructMod). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(GT6RecipesMassfab::load);
	}

	/** Pours the walk into the MASSFAB map. Idempotent; unresolvable rows skip with a count. */
	public static synchronized void load() {
		if (sLoaded) {LOGGER.debug("load() skipped: already poured (generation flag set)"); return;}
		GT6RecipeMaps.init(); // defensive + idempotent: the maps exist from ConstructMod (GTMachines)
		if (GT6RecipeMaps.MASSFAB == null) return; // reset() between init and load — a broken lifecycle
		int tPoured = 0, tSkipped = 0;
		for (OreDictMaterial tMaterial : elements()) {
			for (OreDictPrefix tPrefix : UNIT_PREFIXES) {
				Recipe tRecipe = buildRecipe(tMaterial, tPrefix, 1);
				if (tRecipe == null || GT6RecipeMaps.MASSFAB.addRecipe(tRecipe) == null) {tSkipped++; continue;} // mat() → null / the ghost guard
				tPoured++;
			}
			for (OreDictPrefix tPrefix : BLOCK_PREFIXES) {
				Recipe tRecipe = buildRecipe(tMaterial, tPrefix, 9);
				if (tRecipe == null || GT6RecipeMaps.MASSFAB.addRecipe(tRecipe) == null) {tSkipped++; continue;} // the ×9 block arms
				tPoured++;
			}
		}
		LOGGER.info("GT6 Massfab disintegration recipes poured: {} rows loaded, {} skipped (unresolvable prefix items = upstream mat() null returns)", tPoured, tSkipped);
		sLoaded = true;
	}

	/**
	 * One arm of the :971-981 walk, or null when the prefix×material item does not exist
	 * (the upstream {@code mat() → null} drop). One item in → the two matter fluids out;
	 * a {@code < 1} nucleon count skips its fluid (:971's {@code NF} arms).
	 */
	@Nullable
	public static Recipe buildRecipe(OreDictMaterial aMaterial, OreDictPrefix aPrefix, int aMultiplier) {
		Item tInput = sMaterialItemResolver.apply(aPrefix, aMaterial);
		if (tInput == null) return null; // upstream dust.mat(tMat, 1) → null
		long tDuration = (aMaterial.mNeutrons + aMaterial.mProtons) * DURATION_PER_NUCLEON * aMultiplier; // :971/:977
		Fluid tCharged = sMatterFluidResolver.apply("charged");
		Fluid tNeutral = sMatterFluidResolver.apply("neutral");
		if (tCharged == null || tNeutral == null) return null;
		List<FluidStack> tOutputs = new ArrayList<>();
		if (aMaterial.mProtons >= 1) tOutputs.add(new FluidStack(tCharged, (int)(aMaterial.mProtons * aMultiplier))); // :971
		if (aMaterial.mNeutrons >= 1) tOutputs.add(new FluidStack(tNeutral, (int)(aMaterial.mNeutrons * aMultiplier))); // :971
		return new Recipe(true,
				new ItemStack[] {new ItemStack(tInput, 1)},
				new ItemStack[0],
				new FluidStack[0], tOutputs.toArray(new FluidStack[0]),
				tDuration, 1, 0); // eUt 1 (:971)
	}

	/** The live item lookup (GTMaterialItems.get) — null when the pair has no item-path item (the implosion resolveItem form). */
	@Nullable
	private static Item resolveItem(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		var tHandle = GTMaterialItems.get(aPrefix, aMaterial); // RegistryObject (forge) / DeferredHolder (neo) — the var cross-leg form
		return tHandle == null ? null : tHandle.get();
	}

	/** The live matter-fluid lookup over {@link GTFluids#QU_FLUIDS} (the drying resolver-seam form). */
	@Nullable
	private static Fluid resolveMatterFluid(String aHalf) {
		for (GTFluids.ChemicalFluid tFluid : GTFluids.QU_FLUIDS) {
			if (tFluid.spec.name().equals(aHalf.equals("charged") ? "chargedmatter" : "neutralmatter")) return tFluid.source.get();
		}
		return null;
	}

	/** Test seam: clears the poured flag and the captured table so a fresh generation can re-pour. Public — the BE-package e2e shares the reset. */
	public static void resetForTest() {
		sLoaded = false;
		sElements = null;
	}
}
