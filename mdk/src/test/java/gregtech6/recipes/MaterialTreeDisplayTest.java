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
 * registered into the offline-opened vanilla registry). Only the three ore-chain loaders
 * are poured — the display's chain face reads exactly those maps, so the minimal pour
 * keeps the census deterministic without the full 24-loader arm.
 *
 * <ul>
 * <li><b>Layout table census (acceptance ①)</b>: every pinned prefix lands in its
 *     contracted column (ore/blockRaw → 0, crushed family → 1, purified/centrifuged → 2,
 *     dust family → 3), the {@code ore*} wildcard holds, and non-chain prefixes are
 *     excluded (-1 — the table is the ore-chain filter).</li>
 * <li><b>Iron display (acceptance ①, the Fe 离轴 reality)</b>: the six nodes current main
 *     actually reaches (oreRaw/crushed/crushedTiny/dust/dustTiny/dustDiv72 — crushedPurified
 *     has a table column but NO node until the pooled producers land, the
 *     ore-purified-edge-gap card), the anvil
 *     + shredder edge labels, and the two-face byproduct merge: the DERIVED crusher
 *     redirect (Fe → crushed Fe2O3, MT.java:2885) first, then the DECLARED face of the
 *     crushing target Fe2O3 (MT.java:3820: Ilmenite, GraniticMineralSand, MnO2, ClayRed).</li>
 * <li><b>Copper display (the Cu 在轴 reality)</b>: the crusher hop is same-material (the
 *     chain edge, OreChain :153), so the byproduct column is DECLARED-ONLY
 *     (MT.java:3827: Cobaltite, Au, Ni, Malachite, As).</li>
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

	/** The (prefix, material) -> probe-item resolver armed into the three ore-chain loaders. */
	private static Item prefixItem(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		if (aPrefix == null || aMaterial == null) return null;
		return PREFIX_ITEMS.computeIfAbsent(new PrefixMaterial(aPrefix, aMaterial), aPair ->
			Registry.register(BuiltInRegistries.ITEM, "mtree_display_probe_" + sNextProbeId++,
				new MaterialPrefixItem(new Item.Properties(), aPair.prefix(), aPair.material())));
	}

	/** The offline item-registry unlock (GT6RecipeMapHashIndexTest.probeItem, the tree-a hoisted form). */
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
		GT6RecipesOreChain.sMaterialItemResolver = MaterialTreeDisplayTest::prefixItem;
		GT6RecipesShCL.sMaterialItemResolver = MaterialTreeDisplayTest::prefixItem;
		GT6RecipesAnvil.sMaterialItemResolver = MaterialTreeDisplayTest::prefixItem;
		GT6RecipesOreChain.load(); // the Crusher ore chain (:153 oreRaw, :154 blockRaw, the Fe2O3 redirect face)
		GT6RecipesShCL.load(); // the Shredder terminal legs (:138-143 crushed*/crushedPurified/crushedCentrifuged -> dust*)
		GT6RecipesAnvil.load(); // the Anvil selfcrush + mortar rows (:158-172)
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
		// geometry sanity: every slot rectangle fits the fixed category box
		assertTrue(MaterialTreeDisplay.columnX(MaterialTreeDisplay.COL_BYPRODUCT) + 18 <= MaterialTreeDisplay.WIDTH);
		assertTrue(MaterialTreeDisplay.nodeY(new Node(OP.dust, MaterialTreeDisplay.COL_DUST, MaterialTreeDisplay.MAX_ROWS - 1, net.minecraft.world.item.ItemStack.EMPTY)) + 18
				<= MaterialTreeDisplay.HEIGHT);
	}

	// ------------------------------------------------------------------
	// acceptance ①: the Iron display — Fe 离轴 + the two-face merge
	// ------------------------------------------------------------------

	@Test
	void ironDisplayTwoFaces() {
		MaterialTreeBuilder tTree = MaterialTreeBuilder.build();
		MaterialTreeDisplay tDisplay = MaterialTreeDisplay.of(tTree, MT.Fe, MaterialTreeDisplayTest::prefixItem);
		assertNotNull(tDisplay, "Iron's ore walk must yield a display");

		// the six nodes current main actually reaches (crushedPurified NOT reachable — the pooled
		// producer rows are the ore-purified-edge-gap card; when they land this census grows a
		// node in the empty COL_PURIFIED column with ZERO code changes — the adaptivity clause)
		Map<OreDictPrefix, Node> tNodes = new HashMap<>();
		for (Node tNode : tDisplay.nodes()) tNodes.put(tNode.prefix(), tNode);
		assertEquals(6, tDisplay.nodes().size(), "node census: " + tDisplay.nodes());
		assertTrue(tNodes.containsKey(OP.oreRaw) && tNodes.containsKey(OP.crushed) && tNodes.containsKey(OP.crushedTiny)
				&& tNodes.containsKey(OP.dust) && tNodes.containsKey(OP.dustTiny) && tNodes.containsKey(OP.dustDiv72),
				"node set: " + tNodes.keySet());
		assertFalse(tNodes.containsKey(OP.crushedPurified), "crushedPurified has a table column but no node on current main");
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
	// acceptance ①: the Copper display — Cu 在轴, declared-only byproducts
	// ------------------------------------------------------------------

	@Test
	void copperDisplayDeclaredOnly() {
		MaterialTreeBuilder tTree = MaterialTreeBuilder.build();
		MaterialTreeDisplay tDisplay = MaterialTreeDisplay.of(tTree, MT.Cu, MaterialTreeDisplayTest::prefixItem);
		assertNotNull(tDisplay, "Copper's ore walk must yield a display");

		Map<OreDictPrefix, Node> tNodes = new HashMap<>();
		for (Node tNode : tDisplay.nodes()) tNodes.put(tNode.prefix(), tNode);
		assertEquals(6, tDisplay.nodes().size(), "node census: " + tDisplay.nodes());
		assertTrue(tNodes.containsKey(OP.oreRaw) && tNodes.containsKey(OP.crushed) && tNodes.containsKey(OP.dust),
				"the Cu spine is in the node set: " + tNodes.keySet());

		// the on-axis face: Copper's crusher hop is a SAME-material chain edge (OreChain :153),
		// so the crusher label rides the CHAIN edge, not a byproduct edge (the Fe contrast)
		Edge tOreEdge = edgeOf(tDisplay, OP.oreRaw, OP.crushed);
		assertNotNull(tOreEdge, "oreRaw -> crushed edge");
		assertTrue(tOreEdge.mapNames().contains("gt.recipe.crusher"), "crusher label: " + tOreEdge.mapNames());
		assertNotNull(edgeOf(tDisplay, OP.crushed, OP.dust), "crushed -> dust edge");
		assertTrue(tDisplay.byproducts().stream().noneMatch(Byproduct::derived),
				"Cu has no derived byproduct edge — the dormant prefix byproducts stay out");

		// declared-only: crushingTarget(Cu) == Cu (no redirect), MT.java:3827 verbatim order
		OreDictMaterial[] tDeclared = {MT.OREMATS.Cobaltite, MT.Au, MT.Ni, MT.OREMATS.Malachite, MT.As};
		assertEquals(tDeclared.length, tDisplay.byproducts().size(), "byproduct census");
		for (int i = 0; i < tDeclared.length; i++) {
			assertFalse(tDisplay.byproducts().get(i).derived());
			assertEquals(prefixItem(OP.dust, tDeclared[i]), tDisplay.byproducts().get(i).stack().getItem(),
					"declared byproduct " + tDeclared[i].mNameInternal);
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
		assertEquals(new ResourceLocation("gt6", MaterialTreeDisplay.CATEGORY_UID_PATH), tCategory.getRecipeType().getUid());
		assertEquals(MaterialTreeDisplay.WIDTH, tCategory.getWidth());
		assertEquals(MaterialTreeDisplay.HEIGHT, tCategory.getHeight());

		// EMI leg: same category id (the JEMI balance — both twins ship in this card), and the
		// Fe recipe's input/output split mirrors the JEI slot split
		assertEquals(new ResourceLocation("gt6", MaterialTreeDisplay.CATEGORY_UID_PATH), GT6MaterialTreeEmiCategory.id());
		MaterialTreeBuilder tTree = MaterialTreeBuilder.build();
		MaterialTreeDisplay tFe = MaterialTreeDisplay.of(tTree, MT.Fe, MaterialTreeDisplayTest::prefixItem);
		GT6MaterialTreeEmiRecipe tRecipe = new GT6MaterialTreeEmiRecipe(tFe);
		assertEquals(new ResourceLocation("gt6", MaterialTreeDisplay.CATEGORY_UID_PATH + "/iron"), tRecipe.getId());
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
