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
import brachy.modularui.widgets.FluidDisplayWidget;
import brachy.modularui.widgets.ProgressWidget;
import brachy.modularui.widgets.slot.ItemSlot;
import brachy.modularui.widgets.slot.ModularSlot;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.registry.GT6Distillation.TileEntityDistillationTower;
import gregtech6.tileentity.multiblocks.GTMultiBlocksOfflineTestBase;

/**
 * The distillation-tower offline panel gate (task p33-gui-distill-tower, the
 * GT6BasicMachineMUIPanelTest shape): {@link GTDistillationTowerMUI} builds the tower
 * panel from a local tower fixture (row()==null — the panel factory must not depend on
 * row()) with no player, no screen.
 *
 * <p>Pinned faces (the research.p32-r-distill-gui mapping table):
 * <ul>
 * <li><b>the seat topology</b> — 1 input (53,7) + 3 outputs (107/125/143,7) canPut(false)
 *     + 10 fluid seats (input (53,63); the nine-bank 3x3 bottom-up at (107..143,
 *     63/45/27)) + 1 progress bar at (78,24) 20x18;</li>
 * <li><b>the output-only face</b> — every output seat refuses insertion while the input
 *     accepts;</li>
 * <li><b>the read-only fluid face</b> — no amount text (the upstream icon-only seat);</li>
 * <li><b>the tower background</b> — the borrowed distillationtower.png path.</li>
 * </ul>
 */
class GT6DistillationTowerMUIPanelTest extends GTMultiBlocksOfflineTestBase {

	private static final BlockPos P1 = new BlockPos(120, 64, 100);

	/** The panel-fixture BET (the GT6DistillationTowerTest selfHolder form — the 21.1 BE ctor validates the state against the type). */
	static BlockEntityType<TileEntityDistillationTower> sPanelTowerType;

	@BeforeAll
	static void buildPanelTowerFixtureBet() {
		@SuppressWarnings("unchecked")
		BlockEntityType<TileEntityDistillationTower>[] tHolder =
				(BlockEntityType<TileEntityDistillationTower>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new TileEntityDistillationTower(tHolder[0], aPos, aState),
				Blocks.BRICKS).build(null);
		sPanelTowerType = tHolder[0];
	}

