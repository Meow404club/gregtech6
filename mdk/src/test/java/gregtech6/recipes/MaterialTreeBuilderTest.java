package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.item.MaterialPrefixItem;
import gregtech6.recipes.tree.MaterialTreeBuilder;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTMaterialItems.PrefixMaterial;

/**
 * The material-tree census (task debt-material-tree-a, acceptance card): the dynamically
 * derived {@link MaterialTreeBuilder} over the FULL offline census pour, with the rows'
 * material legs carrying REAL {@link MaterialPrefixItem} identities — the one fixture
 * difference from the phase-gate/hash-index census (their BRICK stand-ins erase the
 * (prefix, material) seam this card derives from; one probe item per referenced pair,
 * lazily created and registered into the offline-opened vanilla registry, the
 * OMComponentFaceTest.probeItem pattern).
 *
 * <ul>
 * <li><b>Iron chain (acceptance ①)</b>: the spine the repo's maps actually contain —
 *     ore(oreRaw)→crushed via {@code gt.recipe.crusher} (GT6RecipesOreChain :153) and
 *     crushed→dust via {@code gt.recipe.shredder} (GT6RecipesShCL :138, the port of the
 *     upstream maceration family Loader_OreProcessing.java:123-133); the purified leg's
 *     TERMINAL edge crushedPurified→dust (ShCL :139 = upstream :130) is present. The
 *     purified PRODUCER rows have LANDED (task debt-ore-purified-edge, the
 *     Loader_OreProcessing.java:351 DUST_ORE sifting walk) — for the on-axis materials the
 *     producer edge is now asserted positively (the Cu spot check); Fe itself is OFF the
 *     walk's material axis (the twelve MT setCrushing sources are all off-axis), so its
 *     crushedPurified stays honestly unreachable (coordinator ruling 2026-09-26, pool item
 *     ore-purified-edge-gap, closed by the landing).</li>
 * <li><b>Spot checks</b>: Copper (same spine) and Coal (the byproduct face: Coke Oven rows
 *     consume Coal-material legs into CoalCoke — the byproduct edge card B displays).</li>
 * <li><b>Index guards (acceptance ②)</b>: structural, never wall-clock (the
 *     known_bugs recipemap-hashindex-flaky lesson) — scannedRows() == the sum of the map
 *     sizes at build, and hammering every indexed query afterwards must not move it.</li>
 * </ul>
 */
