package gregtech6.tileentity.sensors;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * The Super Heavy Weight-O-Meter Sensor (task p34-sensors-trivial-14 row ⑬) — the port
 * of MultiTileEntityWeightometerSuperHeavy.java:32-63, the KILOTON scale (upstream :54
 * {@code rWeightKG / 1000000}). The declared mass-face gap rides the
 * {@link GT6LightWeightometerBlockEntity} declaration verbatim — same seam, the top
 * scale step.
 */
public class GT6SuperHeavyWeightometerBlockEntity extends GTSensorBlockEntity {

	/** BET factory for BlockEntityType.Builder.of — resolves the type through the registry at runtime (the GTCrankBlockEntity shape). */
	public GT6SuperHeavyWeightometerBlockEntity(BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — also the offline (test) entry: a null type falls back to the shared registry type. */
	public GT6SuperHeavyWeightometerBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
		super(aType != null ? aType : gregtech6.registry.GTBlockEntities.SUPERHEAVYWEIGHTOMETER_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "superheavyweightometer";
	}

	@Override
	public long getCurrentValue(@Nullable BlockEntity aTarget) {
		return 0; // upstream :62 minus the un-ported mass faces — kilotons
	}

	@Override
	public long getCurrentMax(@Nullable BlockEntity aTarget) {
		return GT6LightWeightometerBlockEntity.MAX_WEIGHT_READING; // upstream :65 verbatim
	}
}
