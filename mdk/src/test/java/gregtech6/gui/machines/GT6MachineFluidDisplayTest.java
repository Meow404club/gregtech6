package gregtech6.gui.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluids;

import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gregtech6.fluid.FluidTankGT;
import gregtech6.gui.GTRenderSlot;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.GTRecipesOfflineTestBase;
import gregtech6.tileentity.GTItemStackHandler;
import gregtech6.tileentity.machines.TileEntityBasicMachine;

/**
 * The fluid-tank display slots offline (task p16-machine-fluid-gui ②, the
 * {@link GTBasicMachineMenu} half — the GT6CokeOvenMenuTest shape):
 * <ul>
 * <li>the :267-268 display geometry full table ({@link GTBasicMachineMenu#fluidDisplayPos})
 *     — inputs descending from (53,63), outputs ascending from (107,63), both wrapping
 *     upward every 3 — with the Drying shape (RM 流 1,3) landing in[0]=(53,63) and
 *     out0..2=(107/125/143, 63) concrete;</li>
 * <li>the Host extension semantics: the default banks are EMPTY (the multiblock base and
 *     the p8 fakes keep the six-method shape → zero display slots, the cokeoven menu stays
 *     byte-identical to its p8 landing) and the gui-domain adapter reads the W1a public
 *     final tank arrays LIVE (element identity, not a copy);</li>
 * <li>the full offline menu instance over a DRYING-map machine fixture: the 1+1 content
 *     slots + the 4 display slots (in the :267/:268 order) + the 36 player slots, each
 *     display paired 1:1 with its tank ({@code assertSame} on the FluidDisplaySlot.tank
 *     reference), inert in every direction, and the vanilla broadcastChanges poll contract
     * ({@code getItem()} reads EMPTY even with the tank full — nothing syncs, the backing
 *     container is never dereferenced).</li>
 * </ul>
 *
 * <p>Declared offline limits: the {@code gt6:dryer} MenuType registration and the
 * createMenu open path resolve only on a live server (RegistryObjects + FakePlayer) —
 * the RCON chain drives those; here the menu type argument stays null (vanilla
 * AbstractContainerMenu stores it, never calls through it at construction).
 */
class GT6MachineFluidDisplayTest extends GTRecipesOfflineTestBase {

	private static final BlockPos POS = new BlockPos(1, 2, 3);

	/**
	 * The offline machine fixture (the frozen-registry-free BET, the cokeoven-test shape):
	 * a DRYING-map machine with the T1 row config (parallel 8, parallel duration) and a null
	 * menu supplier — the ctor sizes mTanksInput/mTanksOutput from the RM 流 1,3 declaration
	 * (TileEntityBasicMachine :325-328), which is exactly the pairing source.
	 */
	private static BlockEntityType<TileEntityBasicMachine> sMachineType;

	/** A fixture machine for the adapter/menu arms; fresh per test (the maps are fresh per test too). */
	private TileEntityBasicMachine dryingMachine() {
		return sMachineType.create(POS, Blocks.BRICKS.defaultBlockState());
	}

	@BeforeAll
	static void buildMachineFixture() {
		@SuppressWarnings("unchecked")
		BlockEntityType<TileEntityBasicMachine>[] tHolder = (BlockEntityType<TileEntityBasicMachine>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new TileEntityBasicMachine(tHolder[0], aPos, aState, GT6RecipeMaps.DRYING, 8, true, null),
				Blocks.BRICKS).build(null);
		sMachineType = tHolder[0];
	}

	@BeforeEach
	void initRecipeMaps() {
		GT6RecipeMaps.init();
	}

	@AfterEach
	void resetRecipeMaps() {
		GT6RecipeMaps.reset();
	}

	// ---------------------------------------------------------------------------
	// the :267-268 display geometry (the fluidDisplayPos full table)
	// ---------------------------------------------------------------------------

