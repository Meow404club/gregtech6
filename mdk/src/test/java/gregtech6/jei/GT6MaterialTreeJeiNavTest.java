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

package gregtech6.jei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import mezz.jei.api.gui.inputs.IJeiGuiEventListener;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.IRecipeWidget;

import gregtech6.emi.GT6MaterialTreeEmiRecipe;
import gregtech6.emi.GT6MaterialTreeNav;
import gregtech6.recipes.tree.MaterialTreeDisplay;
import gregtech6.recipes.tree.MaterialTreeViewport;
import net.minecraft.client.gui.navigation.ScreenRectangle;

/**
 * The JEI material-tree page nav pins (task nav-m3-jei, the M3 card of the r11-nav-suite,
 * consuming the M2 {@link gregtech6.emi.GT6MaterialTreeNav} action table verbatim):
 *
 * <ul>
 * <li><b>control strip band</b>: the category grows by the SAME 20px strip the EMI twin
 *     uses ({@link GT6MaterialTreeEmiRecipe#CONTROL_STRIP_H}), width unchanged;</li>
 * <li><b>registration seam</b>: {@code createRecipeExtras} mounts the self-drawn tree body
 *     widget, the nav face (an {@link IRecipeWidget} draw hat + an {@link IJeiGuiEventListener}
 *     input hat — one instance, one shared viewport closure) and the S4 corner entry, plus
 *     the whole-canvas pan/zoom handler on the {@code addInputHandler} seam and the hover
 *     hints (task mattree-jei-panzoom updated the counts — the javadoc here states the
 *     migration);</li>
 * <li><b>双向钉 (acceptance ①)</b>: strip button clicks and canvas keys drive the viewport
 *     both ways (2x 档位, centre anchor, ceiling freeze idempotent, reset home);</li>
 * <li><b>red line</b>: clicks outside the three strip cells and unmapped keys fall through
 *     unconsumed — the canvas click/wheel/drag domain belongs to the canvas input handler
 *     (pinned in {@code GT6MaterialTreeJeiPanzoomTest});</li>
 * <li><b>apiSurface guard (acceptance ③)</b>: every {@code mezz/jei/} reference in the
 *     new/changed JEI classes' bytes lives under {@code mezz/jei/api/}, and no
 *     {@code dev/emi/emi} leaks into the JEI leg.</li>
 * </ul>
 *
 * <p>Offline by construction: no display build, no registry, no bootstrap — the nav face
 * is pure viewport state plus JEI api value types (pure JUnit, runs in any order).
 */
public class GT6MaterialTreeJeiNavTest {

	private static final double EPSILON = 1e-9;
	private static final double tCx = MaterialTreeDisplay.WIDTH / 2.0, tCy = MaterialTreeDisplay.HEIGHT / 2.0;

	// ------------------------------------------------------------------
	// the control strip band (the EMI-parity page geometry)
	// ------------------------------------------------------------------

	@Test
	public void jeiCategoryGrowsTheControlStripBand() {
		GT6MaterialTreeJeiCategory tCategory = new GT6MaterialTreeJeiCategory();
		assertEquals(MaterialTreeDisplay.WIDTH, tCategory.getWidth(), "the canvas width is untouched");
		assertEquals(MaterialTreeDisplay.HEIGHT + GT6MaterialTreeEmiRecipe.CONTROL_STRIP_H, tCategory.getHeight(),
				"the strip band extends the category below the tree canvas (the EMI twin's 20px)");
	}

	// ------------------------------------------------------------------
	// the registration seam (createRecipeExtras: one face, two hats)
	// ------------------------------------------------------------------

