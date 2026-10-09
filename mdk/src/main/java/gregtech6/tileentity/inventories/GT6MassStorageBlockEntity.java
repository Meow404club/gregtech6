package gregtech6.tileentity.inventories;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import net.minecraftforge.items.IItemHandler;

import gregtech6.item.MaterialPrefixItem;
import gregtech6.registry.GTMaterialItems;
import gregtech6.tileentity.GTItemStackHandler;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.logistics.ITileEntityLogisticsSemiFilteredItem;
import gregtech6.util.GTItemMover;

import gregapi.data.CS;
import gregapi.data.OP;
import gregapi.data.TD;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;

/**
 * The item mass storage — 1.20.1 port of the upstream
 * {@code gregapi/tileentity/inventories/MultiTileEntityMassStorage.java} (759 lines) base
 * semantics: one item type at up to {@link #mMaxStorage} (default 1000000, the
 * NBT_CAPACITY default :71) units in slot 1, an auto funnel in slot 0, the six tool mode
 * bits, the pixel-grid take/place face (the block carrier's use()), overflow emission and
 * bottom fill on the tick, partial-unit conversion, and the logistics filter faces.
 *
 * <h2>The upstream section map (the functional-audit anchors)</h2>
 * <ul>
 * <li>fields + NBT :71-89 — mMaxStorage/mPartialUnits/mMode (NBT_MODE/NBT_CAPACITY/
 *     NBT_INPUT), the plain in-repo key form;</li>
 * <li>tool arms :122-238 — pincers take-all, soft hammer eject, the four mode toggles,
 *     the status lens; the duct tape / scissors arms ride the tape pool (the
 *     {@code IL.Duct_Tape} item is unported — {@link #MODE_TAPED} stays an NBT-visible
 *     state the kept-content break face honours);</li>
 * <li>pixel grid :241-331 — the front-face 1/4/8/16/32/64 take columns, the centre
 *     insert-all arm, the plain insert arm (the block carrier routes here);</li>
 * <li>tick :333-358 — funnel drain, bottom fill (B[0]) / overflow emit (B[2]) on the
 *     100-tick or mutation gate, the all-but-bottom adjacency wake;</li>
 * <li>insert/emit :377-434 — insertItems, getMaxContent (the +256 overflow headroom),
 *     emitOverflow;</li>
 * <li>logistics :436-505 — the semi-filtered set (prefix-family expansion) + the four
 *     priority/filter answers;</li>
 * <li>inventory contract :542-566 — slots {0,1}, insert/extract gates, the taped
 *     keepSlot, the explosion kill, the partial drop on break;</li>
 * <li>ConnectedInventory :568-591 — the three external-draw arms (the ACT card cut the
 *     consumer domain — decisions.p24-act-deviations ③ — so these land as public
 *     semantics with the interface seat deferred to the consuming card);</li>
 * <li>partial units :593-698 — getPartialStack / getUnitAmount / updatePartialContent /
 *     allowInsertion.</li>
 * </ul>
 *
 * <h2>Declared folds</h2>
 * <ul>
 * <li><b>Resolution face</b>: upstream reads ore-dict data off ANY stack (OM.anydata);
 *     the port resolves {@link MaterialPrefixItem} identity only — every GT material
 *     item carries (prefix, material) on the Item, foreign items answer null and take
 *     the plain same-item path only. The mBlackListed glass carve (:632) is vacuously
 *     true under that face (material items are never blacklisted).</li>
 * <li><b>NEI infinite arm</b> (:387-393/:407-412): the {@code NEI_INFINITE} creative
 *     cheat face has no port consumer — cut.</li>
 * <li><b>Sync throttle</b> (:360-369): the port sync is event-driven (the 03 dispatcher
 *     {@code mSendClientData} + the vanilla block-update channel), so the
 *     oStacksize-diff throttle folds onto {@link #onTickCheck} returning the face-visible
 *     drift exactly as upstream wrote it (SERVER_TIME%5 folds onto the timer phase).</li>
 * <li><b>Covers</b> (isFaceVisible :508): the port mass storage mounts no covers — the
 *     cover conjunct folds to true-visible.</li>
 * <li><b>Item stack limit</b> (IMTE_GetMaxStackSize :557): the conditional 1-when-taped
 *     item limit has no per-stack face on 1.20.1 (Item.Properties is per-Item) — the
 *     BlockItem stacks to 64 flat, declared deviation.</li>
 * </ul>
 *
 * <p>The mass stack rides slot 1 with an UNCLAMPED count (up to {@link #mMaxStorage});
 * the vanilla/Forge slot insert paths cap at the item max stack size, so every slot-1
 * write is a direct {@code setStackInSlot} and the automation view
 * ({@link MassSideView}) chunk-extracts instead of routing through the handler insert.
 */
