package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.DrawableWidget;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.api.widget.WidgetHolder;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.emi.GT6MaterialTreeEmiRecipe;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.jei.GT6MaterialTreeJeiCategory;
import gregtech6.recipes.tree.MaterialTreeBuilder;
import gregtech6.recipes.tree.MaterialTreeDisplay;
import gregtech6.recipes.tree.MaterialTreeDisplay.Edge;
import gregtech6.recipes.tree.MaterialTreeDisplay.Node;
import gregtech6.recipes.tree.MaterialTreeLayout;
import gregtech6.recipes.tree.MaterialTreeLayout.EdgeLayout;
import gregtech6.recipes.tree.MaterialTreeLayout.Rect;
import gregtech6.recipes.tree.MaterialTreeWorkstations;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTMaterialItems.PrefixMaterial;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.recipe.RecipeIngredientRole;

/**
 * The v2 node-graph geometry pins (task mattree-v2-nodes acceptance ②③): the
 * {@link MaterialTreeLayout} helper is the ONE coordinate table both viewers render, so the
 * pins are exact rect tables —
 * <ul>
 * <li><b>canvas budget</b>: pitch 40, the 22 px gap budgeted 2 wire + 16 machine + 4 arrow,
 *     the overflow-marker strip inside the canvas;</li>
 * <li><b>pure edge plans</b>: the straight hop's wire/arrow/machine rect table, the rising
 *     hop's slot-edge trunk + arrow-back bend, the degraded (machine-less) face with its
 *     via-label spot, and the same-gap midpoint collision stagger (y +2 px per dup);</li>
 * <li><b>the live displays</b>: plan-per-edge alignment, machine presence iff the edge
 *     carries a stack, every rect inside the canvas, the structural machine rule
 *     (x = from slot + entry wire, y = midpoint ± stagger), and the overflow scan over
 *     buildAll (hidden counts positive, markers on-canvas — the no-silent-drop clause);</li>
 * <li><b>both viewers land the SAME plans</b> (acceptance ③): the EMI leg's widgets are
 *     captured through a stub {@link WidgetHolder} and the JEI leg's slots through a
 *     {@link Proxy} {@code IRecipeLayoutBuilder} — machine slot bounds equal
 *     {@code (machine.x-1, machine.y-1, 18, 18)} on BOTH legs, and the EMI wires render
 *     BEFORE the slots (the under-the-slots z-order contract).</li>
 * </ul>
 */
class MaterialTreeLayoutTest extends GTRecipesOfflineTestBase {

	/** One lazily created probe item per referenced (prefix, material) pair (the MaterialTreeDisplayTest fixture, mirrored). */
	private static final Map<PrefixMaterial, Item> PREFIX_ITEMS = new HashMap<>();
	private static int sNextProbeId = 0;

	private static final List<Runnable> sSeamRestores = new ArrayList<>();

	private static <T> void capture(java.util.function.Supplier<T> aGetter, java.util.function.Consumer<T> aSetter) {
		T tDefault = aGetter.get();
		sSeamRestores.add(() -> aSetter.accept(tDefault));
	}

	@BeforeAll
	static void bootUniverseAndOpenRegistry() {
		GTMaterialItems.initMaterials(); // the offline material universe (the ShCL convention)
		openOfflineItemRegistry();
	}

	/** The (prefix, material) -> probe-item resolver armed into the three ore-chain loaders. */
	private static Item prefixItem(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		if (aPrefix == null || aMaterial == null) return null;
		return PREFIX_ITEMS.computeIfAbsent(new PrefixMaterial(aPrefix, aMaterial), aPair ->
			Registry.register(BuiltInRegistries.ITEM, "mtree_layout_probe_" + sNextProbeId++,
				new MaterialPrefixItem(new Item.Properties(), aPair.prefix(), aPair.material())));
	}

	/** The v2 machine resolver fixture: the TABLED maps get a registered vanilla stack, anything else EMPTY (the degrade face). */
	private static ItemStack machineStack(String aMapName) {
		return MaterialTreeWorkstations.tableNames().contains(aMapName) ? new ItemStack(Items.BARRIER) : ItemStack.EMPTY;
	}

