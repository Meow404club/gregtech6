package gregtech6.tileentity.inventories;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.registry.GT6Chests;
import gregtech6.registry.GT6Chests.ChestRow;
import gregtech6.registry.GT6Hoppers;
import gregtech6.registry.GT6MaterialTestSupport;
import gregtech6.tileentity.GTOfflineTestBase;
import gregtech6.tileentity.GTItemStackHandler;

/**
 * GT6 metal-chest family offline tests (task material-mc-a-storage-chests acceptance —
 * the research.material-coverage-census mc-A head): the 120-row axis census over the
 * metalset chest pair (Loader_MultiTileEntities.java:132-133 over the shared 60-material
 * loop :186-245 via {@link GT6Hoppers#MATERIALS}), the 54-slot BE posture, the NBT round
 * trip, the comparator rungs, the chest box geometry and the pincers five-pass transfer
 * (the :182-229 order — merge-first, the hotbar keep-out, the pass gates).
 */
public class GT6ChestFamilyTest extends GTOfflineTestBase {

	static BlockEntityType<GT6ChestBlockEntity> sChestType;
	static final BlockPos POS = new BlockPos(2, 3, 4);

	@BeforeAll
	static void buildOfflineFixture() {
		// the hermetic material boot FIRST (the hopper-family lesson): the row-axis anchors
		// resolve loader materials live (MT.Pb & co)
		GT6MaterialTestSupport.materials();
		@SuppressWarnings("unchecked")
		BlockEntityType<GT6ChestBlockEntity>[] tChest = (BlockEntityType<GT6ChestBlockEntity>[]) new BlockEntityType<?>[1];
		tChest[0] = BlockEntityType.Builder.of(
				(aPos, aState) -> new GT6ChestBlockEntity(tChest[0], aPos, aState),
				Blocks.STONE).build(null);
		sChestType = tChest[0];
	}

	private static GT6ChestBlockEntity chest() {
		return new GT6ChestBlockEntity(sChestType, POS, Blocks.STONE.defaultBlockState());
	}

	// ---------------------------------------------------------------------------
	// the row axis (the metalset chest pair :132-133 over the 60-material loop)
	// ---------------------------------------------------------------------------

	@Test
	public void rowAxisReproducesTheLoaderChestPair() {
		// the census: 60 loader lines x the metalset chest pair = 120 rows, the SHARED line table
		assertEquals(60, GT6Hoppers.MATERIALS.size());
		assertEquals(120, GT6Chests.ROWS.size());
		// the verbatim interleaved walk: row 2i = the plain chest of loader line i (:132),
		// row 2i+1 = its reinforced twin (:133)
		for (int i = 0; i < GT6Hoppers.MATERIALS.size(); i++) {
			GT6Hoppers.HopperMaterial tMat = GT6Hoppers.MATERIALS.get(i);
			ChestRow tPlain = GT6Chests.ROWS.get(i * 2), tReinforced = GT6Chests.ROWS.get(i * 2 + 1);
			assertEquals(tMat, tPlain.material());
			assertEquals(tMat, tReinforced.material());
			assertFalse(tPlain.reinforced());
			assertTrue(tReinforced.reinforced());
			// the paths and the id columns (:132 id 0+aID, :133 id 500+aID)
			assertEquals("chest_" + tMat.slug(), tPlain.path());
			assertEquals("reinforced_chest_" + tMat.slug(), tReinforced.path());
			assertEquals(0 + tMat.metaId(), tPlain.metaId());
			assertEquals(500 + tMat.metaId(), tReinforced.metaId());
		}
		// the ids and paths stay unique across the 120 (the plain ladder starts at id 0)
		assertEquals(120, GT6Chests.ROWS.stream().map(ChestRow::metaId).distinct().count());
		assertEquals(120, GT6Chests.ROWS.stream().map(ChestRow::path).distinct().count());
		// the registration integrity — every line resolves its material (the mdh-6 gate
		// filters the walk, so every shipped row's material registers)
		for (ChestRow tRow : GT6Chests.ROWS) {
			assertNotNull(tRow.material().mt(), "no loader material for slug " + tRow.material().slug());
		}
	}

