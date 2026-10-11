package gregtech6.gui.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import brachy.modularui.api.widget.IWidget;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.ModularSyncManager;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.TextWidget;
import brachy.modularui.widgets.slot.ItemSlot;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gregtech6.block.GTBasicMachineBlock;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.registry.GTMachines;
import gregtech6.recipes.GTRecipesOfflineTestBase;
import gregtech6.tileentity.machines.TileEntityBasicMachine;
import gregtech6.tileentity.multiblocks.TileEntityCokeOven;

/**
 * The basic-machine family MUI parity pin (task basic-machines-mui-migration — the user
 * ruling 本来不就应该全部走 MUI 拼接 UI 吗？干嘛搞特殊，我要全部统一; the GT6OvenMenuSlotParityTest
 * re-based form): the vanilla-leg machines — the Dryer ladder, the Canner ladder (+ the ULV
 * row and the two SHARED canner-carrier families) and the Coke Oven multiblock — open
 * through the family ModularUI panel, and their retired vanilla menu geometry re-bases on
 * the panel seats:
 * <ul>
 * <li><b>dryer</b> (RM.DRYING 1/1 items, 1/3 fluids): input (53,25), output (107,25)
 *     canPut(F), the four fluid display seats (53,63)/(107,63)/(125,63)/(143,63) over the
 *     live Host banks (the upstream :267/:268 Slot_Render furniture, machine-fluid-gui ②),
 *     the map-key title;</li>
 * <li><b>canner</b> (RM.CANNER 2/2 items, 1/1 fluids): the case-2 input pair (35,25)+
 *     (53,25) (the R7 expansion arm), outputs (107,25)+(125,25) canPut(F), one display
 *     seat per bank;</li>
 * <li><b>cokeoven</b> (RM.COKE_OVEN 1/9 items, 0/1 capability-side fluids): input (53,25),
 *     the 9-output 3x4 grid from (107,7), ZERO display seats (the multiblock Host keeps
 *     the default-empty banks — the p8 landing the menu javadoc pinned byte-identical),
 *     createMenu the declared null face.</li>
 * <li><b>plantalyzer + bumblelyzer</b> (RM.PLANTALYZER / RM.BUMBLELYZER, the S12
 *     fixback + the S13 debt re-pin): the title pin — both viewer-excluded maps take
 *     the explicit machine-name override through the three-arg buildPanel overload
 *     (the oven precedent), never the raw key.</li>
 * </ul>
 *
 * <p>The open-chain half: every flipped family row dispatches
 * {@link GTBasicMachineBlock#opensModularUi} → {@link GT6MuiMachine#tryOpen} (the census
 * in GTBasicMachineBlockDispatchTest pins all families; here the five flipped ladders are
 * named). The stillValid 64-sq gate and the network round trip resolve live only (the
 * BlockEntityUIFactory surface — the RCON gate).
 */
public class GT6BasicMachinesMuiParityTest extends GTRecipesOfflineTestBase {