class MaterialTreeBuilderTest extends GTRecipesOfflineTestBase {

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
		openOfflineItemRegistry(); // the probeItem three-lock walk, ONCE for the whole class
	}

	/** The (prefix, material) -> probe-item resolver every material seam gets armed with.
	 * The gt6 namespace is load-bearing: a minecraft-namespaced probe would grow the
	 * frozen-vanilla pool GT6RecipesCokeOvenTest's synthetic universe re-deals on. */
	private static Item prefixItem(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		if (aPrefix == null || aMaterial == null) return null; // the loaders' null-pair drop semantics
		return PREFIX_ITEMS.computeIfAbsent(new PrefixMaterial(aPrefix, aMaterial), aPair ->
			Registry.register(BuiltInRegistries.ITEM, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("gt6", "tree_probe_" + sNextProbeId++),
				new MaterialPrefixItem(new Item.Properties(), aPair.prefix(), aPair.material())));
	}

	/** The offline item-registry unlock (the OMComponentFaceTest.probeItem walk, hoisted once per class). */
	private static void openOfflineItemRegistry() {
		var tRegistry = BuiltInRegistries.ITEM;
		//? if forge {
		try {
			// three locks must open (the FileSawTest walk): the vanilla frozen flag, the
			// delegate ForgeRegistry.isFrozen, the NamespacedWrapper.locked register gate
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
		// 21.1: the plain vanilla DefaultedMappedRegistry — a single frozen flag
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

	// ------------------------------------------------------------------
	// the census pour with PREFIX-IDENTITY legs (the hash-index fixture, BRICK -> prefixItem)
	// ------------------------------------------------------------------

	@BeforeEach
	void armFixturesAndPour() throws Exception {
		capture(() -> GT6RecipesEngineFuels.sFluidResolver, aV -> GT6RecipesEngineFuels.sFluidResolver = aV);
		capture(() -> GT6RecipesBurnFuels.sFluidResolver, aV -> GT6RecipesBurnFuels.sFluidResolver = aV);
		capture(() -> GT6RecipesOreChain.sMaterialItemResolver, aV -> GT6RecipesOreChain.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesDistillery.sFluidResolver, aV -> GT6RecipesDistillery.sFluidResolver = aV);
		capture(() -> GT6RecipesDistillery.sCircuitResolver, aV -> GT6RecipesDistillery.sCircuitResolver = aV);
		capture(() -> GT6RecipesDrying.sMaterialItemResolver, aV -> GT6RecipesDrying.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesDrying.sFluidResolver, aV -> GT6RecipesDrying.sFluidResolver = aV);
		capture(() -> GT6RecipesExtruder.sMaterialItemResolver, aV -> GT6RecipesExtruder.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesExtruder.sBlockResolver, aV -> GT6RecipesExtruder.sBlockResolver = aV);
		capture(() -> GT6RecipesExtruder.sPlateMoldResolver, aV -> GT6RecipesExtruder.sPlateMoldResolver = aV);
		capture(() -> GT6RecipesExtruder.sRodMoldResolver, aV -> GT6RecipesExtruder.sRodMoldResolver = aV);
		capture(() -> GT6RecipesAnvil.sMaterialItemResolver, aV -> GT6RecipesAnvil.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesMixer.sMaterialItemResolver, aV -> GT6RecipesMixer.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesMixer.sWaterResolver, aV -> GT6RecipesMixer.sWaterResolver = aV);
		capture(() -> GT6RecipesMixer.sCfoamResolver, aV -> GT6RecipesMixer.sCfoamResolver = aV);
		capture(() -> GT6RecipesMixer.sBaseCfoamResolver, aV -> GT6RecipesMixer.sBaseCfoamResolver = aV);
		capture(() -> GT6RecipesBath.sPlankItemResolver, aV -> GT6RecipesBath.sPlankItemResolver = aV);
		capture(() -> GT6RecipesBath.sOilFluidResolver, aV -> GT6RecipesBath.sOilFluidResolver = aV);
		capture(() -> GT6RecipesSifter.sMaterialItemResolver, aV -> GT6RecipesSifter.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesCompressor.sMaterialItemResolver, aV -> GT6RecipesCompressor.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesWiremill.sMaterialItemResolver, aV -> GT6RecipesWiremill.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesBees.sCombResolver, aV -> GT6RecipesBees.sCombResolver = aV);
		capture(() -> GT6RecipesBees.sFluidResolver, aV -> GT6RecipesBees.sFluidResolver = aV);
		capture(() -> GT6RecipesBees.sMaterialItemResolver, aV -> GT6RecipesBees.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesImplosion.sMaterialItemResolver, aV -> GT6RecipesImplosion.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesImplosion.sTntResolver, aV -> GT6RecipesImplosion.sTntResolver = aV);
		capture(() -> GT6RecipesImplosion.sCircuitResolver, aV -> GT6RecipesImplosion.sCircuitResolver = aV);
		capture(() -> GT6RecipesStoneChisel.sStoneItemResolver, aV -> GT6RecipesStoneChisel.sStoneItemResolver = aV);
		capture(() -> GT6RecipesShCL.sMaterialItemResolver, aV -> GT6RecipesShCL.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesCokeOven.sOutputItemResolver, aV -> GT6RecipesCokeOven.sOutputItemResolver = aV);
		capture(() -> GT6RecipesCokeOven.sInputItemResolver, aV -> GT6RecipesCokeOven.sInputItemResolver = aV);
		capture(() -> GT6RecipesCokeOven.sFluidResolver, aV -> GT6RecipesCokeOven.sFluidResolver = aV);
		capture(() -> GT6RecipesWelder.sPlateResolver, aV -> GT6RecipesWelder.sPlateResolver = aV);
		capture(() -> GT6RecipesWelder.sOutputResolver, aV -> GT6RecipesWelder.sOutputResolver = aV);
		capture(() -> GT6RecipesWelder.sSelectorResolver, aV -> GT6RecipesWelder.sSelectorResolver = aV);
		capture(() -> GT6RecipesMassfab.sMaterialItemResolver, aV -> GT6RecipesMassfab.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesMassfab.sMatterFluidResolver, aV -> GT6RecipesMassfab.sMatterFluidResolver = aV);
		capture(() -> GT6RecipesFusion.sCircuitResolver, aV -> GT6RecipesFusion.sCircuitResolver = aV);
		capture(() -> GT6RecipesFusion.sFluidResolver, aV -> GT6RecipesFusion.sFluidResolver = aV);
		capture(() -> GT6RecipesFusion.sMaterialItemResolver, aV -> GT6RecipesFusion.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesCanner.sDyeFluidResolver, aV -> GT6RecipesCanner.sDyeFluidResolver = aV);
		capture(() -> GT6RecipesCanner.sChlorineResolver, aV -> GT6RecipesCanner.sChlorineResolver = aV);
		capture(() -> GT6RecipesCanner.sEmptyCanResolver, aV -> GT6RecipesCanner.sEmptyCanResolver = aV);
		capture(() -> GT6RecipesCanner.sSprayPaintResolver, aV -> GT6RecipesCanner.sSprayPaintResolver = aV);
		capture(() -> GT6RecipesCanner.sRemoverResolver, aV -> GT6RecipesCanner.sRemoverResolver = aV);
		capture(() -> GT6RecipesCanner.sFoodCanEmptyResolver, aV -> GT6RecipesCanner.sFoodCanEmptyResolver = aV);
		capture(() -> GT6RecipesCanner.sRottenCansResolver, aV -> GT6RecipesCanner.sRottenCansResolver = aV);
		capture(() -> GT6RecipesCanner.sCookiesCanResolver, aV -> GT6RecipesCanner.sCookiesCanResolver = aV);
		capture(() -> GT6RecipesCanner.sCfoamFluidResolver, aV -> GT6RecipesCanner.sCfoamFluidResolver = aV);
		capture(() -> GT6RecipesCanner.sCfoamOwnedFluidResolver, aV -> GT6RecipesCanner.sCfoamOwnedFluidResolver = aV);
		capture(() -> GT6RecipesCanner.sFoamSprayResolver, aV -> GT6RecipesCanner.sFoamSprayResolver = aV);
		capture(() -> GT6RecipesCanner.sFoamSprayOwnedResolver, aV -> GT6RecipesCanner.sFoamSprayOwnedResolver = aV);
		capture(() -> GT6RecipesCanner.sLaserGasFluidResolver, aV -> GT6RecipesCanner.sLaserGasFluidResolver = aV);
		capture(() -> GT6RecipesCanner.sLaserGasEmptyResolver, aV -> GT6RecipesCanner.sLaserGasEmptyResolver = aV);
		capture(() -> GT6RecipesCanner.sLaserGasEmitterResolver, aV -> GT6RecipesCanner.sLaserGasEmitterResolver = aV);
		capture(() -> GT6RecipesSlicer.sSplitBladeResolver, aV -> GT6RecipesSlicer.sSplitBladeResolver = aV);
		capture(() -> GT6RecipesSlicer.sGridBladeResolver, aV -> GT6RecipesSlicer.sGridBladeResolver = aV);
		capture(() -> GT6RecipesSlicer.sTinyPaperResolver, aV -> GT6RecipesSlicer.sTinyPaperResolver = aV);

		GT6RecipesEngineFuels.sFluidResolver = aId -> Fluids.WATER;
		GT6RecipesBurnFuels.sFluidResolver = aId -> Fluids.WATER;
		GT6RecipesOreChain.sMaterialItemResolver = MaterialTreeBuilderTest::prefixItem;
		GT6RecipesDistillery.sFluidResolver = aId -> Fluids.WATER;
		GT6RecipesDistillery.sCircuitResolver = aConfig -> new net.minecraft.world.item.ItemStack(Items.PAPER);
		GT6RecipesDrying.sMaterialItemResolver = MaterialTreeBuilderTest::prefixItem;
		GT6RecipesDrying.sFluidResolver = aId -> Fluids.WATER;
		GT6RecipesExtruder.sMaterialItemResolver = MaterialTreeBuilderTest::prefixItem;
		GT6RecipesExtruder.sBlockResolver = aPair -> new net.minecraft.world.item.ItemStack(Items.BRICK);
		GT6RecipesExtruder.sPlateMoldResolver = () -> new net.minecraft.world.item.ItemStack(Items.BRICK);
		GT6RecipesExtruder.sRodMoldResolver = () -> new net.minecraft.world.item.ItemStack(Items.BRICK);
		GT6RecipesAnvil.sMaterialItemResolver = MaterialTreeBuilderTest::prefixItem;
		GT6RecipesMixer.sMaterialItemResolver = MaterialTreeBuilderTest::prefixItem;
		GT6RecipesMixer.sWaterResolver = aIndex -> Fluids.WATER;
		GT6RecipesMixer.sCfoamResolver = (aIndex, aOwned) -> Fluids.WATER;
		GT6RecipesMixer.sBaseCfoamResolver = () -> Fluids.WATER;
		GT6RecipesBath.sPlankItemResolver = MaterialTreeBuilderTest::prefixItem;
		GT6RecipesBath.sOilFluidResolver = aLeg -> Fluids.WATER;
		GT6RecipesSifter.sMaterialItemResolver = MaterialTreeBuilderTest::prefixItem;
		GT6RecipesCompressor.sMaterialItemResolver = MaterialTreeBuilderTest::prefixItem;
		GT6RecipesWiremill.sMaterialItemResolver = MaterialTreeBuilderTest::prefixItem;
		GT6RecipesBees.sCombResolver = aComb -> Items.BRICK;
		GT6RecipesBees.sFluidResolver = aId -> Fluids.WATER;
		GT6RecipesBees.sMaterialItemResolver = MaterialTreeBuilderTest::prefixItem;
		GT6RecipesImplosion.sMaterialItemResolver = MaterialTreeBuilderTest::prefixItem;
		GT6RecipesImplosion.sTntResolver = () -> Items.TNT;
		GT6RecipesImplosion.sCircuitResolver = aConfig -> new net.minecraft.world.item.ItemStack(Items.PAPER);
		GT6RecipesStoneChisel.sStoneItemResolver = aStone -> Items.BRICK;
		GT6RecipesShCL.sMaterialItemResolver = MaterialTreeBuilderTest::prefixItem;
		GT6RecipesCokeOven.sOutputItemResolver = aOutput -> prefixItem(aOutput.prefix(), aOutput.material());
		GT6RecipesCokeOven.sInputItemResolver = aRow -> prefixItem(aRow.inPrefix(), aRow.inMaterial());
		GT6RecipesCokeOven.sFluidResolver = aId -> Fluids.WATER;
		GT6RecipesWelder.sPlateResolver = (aP, aM) -> {
			Item tItem = prefixItem(aP, aM);
			return tItem == null ? new net.minecraft.world.item.ItemStack(Items.BRICK) : new net.minecraft.world.item.ItemStack(tItem);
		};
		GT6RecipesWelder.sOutputResolver = aId -> Items.BRICK;
		GT6RecipesWelder.sSelectorResolver = aConfig -> new net.minecraft.world.item.ItemStack(Items.PAPER);
		GT6RecipesMassfab.sMaterialItemResolver = MaterialTreeBuilderTest::prefixItem;
		GT6RecipesMassfab.sMatterFluidResolver = aHalf -> aHalf.equals("charged") ? Fluids.LAVA : Fluids.WATER;
		GT6RecipesFusion.sCircuitResolver = aConfig -> new net.minecraft.world.item.ItemStack(Items.PAPER);
		GT6RecipesFusion.sFluidResolver = (aMaterial, aMolten) -> aMolten ? Fluids.LAVA : Fluids.WATER;
		GT6RecipesFusion.sMaterialItemResolver = MaterialTreeBuilderTest::prefixItem;
		GT6RecipesCanner.sDyeFluidResolver = aIndex -> Fluids.WATER;
		GT6RecipesCanner.sChlorineResolver = () -> Fluids.LAVA;
		GT6RecipesCanner.sEmptyCanResolver = () -> new net.minecraft.world.item.ItemStack(Items.PAPER);
		GT6RecipesCanner.sSprayPaintResolver = aIndex -> new net.minecraft.world.item.ItemStack(Items.CLAY_BALL);
		GT6RecipesCanner.sRemoverResolver = () -> new net.minecraft.world.item.ItemStack(Items.CLAY_BALL);
		GT6RecipesCanner.sFoodCanEmptyResolver = () -> new net.minecraft.world.item.ItemStack(Items.PAPER);
		GT6RecipesCanner.sRottenCansResolver = aTier -> new net.minecraft.world.item.ItemStack(Items.CLAY_BALL);
		GT6RecipesCanner.sCookiesCanResolver = () -> new net.minecraft.world.item.ItemStack(Items.BRICK);
		GT6RecipesCanner.sCfoamFluidResolver = aIndex -> Fluids.FLOWING_LAVA;
		GT6RecipesCanner.sCfoamOwnedFluidResolver = aIndex -> Fluids.FLOWING_WATER;
		GT6RecipesCanner.sFoamSprayResolver = aIndex -> new net.minecraft.world.item.ItemStack(Items.CLAY_BALL);
		GT6RecipesCanner.sFoamSprayOwnedResolver = aIndex -> new net.minecraft.world.item.ItemStack(Items.CLAY_BALL);
		GT6RecipesCanner.sLaserGasFluidResolver = aGas -> Fluids.FLOWING_LAVA; // the family seam, every gas resolves (debt-hene-fluid landed the heliumneon row)
		GT6RecipesCanner.sLaserGasEmptyResolver = () -> new net.minecraft.world.item.ItemStack(Items.PAPER);
		GT6RecipesCanner.sLaserGasEmitterResolver = aGas -> "helium".equals(aGas) ? net.minecraft.world.item.ItemStack.EMPTY : new net.minecraft.world.item.ItemStack(Items.CLAY_BALL);
		// the Slicer fixture face (the phase-gate census): the two blades + the tiny-paper output
		GT6RecipesSlicer.sSplitBladeResolver = () -> new net.minecraft.world.item.ItemStack(Items.BRICK);
		GT6RecipesSlicer.sGridBladeResolver = () -> new net.minecraft.world.item.ItemStack(Items.CLAY_BALL);
		GT6RecipesSlicer.sTinyPaperResolver = () -> new net.minecraft.world.item.ItemStack(Items.PAPER);

		pourCensus();
	}

	@AfterEach
	void restoreSeamsAndDropGeneration() {
		for (Runnable tRestore : sSeamRestores) tRestore.run();
		sSeamRestores.clear();
		GT6RecipeMaps.reset();
	}

	/** The full census pour, arm by arm (the phase-gate ledger order, Slicer last). */
	private static void pourCensus() {
		GT6RecipesDistillery.load();
		GT6RecipesDrying.load();
		GT6RecipesBurnFuels.load();
		GT6RecipesEngineFuels.load();
		GT6RecipesCokeOven.load();
		GT6RecipesOreChain.load();
		GT6RecipesShCL.load();
		GT6RecipesStoneChisel.load();
		GT6RecipesCanner.load();
		GT6RecipeMapJsonLoader.pour(java.util.Map.of()); // the ledger arm — the no-op pour
		GT6RecipesMixer.load();
		GT6RecipesSifter.load();
		GT6RecipesCompressor.load();
		GT6RecipesWiremill.load();
		GT6RecipesExtruder.load();
		GT6RecipesPress.load();
		GT6RecipesBath.load();
		GT6RecipesAnvil.load();
		GT6RecipesWelder.load();
		GT6RecipesImplosion.load();
		GT6RecipesBees.load();
		GT6RecipesMassfab.load();
		GT6RecipesFusion.load();
		GT6RecipesSlicer.load();
	}

	// ------------------------------------------------------------------
	// acceptance ①: the Iron chain, derived from THIS repo's maps
	// ------------------------------------------------------------------

	@Test
	void ironChainSpineCensus() {
		MaterialTreeBuilder tTree = MaterialTreeBuilder.build();
		Set<MaterialTreeBuilder.ChainEdge> tIron = tTree.chainEdges(MT.Fe);

		// the same-material spine: ore(oreRaw) -> crushed via the Anvil hand-tool arm
		// (GT6RecipesAnvil :166, selfcrush) and crushed -> dust via the Shredder
		// (ShCL :138 — the Loader_OreProcessing.java:123-133 maceration family port)
		assertTrue(tIron.contains(new MaterialTreeBuilder.ChainEdge(OP.oreRaw, OP.crushed, "gt.recipe.anvil")),
				"ore -> crushed via the Anvil (:166), got: " + tIron);
		assertTrue(tIron.contains(new MaterialTreeBuilder.ChainEdge(OP.crushed, OP.dust, "gt.recipe.shredder")),
				"crushed -> dust via the Shredder (ShCL :138 = upstream :128), got: " + tIron);

		// the Crusher face of the SAME hop is cross-material for Iron — MT.java:2885
		// setCrushing(Fe2O3) redirects raw Iron into crushed Hematite, so the Crusher row
		// (GT6RecipesOreChain :153) lands on the byproduct face — the dual-face design;
		// inPrefix = the row's oreRaw input (the :153 template; task
		// mattree-r2-byproduct-per-step added the field — the producing STEP's band)
		assertTrue(tTree.byproductEdges(MT.Fe).contains(
				new MaterialTreeBuilder.ByproductEdge(MT.Fe, MT.Fe2O3, OP.oreRaw, OP.crushed, "gt.recipe.crusher")),
				"the Crusher ore-chain row is the Fe -> Fe2O3 redirect, got: " + tTree.byproductEdges(MT.Fe));

		// the purified leg's TERMINAL edge (ShCL :139 = upstream :130 crushedPurified -> dust)
		// and the centrifuged sibling (ShCL :140 = upstream :132) — both present as edges
		assertTrue(tIron.contains(new MaterialTreeBuilder.ChainEdge(OP.crushedPurified, OP.dust, "gt.recipe.shredder")),
				"the purified terminal leg crushedPurified -> dust (upstream :130) exists as an edge");
		assertTrue(tIron.contains(new MaterialTreeBuilder.ChainEdge(OP.crushedCentrifuged, OP.dust, "gt.recipe.shredder")),
				"the centrifuged terminal leg crushedCentrifuged -> dust (upstream :132) exists as an edge");

		// the pooled producer rows have LANDED (task debt-ore-purified-edge, the :351 DUST_ORE
		// sifting walk) — but Fe is OFF the walk's material axis (the twelve MT
		// setCrushing(:2884-2895) sources are all off-axis), so no sifter row carries a Fe
		// material leg: crushedPurified stays outside Fe's ore-reachable tree, and the
		// on-axis positive face is pinned in copperAndCoalSpotChecks below
		Set<OreDictPrefix> tReachable = tTree.reachableFromOre(MT.Fe);
		assertTrue(tReachable.contains(OP.oreRaw) && tReachable.contains(OP.crushed) && tReachable.contains(OP.dust),
				"ore -> crushed -> dust is one connected walk, got: " + tReachable);
		assertFalse(tReachable.contains(OP.crushedPurified),
				"crushedPurified is NOT reachable for the OFF-axis Fe — the sifter producer rows exist "
				+ "only for the 53 on-axis materials (debt-ore-purified-edge); only the terminal leg exists for Fe");

		// the anvil companion spine (GT6RecipesAnvil :166 oreRaw -> crushed + crushedTiny)
		assertTrue(tIron.contains(new MaterialTreeBuilder.ChainEdge(OP.oreRaw, OP.crushedTiny, "gt.recipe.anvil")),
				"ore -> crushedTiny via the Anvil (:166) — the hand-tool companion leg");
	}

	// ------------------------------------------------------------------
	// acceptance ①: >= 2 other materials, one with a byproduct face
	// ------------------------------------------------------------------

	@Test
	void copperAndCoalSpotChecks() {
		MaterialTreeBuilder tTree = MaterialTreeBuilder.build();

		// Copper: the same spine
		Set<MaterialTreeBuilder.ChainEdge> tCopper = tTree.chainEdges(MT.Cu);
		assertTrue(tCopper.contains(new MaterialTreeBuilder.ChainEdge(OP.oreRaw, OP.crushed, "gt.recipe.crusher")),
				"Cu ore -> crushed (Crusher :153)");
		assertTrue(tCopper.contains(new MaterialTreeBuilder.ChainEdge(OP.crushed, OP.dust, "gt.recipe.shredder")),
				"Cu crushed -> dust (Shredder :138)");
		// the LANDED producer face (task debt-ore-purified-edge): Cu is ON the sifter walk's
		// material axis, so the DUST_ORE ore-block rows produce its crushedPurified —
		// the on-axis half of the old honest-gap flip (Fe keeps the negative form above)
		assertTrue(tCopper.contains(new MaterialTreeBuilder.ChainEdge(OP.oreGravel, OP.crushedPurified, "gt.recipe.sifter")),
				"Cu oreGravel -> crushedPurified via the landed :351 sifting row, got: " + tCopper);
		assertTrue(tTree.reachableFromOre(MT.Cu).contains(OP.crushedPurified),
				"Cu's ore walk now reaches crushedPurified (the landed producer rows)");
		assertTrue(tTree.reachableFromOre(MT.Cu).contains(OP.dust), "Cu's ore walk reaches dust");

		// Coal: the byproduct face — the Coke Oven consumes Coal-material legs into CoalCoke
		// (StaticRow :780 oreRaw Coal -> ingot CoalCoke; :783 crushedPurified Coal -> chunkGt)
		Set<MaterialTreeBuilder.ByproductEdge> tCoalByproducts = tTree.byproductEdges(MT.Coal);
		assertFalse(tCoalByproducts.isEmpty(), "Coal carries byproduct edges (the Coke Oven coking rows)");
		assertTrue(tCoalByproducts.stream().anyMatch(e -> e.to() == MT.CoalCoke && "gt.recipe.cokeoven".equals(e.mapName())),
				"Coal -> CoalCoke via the Coke Oven, got: " + tCoalByproducts);
		// and Coal still has its own crushing spine (the tree holds BOTH faces per material)
		assertTrue(tTree.chainEdges(MT.Coal).contains(new MaterialTreeBuilder.ChainEdge(OP.crushed, OP.dust, "gt.recipe.shredder")),
				"Coal crushed -> dust chain edge coexists with the byproduct face");
	}

	// ------------------------------------------------------------------
	// acceptance ②: the index is one sweep; queries never re-scan (structural, no wall clock)
	// ------------------------------------------------------------------

	@Test
	void oneSweepIndexAndQueriesNeverRescan() {
		int tMapRows = 0;
		for (RecipeMap tMap : RecipeMap.RECIPE_MAPS.values()) tMapRows += tMap.mRecipeList.size();
		assertTrue(tMapRows > 50_000, "the census pour is the full one (MIXER 56k alone), got " + tMapRows + " rows");

		MaterialTreeBuilder tTree = MaterialTreeBuilder.build();
		assertEquals(tMapRows, tTree.scannedRows(), "build() visited EVERY row of EVERY map exactly once");
		assertTrue(tTree.chainMaterialCount() > 0 && tTree.byproductMaterialCount() > 0, "the index is populated");

		// hammer every indexed query over the full material universe — the counter must not move
		for (PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			tTree.chainEdges(tPair.material());
			tTree.byproductEdges(tPair.material());
			tTree.reachableFromOre(tPair.material());
		}
		// repeat reads are stable (set-equal, still no rescan)
		assertEquals(tTree.chainEdges(MT.Fe), tTree.chainEdges(MT.Fe));
		assertEquals(tMapRows, tTree.scannedRows(), "per-material queries are pure hash reads — no row re-entered");

		// census invariant: every edge label names a live map (the label IS a RECIPE_MAPS key)
		for (PrefixMaterial tPair : GTMaterialItems.registrationOrder()) {
			for (MaterialTreeBuilder.ChainEdge tEdge : tTree.chainEdges(tPair.material())) {
				assertTrue(RecipeMap.RECIPE_MAPS.containsKey(tEdge.mapName()), "chain edge label names a live map: " + tEdge);
			}
			for (MaterialTreeBuilder.ByproductEdge tEdge : tTree.byproductEdges(tPair.material())) {
				assertTrue(RecipeMap.RECIPE_MAPS.containsKey(tEdge.mapName()), "byproduct edge label names a live map: " + tEdge);
			}
		}
		// a material no row touches answers EMPTY, never null: the MT pool is far larger than
		// the row-covered universe, so an untouched material must exist — its read is the empty case
		OreDictMaterial tUntouched = null;
		for (OreDictMaterial tMaterial : gregapi.oredict.OreDictMaterial.MATERIAL_MAP.values()) {
			if (tTree.chainEdges(tMaterial).isEmpty()) {tUntouched = tMaterial; break;}
		}
		assertTrue(tUntouched != null, "some registered material must be untouched by every row (the empty case exists)");
		assertTrue(tTree.byproductEdges(tUntouched).isEmpty(), "the untouched material's byproduct face is empty too");
		assertTrue(tTree.reachableFromOre(tUntouched).isEmpty(), "and its ore walk is the empty set — no NPE, no null");
	}
}
