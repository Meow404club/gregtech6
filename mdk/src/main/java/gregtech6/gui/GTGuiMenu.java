package gregtech6.gui;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Base menu for all GT6 GUIs — direct translation of the common-container half of upstream
 * gregapi/gui/ContainerCommon.java:40 (the slotClick global-intercept half is deferred to the
 * ADR-P3-6 pool; per-slot semantics live in the {@link Slot} subclasses of this package instead).
 *
 * <p>What survived the port verbatim: the 36-slot player inventory binding
 * (ContainerCommon.java:327-332 {@code bindPlayerInventory}, skeleton identical to vanilla
 * AbstractFurnaceMenu.java:50-58). {@code canInteractWith} becomes {@link #stillValid(Player)} and
 * {@code transferStackInSlot} becomes {@link #quickMoveStack(Player, int)} — both stay abstract
 * here because their upstream bodies are tile-entity-bound (canInteractWith delegates to
 * {@code mTileEntity.isUseableByPlayerGUI}, ContainerCommon.java:326), which is the example
 * machine card's (p3-example-machine) job to wire.
 */
public abstract class GTGuiMenu extends AbstractContainerMenu {

    /** Upstream {@code mInventoryPlayer} (ContainerCommon.java:43). */
    public final Inventory inventoryPlayer;

    /** Slot index of the first bound player-inventory slot (content slots are added before {@link #bindPlayerInventory(int)} runs). */
    protected int playerInventoryStart;

    protected GTGuiMenu(MenuType<?> menuType, int containerId, Inventory playerInventory) {
        super(menuType, containerId); // AbstractContainerMenu.java:63
        this.inventoryPlayer = playerInventory;
        this.playerInventoryStart = 0;
    }

    /**
     * Direct translation of ContainerCommon.java:327-332: 3x9 main rows plus the 9-slot hotbar,
     * 18px grid starting at x=8, hotbar at {@code offset + 58} (so {@code offset=84} matches the
     * standard 176x166 panel of AbstractFurnaceMenu.java:52-57). The caller decides the offset;
     * content slots must be added first so {@link #playerInventoryStart} lands after them.
     */
    protected void bindPlayerInventory(int offset) {
        this.playerInventoryStart = this.slots.size();
        for (int i = 0; i < 3; i++) for (int j = 0; j < 9; j++) {
            addSlot(new Slot(this.inventoryPlayer, j + i * 9 + 9, 8 + j * 18, offset + i * 18));
        }
        for (int i = 0; i < 9; i++) {
            addSlot(new Slot(this.inventoryPlayer, i, 8 + i * 18, offset + 58));
        }
    }

    /** Slot window of the bound player inventory ({@link #playerInventoryStart} to {@code +36}). */
    public int playerInventoryStart() {
        return this.playerInventoryStart;
    }

    /**
     * Vanilla shift-click pattern (AbstractFurnaceMenu.java:107-160 shape) parameterised over the
     * content/player boundary — concrete menus only state their two index ranges.
     *
     * @param contentSlotEnd exclusive end of the menu's own (non-player) slots; content slots start at 0
     * @return the moved stack copy, or {@link ItemStack#EMPTY} when nothing moved
     */
    protected ItemStack quickMoveBetween(Player player, int index, int contentSlotEnd) {
        ItemStack copied = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack moving = slot.getItem();
            copied = moving.copy();
            if (index < this.playerInventoryStart) {
                // content -> player inventory, hotbar-first (vanilla reverse=true, AbstractFurnaceMenu.java:125)
                if (!this.moveItemStackTo(moving, this.playerInventoryStart, this.playerInventoryStart + 36, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(moving, 0, contentSlotEnd, false)) {
                return ItemStack.EMPTY;
            }
            if (moving.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return copied;
    }
}
