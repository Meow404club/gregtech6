/**
 * Offline tests for task p12-jei-integration: the JEI plugin detection contract and the
 * lang-key reconciliation seam (acceptance b).
 *
 * <p>JEI discovers plugins by scanning for {@code @JeiPlugin} and instantiating the class
 * through its NO-ARG constructor (JeiPlugin.java, JEI 1.20.1 CommonApi) — so the offline
 * surface is exactly what the contract needs:
 * <ul>
 * <li>the class is instantiable with a plain {@code new} in a bare JVM (no mod loading,
 *     no JEI runtime, no registry events);</li>
 * <li>{@code getPluginUid()} returns a non-null, stable, literal-pinned id;</li>
 * <li>the annotation is present and the no-arg constructor exists (reflection = the same
 *     faces JEI's scanner reads).</li>
 * </ul>
 * The lang-key reconciliation ({@link GT6JeiPlugin#INFO_KEY_COKE_OVEN} ↔ the GT6EnUs
 * provider) rides the datagen-side test (GT6EnUsJeiInfoTest) plus the runData gate; here we
 * pin the consumer half's literal so a silent rename cannot unhook the page.
 */
package gregtech6.jei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;

public class GT6JeiPluginTest {

	@Test
	public void noArgInstantiation() {
		// the JEI detection contract: a bare no-arg construction must succeed
		GT6JeiPlugin tPlugin = new GT6JeiPlugin();
		assertNotNull(tPlugin);
	}

	@Test
	public void declaredConstructorIsNoArg() throws Exception {
		// reflection face — what JEI's scanner actually requires
		assertEquals(0, GT6JeiPlugin.class.getDeclaredConstructor().getParameterCount());
	}

	@Test
	public void jeiPluginAnnotationInBytecode() throws Exception {
		// @JeiPlugin carries NO @Retention meta-annotation = CLASS retention: invisible to
		// runtime reflection (getAnnotation returns null), which is WHY JEI detects plugins by
		// scanning raw class files (ClassGraph) instead. Assert the annotation at the same
		// layer JEI reads it — the compiled bytecode.
		try (java.io.InputStream in = GT6JeiPlugin.class.getResourceAsStream("GT6JeiPlugin.class")) {
			assertNotNull(in, "plugin class resource not found on the test classpath");
			String tBytes = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.ISO_8859_1);
			assertTrue(tBytes.contains("Lmezz/jei/api/JeiPlugin;"),
					"@JeiPlugin annotation missing from the class file — JEI would never detect the plugin");
		}
	}

	@Test
	public void implementsIModPlugin() {
		assertTrue(IModPlugin.class.isAssignableFrom(GT6JeiPlugin.class));
	}

	@Test
	public void pluginUidNonNullAndStable() {
		GT6JeiPlugin tPlugin = new GT6JeiPlugin();
		assertNotNull(tPlugin.getPluginUid());
		assertSame(tPlugin.getPluginUid(), tPlugin.getPluginUid(),
				"uid must be a stable constant, not a fresh object per call");
	}

	@Test
	public void pluginUidPinnedLiteral() {
		// the id is part of the mod's external identity (JEI blacklists/configs key on it)
		assertEquals("gt6:jei_plugin", new GT6JeiPlugin().getPluginUid().toString());
		assertEquals("gt6", new GT6JeiPlugin().getPluginUid().getNamespace());
		assertEquals(GT6JeiPlugin.PLUGIN_UID_PATH, new GT6JeiPlugin().getPluginUid().getPath());
	}

	@Test
	public void infoKeyPinnedLiteral() {
		// the consumer half of the lang reconciliation seam (provider half = GT6EnUsJeiInfoTest)
		assertEquals("gt6.jei.info.multiblock_coke_oven", GT6JeiPlugin.INFO_KEY_COKE_OVEN);
	}
}
