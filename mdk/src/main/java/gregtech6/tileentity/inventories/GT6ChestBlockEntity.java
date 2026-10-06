package gregtech6.tileentity.inventories;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.PanelSyncManager;

import gregtech6.gui.machines.GT6ChestMUI;
import gregtech6.gui.machines.GT6MuiMachine;
import gregtech6.registry.GT6Chests;

/**
 * The metal-chest family BE (task material-mc-a-storage-chests — the upstream
 * {@code MultiTileEntityChest} (gregapi/block/multitileentity/example/MultiTileEntityChest.java)
 * over the metalset chest pair :132-133, the {@link GT6StaticStorageBaseBlockEntity} no-tick
 * posture): 54 slots (the NBT_INV_SIZE column, :132/:133), the facing rides the blockstate
 * with the NBT fallback byte.
 *
 * <p><b>The tick folds</b> (each with its carrier): the onTick adjacency wake (:137-144)
 * is the base class-doc EVENT-DRIVEN fold ({@link #updateInventory} notifies immediately);
 * the lid animation + opener sync (:148-157/:160-168) fold with the TESR (the
 * {@code GTExampleChestBlockEntity} omissions — the blockstate facade renders static); the
 * trapped arm (:305-306) folds with mIsTrapped (never set by a metalset row); the 1200-tick
 * opener resync (:145-147) has no getOpenGUIs consumer.
 *
 * <p><b>The automation face</b> is the MTE default (all slots, all sides, no filter —
 * the {@code isItemValidForSlot} == T posture the example chest cites).
 *
 * <p><b>The pincers face</b> ({@link #pincersTransfer}) is the upstream onToolClick
 * TOOL_pincers arm (:182-229) verbatim in five passes: merge-equal stacks against the whole
 * player inventory first (:187-194), then NBT-less stackables into the main inventory
 * (:196-201), then stackables regardless of NBT (:203-208), then NBT-carrying unstackables
 * (:210-215), then everything else (:217-222) — passes 2-5 scan from the vanilla
 * main-inventory head (slot 9, the hotbar stays free) and each pass only fires when the
 * previous moved nothing.
 */
public class GT6ChestBlockEntity extends GT6StaticStorageBaseBlockEntity implements GT6MuiMachine {

	/** The vanilla main-inventory head — the :189/:197/:205/:212/:219 pass 2-5 scan start (the hotbar 0..8 is the pass-1 merge territory only). */
	public static final int PLAYER_INV_HEAD = 9;

	/** The upstream player-inventory scan bound — the :183/:189/:197/:205/:212/:219 {@code j < 36} loop end (main+hotbar; Inventory.getContainerSize() is 41 with armor+offhand). */
	public static final int PLAYER_INV_BOUND = 36;

	/** BET factory for BlockEntityType.Builder.of — resolves the shared type at runtime. */
	public GT6ChestBlockEntity(BlockPos aPos, BlockState aState) {
		this(gregtech6.registry.GTBlockEntities.CHEST_BE.get(), aPos, aState);
	}

