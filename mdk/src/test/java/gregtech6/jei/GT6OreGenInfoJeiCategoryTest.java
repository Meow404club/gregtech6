/**
 * Offline tests for task debt-ore-gen-display: the JEI leg's category surface — the uid
 * contract (the JEMI skip key), the shared-seam geometry, and the bytecode-layer pin of
 * the invisible-mounting arm (the GTCEu addInvisibleIngredients form: without that call
 * in the compiled category, U on an ore block item never reaches the page — the one face
 * no offline recording double can exercise, so it is pinned where the scanner would see
 * it, the same layer as the GT6JeiPluginTest annotation guard).
 */
package gregtech6.jei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregtech6.worldgen.GT6OreGenInfoLayout;

public class GT6OreGenInfoJeiCategoryTest {

	@BeforeAll
	static void bootVanillaOffline() {
		gregtech6.registry.GTMaterialItems.initMaterials();
		net.minecraft.SharedConstants.tryDetectVersion();
		try {
			net.minecraft.server.Bootstrap.bootStrap();
		} catch (Throwable ignored) {
		}
	}

	@Test
	public void uidMirrorsTheEmiCategoryId() {
		GT6OreGenInfoJeiCategory tCategory = new GT6OreGenInfoJeiCategory();
		assertEquals("gt6:ore_gen_info", tCategory.getRecipeType().getUid().toString(),
				"the JEI uid must mirror the EMI category id one-to-one (the JEMI skip key)");
	}

	@Test
	public void geometryAndTitleComeFromTheSharedSeam() {
		GT6OreGenInfoJeiCategory tCategory = new GT6OreGenInfoJeiCategory();
		assertEquals(GT6OreGenInfoLayout.WIDTH, tCategory.getWidth());
		assertEquals(GT6OreGenInfoLayout.categoryHeight(), tCategory.getHeight(),
				"the category height is the shared worst-entry value, not a local constant");
		assertEquals(GT6OreGenInfoLayout.TITLE_TEXT, tCategory.getTitle().getString());
	}

	/**
	 * The invisible-mounting arm at the bytecode layer: the compiled category must call
	 * {@code addInvisibleIngredients} (the U-reachability face — GTCEu
	 * GTOreVeinInfoCategory.java:51-53 form). Text-layer only here because the JEI runtime
	 * cannot run offline; the name in the constant pool is exactly what loses the face if
	 * the call is dropped.
	 */
	@Test
	public void invisibleMountingArmIsInTheBytecode() throws Exception {
		try (java.io.InputStream in = GT6OreGenInfoJeiCategory.class.getResourceAsStream("GT6OreGenInfoJeiCategory.class")) {
			assertNotNull(in, "category class resource not found on the test classpath");
			String tBytes = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.ISO_8859_1);
			assertTrue(tBytes.contains("addInvisibleIngredients"),
					"the invisible variant mounting call is missing — U on ore items would never reach the page");
		}
	}
}
