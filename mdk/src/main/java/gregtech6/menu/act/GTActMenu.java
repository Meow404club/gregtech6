package gregtech6.menu.act;

import org.jetbrains.annotations.NotNull;

import brachy.modularui.api.IUIHolder;
import brachy.modularui.api.drawable.Text;
import brachy.modularui.factory.AbstractUIFactory;
import brachy.modularui.factory.GuiManager;
import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.ModularScreen;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.GenericSyncValue;
import brachy.modularui.value.sync.InteractionSyncHandler;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.ButtonWidget;
import brachy.modularui.widgets.ItemDisplayWidget;
import brachy.modularui.widgets.SlotGroupWidget;
import brachy.modularui.widgets.slot.ItemSlot;
import brachy.modularui.widgets.slot.ModularSlot;
import brachy.modularui.widgets.slot.PhantomItemSlot;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import gregtech6.item.GT6Circuits;
import gregtech6.tileentity.GTItemStackHandler;
import gregtech6.tileentity.machines.TileEntityAdvancedCraftingTable;
import gregtech6.tileentity.machines.TileEntityChargingCraftingTable;

/**
 * The Advanced Crafting Table ModularUI panel (task act-machine C2 — the mdk-first
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
	 * <p><b>Layout (task act-dual-gui, composed in act-gui-overlay-overhaul):</b> the
	 * panel is the standard 176x166 machine GUI on the composed-parts base — the gt6
	 * theme's 9-slice plate draws the background, the theme {@code itemSlot} frame draws
	 * under every seat, and the borrowed sheet's semantic prints (the hints and the
	 * button/cell faces) ride the {@link gregtech6.gui.machines.GT6GuiParts} ACT cell
	 * crops ({@code background(...)} per seat — the sheet-baked-frame double-stack is
	 * gone with the whole-sheet background). The player inventory 36 seats ride the tree
	 * at the upstream bind offset (ContainerCommon.bindPlayerInventory :327-334 — rows at
	 * y=84/102/120, hotbar y=142; the x follows the fork's own playerInventory widget
	 * precedent, GT6StorageMUI.safePanel). The sheet-follows-VARIANT rule collapsed with
	 * the sheet: the two upstream canvases differed ONLY in the five tool-cell prints
	 * (bbox (81,9)-(166,23)), so the variant now picks the tool-hint crop the same way
	 * (upstream {@code mGUITexture} default :74, the charging registration column's
	 * NBT_GUI swap Loader_MultiTileEntities.java:137).
 *
 * <p><b>The two-GUI channel (the upstream :115-116 split)</b>: the crafting GUI (0) and
 * the belt GUI (1 — the upstream `ContainerCommonDefault(…, 35, 36)` charging GUI, the
 * ContainerCommon case-36 9x4 storage belt at (8,8)..(152,62)) open through
 * {@link Factory#CRAFT}/{@link Factory#BELT} — the GT6BumbliaryMUI.Factory form (one
 * factory per GUI, the factory identity rides the OpenGuiPacket wire, no MenuType).
 * The block use() face routes top → 0, the two along-axis vertical faces → 1, else PASS
 * (upstream :115-117 verbatim). The BE's {@code IUIHolder.buildUI} face stays the
 * crafting panel (the /gt6act open arm's default GUI 0).
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
 *
 * <p><b>Viewer faces (debt-jei-emi-batch4):</b> GHOST DRAG of a JEI/EMI ingredient onto a
 * pattern seat works through the vendored MUI layer as-is — the seats are
 * {@link PhantomItemSlot}s (MUI {@code GhostIngredientSlot}s), delivered by MUI's own JEI
 * screen handler (JEI-only) and EMI drag-drop handler (EMI; the JEI plugin early-returns
 * when EMI is present — the mutual-yield form, pinned by GT6GhostDragCensusTest). The
 * upstream overlay's OTHER half — the recipe TRANSFER moving real items — is DEFERRED:
 * upstream moved real stacks into the real 3x3 grid slots (MultiTileEntityAdvancedCraftingTable.java:716-724),
 * this port's 3x3 is the phantom pattern grid (decisions.p24-act-ghost-form) and real-item
 * transfer needs a client→server bulk-move channel MUI2 does not ship (its
 * {@code JeiContainerHandler} bridge is commented out, no {@code ModularScreen} implements
 * {@code RecipeTransferHandler}) — the act-transfer pool item re-opens when that changes.
 */
public final class GTActMenu {

	/** The panel names (the MUI2 main-panel keys — GUI 0 crafting, GUI 1 belt/charging). */
	public static final String PANEL_NAME = "advanced_crafting_table";
	public static final String PANEL_NAME_BELT = "advanced_crafting_table_belt";

	/** The belt slot group (the shift-transfer face, rowSize 9 — the case-36 grid). */
	public static final String GROUP_BELT36 = "act_belt36";

