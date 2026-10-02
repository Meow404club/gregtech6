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
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.fluid.GTFluids;
import gregtech6.registry.GT6FoodCans;
import gregtech6.registry.GT6FoamSprays;
import gregtech6.registry.GT6SprayCans;

/**
 * The RM.Canner pour home — task canner-machine, the port counterpart of
 * the {@code RM.Canner.addRecipe1} rows of MultiItemRandomTools.java:246 (the 16 colour
 * refills, the :242 loop body) and :272 (the paint-remover refill). Everything else the
 * upstream Canner map eats rides the {@link GT6RecipeMapCanner} dynamic arms (R1).
 * Task food-meat-recipes extends the load() with the food-side explicit rows: the
 * Loader_Recipes_Food.java:44-51 canned-material band (the cooked meat/fish + Tofu +
 * Soylent Green dust ladders over the shared {@link #foodCanRow} row0 shape) and the
 * MultiItemCans.java:113-120 air band (the Canned Air fill/release triplets).
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
 * default (the task dye-chemical-fluids registrations) and the ITEM resolvers close
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
	 * (the live default = the dye-chemical-fluids registration), fixtures injected
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
	 * The C-Foam refill row fluid amount (task c-foam-fluid-refill): the
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

	// task food-meat-recipes — the Loader_Recipes_Food.java:40-51 canned-material band +
	// the MultiItemCans.java:113-120 air band

	/**
	 * The family-can seam shared live face — offline-safe: the shared-test pour (the
	 * PhaseGate census / HashIndex equivalence / MaterialTree drive load() LIVE on both
	 * legs) runs against unbound gt6 RegistryObjects on the forge test JVM, where the
	 * {@code .get()} THROWS ("Registry Object not present") — a null here keeps the
	 * upstream {@code aCans[tier]} mat()-null silent drop instead of blowing up the whole
	 * shared pour (the {@link #liveAirFluid} wrap precedent, the c3190e3d2 lesson).
	 */
	static ItemStack liveFamilyCan(java.util.function.Supplier<ItemStack> aLeg) {
		try {return aLeg.get();} catch (RuntimeException tOffline) {return null;}
	}

	/**
	 * The meat-can family seam: tier 0..5 → the {@code gt6:food_can_meat_<size>} can
	 * (upstream IL.CANS_MEAT, the aCans[tier] dispatch), fixtures injected offline.
	 */
	public static IntFunction<ItemStack> sMeatCansResolver = aTier -> liveFamilyCan(() -> new ItemStack(GT6FoodCans.FOOD_CAN_MEAT.get(aTier).get()));

	/** The fish-can family seam (upstream IL.CANS_FISH), fixtures injected offline. */
	public static IntFunction<ItemStack> sFishCansResolver = aTier -> liveFamilyCan(() -> new ItemStack(GT6FoodCans.FOOD_CAN_FISH.get(aTier).get()));

	/** The veggie-can family seam (upstream IL.CANS_VEGGIE — the Tofu AND the Soylent Green target), fixtures injected offline. */
	public static IntFunction<ItemStack> sVeggieCansResolver = aTier -> liveFamilyCan(() -> new ItemStack(GT6FoodCans.FOOD_CAN_VEGGIE.get(aTier).get()));

	/**
	 * The canned-material seam: (prefix, material) → the GT6 material item (the
	 * {@code OP.<prefix>.mat(MT.<food>, n)} legs of :44-51; the live default is the
	 * GTMaterialItems walk — the GT6RecipesBees resolver share), fixtures injected offline
	 * (GTMaterialItems.get yields null there, the b2b1 lesson — never a throw).
	 */
	public static BiFunction<OreDictPrefix, OreDictMaterial, Item> sFoodMaterialItemResolver = GT6RecipesMixer::resolveItem;

	/**
	 * The air-fluid seam: the upstream FL id → the port source fluid (the
	 * {@code FL.make(tAir, 16000)} legs of :114-116 and the FL.Air.*.make(16000) legs of
	 * :118-120; the live default is the {@link #liveAirFluid} walk — plain {@code air} is
	 * port-ABSENT so its two rows stay pour-face-forever dormant, netherair/enderair
	 * resolve), fixtures injected offline.
	 */
	public static Function<String, Fluid> sAirFluidResolver = GT6RecipesCanner::liveAirFluid;

	/** The air-can seam: the FL id → the filled air can (upstream IL.Food_Can_Air/_Nether/_End), fixtures injected offline. */
	public static Function<String, ItemStack> sAirCanResolver = GT6RecipesCanner::liveAirCan;

	/**
	 * The live air-fluid leg — the GTFluids walk wrapped offline-safe: the shared-test
	 * pour (the PhaseGate census / HashIndex equivalence drives load() LIVE on both legs)
	 * runs against unbound fluid RegistryObjects on the forge test JVM, where the
	 * {@code ChemicalFluid.source.get()} inside {@code GTFluids.liveFluidSource} THROWS
	 * ("Registry Object not present") — a null here keeps the upstream FL.exists silent
	 * drop instead of blowing up the whole shared pour.
	 */
	static Fluid liveAirFluid(String aAirId) {
		try {return GTFluids.liveFluidSource(aAirId);} catch (RuntimeException tOffline) {return null;}
	}

	/**
	 * The live air-can leg — the string-id registry walk over the three air cans (the
	 * GT6RecipesMeat.resolveFoodItem face: offline the unbound registry yields null = the
	 * silent drop; the type-free form also stays clear of the stonecutter
	 * RegistryObject→DeferredHolder swap, whose 21.1 shape carries isBound() instead of
	 * isPresent()).
	 */
	static ItemStack liveAirCan(String aAirId) {
		String tPath = switch (aAirId) {
			case "air" -> "food_can_air";
			case "netherair" -> "food_can_air_nether";
			case "enderair" -> "food_can_air_end";
			default -> null;
		};
		if (tPath == null) return ItemStack.EMPTY;
		Item tCan = ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6", tPath));
		return tCan == null ? ItemStack.EMPTY : new ItemStack(tCan);
	}

	/**
	 * The FluidsGT.AIR trio (CS.java:1515, populated by the FL AIR-category ctor face —
	 * FL.java:64-66: {@code air}, {@code enderair}, {@code netherair}).
	 */
	public static final List<String> AIR_FLUIDS = List.of("air", "enderair", "netherair");

	/**
	 * The :113 fill-walk face: upstream {@code for (String tAir : FluidsGT.AIR) if
	 * (!FL.Air_End.is(tAir) && !FL.Air_Nether.is(tAir))} — the walk minus the two
	 * dimension specials = the plain {@code air} alone; :115/:116 add them back as the
	 * explicit fixed rows.
	 */
	public static final List<String> AIR_FILL_WALK = List.of("air");

	/** The :115-:116 explicit fill rows (the two dimension specials the :113 walk excludes). */
	public static final List<String> AIR_FILL_SPECIALS = List.of("netherair", "enderair");

	/** The :118-:120 release rows (every AIR walk member releases back). */
	public static final List<String> AIR_RELEASE = List.of("air", "netherair", "enderair");

	/** The air fill/release fluid column (:114-120 — the 16000 mB literal). */
	public static final int AIR_MB = 16000;

	/** The air-band EUt column (:114-120 — 16). */
	public static final long AIR_EUT = 16;

	/** The air fill duration (:114-116 — 64; the release rows :118-120 run 16). */
	public static final long AIR_FILL_DURATION = 64;

	/** The air release duration (:118-120 — 16). */
	public static final long AIR_RELEASE_DURATION = 16;

	/** One :44-51 canned-material spec — the loop body of {@code RM.food_can(tStack, 2, name, IL.CANS_*)}. */
	public record CannedMaterial(String note, OreDictMaterial material, IntFunction<ItemStack> family) {}

	/** The seven ST.array prefixes of :44/:46/:48/:50, upstream order. */
	public static List<OreDictPrefix> cannedMaterialPrefixes() {
		return List.of(OP.dustTiny, OP.dustSmall, OP.dust, OP.nugget, OP.chunkGt, OP.billet, OP.ingot);
	}

	/**
	 * The input-count column of :44-51, aligned index-for-index with {@link
	 * #cannedMaterialPrefixes()}: the second {@code mat(prefix, N)} argument per ST.array
	 * element — the stackSize (OreDictManager.java:561-574 {@code ST.amount(aAmount,
	 * rStack)}), poured verbatim as the recipe input by RM.food_can (RM.java:740-753). All
	 * four bands carry the same N per prefix: 9/4/1/9/4/2/1 — every recipe line consumes
	 * ONE FULL material unit, not a single item.
	 */
	public static final List<Integer> CANNED_MATERIAL_COUNTS = List.of(9, 4, 1, 9, 4, 2, 1);

	/**
	 * The four :44-51 material specs, upstream order (FishCooked → Canned Fish, MeatCooked
	 * → Canned Meat, Tofu/SoylentGreen → the veggie family). Every row carries foodValue 2
	 * (the second RM.food_can argument). Lazily built — the {@code @EventBusSubscriber} scan
	 * class-loads at MOD CONSTRUCTION, before MT.init() (the a9027ac lesson).
	 */
	public static List<CannedMaterial> cannedMaterialTable() {
		List<CannedMaterial> tTable = sCannedMaterials;
		if (tTable == null) sCannedMaterials = tTable = List.of(
				new CannedMaterial(":44-45", MT.FishCooked, sFishCansResolver),
				new CannedMaterial(":46-47", MT.MeatCooked, sMeatCansResolver),
				new CannedMaterial(":48-49", MT.Tofu, sVeggieCansResolver),
				new CannedMaterial(":50-51", MT.SoylentGreen, sVeggieCansResolver));
		return tTable;
	}

	/** The captured material table (lazy — the a9027ac lesson). */
	private static volatile List<CannedMaterial> sCannedMaterials = null;

	/** The foodValue column of every :44-51 row (the shared RM.food_can second argument). */
	public static final int CANNED_MATERIAL_FOOD_VALUE = 2;

	// task food-meat-recipes (the coordinator's scope extension) — the T3b-delegated
	// baking-domain band, MultiItemFood.java:600-:782 (the T3b ledger POOLED these
	// food_can rows to the Canner domain; the boundary walk re-homed them here)

	/**
	 * One baking-domain member — the EXPLICIT foodValue form (RM.food_can's second
	 * argument, NOT the item nutrition), the upstream input count carrying verbatim.
	 */
	public record BakingCanMember(String note, String itemId, int inputCount, int foodValue, String family) {}

	/** The bake-food item seam (the ForgeRegistries walk by default, fixtures injected offline — the meat walk's face). */
	public static Function<String, Item> sBakingFoodItemResolver = GT6RecipesMeat::resolveFoodItem;

	/** The bread-can family seam: tier 0..5 → the {@code gt6:food_can_bread_<size>} can (upstream IL.CANS_BREAD), fixtures injected offline. */
	public static IntFunction<ItemStack> sBreadCansResolver = aTier -> liveFamilyCan(() -> new ItemStack(GT6FoodCans.FOOD_CAN_BREAD.get(aTier).get()));

	/**
	 * The :608-:782 baking-domain members, upstream order. Dispositions around them: the
	 * :600 Cookie Tin row is the row0 card's third row (already poured in this load());
	 * the :624 Abyssal arm stays TRUE NEGATIVE — its {@code IL.NeLi_Cookie.exists()} gate
	 * never opens on the port (netherlicious is foreign, the P10 ruling). The display
	 * names ("Raisin Cookie Tin"/"Canned Bread"/"Canned Pain") stay pooled with the
	 * NEI-info face (the foodCanRow doc).
	 */
	public static final List<BakingCanMember> BAKING_CANNED_MEMBERS = List.of(
			new BakingCanMember(":608 raisin cookie tin"          , "gt6:food_cookie_raisins"          , 6, 12, "cookies"),
			new BakingCanMember(":616 chocolate raisin cookie tin", "gt6:food_cookie_chocolate_raisins", 6, 12, "cookies"),
			new BakingCanMember(":680 canned bread bun"           , "gt6:food_bun"                     , 1,  2, "bread"),
			new BakingCanMember(":681 canned bread bun sliced"    , "gt6:food_bun_sliced"              , 2,  2, "bread"),
			new BakingCanMember(":682 canned bread buns sliced"   , "gt6:food_buns_sliced"             , 1,  2, "bread"),
			new BakingCanMember(":719 canned bread"               , "minecraft:bread"                  , 1,  4, "bread"),
			new BakingCanMember(":720 canned bread sliced"        , "gt6:food_bread_sliced"            , 1,  2, "bread"),
			new BakingCanMember(":721 canned breads sliced"       , "gt6:food_breads_sliced"           , 1,  4, "bread"),
			new BakingCanMember(":749 canned pain baguette"       , "gt6:food_baguette"                , 1,  8, "bread"),
			new BakingCanMember(":750 canned pain baguette sliced", "gt6:food_baguette_sliced"         , 1,  4, "bread"),
			new BakingCanMember(":751 canned pain baguettes slice", "gt6:food_baguettes_sliced"        , 1,  8, "bread"),
			new BakingCanMember(":780 canned bread toast raw"     , "gt6:food_toast_raw"               , 1,  8, "bread"),
			new BakingCanMember(":781 canned bread toast"         , "gt6:food_toast"                   , 1,  8, "bread"),
			new BakingCanMember(":782 canned bread toast sliced"  , "gt6:food_toast_sliced"            , 1,  1, "bread"));


	// task qu-laser-domain + debt-laser-gas-family — the gas laser emitter fill family
	// (MultiItemTechnological.java:396-403, the eight upstream Canner rows)

	/** The EUt column of every laser-gas fill row (:396-403, the addRecipe1 second argument). */
	public static final long LASER_GAS_EUT = 16;

	/** The duration column of every laser-gas fill row (:396-403, the addRecipe1 third argument). */
	public static final long LASER_GAS_DURATION = 128;

	/**
	 * The fill row fluid amount: {@code MT.<gas>.gas(U, T)} = ONE unit of material gas. The
	 * port convention (the f1-chemicals gas closure, the p29 mixer rows' CO2 864 = 6×144):
	 * one unit = {@code L} = 144 mB (the R4 mB 1:1 ruling, CS.java:129).
	 */
	public static final int LASER_GAS_MB = 144;

	/** The family walk: the eight gas fluid names, in the :396-403 upstream row order (helium → carbondioxide). */
	public static final List<String> LASER_GAS_FLUIDS = List.of(
			"helium", "neon", "argon", "krypton", "xenon", "heliumneon", "carbonmonoxide", "carbondioxide");

	/** The empty emitter seam ({@code gt6:comp_laser_gas_empty}, upstream IL.Comp_Laser_Gas_Empty :384 — the shared input leg), fixtures injected offline. */
	public static Supplier<ItemStack> sLaserGasEmptyResolver = () -> new ItemStack(gregtech6.items.GT6LaserGas.COMP_LASER_GAS_EMPTY.get());

	/**
	 * The gas seam: fluid name → the {@code gt6:} CHEMICALS source fluid (the upstream
	 * {@code MT.<gas>.gas(U, T)} legs of :396-403; every family fluid is a port row since
	 * task debt-hene-fluid landed the {@code heliumneon} blend), fixtures injected offline.
	 */
	public static Function<String, Fluid> sLaserGasFluidResolver = GT6RecipesCanner::liveLaserGas;

	/**
	 * The emitter seam: fluid name → the filled emitter stack (the :396-403 item output
	 * column), fixtures injected offline. The helium leg resolves through the item
	 * REGISTRY — {@code gt6:comp_laser_gas_he} landed with usb-peripherals, so the row
	 * pours in vivo; the OFFLINE fixtures arm the EMPTY skip (the lookup yields nothing on
	 * an unbooted registry, the upstream FL.exists drop posture).
	 */
	public static Function<String, ItemStack> sLaserGasEmitterResolver = GT6RecipesCanner::liveLaserEmitter;

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
	 * Pours the {@link GT6RecipeMaps#CANNER} stock: the 17 refill rows (the 16 colour
	 * refills + the chlorine remover, p24), the 3 food-can rows of task food-can-row0
	 * (rotten_flesh/spider_eye/cookie), the 32 C-Foam refills of task c-foam-fluid-refill
	 * (the :254 dyed + the :262 owned ladders), the laser gas fill family (qu-laser-domain +
	 * debt-laser-gas-family + debt-hene-fluid, MultiItemTechnological.java:396-403 — the
	 * FULL eight-row walk resolves in vivo: the helium item leg self-heals through the
	 * registry lookup, the heliumneon fluid leg landed with debt-hene-fluid; the offline
	 * test fixtures still arm the helium skip — the registry lookup yields nothing on an
	 * unbooted registry), PLUS the task food-meat-recipes bands: the 28 canned-material
	 * rows (Loader_Recipes_Food.java:44-51 — 4 cooked/tofu materials × the 7 ST.array
	 * prefixes), the 6 air rows (MultiItemCans.java:113-120 — 3 fills + 3 releases; the
	 * plain-air pair stays dormant on the port-absent {@code air} fluid, the
	 * pour-face-forever posture) and the 14 T3b-delegated baking rows (MultiItemFood.java
	 * :600-:782 walk; :600 = the row0 row, :624 = the NeLi TRUE NEGATIVE). The :40 WiMo
	 * row and the :41/:42 row0 pair keep their
	 * row0-card disposition (foreign TRUE NEGATIVE / already poured).
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

		// the food-can-row0 trio — Canner rows carry ZERO tools (the research card's
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

		// the c-foam-fluid-refill 32 — MultiItemRandomTools.java:254 (dyed) / :262 (owned),
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

		// the laser gas fill family — MultiItemTechnological.java:396-403: empty emitter +
		// 1 unit of gas (144 mB) → the filled emitter, one row per family gas; all eight
		// legs resolve in vivo since debt-hene-fluid (the offline fixtures arm the helium
		// skip — the registry lookup yields nothing unbooted)
		for (String tGas : LASER_GAS_FLUIDS) {
			Recipe tRow = laserGasRecipe(tGas);
			if (tRow == null) {tSkipped++; continue;} // the absent-fluid/item silent skip
			tMap.addRecipe(tRow);
			tPoured++;
		}

		// task food-meat-recipes — the :44-51 canned-material band: 4 materials × the 7
		// ST.array prefixes, RM.food_can(tStack, 2, name, IL.CANS_*) — the tier dispatch
		// (foodValue 2 → the tiny can) rides the shared foodCanRow. The input count is the
		// mat() stackSize column (CANNED_MATERIAL_COUNTS — the full material unit per line,
		// the rework food-meat-recipes-rework correction). The upstream canned display
		// names ("Canned Fish"/"Canned Meat"/"Canned Tofu"/"Canned Emerald Green")
		// stay pooled with the NEI-info face (the foodCanRow doc).
		ItemStack tMaterialEmptyCan = sFoodCanEmptyResolver.get();
		if (tMaterialEmptyCan == null || tMaterialEmptyCan.isEmpty()) {
			tSkipped += 4 * cannedMaterialPrefixes().size(); // the empty-can leg feeds every row
		} else for (CannedMaterial tSpec : cannedMaterialTable()) {
			List<OreDictPrefix> tPrefixes = cannedMaterialPrefixes();
			for (int i = 0; i < tPrefixes.size(); i++) {
				Item tMatItem = sFoodMaterialItemResolver.apply(tPrefixes.get(i), tSpec.material());
				Recipe tRow = tMatItem == null ? null
						: foodCanRow(new ItemStack(tMatItem, CANNED_MATERIAL_COUNTS.get(i)), CANNED_MATERIAL_FOOD_VALUE, tSpec.family(), tMaterialEmptyCan);
				if (tRow == null) {tSkipped++; continue;} // the mat()-null silent drop
				tMap.addRecipe(tRow);
				tPoured++;
			}
		}

		// task food-meat-recipes (scope extension) — the T3b-delegated baking band: the
		// MultiItemFood.java:600-:782 food_can walk over the port bake items (the :600 row
		// is the row0 card's; the :624 NeLi arm = the declared TRUE NEGATIVE). The explicit
		// foodValue column and the input counts carry verbatim; the cookie rows ride the
		// row0 cookies-can seam, the bread rows the CANS_BREAD ladder.
		ItemStack tBakingEmptyCan = sFoodCanEmptyResolver.get();
		if (tBakingEmptyCan == null || tBakingEmptyCan.isEmpty()) {
			tSkipped += BAKING_CANNED_MEMBERS.size();
		} else for (BakingCanMember tMember : BAKING_CANNED_MEMBERS) {
			Item tBakeFood = sBakingFoodItemResolver.apply(tMember.itemId());
			IntFunction<ItemStack> tBakeFamily = "cookies".equals(tMember.family())
					? aTier -> sCookiesCanResolver.get() : sBreadCansResolver;
			Recipe tBakeRow = tBakeFood == null ? null
					: foodCanRow(new ItemStack(tBakeFood, tMember.inputCount()), tMember.foodValue(), tBakeFamily, tBakingEmptyCan);
			if (tBakeRow == null) {tSkipped++; continue;} // the unregistered bake-item silent drop
			tMap.addRecipe(tBakeRow);
			tPoured++;
		}

		// task food-meat-recipes — the MultiItemCans.java:113-120 air band: the :113-114
		// walk-minus fill + the :115-:116 explicit fills + the :118-:120 releases, buffered
		// F verbatim. The plain-air pair stays dormant on the port-absent air fluid.
		for (String tAir : AIR_FILL_WALK) {if (pourAir(tMap, airFillRecipe(tAir))) tPoured++; else tSkipped++;}
		for (String tAir : AIR_FILL_SPECIALS) {if (pourAir(tMap, airFillRecipe(tAir))) tPoured++; else tSkipped++;}
		for (String tAir : AIR_RELEASE) {if (pourAir(tMap, airReleaseRecipe(tAir))) tPoured++; else tSkipped++;}

		sLoaded = true;
		LOGGER.info("GT6 Canner poured: {} loaded, {} skipped (unregistered dye/chlorine/can ids, = upstream FL.exists drops)", tPoured, tSkipped);
	}

	/** The pour helper: null → the silent skip (the air-band counter face). */
	private static boolean pourAir(gregtech6.recipes.maps.GT6RecipeMapCanner aMap, @Nullable Recipe aRow) {
		if (aRow == null) return false;
		aMap.addRecipe(aRow);
		return true;
	}

	/**
	 * The MultiItemCans.java:113-116 fill row for ONE air id — buffered F, EUt 16, duration
	 * 64, the empty can in, the air fluid × 16000 mB in, the filled can out. Null when any
	 * leg fails to resolve (the silent skip — the plain-air dormancy face).
	 */
	@Nullable
	static Recipe airFillRecipe(String aAirId) {
		Fluid tAir = sAirFluidResolver.apply(aAirId);
		if (tAir == null) return null;
		ItemStack tEmpty = sFoodCanEmptyResolver.get();
		if (tEmpty == null || tEmpty.isEmpty()) return null;
		ItemStack tCan = sAirCanResolver.apply(aAirId);
		if (tCan == null || tCan.isEmpty()) return null;
		// upstream :113-116 — RM.Canner.addRecipe1(F, 16, 64, IL.Food_Can_Empty.get(1), FL.make(tAir, 16000), NF, IL.Food_Can_Air_*.get(1))
		return new Recipe(false,
				new ItemStack[] {tEmpty.copy()}, new ItemStack[] {tCan.copy()},
				new FluidStack[] {new FluidStack(tAir, AIR_MB)},
				null,
				AIR_FILL_DURATION, AIR_EUT, 0);
	}

	/**
	 * The MultiItemCans.java:118-120 release row for ONE air id — buffered F, EUt 16,
	 * duration 16, the filled can in, the air fluid × 16000 mB + the empty can out. Null
	 * when any leg fails to resolve.
	 */
	@Nullable
	static Recipe airReleaseRecipe(String aAirId) {
		Fluid tAir = sAirFluidResolver.apply(aAirId);
		if (tAir == null) return null;
		ItemStack tEmpty = sFoodCanEmptyResolver.get();
		if (tEmpty == null || tEmpty.isEmpty()) return null;
		ItemStack tCan = sAirCanResolver.apply(aAirId);
		if (tCan == null || tCan.isEmpty()) return null;
		// upstream :118-120 — RM.Canner.addRecipe1(F, 16, 16, IL.Food_Can_Air_*.get(1), NF, FL.Air_*.make(16000), IL.Food_Can_Empty.get(1))
		return new Recipe(false,
				new ItemStack[] {tCan.copy()}, new ItemStack[] {tEmpty.copy()},
				null,
				new FluidStack[] {new FluidStack(tAir, AIR_MB)},
				AIR_RELEASE_DURATION, AIR_EUT, 0);
	}

	/**
	 * The MultiItemTechnological.java:396-403 row for ONE family gas — buffered T, EUt 16,
	 * duration 128, the empty gas laser emitter in, {@code MT.<gas>.gas(U, T)} = 144 mB of
	 * the gas fluid in, the filled emitter out. Null when any leg fails to resolve (the
	 * silent skip: the offline-test helium arm, whose registry lookup yields nothing).
	 */
	@Nullable
	static Recipe laserGasRecipe(String aGas) {
		Fluid tGas = sLaserGasFluidResolver.apply(aGas);
		if (tGas == null) return null;
		ItemStack tEmpty = sLaserGasEmptyResolver.get();
		if (tEmpty == null || tEmpty.isEmpty()) return null;
		ItemStack tEmitter = sLaserGasEmitterResolver.apply(aGas);
		if (tEmitter == null || tEmitter.isEmpty()) return null;
		// upstream :396-403 — RM.Canner.addRecipe1(T, 16, 128, IL.Comp_Laser_Gas_Empty.get(1), MT.<gas>.gas(U, T), NF, IL.Comp_Laser_Gas_<X>.get(1))
		return new Recipe(true,
				new ItemStack[] {tEmpty}, new ItemStack[] {tEmitter},
				new FluidStack[] {new FluidStack(tGas, LASER_GAS_MB)},
				null,
				LASER_GAS_DURATION, LASER_GAS_EUT, 0);
	}

	/** The live gas leg: the {@link GTFluids#CHEMICALS} source fluid of the name, null when absent (the sCarbonDioxideResolver walk, family-shaped). */
	static Fluid liveLaserGas(String aGas) {
		for (GTFluids.ChemicalFluid tChemical : GTFluids.CHEMICALS) {
			if (tChemical.spec.name().equals(aGas)) return tChemical.source.get();
		}
		return null;
	}

	/**
	 * The live emitter leg: the fluid name → the {@code gt6:comp_laser_gas_<x>} item. The
	 * helium row is the REGISTRY-LOOKUP exception (the usb-branch item, see the seam javadoc);
	 * the seven other gases close over this repo's RegistryObjects (the direct switch — no
	 * RegistryObject local, the stonecutter swap table never touches this method).
	 */
	static ItemStack liveLaserEmitter(String aGas) {
		if ("helium".equals(aGas)) {
			Item tHe = ForgeRegistries.ITEMS.getValue(new net.minecraft.resources.ResourceLocation("gt6", "comp_laser_gas_he"));
			return tHe == null || tHe == Items.AIR ? ItemStack.EMPTY : new ItemStack(tHe);
		}
		return switch (aGas) {
			case "neon" -> new ItemStack(gregtech6.items.GT6LaserGas.COMP_LASER_GAS_NE.get());
			case "argon" -> new ItemStack(gregtech6.items.GT6LaserGas.COMP_LASER_GAS_AR.get());
			case "krypton" -> new ItemStack(gregtech6.items.GT6LaserGas.COMP_LASER_GAS_KR.get());
			case "xenon" -> new ItemStack(gregtech6.items.GT6LaserGas.COMP_LASER_GAS_XE.get());
			case "heliumneon" -> new ItemStack(gregtech6.items.GT6LaserGas.COMP_LASER_GAS_HENE.get());
			case "carbonmonoxide" -> new ItemStack(gregtech6.items.GT6LaserGas.COMP_LASER_GAS_CO.get());
			case "carbondioxide" -> new ItemStack(gregtech6.items.GT6LaserGas.COMP_LASER_GAS_CO2.get());
			default -> ItemStack.EMPTY;
		};
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
	public static void resetForTest() {sLoaded = false; sCannedMaterials = null;}

	private GT6RecipesCanner() {}
}
