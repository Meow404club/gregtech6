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
import gregtech6.recipes.tree.MaterialTreeLayout.Pose;
import gregtech6.recipes.tree.MaterialTreeLayout.Rect;
import gregtech6.recipes.tree.MaterialTreeViewport;
import gregtech6.recipes.tree.MaterialTreeWorkstations;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTMaterialItems.PrefixMaterial;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.recipe.RecipeIngredientRole;

/**
 * The v2 node-graph geometry pins (task mattree-v2-nodes acceptance ②③; rotated VERTICAL by
 * task mattree-vertical-layout): the {@link MaterialTreeLayout} helper is the ONE coordinate
 * table both viewers render, so the pins are exact rect tables —
 * <ul>
 * <li><b>canvas budget</b>: stages grow DOWN at band pitch 40 (the 22 px gap budgeted
 *     2 wire + 16 machine + 4 arrow), lanes grow RIGHT at pitch 28 (18 px slot + 10 px —
 *     the retired 挤), the per-band overflow markers inside the canvas;</li>
 * <li><b>the vertical stage map</b>: same band ⇒ same y, lanes step x +28; same lane across
 *     bands ⇒ same x, stages step y +40 (the 行/列语义映射 in code);</li>
 * <li><b>pure edge plans</b>: the straight hop's wire/arrow/machine rect table, the diagonal
 *     hop's slot-edge trunk + arrow-back bend, the degraded (machine-less) face with its
 *     via-label spot, and the same-gap midpoint collision stagger (x +2 px per dup);</li>
 * <li><b>the live displays</b>: plan-per-edge alignment, machine presence iff the edge
 *     carries a stack, every rect inside the canvas, the structural machine rule
 *     (y = from band slot + entry wire, x = midpoint ± stagger), and the overflow scan over
 *     buildAll (hidden counts positive, markers on-canvas — the no-silent-drop clause);</li>
 * <li><b>the layout engine</b> (task mattree-r2-layout-engine, ADR L1): the
 *     {@code plan(tree) -> Result} pure function — determinism (same input, same output),
 *     the content-driven canvas (lanes x bands x margins; the fixed 202x206 is its worst
 *     case), the no-overlap invariant over the whole pour, the spacing
 *     {@link gregtech6.recipes.tree.MaterialTreeLayout.Policy} knob (间距改一处全树生效),
 *     the degenerate trees (single node / empty / deep hop) through the list-trio seam,
 *     the 副产挂边钉 (the hanging byproduct band never moves a chain node or a chain edge
 *     plan), the 覆盖链合流钉 (multi-source arrows converge on the ONE shared target), and
 *     the legacy-statics equality that keeps the EMI/JEI page legs on the same geometry;</li>
 * <li><b>both viewers land the SAME plans</b> (acceptance ③): the EMI leg's widgets are
 *     captured through a stub {@link WidgetHolder} and the JEI leg's slots through a
 *     {@link Proxy} {@code IRecipeLayoutBuilder} — machine slot bounds equal
 *     {@code (machine.x-1, machine.y-1, 18, 18)} on BOTH legs, byproduct slots sit at
 *     {@code (byproductX(i), byproductY())} on BOTH legs, and the EMI wires render
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

	/** The (prefix, material) -> probe-item resolver armed into the three ore-chain loaders.
	 * The gt6 namespace is load-bearing: a minecraft-namespaced probe would grow the
	 * frozen-vanilla pool GT6RecipesCokeOvenTest's synthetic universe re-deals on. */
	private static Item prefixItem(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		if (aPrefix == null || aMaterial == null) return null;
		return PREFIX_ITEMS.computeIfAbsent(new PrefixMaterial(aPrefix, aMaterial), aPair ->
			Registry.register(BuiltInRegistries.ITEM, new net.minecraft.resources.ResourceLocation("gt6", "mtree_layout_probe_" + sNextProbeId++),
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
		// stages grow DOWN: band pitch 40, the gap below each band budgeted 2 wire + 16 machine + 4 arrow
		assertEquals(40, MaterialTreeDisplay.STAGE_PITCH);
		assertEquals(40, MaterialTreeDisplay.stageY(MaterialTreeDisplay.COL_CRUSHED) - MaterialTreeDisplay.stageY(MaterialTreeDisplay.COL_ORE));
		assertEquals(40, MaterialTreeDisplay.stageY(MaterialTreeDisplay.COL_DUST) - MaterialTreeDisplay.stageY(MaterialTreeDisplay.COL_PURIFIED));
		assertEquals(22, MaterialTreeLayout.GAP);
		assertEquals(2, MaterialTreeLayout.ENTRY_WIRE);
		assertEquals(MaterialTreeLayout.GAP, MaterialTreeLayout.ENTRY_WIRE + MaterialTreeLayout.MACHINE + MaterialTreeLayout.ARROW_HEAD);
		// lanes grow RIGHT: the ≥28 floor (18 px slot + ≥10 px gap) — the retired 挤 (old ROW_PITCH 20)
		assertTrue(MaterialTreeDisplay.LANE_PITCH >= 28, "the spec floor: 18 px slot + >=10 px gap");
		assertEquals(28, MaterialTreeDisplay.LANE_PITCH);
		// the canvas: 202 wide (7 lanes + margin — under the old 228 width both viewers already shipped), 206 tall
		assertEquals(202, MaterialTreeDisplay.WIDTH);
		assertEquals(206, MaterialTreeDisplay.HEIGHT);
		// every band's slot row and its "+N" marker strip fit the canvas
		for (int c = MaterialTreeDisplay.COL_ORE; c <= MaterialTreeDisplay.COL_BYPRODUCT; c++) {
			assertTrue(MaterialTreeDisplay.stageY(c) + 18 <= MaterialTreeDisplay.HEIGHT, "band " + c + " slot row on canvas");
			assertTrue(MaterialTreeDisplay.overflowY(c) + 9 <= MaterialTreeDisplay.HEIGHT, "band " + c + " overflow marker on canvas");
			assertTrue(MaterialTreeDisplay.overflowX() >= 0 && MaterialTreeDisplay.overflowX() + 8 <= MaterialTreeDisplay.WIDTH);
		}
		// the widest lane fits (7 lanes at pitch 28 from x 4): the last slot's right edge lands exactly on 190
		assertEquals(190, MaterialTreeDisplay.byproductX(MaterialTreeDisplay.MAX_ROWS - 1) + 18);
		assertTrue(MaterialTreeDisplay.byproductX(MaterialTreeDisplay.MAX_ROWS - 1) + 18 <= MaterialTreeDisplay.WIDTH);
		// the byproduct header rides the wire-free gap above its band
		assertEquals(165, MaterialTreeDisplay.BYPRODUCT_HEADER_Y);
		assertTrue(MaterialTreeDisplay.BYPRODUCT_HEADER_Y + 9 <= MaterialTreeDisplay.stageY(MaterialTreeDisplay.COL_BYPRODUCT));
		// the cap re-evaluation: 7 lanes (the raised horizontal budget)
		assertEquals(7, MaterialTreeDisplay.MAX_ROWS);
	}

	@Test
	void stagesGrowDownLanesGrowRight() {
		// the rotated 行/列语义映射: Node.column() = stage band = y; Node.row() = lane = x
		Node tOreLane0 = new Node(OP.oreRaw, MaterialTreeDisplay.COL_ORE, 0, ItemStack.EMPTY);
		Node tOreLane1 = new Node(OP.oreGravel, MaterialTreeDisplay.COL_ORE, 1, ItemStack.EMPTY);
		Node tCrushedLane0 = new Node(OP.crushed, MaterialTreeDisplay.COL_CRUSHED, 0, ItemStack.EMPTY);
		// same band: same y, lanes step +28 to the right
		assertEquals(MaterialTreeDisplay.nodeY(tOreLane0), MaterialTreeDisplay.nodeY(tOreLane1));
		assertEquals(28, MaterialTreeDisplay.nodeX(tOreLane1) - MaterialTreeDisplay.nodeX(tOreLane0));
		// same lane across bands: same x, stages step +40 downward
		assertEquals(MaterialTreeDisplay.nodeX(tOreLane0), MaterialTreeDisplay.nodeX(tCrushedLane0));
		assertEquals(40, MaterialTreeDisplay.nodeY(tCrushedLane0) - MaterialTreeDisplay.nodeY(tOreLane0));
		// the full stage ladder descends
		for (int c = MaterialTreeDisplay.COL_ORE; c < MaterialTreeDisplay.COL_BYPRODUCT; c++)
			assertTrue(MaterialTreeDisplay.stageY(c) < MaterialTreeDisplay.stageY(c + 1), "stage " + c + " above stage " + (c + 1));
		// the byproduct band spreads its slots right, all on the band's y
		assertEquals(MaterialTreeDisplay.stageY(MaterialTreeDisplay.COL_BYPRODUCT), MaterialTreeDisplay.byproductY());
		assertEquals(28, MaterialTreeDisplay.byproductX(1) - MaterialTreeDisplay.byproductX(0));
	}

	// ------------------------------------------------------------------
	// the pure edge plans
	// ------------------------------------------------------------------

	@Test
	void straightEdgePlan() {
		// band0 lane0 (4,16) -> band1 lane0 (4,56): everything on the centre line (x 13 = 4 + 9)
		EdgeLayout tPlan = MaterialTreeLayout.edge(4, 16, 4, 56, true, 0);
		assertEquals(new Rect(5, 36, 16, 16), tPlan.machine());
		assertEquals(List.of(new Rect(13, 34, 1, 2)), tPlan.wire());
		// the arrowhead: the v2 triangle rotated 90° with its geometry (flat base flush on the
		// target slot's top edge y 55, apex 4 px up the wire at (13,52)); widths 4-|dx| per
		// column, sorted top-down (the same rasterised face the diagonal pin's widest column nails)
		assertEquals(List.of(new Rect(13, 52, 1, 4), new Rect(12, 53, 1, 3), new Rect(14, 53, 1, 3),
				new Rect(11, 54, 1, 2), new Rect(15, 54, 1, 2), new Rect(10, 55, 1, 1), new Rect(16, 55, 1, 1)),
				sorted(tPlan.arrow()));
		assertEquals(0, tPlan.labelX());
		assertEquals(32, tPlan.labelY());
	}

	@Test
	void diagonalEdgePlan() {
		// band0 lane0 (4,16) -> band1 lane1 (44,56): exit trunk along the slot's bottom edge,
		// bend into the machine, cross under it and bend at the arrow's back line
		EdgeLayout tPlan = MaterialTreeLayout.edge(4, 16, 44, 56, true, 0);
		assertEquals(new Rect(25, 36, 16, 16), tPlan.machine()); // centre-line midpoint x 33 - 8
		assertEquals(List.of(
				new Rect(13, 34, 1, 1),   // the 1 px exit stub (trunk sits 1 px off the slot edge)
				new Rect(13, 35, 20, 1),  // the trunk along the from slot's bottom edge to the machine centre
				new Rect(33, 35, 1, 1),   // the bend into the machine icon
				new Rect(33, 52, 20, 1)), // the cross run under the machine bends at the arrow back line (y 52) across to tcx 53
				tPlan.wire());
		// the widest arrowhead column sits flush on the arrow back line, centred on tcx 53
		Rect tBase = tPlan.arrow().stream().filter(r -> r.h() == MaterialTreeLayout.ARROW_HEAD).findFirst().orElseThrow();
		assertEquals(new Rect(53, 52, 1, MaterialTreeLayout.ARROW_HEAD), tBase);
	}

	@Test
	void degradedEdgePlan() {
		// machine-less face: plain arrow + the v1 via-label spot, no machine box
		EdgeLayout tPlan = MaterialTreeLayout.edge(4, 16, 4, 56, false, 0);
		assertNull(tPlan.machine());
		assertEquals(List.of(new Rect(13, 34, 1, 18)), tPlan.wire());
		assertEquals(7, tPlan.arrow().size());
		assertEquals(0, tPlan.labelX());
		assertEquals(32, tPlan.labelY());
		// the diagonal degraded face still routes (trunk + arrow-back bend), label at the midpoint
		EdgeLayout tDiagonal = MaterialTreeLayout.edge(4, 16, 44, 56, false, 0);
		assertEquals(List.of(new Rect(13, 34, 1, 1), new Rect(13, 35, 40, 1), new Rect(53, 35, 1, 17)), tDiagonal.wire());
		assertEquals(7, tDiagonal.arrow().size());
	}

	@Test
	void collidingMidpointsStagger() {
		// the same-gap same-midpoint collision rule (spec clause ⑥'s 错开, rotated HORIZONTAL): 2 px per dup
		EdgeLayout tFirst = MaterialTreeLayout.edge(4, 16, 4, 56, true, 0);
		EdgeLayout tSecond = MaterialTreeLayout.edge(4, 16, 4, 56, true, 1);
		EdgeLayout tThird = MaterialTreeLayout.edge(4, 16, 4, 56, true, 2);
		assertEquals(new Rect(5, 36, 16, 16), tFirst.machine());
		assertEquals(new Rect(7, 36, 16, 16), tSecond.machine());
		assertEquals(new Rect(9, 36, 16, 16), tThird.machine());
		// dup 0 rides the straight centre line; the stagger jogs the whole run +2 px per dup
		// (machine centre 15/17 off the slot centres 13 — a 2 px jog in and out at the bends)
		assertEquals(List.of(new Rect(13, 34, 1, 2)), tFirst.wire());
		assertEquals(List.of(new Rect(13, 34, 1, 1), new Rect(13, 35, 2, 1), new Rect(15, 35, 1, 1), new Rect(13, 52, 2, 1)),
				tSecond.wire());
		assertEquals(List.of(new Rect(13, 34, 1, 1), new Rect(13, 35, 4, 1), new Rect(17, 35, 1, 1), new Rect(13, 52, 4, 1)),
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
					// the structural machine rule: y = from band slot + entry wire, x = midpoint lane - 8 + 2 per dup
					assertEquals(MaterialTreeDisplay.nodeY(tFrom) + 18 + MaterialTreeLayout.ENTRY_WIRE, tPlan.machine().y());
					int tMid = (MaterialTreeDisplay.nodeX(tFrom) + MaterialTreeDisplay.nodeX(tTo)) / 2 + MaterialTreeLayout.SLOT / 2;
					assertTrue((tPlan.machine().x() + 8 - tMid) % 2 == 0, "x = centre-line midpoint - 8 + 2k");
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
			// every overflow record is a positive on-canvas marker under its own band (the explicit clause)
			for (var tOverflow : tDisplay.overflow()) {
				assertTrue(tOverflow.hidden() > 0);
				assertTrue(tOverflow.column() >= 0 && tOverflow.column() <= MaterialTreeDisplay.COL_BYPRODUCT);
				int tMarkerX = MaterialTreeDisplay.overflowX();
				int tMarkerY = MaterialTreeDisplay.overflowY(tOverflow.column());
				assertTrue(tMarkerX >= 0 && tMarkerX + 8 <= MaterialTreeDisplay.WIDTH, "marker on canvas");
				assertTrue(tMarkerY >= 0 && tMarkerY + 9 <= MaterialTreeDisplay.HEIGHT, "marker on canvas");
			}
			// the Cu ore band holds exactly its five reachable nodes (oreRaw + the four DUST_ORE
			// sifting inputs) — under the raised cap 7 they all fit, so Cu carries NO overflow
			if (tDisplay.material == MT.Cu) {
				assertEquals(5, tShown[MaterialTreeDisplay.COL_ORE]);
				assertTrue(tDisplay.overflow().isEmpty(), "Cu fits the raised cap: " + tDisplay.overflow());
			}
			if (tDisplay.material == MT.Fe) {
				assertTrue(tDisplay.overflow().isEmpty(), "Fe fits the raised cap: " + tDisplay.overflow());
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
		// nav-m2-emi grew the display by the control strip (the pin lagged the change;
		// review-seat absorb from the nav-m3-jei branch, seat 8)
		assertEquals(MaterialTreeDisplay.HEIGHT + GT6MaterialTreeEmiRecipe.CONTROL_STRIP_H, tRecipe.getDisplayHeight());

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

		// the byproduct slots sit at (byproductX(i), byproductY()) with 18x18 hover boxes — the SAME shared table
		int tByproductSlots = 0;
		for (int b = 0; b < tDisplay.byproducts().size(); b++) {
			Bounds tExpected = new Bounds(MaterialTreeDisplay.byproductX(b), MaterialTreeDisplay.byproductY(), 18, 18);
			assertTrue(tAdded.stream().filter(w -> w instanceof SlotWidget)
					.anyMatch(w -> ((SlotWidget) w).getBounds().equals(tExpected)), "byproduct slot " + b + " at " + tExpected);
			tByproductSlots++;
		}
		assertTrue(tByproductSlots > 0, "Fe carries byproducts");
	}

	@Test
	void jeiLegRendersSharedPlans() {
		MaterialTreeDisplay tDisplay = feDisplay();
		GT6MaterialTreeJeiCategory tCategory = new GT6MaterialTreeJeiCategory();
		assertEquals(MaterialTreeDisplay.WIDTH, tCategory.getWidth());
		// nav-m3-jei: the category carries the EMI twin's 20px control strip below the canvas
		assertEquals(MaterialTreeDisplay.HEIGHT + GT6MaterialTreeEmiRecipe.CONTROL_STRIP_H, tCategory.getHeight());

		// 旧钉迁移声明 (task mattree-jei-panzoom): this pin used to assert the slot
		// COORDINATES the builder received — that face died with the invisible-slot mount
		// (addInvisibleIngredients carries no coordinates). The shared-plan rendering moved
		// to the self-drawn GT6MaterialTreeJeiTreeWidget (whose geometry is the SAME
		// MaterialTreeLayout table these plan pins cover), and the mount contract (roles,
		// stacks, zero visible slots) is pinned in GT6MaterialTreeJeiPanzoomTest. What stays
		// pinned HERE is the category's geometry contract: canvas + strip against the shared
		// constants, and that setRecipe enters through the invisible seam only.
		List<String> tSlotBuilders = new ArrayList<>();
		List<RecipeIngredientRole> tInvisibleRoles = new ArrayList<>();
		Object tBuilder = Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {IRecipeLayoutBuilder.class},
			(aProxy, aMethod, aArgs) -> {
				String tName = aMethod.getName();
				if (tName.equals("addSlot") || tName.equals("addInputSlot") || tName.equals("addOutputSlot")) {
					tSlotBuilders.add(tName);
					return slotProxy(new ArrayList<>(), 0);
				}
				if (tName.equals("addInvisibleIngredients")) {
					tInvisibleRoles.add((RecipeIngredientRole) aArgs[0]);
					return Proxy.newProxyInstance(getClass().getClassLoader(),
						new Class<?>[] {mezz.jei.api.gui.builder.IIngredientAcceptor.class},
						(aP, aM, aA) -> aM.getName().equals("addItemStack") ? aP : smartDefault(aM.getReturnType()));
				}
				return smartDefault(aMethod.getReturnType());
			});
		Object tFocuses = Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {mezz.jei.api.recipe.IFocusGroup.class},
			(aProxy, aMethod, aArgs) -> smartDefault(aMethod.getReturnType()));
		tCategory.setRecipe((IRecipeLayoutBuilder) tBuilder, tDisplay, (mezz.jei.api.recipe.IFocusGroup) tFocuses);

		assertTrue(tSlotBuilders.isEmpty(), "no visible slot builder may run — the frozen-coordinate face is retired");
		// every role the page mounts (ore INPUT / downstream + byproducts OUTPUT / machines
		// RENDER_ONLY) enters through the invisible seam — the index contract's shape check
		assertTrue(tInvisibleRoles.contains(RecipeIngredientRole.INPUT), "the ore column mounts invisible INPUT");
		assertTrue(tInvisibleRoles.contains(RecipeIngredientRole.OUTPUT), "downstream + byproducts mount invisible OUTPUT");
		assertTrue(tInvisibleRoles.contains(RecipeIngredientRole.RENDER_ONLY), "machine icons mount invisible RENDER_ONLY");
	}

	// ------------------------------------------------------------------
	// the unified pose primitive (task mattree-item-zoom-pose, the zoom-2x acceptance)
	// ------------------------------------------------------------------

	private static final double EPSILON = 1e-9;

	@Test
	void posePrimitiveFollowsTheViewport() {
		// the pose is the ONE translate+scale constructor both legs consume: origin = apply, factor = live scale
		MaterialTreeViewport tView = new MaterialTreeViewport();
		Pose tIdentity = MaterialTreeLayout.pose(tView, 4, 16);
		assertEquals(4.0, tIdentity.x(), EPSILON);
		assertEquals(16.0, tIdentity.y(), EPSILON);
		assertEquals(1.0, tIdentity.scale(), EPSILON, "the fit pose's pose factor is 1");
		tView.zoomAt(101, 103, 2); // the canvas-centre anchor → exactly 2x
		Pose tZoomed = MaterialTreeLayout.pose(tView, 33.5, 77.25);
		assertEquals(tView.apply(33.5, 77.25).x(), tZoomed.x(), EPSILON, "the origin is the applied point");
		assertEquals(tView.apply(33.5, 77.25).y(), tZoomed.y(), EPSILON);
		assertEquals(2.0, tZoomed.scale(), EPSILON, "the factor is the live scale, not a frozen constant");
	}

	@Test
	void zoom2xIconCentresInTheScaledSlotBox() {
		// the acceptance pin (zoom 2x 图标居中缩放槽盒无错位): the icon — the +1 inset, 16 px
		// at the pose scale — sits EXACTLY on the centre of the 18x18 box the corner-pair fill
		// draws, at every scale. 旧钉迁移声明: the former translate-only 16 px icon against the
		// 18×scale box (the symptom23 ③ 错位) died with mattree-item-zoom-pose.
		MaterialTreeViewport tView = new MaterialTreeViewport();
		tView.zoomAt(101, 103, 2);
		assertEquals(2.0, tView.scale(), EPSILON);
		double tSlotX = 4, tSlotY = 16;
		Pose tIcon = MaterialTreeLayout.pose(tView, tSlotX + 1, tSlotY + 1); // the screen drawItem inset
		double tIconCentreX = tIcon.x() + MaterialTreeLayout.MACHINE / 2.0 * tIcon.scale();
		double tIconCentreY = tIcon.y() + MaterialTreeLayout.MACHINE / 2.0 * tIcon.scale();
		MaterialTreeViewport.Point tBoxA = tView.apply(tSlotX, tSlotY);
		MaterialTreeViewport.Point tBoxB = tView.apply(tSlotX + MaterialTreeLayout.SLOT, tSlotY + MaterialTreeLayout.SLOT);
		assertEquals((tBoxA.x() + tBoxB.x()) / 2.0, tIconCentreX, EPSILON, "the icon centre is the scaled box centre");
		assertEquals((tBoxA.y() + tBoxB.y()) / 2.0, tIconCentreY, EPSILON, "the icon centre is the scaled box centre");
		// the scaled icon's extent stays inside the drawn box (16*2 < 18*2) — grown WITH the box, not past it
		assertTrue(tIcon.scale() * MaterialTreeLayout.MACHINE < tBoxB.x() - tBoxA.x());
		// the same centring holds at the 4x ceiling (the strip + lands ON the limit)
		tView.zoomAt(101, 103, 2);
		assertEquals(MaterialTreeViewport.MAX_SCALE, tView.scale(), EPSILON);
		Pose tCapped = MaterialTreeLayout.pose(tView, tSlotX + 1, tSlotY + 1);
		MaterialTreeViewport.Point tCappedA = tView.apply(tSlotX, tSlotY);
		MaterialTreeViewport.Point tCappedB = tView.apply(tSlotX + MaterialTreeLayout.SLOT, tSlotY + MaterialTreeLayout.SLOT);
		assertEquals((tCappedA.x() + tCappedB.x()) / 2.0, tCapped.x() + 8 * tCapped.scale(), EPSILON);
		assertEquals((tCappedA.y() + tCappedB.y()) / 2.0, tCapped.y() + 8 * tCapped.scale(), EPSILON);
	}

	// ------------------------------------------------------------------
	// the layout engine (task mattree-r2-layout-engine, ADR L1):
	// determinism, content bounds, invariants, degenerates, the legacy face
	// ------------------------------------------------------------------

	@Test
	void enginePlanIsDeterministic() {
		// same input, same output — the pure-function contract the fit/anchor consumers ride
		MaterialTreeBuilder tTree = MaterialTreeBuilder.build();
		MaterialTreeDisplay tDisplay = MaterialTreeDisplay.of(tTree, MT.Cu, MaterialTreeLayoutTest::prefixItem, MaterialTreeLayoutTest::machineStack);
		assertNotNull(tDisplay);
		MaterialTreeLayout.Result tFirst = MaterialTreeLayout.plan(tDisplay);
		MaterialTreeLayout.Result tSecond = MaterialTreeLayout.plan(tDisplay);
		assertEquals(tFirst.width(), tSecond.width());
		assertEquals(tFirst.height(), tSecond.height());
		assertEquals(tFirst.byproductBand(), tSecond.byproductBand());
		for (int i = 0; i < tFirst.edges().size(); i++) {
			assertEquals(tFirst.edges().get(i), tSecond.edges().get(i), "edge plan " + i + " identical");
		}
		for (MaterialTreeDisplay.Node tNode : tDisplay.nodes()) {
			assertEquals(tFirst.nodeX(tNode), tSecond.nodeX(tNode));
			assertEquals(tFirst.nodeY(tNode), tSecond.nodeY(tNode));
		}
	}

	@Test
	void engineCanvasFollowsContent() {
		MaterialTreeBuilder tTree = MaterialTreeBuilder.build();
		// the iron display: 3 chain lanes (crushed/crushedTiny; dust/dustTiny/dustDiv72) but
		// 5 byproduct lanes -> the hanging band drives the width; the byproduct band is the
		// deepest used band and drives the height to the full 206
		MaterialTreeDisplay tFe = MaterialTreeDisplay.of(tTree, MT.Fe, MaterialTreeLayoutTest::prefixItem, MaterialTreeLayoutTest::machineStack);
		assertNotNull(tFe);
		MaterialTreeLayout.Result tFeLayout = MaterialTreeLayout.plan(tFe);
		assertTrue(tFeLayout.byproductBand(), "iron carries byproducts");
		assertEquals(4 + (5 - 1) * MaterialTreeDisplay.LANE_PITCH + 18 + 12, tFeLayout.width(),
				"width = laneX0 + (max chain lane, byproduct slots - 1)*pitch + slot + margin = 146");
		assertEquals(MaterialTreeDisplay.stageY(MaterialTreeDisplay.COL_BYPRODUCT) + 18 + 12, tFeLayout.height(),
				"height = the byproduct band's slot row + slot + margin = 206");
		// over the whole pour: every plan fits inside the viewer pages' fixed canvas (the
		// fixed 202x206 is the model caps' worst case — MAX_ROWS lanes x 5 bands), and each
		// width re-derives from the content formula
		for (MaterialTreeDisplay tDisplay : MaterialTreeDisplay.buildAll(tTree, MaterialTreeLayoutTest::prefixItem, MaterialTreeLayoutTest::machineStack)) {
			MaterialTreeLayout.Result tLayout = MaterialTreeLayout.plan(tDisplay);
			assertTrue(tLayout.width() <= MaterialTreeDisplay.WIDTH, "width " + tLayout.width() + " within the page canvas");
			assertTrue(tLayout.height() <= MaterialTreeDisplay.HEIGHT, "height " + tLayout.height() + " within the page canvas");
			int tMaxRow = 0;
			for (MaterialTreeDisplay.Node tNode : tDisplay.nodes()) tMaxRow = Math.max(tMaxRow, tNode.row());
			int tLanes = Math.max(Math.max(tMaxRow + 1, tDisplay.byproducts().size()), 1);
			assertEquals(MaterialTreeDisplay.LANE_X0 + (tLanes - 1) * MaterialTreeDisplay.LANE_PITCH + 18 + 12, tLayout.width(),
					tDisplay.material.mNameInternal + " width = the content formula");
		}
	}

	@Test
	void engineDegenerateTrees() {
		// the trio seam lays out hand-built trees with no registry pour
		MaterialTreeDisplay.Node tSingle = new MaterialTreeDisplay.Node(OP.oreRaw, MaterialTreeDisplay.COL_ORE, 0, ItemStack.EMPTY);
		// the single node: one lane, one band, no byproducts -> the minimal positive canvas
		MaterialTreeLayout.Result tOne = MaterialTreeLayout.plan(List.of(tSingle), List.of(), List.of());
		assertEquals(MaterialTreeDisplay.LANE_X0 + 18 + 12, tOne.width(), "one lane: 34");
		assertEquals(MaterialTreeDisplay.stageY(MaterialTreeDisplay.COL_ORE) + 18 + 12, tOne.height(), "one band: 46");
		assertFalse(tOne.byproductBand());
		assertTrue(tOne.edges().isEmpty());
		// the empty tree: the guard keeps the canvas positive
		MaterialTreeLayout.Result tEmpty = MaterialTreeLayout.plan(List.of(), List.of(), List.of());
		assertTrue(tEmpty.width() > 0 && tEmpty.height() > 0, "the empty plan stays positive: " + tEmpty.width() + "x" + tEmpty.height());
		// a single node WITH byproducts: the hanging band is the deepest face
		MaterialTreeLayout.Result tOneByproduct = MaterialTreeLayout.plan(List.of(tSingle), List.of(),
				List.of(new MaterialTreeDisplay.Byproduct(ItemStack.EMPTY, false, MaterialTreeDisplay.DECLARED_TEXT, MaterialTreeDisplay.COL_ORE)));
		assertTrue(tOneByproduct.byproductBand());
		assertEquals(MaterialTreeDisplay.stageY(MaterialTreeDisplay.COL_BYPRODUCT) + 30, tOneByproduct.height());
		// a deep chain: a band-0 -> band-3 hop lays out and stays inside the plan's own bounds
		MaterialTreeDisplay.Node tDust = new MaterialTreeDisplay.Node(OP.dust, MaterialTreeDisplay.COL_DUST, 0, ItemStack.EMPTY);
		MaterialTreeDisplay.Edge tLong = new MaterialTreeDisplay.Edge(OP.oreRaw, OP.dust, List.of("gt.recipe.anvil"), ItemStack.EMPTY);
		MaterialTreeLayout.Result tDeep = MaterialTreeLayout.plan(List.of(tSingle, tDust), List.of(tLong), List.of());
		assertEquals(1, tDeep.edges().size());
		for (MaterialTreeLayout.EdgeLayout tPlan : tDeep.edges()) {
			for (MaterialTreeLayout.Rect tRect : tPlan.wire()) assertTrue(tRect.y() + tRect.h() <= tDeep.height(), "wire inside: " + tRect);
			for (MaterialTreeLayout.Rect tRect : tPlan.arrow()) assertTrue(tRect.y() + tRect.h() <= tDeep.height(), "arrow inside: " + tRect);
		}
	}

	@Test
	void engineNoOverlapInvariant() {
		MaterialTreeBuilder tTree = MaterialTreeBuilder.build();
		for (MaterialTreeDisplay tDisplay : MaterialTreeDisplay.buildAll(tTree, MaterialTreeLayoutTest::prefixItem, MaterialTreeLayoutTest::machineStack)) {
			MaterialTreeLayout.Result tLayout = MaterialTreeLayout.plan(tDisplay);
			List<MaterialTreeDisplay.Node> tNodes = tDisplay.nodes();
			// chain slots pairwise disjoint (distinct (band, lane) at pitch >= slot)
			for (int i = 0; i < tNodes.size(); i++)
				for (int j = i + 1; j < tNodes.size(); j++)
					assertTrue(boxesDisjoint(tLayout.nodeX(tNodes.get(i)), tLayout.nodeY(tNodes.get(i)),
							tLayout.nodeX(tNodes.get(j)), tLayout.nodeY(tNodes.get(j))), tDisplay.material.mNameInternal + " nodes " + i + "/" + j);
			// byproduct slots pairwise disjoint
			for (int i = 0; i < tDisplay.byproducts().size(); i++)
				for (int j = i + 1; j < tDisplay.byproducts().size(); j++)
					assertTrue(boxesDisjoint(tLayout.byproductX(i), tLayout.byproductY(), tLayout.byproductX(j), tLayout.byproductY()),
							tDisplay.material.mNameInternal + " byproducts " + i + "/" + j);
			// the hanging band sits below every chain band's slot row (outside the main axis)
			for (MaterialTreeDisplay.Node tNode : tNodes)
				if (tNode.column() != MaterialTreeDisplay.COL_BYPRODUCT)
					assertTrue(tLayout.byproductY() >= tLayout.nodeY(tNode) + MaterialTreeLayout.SLOT,
							tDisplay.material.mNameInternal + " byproduct band below " + tNode.prefix().mNameInternal);
		}
	}

	@Test
	void engineSpacingPolicyPin() {
		// the DEFAULT policy is the shipped geometry
		assertEquals(MaterialTreeDisplay.LANE_X0, MaterialTreeLayout.Policy.DEFAULT.laneX0());
		assertEquals(MaterialTreeDisplay.LANE_PITCH, MaterialTreeLayout.Policy.DEFAULT.lanePitch());
		assertEquals(12, MaterialTreeLayout.Policy.DEFAULT.rightMargin());
		assertEquals(12, MaterialTreeLayout.Policy.DEFAULT.bottomMargin());
		// 间距改一处全树生效: one policy field re-flows the lanes, the byproduct band and the canvas
		MaterialTreeDisplay.Node tLane0 = new MaterialTreeDisplay.Node(OP.oreRaw, MaterialTreeDisplay.COL_ORE, 0, ItemStack.EMPTY);
		MaterialTreeDisplay.Node tLane1 = new MaterialTreeDisplay.Node(OP.oreGravel, MaterialTreeDisplay.COL_ORE, 1, ItemStack.EMPTY);
		MaterialTreeLayout.Policy tWide = new MaterialTreeLayout.Policy(4, 40, 20, 12);
		MaterialTreeLayout.Result tResult = MaterialTreeLayout.plan(List.of(tLane0, tLane1), List.of(), List.of(), tWide);
		assertEquals(40, tResult.nodeX(tLane1) - tResult.nodeX(tLane0), "the lane pitch follows the policy alone");
		assertEquals(4 + 40 + 18 + 20, tResult.width(), "the canvas re-flows with the policy pitch + margin");
		assertEquals(MaterialTreeDisplay.stageY(MaterialTreeDisplay.COL_ORE) + 18 + 12, tResult.height(),
				"the vertical band table is the edge-budget contract, not a policy knob");
	}

	@Test
	void engineMatchesTheLegacyStatics() {
		// the viewer pages' static fixed-grid view == the engine under the default policy —
		// the migration pin that keeps the EMI/JEI legs and the engine on ONE geometry
		MaterialTreeBuilder tTree = MaterialTreeBuilder.build();
		for (MaterialTreeDisplay tDisplay : MaterialTreeDisplay.buildAll(tTree, MaterialTreeLayoutTest::prefixItem, MaterialTreeLayoutTest::machineStack)) {
			MaterialTreeLayout.Result tLayout = MaterialTreeLayout.plan(tDisplay);
			for (MaterialTreeDisplay.Node tNode : tDisplay.nodes()) {
				assertEquals(MaterialTreeDisplay.nodeX(tNode), tLayout.nodeX(tNode), "nodeX " + tNode.prefix().mNameInternal);
				assertEquals(MaterialTreeDisplay.nodeY(tNode), tLayout.nodeY(tNode), "nodeY " + tNode.prefix().mNameInternal);
			}
			for (int b = 0; b < tDisplay.byproducts().size(); b++) {
				assertEquals(MaterialTreeDisplay.byproductX(b), tLayout.byproductX(b), "byproductX " + b);
			}
			assertEquals(MaterialTreeDisplay.byproductY(), tLayout.byproductY());
			for (int c = MaterialTreeDisplay.COL_ORE; c <= MaterialTreeDisplay.COL_BYPRODUCT; c++) {
				assertEquals(MaterialTreeDisplay.stageY(c), tLayout.stageY(c), "stageY " + c);
				assertEquals(MaterialTreeDisplay.overflowY(c), tLayout.overflowY(c), "overflowY " + c);
			}
			assertEquals(MaterialTreeDisplay.overflowX(), tLayout.overflowX());
			// the edge plans: the static alias and the engine face land the same rects
			List<MaterialTreeLayout.EdgeLayout> tStatic = MaterialTreeLayout.layout(tDisplay);
			assertEquals(tStatic, tLayout.edges(), "the static alias is the default-policy engine");
		}
	}

	@Test
	void byproductBandNeverMovesTheMainAxis() {
		// the 副产挂边钉: the hanging band's presence and size change ONLY its own extent and
		// the canvas — no chain node coordinate and no chain edge plan moves
		MaterialTreeBuilder tTree = MaterialTreeBuilder.build();
		MaterialTreeDisplay tFe = MaterialTreeDisplay.of(tTree, MT.Fe, MaterialTreeLayoutTest::prefixItem, MaterialTreeLayoutTest::machineStack);
		assertNotNull(tFe);
		assertFalse(tFe.byproducts().isEmpty(), "the fixture actually carries a hanging band");
		MaterialTreeLayout.Result tWith = MaterialTreeLayout.plan(tFe.nodes(), tFe.edges(), tFe.byproducts());
		MaterialTreeLayout.Result tWithout = MaterialTreeLayout.plan(tFe.nodes(), tFe.edges(), List.of());
		for (MaterialTreeDisplay.Node tNode : tFe.nodes()) {
			assertEquals(tWithout.nodeX(tNode), tWith.nodeX(tNode), "chain lane unchanged: " + tNode.prefix().mNameInternal);
			assertEquals(tWithout.nodeY(tNode), tWith.nodeY(tNode), "chain band unchanged: " + tNode.prefix().mNameInternal);
		}
		assertEquals(tWithout.edges(), tWith.edges(), "the chain edge plans are byte-identical");
		assertFalse(tWithout.byproductBand());
		assertTrue(tWith.height() >= tWithout.height(), "the hanging band can only grow the canvas downward");
	}

	@Test
	void convergingSourcesShareOneTarget() {
		// the 覆盖链合流钉 (EMI's merge face, minus the fold): N sources converge on the ONE
		// shared node — every incoming arrow lands on the target's centre line, the target's
		// position is independent of its source count
		MaterialTreeBuilder tTree = MaterialTreeBuilder.build();
		MaterialTreeDisplay tCu = MaterialTreeDisplay.of(tTree, MT.Cu, MaterialTreeLayoutTest::prefixItem, MaterialTreeLayoutTest::machineStack);
		assertNotNull(tCu);
		MaterialTreeLayout.Result tLayout = MaterialTreeLayout.plan(tCu);
		Map<OreDictPrefix, MaterialTreeDisplay.Node> tNodes = new HashMap<>();
		for (MaterialTreeDisplay.Node tNode : tCu.nodes()) tNodes.put(tNode.prefix(), tNode);
		MaterialTreeDisplay.Node tTarget = tNodes.get(OP.crushedPurified);
		assertNotNull(tTarget, "the landed sifter shape gives Cu a crushedPurified node");
		int tSources = 0;
		for (int i = 0; i < tCu.edges().size(); i++) {
			MaterialTreeDisplay.Edge tEdge = tCu.edges().get(i);
			if (tEdge.to() != OP.crushedPurified) continue;
			tSources++;
			MaterialTreeLayout.EdgeLayout tPlan = tLayout.edges().get(i);
			int tCentreX = tLayout.nodeX(tTarget) + MaterialTreeLayout.SLOT / 2;
			Rect tBase = tPlan.arrow().stream().filter(r -> r.h() == MaterialTreeLayout.ARROW_HEAD).findFirst().orElseThrow();
			assertEquals(tCentreX, tBase.x(), "source " + tEdge.from().mNameInternal + " arrow centre on the target centre line");
			assertEquals(tLayout.nodeY(tTarget), tBase.y() + tBase.h(), "the arrow tip lands on the target's top edge");
		}
		assertTrue(tSources >= 4, "the landed sifter rows converge >= 4 ore inputs on crushedPurified, got " + tSources);
	}

	private static boolean boxesDisjoint(int aX1, int aY1, int aX2, int aY2) {
		return aX1 + MaterialTreeLayout.SLOT <= aX2 || aX2 + MaterialTreeLayout.SLOT <= aX1
				|| aY1 + MaterialTreeLayout.SLOT <= aY2 || aY2 + MaterialTreeLayout.SLOT <= aY1;
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
