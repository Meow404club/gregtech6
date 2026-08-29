package gregtech6.tileentity.example;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.tileentity.GTItemStackHandler;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * Acceptance 5 (task p3-example-machine): chest NBT round trip — slot contents survive
 * saveAdditional → load, the GTItemStackHandler content-change hook fires on mutation, and
 * the openers counter (mUsingPlayers) drives the :160-168 sync gate like upstream
 * MultiTileEntityChest.java:160-168. Offline fixtures follow the
 * TestMachineBlockEntityNBTTest pattern (vanilla blocks — the registry is frozen after the
 * offline boot; see GTOfflineTestBase).
 */
public class GTExampleChestBlockEntityNBTTest extends GTOfflineTestBase {

	static final BlockPos POS = new BlockPos(4, 5, 6);

	static BlockEntityType<GTExampleChestBlockEntity> sType;

	@BeforeAll
	static void buildOfflineFixtures() {
		@SuppressWarnings("unchecked")
		BlockEntityType<GTExampleChestBlockEntity>[] tHolder = (BlockEntityType<GTExampleChestBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GTExampleChestBlockEntity(tHolder[0], aPos, aState),
				Blocks.STONE).build(null);
		sType = tHolder[0];
	}

	@Test
	public void chestDefaultsMatchTheUpstreamRegistration() {
		GTExampleChestBlockEntity tChest = sType.create(POS, Blocks.STONE.defaultBlockState());
		assertEquals(54, tChest.getInventory().getSlots(), "Loader_MultiTileEntities.java:132 NBT_INV_SIZE, 54");
		assertEquals("example_chest", tChest.getTileEntityName());
		assertTrue(tChest.mIsTicking, "the chest sits on the ticking 03 chain");
		assertEquals(3, tChest.getFacing(), "upstream mFacing default 3 (MultiTileEntityChest.java:84)");
		assertEquals(0, tChest.getUsingPlayers());
	}

	@Test
	public void nbtRoundTripRestoresSlotContentsAndFacing() {
		GTExampleChestBlockEntity tChest = sType.create(POS, Blocks.STONE.defaultBlockState());
		tChest.getInventory().insertItem(0, new ItemStack(Items.DIAMOND, 32), false);
		tChest.getInventory().insertItem(17, new ItemStack(Items.EMERALD, 7), false);
		tChest.getInventory().insertItem(53, new ItemStack(Items.IRON_INGOT, 9), false);
		tChest.mFacing = 5; // EAST, as the placement hook would set

		CompoundTag tSaved = tChest.saveWithoutMetadata();
		assertEquals("example_chest", tSaved.getString("te_name"));
		assertEquals(5, tSaved.getByte(GTExampleChestBlockEntity.NBT_FACING));
		assertTrue(tSaved.contains(GTExampleChestBlockEntity.NBT_INVENTORY, Tag.TAG_COMPOUND));

		GTExampleChestBlockEntity tBack = sType.create(POS, Blocks.STONE.defaultBlockState());
		tBack.load(tSaved);
		assertEquals(32, tBack.getInventory().getStackInSlot(0).getCount());
		assertEquals(Items.EMERALD, tBack.getInventory().getStackInSlot(17).getItem());
		assertEquals(9, tBack.getInventory().getStackInSlot(53).getCount());
		assertEquals(5, tBack.getFacing());
		assertEquals(0, tBack.getUsingPlayers(), "mUsingPlayers is live state, not persisted (upstream writeToNBT2 :108-113)");
	}

	@Test
	public void contentMutationFiresTheHandlerHook() {
		GTExampleChestBlockEntity tChest = sType.create(POS, Blocks.STONE.defaultBlockState());
		// the chest binds this::setChanged at construction; swap the runnable for a counter to
		// observe the GTItemStackHandler hook firing (GTItemStackHandler.java:62-64)
		int[] tFired = {0};
		tChest.getInventory().setOnContentsChanged(() -> tFired[0]++);

		tChest.getInventory().insertItem(0, new ItemStack(Items.GOLD_INGOT, 4), false);
		assertEquals(1, tFired[0], "insertItem fires onContentsChanged");
		tChest.getInventory().extractItem(0, 4, false);
		assertEquals(2, tFired[0], "extractItem fires onContentsChanged");
	}

	@Test
	public void openersCounterDrivesTheSyncGate() {
		GTExampleChestBlockEntity tChest = sType.create(POS, Blocks.STONE.defaultBlockState());

		tChest.openInventoryGUI();
		tChest.openInventoryGUI();
		assertEquals(2, tChest.getUsingPlayers());
		assertTrue(tChest.onTickCheck(0), "mUsingPlayers != oUsingPlayers arms the sync (upstream :160-163)");

		// the dispatcher itself consumes the check: sendClientData + onTickChecked + onTickResetChecks
		for (int i = 0; i < 4; i++) tChest.updateEntity(); // warm up to mTimer > 2
		assertTrue(tChest.getTimer() > 2, "client sync gate open (03 dispatcher mTimer > 2)");
		assertEquals(2, tChest.oUsingPlayers, "dispatcher resetChecks carried mUsingPlayers over");
		assertFalse(tChest.onTickCheck(4), "reset clears the gate (upstream :164-168)");

		tChest.closeInventoryGUI();
		tChest.closeInventoryGUI();
		assertEquals(0, tChest.getUsingPlayers());
		tChest.closeInventoryGUI();
		assertEquals(-1, tChest.getUsingPlayers(), "upstream :253 has no underflow guard — the port keeps it verbatim");
		assertEquals(2, tChest.oUsingPlayers, "oUsingPlayers only moves in onTickResetChecks");
	}

	@Test
	public void updateTagCarriesTheChestStateBothChannels() {
		GTExampleChestBlockEntity tChest = sType.create(POS, Blocks.STONE.defaultBlockState());
		tChest.mFacing = 4;

		CompoundTag tUpdateTag = tChest.getUpdateTag(); // getUpdateTag = saveWithoutMetadata (03 port)
		assertEquals("example_chest", tUpdateTag.getString("te_name"));
		assertEquals(4, tUpdateTag.getByte(GTExampleChestBlockEntity.NBT_FACING));

		GTExampleChestBlockEntity tClient = sType.create(POS, Blocks.STONE.defaultBlockState());
		tClient.handleUpdateTag(tUpdateTag); // IForgeBlockEntity :68 default = load(tag)
		assertEquals(4, tClient.getFacing());
	}

	@Test
	public void handlerFilterAcceptsEverythingLikeUpstreamIsItemValidForSlot() {
		GTItemStackHandler tInventory = new GTItemStackHandler(GTExampleChestBlockEntity.INVENTORY_SIZE, () -> {});
		// MultiTileEntityChest → isItemValidForSlot = T (TileEntityBase05Inventories.java:116) — no filter
		assertTrue(tInventory.isItemValid(0, new ItemStack(Items.DIAMOND_BLOCK)));
	}
}
