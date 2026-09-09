package gregtech6.tileentity.inventories;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The GT6 Bottle Crate — 1.20.1 counterpart of
 * {@code gregtech/tileentity/inventories/MultiTileEntityBottleCrate.java:54-249}, the
 * half-height 9-bottle crate (the metalset row :144 "Bottlecrate (%s)" id 8600+aID over
 * the wooden 300-ladder :180 — the port rows are the vanilla-planks subset, the declared
 * wave-4 deviation, same fold as the bookshelf).
 *
 * <h2>The inventory face</h2>
 * <ul>
 * <li>9 slots ({@code mDisplay[9]} :56); sided access: the base-default ascending table
 *     (upstream no override — hoppers see all nine);</li>
 * <li>insert gate :247-249 port: the bottle family — potion, glass bottle, XP bottle, or
 *     any item whose crafting remainder is a glass bottle (the {@code ST.container}
 *     check; the BoP/HBM jars fold with their mods, a declared fold);</li>
 * <li>extract :246 = true; stack limit = the base default 64 (upstream no override —
 *     glass bottles stack through).</li>
 * </ul>
 *
 * <h2>The keepSlot fold (upstream :244-245 {@code canDrop = F} + {@code keepSlot = T})</h2>
 * <p>Upstream: the crate ITEM keeps its contents in NBT when broken. The port: the block's
 * onRemove drops ONE BlockItem carrying {@code BlockEntityTag} (the vanilla shulker-box
 * convention — BlockItem.updateCustomBlockEntityTag re-applies it on placement), so the
 * BE save/load pair below IS the content carrier. No per-slot pop on this block.</p>
 *
 * <h2>The no-tick folds</h2>
 * <ul>
 * <li>the pixel 3x3 slot picker (:148-151) folds to the card's shift-all/single-take
 *     ruling — the interaction lives on the BLOCK use face;</li>
 * <li>the mDisplay[] bottle-render sync (36 render passes, :161-249) is the render pool —
 *     placeholder block art (the hopper-family precedent);</li>
 * <li>{@code getDefaultStack :240} (fresh crates pre-filled with an empty bottle) folds —
 *     the port crate ships empty (a pre-fill would fight the keepSlot round trip).</li>
 * </ul>
 */
public class GT6BottleCrateBlockEntity extends GT6StaticStorageBaseBlockEntity {

	/** The slot count (upstream {@code mDisplay[9]} :56 — the 3x3 crate). */
	public static final int INVENTORY_SIZE = 9;

	/** Full constructor — the offline (test) entry point. */
	public GT6BottleCrateBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType, aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "bottlecrate"; // upstream "gt.multitileentity.bottlecrate" family — the BET path mirrors it
	}

	@Override
	protected int inventorySize(BlockState aState) {
		return INVENTORY_SIZE;
	}

	/**
	 * Upstream :247-249 — the bottle family gate: potion / glass bottle / XP bottle, or an
	 * item whose crafting remainder is a glass bottle (the {@code ST.container} arm; the
	 * BoP/HBM jar arms fold with their mods).
	 */
	public static boolean isBottleFamily(ItemStack aStack) {
		if (aStack.isEmpty()) return false;
		if (aStack.is(Items.POTION) || aStack.is(Items.GLASS_BOTTLE) || aStack.is(Items.EXPERIENCE_BOTTLE)) return true;
		return aStack.hasCraftingRemainingItem() && aStack.getCraftingRemainingItem().is(Items.GLASS_BOTTLE);
	}

	@Override
	public int[] getAccessibleSlotsFromSide(byte aSide) {
		// the base-default ascending table (upstream no override — all nine from every side)
		int[] rSlots = new int[INVENTORY_SIZE];
		for (int i = 0; i < INVENTORY_SIZE; i++) rSlots[i] = i;
		return rSlots;
	}

	@Override
	public boolean canInsertItem(int aSlot, ItemStack aStack, byte aSide) {
		return isBottleFamily(aStack); // upstream :247-249
	}

	@Override
	public boolean canExtractItem(int aSlot, byte aSide) {
		return true; // upstream :246
	}

	/** The first empty slot (the click insert pick of the shift-all/single-take ruling). */
	public int firstEmpty() {
		for (int i = 0; i < INVENTORY_SIZE; i++) {
			if (!slotHas(i)) return i;
		}
		return -1;
	}

	/** The last occupied slot (the single-take pick). */
	public int lastOccupied() {
		for (int i = INVENTORY_SIZE - 1; i >= 0; i--) {
			if (slotHas(i)) return i;
		}
		return -1;
	}
}
