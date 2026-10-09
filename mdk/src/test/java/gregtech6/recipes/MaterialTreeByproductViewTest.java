package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import gregtech6.recipes.tree.MaterialTreeDisplay.Byproduct;
import gregtech6.recipes.tree.MaterialTreeDisplay.Node;
import gregtech6.recipes.tree.MaterialTreeLayout;
import gregtech6.recipes.tree.MaterialTreeLayout.ByproductView;
import gregtech6.recipes.tree.MaterialTreeLayout.Result;
import gregtech6.recipes.tree.MaterialTreeWorkstations;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTMaterialItems.PrefixMaterial;

/**
 * The byproduct view-state pins (task mattree-r2-byproduct-per-step, the ADR
 * 2026-10-06-mattree-refactor R2 rider): {@link ByproductView} is the 逐步/逐带
 * reveal face over the hanging band's {@code step} attribution, and the reveal must
 * be deterministic and axis-blind —
 * <ul>
 * <li><b>determinism</b>: the same state over the same tree filters to the same
 *     visible set and plans to the same geometry, twice in a row and across fresh
 *     pours (the pure-filter contract the R3 host-leg switch will ride);</li>
 * <li><b>the main axis is blind to the view</b> (the root card's 副产不挪主轴钉,
 *     task mattree-r2-layout-engine, generalized to every state): chain node
 *     coordinates and edge plans are byte-identical under ALL / step(n) /
 *     cumulative(n) over the whole pour;</li>
 * <li><b>the cumulative reveal is monotone</b>: growing n only ever grows the
 *     content box — the visible sets nest, the box width follows the visible lane
 *     count, and an empty reveal drops the hanging band back to the main axis
 *     instead of leaving a hole;</li>
 * <li><b>the degenerate face</b>: a no-byproduct tree plans identically under
 *     every state;</li>
 * <li><b>the step semantics</b> (step(n) == the step-n slots, a {@code step == -1}
 *     slot shows only under ALL) ride the hand-built list-trio fixture so no live
 *     pour's step distribution can make them vacuous;</li>
 * <li><b>the live attribution</b>: the offline Fe face is all crushing-step (the
 *     derived Fe-&gt;Fe2O3 crusher redirect rides the oreRaw/blockRaw input bands,
 *     both COL_ORE, and the declared face is the same redirect's tail), and every
 *     poured slot's step lands inside the displayed band range or the -1 tail.</li>
 * </ul>
 *
 * <p>旧钉迁移声明: this class ADDS the view-state pins on top of the root card's
 * {@code byproductBandNeverMovesTheMainAxis} (MaterialTreeLayoutTest) — no pin was
 * retired; that pin's invariant is the axis-blind clause above, extended from
 * band-presence to every view state.
 */
class MaterialTreeByproductViewTest extends GTRecipesOfflineTestBase {

	/** One lazily created probe item per referenced (prefix, material) pair (the MaterialTreeLayoutTest fixture, mirrored). */
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

	/** The (prefix, material) -> probe-item resolver armed into the four ore-chain loaders (the LayoutTest fixture). */
	private static Item prefixItem(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		if (aPrefix == null || aMaterial == null) return null;
		return PREFIX_ITEMS.computeIfAbsent(new PrefixMaterial(aPrefix, aMaterial), aPair ->
			Registry.register(BuiltInRegistries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6", "mtree_view_probe_" + sNextProbeId++),
				new MaterialPrefixItem(new Item.Properties(), aPair.prefix(), aPair.material())));
	}

	/** The v2 machine resolver fixture: the TABLED maps get a registered vanilla stack, anything else EMPTY (the LayoutTest fixture). */
	private static ItemStack machineStack(String aMapName) {
		return MaterialTreeWorkstations.tableNames().contains(aMapName) ? new ItemStack(Items.BARRIER) : ItemStack.EMPTY;
	}

	/** The offline item-registry unlock (MaterialTreeLayoutTest.openOfflineItemRegistry, mirrored verbatim). */
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

