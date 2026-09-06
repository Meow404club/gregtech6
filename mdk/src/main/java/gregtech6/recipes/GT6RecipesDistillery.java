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
import gregtech6.item.GT6Circuits;

/**
 * The RM.Distillery water-family pour — task p16-distillery-family ③, the port counterpart
 * of the {@code RM.Distillery.addRecipe1} rows of Loader_Recipes_Chem.java:534-541 (the
 * circuit-selector-gated half of the DISTILLERY map; the GT6RecipesDrying pour covers the
 * parallel {@code addRecipe0} DRYING rows :525-532 — different map, zero file overlap).
 *
 * <p><b>The upstream census</b> (the card's census step, {@code grep -rn "RM.Distillery"}
 * over the 1.7.10 tree): 306 recipe rows total — Potions 252 (Loader_Recipes_Potions, every
 * row a potion fluid this port has not), Food 23 (Loader_Recipes_Food, the honey/juice/sap/
 * sauce family), Chem 21 (:333-347 biomass/oil grades + :534-541 the water family),
 * Temporary 9 (the nether-lily potion rows), Other 2, Crops 1, plus 3 mod-compat rows
 * (BuildCraft/Ganys). EVERY row outside :534-541 depends on fluids or items with no port
 * registration (potions, honeys, biomass, the FL.Oil_* grades, lube) — those stay pooled
 * with the upstream absent-dependency semantics, exactly like the seven DRYING rows the
 * p14 loop-closure card left pooled. What UNLOCKED the water family is this card's own ①:
 * every :534-541 row carries {@code ST.tag(0)} — the Integrated Circuit at config 0 — as
 * its item input (the RM.java:70 mMinimalInputItems=1 slot filler; the Chem.java:333-vs-:346
 * rows show the selector routing: tag(0) vs tag(1) rows on the SAME fluid live in different
 * upstream hash buckets, and the port's {@code Damage}-tagged recipe inputs route the linear
 * scan the same way).
 *
 * <p><b>The eight transcribed rows</b> (:534-541, all EUt 16, duration 16, Water-family in,
 * DistW out): Water 10→8, SpDew 10→8, MnWtr 10→8, Geothermal/Boiling/Hot/HotWater/Cold
 * 25→20. SEVEN pour — water (vanilla) and distw plus the five registered aqua fluids of
 * task p16-aqua-fluids (spdew/mnwtr/water_geothermal/water_boiling/hot_water/cold_water);
 * ONE skips: {@code water_hot} (:539, the "ic2hotwater" IC2 alias, FL.java:116) has NO port
 * registration — the aqua card kept it outside the six on purpose, and the null resolver
 * arm reproduces the upstream {@code if (FL.Water_Hot.exists())} drop verbatim
 * (GT6RecipesDrying's silent-skip precedent).
 *
 * <p><b>Row shape</b>: item input = the circuit selector at config 0 count 1 ({@link
 * GT6Circuits#selector} — the never-consumed semantics ride Recipe.checkStacksEqual's
 * identity-skip, the ① deviation), fluid in AND out, empty item outputs — the {@link
 * RecipeMap#addRecipe} ghost guard does not fire (both legs are real). The offline lookup
 * shape mirrors the live machine call: {@code findRecipe(..., tanks, new ItemStack[1])}.
 *
 * <p><b>Load timing</b> (the GT6RecipesDrying precedent): a self-contained MOD-bus listener
 * pouring at FMLCommonSetup.enqueueWork — the fluid and item DeferredRegisters have fired by
 * then. {@code load()} is idempotent per JVM generation; the unregistered-fluid row skips
 * SILENTLY with a count.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6RecipesDistillery {


	private static final Logger LOGGER = LogUtils.getLogger();

	/** The gt6 fluid id keys the table rows reference (the upstream FL shorthands, snake-cased). */
	public static final String FLUID_WATER = "water", FLUID_DISTW = "distw", FLUID_SPDEW = "spdew",
			FLUID_MNWTR = "mnwtr", FLUID_GEOTHERMAL = "water_geothermal", FLUID_BOILING = "water_boiling",
			FLUID_HOT = "water_hot", FLUID_HOT_WATER = "hot_water", FLUID_COLD = "cold_water";

	/**
	 * The fluid seam: the live lookups by default (vanilla water in, the registered
	 * distilled water out, the aqua family of task p16-aqua-fluids, the census null for the
	 * unregistered water_hot alias), fixtures injected offline (the GT6RecipesDrying
	 * sFluidResolver precedent). Public — the machines-package row-test e2e drives the pour
	 * through it (the GTMachines.applyRow public-test-seam precedent).
	 */
	public static Function<String, Fluid> sFluidResolver = GT6RecipesDistillery::resolveFluid;

	/**
	 * The circuit seam: the live selector stack ({@link GT6Circuits#selector(int)} — every
	 * :534-541 row carries config 0), fixtures injected offline (the RegistryObjects are
	 * unbound outside a live registry, the p16-machine-fluid-gui offline lesson). Public —
	 * the same cross-domain test seam.
	 */
	public static Function<Integer, ItemStack> sCircuitResolver = GT6Circuits::selector;

	/**
	 * One transcribed upstream row: the input fluid id, the in/out litre amounts and the note
	 * of the Loader_Recipes_Chem.java line. EUt 16, duration 16 and the tag(0) circuit are
	 * family constants (every :534-541 row carries them).
	 */
	public record DistilleryRow(String note, String input, long inAmount, long outAmount) {}

	/**
	 * The eight transcribed water-family rows, order mirroring the upstream file order
	 * (:534-541). Lazily built — the @EventBusSubscriber class-load at MOD CONSTRUCTION must
	 * not capture registry state (it is pure strings anyway; the laziness keeps the loaders
	 * structurally identical).
	 */
	private static volatile List<DistilleryRow> sTable = null;

	/** The transcribed rows, captured on first use. */
	public static List<DistilleryRow> table() {
		List<DistilleryRow> tTable = sTable;
		if (tTable == null) sTable = tTable = List.of(
		// Loader_Recipes_Chem.java:534-541 — the circuit-selector water family, all tag(0)
		new DistilleryRow(":534", FLUID_WATER     , 10,  8),
		new DistilleryRow(":535", FLUID_SPDEW     , 10,  8),
		new DistilleryRow(":536", FLUID_MNWTR     , 10,  8),
		new DistilleryRow(":537", FLUID_GEOTHERMAL, 25, 20),
		new DistilleryRow(":538", FLUID_BOILING   , 25, 20),
		// Loader_Recipes_Chem.java:539 — the IC2 water_hot alias: NO port fluid (the aqua
		// card's declared outside-the-six), the upstream if (FL.Water_Hot.exists()) drop
		new DistilleryRow(":539", FLUID_HOT       , 25, 20),
		new DistilleryRow(":540", FLUID_HOT_WATER , 25, 20),
		new DistilleryRow(":541", FLUID_COLD      , 25, 20));
		return tTable;
	}

	/** Poured flag — one generation, one pour (upstream loaders run once per JVM). */
	private static boolean sLoaded = false;

	// ADR-P18: the pour-flag joins the GT6RecipeMaps generation — a bare GT6RecipeMaps.reset()
	// (a dozen unpaired test call sites) must retire the flag WITH the maps, or load() silently
	// early-returns on the "maps cleared × flag set" poison state.
	static {GT6RecipeMaps.registerGenerationResetHook(GT6RecipesDistillery::resetForTest);}

	/** FMLCommonSetup.enqueueWork — the fluid/item DeferredRegisters have fired by this point. */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(GT6RecipesDistillery::load);
	}

	/**
	 * Pours the seven resolvable water-family rows into {@link GT6RecipeMaps#DISTILLERY}.
	 * Idempotent; the unregistered-fluid row skips with a count (the upstream FL.exists
	 * drop).
	 */
	public static synchronized void load() {
		if (sLoaded) {LOGGER.debug("load() skipped: already poured (generation flag set)"); return;}
		GT6RecipeMaps.init(); // defensive + idempotent: the map exists from ConstructMod (GTMachines.java), tests may race it
		RecipeMap tMap = GT6RecipeMaps.DISTILLERY;
		if (tMap == null) return; // reset() between init and load — a broken lifecycle, nothing to pour into

		int tPoured = 0, tSkipped = 0;
		for (DistilleryRow tRow : table()) {
			Recipe tRecipe = buildRecipe(tRow);
			if (tRecipe == null) {tSkipped++; continue;} // the absent-fluid silent skip (the upstream FL.exists drops)
			tMap.addRecipe(tRecipe);
			tPoured++;
		}
		sLoaded = true;
		LOGGER.info("GT6 Distillery poured: {} loaded, {} skipped (unregistered fluid ids, = upstream FL.exists drops)", tPoured, tSkipped);
	}

	/** Row → Recipe, or null when the fluid fails to resolve (the silent-skip semantics). */
	static Recipe buildRecipe(DistilleryRow aRow) {
		Fluid tInput = sFluidResolver.apply(aRow.input());
		if (tInput == null) return null;
		Fluid tOutput = sFluidResolver.apply(FLUID_DISTW);
		if (tOutput == null) return null;
		ItemStack tCircuit = sCircuitResolver.apply(0); // ST.tag(0) — config 0 on every :534-541 row
		if (tCircuit == null || tCircuit.isEmpty()) return null;
		// the upstream addRecipe1(T, 16, 16, ST.tag(0), FL.in.make(in), FL.DistW.make(out), ZL_IS)
		// shape: buffered, circuit-in (count 1, never consumed — the ① identity-skip) +
		// fluid-in + fluid-out, no item outputs, EUt 16 duration 16 verbatim (:534-541).
		// NOTE the port Recipe ctor order: (duration, EUt) — the P14 lesson.
		return new Recipe(true,
				new ItemStack[] {tCircuit}, new ItemStack[0],
				new FluidStack[] {new FluidStack(tInput, (int)aRow.inAmount())},
				new FluidStack[] {new FluidStack(tOutput, (int)aRow.outAmount())},
				16, 16, 0);
	}

	/**
	 * The live fluid lookup — vanilla water for the input row (no registration involved),
	 * the registered distilled water for the output, the six aqua fluids of task
	 * p16-aqua-fluids, and NULL for the water_hot IC2 alias: no port registration, and a
	 * null makes the row skip exactly like an upstream {@code if (FL.Water_Hot.exists())}
	 * guard around its pour line. Only invoked at load() time (the registers are live) or
	 * through injected offline fixtures.
	 */
	@Nullable
	static Fluid resolveFluid(String aFluidId) {
		return switch (aFluidId) {
			case FLUID_WATER     -> Fluids.WATER;
			case FLUID_DISTW     -> GTFluids.DISTILLED_WATER.source.get();
			case FLUID_SPDEW     -> GTFluids.SPDEW.source.get();
			case FLUID_MNWTR     -> GTFluids.MNWTR.source.get();
			case FLUID_GEOTHERMAL -> GTFluids.WATER_GEOTHERMAL.source.get();
			case FLUID_BOILING   -> GTFluids.WATER_BOILING.source.get();
			case FLUID_HOT_WATER -> GTFluids.HOT_WATER.source.get();
			case FLUID_COLD      -> GTFluids.COLD_WATER.source.get();
			default -> null; // water_hot — the IC2 alias, deliberately unregistered (the :539 drop)
		};
	}

	/** Test seam: clears the poured flag so a fresh generation can re-pour (public — the cross-domain e2e drives it). */
	public static void resetForTest() {sLoaded = false;}

	private GT6RecipesDistillery() {}
}
