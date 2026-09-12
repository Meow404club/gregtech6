package gregtech6.tileentity.inventories;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;

//? if forge {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
//?}
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import gregtech6.tileentity.GTItemStackHandler;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.util.GTItemMover;

/**
 * The shared transfer engine of the storage-hopper family — 1.20.1 counterpart of the two
 * upstream BEs' common skeleton (task p26-storage-hopper-family spec ④, the "搬运数学逐字"
 * clause): {@code MultiTileEntityHopper.java:62-301} and
 * {@code MultiTileEntityQueueHopper.java:62-283}, both extending TileEntityBase09FacingSingle
 * and implementing ITileEntityAdjacentInventoryUpdatable. The two kinds share EVERYTHING
 * except the per-kind hooks; the transport math itself consumes {@link GTItemMover} (the
 * ST.move port — the card's "搬运层零新代码" ruling).
 *
 * <h2>The tick skeleton (upstream onTick2 :164-235 / :151-217, unified)</h2>
 * <pre>
 * if (mCheck &gt; 0) mCheck--;
 * else if ((mCheck == 0 || mInventoryChanged || mBlockUpdated || SYNC_SECOND) &amp;&amp; !hasRedstoneIncomingFromNonRail()) {
 *     tMovedItems += moveOutPhase();   // the kind's output loop (hook)
 *     tMovedItems += moveInPhase();    // the shared top input arm (suction slot = hook)
 *     mCheck = tMovedItems &gt; 0 ? 3 : -1;
 *     if (compressInsideGate() &amp;&amp; mInventoryChanged) tMovedItems += compressPhase(); // hopper :211-224
 * }
 * if (!compressInsideGate() &amp;&amp; mInventoryChanged) tMovedItems += compressPhase();      // queue :199-207
 * if (tMovedItems &gt; 0) notifyAdjacentInventories();                                     // :226-231 / :209-214
 * </pre>
 * The two upstream bodies differ in exactly WHERE the compression arm and the notify arm
 * sit: the hopper runs both inside the mCheck/redstone gate, the queue lifts the
 * compression OUT of the gate (an inventory change re-compacts even while redstone-blocked)
 * and the notify onto the tick tail. Hoisting the notify out of the hopper's gate is
 * tick-exact (the gate is the only thing that ever grows {@code tMovedItems} in the hopper).
 *
 * <h2>Folds (declared)</h2>
 * <ul>
 * <li>{@code SYNC_SECOND} (CS.java:316, written per tick at GT_API_Proxy.java:253 as
 *     {@code SERVER_TIME % 20 == 0}) folds onto the BE-local {@code mTimer % 20 == 0} — the
 *     per-BE phase instead of the global phase, a ≤20-tick skew of an OR-arm (the item pipe
 *     {@code (timer + offset) % period} fold precedent).</li>
 * <li>{@code mInventoryChanged} is driven by the {@link GTItemStackHandler} content hook
 *     (every mutation marks it — the external insert path of upstream 04/05 folded) and is
 *     reset in {@link #onTickResetChecks} exactly like TileEntityBase05Inventories.java:86-89.
 *     Internal moves therefore re-arm the gate for one extra pass after which the settled
 *     inventory moves nothing — the flag ledger converges to the upstream state.</li>
 * <li>The anvil speciality of the top arm (upstream
 *     {@code !(tDelegator.mTileEntity instanceof MultiTileEntityAnvil)} — the GT6 anvil's
 *     suck-what-falls-on-it pairing, MultiTileEntityHopper.java:191 for the hopper arm
 *     and MultiTileEntityQueueHopper.java:178 for the queue arm) is IMPLEMENTED as of
 *     task p28-c-anvil: the {@link #topIsAnvil} gate rides the shared drain arm exactly
 *     like those conjuncts, so a hopper under a GT6 anvil skips the (NO_SLOTS-refusing)
 *     drain half and sucks the item entities out of the anvil's block space — the anvil
 *     family card lifted the former defer (the former "no GT6 anvil class to test
 *     against" no longer holds).</li>
 * <li>The snowman walk-over easter egg (:156-159 / :143-147) is DEFERRED (card spec ⑦).</li>
 * </ul>
 */
public abstract class GT6HopperBaseBlockEntity extends TileEntityBase03TicksAndSync implements GT6AdjacentInventoryUpdatable {

	// -----------------------------------------------------------------------
	// side constants — the GT6 order == Direction.getIndex() (01Root doc)
	// -----------------------------------------------------------------------

