package gregtech6.tileentity.inventories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.registry.GT6StaticStorages;
import gregtech6.registry.GT6StaticStorages.Kind;
import gregtech6.registry.GT6StaticStorages.StaticRow;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * GT6 static storage batch offline tests (task p26-storage-static-batch acceptance): the
 * DrawerQuad quadrant index math (the verbatim table over the FACING_ROTATIONS geometry),
 * the Locker armor gate (upstream :84), the Safe 15-slot + dungeon-loot seam + the
 * KeyLocked latch, the bookshelf/bottlecrate range arms and gates, the NBT round trips
 * and the 28-row meta-id census. The live container arms are the RCON chain's.
 */
public class GT6StaticStorageBatchTest extends GTOfflineTestBase {

	static BlockEntityType<GT6LockerBlockEntity> sLockerType;
	static BlockEntityType<GT6DrawerQuadBlockEntity> sDrawerType;
	static BlockEntityType<GT6SafeBlockEntity> sSafeType;
	static BlockEntityType<GT6SafeKeyLockedBlockEntity> sKeySafeType;
	static BlockEntityType<GT6BookShelfBlockEntity> sShelfType;
	static BlockEntityType<GT6BottleCrateBlockEntity> sCrateType;
	static final BlockPos POS = new BlockPos(2, 3, 4);

	@BeforeAll
	static void buildOfflineFixtures() {
		sLockerType = fixture(GT6LockerBlockEntity::new);
		sDrawerType = fixture(GT6DrawerQuadBlockEntity::new);
		sSafeType = fixture(GT6SafeBlockEntity::new);
		sKeySafeType = fixture(GT6SafeKeyLockedBlockEntity::new);
		sShelfType = fixture(GT6BookShelfBlockEntity::new);
		sCrateType = fixture(GT6BottleCrateBlockEntity::new);
	}

	private static <T extends GT6StaticStorageBaseBlockEntity> BlockEntityType<T> fixture(Factory<T> aFactory) {
		@SuppressWarnings("unchecked")
		BlockEntityType<T>[] tHolder = (BlockEntityType<T>[]) new BlockEntityType<?>[1];
		tHolder[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> aFactory.create(tHolder[0], aPos, aState), Blocks.STONE).build(null);
		return tHolder[0];
	}

	private interface Factory<T extends GT6StaticStorageBaseBlockEntity> {
		T create(BlockEntityType<?> aType, BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState);
	}

	// ---------------------------------------------------------------------------
	// the row census (the meta-id columns verbatim, Loader :134-144/:177-180)
	// ---------------------------------------------------------------------------

	@Test
	public void rowCensusReproducesTheLoaderColumns() {
		assertEquals(28, GT6StaticStorages.ROWS.size());
		StaticRow tLockerBronze = row("locker_bronze"), tLockerSteel = row("locker_steel");
		assertEquals(7309, tLockerBronze.metaId()); // :138 7300+aID, Bronze :191 aID 9
		assertEquals(7310, tLockerSteel.metaId()); // Steel :202 aID 10
		assertEquals(4009, row("drawer_bronze").metaId()); // :140 4000+aID
		assertEquals(4010, row("drawer_steel").metaId());
		assertEquals(2009, row("safe_mechanical_bronze").metaId()); // :134 2000+aID
		assertEquals(3009, row("safe_keylocked_bronze").metaId()); // :135 3000+aID
		// the blast-resistance column = aHardness*2 (Bronze 7.0 -> 14.0, Steel 6.0 -> 12.0)
		assertEquals(14.0F, row("safe_mechanical_bronze").resistance(), 1e-6F);
		assertEquals(14.0F, row("safe_keylocked_bronze").resistance(), 1e-6F);
		assertEquals(12.0F, row("safe_mechanical_steel").resistance(), 1e-6F);
		// the metal ladder hardness column
		assertEquals(7.0F, tLockerBronze.hardness(), 1e-6F);
		assertEquals(6.0F, tLockerSteel.hardness(), 1e-6F);
		// the wooden subset ladders: 7000+i / 8700+i over the vanilla planks, crate 0.5/2.0
		assertEquals(7000, row("bookshelf_oak").metaId());
		assertEquals(7009, row("bookshelf_warped").metaId());
		assertEquals(8700, row("bottlecrate_oak").metaId());
		assertEquals(8709, row("bottlecrate_warped").metaId());
		assertEquals(2.0F, row("bookshelf_oak").hardness(), 1e-6F);
		assertEquals(0.5F, row("bottlecrate_oak").hardness(), 1e-6F);
		assertEquals(2.0F, row("bottlecrate_oak").resistance(), 1e-6F);
	}

