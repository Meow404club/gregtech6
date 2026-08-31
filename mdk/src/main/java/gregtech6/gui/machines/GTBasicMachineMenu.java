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
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.machines.TileEntityBasicMachine;
import gregtech6.tileentity.multiblocks.TileEntityBase10MultiBlockMachine;

/**
 * Basic-machine menu — the data-driven generalization of the oven menu (GTOvenMenu) onto the
 * RecipeMap slot shape of upstream gregapi/gui/ContainerCommonBasicMachine.java addSlots
 * (:45-271): getGUIServer (MultiTileEntityBasicMachine.java:1008) becomes this constructor,
 * the client half (getGUIClient2 :1007 / ContainerClientBasicMachine) is replaced by the
 * statically registered {@link GTBasicMachineScreen}.
 *
 * <p>Slot geometry is derived from the RecipeMap constants (the upstream switch :51-270
 * shape): the input slot (content 0) at (53,25) — the mInputItemsCount==1 case :55, with
 * upstream's y = mInputFluidCount&gt;6?7:25 conditional pinned to the 25 arm here (every
 * recipe map this menu serves is fluid-less, so the >6 arm never fires) —, then
 * mOutputItemsCount output slots in the upstream output-grid layout (:169-270, the
 * mOutputFluidCount==0 arms): 1-3 = one row from x 107 at y 25; 4-6 = two rows at y 16/34;
 * 7+ = 3 columns x 4 rows (x 107/125/143, y 7/25/43/61). Shredder/Crusher land on the 12
 * case (the default branch :247-269), the Lathe on the 2 case (:178-181). The special slot
 * (:49) and the fluid displays (:267-268) are out — the port slot shape has neither. All
 * outputs are setCanPut(F) → {@link OutputSlot}. The player inventory binds at the standard
 * 176x166 machine-panel offset 84.
 *
 * <p>The single progress {@link ContainerData} is the :273-297 verbatim three-state
 * translation (the oven carries the same): mSuccessful → Short.MAX_VALUE, mMaxProgress &gt;
 * 0 → units(min(mMaxProgress, mProgress), mMaxProgress, Short.MAX_VALUE, T) normalized, else
 * -1. The server menu computes it live on every broadcastChanges poll; the client menu caches
 * it through set() and the screen reads {@link #getProgressBar()}.
 *
 * <p>Host parametrization (task p8-cokeoven-gui-menu ①): the menu talks to its backing
 * machine through the six-method {@link Host} interface instead of the hard
 * {@link TileEntityBasicMachine} type — the single-block machines keep their ctor (the
 * gui-domain adapter wraps their public face, the machine domain stays untouched) and the
 * multiblock machines ({@link TileEntityBase10MultiBlockMachine}) implement Host directly.
 */
public class GTBasicMachineMenu extends GTGuiMenu {

	/** Success flag value (upstream :281, Short.MAX_VALUE). */
	public static final int PROGRESS_DONE = Short.MAX_VALUE;

	/**
	 * The machine face this menu needs (upstream mTileEntity, ContainerCommon.java:42, cut to
	 * the business six): the inventory the slots bind, the RecipeMap-derived slot shape, the
	 * three-state progress inputs and the GUI texture path. Implemented by the multiblock
	 * machine base directly; the single-block machines are adapted in the gui domain.
	 */
	public interface Host {
		/** The backing inventory (the SlotItemHandler container). */
		GTItemStackHandler getInventory();
		/** The output slot count of the RecipeMap shape (upstream getDefaultInventory :526). */
		int getOutputSlotCount();
		/** The :281 success flag. */
		boolean isSuccessful();
		/** The :283 progress counter. */
		long getProgress();
		/** The :282 process length. */
		long getMaxProgress();
		/** The mGUITexture = mRecipes.mGUIPath semantics (MultiTileEntityBasicMachine.java:114). */
		String getGuiTexture();
	}

	/** The machine bound as the menu's backing container (upstream mTileEntity, ContainerCommon.java:42). */
	public final Host tileEntity;

	/**
	 * The validity face of {@link #stillValid} (upstream isUseableByPlayerGUI,
	 * ContainerCommon.java:326) — every Host provider is a ticking GT6 BE; the six-method
	 * Host stays business-only, the alive/distance check reads the BE directly.
	 */
	private final TileEntityBase03TicksAndSync mBackingEntity;

	/** Content slot count: 1 input + mOutputItemsCount outputs. */
	public final int contentSlotCount;

	/** Client-side progress cache, written by the vanilla data-slot sync (upstream mProgressBar :273). */
	private int mProgressBar = -1;

	public GTBasicMachineMenu(MenuType<?> aMenuType, int aContainerId, Inventory aPlayerInventory, TileEntityBasicMachine aTileEntity) {
		this(aMenuType, aContainerId, aPlayerInventory, hostOf(aTileEntity), aTileEntity);
	}

	/** The multiblock path (upstream getGUIServer shape): the Host provider is its own backing BE. */
	public GTBasicMachineMenu(MenuType<?> aMenuType, int aContainerId, Inventory aPlayerInventory, Host aHost) {
		this(aMenuType, aContainerId, aPlayerInventory, aHost, aHost instanceof TileEntityBase03TicksAndSync tEntity ? tEntity : null);
	}

