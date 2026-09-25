package gregtech6.menu.act;

import brachy.modularui.api.drawable.Text;
import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.ModularScreen;
import brachy.modularui.value.sync.GenericSyncValue;
import brachy.modularui.value.sync.InteractionSyncHandler;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.ButtonWidget;
import brachy.modularui.widgets.ItemDisplayWidget;
import brachy.modularui.widgets.SlotGroupWidget;
import brachy.modularui.widgets.slot.ItemSlot;
import brachy.modularui.widgets.slot.ModularSlot;
import brachy.modularui.widgets.slot.PhantomItemSlot;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import gregtech6.item.GT6Circuits;
import gregtech6.tileentity.GTItemStackHandler;
import gregtech6.tileentity.machines.TileEntityAdvancedCraftingTable;

/**
 * The Advanced Crafting Table ModularUI panel (task p24-act-machine C2 — the mdk-first
 * ModularUI consumer, decisions.p24-act-be-form). The MUI2 machine path: the BE
 * implements {@code IUIHolder<PosGuiData>}, the panel builds SERVER+CLIENT in
 * {@link #buildPanel} (the sync handlers register there), the client screen wraps it in
 * {@link #createScreen}; opening rides {@code BlockEntityUIFactory.INSTANCE.open} (the
 * factory's own network — no vanilla MenuType of ours, the fallback arm stayed unused).
 *
 * <p>Layout mirrors the upstream addSlots :690-744: the 4x4 input belt (0-15), the five
 * tool slots (16-20), the selector slot (30), the drop slot (33) and the neutral slot
 * (34); the output preview (31) is a display widget + an interaction button — the
 * upstream holo click semantics — and the TWO holo-32 positions (:650-653: slotIndex
 * 34 = flush, 35 = sort) become the two action buttons. The 9x4 belt (35-70) is
 * automation-only upstream and shows nowhere in the GUI either.
 *
 * <p>The GUI 3x3 = the PHANTOM PATTERN GRID (decisions.p24-act-ghost-form ②: "GUI 端
 * ModularUI PhantomItemSlot 绑 mPattern") — nine {@link PhantomItemSlot} seats over the
 * {@link GTActPatternHandler} view of the mPattern backing; click/scroll edits route
 * through PhantomItemSlotSyncHandler.readOnServer (:68-79, both legs) into
 * {@code slot.set} on the adapter, which normalizes to count 1 and gates on the
 * selector's presence. The REAL 21-29 slots keep their IItemHandler semantics
 * (consumption stock) without a GUI seat — the upstream sweep (:215-221) pulls their
 * stock into the destination belt cell anyway.
 *
 * <p>The four output click modes (upstream slotClick :599-656) map onto the single
 * interaction button through the MouseData axis:
 * <ul>
 * <li>LEFT = one craft onto the cursor ({@code craftOnce}, :644-646);</li>
 * <li>RIGHT = fill the cursor to the vanilla cap ({@code craftFillCell}, :634-643);</li>
 * <li>SHIFT+LEFT = single-cell inventory traversal ({@code craftTraverse(true)}, :618-632);</li>
 * <li>SHIFT+RIGHT = full inventory traversal ({@code craftTraverse(false)}, :604-617).</li>
 * </ul>
 * All modes gate on {@code canDoCraftingOutput} (:290) inside the BE methods.
 */
public final class GTActMenu {

	/** The panel name (the MUI2 main-panel key). */
	public static final String PANEL_NAME = "advanced_crafting_table";

	private GTActMenu() {
	}

	/** Client face of {@code IUIHolder} (the TestBlockEntity :100 pattern). */
	public static ModularScreen createScreen(PosGuiData aData, ModularPanel<?> aMainPanel) {
		return new ModularScreen(brachy.modularui.ModularUI.MOD_ID, aMainPanel);
	}