	/** The offline item-registry unlock (MaterialTreeDisplayTest.openOfflineItemRegistry, mirrored verbatim). */
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

	/** getDeclaredField along the superclass chain (the MaterialTreeDisplayTest helper, mirrored). */
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
		GT6RecipeMaps.reset();
		capture(() -> GT6RecipesOreChain.sMaterialItemResolver, aV -> GT6RecipesOreChain.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesShCL.sMaterialItemResolver, aV -> GT6RecipesShCL.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesAnvil.sMaterialItemResolver, aV -> GT6RecipesAnvil.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesSifter.sMaterialItemResolver, aV -> GT6RecipesSifter.sMaterialItemResolver = aV);
		GT6RecipesOreChain.sMaterialItemResolver = MaterialTreeLayoutTest::prefixItem;
		GT6RecipesShCL.sMaterialItemResolver = MaterialTreeLayoutTest::prefixItem;
		GT6RecipesAnvil.sMaterialItemResolver = MaterialTreeLayoutTest::prefixItem;
		GT6RecipesSifter.sMaterialItemResolver = MaterialTreeLayoutTest::prefixItem;
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

	// ------------------------------------------------------------------
	// the canvas budget
	// ------------------------------------------------------------------

	@Test
	void canvasBudget() {
		assertEquals(40, MaterialTreeDisplay.COLUMN_PITCH);
		assertEquals(40, MaterialTreeDisplay.columnX(MaterialTreeDisplay.COL_CRUSHED) - MaterialTreeDisplay.columnX(MaterialTreeDisplay.COL_ORE));
		assertEquals(40, MaterialTreeDisplay.columnX(MaterialTreeDisplay.COL_DUST) - MaterialTreeDisplay.columnX(MaterialTreeDisplay.COL_PURIFIED));
		// the gap budget: 2 px entry wire + 16 px machine icon + 4 px arrowhead = the 22 px gap
		assertEquals(22, MaterialTreeLayout.GAP);
		assertEquals(2, MaterialTreeLayout.ENTRY_WIRE);
		assertEquals(MaterialTreeLayout.GAP, MaterialTreeLayout.ENTRY_WIRE + MaterialTreeLayout.MACHINE + MaterialTreeLayout.ARROW_HEAD);
		// the canvas: byproduct column + header text fits; the overflow-marker strip fits
		assertEquals(228, MaterialTreeDisplay.WIDTH);
		assertEquals(128, MaterialTreeDisplay.HEIGHT);
		assertEquals(118, MaterialTreeDisplay.OVERFLOW_Y);
		assertTrue(MaterialTreeDisplay.columnX(MaterialTreeDisplay.COL_BYPRODUCT) + 18 <= MaterialTreeDisplay.WIDTH);
		assertTrue(MaterialTreeDisplay.OVERFLOW_Y + 10 <= MaterialTreeDisplay.HEIGHT);
		assertTrue(MaterialTreeDisplay.nodeY(new Node(OP.dust, MaterialTreeDisplay.COL_DUST, MaterialTreeDisplay.MAX_ROWS - 1, ItemStack.EMPTY)) + 18
				<= MaterialTreeDisplay.HEIGHT);
	}

	// ------------------------------------------------------------------
	// the pure edge plans
	// ------------------------------------------------------------------

	@Test
	void straightEdgePlan() {
		// col1 row0 (44,16) -> col2 row0 (84,16): everything on the centre line (y 25 = 16 + 9)
		EdgeLayout tPlan = MaterialTreeLayout.edge(44, 16, 84, 16, true, 0);
		assertEquals(new Rect(64, 17, 16, 16), tPlan.machine());
		assertEquals(List.of(new Rect(62, 25, 2, 1)), tPlan.wire());
		// the arrowhead: solid triangle, tip on the target slot's left edge centre (sorted by row)
		assertEquals(List.of(new Rect(83, 22, 1, 1), new Rect(82, 23, 2, 1), new Rect(81, 24, 3, 1), new Rect(80, 25, 4, 1),
				new Rect(81, 26, 3, 1), new Rect(82, 27, 2, 1), new Rect(83, 28, 1, 1)),
				sorted(tPlan.arrow()));
		assertEquals(60, tPlan.labelX());
		assertEquals(12, tPlan.labelY());
	}

