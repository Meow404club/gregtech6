package gregtech6.gui.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.recipes.GTRecipesOfflineTestBase;
import gregtech6.tileentity.machines.TileEntityOven;

/**
 * The oven GUI slot parity pin (task oven-gui-slot-audit) — the menu geometry against BOTH
 * authorities, slot by slot:
 * <ul>
 * <li><b>upstream</b> gregapi/gui/ContainerCommonBasicMachine.addSlots (:45-271) on the
 *     RM.Furnace shape (1 in/1 out items, 1 in/1 out fluids): input (53,25) :55, output
 *     (107,25) :162 setCanPut(F), fluid displays (53,63)/(107,63) :267-268 i=0. The :49
 *     special seat adds NOTHING on this map — Recipe.RecipeMap.getSpecialSlot (:400-402)
 *     is null and only RecipeMapAutocrafting (:166) overrides it. The skin's 22x22 gear
 *     print at (77,60) is decoration.</li>
 * <li><b>the composed background</b> (machine-gui-composed-purge — the retired
 *     gui/machines/oven.png sheet): {@link GTOvenScreen} composes the part crops at the
 *     same coordinates — slot frames at (slot-1), the arrow cell at (78,24), the gear
 *     print at (77,60) — and this pin replays that composition from the parts, then
 *     re-asserts the same painted/bare faces the sheet carried.</li>
 * </ul>
 *
 * <p>The retired bug: the menu bound the BE's reserved special index as a functional slot at
 * (80,43) — a mistranslation of :49 (whose getSpecialSlot call yields null) — floating an
 * interactive cell over the bare panel between the two slot rows while the painted gear print sat
 * empty. The BE keeps the reserved inventory index (upstream getDefaultInventory :530
 * keeps the +1; findRecipe still receives its content, TileEntityOven.checkRecipe :452) — it just
 * stays GUI-unbound, reachable through automation only.
 *
 * <p>Declared offline limits: the {@code gt6:oven} MenuType registration and the real open
 * path resolve only live — the menu type argument stays null (vanilla AbstractContainerMenu
 * stores it, never calls through it at construction; the GT6MachineFluidDisplayTest shape).
 */
class GT6OvenMenuSlotParityTest extends GTRecipesOfflineTestBase {

	private static final BlockPos POS = new BlockPos(1, 2, 3);

	/** The flat panel interior of panel_base (the composed base's machine-area blank, measured 203,204,212). */
	private static final int PANEL_FILL = 0xFFCBCCD4; // ARGB of (203,204,212,255)

	/** The decorative gear print box (upstream Default.png idiom): x 77..98, y 60..81, 22x22. */
	private static final int GEAR_X = 77, GEAR_Y = 60, GEAR_SIZE = 22;

	/** The retired (80,43) seat — bare panel on this skin, no functional slot upstream. */
	private static final int DEAD_X = 80, DEAD_Y = 43;

	/** The four bound cells — the menu geometry the frames compose under. */
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

	/** A fixture oven (no level — the menu ctor reads the inventory only). */
	private static TileEntityOven oven() {
		return sOvenType.create(POS, Blocks.BRICKS.defaultBlockState());
	}

	private static GTOvenMenu menu() {
		return new GTOvenMenu(null, 0, new Inventory(null), oven());
	}

	// ---------------------------------------------------------------------------
	// the menu geometry vs upstream (:55/:162/:267-268 — the RM.Furnace addSlots output)
	// ---------------------------------------------------------------------------

