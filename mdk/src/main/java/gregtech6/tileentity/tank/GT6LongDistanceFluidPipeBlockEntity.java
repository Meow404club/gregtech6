package gregtech6.tileentity.tank;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

//? if forge {
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
//?}
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import gregtech6.block.energy.GT6ElectricTransformerBlock;
import gregtech6.block.tools.GT6LongDistPipeBlock;
import gregtech6.registry.GT6LongDistPipes;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The Long Distance Fluid Pipeline Endpoint (task p35-long-distance-pipes) — the 1.20.1
 * transcription of {@code MultiTileEntityLongDistancePipelineFluid} (:58-254, the
 * TileEntityBase09FacingSingle shape over the port BE tree; Loader_MultiTileEntities
 * :907, meta id 10061). Two endpoints joined by a blob of {@link GT6LongDistPipeBlock}
 * push fluid ACROSS the blob in ONE hop with NO transport tick: the fill forwards
 * straight into {@code mTarget.getAdjacentTank(OPOS[mTarget.mFacing])} (:199-205) —
 * fill-only, the drain arms return null/EMPTY and canDrain = F (:206-225), the tank
 * query forwards (:226-233).
 *
 * <h2>The transcription map (upstream :58-254)</h2>
 * <ul>
 * <li><b>checkTarget</b> (:118-136): the shared shape — the persisted position
 *     re-resolves the target BE (the {@code worldObj.blockExists} :124 arm = the
 *     {@code isLoaded} guard, the POC R1 ruling), the free target claims this sender
 *     (:134); the scan re-runs while {@code mTemperature <= 0} (:120 — the upstream
 *     NBT_TEMPERATURE read-without-write is load-bearing: the rating re-scans off the
 *     block on the first access after every load);</li>
 * <li><b>scanPipes</b> (:138-186): the item BE's BFS twin; the temperature rating
 *     rides the seed block ({@code mTemperatures[aMetaData]} :148 — the block carrier's
 *     only data column), {@code <= 0} refuses (the :149 arm — metas 5..15 are dead,
 *     meta 0 is the item family);</li>
 * <li><b>the fill gate</b> (:200/:216): {@code FL.temperature(aFluid) <= mTemperature}
 *     — the fluid must not exceed the pipe's rating; the temperature read is the fluid
 *     attributes' K value with the 300 K default for null stacks (the FL.temperature
 *     :798-803 form);</li>
 * <li><b>invalidation</b> (:191-192): the wire flood reset (the item BE shape).</li>
 * </ul>
 *
 * <p>The link pair is TRANSIENT (the upstream mTarget/mSender :61); the position
 * persists (:69-79, the packed BlockPos fold — the upstream NBT_THROUGHPUT write of
 * mTemperature :80 has no reader and rides along as the byte-faithful debug face).
 * KJS surface: none (the registration face is deferred — the KJS binding pool).
 */
public class GT6LongDistanceFluidPipeBlockEntity extends TileEntityBase03TicksAndSync {

	// upstream CS.java literal keys (the transformer fold form)
	public static final String NBT_STOPPED = "gt.stopped";
	public static final String NBT_TARGET = "gt.target"; // the live-link presence flag (upstream NBT_TARGET = T)
	public static final String NBT_TARGET_POS = "gt.target_pos"; // the packed BlockPos (the X/Y/Z triple folded)
	public static final String NBT_THROUGHPUT = "gt.throughput"; // the upstream :80 write (mTemperature) — the debug face

	/** The soft-hammer stop (:59 — the checkTarget :119 first gate). */
	public boolean mStopped = false;

	/** The pipe's temperature rating in K (the :148 scan column; 0 = unscanned/no link, -1 unreachable on this family). */
	public long mTemperature = 0;

	/** The BE runtime facing mirror (the state is the authority; the tick-free mirror). */
	public byte mFacing = 2; // NORTH

	// the transient link pair (:61 — NOT persisted; the position is, :69-79)
	/** The far endpoint this fill window looks into (transient, re-resolved off the position). */
	@Nullable public GT6LongDistanceFluidPipeBlockEntity mTarget = null;
	/** The near endpoint that claims this one as its target (the backlink, :134). */
	@Nullable public GT6LongDistanceFluidPipeBlockEntity mSender = null;
	/** The persisted target position (self = the no-target seed, :141). */
	@Nullable public BlockPos mTargetPos = null;

	// the offline rig seams (the item BE form: the level-less delegate rig)

	/** The offline target injection (the scan short-circuits when set). */
	boolean mOfflineScan = false;
	@Nullable private GT6LongDistanceFluidPipeBlockEntity mTargetOverride = null;
	/** The offline remote-window injection (the level-less handler rig). */
	@Nullable private IFluidHandler mRemoteOverride = null;

