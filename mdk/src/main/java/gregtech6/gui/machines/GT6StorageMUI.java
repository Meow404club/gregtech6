package gregtech6.gui.machines;

import brachy.modularui.screen.ModularPanel;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.SlotGroupWidget;
import brachy.modularui.widgets.slot.ItemSlot;
import brachy.modularui.widgets.slot.ModularSlot;

import gregtech6.tileentity.inventories.GT6DrawerQuadBlockEntity;
import gregtech6.tileentity.inventories.GT6SafeBlockEntity;

/**
 * The static-storage batch MUI panel factory (task p26-storage-static-batch — the
 * {@link GTBasicMachineMUI} shape, container edition: no progress values, slot grids only).
 * The wave-4 GUI ruling: the storage containers with an upstream GUI (DrawerQuad/Safe)
 * open through {@link GT6MuiMachine#tryOpen} with ZERO new MenuType; the panels build on
 * SERVER and CLIENT inside the BEs' {@code buildUI} (IUIHolder.java:21-44 contract).
 *
 * <p>The player-inventory widget is added UNCONDITIONALLY (issue #3: the old
 * {@code getContainer() == null} headless gate was always-true at runtime — the fork's
 * GuiManager.open runs createPanel before menu.construct; the widget binds by sync key and
 * needs no container, the sync face rides the ModularSyncManager.construct auto-bind).
 */
public final class GT6StorageMUI {

	/** The panel names (the MUI2 main-panel keys, the BET paths they belong to). */
	public static final String DRAWER_PANEL = "drawer_quad";
	public static final String SAFE_PANEL = "safe";

	/** The slot groups — one per drawer quadrant (rowSize 9 = the shift-transfer face), one for the safe row. */
	public static final String GROUP_Q0 = "dq_q0", GROUP_Q1 = "dq_q1", GROUP_Q2 = "dq_q2", GROUP_Q3 = "dq_q3";
	public static final String GROUP_SAFE = "safe_slots";

	/** The drawer panel geometry: two quadrant columns of 162px + a 16px gutter + 8px margins. */
	public static final int DRAWER_WIDTH = 356, DRAWER_HEIGHT = 250;
	static final int QUAD_RIGHT_X = 186, QUAD_BOTTOM_Y = 86;

	private GT6StorageMUI() {
	}

	/**
	 * The DrawerQuad panel — the wave-4 deviation declaration as a widget tree: ONE page
	 * with all 144 slots in the four quadrant blocks (36 each, 9x4), the modern answer to
	 * the 1.7.10 click-a-quadrant-open-36 window (upstream :88/:103-104). Quadrant q
	 * occupies slots {@code q*36..q*36+35} ({@link GT6DrawerQuadBlockEntity#quadrantBase}).
	 */
	public static ModularPanel<?> drawerPanel(GT6DrawerQuadBlockEntity aDrawer, PanelSyncManager aSyncManager) {
		aSyncManager.registerSlotGroup(GROUP_Q0, 9);
		aSyncManager.registerSlotGroup(GROUP_Q1, 9);
		aSyncManager.registerSlotGroup(GROUP_Q2, 9);
		aSyncManager.registerSlotGroup(GROUP_Q3, 9);
		// the player-inventory SYNC face rides the fork auto-bind (ModularSyncManager.construct
		// :68-70 — an explicit bindPlayerInventory here would NPE on the null menu, issue #3)
		ModularPanel<?> tPanel = ModularPanel.defaultPanel(DRAWER_PANEL, DRAWER_WIDTH, DRAWER_HEIGHT);
		tPanel.child(quadrantGrid(aDrawer, 0, GROUP_Q0).pos(8, 8));
		tPanel.child(quadrantGrid(aDrawer, 1, GROUP_Q1).pos(QUAD_RIGHT_X, 8));
		tPanel.child(quadrantGrid(aDrawer, 2, GROUP_Q2).pos(8, QUAD_BOTTOM_Y));
		tPanel.child(quadrantGrid(aDrawer, 3, GROUP_Q3).pos(QUAD_RIGHT_X, QUAD_BOTTOM_Y));
		// the player inventory, centered under the quadrant field (the GTBasicMachineMUI
		// playerInventory-widget face — 3x9 block + hotbar); UNCONDITIONAL (issue #3)
		tPanel.child(SlotGroupWidget.playerInventory((aIndex, aSlot) -> aSlot).pos(97, 166));
		return tPanel;
	}

	/** One quadrant: 36 slots as four 9-wide rows ({@code base = quadrant * 36}). */
	private static SlotGroupWidget quadrantGrid(GT6DrawerQuadBlockEntity aDrawer, int aQuadrant, String aGroup) {
		int tBase = GT6DrawerQuadBlockEntity.quadrantBase(aQuadrant);
		return SlotGroupWidget.builder()
				.row("IIIIIIIII").row("IIIIIIIII").row("IIIIIIIII").row("IIIIIIIII")
				.key('I', i -> new ItemSlot().slot(new ModularSlot(aDrawer.getInventory(), tBase + i)))
				.slotGroup(aGroup)
				.build();
	}

	/**
	 * The Safe panel — the upstream 15-slot GUI (NBT_INV_SIZE 15, Loader :134-135; the
	 * ContainerCommonDefault full-inventory form) on the standard 176x166 panel: 5x3
	 * centered, the player inventory at the machine offset.
	 */
	public static ModularPanel<?> safePanel(GT6SafeBlockEntity aSafe, PanelSyncManager aSyncManager) {
		aSyncManager.registerSlotGroup(GROUP_SAFE, 5);
		// the player-inventory SYNC face rides the fork auto-bind (issue #3, see drawerPanel)
		ModularPanel<?> tPanel = ModularPanel.defaultPanel(SAFE_PANEL, 176, 166);
		tPanel.child(SlotGroupWidget.builder()
				.row("IIIII").row("IIIII").row("IIIII")
				.key('I', i -> new ItemSlot().slot(new ModularSlot(aSafe.getInventory(), i)))
				.slotGroup(GROUP_SAFE)
				.build().pos(43, 8));
		// UNCONDITIONAL — the sync handlers resolve by key at construct, no container needed
		tPanel.child(SlotGroupWidget.playerInventory((aIndex, aSlot) -> aSlot).pos(7, 84));
		return tPanel;
	}
}
