package gregtech6.gui.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import brachy.modularui.api.widget.IWidget;
import brachy.modularui.drawable.UITexture;
import brachy.modularui.drawable.progress.ProgressDrawable;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.ModularSyncManager;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.ProgressWidget;
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

import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.GTRecipesOfflineTestBase;
import gregtech6.tileentity.machines.TileEntityOven;

/**
 * The oven GUI parity pin (task oven-gui-arrow-align — the MUI migration): the oven rides
 * the family ModularUI panel ({@link GTBasicMachineMUI#buildPanel} over the BE as the direct
 * {@link GTBasicMachineMenu.Host}), and the retired gt6:oven vanilla menu/screen chain's
 * parity faces re-base on the panel:
 * <ul>
 * <li><b>upstream</b> gregapi/gui/ContainerCommonBasicMachine.addSlots (:45-271) on the
 *     RM.Furnace shape (1 in/1 out items, 1 in/1 out fluids): input (53,25) :55 bound to
 *     SLOT_INPUT, output (107,25) :162 canPut(F) bound to SLOT_OUTPUT, the two fluid
 *     DISPLAY seats (53,63)/(107,63) :267-268 inert both ways (Slot_Render) over the
 *     reserved display indices. The :49 special seat adds NOTHING on this map — Recipe
 *     .RecipeMap.getSpecialSlot (:400-402) is null and only RecipeMapAutocrafting (:166)
 *     overrides it. The skin's 22x22 gear print at (77,60) is decoration.</li>
 * <li><b>the composed background</b> (machine-gui-composed-purge): the theme base
 *     (parts/panel_base_176x166, the machine-area blank) under the slot frames, the arrow
 *     cell (78,24) and the gear print (77,60) — this pin replays the composition from the
 *     parts and re-asserts the painted/bare faces the retired sheet carried.</li>
 * <li><b>the migration face</b>: the BE implements {@link GTBasicMachineMenu.Host}
 *     DIRECTLY (the multiblock-base implementor form) + {@link gregtech6.gui.machines
 *     .GT6MuiMachine}; the three-state progress rides
 *     {@link GTBasicMachineMenu#progressValue} (the retired menu's computeProgressValue,
 *     :280-286 verbatim); the arrow tooltip identity rides {@code mCurrentRecipe}; the
 *     jump face rides {@code recipes()} (RM.Furnace); the title is the tier-composed
 *     block name (the Furnace map is viewer-excluded — no title key exists).</li>
 * </ul>
 *
 * <p>The retired bug (oven-gui-slot-audit): the old menu bound the BE's reserved special
 * index as a functional slot at (80,43) — a mistranslation of :49 — floating an interactive
 * cell over the bare panel. The BE keeps the reserved inventory index (upstream
 * getDefaultInventory :530 keeps the +1; findRecipe still receives its content,
 * TileEntityOven.checkRecipe :452) — it just stays GUI-unbound, reachable through
 * automation only.
 *
 * <p>Declared offline limits: the panel builds headless (the GT6BasicMachineMUIPanelTest
 * form — the FML dist shaping below); the real open chain (BlockEntityUIFactory's 64-sq
 * range gate, the network round trip) resolves live only.
 */
class GT6OvenMenuSlotParityTest extends GTRecipesOfflineTestBase {