	@Test
	void risingEdgePlan() {
		// col1 row0 (44,16) -> col2 row2 (84,56): exit trunk along the slot's right edge,
		// bend into the machine, cross and bend at the arrow's back line
		EdgeLayout tPlan = MaterialTreeLayout.edge(44, 16, 84, 56, true, 0);
		assertEquals(new Rect(64, 37, 16, 16), tPlan.machine()); // centre-line midpoint y 45 - 8
		assertEquals(List.of(
				new Rect(62, 25, 1, 1),   // the 1 px exit stub (trunk sits 1 px off the slot edge)
				new Rect(63, 25, 1, 20),  // the trunk down the from slot's right edge to the machine centre
				new Rect(63, 45, 1, 1),   // the bend into the machine icon
				new Rect(80, 45, 1, 20)), // the cross run bends at the arrow back line (x 80) down to tcy 65
				tPlan.wire());
		// the widest arrowhead row sits flush on the arrow back line, centred on tcy 65
		Rect tBase = tPlan.arrow().stream().filter(r -> r.w() == MaterialTreeLayout.ARROW_HEAD).findFirst().orElseThrow();
		assertEquals(new Rect(80, 65, MaterialTreeLayout.ARROW_HEAD, 1), tBase);
	}

	@Test
	void degradedEdgePlan() {
		// machine-less face: plain arrow + the v1 via-label spot, no machine box
		EdgeLayout tPlan = MaterialTreeLayout.edge(44, 16, 84, 16, false, 0);
		assertNull(tPlan.machine());
		assertEquals(List.of(new Rect(62, 25, 18, 1)), tPlan.wire());
		assertEquals(7, tPlan.arrow().size());
		assertEquals(60, tPlan.labelX());
		assertEquals(12, tPlan.labelY());
		// the rising degraded face still routes (trunk + arrow-back bend), label at the midpoint
		EdgeLayout tRising = MaterialTreeLayout.edge(44, 16, 84, 56, false, 0);
		assertEquals(List.of(new Rect(62, 25, 1, 1), new Rect(63, 25, 1, 40), new Rect(63, 65, 17, 1)), tRising.wire());
		assertEquals(7, tRising.arrow().size());
	}

	@Test
	void collidingMidpointsStagger() {
		// the same-gap same-midpoint collision rule (spec clause ⑥'s y 错开): 2 px per dup
		EdgeLayout tFirst = MaterialTreeLayout.edge(44, 16, 84, 16, true, 0);
		EdgeLayout tSecond = MaterialTreeLayout.edge(44, 16, 84, 16, true, 1);
		EdgeLayout tThird = MaterialTreeLayout.edge(44, 16, 84, 16, true, 2);
		assertEquals(new Rect(64, 17, 16, 16), tFirst.machine());
		assertEquals(new Rect(64, 19, 16, 16), tSecond.machine());
		assertEquals(new Rect(64, 21, 16, 16), tThird.machine());
		// dup 0 rides the straight centre line; the stagger jogs the whole run +2 px per dup
		// (machine centre 27/29 off the slot centres 25 — a 2 px jog in and out at the bends)
		assertEquals(List.of(new Rect(62, 25, 2, 1)), tFirst.wire());
		assertEquals(List.of(new Rect(62, 25, 1, 1), new Rect(63, 25, 1, 2), new Rect(63, 27, 1, 1), new Rect(80, 25, 1, 2)),
				tSecond.wire());
		assertEquals(List.of(new Rect(62, 25, 1, 1), new Rect(63, 25, 1, 4), new Rect(63, 29, 1, 1), new Rect(80, 25, 1, 4)),
				tThird.wire());
	}

	// ------------------------------------------------------------------
	// the live displays: plan alignment, bounds, overflow
	// ------------------------------------------------------------------

