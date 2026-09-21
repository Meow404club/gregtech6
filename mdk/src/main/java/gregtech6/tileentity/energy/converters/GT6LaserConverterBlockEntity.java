package gregtech6.tileentity.energy.converters;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.IEnergyAdjacency;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.registry.GT6Lasers;
import gregtech6.tileentity.energy.GT6DynamoBlockEntity;

/**
 * The CO2 Laser + Laser Absorber conversion core (task p32-qu-laser-domain) — the two
 * 1.20.1 counterparts of {@code MultiTileEntityLaserElectric} (:38, EU→LU) and
 * {@code MultiTileEntityLaserAbsorberElectric} (:34, LU→EU), both riding the shared
 * {@link GT6DynamoBlockEntity} core (the TileEntityBase10EnergyConverter :45-180 port)
 * with BOTH energy arms re-typed (the ElectricBridgeBlockEntity one-arm re-type shape
 * generalized: the input type becomes the second family axis). ONE class over the family
 * BETs — the families differ ONLY in the type pair and the input face set:
 * <ul>
 * <li><b>CO2 Laser</b> (Loader_MultiTileEntities.java:930-934, ids 10101-10105): EU in /
 *     LU out, input = ALL-BUT-FRONT (Base10 :176 default), output = FRONT (:177).</li>
 * <li><b>Laser Absorber</b> (:976-980, ids 10151-10155): LU in / EU out, input = BACK
 *     (MultiTileEntityLaserAbsorberElectric :35 {@code mFacing == OPOS[aSide]}), output =
 *     FRONT (:36) — exactly the dynamo-core faces.</li>
 * <li><b>Quantum Energizer</b> (task p32-qu-energizer; Loader :961-966, ids 10121-10125):
 *     LU in / QU out, input = BACK / output = FRONT (MultiTileEntityQuantumEnergizerLaser
 *     :36-:37 — the absorber faces), the THIRD type-pair instance. The ladder columns are
 *     numerically the same shared rungs (the Loader energizer rows carry the identical
 *     in/out columns), so the tier read needs no change either — the family differs ONLY
 *     in the type pair and the registration constants (osmiridium 16.0, {@link
 *     gregtech6.registry.GT6QuantumEnergizers}).</li>
 * </ul>
 *
 * <p>Everything else is the core verbatim, and the row columns are the SHARED ladder
 * {32, 128, 512, 2048, 8192} → {16, 64, 256, 1024, 4096} (NBT_INPUT/NBT_OUTPUT verbatim
 * on all fifteen Loader rows — the in column of one family IS the out column of the
 * other): capacitor = 2×in (Base10 :75), the in band min in/2 / rec in / max 2in (:76,
 * every row in &gt; 16), the out band out/2 / out / 2out (:77), the per-tick {@code
 * units(storage, in, out)} conversion (:62), the out/2 emission door (:64) and the
 * WASTE_ENERGY = T vent
 * (:92 — the Loader rows carry {@code NBT_WASTE_ENERGY, T} on every laser and absorber
 * rung: no emit deduction (:81/:87 skipped), the whole bucket vents every tick — the
 * laser chain is lossy at each converter BY DESIGN, the upstream half-rate). The
 * :68-77 overload leg stays UNREACHABLE by construction (storage ≤ 2×in ⇒ tOutput ≤
 * 2×out = outMax) — the dynamo/transformer identical declaration.
 *
 * <h2>Cropped with declaration</h2>
 * The {@code ITileEntitySwitchableMode} mMode dial (the laser implements it upstream; the
 * port has no GT tool-click channel — the transformer/mMode=0 crop precedent, the mode
 * stays the constant 0) and the {@code ITileEntityAdjacentOnOff} toggle ({@code mStopped}
 * is kept with the core's :151 acceptance formula; the toggle interface itself is the
 * pool). The negative-sign conjunct (Base10 :121) is constant false on BOTH families:
 * LU ∉ ALL_NEGATIVE_ALLOWED (TD.java:202) folds one conjunct of each — declared at
 * {@link #negativeOutputAllowed()}. The visual overlay (colored/overlay/overlay_active
 * trichotomy) is the static-face datagen (the task card: 光束渲染 defer, visual = static
 * block face).
 */
public class GT6LaserConverterBlockEntity extends GT6DynamoBlockEntity {

	/** The persisted cumulative intake (the consumed-side units, the bridge RCON face). */
	public static final String NBT_LAST_IN = "gt.last_in";
	/** The persisted cumulative emission (the emitted-side units, the bridge RCON face). */
	public static final String NBT_LAST_OUT = "gt.last_out";

	/** The intake type (EU on the laser rows, LU on the absorber rows — the NBT_ENERGY_ACCEPTED column). */
	private final TagData mInType;
	/** The emission type (LU on the laser rows, EU on the absorber rows — the NBT_ENERGY_EMITTED column). */
	private final TagData mOutType;
	/** True = input only on the BACK face (the absorber, :35); false = all-but-front (the laser, Base10 :176). */
	private final boolean mBackInputOnly;

	/** Cumulative consumed mass (doInject whole packets, the bridge mLastIn form). */
	public long mLastIn = 0;
	/** Cumulative emitted mass (whole accepted packets — the emission packet is atomic at size tOutput). */
	public long mLastOut = 0;

	/** The offline test seam — the core's {@code setAdjacencyOverride} is package-private to its home package. */
	private @Nullable IEnergyAdjacency mAdjacencyOverride = null;

