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

	private static int sFirstIndexOfMeta(int aMeta) {
		for (int i = 0; i < FAMILY_A_ROWS.size(); i++) {
			if (FAMILY_A_ROWS.get(i).meta() == aMeta) return i;
		}
		throw new IllegalStateException("no families-A row with meta " + aMeta);
	}

	/**
	 * The 75 rows in upstream identity order (the empty domain-entry item first, then the
	 * metas ascending — ketchup 3101 lands between 30001 and 32000, BBQ 805 / cream 1101
	 * before the honey segment). Drives the models band, the lang walks and the tab.
	 */
	public static final List<BottleRow> ROWS;
	static {
		List<BottleRow> tRows = new ArrayList<>(1 + FAMILY_A_ROWS.size() + 3);
		tRows.add(EMPTY_ROW);
		tRows.addAll(FAMILY_A_ROWS.subList(0, sFirstIndexOfMeta(1300))); // 0..701
		tRows.add(BBQ_ROW);
		tRows.add(CREAM_ROW);
		tRows.addAll(FAMILY_A_ROWS.subList(sFirstIndexOfMeta(1300), sFirstIndexOfMeta(32000))); // 1300..30001
		tRows.add(KETCHUP_ROW);
		tRows.addAll(FAMILY_A_ROWS.subList(sFirstIndexOfMeta(32000), FAMILY_A_ROWS.size())); // 32000..32766
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
	}

	/** The 75 holders in {@link #ROWS} order (the census-walk face the tests pin). */
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
