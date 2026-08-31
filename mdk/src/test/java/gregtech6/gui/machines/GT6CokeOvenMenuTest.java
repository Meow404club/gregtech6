package gregtech6.gui.machines;

import static org.junit.jupiter.api.Assertions.*;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.GTRecipesOfflineTestBase;
import gregtech6.tileentity.GTItemStackHandler;
import gregtech6.tileentity.machines.TileEntityBasicMachine;
import gregtech6.tileentity.multiblocks.TileEntityBase10MultiBlockMachine;

/**
 * The Coke Oven menu pure-function face offline (task p8-cokeoven-gui-menu ⑧):
 * <ul>
 * <li>the three-state progress function {@link GTBasicMachineMenu#progressValue(Host)}
 *     over fake Hosts — idle = -1, success = 32767, running = the units() normalization
 *     (upstream ContainerCommonBasicMachine.java:280-286);</li>
 * <li>the {@link GTBasicMachineMenu#outputGridPos} full table for the 9-output Coke Oven
 *     shape (107/125/143 x 7/25/43 — the 3x4 default branch), plus the 1/3/6 spot checks;</li>
 * <li>the gui-domain Host adapter ({@code GTBasicMachineMenu.hostOf}) reads the machine's
 *     fields LIVE — each getter reconciled against the raw field it must mirror.</li>
 * </ul>
 *
 * <p>Declared offline limits: BE/Menu construction with live MenuTypes is MC-bootstrap
 * territory (RegistryObject, Inventory, Player) — the menu instance itself and the
 * multiblock Host implementation are exercised live by the RCON chain (card ⑨); here the
 * machine fixture uses the frozen-registry-free test BET (the oven precedent,
 * GTMachinesOfflineTestBase) and the fake Hosts are plain records.
 */
class GT6CokeOvenMenuTest extends GTRecipesOfflineTestBase {

	private static final BlockPos POS = new BlockPos(1, 2, 3);

	/** A fake Host with writable fields — the three-state progress test rig. */
	private static final class FakeHost implements GTBasicMachineMenu.Host {
		final GTItemStackHandler mInventory = new GTItemStackHandler(10);
		int mOutputCount = 9;
		boolean mSuccessful = false;
		long mProgress = 0, mMaxProgress = 0;
		String mGuiTexture = "gt6:textures/gui/machines/cokeoven";

		@Override public GTItemStackHandler getInventory() { return mInventory; }
		@Override public int getOutputSlotCount() { return mOutputCount; }
		@Override public boolean isSuccessful() { return mSuccessful; }
		@Override public long getProgress() { return mProgress; }
		@Override public long getMaxProgress() { return mMaxProgress; }
		@Override public String getGuiTexture() { return mGuiTexture; }
	}

	/**
	 * The offline machine fixture (the frozen-registry-free BET, oven precedent): a Shredder
	 * map machine with a null MenuType supplier — the adapter test needs the public face only.
	 */
	private static BlockEntityType<TileEntityBasicMachine> sMachineType;

