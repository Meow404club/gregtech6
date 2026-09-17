package gregtech6.tileentity.connectors;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

//? if forge {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
//?} else {
/*import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
 *///?}

import gregapi.code.TagData;
import gregapi.data.TD;

import gregtech6.covers.CoverData;
import gregtech6.covers.ICoverableTE;
import gregtech6.registry.GTItemPipes;
import gregtech6.tileentity.GTItemStackHandler;
import gregtech6.util.GTItemMover;
import gregtech6.util.UT6;

/**
 * 1.20.1 counterpart of gregapi/tileentity/connectors/MultiTileEntityPipeItem.java
 * (292 lines) — the trimmed direct translation (task p26-pipe-item spec ①-④).
 *
 * <p>There is NO network object — the SOURCE pipe runs the whole transport: every 10
 * ticks (upstream SERVER_TICK_PR2 gate SERVER_TIME % 10 == 0, :194) a pipe holding
 * items scans its pipe network ({@link #scanPipes}, the ITileEntityItemPipe.Util
 * :77-101 port) in ascending step-distance order and pushes its own inventory slots
 * into the inventories adjacent to the scanned pipes ({@link #insertItemStackIntoTileEntity}
 * :224-241, the actual move through {@link GTItemMover#move(IItemHandler, IItemHandler)}
 * — the ST.move port, the card's consumption-not-rewrite ruling). The per-pipe
 * {@code mTransferredItems} counter with its invSize-per-window budget (:243-251, the
 * window reset every 20 ticks :193) rates the flow; the counters increment along the
 * scanned path prefix (:206).
 *
 * <p>Port scope (each clause anchored to the upstream line):
 * <ul>
 * <li>the invSize-slot inventory (the registration NBT_INV_SIZE :76-83/:94; the Brass
 *     family = 1/2/4 slots over the variant table), a {@link GTItemStackHandler} bound
 *     to {@code setChanged()};</li>
 * <li>the ONE-WAY LATCH (:268 canInsertItem2): while the pipe holds items, inserts pass
 *     only through the latched side AND only into an EMPTY slot; an empty inventory
 *     re-arms the latch to the inserting side;</li>
 * <li>the emit gate (:272 canEmitItemsTo): a pipe never emits back out the side it is
 *     latched to (the anti-ping-pong arm), connected faces only;</li>
 * <li>the monkeywrench face-disable masks (:69/:124-153): two 6-bit bytes
 *     (inputs/outputs), cycled per face by {@link #monkeyWrench(byte)} through the
 *     four-state cycle normal → emit-off → accept-off → both-off (the verbatim
 *     :134-148 arm structure; refused between two item pipes, :130-133);</li>
 * <li>the routing acceptance (:275 canConnect): every item-handler neighbour with slots
 *     (TD.Connectors.PNEUMATIC_ITEM, :286); never another connector as a transfer
 *     TARGET (:227);</li>
 * <li>the hopper/dispenser facing speciality (:228): no push into a hopper/dispenser
 *     that faces back INTO the pipe (the metadata side test → the FACING BlockState).</li>
 * </ul>
 *
 * <p>Folds (declared): the upstream SERVER_TICK_PRE (window reset, :192-193) /
 * SERVER_TICK_PR2 (transfer, :194-220) global lists plus the mHasToAddTimer
 * registration machinery (:97-101/:168-188) become the block-ticker phase gate
 * {@code (timer + offset) % period} — the GTFluidPipeBlockEntity DISTRIBUTION_PERIOD
 * +offset precedent; the offset keeps exactly two transfer rounds inside every capacity
 * window (the intra-tick ordering between different pipes' reset and transfer rounds is
 * the declared ≤1-round skew of the fold). The ITileEntityAdjacentInventoryUpdatable
 * fan-out (:210-215/:254-261) has no port-side consumer (the 1.7.10 machine re-plan
 * hint) and is trimmed.
 *
 * <p>Covers (task p31-retriever-cover ①): the BE implements {@link ICoverableTE} by
 * composition (the TileEntityOven precedent) — the {@link #mCovers} store, the
 * 06Covers :68/:74 NBT round trip, the :191 validity sweep on the first tick, the
 * :200/:202 tickPre/tickPost dispatch and the :184-186 visual-sync window; the
 * retriever cover is the first consumer (upstream CoverRetrieverItem places only on a
 * ticking item pipe, CoverRetrieverItem.java:50).
 */
public class GTItemPipeBlockEntity extends TileEntityBase09Connector implements ICoverableTE {

