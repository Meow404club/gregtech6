package gregtech6.gui.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import brachy.modularui.api.widget.IWidget;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.value.sync.ModularSyncManager;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.slot.ItemSlot;
import brachy.modularui.widgets.slot.ModularSlot;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.block.GTAdvancedCraftingTableBlock;
import gregtech6.menu.act.GTActMenu;
import gregtech6.tileentity.machines.TileEntityAdvancedCraftingTable;
import gregtech6.tileentity.machines.TileEntityChargingCraftingTable;
import gregtech6.tileentity.multiblocks.GTMultiBlocksOfflineTestBase;

/**
 * The ACT dual-GUI offline gate (task act-dual-gui acceptance ②, the
 * GT6BumbliaryMUIPanelTest shape): the use()-face route table (upstream
 * MultiTileEntityAdvancedCraftingTable onBlockActivated3 :115-117 verbatim), the two
 * panel widget trees (the crafting GUI 0 seats + the player inventory 36 the user
 * field-missed; the GUI 1 charging belt = the ContainerCommonDefault(…, 35, 36) case-36
 * 9x4 grid, gregapi/gui/ContainerCommon.java:250-287, + the same player bind
 * :327-334), the composed-parts regime (task act-gui-overlay-overhaul: no code
 * background — the theme plate + theme slot frames draw, the sheet's prints ride the
 * {@link GT6GuiParts} ACT cell crops, the variant picks the tool-hint print) and the
 * borrowed-sheet ledger (the sheets stay shipped as the crop sources).
 */
class GT6ActDualGuiTest extends GTMultiBlocksOfflineTestBase {

	private static final BlockPos P1 = new BlockPos(60, 64, 60);

	/** The panel-fixture BETs (the selfHolder form — the 21.1 BE ctor validates the state against the type). */
	static BlockEntityType<TileEntityAdvancedCraftingTable> sPanelActType;
	static BlockEntityType<TileEntityChargingCraftingTable> sPanelChargingType;

	@BeforeAll
	static void buildPanelActFixtureBets() {
		@SuppressWarnings("unchecked")
		BlockEntityType<TileEntityAdvancedCraftingTable>[] tActHolder =
				(BlockEntityType<TileEntityAdvancedCraftingTable>[]) new BlockEntityType<?>[1];
		tActHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new TileEntityAdvancedCraftingTable(tActHolder[0], aPos, aState),
				Blocks.BRICKS).build(null);
		sPanelActType = tActHolder[0];

