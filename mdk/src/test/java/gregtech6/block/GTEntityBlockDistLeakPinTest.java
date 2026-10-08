package gregtech6.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import gregtech6.tileentity.GTOfflineTestBase;

/**
 * The dedicated-server dist-leak pin (task mui-fill-teardown-distleak). The /fill teardown
 * of any MUI machine used to detonate on a dedicated server:
 * {@code GTEntityBlock.beCanDrop} probed with {@code Class.getMethod("canDrop", int.class)},
 * the class walk MISSED (the census: only TileEntityAdvancedCraftingTable :774 declares
 * it), so the JVM fell back to enumerating every interface default method — and building
 * those Method objects resolves their signatures. The default IUIHolder.createScreen /
 * GT6MuiMachine.createScreen names ModularScreen ({@code @OnlyIn(Dist.CLIENT)}, fork
 * screen/ModularScreen.java:63), FML's RuntimeDistCleaner refused the load —
 * "Attempted to load class brachy/modularui/screen/ModularScreen for invalid dist
 * DEDICATED_SERVER" (gt6_rs_session_1201-forge_qumachines-8aa9515d.log:41195, both legs),
 * the command died, the /fill teardown FAILED (qu_machines expect16).
 *
 * <p>The fix is two mechanisms, and the pins prove BOTH (either alone leaves a vector):
 * <ol>
 * <li>the declared-only walk ({@link GTEntityBlock#probeDeclared}) behind beCanDrop and
 * dropInventory — a {@code getMethod} miss no longer falls into the interface-default
 * enumeration;</li>
 * <li>the {@code @OnlyIn(Dist.CLIENT)} strip on the client-typed declarations themselves
 * (the fork's IUIHolder, the ACT override :855-860 — the act_chain_rcon_red precedent —
 * and now the GT6MuiMachine default): the dedicated server removes them from the bytecode,
 * so even a declared-methods resolution never sees a ModularScreen signature. This pin is
 * the family ratchet: any present or future client-typed declaration without the
 * annotation fails here.</li>
 * </ol>
 * The guard ClassLoader replays the dedicated server by refusing the ModularScreen load by
 * name, the production probes run against the REAL family classes inside that guarded
 * world, and a negative control shows the retired {@code getMethod} strategy detonating
 * under the exact same guard — so the pins cannot pass vacuously.
 *
 * <p>{@link GTOfflineTestBase} is the seat: invoking the production probe initializes the
 * vanilla superclass chain (FeatureElement → Registries → BuiltInRegistries), and an
 * un-bootstrapped JVM dies there (MappedRegistry guards on Bootstrap) — the base class's
 * @BeforeAll bootstraps first and keeps this test from poisoning its JVM-neighbours.
 */
public class GTEntityBlockDistLeakPinTest extends GTOfflineTestBase {

	/** The name the dist cleaner refuses on a dedicated server — blocked verbatim by the guard. */
	static final String DIST_STRIPPED = "brachy.modularui.screen.ModularScreen";

	/**
	 * The dedicated-server replay: child-first over the gregtech6 + brachy class dirs (so
	 * reflective resolution over family classes consults THIS loader), refusing the
	 * ModularScreen load the way RuntimeDistCleaner does, delegating everything else
	 * (vanilla/forge) to the test classpath. The gt6.modularui.classes seam is the build
	 * scripts' feed (build.forge.gradle.kts:269 / build.neoforge.gradle.kts:288).
	 */
	static final class DistGuardLoader extends URLClassLoader {
		DistGuardLoader(URL[] aUrls, ClassLoader aParent) {
			super(aUrls, aParent);
		}

		@Override
		protected synchronized Class<?> loadClass(String aName, boolean aResolve) throws ClassNotFoundException {
			if (DIST_STRIPPED.equals(aName)) {
				throw new ClassNotFoundException("invalid dist DEDICATED_SERVER (guard): " + aName);
			}
			if (aName.startsWith("gregtech6.") || aName.startsWith("brachy.")) {
				Class<?> tLoaded = findLoadedClass(aName);
				if (tLoaded == null) {
					try {
						tLoaded = findClass(aName);
					} catch (ClassNotFoundException aE) {
						return super.loadClass(aName, aResolve); // not in the guard dirs — delegate
					}
				}
				if (aResolve) resolveClass(tLoaded);
				return tLoaded;
			}
			return super.loadClass(aName, aResolve);
		}
	}