	@Test
	public void matrixSpotPinsMatchTheLoaderColumns() {
		// the first line (:186): Lead, aID 0 — the plain chest IS upstream id 0
		ChestRow tLead = GT6Chests.ROWS.get(0), tLeadReinforced = GT6Chests.ROWS.get(1);
		assertEquals("lead", tLead.material().slug());
		assertEquals(0, tLead.metaId());
		assertEquals(500, tLeadReinforced.metaId());
		assertEquals(4.0F, tLead.material().hardness());
		// the Bronze/Steel anchors keep their loader seats (:191 line 6, :202 line 17 — 1-based)
		ChestRow tBronze = GT6Chests.ROWS.get(5 * 2), tSteel = GT6Chests.ROWS.get(16 * 2 + 1);
		assertEquals("bronze", tBronze.material().slug());
		assertEquals(9, tBronze.metaId());
		assertEquals("steel", tSteel.material().slug());
		assertEquals(510, tSteel.metaId());
		assertEquals(7.0F, tBronze.material().hardness());
		assertEquals(6.0F, tSteel.material().hardness());
		// the last line (:245): Infinity, aID 50 — plain 50 / reinforced 550
		ChestRow tInfinity = GT6Chests.ROWS.get(120 - 2);
		assertEquals("infinity", tInfinity.material().slug());
		assertEquals(50, tInfinity.metaId());
		assertEquals(100.0F, tInfinity.material().hardness());
		// the :235 ANY.W line resolves the Tungsten face (ANY.java:133 setLocal)
		ChestRow tTungsten = GT6Chests.ROWS.get(49 * 2);
		assertEquals("tungsten", tTungsten.material().slug());
		assertEquals("Tungsten", tTungsten.material().display());
		// the composed display keys (the zh dump faces: gt.multitileentity.0 铅箱子 / .500 铅强化木箱)
		assertEquals("gt6.row.chest.display", GT6Chests.DISPLAY_KEY);
		assertEquals("gt6.row.reinforced_chest.display", GT6Chests.DISPLAY_REINFORCED_KEY);
		// the shared metalset unit table (the hopper walk emits the 60 words)
		assertEquals("gt6.row.mat.lead", GT6Chests.matUnitKeyOf(tLead.material()));
	}

	// ---------------------------------------------------------------------------
	// the 54-slot BE posture + the NBT round trip
	// ---------------------------------------------------------------------------

	@Test
	public void chestCarriesThe54SlotInventoryNoFilter() {
		GT6ChestBlockEntity tChest = chest();
		assertEquals(54, tChest.getInventory().getSlots()); // the :132/:133 NBT_INV_SIZE column
		// the MTE default automation face: all slots, all sides, no filter
		for (byte tSide = 0; tSide < 7; tSide++) {
			int[] tSlots = tChest.getAccessibleSlotsFromSide(tSide);
			assertEquals(54, tSlots.length);
			assertEquals(54, tSlots[53] + 1);
		}
		assertTrue(tChest.canInsertItem(0, new ItemStack(Items.STONE), (byte) 3));
		assertTrue(tChest.canExtractItem(0, (byte) 3));
	}

	@Test
	public void nbtRoundTripCarriesInventory() {
		GT6ChestBlockEntity tChest = chest();
		tChest.getInventory().setStackInSlot(7, new ItemStack(Items.GOLD_INGOT, 32));
		tChest.setFacingNbtFallback((byte) 5);
		CompoundTag tTag = tChest.saveWithoutMetadata();
		assertTrue(tTag.contains(GT6StaticStorageBaseBlockEntity.NBT_INVENTORY, Tag.TAG_COMPOUND));
		GT6ChestBlockEntity tRested = chest();
		tRested.load(tTag);
		assertEquals(32, tRested.getInventory().getStackInSlot(7).getCount());
		assertTrue(tRested.getInventory().getStackInSlot(7).is(Items.GOLD_INGOT));
		assertEquals(5, tRested.getFacing()); // the level-less fixture: no blockstate property → the NBT fallback byte
	}

	@Test
	public void chestBoxMatchesTheUpstreamSurfaceColumns() {
		// the vanilla chest box — the upstream collision/selection columns verbatim
		// (MultiTileEntityChest.java:308-311: 0.0625..0.9375 x/z, 0..0.875 y)
		VoxelShape tShape = GT6Chests.GT6ChestBlock.CHEST_SHAPE;
		assertEquals(0.0625, tShape.min(net.minecraft.core.Direction.Axis.X)); // 1/16
		assertEquals(0.9375, tShape.max(net.minecraft.core.Direction.Axis.X)); // 15/16
		assertEquals(0.0, tShape.min(net.minecraft.core.Direction.Axis.Y));
		assertEquals(0.875, tShape.max(net.minecraft.core.Direction.Axis.Y)); // 14/16
	}

