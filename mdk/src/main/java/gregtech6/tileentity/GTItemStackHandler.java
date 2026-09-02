package gregtech6.tileentity;

import java.util.function.Predicate;

import org.jetbrains.annotations.NotNull;

import net.minecraft.world.item.ItemStack;

import net.minecraftforge.items.ItemStackHandler;

/**
 * GT6 inventory carrier — isomorphic to GTCEu Modern CustomItemStackHandler
 * (api/transfer/item/CustomItemStackHandler.java:18-52, ADR-P3-2): a content-change
 * hook the owning BlockEntity binds to {@code setChanged()} (Forge ItemStackHandler
 * calls onContentsChanged on every mutating path — setStackInSlot :45, insertItem
 * :101, extractItem :128/:141), plus a per-slot insert validity filter (:26, :45-47).
 */
public class GTItemStackHandler extends ItemStackHandler {

	protected Runnable mOnContentsChanged = () -> {};
	protected Predicate<ItemStack> mFilter = stack -> true;

	public GTItemStackHandler(int aSize) {
		super(aSize);
	}

	/** Hook constructor: BEs bind {@code this::setChanged} so mutation marks the BE dirty (same shape as CustomItemStackHandler.java:50-52). */
	public GTItemStackHandler(int aSize, Runnable aOnContentsChanged) {
		super(aSize);
		mOnContentsChanged = aOnContentsChanged;
	}

	/** Hook constructor with a per-slot insert filter (same shape as CustomItemStackHandler.java:26, :45-47). */
	public GTItemStackHandler(int aSize, Runnable aOnContentsChanged, Predicate<ItemStack> aFilter) {
		super(aSize);
		mOnContentsChanged = aOnContentsChanged;
		mFilter = aFilter;
	}

	public Runnable getOnContentsChanged() {
		return mOnContentsChanged;
	}

	public void setOnContentsChanged(Runnable aOnContentsChanged) {
		mOnContentsChanged = aOnContentsChanged;
	}

	public Predicate<ItemStack> getFilter() {
		return mFilter;
	}

	public void setFilter(Predicate<ItemStack> aFilter) {
		mFilter = aFilter;
	}

	@Override
	public boolean isItemValid(int aSlot, @NotNull ItemStack aStack) {
		return mFilter.test(aStack);
	}

	@Override
	public void onContentsChanged(int aSlot) {
		mOnContentsChanged.run();
	}
}