	@Test
	void contentSlotsBindTheUpstreamFurnaceShape() {
		GTOvenMenu tMenu = menu();

		assertEquals(4, GTOvenMenu.CONTENT_SLOT_COUNT, "4 content slots — the :49 special seat adds nothing on this map");
		assertEquals(40, tMenu.slots.size(), "4 content + 36 player slots");
		assertEquals(4, tMenu.playerInventoryStart(), "the player block starts right after the content slots");

		// :55 — case 1 input, mInputFluidCount(1) not > 6 → y 25
		assertSlot(tMenu.slots.get(0), 53, 25, TileEntityOven.SLOT_INPUT, "input");
		// :162 — the output, setCanPut(F)
		net.minecraft.world.inventory.Slot tOutput = tMenu.slots.get(1);
		assertSlot(tOutput, 107, 25, TileEntityOven.SLOT_OUTPUT, "output");
		assertFalse(tOutput.mayPlace(new ItemStack(net.minecraft.world.item.Items.BUCKET)), "setCanPut(F) — nothing goes into the output");
		// :267/:268 — i=0 of each display bank, Slot_Render inert both ways
		net.minecraft.world.inventory.Slot tFluidIn = tMenu.slots.get(2);
		net.minecraft.world.inventory.Slot tFluidOut = tMenu.slots.get(3);
		assertSlot(tFluidIn, 53, 63, TileEntityOven.SLOT_FLUID_IN_DISPLAY, "fluid display in");
		assertSlot(tFluidOut, 107, 63, TileEntityOven.SLOT_FLUID_OUT_DISPLAY, "fluid display out");
		assertFalse(tFluidIn.mayPlace(new ItemStack(net.minecraft.world.item.Items.BUCKET)), "Slot_Render: nothing in");
		assertFalse(tFluidIn.mayPickup(null), "Slot_Render: nothing out");
		assertFalse(tFluidOut.mayPickup(null), "Slot_Render: nothing out (the player argument is unused)");
	}

	/**
	 * The retired-seat regression pin: no menu slot at (80,43), none inside the decorative
	 * gear print, and the BE's reserved special index rides unbound.
	 */
	@Test
	void theSpecialSeatStaysUnbound() {
		GTOvenMenu tMenu = menu();

		for (int i = 0; i < tMenu.slots.size(); i++) {
			net.minecraft.world.inventory.Slot tSlot = tMenu.slots.get(i);
			String tWhere = "slot " + i + " at (" + tSlot.x + "," + tSlot.y + ")";
			assertFalse(tSlot.x == DEAD_X && tSlot.y == DEAD_Y, "the (80,43) seat stays unbound: " + tWhere);
			assertFalse(tSlot.x >= GEAR_X - 1 && tSlot.x < GEAR_X - 1 + GEAR_SIZE + 1
					&& tSlot.y >= GEAR_Y - 1 && tSlot.y < GEAR_Y - 1 + GEAR_SIZE + 1,
					"the decorative gear print carries no functional slot: " + tWhere);
		}
		// the BE inventory keeps the upstream :530 shape — the reserved index exists, unbound
		assertEquals(5, TileEntityOven.INVENTORY_SIZE, "1 in + 1 out + 1 reserved special + 2 fluid displays (getDefaultInventory :530)");
		assertEquals(2, TileEntityOven.SLOT_SPECIAL, "the reserved inventory index — GUI-unbound, findRecipe still reads it (:452)");
	}

	// ---------------------------------------------------------------------------
	// the composed background vs the bound slots (machine-gui-composed-purge — the
	// retired oven.png sheet's painted/bare faces, replayed from the part crops)
	// ---------------------------------------------------------------------------

	/**
	 * The composed-face replay: the same composition GTOvenScreen.renderBg runs — the
	 * panel base (machine-area blank) under one 18x18 slot_frame cell per bound slot at
	 * (slot-1), the arrow_outline cell at (78,24) and the 22x22 slot_special gear print at
	 * (77,60) — then the same painted/bare assertions the retired sheet carried: every
	 * bound cell painted, the retired (80,43) seat bare panel, the gear print fully painted.
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

		// the composition, verbatim from GTOvenScreen.renderBg's blit sequence
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
		// the retired seat is bare panel — the exact misalignment surface this card retired
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

	// ---------------------------------------------------------------------------
	// helpers
	// ---------------------------------------------------------------------------

	/** Position + bound handler-slot index (vanilla Slot.index carries the handler slot for SlotItemHandler). */
	private static void assertSlot(net.minecraft.world.inventory.Slot aSlot, int aX, int aY, int aHandlerSlot, String aLabel) {
		assertEquals(aX, aSlot.x, aLabel + " x — got " + aSlot.x);
		assertEquals(aY, aSlot.y, aLabel + " y — got " + aSlot.y);
		assertEquals(aHandlerSlot, aSlot.getContainerSlot(), aLabel + " bound handler slot");
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
