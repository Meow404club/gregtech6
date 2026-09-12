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

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.EnergyTarget;
import gregapi.tileentity.energy.IEnergyAdjacency;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.block.energy.GT6DynamoBlock;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The Dynamo family shared conversion core (task p28-c-dynamo-family-be) — the 1.20.1
 * counterpart of {@code TileEntityBase10EnergyConverter} (:45-180) ridden through
 * {@code TE_Behavior_Energy_Converter.doConversion} (:61-94) and
 * {@code TE_Behavior_Energy_Stats.doInject} (:56-66), shared verbatim by both families
 * (the wave-card W1 ruling: two BEs, one core; the TE_Behavior stack is not ported — the
 * GTTransformerRotationBlockEntity precedent carries the same fields flat). The Flux BE
 * (RU→FE) and the Electric BE (RU→EU) differ ONLY in the {@link #emitConverted} arm and
 * the output type/rows.
 *
 * <h2>The bearing semantics (the ratio correctness wall — copied, not redesigned)</h2>
 *
 * <ul>
 * <li><b>Capacitor</b> = NBT_INPUT × 2 (Base10:75 {@code tInput * 2}), carried in RU
 *     units like upstream.</li>
 * <li><b>Input gate</b> (Stats.doInject :56-66): a packet |size| &gt; 2×NBT_INPUT consumes
 *     the WHOLE offer and strikes the overload ladder (Base10:133-136); a full capacitor
 *     returns 0 — the axle-side original-amount refund (Stats:62-63); else whole packets
 *     enter until the capacitor is full (partial COUNTS, never partial packets).</li>
 * <li><b>Overload ladder</b> (Base10:140-148 verbatim): the first 100 strikes just clear
 *     the capacitor (the startup grace — {@code mExplosionPrevention}), strike 101+ arms
 *     {@code overcharge} (the Root :330 explosion family).</li>
 * <li><b>White burn</b>: a packet below the input minimum (NBT_INPUT/2, Base10:76 with
 *     {@code takesAnyLowerSize() = F} and tInput &gt; 16) is swallowed by the Root gate
 *     (EnergyGate.gateInjection → returns aAmount, doInject never runs — the offered
 *     rotation is consumed for nothing, Root:717 verbatim).</li>
 * <li><b>WASTE_ENERGY = T</b> (the Loader rows :946-957, both families): the TWO arms of
 *     Converter:81/:92 — (1) the emit does NOT deduct the capacitor (the :81/:87
 *     {@code !mWasteEnergy} deductions are skipped — the input side already paid), and
 *     (2) EVERY tick the capacitor vents {@code units(mEnergyIN.mMax = 2×NBT_INPUT, 16,
 *     16 - aMode = 16, T)} = 2×NBT_INPUT (the :92 tail; mMode is the constant 0 of this
 *     port — the transformer's crop, declared). The capacitor is a bucket emptied and
 *     refilled every tick: idle or loaded, whatever flowed in burns at the tick end.
 *     Steady-state throughput ≤ 2×NBT_INPUT RU/t → × the output ratio; the ratio itself
 *     lives in the registration constants (NBT_OUTPUT/NBT_INPUT = 88/32 = 2.75 exactly for
 *     Flux, 22/32 = 0.6875 for Electric), NOT in this core.</li>
 * <li><b>The tOutput door</b> (Converter:62/:64): {@code tOutput = units(storage, inRec,
 *     outRec, F)} — the upstream {@code UT.Code.units} direction (UT.java:1682, floor),
 *     reproduced verbatim below since the root port never needed it before. Emit requires
 *     {@code tOutput >= mMin = NBT_OUTPUT/2} (:64). {@code tOutput > mMax} is UNREACHABLE
 *     by construction (storage ≤ 2×inRec ⇒ tOutput ≤ 2×outRec = outMax, equality at the
 *     full capacitor), so the :68-77 consumption-limit/explosion leg never fires —
 *     declared, the transformer's identical declaration.</li>
 * </ul>
 *
 * <h2>Cropped with declaration</h2>
 * The mMode redstone throttle (the :63 cap — constant 0), the multiplier (1),
 * {@code mSizeIrrelevant} per-family handling lives in the Flux arm, the client minecart
 * sound (:111), the paint half, the adjacency ON/OFF toggle channel ({@code mStopped}
 * is kept with its :151 acceptance formula — the toggle interface itself is the pool,
 * the transformer's identical crop).
 */
public abstract class GT6DynamoBlockEntity extends TileEntityBase03TicksAndSync implements ITileEntityEnergy {

	/** The upstream NBT keys (the transformer carriers; CS.NBT_* stay upstream-named). */
	public static final String NBT_STOPPED = "gt.stopped";
	public static final String NBT_CAPACITOR = "gt.capacitor";

	/** WASTE_ENERGY = T on every dynamo row (Loader_MultiTileEntities.java:946-957). */
	public static final boolean WASTE_ENERGY = true;

	/** The row's NBT_INPUT column (32/128/512/2048/8192 RU, Loader :946-957) — T1 default, the block tier rewrites it. */
	public long mInput = 32;

	/** The row's NBT_OUTPUT column (Flux 88/352/1408/5632/22528 FE, Electric 22/88/352/1408/5632 EU) — T1 default. */
	public long mOutput;

	/** The capacitor energy in RU units (upstream mStorage.mEnergy). */
	public long mStorage = 0;

	/** The soft-hammer stop (upstream mStopped :46; the toggle channel is the pool). */
	public boolean mStopped = false;

	/** The last packet's sign (upstream mNegativeInput :46, the :131 doInject write). */
	public boolean mNegativeInput = false;

	/** The explosion-prevention strikes (upstream mExplosionPrevention :47, the :140-148 ladder). */
	public int mExplosionPrevention = 0;

	/** Emitted on the last conversion (upstream mConverter.mEmitsEnergy :66/:82/:88). */
	public boolean mActive = false;

	/** The :64 door readout (upstream mConverter.mCanEmitEnergy). */
	public boolean mCanEmitEnergy = false;

	/** The BE runtime facing mirror (byte, the GT6 side order == Direction.get3DDataValue; the transformer form). FRONT = output. */
	public byte mFacing = 2; // NORTH

	/** BET factory for BlockEntityType.Builder.of — resolves the shared type through the registry at runtime. */
	protected GT6DynamoBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(true, aType, aPos, aState);
	}

	/** Applies the family ladder row of {@code aState} (the TileEntityOven.applyTierInputs static-seam shape). */
	protected void applyTier(BlockState aState, long[] aInputs, long[] aOutputs) {
		int tTier = GT6DynamoBlock.tier(aState);
		mInput = aInputs[tTier];
		mOutput = aOutputs[tTier];
	}

	/** The offline test seam: drives the row columns directly (a Block is unconstructible offline, the intrusive-holder wall). */
	public void tierForOfflineTest(long aInput, long aOutput) {
		mInput = aInput;
		mOutput = aOutput;
	}

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		if (!aIsServerSide) return; // the :110 client branch is the minecart-sound pool
		syncFacingFromState();
		doConversion(aTimer);
	}

	/** The /setblock RCON path: the state is the authority (the transformer syncFacingFromState form). */
	void syncFacingFromState() {
		if (getBlockState().hasProperty(GT6DynamoBlock.FACING)) {
			mFacing = (byte) getBlockState().getValue(GT6DynamoBlock.FACING).get3DDataValue();
		}
	}

	// ---------------------------------------------------------------------------
	// the conversion (TE_Behavior_Energy_Converter.doConversion :61-94, the waste=T /
	// mMode=0 / multiplier=1 constant-folded shape)
	// ---------------------------------------------------------------------------

	/**
	 * The per-tick conversion. The output arm is the family hook; the door, the activity
	 * readout and the waste burn are this core's, verbatim.
	 */
	void doConversion(long aTimer) {
		long tOutput = units(mStorage, mInput, mOutput, false); // :62 — the UT.Code.units floor direction
		mCanEmitEnergy = tOutput >= mOutput / 2; // :64 (mMin = outRec/2, Base10:77 with emitsAnyLowerSize() = F)
		mActive = false; // :66
		if (mCanEmitEnergy) {
			// the :68-77 leg is unreachable: storage ≤ 2×mInput ⇒ tOutput ≤ 2×mOutput = outMax
			// (equality at the full capacitor) — declared in the class doc
			long tUsed = emitConverted(tOutput, mNegativeInput && negativeOutputAllowed()); // :79/:85 (aNegative = Base10:121)
			if (tUsed > 0) mActive = true; // :82/:88 — waste=T: NO capacitor deduction (the :81/:87 arms skipped)
		}
		// the :92 tail, aMode = 0: units(2×inRec, 16, 16, T) = 2×mInput exactly — the vent
		// clears the whole bucket every tick, idle or loaded (the funnel semantics)
		if (WASTE_ENERGY) mStorage = Math.max(0, mStorage - units(mInput * 2, 16, 16, true));
	}

	/**
	 * The family output arm — the :78-90 branches split here. Returns the used amount in
	 * the family's own packet unit (&gt; 0 = something accepted, the mActive sense).
	 */
	protected abstract long emitConverted(long tOutput, boolean aNegative);

	/** Whether the output type rides ALL_NEGATIVE_ALLOWED (the Base10:121 second conjunct; the input conjunct RU is always in). */
	protected abstract boolean negativeOutputAllowed();

	/** The output energy type (TD.Energy.RF for Flux, TD.Energy.EU for Electric). */
	protected abstract TagData outputType();

	/**
	 * Upstream {@code UT.Code.units} (UT.java:1682 verbatim) — the root port never needed
	 * it before this card; reproduced here (the wave-card wall: copy the direction, never
	 * reinvent the remainder behavior).
	 */
	static long units(long aAmount, long aOriginalUnit, long aTargetUnit, boolean aRoundUp) {
		if (aTargetUnit == 0) return 0;
		if (aOriginalUnit == aTargetUnit || aOriginalUnit == 0) return aAmount;
		if (aOriginalUnit %   aTargetUnit == 0) {aOriginalUnit /=   aTargetUnit;   aTargetUnit = 1;} else
		if (aTargetUnit   % aOriginalUnit == 0) {  aTargetUnit /= aOriginalUnit; aOriginalUnit = 1;}
		return Math.max(0, ((aAmount * aTargetUnit) / aOriginalUnit) + (aRoundUp && (aAmount * aTargetUnit) % aOriginalUnit > 0 ? 1 : 0));
	}

	// ---------------------------------------------------------------------------
	// the input (Base10EnergyConverter.doInject :130-138 + Stats.doInject :56-66)
	// ---------------------------------------------------------------------------

	@Override
	public long doInject(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
		if (aSize == 0 || !isEnergyAcceptingFrom(aEnergyType, aSide, false)) return 0;
		if (aDoInject) mNegativeInput = (aSize < 0); // :131
		long tAbs = Math.abs(aSize);
		if (tAbs > mInput * 2) { // the Stats oversize leg :57-61 (mMax = 2×inRec) — consumes ALL
			if (aDoInject) overload(tAbs, aEnergyType); // Base10:133-136
			return aAmount;
		}
		if (mStorage >= capacity()) return 0; // Stats :62 — the full gate: 0, the axle refunds (Stats:62-63)
		long tEnergy = Math.min(capacity() - mStorage, tAbs * aAmount); // Stats :63
		long tConsumed = Math.min(aAmount, (tEnergy / tAbs) + (tEnergy % tAbs != 0 ? 1 : 0)); // Stats :63
		if (aDoInject) mStorage += tConsumed * tAbs; // Stats :64
		return tConsumed;
	}

	/** The overload ladder (Base10EnergyConverter.overload :140-148 verbatim): 100 soft strikes, then overcharge. */
	private void overload(long aSize, TagData aEnergyType) {
		if (mExplosionPrevention < 100) {
			mExplosionPrevention++;
			mStorage = 0;
		} else {
			overcharge(aSize, aEnergyType); // the Root :330 explosion family (offline: log-only)
		}
	}

	/** The capacitor capacity = NBT_INPUT × 2 (Base10:75). */
	public long capacity() {
		return mInput * 2;
	}

	// ---------------------------------------------------------------------------
	// the faces (Base10EnergyConverter :150-159, DynamoFlux :36-39 isInput/isOutput)
	// ---------------------------------------------------------------------------

	/** The input face — the BACK (upstream DynamoFlux :36: {@code mFacing == OPOS[aSide]}). */
	public boolean isInput(byte aSide) {
		return aSide == opposite(mFacing);
	}

	/** The output face — the FRONT (upstream DynamoFlux :37: {@code mFacing == aSide}). */
	public boolean isOutput(byte aSide) {
		return aSide == mFacing;
	}

	/** The GT6 side order opposite (CS OPOS; byte side == Direction.get3DDataValue; the transformer form). */
	public static byte opposite(byte aSide) {
		return (byte) Direction.from3DDataValue(aSide).getOpposite().get3DDataValue();
	}

	@Override
	public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
		// upstream :150: (aEmitting ? mEnergyOUT : mEnergyIN).isType(aEnergyType)
		return aEmitting ? aEnergyType == outputType() : aEnergyType == TD.Energy.RU;
	}

	@Override
	public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {
		// upstream :151 with the waste=T middle term folded: (aTheoretical || !mStopped)
		return (aTheoretical || !mStopped) && isInput(aSide) && isEnergyType(aEnergyType, aSide, false);
	}

	@Override
	public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {
		return isOutput(aSide) && isEnergyType(aEnergyType, aSide, true); // upstream :152
	}

	// the size bands, type-guarded like the upstream Stats.sizeMin/Rec/Max (:46-48 — the
	// wrong type answers 0). Input band (Base10:76): min = in/2 (tInput > 16 every row,
	// takesAnyLowerSize() = F), rec = in, max = 2in. Output band (Base10:77): min = out/2.

	@Override
	public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {
		return aEnergyType == TD.Energy.RU ? mInput / 2 : 0;
	}

	@Override
	public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {
		return aEnergyType == TD.Energy.RU ? mInput : 0;
	}

	@Override
	public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {
		return aEnergyType == TD.Energy.RU ? mInput * 2 : 0;
	}

	@Override
	public long getEnergySizeOutputMin(TagData aEnergyType, byte aSide) {
		return aEnergyType == outputType() ? mOutput / 2 : 0;
	}

	@Override
	public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {
		return aEnergyType == outputType() ? mOutput : 0;
	}

	@Override
	public long getEnergySizeOutputMax(TagData aEnergyType, byte aSide) {
		return aEnergyType == outputType() ? mOutput * 2 : 0;
	}

	@Override
	public Collection<TagData> getEnergyTypes(byte aSide) {
		// upstream :159 — both converter halves (ArrayListNoNulls(F, mEnergyIN.mType, mEnergyOUT.mType))
		return java.util.Arrays.asList(TD.Energy.RU, outputType());
	}

	// pull-based extraction stays the Root default: the gate → doExtract → 0
	// (the dynamo is a pure push source — the upstream doExtract-never-overridden posture,
	// research.p28-r-flux-dynamo: the CoFH pull face answered 0 and the port keeps that
	// faithfulness on the modern legs)

	public byte getFacing() {
		return mFacing;
	}

	// ---------------------------------------------------------------------------
	// the adjacency seam (the transformer/FE-converter D1 form)
	// ---------------------------------------------------------------------------

	/** The offline test seam (the rig/crank mAdjacencyOverride form) — the EU/emit side. */
	private IEnergyAdjacency mAdjacencyOverride = null;

	void setAdjacencyOverride(@Nullable IEnergyAdjacency aAdjacency) {
		mAdjacencyOverride = aAdjacency;
	}

	protected IEnergyAdjacency adjacency() {
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
	// NBT (the transformer carriers: the capacitor + the stop)
	// ---------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putBoolean(NBT_STOPPED, mStopped);
		aNBT.putLong(NBT_CAPACITOR, mStorage);
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_STOPPED, Tag.TAG_ANY_NUMERIC)) mStopped = aNBT.getBoolean(NBT_STOPPED);
		if (aNBT.contains(NBT_CAPACITOR, Tag.TAG_ANY_NUMERIC)) mStorage = Math.max(0, aNBT.getLong(NBT_CAPACITOR));
	}
}
