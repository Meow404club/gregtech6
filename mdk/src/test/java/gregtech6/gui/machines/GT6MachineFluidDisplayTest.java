package gregtech6.gui.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gregtech6.fluid.FluidTankGT;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.GTRecipesOfflineTestBase;
import gregtech6.tileentity.GTItemStackHandler;
import gregtech6.tileentity.machines.TileEntityBasicMachine;

/**
 * The fluid-tank display offline (task machine-fluid-gui ②, re-based by task
 * basic-machines-mui-migration — the user ruling 全部统一 retired the vanilla menu/screen
 * leg, and with it the offline menu-instance arms that pinned the display-slot list):
 * <ul>
 * <li>the :267-268 display geometry full table ({@link GTBasicMachineMenu#fluidDisplayPos})
 *     — inputs descending from (53,63), outputs ascending from (107,63), both wrapping
 *     upward every 3 — with the Drying shape (RM 流 1,3) landing in[0]=(53,63) and
 *     out0..2=(107/125/143, 63) concrete; the MUI panel consumes the same function for its
 *     display seats (the seat parity rides GT6BasicMachinesMuiParityTest);</li>
 * <li>the Host extension semantics: the default banks are EMPTY (the multiblock base and
 *     the p8 fakes keep the six-method shape → zero display seats, the cokeoven panel stays
 *     byte-identical to its p8 landing) and the gui-domain adapter reads the W1a public
 *     final tank arrays LIVE (element identity, not a copy).</li>
 * </ul>
 *
 * <p>The retired arms (the offline menu instance pairing/inertness/quickMove pins) served
 * {@code new GTBasicMachineMenu(...)} over the dryer — a menu instance NO machine opens
 * since the vanilla leg retired; the display-seat face they pinned lives on the family
 * panel now.
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

}
