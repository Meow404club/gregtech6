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
 * GUI composed from the composable GUI part crops (task machine-gui-composed-purge, the
 * user ruling "全部改成拼接，把完整图全部清了" — the per-machine mGUIPath sheets are
 * retired): the {@link GT6GuiParts#PANEL_BASE} theme panel (machine-area blank, the printed
 * player band) under the slot frames ({@link GT6GuiParts#SLOT_FRAME} at each bound slot's
 * (x-1,y-1)), the progress-arrow cell print ({@link GT6GuiParts#ARROW_OUTLINE} at (78,24))
 * and the decorative gear print ({@link GT6GuiParts#SLOT_SPECIAL} at (77,60)) — the exact
 * furniture every machine sheet baked, pixel-aligned to the same coordinates, one screen
 * class serving the cokeoven/dryer family (GTClientMachineListener) off the shared parts.
 *
 * <p>Progress (gui-render-fixes ①): the three-state ContainerData value drives the upstream
 * RecipeMap progress-arrow overlay — the composed arrow pair rides the same parts the MUI
 * panel uses (GTBasicMachineMUI's {@code emptyTexture}/{@code filledTexture} pair): the
 * {@link GT6GuiParts#ARROW_OUTLINE} cell (always drawn, the skins' print at (78,24)) under
 * the {@link GT6GuiParts#ARROW_FORWARD} fill blitted left-to-right, width-clipped by the
 * scaled progress. Upstream draw: ContainerClientBasicMachine.java:54-65, case 0 =
 * drawTexturedModalRect(x+78, y+24, 176, 0, tProgress, 18) over the arrow outline the panel
 * baked at (78,24); the 0..20 step ladder is UT.Code.scale (UT.java:1534-1542) at tSize=20,
 * aMin=0, aMax=Short.MAX_VALUE — 0 at &le;0, 20 at &ge;32767, else 1 + value*19/32767.
 * The port pins the case-0 arm because the maps this screen serves carry direction 0 /
 * amount 1 (COKE_OVEN, DRYING; GT6RecipeMapsTest) — the SIFTING direction-2 map goes
 * through the MUI panel (GTClientMachineListener registers only cokeoven + dryer here).
 *
 * <p>Progress-bar jump (debt-jei-emi-batch4): clicking the arrow rect opens the served
 * RecipeMap's category page in the installed recipe viewer (JEI first, EMI fallback) — the
 * modern counterpart of upstream NEI_RecipeMap.GT_RectHandler (:399-426), whose rect
 * (65,13,36,18) covered the upstream panel's progress arrow and whose hover label
 * "Recipes" (:420) rides again verbatim. Upstream registered the same NEI handler as
 * crafting AND usage (:75-76), so its left/right arms opened the identical page; this port
 * keeps the single destination. The rect tracks the composed arrow cell (the {@code ARROW_*}
 * cell below), not the upstream coordinates — the arrow moved with the modern 176x166 panel.
 */
public class GTBasicMachineScreen extends GTGuiScreen<GTBasicMachineMenu> {

	/**
	 * The arrow overlay draw cell (upstream ContainerClientBasicMachine.java:57 case 0): the
	 * composed arrow cell sits at (78,24), the {@link GT6GuiParts#ARROW_FORWARD} fill is
	 * blitted left-to-right with the width clipped to the progress step ({@link
	 * GuiGraphics#blit} 7-arg = drawTexturedModalRect semantics, GuiGraphics.java:315-317
	 * implicit 256x256 canvas — the part is its own 20x18 texture, art from UV 0,0).
	 */
	private static final int ARROW_X = 78, ARROW_Y = 24;
	private static final int ARROW_WIDTH = 20, ARROW_HEIGHT = 18;

	/** The arrow fill overlay — the forward-arrow strip, the same geometry as the sheets' UV(176,0) overlay. */
	private static final ResourceLocation ARROW_FILL = GT6GuiParts.ARROW_FORWARD.texture();

	/** The empty arrow cell print — the machine skins' (78,24) furniture, always drawn under the fill. */
	private static final ResourceLocation ARROW_OUTLINE = GT6GuiParts.ARROW_OUTLINE.texture();

	/** The decorative gear print — the 22x22 special-slot cell every machine sheet carried at (77,60). */
	private static final ResourceLocation GEAR_PRINT = GT6GuiParts.SLOT_SPECIAL.texture();

	/** The slot frame cell — the standard 18x18 white-stroke frame, painted at (slot.x-1, slot.y-1). */
	private static final ResourceLocation SLOT_FRAME = GT6GuiParts.SLOT_FRAME.texture();

	public GTBasicMachineScreen(GTBasicMachineMenu menu, Inventory playerInventory, Component title) {
		// the panel base rides the vanilla-leg blit face (GTGuiScreen blits it from UV 0,0
		// in renderBg) — the per-machine sheet parse (the old backgroundOf) is retired with
		// the mGUIPath values (machine-gui-composed-purge)
		super(menu, playerInventory, title, GT6GuiParts.PANEL_BASE.texture());
		this.imageWidth = 176;
		this.imageHeight = 166;
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
		super.renderBg(guiGraphics, partialTick, mouseX, mouseY); // GTGuiScreen blits the panel base from UV 0,0
		int x = (this.width - this.imageWidth) / 2;
		int y = (this.height - this.imageHeight) / 2;
		// the slot frames — one 18x18 cell per bound slot before the player block (content
		// AND fluid display seats; the player band is printed on the panel base)
		for (int i = 0; i < this.menu.playerInventoryStart(); i++) {
			net.minecraft.world.inventory.Slot tSlot = this.menu.slots.get(i);
			guiGraphics.blit(SLOT_FRAME, x + tSlot.x - 1, y + tSlot.y - 1, 0, 0, 18, 18);
		}
		// the arrow cell + the gear print — the sheets' composed furniture, same coordinates
		guiGraphics.blit(ARROW_OUTLINE, x + ARROW_X, y + ARROW_Y, 0, 0, ARROW_WIDTH, ARROW_HEIGHT);
		guiGraphics.blit(GEAR_PRINT, x + 77, y + 60, 0, 0, 22, 22);
		int tProgress = this.menu.getProgressBar();
		if (tProgress >= 0) {
			// UT.Code.scale (UT.java:1534-1542) at tSize=20: 0 at <=0, 20 at >=PROGRESS_DONE,
			// else 1 + progress*19/32767 — the upstream step ladder (the % (tSize+1) there is a
			// no-op for in-range values). The clip width covers the arrow left-to-right.
			int tFill = tProgress <= 0 ? 0
					: tProgress >= GTBasicMachineMenu.PROGRESS_DONE ? ARROW_WIDTH
					: 1 + (int)((long)tProgress * (ARROW_WIDTH - 1) / GTBasicMachineMenu.PROGRESS_DONE);
			if (tFill > 0) {
				guiGraphics.blit(ARROW_FILL, x + ARROW_X, y + ARROW_Y, 0, 0, tFill, ARROW_HEIGHT);
			}
		}
	}

	/**
	 * The jump rect probe, in GUI-relative coordinates — the {@code ARROW_*} cell itself,
	 * no margin (the static face the offline test pins; the click face
	 * {@link #onArrowClick} and the tooltip share it).
	 */
	static boolean arrowContains(double aRelX, double aRelY) {
		return aRelX >= ARROW_X && aRelX < ARROW_X + ARROW_WIDTH && aRelY >= ARROW_Y && aRelY < ARROW_Y + ARROW_HEIGHT;
	}

	private boolean onArrowClick(double aMouseX, double aMouseY) {
		return arrowContains(aMouseX - this.leftPos, aMouseY - this.topPos);
	}

	/**
	 * The GT_RectHandler click arm (NEI_RecipeMap.java:401-407): any button over the arrow
	 * opens the map's viewer page (both upstream arms landed on the same handler, :75-76);
	 * no viewer or no map → the vanilla click handling continues untouched.
	 */
	@Override
	public boolean mouseClicked(double aMouseX, double aMouseY, int aButton) {
		if (onArrowClick(aMouseX, aMouseY)
				&& gregtech6.gui.GTViewerJump.openRecipeMapPage(this.menu.tileEntity.getRecipeMap())) {
			return true;
		}
		return super.mouseClicked(aMouseX, aMouseY, aButton);
	}

	/**
	 * The GT_RectHandler tooltip arm (:419-422): the "Recipes" hover label over the rect —
	 * upstream showed it only while the current tooltip was empty, which is the no-slot
	 * case here too (the arrow area carries no slot). No viewer installed → no label (the
	 * upstream label implied the NEI plugin was present; same contract).
	 */
	@Override
	protected void renderTooltip(GuiGraphics aGuiGraphics, int aMouseX, int aMouseY) {
		if (onArrowClick(aMouseX, aMouseY) && gregtech6.gui.GTViewerJump.canJumpToViewer()) {
			aGuiGraphics.renderComponentTooltip(this.font,
					java.util.List.of(Component.literal(gregtech6.gui.GTViewerJump.RECT_TOOLTIP_TEXT)),
					aMouseX, aMouseY);
			return;
		}
		super.renderTooltip(aGuiGraphics, aMouseX, aMouseY);
	}
}
