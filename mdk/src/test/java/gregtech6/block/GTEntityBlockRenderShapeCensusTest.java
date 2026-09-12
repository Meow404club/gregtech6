package gregtech6.block;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;

import org.junit.jupiter.api.Test;

/**
 * The offline census hardening for the p27-cfoam-owned-render root cause: vanilla
 * BaseEntityBlock.java:19-21 defaults {@code getRenderShape} to {@code RenderShape.INVISIBLE}
 * (the BlockEntityRenderer assumption), so EVERY concrete GTEntityBlock subclass must override
 * it to MODEL or the chunk renderer silently skips the block client-side. GT6CFoamOwnedBlock
 * shipped without the override (the foam B card d9048f24) — the owned foam was invisible in
 * both states; this census keeps a third case from landing.
 *
 * <p>Enumeration: the compiled {@code gregtech6} class tree that shipped {@link GTEntityBlock}
 * (its own CodeSource; fallback walks {@code build/classes/java/main} under the mdk root for
 * whichever stonecutter node compiled there). Each candidate is loaded UNinitialized
 * ({@code Class.forName(name, false, ...)} — no registry clinit offline) and checked by
 * reflection: some class strictly between the subclass and BaseEntityBlock must declare
 * {@code getRenderShape(BlockState)}.
 *
 * <p>Declared deviation (the allowlist): {@code gregtech6.block.TestMachineBlock} — the p3
 * framework test block with no blockstate/model datagen face (its javadoc defers JSON to the
 * wave-2 example machine card), so INVISIBLE-vs-modelless is indistinguishable there.
 */
public class GTEntityBlockRenderShapeCensusTest {

	/** The declared deviation — see the class javadoc. Adding a name here needs the reason in the comment. */
	private static final Set<String> ALLOWED_TO_INHERIT_INVISIBLE = Set.of(
			"gregtech6.block.TestMachineBlock" // p3 framework test block, no model datagen face
	);

	/** The enumeration sanity floor — a silent empty scan must fail, never vacuously pass. */
	private static final int EXPECTED_CONCRETE_SUBCLASS_FLOOR = 20;

	@Test
	public void everyConcreteGTEntityBlockSubclassOverridesGetRenderShape() throws Exception {
		Set<String> tChecked = new TreeSet<>();
		Set<String> tViolations = new TreeSet<>();
		for (String tName : enumerateGregtech6ClassNames()) {
			Class<?> tClass;
			try {
				tClass = Class.forName(tName, false, GTEntityBlock.class.getClassLoader());
			} catch (Throwable aIgnored) {
				continue; // not loadable offline (tooling/annotation leftovers) — never a census subject
			}
			int tFlags = tClass.getModifiers();
			if (tClass == GTEntityBlock.class
					|| !GTEntityBlock.class.isAssignableFrom(tClass)
					|| Modifier.isAbstract(tFlags)
					|| Modifier.isInterface(tFlags)
					|| ALLOWED_TO_INHERIT_INVISIBLE.contains(tName)) {
				continue;
			}
			tChecked.add(tName);
			if (!overridesRenderShape(tClass)) {
				tViolations.add(tName);
			}
		}
		assertTrue(tChecked.size() >= EXPECTED_CONCRETE_SUBCLASS_FLOOR,
				"the census enumerated " + tChecked.size() + " concrete GTEntityBlock subclasses — below the floor, "
						+ "the scan went dark (checked: " + tChecked + ")");
		assertTrue(tViolations.isEmpty(),
				"these GTEntityBlock subclasses inherit the BaseEntityBlock INVISIBLE render default and will be "
						+ "skipped by the chunk renderer — override getRenderShape → MODEL (the GTOvenBlock.java:97-98 form): "
						+ tViolations);
	}

	/** Some class strictly between the subclass and BaseEntityBlock must declare the override. */
	private static boolean overridesRenderShape(Class<?> aClass) {
		for (Class<?> tClass = aClass; tClass != null && tClass != BaseEntityBlock.class; tClass = tClass.getSuperclass()) {
			try {
				tClass.getDeclaredMethod("getRenderShape", BlockState.class);
				return true;
			} catch (NoSuchMethodException aIgnored) {
				// keep climbing
			}
		}
		return false;
	}

