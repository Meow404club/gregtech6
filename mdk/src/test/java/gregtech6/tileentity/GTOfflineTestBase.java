package gregtech6.tileentity;

import java.lang.reflect.Method;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
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

	// -------------------------------------------------------------------------
	// the item-fixture seat (task p35 — lifted verbatim from GT6BatteryItemTest, the
	// forge wrapper latches the vanilla ITEM registry write window; on this JVM the
	// latch fields may be unreachable (module access), in which case the fixture
	// consumers ASSUME-SKIP)
	// -------------------------------------------------------------------------

	/** The lazy latch holder — the class-init MUST stay lazy: touching BuiltInRegistries before the @BeforeAll Bootstrap fails the registry class. */
	private static final class ItemLatch {
		static final sun.misc.Unsafe UNSAFE;
		static final long LOCKED_OFFSET;
		static final long FROZEN_OFFSET;
		static final boolean ARMED;
		static {
			sun.misc.Unsafe tUnsafe = null;
			long tLocked = 0, tFrozen = 0;
			boolean tArmed = true;
			try {
				java.lang.reflect.Field tUnsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
				tUnsafeField.setAccessible(true);
				tUnsafe = (sun.misc.Unsafe) tUnsafeField.get(null);
				Class<?> tClass = net.minecraft.core.registries.BuiltInRegistries.ITEM.getClass();
				tLocked = tUnsafe.objectFieldOffset(findNestedField(tClass, "locked"));
				tFrozen = tUnsafe.objectFieldOffset(findNestedField(tClass, "frozen"));
			} catch (Throwable ignored) {
				tArmed = false; // the telemetry leg
			}
			UNSAFE = tUnsafe;
			LOCKED_OFFSET = tLocked;
			FROZEN_OFFSET = tFrozen;
			ARMED = tArmed;
		}
	}

	/** The latch fields live on wrapper superclasses — walk up (getDeclaredField sees one class only). */
	private static java.lang.reflect.Field findNestedField(Class<?> aClass, String aName) throws NoSuchFieldException {
		for (Class<?> tWalk = aClass; tWalk != null; tWalk = tWalk.getSuperclass()) {
			try {
				return tWalk.getDeclaredField(aName);
			} catch (NoSuchFieldException ignored) {
				// keep walking
			}
		}
		throw new NoSuchFieldException(aName + " (walked " + aClass + " up)");
	}

	static void unlockItemRegistry() {
		ItemLatch.UNSAFE.putBoolean(net.minecraft.core.registries.BuiltInRegistries.ITEM, ItemLatch.LOCKED_OFFSET, false);
		ItemLatch.UNSAFE.putBoolean(net.minecraft.core.registries.BuiltInRegistries.ITEM, ItemLatch.FROZEN_OFFSET, false);
	}

	static void lockItemRegistry() {
		ItemLatch.UNSAFE.putBoolean(net.minecraft.core.registries.BuiltInRegistries.ITEM, ItemLatch.FROZEN_OFFSET, true);
		ItemLatch.UNSAFE.putBoolean(net.minecraft.core.registries.BuiltInRegistries.ITEM, ItemLatch.LOCKED_OFFSET, true);
	}

	/** The ItemStack ctor needs a registry DELEGATE (ForgeRegistry.getDelegateOrThrow), so the fixtures register under fixture keys with the latch momentarily open. */
	protected static <T extends Item> T registerItemFixture(String aKey, java.util.function.Supplier<T> aItem) {
		org.junit.jupiter.api.Assumptions.assumeTrue(ItemLatch.ARMED, "the offline registry latch is unreachable on this JVM");
		unlockItemRegistry();
		try {
			return net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.ITEM,
					new net.minecraft.resources.ResourceLocation("gt6", aKey), aItem.get());
		} finally {
			lockItemRegistry();
		}
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
