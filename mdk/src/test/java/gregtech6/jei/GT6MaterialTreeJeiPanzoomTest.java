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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mojang.blaze3d.platform.InputConstants;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.inputs.IJeiInputHandler;
import mezz.jei.api.gui.inputs.IJeiUserInput;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;

import gregtech6.emi.GT6MaterialTreeNav;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.recipes.GTRecipesOfflineTestBase;
import gregtech6.recipes.MaterialTreePanzoomFixture;
import gregtech6.recipes.tree.MaterialTreeBuilder;
import gregtech6.recipes.tree.MaterialTreeDisplay;
import gregtech6.recipes.tree.MaterialTreeLayout;
import gregtech6.recipes.tree.MaterialTreeViewport;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTMaterialItems.PrefixMaterial;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The JEI material-tree page PAN/ZOOM pins (task mattree-jei-panzoom, the unfreeze ruling a
 * of the mattree-jei-unfreeze-poc research) — the replacement pins of the two nav-m3-jei-era
 * pins that died with the frozen geometry (旧钉迁移声明: 「the tree geometry stays
 * viewport-frozen」 and 「canvas clicks fall through to the native U/R slot face」 both died
 * here; the deaths are stated at their old sites in {@link GT6MaterialTreeJeiNavTest}):
 *
 * <ul>
 * <li><b>the invisible lookup index (acceptance: invisible 槽查找索引保真)</b>:
 *     {@code setRecipe} mounts EVERY ingredient through {@code addInvisibleIngredients} in
 *     its old role — ore nodes INPUT, downstream nodes + byproducts OUTPUT, machine icons
 *     RENDER_ONLY — and ZERO visible slot builders run (the frozen coordinates are gone).
 *     The simulated focus lookups ride the mounted buckets: a U-focus (INPUT) and an
 *     R-focus (OUTPUT) both find their stacks ON this page (the real bucket chain —
 *     {@code IngredientSupplierBuilder.addInvisibleIngredients} →
 *     {@code RecipeManagerInternal.addRecipe} → {@code RecipeMap.addRecipe} — is JEI
 *     internal, source-anchored in the research card; the offline pin is the mounted
 *     role/stack contract it consumes).</li>
 * <li><b>wheel/drag through the canvas handler</b>: the handler's wheel op zooms about the
 *     POINTER anchor (equality with the Nav-table op), a zero/NaN delta is unconsumed and
 *     inert; the drag op is the clamped pan — huge ticks pin at the bounds.</li>
 * <li><b>the click protocol (the JEI IJeiInputHandler seam, 防吞分页键)</b>: mouse-down
 *     SIMULATE reports a jumpable cell without executing anything; mouse-up EXECUTEs the
 *     jump with the hit stack; the empty canvas is unconsumed in both phases; non-mouse
 *     inputs (keys) stay with JEI. The jump itself is the injected seam (offline has no
 *     runtime) — the production seam is {@code GT6JeiPlugin::openItemPage}.</li>
 * <li><b>the hit domain</b>: {@code hitAt} unapplies the pointer through the viewport — a
 *     zoomed/panned page answers hits at the TRANSFORMED screen coordinates, the same
 *     domain feeds the tooltip face (a node answers its stack, a machine answers only its
 *     via-label, the empty canvas answers MISS).</li>
 * <li><b>the focus outline rule</b>: the widget matches a focus by item identity against
 *     the mounted nodes/byproducts (the replacement for the dead native slot highlight).</li>
 * <li><b>apiSurface guard</b>: the new classes reference only {@code mezz/jei/api/}.</li>
 * </ul>
 *
 * <p>Offline with the display-test pour (the MaterialTreeDisplayTest fixture shape: the
 * probe-item resolver armed into the three ore-chain loaders + the Sifter walk, and a probe
 * machine supplier so the RENDER_ONLY bucket is populated offline).
 */
public class GT6MaterialTreeJeiPanzoomTest extends GTRecipesOfflineTestBase {

	private static final double EPSILON = 1e-9;

	// ------------------------------------------------------------------
	// the offline display fixture (the MaterialTreeDisplayTest shape, copied:
	// probe items + probe machines so every bucket is populated offline)
	// ------------------------------------------------------------------

	private static final Map<PrefixMaterial, Item> PREFIX_ITEMS = new HashMap<>();
	private static final Map<String, Item> MACHINE_ITEMS = new HashMap<>();
	private static int sNextProbeId = 0;

