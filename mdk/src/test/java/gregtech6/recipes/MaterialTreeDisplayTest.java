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
 * The material-tree display census (task debt-material-tree-b acceptance, reworked onto the
 * EMI recipe-tree face by task mattree-emi-tree-rework): the display assembly over the LIVE
 * ore-chain pour, with the same prefix-identity probe fixture as
 * {@link MaterialTreeBuilderTest} (one MaterialPrefixItem per referenced pair, lazily
 * registered into the offline-opened vanilla registry). The pour arms exactly the maps the
 * ore chain touches — the three ore-chain loaders PLUS the LANDED :351 DUST_ORE sifting
 * walk (task debt-ore-purified-edge; {@code MaterialTreeBuilder.build()} full-spectrum scans
 * {@code RecipeMap.RECIPE_MAPS} at runtime, so the fixture must pour every map the census
 * pins — the OreByproductInfoTest precedent: bare Sifter, no Canner arm).
 *
 * <ul>
 * <li><b>EMI spacing census (SPEC1, the EMI reference table)</b>: the layout constants are
 *     the EMI recipe tree's (BoMScreen.java:61-64/:660-661, tmp/harvest/emi-1.20.1-src;
 *     1.21.1 BoMScreen.java:63-65 byte-identical) with the two ADR deviations documented on
 *     {@code MaterialTreeDisplay} (BAND_PITCH 28 vs 20, uniform lanes vs extent spread).</li>
 * <li><b>Iron display, the coverage pin (SPEC2: 矿石链至最深 + 坩埚链在 + 手工/机器族在)</b>:
 *     the ore chain runs full depth (ore family → crushed → dust), the crucible SELF arm
 *     delivers the INGOT (the dynamic map's edge, zero stored rows), the casting family
 *     delivers plate/gear (the Mold edges), and the byproduct merge keeps its two faces
 *     (the DERIVED crusher redirect first, then the DECLARED face of Fe2O3 — uncapped).</li>
 * <li><b>Copper display (the Cu 在轴 landed shape)</b>: the four DUST_ORE sifting inputs
 *     join the ore band, crushedPurified OCCUPIES band 1 via the {@code gt.recipe.sifter}
 *     edge (BFS: oreGravel → crushedPurified is a one-hop path), and the crucible/casting
 *     arms reach Cu's ingot and shapes the same way.</li>
 * <li><b>Both-viewer faces (acceptance ①双腿注册)</b>: the JEI category and the EMI
 *     category/recipe construct offline with the ONE shared id {@code gt6:material_tree}
 *     (the JEMI balance), and the EMI recipe's input/output split mirrors the JEI slot
 *     split (band-0 inputs, downstream + byproduct outputs).</li>
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
			Registry.register(BuiltInRegistries.ITEM, ResourceLocation.fromNamespaceAndPath("gt6", "mtree_display_probe_" + sNextProbeId++),
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
	void emiSpacingCensus() {
		// SPEC1 — the EMI recipe-tree spacing table (tmp/harvest/emi-1.20.1-src
		// BoMScreen.java:61-64 NODE_WIDTH 30 / NODE_HORIZONTAL_SPACING 8 /
		// NODE_VERTICAL_SPACING 20; the recipe-node box width 42 at :660-661; the 1.21.1
		// generation byte-identical at BoMScreen.java:63-65). The two ADR deviations
		// (BAND_PITCH 28 vs 20 — outline-only boxes cannot paint the rail through the box —
		// and uniform 50px lanes vs extent spread) are documented on MaterialTreeDisplay.
		assertEquals(42, MaterialTreeDisplay.BOX_WIDTH, "EMI's recipe-node box (BoMScreen.java:660-661)");
		assertEquals(20, MaterialTreeDisplay.BOX_HEIGHT, "the symmetric box height (EMI :697-717 is y-11..y+10)");
		assertEquals(50, MaterialTreeDisplay.LANE_PITCH, "box 42 + NODE_HORIZONTAL_SPACING 8 (BoMScreen.java:62)");
		assertEquals(28, MaterialTreeDisplay.BAND_PITCH, "the ADR band pitch (EMI NODE_VERTICAL_SPACING 20, BoMScreen.java:63)");
		assertEquals(16, MaterialTreeDisplay.BAND_Y0, "band 0's centre row");
		assertEquals(4, MaterialTreeDisplay.HEADER_Y, "the header above band 0");
		// the nominal page frame derives from the same constants (the JEI per-category seat)
		assertEquals(MaterialTreeDisplay.LANE_X0 + 7 * MaterialTreeDisplay.LANE_PITCH + MaterialTreeDisplay.BOX_WIDTH / 2 + 12, MaterialTreeDisplay.WIDTH);
		assertEquals(MaterialTreeDisplay.BAND_Y0 + 6 * MaterialTreeDisplay.BAND_PITCH + MaterialTreeDisplay.BOX_HEIGHT / 2 + 12, MaterialTreeDisplay.HEIGHT);
	}

	// ------------------------------------------------------------------
	// acceptance ①: the Iron display — Fe 离轴 + the two-face merge
	// ------------------------------------------------------------------

	@Test
	void ironDisplayFullCoverageDepth() {
		MaterialTreeBuilder tTree = MaterialTreeBuilder.build();
		MaterialTreeDisplay tDisplay = MaterialTreeDisplay.of(tTree, MT.Fe, MaterialTreeDisplayTest::prefixItem);
		assertNotNull(tDisplay, "Iron's ore walk must yield a display");

		// SPEC2 coverage pin (the 「矿物处理完后没了,坩埚没了,各种加工流程也没了」 root): the tree
		// runs the FULL walk now — the ore spine, the crucible ingot (the dynamic zero-row
		// map's synthesized edge) and the casting/machine shapes. No whitelist, no cap.
		Map<OreDictPrefix, Node> tNodes = new HashMap<>();
		for (Node tNode : tDisplay.nodes()) tNodes.put(tNode.prefix(), tNode);
		assertTrue(tNodes.containsKey(OP.oreRaw) && tNodes.containsKey(OP.crushed) && tNodes.containsKey(OP.crushedTiny)
				&& tNodes.containsKey(OP.dust) && tNodes.containsKey(OP.dustTiny) && tNodes.containsKey(OP.dustDiv72),
				"the ore chain at full depth: " + tNodes.keySet());
		assertTrue(tNodes.containsKey(OP.ingot), "THE CRUCIBLE PIN: the dynamic smelting arm delivers the ingot");
		assertEquals(0, tNodes.get(OP.oreRaw).band(), "the ore roots sit on band 0 (the EMI depth axis)");
		assertEquals(1, tNodes.get(OP.crushed).band(), "one hop: crushed");
		assertEquals(2, tNodes.get(OP.dust).band(), "two hops: dust");
		assertEquals(2, tNodes.get(OP.ingot).band(), "the ingot shares band 2 (its shortest hop is the direct crushed -> ingot smelt)");
		assertTrue(tNodes.containsKey(OP.plate) && tNodes.containsKey(OP.gearGt), "THE CASTING/MACHINE PIN: the mold family's shapes are on the tree");
		assertEquals(3, tNodes.get(OP.plate).band(), "the mold shapes ride one band below the dust they pour from");

		// the crucible edge label is the live map; the mold edge label is the declared synthetic
		Edge tIngotEdge = edgeOf(tDisplay, OP.crushed, OP.ingot);
		assertNotNull(tIngotEdge, "crushed -> ingot edge");
		assertTrue(tIngotEdge.mapNames().contains(MaterialTreeBuilder.CRUCIBLE_SMELTING_MAP), "the crucible label: " + tIngotEdge.mapNames());
		Edge tPlateEdge = edgeOf(tDisplay, OP.dust, OP.plate);
		assertNotNull(tPlateEdge, "dust -> plate edge");
		assertTrue(tPlateEdge.mapNames().contains("Mold"), "the casting label: " + tPlateEdge.mapNames());

		// the ore-spine edge labels survive the rework: the anvil selfcrush hop and the shared
		// crushed->dust leg (anvil mortar :167 AND shredder :138 both pour same-material rows)
		Edge tOreEdge = edgeOf(tDisplay, OP.oreRaw, OP.crushed);
		assertNotNull(tOreEdge, "oreRaw -> crushed edge");
		assertTrue(tOreEdge.mapNames().contains("gt.recipe.anvil"), "anvil label: " + tOreEdge.mapNames());
		Edge tDustEdge = edgeOf(tDisplay, OP.crushed, OP.dust);
		assertNotNull(tDustEdge, "crushed -> dust edge");
		assertTrue(tDustEdge.mapNames().contains("gt.recipe.shredder") && tDustEdge.mapNames().contains("gt.recipe.anvil"),
				"both producers label the dust leg: " + tDustEdge.mapNames());

		// the byproduct two-face merge, UNCAPPED now (derived first, then declared)
		List<Byproduct> tByproducts = tDisplay.byproducts();
		assertEquals(5, tByproducts.size(), "byproduct census: " + tByproducts);
		Byproduct tDerived = tByproducts.get(0);
		assertTrue(tDerived.derived(), "the first byproduct is the derived crusher redirect");
		assertEquals(prefixItem(OP.crushed, MT.Fe2O3), tDerived.stack().getItem(), "crushed Hematite stack");
		assertEquals(0, tDerived.step(), "the redirect hangs on band 0 (its ore input)");
		OreDictMaterial[] tDeclared = {MT.OREMATS.Ilmenite, MT.OREMATS.GraniticMineralSand, MT.MnO2, MT.ClayRed};
		for (int i = 0; i < tDeclared.length; i++) {
			Byproduct tByproduct = tByproducts.get(1 + i);
			assertFalse(tByproduct.derived(), "entries 1..4 are the declared face");
			assertEquals(MaterialTreeDisplay.DECLARED_TEXT, tByproduct.sourceLabel());
			assertEquals(prefixItem(OP.dust, tDeclared[i]), tByproduct.stack().getItem(),
					"declared byproduct " + tDeclared[i].mNameInternal + " shows its dust");
		}
	}

	@Test
	void copperDisplayLandedSifter() {
		MaterialTreeBuilder tTree = MaterialTreeBuilder.build();
		MaterialTreeDisplay tDisplay = MaterialTreeDisplay.of(tTree, MT.Cu, MaterialTreeDisplayTest::prefixItem);
		assertNotNull(tDisplay, "Copper's ore walk must yield a display");

		// the LANDED walk face (debt-ore-purified-edge): the four DUST_ORE sifting inputs join
		// oreRaw on band 0, and crushedPurified OCCUPIES band 1 — BFS puts it there directly
		// (oreGravel -> crushedPurified is a one-hop row), while the two-hop bath route would
		// land the same node deeper; shortest-path layering keeps one node
		Map<OreDictPrefix, Node> tNodes = new HashMap<>();
		for (Node tNode : tDisplay.nodes()) tNodes.put(tNode.prefix(), tNode);
		assertTrue(tNodes.containsKey(OP.oreRaw) && tNodes.containsKey(OP.oreGravel) && tNodes.containsKey(OP.oreSand)
				&& tNodes.containsKey(OP.oreRedSand) && tNodes.containsKey(OP.oreMud),
				"band 0 holds oreRaw + the four DUST_ORE sifting inputs: " + tNodes.keySet());
		assertEquals(0, tNodes.get(OP.oreRaw).band());
		assertEquals(1, tNodes.get(OP.crushed).band());
		assertEquals(1, tNodes.get(OP.crushedPurified).band(), "crushedPurified rides band 1 via the landed :351 rows");
		assertEquals(2, tNodes.get(OP.dust).band());
		assertTrue(tNodes.containsKey(OP.ingot), "the crucible arm reaches Cu too");
		assertTrue(tNodes.containsKey(OP.plate), "the casting arm reaches Cu too");

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

		// the byproduct band is TWO-face, uncapped: the derived sifting tinies first
		// (Cu -> Cobaltite/Au/Ni dustTiny, the :331-333 first-three tiers of MT.java:3827),
		// then the declared remainder (Malachite, As; MT.java:3827 order)
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
		assertFalse(tRecipe.getInputs().isEmpty(), "the band-0 ore roots ride inputs (U reachability)");
		assertTrue(tRecipe.getInputs().stream().anyMatch(tIn -> tIn.getEmiStacks().get(0).getItemStack().getItem() == prefixItem(OP.oreRaw, MT.Fe)),
				"the raw ore item is an input");
		assertTrue(tRecipe.getOutputs().stream().anyMatch(tOut -> tOut.getItemStack().getItem() == prefixItem(OP.crushed, MT.Fe)),
				"the crushed node rides outputs (R reachability)");
		assertTrue(tRecipe.getOutputs().size() >= tFe.nodes().size() - tRecipe.getInputs().size() + tFe.byproducts().size(),
				"downstream nodes + byproducts are outputs");
	}

	private static Edge edgeOf(MaterialTreeDisplay aDisplay, OreDictPrefix aFrom, OreDictPrefix aTo) {
		for (Edge tEdge : aDisplay.edges()) if (tEdge.from() == aFrom && tEdge.to() == aTo) return tEdge;
		return null;
	}
}
