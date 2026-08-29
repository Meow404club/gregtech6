package gregtech6.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for the example chest — the client half replacing upstream getGUIClient/
 * ContainerClientChest (MultiTileEntityChest.java:319), statically mounted via
 * MenuScreens.register (GTClientExampleChestListener). Panel geometry follows the vanilla
 * generic container formula {@code 114 + rows * 18} (ContainerScreen.java:18; 54 slots =
 * 6 rows = 222px), the same panel the menu's bindPlayerInventory offset
 * ({@code 103+(rows-4)*18}, ContainerCommonChest.java:42) targets. Texture
 * {@code gt6:textures/gui/example_chest.png} is a generated placeholder
 * (mdk/tools/gen_gui_textures.py), not datagen JSON — no red-line conflict.
 */
public class GTExampleChestScreen extends GTGuiScreen<GTExampleChestMenu> {

	public static final ResourceLocation BACKGROUND =
		ResourceLocation.fromNamespaceAndPath(GTMenuTypes.MOD_ID, "textures/gui/example_chest.png");

	public GTExampleChestScreen(GTExampleChestMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title, BACKGROUND);
		this.imageWidth = 176;
		this.imageHeight = 114 + menu.containerRows * 18; // ContainerScreen.java:18
	}
}