	private GTBasicMachineMenu(MenuType<?> aMenuType, int aContainerId, Inventory aPlayerInventory, Host aHost, @Nullable TileEntityBase03TicksAndSync aBacking) {
		super(aMenuType, aContainerId, aPlayerInventory);
		this.tileEntity = aHost;
		this.mBackingEntity = aBacking;

		GTItemStackHandler tInventory = aHost.getInventory();
		// upstream ContainerCommonBasicMachine.addSlots :55/:162 — 1 input + N outputs, all outputs setCanPut(F)
		addSlot(new SlotItemHandler(tInventory, TileEntityBasicMachine.SLOT_INPUT, 53, 25)); // :55 (mInputItemsCount == 1; upstream y = mInputFluidCount>6?7:25, pinned to the 25 arm here)
		int tOutputs = aHost.getOutputSlotCount();
		for (int i = 0; i < tOutputs; i++) {
			int[] tPos = outputGridPos(i, tOutputs);
			addSlot(new OutputSlot(tInventory, TileEntityBasicMachine.SLOT_INPUT + 1 + i, tPos[0], tPos[1])); // :162 setCanPut(F)
		}
		this.contentSlotCount = 1 + tOutputs;
		bindPlayerInventory(84); // standard machine panel (ContainerCommon.java:327-332 default offset)
		addDataSlots(this.mProgressData);
	}

	/**
	 * The gui-domain adapter (task p8-cokeoven-gui-menu ①): wraps the single-block machine's
	 * public face — getInventory :199 / getOutputSlotCount :208 / mSuccessful/mProgress/
	 * mMaxProgress :107-113 / public final mRecipes :161 (mGUIPath) — so the machine domain
	 * needs no gui knowledge (no implements, the file stays frozen for D3/M1).
	 */
	static Host hostOf(TileEntityBasicMachine aMachine) {
		return new Host() {
			@Override public GTItemStackHandler getInventory() { return aMachine.getInventory(); }
			@Override public int getOutputSlotCount() { return aMachine.getOutputSlotCount(); }
			@Override public boolean isSuccessful() { return aMachine.mSuccessful; }
			@Override public long getProgress() { return aMachine.mProgress; }
			@Override public long getMaxProgress() { return aMachine.mMaxProgress; }
			@Override public String getGuiTexture() { return aMachine.mRecipes.mGUIPath; }
		};
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
	 * closing over the type (GTBasicMachinesMenus), so one menu class serves the machine ids.
	 */
	public static GTBasicMachineMenu network(MenuType<GTBasicMachineMenu> aMenuType, int aContainerId, Inventory aPlayerInventory, @Nullable FriendlyByteBuf aExtraData) {
		return new GTBasicMachineMenu(aMenuType, aContainerId, aPlayerInventory, resolveTileEntity(aPlayerInventory, aExtraData));
	}

	/**
	 * The multiblock-machine factory path (task p8-cokeoven-gui-menu ①) — the cokeoven
	 * MenuType closure (GTBasicMachinesMenus) calls this: same BlockPos payload contract,
	 * the resolve hard-checks {@link TileEntityBase10MultiBlockMachine} instead.
	 */
	public static GTBasicMachineMenu networkMultiBlock(MenuType<GTBasicMachineMenu> aMenuType, int aContainerId, Inventory aPlayerInventory, @Nullable FriendlyByteBuf aExtraData) {
		return new GTBasicMachineMenu(aMenuType, aContainerId, aPlayerInventory, resolveMultiBlockMachine(aPlayerInventory, aExtraData));
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

	/** The multiblock resolve — the cokeoven twin of {@link #resolveTileEntity} (p8 ②). */
	private static TileEntityBase10MultiBlockMachine resolveMultiBlockMachine(Inventory aPlayerInventory, @Nullable FriendlyByteBuf aExtraData) {
		if (aExtraData == null) {
			throw new IllegalArgumentException("gt6 machine menus require the NetworkHooks.openScreen BlockPos payload (no data bridge)");
		}
		if (aPlayerInventory.player.level().getBlockEntity(aExtraData.readBlockPos()) instanceof TileEntityBase10MultiBlockMachine tMachine) {
			return tMachine;
		}
		throw new IllegalArgumentException("gt6 multiblock machine menu opened without its block entity (chunk not loaded?)");
	}

	/**
	 * The three-state progress value (upstream ContainerCommonBasicMachine.java:280-286
	 * verbatim). Server-authoritative: this is the exact function the vanilla data-slot sync
	 * polls every broadcastChanges.
	 */
	public int computeProgressValue() {
		return progressValue(this.tileEntity);
	}

	/**
	 * The three-state progress function over any Host — the static assertion face for the
	 * RCON chain (no Player/Menu instance needed there, task p8-cokeoven-gui-menu ⑨).
	 */
	public static int progressValue(Host aHost) {
		if (aHost.isSuccessful()) {
			return PROGRESS_DONE; // :281
		}
		if (aHost.getMaxProgress() > 0) { // :282
			return (int) TileEntityBasicMachine.units(
					Math.min(aHost.getMaxProgress(), aHost.getProgress()),
					aHost.getMaxProgress(), Short.MAX_VALUE, true); // :283
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
		if (mBackingEntity == null) return false;
		return !mBackingEntity.isDead() && !mBackingEntity.isRemoved()
			&& aPlayer.distanceToSqr(
				mBackingEntity.getBlockPos().getX() + 0.5D,
				mBackingEntity.getBlockPos().getY() + 0.5D,
				mBackingEntity.getBlockPos().getZ() + 0.5D) <= 64.0D;
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
