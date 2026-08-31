package gregtech6.registry;

import net.minecraft.core.registries.Registries;
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
import gregtech6.items.tools.GTCrowbarItem;

/**
 * The GT6 tool registration home — task p9-tool-crowbar spec ③, the ADR
 * 2026-09-01-p9-tool-crowbar ① surface. Card-owned self-contained
 * {@code @EventBusSubscriber(MOD)} DeferredRegister attached from the construct event
 * (the GTBarrels precedent, ADR 2026-08-31-p8-prefixblocks): GT6Mod.java /
 * GTModBusListener.java stay untouched.
 *
 * <p>No creative tab (ADR ①: the tab is a pool cut, the item is {@code /give}
 * reachable — the tool ladder will get its tab with the tool-family pool card).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6Tools {

	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");

	/**
	 * The formal crowbar — item id {@code gt6:crowbar}. Single steel tier, durability
	 * 512 (the declared ADR value; upstream scales per material, Loader_Tools:128 —
	 * the ladder is a pool cut). Durability semantics: one vanilla point per 10000
	 * upstream tool-damage units, so one cover dismantle = one point.
	 */
	public static final RegistryObject<Item> CROWBAR = ITEMS.register("crowbar",
			() -> new GTCrowbarItem(new Item.Properties().durability(GTCrowbarItem.DURABILITY_POINTS)));

	private GT6Tools() {
	}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (GTBarrels.onModConstruct shape). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		IEventBus tModBus = Mod.EventBusSubscriber.Bus.MOD.bus().get();
		ITEMS.register(tModBus);
	}

	/** Registration smoke evidence (the GTFluids/GTBarrels onCommonSetup log shape). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> GT6Mod.LOGGER.info("GT6 tool registered: {} durability {}",
				ForgeRegistries.ITEMS.getKey(GT6Tools.CROWBAR.get()), GTCrowbarItem.DURABILITY_POINTS));
	}
}
