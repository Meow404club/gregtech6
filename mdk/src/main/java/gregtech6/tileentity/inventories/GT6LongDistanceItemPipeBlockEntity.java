package gregtech6.tileentity.inventories;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

//? if forge {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
//?}
import net.minecraftforge.items.IItemHandler;

import gregtech6.block.energy.GT6ElectricTransformerBlock;
import gregtech6.block.tools.GT6LongDistPipeBlock;
import gregtech6.registry.GT6LongDistPipes;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import gregtech6.tileentity.connectors.GTItemPipeBlockEntity;

/**
 * The Long Distance Item Pipeline Endpoint (task p35-long-distance-pipes) — the 1.20.1
 * transcription of {@code MultiTileEntityLongDistancePipelineItem} (:53-306, the
 * TileEntityBase09FacingSingle shape over the port BE tree; Loader_MultiTileEntities
 * :906, meta id 10060). Two endpoints joined by a blob of {@link GT6LongDistPipeBlock}
 * push items ACROSS the blob in ONE hop with NO transport tick: the endpoint IS a
 * window into the TARGET's BACK-adjacent inventory — every slot read, insert and
 * extract delegates straight to {@code mTarget.getAdjacentInventory(OPOS[mTarget.mFacing])}
 * (:193-285), so a hopper pushing the sender writes the remote chest directly.
 *
 * <h2>The transcription map (upstream :53-306)</h2>
 * <ul>
 * <li><b>checkTarget</b> (:110-128): the persisted position re-resolves the target BE
 *     (the {@code worldObj.blockExists} :116 arm = the {@code isLoaded} guard — the p35
 *     cross-dim POC R1 ruling), self-seeded = no target; a free target claims this
 *     sender ({@code mTarget.mSender = this} :126);</li>
 * <li><b>scanPipes</b> (:130-178): lazy BFS from the BACK-adjacent block through SAME
 *     wire-pipe blocks (instance equality = the block+meta match), the first endpoint
 *     whose FRONT-adjacent block sits in the wire set is the target (:162-167) — the
 *     two endpoints face the SAME direction, both mFacings point upstream; the item
 *     family only links over the {@code temperature < 0} meta (the :141 arm — meta 0);
 *     the walk halts on unloaded chunks (upstream loaded them through the
 *     {@code mIgnoreUnloadedChunks = F} window — the declared no-load posture);</li>
 * <li><b>the inventory window</b> (:190-285): the IInventory delegation folded onto the
 *     IItemHandler face — getSlots/getStackInSlot/insertItem/extractItem/getSlotLimit/
 *     isItemValid all forward (the upstream canExtractItem2 = F arm is the GT
 *     machine-pull face, the vanilla decrStackSize forwarding IS the extraction, both
 *     map onto the forwarded extractItem);</li>
 * <li><b>invalidation</b> (:183-184): onCoordinateChange/onMachineBlockUpdate reset the
 *     link — the wire block's place/remove flood calls {@link #invalidateLink()};</li>
 * <li><b>the tool faces</b> (:85-108): the soft-hammer rescan and the magnifying-glass
 *     status chat ride the tool pool; the switchable-onoff channel rides it too
 *     ({@code mStopped} stays the NBT carrier and the checkTarget :111 first gate).</li>
 * </ul>
 *
 * <p>The link pair is TRANSIENT (the upstream mTarget/mSender :55 — not persisted); the
 * position persists (:62-75, the X/Y/Z triple folded into the packed BlockPos, the
 * GT6LongDistanceTransformerBlockEntity fold). KJS surface: none (the registration
 * face is deferred — the KJS binding pool).
 */
public class GT6LongDistanceItemPipeBlockEntity extends TileEntityBase03TicksAndSync {

	// upstream CS.java literal keys (the transformer fold form)
	public static final String NBT_STOPPED = "gt.stopped";
	public static final String NBT_TARGET = "gt.target"; // the live-link presence flag (upstream NBT_TARGET = T)
	public static final String NBT_TARGET_POS = "gt.target_pos"; // the packed BlockPos (the X/Y/Z triple folded)

	/** The soft-hammer stop (:54 — the checkTarget :111 first gate). */
	public boolean mStopped = false;

	/** The BE runtime facing mirror (the state is the authority; the transformer syncFacingFromState form, tick-free). */
	public byte mFacing = 2; // NORTH

