package gregtech6.tileentity.bees;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

import gregtech6.client.render.GTModelProperties;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The Bumble Hive BE acceptance (task p32-bees-lv2, the GTPaintableTest oven-fixture
 * posture): the paintable stratum on the hive carrier (the born-painted worldgen face,
 * WorldgenHives.java:203), the 9-slot inventory under the upstream {@code gt.inv} key
 * (MultiTileEntityBumbleHive.java:99 + CS.NBT_INV_LIST), and the PAINT ModelData supply
 * the client tint resolves.
 *
 * <p>Offline-safe: the BET-over-vanilla-BRICKS fixture — no registry reads, no bootstrapped
 * GT6 blocks.
 */
class GT6BumbleHiveBlockEntityTest {

	static BlockEntityType<GT6BumbleHiveBlockEntity> sHiveType;

	static final BlockPos POS = new BlockPos(1, 2, 3);

	@BeforeAll
	static void boot() {
		// the vanilla bootstrap bracket (the GTOfflineTestBase shape: tryDetectVersion
		// first, then the boot; the BET-unfreeze face rides the base)
		net.minecraft.SharedConstants.tryDetectVersion();
		try {
			net.minecraft.server.Bootstrap.bootStrap();
		} catch (Throwable ignored) {
		}
		gregtech6.tileentity.GTOfflineTestBase.unfreezeBlockEntityTypeRegistry();
		@SuppressWarnings("unchecked")
		BlockEntityType<GT6BumbleHiveBlockEntity>[] tHolder = (BlockEntityType<GT6BumbleHiveBlockEntity>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6BumbleHiveBlockEntity(tHolder[0], aPos, aState),
				Blocks.BRICKS).build(null);
		sHiveType = tHolder[0];
	}

	static GT6BumbleHiveBlockEntity hive() {
		return new GT6BumbleHiveBlockEntity(sHiveType, POS, Blocks.BRICKS.defaultBlockState());
	}

	@Test
	void hiveNameAndSlotsMatchUpstream() {
		GT6BumbleHiveBlockEntity tHive = hive();
		assertEquals("gt.multitileentity.bumble.hive", tHive.getTileEntityName(), "MultiTileEntityBumbleHive.java:101 verbatim");
		assertEquals(9, tHive.inventory().getSlots(), "getDefaultInventory = new ItemStack[9] (:99)");
		for (int i = 0; i < 9; i++) {
			assertTrue(tHive.inventory().getStackInSlot(i).isEmpty(), "fresh hive slots all empty");
		}
	}

	@Test
	void worldgenPaintFaceStoresAndRidesTheFullSave() {
		GT6BumbleHiveBlockEntity tHive = hive();
		assertFalse(tHive.isPainted(), "freshly constructed = unpainted");
		// the placeHive born-painted face (WorldgenHives.java:203 NBT_COLOR+NBT_PAINTED)
		assertTrue(tHive.paint(0xC0C0C0), "the family colour stores (DYE_INT_LightGray, :135)");
		assertTrue(tHive.isPainted());
		assertEquals(0xC0C0C0, tHive.getPaint());

		CompoundTag tSaved = tHive.saveWithoutMetadata();
		assertTrue(tSaved.contains(TileEntityBase03TicksAndSync.NBT_COLOR, Tag.TAG_INT), "gt.color rides the save");
		assertTrue(tSaved.contains(TileEntityBase03TicksAndSync.NBT_PAINTED, Tag.TAG_BYTE), "gt.painted rides the save");
		assertEquals(0xC0C0C0, tSaved.getInt(TileEntityBase03TicksAndSync.NBT_COLOR));

		GT6BumbleHiveBlockEntity tReloaded = hive();
		tReloaded.load(tSaved);
		assertTrue(tReloaded.isPainted());
		assertEquals(0xC0C0C0, tReloaded.getPaint());
	}

	@Test
	void inventoryRidesTheGtInvKeyRoundTrip() {
		GT6BumbleHiveBlockEntity tHive = hive();
		tHive.inventory().setStackInSlot(0, new ItemStack(Items.HONEYCOMB, 4)); // slot 0 = the comb
		tHive.inventory().setStackInSlot(8, new ItemStack(Items.HONEYCOMB, 1)); // a later slot too

		CompoundTag tSaved = tHive.saveWithoutMetadata();
		assertTrue(tSaved.contains(GT6BumbleHiveBlockEntity.NBT_INV_LIST, Tag.TAG_COMPOUND), "gt.inv rides the save");
		CompoundTag tInv = tSaved.getCompound(GT6BumbleHiveBlockEntity.NBT_INV_LIST);
		assertEquals(2, tInv.getList("Items", Tag.TAG_COMPOUND).size(), "only the non-empty slots serialize");

		GT6BumbleHiveBlockEntity tReloaded = hive();
		tReloaded.load(tSaved);
		assertEquals(4, tReloaded.inventory().getStackInSlot(0).getCount(), "the slot-0 comb rehydrates");
		assertEquals(1, tReloaded.inventory().getStackInSlot(8).getCount(), "the slot-8 comb rehydrates");
		assertTrue(tReloaded.inventory().getStackInSlot(4).isEmpty(), "the untouched slot stays empty");
	}

	@Test
	void paintedHiveSuppliesThePaintModelData() {
		GT6BumbleHiveBlockEntity tHive = hive();
		assertFalse(tHive.getModelData().has(GTModelProperties.PAINT), "unpainted = no PAINT property");

		tHive.paint(0xAA0000); // the nether family colour (:111)
		assertEquals(0xAA0000, (int) tHive.getModelData().get(GTModelProperties.PAINT),
				"painted = the PAINT property the GTMachinePaintTint lambda resolves");
	}
}
