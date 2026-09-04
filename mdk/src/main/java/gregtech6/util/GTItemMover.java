package gregtech6.util;

import net.minecraft.world.item.ItemStack;

import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * Port of the {@code ST.move} subset that the two item-transport covers use, re-based from the
 * 1.7.10 {@code IInventory}/{@code ISidedInventory} world onto the 1.20.1 Forge
 * {@link IItemHandler} family. Zero behavior change card: this class is consumed by W2
 * (Conveyor/RobotArm covers), nothing wires it yet.
 *
 * <h2>Archaeology — the upstream surface and its cover call sites</h2>
 *
 * <p>Upstream file: {@code gregapi/util/ST.java} (1.7.10). The three DelegatorTileEntity entry
 * points used by the covers, with every default argument spelled out:
 * <ul>
 * <li>{@code move(from, to)} — ST.java:455, delegating to :457 with
 *     {@code (null filter, F, F, F, T, 64, 1, 64, 1)} = no filter, no side ignores, no filter
 *     inversion, eject allowed, aMaxSize 64, aMinSize 1, aMaxMove 64, aMinMove 1.
 *     CoverConveyor.java:68/:70 calls it twice per timed tick (CoverConveyor.java:65-73:
 *     {@code SERVER_TIME % mTiming == 0 && !mStopped && tileEntity instanceof IInventory};
 *     visual 0 = host→neighbour, visual 1 = neighbour→host; 10 timing tiers 512&gt;&gt;i).</li>
 * <li>{@code moveFrom(from, to, slotFrom)} — ST.java:511/:513. CoverRobotArm.java:87/:89 calls it
 *     with {@code (null, T, F, F, T, 64, 1, 64, 1)} for {@code mValues[side] < 0}, fixed source
 *     slot = {@code -1 - mValues[side]} (encoding: CoverRobotArm.java:54/:64/:71-72) — the
 *     {@code aIgnoreSideFrom = T} argument is what lets the arm reach one fixed slot regardless
 *     of the source's side rules.</li>
 * <li>{@code moveTo(from, to, slotTo)} — ST.java:535/:537. CoverRobotArm.java:93/:95 calls it
 *     with {@code (null, F, T, F, T, 64, 1, 64, 1)} for {@code mValues[side] >= 0}, fixed target
 *     slot = {@code mValues[side]} — the {@code aIgnoreSideTo = T} argument bypasses the
 *     target's side rules for that one slot.</li>
 * </ul>
 *
 * <h2>Ported semantics (each clause anchored)</h2>
 * <ul>
 * <li><b>Slot ranges.</b> {@link #move}: full ascending scan of both sides (ST.java:465-475).
 *     {@link #moveFrom}: fixed source slot, ascending scan of all target slots (ST.java:524-530).
 *     {@link #moveTo}: ascending scan of all source slots, fixed target slot (ST.java:546-553).
 *     Fixed slots are bounds-checked ({@code < 0} and {@code >= getSlots()}: ST.java:514/:517,
 *     :538/:544). In the handler world the side slot-sets of ST.java:460/:463
 *     ({@code ISidedInventory.getAccessibleSlotsFromSide}) live inside the side-view
 *     {@link IItemHandler} the caller supplies; the ignoreSide flags of the robot arm are that
 *     caller's view choice (a side-less view for fixed-slot access), not a parameter here.</li>
 * <li><b>Source-slot gates</b> (ST.java:467, :523, :548): empty slot skipped; source count
 *     below aMinMove skipped; extractability probed — the handler-world equivalent of
 *     {@code canTake} (ST.java:667-678, an {@code ISidedInventory.canExtractItem} boolean) is a
 *     simulated one-item {@code extractItem} probe (IItemHandler.java:64-78 contract).</li>
 * <li><b>Per-pair movable amount</b> (ST.java:470, :526, :550): {@code min(aMaxMove, canPut(...))},
 *     where canPut's slot-size cap is {@code min(aMaxSlotSize, source stack maxStackSize)}.</li>
 * <li><b>canPut arithmetic</b> (ST.java:688-699): empty target slot →
 *     {@code min(aMaxSize, slot limit)}; stackable-identical target → that minus the existing
 *     count; anything else → 0 (ST.java:689, with {@code getInventoryStackLimit()} mapped to
 *     {@link IItemHandler#getSlotLimit}, IItemHandler.java:80-86, and upstream
 *     {@code ST.equal_(..., F)} mapped to {@link ItemHandlerHelper#canItemStacksStack},
 *     ItemHandlerHelper.java:39-45). The validity gates of ST.java:690
 *     ({@code isItemValidForSlot}) and :692 ({@code canInsertItem}) live inside
 *     {@code insertItem} in the handler world (IItemHandler.java:47-62; reference impl
 *     ItemStackHandler.java:64-105 checks isItemValid :69, canItemStacksStack :80 and the slot
 *     limit :76-87), so the authoritative fit is the simulated-insert remainder.</li>
 * <li><b>Transfer</b> ({@code ST.move_}, ST.java:615-629): same-handler same-slot pair aborts
 *     with 0 (:616-617, ported verbatim); amount capped by the source count (:618, :619);
 *     extract then insert (:620/:623 = decrStackSize / set-or-merge); return the actually moved
 *     count (:622/:628). The markDirty pair (:624-627) is implicit in the handlers.</li>
 * <li><b>First-match single transfer.</b> All three entry points transfer at most one source
 *     slot into one target slot per call and return immediately (ST.java:473, :529, :553);
 *     the covers run them on a timer, one stack per interval.</li>
 * </ul>
 *
 * <h2>Trimmed upstream surface (out of the cover subset, none reachable from the call sites)</h2>
 * <ul>
 * <li>Filter/invertFilter/ejectItems parameters (ST.java:457) — every cover call passes
 *     {@code null}/F/F/T; item filtering is the FilterItem cover's job, not the mover's.</li>
 * <li>{@code aMinSize} gate (ST.java:471 second clause) — dead at the covers' aMinSize = 1:
 *     {@code tMovable >= 1} implies {@code tMovable + existing >= 1}.</li>
 * <li>Non-inventory recipient fallback to {@code ST.put} (pipes/auto-trash, ST.java:461/:518/:542/:578)
 *     — no handler-world analog in scope; W2 passes two handlers or does not call.</li>
 * <li>{@code getPotentialDoubleChest} (ST.java:459/:462, :632-649) — a 1.20.1 double chest already
 *     reports a combined handler from its side capability (Forge VanillaDoubleChestItemHandler).</li>
 * <li>{@code moveAll} (ST.java:481-507), the both-slots-fixed {@code move} (ST.java:561-581) and
 *     the same-inventory {@code move(IInventory, ...)} family (ST.java:583-604) — unused by the
 *     covers.</li>
 * <li>Null-guard on the two handlers is a port-ism: upstream routes a null recipient into the
 *     put/eject path (ST.java:461/:703), which is trimmed here, so {@code null} just returns 0.</li>
 * </ul>
 */
public final class GTItemMover {

	/** Upstream ST.move default aMaxMove (ST.java:455/:511/:535 trailing args, first 64). */
	public static final int DEFAULT_MAX_MOVE = 64;

	/** Upstream ST.move default aMinMove (ST.java:455/:511/:535 trailing args, second 1). */
	public static final int DEFAULT_MIN_MOVE = 1;

	/** Upstream ST.move default aMaxSize — the per-slot size cap (ST.java:455/:511/:535 trailing args, third 64). */
	public static final int DEFAULT_MAX_SLOT_SIZE = 64;

	private GTItemMover() {
	}

	/**
	 * Upstream {@code ST.move(DelegatorTileEntity, DelegatorTileEntity)} (ST.java:455) at the cover
	 * defaults — full ascending scan of both sides, first movable pair transfers, one stack per call.
	 * CoverConveyor.java:68/:70 shape.
	 */
	public static int move(IItemHandler aFrom, IItemHandler aTo) {
		return move(aFrom, aTo, DEFAULT_MAX_MOVE, DEFAULT_MIN_MOVE, DEFAULT_MAX_SLOT_SIZE);
	}

	/**
	 * Core of {@link #move} with the quantity caps exposed (the surviving subset of the
	 * ST.java:457 parameter list: aMaxMove, aMinMove, aMaxSize; aMinSize trimmed as dead —
	 * see class javadoc). ST.java:465-477.
	 */
	public static int move(IItemHandler aFrom, IItemHandler aTo, int aMaxMove, int aMinMove, int aMaxSlotSize) {
		if (aFrom == null || aTo == null) return 0;
		for (int aSlotFrom = 0; aSlotFrom < aFrom.getSlots(); aSlotFrom++) {
			ItemStack aStackFrom = aFrom.getStackInSlot(aSlotFrom);
			// ST.java:467 — empty + below-minMove gates, then the canTake (:667-678) extractability probe.
			if (aStackFrom.isEmpty() || aStackFrom.getCount() < aMinMove) continue;
			if (aFrom.extractItem(aSlotFrom, 1, true).isEmpty()) continue;
			for (int aSlotTo = 0; aSlotTo < aTo.getSlots(); aSlotTo++) {
				ItemStack aStackTo = aTo.getStackInSlot(aSlotTo);
				// ST.java:470 — movable = min(aMaxMove, canPut(..., min(aMaxSize, source maxStackSize))).
				int tMovable = Math.min(aMaxMove, canPut(aTo, aSlotTo, aStackFrom, aStackTo, Math.min(aMaxSlotSize, aStackFrom.getMaxStackSize())));
				// ST.java:471 — movable < minMove skips the pair (aMinSize clause trimmed, class javadoc).
				if (tMovable < aMinMove) continue;
				// ST.java:473 — first movable pair transfers and IS the return value.
				return doMove(aFrom, aTo, aStackFrom, aSlotFrom, aSlotTo, tMovable);
			}
		}
		return 0;
	}

	/**
	 * Upstream {@code ST.moveFrom(DelegatorTileEntity, DelegatorTileEntity, int)} (ST.java:511) at
	 * the cover defaults — fixed source slot, ascending scan of the target slots, one stack per call.
	 * CoverRobotArm.java:87/:89 shape (take from slot {@code -1 - mValues[side]}).
	 */
	public static int moveFrom(IItemHandler aFrom, IItemHandler aTo, int aSlotFrom) {
		return moveFrom(aFrom, aTo, aSlotFrom, DEFAULT_MAX_MOVE, DEFAULT_MIN_MOVE, DEFAULT_MAX_SLOT_SIZE);
	}

	/**
	 * Core of {@link #moveFrom} with the quantity caps exposed. ST.java:513-532.
	 */
	public static int moveFrom(IItemHandler aFrom, IItemHandler aTo, int aSlotFrom, int aMaxMove, int aMinMove, int aMaxSlotSize) {
		if (aFrom == null || aTo == null) return 0;
		if (aSlotFrom < 0) return 0;
		if (aSlotFrom >= aFrom.getSlots()) return 0;
		ItemStack aStackFrom = aFrom.getStackInSlot(aSlotFrom);
		// ST.java:523 — the full gate of the fixed source slot, incl. the canTake probe.
		if (aStackFrom.isEmpty() || aStackFrom.getCount() < aMinMove) return 0;
		if (aFrom.extractItem(aSlotFrom, 1, true).isEmpty()) return 0;
		for (int aSlotTo = 0; aSlotTo < aTo.getSlots(); aSlotTo++) {
			ItemStack aStackTo = aTo.getStackInSlot(aSlotTo);
			int tMovable = Math.min(aMaxMove, canPut(aTo, aSlotTo, aStackFrom, aStackTo, Math.min(aMaxSlotSize, aStackFrom.getMaxStackSize())));
			if (tMovable < aMinMove) continue;
			// ST.java:529.
			return doMove(aFrom, aTo, aStackFrom, aSlotFrom, aSlotTo, tMovable);
		}
		return 0;
	}

	/**
	 * Upstream {@code ST.moveTo(DelegatorTileEntity, DelegatorTileEntity, int)} (ST.java:535) at
	 * the cover defaults — ascending scan of the source slots, fixed target slot, one stack per call.
	 * CoverRobotArm.java:93/:95 shape (put into slot {@code mValues[side]}).
	 */
	public static int moveTo(IItemHandler aFrom, IItemHandler aTo, int aSlotTo) {
		return moveTo(aFrom, aTo, aSlotTo, DEFAULT_MAX_MOVE, DEFAULT_MIN_MOVE, DEFAULT_MAX_SLOT_SIZE);
	}

	/**
	 * Core of {@link #moveTo} with the quantity caps exposed. ST.java:537-556.
	 */
	public static int moveTo(IItemHandler aFrom, IItemHandler aTo, int aSlotTo, int aMaxMove, int aMinMove, int aMaxSlotSize) {
		if (aFrom == null || aTo == null) return 0;
		if (aSlotTo < 0) return 0;
		if (aSlotTo >= aTo.getSlots()) return 0;
		for (int aSlotFrom = 0; aSlotFrom < aFrom.getSlots(); aSlotFrom++) {
			ItemStack aStackFrom = aFrom.getStackInSlot(aSlotFrom);
			// ST.java:548 — the source-slot gate of the ascending scan, incl. the canTake probe.
			if (aStackFrom.isEmpty() || aStackFrom.getCount() < aMinMove) continue;
			if (aFrom.extractItem(aSlotFrom, 1, true).isEmpty()) continue;
			ItemStack aStackTo = aTo.getStackInSlot(aSlotTo);
			int tMovable = Math.min(aMaxMove, canPut(aTo, aSlotTo, aStackFrom, aStackTo, Math.min(aMaxSlotSize, aStackFrom.getMaxStackSize())));
			if (tMovable < aMinMove) continue;
			// ST.java:553.
			return doMove(aFrom, aTo, aStackFrom, aSlotFrom, aSlotTo, tMovable);
		}
		return 0;
	}

	/**
	 * Upstream {@code ST.canPut(IInventory, byte, byte, int, ItemStack, ItemStack, int)} (ST.java:688-699)
	 * re-based onto one {@link IItemHandler} slot: the :689 room arithmetic with
	 * {@code getInventoryStackLimit()} mapped to {@link IItemHandler#getSlotLimit} and the
	 * stackability test mapped to {@link ItemHandlerHelper#canItemStacksStack}, then the :690/:692
	 * validity gates measured through a simulated insert (the handler's own insertItem is the
	 * authority — IItemHandler.java:47-62, ItemStackHandler.java:64-105). Returns how many items of
	 * {@code aStackFrom} fit into slot {@code aSlotTo} right now.
	 */
	private static int canPut(IItemHandler aTo, int aSlotTo, ItemStack aStackFrom, ItemStack aStackTo, int aMaxSize) {
		// ST.java:689 — empty → min(aMaxSize, limit); identical → that minus existing; else 0.
		int rMaxMove = aStackTo.isEmpty() ? Math.min(aMaxSize, aTo.getSlotLimit(aSlotTo))
				//? if forge {
				: ItemHandlerHelper.canItemStacksStack(aStackTo, aStackFrom) ? Math.min(aMaxSize, aTo.getSlotLimit(aSlotTo)) - aStackTo.getCount()
				//?} else {
				/*: ItemStack.isSameItemSameComponents(aStackTo, aStackFrom) ? Math.min(aMaxSize, aTo.getSlotLimit(aSlotTo)) - aStackTo.getCount()
				//21.1: ItemHandlerHelper.canItemStacksStack is gone (javap ItemHandlerHelper 21.1.249 —
				//only the insert family remains); this arm guarantees aStackTo non-empty, so the
				//same-item-same-components identity test is the exact stackability predicate.
				*///?}
				: 0;
		// ST.java:690 first clause — no room, no move.
		if (rMaxMove <= 0) return 0;
		// ST.java:690 second clause (isItemValidForSlot) + :692 (canInsertItem) — the simulated
		// remainder is the handler-world measurement of both.
		//? if forge {
		ItemStack tRest = aTo.insertItem(aSlotTo, ItemHandlerHelper.copyStackWithSize(aStackFrom, rMaxMove), true);
		//?} else {
		/*ItemStack tRest = aTo.insertItem(aSlotTo, aStackFrom.copyWithCount(rMaxMove), true); // 21.1: copyStackWithSize deleted — vanilla copyWithCount is the same copy-at-count
		*///?}
		return rMaxMove - tRest.getCount();
	}

	/**
	 * Upstream {@code ST.move_(IInventory, IInventory, ItemStack, ItemStack, int, int, int)}
	 * (ST.java:615-629) re-based: decrStackSize → {@code extractItem(EXECUTE)} (:620), set-or-merge
	 * → {@code insertItem(EXECUTE)} (:623), markDirty pair (:624-627) implicit in the handlers,
	 * return the actually moved count (:622/:628).
	 */
	private static int doMove(IItemHandler aFrom, IItemHandler aTo, ItemStack aStackFrom, int aSlotFrom, int aSlotTo, int aCount) {
		// ST.java:616-617 — verbatim guards (the covers never pass the same handler twice; the
		// identical-pair abort is ported as upstream, incl. its "abort the whole call" reachability).
		if (aFrom == aTo && aSlotFrom == aSlotTo) return 0;
		// ST.java:618-619 — cap by the source count, negative guard kept verbatim.
		aCount = Math.min(aCount, aStackFrom.getCount());
		if (aCount < 0) return 0;
		// ST.java:620-621 — extract for real; nothing extracted means nothing moves.
		ItemStack tExtracted = aFrom.extractItem(aSlotFrom, aCount, false);
		if (tExtracted.isEmpty()) return 0;
		// ST.java:622 — the actual extracted amount is what gets accounted.
		aCount = Math.min(aCount, tExtracted.getCount());
		// ST.java:623 — insert for real; the remainder is pushed back into the source to keep the
		// upstream no-loss invariant (upstream cannot reach a failed insert because canPut
		// pre-verified it at :470-473, but a handler may change state between probe and act).
		ItemStack tLeftover = aTo.insertItem(aSlotTo, tExtracted, false);
		if (!tLeftover.isEmpty()) aFrom.insertItem(aSlotFrom, tLeftover, false);
		// ST.java:628.
		return aCount - tLeftover.getCount();
	}
}
