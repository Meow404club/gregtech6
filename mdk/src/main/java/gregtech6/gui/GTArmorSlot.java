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
        //? if forge {
        return this.equipmentSlot == LivingEntity.getEquipmentSlotForItem(stack);
        //?} else {
        /*net.minecraft.world.item.Equipable tEquipable = net.minecraft.world.item.Equipable.get(stack);
        //21.1: LivingEntity.getEquipmentSlotForItem went instance-side (canUseSlot entity gate,
        //javap 21.1.249) and Equipable moved to world.item — the menu has no entity, so the
        //item-based Equipable route is the same dispatch minus the gate (players accept every
        //equipment slot).
        return tEquipable != null && this.equipmentSlot == tEquipable.getEquipmentSlot();
        *///?}
    }

    @Override
    public int getMaxStackSize() {
        return 1; // Slot_Armor.java:42-44
    }
}