	@Test
	public void createRecipeExtrasRegistersOneSharedNavFace() {
		GT6MaterialTreeJeiCategory tCategory = new GT6MaterialTreeJeiCategory();
		RecordingExtras tExtras = new RecordingExtras();
		tCategory.createRecipeExtras(tExtras.self(), null, null);

		// the panzoom union (task mattree-jei-panzoom): the extras mounts THREE widgets —
		// the self-drawn tree body first (draw-only hat), then the nav strip, then the S4
		// corner entry (both hats over their own instances) — and the whole-canvas pan/zoom/
		// jump face rides the separate addInputHandler seam.
		// 旧钉迁移声明: the old 「2 widgets / 2 listeners」 pin died with the unfreeze —
		// the replacement registration shape is 3 widgets / 2 listeners / 1 input handler.
		assertEquals(3, tExtras.mWidgets.size(), "tree body + nav strip + S4 corner cell");
		assertEquals(2, tExtras.mListeners.size(), "nav strip + corner cell listen");
		assertEquals(1, tExtras.mInputHandlers.size(), "one whole-canvas input handler");
		assertTrue(tExtras.mWidgets.get(0) instanceof gregtech6.jei.GT6MaterialTreeJeiTreeWidget,
				"the tree body leads the widget stack (the draw hat)");
		assertTrue(tExtras.mWidgets.get(1) instanceof GT6MaterialTreeJeiNavWidget, "the nav face is the port's widget");
		assertSame(tExtras.mWidgets.get(1), tExtras.mListeners.get(0),
				"draw and input are ONE instance = one shared viewport closure");
		assertTrue(tExtras.mWidgets.get(1) instanceof IRecipeWidget
				&& tExtras.mListeners.get(0) instanceof IJeiGuiEventListener,
				"the face wears both hats");
		assertTrue(tExtras.mWidgets.get(2) instanceof gregtech6.jei.GT6MaterialTreeJeiScreenButton,
				"the corner entry rides the same seam (nav-s4-tree-screen)");
		assertSame(tExtras.mWidgets.get(2), tExtras.mListeners.get(1), "the cell wears both hats too");

		// the draw origin is the page origin, so draw/input coordinates are page coordinates
		GT6MaterialTreeJeiNavWidget tNav = (GT6MaterialTreeJeiNavWidget)tExtras.mWidgets.get(1);
		assertEquals(0, tNav.getPosition().x(), "draw origin x");
		assertEquals(0, tNav.getPosition().y(), "draw origin y");

		// the listener area spans the whole page (strip included) — JEI translates event
		// coordinates to its origin, so the routing cells are page coordinates
		ScreenRectangle tArea = tNav.getArea();
		assertEquals(0, tArea.position().x(), "area x");
		assertEquals(0, tArea.position().y(), "area y");
		assertEquals(MaterialTreeDisplay.WIDTH, tArea.width(), "area width");
		assertEquals(MaterialTreeDisplay.HEIGHT + GT6MaterialTreeEmiRecipe.CONTROL_STRIP_H, tArea.height(), "area height");

		// three hover hints on the EMI-parity button cells (12px cells, 4px gaps) + the
		// S4 corner cell's hint (the union's fourth area)
		assertEquals(4, tExtras.mTooltipAreas.size(), "zoom in, zoom out, reset + open full tree");
		assertEquals(gregtech6.gui.GT6MaterialTreeScreen.SCREEN_BUTTON_X, tExtras.mTooltipAreas.get(3)[0], "corner cell x");
		assertEquals(gregtech6.gui.GT6MaterialTreeScreen.SCREEN_BUTTON_Y, tExtras.mTooltipAreas.get(3)[1], "corner cell y");
		for (int i = 0; i < 3; i++) {
			int[] tRect = tExtras.mTooltipAreas.get(i);
			assertEquals(GT6MaterialTreeEmiRecipe.BUTTON_X0 + i * GT6MaterialTreeEmiRecipe.BUTTON_PITCH, tRect[0],
					"tooltip area " + i + " x rides the shared EMI strip constants");
			assertEquals(GT6MaterialTreeEmiRecipe.BUTTON_Y, tRect[1], "tooltip area " + i + " y");
			assertEquals(12, tRect[2], "tooltip area " + i + " width");
			assertEquals(12, tRect[3], "tooltip area " + i + " height");
			assertFalse(tExtras.mTooltipTexts.get(i).isEmpty(), "tooltip area " + i + " carries a hint");
		}
	}

	// ------------------------------------------------------------------
	// acceptance ①: strip button clicks -> viewport state, both ways
	// ------------------------------------------------------------------