	/** Upstream :194 — the transfer round period (SERVER_TIME % 10). */
	public static final int TRANSFER_PERIOD = 10;

	/** Upstream :193 — the capacity-window reset period (SERVER_TIME % 20 = the "/s" budget, :117). */
	public static final int CAPACITY_WINDOW = 20;

	/** Upstream :69 SIDE_UNDEFINED — the latch value of a pipe that holds nothing. */
	public static final byte SIDE_UNDEFINED = -1;

	// NBT — the upstream keys (:88-95, "gt.mlast"/"gt.olast"/NBT_INPUT/NBT_OUTPUT/"gt.mtransfer")
	// in the in-repo plain-key form (the fluid pipe "last."/"ioMask" precedent).
	public static final String NBT_MLAST = "mlast";
	public static final String NBT_OLAST = "olast";
	public static final String NBT_INPUT = "inputs";
	public static final String NBT_OUTPUT = "outputs";
	public static final String NBT_TRANSFERRED = "transferred";
	public static final String NBT_INVENTORY = "inventory";

	/** Upstream :68 — the window transfer counter (the rate budget). */
	public long mTransferredItems = 0;

	/** Upstream :69 — the one-way latch: the side the current items came from. */
	public byte mLastReceivedFrom = SIDE_UNDEFINED;

	/** Upstream :69 — the previous round's latch (the transfer gate reads its stability, :195). */
	public byte oLastReceivedFrom = SIDE_UNDEFINED;

	/** Upstream :69 — the per-face input/output disable masks (the monkeywrench layer). */
	public byte mDisabledInputs = 0, mDisabledOutputs = 0;

	/** Upstream :70 (NBT_OPAQUE) — every loader line passes aBlocking = T (:1823-1843). */
	public boolean mBlocking = true;

	/** Upstream :94 — the stepSize axis (the routing distance weight, 32768 base; the variant table :77-82). */
	public long mStepSize = 32768;

	/** The inventory (invSize slots — the registration NBT_INV_SIZE axis). Non-final: the offline latch tests re-shape it. */
	public GTItemStackHandler mInventory;

	/** The random phase offset within {@link #TRANSFER_PERIOD} (assigned on the first server tick). */
	private int mPhaseOffset = 0;
	private boolean mPhaseAssigned = false;

	// ---------------------------------------------------------------------------
	// covers (task p31-retriever-cover ① — the composition attachment, the Oven
	// precedent: the store lives here, the 06Covers behaviour comes from the
	// ICoverableTE defaults; the base-class chain stays untouched)
	// ---------------------------------------------------------------------------

	/** Upstream 06Covers :63 mCovers — {@code null} while no face carries a cover. */
	public CoverData mCovers = null;

	@Override
	public CoverData getCovers() {
		return mCovers;
	}

	@Override
	public void setCovers(CoverData aCoverData) {
		mCovers = aCoverData;
	}