	/** Full constructor — the type-capturing BET factories and the offline (test) entry point. */
	public GT6LaserConverterBlockEntity(@Nullable BlockEntityType<?> aType, TagData aInType, TagData aOutType,
			boolean aBackInputOnly, BlockPos aPos, BlockState aState) {
		super(aType, aPos, aState);
		mInType = aInType;
		mOutType = aOutType;
		mBackInputOnly = aBackInputOnly;
		// the bridge-constructor step: both families share the ONE ladder, so the tier read
		// needs no family axis (GT6DynamoBlock.tier over the carrier state; STONE = rung 0)
		applyTier(aState, GT6Lasers.LASER_INPUTS, GT6Lasers.LASER_OUTPUTS);
	}

	@Override
	public String getTileEntityName() {
		// the BET id twins (GT6Lasers.CO2_LASER_BE / GT6Lasers.LASER_ABSORBER_BE /
		// GT6QuantumEnergizers.QUANTUM_ENERGIZER_BE): the QU emission names the energizer
		// (the absorber and the energizer SHARE the back-input face set — the input face
		// alone cannot tell them apart, the type pair can)
		if (mOutType == TD.Energy.QU) return "quantum_energizer";
		return mBackInputOnly ? "laser_absorber" : "co2_laser";
	}

	@Override
	public TagData outputType() {
		return mOutType;
	}

	/** The intake type — the second family axis the core hardcodes as RU (the bridge re-type axis, widened to a field). */
	public TagData inputType() {
		return mInType;
	}

	@Override
	protected boolean negativeOutputAllowed() {
		// Base10 :121 — the conjunct needs BOTH types in ALL_NEGATIVE_ALLOWED; every laser/
		// absorber pair contains LU, which is not (TD.java:202). Constant false, declared.
		return TD.Energy.ALL_NEGATIVE_ALLOWED.contains(mInType) && TD.Energy.ALL_NEGATIVE_ALLOWED.contains(mOutType);
	}

	@Override
	protected long emitConverted(long tOutput, boolean aNegative) {
		// the Converter :85 size-carrying branch: ONE packet, size = ±tOutput, amount 1;
		// the Util loops the sides, isEnergyEmittingTo gates FRONT-only (the core face)
		long tSign = aNegative ? -1 : 1;
		long tUsed = ITileEntityEnergy.Util.emitEnergyToNetwork(mOutType, tSign * tOutput, 1, this, adjacency());
		if (tUsed > 0) mLastOut += tOutput; // the packet is atomic: accepted = the whole tOutput landed
		return tUsed;
	}

	// --- the intake arm: the core's hardcoded RU re-typed per family (the bridge shape) ---

	@Override
	public boolean isEnergyType(TagData aEnergyType, byte aSide, boolean aEmitting) {
		// upstream :150: (aEmitting ? mEnergyOUT : mEnergyIN).isType(aEnergyType)
		return aEmitting ? aEnergyType == outputType() : aEnergyType == mInType;
	}

	@Override
	public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {
		if (aEnergyType != mInType) return 0;
		return mInput <= 16 ? 1 : mInput / 2; // Base10 :76 verbatim (in > 16 on every row here)
	}

	@Override
	public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {
		return aEnergyType == mInType ? mInput : 0;
	}

	@Override
	public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {
		return aEnergyType == mInType ? mInput * 2 : 0;
	}

	@Override
	public java.util.Collection<TagData> getEnergyTypes(byte aSide) {
		// upstream :159 — both converter halves
		return java.util.Arrays.asList(mInType, outputType());
	}

	@Override
	public boolean isInput(byte aSide) {
		// the ONE face split between the families: the absorber takes the BACK only
		// (MultiTileEntityLaserAbsorberElectric :35), the laser takes everything but the
		// FRONT (Base10 :176 — the core default)
		return mBackInputOnly ? aSide == opposite(mFacing) : aSide != mFacing;
	}

	@Override
	public long doInject(TagData aEnergyType, byte aSide, long aSize, long aAmount, boolean aDoInject) {
		long tConsumed = super.doInject(aEnergyType, aSide, aSize, aAmount, aDoInject);
		if (aDoInject && tConsumed > 0) mLastIn += tConsumed * Math.abs(aSize); // the Stats whole-packet mass
		return tConsumed;
	}

	/** The live accounting snapshot arm — resets both counters (the bridge reset shape). */
	public void resetAccounting() {
		mLastIn = 0;
		mLastOut = 0;
	}

	/** The offline test seam for the emit side (the bridge seam is package-private to gregtech6.registry). */
	public void setAdjacencyOverrideForTest(@Nullable IEnergyAdjacency aAdjacency) {
		mAdjacencyOverride = aAdjacency;
	}

	@Override
	protected IEnergyAdjacency adjacency() {
		if (mAdjacencyOverride != null) return mAdjacencyOverride;
		return super.adjacency();
	}

	@Override
	protected void saveAdditional(CompoundTag aNBT) {
		super.saveAdditional(aNBT);
		aNBT.putLong(NBT_LAST_IN, mLastIn);
		aNBT.putLong(NBT_LAST_OUT, mLastOut);
	}

	@Override
	public void load(CompoundTag aNBT) {
		super.load(aNBT);
		if (aNBT.contains(NBT_LAST_IN, Tag.TAG_ANY_NUMERIC)) mLastIn = Math.max(0, aNBT.getLong(NBT_LAST_IN));
		if (aNBT.contains(NBT_LAST_OUT, Tag.TAG_ANY_NUMERIC)) mLastOut = Math.max(0, aNBT.getLong(NBT_LAST_OUT));
	}
}
