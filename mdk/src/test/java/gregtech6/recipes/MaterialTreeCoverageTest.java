package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.recipes.tree.MaterialTreeBuilder;
import gregtech6.recipes.tree.MaterialTreeCoverage;
import gregtech6.recipes.tree.MaterialTreeCoverage.Band;
import gregtech6.recipes.tree.MaterialTreeCoverage.State;
import gregtech6.recipes.tree.MaterialTreeDisplay;
import gregtech6.recipes.tree.MaterialTreeDisplay.Edge;
import gregtech6.recipes.tree.MaterialTreeDisplay.Node;
import gregtech6.recipes.tree.MaterialTreeLayout;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTMaterialItems.PrefixMaterial;

/**
 * The coverage face pins (task mattree-r2-coverage-display): the {@link MaterialTreeCoverage}
 * pure read over the {@link MaterialTreeDisplay} model —
 * <ul>
 * <li><b>the three states</b> (the minimal determined semantics): entry nodes
 *     ({@code COL_ORE}) are COVERED (the mined start); a node whose displayed incoming edge
 *     carries its machine is COVERED (max over incoming — one machined producer suffices);
 *     a node whose every displayed producer is degraded is REACHABLE (the r11 sluice face:
 *     real data, workstation face inactive); a displayed node with no visible incoming path
 *     and no entry column is UNREACHABLE — the negative example;</li>
 * <li><b>the primitive seam degenerate trees</b> (the layout engine's list-trio precedent):
 *     the empty tree, the single entry node, the single orphan node, and the guard that an
 *     edge with an undisplayed endpoint is no visible path;</li>
 * <li><b>determinism</b>: two builds of the same pour yield identical coverage (content AND
 *     iteration order) — same display in, same states out;</li>
 * <li><b>monotonicity</b> (加路径只增不减): pouring the ore-chain loaders PROGRESSIVELY
 *     (OreChain → +ShCL → +Anvil → +Sifter) never downgrades a node state nor a band state —
 *     every state already reached is kept or upgraded;</li>
 * <li><b>the live offline faces</b> (per-leg — the WorkstationsTest fork): forge offline
 *     never fires registration so the default resolver is EMPTY everywhere → entries
 *     COVERED and the rest REACHABLE and NOTHING unreachable; the neo moddev harness DOES
 *     register → the real machines resolve → everything COVERED; the injected machined
 *     face turns every node COVERED; the mixed face pins the max-over-incoming
 *     rule;</li>
 * <li><b>the band aggregate</b>: counts per state, the STRONGEST node state as the band
 *     state (the monotone aggregate — worst-of would let a fresh degraded hop downgrade a
 *     covered band);</li>
 * <li><b>plan independence</b> (纯读不写): computing coverage does not move the layout —
 *     {@code plan()} before and after is edge-plan, canvas and node-coordinate identical,
 *     and the display's own lists are untouched.</li>
 * </ul>
 */
class MaterialTreeCoverageTest extends GTRecipesOfflineTestBase {

	/** The monotonicity ranking: COVERED &gt; REACHABLE &gt; UNREACHABLE. */
	private static int rank(State aState) {
		return aState == State.COVERED ? 2 : aState == State.REACHABLE ? 1 : 0;
	}

	// ------------------------------------------------------------------
	// the primitive seam: the three states + the degenerate trees
	// ------------------------------------------------------------------

