package gregtech6.gui.machines;

import javax.annotation.Nullable;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.items.SlotItemHandler;

import gregtech6.gui.GTGuiMenu;
import gregtech6.tileentity.GTItemStackHandler;
import gregtech6.tileentity.machines.TileEntityOven;

/**
 * Oven menu — direct translation of the Furnace shape of upstream
 * gregapi/gui/ContainerCommonBasicMachine.java addSlots (:45-271) onto the 1.20.1 Menu
 * model: getGUIServer (MultiTileEntityBasicMachine.java:1008) becomes this constructor,
 * the client half (getGUIClient2 :1007 / ContainerClientBasicMachine) is replaced by the
 * statically registered {@link GTOvenScreen}.
 *
 * <p>Slot geometry (RM.Furnace = 1/1/1f/1f, container order verbatim :49/:55/:162/:267-268):
 * the special slot first — content index 2 at (80,43) —, then the input (content 0 at
 * (53,25)), the output (content 1 at (107,25), setCanPut(F) → {@link OutputSlot}), then the
 * two fluid display slots (contents 3/4 at (53,63)/(107,63), Slot_Render →
 * {@link RenderSlot}; the upstream "Extract using a Tap or Nozzle" tooltip is meaningless
 * with no fluid tanks and stays out). The player inventory binds at the standard 176x166
 * machine-panel offset 84 (ContainerCommon bindPlayerInventory default shape).
 *
 * <p>The single progress {@link ContainerData} is the :273-297 verbatim three-state
 * translation: mSuccessful → Short.MAX_VALUE, mMaxProgress &gt; 0 →
 * units(min(mMaxProgress, mProgress), mMaxProgress, Short.MAX_VALUE, T) normalized, else -1
 * (:280-286). The server menu computes it live on every broadcastChanges poll; the client
 * menu caches it through set() and the screen reads {@link #getProgressBar()}.
 */
public class GTOvenMenu extends GTGuiMenu {

	/** Success flag value (upstream :281, Short.MAX_VALUE). */
	public static final int PROGRESS_DONE = Short.MAX_VALUE;

	/** Content slot count: special + input + output + 2 fluid displays. */
	public static final int CONTENT_SLOT_COUNT = 5;

	/** The BE bound as the menu's backing container (upstream mTileEntity, ContainerCommon.java:42). */
	public final TileEntityOven tileEntity;

	/** Client-side progress cache, written by the vanilla data-slot sync (upstream mProgressBar :273). */
	private int mProgressBar = -1;

	public GTOvenMenu(MenuType<?> aMenuType, int aContainerId, Inventory aPlayerInventory, TileEntityOven aTileEntity) {
		super(aMenuType, aContainerId, aPlayerInventory);
		this.tileEntity = aTileEntity;

		GTItemStackHandler tInventory = aTileEntity.getInventory();
		// upstream ContainerCommonBasicMachine.addSlots :45-270 — Furnace shape, container order verbatim
		addSlot(new SlotItemHandler(tInventory, TileEntityOven.SLOT_SPECIAL, 80, 43));  // :49 special slot
		addSlot(new SlotItemHandler(tInventory, TileEntityOven.SLOT_INPUT, 53, 25));    // :55 input (mInputItemsCount == 1)
		addSlot(new OutputSlot(tInventory, TileEntityOven.SLOT_OUTPUT, 107, 25));       // :162 output setCanPut(F)
		addSlot(new RenderSlot(tInventory, TileEntityOven.SLOT_FLUID_IN_DISPLAY, 53, 63));  // :267 fluid display in
		addSlot(new RenderSlot(tInventory, TileEntityOven.SLOT_FLUID_OUT_DISPLAY, 107, 63)); // :268 fluid display out
		bindPlayerInventory(84); // standard machine panel (ContainerCommon.java:327-332 default offset)
		addDataSlots(this.mProgressData);
	}

