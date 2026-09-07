package gregtech6.gui.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.GTRecipesOfflineTestBase;
import gregtech6.recipes.RecipeMap;
import gregtech6.tileentity.GTItemStackHandler;
import gregtech6.tileentity.machines.TileEntityBasicMachine;

/**
 * The R7 menu input-slot parameterisation regression (task p24-canner-machine, the card's
 * declared highest-risk point): {@link GTBasicMachineMenu} used to hardcode the ONE input
 * slot (the mInputItemsCount==1 case, ContainerCommonBasicMachine.java:55) — the Canner
 * map's 2/2 declaration (:57-60 case 2) needs the count parameterised WITHOUT moving any
 * pre-existing family's slot grid. Two pinned faces:
 * <ul>
 * <li><b>the zero-regression arm</b> — over the five existing single-block families'
 *     RecipeMaps (Shredder/Crusher 1+12, Lathe/Distillery 1+2, Drying 1+1) the menu still
 *     lands exactly one input slot at (53,25), the outputs start at content index 1, and
 *     {@code contentSlotCount} is unchanged — the byte-identical single-input menu;</li>
 * <li><b>the expansion arm</b> — a Host reporting 2 input slots gets the upstream case-2
 *     pair (35,25)+(53,25) (inputSlotPos, :58-59), the outputs shift to content index 2+,
 *     and the Host default stays 1 (the multiblock base and the older fakes keep the six/
 *     seven-method shape — GT6MachineFluidDisplayTest.SixMethodHost is untouched).</li>
 * </ul>
 *
 * <p>Offline menu instances ride the frozen-registry-free BET fixture (the
 * GT6MachineFluidDisplayTest shape); the gt6:* MenuType registrations stay a live-server
 * surface (the RCON gate).
 */
class GT6MenuInputSlotExpansionTest extends GTRecipesOfflineTestBase {

	private static final BlockPos POS = new BlockPos(1, 2, 3);

	/** The offline machine fixture BET, rebuilt per map (fresh map identity per generation). */
	private static BlockEntityType<TileEntityBasicMachine> sMachineType;

	@BeforeEach
	void initRecipeMaps() {
		GT6RecipeMaps.init();
	}

	@AfterEach
	void resetRecipeMaps() {
		GT6RecipeMaps.reset();
	}

	/** A machine fixture over the given map (1-input arm: the ctor sizes the inventory from the RM). */
	private TileEntityBasicMachine machine(RecipeMap aMap, int aParallel, boolean aParallelDuration) {
		@SuppressWarnings("unchecked")
		BlockEntityType<TileEntityBasicMachine>[] tHolder = (BlockEntityType<TileEntityBasicMachine>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new TileEntityBasicMachine(tHolder[0], aPos, aState, aMap, aParallel, aParallelDuration, null),
				Blocks.BRICKS).build(null);
		sMachineType = tHolder[0];
		return sMachineType.create(POS, Blocks.BRICKS.defaultBlockState());
	}

	// ---------------------------------------------------------------------------
	// the zero-regression arm — the five existing families stay byte-identical
	// ---------------------------------------------------------------------------

	@Test
 void existingFamiliesKeepTheSingleInputMenu() {
		// the five existing single-block menu families: Shredder/Crusher (1+12),
		// Lathe/Distillery (1+2), Drying (1+1) — RM.java:134/:135/:97/:70/:71
		RecipeMap[] tMaps = {GT6RecipeMaps.SHREDDER, GT6RecipeMaps.CRUSHER, GT6RecipeMaps.LATHE, GT6RecipeMaps.DISTILLERY, GT6RecipeMaps.DRYING};
		int[] tOutputs = {12, 12, 2, 2, 1};
		for (int i = 0; i < tMaps.length; i++) {
			TileEntityBasicMachine tMachine = machine(tMaps[i], 8, true);
			GTBasicMachineMenu tMenu = new GTBasicMachineMenu(null, 0, new Inventory(null), tMachine);

			assertEquals(1, tMenu.tileEntity.getInputSlotCount(), tMaps[i].mNameInternal + ": the Host default 1");
			assertEquals(1 + tOutputs[i], tMenu.contentSlotCount, tMaps[i].mNameInternal + ": the content shape 1+N");
			int tDisplays = tMaps[i].mInputFluidCount + tMaps[i].mOutputFluidCount; // the :267/:268 display banks (Distillery 1+2, Drying 1+3, the SHCL trio 0)
			assertEquals(1 + tOutputs[i] + tDisplays + 36, tMenu.slots.size(), tMaps[i].mNameInternal + ": N content + displays + 36 player slots (the pre-R7 shape)");

			// content 0 = THE input slot at (53,25), the :55 arm
			assertTrue(tMenu.slots.get(0) instanceof net.minecraftforge.items.SlotItemHandler, "the input slot is the plain SlotItemHandler");
			assertPos(tMenu.slots.get(0), 53, 25, tMaps[i].mNameInternal + " input");
			// the outputs start at content index 1, the untouched output-grid geometry
			for (int j = 0; j < tOutputs[i]; j++) {
				int[] tExpected = GTBasicMachineMenu.outputGridPos(j, tOutputs[i]);
				assertPos(tMenu.slots.get(1 + j), tExpected[0], tExpected[1], tMaps[i].mNameInternal + " output " + j);
			}
			// the player block starts right after the content+display boundary (the dryer
			// fixture pins 6 = 2 content + 4 displays — GT6MachineFluidDisplayTest)
			assertEquals(1 + tOutputs[i] + tDisplays, tMenu.playerInventoryStart(), tMaps[i].mNameInternal + " player offset");
		}
	}

