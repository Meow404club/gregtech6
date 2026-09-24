package gregtech6.tileentity.sensors;

import javax.annotation.Nullable;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import gregtech6.tileentity.connectors.GTWireBlockEntity;

/**
 * The Laser-O-Meter Sensor (task p37-sensors-3, the pool closure) — the port of
 * MultiTileEntityLaserometer.java:36-70. The read face: the LU carrier is LIVE since
 * p32 (the P34 "no measurement domain" pooled note is the stale half — the p10 wire
 * rows carried the data plane, the p32 LU revival lit the transfer bookkeeping):
 * upstream reads the last-tick transferred LU of a {@code MultiTileEntityWireLaser}
 * (:42, {@code mTransferredLast}) against the constant 65535 ceiling (:48); this port's
 * carrier is {@link GTWireBlockEntity} with the laser-family gate {@code isLaser()}
 * (GTWireBlockEntity.java:138/:344, the tick lag :415 is the upstream :61 window lag).
 * Every other neighbour answers 0 (upstream :43/:49).
 */
public class GT6LaserometerBlockEntity extends GTSensorBlockEntity {

	/** BET factory for BlockEntityType.Builder.of — resolves the type through the registry at runtime (the GTCrankBlockEntity shape). */
	public GT6LaserometerBlockEntity(net.minecraft.core.BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — also the offline (test) entry: a null type falls back to the shared registry type. */
	public GT6LaserometerBlockEntity(@Nullable BlockEntityType<?> aType, net.minecraft.core.BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
		super(aType != null ? aType : gregtech6.registry.GTBlockEntities.LASEROMETER_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "laserometer";
	}

	@Override
	public long getCurrentValue(@Nullable BlockEntity aTarget) {
		if (aTarget instanceof GTWireBlockEntity tWire && tWire.isLaser()) return tWire.mTransferredLast; // upstream :42
		return 0; // upstream :43
	}

	@Override
	public long getCurrentMax(@Nullable BlockEntity aTarget) {
		if (aTarget instanceof GTWireBlockEntity tWire && tWire.isLaser()) return 65535; // upstream :48 verbatim
		return 0; // upstream :49
	}
}
