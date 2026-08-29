package gregtech6.gui;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;

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

    /** Registry path of the example chest GUI (MenuType id {@code gt6:example_chest}). */
    public static final String EXAMPLE_CHEST_MENU_ID = "example_chest";

    /** Set during RegisterEvent; read via {@link #gtDebug()}. */
    private static MenuType<GTDebugMenu> gtDebugMenu;

    /**
     * The example chest MenuType (task p3-example-machine) — the DeferredRegister +
     * {@code Bus.MOD.bus().get()} form the BE framework proved self-contained
     * (Mod.java:81; GTBlockEntities precedent). Appended next to the debug RegisterEvent
     * idiom, which stays as-is: both idioms are review-verified paths to the same
     * RegisterEvent stream, and new registrations standardise on this one.
     */
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MOD_ID);

    public static final RegistryObject<MenuType<GTExampleChestMenu>> EXAMPLE_CHEST_MENU =
        MENUS.register(EXAMPLE_CHEST_MENU_ID, () -> IForgeMenuType.create(GTExampleChestMenu::new));

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

    /** The {@code gt6:example_chest} menu type (RegistryObject.get fails fast when unbound). */
    public static MenuType<GTExampleChestMenu> exampleChest() {
        return EXAMPLE_CHEST_MENU.get();
    }

    /** FMLConstructModEvent = first mod-bus lifecycle stage, strictly before any RegisterEvent (GTBlockEntities.onModConstruct doc). */
    @SubscribeEvent
    public static void onModConstruct(FMLConstructModEvent aEvent) {
        MENUS.register(Mod.EventBusSubscriber.Bus.MOD.bus().get());
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
