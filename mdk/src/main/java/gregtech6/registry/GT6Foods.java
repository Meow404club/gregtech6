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
 * tiers.T1/tiers.item_minimum). Card-owned self-contained {@code @EventBusSubscriber(MOD)}
 * DeferredRegister attached from the construct event (the GT6FoodCans precedent verbatim;
 * GT6Mod.java / GTModBusListener.java stay untouched).
 *
 * <p>The 9 rows, in upstream meta order (each row = one upstream {@code addItem} line):
 * <ol>
 * <li>{@code food_cheese} — meta 1000 "Cheese" (MultiItemFood.java:490).</li>
 * <li>{@code food_cheese_sliced} — meta 1001 "Cheese Slice" (:491).</li>
 * <li>{@code food_brown_egg_boiled} — meta 1060 "Boiled Egg" (:497).</li>
 * <li>{@code food_white_egg_boiled} — meta 1061 "Boiled Egg" (:498; the brown twin is the
 *     vanilla egg alias :495, the white twin is the GT6 egg item :496 — both boiled rows
 *     carry the identical FoodStat, the id suffix is the IL name
 *     {@code Food_Brown_Egg_Boiled}/{@code Food_White_Egg_Boiled}).</li>
 * <li>{@code food_potato_chips} — meta 9010 "Potato Chips" (:365).</li>
 * <li>{@code food_chili_chips} — meta 9020 "Chili Chips" (:374).</li>
 * <li>{@code food_ice_cream} — meta 13000 "Ice Cream" (:809; the 37-flavour family
 *     :810-845 stays the T3 pool).</li>
 * <li>{@code food_butter} — meta 32117 "Butter" (:933).</li>
 * <li>{@code food_butter_salted} — meta 32119 "Salted Butter" (:934).</li>
 * </ol>
 *
 * <p>The eat face — the upstream FoodStat translated to the modern FoodProperties with
 * the nutrition/saturationModifier literals verbatim ({@link #foodProperties}; canAlwaysEat
 * is F on every row: the 4-flag tail {@code F, T, F, T} reads
 * alwaysEdible/invisibleParticles/isRotten/autoDetectEmpty, FoodStat.java:64-67 ctor order).
 *
 * <p>POOLED (declared): the extended FoodStat channels (hydration/temperature/
 * temperatureEffect/alcohol/caffeine/dehydration/sugar/fat/radiation — the modern
 * FoodProperties carries no positions for them, the GTDrinks.java:23-31 drink-seam
 * declaration); the GT6 custom potion effects (ID_SLIPPERY on the butters
 * MultiItemFood.java:933-934, ID_FLAMMABLE on the chili chips :374 — the PotionsGT ids are
 * not registered in the port, the vanilla effect slot cannot carry them); the
 * OreDictItemData faces (the slice's Cheese U4 :491, the butters' ingot targets :933-934);
 * the Sandwiches.INGREDIENTS seats; the TC aspects; the IItemRottable rot face; the
 * {@code setFluidContainerStats(0, 8)} faces (:364-375); and every recipe row over the 9
 * (the kX slicing :492/:504, smelting, boxinator rows — the T1b row-backfill card's
 * surface, unlocked by this card).
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
	 * {@code aFoodLevel, aSaturation} literals (the FoodProperties face), and the
	 * registration-row desc tooltip verbatim (the GTPistolItem.Kind tooltipKey face).
	 */
	public record FoodRow(String id, String enName, int nutrition, float saturation, String enTooltip) {
		/** The desc tooltip lang key (the GT6LaserGas tooltip-key-in-ctor face). */
		public String tooltipKey() {
			return "item.gt6." + id + ".tooltip";
		}
	}

	/**
	 * The 9 rows in upstream meta order (MultiItemFood.java:490/:491/:497/:498/:365/:374/
	 * :809/:933/:934) — the registration, tab, datagen and test walk all ride THIS table
	 * so the faces cannot drift.
	 */
	public static final List<FoodRow> FOOD_ROWS = List.of(
			new FoodRow("food_cheese", "Cheese", 2, 1.2F, "Click the Cheese"),
			new FoodRow("food_cheese_sliced", "Cheese Slice", 1, 0.6F, "ALIEN ATTACK!!!, throw the CHEEEEESE!!!"),
			new FoodRow("food_brown_egg_boiled", "Boiled Egg", 2, 1.2F, "Did you expect this to look different after boiling?"),
			new FoodRow("food_white_egg_boiled", "Boiled Egg", 2, 1.2F, "Did you expect this to look different after boiling?"),
			new FoodRow("food_potato_chips", "Potato Chips", 7, 1.2F, "Crunchy"),
			new FoodRow("food_chili_chips", "Chili Chips", 7, 1.2F, "Spicy"),
			new FoodRow("food_ice_cream", "Ice Cream", 1, 0.6F, "Basic Milk Gelato"),
			new FoodRow("food_butter", "Butter", 1, 4.0F, "A chunk of pure Fat"),
			new FoodRow("food_butter_salted", "Salted Butter", 1, 4.0F, "As if it wasn't unhealthy already"));

	/** The 9 registered foods, table order (the {@link #FOOD_ROWS} walk). */
	public static final List<RegistryObject<Item>> FOODS = FOOD_ROWS.stream()
			.map(aRow -> ITEMS.<Item>register(aRow.id(), () -> new GT6FoodItem(new Item.Properties().food(foodProperties(aRow)), aRow.tooltipKey())))
			.toList();

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
		return new FoodProperties.Builder().nutrition(aRow.nutrition()).saturationMod(aRow.saturation()).build();
		//?} else {
		/*return new FoodProperties.Builder().nutrition(aRow.nutrition()).saturationModifier(aRow.saturation()).build();
		//21.1: FoodProperties.Builder.saturationMod → saturationModifier (the GT6SandwichItem fork)
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
