package gregtech6.tileentity.energy;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.BlockState;

import gregapi.code.TagData;
import gregapi.data.TD;
import gregapi.tileentity.energy.EnergyTarget;
import gregapi.tileentity.energy.IEnergyAdjacency;
import gregapi.tileentity.energy.ITileEntityEnergy;
import gregtech6.tileentity.TileEntityBase03TicksAndSync;

public abstract class GTEnergyConverterBlockEntity extends TileEntityBase03TicksAndSync implements ITileEntityEnergy {

	/** The upstream NBT keys (CS.java:1241) + the capacitor carrier (the TE_Behavior stack is not ported). */
	public static final String NBT_STOPPED = "gt.stopped";
	public static final String NBT_CAPACITOR = "gt.capacitor";

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
	public byte mFacing = 2; // NORTH

	protected GTEnergyConverterBlockEntity(BlockEntityType<?> aType, BlockPos aPos, BlockState aState) {
		super(true, aType, aPos, aState);
	}

	// ---------------------------------------------------------------------------
	// the row bands (readEnergyBehavior :73-78 — per-row here, constants upstream)
	// ---------------------------------------------------------------------------

	/** The input band minimum (upstream tInput ≤ 16 ? 1 : tInput/2). */
	public abstract long inputSizeMin();

	/** The input band recommended = the row's NBT_INPUT. */
	public abstract long inputSizeRec();

	/** The input band maximum = tInput * 2. */
	public abstract long inputSizeMax();

	/** The output band minimum (upstream tOutput / 2). */
	public abstract long outputSizeMin();

	/** The output band recommended = the row's NBT_OUTPUT. */
	public abstract long outputSizeRec();

	/** The output band maximum = tOutput * 2. */
	public abstract long outputSizeMax();

	/** The capacitor capacity (upstream :75 = tInput * 2). */
	public abstract long storageCapacity();

	/** The row's NBT_WASTE_ENERGY column — the funnel vent leg of the converter (:92 tail). */
	public abstract boolean wasteEnergy();

	/** The NBT_ENERGY_ACCEPTED type (the input face family). */
	public abstract TagData energyTypeIn();

	/** The NBT_ENERGY_EMITTED type (the output face family). */
	public abstract TagData energyTypeOut();

	/** The input face (the concrete machine's own isInput :42-form). */
	public abstract boolean isInput(byte aSide);

	/** The output face (the concrete machine's own isOutput :43-form). */
	public abstract boolean isOutput(byte aSide);

	// ---------------------------------------------------------------------------
	// the tick (Base10EnergyConverter.onTick2 :106-113)
	// ---------------------------------------------------------------------------

	@Override
	public void onTick(long aTimer, boolean aIsServerSide) {
		if (!aIsServerSide) return; // the :110 client branch is the sound pool
		syncFacingFromState();
		doConversion(aTimer);
	}

	/** The per-tick conversion law — the concrete machine's doConversion (Base10 :120+). */
	protected abstract void doConversion(long aTimer);

	/** The GT6 side order opposite (CS OPOS; byte side == Direction.get3DDataValue). */
	public static byte opposite(byte aSide) {
		return (byte) Direction.from3DDataValue(aSide).getOpposite().get3DDataValue();
	}

	/**
	 * The /setblock RCON path: the state is the authority, the BE re-syncs at each tick
	 * head (the crank syncFacingFromState form, keyed on the PROPERTY).
	 */
	void syncFacingFromState() {
		if (getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
			mFacing = (byte) getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).get3DDataValue();
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
		if (tAbs > inputSizeMax()) { // the Stats.doInject oversize leg (:57-62)
			if (aDoInject) overload();
			return aAmount;
		}
		if (mStorage >= storageCapacity()) return 0; // the Stats full gate (:63)
		long tEnergy = Math.min(storageCapacity() - mStorage, tAbs * aAmount); // Stats :64
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
	protected void overload() {
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

	IEnergyAdjacency adjacency() {
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
		return aEnergyType == (aEmitting ? energyTypeOut() : energyTypeIn()); // upstream :150
	}

	@Override
	public boolean isEnergyAcceptingFrom(TagData aEnergyType, byte aSide, boolean aTheoretical) {
		// upstream :151 — every powertrain row carries NBT_WASTE_ENERGY = T, so the
		// (mWasteEnergy || ...) middle term is constant true; mReversed is cropped
		return (aTheoretical || !mStopped) && isInput(aSide) && isEnergyType(aEnergyType, aSide, false);
	}

	@Override
	public boolean isEnergyEmittingTo(TagData aEnergyType, byte aSide, boolean aTheoretical) {
		return isOutput(aSide) && isEnergyType(aEnergyType, aSide, true); // upstream :152
	}

	@Override
	public long getEnergySizeInputMin(TagData aEnergyType, byte aSide) {
		return inputSizeMin(); // upstream :153 (the Stats band)
	}

	@Override
	public long getEnergySizeInputRecommended(TagData aEnergyType, byte aSide) {
		return inputSizeRec(); // upstream :156
	}

	@Override
	public long getEnergySizeInputMax(TagData aEnergyType, byte aSide) {
		return inputSizeMax(); // upstream :157
	}

	@Override
	public long getEnergySizeOutputMin(TagData aEnergyType, byte aSide) {
		return outputSizeMin(); // upstream :153
	}

	@Override
	public long getEnergySizeOutputRecommended(TagData aEnergyType, byte aSide) {
		return outputSizeRec(); // upstream :154
	}

	@Override
	public long getEnergySizeOutputMax(TagData aEnergyType, byte aSide) {
		return outputSizeMax(); // upstream :155
	}

	@Override
	public java.util.Collection<TagData> getEnergyTypes(byte aSide) {
		return energyTypeIn() == energyTypeOut() ? energyTypeIn().AS_LIST
				: java.util.List.of(energyTypeIn(), energyTypeOut()); // upstream :159
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
