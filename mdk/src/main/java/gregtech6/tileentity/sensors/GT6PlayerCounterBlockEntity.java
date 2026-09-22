package gregtech6.tileentity.sensors;

import javax.annotation.Nullable;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * The Player Counter Sensor (task p34-sensors-trivial-14 row ⑮) — the port of
 * MultiTileEntityPlayerCounter.java:32-49. Upstream read
 * {@code ((WorldServer)worldObj).func_73046_m()} — the obfuscated MinecraftServer — for
 * {@code getCurrentPlayerCount()/getMaxPlayers()} (:34-42); the modern face is
 * {@code Level.getServer()} → {@code MinecraftServer.getPlayerCount()} (MinecraftServer
 * .java:958) / {@code getMaxPlayers()} (:962). The null-server guard (offline BE, the
 * client side) answers 0 — the upstream cast face has no such branch, the port's
 * null-tolerance form.
 */
public class GT6PlayerCounterBlockEntity extends GTSensorBlockEntity {

	/** BET factory for BlockEntityType.Builder.of — resolves the type through the registry at runtime (the GTCrankBlockEntity shape). */
	public GT6PlayerCounterBlockEntity(net.minecraft.core.BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
		this(null, aPos, aState);
	}

	/** Full constructor — also the offline (test) entry: a null type falls back to the shared registry type. */
	public GT6PlayerCounterBlockEntity(@Nullable BlockEntityType<?> aType, net.minecraft.core.BlockPos aPos, net.minecraft.world.level.block.state.BlockState aState) {
		super(aType != null ? aType : gregtech6.registry.GTBlockEntities.PLAYERCOUNTER_BE.get(), aPos, aState);
	}

	@Override
	public String getTileEntityName() {
		return "playercounter";
	}

	@Override
	public long getCurrentValue(@Nullable BlockEntity aTarget) {
		MinecraftServer tServer = server();
		return tServer == null ? 0 : tServer.getPlayerCount(); // upstream :34-36
	}

	@Override
	public long getCurrentMax(@Nullable BlockEntity aTarget) {
		MinecraftServer tServer = server();
		return tServer == null ? 0 : tServer.getMaxPlayers(); // upstream :39-41
	}

	@Nullable
	private MinecraftServer server() {
		return hasLevel() ? getLevel().getServer() : null;
	}
}
