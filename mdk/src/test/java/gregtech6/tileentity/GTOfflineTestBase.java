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
 * <p>On the 1.21.1 leg (MDG {@code unitTest}, task m4-test-infra) the test JVM boots
 * through FML itself (junit-fml LauncherSessionListener), so by the time the first
 * {@code @BeforeAll} runs the registries are fully populated <em>and frozen</em> — the
 * 21.1 {@code BlockEntity} ctor validates its type/state pair
 * ({@code validateBlockState → getType().isValid()}) and the {@code BlockEntityType} ctor
 * takes an intrusive holder ({@code createIntrusiveHolder → validateWrite()}).
 * Synthetic fixture BETs therefore need the registry writable again, mirroring the
 * production registration window; assertions never touch the freeze state.
 *
 * <p><b>Accounting discipline (task offline-testbase-latch-neo)</b>: every domain test
 * report must distinguish T(ested)/F(ailed)/S(kipped) per leg — a class with skips is
 * <em>not</em> true-run green on that leg. History: before 2026-10-08 the item latch
 * was deterministically ARMED=false on the whole 1.21.1 leg (its registries carry no
 * {@code locked} field), so every {@link #registerItemFixture} consumer silently
 * assume-skipped there while the leg still read "0 failures".
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
	// latch opens the vanilla ITEM registry write window. Two runtime shapes:
	// forge 1.20.1 wraps vanilla registries in NamespacedWrapper (its own 'locked'
	// gate on top of vanilla 'frozen'); neo 1.21.1 leaves them bare MappedRegistry
	// where 'frozen' alone guards both the intrusive-holder ctor call and
	// Registry.register, so 'locked' does not exist and stays skipped (task
	// offline-testbase-latch-neo). If both flags are unreachable the latch stays
	// unarmed and the fixture consumers ASSUME-SKIP.)
	// -------------------------------------------------------------------------

	/** The lazy latch holder — the class-init MUST stay lazy: touching BuiltInRegistries before the @BeforeAll Bootstrap fails the registry class. */
	private static final class ItemLatch {
		static final sun.misc.Unsafe UNSAFE;
		static final long LOCKED_OFFSET;
		static final long FROZEN_OFFSET;
		static final boolean ARMED;
		static {
			sun.misc.Unsafe tUnsafe = null;
			long tLocked = -1, tFrozen = -1;
			boolean tArmed = true;
			try {
				java.lang.reflect.Field tUnsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
				tUnsafeField.setAccessible(true);
				tUnsafe = (sun.misc.Unsafe) tUnsafeField.get(null);
				Class<?> tClass = net.minecraft.core.registries.BuiltInRegistries.ITEM.getClass();
				try {
					tLocked = tUnsafe.objectFieldOffset(findNestedField(tClass, "locked"));
				} catch (NoSuchFieldException ignored) {
					// the 21.1 shape: no 'locked' gate anywhere on the chain — 'frozen' is the sole write guard
				}
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

	/**
	 * The latch fields live on wrapper superclasses — walk up (getDeclaredField sees one
	 * class only). Shared with the per-class latch holders (the
	 * GT6LargeMachineTexDatagenTest.BlockLatch / GT6MachineBlockItemTest.BlockLatch shapes)
	 * so the walker does not live in three files.
	 */
	public static java.lang.reflect.Field findNestedField(Class<?> aClass, String aName) throws NoSuchFieldException {
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
		var tRegistry = net.minecraft.core.registries.BuiltInRegistries.ITEM;
		if (ItemLatch.LOCKED_OFFSET != -1) ItemLatch.UNSAFE.putBoolean(tRegistry, ItemLatch.LOCKED_OFFSET, false);
		ItemLatch.UNSAFE.putBoolean(tRegistry, ItemLatch.FROZEN_OFFSET, false);
	}

	static void lockItemRegistry() {
		var tRegistry = net.minecraft.core.registries.BuiltInRegistries.ITEM;
		ItemLatch.UNSAFE.putBoolean(tRegistry, ItemLatch.FROZEN_OFFSET, true);
		if (ItemLatch.LOCKED_OFFSET != -1) ItemLatch.UNSAFE.putBoolean(tRegistry, ItemLatch.LOCKED_OFFSET, true);
	}

	/** The item latch arm state — the per-class telemetry assumes (the bare {@code assumeTrue(ARMED)} mirrors) read this. */
	public static boolean itemLatchArmed() { return ItemLatch.ARMED; }

	/** The ItemStack ctor needs a registry DELEGATE (ForgeRegistry.getDelegateOrThrow), so the fixtures register under fixture keys with the latch momentarily open. Public: fixture seats also live in classes outside the {@code GTOfflineTestBase} tree (the GT6QuMachinePairE2eTest form). */
	public static <T extends Item> T registerItemFixture(String aKey, java.util.function.Supplier<T> aItem) {
		org.junit.jupiter.api.Assumptions.assumeTrue(ItemLatch.ARMED, "the offline registry latch is unreachable on this JVM");
		unlockItemRegistry();
		try {
			return net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.ITEM,
					net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6", aKey), aItem.get());
		} finally {
			lockItemRegistry();
		}
	}

	// -------------------------------------------------------------------------
	// the block-fixture seat (the ItemLatch shape over the BLOCK registry — the
	// per-class BlockLatch mirrors GT6MachineBlockItemTest / GTAxleTintArmTest /
	// GT6LargeMachineTexDatagenTest fold onto this; task probeitem-latch-hygiene).
	// Same lazy class-init discipline and the same locked-optional ARM rule (the
	// frozen offset is the sole ARM condition, the locked offset -1 sentinel skips
	// its poke — task offline-testbase-latch-neo).
	// -------------------------------------------------------------------------

	/** The lazy latch holder over {@code BuiltInRegistries.BLOCK} (the ItemLatch shape verbatim). */
	private static final class BlockLatch {
		static final sun.misc.Unsafe UNSAFE;
		static final long LOCKED_OFFSET;
		static final long FROZEN_OFFSET;
		static final boolean ARMED;
		static {
			sun.misc.Unsafe tUnsafe = null;
			long tLocked = -1, tFrozen = -1;
			boolean tArmed = true;
			try {
				java.lang.reflect.Field tUnsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
				tUnsafeField.setAccessible(true);
				tUnsafe = (sun.misc.Unsafe) tUnsafeField.get(null);
				Class<?> tClass = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getClass();
				try {
					tLocked = tUnsafe.objectFieldOffset(findNestedField(tClass, "locked"));
				} catch (NoSuchFieldException ignored) {
					// the 21.1 shape: no 'locked' gate anywhere on the chain — 'frozen' is the sole write guard
				}
				tFrozen = tUnsafe.objectFieldOffset(findNestedField(tClass, "frozen"));
			} catch (Throwable ignored) {
				tArmed = false; // the telemetry leg (no registered fixtures)
			}
			UNSAFE = tUnsafe;
			LOCKED_OFFSET = tLocked;
			FROZEN_OFFSET = tFrozen;
			ARMED = tArmed;
		}
	}

	/** The block latch arm state (the GT6LargeMachineTexDatagenTest graceful {@code if (ARMED)} form reads this — no assumption there). */
	public static boolean blockLatchArmed() { return BlockLatch.ARMED; }

	/** Runs the work inside an open BLOCK-registry write window (construct/registered blocks, no assume — the caller picks its own unarmed posture). */
	public static <T> T underBlockWriteWindow(java.util.function.Supplier<T> aWork) {
		var tRegistry = net.minecraft.core.registries.BuiltInRegistries.BLOCK;
		if (BlockLatch.LOCKED_OFFSET != -1) BlockLatch.UNSAFE.putBoolean(tRegistry, BlockLatch.LOCKED_OFFSET, false);
		BlockLatch.UNSAFE.putBoolean(tRegistry, BlockLatch.FROZEN_OFFSET, false);
		try {
			return aWork.get();
		} finally {
			BlockLatch.UNSAFE.putBoolean(tRegistry, BlockLatch.FROZEN_OFFSET, true);
			if (BlockLatch.LOCKED_OFFSET != -1) BlockLatch.UNSAFE.putBoolean(tRegistry, BlockLatch.LOCKED_OFFSET, true);
		}
	}

	/** The block fixture under a UNIQUE fixture key with the latch momentarily open (the registerItemFixture shape over BLOCK; assumes armed). */
	public static <T extends net.minecraft.world.level.block.Block> T registerBlockFixture(String aKey, java.util.function.Supplier<T> aBlock) {
		org.junit.jupiter.api.Assumptions.assumeTrue(BlockLatch.ARMED, "the offline block-registry latch is unreachable on this JVM");
		return underBlockWriteWindow(() -> net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.BLOCK,
				new net.minecraft.resources.ResourceLocation("gt6", aKey), aBlock.get()));
	}

	/**
	 * Reopens the write window of the block-entity-type registry so fixture BETs
	 * ({@code BlockEntityType.Builder.of(...).build(null)}) stay constructible after an
	 * FML boot froze it (task m4-test-infra). No-op when already unfrozen, and a
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
