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
 * The GT6 bee-comb registration home — task p31-bees-lv1, the 20-comb static chain of the
 * upstream {@code MultiItemFood} bee block (MultiItemFood.java:226-247, metas 30000-30009 /
 * 30100-30105 / 30200-30203, {@code OD.beeComb}; the six crossbred-family combs carry
 * {@code OD.beeCombCrossbred} upstream — a tag-face the port pools with the bee-item
 * domain). Card-owned self-contained {@code @EventBusSubscriber(MOD)} DeferredRegister
 * attached from the construct event (the GT6FoodCans precedent verbatim).
 *
 * <p>Id flattening (the GT6SprayCans/GT6FoodCans ruling): upstream ids were metas on the
 * {@code MultiItemFood} meta item; the port flattens to one id per comb, snake of the
 * upstream English name ({@code "Honey Comb"} → {@code comb_honey}). The combs are PLAIN
 * items — the upstream FoodStat eat-face, the Sandwiches.INGREDIENTS registration and the
 * Thaumcraft aspects all pool with the food/bee-behaviour domains (Lv1 is the static
 * chain: no eat, no bees, no worldgen).
 *
 * <p>Upstream order is preserved in {@link #COMBS} (declaration order = the meta order
 * 30000..30009, 30100..30105, 30200..30203) — the recipe provider's transcription walk and
 * the offline tests read THIS list exactly like upstream indexes the meta ranges.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6BeeCombs {

	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");
	public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "gt6");

	/** The tab title lang key — the single source both the builder and the GT6EnUs datagen row use. */
	public static final String TAB_TITLE_KEY = "itemGroup.gt6.bee";

	/**
	 * One comb declaration row: the snake id fragment (the {@code comb_} prefix rides the
	 * register call), the upstream English display name (MultiItemFood.java:226-247 — the
	 * GT6EnUs datagen's single source), and the upstream meta kept for traceability.
	 */
	public record CombSpec(String name, String display, int meta) {
		/** The gt6 registry path — {@code comb_} + the snake fragment. */
		public String itemId() {return "comb_" + name;}
	}

	/**
	 * The 20 upstream combs in meta order. Display names verbatim (MultiItemFood.java:226-247;
	 * "Shroomy Comb" is the upstream :233 spelling). The FoodStat/sandwich/thaumcraft faces
	 * of the five edible combs (Honey/Water/Jungle/Shroom/Royal) pool with the food domain.
	 */
	public static final List<CombSpec> COMB_SPECS = List.of(
			new CombSpec("honey"    , "Honey Comb"    , 30000),
			new CombSpec("water"    , "Water Comb"    , 30001),
			new CombSpec("magic"    , "Magic Comb"    , 30002),
			new CombSpec("nether"   , "Nether Comb"   , 30003),
			new CombSpec("end"      , "End Comb"      , 30004),
			new CombSpec("rock"     , "Rock Comb"     , 30005),
			new CombSpec("jungle"   , "Jungle Comb"   , 30006),
			new CombSpec("frozen"   , "Frozen Comb"   , 30007),
			new CombSpec("shroom"   , "Shroomy Comb"  , 30008),
			new CombSpec("sandy"    , "Sandy Comb"    , 30009),
			new CombSpec("clay"     , "Clay Comb"     , 30100),
			new CombSpec("sticky"   , "Sticky Comb"   , 30101),
			new CombSpec("royal"    , "Royal Comb"    , 30102),
			new CombSpec("soul"     , "Soul Comb"     , 30103),
			new CombSpec("amnesic"  , "Amnesic Comb"  , 30104),
			new CombSpec("military" , "Military Comb" , 30105),
			new CombSpec("pyro"     , "Pyro Comb"     , 30200),
			new CombSpec("cryo"     , "Cryo Comb"     , 30201),
			new CombSpec("aero"     , "Aero Comb"     , 30202),
			new CombSpec("tera"     , "Tera Comb"     , 30203));

	/** The 20 registered combs, index-aligned with {@link #COMB_SPECS} (the provider/test walk). */
	public static final List<RegistryObject<Item>> COMBS = COMB_SPECS.stream()
			.map(tSpec -> ITEMS.register(tSpec.itemId(), () -> new Item(new Item.Properties())))
			.toList();

	/** The comb handle of a snake fragment, or null (the provider's input seam). */
	public static RegistryObject<Item> comb(String aName) {
		for (int i = 0; i < COMB_SPECS.size(); i++) if (COMB_SPECS.get(i).name().equals(aName)) return COMBS.get(i);
		return null;
	}

	/**
	 * The "Bees" tab — id {@code gt6:bee}, title key {@link #TAB_TITLE_KEY}, icon the honey
	 * comb (the chain's entry item). The upstream analogue: the MultiItemFood members ride
	 * the food multiitem tab (the port's per-family tab discipline, GT6FoodCans precedent).
	 */
	public static final RegistryObject<CreativeModeTab> BEE_TAB = CREATIVE_MODE_TABS.register("bee",
			() -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
					.title(Component.translatable(TAB_TITLE_KEY))
					.icon(() -> new ItemStack(COMBS.get(0).get()))
					.displayItems((aParameters, aOutput) -> {
						for (RegistryObject<Item> tRow : COMBS) {
							aOutput.accept(new ItemStack(tRow.get()));
						}
						aOutput.accept(new ItemStack(GT6BeeHives.HIVE_ITEM.get())); // task p34-bumbliary-recipes — the R2 carryable hive rides the bee tab
					})
					.build());

	private GT6BeeCombs() {
	}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (GT6FoodCans.onModConstruct shape). */
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
			// the registry lookups (not the field names) make these lines real registration
			// evidence — an unregistered item/tab would throw here and fail the runServer gate.
			GT6Mod.LOGGER.info("GT6 bee combs registered: {} (first {})", COMBS.size(),
					ForgeRegistries.ITEMS.getKey(COMBS.get(0).get()));
			GT6Mod.LOGGER.info("GT6 creative tab registered: {} ({} display rows)",
					BuiltInRegistries.CREATIVE_MODE_TAB.getKey(BEE_TAB.get()), COMBS.size());
		});
	}
}
