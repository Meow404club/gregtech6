package gregtech6.client;

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

import gregtech6.gui.GTDebugMenu;
import gregtech6.gui.GTDebugScreen;
import gregtech6.gui.GTMenuTypes;

/**
 * Client-side mod-bus wiring for the GUI framework (ADR-P3-3 / ADR-P3-4): fully self-contained
 * {@code @Mod.EventBusSubscriber} — GT6Mod.java and the P2 instance-listener
 * (gregtech6.registry.GTModBusListener) are never touched, and this class is skipped entirely on
 * the dedicated server (Dist.CLIENT-gated injection).
 *
 * <p>Screen mounting is a chisel fork (ADR-P15-3 r1 — structural face, the registration
 * mechanism itself differs): on 1.20.1 Forge there is no RegisterMenuScreensEvent, so the
 * screen is mounted inside {@code FMLClientSetupEvent#enqueueWork} — {@code MenuScreens#register}
 * is the only hook and is not thread-safe, hence the enqueueWork requirement (forge-docs 1.20.x
 * gui/screens.md:314; same-version precedent GTCEu ModClientEventListener.java:19-23). On 1.21.1
 * NeoForge {@code MenuScreens#register} is private (javap compiledWithNeoForge 21.1 jar) and the
 * sanctioned hook is the mod-bus {@code RegisterMenuScreensEvent} (its {@code register} method
 * carries the same {@code <M, U> register(MenuType<? extends M>, ScreenConstructor<M, U>)} shape,
 * so the lambda is byte-identical across the legs).
 */
@Mod.EventBusSubscriber(modid = GTMenuTypes.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTClientModBusListener {

    private GTClientModBusListener() {
    }

    //? if forge {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Explicitly-typed lambda: MenuScreens.register(MenuType<? extends M>, ScreenConstructor<M, U>)
        // needs the (menu, inventory, title) factory shape (MenuScreens.java:106-113); a bare method
        // reference does not drive the M/U inference, so the parameters are spelled out
        // (GTCEu ModClientEventListener.java:19-23 does the equivalent with a cast).
        event.enqueueWork(() -> MenuScreens.register(GTMenuTypes.gtDebug(),
            (GTDebugMenu menu, Inventory playerInventory, Component title) -> new GTDebugScreen(menu, playerInventory, title)));
    }
    //? } else {
    /*@SubscribeEvent
    public static void onRegisterScreens(RegisterMenuScreensEvent event) {
        // the identical explicitly-typed lambda — the event's register() is signature-shaped
        // like MenuScreens.register was (RegisterMenuScreensEvent javap, 21.1.249)
        event.register(GTMenuTypes.gtDebug(),
            (GTDebugMenu menu, Inventory playerInventory, Component title) -> new GTDebugScreen(menu, playerInventory, title));
    }*/
    //? }
}
