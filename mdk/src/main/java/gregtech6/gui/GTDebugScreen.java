package gregtech6.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for the {@code /gt6gui} debug menu — the minimal GTGuiScreen instantiation: standard
 * 176x166 panel (AbstractContainerScreen.java:32-33 defaults), three demo slots plus the bound
 * player inventory (GTDebugMenu), title centred and background blitted by the base class.
 * Texture {@code gt6:textures/gui/debug.png} is a generated placeholder (mdk/tools/gen_gui_textures.py),
 * not a datagen JSON — no red-line conflict.
 */
public class GTDebugScreen extends GTGuiScreen<GTDebugMenu> {

    public static final ResourceLocation BACKGROUND =
        ResourceLocation.fromNamespaceAndPath(GTMenuTypes.MOD_ID, "textures/gui/debug.png");

    public GTDebugScreen(GTDebugMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, BACKGROUND);
    }
}
