package gregtech6.tileentity.sensors;

import javax.annotation.Nullable;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import gregtech6.tileentity.connectors.GTWireBlockEntity;

/**
 * The Electrometer Sensor (task p26-sensors-core pioneer ③) — the port of
 * MultiTileEntityElectrometer.java:45-98. The primary read face is the wire wattage:
 * upstream {@code MultiTileEntityWireElectric.mWattageLast} (:51) maps verbatim onto
 * {@link GTWireBlockEntity#mWattageLast} (the EU/t the wire moved in its last transfer
 * window, the p7 energy-network port) with the {@code mAmperage * mVoltage} break-down
 * scale (:69). The IC2 NodeStats fallback (:53-63/:71-79) has no port counterpart — the
 * port's energy face is {@code ITileEntityEnergy} (the root BE base implements it, root
 * src gregapi ... the card anchor), which carries no flow telemetry, so a non-wire
 * neighbour reads 0 (declared cut; the wire grid is the electrometer's live surface).
 */
public class GT6ElectrometerBlockEntity extends GTSensorBlockEntity {

	/** BET factory for BlockEntityType.Builder.of — resolves the type through the registry at runtime (the GTCrankBlockEntity shape). */
	public GT6ElectrometerBlockEntity(net.minecraft.core.BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — also the offline (test) entry: a null type falls back to the shared registry type. */
	public GT6ElectrometerBlockEntity(@Nullable BlockEntityType<?> aType, net.minecraft.core.BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
		super(aType != null ? aType : gregtech6.registry.GTBlockEntities.ELECTROMETER_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "electrometer";
	}

	@Override
	public long getCurrentValue(@Nullable BlockEntity aTarget) {
		if (aTarget instanceof GTWireBlockEntity tWire) return tWire.mWattageLast; // upstream :51 verbatim
		return 0;
	}

	@Override
	public long getCurrentMax(@Nullable BlockEntity aTarget) {
		if (aTarget instanceof GTWireBlockEntity tWire) return tWire.mAmperage * tWire.mVoltage; // upstream :69
		return 0;
	}
}
