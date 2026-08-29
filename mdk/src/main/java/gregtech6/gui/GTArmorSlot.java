package gregtech6.gui;

import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * GT6 armor slot — direct translation of gregapi/gui/Slot_Armor.java:29: stack limit 1, placeable
 * only when the item is valid armor for the slot. The 1.7.10 validity check
 * ({@code item.isValidArmor(stack, armorType, player)}) maps to the vanilla 1.20.1 idiom
 * {@code slot == LivingEntity.getEquipmentSlotForItem(stack)} used by InventoryMenu.java:70-71.
 * The old {@code EntityPlayer} parameter is gone because the 1.20.1 check no longer needs the
 * entity (EquipmentSlot replaces the 0-3 {@code mArmorType} int).
 */
public class GTArmorSlot extends Slot {

    /** The armor body position this slot feeds. */
    public final EquipmentSlot equipmentSlot;

    public GTArmorSlot(Container container, int index, int x, int y, EquipmentSlot equipmentSlot) {
        super(container, index, x, y);
        this.equipmentSlot = equipmentSlot;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return this.equipmentSlot == LivingEntity.getEquipmentSlotForItem(stack);
    }

    @Override
    public int getMaxStackSize() {
        return 1; // Slot_Armor.java:42-44
    }
}
