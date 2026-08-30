package gregtech6.tileentity.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import org.junit.jupiter.api.Test;

/**
 * Acceptance 1 (task p4-machine-oven): the machine NBT round trip — mProgress/mEnergy/
 * mStopped (the acceptance minimum) plus mMaxProgress/mMinEnergy/mIgnited, the facing byte,
 * the inventory and the pending output stack, upstream readFromNBT2 :112-155 key-for-key
 * (plain in-repo key form, chest precedent).
 */
public class TileEntityOvenNBTTest extends GTMachinesOfflineTestBase {

	@Test
	public void nbtRoundTripPreservesMachineStateInventoryAndFacing() {
		TileEntityOven tOven = makeOven(smeltingLevel());
		tOven.getInventory().setStackInSlot(TileEntityOven.SLOT_INPUT, new ItemStack(Items.SAND, 3));
		tOven.getInventory().setStackInSlot(TileEntityOven.SLOT_OUTPUT, new ItemStack(Items.GLASS));
		tOven.getInventory().setStackInSlot(TileEntityOven.SLOT_SPECIAL, new ItemStack(Items.GOLD_NUGGET));
		tOven.mEnergy = 123;
		tOven.mMinEnergy = 16;
		tOven.mProgress = 77;
		tOven.mMaxProgress = 256;
		tOven.mStopped = true;
		tOven.mIgnited = 12;
		tOven.mActive = true;
		tOven.mRunning = true;
		tOven.mFacing = 5; // EAST in the GT6 side order

		CompoundTag tTag = tOven.saveWithoutMetadata();

		TileEntityOven tRestored = sOvenType.create(POS2, Blocks.BRICKS.defaultBlockState());
		tRestored.setLevel(tOven.getLevel());
		tRestored.load(tTag);

		assertEquals(123, tRestored.mEnergy, "NBT_ENERGY :115");
		assertEquals(16, tRestored.mMinEnergy, "NBT_MINENERGY :129");
		assertEquals(77, tRestored.mProgress, "NBT_PROGRESS :133");
		assertEquals(256, tRestored.mMaxProgress, "NBT_MAXPROGRESS :134");
		assertTrue(tRestored.mStopped, "NBT_STOPPED :117");
		assertEquals(12, tRestored.mIgnited, "NBT_IGNITION :136");
		assertTrue(tRestored.mActive, "NBT_ACTIVE :116");
		assertTrue(tRestored.mRunning, "NBT_RUNNING :118");
		assertEquals(5, tRestored.getFacing(), "NBT_FACING :124");

		assertEquals(Items.SAND, tRestored.getInventory().getStackInSlot(TileEntityOven.SLOT_INPUT).getItem());
		assertEquals(3, tRestored.getInventory().getStackInSlot(TileEntityOven.SLOT_INPUT).getCount());
		assertEquals(Items.GLASS, tRestored.getInventory().getStackInSlot(TileEntityOven.SLOT_OUTPUT).getItem());
		assertEquals(Items.GOLD_NUGGET, tRestored.getInventory().getStackInSlot(TileEntityOven.SLOT_SPECIAL).getItem());
	}

	@Test
	public void nbtRoundTripPreservesPendingOutputs() {
		TileEntityOven tOven = makeOven(emptyLevel());
		// a blocked output keeps its pending stack in mOutputItems (upstream NBT_INV_OUT :166-167)
		tOven.getInventory().setStackInSlot(TileEntityOven.SLOT_OUTPUT, new ItemStack(Items.GLASS, 64));
		tOven.mOutputItems = new ItemStack[] {new ItemStack(Items.BRICK, 2)};
		tOven.mProgress = 256;
		tOven.mMaxProgress = 256;

		CompoundTag tTag = tOven.saveWithoutMetadata();
		TileEntityOven tRestored = sOvenType.create(POS2, Blocks.BRICKS.defaultBlockState());
		tRestored.load(tTag);

		assertEquals(1, tRestored.mOutputItems.length, "the pending output persisted");
		assertEquals(Items.BRICK, tRestored.mOutputItems[0].getItem());
		assertEquals(2, tRestored.mOutputItems[0].getCount());
		assertEquals(256, tRestored.mProgress, "the parked progress persisted (blocked output)");
	}

	@Test
	public void freshMachineLoadsToIdleDefaults() {
		TileEntityOven tRestored = sOvenType.create(new BlockPos(9, 2, 3), Blocks.BRICKS.defaultBlockState());
		tRestored.load(new CompoundTag());
		assertEquals(0, tRestored.mEnergy);
		assertEquals(0, tRestored.mProgress);
		assertEquals(0, tRestored.mMaxProgress);
		assertEquals(16, tRestored.mInputMin, "upstream :98 defaults");
		assertEquals(32, tRestored.mInput);
		assertEquals(64, tRestored.mInputMax);
		assertTrue(tRestored.mOutputItems.length == 0, "no pending outputs");
		assertEquals(2, tRestored.getFacing(), "GT6 side order 2 = NORTH");
	}
}