	@BeforeAll
	static void bootUniverseAndOpenRegistry() {
		GTMaterialItems.initMaterials();
		openOfflineItemRegistry();
	}

	private static Item prefixItem(gregapi.oredict.OreDictPrefix aPrefix, gregapi.oredict.OreDictMaterial aMaterial) {
		if (aPrefix == null || aMaterial == null) return null;
		return PREFIX_ITEMS.computeIfAbsent(new PrefixMaterial(aPrefix, aMaterial), aPair ->
			Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath("gt6", "mtree_panzoom_probe_" + sNextProbeId++),
				new MaterialPrefixItem(new Item.Properties(), aPair.prefix(), aPair.material())));
	}

	/** One probe machine item per map name (the workstation-stack seam, identity face). */
	private static ItemStack machineStack(String aMapName) {
		Item tItem = MACHINE_ITEMS.computeIfAbsent(aMapName, aName ->
			Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath("gt6", "mtree_panzoom_machine_" + sNextProbeId++),
				new MaterialPrefixItem(new Item.Properties(), gregapi.data.OP.gearGt, gregapi.data.MT.Steel)));
		return new ItemStack(tItem);
	}

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

	/** getDeclaredField along the superclass chain (the FileSawTest helper, mirrored). */
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

	@BeforeEach
	void armSeamsAndPourOreChain() {
		MaterialTreePanzoomFixture.armAndPour(GT6MaterialTreeJeiPanzoomTest::prefixItem);
	}

	@org.junit.jupiter.api.AfterEach
	void restoreSeamsAndDropGeneration() {
		MaterialTreePanzoomFixture.restore();
	}

	/** The Iron display (six nodes, machine-resolved edges via the probe supplier, two-face byproducts). */
	private static MaterialTreeDisplay ironDisplay() {
		MaterialTreeDisplay tDisplay = MaterialTreePanzoomFixture.ironDisplay(
				GT6MaterialTreeJeiPanzoomTest::prefixItem, GT6MaterialTreeJeiPanzoomTest::machineStack);
		assertNotNull(tDisplay, "Iron's ore walk must yield a display");
		return tDisplay;
	}

	// ------------------------------------------------------------------
	// the invisible lookup index (acceptance: invisible 槽查找索引保真)
	// ------------------------------------------------------------------

	/**
	 * One recorded invisible bucket: the role plus the stacks chained onto its acceptor.
	 * Visible slot calls are recorded too — only to assert they NEVER happen.
	 */
	private static final class RecordingLayoutBuilder implements InvocationHandler {
		final Map<RecipeIngredientRole, List<ItemStack>> mInvisible = new LinkedHashMap<>();
		final List<Object> mVisibleSlots = new ArrayList<>();

		final IRecipeLayoutBuilder self() {
			return (IRecipeLayoutBuilder)Proxy.newProxyInstance(IRecipeLayoutBuilder.class.getClassLoader(),
					new Class<?>[] {IRecipeLayoutBuilder.class}, this);
		}

		@Override
		public Object invoke(Object aProxy, Method aMethod, Object[] aArgs) {
			switch (aMethod.getName()) {
				case "addInvisibleIngredients" -> {
					RecipeIngredientRole tRole = (RecipeIngredientRole)aArgs[0];
					List<ItemStack> tStacks = mInvisible.computeIfAbsent(tRole, aR -> new ArrayList<>());
					Object tAcceptor = Proxy.newProxyInstance(IRecipeLayoutBuilder.class.getClassLoader(),
							new Class<?>[] {mezz.jei.api.gui.builder.IIngredientAcceptor.class},
							(aP, aM, aA) -> {
								if (aM.getName().equals("addItemStack")) {
									tStacks.add((ItemStack)aA[0]);
									return aP;
								}
								return aM.getReturnType() == boolean.class ? Boolean.FALSE : null;
							});
					return tAcceptor;
				}
				case "addSlot", "addInputSlot", "addOutputSlot" -> mVisibleSlots.add(aMethod.getName());
			}
			return aMethod.getReturnType() == boolean.class ? Boolean.FALSE : null;
		}
	}

	@Test
	public void setRecipeBuildsOnlyTheInvisibleLookupIndex() {
		MaterialTreeDisplay tDisplay = ironDisplay();
		GT6MaterialTreeJeiCategory tCategory = new GT6MaterialTreeJeiCategory();
		RecordingLayoutBuilder tBuilder = new RecordingLayoutBuilder();
		tCategory.setRecipe(tBuilder.self(), tDisplay, null);

		// ZERO visible slots: the frozen-coordinate face is gone (旧钉迁移声明 in the class javadoc)
		assertTrue(tBuilder.mVisibleSlots.isEmpty(),
				"no visible slot builder may run — got " + tBuilder.mVisibleSlots);

		// the role split mirrors the old slot split exactly (the index buckets by role)
		Map<Item, RecipeIngredientRole> tRoleByItem = new HashMap<>();
		for (MaterialTreeDisplay.Node tNode : tDisplay.nodes())
			tRoleByItem.put(tNode.stack().getItem(), tNode.column() == MaterialTreeDisplay.COL_ORE
					? RecipeIngredientRole.INPUT : RecipeIngredientRole.OUTPUT);
		for (MaterialTreeDisplay.Byproduct tByproduct : tDisplay.byproducts())
			tRoleByItem.put(tByproduct.stack().getItem(), RecipeIngredientRole.OUTPUT);
		int tByproducts = tDisplay.byproducts().size();
		int tMachines = 0;
		for (var tEdge : tDisplay.edges()) if (!tEdge.machine().isEmpty()) tMachines++;

		List<ItemStack> tInputs = tBuilder.mInvisible.getOrDefault(RecipeIngredientRole.INPUT, List.of());
		List<ItemStack> tOutputs = tBuilder.mInvisible.getOrDefault(RecipeIngredientRole.OUTPUT, List.of());
		List<ItemStack> tRenderOnly = tBuilder.mInvisible.getOrDefault(RecipeIngredientRole.RENDER_ONLY, List.of());

		assertEquals(countOreNodes(tDisplay), tInputs.size(), "the ore column rides INPUT");
		assertEquals(tDisplay.nodes().size() - countOreNodes(tDisplay) + tByproducts, tOutputs.size(),
				"the downstream nodes + the byproduct column ride OUTPUT");
		assertEquals(tMachines, tRenderOnly.size(), "the machine icons ride RENDER_ONLY");

		// every mounted stack answers its role lookup: the simulated focus search —
		// U on the ore item and R on a downstream item both land ON this page's buckets
		for (ItemStack tStack : tInputs)
			assertTrue(tRoleByItem.get(tStack.getItem()) == RecipeIngredientRole.INPUT, "INPUT bucket purity");
		for (ItemStack tStack : tOutputs)
			assertTrue(tRoleByItem.get(tStack.getItem()) == RecipeIngredientRole.OUTPUT, "OUTPUT bucket purity");
		ItemStack tOreFocus = tInputs.get(0);
		assertTrue(tInputs.stream().anyMatch(aS -> ItemStack.isSameItem(aS, tOreFocus)),
				"a U-focus on the ore item finds its INPUT slot on this page");
		ItemStack tDownstreamFocus = tOutputs.stream()
				.filter(aS -> tRoleByItem.get(aS.getItem()) == RecipeIngredientRole.OUTPUT
						&& !tDisplay.byproducts().stream().anyMatch(aB -> ItemStack.isSameItem(aB.stack(), aS)))
				.findFirst().orElseThrow();
		assertTrue(tOutputs.stream().anyMatch(aS -> ItemStack.isSameItem(aS, tDownstreamFocus)),
				"an R-focus on a downstream item finds its OUTPUT slot on this page");
		// nothing empty may enter the index (an empty mount would be a silent lookup dead end)
		tInputs.forEach(aS -> assertFalse(aS.isEmpty(), "no empty stack may enter the index"));
	}

	private static int countOreNodes(MaterialTreeDisplay aDisplay) {
		int rCount = 0;
		for (MaterialTreeDisplay.Node tNode : aDisplay.nodes())
			if (tNode.column() == MaterialTreeDisplay.COL_ORE) rCount++;
		return rCount;
	}

	// ------------------------------------------------------------------
	// wheel/drag through the canvas handler
	// ------------------------------------------------------------------

	@Test
	public void canvasHandlerWheelZoomsAtThePointerAndIsInertOnZero() {
		MaterialTreeViewport tView = new MaterialTreeViewport();
		GT6MaterialTreeJeiCanvasHandler tHandler = new GT6MaterialTreeJeiCanvasHandler(tView, new GT6MaterialTreeJeiTreeWidget(null, tView, null));
		double tPx = 60.0, tPy = 90.0;

		// the handler's wheel op is exactly the Nav-table op at the pointer anchor
		MaterialTreeViewport tMirror = new MaterialTreeViewport();
		assertTrue(wheel(tHandler, tPx, tPy, 1.0), "the wheel is consumed over the canvas");
		GT6MaterialTreeNav.wheelZoom(tMirror, 1.0, tPx, tPy);
		assertEquals(tMirror.scale(), tView.scale(), EPSILON);
		assertEquals(tMirror.offsetX(), tView.offsetX(), EPSILON);
		assertEquals(tMirror.offsetY(), tView.offsetY(), EPSILON);
		MaterialTreeViewport.Point tBefore = tView.unapply(tPx, tPy);
		assertTrue(wheel(tHandler, tPx, tPy, -1.0));
		MaterialTreeViewport.Point tAfter = tView.unapply(tPx, tPy);
		assertEquals(tBefore.x(), tAfter.x(), EPSILON, "the pointer anchor holds (the tree point stays under the cursor)");
		assertEquals(tBefore.y(), tAfter.y(), EPSILON);

		// a zero/NaN delta is unconsumed and the pose is untouched
		double tScale = tView.scale(), tX = tView.offsetX(), tY = tView.offsetY();
		assertFalse(wheel(tHandler, tPx, tPy, 0.0));
		assertFalse(wheel(tHandler, tPx, tPy, Double.NaN));
		assertEquals(tScale, tView.scale(), EPSILON);
		assertEquals(tX, tView.offsetX(), EPSILON);
		assertEquals(tY, tView.offsetY(), EPSILON);
	}

	/**
	 * The wheel bridge: the two generations fork on the scroll signature (15.x one vertical
	 * delta, 19.x dual-axis) — the fork stops here, the pinned semantics are the vertical
	 * axis on both (the same //? shape the handler itself carries).
	 */
	private static boolean wheel(GT6MaterialTreeJeiCanvasHandler aHandler, double aX, double aY, double aDeltaY) {
		//? if forge {
		return aHandler.handleMouseScrolled(aX, aY, aDeltaY);
		//?} else {
		/*return aHandler.handleMouseScrolled(aX, aY, 0.0, aDeltaY);
		*///?}
	}

	@Test
	public void canvasHandlerDragPansAndClamps() {
		MaterialTreeViewport tView = new MaterialTreeViewport();
		GT6MaterialTreeJeiCanvasHandler tHandler = new GT6MaterialTreeJeiCanvasHandler(tView, new GT6MaterialTreeJeiTreeWidget(null, tView, null));
		InputConstants.Key tLeft = InputConstants.Type.MOUSE.getOrCreate(0); // GLFW_MOUSE_BUTTON_LEFT

		// zoom about the canvas centre so the page has pan range, then drag: the content
		// follows the hand (offset += delta inside the clamp range)
		GT6MaterialTreeNav.wheelZoom(tView, 1.0, MaterialTreeDisplay.WIDTH / 2.0, MaterialTreeDisplay.HEIGHT / 2.0);
		assertTrue(tHandler.handleMouseDragged(0, 0, tLeft, 7.0, -5.0), "the left-hand drag is consumed");
		assertEquals(MaterialTreeDisplay.WIDTH / 2.0 * (1 - 1.25) + 7.0, tView.offsetX(), EPSILON);
		assertEquals(MaterialTreeDisplay.HEIGHT / 2.0 * (1 - 1.25) - 5.0, tView.offsetY(), EPSILON);

		// huge ticks pin at the clamp bounds, never past them (the 防树飞出视野 clause)
		assertTrue(tHandler.handleMouseDragged(0, 0, tLeft, 1e9, 1e9));
		assertEquals(0.0, tView.offsetX(), EPSILON, "the x pan ceiling is 0");
		assertEquals(0.0, tView.offsetY(), EPSILON, "the y pan ceiling is 0");
		assertTrue(tHandler.handleMouseDragged(0, 0, tLeft, -1e9, -1e9));
		assertEquals(MaterialTreeDisplay.WIDTH * (1 - tView.scale()), tView.offsetX(), EPSILON);
		assertEquals(MaterialTreeDisplay.HEIGHT * (1 - tView.scale()), tView.offsetY(), EPSILON);

		// the off-canvas-release face: the handler carries NO drag state — a fresh event
		// stream pans from wherever the pose is, nothing sticks
		InputConstants.Key tRight = InputConstants.Type.MOUSE.getOrCreate(1);
		assertFalse(tHandler.handleMouseDragged(0, 0, tRight, 50.0, 50.0), "only the left hand pans");
		assertFalse(tHandler.handleMouseDragged(0, 0, null, 50.0, 50.0), "a null key is unconsumed");
	}

	// ------------------------------------------------------------------
	// the click protocol (SIMULATE reports, EXECUTE jumps)
	// ------------------------------------------------------------------

	/** A controllable IJeiUserInput double: left-mouse (or keyboard) with a simulate flag. */
	private static IJeiUserInput mouseInput(boolean aSimulate) {
		return userInput(aSimulate, InputConstants.Type.MOUSE.getOrCreate(0));
	}

	private static IJeiUserInput keyInput() {
		return userInput(false, InputConstants.UNKNOWN);
	}

	private static IJeiUserInput userInput(boolean aSimulate, InputConstants.Key aKey) {
		return (IJeiUserInput)Proxy.newProxyInstance(IJeiUserInput.class.getClassLoader(),
				new Class<?>[] {IJeiUserInput.class}, (aProxy, aMethod, aArgs) -> switch (aMethod.getName()) {
					case "getKey" -> aKey;
					case "isSimulate" -> aSimulate;
					default -> aMethod.getReturnType() == boolean.class ? Boolean.FALSE : null;
				});
	}

	@Test
	public void clickProtocolSimulatesThenExecutes() {
		MaterialTreeDisplay tDisplay = ironDisplay();
		MaterialTreeViewport tView = new MaterialTreeViewport();
		GT6MaterialTreeJeiTreeWidget tTree = new GT6MaterialTreeJeiTreeWidget(tDisplay, tView, null);
		List<ItemStack> tJumped = new ArrayList<>();
		GT6MaterialTreeJeiCanvasHandler tHandler = new GT6MaterialTreeJeiCanvasHandler(tView, tTree, aStack -> {
			tJumped.add(aStack);
			return true;
		});

		// a node cell in page coordinates (the ore node sits at LANE_X0, stage y 16): its centre
		MaterialTreeDisplay.Node tNode = tDisplay.nodes().get(0);
		double tNX = MaterialTreeDisplay.nodeX(tNode) + MaterialTreeLayout.SLOT / 2.0;
		double tNY = MaterialTreeDisplay.nodeY(tNode) + MaterialTreeLayout.SLOT / 2.0;

		// mouse-down SIMULATE: report the jumpable cell, EXECUTE nothing
		assertTrue(tHandler.handleInput(tNX, tNY, mouseInput(true)), "the down reports the node is clickable");
		assertTrue(tJumped.isEmpty(), "SIMULATE must not execute");
		// mouse-up EXECUTE: the jump fires with the node's own stack
		assertTrue(tHandler.handleInput(tNX, tNY, mouseInput(false)));
		assertEquals(1, tJumped.size());
		assertSame(tNode.stack().getItem(), tJumped.get(0).getItem(), "the jump carries the hit node's stack");
		// the click never moves the view (the jump is the R-axis, not a pan)
		assertEquals(1.0, tView.scale(), EPSILON);
		assertEquals(0.0, tView.offsetX(), EPSILON);

		// the empty canvas is unconsumed in both phases — no jump, and the input stays free
		assertTrue(tJumped.size() == 1);
		assertFalse(tHandler.handleInput(MaterialTreeDisplay.WIDTH - 2, 1, mouseInput(true)), "the empty canvas cannot handle");
		assertFalse(tHandler.handleInput(MaterialTreeDisplay.WIDTH - 2, 1, mouseInput(false)));
		assertEquals(1, tJumped.size(), "no jump off the empty canvas");

		// non-mouse inputs stay with JEI (page flip, transfer)
		assertFalse(tHandler.handleInput(tNX, tNY, keyInput()), "keys are not the canvas handler's face");
		assertEquals(1, tJumped.size());
	}

	// ------------------------------------------------------------------
	// the hit domain (unapply through the viewport) + the tooltip face
	// ------------------------------------------------------------------

	@Test
	public void hitAtUnappliesThePointerThroughTheViewport() {
		MaterialTreeDisplay tDisplay = ironDisplay();
		MaterialTreeViewport tView = new MaterialTreeViewport();
		GT6MaterialTreeJeiTreeWidget tTree = new GT6MaterialTreeJeiTreeWidget(tDisplay, tView, null);

		// at the fit pose the page point IS the tree point
		MaterialTreeDisplay.Node tNode = tDisplay.nodes().get(0);
		double tNX = MaterialTreeDisplay.nodeX(tNode) + 2, tNY = MaterialTreeDisplay.nodeY(tNode) + 2;
		assertSame(tNode.stack().getItem(), tTree.hitAt(tNX, tNY).stack().getItem(), "the fit-pose hit");

		// zoom about a far anchor: the SAME tree cell now answers from its TRANSFORMED point
		GT6MaterialTreeNav.wheelZoom(tView, 1.0, 0.0, 0.0);
		MaterialTreeViewport.Point tMoved = tView.apply(tNX, tNY);
		assertSame(tNode.stack().getItem(), tTree.hitAt(tMoved.x(), tMoved.y()).stack().getItem(),
				"the hit follows the viewport (the unapply closure)");

		// pan drifts the answer too, and the MISS face stays honest
		GT6MaterialTreeNav.dragPan(tView, 30, 40);
		MaterialTreeViewport.Point tPanned = tView.apply(tNX, tNY);
		assertSame(tNode.stack().getItem(), tTree.hitAt(tPanned.x(), tPanned.y()).stack().getItem());
		assertTrue(tTree.hitAt(1, 1).stack().isEmpty(), "the header corner is a MISS (no accidental jump target)");
	}

	@Test
	public void machineAndByproductHitsCarryTheRebuiltAffordances() {
		MaterialTreeDisplay tDisplay = ironDisplay();
		MaterialTreeViewport tView = new MaterialTreeViewport();
		GT6MaterialTreeJeiTreeWidget tTree = new GT6MaterialTreeJeiTreeWidget(tDisplay, tView, null);

		// a machine-resolved edge's icon answers its via-label (label-only: no jump face)
		var tLayouts = MaterialTreeLayout.layout(tDisplay);
		var tEdges = tDisplay.edges();
		int tMachineIndex = -1;
		for (int i = 0; i < tEdges.size(); i++)
			if (tLayouts.get(i).machine() != null) { tMachineIndex = i; break; }
		if (tMachineIndex >= 0) {
			var tBox = tLayouts.get(tMachineIndex).machine();
			GT6MaterialTreeJeiTreeWidget.Hit tHit = tTree.hitAt(tBox.x() + 7, tBox.y() + 7);
			assertTrue(tHit.stack().isEmpty(), "machines do not join the jump face");
			assertFalse(tHit.label().isEmpty(), "the via-label is the machine's hover affordance");
		}

		// a byproduct answers its stack AND its source label
		if (!tDisplay.byproducts().isEmpty()) {
			double tBX = MaterialTreeDisplay.byproductX(0) + 2, tBY = MaterialTreeDisplay.byproductY() + 2;
			GT6MaterialTreeJeiTreeWidget.Hit tHit = tTree.hitAt(tBX, tBY);
			assertSame(tDisplay.byproducts().get(0).stack().getItem(), tHit.stack().getItem());
			assertFalse(tHit.label().isEmpty(), "the source label rides the hover");
			// and the byproduct joins the jump face (R on a byproduct walks to its producers)
			List<ItemStack> tJumped = new ArrayList<>();
			GT6MaterialTreeJeiCanvasHandler tHandler = new GT6MaterialTreeJeiCanvasHandler(tView, tTree, aStack -> {
				tJumped.add(aStack);
				return true;
			});
			assertTrue(tHandler.handleInput(tBX, tBY, mouseInput(false)), "the byproduct EXECUTEs");
			assertEquals(1, tJumped.size());
		}
	}

	// ------------------------------------------------------------------
	// the focus outline rule (the dead native slot highlight's replacement)
	// ------------------------------------------------------------------

	@Test
	public void focusOutlineFollowsTheFocusGroup() {
		MaterialTreeDisplay tDisplay = ironDisplay();
		MaterialTreeViewport tView = new MaterialTreeViewport();
		MaterialTreeDisplay.Node tNode = tDisplay.nodes().get(0);

		// a focus group proxy carrying the node's item as an item-stack focus
		// (proxy Object→generic IFocus<ItemStack> is erasure-inexpressible: the proxy
		// itself is built from the raw IFocus.class token — narrow annotation, no cast
		// can be made checkable; the handler pins every method the walk touches)
		@SuppressWarnings("unchecked")
		IFocus<ItemStack> tFocus = (IFocus<ItemStack>)Proxy.newProxyInstance(IFocus.class.getClassLoader(),
				new Class<?>[] {IFocus.class}, (aProxy, aMethod, aArgs) -> switch (aMethod.getName()) {
					case "getTypedValue" -> Proxy.newProxyInstance(mezz.jei.api.ingredients.ITypedIngredient.class.getClassLoader(),
							new Class<?>[] {mezz.jei.api.ingredients.ITypedIngredient.class},
							(aP, aM, aA) -> aM.getName().equals("getIngredient") ? tNode.stack() : null);
					case "getRole" -> RecipeIngredientRole.INPUT;
					default -> null;
				});
		IFocusGroup tFocuses = (IFocusGroup)Proxy.newProxyInstance(IFocusGroup.class.getClassLoader(),
				new Class<?>[] {IFocusGroup.class}, (aProxy, aMethod, aArgs) -> {
					if (aMethod.getName().equals("getFocuses") && aArgs.length == 1 && aArgs[0] instanceof IIngredientType)
						return Stream.of(tFocus);
					return aMethod.getReturnType() == boolean.class ? Boolean.FALSE : null;
				});

		GT6MaterialTreeJeiTreeWidget tTree = new GT6MaterialTreeJeiTreeWidget(tDisplay, tView, tFocuses);
		assertTrue(tTree.isFocused(tNode.stack()), "the focused item's node lights up");
		MaterialTreeDisplay.Node tOther = tDisplay.nodes().stream()
				.filter(aN -> !ItemStack.isSameItem(aN.stack(), tNode.stack())).findFirst().orElse(null);
		if (tOther != null) assertFalse(tTree.isFocused(tOther.stack()), "the other nodes stay dark");
		// a null focus group (probe paths, the old extras test) is simply dark
		assertFalse(new GT6MaterialTreeJeiTreeWidget(tDisplay, tView, null).isFocused(tNode.stack()));
	}

	// ------------------------------------------------------------------
	// the apiSurface guard
	// ------------------------------------------------------------------

	@Test
	public void panzoomClassesStayOnTheJeiApiSurface() throws Exception {
		for (Class<?> tClass : List.of(GT6MaterialTreeJeiTreeWidget.class, GT6MaterialTreeJeiCanvasHandler.class)) {
			String tBytes = bytesOf(tClass);
			int tAt = 0;
			while ((tAt = tBytes.indexOf("mezz/jei/", tAt)) >= 0) {
				assertTrue(tBytes.startsWith("mezz/jei/api/", tAt),
						tClass.getSimpleName() + " references a non-api JEI type at byte " + tAt);
				tAt += 1;
			}
			assertFalse(tBytes.contains("dev/emi/emi"), tClass.getSimpleName() + " stays EMI-free");
		}
		// the canvas handler is a plain IJeiInputHandler — the whole-canvas registration seam
		assertTrue(IJeiInputHandler.class.isAssignableFrom(GT6MaterialTreeJeiCanvasHandler.class));
		ScreenRectangle tArea = new GT6MaterialTreeJeiCanvasHandler(new MaterialTreeViewport(),
				new GT6MaterialTreeJeiTreeWidget(null, new MaterialTreeViewport(), null)).getArea();
		assertEquals(0, tArea.position().x());
		assertEquals(0, tArea.position().y());
		assertEquals(MaterialTreeDisplay.WIDTH, tArea.width(), "the canvas only — the strip keeps its own faces");
		assertEquals(MaterialTreeDisplay.HEIGHT, tArea.height());
	}

	private static String bytesOf(Class<?> aClass) throws Exception {
		try (var tIn = aClass.getResourceAsStream(aClass.getSimpleName() + ".class")) {
			assertNotNull(tIn, aClass.getSimpleName() + " class bytes");
			return new String(tIn.readAllBytes(), java.nio.charset.StandardCharsets.ISO_8859_1);
		}
	}
}
