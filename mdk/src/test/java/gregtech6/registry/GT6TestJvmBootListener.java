package gregtech6.registry;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

/**
 * One vanilla-registry boot per test fork JVM, before ANY test class loads
 * (junit-platform {@link LauncherSessionListener}, ServiceLoader-discovered via
 * {@code META-INF/services}).
 *
 * <p>The maxParallelForks=6 lottery (known_bugs.registry-scope-preexisting-reds): a fork
 * whose FIRST registry-touching class loads without a prior boot poisons that JVM for
 * every class scheduled after it — the first touch lands in a vanilla registry class's
 * {@code <clinit>} (e.g. GT6BeamBlocks → Registries → BuiltInRegistries), whose
 * MappedRegistry constructor throws {@code IllegalArgumentException: Not bootstrapped}
 * (Bootstrap.checkBootstrapCalled), and a failed {@code <clinit>} is permanent for the
 * JVM: every later touch of that class (or ForgeRegistries, which wraps it) dies with
 * {@code NoClassDefFoundError}. One fragile first-touch class took 13 classes / 42 tests
 * down with it in the filtered {@code gregtech6.registry.*} batch, with the victim set
 * rotating per run as the class-to-fork assignment shifts. The per-class {@code @BeforeAll}
 * brackets stay the house discipline for the material/recipe-map universes (the hermetic
 * rules — those need reset semantics, not a JVM-wide boot); the vanilla/forge registry
 * boot however is JVM-global and idempotent, so booting it once per fork before any class
 * loads removes the order lottery outright instead of whack-a-mole brackets per fragile
 * class (12 registry test classes carried no boot support at all and only lived by the
 * fork draw).
 *
 * <p>Posture copied verbatim from the fixture form (GT6RecyclingProcessingTest.bootOffline):
 * tryDetectVersion FIRST (a bare-JVM first boot poisons DataFixers for every later suite
 * in this JVM — the GT6AxisTakeoverCensusTest.initMaterialSystem note), and the
 * Forge-patched boot may throw offline at NetworkHooks after the registries are already
 * filled — so the throw is ignored.
 */
public final class GT6TestJvmBootListener implements LauncherSessionListener {

	@Override
	public void launcherSessionOpened(LauncherSession aSession) {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap(); // the Forge-patched boot throws offline at NetworkHooks — the registries are ready by then
		} catch (Throwable ignored) {}
	}
}