	/** IContainerFactory shape — resolves the BE from the BlockPos payload (chest menu precedent, NetworkHooks.openScreen :176). */
	public GTOvenMenu(int aContainerId, Inventory aPlayerInventory, @Nullable FriendlyByteBuf aExtraData) {
		this(GTOvenMenus.oven(), aContainerId, aPlayerInventory, resolveTileEntity(aPlayerInventory, aExtraData));
	}

	private static TileEntityOven resolveTileEntity(Inventory aPlayerInventory, @Nullable FriendlyByteBuf aExtraData) {
		if (aExtraData == null) {
			throw new IllegalArgumentException("gt6:oven menu requires the NetworkHooks.openScreen BlockPos payload (no data bridge)");
		}
		if (aPlayerInventory.player.level().getBlockEntity(aExtraData.readBlockPos()) instanceof TileEntityOven tTileEntity) {
			return tTileEntity;
		}
		throw new IllegalArgumentException("gt6:oven menu opened without its block entity (chunk not loaded?)");
	}

	/**
	 * The three-state progress value (upstream ContainerCommonBasicMachine.java:280-286
	 * verbatim). Server-authoritative: this is the exact function the vanilla data-slot sync
	 * polls every broadcastChanges.
	 */
	public int computeProgressValue() {
		if (this.tileEntity.mSuccessful) {
			return PROGRESS_DONE; // :281
		}
		if (this.tileEntity.mMaxProgress > 0) { // :282
			return (int) TileEntityOven.units(
					Math.min(this.tileEntity.mMaxProgress, this.tileEntity.mProgress),
					this.tileEntity.mMaxProgress, Short.MAX_VALUE, true); // :283
		}
		return -1; // :285
	}

	/** The received progress value on the client (upstream mProgressBar :273/:295); the screen's bar source. */
	public int getProgressBar() {
		return mProgressBar;
	}

	/** The 1-slot ContainerData (upstream index 0, :279-297). */
	private final ContainerData mProgressData = new ContainerData() {
		@Override
		public int get(int aIndex) {
			return aIndex == 0 ? computeProgressValue() : 0;
		}

		@Override
		public void set(int aIndex, int aValue) {
			if (aIndex == 0) mProgressBar = aValue; // upstream updateProgressBar :294-296
		}

		@Override
		public int getCount() {
			return 1;
		}
	};

	/** Upstream canInteractWith → isUseableByPlayerGUI (chest menu shape: !isDead, distance² ≤ 64). */
	@Override
	public boolean stillValid(Player aPlayer) {
		return !this.tileEntity.isDead() && !this.tileEntity.isRemoved()
			&& aPlayer.distanceToSqr(
				this.tileEntity.getBlockPos().getX() + 0.5D,
				this.tileEntity.getBlockPos().getY() + 0.5D,
				this.tileEntity.getBlockPos().getZ() + 0.5D) <= 64.0D;
	}

	@Override
	public ItemStack quickMoveStack(Player aPlayer, int aIndex) {
		return quickMoveBetween(aPlayer, aIndex, CONTENT_SLOT_COUNT);
	}

	/**
	 * The output slot: items come out, nothing goes in (upstream Slot_Normal.setCanPut(F),
	 * ContainerCommonBasicMachine.java:162).
	 */
	private static class OutputSlot extends SlotItemHandler {
		OutputSlot(GTItemStackHandler aInventory, int aIndex, int aX, int aY) {
			super(aInventory, aIndex, aX, aY);
		}

		@Override
		public boolean mayPlace(ItemStack aStack) {
			return false;
		}
	}

	/**
	 * The fluid display slot: fully inert (upstream Slot_Render, ContainerCommonBasicMachine
	 * .java:267-268 — nothing in, nothing out).
	 */
	private static class RenderSlot extends SlotItemHandler {
		RenderSlot(GTItemStackHandler aInventory, int aIndex, int aX, int aY) {
			super(aInventory, aIndex, aX, aY);
		}

		@Override
		public boolean mayPlace(ItemStack aStack) {
			return false;
		}

		@Override
		public boolean mayPickup(Player aPlayer) {
			return false;
		}
	}
}
