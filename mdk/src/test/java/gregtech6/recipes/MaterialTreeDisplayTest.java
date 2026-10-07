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
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.emi.GT6MaterialTreeEmiCategory;
import gregtech6.emi.GT6MaterialTreeEmiRecipe;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.jei.GT6MaterialTreeJeiCategory;
import gregtech6.recipes.tree.MaterialTreeBuilder;
import gregtech6.recipes.tree.MaterialTreeDisplay;
import gregtech6.recipes.tree.MaterialTreeDisplay.Byproduct;
import gregtech6.recipes.tree.MaterialTreeDisplay.Edge;
import gregtech6.recipes.tree.MaterialTreeDisplay.Node;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTMaterialItems.PrefixMaterial;

/**
 * The material-tree display census (task debt-material-tree-b acceptance): the display
 * assembly over the LIVE ore-chain pour, with the same prefix-identity probe fixture as
 * {@link MaterialTreeBuilderTest} (one MaterialPrefixItem per referenced pair, lazily
 * registered into the offline-opened vanilla registry). The pour arms exactly the maps the
 * ore chain touches — the three ore-chain loaders PLUS the LANDED :351 DUST_ORE sifting
 * walk (task debt-ore-purified-edge; {@code MaterialTreeBuilder.build()} full-spectrum scans
 * {@code RecipeMap.RECIPE_MAPS} at runtime, so the fixture must pour every map the census
 * pins — the OreByproductInfoTest precedent: bare Sifter, no Canner arm).
 *
 * <ul>
 * <li><b>Layout table census (acceptance ①)</b>: every pinned prefix lands in its
 *     contracted column (ore/blockRaw → 0, crushed family → 1, purified/centrifuged → 2,
 *     dust family → 3), the {@code ore*} wildcard holds, and non-chain prefixes are
 *     excluded (-1 — the table is the ore-chain filter).</li>
 * <li><b>Iron display (acceptance ①, the Fe 离轴 reality)</b>: the six nodes the pour
 *     actually reaches (oreRaw/crushed/crushedTiny/dust/dustTiny/dustDiv72) — crushedPurified
 *     keeps a table column but NO node because Fe is OFF the sifting walk's material axis
 *     (the landed producers only serve the 53 on-axis materials), the anvil
 *     + shredder edge labels, and the two-face byproduct merge: the DERIVED crusher
 *     redirect (Fe → crushed Fe2O3, MT.java:2885) first, then the DECLARED face of the
 *     crushing target Fe2O3 (MT.java:3820: Ilmenite, GraniticMineralSand, MnO2, ClayRed).</li>
 * <li><b>Copper display (the Cu 在轴 landed shape)</b>: the four DUST_ORE sifting inputs
 *     (oreGravel/Sand/RedSand/Mud) join the ore column, crushedPurified OCCUPIES its
 *     COL_PURIFIED column via the {@code gt.recipe.sifter} edge, and the byproduct column is
 *     two-face — the derived sifting tinies (Cobaltite/Au/Ni) first, then the declared
 *     remainder (Malachite, As; MT.java:3827 order).</li>
 * <li><b>Both-viewer faces (acceptance ①双腿注册)</b>: the JEI category and the EMI
 *     category/recipe construct offline with the ONE shared id {@code gt6:material_tree}
 *     (the JEMI balance), and the EMI recipe's input/output split mirrors the JEI slot
 *     split (ore inputs, downstream + byproduct outputs).</li>
 * </ul>
 */
class MaterialTreeDisplayTest extends GTRecipesOfflineTestBase {

