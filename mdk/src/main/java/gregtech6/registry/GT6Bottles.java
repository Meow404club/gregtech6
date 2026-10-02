package gregtech6.registry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * The GT6 bottles registration home — task food-bottles-min landed the MINIMUM 4-bottle
 * subset, task btl-bottles-families-a extends the table with the non-dye batch A: 71 rows
 * (75 total) covering the water family, the alcohol families, the honey/potion segment
 * and the ink/indigo/tail rows of the upstream {@code MultiItemBottles} domain (441
 * lines, ~152 bottles + the 48 dye bottles — the batch B cards). Every {@link #ROWS}
 * entry is one upstream registration row ({@code addItem(meta, name, ...)}), order = the
 * empty bottle first, then the upstream meta ascending.
 *
 * <p>Row id convention (the food-domain flattening snake, the
 * LoaderOreDictReRegistrations.java:870 re-registration-identity precedent): (1) rows the
 * upstream RE-REGISTERS to a {@code food*} name ride the final re-reg name — meta 1300
 * {@code bottleHoney -> foodHoneydrop} (:869) becomes {@code food_honeydrop}, like the
 * landed {@code food_heavycream}; (2) rows whose upstream identity is a bottle-prefixed
 * anchor (the {@code IL.Bottle_*} seat or an {@code OP.bottle.dat(MT.X)} material-prefix
 * container) snake the anchor — {@code bottle_holy_water}, {@code bottle_ink},
 * {@code bottle_indigo}, {@code bottle_loot}, {@code bottle_poison}, {@code bottle_tar},
 * {@code bottle_blood}, {@code bottle_lubricant}, {@code bottle_mercury},
 * {@code bottle_glue}, {@code bottle_honeydew}; (3) every other row (no own oredict
 * identity — the water family, or a category-only oredict like {@code foodGrapejuice}
 * shared by four metas) rides the port fluid id of its content ({@code mnwtr},
 * {@code soda}, {@code grc_grapewine0}, {@code potion.goldenapplejuice}, ...) verbatim —
 * one bottle id per content fluid, zero collisions.
 *
 * <p>DECLARED DEVIATION (the main-session ruling 2026-10-02, the small case b, extended
 * to the whole table): upstream these are fluid-container items (the FoodStatFluid 250mB
 * drink face, IS_CONTAINER + SELF_REFERENCING, OP.java:229) — that would be the port's
 * FIRST capability container. This card registers them as PLAIN items; the drink/fill
 * capability channel, the empty-bottle container-item return (:418-421) and the
 * IItemRottable rot chain (:428-438: 0-4 -> 5 dirty water, 1100/1200 -> 1102 spoiled
 * milk, else <30000 -> 1601 rotten drink) defer to the independent content decision card.
 *
 * <p>DECLARED GATES (the census rulings, state research.bottles-census): the FR honey
 * drop legs (:169-174) = PRIMARY ABSENT(Forestry) TRUE NEGATIVE — the honey/mead bottles
 * 1300-1305 register as GT6-own; the TiC legs OD.itemTar (:378) / OD.itemBlood (:388) and
 * the HBM leg IL.HBM_Mercury_Drop (:405) = PRIMARY ABSENT gates — the tail bottles
 * 32762/32763/32765 register as GT6-own; Behavior_CureZombie (1700, :188) and
 * Behavior_Drop_Loot (32761, :368) = behavior faces, deferred to the behavior card. The
 * upstream bucket-split crafting bands (:129-132/:143-146/:151-154/:164-167/:360-365/
 * :373-378/:383-388/:393-396/:401-405/:410-413) need the container1000/500 oredict ITEM
 * faces the port does not have — SKIP-FAITHFUL (no rows written), not folded.
 *
 * <p>The stack column rides the upstream getDefaultStackLimit split (:423-426): meta
 * {@code >= 32000} stacks 64, everything else stacks {@code OP.bottle.mDefaultStackSize}
 * = 16 (OP.java:229 {@code .setStacksize(16)}) — the four food-bottles-min rows were
 * 64-stack before this column existed; 16 is the faithful value.
 *
 * <p>The "GregTech: Bottles" tab (id {@code gt6:bottles}, MultiItemBottles.java:40) — the
 * icon is meta 1600 "Purple Drink" verbatim; the food-bottles-min empty-bottle icon was
 * the declared deviation and is RECLAIMED here now that 1600 is in the table. The
 * upstream TD.Creative.HIDDEN rows (6/7/8, 203/204, 302, 402, 502, 602, 1400/1401,
 * 1500/1501, 1601) register but do NOT display in the tab.
 *
 * <p>Task btl-bottles-families-b extends the table with the families-B batch: 96 rows
 * (171 total) — the kitchen segment (the sauce family 800-804, the apple rows 900-905,
 * the cooking-oil bottles 1000-1007, mayo/dressing 1020/1021, the milk family
 * 1100/1102, soy milk 1200, the chocolate creams 1900/1901, maple 3500/3501, peanut
 * butter 3601) and the smoothie walk 2000-5705 minus the landed ketchup 3101. The
 * HIDDEN set grows by 802/803/804 (the Diabolo/Diablo/Cow sauces), 1102 (the spoiled
 * milk), 3200 (the golden carrot juice) and 3700 (the rainbow sap). The smoothie walk
 * row count is the walk's ACTUAL output (69 rows, MultiItemBottles.java:197-:341) — the
 * census "~46" headline was an undercount, the row list is the authority (the
 * food-bottles-min 7-vs-8 counting precedent). The oil-family texture source stayed the
 * folder sprites (gt.multiitem.bottles/<meta>.png — gregapi MultiItemRandom.java:366
 * registers the folder icon unconditionally, the census "material prefix texture family"
 * hypothesis refuted; the assets/README.md families-B ledger carries the note).
 *
 * <p>KJS surface: REGISTRATION face only, deferred to the KJS binding card (the
 * GT6CrystalChargers.java:44 declaration form); the item models + textures are
 * datapack-domain, naturally moddable.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Bottles {

	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");
	public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "gt6");

	/** The tab title lang key — the single source both the builder and the GT6EnUs datagen row use. */
	public static final String TAB_TITLE_KEY = "itemGroup.gt6.bottles";

	/** The tab icon item id — meta 1600 "Purple Drink" (MultiItemBottles.java:40, the reclaimed deviation). */
	public static final String TAB_ICON_ID = "purpledrink";

	/**
	 * One upstream registration row: the 1.7.10 meta (negative = the OP.bottle.dat(MT.Empty)
	 * domain-entry item, which has no MultiItemBottles meta), the port id, the upstream
	 * registration-row name verbatim, the desc tooltip verbatim ({@code ""} = no tooltip
	 * key on either locale, the GT6BakeFoods empty-desc ruling), the TD.Creative.HIDDEN
	 * flag and the getDefaultStackLimit split value (:423-426).
	 */
	public record BottleRow(int meta, String id, String enName, String enTooltip, boolean hidden, int stackSize) {

		/** The item lang key. */
		public String langKey() {
			return "item.gt6." + id;
		}

		/** The desc tooltip lang key, null on the {@code ""} rows (the raw-key guard). */
		public String tooltipKey() {
			return enTooltip.isEmpty() ? null : "item.gt6." + id + ".tooltip";
		}

		/** The texture basename under item/bottle/ (the food_ prefix strips, the ketchup.png precedent). */
		public String texture() {
			return id.startsWith("food_") ? id.substring("food_".length()) : id;
		}
	}

	/** The empty-bottle row (the domain-entry item, no upstream meta). */
	public static final BottleRow EMPTY_ROW = new BottleRow(-1, "bottle_empty", "Empty Bottle", "", false, 16);

	/** The Tomato Ketchup row — meta 3101, MultiItemBottles.java:257 (oredict foodKetchup). */
	public static final BottleRow KETCHUP_ROW = new BottleRow(3101, "food_ketchup", "Tomato Ketchup", "", false, 16);

	/** The Barbecue Sauce row — meta 805, MultiItemBottles.java:111 (oredict foodBarbecuesauce). */
	public static final BottleRow BBQ_ROW = new BottleRow(805, "food_barbecuesauce", "Barbecue Sauce", "", false, 16);

	/**
	 * The Heavy Cream row — meta 1101, MultiItemBottles.java:139 (oredict bottleCream,
	 * re-registered to foodHeavycream by LoaderOreDictReRegistrations.java:870 — the port
	 * id IS the final crafting-consumable name).
	 */
	public static final BottleRow CREAM_ROW = new BottleRow(1101, "food_heavycream", "Heavy Cream", "", false, 16);

	/**
	 * The families-A rows, upstream meta order (MultiItemBottles.java:45-:408; the line
	 * anchor lives in the per-row census test and the assets/README.md sha256 ledger).
	 * The 64-stack rows start at meta 32000 (:425); the 14 HIDDEN rows ride
	 * TD.Creative.HIDDEN.
	 */
	public static final List<BottleRow> FAMILY_A_ROWS = List.of(
			new BottleRow(    0, "mnwtr"                    , "Mineral Water"               , "", false, 16), // :45 FL.MnWtr
			new BottleRow(    1, "seawater"                 , "Sea Water"                   , "", false, 16), // :46 FL.Ocean (the GrC/Tropics alias legs = PRIMARY ABSENT gates, census)
			new BottleRow(    2, "soda"                     , "Soda"                        , "", false, 16), // :47 oredict foodBubblywater
			new BottleRow(    3, "mineralsoda"              , "Mineral Soda"                , "", false, 16), // :48
			new BottleRow(    4, "ice"                      , "Ice Water"                   , "", false, 16), // :49 FL.Ice
			new BottleRow(    5, "waterdirty"               , "Dirty Water"                 , "", false, 16), // :50
			new BottleRow(    6, "swampwater"               , "Swampwater"                  , "", true , 16), // :51 HIDDEN
			new BottleRow(    7, "stagnantwater"            , "Stagnant Water"              , "", true , 16), // :52 HIDDEN
			new BottleRow(    8, "spdew"                    , "Spectral Dew"                , "", true , 16), // :53 oredict listAllwater, HIDDEN
			new BottleRow(  100, "juicewhitegrape"          , "Grape Juice"                 , "", false, 16), // :56 Juice_Grape_White
			new BottleRow(  101, "winewhite"                , "White Wine"                  , "", false, 16), // :57
			new BottleRow(  102, "vinegar"                  , "Grape Vinegar"               , "", false, 16), // :58 oredict foodVinegar
			new BottleRow(  103, "juiceredgrape"            , "Grape Juice"                 , "", false, 16), // :59
			new BottleRow(  104, "winered"                  , "Red Wine"                    , "", false, 16), // :60
			new BottleRow(  105, "whitegrapesmoothie"       , "Grape Smoothie"              , "", false, 16), // :61
			new BottleRow(  106, "redgrapesmoothie"         , "Grape Smoothie"              , "", false, 16), // :62
			new BottleRow(  107, "grapesmoothie"            , "Grape Smoothie"              , "", false, 16), // :63
			new BottleRow(  108, "purplegrapesmoothie"      , "Grape Smoothie"              , "", false, 16), // :64
			new BottleRow(  109, "grapejuice"               , "Grape Juice"                 , "", false, 16), // :65
			new BottleRow(  110, "grc_grapewine0"           , "Grape Juice"                 , "", false, 16), // :66 Juice_Grape_Purple = the grc.grapewine0 fluid carrier
			new BottleRow(  111, "wine"                     , "Wine"                        , "", false, 16), // :67
			new BottleRow(  112, "ricardosanchez"           , "Ricardo Sanchez"             , "", false, 16), // :68
			new BottleRow(  200, "juicelemon"               , "Lemon Juice"                 , "", false, 16), // :71
			new BottleRow(  201, "lemonade"                 , "Lemonade"                    , "", false, 16), // :72
			new BottleRow(  202, "limoncello"               , "Limoncello"                  , "", false, 16), // :73
			new BottleRow(  203, "alcopops"                 , "Alcopops"                    , "", true , 16), // :74 HIDDEN
			new BottleRow(  204, "cavejohnsonsgrenadejuice" , "Cave Johnson's Grenade Juice", "", true , 16), // :75 HIDDEN
			new BottleRow(  205, "lemonsmoothie"            , "Lemon Smoothie"              , "", false, 16), // :76
			new BottleRow(  300, "potatojuice"              , "Potato Juice"                , "", false, 16), // :79
			new BottleRow(  301, "vodka"                    , "Vodka"                       , "", false, 16), // :80
			new BottleRow(  302, "leninade"                 , "Leninade"                    , "", true , 16), // :81 HIDDEN
			new BottleRow(  400, "reedwater"                , "Reedwater"                   , "", false, 16), // :84
			new BottleRow(  401, "rumwhite"                 , "Rum"                         , "", false, 16), // :85
			new BottleRow(  402, "rumdark"                  , "Pirate Brew"                 , "", true , 16), // :86 HIDDEN
			new BottleRow(  403, "canevinegar"              , "Cane Vinegar"                , "", false, 16), // :87
			new BottleRow(  404, "pina_colada"              , "Piña Colada"                 , "", false, 16), // :88
			new BottleRow(  500, "hopsmash"                 , "Hops Mash"                   , "", false, 16), // :91
			new BottleRow(  501, "darkbeer"                 , "Dark Beer"                   , "", false, 16), // :92
			new BottleRow(  502, "dragonblood"              , "Dragon Blood"                , "", true , 16), // :93 HIDDEN
			new BottleRow(  600, "mashwheat"                , "Wheaty Mash"                 , "", false, 16), // :96
			new BottleRow(  601, "whiskeywheat"             , "Scotch"                      , "", false, 16), // :97
			new BottleRow(  602, "glenmckenner"             , "Glen McKenner"               , "", true , 16), // :98 HIDDEN
			new BottleRow(  700, "wheathopsmash"            , "Wheaty Hops Mash"            , "", false, 16), // :101
			new BottleRow(  701, "beer"                     , "Beer"                        , "", false, 16), // :103 IL.Bottle_Beer
			new BottleRow( 1300, "food_honeydrop"           , "Honey"                       , "Why does this Bottle look like a Bear and not a Bee?", false, 16), // :157 re-reg bottleHoney->foodHoneydrop (:869); FR drop legs :169-174 = ABSENT(Forestry) gate
			new BottleRow( 1301, "bottle_honeydew"          , "Honeydew"                    , "", false, 16), // :158 OP.bottle.dat(MT.Honeydew)
			new BottleRow( 1302, "royal_jelly"              , "Royal Jelly"                 , "", false, 16), // :159
			new BottleRow( 1303, "ambrosia"                 , "Ambrosia"                    , "", false, 16), // :160
			new BottleRow( 1304, "short_mead"               , "Short Mead"                  , "", false, 16), // :161
			new BottleRow( 1305, "mead"                     , "Mead"                        , "", false, 16), // :162
			new BottleRow( 1400, "potion.goldenapplejuice"  , "Golden Apple Juice"          , "", true , 16), // :176 HIDDEN
			new BottleRow( 1401, "potion.goldencider"       , "Golden Cider"                , "", true , 16), // :177 HIDDEN
			new BottleRow( 1500, "potion.idunsapplejuice"   , "Idun's Apple Juice"          , "", true , 16), // :179 HIDDEN
			new BottleRow( 1501, "potion.notchesbrew"       , "Notches Brew"                , "", true , 16), // :180 HIDDEN
			new BottleRow( 1600, "purpledrink"              , "Purple Drink"                , "", false, 16), // :183 IL.Bottle_Purple_Drink — the TAB ICON
			new BottleRow( 1601, "rottendrink"              , "Rotten Drink"                , "", true , 16), // :185 HIDDEN — the rot-chain sink (:436, deferred)
			new BottleRow( 1700, "bottle_holy_water"        , "Holy Water"                  , "", false, 16), // :188 OP.bottle.dat(MT.HolyWater); Behavior_CureZombie = behavior card defer
			new BottleRow( 1800, "ricewater"                , "Rice Water"                  , "", false, 16), // :190 FL.Mash_Rice
			new BottleRow( 1801, "sake"                     , "Sake"                        , "", false, 16), // :191
			new BottleRow( 1802, "ricevinegar"              , "Rice Vinegar"                , "", false, 16), // :192
			new BottleRow(30000, "medicine.heal"            , "Medicine"                    , "", false, 16), // :342 FL.Med_Heal
			new BottleRow(30001, "medicine.laxative"        , "Laxative"                    , "", false, 16), // :343
			new BottleRow(32000, "bottle_ink"               , "Ink Bottle"                  , "Color: Black", false, 64), // :346 IL.Bottle_Ink — the stack split starts HERE (:425)
			new BottleRow(32001, "bottle_indigo"            , "Bottled Indigo Dye"          , "Color: Blue" , false, 64), // :349 IL.Bottle_Indigo
			new BottleRow(32760, "bottle_poison"            , "Bottle of Poison"           , "", false, 64), // :358 IL.Bottle_Poison
			new BottleRow(32761, "bottle_loot"              , "Clouded Bottle"              , "Loot: A random Bottle", false, 64), // :368 IL.Bottle_Loot; Behavior_Drop_Loot = behavior card defer
			new BottleRow(32762, "bottle_tar"               , "Tar Bottle"                  , "Can be used as Glue too", false, 64), // :371 IL.Bottle_Tar; OD.itemTar leg = ABSENT(TiC) gate; the tar fluid rides the btl-fluids-prereq branch
			new BottleRow(32763, "bottle_blood"             , "Bottle o'Blood"              , "", false, 64), // :381 IL.Bottle_Blood; OD.itemBlood leg = ABSENT(TiC) gate
			new BottleRow(32764, "bottle_lubricant"         , "Lubricant Bottle"            , "", false, 64), // :391 IL.Bottle_Lubricant; FL.LubRoCant leg = ABSENT(RC) gate
			new BottleRow(32765, "bottle_mercury"           , "Mercury Bottle"              , "Also called Quicksilver", false, 64), // :399 IL.Bottle_Mercury; HBM drop leg :405 = ABSENT(HBM) gate
			new BottleRow(32766, "bottle_glue"              , "Glue Bottle"                 , "", false, 64)); // :408 IL.Bottle_Glue

	/**
	 * The families-B rows, upstream meta order (MultiItemBottles.java:106-:341; the line
	 * anchor lives in the per-row census test and the assets/README.md sha256 ledger).
	 * The landed seats (BBQ 805, cream 1101, ketchup 3101) are NOT repeated here — they
	 * splice into {@link #ROWS} at their families-a positions. All 96 rows sit below the
	 * meta 32000 stack split, so the column is 16 across the batch. The fluid-carrier id
	 * rows ride the port GTFluids FOOD_B1/FOOD_B2 ids verbatim; the OP.bottle.dat(MT.X)
	 * material containers (the cooking oils, the milk bottle) and the IL.Bottle_* seats
	 * with bottle names (the slime bottles) snake the anchor; HIDDEN adds 802/803/804,
	 * 1102, 3200 and 3700.
	 */
	public static final List<BottleRow> FAMILY_B_ROWS = List.of(
			new BottleRow(  800, "chillysauce"         , "Chili Sauce"             , "", false, 16), // :106 FL.Sauce_Chili
			new BottleRow(  801, "hotsauce"            , "Hot Sauce"               , "", false, 16), // :107 FL.Sauce_Hot
			new BottleRow(  802, "diabolosauce"        , "Diabolo Sauce"           , "", true , 16), // :108 HIDDEN
			new BottleRow(  803, "diablosauce"         , "Diablo Sauce"            , "", true , 16), // :109 HIDDEN
			new BottleRow(  804, "diablosauce_strong"  , "There is no Cow Sauce"   , "", true , 16), // :110 HIDDEN FL.Sauce_Cow_Level
			new BottleRow(  900, "juiceapple"          , "Apple Juice"             , "", false, 16), // :114 FL.Juice_Apple
			new BottleRow(  901, "ciderapple"          , "Cider"                   , "", false, 16), // :115 FL.Cider_Apple
			new BottleRow(  902, "applevinegar"        , "Apple Cider Vinegar"     , "", false, 16), // :116 FL.Vinegar_Apple
			new BottleRow(  905, "applesmoothie"       , "Apple Smoothie"          , "", false, 16), // :117 FL.Smoothie_Apple
			new BottleRow( 1000, "bottle_olive_oil"    , "Olive Oil"               , "Cooking Oil", false, 16), // :120 OP.bottle.dat(MT.OliveOil); FL.Oil_Olive = the juiceolive fluid carrier
			new BottleRow( 1001, "bottle_sunflower_oil", "Sunflower Oil"           , "Cooking Oil", false, 16), // :121 OP.bottle.dat(MT.SunflowerOil)
			new BottleRow( 1002, "bottle_nut_oil"      , "Nut Oil"                 , "Cooking Oil", false, 16), // :122 OP.bottle.dat(MT.NutOil)
			new BottleRow( 1003, "bottle_seed_oil"     , "Seed Oil"                , "Cooking Oil", false, 16), // :123 OP.bottle.dat(MT.SeedOil)
			new BottleRow( 1004, "bottle_hemp_oil"     , "Hemp Oil"                , "Cooking Oil", false, 16), // :124 OP.bottle.dat(MT.HempOil)
			new BottleRow( 1005, "bottle_lin_oil"      , "Lin Oil"                 , "Cooking Oil", false, 16), // :125 OP.bottle.dat(MT.LinOil)
			new BottleRow( 1006, "bottle_fish_oil"     , "Fish Oil"                , "Cooking Oil", false, 16), // :126 OP.bottle.dat(MT.FishOil)
			new BottleRow( 1007, "bottle_whale_oil"    , "Whale Oil"               , "Cooking Oil", false, 16), // :127 OP.bottle.dat(MT.WhaleOil)
			new BottleRow( 1020, "mayo"                , "Mayo"                    , "", false, 16), // :134 FL.Mayo
			new BottleRow( 1021, "dressing"            , "Dressing"                , "", false, 16), // :135 FL.Dressing
			new BottleRow( 1100, "bottle_milk"         , "Milk"                    , "", false, 16), // :138 OP.bottle.dat(MT.Milk), IL.Bottle_Milk
			new BottleRow( 1102, "spoiledmilk"         , "Milk"                    , "", true , 16), // :141 HIDDEN IL.Bottle_Milk_Spoiled, FL.Milk_Spoiled
			new BottleRow( 1200, "soymilk"             , "Soy Milk"                , "", false, 16), // :149 FL.MilkSoy, IL.Bottle_Milk_Soy
			new BottleRow( 1900, "chocolatecream"      , "Chocolate Cream"         , "", false, 16), // :194 FL.Cream_Chocolate
			new BottleRow( 1901, "nutella"             , "Nutella"                 , "", false, 16), // :195 FL.Cream_Nutella
			new BottleRow( 2000, "strawberryjuice"     , "Strawberry Juice"        , "", false, 16), // :197 FL.Juice_Strawberry
			new BottleRow( 2005, "strawberrysmoothie"  , "Strawberry Smoothie"     , "", false, 16), // :198 FL.Smoothie_Strawberry
			new BottleRow( 2100, "bananajuice"         , "Banana Juice"            , "", false, 16), // :200 FL.Juice_Banana
			new BottleRow( 2105, "bananasmoothie"      , "Banana Smoothie"         , "", false, 16), // :201 FL.Smoothie_Banana
			new BottleRow( 2200, "bottle_slime_green"  , "Green Slime Bottle"      , "Can be used as Glue too", false, 16), // :204 IL.Bottle_Slime_Green, FL.Slime_Green
			new BottleRow( 2201, "bottle_slime_pink"   , "Pink Slime Bottle"       , "Can be used as Glue too", false, 16), // :206 IL.Bottle_Slime_Pink, FL.Slime_Pink
			new BottleRow( 2202, "bottle_slime_blue"   , "Blue Slime Bottle"       , "Can be used as Glue too", false, 16), // :208 IL.Bottle_Slime_Blue, FL.Slime_Blue
			new BottleRow( 2210, "bawls"               , "BAWLS"                   , "", false, 16), // :209 FL.BAWLS
			new BottleRow( 2300, "melonjuice"          , "Melon Juice"             , "", false, 16), // :226 FL.Juice_Melon
			new BottleRow( 2305, "melonsmoothie"       , "Melon Smoothie"          , "", false, 16), // :227 FL.Smoothie_Melon
			new BottleRow( 2400, "juice_juice"         , "Juice"                   , "", false, 16), // :229 FL.Juice = the juice_juice fluid carrier
			new BottleRow( 2405, "fruitsmoothie"       , "Froot Smoothie"          , "", false, 16), // :230 FL.Smoothie_Fruit
			new BottleRow( 2500, "kiwijuice"           , "Kiwi Juice"              , "", false, 16), // :238 FL.Juice_Kiwi
			new BottleRow( 2505, "kiwismoothie"        , "Kiwi Smoothie"           , "", false, 16), // :239 FL.Smoothie_Kiwi
			new BottleRow( 2600, "raspberryjuice"      , "Raspberry Juice"         , "", false, 16), // :241 FL.Juice_Raspberry
			new BottleRow( 2605, "raspberrysmoothie"   , "Raspberry Smoothie"      , "", false, 16), // :242 FL.Smoothie_Raspberry
			new BottleRow( 2700, "blackberryjuice"     , "Blackberry Juice"        , "", false, 16), // :244 FL.Juice_Blackberry
			new BottleRow( 2705, "blackberrysmoothie"  , "Blackberry Smoothie"     , "", false, 16), // :245 FL.Smoothie_Blackberry
			new BottleRow( 2800, "blueberryjuice"      , "Blueberry Juice"         , "", false, 16), // :247 FL.Juice_Blueberry
			new BottleRow( 2805, "blueberrysmoothie"   , "Blueberry Smoothie"      , "", false, 16), // :248 FL.Smoothie_Blueberry
			new BottleRow( 2900, "cranberryjuice"      , "Cranberry Juice"         , "", false, 16), // :250 FL.Juice_Cranberry
			new BottleRow( 2905, "cranberrysmoothie"   , "Cranberry Smoothie"      , "", false, 16), // :251 FL.Smoothie_Cranberry
			new BottleRow( 3000, "gooseberryjuice"     , "Gooseberry Juice"        , "", false, 16), // :253 FL.Juice_Gooseberry
			new BottleRow( 3005, "gooseberrysmoothie"  , "Gooseberry Smoothie"     , "", false, 16), // :254 FL.Smoothie_Gooseberry
			new BottleRow( 3100, "juicetomato"         , "Tomato Juice"            , "", false, 16), // :256 FL.Juice_Tomato
			new BottleRow( 3200, "goldencarrotjuice"   , "Golden Carrot Juice"     , "", true , 16), // :259 HIDDEN goldencarrotjuice fluid (FOOD_B2)
			new BottleRow( 3300, "juicecarrot"         , "Carrot Juice"            , "", false, 16), // :261 FL.Juice_Carrot
			new BottleRow( 3400, "cactuswater"         , "Cactus Water"            , "", false, 16), // :263 FL.Juice_Cactus
			new BottleRow( 3500, "maplesap"            , "Maple Sap"               , "", false, 16), // :265 FL.Sap_Maple
			new BottleRow( 3501, "maplesyrup"          , "Maple Syrup"             , "", false, 16), // :266 FL.Syrup_Maple
			new BottleRow( 3601, "peanutbutter"        , "Peanut Butter"           , "", false, 16), // :273 FL.Nutbutter_Peanut
			new BottleRow( 3700, "rainbowsap"          , "Rainbow Sap"             , "Friendship in a Bottle, definitely not blood of a Tree", true , 16), // :275 HIDDEN FL.Sap_Rainbow (the :2898 fluid), oredict foodRainbowsap
			new BottleRow( 3800, "juicecherry"         , "Cherry Juice"            , "", false, 16), // :282 FL.Juice_Cherry
			new BottleRow( 3805, "cherrysmoothie"      , "Cherry Smoothie"         , "", false, 16), // :283 FL.Smoothie_Cherry
			new BottleRow( 3900, "juicepineapple"      , "Ananas Juice"            , "", false, 16), // :285 FL.Juice_Ananas
			new BottleRow( 3901, "winepineapple"       , "Ananas Cider"            , "", false, 16), // :286 FL.Cider_Ananas
			new BottleRow( 3905, "pineapplesmoothie"   , "Ananas Smoothie"         , "", false, 16), // :287 FL.Smoothie_Ananas
			new BottleRow( 4000, "currantjuice"        , "Currant Juice"           , "", false, 16), // :289 FL.Juice_Currant
			new BottleRow( 4005, "currantsmoothie"     , "Currant Smoothie"        , "", false, 16), // :290 FL.Smoothie_Currant
			new BottleRow( 4100, "juiceplum"           , "Plum Juice"              , "", false, 16), // :292 FL.Juice_Plum
			new BottleRow( 4105, "plumsmoothie"        , "Plum Smoothie"           , "", false, 16), // :293 FL.Smoothie_Plum
			new BottleRow( 4200, "juicepeach"          , "Peach Juice"             , "", false, 16), // :295 FL.Juice_Peach
			new BottleRow( 4205, "peachsmoothie"       , "Peach Smoothie"          , "", false, 16), // :296 FL.Smoothie_Peach
			new BottleRow( 4300, "juiceelderberry"     , "Elderberry Juice"        , "", false, 16), // :298 FL.Juice_Elderberry
			new BottleRow( 4305, "elderberrysmoothie"  , "Elderberry Smoothie"     , "", false, 16), // :299 FL.Smoothie_Elderberry
			new BottleRow( 4400, "juicegrapefruit"     , "Grapefruit Juice"        , "", false, 16), // :301 FL.Juice_Grapefruit
			new BottleRow( 4405, "grapefruitsmoothie"  , "Grapefruit Smoothie"     , "", false, 16), // :302 FL.Smoothie_Grapefruit
			new BottleRow( 4500, "juicelime"           , "Lime Juice"              , "", false, 16), // :304 FL.Juice_Lime
			new BottleRow( 4505, "limesmoothie"        , "Lime Smoothie"           , "", false, 16), // :305 FL.Smoothie_Lime
			new BottleRow( 4600, "juiceorange"         , "Orange Juice"            , "", false, 16), // :307 FL.Juice_Orange
			new BottleRow( 4605, "orangesmoothie"      , "Orange Smoothie"         , "", false, 16), // :308 FL.Smoothie_Orange
			new BottleRow( 4700, "juiceapricot"        , "Apricot Juice"           , "", false, 16), // :310 FL.Juice_Apricot
			new BottleRow( 4705, "apricotsmoothie"     , "Apricot Smoothie"        , "", false, 16), // :311 FL.Smoothie_Apricot
			new BottleRow( 4800, "juicepear"           , "Pear Juice"              , "", false, 16), // :313 FL.Juice_Pear
			new BottleRow( 4805, "pearsmoothie"        , "Pear Smoothie"           , "", false, 16), // :314 FL.Smoothie_Pear
			new BottleRow( 4900, "pumpkinjuice"        , "Pumpkin Juice"           , "", false, 16), // :316 FL.Juice_Pumpkin
			new BottleRow( 5000, "persimmonjuice"      , "Persimmon Juice"         , "", false, 16), // :318 FL.Juice_Persimmon
			new BottleRow( 5005, "persimmonsmoothie"   , "Persimmon Smoothie"      , "", false, 16), // :319 FL.Smoothie_Persimmon
			new BottleRow( 5100, "starfruitjuice"      , "Starfruit Juice"         , "", false, 16), // :321 FL.Juice_Starfruit
			new BottleRow( 5105, "starfruitsmoothie"   , "Starfruit Smoothie"      , "", false, 16), // :322 FL.Smoothie_Starfruit
			new BottleRow( 5200, "figjuice"            , "Fig Juice"               , "", false, 16), // :324 FL.Juice_Fig
			new BottleRow( 5205, "figsmoothie"         , "Fig Smoothie"            , "", false, 16), // :325 FL.Smoothie_Fig
			new BottleRow( 5300, "pomegranatejuice"    , "Pomegranate Juice"       , "", false, 16), // :327 FL.Juice_Pomegranate
			new BottleRow( 5305, "pomegranatesmoothie" , "Pomegranate Smoothie"    , "", false, 16), // :328 FL.Smoothie_Pomegranate
			new BottleRow( 5400, "mangojuice"          , "Mango Juice"             , "", false, 16), // :330 FL.Juice_Mango
			new BottleRow( 5405, "mangosmoothie"       , "Mango Smoothie"          , "", false, 16), // :331 FL.Smoothie_Mango
			new BottleRow( 5500, "papayajuice"         , "Papaya Juice"            , "", false, 16), // :333 FL.Juice_Papaya
			new BottleRow( 5505, "papayasmoothie"      , "Papaya Smoothie"         , "", false, 16), // :334 FL.Smoothie_Papaya
			new BottleRow( 5600, "coconutmilk"         , "Coconut Milk"            , "Cooking Oil", false, 16), // :336 FL.Juice_Coconut
			new BottleRow( 5604, "coconutcream"        , "Coconut Cream"           , "", false, 16), // :337 FL.Cream_Coconut
			new BottleRow( 5605, "coconutsmoothie"     , "Coconut Smoothie"        , "", false, 16), // :338 FL.Smoothie_Coconut
			new BottleRow( 5700, "beetjuice"           , "Beet Juice"              , "", false, 16)); // :340 FL.Juice_Beet

	private static int sIndexOf(List<BottleRow> aRows, int aMeta) {
		for (int i = 0; i < aRows.size(); i++) {
			if (aRows.get(i).meta() == aMeta) return i;
		}
		throw new IllegalStateException("no bottle row with meta " + aMeta);
	}

	/**
	 * The 171 rows in upstream identity order (the empty domain-entry item first, then the
	 * metas ascending with the families-a landed seats preserved: BBQ 805 between the
	 * 800-804 sauces and the apple rows, cream 1101 inside the milk family, ketchup 3101
	 * between 30001 and 32000). Drives the models band, the lang walks and the tab.
	 */
	public static final List<BottleRow> ROWS;
	static {
		List<BottleRow> tRows = new ArrayList<>(1 + FAMILY_A_ROWS.size() + FAMILY_B_ROWS.size() + 3);
		tRows.add(EMPTY_ROW);
		tRows.addAll(FAMILY_A_ROWS.subList(0, sIndexOf(FAMILY_A_ROWS, 1300))); // 0..701
		tRows.addAll(FAMILY_B_ROWS.subList(sIndexOf(FAMILY_B_ROWS, 800), sIndexOf(FAMILY_B_ROWS, 900))); // 800..804
		tRows.add(BBQ_ROW); // 805, the landed seat
		tRows.addAll(FAMILY_B_ROWS.subList(sIndexOf(FAMILY_B_ROWS, 900), sIndexOf(FAMILY_B_ROWS, 1102))); // 900..1100
		tRows.add(CREAM_ROW); // 1101, the landed seat
		tRows.addAll(FAMILY_B_ROWS.subList(sIndexOf(FAMILY_B_ROWS, 1102), sIndexOf(FAMILY_B_ROWS, 1900))); // 1102..1200
		tRows.addAll(FAMILY_A_ROWS.subList(sIndexOf(FAMILY_A_ROWS, 1300), sIndexOf(FAMILY_A_ROWS, 30000))); // 1300..1802
		tRows.addAll(FAMILY_B_ROWS.subList(sIndexOf(FAMILY_B_ROWS, 1900), FAMILY_B_ROWS.size())); // 1900..5700
		tRows.addAll(FAMILY_A_ROWS.subList(sIndexOf(FAMILY_A_ROWS, 30000), sIndexOf(FAMILY_A_ROWS, 32000))); // 30000..30001
		tRows.add(KETCHUP_ROW); // 3101, the landed seat between 30001 and 32000
		tRows.addAll(FAMILY_A_ROWS.subList(sIndexOf(FAMILY_A_ROWS, 32000), FAMILY_A_ROWS.size())); // 32000..32766
		ROWS = List.copyOf(tRows);
	}

	/** The empty glass bottle — {@code OP.bottle.dat(MT.Empty)}, OP.java:229 (the declared plain-item face). */
	public static final RegistryObject<Item> BOTTLE_EMPTY = ITEMS.register(EMPTY_ROW.id(), () -> new Item(new Item.Properties().stacksTo(EMPTY_ROW.stackSize())));

	/** The Tomato Ketchup bottle holder (the crafting ladder consumer, GT6CraftingRecipes bottleCraftRows). */
	public static final RegistryObject<Item> FOOD_KETCHUP = ITEMS.register(KETCHUP_ROW.id(), () -> new Item(new Item.Properties().stacksTo(KETCHUP_ROW.stackSize())));

	/** The Barbecue Sauce bottle holder (the ribs row consumer). */
	public static final RegistryObject<Item> FOOD_BARBECUESAUCE = ITEMS.register(BBQ_ROW.id(), () -> new Item(new Item.Properties().stacksTo(BBQ_ROW.stackSize())));

	/** The Heavy Cream bottle holder (the cake row consumer). */
	public static final RegistryObject<Item> FOOD_HEAVYCREAM = ITEMS.register(CREAM_ROW.id(), () -> new Item(new Item.Properties().stacksTo(CREAM_ROW.stackSize())));

	/** The id -> holder map (registration order = {@link #ROWS} order). */
	public static final Map<String, RegistryObject<Item>> BY_ID = new LinkedHashMap<>();
	static {
		BY_ID.put(EMPTY_ROW.id(), BOTTLE_EMPTY);
		BY_ID.put(KETCHUP_ROW.id(), FOOD_KETCHUP);
		BY_ID.put(BBQ_ROW.id(), FOOD_BARBECUESAUCE);
		BY_ID.put(CREAM_ROW.id(), FOOD_HEAVYCREAM);
		for (BottleRow tRow : FAMILY_A_ROWS) {
			BY_ID.put(tRow.id(), ITEMS.register(tRow.id(), () -> new Item(new Item.Properties().stacksTo(tRow.stackSize()))));
		}
		for (BottleRow tRow : FAMILY_B_ROWS) {
			BY_ID.put(tRow.id(), ITEMS.register(tRow.id(), () -> new Item(new Item.Properties().stacksTo(tRow.stackSize()))));
		}
	}

	/** The 171 holders in {@link #ROWS} order (the census-walk face the tests pin). */
	public static final List<RegistryObject<Item>> BOTTLES = ROWS.stream().map(tRow -> BY_ID.get(tRow.id())).toList();

	/** The tab face: the non-HIDDEN rows in {@link #ROWS} order (the TD.Creative.HIDDEN rows stay out). */
	public static final List<RegistryObject<Item>> TAB_BOTTLES = ROWS.stream().filter(tRow -> !tRow.hidden()).map(tRow -> BY_ID.get(tRow.id())).toList();

	/**
	 * The "GregTech: Bottles" tab — id {@code gt6:bottles}, title key {@link #TAB_TITLE_KEY},
	 * icon meta 1600 "Purple Drink" (MultiItemBottles.java:40, the reclaimed deviation).
	 */
	public static final RegistryObject<CreativeModeTab> BOTTLES_TAB = CREATIVE_MODE_TABS.register("bottles",
			() -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
					.title(Component.translatable(TAB_TITLE_KEY))
					.icon(() -> new ItemStack(BY_ID.get(TAB_ICON_ID).get()))
					.displayItems((aParameters, aOutput) -> {
						for (RegistryObject<Item> tRow : TAB_BOTTLES) {
							aOutput.accept(new ItemStack(tRow.get()));
						}
					})
					.build());

	private GT6Bottles() {
	}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6Foods shape). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		 *///?}
		ITEMS.register(tModBus);
		CREATIVE_MODE_TABS.register(tModBus);
	}
}