public class GT6MassStorageBlockEntity extends TileEntityBase03TicksAndSync
		implements GT6AdjacentInventoryUpdatable, ITileEntityLogisticsSemiFilteredItem {

	/** The auto-funnel slot (upstream ACCESSIBLE_SLOTS[0] :542 — the piped insert lands here, the tick drains it). */
	public static final int SLOT_FUNNEL = 0;
	/** The mass slot (upstream slot 1 — the single-item store, unclamped count). */
	public static final int SLOT_MASS = 1;

	/** Upstream B[0] — fill the inventory BELOW (the monkey wrench toggle :217-223). */
	public static final byte MODE_FILL_BELOW = 1;
	/** Upstream B[1] — the filter resets when the store runs empty (the screwdriver toggle :209-216). */
	public static final byte MODE_FILTER_RESET = 2;
	/** Upstream B[2] — emit overflow to the inventory below (the cutter toggle :202-208). */
	public static final byte MODE_EMIT_OVERFLOW = 4;
	/** Upstream B[3] — duct-taped: no interaction, content keeps on harvest (:112/:128/:184-201). */
	public static final byte MODE_TAPED = 8;

	/** NBT keys (the upstream NBT_MODE/NBT_CAPACITY/NBT_INPUT columns, the plain in-repo form). */
	public static final String NBT_MODE = "mode", NBT_CAPACITY = "capacity", NBT_PARTIAL = "partial";
	/** NBT_INVENTORY — the slot pair (the static-batch key). */
	public static final String NBT_INVENTORY = "inventory";
	/** NBT_FACING — the NBT fallback byte (the static-batch key). */
	public static final String NBT_FACING = "facing";

	/** CS.SIDE_BOTTOM (the GT6 side byte == Direction.get3DDataValue; the hopper-base form). */
	public static final byte SIDE_BOTTOM = 0;
	/** CS.SIDE_ANY — the side-less view. */
	public static final byte SIDE_ANY = 6;
	/** CS.U — the material unit (root CS.java:49). */
	public static final long U = CS.U;
	/** Upstream CS.PX_P — px i from the positive edge, i/16 (CS.java:492). */
	public static final float[] PX_P = {0, 1/16F, 2/16F, 3/16F, 4/16F, 5/16F, 6/16F, 7/16F, 8/16F, 9/16F, 10/16F, 11/16F, 12/16F, 13/16F, 14/16F, 15/16F, 1};
	/** Upstream CS.PX_N — px i from the negative edge, (16-i)/16 (CS.java:504). */
	public static final float[] PX_N = {1, 15/16F, 14/16F, 13/16F, 12/16F, 11/16F, 10/16F, 9/16F, 8/16F, 7/16F, 6/16F, 5/16F, 4/16F, 3/16F, 2/16F, 1/16F, 0};

	/** Upstream :71 oStacksize — the last synced count (the throttle compare). */
	public int oStacksize = 0;
	/** Upstream :71 mMaxStorage — the capacity (NBT_CAPACITY, default 1000000). */
	public int mMaxStorage = 1000000;
	/** Upstream :72 mPartialUnits — the sub-unit residue (NBT_INPUT). */
	public long mPartialUnits = 0;
	/** Upstream :73 mMode — the four mode bits. */
	public byte mMode = 0;

	/** The mutation gate (the hopper-family face: every handler write arms one drain pass). */
	public boolean mInventoryChanged = false;

	/** Full constructor — also the offline (test) entry point (the hopper-family shape). */
	public GT6MassStorageBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(true, aType, aPos, aState); // the mass storage ticks (upstream onTick2 :333-358)
		GTItemStackHandler tInventory = new GTItemStackHandler(2, this::updateInventory);
		setInventory(tInventory);
	}

	/** The BET constructor (the vanilla two-arg face; the type rides the multi-mount). */
	public GT6MassStorageBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/** The BET registry path (the getTileEntityName convention). */
	@Override
	public String getTileEntityName() {
		return "mass_storage";
	}

	// ---------------------------------------------------------------------------
	// facing (the static-batch shape: the blockstate FACING is the live truth)
	// ---------------------------------------------------------------------------

	/** The front face: the blockstate FACING when present, the NBT byte otherwise. */
	public byte getFacing() {
		BlockState tState = getBlockState();
		if (tState != null && tState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
			return (byte) tState.getValue(BlockStateProperties.HORIZONTAL_FACING).get3DDataValue();
		}
		return mFacingNbt;
	}

	/** The NBT-fallback byte write (the block's setPlacedBy keeps it in step). */
	public void setFacingNbtFallback(byte aFacing) {
		mFacingNbt = aFacing;
	}

	private byte mFacingNbt = 3; // upstream getDefaultSide SIDE_FRONT :538 — south is the 1.20.1 horizontal front

	// ---------------------------------------------------------------------------
	// the slot vocabulary (upstream slot()/slotHas()/slotKill forms, :542-556)
	// ---------------------------------------------------------------------------

	public GTItemStackHandler getInventory() {
		return mInventory;
	}

	public boolean slotHas(int aIndex) {
		return !mInventory.getStackInSlot(aIndex).isEmpty();
	}

	public ItemStack slot(int aIndex) {
		return mInventory.getStackInSlot(aIndex);
	}

	/** The direct slot write — slot 1 rides unclamped counts, so it NEVER goes through insertItem. */
	public void slot(int aIndex, @Nullable ItemStack aStack) {
		mInventory.setStackInSlot(aIndex, aStack == null || aStack.isEmpty() ? ItemStack.EMPTY : aStack);
	}

	/** Upstream slotNull (the empty-and-clean probe): an empty slot 1 clears to EMPTY and answers true. */
	public boolean slotNull(int aIndex) {
		if (!slotHas(aIndex)) return true;
		if (slot(aIndex).getCount() <= 0) {
			slot(aIndex, ItemStack.EMPTY);
			return true;
		}
		return false;
	}

	/** Upstream slotKill. */
	public void slotKill(int aIndex) {
		slot(aIndex, ItemStack.EMPTY);
	}

	/** The change point: setChanged + the adjacency wake + the client-data flag (the 05 fold). */
	public void updateInventory() {
		mInventoryChanged = true;
		setChanged();
		notifyAdjacentInventories();
	}

	/**
	 * The upstream Locker-wake face (the static-batch fold, event-driven): every neighbour
	 * implementing {@link GT6AdjacentInventoryUpdatable} is told which of ITS sides faces
	 * back at this container.
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

	@Override
	public void adjacentInventoryUpdated(byte aSide, BlockEntity aSource) {
		// upstream :551 — the bottom neighbour changed: re-run the fill/overflow gate
		if (aSide == SIDE_BOTTOM) updateInventory();
	}

	// ---------------------------------------------------------------------------
	// the tick (upstream onTick2 :333-358 + the sync throttle :360-369)
	// ---------------------------------------------------------------------------

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		if (!aIsServerSide || (mMode & MODE_TAPED) != 0) return;
		if (!slotNull(SLOT_FUNNEL)) slot(SLOT_FUNNEL, insertItems(slot(SLOT_FUNNEL), false));
		boolean tTemp = false;
		if (mInventoryChanged || aTimer % 100 == 0) {
			if (slotHas(SLOT_MASS)) {
				if ((mMode & MODE_FILL_BELOW) != 0 && slot(SLOT_MASS).getCount() > 0) {
					if (moveBottom(GTItemMover.DEFAULT_MAX_MOVE) > 0) tTemp = true;
				} else // else, because if it already tried to emit normally, then it doesn't need to check a second time. (:343)
				if ((mMode & MODE_EMIT_OVERFLOW) != 0 && slot(SLOT_MASS).getCount() > mMaxStorage) {
					emitOverflow();
				}
			}
		}
		if (mInventoryChanged || tTemp) {
			for (byte tSide = 0; tSide < 6; tSide++) {
				if (tSide == SIDE_BOTTOM) continue; // ALL_SIDES_BUT_BOTTOM :350
				BlockEntity tNeighbor = getLevel().getBlockEntity(getBlockPos().relative(Direction.from3DDataValue(tSide)));
				if (tNeighbor instanceof GT6AdjacentInventoryUpdatable) {
					((GT6AdjacentInventoryUpdatable) tNeighbor).adjacentInventoryUpdated(
							(byte) Direction.from3DDataValue(tSide).getOpposite().get3DDataValue(), this);
				}
			}
		}
	}

	@Override
	public boolean onTickCheck(long aTimer) {
		// upstream :362 — sync only when the visible face count drifted (fast lane every 5
		// ticks over a 64 drift, slow lane on the second beat)
		return isFaceVisible() && (slotHas(SLOT_MASS)
				? slot(SLOT_MASS).getCount() != oStacksize
						&& (Math.abs(slot(SLOT_MASS).getCount() - oStacksize) > 64 ? aTimer % 5 == 0 : aTimer % 20 == 0)
				: oStacksize != 0);
	}

	@Override
	public void onTickChecked(long aTimer) {
		oStacksize = slotHas(SLOT_MASS) ? slot(SLOT_MASS).getCount() : 0;
	}

	@Override
	public void onTickResetChecks(long aTimer, boolean aIsServerSide) {
		super.onTickResetChecks(aTimer, aIsServerSide);
		mInventoryChanged = false; // the TileEntityBase05Inventories :86-89 ledger
	}

	/** Upstream isFaceVisible :507-509 — horizontal front, untaped; the cover conjunct folds (class doc). */
	public boolean isFaceVisible() {
		byte tFacing = getFacing();
		return tFacing >= 2 && tFacing <= 5 && (mMode & MODE_TAPED) == 0;
	}

	/** One bottom-fill pass into the inventory below (the ST.move(delegator, inv) default face). @return the moved count */
	protected int moveBottom(int aMaxMove) {
		if (!hasLevel()) return 0; // the offline face: no adjacency to pump (the live run always has one)
		BlockEntity tBelow = getLevel().getBlockEntity(getBlockPos().below());
		if (tBelow == null) return 0;
		//? if forge {
		IItemHandler tBelowHandler = tBelow.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER, Direction.UP).orElse(null);
		//?} else {
		/*IItemHandler tBelowHandler = tBelow.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, tBelow.getBlockPos(), Direction.UP);
		 *///?}
		if (tBelowHandler == null) return 0;
		return GTItemMover.move(massView(), tBelowHandler, aMaxMove, GTItemMover.DEFAULT_MIN_MOVE, GTItemMover.DEFAULT_MAX_SLOT_SIZE, null, false);
	}

	/** Upstream emitOverflow :428-434 — push everything past mMaxStorage into the inventory below. */
	public void emitOverflow() {
		while (slotHas(SLOT_MASS) && slot(SLOT_MASS).getCount() > mMaxStorage) {
			int tToBeMoved = (int)Math.min(Integer.MAX_VALUE, slot(SLOT_MASS).getCount() - (long)mMaxStorage);
			if (moveBottom(tToBeMoved) <= 0) break;
		}
	}

	// ---------------------------------------------------------------------------
	// the automation face (upstream getAccessibleSlotsFromSide2/canInsertItem2/
	// canExtractItem2 :542-550 + the SideItemHandler posture over an unclamped slot 1)
	// ---------------------------------------------------------------------------

	/** The automation slot table (upstream ACCESSIBLE_SLOTS :542). */
	public int[] getAccessibleSlotsFromSide(@Nullable byte aSide) {
		return new int[] {SLOT_FUNNEL, SLOT_MASS};
	}

	/** Upstream canInsertItem2 :548 — the funnel gates. */
	public boolean canInsertItem(int aSlot, ItemStack aStack, byte aSide) {
		return aSlot == SLOT_FUNNEL && (mMode & MODE_TAPED) == 0
				&& (aSide != SIDE_BOTTOM || (mMode & MODE_FILL_BELOW) == 0)
				&& (!slotHas(SLOT_MASS) || ((long)slot(SLOT_MASS).getCount() < getMaxContent() && allowInsertion(aStack)));
	}

	/** Upstream canExtractItem2 :549. */
	public boolean canExtractItem(int aSlot, byte aSide) {
		return aSlot == SLOT_FUNNEL || (slotHas(SLOT_MASS) && slot(SLOT_MASS).getCount() > 0 && (mMode & MODE_TAPED) == 0);
	}

	/** Upstream allowZeroStacks :550 — slot 1 may hold a zero-count ghost while the filter-reset mode is off... inverted: while ON with residue. */
	public boolean allowZeroStacks(int aSlot) {
		return aSlot == SLOT_MASS && ((mMode & MODE_FILTER_RESET) == 0 || mPartialUnits > 0);
	}

	/** The per-side automation view (fresh per call — the static-batch posture). */
	public IItemHandler sideView(@Nullable Direction aSide) {
		return new MassSideView(this, aSide == null ? SIDE_ANY : (byte)aSide.get3DDataValue());
	}

	/** The single-slot mass view the mover pumps from (the emit/bottom-fill source). */
	public IItemHandler massView() {
		return new MassSideView(this, SIDE_ANY);
	}

	//? if forge {
	@Override
	public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> aCapability, @Nullable Direction aSide) {
		if (aCapability == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER) {
			return net.minecraftforge.common.util.LazyOptional.of(() -> sideView(aSide)).cast();
		}
		return super.getCapability(aCapability, aSide);
	}
	//?} else {
	/*// 21.1: BlockEntity carries no capability override — the seam member the
	// GT6CapabilityWiring rows delegate to (the static-batch fork verbatim).
	// (T) is the BlockCapability dispatch the caller types — T is not expressible
	// here (no Class token on BlockCapability); the platform-documented shape.
	@SuppressWarnings("unchecked")
	public <T> T getCapability(net.neoforged.neoforge.capabilities.BlockCapability<T, Direction> aCapability, @Nullable Direction aSide) {
		if (aCapability == net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK) {
			return (T) sideView(aSide);
		}
		return null;
	}
	 *///?}

	/**
	 * The mass-storage automation view: slot 0 inserts drain EVENT-DRIVEN into the store
	 * (the onTick2 :337 funnel arm folded onto the mutation), slot 1 chunk-extracts from
	 * the unclamped mass stack — the vanilla ItemStackHandler path would clamp both.
	 */
	public static final class MassSideView implements IItemHandler {

		private final GT6MassStorageBlockEntity mTE;
		private final byte mSide;

		MassSideView(GT6MassStorageBlockEntity aTE, byte aSide) {
			mTE = aTE;
			mSide = aSide;
		}

		@Override
		public int getSlots() {
			return 2;
		}

		@Override
		public ItemStack getStackInSlot(int aSlot) {
			return mTE.slot(aSlot);
		}

		@Override
		public ItemStack insertItem(int aSlot, ItemStack aStack, boolean aSimulate) {
			if (aSlot != SLOT_FUNNEL || aStack.isEmpty()) return aStack;
			if (!mTE.canInsertItem(SLOT_FUNNEL, aStack, mSide)) return aStack;
			if (aSimulate) {
				// the funnel arm runs insertItems for real; the simulate face clamps to the
				// space answer only (the capacity gate the tick drain re-checks)
				if (mTE.slotHas(SLOT_MASS) && !mTE.allowInsertion(aStack)) return aStack;
				return ItemStack.EMPTY;
			}
			return mTE.insertItems(aStack, false);
		}

		@Override
		public ItemStack extractItem(int aSlot, int aAmount, boolean aSimulate) {
			if (!mTE.canExtractItem(aSlot, mSide)) return ItemStack.EMPTY;
			ItemStack tContent = mTE.slot(aSlot);
			if (tContent.isEmpty() || aAmount <= 0) return ItemStack.EMPTY;
			int tTake = Math.min(Math.min(aAmount, tContent.getCount()), Integer.MAX_VALUE);
			if (aSimulate) return mTE.slot(aSlot).copyWithCount(tTake);
			ItemStack rOut = mTE.slot(aSlot).copyWithCount(tTake);
			if (tTake >= tContent.getCount()) {
				mTE.slotKill(aSlot);
			} else {
				tContent.setCount(tContent.getCount() - tTake);
			}
			mTE.updateInventory();
			mTE.updateClientData();
			return rOut;
		}

		@Override
		public int getSlotLimit(int aSlot) {
			return aSlot == SLOT_MASS ? mTE.mMaxStorage : GTItemMover.DEFAULT_MAX_SLOT_SIZE;
		}

		@Override
		public boolean isItemValid(int aSlot, ItemStack aStack) {
			return mTE.canInsertItem(aSlot, aStack, mSide);
		}
	}

	// ---------------------------------------------------------------------------
	// insert / emit (upstream :377-434)
	// ---------------------------------------------------------------------------

	/** Upstream getMaxContent :377-379 — the overflow mode carries a 256 headroom above capacity. */
	public int getMaxContent() {
		return (mMode & MODE_EMIT_OVERFLOW) != 0 ? mMaxStorage + 256 : mMaxStorage;
	}

	/**
	 * Upstream insertItems :382-426 — @return the leftover (null = fully consumed).
	 * The NEI-infinite arms cut (class doc).
	 */
	@Nullable
	public ItemStack insertItems(ItemStack aStack, boolean aCheckForNEI) {
		if (aStack.isEmpty() || (mMode & MODE_TAPED) != 0) return aStack;

		if (!slotHas(SLOT_MASS)) {
			mLogisticsCache = null;
			slot(SLOT_MASS, aStack.copyWithCount(aStack.getCount()));
			updateClientData();
			updateInventory();
			if ((mMode & MODE_EMIT_OVERFLOW) != 0 && aStack.getCount() > mMaxStorage) emitOverflow();
			return null;
		}

		int tMaxStorage = getMaxContent();
		ItemStack tContent = slot(SLOT_MASS);

		if (tContent.getCount() >= tMaxStorage) return aStack;

		if (equalStacks(aStack, tContent)) {
			ItemStack rStack = null;
			if ((long)aStack.getCount() + tContent.getCount() > tMaxStorage) {
				rStack = aStack.copyWithCount((int)((long)aStack.getCount() + tContent.getCount() - tMaxStorage));
			}
			tContent.setCount((int)Math.min(tMaxStorage, tContent.getCount() + (long)aStack.getCount()));
			updateInventory();
			if ((mMode & MODE_EMIT_OVERFLOW) != 0 && tContent.getCount() > mMaxStorage) emitOverflow();
			return rStack;
		}

		// the cross-form arm: compatible prefixes of the same material convert into
		// partial units and the stack is CONSUMED (upstream :421-425)
		if (updatePartialContent(getUnitAmount(aStack) * aStack.getCount())) {
			if ((mMode & MODE_EMIT_OVERFLOW) != 0 && tContent.getCount() > mMaxStorage) emitOverflow();
			return null;
		}
		return aStack;
	}

	// ---------------------------------------------------------------------------
	// the ConnectedInventory arms (upstream :568-591 + :552; the interface seat defers
	// to the consuming card — the ACT cut the domain, decisions.p24-act-deviations ③)
	// ---------------------------------------------------------------------------

	/** Upstream getAmountOfItemsInConnectedInventory :552. */
	public long getAmountOfItemsInConnectedInventory(byte aSide, ItemStack aStack, long aStopCountingAtThisNumber) {
		return slotHas(SLOT_MASS) && equalStacks(slot(SLOT_MASS), aStack) ? slot(SLOT_MASS).getCount() : 0;
	}

	/** Upstream addStackToConnectedInventory :568-577. @return the added count */
	public int addStackToConnectedInventory(byte aSide, ItemStack aStack, boolean aOnlyAddIfItAlreadyHasItemsOfThatTypeOrIsDedicated) {
		if ((mMode & MODE_TAPED) != 0) return 0;
		if (!aOnlyAddIfItAlreadyHasItemsOfThatTypeOrIsDedicated || slotHas(SLOT_MASS)) {
			ItemStack tStack = insertItems(aStack, false);
			if (tStack == null) return aStack.getCount();
			if (tStack.getCount() < aStack.getCount()) return aStack.getCount() - tStack.getCount();
		}
		return 0;
	}

	/** Upstream removeStackFromConnectedInventory :579-591. @return the removed count */
	public int removeStackFromConnectedInventory(byte aSide, ItemStack aStack, boolean aOnlyRemoveIfItCanRemoveAllAtOnce) {
		if ((mMode & MODE_TAPED) != 0) return 0;
		if (slotHas(SLOT_MASS) && equalStacks(slot(SLOT_MASS), aStack)) {
			if (aOnlyRemoveIfItCanRemoveAllAtOnce && slot(SLOT_MASS).getCount() < aStack.getCount()) return 0;
			int tAmount = (int)Math.min((long)aStack.getCount(), slot(SLOT_MASS).getCount());
			slot(SLOT_MASS).setCount(slot(SLOT_MASS).getCount() - tAmount);
			if ((mMode & MODE_FILTER_RESET) != 0 && mPartialUnits <= 0 && slotNull(SLOT_MASS)) updateClientData();
			updateInventory();
			return tAmount;
		}
		return 0;
	}

	/** Upstream getProgressValue/getProgressMax :553-554 (the progress-meter face). */
	public long getProgressValue(byte aSide) {
		return slotHas(SLOT_MASS) ? slot(SLOT_MASS).getCount() : 0;
	}

	public long getProgressMax(byte aSide) {
		return mMaxStorage;
	}

	/** Upstream canDrop/keepSlot :555-556 — the taped mass stack rides the harvested item (the GTEntityBlock probe face). */
	public boolean canDrop(int aSlot) {
		return !(aSlot == SLOT_MASS && (mMode & MODE_TAPED) != 0);
	}

	/** Upstream onExploded :544 — the mass store does not survive a blast (the funnel slot pops via the block fallback). */
	public void killForExplosion() {
		slotKill(SLOT_MASS);
	}

	/** Upstream breakBlock :559-566 — the partial residue drops before the inventory pop. */
	public void dropPartialUnits() {
		if (hasLevel() && !isClientSide() && mPartialUnits > 0) {
			ItemStack tPartial = getPartialStack();
			if (!tPartial.isEmpty()) {
				net.minecraft.world.level.block.Block.popResource(getLevel(), getBlockPos(), tPartial);
			}
			mPartialUnits = 0;
		}
	}

	/**
	 * The digit strip colour — the standard set renders WHITE digits (upstream :125
	 * CA_WHITE) and both sets render the RED "100%" at full (:117-124 CA_RED_255).
	 * The logistics twin overrides to cyan (:126 CA_CYAN_255).
	 */
	public int digitARGB() {
		return 0xFFFFFFFF;
	}

	/** The full-state red (upstream CA_RED_255, both classes :120-123). */
	public static final int FULL_DIGIT_ARGB = 0xFFFF3C3C;

	// ---------------------------------------------------------------------------
	// the tool arms (upstream onToolClick2 :122-238 — the block carrier routes here)
	// ---------------------------------------------------------------------------

	/** The pincers take-all (upstream :127-152 — the 27-slot main-inventory walk). @return the moved count */
	public int pincersTakeAll(Player aPlayer) {
		if ((mMode & MODE_TAPED) != 0) return 0;
		int rCount = 0;
		if (mPartialUnits > 0) {
			aPlayer.getInventory().placeItemBackInInventory(getPartialStack());
			mPartialUnits = 0;
			rCount += 10;
		}
		// the upstream ST.move(this, aPlayerInventory, 0, j) walk: pour funnel then mass
		// store into the player inventory; what does not fit STAYS in the storage
		for (int aSlot = 0; aSlot < 2; aSlot++) {
			if (!slotHas(aSlot)) continue;
			int tMaxStack = Math.max(1, slot(aSlot).getMaxStackSize());
			for (int i = 0; i < aPlayer.getInventory().getContainerSize() && slotHas(aSlot); i++) {
				int tChunk = Math.min(slot(aSlot).getCount(), tMaxStack);
				ItemStack tMove = slot(aSlot).copyWithCount(tChunk);
				aPlayer.getInventory().add(tMove); // mutates: the unfitted count stays on tMove
				int tMoved = tChunk - tMove.getCount();
				if (tMoved <= 0) break;
				if (tMoved >= slot(aSlot).getCount()) {
					slotKill(aSlot);
				} else {
					slot(aSlot).setCount(slot(aSlot).getCount() - tMoved);
				}
				rCount += tMoved;
			}
		}
		if (rCount > 0) {
			playCollectSound();
			aPlayer.getInventory().setChanged();
			updateClientData();
			updateInventory();
		}
		return rCount;
	}

	/** The soft hammer eject (upstream :153-183): the funnel stack or full max-stack chunks off the mass store. */
	public void softHammerEject() {
		if ((mMode & MODE_TAPED) != 0) return;
		if (slotHas(SLOT_FUNNEL)) {
			spawnAtFront(slot(SLOT_FUNNEL).copy());
			slotKill(SLOT_FUNNEL);
			updateInventory();
			return;
		}
		if (slotHas(SLOT_MASS)) {
			int tMaxStack = Math.max(1, slot(SLOT_MASS).getMaxStackSize());
			for (int i = 0; i < 128 && slot(SLOT_MASS).getCount() > tMaxStack; i++) {
				spawnAtFront(slot(SLOT_MASS).copyWithCount(tMaxStack));
				slot(SLOT_MASS).setCount(slot(SLOT_MASS).getCount() - tMaxStack);
			}
			if (mPartialUnits > 0) {
				ItemStack tPartial = getPartialStack();
				if (!tPartial.isEmpty()) net.minecraft.world.level.block.Block.popResource(getLevel(), getBlockPos(), tPartial);
				mPartialUnits = 0;
			}
			if (slotNull(SLOT_MASS)) {
				updateClientData();
			} else if (slot(SLOT_MASS).getCount() > 0) {
				if (slot(SLOT_MASS).getCount() <= tMaxStack) {
					spawnAtFront(slot(SLOT_MASS).copy());
					slotKill(SLOT_MASS);
					updateClientData();
				}
			} else {
				slotKill(SLOT_MASS);
				updateClientData();
			}
			updateInventory();
		}
	}

	/** The cutter toggle (upstream :202-208). @return the chat line */
	public String cutterToggle() {
		mMode ^= MODE_EMIT_OVERFLOW;
		updateClientData();
		updateInventory();
		return (mMode & MODE_EMIT_OVERFLOW) == 0 ? "Won't emit Overflow" : "Will emit Overflow to Inventories below";
	}

	/** The screwdriver toggle (upstream :209-216). @return the chat line */
	public String screwdriverToggle() {
		mMode ^= MODE_FILTER_RESET;
		String rLine = (mMode & MODE_FILTER_RESET) == 0 ? "Filter stays when empty" : "Filter resets when empty";
		if (!allowZeroStacks(SLOT_MASS)) slotNull(SLOT_MASS);
		updateClientData();
		updateInventory();
		return rLine;
	}

	/** The monkey wrench toggle (upstream :217-223). @return the chat line */
	public String monkeyWrenchToggle() {
		mMode ^= MODE_FILL_BELOW;
		updateClientData();
		updateInventory();
		return (mMode & MODE_FILL_BELOW) == 0 ? "Won't fill Inventories below" : "Will fill Inventories below";
	}

	/** The magnifying lens status (upstream :224-237). @return the chat lines */
	public List<Component> lensStatus() {
		List<Component> rLines = new ArrayList<>();
		rLines.add(Component.literal(slotHas(SLOT_MASS)
				? "Contains: " + slot(SLOT_MASS).getCount() + " " + slot(SLOT_MASS).getHoverName().getString()
				: "Storage is empty"));
		rLines.add(Component.literal((mMode & MODE_FILL_BELOW) == 0 ? "Won't fill Inventories below" : "Will fill Inventories below"));
		rLines.add(Component.literal((mMode & MODE_FILTER_RESET) == 0 ? "Filter stays when empty" : "Filter resets when empty"));
		rLines.add(Component.literal((mMode & MODE_EMIT_OVERFLOW) == 0 ? "Won't emit Overflow" : "Will emit Overflow to Inventories below"));
		if ((mMode & MODE_TAPED) != 0) rLines.add(Component.literal("Will keep content when harvested."));
		return rLines;
	}

	/**
	 * The pixel-grid face (upstream onBlockActivated3 :241-331) — the block carrier's
	 * use() routes here with the front-face hit fractions and the USED-hand stack
	 * (upstream getCurrentEquippedItem). @return the click consumed
	 */
	public boolean clickFrontFace(Player aPlayer, ItemStack tHeld, byte aSide, float aHitX, float aHitY, float aHitZ) {
		if (aSide != getFacing() || (mMode & MODE_TAPED) != 0) return false;
		float[] tCoords = facingCoordsClicked(aSide, aHitX, aHitY, aHitZ);
		if (tCoords[0] < PX_P[1] || tCoords[0] > PX_N[1] || tCoords[1] < PX_P[1] || tCoords[1] > PX_N[1]) return false;
		if (isClientSide() || aPlayer == null) return true;

		updatePartialContent();
		if (slotHas(SLOT_MASS)) {
			int tAmount = takeTierAt(tCoords[0], tCoords[1]);
			if (tAmount > 0) {
				tAmount = Math.min(tAmount, slot(SLOT_MASS).getCount());
				if (tAmount > 0) {
					slot(SLOT_MASS).setCount(slot(SLOT_MASS).getCount() - tAmount);
					int tLeft = tAmount;
					while (tLeft > 0 && slotHas(SLOT_MASS)) {
						int tChunk = Math.min(tLeft, Math.max(1, slot(SLOT_MASS).getMaxStackSize()));
						tLeft -= tChunk;
						spawnAtFront(slot(SLOT_MASS).copyWithCount(tChunk));
					}
					if (slotHas(SLOT_MASS) && slot(SLOT_MASS).getCount() <= 0) slotKill(SLOT_MASS);
					updateInventory();
					playCollectSound();
				}
			} else {
				if (!tHeld.isEmpty()) {
					ItemStack tLeftover = insertItems(tHeld, false);
					if (tLeftover == null) {
						tHeld.setCount(0);
						playCollectSound();
					} else if (tLeftover.getCount() < tHeld.getCount()) {
						tHeld.setCount(tLeftover.getCount());
						playCollectSound();
					}
				} else {
					if (tAmount == -1) {
						// the centre cell: insert EVERY compatible stack from the player
						// inventory (upstream :289-309, breaking at the first refusal)
						boolean tTemp = false;
						for (int i = 0; i < aPlayer.getInventory().getContainerSize(); i++) {
							ItemStack tInvStack = aPlayer.getInventory().getItem(i);
							if (!tInvStack.isEmpty() && allowInsertion(tInvStack)) {
								ItemStack tLeftover = insertItems(tInvStack, false);
								if (tLeftover == null) {
									tTemp = true;
									aPlayer.getInventory().setItem(i, ItemStack.EMPTY);
									continue;
								}
								if (tLeftover.getCount() < tInvStack.getCount()) {
									tTemp = true;
									aPlayer.getInventory().setItem(i, tLeftover);
									continue;
								}
								break;
							}
						}
						if (tTemp) {
							aPlayer.getInventory().setChanged();
							playCollectSound();
						}
					}
				}
			}
			if ((mMode & MODE_FILTER_RESET) != 0 && mPartialUnits <= 0 && slotNull(SLOT_MASS)) {
				updateClientData();
				updateInventory();
			}
		} else {
			if (!tHeld.isEmpty()) {
				ItemStack tLeftover = insertItems(tHeld, false);
				if (tLeftover == null) {
					tHeld.setCount(0);
					playCollectSound();
				} else if (tLeftover.getCount() < tHeld.getCount()) {
					tHeld.setCount(tLeftover.getCount());
					playCollectSound();
				}
			}
		}
		updatePartialContent();
		return true;
	}

	// ---------------------------------------------------------------------------
	// partial units (upstream :593-698 over the MaterialPrefixItem resolution face)
	// ---------------------------------------------------------------------------

	/** The (prefix, material) answer of a stack — the MaterialPrefixItem identity face (class doc). */
	public static OreDictPrefix prefixOf(ItemStack aStack) {
		return aStack.getItem() instanceof MaterialPrefixItem tItem ? tItem.prefix : null;
	}

	public static OreDictMaterial materialOf(ItemStack aStack) {
		return aStack.getItem() instanceof MaterialPrefixItem tItem ? tItem.material : null;
	}

	/** Upstream getPartialStack :593-624 — the smallest-form refund of the residue. */
	public ItemStack getPartialStack() {
		if (mPartialUnits <= 0 || !slotHas(SLOT_MASS)) return ItemStack.EMPTY;
		OreDictPrefix tPrefix = prefixOf(slot(SLOT_MASS));
		OreDictMaterial tMaterial = materialOf(slot(SLOT_MASS));
		if (tPrefix == null || tMaterial == null) return ItemStack.EMPTY;
		OreDictPrefix tRefund = refundPrefixOf(tPrefix);
		if (tRefund == null) return ItemStack.EMPTY;
		// the smallest-form count: the family refund units the residue verbatim
		// (dust/ingot = the residue count directly, :598-599; the shaped families
		// divide by the refund form's unit, :600-621)
		if (tRefund == OP.dust || tRefund == OP.ingot) return countOf(GTMaterialItems.stackOf(tRefund, tMaterial), mPartialUnits);
		return countOf(GTMaterialItems.stackOf(tRefund, tMaterial), mPartialUnits / tRefund.mAmount);
	}

	/**
	 * The refund-form decision (upstream :598-621 verbatim, the pure face): which
	 * smallest prefix answers the residue of a stored prefix, null when none.
	 */
	public static OreDictPrefix refundPrefixOf(OreDictPrefix tPrefix) {
		if (tPrefix.contains(TD.Prefix.DUST_BASED)) return OP.dust;
		if (tPrefix.contains(TD.Prefix.INGOT_BASED)) return OP.ingot;
		if (tPrefix.contains(TD.Prefix.WIRE_BASED)) return OP.wireGt01;
		if (tPrefix == OP.gem || tPrefix == OP.blockGem) return OP.gem;
		if (tPrefix == OP.plate || tPrefix == OP.blockPlate) return OP.plate;
		if (tPrefix == OP.plateGem || tPrefix == OP.blockPlateGem) return OP.plateGem;
		if (tPrefix == OP.crushed || tPrefix == OP.crushedTiny) return OP.crushedTiny;
		if (tPrefix == OP.crushedPurified || tPrefix == OP.crushedPurifiedTiny) return OP.crushedPurifiedTiny;
		if (tPrefix == OP.crushedCentrifuged || tPrefix == OP.crushedCentrifugedTiny) return OP.crushedCentrifugedTiny;
		if (tPrefix == OP.oreRaw || tPrefix == OP.blockRaw) return OP.oreRaw;
		return null;
	}

	/**
	 * The stackability identity (the ST.equal face): the forge leg rides
	 * {@code ItemHandlerHelper.canItemStacksStack} (same item + same tag — the
	 * GTItemMover.canPut duality), the 21.1 leg the vanilla same-item-same-components.
	 */
	private static boolean equalStacks(ItemStack aA, ItemStack aB) {
		//? if forge {
		return aA.isEmpty() == aB.isEmpty() && net.minecraftforge.items.ItemHandlerHelper.canItemStacksStack(aA, aB);
		//?} else {
		/*return ItemStack.isSameItemSameComponents(aA, aB);
		*///?}
	}

	/** The count-clamped material stack helper (the ST.amount face). */
	private static ItemStack countOf(ItemStack aTemplate, long aCount) {
		if (aTemplate.isEmpty() || aCount <= 0) return ItemStack.EMPTY;
		return aTemplate.copyWithCount((int)Math.min(Integer.MAX_VALUE, aCount));
	}

	/** Upstream getUnitAmount(OreDictPrefix) :626-628. */
	public static long unitAmountOf(OreDictPrefix aPrefix) {
		return aPrefix == OP.oreRaw ? U : aPrefix == OP.blockRaw ? U * 9 : aPrefix.mAmount;
	}

	/**
	 * Upstream getUnitAmount(ItemStack) :630-665 — the inserted form's unit when it is a
	 * compatible prefix of the SAME material as the store, else 0.
	 */
	public long getUnitAmount(ItemStack aStack) {
		OreDictPrefix tStored = prefixOf(slot(SLOT_MASS));
		OreDictMaterial tStoredMat = materialOf(slot(SLOT_MASS));
		OreDictPrefix tForm = prefixOf(aStack);
		OreDictMaterial tFormMat = materialOf(aStack);
		if (tStored == null || tForm == null || tStoredMat == null || tFormMat == null || tStoredMat != tFormMat
				|| mPartialUnits >= unitAmountOf(tStored)) {
			return 0;
		}
		if (tStored.contains(TD.Prefix.DUST_BASED)) return tForm.contains(TD.Prefix.DUST_BASED) ? tForm.mAmount : 0;
		if (tStored.contains(TD.Prefix.INGOT_BASED)) return tForm.contains(TD.Prefix.INGOT_BASED) ? tForm.mAmount : 0;
		if (tStored.contains(TD.Prefix.WIRE_BASED)) return tForm.contains(TD.Prefix.WIRE_BASED) ? tForm.mAmount : 0;
		if (tStored == OP.gem || tStored == OP.blockGem) return tForm == OP.gem || tForm == OP.blockGem ? tForm.mAmount : 0;
		if (tStored == OP.plate || tStored == OP.blockPlate) return tForm == OP.plate || tForm == OP.blockPlate ? tForm.mAmount : 0;
		if (tStored == OP.plateGem || tStored == OP.blockPlateGem) return tForm == OP.plateGem || tForm == OP.blockPlateGem ? tForm.mAmount : 0;
		if (tStored == OP.crushed || tStored == OP.crushedTiny) return tForm == OP.crushed || tForm == OP.crushedTiny ? tForm.mAmount : 0;
		if (tStored == OP.crushedPurified || tStored == OP.crushedPurifiedTiny) return tForm == OP.crushedPurified || tForm == OP.crushedPurifiedTiny ? tForm.mAmount : 0;
		if (tStored == OP.crushedCentrifuged || tStored == OP.crushedCentrifugedTiny) return tForm == OP.crushedCentrifuged || tForm == OP.crushedCentrifugedTiny ? tForm.mAmount : 0;
		if (tStored == OP.oreRaw || tStored == OP.blockRaw) return tForm == OP.oreRaw ? U : tForm == OP.blockRaw ? U * 9 : 0;
		return 0;
	}

	/** Upstream allowInsertion :667-671. */
	public boolean allowInsertion(ItemStack aStack) {
		if ((mMode & MODE_TAPED) != 0) return false;
		if (slotHas(SLOT_MASS) && equalStacks(slot(SLOT_MASS), aStack)) return true;
		return getUnitAmount(aStack) > 0;
	}

	/** Upstream updatePartialContent(long) :673-677. */
	public boolean updatePartialContent(long aAmountAdded) {
		if (aAmountAdded <= 0) return false;
		mPartialUnits += aAmountAdded;
		return updatePartialContent();
	}

	/** Upstream updatePartialContent() :679-698 — the residue converts into whole stacks. */
	public boolean updatePartialContent() {
		int tMaxStorage = getMaxContent();
		ItemStack tContent = slot(SLOT_MASS);
		if (mPartialUnits > 0 && slotHas(SLOT_MASS) && tContent.getCount() < tMaxStorage) {
			OreDictPrefix tPrefix = prefixOf(tContent);
			if (tPrefix != null) {
				long tTargetAmount = unitAmountOf(tPrefix);
				if (mPartialUnits >= tTargetAmount) {
					long tStackCount = mPartialUnits / tTargetAmount;
					if (tStackCount > 0) {
						mPartialUnits -= tTargetAmount * tStackCount;
						if (tStackCount + tContent.getCount() > tMaxStorage) {
							mPartialUnits += tTargetAmount * (tStackCount + tContent.getCount() - tMaxStorage);
						}
						tContent.setCount((int)Math.min(tMaxStorage, tContent.getCount() + tStackCount));
						updateInventory();
					}
				}
			}
		}
		return true;
	}

	// ---------------------------------------------------------------------------
	// the logistics faces (upstream :436-505; the expansion over registered items)
	// ---------------------------------------------------------------------------

	/** The expansion cache (upstream mLogisticsCache :436). */
	private Set<ItemStack> mLogisticsCache = null;

	@Override
	public java.util.Collection<ItemStack> getLogisticsFilter(byte aSide) {
		if (!slotHas(SLOT_MASS)) return mLogisticsCache = null;
		if (mLogisticsCache != null) return mLogisticsCache;
		Set<ItemStack> rCache = new LinkedHashSet<>();
		ItemStack tContent = slot(SLOT_MASS);
		rCache.add(tContent.copyWithCount(1));
		OreDictPrefix tPrefix = prefixOf(tContent);
		OreDictMaterial tMaterial = materialOf(tContent);
		if (tPrefix != null && tMaterial != null) {
			if (tPrefix.contains(TD.Prefix.DUST_BASED)) {
				addAll(rCache, OP.blockDust, tMaterial);
				addAll(rCache, OP.dust, tMaterial);
				addAll(rCache, OP.dustSmall, tMaterial);
				addAll(rCache, OP.dustTiny, tMaterial);
				addAll(rCache, OP.dustDiv72, tMaterial);
			} else if (tPrefix.contains(TD.Prefix.INGOT_BASED)) {
				addAll(rCache, OP.blockIngot, tMaterial);
				addAll(rCache, OP.ingot, tMaterial);
				addAll(rCache, OP.billet, tMaterial);
				addAll(rCache, OP.chunkGt, tMaterial);
				addAll(rCache, OP.nugget, tMaterial);
			} else if (tPrefix.contains(TD.Prefix.WIRE_BASED)) {
				addAll(rCache, OP.wireGt01, tMaterial);
				addAll(rCache, OP.wireGt02, tMaterial);
				addAll(rCache, OP.wireGt03, tMaterial);
				addAll(rCache, OP.wireGt04, tMaterial);
				addAll(rCache, OP.wireGt05, tMaterial);
				addAll(rCache, OP.wireGt06, tMaterial);
				addAll(rCache, OP.wireGt07, tMaterial);
				addAll(rCache, OP.wireGt08, tMaterial);
				addAll(rCache, OP.wireGt09, tMaterial);
				addAll(rCache, OP.wireGt10, tMaterial);
				addAll(rCache, OP.wireGt11, tMaterial);
				addAll(rCache, OP.wireGt12, tMaterial);
				addAll(rCache, OP.wireGt13, tMaterial);
				addAll(rCache, OP.wireGt14, tMaterial);
				addAll(rCache, OP.wireGt15, tMaterial);
				addAll(rCache, OP.wireGt16, tMaterial);
			} else if (tPrefix == OP.gem || tPrefix == OP.blockGem) {
				addAll(rCache, OP.gem, tMaterial);
				addAll(rCache, OP.blockGem, tMaterial);
			} else if (tPrefix == OP.plate || tPrefix == OP.blockPlate) {
				addAll(rCache, OP.plate, tMaterial);
				addAll(rCache, OP.blockPlate, tMaterial);
			} else if (tPrefix == OP.plateGem || tPrefix == OP.blockPlateGem) {
				addAll(rCache, OP.plateGem, tMaterial);
				addAll(rCache, OP.blockPlateGem, tMaterial);
			} else if (tPrefix == OP.crushed || tPrefix == OP.crushedTiny) {
				addAll(rCache, OP.crushed, tMaterial);
				addAll(rCache, OP.crushedTiny, tMaterial);
			} else if (tPrefix == OP.crushedPurified || tPrefix == OP.crushedPurifiedTiny) {
				addAll(rCache, OP.crushedPurified, tMaterial);
				addAll(rCache, OP.crushedPurifiedTiny, tMaterial);
			} else if (tPrefix == OP.crushedCentrifuged || tPrefix == OP.crushedCentrifugedTiny) {
				addAll(rCache, OP.crushedCentrifuged, tMaterial);
				addAll(rCache, OP.crushedCentrifugedTiny, tMaterial);
			} else if (tPrefix == OP.oreRaw || tPrefix == OP.blockRaw) {
				addAll(rCache, OP.oreRaw, tMaterial);
				addAll(rCache, OP.blockRaw, tMaterial);
			}
		}
		mLogisticsCache = rCache;
		return rCache;
	}

	/** The registered-item expansion arm (the OreDictManager.getOres face — registered items only). */
	private static void addAll(Set<ItemStack> aCache, OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		ItemStack tStack = GTMaterialItems.stackOf(aPrefix, aMaterial); // the registered-items-only face, both legs
		if (!tStack.isEmpty()) aCache.add(tStack);
	}

	/** Upstream canLogistics :501. */
	public boolean canLogistics(byte aSide) {
		return true;
	}

	/** Upstream getLogisticsPriorityItem :502. */
	public int getLogisticsPriorityItem() {
		return slotHas(SLOT_MASS) ? 2 : 1;
	}

	/** Upstream getLogisticsPriorityFluid :503. */
	public int getLogisticsPriorityFluid() {
		return 0;
	}

	/** Upstream getLogisticsFilterFluid :504. */
	@Nullable
	public net.minecraft.world.level.material.Fluid getLogisticsFilterFluid() {
		return null;
	}

	/** Upstream getLogisticsFilterItem :505. */
	@Nullable
	public ItemStack getLogisticsFilterItem() {
		return null;
	}

	// ---------------------------------------------------------------------------
	// the sound + spawn helpers (the SFX.MC_COLLECT/ST.place faces)
	// ---------------------------------------------------------------------------

	/** The collect blip (upstream playCollect / SFX.MC_COLLECT :146/:276). */
	public void playCollectSound() {
		if (hasLevel()) {
			getLevel().playSound(null, getBlockPos(), net.minecraft.sounds.SoundEvents.ITEM_PICKUP,
					net.minecraft.sounds.SoundSource.BLOCKS, 0.2F, ((getLevel().random.nextFloat() - getLevel().random.nextFloat()) * 0.7F + 1.0F) * 2.0F);
		}
	}

	/** The front-face spawn point (upstream getOffsetX/Y/Z(mFacing) + 0.5 :273/:156). */
	private void spawnAtFront(ItemStack aStack) {
		if (!hasLevel() || aStack.isEmpty()) return;
		Direction tFront = Direction.from3DDataValue(getFacing());
		net.minecraft.world.level.block.Block.popResource(getLevel(), getBlockPos().relative(tFront), aStack);
	}

	/**
	 * The take/place tier table (upstream :251-265 verbatim, face-local u/v): three
	 * 2px rows of left/right take cells (8/64, 4/32, 1/16 top to bottom), the centre
	 * column (4..12px, from y 6px up) the -1 insert-all cell, else 0 = the plain insert.
	 */
	public static int takeTierAt(float aU, float aV) {
		int tAmount = 0;
		if (aV >= PX_P[6] && aV <= PX_P[8]) {
			if (aU >= PX_P[1] && aU <= PX_P[3]) tAmount = 8;
			if (aU >= PX_N[3] && aU <= PX_N[1]) tAmount = 64;
		}
		if (aV >= PX_P[9] && aV <= PX_P[11]) {
			if (aU >= PX_P[1] && aU <= PX_P[3]) tAmount = 4;
			if (aU >= PX_N[3] && aU <= PX_N[1]) tAmount = 32;
		}
		if (aV >= PX_P[12] && aV <= PX_P[14]) {
			if (aU >= PX_P[1] && aU <= PX_P[3]) tAmount = 1;
			if (aU >= PX_N[3] && aU <= PX_N[1]) tAmount = 16;
		}
		if (aV >= PX_P[6]) {
			if (aU >= PX_P[4] && aU <= PX_N[4]) tAmount = -1;
		}
		return tAmount;
	}

	/** The UT.Code.getFacingCoordsClicked face (UT.java:1734-1744, the horizontal four). */
	public static float[] facingCoordsClicked(byte aSide, float aHitX, float aHitY, float aHitZ) {
		return switch (aSide) {
			case 2 -> new float[] {Math.min(0.99F, Math.max(0, 1 - aHitX)), Math.min(0.99F, Math.max(0, 1 - aHitY))};
			case 3 -> new float[] {Math.min(0.99F, Math.max(0, aHitX)), Math.min(0.99F, Math.max(0, 1 - aHitY))};
			case 4 -> new float[] {Math.min(0.99F, Math.max(0, aHitZ)), Math.min(0.99F, Math.max(0, 1 - aHitY))};
			case 5 -> new float[] {Math.min(0.99F, Math.max(0, 1 - aHitZ)), Math.min(0.99F, Math.max(0, 1 - aHitY))};
			default -> new float[] {0.5F, 0.5F};
		};
	}

	// ---------------------------------------------------------------------------
	// NBT (the plain in-repo key form)
	// ---------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putByte(NBT_FACING, mFacingNbt);
		aNBT.putByte(NBT_MODE, mMode);
		aNBT.putInt(NBT_CAPACITY, mMaxStorage);
		aNBT.putLong(NBT_PARTIAL, mPartialUnits);
		//? if forge {
		aNBT.put(NBT_INVENTORY, mInventory.serializeNBT());
		//?} else {
		/*aNBT.put(NBT_INVENTORY, mInventory.serializeNBT(TileEntityBase03TicksAndSync.NBT_ACCESS)); // 21.1: provider-first
		 *///?}
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_FACING, Tag.TAG_ANY_NUMERIC)) mFacingNbt = aNBT.getByte(NBT_FACING);
		if (aNBT.contains(NBT_MODE, Tag.TAG_ANY_NUMERIC)) mMode = aNBT.getByte(NBT_MODE);
		if (aNBT.contains(NBT_CAPACITY, Tag.TAG_ANY_NUMERIC)) mMaxStorage = aNBT.getInt(NBT_CAPACITY);
		if (aNBT.contains(NBT_PARTIAL, Tag.TAG_ANY_NUMERIC)) mPartialUnits = aNBT.getLong(NBT_PARTIAL);
		if (aNBT.contains(NBT_INVENTORY, Tag.TAG_COMPOUND)) {
			//? if forge {
			mInventory.deserializeNBT(aNBT.getCompound(NBT_INVENTORY));
			//?} else {
			/*mInventory.deserializeNBT(TileEntityBase03TicksAndSync.NBT_ACCESS, aNBT.getCompound(NBT_INVENTORY)); // 21.1: provider-first
			 *///?}
		}
	}
}