	/**
	 * The family census + immunity walk. For every gregtech6 class whose bytecode
	 * references GT6MuiMachine (a raw-byte filesystem scan — the census seed), expanded
	 * down the superclass chain to the full implementor family, two proofs:
	 * <ul>
	 * <li><b>the ratchet</b>: every chain-declared method whose signature names the
	 * dist-stripped class carries {@code @OnlyIn(Dist.CLIENT)} — the annotation the
	 * dedicated server strips the method by, and the exact convention of the fork's
	 * IUIHolder.createScreen and the ACT override. Without it the server bytecode keeps a
	 * ModularScreen signature in the declared-methods array and ANY reflective walk dies
	 * (the getDeclaredMethod shape this pin caught on the first fix draft).</li>
	 * <li><b>the guard walk</b>: chains whose UNSTRIPPED bytecode never mentions the
	 * stripped class (the TileEntityBasicMachine scanner shape — the actual crash) are
	 * probed with the production declared-only walk inside the guard world and must not
	 * touch it. Byte-mentioning chains (client-typed declarations) are proven by the
	 * ratchet instead — the reflection census cannot serve as the bucket here because the
	 * neo leg's FML TRANSFORMER hands out member-stripped classes, while the bytes never
	 * lie.</li>
	 * </ul>
	 * The roster is the negative-account ledger (per-name immunity, in the failure message).
	 */
	@Test
	public void muiMachineFamilySurvivesTheDedicatedServerGuard() throws Exception {
		Class<?> tScreen = Class.forName(DIST_STRIPPED); // the test world carries the class
		List<String> tDetonations = new ArrayList<>();
		try (DistGuardLoader tGuard = guard()) {
			Class<?> tMuiMachine = Class.forName("gregtech6.gui.machines.GT6MuiMachine", false, tGuard);
			Set<String> tFamily = new LinkedHashSet<>();
			for (String tName : muiReferencingClassNames()) {
				Class<?> tClass = Class.forName(tName, false, tGuard);
				for (Class<?> tWalker = tClass; tWalker != null && tMuiMachine.isAssignableFrom(tWalker); tWalker = tWalker.getSuperclass()) {
					tFamily.add(tWalker.getName());
				}
			}
			assertTrue(tFamily.size() >= 8, "the MUI machine family census collapsed — investigate before trusting this pin (found: " + tFamily + ")");
			assertTrue(tFamily.contains("gregtech6.tileentity.machines.TileEntityBasicMachine"),
					"the scanner-crash class must anchor the family roster (found: " + tFamily + ")");
			assertTrue(tFamily.contains("gregtech6.tileentity.inventories.GT6HopperBaseBlockEntity"),
					"the storage family must ride the roster (found: " + tFamily + ")");

			// the production probe, resolved INSIDE the guard world (its own copy of
			// GTEntityBlock — reflective resolution over family classes hits the guard)
			Method tProbe = Class.forName("gregtech6.block.GTEntityBlock", false, tGuard)
					.getDeclaredMethod("probeDeclared", Class.class, String.class, Class[].class);
			tProbe.setAccessible(true);

			for (String tName : tFamily) {
				// the ratchet reads the TEST-world class. Leg note: the forge leg's plain JVM
				// keeps every member, so annotated AND unannotated client-typed methods are
				// visible; the neo leg's FML TRANSFORMER already STRIPS the annotated ones
				// (server-dist member cleaning) — the stripping itself proves those members
				// carry @OnlyIn — while an UNANNOTATED client-typed method survives and is
				// still caught by the check below. Both legs fail the census on a leak.
				List<Method> tClientTyped = clientTypedMethods(Class.forName(tName), tScreen);
				for (Method tMethod : tClientTyped) {
					assertTrue(onlyInClient(tMethod),
							"dist seam violation: " + tName + " declares " + tMethod.getName()
									+ " naming the client-only ModularScreen WITHOUT @OnlyIn(Dist.CLIENT)"
									+ " — the dedicated server would detonate on any reflective walk ("
									+ tFamily.size() + "-family census)");
				}

				// the guard walk runs only on chains whose UNSTRIPPED bytecode (the guard
				// world — the reflection view here can be stripped on the neo leg) never
				// mentions the stripped class; byte-mentioning chains are proven by the
				// ratchet above plus the platform stripping
				if (tClientTyped.isEmpty() && !bytesMentionModularScreen(tName)) {
					try {
						Class<?> tClass = Class.forName(tName, false, tGuard);
						Object tCanDrop = tProbe.invoke(null, new Object[] {tClass, "canDrop", new Class[] {int.class}});
						Object tInventory = tProbe.invoke(null, new Object[] {tClass, "getInventory", new Class[0]});
						assertTrue(tCanDrop == null || tCanDrop instanceof Method, "canDrop probe yields the gate or nothing on " + tName);
						assertTrue(tInventory == null || tInventory instanceof Method, "getInventory probe yields the walk or nothing on " + tName);
					} catch (Throwable aE) {
						tDetonations.add(tName + " -> " + aE);
					}
				}
			}
		}
		assertTrue(tDetonations.isEmpty(),
				"family members detonated under the dedicated-server guard (the /fill teardown crash shape): " + tDetonations);
	}

