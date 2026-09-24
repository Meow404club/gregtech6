package gregtech6.tileentity.sensors;

import javax.annotation.Nullable;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import gregtech6.tileentity.energy.GTAxleBlockEntity;
import gregtech6.tileentity.energy.GTGearBoxBlockEntity;

/**
 * The Tachometer Sensor (task p37-sensors-3, the pool closure) — the port of
 * MultiTileEntityTachometer.java:37-73. The read face: the axle/gearbox RU bookkeeping
 * the P34 pooled javadoc called a missing seam is LIVE since p12/p28 (the census-erratum
 * corrigendum: the P34 "the axles cut at GtAxleBE" note predates the kinetics build —
 * P28 landed the carriers, P37 lights the sensor). Upstream reads the last-tick
 * transferred RU of an {@code MultiTileEntityAxle} (:43) or {@code MultiTileEntityGearBox}
 * (:44) against the product ratings (:50 axle {@code mPower * mSpeed}; :51 gearbox
 * {@code mMaxThroughPut * 16}); this port's carriers expose the same public fields
 * (GTAxleBlockEntity.java:103-115 mSpeed/mPower/mTransferredLast — the tick lag :166 is
 * the upstream :95 tachometer readout; GTGearBoxBlockEntity.java:99/:105 mMaxThroughPut/
 * mTransferredLast — both already read by the /gt6engine stat channel). Every other
 * neighbour answers 0 (upstream :45/:52).
 */
public class GT6TachometerBlockEntity extends GTSensorBlockEntity {

	/** BET factory for BlockEntityType.Builder.of — resolves the type through the registry at runtime (the GTCrankBlockEntity shape). */
	public GT6TachometerBlockEntity(net.minecraft.core.BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — also the offline (test) entry: a null type falls back to the shared registry type. */
	public GT6TachometerBlockEntity(@Nullable BlockEntityType<?> aType, net.minecraft.core.BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
		super(aType != null ? aType : gregtech6.registry.GTBlockEntities.TACHOMETER_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "tachometer";
	}

	@Override
	public long getCurrentValue(@Nullable BlockEntity aTarget) {
		if (aTarget instanceof GTAxleBlockEntity tAxle) return tAxle.mTransferredLast; // upstream :43
		if (aTarget instanceof GTGearBoxBlockEntity tBox) return tBox.mTransferredLast; // upstream :44
		return 0; // upstream :45
	}

	@Override
	public long getCurrentMax(@Nullable BlockEntity aTarget) {
		if (aTarget instanceof GTAxleBlockEntity tAxle) return tAxle.mPower * tAxle.mSpeed; // upstream :50
		if (aTarget instanceof GTGearBoxBlockEntity tBox) return tBox.mMaxThroughPut * 16; // upstream :51
		return 0; // upstream :52
	}
}
