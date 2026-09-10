package gregtech6.gui.machines;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import gregtech6.gui.GTGuiScreen;

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
 * <p>Progress (p27-gui-render-fixes ①): the three-state ContainerData value drives the upstream
 * RecipeMap progress-arrow overlay — the white arrow baked at UV (176,0) 20x18 on the same
 * 256x256 canvas as the panel (every machine GUI texture ships it next to the 176x166 panel,
 * cokeoven.png/shredder.png/... pixel-verified), width-clipped by the scaled progress. Upstream
 * draw: ContainerClientBasicMachine.java:54-65, case 0 = drawTexturedModalRect(x+78, y+24,
 * 176, 0, tProgress, 18) over the arrow outline the panel bakes at (78,24); the 0..20 step
 * ladder is UT.Code.scale (UT.java:1534-1542) at tSize=20, aMin=0, aMax=Short.MAX_VALUE —
 * 0 at &le;0, 20 at &ge;32767, else 1 + value*19/32767. The port pins the case-0 arm because
 * both maps this screen serves carry direction 0 / amount 1 (COKE_OVEN GT6RecipeMaps.java:348,
 * DRYING :427; GT6RecipeMapsTest:58/:166) — the SIFTING direction-2 map goes through the MUI
 * panel (GTClientMachineListener registers only cokeoven + dryer here).
 */
public class GTBasicMachineScreen extends GTGuiScreen<GTBasicMachineMenu> {

	/**
	 * The arrow overlay draw cell (upstream ContainerClientBasicMachine.java:57 case 0): the
	 * panel's arrow outline sits at (78,24), the white overlay is the 20x18 region at UV
	 * (176,0) of the same texture, blitted left-to-right with the width clipped to the
	 * progress step ({@link GuiGraphics#blit} 7-arg = drawTexturedModalRect semantics,
	 * GuiGraphics.java:315-317 implicit 256x256 canvas).
	 */
	private static final int ARROW_X = 78, ARROW_Y = 24, ARROW_U = 176, ARROW_V = 0;
	private static final int ARROW_WIDTH = 20, ARROW_HEIGHT = 18;

	public GTBasicMachineScreen(GTBasicMachineMenu menu, Inventory playerInventory, Component title) {
		super(menu, playerInventory, title, backgroundOf(menu.tileEntity));
		this.imageWidth = 176;
		this.imageHeight = 166;
	}

	/**
	 * Upstream mGUITexture = mRecipes.mGUIPath (:114) as a ResourceLocation — the map
	 * constants are full "namespace:path.png" strings (RecipeMap.java:84 appends .png).
	 * Host-typed since p8-cokeoven-gui-menu ①: single-block and multiblock machines
	 * alike carry the path through {@link GTBasicMachineMenu.Host#getGuiTexture()}.
	 */
	public static ResourceLocation backgroundOf(GTBasicMachineMenu.Host aMachine) {
		String tPath = aMachine.getGuiTexture();
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
			// UT.Code.scale (UT.java:1534-1542) at tSize=20: 0 at <=0, 20 at >=PROGRESS_DONE,
			// else 1 + progress*19/32767 — the upstream step ladder (the % (tSize+1) there is a
			// no-op for in-range values). The clip width covers the arrow left-to-right.
			int tFill = tProgress <= 0 ? 0
					: tProgress >= GTBasicMachineMenu.PROGRESS_DONE ? ARROW_WIDTH
					: 1 + (int)((long)tProgress * (ARROW_WIDTH - 1) / GTBasicMachineMenu.PROGRESS_DONE);
			if (tFill > 0) {
				guiGraphics.blit(this.backgroundTexture, x + ARROW_X, y + ARROW_Y, ARROW_U, ARROW_V, tFill, ARROW_HEIGHT);
			}
		}
	}
}
