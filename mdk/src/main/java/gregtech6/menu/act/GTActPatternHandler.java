package gregtech6.menu.act;

import java.util.function.BooleanSupplier;

import net.minecraft.world.item.ItemStack;

//? if forge {
import net.minecraftforge.items.IItemHandlerModifiable;
//?} else {
/*import net.neoforged.neoforge.items.IItemHandlerModifiable;
*///?}

import gregtech6.tileentity.machines.TileEntityAdvancedCraftingTable;

/**
 * The ghost-grid GUI adapter (task p24-act-machine C2): a 9-slot
 * {@link IItemHandlerModifiable} VIEW over {@link TileEntityAdvancedCraftingTable#mPattern}
 * — the server backing stays a bare array (decisions.p24-act-ghost-form: never an
 * IItemHandler on the BE capability face, so no automation/drop leak); this adapter exists
 * only inside the ModularUI panel as the {@code PhantomItemSlot} seat, which needs the
 * handler interface for its server-side click/scroll editing
 * (PhantomItemSlotSyncHandler.readOnServer :68-79 → phantomClick → {@code slot.set}).
 *
 * <p>Semantics: every write is count-NORMALIZED to 1 and gated on the selector being
 * present in slot 30 (the upstream pattern is selector-driven — a manual ghost with no
 * selector would be killed by the next {@code getCraftingOutput} sweep anyway; the gate
 * keeps the invariant visible at the write point). Reads pass through; insert/extract
 * return untouched/EMPTY (a phantom cell is not an item face).
 */
public class GTActPatternHandler implements IItemHandlerModifiable {

	private final TileEntityAdvancedCraftingTable mTable;
	private final BooleanSupplier mSelectorPresent;

	public GTActPatternHandler(TileEntityAdvancedCraftingTable aTable, BooleanSupplier aSelectorPresent) {
		mTable = aTable;
		mSelectorPresent = aSelectorPresent;
	}

	@Override
	public int getSlots() {
		return 9;
	}

	@Override
	public ItemStack getStackInSlot(int aSlot) {
		ItemStack tGhost = mTable.mPattern[aSlot];
		return tGhost == null ? ItemStack.EMPTY : tGhost;
	}

	@Override
	public void setStackInSlot(int aSlot, ItemStack aStack) {
		if (!mSelectorPresent.getAsBoolean()) return; // the selector gate — no selector, no manual pattern
		if (aStack.isEmpty()) {
			mTable.mPattern[aSlot] = ItemStack.EMPTY;
			return;
		}
		ItemStack tGhost = aStack.copy();
		tGhost.setCount(1); // count NORMALIZED to 1 — decisions.p24-act-ghost-form
		mTable.mPattern[aSlot] = tGhost;
	}

	@Override
	public ItemStack insertItem(int aSlot, ItemStack aStack, boolean aSimulate) {
		return aStack; // a phantom cell is not an item face
	}

	@Override
	public ItemStack extractItem(int aSlot, int aAmount, boolean aSimulate) {
		return ItemStack.EMPTY;
	}

	@Override
	public int getSlotLimit(int aSlot) {
		return 1;
	}

	@Override
	public boolean isItemValid(int aSlot, ItemStack aStack) {
		return true; // the MUI phantomClick probes this before the set; the selector gate rules the write
	}
}
