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
 * The FM.Burn fuel table — task p13-burning-box-family spec ⑥, the port counterpart of
 * FM.Burn = "gt.recipe.fuels.burn" (FM.java:41) poured from Loader_Fuels.java:77-120 —
 * the SAME pinned row range the p12 ENGINE_FUELS table transcribed, with the BURN column
 * values (every upstream row appears in both maps side by side, FM.Burn/:78 vs
 * FM.Engine/:79 etc.).
 *
 * <p><b>Upstream source</b>: every {@code FM.Burn.addRecipe0(T, aEUt, aDuration, tFuel,
 * FL.CarbonDioxide.make(1), ZL_IS)} row of :77-120, transcribed whole (the wire W1
 * full-table ruling — no subset cut), with the BURN power values:
 * <ul>
 * <li>JetFuel −128 × 9 (:78) = 1152 power per fluid unit;</li>
 * <li>Kerosine −64 × 5 (:82), Petrol −64 × 5 (:86), Diesel −64 × 5 (:90) = 320;</li>
 * <li>Fuel −64 × 6 (:94) = 384;</li>
 * <li>nitrofuel −64 × 9 (:97, the {@code FL.make("nitrofuel", 1)} plain-name row) = 576;</li>
 * <li>the alcohol family −16 × 6 (:100-103 RUM, :104-107 WHISKEY, :108-111 VINEGAR,
 *     :113-116 Vodka — all identical values) = 96, carried by the ONE representative the
 *     port registers, {@code gt6:ethanol} (the p12 "alcohol family representative"
 *     ruling, identically applied). BioEthanol (:121-124) and the rows past :120 are OUT
 *     of the pinned range.</li>
 * </ul>
 * The row value is the HEAT per fluid unit: {@code getAbsoluteTotalPower() =
 * |EUt × duration|} (upstream Recipe.java:723-725, the port {@link Recipe#getAbsoluteTotalPower()}
 * line-for-line) — the number the Liquid/Gas Burning Box charges into its buffer per
 * burnt litre before the efficiency translation
 * ({@code units(power, 10000, mEfficiency, F)}, MultiTileEntityGeneratorLiquid.java:140).
 *
 * <p><b>FluidBed rows — the spec ⑥ archaeology verdict</b>: the row SOURCE is found —
 * upstream Loader_Fuels.java:37-43 pours FM.FluidBed with a material-driven loop over
 * 11 burning materials (Charcoal, Coal, CoalCoke, Lignite, LigniteCoke, Anthracite,
 * Prismane, Lonsdaleite, PetCoke, Peat, PeatBituminous) × 5 dust sizes (blockDust ×9
 * power, dust ×1, dustSmall ÷4, dustTiny ÷9, dustDiv72 ÷72), each row burning
 * {@code (mFurnaceBurnTime * 3 * EU_PER_FURNACE_TICK)} GU of dust against a Calcite
 * fluid input (648/72/18/8/1 L) and outputting the target-burning ash dust. The LIVE
 * pour stays EMPTY on three unportable primitives, all pool-card content:
 * (a) {@code gt6} registers no Calcite fluid (GTFluids census) and FLUIDBED's
 * minimal-fluid-input column is 1 — a row without the fluid leg cannot exist;
 * (b) the port {@link gregapi.oredict.OreDictMaterial} loads NO burn-time or
 * target-burning DATA — the {@code mFurnaceBurnTime} field (:91) and the
 * {@code mTargetBurning} stack (:154) exist but nothing in the port assigns them
 * (mFurnaceBurnTime stays 0; mTargetBurning amount stays 0), so the row
 * power/output legs have no data source;
 * (c) {@code OP.blockDust} is not one of the port's item-path prefixes
 * (GTMaterialItems.itemPathPrefixes, the Loader_Items.java:57-171 gate) — the 5-size
 * fan cannot resolve its biggest member.
 * GT6RecipeMaps.FLUIDBED therefore stays in its W1 declared-empty state and the
 * Fluidized Bed Burning Box BE consumes an empty map until a later card lands the
 * calcite/ash/burn-time data face.
 *
 * <p><b>CO2 byproduct (the p12 declared deviation, repeated)</b>: every upstream row
 * emits {@code FL.CarbonDioxide.make(1)}, carried as the {@link BurnRow#co2()} DATA mark
 * (gt6:carbon_dioxide is not a registered fluid); the Liquid/Gas Burning Box ignores
 * recipe outputs entirely (upstream the burn loop never stores them,
 * MultiTileEntityGeneratorLiquid.java:135-145 — no exhaust surface exists for the
 * burning boxes), so the mark rides the row for the future exhaust card only.
 *
 * <p><b>Load timing</b>: the {@link GT6RecipesEngineFuels} shape verbatim — a
 * self-contained MOD-bus listener pouring at FMLCommonSetup.enqueueWork; unregistered
 * fluid rows skip silently with a count; {@code load()} is idempotent per JVM
 * generation.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6RecipesBurnFuels {

	private static final Logger LOGGER = LogUtils.getLogger();

	/**
	 * One transcribed upstream row: the gt6 fluid id path, the verbatim EUt (negative =
	 * the generator convention) and duration, and the CO2 byproduct mark (:77-120 emits
	 * it on every row — carried as data, see the class doc).
	 */
	public record BurnRow(String note, String fluid, long eUt, long duration, boolean co2) {
		/** The burnable heat per fluid unit, upstream Recipe.java:723-725. */
		public long absolutePowerPerUnit() {return Math.abs(eUt * duration);}
	}

	/** The gt6 fluid id paths the table rows burn ({@code gt6:} namespace paths, GTFluids registrations). */
	public static final String FLUID_JETFUEL = "jetfuel", FLUID_KEROSINE = "kerosine", FLUID_PETROL = "petrol",
			FLUID_DIESEL = "diesel", FLUID_FUEL = "fuel", FLUID_NITROFUEL = "nitrofuel", FLUID_ETHANOL = "ethanol";

	/** The fluid seam: the live GTFluids lookups by default, fixtures injected offline. */
	static Function<String, Fluid> sFluidResolver = GT6RecipesBurnFuels::resolveFluid;

	/**
	 * The seven transcribed rows, order mirroring the upstream file order (:77-120).
	 * Lazily built — the GT6RecipesEngineFuels {@code TABLE} laziness precedent (the
	 * @EventBusSubscriber class-load at MOD CONSTRUCTION must not capture registry state).
	 */
	private static volatile List<BurnRow> sTable = null;

	/** The transcribed rows, captured on first use. */
	public static List<BurnRow> table() {
		List<BurnRow> tTable = sTable;
		if (tTable == null) sTable = tTable = List.of(
		// Loader_Fuels.java:77-80 — JetFuel, −128 × 9 = 1152
		new BurnRow(":78", FLUID_JETFUEL , -128, 9, true),
		// Loader_Fuels.java:81-84 — Kerosine, −64 × 5 = 320
		new BurnRow(":82", FLUID_KEROSINE, - 64, 5, true),
		// Loader_Fuels.java:85-88 — Petrol, −64 × 5 = 320
		new BurnRow(":86", FLUID_PETROL  , - 64, 5, true),
		// Loader_Fuels.java:89-92 — Diesel, −64 × 5 = 320
		new BurnRow(":90", FLUID_DIESEL  , - 64, 5, true),
		// Loader_Fuels.java:93-96 — Fuel, −64 × 6 = 384
		new BurnRow(":94", FLUID_FUEL    , - 64, 6, true),
		// Loader_Fuels.java:97 — nitrofuel (FL.make plain name), −64 × 9 = 576
		new BurnRow(":97", FLUID_NITROFUEL, - 64, 9, true),
		// Loader_Fuels.java:100-116 — the alcohol family (RUM/WHISKEY/VINEGAR/Vodka, all
		// −16 × 6 = 96), carried by the one representative: gt6:ethanol
		new BurnRow(":101", FLUID_ETHANOL, - 16, 6, true));
		return tTable;
	}

	/** Poured flag — one generation, one pour (upstream loaders run once per JVM). */
	private static boolean sLoaded = false;

	/** FMLCommonSetup.enqueueWork — the GTFluids DeferredRegisters have fired by this point. */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(GT6RecipesBurnFuels::load);
	}

	/** Pours the table into {@link GT6RecipeMaps#BURN}. Idempotent; unregistered-fluid rows skip with a count. */
	public static synchronized void load() {
		if (sLoaded) return;
		GT6RecipeMaps.init(); // defensive + idempotent: the map exists from ConstructMod, tests may race it
		RecipeMap tMap = GT6RecipeMaps.BURN;
		if (tMap == null) return; // reset() between init and load — a broken lifecycle, nothing to pour into

		int tPoured = 0, tSkipped = 0;
		for (BurnRow tRow : table()) {
			Recipe tRecipe = buildRecipe(tRow);
			if (tRecipe == null) {tSkipped++; continue;} // the unregistered-fluid silent skip (upstream FL.exists drops)
			tMap.addRecipe(tRecipe);
			tPoured++;
		}
		sLoaded = true;
		LOGGER.info("GT6 Burn Fuels poured: {} loaded, {} skipped (unregistered fluid ids, = upstream FL.exists drops)", tPoured, tSkipped);
	}

	/** Row → Recipe, or null when the fluid fails to resolve (the silent-skip semantics). */
	static Recipe buildRecipe(BurnRow aRow) {
		Fluid tFluid = sFluidResolver.apply(aRow.fluid());
		if (tFluid == null) return null;
		// the upstream addRecipe0(T, aEUt, aDuration, tFuel, CO2, ZL_IS) shape: fluid-only,
		// buffered, the CO2 output carried as the row's data mark (class doc), no special value
		return new Recipe(true,
				new ItemStack[0], new ItemStack[0],
				new FluidStack[] {new FluidStack(tFluid, 1)}, new FluidStack[0],
				aRow.duration(), aRow.eUt(), 0);
	}

	/** The live fluid lookup by gt6 id path — null for an unknown path (the row skips). Only invoked at load() time or through injected offline fixtures. */
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
			default -> null; // the FM.Burn rows outside the port's nine fluids (Vodka et al — not registered content)
		};
		return tFamily == null ? null : tFamily.source.get();
	}

	/** Test seam: clears the poured flag and the captured table so a fresh generation can re-pour. */
	static void resetForTest() {
		sLoaded = false;
		sTable = null;
	}
}