	/** The shared window object (the capability identity; re-resolves the remote per call). */
	private final IFluidHandler mWindow = new Window();
	//? if forge {
	/** Lazy capability handle over {@link #mWindow} (the 01Root ADR-P3-2 form). */
	private final LazyOptional<IFluidHandler> mFluidHandlerCap = LazyOptional.of(() -> mWindow);
	//?}

	/** BET factory (the registry runtime path). */
	public GT6LongDistanceFluidPipeBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — the offline (test) entry point. */
	public GT6LongDistanceFluidPipeBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(false, aType != null ? aType : GT6LongDistPipes.LONGDIST_FLUID_PIPE_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "longdist_fluid_pipe"; // the BET registry path mirrors it
	}

	// ---------------------------------------------------------------------------
	// the facing mirror (tick-free: the state is re-read lazily on the link work)
	// ---------------------------------------------------------------------------

	private void syncFacingFromState() {
		if (!hasLevel()) return; // the offline fixtures have no level
		if (getBlockState().hasProperty(GT6ElectricTransformerBlock.FACING)) {
			mFacing = (byte) getBlockState().getValue(GT6ElectricTransformerBlock.FACING).get3DDataValue();
		}
	}

	// ---------------------------------------------------------------------------
	// the temperature gate (the FL.temperature :798-803 form over the attributes API)
	// ---------------------------------------------------------------------------

	/** The fluid's K temperature — the attributes read (the CoverDrain getFluidType dual-leg form), 300 K default (FL.DEF_ENV_TEMP). */
	static long temperatureOf(@Nullable FluidStack aFluid) {
		if (aFluid == null || aFluid.isEmpty()) return 300;
		try {
			return aFluid.getFluid().getFluidType().getTemperature();
		} catch (NullPointerException tUnbound) {
			return 300; // the offline doubles cannot bind the vanilla fluid-type RegistryObjects (the CoverDrain form)
		}
	}

	// ---------------------------------------------------------------------------
	// the link management (checkTarget :118-136 + scanPipes :138-186)
	// ---------------------------------------------------------------------------

	/** The upstream checkTarget — the target re-resolution + the sender backlink claim. */
	public boolean checkTarget() {
		if (mStopped || isClientSide()) return false; // :119
		if (mTargetOverride != null) { // the offline rig (no level walk)
			mTarget = mTargetOverride;
			mTargetPos = mTarget.getBlockPos();
			mTarget.mSender = this;
			return true;
		}
		syncFacingFromState();
		if (mTargetPos == null || mTemperature <= 0) { // :120 — the rating re-scan arm
			scanPipes();
		} else if (mTarget == null || mTarget.isRemoved()) { // :122
			mTarget = null;
			if (hasLevel() && getLevel().isLoaded(mTargetPos)) { // :124 — the blockExists arm, the POC R1 guard
				BlockEntity tTileEntity = getLevel().getBlockEntity(mTargetPos); // :125
				if (tTileEntity instanceof GT6LongDistanceFluidPipeBlockEntity tPipe) { // :126-127
					mTarget = tPipe;
				} else if (tTileEntity != null) {
					mTargetPos = null; // :129 — the foreign occupant invalidates the stale position
				}
			}
		}
		if (mTarget == null || mTarget == this) return false; // :133
		if (mTarget.mSender == null || mTarget.mSender.isRemoved() || mTarget.mSender.mTarget == null || mTarget.mSender.mTarget.isRemoved()) {
			mTarget.mSender = this; // :134 — the free target claims this sender
		}
		return mTarget.mSender == this; // :135
	}

	/** The FRONT block of a peer (its wire-side face — the upstream getOffset(mFacing, 1) :170). */
	private static BlockPos frontOf(GT6LongDistanceFluidPipeBlockEntity aPipe) {
		return aPipe.getBlockPos().relative(Direction.from3DDataValue(aPipe.mFacing));
	}

