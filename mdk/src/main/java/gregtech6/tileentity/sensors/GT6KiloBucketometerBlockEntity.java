package gregtech6.tileentity.sensors;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * The Kilo-Bucket-O-Meter Sensor (task p34-sensors-trivial-14 row ⑨) — the port of
 * MultiTileEntityKiloBucketometer.java:29-56: the same tank census at the
 * CUBIC-DECAMETER grain ({@code / 1000000}, :32-39/:44-48). Upstream's world-fluid arm
 * is ABSENT on this class verbatim (only the kilobucketometer body has no
 * water/lava/IFluidBlock branch beyond the handler) — the handler census is the whole
 * read.
 */
public class GT6KiloBucketometerBlockEntity extends GTSensorBlockEntity {

	/** BET factory for BlockEntityType.Builder.of — resolves the type through the registry at runtime (the GTCrankBlockEntity shape). */
	public GT6KiloBucketometerBlockEntity(BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — also the offline (test) entry: a null type falls back to the shared registry type. */
	public GT6KiloBucketometerBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
		super(aType != null ? aType : gregtech6.registry.GTBlockEntities.KILOBUCKETOMETER_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "kilobucketometer";
	}

	@Override
	public long getCurrentValue(@Nullable BlockEntity aTarget) {
		long[] tTanks = GT6FluidometerBlockEntity.tankCensus(aTarget, mSecondFacing);
		if (tTanks != null) return GT6FluidometerBlockEntity.sumContents(tTanks) / 1000000; // upstream :32-39
		return 0;
	}

	@Override
	public long getCurrentMax(@Nullable BlockEntity aTarget) {
		long[] tTanks = GT6FluidometerBlockEntity.tankCensus(aTarget, mSecondFacing);
		if (tTanks != null) return GT6FluidometerBlockEntity.sumCapacity(tTanks) / 1000000; // upstream :44-48
		return 0;
	}
}