	/** One lazily created probe item per referenced (prefix, material) pair — the identity seam, offline. */
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
			Registry.register(BuiltInRegistries.ITEM, new ResourceLocation("gt6", "mtree_display_probe_" + sNextProbeId++),
				new MaterialPrefixItem(new Item.Properties(), aPair.prefix(), aPair.material())));
	}

	/** The offline item-registry unlock (the OMComponentFaceTest.probeItem walk, the tree-a hoisted form). */
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
		GT6RecipeMaps.reset(); // deterministic slate regardless of sibling-class order (the isolation lesson)
		capture(() -> GT6RecipesOreChain.sMaterialItemResolver, aV -> GT6RecipesOreChain.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesShCL.sMaterialItemResolver, aV -> GT6RecipesShCL.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesAnvil.sMaterialItemResolver, aV -> GT6RecipesAnvil.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesSifter.sMaterialItemResolver, aV -> GT6RecipesSifter.sMaterialItemResolver = aV);
		GT6RecipesOreChain.sMaterialItemResolver = MaterialTreeDisplayTest::prefixItem;
		GT6RecipesShCL.sMaterialItemResolver = MaterialTreeDisplayTest::prefixItem;
		GT6RecipesAnvil.sMaterialItemResolver = MaterialTreeDisplayTest::prefixItem;
		GT6RecipesSifter.sMaterialItemResolver = MaterialTreeDisplayTest::prefixItem;
		GT6RecipesOreChain.load(); // the Crusher ore chain (:153 oreRaw, :154 blockRaw, the Fe2O3 redirect face)
		GT6RecipesShCL.load(); // the Shredder terminal legs (:138-143 crushed*/crushedPurified/crushedCentrifuged -> dust*)
		GT6RecipesAnvil.load(); // the Anvil selfcrush + mortar rows (:158-172)
		GT6RecipesSifter.load(); // the LANDED :351 DUST_ORE walk (debt-ore-purified-edge; the OreByproductInfoTest precedent — bare Sifter, no Canner arm)
	}

	@AfterEach
	void restoreSeamsAndDropGeneration() {
		for (Runnable tRestore : sSeamRestores) tRestore.run();
		sSeamRestores.clear();
		GT6RecipeMaps.reset();
	}

	// ------------------------------------------------------------------
	// acceptance ①: the layout table census
	// ------------------------------------------------------------------

	@Test
	void layoutTableCensus() {
		assertEquals(MaterialTreeDisplay.COL_ORE, MaterialTreeDisplay.columnOf(OP.oreRaw));
		assertEquals(MaterialTreeDisplay.COL_ORE, MaterialTreeDisplay.columnOf(OP.ore)); // the wildcard face
		assertEquals(MaterialTreeDisplay.COL_ORE, MaterialTreeDisplay.columnOf(OP.blockRaw));
		assertEquals(MaterialTreeDisplay.COL_CRUSHED, MaterialTreeDisplay.columnOf(OP.crushed));
		assertEquals(MaterialTreeDisplay.COL_CRUSHED, MaterialTreeDisplay.columnOf(OP.crushedTiny));
		assertEquals(MaterialTreeDisplay.COL_PURIFIED, MaterialTreeDisplay.columnOf(OP.crushedPurified));
		assertEquals(MaterialTreeDisplay.COL_PURIFIED, MaterialTreeDisplay.columnOf(OP.crushedCentrifuged));
		assertEquals(MaterialTreeDisplay.COL_DUST, MaterialTreeDisplay.columnOf(OP.dust));
		assertEquals(MaterialTreeDisplay.COL_DUST, MaterialTreeDisplay.columnOf(OP.dustTiny));
		assertEquals(MaterialTreeDisplay.COL_DUST, MaterialTreeDisplay.columnOf(OP.dustDiv72));
		// the table is the ore-chain filter: the item-tree tail never leaks into the diagram
		assertEquals(-1, MaterialTreeDisplay.columnOf(OP.ingot));
		assertEquals(-1, MaterialTreeDisplay.columnOf(OP.plate));
		assertEquals(-1, MaterialTreeDisplay.columnOf(OP.gem));
		// geometry sanity (the vertical rotation): the deepest band's slot row and the widest
		// lane's slot column both fit the fixed category box
		assertTrue(MaterialTreeDisplay.byproductX(MaterialTreeDisplay.MAX_ROWS - 1) + 18 <= MaterialTreeDisplay.WIDTH);
		assertTrue(MaterialTreeDisplay.stageY(MaterialTreeDisplay.COL_BYPRODUCT) + 18 <= MaterialTreeDisplay.HEIGHT);
	}

	// ------------------------------------------------------------------
	// acceptance ①: the Iron display — Fe 离轴 + the two-face merge
	// ------------------------------------------------------------------

	@Test
	void ironDisplayTwoFaces() {
		MaterialTreeBuilder tTree = MaterialTreeBuilder.build();
		MaterialTreeDisplay tDisplay = MaterialTreeDisplay.of(tTree, MT.Fe, MaterialTreeDisplayTest::prefixItem);
		assertNotNull(tDisplay, "Iron's ore walk must yield a display");

		// the six nodes Fe actually reaches — unchanged by the LANDED producer rows (task
		// debt-ore-purified-edge): Fe is OFF the sifting walk's material axis (the twelve MT
		// setCrushing redirecters are all off-axis, MaterialTreeBuilderTest's ironChainSpineCensus
		// pins the same negative), so crushedPurified keeps its table column but NO Fe node —
		// only the terminal ShCL leg exists for Fe
		Map<OreDictPrefix, Node> tNodes = new HashMap<>();
		for (Node tNode : tDisplay.nodes()) tNodes.put(tNode.prefix(), tNode);
		assertEquals(6, tDisplay.nodes().size(), "node census: " + tDisplay.nodes());
		assertTrue(tNodes.containsKey(OP.oreRaw) && tNodes.containsKey(OP.crushed) && tNodes.containsKey(OP.crushedTiny)
				&& tNodes.containsKey(OP.dust) && tNodes.containsKey(OP.dustTiny) && tNodes.containsKey(OP.dustDiv72),
				"node set: " + tNodes.keySet());
		assertFalse(tNodes.containsKey(OP.crushedPurified),
				"crushedPurified has a table column but no node for the OFF-axis Fe");
		assertEquals(MaterialTreeDisplay.COL_ORE, tNodes.get(OP.oreRaw).column());
		assertEquals(MaterialTreeDisplay.COL_CRUSHED, tNodes.get(OP.crushed).column());
		assertEquals(MaterialTreeDisplay.COL_CRUSHED, tNodes.get(OP.crushedTiny).column());
		assertEquals(MaterialTreeDisplay.COL_DUST, tNodes.get(OP.dust).column());
		assertEquals(MaterialTreeDisplay.COL_DUST, tNodes.get(OP.dustTiny).column());
		assertEquals(MaterialTreeDisplay.COL_DUST, tNodes.get(OP.dustDiv72).column());

		// the edge labels: the anvil selfcrush hop and the shared crushed->dust leg (anvil mortar
		// :167 AND shredder :138 both pour same-material rows)
		Edge tOreEdge = edgeOf(tDisplay, OP.oreRaw, OP.crushed);
		assertNotNull(tOreEdge, "oreRaw -> crushed edge");
		assertTrue(tOreEdge.mapNames().contains("gt.recipe.anvil"), "anvil label: " + tOreEdge.mapNames());
		assertNotNull(edgeOf(tDisplay, OP.oreRaw, OP.crushedTiny), "the anvil companion leg");
		Edge tDustEdge = edgeOf(tDisplay, OP.crushed, OP.dust);
		assertNotNull(tDustEdge, "crushed -> dust edge");
		assertTrue(tDustEdge.mapNames().contains("gt.recipe.shredder") && tDustEdge.mapNames().contains("gt.recipe.anvil"),
				"both producers label the dust leg: " + tDustEdge.mapNames());
		assertTrue(edgeOf(tDisplay, OP.crushedTiny, OP.dustDiv72) != null || edgeOf(tDisplay, OP.crushed, OP.dustDiv72) != null,
				"a dustDiv72 chance edge exists");

		// the byproduct two-face merge: derived (the crusher redirect Fe -> crushed Fe2O3,
		// MT.java:2885 setCrushing) first, then the DECLARED face of the crushing target
		// Fe2O3 (MT.java:3820) — capped at MAX_ROWS, which the 1+4 exactly fills
		List<Byproduct> tByproducts = tDisplay.byproducts();
		assertEquals(5, tByproducts.size(), "byproduct census: " + tByproducts);
		Byproduct tDerived = tByproducts.get(0);
		assertTrue(tDerived.derived(), "the first byproduct is the derived crusher redirect");
		assertEquals(prefixItem(OP.crushed, MT.Fe2O3), tDerived.stack().getItem(), "crushed Hematite stack");
		OreDictMaterial[] tDeclared = {MT.OREMATS.Ilmenite, MT.OREMATS.GraniticMineralSand, MT.MnO2, MT.ClayRed};
		for (int i = 0; i < tDeclared.length; i++) {
			Byproduct tByproduct = tByproducts.get(1 + i);
			assertFalse(tByproduct.derived(), "entries 1..4 are the declared face");
			assertEquals(MaterialTreeDisplay.DECLARED_TEXT, tByproduct.sourceLabel());
			assertEquals(prefixItem(OP.dust, tDeclared[i]), tByproduct.stack().getItem(),
					"declared byproduct " + tDeclared[i].mNameInternal + " shows its dust");
		}
	}

	// ------------------------------------------------------------------
	// acceptance ①: the Copper display — Cu 在轴, the LANDED sifter shape
	// ------------------------------------------------------------------

	@Test
	void copperDisplayLandedSifter() {
		MaterialTreeBuilder tTree = MaterialTreeBuilder.build();
		MaterialTreeDisplay tDisplay = MaterialTreeDisplay.of(tTree, MT.Cu, MaterialTreeDisplayTest::prefixItem);
		assertNotNull(tDisplay, "Copper's ore walk must yield a display");

		// the LANDED node census (debt-ore-purified-edge): the four DUST_ORE sifting inputs join
		// oreRaw in the wildcard ore* column (exactly the MAX_ROWS cap) and crushedPurified now
		// OCCUPIES its COL_PURIFIED column — the old "column with no node" gap is closed
		Map<OreDictPrefix, Node> tNodes = new HashMap<>();
		for (Node tNode : tDisplay.nodes()) tNodes.put(tNode.prefix(), tNode);
		assertEquals(12, tDisplay.nodes().size(), "node census: " + tDisplay.nodes());
		assertTrue(tNodes.containsKey(OP.oreRaw) && tNodes.containsKey(OP.oreGravel) && tNodes.containsKey(OP.oreSand)
				&& tNodes.containsKey(OP.oreRedSand) && tNodes.containsKey(OP.oreMud),
				"the ore column holds oreRaw + the four DUST_ORE sifting inputs: " + tNodes.keySet());
		assertEquals(MaterialTreeDisplay.COL_ORE, tNodes.get(OP.oreRaw).column());
		assertEquals(MaterialTreeDisplay.COL_CRUSHED, tNodes.get(OP.crushed).column());
		assertEquals(MaterialTreeDisplay.COL_CRUSHED, tNodes.get(OP.crushedTiny).column());
		assertEquals(MaterialTreeDisplay.COL_PURIFIED, tNodes.get(OP.crushedPurified).column(),
				"crushedPurified occupies the purified column via the landed :351 rows");
		assertEquals(MaterialTreeDisplay.COL_DUST, tNodes.get(OP.dust).column());

		// the on-axis crusher face (unchanged): Copper's crusher hop is a SAME-material chain
		// edge (OreChain :153), so the crusher label rides the CHAIN edge, not a byproduct edge
		Edge tOreEdge = edgeOf(tDisplay, OP.oreRaw, OP.crushed);
		assertNotNull(tOreEdge, "oreRaw -> crushed edge");
		assertTrue(tOreEdge.mapNames().contains("gt.recipe.crusher"), "crusher label: " + tOreEdge.mapNames());
		assertNotNull(edgeOf(tDisplay, OP.crushed, OP.dust), "crushed -> dust edge");

		// the landed producer edge (the :351 sifting walk, gt.recipe.sifter): the ore-BLOCK
		// input sifting rows are the crushedPurified producers — the edge label names the map
		Edge tPurifiedEdge = edgeOf(tDisplay, OP.oreGravel, OP.crushedPurified);
		assertNotNull(tPurifiedEdge, "the landed oreGravel -> crushedPurified edge");
		assertTrue(tPurifiedEdge.mapNames().contains("gt.recipe.sifter"), "sifter label: " + tPurifiedEdge.mapNames());

		// the byproduct column is now TWO-face: the sifting rows' tiny outputs are the DERIVED
		// face (Cu -> Cobaltite/Au/Ni dustTiny, the :331-333 first-three tiers of MT.java:3827),
		// then the declared face continues with what the derived rows do not already show —
		// the 4th+ declared byproducts (Malachite, As) fill up to the MAX_ROWS cap
		List<Byproduct> tByproducts = tDisplay.byproducts();
		assertEquals(5, tByproducts.size(), "byproduct census: " + tByproducts);
		OreDictMaterial[] tDerived = {MT.OREMATS.Cobaltite, MT.Au, MT.Ni};
		for (int i = 0; i < tDerived.length; i++) {
			Byproduct tByproduct = tByproducts.get(i);
			assertTrue(tByproduct.derived(), "entries 0..2 are the derived sifting tinies: " + tByproducts);
			assertEquals(prefixItem(OP.dustTiny, tDerived[i]), tByproduct.stack().getItem(),
					"derived sifting tiny " + tDerived[i].mNameInternal);
		}
		OreDictMaterial[] tDeclaredTail = {MT.OREMATS.Malachite, MT.As};
		for (int i = 0; i < tDeclaredTail.length; i++) {
			Byproduct tByproduct = tByproducts.get(3 + i);
			assertFalse(tByproduct.derived(), "entries 3..4 are the declared remainder");
			assertEquals(MaterialTreeDisplay.DECLARED_TEXT, tByproduct.sourceLabel());
			assertEquals(prefixItem(OP.dust, tDeclaredTail[i]), tByproduct.stack().getItem(),
					"declared byproduct " + tDeclaredTail[i].mNameInternal);
		}
	}

	// ------------------------------------------------------------------
	// acceptance ①: both-viewer registration faces, offline-constructed
	// ------------------------------------------------------------------

	@Test
	void bothViewerFacesConstructOffline() {
		assertTrue(MaterialTreeDisplay.SHOWN, "the show switch defaults on (the hideOreProcessingDiagrams precedent)");

		// JEI leg: the category's uid is the shared id, geometry from the shared model
		GT6MaterialTreeJeiCategory tCategory = new GT6MaterialTreeJeiCategory();
		assertEquals(ResourceLocation.fromNamespaceAndPath("gt6", MaterialTreeDisplay.CATEGORY_UID_PATH), tCategory.getRecipeType().getUid());
		assertEquals(MaterialTreeDisplay.WIDTH, tCategory.getWidth());
		// nav-m3-jei: the category carries the EMI twin's 20px control strip below the canvas
		assertEquals(MaterialTreeDisplay.HEIGHT + GT6MaterialTreeEmiRecipe.CONTROL_STRIP_H, tCategory.getHeight());

		// EMI leg: same category id (the JEMI balance — both twins ship in this card), and the
		// Fe recipe's input/output split mirrors the JEI slot split
		assertEquals(ResourceLocation.fromNamespaceAndPath("gt6", MaterialTreeDisplay.CATEGORY_UID_PATH), GT6MaterialTreeEmiCategory.id());
		MaterialTreeBuilder tTree = MaterialTreeBuilder.build();
		MaterialTreeDisplay tFe = MaterialTreeDisplay.of(tTree, MT.Fe, MaterialTreeDisplayTest::prefixItem);
		GT6MaterialTreeEmiRecipe tRecipe = new GT6MaterialTreeEmiRecipe(tFe);
		assertEquals(ResourceLocation.fromNamespaceAndPath("gt6", MaterialTreeDisplay.CATEGORY_UID_PATH + "/iron"), tRecipe.getId());
		assertEquals(GT6MaterialTreeEmiCategory.INSTANCE, tRecipe.getCategory());
		assertFalse(tRecipe.getInputs().isEmpty(), "the ore column rides inputs (U reachability)");
		assertTrue(tRecipe.getInputs().stream().anyMatch(tIn -> tIn.getEmiStacks().get(0).getItemStack().getItem() == prefixItem(OP.oreRaw, MT.Fe)),
				"the raw ore item is an input");
		assertTrue(tRecipe.getOutputs().stream().anyMatch(tOut -> tOut.getItemStack().getItem() == prefixItem(OP.crushed, MT.Fe)),
				"the crushed node rides outputs (R reachability)");
		assertTrue(tRecipe.getOutputs().size() >= tFe.nodes().size() - 1 + tFe.byproducts().size(),
				"downstream nodes + byproducts are outputs");
	}

	private static Edge edgeOf(MaterialTreeDisplay aDisplay, OreDictPrefix aFrom, OreDictPrefix aTo) {
		for (Edge tEdge : aDisplay.edges()) if (tEdge.from() == aFrom && tEdge.to() == aTo) return tEdge;
		return null;
	}
}
