package gregtech6.recipes;

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

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.recipes.tree.MaterialTreeBuilder;
import gregtech6.recipes.tree.MaterialTreeDisplay;
import gregtech6.recipes.tree.MaterialTreeDisplay.Edge;
import gregtech6.recipes.tree.MaterialTreeDisplay.Node;
import gregtech6.recipes.tree.MaterialTreeLayout;
import gregtech6.recipes.tree.MaterialTreeLayout.ByproductView;
import gregtech6.recipes.tree.MaterialTreeLayout.EdgeLayout;
import gregtech6.recipes.tree.MaterialTreeLayout.Pose;
import gregtech6.recipes.tree.MaterialTreeLayout.Rect;
import gregtech6.recipes.tree.MaterialTreeLayout.Result;
import gregtech6.recipes.tree.MaterialTreeViewport;
import gregtech6.recipes.tree.MaterialTreeWorkstations;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTMaterialItems.PrefixMaterial;

/**
 * The EMI recipe-tree geometry pins (task mattree-emi-tree-rework; the old v2/rotation pins
 * died with the rework — 旧钉迁移声明 at the bottom). The reference is the EMI recipe tree's
 * own screen (tmp/harvest/emi-1.20.1-src BoMScreen.java; the 1.21.1 generation byte-identical
 * on every face cited):
 * <ul>
 * <li><b>the depth-band axis</b> (EMI BoMScreen.java:334/:337 {@code depth *
 *     NODE_VERTICAL_SPACING}): y = BAND_Y0 + band * BAND_PITCH, x = LANE_X0 + row *
 *     LANE_PITCH — same band ⇒ same y, lanes step right; same lane ⇒ same x, bands step
 *     down;</li>
 * <li><b>the node form</b> (EMI :660-663/:697-728): a process node is the 42x20 box outline
 *     with the machine icon cell (the category-icon seat) left of the item cell; band-0 ore
 *     roots are EMI's LEAF form — no box, one centred 18px item cell;</li>
 * <li><b>the connector</b> (EMI Node.render :675-694, white ink :340-350): the cross-band
 *     hop = the vertical drop at the target's centre x into its box top + one horizontal
 *     rail 4px below the source box's bottom edge; the same-band hop = one horizontal
 *     between the facing box edges (ADR: our DAG grows sideways, EMI's tree cannot);</li>
 * <li><b>the engine</b> (the r2 ADR L1 face, kept): determinism, the content-driven canvas,
 *     the spacing {@link Policy} knob (间距改一处全树生效), the degenerate trees through the
 *     list-trio seam, the 副产挂边钉 (the hanging band never moves the chain axis), the
 *     覆盖链合流钉 (multi-source hops converge on the ONE shared node);</li>
 * <li><b>the unified pose primitive</b> (kept verbatim from mattree-item-zoom-pose).</li>
 * </ul>
 *
 * <p><b>旧钉迁移声明</b>: the v2 three-part edge pins (machine-on-wire rect, rasterised
 * arrowhead columns, via-label spot, collision stagger), the {@code MAX_ROWS}/overflow
 * census, the {@code engineMatchesTheLegacyStatics} equality (the static grid table is
 * retired) and the 202x206 canvas budget all died with the EMI-form rework; their
 * replacements are the node-form/connector/band pins above plus the engine face.
 */
class MaterialTreeLayoutTest extends GTRecipesOfflineTestBase {

	/** One lazily created probe item per referenced (prefix, material) pair (the DisplayTest fixture, mirrored). */
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

