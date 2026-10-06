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

package gregtech6.recipes.tree;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import gregtech6.recipes.tree.MaterialTreeViewport.Point;

/**
 * The viewport math pins (task nav-s1-viewport, reworked by task mattree-viewport-fit /
 * ADR 2026-10-06-mattree-refactor L2: M2 EMI TransformSlot/buttons/keys, M3 JEI canvas
 * listener, S4 standalone screen all consume this ONE class) —
 * <ul>
 * <li><b>zero viewer/MC surface</b>: every type reachable from the class' reflection API
 *     surface is {@code java.*}, a primitive, or the viewport family itself (acceptance ①);
 *     no MC bootstrap needed, the whole suite is plain JUnit;</li>
 * <li><b>the centred fit</b>: a fresh viewport IS the fit pose — scale
 *     {@code min(pane/content)} capped at MAX, both axes centred — and {@code reset()}
 *     returns to it exactly; for the default page (content == pane) the fit is the unit
 *     transform. <b>旧钉迁移声明</b>: the former 「reset = 单位变换、fit 恒 1:1」 pins died
 *     with mattree-viewport-fit (the 全屏仍挤左上角 symptom); the replacement pins are the
 *     dynamic {@code minScale()} floor tests below;</li>
 * <li><b>anchored zoom</b>: the tree point under the focus comes back UNCHANGED through
 *     {@code zoomAt} (the mouse-wheel/button standard), with the formula itself hand-pinned
 *     from the identity pose (a do-nothing zoomAt would pass the invariance pin alone);</li>
 * <li><b>zoom clamp idempotence</b>: past the live floor ({@code minScale()})/{@code MAX_SCALE}
 *     the pose freezes completely (the 缩放档位幂等 research-ledger clause); degenerate
 *     factors (NaN/Infinite/0/negative) are no-ops;</li>
 * <li><b>apply/unapply closure</b>: the inverse transform roundtrips ≥ 3 poses — identity,
 *     a limit-exact corner zoom, and a pan-then-zoom accumulation — over canvas corners and
 *     centres (acceptance ②);</li>
 * <li><b>pan clamp</b>: at the default page's fit (scale 1, content == pane) the offsets pin
 *     to 0 — the tree can never be dragged out of view; zoomed in, the pan range follows the
 *     canvas basis ({@code pane - content*scale .. 0}, the 202x206 基准 through behaviour:
 *     the MAX-scale x floor is {@code 202 - 4*202 == -606});</li>
 * <li><b>fit semantics (mattree-viewport-fit acceptance ①②)</b>: a bigger pane MAGNIFIES
 *     (fit &gt; 1, centred, capped at the ceiling), a smaller pane fits by shrinking — the
 *     former 「never below 1:1」 reading retired with the fit rework (class javadoc);</li>
 * <li><b>resize 保锚</b>: {@code resizeTo} keeps the old pane centre's tree point under the
 *     new pane centre at the kept zoom level, clamped into the new floor range;</li>
 * <li><b>snapshot copy</b>: {@code copy()} freezes a pose the original can no longer touch.</li>
 * </ul>
 */
class MaterialTreeViewportTest {

	private static final double EPSILON = 1e-9;

	// ------------------------------------------------------------------
	// acceptance ①: the pure-math pin — java.* only
	// ------------------------------------------------------------------

	@Test
	void apiSurfaceReferencesOnlyJavaTypes() {
		Class<?> tRoot = MaterialTreeViewport.class;
		assertEquals("gregtech6.recipes.tree", tRoot.getPackage().getName());
		assertTrue(Modifier.isFinal(tRoot.getModifiers()), "a value class stays final");
		Set<Class<?>> tSeen = new HashSet<>();
		Deque<Class<?>> tQueue = new ArrayDeque<>();
		tQueue.add(tRoot);
		while (!tQueue.isEmpty()) {
			Class<?> tClass = tQueue.poll();
			if (!tSeen.add(tClass)) continue;
			for (Field tField : tClass.getDeclaredFields())
				assertJavaOrSelf(tField.getType(), tField + " field type");
			for (Constructor<?> tCtor : tClass.getDeclaredConstructors())
				for (Class<?> tParam : tCtor.getParameterTypes())
					assertJavaOrSelf(tParam, tCtor + " ctor param");
			for (Method tMethod : tClass.getDeclaredMethods()) {
				assertJavaOrSelf(tMethod.getReturnType(), tMethod + " return");
				for (Class<?> tParam : tMethod.getParameterTypes())
					assertJavaOrSelf(tParam, tMethod + " param");
			}
			for (Class<?> tNested : tClass.getDeclaredClasses()) tQueue.add(tNested);
		}
	}

