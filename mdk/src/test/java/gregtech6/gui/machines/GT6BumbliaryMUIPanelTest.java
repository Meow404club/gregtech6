package gregtech6.gui.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import brachy.modularui.api.widget.IWidget;
import brachy.modularui.drawable.UITexture;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.value.sync.ModularSyncManager;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.SlotGroupWidget;
import brachy.modularui.widgets.slot.ItemSlot;
import brachy.modularui.widgets.slot.ModularSlot;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.tileentity.bees.GT6BumbliaryBlockEntity;
import gregtech6.tileentity.multiblocks.GTMultiBlocksOfflineTestBase;

/**
 * The Bumbliary offline panel gate (task p34-bumbliary-gui acceptance ①, the
 * GT6DistillationTowerMUIPanelTest shape): the dual-variant slot geometry and the
 * per-slot interaction flags over the mAdvanced/scoop pair, exactly the upstream
 * :396-434 (primary normal), :450-488 (primary scoop) and the Advanced :397-419/:435-457
 * tables.
 *
 * <p>The upstream slot-flag vocabulary folds: {@code setCanPut(F)} = take-only (the comb
 * and dead seats), {@code setCanPut(F).setCanTake(F)} = the inert satellite drone ring,
 * the bare {@code setCanTake(F)} on the ROYAL/DRONE seats = insert-only, and the scoop
 * pair drops every flag on those two seats (fully interactive).
 */
class GT6BumbliaryMUIPanelTest extends GTMultiBlocksOfflineTestBase {

	private static final BlockPos P1 = new BlockPos(50, 64, 50);

	/** The panel-fixture BET (the selfHolder form — the 21.1 BE ctor validates the state against the type). */
	static BlockEntityType<GT6BumbliaryBlockEntity> sPanelBumbliaryType;

	@BeforeAll
	static void buildPanelBumbliaryFixtureBet() {
		@SuppressWarnings("unchecked")
		BlockEntityType<GT6BumbliaryBlockEntity>[] tHolder =
				(BlockEntityType<GT6BumbliaryBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6BumbliaryBlockEntity(false, tHolder[0], aPos, aState),
				Blocks.BRICKS).build(null);
		sPanelBumbliaryType = tHolder[0];
	}

