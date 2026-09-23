package gregtech6.tileentity.energy;

import java.util.ArrayDeque;
import java.util.Collection;
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

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.IEnergyAdjacency;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.block.energy.GT6ElectricTransformerBlock;
import gregtech6.block.energy.GT6LongDistWireBlock;
import gregtech6.registry.GT6LongDistanceTransformers;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The Long Distance Transformer Endpoint (task p35-energy-tail-machines) — the 1.20.1
 * transcription of {@code MultiTileEntityLongDistanceTransformer} (:58-325, the
 * TileEntityBase09FacingSingle shape over the port BE tree). Two endpoints joined by a
 * line of {@link GT6LongDistWireBlock} push EU ACROSS the wire blob in ONE hop
 * (sender FRONT in → BACK out into the wires → BFS → the target's FRONT), trading
 * packet size for distance: the doInject loss {@code aSize - max(64, mDistance/8)}
 * (the upstream 0.125 EU/m tooltip column).
 *
 * <h2>The transcription map (upstream :58-325)</h2>
 * <ul>
 * <li><b>scanWires</b> (:175-229): BFS from the BACK-adjacent block through SAME wire
 *     blocks (instance equality = the upstream block+meta match); the first LD
 *     transformer whose FRONT-adjacent block sits in the wire set is the target
 *     (:212-218); throughput = the wire's VMAX[tier] (:186); the burn arm (:206-209,
 *     scanWires(T) on an oversize-throughput packet) consumes the wires — the fire
 *     spread half is the render/pool face, the wires just die here (declared);</li>
 * <li><b>checkTarget</b> (:155-173): the persisted position re-resolves the target BE
 *     (isLoaded = the upstream worldObj.blockExists), self-seeded = no target;</li>
 * <li><b>doInject</b> (:232-259): stopped → 0; oversize vs the Root max → the
 *     overcharge ladder, consumed-all; no target → 0; packet over the wire throughput →
 *     burn-rescan, consumed-all; the distance loss; below the target's output min → 0;
 *     above the target's output max → the TARGET overcharges; the accepted rest goes
 *     ONE {@code emitEnergyToNetwork(target type, ..., mTarget)} (:254 — the target is
 *     the EMITTER, its BACK face feeds the consumer net) and both mActive flags ride
 *     (:255);</li>
 * <li><b>faces</b> (:261-262/:284-285): FRONT = input, BACK = output, EU/EU only;</li>
 * <li><b>bands</b> (:282-283 rec + the Root defaults): in rec = V[i], out rec = V[i]
 *     (NBT_INPUT/NBT_OUTPUT both V[i] — the :909-:913 rows), min = rec/2, max = rec*2.</li>
 * </ul>
 *
 * <h2>Cropped with declaration</h2>
 * The 4-state blink visuals (:134-153 mActiveData/mActiveState) collapse to the
 * {@link #mActive} flag (the W2 render posture); the tool-click chat face (:108-131)
 * and the switchable-onoff tool channel ride the tool pool ({@code mStopped} stays the
 * NBT/soft-hammer carrier); ITileEntityMachineBlockUpdateable wire-invalidation
 * (:288) folds — the periodic checkTarget in doInject re-scans when the target died,
 * the neighbor hook is an optimization at these scales; ITileEntityCanDelegate stays
 * unported (the upstream interface seat is the pipe family's; the transformer's own
 * doInject does the delegation work here).
 */
public class GT6LongDistanceTransformerBlockEntity extends TileEntityBase03TicksAndSync implements ITileEntityEnergy {

	// upstream CS.java literal keys
	public static final String NBT_STOPPED = "gt.stopped";
	public static final String NBT_ACTIVE = "gt.active";
	public static final String NBT_TARGET = "gt.target"; // the live-link presence flag (upstream NBT_TARGET = T)
	public static final String NBT_TARGET_POS = "gt.target_pos"; // the packed BlockPos (the upstream X/Y/Z triple folded)
	public static final String NBT_DISTANCE = "gt.distance";
	public static final String NBT_THROUGHPUT = "gt.throughput";

	/** The row's ladder index i = 4..8 (the :909-:913 line order; NBT_INPUT = NBT_OUTPUT = V[i]). */
	public final int mTier;

	/** The row packet size, BOTH directions (the NBT_INPUT/NBT_OUTPUT columns). */
	public final long mInput, mOutput;

	/** The soft-hammer stop (:59). */
	public boolean mStopped = false;
	/** Emitted on the last pass (:59). */
	public boolean mActive = false;
	/** The wire throughput = VMAX[tier] of the blob's wire (the :186 scan column; 0 = unscanned). */
	public long mThroughput = 0;
	/** The BFS hop count = the wire-line length in blocks (:181-195). */
	public long mDistance = 0;
	/** The energy type pair (:62-63, both EU on every row). */
	public TagData mEnergyTypeAccepted = TD.Energy.EU, mEnergyTypeEmitted = TD.Energy.EU;

	// the transient link pair (:64 — NOT persisted; the position is, :86-93)
	/** The far endpoint this sender pushes into (transient, re-resolved off the position). */
	@Nullable public GT6LongDistanceTransformerBlockEntity mTarget = null;
	/** The near endpoint that pushes into this one (the target's backlink, :171). */
	@Nullable public GT6LongDistanceTransformerBlockEntity mSender = null;
	/** The persisted target position (self = the no-target seed, :178-179). */
	@Nullable public BlockPos mTargetPos = null;

	/** The BE runtime facing mirror (FRONT = input, the transformer form). */
	public byte mFacing = 2; // NORTH

	/** The offline test seam (the transformer mAdjacencyOverride form — here the TARGET seam: the offline rig injects the far endpoint without a level). */
	@Nullable
	private GT6LongDistanceTransformerBlockEntity mTargetOverride = null;
	/** The offline test seam mirror: the scan short-circuits (no level walk) when set. */
	boolean mOfflineScan = false;

	/** BET factory (the registry runtime path; the tier resolves off the block state). */
	public GT6LongDistanceTransformerBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — the offline (test) entry point; tier resolves off the block (STONE fallback 4 = the first :909 row). */
	public GT6LongDistanceTransformerBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		this(aType, aPos, aState, resolveTier(aState));
	}

	/** The explicit-tier constructor (the offline ladder fixture). */
	public GT6LongDistanceTransformerBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState, int aTier) {
		super(true, aType != null ? aType : GT6LongDistanceTransformers.LONG_DISTANCE_TRANSFORMER_BE.get(), aPos, aState);
		mTier = aTier;
		mInput = mOutput = GTWireSpecsV(aTier); // NBT_INPUT = NBT_OUTPUT = V[i] (:909-:913)
	}

	/** The GTWireSpecs.V read (kept in a member for the same lazy shape the registries use). */
	private static long GTWireSpecsV(int aTier) {
		return gregtech6.registry.GTWireSpecs.V[aTier];
	}

	/** The tier ladder index off the block (the transformer resolveTier form; STONE fallback = 4, the first row). */
	static int resolveTier(BlockState aState) {
		return aState.getBlock() instanceof GT6ElectricTransformerBlock tBlock && tBlock.tier() >= 4 ? tBlock.tier() : 4;
	}

	@Override
	public String getTileEntityName() {
		return "longdist_transformer"; // the BET registry path mirrors it
	}

	// ---------------------------------------------------------------------------
	// the tick — the pass-through has NO capacitor work (the emit is doInject-driven);
	// only the facing mirror rides (the dynamo syncFacingFromState form)
	// ---------------------------------------------------------------------------

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		if (!aIsServerSide) return;
		syncFacingFromState();
	}

	/** The /setblock RCON path: the state is the authority. */
	void syncFacingFromState() {
		if (getBlockState().hasProperty(GT6ElectricTransformerBlock.FACING)) {
			mFacing = (byte) getBlockState().getValue(GT6ElectricTransformerBlock.FACING).get3DDataValue();
		}
	}

	// ---------------------------------------------------------------------------
	// the link management (checkTarget :155-173 + scanWires :175-229)
	// ---------------------------------------------------------------------------

	/** The upstream checkTarget — the target re-resolution + the sender backlink claim. */
	public boolean checkTarget() {
		if (mStopped || isClientSide()) return false;
		if (mTargetOverride != null) { // the offline rig (no level walk)
			mTarget = mTargetOverride;
			mTargetPos = mTarget.getBlockPos();
			mTarget.mSender = this;
			return true;
		}
		if (mTargetPos == null || mDistance <= 0 || mThroughput <= 0) {
			scanWires(false);
		} else if (mTarget == null || mTarget.isRemoved()) {
			mTarget = null;
			if (hasLevel() && getLevel().isLoaded(mTargetPos)) {
				BlockEntity tTileEntity = getLevel().getBlockEntity(mTargetPos);
				if (tTileEntity instanceof GT6LongDistanceTransformerBlockEntity tTrans) {
					mTarget = tTrans;
				} else if (tTileEntity != null) {
					mTargetPos = null; // the :166 foreign occupant invalidates the stale position
				}
			}
		}
		if (mTarget == null || mTarget == this) return false;
		if (mTarget.mSender == null || mTarget.mSender.isRemoved() || mTarget.mSender.mTarget == null || mTarget.mSender.mTarget.isRemoved()) {
			mTarget.mSender = this;
		}
		return mTarget.mSender == this;
	}

	/** The FRONT block of a peer (its wire-side face — the upstream getOffset(mFacing, 1)). */
	private static BlockPos frontOf(GT6LongDistanceTransformerBlockEntity aTrans) {
		return aTrans.getBlockPos().relative(Direction.from3DDataValue(aTrans.mFacing));
	}

	/**
	 * The upstream scanWires (:175-229) — the BFS over same-instance wire blocks from the
	 * BACK-adjacent position; {@code aBurnWires} = the oversize-throughput burn arm (the
	 * wires are consumed; the fire spread is the declared pool face).
	 */
	public void scanWires(boolean aBurnWires) {
		if (mSender != null && !mSender.isRemoved() && mSender.mTarget == this) return; // :176
		mTargetPos = getBlockPos(); // :178 — the self seed
		mTarget = this; // :179
		mSender = null; // :180
		mDistance = 0; // :181
		mThroughput = 0; // :182
		if (!hasLevel() || mOfflineScan) return;
		Direction tBack = Direction.from3DDataValue(mFacing).getOpposite();
		BlockState tBehind = getLevel().getBlockState(getBlockPos().relative(tBack));
		if (!(tBehind.getBlock() instanceof GT6LongDistWireBlock tWireBlock)) return; // :185 — no wire face, no link
		mThroughput = tWireBlock.throughput(); // :186 — VMAX[tier]

		Set<BlockPos> tOldChecks = new HashSet<>();
		tOldChecks.add(getBlockPos()); // :189
		Deque<BlockPos> tToCheck = new ArrayDeque<>();
		tToCheck.add(getBlockPos().relative(tBack)); // :190
		Set<BlockPos> tWires = new HashSet<>(); // :191
		mDistance = -1; // :193
		while (!tToCheck.isEmpty()) {
			mDistance++;
			Set<BlockPos> tNewChecks = new HashSet<>();
			for (BlockPos aCoords : tToCheck) {
				if (getLevel().getBlockState(aCoords).getBlock() == tWireBlock) { // :197 — the same-instance match
					tWires.add(aCoords);
					for (Direction tDir : Direction.values()) {
						BlockPos tNext = aCoords.relative(tDir);
						if (tOldChecks.add(tNext)) tNewChecks.add(tNext); // :200-205
					}
					if (aBurnWires) { // :206-209 — the burn arm (fire spread = the pool face)
						getLevel().removeBlock(aCoords, false);
					}
				} else {
					BlockEntity tTileEntity = getLevel().getBlockEntity(aCoords); // :211
					if (tTileEntity != this && tTileEntity instanceof GT6LongDistanceTransformerBlockEntity tOther) {
						if (tWires.contains(frontOf(tOther))) { // :213 — the peer whose FRONT sits in the blob
							mTarget = tOther; // :214-217
							mTargetPos = tOther.getBlockPos();
							return;
						}
						tOldChecks.remove(aCoords); // :219
					}
				}
			}
			tToCheck = new ArrayDeque<>(tNewChecks); // :223-225
		}
	}

	// ---------------------------------------------------------------------------
	// the pass-through (doInject :232-259 verbatim)
	// ---------------------------------------------------------------------------

	@Override
	public long doInject(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
		if (mStopped) return 0; // :233
		byte aSign = 1;
		if (aSize < 0) {aSize = -aSize; aSign = -1;} // :234-235
		if (aSize > getEnergySizeInputMax(aEnergyType, aSide)) { // :236 — the Root max (rec*2)
			if (aDoInject) overcharge(aSize, aEnergyType); // :237 — the Root explosion family (offline: log-only)
			return aAmount; // :238
		}
		if (!checkTarget()) return 0; // :240
		if (aSize > mThroughput) { // :241 — the wire rating
			if (aDoInject) scanWires(true); // :242 — the burn-rescan
			return aAmount; // :243
		}
		aSize = Math.max(0, aSize - Math.max(64, mDistance / 8)); // :245 — the 0.125/m loss floor
		if (aSize <= 0 || aSize < mTarget.getEnergySizeOutputMin(mTarget.mEnergyTypeEmitted, aSide)) return 0; // :246-248
		if (aSize > mTarget.getEnergySizeOutputMax(mTarget.mEnergyTypeEmitted, aSide)) { // :249
			if (aDoInject) mTarget.overcharge(aSize, aEnergyType); // :250
			return aAmount; // :251
		}
		if (aDoInject) { // :253-257 — the push leaves through the TARGET's output sides (its BACK face) into the consumer net
			long tConsumed = ITileEntityEnergy.Util.emitEnergyToNetwork(mTarget.mEnergyTypeEmitted, aSize * aSign, aAmount, mTarget, mTarget.adjacency());
			mTarget.mActive = mActive = (mActive || tConsumed > 0);
			return tConsumed;
		}
		return aAmount; // :258
	}

	// ---------------------------------------------------------------------------
	// the faces (:261-262/:281-285)
	// ---------------------------------------------------------------------------

	@Override
	public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
		return aEmitting ? aEnergyType == mEnergyTypeEmitted : aEnergyType == mEnergyTypeAccepted; // :281
	}

	@Override
	public Collection<TagData> getEnergyTypes(byte aSide) {
		return mEnergyTypeAccepted == mEnergyTypeEmitted ? mEnergyTypeAccepted.AS_LIST
				: java.util.List.of(mEnergyTypeAccepted, mEnergyTypeEmitted); // :263 (the dedup fold)
	}

	@Override
	public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {
		return (aTheoretical || checkTarget()) && (aSide == 6 || isInput(aSide)) && isEnergyType(aEnergyType, aSide, false); // :261
	}

	@Override
	public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {
		return (aSide == 6 || isOutput(aSide)) && isEnergyType(aEnergyType, aSide, true); // :262
	}

	@Override
	public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {return mOutput;} // :282
	@Override
	public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {return mInput;} // :283
	// the min/max bands ride the Root defaults over the rec pair (min = rec/2, max = rec*2),
	// the upstream no-override shape (:282-283 are the only band overrides)

	/** FRONT = input, BACK = output (:284-285). */
	public boolean isInput(byte aSide) {return aSide == mFacing;}
	public boolean isOutput(byte aSide) {return aSide == Direction.from3DDataValue(mFacing).getOpposite().get3DDataValue();}

	// ---------------------------------------------------------------------------
	// NBT (:68-97 — the link position persists, the link pair does not)
	// ---------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putBoolean(NBT_ACTIVE, mActive); // :95
		aNBT.putBoolean(NBT_STOPPED, mStopped); // :96
		aNBT.putByte("facing", mFacing);
		if (mTargetPos != null && mTarget != this) { // :86-93 — the live link persists with its metrics
			aNBT.putBoolean(NBT_TARGET, true);
			aNBT.putLong(NBT_TARGET_POS, mTargetPos.asLong()); // the packed position (the bindInt triple folded)
			aNBT.putLong(NBT_DISTANCE, mDistance); // :91
			aNBT.putLong(NBT_THROUGHPUT, mThroughput); // :92
		}
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_STOPPED, Tag.TAG_ANY_NUMERIC)) mStopped = aNBT.getBoolean(NBT_STOPPED); // :71
		if (aNBT.contains(NBT_ACTIVE, Tag.TAG_ANY_NUMERIC)) mActive = aNBT.getBoolean(NBT_ACTIVE); // :72
		if (aNBT.contains(NBT_TARGET, Tag.TAG_ANY_NUMERIC) && aNBT.contains(NBT_TARGET_POS, Tag.TAG_ANY_NUMERIC)) {
			mTargetPos = BlockPos.of(aNBT.getLong(NBT_TARGET_POS));
		}
		if (aNBT.contains(NBT_DISTANCE, Tag.TAG_ANY_NUMERIC)) mDistance = aNBT.getLong(NBT_DISTANCE); // :76
		if (aNBT.contains(NBT_THROUGHPUT, Tag.TAG_ANY_NUMERIC)) mThroughput = aNBT.getLong(NBT_THROUGHPUT); // :77
		if (aNBT.contains("facing", Tag.TAG_ANY_NUMERIC)) mFacing = aNBT.getByte("facing");
	}

	// the adjacency seam (the transformer D1 form — the offline override + the live walk)

	@Nullable
	private IEnergyAdjacency mAdjacencyOverride = null;

	/** The offline emit seam (the counting sink rides here — the target's BACK face). */
	void setAdjacencyOverride(@Nullable IEnergyAdjacency aAdjacency) {
		mAdjacencyOverride = aAdjacency;
	}

	/** The live neighbor walk for the emit arms (the GT6ElectricTransformerBlockEntity form). */
	IEnergyAdjacency adjacency() {
		if (mAdjacencyOverride != null) return mAdjacencyOverride;
		return aSide -> {
			if (!hasLevel()) return null;
			BlockEntity tNeighbor = getLevel().getBlockEntity(getBlockPos().relative(Direction.from3DDataValue(aSide)));
			if (tNeighbor == null || tNeighbor.isRemoved()) return null;
			byte tOpposite = (byte) Direction.from3DDataValue(aSide).getOpposite().get3DDataValue();
			return new gregapi.tileentity.energy.EnergyTarget(tNeighbor, tOpposite);
		};
	}

	// the offline rig seams (package-visible, the transformer setAdjacencyOverride form)

	/** The offline target injection (the level-less delegate rig). */
	void setTargetOverride(@Nullable GT6LongDistanceTransformerBlockEntity aTarget) {
		mTargetOverride = aTarget;
		mOfflineScan = aTarget != null;
	}

	@Override
	public String toString() {
		return "LDTrans@" + (hasLevel() ? getBlockPos() : "offline") + (mStopped ? " stopped" : "");
	}
}