	// the transient link pair (:55 — NOT persisted; the position is, :62-75)
	/** The far endpoint this window looks into (transient, re-resolved off the position). */
	@Nullable public GT6LongDistanceItemPipeBlockEntity mTarget = null;
	/** The near endpoint that claims this one as its target (the backlink, :126). */
	@Nullable public GT6LongDistanceItemPipeBlockEntity mSender = null;
	/** The persisted target position (self = the no-target seed, :133). */
	@Nullable public BlockPos mTargetPos = null;

	// the offline rig seams (the transformer form: the level-less delegate rig)

	/** The offline target injection (the scan short-circuits when set). */
	boolean mOfflineScan = false;
	@Nullable private GT6LongDistanceItemPipeBlockEntity mTargetOverride = null;
	/** The offline remote-window injection (the level-less handler rig). */
	@Nullable private IItemHandler mRemoteOverride = null;

	/** The shared window object (the capability identity; re-resolves the remote per call). */
	private final IItemHandler mWindow = new Window();
	//? if forge {
	/** Lazy capability handle over {@link #mWindow} (the 01Root ADR-P3-2 form). */
	private final LazyOptional<IItemHandler> mItemHandlerCap = LazyOptional.of(() -> mWindow);
	//?}

	/** BET factory (the registry runtime path). */
	public GT6LongDistanceItemPipeBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — the offline (test) entry point. */
	public GT6LongDistanceItemPipeBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(false, aType != null ? aType : GT6LongDistPipes.LONGDIST_ITEM_PIPE_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "longdist_item_pipe"; // the BET registry path mirrors it
	}

	// ---------------------------------------------------------------------------
	// the facing mirror (tick-free: the LOGIC reads the state through {@link #facing()}
	// at every use — a freshly placed BE runs no load and no tick, so the cached
	// {@link #mFacing} byte alone would serve a stale default to the peer reads)
	// ---------------------------------------------------------------------------

	/** The live facing — the state is the authority; the byte is the offline/NBT fallback. */
	public byte facing() {
		if (hasLevel() && getBlockState().hasProperty(GT6ElectricTransformerBlock.FACING)) {
			return (byte) getBlockState().getValue(GT6ElectricTransformerBlock.FACING).get3DDataValue();
		}
		return mFacing;
	}

	// ---------------------------------------------------------------------------
	// the link management (checkTarget :110-128 + scanPipes :130-178)
	// ---------------------------------------------------------------------------

	/** The upstream checkTarget — the target re-resolution + the sender backlink claim. */
	public boolean checkTarget() {
		if (mStopped || isClientSide()) return false; // :111
		if (mTargetOverride != null) { // the offline rig (no level walk)
			mTarget = mTargetOverride;
			mTargetPos = mTarget.getBlockPos();
			mTarget.mSender = this;
			return true;
		}
		if (mTargetPos == null) { // :112-113
			scanPipes();
		} else if (mTarget == null || mTarget.isRemoved()) { // :114
			mTarget = null;
			if (hasLevel() && getLevel().isLoaded(mTargetPos)) { // :116 — the blockExists arm, the POC R1 guard
				BlockEntity tTileEntity = getLevel().getBlockEntity(mTargetPos); // :117
				if (tTileEntity instanceof GT6LongDistanceItemPipeBlockEntity tPipe) { // :118-119
					mTarget = tPipe;
				} else if (tTileEntity != null) {
					mTargetPos = null; // :121 — the foreign occupant invalidates the stale position
				}
			}
		}
		if (mTarget == null || mTarget == this) return false; // :125
		if (mTarget.mSender == null || mTarget.mSender.isRemoved() || mTarget.mSender.mTarget == null || mTarget.mSender.mTarget.isRemoved()) {
			mTarget.mSender = this; // :126 — the free target claims this sender
		}
		return mTarget.mSender == this; // :127
	}

	/** The FRONT block of a peer (its wire-side face — the upstream getOffset(mFacing, 1) :162). */
	private static BlockPos frontOf(GT6LongDistanceItemPipeBlockEntity aPipe) {
		return aPipe.getBlockPos().relative(Direction.from3DDataValue(aPipe.facing()));
	}

