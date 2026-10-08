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

package gregtech6.emi;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.emi.emi.api.widget.ButtonWidget;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.DrawableWidget;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;

import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.gui.GT6MaterialTreeScreen;
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

/**
 * The EMI material-tree page nav suite pins (task nav-m2-emi, the M2 card of the
 * r11-nav-suite, consuming the S1 {@link MaterialTreeViewport}):
 *
 * <ul>
 * <li><b>nav action table</b>: the viewer-neutral {@link GT6MaterialTreeNav} drives the
 *     viewport both ways — zoom-in/out (exact 2x 档位 factors, the floor freeze included),
 *     the four pan arrows (one 28px lane per press, the clamp respected), reset to the
 *     exact unit transform (acceptance ①);</li>
 * <li><b>keyboard map</b>: GLFW key codes (verified javap lwjgl-glfw 3.3.1) map to the
 *     actions, unknown keys return null;</li>
 * <li><b>wheel actions</b> (<b>旧钉迁移声明</b>, task mattree-r3-nav-unify): the
 *     emi-panzoom WHEEL_IN/WHEEL_OUT table entries and their direct pin died here — the
 *     pointer-anchored 1.25x fine grain they covered is {@link GT6MaterialTreeNav#wheelZoom}'s
 *     own pin (wheelZoomAnchorsAtThePointerAndDragsPan), and the hostile-delta divergence the
 *     seam's signum branch carried (a NaN delta scrolled OUT) is now pinned frozen through the
 *     one guard, in thePointerSeamGatesWheelAndPanToTheCanvas;</li>
 * <li><b>the pointer seam</b> (task mattree-emi-panzoom): the nav canvas implements the
 *     vendored MUI {@code EmiInteractionSink} — wheel consumes only inside the tree
 *     canvas (strip and off-page scrolls stay EMI's) and pans only from an empty-canvas
 *     press, while slot presses yield to the native U/R/drag-stack faces and a dead
 *     gesture never consumes release;</li>
 * <li><b>double-click detection (mattree-zoom-anchor)</b>: {@link GT6MaterialTreeNav#isDoubleClick}
 *     is the vanilla list idiom's 250 ms window made spatial (the 4 px radius) — the screen
 *     leg's double-click re-fit rides it;</li>
 * <li><b>click-track slider</b>: a click at track fraction f zooms to MIN + f*(MAX-MIN)
 *     about the canvas centre (the direct "point at a level" face beside the seam);</li>
 * <li><b>wheel/drag ops (task mattree-jei-panzoom)</b>: the JEI canvas face of the table —
 *     wheelZoom steps WHEEL_STEP about the POINTER anchor (sign-direction, zero/NaN no-op,
 *     ceiling freeze), dragPan is the clamped pan.</li>
 * <li><b>apply/unapply closure</b>: through every nav op the apply/unapply roundtrip
 *     stays exact, and two page opens are independent identities (no static state);</li>
 * <li><b>TransformSlot seam</b>: the bounds math (SlotWidget.getBounds is a non-final
 *     @Override the emi 1.1.24 render/hit/tooltip faces all consume — SlotWidget.java
 *     :146-155) is the viewport transform, exact at identity and at an anchored zoom;</li>
 * <li><b>apiSurface guard (acceptance ②)</b>: every {@code dev/emi/emi/} reference in the
 *     four NEW classes' compiled bytes lives under {@code dev/emi/emi/api/} — no EMI
 *     internal (unstable) API enters the nav suite;</li>
 * <li><b>page wiring</b>: the real {@link GT6MaterialTreeEmiRecipe#addWidgets} layout —
 *     the wires/labels drawable leads the z stack, every slot is a TransformSlot, the
 *     control strip (three ButtonWidgets + slider + key canvas) rides below the canvas —
 *     and driving the REAL widgets (button actions via the action field, the slider/nav
 *     widgets directly) moves the viewport and reset brings the page home (acceptance ①).</li>
 * </ul>
 */
public class GT6MaterialTreeNavTest extends GTRecipesOfflineTestBase {

	private static final double EPSILON = 1e-9;

	// ------------------------------------------------------------------
	// the nav action table (acceptance ①: viewport state moves both ways)
	// ------------------------------------------------------------------

