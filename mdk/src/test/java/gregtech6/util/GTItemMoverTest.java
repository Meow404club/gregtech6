package gregtech6.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.minecraftforge.items.ItemStackHandler;

import org.junit.jupiter.api.Test;

import gregtech6.tileentity.GTItemStackHandler;
import gregtech6.tileentity.GTOfflineTestBase;

/**
 * Truth table for {@link GTItemMover} — the ST.move subset semantics the W2 covers consume.
 * Every case anchors the upstream clause it pins (gregapi/util/ST.java line numbers in comments);
 * "probe" cases pin that the simulated canPut/canTake measurements never touch the books.
 */
public class GTItemMoverTest extends GTOfflineTestBase {

	private static ItemStack stone(int aCount) {
		return new ItemStack(Items.STONE, aCount);
	}

	private static ItemStack dirt(int aCount) {
		return new ItemStack(Items.DIRT, aCount);
	}

	private static ItemStackHandler handler(int aSlots, ItemStack... aStacks) {
		ItemStackHandler tHandler = new ItemStackHandler(aSlots);
		for (int i = 0; i < aStacks.length; i++) {
			tHandler.setStackInSlot(i, aStacks[i]);
		}
		return tHandler;
	}

	@Test
	public void emptySourceMovesNothing() {
		ItemStackHandler tFrom = handler(3);
		ItemStackHandler tTo = handler(3);

		assertEquals(0, GTItemMover.move(tFrom, tTo));
		assertEquals(0, GTItemMover.moveFrom(tFrom, tTo, 0));
		assertEquals(0, GTItemMover.moveTo(tFrom, tTo, 0));

		for (int i = 0; i < 3; i++) {
			assertTrue(tFrom.getStackInSlot(i).isEmpty());
			assertTrue(tTo.getStackInSlot(i).isEmpty());
		}
	}

	@Test
	public void foreignAndFullTargetsReject() {
		// ST.java:689 — unequal target stacks canPut 0; identical-but-full target rooms 0.
		ItemStackHandler tFrom = handler(1, stone(10));

		ItemStackHandler tForeign = handler(1, dirt(3));
		assertEquals(0, GTItemMover.move(tFrom, tForeign));
		assertEquals(10, tFrom.getStackInSlot(0).getCount());
		assertEquals(3, tForeign.getStackInSlot(0).getCount());

		ItemStackHandler tFull = handler(1, stone(64));
		assertEquals(0, GTItemMover.move(tFrom, tFull));
		assertEquals(10, tFrom.getStackInSlot(0).getCount());
		assertEquals(64, tFull.getStackInSlot(0).getCount());
	}

	@Test
	public void partialMergeFillsToSlotLimitWithExactBooks() {
		// ST.java:689 equal branch: room = 64 - 30 = 34; return == source loss == target gain.
		ItemStackHandler tFrom = handler(1, stone(40));
		ItemStackHandler tTo = handler(1, stone(30));

		assertEquals(34, GTItemMover.move(tFrom, tTo));
		assertEquals(6, tFrom.getStackInSlot(0).getCount());
		assertEquals(64, tTo.getStackInSlot(0).getCount());
	}

	@Test
	public void emptyTargetTakesUpToDefaultMaxMove() {
		// Oversized source stack; movable = min(64, 64) = 64 caps the transfer (ST.java:470).
		ItemStackHandler tFrom = handler(1, stone(100));
		ItemStackHandler tTo = handler(1);

		assertEquals(64, GTItemMover.move(tFrom, tTo));
		assertEquals(36, tFrom.getStackInSlot(0).getCount());
		assertEquals(64, tTo.getStackInSlot(0).getCount());
	}

	@Test
	public void sourceSmallerThanRoomMovesAll() {
		ItemStackHandler tFrom = handler(1, stone(10));
		ItemStackHandler tTo = handler(2);

		// ST.java:618 — the movable amount is capped by the source count.
		assertEquals(10, GTItemMover.move(tFrom, tTo));
		assertTrue(tFrom.getStackInSlot(0).isEmpty());
		assertEquals(10, tTo.getStackInSlot(0).getCount());
		assertTrue(tTo.getStackInSlot(1).isEmpty());
	}

	@Test
	public void firstMatchAscendingTargetSlot() {
		// ST.java:468-474 — empty source slots are skipped, targets fill from slot 0 up.
		ItemStackHandler tFrom = handler(2, ItemStack.EMPTY, stone(5));
		ItemStackHandler tTo = handler(2);

		assertEquals(5, GTItemMover.move(tFrom, tTo));
		assertEquals(5, tTo.getStackInSlot(0).getCount());
		assertTrue(tTo.getStackInSlot(1).isEmpty());
	}

