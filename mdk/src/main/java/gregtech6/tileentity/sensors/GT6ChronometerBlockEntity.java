package gregtech6.tileentity.sensors;

import javax.annotation.Nullable;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * The Chronometer Sensor (task p34-sensors-trivial-14 row ③) — the port of
 * MultiTileEntityChronometer.java:33-56. The read is the upstream time-of-day minute
 * ((:34) {@code ((worldTime+6000)%24000*60)/1000}, max 1440 — the vanilla day cycle
 * offset +6000 puts midnight at minute 0); the 1.7.10 {@code getWorldTime()} face is the
 * modern {@code Level.getDayTime()} (Level.java:702, the levelData day clock —
 * {@code getGameTime()} is the uptime counter and NOT this face). The conversion is a
 * pure static so the offline suite pins the arithmetic without a Level.
 */
public class GT6ChronometerBlockEntity extends GTSensorBlockEntity {

	/** BET factory for BlockEntityType.Builder.of — resolves the type through the registry at runtime (the GTCrankBlockEntity shape). */
	public GT6ChronometerBlockEntity(net.minecraft.core.BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — also the offline (test) entry: a null type falls back to the shared registry type. */
	public GT6ChronometerBlockEntity(@Nullable BlockEntityType<?> aType, net.minecraft.core.BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
		super(aType != null ? aType : gregtech6.registry.GTBlockEntities.CHRONOMETER_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "chronometer";
	}

	/** Upstream :34 verbatim — the day-clock minute of the (real-world noon anchored) GT day. */
	public static long minutesOfDay(long aDayTime) {
		return (((aDayTime + 6000) % 24000) * 60) / 1000;
	}

	@Override
	public long getCurrentValue(@Nullable BlockEntity aTarget) {
		if (!hasLevel()) return 0;
		return minutesOfDay(getLevel().getDayTime()); // upstream :34
	}

	@Override
	public long getCurrentMax(@Nullable BlockEntity aTarget) {
		return 1440; // upstream :37 verbatim
	}
}
