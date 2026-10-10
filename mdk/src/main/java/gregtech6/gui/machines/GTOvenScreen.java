package gregtech6.gui.machines;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import gregtech6.gui.GTGuiScreen;

/**
 * Oven screen — the client half replacing upstream getGUIClient2/ContainerClientBasicMachine
 * (MultiTileEntityBasicMachine.java:1007), statically mounted via MenuScreens.register
 * (GTClientOvenListener). Panel = the standard 176x166 machine GUI composed from the
 * composable GUI part crops (task machine-gui-composed-purge, the user ruling
 * "全部改成拼接，把完整图全部清了" — the machines/oven.png sheet is retired): the
 * {@link GT6GuiParts#PANEL_BASE} theme panel (machine-area blank, the printed player band)
 * under the slot frames ({@link GT6GuiParts#SLOT_FRAME} at each bound slot's (x-1,y-1)),
 * the progress-arrow cell print ({@link GT6GuiParts#ARROW_OUTLINE} at (78,24)) and the
 * decorative gear print ({@link GT6GuiParts#SLOT_SPECIAL} at (77,60)) — the exact furniture
 * the sheet baked, pixel-aligned to the same coordinates.
 *
 * <p>Progress bar (spec 6): the three-state ContainerData value drives a flat fill between
 * the input and output slots — the A-tier stand-in for the upstream RecipeMap progress
 * arrow (mProgressBarDirection/Amount are the NEI/GUI metadata of the 1.7.10 texture
 * pipeline and stay out until the real GUI atlas work).
 */
public class GTOvenScreen extends GTGuiScreen<GTOvenMenu> {

	/** The arrow cell print — the empty progress arrow the machine skins print at (78,24). */
	private static final ResourceLocation ARROW_OUTLINE = GT6GuiParts.ARROW_OUTLINE.texture();

	/** The decorative gear print — the 22x22 special-slot cell every machine skin carries at (77,60). */
	private static final ResourceLocation GEAR_PRINT = GT6GuiParts.SLOT_SPECIAL.texture();

	/** The slot frame cell — the standard 18x18 white-stroke frame, painted at (slot.x-1, slot.y-1). */
	private static final ResourceLocation SLOT_FRAME = GT6GuiParts.SLOT_FRAME.texture();

	/** Bar geometry in panel coordinates: the 36px gap between the input slot (right edge 71) and the output slot (107). */
	private static final int BAR_X = 73, BAR_Y = 30, BAR_WIDTH = 32, BAR_HEIGHT = 4;

	private static final int COLOR_TRACK = 0xFF373737;
	private static final int COLOR_FILL = 0xFFFF8800;

	public GTOvenScreen(GTOvenMenu menu, Inventory playerInventory, Component title) {
		// the panel base rides the vanilla-leg blit face (GTGuiScreen blits it from UV 0,0
		// in renderBg) — the composed furniture stacks on top below
		super(menu, playerInventory, title, GT6GuiParts.PANEL_BASE.texture());
		this.imageWidth = 176;
		this.imageHeight = 166;
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
		super.renderBg(guiGraphics, partialTick, mouseX, mouseY); // GTGuiScreen blits the panel base from UV 0,0
		int x = (this.width - this.imageWidth) / 2;
		int y = (this.height - this.imageHeight) / 2;
		// the slot frames — one 18x18 cell per bound slot before the player block (the sheet
		// painted the frame at (slot-1) around each 16x16 seat; the player band is printed
		// on the panel base)
		for (int i = 0; i < this.menu.playerInventoryStart(); i++) {
			Slot tSlot = this.menu.slots.get(i);
			guiGraphics.blit(SLOT_FRAME, x + tSlot.x - 1, y + tSlot.y - 1, 0, 0, 18, 18);
		}
		// the arrow cell + the gear print — the sheet's composed furniture, same coordinates
		guiGraphics.blit(ARROW_OUTLINE, x + 78, y + 24, 0, 0, 20, 18);
		guiGraphics.blit(GEAR_PRINT, x + 77, y + 60, 0, 0, 22, 22);
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
