package gregtech6.tileentity.sensors;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Bucket-O-Meter Sensor (task p34-sensors-trivial-14 row ⑧) — the port of
 * MultiTileEntityBucketometer.java:30-63: the Fluidometer tank walk at the CUBIC-METER
 * grain — contents/capacity {@code / 1000} (:33-40/:47-52) via the shared
 * {@link GT6FluidometerBlockEntity#tankCensus} census. The still-fluid arm (:42-45,
 * {@code metadata == 0 → 1}) lands as the vanilla source-state check → 1 m³, feeding
 * {@link #getCurrentValue} ONLY (the max side keeps the upstream :53-58 handler-only
 * zero, the Fluidometer 1000/0 asymmetry verbatim). Upstream's {@code IFluidBlock}
 * modded-flow arm (:44-46) is CUT declared — the port's world fluids are the vanilla
 * FluidState face (the Fluidometer still-arm precedent).
 */
public class GT6BucketometerBlockEntity extends GTSensorBlockEntity {

	/** BET factory for BlockEntityType.Builder.of — resolves the type through the registry at runtime (the GTCrankBlockEntity shape). */
	public GT6BucketometerBlockEntity(BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — also the offline (test) entry: a null type falls back to the shared registry type. */
	public GT6BucketometerBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
		super(aType != null ? aType : gregtech6.registry.GTBlockEntities.BUCKETOMETER_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "bucketometer";
	}

	@Override
	public long getCurrentValue(@Nullable BlockEntity aTarget) {
		long[] tTanks = GT6FluidometerBlockEntity.tankCensus(aTarget, mSecondFacing);
		if (tTanks != null) return GT6FluidometerBlockEntity.sumContents(tTanks) / 1000; // upstream :33-40
		return stillFluidValue(); // upstream :42-45, 1 m³ per source block
	}

	@Override
	public long getCurrentMax(@Nullable BlockEntity aTarget) {
		long[] tTanks = GT6FluidometerBlockEntity.tankCensus(aTarget, mSecondFacing);
		if (tTanks != null) return GT6FluidometerBlockEntity.sumCapacity(tTanks) / 1000; // upstream :47-52
		return 0; // upstream :53-58 verbatim — the source arm has NO max (the 1/0 pair)
	}

	/** The still-fluid arm (upstream :42-45): a water/lava SOURCE block reads 1 (m³). */
	private long stillFluidValue() {
		if (hasLevel()) {
			BlockState tState = getLevel().getBlockState(getBlockPos().relative(Direction.from3DDataValue(mSecondFacing)));
			if ((tState.getBlock() == net.minecraft.world.level.block.Blocks.WATER || tState.getBlock() == net.minecraft.world.level.block.Blocks.LAVA)
					&& tState.getFluidState().isSource()) {
				return 1;
			}
		}
		return 0;
	}
}
