package gregtech6.registry;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;

/**
 * The GT6 explosives item registration home — task explosives-chain, the press-row
 * unlock subset of the upstream dynamite family (Loader_MultiTileEntities.java:2236-2238,
 * the "Misc Tool Blocks" MTEs over MultiTileEntityDynamite): Boomstick (meta 32104,
 * MT.Orange), Dynamite (32713, MT.Red) and Strong Dynamite (32712, MT.Purple). The three
 * items are the output/input faces of the RM.Press explosives rows
 * (Loader_Recipes_Other.java:641-655, poured as the press.json dynamite/boomstick band).
 * Card-owned self-contained {@code @EventBusSubscriber(MOD)} DeferredRegister attached from
 * the construct event (the GT6ExtruderMolds shape verbatim; GT6Mod.java stays untouched).
 *
 * <p>Id flattening (the GT6FoodCans ruling): upstream ids were MTE meta ids; the port
 * flattens to one id per item, snake of the IL field name (IL.java:469 —
 * {@code Boomstick, Dynamite, Dynamite_Strong} → {@code boomstick}/{@code dynamite}/
 * {@code dynamite_strong}).
 *
 * <p><b>Declared deviation — the placement face</b>: upstream these are placeable
 * MultiTileEntityDynamite bombs (ignite/explode/fortune), not plain items. The port ships
 * the REGISTRATION identity only (the press rows' recipes need no block face); the
 * placement/ignition behaviour is the pooled face of a future MTE card. The three lookalike
 * variants keep their upstream colour identity through the borrowed greyscale block sprite +
 * the per-item {@code ItemColor} tint over the row material ({@link #tintARGB}, the
 * GT6MoldTintListener seam form; upstream tints the same "colored" sprite via the MTE
 * material pass).
 *
 * <p>The material rides a {@link Supplier} (the GT6Molds.MoldRow lesson: the class-load-time
 * static rows initialize before MT.init(); a direct MT.Red captured null).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Explosives {

	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/**
	 * One registration row — the Loader_MultiTileEntities :2236-2238 projection (path + the
	 * tint material).
	 */
	public record DynamiteRow(String path, Supplier<OreDictMaterial> material) {}

	/** The three rows in upstream meta order (:2236 Boomstick, :2237 Dynamite, :2238 Strong). */
	public static final List<DynamiteRow> ROWS = List.of(
			new DynamiteRow("boomstick", () -> MT.Orange),
			new DynamiteRow("dynamite", () -> MT.Red),
			new DynamiteRow("dynamite_strong", () -> MT.Purple));

	/** The registered items by path. */
	public static final Map<String, RegistryObject<Item>> ITEMS_BY_PATH = new LinkedHashMap<>();

	static {
		for (DynamiteRow tRow : ROWS) {
			ITEMS_BY_PATH.put(tRow.path(), ITEMS.register(tRow.path(), () -> new Item(new Item.Properties())));
		}
	}

	/**
	 * The item tint seam (the GT6MoldTintListener.materialTintARGB form): tint index 0 (the
	 * greyscale body layer) answers the row material's {@code mRGBaSolid}; the overlay layer
	 * and unknown items answer -1 = the sprite renders as-is.
	 */
	public static int tintARGB(@Nullable Item aItem, int aTintIndex) {
		if (aTintIndex != 0 || aItem == null) return -1;
		for (DynamiteRow tRow : ROWS) {
			if (ITEMS_BY_PATH.get(tRow.path()).get() == aItem) return tintARGBByPath(tRow.path(), aTintIndex);
		}
		return -1;
	}

	/** The path-keyed seam face (the offline tests resolve no registry bindings — the sMoldTest two-face form). */
	public static int tintARGBByPath(@Nullable String aPath, int aTintIndex) {
		if (aTintIndex != 0 || aPath == null) return -1;
		for (DynamiteRow tRow : ROWS) {
			if (tRow.path().equals(aPath)) {
				OreDictMaterial tMaterial = tRow.material().get();
				return 0xFF000000 | (tMaterial.mRGBaSolid[0] << 16) | (tMaterial.mRGBaSolid[1] << 8) | tMaterial.mRGBaSolid[2];
			}
		}
		return -1;
	}

	private GT6Explosives() {
	}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (GT6ExtruderMolds.onModConstruct shape). */
	@net.minecraftforge.eventbus.api.SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		//?} else {
		/*IEventBus tModBus = net.neoforged.fml.ModList.get().getModContainerById("gt6").orElseThrow().getEventBus();
		 *///?}
		ITEMS.register(tModBus);
	}

	/**
	 * The MACHINES_TAB join (the GT6SlicerBlades.onBuildTabContents verbatim form): upstream
	 * the three ride the "Misc Tool Blocks" MTE tab; the port pools them with the machines
	 * tab. Registration without a tab = creative-menu and JEI double blindness (issue#10).
	 */
	@net.minecraftforge.eventbus.api.SubscribeEvent
	public static void onBuildTabContents(BuildCreativeModeTabContentsEvent aEvent) {
		if (aEvent.getTabKey().location().equals(GTMachines.MACHINES_TAB.getId())) {
			for (RegistryObject<Item> tItem : ITEMS_BY_PATH.values()) {
				aEvent.accept(new ItemStack(tItem.get()));
			}
		}
	}
}
