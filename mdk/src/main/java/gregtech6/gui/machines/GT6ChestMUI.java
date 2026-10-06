package gregtech6.gui.machines;

import brachy.modularui.screen.ModularPanel;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.SlotGroupWidget;
import brachy.modularui.widgets.slot.ItemSlot;
import brachy.modularui.widgets.slot.ModularSlot;

import gregtech6.tileentity.inventories.GT6ChestBlockEntity;

/**
 * The metal-chest-family ModularUI panel factory (task material-mc-a-storage-chests — the
 * {@link GT6HopperMUI} shape): the chest face the upstream BE carries on
 * {@code getGUIServer/getGUIClient} — {@code ContainerCommonChest} (MultiTileEntityChest
 * .java:319-320, the vanilla chest container over the NBT_INV_SIZE 54 registration column,
 * Loader :132-133). ONE panel over the whole family: all 120 rows share the
 * {@link GT6ChestBlockEntity}, whose {@code buildUI} lands here. The 54 seats are the fixed
 * 6x9 chest grid (18px pitch from (8,8), the vanilla six-row chest geometry), the player
 * inventory rides at (7,128) below it, the standard MUI panel proportions stretched (the
 * 176x210 vanilla-form face). No other interactive widgets exist upstream on this GUI
 * (plain seats only), so the port adds none. NO upstream PNG (the P20 ruling): the
 * client face is the code-drawn default MUI background.
 *
 * <p>The player-inventory widget is added UNCONDITIONALLY (issue #3, the
 * {@link GT6StorageMUI} note: the fork's GuiManager.open runs createPanel before
 * menu.construct — the widget binds by sync key, the sync face rides the
 * ModularSyncManager.construct auto-bind).
 */
public final class GT6ChestMUI {

	/** The panel name (the MUI2 main-panel key — one page over the whole family). */
	public static final String PANEL_NAME = "chest";

	/** The slot group of the chest seats — the shift-transfer face. */
	public static final String GROUP_SLOTS = "chest_slots";

	/** The grid geometry — 6 rows of 9 at 18px pitch from (8,8); the player inventory at (7,128), the 176x210 vanilla six-row chest face. */
	public static final int GRID_COLS = 9;
	public static final int GRID_ROWS = 6;
	public static final int GRID_X = 8, GRID_Y = 8, PITCH = 18;
	public static final int PANEL_WIDTH = 176, PANEL_HEIGHT = 210, PLAYER_INV_Y = 128;

	private GT6ChestMUI() {
	}

	/**
	 * The family panel — runs on SERVER and CLIENT (the sync handlers must exist on
	 * both, the IUIHolder buildUI contract). The seat count reads the BE's live
	 * inventory (upstream getSizeInventory = the registration NBT_INV_SIZE).
	 */
	public static ModularPanel<?> panel(GT6ChestBlockEntity aChest, PanelSyncManager aSyncManager) {
		int tSlots = aChest.getInventory().getSlots();
		aSyncManager.registerSlotGroup(GROUP_SLOTS, GRID_COLS);
		ModularPanel<?> tPanel = ModularPanel.defaultPanel(PANEL_NAME, PANEL_WIDTH, PANEL_HEIGHT);
		for (int i = 0; i < tSlots; i++) {
			tPanel.child(new ItemSlot()
					.slot(new ModularSlot(aChest.getInventory(), i).slotGroup(GROUP_SLOTS))
					.pos(GRID_X + (i % GRID_COLS) * PITCH, GRID_Y + (i / GRID_COLS) * PITCH)
					.name("slot_" + i));
		}
		// UNCONDITIONAL — the sync handlers resolve by key at construct, no container needed
		tPanel.child(SlotGroupWidget.playerInventory((aIndex, aSlot) -> aSlot).pos(7, PLAYER_INV_Y));
		return tPanel;
	}
}