	// ---------------------------------------------------------------------------
	// the expansion arm — the upstream case-2 geometry
	// ---------------------------------------------------------------------------

	/** The inputSlotPos full table: the :55 single arm and the :58-59 pair. */
	@Test
	void inputSlotPosTable() {
		assertPos2(GTBasicMachineMenu.inputSlotPos(0, 1), 53, 25, ":55 the single-input arm");
		assertPos2(GTBasicMachineMenu.inputSlotPos(0, 2), 35, 25, ":58 the case-2 first slot");
		assertPos2(GTBasicMachineMenu.inputSlotPos(1, 2), 53, 25, ":59 the case-2 second slot");
	}

	/** A two-input Host — the Canner map shape over a 4-slot inventory (2+2). */
	private static final class TwoInputHost implements GTBasicMachineMenu.Host {
		final GTItemStackHandler mInventory = new GTItemStackHandler(4);
		@Override public GTItemStackHandler getInventory() { return mInventory; }
		@Override public int getInputSlotCount() { return 2; }
		@Override public int getOutputSlotCount() { return 2; }
		@Override public boolean isSuccessful() { return false; }
		@Override public long getProgress() { return 0; }
		@Override public long getMaxProgress() { return 0; }
		@Override public String getGuiTexture() { return "gt6:textures/gui/machines/canner"; }
	}

	@Test
	void twoInputHostGetsTheCase2Geometry() {
		GTBasicMachineMenu tMenu = new GTBasicMachineMenu(null, 0, new Inventory(null), new TwoInputHost());

		assertEquals(4, tMenu.contentSlotCount, "the Canner 2+2 content shape");
		assertEquals(4 + 36, tMenu.slots.size(), "4 content + 36 player");
		// the two inputs at the :58-59 positions, inventory indices 0/1
		assertPos(tMenu.slots.get(0), 35, 25, "input 0");
		assertPos(tMenu.slots.get(1), 53, 25, "input 1");
		// the outputs SHIFT to content index 2 — the R7 point (they used to start at 1)
		int[] tOut0 = GTBasicMachineMenu.outputGridPos(0, 2);
		int[] tOut1 = GTBasicMachineMenu.outputGridPos(1, 2);
		assertPos(tMenu.slots.get(2), tOut0[0], tOut0[1], "output 0 rides content index 2");
		assertPos(tMenu.slots.get(3), tOut1[0], tOut1[1], "output 1 rides content index 3");
		assertEquals(4, tMenu.playerInventoryStart(), "the player block follows the shifted boundary");
	}

	/** A Host keeping the pre-R7 method set (no getInputSlotCount override) still builds the single-input menu. */
	@Test
	void defaultHostKeepsTheSingleInputMenu() {
		GTBasicMachineMenu.Host tHost = new GTBasicMachineMenu.Host() {
			final GTItemStackHandler mInventory = new GTItemStackHandler(2);
			@Override public GTItemStackHandler getInventory() { return mInventory; }
			@Override public int getOutputSlotCount() { return 1; }
			@Override public boolean isSuccessful() { return false; }
			@Override public long getProgress() { return 0; }
			@Override public long getMaxProgress() { return 0; }
			@Override public String getGuiTexture() { return "gt6:textures/gui/machines/dryer"; }
		};
		assertEquals(1, tHost.getInputSlotCount(), "the interface default — the multiblock base and older fakes keep their shape");
		GTBasicMachineMenu tMenu = new GTBasicMachineMenu(null, 0, new Inventory(null), tHost);
		assertEquals(2, tMenu.contentSlotCount, "1+1 unchanged");
		assertPos(tMenu.slots.get(0), 53, 25, "the byte-identical single input");
	}

	private static void assertPos(net.minecraft.world.inventory.Slot aSlot, int aX, int aY, String aMessage) {
		String tFull = aMessage + " — got (" + aSlot.x + "," + aSlot.y + ")";
		assertTrue(aSlot.x == aX && aSlot.y == aY, tFull);
	}

	private static void assertPos2(int[] aActual, int aX, int aY, String aMessage) {
		org.junit.jupiter.api.Assertions.assertArrayEquals(new int[] {aX, aY}, aActual, aMessage);
	}
}
