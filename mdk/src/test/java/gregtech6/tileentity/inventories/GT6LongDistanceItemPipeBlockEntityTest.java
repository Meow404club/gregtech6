package gregtech6.tileentity.inventories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraftforge.items.IItemHandler;

import gregtech6.registry.GT6LongDistPipes;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * GT6LongDistanceItemPipeBlockEntity offline tests (task p35-long-distance-pipes): the
 * Loader_Blocks :179 temperature row (the literal pin over the zh dump faces), the
 * delegation window (the insert/extract/getSlots forwarding over the offline rig), the
 * sender-claim protocol (:126 — the first sender keeps the target), and the no-link /
 * stopped refusals (:111/:125) + the NBT face (:59-75).
 *
 * <p>Offline harness (the transformer test form): the {@code setTargetOverride} seam
 * replaces the wire BFS (no level), the {@code setRemoteOverride} seam injects the
 * remote inventory behind the target's BACK (no level query).
 */
public class GT6LongDistanceItemPipeBlockEntityTest extends GTOfflineTestBase {

	static BlockEntityType<GT6LongDistanceItemPipeBlockEntity> sType;
	static final BlockPos POS = new BlockPos(10, 4, 5);
	static final BlockPos FAR = new BlockPos(110, 4, 5); // the far endpoint, whole chunks away

	/** The fresh item-endpoint fixture (the transformer offline form over a synthetic BET). */
	private static GT6LongDistanceItemPipeBlockEntity endpoint() {
		return new GT6LongDistanceItemPipeBlockEntity(sType, POS, Blocks.STONE.defaultBlockState());
	}

	/** The counting remote inventory (the chest behind the target's BACK). */
	public static class StubInventory implements IItemHandler {
		final ItemStack[] slots = {ItemStack.EMPTY, ItemStack.EMPTY};
		int inserts = 0, extracts = 0;

		@Override
		public int getSlots() {return 2;}

		@Override
		public ItemStack getStackInSlot(int aSlot) {return slots[aSlot];}

		@Override
		public ItemStack insertItem(int aSlot, ItemStack aStack, boolean aSimulate) {
			if (!slots[aSlot].isEmpty()) return aStack;
			inserts++;
			if (!aSimulate) slots[aSlot] = aStack.copy();
			return ItemStack.EMPTY;
		}

		@Override
		public ItemStack extractItem(int aSlot, int aAmount, boolean aSimulate) {
			if (slots[aSlot].isEmpty()) return ItemStack.EMPTY;
			extracts++;
			ItemStack tOut = slots[aSlot].copy();
			if (!aSimulate) slots[aSlot] = ItemStack.EMPTY;
			return tOut;
		}

		@Override
		public int getSlotLimit(int aSlot) {return 64;}

		@Override
		public boolean isItemValid(int aSlot, ItemStack aStack) {return true;}
	}

	/** The rig: sender windowing the target, the stub inventory behind the target's BACK. */
	private static StubInventory wire(GT6LongDistanceItemPipeBlockEntity aSender, GT6LongDistanceItemPipeBlockEntity aTarget) {
		StubInventory tRemote = new StubInventory();
		aSender.setTargetOverride(aTarget);
		aSender.setRemoteOverride(tRemote);
		return tRemote;
	}

	@BeforeAll
	static void buildOfflineFixture() {
		BlockEntityType<GT6LongDistanceItemPipeBlockEntity>[] tHolder = (BlockEntityType<GT6LongDistanceItemPipeBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6LongDistanceItemPipeBlockEntity(tHolder[0], aPos, aState), Blocks.STONE).build(null);
		sType = tHolder[0];
	}

	// ---------------------------------------------------------------------------
	// 1. the row-table pin (the Loader_Blocks :179 row over the zh dump faces)
	// ---------------------------------------------------------------------------