	/** The panel body — runs on SERVER and CLIENT (the sync handlers must exist on both). */
	public static ModularPanel<?> buildPanel(TileEntityAdvancedCraftingTable aTable, PanelSyncManager aSyncManager) {
		GTItemStackHandler tInv = aTable.getInventory();
		aSyncManager.registerSlotGroup("act_belt16", 4); // the STORAGE_SLOT_PRIO shift-transfer face
		aSyncManager.registerSlotGroup("act_tools", 5);
		// the player-inventory SYNC face rides the fork auto-bind (ModularSyncManager.construct
		// :68-70) — the explicit bindPlayerInventory(getPlayer()) here dereferenced the null
		// menu (getPlayer() → menu.getPlayer(), ModularSyncManager.java:167-169) and NPEd on
		// every open (issue #3 reverse mine); the panel carries no player widget by design
		// (the 176x210 layout, the sync face only)
		// the display half of the output seat — the stack syncs server→client
		aSyncManager.syncValue("act_output", GenericSyncValue.forItem(() -> tInv.getStackInSlot(31), null));

		return ModularPanel.defaultPanel(PANEL_NAME, 176, 210)
				// the 4x4 input belt (upstream :693-708)
				.child(SlotGroupWidget.builder()
						.row("IIII").row("IIII").row("IIII").row("IIII")
						.key('I', i -> new ItemSlot().slot(new ModularSlot(tInv, i)))
						.slotGroup("act_belt16").build().pos(7, 8))
				// the five tool slots (upstream :710-714)
				.child(SlotGroupWidget.builder()
						.row("TTTTT")
						.key('T', i -> new ItemSlot().slot(new ModularSlot(tInv, 16 + i)))
						.slotGroup("act_tools").build().pos(80, 8))
				// the 3x3 PHANTOM PATTERN GRID (the hybrid ghost face — see the class doc)
				.child(patternGrid(aTable).pos(80, 28))
				// the selector seat (upstream :691) — the handler isItemValid face carries the [2, 9] whitelist
				.child(new ItemSlot().slot(new ModularSlot(tInv, 30)).pos(135, 28))
				// the drop + neutral slots (upstream :726-727)
				.child(new ItemSlot().slot(new ModularSlot(tInv, 33)).pos(153, 28))
				.child(new ItemSlot().slot(new ModularSlot(tInv, 34)).pos(153, 64))
				// the output preview (holo 31) + the four-mode craft button (upstream :599-656)
				.child(new ItemDisplayWidget().syncHandler("act_output").displayAmount(true).pos(135, 64))
				.child(craftButton(aTable, aSyncManager).pos(135, 64))
				// the two holo-32 positions (:650-653): slotIndex 34 = flush, 35 = sort
				.child(actionButton("F", "Flush automation bands", () -> aTable.mFlushMode = true).pos(153, 46))
				.child(actionButton("S", "Sort grid into slots", () -> aTable.sortIntoTheInputSlots()).pos(135, 46));
	}

	/** The 3x3 phantom pattern seats over the mPattern view (the hybrid ghost face). */
	public static SlotGroupWidget patternGrid(TileEntityAdvancedCraftingTable aTable) {
		GTActPatternHandler tPattern = new GTActPatternHandler(aTable,
				() -> {
					ItemStack tSelector = aTable.getInventory().getStackInSlot(30);
					return GT6Circuits.isSelector(tSelector)
							&& TileEntityAdvancedCraftingTable.isSelectorConfigValid(GT6Circuits.configurationOf(tSelector));
				});
		return SlotGroupWidget.builder()
				.row("PPP").row("PPP").row("PPP")
				.key('P', i -> new PhantomItemSlot().slot(new ModularSlot(tPattern, i).ignoreMaxStackSize(true)))
				.build();
	}

	/** The four-mode craft button — the MouseData axis maps the upstream slotClick modes. */
	private static ButtonWidget<?> craftButton(TileEntityAdvancedCraftingTable aTable, PanelSyncManager aSyncManager) {
		return new ButtonWidget<>()
				.size(18)
				.syncHandler(new InteractionSyncHandler()
						.setOnMousePressed(aMouseData -> {
							if (aMouseData.shift()) {
								// SHIFT+LEFT stops at the first productive cell; SHIFT+RIGHT traverses all (:618-632 / :604-617)
								aTable.craftTraverse(inventorySink(aSyncManager), !aMouseData.isRightMouseButton());
							} else if (aMouseData.isRightMouseButton()) {
								aTable.craftFillCell(cursorSink(aSyncManager), 0); // :634-643
							} else {
								aTable.craftOnce(cursorSink(aSyncManager), 0); // :644-646
							}
						}));
	}

	/** One holo-32 action button — the server action runs the upstream arm verbatim. */
	private static ButtonWidget<?> actionButton(String aLabel, String aTooltip, Runnable aAction) {
		return new ButtonWidget<>()
				.size(18)
				.tooltip(aTooltipConsumer -> aTooltipConsumer.addLine(Text.str(aTooltip)))
				.overlay(Text.str(aLabel).scale(0.7f))
				.syncHandler(new InteractionSyncHandler()
						.setOnMousePressed(aMouseData -> aAction.run()));
	}

	/** The cursor as a one-cell sink (the upstream cursor stack, :640/:645). */
	private static TileEntityAdvancedCraftingTable.ICraftOutputSink cursorSink(PanelSyncManager aSyncManager) {
		return new TileEntityAdvancedCraftingTable.ICraftOutputSink() {
			@Override public int cellCount() {
				return 1;
			}

			@Override public ItemStack getHold(int aCell) {
				return aSyncManager.getCursorItem();
			}

			@Override public void setHold(int aCell, ItemStack aHold) {
				aSyncManager.setCursorItem(aHold);
			}
		};
	}

	/** The player main inventory (36 cells) as the shift-mode traversal sink (:606-631). */
	private static TileEntityAdvancedCraftingTable.ICraftOutputSink inventorySink(PanelSyncManager aSyncManager) {
		Player tPlayer = aSyncManager.getPlayer();
		Inventory tInventory = tPlayer.getInventory();
		return new TileEntityAdvancedCraftingTable.ICraftOutputSink() {
			@Override public int cellCount() {
				return 36; // the upstream mainInventory length
			}

			@Override public ItemStack getHold(int aCell) {
				return tInventory.getItem(aCell);
			}

			@Override public void setHold(int aCell, ItemStack aHold) {
				tInventory.setItem(aCell, aHold);
			}
		};
	}
}