	@Test
	void fluidDisplayPosFullTable() {
		// :267 — the input bank descends from x 53 right-to-left, wrapping upward every 3
		int[][] tIn = {{53, 63}, {35, 63}, {17, 63}, {53, 45}, {35, 45}, {17, 45}};
		for (int i = 0; i < tIn.length; i++) {
			int[] tPos = GTBasicMachineMenu.fluidDisplayPos(false, i);
			assertArrayEquals2(tPos, tIn[i], ":267 input display " + i);
		}
		// :268 — the output bank ascends from x 107 left-to-right, wrapping upward every 3
		int[][] tOut = {{107, 63}, {125, 63}, {143, 63}, {107, 45}, {125, 45}, {143, 45}};
		for (int i = 0; i < tOut.length; i++) {
			int[] tPos = GTBasicMachineMenu.fluidDisplayPos(true, i);
			assertArrayEquals2(tPos, tOut[i], ":268 output display " + i);
		}
	}

	/** The Drying RM shape (流 1,3) concrete: one input display + three output displays on the y-63 row. */
	@Test
	void dryingShapeLandsTheCanonicalRow() {
		assertArrayEquals2(GTBasicMachineMenu.fluidDisplayPos(false, 0), new int[] {53, 63}, "in[0]");
		assertArrayEquals2(GTBasicMachineMenu.fluidDisplayPos(true, 0), new int[] {107, 63}, "out[0]");
		assertArrayEquals2(GTBasicMachineMenu.fluidDisplayPos(true, 1), new int[] {125, 63}, "out[1]");
		assertArrayEquals2(GTBasicMachineMenu.fluidDisplayPos(true, 2), new int[] {143, 63}, "out[2]");
	}

	private static void assertArrayEquals2(int[] aActual, int[] aExpected, String aMessage) {
		org.junit.jupiter.api.Assertions.assertArrayEquals(aExpected, aActual, aMessage);
	}

	// ---------------------------------------------------------------------------
	// the Host extension semantics (the p16 ② default banks + the adapter)
	// ---------------------------------------------------------------------------

	/** A Host that keeps the p8 six-method shape — the default banks must read empty. */
	private static final class SixMethodHost implements GTBasicMachineMenu.Host {
		final GTItemStackHandler mInventory = new GTItemStackHandler(2);
		@Override public GTItemStackHandler getInventory() { return mInventory; }
		@Override public int getOutputSlotCount() { return 1; }
		@Override public boolean isSuccessful() { return false; }
		@Override public long getProgress() { return 0; }
		@Override public long getMaxProgress() { return 0; }
		@Override public String getGuiTexture() { return "gt6:textures/gui/machines/cokeoven"; }
	}

	@Test
	void hostDefaultBanksAreEmpty() {
		GTBasicMachineMenu.Host tHost = new SixMethodHost();
		assertEquals(0, tHost.getFluidInputTanks().length, "the :267 bank defaults empty — the multiblock base keeps the six-method shape");
		assertEquals(0, tHost.getFluidOutputTanks().length, "the :268 bank defaults empty — the cokeoven menu renders zero display slots (the p8 shape)");
	}

	@Test
	void adapterReadsTheTankBanksLive() {
		TileEntityBasicMachine tMachine = dryingMachine();
		// the RM 流 1,3 declaration flowed into the W1a arrays at construction
		assertEquals(1, tMachine.mTanksInput.length, "the Drying input bank (mInputFluidCount)");
		assertEquals(3, tMachine.mTanksOutput.length, "the Drying output bank (mOutputFluidCount)");

		GTBasicMachineMenu.Host tAdapter = GTBasicMachineMenu.hostOf(tMachine);
		assertSame(tMachine.mTanksInput, tAdapter.getFluidInputTanks(), "the :267 bank is the LIVE array, not a copy");
		assertSame(tMachine.mTanksOutput, tAdapter.getFluidOutputTanks(), "the :268 bank is the LIVE array, not a copy");
		assertSame(tMachine.mTanksInput[0], tAdapter.getFluidInputTanks()[0], "in[0] identity — the slot-tank pairing stands on it");
		assertSame(tMachine.mTanksOutput[2], tAdapter.getFluidOutputTanks()[2], "out[2] identity");
	}

	// ---------------------------------------------------------------------------
	// the full offline menu instance (the slot-tank pairing + inertness)
	// ---------------------------------------------------------------------------

