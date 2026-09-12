package gregtech6.tileentity.energy;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
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
import gregtech6.registry.GT6ElectricTransformers;
import gregtech6.registry.GTWireSpecs;
import gregtech6.block.energy.GT6ElectricTransformerBlock;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

/**
 * The Electric Transformer ULV-LV (task p28-c-ulv-lv-transformer) — the 1.20.1
 * counterpart of {@code MultiTileEntityTransformerElectric} over the bidirectional
 * base semantics ({@code TileEntityBase11Bidirectional} :40-95, the row
 * Loader_MultiTileEntities.java:881: {@code NBT_INPUT, V[1]=32, NBT_OUTPUT, V[0]=8,
 * NBT_MULTIPLIER, 4, NBT_WASTE_ENERGY, F}). THE FIRST EU voltage transformer of the
 * port — the p12 rotation transformer is the RU kinetic twin (÷4×4 speed/torque), this
 * is the EU packet transformer (×4/÷4 packet size/amps).
 *
 * <h2>The two modes (Base11 :41 {@code mReversed} + the :52-61 converter swap)</h2>
 * Upstream builds TWO converters over ONE shared capacitor ({@code mStorage =
 * tInput*2} = 64, Base10 :75) and the monkey wrench swaps them (Base11 :80-88, storage
 * cleared on the flip). Both converters of the :881 row keep {@code inRec = 32}:
 * <ul>
 * <li><b>Normal = step-down LV→ULV</b> (the upstream default, the regression anchor):
 * in band min 16 / rec 32 / max 64 (Base10 :76 — {@code tInput = 32 > 16},
 * {@code takesAnyLowerSize() = F}), out band min 4 / rec 8 / max 16 (:77). One 32 EU
 * packet in → {@code units(32, 32, 8) = 8} → up to {@code mMultiplier = 4} packets of
 * 8 EU out (:85: size {@code tOutput}, amount {@code mMultiplier}) = 32 EU out. The
 * deduction (:87) is the packets actually accepted × their EU — conservation.</li>
 * <li><b>Reversed = step-up ULV→LV</b> (Base11 :56-57 derived bands): in band min 1
 * ({@code outMin 4 <= 8} fold) / rec 32 / max 64, out band min 24
 * ({@code inRec*3/4}) / rec 32 / max 64. Each accepted 8 EU packet fills the
 * capacitor; {@code units(storage, 32, 32) = storage} ≥ 24 emits ONE packet of the
 * accumulated size (:85 with multiplier 1) — the canonical steady state 4×8 EU in →
 * 1×32 EU out is conservation, and the door never emits a sub-24 packet that would
 * bounce off an LV machine (min 16 accepted, GTMachines.TIER_INPUTS[0][0]).</li>
 * </ul>
 *
 * <p>The emit arm is the {@code :85} size-carrying branch (EU is NOT
 * size-irrelevant): one {@link ITileEntityEnergy.Util#emitEnergyToNetwork} push per
 * tick, packet size ±tOutput, packet count = multiplier (normal) / 1 (reversed); the
 * face predicates ({@link #isOutput}) gate the sides (Base11 :63-64 — normal: FRONT
 * in, ALL-BUT-FRONT out; reversed: the swap). WASTE_ENERGY = F on every transformer
 * row (Loader :881-889) — NO capacitor vent (the dynamo's :92 tail does not exist
 * here); the :68-77 overload leg is UNREACHABLE by construction (storage ≤ capacity
 * 64 = both modes' outMax, equality at the full capacitor — declared, the
 * GT6DynamoBlockEntity identical declaration).
 *
 * <h2>Cropped with declaration</h2>
 * The monkey-wrench/magnifying-glass tool interaction (Base11 :75-94) — the port has
 * no GT tool-click channel on this block; the mode flip is the NBT face
 * ({@code gt.reversed}, the /data merge RCON path — the W3 p28_ulv_chain arm drives
 * it) via {@link #toggleReversed()} with the :81 storage-clear kept verbatim. The
 * adjacency ON/OFF toggle channel (ITileEntityAdjacentOnOff, the dynamo's identical
 * crop) and the paint half are the pool. mStopped keeps its :151 acceptance formula
 * (the dynamo form); the activity visual rides the W2 render card (the trinary
 * collapsed to the {@link #mActive} flag).
 */
public class GT6ElectricTransformerBlockEntity extends TileEntityBase03TicksAndSync implements ITileEntityEnergy {

	/** The upstream NBT keys (CS.NBT_* stay upstream-named; the dynamo carrier form). */
	public static final String NBT_REVERSED = "gt.reversed";
	public static final String NBT_CAPACITOR = "gt.capacitor";
	public static final String NBT_STOPPED = "gt.stopped";

