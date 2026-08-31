package gregtech6.tileentity.machines;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.junit.jupiter.api.Test;

import gregtech6.recipes.GT6RecipeMaps;

/**
 * Acceptance 1 (task p7-basicmachine-family ⑦): the NBT round trip — the plain-key oven
 * contract (TileEntityOven.java:111-121) over the machine field set, facing/active/running
 * persistence, and the pending-output list.
 *
 * <p>The literal BlockState assertion is not possible offline (a real GTBasicMachineBlock
 * cannot be constructed post-bootstrap and the stub level has no chunk source — the oven
 * FacingTest lesson); the NBT half of the spec-7 double write is asserted here, the
 * setBlock(state, 3) half is covered live by the RCON chain.
 */
public class TileEntityBasicMachineNBTTest extends TileEntityBasicMachineOfflineTestBase {

	@Test
	void roundTripPersistsTheMachineState() {
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.SHREDDER, 1, false);
		tMachine.mEnergy = 37;
		tMachine.mMinEnergy = 16;
		tMachine.mProgress = 120;
		tMachine.mMaxProgress = 256;
		tMachine.mStopped = true;
		tMachine.mIgnited = 33;
		tMachine.mActive = true;
		tMachine.mRunning = true;
		tMachine.mFacing = 4;
		tMachine.getInventory().setStackInSlot(TileEntityBasicMachine.SLOT_INPUT, new ItemStack(Items.GRAVEL, 5));
		tMachine.getInventory().setStackInSlot(1, new ItemStack(Items.SAND, 9));
		tMachine.mOutputItems = new ItemStack[] {new ItemStack(Items.STRING, 2)};

		CompoundTag tTag = new CompoundTag();
		tMachine.saveAdditional(tTag);

		// the plain-key oven contract (upstream readFromNBT2 :112-155 names)
		assertTrue(tTag.contains(TileEntityBasicMachine.NBT_FACING, Tag.TAG_ANY_NUMERIC));
		assertTrue(tTag.contains(TileEntityBasicMachine.NBT_INVENTORY, Tag.TAG_COMPOUND));
		assertTrue(tTag.contains(TileEntityBasicMachine.NBT_ENERGY, Tag.TAG_ANY_NUMERIC));
		assertTrue(tTag.contains(TileEntityBasicMachine.NBT_MINENERGY, Tag.TAG_ANY_NUMERIC));
		assertTrue(tTag.contains(TileEntityBasicMachine.NBT_PROGRESS, Tag.TAG_ANY_NUMERIC));
		assertTrue(tTag.contains(TileEntityBasicMachine.NBT_MAXPROGRESS, Tag.TAG_ANY_NUMERIC));
		assertTrue(tTag.contains(TileEntityBasicMachine.NBT_STOPPED));
		assertTrue(tTag.contains(TileEntityBasicMachine.NBT_IGNITED, Tag.TAG_ANY_NUMERIC));
		assertTrue(tTag.contains(TileEntityBasicMachine.NBT_ACTIVE));
		assertTrue(tTag.contains(TileEntityBasicMachine.NBT_RUNNING));
		assertTrue(tTag.contains(TileEntityBasicMachine.NBT_OUTPUT, Tag.TAG_LIST));

		TileEntityBasicMachine tRestored = makeMachine(GT6RecipeMaps.SHREDDER, 1, false);
		tRestored.load(tTag);

		assertEquals(37, tRestored.mEnergy);
		assertEquals(16, tRestored.mMinEnergy);
		assertEquals(120, tRestored.mProgress);
		assertEquals(256, tRestored.mMaxProgress);
		assertTrue(tRestored.mStopped);
		assertEquals(33, tRestored.mIgnited);
		assertTrue(tRestored.mActive);
		assertTrue(tRestored.mRunning);
		assertEquals(4, tRestored.getFacing(), "the NBT half of the spec-7 double write");
		assertEquals(5, tRestored.getInventory().getStackInSlot(TileEntityBasicMachine.SLOT_INPUT).getCount());
		assertEquals(Items.SAND, tRestored.getInventory().getStackInSlot(1).getItem());
		assertEquals(1, tRestored.mOutputItems.length, "the pending-output list survives");
		assertEquals(Items.STRING, tRestored.mOutputItems[0].getItem());
		assertEquals(2, tRestored.mOutputItems[0].getCount());
	}

	@Test
	void loadWithoutKeysKeepsTheDefaults() {
		TileEntityBasicMachine tMachine = makeMachine(GT6RecipeMaps.LATHE, 1, false);
		tMachine.load(new CompoundTag()); // :112-155 hasKey guards — an empty tag is a no-op
		assertEquals(0, tMachine.mEnergy, ":98 defaults");
		assertEquals(0, tMachine.mProgress);
		assertEquals(0, tMachine.mMaxProgress);
		assertFalse(tMachine.mStopped);
		assertEquals(0, tMachine.mIgnited);
		assertEquals(2, tMachine.getFacing(), "the chest-precedent default facing");
	}

	@Test
	void loadKeepsTheConstructorInjectedConfig() {
		// the registration config (upstream NBT make(...) :1294-1309) is constructor-injected,
		// NOT persisted — the load path cannot override it
		TileEntityBasicMachine tCrusher = makeMachine(GT6RecipeMaps.CRUSHER, 4, true);
		tCrusher.load(new CompoundTag());
		assertEquals(4, tCrusher.mParallel);
		assertTrue(tCrusher.mParallelDuration);
		assertEquals(13, tCrusher.getInventory().getSlots(), "1 input + 12 outputs (getDefaultInventory :526)");
	}
}