	@BeforeAll
	static void buildMachineFixture() {
		@SuppressWarnings("unchecked")
		BlockEntityType<TileEntityBasicMachine>[] tHolder = (BlockEntityType<TileEntityBasicMachine>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new TileEntityBasicMachine(tHolder[0], aPos, aState, GT6RecipeMaps.SHREDDER, 1, false, null),
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
	// the three-state progress (upstream :280-286)
	// ---------------------------------------------------------------------------

	@Test
	void progressIdleIsMinusOne() {
		FakeHost tHost = new FakeHost();
		tHost.mSuccessful = false;
		tHost.mMaxProgress = 0; // no active process
		tHost.mProgress = 1234;
		assertEquals(-1, GTBasicMachineMenu.progressValue(tHost), ":285 — no process, no bar");
	}

	@Test
	void progressSuccessIs32767() {
		FakeHost tHost = new FakeHost();
		tHost.mSuccessful = true;
		tHost.mMaxProgress = 3600;
		tHost.mProgress = 100;
		assertEquals(GTBasicMachineMenu.PROGRESS_DONE, GTBasicMachineMenu.progressValue(tHost), ":281 — the success flag wins");
		assertEquals(32767, GTBasicMachineMenu.PROGRESS_DONE, "Short.MAX_VALUE (upstream :281)");
		// the success flag dominates even a full progress counter
		tHost.mProgress = 3600;
		assertEquals(32767, GTBasicMachineMenu.progressValue(tHost));
	}

	@Test
	void progressRunningIsUnitsNormalized() {
		FakeHost tHost = new FakeHost();
		tHost.mSuccessful = false;
		tHost.mMaxProgress = 3600; // the cokeoven row duration
		// the normalization is units(min(max, progress), max, 32767, T) — asserted against the
		// same static units() the production path calls (TileEntityBasicMachine.units :739)
		tHost.mProgress = 100;
		assertEquals((int) TileEntityBasicMachine.units(100, 3600, 32767, true), GTBasicMachineMenu.progressValue(tHost));
		tHost.mProgress = 1800;
		assertEquals((int) TileEntityBasicMachine.units(1800, 3600, 32767, true), GTBasicMachineMenu.progressValue(tHost));
		// hand-checked: 1800*32767/3600 = 16383 remainder 1800 → round up → 16384
		assertEquals(16384, GTBasicMachineMenu.progressValue(tHost));
		// the min(max, progress) clamp: an over-counted progress reads as the full bar
		tHost.mProgress = 5000;
		assertEquals(32767, GTBasicMachineMenu.progressValue(tHost), ":283 min clamp → exact full bar, NOT the success path");
		// progress strictly inside (0, 32767) — the RCON mid-run assertion window
		tHost.mProgress = 900;
		int tMid = GTBasicMachineMenu.progressValue(tHost);
		assertTrue(tMid > 0 && tMid < 32767, "mid-run progress must sit in (0, 32767), got " + tMid);
	}

	@Test
	void progressFullProcessReadsExactlyDone() {
		FakeHost tHost = new FakeHost();
		tHost.mSuccessful = false;
		tHost.mMaxProgress = 3600;
		tHost.mProgress = 3600; // the completion tick before the success flag flips
		assertEquals(32767, GTBasicMachineMenu.progressValue(tHost));
	}

	// ---------------------------------------------------------------------------
	// the output grid geometry (upstream :169-270, the 3x4 default branch)
	// ---------------------------------------------------------------------------

	@Test
	void outputGridPosNineOutputsFullTable() {
		int[][] tExpected = {
				{107, 7}, {125, 7}, {143, 7},
				{107, 25}, {125, 25}, {143, 25},
				{107, 43}, {125, 43}, {143, 43}};
		for (int i = 0; i < 9; i++) {
			int[] tPos = GTBasicMachineMenu.outputGridPos(i, 9);
			assertArrayEquals(tExpected[i], tPos, "cokeoven output " + i + " (row " + (i / 3) + ", column " + (i % 3) + ")");
		}
	}

	@Test
	void outputGridPosSmallerShapes() {
		// 1-3 outputs: one row from x 107 at y 25 (:178-181)
		assertArrayEquals(new int[] {107, 25}, GTBasicMachineMenu.outputGridPos(0, 1));
		assertArrayEquals(new int[] {107, 25}, GTBasicMachineMenu.outputGridPos(0, 3));
		assertArrayEquals(new int[] {143, 25}, GTBasicMachineMenu.outputGridPos(2, 3));
		// 4-6 outputs: two rows at y 16/34 (:208-230 shape)
		assertArrayEquals(new int[] {107, 16}, GTBasicMachineMenu.outputGridPos(0, 6));
		assertArrayEquals(new int[] {143, 34}, GTBasicMachineMenu.outputGridPos(5, 6));
		// 12 outputs: the shredder/crusher 3x4 — same columns, the fourth row at y 61
		assertArrayEquals(new int[] {143, 61}, GTBasicMachineMenu.outputGridPos(11, 12));
	}

	// ---------------------------------------------------------------------------
	// the gui-domain Host adapter reconciliation (p8 ①)
	// ---------------------------------------------------------------------------

	@Test
	void machineHostAdapterReadsFieldsLive() {
		TileEntityBasicMachine tMachine = sMachineType.create(POS, Blocks.BRICKS.defaultBlockState());
		GTBasicMachineMenu.Host tAdapter = GTBasicMachineMenu.hostOf(tMachine);

		// the inventory identity — the menu slots bind THE machine's handler, not a copy
		assertSame(tMachine.getInventory(), tAdapter.getInventory());
		// the RecipeMap-derived slot shape
		assertEquals(tMachine.getOutputSlotCount(), tAdapter.getOutputSlotCount());
		assertEquals(GT6RecipeMaps.SHREDDER.mOutputItemsCount, tAdapter.getOutputSlotCount());
		// the GUI texture = mRecipes.mGUIPath (the field the screen parses)
		assertEquals(tMachine.mRecipes.mGUIPath, tAdapter.getGuiTexture());

		// the three progress inputs — live reads, not a snapshot
		tMachine.mSuccessful = false;
		tMachine.mProgress = 100;
		tMachine.mMaxProgress = 3600;
		assertEquals(100, tAdapter.getProgress());
		assertEquals(3600, tAdapter.getMaxProgress());
		assertFalse(tAdapter.isSuccessful());
		assertEquals(GTBasicMachineMenu.progressValue(tAdapter), tMachine.mSuccessful ? 32767 : (int) TileEntityBasicMachine.units(Math.min(tMachine.mMaxProgress, tMachine.mProgress), tMachine.mMaxProgress, 32767, true));

		tMachine.mProgress = 1800;
		assertEquals(1800, tAdapter.getProgress(), "the adapter reads THROUGH to the live field");
		tMachine.mSuccessful = true;
		assertTrue(tAdapter.isSuccessful());
		assertEquals(32767, GTBasicMachineMenu.progressValue(tAdapter));
	}

	// ---------------------------------------------------------------------------
	// the multiblock face constants the RCON message asserts (p8 ⑨ anchors)
	// ---------------------------------------------------------------------------

	@Test
	void multiblockSlotConstantsMatchMenuShape() {
		// the menu binds slot 0 as the input and 1..9 as outputs — the RCON report's
		// content_slots=10 / slot0=(53,25) / out0..out8 geometry stands on these
		assertEquals(0, TileEntityBasicMachine.SLOT_INPUT);
		assertEquals(0, TileEntityBase10MultiBlockMachine.SLOT_INPUT);
		assertEquals(11, TileEntityBase10MultiBlockMachine.INVENTORY_SIZE);
		assertEquals(10, TileEntityBase10MultiBlockMachine.SLOT_SPECIAL);
	}
}
