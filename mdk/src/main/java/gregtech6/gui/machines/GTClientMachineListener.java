package gregtech6.gui.machines;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-side mod-bus wiring for the machine screens (the GTClientOvenListener shape):
 * self-contained {@code @Mod.EventBusSubscriber(Dist.CLIENT)} — all three MenuTypes mount
 * the shared {@link GTBasicMachineScreen} inside {@code FMLClientSetupEvent#enqueueWork}
 * (MenuScreens.register is not thread-safe, forge-docs 1.20.x gui/screens.md:314); the
 * explicitly-typed lambda drives the M/U inference of
 * {@code MenuScreens.register(MenuType<? extends M>, ScreenConstructor<M, U>)}.
 */
@Mod.EventBusSubscriber(modid = GTBasicMachinesMenus.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTClientMachineListener {

	private GTClientMachineListener() {
	}

	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			MenuScreens.register(GTBasicMachinesMenus.shredder(),
				(GTBasicMachineMenu menu, Inventory playerInventory, Component title) -> new GTBasicMachineScreen(menu, playerInventory, title));
			MenuScreens.register(GTBasicMachinesMenus.crusher(),
				(GTBasicMachineMenu menu, Inventory playerInventory, Component title) -> new GTBasicMachineScreen(menu, playerInventory, title));
			MenuScreens.register(GTBasicMachinesMenus.lathe(),
				(GTBasicMachineMenu menu, Inventory playerInventory, Component title) -> new GTBasicMachineScreen(menu, playerInventory, title));
		});
	}
}