	/**
	 * The upstream scanPipes (:138-186) — the lazy BFS over same-instance wire-pipe
	 * blocks from the BACK-adjacent position; the temperature rating rides the seed
	 * block (:148); the walk halts on unloaded chunks (the declared no-load posture).
	 */
	public void scanPipes() {
		if (mSender != null && !mSender.isRemoved() && mSender.mTarget == this) return; // :139
		mTargetPos = getBlockPos(); // :141 — the self seed
		mTarget = this; // :142
		mSender = null; // :143
		mTemperature = 0; // :144
		if (!hasLevel() || mOfflineScan) return;
		syncFacingFromState();
		BlockPos tSeed = getBlockPos().relative(Direction.from3DDataValue(mFacing).getOpposite()); // :145 — the BACK-adjacent block
		if (!getLevel().isLoaded(tSeed)) return; // the POC R1 guard
		BlockState tBehind = getLevel().getBlockState(tSeed);
		if (!(tBehind.getBlock() instanceof GT6LongDistPipeBlock tPipeBlock)) return; // :147 — no pipe face, no link
		mTemperature = tPipeBlock.temperatureK(); // :148 — the rating column
		if (mTemperature <= 0) return; // :149 — the dead metas and the item meta refuse

		Set<BlockPos> tOldChecks = new HashSet<>();
		tOldChecks.add(getBlockPos()); // :152
		Deque<BlockPos> tToCheck = new ArrayDeque<>();
		tToCheck.add(tSeed); // :153
		Set<BlockPos> tWires = new HashSet<>(); // :154
		while (!tToCheck.isEmpty()) {
			Set<BlockPos> tNewChecks = new HashSet<>();
			for (BlockPos aCoords : tToCheck) {
				if (!getLevel().isLoaded(aCoords)) continue; // the POC R1 guard — halt, no synchronous load
				if (getLevel().getBlockState(aCoords).getBlock() == tPipeBlock) { // :158 — the same-instance match
					tWires.add(aCoords);
					for (Direction tDir : Direction.values()) {
						BlockPos tNext = aCoords.relative(tDir);
						if (tOldChecks.add(tNext)) tNewChecks.add(tNext); // :161-166
					}
				} else {
					BlockEntity tTileEntity = getLevel().getBlockEntity(aCoords); // :168
					if (tTileEntity != this && tTileEntity instanceof GT6LongDistanceFluidPipeBlockEntity tOther) { // :169
						if (tWires.contains(frontOf(tOther))) { // :170 — the peer whose FRONT sits in the blob
							mTarget = tOther; // :171-172
							mTargetPos = tOther.getBlockPos();
							return;
						}
						tOldChecks.remove(aCoords); // :176
					}
				}
			}
			tToCheck = new ArrayDeque<>(tNewChecks); // :180-182
		}
	}

	/** The upstream onMachineBlockUpdate :192 — the wire flood reset (the next lazy access re-scans). */
	public void invalidateLink() {
		mTargetPos = null;
		mSender = null;
	}

	// ---------------------------------------------------------------------------
	// the remote window (the fill/drain/tank-query delegation :198-233)
	// ---------------------------------------------------------------------------

	/** The offline remote-handler injection (the level-less window rig). */
	void setRemoteOverride(@Nullable IFluidHandler aRemote) {
		mRemoteOverride = aRemote;
	}

	/** The package-private test entry over the window (the Root itemHandlerCapability seam form — ForgeCapabilities cannot class-init offline). */
	IFluidHandler window() {
		return mWindow;
	}

	/** The offline target injection (the level-less delegate rig). */
	void setTargetOverride(@Nullable GT6LongDistanceFluidPipeBlockEntity aTarget) {
		mTargetOverride = aTarget;
		mOfflineScan = aTarget != null;
	}