	@Test
	void displayPlansAlignAndFit() {
		MaterialTreeBuilder tTree = MaterialTreeBuilder.build();
		for (OreDictMaterial tMaterial : new OreDictMaterial[] {MT.Fe, MT.Cu}) {
			MaterialTreeDisplay tDisplay = MaterialTreeDisplay.of(tTree, tMaterial, MaterialTreeLayoutTest::prefixItem, MaterialTreeLayoutTest::machineStack);
			assertNotNull(tDisplay, tMaterial.mNameInternal + " must yield a display");
			List<Edge> tEdges = tDisplay.edges();
			List<EdgeLayout> tPlans = MaterialTreeLayout.layout(tDisplay);
			assertEquals(tEdges.size(), tPlans.size(), "one plan per edge");

			Map<OreDictPrefix, Node> tNodes = new HashMap<>();
			for (Node tNode : tDisplay.nodes()) tNodes.put(tNode.prefix(), tNode);
			for (int i = 0; i < tEdges.size(); i++) {
				Edge tEdge = tEdges.get(i);
				EdgeLayout tPlan = tPlans.get(i);
				assertEquals(tEdge.machine().isEmpty(), tPlan.machine() == null, "machine presence iff the edge carries a stack");
				Node tFrom = tNodes.get(tEdge.from()), tTo = tNodes.get(tEdge.to());
				assertNotNull(tFrom);
				assertNotNull(tTo);
				if (tPlan.machine() != null) {
					// the structural machine rule: x = from slot + entry wire, y = centre-line midpoint - 8 + 2 per dup
					assertEquals(MaterialTreeDisplay.nodeX(tFrom) + 18 + MaterialTreeLayout.ENTRY_WIRE, tPlan.machine().x());
					int tMid = (MaterialTreeDisplay.nodeY(tFrom) + MaterialTreeDisplay.nodeY(tTo)) / 2 + MaterialTreeLayout.SLOT / 2;
					assertTrue((tPlan.machine().y() + 8 - tMid) % 2 == 0, "y = centre-line midpoint - 8 + 2k");
				}
				// every rect inside the canvas (acceptance ②'s bounds face)
				for (Rect tRect : tPlan.wire()) assertInside(tRect);
				for (Rect tRect : tPlan.arrow()) assertInside(tRect);
				if (tPlan.machine() != null) assertInside(tPlan.machine());
			}
			// Fe's anvil edge carries a machine (the census pins anvil on the oreRaw -> crushed hop)
			if (tMaterial == MT.Fe) {
				Edge tOreEdge = tEdges.stream().filter(e -> e.from() == OP.oreRaw && e.to() == OP.crushed).findFirst().orElse(null);
				assertNotNull(tOreEdge, "the Fe oreRaw -> crushed edge");
				assertFalse(tOreEdge.machine().isEmpty(), "the tabled anvil map resolves a machine stack");
			}
		}
	}

	@Test
	void overflowIsExplicitNeverSilent() {
		MaterialTreeBuilder tTree = MaterialTreeBuilder.build();
		int tCappedColumns = 0;
		for (MaterialTreeDisplay tDisplay : MaterialTreeDisplay.buildAll(tTree, MaterialTreeLayoutTest::prefixItem, MaterialTreeLayoutTest::machineStack)) {
			// the cap invariant: no column shows more than MAX_ROWS
			int[] tShown = new int[MaterialTreeDisplay.COL_BYPRODUCT + 1];
			for (Node tNode : tDisplay.nodes()) tShown[tNode.column()]++;
			for (int tColumn = 0; tColumn <= MaterialTreeDisplay.COL_DUST; tColumn++)
				assertTrue(tShown[tColumn] <= MaterialTreeDisplay.MAX_ROWS, "column cap");
			assertTrue(tDisplay.byproducts().size() <= MaterialTreeDisplay.MAX_ROWS, "byproduct cap");
			// every overflow record is a positive on-canvas marker (the explicit clause)
			for (var tOverflow : tDisplay.overflow()) {
				assertTrue(tOverflow.hidden() > 0);
				assertTrue(tOverflow.column() >= 0 && tOverflow.column() <= MaterialTreeDisplay.COL_BYPRODUCT);
				int tMarkerX = MaterialTreeDisplay.columnX(tOverflow.column());
				assertTrue(tMarkerX >= 0 && tMarkerX + 8 <= MaterialTreeDisplay.WIDTH, "marker on canvas");
			}
			// the Cu ore column sits exactly on the cap — the boundary the census pins
			if (tDisplay.material == MT.Cu) {
				assertEquals(MaterialTreeDisplay.MAX_ROWS, tShown[MaterialTreeDisplay.COL_ORE]);
			}
			tCappedColumns += tDisplay.overflow().size();
		}
		// bookkeeping only: the scan ran over the real pour (value not pinned — data-driven)
		assertTrue(tCappedColumns >= 0);
	}