	@Test
	void primitiveSeamDegenerateTrees() {
		// the empty tree: no states, no bands
		MaterialTreeCoverage tEmpty = MaterialTreeCoverage.of(List.of(), List.of());
		assertTrue(tEmpty.nodes().isEmpty(), "the empty tree has no node states");
		assertTrue(tEmpty.bands().isEmpty(), "the empty tree has no bands");

		// the single entry node: the mined start is COVERED, its band aggregated
		Node tEntry = new Node(OP.oreRaw, MaterialTreeDisplay.COL_ORE, 0, ItemStack.EMPTY);
		MaterialTreeCoverage tSingle = MaterialTreeCoverage.of(List.of(tEntry), List.of());
		assertEquals(State.COVERED, tSingle.node(OP.oreRaw), "the entry node's path is 「mine the world」");
		assertEquals(new Band(MaterialTreeDisplay.COL_ORE, 1, 0, 0), tSingle.band(MaterialTreeDisplay.COL_ORE));
		assertEquals(State.COVERED, tSingle.band(MaterialTreeDisplay.COL_ORE).state());
		assertNull(tSingle.band(MaterialTreeDisplay.COL_DUST), "a band without nodes is absent, not empty");

		// the negative example: a displayed non-entry node with no visible path is UNREACHABLE
		Node tOrphan = new Node(OP.dust, MaterialTreeDisplay.COL_DUST, 1, ItemStack.EMPTY);
		MaterialTreeCoverage tOrphans = MaterialTreeCoverage.of(List.of(tEntry, tOrphan), List.of());
		assertEquals(State.UNREACHABLE, tOrphans.node(OP.dust), "no displayed producer, no entry column: the gap face");
		assertEquals(State.UNREACHABLE, tOrphans.band(MaterialTreeDisplay.COL_DUST).state());

		// the state trio over one hop: degraded → REACHABLE, machined → COVERED (entry stays COVERED)
		Edge tDegraded = new Edge(OP.oreRaw, OP.dust, List.of("gt.recipe.sluice"), ItemStack.EMPTY);
		assertEquals(State.REACHABLE, MaterialTreeCoverage.of(List.of(tEntry, tOrphan), List.of(tDegraded)).node(OP.dust),
				"a real hop with an untabled workstation face: reachable, not covered");
		Edge tMachined = new Edge(OP.oreRaw, OP.dust, List.of("gt.recipe.crusher"), new ItemStack(Items.BARRIER));
		MaterialTreeCoverage tCovered = MaterialTreeCoverage.of(List.of(tEntry, tOrphan), List.of(tMachined));
		assertEquals(State.COVERED, tCovered.node(OP.dust), "a machined producer is the known-path face");
		assertEquals(State.COVERED, tCovered.node(OP.oreRaw), "the entry stays COVERED regardless of its outgoing hops");

		// the guard: an edge with an UNDISPLAYED endpoint is not a visible path
		Node tShown = new Node(OP.crushed, MaterialTreeDisplay.COL_CRUSHED, 0, ItemStack.EMPTY);
		Edge tGhost = new Edge(OP.ore, OP.crushed, List.of("gt.recipe.crusher"), new ItemStack(Items.BARRIER));
		assertEquals(State.UNREACHABLE, MaterialTreeCoverage.of(List.of(tShown), List.of(tGhost)).node(OP.crushed),
				"the producer is not on the tree — no visible path, the honest gap face");

		// absence is not UNREACHABLE: a prefix off the tree has no state at all
		assertNull(tSingle.node(OP.dust), "not on the tree = no state (absence ≠ unreachable)");
	}

	// ------------------------------------------------------------------
	// determinism: same pour, same coverage, same iteration order
	// ------------------------------------------------------------------

	@Test
	void determinismSamePourSameCoverage() {
		MaterialTreeDisplay tDisplayA = ironDisplay();
		MaterialTreeDisplay tDisplayB = ironDisplay(); // a second build() over the same deterministic pour
		MaterialTreeCoverage tCoverageA = MaterialTreeCoverage.of(tDisplayA);
		MaterialTreeCoverage tCoverageB = MaterialTreeCoverage.of(tDisplayB);
		List<Map.Entry<OreDictPrefix, State>> tEntriesA = new ArrayList<>(tCoverageA.nodes().entrySet());
		List<Map.Entry<OreDictPrefix, State>> tEntriesB = new ArrayList<>(tCoverageB.nodes().entrySet());
		assertEquals(tEntriesA, tEntriesB, "same display, same states in the same order");
		assertEquals(tCoverageA.bands(), tCoverageB.bands(), "same display, same band aggregates in the same order");
		assertTrue(!tEntriesA.isEmpty(), "the iron display carries states");
	}

	// ------------------------------------------------------------------
	// monotonicity: adding loaders (paths) only upgrades, never downgrades
	// ------------------------------------------------------------------