	/**
	 * The offline FML dist shaping (the GT6BasicMachineMUIPanelTest form — shape the
	 * launcher environment the game normally provides) — must run BEFORE the first widget
	 * class initializes, hence @BeforeAll.
	 */
	@BeforeAll
	static void armFmlDistOffline() throws Exception {
		// the base class boots vanilla (GTRecipesOfflineTestBase.bootVanillaOffline runs
		// first — superclass @BeforeAll); here only the FML dist shaping, BEFORE the first
		// widget class initializes (the GT6OvenMenuSlotParityTest form)
		Class<?> tFmlEnv = Class.forName("net.minecraftforge.fml.loading.FMLEnvironment", false,
				GT6BasicMachinesMuiParityTest.class.getClassLoader());
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

	private static final BlockPos POS = new BlockPos(1, 2, 3);

	private static BlockEntityType<TileEntityBasicMachine> sDryerType;
	private static BlockEntityType<TileEntityBasicMachine> sCannerType;
	private static BlockEntityType<TileEntityBasicMachine> sPlantalyzerType;
	private static BlockEntityType<TileEntityCokeOven> sCokeOvenType;

	@BeforeAll
	static void buildFixtures() {
		@SuppressWarnings("unchecked")
		BlockEntityType<TileEntityBasicMachine>[] tSingle = (BlockEntityType<TileEntityBasicMachine>[]) new BlockEntityType<?>[3];
		// the T1 row configs (the GT6MachineFluidDisplayTest / GT6CokeOvenMenuTest fixture shapes;
		// the plantalyzer tier-1 row: parallel 64, no parallel duration — the :1601-1605 shape)
		tSingle[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new TileEntityBasicMachine(tSingle[0], aPos, aState, GT6RecipeMaps.DRYING, 8, true, null),
				Blocks.BRICKS).build(null);
		tSingle[1] = BlockEntityType.Builder.of(
				(aPos, aState) -> new TileEntityBasicMachine(tSingle[1], aPos, aState, GT6RecipeMaps.CANNER, 1, false, null),
				Blocks.BRICKS).build(null);
		tSingle[2] = BlockEntityType.Builder.of(
				(aPos, aState) -> new TileEntityBasicMachine(tSingle[2], aPos, aState, GT6RecipeMaps.PLANTALYZER, 64, false, null),
				Blocks.BRICKS).build(null);
		sPlantalyzerType = tSingle[2];
		sDryerType = tSingle[0];
		sCannerType = tSingle[1];
		@SuppressWarnings("unchecked")
		BlockEntityType<TileEntityCokeOven>[] tMulti = (BlockEntityType<TileEntityCokeOven>[]) new BlockEntityType<?>[1];
		tMulti[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new TileEntityCokeOven(tMulti[0], aPos, aState),
				Blocks.BRICKS).build(null);
		sCokeOvenType = tMulti[0];
	}

	@BeforeEach
	void initRecipeMaps() {
		GT6RecipeMaps.init();
	}

	@AfterEach
	void resetRecipeMaps() {
		GT6RecipeMaps.reset();
	}

	private static TileEntityBasicMachine dryer() {
		return sDryerType.create(POS, Blocks.BRICKS.defaultBlockState());
	}

	private static TileEntityBasicMachine canner() {
		return sCannerType.create(POS, Blocks.BRICKS.defaultBlockState());
	}

	private static TileEntityBasicMachine plantalyzer() {
		return sPlantalyzerType.create(POS, Blocks.BRICKS.defaultBlockState());
	}

	private static TileEntityCokeOven cokeOven() {
		return sCokeOvenType.create(POS, Blocks.BRICKS.defaultBlockState());
	}

	/** A fresh headless sync manager (the widget binds by sync key — no container/player needed at build). */
	private static PanelSyncManager headlessSyncManager() {
		return new PanelSyncManager(new ModularSyncManager(false), true);
	}

	// ---------------------------------------------------------------------------
	// the open chain — every flipped family row dispatches the MUI arm
	// ---------------------------------------------------------------------------

	@Test
	void theFlippedLaddersDispatchTheMuiChain() {
		// the five ladders whose suppliers retired with the vanilla leg (the SHARED
		// canner-carrier families included); the census pins the whole registry
		List<GTBasicMachineBlock.MachineRow> tFlipped = new ArrayList<>();
		tFlipped.addAll(GTMachines.DRYER_ROWS);
		tFlipped.addAll(GTMachines.CANNER_ROWS);
		tFlipped.addAll(GTMachines.CANNER_ULV_ROWS);
		tFlipped.addAll(GTMachines.BURNER_MIXER_ROWS);
		tFlipped.addAll(GTMachines.PLANTALYZER_ROWS);
		for (GTBasicMachineBlock.MachineRow tRow : tFlipped) {
			// the dispatch KEY itself is package-private to the block domain — the census
			// (GTBasicMachineBlockDispatchTest) pins opensModularUi over the same rows;
			// here the menu-less column is the pin surface
			assertNull(tRow.menu(), tRow.path() + " is menu-less — the vanilla leg is retired");
		}
	}

	@Test
	void theCokeOvenRidesTheMuiFamilyChain() {
		TileEntityCokeOven tOven = cokeOven();

		assertInstanceOf(GT6MuiMachine.class, tOven, "the BE rides the MUI open chain");
		assertInstanceOf(GTBasicMachineMenu.Host.class, tOven, "the multiblock base is a direct Host");
		assertSame(GT6RecipeMaps.COKE_OVEN, tOven.getRecipeMap(), "the served map (the jump face)");
		assertEquals(9, tOven.getOutputSlotCount(), "the COKE_OVEN output grid shape");
		// the declared MenuProvider face (the ACT C2 form) — never called by the open chain
		assertNull(tOven.createMenu(0, null, null), "createMenu is the declared face, returns null");
		// the direct-Host banks stay default-empty — zero display seats, the p8 landing
		assertEquals(0, tOven.getFluidInputTanks().length, "the :267 bank default");
		assertEquals(0, tOven.getFluidOutputTanks().length, "the :268 bank default");
	}

	// ---------------------------------------------------------------------------
	// the dryer panel — the RM.DRYING seat parity
	// ---------------------------------------------------------------------------

	@Test
	void dryerPanelSeatsMatchTheRetiredMenuGeometry() throws Exception {
		TileEntityBasicMachine tDryer = dryer();
		assertTrue(tDryer instanceof GT6MuiMachine, "the single-block BE rides the MUI open chain");
		ModularPanel<?> tPanel = tDryer.buildUI(null, headlessSyncManager(), new UISettings());
		assertNotNull(tPanel, "buildUI returns the panel");

		List<ItemSlot> tSeats = itemSeats(tPanel);
		assertEquals(2, tSeats.size(), "1 input + 1 output content shape (the displays are FluidDisplayWidgets)");

		ItemSlot tInput = tSeats.get(0);
		assertEquals(53, posOf(tInput, true), "input x (:55 case 1)");
		assertEquals(25, posOf(tInput, false), "input y");
		assertEquals(TileEntityBasicMachine.SLOT_INPUT, tInput.getSlot().getSlotIndex(), "input bound handler slot");
		assertTrue(tInput.getSlot().mayPlace(new ItemStack(Blocks.BRICKS)), "the input accepts items");

		ItemSlot tOutput = tSeats.get(1);
		assertEquals(107, posOf(tOutput, true), "output x (:162 the 1-output arm)");
		assertEquals(25, posOf(tOutput, false), "output y");
		assertEquals(TileEntityBasicMachine.SLOT_INPUT + 1, tOutput.getSlot().getSlotIndex(), "output bound handler slot");
		assertFalse(tOutput.getSlot().mayPlace(new ItemStack(Blocks.BRICKS)), "setCanPut(F) — nothing goes into the output");

		// the four fluid display seats over the LIVE banks (the RM 流 1,3 对位): the
		// widget geometry + the sync-key pairing are the display face the menu's
		// FluidDisplaySlot list used to pin
		assertFluidSeat(tPanel, "fluid_in_0", 53, 63, tDryer.mTanksInput[0]);
		assertFluidSeat(tPanel, "fluid_out_0", 107, 63, tDryer.mTanksOutput[0]);
		assertFluidSeat(tPanel, "fluid_out_1", 125, 63, tDryer.mTanksOutput[1]);
		assertFluidSeat(tPanel, "fluid_out_2", 143, 63, tDryer.mTanksOutput[2]);

		// the family sync seams + the map-key title (DRYING is viewer-visible)
		PanelSyncManager tSync = headlessSyncManager();
		tDryer.buildUI(null, tSync, new UISettings());
		assertNotNull(tSync.findSyncHandlerNullable(GTBasicMachineMUI.SYNC_PROGRESS), "the progress sync value");
		assertNotNull(tSync.findSyncHandlerNullable(GTBasicMachineMUI.SYNC_RECIPE), "the recipe identity sync value");
		TextWidget<?> tTitle = (TextWidget<?>) named(tPanel, "title");
		assertNotNull(tTitle, "the title prints (the map-key arm)");
		assertEquals("gt6.jei.recipe_map.drying",
				((net.minecraft.network.chat.contents.TranslatableContents) tTitle.getKey().getContents()).getKey(),
				"the shared viewer title key");
	}

	// ---------------------------------------------------------------------------
	// the canner panel — the case-2 expansion parity
	// ---------------------------------------------------------------------------

	@Test
	void cannerPanelSeatsMatchTheRetiredMenuGeometry() throws Exception {
		TileEntityBasicMachine tCanner = canner();
		ModularPanel<?> tPanel = tCanner.buildUI(null, headlessSyncManager(), new UISettings());
		assertNotNull(tPanel, "buildUI returns the panel");

		List<ItemSlot> tSeats = itemSeats(tPanel);
		assertEquals(4, tSeats.size(), "the 2/2 item shape (2 inputs + 2 outputs)");

		// the case-2 input pair (:58-59) — the R7 expansion arm the canner exists to serve
		ItemSlot tIn0 = tSeats.get(0), tIn1 = tSeats.get(1);
		assertEquals(35, posOf(tIn0, true), "input 0 x (the case-2 left seat)");
		assertEquals(25, posOf(tIn0, false), "input 0 y");
		assertEquals(53, posOf(tIn1, true), "input 1 x (the case-2 right seat)");
		assertEquals(25, posOf(tIn1, false), "input 1 y");
		assertEquals(TileEntityBasicMachine.SLOT_INPUT, tIn0.getSlot().getSlotIndex(), "input 0 handler slot");
		assertEquals(TileEntityBasicMachine.SLOT_INPUT + 1, tIn1.getSlot().getSlotIndex(), "input 1 handler slot");
		assertTrue(tIn0.getSlot().mayPlace(new ItemStack(Blocks.BRICKS)), "input 0 accepts items");
		assertTrue(tIn1.getSlot().mayPlace(new ItemStack(Blocks.BRICKS)), "input 1 accepts items");

		// the 2-output row, canPut(F)
		ItemSlot tOut0 = tSeats.get(2), tOut1 = tSeats.get(3);
		assertEquals(107, posOf(tOut0, true), "output 0 x");
		assertEquals(107 + 18, posOf(tOut1, true), "output 1 x (the one-row arm)");
		assertEquals(25, posOf(tOut0, false), "output 0 y");
		assertFalse(tOut0.getSlot().mayPlace(new ItemStack(Blocks.BRICKS)), "setCanPut(F)");
		assertFalse(tOut1.getSlot().mayPlace(new ItemStack(Blocks.BRICKS)), "setCanPut(F)");

		// the 1/1 display banks
		assertFluidSeat(tPanel, "fluid_in_0", 53, 63, tCanner.mTanksInput[0]);
		assertFluidSeat(tPanel, "fluid_out_0", 107, 63, tCanner.mTanksOutput[0]);

		TextWidget<?> tTitle = (TextWidget<?>) named(tPanel, "title");
		assertNotNull(tTitle, "the title prints");
		assertEquals("gt6.jei.recipe_map.canner",
				((net.minecraft.network.chat.contents.TranslatableContents) tTitle.getKey().getContents()).getKey(),
				"the shared viewer title key");
	}

	// ---------------------------------------------------------------------------
	// the plantalyzer title — the viewer-excluded map arm overridden (S12 fixback)
	// ---------------------------------------------------------------------------

	/**
	 * The S12 rejection blocker (task basic-machines-mui-migration-fixback): PLANTALYZER and
	 * BUMBLELYZER are both viewer-EXCLUDED (the GT6RecipeMapViewerMeta exclusion table — the
	 * Forestry/IC2 compat dead surface, and the display stock riding sFakeRecipes outside
	 * mRecipeList), so no {@code gt6.jei.recipe_map.*} title row exists for either (the
	 * datagen emits rows for the visible maps only, the GT6RecipeMapLangTest domain pin) and
	 * the family map-title arm would print the raw key on all tiers — a two-headed
	 * regression against upstream (Recipe.java:138 registers mNameLocal,
	 * ContainerClientBasicMachine.java:44 displayed "Plantalyzer") and the pre-migration
	 * vanilla leg (the block name). Both families ride the same
	 * {@link TileEntityBasicMachine} carrier, so both are pinned under the explicit
	 * machine-name override — the {@link GTBasicMachineMUI#buildPanel} three-arg title
	 * override — the TileEntityOven.buildUI precedent for the FURNACE trap
	 * (oven-gui-arrow-align). The bumblelyzer half is the S13 debt re-pin (debt sweep
	 * 2026-10-11): both excluded families covered by an explicit title arm, so no future
	 * family re-arms the bare excluded key.
	 */
	@Test
	void excludedMapFamiliesTitleIsTheExplicitMachineNameNotTheRawKey() throws Exception {
		// every tier of both families serves its excluded map — the condition the fix keys on
		for (GTBasicMachineBlock.MachineRow tRow : GTMachines.PLANTALYZER_ROWS) {
			assertSame(GT6RecipeMaps.PLANTALYZER, tRow.recipes().get(), tRow.path() + " serves the excluded map");
			assertFalse(gregtech6.jei.GT6RecipeMapViewerMeta.visibleToViewers(GT6RecipeMaps.PLANTALYZER),
					tRow.path() + " has no viewer title row — the map-key arm would print the raw key");
		}
		for (GTBasicMachineBlock.MachineRow tRow : GTMachines.BUMBLELYZER_ROWS) {
			assertSame(GT6RecipeMaps.BUMBLELYZER, tRow.recipes().get(), tRow.path() + " serves the excluded map");
			assertFalse(gregtech6.jei.GT6RecipeMapViewerMeta.visibleToViewers(GT6RecipeMaps.BUMBLELYZER),
					tRow.path() + " has no viewer title row — the map-key arm would print the raw key");
		}

		TileEntityBasicMachine tMachine = plantalyzer();
		ModularPanel<?> tPanel = tMachine.buildUI(null, headlessSyncManager(), new UISettings());
		TextWidget<?> tTitle = (TextWidget<?>) named(tPanel, "title");
		assertNotNull(tTitle, "the title prints (the explicit-name override, not dropped)");
		// the override rides VERBATIM — the explicit title value passed into the overload
		// (the fixture state's block name, the oven title-pin form; a real plantalyzer row
		// composes "Plantalyzer (LV)" through gt6.row.machine.plantalyzer, localizable in
		// both locales) — never the raw excluded-map key
		assertEquals(Blocks.BRICKS.getName().getContents(), tTitle.getKey().getContents(),
				"the explicit title override value — never the raw viewer key");
		assertEquals(0, posOf(tTitle, true), "title x (the centered full-width row)");
		assertEquals(4, posOf(tTitle, false), "title y (upstream :44)");
	}

	// ---------------------------------------------------------------------------
	// the cokeoven panel — the 1/9 multiblock grid parity
	// ---------------------------------------------------------------------------

	@Test
	void cokeOvenPanelSeatsMatchTheRetiredMenuGeometry() throws Exception {
		TileEntityCokeOven tOven = cokeOven();
		ModularPanel<?> tPanel = tOven.buildUI(null, headlessSyncManager(), new UISettings());
		assertNotNull(tPanel, "buildUI returns the panel");

		List<ItemSlot> tSeats = itemSeats(tPanel);
		assertEquals(10, tSeats.size(), "1 input + 9 outputs (the COKE_OVEN 1/9 shape)");

		ItemSlot tInput = tSeats.get(0);
		assertEquals(53, posOf(tInput, true), "input x (:55 case 1)");
		assertEquals(25, posOf(tInput, false), "input y");
		assertTrue(tInput.getSlot().mayPlace(new ItemStack(Blocks.BRICKS)), "the input accepts items");

		// the 7+ case = the 3x4 grid from (107,7): grid3 rows y 7/25/43
		int[][] tGrid = {
				{107, 7}, {125, 7}, {143, 7},
				{107, 25}, {125, 25}, {143, 25},
				{107, 43}, {125, 43}, {143, 43}};
		for (int i = 0; i < 9; i++) {
			ItemSlot tSeat = tSeats.get(1 + i);
			assertEquals(tGrid[i][0], posOf(tSeat, true), "output " + i + " x (the 3-column grid)");
			assertEquals(tGrid[i][1], posOf(tSeat, false), "output " + i + " y");
			assertFalse(tSeat.getSlot().mayPlace(new ItemStack(Blocks.BRICKS)),
					"output " + i + " setCanPut(F) — items come out, nothing goes in");
			assertEquals(TileEntityBasicMachine.SLOT_INPUT + 1 + i, tSeat.getSlot().getSlotIndex(),
					"output " + i + " bound handler slot (the 1-input shape)");
		}

		// zero fluid display seats — the direct Host banks are default-empty (the COKE_OVEN
		// fluid face rides the capability, NOT the GUI: the p8 landing verbatim)
		for (IWidget tWidget : allWidgets(tPanel)) {
			assertFalse(tWidget.getName() != null && tWidget.getName().startsWith("fluid_"),
					"no fluid display seat on the cokeoven panel: " + tWidget.getName());
		}

		TextWidget<?> tTitle = (TextWidget<?>) named(tPanel, "title");
		assertNotNull(tTitle, "the title prints (COKE_OVEN is viewer-visible — no override needed, unlike the oven)");
		assertEquals("gt6.jei.recipe_map.cokeoven",
				((net.minecraft.network.chat.contents.TranslatableContents) tTitle.getKey().getContents()).getKey(),
				"the shared viewer title key");

		// the three-state progress over the base Host face (the RCON-shared function)
		assertEquals(-1, GTBasicMachineMenu.progressValue((GTBasicMachineMenu.Host) tOven), "idle — max progress 0");
	}

	// ---------------------------------------------------------------------------
	// the widget-walk helpers (the GT6OvenMenuSlotParityTest form)
	// ---------------------------------------------------------------------------

	/** Pins one fluid display seat: the widget identity + the :267/:268 bank geometry. */
	private static void assertFluidSeat(ModularPanel<?> aPanel, String aName,
			int aX, int aY, gregtech6.fluid.FluidTankGT aTank) throws Exception {
		IWidget tSeat = named(aPanel, aName);
		assertNotNull(tSeat, "the display seat " + aName);
		assertEquals(aX, posOf(tSeat, true), aName + " x (the :267/:268 bank geometry)");
		assertEquals(aY, posOf(tSeat, false), aName + " y");
		// the seat drains the family sync key registered over the LIVE bank element — the
		// tank-identity face is adapterReadsTheTankBanksLive (GT6MachineFluidDisplayTest);
		// here the bank must be real for the pairing to stand on
		assertNotNull(aTank, aName + " pairs a real bank tank");
	}

	private static IWidget named(ModularPanel<?> aPanel, String aName) {
		return allWidgets(aPanel).stream().filter(w -> aName.equals(w.getName())).findFirst().orElse(null);
	}

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

	/** Every ItemSlot in the panel tree, in child order (the player-inventory group subtree excluded). */
	private static List<ItemSlot> itemSeats(ModularPanel<?> aPanel) {
		List<ItemSlot> tSeats = new ArrayList<>();
		collectSeats(aPanel, tSeats);
		return tSeats;
	}

	private static void collectSeats(IWidget aWidget, List<ItemSlot> aSink) {
		if ("player_inventory".equals(aWidget.getName())) return;
		if (aWidget instanceof ItemSlot tSlot) {
			aSink.add(tSlot);
			return; // ItemSlot is a leaf
		}
		if (aWidget instanceof brachy.modularui.widget.ParentWidget<?> tParent) {
			for (IWidget tChild : tParent.getChildren()) {
				collectSeats(tChild, aSink);
			}
		}
	}

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
}