	private static void assertJavaOrSelf(Class<?> aType, String aWhere) {
		while (aType.isArray()) aType = aType.getComponentType();
		if (aType.isPrimitive()) return;
		String tName = aType.getName();
		assertTrue(tName.startsWith("java.") || tName.startsWith("gregtech6.recipes.tree.MaterialTreeViewport"),
				"non-java type in the viewport API surface at " + aWhere + ": " + tName);
	}

	// ------------------------------------------------------------------
	// identity and reset
	// ------------------------------------------------------------------

	@Test
	void freshViewportIsTheUnitTransform() {
		MaterialTreeViewport tViewport = new MaterialTreeViewport();
		assertEquals(1.0, tViewport.scale());
		assertEquals(0.0, tViewport.offsetX());
		assertEquals(0.0, tViewport.offsetY());
		assertEquals(new Point(4, 16), tViewport.apply(4, 16));
		assertEquals(new Point(202, 206), tViewport.unapply(202, 206));
	}

	@Test
	void resetReturnsToTheCentredFitPoseExactly() {
		// the default page's fit IS the unit transform (content == pane); the general
		// reset-is-centred-fit pins live in fitMagnifiesABiggerPaneAndCentresIt below
		MaterialTreeViewport tViewport = new MaterialTreeViewport();
		tViewport.zoomAt(101, 103, 2);
		tViewport.pan(-50, 30);
		assertNotEquals(1.0, tViewport.scale());
		tViewport.reset();
		assertEquals(1.0, tViewport.scale());
		assertEquals(0.0, tViewport.offsetX());
		assertEquals(0.0, tViewport.offsetY());
		assertEquals(new Point(33.5, 77.25), tViewport.apply(33.5, 77.25));
	}

	// ------------------------------------------------------------------
	// anchored zoom
	// ------------------------------------------------------------------

	@Test
	void zoomAtKeepsTheFocusedTreePointStill() {
		// the screen point under the cursor keeps showing the same tree point through the zoom
		MaterialTreeViewport tViewport = new MaterialTreeViewport();
		tViewport.zoomAt(101.5, 103.25, 2);
		Point tBefore = tViewport.unapply(101.5, 103.25);
		tViewport.zoomAt(101.5, 103.25, 1.5);
		Point tAfter = tViewport.unapply(101.5, 103.25);
		assertEquals(tBefore.x(), tAfter.x(), EPSILON);
		assertEquals(tBefore.y(), tAfter.y(), EPSILON);
		// and on a pan-accumulated pose (zoomed in first — at fit the pan clamps back to 0)
		tViewport.reset();
		tViewport.zoomAt(0, 0, 2);
		tViewport.pan(-101, -103);
		tViewport.zoomAt(50.5, 51.5, 2);
		Point tAnchor = tViewport.unapply(50.5, 51.5);
		assertEquals(50.5, tViewport.apply(tAnchor.x(), tAnchor.y()).x(), EPSILON);
		assertEquals(51.5, tViewport.apply(tAnchor.x(), tAnchor.y()).y(), EPSILON);
	}

	@Test
	void zoomAtFormulaHandPinnedFromIdentity() {
		// identity, factor 2, focus (101.5, 103.25): the anchor algebra gives offset = focus*(1-factor)
		MaterialTreeViewport tViewport = new MaterialTreeViewport();
		tViewport.zoomAt(101.5, 103.25, 2);
		assertEquals(2.0, tViewport.scale());
		assertEquals(-101.5, tViewport.offsetX(), EPSILON);
		assertEquals(-103.25, tViewport.offsetY(), EPSILON);
		assertEquals(new Point(101.5, 103.25), tViewport.apply(101.5, 103.25));
	}

	// ------------------------------------------------------------------
	// zoom clamps: limit freezes, degenerate factors are no-ops
	// ------------------------------------------------------------------

