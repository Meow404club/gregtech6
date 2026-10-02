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

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import gregtech6.registry.GT6Foods;
import gregtech6.registry.GTMaterialItems;

/**
 * The food-domain T1b recipe-row backfill (task food-recipes-t1b) — the 29-row band the
 * nine {@link GT6Foods} items unlock, transcribed verbatim from the upstream loaders
 * (every number/duration/EUt/fluid-amount is the upstream literal; the {@code note} of
 * each row carries the upstream file:line):
 *
 * <ul>
 *   <li><b>Coagulator, the 2 cheese rows</b> — Loader_Recipes_Food.java:694-695
 *       ({@code Milk}/{@code MilkGrC} 1000 → {@code Food_Cheese} at 1024t). The :696
 *       {@code MilkSoy→Tofu} row is already live — not re-added.</li>
 *   <li><b>Cryo Mixer, the 8 ice-cream-base rows</b> — MultiItemFood.java:847-850, the
 *       {@code FL.waters(250)} walk (FL.java:689 = Water/MnWtr/DistW/SpDew, the
 *       GT6RecipesMixer.resolveWater carriers) × the two row shapes: salt dustSmall +
 *       water 250 + cream 250 → 1 ice cream (16t), salt dust + water 1000 + cream 1000 →
 *       4 ice cream (64t).</li>
 *   <li><b>Centrifuge + Mixer, the 5 dairy butter rows</b> — Loader_Recipes_Food.java
 *       :660/:661/:662 (milk family 50 → cream 50), :664 (cream 250 → butter, 64t) and
 *       :670 (the mixer twin, treated-wood stick catalyst + cream 250 → butter).</li>
 *   <li><b>Mixer, the 3 salted-butter rows</b> — :729-731 (NaCl dust/dustSmall/dustTiny +
 *       butter 9/2/1 → salted butter 9/2/1 at 144t/32t/16t).</li>
 *   <li><b>Mixer, the 3 chili-chips rows</b> — :771 (the chili dust/dustSmall/dustTiny
 *       forms 1/4/9 + potato chips → chili chips).</li>
 *   <li><b>Bath, the 8 boiled-egg specs</b> — :303-318 Bath arm (:306-:312): the egg +
 *       100 mB hot fluid → 100 mB water + boiled egg + the shell by-product at the
 *       {@code IL.Dye_Bonemeal} fallback position (the Birb shell is port-absent; the
 *       fallback's port identity is {@link Items#BONE_MEAL}, the GT6RecipesShCL:206
 *       white-dye ruling). Two egg identities × four hot-fluid legs: the brown rows key
 *       on the vanilla egg ({@code ST.equal(Items.egg)} → {@code Food_Brown_Egg_Boiled}),
 *       the white rows key on the raw GT6 egg (port-absent — the rows are wired LIVE on
 *       {@link #sWhiteEggResolver}, zero pours until that item lands, the
 *       GT6RecipesBath wood-ladder pour-face-forever form).</li>
 * </ul>
 *
 * <p><b>TRUE NEGATIVE pins (the B2a coagulator verdict, P10 foreign-identity ruling)</b>:
 * the :673-:686 coagulator band ({@code dropHoney}/{@code dropHoneydew} — Forestry ore-dict
 * {@code getFirstOre} outputs, OD.java:159-160 zero members in a GT6-only env; :680-:681
 * {@code RoyalJelly} — the FR/HaC items; :683-:686 {@code Glue}/{@code Blood} — the TiC
 * items) registers NOTHING upstream in a GT6-only environment and gets NO GT6 substitute
 * here — {@link #SKIPPED_UPSTREAM} carries the declaration and the test pins the absence.
 *
 * <p><b>Declared out-of-band</b> (each with its reason, see {@link #SKIPPED_UPSTREAM}):
 * the :663 coconut-cream pair and the :666-:669 mixer milk→cream catalyst legs (the
 * cream-fluid chain rows, the T2 搅拌机流体行 domain), the :313 Autoclave / :314 Juicer /
 * :315 Centrifuge / :317 smelting egg legs (outside the Bath arm, the T4 蛋肉罐头 domain),
 * the :305 {@code itemEggBig} tAmount=4 face (no big-egg item in the port — tAmount is 1).
 *
 * <p><b>Seams</b> (the GT6RecipesMixer/GT6RecipesDrying shape): the material-item, food-item,
 * white-egg and named-fluid resolvers are live by default, fixtures injected offline. The
 * table is LAZILY built — no static initializer captures MT/OP/registry state (the a9027ac
 * lesson). {@code load()} is idempotent per JVM generation; an unresolvable row skips with
 * a count (the upstream {@code mat()} null-drop semantics), and the per-map counts stay
 * readable for the offline reconciliation ({@link #lastPoured}/{@link #lastSkipped}).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6RecipesFood {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** The band sizes (the ratchet face): 2 + 8 + 4 + 7 + 8 = {@link #TOTAL_ROWS}. */
	public static final int COAGULATOR_ROWS = 2;
	public static final int CRYO_MIXER_ROWS = 8;
	public static final int CENTRIFUGE_ROWS = 4;
	public static final int MIXER_ROWS = 7;
	public static final int BATH_ROWS = 8;
	public static final int TOTAL_ROWS = COAGULATOR_ROWS + CRYO_MIXER_ROWS + CENTRIFUGE_ROWS + MIXER_ROWS + BATH_ROWS;

	/** The {@code FL.waters(250)} walk width (FL.java:689 = Water/MnWtr/DistW/SpDew). */
	public static final int WATER_WALK = 4;

	/**
	 * The {@link GT6Foods#FOODS} indices this band consumes — FOOD_ROWS order (the
	 * registration table the tests pin id-by-index, so an items-core reorder trips here
	 * instead of silently rewiring the rows).
	 */
	public static final int FOOD_CHEESE = 0, FOOD_BROWN_EGG_BOILED = 3, FOOD_WHITE_EGG_BOILED = 4,
			FOOD_POTATO_CHIPS = 33, FOOD_CHILI_CHIPS = 34, FOOD_ICE_CREAM = 35, FOOD_BUTTER = 37, FOOD_BUTTER_SALTED = 38;
	// REVIEW FIX seat XVII: the T4a egg/meat rows joined FOOD_ROWS out of this card's band
	// (upstream meta order — white_egg :496 + cheese_sliced insert ahead of the boiled twins),
	// shifting every post-egg index; bumped to the current 38-row table, the pin test validates.
	// +1 again (task pool-drain-food-t5-tail): food_ice_cream_raisin :811 joined FOOD_ROWS at
	// index 36 — the upstream meta slot between the ice cream :809 and the butter :933.

	/** The five target maps of the band (the enum keeps the lazy table registry-free). */
	public enum Target {COAGULATOR, CRYO_MIXER, CENTRIFUGE, MIXER, BATH}

	/**
	 * One item leg: a vanilla item reference, a (prefix, material) pair, or a
	 * {@link GT6Foods#FOODS} index ({@code foodIndex >= 0}); exactly one is set. The
	 * food index rides the resolver seam ({@link #sFoodItemResolver}), the vanilla arm
	 * dereferences at build time (the ShCL FixedRow shape).
	 */
	record ItemLeg(@Nullable Supplier<Item> vanilla, @Nullable OreDictPrefix prefix, @Nullable OreDictMaterial material,
			int foodIndex, int count) {
		static ItemLeg of(Supplier<Item> aItem, int aCount) {return new ItemLeg(aItem, null, null, -1, aCount);}
		static ItemLeg of(OreDictPrefix aPrefix, OreDictMaterial aMaterial, int aCount) {return new ItemLeg(null, aPrefix, aMaterial, -1, aCount);}
		static ItemLeg ofFood(int aFoodIndex, int aCount) {return new ItemLeg(null, null, null, aFoodIndex, aCount);}
	}

	/** One fluid leg: the upstream FL name ({@link #resolveNamedFluid} key) + the mB amount. */
	record FluidLeg(String name, int amount) {}

	/**
	 * One transcribed row: the upstream loader line(s), the target map, the legs and the
	 * shared columns ({@code eUt}/{@code duration} are the upstream literals; every row
	 * is buffered — the {@code addRecipeX(T, …)} form).
	 */
	record Row(String note, Target target, ItemLeg[] inItems, FluidLeg[] inFluids, FluidLeg[] outFluids, ItemLeg[] outItems,
			long eUt, long duration) {}

	/** The upstream milk/cream family ids (FL.java:134-136/:258) → the port carriers. */
	public static final String MILK = "milk", MILK_GRC = "grcmilk.milk", MILK_SOY = "soymilk", CREAM = "grcmilk.cream";
	/** The FL.waters members (FL.java:689): vanilla water + MnWtr + DistW + SpDew. */
	public static final String WATER = "water", MNWTR = "mnwtr", DISTW = "distilled_water", SPDEW = "spdew";
	/** The Bath hot-fluid legs (:306-:312): the ic2hotwater alias is the DELIBERATE null (the Drying :530 verdict). */
	public static final String WATER_HOT_IC2 = "ic2hotwater", HOT_WATER = "hot_water", WATER_BOILING = "water_boiling", WATER_GEOTHERMAL = "water_geothermal";

	/** The four Bath hot-fluid legs in upstream order with their gating lines (:306-:312). */
	public static final List<FluidLeg> BATH_LEGS = List.of(
			new FluidLeg(WATER_HOT_IC2   , 100), // :306-307 — if (FL.Water_Hot.exists())
			new FluidLeg(HOT_WATER       , 100), // :308-309 — if (FL.Hot_Water.exists())
			new FluidLeg(WATER_BOILING   , 100), // :310-311 — if (FL.Water_Boiling.exists())
			new FluidLeg(WATER_GEOTHERMAL, 100)); // :312 — the ungated GT6-core leg

	/** The skipped upstream surface, kept as DATA for the audit walk (see class doc). */
	public static final List<String> SKIPPED_UPSTREAM = List.of(
			"Loader_Recipes_Food.java:673-:677 dropHoney x3 (FL.Honey/HoneyGrC/HoneyBoP 200 → OreDictManager.getFirstOre(\"dropHoney\")) — Forestry ore-dict output, OD.java:159-160 zero members in a GT6-only environment: TRUE NEGATIVE, upstream registers nothing and NO GT6 substitute is fabricated (the P10 foreign-identity ruling, the B2a coagulator verdict)",
			"Loader_Recipes_Food.java:678-:679 dropHoneydew — the same Forestry getFirstOre face: TRUE NEGATIVE, no substitute",
			"Loader_Recipes_Food.java:680-:681 RoyalJelly (FL.RoyalJelly 100 → IL.FR_Royal_Jelly/IL.HaC_Royal_Jelly) — foreign-mod items: TRUE NEGATIVE, no substitute",
			"Loader_Recipes_Food.java:683-:686 Glue/Blood (FL.Glue 144 / FL.Blood 160 → TiC materials:36 / strangeFood:1) — foreign-mod items: TRUE NEGATIVE, no substitute",
			"Loader_Recipes_Food.java:663 Juice_Coconut→Cream_Coconut + :666-:669 the mixer milk→cream stick-catalyst legs — the cream-fluid chain rows (the T2 搅拌机流体行 domain), outside this card's 5-row butter band (:660-:662/:664/:670)",
			"Loader_Recipes_Food.java:696 MilkSoy→Tofu — already live, not re-added (the card boundary)",
			"Loader_Recipes_Food.java:313 Autoclave / :314 Juicer / :315 Centrifuge / :317 smelting egg legs — outside the Bath arm (the T4 蛋肉罐头 domain)",
			"Loader_Recipes_Food.java:305 itemEggBig tAmount=4 face — no big-egg item in the port, tAmount rides as the literal 1");

	/** The material-item seam: (prefix, material) → the registered item (the Mixer resolver share). */
	static BiFunction<OreDictPrefix, OreDictMaterial, Item> sMaterialItemResolver = GT6RecipesFood::resolveItem;

	/** The food-item seam: the {@link GT6Foods#FOODS} index → the registered item (unbind-safe: null offline). */
	static java.util.function.IntFunction<Item> sFoodItemResolver = GT6RecipesFood::defaultFoodItem;

	/** The live food lookup — null offline (the holder unbind guard), the item on a live server. */
	@Nullable
	static Item defaultFoodItem(int aIndex) {
		//? if forge {
		RegistryObject<Item> tHandle = GT6Foods.FOODS.get(aIndex);
		return tHandle.isPresent() ? tHandle.get() : null;
		//?} else {
		/*net.neoforged.neoforge.registries.DeferredHolder<Item, Item> tHandle = GT6Foods.FOODS.get(aIndex);
		return tHandle.isBound() ? tHandle.get() : null;
		*///?}
	}

	/** The raw GT6 white-egg seam — port-absent today; the moment the item lands the 4 white Bath rows pour with no code change. */
	static Supplier<Item> sWhiteEggResolver = () -> null;

	/** The named-fluid seam: the upstream FL name → the port carrier (the Drying resolveFluid shape). */
	static Function<String, Fluid> sNamedFluidResolver = GT6RecipesFood::resolveNamedFluid;

	/** The transcribed band, captured on first use (one material generation; the a9027ac laziness). */
	private static volatile List<Row> sTable = null;

	/**
	 * The 29 transcribed rows, band order = the class doc order = the upstream file order
	 * inside each band. Loop walks (the 4 waters × 2 cryo shapes, the 3 chili forms, the
	 * 2 egg identities × 4 Bath legs) are expanded statically against the live families.
	 */
	public static List<Row> table() {
		List<Row> tTable = sTable;
		if (tTable == null) {
			List<Row> tRows = new ArrayList<>(TOTAL_ROWS);
			// Loader_Recipes_Food.java:694-695 — the coagulator cheese pair
			tRows.add(new Row(":694", Target.COAGULATOR, new ItemLeg[0],
					new FluidLeg[] {new FluidLeg(MILK, 1000)}, new FluidLeg[0],
					new ItemLeg[] {ItemLeg.ofFood(FOOD_CHEESE, 1)}, 0, 1024));
			tRows.add(new Row(":695", Target.COAGULATOR, new ItemLeg[0],
					new FluidLeg[] {new FluidLeg(MILK_GRC, 1000)}, new FluidLeg[0],
					new ItemLeg[] {ItemLeg.ofFood(FOOD_CHEESE, 1)}, 0, 1024));
			// MultiItemFood.java:847-850 — the ice-cream base, FL.waters(250) x the two shapes
			String[] tWaters = {WATER, MNWTR, DISTW, SPDEW};
			for (String tWater : tWaters) {
				tRows.add(new Row(":848[" + tWater + "]", Target.CRYO_MIXER,
						new ItemLeg[] {ItemLeg.of(OP.dustSmall, MT.NaCl, 1)},
						new FluidLeg[] {new FluidLeg(tWater, 250), new FluidLeg(CREAM, 250)}, new FluidLeg[0],
						new ItemLeg[] {ItemLeg.ofFood(FOOD_ICE_CREAM, 1)}, 16, 16));
				tRows.add(new Row(":849[" + tWater + "]", Target.CRYO_MIXER,
						new ItemLeg[] {ItemLeg.of(OP.dust, MT.NaCl, 1)},
						new FluidLeg[] {new FluidLeg(tWater, 1000), new FluidLeg(CREAM, 1000)}, new FluidLeg[0],
						new ItemLeg[] {ItemLeg.ofFood(FOOD_ICE_CREAM, 4)}, 16, 64));
			}
			// Loader_Recipes_Food.java:660-:664 — the centrifuge dairy band (the :663 coconut row is out-of-band)
			tRows.add(new Row(":660", Target.CENTRIFUGE, new ItemLeg[0],
					new FluidLeg[] {new FluidLeg(MILK, 50)}, new FluidLeg[] {new FluidLeg(CREAM, 50)},
					new ItemLeg[0], 16, 16));
			tRows.add(new Row(":661", Target.CENTRIFUGE, new ItemLeg[0],
					new FluidLeg[] {new FluidLeg(MILK_GRC, 50)}, new FluidLeg[] {new FluidLeg(CREAM, 50)},
					new ItemLeg[0], 16, 16));
			tRows.add(new Row(":662", Target.CENTRIFUGE, new ItemLeg[0],
					new FluidLeg[] {new FluidLeg(MILK_SOY, 50)}, new FluidLeg[] {new FluidLeg(CREAM, 50)},
					new ItemLeg[0], 16, 16));
			tRows.add(new Row(":664", Target.CENTRIFUGE, new ItemLeg[0],
					new FluidLeg[] {new FluidLeg(CREAM, 250)}, new FluidLeg[0],
					new ItemLeg[] {ItemLeg.ofFood(FOOD_BUTTER, 1)}, 16, 64));
			// Loader_Recipes_Food.java:670 — the mixer butter twin (:666-:669 are the out-of-band cream legs)
			tRows.add(new Row(":670", Target.MIXER,
					new ItemLeg[] {ItemLeg.of(OP.stick, MT.WoodTreated, 1)},
					new FluidLeg[] {new FluidLeg(CREAM, 250)}, new FluidLeg[0],
					new ItemLeg[] {ItemLeg.ofFood(FOOD_BUTTER, 1)}, 16, 64));
			// Loader_Recipes_Food.java:729-731 — the salted-butter ladder
			tRows.add(new Row(":729", Target.MIXER,
					new ItemLeg[] {ItemLeg.of(OP.dust, MT.NaCl, 1), ItemLeg.ofFood(FOOD_BUTTER, 9)},
					new FluidLeg[0], new FluidLeg[0],
					new ItemLeg[] {ItemLeg.ofFood(FOOD_BUTTER_SALTED, 9)}, 16, 144));
			tRows.add(new Row(":730", Target.MIXER,
					new ItemLeg[] {ItemLeg.of(OP.dustSmall, MT.NaCl, 1), ItemLeg.ofFood(FOOD_BUTTER, 2)},
					new FluidLeg[0], new FluidLeg[0],
					new ItemLeg[] {ItemLeg.ofFood(FOOD_BUTTER_SALTED, 2)}, 16, 32));
			tRows.add(new Row(":731", Target.MIXER,
					new ItemLeg[] {ItemLeg.of(OP.dustTiny, MT.NaCl, 1), ItemLeg.ofFood(FOOD_BUTTER, 1)},
					new FluidLeg[0], new FluidLeg[0],
					new ItemLeg[] {ItemLeg.ofFood(FOOD_BUTTER_SALTED, 1)}, 16, 16));
			// Loader_Recipes_Food.java:770-:771 — the chili-chips walk (the :770 array = dust 1 / dustSmall 4 / dustTiny 9)
			int[] tChiliCounts = {1, 4, 9};
			OreDictPrefix[] tChiliForms = {OP.dust, OP.dustSmall, OP.dustTiny};
			for (int i = 0; i < tChiliCounts.length; i++) {
				tRows.add(new Row(":771", Target.MIXER,
						new ItemLeg[] {ItemLeg.of(tChiliForms[i], MT.Chili, tChiliCounts[i]), ItemLeg.ofFood(FOOD_POTATO_CHIPS, 1)},
						new FluidLeg[0], new FluidLeg[0],
						new ItemLeg[] {ItemLeg.ofFood(FOOD_CHILI_CHIPS, 1)}, 16, 16));
			}
			// Loader_Recipes_Food.java:303-:312 — the Bath boiled-egg arm, 2 egg identities x 4 hot-fluid legs
			for (int tEgg = 0; tEgg < 2; tEgg++) {
				boolean tWhite = tEgg == 1;
				ItemLeg tEggIn = tWhite ? ItemLeg.of(GT6RecipesFood::whiteEgg, 1) : ItemLeg.of(() -> Items.EGG, 1);
				ItemLeg tEggOut = ItemLeg.ofFood(tWhite ? FOOD_WHITE_EGG_BOILED : FOOD_BROWN_EGG_BOILED, 1);
				// the shell by-product rides the IL.Dye_Bonemeal fallback position (the Birb shell is port-absent)
				ItemLeg[] tEggOuts = {tEggOut, ItemLeg.of(() -> Items.BONE_MEAL, 1)};
				for (FluidLeg tLeg : BATH_LEGS) {
					tRows.add(new Row(":" + legLine(tLeg) + (tWhite ? "[white]" : "[brown]"), Target.BATH,
							new ItemLeg[] {tEggIn},
							new FluidLeg[] {tLeg}, new FluidLeg[] {new FluidLeg(WATER, 100)},
							tEggOuts, 0, 128));
				}
			}
			sTable = tTable = List.copyOf(tRows);
		}
		return tTable;
	}

	/** The upstream line of one Bath hot-fluid leg (:307/:309/:311/:312 — the addRecipe lines). */
	private static int legLine(FluidLeg aLeg) {
		return switch (aLeg.name()) {
			case WATER_HOT_IC2 -> 307;
			case HOT_WATER -> 309;
			case WATER_BOILING -> 311;
			default -> 312; // WATER_GEOTHERMAL
		};
	}

	/** The white-egg input delegate — consults the seam at build time (seam-swappable without a table rebuild). */
	@Nullable
	private static Item whiteEgg() {return sWhiteEggResolver.get();}

	/** Poured flag — one generation, one pour (upstream loaders run once per JVM). */
	private static boolean sLoaded = false;
	/** The reconciliation counters of the last {@link #load()} (the offline audit face). */
	private static final int[] sPoured = new int[Target.values().length], sSkipped = new int[Target.values().length];

	// ADR-P18: the pour-flag joins the GT6RecipeMaps generation (the Canner/Drying/Mixer form).
	static {GT6RecipeMaps.registerGenerationResetHook(GT6RecipesFood::resetForTest);}

	/** FMLCommonSetup.enqueueWork — items, fluids and the maps have fired by this point. */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(GT6RecipesFood::load);
	}

	/**
	 * Pours the 29-row band into its five maps. Idempotent; an unresolvable row skips with
	 * a count (the declared dormancies: the 4 white-egg rows and the :307 ic2hotwater leg
	 * in today's port universe = 24 pours / 5 skips).
	 */
	public static synchronized void load() {
		if (sLoaded) {LOGGER.debug("load() skipped: already poured (generation flag set)"); return;}
		GT6RecipeMaps.init(); // defensive + idempotent: the maps exist from ConstructMod, tests may race it
		if (recipeMap(Target.COAGULATOR) == null || recipeMap(Target.CRYO_MIXER) == null
				|| recipeMap(Target.CENTRIFUGE) == null || recipeMap(Target.MIXER) == null
				|| recipeMap(Target.BATH) == null) return; // reset() between init and load — a broken lifecycle
		int tPoured = 0, tSkipped = 0;
		for (Row tRow : table()) {
			Recipe tRecipe = build(tRow);
			if (tRecipe == null) {sSkipped[tRow.target().ordinal()]++; tSkipped++; continue;} // = upstream mat() → null silent drop
			recipeMap(tRow.target()).addRecipe(tRecipe);
			sPoured[tRow.target().ordinal()]++; tPoured++;
		}
		sLoaded = true;
		LOGGER.info("GT6 food band poured: {} rows loaded, {} skipped (the declared dormancies: the raw GT6 white egg and the ic2hotwater alias), = upstream mat() null drops", tPoured, tSkipped);
	}

	/** The map of one band target (post-{@link GT6RecipeMaps#init()}). */
	static RecipeMap recipeMap(Target aTarget) {
		return switch (aTarget) {
			case COAGULATOR -> GT6RecipeMaps.COAGULATOR;
			case CRYO_MIXER -> GT6RecipeMaps.CRYO_MIXER;
			case CENTRIFUGE -> GT6RecipeMaps.CENTRIFUGE;
			case MIXER -> GT6RecipeMaps.MIXER;
			case BATH -> GT6RecipeMaps.BATH;
		};
	}

	/**
	 * Row → Recipe, or null when any leg fails to resolve (the upstream silent-drop
	 * semantics). Every row is buffered (the upstream {@code addRecipeX(T, …)} form).
	 */
	@Nullable
	static Recipe build(Row aRow) {
		ItemStack[] tIns = resolveItems(aRow.inItems());
		if (tIns == null) return null;
		ItemStack[] tOuts = resolveItems(aRow.outItems());
		if (tOuts == null) return null;
		FluidStack[] tFluidIns = resolveFluids(aRow.inFluids());
		if (tFluidIns == null) return null;
		FluidStack[] tFluidOuts = resolveFluids(aRow.outFluids());
		if (tFluidOuts == null) return null;
		return new Recipe(true, tIns, tOuts, tFluidIns, tFluidOuts, aRow.duration(), aRow.eUt(), 0);
	}

	/** Legs → stacks, or null when any leg fails (the count carries the upstream literal). */
	@Nullable
	private static ItemStack[] resolveItems(ItemLeg[] aLegs) {
		ItemStack[] rStacks = new ItemStack[aLegs.length];
		for (int i = 0; i < aLegs.length; i++) {
			ItemLeg tLeg = aLegs[i];
			Item tItem = tLeg.foodIndex() >= 0 ? sFoodItemResolver.apply(tLeg.foodIndex())
					: tLeg.prefix() != null ? sMaterialItemResolver.apply(tLeg.prefix(), tLeg.material())
					: tLeg.vanilla().get();
			if (tItem == null) return null;
			rStacks[i] = new ItemStack(tItem, tLeg.count());
		}
		return rStacks;
	}

	/** Legs → stacks, or null when any named fluid fails to resolve. */
	@Nullable
	private static FluidStack[] resolveFluids(FluidLeg[] aLegs) {
		FluidStack[] rStacks = new FluidStack[aLegs.length];
		for (int i = 0; i < aLegs.length; i++) {
			Fluid tFluid = sNamedFluidResolver.apply(aLegs[i].name());
			if (tFluid == null) return null;
			rStacks[i] = new FluidStack(tFluid, aLegs[i].amount());
		}
		return rStacks;
	}

	/** The live material-item lookup (GTMaterialItems.get) — null when the pair has no item-path item or the handle is unbound. */
	@Nullable
	static Item resolveItem(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		//? if forge {
		RegistryObject<Item> tHandle = GTMaterialItems.get(aPrefix, aMaterial);
		return tHandle == null || !tHandle.isPresent() ? null : tHandle.get();
		//?} else {
		/*net.neoforged.neoforge.registries.DeferredHolder<Item, Item> tHandle = GTMaterialItems.get(aPrefix, aMaterial);
		return tHandle == null || !tHandle.isBound() ? null : tHandle.get();
		*///?}
	}

	/**
	 * The live named-fluid lookup — the Drying resolveFluid shape. The waters ride the
	 * GT6RecipesMixer.resolveWater carriers (FL.DistW → the distilled_water EngineFluid,
	 * the two-precedent convergence); the :307 {@code FL.Water_Hot} IC2 alias is the
	 * DELIBERATE null (the Drying :530 verdict — no ic2hotwater carrier is fabricated).
	 */
	@Nullable
	static Fluid resolveNamedFluid(String aName) {
		return switch (aName) {
			case WATER           -> Fluids.WATER;
			case MNWTR           -> source(GTFluids.MNWTR.source);
			case DISTW           -> source(GTFluids.DISTILLED_WATER.source);
			case SPDEW           -> source(GTFluids.SPDEW.source);
			case MILK            -> source(GTFluids.MILK.source);
			case MILK_GRC        -> source(GTFluids.GRCMILK_MILK.source);
			case MILK_SOY        -> source(GTFluids.SOYMILK.source);
			case CREAM           -> source(GTFluids.GRCMILK_CREAM.source);
			case WATER_HOT_IC2   -> null; // the FL.Water_Hot IC2 alias — port-absent, no carrier fabricated
			case HOT_WATER       -> source(GTFluids.HOT_WATER.source);
			case WATER_BOILING   -> source(GTFluids.WATER_BOILING.source);
			case WATER_GEOTHERMAL-> source(GTFluids.WATER_GEOTHERMAL.source);
			default -> null;
		};
	}

	//? if forge {
	/** The RegistryObject unbind-safe dereference (the GT6RecipesBath resolveOil guard): null offline, the fluid on a live server. */
	@Nullable
	private static Fluid source(RegistryObject<net.minecraft.world.level.material.FlowingFluid> aHandle) {
		return aHandle.isPresent() ? aHandle.get() : null;
	}
	//?} else {
	/*//21.1: the DeferredHolder/Holder.isBound face of the same guard.
	@Nullable
	private static Fluid source(net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.level.material.Fluid, net.minecraft.world.level.material.FlowingFluid> aHandle) {
		return aHandle.isBound() ? aHandle.get() : null;
	}
	*///?}

	/** The last pour's poured count for one band (the offline reconciliation face). */
	public static int lastPoured(Target aTarget) {return sPoured[aTarget.ordinal()];}

	/** The last pour's skipped count for one band (the offline reconciliation face). */
	public static int lastSkipped(Target aTarget) {return sSkipped[aTarget.ordinal()];}

	/** Test seam: clears the poured flag, the captured table and the counters so a fresh generation can re-pour. */
	static void resetForTest() {
		sLoaded = false;
		sTable = null;
		java.util.Arrays.fill(sPoured, 0);
		java.util.Arrays.fill(sSkipped, 0);
	}

	private GT6RecipesFood() {}
}
