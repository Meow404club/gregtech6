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
 * Machine {@link MenuType} registration, card-owned (ADR-P3-4): the machine family gets its
 * own self-contained DeferredRegister listener (GTOvenMenus :21 shape — GTMenuTypes stays
 * frozen). One MenuType per legacy-row machine ({@code gt6:cokeoven|dryer|canner}); each
 * factory closes over its own RegistryObject so the shared {@link GTBasicMachineMenu}
 * network constructor binds the type it was opened with. The cokeoven entry is the first
 * multiblock consumer — its factory takes the
 * {@link GTBasicMachineMenu#networkMultiBlock} path (task p8-cokeoven-gui-menu ③).
 *
 * <p>The shredder/crusher/lathe trio has NO vanilla MenuType since task
 * p26-mui-a-menu-deregistration: the GUI opens through ModularUI (the
 * {@code GT6MuiMachine} open chain, task p26-mui-a-open-chain), and the family marks that
 * with a null menu supplier (TileEntityBasicMachine mMenuType semantics). Per the
 * {@code gt6:*} MenuType freeze ruling, no new machine-family MenuType gets registered —
 * a ported machine without legacy rows is ModularUI-only.
 */
@Mod.EventBusSubscriber(modid = GTBasicMachinesMenus.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTBasicMachinesMenus {

	public static final String MOD_ID = "gt6";

	private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

	/** Registry paths of the machine GUIs (MenuType ids, lowercase — Loader_MultiTileEntities :1193/:1294-1309/:1477-1480). */
	public static final String COKE_OVEN_MENU_ID = "cokeoven";
	/** Registry path of the Dryer GUI (task p16-machine-fluid-gui ① — the p14-dryer-family row.menu pool promise redeemed). */
	public static final String DRYER_MENU_ID = "dryer";
	/** Registry path of the Canner GUI (task p24-canner-machine — upstream machines/Canner.png lowercased). */
	public static final String CANNER_MENU_ID = "canner";

	public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MOD_ID);

	//? if forge {
	public static final RegistryObject<MenuType<GTBasicMachineMenu>> COKE_OVEN_MENU =
		MENUS.register(COKE_OVEN_MENU_ID, () -> IForgeMenuType.create((aId, aInv, aData) -> GTBasicMachineMenu.networkMultiBlock(GTBasicMachinesMenus.COKE_OVEN_MENU.get(), aId, aInv, aData)));

	public static final RegistryObject<MenuType<GTBasicMachineMenu>> DRYER_MENU =
		MENUS.register(DRYER_MENU_ID, () -> IForgeMenuType.create((aId, aInv, aData) -> GTBasicMachineMenu.network(GTBasicMachinesMenus.DRYER_MENU.get(), aId, aInv, aData)));

	public static final RegistryObject<MenuType<GTBasicMachineMenu>> CANNER_MENU =
		MENUS.register(CANNER_MENU_ID, () -> IForgeMenuType.create((aId, aInv, aData) -> GTBasicMachineMenu.network(GTBasicMachinesMenus.CANNER_MENU.get(), aId, aInv, aData)));
	//?} else {
	/*public static final DeferredHolder<MenuType<?>, MenuType<GTBasicMachineMenu>> COKE_OVEN_MENU =
		MENUS.register(COKE_OVEN_MENU_ID, () -> IForgeMenuType.create((aId, aInv, aData) -> GTBasicMachineMenu.networkMultiBlock(GTBasicMachinesMenus.COKE_OVEN_MENU.get(), aId, aInv, aData)));

	public static final DeferredHolder<MenuType<?>, MenuType<GTBasicMachineMenu>> DRYER_MENU =
		MENUS.register(DRYER_MENU_ID, () -> IForgeMenuType.create((aId, aInv, aData) -> GTBasicMachineMenu.network(GTBasicMachinesMenus.DRYER_MENU.get(), aId, aInv, aData)));

	public static final DeferredHolder<MenuType<?>, MenuType<GTBasicMachineMenu>> CANNER_MENU =
		MENUS.register(CANNER_MENU_ID, () -> IForgeMenuType.create((aId, aInv, aData) -> GTBasicMachineMenu.network(GTBasicMachinesMenus.CANNER_MENU.get(), aId, aInv, aData)));
	 *///?}

	private GTBasicMachinesMenus() {
	}

	/** The {@code gt6:cokeoven} menu type (the multiblock machine GUI). */
	public static MenuType<GTBasicMachineMenu> cokeoven() {
		return COKE_OVEN_MENU.get();
	}

	/** The {@code gt6:dryer} menu type (task p16-machine-fluid-gui ①, the single-block network path). */
	public static MenuType<GTBasicMachineMenu> dryer() {
		return DRYER_MENU.get();
	}

	/** The {@code gt6:canner} menu type (task p24-canner-machine, the single-block network path — the 2-input GTBasicMachineMenu shape). */
	public static MenuType<GTBasicMachineMenu> canner() {
		return CANNER_MENU.get();
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
		LOGGER.info("GT6 machine menu 'cokeoven' registered (gt6:cokeoven — the TileEntityBase10MultiBlockMachine GUI path, task p8-cokeoven-gui-menu)");
		LOGGER.info("GT6 machine menu 'dryer' registered (gt6:dryer — the four-tier Dryer family GUI path, task p16-machine-fluid-gui)");
		LOGGER.info("GT6 machine menu 'canner' registered (gt6:canner — the four-tier Canner family GUI path, the 2-input menu shape, task p24-canner-machine)");
		// task p34-bumbliary-gui — the bumbliary GUI pair rides the MUI factory identity
		// (the OpenGuiPacket wire carries the factory name, so the scoop/normal variant
		// needs no MenuType and no PosGuiData change; the P26 no-new-MenuType freeze holds)
		brachy.modularui.factory.GuiManager.registerFactory(GT6BumbliaryMUI.Factory.NORMAL);
		brachy.modularui.factory.GuiManager.registerFactory(GT6BumbliaryMUI.Factory.SCOOP);
		LOGGER.info("GT6 bumbliary UI factories registered (gt6:bumbliary + gt6:bumbliary_scoop — the ModularUI factory-identity wire, task p34-bumbliary-gui)");
	}
}