	@Test
	void monotonicityProgressivePoursOnlyUpgrade() {
		OreDictMaterial[] tMaterials = {MT.Fe, MT.Cu};
		List<Runnable> tGens = new ArrayList<>();
		tGens.add(GT6RecipesOreChain::load);
		tGens.add(GT6RecipesShCL::load);
		tGens.add(GT6RecipesAnvil::load);
		tGens.add(GT6RecipesSifter::load);
		Map<OreDictMaterial, MaterialTreeCoverage> tPrev = new LinkedHashMap<>();
		for (int tGen = 1; tGen <= tGens.size(); tGen++) {
			GT6RecipeMaps.reset();
			for (int i = 0; i < tGen; i++) tGens.get(i).run(); // the pour ACCUMULATES: gen g = loaders 1..g (the pin's 「adding loaders」 premise)
			MaterialTreeBuilder tTree = MaterialTreeBuilder.build();
			Map<OreDictMaterial, MaterialTreeCoverage> tNext = new LinkedHashMap<>();
			for (OreDictMaterial tMaterial : tMaterials) {
				MaterialTreeDisplay tDisplay = MaterialTreeDisplay.of(tTree, tMaterial, MaterialTreeCoverageTest::prefixItem);
				// absence before the material's chain generation is LEGAL data (Fe's crusher row is
				// the cross-material Fe2O3 redirect — its same-material hops only appear with the
				// anvil selfcrush) — the monotone pin binds once the tree EXISTS: never dropped,
				// never downgraded
				if (tDisplay == null) continue;
				tNext.put(tMaterial, MaterialTreeCoverage.of(tDisplay));
			}
			for (Map.Entry<OreDictMaterial, MaterialTreeCoverage> tOld : tPrev.entrySet()) {
				MaterialTreeCoverage tNow = tNext.get(tOld.getKey());
				assertNotNull(tNow, "once a material's tree exists it never disappears (gen " + tGen + ", " + tOld.getKey().mNameInternal + ")");
				for (Map.Entry<OreDictPrefix, State> tNode : tOld.getValue().nodes().entrySet()) {
					State tNowState = tNow.node(tNode.getKey());
					assertNotNull(tNowState, "adding paths never drops a node from the tree: " + tNode.getKey().mNameInternal);
					assertTrue(rank(tNowState) >= rank(tNode.getValue()),
							"node " + tNode.getKey().mNameInternal + " of " + tOld.getKey().mNameInternal
									+ ": " + tNode.getValue() + " → " + tNowState + " must not downgrade");
				}
				for (Band tBand : tOld.getValue().bands()) {
					Band tNowBand = tNow.band(tBand.column());
					assertNotNull(tNowBand, "adding paths never empties a band: band " + tBand.column());
					assertTrue(rank(tNowBand.state()) >= rank(tBand.state()),
							"band " + tBand.column() + " of " + tOld.getKey().mNameInternal
									+ ": " + tBand.state() + " → " + tNowBand.state() + " must not downgrade");
				}
			}
			tPrev = tNext;
		}
	}

	// ------------------------------------------------------------------
	// the live offline faces: degraded default, machined, mixed
	// ------------------------------------------------------------------

	@Test
	void liveOfflineFacesEntryCoveredRestReachableNothingUnreachable() {
		// the DEFAULT resolver face is PER-LEG (the WorkstationsTest harness fork): forge
		// never fires RegisterEvent offline → every workstation handle unbound → every edge
		// degraded → entries COVERED (the mined start), everything else REACHABLE; the neo
		// moddev harness DOES fire registration → the real chain machines resolve → the
		// machined face everywhere. Either leg: the negative face is ABSENT (the pour never
		// displays a pathless node).
		MaterialTreeDisplay tDisplay = ironDisplay();
		MaterialTreeCoverage tCoverage = MaterialTreeCoverage.of(tDisplay);
		for (Node tNode : tDisplay.nodes()) {
			State tState = tCoverage.node(tNode.prefix());
			assertNotNull(tState, "every displayed node has a state: " + tNode.prefix().mNameInternal);
			//? if forge {
			if (tNode.column() == MaterialTreeDisplay.COL_ORE)
				assertEquals(State.COVERED, tState, "entry " + tNode.prefix().mNameInternal + " is the mined start");
			else
				assertEquals(State.REACHABLE, tState, "offline every workstation face is inactive: " + tNode.prefix().mNameInternal);
			//?} else {
			/*assertEquals(State.COVERED, tState, "the neo harness resolves the real machines: " + tNode.prefix().mNameInternal);
			*///?}
		}
		assertEquals(State.COVERED, tCoverage.band(MaterialTreeDisplay.COL_ORE).state());
		//? if forge {
		assertEquals(State.REACHABLE, tCoverage.band(MaterialTreeDisplay.COL_CRUSHED).state());
		//?} else {
		/*assertEquals(State.COVERED, tCoverage.band(MaterialTreeDisplay.COL_CRUSHED).state());
		*///?}
		for (Band tBand : tCoverage.bands())
			assertEquals(0, tBand.unreachable(), "no unreachable node in band " + tBand.column() + " (the negative face is absent from the pour)");
	}