	/** The row's high side = NBT_INPUT = V[1] (Loader :881; CS.java:148 V table via GTWireSpecs). */
	public static final long VOLTAGE_HIGH = GTWireSpecs.V[1];

	/** The row's low side = NBT_OUTPUT = V[0] (Loader :881). */
	public static final long VOLTAGE_LOW = GTWireSpecs.V[0];

	/** NBT_MULTIPLIER = V[1]/V[0] = 4 (Loader :881) — the step-down packet count. */
	public static final long MULTIPLIER = VOLTAGE_HIGH / VOLTAGE_LOW;

	/** NBT_WASTE_ENERGY = F (Loader :881-889) — the transformer stores, never vents. */
	public static final boolean WASTE_ENERGY = false;

	/** The shared capacitor = NBT_INPUT × 2 = 64 (Base10 :75) — both modes, one bucket. */
	public static final long CAPACITY = VOLTAGE_HIGH * 2;

	/** The input band max — 64 in BOTH modes (Base10 :76 {@code inRec*2}; Base11 :56 {@code max(inRec, outMax*mult) = max(32, 64)}). */
	public static final long INPUT_MAX = VOLTAGE_HIGH * 2;

	/** The step-down input min (Base10 :76: {@code tInput > 16}, {@code takesAnyLowerSize() = F}). */
	public static final long INPUT_MIN_DOWN = VOLTAGE_HIGH / 2;

	/** The step-down output min (Base10 :77). */
	public static final long OUTPUT_MIN_DOWN = VOLTAGE_LOW / 2;

	/** The step-up input min (Base11 :56: {@code mEnergyOUT.mMin = 4 <= 8 → 1}). */
	public static final long INPUT_MIN_UP = 1;

	/** The step-up output min (Base11 :57: {@code mEnergyIN.mRec * 3 / 4} = 24). */
	public static final long OUTPUT_MIN_UP = VOLTAGE_HIGH * 3 / 4;

	/** The capacitor energy in EU (upstream mStorage.mEnergy — shared by both converters). */
	public long mStorage = 0;

	/** The Base11 :41 mode flag — false = step-down (upstream default), true = step-up. */
	public boolean mReversed = false;

	/** The soft-hammer stop (the Base10 :46 field; the toggle channel is the pool). */
	public boolean mStopped = false;

	/** The last packet's sign (Base10 :46/:131). */
	public boolean mNegativeInput = false;

	/** The explosion-prevention strikes (Base10 :47, the :140-148 ladder). */
	public int mExplosionPrevention = 0;

	/** Emitted on the last conversion (the converter :66/:82/:88 mEmitsEnergy). */
	public boolean mActive = false;

	/** The :64 door readout (the converter mCanEmitEnergy). */
	public boolean mCanEmitEnergy = false;

	/** The BE runtime facing mirror (byte, the GT6 side order == Direction.get3DDataValue; the dynamo form). FRONT = input (normal mode). */
	public byte mFacing = 2; // NORTH

