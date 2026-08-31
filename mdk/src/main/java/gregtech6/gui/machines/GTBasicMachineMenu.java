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
import gregtech6.tileentity.machines.TileEntityBasicMachine;

/**
 * Basic-machine menu — the data-driven generalization of the oven menu (GTOvenMenu) onto the
 * RecipeMap slot shape of upstream gregapi/gui/ContainerCommonBasicMachine.java addSlots
 * (:45-271): getGUIServer (MultiTileEntityBasicMachine.java:1008) becomes this constructor,
 * the client half (getGUIClient2 :1007 / ContainerClientBasicMachine) is replaced by the
 * statically registered {@link GTBasicMachineScreen}.
 *
 * <p>Slot geometry is derived from the RecipeMap constants (the upstream switch :51-270
 * shape): the input slot (content 0) at (53,25) — the mInputItemsCount==1 case :55 —, then
 * mOutputItemsCount output slots in the upstream output-grid layout (:169-270, the
 * mOutputFluidCount==0 arms): 1-3 = one row from x 107 at y 25; 4-6 = two rows at y 16/34;
 * 7+ = 3 columns x 4 rows (x 107/125/143, y 7/25/43/61). Shredder/Crusher land on the 12
 * case (the default branch :247-269), the Lathe on the 2 case (:178-181). The special slot
 * (:49) and the fluid displays (:274-275) are out — the port slot shape has neither. All
 * outputs are setCanPut(F) → {@link OutputSlot}. The player inventory binds at the standard
 * 176x166 machine-panel offset 84.
 *
 * <p>The single progress {@link ContainerData} is the :273-297 verbatim three-state
 * translation (the oven carries the same): mSuccessful → Short.MAX_VALUE, mMaxProgress &gt;
 * 0 → units(min(mMaxProgress, mProgress), mMaxProgress, Short.MAX_VALUE, T) normalized, else
 * -1. The server menu computes it live on every broadcastChanges poll; the client menu caches
 * it through set() and the screen reads {@link #getProgressBar()}.
 */
public class GTBasicMachineMenu extends GTGuiMenu {

	/** Success flag value (upstream :281, Short.MAX_VALUE). */
	public static final int PROGRESS_DONE = Short.MAX_VALUE;

	/** The BE bound as the menu's backing container (upstream mTileEntity, ContainerCommon.java:42). */
	public final TileEntityBasicMachine tileEntity;

	/** Content slot count: 1 input + mOutputItemsCount outputs. */
	public final int contentSlotCount;

	/** Client-side progress cache, written by the vanilla data-slot sync (upstream mProgressBar :273). */
	private int mProgressBar = -1;

	public GTBasicMachineMenu(MenuType<?> aMenuType, int aContainerId, Inventory aPlayerInventory, TileEntityBasicMachine aTileEntity) {
		super(aMenuType, aContainerId, aPlayerInventory);
		this.tileEntity = aTileEntity;

		GTItemStackHandler tInventory = aTileEntity.getInventory();
		// upstream ContainerCommonBasicMachine.addSlots :55/:162 — 1 input + N outputs, all outputs setCanPut(F)
		addSlot(new SlotItemHandler(tInventory, TileEntityBasicMachine.SLOT_INPUT, 53, 25)); // :55 (mInputItemsCount == 1)
		int tOutputs = aTileEntity.getOutputSlotCount();
		for (int i = 0; i < tOutputs; i++) {
			int[] tPos = outputGridPos(i, tOutputs);
			addSlot(new OutputSlot(tInventory, TileEntityBasicMachine.SLOT_INPUT + 1 + i, tPos[0], tPos[1])); // :162 setCanPut(F)
		}
		this.contentSlotCount = 1 + tOutputs;
		bindPlayerInventory(84); // standard machine panel (ContainerCommon.java:327-332 default offset)
		addDataSlots(this.mProgressData);
	}

	/**
	 * The upstream output-grid layout (ContainerCommonBasicMachine.java:169-270, the
	 * mOutputFluidCount==0 arms): 1-3 → one row from x 107 at y 25; 4-6 → two rows at y 16/34;
	 * 7+ → 3 columns x 4 rows (x 107/125/143, y 7/25/43/61 — the 12-output default branch).
	 */
	public static int[] outputGridPos(int aIndex, int aCount) {		if (aCount <= 3) return new int[] {107 + 18 * aIndex, 25};
		if (aCount <= 6) return new int[] {107 + 18 * (aIndex % 3), aIndex < 3 ? 16 : 34};
		return new int[] {107 + 18 * (aIndex % 3), 7 + 18 * (aIndex / 3)};
	}

	/**
	 * IForgeMenuType factory shape — resolves the BE from the BlockPos payload (oven menu
	 * precedent, NetworkHooks.openScreen :176). Each registered MenuType binds its own factory
	 * closing over the type (GTBasicMachinesMenus), so one menu class serves three ids.
	 */
	public static GTBasicMachineMenu network(MenuType<GTBasicMachineMenu> aMenuType, int aContainerId, Inventory aPlayerInventory, @Nullable FriendlyByteBuf aExtraData) {
		return new GTBasicMachineMenu(aMenuType, aContainerId, aPlayerInventory, resolveTileEntity(aPlayerInventory, aExtraData));
	}

	private static TileEntityBasicMachine resolveTileEntity(Inventory aPlayerInventory, @Nullable FriendlyByteBuf aExtraData) {
		if (aExtraData == null) {
			throw new IllegalArgumentException("gt6 machine menus require the NetworkHooks.openScreen BlockPos payload (no data bridge)");
		}
		if (aPlayerInventory.player.level().getBlockEntity(aExtraData.readBlockPos()) instanceof TileEntityBasicMachine tTileEntity) {
			return tTileEntity;
		}
		throw new IllegalArgumentException("gt6 machine menu opened without its block entity (chunk not loaded?)");
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
			return (int) TileEntityBasicMachine.units(
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
		return quickMoveBetween(aPlayer, aIndex, contentSlotCount);
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
}