	/**
	 * The offline FML dist shaping (the GT6BasicMachineMUIPanelTest form — shape the
	 * launcher environment the game normally provides) — must run BEFORE the first widget
	 * class initializes, hence @BeforeAll.
	 */
	@BeforeAll
	static void armFmlDistOffline() throws Exception {
		Class<?> tFmlEnv = Class.forName("net.minecraftforge.fml.loading.FMLEnvironment", false,
				GT6OvenMenuSlotParityTest.class.getClassLoader());
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

	/** The flat panel interior of panel_base (the composed base's machine-area blank, measured 203,204,212). */
	private static final int PANEL_FILL = 0xFFCBCCD4; // ARGB of (203,204,212,255)

	/** The decorative gear print box (upstream Default.png idiom): x 77..98, y 60..81, 22x22. */
	private static final int GEAR_X = 77, GEAR_Y = 60, GEAR_SIZE = 22;

	/** The retired (80,43) seat — bare panel on this skin, no functional slot upstream. */
	private static final int DEAD_X = 80, DEAD_Y = 43;

	/** The four bound cells — input/output seats + the two fluid display seats. */
	private static final int[][] BOUND_CELLS = {{53, 25}, {107, 25}, {53, 63}, {107, 63}};

	private static BlockEntityType<TileEntityOven> sOvenType;

	@BeforeAll
	static void buildOvenFixture() {
		@SuppressWarnings("unchecked")
		BlockEntityType<TileEntityOven>[] tHolder = (BlockEntityType<TileEntityOven>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new TileEntityOven(tHolder[0], aPos, aState),
				Blocks.BRICKS).build(null);
		sOvenType = tHolder[0];
	}

	@BeforeEach
	void initRecipeMaps() {
		GT6RecipeMaps.init();
	}

	@AfterEach
	void resetRecipeMaps() {
		GT6RecipeMaps.reset();
	}

	/** A fixture oven (no level — the panel build reads the inventory and the Host face only). */
	private static TileEntityOven oven() {
		return sOvenType.create(POS, Blocks.BRICKS.defaultBlockState());
	}

	/** A fresh headless sync manager (the widget binds by sync key — no container/player needed at build). */
	private static PanelSyncManager headlessSyncManager() {
		return new PanelSyncManager(new ModularSyncManager(false), true);
	}

	// ---------------------------------------------------------------------------
	// the migration face — the family chain implementors
	// ---------------------------------------------------------------------------

	@Test
	void theOvenRidesTheMuiFamilyChain() {
		TileEntityOven tOven = oven();

		assertInstanceOf(GTBasicMachineMenu.Host.class, tOven, "the BE is a direct Host (the multiblock-base form)");
		assertInstanceOf(gregtech6.gui.machines.GT6MuiMachine.class, tOven, "the BE rides the MUI open chain");
		// the Host face values the panel's sync getters poll
		assertSame(GT6RecipeMaps.FURNACE, tOven.getRecipeMap(), "the served map is RM.Furnace (the jump face)");
		assertNull(tOven.getCurrentRecipe(), "no recipe applied yet — the tooltip identity arm");
		assertEquals(TileEntityOven.OUTPUT_ITEMS_COUNT, tOven.getOutputSlotCount(), "the Furnace output shape");
		// the declared MenuProvider face (the ACT C2 form) — never called by the open chain
		assertNull(tOven.createMenu(0, null, null), "createMenu is the declared face, returns null");
	}

	// ---------------------------------------------------------------------------
	// the panel topology vs upstream (:55/:162/:267-268 — the RM.Furnace addSlots output)
	// ---------------------------------------------------------------------------

	@Test
	void contentSeatsBindTheUpstreamFurnaceShape() throws Exception {
		TileEntityOven tOven = oven();
		ModularPanel<?> tPanel = tOven.buildUI(null, headlessSyncManager(), new UISettings());
		assertNotNull(tPanel, "buildUI (the open arm's dispatch face) returns the panel");

		// the four seats: input + output (the family content seats) + the two inert display seats
		List<ItemSlot> tSeats = itemSeats(tPanel);
		assertEquals(4, tSeats.size(), "1 input + 1 output + 2 fluid displays = the four-seat Furnace shape");

		// :55 — case 1 input, mInputFluidCount(1) not > 6 → y 25; bound to SLOT_INPUT via
		// the family SLOT_INPUT + i offset
		ItemSlot tInput = tSeats.get(0);
		assertEquals(53, posOf(tInput, true), "input x");
		assertEquals(25, posOf(tInput, false), "input y");
		assertEquals(TileEntityOven.SLOT_INPUT, tInput.getSlot().getSlotIndex(), "input bound handler slot");
		assertTrue(tInput.getSlot().mayPlace(new ItemStack(Blocks.BRICKS)), "the input accepts items");

		// :162 — the output, setCanPut(F)
		ItemSlot tOutput = tSeats.get(1);
		assertEquals(107, posOf(tOutput, true), "output x");
		assertEquals(25, posOf(tOutput, false), "output y");
		assertEquals(TileEntityOven.SLOT_OUTPUT, tOutput.getSlot().getSlotIndex(), "output bound handler slot");
		assertFalse(tOutput.getSlot().mayPlace(new ItemStack(Blocks.BRICKS)), "setCanPut(F) — nothing goes into the output");

		// :267/:268 — i=0 of each display bank over the reserved BE indices, Slot_Render
		// inert both ways (the retired GTOvenMenu RenderSlot semantics)
		ItemSlot tFluidIn = tSeats.get(2);
		ItemSlot tFluidOut = tSeats.get(3);
		assertEquals(53, posOf(tFluidIn, true), "fluid display in x");
		assertEquals(63, posOf(tFluidIn, false), "fluid display in y");
		assertEquals(TileEntityOven.SLOT_FLUID_IN_DISPLAY, tFluidIn.getSlot().getSlotIndex(), "display in bound handler slot");
		assertEquals(107, posOf(tFluidOut, true), "fluid display out x");
		assertEquals(63, posOf(tFluidOut, false), "fluid display out y");
		assertEquals(TileEntityOven.SLOT_FLUID_OUT_DISPLAY, tFluidOut.getSlot().getSlotIndex(), "display out bound handler slot");
		assertFalse(tFluidIn.getSlot().mayPlace(new ItemStack(Blocks.BRICKS)), "Slot_Render: nothing in");
		assertFalse(tFluidIn.getSlot().isCanTake(), "Slot_Render: nothing out");
		assertFalse(tFluidOut.getSlot().isCanTake(), "Slot_Render: nothing out");

		// the player-inventory widget rides the family panel unconditionally
		assertNotNull(named(tPanel, "player_inventory"), "the player inventory widget");
		// the family sync seams: the three-state progress + the recipe identity
		PanelSyncManager tSync = headlessSyncManager();
		tOven.buildUI(null, tSync, new UISettings());
		assertNotNull(tSync.findSyncHandlerNullable(GTBasicMachineMUI.SYNC_PROGRESS), "the progress sync value");
		assertNotNull(tSync.findSyncHandlerNullable(GTBasicMachineMUI.SYNC_RECIPE), "the recipe identity sync value");
	}

	/**
	 * The three-state progress rides the Host face verbatim (the retired menu's
	 * computeProgressValue :280-286 — the exact function the family panel's progress sync
	 * and /gt6oven run both poll): success → PROGRESS_DONE, running → the units()
	 * normalization, idle (max 0) → -1.
	 */
	@Test
	void theThreeStateProgressRidesTheHostFace() {
		TileEntityOven tOven = oven();

		assertEquals(-1, GTBasicMachineMenu.progressValue(tOven), ":285 idle — max progress 0");

		tOven.mSuccessful = true;
		assertEquals(GTBasicMachineMenu.PROGRESS_DONE, GTBasicMachineMenu.progressValue(tOven), ":281 the success sentinel");

		tOven.mSuccessful = false;
		tOven.mMaxProgress = 100;
		tOven.mProgress = 50;
		assertEquals(16384, GTBasicMachineMenu.progressValue(tOven), ":283 the units() ceil (50/100 → 16384/32767)");
	}

	/**
	 * The retired-seat regression pin: no seat at (80,43), none inside the decorative gear
	 * print, and the BE's reserved special index rides unbound.
	 */
	@Test
	void theSpecialSeatStaysUnbound() throws Exception {
		TileEntityOven tOven = oven();
		ModularPanel<?> tPanel = tOven.buildUI(null, headlessSyncManager(), new UISettings());

		for (ItemSlot tSeat : itemSeats(tPanel)) {
			int tX = posOf(tSeat, true), tY = posOf(tSeat, false);
			String tWhere = "seat at (" + tX + "," + tY + ")";
			assertFalse(tX == DEAD_X && tY == DEAD_Y, "the (80,43) seat stays unbound: " + tWhere);
			assertFalse(tX >= GEAR_X - 1 && tX < GEAR_X - 1 + GEAR_SIZE + 1
					&& tY >= GEAR_Y - 1 && tY < GEAR_Y - 1 + GEAR_SIZE + 1,
					"the decorative gear print carries no functional slot: " + tWhere);
		}
		// the BE inventory keeps the upstream :530 shape — the reserved index exists, unbound
		assertEquals(5, TileEntityOven.INVENTORY_SIZE, "1 in + 1 out + 1 reserved special + 2 fluid displays (getDefaultInventory :530)");
		assertEquals(2, TileEntityOven.SLOT_SPECIAL, "the reserved inventory index — GUI-unbound, findRecipe still reads it (:452)");
	}

	// ---------------------------------------------------------------------------
	// the title — the tier-composed block name (the viewer-excluded map arm overridden)
	// ---------------------------------------------------------------------------

	@Test
	void theTitleIsTheTierComposedBlockName() throws Exception {
		TileEntityOven tOven = oven();
		ModularPanel<?> tPanel = tOven.buildUI(null, headlessSyncManager(), new UISettings());

		TextWidget<?> tTitle = (TextWidget<?>) named(tPanel, "title");
		assertNotNull(tTitle, "the title prints (the block-name override)");
		// the override is passed through verbatim — the fixture state's block name (a real
		// oven row composes "Oven (Steel)" through gt6.row.oven.display, already localizable)
		assertEquals(Blocks.BRICKS.getName().getContents(), tTitle.getKey().getContents(),
				"the block name rides the title row");
		assertEquals(0, posOf(tTitle, true), "title x (the centered full-width row)");
		assertEquals(4, posOf(tTitle, false), "title y (upstream :44)");
	}

	// ---------------------------------------------------------------------------
	// the composed background vs the bound seats (machine-gui-composed-purge — the
	// retired oven.png sheet's painted/bare faces, replayed from the part crops)
	// ---------------------------------------------------------------------------

	/**
	 * The composed-face replay: the composition the family panel draws at runtime — the
	 * theme base (panel_base, machine-area blank) under one 18x18 slot_frame cell per bound
	 * seat at (seat-1), the arrow cell (78,24) (the family ProgressWidget's ARROW_OUTLINE
	 * empty face under the ARROW_FORWARD fill, direction RIGHT — the Furnace map's case 0)
	 * and the 22x22 slot_special gear print at (77,60) — then the same painted/bare
	 * assertions the retired sheet carried: every bound cell painted, the retired (80,43)
	 * seat bare panel, the gear print fully painted.
	 */
	@Test
	void composedBackgroundPaintsExactlyTheBoundCells() throws IOException {
		BufferedImage tBase = ImageIO.read(partTexture("panel_base_176x166.png").toFile());
		BufferedImage tFrame = ImageIO.read(partTexture("slot_frame_18x18.png").toFile());
		BufferedImage tArrow = ImageIO.read(partTexture("arrow_outline_20x18.png").toFile());
		BufferedImage tGear = ImageIO.read(partTexture("slot_special_22x22.png").toFile());
		assertNotNull(tBase, "the panel base part decodes");
		assertEquals(176, tBase.getWidth());
		assertEquals(166, tBase.getHeight(), "the panel base is the 176x166 panel");

		// the composition, verbatim from the family panel's furniture
		BufferedImage tPanel = new BufferedImage(176, 166, BufferedImage.TYPE_INT_ARGB);
		tPanel.getGraphics().drawImage(tBase, 0, 0, null);
		for (int[] tCell : BOUND_CELLS) {
			tPanel.getGraphics().drawImage(tFrame, tCell[0] - 1, tCell[1] - 1, null);
		}
		tPanel.getGraphics().drawImage(tArrow, 78, 24, null);
		tPanel.getGraphics().drawImage(tGear, GEAR_X, GEAR_Y, null);

		// the four bound cells are painted: not one bare-panel texel inside each 16x16
		for (int[] tCell : BOUND_CELLS) {
			for (int y = tCell[1]; y < tCell[1] + 16; y++) {
				for (int x = tCell[0]; x < tCell[0] + 16; x++) {
					assertTrue(tPanel.getRGB(x, y) != PANEL_FILL,
							"the bound cell at (" + tCell[0] + "," + tCell[1] + ") is painted at (" + x + "," + y + ")");
				}
			}
		}
		// the retired seat is bare panel — the exact misalignment surface oven-gui-slot-audit retired
		for (int y = DEAD_Y; y < DEAD_Y + 16; y++) {
			for (int x = DEAD_X; x < DEAD_X + 16; x++) {
				assertEquals(PANEL_FILL, tPanel.getRGB(x, y),
						"the (80,43) seat is bare panel — nothing may anchor a slot there");
			}
		}
		// the gear print is fully painted decoration (no bare panel inside the 22x22)
		for (int y = GEAR_Y; y < GEAR_Y + GEAR_SIZE; y++) {
			for (int x = GEAR_X; x < GEAR_X + GEAR_SIZE; x++) {
				assertTrue(tPanel.getRGB(x, y) != PANEL_FILL,
						"the gear print at (" + x + "," + y + ") is painted decoration");
			}
		}
	}

	/**
	 * The arrow rides the family ProgressWidget with the parts pair and the Furnace
	 * direction: the empty cell (arrow_outline, the (78,24) print) under the clipped fill
	 * (arrow_forward), growing RIGHT (the Furnace map's mProgressBarDirection case 0) at
	 * the family seat (78,24).
	 */
	@Test
	void theArrowWearsThePartsPairInTheFurnaceDirection() throws Exception {
		TileEntityOven tOven = oven();
		ModularPanel<?> tPanel = tOven.buildUI(null, headlessSyncManager(), new UISettings());

		ProgressWidget tArrow = (ProgressWidget) allWidgets(tPanel).stream()
				.filter(w -> w instanceof ProgressWidget).findFirst().orElseThrow();
		assertEquals(78, posOf(tArrow, true), "the arrow cell x (the skins' (78,24) print)");
		assertEquals(24, posOf(tArrow, false), "the arrow cell y");

		java.lang.reflect.Field tField = ProgressWidget.class.getDeclaredField("progress");
		tField.setAccessible(true);
		ProgressDrawable tDrawable = (ProgressDrawable) tField.get(tArrow);
		UITexture tFill = assertInstanceOf(UITexture.class, tDrawable.getFilledTexture(), "the fill is a texture");
		assertEquals(partsLocation("arrow_forward_20x18.png"), tFill.location(), "the amazawa arrow fill part");
		UITexture tEmpty = assertInstanceOf(UITexture.class, tDrawable.getEmptyBackground(), "the empty cell is a texture");
		assertEquals(partsLocation("arrow_outline_20x18.png"), tEmpty.location(), "the amazawa arrow outline part");
		assertEquals(ProgressDrawable.Direction.RIGHT, tDrawable.getDirection(), "the Furnace map grows right (case 0)");
	}

	/** The gt6 gui/parts location for a part file name. */
	private static net.minecraft.resources.ResourceLocation partsLocation(String aFile) {
		return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6", "textures/gui/parts/" + aFile);
	}

	// ---------------------------------------------------------------------------
	// helpers (the GT6BasicMachineMUIPanelTest forms)
	// ---------------------------------------------------------------------------

	/** The widget by its build name ({@code null} = absent). */
	@javax.annotation.Nullable
	private static IWidget named(ModularPanel<?> aPanel, String aName) {
		return allWidgets(aPanel).stream().filter(w -> aName.equals(w.getName())).findFirst().orElse(null);
	}

	/** The widget build position — the resizer's start Unit (the Area only resolves at layout). */
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

	/** Every widget in the panel tree. */
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

	/** The gui/parts asset walk (the mdk-root probe the census tests share). */
	private static Path partTexture(String aFile) {
		for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
			if (Files.isRegularFile(p.resolve("tools").resolve("gen_textures.py"))) {
				return p.resolve(Path.of("src", "main", "resources", "assets", "gt6",
						"textures", "gui", "parts", aFile));
			}
		}
		throw new AssertionError("mdk root (tools/gen_textures.py) not found upward from " + Path.of("").toAbsolutePath());
	}
}