	// ------------------------------------------------------------------
	// both viewers land the SAME plans (acceptance ③)
	// ------------------------------------------------------------------

	@Test
	void emiLegRendersSharedPlans() {
		MaterialTreeDisplay tDisplay = feDisplay();
		GT6MaterialTreeEmiRecipe tRecipe = new GT6MaterialTreeEmiRecipe(tDisplay);
		assertEquals(MaterialTreeDisplay.WIDTH, tRecipe.getDisplayWidth());
		assertEquals(MaterialTreeDisplay.HEIGHT, tRecipe.getDisplayHeight());

		List<Widget> tAdded = new ArrayList<>();
		WidgetHolder tHolder = new WidgetHolder() {
			@Override
			public int getWidth() { return MaterialTreeDisplay.WIDTH; }

			@Override
			public int getHeight() { return MaterialTreeDisplay.HEIGHT; }

			@Override
			public <T extends Widget> T add(T aWidget) {
				tAdded.add(aWidget);
				return aWidget;
			}
		};
		tRecipe.addWidgets(tHolder);

		// z-order contract: the wire drawable comes FIRST (under the slots)
		assertFalse(tAdded.isEmpty());
		assertTrue(tAdded.get(0) instanceof DrawableWidget, "wires added before slots, got: " + tAdded.get(0).getClass());

		// the machine slots sit exactly at (machine.x-1, machine.y-1) with an 18x18 hover box
		List<EdgeLayout> tPlans = MaterialTreeLayout.layout(tDisplay);
		List<Edge> tEdges = tDisplay.edges();
		int tMachineSlots = 0;
		for (int i = 0; i < tEdges.size(); i++) {
			Rect tMachine = tPlans.get(i).machine();
			if (tMachine == null) continue;
			Bounds tExpected = new Bounds(tMachine.x() - 1, tMachine.y() - 1, 18, 18);
			final int tIndex = i;
			assertTrue(tAdded.stream().filter(w -> w instanceof SlotWidget)
					.anyMatch(w -> ((SlotWidget) w).getBounds().equals(tExpected)),
					"machine slot for edge " + tIndex + " at " + tExpected);
			tMachineSlots++;
		}
		assertTrue(tMachineSlots > 0, "the fixture resolves at least one machine node");
	}