	@Test
	void machinedFaceCoversEverythingAndMixedFacePinsMaxOverIncoming() {
		MaterialTreeBuilder tTree = MaterialTreeBuilder.build();
		// the machined face: every map resolves → every hop renders its machine → all COVERED
		MaterialTreeDisplay tIronAll = MaterialTreeDisplay.of(tTree, MT.Fe, MaterialTreeCoverageTest::prefixItem, aName -> new ItemStack(Items.BARRIER));
		MaterialTreeCoverage tAll = MaterialTreeCoverage.of(tIronAll);
		for (Map.Entry<OreDictPrefix, State> tNode : tAll.nodes().entrySet())
			assertEquals(State.COVERED, tNode.getValue(), "a machined producer everywhere covers " + tNode.getKey().mNameInternal);
		for (Band tBand : tAll.bands())
			assertEquals(State.COVERED, tBand.state(), "band " + tBand.column() + " fully covered");
		// the mixed face: ONLY the anvil resolves — dust Fe has the anvil mortar leg among its
		// producers → COVERED (max over incoming), while Cu's crushedPurified rides the sifter
		// alone → still REACHABLE
		MaterialTreeDisplay tIronAnvil = MaterialTreeDisplay.of(tTree, MT.Fe, MaterialTreeCoverageTest::prefixItem,
				aName -> "gt.recipe.anvil".equals(aName) ? new ItemStack(Items.BARRIER) : ItemStack.EMPTY);
		assertEquals(State.COVERED, MaterialTreeCoverage.of(tIronAnvil).node(OP.dust),
				"one machined producer among the degraded ones suffices (max over incoming)");
		MaterialTreeDisplay tCuAnvil = MaterialTreeDisplay.of(tTree, MT.Cu, MaterialTreeCoverageTest::prefixItem,
				aName -> "gt.recipe.anvil".equals(aName) ? new ItemStack(Items.BARRIER) : ItemStack.EMPTY);
		assertEquals(State.REACHABLE, MaterialTreeCoverage.of(tCuAnvil).node(OP.crushedPurified),
				"the landed sifter hop is the only producer: reachable, not covered");
	}

	// ------------------------------------------------------------------
	// the band aggregate: counts + the strongest-state rule
	// ------------------------------------------------------------------

	@Test
	void bandAggregatesCountsAndTheStrongestState() {
		// entry → machined hop to dust lane 0, degraded hop to dust lane 1: the dust band
		// holds one COVERED + one REACHABLE node and its own state stays COVERED
		Node tEntry = new Node(OP.oreRaw, MaterialTreeDisplay.COL_ORE, 0, ItemStack.EMPTY);
		Node tDustA = new Node(OP.dust, MaterialTreeDisplay.COL_DUST, 0, ItemStack.EMPTY);
		Node tDustB = new Node(OP.dustTiny, MaterialTreeDisplay.COL_DUST, 1, ItemStack.EMPTY);
		List<Edge> tEdges = List.of(
				new Edge(OP.oreRaw, OP.dust, List.of("gt.recipe.crusher"), new ItemStack(Items.BARRIER)),
				new Edge(OP.oreRaw, OP.dustTiny, List.of("gt.recipe.sluice"), ItemStack.EMPTY));
		MaterialTreeCoverage tCoverage = MaterialTreeCoverage.of(List.of(tEntry, tDustA, tDustB), tEdges);
		assertEquals(new Band(MaterialTreeDisplay.COL_DUST, 1, 1, 0), tCoverage.band(MaterialTreeDisplay.COL_DUST),
				"the counts face: one covered, one reachable");
		assertEquals(State.COVERED, tCoverage.band(MaterialTreeDisplay.COL_DUST).state(),
				"the band state is the STRONGEST node state (the monotone aggregate)");
	}

