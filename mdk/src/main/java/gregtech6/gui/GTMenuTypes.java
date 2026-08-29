package gregtech6.gui;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;

/**
 * GT6 {@link MenuType} registry — every GT6 GUI shape gets one MenuType (the 1.20.1 replacement of
 * the upstream integer GUIID scheme; TileEntityBase01Root.java:180 / ITileEntityGUI.java:29-40 map
 * here). Menus are built through Forge's {@link IForgeMenuType#create(IContainerFactory)} so each
 * factory receives the standard {@code (windowId, playerInventory, FriendlyByteBuf)} triple
 * (IForgeMenuType.java:17, IContainerFactory.java:15; the two-arg MenuSupplier bridge passes
 * {@code data = null} for the vanilla openMenu path).
 *
 * <p>ADR-P3-4: registration is fully self-contained — this class is its own
 * {@code @Mod.EventBusSubscriber(Bus.MOD)} listener and GT6Mod.java / GTModBusListener.java are
 * never touched. GTCEu's GTMenuTypes.java:11-22 shape (DeferredRegister + {@code init(modBus)}
 * called from the {@code @Mod} constructor) cannot be copied verbatim under that rule, because
 * DeferredRegister needs the mod bus via {@code register(IEventBus)} (DeferredRegister.java:312)
 * and no self-contained listener can obtain it; the public RegisterEvent.register
 * (RegisterEvent.java:72, RegisterHelper at :116) is the equivalent self-contained idiom and is
 * what DeferredRegister wraps internally (DeferredRegister.java:379 addEntries). Typed access goes
 * through the {@code gtDebug()} accessor, which fails fast if used before registration completed
 * on the current side (registry events fire on both client and dedicated server).
 */
@Mod.EventBusSubscriber(modid = GTMenuTypes.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GTMenuTypes {

    public static final String MOD_ID = "gt6";

    /** Registry path of the debug GUI (MenuType id {@code gt6:debug}). */
    public static final String DEBUG_MENU_ID = "debug";

    /** Set during RegisterEvent; read via {@link #gtDebug()}. */
    private static MenuType<GTDebugMenu> gtDebugMenu;

    private GTMenuTypes() {
    }

    /** The {@code gt6:debug} menu type (fails fast when used before the registry phase ran). */
    public static MenuType<GTDebugMenu> gtDebug() {
        MenuType<GTDebugMenu> menuType = gtDebugMenu;
        if (menuType == null) {
            throw new IllegalStateException(MOD_ID + ":" + DEBUG_MENU_ID + " used before registration");
        }
        return menuType;
    }

    /** Menu registry fill; the MENU registry key filters the fan-out (one event per registry type). */
    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        event.register(Registries.MENU, helper -> {
            MenuType<GTDebugMenu> menuType = IForgeMenuType.create(GTDebugMenu::new);
            helper.register(ResourceLocation.fromNamespaceAndPath(MOD_ID, DEBUG_MENU_ID), menuType);
            gtDebugMenu = menuType;
        });
    }
}
