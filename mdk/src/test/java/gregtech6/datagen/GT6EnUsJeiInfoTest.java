/**
 * Offline test for task p12-jei-integration: the producer half of the lang-key reconciliation
 * (acceptance b — "EnUs lang key 对账断言").
 *
 * <p>GT6JeiPlugin.registerRecipes hangs JEI's built-in ingredient info page on the coke oven
 * controller item via {@code Component.translatable(GT6JeiPlugin.INFO_KEY_COKE_OVEN)} — a key
 * that only carries text if the GT6EnUs provider generates it (the hand-written-JSON red line
 * forbids editing en_us.json directly). The reconciliation rides a recording subclass of
 * LanguageProvider: {@code add(String,String)} is public and non-final, so a test subclass in
 * this package can capture every entry {@code addTranslations()} would emit and assert the
 * consumer's key is covered — in a bare JVM, no datagen run needed (the generated file itself
 * is gated by runData + the second-run written:0 check).
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.data.PackOutput;
import net.minecraft.server.Bootstrap;

import gregtech6.jei.GT6JeiPlugin;
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
	public void providerCoversThePluginInfoKey() {
		Map<String, String> tEntries = collectTranslations();
		assertTrue(tEntries.containsKey(GT6JeiPlugin.INFO_KEY_COKE_OVEN),
			"GT6EnUs must generate the JEI info key — without it the info page renders the raw key");
	}

	@Test
	public void infoTextIsNotBlankAndPinsTheStructureFacts() {
		Map<String, String> tEntries = collectTranslations();
		String tText = tEntries.get(GT6JeiPlugin.INFO_KEY_COKE_OVEN);
		assertNotNull(tText);
		assertTrue(!tText.isBlank());
		// the live gate (p6 RCON gt6multiblock frame/check, linked_parts=25/25) fixed the facts:
		// 3x3x3 cube, empty center, controller on one face, 25 bricks
		assertTrue(tText.contains("3x3x3"), "info text must state the cube size");
		assertTrue(tText.contains("25"), "info text must state the brick count (linked_parts=25/25)");
		assertTrue(tText.contains("Coke Oven Bricks"), "info text must name the part block");
	}

	@Test
	public void pluginKeyLiteralMatchesTheGeneratedKeyNamespace() {
		// both halves pinned: consumer literal (GT6JeiPluginTest) vs provider coverage, joined
		// by the shared constant — this test fails if anyone re-hardcodes either side
		assertEquals("gt6.jei.info.multiblock_coke_oven", GT6JeiPlugin.INFO_KEY_COKE_OVEN);
		assertTrue(collectTranslations().containsKey("gt6.jei.info.multiblock_coke_oven"));
	}
}