	// ---------------------------------------------------------------------------
	// the comparator rungs (upstream :256 the calcRedstoneFromInventory identity)
	// ---------------------------------------------------------------------------

	@Test
	public void comparatorRungsMatchTheVanillaFullnessFormula() {
		GTItemStackHandler tInventory = new GTItemStackHandler(54);
		assertEquals(0, GT6Chests.GT6ChestBlock.comparatorSignal(tInventory)); // empty
		// the one-stack rung: floor((64/64)/54 * 14) + 1 = 1 (the vanilla MEAN-fullness
		// semantics — one stack in one of 54 slots is 1/54 full, not 15)
		tInventory.setStackInSlot(0, new ItemStack(Items.STONE, 64));
		assertEquals(1, GT6Chests.GT6ChestBlock.comparatorSignal(tInventory));
		wipe(tInventory);
		for (int i = 0; i < 27; i++) tInventory.setStackInSlot(i, new ItemStack(Items.STONE, 64));
		assertEquals(8, GT6Chests.GT6ChestBlock.comparatorSignal(tInventory)); // half the slots: floor(14/2)+1
		wipe(tInventory);
		tInventory.setStackInSlot(0, new ItemStack(Items.STONE, 1));
		assertEquals(1, GT6Chests.GT6ChestBlock.comparatorSignal(tInventory)); // the any-item +1
		for (int i = 0; i < 54; i++) tInventory.setStackInSlot(i, new ItemStack(Items.STONE, 64));
		assertEquals(15, GT6Chests.GT6ChestBlock.comparatorSignal(tInventory)); // every slot full: 14+1
	}

	private static void wipe(GTItemStackHandler aInventory) {
		for (int i = 0; i < aInventory.getSlots(); i++) aInventory.setStackInSlot(i, ItemStack.EMPTY);
	}

	// ---------------------------------------------------------------------------
	// the pincers five-pass transfer (upstream :182-229)
	// ---------------------------------------------------------------------------

	/** A player inventory wrapper over the vanilla 36-slot carrier (the :183 player-inventory face). */
	private static net.minecraft.world.entity.player.Inventory playerInv() {
		return new net.minecraft.world.entity.player.Inventory(null) {
			@Override
			public int getContainerSize() {
				return 36;
			}
		};
	}

	@Test
	public void pincersPassOneMergesEqualStacksBeforePassTwoFills() {
		// pass 1 (the :188-194 ST.equal scan) merges +10 into slot 5; the UNGATED pass 2
		// (:196-201, no rCount gate upstream) then vacates the remaining 10 into the first
		// free main-inventory slot — the merge priority shows in the LAYOUT, not the total
		GT6ChestBlockEntity tChest = chest();
		tChest.getInventory().setStackInSlot(0, new ItemStack(Items.STONE, 20));
		net.minecraft.world.entity.player.Inventory tPlayer = playerInv();
		tPlayer.setItem(5, new ItemStack(Items.STONE, 10));
		tPlayer.setItem(9, new ItemStack(Items.DIRT, 3));
		// the ST.move merge leg moves the WHOLE chest stack while the player slot has room
		// (20 into the 10-stack → 30 ≤ 64): the whole chest vacates through the merge
		assertEquals(20, tChest.pincersTransfer(tPlayer)); // the RETURN is the moved count
		assertEquals(30, tPlayer.getItem(5).getCount()); // 10 + the merged 20
		assertEquals(3, tPlayer.getItem(9).getCount()); // the occupied dirt slot untouched
		assertTrue(tChest.getInventory().getStackInSlot(0).isEmpty());
		// the pass-2 fill face standalone: an equal-slot-free drain lands at the main head
		GT6ChestBlockEntity tChest2 = chest();
		tChest2.getInventory().setStackInSlot(0, new ItemStack(Items.STONE, 10));
		net.minecraft.world.entity.player.Inventory tPlayer2 = playerInv();
		tPlayer2.setItem(9, new ItemStack(Items.DIRT, 3)); // the occupied unequal slot at the head
		assertEquals(10, tChest2.pincersTransfer(tPlayer2));
		assertEquals(10, tPlayer2.getItem(10).getCount()); // the first FREE slot after 9
		assertEquals(3, tPlayer2.getItem(9).getCount());
	}