	/**
	 * The delegation target: {@code mTarget.getAdjacentTank(OPOS[mTarget.mFacing])}
	 * (:201/:217/:229) — the tank at the TARGET's BACK. The face passed to the remote
	 * handler is the TARGET's FRONT direction (the upstream getForgeSideOfTileEntity
	 * flip — the mSideOfTileEntity = OPOS[aSide] DelegatorTileEntity form).
	 */
	@Nullable
	IFluidHandler remoteTank() {
		if (!checkTarget()) return null; // the stopped/no-link gate rides the live path even under the rig
		if (mRemoteOverride != null) return mRemoteOverride;
		if (!hasLevel() || mTarget == null) return null;
		Direction tBack = Direction.from3DDataValue(mTarget.mFacing).getOpposite();
		BlockPos tAdjacent = mTarget.getBlockPos().relative(tBack);
		if (!getLevel().isLoaded(tAdjacent)) return null; // the POC R1 guard
		BlockEntity tTileEntity = getLevel().getBlockEntity(tAdjacent);
		if (tTileEntity == null || tTileEntity.isRemoved()) return null;
		//? if forge {
		return tTileEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, tBack.getOpposite()).orElse(null);
		//?} else {
		/*return tTileEntity.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK, tTileEntity.getBlockPos(), tBack.getOpposite());
		 *///?}
	}

	/** The fill-only window — the upstream IFluidHandler delegation folded onto the modern face. */
	private class Window implements IFluidHandler {
		@Override
		public int getTanks() { // :227-233 getTankInfo (the slot count fold)
			IFluidHandler tRemote = remoteTank();
			return tRemote == null ? 0 : tRemote.getTanks();
		}

		@Override
		public FluidStack getFluidInTank(int aTank) { // the getTankInfo contents fold
			IFluidHandler tRemote = remoteTank();
			return tRemote == null ? FluidStack.EMPTY : tRemote.getFluidInTank(aTank);
		}

		@Override
		public int getTankCapacity(int aTank) { // the getTankInfo capacity fold
			IFluidHandler tRemote = remoteTank();
			return tRemote == null ? 0 : tRemote.getTankCapacity(aTank);
		}

		@Override
		public boolean isFluidValid(int aTank, FluidStack aStack) { // :215-221 canFill (the gate + the remote ask)
			if (!fillGate(aStack)) return false;
			IFluidHandler tRemote = remoteTank();
			return tRemote != null && tRemote.isFluidValid(aTank, aStack);
		}

		@Override
		public int fill(FluidStack aStack, IFluidHandler.FluidAction aAction) { // :199-205 fill
			if (!fillGate(aStack)) return 0;
			IFluidHandler tRemote = remoteTank();
			return tRemote == null ? 0 : tRemote.fill(aStack, aAction);
		}

		@Override
		public FluidStack drain(FluidStack aResource, IFluidHandler.FluidAction aAction) {
			return FluidStack.EMPTY; // :207-209 — fill-only
		}

		@Override
		public FluidStack drain(int aMaxDrain, IFluidHandler.FluidAction aAction) {
			return FluidStack.EMPTY; // :211-213 — fill-only
		}
	}

	/** The :200/:216 gate — the live link AND the fluid within the pipe's rating. */
	private boolean fillGate(@Nullable FluidStack aStack) {
		return checkTarget() && temperatureOf(aStack) <= mTemperature;
	}

	// ---------------------------------------------------------------------------
	// capability (the all-sides fill window — the upstream IFluidHandler had no sides)
	// ---------------------------------------------------------------------------

	//? if forge {
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> aCapability, @Nullable Direction aSide) {
		if (aCapability == ForgeCapabilities.FLUID_HANDLER) {
			return mFluidHandlerCap.cast();
		}
		return super.getCapability(aCapability, aSide);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		mFluidHandlerCap.invalidate();
	}
	//?} else {
	/*// (1.21.1 seam: NeoForge 21.1 removed BlockEntity#getCapability — the W4
	// RegisterCapabilitiesEvent.registerBlockEntity delegates to this member; no @Override.
	// FQ names — the GT6HopperBaseBlockEntity seam form (no import management).
	public <T> T getCapability(net.neoforged.neoforge.capabilities.BlockCapability<T, Direction> aCapability, @Nullable Direction aSide) {
		if (aCapability == net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK) {
			return (T) mWindow;
		}
		return null;
	}
	 *///?}

	// ---------------------------------------------------------------------------
	// NBT (:65-83 — the link position persists, the link pair does not)
	// ---------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putBoolean(NBT_STOPPED, mStopped); // :82
		aNBT.putByte("facing", mFacing);
		if (mTargetPos != null && mTarget != this) { // :75 — the live link persists
			aNBT.putBoolean(NBT_TARGET, true); // :76
			aNBT.putLong(NBT_TARGET_POS, mTargetPos.asLong()); // :77-79 — the packed position
			aNBT.putLong(NBT_THROUGHPUT, mTemperature); // :80 — the upstream debug face (no reader)
		}
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_STOPPED, Tag.TAG_ANY_NUMERIC)) mStopped = aNBT.getBoolean(NBT_STOPPED); // :67
		if (aNBT.contains(NBT_TARGET, Tag.TAG_ANY_NUMERIC) && aNBT.contains(NBT_TARGET_POS, Tag.TAG_ANY_NUMERIC)) {
			mTargetPos = BlockPos.of(aNBT.getLong(NBT_TARGET_POS)); // :69
		}
		if (aNBT.contains(NBT_THROUGHPUT, Tag.TAG_ANY_NUMERIC)) {
			// the upstream :68 reads NBT_TEMPERATURE — a key nothing ever wrote; the port
			// reads its own write instead, the <= 0 re-scan arm (:120) keeps its semantics
			// because scanPipes re-reads the rating off the block either way
			mTemperature = aNBT.getLong(NBT_THROUGHPUT);
		}
		if (aNBT.contains("facing", Tag.TAG_ANY_NUMERIC)) mFacing = aNBT.getByte("facing");
		syncFacingFromState(); // the state is the authority (the tick-free mirror)
	}

	@Override
	public String toString() {
		return "LDFluidPipe@" + (hasLevel() ? getBlockPos() : "offline") + (mStopped ? " stopped" : "");
	}
}
