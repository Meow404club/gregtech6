package gregtech6.tileentity.sensors;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import gregapi.tileentity.temperature.ITileEntityTemperature;
import gregtech6.util.UT6;

/**
 * The Thermometer Sensor (task p34-sensors-trivial-14 row ①) — the port of
 * MultiTileEntityThermometer.java:35-71. The primary read face is the upstream
 * {@code ITileEntityTemperature} instanceof (:40-41/:53-54) — the gregapi interface the
 * crucible chain already carries (TileEntityCrucible.java:89/:316/:321
 * getTemperatureValue/getTemperatureMax, the P26 seam this card hangs on, side byte =
 * the target face {@code UT6.OPOS[mSecondFacing]}).
 *
 * <p>Fallback arms, against the upstream body:
 * <ul>
 * <li>the IC2 reactor/chamber heat reads (:43-50, {@code getHeat()/5}) are CUT declared —
 *     no IC2 in the port;</li>
 * <li>the {@code WD.temperature} ambient read (:42) lands as the biome-climate form the
 *     smeltery envTemp already pinned (TileEntitySmeltery.java:540-546 —
 *     {@code max(1, C-3+biomeTemp*20)}, WD.java:413, C=273); the burning-block/neighbour
 *     scan of upstream WD.temperature (WD.java:445-452) is CUT declared (the WD-port
 *     face).</li>
 * </ul>
 * A non-temperature neighbour keeps the upstream zero max (:55-70 tail) — the env arm
 * feeds {@link #getCurrentValue} ONLY.
 */
public class GT6ThermometerBlockEntity extends GTSensorBlockEntity {

	/** The Kelvin offset of the envTemp formula (WD.java:413 {@code C - 3}, CS.C = 273). */
	public static final long KELVIN_OFFSET = 270;

	/** The seam-less env answer (upstream CS.DEF_ENV_TEMP = C + 20 = 293, the Crucible :108 local-constant form). */
	public static final long DEF_ENV_TEMP = 293;

	/** BET factory for BlockEntityType.Builder.of — resolves the type through the registry at runtime (the GTCrankBlockEntity shape). */
	public GT6ThermometerBlockEntity(BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — also the offline (test) entry: a null type falls back to the shared registry type. */
	public GT6ThermometerBlockEntity(@Nullable BlockEntityType<?> aType, BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
		super(aType != null ? aType : gregtech6.registry.GTBlockEntities.THERMOMETER_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "thermometer";
	}

	@Override
	public long getCurrentValue(@Nullable BlockEntity aTarget) {
		if (aTarget instanceof ITileEntityTemperature) return ((ITileEntityTemperature)aTarget).getTemperatureValue(UT6.OPOS[mSecondFacing]); // upstream :40-41
		return envTemp(); // upstream :42, the ambient fallback
	}

	@Override
	public long getCurrentMax(@Nullable BlockEntity aTarget) {
		if (aTarget instanceof ITileEntityTemperature) return ((ITileEntityTemperature)aTarget).getTemperatureMax(UT6.OPOS[mSecondFacing]); // upstream :53-54
		return 0; // upstream :70 verbatim — the env arm has NO max
	}

	/** The ambient fallback (WD.envTemp :404-406 over the vanilla biome climate, the Smeltery live form). */
	private long envTemp() {
		if (!hasLevel()) return DEF_ENV_TEMP;
		BlockPos tPos = getBlockPos().relative(Direction.from3DDataValue(mSecondFacing));
		float tBiomeTemp = getLevel().getBiome(tPos).value().getBaseTemperature();
		return Math.max(1, KELVIN_OFFSET + (long)(tBiomeTemp * 20));
	}
}