		@SuppressWarnings("unchecked")
		BlockEntityType<TileEntityChargingCraftingTable>[] tChargingHolder =
				(BlockEntityType<TileEntityChargingCraftingTable>[]) new BlockEntityType<?>[1];
		tChargingHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new TileEntityChargingCraftingTable(tChargingHolder[0], aPos, aState),
				Blocks.BRICKS).build(null);
		sPanelChargingType = tChargingHolder[0];
	}

	/**
	 * The offline FML dist shaping (the GT6BumbliaryMUIPanelTest form — the vendored
	 * MUI widget classes read FMLEnvironment.dist during their static init). Must run BEFORE
	 * the first widget class initializes.
	 */
	@BeforeAll
	static void armFmlDistOffline() throws Exception {
		Class<?> tFmlEnv = Class.forName("net.minecraftforge.fml.loading.FMLEnvironment", false,
				GT6ActDualGuiTest.class.getClassLoader());
		java.lang.reflect.Field tDist = tFmlEnv.getDeclaredField("dist");
		sun.misc.Unsafe tUnsafe;
		java.lang.reflect.Field tTheUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
		tTheUnsafe.setAccessible(true);
		tUnsafe = (sun.misc.Unsafe) tTheUnsafe.get(null);
		if (tUnsafe.getObject(tFmlEnv, tUnsafe.staticFieldOffset(tDist)) == null) {
			tUnsafe.putObject(tFmlEnv, tUnsafe.staticFieldOffset(tDist),
					net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER);
		}
	}

	private static TileEntityAdvancedCraftingTable plainTable() {
		return new TileEntityAdvancedCraftingTable(sPanelActType, P1, Blocks.BRICKS.defaultBlockState());
	}

	private static TileEntityChargingCraftingTable chargingTable() {
		return new TileEntityChargingCraftingTable(sPanelChargingType, P1, Blocks.BRICKS.defaultBlockState());
	}

	/** A fresh headless sync manager (no player → the panel builds without the inventory bind). */
	private static PanelSyncManager headlessSyncManager() {
		return new PanelSyncManager(new ModularSyncManager(false), true);
	}

	/** Every widget in the tree rooted at the widget, in child order. */
	private static List<IWidget> allWidgets(IWidget aRoot) {
		List<IWidget> tAll = new ArrayList<>();
		collectAll(aRoot, tAll);
		return tAll;
	}

	private static void collectAll(IWidget aWidget, List<IWidget> aSink) {
		aSink.add(aWidget);
		if (aWidget instanceof brachy.modularui.widget.ParentWidget<?> tParent) {
			for (IWidget tChild : tParent.getChildren()) {
				collectAll(tChild, aSink);
			}
		}
	}

	/** The widget by its build name. */
	@Nullable
	private static IWidget named(ModularPanel<?> aPanel, String aName) {
		return allWidgets(aPanel).stream().filter(w -> aName.equals(w.getName())).findFirst().orElse(null);
	}

	/** The widget build position — pos() lands in the resizer's start Unit (the Bumbliary pin face). */
	private static int posOf(IWidget aWidget, boolean aX) throws Exception {
		brachy.modularui.api.widget.IPositioned tPositioned = (brachy.modularui.api.widget.IPositioned) aWidget;
		java.lang.reflect.Field tAxis = tPositioned.resizer().getClass().getDeclaredField(aX ? "x" : "y");
		tAxis.setAccessible(true);
		Object tSizer = tAxis.get(tPositioned.resizer());
		java.lang.reflect.Field tStart = tSizer.getClass().getDeclaredField("start");
		tStart.setAccessible(true);
		Object tUnit = tStart.get(tSizer);
		return (int) ((brachy.modularui.widget.sizer.Unit) tUnit).getValue();
	}

	private static void assertPos(ModularPanel<?> aPanel, String aName, int aX, int aY, String aWhat) throws Exception {
		IWidget tWidget = named(aPanel, aName);
		assertNotNull(tWidget, aWhat);
		assertEquals(aX, posOf(tWidget, true), aWhat + " x");
		assertEquals(aY, posOf(tWidget, false), aWhat + " y");
	}

	// ---------------------------------------------------------------------------
	// the use() route table — upstream :115-117 verbatim
	// ---------------------------------------------------------------------------

	/**
	 * All 6 hit faces x all 4 horizontal facings: the top opens GUI 0 (SIDES_TOP :115),
	 * the along-axis vertical pair opens GUI 1 (ALONG_AXIS :116), every other face is
	 * no GUI (the :117 return F → the port PASS).
	 */
	@Test
	public void theUseRouteTableIsTheUpstream115to117Split() {
		Direction[] tFacings = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
		for (Direction tFacing : tFacings) {
			assertEquals(0, GTAdvancedCraftingTableBlock.guiIdFor(Direction.UP, tFacing),
					"SIDES_TOP -> the crafting GUI 0 (:115), facing " + tFacing);
			assertEquals(1, GTAdvancedCraftingTableBlock.guiIdFor(tFacing, tFacing),
					"the front face -> the belt/charging GUI 1 (:116), facing " + tFacing);
			assertEquals(1, GTAdvancedCraftingTableBlock.guiIdFor(tFacing.getOpposite(), tFacing),
					"the back face -> the belt/charging GUI 1 (:116), facing " + tFacing);
			assertEquals(-1, GTAdvancedCraftingTableBlock.guiIdFor(Direction.DOWN, tFacing),
					"the bottom is no GUI (:117), facing " + tFacing);
			for (Direction tSide : Direction.values()) {
				if (tSide == Direction.UP || tSide == Direction.DOWN || tSide == tFacing
						|| tSide == tFacing.getOpposite()) continue;
				assertEquals(-1, GTAdvancedCraftingTableBlock.guiIdFor(tSide, tFacing),
						"the perpendicular side is no GUI (:117), facing " + tFacing + " side " + tSide);
			}
		}
	}

	// ---------------------------------------------------------------------------
	// the crafting panel (GUI 0) — the machine seats + the player inventory
	// ---------------------------------------------------------------------------

	@Test
	public void theCraftingPanelCarriesTheMachineSeatsAndThePlayerInventory() throws Exception {
		TileEntityAdvancedCraftingTable tTable = plainTable();
		ModularPanel<?> tPanel = GTActMenu.buildPanel(tTable, headlessSyncManager());

		assertEquals(GTActMenu.PANEL_NAME, tPanel.getName(), "the crafting panel name");
		// 16 (the 4x4 input belt) + 5 (tools) + 9 (the phantom pattern grid) + 3 (selector
		// 30 / drop 33 / neutral 34) + 36 (the player inventory — the user field-miss)
		long tSeats = allWidgets(tPanel).stream().filter(w -> w instanceof ItemSlot).count();
		assertEquals(69, tSeats, "the crafting machine seats + the 36 player seats");

		// the player inventory widget at the upstream bind offset (ContainerCommon
		// :327-334 — rows y=84/102/120, hotbar y=142; the fork-widget x precedent)
		IWidget tPlayerGroup = named(tPanel, "player_inventory");
		assertNotNull(tPlayerGroup, "the player inventory rides the tree (the GT6StorageMUI issue-#3 form)");
		assertPos(tPanel, "player_inventory", 7, 84, "the player inventory bind");
		// the hotbar present: the group's hotbar row sits at the relative +58 (=84+58=142 upstream)
		assertPos(tPanel, "slot_0", 0, 58, "the first hotbar seat (relative to the group)");
		long tPlayerSeats = allWidgets(tPlayerGroup).stream().filter(w -> w instanceof ItemSlot).count();
		assertEquals(36, tPlayerSeats, "the player 27+9 seats");
	}

	// ---------------------------------------------------------------------------
	// the belt/charging panel (GUI 1) — the case-36 9x4 grid + the player inventory
	// ---------------------------------------------------------------------------

	@Test
	public void theBeltPanelIsTheCase36GridPlusThePlayerInventory() throws Exception {
		TileEntityChargingCraftingTable tTable = chargingTable();
		ModularPanel<?> tPanel = GTActMenu.buildBeltPanel(tTable, headlessSyncManager());

		assertEquals(GTActMenu.PANEL_NAME_BELT, tPanel.getName(), "the belt panel name");
		// 36 (the storage belt 35-70) + 36 (the player inventory)
		long tSeats = allWidgets(tPanel).stream().filter(w -> w instanceof ItemSlot).count();
		assertEquals(72, tSeats, "the case-36 belt + the 36 player seats");

		// the belt seats bind 35..70 in row-major order and step (8,8) by 18
		// (gregapi/gui/ContainerCommon.java:250-287) — the UNNAMED ItemSlots (the
		// player-inventory group names all its seats; getParent() is state-gated offline)
		List<ItemSlot> tBeltSeats = new ArrayList<>();
		IWidget tBeltGroup = null;
		for (IWidget tWidget : tPanel.getChildren()) {
			if ("player_inventory".equals(tWidget.getName())) continue;
			if (tWidget instanceof brachy.modularui.widget.ParentWidget<?>) tBeltGroup = tWidget;
		}
		for (IWidget tWidget : tBeltGroup.getChildren()) {
			if (tWidget instanceof ItemSlot tSlot) tBeltSeats.add(tSlot);
		}
		assertEquals(36, tBeltSeats.size(), "the belt group holds the 36 belt slots");
		for (int i = 0; i < 36; i++) {
			assertEquals(35 + i, tBeltSeats.get(i).getSlot().getSlotIndex(),
					"the belt seat " + i + " binds the storage slot " + (35 + i));
		}
		// the belt seats step 18px from the group origin (the row-major case-36 walk)
		assertEquals(0, posOf(tBeltSeats.get(0), true), "the first belt seat relative x");
		assertEquals(0, posOf(tBeltSeats.get(0), false), "the first belt seat relative y");
		assertEquals(144, posOf(tBeltSeats.get(8), true), "row 0 tail (8*18)");
		assertEquals(0, posOf(tBeltSeats.get(9), true), "row 1 head x");
		assertEquals(18, posOf(tBeltSeats.get(9), false), "row 1 head y");
		assertEquals(144, posOf(tBeltSeats.get(35), true), "row 3 tail x");
		assertEquals(54, posOf(tBeltSeats.get(35), false), "row 3 tail y");
		// the group's own build position (8,8) — the upstream case-36 origin
		assertEquals(8, posOf(tBeltGroup, true), "the belt group x");
		assertEquals(8, posOf(tBeltGroup, false), "the belt group y");

		// the same player bind as GUI 0 (upstream one ContainerCommon.bindPlayerInventory face)
		assertNotNull(named(tPanel, "player_inventory"), "the belt panel carries the player inventory");
		assertPos(tPanel, "player_inventory", 7, 84, "the belt player inventory bind");
	}

	// ---------------------------------------------------------------------------
	// the composed base (task act-gui-overlay-overhaul) — no code background, the
	// variant rides the tool-hint print, the buttons/cells carry the sheet faces
	// ---------------------------------------------------------------------------

	/**
	 * The composed-parts regime (the user's "GUI 底是原版的+硬贴格子" fix): both panels
	 * carry NO code background — the gt6 theme's 9-slice plate draws under the panel and
	 * the theme itemSlot frame under every plain seat, so the sheet-baked frames can no
	 * longer double-stack under the code slots (the double-slot elimination). The
	 * former {@code guiSheet}/{@code panelBackground} pair is retired; the sheets stay
	 * shipped as the {@code GT6GuiParts} ACT cell-crop sources (the ledger test below).
	 */
	@Test
	public void thePanelsCarryNoCodeBackgroundTheThemePlateDraws() {
		for (ModularPanel<?> tPanel : new ModularPanel<?>[] {
				GTActMenu.buildPanel(plainTable(), headlessSyncManager()),
				GTActMenu.buildPanel(chargingTable(), headlessSyncManager()),
				GTActMenu.buildBeltPanel(chargingTable(), headlessSyncManager())}) {
			assertTrue(tPanel.getBackground() == null,
					tPanel.getName() + ": the panel carries NO code background — the theme"
							+ " plate draws (the composed base; a code sheet here was the"
							+ " double-slot root cause)");
		}
	}

	/**
	 * The variant + icon seats (task act-gui-overlay-overhaul): the five tool slots wear
	 * the sheet's holder print (the plain crop for a plain row, the charging crop for a
	 * charging row — the ONLY canvas difference between the two upstream sheets, the
	 * Loader:137 NBT_GUI semantic); the selector/drop/neutral seats wear the
	 * blueprint/arrow/P prints; the two holo-32 buttons wear the sort/flush faces; the
	 * output display wears the craft-hammer face and the four-mode craft button is the
	 * pure hit area (invisible — theme background disabled, no code background, so the
	 * print + preview show through).
	 */
	@Test
	public void theIconSeatsCarryTheSheetCellPrintsAndFollowTheVariant() throws Exception {
		ModularPanel<?> tPlainPanel = GTActMenu.buildPanel(plainTable(), headlessSyncManager());
		ModularPanel<?> tChargingPanel = GTActMenu.buildPanel(chargingTable(), headlessSyncManager());

		// the five tool slots wear the variant's holder print (slot indices 16-20) —
		// the ONLY canvas difference between the two upstream sheets (the Loader:137
		// NBT_GUI semantic, cropped per sheet)
		assertEquals(5, countTextureSeats(tPlainPanel, 16, 20,
				GT6GuiParts.ACT_CELL_TOOLS.fileName()), "plain: the five tool seats carry the plain print");
		assertEquals(5, countTextureSeats(tChargingPanel, 16, 20,
				GT6GuiParts.ACT_CELL_TOOLS_CHARGING.fileName()), "charging: the five tool seats carry the charging print");

		// the fixed-print seats on the plain panel: selector 30 = blueprint,
		// drop 33 = arrow, neutral 34 = P (the sheet's hint prints)
		assertEquals(1, countTextureSeats(tPlainPanel, 30, 30, GT6GuiParts.ACT_CELL_BLUEPRINT.fileName()),
				"the selector seat carries the blueprint print");
		assertEquals(1, countTextureSeats(tPlainPanel, 33, 33, GT6GuiParts.ACT_CELL_DROP_ARROW.fileName()),
				"the drop seat carries the arrow print");
		assertEquals(1, countTextureSeats(tPlainPanel, 34, 34, GT6GuiParts.ACT_CELL_NEUTRAL.fileName()),
				"the neutral seat carries the P print");

		// the buttons: sort (135,46) + flush (153,46) carry their faces; the craft
		// button (135,64) is the invisible hit area; the display (135,64) carries the
		// craft-hammer face
		List<IWidget> tButtons = new ArrayList<>();
		IWidget tDisplay = null;
		for (IWidget tWidget : allWidgets(tPlainPanel)) {
			if (tWidget instanceof brachy.modularui.widgets.ButtonWidget<?>) tButtons.add(tWidget);
			if (tWidget instanceof brachy.modularui.widgets.ItemDisplayWidget) tDisplay = tWidget;
		}
		assertEquals(3, tButtons.size(), "the craft button + the two holo-32 action buttons");
		int tFacedButtons = 0;
		for (IWidget tButton : tButtons) {
			if (posOf(tButton, true) == 135 && posOf(tButton, false) == 46) {
				tFacedButtons++;
				assertTexturePath(tButton, GT6GuiParts.ACT_CELL_SORT.texture().getPath(), "the sort button");
			} else if (posOf(tButton, true) == 153 && posOf(tButton, false) == 46) {
				tFacedButtons++;
				assertTexturePath(tButton, GT6GuiParts.ACT_CELL_FLUSH.texture().getPath(), "the flush button");
			} else {
				// the craft button — invisible: no code background AND the theme
				// background disabled (the vanilla bevel would cover print + preview)
				assertTrue(((brachy.modularui.widget.Widget<?>)tButton).getBackground() == null,
						"the craft button carries no code background");
				assertTrue(((brachy.modularui.widget.Widget<?>)tButton).isDisableThemeBackground(),
						"the craft button disables the theme button background (the invisible"
								+ " hit area over the print + preview cell)");
			}
		}
		assertEquals(2, tFacedButtons, "the sort + flush buttons carry their cell faces");
		assertNotNull(tDisplay, "the output display rides the tree");
		assertTexturePath(tDisplay, GT6GuiParts.ACT_CELL_CRAFT.texture().getPath(), "the output display");
	}

	/** Counts the machine ItemSlots in [aFrom, aTo] whose code background is the named part texture; asserts each match. Skips the player_inventory subtree (its seats resolve their sync handler at construct, getSlot() is build-time-null there). */
	private static int countTextureSeats(ModularPanel<?> aPanel, int aFrom, int aTo, String aPartFile) {
		java.util.Set<IWidget> tPlayerSeats = new java.util.HashSet<>();
		for (IWidget tWidget : allWidgets(aPanel)) {
			if ("player_inventory".equals(tWidget.getName())) tPlayerSeats.addAll(allWidgets(tWidget));
		}
		int rCount = 0;
		for (IWidget tWidget : allWidgets(aPanel)) {
			if (tPlayerSeats.contains(tWidget)) continue;
			if (tWidget instanceof ItemSlot tSlot && tSlot.getSlot() != null
					&& tSlot.getSlot().getSlotIndex() >= aFrom && tSlot.getSlot().getSlotIndex() <= aTo) {
				assertTexturePath(tWidget, "textures/gui/parts/" + aPartFile,
						"seat " + tSlot.getSlot().getSlotIndex());
				rCount++;
			}
		}
		return rCount;
	}

	/** The widget's code background must be the UITexture at the path. */
	private static void assertTexturePath(IWidget aWidget, String aExpectedPath, String aWhat) {
		assertTrue(aWidget instanceof brachy.modularui.widget.Widget<?>, aWhat + ": is a fork widget");
		var tBackground = ((brachy.modularui.widget.Widget<?>)aWidget).getBackground();
		assertNotNull(tBackground, aWhat + ": carries a code background");
		assertTrue(tBackground instanceof brachy.modularui.drawable.UITexture,
				aWhat + ": background is a UITexture, found " + tBackground.getClass());
		assertEquals(aExpectedPath, ((brachy.modularui.drawable.UITexture)tBackground).location.getPath(),
				aWhat + ": the cell print texture");
	}

	// ---------------------------------------------------------------------------
	// the borrowed sheets exist and match the assets/README.md ledger (they stay
	// shipped as the GT6GuiParts ACT cell-crop SOURCES after the composed-base swap)
	// ---------------------------------------------------------------------------

	@Test
	public void theBorrowedSheetsExistAndMatchTheLedger() throws Exception {
		String[] tSheets = {
				"assets/gt6/textures/gui/machines/advancedcraftingtable.png",
				"assets/gt6/textures/gui/machines/advancedcraftingtablecharging.png"};
		String[] tLedgerSha = {
				"af774ea0631d00b61242ea8d18deaba148c9fc581fd90201c615091381729a5a",
				"cd57524d0fcb72efd3519e4a0e9a62fe8e67b7422aff61194985da9a500f198a"};
		for (int i = 0; i < tSheets.length; i++) {
			byte[] tPng;
			try (var tIn = GT6ActDualGuiTest.class.getClassLoader().getResourceAsStream(tSheets[i])) {
				assertNotNull(tIn, "the borrowed sheet is on the classpath: " + tSheets[i]);
				tPng = tIn.readAllBytes();
			}
			StringBuilder tHex = new StringBuilder();
			for (byte tByte : MessageDigest.getInstance("SHA-256").digest(tPng)) {
				tHex.append(String.format("%02x", tByte));
			}
			assertEquals(tLedgerSha[i], tHex.toString(), "the sheet digest matches the ledger: " + tSheets[i]);
		}
		// the README ledger rows (the assets/README.md sha256 form, the food-can-row0 precedent)
		String tLedger;
		try (var tIn = GT6ActDualGuiTest.class.getClassLoader().getResourceAsStream("assets/README.md")) {
			assertNotNull(tIn, "the assets ledger is on the classpath");
			tLedger = new String(tIn.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
		}
		for (String tSha : tLedgerSha) {
			assertTrue(tLedger.contains(tSha), "the ledger carries the digest " + tSha);
		}
		assertTrue(tLedger.contains("advancedcraftingtable.png"), "the ledger names the plain sheet");
		assertTrue(tLedger.contains("advancedcraftingtablecharging.png"), "the ledger names the charging sheet");
	}

	// ---------------------------------------------------------------------------
	// the factory registration — the OpenGuiPacket client reverse lookup (the crash face)
	// ---------------------------------------------------------------------------

	/**
	 * The two ACT factories must be resolvable from {@code GuiManager} by name: the
	 * server {@code Factory.open} hands the factory object straight to
	 * {@code GuiManager.open} (no registry check, GuiManager.java:75), but the client
	 * {@code OpenGuiPacket} ctor re-resolves it by id (OpenGuiPacket.java:29 →
	 * GuiManager.getFactory :65-68) — an unregistered factory is the "No UI factory for
	 * name 'gt6:advanced_crafting_table'" client crash. Drives the production seam
	 * (the onModConstruct registration body, r11a-act-factory-register).
	 */
	@Test
	public void theActUIFactoriesResolveThroughTheClientLookupPath() {
		// the forge leg's plain JUnit drives the production seam here; the neo leg's
		// FML-backed JUnit harness already constructs the mod (FMLConstructModEvent →
		// onModConstruct → the seam ran, and the GuiManager duplicate guard :59-61
		// throws on a second registration) — so only drive it when unregistered.
		// Either way the lookup contract below must hold.
		if (!brachy.modularui.factory.GuiManager.hasFactory(GTActMenu.Factory.CRAFT.getFactoryName())) {
			GTBasicMachinesMenus.registerActUIFactories();
		}

		// the wire identities (the route table opens exactly these two)
		assertEquals("gt6:advanced_crafting_table", GTActMenu.Factory.CRAFT.getFactoryName().toString(),
				"the crafting factory identity (the PANEL_NAME id)");
		assertEquals("gt6:advanced_crafting_table_belt", GTActMenu.Factory.BELT.getFactoryName().toString(),
				"the belt factory identity (the PANEL_NAME_BELT id)");
		assertTrue(brachy.modularui.factory.GuiManager.hasFactory(GTActMenu.Factory.CRAFT.getFactoryName()),
				"the crafting factory is registered (the client lookup must not throw)");
		assertTrue(brachy.modularui.factory.GuiManager.hasFactory(GTActMenu.Factory.BELT.getFactoryName()),
				"the belt factory is registered (the client lookup must not throw)");
		// the exact OpenGuiPacket.java:29 call — the same instance comes back
		assertEquals(GTActMenu.Factory.CRAFT,
				brachy.modularui.factory.GuiManager.getFactory(GTActMenu.Factory.CRAFT.getFactoryName()),
				"the crafting factory round-trips (the OpenGuiPacket wire path)");
		assertEquals(GTActMenu.Factory.BELT,
				brachy.modularui.factory.GuiManager.getFactory(GTActMenu.Factory.BELT.getFactoryName()),
				"the belt factory round-trips (the OpenGuiPacket wire path)");
	}
}