	@Test
	public void firstMatchAscendingSourceSlotSkipsForeignPairs() {
		// ST.java:465-475 — source slot 0 (stone) cannot enter the dirt target and is passed over;
		// source slot 1 (dirt) merges with the room that is left.
		ItemStackHandler tFrom = handler(2, stone(4), dirt(7));
		ItemStackHandler tTo = handler(1, dirt(60));

		assertEquals(4, GTItemMover.move(tFrom, tTo));
		assertEquals(4, tFrom.getStackInSlot(0).getCount());
		assertEquals(3, tFrom.getStackInSlot(1).getCount());
		assertEquals(64, tTo.getStackInSlot(0).getCount());
	}

	@Test
	public void nonStackableItemsNeverMerge() {
		// WATER_BUCKET stacksTo(1) (vanilla Items.java:1000) — room onto an identical bucket is 0.
		ItemStackHandler tFrom = handler(1, new ItemStack(Items.WATER_BUCKET, 1));
		ItemStackHandler tTo = handler(1, new ItemStack(Items.WATER_BUCKET, 1));

		assertEquals(0, GTItemMover.move(tFrom, tTo));
		assertEquals(1, tFrom.getStackInSlot(0).getCount());

		// Onto an empty slot it does move, exactly one.
		ItemStackHandler tEmpty = handler(1);
		assertEquals(1, GTItemMover.move(tFrom, tEmpty));
		assertTrue(tFrom.getStackInSlot(0).isEmpty());
		assertEquals(1, tEmpty.getStackInSlot(0).getCount());
	}

	@Test
	public void moveFromPinnedSourceSlot() {
		// CoverRobotArm.java:87/:89 shape — take from one fixed slot, targets scan ascending.
		ItemStackHandler tFrom = handler(3, ItemStack.EMPTY, stone(8), dirt(4));
		ItemStackHandler tTo = handler(2);

		assertEquals(0, GTItemMover.moveFrom(tFrom, tTo, 0)); // empty fixed slot
		assertEquals(8, GTItemMover.moveFrom(tFrom, tTo, 1)); // the stone slot
		assertTrue(tFrom.getStackInSlot(1).isEmpty());
		assertEquals(8, tTo.getStackInSlot(0).getCount());
		assertTrue(tTo.getStackInSlot(1).isEmpty());

		assertEquals(4, GTItemMover.moveFrom(tFrom, tTo, 2)); // next interval takes the dirt
		assertTrue(tFrom.getStackInSlot(2).isEmpty());
	}

	@Test
	public void moveFromOutOfRangeSlotsReturnZero() {
		// ST.java:514 (negative) and :517 (>= size) — no read, no write.
		ItemStackHandler tFrom = handler(2, stone(1));
		ItemStackHandler tTo = handler(1);

		assertEquals(0, GTItemMover.moveFrom(tFrom, tTo, -1));
		assertEquals(0, GTItemMover.moveFrom(tFrom, tTo, 2));
		assertEquals(1, tFrom.getStackInSlot(0).getCount());
		assertTrue(tTo.getStackInSlot(0).isEmpty());
	}

	@Test
	public void moveToPinnedTargetSlot() {
		// CoverRobotArm.java:93/:95 shape — put into one fixed slot, sources scan ascending.
		ItemStackHandler tFrom = handler(2, stone(5), dirt(7));
		ItemStackHandler tTo = handler(2, stone(3), ItemStack.EMPTY);

		assertEquals(5, GTItemMover.moveTo(tFrom, tTo, 0));
		assertTrue(tFrom.getStackInSlot(0).isEmpty());
		assertEquals(8, tTo.getStackInSlot(0).getCount());

		assertEquals(7, GTItemMover.moveTo(tFrom, tTo, 1));
		assertTrue(tFrom.getStackInSlot(1).isEmpty());
		assertEquals(7, tTo.getStackInSlot(1).getCount());
	}

	@Test
	public void moveToFullSameItemSlotReturnsZero() {
		// ST.java:689 equal branch with zero room; ST.java:538/:544 bounds still hold.
		ItemStackHandler tFrom = handler(1, stone(5));
		ItemStackHandler tTo = handler(1, stone(64));

		assertEquals(0, GTItemMover.moveTo(tFrom, tTo, 0));
		assertEquals(5, tFrom.getStackInSlot(0).getCount());
		assertEquals(64, tTo.getStackInSlot(0).getCount());
	}

