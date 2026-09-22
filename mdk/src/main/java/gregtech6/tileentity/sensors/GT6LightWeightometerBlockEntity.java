package gregtech6.tileentity.sensors;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * The Light Weight-O-Meter Sensor (task p34-sensors-trivial-14 row ⑩) — the port of
 * MultiTileEntityWeightometerLight.java:32-67, the GRAM scale: upstream returns
 * {@code (long)(rWeightKG * 1000)} (:58). The measured quantity is the inventory mass:
 * upstream reads {@code ITileEntityWeight.getWeightValue} (:36-37) or sums
 * {@code OM.weight(stack)} over the inventory (:38-47, the MAX_WEIGHT clip
 * {@code (B[16]-1)/1000} kg at :34/:56). The port has NEITHER face yet: no
 * {@code ITileEntityWeight} implementor exists, and the ItemStack→kg projection
 * ({@code OM.weight}) lives in the crucible material-stack domain
 * ({@code CruciblePhysics.weight(List<OreDictMaterialStack>)}, OM.java:82-95 anchor)
 * with no ItemStack decomposition — the meter rides the declared 0 baseline (the
 * {@link GT6ElectrometerBlockEntity} non-wire form) until the mass-projection pool card
 * lands; the constant max (:60-62 {@code B[16]-1}) and the sensor block itself stay
 * fully live.
 */
public class GT6LightWeightometerBlockEntity extends GTSensorBlockEntity {

	/** Upstream :60-62 verbatim — the {@code B[16]-1} full scale, target-independent. */
	public static final long MAX_WEIGHT_READING = 65535;

	/** BET factory for BlockEntityType.Builder.of — resolves the type through the registry at runtime (the GTCrankBlockEntity shape). */
	public GT6LightWeightometerBlockEntity(BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — also the offline (test) entry: a null type falls back to the shared registry type. */
	public GT6LightWeightometerBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
		super(aType != null ? aType : gregtech6.registry.GTBlockEntities.LIGHTWEIGHTOMETER_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "lightweightometer";
	}

	@Override
	public long getCurrentValue(@Nullable BlockEntity aTarget) {
		return 0; // upstream :62 minus the un-ported mass faces (declared above) — grams
	}

	@Override
	public long getCurrentMax(@Nullable BlockEntity aTarget) {
		return MAX_WEIGHT_READING; // upstream :65 verbatim
	}
}