	/** CS.SIDE_BOTTOM (upstream getDefaultSide :254/:236). */
	public static final byte SIDE_BOTTOM = 0;
	/** CS.SIDE_TOP — the input face. */
	public static final byte SIDE_TOP = 1;
	/** The side-less view (null-Direction capability queries) — "not the output face" rules. */
	public static final byte SIDE_ANY = 6;

	// -----------------------------------------------------------------------
	// NBT keys (upstream NBT_FACING / NBT_MODE / NBT_MODE+".a" — the in-repo
	// plain-key form, the GTExampleChestBlockEntity precedent)
	// -----------------------------------------------------------------------

	public static final String NBT_FACING = "facing";
	public static final String NBT_INVENTORY = "inventory";
	public static final String NBT_MODE = "mode";
	public static final String NBT_MODE_EXACT = "mode.a";

	/** The SYNC_SECOND period (CS.java:316 fold, see class doc). */
	public static final int SYNC_PERIOD = 20;

	/** The slot-less stand-in for "a tile entity without an inventory" (suppresses suction, moves nothing). */
	private static final GTItemStackHandler NO_SLOTS = new GTItemStackHandler(0);

	/** Upstream MultiTileEntityHopper.java:63 — the divisible/exact emission axis. */
	public boolean mExactMode = false;

	/** Upstream :63/:63 — the own-output-face extract bypass held around the output move. */
	public boolean mLock = false;

	/** Upstream :64 — the batch axis: 0..64 (hopper, 0 = "up to a stack per round") or 1..64 (queue, per-slot cap). */
	public byte mMode = 0;

	/** Upstream :64 — the 3-tick re-plan throttle: 3 countdown → process → 3 on movement, -1 idle. */
	public byte mCheck = 3;

	/** Upstream TileEntityBase05Inventories.java:44 — the compression/change trigger flag. */
	public boolean mInventoryChanged = false;

	/** Upstream 09FacingSingle:45 — mFacing = getDefaultSide() = SIDE_BOTTOM; the blockstate FACING is the live truth. */
	protected byte mFacing = SIDE_BOTTOM;

