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

import javax.annotation.Nullable;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import gregtech6.fluid.GTFluids;

/**
 * The RM.Drying recipe book — task p14-loop-closure-chain (the water family foundation)
 * extended by task p16-drying-rows-backfill: the port counterpart of the
 * {@code RM.Drying} rows of Loader_Recipes_Chem.java:525-532 (the water-to-distilled-water
 * half) — the ice/snow rows :510-522 land with the same card, the Distillery rows
 * :534-541 stay pooled (the circuit-selector-gated half, the distillery card's domain).
 *
 * <p><b>The seven poured water rows</b> (:525-532): {@code Water 10 → DistW 8} (:525, the
 * canonical distillation loop row that closes the P14 loop), then the p16 backfill —
 * {@code SpDew 10 → 8} (:526), {@code MnWtr 10 → 8} (:527), {@code Water_Geothermal 25 →
 * 20} (:528), {@code Water_Boiling 25 → 20} (:529), {@code Hot_Water 25 → 20} (:531) and
 * {@code Cold_Water 25 → 20} (:532) — all EUt 16, duration 16, the buffered
 * {@code addRecipe0(T, ...)} fluid-only shape. Six of the seven inputs are the
 * {@link GTFluids.AQUA_SPECS} registrations of task p16-aqua-fluids (spdew/mnwtr/
 * water_geothermal/water_boiling/hot_water/cold_water); water is vanilla.
 *
 * <p><b>The one pooled water row</b> (:530): {@code Water_Hot} — the IC2 hot-water alias
 * ("ic2hotwater", FL.java:116) is deliberately unregistered in this port (outside the
 * p16-aqua-fluids six), so its row keeps the upstream absent-fluid skip: the resolver
 * answers null and {@link #load()} drops the row with the upstream
 * {@code if (FL.Water_Hot.exists())} guard semantics (the guard sits at the END of the
 * :528 line, before the :529 pour).
 *
 * <p><b>Row shape</b>: fluid-in AND fluid-out, empty item arrays — the {@link RecipeMap#addRecipe}
 * ghost guard does not fire (the fluid leg is a real input). The offline lookup shape
 * mirrors the live machine call exactly: {@code findRecipe(..., tanks, new ItemStack[1])}
 * — the RecipeMap's empty-item-array hard return (RecipeMap.java:138) needs the length-1
 * slot array the machine always passes (TileEntityBasicMachine.java:512, mInputItemsCount
 * slots that may be empty), and the row's zero item inputs pass the stack check trivially.
 *
 * <p><b>Load timing</b> (the GT6RecipesEngineFuels precedent, ADR ruling ②): a self-contained
 * MOD-bus listener pouring at FMLCommonSetup.enqueueWork — by then the DeferredRegisters of
 * {@link GTFluids} have fired, so the distilled-water output resolves. {@code load()} is
 * idempotent per JVM generation; rows that lose their fluid or item skip SILENTLY with a
 * count (must not block the others).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6RecipesDrying {


	private static final Logger LOGGER = LogUtils.getLogger();

	/** The gt6 fluid id keys the table rows reference (the upstream FL shorthands, snake-cased). */
	public static final String FLUID_WATER = "water", FLUID_DISTW = "distw", FLUID_SPDEW = "spdew",
			FLUID_MNWTR = "mnwtr", FLUID_GEOTHERMAL = "water_geothermal", FLUID_BOILING = "water_boiling",
			FLUID_HOT = "water_hot", FLUID_HOT_WATER = "hot_water", FLUID_COLD = "cold_water";

	/**
	 * The fluid seam: the live lookups by default (vanilla water in, the registered
	 * distilled water out, the six p16-aqua-fluids registrations, and the deliberate
	 * null for the unregistered water_hot alias), fixtures injected offline (the
	 * GT6RecipesEngineFuels sFluidResolver precedent).
	 */
	static Function<String, Fluid> sFluidResolver = GT6RecipesDrying::resolveFluid;

	/**
	 * One transcribed upstream row: the input fluid id, the in/out litre amounts and the
	 * note of the Loader_Recipes_Chem.java line. EUt 16 and duration 16 are family
	 * constants (every :525-532 row carries them).
	 */
	public record DryingRow(String note, String input, long inAmount, long outAmount) {}

	/**
	 * The eight transcribed water-family rows, order mirroring the upstream file order
	 * (:525-532). Lazily built — the @EventBusSubscriber class-load at MOD CONSTRUCTION
	 * must not capture registry state (it is pure strings anyway; the laziness keeps the
	 * loaders structurally identical).
	 */
	private static volatile List<DryingRow> sTable = null;

	/** The transcribed rows, captured on first use. */
	public static List<DryingRow> table() {
		List<DryingRow> tTable = sTable;
		if (tTable == null) sTable = tTable = List.of(
		// Loader_Recipes_Chem.java:525 — the Water row, the P14 loop-closure foundation
		new DryingRow(":525", FLUID_WATER     , 10,  8),
		// Loader_Recipes_Chem.java:526-532 — the p16-aqua-fluids six + the :530 pooled alias
		new DryingRow(":526", FLUID_SPDEW     , 10,  8),
		new DryingRow(":527", FLUID_MNWTR     , 10,  8),
		new DryingRow(":528", FLUID_GEOTHERMAL, 25, 20),
		new DryingRow(":529", FLUID_BOILING   , 25, 20),
		new DryingRow(":530", FLUID_HOT       , 25, 20),
		new DryingRow(":531", FLUID_HOT_WATER , 25, 20),
		new DryingRow(":532", FLUID_COLD      , 25, 20));
		return tTable;
	}

	/** Poured flag — one generation, one pour (upstream loaders run once per JVM). */
	private static boolean sLoaded = false;

	/** FMLCommonSetup.enqueueWork — the GTFluids DeferredRegisters have fired by this point. */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(GT6RecipesDrying::load);
	}

	/** Pours the Water row into {@link GT6RecipeMaps#DRYING}. Idempotent; unregistered-fluid rows skip with a count. */
	public static synchronized void load() {
		if (sLoaded) return;
		GT6RecipeMaps.init(); // defensive + idempotent: the map exists from ConstructMod (GTMachines.java:91), tests may race it
		RecipeMap tMap = GT6RecipeMaps.DRYING;
		if (tMap == null) return; // reset() between init and load — a broken lifecycle, nothing to pour into

		int tPoured = 0, tSkipped = 0;
		for (DryingRow tRow : table()) {
			Recipe tRecipe = buildRecipe(tRow);
			if (tRecipe == null) {tSkipped++; continue;} // the absent-fluid silent skip (upstream FL.exists drops)
			tMap.addRecipe(tRecipe);
			tPoured++;
		}
		sLoaded = true;
		LOGGER.info("GT6 Drying poured: {} loaded, {} skipped (unregistered fluid ids, = upstream FL.exists drops)", tPoured, tSkipped);
	}

	/** Row → Recipe, or null when the fluid fails to resolve (the silent-skip semantics). */
	static Recipe buildRecipe(DryingRow aRow) {
		Fluid tInput = sFluidResolver.apply(aRow.input());
		if (tInput == null) return null;
		Fluid tOutput = sFluidResolver.apply(FLUID_DISTW);
		if (tOutput == null) return null;
		// the upstream addRecipe0(T, 16, 16, FL.in.make(in), FL.DistW.make(out), ZL_IS) shape:
		// buffered, fluid-in + fluid-out, no items, EUt 16 duration 16 verbatim (:525-532)
		return new Recipe(true,
				new ItemStack[0], new ItemStack[0],
				new FluidStack[] {new FluidStack(tInput, (int)aRow.inAmount())},
				new FluidStack[] {new FluidStack(tOutput, (int)aRow.outAmount())},
				16, 16, 0);
	}

	/**
	 * The live fluid lookup — vanilla water for the :525 row, the registered distilled
	 * water for the output, the six p16-aqua-fluids registrations for the :526-529/:531-532
	 * rows (the RegistryObjects are live at load() time), and NULL for the water_hot alias
	 * (:530 — "ic2hotwater", FL.java:116, unregistered in this port): a null makes the row
	 * skip exactly like the upstream {@code if (FL.Water_Hot.exists())} guard around its
	 * pour line. Fixtures replace the whole function offline (the tests never touch the
	 * RegistryObjects outside a live registry).
	 */
	@Nullable
	static Fluid resolveFluid(String aFluidId) {
		return switch (aFluidId) {
			case FLUID_WATER       -> Fluids.WATER;
			case FLUID_DISTW       -> GTFluids.DISTILLED_WATER.source.get();
			case FLUID_SPDEW       -> GTFluids.SPDEW.source.get();
			case FLUID_MNWTR       -> GTFluids.MNWTR.source.get();
			case FLUID_GEOTHERMAL  -> GTFluids.WATER_GEOTHERMAL.source.get();
			case FLUID_BOILING     -> GTFluids.WATER_BOILING.source.get();
			case FLUID_HOT_WATER   -> GTFluids.HOT_WATER.source.get();
			case FLUID_COLD        -> GTFluids.COLD_WATER.source.get();
			default -> null; // FLUID_HOT (:530) — the absent-fluid skip, the IC2 alias stands unregistered
		};
	}

	/** Test seam: clears the poured flag so a fresh generation can re-pour. */
	static void resetForTest() {sLoaded = false;}

	private GT6RecipesDrying() {}
}