	@Test
	public void navActionTableMovesTheViewportBothWays() {
		MaterialTreeViewport tView = new MaterialTreeViewport();
		double tCx = MaterialTreeDisplay.WIDTH / 2.0, tCy = MaterialTreeDisplay.HEIGHT / 2.0;

		// zoom in: the exact 2x 档位 anchored at the canvas centre (101*(1-2) = -101)
		assertTrue(GT6MaterialTreeNav.handle(tView, GT6MaterialTreeNav.Action.ZOOM_IN, tCx, tCy));
		assertEquals(2.0, tView.scale(), EPSILON);
		assertEquals(-101.0, tView.offsetX(), EPSILON);
		assertEquals(-103.0, tView.offsetY(), EPSILON);
		// and out again
		assertTrue(GT6MaterialTreeNav.handle(tView, GT6MaterialTreeNav.Action.ZOOM_OUT, tCx, tCy));
		assertEquals(1.0, tView.scale(), EPSILON);
		assertEquals(0.0, tView.offsetX(), EPSILON);
		assertEquals(0.0, tView.offsetY(), EPSILON);

		// the ceiling freezes (zoom past MAX is the S1 档位幂等 no-op)
		assertTrue(GT6MaterialTreeNav.handle(tView, GT6MaterialTreeNav.Action.ZOOM_IN, tCx, tCy));
		assertTrue(GT6MaterialTreeNav.handle(tView, GT6MaterialTreeNav.Action.ZOOM_IN, tCx, tCy));
		assertEquals(MaterialTreeViewport.MAX_SCALE, tView.scale(), EPSILON);
		double tFrozenX = tView.offsetX(), tFrozenY = tView.offsetY();
		assertTrue(GT6MaterialTreeNav.handle(tView, GT6MaterialTreeNav.Action.ZOOM_IN, tCx, tCy));
		assertEquals(MaterialTreeViewport.MAX_SCALE, tView.scale(), EPSILON);
		assertEquals(tFrozenX, tView.offsetX(), EPSILON);
		assertEquals(tFrozenY, tView.offsetY(), EPSILON);

		// the pan arrows: one 28px lane per press, up = content down (offset grows toward 0)
		assertTrue(GT6MaterialTreeNav.handle(tView, GT6MaterialTreeNav.Action.PAN_UP, tCx, tCy));
		assertEquals(tFrozenY + GT6MaterialTreeNav.PAN_STEP, tView.offsetY(), EPSILON);
		assertTrue(GT6MaterialTreeNav.handle(tView, GT6MaterialTreeNav.Action.PAN_DOWN, tCx, tCy));
		assertEquals(tFrozenY, tView.offsetY(), EPSILON);
		assertTrue(GT6MaterialTreeNav.handle(tView, GT6MaterialTreeNav.Action.PAN_LEFT, tCx, tCy));
		assertEquals(tFrozenX + GT6MaterialTreeNav.PAN_STEP, tView.offsetX(), EPSILON);
		assertTrue(GT6MaterialTreeNav.handle(tView, GT6MaterialTreeNav.Action.PAN_RIGHT, tCx, tCy));
		assertEquals(tFrozenX, tView.offsetX(), EPSILON);

		// reset: the exact unit transform from anywhere
		assertTrue(GT6MaterialTreeNav.handle(tView, GT6MaterialTreeNav.Action.RESET, tCx, tCy));
		assertEquals(1.0, tView.scale(), EPSILON);
		assertEquals(0.0, tView.offsetX(), EPSILON);
		assertEquals(0.0, tView.offsetY(), EPSILON);

		// the pan floor also clamps (the 防树飞出视野 clause through the nav table)
		assertTrue(GT6MaterialTreeNav.handle(tView, GT6MaterialTreeNav.Action.ZOOM_IN, tCx, tCy));
		for (int i = 0; i < 100; i++)
			GT6MaterialTreeNav.handle(tView, GT6MaterialTreeNav.Action.PAN_DOWN, tCx, tCy);
		assertEquals(MaterialTreeDisplay.HEIGHT * (1 - tView.scale()), tView.offsetY(), EPSILON,
				"the y pan floor is the canvas basis (206*(1-scale))");

		// guards: null action / null viewport are a consumed-nothing false
		assertFalse(GT6MaterialTreeNav.handle(tView, null, tCx, tCy));
		assertFalse(GT6MaterialTreeNav.handle(null, GT6MaterialTreeNav.Action.ZOOM_IN, tCx, tCy));
	}

	// ------------------------------------------------------------------
	// the wheel/drag ops (task mattree-jei-panzoom: the JEI canvas face of the table)
	// ------------------------------------------------------------------

	@Test
	public void wheelZoomAnchorsAtThePointerAndDragsPan() {
		double tPx = 60.0, tPy = 90.0; // a pointer anchor off-centre — the wheel's whole point

		// one notch in: the exact WHEEL_STEP, the tree point under the pointer stays under it
		MaterialTreeViewport tView = new MaterialTreeViewport();
		MaterialTreeViewport.Point tBefore = tView.unapply(tPx, tPy);
		GT6MaterialTreeNav.wheelZoom(tView, 1.0, tPx, tPy);
		assertEquals(GT6MaterialTreeNav.WHEEL_STEP, tView.scale(), EPSILON);
		MaterialTreeViewport.Point tAfter = tView.unapply(tPx, tPy);
		assertEquals(tBefore.x(), tAfter.x(), EPSILON, "the pointer anchor holds on zoom-in");
		assertEquals(tBefore.y(), tAfter.y(), EPSILON, "the pointer anchor holds on zoom-in");

		// one notch out undoes it exactly (1.25 * 1/1.25); a fractional delta rides the signum
		GT6MaterialTreeNav.wheelZoom(tView, -0.25, tPx, tPy);
		assertEquals(1.0, tView.scale(), EPSILON);
		assertEquals(0.0, tView.offsetX(), EPSILON);
		assertEquals(0.0, tView.offsetY(), EPSILON);

		// zero/NaN deltas are no-ops (a hostile event cannot move the pose)
		GT6MaterialTreeNav.wheelZoom(tView, 0.0, tPx, tPy);
		GT6MaterialTreeNav.wheelZoom(tView, Double.NaN, tPx, tPy);
		assertEquals(1.0, tView.scale(), EPSILON);

		// the ceiling freezes through the wheel too (the 档位幂等, no residue)
		for (int i = 0; i < 40; i++) GT6MaterialTreeNav.wheelZoom(tView, 1.0, tPx, tPy);
		assertEquals(MaterialTreeViewport.MAX_SCALE, tView.scale(), EPSILON);
		double tFrozenX = tView.offsetX(), tFrozenY = tView.offsetY();
		GT6MaterialTreeNav.wheelZoom(tView, 1.0, tPx, tPy);
		assertEquals(tFrozenX, tView.offsetX(), EPSILON);
		assertEquals(tFrozenY, tView.offsetY(), EPSILON);

		// the drag op is the clamped pan: huge ticks pin at the bounds, free ticks travel
		GT6MaterialTreeNav.dragPan(tView, 1e9, 0);
		assertEquals(0.0, tView.offsetX(), EPSILON, "the x pan ceiling is 0 (clamped)");
		GT6MaterialTreeNav.dragPan(tView, 0, -1e9);
		assertEquals(MaterialTreeDisplay.HEIGHT * (1 - tView.scale()), tView.offsetY(), EPSILON,
				"the y pan floor is the canvas basis (206*(1-scale), clamped)");
		// free travel away from the pinned corners: x opens leftward, y opens upward
		GT6MaterialTreeNav.dragPan(tView, -1.0, 1.0);
		assertEquals(-1.0, tView.offsetX(), EPSILON);
		assertEquals(MaterialTreeDisplay.HEIGHT * (1 - tView.scale()) + 1.0, tView.offsetY(), EPSILON);
	}

