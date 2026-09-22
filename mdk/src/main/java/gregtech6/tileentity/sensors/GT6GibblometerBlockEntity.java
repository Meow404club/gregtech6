package gregtech6.tileentity.sensors;

import javax.annotation.Nullable;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * The Gibbl-O-Meter Sensor (task p34-sensors-trivial-14 row ④) — the port of
 * MultiTileEntityGibblometer.java:32-53. Upstream reads
 * {@code ITileEntityGibbl.getGibblValue/getGibblMax / 1000} (:33-34, the boiler steam
 * compression face, the Gibbl unit = 1000 steam units per Gibbl). The port has NO
 * {@code ITileEntityGibbl} seam yet: the boiler cards pooled their Gibbl display face
 * (GTBoilerTankBlockEntity.java:128, TileEntityLargeBoiler.java:143) and no producer
 * landed since. The meter therefore rides the declared 0/0 baseline — the
 * {@link GT6ElectrometerBlockEntity} non-wire form — until the Gibbl seam pool card
 * lands; the sensor block itself (modes/keypad/averaging/redstone) is fully live.
 * <p>ponytail: no dead interface invented for a seam with zero producers — the
 * upstream body IS the instanceof check, and instanceof-nothing is the literal zero.
 */
public class GT6GibblometerBlockEntity extends GTSensorBlockEntity {

	/** The upstream divisor (:33 — Gibbl = 1000 steam units, the meter reads whole Gibbl). */
	public static final long SCALE = 1000;

	/** BET factory for BlockEntityType.Builder.of — resolves the type through the registry at runtime (the GTCrankBlockEntity shape). */
	public GT6GibblometerBlockEntity(net.minecraft.core.BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — also the offline (test) entry: a null type falls back to the shared registry type. */
	public GT6GibblometerBlockEntity(@Nullable BlockEntityType<?> aType, net.minecraft.core.BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
		super(aType != null ? aType : gregtech6.registry.GTBlockEntities.GIBBLOMETER_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "gibblometer";
	}

	@Override
	public long getCurrentValue(@Nullable BlockEntity aTarget) {
		return 0; // upstream :33 minus the un-ported ITileEntityGibbl arm (declared above)
	}

	@Override
	public long getCurrentMax(@Nullable BlockEntity aTarget) {
		return 0; // upstream :34 ditto
	}
}