	/** getDeclaredField along the superclass chain (the MaterialTreeLayoutTest helper, mirrored). */
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
		pourSeams();
	}

	@AfterEach
	void restoreSeamsAndDropGeneration() {
		for (Runnable tRestore : sSeamRestores) tRestore.run();
		sSeamRestores.clear();
		GT6RecipeMaps.reset();
	}

	private void pourSeams() {
		GT6RecipesOreChain.sMaterialItemResolver = MaterialTreeByproductViewTest::prefixItem;
		GT6RecipesShCL.sMaterialItemResolver = MaterialTreeByproductViewTest::prefixItem;
		GT6RecipesAnvil.sMaterialItemResolver = MaterialTreeByproductViewTest::prefixItem;
		GT6RecipesSifter.sMaterialItemResolver = MaterialTreeByproductViewTest::prefixItem;
		GT6RecipesOreChain.load();
		GT6RecipesShCL.load();
		GT6RecipesAnvil.load();
		GT6RecipesSifter.load();
	}

	/** Every state the R3 switch will cycle: the aggregate face, each single step, each cumulative cut. */
	private static List<ByproductView> allStates() {
		List<ByproductView> rStates = new ArrayList<>(List.of(ByproductView.ALL));
		for (int n = MaterialTreeDisplay.COL_ORE; n <= MaterialTreeDisplay.COL_DUST; n++) {
			rStates.add(ByproductView.step(n));
			rStates.add(ByproductView.cumulative(n));
		}
		return rStates;
	}

	/** The hand-built trio's hanging band: two step-0 slots, one step-2, one step-3, one step-less (-1) tail. */
	private static List<Byproduct> trioSlots() {
		return List.of(
				new Byproduct(ItemStack.EMPTY, true, "s0a", 0),
				new Byproduct(ItemStack.EMPTY, true, "s0b", 0),
				new Byproduct(ItemStack.EMPTY, true, "s2", 2),
				new Byproduct(ItemStack.EMPTY, true, "s3", 3),
				new Byproduct(ItemStack.EMPTY, true, "tail", -1));
	}

	/** The trio's chain: ore -> crushed -> ... -> dust on one lane. */
	private static List<Node> trioNodes() {
		return List.of(
				new Node(OP.oreRaw, MaterialTreeDisplay.COL_ORE, 0, ItemStack.EMPTY),
				new Node(OP.crushed, MaterialTreeDisplay.COL_CRUSHED, 0, ItemStack.EMPTY),
				new Node(OP.dust, MaterialTreeDisplay.COL_DUST, 0, ItemStack.EMPTY));
	}

	private static List<MaterialTreeDisplay.Edge> trioEdges() {
		return List.of(new MaterialTreeDisplay.Edge(OP.oreRaw, OP.dust, List.of("gt.recipe.anvil"), ItemStack.EMPTY));
	}

	// ------------------------------------------------------------------
	// determinism: the same state, the same output
	// ------------------------------------------------------------------

	@Test
	void viewSwitchIsDeterministic() {
		MaterialTreeBuilder tTree = MaterialTreeBuilder.build();
		MaterialTreeDisplay tFe = MaterialTreeDisplay.of(tTree, MT.Fe, MaterialTreeByproductViewTest::prefixItem, MaterialTreeByproductViewTest::machineStack);
		assertNotNull(tFe);
		assertFalse(tFe.byproducts().isEmpty(), "the fixture actually carries a hanging band");
		// ALL is the identity filter — the zero-change face the page legs ride
		assertEquals(tFe.byproducts(), ByproductView.ALL.apply(tFe.byproducts()), "ALL returns the band's own list");
		for (ByproductView tState : allStates()) {
			// the filter: same state, same visible set (compared on the reveal contract's own
			// face — label+step; cross-instance ItemStack equality is not load-bearing here)
			assertEquals(labels(tState.apply(tFe.byproducts())), labels(tState.apply(tFe.byproducts())), tState + " visible set twice");
			// the plan: same state, same geometry
			Result tFirst = MaterialTreeLayout.plan(tFe, tState);
			Result tSecond = MaterialTreeLayout.plan(tFe, tState);
			assertEquals(tFirst.width(), tSecond.width(), tState + " width twice");
			assertEquals(tFirst.height(), tSecond.height(), tState + " height twice");
			assertEquals(tFirst.byproductBand(), tSecond.byproductBand(), tState + " band twice");
			assertEquals(tFirst.edges(), tSecond.edges(), tState + " edge plans twice");
		}
		// the default alias IS the ALL face
		Result tDefault = MaterialTreeLayout.plan(tFe);
		Result tExplicit = MaterialTreeLayout.plan(tFe, ByproductView.ALL);
		assertEquals(tDefault.width(), tExplicit.width());
		assertEquals(tDefault.height(), tExplicit.height());
		assertEquals(tDefault.edges(), tExplicit.edges());
		// and a fresh pour of the same maps filters to the same sets
		GT6RecipeMaps.reset();
		pourSeams();
		MaterialTreeDisplay tFeAgain = MaterialTreeDisplay.of(MaterialTreeBuilder.build(), MT.Fe, MaterialTreeByproductViewTest::prefixItem, MaterialTreeByproductViewTest::machineStack);
		assertNotNull(tFeAgain);
		for (ByproductView tState : allStates())
			assertEquals(labels(tState.apply(tFe.byproducts())), labels(tState.apply(tFeAgain.byproducts())), tState + " visible set across fresh pours");
	}

	/** The reveal contract's comparable face: sourceLabel + step in the band's own order. */
	private static List<String> labels(List<Byproduct> aVisible) {
		List<String> rLabels = new ArrayList<>(aVisible.size());
		for (Byproduct tSlot : aVisible) rLabels.add(tSlot.sourceLabel() + "@" + tSlot.step());
		return rLabels;
	}

	// ------------------------------------------------------------------
	// the main axis is blind to the view (the 副产不挪主轴钉, every state)
	// ------------------------------------------------------------------

	@Test
	void stepViewNeverMovesTheMainAxis() {
		MaterialTreeBuilder tTree = MaterialTreeBuilder.build();
		for (MaterialTreeDisplay tDisplay : MaterialTreeDisplay.buildAll(tTree, MaterialTreeByproductViewTest::prefixItem, MaterialTreeByproductViewTest::machineStack)) {
			Result tAll = MaterialTreeLayout.plan(tDisplay);
			for (ByproductView tState : allStates()) {
				Result tViewed = MaterialTreeLayout.plan(tDisplay, tState);
				for (Node tNode : tDisplay.nodes()) {
					assertEquals(tAll.nodeX(tNode), tViewed.nodeX(tNode), tDisplay.material.mNameInternal + " " + tState + " chain lane: " + tNode.prefix().mNameInternal);
					assertEquals(tAll.nodeY(tNode), tViewed.nodeY(tNode), tDisplay.material.mNameInternal + " " + tState + " chain band: " + tNode.prefix().mNameInternal);
				}
				assertEquals(tAll.edges(), tViewed.edges(), tDisplay.material.mNameInternal + " " + tState + " edge plans byte-identical");
			}
		}
	}

	// ------------------------------------------------------------------
	// the cumulative reveal: the content box only grows
	// ------------------------------------------------------------------

	@Test
	void cumulativeContentBoxOnlyGrows() {
		List<Byproduct> tSlots = trioSlots();
		// the nested reveal: cumulative(n) == the union of step(0..n), order preserved
		List<Byproduct> tPrevious = List.of();
		for (int n = MaterialTreeDisplay.COL_ORE; n <= MaterialTreeDisplay.COL_DUST; n++) {
			List<Byproduct> tCut = ByproductView.cumulative(n).apply(tSlots);
			List<Byproduct> tUnion = new ArrayList<>();
			for (int j = MaterialTreeDisplay.COL_ORE; j <= n; j++) for (Byproduct tSlot : ByproductView.step(j).apply(tSlots))
				if (!tUnion.contains(tSlot)) tUnion.add(tSlot);
			assertEquals(tUnion, tCut, "cut " + n + " == the union of steps 0.." + n);
			assertTrue(tCut.size() >= tPrevious.size(), "cut " + n + " reveals at least what cut " + (n - 1) + " did");
			assertFalse(tCut.contains(tSlots.get(4)), "cut " + n + " never reveals the step-less tail");
			tPrevious = tCut;
		}
		// the boxes: width = the visible lane count's formula, climbing with n, inside ALL
		Result tAll = MaterialTreeLayout.plan(trioNodes(), trioEdges(), tSlots);
		int tPreviousWidth = 0;
		for (int n = MaterialTreeDisplay.COL_ORE; n <= MaterialTreeDisplay.COL_DUST; n++) {
			int tVisible = ByproductView.cumulative(n).apply(tSlots).size();
			Result tViewed = MaterialTreeLayout.plan(trioNodes(), trioEdges(), ByproductView.cumulative(n).apply(tSlots));
			assertTrue(tViewed.byproductBand(), "cut " + n + " keeps the hanging band");
			assertEquals(MaterialTreeDisplay.stageY(MaterialTreeDisplay.COL_BYPRODUCT) + 18 + 12, tViewed.height(), "cut " + n + " height = the hanging band's row");
			assertEquals(MaterialTreeDisplay.LANE_X0 + (tVisible - 1) * MaterialTreeDisplay.LANE_PITCH + 18 + 12, tViewed.width(),
					"cut " + n + " width = the content formula over " + tVisible + " visible slots");
			assertTrue(tViewed.width() >= tPreviousWidth, "cut " + n + " box only grows");
			assertTrue(tViewed.width() <= tAll.width() && tViewed.height() <= tAll.height(), "cut " + n + " inside the aggregate box");
			tPreviousWidth = tViewed.width();
		}
		// the aggregate face shows everything, tail included
		assertEquals(tSlots, ByproductView.ALL.apply(tSlots));
		assertEquals(tSlots.size(), ByproductView.cumulative(MaterialTreeDisplay.COL_DUST).apply(tSlots).size() + 1, "ALL = the last cut plus the tail");
	}

	@Test
	void emptyStepViewDropsTheBand() {
		// the trio has no step-1 slot: the step(1) reveal collapses the hanging band back
		// to the main axis instead of leaving a hole
		List<Byproduct> tEmpty = ByproductView.step(1).apply(trioSlots());
		assertTrue(tEmpty.isEmpty(), "the trio carries no step-1 slot");
		List<Byproduct> tSlots = trioSlots();
		Result tHole = MaterialTreeLayout.plan(trioNodes(), trioEdges(), ByproductView.step(1).apply(tSlots));
		assertFalse(tHole.byproductBand(), "an empty reveal drops the band");
		assertEquals(MaterialTreeDisplay.stageY(MaterialTreeDisplay.COL_DUST) + 18 + 12, tHole.height(), "height = the deepest chain band's row");
		// ...and the chain axis is still exactly the ALL face's
		Result tAll = MaterialTreeLayout.plan(trioNodes(), trioEdges(), tSlots);
		assertEquals(tAll.edges(), tHole.edges());
		// revealed slots pack consecutive lanes from the left — no reveal holes in a cut
		Result tCut = MaterialTreeLayout.plan(trioNodes(), trioEdges(), ByproductView.cumulative(MaterialTreeDisplay.COL_DUST).apply(tSlots));
		assertEquals(MaterialTreeDisplay.byproductX(0), tCut.byproductX(0));
		assertEquals(MaterialTreeDisplay.byproductX(3), tCut.byproductX(3));
	}

	// ------------------------------------------------------------------
	// the degenerate face: a no-byproduct tree under every state
	// ------------------------------------------------------------------

	@Test
	void degenerateTreeEveryStateIdentical() {
		List<Node> tNodes = List.of(
				new Node(OP.oreRaw, MaterialTreeDisplay.COL_ORE, 0, ItemStack.EMPTY),
				new Node(OP.dust, MaterialTreeDisplay.COL_DUST, 0, ItemStack.EMPTY));
		List<MaterialTreeDisplay.Edge> tEdges = List.of(new MaterialTreeDisplay.Edge(OP.oreRaw, OP.dust, List.of("gt.recipe.anvil"), ItemStack.EMPTY));
		for (ByproductView tState : allStates()) {
			Result tViewed = MaterialTreeLayout.plan(tNodes, tEdges, tState.apply(List.of()));
			assertFalse(tViewed.byproductBand(), tState + " keeps the band off a byproduct-less tree");
			assertEquals(MaterialTreeDisplay.LANE_X0 + 18 + 12, tViewed.width(), tState + " width = the one-lane box");
			assertEquals(MaterialTreeDisplay.stageY(MaterialTreeDisplay.COL_DUST) + 18 + 12, tViewed.height(), tState + " height = the two-band box");
			assertEquals(1, tViewed.edges().size(), tState + " keeps the edge");
		}
	}

	// ------------------------------------------------------------------
	// the live attribution: the step rides the producing row's input band
	// ------------------------------------------------------------------

	@Test
	void stepAttributionRidesTheInputBand() {
		MaterialTreeBuilder tTree = MaterialTreeBuilder.build();
		// Fe's whole offline face is the CRUSHING step: the derived Fe -> Fe2O3 redirect's
		// inputs are the ore family (oreRaw/blockRaw, both COL_ORE rows of the :153/:154
		// crusher templates) and the declared face IS that redirect's tail => every Fe slot
		// hangs on COL_ORE, so the step(0) reveal IS Fe's whole band
		MaterialTreeDisplay tFe = MaterialTreeDisplay.of(tTree, MT.Fe, MaterialTreeByproductViewTest::prefixItem, MaterialTreeByproductViewTest::machineStack);
		assertNotNull(tFe);
		assertFalse(tFe.byproducts().isEmpty());
		for (Byproduct tSlot : tFe.byproducts())
			assertEquals(MaterialTreeDisplay.COL_ORE, tSlot.step(), "the Fe slot " + tSlot.sourceLabel() + " hangs on the crushing step");
		assertEquals(tFe.byproducts(), ByproductView.step(MaterialTreeDisplay.COL_ORE).apply(tFe.byproducts()), "step(0) reveals Fe's whole band");
		// over the whole pour: every step lands in the displayed band range or the -1 tail
		for (MaterialTreeDisplay tDisplay : MaterialTreeDisplay.buildAll(tTree, MaterialTreeByproductViewTest::prefixItem, MaterialTreeByproductViewTest::machineStack))
			for (Byproduct tSlot : tDisplay.byproducts())
				assertTrue((tSlot.step() >= MaterialTreeDisplay.COL_ORE && tSlot.step() <= MaterialTreeDisplay.COL_DUST) || tSlot.step() == -1,
						tDisplay.material.mNameInternal + " slot step in range: " + tSlot);
	}
}