	@Test
	void zoomClampsAndFreezesAtTheLimits() {
		assertTrue(MaterialTreeViewport.MIN_SCALE == 1.0,
				"the legacy 1:1 constant survives for the nav slider range (旧钉迁移声明, class javadoc)");
		assertTrue(MaterialTreeViewport.MAX_SCALE >= 2.0, "at least a 2x zoom-in is on offer");
		assertEquals(1.0, new MaterialTreeViewport().minScale(), EPSILON,
				"on the default page the dynamic fit floor IS 1:1");
		MaterialTreeViewport tViewport = new MaterialTreeViewport();
		tViewport.zoomAt(202, 206, 8); // far past MAX: lands ON the limit...
		assertEquals(MaterialTreeViewport.MAX_SCALE, tViewport.scale());
		double tFrozenX = tViewport.offsetX(), tFrozenY = tViewport.offsetY();
		tViewport.zoomAt(202, 206, 2); // ...and further zoom moves NOTHING (档位幂等)
		assertEquals(MaterialTreeViewport.MAX_SCALE, tViewport.scale());
		assertEquals(tFrozenX, tViewport.offsetX());
		assertEquals(tFrozenY, tViewport.offsetY());
		tViewport.zoomAt(202, 206, 0.5); // 4 -> 2: above MIN, so this one legitimately moves
		assertEquals(2.0, tViewport.scale());
		// the MIN floor freezes too: from the fit pose a zoom-out is a full no-op
		MaterialTreeViewport tFloor = new MaterialTreeViewport();
		tFloor.zoomAt(202, 206, 0.5);
		assertEquals(1.0, tFloor.scale());
		assertEquals(0.0, tFloor.offsetX());
		assertEquals(0.0, tFloor.offsetY());
	}

	@Test
	void degenerateZoomFactorsAreNoOps() {
		MaterialTreeViewport tViewport = new MaterialTreeViewport();
		tViewport.zoomAt(50, 60, 2);
		tViewport.pan(-30, -20);
		MaterialTreeViewport tSnapshot = tViewport.copy();
		for (double tBad : new double[] {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0.0, -3.0}) {
			tViewport.zoomAt(10, 10, tBad);
			assertEquals(tSnapshot.scale(), tViewport.scale(), "factor " + tBad + " must not touch the scale");
			assertEquals(tSnapshot.offsetX(), tViewport.offsetX(), "factor " + tBad + " must not touch offsetX");
			assertEquals(tSnapshot.offsetY(), tViewport.offsetY(), "factor " + tBad + " must not touch offsetY");
		}
	}

	// ------------------------------------------------------------------
	// apply/unapply closure (acceptance ②: ≥ 3 poses)
	// ------------------------------------------------------------------

	@Test
	void unapplyInvertsApplyAcrossPoses() {
		double[][] tPoints = {{0, 0}, {202, 206}, {101, 103}, {4, 16}, {33.5, 77.25}};
		// pose 1: identity
		// pose 2: limit-exact corner zoom (anchored at the canvas origin)
		MaterialTreeViewport tCornerZoom = new MaterialTreeViewport();
		tCornerZoom.zoomAt(0, 0, 2.5);
		// pose 3: pan-then-zoom accumulation, then driven into the MAX corner
		MaterialTreeViewport tAccumulated = new MaterialTreeViewport();
		tAccumulated.zoomAt(202, 206, 2);
		tAccumulated.pan(-101, -103);
		tAccumulated.zoomAt(50.5, 51.5, 2);
		tAccumulated.pan(-1000, -1000); // clamps onto the pan floor: the bottom-right view
		MaterialTreeViewport[] tPoses = {new MaterialTreeViewport(), tCornerZoom, tAccumulated};
		for (MaterialTreeViewport tPose : tPoses) {
			for (double[] tPoint : tPoints) {
				Point tScreen = tPose.apply(tPoint[0], tPoint[1]);
				Point tTree = tPose.unapply(tScreen.x(), tScreen.y());
				assertEquals(tPoint[0], tTree.x(), EPSILON, "x roundtrip at scale " + tPose.scale());
				assertEquals(tPoint[1], tTree.y(), EPSILON, "y roundtrip at scale " + tPose.scale());
			}
		}
		// the accumulated pose really sits at the clamped corner: the full tree is still on the pane
		assertEquals(MaterialTreeViewport.MAX_SCALE, tAccumulated.scale());
	}

	// ------------------------------------------------------------------
	// pan: accumulates, clamps so the tree can never be lost
	// ------------------------------------------------------------------

