package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * The datagen single-writer pin (task p30-ops-datagen-lang-order, the ADR-P17-1 single
 * producer discipline over the lang face): exactly ONE {@code @Mod.EventBusSubscriber} in
 * this package (GT6DataGenerators — every provider band folds into its listener) and the
 * ONE registered en_us writer is the FULL-UNION chain tail. The disease: three subscribers
 * (GT6DataGenerators/GT6CrucibleDatagen/GT6MoldDatagen) each registered a full-file lang
 * writer — LanguageProvider.finish rewrites en_us.json whole, so the last writer won and
 * the winner was drawn from the annotation-scan order (a classes-dir enumeration order an
 * incremental recompile flips; the live repro lost the 66 ceramic mold/faucet keys to it,
 * known_bugs.datagen_lang_order_lottery). The annotation rides DIFFERENT simple names per
 * leg (forge {@code net.minecraftforge.fml.common.Mod$EventBusSubscriber} vs the 21.1 swap
 * table's top-level {@code net.neoforged.fml.common.EventBusSubscriber}), so the pin
 * matches the {@code fml.common.*EventBusSubscriber} shape instead of forking.
 */
public class GT6DatagenSingleWriterTest {

	/**
	 * Every top-level class of this package off the classpath (the gradle test JVM sees the
	 * main output dir, the GT6DualDirectoryFacesTest resource-walk form). {@code
	 * initialize=false} — annotation metadata only, no static init (the datagen classes'
	 * clinit paths touch registry objects an offline JVM must not run).
	 */
	private static List<Class<?>> datagenPackageClasses() throws Exception {
		List<Class<?>> r = new ArrayList<>();
		ClassLoader tLoader = GT6DatagenSingleWriterTest.class.getClassLoader();
		URL tPackage = tLoader.getResource("gregtech6/datagen");
		assertNotNull(tPackage, "the gregtech6/datagen main output must be on the test classpath");
		try (Stream<Path> tWalk = Files.list(Paths.get(tPackage.toURI()))) {
			tWalk.filter(tFile -> tFile.getFileName().toString().endsWith(".class"))
					.filter(tFile -> tFile.getFileName().toString().indexOf('$') < 0)
					.map(tFile -> tFile.getFileName().toString().replace(".class", ""))
					.forEach(tName -> {
						try {
							r.add(Class.forName("gregtech6.datagen." + tName, false, tLoader));
						} catch (ClassNotFoundException tError) {
							throw new IllegalStateException(tName, tError);
						}
					});
		}
		assertFalse(r.isEmpty(), "the package walk must see the datagen classes");
		return r;
	}

	/** No second GatherDataEvent subscriber may re-open the last-writer lottery. */
	@Test
	public void onlyGT6DataGeneratorsIsAModBusSubscriber() throws Exception {
		for (Class<?> tClass : datagenPackageClasses()) {
			boolean tSubscriber = false;
			for (java.lang.annotation.Annotation tAnnotation : tClass.getAnnotations()) {
				String tName = tAnnotation.annotationType().getName();
				if (tName.contains("fml.common") && tName.endsWith("EventBusSubscriber")) {
					tSubscriber = true;
				}
			}
			if ("gregtech6.datagen.GT6DataGenerators".equals(tClass.getName())) {
				assertTrue(tSubscriber, tClass.getSimpleName()
						+ " must carry @Mod.EventBusSubscriber (the single GatherDataEvent listener)");
			} else {
				assertFalse(tSubscriber, tClass.getSimpleName()
						+ " must NOT be a @Mod.EventBusSubscriber — fold its provider band into"
						+ " GT6DataGenerators (the p30 single-listener discipline)");
			}
		}
	}

	/**
	 * The single registered en writer is the union tail: base ⊕ crucible ⊕ mold replay
	 * through the super chain, so one write lands the complete en_us.json.
	 */
	@Test
	public void theEnWriterChainReachesTheFullUnion() {
		assertEquals(GT6CrucibleDatagen.Lang.class, GT6MoldDatagen.Lang.class.getSuperclass(),
				"the mold lang must chain below the crucible lang");
		assertEquals(GT6EnUs.class, GT6CrucibleDatagen.Lang.class.getSuperclass(),
				"the crucible lang must chain below GT6EnUs (the base table)");
	}
}
