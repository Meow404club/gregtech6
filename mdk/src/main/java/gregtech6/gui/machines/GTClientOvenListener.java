package gregtech6.gui.machines;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-side mod-bus wiring for the oven screen (ADR-P3-4): self-contained
 * {@code @Mod.EventBusSubscriber(Dist.CLIENT)} — the gt6:debug (GTClientModBusListener) and
 * example-chest (GTClientExampleChestListener) precedents each carry their own listener and
 * this package is the machine family's home. Screen mounting happens exactly once inside
 * {@code FMLClientSetupEvent#enqueueWork} (MenuScreens.register is not thread-safe,
 * forge-docs 1.20.x gui/screens.md:314); the explicitly-typed lambda drives the M/U
 * inference of {@code MenuScreens.register(MenuType<? extends M>, ScreenConstructor<M, U>)}.
 */
@Mod.EventBusSubscriber(modid = GTOvenMenus.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTClientOvenListener {

	private GTClientOvenListener() {
	}

	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> MenuScreens.register(GTOvenMenus.oven(),
			(GTOvenMenu menu, Inventory playerInventory, Component title) -> new GTOvenScreen(menu, playerInventory, title)));
	}
}
