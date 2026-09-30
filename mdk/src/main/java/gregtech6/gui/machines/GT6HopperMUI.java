package gregtech6.gui.machines;

import brachy.modularui.screen.ModularPanel;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.SlotGroupWidget;
import brachy.modularui.widgets.slot.ItemSlot;
import brachy.modularui.widgets.slot.ModularSlot;

import gregtech6.tileentity.inventories.GT6HopperBaseBlockEntity;

/**
 * The storage-hopper-family ModularUI panel factory (task hopper-gui-family — the
 * {@link GT6BatteryBoxMUI} shape, the R-L GUI census A2 card): the chest face the two
 * upstream BEs carry on {@code getGUIServer2/getGUIClient2} —
 * {@code MultiTileEntityHopper.java:299-300} and
 * {@code MultiTileEntityQueueHopper.java:281-282}, both
 * {@code new ContainerCommonDefault(aPlayer.inventory, this, aGUIID)} over the base
 * default (TileEntityBase04Covers:503 {@code getGUIServer2 → null} = no GUI; the
 * override IS the GUI's only authority). ONE panel over the whole family: both port BE
 * kinds ride the shared {@link GT6HopperBaseBlockEntity}, whose {@code buildUI} lands
 * here — the upstream heritage too, both upstream classes re-declare the identical
 * two-liner.
 *
 * <p>The seat geometry is the verbatim transcription of the upstream
 * {@code ContainerCommon.addSlots} default case table (gregapi/gui/ContainerCommon.java
 * :73-288 — {@code useDefaultSlots() = T}, ContainerCommonDefault.java:40), keyed by
 * {@code getSizeInventoryGUI()} = the registration {@code NBT_INV_SIZE} column
 * (TileEntityBase05Inventories.java:131 {@code mInventory.length}; the loader
 * {@code Math.max(1|2, aHopperSize)} — Loader_MultiTileEntities.java:145-146, the
 * material ladder's aHopperSize 1..36). Every case the ladder reaches is transcribed
 * (1-9, 12, 14, 15, 16, 18, 27, 36) so the hopper-matrix row expansion lands GUI-complete;
 * the current 4 registered rows reach the case-3 (Bronze, aHopperSize 3, Loader :191)
 * and case-5 (Steel, :202) grids for both kinds (queue = {@code max(2, size)}, same
 * case table). The player inventory rides at the standard (7,84) — the upstream
 * {@code bindPlayerInventory} offset (ContainerCommon.java:289/327-333). No other
 * interactive widgets exist upstream on this GUI (plain {@code Slot_Normal} seats only —
 * no buttons, no filter items), so the port adds none. NO upstream PNG (the P20
 * ruling): the client face is the code-drawn default MUI background, same as the
 * Safe/Drawer/BatteryBox panels.
 *
 * <p>The player-inventory widget is added UNCONDITIONALLY (issue #3, the
 * {@link GT6StorageMUI} note: the fork's GuiManager.open runs createPanel before
 * menu.construct — the widget binds by sync key, the sync face rides the
 * ModularSyncManager.construct auto-bind).
 */
public final class GT6HopperMUI {

	/** The panel name (the MUI2 main-panel key — one page over the whole family). */
	public static final String PANEL_NAME = "hopper";

	/** The slot group of the machine seats — the shift-transfer face. */
	public static final String GROUP_SLOTS = "hopper_slots";

	private GT6HopperMUI() {
	}

	/**
	 * The family panel — runs on SERVER and CLIENT (the sync handlers must exist on
	 * both, the IUIHolder buildUI contract). The seat count reads the BE's live
	 * inventory ({@code getSizeInventoryGUI} = {@code mInventory.length} upstream).
	 */
	public static ModularPanel<?> panel(GT6HopperBaseBlockEntity aHopper, PanelSyncManager aSyncManager) {
		int tSlots = aHopper.getInventory().getSlots();
		aSyncManager.registerSlotGroup(GROUP_SLOTS, rowSize(tSlots));
		ModularPanel<?> tPanel = ModularPanel.defaultPanel(PANEL_NAME, 176, 166);
		int[][] tPositions = slotPositions(tSlots);
		for (int i = 0; i < tSlots; i++) {
			tPanel.child(new ItemSlot()
					.slot(new ModularSlot(aHopper.getInventory(), i).slotGroup(GROUP_SLOTS))
					.pos(tPositions[i][0], tPositions[i][1])
					.name("slot_" + i));
		}
		// UNCONDITIONAL — the sync handlers resolve by key at construct, no container needed
		tPanel.child(SlotGroupWidget.playerInventory((aIndex, aSlot) -> aSlot).pos(7, 84));
		return tPanel;
	}