	@Test
	public void temperatureRowPinsTheUpstreamDump() {
		assertEquals(16, GT6LongDistPipes.ROWS.size());
		long[] tExpected = {-1, 1943, 3695, 5425, 4500, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
		for (int i = 0; i < 16; i++) {
			assertEquals(i, GT6LongDistPipes.ROWS.get(i).meta());
			assertEquals(tExpected[i], GT6LongDistPipes.ROWS.get(i).temperatureK(), "meta " + i + ": the rating");
		}
		assertEquals("long_dist_pipe_0", GT6LongDistPipes.pathOf(0));
		assertEquals("long_dist_pipe_15", GT6LongDistPipes.pathOf(15));
	}

	// ---------------------------------------------------------------------------
	// 2. the delegation window (the :190-285 IInventory delegation folded)
	// ---------------------------------------------------------------------------

	@Test
	public void windowForwardsSlotsInsertAndExtractToTheRemoteInventory() {
		GT6LongDistanceItemPipeBlockEntity tSender = endpoint();
		GT6LongDistanceItemPipeBlockEntity tTarget = new GT6LongDistanceItemPipeBlockEntity(sType, FAR, Blocks.STONE.defaultBlockState());
		StubInventory tRemote = wire(tSender, tTarget);

		assertEquals(2, tSender.window().getSlots(), "the remote slot count through the window");
		assertTrue(tSender.window().insertItem(0, new ItemStack(Items.IRON_INGOT, 8), false).isEmpty(), "the insert lands remotely");
		assertEquals(1, tRemote.inserts, "one remote insert");
		assertEquals(8, tRemote.slots[0].getCount(), "the whole stack");
		assertFalse(tSender.window().getStackInSlot(0).isEmpty(), "the window reads the remote slot");
		assertFalse(tSender.window().extractItem(0, 8, false).isEmpty(), "the extraction pulls through the window");
		assertEquals(1, tRemote.extracts);
		assertTrue(tSender.window().isItemValid(0, new ItemStack(Items.IRON_INGOT)), "the remote admission");
		assertEquals(64, tSender.window().getSlotLimit(0), "the remote limit");
	}

	@Test
	public void fullRemoteSlotRefusesTheInsert() {
		GT6LongDistanceItemPipeBlockEntity tSender = endpoint();
		GT6LongDistanceItemPipeBlockEntity tTarget = new GT6LongDistanceItemPipeBlockEntity(sType, FAR, Blocks.STONE.defaultBlockState());
		StubInventory tRemote = wire(tSender, tTarget);
		tRemote.slots[0] = new ItemStack(Items.GOLD_INGOT, 64);

		ItemStack tInput = new ItemStack(Items.IRON_INGOT, 8);
		assertEquals(tInput, tSender.window().insertItem(0, tInput, false), "the occupied slot refuses — the stack stays with the sender");
	}

	@Test
	public void noLinkRefusesEverything() {
		GT6LongDistanceItemPipeBlockEntity tSender = endpoint(); // no override: the offline scan self-seeds
		assertEquals(0, tSender.window().getSlots(), "no target, no slots");
		assertTrue(tSender.window().getStackInSlot(0).isEmpty());
		ItemStack tInput = new ItemStack(Items.IRON_INGOT, 8);
		assertEquals(tInput, tSender.window().insertItem(0, tInput, false), "no link, no transfer");
		assertTrue(tSender.window().extractItem(0, 8, false).isEmpty());
		assertFalse(tSender.checkTarget(), ":125 — the self-seed is no target");
		assertEquals(POS, tSender.mTargetPos, "the scan leaves the self-seed marker (:133)");
	}

	@Test
	public void stoppedEndpointRefusesEverything() {
		GT6LongDistanceItemPipeBlockEntity tSender = endpoint();
		GT6LongDistanceItemPipeBlockEntity tTarget = new GT6LongDistanceItemPipeBlockEntity(sType, FAR, Blocks.STONE.defaultBlockState());
		wire(tSender, tTarget);
		tSender.mStopped = true; // the soft-hammer channel (:111)
		ItemStack tInput = new ItemStack(Items.IRON_INGOT, 8);
		assertEquals(tInput, tSender.window().insertItem(0, tInput, false), ":111 — the stopped guard");
		assertFalse(tSender.checkTarget());
	}

	// ---------------------------------------------------------------------------
	// 3. the claim protocol (:126 — the first sender keeps the target)
	// ---------------------------------------------------------------------------

	@Test
	public void firstSenderKeepsTheTargetClaim() {
		GT6LongDistanceItemPipeBlockEntity tSenderA = endpoint();
		GT6LongDistanceItemPipeBlockEntity tSenderB = new GT6LongDistanceItemPipeBlockEntity(sType, new BlockPos(10, 4, 6), Blocks.STONE.defaultBlockState());
		GT6LongDistanceItemPipeBlockEntity tTarget = new GT6LongDistanceItemPipeBlockEntity(sType, FAR, Blocks.STONE.defaultBlockState());

		wire(tSenderA, tTarget);
		assertTrue(tSenderA.checkTarget(), "the first claim binds");
		assertEquals(tSenderA, tTarget.mSender, "the target's backlink (:126)");

		// B re-resolves the SAME target off its persisted position (no rig override — the
		// claim protocol must run): the bound sender is alive and valid, B is refused
		tSenderB.mTargetPos = FAR;
		tSenderB.mTarget = tTarget;
		assertFalse(tSenderB.checkTarget(), ":126-127 — a bound sender keeps the target");
	}

	// ---------------------------------------------------------------------------
	// 4. the NBT face (:59-75 — the position persists, the link pair does not)
	// ---------------------------------------------------------------------------

	@Test
	public void nbtCarriesThePositionButNotTheLinkPair() {
		GT6LongDistanceItemPipeBlockEntity tSender = endpoint();
		GT6LongDistanceItemPipeBlockEntity tTarget = new GT6LongDistanceItemPipeBlockEntity(sType, FAR, Blocks.STONE.defaultBlockState());
		wire(tSender, tTarget);
		assertTrue(tSender.checkTarget());
		tSender.mFacing = 4;

		CompoundTag tNBT = tSender.saveWithoutMetadata();
		GT6LongDistanceItemPipeBlockEntity tRevived = endpoint();
		tRevived.load(tNBT);

		assertEquals(FAR, tRevived.mTargetPos, "the persisted position");
		assertNull(tRevived.mTarget, "the link pair is transient (:55)");
		assertNull(tRevived.mSender);
		assertFalse(tRevived.mStopped, "the running state round-trips");
		assertEquals(4, tRevived.mFacing, "the facing byte");
		assertTrue(tNBT.getBoolean(GT6LongDistanceItemPipeBlockEntity.NBT_TARGET), "the live-link flag rides the NBT");
	}
}