	private static StaticRow row(String aPath) {
		for (StaticRow tRow : GT6StaticStorages.ROWS) {
			if (tRow.path().equals(aPath)) return tRow;
		}
		throw new AssertionError("no row " + aPath);
	}

	// ---------------------------------------------------------------------------
	// the DrawerQuad quadrant math (upstream :88/:103-104/:106-114)
	// ---------------------------------------------------------------------------

	@Test
	public void drawerQuadrantMathIsTheVerbatimTable() {
		// quadrantBase = (aGUIID % 4) * 36 (:103-104)
		for (int q = 0; q < 4; q++) {
			assertEquals(q * 36, GT6DrawerQuadBlockEntity.quadrantBase(q));
		}
		// the face-quadrant pick (:88 verbatim): (x > 0.5 ? 1 : 0) | (y > 0.5 ? 2 : 0)
		assertEquals(0, GT6DrawerQuadBlockEntity.quadrantOfFace(0.25F, 0.25F));
		assertEquals(1, GT6DrawerQuadBlockEntity.quadrantOfFace(0.75F, 0.25F));
		assertEquals(2, GT6DrawerQuadBlockEntity.quadrantOfFace(0.25F, 0.75F));
		assertEquals(3, GT6DrawerQuadBlockEntity.quadrantOfFace(0.75F, 0.75F));
		// the literal table compositions (:106-114)
		assertEquals(72, GT6DrawerQuadBlockEntity.TOP_HALF.length);
		assertEquals(72, GT6DrawerQuadBlockEntity.BOTTOM_HALF.length);
		assertEquals(72, GT6DrawerQuadBlockEntity.LEFT_HALF.length);
		assertEquals(72, GT6DrawerQuadBlockEntity.RIGHT_HALF.length);
		assertEquals(0, GT6DrawerQuadBlockEntity.TOP_HALF[0]); // top half starts at 0
		assertEquals(71, GT6DrawerQuadBlockEntity.TOP_HALF[71]);
		assertEquals(72, GT6DrawerQuadBlockEntity.BOTTOM_HALF[0]); // bottom half starts at 72
		assertEquals(143, GT6DrawerQuadBlockEntity.BOTTOM_HALF[71]); // bottom half ends at 143
		assertEquals(72, GT6DrawerQuadBlockEntity.LEFT_HALF[36]); // left column's second span starts at 72
		assertEquals(108, GT6DrawerQuadBlockEntity.RIGHT_HALF[36]); // right column's second span starts at 108
		assertEquals(144, GT6DrawerQuadBlockEntity.ALL.length);
	}

	@Test
	public void drawerSidedAccessAnywheresDefaultSeesAll() {
		GT6DrawerQuadBlockEntity tDrawer = new GT6DrawerQuadBlockEntity(sDrawerType, POS, Blocks.STONE.defaultBlockState());
		// Anywhere (default, :94-98 toggle state): every side sees all 144
		assertFalse(tDrawer.sidedAccess());
		for (byte tSide = 0; tSide < 7; tSide++) {
			org.junit.jupiter.api.Assertions.assertArrayEquals(GT6DrawerQuadBlockEntity.ALL,
					tDrawer.getAccessibleSlotsFromSide(tSide));
		}
	}

