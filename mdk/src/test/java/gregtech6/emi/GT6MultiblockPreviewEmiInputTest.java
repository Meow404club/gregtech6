/**
 * Offline guard for the multiblock preview's EMI input chain (task mb-preview-emi-drag-fix).
 * The card opened as a "dead code" cleanup: the r11 nav-suite scoping read upstream EMI's
 * RecipeScreen dispatch (scroll → sidebar paging :510-524, drag → pressed-slot stack
 * :486-507, release → pressedSlot :460-483) and concluded the drag/scroll methods on
 * {@code ModularUIEmiRecipe.UIWrapperWidget} (:237/:241/:245) can never be called. That
 * premise is FALSE: the vendored ModularUI fork ships
 * {@code brachy/modularui/core/mixins/emi/RecipeScreenMixin} (upstream MUI PR #39, "emi
 * scroll and drag event capturing"), which injects at the HEAD of all three RecipeScreen
 * methods and forwards to those exact methods — so scroll-zoom, drag-rotate AND the
 * left-click select (its {@code IGuiAction.MouseReleased} listener rides the same
 * forwarded release) are live on the EMI leg. Deleting the "dead" methods was the card's
 * prescribed fix; this test is the red pin that keeps them: it fails on de-registration,
 * on dropping the mixin, or on deleting any of the three methods — the same failures the
 * field would show as "preview won't drag/zoom in EMI".
 *
 * <p>Everything pinned here lives in the vendored modularui artifact (compile-only for the
 * mdk main sources; its classes link the EMI/Minecraft client stacks and are never
 * class-loaded offline). The bytes are read off the FILESYSTEM, not the classloader: both
 * build scripts hand the modularui node's build output dirs over via the
 * {@code gt6.modularui.*} system properties, because the neoforge test JVM runs inside
 * FML's module layer (forgejunitdev, TRANSFORMER layer) where each mod is its own module
 * classloader and cross-mod resource reads come back null (2026-10-03 probe). Constant-pool
 * strings are remap-stable: the mixin's targets and the forwarded methods are EMI/MUI's own
 * (unobfuscated) names.
 */
package gregtech6.emi;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

public class GT6MultiblockPreviewEmiInputTest {

	/** Raw file bytes as ISO-8859-1 (constant-pool-safe lossless view). */
	private static String modularuiFile(String aDirProperty, String... aSegments) throws IOException {
		String tBase = System.getProperty("gt6.modularui." + aDirProperty);
		assertNotNull(tBase, "gt6.modularui." + aDirProperty
				+ " system property (both build scripts feed the modularui node's build output dirs)");
		Path tPath = Paths.get(tBase, aSegments);
		assertTrue(Files.exists(tPath), tPath + " exists (the vendored modularui node has been built)");
		return new String(Files.readAllBytes(tPath), StandardCharsets.ISO_8859_1);
	}

	@Test
	public void emiInputMixinStaysRegistered() throws IOException {
		// the mixins.json registration is gated on isModLoaded("emi") by ModularUIMixinPlugin;
		// dropping the entry silently kills EMI-page scroll/drag/release for every MUI recipe
		assertTrue(modularuiFile("resources", "modularui.mixins.json").contains("emi.RecipeScreenMixin"),
				"RecipeScreenMixin must stay registered — it is the only path delivering scroll/drag/release to EMI recipe pages");
	}

	@Test
	public void mixinCapturesTheThreeEventsAndForwardsThem() throws IOException {
		String tMixin = modularuiFile("classes", "brachy", "modularui", "core", "mixins", "emi",
				"RecipeScreenMixin.class");
		assertTrue(tMixin.contains("dev/emi/emi/screen/RecipeScreen"),
				"the mixin targets EMI's RecipeScreen (the recipe page screen)");
		assertTrue(tMixin.contains("mouseScrolled") && tMixin.contains("mouseDragged")
				&& tMixin.contains("mouseReleased"),
				"the mixin injects all three events upstream EMI never dispatches to widgets");
		// 旧钉迁移声明 (§6-7, task mattree-emi-panzoom): the pin used to read
		// 「ModularUIEmiRecipe$UIWrapperWidget」 in the mixin bytes — the gate widened from the
		// wrapper instanceof to the MUI-owned EmiInteractionSink interface (UIWrapperWidget
		// implements it; MUI behaviour unchanged), so the seam pin migrates to that name
		assertTrue(tMixin.contains("brachy/modularui/integration/emi/recipe/EmiInteractionSink"),
				"the mixin forwards through the EmiInteractionSink seam (the widened UIWrapperWidget gate)");
	}

