package gregtech6.gui.machines;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.items.SlotItemHandler;

import gregtech6.fluid.FluidTankGT;
import gregtech6.gui.GTGuiMenu;
import gregtech6.gui.GTRenderSlot;
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
 * shape): the input slots — the mInputItemsCount cases :55/:57-60 (count 1 = (53,25),
 * count 2 = (35,25)+(53,25), the R7 parameterisation riding
 * {@link Host#getInputSlotCount} whose default 1 keeps every pre-Canner menu
 * byte-identical), with upstream's y = mInputFluidCount&gt;6?7:25 conditional pinned to the
 * 25 arm here (the
 * served maps stay at mInputFluidCount ≤ 6, Drying's 1 making the 25 arm fire) —, then
 * mOutputItemsCount output slots in the upstream output-grid layout (:169-270, the
 * mOutputFluidCount==0 arms): 1-3 = one row from x 107 at y 25; 4-6 = two rows at y 16/34;
 * 7+ = 3 columns x 4 rows (x 107/125/143, y 7/25/43/61). Shredder/Crusher land on the 12
 * case (the default branch :247-269), the Lathe on the 2 case (:178-181). The special slot
 * (:49) stays out. All outputs are setCanPut(F) → {@link OutputSlot}. The player inventory
 * binds at the standard 176x166 machine-panel offset 84.
 *
 * <p>Fluid display slots (task p16-machine-fluid-gui ②, the p8 pool item): the upstream
 * :267-268 pair of Slot_Render banks, one display slot per RecipeMap-declared tank —
 * mInputFluidCount inputs descending from (53,63) right-to-left, mOutputFluidCount outputs
 * ascending from (107,63) left-to-right, both wrapping upward every 3 ({@link
 * #fluidDisplayPos}). The Host exposes the banks as the BE's public final mTanksInput/
 * mTanksOutput arrays (sized by the same RM counts, TileEntityBasicMachine :325-328), so
 * each {@link FluidDisplaySlot} is paired 1:1 with its tank — the display face stays inert
 * ({@code Slot_Render = Slot_Holo(false,false,0)}: hasItem/mayPlace/mayPickup all false,
 * {@link #getItem()} reads EMPTY so the vanilla broadcastChanges per-slot poll
 * (AbstractContainerMenu.java:168-170) never syncs or dereferences the backing container);
 * the fancy fluid-content rendering stays pooled ("基础槽显示即可" — the frames are painted
 * by the GUI background texture). The direct Host implementor (the multiblock base) and
 * the test fakes keep the p8 six-method shape through the default-empty banks, so the
 * cokeoven menu is byte-identical to its p8 landing (zero display slots — the COKE_OVEN
 * map's 0-in/1-out fluid face rides the capability, not the GUI).
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
		/**
		 * The input slot count of the RecipeMap shape (upstream mInputItemsCount — the
		 * addSlots switch :51-270 case selector). Default 1 = the exact shape every
		 * pre-Canner family serves (the 1-in RM maps and the COKE_OVEN 1-in map), so the
		 * multiblock Host implementor and the older fakes keep their byte-identical
		 * single-input menu. Task p24-canner-machine R7: the parameterisation exists for
		 * the RM.Canner 2/2 declaration (ContainerCommonBasicMachine.java:57-60 case 2),
		 * the gui-domain adapter overrides with the machine's live mInputItemsCount.
		 */
		default int getInputSlotCount() { return 1; }
		/** The :281 success flag. */
		boolean isSuccessful();
		/** The :283 progress counter. */
		long getProgress();
		/** The :282 process length. */
		long getMaxProgress();
		/** The mGUITexture = mRecipes.mGUIPath semantics (MultiTileEntityBasicMachine.java:114). */
		String getGuiTexture();
		/**
		 * The :267 display bank — the input tanks, in tank order (task p16-machine-fluid-gui ②).
		 * Default-empty on purpose: the direct implementor (the multiblock base) and the test
		 * fakes keep the p8 six-method shape and render zero display slots; the gui-domain
		 * {@link #hostOf} adapter overrides this with the machine's live public mTanksInput.
		 */
		default FluidTankGT[] getFluidInputTanks() { return new FluidTankGT[0]; }
		/**
		 * The :268 display bank — the output tanks, in tank order (task p16-machine-fluid-gui ②).
		 * Default-empty, same reasoning as {@link #getFluidInputTanks()}.
		 */
		default FluidTankGT[] getFluidOutputTanks() { return new FluidTankGT[0]; }
	}

	/** The machine bound as the menu's backing container (upstream mTileEntity, ContainerCommon.java:42). */
	public final Host tileEntity;

	/**
	 * The validity face of {@link #stillValid} (upstream isUseableByPlayerGUI,
	 * ContainerCommon.java:326) — every Host provider is a ticking GT6 BE; the six-method
	 * Host stays business-only, the alive/distance check reads the BE directly.
	 */
	private final TileEntityBase03TicksAndSync mBackingEntity;

	/** Content slot count: the input slots + mOutputItemsCount outputs (the fluid display slots sit AFTER this boundary — quickMove never targets them). */
	public final int contentSlotCount;

	/** Client-side progress cache, written by the vanilla data-slot sync (upstream mProgressBar :273). */
	private int mProgressBar = -1;

	/**
	 * The display slots in menu order — the :267 input bank first, then the :268 output bank
	 * (task p16-machine-fluid-gui ②); slot i of each bank is paired with tank i of the
	 * corresponding Host array (each {@link FluidDisplaySlot} carries its tank reference).
	 */
	private final List<FluidDisplaySlot> mFluidDisplaySlots = new ArrayList<>();

	/**
	 * The never-read backing container of the display slots: {@link FluidDisplaySlot#getItem()}
	 * is overridden to EMPTY (the vanilla broadcastChanges poll, AbstractContainerMenu.java:168-170,
	 * reads every slot every tick), so this container is only ever stored by the Slot supertype.
	 * Per-menu on purpose: {@link FluidDisplaySlot} is static, so without the pass-through every
	 * open menu's display slots would alias one shared dummy instance (a static is exactly what
	 * this looked like before — one field, no per-menu state — it just was never necessary).
	 */
	private final SimpleContainer mDisplayContainer = new SimpleContainer(0);

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
		// upstream ContainerCommonBasicMachine.addSlots — the input-switch cases :55 (case 1)
		// and :57-60 (case 2), then N outputs, all outputs setCanPut(F). The R7 parameterisation
		// rides the Host.getInputSlotCount() default 1: every pre-Canner family keeps the
		// byte-identical single-input menu.
		int tInputSlots = aHost.getInputSlotCount();
		for (int i = 0; i < tInputSlots; i++) {
			int[] tPos = inputSlotPos(i, tInputSlots);
			addSlot(new SlotItemHandler(tInventory, TileEntityBasicMachine.SLOT_INPUT + i, tPos[0], tPos[1])); // :55/:58-59 (upstream y = mInputFluidCount>6?7:25, pinned to the 25 arm — every served map stays at mInputFluidCount ≤ 6)
		}
		int tOutputs = aHost.getOutputSlotCount();
		for (int i = 0; i < tOutputs; i++) {
			int[] tPos = outputGridPos(i, tOutputs);
			addSlot(new OutputSlot(tInventory, TileEntityBasicMachine.SLOT_INPUT + tInputSlots + i, tPos[0], tPos[1])); // :162 setCanPut(F)
		}
		this.contentSlotCount = tInputSlots + tOutputs;
		// upstream :267-268 — the Slot_Render fluid banks, one display slot per RM-declared tank,
		// each paired 1:1 with its BE tank (task p16-machine-fluid-gui ②)
		FluidTankGT[] tInTanks = aHost.getFluidInputTanks();
		for (int i = 0; i < tInTanks.length; i++) {
			FluidDisplaySlot tSlot = new FluidDisplaySlot(tInTanks[i], i, fluidDisplayPos(false, i), mDisplayContainer);
			mFluidDisplaySlots.add(tSlot);
			addSlot(tSlot);
		}
		FluidTankGT[] tOutTanks = aHost.getFluidOutputTanks();
		for (int i = 0; i < tOutTanks.length; i++) {
			FluidDisplaySlot tSlot = new FluidDisplaySlot(tOutTanks[i], i, fluidDisplayPos(true, i), mDisplayContainer);
			mFluidDisplaySlots.add(tSlot);
			addSlot(tSlot);
		}
		bindPlayerInventory(84); // standard machine panel (ContainerCommon.java:327-332 default offset)
		addDataSlots(this.mProgressData);
	}

	/** The display slots in menu order ({@code unmodifiable}) — the :267 input bank then the :268 output bank. */
	public List<FluidDisplaySlot> fluidDisplaySlots() {
		return Collections.unmodifiableList(mFluidDisplaySlots);
	}

	/**
	 * The upstream :267-268 display geometry: the input bank descends from x 53 right-to-left
	 * ({@code 53 - (i%3)*18, 63 - (i/3)*18}), the output bank ascends from x 107 left-to-right
	 * ({@code 107 + (i%3)*18, 63 - (i/3)*18}), both wrapping upward every 3 — the Drying shape
	 * lands in[0]=(53,63) and out0..2=(107/125/143, 63). Static for the offline menu test
	 * (the outputGridPos precedent).
	 *
	 * @param aOutput false = the input bank (:267), true = the output bank (:268)
	 * @param aIndex  the tank index within the bank
	 */
	public static int[] fluidDisplayPos(boolean aOutput, int aIndex) {
		return aOutput
				? new int[] {107 + 18 * (aIndex % 3), 63 - 18 * (aIndex / 3)}
				: new int[] {53 - 18 * (aIndex % 3), 63 - 18 * (aIndex / 3)};
	}

	/**
	 * The upstream input-slot geometry (ContainerCommonBasicMachine.java:51-60, the
	 * mInputItemsCount switch, y = mInputFluidCount&gt;6?7:25 pinned to the 25 arm): count 1
	 * = the single (53,25) slot; count 2 = the (35,25)+(53,25) pair (the RM.Canner 2/2 arm).
	 * WARNING: the count&gt;=2 arm generalizes as {@code 35 + 18*i}, which does NOT reproduce
	 * the upstream case-3 layout (x 17/35/53 — ContainerCommonBasicMachine.java:61-65) or
	 * beyond: a 3-input RecipeMap landing here must transcribe its own case from the
	 * upstream switch instead of trusting this extrapolation (no such map is served today).
	 * Static for the offline menu test (the outputGridPos/fluidDisplayPos precedent).
	 *
	 * @param aIndex the input slot within the input bank
	 * @param aCount the input slot count of the RecipeMap shape
	 */
	public static int[] inputSlotPos(int aIndex, int aCount) {
		if (aCount >= 2) return new int[] {35 + 18 * aIndex, 25}; // :58-59 — x 35 then x 53
		return new int[] {53, 25}; // :55 — the single-input arm
	}

	/**
	 * The gui-domain adapter (task p8-cokeoven-gui-menu ①): wraps the single-block machine's
	 * public face — getInventory :199 / getOutputSlotCount :208 / mSuccessful/mProgress/
	 * mMaxProgress :107-113 / public final mRecipes :161 (mGUIPath) — so the machine domain
	 * needs no gui knowledge (no implements, the file stays frozen for D3/M1). The p16 ②
	 * overrides read the W1a public final tank arrays LIVE (same identity the fluid face
	 * gates answer through), no snapshot.
	 */
	static Host hostOf(TileEntityBasicMachine aMachine) {
		return new Host() {
		@Override public GTItemStackHandler getInventory() { return aMachine.getInventory(); }
		@Override public int getOutputSlotCount() { return aMachine.getOutputSlotCount(); }
		@Override public int getInputSlotCount() { return aMachine.getInputSlotCount(); } // the R7 live mRecipes.mInputItemsCount (1 on every pre-Canner family, 2 on the Canner)
			@Override public boolean isSuccessful() { return aMachine.mSuccessful; }
			@Override public long getProgress() { return aMachine.mProgress; }
			@Override public long getMaxProgress() { return aMachine.mMaxProgress; }
			@Override public String getGuiTexture() { return aMachine.mRecipes.mGUIPath; }
			@Override public FluidTankGT[] getFluidInputTanks() { return aMachine.mTanksInput; }
			@Override public FluidTankGT[] getFluidOutputTanks() { return aMachine.mTanksOutput; }
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

	/**
	 * The fluid tank display slot (task p16-machine-fluid-gui ②, the upstream :267-268
	 * Slot_Render): a {@link GTRenderSlot} — Slot_Holo(false,false,0), inert in every
	 * direction — paired with its backing tank ({@link #tank}, the 1:1 slot-tank pairing the
	 * menu test asserts and the pooled fluid-content rendering would read). The slot itself
	 * never holds live content: {@link #getItem()} reads EMPTY so the vanilla broadcastChanges
	 * per-slot poll (AbstractContainerMenu.java:168-170) syncs nothing and never dereferences
	 * the menu-owned {@code mDisplayContainer} stand-in passed in by
	 * {@link #FluidDisplaySlot(FluidTankGT, int, int[], SimpleContainer)}. Display-only —
	 * players cannot put anything in, take anything out, or shift-click through it
	 * (hasItem()=false short-circuits quickMove).
	 */
	public static final class FluidDisplaySlot extends GTRenderSlot {
		/** The tank this display is paired with (the Host bank element at this slot's bank index). */
		public final FluidTankGT tank;

		FluidDisplaySlot(FluidTankGT aTank, int aBankIndex, int[] aPos, SimpleContainer aDisplayContainer) {
			super(aDisplayContainer, aBankIndex, aPos[0], aPos[1]);
			this.tank = aTank;
		}

		/** Slot_Render never holds live content — the screen renders the frame, the pool owns any fluid visual. */
		@Override
		public ItemStack getItem() {
			return ItemStack.EMPTY;
		}
	}
}