	@Test
	void dryerMenuInstanceCarriesThePairedDisplaySlots() {
		TileEntityBasicMachine tMachine = dryingMachine();
		// the createMenu shape (TileEntityBasicMachine.java:1474) minus the live MenuType —
		// vanilla AbstractContainerMenu stores the type, never calls through it here
		GTBasicMachineMenu tMenu = new GTBasicMachineMenu(null, 0, new Inventory(null), tMachine);

		// 2 content (1 in + 1 out) + 4 displays (1 in-bank + 3 out-bank) + 36 player slots
		assertEquals(2, tMenu.contentSlotCount, "the Drying 1+1 content shape");
		assertEquals(6, tMenu.playerInventoryStart(), "the displays sit AFTER the content boundary, BEFORE the player block");
		assertEquals(42, tMenu.slots.size(), "2 content + 4 displays + 36 player");

		// the display list: :267 bank first, then :268 — menu indices 2..5
		var tDisplays = tMenu.fluidDisplaySlots();
		assertEquals(4, tDisplays.size(), "1 input + 3 output displays (the RM 流 1,3 对位)");
		for (GTBasicMachineMenu.FluidDisplaySlot tSlot : tDisplays) {
			assertTrue(tSlot instanceof GTRenderSlot, "every display is a Slot_Render (Slot_Holo(F,F,0))");
		}
		// the slot-tank pairing 1:1 — slot i of each bank IS tank i of the Host array
		assertSame(tMachine.mTanksInput[0], tDisplays.get(0).tank, "display 0 pairs with the input tank");
		assertSame(tMachine.mTanksOutput[0], tDisplays.get(1).tank, "display 1 pairs with output tank 0");
		assertSame(tMachine.mTanksOutput[1], tDisplays.get(2).tank, "display 2 pairs with output tank 1");
		assertSame(tMachine.mTanksOutput[2], tDisplays.get(3).tank, "display 3 pairs with output tank 2");

		// the menu geometry: content 0=(53,25), out 1=(107,25), then the :267/:268 banks
		assertPos(tMenu.slots.get(0), 53, 25, "content input");
		assertPos(tMenu.slots.get(1), 107, 25, "content output (the 1-output arm)");
		assertPos(tDisplays.get(0), 53, 63, "display in[0] (:267)");
		assertPos(tDisplays.get(1), 107, 63, "display out[0] (:268)");
		assertPos(tDisplays.get(2), 125, 63, "display out[1] (:268)");
		assertPos(tDisplays.get(3), 143, 63, "display out[2] (:268)");
	}

	/** The displays are inert and the vanilla sync poll reads them as empty even with fluid in the tanks. */
	@Test
	void displaySlotsAreInertAndSyncNothing() {
		TileEntityBasicMachine tMachine = dryingMachine();
		tMachine.mTanksInput[0].fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
		GTBasicMachineMenu tMenu = new GTBasicMachineMenu(null, 0, new Inventory(null), tMachine);

		GTBasicMachineMenu.FluidDisplaySlot tDisplay = tMenu.fluidDisplaySlots().get(0);
		assertSame(tMachine.mTanksInput[0], tDisplay.tank, "the pairing holds");
		assertTrue(tDisplay.tank.has(), "precondition: the paired tank holds the water");
		assertTrue(tDisplay.getItem().isEmpty(), "getItem reads EMPTY regardless of the tank content — "
				+ "the broadcastChanges poll (AbstractContainerMenu.java:168-170) syncs nothing and never dereferences the backing container");
		assertFalse(tDisplay.hasItem(), "Slot_Render.hasItem is hardcoded false — the screen draws the frame only");
		assertFalse(tDisplay.mayPlace(new ItemStack(net.minecraft.world.item.Items.BUCKET)), "nothing goes in");
		assertFalse(tDisplay.mayPickup(null), "nothing comes out (the player argument is never dereferenced)");
		assertTrue(tDisplay.remove(1).isEmpty(), "remove stays EMPTY (mCanStackItem F)");
		// shift-click through a display slot moves nothing (hasItem()=false short-circuits quickMove;
		// the Player argument is unused by quickMoveBetween, null is safe offline)
		assertTrue(tMenu.quickMoveStack(null, 2).isEmpty(), "a display slot is never a quickMove source");
		assertTrue(tMenu.quickMoveStack(null, 5).isEmpty(), "the last output display neither");
	}

	private static void assertPos(net.minecraft.world.inventory.Slot aSlot, int aX, int aY, String aMessage) {
		String tFull = aMessage + " — got (" + aSlot.x + "," + aSlot.y + ")";
		assertTrue(aSlot.x == aX && aSlot.y == aY, tFull);
	}
}
