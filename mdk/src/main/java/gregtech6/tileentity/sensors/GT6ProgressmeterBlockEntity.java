package gregtech6.tileentity.sensors;

import javax.annotation.Nullable;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import gregtech6.gui.machines.GTBasicMachineMenu;

/**
 * The Progress Sensor (task p26-sensors-core pioneer ①) — the port of
 * MultiTileEntityProgressmeter.java:38-79 (the ≈40-line per-sensor典型). The read face:
 * upstream {@code ITileEntityProgress} (the gregapi interface the port never grew — the
 * progress face lives on the menu Host here) maps onto {@link GTBasicMachineMenu.Host}
 * {@code getProgress()/getMaxProgress()} (GTBasicMachineMenu.java:105/:107), which both
 * the single-block machine adapter and the multiblock base implement directly — so every
 * ticking GT6 machine is measurable, upstream :44/:53 verbatim in spirit. The mob-spawner
 * arm (:45-48/:54-57, the spawnDelay read) is CUT declared: the 1.20.1 mojmap
 * {@code BaseSpawner.spawnDelay} is private (the compile-proof) and the accessor face
 * differs per leg — the spawner metering is the render-pool-adjacent cut of this card.
 */
public class GT6ProgressmeterBlockEntity extends GTSensorBlockEntity {

	/** BET factory for BlockEntityType.Builder.of — resolves the type through the registry at runtime (the GTCrankBlockEntity shape). */
	public GT6ProgressmeterBlockEntity(net.minecraft.core.BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — also the offline (test) entry: a null type falls back to the shared registry type. */
	public GT6ProgressmeterBlockEntity(@Nullable BlockEntityType<?> aType, net.minecraft.core.BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
		super(aType != null ? aType : gregtech6.registry.GTBlockEntities.PROGRESSMETER_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "progressmeter";
	}

	@Override
	public long getCurrentValue(@Nullable BlockEntity aTarget) {
		if (aTarget instanceof GTBasicMachineMenu.Host tHost) return tHost.getProgress(); // upstream :44
		return 0;
	}

	@Override
	public long getCurrentMax(@Nullable BlockEntity aTarget) {
		if (aTarget instanceof GTBasicMachineMenu.Host tHost) return tHost.getMaxProgress(); // upstream :53
		return 0;
	}
}
