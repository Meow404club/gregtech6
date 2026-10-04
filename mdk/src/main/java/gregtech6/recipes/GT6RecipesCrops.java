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

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import gregapi.data.ANY;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.registry.GTMaterialItems;

/**
 * The crop consumption chain — task cbc-5-crop-consumption, the Loader_Recipes_Crops.java
 * (927 lines, tmp/gt6-1.7.10/src/main/java/gregtech/loaders/c/) pour face in the
 * GT6RecipesCompressor/Bath form: upstream rows transcribed as DATA, resolved LIVE at
 * {@link #load()} against the port item/fluid universe, so the pour fills itself with no
 * code change as item families land (the Bath "pour-face-forever" rationale).
 *
 * <p><b>The upstream form is OreDict-listener dispatch</b> — every body from :46 onward
 * fires per oredict registrant. The port has no oredict listener bus (the gregapi
 * oredict/event package is not ported), so the listener form flattens to an EXPLICIT
 * registrant-face table: for each upstream listener name the port faces that upstream
 * would have dispatched are enumerated (vanilla aliases per LoaderItemList.java:760/:761
 * — {@code Bale_Wheat} = the hay block, {@code Crop_Wheat} = wheat — plus the GT6 food
 * band ids {@code gt6:food_grass*}/{@code gt6:food_crop_*} of work/food-crop-items, and
 * the prefix-item feeders of :41-44). Foreign-mod registrants (HarvestCraft crops, IC2
 * plantballs, AM2/Thaumcraft flowers — the P10 foreign mass) are NOT resolvable by
 * construction and stay declared in {@link #SKIPPED_UPSTREAM}.
 *
 * <p><b>Resolution discipline</b> (the recipe-data family method): an unresolvable INPUT
 * or fluid leg skips the row with a count (the upstream {@code mat()}→null silent drop);
 * an unresolvable ITEM OUTPUT truncates the trailing slot run and the row survives on
 * what remains (the Recipe.java:895 trailing-null form, the b2b2丢槽保行 precedent — so a
 * juice row pours before the Remains face lands and self-heals at the merge), while a row
 * left with NO output of either kind skips (upstream Recipe.add :323 needsOutputs). The
 * upstream-conditional null slots (the {@code crop()} {@code aRemains == null} variants,
 * the {@code NI} second mortarize output) are simply not part of a row's slots.
 *
 * <p><b>The RM helper expansions</b> transcribed here (RM.java line anchors in the row
 * notes): {@code RM.biomass} (:682-705, the 66-leg Fermenter walk — 2 drinks + 4 waters +
 * the MnWtr singleton repeat + 4 milks + 47 juices + 3 special juices + 3 honeys +
 * honeydew + royal jelly, every amount/duration the exact upstream integer division by
 * the stack size), {@code RM.mortarize} (:954-965 — Mortar + Shredder legs; the AE/TE/IC2
 * faces are foreign), {@code RM.compact} (:254-260 — Compressor leg only; the Boxinator
 * leg rides the {@code ST.tag(n)} config face the port Recipe slot form does not carry,
 * the b1 JSON-v1 limitation precedent), {@code RM.unpack} (:233-238 — Unboxinator leg;
 * the ic2_extractor leg is the P10 foreign face), and the {@code RM.crop*} family
 * (:758-778 — Squeezer at {@code chance-1000} + Juicer at {@code chance} with the
 * {@code 1+(aAmount/250)} juicer discount + Shredder/Mortar remains legs + the food_can
 * Canner face).
 *
 * <p><b>Declared faces (each pinned by the test, see {@link #SKIPPED_UPSTREAM})</b>: the
 * P10 foreign-mod outputs (IC2 Plantball / HBM Biomass — the GT6RecipesCompressor.java:217
 * precedent) and the IC2/AE/TE machine faces of the RM helpers; the four Thaumcraft/AM2
 * flower listeners (:48-71); the seed-oil oredict fan (:256-305 — the four VANILLA seeds
 * already ride juicer.json per the r8-hotfix-juicer fold ruling, the squeezer legs and
 * the foreign seed mass pool to the recipe-data JSON family); the baleGrass* listeners
 * (:80-124 — the bale item faces are the bale-block card's pool); the foreign crop-food
 * band (:309-870); the generic crop/flower/treeLeaves/treeSapling listeners (:876-925);
 * the :720 CR.shaped wool←fiber crafting face (not a RecipeMap row) and the :975-:990
 * fiber→string GENERIFIER rows (seated in generifier.json, the b2c-generify static replay —
 * live since the dye-item-axis items landed); the FoodsGT/Sandwiches side channels and the
 * vanilla crafting ({@code CR.shaped}) face. The :719 i=1..15 dyed-wool shred legs are
 * ENUMERATED since task recipe-b4-juicer-squeezer-flowerfruit (the MT.DATA.Dye_Materials
 * array + the 32 dust/plantGtFiber items landed, so the 15 gated legs resolve live).
 *
 * <p><b>Load timing</b>: the Compressor/Bath form — MOD-bus listener at
 * FMLCommonSetup.enqueueWork, lazily built tables (no static MT/OP capture, the a9027ac
 * lesson), generation-tracked pour flag. An unresolvable row skips with a count; the
 * pour/skip ledger stays readable ({@link #lastAttempted()}/{@link #lastPoured()}/
 * {@link #lastSkipped()}).
 *
 * <p><b>Card declarations</b>: runData N/A (runtime RecipeMap rows, not datagen JSON);
 * RCON exempt (offline data card, the recipe-data family); KJS zero-adaptation (the
 * GT6Recipes.java:44-71 generic {@code GT6Recipes.map(...)} row seam covers every
 * {@code RecipeMap.RECIPE_MAPS} key, so this surface is script-editable for free).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6RecipesCrops {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** The RM.biomass output: FL.BiomassIC2.make(n, FL.Biomass) — the ic2biomass alias is
	 * unported (the single-name ruling), so the fallback FL.Biomass = gt6:biomass carries it. */
	public static final String BIOMASS_FLUID = "gt6:biomass";

	// ------------------------------------------------------------------ seams

	/** The (prefix, material) item seam: the live default is the GTMaterialItems walk (the Mixer resolver share). */
	static BiFunction<OreDictPrefix, OreDictMaterial, Item> sMaterialItemResolver = GT6RecipesMixer::resolveItem;
	/** The registry-id item seam ("minecraft:wheat", "gt6:food_crop_rye") — the JsonLoader sItemResolver face. */
	static Function<String, Item> sIdItemResolver = GT6RecipesCrops::resolveItemById;
	/** The registry-id fluid seam ("minecraft:water", "gt6:biomass") — the JsonLoader sFluidResolver face. */
	static Function<String, Fluid> sFluidResolver = GT6RecipesCrops::resolveFluidById;

	@Nullable
	static Item resolveItemById(String aId) {
		Item tItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(aId));
		return tItem == null || tItem == Items.AIR ? null : tItem;
	}

	@Nullable
	static Fluid resolveFluidById(String aId) {
		if ("minecraft:water".equals(aId)) return Fluids.WATER; // the source-still constant, registry-free
		Fluid tFluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(aId));
		return tFluid == null || tFluid == Fluids.EMPTY ? null : tFluid;
	}

	// ------------------------------------------------------------------ row shape

	/** One slot: either a registry id or a (prefix, material) pair (the Compressor Slot form, id-based). */
	public record Slot(@Nullable String id, @Nullable OreDictPrefix prefix, @Nullable OreDictMaterial material, int count) {
		public static Slot id(String aId, int aCount) { return new Slot(aId, null, null, aCount); }
		public static Slot mat(OreDictPrefix aPrefix, OreDictMaterial aMaterial, int aCount) { return new Slot(null, aPrefix, aMaterial, aCount); }
	}

	/** One fluid leg: the registry id plus the mB amount. */
	public record FluidLeg(String id, long amount) {}

	/**
	 * One transcribed upstream addRecipe call. {@code note} carries the upstream file:line;
	 * {@code buffered} is the upstream first addRecipe argument verbatim (F on the
	 * RM.biomass Fermenter rows, T elsewhere); {@code chances} align to the ITEM outputs
	 * (upstream Recipe.java:956 — chances index item outputs only).
	 */
	public record Row(String note, String map, boolean buffered, long eUt, long duration, @Nullable long[] chances,
			Slot[] inputs, @Nullable FluidLeg fluidIn, Slot[] outputs, @Nullable FluidLeg fluidOut) {
		/** The deterministic compact form (null chances) — most rows are chance-free (the Compressor FixedRow form). */
		public Row(String note, String map, boolean buffered, long eUt, long duration,
				Slot[] inputs, @Nullable FluidLeg fluidIn, Slot[] outputs, @Nullable FluidLeg fluidOut) {
			this(note, map, buffered, eUt, duration, null, inputs, fluidIn, outputs, fluidOut);
		}
	}

	// ------------------------------------------------------------------ the RM.biomass walk (RM.java:682-705)

	/**
	 * One biomass leg: the input fluid id and the NUMERATORS of the upstream divisions —
	 * amounts are {@code units / tSize}, duration is {@code (speed * durationUnits) / tSize}
	 * (the exact upstream long-division truncation, tSize = the feeder stack size).
	 */
	public record BiomassLeg(String note, String inFluidId, long inUnits, long outUnits, long durationUnits) {}

	/** The FluidsGT.JUICE walk (RM.java:695), upstream FL.java:187-234 ids mapped to the port
	 * fluid universe (the b2b1 id table: the binnie./grc. dots flatten to underscores, the
	 * bare "juice" carrier is juice_juice). All 47 members are port-registered. */
	public static final List<String> JUICE_WALK = List.of(
			"juice_juice"      , // FL.java:187 "juice"
			"kiwijuice"        , // :188
			"juicelime"        , // :189 "binnie.juicelime"
			"juicelemon"       , // :190
			"juiceorange"      , // :191
			"persimmonjuice"   , // :192
			"melonjuice"       , // :193
			"currantjuice"     , // :194
			"raspberryjuice"   , // :195
			"blackberryjuice"  , // :196
			"blueberryjuice"   , // :197
			"gooseberryjuice"  , // :198
			"strawberryjuice"  , // :199
			"juiceplum"        , // :200
			"juicepeach"       , // :201
			"juiceelderberry"  , // :202
			"hellderberryjuice", // :203
			"juicegrapefruit"  , // :204
			"juiceapricot"     , // :205
			"juicepear"        , // :206
			"grapejuice"       , // :207
			"grc_grapewine0"   , // :208 "grc.grapewine0"
			"juiceredgrape"    , // :209
			"juicewhitegrape"  , // :210
			"juiceapple"       , // :211
			"grc_applecider0"  , // :212 "grc.applecider0"
			"juicepineapple"   , // :213
			"juicebanana"      , // :214
			"juicecherry"      , // :215
			"juicecranberry"   , // :216
			"cactusfruitjuice" , // :217
			"mangojuice"       , // :218
			"pomegranatejuice" , // :219
			"starfruitjuice"   , // :220
			"papayajuice"      , // :221
			"figjuice"         , // :222
			"coconutmilk"      , // :223
			"datejuice"        , // :224
			"juicecarrot"      , // :226 "binnie.juicecarrot"
			"juicetomato"      , // :227
			"beetjuice"        , // :228
			"pumpkinjuice"     , // :229
			"cucumberjuice"    , // :230
			"onionjuice"       , // :231
			"potatojuice"      , // :232
			"reedwater"        , // :233
			"cactuswater"      );// :234

	/** The HONEY walk (RM.java:700-701): upstream FluidsGT.HONEY = {for.honey, grc.honey, honey} (FL.java:139-141). */
	public static final List<String> HONEY_WALK = List.of("for_honey", "grc_honey", "honey");

	/** The MILK walk (RM.java:693-694): upstream FL.java:134-137. */
	public static final List<String> MILK_WALK = List.of("milk", "soymilk", "grcmilk_milk", "spoiledmilk");

	/** The WATER walk (RM.java:690-691): FL.waters = Water/MnWtr/DistW/SpDew (FL.java:689, the Mixer WATER_COUNT precedent). */
	public static final List<String> WATER_WALK = List.of("minecraft:water", "gt6:mnwtr", "gt6:distilled_water", "gt6:spdew");

	/**
	 * The 66 biomass legs in upstream order (RM.java:688-703): rottendrink + mushroomsoup,
	 * the 4 waters, the MnWtr singleton repeat (upstream :692 re-adds the walk member —
	 * verbatim faithful), the 4 milks, the 47 juices, the 3 special juices (:697-699 — the
	 * exclusion trio of :695 is the foreign compat fan, port-absent by construction), the
	 * 3 honeys, honeydew and royal jelly.
	 */
	public static List<BiomassLeg> biomassLegs() {
		List<BiomassLeg> rLegs = new ArrayList<>(66);
		rLegs.add(new BiomassLeg("RM.java:688", "gt6:rottendrink"      , 1080, 3240  , 2)); // FL.Rotten_Drink
		rLegs.add(new BiomassLeg("RM.java:689", "gt6:soup_mushroom"    , 1080, 3240  , 2)); // FL.Soup_Mushroom
		for (String tWater : WATER_WALK) rLegs.add(new BiomassLeg("RM.java:690", tWater, 1080, 1080, 4));
		rLegs.add(new BiomassLeg("RM.java:692", "gt6:mnwtr"            , 1080, 1080  , 4)); // FL.MnWtr — the walk-member repeat, upstream verbatim
		for (String tMilk : MILK_WALK) rLegs.add(new BiomassLeg("RM.java:693", "gt6:" + tMilk, 1080, 2160, 3));
		for (String tJuice : JUICE_WALK) rLegs.add(new BiomassLeg("RM.java:695", "gt6:" + tJuice, 1080, 3240, 3));
		rLegs.add(new BiomassLeg("RM.java:697", "gt6:potion.idunsapplejuice"  , 1080, 210600, 1));
		rLegs.add(new BiomassLeg("RM.java:698", "gt6:potion.goldenapplejuice" , 1080, 29160 , 1));
		rLegs.add(new BiomassLeg("RM.java:699", "gt6:goldencarrotjuice"       , 1080, 6480  , 1));
		for (String tHoney : HONEY_WALK) rLegs.add(new BiomassLeg("RM.java:700", "gt6:" + tHoney, 1080, 3240, 3));
		rLegs.add(new BiomassLeg("RM.java:702", "gt6:honeydew"         , 1080, 3240  , 2)); // FL.Honeydew (ALCOHOLIC, outside the HONEY walk)
		rLegs.add(new BiomassLeg("RM.java:703", "gt6:royal_jelly"      , 1080, 12560 , 2)); // FL.RoyalJelly
		return rLegs;
	}

	/** The biomass leg count: 2 + 4 + 1 + 4 + 47 + 3 + 3 + 2. */
	public static final int BIOMASS_LEG_COUNT = 66;

	/** One RM.biomass call site → its 66 Fermenter rows (the row note carries feeder + leg anchors). */
	public static List<Row> biomassRows(String aFeederNote, Slot aFeeder, long aSpeed) {
		long tSize = aFeeder.count();
		List<Row> rRows = new ArrayList<>(BIOMASS_LEG_COUNT);
		for (BiomassLeg tLeg : biomassLegs()) {
			rRows.add(new Row(aFeederNote + " " + tLeg.note(), "fermenter", false, 16,
					(aSpeed * tLeg.durationUnits()) / tSize, null,
					new Slot[] {aFeeder}, new FluidLeg(tLeg.inFluidId(), tLeg.inUnits() / tSize),
					new Slot[0], new FluidLeg(BIOMASS_FLUID, tLeg.outUnits() / tSize)));
		}
		return rRows;
	}

	// ------------------------------------------------------------------ section tables

	private static volatile List<Row> sRows = null;

	/** The whole enumerated table, captured on first use (lazy — no static OP/MT capture). */
	public static List<Row> table() {
		List<Row> tTable = sRows;
		if (tTable == null) sRows = tTable = buildTable();
		return tTable;
	}

	private static List<Row> buildTable() {
		List<Row> rRows = new ArrayList<>(1800);
		// :41-44 — the FlourGrains→biomass walk (dust x9 at speed 64; the blockDust arm is
		// enumerated too and skips on the non-item-path prefix, the Compressor zero-pour arm form)
		for (OreDictMaterial tMat : ANY.FlourGrains.mToThis) {
			rRows.addAll(biomassRows(":41 dust x9 " + tMat.mNameInternal, Slot.mat(OP.dust, tMat, 9), 64));
			rRows.addAll(biomassRows(":43 blockDust x1 " + tMat.mNameInternal, Slot.mat(OP.blockDust, tMat, 1), 64));
		}
		// :719 i==0 — the WHITE wool shred (Vanilla seam; chance 9000 -> 4 string, the special case)
		rRows.add(new Row("Vanilla:719 wool white", "shredder", true, 16, 16, new long[] {9000},
				new Slot[] {Slot.id("minecraft:white_wool", 1)}, null,
				new Slot[] {Slot.id("minecraft:string", 4)}, null));
		// :719 i=1..15 — the DYED wool shred legs (task recipe-b4-juicer-squeezer-flowerfruit):
		// plantGtFiber x4 of MT.DATA.Dye_Materials[15-i] at chance 9000, one per dyed wool color.
		// The wool colors walk the vanilla meta order (1=orange..15=black) while Dye_Materials
		// runs the vanilla dye order (0=Black..15=White), hence the [15-i] index — the same
		// expression the upstream loop uses. The array and the dye items are the dye-item-axis
		// landing; buildTable() is lazy (post-MT-init), so the array read is safe here.
		String[] tWool = {"orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray",
				"cyan", "purple", "blue", "brown", "green", "red", "black"};
		for (int i = 1; i < 16; i++) {
			rRows.add(new Row(":719 dyed wool i=" + i + " (" + tWool[i - 1] + "_wool)", "shredder", true, 16, 16,
					new long[] {9000},
					new Slot[] {Slot.id("minecraft:" + tWool[i - 1] + "_wool", 1)}, null,
					new Slot[] {Slot.mat(OP.plantGtFiber, MT.DATA.Dye_Materials[15 - i], 4)}, null));
		}
		// :125-141 — the itemGrass fodder family, live-wired to the work/food-crop-items ids
		// (the compact legs of all four listeners ride the bale-item pool — declared)
		rRows.addAll(biomassRows(":125 itemGrassRotten", Slot.id("gt6:food_grass_rotten", 9), 16));
		rRows.addAll(biomassRows(":129 itemGrassMoldy"  , Slot.id("gt6:food_grass_moldy"  , 9), 16));
		rRows.addAll(biomassRows(":133 itemGrassDry"    , Slot.id("gt6:food_grass_dry"    , 9), 64));
		rRows.add(new Row(":138 itemGrass Drying", "drying", true, 16, 40,
				new Slot[] {Slot.id("gt6:food_grass", 1)}, null,
				new Slot[] {Slot.id("gt6:food_grass_dry", 1)}, new FluidLeg("gt6:distilled_water", 20)));
		rRows.addAll(biomassRows(":139 itemGrass", Slot.id("gt6:food_grass", 9), 64));
		// :156-236 — the four GT6 grain crops (work/food-crop-items ids); the compact legs ride
		// the bale-item pool (declared), the Mash_Grain double member (oats/barley) is upstream-verbatim
		rRows.addAll(grainCropRows(":158 cropRice" , "gt6:food_crop_rice"  , "gt6:ricewater", MT.Rice));
		rRows.addAll(grainCropRows(":174 cropOats" , "gt6:food_crop_oats"  , "gt6:mashgrain", MT.Oat));
		rRows.addAll(grainCropRows(":208 cropBarley", "gt6:food_crop_barley", "gt6:mashgrain", MT.Barley));
		rRows.addAll(grainCropRows(":224 cropRye"  , "gt6:food_crop_rye"   , "gt6:mashrye"  , MT.Rye));
		// :238-252 — cropWheat (the vanilla alias face) + compact → the hay block
		// (LoaderItemList.java:760/:761) + :247-251 the baleWheat listener over the same block
		rRows.add(new Row(":240 cropWheat Drying", "drying", true, 16, 40,
				new Slot[] {Slot.id("minecraft:wheat", 1)}, null,
				new Slot[] {Slot.id("gt6:food_grass_dry", 1)}, new FluidLeg("gt6:distilled_water", 20)));
		for (String tWater : WATER_WALK) {
			rRows.add(new Row(":242 cropWheat Mash", "mixer", true, 16, 16,
					new Slot[] {Slot.id("minecraft:wheat", 1)}, new FluidLeg(tWater, 250),
					new Slot[0], new FluidLeg("gt6:mashwheat", 250)));
		}
		rRows.add(new Row(":243 cropWheat mortarize Mortar"  , "mortar"  , true, 16, 64,
				new Slot[] {Slot.id("minecraft:wheat", 1)}, null,
				new Slot[] {Slot.mat(OP.dust, MT.Wheat, 1), Slot.id("gt6:food_grass", 1)}, null));
		rRows.add(new Row(":243 cropWheat mortarize Shredder", "shredder", true, 16, 64,
				new Slot[] {Slot.id("minecraft:wheat", 1)}, null,
				new Slot[] {Slot.mat(OP.dust, MT.Wheat, 1), Slot.id("gt6:food_grass", 1)}, null));
		rRows.addAll(biomassRows(":244 cropWheat biomass", Slot.id("minecraft:wheat", 9), 64));
		rRows.add(new Row(":245 cropWheat compact Compressor", "compressor", true, 16, 16,
				new Slot[] {Slot.id("minecraft:wheat", 9)}, null,
				new Slot[] {Slot.id("minecraft:hay_block", 1)}, null));
		rRows.add(new Row(":248 baleWheat Shredder", "shredder", true, 16, 144,
				new Slot[] {Slot.id("minecraft:hay_block", 1)}, null,
				new Slot[] {Slot.mat(OP.dust, MT.Wheat, 9), Slot.id("gt6:food_grass", 9)}, null));
		// :249 the baleWheat Drying leg rides the IL.Bale_Dry face — the bale pool (declared)
		rRows.addAll(biomassRows(":250 baleWheat biomass", Slot.id("minecraft:hay_block", 1), 64));
		rRows.add(new Row(":251 baleWheat unpack", "unboxinator", true, 16, 16,
				new Slot[] {Slot.id("minecraft:hay_block", 1)}, null,
				new Slot[] {Slot.id("minecraft:wheat", 9)}, null));
		// :469-501 + :791-812 — the vanilla-anchored fruit/veg faces (the RM.crop* family:
		// Squeezer + Juicer rows pour on the juice leg; the Remains_* faces ride their pool
		// and only the potato's dustSmall remains resolves today; the food_can CANS_* legs,
		// the Slicer/Pill/Bath-gold/stick-walk faces are declared)
		rRows.addAll(cropRows(":495 cropApple"  , "minecraft:apple"       , "gt6:juiceapple"  , 100, 7000, null));
		rRows.addAll(cropRows(":500 cropMelon"  , "minecraft:melon_slice" , "gt6:melonjuice"  , 250, 6000, null));
		rRows.addAll(cropRows(":793 cropBeet"   , "minecraft:beetroot"    , "gt6:beetjuice"   , 200, 7000, null));
		rRows.addAll(cropRows(":796 cropCarrot" , "minecraft:carrot"      , "gt6:juicecarrot" , 100, 7000, null));
		rRows.addAll(cropRows(":806 cropPotato" , "minecraft:potato"      , "gt6:potatojuice" , 100, 8000, Slot.mat(OP.dustSmall, MT.Potato, 4)));
		return rRows;
	}

	/** One grain-crop listener body (:156-236 shape): Drying + 4 waters mash + mortarize
	 * (Mortar + Shredder) + biomass + the compact Compressor leg — the compact output is
	 * the crop's GT6 bale item (the bale pool), so NO compact row is enumerated here. */
	private static List<Row> grainCropRows(String aNote, String aCropId, String aMashId, OreDictMaterial aMaterial) {
		List<Row> rRows = new ArrayList<>(73);
		rRows.add(new Row(aNote + " Drying", "drying", true, 16, 40,
				new Slot[] {Slot.id(aCropId, 1)}, null,
				new Slot[] {Slot.id("gt6:food_grass_dry", 1)}, new FluidLeg("gt6:distilled_water", 20)));
		for (String tWater : WATER_WALK) {
			rRows.add(new Row(aNote + " Mash", "mixer", true, 16, 16,
					new Slot[] {Slot.id(aCropId, 1)}, new FluidLeg(tWater, 250),
					new Slot[0], new FluidLeg(aMashId, 250)));
		}
		rRows.add(new Row(aNote + " mortarize Mortar", "mortar", true, 16, 64,
				new Slot[] {Slot.id(aCropId, 1)}, null,
				new Slot[] {Slot.mat(OP.dust, aMaterial, 1), Slot.id("gt6:food_grass", 1)}, null));
		rRows.add(new Row(aNote + " mortarize Shredder", "shredder", true, 16, 64,
				new Slot[] {Slot.id(aCropId, 1)}, null,
				new Slot[] {Slot.mat(OP.dust, aMaterial, 1), Slot.id("gt6:food_grass", 1)}, null));
		rRows.addAll(biomassRows(aNote + " biomass", Slot.id(aCropId, 9), 64));
		return rRows;
	}

	/** The RM.crop_fruit/crop_veggie/crop expansion (:758-778): Squeezer at chance-1000,
	 * Juicer at chance with the {@code 1+(aAmount/250)} discount, Shredder/Mortar only when
	 * a remains face exists (the food_can Canner legs are the CANS_* pool — declared). */
	private static List<Row> cropRows(String aNote, String aCropId, String aJuiceId, long aAmount, long aChance, @Nullable Slot aRemains) {
		List<Row> rRows = new ArrayList<>(4);
		Slot[] tOuts = aRemains == null ? new Slot[0] : new Slot[] {aRemains};
		rRows.add(new Row(aNote + " Squeezer", "squeezer", true, 16, 16, new long[] {aChance - 1000},
				new Slot[] {Slot.id(aCropId, 1)}, null, tOuts, new FluidLeg(aJuiceId, aAmount)));
		rRows.add(new Row(aNote + " Juicer", "juicer", true, 16, 16, new long[] {aChance},
				new Slot[] {Slot.id(aCropId, 1)}, null, tOuts,
				new FluidLeg(aJuiceId, aAmount - (aAmount < 100 ? aAmount / 3 : 1 + (aAmount / 250)) * 25)));
		if (aRemains != null) {
			rRows.add(new Row(aNote + " Shredder", "shredder", true, 16, 16, new long[] {aChance},
					new Slot[] {Slot.id(aCropId, 1)}, null, new Slot[] {aRemains}, null));
			rRows.add(new Row(aNote + " Mortar", "mortar", true, 16, 16, new long[] {aChance / 2},
					new Slot[] {Slot.id(aCropId, 1)}, null, new Slot[] {aRemains}, null));
		}
		return rRows;
	}

	// ------------------------------------------------------------------ pour

	/** Poured flag — one generation, one pour (upstream loaders run once per JVM). */
	private static boolean sLoaded = false;
	/** The reconciliation counters of the last {@link #load()} (the offline audit face). */
	private static volatile int sAttempted = 0, sPoured = 0, sSkipped = 0;

	// ADR-P18: the pour-flag joins the GT6RecipeMaps generation (the Compressor/Bath form).
	static {GT6RecipeMaps.registerGenerationResetHook(GT6RecipesCrops::resetForTest);}

	/** FMLCommonSetup.enqueueWork — items and fluids are registered by this point. */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(GT6RecipesCrops::load);
	}

	/** The map-key lookup for the maps this card pours (the JsonLoader mapFor face, local copy). */
	@Nullable
	static RecipeMap mapFor(String aKey) {
		return switch (aKey) {
			case "fermenter" -> GT6RecipeMaps.FERMENTER;
			case "shredder" -> GT6RecipeMaps.SHREDDER;
			case "mixer" -> GT6RecipeMaps.MIXER;
			case "mortar" -> GT6RecipeMaps.MORTAR;
			case "compressor" -> GT6RecipeMaps.COMPRESSOR;
			case "unboxinator" -> GT6RecipeMaps.UNBOXINATOR;
			case "drying" -> GT6RecipeMaps.DRYING;
			case "squeezer" -> GT6RecipeMaps.SQUEEZER;
			case "juicer" -> GT6RecipeMaps.JUICER;
			default -> null;
		};
	}

	/** Pours the table. Idempotent; an unresolvable row skips with a count (the class-doc resolution discipline). */
	public static synchronized void load() {
		if (sLoaded) {LOGGER.debug("load() skipped: already poured (generation flag set)"); return;}
		GT6RecipeMaps.init(); // defensive + idempotent
		int tAttempted = 0, tPoured = 0, tSkipped = 0;
		for (Row tRow : table()) {
			RecipeMap tMap = mapFor(tRow.map());
			if (tMap == null) continue; // a broken lifecycle — nothing to pour into
			tAttempted++;
			Recipe tRecipe = build(tRow);
			if (tRecipe == null) {tSkipped++; continue;}
			tMap.addRecipe(tRecipe);
			tPoured++;
		}
		sAttempted = tAttempted; sPoured = tPoured; sSkipped = tSkipped;
			LOGGER.info("GT6 crop consumption rows poured: {} / {} attempted, {} skipped (unresolvable faces = the declared pools; "
					+ "the seed fan rides juicer.json, the bale/Remains/CANS/Shape_Slicer faces their own pools, the dye/plantGtFiber "
					+ "faces landed with the dye-item-axis + recipe-b4 cards, the P10 foreign mass untouched)",
					tPoured, tAttempted, tSkipped);
		sLoaded = true;
	}

	/**
	 * Row → Recipe, or null per the class-doc resolution discipline: inputs strict (the
	 * upstream null-input drop), item outputs resolved in order with a null allowed only as
	 * a trailing run (the Recipe.withoutTrailingNulls + b2b2 Recipe.java:895 truncation form),
	 * the fluid legs strict, and at least one output of either kind (upstream Recipe.add
	 * :319/:323 needsOutputs/minimal gates).
	 */
	@Nullable
	static Recipe build(Row aRow) {
		ItemStack[] tInputs = new ItemStack[aRow.inputs().length];
		for (int i = 0; i < tInputs.length; i++) {
			Item tItem = resolveSlot(aRow.inputs()[i]);
			if (tItem == null) return null; // the upstream null-input drop
			tInputs[i] = new ItemStack(tItem, aRow.inputs()[i].count());
		}
		ItemStack[] tOutputs = new ItemStack[aRow.outputs().length];
		for (int i = 0; i < tOutputs.length; i++) {
			Item tItem = resolveSlot(aRow.outputs()[i]);
			tOutputs[i] = tItem == null ? null : new ItemStack(tItem, aRow.outputs()[i].count());
		}
		// interior nulls fail the row; the trailing null run truncates
		int tLast = tOutputs.length;
		while (tLast > 0 && tOutputs[tLast - 1] == null) tLast--;
		for (int i = 0; i < tLast; i++) if (tOutputs[i] == null) return null;
		ItemStack[] rOutputs = java.util.Arrays.copyOf(tOutputs, tLast);
		FluidStack tFluidIn = null, tFluidOut = null;
		if (aRow.fluidIn() != null) {
			Fluid tFluid = sFluidResolver.apply(aRow.fluidIn().id());
			if (tFluid == null) return null;
			tFluidIn = new FluidStack(tFluid, (int)aRow.fluidIn().amount());
		}
		if (aRow.fluidOut() != null) {
			Fluid tFluid = sFluidResolver.apply(aRow.fluidOut().id());
			if (tFluid == null) return null;
			tFluidOut = new FluidStack(tFluid, (int)aRow.fluidOut().amount());
		}
		if (tInputs.length + (tFluidIn == null ? 0 : 1) <= 0) return null; // upstream :319
		if (rOutputs.length + (tFluidOut == null ? 0 : 1) <= 0) return null; // upstream :323 needsOutputs
		return new Recipe(aRow.buffered(), tInputs, rOutputs,
				tFluidIn == null ? new FluidStack[0] : new FluidStack[] {tFluidIn},
				tFluidOut == null ? new FluidStack[0] : new FluidStack[] {tFluidOut},
				aRow.duration(), aRow.eUt(), 0, aRow.chances());
	}

	/** Slot → Item, or null when the face is absent (the strict input / lenient output caller decides). */
	@Nullable
	private static Item resolveSlot(Slot aSlot) {
		return aSlot.id() != null
				? sIdItemResolver.apply(aSlot.id())
				: sMaterialItemResolver.apply(aSlot.prefix(), aSlot.material());
	}

	/** The last pour's attempted count (the offline reconciliation face). */
	public static int lastAttempted() {return sAttempted;}
	/** The last pour's poured count. */
	public static int lastPoured() {return sPoured;}
	/** The last pour's skipped count. */
	public static int lastSkipped() {return sSkipped;}

	/** Test seam: clears the poured flag and the counters so a fresh generation can re-pour. */
	static void resetForTest() {sLoaded = false; sAttempted = 0; sPoured = 0; sSkipped = 0;}

	private GT6RecipesCrops() {}

	/**
	 * The skipped upstream surface, kept as DATA for the audit walk (the Compressor
	 * SKIPPED_UPSTREAM form). Every entry is upstream line-anchored.
	 */
	public static final List<String> SKIPPED_UPSTREAM = List.of(
			"P10 foreign-mod outputs — every RM.ic2_compressor/RM.ic2_extractor/RM.Compressor-into-IC2_Plantball/"
			+ "HBM_Biomass face of the bale/itemPlantRemains/final-four listeners (Crops :81-87/:92-98/:102-109/:113-120/"
			+ ":142-149/:876-925) and the ic2 legs inside RM.compact/RM.unpack (RM.java:251/:236): foreign-mod outputs, the "
			+ "GT6RecipesCompressor.java:217 ruling (18 Compressor sites already declared there)",
			":48-71 the four flower listeners (flowerCerublossom AM2, flowerDesertNova/flowerCinderpearl/flowerShimmerleaf "
			+ "Thaumcraft) — foreign flower items; DYE_FLUIDS_FLOWER and IL.Remains_Plant have no port faces; the ic2_extractor "
			+ "legs are the P10 face (TRUE NEGATIVE)",
			":74-77 the OD.bamboo listener (biomass + Coke Oven → IL.GrC_Bamboo_Charcoal) — the registrant family is foreign "
			+ "(Natura/BoP/ExB bamboo; the 1.20.1 vanilla bamboo is a different item with no GT6 oredict membership upstream); "
			+ "GrC_Bamboo_Charcoal has no port face (TRUE NEGATIVE)",
			":80-124 the four baleGrass* listeners — the bale item faces (IL.Bale/Bale_Dry/Bale_Moldy/Bale_Rotten and the "
			+ "grass-bale block-items) are the bale-block card's pool (GT6CropFoods javadoc: the 8 GT6 bale items are "
			+ "block-item forms, pooled with BlockBaleGrass/BlockBaleCrop); the RM.biomass/unpack legs re-enter automatically "
			+ "with that card's faces",
			":156-236 the baleRice/baleOats/baleAbyssalOats/baleBarley/baleRye/baleWheat-native-item legs (and the :249 "
			+ "baleWheat Drying leg — its IL.Bale_Dry output) — same bale pool; "
			+ "the enumerated :247-251 rows cover only the LoaderItemList Bale_Wheat = hay_block alias",
			":188-204 cropAbyssalOats/baleAbyssalOats — the Netherlicious foreign item family (GT6CropFoods: the "
			+ "IL.Bale_AbyssalOats/Crop_AbyssalOats exists() arms are never GT6-registered — foreign-identity TRUE NEGATIVES)",
			":256-305 the seed-oil oredict fan — the foreign seed mass (HarvestCraft/IC2) is TRUE NEGATIVE; the four VANILLA "
			+ "seeds already ride juicer.json per the r8-hotfix-juicer fold ruling (:264/:269/:274/:279, one Juicer row each); "
			+ "the four squeezer legs and the FL.lube seedCotton/Hemp/Flax/Canola foreign-crop legs pool to the recipe-data "
			+ "JSON family (this card does not touch recipe_maps JSON)",
			":309-320 cropHemp/cropFlax (Rope/Loom tag(10) faces) and :321-329 cropCanola — HempIndustrie/GrowthCraft/foreign "
			+ "registrants; Oil_Hemp/Oil_Canola fluids have no port faces (TRUE NEGATIVE)",
			":330-339 cropIvy — HBM_Poison_Powder/IC2_Grin_Powder outputs and the Potion_Poison_1/Awkward potion fluid legs "
			+ "over a foreign crop (TRUE NEGATIVE)",
			":340-347 cropCandle/cropCandleberry — foreign crops; MT.WaxPlant dustSmall and the ae_grinder face (P10 foreign) "
			+ "(TRUE NEGATIVE)",
			":350-870 the foreign crop-food band (olive/sunflower/star-anise/cocoa/coffee/tea/hops/tobacco/coca/cannabis/the "
			+ "nut family/the corn/apple-cultivar/melon-cultivar/berry/cherry/cactus/citrus/grape/banana/pineapple/veg/spice "
			+ "listeners) — every registrant is foreign; the food_can/CANS_FRUIT/CANS_VEGGIE/CANS_UNDEFINED faces, the "
			+ "Shape_Slicer_*/Pill_*/Food_*_Sliced/IL.Rope faces and the FoodsGT.put/Sandwiches.INGREDIENTS side channels are "
			+ "their own pools (the T5b band + the food-cans pool; the enumerated vanilla-anchored rows cover the "
			+ "LoaderItemList-aliased faces)",
			":443/:445/:449/:587/:636/:690/:733/:773 the FR crate boxunbox legs (MD.FR crate/cratedX) — Forestry foreign "
			+ "items (TRUE NEGATIVE)",
			":493 the golden-apple Bath leg — apple + MT.Au.liquid(U*8): the molten-gold fluid is not registered (only "
			+ "gt6:iron_molten carries the molten face) — unlocks with the molten-metal registry card",
			":720 the CR.shaped wool←fiber legs — the vanilla CRAFTING face, not a RecipeMap row (declared); "
			+ ":975-990 the 16 plantGtFiber→string GENERIFIER rows over the DYE materials ride generifier.json "
			+ "(the b2c-generify static replay, live since the dye-item-axis items landed); the :719 i=1..15 "
			+ "dyed-wool shred legs are ENUMERATED since task recipe-b4-juicer-squeezer-flowerfruit — the "
			+ "MT.DATA.Dye_Materials array and the 32 dust/plantGtFiber items landed, so the old 'no nameable "
			+ "material' block is resolved (the unlock declared here, the Dye_Materials/PLANTS ground on record)",
			":742 the HaC grape→raisins Drying row (MD.HaC grapeItem) — HarvestCraft foreign (TRUE NEGATIVE)",
			":377-378 cropHops, :453-466 cropCorn/Crop_Devilish_Maize Mash_Corn faces, :826-827 the VINEGAR pickle Bath walk, "
			+ ":848 the Sandwiches.INGREDIENTS seat — foreign-crop or side-channel faces riding the T5b/potion pools",
			":876-925 the four generic listeners (crop/flower/treeLeaves/treeSapling) — the Plantball outputs are the P10 "
			+ "face; the Shredder/Mortar → Remains_Plant legs ride the Remains pool; the port has no oredict dispatch bus to "
			+ "fan these out per registrant (the flattened vanilla faces are enumerated where an alias exists)",
			"RM.java:961-963 the ae_grinder/te_pulverizer/ic2_macerator legs inside RM.mortarize, RM.java:967-969 "
			+ "ae_grinder, RM.java:805-821 EtFu smoker/blast faces — Applied Energistics/Thermal Expansion/IC2/EtFuturum "
			+ "foreign machine faces (P10)",
			"the mAspects/TC, pet-feeding, smelt-time and replicateOrganic side faces of the band — the GT6CropFoods POOLED "
			+ "declaration carries them");
}