	@Test
	public void moveToOutOfRangeSlotsReturnZero() {
		// ST.java:538 (negative) and :544 (>= size).
		ItemStackHandler tFrom = handler(1, stone(5));
		ItemStackHandler tTo = handler(2);

		assertEquals(0, GTItemMover.moveTo(tFrom, tTo, -3));
		assertEquals(0, GTItemMover.moveTo(tFrom, tTo, 2));
		assertEquals(5, tFrom.getStackInSlot(0).getCount());
		for (int i = 0; i < 2; i++) {
			assertTrue(tTo.getStackInSlot(i).isEmpty());
		}
	}

	@Test
	public void quantityCapMaxMoveClampsTheTransfer() {
		// ST.java:470 Math.min(aMaxMove, ...) — 8 of 40 move, and only 8 (first-match single transfer).
		ItemStackHandler tFrom = handler(1, stone(40));
		ItemStackHandler tTo = handler(2);

		assertEquals(8, GTItemMover.move(tFrom, tTo, 8, 1, 64));
		assertEquals(32, tFrom.getStackInSlot(0).getCount());
		assertEquals(8, tTo.getStackInSlot(0).getCount());
		assertTrue(tTo.getStackInSlot(1).isEmpty());
	}

	@Test
	public void minMoveGatesBothSides() {
		// ST.java:467 — a source stack smaller than aMinMove is never a candidate.
		ItemStackHandler tSmallSource = handler(1, stone(5));
		ItemStackHandler tTo = handler(1);
		assertEquals(0, GTItemMover.move(tSmallSource, tTo, 64, 8, 64));
		assertEquals(5, tSmallSource.getStackInSlot(0).getCount());
		assertTrue(tTo.getStackInSlot(0).isEmpty());

		// ST.java:471 — room below aMinMove skips the pair.
		ItemStackHandler tFrom = handler(1, stone(40));
		ItemStackHandler tTight = handler(1, stone(60));
		assertEquals(0, GTItemMover.move(tFrom, tTight, 64, 8, 64));
		assertEquals(40, tFrom.getStackInSlot(0).getCount());
		assertEquals(60, tTight.getStackInSlot(0).getCount());
	}

	@Test
	public void slotSizeCapClampsTheTransfer() {
		// ST.java:470 — aMaxSize = min(aMaxSlotSize, source maxStackSize) clamps canPut's room.
		ItemStackHandler tFrom = handler(1, stone(40));
		ItemStackHandler tTo = handler(2);

		assertEquals(16, GTItemMover.move(tFrom, tTo, 64, 1, 16));
		assertEquals(24, tFrom.getStackInSlot(0).getCount());
		assertEquals(16, tTo.getStackInSlot(0).getCount());
		assertTrue(tTo.getStackInSlot(1).isEmpty());
	}

	@Test
	public void itemMaxStackSizeClampsRoom() {
		// ENDER_PEARL stacksTo(16) (vanilla Items.java:1097) — room can never exceed the item's own cap.
		ItemStackHandler tFrom = handler(1, new ItemStack(Items.ENDER_PEARL, 40));
		ItemStackHandler tTo = handler(1);

		assertEquals(16, GTItemMover.move(tFrom, tTo));
		assertEquals(24, tFrom.getStackInSlot(0).getCount());
		assertEquals(16, tTo.getStackInSlot(0).getCount());
	}

	@Test
	public void isItemValidGateReachesTheMover() {
		// ST.java:690/:692 validity gates live inside insertItem (ItemStackHandler.java:69) —
		// a slot that can never take the item yields movable 0, no trace on either side.
		ItemStackHandler tFrom = handler(1, stone(5));
		GTItemStackHandler tFiltered = new GTItemStackHandler(1, () -> {}, aStack -> aStack.getItem() == Items.DIAMOND);

		assertEquals(0, GTItemMover.move(tFrom, tFiltered));
		assertEquals(5, tFrom.getStackInSlot(0).getCount());
		assertTrue(tFiltered.getStackInSlot(0).isEmpty());
	}

	@Test
	public void probesLeaveNoTraceOnTheChangeHook() {
		// The canPut simulate insert and the canTake simulate extract must not mutate anything —
		// ItemStackHandler.java:91-102 only touches state when simulate is false.
		ItemStackHandler tFrom = handler(1, stone(5));
		int[] tHookCalls = {0};
		GTItemStackHandler tHooked = new GTItemStackHandler(2, () -> tHookCalls[0]++);

		// A rejected move (foreign filter) probes both slots and fires nothing.
		GTItemStackHandler tRejecting = new GTItemStackHandler(1, () -> tHookCalls[0]++, aStack -> aStack.getItem() == Items.DIAMOND);
		assertEquals(0, GTItemMover.move(tFrom, tRejecting));
		assertEquals(0, tHookCalls[0]);

		// A successful move fires exactly once — the real insert; the probe stayed silent.
		assertEquals(5, GTItemMover.move(tFrom, tHooked));
		assertEquals(1, tHookCalls[0]);
	}

