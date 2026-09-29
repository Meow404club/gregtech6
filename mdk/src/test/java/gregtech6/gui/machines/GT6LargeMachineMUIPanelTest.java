package gregtech6.gui.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
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

import gregtech6.registry.GT6LargeMachines;
import gregtech6.registry.GT6LargeMachines.GTLargeMachineBlock;
import gregtech6.registry.GT6LargeMachines.GTLargeMachineBlockEntity;
import gregtech6.recipes.RecipeMap;
import gregtech6.tileentity.multiblocks.GTMultiBlocksOfflineTestBase;

/**
 * The twelve-large-machine offline panel gate (issue r4-24a, the
 * GT6DistillationTowerMUIPanelTest shape over the SHARED {@link GTBasicMachineMUI}
 * panel): every row's controller BE builds the panel headless (no player, no screen)
 * against its REAL RecipeMap — the BE IS the Host, the row-null fixture seam injects the
 * row directly.
 *
 * <p>Pinned faces (the issue-#24 root-cause trio):
 * <ul>
 * <li><b>the open chain</b> — the BE implements {@link GT6MuiMachine} (the tryOpen
 *     bound) and the controller block carries the use arm under either leg's name;</li>
 * <li><b>the background</b> — recipes().mGUIPath of the ROW's map (the upstream
 *     MultiTileEntityBasicMachine.java:114 chain), not the base's COKE_OVEN fallback;</li>
 * <li><b>the seat topology</b> — the map's mInputItemsCount inputs + the map's
 *     mOutputItemsCount outputs canPut(false) + the 1/1 fluid banks (the :267/:268
 *     Slot_Render face) + 1 progress bar + the 36 player seats.</li>
 * </ul>
 *
 * <p>Plus the map-sized inventory: the Crusher/Shredder 1+12 shape overflows the base
 * INVENTORY_SIZE 11 — the row ctor re-sizes, every ≤11 map keeps the base handler. And the
 * input-truth face (task r8-gui-layout-descriptor): getInputSlotCount returns the row
 * map's live mInputItemsCount (the Host default 1 was the r4-24a leftover), the MIXER/BATH
 * six input seats land on the descriptor's case-6 table.
 */
class GT6LargeMachineMUIPanelTest extends GTMultiBlocksOfflineTestBase {

	private static final BlockPos P1 = new BlockPos(140, 64, 100);

	/** The panel-fixture BET (the GT6LargeMachineSemanticsTest selfHolder form). */
	static BlockEntityType<GTLargeMachineBlockEntity> sPanelType;

	@BeforeAll
	static void buildPanelFixtureBet() {
		// the row ctor resolves the row's real map (the BE inventory sizes from it) — the
		// idempotent bootstrap (GTMachinesOfflineTestBase.java:144 form)
		gregtech6.recipes.GT6RecipeMaps.init();
		@SuppressWarnings("unchecked")
		BlockEntityType<GTLargeMachineBlockEntity>[] tHolder =
				(BlockEntityType<GTLargeMachineBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTLargeMachineBlockEntity(tHolder[0], aPos, aState, null),
				Blocks.BRICKS).build(null);
		sPanelType = tHolder[0];
	}