	/**
	 * Tier 1: walk the code source that shipped GTEntityBlock (a classes directory or a jar).
	 * Tier 2 (the stonecutter-layout fallback): find the mdk root from the working directory and
	 * walk only the {@code build|classes|java|main} chains (plus {@code .stonecutter}) collecting
	 * every {@code gregtech6} classes directory. An empty union fails the caller loudly.
	 */
	private static List<String> enumerateGregtech6ClassNames() throws IOException, URISyntaxException {
		List<String> tNames = new ArrayList<>();
		URL tSource = GTEntityBlock.class.getProtectionDomain().getCodeSource() != null
				? GTEntityBlock.class.getProtectionDomain().getCodeSource().getLocation()
				: null;
		if (tSource != null && "file".equals(tSource.getProtocol())) {
			Path tLocation = Paths.get(tSource.toURI());
			if (Files.isDirectory(tLocation)) {
				collectClassNamesFromDirectory(tLocation, tNames);
			} else {
				try (JarFile tJar = new JarFile(tLocation.toFile())) {
					Enumeration<JarEntry> tEntries = tJar.entries();
					while (tEntries.hasMoreElements()) {
						String tEntry = tEntries.nextElement().getName();
						if (tEntry.startsWith("gregtech6/") && tEntry.endsWith(".class")) {
							tNames.add(binaryName(tEntry));
						}
					}
				}
			}
		}
		if (tNames.isEmpty()) {
			Path tMdkRoot = locateMdkRoot();
			if (tMdkRoot != null) {
				for (Path tClassesRoot : collectMainClassesRoots(tMdkRoot)) {
					collectClassNamesFromDirectory(tClassesRoot, tNames);
				}
			}
		}
		return tNames;
	}

	private static String binaryName(String aPath) {
		return aPath.substring("gregtech6/".length(), aPath.length() - ".class".length())
				.replace('/', '.').replace("\\", ".");
	}

	private static void collectClassNamesFromDirectory(Path aRoot, List<String> aNames) throws IOException {
		Path tPackageRoot = aRoot.resolve("gregtech6");
		if (!Files.isDirectory(tPackageRoot)) {
			return;
		}
		try (var tWalk = Files.walk(tPackageRoot)) {
			tWalk.filter(Files::isRegularFile)
					// relativized against the CLASSES ROOT, never the absolute path: an
					// absolute string carries an arbitrary checkout name — a repo cloned
					// into a directory literally named "gregtech6" made indexOf(first marker)
					// bind to the checkout name and every derived binary name CNF'd
					// (checked=[] — the p28-c-ulv-machine-ladder merge-session finding)
					.map(aPath -> aRoot.relativize(aPath).toString())
					.filter(aName -> aName.endsWith(".class") && !aName.endsWith("module-info.class"))
					.map(GTEntityBlockRenderShapeCensusTest::pathToBinaryName)
					.filter(aName -> !aName.matches(".*\\$\\d.*")) // anonymous/synthetic classes are not block carriers
					.forEach(aNames::add);
		}
	}

	private static String pathToBinaryName(String aPath) {
		String tMarker = "gregtech6" + java.io.File.separator;
		int tIndex = aPath.indexOf(tMarker);
		return "gregtech6." + aPath.substring(tIndex + tMarker.length(), aPath.length() - ".class".length())
				.replace(java.io.File.separator, ".");
	}

	/** Climb from the working directory to the mdk root (the dir holding the GTEntityBlock source). */
	private static Path locateMdkRoot() {
		Path tDir = Paths.get("").toAbsolutePath();
		for (int i = 0; i < 8 && tDir != null; i++, tDir = tDir.getParent()) {
			if (Files.isRegularFile(tDir.resolve("src/main/java/gregtech6/block/GTEntityBlock.java"))) {
				return tDir;
			}
		}
		return null;
	}

	/** Only the {@code build/classes/java/main} chains (and {@code .stonecutter}) are walked. */
	private static List<Path> collectMainClassesRoots(Path aMdkRoot) throws IOException {
		List<Path> tRoots = new ArrayList<>();
		try (var tWalk = Files.walk(aMdkRoot, 12)) {
			tWalk.filter(aPath -> isWithinAClassesChain(aMdkRoot, aPath))
					.filter(aPath -> aPath.getFileName() != null && aPath.getFileName().toString().equals("main"))
					.forEach(aPath -> {
						Path tPackage = aPath.resolve("gregtech6");
						if (Files.isDirectory(tPackage)) {
							tRoots.add(aPath);
						}
					});
		}
		return tRoots;
	}

	private static boolean isWithinAClassesChain(Path aMdkRoot, Path aPath) {
		java.util.Iterator<java.nio.file.Path> tSegments = aMdkRoot.relativize(aPath).iterator();
		// the stonecutter 0.7 layout nests node builds at versions/<node>/build/classes/java/main
		if (tSegments.hasNext() && tSegments.next().toString().equals("versions") && tSegments.hasNext()) {
			tSegments.next(); // the node directory itself (e.g. 1.20.1-forge) — allowed verbatim
		}
		while (tSegments.hasNext()) {
			String tName = tSegments.next().toString();
			if (!(tName.equals(".stonecutter") || tName.equals("build") || tName.equals("classes")
					|| tName.equals("java") || tName.equals("main") || tName.equals("gregtech6"))) {
				return false;
			}
		}
		return true;
	}
}