	/**
	 * The offline FML dist shaping (the GT6DistillationTowerMUIPanelTest form — the vendored
	 * MUI widget classes read FMLEnvironment.dist during their static init). Must run BEFORE
	 * the first widget class initializes.
	 */
	@BeforeAll
	static void armFmlDistOffline() throws Exception {
		Class<?> tFmlEnv = Class.forName("net.minecraftforge.fml.loading.FMLEnvironment", false,
				GT6BumbliaryMUIPanelTest.class.getClassLoader());
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

	private static GT6BumbliaryBlockEntity bumbliary(boolean aAdvanced) {
		return new GT6BumbliaryBlockEntity(aAdvanced, sPanelBumbliaryType, P1, Blocks.BRICKS.defaultBlockState());
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
		return (ItemSlot)named(aPanel, "slot_" + aSlot);
	}

	/** The widget build position — pos() lands in the resizer's start Unit (the headless face the distill test pins). */
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

	// ---------------------------------------------------------------------------
	// the primary normal panel — the :396-434 table
	// ---------------------------------------------------------------------------

	@Test
	public void thePrimaryNormalPanelCarriesThe36SeatTable() throws Exception {
		GT6BumbliaryBlockEntity tBumbliary = bumbliary(false);
		ModularPanel<?> tPanel = GT6BumbliaryMUI.buildPanel(tBumbliary, headlessSyncManager(), false);

		long tSeats = allWidgets(tPanel).stream().filter(w -> w instanceof ItemSlot).count();
		assertEquals(36, tSeats, "the primary 4x9 grid");
		assertEquals(GT6BumbliaryMUI.PANEL_NAME, tPanel.getName(), "the normal panel name");

		// the geometry — the grid steps from (8,8) by 18
		assertPos(tPanel, "slot_0", 8, 8, "the first comb seat");
		assertPos(tPanel, "slot_8", 152, 8, "row 0 tail");
		assertPos(tPanel, "slot_9", 8, 26, "row 1 head");
		assertPos(tPanel, "slot_22", 80, 44, "the dedicated DRONE seat");
		assertPos(tPanel, "slot_35", 152, 62, "the last dead seat");

		// the flags — the :396-434 interaction vocabulary
		ModularSlot tComb = seat(tPanel, 0).getSlot();
		assertEquals(0, tComb.getSlotIndex(), "the comb seat binds its index");
		assertFalse(tComb.mayPlace(new ItemStack(Blocks.BRICKS)), "the comb seat refuses inserts (setCanPut(F))");
		assertTrue(tComb.isCanTake(), "the comb seat takes (the product face)");
		ModularSlot tSatellite = seat(tPanel, 3).getSlot();
		assertFalse(tSatellite.mayPlace(new ItemStack(Blocks.BRICKS)), "the satellite drone seat is inert on insert");
		assertFalse(tSatellite.isCanTake(), "the satellite drone seat is inert on take (:399)");
		ModularSlot tRoyal = seat(tPanel, 13).getSlot();
		assertTrue(tRoyal.isCanPut(), "the ROYAL seat takes inserts (:410 bare setCanTake(F))");
		assertFalse(tRoyal.isCanTake(), "the ROYAL seat never releases the queen");
		ModularSlot tDrone = seat(tPanel, 22).getSlot();
		assertTrue(tDrone.isCanPut(), "the DRONE seat takes inserts (:420)");
		assertFalse(tDrone.isCanTake(), "the DRONE seat holds the pairing drone");
		ModularSlot tDead = seat(tPanel, 27).getSlot();
		assertFalse(tDead.mayPlace(new ItemStack(Blocks.BRICKS)), "the dead seat refuses inserts");
		assertTrue(tDead.isCanTake(), "the dead seat takes (the :426 row)");
	}

	// ---------------------------------------------------------------------------
	// the primary scoop panel — the :450-488 table
	// ---------------------------------------------------------------------------

	@Test
	public void thePrimaryScoopPanelOpensTheRoyalAndDroneSeats() {
		GT6BumbliaryBlockEntity tBumbliary = bumbliary(false);
		ModularPanel<?> tPanel = GT6BumbliaryMUI.buildPanel(tBumbliary, headlessSyncManager(), true);

		assertEquals(GT6BumbliaryMUI.PANEL_NAME_SCOOP, tPanel.getName(), "the scoop panel name");
		ModularSlot tRoyal = seat(tPanel, 13).getSlot();
		assertTrue(tRoyal.isCanPut(), "the scoop ROYAL seat is fully open (:464 — no flags)");
		assertTrue(tRoyal.isCanTake(), "the scoop ROYAL seat releases the queen");
		ModularSlot tDrone = seat(tPanel, 22).getSlot();
		assertTrue(tDrone.isCanPut(), "the scoop DRONE seat is fully open (:474)");
		assertTrue(tDrone.isCanTake(), "the scoop DRONE seat takes");
		ModularSlot tComb = seat(tPanel, 0).getSlot();
		assertFalse(tComb.mayPlace(new ItemStack(Blocks.BRICKS)), "the scoop comb seat stays take-only (:450)");
		assertTrue(tComb.isCanTake(), "the scoop comb seat takes");
		ModularSlot tSatellite = seat(tPanel, 3).getSlot();
		assertFalse(tSatellite.mayPlace(new ItemStack(Blocks.BRICKS)), "the scoop satellite stays inert on insert (:453)");
		assertTrue(tSatellite.isCanTake(), "the scoop satellite takes (:453 has only setCanPut(F))");
	}

	// ---------------------------------------------------------------------------
	// the advanced panel — the :397-419/:435-457 pair
	// ---------------------------------------------------------------------------

	@Test
	public void theAdvancedPanelIsThe20SeatFiveWideGrid() throws Exception {
		GT6BumbliaryBlockEntity tBumbliary = bumbliary(true);
		ModularPanel<?> tPanel = GT6BumbliaryMUI.buildPanel(tBumbliary, headlessSyncManager(), false);

		long tSeats = allWidgets(tPanel).stream().filter(w -> w instanceof ItemSlot).count();
		assertEquals(20, tSeats, "the advanced 4x5 grid");
		assertPos(tPanel, "slot_0", 44, 8, "the advanced grid steps from x=44 (:397)");
		assertPos(tPanel, "slot_4", 116, 8, "the advanced row tail");
		assertPos(tPanel, "slot_7", 80, 26, "the advanced ROYAL seat (:405)");
		assertPos(tPanel, "slot_12", 80, 44, "the advanced DRONE seat (:411)");
		assertPos(tPanel, "slot_19", 116, 62, "the advanced dead tail (:419)");

		ModularSlot tRoyal = seat(tPanel, 7).getSlot();
		assertTrue(tRoyal.isCanPut(), "the advanced ROYAL takes inserts");
		assertFalse(tRoyal.isCanTake(), "the advanced ROYAL never releases");
		ModularSlot tDrone = seat(tPanel, 12).getSlot();
		assertFalse(tDrone.isCanTake(), "the advanced DRONE holds (:412)");

		ModularPanel<?> tScoop = GT6BumbliaryMUI.buildPanel(tBumbliary, headlessSyncManager(), true);
		assertTrue(seat(tScoop, 7).getSlot().isCanTake(), "the advanced scoop ROYAL opens (:443)");
		assertTrue(seat(tScoop, 12).getSlot().isCanTake(), "the advanced scoop DRONE opens (:449)");
	}

	// ---------------------------------------------------------------------------
	// the backgrounds, the player inventory and the factory dispatch
	// ---------------------------------------------------------------------------

	@Test
	public void theBackgroundsFollowTheVariantAndTheFactoriesCarryTheVariant() {
		GT6BumbliaryBlockEntity tPrimary = bumbliary(false);
		GT6BumbliaryBlockEntity tAdvanced = bumbliary(true);

		ModularPanel<?> tNormal = GT6BumbliaryMUI.buildPanel(tPrimary, headlessSyncManager(), false);
		UITexture tNormalBackground = assertInstanceOf(UITexture.class, tNormal.getBackground(), "the normal background is a texture");
		assertEquals(ResourceLocation.fromNamespaceAndPath("gt6", "textures/gui/machines/bumbliary.png"), tNormalBackground.location(),
				"the :500/:507 primary pair background");

		ModularPanel<?> tAdv = GT6BumbliaryMUI.buildPanel(tAdvanced, headlessSyncManager(), false);
		UITexture tAdvBackground = assertInstanceOf(UITexture.class, tAdv.getBackground(), "the advanced background is a texture");
		assertEquals(ResourceLocation.fromNamespaceAndPath("gt6", "textures/gui/machines/bumbliaryadvanced.png"), tAdvBackground.location(),
				"the Advanced :469 background");

		// the factory dispatch — the identity IS the variant (the acceptance ① mAdvanced/scoop arms)
		assertFalse(GT6BumbliaryMUI.Factory.NORMAL.scoop(), "the normal factory");
		assertTrue(GT6BumbliaryMUI.Factory.SCOOP.scoop(), "the scoop factory");
		assertEquals(ResourceLocation.fromNamespaceAndPath("gt6", "bumbliary"), GT6BumbliaryMUI.Factory.NORMAL.getFactoryName(), "the wire identity (normal)");
		assertEquals(ResourceLocation.fromNamespaceAndPath("gt6", "bumbliary_scoop"), GT6BumbliaryMUI.Factory.SCOOP.getFactoryName(), "the wire identity (scoop)");

		// the BE implements the GT6MuiMachine chain (the tryOpen bound — the normal open path)
		assertTrue(GT6MuiMachine.class.isAssignableFrom(GT6BumbliaryBlockEntity.class),
				"the bumbliary BE implements GT6MuiMachine (the normal-panel tryOpen bound)");
	}

	// ---------------------------------------------------------------------------
	// the player inventory bind — the runtime half (headless skips it)
	// ---------------------------------------------------------------------------

	@Test
	public void theHeadlessBuildSkipsThePlayerInventoryBind() {
		GT6BumbliaryBlockEntity tBumbliary = bumbliary(false);
		ModularPanel<?> tPanel = GT6BumbliaryMUI.buildPanel(tBumbliary, headlessSyncManager(), false);
		long tPlayerSeats = allWidgets(tPanel).stream().filter(w -> w instanceof SlotGroupWidget).count();
		assertEquals(0, tPlayerSeats, "no player-inventory group builds headless (the mui-a getPlayer gate)");
	}
}
