package gregtech6.tileentity.multiblocks;

import org.junit.jupiter.api.Test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import gregtech6.tileentity.multiblocks.ITileEntityMultiBlockController.Util;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The checkAndSetTarget tests (task p4-multiblock-framework acceptance ①): the occupation
 * arbitration (upstream ITileEntityMultiBlockController.Util :70-75), the two-pass wand
 * semantics (:51-68 with the stale-reference quirk) and the inventory consumption.
 */
public class CheckAndSetTargetTest extends GTMultiBlocksOfflineTestBase {

	private static BlockPos partCell(int i, int j, int k) {
		// C1 centre = (100,64,101); C1 is NOT a part cell, the 26 others are
		return new BlockPos(100 + i, 64 + j, 101 + k);
	}

	@Test
	void controllerCellAlwaysPasses() {
		// upstream :49
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestCokeOven tOven = placeController(tLevel, sCokeOvenType, C1, (byte) 2);
		assertTrue(Util.checkAndSetTarget(tOven, C1.getX(), C1.getY(), C1.getZ(), Blocks.BRICKS, 0, 0, null, null, null));
	}

	@Test
	void emptyCellWithoutInventoryFailsWithoutPlacing() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestCokeOven tOven = placeController(tLevel, sCokeOvenType, C1, (byte) 2);
		BlockPos tCell = partCell(0, -1, -1);

