package gregtech6.gui.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import brachy.modularui.api.widget.IWidget;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.value.sync.ModularSyncManager;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.slot.ItemSlot;
import brachy.modularui.widgets.slot.ModularSlot;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.registry.GT6Hoppers;
import gregtech6.registry.GT6Hoppers.GT6HopperBlock;
import gregtech6.tileentity.inventories.GT6HopperBaseBlockEntity;
import gregtech6.tileentity.inventories.GT6HopperBlockEntity;
import gregtech6.tileentity.inventories.GT6QueueHopperBlockEntity;
import gregtech6.tileentity.multiblocks.GTMultiBlocksOfflineTestBase;

/**
 * The storage-hopper-family offline panel gate (task hopper-gui-family, the
 * GT6BatteryBoxMUIPanelTest shape): the case-3/case-5 grids of the Bronze/Steel anchor
 * rows of the hopper-matrix 120-row ladder (Bronze/Steel x hopper/queue — Loader
 * :191/:202 over :145-146; the rebase-of-record fix — the rows are picked BY PATH, the
 * pre-matrix positional {@code ROWS.get(0..3)} died when hopper-matrix widened ROWS to
 * 60 materials x 2 kinds), the verbatim transcription pin over the WHOLE upstream
 * {@code ContainerCommon.addSlots} default case table (the 120-row ladder reaches every
 * case), the player-inventory pin and the family-reach pins — ONE base-BE
 * {@code buildUI} GUIs both kinds.
 */
class GT6HopperMUIPanelTest extends GTMultiBlocksOfflineTestBase {

	private static final BlockPos P1 = new BlockPos(50, 64, 50);

	/** The panel-fixture BETs (the selfHolder form — the 21.1 BE ctor validates the state against the type, so the cached REAL row blocks ride the valid set). */
	static BlockEntityType<GT6HopperBlockEntity> sHopperType;
	static BlockEntityType<GT6QueueHopperBlockEntity> sQueueType;

	/** The cached row blocks — ONE instance per row, shared by the BET valid set and every fixture BE (the identity the isValid face compares). */
	static GT6HopperBlock sBronze, sSteel, sQueueBronze, sQueueSteel;

	@BeforeAll
	static void buildFixtureBets() {
		// the row blocks are constructed offline, after the boot freeze — the m4-test-infra
		// remedy (GTOfflineTestBase.unfreezeBlockEntityTypeRegistry face) applied to the
		// BLOCK registry: reopen the write window so the Block intrusive-holder init passes
		try {
			Object tRegistry = net.minecraft.core.registries.BuiltInRegistries.BLOCK;
			java.lang.reflect.Method tUnfreeze = tRegistry.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(tRegistry);
		} catch (Throwable ignored) {
			// fixture construction falls back to the vanilla-STONE state; never mask a test assertion
		}
		sBronze = blockOf(row("hopper_bronze"));
		sSteel = blockOf(row("hopper_steel"));
		sQueueBronze = blockOf(row("queue_hopper_bronze"));
		sQueueSteel = blockOf(row("queue_hopper_steel"));
		// the valid sets carry EVERY row block of their kind — the family-reach loop
		// instantiates a BE per registered row (the 21.1 BE ctor validates the state
		// against the type, so the state's block instance must ride the BET valid set)
		List<net.minecraft.world.level.block.Block> tHopperValid = new ArrayList<>();
		tHopperValid.add(Blocks.STONE);
		List<net.minecraft.world.level.block.Block> tQueueValid = new ArrayList<>();
		tQueueValid.add(Blocks.STONE);
		for (GT6Hoppers.HopperRow tRow : GT6Hoppers.ROWS) {
			(tRow.queue() ? tQueueValid : tHopperValid).add(blockOf(tRow));
		}
		@SuppressWarnings("unchecked")
		BlockEntityType<GT6HopperBlockEntity>[] tHopper = (BlockEntityType<GT6HopperBlockEntity>[]) new BlockEntityType<?>[1];
		tHopper[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6HopperBlockEntity(tHopper[0], aPos, aState),
				tHopperValid.toArray(new Block[0])).build(null);
		sHopperType = tHopper[0];
		@SuppressWarnings("unchecked")
		BlockEntityType<GT6QueueHopperBlockEntity>[] tQueue = (BlockEntityType<GT6QueueHopperBlockEntity>[]) new BlockEntityType<?>[1];
		tQueue[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6QueueHopperBlockEntity(tQueue[0], aPos, aState),
				tQueueValid.toArray(new Block[0])).build(null);
		sQueueType = tQueue[0];
	}

