package gregtech6.tileentity;

import java.lang.reflect.Method;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.block.entity.BlockEntityType;

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
 *
 * <p>On the 1.21.1 leg (MDG {@code unitTest}, task p15-m4-test-infra) the test JVM boots
 * through FML itself (junit-fml LauncherSessionListener), so by the time the first
 * {@code @BeforeAll} runs the registries are fully populated <em>and frozen</em> — the
 * 21.1 {@code BlockEntity} ctor validates its type/state pair
 * ({@code validateBlockState → getType().isValid()}) and the {@code BlockEntityType} ctor
 * takes an intrusive holder ({@code createIntrusiveHolder → validateWrite()}).
 * Synthetic fixture BETs therefore need the registry writable again, mirroring the
 * production registration window; assertions never touch the freeze state.
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
		unfreezeBlockEntityTypeRegistry();
	}

	/**
	 * Reopens the write window of the block-entity-type registry so fixture BETs
	 * ({@code BlockEntityType.Builder.of(...).build(null)}) stay constructible after an
	 * FML boot froze it (task p15-m4-test-infra). No-op when already unfrozen, and a
	 * silent no-op on any runtime where the method shape drifts.
	 */
	public static void unfreezeBlockEntityTypeRegistry() {
		try {
			var tRegistry = BuiltInRegistries.BLOCK_ENTITY_TYPE;
			Method tUnfreeze = tRegistry.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(tRegistry);
		} catch (Throwable ignored) {
			// fixture construction falls back to vanilla BETs; never mask a test assertion
		}
	}
}
