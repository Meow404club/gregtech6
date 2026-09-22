package gregtech6.tileentity.sensors;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * The Luminometer Sensor (task p34-sensors-trivial-14 row ②) — the port of
 * MultiTileEntityLuminometer.java:33-60. The read is the upstream
 * {@code getBlockLightValue(x, y, z)} (:37, BLOCK light only, 0-15) at the probe
 * position — the vanilla-1.20.1 face is
 * {@code Level.getBrightness(LightLayer.BLOCK, pos)} (BlockAndTintGetter.java:22, the
 * layer-listener dispatch). The {@code getLightOpacity() == NONE} override (:53) is CUT
 * declared: it tuned the upstream thin-plate self-shadowing, while this port's carrier
 * is the shared full-cube {@code GTSensorBlock} (the zero-diff mandate on the existing
 * three rows) and the sample reads the NEIGHBOUR position, not the sensor block itself.
 */
public class GT6LuminometerBlockEntity extends GTSensorBlockEntity {

	/** BET factory for BlockEntityType.Builder.of — resolves the type through the registry at runtime (the GTCrankBlockEntity shape). */
	public GT6LuminometerBlockEntity(BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — also the offline (test) entry: a null type falls back to the shared registry type. */
	public GT6LuminometerBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
		super(aType != null ? aType : gregtech6.registry.GTBlockEntities.LUMINOMETER_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "luminometer";
	}

	@Override
	public long getCurrentValue(@Nullable BlockEntity aTarget) {
		if (!hasLevel()) return 0;
		BlockPos tPos = getBlockPos().relative(Direction.from3DDataValue(mSecondFacing));
		return getLevel().getBrightness(LightLayer.BLOCK, tPos); // upstream :37 verbatim in the port's light face
	}

	@Override
	public long getCurrentMax(@Nullable BlockEntity aTarget) {
		return 15; // upstream :40 verbatim
	}
}
