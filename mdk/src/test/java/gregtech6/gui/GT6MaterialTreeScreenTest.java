/**
 * Copyright (c) 2026 GregTech-6 Team
 *
 * This file is part of GregTech.
 *
 * GregTech is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GregTech is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GregTech. If not, see <http://www.gnu.org/licenses/>.
 */

package gregtech6.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.emi.GT6EmiPlugin;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.jei.GT6JeiPlugin;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.GT6RecipesAnvil;
import gregtech6.recipes.GT6RecipesOreChain;
import gregtech6.recipes.GT6RecipesSifter;
import gregtech6.recipes.GT6RecipesShCL;
import gregtech6.recipes.GTRecipesOfflineTestBase;
import gregtech6.recipes.tree.MaterialTreeBuilder;
import gregtech6.recipes.tree.MaterialTreeDisplay;
import gregtech6.recipes.tree.MaterialTreeViewport;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTMaterialItems.PrefixMaterial;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The standalone material-tree screen pins (task nav-s4-tree-screen, reworked by task
 * mattree-viewport-fit / ADR 2026-10-06-mattree-refactor L2) — the offline red-green over
 * the screen's pure seams (the M2 harness form; the render pass needs a live client and
 * stays field_test's):
 *
 * <ul>
 * <li><b>construction + viewport wiring</b>: the screen rides the S1 four-arg
 *     {@link MaterialTreeViewport} with pane = the REAL window (600x500 synthetically) —
 *     the fit pose magnifies the 202x206 canvas into it, centred (the content centre rides
 *     the pane centre). <b>旧钉迁移声明</b>: the former pins 「pane clamped to
 *     min(window, content)」 + 「constant base translation (199, 147)」 died with
 *     mattree-viewport-fit — the base field no longer exists, the centring lives in the
 *     viewport's fit offsets;</li>
 * <li><b>resize 保锚</b>: a re-init hands the live viewport to {@code resizeTo} — the pane
 *     centre's tree point and the zoom level survive; a fresh screen opens on the centred
 *     fit; a degenerate pane keeps the last pose;</li>
 * <li><b>hit test</b>: node/byproduct clicks resolve through the viewport unapply inverse
 *     (the 所点即所跳 seam);</li>
 * <li><b>wheel zoom</b>: the 2x 档位 anchored under the mouse (the S1 screen-anchor
 *     formula, exact); from a 2.4x fit one notch lands ON the 4x ceiling (the clamp), the
 *     binary round trip re-lands on the exact centred fit, the fit-pose freeze;</li>
 * <li><b>pan range = the real window</b> (acceptance ②): zoomed in, the pan floor is
 *     {@code pane - content*scale} of the REAL pane;</li>
 * <li><b>drag pan + click slop</b>: a press-drag pans the viewport, a release within the
 *     slop is a CLICK and fires the jump seam, a dragged release does not;</li>
 * <li><b>keyboard</b>: the M2 action table verbatim (arrows pan, unmapped keys fall
 *     through);</li>
 * <li><b>strip buttons</b>: the +/−/R cells drive the same table, R = the exact centred
 *     fit;</li>
 * <li><b>jump routing</b>: the pure arms refuse without a viewer answer (the
 *     {@link GTViewerJump#preferredViewer()} predicate is the live router, EMI 双装优先 —
 *     the kitchen-NEI seam, reused not re-implemented);</li>
 * <li><b>bytecode</b>: the jump legs and the setScreen open seam are wired, and the two
 *     plugin faces carry {@code openItemPage}.</li>
 * </ul>
 */
public class GT6MaterialTreeScreenTest extends GTRecipesOfflineTestBase {

	private static final double EPSILON = 1e-9;
	/** The synthetic pane 600x500's fit numbers: fit = min(600/202, 500/206) = 250/103, x offset (600 - 202*fit)/2 = 5650/103, y offset 0. */
	private static final double FIT = 250.0 / 103.0, FIT_X = 5650.0 / 103.0;

	// ------------------------------------------------------------------
	// construction + the S1 four-arg viewport wiring (real pane, centred fit)
	// ------------------------------------------------------------------

	@Test
	public void constructionPinsTheDisplayAndTheCentredFitPose() {
		MaterialTreeDisplay tDisplay = ironDisplay();
		GT6MaterialTreeScreen tScreen = new GT6MaterialTreeScreen(tDisplay);
		assertTrue(tScreen.getTitle().getString().contains(MaterialTreeDisplay.CATEGORY_TITLE),
				"the title carries the shared category title");
		assertNull(tScreen.mView, "no viewport before init");
		tScreen.navInit(600, 500);
		assertNotNull(tScreen.mView, "init wires the viewport");
		assertEquals(FIT, tScreen.mView.scale(), EPSILON, "the fit magnifies into the real pane (250/103 > 1)");
		assertEquals(FIT, tScreen.mView.minScale(), EPSILON, "the fit is the live zoom floor");
		assertEquals(FIT_X, tScreen.mView.offsetX(), EPSILON);
		assertEquals(0.0, tScreen.mView.offsetY(), EPSILON, "the limiting axis pins to 0");
		MaterialTreeViewport.Point tCentre = tScreen.mView.apply(MaterialTreeDisplay.WIDTH / 2.0, MaterialTreeDisplay.HEIGHT / 2.0);
		assertEquals(300.0, tCentre.x(), EPSILON, "the content centre rides the pane centre");
		assertEquals(250.0, tCentre.y(), EPSILON, "the content centre rides the pane centre");
	}

	// ------------------------------------------------------------------
	// the hit test (the 所点即所跳 inverse)
	// ------------------------------------------------------------------

	@Test
	public void hitTestRoundtripsThroughTheViewport() {
		GT6MaterialTreeScreen tScreen = screenAt(600, 500);
		MaterialTreeDisplay tDisplay = tScreen.mDisplay;
		MaterialTreeDisplay.Node tNode0 = tDisplay.nodes().get(0);
		// the fit pose: screen = apply(tree) — the input inverse (unapply) roundtrips it
		double tNodeX = tScreen.mView.apply(MaterialTreeDisplay.nodeX(tNode0) + 9, MaterialTreeDisplay.nodeY(tNode0) + 9).x();
		double tNodeY = tScreen.mView.apply(MaterialTreeDisplay.nodeX(tNode0) + 9, MaterialTreeDisplay.nodeY(tNode0) + 9).y();
		assertFalse(tScreen.itemAt(tNodeX, tNodeY).isEmpty(), "the node's centre hits");
		assertEquals(tNode0.stack().getItem(), tScreen.itemAt(tNodeX, tNodeY).getItem(), "the exact node stack");
		// the byproduct band (the same loop family, its own leg)
		assertFalse(tDisplay.byproducts().isEmpty(), "the iron display has byproducts (the two-face merge)");
		double tByX = tScreen.mView.apply(MaterialTreeDisplay.byproductX(0) + 9, MaterialTreeDisplay.byproductY() + 9).x();
		double tByY = tScreen.mView.apply(MaterialTreeDisplay.byproductX(0) + 9, MaterialTreeDisplay.byproductY() + 9).y();
		assertEquals(tDisplay.byproducts().get(0).stack().getItem(), tScreen.itemAt(tByX, tByY).getItem(),
				"the first byproduct slot hits");
		assertTrue(tScreen.itemAt(10, 10).isEmpty(), "the header corner is no slot");
		assertTrue(tScreen.itemAt(10, 490).isEmpty(), "the pane's left edge is no slot");
	}

	// ------------------------------------------------------------------
	// wheel zoom (the S1 anchor math through the leg-neutral core)
	// ------------------------------------------------------------------

	@Test
	public void wheelZoomAnchorsUnderTheMouse() {
		GT6MaterialTreeScreen tScreen = screenAt(600, 500);
		assertTrue(tScreen.scroll(300, 210, 1), "the wheel is consumed");
		// from the 2.43x fit one notch rides the clamp onto the 4x ceiling (the 档位 freeze lands ON the limit)
		assertEquals(MaterialTreeViewport.MAX_SCALE, tScreen.mView.scale(), EPSILON);
		// the S1 screen-anchor formula: offset = focus - (focus - offset) * (new/old)
		assertEquals(300.0 - (300.0 - FIT_X) * (MaterialTreeViewport.MAX_SCALE / FIT), tScreen.mView.offsetX(), EPSILON,
				"the x anchor holds the tree point under the pointer");
		assertEquals(210.0 * (1.0 - MaterialTreeViewport.MAX_SCALE / FIT), tScreen.mView.offsetY(), EPSILON,
				"the y anchor holds the tree point under the pointer");
		// the anchor invariant: zoom at a node's centre and the node stays under the pointer
		MaterialTreeDisplay.Node tNode0 = tScreen.mDisplay.nodes().get(0);
		GT6MaterialTreeScreen tAnchored = screenAt(600, 500);
		double tNodeX = tAnchored.mView.apply(MaterialTreeDisplay.nodeX(tNode0) + 9, MaterialTreeDisplay.nodeY(tNode0) + 9).x();
		double tNodeY = tAnchored.mView.apply(MaterialTreeDisplay.nodeX(tNode0) + 9, MaterialTreeDisplay.nodeY(tNode0) + 9).y();
		tAnchored.scroll(tNodeX, tNodeY, 1);
		assertEquals(tNode0.stack().getItem(), tAnchored.itemAt(tNodeX, tNodeY).getItem(),
				"the zoomed pose keeps the anchored node under the pointer");
		// and back: the binary round trip re-lands on the EXACT centred fit pose
		assertTrue(tScreen.scroll(300, 210, -1));
		assertEquals(FIT, tScreen.mView.scale(), EPSILON);
		assertEquals(FIT_X, tScreen.mView.offsetX(), EPSILON);
		assertEquals(0.0, tScreen.mView.offsetY(), EPSILON);
		// a zero delta zooms OUT, which freezes at the fit pose (the S1 档位幂等)
		assertTrue(tScreen.scroll(300, 210, 0));
		assertEquals(FIT, tScreen.mView.scale(), EPSILON);
		assertEquals(FIT_X, tScreen.mView.offsetX(), EPSILON);
	}

	// ------------------------------------------------------------------
	// drag pan + the click/drag slop around the jump
	// ------------------------------------------------------------------

	@Test
	public void dragPansAndTheClickSlopGuardsTheJump() {
		GT6MaterialTreeScreen tScreen = screenAt(600, 500);
		assertTrue(tScreen.scroll(300, 250, 1), "zoom in so the pan has room");
		double tZoomX = 300.0 - (300.0 - FIT_X) * (MaterialTreeViewport.MAX_SCALE / FIT);
		double tZoomY = 250.0 * (1.0 - MaterialTreeViewport.MAX_SCALE / FIT);
		List<ItemStack> tJumped = new ArrayList<>();
		tScreen.mJump = tJumped::add;

		// a press, dragged well past the slop: pans, never jumps
		assertTrue(tScreen.mouseClicked(560, 460, 0), "the press starts the drag");
		assertTrue(tScreen.mouseDragged(580, 470, 0, 20, 10), "the drag pans");
		assertEquals(tZoomX + 20, tScreen.mView.offsetX(), EPSILON, "the zoom offset + 20, inside the real-pane clamp");
		assertEquals(tZoomY + 10, tScreen.mView.offsetY(), EPSILON, "the zoom offset + 10, inside the real-pane clamp");
		assertTrue(tScreen.mouseReleased(580, 470, 0));
		assertTrue(tJumped.isEmpty(), "a dragged release is no jump");

		// a press on a node, released in place: the jump fires with the node's stack
		MaterialTreeViewport.Point tNode = tScreen.mView.apply(
				MaterialTreeDisplay.nodeX(tScreen.mDisplay.nodes().get(0)) + 9,
				MaterialTreeDisplay.nodeY(tScreen.mDisplay.nodes().get(0)) + 9);
		assertTrue(tScreen.mouseClicked(tNode.x(), tNode.y(), 0));
		assertTrue(tScreen.mouseReleased(tNode.x(), tNode.y(), 0));
		assertEquals(1, tJumped.size(), "the clean click jumps");
		assertEquals(tScreen.mDisplay.nodes().get(0).stack().getItem(), tJumped.get(0).getItem(), "the clicked node's stack");

		// a press on a node that becomes a drag does NOT jump (the slop guard)
		assertTrue(tScreen.mouseClicked(tNode.x(), tNode.y(), 0));
		assertTrue(tScreen.mouseDragged(tNode.x() + 30, tNode.y() + 30, 0, 30, 30));
		assertTrue(tScreen.mouseReleased(tNode.x() + 30, tNode.y() + 30, 0));
		assertEquals(1, tJumped.size(), "still exactly one jump");
	}

	// ------------------------------------------------------------------
	// keyboard (the M2 action table verbatim)
	// ------------------------------------------------------------------

	@Test
	public void keyboardPansAndUnmappedKeysFallThrough() {
		GT6MaterialTreeScreen tScreen = screenAt(600, 500);
		assertTrue(tScreen.scroll(300, 250, 1));
		double tZoomX = 300.0 - (300.0 - FIT_X) * (MaterialTreeViewport.MAX_SCALE / FIT);
		assertTrue(tScreen.keyPressed(263, 0, 0), "LEFT is consumed");
		assertEquals(tZoomX + 28.0, tScreen.mView.offsetX(), EPSILON, "PAN_LEFT = +28, the image-viewer semantics");
		assertFalse(tScreen.keyPressed(65, 0, 0), "an unmapped key falls through");
		// the pan range follows the REAL window (acceptance ②): drive to the corner
		assertTrue(tScreen.mouseClicked(300, 250, 0), "the press arms the drag");
		assertTrue(tScreen.mouseDragged(0, 0, 0, -4000, -4000), "a huge drag slams the corner");
		assertEquals(600.0 - 202.0 * MaterialTreeViewport.MAX_SCALE, tScreen.mView.offsetX(), EPSILON,
				"the x pan floor is the real pane 600 - 202*4");
		assertEquals(500.0 - 206.0 * MaterialTreeViewport.MAX_SCALE, tScreen.mView.offsetY(), EPSILON,
				"the y pan floor is the real pane 500 - 206*4");
	}

	// ------------------------------------------------------------------
	// the strip buttons (+/−/R)
	// ------------------------------------------------------------------

	@Test
	public void stripButtonsDriveTheNavTable() {
		GT6MaterialTreeScreen tScreen = screenAt(600, 500);
		assertTrue(tScreen.mouseClicked(8, 8, 0), "the + cell");
		assertEquals(MaterialTreeViewport.MAX_SCALE, tScreen.mView.scale(), EPSILON, "one 档位 rides the clamp onto the ceiling");
		assertTrue(tScreen.mouseClicked(24, 8, 0), "the - cell");
		assertEquals(FIT, tScreen.mView.scale(), EPSILON, "one 档位 down re-freezes on the fit floor");
		assertTrue(tScreen.mouseClicked(40, 8, 0), "the R cell");
		assertEquals(FIT_X, tScreen.mView.offsetX(), EPSILON, "reset is the exact centred fit");
		assertEquals(0.0, tScreen.mView.offsetY(), EPSILON);
		// outside the cells the press starts a drag (and jumping on release stays the slop's business)
		assertTrue(tScreen.mouseClicked(300, 300, 0));
		assertTrue(tScreen.mouseReleased(300, 300, 0));
	}

	// ------------------------------------------------------------------
	// resize 保锚 (mattree-viewport-fit: the anchor survives the window resize)
	// ------------------------------------------------------------------

	@Test
	public void resizeKeepsTheAnchorOnTheLiveViewport() {
		GT6MaterialTreeScreen tScreen = screenAt(600, 500);
		assertTrue(tScreen.scroll(300, 250, 1), "zoom in first");
		MaterialTreeViewport.Point tBefore = tScreen.mView.unapply(300, 250);
		tScreen.navInit(800, 600); // the window resize re-inits — the live viewport survives
		assertEquals(MaterialTreeViewport.MAX_SCALE, tScreen.mView.scale(), EPSILON, "the zoom level survives the resize");
		MaterialTreeViewport.Point tAfter = tScreen.mView.unapply(400, 300);
		assertEquals(tBefore.x(), tAfter.x(), EPSILON, "the new pane centre shows the old pane centre's tree point");
		assertEquals(tBefore.y(), tAfter.y(), EPSILON, "the new pane centre shows the old pane centre's tree point");
		assertEquals(-4.0, tScreen.mView.offsetX(), EPSILON, "400 - 101*4");
		assertEquals(-112.0, tScreen.mView.offsetY(), EPSILON, "300 - 103*4");
		// a degenerate pane (a minimized window's 0) keeps the last pose untouched
		tScreen.navInit(0, 0);
		assertEquals(-4.0, tScreen.mView.offsetX(), EPSILON);
		// while a FRESH screen still opens on the centred fit of its real pane
		GT6MaterialTreeScreen tFresh = screenAt(800, 600);
		MaterialTreeViewport.Point tCentre = tFresh.mView.apply(MaterialTreeDisplay.WIDTH / 2.0, MaterialTreeDisplay.HEIGHT / 2.0);
		assertEquals(400.0, tCentre.x(), EPSILON);
		assertEquals(300.0, tCentre.y(), EPSILON);
	}

	// ------------------------------------------------------------------
	// the viewer jump routing (the GTViewerJump predicate, reused)
	// ------------------------------------------------------------------

	@Test
	public void pureJumpArmsRefuseWithoutAViewer() {
		ItemStack tStack = ironDisplay().nodes().get(0).stack();
		assertFalse(GT6MaterialTreeScreen.openInViewer(null, "emi"), "no stack, no jump");
		assertFalse(GT6MaterialTreeScreen.openInViewer(ItemStack.EMPTY, "emi"), "an empty stack, no jump");
		assertFalse(GT6MaterialTreeScreen.openInViewer(tStack, null), "no viewer answer, no jump");
		assertFalse(GT6MaterialTreeScreen.openInViewer(tStack, "nei"), "an unknown viewer answer, no jump");
	}

	@Test
	public void bytecodePinsTheJumpAndOpenSeams() throws Exception {
		String tScreen = bytesOf(GT6MaterialTreeScreen.class);
		assertTrue(tScreen.contains("gregtech6/emi/GT6EmiPlugin"), "the EMI jump leg");
		assertTrue(tScreen.contains("gregtech6/jei/GT6JeiPlugin"), "the JEI jump leg");
		assertTrue(tScreen.contains("gregtech6/gui/GTViewerJump"), "the shared preferredViewer predicate");
		assertTrue(tScreen.contains("net/minecraft/client/Minecraft"), "the open face");
		assertTrue(tScreen.contains("setScreen"), "the open seam replaces the current screen");
		assertTrue(bytesOf(GT6EmiPlugin.class).contains("openItemPage"), "the EMI item face exists");
		assertTrue(bytesOf(GT6JeiPlugin.class).contains("openItemPage"), "the JEI item face exists");
	}

	// ------------------------------------------------------------------
	// the offline fixture (the GT6MaterialTreeNavTest probe form, condensed)
	// ------------------------------------------------------------------

	private static GT6MaterialTreeScreen screenAt(int aPaneWidth, int aPaneHeight) {
		GT6MaterialTreeScreen tScreen = new GT6MaterialTreeScreen(ironDisplay());
		tScreen.navInit(aPaneWidth, aPaneHeight);
		return tScreen;
	}

	private static final Map<PrefixMaterial, Item> PREFIX_ITEMS = new HashMap<>();
	private static int sNextProbeId = 0;

	/** The offline universe + the OPEN item registry — the probe items register during the pour. */
	@BeforeAll
	static void openTheUniverseAndRegistry() {
		GTMaterialItems.initMaterials();
		openOfflineItemRegistry();
	}

	private static Item prefixItem(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		if (aPrefix == null || aMaterial == null) return null;
		return PREFIX_ITEMS.computeIfAbsent(new PrefixMaterial(aPrefix, aMaterial), aPair ->
			Registry.register(BuiltInRegistries.ITEM, "mtree_s4_probe_" + sNextProbeId++,
				new MaterialPrefixItem(new Item.Properties(), aPair.prefix(), aPair.material())));
	}

	private static final List<Runnable> sSeamRestores = new ArrayList<>();

	@BeforeEach
	void armSeamsAndPourOreChain() {
		GT6RecipeMaps.reset();
		captureResolver(GT6RecipesOreChain.class);
		captureResolver(GT6RecipesShCL.class);
		captureResolver(GT6RecipesAnvil.class);
		captureResolver(GT6RecipesSifter.class);
		GT6RecipesOreChain.load();
		GT6RecipesShCL.load();
		GT6RecipesAnvil.load();
		GT6RecipesSifter.load();
	}

	@AfterEach
	void restoreSeamsAndDropGeneration() {
		for (Runnable tRestore : sSeamRestores) tRestore.run();
		sSeamRestores.clear();
		GT6RecipeMaps.reset();
	}

	/** Captures the loader's package-private sMaterialItemResolver, arms the probe, queues the restore. */
	@SuppressWarnings("unchecked")
	private static void captureResolver(Class<?> aLoader) {
		try {
			Field tField = aLoader.getDeclaredField("sMaterialItemResolver");
			tField.setAccessible(true);
			Object tDefault = tField.get(null);
			sSeamRestores.add(() -> {
				try {
					tField.set(null, tDefault);
				} catch (ReflectiveOperationException aE) {
					throw new IllegalStateException("resolver restore failed", aE);
				}
			});
			tField.set(null, (java.util.function.BiFunction<OreDictPrefix, OreDictMaterial, Item>) GT6MaterialTreeScreenTest::prefixItem);
		} catch (ReflectiveOperationException aE) {
			throw new IllegalStateException("resolver seam missing on " + aLoader.getSimpleName(), aE);
		}
	}

	/** The Iron display (the NavTest ironRecipe form, minus the recipe wrapper). */
	private static MaterialTreeDisplay ironDisplay() {
		openOfflineItemRegistry();
		MaterialTreeBuilder tTree = MaterialTreeBuilder.build();
		MaterialTreeDisplay tFe = MaterialTreeDisplay.of(tTree, MT.Fe, GT6MaterialTreeScreenTest::prefixItem);
		assertNotNull(tFe, "the iron ore walk yields a display");
		return tFe;
	}

	// the display test's offline registry unlock, verbatim shape
	private static void openOfflineItemRegistry() {
		var tRegistry = BuiltInRegistries.ITEM;
		//? if forge {
		try {
			java.lang.reflect.Method tUnfreeze = tRegistry.getClass().getMethod("unfreeze");
			tUnfreeze.setAccessible(true);
			tUnfreeze.invoke(tRegistry);
			Field tDelegate = inheritedField(tRegistry.getClass(), "delegate");
			tDelegate.setAccessible(true);
			Object tForgeRegistry = tDelegate.get(tRegistry);
			java.lang.reflect.Method tForgeUnfreeze = tForgeRegistry.getClass().getMethod("unfreeze");
			tForgeUnfreeze.setAccessible(true);
			tForgeUnfreeze.invoke(tForgeRegistry);
			Field tLocked = inheritedField(tRegistry.getClass(), "locked");
			tLocked.setBoolean(tRegistry, false);
		} catch (Exception aE) {
			throw new IllegalStateException("could not open the offline item registry [" + tRegistry.getClass().getName() + "]", aE);
		}
		//?} else {
		/*try {
		java.lang.reflect.Method tUnfreeze = tRegistry.getClass().getMethod("unfreeze");
		tUnfreeze.setAccessible(true);
		tUnfreeze.invoke(tRegistry);
		} catch (Exception aE) {
		throw new IllegalStateException("could not open the offline item registry [" + tRegistry.getClass().getName() + "]", aE);
		}
		*///?}
	}

	/** getDeclaredField along the superclass chain (the DisplayTest helper, mirrored). */
	private static Field inheritedField(Class<?> aClass, String aName) throws NoSuchFieldException {
		for (Class<?> c = aClass; c != null; c = c.getSuperclass()) {
			try {
				Field rField = c.getDeclaredField(aName);
				rField.setAccessible(true);
				return rField;
			} catch (NoSuchFieldException ignored) {
				// walk up
			}
		}
		throw new NoSuchFieldException(aName);
	}

	private static String bytesOf(Class<?> aClass) throws Exception {
		try (var tIn = aClass.getResourceAsStream(aClass.getSimpleName() + ".class")) {
			assertNotNull(tIn, aClass.getSimpleName() + " class bytes");
			return new String(tIn.readAllBytes(), java.nio.charset.StandardCharsets.ISO_8859_1);
		}
	}
}