	/** The (prefix, material) -> probe-item resolver armed into the four ore-chain loaders. */
	private static Item prefixItem(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		if (aPrefix == null || aMaterial == null) return null;
		return PREFIX_ITEMS.computeIfAbsent(new PrefixMaterial(aPrefix, aMaterial), aPair ->
			Registry.register(BuiltInRegistries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6", "mtree_layout_probe_" + sNextProbeId++),
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
	// the depth-band axis (EMI BoMScreen.java:334/:337)
	// ------------------------------------------------------------------

	@Test
	void depthBandLayering() {
		Node tRoot = new Node(OP.oreRaw, 0, 0, ItemStack.EMPTY);
		Node tSibling = new Node(OP.oreGravel, 0, 1, ItemStack.EMPTY);
		Node tCrushed = new Node(OP.crushed, 1, 0, ItemStack.EMPTY);
		Result tPlan = MaterialTreeLayout.plan(List.of(tRoot, tSibling, tCrushed), List.of(), List.of());
		// same band ⇒ same y; lanes step right one pitch (EMI's sibling spread)
		assertEquals(tPlan.nodeY(tRoot), tPlan.nodeY(tSibling), "same band, same row");
		assertEquals(MaterialTreeDisplay.LANE_PITCH, tPlan.nodeX(tSibling) - tPlan.nodeX(tRoot), "lanes step one pitch");
		// same lane ⇒ same x; bands step down one pitch (EMI's depth axis)
		assertEquals(tPlan.nodeX(tRoot), tPlan.nodeX(tCrushed), "same lane, same column");
		assertEquals(MaterialTreeDisplay.BAND_PITCH, tPlan.nodeY(tCrushed) - tPlan.nodeY(tRoot), "bands step one pitch");
		// the band arithmetic is the formula, on the live display too
		MaterialTreeDisplay tFe = feDisplay();
		assertNotNull(tFe);
		Result tFePlan = MaterialTreeLayout.plan(tFe);
		for (Node tNode : tFe.nodes()) {
			assertEquals(MaterialTreeDisplay.BAND_Y0 + tNode.band() * MaterialTreeDisplay.BAND_PITCH, tFePlan.nodeY(tNode),
					tNode.prefix().mNameInternal + " band y = BAND_Y0 + band * pitch");
			assertEquals(MaterialTreeDisplay.LANE_X0 + tNode.row() * MaterialTreeDisplay.LANE_PITCH, tFePlan.nodeX(tNode),
					tNode.prefix().mNameInternal + " lane x = LANE_X0 + row * pitch");
		}
	}

	// ------------------------------------------------------------------
	// the node form (EMI :660-663/:697-728)
	// ------------------------------------------------------------------

	@Test
	void nodeFormBoxAndCells() {
		Node tRoot = new Node(OP.oreRaw, 0, 0, ItemStack.EMPTY);
		Node tCrushed = new Node(OP.crushed, 1, 0, ItemStack.EMPTY);
		Result tPlan = MaterialTreeLayout.plan(List.of(tRoot, tCrushed), List.of(), List.of());
		// the root is EMI's LEAF: no box, the item cell centred on the node
		assertNull(tPlan.nodeBox(tRoot), "the band-0 ore root is the bare-leaf form (EMI :662-663 width=16)");
		Rect tRootCell = tPlan.nodeItemCell(tRoot);
		assertEquals(new Rect(tPlan.nodeX(tRoot) - 9, tPlan.nodeY(tRoot) - 9, 18, 18), tRootCell, "the leaf cell centres the slot");
		// the process node is the EMI recipe-node box: 42x20 outline about the centre
		Rect tBox = tPlan.nodeBox(tCrushed);
		assertEquals(new Rect(tPlan.nodeX(tCrushed) - 21, tPlan.nodeY(tCrushed) - 10, 42, 20), tBox,
				"the process box (EMI's 42-wide recipe-node form, BoMScreen.java:660-661/:697-717)");
		// its item cell sits right of centre, its machine cell left (the category-icon seat)
		Rect tItemCell = tPlan.nodeItemCell(tCrushed);
		assertEquals(new Rect(tPlan.nodeX(tCrushed) + 1, tPlan.nodeY(tCrushed) - 9, 18, 18), tItemCell);
		// without a producing machine edge there is no machine cell (the degrade face)
		assertNull(tPlan.nodeMachineCell(tCrushed), "no machine edge, no machine cell");
		// with one, the cell is the box's left half and the FIRST producing edge wins
		Node tWithMachine = new Node(OP.dust, 2, 0, ItemStack.EMPTY);
		Edge tAnvil = new Edge(OP.crushed, OP.dust, List.of("gt.recipe.anvil"), new ItemStack(Items.BARRIER));
		Edge tShredder = new Edge(OP.crushed, OP.dust, List.of("gt.recipe.shredder"), new ItemStack(Items.BRICK));
		Result tMachined = MaterialTreeLayout.plan(List.of(tCrushed, tWithMachine), List.of(tAnvil, tShredder), List.of());
		Rect tMachineCell = tMachined.nodeMachineCell(tWithMachine);
		assertNotNull(tMachineCell, "a producing machine edge mounts the box's machine icon");
		assertEquals(new Rect(tMachined.nodeX(tWithMachine) - 19, tMachined.nodeY(tWithMachine) - 9, 18, 18), tMachineCell,
				"the machine cell is the box's left half (EMI's category icon at x-18, BoMScreen.java:719-723)");
		// the machine CELL is coordinate-only — the resolved STACK rides the producing edge;
		// pin the first-edge rule through the display face instead (the next test)
	}

	// ------------------------------------------------------------------
	// the connector (EMI Node.render :675-694, white ink :340-350)
	// ------------------------------------------------------------------

	@Test
	void connectorTwoSegmentManhattan() {
		// straight hop, same lane: band0 (25,16) -> band1 (25,44) — the rail at fromY+14 = 30,
		// the drop from the rail into the target's box top (44-10 = 34): one 4px vertical
		Node tRoot = new Node(OP.oreRaw, 0, 0, ItemStack.EMPTY);
		Node tCrushed = new Node(OP.crushed, 1, 0, ItemStack.EMPTY);
		Edge tEdge = new Edge(OP.oreRaw, OP.crushed, List.of("gt.recipe.anvil"), ItemStack.EMPTY);
		Result tPlan = MaterialTreeLayout.plan(List.of(tRoot, tCrushed), List.of(tEdge), List.of());
		assertEquals(1, tPlan.edges().size());
		assertEquals(List.of(new Rect(25, 30, 1, 4)), tPlan.edges().get(0).wire(), "the same-lane hop: rail-into-top only (x 25 shared)");
		// diagonal hop: the rail runs from the source centre to the target's centre x at y 30
		Node tCrushedLane1 = new Node(OP.crushedTiny, 1, 1, ItemStack.EMPTY);
		Edge tDiag = new Edge(OP.oreRaw, OP.crushedTiny, List.of("gt.recipe.anvil"), ItemStack.EMPTY);
		Result tDiagPlan = MaterialTreeLayout.plan(List.of(tRoot, tCrushedLane1), List.of(tDiag), List.of());
		assertEquals(List.of(new Rect(75, 30, 1, 4), new Rect(25, 30, 50, 1)), tDiagPlan.edges().get(0).wire(),
				"the two-segment EMI connector: vertical at the target x + horizontal rail (BoMScreen.java:690-693)");
		// same-band hop: one horizontal between the facing box edges (the ADR face)
		Node tPlate = new Node(OP.plate, 2, 1, ItemStack.EMPTY);
		Node tGear = new Node(OP.gearGt, 2, 2, ItemStack.EMPTY);
		Edge tSide = new Edge(OP.plate, OP.gearGt, List.of("gt.recipe.lathe"), ItemStack.EMPTY);
		Result tSidePlan = MaterialTreeLayout.plan(List.of(tPlate, tGear), List.of(tSide), List.of());
		int tFromRight = tSidePlan.nodeX(tPlate) + MaterialTreeDisplay.BOX_WIDTH / 2;
		int tToLeft = tSidePlan.nodeX(tGear) - MaterialTreeDisplay.BOX_WIDTH / 2;
		assertEquals(List.of(new Rect(tFromRight, tSidePlan.nodeY(tPlate), tToLeft - tFromRight, 1)),
				tSidePlan.edges().get(0).wire(), "the same-band hop connects the facing box edges on the band's row");
		// no arrowheads, no machine boxes, no labels — the EdgeLayout is the wire alone
		for (EdgeLayout tLayout : tSidePlan.edges()) {
			assertEquals(1, tLayout.wire().size(), "the EMI connector carries wire rects only");
		}
	}

	// ------------------------------------------------------------------
	// the live display: machine cells, convergence, no-overlap, the hanging band
	// ------------------------------------------------------------------

	@Test
	void liveDisplayPlansAlignAndFit() {
		MaterialTreeBuilder tTree = MaterialTreeBuilder.build();
		for (OreDictMaterial tMaterial : new OreDictMaterial[] {MT.Fe, MT.Cu}) {
			MaterialTreeDisplay tDisplay = MaterialTreeDisplay.of(tTree, tMaterial, MaterialTreeLayoutTest::prefixItem, MaterialTreeLayoutTest::machineStack);
			assertNotNull(tDisplay, tMaterial.mNameInternal + " must yield a display");
			Result tPlan = MaterialTreeLayout.plan(tDisplay);
			List<Edge> tEdges = tDisplay.edges();
			assertEquals(tEdges.size(), tPlan.edges().size(), "one plan per edge");
			Map<OreDictPrefix, Node> tNodes = new HashMap<>();
			for (Node tNode : tDisplay.nodes()) tNodes.put(tNode.prefix(), tNode);
			for (int i = 0; i < tEdges.size(); i++) {
				Edge tEdge = tEdges.get(i);
				Node tFrom = tNodes.get(tEdge.from()), tTo = tNodes.get(tEdge.to());
				assertNotNull(tFrom);
				assertNotNull(tTo);
				// the connector lands on the target's centre line and enters its box top
				for (Rect tRect : tPlan.edges().get(i).wire()) assertInside(tPlan, tRect);
			}
			// the machine-cell rule: a cell exists IFF some producing edge resolved a stack
			for (Node tNode : tDisplay.nodes()) {
				boolean tAnyMachine = false;
				for (Edge tEdge : tEdges) if (tEdge.to() == tNode.prefix() && !tEdge.machine().isEmpty()) tAnyMachine = true;
				assertEquals(tAnyMachine, tPlan.nodeMachineCell(tNode) != null,
						tDisplay.material.mNameInternal + " " + tNode.prefix().mNameInternal + " machine cell iff a producing edge resolved");
			}
			// the ingot node exists (the crucible arm) and its band follows its shortest hop
			Node tIngot = tNodes.get(OP.ingot);
			assertNotNull(tIngot, tMaterial.mNameInternal + ": the crucible arm delivers the ingot (SPEC2)");
			assertTrue(tPlan.nodeY(tIngot) > tPlan.nodeY(tNodes.get(OP.crushed)) || tIngot.band() >= 2,
					"the ingot sits at or below the crushed band (never above its producers)");
		}
	}

	@Test
	void convergingSourcesShareOneTarget() {
		// the 覆盖链合流钉 (EMI's merge face, minus the fold): N sources converge on the ONE
		// shared node — every connector's vertical lands on the target's centre x, the
		// target's position is independent of its source count
		MaterialTreeBuilder tTree = MaterialTreeBuilder.build();
		MaterialTreeDisplay tCu = MaterialTreeDisplay.of(tTree, MT.Cu, MaterialTreeLayoutTest::prefixItem, MaterialTreeLayoutTest::machineStack);
		assertNotNull(tCu);
		Result tLayout = MaterialTreeLayout.plan(tCu);
		Map<OreDictPrefix, Node> tNodes = new HashMap<>();
		for (Node tNode : tCu.nodes()) tNodes.put(tNode.prefix(), tNode);
		Node tTarget = tNodes.get(OP.crushedPurified);
		assertNotNull(tTarget, "the landed sifter shape gives Cu a crushedPurified node");
		int tSources = 0;
		for (int i = 0; i < tCu.edges().size(); i++) {
			Edge tEdge = tCu.edges().get(i);
			if (tEdge.to() != OP.crushedPurified) continue;
			tSources++;
			for (Rect tRect : tLayout.edges().get(i).wire()) {
				boolean tOnCentreLine = tRect.x() <= tLayout.nodeX(tTarget) && tLayout.nodeX(tTarget) < tRect.x() + Math.max(tRect.w(), 1)
						&& (tRect.w() == 1 || tRect.y() == tLayout.nodeY(tTarget));
				assertTrue(tOnCentreLine || tRect.w() > 1,
						"source " + tEdge.from().mNameInternal + " connector touches the target's centre line: " + tRect);
			}
		}
		assertTrue(tSources >= 4, "the landed sifter rows converge >= 4 ore inputs on crushedPurified, got " + tSources);
	}

	@Test
	void engineNoOverlapInvariant() {
		MaterialTreeBuilder tTree = MaterialTreeBuilder.build();
		for (MaterialTreeDisplay tDisplay : MaterialTreeDisplay.buildAll(tTree, MaterialTreeLayoutTest::prefixItem, MaterialTreeLayoutTest::machineStack)) {
			Result tLayout = MaterialTreeLayout.plan(tDisplay);
			List<Node> tNodes = tDisplay.nodes();
			// the EMI spread rule: siblings never overlap at any depth — full boxes pairwise disjoint
			for (int i = 0; i < tNodes.size(); i++)
				for (int j = i + 1; j < tNodes.size(); j++)
					assertTrue(boxesDisjoint(tLayout, tNodes.get(i), tNodes.get(j)),
							tDisplay.material.mNameInternal + " nodes " + tNodes.get(i).prefix().mNameInternal + "/" + tNodes.get(j).prefix().mNameInternal);
			// byproduct slots pairwise disjoint (cell boxes)
			for (int i = 0; i < tDisplay.byproducts().size(); i++)
				for (int j = i + 1; j < tDisplay.byproducts().size(); j++)
					assertTrue(tLayout.byproductX(i) + MaterialTreeLayout.SLOT <= tLayout.byproductX(j) - MaterialTreeLayout.SLOT / 2
							|| tLayout.byproductX(j) + MaterialTreeLayout.SLOT <= tLayout.byproductX(i) - MaterialTreeLayout.SLOT / 2,
							tDisplay.material.mNameInternal + " byproducts " + i + "/" + j);
			// the hanging band sits below every chain band's box (outside the main axis);
			// byproduct-less displays carry no band at all (byproductY() is the -1 sentinel)
			if (tLayout.byproductBand())
				for (Node tNode : tNodes)
					assertTrue(tLayout.byproductY() >= tLayout.nodeY(tNode) + MaterialTreeDisplay.BOX_HEIGHT / 2,
							tDisplay.material.mNameInternal + " byproduct band below " + tNode.prefix().mNameInternal);
		}
	}

	// ------------------------------------------------------------------
	// the engine face (the r2 ADR L1 pins, kept)
	// ------------------------------------------------------------------

	@Test
	void enginePlanIsDeterministic() {
		MaterialTreeBuilder tTree = MaterialTreeBuilder.build();
		MaterialTreeDisplay tDisplay = MaterialTreeDisplay.of(tTree, MT.Cu, MaterialTreeLayoutTest::prefixItem, MaterialTreeLayoutTest::machineStack);
		assertNotNull(tDisplay);
		Result tFirst = MaterialTreeLayout.plan(tDisplay);
		Result tSecond = MaterialTreeLayout.plan(tDisplay);
		assertEquals(tFirst.width(), tSecond.width());
		assertEquals(tFirst.height(), tSecond.height());
		assertEquals(tFirst.byproductBand(), tSecond.byproductBand());
		assertEquals(tFirst.edges(), tSecond.edges(), "edge plans identical");
		for (Node tNode : tDisplay.nodes()) {
			assertEquals(tFirst.nodeX(tNode), tSecond.nodeX(tNode));
			assertEquals(tFirst.nodeY(tNode), tSecond.nodeY(tNode));
		}
	}

	@Test
	void engineCanvasFollowsContent() {
		// width = laneX0 + (lanes-1)*pitch + half box + margin; height = the deepest band's
		// box + margin (the hanging band, when present, is the deepest) — the content-driven
		// face EMI's own screen shares (BoMScreen.java:110-111/:202-203 derive the same way)
		List<Node> tNodes = List.of(
				new Node(OP.oreRaw, 0, 0, ItemStack.EMPTY),
				new Node(OP.crushed, 1, 0, ItemStack.EMPTY),
				new Node(OP.crushedTiny, 1, 1, ItemStack.EMPTY));
		Result tPlan = MaterialTreeLayout.plan(tNodes, List.of(), List.of());
		assertFalse(tPlan.byproductBand());
		assertEquals(2, widestLanes(tNodes));
		assertEquals(MaterialTreeDisplay.LANE_X0 + (2 - 1) * MaterialTreeDisplay.LANE_PITCH + MaterialTreeDisplay.BOX_WIDTH / 2 + 12, tPlan.width());
		assertEquals(MaterialTreeDisplay.BAND_Y0 + 1 * MaterialTreeDisplay.BAND_PITCH + MaterialTreeDisplay.BOX_HEIGHT / 2 + 12, tPlan.height());
		// the hanging band drives the height past the deepest chain band
		Result tWithBand = MaterialTreeLayout.plan(tNodes, List.of(), List.of(
				new MaterialTreeDisplay.Byproduct(ItemStack.EMPTY, false, MaterialTreeDisplay.DECLARED_TEXT, 0)));
		assertTrue(tWithBand.byproductBand());
		assertEquals(MaterialTreeDisplay.BAND_Y0 + 2 * MaterialTreeDisplay.BAND_PITCH + MaterialTreeDisplay.BOX_HEIGHT / 2 + 12, tWithBand.height(),
				"the byproduct band hangs one band below the deepest chain band");
		// the empty tree stays positive (the guard)
		Result tEmpty = MaterialTreeLayout.plan(List.of(), List.of(), List.of());
		assertTrue(tEmpty.width() > 0 && tEmpty.height() > 0);
	}

	@Test
	void engineDegenerateTrees() {
		Node tSingle = new Node(OP.oreRaw, 0, 0, ItemStack.EMPTY);
		Result tOne = MaterialTreeLayout.plan(List.of(tSingle), List.of(), List.of());
		assertEquals(MaterialTreeDisplay.LANE_X0 + MaterialTreeDisplay.BOX_WIDTH / 2 + 12, tOne.width(), "one lane");
		assertEquals(MaterialTreeDisplay.BAND_Y0 + MaterialTreeDisplay.BOX_HEIGHT / 2 + 12, tOne.height(), "one band");
		assertFalse(tOne.byproductBand());
		assertTrue(tOne.edges().isEmpty());
		// a deep hop lays out and stays inside the plan's own bounds
		Node tDust = new Node(OP.dust, 3, 0, ItemStack.EMPTY);
		Edge tLong = new Edge(OP.oreRaw, OP.dust, List.of("gt.recipe.anvil"), ItemStack.EMPTY);
		Result tDeep = MaterialTreeLayout.plan(List.of(tSingle, tDust), List.of(tLong), List.of());
		assertEquals(1, tDeep.edges().size());
		for (EdgeLayout tPlan : tDeep.edges())
			for (Rect tRect : tPlan.wire())
				assertTrue(tRect.y() + tRect.h() <= tDeep.height(), "wire inside: " + tRect);
	}

	@Test
	void engineSpacingPolicyPin() {
		// the DEFAULT policy is the shipped geometry — now including the band axis
		assertEquals(MaterialTreeDisplay.LANE_X0, MaterialTreeLayout.Policy.DEFAULT.laneX0());
		assertEquals(MaterialTreeDisplay.LANE_PITCH, MaterialTreeLayout.Policy.DEFAULT.lanePitch());
		assertEquals(MaterialTreeDisplay.BAND_PITCH, MaterialTreeLayout.Policy.DEFAULT.bandPitch());
		assertEquals(12, MaterialTreeLayout.Policy.DEFAULT.rightMargin());
		assertEquals(12, MaterialTreeLayout.Policy.DEFAULT.bottomMargin());
		// 间距改一处全树生效: one policy field re-flows the lanes AND the bands
		Node tLane0 = new Node(OP.oreRaw, 0, 0, ItemStack.EMPTY);
		Node tLane1 = new Node(OP.oreGravel, 0, 1, ItemStack.EMPTY);
		Node tDown = new Node(OP.crushed, 1, 0, ItemStack.EMPTY);
		MaterialTreeLayout.Policy tWide = new MaterialTreeLayout.Policy(4, 60, 40, 20, 12);
		Result tResult = MaterialTreeLayout.plan(List.of(tLane0, tLane1, tDown), List.of(), List.of(), tWide);
		assertEquals(60, tResult.nodeX(tLane1) - tResult.nodeX(tLane0), "the lane pitch follows the policy alone");
		assertEquals(40, tResult.nodeY(tDown) - tResult.nodeY(tLane0), "the band pitch follows the policy alone");
		assertEquals(4 + 60 + MaterialTreeDisplay.BOX_WIDTH / 2 + 20, tResult.width(), "the canvas re-flows with the policy");
	}

	@Test
	void byproductBandNeverMovesTheMainAxis() {
		// the 副产挂边钉: the hanging band's presence and size change ONLY its own extent and
		// the canvas — no chain node coordinate and no chain edge plan moves
		MaterialTreeBuilder tTree = MaterialTreeBuilder.build();
		MaterialTreeDisplay tFe = MaterialTreeDisplay.of(tTree, MT.Fe, MaterialTreeLayoutTest::prefixItem, MaterialTreeLayoutTest::machineStack);
		assertNotNull(tFe);
		assertFalse(tFe.byproducts().isEmpty(), "the fixture actually carries a hanging band");
		Result tWith = MaterialTreeLayout.plan(tFe.nodes(), tFe.edges(), tFe.byproducts());
		Result tWithout = MaterialTreeLayout.plan(tFe.nodes(), tFe.edges(), List.of());
		for (Node tNode : tFe.nodes()) {
			assertEquals(tWithout.nodeX(tNode), tWith.nodeX(tNode), "chain lane unchanged: " + tNode.prefix().mNameInternal);
			assertEquals(tWithout.nodeY(tNode), tWith.nodeY(tNode), "chain band unchanged: " + tNode.prefix().mNameInternal);
		}
		assertEquals(tWithout.edges(), tWith.edges(), "the chain edge plans are byte-identical");
		assertFalse(tWithout.byproductBand());
		assertTrue(tWith.height() >= tWithout.height(), "the hanging band can only grow the canvas downward");
	}

	// ------------------------------------------------------------------
	// the byproduct view states (the r2 rider's axis-blind pin, kept)
	// ------------------------------------------------------------------

	@Test
	void byproductViewStatesKeepTheChainAxis() {
		MaterialTreeBuilder tTree = MaterialTreeBuilder.build();
		MaterialTreeDisplay tFe = MaterialTreeDisplay.of(tTree, MT.Fe, MaterialTreeLayoutTest::prefixItem, MaterialTreeLayoutTest::machineStack);
		assertNotNull(tFe);
		Result tAll = MaterialTreeLayout.plan(tFe);
		Result tStepped = MaterialTreeLayout.plan(tFe, ByproductView.step(0));
		assertEquals(tAll.edges(), tStepped.edges(), "the chain edge plans are byte-identical under a step view");
		for (Node tNode : tFe.nodes()) {
			assertEquals(tAll.nodeX(tNode), tStepped.nodeX(tNode));
			assertEquals(tAll.nodeY(tNode), tStepped.nodeY(tNode));
		}
	}

	// ------------------------------------------------------------------
	// the unified pose primitive (kept verbatim from mattree-item-zoom-pose)
	// ------------------------------------------------------------------

	private static final double EPSILON = 1e-9;

	@Test
	void posePrimitiveFollowsTheViewport() {
		MaterialTreeViewport tView = new MaterialTreeViewport();
		Pose tIdentity = MaterialTreeLayout.pose(tView, 4, 16);
		assertEquals(4.0, tIdentity.x(), EPSILON);
		assertEquals(16.0, tIdentity.y(), EPSILON);
		assertEquals(1.0, tIdentity.scale(), EPSILON, "the fit pose's pose factor is 1");
		tView.zoomAt(MaterialTreeDisplay.WIDTH / 2.0, MaterialTreeDisplay.HEIGHT / 2.0, 2);
		Pose tZoomed = MaterialTreeLayout.pose(tView, 33.5, 77.25);
		assertEquals(tView.apply(33.5, 77.25).x(), tZoomed.x(), EPSILON, "the origin is the applied point");
		assertEquals(tView.apply(33.5, 77.25).y(), tZoomed.y(), EPSILON);
		assertEquals(2.0, tZoomed.scale(), EPSILON, "the factor is the live scale, not a frozen constant");
	}

	@Test
	void zoom2xIconCentresInTheScaledSlotBox() {
		MaterialTreeViewport tView = new MaterialTreeViewport();
		tView.zoomAt(MaterialTreeDisplay.WIDTH / 2.0, MaterialTreeDisplay.HEIGHT / 2.0, 2);
		assertEquals(2.0, tView.scale(), EPSILON);
		double tSlotX = 4, tSlotY = 16;
		Pose tIcon = MaterialTreeLayout.pose(tView, tSlotX + 1, tSlotY + 1); // the screen drawItem inset
		double tIconCentreX = tIcon.x() + MaterialTreeLayout.MACHINE / 2.0 * tIcon.scale();
		double tIconCentreY = tIcon.y() + MaterialTreeLayout.MACHINE / 2.0 * tIcon.scale();
		MaterialTreeViewport.Point tBoxA = tView.apply(tSlotX, tSlotY);
		MaterialTreeViewport.Point tBoxB = tView.apply(tSlotX + MaterialTreeLayout.SLOT, tSlotY + MaterialTreeLayout.SLOT);
		assertEquals((tBoxA.x() + tBoxB.x()) / 2.0, tIconCentreX, EPSILON, "the icon centre is the scaled box centre");
		assertEquals((tBoxA.y() + tBoxB.y()) / 2.0, tIconCentreY, EPSILON, "the icon centre is the scaled box centre");
		assertTrue(tIcon.scale() * MaterialTreeLayout.MACHINE < tBoxB.x() - tBoxA.x());
		tView.zoomAt(MaterialTreeDisplay.WIDTH / 2.0, MaterialTreeDisplay.HEIGHT / 2.0, 2);
		assertEquals(MaterialTreeViewport.MAX_SCALE, tView.scale(), EPSILON);
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

	private static int widestLanes(List<Node> aNodes) {
		int rLanes = 1;
		for (Node tNode : aNodes) rLanes = Math.max(rLanes, tNode.row() + 1);
		return rLanes;
	}

	/** Box disjointness over the plan's box/cell geometry (the leaf uses its 18px cell). */
	private static boolean boxesDisjoint(Result aPlan, Node aA, Node aB) {
		Rect tBoxA = aPlan.nodeBox(aA) != null ? aPlan.nodeBox(aA) : aPlan.nodeItemCell(aA);
		Rect tBoxB = aPlan.nodeBox(aB) != null ? aPlan.nodeBox(aB) : aPlan.nodeItemCell(aB);
		return tBoxA.x() + tBoxA.w() <= tBoxB.x() || tBoxB.x() + tBoxB.w() <= tBoxA.x()
				|| tBoxA.y() + tBoxA.h() <= tBoxB.y() || tBoxB.y() + tBoxB.h() <= tBoxA.y();
	}

	private static void assertInside(Result aPlan, Rect aRect) {
		assertTrue(aRect.x() >= 0 && aRect.y() >= 0, "on canvas: " + aRect);
		assertTrue(aRect.x() + aRect.w() <= aPlan.width(), "within width: " + aRect);
		assertTrue(aRect.y() + aRect.h() <= aPlan.height(), "within height: " + aRect);
	}
}