	@Test
	public void keyboardMapPinsGlfwCodes() {
		// verified with javap against lwjgl-glfw 3.3.1 (the dev-env jar)
		assertEquals(GT6MaterialTreeNav.Action.PAN_LEFT, GT6MaterialTreeNav.keyToAction(263)); // GLFW_KEY_LEFT
		assertEquals(GT6MaterialTreeNav.Action.PAN_RIGHT, GT6MaterialTreeNav.keyToAction(262)); // GLFW_KEY_RIGHT
		assertEquals(GT6MaterialTreeNav.Action.PAN_UP, GT6MaterialTreeNav.keyToAction(265)); // GLFW_KEY_UP
		assertEquals(GT6MaterialTreeNav.Action.PAN_DOWN, GT6MaterialTreeNav.keyToAction(264)); // GLFW_KEY_DOWN
		assertEquals(GT6MaterialTreeNav.Action.ZOOM_IN, GT6MaterialTreeNav.keyToAction(61)); // GLFW_KEY_EQUAL (+)
		assertEquals(GT6MaterialTreeNav.Action.ZOOM_IN, GT6MaterialTreeNav.keyToAction(334)); // GLFW_KEY_KP_ADD
		assertEquals(GT6MaterialTreeNav.Action.ZOOM_OUT, GT6MaterialTreeNav.keyToAction(45)); // GLFW_KEY_MINUS
		assertEquals(GT6MaterialTreeNav.Action.ZOOM_OUT, GT6MaterialTreeNav.keyToAction(333)); // GLFW_KEY_KP_SUBTRACT
		assertEquals(GT6MaterialTreeNav.Action.RESET, GT6MaterialTreeNav.keyToAction(48)); // GLFW_KEY_0
		assertEquals(GT6MaterialTreeNav.Action.RESET, GT6MaterialTreeNav.keyToAction(82)); // GLFW_KEY_R
		// typing keys and controls stay with EMI (search etc.)
		assertNull(GT6MaterialTreeNav.keyToAction(256)); // ESC
		assertNull(GT6MaterialTreeNav.keyToAction(65)); // A
		assertNull(GT6MaterialTreeNav.keyToAction(257)); // ENTER
	}

	// ------------------------------------------------------------------
	// (the wheel-actions direct pin retired — see the class javadoc 旧钉迁移声明,
	// task mattree-r3-nav-unify: the entries died with their consumer)
	// ------------------------------------------------------------------

	// ------------------------------------------------------------------
	// the pointer seam (task mattree-emi-panzoom: bounds-gated wheel/pan on the canvas)
	// ------------------------------------------------------------------

	@Test
	public void thePointerSeamGatesWheelAndPanToTheCanvas() {
		// a synthetic page: one slot at (10,10) 18px — everything else in the canvas is
		// provably empty, so the gesture coords are deterministic without the layout table
		MaterialTreeViewport tView = new MaterialTreeViewport();
		GT6MaterialTreeTransformSlot tSlot = new GT6MaterialTreeTransformSlot(dev.emi.emi.api.stack.EmiStack.EMPTY, 10, 10, tView);
		GT6MaterialTreeNavWidget tNav = new GT6MaterialTreeNavWidget(tView, 0, 0,
				MaterialTreeDisplay.WIDTH, MaterialTreeDisplay.HEIGHT + GT6MaterialTreeEmiRecipe.CONTROL_STRIP_H,
				MaterialTreeDisplay.WIDTH / 2.0, MaterialTreeDisplay.HEIGHT / 2.0, List.of(tSlot));

		// wheel inside the canvas: consumed, pointer-anchored 1.25x
		//? if forge {
		assertTrue(tNav.mouseScrolled(80, 60, 1), "in-canvas wheel is consumed");
		//?} else {
		/*assertTrue(tNav.mouseScrolled(80, 60, 0, 1), "in-canvas wheel is consumed");
		 *///?}
		assertEquals(1.25, tView.scale(), EPSILON);
		assertEquals(-20.0, tView.offsetX(), EPSILON);
		assertEquals(-15.0, tView.offsetY(), EPSILON);
		// wheel outside (the control strip below the canvas, and off-page negative x):
		// NOT consumed — sidebar scroll / page flip keep their EMI behaviour
		//? if forge {
		assertFalse(tNav.mouseScrolled(GT6MaterialTreeEmiRecipe.SLIDER_X + 4, MaterialTreeDisplay.HEIGHT + 8, 1));
		assertFalse(tNav.mouseScrolled(-3, 60, 1));
		//?} else {
		/*assertFalse(tNav.mouseScrolled(GT6MaterialTreeEmiRecipe.SLIDER_X + 4, MaterialTreeDisplay.HEIGHT + 8, 0, 1));
		assertFalse(tNav.mouseScrolled(-3, 60, 0, 1));
		 *///?}
		assertEquals(1.25, tView.scale(), EPSILON, "a refused wheel moves nothing");
		// a hostile delta is CONSUMED (the canvas is ours) but moves nothing — the one
		// guard in Nav.wheelZoom is every host's wheel semantics (the former seam-local
		// signum branch routed a NaN into a zoom-out; mattree-r3-nav-unify)
		tView.reset();
		//? if forge {
		assertTrue(tNav.mouseScrolled(80, 60, Double.NaN), "in-canvas scroll stays consumed");
		//?} else {
		/*assertTrue(tNav.mouseScrolled(80, 60, 0, Double.NaN), "in-canvas scroll stays consumed");
		 *///?}
		assertEquals(1.0, tView.scale(), EPSILON, "a hostile delta moves nothing");
		tView.reset();
		tView.zoomAt(19, 19, 2); // the identity pose has ZERO pan slack (the S1 clamp pins it) —
		// zoom 2x about the slot corner first: slot rect lands at (1..37), offsets (-19,-19)

		// a slot press yields: the native U/R / drag-stack faces own it, no gesture opens
		assertFalse(tNav.mouseClicked(30, 30, 0), "the transformed slot rect (1..37) is the native slot's press");
		assertFalse(tNav.mouseDragged(0, 4, 4), "no gesture, no pan");
		assertFalse(tNav.mouseReleased(0), "no gesture, release passes through");

		// an empty-canvas press opens the pan gesture; the content follows the cursor
		assertTrue(tNav.mouseClicked(60, 60, 0), "empty canvas press opens the pan gesture");
		assertTrue(tNav.mouseDragged(0, 12, -7), "the live gesture consumes the drag");
		assertEquals(-7.0, tView.offsetX(), EPSILON, "-19 + 12 (content follows the cursor)");
		assertEquals(-26.0, tView.offsetY(), EPSILON, "-19 - 7");
		assertTrue(tNav.mouseReleased(0), "the live gesture consumes the release");
		assertFalse(tNav.mouseDragged(0, 5, 0), "the gesture is dead after release");
		assertFalse(tNav.mouseReleased(0), "a dead gesture never consumes release");

		// right button never pans; middle does (the standalone-screen precedent's shape)
		assertFalse(tNav.mouseClicked(60, 60, 1));
		assertTrue(tNav.mouseClicked(60, 60, 2), "middle opens the pan gesture too");
		assertTrue(tNav.mouseDragged(2, 3, 3));
		assertTrue(tNav.mouseReleased(2));
		// the strip is control chrome, not canvas: no gesture from there
		assertFalse(tNav.mouseClicked(GT6MaterialTreeEmiRecipe.SLIDER_X + 4, MaterialTreeDisplay.HEIGHT + 8, 0));
	}

