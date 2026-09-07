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
import gregtech6.item.spraycan.GTSprayCanItem;

/**
 * The GT6 spray-can registration home — task p22-spraycan-items. Card-owned self-contained
 * {@code @EventBusSubscriber(MOD)} DeferredRegister attached from the construct event (the
 * GT6Tools/GTBarrels precedent; GT6Mod.java / GTModBusListener.java stay untouched).
 *
 * <p>18 items + the family tab (the per-family tab discipline): the 16 colour cans ({@link
 * GTSprayCanItem#SPRAY_USES} 512 uses, MultiItemRandomTools.java:245), the paint removal
 * spray ({@link GTSprayCanItem#REMOVER_USES} 256 uses, :271) and the empty can (upstream
 * IL.Spray_Empty :235 — the depletion swap target, NOT refillable until the Canner pool
 * card). v1 acquisition = the creative tab + {@code /give} (the card spec: no crafting,
 * no refill). Upstream ids were meta ids on the MultiItemRandomTools meta item
 * (999/1000+2i/1096); the port flattens to one id per can, snake of the upstream
 * {@code "Spray Paint (" + DYE_NAMES[i] + ")"} display rows :243/:269/:235.
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6SprayCans {

	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");
	public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "gt6");

	/** The tab title lang key — the single source both the builder and the GT6EnUs datagen row use. */
	public static final String TAB_TITLE_KEY = "itemGroup.gt6.spray_cans";

	/** The empty spray can — id {@code gt6:spray_can_empty}, the depletion swap target (upstream :235). */
	public static final RegistryObject<Item> SPRAY_CAN_EMPTY = ITEMS.register("spray_can_empty",
			() -> new Item(new Item.Properties()));

	/** The 16 colour cans, indexed by the GT6 dye index 0=Black..15=White ({@link GTSprayCanItem#DYE_IDS}). */
	public static final List<RegistryObject<Item>> SPRAY_PAINTS = buildSprayPaints();

	/** The paint removal spray — id {@code gt6:spray_paint_remover}, 256 uses (upstream :269/:271). */
	public static final RegistryObject<Item> SPRAY_PAINT_REMOVER = ITEMS.register("spray_paint_remover",
			() -> new GTSprayCanItem(() -> SPRAY_CAN_EMPTY.get(), GTSprayCanItem.REMOVER_USES,
					GTSprayCanItem.REMOVER, new Item.Properties().stacksTo(1)));

	/**
	 * The 16 colour-can rows — the MultiItemRandomTools.java:243 loop flattened: one item per
	 * dye ({@code "Spray Paint (" + DYE_NAMES[i] + ")"}), 512 uses, stacksTo(1) (upstream
	 * Behavior_Spray_Color.java:61 refuses stacks != 1; the port makes it structural).
	 */
	private static List<RegistryObject<Item>> buildSprayPaints() {
		java.util.ArrayList<RegistryObject<Item>> rList = new java.util.ArrayList<>(16);
		for (byte i = 0; i < 16; i++) {
			byte tIndex = i;
			rList.add(ITEMS.register("spray_paint_" + GTSprayCanItem.DYE_IDS[i],
					() -> new GTSprayCanItem(() -> SPRAY_CAN_EMPTY.get(), GTSprayCanItem.SPRAY_USES, tIndex,
							new Item.Properties().stacksTo(1))));
		}
		return java.util.List.copyOf(rList);
	}

	/**
	 * The tab display table — the 16 colour cans in dye order, then the remover, then the
	 * empty can. Table-driven so the offline test asserts the shape + the ITEMS parity
	 * without resolving {@code get()} (the GT6ToolsCreativeTabTest face).
	 */
	public static final List<RegistryObject<Item>> TAB_TABLE = buildTabTable();

	private static List<RegistryObject<Item>> buildTabTable() {
		java.util.ArrayList<RegistryObject<Item>> rList = new java.util.ArrayList<>(SPRAY_PAINTS);
		rList.add(SPRAY_PAINT_REMOVER);
		rList.add(SPRAY_CAN_EMPTY);
		return java.util.List.copyOf(rList);
	}

	/**
	 * The "Spray Cans" tab — id {@code gt6:spray_cans}, title key {@link #TAB_TITLE_KEY}, icon
	 * the black spray can (dye index 0). The upstream analogue: the cans live in the
	 * ToolsGT/randomtools meta-item creative category (MultiItemRandomTools) — the port gives
	 * them their own family tab (the per-family discipline).
	 */
	public static final RegistryObject<CreativeModeTab> SPRAY_CANS_TAB = CREATIVE_MODE_TABS.register("spray_cans",
			() -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
					.title(Component.translatable(TAB_TITLE_KEY))
					.icon(() -> new ItemStack(SPRAY_PAINTS.get(0).get()))
					.displayItems((aParameters, aOutput) -> {
						for (RegistryObject<Item> tRow : TAB_TABLE) {
							aOutput.accept(new ItemStack(tRow.get()));
						}
					})
					.build());

	private GT6SprayCans() {
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

	/** Registration smoke evidence (the GT6Tools onCommonSetup log shape). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> {
			GT6Mod.LOGGER.info("GT6 spray cans registered: {} colours x {} uses, remover {} uses, empty swap target {}",
					SPRAY_PAINTS.size(), GTSprayCanItem.SPRAY_USES, GTSprayCanItem.REMOVER_USES,
					ForgeRegistries.ITEMS.getKey(SPRAY_CAN_EMPTY.get()));
			// the registry lookup (not the field name) makes this line real registration
			// evidence — an unregistered tab would throw here and fail the runServer gate.
			GT6Mod.LOGGER.info("GT6 creative tab registered: {} ({} display rows)",
					BuiltInRegistries.CREATIVE_MODE_TAB.getKey(SPRAY_CANS_TAB.get()), TAB_TABLE.size());
		});
	}
}
