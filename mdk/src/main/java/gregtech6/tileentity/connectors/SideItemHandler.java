package gregtech6.tileentity.connectors;

import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

//? if forge {
import net.minecraftforge.items.IItemHandler;
//?} else {
/*import net.neoforged.neoforge.items.IItemHandler;
 *///?}

/**
 * The side-aware {@link IItemHandler} wrapper over a {@link GTItemPipeBlockEntity} —
 * the item-face twin of {@link SideFluidHandler} (task p26-pipe-item spec ①): the
 * 1.20.1 IItemHandler carries no Direction parameter, so the side travels through the
 * {@code getCapability(ITEM_HANDLER, Direction)} wrapper (the fresh-per-call
 * SideFluidHandler posture — the side is part of the handler identity).
 *
 * <p>This is the verbatim home of the upstream per-side insert/extract gates
 * (MultiTileEntityPipeItem.java:268-269):
 * <ul>
 * <li>{@link #insertItem} routes through {@link GTItemPipeBlockEntity#canInsertItem}
 *     — the ONE-WAY LATCH (connected + input-enabled face, the latched side, an empty
 *     slot) before the internal handler sees the stack;</li>
 * <li>{@link #extractItem} routes through {@link GTItemPipeBlockEntity#canExtractItem}
 *     (upstream :269 — side-less queries and connected faces extract).</li>
 * </ul>
 * The pipe's own transfer loop never touches this wrapper — it moves straight out of
 * {@link GTItemPipeBlockEntity#mInventory} (the upstream SIDE_ANY full view, :236).
 */
public class SideItemHandler implements IItemHandler {

	private final GTItemPipeBlockEntity mTile;
	/** The GT6 side index (0..5, Direction.get3DDataValue order) this handler fronts; -1 for the side-less query. */
	private final byte mSide;

	public SideItemHandler(GTItemPipeBlockEntity aTile, byte aSide) {
		mTile = aTile;
		mSide = aSide;
	}

	public SideItemHandler(GTItemPipeBlockEntity aTile, @Nullable Direction aSide) {
		this(aTile, (byte)(aSide == null ? -1 : aSide.get3DDataValue()));
	}

	@Override
	public int getSlots() {
		return mTile.mInventory.getSlots();
	}

	@Override
	public ItemStack getStackInSlot(int aSlot) {
		return mTile.mInventory.getStackInSlot(aSlot);
	}

	@Override
	public int getSlotLimit(int aSlot) {
		return mTile.mInventory.getSlotLimit(aSlot);
	}

	@Override
	public boolean isItemValid(int aSlot, ItemStack aStack) {
		// the latch side of the upstream canInsertItem2 is the VALIDITY face: a side that
		// can never insert reports invalid for everything (the probe-friendly form of :268)
		return mTile.canInsertItem(aSlot, aStack, mSide) && mTile.mInventory.isItemValid(aSlot, aStack);
	}

	/** Upstream :268 via canInsertItem, then the internal handler performs the slot arithmetic. */
	@Override
	public ItemStack insertItem(int aSlot, ItemStack aStack, boolean aSimulate) {
		if (aStack.isEmpty()) return aStack;
		if (!mTile.canInsertItem(aSlot, aStack, mSide)) return aStack;
		return mTile.mInventory.insertItem(aSlot, aStack, aSimulate);
	}

	/** Upstream :269 via canExtractItem, then the internal handler performs the take. */
	@Override
	public ItemStack extractItem(int aSlot, int aAmount, boolean aSimulate) {
		if (aAmount <= 0) return ItemStack.EMPTY;
		if (!mTile.canExtractItem(aSlot, mTile.mInventory.getStackInSlot(aSlot), mSide)) return ItemStack.EMPTY;
		return mTile.mInventory.extractItem(aSlot, aAmount, aSimulate);
	}
}