	@Test
	public void stripButtonsDriveTheViewportBothWays() {
		GT6MaterialTreeJeiNavWidget tNav = new GT6MaterialTreeJeiNavWidget(new MaterialTreeViewport());
		MaterialTreeViewport tView = tNav.mView;
		int tY = GT6MaterialTreeEmiRecipe.BUTTON_Y + 1;

		// zoom-in cell: the exact 2x 档位 anchored at the canvas centre (101/103 = centre*(1-2))
		assertTrue(tNav.mouseClicked(GT6MaterialTreeEmiRecipe.BUTTON_X0 + 1, tY, 0), "zoom-in click consumed");
		assertEquals(2.0, tView.scale(), EPSILON);
		assertEquals(-101.0, tView.offsetX(), EPSILON);
		assertEquals(-103.0, tView.offsetY(), EPSILON);
		// zoom-out cell undoes it exactly
		assertTrue(tNav.mouseClicked(GT6MaterialTreeEmiRecipe.BUTTON_X0 + GT6MaterialTreeEmiRecipe.BUTTON_PITCH + 1, tY, 0),
				"zoom-out click consumed");
		assertEquals(1.0, tView.scale(), EPSILON);
		assertEquals(0.0, tView.offsetX(), EPSILON);
		assertEquals(0.0, tView.offsetY(), EPSILON);

		// the ceiling freezes (S1 档位幂等 through the button, no residue)
		for (int i = 0; i < 5; i++)
			assertTrue(tNav.mouseClicked(GT6MaterialTreeEmiRecipe.BUTTON_X0 + 1, tY, 0));
		assertEquals(MaterialTreeViewport.MAX_SCALE, tView.scale(), EPSILON);
		double tFrozenX = tView.offsetX(), tFrozenY = tView.offsetY();
		assertTrue(tNav.mouseClicked(GT6MaterialTreeEmiRecipe.BUTTON_X0 + 1, tY, 0));
		assertEquals(MaterialTreeViewport.MAX_SCALE, tView.scale(), EPSILON);
		assertEquals(tFrozenX, tView.offsetX(), EPSILON);
		assertEquals(tFrozenY, tView.offsetY(), EPSILON);

		// reset cell: the exact unit transform from anywhere
		assertTrue(tNav.mouseClicked(GT6MaterialTreeEmiRecipe.BUTTON_X0 + 2 * GT6MaterialTreeEmiRecipe.BUTTON_PITCH + 1, tY, 0),
				"reset click consumed");
		assertEquals(1.0, tView.scale(), EPSILON);
		assertEquals(0.0, tView.offsetX(), EPSILON);
		assertEquals(0.0, tView.offsetY(), EPSILON);
	}

	// ------------------------------------------------------------------
	// acceptance ①: canvas keyboard -> viewport state
	// ------------------------------------------------------------------

	@Test
	public void canvasKeyboardDrivesTheViewport() {
		GT6MaterialTreeJeiNavWidget tNav = new GT6MaterialTreeJeiNavWidget(new MaterialTreeViewport());
		MaterialTreeViewport tView = tNav.mView;

		// '+' zooms in (GLFW_KEY_EQUAL 61), R resets (GLFW_KEY_R 82)
		assertTrue(tNav.keyPressed(100, 100, 61, 0, 0), "+ is consumed");
		assertEquals(2.0, tView.scale(), EPSILON);
		assertEquals(-101.0, tView.offsetX(), EPSILON);
		assertTrue(tNav.keyPressed(100, 100, 82, 0, 0), "R is consumed");
		assertEquals(1.0, tView.scale(), EPSILON);
		assertEquals(0.0, tView.offsetX(), EPSILON);

		// the arrows are consumed at the fit pose too, but the clamp pins the state
		// (the S1 fit 姿态 has no pan range — the keys wake up past the first zoom)
		assertTrue(tNav.keyPressed(100, 100, 265, 0, 0), "UP is consumed");
		assertEquals(0.0, tView.offsetY(), EPSILON, "the fit pose has no pan range (clamped)");
		assertTrue(tNav.keyPressed(100, 100, 61, 0, 0));
		assertTrue(tNav.keyPressed(100, 100, 265, 0, 0), "UP is consumed after zoom");
		assertEquals(GT6MaterialTreeNav.PAN_STEP - 103.0, tView.offsetY(), EPSILON,
				"UP pans one lane toward the ceiling (offset -103 -> -75)");
		assertTrue(tNav.keyPressed(100, 100, 48, 0, 0), "0 resets");
		assertEquals(1.0, tView.scale(), EPSILON);
	}

	// ------------------------------------------------------------------
	// the red line: clicks/keys outside the strip cells fall through
	// ------------------------------------------------------------------

