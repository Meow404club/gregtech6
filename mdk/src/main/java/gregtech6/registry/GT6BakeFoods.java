package gregtech6.registry;

import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
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
 * The GT6 food T3 bake-chain registration home — task food-bake-items, the BAKERY subset
 * of the upstream {@code MultiItemFood} domain: 60 food rows (the dough ×10 family, the
 * bun/bread/baguette/toast loaves and slices, the cookies, the cake bottom, the pizzas,
 * the burgers, the sandwiches and large sandwiches, the fries, the potatoes on sticks)
 * plus the 6 {@code Shape_Foodmold_*} items from {@code MultiItemTechnological}. The
 * food-items-core {@code GT6Foods} shape verbatim (card-owned self-contained
 * {@code @EventBusSubscriber(MOD)} DeferredRegister attached from the construct event;
 * GT6Mod.java / GTModBusListener.java stay untouched).
 *
 * <p>The 60 rows ride upstream meta order (each row = one upstream {@code addItem} line,
 * MultiItemFood.java anchors in {@link #BAKE_ROWS}); the 6 molds are MultiItemTechnological
 * :334/:338-342. The four vanilla ALIASES of the domain are NOT registered (the
 * food-brown-egg-alias precedent): Food_Bread = {@code Items.bread} (:715), Food_Potato =
 * {@code Items.potato} (:340), Food_Potato_Baked = {@code Items.baked_potato} (:346),
 * Food_Potato_Poisonous = {@code Items.poisonous_potato} (:336) — the T3b recipe card
 * keys them on the vanilla items directly.
 *
 * <p>The eat face — the upstream FoodStat translated to the modern FoodProperties with the
 * nutrition/saturationModifier literals verbatim ({@link #foodProperties}); canAlwaysEat
 * is F everywhere except the Chum Burger (the {@code T, F, T, T} flag tail,
 * MultiItemFood.java:693 — the T-slot is alwaysEdible, FoodStat.java:64-67 ctor order).
 *
 * <p>POOLED (declared): the extended FoodStat channels (hydration/temperature/
 * temperatureEffect/alcohol/caffeine/dehydration/sugar/fat/radiation — no FoodProperties
 * positions, the GT6Foods drink-seam declaration); the GT6 custom potion effects (the
 * Chum Burger's hunger + confusion rows :693 — the PotionsGT ids are not registered in the
 * port); the isRotten face of the same row (the modern record carries no rotten column);
 * the FoodsGT wolf/pet feeding rows (the FoodsGT.put faces :336-355); the OreDictItemData
 * faces (the fries' Potato U :354, the potato-on-stick's Potato U + Wood U2 :341/:347);
 * the {@code setFluidContainerStats(0, 8/16/32)} faces (:354-356, :729-732, :758-761); the
 * TC aspects (every row) and the molds' BooksGT.BOOK_REGISTER seats (:334-342); the
 * Sandwiches.INGREDIENTS seats (:776-777 — the T3b registration face); the burn values
 * (the dough's smelt-time faces); and every RECIPE row over the 66 (the Loader_Recipes_Food
 * listener block :137-153 + the MultiItemFood inline crafts + the oven smelts + the
 * Press/RollingMill/Mixer rows — the T3b card's surface, unlocked by this card; the six
 * CR.shaped mold crafts ARE in, GT6CraftingRecipes foodMold band, MultiItemTechnological
 * :336/:344-348 verbatim).
 *
 * <p>Id flattening (the GT6FoodCans ruling): upstream ids were meta ids on the
 * MultiItemFood/MultiItemTechnological meta items; the port flattens to one snake id per
 * item ({@code Food_Bun_Sliced} → {@code food_bun_sliced},
 * {@code Shape_Foodmold_Empty} → {@code shape_foodmold_empty}). The molds are PLAIN items
 * — zero shaping behaviour on the item itself (the GT6ExtruderMolds plain-Item form); the
 * upstream mold display tab is the "GregTech: Technology" multiitem tab (not ported), so
 * they ride the machines tab walk (the extruder-mold precedent, GTMachines displayItems).
 *
 * <p>KJS surface: REGISTRATION face only, deferred to the KJS binding card (the
 * GT6CrystalChargers.java:44 declaration form); the item models + textures are
 * datapack-domain, naturally moddable.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6BakeFoods {

	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/**
	 * One bake-chain food row — the upstream {@code addItem} registration line as plain
	 * data: the port snake id, the upstream display name verbatim, the FoodStat
	 * {@code aFoodLevel, aSaturation} literals, the registration-row desc tooltip verbatim,
	 * and the alwaysEdible flag (the 4-flag head slot; T only on the Chum Burger).
	 */
	public record BakeRow(String id, String enName, int nutrition, float saturation, String enTooltip, boolean alwaysEdible) {
		/** The desc tooltip lang key (the GT6Foods.FoodRow tooltipKey face). */
		public String tooltipKey() {
			return "item.gt6." + id + ".tooltip";
		}
	}

	/**
	 * The 60 rows in upstream meta order (MultiItemFood.java line anchors per row: the
	 * cookies :598/:605-606/:613-614/:621, the cake bottom :632-633, the flat doughs
	 * :640/:641, the pizzas :650-651/:656-657/:662-663/:666-667, the buns :675-678, the
	 * burgers :690-693/:694-696, the breads :714/:716-717, the sandwiches :729-732, the
	 * baguettes :744-747, the large sandwiches :758-761, the fries :354-356, the toasts
	 * :774-777, the doughs :583-589, the potatoes on sticks :341/:347).
	 */
	public static final List<BakeRow> BAKE_ROWS = List.of(
			new BakeRow("food_cookie_raw", "Cookie shaped Dough", 1, 0.2F, "For baking Cookies", false),
			new BakeRow("food_cookie_raisins_raw", "Cookie shaped Raisin Dough", 1, 0.2F, "For baking Raisin Cookies", false),
			new BakeRow("food_cookie_raisins", "Raisin Cookie", 2, 0.2F, "You don't like it? I don't care! It's delicious!", false),
			new BakeRow("food_cookie_chocolate_raisins_raw", "Cookie shaped Chocolate Raisin Dough", 1, 0.2F, "Almost looks like a regular Chocolate Chip Cookie >:D", false),
			new BakeRow("food_cookie_chocolate_raisins", "Cookie", 2, 0.2F, "", false),
			new BakeRow("food_cookie_abyssal_raw", "Cookie shaped Abyssal Dough", 1, 0.2F, "For baking netherlicious Cookies", false),
			new BakeRow("food_cakebottom_raw", "Raw Cake Bottom", 2, 0.2F, "For making Cake", false),
			new BakeRow("food_cakebottom", "Cake Bottom", 3, 0.2F, "I know I promised you an actual Cake, but well...", false),
			new BakeRow("food_dough_flat", "Flattened Dough", 1, 0.2F, "For making Pizza", false),
			new BakeRow("food_dough_flat_ketchup", "Flat Dough with Ketchup", 2, 0.2F, "For making Pizza", false),
			new BakeRow("food_pizza_cheese_raw", "Raw Pizza Margherita", 2, 0.3F, "Into the Oven with it!", false),
			new BakeRow("food_pizza_cheese", "Pizza Margherita", 6, 1.2F, "Cheese Pizza", false),
			new BakeRow("food_pizza_meat_raw", "Raw Mince Meat Pizza", 2, 0.3F, "Into the Oven with it!", false),
			new BakeRow("food_pizza_meat", "Mince Meat Pizza", 7, 1.2F, "Emo Pizza, it cuts itself!", false),
			new BakeRow("food_pizza_veggie_raw", "Raw Veggie Pizza", 1, 0.3F, "Into the Oven with it!", false),
			new BakeRow("food_pizza_veggie", "Veggie Pizza", 5, 1.2F, "The next they want is Gluten Free Pizzas...", false),
			new BakeRow("food_pizza_ananas_raw", "Raw Pizza Hawaii", 2, 0.3F, "Did you seriously just put Pineapple on a Pizza?", false),
			new BakeRow("food_pizza_ananas", "Pizza Hawaii", 7, 1.2F, "This is an Abomination! Who puts Pineapple on a Pizza!?", false),
			new BakeRow("food_bun_raw", "Dough (Bun)", 1, 0.6F, "In Bun Shape", false),
			new BakeRow("food_bun", "Bun", 2, 1.2F, "", false),
			new BakeRow("food_bun_sliced", "Sliced Bun", 1, 1.2F, "Just half a Bun", false),
			new BakeRow("food_buns_sliced", "Buns", 2, 1.2F, "Pre Sliced", false),
			new BakeRow("food_burger_veggie", "Veggie Burger", 4, 1.2F, "No matter how you call this, this is NOT a Burger!", false),
			new BakeRow("food_burger_cheese", "Cheese Burger", 4, 1.4F, "Cheesy!", false),
			new BakeRow("food_burger_meat", "Hamburger", 6, 1.6F, "The Mc Burger Queen Burger", false),
			new BakeRow("food_burger_chum", "Chum Burger", 6, 1.6F, "Fum is Chum!", true),
			new BakeRow("food_burger_tofu", "Tofu Burger", 4, 1.4F, "Just a white thingy inside Buns", false),
			new BakeRow("food_burger_soylent", "Soylent Burger", 5, 1.4F, "Don't forget your Soylent Salad with Soylent Cola!", false),
			new BakeRow("food_burger_fish", "Fish Burger", 6, 1.6F, "Smells Fishy", false),
			new BakeRow("food_bread_raw", "Dough (Bread)", 1, 0.6F, "In Bread Shape", false),
			new BakeRow("food_bread_sliced", "Sliced Bread", 2, 1.2F, "", false),
			new BakeRow("food_breads_sliced", "Breads", 5, 1.2F, "Pre Sliced", false),
			new BakeRow("food_sandwich_veggie", "Veggie Sandwich", 7, 1.2F, "It's Canon, Guys! Season 4, Vegan Morty!", false),
			new BakeRow("food_sandwich_cheese", "Cheese Sandwich", 7, 1.4F, "Say Cheese!", false),
			new BakeRow("food_sandwich_bacon", "Bacon Sandwich", 10, 1.8F, "The best Sandwich ever!", false),
			new BakeRow("food_sandwich_steak", "Steak Sandwich", 10, 1.6F, "Not a 'Steam Sandwich'", false),
			new BakeRow("food_baguette_raw", "Dough (Baguette)", 1, 0.6F, "In Baguette Shape", false),
			new BakeRow("food_baguette", "Baguette", 8, 1.2F, "I teleported nothing BUT Bread!!!", false),
			new BakeRow("food_baguette_sliced", "Sliced Baguette", 4, 1.2F, "Just half a Baguette", false),
			new BakeRow("food_baguettes_sliced", "Baguettes", 8, 1.2F, "Pre Sliced", false),
			new BakeRow("food_large_sandwich_veggie", "Large Veggie Sandwich", 15, 2.2F, "Meatless", false),
			new BakeRow("food_large_sandwich_cheese", "Large Cheese Sandwich", 15, 2.4F, "I need another cheesy tooltip for this", false),
			new BakeRow("food_large_sandwich_bacon", "Large Bacon Sandwich", 20, 2.8F, "For Men! (and manly Women)", false),
			new BakeRow("food_large_sandwich_steak", "Large Steak Sandwich", 20, 2.6F, "Yes, I once accidentially called it 'Steam Sandwich'", false),
			new BakeRow("food_fries_raw", "Potato Strips", 1, 1.2F, "Long Potatoes", false),
			new BakeRow("food_fries", "Fries", 7, 1.2F, "Not to be confused with that Futurama Guy", false),
			new BakeRow("food_fries_packaged", "Fries", 7, 1.2F, "Ketchup not included", false),
			new BakeRow("food_toast_raw", "Dough (Toast Loaf)", 1, 0.6F, "Shape of a Toast Loaf", false),
			new BakeRow("food_toast", "Loaf of Toast", 8, 1.2F, "Do not teleport Bread!", false),
			new BakeRow("food_toast_sliced", "Toast", 1, 1.2F, "Best thing since sliced Bread, oh wait...", false),
			new BakeRow("food_toasted_sliced", "Toasted Toast", 1, 1.2F, "", false),
			new BakeRow("food_dough", "Dough", 1, 1.0F, "For making Bread", false),
			new BakeRow("food_dough_sugar", "Sugary Dough", 1, 1.0F, "Don't eat the Dough before it is baken", false),
			new BakeRow("food_dough_chocolate", "Chocolate Dough", 1, 1.0F, "I said don't eat the Dough!", false),
			new BakeRow("food_dough_egg", "Egg Dough", 1, 1.0F, "For making Pasta", false),
			new BakeRow("food_dough_sugar_raisins", "Sugary Raisin Dough", 1, 1.0F, "Don't eat the Dough before it is baken", false),
			new BakeRow("food_dough_sugar_chocolate_raisins", "Sugary Chocolate Raisin Dough", 1, 1.0F, "Almost looks like Chocolate Chips", false),
			new BakeRow("food_dough_abyssal", "Abyssal Dough", 1, 1.0F, "For practicing netherlicious Bakery", false),
			new BakeRow("food_potato_on_stick", "Potato on a Stick", 1, 0.6F, "Totally looks like a Crab Claw", false),
			new BakeRow("food_potato_on_stick_roasted", "Roasted Potato on a Stick", 5, 1.2F, "Still looks like a Crab Claw", false));

	/** The 60 registered bake foods, table order (the {@link #BAKE_ROWS} walk). */
	public static final List<RegistryObject<Item>> FOODS = BAKE_ROWS.stream()
			.map(aRow -> ITEMS.<Item>register(aRow.id(), () -> new GT6Foods.GT6FoodItem(new Item.Properties().food(foodProperties(aRow)), aRow.tooltipKey())))
			.toList();

	/**
	 * The 6 food-grade molds in upstream meta order (MultiItemTechnological.java
	 * :334 Empty, :338 Bun, :339 Bread, :340 Baguette, :341 Cylinder, :342 Toast — the
	 * Cylinder is upstream-real, the card's "if upstream has it" clause resolves to yes).
	 * Plain items: the shaping work rides the machine rows, not the item (the
	 * GT6ExtruderMolds plain-Item form; the Press/RollingMill/Mixer rows are T3b).
	 */
	public static final List<RegistryObject<Item>> MOLDS = List.of(
			ITEMS.register("shape_foodmold_empty", () -> new Item(new Item.Properties())),
			ITEMS.register("shape_foodmold_bun", () -> new Item(new Item.Properties())),
			ITEMS.register("shape_foodmold_bread", () -> new Item(new Item.Properties())),
			ITEMS.register("shape_foodmold_baguette", () -> new Item(new Item.Properties())),
			ITEMS.register("shape_foodmold_cylinder", () -> new Item(new Item.Properties())),
			ITEMS.register("shape_foodmold_toast", () -> new Item(new Item.Properties())));

	/**
	 * The upstream FoodStat → the modern FoodProperties (the {@link GT6Foods#foodProperties}
	 * translation forked for the one alwaysEdible=T row — the Chum Burger :693; the builder
	 * input is the modifier on BOTH legs, the 21.1 record folds it at build). Package-private
	 * so the offline test pins the SAME construction the registration runs.
	 */
	static FoodProperties foodProperties(BakeRow aRow) {
		//? if forge {
		FoodProperties.Builder tBuilder = new FoodProperties.Builder().nutrition(aRow.nutrition()).saturationMod(aRow.saturation());
		//?} else {
		/*FoodProperties.Builder tBuilder = new FoodProperties.Builder().nutrition(aRow.nutrition()).saturationModifier(aRow.saturation());
		//21.1: FoodProperties.Builder.saturationMod → saturationModifier (the GT6Foods fork)
		*///?}
		//? if forge {
		if (aRow.alwaysEdible()) tBuilder.alwaysEat();
		//?} else {
		/*if (aRow.alwaysEdible()) tBuilder.alwaysEdible();
		*///?}
		//21.1: FoodProperties.Builder.alwaysEat → alwaysEdible (the 1.21.1 rename, the
		//GT6Foods saturationMod fork shape)
		return tBuilder.build();
	}

	private GT6BakeFoods() {
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
	}

	/** Registration smoke evidence (the GT6Foods onCommonSetup log shape). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> {
			GT6Mod.LOGGER.info("GT6 bake foods registered: {} food rows + {} food molds (the T3 bake chain)",
					BAKE_ROWS.size(), MOLDS.size());
			GT6Mod.LOGGER.info("GT6 dough registered: {} (the bake chain root item, meta 32000)",
					ForgeRegistries.ITEMS.getKey(FOODS.get(51).get()));
		});
	}
}
