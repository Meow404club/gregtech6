package gregtech6.gui.machines;

import brachy.modularui.screen.ModularPanel;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.SlotGroupWidget;
import brachy.modularui.widgets.slot.ItemSlot;
import brachy.modularui.widgets.slot.ModularSlot;

import gregtech6.tileentity.energy.GT6BatteryBoxBlockEntity;

/**
 * The BatteryBox-family ModularUI panel factory (task batterybox-gui — the
 * {@link GT6StorageMUI} shape, the R-L GUI census A1 card): the chest-grid battery
 * face the upstream {@code TileEntityBase10EnergyBatBox} carries on
 * {@code getGUIServer2/getGUIClient2} (:195-196 — the {@code ContainerCommonDefault}
 * {@code useDefaultSlots} form). ONE panel over the WHOLE energy-storage family: the
 * BatteryBoxes (12 port blocks), the Crystal Chargers (20) and the ZPM Dechargers (2)
 * all ride the shared {@link GT6BatteryBoxBlockEntity} (and the {@code GT6ZpmDecharger}
 * pair subclasses it), so one {@code buildUI} implementation re-GUIs all 34 machines —
 * the upstream heritage too: the Charger/Decharger classes inherit the same base GUI
 * unmodified (MultiTileEntityBatteryBox:31 / CrystalCharger:31 / ZPMDechargerEU:35).
 *
 * <p>The seat geometry is the verbatim transcription of the upstream
 * {@code ContainerCommon.addSlots} case table (the only three cases the family's slot
 * counts reach): 1 seat at (80,35) — the ZPM Decharger rows' {@code NBT_INV_SIZE 1};
 * 4 seats as the 2x2 at (71,26)..(89,44) — the Battery Box / Crystal Charger small
 * rows; 16 seats as the 4x4 at (53,8)..(107,62) — the Large rows. The player
 * inventory rides at the standard (7,84) — the {@code bindPlayerInventory} default.
 * NO upstream PNG (the P20 ruling): the panel is the default MUI background, the same
 * code-drawn form as the Safe/Drawer panels.
 *
 * <p>The player-inventory widget is added UNCONDITIONALLY (issue #3, the
 * {@link GT6StorageMUI} note: the fork's GuiManager.open runs createPanel before
 * menu.construct — the widget binds by sync key, the sync face rides the
 * ModularSyncManager.construct auto-bind).
 */
public final class GT6BatteryBoxMUI {

	/** The panel name (the MUI2 main-panel key — one page over the whole family). */
	public static final String PANEL_NAME = "battery_box";

	/** The slot group of the battery seats — the shift-transfer face. */
	public static final String GROUP_SLOTS = "battery_slots";

	private GT6BatteryBoxMUI() {
	}

	/**
	 * The family panel — runs on SERVER and CLIENT (the sync handlers must exist on
	 * both, the IUIHolder buildUI contract). The seat count reads the BE's family slot
	 * count (the NBT_INV_SIZE column: 1/4/16 — the only shapes the rows register).
	 */
	public static ModularPanel<?> panel(GT6BatteryBoxBlockEntity aBox, PanelSyncManager aSyncManager) {
		int tSlots = aBox.slots();
		aSyncManager.registerSlotGroup(GROUP_SLOTS, rowSize(tSlots));
		ModularPanel<?> tPanel = ModularPanel.defaultPanel(PANEL_NAME, 176, 166);
		int[][] tPositions = slotPositions(tSlots);
		for (int i = 0; i < tSlots; i++) {
			tPanel.child(new ItemSlot()
					.slot(new ModularSlot(aBox.getInventory(), i).slotGroup(GROUP_SLOTS))
					.pos(tPositions[i][0], tPositions[i][1])
					.name("slot_" + i));
		}
		// UNCONDITIONAL — the sync handlers resolve by key at construct, no container needed
		tPanel.child(SlotGroupWidget.playerInventory((aIndex, aSlot) -> aSlot).pos(7, 84));
		return tPanel;
	}

	/** The square-grid row size of the family's slot counts (the shift-transfer row face). */
	private static int rowSize(int aSlots) {
		return switch (aSlots) {
			case 1 -> 1;
			case 4 -> 2;
			case 16 -> 4;
			default -> throw new IllegalArgumentException("gt6 battery box: no GUI layout for " + aSlots + " slots");
		};
	}

	/**
	 * The upstream {@code ContainerCommon.addSlots} case-table rows (verbatim
	 * coordinates; row-major, the slot-index order): case 1 = (80,35), case 4 = the
	 * 2x2 at (71,26), case 16 = the 4x4 at (53,8).
	 */
	private static int[][] slotPositions(int aSlots) {
		return switch (aSlots) {
			case 1 -> new int[][] {{80, 35}};
			case 4 -> new int[][] {{71, 26}, {89, 26}, {71, 44}, {89, 44}};
			case 16 -> new int[][] {
					{53, 8}, {71, 8}, {89, 8}, {107, 8},
					{53, 26}, {71, 26}, {89, 26}, {107, 26},
					{53, 44}, {71, 44}, {89, 44}, {107, 44},
					{53, 62}, {71, 62}, {89, 62}, {107, 62}};
			default -> throw new IllegalArgumentException("gt6 battery box: no GUI layout for " + aSlots + " slots");
		};
	}
}
