package gregtech6.gui.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.block.energy.GT6BatteryBoxBlock;
import gregtech6.block.energy.GT6ZpmDechargerBlock;
import gregtech6.registry.GT6Batteries;
import gregtech6.tileentity.energy.GT6BatteryBoxBlockEntity;
import gregtech6.tileentity.energy.GT6ZpmDechargerBlockEntity;
import gregtech6.tileentity.multiblocks.GTMultiBlocksOfflineTestBase;

/**
 * The BatteryBox-family offline panel gate (task batterybox-gui, the
 * GT6BumbliaryMUIPanelTest shape): the three upstream
 * {@code ContainerCommon.addSlots} case-table geometries the family's slot counts
 * reach (4 = the small Battery Box / Crystal Charger rows, 16 = the Large rows,
 * 1 = the ZPM Decharger rows) plus the family-reach pins — ONE BE implementation
 * GUIs all 12 BatteryBoxes (and the 20 chargers + 2 dechargers riding the same BE).
 */
class GT6BatteryBoxMUIPanelTest extends GTMultiBlocksOfflineTestBase {

	private static final BlockPos P1 = new BlockPos(50, 64, 50);

	/** The panel-fixture BET (the selfHolder form — the 21.1 BE ctor validates the state against the type). */
	static BlockEntityType<GT6BatteryBoxBlockEntity> sPanelBoxType;

	@BeforeAll
	static void buildPanelBoxFixtureBet() {
		@SuppressWarnings("unchecked")
		BlockEntityType<GT6BatteryBoxBlockEntity>[] tHolder =
				(BlockEntityType<GT6BatteryBoxBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6BatteryBoxBlockEntity(tHolder[0], aPos, aState),
				Blocks.STONE).build(null);
		sPanelBoxType = tHolder[0];
	}