	@Test
	public void doubleClickDetectionPinsTheVanillaWindowMadeSpatial() {
		// no previous click (the fresh state) is never a double
		assertFalse(GT6MaterialTreeNav.isDoubleClick(1000, 0, 50, 50, 50, 50));
		// inside the 250 ms window and the 4 px radius: a double
		assertTrue(GT6MaterialTreeNav.isDoubleClick(1150, 1000, 50, 50, 50, 50));
		assertTrue(GT6MaterialTreeNav.isDoubleClick(1249, 1000, 54, 50, 54, 50), "the radius corner is inside");
		// the window closes at 250 ms
		assertFalse(GT6MaterialTreeNav.isDoubleClick(1250, 1000, 50, 50, 50, 50), "the vanilla window is exclusive");
		// and the spatial bound holds per axis
		assertFalse(GT6MaterialTreeNav.isDoubleClick(1100, 1000, 55, 50, 50, 50), "5 px off in x is a fresh click");
		assertFalse(GT6MaterialTreeNav.isDoubleClick(1100, 1000, 50, 50, 55, 50), "5 px off in y is a fresh click");
	}

	// ------------------------------------------------------------------
	// the click-track slider (acceptance ①: click position -> zoom level)
	// ------------------------------------------------------------------

	@Test
	public void sliderClickTrackJumpsToTheClickedZoomLevel() {
		double tCx = MaterialTreeDisplay.WIDTH / 2.0, tCy = MaterialTreeDisplay.HEIGHT / 2.0;
		MaterialTreeViewport tView = new MaterialTreeViewport();

		GT6MaterialTreeNav.zoomToFraction(tView, 0.0, tCx, tCy);
		assertEquals(MaterialTreeViewport.MIN_SCALE, tView.scale(), EPSILON, "the track's left end is the fit pose");
		GT6MaterialTreeNav.zoomToFraction(tView, 1.0, tCx, tCy);
		assertEquals(MaterialTreeViewport.MAX_SCALE, tView.scale(), EPSILON, "the track's right end is the ceiling");
		// the exact midpoint: 1 + 0.5*3 == 2.5
		GT6MaterialTreeNav.zoomToFraction(tView, 0.5, tCx, tCy);
		assertEquals(2.5, tView.scale(), EPSILON);
		assertEquals(101.0 * (1 - 2.5), tView.offsetX(), EPSILON, "the zoom anchors at the canvas centre");
		// out-of-range clicks clamp onto the ends
		GT6MaterialTreeNav.zoomToFraction(tView, -0.75, tCx, tCy);
		assertEquals(MaterialTreeViewport.MIN_SCALE, tView.scale(), EPSILON);
		GT6MaterialTreeNav.zoomToFraction(tView, 1.75, tCx, tCy);
		assertEquals(MaterialTreeViewport.MAX_SCALE, tView.scale(), EPSILON);
		// clicking the handle's own position is a no-op (the 档位幂等 through the slider)
		double tX = tView.offsetX(), tY = tView.offsetY();
		GT6MaterialTreeNav.zoomToFraction(tView, GT6MaterialTreeNav.fractionOfScale(tView.scale()), tCx, tCy);
		assertEquals(MaterialTreeViewport.MAX_SCALE, tView.scale(), EPSILON);
		assertEquals(tX, tView.offsetX(), EPSILON);
		assertEquals(tY, tView.offsetY(), EPSILON);
	}

