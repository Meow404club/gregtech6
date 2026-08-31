package gregtech6.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.color.item.ItemColor;
import net.minecraft.locale.Language;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;

import gregtech6.block.material.GTMaterialPrefixBlock;
import gregtech6.client.wire.GTWireTint;
import gregtech6.item.GTMaterialPrefixBlockItem;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.registry.GTMaterialBlocks;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTWires;

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
        modBus.addListener(GTClientHandlers::onRegisterBlockColors); // task p8-prefixblock-render ③: world-side tint
        modBus.addListener(GTClientHandlers::onRegisterWireColors); // task p9-wire-family-w2: the wire family tints
    }

    /** Material tint for every registered material prefix item (GTCEu TagPrefixItem.java:55-57 isomorph). */
    private static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        ItemColor tColor = MaterialPrefixItem.tintColor();
        event.getItemColors().register(tColor, GTMaterialItems.itemArray()); // ItemColors.register(ItemColor, ItemLike...)
        // task p8-prefixblock-render: the 3773 block items tint through the same colour seam —
        // per the Forge docs a BlockColor does NOT colour its BlockItem (PrefixBlockItem.java:103
        // tints the item side upstream), so the GTMaterialPrefixBlockItem ItemColor is registered
        // explicitly over the block-item array.
        event.getItemColors().register(GTMaterialPrefixBlockItem.tintColor(),
                GTMaterialBlocks.items().values().stream().map(RegistryObject::get).toArray(Item[]::new));
    }

    /** Task p8-prefixblock-render ③: the material tint for every registered material prefix block (GTMaterialPrefixBlock.blockColor). */
    private static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        event.getBlockColors().register(GTMaterialPrefixBlock.blockColor(), GTMaterialBlocks.blockArray());
    }

    /**
     * Task p9-wire-family-w2: the wire family tints — the world half over every wire block
     * (the p7 legacy pair + the 620 family, {@code GTWires.wireBlockArray()}), the inventory
     * half over the same blocks' items (a BlockColor does not colour its BlockItem, the
     * prefix-card comment above). {@link GTWireTint} = fRGBaSolid on tint index 0, the
     * fixed insulation gray on index 1.
     */
    private static void onRegisterWireColors(RegisterColorHandlersEvent event) {
        if (event instanceof RegisterColorHandlersEvent.Block tBlockEvent) {
            tBlockEvent.getBlockColors().register(GTWireTint.blockColor(), GTWires.wireBlockArray());
        } else if (event instanceof RegisterColorHandlersEvent.Item tItemEvent) {
            List<Item> tWireItems = new ArrayList<>();
            tWireItems.add(GTWires.WIRE_ELECTRIC_1X_ITEM.get());
            tWireItems.add(GTWires.WIRE_ELECTRIC_2X_ITEM.get());
            for (RegistryObject<Item> tFamilyItem : GTWires.FAMILY_ITEMS) tWireItems.add(tFamilyItem.get());
            tItemEvent.getItemColors().register(GTWireTint.itemColor(), tWireItems.toArray(Item[]::new));
        }
    }

    /** Translation key existence check (Language.getInstance Language.java:83, has :97). */
    public static boolean hasTranslation(String key) {
        return Language.getInstance().has(key);
    }
}
