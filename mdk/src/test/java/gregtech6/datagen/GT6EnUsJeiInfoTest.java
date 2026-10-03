/**
 * Offline test for the jei-info lang domain (task jei-integration origin, ADR
 * 2026-09-02-jei-dependency; since task mbpreview-shell-replicate a ZERO face).
 *
 * <p>The coke oven structure description key (and its whole {@code gt6.jei.info.multiblock_*}
 * band) is RETIRED — the 3D preview page ({@code GT6MultiblockPreviewWidget}, both viewer
 * legs) speaks for itself, and no provider may re-grow a description row. The census rides
 * a recording subclass of LanguageProvider: {@code add(String,String)} is public and
 * non-final, so a test subclass in this package captures every entry
 * {@code addTranslations()} would emit and asserts the retired band stays empty — in a
 * bare JVM, no datagen run needed (the generated files themselves are gated by runData +
 * the second-run written:0 check).
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.data.PackOutput;
import net.minecraft.server.Bootstrap;

import gregtech6.registry.GTMaterialItems;

public class GT6EnUsJeiInfoTest {

	@BeforeAll
	public static void boot() {
		// the provider walk loads MaterialPrefixItem (an Item subclass) via snakeCase —
		// vanilla Item static init needs the bootstrap (GT6ToolsCreativeTabTest.boot shape;
		// NetworkHooks init failure is expected offline, registries are ready by then)
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// offline-expected
		}
		// addTranslations walks OP prefixes + the material registry (addPrefixTemplates/addMaterialNames)
		GTMaterialItems.initMaterials();
	}

	/** Records every add() the provider makes, then runs the real translation walk. */
	private static Map<String, String> collectTranslations() {
		Map<String, String> tEntries = new HashMap<>();
		GT6EnUs tProvider = new GT6EnUs(new PackOutput(Path.of("build", "tmp", "gt6enus-jei-test"))) {
			@Override
			public void add(String aKey, String aValue) {
				if (tEntries.put(aKey, aValue) != null) {
					throw new IllegalStateException("Duplicate translation key " + aKey);
				}
			}
		};
		tProvider.addTranslations(); // protected + same package = accessible
		return tEntries;
	}

	@Test
	public void multiblockDescriptionBandStaysRetired() {
		// the text-face-zero census (task mbpreview-shell-replicate): the description band
		// must never re-grow a row — the 3D preview replaced the words (the TITLE_KEY
		// category name under gt6.jei.multiblock_preview is the ONLY survivor, and it
		// lives OUTSIDE the gt6.jei.info.* band)
		long tBand = collectTranslations().keySet().stream()
				.filter(tKey -> tKey.startsWith("gt6.jei.info.multiblock"))
				.count();
		assertTrue(tBand == 0, "the gt6.jei.info.multiblock_* description band must stay empty ("
				+ tBand + " rows re-grown) — the 3D preview page replaced the text");
	}
}
