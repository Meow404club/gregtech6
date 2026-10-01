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
 * The GT6 food T5a crop-domain registration home — task food-crop-items: the BERRY/NUT/
 * FRUIT band of the upstream {@code MultiItemFood} (49 food rows, the veg/fruit band
 * lemon→coconut, the 4 grape colours + 5 raisins, the 10 berries, the 3 GT6 apples of the
 * 4-colour family with their slices, peanut/hazelnut/ananas/cinnamon/coconut), the 4
 * inedible apple cores, and the fodder family (4 grass states + the 4 GT6 crops) — 61
 * rows total. The food-items-core {@code GT6Foods} shape verbatim (card-owned
 * self-contained {@code @EventBusSubscriber(MOD)} DeferredRegister attached from the
 * construct event; GT6Mod.java / GTModBusListener.java stay untouched; the rows ride the
 * SAME {@code gt6:food} creative tab, the GT6BakeFoods displayItems ride form).
 *
 * <p>Baseline: the T4a meat/egg band (work/food-meat-items) is NOT a dependency — every
 * row here is self-contained data, so the card builds on the main tip and the bands merge
 * in any order (the T3a→T4a→T5a merge order only orders the shared lang seams).
 *
 * <p>The vanilla ALIASES and foreign/block faces of the domain are NOT registered (the
 * food-brown-egg-alias precedent):
 * <ul>
 * <li>{@code Food_Apple_Red} = {@code Items.apple} (MultiItemFood.java:450 — its slice/core
 * ARE the GT6 items 231/232 here);</li>
 * <li>{@code Food_Carrot} = {@code Items.carrot} (:330), the potato family = the T3 bake
 * chain aliases (:336/:340/:346);</li>
 * <li>the meta-251 migration stub (the nameless {@code Behavior_Turn_Into} hidden row :468
 * — a GT6U id-migration face, not an item);</li>
 * <li>{@code Crop_Wheat} = {@code Items.wheat} and {@code Bale_Wheat} = the hay block
 * (gregapi LoaderItemList.java:761/:760 aliases);</li>
 * <li>{@code Crop_AbyssalOats} = the Netherlicious item (:1319) and
 * {@code Bale_AbyssalOats} = the foreign conditional (Loader_Recipes_Crops.java:196
 * {@code exists()} arm — never GT6-registered): foreign-identity TRUE NEGATIVES;</li>
 * <li>the 8 GT6 Bale items ({@code Bale/Moldy/Dry/Rotten} + {@code Bale_Rye/Oats/Barley/
 * Rice}) are the BLOCK-ITEM forms of BlockBaleGrass.java:53-83 / BlockBaleCrop.java:42-72
 * — the port has no bale blocks, so the item forms stay pooled with the block face (the
 * card boundary: report to T5b or a block card).</li>
 * </ul>
 *
 * <p>The eat face — the upstream FoodStat translated to the modern FoodProperties with the
 * nutrition/saturationModifier literals verbatim ({@link #foodProperties}); canAlwaysEat
 * is F on every row (the 4-flag tail {@code F, T, F, T}, FoodStat.java:64-67 ctor order —
 * no alwaysEdible=T row exists in this band).
 *
 * <p>Id flattening (the GT6FoodCans ruling): one snake id per upstream item, IL name
 * lowercased wholesale preserving underscores (the GT6BakeFoods
 * {@code Food_CakeBottom_Raw → food_cakebottom_raw} form). EVERY row keeps the
 * {@code food_} family prefix — also the fodder/crop rows — because the bare
 * {@code gt6:grass} id is TAKEN by the GTGrassBlocks variant-0 block (its id scheme
 * javadoc: "variant 0 keeps the bare id gt6:grass") and all 61 upstream ids share the one
 * gt.multiitem.food meta namespace anyway.
 *
 * <p>POOLED (declared): the extended FoodStat channels (hydration/temperature/
 * temperatureEffect/alcohol/caffeine/dehydration/sugar/fat/radiation — no FoodProperties
 * positions, the GT6Foods drink-seam declaration); the GT6 custom potion effects (the
 * lemon's ID_CONDUCTIVE :273, the banana's ID_SLIPPERY :384, the candleberry's
 * ID_FLAMMABLE :403 — the PotionsGT ids are not registered in the port); the
 * OreDictItemData faces (the pomeraisins' raisins seat :391, the crop/grass oredicts
 * OD.itemGrass family :53-60); the pet-feeding behaviors (Behavior_FeedGrass :53-60,
 * Behavior_FeedPig on the apples/cores, Behavior_FeedDog on the pomeraisins); the
 * BushesGT bush-render colors (:395-429 — the bush blocks are unpored); the
 * Sandwiches.INGREDIENTS seats (every *_Sliced row); the TC aspects; the smelt-time
 * columns (the cores/bark TICKS_PER_SMELT faces); the replicateOrganic/`kX` slicing/
 * food_can rows over the band (the T5b recipe card's surface); the apple-core
 * Behavior_Turn_Into composting face.
 *
 * <p>KJS surface: REGISTRATION face only, deferred to the KJS binding card (the
 * GT6CrystalChargers.java:44 declaration form); the item models + textures are
 * datapack-domain, naturally moddable.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6CropFoods {

	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/**
	 * One crop-band food row — the upstream {@code addItem} registration line as plain
	 * data: the port snake id, the upstream display name verbatim, the FoodStat
	 * {@code aFoodLevel, aSaturation} literals, and the registration-row desc tooltip
	 * verbatim ({@code ""} rows emit no tooltip key on either locale — the GT6BakeFoods
	 * empty-desc ruling).
	 */
	public record CropFoodRow(String id, String enName, int nutrition, float saturation, String enTooltip) {
		/** The desc tooltip lang key, null on the {@code ""} rows (the raw-key guard). */
		public String tooltipKey() {
			return enTooltip.isEmpty() ? null : "item.gt6." + id + ".tooltip";
		}
	}

	/** One inedible row (the apple cores + the fodder family) — a plain tooltip item. */
	public record CropPlainRow(String id, String enName, String enTooltip) {
		/** The desc tooltip lang key, null on the {@code ""} rows (the raw-key guard). */
		public String tooltipKey() {
			return enTooltip.isEmpty() ? null : "item.gt6." + id + ".tooltip";
		}
	}

	/**
	 * The 49 food rows in upstream meta order (MultiItemFood.java anchors per row: the
	 * lemon pair :273-274, the tomato trio :279-285, the onion pair :290-291, the
	 * cucumber/pickle band :296-299, the chili :307, the grapes/raisins :311-327, the
	 * carrot slice :331, the banana pair :384-385, the pomegranate pair :390-391, the 10
	 * berries :395-431, the apple slices :437/:444/:451/:458 + the whole green/yellow/
	 * dark-red apples :436/:443/:457, the nuts/ananas/cinnamon/coconut :467-486).
	 */
	public static final List<CropFoodRow> FOOD_ROWS = List.of(
			new CropFoodRow("food_lemon", "Lemon", 1, 0.6F, "Don't make Lemonade"),
			new CropFoodRow("food_lemon_sliced", "Lemon Slice", 0, 0.15F, "Ideal to put on your Drink"),
			new CropFoodRow("food_tomato", "Tomato", 1, 0.6F, "Solid Ketchup"),
			new CropFoodRow("food_tomato_sliced", "Tomato Slice", 0, 0.15F, "Solid Ketchup"),
			new CropFoodRow("food_mtomato", "Maxim Tomato", 9, 1.0F, "Ten Hearts in one Tomato"),
			new CropFoodRow("food_onion", "Onion", 1, 1.2F, "Taking over the whole Taste"),
			new CropFoodRow("food_onion_sliced", "Onion Slice", 0, 0.3F, "ONIONS, UNITE!"),
			new CropFoodRow("food_cucumber", "Cucumber", 1, 1.2F, "Is it a type of Melon or a type of Pumpkin?"),
			new CropFoodRow("food_cucumber_sliced", "Cucumber Slice", 0, 0.3F, "Not a sliced Sea Cucumber!"),
			new CropFoodRow("food_pickle", "Pickle", 1, 1.2F, "Not a Sea Pickle! Not Rick either!"),
			new CropFoodRow("food_pickle_sliced", "Pickle Slice", 0, 0.3F, "You seem to be in a Pickle."),
			new CropFoodRow("food_chili_pepper", "Chili Pepper", 1, 1.2F, "It is red and hot"),
			new CropFoodRow("food_grapes_green", "Green Grapes", 1, 0.6F, "Source of Wine"),
			new CropFoodRow("food_raisins_green", "Green Raisins", 2, 0.6F, "Dried Grapes"),
			new CropFoodRow("food_grapes_white", "White Grapes", 1, 0.6F, "Source of Wine"),
			new CropFoodRow("food_raisins_white", "White Raisins", 2, 0.6F, "Dried Grapes"),
			new CropFoodRow("food_grapes_red", "Red Grapes", 1, 0.6F, "Source of Wine"),
			new CropFoodRow("food_raisins_red", "Red Raisins", 2, 0.6F, "Dried Grapes"),
			new CropFoodRow("food_grapes_purple", "Purple Grapes", 1, 0.6F, "Source of Wine"),
			new CropFoodRow("food_raisins_purple", "Purple Raisins", 2, 0.6F, "Dried Grapes"),
			new CropFoodRow("food_raisins_chocolate", "Chocolate Raisins", 3, 1.2F, "Dried Grapes coated in Chocolate"),
			new CropFoodRow("food_carrot_sliced", "Carrot Slice", 0, 0.3F, "Sliced Goku"),
			new CropFoodRow("food_banana", "Banana", 1, 0.6F, "For Scale"),
			new CropFoodRow("food_banana_sliced", "Banana Slice", 0, 0.15F, "Food for Minions"),
			new CropFoodRow("food_pomegranate", "Pomegranate", 1, 0.6F, "A seeded Apple"),
			new CropFoodRow("food_pomeraisins", "Pomeraisins", 2, 0.6F, "Lesser Dogs favourite Food"),
			new CropFoodRow("food_blueberry", "Blueberry", 1, 0.6F, ""),
			new CropFoodRow("food_gooseberry", "Gooseberry", 1, 0.6F, ""),
			new CropFoodRow("food_candleberry", "Candleberry", 1, 0.6F, ""),
			new CropFoodRow("food_cranberry", "Cranberry", 1, 0.6F, ""),
			new CropFoodRow("food_currants_black", "Black Currants", 1, 0.6F, ""),
			new CropFoodRow("food_currants_white", "White Currants", 1, 0.6F, ""),
			new CropFoodRow("food_currants_red", "Red Currants", 1, 0.6F, ""),
			new CropFoodRow("food_blackberry", "Blackberry", 1, 0.6F, ""),
			new CropFoodRow("food_raspberry", "Raspberry", 1, 0.6F, ""),
			new CropFoodRow("food_strawberry", "Strawberry", 1, 0.6F, ""),
			new CropFoodRow("food_apple_green", "Apple", 4, 0.4F, ""),
			new CropFoodRow("food_apple_green_sliced", "Apple Slice", 1, 0.4F, ""),
			new CropFoodRow("food_apple_yellow", "Apple", 5, 0.3F, ""),
			new CropFoodRow("food_apple_yellow_sliced", "Apple Slice", 1, 0.3F, ""),
			new CropFoodRow("food_apple_red_sliced", "Apple Slice", 1, 0.3F, ""),
			new CropFoodRow("food_apple_darkred", "Apple", 5, 0.4F, ""),
			new CropFoodRow("food_apple_darkred_sliced", "Apple Slice", 1, 0.4F, ""),
			new CropFoodRow("food_peanut", "Peanut", 2, 0.3F, "Deez Nutz"),
			new CropFoodRow("food_hazelnut", "Hazelnut", 2, 0.3F, ""),
			new CropFoodRow("food_ananas", "Ananas", 4, 0.3F, "Who lives in a Pineapple under the the Sea?"),
			new CropFoodRow("food_ananas_sliced", "Ananas Slice", 1, 0.3F, "Did Ted ever find out about the Mystery Pineapple?"),
			new CropFoodRow("food_cinnamon", "Cinnamon Bark", 2, 0.3F, "Don't let anyone Challenge you!"),
			new CropFoodRow("food_coconut", "Coconut", 2, 0.3F, "His Coconut Gun can fire in spurts. If he shoots ya, it's gonna hurt!"));

	/**
	 * The 12 inedible rows in upstream meta order (the 4 apple cores :438/:445/:452/:459,
	 * the 4 grass states :53-56, the 4 crops :57-60). Plain items: the compost/feeding/
	 * bale work rides the machine rows and the unpored block faces, not the item.
	 */
	public static final List<CropPlainRow> PLAIN_ROWS = List.of(
			new CropPlainRow("food_apple_green_core", "Apple Core", "Not to be confused with the Mod"),
			new CropPlainRow("food_apple_yellow_core", "Apple Core", "Not to be confused with the Mod"),
			new CropPlainRow("food_apple_red_core", "Apple Core", "Not to be confused with the Mod"),
			new CropPlainRow("food_apple_darkred_core", "Apple Core", "Not to be confused with the Mod"),
			new CropPlainRow("food_grass", "Grass", "Make 9 of this into a Bale in order to dry it"),
			new CropPlainRow("food_grass_dry", "Dry Grass", "Useful for making a simple Fire Starter"),
			new CropPlainRow("food_grass_moldy", "Moldy Grass", ""),
			new CropPlainRow("food_grass_rotten", "Rotten Grass", ""),
			new CropPlainRow("food_crop_rye", "Rye", ""),
			new CropPlainRow("food_crop_oats", "Oats", ""),
			new CropPlainRow("food_crop_barley", "Barley", ""),
			new CropPlainRow("food_crop_rice", "Rice", ""));

	/** The 49 registered crop foods, table order (the {@link #FOOD_ROWS} walk). */
	public static final List<RegistryObject<Item>> FOODS = FOOD_ROWS.stream()
			.map(aRow -> ITEMS.<Item>register(aRow.id(), () -> new GT6Foods.GT6FoodItem(new Item.Properties().food(foodProperties(aRow)), aRow.tooltipKey())))
			.toList();

	/** The 12 registered inedibles, table order (the {@link #PLAIN_ROWS} walk). */
	public static final List<RegistryObject<Item>> PLAINS = PLAIN_ROWS.stream()
			.map(aRow -> ITEMS.<Item>register(aRow.id(), () -> new GT6Foods.GT6FoodItem(new Item.Properties(), aRow.tooltipKey())))
			.toList();

	/**
	 * The upstream FoodStat → the modern FoodProperties (the {@link GT6Foods#foodProperties}
	 * T1 form — canAlwaysEat is F on every row of this band, no alwaysEdible arm needed;
	 * the builder input is the modifier on BOTH legs, the 21.1 record folds it at build).
	 * Package-private so the offline test pins the SAME construction the registration runs.
	 */
	static FoodProperties foodProperties(CropFoodRow aRow) {
		//? if forge {
		return new FoodProperties.Builder().nutrition(aRow.nutrition()).saturationMod(aRow.saturation()).build();
		//?} else {
		/*return new FoodProperties.Builder().nutrition(aRow.nutrition()).saturationModifier(aRow.saturation()).build();
		//21.1: FoodProperties.Builder.saturationMod → saturationModifier (the GT6Foods fork)
		*///?}
	}

	private GT6CropFoods() {
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
			GT6Mod.LOGGER.info("GT6 crop foods registered: {} food rows + {} inedibles (the T5a band)",
					FOOD_ROWS.size(), PLAIN_ROWS.size());
			GT6Mod.LOGGER.info("GT6 lemon registered: {} (the band head item, meta 0 — the upstream tab icon face)",
					ForgeRegistries.ITEMS.getKey(FOODS.get(0).get()));
		});
	}
}
