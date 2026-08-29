package gregtech6.client;

import net.minecraft.client.color.item.ItemColor;
import net.minecraft.locale.Language;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.IEventBus;

import gregtech6.item.MaterialPrefixItem;
import gregtech6.registry.GTMaterialItems;

/**
 * Client-only event wiring. Kept in a {@code @OnlyIn(Dist.CLIENT)} class so the dedicated server
 * never loads it; the only call site is dist-guarded in the {@code @Mod} constructor. The mod bus
 * fires RegisterColorHandlersEvent only on the logical client (RegisterColorHandlersEvent.java:33-45).
 */
@OnlyIn(Dist.CLIENT)
public final class GTClientHandlers {

    private GTClientHandlers() {
    }

    /** Registers the client-only mod-bus listeners (called under a Dist.CLIENT guard). */
    public static void init(IEventBus modBus) {
        modBus.addListener(GTClientHandlers::onRegisterItemColors);
    }

    /** Material tint for every registered material prefix item (GTCEu TagPrefixItem.java:55-57 isomorph). */
    private static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        ItemColor tColor = MaterialPrefixItem.tintColor();
        event.getItemColors().register(tColor, GTMaterialItems.itemArray()); // ItemColors.register(ItemColor, ItemLike...)
    }

    /** Translation key existence check (Language.getInstance Language.java:83, has :97). */
    public static boolean hasTranslation(String key) {
        return Language.getInstance().has(key);
    }
}
