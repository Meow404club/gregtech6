package gregtech6.gui.machines;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

//? if neoforge {
/*import net.minecraftforge.client.event.RegisterMenuScreensEvent;*/
//? }

/**
 * Client-side mod-bus wiring for the machine screens (the GTClientOvenListener shape):
 * self-contained {@code @Mod.EventBusSubscriber(Dist.CLIENT)} — the legacy-row MenuTypes
 * mount the shared {@link GTBasicMachineScreen} inside {@code FMLClientSetupEvent#enqueueWork}
 * (MenuScreens.register is not thread-safe, forge-docs 1.20.x gui/screens.md:314); the
 * explicitly-typed lambda drives the M/U inference of
 * {@code MenuScreens.register(MenuType<? extends M>, ScreenConstructor<M, U>)}.
 *
 * <p>The shredder/crusher/lathe trio has no entry since task p26-mui-a-menu-deregistration:
 * their GUI is ModularUI (no vanilla MenuType, task p26-mui-a-open-chain open chain).
 *
 * <p>1.21.1: {@code MenuScreens#register} is private (javap universal 21.1.249) — the
 * sanctioned hook is the mod-bus {@code RegisterMenuScreensEvent} whose {@code register}
 * carries the same {@code <M, U> register(MenuType<? extends M>, ScreenConstructor<M, U>)}
 * shape, lambda byte-identical across the legs (GTClientModBusListener precedent).
 */
@Mod.EventBusSubscriber(modid = GTBasicMachinesMenus.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTClientMachineListener {

	private GTClientMachineListener() {
	}

	//? if forge {
	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			MenuScreens.register(GTBasicMachinesMenus.cokeoven(),
				(GTBasicMachineMenu menu, Inventory playerInventory, Component title) -> new GTBasicMachineScreen(menu, playerInventory, title));
			MenuScreens.register(GTBasicMachinesMenus.dryer(),
				(GTBasicMachineMenu menu, Inventory playerInventory, Component title) -> new GTBasicMachineScreen(menu, playerInventory, title));
		});
	}
	//?} else {
	/*@SubscribeEvent
	public static void onRegisterScreens(RegisterMenuScreensEvent event) {
		event.register(GTBasicMachinesMenus.cokeoven(),
			(GTBasicMachineMenu menu, Inventory playerInventory, Component title) -> new GTBasicMachineScreen(menu, playerInventory, title));
		event.register(GTBasicMachinesMenus.dryer(),
			(GTBasicMachineMenu menu, Inventory playerInventory, Component title) -> new GTBasicMachineScreen(menu, playerInventory, title));
	}*/
	//?}
}
