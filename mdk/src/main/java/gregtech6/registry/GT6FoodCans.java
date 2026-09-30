package gregtech6.registry;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.GT6Mod;

/**
 * The GT6 food-can registration home — task food-can-row0 (the row0 MINIMAL subset of
 * the upstream {@code MultiItemCans} domain, decisions.p25-foodcan-row0-minimal-subset),
 * COMPLETED by task food-meat-items to the full 58-item census (IL.java:287-296): the
 * nine 6-tier families (unknown/rotten/veggie/fruit/bread/meat/fish/chum/cookies) + the
 * three air cans + the empty can. Card-owned self-contained
 * {@code @EventBusSubscriber(MOD)} DeferredRegister attached from the construct event
 * (the GT6SprayCans/GT6Tools precedent; GT6Mod.java / GTModBusListener.java stay
 * untouched).
 *
 * <p>The census — upstream metas on the MultiItemCans meta item (MultiItemCans.java:46-111):
 * unknown 1-6, rotten 11-16, veggie 21-26, fruit 31-36, bread 41-46, meat 51-56, fish
 * 61-66, chum 71-76, cookies 81-86, air 32764-32766, the empty can 998 on
 * MultiItemRandomTools (:234). Id flattening (the GT6SprayCans ruling): one id per can,
 * snake of the size adjective + family ({@code "Tiny Food Can (Rotten)"} →
 * {@code food_can_rotten_tiny}); the air cans keep their IL-name snakes
 * {@code food_can_air}/{@code _nether}/{@code _end}.
 *
 * <p>The cans stay PLAIN items — zero FoodStat/finishUsingItem surface (the row0 spec ⑤
 * cut, now over the whole census): the eat face (EnumAction.drink on the air cans, the
 * eat/rot/potion channels on the food cans, MultiItemCans.getContainerItem :124-126 and
 * getRotten :128-132) stays POOLED, and the canning/air recipe rows (RM.food_can, the
 * Canner air fill/drain :113-120) are the T4b datapack card's surface.
 *
 * <p>KJS surface: REGISTRATION face only, deferred to the KJS binding card (the
 * GT6CrystalChargers.java:44 declaration form).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6FoodCans {

	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");
	public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "gt6");

	/** The tab title lang key — the single source both the builder and the GT6EnUs datagen row use. */
	public static final String TAB_TITLE_KEY = "itemGroup.gt6.food_cans";

	/**
	 * The empty food can — id {@code gt6:food_can_empty} (upstream meta 998 "Empty Food
	 * Can", MultiItemRandomTools.java:234; the OreDictItemData = TinAlloy plateCurved face
	 * rides the port's {@code #gt6:plate_curved_tin} crafting INPUT tag on the recipe row,
	 * not the item).
	 */
	public static final RegistryObject<Item> FOOD_CAN_EMPTY = ITEMS.register("food_can_empty",
			() -> new Item(new Item.Properties()));

	/**
	 * The CANS_ROTTEN family, all 6 tiers, index 0 = tiny .. 5 = huge (the
	 * {@code IL.CANS_ROTTEN} array order, IL.java:508 over MultiItemCans.java:53-58). The
	 * {@link gregtech6.recipes.GT6RecipesCanner} food dispatch indexes THIS list exactly
	 * like upstream indexes {@code aCans[tier]} (RM.java:743-753).
	 */
	public static final List<RegistryObject<Item>> FOOD_CAN_ROTTEN = List.of(
			ITEMS.register("food_can_rotten_tiny", () -> new Item(new Item.Properties())),
			ITEMS.register("food_can_rotten_small", () -> new Item(new Item.Properties())),
			ITEMS.register("food_can_rotten_tall", () -> new Item(new Item.Properties())),
			ITEMS.register("food_can_rotten_wide", () -> new Item(new Item.Properties())),
			ITEMS.register("food_can_rotten_large", () -> new Item(new Item.Properties())),
			ITEMS.register("food_can_rotten_huge", () -> new Item(new Item.Properties())));

	/**
	 * The tier-6 cookies can — id {@code gt6:food_can_cookies_huge} (upstream
	 * {@code Food_Can_Cookies_6} meta 86, MultiItemCans.java:107): the Cookie Tin row's
	 * output, the ONLY CANS_COOKIES member row0 needs (the tier-5 index of the dispatch
	 * default branch, RM.java:753).
	 */
	public static final RegistryObject<Item> FOOD_CAN_COOKIES_HUGE = ITEMS.register("food_can_cookies_huge",
			() -> new Item(new Item.Properties()));

	/** The six tier adjectives in upstream size order (the "Tiny .. Huge" ladder). */
	private static final String[] SIZES = {"tiny", "small", "tall", "wide", "large", "huge"};

	/** One plain-can registration (the row0 {@code new Item} face over the snake id). */
	private static RegistryObject<Item> can(String aId) {
		return ITEMS.register(aId, () -> new Item(new Item.Properties()));
	}

	/** One six-tier family — the upstream "Tiny/Small/Tall/Wide/Large/Huge Food Can (X)" ladder. */
	private static List<RegistryObject<Item>> family(String aName) {
		List<RegistryObject<Item>> rList = new java.util.ArrayList<>(6);
		for (String tSize : SIZES) rList.add(can("food_can_" + aName + "_" + tSize));
		return java.util.List.copyOf(rList);
	}

	/**
	 * The Food_Can_Undefined family, all 6 tiers (upstream metas 1-6, MultiItemCans.java:
	 * 46-51, display "(Unknown)"). The display word rides the EnUs datagen table.
	 */
	public static final List<RegistryObject<Item>> FOOD_CAN_UNKNOWN = family("unknown");

	/** The Food_Can_Veggie family, all 6 tiers (metas 21-26, display "(Vegetables)"). */
	public static final List<RegistryObject<Item>> FOOD_CAN_VEGGIE = family("veggie");

	/** The Food_Can_Fruit family, all 6 tiers (metas 31-36, display "(Fruits)"). */
	public static final List<RegistryObject<Item>> FOOD_CAN_FRUIT = family("fruit");

	/** The Food_Can_Bread family, all 6 tiers (metas 41-46). */
	public static final List<RegistryObject<Item>> FOOD_CAN_BREAD = family("bread");

	/** The Food_Can_Meat family, all 6 tiers (metas 51-56 — the RM.food_can tier-0/1 target). */
	public static final List<RegistryObject<Item>> FOOD_CAN_MEAT = family("meat");

	/** The Food_Can_Fish family, all 6 tiers (metas 61-66). */
	public static final List<RegistryObject<Item>> FOOD_CAN_FISH = family("fish");

	/** The Food_Can_Chum family, all 6 tiers (metas 71-76 — the Chum canning target). */
	public static final List<RegistryObject<Item>> FOOD_CAN_CHUM = family("chum");

	/**
	 * The CANS_COOKIES tiers 1-5 (metas 81-85, MultiItemCans.java:102-106); the tier-6
	 * "Cookie Tin" stays the standalone {@link #FOOD_CAN_COOKIES_HUGE} (the row0 field,
	 * GT6RecipesCanner's output resolver).
	 */
	public static final List<RegistryObject<Item>> FOOD_CAN_COOKIES = List.of(
			can("food_can_cookies_tiny"), can("food_can_cookies_small"), can("food_can_cookies_tall"),
			can("food_can_cookies_wide"), can("food_can_cookies_large"));

	/** The Canned Space Air — upstream meta 32764 (MultiItemCans.java:109, the End air). */
	public static final RegistryObject<Item> FOOD_CAN_AIR_END = can("food_can_air_end");

	/** The Canned Hot Air — upstream meta 32765 (:110, the Nether air). */
	public static final RegistryObject<Item> FOOD_CAN_AIR_NETHER = can("food_can_air_nether");

	/** The Canned Air — upstream meta 32766 (:111, the overworld air). */
	public static final RegistryObject<Item> FOOD_CAN_AIR = can("food_can_air");

	/**
	 * The "Food Cans" tab display table — the FULL 58-can census now (task food-meat-items
	 * completed the row0 subset): the empty can head (the tab icon), then the nine tiered
	 * families in upstream meta order (unknown 1-6, rotten 11-16, veggie 21-26, fruit
	 * 31-36, bread 41-46, meat 51-56, fish 61-66, chum 71-76, cookies 81-86), then the
	 * three air cans in registration order (End 32764, Nether 32765, air 32766).
	 * Table-driven so the offline test asserts the shape + the ITEMS parity without
	 * resolving {@code get()} (the GT6ToolsCreativeTabTest face).
	 */
	public static final List<RegistryObject<Item>> TAB_TABLE = buildTabTable();

	private static List<RegistryObject<Item>> buildTabTable() {
		java.util.ArrayList<RegistryObject<Item>> rList = new java.util.ArrayList<>(58);
		rList.add(FOOD_CAN_EMPTY);
		for (List<RegistryObject<Item>> tFamily : java.util.List.of(FOOD_CAN_UNKNOWN, FOOD_CAN_ROTTEN, FOOD_CAN_VEGGIE,
				FOOD_CAN_FRUIT, FOOD_CAN_BREAD, FOOD_CAN_MEAT, FOOD_CAN_FISH, FOOD_CAN_CHUM)) {
			rList.addAll(tFamily);
		}
		rList.addAll(FOOD_CAN_COOKIES);
		rList.add(FOOD_CAN_COOKIES_HUGE);
		rList.add(FOOD_CAN_AIR_END);
		rList.add(FOOD_CAN_AIR_NETHER);
		rList.add(FOOD_CAN_AIR);
		return java.util.List.copyOf(rList);
	}

	/**
	 * The "Food Cans" tab — id {@code gt6:food_cans}, title key {@link #TAB_TITLE_KEY},
	 * icon the empty can (the canning workflow's entry item). The upstream analogue: the
	 * "GregTech: Cans" creative category over the MultiItemCans meta item
	 * (MultiItemCans.java:41 — the port gives the row0 subset the same family tab, the
	 * per-family tab discipline).
	 */
	public static final RegistryObject<CreativeModeTab> FOOD_CANS_TAB = CREATIVE_MODE_TABS.register("food_cans",
			() -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
					.title(Component.translatable(TAB_TITLE_KEY))
					.icon(() -> new ItemStack(FOOD_CAN_EMPTY.get()))
					.displayItems((aParameters, aOutput) -> {
						for (RegistryObject<Item> tRow : TAB_TABLE) {
							aOutput.accept(new ItemStack(tRow.get()));
						}
					})
					.build());

	private GT6FoodCans() {
	}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (GT6Tools.onModConstruct shape). */
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

	/** Registration smoke evidence (the GT6SprayCans onCommonSetup log shape). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> {
			GT6Mod.LOGGER.info("GT6 food cans registered: 1 empty + 9 six-tier families + 3 air cans = {} (the full census, {} tab rows)",
					ITEMS.getEntries().size(), TAB_TABLE.size());
			// the registry lookup (not the field name) makes this line real registration
			// evidence — an unregistered tab would throw here and fail the runServer gate.
			GT6Mod.LOGGER.info("GT6 creative tab registered: {} ({} display rows)",
					BuiltInRegistries.CREATIVE_MODE_TAB.getKey(FOOD_CANS_TAB.get()), TAB_TABLE.size());
			GT6Mod.LOGGER.info("GT6 food can registered: {} (the row0 canning input)",
					ForgeRegistries.ITEMS.getKey(FOOD_CAN_EMPTY.get()));
		});
	}
}