	@Test
	void panAccumulatesThenClamps() {
		MaterialTreeViewport tViewport = new MaterialTreeViewport();
		// at the fit pose (scale 1, content == pane 202x206) the offsets pin to 0 both ways
		tViewport.pan(1000, -1000);
		assertEquals(0.0, tViewport.offsetX());
		assertEquals(0.0, tViewport.offsetY());
		// zoomed in, pans accumulate within [pane - content*scale, 0]
		tViewport.zoomAt(0, 0, 2);
		tViewport.pan(-100, -100);
		assertEquals(-100.0, tViewport.offsetX(), EPSILON);
		assertEquals(-100.0, tViewport.offsetY(), EPSILON);
		tViewport.pan(-1000, 1000); // overshoot BOTH ends in one step
		assertEquals(-202.0, tViewport.offsetX(), EPSILON, "the x pan floor is pane 202 - content 202*2");
		assertEquals(0.0, tViewport.offsetY(), EPSILON, "the y pan ceiling is 0");
		tViewport.pan(50, 50); // partial steps walk back inside the range
		assertEquals(-152.0, tViewport.offsetX(), EPSILON);
		assertEquals(0.0, tViewport.offsetY(), EPSILON);
	}

	@Test
	void panRangeFollowsTheCanvasBasis() {
		// the default viewport binds the material-tree canvas 202x206 as BOTH content and pane;
		// at MAX scale 4 the x pan floor is 202 - 4*202 == -606 and the y floor 206 - 4*206 == -618
		MaterialTreeViewport tViewport = new MaterialTreeViewport();
		tViewport.zoomAt(0, 0, 100);
		assertEquals(MaterialTreeViewport.MAX_SCALE, tViewport.scale());
		tViewport.pan(-10000, -10000);
		assertEquals(202.0 * (1 - MaterialTreeViewport.MAX_SCALE), tViewport.offsetX(), EPSILON);
		assertEquals(206.0 * (1 - MaterialTreeViewport.MAX_SCALE), tViewport.offsetY(), EPSILON);
	}

	// ------------------------------------------------------------------
	// the dynamic fit (mattree-viewport-fit acceptance ①: bigger pane magnifies)
	// ------------------------------------------------------------------

	@Test
	void fitMagnifiesABiggerPaneAndCentresIt() {
		// pane 404x309, content 202x206: fit = min(2, 1.5) = 1.5 — the fit pose magnifies AND centres
		MaterialTreeViewport tViewport = new MaterialTreeViewport(202, 206, 404, 309);
		assertEquals(1.5, tViewport.scale(), EPSILON);
		assertEquals(1.5, tViewport.minScale(), EPSILON, "the fit is the live zoom floor");
		assertEquals(50.5, tViewport.offsetX(), EPSILON, "(404 - 202*1.5) / 2");
		assertEquals(0.0, tViewport.offsetY(), EPSILON, "the limiting axis pins to 0");
		assertEquals(new Point(202.0, 154.5), tViewport.apply(101, 103), "the content centre rides the pane centre");
		// the zoom-out freeze lands on the fit, not on the legacy 1:1
		tViewport.zoomAt(202, 154.5, 0.5);
		assertEquals(1.5, tViewport.scale(), EPSILON);
		// zoom-in walks the 档位 up from the fit...
		tViewport.zoomAt(202, 154.5, 2);
		assertEquals(3.0, tViewport.scale(), EPSILON);
		// ...and reset re-lands on the exact centred fit (the reset-is-fit pin)
		tViewport.reset();
		assertEquals(1.5, tViewport.scale(), EPSILON);
		assertEquals(50.5, tViewport.offsetX(), EPSILON);
		assertEquals(0.0, tViewport.offsetY(), EPSILON);
	}

	@Test
	void fitCapsAtTheZoomCeilingOnAHugePane() {
		// pane 2020x2060: the raw fit is 10x, capped at the 4x ceiling — floor == ceiling freezes the zoom
		MaterialTreeViewport tViewport = new MaterialTreeViewport(202, 206, 2020, 2060);
		assertEquals(MaterialTreeViewport.MAX_SCALE, tViewport.scale(), EPSILON);
		assertEquals(606.0, tViewport.offsetX(), EPSILON, "(2020 - 202*4) / 2");
		assertEquals(618.0, tViewport.offsetY(), EPSILON, "(2060 - 206*4) / 2");
		tViewport.zoomAt(1010, 1030, 2);
		assertEquals(MaterialTreeViewport.MAX_SCALE, tViewport.scale(), EPSILON, "the zoom is frozen at the capped fit");
		assertEquals(606.0, tViewport.offsetX(), EPSILON);
		assertEquals(618.0, tViewport.offsetY(), EPSILON);
	}

