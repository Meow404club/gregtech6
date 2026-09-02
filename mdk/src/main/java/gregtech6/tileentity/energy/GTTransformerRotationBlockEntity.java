package gregtech6.tileentity.energy;

import java.util.Collection;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.EnergyTarget;
import gregapi.tileentity.energy.IEnergyAdjacency;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.block.energy.GTTransformerRotationBlock;
import gregtech6.registry.GTBlockEntities;
import gregtech6.registry.GT6Kinetics;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * 1.20.1 counterpart of the GT6 Transformer Gearbox — task p12-gearbox-transformer
 * spec 2, ported from
 * gregtech/tileentity/energy/transformers/MultiTileEntityTransformerRotation.java
 * (:34-67) over the Base10EnergyConverter / Base11Bidirectional converter shape
 * (TileEntityBase10EnergyConverter.java:45-180, TileEntityBase11Bidirectional.java:40-95,
 * TE_Behavior_Energy_Converter.doConversion): the RU→RU torque transformer with
 * NBT_MULTIPLIER = 4 — speed ÷ 4, power × 4 (the wood row 8→2, the bronze row 32→8,
 * Loader :1668/:1677), |speed × power| conserved exactly.
 *
 * <p>The conversion (upstream doConversion :120-127 over the converter :63-89, the wood
 * row numbers): the input packet ({@code |size| ≤ 16}, the stats max = tInput × 2) fills
 * the capacitor (capacity = tInput × 2 = 16, Stats.doInject:63-66 form); the tick converts
 * {@code tOutput = units(storage, inRec, outRec) = storage × 2/8} and emits ONE burst of
 * {@code MULTIPLIER} packets of size {@code tOutput} out of the back face
 * (emitEnergyToNetwork(RU, ±tOutput, 4)); the drain books {@code consumed × tOutput}
 * back out (units(consumed × tOut, outRec × mult = 8, inRec = 8) — the 8|8 identity).
 * So one 16×1 input becomes one 4×4 output burst: ÷4 speed, ×4 power.
 *
 * <p>Direction (upstream :131 {@code mNegativeInput = (aSize < 0)} + the converter
 * aNegative leg): RU ∈ ALL_NEGATIVE_ALLOWED (TD.java:205 — the gate :121 is live), so a
 * negative (counterclockwise) input emits a NEGATIVE output burst — the sign is
 * preserved, the ÷4×4 pair rides it.
 *
 * <p>Faces (the rotation transformer's own override, MultiTileEntityTransformerRotation
 * :42-43): the FRONT face ({@code mFacing}) is the only input, the BACK face the only
 * output (the electric transformer's any-but-front input family is not this machine).
 * The band (the readEnergyBehavior :73-78 wood row): input min 1 / rec 8 / max 16,
 * output min 1 / rec 2 / max 4.
 *
 * <p>Overload (upstream Stats.doInject:56-62 + Base10EnergyConverter.overload :140-148):
 * a packet above the input max consumes fully and strikes the explosion-prevention
 * counter — below 100 strikes the capacitor just clears (the upstream behavior for the
 * first 100 strikes, ported verbatim; the overcharge explosion itself is the explosion
 * family pool).
 *
 * <p>Cropped with declaration: {@code mReversed} (the monkey-wrench reversal, Base11Bidirectional
 * :80-88 — the tool toggle is the pool card, the field has no channel in this port), the
 * mMode throttle, the :111 client sound, the activity visual byte, and the paint half.
 * The monkey-wrench-facing and the wrench gear installation ride the pool with the gearbox.
 */
public class GTTransformerRotationBlockEntity extends TileEntityBase03TicksAndSync implements ITileEntityEnergy {

	/** The upstream NBT keys (CS.java:1241) + the capacitor carrier (the TE_Behavior stack is not ported). */
	public static final String NBT_STOPPED = "gt.stopped";
	public static final String NBT_CAPACITOR = "gt.capacitor";

	/** The wood row (Loader :1668): NBT_INPUT = V[0] = 8, NBT_OUTPUT = 2, NBT_MULTIPLIER = 4. */
	public static final long INPUT_SPEED = GT6Kinetics.TRANSFORMER_INPUT_SPEED;
	public static final long OUTPUT_SPEED = GT6Kinetics.TRANSFORMER_OUTPUT_SPEED;
	public static final long MULTIPLIER = GT6Kinetics.TRANSFORMER_MULTIPLIER;

	/** The stats bands (readEnergyBehavior :76-77 with tInput = 8 ≤ 16 → in min 1; out min = outRec/2). */
	public static final long INPUT_SIZE_MIN = 1, INPUT_SIZE_REC = INPUT_SPEED, INPUT_SIZE_MAX = INPUT_SPEED * 2;
	public static final long OUTPUT_SIZE_MIN = OUTPUT_SPEED / 2, OUTPUT_SIZE_REC = OUTPUT_SPEED, OUTPUT_SIZE_MAX = OUTPUT_SPEED * 2;

	/** The capacitor capacity (readEnergyBehavior :75 = tInput × 2). */
	public static final long STORAGE_CAPACITY = INPUT_SPEED * 2;

	/** The capacitor energy in input-size units (upstream mStorage.mEnergy). */
	public long mStorage = 0;

	/** The soft-hammer stop (upstream mStopped :46; no toggle channel in this port — the pool). */
	public boolean mStopped = false;

	/** The last packet's sign (upstream mNegativeInput :46, the :131 doInject write). */
	public boolean mNegativeInput = false;

	/** The explosion-prevention strikes (upstream mExplosionPrevention :47, the overload :140-148 form). */
	public int mExplosionPrevention = 0;

	/** The activity readout (the mActivity trinary collapsed to the emitted-last-conversion flag). */
	public boolean mActive = false;

	/** The last accepted input / last emitted burst, the /gt6engine stat readout (the ÷4×4 assertion channel). */
	public long mLastInSize = 0, mLastInAmount = 0, mLastOutSize = 0, mLastOutAmount = 0;

	/** The BE runtime facing mirror (byte, the GT6 side order == Direction.get3DDataValue; the crank form). */
	public byte mFacing = 2; // NORTH — the FRONT face, the only input

	/** BET factory for BlockEntityType.Builder.of — resolves the shared type through the registry at runtime. */
	public GTTransformerRotationBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/**
	 * Full constructor — also the offline (test) entry point: a null type falls back to
	 * the shared registry type at runtime, tests pass an offline-built BET.
	 */
	public GTTransformerRotationBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(true, aType != null ? aType : GTBlockEntities.TRANSFORMER_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "transformer_rotation"; // BET registry path mirrors it (GTBlockEntities.TRANSFORMER_BE)
	}

	// ---------------------------------------------------------------------------
	// the faces (MultiTileEntityTransformerRotation :42-43 + the crank facing mirror)
	// ---------------------------------------------------------------------------

	/** The input face (upstream isInput :42, the non-reversed form). */
	public boolean isInput(byte aSide) {
		return aSide == mFacing;
	}

	/** The output face (upstream isOutput :43 — the opposite of the front). */
	public boolean isOutput(byte aSide) {
		return aSide == opposite(mFacing);
	}

	/** The GT6 side order opposite (CS OPOS; byte side == Direction.get3DDataValue). */
	public static byte opposite(byte aSide) {
		return (byte) Direction.from3DDataValue(aSide).getOpposite().get3DDataValue();
	}

	/**
	 * The /setblock RCON path: the state is the authority, the BE re-syncs at each tick
	 * head (the crank syncFacingFromState form, keyed on the PROPERTY).
	 */
	void syncFacingFromState() {
		if (getBlockState().hasProperty(GTTransformerRotationBlock.FACING)) {
			mFacing = (byte) getBlockState().getValue(GTTransformerRotationBlock.FACING).get3DDataValue();
		}
	}

	// ---------------------------------------------------------------------------
	// the tick (Base10EnergyConverter.onTick2 :106-113 + doConversion :120-127)
	// ---------------------------------------------------------------------------

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		if (!aIsServerSide) return; // the :110 client branch is the sound pool
		syncFacingFromState();
		doConversion();
	}

	/**
	 * The per-tick conversion (upstream doConversion :120-127 over the converter
	 * doConversion :63-89, the size-carrying RU branch): tOutput from the capacitor, ONE
	 * emitEnergyToNetwork burst of MULTIPLIER packets, the drain books the consumed
	 * energy back out. tOutput > outMax is unreachable by construction (cap 16 →
	 * tOutput ≤ 4 = outMax), so the upstream mLimitConsumption/explosion leg of the
	 * converter :78-84 never fires — declared.
	 */
	void doConversion() {
		long tOutput = mStorage * OUTPUT_SPEED / INPUT_SPEED; // units(storage, inRec=8, outRec=2, roundDown) — 8|2 divides
		boolean tCanEmit = tOutput >= OUTPUT_SIZE_MIN; // the converter :65 gate
		mActive = false;
		if (tCanEmit) {
			// the converter :87-88 emit — RU is negative-allowed (TD.java:205) so the
			// aNegative leg (Base10EnergyConverter :121) signs the burst
			long tSign = mNegativeInput ? -1 : 1;
			long tEmitted = ITileEntityEnergy.Util.emitEnergyToNetwork(TD.Energy.RU, tSign * tOutput, MULTIPLIER, this, adjacency());
			if (tEmitted > 0) {
				// the converter :90-92 drain — units(consumed × tOut, outRec × mult = 8, inRec = 8) = consumed × tOut
				mStorage -= tEmitted * tOutput;
				mActive = true;
				mLastOutSize = tSign * tOutput;
				mLastOutAmount = tEmitted;
			}
			// a REFUSED burst (the consumer's queue full) keeps the last successful
			// records — "last out" means the last EMITTED burst, not this tick's
		}
	}

	// ---------------------------------------------------------------------------
	// the input (Base10EnergyConverter.doInject :130-138 + Stats.doInject :56-66)
	// ---------------------------------------------------------------------------

	@Override
	public long doInject(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
		if (aSize == 0 || !isEnergyAcceptingFrom(aEnergyType, aSide, false)) return 0;
		if (aDoInject) mNegativeInput = (aSize < 0); // upstream :131
		long tAbs = Math.abs(aSize);
		if (tAbs > INPUT_SIZE_MAX) { // the Stats.doInject oversize leg (:57-62)
			if (aDoInject) overload();
			return aAmount;
		}
		if (mStorage >= STORAGE_CAPACITY) return 0; // the Stats full gate (:63)
		long tEnergy = Math.min(STORAGE_CAPACITY - mStorage, tAbs * aAmount); // Stats :64
		long tConsumed = Math.min(aAmount, (tEnergy / tAbs) + (tEnergy % tAbs != 0 ? 1 : 0)); // Stats :64
		if (aDoInject) {
			mStorage += tConsumed * tAbs; // Stats :65
			mLastInSize = aSize;
			mLastInAmount = tConsumed;
		}
		return tConsumed;
	}

	/**
	 * The overload (upstream Base10EnergyConverter.overload :140-148, the first-100
	 * strikes form verbatim): clear the capacitor, count the strike. The overcharge
	 * explosion after 100 strikes is the explosion family pool.
	 */
	private void overload() {
		if (mExplosionPrevention < 100) {
			mExplosionPrevention++;
			mStorage = 0;
		}
		// the upstream overcharge(aSize, aEnergyType) arm :146 — the explosion pool
	}

	// ---------------------------------------------------------------------------
	// the adjacency seam (the crank/rig D1 form)
	// ---------------------------------------------------------------------------

	/** The offline test seam (the rig/crank mAdjacencyOverride form). */
	private IEnergyAdjacency mAdjacencyOverride = null;

	void setAdjacencyOverride(@Nullable IEnergyAdjacency aAdjacency) {
		mAdjacencyOverride = aAdjacency;
	}

	private IEnergyAdjacency adjacency() {
		if (mAdjacencyOverride != null) return mAdjacencyOverride;
		return aSide -> {
			if (!hasLevel()) return null;
			BlockEntity tNeighbor = getLevel().getBlockEntity(getBlockPos().relative(Direction.from3DDataValue(aSide)));
			if (tNeighbor == null || tNeighbor.isRemoved()) return null;
			byte tOpposite = (byte) Direction.from3DDataValue(aSide).getOpposite().get3DDataValue();
			return new EnergyTarget(tNeighbor, tOpposite);
		};
	}

	// ---------------------------------------------------------------------------
	// the energy face family (Base10EnergyConverter :150-159 + the row bands)
	// ---------------------------------------------------------------------------

	@Override
	public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
		return aEnergyType == TD.Energy.RU; // upstream :150 (both converter halves are RU)
	}

	@Override
	public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {
		// upstream :151 — the row carries NBT_WASTE_ENERGY = T (Loader :1668), so the
		// (mWasteEnergy || ...) middle term is constant true; mReversed is cropped
		return (aTheoretical || !mStopped) && isInput(aSide) && isEnergyType(aEnergyType, aSide, false);
	}

	@Override
	public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {
		return isOutput(aSide) && isEnergyType(aEnergyType, aSide, true); // upstream :152
	}

	@Override
	public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {
		return INPUT_SIZE_MIN; // the Stats band (readEnergyBehavior :76 with tInput = 8 ≤ 16)
	}

	@Override
	public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {
		return INPUT_SIZE_REC; // upstream :156
	}

	@Override
	public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {
		return INPUT_SIZE_MAX; // upstream :157
	}

	@Override
	public long getEnergySizeOutputMin(TagData aEnergyType, byte aSide) {
		return OUTPUT_SIZE_MIN; // upstream :153
	}

	@Override
	public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {
		return OUTPUT_SIZE_REC; // upstream :154
	}

	@Override
	public long getEnergySizeOutputMax(TagData aEnergyType, byte aSide) {
		return OUTPUT_SIZE_MAX; // upstream :155
	}

	@Override
	public Collection<TagData> getEnergyTypes(byte aSide) {
		return TD.Energy.RU.AS_LIST; // upstream :159 (both halves the same type)
	}

	// pull-based extraction stays the Root default: the gate → doExtract → 0
	// (this machine has no extract surface, upstream Base10EnergyConverter overrides none)

	public byte getFacing() {
		return mFacing;
	}

	// ---------------------------------------------------------------------------
	// NBT (upstream writeToNBT2 :46-49 + the capacitor carrier)
	// ---------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putBoolean(NBT_STOPPED, mStopped); // upstream :48
		aNBT.putLong(NBT_CAPACITOR, mStorage);
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_STOPPED, Tag.TAG_ANY_NUMERIC)) mStopped = aNBT.getBoolean(NBT_STOPPED); // upstream :53
		if (aNBT.contains(NBT_CAPACITOR, Tag.TAG_ANY_NUMERIC)) mStorage = Math.max(0, aNBT.getLong(NBT_CAPACITOR));
	}
}
