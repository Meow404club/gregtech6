package gregtech6.gui;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-side mod-bus wiring for the example chest screen (ADR-P3-4): self-contained
 * {@code @Mod.EventBusSubscriber} living in this package because gregtech6.client is outside
 * the card's FILES_SCOPE and GTClientModBusListener (the gt6:debug precedent) must stay
 * untouched — the example machine gets its own listener instead of an append there.
 *
 * <p>Screen mounting happens exactly once, inside {@code FMLClientSetupEvent#enqueueWork};
 * {@code MenuScreens#register} is public through the Forge accesstransformer.cfg and is not
 * thread-safe (forge-docs 1.20.x gui/screens.md:314). Explicitly-typed lambda: a bare method
 * reference does not drive the M/U inference of
 * {@code MenuScreens.register(MenuType<? extends M>, ScreenConstructor<M, U>)}
 * (GTClientModBusListener doc, GTCEu ModClientEventListener.java:19-23 shape).
 */
@Mod.EventBusSubscriber(modid = GTMenuTypes.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTClientExampleChestListener {

	private GTClientExampleChestListener() {
	}

	@SubscribeEvent
	public static void onClientSetup(FMLClientSetupEvent event) {
		event.enqueueWork(() -> MenuScreens.register(GTMenuTypes.exampleChest(),
			(GTExampleChestMenu menu, Inventory playerInventory, Component title) -> new GTExampleChestScreen(menu, playerInventory, title)));
	}
}