	/**
	 * The negative control: under the SAME guard, the retired {@code getMethod} strategy
	 * detonates exactly the way the server did. If this ever stops throwing, the guard has
	 * stopped biting (or the JVM stopped resolving interface default signatures during
	 * enumeration) — the immunity pins above are then vacuous and must be re-derived.
	 */
	@Test
	public void theRetiredGetMethodStrategyDetonatesUnderTheGuard() throws Exception {
		try (DistGuardLoader tGuard = guard()) {
			Class<?> tBasicMachine = Class.forName("gregtech6.tileentity.machines.TileEntityBasicMachine", false, tGuard);
			// the class walk misses (no canDrop(int) anywhere up this chain — only the ACT
			// subclass declares it) → the interface-default enumeration → the dist seam
			LinkageError tBoom = assertThrows(LinkageError.class,
					() -> tBasicMachine.getMethod("canDrop", int.class),
					"the retired getMethod strategy must detonate under the guard — without this the immunity pins prove nothing");
			assertTrue(chainNames(tBoom, DIST_STRIPPED),
					"the detonation must be the ModularScreen dist seam, not an unrelated miss: " + tBoom);
		}
	}

	/**
	 * The semantics parity, over the real census classes (the dist-independent half of the
	 * contract): the declared-only walk returns the SAME Method the retired
	 * {@code getMethod} answered — the ACT holo gate (TileEntityAdvancedCraftingTable
	 * :774) and the machine inventory walk (TileEntityBasicMachine :376) — and answers
	 * nothing where the family carries neither ({@link GTEntityBlock} itself), so
	 * beCanDrop keeps its all-drop default. The ACT's onRemove BEHAVIOR under the walk is
	 * the GTEntityBlockInventoryDropTest.actHoloSlotsStayBehindOnBreak seat.
	 */
	@Test
	public void declaredWalkKeepsTheGetMethodSemantics() throws Exception {
		Method tProbe = GTEntityBlock.class.getDeclaredMethod("probeDeclared", Class.class, String.class, Class[].class);
		tProbe.setAccessible(true);

		Class<?> tAct = gregtech6.tileentity.machines.TileEntityAdvancedCraftingTable.class;
		assertEquals(tAct.getMethod("canDrop", int.class),
				tProbe.invoke(null, new Object[] {tAct, "canDrop", new Class[] {int.class}}),
				"the walk finds the exact declaration getMethod answered (the ACT holo gate :774)");
		Class<?> tBasicMachine = Class.forName("gregtech6.tileentity.machines.TileEntityBasicMachine");
		assertEquals(tBasicMachine.getMethod("getInventory"),
				tProbe.invoke(null, new Object[] {tBasicMachine, "getInventory", new Class[0]}),
				"the walk finds the exact declaration getMethod answered (the machine inventory :376)");

		assertNull(tProbe.invoke(null, new Object[] {GTEntityBlock.class, "canDrop", new Class[] {int.class}}),
				"no declaration up the block chain → null → the all-drop default");
		assertNull(tProbe.invoke(null, new Object[] {GTEntityBlock.class, "getInventory", new Class[0]}),
				"no declaration up the block chain → null → no inventory walk");
	}

	/**
	 * The production-path pin: the compiled {@code GTEntityBlock} must carry the
	 * declared-only strategy and NOT the retired one — the byte level makes a silent
	 * {@code getMethod} re-introduction impossible without failing here first.
	 */
	@Test
	public void theCompiledGatesCarryTheDeclaredOnlyStrategy() throws Exception {
		Path tClassFile = mainClassesDir().resolve("gregtech6/block/GTEntityBlock.class");
		String tPool = new String(Files.readAllBytes(tClassFile), java.nio.charset.StandardCharsets.ISO_8859_1);
		assertTrue(tPool.contains("getDeclaredMethod"), "the walk rides getDeclaredMethod (" + tClassFile + ")");
		assertTrue(tPool.contains("probeDeclared"), "both gates route through the shared declared-only probe");
		assertTrue(!tPool.contains("getMethod"), "the retired Class.getMethod enumeration must be gone (" + tClassFile + ")");
	}

