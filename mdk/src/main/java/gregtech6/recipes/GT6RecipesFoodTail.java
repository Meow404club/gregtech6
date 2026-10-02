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
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import gregapi.data.ANY;
import gregapi.data.CS;
import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.fluid.GTFluids;

/**
 * The food-domain machine-row TAIL (task pool-drain-food-machine-tail) — the RUNTIME RM
 * rows of {@code Loader_Recipes_Food.java} that the earlier food cards left: the cheese
 * listener band (:123-:136), the egg bands (:295-:323), the chum Mixer quartet (:390-:398),
 * the fish-oil Squeezer/Juicer pair (:400-:414, the T4b SKIPPED_UPSTREAM leftover ①), the
 * mushroom soup (:517-:520), the two fluid-only singletons (:552/:555) and the inline
 * Mixer band (:719-:737) plus the raisin cluster (:154-:157/:253-:260). The
 * {@link GT6RecipesBake} transcribed-row form (static table + resolver seams + one
 * {@code load()} per-generation pour); NOT folded into {@link GT6RecipesMeat} so its
 * census ratchet stays still. Every row's {@code note} carries the upstream file:line.
 *
 * <p><b>The member universes</b> (the port oredict-less doctrine: the tables below ARE
 * the universe, tests pin them bidirectional-exact):
 * <ul>
 * <li><b>foodCheese</b> — the one GT6 food {@code gt6:food_cheese} (MultiItemFood.java:490;
 *     the ingotCheese re-registration LoaderOreDictReRegistrations.java:858 is the material
 *     item, not a food-row member — the :131 DUST_BASED guard excludes dust carriers and the
 *     ingot pours no cheese rows upstream either... it DOES: it rides the same listener; the
 *     port keeps the card's declared single-member face, the material-item members stay
 *     POOLED with the orechain domain). :135 {@code FoodsGT.put} = the T4b data domain
 *     (declared-skip, zero port surface).</li>
 * <li><b>foodBoiledegg</b> — {@code gt6:food_brown_egg_boiled} + {@code gt6:food_white_egg_boiled}
 *     (MultiItemFood.java:497/:498). The :296 {@code listAllmeatsubstitute} early-return is
 *     vacuous for them (neither is a substitute member).</li>
 * <li><b>OD.itemEgg</b> — vanilla {@code Items.EGG} (LoaderItemData.java:440) +
 *     {@code gt6:food_white_egg} (MultiItemFood.java:496, the ingredient egg). The :305
 *     {@code itemEggBig} tAmount=4 face is TRUE NEGATIVE: every big-egg member is a Birb mod
 *     egg (LoaderItemData.java:465-470, the MD.Birb rows) and the port has no Birb — the
 *     amount column rides as the literal 1 (the GT6RecipesFood :305 declaration verbatim).</li>
 * <li><b>foodScrapmeat</b> — EMPTY: the only GT6 member is the Scrap Meat meta 1998
 *     (MultiItemFood.java:581), pooled T3 and unregistered in the port. The :393 five-leg
 *     loop's ONE valid leg is the poisonous potato ({@code Items.POISONOUS_POTATO} —
 *     MultiItemFood.java:336 is a vanilla alias, zero registration need; the FZ/IE/TE slag
 *     legs are foreign-invalid), so the four loop-body {@code addRecipeX} lines are
 *     transcribed VERBATIM (:394-:397) with the declared {@code gt6:food_scrap_meat} input —
 *     and both skip at pour time (the unregistered input = the upstream silent drop; the
 *     sludge fluid-output leg is the second declared-null: FL.java:434 {@code Sludge} has no
 *     port spec and this card does NOT touch GTFluids — the fluids-prereq card owns the
 *     registration, one writer per seam).</li>
 * <li><b>listAllfishraw (the second listener :400-:414)</b> — the four vanilla fish, the
 *     GT6RecipesMeat CANNED_MEMBERS universe (cod/salmon/tropical/pufferfish), with the
 *     per-member oil columns of LoaderItemData.java:2601-2604 (the Bone stack never becomes
 *     the byproduct, :410; the FishRaw half IS the byproduct dust). The FIRST listAllfishraw
 *     listener (:369-:387) declares: :373 generify = TRUE NEGATIVE (vanilla members
 *     self-map, the port builds no GENERIFIER map), the :376-:384 Mortar face already poured
 *     with the meat card, the :386 Sandwiches seat POOLED. The :417 cooked listener has no
 *     machine rows beyond the canned face (already poured) — no rows here.</li>
 * <li><b>listAllmushroom</b> — vanilla brown + red (:504-:505, the only GT6-only members;
 *     EBXL/BoP/BOTA are foreign). The :518 guard excludes brown from the INPUT side (it is
 *     the second input of every row), so exactly one row: red + brown → soup.</li>
 * <li><b>foodRaisins</b> — green/white/red/purple/pomeraisins (MultiItemFood.java:312/:316/
 *     :320/:324/:391); <b>foodChocolateraisins</b> — the chocolate raisins (:327).</li>
 * <li><b>foodSugarDough</b> — all three sugary doughs (:584/:587/:588) but the :254/:258
 *     dedup gate verbatim excludes the two raisin doughs from the INPUT side, leaving
 *     {@code food_dough_sugar}.</li>
 * </ul>
 *
 * <p><b>TRUE NEGATIVE / dropped faces</b> (each with its reason, the test pins the
 * absence): the :123-:128 foodVanilla Shredder/Mortar band (the oredict name has NO GT6
 * carrier — MultiItemFood registers no vanilla-spice food item and the only GT6-only
 * carrier would be the dustVanilla item, which the :124 DUST_BASED guard excludes; the
 * mod-carrier members are foreign) — zero rows, no substitute fabricated; the :316 Birb
 * generify (above); the :373 fish generify (above); the :734/:736 gemChipped Sugar
 * chocolate rows (the pair is unregistered — the T2 :761 verdict, the bake :140 dormant
 * precedent — DROPPED per the card ruling, the juicer :860/:861 precedent); the :135/:391
 * FoodsGT.put arms (T4b data domain); the :307-:312 Bath boiled-egg legs + the :317 egg
 * smelting + the :660-:731 dairy/butter/chili bands (landed with GT6RecipesFood, the T1b
 * card) and the :404-:414/:417 canned faces (landed with GT6RecipesMeat, the T4b card).
 *
 * <p><b>Seams</b> (the bake form): the id-item, material-item, selector, blade and
 * named-fluid resolvers are live by default, fixtures injected offline. The named-fluid
 * resolver routes the spec families through {@link GTFluids#liveFluidSource} and the two
 * engine fluids through the bind-guarded sources; {@code sludge} resolves to null — the
 * declared-null chum output leg. The table is LAZILY built (no static capture of MT/OP
 * state, the a9027ac lesson). {@code load()} is idempotent per JVM generation; an
 * unresolvable row skips with a count; the last pour's counters stay readable
 * ({@link #lastPoured}/{@link #lastSkipped}).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6RecipesFoodTail {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** The transcribed-band sizes (the ratchet face; the walk segments grow with ANY). */
	public static final int CHEESE_ROWS = 3; // :130 Slicer + :132 Shredder + :133 Mortar over the one member
	public static final int EGG_SLICER_ROWS = 2; // :297 over the two boiled eggs
	public static final int EGG_MACHINE_ROWS = 6; // :313/:314/:315 x the two itemEgg members
	public static final int CHUM_ROWS = 4; // :394-:397 verbatim, BOTH legs declared-null (see class doc)
	public static final int FISH_OIL_ROWS = 8; // :412/:413 x the four fish
	public static final int RAISIN_ROWS = 10; // :155/:156 x the five foodRaisins members
	public static final int TWO_NAME_ROWS = 6; // :255 x 5 + :259 x 1 (the dedup gate)
	public static final int SINGLE_ROWS = 7; // :519 mushroom + :552 ambrosia + :555 spoiledmilk + :725 oat + :733/:735/:737 chocolate

	/** The vanilla water name (the FL.Water carrier key). */
	public static final String WATER = "water";
	/** The declared-null sludge carrier — resolves to null by contract (see class doc). */
	public static final String SLUDGE_NULL = "sludge";

	/** The nine target maps of the band. */
	public enum Target {SHREDDER, MORTAR, SLICER, AUTOCLAVE, JUICER, CENTRIFUGE, MIXER, SQUEEZER, BATH}

	/**
	 * One item leg: the {@code kind} picks the carrier — a vanilla/registry item reference,
	 * a gt6 food id (any of the three food registries, resolved by snake id), a
	 * {@code (prefix, material)} pair, a selector-tag configuration, or a slicer blade
	 * (the never-consumed shaping tool); exactly one kind is set.
	 */
	record ItemLeg(byte kind, @Nullable Supplier<Item> vanilla, @Nullable String foodId,
			@Nullable OreDictPrefix prefix, @Nullable OreDictMaterial material, int config, int count) {
		static final byte VANILLA = 0, FOOD = 1, MATERIAL = 2, SELECTOR = 3, BLADE = 4;
		static ItemLeg of(Supplier<Item> aItem, int aCount) {return new ItemLeg(VANILLA, aItem, null, null, null, -1, aCount);}
		static ItemLeg ofFood(String aId, int aCount) {return new ItemLeg(FOOD, null, aId, null, null, -1, aCount);}
		static ItemLeg of(OreDictPrefix aPrefix, OreDictMaterial aMaterial, int aCount) {return new ItemLeg(MATERIAL, null, null, aPrefix, aMaterial, -1, aCount);}
		static ItemLeg ofSelector(int aConfig) {return new ItemLeg(SELECTOR, null, null, null, null, aConfig, 1);}
		static ItemLeg ofBlade() {return new ItemLeg(BLADE, null, null, null, null, 0, 1);}
	}

	/** One fluid leg: the upstream FL name ({@link #resolveNamedFluid} key) + the mB amount. */
	record FluidLeg(String name, int amount) {}

	/**
	 * One transcribed row: the upstream loader line, the target map, the legs and the
	 * shared columns ({@code eUt}/{@code duration} are the upstream literals; every row
	 * is buffered — the {@code addRecipeX(T, …)} form).
	 */
	record Row(String note, Target target, ItemLeg[] inItems, FluidLeg[] inFluids, FluidLeg[] outFluids, ItemLeg[] outItems,
			long eUt, long duration) {}

	/**
	 * The skipped/declared upstream surface, kept as DATA for the audit walk (see class
	 * doc for the member-universe verdicts).
	 */
	public static final List<String> SKIPPED_UPSTREAM = List.of(
			"Loader_Recipes_Food.java:123-128 foodVanilla Shredder/Mortar — the oredict name has NO GT6-only carrier (MultiItemFood registers no vanilla-spice food item; the dustVanilla item would be DUST_BASED-guarded at :124): TRUE NEGATIVE, zero rows, no substitute fabricated",
			"Loader_Recipes_Food.java:135 FoodsGT.put(foodCheese 0,0,8,0,8) — the sandwich/canning data domain, zero port surface (the T4b declared-skip precedent)",
			"Loader_Recipes_Food.java:296/:300/:304/:320/:401 listAllmeatsubstitute early-returns — vacuous for every port member (none is a substitute)",
			"Loader_Recipes_Food.java:305 itemEggBig tAmount=4 face — every big-egg member is a Birb mod egg (LoaderItemData.java:465-470), the port has no Birb: TRUE NEGATIVE, the amount column rides as the literal 1",
			"Loader_Recipes_Food.java:307-312 Bath boiled-egg legs + :317 egg smelting — landed with GT6RecipesFood (the T1b Bath arm + the T5-tail bake_smelt_egg.json), not re-poured here",
			"Loader_Recipes_Food.java:316 Birb generify — MD.Birb gate, the port has no Birb: TRUE NEGATIVE (the GENERIFIER map is not built, declaration is the closure)",
			"Loader_Recipes_Food.java:369-387 the FIRST listAllfishraw listener — :373 generify TRUE NEGATIVE (vanilla members self-map, no GENERIFIER map); :376-:384 Mortar face already poured with GT6RecipesMeat; :386 Sandwiches seat POOLED",
			"Loader_Recipes_Food.java:391 FoodsGT.put(foodScrapmeat) + :404/:417 RM.food_can faces — the canned faces poured with GT6RecipesMeat, the FoodsGT arm the T4b data domain",
			"Loader_Recipes_Food.java:394-397 the chum rows' FL.Sludge output leg — DECLARED NULL: FL.java:434 has no port spec, this card does NOT touch GTFluids (the fluids-prereq card owns the registration, one writer per seam); the rows also carry the declared-absent gt6:food_scrap_meat input (the pooled T3 item) — both faces skip at pour",
			"Loader_Recipes_Food.java:417 listAllfishcooked — no machine rows beyond the canned face (already poured with GT6RecipesMeat): no rows here",
			"Loader_Recipes_Food.java:660-:731 the dairy/butter/chili/chocolate-cream bands — landed with GT6RecipesFood (the T1b five-map band)",
			"Loader_Recipes_Food.java:734/:736 the gemChipped Sugar chocolate rows — the (gemChipped, Sugar) pair is unregistered (the T2 :761 verdict, the bake :140 dormant precedent): DROPPED per the card ruling (the juicer :860/:861 precedent), 5 lines → 3 rows");

	/** The food-id seam (gt6 foods/bake/crop + vanilla, resolved by snake id) — live by default, fixtures injected offline. */
	public static Function<String, Item> sFoodItemResolver = GT6RecipesFoodTail::resolveFoodItem;

	/** The material-item seam (the Mixer resolver share). */
	static BiFunction<OreDictPrefix, OreDictMaterial, Item> sMaterialItemResolver = GT6RecipesFoodTail::resolveItem;

	/** The selector-tag seam (the upstream {@code ST.tag(n)} face), wrapped offline-safe: the forge test JVM leaves the gt6 circuit ITEM unbound, the bare {@code GT6Circuits.selector} THROWS ("Registry Object not present") and the shared-test pour dies — the null keeps the upstream silent drop (the GT6RecipesMeat.liveBbqSauce wrap precedent). */
	static IntFunction<ItemStack> sSelectorResolver = GT6RecipesFoodTail::liveSelector;

	/** The offline-safe selector leg (the liveBbqSauce wrap form). */
	@Nullable
	static ItemStack liveSelector(int aConfig) {
		try {return gregtech6.item.GT6Circuits.selector(aConfig);} catch (RuntimeException tOffline) {return null;}
	}

	/** The blade seam — the flat blade (the :130/:297 shaping tool, never consumed). */
	static Supplier<Item> sBladeResolver = () -> bound(gregtech6.registry.GT6SlicerBlades.SHAPE_SLICER_FLAT);

	/** The named-fluid seam: the upstream FL name → the port carrier (the bake form over {@link GTFluids#liveFluidSource}). */
	static Function<String, Fluid> sNamedFluidResolver = GT6RecipesFoodTail::resolveNamedFluid;

	/** The live food-id lookup over both namespaces — null when absent (the silent-drop face). */
	@Nullable
	static Item resolveFoodItem(String aId) {
		int tColon = aId.indexOf(':');
		return ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
				tColon < 0 ? "gt6" : aId.substring(0, tColon), aId.substring(tColon + 1)));
	}

	/** The live material-item lookup (GTMaterialItems.get) — null when the pair has no item-path item. */
	@Nullable
	static Item resolveItem(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		//? if forge {
		RegistryObject<Item> tHandle = gregtech6.registry.GTMaterialItems.get(aPrefix, aMaterial);
		return tHandle == null || !tHandle.isPresent() ? null : tHandle.get();
		//?} else {
		/*net.neoforged.neoforge.registries.DeferredHolder<Item, Item> tHandle = gregtech6.registry.GTMaterialItems.get(aPrefix, aMaterial);
		return tHandle == null || !tHandle.isBound() ? null : tHandle.get();
		*///?}
	}

	/** The RegistryObject unbind-safe dereference (the bake guard): null offline, the item on a live server. */
	@Nullable
	static Item bound(RegistryObject<Item> aHandle) {
		//? if forge {
		return aHandle.isPresent() ? aHandle.get() : null;
		//?} else {
		/*net.neoforged.neoforge.registries.DeferredHolder<Item, Item> tHandle = aHandle;
		return tHandle.isBound() ? tHandle.get() : null;
		*///?}
	}

	/**
	 * The live named-fluid lookup: the two engine fluids ride the bind-guarded sources,
	 * everything else walks {@link GTFluids#liveFluidSource} (the spec families + the
	 * aqua/simple/food walks). {@code sludge} resolves to null BY CONTRACT — the declared
	 * -null chum output leg (see class doc). The whole walk is wrapped offline-safe: the
	 * forge test JVM leaves the gt6 FLUIDS unbound and the inner {@code source.get()}s
	 * THROW ("Registry Object not present") — the null keeps the upstream silent drop
	 * (the liveBbqSauce wrap precedent).
	 */
	@Nullable
	static Fluid resolveNamedFluid(String aName) {
		switch (aName) {
		case "steam" -> {return source(GTFluids.STEAM.source);}
		case "distilled_water" -> {return source(GTFluids.DISTILLED_WATER.source);}
		case SLUDGE_NULL -> {return null;}
		// the FOOD_B1 carriers: liveFluidSource walks foodFluids() only, the B1/B2/TAIL
		// families stay outside it (the bake card's per-name HOT_FRYING_OIL case precedent)
		case "purpledrink" -> {return source(GTFluids.PURPLEDRINK.source);}
		case "fishoil" -> {return source(GTFluids.FISHOIL.source);}
		case "whaleoil" -> {return source(GTFluids.WHALEOIL.source);}
		case "soda" -> {return source(GTFluids.SODA.source);}
		case "spoiledmilk" -> {return source(GTFluids.SPOILEDMILK.source);}
		case "hellderberryjuice" -> {return source(GTFluids.HELLDERBERRYJUICE.source);}
		default -> {try {return GTFluids.liveFluidSource(aName);} catch (RuntimeException tOffline) {return null;}}
		}
	}

	//? if forge {
	/** The RegistryObject unbind-safe dereference (the bake source guard): null offline. */
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

	// ------------------------------------------------------------------ the members

	/** The OD.itemEgg members — input id, tAmount (the :305 face, literal 1: no big-egg member), boiled output id. */
	public record EggMember(String note, String itemId, int amount, String boiledId) {}

	/** The itemEgg walk (bidirectional-exact universe; see class doc). */
	public static final List<EggMember> EGG_MEMBERS = List.of(
			new EggMember(":440 vanilla egg"    , "minecraft:egg"        , 1, "gt6:food_brown_egg_boiled"),
			new EggMember(":496 gt6 white egg"  , "gt6:food_white_egg"   , 1, "gt6:food_white_egg_boiled"));

	/** The two foodBoiledegg Slicer members (:297). */
	public static final List<String> BOILED_EGG_MEMBERS = List.of("gt6:food_brown_egg_boiled", "gt6:food_white_egg_boiled");

	/**
	 * The fish-oil columns (LoaderItemData.java:2601-2604): the oil stack amount, the
	 * byproduct = the FishRaw amount halved (:410). Amounts in material units (CS.U).
	 */
	public record FishOilMember(String note, String itemId, long oilAmount, long byProductAmount) {}

	/** The listAllfishraw second-listener walk (the GT6RecipesMeat universe; bidirectional-exact). */
	public static final List<FishOilMember> FISH_OIL_MEMBERS = List.of(
			new FishOilMember(":2601 cod"       , "minecraft:cod"           , CS.U * 2, CS.U),
			new FishOilMember(":2602 salmon"    , "minecraft:salmon"        , CS.U * 4, CS.U),
			new FishOilMember(":2603 tropical"  , "minecraft:tropical_fish" , CS.U    , CS.U),
			new FishOilMember(":2604 pufferfish", "minecraft:pufferfish"    , CS.U    , CS.U2));

	/** The foodRaisins members (MultiItemFood.java:312/:316/:320/:324/:391). */
	public static final List<String> RAISIN_MEMBERS = List.of(
			"gt6:food_raisins_green", "gt6:food_raisins_white", "gt6:food_raisins_red", "gt6:food_raisins_purple", "gt6:food_pomeraisins");

	/** The foodChocolateraisins member (:327). */
	public static final String CHOCOLATE_RAISIN_MEMBER = "gt6:food_raisins_chocolate";

	/** The four chum row shapes (:394-:397 verbatim — the rotten-meat x oil matrix over the one valid loop leg). */
	public record ChumRow(String note, OreDictMaterial rotten, String oilName, int oilAmount) {}

	/**
	 * The chum quartet (the loop body in file order). LAZY — the a9027ac lesson: a static
	 * initializer capturing {@code MT.MeatRotten} reads null when the class loads before
	 * {@code MT.init()} (the neo-leg test-JVM load order empirics), so the table resolves
	 * at {@link #table()} time like every other walk.
	 */
	public static List<ChumRow> chumRows() {
		return List.of(
				new ChumRow(":394", MT.MeatRotten, "fishoil"  , 1000),
				new ChumRow(":395", MT.FishRotten, "fishoil"  , 1000),
				new ChumRow(":396", MT.MeatRotten, "whaleoil" ,  500),
				new ChumRow(":397", MT.FishRotten, "whaleoil" ,  500));
	}

	/** The Scrap Meat input id — pooled T3, unregistered: the declared-absent chum input (see class doc). */
	public static final String SCRAP_MEAT_INPUT = "gt6:food_scrap_meat";

	// ------------------------------------------------------------------ the table

	/** The transcribed band, captured on first use (one material generation; the a9027ac laziness). */
	private static volatile List<Row> sTable = null;

	/**
	 * The transcribed rows, segment order = the class doc order = the upstream file order
	 * inside each bucket. The walk segments (egg dough, meat ingots, raisins, two-names)
	 * expand over the live ANY walks / member tables at build time.
	 */
	public static List<Row> table() {
		List<Row> tTable = sTable;
		if (tTable == null) {
			List<Row> tRows = new ArrayList<>(64);

			// :123-136 — the cheese listener band (foodVanilla = TRUE NEGATIVE, see SKIPPED_UPSTREAM)
			tRows.add(new Row(":130", Target.SLICER,
					new ItemLeg[] {ItemLeg.ofFood("gt6:food_cheese", 1), ItemLeg.ofBlade()}, new FluidLeg[0], new FluidLeg[0],
					new ItemLeg[] {ItemLeg.ofFood("gt6:food_cheese_sliced", 4)}, 16, 16));
			tRows.add(new Row(":132", Target.SHREDDER,
					new ItemLeg[] {ItemLeg.ofFood("gt6:food_cheese", 1)}, new FluidLeg[0], new FluidLeg[0],
					new ItemLeg[] {ItemLeg.of(OP.dust, MT.Cheese, 1)}, 16, 16));
			tRows.add(new Row(":133", Target.MORTAR,
					new ItemLeg[] {ItemLeg.ofFood("gt6:food_cheese", 1)}, new FluidLeg[0], new FluidLeg[0],
					new ItemLeg[] {ItemLeg.of(OP.dustSmall, MT.Cheese, 2)}, 16, 16));

			// :154-157 — the raisins listener (5 members)
			for (String tRaisin : RAISIN_MEMBERS) {
				tRows.add(new Row(":155[" + tail(tRaisin) + "]", Target.BATH,
						new ItemLeg[] {ItemLeg.ofFood(tRaisin, 1)},
						new FluidLeg[] {new FluidLeg("chocolate_molten", 250)}, new FluidLeg[0],
						new ItemLeg[] {ItemLeg.ofFood(CHOCOLATE_RAISIN_MEMBER, 1)}, 0, 16));
				tRows.add(new Row(":156[" + tail(tRaisin) + "]", Target.MIXER,
						new ItemLeg[] {ItemLeg.ofFood("gt6:food_ice_cream", 1), ItemLeg.ofFood(tRaisin, 1)}, new FluidLeg[0], new FluidLeg[0],
						new ItemLeg[] {ItemLeg.ofFood("gt6:food_ice_cream_raisin", 1)}, 16, 16));
			}

			// :253-260 — the TwoNames mixer pairs (the :254/:258 dedup gate verbatim: only the plain sugary dough is an input)
			for (String tRaisin : RAISIN_MEMBERS) {
				tRows.add(new Row(":255[" + tail(tRaisin) + "]", Target.MIXER,
						new ItemLeg[] {ItemLeg.ofFood("gt6:food_dough_sugar", 1), ItemLeg.ofFood(tRaisin, 1)}, new FluidLeg[0], new FluidLeg[0],
						new ItemLeg[] {ItemLeg.ofFood("gt6:food_dough_sugar_raisins", 1)}, 16, 16));
			}
			tRows.add(new Row(":259", Target.MIXER,
					new ItemLeg[] {ItemLeg.ofFood("gt6:food_dough_sugar", 1), ItemLeg.ofFood(CHOCOLATE_RAISIN_MEMBER, 1)}, new FluidLeg[0], new FluidLeg[0],
					new ItemLeg[] {ItemLeg.ofFood("gt6:food_dough_sugar_chocolate_raisins", 1)}, 16, 16));

			// :295-298 — the foodBoiledegg Slicer band
			for (String tBoiled : BOILED_EGG_MEMBERS) {
				tRows.add(new Row(":297[" + tail(tBoiled) + "]", Target.SLICER,
						new ItemLeg[] {ItemLeg.ofFood(tBoiled, 1), ItemLeg.ofBlade()}, new FluidLeg[0], new FluidLeg[0],
						new ItemLeg[] {ItemLeg.ofFood("gt6:food_egg_sliced", 4)}, 16, 16));
			}

			// :303-315 — the OD.itemEgg machine band (the tAmount=4 Birb face TRUE NEGATIVE; the Birb generify :316 likewise)
			for (EggMember tEgg : EGG_MEMBERS) {
				tRows.add(new Row(":313[" + tail(tEgg.itemId()) + "]", Target.AUTOCLAVE,
						new ItemLeg[] {ItemLeg.ofFood(tEgg.itemId(), 1), ItemLeg.ofSelector(0)},
						new FluidLeg[] {new FluidLeg("steam", 800 * tEgg.amount())},
						new FluidLeg[] {new FluidLeg("distilled_water", 5 * tEgg.amount())},
						new ItemLeg[] {ItemLeg.ofFood(tEgg.boiledId(), tEgg.amount()), ItemLeg.of(() -> Items.BONE_MEAL, tEgg.amount())}, 0, 128));
				tRows.add(new Row(":314[" + tail(tEgg.itemId()) + "]", Target.JUICER,
						new ItemLeg[] {ItemLeg.ofFood(tEgg.itemId(), 1)}, new FluidLeg[0], new FluidLeg[0],
						new ItemLeg[] {ItemLeg.ofFood("gt6:food_egg_white", tEgg.amount()), ItemLeg.ofFood("gt6:food_egg_yolk", tEgg.amount()), ItemLeg.of(() -> Items.BONE_MEAL, tEgg.amount())}, 16, 16));
				tRows.add(new Row(":315[" + tail(tEgg.itemId()) + "]", Target.CENTRIFUGE,
						new ItemLeg[] {ItemLeg.ofFood(tEgg.itemId(), 1)}, new FluidLeg[0], new FluidLeg[0],
						new ItemLeg[] {ItemLeg.ofFood("gt6:food_egg_white", tEgg.amount()), ItemLeg.ofFood("gt6:food_egg_yolk", tEgg.amount()), ItemLeg.of(() -> Items.BONE_MEAL, tEgg.amount())}, 16, 64));
			}

			// :319-323 — the scrambled-egg dough walk over ANY.Flour
			for (OreDictMaterial tMat : ANY.Flour.mToThis) {
				tRows.add(new Row(":322[" + mNameOrEmpty(tMat) + "]", Target.MIXER,
						new ItemLeg[] {ItemLeg.ofFood("gt6:food_egg_scrambled", 1), ItemLeg.of(OP.dust, tMat, 1)}, new FluidLeg[0], new FluidLeg[0],
						new ItemLeg[] {ItemLeg.ofFood("gt6:food_dough_egg", 1)}, 16, 16));
			}

			// :390-398 — the chum quartet (both legs declared-null, see SKIPPED_UPSTREAM)
			for (ChumRow tChum : chumRows()) {
				tRows.add(new Row(tChum.note(), Target.MIXER,
						new ItemLeg[] {ItemLeg.ofFood(SCRAP_MEAT_INPUT, 1), ItemLeg.of(() -> Items.FERMENTED_SPIDER_EYE, 1),
								ItemLeg.of(OP.dust, tChum.rotten(), 1), ItemLeg.of(OP.dust, MT.Bone, 1),
								ItemLeg.of(() -> Items.POISONOUS_POTATO, 1), ItemLeg.of(() -> Items.RED_MUSHROOM, 1)},
						new FluidLeg[] {new FluidLeg("purpledrink", 1000), new FluidLeg(tChum.oilName(), tChum.oilAmount())},
						new FluidLeg[] {new FluidLeg(SLUDGE_NULL, 1000)},
						new ItemLeg[] {ItemLeg.ofFood("gt6:food_chum", 8)}, 16, 256));
			}

			// :400-414 — the fish-oil Squeezer/Juicer pair (the T4b leftover ①; :405-:410 walk columns)
			for (FishOilMember tFish : FISH_OIL_MEMBERS) {
				tRows.add(new Row(":412[" + tail(tFish.itemId()) + "]", Target.SQUEEZER,
						new ItemLeg[] {ItemLeg.ofFood(tFish.itemId(), 1)}, new FluidLeg[0],
						new FluidLeg[] {new FluidLeg("fishoil", (int)(tFish.oilAmount() * 1000 / CS.U))},
						new ItemLeg[] {dustOut(tFish.byProductAmount(), MT.FishRaw)}, 16, 32));
				tRows.add(new Row(":413[" + tail(tFish.itemId()) + "]", Target.JUICER,
						new ItemLeg[] {ItemLeg.ofFood(tFish.itemId(), 1)}, new FluidLeg[0],
						new FluidLeg[] {new FluidLeg("fishoil", (int)(tFish.oilAmount() * 1000 / CS.U / 2))},
						new ItemLeg[] {dustOut(tFish.byProductAmount(), MT.FishRaw)}, 16, 32));
			}

			// :517-520 — the mushroom soup (brown guard-excluded from the input side, :518)
			tRows.add(new Row(":519", Target.MIXER,
					new ItemLeg[] {ItemLeg.of(() -> Items.RED_MUSHROOM, 1), ItemLeg.of(() -> Items.BROWN_MUSHROOM, 1)}, new FluidLeg[0],
					new FluidLeg[] {new FluidLeg("soup_mushroom", 1000)},
					new ItemLeg[0], 16, 16));

			// :552 / :555 — the fluid singletons
			tRows.add(new Row(":552", Target.MIXER,
					new ItemLeg[0],
					new FluidLeg[] {new FluidLeg("royal_jelly", 100), new FluidLeg("honeydew", 200)},
					new FluidLeg[] {new FluidLeg("ambrosia", 400)},
					new ItemLeg[0], 16, 16));
			tRows.add(new Row(":555", Target.MIXER,
					new ItemLeg[] {ItemLeg.of(OP.dust, MT.Milk, 1)},
					new FluidLeg[] {new FluidLeg("soda", 500)},
					new FluidLeg[] {new FluidLeg("spoiledmilk", 1000)},
					new ItemLeg[0], 16, 16));

			// :719-724 — the meat-ingot walk over ANY.FlourGrains
			for (OreDictMaterial tMat : ANY.FlourGrains.mToThis) {
				tRows.add(new Row(":720[" + mNameOrEmpty(tMat) + "]", Target.MIXER,
						new ItemLeg[] {ItemLeg.of(OP.dust, tMat, 1), ItemLeg.of(OP.dust, MT.MeatRaw, 1)}, new FluidLeg[0], new FluidLeg[0],
						new ItemLeg[] {ItemLeg.of(OP.ingot, MT.MeatRaw, 1)}, 16, 16));
				tRows.add(new Row(":721[" + mNameOrEmpty(tMat) + "]", Target.MIXER,
						new ItemLeg[] {ItemLeg.of(OP.dust, tMat, 1), ItemLeg.of(OP.dust, MT.FishRaw, 1)}, new FluidLeg[0], new FluidLeg[0],
						new ItemLeg[] {ItemLeg.of(OP.ingot, MT.FishRaw, 1)}, 16, 16));
				tRows.add(new Row(":722[" + mNameOrEmpty(tMat) + "]", Target.MIXER,
						new ItemLeg[] {ItemLeg.of(OP.dust, tMat, 1), ItemLeg.of(OP.dust, MT.MeatRotten, 1)}, new FluidLeg[0], new FluidLeg[0],
						new ItemLeg[] {ItemLeg.of(OP.ingot, MT.MeatRotten, 1)}, 16, 16));
				tRows.add(new Row(":723[" + mNameOrEmpty(tMat) + "]", Target.MIXER,
						new ItemLeg[] {ItemLeg.of(OP.dust, tMat, 1), ItemLeg.of(OP.dust, MT.FishRotten, 1)}, new FluidLeg[0], new FluidLeg[0],
						new ItemLeg[] {ItemLeg.of(OP.ingot, MT.FishRotten, 1)}, 16, 16));
			}

			// :725 — the abyssal dough
			tRows.add(new Row(":725", Target.MIXER,
					new ItemLeg[] {ItemLeg.of(OP.dust, MT.OatAbyssal, 1)},
					new FluidLeg[] {new FluidLeg("hellderberryjuice", 100)}, new FluidLeg[0],
					new ItemLeg[] {ItemLeg.ofFood("gt6:food_dough_abyssal", 1)}, 16, 16));

			// :733-737 — the chocolate dust ladder (:734/:736 DROPPED, see SKIPPED_UPSTREAM)
			tRows.add(new Row(":733", Target.MIXER,
					new ItemLeg[] {ItemLeg.of(OP.dust, MT.Sugar, 1), ItemLeg.of(OP.dust, MT.Cocoa, 1)}, new FluidLeg[0], new FluidLeg[0],
					new ItemLeg[] {ItemLeg.of(OP.dust, MT.Chocolate, 2)}, 16, 16));
			tRows.add(new Row(":735", Target.MIXER,
					new ItemLeg[] {ItemLeg.of(OP.dustSmall, MT.Sugar, 1), ItemLeg.of(OP.dustSmall, MT.Cocoa, 1)}, new FluidLeg[0], new FluidLeg[0],
					new ItemLeg[] {ItemLeg.of(OP.dustSmall, MT.Chocolate, 2)}, 16, 16));
			tRows.add(new Row(":737", Target.MIXER,
					new ItemLeg[] {ItemLeg.of(OP.dustTiny, MT.Sugar, 1), ItemLeg.of(OP.dustTiny, MT.Cocoa, 1)}, new FluidLeg[0], new FluidLeg[0],
					new ItemLeg[] {ItemLeg.of(OP.dustTiny, MT.Chocolate, 2)}, 16, 16));

			sTable = tTable = List.copyOf(tRows);
		}
		return tTable;
	}

	/** The :410-:412 byproduct-dust output face — the OM.dust ladder (the meat-card transcription, reused). */
	private static ItemLeg dustOut(long aMaterialAmount, OreDictMaterial aMeat) {
		GT6RecipesMeat.DustOut tDust = GT6RecipesMeat.dustLadder(aMaterialAmount);
		return ItemLeg.of(tDust.prefix(), aMeat, tDust.count());
	}

	/** The member-note tail (id after the colon). */
	private static String tail(String aId) {return aId.substring(aId.indexOf(':') + 1);}

	/** The material name for a walk-row note. */
	private static String mNameOrEmpty(OreDictMaterial aMat) {return aMat == null ? "" : aMat.mNameInternal;}

	// ------------------------------------------------------------------ the pour

	/** Poured flag — one generation, one pour (upstream loaders run once per JVM). */
	private static boolean sLoaded = false;
	/** The reconciliation counters of the last {@link #load()} (the offline audit face). */
	private static final int[] sPoured = new int[Target.values().length], sSkipped = new int[Target.values().length];

	// ADR-P18: the pour-flag joins the GT6RecipeMaps generation (the bake form).
	static {GT6RecipeMaps.registerGenerationResetHook(GT6RecipesFoodTail::resetForTest);}

	/** FMLCommonSetup.enqueueWork — items, fluids and the maps have fired by this point. */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(GT6RecipesFoodTail::load);
	}

	/**
	 * Pours the transcribed band into its nine maps. Idempotent; an unresolvable row skips
	 * with a count (the upstream silent-drop semantics — today: the four chum rows ride
	 * their declared-null faces, everything else resolves live).
	 */
	public static synchronized void load() {
		if (sLoaded) {LOGGER.debug("load() skipped: already poured (generation flag set)"); return;}
		GT6RecipeMaps.init(); // defensive + idempotent: the maps exist from ConstructMod, tests may race it
		for (Target tTarget : Target.values()) if (recipeMap(tTarget) == null) return; // reset() between init and load — a broken lifecycle
		int tPoured = 0, tSkipped = 0;
		for (Row tRow : table()) {
			Recipe tRecipe = build(tRow);
			if (tRecipe == null) {sSkipped[tRow.target().ordinal()]++; tSkipped++; continue;} // = upstream mat()/FL.exists silent drop
			recipeMap(tRow.target()).addRecipe(tRecipe);
			sPoured[tRow.target().ordinal()]++; tPoured++;
		}
		sLoaded = true;
		LOGGER.info("GT6 food tail poured: {} rows loaded, {} skipped (the declared chum faces + unregistered walk members), = upstream silent drops", tPoured, tSkipped);
	}

	/** The map of one band target (post-{@link GT6RecipeMaps#init()}). */
	static RecipeMap recipeMap(Target aTarget) {
		return switch (aTarget) {
			case SHREDDER   -> GT6RecipeMaps.SHREDDER;
			case MORTAR     -> GT6RecipeMaps.MORTAR;
			case SLICER     -> GT6RecipeMaps.SLICER;
			case AUTOCLAVE  -> GT6RecipeMaps.AUTOCLAVE;
			case JUICER     -> GT6RecipeMaps.JUICER;
			case CENTRIFUGE -> GT6RecipeMaps.CENTRIFUGE;
			case MIXER      -> GT6RecipeMaps.MIXER;
			case SQUEEZER   -> GT6RecipeMaps.SQUEEZER;
			case BATH       -> GT6RecipeMaps.BATH;
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
				case ItemLeg.FOOD -> {
					Item tItem = sFoodItemResolver.apply(tLeg.foodId());
					if (tItem == null) return null;
					tStack = new ItemStack(tItem, tLeg.count());
				}
				case ItemLeg.MATERIAL -> {
					Item tItem = sMaterialItemResolver.apply(tLeg.prefix(), tLeg.material());
					if (tItem == null) return null;
					tStack = new ItemStack(tItem, tLeg.count());
				}
				case ItemLeg.BLADE -> {
					Item tItem = sBladeResolver.get();
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

	private GT6RecipesFoodTail() {}
}
