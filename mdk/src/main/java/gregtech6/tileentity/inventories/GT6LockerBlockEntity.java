package gregtech6.tileentity.inventories;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregtech6.registry.GTBlockEntities;


/**
 * The GT6 Locker — 1.20.1 counterpart of
 * {@code gregtech/tileentity/inventories/MultiTileEntityLocker.java} (114 lines), the
 * four-slot ARMOR locker of the metalset row :138 ("Locker (Mat)", id 7300+aID, no
 * NBT_INV_SIZE — the {@code getDefaultInventory :82} size 4 is the registration).
 *
 * <p>Semantics, each clause anchored:
 * <ul>
 * <li>4 slots, all four open to automation from every side ({@code :83
 *     getAccessibleSlotsFromSide2 = getAscendingArray(4)});</li>
 * <li>insert gate :84 verbatim {@code isValidArmor(aStack, 3-aSlot)} — locker slot i holds
 *     the armor piece of 1.7.10 type 3-i, i.e. the slot order mirrors the player armor row
 *     bottom-up: 0 boots, 1 leggings, 2 chestplate, 3 helmet (the modern mapping is the
 *     {@link Mob#getEquipmentSlotForItem} piece check against {@link #SLOT_PIECES}[i]);
 *     the Throwable-swallowed third argument (aPlayer) folds away — 1.20.1's piece
 *     resolution needs no entity;</li>
 * <li>extract gate :85 = always true; stack limit 64 (the base default);</li>
 * <li>the interaction arm :58-79 lives on the BLOCK use face ({@code onBlockActivated3}
 *     ported there like every in-repo container block): right-clicking the front face
 *     SWAPS the player's four armor pieces with the locker contents — armor-type-matching
 *     pieces trade places, the empty locker slot takes the worn piece. The upstream
 *     backpack keep-out ({@code IL.BTRS_* :65}) folds: those mod items do not exist in the
 *     port universe;</li>
 * <li>no GUI upstream (the class overrides no getGUI*): the swap IS the interface —
 *     zero MenuType, zero MUI panel for this one, exactly the card's menu=null ruling by
 *     vacuity;</li>
 * <li>the adjacency wake :49-55 rides the base class event-driven fold.</li>
 * </ul>
 */
public class GT6LockerBlockEntity extends GT6StaticStorageBaseBlockEntity {

	/** The slot count (upstream :82 getDefaultInventory = ItemStack[4]). */
	public static final int INVENTORY_SIZE = 4;

	/**
	 * The armor piece per locker slot: index i holds the piece of 1.7.10 type 3-i —
	 * 0 boots, 1 leggings, 2 chestplate, 3 helmet (the player armor-row order, class doc).
	 */
	public static final EquipmentSlot[] SLOT_PIECES = {
			EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};

	/** BET factory for BlockEntityType.Builder.of — resolves the shared type at runtime. */
	public GT6LockerBlockEntity(BlockPos aPos, BlockState aState) {
		this(GTBlockEntities.LOCKER_BE.get(), aPos, aState);
	}

	/** Full constructor — the offline (test) entry point. */
	public GT6LockerBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType, aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "locker"; // upstream :113 "gt.multitileentity.locker.normal" — the BET path mirrors it
	}

	@Override
	protected int inventorySize(BlockState aState) {
		return INVENTORY_SIZE; // the :82 constant — no NBT_INV_SIZE on the :138 row
	}

	/** The armor-piece gate of slot i — upstream :84 {@code isValidArmor(aStack, 3-aSlot)}. */
	public static boolean isValidArmorForSlot(ItemStack aStack, int aSlot) {
		if (aStack.isEmpty() || aSlot < 0 || aSlot >= INVENTORY_SIZE) return false;
		//? if forge {
		return Mob.getEquipmentSlotForItem(aStack) == SLOT_PIECES[aSlot]; // the 1.20.1 static face
		//?} else {
		/*// 21.1: the resolver went instance-side (LivingEntity.getEquipmentSlotForItem) and the
		//Equipable.get entry point is package-private — the ArmorItem face covers every armor
		//piece (vanilla + modded ArmorItem armor), the declared fold.
		return aStack.getItem() instanceof net.minecraft.world.item.ArmorItem tArmor
				&& tArmor.getEquipmentSlot() == SLOT_PIECES[aSlot];
		*///?}
	}

	@Override
	public int[] getAccessibleSlotsFromSide(byte aSide) {
		// upstream :83 — all four slots ascending from every side
		return new int[] {0, 1, 2, 3};
	}

	@Override
	public boolean canInsertItem(int aSlot, ItemStack aStack, byte aSide) {
		return isValidArmorForSlot(aStack, aSlot); // upstream :84
	}

	@Override
	public boolean canExtractItem(int aSlot, byte aSide) {
		return true; // upstream :85
	}
}
