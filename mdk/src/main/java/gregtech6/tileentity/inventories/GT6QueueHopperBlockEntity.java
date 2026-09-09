package gregtech6.tileentity.inventories;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import net.minecraftforge.items.IItemHandler;

import gregtech6.registry.GTBlockEntities;
import gregtech6.registry.GT6Hoppers;
import gregtech6.util.GTItemMover;

/**
 * The GT6 queue hopper — 1.20.1 counterpart of
 * gregtech/tileentity/inventories/MultiTileEntityQueueHopper.java:62-283 over the shared
 * {@link GT6HopperBaseBlockEntity} engine.
 *
 * <p>Kind semantics (each clause anchored):
 * <ul>
 * <li>the FIFO axis {@code mMode = 1..64} (:64, screwdriver-cycled :121-128, the :70
 *     {@code <= 0 → 64} load clamp) — the PER-SLOT cap (:230
 *     {@code getInventoryStackLimit() = mMode}), no exact axis (no monkeywrench arm);</li>
 * <li>the output loop :164-171 verbatim: rounds toward the {@code mMode} budget, per-slot
 *     capped at {@code mMode}, aMinMove 1;</li>
 * <li>the FIFO compaction fixed point OUTSIDE the mCheck gate (:199-207 — an inventory
 *     change re-compacts even while redstone-blocked): every slot pushes toward the END
 *     until a full pass moves nothing, so slot 0 = the newest and the last slot = the
 *     oldest;</li>
 * <li>slot access :227 = {0, last} from every side; inserts only into slot 0 (:228, the
 *     tail), extraction only from the last slot (:229, the head, output face gated by the
 *     {@code mLock} belt);</li>
 * <li>suction lands in the LAST slot only (:182-183, the tail), keeping the FIFO order.</li>
 * </ul>
 */
public class GT6QueueHopperBlockEntity extends GT6HopperBaseBlockEntity {

	/** BET factory for BlockEntityType.Builder.of — resolves the shared type at runtime. */
	public GT6QueueHopperBlockEntity(BlockPos aPos, BlockState aState) {
		this(GTBlockEntities.QUEUE_HOPPER_BE.get(), aPos, aState);
	}

	/** Full constructor — the offline (test) entry point. */
	public GT6QueueHopperBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType, aPos, aState);
		mMode = 64; // upstream :64 mMode = 64 (the per-slot default)
	}

	@Override
	public String getTileEntityName() {
		return "queue_hopper"; // upstream :277 "gt.multitileentity.queuehopper" — the BET path mirrors it
	}

	@Override
	protected int inventorySize(BlockState aState) {
		// Loader_MultiTileEntities.java:146 — NBT_INV_SIZE = Math.max(2, aHopperSize)
		return aState.getBlock() instanceof GT6Hoppers.GT6HopperBlock tBlock && tBlock.row().queue()
				? Math.max(2, tBlock.row().slots())
				: 3; // the offline/plain-state fallback = the Bronze anchor row (Loader :191)
	}

	@Override
	public int stackLimit() {
		return mMode; // upstream :230 verbatim — the per-slot cap IS the batch axis
	}

	@Override
	protected byte defaultMode() {
		return 64;
	}

	@Override
	protected void clampMode() {
		if (mMode <= 0) mMode = 64; // upstream :70 verbatim
	}

	@Override
	protected byte modeMin() {
		return 1;
	}

	@Override
	protected byte modeMax() {
		return 64;
	}

	@Override
	protected boolean compressInsideGate() {
		return false; // the queue lifts the FIFO compaction out of the gate (:199)
	}

	@Override
	protected int compressPhase() {
		return compressPhaseQueue();
	}

	@Override
	protected int findSuctionSlot() {
		// upstream :182-183 — the LAST slot only (the FIFO tail)
		int i = invsize() - 1;
		return !slotHas(i) ? i : -1;
	}

	@Override
	public int[] getAccessibleSlotsFromSide(byte aSide) {
		// upstream :227 — {0, invsize()-1}, every side
		return new int[] {0, invsize() - 1};
	}

	@Override
	public boolean canInsertItem(int aSlot, ItemStack aStack, byte aSide) {
		return aSlot == 0; // upstream :228 — the tail takes the inserts (any side)
	}

	@Override
	public boolean canExtractItem(int aSlot, byte aSide) {
		// upstream :229 — the head (the last slot) yields, output face gated by the mLock belt
		return aSlot == invsize() - 1 && (mLock || aSide != getFacing());
	}

	@Override
	public boolean hasExactMode() {
		return false;
	}

	/** Upstream :164-171 verbatim — the mMode-budget emission loop over the output face. */
	@Override
	protected int moveOutPhase() {
		int rMoved = 0;
		if (outputGatesOpen()) {
			IItemHandler tTarget = outputTarget();
			if (tTarget != null) {
				while (rMoved < mMode) {
					mLock = true;
					int tMoved = GTItemMover.move(sideView(getFacing()), tTarget,
							mMode - rMoved, // aMaxMove (:166)
							1,              // aMinMove (:166)
							mMode);         // aMaxSize (:166 — the per-slot cap rides the move)
					mLock = false;
					if (tMoved <= 0) break;
					rMoved += tMoved;
				}
			}
		}
		return rMoved;
	}
}
