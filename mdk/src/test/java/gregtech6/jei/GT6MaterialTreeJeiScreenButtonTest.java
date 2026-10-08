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

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import mezz.jei.api.gui.inputs.IJeiGuiEventListener;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.IRecipeWidget;

import gregtech6.gui.GT6MaterialTreeScreen;
import net.minecraft.client.gui.navigation.ScreenRectangle;

/**
 * The JEI leg's S4 entry pins (task nav-s4-tree-screen) — the registration seam and the
 * click face of the corner cell that opens the standalone tree screen:
 *
 * <ul>
 * <li><b>registration seam</b>: the category's {@code createRecipeExtras} mounts the cell
 *     as BOTH an {@link IRecipeWidget} and an {@link IJeiGuiEventListener} — the same
 *     one-instance-two-hats shape the nav-m3-jei review card established (a Proxy double
 *     records the builder calls, both JEI generations, no hand stub);</li>
 * <li><b>the click face</b>: the area is the canvas's top-right 12px cell; ONLY the left
 *     click inside it is consumed and fires the action seam (the screen open); everything
 *     else returns false and falls through to the native routing;</li>
 * <li><b>bytecode</b>: the two JEI hats, the screen seam, JEI-only leg purity, and the
 *     category carrying the extras override.</li>
 * </ul>
 */
public class GT6MaterialTreeJeiScreenButtonTest {

	@Test
	public void registrationSeamMountsTheButtonAsWidgetAndListener() {
		List<String> tCalls = new ArrayList<>();
		List<Object> tArgs = new ArrayList<>();
		IRecipeExtrasBuilder tBuilder = (IRecipeExtrasBuilder) Proxy.newProxyInstance(
				GT6MaterialTreeJeiScreenButtonTest.class.getClassLoader(), new Class[]{IRecipeExtrasBuilder.class},
				(aProxy, aMethod, aMethodArgs) -> {
					tCalls.add(aMethod.getName());
					if (aMethodArgs != null && aMethodArgs.length > 0) tArgs.add(aMethodArgs[0]);
					Class<?> tReturn = aMethod.getReturnType();
					// the fluent faces (addTooltipArea...) hand back an opaque double
					return tReturn.isInterface()
							? Proxy.newProxyInstance(tReturn.getClassLoader(), new Class[]{tReturn}, (p, m, a) -> null)
							: null;
				});
		new GT6MaterialTreeJeiCategory().createRecipeExtras(tBuilder, null, null);
		assertTrue(tCalls.contains("addWidget"), "the cell mounts as a draw widget");
		assertTrue(tCalls.contains("addGuiEventListener"), "the cell mounts as an input listener");
		// the extras union (task mattree-jei-panzoom added the tree-body widget in front):
		// the mount order is tree body -> nav face -> this cell — found by class, not position
		Object tWidget = null, tListener = null;
		for (int i = 0; i < tCalls.size(); i++) {
			if ("addWidget".equals(tCalls.get(i)) && tArgs.get(i) instanceof GT6MaterialTreeJeiScreenButton) {
				tWidget = tArgs.get(i);
				break;
			}
		}
		for (int i = 0; i < tCalls.size(); i++) {
			if ("addGuiEventListener".equals(tCalls.get(i)) && tArgs.get(i) == tWidget) {
				tListener = tArgs.get(i);
				break;
			}
		}
		assertTrue(tWidget instanceof GT6MaterialTreeJeiScreenButton, "the S4 cell class");
		assertSame(tWidget, tListener, "one instance wears both hats");
	}

	@Test
	public void theCellClickOpensTheScreenThroughTheActionSeam() {
		GT6MaterialTreeJeiScreenButton tButton = new GT6MaterialTreeJeiScreenButton(null);
		List<Boolean> tFired = new ArrayList<>();
		tButton.mAction = () -> tFired.add(Boolean.TRUE);
		ScreenRectangle tArea = tButton.getArea();
		assertEquals(GT6MaterialTreeScreen.SCREEN_BUTTON_X, tArea.left(), "the canvas's top-right corner");
		assertEquals(GT6MaterialTreeScreen.SCREEN_BUTTON_Y, tArea.top());
		assertEquals(12, tArea.width());
		assertEquals(12, tArea.height());
		double tCx = GT6MaterialTreeScreen.SCREEN_BUTTON_X + 6, tCy = GT6MaterialTreeScreen.SCREEN_BUTTON_Y + 6;
		assertFalse(tButton.mouseClicked(100, 100, 0), "outside the cell falls through");
		assertFalse(tButton.mouseClicked(tCx, tCy, 1), "a non-left button falls through");
		assertTrue(tButton.mouseClicked(tCx, tCy, 0), "the left click inside the cell is consumed");
		assertEquals(1, tFired.size(), "the action seam fired once");
		assertNotNull(tButton.mAction, "the default action is wired even when the test swaps it");
	}

	@Test
	public void bytecodePinsTheHatsTheSeamAndTheLegPurity() throws Exception {
		String tButton = bytesOf(GT6MaterialTreeJeiScreenButton.class);
		assertTrue(tButton.contains("mezz/jei/api/gui/widgets/IRecipeWidget"), "the draw hat");
		assertTrue(tButton.contains("mezz/jei/api/gui/inputs/IJeiGuiEventListener"), "the input hat");
		assertTrue(tButton.contains("gregtech6/gui/GT6MaterialTreeScreen"), "the screen open seam");
		assertFalse(tButton.contains("dev/emi"), "the JEI-leg class stays JEI-only");
		String tCategory = bytesOf(GT6MaterialTreeJeiCategory.class);
		assertTrue(tCategory.contains("createRecipeExtras"), "the category carries the extras override");
		assertTrue(tCategory.contains("GT6MaterialTreeJeiScreenButton"), "the category mounts the S4 cell");
	}

	private static String bytesOf(Class<?> aClass) throws Exception {
		try (var tIn = aClass.getResourceAsStream(aClass.getSimpleName() + ".class")) {
			assertNotNull(tIn, aClass.getSimpleName() + " class bytes");
			return new String(tIn.readAllBytes(), java.nio.charset.StandardCharsets.ISO_8859_1);
		}
	}
}
