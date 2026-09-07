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

import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import gregtech6.fluid.GTFluids;
import gregtech6.registry.GT6SprayCans;

/**
 * The RM.Canner spray-can refill pour — task p24-canner-machine, the port counterpart of
 * the {@code RM.Canner.addRecipe1} rows of MultiItemRandomTools.java:246 (the 16 colour
 * refills, the :242 loop body) and :272 (the paint-remover refill). Everything else the
 * upstream Canner map eats rides the {@link GT6RecipeMapCanner} dynamic arms (R1).
 *
 * <p><b>Row shape</b> (the two upstream lines verbatim over the port Recipe ctor):
 * buffered T, EUt 16, duration 256, item input = the empty spray can count 1, fluid input
 * = the chemical dye at {@code 16 * L} = 2304 mB (the R4 mB 1:1 ruling — 1.7.10
 * FluidStack.amount IS mB, CS.java:129 L = 144, the FL.mul(..., 16) of :246; chlorine is
 * the MT.Cl.fluid(16*U, T) of :272, the same 2304 mB), NO fluid output (NF), item output
 * = the full can count 1. The full-can output carries ZERO NBT (the R5 ruling): the colour
 * is the item identity (one {@code gt6:spray_paint_<GTSprayCanItem.DYE_IDS[i]>} item per
 * dye, GT6SprayCans.SPRAY_PAINTS) and a fresh GTSprayCanItem is IMPLICITLY full
 * ({@code gt.remaining} is only ever written on first use) — so {@code new ItemStack(item)}
 * is the exact upstream IL.SPRAY_CAN_DYES[i].get(1).
 *
 * <p><b>Seams</b> (the GT6RecipesDistillery shape): the fluid resolvers are live by
 * default (the task p24-dye-chemical-fluids registrations) and the ITEM resolvers close
 * over the GT6SprayCans RegistryObjects — injected offline where the RegistryObjects are
 * unbound (the sCircuitResolver precedent). Every resolver is consulted at load()/lookup
 * time only; the static table is pure data (the @EventBusSubscriber class-load lesson).
 *
 * <p><b>Load timing</b>: a self-contained MOD-bus listener pouring at
 * FMLCommonSetup.enqueueWork — the fluid and item DeferredRegisters have fired by then.
 * {@code load()} is idempotent per JVM generation; an unresolvable row skips SILENTLY
 * with a count (the upstream {@code FL.exists()} drop semantics, the Distillery precedent).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6RecipesCanner {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** The refill row fluid amount: {@code 16 * L} = 16 × 144 = 2304 mB (MultiItemRandomTools.java:246 FL.mul / :272 MT.Cl.fluid(16*U), the R4 ruling). */
	public static final int REFILL_MB = 16 * 144;

	/** The EUt column of every refill row (:246/:272, the first addRecipe1 argument). */
	public static final long REFILL_EUT = 16;

	/** The duration column of every refill row (:246/:272, the second addRecipe1 argument). */
	public static final long REFILL_DURATION = 256;

	/**
	 * The dye-fluid seam: index i → the {@code gt6:dye_chemical_<DYE_IDS[i]>} source fluid
	 * (the live default = the p24-dye-chemical-fluids registration), fixtures injected
	 * offline. Public — the row test drives the pour through it.
	 */
	public static IntFunction<Fluid> sDyeFluidResolver = aIndex -> GTFluids.DYE_CHEMICALS.get(aIndex).source.get();

	/** The chlorine seam: the {@code gt6:chlorine} source fluid (the R3 standalone row), fixtures injected offline. */
	public static Supplier<Fluid> sChlorineResolver = () -> GTFluids.CHLORINE.get();

	/** The empty-can seam ({@code gt6:spray_can_empty}, upstream IL.Spray_Empty :235), fixtures injected offline. */
	public static Supplier<ItemStack> sEmptyCanResolver = () -> new ItemStack(GT6SprayCans.SPRAY_CAN_EMPTY.get());

	/** The full-can seam: dye index i → the {@code gt6:spray_paint_<DYE_IDS[i]>} full can (upstream IL.SPRAY_CAN_DYES[i] :243), fixtures injected offline. */
	public static IntFunction<ItemStack> sSprayPaintResolver = aIndex -> new ItemStack(GT6SprayCans.SPRAY_PAINTS.get(aIndex).get());

	/** The remover seam ({@code gt6:spray_paint_remover}, upstream IL.Spray_Color_Remover :269), fixtures injected offline. */
	public static Supplier<ItemStack> sRemoverResolver = () -> new ItemStack(GT6SprayCans.SPRAY_PAINT_REMOVER.get());

	/** The dye indices the 16 refill rows walk (0..15, the :242 loop). */
	public static final List<Integer> DYE_INDICES = List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15);

	/** Poured flag — one generation, one pour (upstream loaders run once per JVM). */
	private static boolean sLoaded = false;

	// ADR-P18: the pour-flag joins the GT6RecipeMaps generation (the Distillery/BurnFuels form).
	static {GT6RecipeMaps.registerGenerationResetHook(GT6RecipesCanner::resetForTest);}

	/** FMLCommonSetup.enqueueWork — the fluid/item DeferredRegisters have fired by this point. */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(GT6RecipesCanner::load);
	}

	/**
	 * Pours the 17 refill rows into {@link GT6RecipeMaps#CANNER}. Idempotent; an
	 * unresolvable row skips with a count (the upstream FL.exists drops).
	 */
	public static synchronized void load() {
		if (sLoaded) {LOGGER.debug("load() skipped: already poured (generation flag set)"); return;}
		GT6RecipeMaps.init(); // defensive + idempotent: the map exists from ConstructMod (GTMachines.java), tests may race it
		gregtech6.recipes.maps.GT6RecipeMapCanner tMap = GT6RecipeMaps.CANNER;
		if (tMap == null) return; // reset() between init and load — a broken lifecycle, nothing to pour into

		int tPoured = 0, tSkipped = 0;
		for (int i : DYE_INDICES) {
			Recipe tRecipe = refillRecipe(i);
			if (tRecipe == null) {tSkipped++; continue;} // the absent-fluid/item silent skip
			tMap.addRecipe(tRecipe);
			tPoured++;
		}
		Recipe tRemover = removerRecipe();
		if (tRemover == null) tSkipped++;
		else {tMap.addRecipe(tRemover); tPoured++;}
		sLoaded = true;
		LOGGER.info("GT6 Canner poured: {} loaded, {} skipped (unregistered dye/chlorine/can ids, = upstream FL.exists drops)", tPoured, tSkipped);
	}

	/**
	 * The MultiItemRandomTools.java:246 row for dye index i — buffered T, EUt 16, duration
	 * 256, empty can in, {@code dye_chemical_<i>} × 2304 mB in, full can out (ZERO NBT, the
	 * R5 ruling). Null when any leg fails to resolve (the silent skip).
	 */
	@Nullable
	static Recipe refillRecipe(int aIndex) {
		Fluid tDye = sDyeFluidResolver.apply(aIndex);
		if (tDye == null) return null;
		ItemStack tEmpty = sEmptyCanResolver.get();
		if (tEmpty == null || tEmpty.isEmpty()) return null;
		ItemStack tFull = sSprayPaintResolver.apply(aIndex);
		if (tFull == null || tFull.isEmpty()) return null;
		// upstream: RM.Canner.addRecipe1(T, 16, 256, IL.Spray_Empty.get(1), FL.mul(DYE_FLUIDS_CHEMICAL[i], 16), NF, IL.SPRAY_CAN_DYES[i].get(1))
		return new Recipe(true,
				new ItemStack[] {tEmpty}, new ItemStack[] {tFull},
				new FluidStack[] {new FluidStack(tDye, REFILL_MB)},
				null,
				REFILL_DURATION, REFILL_EUT, 0);
	}

	/**
	 * The MultiItemRandomTools.java:272 row — buffered T, EUt 16, duration 256, empty can
	 * in, {@code chlorine} × 2304 mB in ({@code MT.Cl.fluid(16*U, T)} — the R3 standalone
	 * chlorine registration), paint remover out. Null when any leg fails to resolve.
	 */
	@Nullable
	static Recipe removerRecipe() {
		Fluid tChlorine = sChlorineResolver.get();
		if (tChlorine == null) return null;
		ItemStack tEmpty = sEmptyCanResolver.get();
		if (tEmpty == null || tEmpty.isEmpty()) return null;
		ItemStack tRemover = sRemoverResolver.get();
		if (tRemover == null || tRemover.isEmpty()) return null;
		// upstream: RM.Canner.addRecipe1(T, 16, 256, IL.Spray_Empty.get(1), MT.Cl.fluid(16*U, T), NF, IL.Spray_Color_Remover.get(1))
		return new Recipe(true,
				new ItemStack[] {tEmpty}, new ItemStack[] {tRemover},
				new FluidStack[] {new FluidStack(tChlorine, REFILL_MB)},
				null,
				REFILL_DURATION, REFILL_EUT, 0);
	}

	/** Test seam: clears the poured flag so a fresh generation can re-pour (public — the cross-domain e2e drives it). */
	public static void resetForTest() {sLoaded = false;}

	private GT6RecipesCanner() {}
}
