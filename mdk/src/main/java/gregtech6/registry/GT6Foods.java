package gregtech6.registry;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
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
 * The GT6 food-item registration home — task food-items-core, the T1 MINIMUM subset of
 * the upstream {@code MultiItemFood} domain: the ~9 items every T1b recipe row backfill
 * needs as an output (the B2 unlock verdict, state research.food-domain
 * tiers.T1/tiers.item_minimum), EXTENDED by task food-meat-items (T4a) with the egg
 * family tail + the meat/chum families (MultiItemFood.java:495-:560/:563-:578/:594/:798
 * — 29 rows, the whole table now walks ascending upstream meta order). Card-owned
 * self-contained {@code @EventBusSubscriber(MOD)} DeferredRegister attached from the
 * construct event (the GT6FoodCans precedent verbatim; GT6Mod.java /
 * GTModBusListener.java stay untouched).
 *
 * <p>The T4a rows, by family (each row = one upstream {@code addItem} line):
 * <ul>
 * <li><b>eggs</b> — {@code food_white_egg} meta 1051 (:496; the NOT-edible ingredient
 *     egg — the vanilla-egg alias {@code Food_Brown_Egg} :495 stays the vanilla item and
 *     registers nothing), {@code food_egg_fried/scrambled/sliced/yolk/white} metas
 *     1070-1074 (:499-:503; the boiled twins 1060/1061 :497-:498 already landed with the
 *     T1 subset and are NOT re-registered). The yolk-mayo row face = the T2 fluid domain
 *     (FL.Mayo landed with food-fluids-b1), the egg recipe rows = T4b.</li>
 * <li><b>meat</b> — ham 1100-1103 (:529-:538), bacon 1112/1113 (:541-:543), ribs
 *     1200-1202 (:546-:550), rib eye 1210/1211 (:553-:555), dogmeat 1300/1301
 *     (:558-:560), mutton 1400/1401 (:563-:565), horse 1500/1501 (:568-:570), mule
 *     1510/1511 (:572-:574), donkey 1520/1521 (:576-:578), chum 10000 (:594) + chum on a
 *     stick 10010 (:798; the ONLY alwaysEdible rows, the 4-flag tail head T).</li>
 * </ul>
 *
 * <p>The eat face — the upstream FoodStat translated to the modern FoodProperties with
 * the nutrition/saturationModifier literals verbatim ({@link #foodProperties}; canAlwaysEat
 * rides the 4-flag tail head {@code alwaysEdible}, FoodStat.java:64-67).
 *
 * <p>POOLED (declared): the extended FoodStat channels (hydration/temperature/
 * temperatureEffect/alcohol/caffeine/dehydration/sugar/fat/radiation — the modern
 * FoodProperties carries no positions for them, the GTDrinks.java:23-31 drink-seam
 * declaration); the GT6 custom potion effects (ID_SLIPPERY on the scrambled egg/yolk,
 * ID_STICKY on the scrambled egg/white/chum pair — the PotionsGT ids are not registered
 * in the port); the OreDictItemData faces (MeatRaw/MeatCooked/Bone composites); the
 * Sandwiches.INGREDIENTS seats; the FoodsGT.put vanilla meat/fish values table
 * (:514-:526 — the sandwich/canning data domain, zero port surface, the T4b rows); the
 * TC aspects; the IItemRottable rot face; the Behavior_FeedDog/FeedCat faces; the
 * {@code setFluidContainerStats(0, 8)} faces; the ice-cream 37-flavour family, the
 * scrap-meat 1998 and the ingot-bar metas 32101-32115 (:925-:932, the T3/pool rows); and
 * every recipe row (smelting, kX slicing, the Slicer/Shredder/Mortar/Fermenter listener
 * block Loader_Recipes_Food.java:350-:516 — the T4b row-backfill card's surface).
 *
 * <p>Id flattening (the GT6FoodCans ruling): upstream ids were meta ids on the
 * MultiItemFood meta item; the port flattens to one snake id per food.
 *
 * <p>KJS surface: REGISTRATION face only, deferred to the KJS binding card (the
 * GT6CrystalChargers.java:44 declaration form); the item models + textures are
 * datapack-domain, naturally moddable.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Foods {

	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");
	public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "gt6");

	/** The tab title lang key — the single source both the builder and the GT6EnUs datagen row use. */
	public static final String TAB_TITLE_KEY = "itemGroup.gt6.food";

	/**
	 * One food row — the upstream {@code addItem} registration line as plain data: the
	 * port snake id, the upstream display name verbatim, the FoodStat
	 * {@code aFoodLevel, aSaturation} literals (the FoodProperties face), the
	 * registration-row desc tooltip verbatim (the GTPistolItem.Kind tooltipKey face; an
	 * empty tooltip = the upstream row carries no desc column, so NO tooltip key exists
	 * and the item renders no line), and the upstream 4-flag tail head
	 * {@code alwaysEdible} (FoodStat.java:64-67 — T only on the two Chum rows
	 * MultiItemFood.java:594/:798, F everywhere else).
	 *
	 * <p>{@code nutrition == 0} = the upstream row carries NO FoodStat at all — the
	 * plain-item face (the White Egg :496, an ingredient egg; unlike the vanilla-egg
	 * alias :495 which stays the vanilla item, the GT6 twin is not edible).
	 */
	public record FoodRow(String id, String enName, int nutrition, float saturation, String enTooltip, boolean canAlwaysEat) {
		/** The desc tooltip lang key (the GT6LaserGas tooltip-key-in-ctor face). */
		public String tooltipKey() {
			return "item.gt6." + id + ".tooltip";
		}

		/** The upstream FoodStat presence face: 0 nutrition = the no-FoodStat row. */
		public boolean edible() {
			return nutrition > 0;
		}
	}

	/**
	 * The 38 rows in upstream meta order (MultiItemFood.java:490/:491/:497/:498/:496/
	 * :1070/:1071/:1072/:1073/:1074/:529-:560/:563-:578/:594/:798/:365/:374/:809/:933/
	 * :934) — the registration, tab, datagen and test walk all ride THIS table so the
	 * faces cannot drift.
	 */
	public static final List<FoodRow> FOOD_ROWS = List.of(
			new FoodRow("food_cheese", "Cheese", 2, 1.2F, "Click the Cheese", false),
			new FoodRow("food_cheese_sliced", "Cheese Slice", 1, 0.6F, "ALIEN ATTACK!!!, throw the CHEEEEESE!!!", false),
			// task food-meat-items — the egg family head (:495 vanilla-egg alias stays the
			// vanilla item; the GT6 twin :496 IS a new item, the boiled twins :497-:498 above
			// predate it in the table only by their earlier landing):
			new FoodRow("food_white_egg", "Egg", 0, 0.0F, "The Egg came before the Chicken!", false),
			new FoodRow("food_brown_egg_boiled", "Boiled Egg", 2, 1.2F, "Did you expect this to look different after boiling?", false),
			new FoodRow("food_white_egg_boiled", "Boiled Egg", 2, 1.2F, "Did you expect this to look different after boiling?", false),
			new FoodRow("food_egg_fried", "Fried Egg", 2, 1.2F, "", false),
			new FoodRow("food_egg_scrambled", "Scrambled Egg", 2, 1.2F, "", false),
			new FoodRow("food_egg_sliced", "Sliced Egg", 1, 0.6F, "Eggcellent!", false),
			new FoodRow("food_egg_yolk", "Egg Yolk", 1, 1.2F, "That's all, Yolks!", false),
			new FoodRow("food_egg_white", "Egg White", 1, 1.2F, "", false),
			// the meat family (MultiItemFood.java:529-560 + the mutton/horse/mule/donkey
			// rows :563-:578 + the chum pair :594/:798):
			new FoodRow("food_ham_raw", "Raw Ham", 3, 0.6F, "Dropped by Pigs and Boars", false),
			new FoodRow("food_ham_cooked", "Cooked Ham", 10, 1.6F, "", false),
			new FoodRow("food_ham_slice_raw", "Raw Ham Slice", 1, 0.6F, "", false),
			new FoodRow("food_ham_slice_cooked", "Cooked Ham Slice", 3, 1.6F, "", false),
			new FoodRow("food_bacon_raw", "Raw Bacon", 1, 0.9F, "Dropped by Pigs and Boars", false),
			new FoodRow("food_bacon_cooked", "Grilled Bacon", 3, 1.8F, "", false),
			new FoodRow("food_rib_raw", "Raw Ribs", 3, 0.6F, "Dropped by large Animals", false),
			new FoodRow("food_rib_cooked", "Grilled Ribs", 10, 1.6F, "", false),
			new FoodRow("food_rib_bbq", "Barbecue Ribs", 10, 1.6F, "High Quality Video Game Ribs", false),
			new FoodRow("food_ribeyesteak_raw", "Raw Rib Eye Steak", 3, 0.6F, "Dropped by large Animals", false),
			new FoodRow("food_ribeyesteak_cooked", "Grilled Rib Eye Steak", 10, 1.6F, "It is staring at you", false),
			new FoodRow("food_dogmeat_raw", "Dogmeat", 2, 0.6F, "Why didn't you use [MERCY]?", false),
			new FoodRow("food_dogmeat_cooked", "Grilled Dogmeat", 8, 1.6F, "You monster!", false),
			new FoodRow("food_mutton_raw", "Mutton", 2, 0.6F, "Beep Beep, I'm a Sheep, I said: Beep Beep Imma Sheep", false),
			new FoodRow("food_mutton_cooked", "Grilled Mutton", 7, 2.0F, "", false),
			new FoodRow("food_horse_raw", "Horse Meat", 2, 0.6F, "", false),
			new FoodRow("food_horse_cooked", "Grilled Horse Meat", 8, 1.6F, "", false),
			new FoodRow("food_mule_raw", "Mule Meat", 3, 0.8F, "", false),
			new FoodRow("food_mule_cooked", "Grilled Mule Meat", 10, 1.8F, "", false),
			new FoodRow("food_donkey_raw", "Donkey Meat", 2, 0.6F, "", false),
			new FoodRow("food_donkey_cooked", "Grilled Donkey Meat", 8, 1.6F, "", false),
			new FoodRow("food_chum", "Chum", 5, 1.6F, "Chum is Fum!", true),
			new FoodRow("food_chum_on_stick", "Chum on a Stick", 5, 1.6F, "Don't forget to try our Chum-balaya", true),
			// back to the chips/ice-cream/butter metas (:365/:374/:809/:933/:934):
			new FoodRow("food_potato_chips", "Potato Chips", 7, 1.2F, "Crunchy", false),
			new FoodRow("food_chili_chips", "Chili Chips", 7, 1.2F, "Spicy", false),
			new FoodRow("food_ice_cream", "Ice Cream", 1, 0.6F, "Basic Milk Gelato", false),
			new FoodRow("food_butter", "Butter", 1, 4.0F, "A chunk of pure Fat", false),
			new FoodRow("food_butter_salted", "Salted Butter", 1, 4.0F, "As if it wasn't unhealthy already", false));

	/** The 38 registered foods, table order (the {@link #FOOD_ROWS} walk). */
	public static final List<RegistryObject<Item>> FOODS = FOOD_ROWS.stream()
			.map(aRow -> ITEMS.<Item>register(aRow.id(), () -> new GT6FoodItem(foodProps(aRow), aRow.enTooltip().isEmpty() ? null : aRow.tooltipKey())))
			.toList();

	/** The item properties face — the food component only on the FoodStat rows (the White Egg :496 carries none). */
	private static Item.Properties foodProps(FoodRow aRow) {
		return aRow.edible() ? new Item.Properties().food(foodProperties(aRow)) : new Item.Properties();
	}

	/**
	 * The upstream FoodStat → the modern FoodProperties, the card's one translation
	 * point: nutrition/saturationModifier ride the row literals, canAlwaysEat is F
	 * (the upstream 4-flag tail, FoodStat.java:64-67). The builder input is the modifier
	 * on BOTH legs; the 21.1 record folds it at build (FoodProperties.java:113-115,
	 * FoodConstants.saturationByModifier = modifier x nutrition x 2 — the total-energy
	 * face), the 1.20.1 getter keeps the raw modifier. Package-private so the offline
	 * test pins the SAME construction the registration runs.
	 */
	static FoodProperties foodProperties(FoodRow aRow) {
		//? if forge {
		FoodProperties.Builder tBuilder = new FoodProperties.Builder().nutrition(aRow.nutrition()).saturationMod(aRow.saturation());
		if (aRow.canAlwaysEat()) tBuilder.alwaysEat();
		return tBuilder.build();
		//?} else {
		/*FoodProperties.Builder tBuilder = new FoodProperties.Builder().nutrition(aRow.nutrition()).saturationModifier(aRow.saturation());
		if (aRow.canAlwaysEat()) tBuilder.alwaysEdible();
		return tBuilder.build();
		//21.1: FoodProperties.Builder.saturationMod → saturationModifier, alwaysEat → alwaysEdible (javap 21.1.249)
		*///?}
	}

	/**
	 * The "GregTech: Nature & Foods" tab — id {@code gt6:food}, title key
	 * {@link #TAB_TITLE_KEY}, icon the cheese (the domain entry item; the upstream tab
	 * icon is the MultiItemFood meta-0 face, the unpored lemon — MultiItemFood.java:48
	 * "GregTech: Nature & Foods", the per-family tab discipline).
	 */
	public static final RegistryObject<CreativeModeTab> FOOD_TAB = CREATIVE_MODE_TABS.register("food",
			() -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
					.title(Component.translatable(TAB_TITLE_KEY))
					.icon(() -> new ItemStack(FOODS.get(0).get()))
					.displayItems((aParameters, aOutput) -> {
						for (RegistryObject<Item> tRow : FOODS) {
							aOutput.accept(new ItemStack(tRow.get()));
						}
						// task food-bake-items: the T3 bake chain rides the SAME "GregTech: Nature
						// & Foods" tab (all 60 rows are MultiItemFood items — the upstream
						// per-multiitem tab discipline; the T1 rows lead, the T3 tail appends)
						for (RegistryObject<Item> tRow : GT6BakeFoods.FOODS) {
							aOutput.accept(new ItemStack(tRow.get()));
						}
						// task food-crop-items: the T5a berry/nut/fruit band + the fodder family
						// ride the SAME "GregTech: Nature & Foods" tab (all 61 rows are
						// MultiItemFood items — the upstream per-multiitem tab discipline; the
						// T1 rows lead, the T5a tail appends)
						for (RegistryObject<Item> tRow : GT6CropFoods.FOODS) {
							aOutput.accept(new ItemStack(tRow.get()));
						}
						for (RegistryObject<Item> tRow : GT6CropFoods.PLAINS) {
							aOutput.accept(new ItemStack(tRow.get()));
						}
					})
					.build());

	private GT6Foods() {
	}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (the GT6FoodCans shape). */
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

	/** Registration smoke evidence (the GT6FoodCans onCommonSetup log shape). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> {
			GT6Mod.LOGGER.info("GT6 foods registered: {} rows (the T1 subset, {} tab rows)",
					FOOD_ROWS.size(), FOODS.size());
			GT6Mod.LOGGER.info("GT6 creative tab registered: {} ({} display rows)",
					BuiltInRegistries.CREATIVE_MODE_TAB.getKey(FOOD_TAB.get()), FOODS.size());
			GT6Mod.LOGGER.info("GT6 cheese registered: {} (the domain entry item)",
					ForgeRegistries.ITEMS.getKey(FOODS.get(0).get()));
		});
	}

	/**
	 * The food item — a vanilla food Item plus the registration-row desc tooltip (the
	 * GTPistolItem appendHoverText face; the eat walk is the vanilla default, the
	 * upstream container/alwaysEdible/potion channels are the declared pool). A
	 * {@code null} tooltip key (the upstream {@code ""} desc rows, task food-crop-items)
	 * emits no tooltip line — a missing lang key would render the RAW key at runtime.
	 */
	public static class GT6FoodItem extends Item {

		/** null = the upstream row carries no desc column — no tooltip line (the "" rows). */
		private final String mTooltipKey;

		public GT6FoodItem(Properties aProperties, String aTooltipKey) {
			super(aProperties);
			mTooltipKey = aTooltipKey;
		}

		//? if forge {
		@Override
		public void appendHoverText(ItemStack aStack, net.minecraft.world.level.Level aLevel, List<Component> aTooltip, TooltipFlag aFlag) {
			super.appendHoverText(aStack, aLevel, aTooltip, aFlag);
			if (mTooltipKey != null) aTooltip.add(Component.translatable(mTooltipKey));
		}
		//?} else {
		/*@Override
		public void appendHoverText(ItemStack aStack, Item.TooltipContext aContext, List<Component> aTooltip, TooltipFlag aFlag) {
		//21.1: the hover signature carries the Item.TooltipContext (the GTPistolItem fork).
			super.appendHoverText(aStack, aContext, aTooltip, aFlag);
			if (mTooltipKey != null) aTooltip.add(Component.translatable(mTooltipKey));
		}
		*///?}
	}
}