	// ------------------------------------------------------------------
	// plan independence: the pure read never moves the layout (纯读不写)
	// ------------------------------------------------------------------

	@Test
	void planIndependenceCoverageNeverMovesTheLayout() {
		MaterialTreeDisplay tDisplay = ironDisplay();
		MaterialTreeLayout.Result tBefore = MaterialTreeLayout.plan(tDisplay);
		MaterialTreeCoverage tCoverage = MaterialTreeCoverage.of(tDisplay);
		MaterialTreeLayout.Result tAfter = MaterialTreeLayout.plan(tDisplay);
		assertEquals(tBefore.edges(), tAfter.edges(), "the edge plans are byte-identical across the coverage read");
		assertEquals(tBefore.width(), tAfter.width(), "the content box width is untouched");
		assertEquals(tBefore.height(), tAfter.height(), "the content box height is untouched");
		for (Node tNode : tDisplay.nodes()) {
			assertEquals(tBefore.nodeX(tNode), tAfter.nodeX(tNode), "node x untouched");
			assertEquals(tBefore.nodeY(tNode), tAfter.nodeY(tNode), "node y untouched");
		}
		// and the display's own lists never mutated
		assertEquals(tDisplay.nodes().size(), tCoverage.nodes().size(), "one state per displayed node");
		assertTrue(!tCoverage.nodes().isEmpty() && !tDisplay.edges().isEmpty(), "the iron display has both");
	}

	// ------------------------------------------------------------------
	// the offline fixture (the DisplayTest pattern, mirrored)
	// ------------------------------------------------------------------

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

	/** The (prefix, material) -> probe-item resolver armed into the four ore-chain loaders (the gt6 namespace is load-bearing). */
	private static Item prefixItem(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		if (aPrefix == null || aMaterial == null) return null;
		return PREFIX_ITEMS.computeIfAbsent(new PrefixMaterial(aPrefix, aMaterial), aPair ->
			Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath("gt6", "mtree_coverage_probe_" + sNextProbeId++),
				new MaterialPrefixItem(new Item.Properties(), aPair.prefix(), aPair.material())));
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
		GT6RecipeMaps.reset(); // deterministic slate regardless of sibling-class order (the isolation lesson)
		capture(() -> GT6RecipesOreChain.sMaterialItemResolver, aV -> GT6RecipesOreChain.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesShCL.sMaterialItemResolver, aV -> GT6RecipesShCL.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesAnvil.sMaterialItemResolver, aV -> GT6RecipesAnvil.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesSifter.sMaterialItemResolver, aV -> GT6RecipesSifter.sMaterialItemResolver = aV);
		GT6RecipesOreChain.sMaterialItemResolver = MaterialTreeCoverageTest::prefixItem;
		GT6RecipesShCL.sMaterialItemResolver = MaterialTreeCoverageTest::prefixItem;
		GT6RecipesAnvil.sMaterialItemResolver = MaterialTreeCoverageTest::prefixItem;
		GT6RecipesSifter.sMaterialItemResolver = MaterialTreeCoverageTest::prefixItem;
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

	/** The Iron display over the full four-loader pour (the ScreenTest form). */
	private static MaterialTreeDisplay ironDisplay() {
		MaterialTreeBuilder tTree = MaterialTreeBuilder.build();
		MaterialTreeDisplay tFe = MaterialTreeDisplay.of(tTree, MT.Fe, MaterialTreeCoverageTest::prefixItem);
		assertNotNull(tFe, "the iron ore walk yields a display");
		return tFe;
	}
}
