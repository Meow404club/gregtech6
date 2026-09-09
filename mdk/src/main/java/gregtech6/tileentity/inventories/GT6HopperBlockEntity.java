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
 * The regular GT6 hopper — 1.20.1 counterpart of
 * gregtech/tileentity/inventories/MultiTileEntityHopper.java:62-301 over the shared
 * {@link GT6HopperBaseBlockEntity} engine.
 *
 * <p>Kind semantics (each clause anchored):
 * <ul>
 * <li>the batch axis {@code mMode = 0..64} (:64, screwdriver-cycled :128-135) with the
 *     monkey-wrench exact/divisible toggle (:137-141) and the soft-hammer reset (:142-147,
 *     the reset arm itself cut with the missing soft-hammer item — the cycle reaches the
 *     same states);</li>
 * <li>the output loop :177-184 verbatim: rounds of {@code min(mMode, budget)} items while
 *     {@code tMovedItems + (mMode<=0?1:mMode) <= 64}, exact mode breaks after the first
 *     successful round, divisible mode keeps emitting within the 64 budget;</li>
 * <li>the per-slot cap :248 verbatim {@code mMode<=0 ? 64 : mMode*Math.max(1, 64/mMode)} —
 *     the divisible-stack arithmetic (mode 16 → slots hold 4×16);</li>
 * <li>slot access :245 (all slots ascending from every side), insert refused on the output
 *     face :246, extract refused on the output face without the {@code mLock} belt :247;</li>
 * <li>the slot compaction inside the mCheck gate (:211-224, the inventory settles toward
 *     slot 0);</li>
 * <li>suction lands in the HIGHEST empty slot (the descending :195-196 scan).</li>
 * </ul>
 */
public class GT6HopperBlockEntity extends GT6HopperBaseBlockEntity {

	/** BET factory for BlockEntityType.Builder.of — resolves the shared type at runtime. */
	public GT6HopperBlockEntity(BlockPos aPos, BlockState aState) {
		this(GTBlockEntities.HOPPER_BE.get(), aPos, aState);
	}

	/** Full constructor — the offline (test) entry point. */
	public GT6HopperBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType, aPos, aState);
		mMode = 0; // upstream :64 mMode = 0 (the NBT-default, writeToNBT2 :76)
	}

	@Override
	public String getTileEntityName() {
		return "hopper"; // upstream :295 "gt.multitileentity.hopper" — the BET registry path mirrors it
	}

	@Override
	protected int inventorySize(BlockState aState) {
		// Loader_MultiTileEntities.java:145 — NBT_INV_SIZE = Math.max(1, aHopperSize)
		return aState.getBlock() instanceof GT6Hoppers.GT6HopperBlock tBlock && !tBlock.row().queue()
				? Math.max(1, tBlock.row().slots())
				: 3; // the offline/plain-state fallback = the Bronze anchor row (Loader :191)
	}

	@Override
	public int stackLimit() {
		// upstream :248 verbatim — the divisible-stack arithmetic
		return mMode <= 0 ? 64 : mMode * Math.max(1, 64 / mMode);
	}

	@Override
	protected byte defaultMode() {
		return 0;
	}

	@Override
	protected void clampMode() {
		if (mMode < 0) mMode = 0;
		if (mMode > 64) mMode = 64;
	}

	@Override
	protected byte modeMin() {
		return 0;
	}

	@Override
	protected byte modeMax() {
		return 64;
	}

	@Override
	protected boolean compressInsideGate() {
		return true; // the hopper runs the compaction inside the mCheck/redstone gate (:211)
	}

	@Override
	protected int compressPhase() {
		return compressPhaseHopper();
	}

	@Override
	protected int findSuctionSlot() {
		// upstream :195-196 — the DESCENDING scan: the highest empty slot takes the drop
		for (int i = invsize() - 1; i >= 0; i--) {
			if (!slotHas(i)) return i;
		}
		return -1;
	}

	@Override
	public int[] getAccessibleSlotsFromSide(byte aSide) {
		// upstream :245 — UT.Code.getAscendingArray(invsize()), every side
		int[] rSlots = new int[invsize()];
		for (int i = 0; i < rSlots.length; i++) rSlots[i] = i;
		return rSlots;
	}

	@Override
	public boolean canInsertItem(int aSlot, ItemStack aStack, byte aSide) {
		return aSide != getFacing(); // upstream :246
	}

	@Override
	public boolean canExtractItem(int aSlot, byte aSide) {
		return mLock || aSide != getFacing(); // upstream :247
	}

	@Override
	public boolean hasExactMode() {
		return true;
	}

	/** Upstream :177-184 verbatim — the ≤64-budget emission loop over the output face. */
	@Override
	protected int moveOutPhase() {
		int rMoved = 0;
		if (outputGatesOpen()) {
			IItemHandler tTarget = outputTarget();
			if (tTarget != null) {
				while (rMoved + (mMode <= 0 ? 1 : mMode) <= 64) {
					mLock = true;
					int tMoved = GTItemMover.move(sideView(getFacing()), tTarget,
							mMode <= 0 ? 64 - rMoved : mMode, // aMaxMove (:179)
							mMode <= 0 ? 1 : mMode,           // aMinMove (:179)
							64);                              // aMaxSize (:179)
					mLock = false;
					if (tMoved <= 0) break;
					rMoved += tMoved;
					if (mExactMode) break; // upstream :183
				}
			}
		}
		return rMoved;
	}
}