	@Test
	public void pincersPassTwoLandsNbtLessStackablesBelowTheHotbar() {
		GT6ChestBlockEntity tChest = chest();
		tChest.getInventory().setStackInSlot(3, new ItemStack(Items.STONE, 5));
		net.minecraft.world.entity.player.Inventory tPlayer = playerInv();
		assertEquals(5, tChest.pincersTransfer(tPlayer));
		assertEquals(5, tPlayer.getItem(9).getCount()); // slot 9 = the main-inventory head (:197 scan start)
		assertTrue(tChest.getInventory().getStackInSlot(3).isEmpty());
	}

	@Test
	public void pincersNbtCarryingStackableTakesTheLaterStackablePass() {
		// the tagged diamond: pass 1 merges only equal stacks (none), pass 2 refuses NBT
		// carriers, pass 3's stackable arm fills the first main-inventory slot
		GT6ChestBlockEntity tChest = chest();
		ItemStack tTagged = new ItemStack(Items.DIAMOND, 4);
		//? if forge {
		tTagged.getOrCreateTag().putBoolean("gt6_test", true);
		//?} else {
		/*net.minecraft.nbt.CompoundTag tTag = new net.minecraft.nbt.CompoundTag();
		tTag.putBoolean("gt6_test", true);
		tTagged.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
				net.minecraft.world.item.component.CustomData.of(tTag)); // the 21.1 components carrier (GTItemPaintTintTest form)
		*///?}
		tChest.getInventory().setStackInSlot(0, tTagged);
		net.minecraft.world.entity.player.Inventory tPlayer = playerInv();
		assertEquals(4, tChest.pincersTransfer(tPlayer));
		assertEquals(4, tPlayer.getItem(9).getCount());
	}

	@Test
	public void pincersPassFourTakesNbtCarryingUnstackablesAndPassFiveTheBareSword() {
		// pass 4: the enchanted book (unstackable + NBT — passes 1-3 all refuse)
		GT6ChestBlockEntity tChest = chest();
		tChest.getInventory().setStackInSlot(0, new ItemStack(Items.ENCHANTED_BOOK));
		net.minecraft.world.entity.player.Inventory tPlayer = playerInv();
		assertEquals(1, tChest.pincersTransfer(tPlayer));
		assertFalse(tPlayer.getItem(9).isEmpty());
		// pass 5: the bare sword (unstackable, no NBT) — the last-resort arm
		GT6ChestBlockEntity tChest2 = chest();
		tChest2.getInventory().setStackInSlot(1, new ItemStack(Items.DIAMOND_SWORD));
		net.minecraft.world.entity.player.Inventory tPlayer2 = playerInv();
		assertEquals(1, tChest2.pincersTransfer(tPlayer2));
		assertFalse(tPlayer2.getItem(9).isEmpty());
	}

	@Test
	public void pincersPassTwoKeepsTheHotbarFree() {
		GT6ChestBlockEntity tChest = chest();
		tChest.getInventory().setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 2));
		net.minecraft.world.entity.player.Inventory tPlayer = playerInv();
		tPlayer.setItem(20, new ItemStack(Items.IRON_INGOT, 1)); // an occupied merge target above the hotbar
		assertEquals(2, tChest.pincersTransfer(tPlayer)); // pass 1 merges the pair, no hotbar touch
		assertEquals(3, tPlayer.getItem(20).getCount());
	}

	@Test
	public void pincersSurplusSplitsAcrossTheMainInventory() {
		GT6ChestBlockEntity tChest = chest();
		tChest.getInventory().setStackInSlot(0, new ItemStack(Items.STONE, 70)); // over one stack
		net.minecraft.world.entity.player.Inventory tPlayer = playerInv();
		tPlayer.setItem(10, new ItemStack(Items.STONE, 63)); // one merge room in the main inventory
		// pass 1: +1 into slot 10 (64 full). pass 2: 64 into the empty slot 9, then 5 into
		// the next empty slot 11 — the chest drains whole (the surplus never vanishes)
		assertEquals(70, tChest.pincersTransfer(tPlayer));
		assertTrue(tChest.getInventory().getStackInSlot(0).isEmpty());
		assertEquals(64, tPlayer.getItem(9).getCount());
		assertEquals(64, tPlayer.getItem(10).getCount());
		assertEquals(5, tPlayer.getItem(11).getCount());
	}
}