	/** BET factory for BlockEntityType.Builder.of — resolves the shared type through the registry at runtime. */
	public GT6ElectricTransformerBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — also the offline (test) entry point (the dual-constructor precedent). */
	public GT6ElectricTransformerBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(true, aType != null ? aType : GT6ElectricTransformers.ELECTRIC_TRANSFORMER_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "electric_transformer"; // the BET registry path mirrors it (GT6ElectricTransformers.ELECTRIC_TRANSFORMER_BE)
	}

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		if (!aIsServerSide) return; // the Base10 :110 client branch is the sound pool
		syncFacingFromState();
		if (!mStopped) doConversion();
	}

	/** The /setblock RCON path: the state is the authority (the dynamo syncFacingFromState form). */
	void syncFacingFromState() {
		if (getBlockState().hasProperty(GT6ElectricTransformerBlock.FACING)) {
			mFacing = (byte) getBlockState().getValue(GT6ElectricTransformerBlock.FACING).get3DDataValue();
		}
	}

	// ---------------------------------------------------------------------------
	// the conversion (TE_Behavior_Energy_Converter.doConversion :61-94, the waste=F /
	// mMode=0 shape — the two Base11 converters folded over ONE capacitor)
	// ---------------------------------------------------------------------------

	/**
	 * The per-tick conversion. {@code inRec = 32} in both modes (Base11 :56 — the
	 * reversed converter's inRec IS the row's mEnergyIN.mRec); the out rec/min and the
	 * packet count split on {@link #mReversed}.
	 */
	void doConversion() {
		long tOutRec = mReversed ? VOLTAGE_HIGH : VOLTAGE_LOW; // 32 : 8 (Base11 :57 rec / Base10 :77 rec)
		long tOutMin = mReversed ? OUTPUT_MIN_UP : OUTPUT_MIN_DOWN; // 24 : 4 (Base11 :57 / Base10 :77)
		long tPackets = mReversed ? 1 : MULTIPLIER; // the :85 amount argument (mMultiplier per mode)
		long tOutput = units(mStorage, VOLTAGE_HIGH, tOutRec, false); // :62 — the UT.Code.units floor direction
		mCanEmitEnergy = tOutput >= tOutMin; // :64
		mActive = false; // :66
		if (mCanEmitEnergy) {
			// the :68-77 leg is unreachable: storage ≤ CAPACITY 64 = both modes' outMax
			// (equality at the full capacitor) — declared in the class doc
			long tUsed = emitConverted(tOutput, tPackets); // :85 (size ±tOutput, amount tPackets)
			if (tUsed > 0) { // :82/:88 — waste=F: the :87 deduction RUNS (the dynamo's skip is waste=T only)
				mActive = true;
				// :87 verbatim: units(packetsAccepted × tOutput, outRec×mult, inRec, T) —
				// for this row outRec×mult == inRec == 32 in both modes, so the charge is
				// exactly the EU that left (conservation); kept in the general units() form
				// so a future ladder row with a different ratio inherits the upstream math
				mStorage -= units(tUsed * tOutput, tOutRec * tPackets, VOLTAGE_HIGH, true);
				if (mStorage < 0) mStorage = 0;
			}
		}
		// NO :92 tail — NBT_WASTE_ENERGY = F (Loader :881): the capacitor persists idle
	}

	/**
	 * The :85 emit arm — ONE push, packet size ±tOutput (mFactor = 1, the Base10
	 * aNegativeOutput = F), packet count = the mode's multiplier; the Util loops the
	 * sides, {@link #isEnergyEmittingTo} gates them (Base11 :64). Returns the accepted
	 * packet count.
	 */
	long emitConverted(long tOutput, long tPackets) {
		long tSign = mNegativeInput ? -1 : 1;
		return ITileEntityEnergy.Util.emitEnergyToNetwork(TD.Energy.EU, tSign * tOutput, tPackets, this, adjacency());
	}

	/**
	 * Upstream {@code UT.Code.units} (UT.java:1682 verbatim) — the
	 * {@link GT6DynamoBlockEntity#units} static (same package, shared verbatim — the
	 * wave-card rule: one units() per port, never a second remainder convention).
	 */
	static long units(long aAmount, long aOriginalUnit, long aTargetUnit, boolean aRoundUp) {
		return GT6DynamoBlockEntity.units(aAmount, aOriginalUnit, aTargetUnit, aRoundUp);
	}

	// ---------------------------------------------------------------------------
	// the input (Base10EnergyConverter.doInject :130-138 + Stats.doInject :56-66 —
	// the dynamo form with the row's bands)
	// ---------------------------------------------------------------------------

	@Override
	public long doInject(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
		if (aSize == 0 || !isEnergyAcceptingFrom(aEnergyType, aSide, false)) return 0;
		if (aDoInject) mNegativeInput = (aSize < 0); // :131
		long tAbs = Math.abs(aSize);
		if (tAbs > INPUT_MAX) { // the Stats oversize leg :57-61 (mMax = 64 BOTH modes) — consumes ALL
			if (aDoInject) overload(tAbs, aEnergyType); // Base10:133-136
			return aAmount;
		}
		if (mStorage >= capacity()) return 0; // Stats :62 — the full gate: 0
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

	/** The capacitor capacity = NBT_INPUT × 2 = 64 (Base10 :75). */
	public long capacity() {
		return CAPACITY;
	}

	// ---------------------------------------------------------------------------
	// the mode flip (Base11 :80-88 — the storage-clear kept verbatim)
	// ---------------------------------------------------------------------------

	/** The monkey-wrench mode flip minus the tool channel (see the crop note): clears the capacitor first. */
	public void toggleReversed() {
		mStorage = 0; // Base11 :81 — "less likely to cause overcharge when switching Modes by accident"
		mReversed = !mReversed;
	}

	// ---------------------------------------------------------------------------
	// the faces (Base11 :63-64 — normal: FRONT in / ALL-BUT-FRONT out; reversed: swap)
	// ---------------------------------------------------------------------------

	/** The input side set — the Base11 :63 predicate verbatim. */
	public boolean isInput(byte aSide) {
		return mReversed ? aSide != mFacing : aSide == mFacing;
	}

	/** The output side set — the Base11 :64 predicate verbatim. */
	public boolean isOutput(byte aSide) {
		return mReversed ? aSide == mFacing : aSide != mFacing;
	}

	/** The GT6 side order opposite (CS OPOS; byte side == Direction.get3DDataValue; the dynamo form). */
	public static byte opposite(byte aSide) {
		return (byte) net.minecraft.core.Direction.from3DDataValue(aSide).getOpposite().get3DDataValue();
	}

	@Override
	public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
		return aEnergyType == TD.Energy.EU; // NBT_ENERGY_ACCEPTED/EMITTED both EU (Loader :881)
	}

	@Override
	public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {
		// Base10 :151 verbatim: (aTheoretical || (!mStopped && (mWasteEnergy ||
		// (mEmitsEnergy == mCanEmitEnergy)))). WASTE_ENERGY = F here (Loader :881), so the
		// :151 throttle IS live — when the output net is plugged (canEmit but nothing
		// emitted last tick) the input side pauses, exactly upstream. The dynamo folded
		// this term away only because its rows are waste=T (the arm is constant-true
		// there); the transformer is the first waste=F converter of the port.
		return (aTheoretical || (!mStopped && (WASTE_ENERGY || (mActive == mCanEmitEnergy))))
				&& isInput(aSide) && isEnergyType(aEnergyType, aSide, false);
	}

	@Override
	public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {
		return isOutput(aSide) && isEnergyType(aEnergyType, aSide, true); // Base10 :152
	}

	// the size bands (Base10 :76-77 normal + Base11 :56-57 reversed; the wrong type
	// answers 0 like the upstream Stats.sizeMin/Rec/Max :46-48)

	@Override
	public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {
		return aEnergyType == TD.Energy.EU ? (mReversed ? INPUT_MIN_UP : INPUT_MIN_DOWN) : 0;
	}

	@Override
	public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {
		return aEnergyType == TD.Energy.EU ? VOLTAGE_HIGH : 0; // rec = inRec = 32 in BOTH modes
	}

	@Override
	public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {
		return aEnergyType == TD.Energy.EU ? INPUT_MAX : 0;
	}

	@Override
	public long getEnergySizeOutputMin(TagData aEnergyType, byte aSide) {
		return aEnergyType == TD.Energy.EU ? (mReversed ? OUTPUT_MIN_UP : OUTPUT_MIN_DOWN) : 0;
	}

	@Override
	public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {
		return aEnergyType == TD.Energy.EU ? (mReversed ? VOLTAGE_HIGH : VOLTAGE_LOW) : 0;
	}

	@Override
	public long getEnergySizeOutputMax(TagData aEnergyType, byte aSide) {
		return aEnergyType == TD.Energy.EU ? (mReversed ? VOLTAGE_HIGH * 2 : VOLTAGE_LOW * 2) : 0;
	}

	@Override
	public java.util.Collection<TagData> getEnergyTypes(byte aSide) {
		return java.util.Arrays.asList(TD.Energy.EU); // both converters EU (Base10 :159 dedup)
	}

	public byte getFacing() {
		return mFacing;
	}

	// ---------------------------------------------------------------------------
	// the adjacency seam (the dynamo D1 form — the offline test override + the live walk)
	// ---------------------------------------------------------------------------

	/** The offline test seam (the rig/crank mAdjacencyOverride form) — the EU emit side. */
	private IEnergyAdjacency mAdjacencyOverride = null;

	void setAdjacencyOverride(@Nullable IEnergyAdjacency aAdjacency) {
		mAdjacencyOverride = aAdjacency;
	}

	protected IEnergyAdjacency adjacency() {
		if (mAdjacencyOverride != null) return mAdjacencyOverride;
		return aSide -> {
			if (!hasLevel()) return null;
			BlockEntity tNeighbor = getLevel().getBlockEntity(getBlockPos().relative(net.minecraft.core.Direction.from3DDataValue(aSide)));
			if (tNeighbor == null || tNeighbor.isRemoved()) return null;
			byte tOpposite = (byte) net.minecraft.core.Direction.from3DDataValue(aSide).getOpposite().get3DDataValue();
			return new EnergyTarget(tNeighbor, tOpposite);
		};
	}

	// ---------------------------------------------------------------------------
	// NBT (the Base11 :46-49 reversed flag + the shared capacitor + the stop)
	// ---------------------------------------------------------------------------

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putBoolean(NBT_REVERSED, mReversed);
		aNBT.putBoolean(NBT_STOPPED, mStopped);
		aNBT.putLong(NBT_CAPACITOR, mStorage);
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_REVERSED, Tag.TAG_ANY_NUMERIC)) mReversed = aNBT.getBoolean(NBT_REVERSED);
		if (aNBT.contains(NBT_STOPPED, Tag.TAG_ANY_NUMERIC)) mStopped = aNBT.getBoolean(NBT_STOPPED);
		if (aNBT.contains(NBT_CAPACITOR, Tag.TAG_ANY_NUMERIC)) mStorage = Math.max(0, aNBT.getLong(NBT_CAPACITOR));
	}
}