	@Test
	public void sameHandlerIdenticalSlotPairAborts() {
		// ST.java:616-617 verbatim — with from == to the first movable pair is (0, 0), whose
		// transfer aborts the whole call with 0. Upstream quirk, ported as-is (the covers never
		// pass the same handler twice; pinned so a future refactor cannot silently change it).
		ItemStackHandler tSame = handler(2, stone(32));

		assertEquals(0, GTItemMover.move(tSame, tSame));
		assertEquals(32, tSame.getStackInSlot(0).getCount());
		assertTrue(tSame.getStackInSlot(1).isEmpty());
	}

	@Test
	public void nullAndEmptyHandlersReturnZero() {
		ItemStackHandler tOne = handler(1, stone(1));

		assertEquals(0, GTItemMover.move(null, tOne));
		assertEquals(0, GTItemMover.move(tOne, null));
		assertEquals(0, GTItemMover.move(null, null));

		// Zero-slot handlers have no candidate pairs.
		assertEquals(0, GTItemMover.move(handler(0), tOne));
		assertEquals(0, GTItemMover.move(tOne, handler(0)));
		assertEquals(0, GTItemMover.moveFrom(handler(0), tOne, 0));
		assertEquals(0, GTItemMover.moveTo(tOne, handler(0), 0));
	}

	// ---------------------------------------------------------------------------
	// the :467 filter gate (task p31-retriever-cover — the ST.java:457 filter pair)
	// ---------------------------------------------------------------------------

	@Test
	public void filterGateAdmitsOnlyTheFilterItem() {
		// ST.java:467 — aFilter set, aInvertFilter F: skip when contains(stack) == F,
		// i.e. only the filter item moves; the first scan order still applies.
		ItemStackHandler tFrom = handler(3, dirt(10), stone(10), stone(5));
		ItemStackHandler tTo = handler(1);

		assertEquals(10, GTItemMover.move(tFrom, tTo, stone(1), false));
		assertEquals(10, tTo.getStackInSlot(0).getCount(), "the first stone slot moved");
		assertEquals(10, tFrom.getStackInSlot(0).getCount(), "the dirt slot was refused untouched");
		assertEquals(5, tFrom.getStackInSlot(1).getCount() + tFrom.getStackInSlot(2).getCount(),
				"exactly one stone stack moved");
	}

	@Test
	public void filterGateInvertedAdmitsEverythingBut() {
		// ST.java:467 — aInvertFilter T: skip when contains(stack) == T, i.e. only
		// NON-matching stacks move.
		ItemStackHandler tFrom = handler(3, stone(10), dirt(10), dirt(5));
		ItemStackHandler tTo = handler(1);

		assertEquals(10, GTItemMover.move(tFrom, tTo, stone(1), true));
		assertTrue(tTo.getStackInSlot(0).getItem() == Items.DIRT, "the first non-stone stack moved");
		assertEquals(10, tFrom.getStackInSlot(0).getCount(), "the matching stone slot stayed");
	}

	@Test
	public void nullFilterPullsAnything() {
		// ST.java:66 upstream — ST.invalid(tStack) ? null : hashset: an unset filter lane
		// passes null = no gate at all.
		ItemStackHandler tFrom = handler(2, dirt(10), stone(10));
		ItemStackHandler tTo = handler(2);

		assertEquals(10, GTItemMover.move(tFrom, tTo, null, false));
		assertTrue(tTo.getStackInSlot(0).getItem() == Items.DIRT);
		assertEquals(10, GTItemMover.move(tFrom, tTo, null, true));
		assertTrue(tTo.getStackInSlot(1).getItem() == Items.STONE, "invert is moot with a null filter");
	}

	@Test
	public void filterIsNBTAndCountInsensitive() {
		// the ItemStackSet.contains(stack, T) match — item identity only. A damaged/enchanted
		// stack still matches the plain filter item.
		ItemStackHandler tFrom = handler(1, stone(10));
		ItemStackHandler tTo = handler(1);
		ItemStack tFilter = stone(64);

		assertEquals(10, GTItemMover.move(tFrom, tTo, tFilter, false));
		assertEquals(0, tFrom.getStackInSlot(0).getCount());
	}
}