	private GTActMenu() {
	}

	/**
	 * The machine-sheet pair (the act-dual-gui borrows) is RETIRED from panel duty —
	 * task act-gui-overlay-overhaul switched both panels to the composed-parts base
	 * (the theme 9-slice plate + the theme slot frames), the whole-sheet stretch was
	 * the double-slot root cause (sheet-baked frames under the theme frames). The two
	 * PNGs stay shipped as the {@code GT6GuiParts} ACT cell-crop SOURCES (the
	 * crop_gui_parts.py provenance chain, assets/README.md both sections).
	 */

	/** Client face of {@code IUIHolder} (the TestBlockEntity :100 pattern). */
	public static ModularScreen createScreen(PosGuiData aData, ModularPanel<?> aMainPanel) {
		return new ModularScreen(brachy.modularui.ModularUI.MOD_ID, aMainPanel);
	}

	/**
	 * The panel body (GUI 0) — runs on SERVER and CLIENT (the sync handlers must exist on both).
	 *
	 * <p><b>Composed base (task act-gui-overlay-overhaul, the user's "GUI 底是原版的+硬贴
	 * 格子"):</b> the borrowed whole-sheet background is RETIRED — the panel rides the
	 * machine-same composed-parts regime (the {@link gregtech6.gui.machines.GTBasicMachineMUI}
	 * shape): the gt6 theme's 9-slice panel base draws under the panel (fork
	 * Widget.drawBackground :255-264 theme arm) and the theme's {@code itemSlot} frame
	 * ({@code modern.json} → {@code slot_frame_18x18}) draws under every seat — so the
	 * old sheet-baked frames + code slots double-stack is gone by construction (no code
	 * background, frames drawn once). The sheet's semantic prints (the hints and button
	 * faces) ride the {@link gregtech6.gui.machines.GT6GuiParts} ACT cell crops instead:
	 * the {@code background(...)} setter disables the theme frame for its seat and draws
	 * the upstream print (frame+icon in one crop — no double frame again).
	 */
	public static ModularPanel<?> buildPanel(TileEntityAdvancedCraftingTable aTable, PanelSyncManager aSyncManager) {
		GTItemStackHandler tInv = aTable.getInventory();
		aSyncManager.registerSlotGroup("act_belt16", 4); // the STORAGE_SLOT_PRIO shift-transfer face
		aSyncManager.registerSlotGroup("act_tools", 5);
		// the player-inventory SYNC face rides the fork auto-bind (ModularSyncManager.construct
		// :68-70) — the explicit bindPlayerInventory(getPlayer()) here dereferenced the null
		// menu (getPlayer() → menu.getPlayer(), ModularSyncManager.java:167-169) and NPEd on
		// every open (issue #3 reverse mine); the widget binds by sync key instead
		// the display half of the output seat — the stack syncs server→client
		aSyncManager.syncValue("act_output", GenericSyncValue.forItem(() -> tInv.getStackInSlot(31), null));

		// the tool-slot hint print follows the VARIANT (the sheet swap was the only
		// canvas difference — bbox (81,9)-(166,23), the five tool cells; Loader:137 NBT_GUI)
		gregtech6.gui.machines.GT6GuiParts.GuiPart tToolHint = aTable instanceof TileEntityChargingCraftingTable
				? gregtech6.gui.machines.GT6GuiParts.ACT_CELL_TOOLS_CHARGING
				: gregtech6.gui.machines.GT6GuiParts.ACT_CELL_TOOLS;

		return ModularPanel.defaultPanel(PANEL_NAME, 176, 166)
				// NO code background: the theme's 9-slice panel base draws here (the
				// GTBasicMachineMUI shape) — the retired .background(panelBackground(...))
				// was the double-slot half (sheet-baked frames under the theme frames)
				// the 4x4 input belt (upstream :693-708)
				.child(SlotGroupWidget.builder()
						.row("IIII").row("IIII").row("IIII").row("IIII")
						.key('I', i -> new ItemSlot().slot(new ModularSlot(tInv, i)))
						.slotGroup("act_belt16").build().pos(7, 8))
				// the five tool slots (upstream :710-714) over the variant's holder print
				.child(SlotGroupWidget.builder()
						.row("TTTTT")
						.key('T', i -> new ItemSlot().slot(new ModularSlot(tInv, 16 + i))
								.background(gregtech6.gui.machines.GT6GuiParts.asUITexture(tToolHint)))
						.slotGroup("act_tools").build().pos(80, 8))
				// the 3x3 PHANTOM PATTERN GRID (the hybrid ghost face — see the class doc)
				.child(patternGrid(aTable).pos(80, 28))
				// the selector seat (upstream :691) over the blueprint print — the handler
				// isItemValid face carries the [2, 9] whitelist
				.child(new ItemSlot().slot(new ModularSlot(tInv, 30))
						.background(gregtech6.gui.machines.GT6GuiParts.asUITexture(gregtech6.gui.machines.GT6GuiParts.ACT_CELL_BLUEPRINT))
						.pos(135, 28))
				// the drop + neutral slots (upstream :726-727) over their prints
				.child(new ItemSlot().slot(new ModularSlot(tInv, 33))
						.background(gregtech6.gui.machines.GT6GuiParts.asUITexture(gregtech6.gui.machines.GT6GuiParts.ACT_CELL_DROP_ARROW))
						.pos(153, 28))
				.child(new ItemSlot().slot(new ModularSlot(tInv, 34))
						.background(gregtech6.gui.machines.GT6GuiParts.asUITexture(gregtech6.gui.machines.GT6GuiParts.ACT_CELL_NEUTRAL))
						.pos(153, 64))
				// the output preview (holo 31) over the craft-hammer print + the four-mode
				// craft button as the invisible hit area (upstream :599-656 — the holo cell
				// renders the item OVER the print, the click arm sits on top)
				.child(new ItemDisplayWidget().syncHandler("act_output").displayAmount(true)
						.background(gregtech6.gui.machines.GT6GuiParts.asUITexture(gregtech6.gui.machines.GT6GuiParts.ACT_CELL_CRAFT))
						.pos(135, 64))
				.child(craftButton(aTable, aSyncManager).pos(135, 64))
				// the two holo-32 positions (:650-653): slotIndex 34 = flush, 35 = sort —
				// the button faces ARE the sheet's prints (symptom ③: the empty buttons)
				.child(actionButton("Flush automation bands", () -> aTable.mFlushMode = true)
						.background(gregtech6.gui.machines.GT6GuiParts.asUITexture(gregtech6.gui.machines.GT6GuiParts.ACT_CELL_FLUSH))
						.pos(153, 46))
				.child(actionButton("Sort grid into slots", () -> aTable.sortIntoTheInputSlots())
						.background(gregtech6.gui.machines.GT6GuiParts.asUITexture(gregtech6.gui.machines.GT6GuiParts.ACT_CELL_SORT))
						.pos(135, 46))
				// the player inventory at the upstream bind offset 84 (ContainerCommon
				// :327-334; the x follows the fork playerInventory widget precedent,
				// GT6StorageMUI.safePanel) — UNCONDITIONAL (the sync handlers resolve by
				// key at construct, the GT6StorageMUI issue-#3 form)
				.child(SlotGroupWidget.playerInventory((aIndex, aSlot) -> aSlot).pos(7, 84));
	}