	@Test
	public void sliderGeometryHelpersPinTheTrackMath() {
		// track-relative click fraction, clamped
		assertEquals(0.0, GT6MaterialTreeNav.trackFraction(56, 56, 138), EPSILON);
		assertEquals(1.0, GT6MaterialTreeNav.trackFraction(194, 56, 138), EPSILON);
		assertEquals(0.5, GT6MaterialTreeNav.trackFraction(125, 56, 138), EPSILON);
		assertEquals(0.0, GT6MaterialTreeNav.trackFraction(0, 56, 138), EPSILON, "left overshoot clamps");
		assertEquals(1.0, GT6MaterialTreeNav.trackFraction(999, 56, 138), EPSILON, "right overshoot clamps");
		assertEquals(0.0, GT6MaterialTreeNav.trackFraction(10, 56, 0), EPSILON, "degenerate track stays safe");
		// the handle position face (render reads it)
		assertEquals(0.0, GT6MaterialTreeNav.fractionOfScale(1.0), EPSILON);
		assertEquals(0.5, GT6MaterialTreeNav.fractionOfScale(2.5), EPSILON);
		assertEquals(1.0, GT6MaterialTreeNav.fractionOfScale(4.0), EPSILON);
		assertEquals(0.0, GT6MaterialTreeNav.fractionOfScale(0.5), EPSILON, "past-MIN renders as the left end");
		assertEquals(1.0, GT6MaterialTreeNav.fractionOfScale(9.0), EPSILON, "past-MAX renders as the right end");
	}

	// ------------------------------------------------------------------
	// apply/unapply closure (acceptance ①: the S1 invariant survives the nav ops)
	// ------------------------------------------------------------------

	@Test
	public void applyUnapplyClosureSurvivesNavOpsAndPageReopens() {
		double[][] tPoints = {{0, 0}, {MaterialTreeDisplay.WIDTH, MaterialTreeDisplay.HEIGHT}, {33.5, 77.25}};
		MaterialTreeViewport tView = new MaterialTreeViewport();
		GT6MaterialTreeNav.Action[] tOps = {
				GT6MaterialTreeNav.Action.ZOOM_IN, GT6MaterialTreeNav.Action.PAN_LEFT,
				GT6MaterialTreeNav.Action.PAN_UP, GT6MaterialTreeNav.Action.ZOOM_IN,
				GT6MaterialTreeNav.Action.PAN_RIGHT, GT6MaterialTreeNav.Action.ZOOM_OUT};
		for (GT6MaterialTreeNav.Action tOp : tOps) {
			GT6MaterialTreeNav.handle(tView, tOp, 101, 103);
			for (double[] tPoint : tPoints) {
				MaterialTreeViewport.Point tScreen = tView.apply(tPoint[0], tPoint[1]);
				MaterialTreeViewport.Point tTree = tView.unapply(tScreen.x(), tScreen.y());
				assertEquals(tPoint[0], tTree.x(), EPSILON, "x roundtrip after " + tOp);
				assertEquals(tPoint[1], tTree.y(), EPSILON, "y roundtrip after " + tOp);
			}
		}
		// a reopened page is a FRESH identity — the per-addWidgets lifecycle leaves nothing static
		assertTrue(tView.scale() > 1.0, "the driven page actually moved");
		MaterialTreeViewport tReopened = new MaterialTreeViewport();
		assertEquals(1.0, tReopened.scale(), EPSILON);
		assertEquals(0.0, tReopened.offsetX(), EPSILON);
		assertEquals(0.0, tReopened.offsetY(), EPSILON);
	}

	// ------------------------------------------------------------------
	// the TransformSlot seam (the getBounds override math)
	// ------------------------------------------------------------------

	@Test
	public void transformSlotBoundsFollowTheViewport() {
		// identity: the layout coordinates verbatim
		Bounds tIdentity = GT6MaterialTreeTransformSlot.transformedBounds(new MaterialTreeViewport(), 4, 16, 18);
		assertEquals(new Bounds(4, 16, 18, 18), tIdentity);
		// a 2x zoom anchored at the canvas origin doubles position and box exactly
		MaterialTreeViewport tZoom = new MaterialTreeViewport();
		tZoom.zoomAt(0, 0, 2);
		assertEquals(new Bounds(8, 32, 36, 36), GT6MaterialTreeTransformSlot.transformedBounds(tZoom, 4, 16, 18));
		// the centre-anchored pose the buttons produce: apply(4,16) with offset (-101,-103)
		MaterialTreeViewport tCentre = new MaterialTreeViewport();
		tCentre.zoomAt(MaterialTreeDisplay.WIDTH / 2.0, MaterialTreeDisplay.HEIGHT / 2.0, 2);
		assertEquals(new Bounds(-93, -71, 36, 36), GT6MaterialTreeTransformSlot.transformedBounds(tCentre, 4, 16, 18));
		// the S1 invariant the seam rides on: the slot's screen anchor unapplies back to the tree point
		MaterialTreeViewport.Point tBack = tCentre.unapply(-93, -71);
		assertEquals(4.0, tBack.x(), EPSILON);
		assertEquals(16.0, tBack.y(), EPSILON);
	}

	// ------------------------------------------------------------------
	// acceptance ②: the apiSurface guard — EMI api only, byte-level
	// ------------------------------------------------------------------

	@Test
	public void newNavClassesReferenceOnlyTheEmiApiSurface() throws Exception {
		// the recipe view widgets live in dev.emi.emi.api.widget (Widget.java/SlotWidget.java/
		// ButtonWidget.java of the 1.1.24 source); the pin fails the class the day it reaches
		// for an EMI internal (EmiRenderHelper, EmiDrawContext, RecipeScreen, ...)
		for (Class<?> tClass : List.of(GT6MaterialTreeNav.class, GT6MaterialTreeTransformSlot.class,
				GT6MaterialTreeNavWidget.class, GT6MaterialTreeSliderWidget.class)) {
			String tBytes;
			try (var tIn = tClass.getResourceAsStream(tClass.getSimpleName() + ".class")) {
				assertNotNull(tIn, tClass.getSimpleName() + " class bytes");
				tBytes = new String(tIn.readAllBytes(), java.nio.charset.StandardCharsets.ISO_8859_1);
			}
			int tAt = 0;
			while ((tAt = tBytes.indexOf("dev/emi/emi/", tAt)) >= 0) {
				assertTrue(tBytes.startsWith("dev/emi/emi/api/", tAt),
						tClass.getSimpleName() + " references a non-api EMI type at byte " + tAt);
				tAt += 1;
			}
		}
		// the pure nav table is viewer-blind: no EMI and no MC in its bytes at all
		String tNav;
		try (var tIn = GT6MaterialTreeNav.class.getResourceAsStream("GT6MaterialTreeNav.class")) {
			tNav = new String(tIn.readAllBytes(), java.nio.charset.StandardCharsets.ISO_8859_1);
		}
		assertFalse(tNav.contains("dev/emi/emi"), "the nav table stays viewer-neutral");
		assertFalse(tNav.contains("net/minecraft"), "the nav table stays MC-free");
	}

