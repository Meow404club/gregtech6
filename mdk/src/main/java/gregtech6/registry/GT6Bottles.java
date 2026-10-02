package gregtech6.registry;

import java.util.List;

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
 * The GT6 bottles registration home — task food-bottles-min: the MINIMUM 4-bottle subset
 * of the upstream {@code MultiItemBottles} domain (441 lines, ~150 bottles + the 48 dye
 * bottles — the pool card) that the bottle-unlocked crafting tail needs: the ketchup
 * ladder (MultiItemFood.java:643-:647), the heavy-cream cake (:636) and the BBQ ribs
 * (:550). Landing these rows also closes the pizza dead end — {@code food_dough_flat_
 * ketchup} had zero producers, so the already-landed {@code bake_pizza_cheese_raw}/
 * {@code bake_pizza_meat_raw} rows were unreachable (the state research.food-crafting-tail
 * "活缺陷_披萨死端" finding).
 *
 * <p>Rows (each = one upstream identity, the food-domain snake flattening over the
 * oredict identity the crafting rows consume):
 * <ul>
 * <li>{@code bottle_empty} — the empty glass bottle: upstream {@code OP.bottle.dat(MT.Empty)}
 *     (OP.java:229, the IS_CONTAINER/SELF_REFERENCING material-prefix technical container;
 *     local name template " Bottle" over the material word "Empty").</li>
 * <li>{@code food_ketchup} — the Tomato Ketchup bottle (meta 3101, MultiItemBottles.java:
 *     :257, oredict {@code foodKetchup}).</li>
 * <li>{@code food_barbecuesauce} — the Barbecue Sauce bottle (meta 805, :111, oredict
 *     {@code foodBarbecuesauce}).</li>
 * <li>{@code food_heavycream} — the Heavy Cream bottle (meta 1101, :139, oredict
 *     {@code bottleCream} RE-REGISTERED to {@code foodHeavycream},
 *     LoaderOreDictReRegistrations.java:870) — the re-registration is an identity in the
 *     port (one id, one item, the crafting face consumes the final name).</li>
 * </ul>
 *
 * <p>DECLARED DEVIATION (the main-session ruling 2026-10-02, the small case b): upstream
 * these are fluid-container items (the FoodStatFluid 250mB drink face, IS_CONTAINER +
 * SELF_REFERENCING, OP.java:229) — that would be the port's FIRST capability container.
 * This card registers them as PLAIN items; the drink/fill capability channel and the
 * machine filling rows defer to the independent content decision card (P10 TRUE NEGATIVE
 * does not apply: 1.7.10 GT6-only registers these bottles itself, no foreign mod owns
 * them).
 *
 * <p>POOLED (declared): the FoodStatFluid drink face + the OD.container250* container
 * faces + the TC aspects + the Sandwiches.INGREDIENTS seats + the IItemRottable rot face
 * (:436, the drunk-bottle rot) + every other bottle of the domain (the pool card).
 *
 * <p>The "GregTech: Bottles" tab (id {@code gt6:bottles}, MultiItemBottles.java:40) — the
 * upstream icon is meta 1600 "Purple Drink", which is NOT in the minimum subset, so the
 * port icon is the domain-entry empty bottle (declared deviation).
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

	/** The empty glass bottle — {@code OP.bottle.dat(MT.Empty)}, OP.java:229 (the declared plain-item face). */
	public static final RegistryObject<Item> BOTTLE_EMPTY = ITEMS.register("bottle_empty", () -> new Item(new Item.Properties()));

	/** The Tomato Ketchup bottle — meta 3101, MultiItemBottles.java:257 (oredict foodKetchup). */
	public static final RegistryObject<Item> FOOD_KETCHUP = ITEMS.register("food_ketchup", () -> new Item(new Item.Properties()));

	/** The Barbecue Sauce bottle — meta 805, MultiItemBottles.java:111 (oredict foodBarbecuesauce). */
	public static final RegistryObject<Item> FOOD_BARBECUESAUCE = ITEMS.register("food_barbecuesauce", () -> new Item(new Item.Properties()));

	/**
	 * The Heavy Cream bottle — meta 1101, MultiItemBottles.java:139 (oredict bottleCream,
	 * re-registered to foodHeavycream by LoaderOreDictReRegistrations.java:870 — the port
	 * id IS the final crafting-consumable name).
	 */
	public static final RegistryObject<Item> FOOD_HEAVYCREAM = ITEMS.register("food_heavycream", () -> new Item(new Item.Properties()));

	/** The 4 registered bottles, upstream identity order (empty, ketchup :257, BBQ :111, cream :139). */
	public static final List<RegistryObject<Item>> BOTTLES = List.of(BOTTLE_EMPTY, FOOD_KETCHUP, FOOD_BARBECUESAUCE, FOOD_HEAVYCREAM);

	/**
	 * The "GregTech: Bottles" tab — id {@code gt6:bottles}, title key {@link #TAB_TITLE_KEY},
	 * icon the empty bottle (the domain-entry item; the upstream icon meta 1600 "Purple
	 * Drink" is pool, MultiItemBottles.java:40 "GregTech: Bottles", the per-multiitem tab
	 * discipline).
	 */
	public static final RegistryObject<CreativeModeTab> BOTTLES_TAB = CREATIVE_MODE_TABS.register("bottles",
			() -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
					.title(Component.translatable(TAB_TITLE_KEY))
					.icon(() -> new ItemStack(BOTTLE_EMPTY.get()))
					.displayItems((aParameters, aOutput) -> {
						for (RegistryObject<Item> tRow : BOTTLES) {
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
