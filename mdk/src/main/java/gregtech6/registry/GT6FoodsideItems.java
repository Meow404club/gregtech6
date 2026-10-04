package gregtech6.registry;

import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * The foodside small-item band (task vanilla-alias-foodside) — the LoaderItemList
 * vanilla-alias tail + the MultiItemFood remains family + the new-native honey drops,
 * one registration home for the three research.r11-gap-backlog-batch2 sub-faces:
 *
 * <ul>
 * <li><b>The dye vanilla aliases</b> — upstream {@code IL.Dye_SquidInk
 * .set(ST.make(Items.dye, 1, 0))} / {@code IL.Dye_Cactus.set(ST.make(Items.dye, 1, 2))}
 * (LoaderItemList.java:756-757) are NOT GT6 items: they pin the IL seats onto the vanilla
 * dye metas 0/2. The port seam is the same shape one register up: plain handles onto the
 * modern vanilla items {@code minecraft:ink_sac} / {@code minecraft:green_dye} (the
 * GT6RecipesShCL {@code () -> Items.BONE_MEAL} LoaderItemList:762 alias form). Nothing
 * registers — the handles ARE the alias; {@link #DYE_SQUID_INK}/{@link #DYE_CACTUS} are
 * the b4 flower-card anchors (the Tungstus row output IL.Dye_Cactus face). The other
 * LoaderItemList dye aliases (Dye_Bonemeal :755, Dye_Cocoa :758) stay OUT — the card
 * scopes the seam to the two rows the downstream needs.</li>
 * <li><b>The Remains family</b> — the four MultiItemFood.java:113-116 items (metas
 * 12100-12103, names verbatim), each carrying the {@code OD.itemPlantRemains} oredict
 * seat (all four rows list it); the port translation is the shared item tag
 * {@code #gt6:item_plant_remains} (GT6ItemTags.addFoodsideTags, the OD.beamWood
 * beam_wood face).</li>
 * <li><b>The honey drops</b> — <b>FAITHFUL-CALIBER CHANGE (user ruling 2026-10-04, the
 * 全补 ruling)</b>: upstream {@code OD.dropHoney}/{@code OD.dropHoneydew} (OD.java:159-160)
 * are Forestry ore-dict names with NO GT6-native item behind them (the
 * GT6RecipesFood:178-179 TRUE NEGATIVE probe) — downstream consumers (Loader_Recipes_Food
 * .java:534-550 squeeze/juice listeners, the :674-679 coagulator band) fired only in a
 * Forestry environment. The user ruling opens two GT6-NATIVE small items
 * ({@code drop_honey}/{@code drop_honeydew}) so the downstream rows have a port face to
 * unlock against — this card registers the items + pins the tags ONLY, the recipe-row
 * backfill rides the downstream food cards. The ids snake the oredict names (the
 * loader-identity anchor, the bottle_honeydew convention); the display names are
 * new-native wordings ("Honey Drop"/"Honeydew Drop" — no upstream item exists to be
 * verbatim against; "Honeydew Drop" disambiguates from the landed bottle_honeydew
 * "Honeydew"). The icons are COMPOSED placeholders (the P20 stdlib generator convention
 * — no upstream sprite exists to borrow).</li>
 * </ul>
 *
 * <p>All six ride the "GregTech: Nature & Foods" tab (GT6Foods.FOOD_TAB tail append —
 * the four remains are MultiItemFood items, the drops are the honey/bee product domain
 * the tab's food side already hosts). Stack 64 = the vanilla default (the upstream
 * addItem default; no stacksize column on these rows).
 *
 * <p>KJS surface: REGISTRATION face only, deferred to the KJS binding card (the
 * GT6CrystalChargers.java:44 declaration form); the item models + textures + tags are
 * datapack-domain, naturally moddable.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6FoodsideItems {

	/** The DeferredRegister home (the GT6Foods shape — one DR per domain class). */
	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/**
	 * One registration row: the port id, the upstream display name verbatim (the
	 * MultiItemFood.java:113-116 name column; the drops carry new-native wordings), the
	 * upstream meta ({@code -1} = the new-native rows with no upstream addItem line), the
	 * oredict-translation tag path (the shared {@code item_plant_remains} on the four
	 * remains rows, the own {@code drop_*} paths on the drops) and the new-native flag
	 * (the faithful-caliber declaration face the tests pin).
	 */
	public record SideRow(String id, String enName, int meta, String tagPath, boolean newNative) {

		/** The item lang key. */
		public String langKey() {
			return "item.gt6." + id;
		}
	}

	/** The six rows in upstream identity order (the four remains metas ascending, the drops tail). */
	public static final List<SideRow> ROWS = List.of(
			new SideRow("remains_plant"  , "Plant Remains"     , 12100, "item_plant_remains", false), // MultiItemFood.java:113
			new SideRow("remains_fruit"  , "Fruit Remains"     , 12101, "item_plant_remains", false), // :114
			new SideRow("remains_veggie" , "Vegetable Remains" , 12102, "item_plant_remains", false), // :115
			new SideRow("remains_nut"    , "Nut Remains"       , 12103, "item_plant_remains", false), // :116
			new SideRow("drop_honey"     , "Honey Drop"        ,    -1, "drop_honey"       , true ), // new-native, OD.dropHoney (OD.java:159)
			new SideRow("drop_honeydew"  , "Honeydew Drop"     ,    -1, "drop_honeydew"    , true ));// new-native, OD.dropHoneydew (OD.java:160)

	/** The six registered items, {@link #ROWS} order (the census-walk face the tests pin). */
	public static final List<RegistryObject<Item>> ITEMS_LIST = ROWS.stream()
			.map(aRow -> ITEMS.register(aRow.id(), () -> new Item(new Item.Properties())))
			.toList();

	/** The honey drop (the OD.dropHoney carrier — the downstream squeeze/juice listener face). */
	public static final RegistryObject<Item> DROP_HONEY = ITEMS_LIST.get(4);

	/** The honeydew drop (the OD.dropHoneydew carrier — the coagulator band face). */
	public static final RegistryObject<Item> DROP_HONEYDEW = ITEMS_LIST.get(5);

	/**
	 * The {@code IL.Dye_SquidInk} alias — LoaderItemList.java:756 pins it on vanilla dye
	 * meta 0 = the modern {@code minecraft:ink_sac}.
	 */
	public static final java.util.function.Supplier<Item> DYE_SQUID_INK = () -> net.minecraft.world.item.Items.INK_SAC;

	/**
	 * The {@code IL.Dye_Cactus} alias — LoaderItemList.java:757 pins it on vanilla dye
	 * meta 2 = the modern {@code minecraft:green_dye} (the b4 Tungstus row output face).
	 */
	public static final java.util.function.Supplier<Item> DYE_CACTUS = () -> net.minecraft.world.item.Items.GREEN_DYE;

	private GT6FoodsideItems() {
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
}