	@Test
	public void widgetClassesPinTheEmiInheritanceSeams() throws Exception {
		// TransformSlot: the SlotWidget.getBounds override seam (non-final @Override, 1.1.24)
		String tSlot = bytesOf(GT6MaterialTreeTransformSlot.class);
		assertTrue(tSlot.contains("dev/emi/emi/api/widget/SlotWidget"), "TransformSlot extends SlotWidget");
		assertTrue(tSlot.contains("getBounds"), "TransformSlot overrides getBounds");
		// the canvas key widget and the slider ride the plain Widget hooks (click/key 五件套)
		assertTrue(bytesOf(GT6MaterialTreeNavWidget.class).contains("dev/emi/emi/api/widget/Widget"));
		assertTrue(bytesOf(GT6MaterialTreeSliderWidget.class).contains("dev/emi/emi/api/widget/Widget"));
		assertTrue(bytesOf(GT6MaterialTreeNavWidget.class).contains("keyPressed"));
		assertTrue(bytesOf(GT6MaterialTreeSliderWidget.class).contains("mouseClicked"));
		// the S4 corner entry (task nav-s4-tree-screen): the recipe face references the screen seam
		assertTrue(bytesOf(GT6MaterialTreeEmiRecipe.class).contains("gregtech6/gui/GT6MaterialTreeScreen"),
				"the EMI page's corner entry opens the standalone screen");
	}

	private static String bytesOf(Class<?> aClass) throws Exception {
		try (var tIn = aClass.getResourceAsStream(aClass.getSimpleName() + ".class")) {
			assertNotNull(tIn, aClass.getSimpleName() + " class bytes");
			return new String(tIn.readAllBytes(), java.nio.charset.StandardCharsets.ISO_8859_1);
		}
	}

	/**
	 * The nav-table unification pin (task mattree-r3-nav-unify acceptance ③): wheel and drag
	 * on EVERY host ride the one {@link GT6MaterialTreeNav} table — no host carries a wheel or
	 * pan verb of its own (the byte refs are the invocation seams), and the retired
	 * WHEEL_IN/WHEEL_OUT enum seats stay retired (the single-declaration terminal state).
	 */
	@Test
	public void theNavTableDrivesAllThreeHosts() throws Exception {
		assertTrue(bytesOf(gregtech6.gui.GT6MaterialTreeScreen.class).contains("dragPan"), "the screen's drag rides the table");
		assertTrue(bytesOf(gregtech6.gui.GT6MaterialTreeScreen.class).contains("wheelZoom"), "the screen's wheel rides the table");
		assertTrue(bytesOf(GT6MaterialTreeNavWidget.class).contains("dragPan"), "the EMI canvas drag rides the table");
		assertTrue(bytesOf(GT6MaterialTreeNavWidget.class).contains("wheelZoom"), "the EMI canvas wheel rides the table");
		assertTrue(bytesOf(gregtech6.jei.GT6MaterialTreeJeiCanvasHandler.class).contains("dragPan"), "the JEI canvas drag rides the table");
		assertTrue(bytesOf(gregtech6.jei.GT6MaterialTreeJeiCanvasHandler.class).contains("wheelZoom"), "the JEI canvas wheel rides the table");
		assertFalse(bytesOf(GT6MaterialTreeNav.class).contains("WHEEL_IN"), "the WHEEL_IN enum seat is retired");
		assertFalse(bytesOf(GT6MaterialTreeNav.class).contains("WHEEL_OUT"), "the WHEEL_OUT enum seat is retired");
	}

	// ------------------------------------------------------------------
	// the page wiring (acceptance ①: the REAL widgets drive the viewport)
	// ------------------------------------------------------------------