	/**
	 * The shift-transfer row width of each case (the seat-grid columns; the
	 * {@link GT6BatteryBoxMUI} square-grid rowSize face generalized to the chest rows).
	 */
	static int rowSize(int aSlots) {
		return switch (aSlots) {
			case 1 -> 1;
			case 2 -> 2;
			case 3 -> 3;
			case 4 -> 2;
			case 5 -> 5;
			case 6 -> 3;
			case 7 -> 7;
			case 8 -> 4;
			case 9 -> 3;
			case 12 -> 6;
			case 14 -> 7;
			case 15 -> 5;
			case 16 -> 4;
			case 18 -> 9;
			case 27 -> 9;
			case 36 -> 9;
			default -> throw new IllegalArgumentException("gt6 hopper: no GUI layout for " + aSlots + " slots");
		};
	}

	/**
	 * The upstream {@code ContainerCommon.addSlots} case table, verbatim coordinates
	 * (ContainerCommon.java:73-288; row-major = the slot-index order of the sequential
	 * {@code i++} binding). Package-visible for the transcription pin test.
	 */
	static int[][] slotPositions(int aSlots) {
		return switch (aSlots) {
			case 1 -> new int[][] {{80, 35}};
			case 2 -> new int[][] {{71, 35}, {89, 35}};
			case 3 -> new int[][] {{62, 35}, {80, 35}, {98, 35}};
			case 4 -> new int[][] {{71, 26}, {89, 26}, {71, 44}, {89, 44}};
			case 5 -> new int[][] {{44, 35}, {62, 35}, {80, 35}, {98, 35}, {116, 35}};
			case 6 -> new int[][] {{62, 26}, {80, 26}, {98, 26}, {62, 44}, {80, 44}, {98, 44}};
			case 7 -> new int[][] {{26, 35}, {44, 35}, {62, 35}, {80, 35}, {98, 35}, {116, 35}, {134, 35}};
			case 8 -> new int[][] {
					{53, 26}, {71, 26}, {89, 26}, {107, 26},
					{53, 44}, {71, 44}, {89, 44}, {107, 44}};
			case 9 -> new int[][] {
					{62, 17}, {80, 17}, {98, 17},
					{62, 35}, {80, 35}, {98, 35},
					{62, 53}, {80, 53}, {98, 53}};
			case 12 -> new int[][] {
					{35, 26}, {53, 26}, {71, 26}, {89, 26}, {107, 26}, {125, 26},
					{35, 44}, {53, 44}, {71, 44}, {89, 44}, {107, 44}, {125, 44}};
			case 14 -> new int[][] {
					{26, 26}, {44, 26}, {62, 26}, {80, 26}, {98, 26}, {116, 26}, {134, 26},
					{26, 44}, {44, 44}, {62, 44}, {80, 44}, {98, 44}, {116, 44}, {134, 44}};
			case 15 -> new int[][] {
					{44, 17}, {62, 17}, {80, 17}, {98, 17}, {116, 17},
					{44, 35}, {62, 35}, {80, 35}, {98, 35}, {116, 35},
					{44, 53}, {62, 53}, {80, 53}, {98, 53}, {116, 53}};
			case 16 -> new int[][] {
					{53, 8}, {71, 8}, {89, 8}, {107, 8},
					{53, 26}, {71, 26}, {89, 26}, {107, 26},
					{53, 44}, {71, 44}, {89, 44}, {107, 44},
					{53, 62}, {71, 62}, {89, 62}, {107, 62}};
			case 18 -> new int[][] {
					{8, 26}, {26, 26}, {44, 26}, {62, 26}, {80, 26}, {98, 26}, {116, 26}, {134, 26}, {152, 26},
					{8, 44}, {26, 44}, {44, 44}, {62, 44}, {80, 44}, {98, 44}, {116, 44}, {134, 44}, {152, 44}};
			case 27 -> grid(17, 3);
			case 36 -> grid(8, 4);
			default -> throw new IllegalArgumentException("gt6 hopper: no GUI layout for " + aSlots + " slots");
		};
	}

	/**
	 * The 9-column chest rows (the case-27/36 shape, ContainerCommon.java:221-248 /
	 * :250-287): {@code x = 8 + 18*col}, rows stepping 18 from {@code aTopY} — the same
	 * 8/26/44/62/152 ladder every 9-wide case table row spells out.
	 */
	private static int[][] grid(int aTopY, int aRowCount) {
		int[][] rGrid = new int[9 * aRowCount][];
		for (int i = 0; i < rGrid.length; i++) {
			rGrid[i] = new int[] {8 + 18 * (i % 9), aTopY + 18 * (i / 9)};
		}
		return rGrid;
	}
}
