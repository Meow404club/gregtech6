package gregtech6.gui;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * Minimal Menu/Screen demo pair backing the {@code /gt6gui} debug command — proves the whole
 * framework chain (MenuType registration, MenuProvider opening, screen binding, player inventory
 * binding, quick-move) with no block entity behind it. The example machine card
 * (p3-example-machine) swaps the {@link SimpleContainer} stand-in for the real machine inventory
 * and replaces {@link #stillValid(Player)} with the BE check (upstream ContainerCommon.java:326
 * {@code mTileEntity.isUseableByPlayerGUI}); ContainerData progress sync (upstream
 * ContainerCommonBasicMachine.java:277-297) is likewise out of this card's scope per ADR-P3.
 */
public class GTDebugMenu extends GTGuiMenu {

    /** Panel layout: three demo slots centred at y=24 on the standard 176x166 panel. */
    public static final int CONTENT_SLOT_COUNT = 3;
    public static final int PLAYER_INVENTORY_OFFSET = 84; // rows 84/102/120, hotbar 142 (AbstractFurnaceMenu.java:52-57 geometry)

    /** Stand-in for the machine inventory until a real BE exists. */
    public final SimpleContainer content = new SimpleContainer(CONTENT_SLOT_COUNT);

    public GTDebugMenu(int containerId, Inventory playerInventory) {
        super(GTMenuTypes.gtDebug(), containerId, playerInventory);
        for (int i = 0; i < CONTENT_SLOT_COUNT; i++) {
            // (176 - 3*18)/2 = 61: centred row of three 18px slots
            addSlot(new Slot(this.content, i, 61 + i * 18, 24));
        }
        bindPlayerInventory(PLAYER_INVENTORY_OFFSET);
    }

    /**
     * IContainerFactory shape (IContainerFactory.java:15): the network path constructs through this
     * ctor. The debug GUI carries no extra payload, so {@code extraData} is always empty/null
     * (vanilla openMenu bridges the two-arg MenuSupplier with data=null, IContainerFactory.java:18-21).
     */
    public GTDebugMenu(int containerId, Inventory playerInventory, @Nullable FriendlyByteBuf extraData) {
        this(containerId, playerInventory);
    }

    /**
     * Debug GUI: no BE binding to validate against, so the menu stays open unconditionally (vanilla
     * closes only on player disconnect/menu close). Real machines must delegate to
     * {@code container.stillValid(player)} / a distance check.
     */
    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return quickMoveBetween(player, index, CONTENT_SLOT_COUNT);
    }

    /** Component for the screen title, matching the open-packet display name. */
    public static Component title() {
        return Component.literal("GT6 Debug GUI");
    }
}
