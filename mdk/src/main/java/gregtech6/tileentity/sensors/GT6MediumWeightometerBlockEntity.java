package gregtech6.tileentity.sensors;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * The Medium Weight-O-Meter Sensor (task p34-sensors-trivial-14 row ⑪) — the port of
 * MultiTileEntityWeightometerMedium.java:32-63, the KILOGRAM scale (upstream :62 returns
 * the raw kg reading). The declared mass-face gap rides the
 * {@link GT6LightWeightometerBlockEntity} declaration verbatim — same seam, one scale
 * step down.
 */
public class GT6MediumWeightometerBlockEntity extends GTSensorBlockEntity {

	/** BET factory for BlockEntityType.Builder.of — resolves the type through the registry at runtime (the GTCrankBlockEntity shape). */
	public GT6MediumWeightometerBlockEntity(BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — also the offline (test) entry: a null type falls back to the shared registry type. */
	public GT6MediumWeightometerBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
		super(aType != null ? aType : gregtech6.registry.GTBlockEntities.MEDIUMWEIGHTOMETER_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "mediumweightometer";
	}

	@Override
	public long getCurrentValue(@Nullable BlockEntity aTarget) {
		return 0; // upstream :62 minus the un-ported mass faces — kilograms
	}

	@Override
	public long getCurrentMax(@Nullable BlockEntity aTarget) {
		return GT6LightWeightometerBlockEntity.MAX_WEIGHT_READING; // upstream :65 verbatim
	}
}
