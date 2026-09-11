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
 *
 * <p>Inventory label (p27-gui-render-fixes ②): AbstractContainerScreen computes
 * {@code inventoryLabelY = imageHeight - 94} in its CONSTRUCTOR (AbstractContainerScreen.java:78),
 * where imageHeight is still the 166 field default (:33) — the subclass height assignment below
 * runs too late and the label lands at y=72, on the chest grid's 4th row. Vanilla re-pins the
 * label after the panel height in exactly the same shape (ContainerScreen.java:18-19); the
 * 6-row panel yields 222-94 = 128, the band between the grid bottom (125) and the player
 * inventory top (139). Slot-cell backgrounds need no screen-side work: the generated texture's
 * cells are pixel-identical across the grid/player/hotbar regions and match the vanilla inset
 * pattern the machine GUI textures bake (0x8B fill + 0x37 top/left + white bottom/right).
 */
public class GTExampleChestScreen extends GTGuiScreen<GTExampleChestMenu> {

	public static final ResourceLocation BACKGROUND =
		ResourceLocation.fromNamespaceAndPath(GTMenuTypes.MOD_ID, "textures/gui/example_chest.png");

	public GTExampleChestScreen(GTExampleChestMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title, BACKGROUND);
		this.imageWidth = 176;
		this.imageHeight = 114 + menu.containerRows * 18; // ContainerScreen.java:18
		this.inventoryLabelY = this.imageHeight - 94; // ContainerScreen.java:19 — after the panel height (AbstractContainerScreen.java:78 ran with the 166 default)
	}
}
