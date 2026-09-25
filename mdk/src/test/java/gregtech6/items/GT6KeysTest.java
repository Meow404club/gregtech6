package gregtech6.items;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.tileentity.inventories.GT6SafeKeyLockedBlockEntity;

/**
 * The key family offline pins (task dungeon-keys): the ten-row pool census in the
 * IL.KEYS order (IL.java:516 — the dungeon draw table WorldgenDungeonGT.java:173 walks),
 * the Behavior_Key use face over the ported lock (claim :53-57 / toggle :51 / bounce /
 * clone :58-61), the dungeon-stack rename+NBT (WorldgenDungeonGT.java:173), and the
 * fresh-id roll (Behavior_Key.java:54). The worldgen hiding pins live with the
 * structure tests.
 */
public class GT6KeysTest extends GTOfflineTestBase {

	static BlockEntityType<GT6SafeKeyLockedBlockEntity> sKeySafeType;
	static final BlockPos POS = new BlockPos(2, 3, 4);

	@BeforeAll
	static void buildOfflineFixtures() {
		sKeySafeType = fixture(GT6SafeKeyLockedBlockEntity::new);
	}

	private static <T extends GT6SafeKeyLockedBlockEntity> BlockEntityType<T> fixture(Factory<T> aFactory) {
		@SuppressWarnings("unchecked")
		BlockEntityType<T>[] tHolder = (BlockEntityType<T>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> aFactory.create(tHolder[0], aPos, aState), Blocks.STONE).build(null);
		return tHolder[0];
	}

	private interface Factory<T extends GT6SafeKeyLockedBlockEntity> {
		T create(BlockEntityType<?> aType, BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState);
	}

	private static GT6SafeKeyLockedBlockEntity newLock() {
		return new GT6SafeKeyLockedBlockEntity(sKeySafeType, POS, Blocks.STONE.defaultBlockState());
	}

	// ---------------------------------------------------------------------------
	// the pool census (IL.java:516 order = the draw table)
	// ---------------------------------------------------------------------------

	@Test
	public void thePoolShipsTenKeysInTheIlKeysOrder() {
		assertEquals(10, GT6Keys.KEYS.size());
		String[] tPaths = new String[GT6Keys.KEYS.size()];
		for (int i = 0; i < GT6Keys.KEYS.size(); i++) tPaths[i] = GT6Keys.KEYS.get(i).getId().getPath();
		// IL.java:516 verbatim order — the dungeon draw semantics ride the order.
		assertArrayEquals(new String[] {"key_brass", "key_bronze", "key_copper", "key_gold", "key_iron",
				"key_lead", "key_plastic", "key_platinum", "key_silver", "key_tin"}, tPaths);
	}

	@Test
	public void theRegistryRowsMatchThePool() {
		// the registration face offline: the DeferredRegister holds the NAMES (the holders
		// populate at the mod-construct event, unreachable in this JVM — the tab-census
		// offline discipline). The pool rows are the same RegistryObjects, so the name walk
		// IS the row census.
		assertEquals(10, GT6Keys.ITEMS.getEntries().size());
		assertEquals("key_brass", GT6Keys.KEYS.get(0).getId().getPath());
		assertEquals("key_tin", GT6Keys.KEYS.get(9).getId().getPath());
	}

	// ---------------------------------------------------------------------------
	// the id carrier
	// ---------------------------------------------------------------------------

	@Test
	public void theIdRidesTheGtKeyTagAndBlankReadsZero() {
		ItemStack tStack = new ItemStack(Items.STICK);
		assertEquals(0, GT6Keys.keyIdOf(tStack));
		GT6Keys.setKeyId(tStack, 42L);
		assertEquals(42L, GT6Keys.keyIdOf(tStack));
		CompoundTag tTag = tStack.getTag();
		assertTrue(tTag.contains("gt.key", Tag.TAG_ANY_NUMERIC)); // the CS.NBT_KEY pair with the lock BE
	}

