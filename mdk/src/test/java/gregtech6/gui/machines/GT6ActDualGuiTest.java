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
import net.minecraft.resources.ResourceLocation;
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
 * :327-334), the variant-follows-the-sheet rule (Loader_MultiTileEntities.java:137
 * NBT_GUI over the :74 plain default) and the borrowed-sheet ledger.
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
	// the sheet follows the VARIANT (upstream NBT_GUI :137 over the :74 default)
	// ---------------------------------------------------------------------------

	@Test
	public void theGuiSheetFollowsTheVariantNotTheGuiId() {
		ResourceLocation tPlain = GTActMenu.guiSheet(plainTable());
		assertEquals("gt6", tPlain.getNamespace(), "the sheet namespace");
		assertEquals("textures/gui/machines/advancedcraftingtable.png", tPlain.getPath(),
				"the plain row keeps the :74 mGUITexture default");

		ResourceLocation tCharging = GTActMenu.guiSheet(chargingTable());
		assertEquals("textures/gui/machines/advancedcraftingtablecharging.png", tCharging.getPath(),
				"the charging row swaps the sheet (Loader:137 NBT_GUI)");
	}

	// ---------------------------------------------------------------------------
	// the borrowed sheets exist and match the assets/README.md ledger
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
}
