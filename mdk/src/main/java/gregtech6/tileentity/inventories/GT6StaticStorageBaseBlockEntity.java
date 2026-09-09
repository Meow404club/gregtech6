package gregtech6.tileentity.inventories;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

//? if forge {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
//?}
import net.minecraftforge.items.IItemHandler;

import gregtech6.tileentity.GTItemStackHandler;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The shared skeleton of the static storage batch (task p26-storage-static-batch): the
 * no-tick pure slot-face posture of the five upstream storage containers
 * {@code MultiTileEntityLocker.java} / {@code MultiTileEntityDrawerQuad.java} /
 * {@code MultiTileEntitySafe.java} / {@code MultiTileEntityBookShelf.java} /
 * {@code MultiTileEntityBottleCrate.java} — all five extend TileEntityBase09FacingSingle
 * and carry nothing but an inventory, a facing and their interaction arms.
 *
 * <h2>The no-tick fold (the card headline "无 tick 纯槽面，onTick2 空")</h2>
 * <p>The base mounts with {@code mIsTicking = false} (the TestMachineBlock notick chain
 * equivalence): {@code canUpdate()} never holds, so the 03 dispatcher never runs — the
 * exact semantic of the upstream static containers whose {@code onTick2} carries no
 * machine work.
 *
 * <h2>The adjacency-notify fold (declared equivalence)</h2>
 * <p>Upstream Locker :49-55 and DrawerQuad :70-76 wake neighbouring
 * {@code ITileEntityAdjacentInventoryUpdatable} machines from {@code onTick2} whenever
 * {@code mInventoryChanged} — a tick-coalesced notify. The port fires it EVENT-DRIVEN:
 * every inventory mutation (the {@link GTItemStackHandler} content hook) notifies
 * immediately instead of at the next tick. Strictly-not-later than upstream (the tick arm
 * fires ≤1 tick after the change), zero ticks burned, and the in-repo receiver side is the
 * card-① {@link GT6AdjacentInventoryUpdatable} interface with the identical neighbour walk
 * ({@link GT6HopperBaseBlockEntity#notifyAdjacentInventories} shape).
 *
 * <h2>The side view (upstream getAccessibleSlotsFromSide2/canInsertItem2/canExtractItem2)</h2>
 * <p>The external automation face is a per-side {@link IItemHandler} view over the real
 * inventory: the slot mapping comes from the subclass access table, insert/extract from the
 * subclass gates — the same posture as the hopper family's SideItemHandler. The Forge leg
 * answers {@code getCapability(ITEM_HANDLER, side)} directly; the 21.1 leg exposes the same
 * seam member ({@code BlockEntity} lost the capability override there — 21.1.249 javap) and
 * the GT6CapabilityWiring rows hand the BETs to {@code registerBlockEntity} with it. The
 * root's RAW whole-inventory exposure is OVERRIDDEN on purpose: the raw handler would bypass
 * the per-side slot tables and the insert gates (the safe must answer a 0-slot view, not its
 * 15 real slots).
 */
public abstract class GT6StaticStorageBaseBlockEntity extends TileEntityBase03TicksAndSync {

	/** NBT key of {@link #mFacing} (the plain in-repo form, GTExampleChestBlockEntity precedent). */
	public static final String NBT_FACING = "facing";

	/** NBT key of the slot contents (the plain in-repo form; the ItemStackHandler list shape). */
	public static final String NBT_INVENTORY = "inventory";

	/** GT6 SIDE_ANY — the side-less capability view ("every side" for the access table). */
	public static final byte SIDE_ANY = 6;

	/** Upstream 09FacingSingle:45 mFacing default SIDE_FRONT; the blockstate FACING is the live truth. */
	protected byte mFacing = 3;

	/** Full constructor — also the offline (test) entry point (the hopper-family shape). */
	protected GT6StaticStorageBaseBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(false, aType, aPos, aState); // the no-tick chain: canUpdate() never holds
		setInventory(new GTItemStackHandler(inventorySize(aState), this::updateInventory));
	}

	/** The registration slot count — read off the block carrier or the class constant. */
	protected abstract int inventorySize(BlockState aState);

	/** The BET registry path (the getTileEntityName convention — the BET row mirrors it). */
	@Override
	public abstract String getTileEntityName();

	// ---------------------------------------------------------------------------
	// facing (upstream 09FacingSingle; the blockstate FACING is the live truth)
	// ---------------------------------------------------------------------------

	/** The front face: the blockstate FACING when present, the NBT byte otherwise. */
	public byte getFacing() {
		BlockState tState = getBlockState();
		if (tState != null && tState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
			return (byte) tState.getValue(BlockStateProperties.HORIZONTAL_FACING).get3DDataValue();
		}
		return mFacing;
	}

	/** The NBT-fallback byte write (the block's setPlacedBy keeps it in step). */
	public void setFacingNbtFallback(byte aFacing) {
		mFacing = aFacing;
	}

	// ---------------------------------------------------------------------------
	// the 05 inventory vocabulary (the hopper-base shape)
	// ---------------------------------------------------------------------------

	public GTItemStackHandler getInventory() {
		return mInventory;
	}

	protected int invsize() {
		return mInventory.getSlots();
	}

	protected boolean slotHas(int aIndex) {
		return !mInventory.getStackInSlot(aIndex).isEmpty();
	}

	protected ItemStack slot(int aIndex) {
		return mInventory.getStackInSlot(aIndex);
	}

	protected void slot(int aIndex, @Nullable ItemStack aStack) {
		mInventory.setStackInSlot(aIndex, aStack == null ? ItemStack.EMPTY : aStack);
	}

	/**
	 * Upstream 05:103 updateInventory — the change point. The port adds the adjacency wake
	 * here (the class-doc fold): setChanged + the immediate neighbour notify.
	 */
	public void updateInventory() {
		setChanged();
		notifyAdjacentInventories();
	}

	/**
	 * The upstream Locker :51-54 / DrawerQuad :72-75 wake, event-driven (class doc): every
	 * neighbour implementing {@link GT6AdjacentInventoryUpdatable} is told which of ITS sides
	 * faces back at this container.
	 */
	protected void notifyAdjacentInventories() {
		if (!hasLevel() || isClientSide()) return;
		for (byte tSide = 0; tSide < 6; tSide++) {
			BlockEntity tNeighbor = getLevel().getBlockEntity(getBlockPos().relative(Direction.from3DDataValue(tSide)));
			if (tNeighbor instanceof GT6AdjacentInventoryUpdatable) {
				((GT6AdjacentInventoryUpdatable) tNeighbor).adjacentInventoryUpdated(
						(byte) Direction.from3DDataValue(tSide).getOpposite().get3DDataValue(), this);
			}
		}
	}

	// ---------------------------------------------------------------------------
	// the side view + capability (class doc)
	// ---------------------------------------------------------------------------

	/** The side slot mapping — upstream getAccessibleSlotsFromSide2. */
	public abstract int[] getAccessibleSlotsFromSide(byte aSide);

	/** Upstream canInsertItem2. */
	public abstract boolean canInsertItem(int aSlot, ItemStack aStack, byte aSide);

	/** Upstream canExtractItem2. */
	public abstract boolean canExtractItem(int aSlot, byte aSide);

	/** The per-slot stack cap — upstream getInventoryStackLimit (the GUI + the view share it). */
	public int stackLimit() {
		return 64;
	}

	/** The side view handed to every external consumer (the hopper SideItemHandler posture). */
	public IItemHandler sideView(byte aSide) {
		return new SideItemHandler(this, aSide);
	}

	/** The per-side handler view; the side is part of the identity (fresh per call). */
	public static final class SideItemHandler implements IItemHandler {

		private final GT6StaticStorageBaseBlockEntity mTE;
		private final byte mSide;
		private final int[] mSlots;

		SideItemHandler(GT6StaticStorageBaseBlockEntity aTE, byte aSide) {
			mTE = aTE;
			mSide = aSide;
			mSlots = aTE.getAccessibleSlotsFromSide(aSide);
		}

		@Override
		public int getSlots() {
			return mSlots.length;
		}

		@Override
		public ItemStack getStackInSlot(int aViewSlot) {
			return mTE.mInventory.getStackInSlot(realSlot(aViewSlot));
		}

		@Override
		public ItemStack insertItem(int aViewSlot, ItemStack aStack, boolean aSimulate) {
			int tReal = realSlot(aViewSlot);
			if (!mTE.canInsertItem(tReal, aStack, mSide)) return aStack;
			return mTE.mInventory.insertItem(tReal, aStack, aSimulate);
		}

		@Override
		public ItemStack extractItem(int aViewSlot, int aAmount, boolean aSimulate) {
			int tReal = realSlot(aViewSlot);
			if (!mTE.canExtractItem(tReal, mSide)) return ItemStack.EMPTY;
			return mTE.mInventory.extractItem(tReal, aAmount, aSimulate);
		}

		@Override
		public int getSlotLimit(int aViewSlot) {
			return mTE.stackLimit();
		}

		@Override
		public boolean isItemValid(int aViewSlot, ItemStack aStack) {
			return mTE.canInsertItem(realSlot(aViewSlot), aStack, mSide);
		}

		private int realSlot(int aViewSlot) {
			return mSlots[aViewSlot];
		}
	}

	/** The GT6 byte of a Direction (the Direction.getIndex order — the 01Root port doc). */
	public static byte sideOf(@Nullable Direction aSide) {
		return aSide == null ? SIDE_ANY : (byte) aSide.get3DDataValue();
	}

	// ---------------------------------------------------------------------------
	// capability exposure (the class-doc override posture)
	// ---------------------------------------------------------------------------

	//? if forge {
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> aCapability, @Nullable Direction aSide) {
		if (aCapability == ForgeCapabilities.ITEM_HANDLER) {
			return LazyOptional.of(() -> sideView(sideOf(aSide))).cast();
		}
		return super.getCapability(aCapability, aSide);
	}
	//?} else {
	/*// 21.1: BlockEntity carries no capability override (21.1.249 javap) — the seam member
	// the GT6CapabilityWiring rows delegate to (the GTItemPipeBlockEntity seam shape).
	public <T> T getCapability(net.neoforged.neoforge.capabilities.BlockCapability<T, Direction> aCapability, @Nullable Direction aSide) {
		if (aCapability == net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK) {
			return (T) sideView(sideOf(aSide));
		}
		return null;
	}
	 *///?}

	// ---------------------------------------------------------------------------
	// NBT (the plain in-repo key form; the 21.1 provider-first serialize seam)
	// ---------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putByte(NBT_FACING, mFacing);
		//? if forge {
		aNBT.put(NBT_INVENTORY, mInventory.serializeNBT());
		//?} else {
		/*aNBT.put(NBT_INVENTORY, mInventory.serializeNBT(TileEntityBase03TicksAndSync.NBT_ACCESS)); // 21.1: provider-first
		 *///?}
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_FACING, Tag.TAG_ANY_NUMERIC)) {
			mFacing = aNBT.getByte(NBT_FACING);
		}
		if (aNBT.contains(NBT_INVENTORY, Tag.TAG_COMPOUND)) {
			//? if forge {
			mInventory.deserializeNBT(aNBT.getCompound(NBT_INVENTORY));
			//?} else {
			/*mInventory.deserializeNBT(TileEntityBase03TicksAndSync.NBT_ACCESS, aNBT.getCompound(NBT_INVENTORY)); // 21.1: provider-first
			 *///?}
		}
	}
}
