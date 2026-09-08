package gregtech6.registry;

import java.util.ArrayList;
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
import net.minecraftforge.registries.RegistryObject;

import gregtech6.GT6Mod;
import gregtech6.item.foamspray.GT6FoamSprayItem;
import gregtech6.item.spraycan.GTSprayCanItem;

/**
 * The GT6 C-Foam spray registration home — task p25-c-foam-pipe-spray spec ①. The
 * {@link GT6SprayCans} structure verbatim (card-owned self-contained {@code @EventBusSubscriber}
 * DeferredRegister attached from the construct event; GT6Mod/GTModBusListener untouched).
 *
 * <p>32 items + the family tab (the per-family tab discipline): the 16 C-Foam Sprays (256
 * uses, owned=F — upstream ids 1100+2i, MultiItemRandomTools.java:251-253) and the 16
 * Advanced owned variants (256 uses, owned=T — upstream ids 1132+2i, :259-261 "Full
 * (C-Foam only breakable by Owner once dry)"). The p22 flattening applies: one item per
 * colour, {@code stacksTo(1)} (upstream :70 refuses stacks != 1), the used can CUT. The
 * depletion swap target is the SHARED {@link GT6SprayCans#SPRAY_CAN_EMPTY} ({@code
 * gt6:spray_can_empty} — one empty can across the whole spray domain, upstream :235).
 * v1 acquisition = the creative tab + {@code /give} (the p22 form: no crafting, no Canner
 * refill — refill/硬化/清除剂 are pool rows, the card note).
 */
@Mod.EventBusSubscriber(modid = "gt6", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GT6FoamSprays {

	public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "gt6");
	public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "gt6");

	/** The tab title lang key — the single source both the builder and the GT6EnUs datagen row use. */
	public static final String TAB_TITLE_KEY = "itemGroup.gt6.foam_sprays";

	/** The 16 C-Foam Sprays, indexed by the GT6 dye index 0=Black..15=White ({@link GTSprayCanItem#DYE_IDS}). */
	public static final List<RegistryObject<Item>> FOAM_SPRAYS = buildFoamSprays(false);

	/** The 16 Advanced (owned) C-Foam Sprays, same order. */
	public static final List<RegistryObject<Item>> FOAM_SPRAYS_OWNED = buildFoamSprays(true);

	/**
	 * The loop-flattened registration rows — the MultiItemRandomTools.java:251-264 loop
	 * over the two id ladders: {@code foam_spray_<dye>} (owned=F, the 1100+2i row) and
	 * {@code foam_spray_owned_<dye>} (owned=T, the 1132+2i "Advanced" row). Both capacities
	 * are 256 uses (the upstream Behavior_Spray_Foam ctor arguments :253/:261).
	 */
	private static List<RegistryObject<Item>> buildFoamSprays(boolean aOwned) {
		ArrayList<RegistryObject<Item>> rList = new ArrayList<>(16);
		for (byte i = 0; i < 16; i++) {
			byte tIndex = i;
			rList.add(ITEMS.register((aOwned ? "foam_spray_owned_" : "foam_spray_") + GTSprayCanItem.DYE_IDS[i],
					() -> new GT6FoamSprayItem(() -> GT6SprayCans.SPRAY_CAN_EMPTY.get(), tIndex, aOwned,
							new Item.Properties().stacksTo(1))));
		}
		return java.util.List.copyOf(rList);
	}

	/**
	 * The tab display table — the 16 plain cans in dye order, then the 16 Advanced. 
	 * Table-driven so the offline test asserts the shape + the ITEMS parity without
	 * resolving {@code get()} (the GT6SprayCans TAB_TABLE face).
	 */
	public static final List<RegistryObject<Item>> TAB_TABLE = buildTabTable();

	private static List<RegistryObject<Item>> buildTabTable() {
		ArrayList<RegistryObject<Item>> rList = new ArrayList<>(FOAM_SPRAYS);
		rList.addAll(FOAM_SPRAYS_OWNED);
		return java.util.List.copyOf(rList);
	}

	/**
	 * The "C-Foam Sprays" tab — id {@code gt6:foam_sprays}, title key {@link #TAB_TITLE_KEY},
	 * icon the black C-Foam Spray (dye index 0). The upstream analogue: the cans live in
	 * the ToolsGT/randomtools meta-item creative category (MultiItemRandomTools) — the port
	 * gives them their own family tab (the per-family discipline).
	 */
	public static final RegistryObject<CreativeModeTab> FOAM_SPRAYS_TAB = CREATIVE_MODE_TABS.register("foam_sprays",
			() -> CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
					.title(Component.translatable(TAB_TITLE_KEY))
					.icon(() -> new ItemStack(FOAM_SPRAYS.get(0).get()))
					.displayItems((aParameters, aOutput) -> {
						for (RegistryObject<Item> tRow : TAB_TABLE) {
							aOutput.accept(new ItemStack(tRow.get()));
						}
					})
					.build());

	private GT6FoamSprays() {
	}

	/** FMLConstructModEvent = the first mod-bus lifecycle stage (GT6SprayCans.onModConstruct shape). */
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

	/** Registration smoke evidence (the GT6SprayCans onCommonSetup log shape). */
	@SubscribeEvent
	public static void onCommonSetup(FMLCommonSetupEvent aEvent) {
		aEvent.enqueueWork(() -> {
			GT6Mod.LOGGER.info("GT6 foam sprays registered: {} + {} owned x {} uses, empty swap target {}",
					FOAM_SPRAYS.size(), FOAM_SPRAYS_OWNED.size(), GT6FoamSprayItem.FOAM_USES,
					net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(GT6SprayCans.SPRAY_CAN_EMPTY.get()));
			// the registry lookup (not the field name) makes this line real registration
			// evidence — an unregistered tab would throw here and fail the runServer gate.
			GT6Mod.LOGGER.info("GT6 creative tab registered: {} ({} display rows)",
					BuiltInRegistries.CREATIVE_MODE_TAB.getKey(FOAM_SPRAYS_TAB.get()), TAB_TABLE.size());
		});
	}
}