	/** BET factory for BlockEntityType.Builder.of — resolves the shared type through the registry at runtime. */
	public GTItemPipeBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/**
	 * Full constructor — also the offline (test) entry point: a null type falls back to
	 * the shared registry type at runtime, tests pass an offline-built BET over vanilla
	 * blocks (which fall back to the loader base axis 32768/1, :1823). stepSize/invSize
	 * come from the block row (the registration NBT carriers).
	 */
	public GTItemPipeBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(true, aType != null ? aType : GTItemPipes.ITEM_PIPE_BE.get(), aPos, aState);
		long tStepSize = 32768;
		int tInvSize = 1;
		if (aState.getBlock() instanceof gregtech6.block.pipe.GTItemPipeBlock tBlock) {
			tStepSize = tBlock.row().stepSize();
			tInvSize = tBlock.row().invSize();
		}
		mStepSize = tStepSize;
		mInventory = new GTItemStackHandler(tInvSize, this::setChanged);
	}

	@Override
	public String getTileEntityName() {
		return "item_pipe"; // BET registry path mirrors it (upstream :291 "gt.multitileentity.connector.pipe.item")
	}

	// ---------------------------------------------------------------------------
	// tick (upstream onServerTickPre :191-221 — the two-phase fold, see class doc)
	// ---------------------------------------------------------------------------

	@Override
	public void onTickFirst(boolean aIsServerSide) {
		if (aIsServerSide) {
			// upstream 06Covers :191 — the validity sweep rides onTickFirst before the pipe business
			checkCoverValidity();
			// the level-less offline fixtures take phase 0 (the rng seam returns 0 there) —
			// deterministic ticking for the offline tests
			mPhaseOffset = hasLevel() ? getLevel().random.nextInt(TRANSFER_PERIOD) : 0;
			mPhaseAssigned = true;
		}
	}

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		// upstream 06Covers :200 — the cover tick precedes the pipe business (the Oven shape;
		// the pipe carries no mInventoryChanged writer, the inventory handler marks setChanged)
		if (hasCovers()) getCovers().tickPre(aTimer, aIsServerSide, mBlockUpdated, false);
		if (!aIsServerSide || !mPhaseAssigned) {
			if (hasCovers()) getCovers().tickPost(aTimer, aIsServerSide, mBlockUpdated, false);
			return;
		}
		long tPhase = aTimer + mPhaseOffset;
		if (tPhase % CAPACITY_WINDOW == 0) {
			mTransferredItems = 0; // upstream :193 (the PRE-list half)
		}
		if (tPhase % TRANSFER_PERIOD == 0) {
			transferRound(); // upstream :195-216 (the PR2-list half)
			// upstream :218-219 — the latch releases with the last item, the echo follows
			if (!inventoryHasSomething()) mLastReceivedFrom = SIDE_UNDEFINED;
			oLastReceivedFrom = mLastReceivedFrom;
		}
		// upstream 06Covers :202 — the cover tick follows the pipe business
		if (hasCovers()) getCovers().tickPost(aTimer, aIsServerSide, mBlockUpdated, false);
	}

	/**
	 * Upstream :195-216 verbatim. The transfer runs only when the latch is STABLE across
	 * two rounds ({@code oLastReceivedFrom == mLastReceivedFrom}, :195 — a fresh fill
	 * waits one round). Outer loop: repeat whole-network passes while this pipe still
	 * holds items and its budget allows; inner: the scanned pipes in ascending distance,
	 * each draining {@link #sendItemStack} repeatedly, every pipe of the accumulated path
	 * prefix charged per item (:206 — the travel-distance accounting).
	 */
	public void transferRound() {
		if (oLastReceivedFrom != mLastReceivedFrom) return;
		if (!hasLevel()) return; // the send chain walks live neighbours
		boolean tUpdate = false;
		List<GTItemPipeBlockEntity> tPipeList = new ArrayList<>();
		for (boolean temp = true; temp && inventoryHasSomething() && pipeCapacityCheck(); ) {
			temp = false;
			tPipeList.clear();
			for (GTItemPipeBlockEntity tTileEntity : pipesAscending(scanPipes(this, new LinkedHashMap<>(), 0, false))) {
				if (temp) break;
				tPipeList.add(tTileEntity);
				while (!temp && inventoryHasSomething() && tTileEntity.sendItemStack(this)) {
					tUpdate = true;
					for (GTItemPipeBlockEntity tPipe : tPipeList) {
						if (!tPipe.incrementTransferCounter(1)) temp = true;
					}
				}
			}
		}
		if (tUpdate) setChanged();
	}

	// ---------------------------------------------------------------------------
	// the send chain (:224-251)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream :224-241. The output face gates (:225 disabled outputs + :272
	 * canEmitItemsTo), the target must hold an item handler and NOT be another connector
	 * (:227), the hopper/dispenser facing speciality (:228), then the move —
	 * {@link GTItemMover#move} over the source pipe's full inventory view into the
	 * target's side view (upstream :236 {@code ST.move(Delegator(aSender, SIDE_ANY), tDelegator)}).
	 *
	 * @return true when anything moved
	 */
	public boolean insertItemStackIntoTileEntity(Object aSender, byte aSide) {
		if (aSide < 0 || aSide >= 6) return false;
		if ((mDisabledOutputs & SBIT[aSide]) != 0) return false; // :225 FACE_CONNECTED[aSide][mDisabledOutputs]
		if (!canEmitItemsTo(aSide, aSender)) return false;       // :225
		Direction tDirection = Direction.from3DDataValue(aSide);
		Direction tOpposite = tDirection.getOpposite();
		BlockEntity tNeighbor = getLevel().getBlockEntity(getBlockPos().relative(tDirection));
		if (tNeighbor == null || tNeighbor instanceof TileEntityBase09Connector) return false; // :227
		IItemHandler tHandler = itemHandlerOf(tNeighbor, tOpposite);
		if (tHandler == null || tHandler.getSlots() <= 0) return false; // :227 ST.canConnect
		// :228 — don't push into a hopper/dispenser facing back into the pipe (the 1.7.10
		// metadata-side test becomes the FACING BlockState read; the facing numeral order is
		// the same 0=down,2..5=N/S/W/E vanilla continuity)
		if (tNeighbor.getBlockState().getBlock() instanceof net.minecraft.world.level.block.HopperBlock
				|| tNeighbor.getBlockState().getBlock() instanceof net.minecraft.world.level.block.DispenserBlock) {
			if (facingOf(tNeighbor) == tOpposite) return false;
		}
		if (!(aSender instanceof GTItemPipeBlockEntity tSource)) return false; // the :236 (TileEntity)aSender cast
		return GTItemMover.move(tSource.mInventory, tHandler) > 0; // :236
	}

	/**
	 * Upstream :244 verbatim — the random starting face ({@code rng(6)}) then all six:
	 * the "distance ascending pipes, random faces" transfer shape.
	 */
	public boolean sendItemStack(Object aSender) {
		if (pipeCapacityCheck()) {
			for (byte i = 0, j = (byte)rng(6); i < 6; i++) {
				if (insertItemStackIntoTileEntity(aSender, (byte)((i + j) % 6))) return true;
			}
		}
		return false;
	}

	// ---------------------------------------------------------------------------
	// capacity (:243-251) — the invSize-per-window budget
	// ---------------------------------------------------------------------------

	/** Upstream :243. */
	public boolean incrementTransferCounter(long aIncrement) {
		mTransferredItems += aIncrement;
		return pipeCapacityCheck();
	}

	/** Upstream :246 — the first transfer of a window always passes ({@code <= 0} arm). */
	public boolean pipeCapacityCheck() {
		return mTransferredItems <= 0 || getPipeContent() < getMaxPipeCapacity();
	}

	/** Upstream :247 — the routing weight (the stepSize axis, variant table :77-82). */
	public long getStepSize() {
		return mStepSize;
	}

	/** Upstream :249. */
	protected long getPipeContent() {
		return mTransferredItems;
	}

	/** Upstream :250 — Math.max(1, capacity). */
	protected long getMaxPipeCapacity() {
		return Math.max(1, getPipeCapacity());
	}

	/** Upstream :251 — the capacity IS the inventory size (items per 20-tick window, the "/s" tooltip :117). */
	protected long getPipeCapacity() {
		return mInventory.getSlots();
	}

	// ---------------------------------------------------------------------------
	// the one-way latch (:268-269) — the external item-handler face gates
	// ---------------------------------------------------------------------------

	/**
	 * Upstream :268 canInsertItem2 verbatim. Unconnected or input-disabled faces refuse;
	 * an empty inventory re-arms the latch to the inserting side; then the insert passes
	 * only through the LATCHED side and only into an EMPTY slot — while the pipe holds
	 * items nobody inserts (the {@code !slotHas(aSlot)} arm), and once it drains the next
	 * filler takes the latch. The latched side is the recorded {@code mLastReceivedFrom}
	 * even after the pipe drains mid-round (the :218 release only runs on the tick gate).
	 */
	public boolean canInsertItem(int aSlot, ItemStack aStack, byte aSide) {
		if (aSide < 0 || aSide >= 6) return false;
		if (!connected(aSide) || (mDisabledInputs & SBIT[aSide]) != 0) return false;
		if (!inventoryHasSomething()) mLastReceivedFrom = aSide;
		return mLastReceivedFrom == aSide && !slotHas(aSlot);
	}

	/** Upstream :269 canExtractItem2 — invalid (side-less) queries and connected faces extract. */
	public boolean canExtractItem(int aSlot, ItemStack aStack, byte aSide) {
		return aSide < 0 || connected(aSide);
	}

	// ---------------------------------------------------------------------------
	// routing gates (:272-273) and the connection acceptance (:275)
	// ---------------------------------------------------------------------------

	/** Upstream :272 — never emit back out the latched side (the aSender == this arm), connected faces only. */
	public boolean canEmitItemsTo(byte aSide, Object aSender) {
		return (aSender != this || aSide != mLastReceivedFrom) && connected(aSide);
	}

	/** Upstream :273. */
	public boolean canAcceptItemsFrom(byte aSide, Object aSender) {
		return connected(aSide);
	}

	/**
	 * Upstream :275 — the connect handshake acceptance for non-connector neighbours: any
	 * item handler with slots (the ISidedInventory/ST.canConnect disjunction folds into
	 * the single handler-world query). Connector neighbours never reach this (the base
	 * {@code connect} tag-intersection branch, TileEntityBase09Connector.java:112-119).
	 */
	@Override
	public boolean canConnect(byte aSide, @Nullable BlockEntity aNeighbor) {
		if (aNeighbor == null) return false;
		IItemHandler tHandler = itemHandlerOf(aNeighbor, Direction.from3DDataValue(aSide).getOpposite());
		return tHandler != null && tHandler.getSlots() > 0;
	}

	@Override
	public List<TagData> getConnectorTypes(byte aSide) {
		return TD.Connectors.PNEUMATIC_ITEM.AS_LIST; // upstream :286
	}

	/** The mask becomes the CONNECTS BlockState — the GTFluidPipeBlockEntity.onConnectionChange shape. */
	@Override
	public void onConnectionChange(byte aPreviousConnections) {
		super.onConnectionChange(aPreviousConnections);
		if (hasLevel()) {
			BlockState tState = getBlockState();
			if (tState.hasProperty(gregtech6.block.pipe.GTItemPipeBlock.CONNECTIONS)
					&& tState.getValue(gregtech6.block.pipe.GTItemPipeBlock.CONNECTIONS) != (int)getConnections()) {
				getLevel().setBlock(getBlockPos(), tState.setValue(gregtech6.block.pipe.GTItemPipeBlock.CONNECTIONS, (int)getConnections()),
						Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
			}
		}
	}

	/** Upstream onPlaced (TileEntityBase09Connector.java:82-96) — the clicked-face support connect + the back-connect loop. */
	public void onPlaced(byte aSide) {
		if (aSide < 0 || aSide >= 6 || !hasLevel() || !isServerSide()) return;
		connect(UT6.OPOS[aSide], true); // upstream :84/:87
		for (byte tSide = 0; tSide < 6; tSide++) { // upstream :88-93
			BlockEntity tNeighbor = getLevel().getBlockEntity(getBlockPos().relative(Direction.from3DDataValue(tSide)));
			if (tNeighbor instanceof TileEntityBase09Connector tConnector) {
				byte tOpposite = (byte)Direction.from3DDataValue(tSide).getOpposite().get3DDataValue();
				if (tConnector.connected(tOpposite)
						&& haveOneCommonElement(tConnector.getConnectorTypes(tOpposite), getConnectorTypes(tSide))) {
					connect(tSide, true); // upstream :91
				}
			}
		}
	}

	/**
	 * The wrench connection toggle (the GTFluidPipeBlockEntity.toggleConnection shape minus
	 * the ownership stratum — the item pipe family carries no foam/ownable layer): connected
	 * → disconnect, else connect. The plain hoe right-click face (upstream getFacingTool
	 * TOOL_wrench, MultiTileEntityPipeItem.java:288 + 09Connector.onToolClick2 :70-79).
	 */
	public boolean toggleConnection(byte aSide) {
		if (aSide < 0 || aSide >= 6) return false;
		if (connected(aSide)) return disconnect(aSide, true);
		return connect(aSide, true);
	}

	// ---------------------------------------------------------------------------
	// scanPipes (ITileEntityItemPipe.Util :77-101)
	// ---------------------------------------------------------------------------

	/**
	 * The network scan, ported as an ITERATIVE minimum-step relaxation (a priority-queue
	 * Dijkstra — the upstream TODO "Make this iterative instead of recursive" :79; the
	 * min-distance-per-pipe result is identical to the recursive form over the
	 * non-negative step axis, which the variant table :77-82 guarantees). The
	 * {@code aSuckItems} reverse-scanning arm of the upstream signature (:83-89) has no
	 * port-side caller (the only upstream call site passes F, :201) and is trimmed — the
	 * GTItemMover trimmed-surface precedent.
	 *
	 * <p>Gates, verbatim: the visited pipe's own capacity check unless aIgnoreCapacity
	 * (:80 — a pipe at capacity is neither a target nor routed through), the emitting
	 * face ({@code canEmitItemsTo(side, null)} = connected, :91), the neighbour must be
	 * an item pipe with an intersecting connector type accepting on its back face (:93).
	 * The face-disable masks are NOT consulted here — routing ignores them, the transfer
	 * enforces them (:225/:268, upstream-consistent). Ties keep insertion order (the
	 * deterministic counterpart of the upstream hash order).
	 *
	 * @return the map pipe -> accumulated stepSize (min over the paths), insertion-ordered
	 */
	public static Map<GTItemPipeBlockEntity, Long> scanPipes(GTItemPipeBlockEntity aPipe, Map<GTItemPipeBlockEntity, Long> aMap, long aStep, boolean aIgnoreCapacity) {
		final Map<GTItemPipeBlockEntity, Long> tMap;
		if (aMap instanceof LinkedHashMap) {
			tMap = aMap; // the insertion-ordered result the transfer loop walks
		} else {
			tMap = new LinkedHashMap<>(aMap);
		}
		record Entry(GTItemPipeBlockEntity pipe, long step, long order) {}
		PriorityQueue<Entry> tQueue = new PriorityQueue<>(Comparator.comparingLong(Entry::step).thenComparingLong(Entry::order));
		Map<GTItemPipeBlockEntity, Long> tBest = new HashMap<>();
		long tOrder = 0;
		tBest.put(aPipe, aStep + aPipe.getStepSize()); // upstream :78 — the callee's aStep accumulation
		tQueue.add(new Entry(aPipe, aStep + aPipe.getStepSize(), tOrder++));
		while (!tQueue.isEmpty()) {
			Entry tEntry = tQueue.poll();
			GTItemPipeBlockEntity tPipe = tEntry.pipe();
			if (tBest.get(tPipe).longValue() != tEntry.step()) continue; // a stale frontier entry
			if (aIgnoreCapacity || tPipe.pipeCapacityCheck()) { // upstream :80
				Long tPrevious = tMap.get(tPipe);
				if (tPrevious == null || tPrevious > tEntry.step()) {
					tMap.put(tPipe, tEntry.step()); // upstream :81 — the min-distance memo
					for (byte tSide = 0; tSide < 6; tSide++) { // upstream :82
						if (!tPipe.canEmitItemsTo(tSide, null)) continue; // upstream :91 (aSender null → connected)
						byte tOpposite = (byte)Direction.from3DDataValue(tSide).getOpposite().get3DDataValue();
						GTItemPipeBlockEntity tNext = tPipe.adjacentItemPipe(tSide); // upstream :92 the Delegator walk
						if (tNext == null) continue; // upstream :93 instanceof arm
						if (!haveOneCommonElement(tPipe.getConnectorTypes(tSide), tNext.getConnectorTypes(tOpposite))) continue; // :93
						if (!tNext.canAcceptItemsFrom(tOpposite, null)) continue; // :93
						long tNextStep = tEntry.step() + tNext.getStepSize(); // upstream :78 on the callee
						Long tNextBest = tBest.get(tNext);
						if (tNextBest == null || tNextBest > tNextStep) {
							tBest.put(tNext, tNextStep);
							tQueue.add(new Entry(tNext, tNextStep, tOrder++));
						}
					}
				}
			}
		}
		return tMap;
	}

	/**
	 * Upstream :201 {@code UT.Code.sortByValuesAcending(...).keySet()} — the scan map as
	 * pipes in ASCENDING distance order, ties in insertion order.
	 */
	public static List<GTItemPipeBlockEntity> pipesAscending(Map<GTItemPipeBlockEntity, Long> aMap) {
		List<Map.Entry<GTItemPipeBlockEntity, Long>> tEntries = new ArrayList<>(aMap.entrySet());
		tEntries.sort(Map.Entry.comparingByValue());
		List<GTItemPipeBlockEntity> rPipes = new ArrayList<>(tEntries.size());
		for (Map.Entry<GTItemPipeBlockEntity, Long> tEntry : tEntries) rPipes.add(tEntry.getKey());
		return rPipes;
	}

	/**
	 * The scan adjacency face (upstream :92, the {@code getAdjacentTileEntity} walk folded
	 * to its pipe-type test): the item pipe sitting on aSide, null otherwise. Protected —
	 * the offline tests override it to wire synthetic networks (the rng seam precedent).
	 */
	@Nullable
	protected GTItemPipeBlockEntity adjacentItemPipe(byte aSide) {
		if (!hasLevel()) return null;
		BlockEntity tNeighbor = getLevel().getBlockEntity(getBlockPos().relative(Direction.from3DDataValue(aSide)));
		return tNeighbor instanceof GTItemPipeBlockEntity tPipe ? tPipe : null;
	}

	/**
	 * The adjacent-inventory walk of the retriever cover (task p31-retriever-cover; the
	 * upstream CoverRetrieverItem :67 {@code getAdjacentTileEntity} / :71
	 * {@code getAdjacentInventory} pair folded to one query): the item handler of the BE
	 * sitting on aSide of aPipe — never a connector (the :72 non-pipe arm) and only when
	 * the neighbour exposes slots. Null when the face has no pull source/target. Public
	 * (the upstream getAdjacentInventory surface — the retriever cover is the consumer);
	 * the offline tests override it to wire synthetic containers (the
	 * {@link #adjacentItemPipe} seam precedent).
	 */
	@Nullable
	public IItemHandler adjacentInventoryOf(GTItemPipeBlockEntity aPipe, byte aSide) {
		if (!aPipe.hasLevel()) return null;
		BlockEntity tNeighbor = aPipe.getLevel().getBlockEntity(aPipe.getBlockPos().relative(Direction.from3DDataValue(aSide)));
		if (tNeighbor == null || tNeighbor instanceof TileEntityBase09Connector) return null;
		IItemHandler tHandler = itemHandlerOf(tNeighbor, Direction.from3DDataValue(aSide).getOpposite());
		return tHandler != null && tHandler.getSlots() > 0 ? tHandler : null;
	}

	// ---------------------------------------------------------------------------
	// the monkeywrench face-disable cycle (:128-153)
	// ---------------------------------------------------------------------------

	/**
	 * Upstream onToolClick2 TOOL_monkeywrench (:128-153). Refused between two item pipes
	 * (:130-133 — the neighbour on the TARGET side is a pipe, the tool does nothing); the
	 * cycle is the upstream four-arm structure PORTED VERBATIM. The effective truth table
	 * is the FULL four-state cycle — each click advances one step:
	 * (0,0) -> (0,1) emit-off -> (1,0) accept-off -> (1,1) both-off -> (0,0) normal
	 * (outer arm inputs-clear + outputs-set flips BOTH; every other arm flips the OUTPUT
	 * mask only).
	 *
	 * <p>The upstream 2500 tool-durability return (:153) has no port-side tool-damage
	 * consumer (the hoe-substitute interaction family); the cycle itself is the payload.
	 *
	 * @return true when the cycle mutated the masks (the chat feedback upstream :149-152
	 *         is the /gt6itempipe wrench + stat face in the port)
	 */
	public boolean monkeyWrench(byte aTargetSide) {
		if (aTargetSide < 0 || aTargetSide >= 6) return false;
		if (hasLevel()) {
			BlockEntity tNeighbor = getLevel().getBlockEntity(getBlockPos().relative(Direction.from3DDataValue(aTargetSide)));
			if (tNeighbor instanceof GTItemPipeBlockEntity) return false; // :130-133 — "Will not work between two Item Pipes!"
		}
		// upstream :134-148 verbatim — the duplicated arms are the upstream text
		if ((mDisabledInputs & SBIT[aTargetSide]) != 0) { // FACE_CONNECTED[aTargetSide][mDisabledInputs]
			if ((mDisabledOutputs & SBIT[aTargetSide]) != 0) { // FACE_CONNECTED[aTargetSide][mDisabledOutputs]
				mDisabledInputs ^= SBIT[aTargetSide];
				mDisabledOutputs ^= SBIT[aTargetSide];
			} else {
				mDisabledOutputs ^= SBIT[aTargetSide];
			}
		} else {
			if ((mDisabledOutputs & SBIT[aTargetSide]) != 0) {
				mDisabledInputs ^= SBIT[aTargetSide];
				mDisabledOutputs ^= SBIT[aTargetSide];
			} else {
				mDisabledOutputs ^= SBIT[aTargetSide];
			}
		}
		setChanged();
		updateClientData();
		return true;
	}

	// ---------------------------------------------------------------------------
	// inventory helpers
	// ---------------------------------------------------------------------------

	/** Upstream {@code UT.Code.containsSomething(getInventory())} — any non-empty slot. */
	public boolean inventoryHasSomething() {
		for (int i = 0; i < mInventory.getSlots(); i++) {
			if (!mInventory.getStackInSlot(i).isEmpty()) return true;
		}
		return false;
	}

	/** Upstream :268 slotHas — the slot holds anything; out-of-range slots count as EMPTY-slot-capable never (gate refuses). */
	private boolean slotHas(int aSlot) {
		return aSlot >= 0 && aSlot < mInventory.getSlots() && !mInventory.getStackInSlot(aSlot).isEmpty();
	}

	/** The uniform random roll (the GTFluidPipeBlockEntity.rng seam — the sendItemStack random face). */
	protected int rng(int aBound) {
		return hasLevel() ? getLevel().random.nextInt(aBound) : 0;
	}

	/** The sided item-handler query (aTargetSide = the side of the NEIGHBOUR that faces us). */
	@Nullable
	public static IItemHandler itemHandlerOf(BlockEntity aNeighbor, Direction aTargetSide) {
		//? if forge {
		return aNeighbor.getCapability(ForgeCapabilities.ITEM_HANDLER, aTargetSide).orElse(null);
		//?} else {
		/*return aNeighbor.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, aNeighbor.getBlockPos(), aTargetSide);
		 *///?}
	}

	@Nullable
	private static Direction facingOf(BlockEntity aNeighbor) {
		BlockState tState = aNeighbor.getBlockState();
		return tState.hasProperty(BlockStateProperties.FACING)
				? tState.getValue(BlockStateProperties.FACING)
				: null;
	}

	// ---------------------------------------------------------------------------
	// capability (the side wrapper — the SideFluidHandler posture, item face)
	// ---------------------------------------------------------------------------

	//? if forge {
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> aCapability, @Nullable Direction aSide) {
		if (aCapability == ForgeCapabilities.ITEM_HANDLER) {
			// fresh per-call wrapper: the side is part of the handler identity
			return LazyOptional.of(() -> new SideItemHandler(this, aSide)).cast();
		}
		return super.getCapability(aCapability, aSide);
	}
	//?} else {
	/*// (1.21.1 seam: NeoForge 21.1 removed BlockEntity#getCapability — the W4
	// RegisterCapabilitiesEvent.registerBlockEntity delegates to this member; no @Override.
	// Fresh per-call wrapper kept: the side is part of the handler identity.
	public <T> T getCapability(BlockCapability<T, Direction> aCapability, @Nullable Direction aSide) {
		if (aCapability == Capabilities.ItemHandler.BLOCK) {
			return (T) new SideItemHandler(this, aSide);
		}
		return null;
	}
	 *///?}

	// ---------------------------------------------------------------------------
	// NBT (upstream :86-95/:105-112 + the inventory carrier)
	// ---------------------------------------------------------------------------

	@Override
	public boolean onTickCheck(long aTimer) {
		// upstream 06Covers :184-186 — the cover visual sync (the Oven shape) over the connector mask gate
		return (hasCovers() && getCovers().requiresSync()) || super.onTickCheck(aTimer);
	}

	@Override
	public void onTickChecked(long aTimer) {
		super.onTickChecked(aTimer);
		// upstream 06Covers :178-181 — the visual sync flags reset after the sync window
		if (hasCovers()) getCovers().resetSync();
	}

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putByte(NBT_MLAST, mLastReceivedFrom);
		aNBT.putByte(NBT_OLAST, oLastReceivedFrom);
		aNBT.putByte(NBT_INPUT, mDisabledInputs);
		aNBT.putByte(NBT_OUTPUT, mDisabledOutputs);
		aNBT.putLong(NBT_TRANSFERRED, mTransferredItems);
		aNBT.put(NBT_INVENTORY, serializeInventory());
		writeCoversToNBT(aNBT); // upstream 06Covers :74
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_MLAST, Tag.TAG_ANY_NUMERIC)) mLastReceivedFrom = aNBT.getByte(NBT_MLAST);
		if (aNBT.contains(NBT_OLAST, Tag.TAG_ANY_NUMERIC)) oLastReceivedFrom = aNBT.getByte(NBT_OLAST);
		if (aNBT.contains(NBT_INPUT, Tag.TAG_ANY_NUMERIC)) mDisabledInputs = aNBT.getByte(NBT_INPUT);
		if (aNBT.contains(NBT_OUTPUT, Tag.TAG_ANY_NUMERIC)) mDisabledOutputs = aNBT.getByte(NBT_OUTPUT);
		if (aNBT.contains(NBT_TRANSFERRED, Tag.TAG_ANY_NUMERIC)) mTransferredItems = aNBT.getLong(NBT_TRANSFERRED);
		if (aNBT.contains(NBT_INVENTORY, Tag.TAG_COMPOUND)) {
			deserializeInventory(aNBT.getCompound(NBT_INVENTORY));
		}
		readCoversFromNBT(aNBT); // upstream 06Covers :68
	}

	/** The ItemStackHandler NBT face (21.1 takes the registry provider — the 03 NBT_ACCESS seam). */
	private CompoundTag serializeInventory() {
		//? if forge {
		return mInventory.serializeNBT();
		//?} else {
		/*return mInventory.serializeNBT(gregtech6.tileentity.TileEntityBase03TicksAndSync.NBT_ACCESS); // 21.1: the registries ride along
		 *///?}
	}

	private void deserializeInventory(CompoundTag aTag) {
		//? if forge {
		mInventory.deserializeNBT(aTag);
		//?} else {
		/*mInventory.deserializeNBT(gregtech6.tileentity.TileEntityBase03TicksAndSync.NBT_ACCESS, aTag); // 21.1: provider-first
		 *///?}
	}
}
