package gregtech6.tileentity.sensors;

import javax.annotation.Nullable;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * The Kilo-Gibbl-O-Meter Sensor (task p34-sensors-trivial-14 row ⑤) — the port of
 * MultiTileEntityKiloGibblometer.java:31-46, the {@link GT6GibblometerBlockEntity}
 * sibling at the Kilo divisor (upstream :32-33 {@code / 1000000} vs the Gibbl meter's
 * {@code / 1000} — the SAME ITileEntityGibbl seam, so the declared-zero baseline and its
 * reasoning live on the Gibbl class; this row exists so the census closes at the
 * upstream 21-row shape, not as a separate seam).
 */
public class GT6KiloGibblometerBlockEntity extends GTSensorBlockEntity {

	/** The upstream divisor (:32 — Kilo-Gibbl = 1e6 steam units). */
	public static final long SCALE = 1000000;

	/** BET factory for BlockEntityType.Builder.of — resolves the type through the registry at runtime (the GTCrankBlockEntity shape). */
	public GT6KiloGibblometerBlockEntity(net.minecraft.core.BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — also the offline (test) entry: a null type falls back to the shared registry type. */
	public GT6KiloGibblometerBlockEntity(@Nullable BlockEntityType<?> aType, net.minecraft.core.BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
		super(aType != null ? aType : gregtech6.registry.GTBlockEntities.KILOGIBBLOMETER_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "kilogibblometer";
	}

	@Override
	public long getCurrentValue(@Nullable BlockEntity aTarget) {
		return 0; // upstream :32 minus the un-ported ITileEntityGibbl arm (the Gibbl class declaration)
	}

	@Override
	public long getCurrentMax(@Nullable BlockEntity aTarget) {
		return 0; // upstream :33 ditto
	}
}