	/**
	 * The client-typed declaration census over one family chain: every chain-declared
	 * method whose return or parameter names the dist-stripped class — the surfaces both
	 * the platform's stripping and the declared-only walk act on.
	 */
	private static List<Method> clientTypedMethods(Class<?> aClass, Class<?> aClientType) {
		List<Method> tOut = new ArrayList<>();
		for (Class<?> tWalk = aClass; tWalk != null && tWalk != Object.class; tWalk = tWalk.getSuperclass()) {
			for (Method tMethod : tWalk.getDeclaredMethods()) {
				if (tMethod.getReturnType() == aClientType) {
					tOut.add(tMethod);
					continue;
				}
				for (Class<?> tParam : tMethod.getParameterTypes()) {
					if (tParam == aClientType) {
						tOut.add(tMethod);
						break;
					}
				}
			}
		}
		return tOut;
	}

	/** The {@code @OnlyIn(Dist.CLIENT)} check by NAME — the Dist package differs per leg. */
	private static boolean onlyInClient(Method aMethod) {
		for (Annotation tAnnotation : aMethod.getAnnotations()) {
			Class<? extends Annotation> tType = tAnnotation.annotationType();
			if (!"OnlyIn".equals(tType.getSimpleName())) continue;
			try {
				Object tValue = tType.getMethod("value").invoke(tAnnotation);
				return "CLIENT".equals(((Enum<?>) tValue).name());
			} catch (ReflectiveOperationException aE) {
				return false;
			}
		}
		return false;
	}

	/**
	 * The UNSTRIPPED view of one family class: whether its own constant pool names the
	 * dist-stripped class (a signature or a body). The reflection census cannot serve here
	 * — the neo leg's FML TRANSFORMER hands out member-stripped classes, so a client-typed
	 * declaration that carries @OnlyIn vanishes from it; the bytes never do.
	 */
	private static boolean bytesMentionModularScreen(String aClassName) throws Exception {
		Path tFile = mainClassesDir().resolve(aClassName.replace('.', '/') + ".class");
		// the constant pool stores the internal (slashed) form
		return contains(Files.readAllBytes(tFile),
				DIST_STRIPPED.replace('.', '/').getBytes(java.nio.charset.StandardCharsets.UTF_8));
	}

	// -----------------------------------------------------------------------
	// helpers
	// -----------------------------------------------------------------------

	private static DistGuardLoader guard() throws Exception {
		String tMuiClasses = System.getProperty("gt6.modularui.classes");
		assertNotNull(tMuiClasses, "gt6.modularui.classes system property (the build scripts feed it)");
		URL[] tUrls = new URL[] {mainClassesDir().toUri().toURL(), Paths.get(tMuiClasses).toUri().toURL()};
		return new DistGuardLoader(tUrls, GTEntityBlockDistLeakPinTest.class.getClassLoader());
	}

	private static Path mainClassesDir() throws Exception {
		return Paths.get(GTEntityBlock.class.getProtectionDomain().getCodeSource().getLocation().toURI());
	}

	/** The raw-byte candidate scan: every class whose constant pool names GT6MuiMachine. */
	private static List<String> muiReferencingClassNames() throws Exception {
		Path tRoot = mainClassesDir().resolve("gregtech6");
		List<String> tNames = new ArrayList<>();
		byte[] tNeedle = "gregtech6/gui/machines/GT6MuiMachine".getBytes(java.nio.charset.StandardCharsets.UTF_8);
		try (Stream<Path> tWalk = Files.walk(tRoot)) {
			tWalk.filter(p -> p.toString().endsWith(".class")).forEach(p -> {
				try {
					if (contains(Files.readAllBytes(p), tNeedle)) {
						String tName = tRoot.relativize(p).toString()
								.replace('\\', '/').replace(".class", "").replace('/', '.');
						tNames.add("gregtech6." + tName);
					}
				} catch (Exception aE) {
					throw new IllegalStateException("scan failed on " + p, aE);
				}
			});
		}
		assertTrue(tNames.size() >= 5, "the raw-byte census found too few GT6MuiMachine references — the scan itself broke");
		return tNames;
	}

	private static boolean contains(byte[] aHaystack, byte[] aNeedle) {
		outer:
		for (int i = 0; i <= aHaystack.length - aNeedle.length; i++) {
			for (int j = 0; j < aNeedle.length; j++) {
				if (aHaystack[i + j] != aNeedle[j]) continue outer;
			}
			return true;
		}
		return false;
	}

	private static boolean chainNames(Throwable aThrowable, String aName) {
		for (Throwable t = aThrowable; t != null; t = t.getCause() == t ? null : t.getCause()) {
			if (String.valueOf(t.getMessage()).contains(aName) || String.valueOf(t).contains(aName)) return true;
		}
		return false;
	}
}