	/**
	 * The offline FML dist shaping (the GT6DistillationTowerMUIPanelTest form — the vendored
	 * MUI widget classes read FMLEnvironment.dist during their static init; SERVER selects
	 * the headless text branch). Must run BEFORE the first widget class initializes.
	 */
	@BeforeAll
	static void armFmlDistOffline() throws Exception {
		Class<?> tFmlEnv = Class.forName("net.minecraftforge.fml.loading.FMLEnvironment", false,
				GT6LargeMachineMUIPanelTest.class.getClassLoader());
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

	/** The row fixture — the row injected through the test-seam ctor (the config applied live). */
	private static GTLargeMachineBlockEntity newMachine(GT6LargeMachines.LargeMachineRow aRow) {
		return new GTLargeMachineBlockEntity(sPanelType, P1, Blocks.BRICKS.defaultBlockState(), aRow);
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

	// ---------------------------------------------------------------------------
	// the twelve panels — background / seats / bindings / inventory, per row
	// ---------------------------------------------------------------------------

	@Test
	public void everyRowBuildsTheSharedPanelOverItsOwnMap() {
		assertTrue(GT6LargeMachines.ROWS.size() == 12, "the twelve-row family");
		for (GT6LargeMachines.LargeMachineRow tRow : GT6LargeMachines.ROWS) {
			GTLargeMachineBlockEntity tMachine = newMachine(tRow);
			RecipeMap tMap = tRow.recipes().get();
			ModularPanel<?> tPanel = tMachine.buildUI(null, headlessSyncManager(), null);

			// the background = the ROW map's mGUIPath (RecipeMap.java:123 appends .png) —
			// if the BE wiring fell back to the base's COKE_OVEN this diverges
			UITexture tBackground = assertInstanceOf(UITexture.class, tPanel.getBackground(),
					tRow.path() + ": the panel background is a texture");
			String tGuiPath = tMap.mGUIPath;
			int tColon = tGuiPath.indexOf(':');
			assertTrue(tColon > 0, tRow.path() + ": namespaced mGUIPath");
			assertEquals(ResourceLocation.fromNamespaceAndPath(tGuiPath.substring(0, tColon), tGuiPath.substring(tColon + 1)),
					tBackground.location(), tRow.path() + ": the background is the row map's mGUIPath");

			// the seat topology — the map's input truth + the map's outputs + the 36 player seats
			List<IWidget> tAll = allWidgets(tPanel);
			long tItemSeats = tAll.stream().filter(w -> w instanceof ItemSlot).count();
			long tFluidSeats = tAll.stream().filter(w -> w instanceof FluidDisplayWidget).count();
			long tProgress = tAll.stream().filter(w -> w instanceof ProgressWidget).count();
			assertEquals(tMap.mInputItemsCount + tMap.mOutputItemsCount + 36, tItemSeats,
					tRow.path() + ": " + tMap.mInputItemsCount + " inputs + " + tMap.mOutputItemsCount + " outputs + 36 player seats");
			assertEquals(2, tFluidSeats, tRow.path() + ": the 1/1 fluid banks (:267/:268)");
			assertEquals(1, tProgress, tRow.path() + ": exactly the progress bar");
			assertNotNull(tAll.stream().filter(w -> "player_inventory".equals(w.getName())).findFirst().orElse(null),
					tRow.path() + ": the player inventory widget");

			// the bindings — every input accepts, every output refuses (the setCanPut(F) face)
			for (int i = 0; i < tMap.mInputItemsCount; i++) {
				ModularSlot tInput = ((ItemSlot) named(tPanel, "input_" + i)).getSlot();
				assertEquals(i, tInput.getSlotIndex(), tRow.path() + ": input " + i + " binds SLOT_INPUT " + i);
				assertTrue(tInput.mayPlace(new ItemStack(Blocks.BRICKS.asItem())), tRow.path() + ": input " + i + " accepts");
			}
			for (int i = 0; i < tMap.mOutputItemsCount; i++) {
				ModularSlot tOutput = ((ItemSlot) named(tPanel, "output_" + i)).getSlot();
				assertEquals(tMap.mInputItemsCount + i, tOutput.getSlotIndex(), tRow.path() + ": output " + i + " binds after the inputs");
				assertFalse(tOutput.mayPlace(new ItemStack(Blocks.BRICKS.asItem())),
						tRow.path() + ": output " + i + " refuses insertion");
			}

			// the map-sized inventory — the GUI binds the full input+output index range
			assertTrue(tMachine.getInventory().getSlots() >= tMap.mInputItemsCount + tMap.mOutputItemsCount,
					tRow.path() + ": the inventory holds the GUI's slot range");
		}
	}

	// ---------------------------------------------------------------------------
	// the input truth — the row map's mInputItemsCount, the MIXER/BATH case-6 table
	// ---------------------------------------------------------------------------

	/**
	 * The widget build position — the GT6BasicMachineMUIPanelTest.posOf form (pos() lands in
	 * the resizer's start Unit; the Area only resolves at layout, which the headless build lacks).
	 */
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

	/**
	 * The six-input rows seat their inputs at the descriptor's case-6 table (upstream
	 * ContainerCommonBasicMachine.java:80-85) — the r4-24a leftover in its end form: the
	 * Host default 1 used to render a single (53,25) seat for the MIXER/BATH.
	 */
	@Test
	public void theSixInputRowsSeatTheCase6Table() throws Exception {
		for (String tPath : new String[] {"large_batch_mixer", "large_bath"}) {
			GTLargeMachineBlockEntity tMachine = newMachine(GT6LargeMachines.ROWS_BY_PATH.get(tPath));
			RecipeMap tMap = tMachine.recipes();
			assertEquals(6, tMap.mInputItemsCount, tPath + ": the six-input row");
			assertEquals(6, tMachine.getInputSlotCount(), tPath + ": the live input truth (the Host default 1 was the r4-24a leftover)");

			ModularPanel<?> tPanel = tMachine.buildUI(null, headlessSyncManager(), null);
			int[][] tExpected = GT6MachineGuiLayout.inputPositions(6, tMachine.getFluidInputTanks().length);
			for (int i = 0; i < 6; i++) {
				IWidget tSeat = named(tPanel, "input_" + i);
				assertNotNull(tSeat, tPath + ": input_" + i + " seated");
				assertEquals(tExpected[i][0], posOf(tSeat, true), tPath + ": input_" + i + " x == the case-6 table");
				assertEquals(tExpected[i][1], posOf(tSeat, false), tPath + ": input_" + i + " y == the case-6 table");
			}
		}
	}

	// ---------------------------------------------------------------------------
	// the map-sized inventory — the Crusher/Shredder overflow is the point
	// ---------------------------------------------------------------------------

	@Test
	public void theThirteenSlotRowsResizePastTheBaseEleven() {
		GTLargeMachineBlockEntity tCrusher = newMachine(GT6LargeMachines.ROWS_BY_PATH.get("large_crusher"));
		assertEquals(13, tCrusher.getInventory().getSlots(),
				"the crusher 1+12 map re-sizes the base INVENTORY_SIZE 11 (upstream getDefaultInventory :524-530)");
		GTLargeMachineBlockEntity tShredder = newMachine(GT6LargeMachines.ROWS_BY_PATH.get("large_shredder"));
		assertEquals(13, tShredder.getInventory().getSlots(), "the shredder 1+12 map re-sizes too");
		// the ≤11 maps keep the base handler shape (zero change for ten of twelve)
		GTLargeMachineBlockEntity tCoagulator = newMachine(GT6LargeMachines.ROWS_BY_PATH.get("large_coagulator"));
		assertEquals(11, tCoagulator.getInventory().getSlots(), "the coagulator keeps the base 11-slot shape");
	}

	// ---------------------------------------------------------------------------
	// the open chain — the use arm + the BE contract (the offline half)
	// ---------------------------------------------------------------------------

	@Test
	public void theBlockCarriesTheUseArmAndTheBEImplementsTheOpenChain() {
		// the BE contract: GTLargeMachineBlockEntity implements GT6MuiMachine, so tryOpen's
		// intersection bound (BlockEntity & GT6MuiMachine) accepts it — the compile-time
		// face the use arm dispatches through
		assertTrue(GT6MuiMachine.class.isAssignableFrom(GTLargeMachineBlockEntity.class),
				"the large-machine BE implements GT6MuiMachine (the tryOpen bound)");
		// the use face exists under either leg's name (forge use / 21.1 useWithoutItem)
		assertTrue(Arrays.stream(GTLargeMachineBlock.class.getDeclaredMethods())
						.anyMatch(m -> m.getName().equals("use") || m.getName().equals("useWithoutItem")),
				"the controller block dispatches the open (issue #24 root cause 1: the missing use face)");
		// the registration intact (the use arm rides the same block class)
		assertTrue(GT6LargeMachines.BLOCKS_BY_PATH.containsKey("large_electrolyzer"),
				"the twelve controllers registered (the use arm rides the same block class)");
	}
}
