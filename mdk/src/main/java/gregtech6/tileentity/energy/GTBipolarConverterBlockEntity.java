package gregtech6.tileentity.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;
import net.minecraft.world.level.block.state.BlockState;

import gregapi.tileentity.energy.ITileEntityEnergy;

/**
 * The GT6 BIPOLAR converter base (task kinetics-be-function-family — the
 * TileEntityBase11Bipolar port, gregapi/tileentity/energy/TileEntityBase11Bipolar.java
 * :33-67): a converter that drives BOTH front and back simultaneously with OPPOSITE
 * rotation signs — the doBipolar law (TE_Behavior_Energy_Converter.doBipolar :108-150):
 * {@code +tOutput} packets out one axis face and {@code -tOutput} out the other, the
 * drain booking {@code units(emitted × tOutput, outRec, inRec)}, the funnel waste leg
 * venting {@code units(inMax, 16, 16) = inMax} every tick.
 *
 * <p>Faces (upstream :63-66): the input is ANY side OFF the front/back axis
 * ({@code !ALONG_AXIS}), the output IS the axis (front + back, the two bipolar poles).
 *
 * <p>The concrete machine picks the sign assignment per tick — the rotation engine
 * alternates which pole gets the positive half every 16 ticks (the
 * {@code aTimer % 32 < 16} cycle, MultiTileEntityEngineRotation :36).
 */
public abstract class GTBipolarConverterBlockEntity extends GTEnergyConverterBlockEntity {

	protected GTBipolarConverterBlockEntity(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(aType, aPos, aState);
	}

	// ---------------------------------------------------------------------------
	// the faces (TileEntityBase11Bipolar :63-64)
	// ---------------------------------------------------------------------------

	@Override
	public boolean isInput(byte aSide) {
		return aSide != mFacing && aSide != opposite(mFacing); // !ALONG_AXIS[aSide][mFacing]
	}

	@Override
	public boolean isOutput(byte aSide) {
		return aSide == mFacing || aSide == opposite(mFacing); // ALONG_AXIS[aSide][mFacing]
	}

	// ---------------------------------------------------------------------------
	// the bipolar conversion (TE_Behavior_Energy_Converter.doBipolar :108-150, the
	// size-relevant branch — KU is not SIZE_IRRELEVANT)
	// ---------------------------------------------------------------------------

	/**
	 * One bipolar conversion tick: {@code tOutput = units(storage, inRec, outRec, F)};
	 * below the output minimum nothing flows; above the output maximum the machine
	 * OVERLOADS (the converter :117-123 arm — aTimer > 2 sets mOverloaded, the engine
	 * then clears its storage through {@link #overload()}); the emit fires +tOutput out
	 * {@code aSidePos} and -tOutput out {@code aSideNeg}, and a non-zero acceptance
	 * drains {@code units(packets × tOutput, outRec, inRec)}; the waste leg vents
	 * {@code inputSizeMax()} unconditionally.
	 *
	 * @return true when the machine is ACTIVE (the mActivity readout, upstream :36)
	 */
	protected boolean doBipolar(long aTimer, byte aSidePos, byte aSideNeg) {
		long tOutput = mStorage * outputSizeRec() / inputSizeRec(); // units(storage, inRec, outRec, F)
		boolean tCanEmit = tOutput >= outputSizeMin(); // the converter :110 gate
		boolean rActive = false;
		if (tCanEmit) {
			if (tOutput > outputSizeMax()) {
				// the converter :117-123 — mLimitConsumption is F for the powertrain rows
				// (RU/KU are not CONSUMPTION_LIMITED) and aTimer > 2 past chunkload
				overload();
				if (wasteEnergy()) mStorage = Math.max(0, mStorage - inputSizeMax());
				return false;
			}
			// the size-relevant arm :138-140 — the ± signs ARE the rotation directions
			long tEmitted = ITileEntityEnergy.Util.emitEnergyToSide(energyTypeOut(), aSidePos, tOutput, 1, this, adjacency())
					+ ITileEntityEnergy.Util.emitEnergyToSide(energyTypeOut(), aSideNeg, -tOutput, 1, this, adjacency());
			if (tEmitted > 0) {
				// the converter :141-143 drain — units(packets × tOutput, outRec, inRec, T)
				mStorage -= tEmitted * tOutput;
				if (mStorage < 0) mStorage = 0;
				mLastOutSize = tOutput;
				mLastOutAmount = tEmitted;
				rActive = true;
			}
		}
		if (wasteEnergy()) mStorage = Math.max(0, mStorage - inputSizeMax()); // the converter :150 tail
		return rActive;
	}
}
