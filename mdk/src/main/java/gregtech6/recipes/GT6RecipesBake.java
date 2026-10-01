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
import java.util.function.IntFunction;
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
import gregtech6.item.GT6Circuits;
import gregtech6.registry.GT6BakeFoods;
import gregtech6.registry.GT6SlicerBlades;
import gregtech6.registry.GTMaterialItems;

/**
 * The food-domain T3b recipe-row replay (task food-bake-recipes) — the RUNTIME RM rows of
 * the bake chain over the {@link GT6BakeFoods} item universe, transcribed verbatim from the
 * upstream loaders (every number/duration/EUt/fluid-amount is the upstream literal; the
 * {@code note} of each row carries the upstream file:line). Four accounting buckets:
 *
 * <ul>
 *   <li><b>Bucket 1, the listener block :137-153</b> — Loader_Recipes_Food.java, the
 *       {@code foodDough}/{@code foodSugarDough} ore-dict listeners statically expanded
 *       over the GT6-owned members (the GT6RecipesBees listener-expansion house form): the
 *       {@code foodDough} member set is exactly {@code Food_Dough} (MultiItemFood.java:583
 *       — the sibling doughs carry their own names :585/:586/:589), the
 *       {@code foodSugarDough} set is {@code Food_Dough_Sugar}/{@code _Sugar_Raisins}/
 *       {@code _Sugar_Chocolate_Raisins} (:584/:587/:588, all three carry the name).
 *       Per member: RollingMill dough → flat (:139), Mixer sugar/cocoa/chocolate (:140-143),
 *       Press bun/bread/baguette/toast molds (:146-149), Press cylinder → raw cake bottom
 *       (:152). The :138 {@code RM.rem_smelting} face is the declared no-op (the port
 *       registers no dough smelting to remove — {@link #SKIPPED_UPSTREAM}).</li>
 *   <li><b>Bucket 4, the MultiItemFood inline RM rows</b> — :650-761 band + the
 *       :341-:360 potato/fries band: the Slicer rows (:602/:610/:618/:629 cookie doughs and
 *       :688/:727/:757/:785 loaf splits — the blade rides the upstream stack-size-0 marker
 *       as the never-consumed input, the {@link Recipe#sNotConsumable} shaping-tool arm),
 *       the :357 fries Bath row (10 mB hot frying oil = the upstream
 *       {@code MT.FryingOilHot.liquid(U/100)} — OreDictMaterial.liquid:1307-1313 folds the
 *       material units), and the three :687/:726/:756 {@code RM.packunpack} pairs
 *       (RM.java:239-244 = Boxinator pack {@code content xN + tag(n)} → full, Unboxinator
 *       unpack full → {@code N x content}; the selector tag is the
 *       {@link GT6Circuits#selector} configuration face). The :756 pair is the upstream
 *       copy-paste quirk VERBATIM (second arg {@code Food_Baguette_Sliced.get(1)} where the
 *       :754-:755 crafting twins use {@code Food_Baguettes_Sliced}) — the 2↔1 self-loop is
 *       ported as written and pinned in the test.</li>
	 *   <li><b>Buckets 2/3, the oven smelt family and the crafting-table faces</b> — NOT
	 *       poured here (the vanilla-recipe JSON domain): the port FURNACE map is a vanilla
	 *       RecipeManager proxy (RecipeMapFurnace.java:37-40 — the stock set is never read
	 *       and the map is JEI-excluded, GT6RecipeMapViewerMeta:63), and the crafting rows
	 *       have no runtime channel. They land in the {@code GT6CraftingRecipes} foodBake
	 *       datagen band (15 smelt + 37 leg-complete crafting rows, the same card) — the
	 *       follow-up-declared remainder (absent legs, foreign gates, the delate face) and
	 *       the POOLED domains stay in {@link #SKIPPED_UPSTREAM}.</li>
 *   <li><b>The POOLED faces</b> — the Sandwiches.INGREDIENTS system and its :158-250 seat
 *       block (un-ported system; the GT6-only legs are TRUE NEGATIVE — every listener gates
 *       {@code !ST.isGT} so a GT6-only environment registers nothing), the FoodsGT wolf
 *       feeding, the OreDictItemData/stat container/TC/BooksGT seats, the chum potion
 *       faces, the {@code RM.food_can} tin family (the Canner-map domain) and the
 *       {@code replicateOrganic} crop faces — all listed in {@link #SKIPPED_UPSTREAM}.</li>
 * </ul>
 *
 * <p><b>Declared dormancy</b> (the transcribed row whose material pair has no registered
 * item — the upstream {@code mat()} null-drop semantics, it skips with a count): the :140
 * {@code gemChipped Sugar} Mixer leg (the pair is unregistered — the T2 :761 verdict, and
 * the 21.1 live registry confirms). The :360 {@code plateDouble Paper} Boxinator leg is
 * NOT dormant: the pair resolves in the live item flood ({@code gt6:plate_double_paper} —
 * the force-table comment covers plateTiny only; the earlier dormancy call was a misread,
 * corrected against the 21.1 live registry). 28 rows transcribed, 27 pour in the live
 * port universe.
 *
 * <p><b>Seams</b> (the GT6RecipesFood/GT6RecipesSlicer shape): the material-item, bake-item,
 * mold-item, blade, selector and named-fluid resolvers are live by default, fixtures
 * injected offline. The table is LAZILY built — no static initializer captures MT/OP/
 * registry state (the a9027ac lesson). {@code load()} is idempotent per JVM generation; an
 * unresolvable row skips with a count; the per-map counts stay readable for the offline
 * reconciliation ({@link #lastPoured}/{@link #lastSkipped}).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6RecipesBake {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** The band sizes (the ratchet face): listener 12 (9 foodDough + 3 foodSugarDough) + inline 16 = {@link #TOTAL_ROWS}. */
	public static final int LISTENER_ROWS = 12;
	public static final int INLINE_ROWS = 16;
	public static final int TOTAL_ROWS = LISTENER_ROWS + INLINE_ROWS;

	/** The {@link GT6BakeFoods#BAKE_ROWS} indices this band consumes (the wiring face — the tests pin id-by-index). */
	public static final int BAKE_COOKIE_RAW = 0, BAKE_COOKIE_RAISINS_RAW = 1, BAKE_COOKIE_CHOCO_RAISINS_RAW = 3,
			BAKE_COOKIE_ABYSSAL_RAW = 5, BAKE_CAKEBOTTOM_RAW = 6, BAKE_DOUGH_FLAT = 8, BAKE_BUN_RAW = 18, BAKE_BUN = 19,
			BAKE_BUN_SLICED = 20, BAKE_BUNS_SLICED = 21, BAKE_BREAD_RAW = 29, BAKE_BREAD_SLICED = 30, BAKE_BREADS_SLICED = 31,
			BAKE_BAGUETTE_RAW = 36, BAKE_BAGUETTE = 37, BAKE_BAGUETTE_SLICED = 38, BAKE_BAGUETTES_SLICED = 39,
			BAKE_FRIES_RAW = 44, BAKE_FRIES = 45, BAKE_FRIES_PACKAGED = 46, BAKE_TOAST_RAW = 47, BAKE_TOAST = 48,
			BAKE_TOAST_SLICED = 49, BAKE_DOUGH = 51, BAKE_DOUGH_SUGAR = 52, BAKE_DOUGH_CHOCOLATE = 53,
			BAKE_DOUGH_SUGAR_RAISINS = 55, BAKE_DOUGH_SUGAR_CHOCO_RAISINS = 56, BAKE_DOUGH_ABYSSAL = 57;

	/** The {@link GT6BakeFoods#MOLDS} indices (upstream meta order: :334 empty, :338 bun, :339 bread, :340 baguette, :341 cylinder, :342 toast). */
	public static final int MOLD_EMPTY = 0, MOLD_BUN = 1, MOLD_BREAD = 2, MOLD_BAGUETTE = 3, MOLD_CYLINDER = 4, MOLD_TOAST = 5;

	/** The blade indices of the port's slicer-blade registry ({@link GT6SlicerBlades#SHAPE_SLICER_FLAT}/{@code _SPLIT}). */
	public static final int BLADE_FLAT = 0, BLADE_SPLIT = 1;

	/** The vanilla water name (the FL.Water carrier key). */
	public static final String WATER = "water";
	/** The upstream {@code FL.FryingOilHot} carrier (GTFluids.java:1100/:1335, the FOOD_B1 aqua row). */
	public static final String HOT_FRYING_OIL = "hotfryingoil";

	/** The seven target maps of the band (the enum keeps the lazy table registry-free). */
	public enum Target {ROLLING_MILL, MIXER, PRESS, SLICER, BATH, BOXINATOR, UNBOXINATOR}

	/**
	 * One item leg: the {@code kind} picks the carrier — a vanilla/registry item reference
	 * ({@code vanilla}, the supplier is responsible for the offline null, the {@link #bound}
	 * probe shape), a {@code (prefix, material)} pair, a selector-tag configuration
	 * (the upstream {@code ST.tag(n)} face, ST.java:779-781), or one of the three indexed
	 * registry walks (bake food / food mold / slicer blade — the {@code config} carries the
	 * index); exactly one kind is set.
	 */
	record ItemLeg(byte kind, @Nullable Supplier<Item> vanilla, @Nullable OreDictPrefix prefix, @Nullable OreDictMaterial material,
			int config, int count) {
		static final byte VANILLA = 0, MATERIAL = 1, SELECTOR = 2, BAKE = 3, MOLD = 4, BLADE = 5;
		static ItemLeg of(Supplier<Item> aItem, int aCount) {return new ItemLeg(VANILLA, aItem, null, null, -1, aCount);}
		static ItemLeg of(OreDictPrefix aPrefix, OreDictMaterial aMaterial, int aCount) {return new ItemLeg(MATERIAL, null, aPrefix, aMaterial, -1, aCount);}
		static ItemLeg ofSelector(int aConfig) {return new ItemLeg(SELECTOR, null, null, null, aConfig, 1);}
		static ItemLeg ofBake(int aIndex, int aCount) {return new ItemLeg(BAKE, null, null, null, aIndex, aCount);}
		static ItemLeg ofMold(int aIndex) {return new ItemLeg(MOLD, null, null, null, aIndex, 1);}
		static ItemLeg ofBlade(int aIndex) {return new ItemLeg(BLADE, null, null, null, aIndex, 1);}
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

	/**
	 * The skipped/declared upstream surface, kept as DATA for the audit walk (see class
	 * doc). The rows that LANDED in the GT6CraftingRecipes foodBake datagen band (the 15
	 * oven smelts + the 37 leg-complete crafting rows) are NOT repeated here — this ledger
	 * carries only what that band does not: the foreign/absent-leg skips, the dormancies,
	 * the no-op faces and the POOLED domains.
	 */
	public static final List<String> SKIPPED_UPSTREAM = List.of(
			"GT6CraftingRecipes.java foodBake band — the vanilla-recipe JSON faces of this card: the 15 oven smelt rows (MultiItemFood.java :348/:599/:607/:615/:626/:634/:653/:659/:665/:671/:679/:718/:748/:778/:779, the vanilla smelt constants carrying the unspecified upstream columns) and the 37 leg-complete crafting rows (the kX quartet :601/:609/:617/:628 + loaf splits :683/:722/:752/:783 + the :358 knife-fries + :642 rolling pin + :635 cake bottom + the :684/:723/:753/:784 dough ladder + :343/:349 potato sticks + :652/:658 pizza + :699-:708 burgers + :735-:740/:765-:770 sandwiches) — LISTED FOR THE AUDIT WALK, implemented in the datagen band, not poured here",
			"MultiItemFood.java:590 Dough_Abyssal → NeLi_Bread — the NeLi (Netherlicious) exists() gate is foreign and port-absent: the row is structurally dead in a GT6-only environment, faithful absence, TRUE NEGATIVE (the B2a verdict form)",
			"MultiItemFood.java:643-647 the ketchup ladder ×5 (Dough_Flat_Ketchup ×1..5 ← foodKetchup + N× Dough_Flat) — the foodKetchup ITEM is port-absent (the KETCHUP fluid is in, GTFluids FOOD_B1): input-unresolvable, DECLARED",
			"MultiItemFood.java:636 vanilla cake ← foodHeavycream + CakeBottom — the heavy-cream ITEM is port-absent (the fluid is in): input-unresolvable, DECLARED; :637 CR.delate(vanilla cake) — the port has no recipe-removal datagen channel, the vanilla cake JSON stays: DECLARED deviation",
			"MultiItemFood.java the T4/T5-leg crafting rows — :664/:670 the veggie/ananas pizzas (Cucumber/Tomato/Onion/Ananas/Ham slices, T5), :697/:698 the veggie burgers (same slices), :709/:710 the chum burgers (foodChum, T4), :733/:734 + :763/:764 the veggie sandwiches (slices), :737/:738 + :767/:768 the bacon sandwiches (foodBaconcooked, T4), :359 the plateDouble-Paper fries pack (the pair is unregistered) — input-unresolvable in this card's universe, DECLARED",
			"MultiItemFood.java:158-250 the Sandwiches.INGREDIENTS seat block — the Sandwiches system is un-ported; the GT6-only legs are TRUE NEGATIVE: the dust listeners :158-181 all gate !ST.isGT so a GT6-only environment registers nothing, :182-187 tofu (foreign-carrier faces), :188-194 caramel (the Mixer row's Food_Ice_Cream_Caramel output is port-absent), :225-241 bacon (the items are the T4 domain), :242-250 chum (T4 + Chum_On_Stick absent); the GT6-owned seats of the bake domain (:776 Toast_Sliced (byte)254, :777 Toasted_Sliced (byte)253) ride the Sandwiches POOLED declaration",
			"MultiItemFood.java:600/:608/:616/:624/:680-682/:719-721/:749-751/:780-782 the RM.food_can tin family (Cookie Tin ×4, Canned Bread ×6, Canned Pain ×3) — the Canner-map domain (the GT6RecipesCanner family), POOLED",
			"MultiItemFood.java POOLED faces — FoodsGT wolf/pet feeding (:336-355), the OreDictItemData faces (fries Potato U :354, potato-stick Potato U + Wood U2 :341/:347), the setFluidContainerStats faces (:354-356/:729-732/:758-761), the TC aspects (every row), the molds' BooksGT.BOOK_REGISTER seats, the chum/Scrap potion effects (:693/:798), replicateOrganic (:321/:325/:332/:337/:342 — the generify/crop domain, T5), the ic2_extractor leg of RM.packunpack (RM.java:236 — the foreign machine, a no-op in a GT6-only environment)",
			"Loader_Recipes_Food.java:138 RM.rem_smelting(foodDough) — the port registers no dough smelting, the removal face is vacuous: declared NO-OP",
			"Loader_Recipes_Food.java:140 gemChipped Sugar Mixer leg — DORMANT: the (gemChipped, Sugar) pair is unregistered (the T2 :761 verdict, gem_chipped_sugar absent; the 21.1 live registry confirms), the row is transcribed and skips with a count",
			"MultiItemFood.java:359-360 the fries-pack pair is LIVE (the earlier plateDouble-Paper dormancy call was a force-table misread — the pair resolves as gt6:plate_double_paper in the live flood): the :360 Boxinator row pours, the :359 CR twin rides the datagen band");

	/** The material-item seam: (prefix, material) → the registered item (the Mixer resolver share). */
	static BiFunction<OreDictPrefix, OreDictMaterial, Item> sMaterialItemResolver = GT6RecipesBake::resolveItem;

	/** The bake-item seam: the {@link GT6BakeFoods#FOODS} index → the registered item (unbind-safe: null offline). */
	static IntFunction<Item> sBakeItemResolver = GT6RecipesBake::defaultBakeItem;

	/** The mold-item seam: the {@link GT6BakeFoods#MOLDS} index → the registered item (unbind-safe: null offline). */
	static IntFunction<Item> sMoldItemResolver = GT6RecipesBake::defaultMoldItem;

	/** The blade seam: the {@link #BLADE_FLAT}/{@link #BLADE_SPLIT} index → the registered blade (unbind-safe: null offline). */
	static IntFunction<Item> sBladeResolver = GT6RecipesBake::defaultBladeItem;

	/** The selector-tag seam: the configuration n → the {@link GT6Circuits#selector} stack (the ST.tag face). */
	static IntFunction<ItemStack> sSelectorResolver = GT6Circuits::selector;

	/** The named-fluid seam: the upstream FL name → the port carrier (the Drying resolveFluid shape). */
	static Function<String, Fluid> sNamedFluidResolver = GT6RecipesBake::resolveNamedFluid;

	/**
	 * The shaping-tool identity seam of {@link Recipe#sNotConsumable}'s fifth arm: the six
	 * food molds (the upstream {@code IL.Shape_Foodmold_*.get(0)} press faces :146-:152)
	 * plus the flat slicer blade (the :602-:629/:785 rows — the flat blade is registered
	 * but carries no row of the earlier slicer card, so it is outside
	 * {@code GT6SlicerBlades.isBlade}'s row0 pair). Production default = item identity,
	 * fixtures injected offline (the sBladeTest two-contract rule).
	 */
	public static java.util.function.Predicate<ItemStack> sShapingToolTest = GT6RecipesBake::isShapingToolLive;

	/** The live identity walk over the six molds + the flat blade (the bound-guarded deref). */
	static boolean isShapingToolLive(@Nullable ItemStack aStack) {
		if (aStack == null || aStack.isEmpty()) return false;
		for (int i = 0; i < GT6BakeFoods.MOLDS.size(); i++) {
			Item tMold = defaultMoldItem(i);
			if (tMold != null && aStack.is(tMold)) return true;
		}
		Item tFlat = defaultBladeItem(BLADE_FLAT);
		return tFlat != null && aStack.is(tFlat);
	}

	/** The identity face {@code Recipe.sNotConsumable} consults — null-safe. */
	public static boolean isFoodShapingTool(@Nullable ItemStack aStack) {
		return sShapingToolTest.test(aStack);
	}

	/** The transcribed band, captured on first use (one material generation; the a9027ac laziness). */
	private static volatile List<Row> sTable = null;

	/**
	 * The 28 transcribed rows, band order = the class doc order = the upstream file order
	 * inside each bucket. The :152 sugar-dough Press row is expanded over the three
	 * {@code foodSugarDough} members (the listener-expansion face).
	 */
	public static List<Row> table() {
		List<Row> tTable = sTable;
		if (tTable == null) {
			List<Row> tRows = new ArrayList<>(TOTAL_ROWS);
			// Loader_Recipes_Food.java:137-150 — the foodDough listener, the one GT6-owned member
			tRows.add(new Row(":139", Target.ROLLING_MILL,
					new ItemLeg[] {bake(BAKE_DOUGH, 1)}, new FluidLeg[0], new FluidLeg[0],
					new ItemLeg[] {bake(BAKE_DOUGH_FLAT, 1)}, 16, 16));
			tRows.add(new Row(":140", Target.MIXER, // the gemChipped Sugar leg is DORMANT (the pair is unregistered)
					new ItemLeg[] {bake(BAKE_DOUGH, 1), ItemLeg.of(OP.gemChipped, MT.Sugar, 4)}, new FluidLeg[0], new FluidLeg[0],
					new ItemLeg[] {bake(BAKE_DOUGH_SUGAR, 2)}, 16, 16));
			tRows.add(new Row(":141", Target.MIXER,
					new ItemLeg[] {bake(BAKE_DOUGH, 1), ItemLeg.of(OP.dust, MT.Sugar, 1)}, new FluidLeg[0], new FluidLeg[0],
					new ItemLeg[] {bake(BAKE_DOUGH_SUGAR, 2)}, 16, 16));
			tRows.add(new Row(":142", Target.MIXER,
					new ItemLeg[] {bake(BAKE_DOUGH, 1), ItemLeg.of(OP.dust, MT.Cocoa, 1)}, new FluidLeg[0], new FluidLeg[0],
					new ItemLeg[] {bake(BAKE_DOUGH_CHOCOLATE, 1)}, 16, 16));
			tRows.add(new Row(":143", Target.MIXER,
					new ItemLeg[] {bake(BAKE_DOUGH, 1), ItemLeg.of(OP.dust, MT.Chocolate, 1)}, new FluidLeg[0], new FluidLeg[0],
					new ItemLeg[] {bake(BAKE_DOUGH_CHOCOLATE, 2)}, 16, 16));
			tRows.add(new Row(":146", Target.PRESS,
					new ItemLeg[] {bake(BAKE_DOUGH, 1), mold(MOLD_BUN)}, new FluidLeg[0], new FluidLeg[0],
					new ItemLeg[] {bake(BAKE_BUN_RAW, 1)}, 16, 16));
			tRows.add(new Row(":147", Target.PRESS,
					new ItemLeg[] {bake(BAKE_DOUGH, 2), mold(MOLD_BREAD)}, new FluidLeg[0], new FluidLeg[0],
					new ItemLeg[] {bake(BAKE_BREAD_RAW, 1)}, 16, 32));
			tRows.add(new Row(":148", Target.PRESS,
					new ItemLeg[] {bake(BAKE_DOUGH, 3), mold(MOLD_BAGUETTE)}, new FluidLeg[0], new FluidLeg[0],
					new ItemLeg[] {bake(BAKE_BAGUETTE_RAW, 1)}, 16, 48));
			tRows.add(new Row(":149", Target.PRESS,
					new ItemLeg[] {bake(BAKE_DOUGH, 4), mold(MOLD_TOAST)}, new FluidLeg[0], new FluidLeg[0],
					new ItemLeg[] {bake(BAKE_TOAST_RAW, 1)}, 16, 64));
			// Loader_Recipes_Food.java:151-153 — the foodSugarDough listener, three GT6-owned members
			for (int[] tMember : new int[][] {{BAKE_DOUGH_SUGAR, 0}, {BAKE_DOUGH_SUGAR_RAISINS, 1}, {BAKE_DOUGH_SUGAR_CHOCO_RAISINS, 2}}) {
				tRows.add(new Row(":152[" + new String[] {"sugar", "sugar_raisins", "sugar_choco_raisins"}[tMember[1]] + "]", Target.PRESS,
						new ItemLeg[] {bake(tMember[0], 4), mold(MOLD_CYLINDER)}, new FluidLeg[0], new FluidLeg[0],
						new ItemLeg[] {bake(BAKE_CAKEBOTTOM_RAW, 1)}, 16, 64));
			}
			// MultiItemFood.java:341-360 — the potato/fries band
			tRows.add(new Row(":357", Target.BATH,
					new ItemLeg[] {bake(BAKE_FRIES_RAW, 1)},
					new FluidLeg[] {new FluidLeg(HOT_FRYING_OIL, 10)}, new FluidLeg[0], // the upstream MT.FryingOilHot.liquid(U/100) = 10 mB
					new ItemLeg[] {bake(BAKE_FRIES, 1)}, 0, 16));
			tRows.add(new Row(":360", Target.BOXINATOR, // the plateDouble Paper leg is DORMANT (the pair is unregistered)
					new ItemLeg[] {bake(BAKE_FRIES, 1), ItemLeg.of(OP.plateDouble, MT.Paper, 1)}, new FluidLeg[0], new FluidLeg[0],
					new ItemLeg[] {bake(BAKE_FRIES_PACKAGED, 1)}, 16, 16));
			// MultiItemFood.java:598-629 — the cookie doughs, the flat blade
			tRows.add(new Row(":602", Target.SLICER,
					new ItemLeg[] {bake(BAKE_DOUGH_CHOCOLATE, 1), blade(BLADE_FLAT)}, new FluidLeg[0], new FluidLeg[0],
					new ItemLeg[] {bake(BAKE_COOKIE_RAW, 4)}, 16, 16));
			tRows.add(new Row(":610", Target.SLICER,
					new ItemLeg[] {bake(BAKE_DOUGH_SUGAR_RAISINS, 1), blade(BLADE_FLAT)}, new FluidLeg[0], new FluidLeg[0],
					new ItemLeg[] {bake(BAKE_COOKIE_RAISINS_RAW, 4)}, 16, 16));
			tRows.add(new Row(":618", Target.SLICER,
					new ItemLeg[] {bake(BAKE_DOUGH_SUGAR_CHOCO_RAISINS, 1), blade(BLADE_FLAT)}, new FluidLeg[0], new FluidLeg[0],
					new ItemLeg[] {bake(BAKE_COOKIE_CHOCO_RAISINS_RAW, 4)}, 16, 16));
			tRows.add(new Row(":629", Target.SLICER,
					new ItemLeg[] {bake(BAKE_DOUGH_ABYSSAL, 1), blade(BLADE_FLAT)}, new FluidLeg[0], new FluidLeg[0],
					new ItemLeg[] {bake(BAKE_COOKIE_ABYSSAL_RAW, 4)}, 16, 16));
			// MultiItemFood.java:675-785 — the loaf splits and the packunpack pairs
			tRows.add(new Row(":688", Target.SLICER,
					new ItemLeg[] {bake(BAKE_BUN, 1), blade(BLADE_SPLIT)}, new FluidLeg[0], new FluidLeg[0],
					new ItemLeg[] {bake(BAKE_BUN_SLICED, 2)}, 16, 16));
			tRows.add(new Row(":727", Target.SLICER,
					new ItemLeg[] {ItemLeg.of(() -> Items.BREAD, 1), blade(BLADE_SPLIT)}, new FluidLeg[0], new FluidLeg[0],
					new ItemLeg[] {bake(BAKE_BREAD_SLICED, 2)}, 16, 16));
			tRows.add(new Row(":757", Target.SLICER,
					new ItemLeg[] {bake(BAKE_BAGUETTE, 1), blade(BLADE_SPLIT)}, new FluidLeg[0], new FluidLeg[0],
					new ItemLeg[] {bake(BAKE_BAGUETTE_SLICED, 2)}, 16, 16));
			tRows.add(new Row(":785", Target.SLICER,
					new ItemLeg[] {bake(BAKE_TOAST, 1), blade(BLADE_FLAT)}, new FluidLeg[0], new FluidLeg[0],
					new ItemLeg[] {bake(BAKE_TOAST_SLICED, 8)}, 16, 16));
			tRows.add(new Row(":687", Target.BOXINATOR,
					new ItemLeg[] {bake(BAKE_BUN_SLICED, 2), ItemLeg.ofSelector(2)}, new FluidLeg[0], new FluidLeg[0],
					new ItemLeg[] {bake(BAKE_BUNS_SLICED, 1)}, 16, 16));
			tRows.add(new Row(":687[unpack]", Target.UNBOXINATOR,
					new ItemLeg[] {bake(BAKE_BUNS_SLICED, 1)}, new FluidLeg[0], new FluidLeg[0],
					new ItemLeg[] {bake(BAKE_BUN_SLICED, 2)}, 16, 16));
			tRows.add(new Row(":726", Target.BOXINATOR,
					new ItemLeg[] {bake(BAKE_BREAD_SLICED, 2), ItemLeg.ofSelector(2)}, new FluidLeg[0], new FluidLeg[0],
					new ItemLeg[] {bake(BAKE_BREADS_SLICED, 1)}, 16, 16));
			tRows.add(new Row(":726[unpack]", Target.UNBOXINATOR,
					new ItemLeg[] {bake(BAKE_BREADS_SLICED, 1)}, new FluidLeg[0], new FluidLeg[0],
					new ItemLeg[] {bake(BAKE_BREAD_SLICED, 2)}, 16, 16));
			// the :756 upstream copy-paste quirk VERBATIM (second arg Food_Baguette_Sliced.get(1), the :754-:755 twins use Baguettes_Sliced)
			tRows.add(new Row(":756", Target.BOXINATOR,
					new ItemLeg[] {bake(BAKE_BAGUETTE_SLICED, 2), ItemLeg.ofSelector(2)}, new FluidLeg[0], new FluidLeg[0],
					new ItemLeg[] {bake(BAKE_BAGUETTE_SLICED, 1)}, 16, 16));
			tRows.add(new Row(":756[unpack]", Target.UNBOXINATOR,
					new ItemLeg[] {bake(BAKE_BAGUETTE_SLICED, 1)}, new FluidLeg[0], new FluidLeg[0],
					new ItemLeg[] {bake(BAKE_BAGUETTE_SLICED, 2)}, 16, 16));
			sTable = tTable = List.copyOf(tRows);
		}
		return tTable;
	}

	// ------------------------------------------------------------------ leg factories

	/** A bake food leg — the {@link GT6BakeFoods#FOODS} index resolves at row build. */
	private static ItemLeg bake(int aIndex, int aCount) {return ItemLeg.ofBake(aIndex, aCount);}

	/** A food-mold leg — the never-consumed shaping tool (the upstream stack-size-0 marker). */
	private static ItemLeg mold(int aIndex) {return ItemLeg.ofMold(aIndex);}

	/** A slicer-blade leg — the never-consumed shaping tool (the upstream stack-size-0 marker). */
	private static ItemLeg blade(int aIndex) {return ItemLeg.ofBlade(aIndex);}

	// ------------------------------------------------------------------ resolvers

	/** The live bake-food lookup — null offline (the holder unbind guard), the item on a live server. */
	@Nullable
	static Item defaultBakeItem(int aIndex) {return bound(GT6BakeFoods.FOODS.get(aIndex));}

	/** The live food-mold lookup — null offline. */
	@Nullable
	static Item defaultMoldItem(int aIndex) {return bound(GT6BakeFoods.MOLDS.get(aIndex));}

	/** The live blade lookup — the flat/split registry entries, null offline. */
	@Nullable
	static Item defaultBladeItem(int aIndex) {
		return bound(aIndex == BLADE_FLAT ? GT6SlicerBlades.SHAPE_SLICER_FLAT : GT6SlicerBlades.SHAPE_SLICER_SPLIT);
	}

	/** The RegistryObject unbind-safe dereference (the GT6RecipesFood.defaultFoodItem guard): null offline, the item on a live server. */
	@Nullable
	static Item bound(RegistryObject<Item> aHandle) {
		//? if forge {
		return aHandle.isPresent() ? aHandle.get() : null;
		//?} else {
		/*net.neoforged.neoforge.registries.DeferredHolder<Item, Item> tHandle = aHandle;
		return tHandle.isBound() ? tHandle.get() : null;
		*///?}
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
	 * The live named-fluid lookup — the Drying resolveFluid shape. The only bake face today
	 * is the :357 hot frying oil (the FOOD_B1 aqua carrier).
	 */
	@Nullable
	static Fluid resolveNamedFluid(String aName) {
		return switch (aName) {
			case WATER          -> Fluids.WATER;
			case HOT_FRYING_OIL -> source(GTFluids.HOTFRYINGOIL.source);
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

	// ------------------------------------------------------------------ the pour

	/** Poured flag — one generation, one pour (upstream loaders run once per JVM). */
	private static boolean sLoaded = false;
	/** The reconciliation counters of the last {@link #load()} (the offline audit face). */
	private static final int[] sPoured = new int[Target.values().length], sSkipped = new int[Target.values().length];

	// ADR-P18: the pour-flag joins the GT6RecipeMaps generation (the Canner/Drying/Mixer form).
	static {GT6RecipeMaps.registerGenerationResetHook(GT6RecipesBake::resetForTest);}

	/** FMLCommonSetup.enqueueWork — items, fluids and the maps have fired by this point. */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(GT6RecipesBake::load);
	}

	/**
	 * Pours the 28-row band into its seven maps. Idempotent; an unresolvable row skips with
	 * a count (the declared dormancy: the :140 gemChipped Sugar leg in today's
	 * port universe = 27 pours / 1 skip).
	 */
	public static synchronized void load() {
		if (sLoaded) {LOGGER.debug("load() skipped: already poured (generation flag set)"); return;}
		GT6RecipeMaps.init(); // defensive + idempotent: the maps exist from ConstructMod, tests may race it
		for (Target tTarget : Target.values()) if (recipeMap(tTarget) == null) return; // reset() between init and load — a broken lifecycle
		int tPoured = 0, tSkipped = 0;
		for (Row tRow : table()) {
			Recipe tRecipe = build(tRow);
			if (tRecipe == null) {sSkipped[tRow.target().ordinal()]++; tSkipped++; continue;} // = upstream mat() → null silent drop
			recipeMap(tRow.target()).addRecipe(tRecipe);
			sPoured[tRow.target().ordinal()]++; tPoured++;
		}
		sLoaded = true;
		LOGGER.info("GT6 bake band poured: {} rows loaded, {} skipped (the declared dormancy: the gemChipped Sugar pair), = upstream mat() null drops", tPoured, tSkipped);
	}

	/** The map of one band target (post-{@link GT6RecipeMaps#init()}). */
	static RecipeMap recipeMap(Target aTarget) {
		return switch (aTarget) {
			case ROLLING_MILL -> GT6RecipeMaps.ROLLING_MILL;
			case MIXER        -> GT6RecipeMaps.MIXER;
			case PRESS        -> GT6RecipeMaps.PRESS;
			case SLICER       -> GT6RecipeMaps.SLICER;
			case BATH         -> GT6RecipeMaps.BATH;
			case BOXINATOR    -> GT6RecipeMaps.BOXINATOR;
			case UNBOXINATOR  -> GT6RecipeMaps.UNBOXINATOR;
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
			ItemStack tStack;
			switch (tLeg.kind()) {
				case ItemLeg.SELECTOR -> tStack = sSelectorResolver.apply(tLeg.config());
				case ItemLeg.MATERIAL -> {
					Item tItem = sMaterialItemResolver.apply(tLeg.prefix(), tLeg.material());
					if (tItem == null) return null;
					tStack = new ItemStack(tItem, tLeg.count());
				}
				case ItemLeg.BAKE -> {
					Item tItem = sBakeItemResolver.apply(tLeg.config());
					if (tItem == null) return null;
					tStack = new ItemStack(tItem, tLeg.count());
				}
				case ItemLeg.MOLD -> {
					Item tItem = sMoldItemResolver.apply(tLeg.config());
					if (tItem == null) return null;
					tStack = new ItemStack(tItem, tLeg.count());
				}
				case ItemLeg.BLADE -> {
					Item tItem = sBladeResolver.apply(tLeg.config());
					if (tItem == null) return null;
					tStack = new ItemStack(tItem, tLeg.count());
				}
				default -> {
					Item tItem = tLeg.vanilla().get();
					if (tItem == null) return null;
					tStack = new ItemStack(tItem, tLeg.count());
				}
			}
			if (tStack == null || tStack.isEmpty()) return null;
			rStacks[i] = tStack;
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

	private GT6RecipesBake() {}
}