	/** Full constructor — also the offline (test) entry point: Builder.of(...).build(null) works without a registry. */
	public GT6ChestBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType, aPos, aState);
	}

	@Override
	protected int inventorySize(BlockState aState) {
		return GT6Chests.INVENTORY_SIZE; // the :132/:133 NBT_INV_SIZE 54 column
	}

	/** The BET registry path (the getTileEntityName convention — the BET row mirrors it). */
	@Override
	public String getTileEntityName() {
		return "chest";
	}

	// ---------------------------------------------------------------------------
	// the automation face (the MTE default — all slots, all sides, no filter)
	// ---------------------------------------------------------------------------

	@Override
	public int[] getAccessibleSlotsFromSide(byte aSide) {
		int[] rSlots = new int[invsize()];
		for (int i = 0; i < rSlots.length; i++) rSlots[i] = i;
		return rSlots;
	}

	@Override
	public boolean canInsertItem(int aSlot, ItemStack aStack, byte aSide) {
		return true; // the isItemValidForSlot == T posture (the 05 inventory base)
	}

	@Override
	public boolean canExtractItem(int aSlot, byte aSide) {
		return true;
	}

	// ---------------------------------------------------------------------------
	// the GUI (the wave-4 MUI ruling, zero MenuType — the 54-grid panel)
	// ---------------------------------------------------------------------------

	@Override
	public ModularPanel<?> buildUI(brachy.modularui.factory.PosGuiData aData, PanelSyncManager aSyncManager, UISettings aSettings) {
		return GT6ChestMUI.panel(this, aSyncManager);
	}

	// ---------------------------------------------------------------------------
	// the pincers transfer (upstream onToolClick :182-229, the five-pass order)
	// ---------------------------------------------------------------------------

	/**
	 * The :182-229 arm over the vanilla player inventory. Pass 1 merges equal stacks
	 * against EVERY player slot (:188-194); passes 2-5 move chest slots into the main
	 * inventory 9..35 (:196-222), each pass gated on the previous having moved nothing
	 * (:196/:203/:210/:217). The achievement check (:186 ST.check) folds with the port's
	 * no-advancement-hook face; the collect SFX rides the block use arm.
	 *
	 * @return the moved item count (the :224-228 return, 0 = nothing was done)
	 */
	public int pincersTransfer(Inventory aPlayerInventory) {
		int rCount = 0;
		// pass 1 — merge stacks first when applicable (:187-194): the move sits inside the
		// ST.equal guard upstream, so occupied-equal player slots only (no empty-slot fill)
		for (int i = 0; i < invsize(); i++) {
			if (!slotHas(i)) continue;
			for (int j = 0; j < PLAYER_INV_BOUND; j++) {
				rCount += moveIntoPlayer(aPlayerInventory, i, j, true);
				if (!slotHas(i)) break;
			}
		}
		// pass 2 — stackable NBT-less items second (:196-201): NO gate, the pass always runs
		for (int i = 0; i < invsize(); i++) {
			if (slotHas(i) && slot(i).getMaxStackSize() > 1 && !hasNbt(slot(i))) {
				for (int j = PLAYER_INV_HEAD; j < PLAYER_INV_BOUND; j++) {
					rCount += moveIntoPlayer(aPlayerInventory, i, j, false);
					if (!slotHas(i)) break;
				}
			}
		}
		// pass 3 — stackable NBT-containing items third (:203-208)
		if (rCount <= 0) for (int i = 0; i < invsize(); i++) {
			if (slotHas(i) && slot(i).getMaxStackSize() > 1) {
				for (int j = PLAYER_INV_HEAD; j < PLAYER_INV_BOUND; j++) {
					rCount += moveIntoPlayer(aPlayerInventory, i, j, false);
					if (!slotHas(i)) break;
				}
			}
		}
		// pass 4 — unstackable NBT-containing items fourth (:210-215)
		if (rCount <= 0) for (int i = 0; i < invsize(); i++) {
			if (slotHas(i) && hasNbt(slot(i))) {
				for (int j = PLAYER_INV_HEAD; j < PLAYER_INV_BOUND; j++) {
					rCount += moveIntoPlayer(aPlayerInventory, i, j, false);
					if (!slotHas(i)) break;
				}
			}
		}
		// pass 5 — unstackable NBT-less items fifth (:217-222)
		if (rCount <= 0) for (int i = 0; i < invsize(); i++) {
			if (slotHas(i)) {
				for (int j = PLAYER_INV_HEAD; j < PLAYER_INV_BOUND; j++) {
					rCount += moveIntoPlayer(aPlayerInventory, i, j, false);
					if (!slotHas(i)) break;
				}
			}
		}
		return rCount;
	}

	/**
	 * One ST.move leg (chest slot i → player slot j): as much of the chest stack as the
	 * player slot accepts, the remainder stays in the chest. The merge-only face (pass 1)
	 * refuses the empty player slot — the upstream ST.equal guard — the free face (passes
	 * 2-5) fills it.
	 */
	private int moveIntoPlayer(Inventory aPlayerInventory, int aChestSlot, int aPlayerSlot, boolean aMergeOnly) {
		ItemStack tChestStack = slot(aChestSlot);
		if (tChestStack.isEmpty()) return 0;
		ItemStack tPlayerStack = aPlayerInventory.getItem(aPlayerSlot);
		if (tPlayerStack.isEmpty()) {
			if (aMergeOnly) return 0;
			// the empty-slot fill caps at the stack's own max (the ST.move slot limit — a
			// 70-count chest slot splits across the empty slots, the surplus stays)
			int tMoved = Math.min(tChestStack.getCount(), tChestStack.getMaxStackSize());
			aPlayerInventory.setItem(aPlayerSlot, copyAt(tChestStack, tMoved));
			tChestStack.shrink(tMoved);
			if (tChestStack.isEmpty()) slot(aChestSlot, ItemStack.EMPTY);
			updateInventory();
			return tMoved;
		}
		if (!stackablePair(tPlayerStack, tChestStack)) return 0;
		int tRoom = tPlayerStack.getMaxStackSize() - tPlayerStack.getCount();
		if (tRoom <= 0) return 0;
		int tMoved = Math.min(tRoom, tChestStack.getCount());
		tPlayerStack.grow(tMoved);
		tChestStack.shrink(tMoved);
		if (tChestStack.isEmpty()) slot(aChestSlot, ItemStack.EMPTY);
		aPlayerInventory.setChanged();
		updateInventory();
		return tMoved;
	}

	/** The {@code ST.nbt(_) == null} face — the per-leg NBT-presence read (the GT6Keys.keyIdOf fork shape). */
	private static boolean hasNbt(ItemStack aStack) {
		//? if forge {
		return aStack.hasTag();
		//?} else {
		/*return aStack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA) != null; // 21.1: the components read
		 *///?}
	}

	/** The copy-at-count seam (1.20.1 ItemHandlerHelper.copyStackWithSize; 21.1 the vanilla copyWithCount) — the {@code GT6HopperBaseBlockEntity.copyAt} verbatim. */
	private static ItemStack copyAt(ItemStack aStack, int aCount) {
		//? if forge {
		return net.minecraftforge.items.ItemHandlerHelper.copyStackWithSize(aStack, aCount);
		//?} else {
		/*return aStack.copyWithCount(aCount); // 21.1: copyStackWithSize deleted — vanilla copy-at-count
		 *///?}
	}

	/** The {@code ST.equal(_, _, F)} mapping — the same predicate {@code GT6HopperBaseBlockEntity.stackablePair} uses. */
	private static boolean stackablePair(ItemStack aTo, ItemStack aFrom) {
		//? if forge {
		return net.minecraftforge.items.ItemHandlerHelper.canItemStacksStack(aTo, aFrom);
		//?} else {
		/*return net.minecraft.world.item.ItemStack.isSameItemSameComponents(aTo, aFrom); // 21.1: the canPut fork reason verbatim
		 *///?}
	}
}
