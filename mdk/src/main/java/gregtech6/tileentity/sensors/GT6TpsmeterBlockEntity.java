package gregtech6.tileentity.sensors;

import javax.annotation.Nullable;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * The TPS Sensor (task p34-sensors-trivial-14 row ⑭) — the port of
 * MultiTileEntityTPSmeter.java:35-58. The read is the wall-clock milli-TPS: every
 * {@code getTickRate()} (=20, :24 verbatim) server ticks the upstream onTick2 (:39-45)
 * measures the elapsed milliseconds and folds them into
 * {@code (rate * 100000) / elapsed} (:43 — a perfect 20 TPS server reads 2000, a 10 TPS
 * one 1000), zero-elapsed guarded to 2000. The measurement rides the sample tick
 * (before the sample pair reads it, the upstream super.onTick2 ordering), the base
 * {@code onTick} rate gate is the upstream :40 gate verbatim.
 */
public class GT6TpsmeterBlockEntity extends GTSensorBlockEntity {

	/** Upstream :36 — the last sample's wall clock. */
	public long mTime = System.currentTimeMillis();

	/** Upstream :36 — the folded milli-TPS reading (2000 = 20.00 TPS). */
	public long mCurrentTime = 2000;

	/** BET factory for BlockEntityType.Builder.of — resolves the type through the registry at runtime (the GTCrankBlockEntity shape). */
	public GT6TpsmeterBlockEntity(net.minecraft.core.BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — also the offline (test) entry: a null type falls back to the shared registry type. */
	public GT6TpsmeterBlockEntity(@Nullable BlockEntityType<?> aType, net.minecraft.core.BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
		super(aType != null ? aType : gregtech6.registry.GTBlockEntities.TPSMETER_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "tpsmeter";
	}

	@Override
	public long getTickRate() {
		return 20; // upstream :24 verbatim
	}

	@Override
	protected void sampleTick() {
		long tTime = mTime; // upstream :41-43 — the wall-clock fold, BEFORE the sample pair reads it
		mTime = System.currentTimeMillis();
		mCurrentTime = (mTime - tTime > 0 ? (getTickRate() * 100000) / (mTime - tTime) : 2000);
		super.sampleTick();
	}

	@Override
	public long getCurrentValue(@Nullable BlockEntity aTarget) {
		return mCurrentTime; // upstream :47
	}

	@Override
	public long getCurrentMax(@Nullable BlockEntity aTarget) {
		return 2000; // upstream :50 verbatim — the 20 TPS reference
	}
}