	/**
	 * The upstream scanPipes (:130-178) — the lazy BFS over same-instance wire-pipe
	 * blocks from the BACK-adjacent position; the walk halts on unloaded chunks (the
	 * declared no-load posture over the upstream {@code mIgnoreUnloadedChunks = F}
	 * load-on-demand window).
	 */
	public void scanPipes() {
		if (mSender != null && !mSender.isRemoved() && mSender.mTarget == this) return; // :131
		mTargetPos = getBlockPos(); // :133 — the self seed
		mTarget = this; // :134
		mSender = null; // :135
		if (!hasLevel() || mOfflineScan) return;
		byte tFacing = facing();
		BlockPos tSeed = getBlockPos().relative(Direction.from3DDataValue(tFacing).getOpposite()); // :137 — the BACK-adjacent block
		if (!getLevel().isLoaded(tSeed)) return; // the POC R1 guard
		BlockState tBehind = getLevel().getBlockState(tSeed);
		if (!(tBehind.getBlock() instanceof GT6LongDistPipeBlock tPipeBlock)) return; // :139 — no pipe face, no link
		if (tPipeBlock.temperatureK() >= 0) return; // :141 — the item family rides the < 0 meta only

		Set<BlockPos> tOldChecks = new HashSet<>();
		tOldChecks.add(getBlockPos()); // :144
		Deque<BlockPos> tToCheck = new ArrayDeque<>();
		tToCheck.add(tSeed); // :145
		Set<BlockPos> tWires = new HashSet<>(); // :146
		while (!tToCheck.isEmpty()) {
			Set<BlockPos> tNewChecks = new HashSet<>();
			for (BlockPos aCoords : tToCheck) {
				if (!getLevel().isLoaded(aCoords)) continue; // the POC R1 guard — halt, no synchronous load
				if (getLevel().getBlockState(aCoords).getBlock() == tPipeBlock) { // :150 — the same-instance match
					tWires.add(aCoords);
					for (Direction tDir : Direction.values()) {
						BlockPos tNext = aCoords.relative(tDir);
						if (tOldChecks.add(tNext)) tNewChecks.add(tNext); // :153-158
					}
				} else {
					BlockEntity tTileEntity = getLevel().getBlockEntity(aCoords); // :160
					if (tTileEntity != this && tTileEntity instanceof GT6LongDistanceItemPipeBlockEntity tOther) { // :161
						if (tWires.contains(frontOf(tOther))) { // :162 — the peer whose FRONT sits in the blob
							mTarget = tOther; // :163-164
							mTargetPos = tOther.getBlockPos();
							return;
						}
						tOldChecks.remove(aCoords); // :168
					}
				}
			}
			tToCheck = new ArrayDeque<>(tNewChecks); // :172-174
		}
	}

	/** The upstream onMachineBlockUpdate :184 — the wire flood reset (the next lazy access re-scans). */
	public void invalidateLink() {
		mTargetPos = null;
		mSender = null;
	}

	// ---------------------------------------------------------------------------
	// the remote window (the IInventory delegation :190-285)
	// ---------------------------------------------------------------------------

	/** The offline remote-handler injection (the level-less window rig). */
	void setRemoteOverride(@Nullable IItemHandler aRemote) {
		mRemoteOverride = aRemote;
	}

	/** The package-private test entry over the window (the Root itemHandlerCapability seam form — ForgeCapabilities cannot class-init offline). */
	IItemHandler window() {
		return mWindow;
	}

	/** The offline target injection (the level-less delegate rig). */
	void setTargetOverride(@Nullable GT6LongDistanceItemPipeBlockEntity aTarget) {
		mTargetOverride = aTarget;
		mOfflineScan = aTarget != null;
	}

	/**
	 * The delegation target: {@code mTarget.getAdjacentInventory(OPOS[mTarget.mFacing])}
	 * (:193/:201/:209/…) — the inventory at the TARGET's BACK. The face passed to the
	 * remote handler is the TARGET's FRONT direction (the upstream mSideOfTileEntity =
	 * OPOS[aSide] flip, DelegatorTileEntity via getAdjacentTileEntity :221).
	 */
	@Nullable
	IItemHandler remoteInventory() {
		if (!checkTarget()) return null; // the stopped/no-link gate rides the live path even under the rig
		if (mRemoteOverride != null) return mRemoteOverride;
		if (!hasLevel() || mTarget == null) return null;
		Direction tBack = Direction.from3DDataValue(mTarget.facing()).getOpposite();
		BlockPos tAdjacent = mTarget.getBlockPos().relative(tBack);
		if (!getLevel().isLoaded(tAdjacent)) return null; // the POC R1 guard
		BlockEntity tTileEntity = getLevel().getBlockEntity(tAdjacent);
		if (tTileEntity == null || tTileEntity.isRemoved()) return null;
		return GTItemPipeBlockEntity.itemHandlerOf(tTileEntity, tBack.getOpposite());
	}

