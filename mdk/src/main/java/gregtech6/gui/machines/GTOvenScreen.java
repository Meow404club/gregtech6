package gregtech6.gui.machines;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import gregtech6.gui.GTGuiScreen;

/**
 * Oven screen — the client half replacing upstream getGUIClient2/ContainerClientBasicMachine
 * (MultiTileEntityBasicMachine.java:1007), statically mounted via MenuScreens.register
 * (GTClientOvenListener). Panel = the standard 176x166 machine GUI the upstream mGUITexture
 * machines/oven.png family uses (Example_Mod.java:165), with the slot cells at the
 * ContainerCommonBasicMachine coordinates the menu binds.
 *
 * <p>Progress bar (spec 6): the three-state ContainerData value drives a flat fill between
 * the input and output slots — the A-tier stand-in for the upstream RecipeMap progress
 * arrow (mProgressBarDirection/Amount are the NEI/GUI metadata of the 1.7.10 texture
 * pipeline and stay out until the real GUI atlas work).
 */
public class GTOvenScreen extends GTGuiScreen<GTOvenMenu> {

	public static final ResourceLocation BACKGROUND =
		ResourceLocation.fromNamespaceAndPath(GTOvenMenus.MOD_ID, "textures/gui/machines/oven.png");

	/** Bar geometry in panel coordinates: the 36px gap between the input slot (right edge 71) and the output slot (107). */
	private static final int BAR_X = 73, BAR_Y = 30, BAR_WIDTH = 32, BAR_HEIGHT = 4;

	private static final int COLOR_TRACK = 0xFF373737;
	private static final int COLOR_FILL = 0xFFFF8800;

	public GTOvenScreen(GTOvenMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title, BACKGROUND);
		this.imageWidth = 176;
		this.imageHeight = 166;
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
		super.renderBg(guiGraphics, partialTick, mouseX, mouseY); // GTGuiScreen blits the texture from UV 0,0
		int x = (this.width - this.imageWidth) / 2;
		int y = (this.height - this.imageHeight) / 2;
		int tProgress = this.menu.getProgressBar();
		if (tProgress >= 0) {
			guiGraphics.fill(x + BAR_X, y + BAR_Y, x + BAR_X + BAR_WIDTH, y + BAR_Y + BAR_HEIGHT, COLOR_TRACK);
			int tFill = tProgress >= GTOvenMenu.PROGRESS_DONE
					? BAR_WIDTH
					: (int)((long)tProgress * BAR_WIDTH / GTOvenMenu.PROGRESS_DONE);
			if (tFill > 0) {
				guiGraphics.fill(x + BAR_X, y + BAR_Y, x + BAR_X + tFill, y + BAR_Y + BAR_HEIGHT, COLOR_FILL);
			}
		}
	}
}