	@Test
	public void theFreshIdRollIsPositiveAndUnique() {
		long tFirst = GT6Keys.newKeyId();
		long tSecond = GT6Keys.newKeyId();
		assertTrue(tFirst > 0); // Behavior_Key.java:54 1+max(...) shape
		assertNotEquals(tFirst, tSecond);
	}

	// ---------------------------------------------------------------------------
	// the Behavior_Key use face (Behavior_Key.java:44-63 over the ported lock)
	// ---------------------------------------------------------------------------

	@Test
	public void aKeyedStackTogglesTheMatchingLock() {
		GT6SafeKeyLockedBlockEntity tLock = newLock();
		ItemStack tKey = new ItemStack(Items.STICK);
		GT6Keys.setKeyId(tKey, 7L);
		assertFalse(tLock.mOpened);
		assertTrue(GT6Keys.useOnKeyLocked(tLock, tKey));
		assertTrue(tLock.mOpened);
		assertEquals(7L, tLock.mID);
		assertTrue(GT6Keys.useOnKeyLocked(tLock, tKey));
		assertFalse(tLock.mOpened);
	}

	@Test
	public void aBlankKeyClaimsAnUnclaimedLock() {
		GT6SafeKeyLockedBlockEntity tLock = newLock();
		ItemStack tKey = new ItemStack(Items.STICK);
		assertEquals(0, tLock.mID);
		assertTrue(GT6Keys.useOnKeyLocked(tLock, tKey));
		long tClaimed = GT6Keys.keyIdOf(tKey);
		assertTrue(tClaimed != 0); // the key left WITH an id (Behavior_Key.java:55)
		assertEquals(tClaimed, tLock.mID);
		assertTrue(tLock.mOpened); // the claim plays the toggle
	}

	@Test
	public void aWrongIdBouncesAndLeavesTheLatchAlone() {
		GT6SafeKeyLockedBlockEntity tLock = newLock();
		tLock.mID = 5L;
		ItemStack tKey = new ItemStack(Items.STICK);
		GT6Keys.setKeyId(tKey, 6L);
		assertFalse(GT6Keys.useOnKeyLocked(tLock, tKey));
		assertFalse(tLock.mOpened);
		assertEquals(5L, tLock.mID);
	}

	@Test
	public void aBlankKeyClonesTheIdOfAnOpenClaimedLock() {
		GT6SafeKeyLockedBlockEntity tLock = newLock();
		tLock.mID = 9L;
		tLock.mOpened = true;
		ItemStack tKey = new ItemStack(Items.STICK);
		assertTrue(GT6Keys.useOnKeyLocked(tLock, tKey));
		assertEquals(9L, GT6Keys.keyIdOf(tKey)); // the canCloneKey :94-96 copy
		assertTrue(tLock.mOpened); // the clone does NOT flip
		// the closed gate: the same blank on a closed claimed lock bounces
		GT6SafeKeyLockedBlockEntity tShut = newLock();
		tShut.mID = 9L;
		ItemStack tBlank = new ItemStack(Items.STICK);
		assertFalse(GT6Keys.useOnKeyLocked(tShut, tBlank));
		assertEquals(0L, GT6Keys.keyIdOf(tBlank));
	}

	// ---------------------------------------------------------------------------
	// the dungeon stack (WorldgenDungeonGT.java:173 getWithNameAndNBT port)
	// ---------------------------------------------------------------------------

	@Test
	public void theDungeonStackCarriesTheIdAndTheKeyNName() {
		ItemStack tStack = GT6Keys.dungeonStack(Items.STICK, 1, 1234567890123L);
		assertEquals(1234567890123L, GT6Keys.keyIdOf(tStack));
		assertEquals("Key #2", tStack.getHoverName().getString()); // the "Key #"+(i+1) rename
		assertEquals(1, tStack.getCount());
	}
}
