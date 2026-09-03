package gregtech6.gui.machines;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
//? if forge {
import net.minecraftforge.registries.RegistryObject;
//?} else {
/*import net.neoforged.neoforge.registries.DeferredHolder;
// 21.1: RegistryObject → DeferredHolder (one extra generic parameter, javap
// neoforge-21.1.249); not swap-able, forked per file (ADR-P15-3 r1 priority 3).
 *///?}

/**
 * Machine {@link MenuType} registration, card-owned (ADR-P3-4): the oven gets its own
 * self-contained DeferredRegister listener because gregtech6.gui.GTMenuTypes is outside the
 * task's files scope (frozen like GT6Mod.java). Form = the DeferredRegister +
 * {@code Bus.MOD.bus().get()} idiom the GTMenuTypes doc prescribes for new registrations
 * ("new registrations standardise on this one", GTMenuTypes.java:52-54).
 */
@Mod.EventBusSubscriber(modid = GTOvenMenus.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTOvenMenus {

	public static final String MOD_ID = "gt6";

	/** Registry path of the oven GUI (MenuType id {@code gt6:oven}). */
	public static final String OVEN_MENU_ID = "oven";

	public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MOD_ID);

	//? if forge {
	public static final RegistryObject<MenuType<GTOvenMenu>> OVEN_MENU =
		MENUS.register(OVEN_MENU_ID, () -> IForgeMenuType.create(GTOvenMenu::new));
	//?} else {
	/*public static final DeferredHolder<MenuType<?>, MenuType<GTOvenMenu>> OVEN_MENU =
		MENUS.register(OVEN_MENU_ID, () -> IForgeMenuType.create(GTOvenMenu::new));
	 *///?}

	private GTOvenMenus() {
	}

	/** The {@code gt6:oven} menu type (RegistryObject.get fails fast when unbound). */
	public static MenuType<GTOvenMenu> oven() {
		return OVEN_MENU.get();
	}

	/** FMLConstructModEvent = first mod-bus lifecycle stage, strictly before any RegisterEvent (GTMenuTypes.java:76-80 precedent). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		//? if forge {
		MENUS.register(Mod.EventBusSubscriber.Bus.MOD.bus().get());
		//?} else {
		/*// 21.1: the self-contained bus source is the container (ModContainer.getEventBus,
		// loader-4.0.44 javap; Bus.MOD.bus() has no accessor there) — GTMenuTypes.onModConstruct form.
		MENUS.register(net.neoforged.fml.ModList.get().getModContainerById(MOD_ID).orElseThrow().getEventBus());
		 *///?}
	}
}