	@Test
	public void pageWiringLeadsWithWiresAndClosesWithControls() throws Exception {
		GT6MaterialTreeEmiRecipe tRecipe = ironRecipe();
		assertEquals(MaterialTreeDisplay.WIDTH, tRecipe.getDisplayWidth());
		assertEquals(MaterialTreeDisplay.HEIGHT + GT6MaterialTreeEmiRecipe.CONTROL_STRIP_H, tRecipe.getDisplayHeight(),
				"the control strip extends the page below the tree canvas");

		RecordingHolder tHolder = new RecordingHolder();
		tRecipe.addWidgets(tHolder);

		// z contract: the wires/labels drawable leads (renders under every slot)
		assertTrue(tHolder.mAll.get(0) instanceof DrawableWidget, "the wire/label layer is widget #0");
		List<GT6MaterialTreeTransformSlot> tSlots = new ArrayList<>();
		for (Widget tWidget : tHolder.mAll) {
			if (tWidget instanceof SlotWidget tSlot) {
				assertTrue(tWidget instanceof GT6MaterialTreeTransformSlot,
						"every slot pans/zooms: plain SlotWidget leaked into the tree page");
				tSlots.add((GT6MaterialTreeTransformSlot) tWidget);
			}
		}
		assertTrue(tSlots.size() >= 3, "the iron tree has node + machine + byproduct slots");

		// the control strip: exactly three buttons, then the slider, then the key canvas —
		// all AFTER the slots (last added = topmost z)
		List<ButtonWidget> tButtons = new ArrayList<>();
		int tSliderAt = -1, tNavAt = -1, tLastSlotAt = -1;
		for (int i = 0; i < tHolder.mAll.size(); i++) {
			Widget tWidget = tHolder.mAll.get(i);
			if (tWidget instanceof GT6MaterialTreeTransformSlot) tLastSlotAt = i;
			else if (tWidget instanceof ButtonWidget tButton) tButtons.add(tButton);
			else if (tWidget instanceof GT6MaterialTreeSliderWidget tSlider) tSliderAt = i;
			else if (tWidget instanceof GT6MaterialTreeNavWidget tNav) tNavAt = i;
		}
		assertEquals(4, tButtons.size(), "zoom in, zoom out, reset + the S4 corner entry (nav-s4-tree-screen)");
		assertTrue(tSliderAt > tLastSlotAt && tNavAt > tLastSlotAt, "controls ride above the tree layer");
		// the strip geometry: 12px buttons on the HEIGHT+4 row, the 8px track centred under them
		assertEquals(new Bounds(4, MaterialTreeDisplay.HEIGHT + 4, 12, 12), tButtons.get(0).getBounds());
		assertEquals(new Bounds(20, MaterialTreeDisplay.HEIGHT + 4, 12, 12), tButtons.get(1).getBounds());
		assertEquals(new Bounds(36, MaterialTreeDisplay.HEIGHT + 4, 12, 12), tButtons.get(2).getBounds());
		// the S4 corner entry: the canvas's top-right 12px cell, always live
		assertEquals(new Bounds(GT6MaterialTreeScreen.SCREEN_BUTTON_X, GT6MaterialTreeScreen.SCREEN_BUTTON_Y, 12, 12),
				tButtons.get(3).getBounds());
		assertTrue(activeOf(tButtons.get(3)), "the corner entry never sleeps");
		assertEquals(new Bounds(GT6MaterialTreeEmiRecipe.SLIDER_X, GT6MaterialTreeEmiRecipe.SLIDER_Y,
				GT6MaterialTreeEmiRecipe.SLIDER_W, GT6MaterialTreeEmiRecipe.SLIDER_H), tHolder.mSlider.getBounds());
		// the key canvas covers the whole page (the RecipeScreen hover gate is bounds-based)
		Bounds tNavBounds = tHolder.mNav.getBounds();
		assertTrue(tNavBounds.contains(0, 0) && tNavBounds.contains(tRecipe.getDisplayWidth() - 1, tRecipe.getDisplayHeight() - 1),
				"the key canvas spans the page");
	}

	@Test
	public void drivingTheRealWidgetsMovesTheViewportAndResetBringsItHome() throws Exception {
		GT6MaterialTreeEmiRecipe tRecipe = ironRecipe();
		RecordingHolder tHolder = new RecordingHolder();
		tRecipe.addWidgets(tHolder);

		// identity: the first node slot sits at its layout coordinate
		GT6MaterialTreeTransformSlot tFirst = tHolder.mSlots.get(0);
		var tNode0 = tRecipe.mDisplay.nodes().get(0);
		Bounds tHome = tFirst.getBounds();
		assertEquals(new Bounds(MaterialTreeDisplay.nodeX(tNode0), MaterialTreeDisplay.nodeY(tNode0), 18, 18), tHome,
				"the fresh page is the fit pose");
		// the whole page shares ONE live viewport (the first slot carries it for the test)
		MaterialTreeViewport tView = tFirst.mViewport;

		// the zoom-in button's action (ButtonWidget.mouseClicked plays sounds through the
		// client — unreachable offline; the injected ClickAction is the same consumer the
		// real click routes to, RecipeScreen.java:429-436)
		// add order: in, out, reset (the S4 corner entry rides 4th — indexed picks, the loop
		// form would swallow it into tReset's chair)
		ButtonWidget tZoomIn = tHolder.mButtons.get(0), tZoomOut = tHolder.mButtons.get(1), tReset = tHolder.mButtons.get(2);
		click(tZoomIn);
		Bounds tZoomed = tFirst.getBounds();
		assertTrue(tZoomed.width() > tHome.width() && tZoomed.height() > tHome.height(),
				"the zoom-in button scaled the slot box");
		assertEquals(tHome.x() * 2 - 101, tZoomed.x(), "the centre-anchored transform moved the slot (2x, offset -101)");
		assertEquals(tHome.y() * 2 - 103, tZoomed.y(), "the centre-anchored transform moved the slot (2x, offset -103)");
		// the zoom-out button un-does it
		click(tZoomOut);
		assertEquals(tHome, tFirst.getBounds(), "the zoom-out button returns the slot");

		// the slider: a click at the track's midpoint jumps to 2.5x
		tHolder.mSlider.mouseClicked(GT6MaterialTreeEmiRecipe.SLIDER_X + GT6MaterialTreeEmiRecipe.SLIDER_W / 2,
				GT6MaterialTreeEmiRecipe.SLIDER_Y, 0);
		assertEquals(2.5, tView.scale(), EPSILON, "the click-track slider jumped to the midpoint zoom");
		assertEquals(Math.round(tHome.x() * 2.5 - 151.5), tFirst.getBounds().x(), EPSILON,
				"the centre-anchored 2.5x pose: apply(4) = -141.5, the corner round (half-up) lands on -141");

		// the keyboard: UP pans one lane, R resets home
		assertTrue(tHolder.mNav.keyPressed(265, 0, 0), "UP is consumed by the canvas");
		assertEquals(MaterialTreeDisplay.HEIGHT / 2.0 * (1 - 2.5) + GT6MaterialTreeNav.PAN_STEP,
				tView.offsetY(), EPSILON);
		assertTrue(tHolder.mNav.keyPressed(82, 0, 0), "R is consumed by the canvas");
		assertEquals(1.0, tView.scale(), EPSILON);
		assertEquals(tHome, tFirst.getBounds(), "reset brings every slot home");
		assertFalse(tHolder.mNav.keyPressed(65, 0, 0), "an unmapped key falls through");

		// the buttons' active faces: at the fit pose zoom-out sleeps, at the ceiling zoom-in does
		assertTrue(activeOf(tZoomIn), "zoom-in is live at the fit pose");
		assertTrue(activeOf(tReset), "reset is always live");
		GT6MaterialTreeNav.zoomToFraction(tView, 1.0, 101, 103);
		assertFalse(activeOf(tZoomIn), "zoom-in sleeps at the ceiling");
		assertTrue(activeOf(tZoomOut), "zoom-out is live at the ceiling");
		GT6MaterialTreeNav.handle(tView, GT6MaterialTreeNav.Action.RESET, 101, 103);
		assertFalse(activeOf(tZoomOut), "zoom-out sleeps at the fit pose");
	}