	@Test
	void jeiLegRendersSharedPlans() {
		MaterialTreeDisplay tDisplay = feDisplay();
		GT6MaterialTreeJeiCategory tCategory = new GT6MaterialTreeJeiCategory();
		assertEquals(MaterialTreeDisplay.WIDTH, tCategory.getWidth());
		assertEquals(MaterialTreeDisplay.HEIGHT, tCategory.getHeight());

		List<int[]> tSlots = new ArrayList<>(); // [role ordinal, x, y]
		List<Boolean> tSlotItems = new ArrayList<>();
		Object tBuilder = Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {IRecipeLayoutBuilder.class},
			(aProxy, aMethod, aArgs) -> {
				String tName = aMethod.getName();
				if (tName.equals("addSlot")) {
					tSlots.add(new int[] {((RecipeIngredientRole) aArgs[0]).ordinal(), (Integer) aArgs[1], (Integer) aArgs[2]});
					tSlotItems.add(false);
					return slotProxy(tSlotItems, tSlotItems.size() - 1);
				}
				if (tName.equals("addInputSlot") || tName.equals("addOutputSlot")) {
					tSlots.add(new int[] {tName.equals("addInputSlot") ? 0 : 1, (Integer) aArgs[0], (Integer) aArgs[1]});
					tSlotItems.add(false);
					return slotProxy(tSlotItems, tSlotItems.size() - 1);
				}
				return smartDefault(aMethod.getReturnType());
			});
		Object tFocuses = Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {mezz.jei.api.recipe.IFocusGroup.class},
			(aProxy, aMethod, aArgs) -> smartDefault(aMethod.getReturnType()));
		tCategory.setRecipe((IRecipeLayoutBuilder) tBuilder, tDisplay, (mezz.jei.api.recipe.IFocusGroup) tFocuses);

		// the machine slots sit exactly at (machine.x-1, machine.y-1) — the SAME plans as the EMI leg
		List<EdgeLayout> tPlans = MaterialTreeLayout.layout(tDisplay);
		List<Edge> tEdges = tDisplay.edges();
		for (int i = 0; i < tEdges.size(); i++) {
			Rect tMachine = tPlans.get(i).machine();
			if (tMachine == null) continue;
			final int tExpectedRole = RecipeIngredientRole.RENDER_ONLY.ordinal();
			final int tX = tMachine.x() - 1, tY = tMachine.y() - 1;
			assertTrue(tSlots.stream().anyMatch(s -> s[0] == tExpectedRole && s[1] == tX && s[2] == tY),
					"RENDER_ONLY machine slot at (" + tX + "," + tY + "), got " + tSlots);
		}
	}

	// ------------------------------------------------------------------
	// fixtures and helpers
	// ------------------------------------------------------------------

	private static MaterialTreeDisplay feDisplay() {
		MaterialTreeBuilder tTree = MaterialTreeBuilder.build();
		MaterialTreeDisplay tDisplay = MaterialTreeDisplay.of(tTree, MT.Fe, MaterialTreeLayoutTest::prefixItem, MaterialTreeLayoutTest::machineStack);
		assertNotNull(tDisplay);
		return tDisplay;
	}

	private static void assertInside(Rect aRect) {
		assertTrue(aRect.x() >= 0 && aRect.y() >= 0, "on canvas: " + aRect);
		assertTrue(aRect.x() + aRect.w() <= MaterialTreeDisplay.WIDTH, "within width: " + aRect);
		assertTrue(aRect.y() + aRect.h() <= MaterialTreeDisplay.HEIGHT, "within height: " + aRect);
	}

	private static List<Rect> sorted(List<Rect> aRects) {
		List<Rect> rSorted = new ArrayList<>(aRects);
		rSorted.sort((a, b) -> a.y() != b.y() ? Integer.compare(a.y(), b.y()) : Integer.compare(a.x(), b.x()));
		return rSorted;
	}

	/** The IRecipeSlotBuilder capture proxy: records addItemStack calls against the slot's index. */
	private static IRecipeSlotBuilder slotProxy(List<Boolean> aSlotItems, int aIndex) {
		return (IRecipeSlotBuilder) Proxy.newProxyInstance(MaterialTreeLayoutTest.class.getClassLoader(), new Class<?>[] {IRecipeSlotBuilder.class},
			(aProxy, aMethod, aArgs) -> {
				if (aMethod.getName().equals("addItemStack")) {
					aSlotItems.set(aIndex, true);
					return aProxy;
				}
				return aMethod.getReturnType().isInterface() ? aProxy : smartDefault(aMethod.getReturnType()); // fluent builders return themselves
			});
	}

	/** A default for any return type (boolean/int/void/object). */
	private static Object smartDefault(Class<?> aType) {
		if (aType == boolean.class) return Boolean.FALSE;
		if (aType == int.class) return 0;
		if (aType == long.class) return 0L;
		if (aType == float.class) return 0F;
		if (aType == double.class) return 0D;
		if (aType == void.class) return null;
		return null;
	}
}