	/** The forwarded IItemHandler — the upstream IInventory delegation folded onto the modern face. */
	private class Window implements IItemHandler {
		@Override
		public int getSlots() { // :223-229 getSizeInventory
			IItemHandler tRemote = remoteInventory();
			return tRemote == null ? 0 : tRemote.getSlots();
		}

		@Override
		public ItemStack getStackInSlot(int aSlot) { // :207-213 getStackInSlot
			IItemHandler tRemote = remoteInventory();
			return tRemote == null ? ItemStack.EMPTY : tRemote.getStackInSlot(aSlot);
		}

		@Override
		public ItemStack insertItem(int aSlot, ItemStack aStack, boolean aSimulate) { // :239-244 setInventorySlotContents face
			IItemHandler tRemote = remoteInventory();
			return tRemote == null ? aStack : tRemote.insertItem(aSlot, aStack, aSimulate);
		}

		@Override
		public ItemStack extractItem(int aSlot, int aAmount, boolean aSimulate) { // :191-205 decrStackSize face
			IItemHandler tRemote = remoteInventory();
			return tRemote == null ? ItemStack.EMPTY : tRemote.extractItem(aSlot, aAmount, aSimulate);
		}

		@Override
		public int getSlotLimit(int aSlot) { // :231-237 getInventoryStackLimit
			IItemHandler tRemote = remoteInventory();
			return tRemote == null ? 0 : tRemote.getSlotLimit(aSlot);
		}

		@Override
		public boolean isItemValid(int aSlot, ItemStack aStack) { // :254-260 isItemValidForSlot
			IItemHandler tRemote = remoteInventory();
			return tRemote != null && tRemote.isItemValid(aSlot, aStack);
		}
	}

	// ---------------------------------------------------------------------------
	// capability (the all-sides window — the upstream IInventory had no admission sides)
	// ---------------------------------------------------------------------------

	//? if forge {
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> aCapability, @Nullable Direction aSide) {
		if (aCapability == ForgeCapabilities.ITEM_HANDLER) {
			return mItemHandlerCap.cast();
		}
		return super.getCapability(aCapability, aSide);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		mItemHandlerCap.invalidate();
	}
	//?} else {
	/*// (1.21.1 seam: NeoForge 21.1 removed BlockEntity#getCapability — the W4
	// RegisterCapabilitiesEvent.registerBlockEntity delegates to this member; no @Override.
	// FQ names — the GT6HopperBaseBlockEntity seam form (no import management).
	public <T> T getCapability(net.neoforged.neoforge.capabilities.BlockCapability<T, Direction> aCapability, @Nullable Direction aSide) {
		if (aCapability == net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK) {
			return (T) mWindow;
		}
		return null;
	}
	 *///?}

	// ---------------------------------------------------------------------------
	// NBT (:59-75 — the link position persists, the link pair does not)
	// ---------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putBoolean(NBT_STOPPED, mStopped); // :74
		aNBT.putByte("facing", mFacing);
		if (mTargetPos != null && mTarget != this) { // :68 — the live link persists
			aNBT.putBoolean(NBT_TARGET, true); // :69
			aNBT.putLong(NBT_TARGET_POS, mTargetPos.asLong()); // :70-72 — the packed position
		}
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_STOPPED, Tag.TAG_ANY_NUMERIC)) mStopped = aNBT.getBoolean(NBT_STOPPED); // :61
		if (aNBT.contains(NBT_TARGET, Tag.TAG_ANY_NUMERIC) && aNBT.contains(NBT_TARGET_POS, Tag.TAG_ANY_NUMERIC)) {
			mTargetPos = BlockPos.of(aNBT.getLong(NBT_TARGET_POS)); // :62
		}
		if (aNBT.contains("facing", Tag.TAG_ANY_NUMERIC)) mFacing = aNBT.getByte("facing");
	}

	@Override
	public String toString() {
		return "LDItemPipe@" + (hasLevel() ? getBlockPos() : "offline") + (mStopped ? " stopped" : "");
	}
}