	@Test
	void fitShrinksIntoASmallerPane() {
		// pane 101x103: fit = 0.5 — the former 「never below 1:1」 reading retired with the fit
		// rework (旧钉迁移声明: the class javadoc; the replacement pin is fit = min(pane/content))
		MaterialTreeViewport tViewport = new MaterialTreeViewport(202, 206, 101, 103);
		assertEquals(0.5, tViewport.scale(), EPSILON);
		assertEquals(0.5, tViewport.minScale(), EPSILON);
		assertEquals(0.0, tViewport.offsetX(), EPSILON, "both axes shrink exactly into the pane");
		assertEquals(0.0, tViewport.offsetY(), EPSILON);
		// zooming in works normally from the shrunken fit...
		tViewport.zoomAt(50.5, 51.5, 2);
		assertEquals(1.0, tViewport.scale(), EPSILON);
		// ...and zooming back out re-freezes on the fit floor
		tViewport.zoomAt(50.5, 51.5, 0.5);
		assertEquals(0.5, tViewport.scale(), EPSILON);
	}

	// ------------------------------------------------------------------
	// resize 保锚 (mattree-viewport-fit: the anchor survives the window resize)
	// ------------------------------------------------------------------

	@Test
	void resizeToKeepsTheCentredAnchorAndTheZoom() {
		MaterialTreeViewport tViewport = new MaterialTreeViewport(202, 206, 404, 309);
		tViewport.zoomAt(202, 154.5, 2); // scale 3 about the pane centre: offsets (-101, -154.5)
		assertEquals(3.0, tViewport.scale(), EPSILON);
		Point tBefore = tViewport.unapply(202, 154.5); // the old pane centre's tree point
		tViewport.resizeTo(606, 618); // the window grew; fit 3
		assertEquals(3.0, tViewport.scale(), EPSILON, "the zoom level survives the resize");
		assertEquals(0.0, tViewport.offsetX(), EPSILON);
		assertEquals(0.0, tViewport.offsetY(), EPSILON);
		Point tAfter = tViewport.unapply(303, 309); // the NEW pane centre shows the same tree point
		assertEquals(tBefore.x(), tAfter.x(), EPSILON);
		assertEquals(tBefore.y(), tAfter.y(), EPSILON);
	}

	@Test
	void resizeToKeepsTheAnchorAcrossAPannedPose() {
		MaterialTreeViewport tViewport = new MaterialTreeViewport(202, 206, 404, 309);
		tViewport.zoomAt(202, 154.5, 2); // scale 3, offsets (-101, -154.5)
		tViewport.pan(30, 40); // offsets (-71, -114.5), inside [(-202..0), (-309..0)]
		Point tBefore = tViewport.unapply(202, 154.5); // (91, 89.666..)
		tViewport.resizeTo(808, 412); // fit 2, scale kept 3
		assertEquals(3.0, tViewport.scale(), EPSILON);
		Point tAfter = tViewport.unapply(404, 206); // the new pane centre shows the same tree point
		assertEquals(tBefore.x(), tAfter.x(), EPSILON);
		assertEquals(tBefore.y(), tAfter.y(), EPSILON);
	}

	// ------------------------------------------------------------------
	// the snapshot copy
	// ------------------------------------------------------------------

	@Test
	void copyIsAnIndependentSnapshot() {
		MaterialTreeViewport tViewport = new MaterialTreeViewport();
		tViewport.zoomAt(50, 60, 2);
		tViewport.pan(-30, -20);
		MaterialTreeViewport tSnapshot = tViewport.copy();
		tViewport.reset();
		// the snapshot keeps the zoomAt(-50,-60) + pan(-30,-20) pose: offset (-80, -80)
		assertEquals(tSnapshot.scale(), 2.0);
		assertEquals(tSnapshot.offsetX(), -80.0, EPSILON);
		assertEquals(tSnapshot.offsetY(), -80.0, EPSILON);
		// and the snapshot keeps transforming on its own
		tSnapshot.zoomAt(0, 0, 2);
		assertEquals(4.0, tSnapshot.scale());
		assertEquals(1.0, tViewport.scale(), "the original is untouched by the snapshot's ops");
	}
}
