package gregtech6.client;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import gregtech6.gui.GTDebugMenu;
import gregtech6.gui.GTDebugScreen;
import gregtech6.gui.GTMenuTypes;

/**
 * Client-side mod-bus wiring for the GUI framework (ADR-P3-3 / ADR-P3-4): fully self-contained
 * {@code @Mod.EventBusSubscriber} — GT6Mod.java and the P2 instance-listener
 * (gregtech6.registry.GTModBusListener) are never touched, and this class is skipped entirely on
 * the dedicated server (Dist.CLIENT-gated injection).
 *
 * <p>Screen mounting happens exactly once, inside {@code FMLClientSetupEvent#enqueueWork} — in
 * 1.20.1 Forge there is no RegisterMenuScreensEvent; {@code MenuScreens#register} is the only
 * hook and is not thread-safe, hence the enqueueWork requirement (forge-docs 1.20.x
 * gui/screens.md:314; same-version precedent GTCEu ModClientEventListener.java:19-23).
 */
@Mod.EventBusSubscriber(modid = GTMenuTypes.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTClientModBusListener {

    private GTClientModBusListener() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Explicitly-typed lambda: MenuScreens.register(MenuType<? extends M>, ScreenConstructor<M, U>)
        // needs the (menu, inventory, title) factory shape (MenuScreens.java:106-113); a bare method
        // reference does not drive the M/U inference, so the parameters are spelled out
        // (GTCEu ModClientEventListener.java:19-23 does the equivalent with a cast).
        event.enqueueWork(() -> MenuScreens.register(GTMenuTypes.gtDebug(),
            (GTDebugMenu menu, Inventory playerInventory, Component title) -> new GTDebugScreen(menu, playerInventory, title)));
    }
}
