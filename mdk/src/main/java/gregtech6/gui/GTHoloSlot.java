package gregtech6.gui;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * GT6 holo (ghost) slot — direct translation of gregapi/gui/Slot_Holo.java:32 (1.7.10 semantics
 * onto the 1.20.1 Slot capability model, ADR-P3-6: the click behaviour that upstream intercepted
 * in ContainerCommon.slotClick is expressed here through mayPlace/mayPickup/remove instead):
 * <ul>
 * <li>{@code isItemValid} -> {@link #mayPlace(ItemStack)} gated on {@code mCanInsertItem};</li>
 * <li>{@code canTakeStack} (upstream hardcoded {@code false}) -> {@link #mayPickup(Player)} always
 * {@code false} — players can drop items in but never pull them out with the mouse;</li>
 * <li>{@code getSlotStackLimit} (upstream default 127) -> {@link #getMaxStackSize()} and its
 * per-stack overload, so >64 ghost stacks stay possible;</li>
 * <li>{@code getHasStack} (upstream hardcoded {@code false}) -> {@link #hasItem()} always
 * {@code false} — shift-click insertion treats the slot as always empty, and the default
 * screen rendering draws nothing for it (ghost content is drawn by the GUI itself, as upstream
 * does in its own render pass);</li>
 * <li>{@code decrStackSize} -> {@link #remove(int)} gated on {@code mCanStackItem}.</li>
 * </ul>
 */
public class GTHoloSlot extends Slot {

    /** Upstream {@code mCanInsertItem}: may items be placed into this slot at all. */
    public final boolean canInsertItem;
    /** Upstream {@code mCanStackItem}: may items be pulled back out through {@link #remove(int)}. */
    public final boolean canStackItem;
    /** Upstream {@code mMaxStacksize}: insertion stack cap, 127 upstream default. */
    public final int maxStacksize;

    public GTHoloSlot(Container container, int index, int x, int y, boolean canInsertItem, boolean canStackItem, int maxStacksize) {
        super(container, index, x, y);
        this.canInsertItem = canInsertItem;
        this.canStackItem = canStackItem;
        this.maxStacksize = maxStacksize;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return this.canInsertItem;
    }

    @Override
    public boolean mayPickup(Player player) {
        return false;
    }

    @Override
    public boolean hasItem() {
        return false;
    }

    @Override
    public int getMaxStackSize() {
        return this.maxStacksize;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        // bypass the item's own max so the 127 upstream ghost cap holds (Slot_Holo.java:41-44)
        return this.maxStacksize;
    }

    @Override
    public ItemStack remove(int amount) {
        if (!this.canStackItem) return ItemStack.EMPTY;
        return super.remove(amount);
    }
}