		assertFalse(Util.checkAndSetTarget(tOven, tCell.getX(), tCell.getY(), tCell.getZ(), Blocks.BRICKS, 0, 0, null, null, null));
		assertTrue(tLevel.getBlockState(tCell).isAir(), "no inventory, no auto-place");
	}

	@Test
	void wandPathIsTwoPassFillThenLink() {
		// the upstream onToolClick2 :141-146 sequence: pass 1 places (+consumes), pass 2 links
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestCokeOven tOven = placeController(tLevel, sCokeOvenType, C1, (byte) 2);
		tLevel.mBeFactory = (aPos, aState) -> {
			MultiBlockPartBlockEntity tPart = sPartType.create(aPos, aState);
			tPart.setLevel(tLevel);
			return tPart;
		};
		BlockPos tCell = partCell(1, 1, -1);
		SimpleContainer tInventory = new SimpleContainer(3);
		tInventory.setItem(0, new ItemStack(Items.BRICKS, 1));
		tInventory.setItem(1, new ItemStack(Items.STONE, 4)); // a non-matching filler

		// pass 1: the placement happens, the stale-reference quirk fails the call (upstream :70 sees the OLD cell)
		boolean tFirst = Util.checkAndSetTarget(tOven, tCell.getX(), tCell.getY(), tCell.getZ(),
				Blocks.BRICKS, 0, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_ENERGY, tOven.getBlockPos(), null, tInventory);
		assertFalse(tFirst, "pass 1 fails on the stale pre-placement reference");
		assertEquals(0, tInventory.getItem(0).getCount(), "the matching stack was consumed (ST.use shrink)");
		assertEquals(4, tInventory.getItem(1).getCount(), "the non-matching stack is untouched");
		assertInstanceOf(MultiBlockPartBlockEntity.class, tLevel.getBlockEntity(tCell), "the part was auto-placed");

		// pass 2: the fresh fetch sees the part, arbitration claims it for this controller
		boolean tSecond = Util.checkAndSetTarget(tOven, tCell.getX(), tCell.getY(), tCell.getZ(),
				Blocks.BRICKS, 0, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_ENERGY, tOven.getBlockPos(), null, tInventory);
		assertTrue(tSecond);
		MultiBlockPartBlockEntity tPart = (MultiBlockPartBlockEntity) tLevel.getBlockEntity(tCell);
		assertSame(tOven, tPart.mTarget);
		assertEquals(MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_ENERGY, tPart.mMode);
	}

	@Test
	void survivalConsumeWithNullPlayerTakesExactlyOne() {
		// null player = canEdit auto-approve (UT.Entities.canEdit :3159) + the ST.use consume branch
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestCokeOven tOven = placeController(tLevel, sCokeOvenType, C1, (byte) 2);
		tLevel.mBeFactory = (aPos, aState) -> sPartType.create(aPos, aState);
		BlockPos tCell = partCell(-1, -1, -1);
		SimpleContainer tInventory = new SimpleContainer(1);
		tInventory.setItem(0, new ItemStack(Items.BRICKS, 5));

		assertDoesNotThrow(() -> Util.checkAndSetTarget(tOven, tCell.getX(), tCell.getY(), tCell.getZ(),
				Blocks.BRICKS, 0, 0, null, null, tInventory));
		assertEquals(4, tInventory.getItem(0).getCount(), "the consume path shrank exactly one");
	}

	@Test
	void occupiedByAnotherControllerFailsArbitration() {
		// upstream :72 — a part claimed by a DIFFERENT controller that still contains the cell is a failure
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestCokeOven tOven = placeController(tLevel, sCokeOvenType, C1, (byte) 2);
		TestCokeOven tOther = placeController(tLevel, sCokeOvenType, C2, (byte) 2);
		MultiBlockPartBlockEntity tPart = placePart(tLevel, SHARED_CELL); // inside BOTH structures' boxes
		tPart.setTarget(tOther, 0, MultiBlockPartBlockEntity.EVERYTHING);

		assertTrue(tOther.isInsideStructure(SHARED_CELL.getX(), SHARED_CELL.getY(), SHARED_CELL.getZ()),
				"fixture sanity: the shared cell lies inside the other structure");
		assertFalse(Util.checkAndSetTarget(tOven, SHARED_CELL.getX(), SHARED_CELL.getY(), SHARED_CELL.getZ(),
				Blocks.BRICKS, 0, 0, null, null, null), "occupied by another controller -> failure");
		assertSame(tOther, tPart.mTarget, "the claim is untouched");
	}

	@Test
	void reclaimByTheSameControllerIsIdempotent() {
		// upstream :73-74 — the current owner re-setting its own part passes
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestCokeOven tOven = placeController(tLevel, sCokeOvenType, C1, (byte) 2);
		MultiBlockPartBlockEntity tPart = placePart(tLevel, SHARED_CELL);
		tPart.setTarget(tOven, 0, MultiBlockPartBlockEntity.EVERYTHING);

		assertTrue(Util.checkAndSetTarget(tOven, SHARED_CELL.getX(), SHARED_CELL.getY(), SHARED_CELL.getZ(),
				Blocks.BRICKS, 0, MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_ENERGY, null, null, null));
		assertSame(tOven, tPart.mTarget);
		assertEquals(MultiBlockPartBlockEntity.ONLY_ITEM_FLUID_ENERGY, tPart.mMode, "the mode was re-applied");
	}

	@Test
	void wrongPartBlockTypeFails() {
		// upstream :70 — the part TYPE must match (the Block identity here)
		MultiBlockLevel tLevel = new MultiBlockLevel();
		TestCokeOven tOven = placeController(tLevel, sCokeOvenType, C1, (byte) 2);
		placePart(tLevel, SHARED_CELL); // a BRICKS part

		assertFalse(Util.checkAndSetTarget(tOven, SHARED_CELL.getX(), SHARED_CELL.getY(), SHARED_CELL.getZ(),
				Blocks.STONE, 0, 0, null, null, null), "a stone requirement does not accept a bricks part");
	}

	@Test
	void cokeOvenStructureFormsAndUnformsEndToEnd() {
		MultiBlockLevel tLevel = new MultiBlockLevel();
		tLevel.mBeFactory = (aPos, aState) -> sPartType.create(aPos, aState);
		TestCokeOven tOven = placeController(tLevel, sCokeOvenType, C1, (byte) 2);
		BlockPos tCenter = new BlockPos(100, 64, 101);

		// the centre must ALREADY be air (upstream :52 — a non-air centre fails)
		tLevel.mStates.put(tCenter, Blocks.BRICKS.defaultBlockState());
		assertFalse(tOven.checkStructure2(null, null, null), "a blocked centre fails the check");

		// 25 bricks in place (the controller occupies the last shell cell), centre air -> the check links every part
		tLevel.mStates.put(tCenter, Blocks.AIR.defaultBlockState());
		for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) for (int k = -1; k <= 1; k++) {
			if (i == 0 && j == 0 && k == 0) continue;
			BlockPos tCell = tCenter.offset(i, j, k);
			if (tCell.equals(C1)) continue; // the controller's own shell cell passes via the :49 self-check
			placePart(tLevel, tCell);
		}
		assertTrue(tOven.checkStructure2(null, null, null));
		int tLinked = 0;
		for (int i = -1; i <= 1; i++) for (int j = -1; j <= 1; j++) for (int k = -1; k <= 1; k++) {
			if (i == 0 && j == 0 && k == 0) continue;
			if (tLevel.getBlockEntity(tCenter.offset(i, j, k)) instanceof MultiBlockPartBlockEntity tPart && tPart.mTarget == tOven) tLinked++;
		}
		assertEquals(25, tLinked, "every part claimed by the controller");

		// the template flip rides the check: forced pass forms the structure
		assertTrue(tOven.checkStructure(true));
		assertTrue(tOven.mStructureOkay);

		// one part vanishes (breakBlock propagation: clear + flag) -> the recheck unforms
		MultiBlockPartBlockEntity tBroken = (MultiBlockPartBlockEntity) tLevel.getBlockEntity(partCell(1, 1, -1));
		tBroken.clearTarget();
		tOven.onStructureChange();
		tLevel.mBlockEntities.remove(partCell(1, 1, -1));
		tLevel.mStates.put(partCell(1, 1, -1), Blocks.AIR.defaultBlockState());
		assertFalse(tOven.checkStructure(false), "the changed flag drives the recheck -> unformed");
		assertFalse(tOven.mStructureOkay);
	}
}