	@Test
	public void clicksAndKeysOutsideTheStripCellsFallThrough() {
		GT6MaterialTreeJeiNavWidget tNav = new GT6MaterialTreeJeiNavWidget(new MaterialTreeViewport());
		MaterialTreeViewport tView = tNav.mView;

		// a canvas click is unconsumed and inert here — it belongs to the registered canvas
		// input handler (JEI routes input handlers BEFORE the gui event listeners; the old
		// 「native slot territory」 reading died with the invisible slots — the replacement
		// face is GT6MaterialTreeJeiPanzoomTest's click-protocol pins)
		assertFalse(tNav.mouseClicked(16, 60, 0), "canvas clicks fall through to the canvas input handler");
		assertEquals(1.0, tView.scale(), EPSILON);
		// a non-left button on a strip cell is unconsumed too
		assertFalse(tNav.mouseClicked(GT6MaterialTreeEmiRecipe.BUTTON_X0 + 1, GT6MaterialTreeEmiRecipe.BUTTON_Y + 1, 1),
				"only the left button drives the strip");
		assertEquals(1.0, tView.scale(), EPSILON);
		// typing keys and controls stay with JEI (search etc.)
		assertFalse(tNav.keyPressed(100, 100, 65, 0, 0), "A falls through");
		assertFalse(tNav.keyPressed(100, 100, 256, 0, 0), "ESC falls through");
		assertFalse(tNav.keyPressed(100, 100, 257, 0, 0), "ENTER falls through");
		assertEquals(1.0, tView.scale(), EPSILON);
		assertEquals(0.0, tView.offsetX(), EPSILON);
	}

	// ------------------------------------------------------------------
	// acceptance ③: the apiSurface guard — JEI api only, byte-level
	// ------------------------------------------------------------------

	@Test
	public void jeiNavClassesStayOnTheJeiApiSurface() throws Exception {
		for (Class<?> tClass : List.of(GT6MaterialTreeJeiNavWidget.class, GT6MaterialTreeJeiCategory.class)) {
			String tBytes = bytesOf(tClass);
			int tAt = 0;
			while ((tAt = tBytes.indexOf("mezz/jei/", tAt)) >= 0) {
				assertTrue(tBytes.startsWith("mezz/jei/api/", tAt),
						tClass.getSimpleName() + " references a non-api JEI type at byte " + tAt);
				tAt += 1;
			}
			assertFalse(tBytes.contains("dev/emi/emi"), tClass.getSimpleName() + " stays EMI-free");
		}
	}

	private static String bytesOf(Class<?> aClass) throws Exception {
		try (var tIn = aClass.getResourceAsStream(aClass.getSimpleName() + ".class")) {
			assertNotNull(tIn, aClass.getSimpleName() + " class bytes");
			return new String(tIn.readAllBytes(), java.nio.charset.StandardCharsets.ISO_8859_1);
		}
	}

	// ------------------------------------------------------------------
	// the recording double: a Proxy so both JEI generations (15.x/19.x) fit
	// without hand-stubbing the whole builder interface
	// ------------------------------------------------------------------

	private static final class RecordingExtras implements InvocationHandler {
		final List<Object> mWidgets = new ArrayList<>();
		final List<Object> mListeners = new ArrayList<>();
		final List<Object> mInputHandlers = new ArrayList<>();
		final List<int[]> mTooltipAreas = new ArrayList<>();
		final List<List<Object>> mTooltipTexts = new ArrayList<>();
		private List<Object> mOpenTooltips = new ArrayList<>();

		final IRecipeExtrasBuilder self() {
			return (IRecipeExtrasBuilder)Proxy.newProxyInstance(IRecipeExtrasBuilder.class.getClassLoader(),
					new Class<?>[] {IRecipeExtrasBuilder.class}, this);
		}

		@Override
		public Object invoke(Object aProxy, Method aMethod, Object[] aArgs) {
			switch (aMethod.getName()) {
				case "addWidget" -> mWidgets.add(aArgs[0]);
				case "addGuiEventListener" -> mListeners.add(aArgs[0]);
				case "addInputHandler" -> mInputHandlers.add(aArgs[0]);
				case "addTooltipArea" -> {
					mTooltipAreas.add(new int[] {(Integer)aArgs[0], (Integer)aArgs[1], (Integer)aArgs[2], (Integer)aArgs[3]});
					mOpenTooltips = new ArrayList<>();
					mTooltipTexts.add(mOpenTooltips);
					return Proxy.newProxyInstance(IRecipeExtrasBuilder.class.getClassLoader(),
							new Class<?>[] {mezz.jei.api.gui.widgets.IDrawableWidget.class}, this);
				}
				case "setTooltip" -> {
					if (mOpenTooltips != null && aArgs != null && aArgs.length > 0) mOpenTooltips.add(aArgs[0]);
					return aProxy;
				}
			}
			// everything else this card never calls: a safe type-appropriate default
			return aMethod.getReturnType() == boolean.class ? Boolean.FALSE
					: aMethod.getReturnType() == int.class ? Integer.valueOf(0) : null;
		}
	}
}
