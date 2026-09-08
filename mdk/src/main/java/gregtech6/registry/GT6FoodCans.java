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
 * The GT6 food-can registration home — task p25-food-can-row0, the row0 MINIMAL subset of
 * the upstream {@code MultiItemCans} domain (decisions.p25-foodcan-row0-minimal-subset).
 * Card-owned self-contained {@code @EventBusSubscriber(MOD)} DeferredRegister attached from
 * the construct event (the GT6SprayCans/GT6Tools precedent; GT6Mod.java /
 * GTModBusListener.java stay untouched).
 *
 * <p>The subset — 8 items of the upstream 51-meta census (8 families x 6 tiers + 3 air
 * cans + the empty can), everything else POOLED (acceptance 4: zero appearances):
 * <ul>
 * <li><b>the empty can</b> — upstream {@code IL.Food_Can_Empty} meta 998 "Empty Food Can"
 *     (MultiItemRandomTools.java:234, registered on the RANDOMTOOLS multiitem upstream, the
 *     cans tab member by creative-tab id 74); the canning INPUT of every row0 recipe and
 *     the crafting row's output.</li>
 * <li><b>the CANS_ROTTEN family, all 6 tiers</b> — upstream metas 11-16
 *     (MultiItemCans.java:53-58, {@code IL.CANS_ROTTEN} = {Food_Can_Rotten_1..6},
 *     IL.java:508); the tiered family ARRAY is what the RM.food_can dispatch indexes
 *     (RM.java:743-753 {@code aCans[tier]}), which is why all six land in row0 and not
 *     just the two the rotten_flesh/spider_eye rows output.</li>
 * <li><b>the Cookie Tin output</b> — upstream {@code Food_Can_Cookies_6} meta 86 "Huge
 *     Food Can (Cookies)" (MultiItemCans.java:107, {@code IL.CANS_COOKIES[5]}); the
 *     cookie x6 row's product (the canned-name "Cookie Tin" is the NEI display face of
 *     the row, MultiItemFood.java:600 — the ITEM is the tier-6 cookies can).</li>
 * </ul>
 * POOLED with the other food-can cards: the Undefined/Veggie/Fruit/Bread/Meat/Fish/Chum
 * families, the other five Cookies tiers, the three air cans, the eat-face FoodStat
 * (finishUsingItem — MultiItemCans.getContainerItem :124-126), the rot conversion
 * (getRotten :128-132) and the IC2 can rows.
 *
 * <p>Id flattening (the GT6SprayCans ruling): upstream ids were meta ids on the
 * MultiItemCans meta item (11-16/86/998); the port flattens to one id per can, snake of
 * the upstream size adjective + family ({@code "Tiny Food Can (Rotten)"} →
 * {@code food_can_rotten_tiny}). The cans are PLAIN items — zero FoodStat/finishUsingItem
 * surface (spec ⑤ cut), so a fresh {@code Item} carries the whole declared behaviour.
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

	/**
	 * The "Food Cans" tab display table — the empty can, then the rotten family in tier
	 * order, then the cookies tin. Table-driven so the offline test asserts the shape +
	 * the ITEMS parity without resolving {@code get()} (the GT6ToolsCreativeTabTest face).
	 */
	public static final List<RegistryObject<Item>> TAB_TABLE = buildTabTable();

	private static List<RegistryObject<Item>> buildTabTable() {
		java.util.ArrayList<RegistryObject<Item>> rList = new java.util.ArrayList<>(1 + FOOD_CAN_ROTTEN.size() + 1);
		rList.add(FOOD_CAN_EMPTY);
		rList.addAll(FOOD_CAN_ROTTEN);
		rList.add(FOOD_CAN_COOKIES_HUGE);
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
			GT6Mod.LOGGER.info("GT6 food cans registered: 1 empty + {} rotten tiers + the cookies tin (the row0 subset, {} tab rows)",
					FOOD_CAN_ROTTEN.size(), TAB_TABLE.size());
			// the registry lookup (not the field name) makes this line real registration
			// evidence — an unregistered tab would throw here and fail the runServer gate.
			GT6Mod.LOGGER.info("GT6 creative tab registered: {} ({} display rows)",
					BuiltInRegistries.CREATIVE_MODE_TAB.getKey(FOOD_CANS_TAB.get()), TAB_TABLE.size());
			GT6Mod.LOGGER.info("GT6 food can registered: {} (the row0 canning input)",
					ForgeRegistries.ITEMS.getKey(FOOD_CAN_EMPTY.get()));
		});
	}
}