	// the ButtonWidget reflection seams (its fields are EMI's own names, stable in the jar)
	private static void click(ButtonWidget aButton) throws Exception {
		((ButtonWidget.ClickAction) inheritedField(aButton.getClass(), "action").get(aButton)).click(0, 0, 0);
	}

	private static boolean activeOf(ButtonWidget aButton) throws Exception {
		return ((java.util.function.BooleanSupplier) inheritedField(aButton.getClass(), "isActive").get(aButton))
				.getAsBoolean();
	}

	// ------------------------------------------------------------------
	// the offline fixture (the MaterialTreeDisplayTest probe form, condensed)
	// ------------------------------------------------------------------

	private static final Map<PrefixMaterial, Item> PREFIX_ITEMS = new HashMap<>();
	private static int sNextProbeId = 0;

	/** The offline universe + the OPEN item registry — the probe items register during the pour. */
	@org.junit.jupiter.api.BeforeAll
	static void openTheUniverseAndRegistry() {
		GTMaterialItems.initMaterials();
		openOfflineItemRegistry();
	}

	private static Item prefixItem(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		if (aPrefix == null || aMaterial == null) return null;
		return PREFIX_ITEMS.computeIfAbsent(new PrefixMaterial(aPrefix, aMaterial), aPair ->
			Registry.register(BuiltInRegistries.ITEM, new net.minecraft.resources.ResourceLocation("gt6", "mtree_nav_probe_" + sNextProbeId++),
				new MaterialPrefixItem(new Item.Properties(), aPair.prefix(), aPair.material())));
	}

	private static final List<Runnable> sSeamRestores = new ArrayList<>();

	@BeforeEach
	void armSeamsAndPourOreChain() {
		GT6RecipeMaps.reset();
		// the resolver seams are package-private (the DisplayTest fixture sits inside
		// gregtech6.recipes; this EMI test does not) — the same capture/restore through reflection
		captureResolver(GT6RecipesOreChain.class, GT6MaterialTreeNavTest::prefixItem);
		captureResolver(GT6RecipesShCL.class, GT6MaterialTreeNavTest::prefixItem);
		captureResolver(GT6RecipesAnvil.class, GT6MaterialTreeNavTest::prefixItem);
		captureResolver(GT6RecipesSifter.class, GT6MaterialTreeNavTest::prefixItem);
		GT6RecipesOreChain.load();
		GT6RecipesShCL.load();
		GT6RecipesAnvil.load();
		GT6RecipesSifter.load();
	}

	/** Captures the loader's package-private sMaterialItemResolver, arms the probe, queues the restore. */
	@SuppressWarnings("unchecked")
	private static void captureResolver(Class<?> aLoader, java.util.function.BiFunction<OreDictPrefix, OreDictMaterial, Item> aProbe) {
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
			tField.set(null, aProbe);
		} catch (ReflectiveOperationException aE) {
			throw new IllegalStateException("resolver seam missing on " + aLoader.getSimpleName(), aE);
		}
	}

	@AfterEach
	void restoreSeamsAndDropGeneration() {
		for (Runnable tRestore : sSeamRestores) tRestore.run();
		sSeamRestores.clear();
		GT6RecipeMaps.reset();
	}

	/** The Iron display wrapped in the EMI recipe face (the DisplayTest form). */
	private static GT6MaterialTreeEmiRecipe ironRecipe() {
		openOfflineItemRegistry();
		MaterialTreeBuilder tTree = MaterialTreeBuilder.build();
		MaterialTreeDisplay tFe = MaterialTreeDisplay.of(tTree, MT.Fe, GT6MaterialTreeNavTest::prefixItem);
		assertNotNull(tFe, "the iron ore walk yields a display");
		return new GT6MaterialTreeEmiRecipe(tFe);
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

	/** The recording double (the GT6RecipeMapEmiCategoryTest form) — add order is the z face. */
	private static final class RecordingHolder implements WidgetHolder {
		final List<Widget> mAll = new ArrayList<>();
		final List<GT6MaterialTreeTransformSlot> mSlots = new ArrayList<>();
		final List<ButtonWidget> mButtons = new ArrayList<>();
		GT6MaterialTreeSliderWidget mSlider;
		GT6MaterialTreeNavWidget mNav;

		@Override
		public int getWidth() {
			return MaterialTreeDisplay.WIDTH;
		}

		@Override
		public int getHeight() {
			return MaterialTreeDisplay.HEIGHT + GT6MaterialTreeEmiRecipe.CONTROL_STRIP_H;
		}

		@Override
		public <T extends Widget> T add(T aWidget) {
			mAll.add(aWidget);
			if (aWidget instanceof GT6MaterialTreeTransformSlot tSlot) mSlots.add(tSlot);
			else if (aWidget instanceof ButtonWidget tButton) mButtons.add(tButton);
			else if (aWidget instanceof GT6MaterialTreeSliderWidget tSlider) mSlider = tSlider;
			else if (aWidget instanceof GT6MaterialTreeNavWidget tNav) mNav = tNav;
			return aWidget;
		}

		@Override
		public dev.emi.emi.api.widget.TooltipWidget addTooltipText(List<net.minecraft.network.chat.Component> aTooltip,
				int aX, int aY, int aWidth, int aHeight) {
			return null; // the text faces are not under test here; no EMI tooltip widget construction offline
		}
	}
}