	/**
	 * The offline FML dist shaping (the Bumbliary form — the vendored MUI widget classes
	 * read FMLEnvironment.dist during their static init). Must run BEFORE the first
	 * widget class initializes.
	 */
	@BeforeAll
	static void armFmlDistOffline() throws Exception {
		Class<?> tFmlEnv = Class.forName("net.minecraftforge.fml.loading.FMLEnvironment", false,
				GT6BatteryBoxMUIPanelTest.class.getClassLoader());
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

	/** The offline fixture BE — the explicit-row ctor carries the slot count (STONE states have no GT6 block). */
	private static GT6BatteryBoxBlockEntity box(int aTier, int aSlots) {
		return new GT6BatteryBoxBlockEntity(sPanelBoxType, P1, Blocks.STONE.defaultBlockState(), aTier, aSlots);
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

	/** The widget build position — pos() lands in the resizer's start Unit (the headless face the Bumbliary test pins). */
	private static int posOf(IWidget aWidget, boolean aX) throws Exception {
		brachy.modularui.api.widget.IPositioned<?> tPositioned = (brachy.modularui.api.widget.IPositioned<?>) aWidget;
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

	// ---------------------------------------------------------------------------
	// the case-table geometries (the ContainerCommon.addSlots verbatim coordinates)
	// ---------------------------------------------------------------------------

	@Test
	public void theSmallPanelIsThe2x2Case4Grid() throws Exception {
		ModularPanel<?> tPanel = GT6BatteryBoxMUI.panel(box(0, 4), headlessSyncManager());
		assertEquals(GT6BatteryBoxMUI.PANEL_NAME, tPanel.getName(), "the family panel name");
		long tSeats = allWidgets(tPanel).stream().filter(w -> w instanceof ItemSlot).count();
		assertEquals(4 + 36, tSeats, "the 2x2 battery grid + the 36 player seats (issue #3)");

		// the case-4 rows verbatim (ContainerCommon.addSlots — 71/89 x 26/44)
		assertPos(tPanel, "slot_0", 71, 26, "the case-4 head");
		assertPos(tPanel, "slot_1", 89, 26, "the case-4 row tail");
		assertPos(tPanel, "slot_2", 71, 44, "the case-4 row-2 head");
		assertPos(tPanel, "slot_3", 89, 44, "the case-4 tail");

		// the seat order binds the handler indices row-major
		assertEquals(0, seat(tPanel, 0).getSlot().getSlotIndex(), "seat 0 binds index 0");
		assertEquals(3, seat(tPanel, 3).getSlot().getSlotIndex(), "seat 3 binds index 3");

		// the family gate rides the handler (the canInsertItem2 :199 IItemEnergy-only face)
		ModularSlot tSeat = seat(tPanel, 0).getSlot();
		assertFalse(tSeat.mayPlace(new ItemStack(Blocks.STONE)), "a non-energy stack never enters (the handler isItemValid gate)");
	}

	@Test
	public void theLargePanelIsThe4x4Case16Grid() throws Exception {
		ModularPanel<?> tPanel = GT6BatteryBoxMUI.panel(box(0, 16), headlessSyncManager());
		long tSeats = allWidgets(tPanel).stream().filter(w -> w instanceof ItemSlot).count();
		assertEquals(16 + 36, tSeats, "the 4x4 battery grid + the 36 player seats (issue #3)");

		// the case-16 rows verbatim (ContainerCommon.addSlots — 53/71/89/107 x 8/26/44/62)
		assertPos(tPanel, "slot_0", 53, 8, "the case-16 head");
		assertPos(tPanel, "slot_3", 107, 8, "the case-16 row-0 tail");
		assertPos(tPanel, "slot_4", 53, 26, "the case-16 row-1 head");
		assertPos(tPanel, "slot_15", 107, 62, "the case-16 tail");
	}

	@Test
	public void theSingleSeatPanelIsTheZpmDechargerCase() throws Exception {
		ModularPanel<?> tPanel = GT6BatteryBoxMUI.panel(box(7, 1), headlessSyncManager());
		long tSeats = allWidgets(tPanel).stream().filter(w -> w instanceof ItemSlot).count();
		assertEquals(1 + 36, tSeats, "the single ZPM seat + the 36 player seats (issue #3)");
		assertPos(tPanel, "slot_0", 80, 35, "the case-1 single seat");
	}

	// ---------------------------------------------------------------------------
	// the player inventory widget — UNCONDITIONAL since issue #3
	// ---------------------------------------------------------------------------

	@Test
	public void thePlayerInventoryIsAlwaysAtTheMachineOffset() throws Exception {
		for (int tSlots : new int[] {1, 4, 16}) {
			ModularPanel<?> tPanel = GT6BatteryBoxMUI.panel(box(0, tSlots), headlessSyncManager());
			assertNotNull(named(tPanel, "player_inventory"), "the player-inventory group rides the " + tSlots + "-slot tree (issue #3)");
			assertPos(tPanel, "player_inventory", 7, 84, "the " + tSlots + "-slot player inventory");
		}
	}

	// ---------------------------------------------------------------------------
	// the r11 clean-base takeover — the theme 9-slice base is the only visual
	// (task r11-gui-storage-clean; the shared helpers live on the storage gate)
	// ---------------------------------------------------------------------------

	@Test
	public void theThemeHandsTheNineSliceBaseToTheFamilyPanel() throws Exception {
		ModularPanel<?> tPanel = GT6BatteryBoxMUI.panel(box(0, 4), headlessSyncManager());
		GT6StorageMUIPanelTest.assertCleanBaseHandsTo(tPanel, "the batterybox family panel");
		assertEquals(176, GT6StorageMUIPanelTest.sizeOf(tPanel, true), "the standard panel width");
		assertEquals(166, GT6StorageMUIPanelTest.sizeOf(tPanel, false), "the standard panel height");
	}

	// ---------------------------------------------------------------------------
	// the family reach — ONE face over the energy-storage crowd
	// ---------------------------------------------------------------------------

	@Test
	public void theTwelveBatteryBoxRowsReachTheOneGuiFace() {
		// the 12 registration rows: 6 small (NBT_INV_SIZE 4) + 6 Large (NBT_INV_SIZE 16)
		assertEquals(12, GT6Batteries.BOX_ROWS.size(), "the tier-closed Battery Box family");
		for (int i = 0; i < 6; i++) {
			assertEquals(4, GT6Batteries.BOX_ROWS.get(i).slots(), "small row " + i + " = 4 slots");
			assertEquals(16, GT6Batteries.BOX_ROWS.get(i + 6).slots(), "large row " + i + " = 16 slots");
		}
		// ONE BE class carries the buildUI implementation — the "one face repairs 12" bound
		assertTrue(GT6MuiMachine.class.isAssignableFrom(GT6BatteryBoxBlockEntity.class),
				"the family BE implements GT6MuiMachine (the tryOpen bound, zero new MenuType)");
		// the family panel factory reaches every registered slot count
		for (GT6Batteries.BoxRow tRow : GT6Batteries.BOX_ROWS) {
			ModularPanel<?> tPanel = GT6BatteryBoxMUI.panel(box(tRow.tier(), tRow.slots()), headlessSyncManager());
			assertEquals(GT6BatteryBoxMUI.PANEL_NAME, tPanel.getName(), "row " + tRow.path() + " opens the family panel");
		}
	}

	@Test
	public void theChargersAndDechargersRideTheSameFace() {
		// the upstream heritage: the Charger/Decharger classes inherit the BatBox GUI
		// (BatteryBox:31 / CrystalCharger:31 / ZPMDechargerEU:35) — the port mirror is
		// the same inheritance over the shared BE and the shared block dispatch
		assertTrue(GT6BatteryBoxBlock.class.isAssignableFrom(GT6ZpmDechargerBlock.class),
				"the ZPM decharger block inherits the two-arm use() dispatch");
		assertTrue(GT6BatteryBoxBlockEntity.class.isAssignableFrom(GT6ZpmDechargerBlockEntity.class),
				"the ZPM decharger BE inherits the buildUI panel");
	}
}