	/**
	 * The belt/charging panel body (GUI 1) — the upstream
	 * {@code ContainerCommonDefault(aPlayer.inventory, this, aGUIID, 35, 36)} charging
	 * GUI (:585-586): the case-36 default slots 35-70 as the 9x4 grid stepping from
	 * (8,8) (gregapi/gui/ContainerCommon.java:250-287) over the same player-inventory
	 * bind as GUI 0. No interaction widgets — the upstream GUI 1 is pure Slot_Normal.
	 * Composed base like GUI 0 (task act-gui-overlay-overhaul): the theme plate + the
	 * theme slot frames, no code background — the sheet's only canvas difference lived
	 * in the crafting GUI's tool-cell prints, so both variants share this panel as-is.
	 */
	public static ModularPanel<?> buildBeltPanel(TileEntityAdvancedCraftingTable aTable, PanelSyncManager aSyncManager) {
		GTItemStackHandler tInv = aTable.getInventory();
		aSyncManager.registerSlotGroup(GROUP_BELT36, 9);
		ModularPanel<?> tPanel = ModularPanel.defaultPanel(PANEL_NAME_BELT, 176, 166);
		tPanel.child(SlotGroupWidget.builder()
				.row("IIIIIIIII").row("IIIIIIIII").row("IIIIIIIII").row("IIIIIIIII")
				.key('I', i -> new ItemSlot().slot(new ModularSlot(tInv, 35 + i)))
				.slotGroup(GROUP_BELT36).build().pos(8, 8));
		tPanel.child(SlotGroupWidget.playerInventory((aIndex, aSlot) -> aSlot).pos(7, 84));
		return tPanel;
	}

	/**
	 * The open chain of the GUI pair (the GT6BumbliaryMUI.Factory form): the framework
	 * doctrine is one factory per GUI ("to make sure they are same on client and
	 * server", SimpleUIFactory javadoc) — the factory identity rides the OpenGuiPacket
	 * wire, the variant does not touch the pos-only PosGuiData format and needs no
	 * MenuType (the P26 no-new-MenuType ruling).
	 */
	public static final class Factory extends AbstractUIFactory<PosGuiData> {