	/** Full constructor — the offline (test) entry point (the GTExampleChestBlockEntity shape). */
	public GT6HopperBaseBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(true, aType, aPos, aState);
		// inventorySize(aState) reads ONLY its argument (the block-carrier row) — safe from
		// the base ctor; the mMode read inside stackLimit() is lazy (the handler slot-limit
		// hook) because the subclass ctor body (mMode = defaultMode()) runs right after
		setInventory(new HopperInventory(inventorySize(aState), this::updateInventory));
	}

	/**
	 * The registration NBT_INV_SIZE (Loader_MultiTileEntities.java:145-146 —
	 * {@code Math.max(1|2, aHopperSize)}): read off the block-carrier row, with the
	 * plain-state offline fallback. Must not touch subclass fields (called from the base ctor).
	 */
	protected abstract int inventorySize(BlockState aState);

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_FACING, Tag.TAG_ANY_NUMERIC)) {
			mFacing = aNBT.getByte(NBT_FACING);
		}
		if (aNBT.contains(NBT_MODE, Tag.TAG_ANY_NUMERIC)) {
			mMode = aNBT.getByte(NBT_MODE);
			clampMode(); // upstream queue :70 verbatim; the hopper keeps 0..64 (declared defensive clamp)
		}
		if (aNBT.contains(NBT_MODE_EXACT)) {
			mExactMode = aNBT.getBoolean(NBT_MODE_EXACT);
		}
		if (aNBT.contains(NBT_INVENTORY, Tag.TAG_COMPOUND)) {
			deserializeInventory(aNBT.getCompound(NBT_INVENTORY));
		}
	}

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putByte(NBT_FACING, mFacing);
		// upstream writeToNBT2 :76/:76 — the default mode stays out of the disk NBT
		if (mMode != defaultMode()) aNBT.putByte(NBT_MODE, mMode);
		if (mExactMode) aNBT.putBoolean(NBT_MODE_EXACT, true);
		aNBT.put(NBT_INVENTORY, serializeInventory());
	}

	/** The ItemStackHandler NBT face (21.1 takes the registry provider — the 03 NBT_ACCESS seam). */
	private CompoundTag serializeInventory() {
		//? if forge {
		return mInventory.serializeNBT();
		//?} else {
		/*return mInventory.serializeNBT(gregtech6.tileentity.TileEntityBase03TicksAndSync.NBT_ACCESS); // 21.1: provider-first
		 *///?}
	}

	private void deserializeInventory(CompoundTag aTag) {
		//? if forge {
		mInventory.deserializeNBT(aTag);
		//?} else {
		/*mInventory.deserializeNBT(gregtech6.tileentity.TileEntityBase03TicksAndSync.NBT_ACCESS, aTag); // 21.1: provider-first
		 *///?}
	}

	// -----------------------------------------------------------------------
	// facing (upstream 09FacingSingle; the placement face rides the blockstate
	// FACING property, this NBT field is the plain-state/offline fallback)
	// -----------------------------------------------------------------------

	/** The output face: the blockstate FACING when present, the NBT byte otherwise. */
	public byte getFacing() {
		BlockState tState = getBlockState();
		if (tState != null && tState.hasProperty(BlockStateProperties.FACING)) {
			return (byte) tState.getValue(BlockStateProperties.FACING).get3DDataValue();
		}
		return mFacing;
	}

	/** The NBT-fallback byte write (the block's setPlacedBy keeps it in step; the state is the live truth). */
	public void setFacingNbtFallback(byte aFacing) {
		mFacing = aFacing;
	}

	// -----------------------------------------------------------------------
	// the 05 inventory vocabulary (upstream 05Inventories :91-103)
	// -----------------------------------------------------------------------

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

	protected boolean invempty() {
		for (int i = 0; i < invsize(); i++) {
			if (slotHas(i)) return false;
		}
		return true;
	}

	/** Upstream 05:103 updateInventory — the change flag (the handler hook funnels here). */
	public void updateInventory() {
		mInventoryChanged = true;
	}

	/** Upstream getInventoryStackLimit :248/:230 — the per-kind per-slot cap. */
	public abstract int stackLimit();

	/** The NBT-default mode (hopper 0, queue 64 — upstream writeToNBT2 :76/:76). */
	protected abstract byte defaultMode();

	/** The load-time mode clamp (upstream queue :70 {@code mMode <= 0 → 64}). */
	protected abstract void clampMode();

	/** The screwdriver cycle lower bound (hopper 0 :130-132, queue 1 :123-125). */
	protected abstract byte modeMin();

	/** The screwdriver cycle upper bound (64 both). */
	protected abstract byte modeMax();

	/** Where the compression arm sits (see class doc). */
	protected abstract boolean compressInsideGate();

	/** The kind's compression arm (dispatched by the tick skeleton). */
	protected abstract int compressPhase();

	/** The suction slot pick (hopper: the highest empty slot :195-196; queue: the last slot :182-183). */
	protected abstract int findSuctionSlot();

	/** The side slot set (hopper: all ascending :245; queue: {0, last} :227). */
	public abstract int[] getAccessibleSlotsFromSide(byte aSide);

	/** Upstream canInsertItem2 :246/:228. */
	public abstract boolean canInsertItem(int aSlot, ItemStack aStack, byte aSide);

	/** Upstream canExtractItem2 :247/:229. */
	public abstract boolean canExtractItem(int aSlot, byte aSide);

	// -----------------------------------------------------------------------
	// the tick skeleton (upstream onTick2, see class doc)
	// -----------------------------------------------------------------------

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		super.onTick(aTimer, aIsServerSide);
		if (!aIsServerSide) return;
		int tMovedItems = 0;
		if (mCheck > 0) {
			mCheck--;
		} else if ((mCheck == 0 || mInventoryChanged || mBlockUpdated || aTimer % SYNC_PERIOD == 0) && !hasRedstoneIncomingFromNonRail()) {
			tMovedItems += moveOutPhase();
			tMovedItems += moveInPhase();
			// upstream :206-210 / :193-197 verbatim
			mCheck = (byte) (tMovedItems > 0 ? 3 : -1);
			if (compressInsideGate() && mInventoryChanged) {
				tMovedItems += compressPhase();
			}
		}
		if (!compressInsideGate() && mInventoryChanged) {
			tMovedItems += compressPhase();
		}
		if (tMovedItems > 0) {
			notifyAdjacentInventories();
		}
	}

	@Override
	public void onTickResetChecks(long aTimer, boolean aIsServerSide) {
		super.onTickResetChecks(aTimer, aIsServerSide);
		mInventoryChanged = false; // upstream TileEntityBase05Inventories.java:86-89 verbatim
	}

	// -----------------------------------------------------------------------
	// the output arm (upstream :171-184 hopper / :158-172 queue)
	// -----------------------------------------------------------------------

	/** The upstream gate pair :171/:158 — never emit upward, never emit empty. */
	protected final boolean outputGatesOpen() {
		return getFacing() != SIDE_TOP && !invempty();
	}

	/**
	 * The kind's emission loop. The transfer rides
	 * {@link GTItemMover#move(IItemHandler, IItemHandler, int, int, int)} with the upstream
	 * ST.move(:457) quantity columns spelled out at the upstream values; the {@code mLock}
	 * belt is the upstream :178/:165 verbatim.
	 */
	protected abstract int moveOutPhase();

	// -----------------------------------------------------------------------
	// the top input arm (upstream :186-205 hopper / :173-192 queue)
	// -----------------------------------------------------------------------

	/**
	 * The shared shape: a container above (with the rail-minecart interception, :187-190 /
	 * :174-177) is drained into the own top side view with the ST.move defaults (:192/:179 =
	 * GTItemMover.move(64, 1, 64)); no container — OR an anvil above (the upstream
	 * {@code !(tDelegator.mTileEntity instanceof MultiTileEntityAnvil)} conjunct —
	 * MultiTileEntityHopper.java:191, the queue arm's twin at MultiTileEntityQueueHopper
	 * .java:178 — implemented by task p28-c-anvil after the former defer, the class doc)
	 * — and a see-through block space → the {@link #findSuctionSlot} hook picks the slot
	 * and one dropped item entity is sucked (:196-204 / :182-190, the WD.suck arm).
	 */
	protected int moveInPhase() {
		int rMoved = 0;
		IItemHandler tSource = topSource();
		if (tSource != null && !topIsAnvil()) { // upstream :191 (hopper) / :178 (queue) — the anvil refuses the drain half
			rMoved += GTItemMover.move(tSource, sideView(SIDE_TOP)); // upstream :192/:179 verbatim (the ST.move defaults)
		} else if (!topVisiblyOpaque()) {
			int tSlot = findSuctionSlot();
			if (tSlot >= 0 && !slotHas(tSlot)) {
				slot(tSlot, suckTopItem()); // upstream :197/:184 — WD.suck of the block space above
				if (slotHas(tSlot)) {
					rMoved += slot(tSlot).getCount();
					updateInventory();
				}
			}
		}
		return rMoved;
	}

	/**
	 * The upstream anvil gate (MultiTileEntityHopper.java:191, the queue arm's twin at
	 * MultiTileEntityQueueHopper.java:178) — the GT6 anvil's suck-what-falls-on-it pairing
	 * (task p28-c-anvil): a hopper under a {@link gregtech6.tileentity.tools.GT6AnvilBlockEntity}
	 * skips the drain half and sucks the item entities sitting in the anvil's block space
	 * (outputs spawn at y + 1.2 and land back inside it), never its working slots.
	 */
	protected boolean topIsAnvil() {
		return hasLevel() && !isClientSide()
				&& getLevel().getBlockEntity(getBlockPos().above()) instanceof gregtech6.tileentity.tools.GT6AnvilBlockEntity;
	}

	// -----------------------------------------------------------------------
	// the compression arms
	// -----------------------------------------------------------------------

	/**
	 * The hopper's slot compaction (:212-223 verbatim): for every slot i ascending, pull
	 * every later slot j into it while it fits — the inventory settles toward slot 0.
	 * {@code l = getInventoryStackLimit()} is the upstream :212 limit read.
	 */
	protected int compressPhaseHopper() {
		int rMoved = 0;
		for (int i = 0, k = invsize(), l = stackLimit(); i < k; i++) {
			for (int j = i + 1; j < k; j++) {
				if (slotHas(j)) {
					int tMaxSize = Math.min(l, slot(j).getMaxStackSize());
					if (slotHas(i)) {
						if (slot(i).getCount() < tMaxSize && stackablePair(slot(i), slot(j))) {
							rMoved += moveSlot(j, i);
							if (slot(i).getCount() >= tMaxSize) break;
						}
					} else {
						rMoved += moveSlot(j, i);
						if (slotHas(i) && slot(i).getCount() >= tMaxSize) break;
					}
				}
			}
		}
		return rMoved;
	}

	/**
	 * The queue's FIFO compaction fixed point (:200-206 verbatim): push every slot toward
	 * the END ({@code ST.move(this, i-1, i)} — ST.java:583 takes (aSlotFrom, aSlotTo)) until
	 * a full pass moves nothing; slot 0 = the newest, the last slot = the oldest (extracted
	 * first, :229).
	 */
	protected int compressPhaseQueue() {
		int rMoved = 0, oMovedItems = -1;
		while (oMovedItems != rMoved) {
			oMovedItems = rMoved;
			for (int i = 1, j = invsize(); i < j; i++) {
				rMoved += moveSlot(i - 1, i);
			}
		}
		return rMoved;
	}

	/**
	 * The same-inventory slot move — the ST.move(IInventory, aSlotFrom, aSlotTo) overload
	 * (ST.java:583-586 + move_ :615-629, the GTItemMover-trimmed same-inventory family),
	 * re-based onto the own handler. The count cap is the upstream
	 * {@code min(from.count, min(invStackLimit, to == null ? from.max : to.max - to.count))}.
	 */
	protected int moveSlot(int aSlotFrom, int aSlotTo) {
		if (aSlotFrom == aSlotTo) return 0; // ST.java:584
		ItemStack tFrom = mInventory.getStackInSlot(aSlotFrom), tTo = mInventory.getStackInSlot(aSlotTo);
		if (tFrom.isEmpty() || (!tTo.isEmpty() && !stackablePair(tTo, tFrom))) return 0; // :585 (equal_ → the canPut stackability mapping)
		int tCount = Math.min(tFrom.getCount(), Math.min(stackLimit(), tTo.isEmpty() ? tFrom.getMaxStackSize() : tTo.getMaxStackSize() - tTo.getCount())); // :586
		if (tCount <= 0) return 0; // move_ :618-619 (the 0-count decr yields nothing)
		// move_ :620-628 — extract then insert, the leftover returns (the no-loss invariant)
		ItemStack tExtracted = mInventory.extractItem(aSlotFrom, tCount, false);
		if (tExtracted.isEmpty()) return 0;
		tCount = Math.min(tCount, tExtracted.getCount());
		ItemStack tLeftover = mInventory.insertItem(aSlotTo, tExtracted, false);
		if (!tLeftover.isEmpty()) mInventory.insertItem(aSlotFrom, tLeftover, false);
		return tCount - tLeftover.getCount();
	}

	/** The ST.equal(_, _, F) mapping — the same predicate GTItemMover.canPut uses. */
	protected static boolean stackablePair(ItemStack aTo, ItemStack aFrom) {
		//? if forge {
		return ItemHandlerHelper.canItemStacksStack(aTo, aFrom);
		//?} else {
		/*return net.minecraft.world.item.ItemStack.isSameItemSameComponents(aTo, aFrom); // 21.1: the canPut fork reason verbatim
		 *///?}
	}

	/** The copy-at-count seam (1.20.1 ItemHandlerHelper.copyStackWithSize; 21.1 the vanilla copyWithCount). */
	static ItemStack copyAt(ItemStack aStack, int aCount) {
		//? if forge {
		return ItemHandlerHelper.copyStackWithSize(aStack, aCount);
		//?} else {
		/*return aStack.copyWithCount(aCount); // 21.1: copyStackWithSize deleted — vanilla copy-at-count
		 *///?}
	}

	// -----------------------------------------------------------------------
	// the adjacency wake (upstream :226-231 / :209-214 + the receiver :252/:234)
	// -----------------------------------------------------------------------

	/** The tick-tail fan-out to every non-top non-output neighbour implementing the interface. */
	protected void notifyAdjacentInventories() {
		if (!hasLevel()) return;
		for (byte tSide = 0; tSide < 6; tSide++) {
			if (tSide == SIDE_TOP || tSide == getFacing()) continue; // ALL_SIDES_BUT_TOP minus the own output face
			BlockEntity tNeighbor = getLevel().getBlockEntity(getBlockPos().relative(Direction.from3DDataValue(tSide)));
			if (tNeighbor instanceof GT6AdjacentInventoryUpdatable) {
				// the receiver side = the side of the NEIGHBOUR facing back at the caller
				((GT6AdjacentInventoryUpdatable) tNeighbor).adjacentInventoryUpdated(
						(byte) Direction.from3DDataValue(tSide).getOpposite().get3DDataValue(), this);
			}
		}
	}

	/** Upstream :252/:234 verbatim — an input-face change re-arms an idle hopper. */
	@Override
	public void adjacentInventoryUpdated(byte aSide, BlockEntity aSource) {
		if (aSide == SIDE_TOP || aSide == getFacing()) {
			if (mCheck < 0) mCheck = 0;
		}
	}

	// -----------------------------------------------------------------------
	// the redstone gate (upstream TileEntityBase01Root.java:568-573)
	// -----------------------------------------------------------------------

	/**
	 * The upstream hasRedstoneIncomingFromNonRail verbatim: any neighbour that is NOT a rail
	 * and feeds weak power in blocks the transfer round. {@code getIndirectPowerLevelTo}
	 * maps onto {@code Level.getSignal(pos, dir)}; the offline (level-less) fixture reads no
	 * redstone, keeping the gate open for the tests.
	 */
	public boolean hasRedstoneIncomingFromNonRail() {
		if (!hasLevel() || isClientSide()) return false;
		for (byte tSide = 0; tSide < 6; tSide++) {
			BlockPos tPos = getBlockPos().relative(Direction.from3DDataValue(tSide));
			if (getLevel().getBlockState(tPos).getBlock() instanceof net.minecraft.world.level.block.BaseRailBlock) continue;
			if (getLevel().getSignal(tPos, Direction.from3DDataValue(tSide)) > 0) return true;
		}
		return false;
	}

	// -----------------------------------------------------------------------
	// the world seams (level-walking defaults; the offline tests override)
	// -----------------------------------------------------------------------

	/**
	 * The container view of the block at the OWN OUTPUT FACE (upstream
	 * {@code getAdjacentTileEntity(mFacing)} + the rail interception :173-176): a tile entity
	 * with an item handler wins; on a rail block a container ENTITY inside the block space
	 * (a chest/hopper minecart — the IEntitySelector.selectInventories port) is wrapped
	 * instead; else null (the move arm idles this tick).
	 */
	@Nullable
	protected IItemHandler outputTarget() {
		return hasLevel() && !isClientSide()
				? handlerAt(getBlockPos().relative(Direction.from3DDataValue(getFacing())), Direction.from3DDataValue(getFacing()))
				: null;
	}

	/**
	 * The container view of the block ABOVE (upstream {@code getAdjacentTileEntity(SIDE_TOP)}
	 * + the rail interception :187-190): a tile entity yields its handler or the slot-less
	 * stand-in (a TE without an inventory REFUSES the drain but still suppresses suction,
	 * exactly the upstream {@code tDelegator.mTileEntity != null} arm); no tile entity → the
	 * rail entity wrap → else null (the suction arm).
	 */
	@Nullable
	protected IItemHandler topSource() {
		if (!hasLevel() || isClientSide()) return null;
		return handlerAt(getBlockPos().above(), Direction.UP);
	}

	/** The shared adjacency walk of the two seams above. */
	@Nullable
	protected final IItemHandler handlerAt(BlockPos aPos, Direction aOwnSide) {
		BlockEntity tTE = getLevel().getBlockEntity(aPos);
		if (tTE != null) {
			IItemHandler tHandler = itemHandlerOf(tTE, aOwnSide.getOpposite());
			return tHandler != null && tHandler.getSlots() > 0 ? tHandler : NO_SLOTS;
		}
		// upstream :174-175 / :161-162 — the rail block intercepts a container entity in its space
		if (getLevel().getBlockState(aPos).getBlock() instanceof net.minecraft.world.level.block.BaseRailBlock) {
			List<Entity> tList = getLevel().getEntities((Entity) null, new AABB(aPos), net.minecraft.world.entity.EntitySelector.CONTAINER_ENTITY_SELECTOR);
			if (!tList.isEmpty()) {
				return containerHandler((net.minecraft.world.Container) tList.get(0)); // :175/:162 — the first inventory entity
			}
		}
		return null;
	}

	/** The sided neighbour handler query (the pipe BE itemHandlerOf shape). */
	@Nullable
	static IItemHandler itemHandlerOf(BlockEntity aNeighbor, @Nullable Direction aTargetSide) {
		//? if forge {
		return aNeighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, aTargetSide).orElse(null);
		//?} else {
		/*return aNeighbor.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, aNeighbor.getBlockPos(), aTargetSide);
		 *///?}
	}

	/**
	 * The WD.suck(world, x, y+1, z) port (WD.java:93-105): the first live item entity in the
	 * block space above, removed and returned whole.
	 */
	@Nullable
	protected ItemStack suckTopItem() {
		if (!hasLevel() || isClientSide()) return null;
		BlockPos tAbove = getBlockPos().above();
		for (net.minecraft.world.entity.item.ItemEntity tItem : getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new AABB(tAbove), Entity::isAlive)) {
			ItemStack rStack = tItem.getItem();
			tItem.discard(); // WD.suck :99-101 — removeEntity + setDead
			return rStack;
		}
		return null;
	}

	/**
	 * The suction visibility gate — upstream {@code !WD.visOpq(world, x, y+1, z, F, T)}
	 * (WD.java:645-646: an unloaded chunk keeps the aDefault T = no suction; else the
	 * isOpaqueCube check): the 1.20.1 {@code isSolidRender} full-cube form.
	 */
	protected boolean topVisiblyOpaque() {
		if (!hasLevel()) return true;
		BlockPos tAbove = getBlockPos().above();
		if (!getLevel().isLoaded(tAbove)) return true; // the upstream aDefault=T arm
		return getLevel().getBlockState(tAbove).isSolidRender(getLevel(), tAbove);
	}

	// -----------------------------------------------------------------------
	// the external side view (the SideItemHandler posture — upstream
	// getAccessibleSlotsFromSide2/canInsertItem2/canExtractItem2 as the view's
	// slot mapping + insert/extract gates)
	// -----------------------------------------------------------------------

	/** The side view handed to every external consumer (GTItemMover, vanilla hopper push). */
	public IItemHandler sideView(byte aSide) {
		return new SideItemHandler(this, aSide);
	}

	/** The per-side handler view; the side is part of the identity (fresh per call). */
	public static final class SideItemHandler implements IItemHandler {

		private final GT6HopperBaseBlockEntity mTE;
		private final byte mSide;
		private final int[] mSlots;

		SideItemHandler(GT6HopperBaseBlockEntity aTE, byte aSide) {
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
			return mTE.stackLimit(); // the upstream getInventoryStackLimit :248-249/:230-231
		}

		@Override
		public boolean isItemValid(int aViewSlot, ItemStack aStack) {
			return mTE.canInsertItem(realSlot(aViewSlot), aStack, mSide);
		}

		private int realSlot(int aViewSlot) {
			return mSlots[aViewSlot];
		}
	}

	// -----------------------------------------------------------------------
	// capability (the fresh-per-call side view — the pipe BE posture)
	// -----------------------------------------------------------------------

	//? if forge {
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> aCapability, @Nullable Direction aSide) {
		if (aCapability == ForgeCapabilities.ITEM_HANDLER) {
			byte tSide = aSide == null ? SIDE_ANY : (byte) aSide.get3DDataValue();
			return LazyOptional.of(() -> new SideItemHandler(this, tSide)).cast();
		}
		return super.getCapability(aCapability, aSide);
	}
	//?} else {
	/*// (21.1 seam: NeoForge removed BlockEntity#getCapability — the W4 registerBlockEntity
	// delegates to this member; no @Override. Fresh per-call wrapper kept: the side is part
	// of the handler identity (the GTItemPipeBlockEntity seam shape).
	public <T> T getCapability(net.neoforged.neoforge.capabilities.BlockCapability<T, Direction> aCapability, @Nullable Direction aSide) {
		if (aCapability == net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK) {
			byte tSide = aSide == null ? SIDE_ANY : (byte) aSide.get3DDataValue();
			return (T) new SideItemHandler(this, tSide);
		}
		return null;
	}
	 *///?}

	// -----------------------------------------------------------------------
	// the tool surface (upstream onToolClick2 :126-153 / :119-140; the port's
	// dispatch: GT6ToolActions.SCREWDRIVER = the mode cycle, the wrench layer
	// (ToolActions.HOE_DIG) = the exact-mode toggle)
	// -----------------------------------------------------------------------

	/** Upstream TOOL_screwdriver :128-135 / :121-128 — the mode cycle, sneak reverses. */
	public void screwdriver(boolean aReverse, @Nullable Player aFeedback) {
		if (aReverse) {
			if (--mMode < modeMin()) mMode = modeMax();
		} else {
			if (++mMode > modeMax()) mMode = modeMin();
		}
		modeFeedback(aFeedback);
		setChanged();
		updateClientData();
	}

	/**
	 * Upstream TOOL_monkeywrench :137-141 — the exact/divisible toggle. Queue hoppers have
	 * no monkeywrench arm upstream (falls through :139), so the block dispatch gates on
	 * {@link #hasExactMode()}.
	 */
	public void monkeyWrench(@Nullable Player aFeedback) {
		mExactMode = !mExactMode;
		modeFeedback(aFeedback);
		setChanged();
		updateClientData();
	}

	/** The hopper-kind marker: only the regular hopper carries the exact axis (:63 vs :63-64). */
	public abstract boolean hasExactMode();

	/** The upstream aChatReturn strings (:134/:139/:145 / :127/:132/:136), verbatim. */
	public String modeChatLine() {
		if (hasExactMode()) {
			return mMode <= 0 ? (mExactMode ? "Emits up to 1 Stack" : "Emits up to 64 Items")
					: (mExactMode ? "Emits exact Stacksize of: " : "Emits divisible Stacksize of: ") + mMode;
		}
		return "Max Stacksize: " + mMode;
	}

	private void modeFeedback(@Nullable Player aPlayer) {
		if (aPlayer != null) {
			aPlayer.displayClientMessage(Component.literal(modeChatLine()), true);
		}
	}

	// -----------------------------------------------------------------------
	// helpers
	// -----------------------------------------------------------------------

	/**
	 * The inventory carrier with the mMode-capped slot limit — the lazy
	 * {@code getSlotLimit} read keeps the :586 insert cap and the vanilla insertItem room
	 * arithmetic on the current mode. Inner (non-static) so {@link #stackLimit()} dispatches.
	 */
	private final class HopperInventory extends GTItemStackHandler {
		private HopperInventory(int aSize, Runnable aOnChange) {
			super(aSize, aOnChange);
		}

		@Override
		public int getSlotLimit(int aSlot) {
			return stackLimit();
		}
	}

	/**
	 * The IItemHandler bridge over a container ENTITY (a chest/hopper minecart — the
	 * IEntitySelector.selectInventories interception target, upstream :175/:162). This is the
	 * entity-seam ADAPTER, not transport logic: every move still rides GTItemMover. The slot
	 * semantics follow the vanilla hopper's own Container handling
	 * (HopperBlockEntity.canPlaceItemInContainer :242-252 / tryMoveInItem :266-275):
	 * canPlaceItem + min(container max, stack max) room, extract via removeItem.
	 */
	public static IItemHandler containerHandler(net.minecraft.world.Container aContainer) {
		return new IItemHandler() {
			@Override
			public int getSlots() {
				return aContainer.getContainerSize();
			}

			@Override
			public ItemStack getStackInSlot(int aSlot) {
				return aContainer.getItem(aSlot);
			}

			@Override
			public ItemStack insertItem(int aSlot, ItemStack aStack, boolean aSimulate) {
				if (aStack.isEmpty()) return ItemStack.EMPTY;
				if (!aContainer.canPlaceItem(aSlot, aStack)) return aStack;
				int tLimit = Math.min(aContainer.getMaxStackSize(), aStack.getMaxStackSize());
				ItemStack tExisting = aContainer.getItem(aSlot);
				int tMoved;
				if (tExisting.isEmpty()) {
					tMoved = Math.min(aStack.getCount(), tLimit);
				} else if (stackablePair(tExisting, aStack)) {
					tMoved = Math.min(aStack.getCount(), tLimit - tExisting.getCount());
				} else {
					return aStack;
				}
				if (tMoved <= 0) return aStack;
				if (!aSimulate) {
					if (tExisting.isEmpty()) {
						aContainer.setItem(aSlot, copyAt(aStack, tMoved));
					} else {
						tExisting.grow(tMoved);
					}
				}
				return tMoved >= aStack.getCount() ? ItemStack.EMPTY : copyAt(aStack, aStack.getCount() - tMoved);
			}

			@Override
			public ItemStack extractItem(int aSlot, int aAmount, boolean aSimulate) {
				ItemStack tExisting = aContainer.getItem(aSlot);
				if (tExisting.isEmpty() || aAmount <= 0) return ItemStack.EMPTY;
				int tTake = Math.min(aAmount, tExisting.getCount());
				if (aSimulate) {
					return copyAt(tExisting, tTake);
				}
				return aContainer.removeItem(aSlot, tTake);
			}

			@Override
			public int getSlotLimit(int aSlot) {
				return aContainer.getMaxStackSize();
			}

			@Override
			public boolean isItemValid(int aSlot, ItemStack aStack) {
				return aContainer.canPlaceItem(aSlot, aStack);
			}
		};
	}
}