	@Test
	public void drawerSidedAccessGeometryPinsTheFACING_ROTATIONSDecode() {
		GT6DrawerQuadBlockEntity tDrawer = new GT6DrawerQuadBlockEntity(sDrawerType, POS, Blocks.STONE.defaultBlockState());
		tDrawer.monkeyWrench();
		assertTrue(tDrawer.sidedAccess());
		// all four horizontal facings, every pin decoded row by row from the upstream
		// byte table (CS.java:528-537; the left column = the SLOTS[2]-shaped {q0,q2}
		// column, the right = SLOTS[4]-shaped {q1,q3}). The E/W rows were the review
		// reject: FACING_ROTATIONS[4] (west) maps world north->left, [5] (east) maps
		// world south->left — a flipped pair sidesteps every N/S-only pin, hence the
		// four-orientation coverage.
		for (Direction tFacing : new Direction[] {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
			tDrawer.setFacingNbtFallback((byte) tFacing.get3DDataValue());
			// the Y arms are facing-independent (the physical halves)
			org.junit.jupiter.api.Assertions.assertArrayEquals(GT6DrawerQuadBlockEntity.TOP_HALF,
					tDrawer.getAccessibleSlotsFromSide((byte) Direction.UP.get3DDataValue()));
			org.junit.jupiter.api.Assertions.assertArrayEquals(GT6DrawerQuadBlockEntity.BOTTOM_HALF,
					tDrawer.getAccessibleSlotsFromSide((byte) Direction.DOWN.get3DDataValue()));
			// front and back keep the full set (the GUI face and its opposite)
			org.junit.jupiter.api.Assertions.assertArrayEquals(GT6DrawerQuadBlockEntity.ALL,
					tDrawer.getAccessibleSlotsFromSide((byte) tFacing.get3DDataValue()));
			org.junit.jupiter.api.Assertions.assertArrayEquals(GT6DrawerQuadBlockEntity.ALL,
					tDrawer.getAccessibleSlotsFromSide((byte) tFacing.getOpposite().get3DDataValue()));
			// the columns: the viewer-left side sees {q0,q2}, the viewer-right {q1,q3}
			org.junit.jupiter.api.Assertions.assertArrayEquals(GT6DrawerQuadBlockEntity.LEFT_HALF,
					tDrawer.getAccessibleSlotsFromSide((byte) GT6DrawerQuadBlockEntity.viewerLeftOf(tFacing).get3DDataValue()));
			org.junit.jupiter.api.Assertions.assertArrayEquals(GT6DrawerQuadBlockEntity.RIGHT_HALF,
					tDrawer.getAccessibleSlotsFromSide((byte) GT6DrawerQuadBlockEntity.viewerLeftOf(tFacing).getOpposite().get3DDataValue()));
		}
		// the four rows spelled out against the literal FACING_ROTATIONS entries (the
		// viewer-left side per compass: N-facing->east, S-facing->west, E-facing->south,
		// W-facing->north):
		// NORTH (2): {0,1,3,5,4,2,6,6} — east(5)->2 left, west(4)->4 right
		tDrawer.setFacingNbtFallback((byte) Direction.NORTH.get3DDataValue());
		assertEquals(Direction.EAST, GT6DrawerQuadBlockEntity.viewerLeftOf(Direction.NORTH));
		org.junit.jupiter.api.Assertions.assertArrayEquals(GT6DrawerQuadBlockEntity.LEFT_HALF,
				tDrawer.getAccessibleSlotsFromSide((byte) Direction.EAST.get3DDataValue()));
		org.junit.jupiter.api.Assertions.assertArrayEquals(GT6DrawerQuadBlockEntity.RIGHT_HALF,
				tDrawer.getAccessibleSlotsFromSide((byte) Direction.WEST.get3DDataValue()));
		// SOUTH (3): {0,1,5,3,2,4,6,6} — west(4)->2 left, east(5)->4 right
		tDrawer.setFacingNbtFallback((byte) Direction.SOUTH.get3DDataValue());
		assertEquals(Direction.WEST, GT6DrawerQuadBlockEntity.viewerLeftOf(Direction.SOUTH));
		org.junit.jupiter.api.Assertions.assertArrayEquals(GT6DrawerQuadBlockEntity.LEFT_HALF,
				tDrawer.getAccessibleSlotsFromSide((byte) Direction.WEST.get3DDataValue()));
		org.junit.jupiter.api.Assertions.assertArrayEquals(GT6DrawerQuadBlockEntity.RIGHT_HALF,
				tDrawer.getAccessibleSlotsFromSide((byte) Direction.EAST.get3DDataValue()));
		// EAST (5): {0,1,4,2,5,3,6,6} — south(3)->2 left, north(2)->4 right
		tDrawer.setFacingNbtFallback((byte) Direction.EAST.get3DDataValue());
		assertEquals(Direction.SOUTH, GT6DrawerQuadBlockEntity.viewerLeftOf(Direction.EAST));
		org.junit.jupiter.api.Assertions.assertArrayEquals(GT6DrawerQuadBlockEntity.LEFT_HALF,
				tDrawer.getAccessibleSlotsFromSide((byte) Direction.SOUTH.get3DDataValue()));
		org.junit.jupiter.api.Assertions.assertArrayEquals(GT6DrawerQuadBlockEntity.RIGHT_HALF,
				tDrawer.getAccessibleSlotsFromSide((byte) Direction.NORTH.get3DDataValue()));
		// WEST (4): {0,1,2,4,3,5,6,6} — north(2)->2 left, south(3)->4 right
		tDrawer.setFacingNbtFallback((byte) Direction.WEST.get3DDataValue());
		assertEquals(Direction.NORTH, GT6DrawerQuadBlockEntity.viewerLeftOf(Direction.WEST));
		org.junit.jupiter.api.Assertions.assertArrayEquals(GT6DrawerQuadBlockEntity.LEFT_HALF,
				tDrawer.getAccessibleSlotsFromSide((byte) Direction.NORTH.get3DDataValue()));
		org.junit.jupiter.api.Assertions.assertArrayEquals(GT6DrawerQuadBlockEntity.RIGHT_HALF,
				tDrawer.getAccessibleSlotsFromSide((byte) Direction.SOUTH.get3DDataValue()));
		// the side view rides the same table (the automation face): a perpendicular side
		// sees one column, the side-any view sees all
		tDrawer.setFacingNbtFallback((byte) Direction.NORTH.get3DDataValue());
		assertEquals(72, tDrawer.sideView((byte) Direction.EAST.get3DDataValue()).getSlots());
		assertEquals(144, tDrawer.sideView((byte) 6).getSlots());
		// the chat line is the :96 feedback verbatim
		assertEquals("Automation-Access: Sided", tDrawer.accessChatLine());
	}

	@Test
	public void drawerFrontLocalUFollowsTheGetFacingCoordsClickedArms() {
		// UT.Code.getFacingCoordsClicked (UT.java:1734-1743): the texture-left edge is the
		// viewer's left on every horizontal face — north face u=1-x (:1737), south u=x
		// (:1738), west face u=z (:1742), east face u=1-z (:1743). The E/W arms are the
		// same compass flip the sided columns had.
		assertEquals(0.75, GT6DrawerQuadBlockEntity.frontLocalU(Direction.NORTH, 0.25, 0.5), 1e-9);
		assertEquals(0.25, GT6DrawerQuadBlockEntity.frontLocalU(Direction.SOUTH, 0.25, 0.5), 1e-9);
		assertEquals(0.75, GT6DrawerQuadBlockEntity.frontLocalU(Direction.EAST, 0.5, 0.25), 1e-9);
		assertEquals(0.25, GT6DrawerQuadBlockEntity.frontLocalU(Direction.WEST, 0.5, 0.25), 1e-9);
	}

	@Test
	public void drawerSideViewInsertExtractWalkTheGates() {
		GT6DrawerQuadBlockEntity tDrawer = new GT6DrawerQuadBlockEntity(sDrawerType, POS, Blocks.STONE.defaultBlockState());
		ItemStack tStone = new ItemStack(Items.STONE, 8);
		ItemStack tRest = tDrawer.sideView((byte) 6).insertItem(0, tStone, false);
		assertTrue(tRest.isEmpty());
		assertEquals(8, tDrawer.getInventory().getStackInSlot(0).getCount());
		ItemStack tOut = tDrawer.sideView((byte) 6).extractItem(0, 8, false);
		assertEquals(8, tOut.getCount());
		assertTrue(tDrawer.getInventory().getStackInSlot(0).isEmpty());
	}

	// ---------------------------------------------------------------------------
	// the Locker (upstream :82-85)
	// ---------------------------------------------------------------------------

	@Test
	public void lockerGateIsTheArmorTypeOfTheMirroredSlot() {
		GT6LockerBlockEntity tLocker = new GT6LockerBlockEntity(sLockerType, POS, Blocks.STONE.defaultBlockState());
		assertEquals(4, tLocker.getInventory().getSlots());
		// slot i holds the piece of type 3-i: 0 boots, 1 leggings, 2 chest, 3 helmet
		assertTrue(GT6LockerBlockEntity.isValidArmorForSlot(new ItemStack(Items.IRON_BOOTS), 0));
		assertTrue(GT6LockerBlockEntity.isValidArmorForSlot(new ItemStack(Items.IRON_LEGGINGS), 1));
		assertTrue(GT6LockerBlockEntity.isValidArmorForSlot(new ItemStack(Items.IRON_CHESTPLATE), 2));
		assertTrue(GT6LockerBlockEntity.isValidArmorForSlot(new ItemStack(Items.IRON_HELMET), 3));
		// the wrong piece for the slot bounces (upstream :84 isValidArmor(aStack, 3-aSlot))
		assertFalse(GT6LockerBlockEntity.isValidArmorForSlot(new ItemStack(Items.IRON_HELMET), 0));
		assertFalse(GT6LockerBlockEntity.isValidArmorForSlot(new ItemStack(Items.DIAMOND_CHESTPLATE), 3));
		assertFalse(GT6LockerBlockEntity.isValidArmorForSlot(ItemStack.EMPTY, 0));
		assertFalse(GT6LockerBlockEntity.isValidArmorForSlot(new ItemStack(Items.STONE), 2));
		// all four slots open from every side (:83), extract always true (:85)
		for (byte tSide = 0; tSide < 7; tSide++) {
			assertEquals(4, tLocker.sideView(tSide).getSlots());
		}
		assertTrue(tLocker.canExtractItem(0, (byte) 6));
	}

	// ---------------------------------------------------------------------------
	// the Safe (upstream :68-111) + the KeyLocked latch
	// ---------------------------------------------------------------------------

	@Test
	public void safeAnswersTheZeroSlotAutomationFace() {
		GT6SafeBlockEntity tSafe = new GT6SafeBlockEntity(sSafeType, POS, Blocks.STONE.defaultBlockState());
		assertEquals(15, tSafe.getInventory().getSlots()); // the :134-135 NBT_INV_SIZE
		assertEquals(0, tSafe.sideView((byte) 6).getSlots()); // upstream :105 ZL_INTEGER
		assertFalse(tSafe.canInsertItem(0, new ItemStack(Items.STONE), (byte) 0)); // :106
		assertFalse(tSafe.canExtractItem(0, (byte) 0)); // :107
		assertTrue(tSafe.isOpen()); // the mechanical personality never locks (the mOwner fold)
	}

	@Test
	public void safeDungeonLootFillsTheEmptySlotsAndClearsTheMarker() {
		GT6SafeBlockEntity tSafe = new GT6SafeBlockEntity(sSafeType, POS, Blocks.STONE.defaultBlockState());
		tSafe.mDungeonLootName = "gt6:chests/safe_dungeon";
		tSafe.getInventory().setStackInSlot(7, new ItemStack(Items.DIAMOND)); // an occupied slot stays
		tSafe.generateDungeonLootFrom(aName -> {
			assertEquals("gt6:chests/safe_dungeon", aName, "the roller receives the marker verbatim");
			return new ItemStack(Items.EMERALD, 3);
		});
		for (int i = 0; i < 15; i++) {
			if (i == 7) {
				assertEquals(Items.DIAMOND, tSafe.getInventory().getStackInSlot(i).getItem());
			} else {
				assertEquals(Items.EMERALD, tSafe.getInventory().getStackInSlot(i).getItem());
			}
		}
		assertEquals("", tSafe.mDungeonLootName, "upstream :75 clears the marker after the fill");
		// the guarded face no-ops offline (no level) and never touches a cleared marker
		tSafe.mDungeonLootName = "gt6:chests/safe_dungeon";
		tSafe.tryGenerateDungeonLoot();
		assertEquals("gt6:chests/safe_dungeon", tSafe.mDungeonLootName);
	}

	@Test
	public void safeDungeonLootMarkerRidesTheNBT() {
		GT6SafeBlockEntity tSafe = new GT6SafeBlockEntity(sSafeType, POS, Blocks.STONE.defaultBlockState());
		tSafe.mDungeonLootName = "minecraft:chests/simple_dungeon";
		CompoundTag tTag = tSafe.saveWithoutMetadata();
		assertEquals("minecraft:chests/simple_dungeon", tTag.getString(GT6SafeBlockEntity.NBT_DUNGEON_LOOT));
		GT6SafeBlockEntity tCopy = new GT6SafeBlockEntity(sSafeType, POS, Blocks.STONE.defaultBlockState());
		tCopy.load(tTag);
		assertEquals("minecraft:chests/simple_dungeon", tCopy.mDungeonLootName);
	}

	@Test
	public void keyLockedSafeLatchFollowsTheUseKeySeam() {
		GT6SafeKeyLockedBlockEntity tSafe = new GT6SafeKeyLockedBlockEntity(sKeySafeType, POS, Blocks.STONE.defaultBlockState());
		assertFalse(tSafe.isOpen()); // upstream :53 mOpened = F
		// the first key claims (upstream :86-87 mID == 0 -> mID = tID) and flips open
		assertTrue(tSafe.useKey(4242L));
		assertEquals(4242L, tSafe.mID);
		assertTrue(tSafe.isOpen());
		// the matching key flips closed again
		assertTrue(tSafe.useKey(4242L));
		assertFalse(tSafe.isOpen());
		// a wrong key bounces and never re-claims (upstream :90 tID == mID)
		assertFalse(tSafe.useKey(7L));
		assertFalse(tSafe.isOpen());
		// the latch rides the NBT (upstream :62-67)
		CompoundTag tTag = tSafe.saveWithoutMetadata();
		GT6SafeKeyLockedBlockEntity tCopy = new GT6SafeKeyLockedBlockEntity(sKeySafeType, POS, Blocks.STONE.defaultBlockState());
		tCopy.load(tTag);
		assertEquals(4242L, tCopy.mID);
		assertFalse(tCopy.isOpen());
		assertTrue(tCopy.useKey(4242L));
		assertTrue(tCopy.isOpen());
	}

	// ---------------------------------------------------------------------------
	// the Bookshelf (upstream :362-364) and the Bottlecrate (:246-249)
	// ---------------------------------------------------------------------------

	@Test
	public void bookshelfRangesAndArmsFollowTheFaceSplit() {
		GT6BookShelfBlockEntity tShelf = new GT6BookShelfBlockEntity(sShelfType, POS, Blocks.STONE.defaultBlockState());
		assertEquals(28, tShelf.getInventory().getSlots());
		assertEquals(1, tShelf.stackLimit()); // upstream :362
		// the redstone-arm exclusions (:363-364)
		assertTrue(GT6BookShelfBlockEntity.isRedstoneArm(new ItemStack(Items.COBBLESTONE)));
		assertTrue(GT6BookShelfBlockEntity.isRedstoneArm(new ItemStack(Items.REDSTONE_TORCH)));
		assertTrue(GT6BookShelfBlockEntity.isRedstoneArm(new ItemStack(Items.LEVER)));
		assertTrue(GT6BookShelfBlockEntity.isRedstoneArm(new ItemStack(Items.STONE_BUTTON)));
		assertFalse(GT6BookShelfBlockEntity.isRedstoneArm(new ItemStack(Items.BOOK)));
		// the range-level picks of the shift-all/single-take fold
		assertEquals(0, tShelf.firstEmptyOfFace(false));
		assertEquals(14, tShelf.firstEmptyOfFace(true)); // the back face starts at slot 14
		tShelf.getInventory().setStackInSlot(13, new ItemStack(Items.BOOK));
		tShelf.getInventory().setStackInSlot(20, new ItemStack(Items.WRITTEN_BOOK));
		assertEquals(13, tShelf.lastOccupiedOfFace(false));
		assertEquals(20, tShelf.lastOccupiedOfFace(true));
		// the arm slot refuses both directions even when the tag would admit
		tShelf.getInventory().setStackInSlot(5, new ItemStack(Items.LEVER));
		assertFalse(tShelf.canExtractItem(5, (byte) 0));
	}

	@Test
	public void bottlecrateGateIsTheBottleFamilyAndKeepsItsSlots() {
		GT6BottleCrateBlockEntity tCrate = new GT6BottleCrateBlockEntity(sCrateType, POS, Blocks.STONE.defaultBlockState());
		assertEquals(9, tCrate.getInventory().getSlots());
		assertTrue(GT6BottleCrateBlockEntity.isBottleFamily(new ItemStack(Items.POTION)));
		assertTrue(GT6BottleCrateBlockEntity.isBottleFamily(new ItemStack(Items.GLASS_BOTTLE)));
		assertTrue(GT6BottleCrateBlockEntity.isBottleFamily(new ItemStack(Items.EXPERIENCE_BOTTLE)));
		assertTrue(GT6BottleCrateBlockEntity.isBottleFamily(new ItemStack(Items.HONEY_BOTTLE)), "the container-item arm: the honey bottle's remainder is a glass bottle");
		assertFalse(GT6BottleCrateBlockEntity.isBottleFamily(new ItemStack(Items.STONE)));
		assertTrue(tCrate.canInsertItem(0, new ItemStack(Items.POTION), (byte) 0)); // :247-249
		assertFalse(tCrate.canInsertItem(0, new ItemStack(Items.STONE), (byte) 0));
		assertTrue(tCrate.canExtractItem(0, (byte) 0)); // :246
		assertEquals(-1, tCrate.lastOccupied());
		tCrate.getInventory().setStackInSlot(4, new ItemStack(Items.EXPERIENCE_BOTTLE));
		assertEquals(4, tCrate.lastOccupied());
		assertEquals(0, tCrate.firstEmpty());
	}

	// ---------------------------------------------------------------------------
	// the shared skeleton (the facing fallback, the base NBT pair)
	// ---------------------------------------------------------------------------

	@Test
	public void baseFacingAndInventoryRideTheNBTRoundTrip() {
		GT6DrawerQuadBlockEntity tDrawer = new GT6DrawerQuadBlockEntity(sDrawerType, POS, Blocks.STONE.defaultBlockState());
		tDrawer.setFacingNbtFallback((byte) 5); // EAST (the block's setPlacedBy keeps it in step)
		CompoundTag tTag = tDrawer.saveWithoutMetadata();
		assertEquals((byte) 5, tTag.getByte(GT6StaticStorageBaseBlockEntity.NBT_FACING));
		assertTrue(tTag.contains(GT6StaticStorageBaseBlockEntity.NBT_INVENTORY), "the 144-slot handler serializes");
		GT6DrawerQuadBlockEntity tCopy = new GT6DrawerQuadBlockEntity(sDrawerType, POS, Blocks.STONE.defaultBlockState());
		tCopy.load(tTag);
		assertEquals(5, tCopy.getFacing()); // the blockstate fallback: STONE carries no FACING property
		assertEquals(144, tCopy.getInventory().getSlots());
	}

	@Test
	public void contentHookFiresOnMutation() {
		// the adjacency wake rides this hook (the event-driven fold — the base binds
		// updateInventory, which fans out to the neighbours server-side; here we count)
		GT6DrawerQuadBlockEntity tDrawer = new GT6DrawerQuadBlockEntity(sDrawerType, POS, Blocks.STONE.defaultBlockState());
		int[] tWakes = {0};
		tDrawer.getInventory().setOnContentsChanged(() -> tWakes[0]++);
		tDrawer.getInventory().setStackInSlot(0, new ItemStack(Items.STONE));
		assertTrue(tWakes[0] > 0, "the GTItemStackHandler content hook fires on mutation");
	}
}
