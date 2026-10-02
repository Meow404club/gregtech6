/**
 * Copyright (c) 2025 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3, or (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see http://www.gnu.org/licenses/lgpl-3.0.txt
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

import gregapi.data.CS;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.fluid.GTFluids;

/**
 * The meat/fish oredict-listener walk (task food-meat-recipes) — the port of
 * {@code Loader_Recipes_Food.java:350-516}, the listener block expanded STATICALLY over
 * the declared port member universe (the GT6RecipesBees listener-expansion form: the
 * port has no oredict list layer, so the member tables below ARE the universe and the
 * tests pin them bidirectional-exact). Members = the upstream GT6+vanilla(1.7.10)
 * registrations, mapped by identity: the GT6 foods re-register through
 * {@code LoaderOreDictReRegistrations.java:697-767} (foodHamraw → listAllhamraw →
 * listAllmeatraw, foodMuleraw → listAllhorseraw, ...) and the vanilla meats/fish
 * register + carry data through {@code LoaderItemData.java:2589-2600}.
 *
 * <p><b>The four poured faces</b> (the card SPEC's 三面 + the two named extras):
 * <ul>
 * <li><b>food_can</b> — 30 rows into {@link GT6RecipeMaps#CANNER}, the
 *     {@code :404-:514} family listeners verbatim: {@code RM.food_can(stack,
 *     Math.max(1, ST.food(stack)), name, rotten ? CANS_ROTTEN : family)}. Port
 *     {@code ST.food} = the item nutrition (ST.java:897 — the FoodStat level face);
 *     the nutrition column of the member table IS that transcription. Every port
 *     member is non-rotten, so the family ternary collapses to the non-rotten leg
 *     (declared). Rows ride the shared {@link GT6RecipesCanner#foodCanRow} (the row0
 *     row shape, the tier dispatch verbatim).</li>
 * <li><b>Fermenter</b> — 12 rows into {@link GT6RecipeMaps#FERMENTER}: every
 *     {@code listAllmeatraw} member spoils to rotten flesh ({@code :354/:357}, buffered
 *     T, EUt 16, duration 288). The fish members skip (the {@code :351} guard excludes
 *     listAllfishraw).</li>
 * <li><b>Mortar</b> — 16 rows into {@link GT6RecipeMaps#MORTAR}: the 绞碎 face, raw
 *     meats → the MeatRaw dust ({@code :364}), raw fish → the FishRaw dust
 *     ({@code :384}), EUt 16, duration 16. The output amount = the MEAT-property
 *     material sum of the member's OreDictItemData, run through the
 *     {@link #dustLadder} transcription of {@code OM.java:460-468} (verbatim; the port
 *     OM util carries no dust face). The rotten-dust arm ({@code :362/:382}) collapses:
 *     no port member carries the ROTTEN property (declared).</li>
 * <li><b>Bath</b> — 1 row into {@link GT6RecipeMaps#BATH}: the {@code :430} Rib_BBQ
 *     face, the ribcooked member + BBQ sauce 250 mB → the barbecue ribs, EUt 0,
 *     duration 16, buffered T (the fluid lives upstream as FL.Sauce_BBQ, port id
 *     {@code bbqsauce}, GTFluids FOOD_B1 row).</li>
 * </ul>
 *
 * <p><b>Declared non-pours</b> (the :350-516 census, every line accounted):
 * <ul>
 * <li><b>TRUE NEGATIVE — the :393-398 chum Mixer rows</b>: the five-stack loop
 *     ({@code IL.Food_Potato_Poisonous, IL.FZ_Sludge, IL.IE_Slag, IL.TE_Slag,
 *     IL.TE_Slag_Rich}) is entirely invalid in the port — the poisonous potato is not
 *     registered (T4a carried no potato row) and the three slag identities are foreign
 *     (Forestry/IE/TE, the b2_unlock_verdict 口径). Zero iterations, zero rows. Unlock:
 *     a poisonous-potato item card re-arms the loop.</li>
 * <li><b>TRUE NEGATIVE — the nine foreign-only family listeners</b>: rabbit (:452),
 *     turkey (:457), crab (:462), rat (:467), turtle (:472), ostrich (:477), venison
 *     (:482), titan (:487), hydra (:512) have zero port members (1.7.10 vanilla carried
 *     none of these; the lists were mod-compat faces). The 1.20.1-only vanilla
 *     mutton/rabbit pair pours NO rows for the same reason — no 1.7.10 counterpart
 *     means no upstream row to transcribe.</li>
 * <li><b>POOLED — the Sandwiches.INGREDIENTS seats</b> (:366/:386/:419 and the seat
 *     rows throughout): the Sandwiches mechanism card (the T4a pool declaration).</li>
 * <li><b>POOLED — the FoodsGT.put arms</b> (:391/:418/:424-:515): the sandwich/canning
 *     data domain, zero port surface (the GT6Foods doc pool, the T4b rows).</li>
 * <li><b>SKIPPED_UPSTREAM — the :373 fish generify row</b> (RM.generify raw fish →
 *     vanilla fish): a GENERIFIER-map row face, deferred.</li>
 * <li><b>SKIPPED_UPSTREAM — the :412-413 Squeezer/Juicer fish-oil rows</b>: the OM
 *     byproduct-dust dynamic walk (MT.FishOil liquid + per-member byproduct), a data
 *     face deferred.</li>
 * </ul>
 *
 * <p>Load timing: the GT6RecipesBees form — self-contained MOD-bus listener at
 * FMLCommonSetup.enqueueWork, generation-tracked pour flag, one log line per map.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6RecipesMeat {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** One canned-walk member — a {@code :404-:514} family listener registration line. */
	public record CannedMember(String note, String itemId, int nutrition, String family) {}

	/** One raw-meat/fish Mortar+Fermenter member — the item plus its OreDictItemData meat sum (the amount column verbatim). */
	public record RawMember(String note, String itemId, long meatAmount) {}

	/** One dust-ladder result — the OM.dust single-stack face as (prefix, count). */
	public record DustOut(OreDictPrefix prefix, int count) {}

	/** The resolution seams: live lookups by default, fixtures injected offline (the Bees precedent). */
	public static Function<String, Item> sFoodItemResolver = GT6RecipesMeat::resolveFoodItem;
	public static BiFunction<OreDictPrefix, OreDictMaterial, Item> sDustItemResolver = GT6RecipesMixer::resolveItem;
	/** The BBQ sauce seam — the {@code GTFluids.BBQSAUCE} AquaFluid carrier's source (the FOOD_B1 fluid), wrapped offline-safe: the forge test JVM RESOLVES the gt6 ITEMS (the rib leg does NOT null out there — the 07:53 XML empirics) but leaves the gt6 FLUIDS unbound, so the bare {@code .get()} THROWS ("Registry Object not present: gt6:bbqsauce") and the shared-test pour dies — the null keeps the upstream FL.exists silent drop (the liveFamilyCan wrap precedent). */
	public static Supplier<Fluid> sBbqSauceResolver = GT6RecipesMeat::liveBbqSauce;

	/** The offline-safe sauce leg (the GT6RecipesCanner.liveFamilyCan wrap form). */
	@Nullable
	static Fluid liveBbqSauce() {
		try {return GTFluids.BBQSAUCE.source.get();} catch (RuntimeException tOffline) {return null;}
	}

	/** The live item walk over both namespaces (null when absent — the silent-drop face). */
	@Nullable
	static Item resolveFoodItem(String aId) {
		int tColon = aId.indexOf(':');
		return ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
				tColon < 0 ? "gt6" : aId.substring(0, tColon), aId.substring(tColon + 1)));
	}

	/** The canned-walk members, upstream listener order :404→:514 (bidirectional-exact universe). */
	public static final List<CannedMember> CANNED_MEMBERS = List.of(
			// :404 listAllfishraw (LoaderItemData.java:2597-2600 the four 1.7.10 fish metas → the 1.20.1 identities)
			new CannedMember(":404 fishraw cod"          , "minecraft:cod"           , 2, "fish"),
			new CannedMember(":404 fishraw salmon"       , "minecraft:salmon"        , 2, "fish"),
			new CannedMember(":404 fishraw tropical"     , "minecraft:tropical_fish" , 1, "fish"),
			new CannedMember(":404 fishraw pufferfish"   , "minecraft:pufferfish"    , 1, "fish"),
			// :417 listAllfishcooked (LoaderItemData.java:2593+2605-2606 the two cooked metas)
			new CannedMember(":417 fishcooked cod"       , "minecraft:cooked_cod"    , 5, "fish"),
			new CannedMember(":417 fishcooked salmon"    , "minecraft:cooked_salmon" , 6, "fish"),
			// :423/:428 listAllrib* (GT6 foodRibraw/foodRibcooked, LoaderOreDictReRegistrations.java:699-700)
			new CannedMember(":423 ribraw"               , "gt6:food_rib_raw"        , 3, "meat"),
			new CannedMember(":428 ribcooked"            , "gt6:food_rib_cooked"     , 10, "meat"),
			// :434 listAllbeef* (ribeye via :697-698 + vanilla via LoaderItemData.java:2590/:2595)
			new CannedMember(":434 beefraw ribeye"       , "gt6:food_ribeyesteak_raw", 3, "meat"),
			new CannedMember(":434 beefraw vanilla"      , "minecraft:beef"          , 3, "meat"),
			new CannedMember(":434 beefcooked ribeye"    , "gt6:food_ribeyesteak_cooked", 10, "meat"),
			new CannedMember(":434 beefcooked vanilla"   , "minecraft:cooked_beef"   , 8, "meat"),
			// :439 listAllchicken* (vanilla only, LoaderItemData.java:2591/:2596)
			new CannedMember(":439 chickenraw"           , "minecraft:chicken"       , 2, "meat"),
			new CannedMember(":439 chickencooked"        , "minecraft:cooked_chicken", 6, "meat"),
			// :444 listAllmutton* (GT6 mutton only, :727-728; the 1.20.1 vanilla mutton pair has no 1.7.10 counterpart)
			new CannedMember(":444 muttonraw"            , "gt6:food_mutton_raw"     , 2, "meat"),
			new CannedMember(":444 muttoncooked"         , "gt6:food_mutton_cooked"  , 7, "meat"),
			// :449 listAllpork* (vanilla only, LoaderItemData.java:2589/:2594)
			new CannedMember(":449 porkraw"              , "minecraft:porkchop"      , 3, "meat"),
			new CannedMember(":449 porkcooked"           , "minecraft:cooked_porkchop", 8, "meat"),
			// :494 listAllham* (GT6 ham head only — the :534-:535 slices carry no oredict name)
			new CannedMember(":494 hamraw"               , "gt6:food_ham_raw"        , 3, "meat"),
			new CannedMember(":494 hamcooked"            , "gt6:food_ham_cooked"     , 10, "meat"),
			// :499 listAllhorse* (horse+mule+donkey via :719-:724, all three ride the horse lists)
			new CannedMember(":499 horseraw"             , "gt6:food_horse_raw"      , 2, "meat"),
			new CannedMember(":499 horsecooked"          , "gt6:food_horse_cooked"   , 8, "meat"),
			new CannedMember(":499 muleraw"              , "gt6:food_mule_raw"       , 3, "meat"),
			new CannedMember(":499 mulecooked"           , "gt6:food_mule_cooked"    , 10, "meat"),
			new CannedMember(":499 donkeyraw"            , "gt6:food_donkey_raw"     , 2, "meat"),
			new CannedMember(":499 donkeycooked"         , "gt6:food_donkey_cooked"  , 8, "meat"),
			// :504 listAlldog* (:725-726)  :509 foodBacon* (:703-704 — GT6 bacon registers the meat lists directly)
			new CannedMember(":504 dograw"               , "gt6:food_dogmeat_raw"    , 2, "meat"),
			new CannedMember(":504 dogcooked"            , "gt6:food_dogmeat_cooked" , 8, "meat"),
			new CannedMember(":509 baconraw"             , "gt6:food_bacon_raw"      , 1, "meat"),
			new CannedMember(":509 baconcooked"          , "gt6:food_bacon_cooked"   , 3, "meat"));

	/** The :430 Bath input column — the ribcooked members (one port member). */
	public static final List<String> RIB_COOKED_MEMBERS = List.of("gt6:food_rib_cooked");

	/**
	 * The Fermenter+Mortar raw members — the {@code listAllmeatraw} face (fish excluded
	 * by the :351 guard, the fish ride their own :369 face below). The meatAmount column
	 * = the MEAT-property sum of the member's data (MultiItemFood.java:529/:541/:546/
	 * :553/:558/:563/:568/:572/:576 for the GT6 rows, LoaderItemData.java:2589-2591 for
	 * the vanilla trio — all MT.MeatRaw, Bone never carries the property).
	 */
	public static final List<RawMember> RAW_MEAT_MEMBERS = List.of(
			new RawMember(":529 ham raw"        , "gt6:food_ham_raw"        , CS.U * 2),
			new RawMember(":541 bacon raw"      , "gt6:food_bacon_raw"      , CS.U2),
			new RawMember(":546 rib raw"        , "gt6:food_rib_raw"        , CS.U * 2),
			new RawMember(":553 ribeye raw"     , "gt6:food_ribeyesteak_raw", CS.U * 3),
			new RawMember(":558 dogmeat raw"    , "gt6:food_dogmeat_raw"    , CS.U * 2),
			new RawMember(":563 mutton raw"     , "gt6:food_mutton_raw"     , CS.U * 2),
			new RawMember(":568 horse raw"      , "gt6:food_horse_raw"      , CS.U * 2),
			new RawMember(":572 mule raw"       , "gt6:food_mule_raw"       , 5 * CS.U2),
			new RawMember(":576 donkey raw"     , "gt6:food_donkey_raw"     , 5 * CS.U2),
			new RawMember(":2589 vanilla pork"  , "minecraft:porkchop"      , CS.U * 2),
			new RawMember(":2590 vanilla beef"  , "minecraft:beef"          , CS.U * 2),
			new RawMember(":2591 vanilla chicken", "minecraft:chicken"      , CS.U * 2));

	/**
	 * The raw-fish Mortar members — the :369-387 face (no Fermenter arm on this
	 * listener). Amounts = the fish meta data (LoaderItemData.java:2597-2600), MT.FishRaw.
	 */
	public static final List<RawMember> RAW_FISH_MEMBERS = List.of(
			new RawMember(":2597 vanilla cod"       , "minecraft:cod"           , CS.U * 2),
			new RawMember(":2598 vanilla salmon"    , "minecraft:salmon"        , CS.U * 2),
			new RawMember(":2599 vanilla tropical"  , "minecraft:tropical_fish" , CS.U * 2),
			new RawMember(":2600 vanilla pufferfish", "minecraft:pufferfish"    , CS.U));

	/**
	 * The {@code OM.java:460-468} dust ladder, verbatim (the single-stack face — the
	 * port OM util carries no dust helper; the upstream {@code mat()} null face becomes
	 * the resolver null face at pour time).
	 */
	public static DustOut dustLadder(long aMaterialAmount) {
		if (aMaterialAmount < CS.U72 || aMaterialAmount < 0) return null;
		if (aMaterialAmount >= CS.U * 72) return new DustOut(OP.blockDust, bindStack(aMaterialAmount / (CS.U * 9)));
		if (aMaterialAmount >= CS.U && (aMaterialAmount >= CS.U * 16 || aMaterialAmount % CS.U == 0))
			return new DustOut(OP.dust, bindStack(aMaterialAmount / CS.U));
		if (aMaterialAmount >= CS.U4 && (aMaterialAmount >= CS.U * 8 || aMaterialAmount % CS.U4 <= aMaterialAmount % CS.U9))
			return new DustOut(OP.dustSmall, bindStack((aMaterialAmount * 4) / CS.U));
		if (aMaterialAmount >= CS.U9 && (aMaterialAmount >= CS.U || aMaterialAmount % CS.U9 <= aMaterialAmount % CS.U72))
			return new DustOut(OP.dustTiny, bindStack((aMaterialAmount * 9) / CS.U));
		return new DustOut(OP.dustDiv72, bindStack((aMaterialAmount * 72) / CS.U));
	}

	/** The UT.Code.bindStack face (the 0..64 item-count clamp). */
	private static int bindStack(long aNumber) {
		long tNumber = aNumber < 1 ? 0 : Math.min(64, aNumber);
		return (int)tNumber;
	}

	/** Poured flag — one generation, one pour (upstream loaders run once per JVM). */
	private static boolean sLoaded = false;

	// ADR-P18: the pour-flag joins the GT6RecipeMaps generation (the Bees form).
	static {GT6RecipeMaps.registerGenerationResetHook(GT6RecipesMeat::resetForTest);}

	/** FMLCommonSetup.enqueueWork — the items/fluids DeferredRegisters have fired by this point. */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(GT6RecipesMeat::load);
	}

	/**
	 * Pours the four faces: 30 canned rows into CANNER, 12 Fermenter rows, 16 Mortar
	 * rows, 1 Bath row. Idempotent; unresolvable rows skip with a count (the upstream
	 * mat()/FL.exists silent-drop semantics).
	 */
	public static synchronized void load() {
		if (sLoaded) {LOGGER.debug("load() skipped: already poured (generation flag set)"); return;}
		GT6RecipeMaps.init(); // defensive + idempotent: the maps exist from ConstructMod
		if (GT6RecipeMaps.CANNER == null || GT6RecipeMaps.FERMENTER == null
				|| GT6RecipeMaps.MORTAR == null || GT6RecipeMaps.BATH == null) return;

		int tPoured = 0, tSkipped = 0;
		for (CannedMember tMember : CANNED_MEMBERS) {
			Recipe tRow = cannedRow(tMember);
			if (tRow == null) {tSkipped++; continue;} // = upstream mat()/ST.food null drop
			GT6RecipeMaps.CANNER.addRecipe(tRow);
			tPoured++;
		}
		LOGGER.info("GT6 Meat food_can poured: {} loaded, {} skipped (unregistered food/can ids, = upstream FL.exists drops)", tPoured, tSkipped);

		tPoured = 0; tSkipped = 0;
		for (RawMember tMember : RAW_MEAT_MEMBERS) {
			Recipe tRow = fermenterRow(tMember);
			if (tRow == null) {tSkipped++; continue;}
			GT6RecipeMaps.FERMENTER.addRecipe(tRow);
			tPoured++;
		}
		LOGGER.info("GT6 Meat fermenter poured: {} loaded, {} skipped (the :354/:357 rotten-flesh face)", tPoured, tSkipped);

		tPoured = 0; tSkipped = 0;
		for (RawMember tMember : RAW_MEAT_MEMBERS) {
			Recipe tRow = mortarRow(tMember, MT.MeatRaw);
			if (tRow == null) {tSkipped++; continue;}
			GT6RecipeMaps.MORTAR.addRecipe(tRow);
			tPoured++;
		}
		for (RawMember tMember : RAW_FISH_MEMBERS) {
			Recipe tRow = mortarRow(tMember, MT.FishRaw);
			if (tRow == null) {tSkipped++; continue;}
			GT6RecipeMaps.MORTAR.addRecipe(tRow);
			tPoured++;
		}
		LOGGER.info("GT6 Meat mortar poured: {} loaded, {} skipped (the :364/:384 dust faces, the OM.java:460-468 ladder)", tPoured, tSkipped);

		tPoured = 0; tSkipped = 0;
		for (String tRib : RIB_COOKED_MEMBERS) {
			Recipe tRow = bathRow(tRib);
			if (tRow == null) {tSkipped++; continue;}
			GT6RecipeMaps.BATH.addRecipe(tRow);
			tPoured++;
		}
		LOGGER.info("GT6 Meat bath poured: {} loaded, {} skipped (the :430 Rib_BBQ face)", tPoured, tSkipped);
		sLoaded = true;
	}

	/** One canned row — the :404 family form over the shared row0 row shape (Math.max(1, ST.food) → the nutrition column). */
	@Nullable
	static Recipe cannedRow(CannedMember aMember) {
		Item tFood = sFoodItemResolver.apply(aMember.itemId());
		if (tFood == null) return null;
		IntFunction<ItemStack> tFamily = "fish".equals(aMember.family())
				? GT6RecipesCanner.sFishCansResolver : GT6RecipesCanner.sMeatCansResolver;
		ItemStack tEmpty = GT6RecipesCanner.sFoodCanEmptyResolver.get();
		if (tEmpty == null || tEmpty.isEmpty()) return null;
		// upstream :404 etc. — RM.food_can(tStack, Math.max(1, ST.food(tStack)), name, rotten ? CANS_ROTTEN : family)
		return GT6RecipesCanner.foodCanRow(new ItemStack(tFood, 1), Math.max(1, aMember.nutrition()), tFamily, tEmpty);
	}

	/** One Fermenter row — :354/:357 verbatim: raw meat → rotten flesh, buffered T, EUt 16, duration 288, no fluids. */
	@Nullable
	static Recipe fermenterRow(RawMember aMember) {
		Item tFood = sFoodItemResolver.apply(aMember.itemId());
		if (tFood == null) return null;
		return new Recipe(true,
				new ItemStack[] {new ItemStack(tFood, 1)},
				new ItemStack[] {new ItemStack(Items.ROTTEN_FLESH, 1)},
				null, null,
				288, 16, 0);
	}

	/** One Mortar row — :364/:384 verbatim: raw → the OM.dust ladder stack, buffered T, EUt 16, duration 16. */
	@Nullable
	static Recipe mortarRow(RawMember aMember, OreDictMaterial aMeat) {
		Item tFood = sFoodItemResolver.apply(aMember.itemId());
		if (tFood == null) return null;
		DustOut tDust = dustLadder(aMember.meatAmount());
		if (tDust == null) return null;
		Item tDustItem = sDustItemResolver.apply(tDust.prefix(), aMeat);
		if (tDustItem == null) return null;
		return new Recipe(true,
				new ItemStack[] {new ItemStack(tFood, 1)},
				new ItemStack[] {new ItemStack(tDustItem, tDust.count())},
				null, null,
				16, 16, 0);
	}

	/** The :430 Bath row verbatim: ribcooked + BBQ sauce 250 mB → Rib_BBQ, buffered T, EUt 0, duration 16. */
	@Nullable
	static Recipe bathRow(String aRibCookedId) {
		Item tRib = sFoodItemResolver.apply(aRibCookedId);
		if (tRib == null) return null;
		Item tBbq = sFoodItemResolver.apply("gt6:food_rib_bbq");
		if (tBbq == null) return null;
		Fluid tSauce = sBbqSauceResolver.get();
		if (tSauce == null) return null;
		return new Recipe(true,
				new ItemStack[] {new ItemStack(tRib, 1)},
				new ItemStack[] {new ItemStack(tBbq, 1)},
				new FluidStack[] {new FluidStack(tSauce, 250)},
				null,
				16, 0, 0);
	}

	/** Test seam: clears the poured flag so a fresh generation can re-pour. */
	static void resetForTest() {sLoaded = false;}

	private GT6RecipesMeat() {}
}
