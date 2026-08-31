package gregtech6.gui.machines;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Machine {@link MenuType} registration, card-owned (ADR-P3-4): the machine family gets its
 * own self-contained DeferredRegister listener (GTOvenMenus :21 shape — GTMenuTypes stays
 * frozen). One MenuType per registered machine ({@code gt6:shredder|crusher|lathe|cokeoven});
 * each factory closes over its own RegistryObject so the shared {@link GTBasicMachineMenu}
 * network constructor binds the type it was opened with. The cokeoven entry is the first
 * multiblock consumer — its factory takes the
 * {@link GTBasicMachineMenu#networkMultiBlock} path (task p8-cokeoven-gui-menu ③).
 */
@Mod.EventBusSubscriber(modid = GTBasicMachinesMenus.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTBasicMachinesMenus {

	public static final String MOD_ID = "gt6";

	private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

	/** Registry paths of the machine GUIs (MenuType ids, lowercase — Loader_MultiTileEntities :1193/:1294-1309). */
	public static final String SHREDDER_MENU_ID = "shredder";
	public static final String CRUSHER_MENU_ID = "crusher";
	public static final String LATHE_MENU_ID = "lathe";
	public static final String COKE_OVEN_MENU_ID = "cokeoven";

	public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MOD_ID);

	public static final RegistryObject<MenuType<GTBasicMachineMenu>> SHREDDER_MENU =
		MENUS.register(SHREDDER_MENU_ID, () -> IForgeMenuType.create((aId, aInv, aData) -> GTBasicMachineMenu.network(GTBasicMachinesMenus.SHREDDER_MENU.get(), aId, aInv, aData)));

	public static final RegistryObject<MenuType<GTBasicMachineMenu>> CRUSHER_MENU =
		MENUS.register(CRUSHER_MENU_ID, () -> IForgeMenuType.create((aId, aInv, aData) -> GTBasicMachineMenu.network(GTBasicMachinesMenus.CRUSHER_MENU.get(), aId, aInv, aData)));

	public static final RegistryObject<MenuType<GTBasicMachineMenu>> LATHE_MENU =
		MENUS.register(LATHE_MENU_ID, () -> IForgeMenuType.create((aId, aInv, aData) -> GTBasicMachineMenu.network(GTBasicMachinesMenus.LATHE_MENU.get(), aId, aInv, aData)));

	public static final RegistryObject<MenuType<GTBasicMachineMenu>> COKE_OVEN_MENU =
		MENUS.register(COKE_OVEN_MENU_ID, () -> IForgeMenuType.create((aId, aInv, aData) -> GTBasicMachineMenu.networkMultiBlock(GTBasicMachinesMenus.COKE_OVEN_MENU.get(), aId, aInv, aData)));

	private GTBasicMachinesMenus() {
	}

	/** The {@code gt6:shredder} menu type (RegistryObject.get fails fast when unbound). */
	public static MenuType<GTBasicMachineMenu> shredder() {
		return SHREDDER_MENU.get();
	}

	/** The {@code gt6:crusher} menu type. */
	public static MenuType<GTBasicMachineMenu> crusher() {
		return CRUSHER_MENU.get();
	}

	/** The {@code gt6:lathe} menu type. */
	public static MenuType<GTBasicMachineMenu> lathe() {
		return LATHE_MENU.get();
	}

	/** The {@code gt6:cokeoven} menu type (the multiblock machine GUI). */
	public static MenuType<GTBasicMachineMenu> cokeoven() {
		return COKE_OVEN_MENU.get();
	}

	/** FMLConstructModEvent = first mod-bus lifecycle stage, strictly before any RegisterEvent (GTMenuTypes.java:76-80 precedent). */
	@SubscribeEvent
	public static void onModConstruct(FMLConstructModEvent aEvent) {
		MENUS.register(Mod.EventBusSubscriber.Bus.MOD.bus().get());
		LOGGER.info("GT6 machine menu 'cokeoven' registered (gt6:cokeoven — the TileEntityBase10MultiBlockMachine GUI path, task p8-cokeoven-gui-menu)");
	}
}