		/** The crafting GUI factory (upstream openGUI(aPlayer, 0), :115 top face). */
		public static final Factory CRAFT = new Factory(PANEL_NAME, false);
		/** The belt/charging GUI factory (upstream openGUI(aPlayer, 1), :116 along-axis faces). */
		public static final Factory BELT = new Factory(PANEL_NAME_BELT, true);

		private final boolean mBelt;

		private Factory(String aName, boolean aBelt) {
			super(ResourceLocation.fromNamespaceAndPath("gt6", aName)); // the two-arg ctor is private in 1.21.1 (the GTWireBakedModel idiom)
			mBelt = aBelt;
		}

		/** The variant flag — the panel dispatch key. */
		public boolean belt() {
			return mBelt;
		}

		/** The server open (the BlockEntityUIFactory.open shape over this factory's identity). */
		public void open(ServerPlayer aPlayer, TileEntityAdvancedCraftingTable aTable) {
			GuiManager.open(this, new PosGuiData(aPlayer, aTable.getBlockPos()), aPlayer);
		}

		/** The panel dispatch — the variant rides the factory, not the wire data. */
		@Override
		public ModularPanel<?> createPanel(PosGuiData aData, PanelSyncManager aSyncManager, UISettings aSettings) {
			TileEntityAdvancedCraftingTable tTable = (TileEntityAdvancedCraftingTable)aData.getBlockEntity();
			return mBelt ? buildBeltPanel(tTable, aSyncManager) : buildPanel(tTable, aSyncManager);
		}

		@Override
		public @NotNull IUIHolder<PosGuiData> getGuiHolder(PosGuiData aData) {
			return (TileEntityAdvancedCraftingTable)aData.getBlockEntity();
		}

		@Override
		public boolean canInteractWith(Player aPlayer, PosGuiData aGuiData) {
			// the BlockEntityUIFactory :70-73 body verbatim — the 64-sq range gate
			return aPlayer == aGuiData.getPlayer() && aGuiData.getBlockEntity() != null
					&& aGuiData.getSquaredDistance(aPlayer) <= 64;
		}

		/** The wire faces — the BlockEntityUIFactory :77/:86 bodies verbatim (pos-only; the variant rides the factory identity). */
		@Override
		//? if forge {
		public void writeGuiData(PosGuiData aGuiData, net.minecraft.network.FriendlyByteBuf aBuffer) {
			aBuffer.writeBlockPos(aGuiData.getBlockPos());
		}

		@Override
		public @NotNull PosGuiData readGuiData(Player aPlayer, net.minecraft.network.FriendlyByteBuf aBuffer) {
			return new PosGuiData(aPlayer, aBuffer.readBlockPos());
		}
		//?} else {
		/*public void writeGuiData(PosGuiData aGuiData, net.minecraft.network.RegistryFriendlyByteBuf aBuffer) {
			aBuffer.writeBlockPos(aGuiData.getBlockPos());
		}

		@Override
		public @NotNull PosGuiData readGuiData(Player aPlayer, net.minecraft.network.RegistryFriendlyByteBuf aBuffer) {
			return new PosGuiData(aPlayer, aBuffer.readBlockPos());
		}
		 *///?}
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

	/**
	 * The four-mode craft button — the MouseData axis maps the upstream slotClick modes.
	 * Pure HIT AREA (task act-gui-overlay-overhaul): the upstream holo cell renders the
	 * crafted preview OVER the hammer print, so the click arm rides
	 * {@link ButtonWidget#invisible} — the theme button background would paint a vanilla
	 * bevel box over the print + preview (the old "empty button over the output" stack).
	 */
	private static ButtonWidget<?> craftButton(TileEntityAdvancedCraftingTable aTable, PanelSyncManager aSyncManager) {
		return new ButtonWidget<>()
				.size(18)
				.invisible()
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

	/**
	 * One holo-32 action button — the server action runs the upstream arm verbatim. The
	 * face IS the sheet's cell print (task act-gui-overlay-overhaul, the "按钮图标全空"
	 * fix: the caller attaches the {@code ACT_CELL_*} crop via {@code background(...)},
	 * which also disables the theme button background — no vanilla bevel over the print).
	 * The tooltip rides the DYNAMIC form: the fork's every text constructor funnels through
	 * the package-private vanilla MutableComponent ctor (the fork accesstransformer.cfg
	 * line, applied by FML at mod-load only), so an EAGER tooltip/overlay text would
	 * throw IllegalAccessError on any headless panel build (the offline tests); the
	 * dynamic builder defers construction to the client hover render, identical output
	 * for constant text.
	 */
	private static ButtonWidget<?> actionButton(String aTooltip, Runnable aAction) {
		return new ButtonWidget<>()
				.size(18)
				.tooltipDynamic(aTooltipConsumer -> aTooltipConsumer.addLine(Text.str(aTooltip)))
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