	/**
	 * The offline FML dist shaping (the GT6BasicMachineMUIPanelTest form — the vendored MUI
	 * widget classes read FMLEnvironment.dist during their static init; SERVER selects the
	 * headless text branch). Must run BEFORE the first widget class initializes.
	 */
	@BeforeAll
	static void armFmlDistOffline() throws Exception {
		Class<?> tFmlEnv = Class.forName("net.minecraftforge.fml.loading.FMLEnvironment", false,
				GT6DistillationTowerMUIPanelTest.class.getClassLoader());
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

	/** The row-less tower fixture (row()==null — the risk line the panel factory must survive), fed a 1-in/3-out/1-fluid fake map so the Host projection (getOutputSlotCount/getGuiTexture) resolves. */
	private static TileEntityDistillationTower newTower() {
		TileEntityDistillationTower tTower =
				new TileEntityDistillationTower(sPanelTowerType, P1, Blocks.BRICKS.defaultBlockState());
		// the tower-map shape (RM.java:65/:66 items 1/3/0 fluid 1/9/0) — the same fake-map
		// form GT6DistillationTowerTest rides (its own rows are empty; the panel needs only
		// the slot-shape numbers and a namespaced GUI path)
		gregtech6.recipes.RecipeMap tMap = new gregtech6.recipes.RecipeMap(new java.util.HashSet<>(),
				"gt6.test.towerpanel." + System.nanoTime(), "Tower Panel Test", null, 0, 1,
				"gt6:textures/gui/machines/distillationtower", 1, 3, 1, 1, 9, 0, 1, 1);
		tTower.mRecipes = tMap;
		return tTower;
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

	/** The widget build position — pos() lands in the resizer's start Unit (the Area only resolves at layout, which needs an initialized ResizeNode tree the headless build lacks); read the stored pixel value directly. */
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
		assertPos(named(aPanel, aName), aX, aY, aWhat);
	}

	/** The widget build size — size() rides the resizer's size Unit (same headless face as pos()); read the stored pixel value directly. */
	private static int sizeOf(IWidget aWidget, boolean aWidth) throws Exception {
		brachy.modularui.api.widget.IPositioned tPositioned = (brachy.modularui.api.widget.IPositioned) aWidget;
		java.lang.reflect.Field tAxis = tPositioned.resizer().getClass().getDeclaredField(aWidth ? "x" : "y");
		tAxis.setAccessible(true);
		Object tSizer = tAxis.get(tPositioned.resizer());
		java.lang.reflect.Field tSize = tSizer.getClass().getDeclaredField("size");
		tSize.setAccessible(true);
		Object tUnit = tSize.get(tSizer);
		return (int) ((brachy.modularui.widget.sizer.Unit) tUnit).getValue();
	}

	private static void assertPos(IWidget aWidget, int aX, int aY, String aWhat) throws Exception {
		assertEquals(aX, posOf(aWidget, true), aWhat + " x");
		assertEquals(aY, posOf(aWidget, false), aWhat + " y");
	}

	// ---------------------------------------------------------------------------
	// the seat topology — 4 item seats + 10 fluid seats + 1 progress bar
	// ---------------------------------------------------------------------------

	@Test
	public void thePanelCarriesTheUpstreamSeatTopology() throws Exception {
		TileEntityDistillationTower tTower = newTower();
		PanelSyncManager tSync = headlessSyncManager();
		ModularPanel<?> tPanel = GTDistillationTowerMUI.buildPanel(tTower, tSync);

		List<IWidget> tAll = allWidgets(tPanel);
		long tItemSeats = tAll.stream().filter(w -> w instanceof ItemSlot).count();
		long tFluidSeats = tAll.stream().filter(w -> w instanceof FluidDisplayWidget).count();
		long tProgress = tAll.stream().filter(w -> w instanceof ProgressWidget).count();
		assertEquals(4, tItemSeats, "1 input + 3 outputs (the case1/:55 + case3/:168 rows)");
		assertEquals(10, tFluidSeats, "the input tank + the NINE-tank output bank (:267/:268)");
		assertEquals(1, tProgress, "exactly the progress bar");

		// the geometry — the mapping table verbatim
		assertPos(tPanel, "input_0", 53, 7, "input");
		for (int i = 0; i < 3; i++) {
			assertPos(tPanel, "output_" + i, 107 + 18 * i, 7, "output " + i);
		}
		assertPos(tPanel, "fluid_in", 53, 63, "fluid-in (:267)");
		for (int i = 0; i < TileEntityDistillationTower.OUTPUT_TANK_COUNT; i++) {
			assertPos(tPanel, "fluid_out_" + i, 107 + 18 * (i % 3), 63 - 18 * (i / 3), "bank tank " + i);
		}
		// the progress arrow — (78,24) 20x18, ContainerClientBasicMachine.java:57
		brachy.modularui.widget.Widget<?> tBar = (brachy.modularui.widget.Widget<?>) tAll.stream()
				.filter(w -> w instanceof ProgressWidget).findFirst().orElseThrow();
		assertPos(tBar, 78, 24, "progress arrow");
		assertEquals(20, sizeOf(tBar, true), "progress arrow w 20");
		assertEquals(18, sizeOf(tBar, false), "progress arrow h 18");
	}

	// ---------------------------------------------------------------------------
	// the output-only face + the slot bindings
	// ---------------------------------------------------------------------------

	@Test
	public void theOutputsRefuseInsertionAndBindTheirIndices() {
		TileEntityDistillationTower tTower = newTower();
		PanelSyncManager tSync = headlessSyncManager();
		ModularPanel<?> tPanel = GTDistillationTowerMUI.buildPanel(tTower, tSync);

		ModularSlot tInput = ((ItemSlot) named(tPanel, "input_0")).getSlot();
		assertEquals(0, tInput.getSlotIndex(), "the input binds SLOT_INPUT 0");
		assertTrue(tInput.mayPlace(new ItemStack(Blocks.BRICKS.asItem())), "the input accepts");

		for (int i = 0; i < 3; i++) {
			ModularSlot tOutput = ((ItemSlot) named(tPanel, "output_" + i)).getSlot();
			assertEquals(1 + i, tOutput.getSlotIndex(), "the output binds the slot after the input");
			assertFalse(tOutput.mayPlace(new ItemStack(Blocks.BRICKS.asItem())),
					"output " + i + " refuses insertion (the upstream setCanPut(F))");
		}
	}

	// ---------------------------------------------------------------------------
	// the read-only fluid face + the background
	// ---------------------------------------------------------------------------

	@Test
	public void theFluidSeatsAreReadOnlyDisplayAndTheBackgroundIsTheTowerPair() {
		TileEntityDistillationTower tTower = newTower();
		PanelSyncManager tSync = headlessSyncManager();
		ModularPanel<?> tPanel = GTDistillationTowerMUI.buildPanel(tTower, tSync);

		// the amount text is off (the upstream icon-only seat — the declared fidelity line)
		for (IWidget tWidget : allWidgets(tPanel)) {
			if (tWidget instanceof FluidDisplayWidget tSeat) {
				assertFalse(tSeat.isDisplayAmount(), "the fluid seat draws no amount text");
			}
		}

		// the background rides the tower's mGUIPath (the borrowed PNG pair)
		UITexture tBackground = assertInstanceOf(UITexture.class, tPanel.getBackground(), "the panel background is a texture");
		assertEquals(new ResourceLocation("gt6", "textures/gui/machines/distillationtower.png"), tBackground.location(),
				"the tower background path (the row-less fixture defaults the HU tower name)");
	}

	// ---------------------------------------------------------------------------
	// the use arm — the MUI open chain dispatch key (the offline half)
	// ---------------------------------------------------------------------------

	@Test
	public void theTowerBlockImplementsTheMuiOpenChain() {
		// the BE contract: TileEntityDistillationTower implements GT6MuiMachine, so
		// tryOpen's intersection bound (BlockEntity & GT6MuiMachine) accepts it — the
		// compile-time face the use arm dispatches through
		assertTrue(GT6MuiMachine.class.isAssignableFrom(TileEntityDistillationTower.class),
				"the tower BE implements GT6MuiMachine (the tryOpen bound)");
		// the panel factory consumes the tower BE directly (the pinned buildPanel signature)
		assertTrue(gregtech6.registry.GT6Distillation.TOWER_BLOCKS_BY_PATH.containsKey("distillation_tower"),
				"the tower block registration intact (the use arm rides the same block class)");
	}
}
