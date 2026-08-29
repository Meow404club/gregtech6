package gregtech6.tileentity;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import org.junit.jupiter.api.BeforeAll;

/**
 * Offline boot for mdk unit tests. Vanilla registries need
 * {@link SharedConstants#tryDetectVersion()} (otherwise getCurrentVersion throws
 * "Game version not set" the moment a MappedRegistry initializes) and
 * {@link Bootstrap#bootStrap()} (MappedRegistry.<init> guards on it).
 *
 * <p>The Forge-patched bootStrap ends with {@code NetworkHooks.init()}, which needs a
 * live network stack and throws offline — but by then vanilla items/blocks are
 * registered and the registries are frozen, which is exactly the state the unit
 * tests need. Consequence: constructing NEW Blocks is impossible after the boot
 * (intrusive holder registration hits the frozen registry), so offline fixtures use
 * vanilla blocks.
 */
public abstract class GTOfflineTestBase {

	@BeforeAll
	static void bootVanillaOffline() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// NetworkHooks.init() failure is expected offline; registries are ready by now.
		}
	}
}
