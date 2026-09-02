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
import net.minecraftforge.fluids.FluidStack;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import gregtech6.fluid.GTFluids;

/**
 * The FM.Engine fuel table — task p12-engine-fuel-fluids spec ②, the port counterpart of
 * RM.sFuelsEngine = FM.Engine (RM.java:172) poured from Loader_Fuels.java:77-120.
 *
 * <p><b>Upstream source</b>: every {@code FM.Engine.addRecipe0(T, aEUt, aDuration, tFuel,
 * FL.CarbonDioxide.make(1), ZL_IS)} row of :77-120, transcribed whole (the wire W1
 * full-table ruling — no subset cut):
 * <ul>
 * <li>JetFuel −128 × 12 (:79) = 1536 power per fluid unit;</li>
 * <li>Kerosine −64 × 7 (:83), Petrol −64 × 7 (:87), Diesel −64 × 7 (:91) = 448;</li>
 * <li>Fuel −64 × 8 (:95) = 512;</li>
 * <li>nitrofuel −64 × 12 (:98, the {@code FL.make("nitrofuel", 1)} plain-name row) = 768;</li>
 * <li>the alcohol family −16 × 9 (:100-103 RUM, :104-107 WHISKEY, :108-111 VINEGAR,
 *     :113-116 Vodka — all identical values) = 144, carried by the ONE representative the
 *     port registers, {@code gt6:ethanol} (the card's "alcohol family representative"
 *     ruling; the extra per-drink fluids are not port content). BioEthanol (:121-124,
 *     −16 × 12 = 192) and the remaining rows past :120 are OUT of the pinned range — the
 *     Reikanol/BioDiesel/BioFuel family stays pooled until a card registers those fluids.</li>
 * </ul>
 * The row value is the ENGINE POWER per fluid unit: {@code getAbsoluteTotalPower() =
 * |EUt × duration|} (upstream Recipe.java:723-725, the port {@link Recipe#getAbsoluteTotalPower()}
 * line-for-line), the number the MotorLiquid diesel engine burns at (16..512 RU/t over the
 * eight tiers). Negative EUt = the generator convention, carried verbatim.
 *
 * <p><b>CO2 byproduct (declared deviation)</b>: every upstream row emits
 * {@code FL.CarbonDioxide.make(1)} (:79/:83/:87/:91/:95/:98/:102, ...), but gt6:carbon_dioxide
 * is NOT one of this card's nine fluids, and a missing output fluid would zero the whole
 * table at pour time. The byproduct therefore rides the row as the {@link FuelRow#co2()}
 * DATA mark (asserted for all seven rows) instead of a FluidStack in the Recipe — the
 * p12-engine-diesel card owns the live exhaust surface and decides the emission form.
 *
 * <p><b>Row shape</b>: fluid-only — empty item arrays, one 1-unit FluidStack input (the
 * upstream {@code tFuel = FL.X.list(1)} amount), no fluid outputs, duration/EUt verbatim.
 * The double-empty guard of {@link RecipeMap#addRecipe} does not fire (the fluid leg is a
 * real input).
 *
 * <p><b>Lookup seam handed to p12-engine-diesel</b>: upstream answers fluid-only lookups
 * through the fluid hash indexes when {@code mMinimalInputItems == 0} (Recipe.java:518-523),
 * but the port's {@code RecipeMap.findRecipe} hard-returns on empty item input arrays
 * (RecipeMap.java:137-138, a Furnace-level port simplification predating this card). The
 * consumer card extends the lookup when it wires the diesel engine — the rows here are
 * queryable today through the exact predicate findRecipe scans with
 * ({@code isRecipeInputEqual}, asserted offline).
 *
 * <p><b>Load timing</b> (the GT6RecipesCokeOven precedent, ADR ruling ②): a self-contained
 * MOD-bus listener pouring at FMLCommonSetup.enqueueWork — by then the DeferredRegisters of
 * {@link GTFluids} have fired, so the row fluids resolve; ConstructMod-time would not
 * (GTMachines registering the maps runs before any RegisterEvent). GT6Mod stays frozen
 * (ADR-P3-4). Unregistered-fluid rows skip SILENTLY with a count (the upstream
 * {@code FL.exists}/absent-fluid semantics — a fluid family that lost its registration
 * must not block the others). {@code load()} is idempotent per JVM generation.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6RecipesEngineFuels {

	private static final Logger LOGGER = LogUtils.getLogger();

	/**
	 * One transcribed upstream row: the gt6 fluid id path, the verbatim EUt (negative = the
	 * generator convention) and duration, and the CO2 byproduct mark (:77-120 emits it on
	 * every row — carried as data, see the class doc).
	 */
	public record FuelRow(String note, String fluid, long eUt, long duration, boolean co2) {
		/** The engine power per fluid unit, upstream Recipe.java:723-725. */
		public long absolutePowerPerUnit() {return Math.abs(eUt * duration);}
	}

	/** The gt6 fluid id paths the table rows burn ({@code gt6:} namespace paths, GTFluids registrations). */
	public static final String FLUID_JETFUEL = "jetfuel", FLUID_KEROSINE = "kerosine", FLUID_PETROL = "petrol",
			FLUID_DIESEL = "diesel", FLUID_FUEL = "fuel", FLUID_NITROFUEL = "nitrofuel", FLUID_ETHANOL = "ethanol";

	/** The fluid seam: the live GTFluids lookups by default, fixtures injected offline. */
	static Function<String, Fluid> sFluidResolver = GT6RecipesEngineFuels::resolveFluid;

	/**
	 * The seven transcribed rows, order mirroring the upstream file order (:77-120).
	 * Lazily built — {@code TABLE} is assembled on first use exactly like the
	 * GT6RecipesCokeOven {@code table()} precedent (the @EventBusSubscriber class-load at
	 * MOD CONSTRUCTION must not capture material/registry state; here it is pure strings,
	 * the laziness keeps the two loaders structurally identical).
	 */
	private static volatile List<FuelRow> sTable = null;

	/** The transcribed rows, captured on first use. */
	public static List<FuelRow> table() {
		List<FuelRow> tTable = sTable;
		if (tTable == null) sTable = tTable = List.of(
		// Loader_Fuels.java:77-80 — JetFuel, −128 × 12 = 1536
		new FuelRow(":79", FLUID_JETFUEL , -128, 12, true),
		// Loader_Fuels.java:81-84 — Kerosine, −64 × 7 = 448
		new FuelRow(":83", FLUID_KEROSINE, - 64,  7, true),
		// Loader_Fuels.java:85-88 — Petrol, −64 × 7 = 448
		new FuelRow(":87", FLUID_PETROL  , - 64,  7, true),
		// Loader_Fuels.java:89-92 — Diesel, −64 × 7 = 448
		new FuelRow(":91", FLUID_DIESEL  , - 64,  7, true),
		// Loader_Fuels.java:93-96 — Fuel, −64 × 8 = 512
		new FuelRow(":95", FLUID_FUEL    , - 64,  8, true),
		// Loader_Fuels.java:97-98 — nitrofuel (FL.make plain name), −64 × 12 = 768
		new FuelRow(":98", FLUID_NITROFUEL, - 64, 12, true),
		// Loader_Fuels.java:100-116 — the alcohol family (RUM/WHISKEY/VINEGAR/Vodka, all
		// −16 × 9 = 144), carried by the one representative: gt6:ethanol
		new FuelRow(":102", FLUID_ETHANOL, - 16,  9, true));
		return tTable;
	}

	/** Poured flag — one generation, one pour (upstream loaders run once per JVM). */
	private static boolean sLoaded = false;

	/** FMLCommonSetup.enqueueWork — the GTFluids DeferredRegisters have fired by this point. */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(GT6RecipesEngineFuels::load);
	}

	/** Pours the table into {@link GT6RecipeMaps#ENGINE_FUELS}. Idempotent; unregistered-fluid rows skip with a count. */
	public static synchronized void load() {
		if (sLoaded) return;
		GT6RecipeMaps.init(); // defensive + idempotent: the map exists from ConstructMod (GTMachines.java:91), tests may race it
		RecipeMap tMap = GT6RecipeMaps.ENGINE_FUELS;
		if (tMap == null) return; // reset() between init and load — a broken lifecycle, nothing to pour into

		int tPoured = 0, tSkipped = 0;
		for (FuelRow tRow : table()) {
			Recipe tRecipe = buildRecipe(tRow);
			if (tRecipe == null) {tSkipped++; continue;} // the unregistered-fluid silent skip (upstream absent-fluid semantics)
			tMap.addRecipe(tRecipe);
			tPoured++;
		}
		sLoaded = true;
		LOGGER.info("GT6 Engine Fuels poured: {} loaded, {} skipped (unregistered fluid ids, = upstream FL.exists drops)", tPoured, tSkipped);
	}

	/** Row → Recipe, or null when the fluid fails to resolve (the silent-skip semantics). */
	static Recipe buildRecipe(FuelRow aRow) {
		Fluid tFluid = sFluidResolver.apply(aRow.fluid());
		if (tFluid == null) return null;
		// the upstream addRecipe0(T, aEUt, aDuration, tFuel, CO2, ZL_IS) shape: fluid-only,
		// buffered, the CO2 output carried as the row's data mark (class doc), no special value
		return new Recipe(true,
				new ItemStack[0], new ItemStack[0],
				new FluidStack[] {new FluidStack(tFluid, 1)}, new FluidStack[0],
				aRow.duration(), aRow.eUt(), 0);
	}

	/** The live fluid lookup by gt6 id path — null for an unknown path (the row skips). Only invoked at load() time (the registers are live) or through injected offline fixtures. */
	@Nullable
	private static Fluid resolveFluid(String aFluidId) {
		GTFluids.EngineFluid tFamily = switch (aFluidId) {
			case FLUID_JETFUEL   -> GTFluids.JETFUEL;
			case FLUID_KEROSINE  -> GTFluids.KEROSINE;
			case FLUID_PETROL    -> GTFluids.PETROL;
			case FLUID_DIESEL    -> GTFluids.DIESEL;
			case FLUID_FUEL      -> GTFluids.FUEL;
			case FLUID_NITROFUEL -> GTFluids.NITROFUEL;
			case FLUID_ETHANOL   -> GTFluids.ETHANOL;
			default -> null; // steam/distilled_water are engine coolant faces, not FM.Engine rows
		};
		return tFamily == null ? null : tFamily.source.get();
	}

	/** Test seam: clears the poured flag and the captured table so a fresh generation can re-pour. */
	static void resetForTest() {
		sLoaded = false;
		sTable = null;
	}
}
