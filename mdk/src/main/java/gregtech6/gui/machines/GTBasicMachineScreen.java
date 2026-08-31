package gregtech6.gui.machines;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import gregtech6.gui.GTGuiScreen;
import gregtech6.tileentity.machines.TileEntityBasicMachine;

/**
 * Basic-machine screen — the client half replacing upstream getGUIClient2/
 * ContainerClientBasicMachine (MultiTileEntityBasicMachine.java:1007), statically mounted
 * via MenuScreens.register (GTClientMachineListener). Panel = the standard 176x166 machine
 * GUI; the background texture is the mRecipes.mGUIPath semantics of upstream
 * mGUITexture = mRecipes.mGUIPath (:114) — the W1 RecipeMap GUI strings
 * (gt6:textures/gui/machines/shredder|crusher|lathe + the RecipeMap-appended .png,
 * GT6RecipeMaps.java:97/:105/:113) parsed into a ResourceLocation per machine, so one screen
 * class serves the three backgrounds the gui-family card landed.
 *
 * <p>Progress bar (spec 6): the three-state ContainerData value drives a flat fill between
 * the input slot and the output column — the oven bar geometry (GTOvenScreen), the A-tier
 * stand-in for the upstream RecipeMap progress arrow.
 */
public class GTBasicMachineScreen extends GTGuiScreen<GTBasicMachineMenu> {

	/** Bar geometry in panel coordinates: the 36px gap between the input slot (right edge 71) and the output column (107). */
	private static final int BAR_X = 73, BAR_Y = 30, BAR_WIDTH = 32, BAR_HEIGHT = 4;

	private static final int COLOR_TRACK = 0xFF373737;
	private static final int COLOR_FILL = 0xFFFF8800;

	public GTBasicMachineScreen(GTBasicMachineMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title, backgroundOf(menu.tileEntity));
		this.imageWidth = 176;
		this.imageHeight = 166;
	}

	/**
	 * Upstream mGUITexture = mRecipes.mGUIPath (:114) as a ResourceLocation — the W1 map
	 * constants are full "namespace:path.png" strings (RecipeMap.java:84 appends .png).
	 */
	public static ResourceLocation backgroundOf(TileEntityBasicMachine aMachine) {
		String tPath = aMachine.mRecipes.mGUIPath;
		int tColon = tPath.indexOf(':');
		if (tColon < 0) throw new IllegalArgumentException("RecipeMap mGUIPath is not a namespaced path: " + tPath);
		return ResourceLocation.fromNamespaceAndPath(tPath.substring(0, tColon), tPath.substring(tColon + 1));
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
		super.renderBg(guiGraphics, partialTick, mouseX, mouseY); // GTGuiScreen blits the texture from UV 0,0
		int x = (this.width - this.imageWidth) / 2;
		int y = (this.height - this.imageHeight) / 2;
		int tProgress = this.menu.getProgressBar();
		if (tProgress >= 0) {
			guiGraphics.fill(x + BAR_X, y + BAR_Y, x + BAR_X + BAR_WIDTH, y + BAR_Y + BAR_HEIGHT, COLOR_TRACK);
			int tFill = tProgress >= GTBasicMachineMenu.PROGRESS_DONE
					? BAR_WIDTH
					: (int)((long)tProgress * BAR_WIDTH / GTBasicMachineMenu.PROGRESS_DONE);
			if (tFill > 0) {
				guiGraphics.fill(x + BAR_X, y + BAR_Y, x + BAR_X + tFill, y + BAR_Y + BAR_HEIGHT, COLOR_FILL);
			}
		}
	}
}
