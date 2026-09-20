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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import gregtech6.fluid.GTFluids;
import gregtech6.registry.GT6FoodCans;
import gregtech6.registry.GT6FoamSprays;
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

	/** The food-can empty seam ({@code gt6:food_can_empty}, upstream IL.Food_Can_Empty meta 998, MultiItemRandomTools.java:234), fixtures injected offline. */
	public static Supplier<ItemStack> sFoodCanEmptyResolver = () -> new ItemStack(GT6FoodCans.FOOD_CAN_EMPTY.get());

	/** The CANS_ROTTEN family seam: tier 0..5 → the {@code gt6:food_can_rotten_<size>} can (upstream IL.CANS_ROTTEN, IL.java:508), fixtures injected offline. */
	public static IntFunction<ItemStack> sRottenCansResolver = aTier -> new ItemStack(GT6FoodCans.FOOD_CAN_ROTTEN.get(aTier).get());

	/** The Cookie Tin seam (the tier-6 cookies can, upstream IL.CANS_COOKIES[5] meta 86, MultiItemCans.java:107), fixtures injected offline. */
	public static Supplier<ItemStack> sCookiesCanResolver = () -> new ItemStack(GT6FoodCans.FOOD_CAN_COOKIES_HUGE.get());

	/** The EUt column of every food-can row (RM.java:743-753, the addRecipe2 second argument). */
	public static final long FOOD_EUT = 16;

	/** The duration column of every food-can row (RM.java:743-753, the addRecipe2 first argument) — CONSTANT 16t, no food-value scaling. */
	public static final long FOOD_DURATION = 16;

	/** The explicit food values of the three row0 registrations (Loader_Recipes_Food.java:41/:42, MultiItemFood.java:600 — literal upstream arguments). */
	public static final int FOOD_VALUE_ROTTEN_FLESH = 4;
	public static final int FOOD_VALUE_SPIDER_EYE = 2;
	public static final int FOOD_VALUE_COOKIE = 12;

	/** The dye indices the 16 refill rows walk (0..15, the :242 loop). */
	public static final List<Integer> DYE_INDICES = List.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15);

	/**
	 * The C-Foam refill row fluid amount (task p26-c-foam-fluid-refill): the
	 * MultiItemRandomTools.java:254/:262 {@code FL.mul(DYED_C_FOAMS[i], 256)} /
	 * {@code FL.mul(DYED_C_FOAMS_OWNED[i], 256)} = 256 × the 100-unit bucket
	 * ({@link GTFluids#CFOAM_BUCKET_UNITS}, FL.java:432 "// 100 per Unit") = 25600 mB —
	 * the R4 mB 1:1 ruling, the same translation the {@link #REFILL_MB} dyes ride.
	 */
	public static final int FOAM_REFILL_MB = 256 * 100;

	/** The C-Foam fluid seam: dye index i → the {@code gt6:cfoam_<DYE_IDS[i]>} source fluid (the live default), fixtures injected offline. */
	public static IntFunction<Fluid> sCfoamFluidResolver = aIndex -> GTFluids.cfoam(aIndex, false).source.get();

	/** The owned C-Foam fluid seam: dye index i → the {@code gt6:cfoam_owned_<DYE_IDS[i]>} source fluid, fixtures injected offline. */
	public static IntFunction<Fluid> sCfoamOwnedFluidResolver = aIndex -> GTFluids.cfoam(aIndex, true).source.get();

	/** The full C-Foam spray seam: dye index i → the {@code gt6:foam_spray_<DYE_IDS[i]>} can (upstream IL.SPRAY_CAN_FOAM[i] :251, GT6FoamSprays.FOAM_SPRAYS), fixtures injected offline. */
	public static IntFunction<ItemStack> sFoamSprayResolver = aIndex -> new ItemStack(GT6FoamSprays.FOAM_SPRAYS.get(aIndex).get());

	/** The full Advanced spray seam: dye index i → the {@code gt6:foam_spray_owned_<DYE_IDS[i]>} can (upstream IL.SPRAY_CAN_FOAM_OWNED[i] :259, GT6FoamSprays.FOAM_SPRAYS_OWNED), fixtures injected offline. */
	public static IntFunction<ItemStack> sFoamSprayOwnedResolver = aIndex -> new ItemStack(GT6FoamSprays.FOAM_SPRAYS_OWNED.get(aIndex).get());

	// task p32-qu-laser-domain — the gas laser emitter fill row (MultiItemTechnological.java:403)

	/** The EUt column of the laser-gas fill row (:403, the addRecipe1 second argument). */
	public static final long LASER_GAS_EUT = 16;

	/** The duration column of the laser-gas fill row (:403, the addRecipe1 third argument). */
	public static final long LASER_GAS_DURATION = 128;

	/**
	 * The fill row fluid amount: {@code MT.CO2.gas(U, T)} = ONE unit of material gas. The
	 * port convention (the f1-chemicals gas closure, the p29 mixer rows' CO2 864 = 6×144):
	 * one unit = {@code L} = 144 mB (the R4 mB 1:1 ruling, CS.java:129).
	 */
	public static final int LASER_GAS_MB = 144;

	/** The empty emitter seam ({@code gt6:comp_laser_gas_empty}, upstream IL.Comp_Laser_Gas_Empty :384), fixtures injected offline. */
	public static Supplier<ItemStack> sLaserGasEmptyResolver = () -> new ItemStack(gregtech6.items.GT6LaserGas.COMP_LASER_GAS_EMPTY.get());

	/** The CO2 emitter seam ({@code gt6:comp_laser_gas_co2}, upstream IL.Comp_Laser_Gas_CO2 :394), fixtures injected offline. */
	public static Supplier<ItemStack> sLaserGasCo2Resolver = () -> new ItemStack(gregtech6.items.GT6LaserGas.COMP_LASER_GAS_CO2.get());

	/** The CO2 gas seam ({@code gt6:carbondioxide}, the upstream MT.CO2.gas(U, T) of :403 — the f1-chemicals gas closure row), fixtures injected offline. */
	public static Supplier<Fluid> sCarbonDioxideResolver = () -> {
		for (GTFluids.ChemicalFluid tChemical : GTFluids.CHEMICALS) {
			if (tChemical.spec.name().equals("carbondioxide")) return tChemical.source.get();
		}
		return null;
	};

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
	 * Pours the 53 rows into {@link GT6RecipeMaps#CANNER}: the 17 refill rows (the 16
	 * colour refills + the chlorine remover, p24), the 3 food-can rows of task
	 * p25-food-can-row0 (rotten_flesh/spider_eye/cookie), the 32 C-Foam refills of task
	 * p26-c-foam-fluid-refill (the :254 dyed + the :262 owned ladders) and the CO2 laser
	 * gas fill row of task p32-qu-laser-domain (MultiItemTechnological.java:403).
	 * Idempotent; an unresolvable row skips with a count (the upstream FL.exists drops).
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

		// the p25-food-can-row0 trio — Canner rows carry ZERO tools (the research card's
		// correction: the blocked face was only the empty-can CRAFTING row, not these):
		// upstream RM.food_can(ST.make(Items.rotten_flesh, 1, W), 4, "Canned Meat", IL.CANS_ROTTEN) — Loader_Recipes_Food.java:41
		// upstream RM.food_can(ST.make(Items.spider_eye , 1, W), 2, "Canned Meat", IL.CANS_ROTTEN) — :42
		// upstream RM.food_can(ST.make(Items.cookie, 6, W), 12, "Cookie Tin", IL.CANS_COOKIES) — MultiItemFood.java:600
		// (the WiMo row :40 and the GT-material dust rows :44-51 stay POOLED — non-vanilla items)
		ItemStack tFoodCanEmpty = sFoodCanEmptyResolver.get();
		if (tFoodCanEmpty == null || tFoodCanEmpty.isEmpty()) {tSkipped += 3;}
		else {
			Recipe tRottenFlesh = foodCanRow(new ItemStack(Items.ROTTEN_FLESH, 1), FOOD_VALUE_ROTTEN_FLESH, sRottenCansResolver, tFoodCanEmpty);
			if (tRottenFlesh == null) tSkipped++;
			else {tMap.addRecipe(tRottenFlesh); tPoured++;}
			Recipe tSpiderEye = foodCanRow(new ItemStack(Items.SPIDER_EYE, 1), FOOD_VALUE_SPIDER_EYE, sRottenCansResolver, tFoodCanEmpty);
			if (tSpiderEye == null) tSkipped++;
			else {tMap.addRecipe(tSpiderEye); tPoured++;}
			Recipe tCookie = foodCanRow(new ItemStack(Items.COOKIE, 6), FOOD_VALUE_COOKIE, aTier -> sCookiesCanResolver.get(), tFoodCanEmpty);
			if (tCookie == null) tSkipped++;
			else {tMap.addRecipe(tCookie); tPoured++;}
		}

		// the p26-c-foam-fluid-refill 32 — MultiItemRandomTools.java:254 (dyed) / :262 (owned),
		// one row per colour per ladder: empty can + 256 buckets of C-Foam → the full spray
		for (int i : DYE_INDICES) {
			Recipe tFoam = foamRefillRecipe(i, false);
			if (tFoam == null) {tSkipped++; continue;} // the absent-fluid/item silent skip
			tMap.addRecipe(tFoam);
			tPoured++;
			Recipe tOwned = foamRefillRecipe(i, true);
			if (tOwned == null) {tSkipped++; continue;}
			tMap.addRecipe(tOwned);
			tPoured++;
		}

		// the p32-qu-laser-domain fill row — MultiItemTechnological.java:403: empty emitter +
		// 1 unit of CO2 gas (144 mB) → the Carbon Dioxide Laser Emitter
		Recipe tLaserGas = laserGasRecipe();
		if (tLaserGas == null) tSkipped++;
		else {tMap.addRecipe(tLaserGas); tPoured++;}
		sLoaded = true;
		LOGGER.info("GT6 Canner poured: {} loaded, {} skipped (unregistered dye/chlorine/can ids, = upstream FL.exists drops)", tPoured, tSkipped);
	}

	/**
	 * The MultiItemTechnological.java:403 row — buffered T, EUt 16, duration 128, the empty
	 * gas laser emitter in, {@code MT.CO2.gas(U, T)} = 144 mB of {@code gt6:carbondioxide}
	 * in, the CO2 emitter out. Null when any leg fails to resolve (the silent skip).
	 */
	@Nullable
	static Recipe laserGasRecipe() {
		Fluid tCo2 = sCarbonDioxideResolver.get();
		if (tCo2 == null) return null;
		ItemStack tEmpty = sLaserGasEmptyResolver.get();
		if (tEmpty == null || tEmpty.isEmpty()) return null;
		ItemStack tCo2Emitter = sLaserGasCo2Resolver.get();
		if (tCo2Emitter == null || tCo2Emitter.isEmpty()) return null;
		// upstream :403 — RM.Canner.addRecipe1(T, 16, 128, IL.Comp_Laser_Gas_Empty.get(1), MT.CO2.gas(U, T), NF, IL.Comp_Laser_Gas_CO2.get(1))
		return new Recipe(true,
				new ItemStack[] {tEmpty}, new ItemStack[] {tCo2Emitter},
				new FluidStack[] {new FluidStack(tCo2, LASER_GAS_MB)},
				null,
				LASER_GAS_DURATION, LASER_GAS_EUT, 0);
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

	/**
	 * The MultiItemRandomTools.java:254 (owned=F) / :262 (owned=T) row for dye index i —
	 * buffered T, EUt 16, duration 256, empty can in, {@code cfoam[_owned]_<i>} × 25600 mB
	 * in (256 × the 100-unit bucket, {@link #FOAM_REFILL_MB}), full can out (ZERO NBT — the
	 * R5 colour-as-identity ruling, a fresh GT6FoamSprayItem is implicitly full). Null when
	 * any leg fails to resolve (the silent skip).
	 */
	@Nullable
	static Recipe foamRefillRecipe(int aIndex, boolean aOwned) {
		Fluid tCfoam = aOwned ? sCfoamOwnedFluidResolver.apply(aIndex) : sCfoamFluidResolver.apply(aIndex);
		if (tCfoam == null) return null;
		ItemStack tEmpty = sEmptyCanResolver.get();
		if (tEmpty == null || tEmpty.isEmpty()) return null;
		ItemStack tFull = aOwned ? sFoamSprayOwnedResolver.apply(aIndex) : sFoamSprayResolver.apply(aIndex);
		if (tFull == null || tFull.isEmpty()) return null;
		// upstream :254 — RM.Canner.addRecipe1(T, 16, 256, IL.Spray_Empty.get(1), FL.mul(DYED_C_FOAMS[i], 256), NF, IL.SPRAY_CAN_FOAM[i].get(1))
		// upstream :262 — RM.Canner.addRecipe1(T, 16, 256, IL.Spray_Empty.get(1), FL.mul(DYED_C_FOAMS_OWNED[i], 256), NF, IL.SPRAY_CAN_FOAM_OWNED[i].get(1))
		return new Recipe(true,
				new ItemStack[] {tEmpty}, new ItemStack[] {tFull},
				new FluidStack[] {new FluidStack(tCfoam, FOAM_REFILL_MB)},
				null,
				REFILL_DURATION, REFILL_EUT, 0);
	}

	/**
	 * The RM.food_can row for ONE food — the port of the upstream
	 * {@code Canner.addRecipe2(T, 16, 16, aStack, IL.Food_Can_Empty.get(N),
	 * aCans[tier].getWithName(N, aCannedName), ST.container(aStack, T))} rows (RM.java:743-753):
	 * buffered T, EUt {@link #FOOD_EUT}, duration {@link #FOOD_DURATION} (CONSTANT — no
	 * food-value scaling), inputs [the food stack AS REGISTERED (count carries, the cookie
	 * row eats 6), the empty can x N], outputs [the tier can x N]. The fourth upstream
	 * output {@code ST.container(aStack, T)} is EMPTY for all three row0 foods (no vanilla
	 * container item), so the output leg is the can alone. The canned display NAME
	 * ("Canned Meat"/"Cookie Tin", the getWithName face) has no port Recipe surface — the
	 * row identity IS the item, the name stays pooled with the NEI-info card.
	 *
	 * @param aFood the food input stack (count = the registered amount)
	 * @param aFoodValue the EXPLICIT upstream food-value argument (NOT derived from the stack)
	 * @param aCans the family resolver: tier 0..5 → the can stack (the aCans[tier] dispatch)
	 * @param aEmptyCan the empty-can stack (its count is overridden by the dispatch)
	 * @return the row, or null when any leg is missing (the silent skip)
	 */
	@Nullable
	static Recipe foodCanRow(ItemStack aFood, int aFoodValue, IntFunction<ItemStack> aCans, ItemStack aEmptyCan) {
		if (aFood == null || aFood.isEmpty() || aFoodValue <= 0) return null;
		if (aEmptyCan == null || aEmptyCan.isEmpty()) return null;
		int[] tDispatch = foodCanTier(aFoodValue);
		ItemStack tCan = aCans.apply(tDispatch[1]);
		if (tCan == null || tCan.isEmpty()) return null;
		ItemStack tEmpty = aEmptyCan.copy();
		tEmpty.setCount(tDispatch[0]);
		ItemStack tOutput = tCan.copy();
		tOutput.setCount(tDispatch[0]);
		return new Recipe(true,
				new ItemStack[] {aFood.copy(), tEmpty}, new ItemStack[] {tOutput},
				null, null,
				FOOD_DURATION, FOOD_EUT, 0);
	}

	/**
	 * The RM.food_can tier dispatch — switch(aFoodValue / 2) VERBATIM (RM.java:742-753):
	 * returns {canCount, familyTier}. Cases 0-5 pick tiers 0-4 at count 1 (the tiny..
	 * large ladder), the doubled/tripled/quadrupled/quintupled bands reuse tiers 3/4 at
	 * counts 2/3/4/5, and the DEFAULT branch (cookie = foodValue 12 falls here: 12/2 = 6
	 * hits no case) is {@code count = aFoodValue / 12, tier = 5} — the huge-can tier.
	 */
	static int[] foodCanTier(int aFoodValue) {
		switch (aFoodValue / 2) {
		case 0: case 1: return new int[] {1, 0};
		case 2:         return new int[] {1, 1};
		case 3:         return new int[] {1, 2};
		case 4:         return new int[] {1, 3};
		case 5:         return new int[] {1, 4};
		case 8: case 9: return new int[] {2, 3};
		case 10: case 11: return new int[] {2, 4};
		case 15: case 16: case 17: return new int[] {3, 4};
		case 20: case 21: case 22: case 23: return new int[] {4, 4};
		case 25: case 26: case 27: case 28: case 29: return new int[] {5, 4};
		default:        return new int[] {aFoodValue / 12, 5};
		}
	}

	/** Test seam: clears the poured flag so a fresh generation can re-pour (public — the cross-domain e2e drives it). */
	public static void resetForTest() {sLoaded = false;}

	private GT6RecipesCanner() {}
}
