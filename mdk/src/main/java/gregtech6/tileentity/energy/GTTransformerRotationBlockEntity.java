package gregtech6.tileentity.energy;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.registry.GTBlockEntities;
import gregtech6.registry.GT6Kinetics;

/**
 * 1.20.1 counterpart of the GT6 Transformer Gearbox — task gearbox-transformer
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
 * <p>The waste leg (the converter :92 tail, LIVE — the row carries
 * {@code NBT_WASTE_ENERGY = T}, Loader :1668): EVERY tick the capacitor vents
 * {@code units(mEnergyIN.mMax = 16, 16, 16 - aMode = 16) = 16} = the whole capacity
 * (mMode is the constant 0 of this port), unconditionally after the emit attempt.
 * Upstream is a FUNNEL that vents what did not flow ("不通就漏光") — a burst the
 * consumer refused is gone and the next tick has nothing to re-emit — not a buffer.
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
 *
 * <p>Task kinetics-be-function-family: the shared converter machinery (capacitor, Stats
 * gate, overload, adjacency, the energy-face family, the facing mirror, NBT) moved UP
 * into {@link GTEnergyConverterBlockEntity} (the Base10 port — the rotation engine and
 * the steam turbine mount the same base); this class keeps its ÷4×4 law and the ROW
 * ADOPTION — the 12 metal Transformer Gearbox rows (task material-mc-d-powertrain-rows)
 * ride their row's V[t]→V[t-1] pair through the same law when the mounted block is a
 * {@code GT6Kinetics.PowertrainBlock} of the transformer family; the seated wooden
 * singleton keeps the :1668 constants.
 */
public class GTTransformerRotationBlockEntity extends GTEnergyConverterBlockEntity {

	/** The wood row (Loader :1668): NBT_INPUT = V[0] = 8, NBT_OUTPUT = 2, NBT_MULTIPLIER = 4. */
	public static final long INPUT_SPEED = GT6Kinetics.TRANSFORMER_INPUT_SPEED;
	public static final long OUTPUT_SPEED = GT6Kinetics.TRANSFORMER_OUTPUT_SPEED;
	public static final long MULTIPLIER = GT6Kinetics.TRANSFORMER_MULTIPLIER;

	/** BET factory for BlockEntityType.Builder.of — the shared type resolves through the registry at runtime. */
	public GTTransformerRotationBlockEntity(BlockPos aPos, BlockState aState) {
		this(null, aPos, aState);
	}

	/**
	 * Full constructor — also the offline (test) entry point: a null type falls back to
	 * the shared registry type at runtime, tests pass an offline-built BET.
	 */
	public GTTransformerRotationBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType != null ? aType : GTBlockEntities.TRANSFORMER_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "transformer_rotation"; // BET registry path mirrors it (GTBlockEntities.TRANSFORMER_BE)
	}

	// ---------------------------------------------------------------------------
	// the row adoption (task material-mc-d-powertrain-rows — the mc-D data layer this
	// card wires): the mounted block's TransformerGearboxRow carries the V[t]→V[t-1]
	// pair; the seated wooden singleton keeps the :1668 constants.
	// ---------------------------------------------------------------------------

	/** The row's input speed V[t] (the wooden row = V[0] = 8). */
	public long inputSpeed() {
		if (getBlockState().getBlock() instanceof GT6Kinetics.PowertrainBlock tBlock
				&& tBlock.row() instanceof GT6Kinetics.TransformerGearboxRow tRow) {
			return tRow.inputSpeed();
		}
		return INPUT_SPEED;
	}

	/** The row's output speed V[t-1] (the wooden row = 2). */
	public long outputSpeed() {
		if (getBlockState().getBlock() instanceof GT6Kinetics.PowertrainBlock tBlock
				&& tBlock.row() instanceof GT6Kinetics.TransformerGearboxRow tRow) {
			return tRow.outputSpeed();
		}
		return OUTPUT_SPEED;
	}

	// ---------------------------------------------------------------------------
	// the faces (MultiTileEntityTransformerRotation :42-43 + the crank facing mirror)
	// ---------------------------------------------------------------------------

	/** The input face (upstream isInput :42, the non-reversed form). */
	@Override
	public boolean isInput(byte aSide) {
		return aSide == mFacing;
	}

	/** The output face (upstream isOutput :43 — the opposite of the front). */
	@Override
	public boolean isOutput(byte aSide) {
		return aSide == opposite(mFacing);
	}

	// ---------------------------------------------------------------------------
	// the bands (readEnergyBehavior :73-78 over the adopted row)
	// ---------------------------------------------------------------------------

	@Override
	public long inputSizeMin() {
		return 1; // the Stats band (readEnergyBehavior :76 with tInput = 8 ≤ 16)
	}

	@Override
	public long inputSizeRec() {
		return inputSpeed();
	}

	@Override
	public long inputSizeMax() {
		return inputSpeed() * 2;
	}

	@Override
	public long outputSizeMin() {
		return outputSpeed() / 2;
	}

	@Override
	public long outputSizeRec() {
		return outputSpeed();
	}

	@Override
	public long outputSizeMax() {
		return outputSpeed() * 2;
	}

	@Override
	public long storageCapacity() {
		return inputSpeed() * 2; // readEnergyBehavior :75 = tInput × 2
	}

	@Override
	public boolean wasteEnergy() {
		return true; // the row column, Loader :1668/:1677
	}

	@Override
	public TagData energyTypeIn() {
		return TD.Energy.RU;
	}

	@Override
	public TagData energyTypeOut() {
		return TD.Energy.RU;
	}

	// ---------------------------------------------------------------------------
	// the per-tick conversion (upstream doConversion :120-127 over the converter
	// doConversion :63-89, the size-carrying RU branch)
	// ---------------------------------------------------------------------------

	@Override
	protected void doConversion(long aTimer) {
		long tOutput = mStorage * outputSpeed() / inputSpeed(); // units(storage, inRec=8, outRec=2, roundDown) — 8|2 divides
		boolean tCanEmit = tOutput >= outputSizeMin(); // the converter :65 gate
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
		// the waste leg (the converter :92 tail, aMode = 0 constant-folded: units(16, 16,
		// 16) = 16 = INPUT_SIZE_MAX) — runs UNCONDITIONALLY every tick, so whatever the
		// capacitor held past this tick's flow is VENTED: a refused burst drains the
		// whole store and the next tick has nothing to re-emit (the funnel semantics)
		if (wasteEnergy()) mStorage = Math.max(0, mStorage - inputSizeMax());
	}
}