	@Test
	public void uiWrapperWidgetKeepsTheMixinInvocationContract() throws IOException {
		String tWrapper = modularuiFile("classes", "brachy", "modularui", "integration", "emi", "recipe",
				"ModularUIEmiRecipe$UIWrapperWidget.class");
		// the r11-recommended deletion breaks this pin AND the submodule compile (the mixin
		// references the methods) — these three are mixin-invoked, not dead
		assertTrue(tWrapper.contains("mouseScrolled") && tWrapper.contains("mouseDragged")
				&& tWrapper.contains("mouseReleased"),
				"the three 'dead-looking' methods are the mixin's invocation contract — deletion kills EMI drag/zoom");
		// the pair upstream EMI dispatches natively (RecipeScreen :432 click / :553 key)
		assertTrue(tWrapper.contains("mouseClicked") && tWrapper.contains("keyPressed"),
				"the Widget-face pair (click/key) stays forwarded too");
	}

	/**
	 * The seam widening (task mattree-emi-panzoom): the gate is the MUI-owned
	 * {@code EmiInteractionSink} interface, and the GT6 material-tree nav canvas is on it.
	 * Pinned byte-level, this leg's own build outputs (the property is per-leg): the
	 * interface class exists and carries the three event names, the descriptor pins hold
	 * the per-leg scroll signature (forge single amount / neoforge dual-axis — the same
	 * {@code //?} split the mixin has), the mixin gates on the interface, the wrapper
	 * implements it, and the nav canvas implements it. Dropping any link silently kills
	 * the tree page's wheel/pan (or MUI's own scroll/drag) — this pin fails first.
	 */
	@Test
	public void theSeamInterfaceCarriesTheWidenedGateAndBothLegSignatures() throws IOException {
		String tSink = modularuiFile("classes", "brachy", "modularui", "integration", "emi", "recipe",
				"EmiInteractionSink.class");
		assertTrue(tSink.contains("mouseScrolled") && tSink.contains("mouseDragged")
				&& tSink.contains("mouseReleased"),
				"the seam interface carries the three undispatched event names");
		// the per-leg scroll signature + the leg-independent drag/release descriptors
		//? if forge {
		assertTrue(tSink.contains("(DDD)Z"), "forge scroll = (mouseX, mouseY, amount)");
		assertFalse(tSink.contains("(DDDD)Z"), "forge has no dual-axis scroll");
		//?} else {
		/*assertTrue(tSink.contains("(DDDD)Z"), "neoforge scroll = (mouseX, mouseY, scrollX, scrollY)");
		 *///?}
		assertTrue(tSink.contains("(IDD)Z") && tSink.contains("(I)Z"),
				"drag = (button, dX, dY), release = (button)");
		// the gate: the mixin names the interface (instanceof + invokeinterface)
		String tMixin = modularuiFile("classes", "brachy", "modularui", "core", "mixins", "emi",
				"RecipeScreenMixin.class");
		assertTrue(tMixin.contains("brachy/modularui/integration/emi/recipe/EmiInteractionSink"),
				"the mixin's gate is the seam interface");
		// the two implementers: MUI's wrapper (behaviour unchanged) and GT6's nav canvas
		String tWrapper = modularuiFile("classes", "brachy", "modularui", "integration", "emi", "recipe",
				"ModularUIEmiRecipe$UIWrapperWidget.class");
		assertTrue(tWrapper.contains("brachy/modularui/integration/emi/recipe/EmiInteractionSink"),
				"UIWrapperWidget implements the seam (the legacy consumer keeps its forwards)");
		assertTrue(mainClassBytes(GT6MaterialTreeNavWidget.class)
						.contains("brachy/modularui/integration/emi/recipe/EmiInteractionSink"),
				"GT6MaterialTreeNavWidget implements the seam (the tree page's wheel/pan face)");
	}

	/** Main output class bytes as ISO-8859-1 (the NavTest bytesOf form, duplicated to keep this test fixture-free). */
	private static String mainClassBytes(Class<?> aClass) throws IOException {
		try (var tIn = aClass.getResourceAsStream(aClass.getSimpleName() + ".class")) {
			assertNotNull(tIn, aClass.getSimpleName() + " class bytes (the mdk main output is on the test classpath)");
			return new String(tIn.readAllBytes(), StandardCharsets.ISO_8859_1);
		}
	}
}