	/** The row by its registered path (name-keyed — the positional get(i) died with the matrix). */
	private static GT6Hoppers.HopperRow row(String aPath) {
		return GT6Hoppers.ROWS.stream().filter(r -> r.path().equals(aPath)).findFirst()
				.orElseThrow(() -> new AssertionError("hopper row missing: " + aPath));
	}

	/** The offline-built block of a row (ONE instance per row, cached — the identity the BET valid set and the state-vs-type check compare). */
	private static final Map<String, GT6HopperBlock> sRowBlocks = new HashMap<>();

	private static GT6HopperBlock blockOf(GT6Hoppers.HopperRow aRow) {
		return sRowBlocks.computeIfAbsent(aRow.path(), p -> new GT6HopperBlock(aRow, aRow.properties()));
	}

	/** The cached block of a row (ONE instance per row — see the field doc). */
	private static GT6HopperBlock rowBlock(GT6Hoppers.HopperRow aRow) {
		return blockOf(aRow);
	}

	/**
	 * The offline FML dist shaping (the Bumbliary form — the vendored MUI widget classes
	 * read FMLEnvironment.dist during their static init). Must run BEFORE the first
	 * widget class initializes.
	 */
	@BeforeAll
	static void armFmlDistOffline() throws Exception {
		Class<?> tFmlEnv = Class.forName("net.minecraftforge.fml.loading.FMLEnvironment", false,
				GT6HopperMUIPanelTest.class.getClassLoader());
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

	/** The fixture BE of a row — the real row block's state drives the inventorySize read. */
	private static GT6HopperBaseBlockEntity beOf(GT6Hoppers.HopperRow aRow) {
		return aRow.queue()
				? new GT6QueueHopperBlockEntity(sQueueType, P1, rowBlock(aRow).defaultBlockState())
				: new GT6HopperBlockEntity(sHopperType, P1, rowBlock(aRow).defaultBlockState());
	}

	/** A fresh headless sync manager (no player → the panel builds without the inventory bind). */
	private static PanelSyncManager headlessSyncManager() {
		return new PanelSyncManager(new ModularSyncManager(false), true);
	}

	/** Every widget in the panel tree, in child order. */
	private static List<IWidget> allWidgets(ModularPanel<?> aPanel) {
		List<IWidget> tAll = new ArrayList<>();
		collectAll(aPanel, tAll);
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

	private static ItemSlot seat(ModularPanel<?> aPanel, int aSlot) {
		return (ItemSlot) named(aPanel, "slot_" + aSlot);
	}

	/** The widget build position — pos() lands in the resizer's start Unit (the headless face the batterybox test pins). */
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
		assertEquals(aX, posOf(tWidget, true), aWhat + " x");
		assertEquals(aY, posOf(tWidget, false), aWhat + " y");
	}

	/** The full coordinate array of a live panel's machine seats, in slot order. */
	private static int[][] liveSeats(ModularPanel<?> aPanel, int aSlots) throws Exception {
		int[][] rSeats = new int[aSlots][];
		for (int i = 0; i < aSlots; i++) {
			rSeats[i] = new int[] {posOf(seat(aPanel, i), true), posOf(seat(aPanel, i), false)};
		}
		return rSeats;
	}

	// ---------------------------------------------------------------------------
	// the registered rows' live panels (Loader :191/:202 — case 3 and case 5)
	// ---------------------------------------------------------------------------

	@Test
	public void theBronzeHopperPanelIsTheCase3Row() throws Exception {
		GT6HopperBaseBlockEntity tHopper = beOf(row("hopper_bronze"));
		assertEquals(3, tHopper.getInventory().getSlots(), "the Bronze row = aHopperSize 3 (Loader :191)");
		ModularPanel<?> tPanel = GT6HopperMUI.panel(tHopper, headlessSyncManager());
		assertEquals(GT6HopperMUI.PANEL_NAME, tPanel.getName(), "the family panel name");
		long tSeats = allWidgets(tPanel).stream().filter(w -> w instanceof ItemSlot).count();
		assertEquals(3 + 36, tSeats, "the case-3 row + the 36 player seats (issue #3)");

		// the case-3 row verbatim (ContainerCommon.addSlots :81-83 — 62/80/98 x 35)
		assertPos(tPanel, "slot_0", 62, 35, "the case-3 head");
		assertPos(tPanel, "slot_1", 80, 35, "the case-3 middle");
		assertPos(tPanel, "slot_2", 98, 35, "the case-3 tail");

		// the seat order binds the handler indices ascending
		assertEquals(0, seat(tPanel, 0).getSlot().getSlotIndex(), "seat 0 binds index 0");
		assertEquals(2, seat(tPanel, 2).getSlot().getSlotIndex(), "seat 2 binds index 2");

		// the plain chest seat (the upstream Slot_Normal): no phantom filter
		ModularSlot tSeat = seat(tPanel, 0).getSlot();
		assertTrue(tSeat.mayPlace(new ItemStack(Blocks.STONE)), "a hopper seat accepts any stack (no filter face upstream)");
	}

	@Test
	public void theSteelHopperPanelIsTheCase5Row() throws Exception {
		GT6HopperBaseBlockEntity tHopper = beOf(row("hopper_steel"));
		assertEquals(5, tHopper.getInventory().getSlots(), "the Steel row = aHopperSize 5 (Loader :202)");
		ModularPanel<?> tPanel = GT6HopperMUI.panel(tHopper, headlessSyncManager());
		long tSeats = allWidgets(tPanel).stream().filter(w -> w instanceof ItemSlot).count();
		assertEquals(5 + 36, tSeats, "the case-5 row + the 36 player seats (issue #3)");

		// the case-5 row verbatim (ContainerCommon.addSlots :92-96 — 44..116 x 35)
		assertPos(tPanel, "slot_0", 44, 35, "the case-5 head");
		assertPos(tPanel, "slot_1", 62, 35, "the case-5 second");
		assertPos(tPanel, "slot_2", 80, 35, "the case-5 middle");
		assertPos(tPanel, "slot_3", 98, 35, "the case-5 fourth");
		assertPos(tPanel, "slot_4", 116, 35, "the case-5 tail");
	}

	@Test
	public void theQueueHopperPanelsMatchTheSameCaseTables() throws Exception {
		// the queue kind = max(2, aHopperSize) over the SAME ContainerCommonDefault case
		// table (upstream :281-282 = the identical re-declaration)
		GT6HopperBaseBlockEntity tQueueBronze = beOf(row("queue_hopper_bronze"));
		GT6HopperBaseBlockEntity tQueueSteel = beOf(row("queue_hopper_steel"));
		assertEquals(3, tQueueBronze.getInventory().getSlots(), "the Bronze queue row = max(2, 3)");
		assertEquals(5, tQueueSteel.getInventory().getSlots(), "the Steel queue row = max(2, 5)");
		assertEquals(3 + 36, allWidgets(GT6HopperMUI.panel(tQueueBronze, headlessSyncManager())).stream()
				.filter(w -> w instanceof ItemSlot).count(), "the queue case-3 seat count");
		ModularPanel<?> tPanel = GT6HopperMUI.panel(tQueueSteel, headlessSyncManager());
		int[][] tSeats = liveSeats(tPanel, 5);
		int[][] tExpected = {{44, 35}, {62, 35}, {80, 35}, {98, 35}, {116, 35}};
		for (int i = 0; i < 5; i++) {
			assertEquals(tExpected[i][0], tSeats[i][0], "the queue case-5 seat " + i + " x");
			assertEquals(tExpected[i][1], tSeats[i][1], "the queue case-5 seat " + i + " y");
		}
	}

	// ---------------------------------------------------------------------------
	// the player inventory widget — UNCONDITIONAL since issue #3
	// ---------------------------------------------------------------------------

	@Test
	public void thePlayerInventoryIsAlwaysAtTheMachineOffset() throws Exception {
		for (GT6Hoppers.HopperRow tRow : GT6Hoppers.ROWS) {
			ModularPanel<?> tPanel = GT6HopperMUI.panel(beOf(tRow), headlessSyncManager());
			assertNotNull(named(tPanel, "player_inventory"), "the player-inventory group rides the " + tRow.path() + " tree (issue #3)");
			assertPos(tPanel, "player_inventory", 7, 84, "the " + tRow.path() + " player inventory");
		}
	}

	// ---------------------------------------------------------------------------
	// the full case-table transcription pin (ContainerCommon.addSlots :73-288)
	// ---------------------------------------------------------------------------

	@Test
	public void theCaseTableIsTheUpstreamVerbatimTranscription() {
		// every case the material ladder reaches, with the exact lengths and the
		// shift-transfer row widths (the seat-grid columns of each case geometry)
		int[] tCases = {1, 2, 3, 4, 5, 6, 7, 8, 9, 12, 14, 15, 16, 18, 27, 36};
		int[] tRowWidths = {1, 2, 3, 2, 5, 3, 7, 4, 3, 6, 7, 5, 4, 9, 9, 9};
		for (int i = 0; i < tCases.length; i++) {
			assertEquals(tCases[i], GT6HopperMUI.slotPositions(tCases[i]).length, "case " + tCases[i] + " seat count");
			assertEquals(tRowWidths[i], GT6HopperMUI.rowSize(tCases[i]), "case " + tCases[i] + " row width");
		}
		// the heads/tails of every irregular case (ContainerCommon.addSlots line anchors)
		assertEquals(80, GT6HopperMUI.slotPositions(1)[0][0], "case 1 head x (:74)");
		assertEquals(35, GT6HopperMUI.slotPositions(1)[0][1], "case 1 head y (:74)");
		assertEquals(89, GT6HopperMUI.slotPositions(2)[1][0], "case 2 tail x (:78)");
		assertEquals(98, GT6HopperMUI.slotPositions(3)[2][0], "case 3 tail x (:83)");
		assertEquals(26, GT6HopperMUI.slotPositions(4)[0][1], "case 4 head y (:86)");
		assertEquals(44, GT6HopperMUI.slotPositions(4)[3][1], "case 4 tail y (:89)");
		assertEquals(116, GT6HopperMUI.slotPositions(5)[4][0], "case 5 tail x (:96)");
		assertEquals(98, GT6HopperMUI.slotPositions(6)[5][0], "case 6 tail x (:104)");
		assertEquals(134, GT6HopperMUI.slotPositions(7)[6][0], "case 7 tail x (:113)");
		assertEquals(107, GT6HopperMUI.slotPositions(8)[7][0], "case 8 tail x (:123)");
		assertEquals(98, GT6HopperMUI.slotPositions(9)[8][0], "case 9 tail x (:134)");
		assertEquals(53, GT6HopperMUI.slotPositions(9)[8][1], "case 9 tail y (:134)");
		assertEquals(125, GT6HopperMUI.slotPositions(12)[11][0], "case 12 tail x (:148)");
		assertEquals(134, GT6HopperMUI.slotPositions(14)[13][0], "case 14 tail x (:164)");
		assertEquals(116, GT6HopperMUI.slotPositions(15)[14][0], "case 15 tail x (:181)");
		assertEquals(107, GT6HopperMUI.slotPositions(16)[15][0], "case 16 tail x (:199)");
		assertEquals(62, GT6HopperMUI.slotPositions(16)[15][1], "case 16 tail y (:199)");
		// the 9-column chest rows — the "柜式 27" shape the card names (case 27 = the
		// TungstenSteel ladder rows; case 36 = the top ladder)
		for (int i = 0; i < 27; i++) {
			assertEquals(8 + 18 * (i % 9), GT6HopperMUI.slotPositions(27)[i][0], "case 27 seat " + i + " x (:222-248)");
			assertEquals(17 + 18 * (i / 9), GT6HopperMUI.slotPositions(27)[i][1], "case 27 seat " + i + " y (:222-248)");
		}
		for (int i = 0; i < 36; i++) {
			assertEquals(8 + 18 * (i % 9), GT6HopperMUI.slotPositions(36)[i][0], "case 36 seat " + i + " x (:251-286)");
			assertEquals(8 + 18 * (i / 9), GT6HopperMUI.slotPositions(36)[i][1], "case 36 seat " + i + " y (:251-286)");
		}
		// fail-loud outside the table (the upstream switch falls through to NO seats;
		// the port refuses instead — the batterybox fail-loud precedent)
		assertThrows(IllegalArgumentException.class, () -> GT6HopperMUI.slotPositions(13));
		assertThrows(IllegalArgumentException.class, () -> GT6HopperMUI.rowSize(0));
	}

	// ---------------------------------------------------------------------------
	// the family reach — ONE face over both kinds and every registered row
	// ---------------------------------------------------------------------------

	@Test
	public void theFourRegisteredRowsReachTheOneGuiFace() {
		assertEquals(120, GT6Hoppers.ROWS.size(), "the hopper-matrix ladder (60 materials x hopper/queue — the registration surface rides hopper-matrix, this card adds none)");
		// ONE BE base carries the buildUI implementation — the "one face repairs both kinds" bound
		assertTrue(GT6MuiMachine.class.isAssignableFrom(GT6HopperBaseBlockEntity.class),
				"the family BE base implements GT6MuiMachine (the tryOpen bound, zero new MenuType)");
		assertTrue(GT6HopperBaseBlockEntity.class.isAssignableFrom(GT6HopperBlockEntity.class),
				"the regular hopper inherits the buildUI panel");
		assertTrue(GT6HopperBaseBlockEntity.class.isAssignableFrom(GT6QueueHopperBlockEntity.class),
				"the queue hopper inherits the buildUI panel");
		// the family panel factory reaches every registered row
		for (GT6Hoppers.HopperRow tRow : GT6Hoppers.ROWS) {
			ModularPanel<?> tPanel = GT6HopperMUI.panel(beOf(tRow), headlessSyncManager());
			assertEquals(GT6HopperMUI.PANEL_NAME, tPanel.getName(), "row " + tRow.path() + " opens the family panel");
		}
	}
}
