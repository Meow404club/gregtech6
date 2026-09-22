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
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.RegistryObject;

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
import gregtech6.items.bees.GT6Bumbles;
import gregtech6.recipes.maps.GT6RecipeMapBumblelyzer;
import gregtech6.registry.GT6BeeCombs;
import gregtech6.registry.GTMaterialItems;

/**
 * The bee-comb recipe book (task p31-bees-lv1) — the two pours of the Lv1 static chain.
 * Both maps are the card-A DECLARED-EMPTY maps, consumed directly:
 * <ul>
 * <li><b>CENTRIFUGE — the 20 specific comb rows</b> (MultiItemFood.java:251-270, all
 *     {@code RM.Centrifuge.addRecipe1(T, 16, 64, ...)}: eUt 16, duration 64, item-output
 *     chances verbatim). The foreign-identity outputs CUT per the P10 ruling with the
 *     chances prefix-trim (the GT6RecipesSifter declared-deviation form):
 *     <ul>
 *     <li>:251 Honey comb — the FR_Propolis (Forestry) tail CUT → chances {10000, 1000}
 *         trims to {10000};</li>
 *     <li>:255 End comb — the FR_Propolis_Pulsating (Forestry, no fallback) CUT → chances
 *         {10000, 1000, 1000} trims to {10000, 1000};</li>
 *     <li>:257 Jungle comb — the FR_Propolis_Silky rides its upstream fallback identity
 *         {@code ST.make(Items.string, 1, 0)} (the fallback IS vanilla — kept, 10%);</li>
 *     <li>:262 Sticky comb — the FR_Propolis_Sticky → IL.IC2_Resin → IL.Resin fallback
 *         chain lands on foreign-mod identities with no port item — CUT → chances
 *         {10000, 3000} trims to {10000}.</li>
 *     </ul>
 *     The remaining chances-array shapes are transcribed verbatim, including the rows
 *     whose trailing chance has no aligned output at upstream (Water :252 / Rock :256 /
 *     Sandy :260 carry {10000, 1000} over ONE item output — the array is item-output-
 *     aligned and the trailing entry is dead weight upstream too, kept for transcription
 *     fidelity; the upstream {@code FL.Potion_Harm_1} / honey chances are NOT among them).</li>
 * <li><b>SQUEEZER — 20 generalization rows</b>, one per comb: 90 L Honey + WaxBee dust
 *     (Loader_Recipes_Food.java:264, the {@code OD.materialHoneycomb} listener body: eUt 16,
 *     duration 64, no chances array). <b>Declared port deviation</b>: upstream the listener
 *     fires per {@code OD.materialHoneycomb} registration — among the 20 GT combs only
 *     Comb_Honey carries the tag (:226; the CANDY/BoP/FR honey combs are the other
 *     carriers). The port replaces the oredict-listener mechanism with static rows over
 *     the registered item set (the standard port listener-expansion form), and the card
 *     SPEC pins SQUEEZER at 20 loaded rows — so the generalization body lands once per
 *     comb. The Juicer/Mortar arms of the same listener pool (no Juicer map declared, the
 *     Mortar is a tool-recipe domain).</li>
 * </ul>
 *
 * <p>Fluid identities: vanilla Water, and eleven gt6 fluids — the four honey-family rows
 * and the seven bee-row dependency rows of {@link GTFluids#HONEY_FLUID_SPECS} /
 * {@link GTFluids#BEE_ROW_FLUID_SPECS} (this card's registration face), plus the four
 * pre-existing carriers the rows reference: {@code blaze} (the p29-w4-hot-lube closure
 * carrier), {@code cactuswater} (p21 food family), {@code soulsandoil} (p29-w4-f1-chemicals)
 * and {@code lubricant} (p29-w4-hot-lube). Amounts are the 1.7.10 literals verbatim —
 * {@code CS.L = 144} (CS.java:129) and the material-liquid {@code U} → 144 L per unit
 * (the iron_molten port anchor).
 *
 * <p>Load timing: the GT6RecipesSifter form — self-contained MOD-bus listener at
 * FMLCommonSetup.enqueueWork, lazily built tables, generation-tracked pour flag, one log
 * line per map (the pour-evidence face the acceptance reads).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6RecipesBees {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** One item output: a material prefix item or a vanilla item (the GT6RecipesSifter.Slot shape, comb inputs resolved separately). */
	public record Slot(@Nullable Supplier<Item> vanilla, @Nullable OreDictPrefix prefix, @Nullable OreDictMaterial material, int count) {
		/** The vanilla output — the supplier dereferences only at pour time. */
		public static Slot vanilla(Supplier<Item> aItem, int aCount) { return new Slot(aItem, null, null, aCount); }
		/** The material output — dust/dustTiny/stick per the transcription. */
		public static Slot material(OreDictPrefix aPrefix, OreDictMaterial aMaterial, int aCount) { return new Slot(null, aPrefix, aMaterial, aCount); }
	}

	/** One fluid output leg: the gt6/vanilla fluid id and the verbatim amount (mB — the 1.7.10 FluidStack.amount unit). */
	public record FluidLeg(String fluid, int amount) {}

	/**
	 * One comb row: the comb snake fragment ({@link GT6BeeCombs#comb}), the item-aligned
	 * chances ({@code null} = the no-chances upstream listener form, every output 100%),
	 * the fluid output legs (one on every row except the Royal :263 pair), and the item
	 * outputs.
	 */
	public record BeeRow(String note, String comb, @Nullable long[] chances, List<FluidLeg> fluidOuts, Slot... outputs) {}

	/** The resolution seams: live lookups by default, fixtures injected offline (the GT6RecipesSifter/GT6RecipesDistillery precedent). */
	public static Function<String, Item> sCombResolver = GT6RecipesBees::resolveComb;
	public static Function<String, Fluid> sFluidResolver = GT6RecipesBees::resolveFluid;
	public static BiFunction<OreDictPrefix, OreDictMaterial, Item> sMaterialItemResolver = GT6RecipesBees::resolveMaterialItem;
	public static Function<Supplier<Item>, Item> sVanillaItemResolver = Supplier::get;

	/** The live comb lookup (GT6BeeCombs.comb → the registered item); null when absent. */
	@Nullable
	private static Item resolveComb(String aName) {
		RegistryObject<Item> tHandle = GT6BeeCombs.comb(aName);
		return tHandle == null ? null : tHandle.get();
	}

	/** The live fluid lookup — vanilla water, then the GTFluids spec-family walk; null when absent. */
	@Nullable
	static Fluid resolveFluid(String aFluidId) {
		if ("water".equals(aFluidId)) return Fluids.WATER;
		return GTFluids.liveFluidSource(aFluidId);
	}

	/** The live material-item lookup (GTMaterialItems.get — null when the pair has no item-path item). */
	@Nullable
	private static Item resolveMaterialItem(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		RegistryObject<Item> tHandle = GTMaterialItems.get(aPrefix, aMaterial);
		return tHandle == null ? null : tHandle.get();
	}

	/**
	 * The 20 centrifuge rows, order = the upstream file order (:251-270). Lazily built — the
	 * {@code @EventBusSubscriber} scan class-loads at MOD CONSTRUCTION, before MT.init()
	 * (the a9027ac lesson).
	 */
	private static volatile List<BeeRow> sCentrifugeRows = null;

	/** The centrifuge transcription, captured on first use. */
	public static List<BeeRow> centrifugeTable() {
		List<BeeRow> tTable = sCentrifugeRows;
		if (tTable == null) sCentrifugeRows = tTable = List.of(
			// MultiItemFood.java:251-270 — the 20 comb rows verbatim; the FR propolis tails
			// CUT with their chances (the class-doc cut list)
			new BeeRow(":251", "honey"  , new long[] {10000}, List.of(new FluidLeg("honey", 100)),
					Slot.material(OP.dust, MT.WaxBee, 1)),
			new BeeRow(":252", "water"  , new long[] {10000, 1000}, List.of(new FluidLeg("water", 1000)),
					Slot.material(OP.dust, MT.WaxBee, 1)),
			new BeeRow(":253", "magic"  , new long[] {10000, 1000}, List.of(new FluidLeg("ambrosia", 100)),
					Slot.material(OP.dust, MT.WaxMagic, 1)),
			new BeeRow(":254", "nether" , new long[] {10000, 1000}, List.of(new FluidLeg("blaze", 144)),
					Slot.material(OP.dust, MT.WaxRefractory, 1)),
			new BeeRow(":255", "end"    , new long[] {10000, 1000}, List.of(new FluidLeg("dragon_breath", 125)),
					Slot.material(OP.dust, MT.Endstone, 1),
					Slot.vanilla(() -> Items.CHORUS_FRUIT, 1)),
			new BeeRow(":256", "rock"   , new long[] {10000, 1000}, List.of(new FluidLeg("concrete", 144)),
					Slot.material(OP.dust, MT.Stone, 1)),
			new BeeRow(":257", "jungle" , new long[] {10000, 1000}, List.of(new FluidLeg("chocolate_molten", 144)),
					Slot.material(OP.dust, MT.Cocoa, 1),
					Slot.vanilla(() -> Items.STRING, 1)),
			new BeeRow(":258", "frozen" , new long[] {10000, 1000}, List.of(new FluidLeg("ice", 1000)),
					Slot.material(OP.dust, MT.Ice, 1)),
			new BeeRow(":259", "shroom" , new long[] {6000, 6000}, List.of(new FluidLeg("soup_mushroom", 1000)),
					Slot.vanilla(() -> Blocks.RED_MUSHROOM_BLOCK.asItem(), 1),
					Slot.vanilla(() -> Blocks.BROWN_MUSHROOM_BLOCK.asItem(), 1)),
			new BeeRow(":260", "sandy"  , new long[] {10000, 1000}, List.of(new FluidLeg("cactuswater", 100)),
					Slot.vanilla(() -> Blocks.SAND.asItem(), 1)),
			new BeeRow(":261", "clay"   , new long[] {2000, 2000, 2000, 2000, 2000, 2000}, List.of(new FluidLeg("concrete", 144)),
					Slot.material(OP.dust, MT.Clay, 1),
					Slot.material(OP.dust, MT.ClayBrown, 1),
					Slot.material(OP.dust, MT.ClayRed, 1),
					Slot.material(OP.dust, MT.Bentonite, 1),
					Slot.material(OP.dust, MT.Palygorskite, 1),
					Slot.material(OP.dust, MT.Kaolinite, 1)),
			new BeeRow(":262", "sticky" , new long[] {10000}, List.of(new FluidLeg("latex", 144)),
					Slot.material(OP.dust, MT.WaxBee, 1)),
			new BeeRow(":263", "royal"  , new long[] {10000}, List.of(new FluidLeg("honey", 50), new FluidLeg("royal_jelly", 10)),
					Slot.material(OP.dust, MT.WaxBee, 1)),
			new BeeRow(":264", "soul"   , new long[] {10000, 9000}, List.of(new FluidLeg("soulsandoil", 50)),
					Slot.material(OP.dust, MT.WaxSoulful, 1),
					Slot.material(OP.dust, MT.SoulSand, 1)),
			new BeeRow(":265", "amnesic", new long[] {10000, 1000}, List.of(new FluidLeg("lubricant", 1000)),
					Slot.material(OP.dust, MT.WaxAmnesic, 1)),
			new BeeRow(":266", "military", new long[] {10000, 500, 500, 250}, List.of(new FluidLeg("potion_harm_1", 50)),
					Slot.material(OP.dust, MT.Bone, 1),
					Slot.vanilla(() -> Items.BONE, 1),
					Slot.vanilla(() -> Items.ROTTEN_FLESH, 1),
					Slot.vanilla(() -> Items.SPIDER_EYE, 1)),
			new BeeRow(":267", "pyro"   , new long[] {10000, 1000}, List.of(new FluidLeg("blaze", 72)),
					Slot.material(OP.dustTiny, MT.Blaze, 1),
					Slot.material(OP.stick, MT.Blaze, 1)),
			new BeeRow(":268", "cryo"   , new long[] {10000, 1000}, List.of(new FluidLeg("ice", 500)),
					Slot.material(OP.dustTiny, MT.Blizz, 1),
					Slot.material(OP.stick, MT.Blizz, 1)),
			new BeeRow(":269", "aero"   , new long[] {10000, 500, 500}, List.of(new FluidLeg("dragon_breath", 50)),
					Slot.material(OP.dustTiny, MT.Blitz, 1),
					Slot.material(OP.stick, MT.Blitz, 1),
					Slot.material(OP.stick, MT.Breeze, 1)),
			new BeeRow(":270", "tera"   , new long[] {10000, 1000}, List.of(new FluidLeg("concrete", 144)),
					Slot.material(OP.dustTiny, MT.Basalz, 1),
					Slot.material(OP.stick, MT.Basalz, 1)));
		return tTable;
	}

	/** The 20 squeezer generalization rows — the materialHoneycomb listener body per comb (the class-doc deviation). */
	private static volatile List<BeeRow> sSqueezerRows = null;

	/** The squeezer transcription, captured on first use (one row per comb, declaration order). */
	public static List<BeeRow> squeezerTable() {
		List<BeeRow> tTable = sSqueezerRows;
		if (tTable == null) {
			java.util.ArrayList<BeeRow> tRows = new java.util.ArrayList<>(GT6BeeCombs.COMB_SPECS.size());
			for (GT6BeeCombs.CombSpec tSpec : GT6BeeCombs.COMB_SPECS) {
				tRows.add(new BeeRow("Listener:264/" + tSpec.itemId(), tSpec.name(), null, List.of(new FluidLeg("honey", 90)),
						Slot.material(OP.dust, MT.WaxBee, 1)));
			}
			sSqueezerRows = tTable = java.util.List.copyOf(tRows);
		}
		return tTable;
	}

	/** Poured flag — one generation, one pour (upstream loaders run once per JVM). */
	private static boolean sLoaded = false;

	// ADR-P18: the pour-flag joins the GT6RecipeMaps generation (the Sifter/Distillery form).
	static {GT6RecipeMaps.registerGenerationResetHook(GT6RecipesBees::resetForTest);}

	/** FMLCommonSetup.enqueueWork — the items/fluids DeferredRegisters have fired by this point. */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(GT6RecipesBees::load);
	}

	/**
	 * Pours the 20 comb rows into CENTRIFUGE and the 20 generalization rows into SQUEEZER.
	 * Idempotent; unresolvable rows skip with a count (the upstream silent-drop semantics).
	 */
	public static synchronized void load() {
		if (sLoaded) {LOGGER.debug("load() skipped: already poured (generation flag set)"); return;}
		GT6RecipeMaps.init(); // defensive + idempotent: the maps exist from ConstructMod (card A)
		RecipeMap tCentrifuge = GT6RecipeMaps.CENTRIFUGE, tSqueezer = GT6RecipeMaps.SQUEEZER;
		if (tCentrifuge == null || tSqueezer == null) return; // reset() between init and load — a broken lifecycle

		int tPoured = 0, tSkipped = 0;
		for (BeeRow tRow : centrifugeTable()) {
			Recipe tRecipe = buildCentrifugeRecipe(tRow);
			if (tRecipe == null) {tSkipped++; continue;} // = upstream mat() → null silent drop
			tCentrifuge.addRecipe(tRecipe);
			tPoured++;
		}
		LOGGER.info("GT6 Bee centrifuge poured: {} loaded, {} skipped (unresolvable comb/fluid/items, = upstream mat() null drops)", tPoured, tSkipped);

		tPoured = 0; tSkipped = 0;
		for (BeeRow tRow : squeezerTable()) {
			Recipe tRecipe = buildSqueezerRecipe(tRow);
			if (tRecipe == null) {tSkipped++; continue;}
			tSqueezer.addRecipe(tRecipe);
			tPoured++;
		}
		LOGGER.info("GT6 Bee squeezer poured: {} loaded, {} skipped (unresolvable comb/fluid/items, = upstream mat() null drops)", tPoured, tSkipped);
		// task p34-machines-bumblelyzer-crucible — the Bumblelyzer scan display stock joins
		// the bee loader (the same timing: the maps exist, the bee items/fluids registered;
		// the display stock census is the acceptance's 行数对账 read). The try face keeps the
		// OFFLINE legs alive (the frozen registry forbids the live RegistryObject walk — the
		// GT6RecipesBeesTest posture; the census test swaps the seams and drives the fill
		// directly, and a live throw would surface in the RCON arm).
		try {
			GT6Bumbles.addScanFakeRecipes();
			LOGGER.info("GT6 Bee bumblelyzer display stock: {} fake rows (the :581-588 make() walk over {} species)",
					GT6RecipeMapBumblelyzer.sFakeRecipes.size(), GT6Bumbles.SPECIES.size());
		} catch (RuntimeException tOffline) {
			LOGGER.info("GT6 Bee bumblelyzer display stock skipped: the live item/fluid seams are unbound in this JVM ({})", tOffline.toString());
		}
		sLoaded = true;
	}

	/**
	 * Comb row → centrifuge Recipe, or null when any segment fails to resolve (the upstream
	 * silent-drop semantics). eUt 16, duration 64 (every :251-270 row); chances verbatim,
	 * null = the no-chances form.
	 */
	static Recipe buildCentrifugeRecipe(BeeRow aRow) {
		return buildRecipe(aRow, aRow.chances());
	}

	/** Comb row → squeezer Recipe: the listener form has NO chances array (every output 100%). */
	static Recipe buildSqueezerRecipe(BeeRow aRow) {
		return buildRecipe(aRow, null);
	}

	/** The shared row builder (the GT6RecipesSifter.buildFixedRecipe shape, + the fluid legs). */
	@Nullable
	private static Recipe buildRecipe(BeeRow aRow, @Nullable long[] aChances) {
		Item tComb = sCombResolver.apply(aRow.comb());
		if (tComb == null) return null;
		ItemStack[] tOutputs = new ItemStack[aRow.outputs().length];
		for (int i = 0; i < tOutputs.length; i++) {
			Slot tSlot = aRow.outputs()[i];
			Item tItem = tSlot.vanilla() != null
					? sVanillaItemResolver.apply(tSlot.vanilla())
					: sMaterialItemResolver.apply(tSlot.prefix(), tSlot.material());
			if (tItem == null) return null;
			tOutputs[i] = new ItemStack(tItem, tSlot.count());
		}
		FluidStack[] tFluidOutputs = new FluidStack[aRow.fluidOuts().size()];
		for (int i = 0; i < tFluidOutputs.length; i++) {
			Fluid tFluid = sFluidResolver.apply(aRow.fluidOuts().get(i).fluid());
			if (tFluid == null) return null;
			tFluidOutputs[i] = new FluidStack(tFluid, aRow.fluidOuts().get(i).amount());
		}
		// the port Recipe ctor: (duration, EUt) order — the P14 lesson; eUt 16, duration 64
		// verbatim on every row (MultiItemFood.java:251-270 / Loader_Recipes_Food.java:264).
		return new Recipe(true, new ItemStack[] {new ItemStack(tComb, 1)}, tOutputs,
				new FluidStack[0], tFluidOutputs, 64, 16, 0, aChances);
	}

	/** Test seam: clears the poured flag and the captured tables so a fresh generation can re-pour. */
	static void resetForTest() {
		sLoaded = false;
		sCentrifugeRows = null;
		sSqueezerRows = null;
	}
}
