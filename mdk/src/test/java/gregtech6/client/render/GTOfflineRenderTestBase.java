package gregtech6.client.render;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import org.junit.jupiter.api.BeforeAll;

/**
 * Offline boot for the render tests (same recipe as gregtech6.tileentity.GTOfflineTestBase,
 * mirrored package-locally because its @BeforeAll is package-private). Vanilla registries
 * need {@link SharedConstants#tryDetectVersion()} + {@link Bootstrap#bootStrap()}; the
 * Forge-patched bootStrap ends with {@code NetworkHooks.init()}, which throws offline —
 * by then vanilla blocks are registered (frozen), which is the state these tests need.
 *
 * <p>ModelData/ModelProperty are pure data classes (Guava only) and are offline-testable;
 * the baked-model tests avoid constructing BakedQuads entirely by asserting on sentinel
 * list identities.
 */
public abstract class GTOfflineRenderTestBase {

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
