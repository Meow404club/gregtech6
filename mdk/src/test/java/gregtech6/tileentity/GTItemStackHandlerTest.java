package gregtech6.tileentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import org.junit.jupiter.api.Test;

/**
 * GTItemStackHandler contract: content-change hook on every mutating path
 * (Forge ItemStackHandler setStackInSlot :45 / insertItem :101 / extractItem :128/:141)
 * and per-slot insert filter (same shape as GTCEu CustomItemStackHandler.java:26,:45-47).
 */
public class GTItemStackHandlerTest extends GTOfflineTestBase {

	@Test
	public void contentChangeHookFiresOnEveryMutatingPath() {
		int[] tCalls = {0};
		GTItemStackHandler tHandler = new GTItemStackHandler(2, () -> tCalls[0]++);

		tHandler.setStackInSlot(0, new ItemStack(Items.DIAMOND, 1));
		assertEquals(1, tCalls[0]);

		ItemStack tRest = tHandler.insertItem(1, new ItemStack(Items.EMERALD, 5), false);
		assertTrue(tRest.isEmpty());
		assertEquals(2, tCalls[0]);

		tHandler.extractItem(1, 5, false);
		assertEquals(3, tCalls[0]);

		// simulate must not mutate nor fire the hook
		tHandler.extractItem(0, 1, true);
		assertEquals(3, tCalls[0]);
	}

	@Test
	public void insertFilterRejectsForeignItems() {
		GTItemStackHandler tHandler = new GTItemStackHandler(1, () -> {}, stack -> stack.getItem() == Items.DIAMOND);

		ItemStack tRejected = tHandler.insertItem(0, new ItemStack(Items.EMERALD, 5), false);
		assertEquals(5, tRejected.getCount());
		assertTrue(tHandler.getStackInSlot(0).isEmpty());
		assertFalse(tHandler.isItemValid(0, new ItemStack(Items.EMERALD, 1)));

		ItemStack tAccepted = tHandler.insertItem(0, new ItemStack(Items.DIAMOND, 5), false);
		assertTrue(tAccepted.isEmpty());
		assertEquals(5, tHandler.getStackInSlot(0).getCount());
		assertTrue(tHandler.isItemValid(0, new ItemStack(Items.DIAMOND, 1)));
	}

	@Test
	public void nbtRoundTripPreservesSlots() {
		GTItemStackHandler tHandler = new GTItemStackHandler(4);
		tHandler.setStackInSlot(1, new ItemStack(Items.DIAMOND, 32));
		tHandler.setStackInSlot(3, new ItemStack(Items.EMERALD, 7));

		//? if forge {
		CompoundTag tTag = tHandler.serializeNBT();
		//?} else {
		/*CompoundTag tTag = tHandler.serializeNBT(gregtech6.tileentity.TileEntityBase03TicksAndSync.NBT_ACCESS); // 21.1: ItemStackHandler NBT takes the registries
		*///?}
		GTItemStackHandler tBack = new GTItemStackHandler(4);
		//? if forge {
		tBack.deserializeNBT(tTag);
		//?} else {
		/*tBack.deserializeNBT(gregtech6.tileentity.TileEntityBase03TicksAndSync.NBT_ACCESS, tTag); // 21.1: provider-first
		*///?}

		assertEquals(0, tBack.getStackInSlot(0).getCount());
		assertEquals(32, tBack.getStackInSlot(1).getCount());
		assertEquals(Items.DIAMOND, tBack.getStackInSlot(1).getItem());
		assertEquals(7, tBack.getStackInSlot(3).getCount());
		assertTrue(tBack.getStackInSlot(2).isEmpty());
		assertTrue(tTag.contains("Size", Tag.TAG_INT));
	}
}
