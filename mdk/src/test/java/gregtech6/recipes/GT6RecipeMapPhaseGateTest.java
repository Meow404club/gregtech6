package gregtech6.recipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;

import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.registry.GTMaterialItems;

/**
 * The RM phase gate (task p32-rm-phase-gate): the whole map generation lives in the
 * OPEN→FROZEN phase ({@link GT6RecipeMaps.Phase}, the GTCEu MaterialRegistry shape,
 * MaterialRegistry.java:34-45). Three contracts, each with its pin:
 * <ol>
 * <li><b>Registration pours stay legal</b> (acceptance ①) — the whole static census
 * (every hook-ledger loader) pours clean in the OPEN phase, over the shared offline
 * fixture (the GT6RecipeGenerationGuardTest convention).</li>
 * <li><b>A late pour fails loud</b> (acceptance ②) — after {@link GT6RecipeMaps#freeze()}
 * every {@code addRecipe} throws {@link IllegalStateException} carrying the map name; the
 * offending call stack rides the exception itself. {@link GT6RecipeMaps#reset()} rewinds
 * the phase with the rest of the generation (the P18 ledger discipline) so pours are legal
 * again in the fresh generation.</li>
 * <li><b>The freeze point is pinned</b> (acceptance ③) — the per-map row counts of the
 * fully-poured generation are the snapshot; freeze must preserve them exactly (no row
 * lost, none added). A card that changes any map's registration stock turns the table red
 * and must consciously bump it — the freeze-point ratchet. Census discipline: the pour
 * census below must stay equal to the generation hook ledger (the poison-capable census
 * the guard test pins), so a new stock loader cannot silently miss the snapshot.</li>
 * </ol>
 *
 * <p><b>Declared snapshot scope</b>: the static loader stock only. The JSON smoke stock is
 * pinned per-file by the dedicated *JsonTest/*RowsPourTest suite (re-deriving its item/fluid
 * resolution here would duplicate those fixtures); the CokeOven tag-listener subset is
 * runtime tag data and deliberately not pinnable offline. Both ride the same funnel and
 * the same window the dedicated pins cover.
 */
class GT6RecipeMapPhaseGateTest extends GTRecipesOfflineTestBase {

	/**
	 * The pour census: every hook-ledger loader's {@code load()} arm, in ledger order (the
	 * guard test's POISON_CAPABLE_LOADERS shape). The JSON loader's ledger arm is the
	 * no-op {@code pour(Map.of())}. The equality pin against
	 * {@link GT6RecipeMaps#generationResetHooks()} (after class-loading) keeps this census
	 * and the guard test's ledger from drifting apart independently.
	 */
	private static final String[] POUR_CENSUS = {
			"gregtech6.recipes.GT6RecipesDistillery",
			"gregtech6.recipes.GT6RecipesDrying",
			"gregtech6.recipes.GT6RecipesBurnFuels",
			"gregtech6.recipes.GT6RecipesEngineFuels",
			"gregtech6.recipes.GT6RecipesCokeOven",
			"gregtech6.recipes.GT6RecipesOreChain",
			"gregtech6.recipes.GT6RecipesShCL",
			"gregtech6.recipes.GT6RecipesStoneChisel",
			"gregtech6.recipes.GT6RecipesCanner",
			"gregtech6.recipes.GT6RecipeMapJsonLoader",
			"gregtech6.recipes.GT6RecipesMixer",
			"gregtech6.recipes.GT6RecipesSifter",
			"gregtech6.recipes.GT6RecipesCompressor",
			"gregtech6.recipes.GT6RecipesWiremill",
			"gregtech6.recipes.GT6RecipesExtruder",
			"gregtech6.recipes.GT6RecipesPress",
			"gregtech6.recipes.GT6RecipesBath",
			"gregtech6.recipes.GT6RecipesAnvil",
			"gregtech6.recipes.GT6RecipesWelder",
			"gregtech6.recipes.GT6RecipesImplosion",
			"gregtech6.recipes.GT6RecipesBees",
			"gregtech6.recipes.GT6RecipesMassfab",
			"gregtech6.recipes.GT6RecipesFusion",
	};

	/** The freeze-point snapshot: map field name → expected row count after the full census pour. Upstream registration order. */
	private static final Map<String, Integer> SNAPSHOT = new LinkedHashMap<>();
	static {
		SNAPSHOT.put("FURNACE", 0);
		SNAPSHOT.put("COKE_OVEN", 39);
		SNAPSHOT.put("SHREDDER", 353);
		SNAPSHOT.put("CRUSHER", 1632);
		SNAPSHOT.put("LATHE", 74);
		SNAPSHOT.put("CHISEL", 36);
		SNAPSHOT.put("ENGINE_FUELS", 7);
		SNAPSHOT.put("FLUIDBED", 0);
		SNAPSHOT.put("BURN", 7);
		SNAPSHOT.put("GAS_FUELS", 0);
		SNAPSHOT.put("DISTILLERY", 8);
		SNAPSHOT.put("DRYING", 42);
		SNAPSHOT.put("CANNER", 53); // +1: the CO2 laser gas fill row (task p32-qu-laser-domain, MultiItemTechnological.java:403)
		SNAPSHOT.put("MIXER", 56000);
		SNAPSHOT.put("SIFTING", 1);
		// the ONE version-sensitive census: the Compressor walk rides the vanilla item
		// universe, which differs 1.20.1 vs 1.21.1 by 109 compressibles — the per-leg pin
		// (the stonecutter swap-table convention, GTRecipesOfflineTestBase shape)
		//? if forge {
		SNAPSHOT.put("COMPRESSOR", 2630);
		//?} else {
		/*SNAPSHOT.put("COMPRESSOR", 2521);
		*///?}
		SNAPSHOT.put("WIREMILL", 912);
		SNAPSHOT.put("ROLLING_MILL", 0);
		SNAPSHOT.put("BATH", 1089);
		SNAPSHOT.put("FURNACE_FUEL", 0);
		SNAPSHOT.put("PRESS", 0);
		SNAPSHOT.put("EXTRUDER", 966);
		SNAPSHOT.put("CRUCIBLE_SMELTING", 0);
		SNAPSHOT.put("CRUCIBLE_ALLOYING", 0);
		SNAPSHOT.put("ANVIL", 145);
		SNAPSHOT.put("ANVIL_BEND", 27);
		SNAPSHOT.put("FERMENTER", 0);
		SNAPSHOT.put("LOOM", 0);
		SNAPSHOT.put("PRESSURE_WASHER", 0);
		SNAPSHOT.put("SQUEEZER", 20);
		SNAPSHOT.put("BEDROCK_ORE_LIST", 0);
		SNAPSHOT.put("CLUSTER_MILL", 0);
		SNAPSHOT.put("ROLL_BENDER", 0);
		SNAPSHOT.put("ROLL_FORMER", 0);
		SNAPSHOT.put("CENTRIFUGE", 20);
		SNAPSHOT.put("SHARPENING", 0);
		SNAPSHOT.put("CUTTER", 0);
		SNAPSHOT.put("BOXINATOR", 0);
		SNAPSHOT.put("UNBOXINATOR", 0);
		SNAPSHOT.put("SLUICE", 0);
		SNAPSHOT.put("AUTOCRAFTER", 0);
		SNAPSHOT.put("STEAM_CRACKING", 0);
		SNAPSHOT.put("CATALYTIC_CRACKING", 0);
		SNAPSHOT.put("COAGULATOR", 0);
		SNAPSHOT.put("CRYO_MIXER", 0);
		SNAPSHOT.put("MAGNETIC_SEPARATOR", 0);
		SNAPSHOT.put("INJECTOR", 0);
		SNAPSHOT.put("LAMINATOR", 0);
		SNAPSHOT.put("AUTOCLAVE", 0);
		SNAPSHOT.put("FREEZER", 0);
		SNAPSHOT.put("POLARIZER", 0);
		SNAPSHOT.put("LIGHTNING", 0);
		SNAPSHOT.put("SLICER", 0);
		SNAPSHOT.put("LASER_ENGRAVER", 0);
		SNAPSHOT.put("WELDER", 22);
		SNAPSHOT.put("ELECTROLYZER", 0);
		SNAPSHOT.put("PRINTER", 0);
		SNAPSHOT.put("SCANNER_VISUALS", 0);
		SNAPSHOT.put("GENERIFIER", 0);
		SNAPSHOT.put("DISTILLATION_TOWER", 0);
		SNAPSHOT.put("CRYO_DISTILLATION_TOWER", 0);
		SNAPSHOT.put("MELTER", 0);
		SNAPSHOT.put("SMELTER", 0);
		SNAPSHOT.put("FUELS_HOT", 0);
		SNAPSHOT.put("ROASTING", 0);
		SNAPSHOT.put("IMPLOSION", 268);
		SNAPSHOT.put("SCANNER_MOLECULAR", 0);
		SNAPSHOT.put("MASSFAB", 4220);
		SNAPSHOT.put("REPLICATOR", 0);
		SNAPSHOT.put("FUSION", 18);
	}

	/** The captured loader seams, restored after each test so sibling classes see defaults (the guard-test convention). */
	private static final List<Runnable> sSeamRestores = new ArrayList<>();

	@BeforeAll
	static void bootAndCaptureDefaults() {
		GTMaterialItems.initMaterials(); // the offline material universe (the guard-test precedent)
		capture(() -> GT6RecipesEngineFuels.sFluidResolver, aV -> GT6RecipesEngineFuels.sFluidResolver = aV);
		capture(() -> GT6RecipesBurnFuels.sFluidResolver, aV -> GT6RecipesBurnFuels.sFluidResolver = aV);
		capture(() -> GT6RecipesOreChain.sMaterialItemResolver, aV -> GT6RecipesOreChain.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesDistillery.sFluidResolver, aV -> GT6RecipesDistillery.sFluidResolver = aV);
		capture(() -> GT6RecipesDistillery.sCircuitResolver, aV -> GT6RecipesDistillery.sCircuitResolver = aV);
		capture(() -> GT6RecipesDrying.sFluidResolver, aV -> GT6RecipesDrying.sFluidResolver = aV);
		capture(() -> GT6RecipesDrying.sMaterialItemResolver, aV -> GT6RecipesDrying.sMaterialItemResolver = aV);
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
		capture(() -> GT6RecipesCanner.sCarbonDioxideResolver, aV -> GT6RecipesCanner.sCarbonDioxideResolver = aV);
		capture(() -> GT6RecipesCanner.sLaserGasEmptyResolver, aV -> GT6RecipesCanner.sLaserGasEmptyResolver = aV);
		capture(() -> GT6RecipesCanner.sLaserGasCo2Resolver, aV -> GT6RecipesCanner.sLaserGasCo2Resolver = aV);
	}

	private static <T> void capture(Supplier<T> aGetter, Consumer<T> aSetter) {
		T tDefault = aGetter.get();
		sSeamRestores.add(() -> aSetter.accept(tDefault));
	}

	/**
	 * The shared offline fixture: resolve-everything identity stand-ins (BRICK/WATER/PAPER —
	 * the recipe mechanics only compare identities; the counts depend on resolution success,
	 * not on which stand-in, and Recipe carries no equals so identical stand-ins never
	 * collapse rows). Every registry-object-reading default is replaced, the synthetic
	 * material-universe pattern of the per-loader pour tests. The material resolver keeps
	 * the null-drop convention (a null prefix slot = the upstream mat() → null invalid-output
	 * drop, the Anvil/ShCL template shape — an all-non-null resolver would NPE where the
	 * convention drops).
	 */
	private static Item brickOrNull(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		return aPrefix == null || aMaterial == null ? null : Items.BRICK;
	}

	private static ItemStack brickStackOrNull(OreDictPrefix aPrefix, OreDictMaterial aMaterial) {
		return aPrefix == null || aMaterial == null ? null : new ItemStack(Items.BRICK);
	}

	@BeforeEach
	void armFixturesAndFreshGeneration() {
		GT6RecipesEngineFuels.sFluidResolver = aId -> Fluids.WATER;
		GT6RecipesBurnFuels.sFluidResolver = aId -> Fluids.WATER;
		GT6RecipesOreChain.sMaterialItemResolver = GT6RecipeMapPhaseGateTest::brickOrNull;
		GT6RecipesDistillery.sFluidResolver = aId -> Fluids.WATER;
		GT6RecipesDistillery.sCircuitResolver = aConfig -> new ItemStack(Items.PAPER);
		GT6RecipesDrying.sFluidResolver = aId -> Fluids.WATER;
		GT6RecipesDrying.sMaterialItemResolver = GT6RecipeMapPhaseGateTest::brickOrNull;
		GT6RecipesExtruder.sMaterialItemResolver = GT6RecipeMapPhaseGateTest::brickOrNull;
		GT6RecipesExtruder.sBlockResolver = aPair -> new ItemStack(Items.BRICK);
		GT6RecipesExtruder.sPlateMoldResolver = () -> new ItemStack(Items.BRICK);
		GT6RecipesExtruder.sRodMoldResolver = () -> new ItemStack(Items.BRICK);
		GT6RecipesAnvil.sMaterialItemResolver = GT6RecipeMapPhaseGateTest::brickOrNull;
		GT6RecipesMixer.sMaterialItemResolver = GT6RecipeMapPhaseGateTest::brickOrNull;
		GT6RecipesMixer.sWaterResolver = aIndex -> Fluids.WATER;
		GT6RecipesMixer.sCfoamResolver = (aIndex, aOwned) -> Fluids.WATER;
		GT6RecipesMixer.sBaseCfoamResolver = () -> Fluids.WATER;
		GT6RecipesBath.sPlankItemResolver = GT6RecipeMapPhaseGateTest::brickOrNull;
		GT6RecipesBath.sOilFluidResolver = aLeg -> Fluids.WATER;
		GT6RecipesSifter.sMaterialItemResolver = GT6RecipeMapPhaseGateTest::brickOrNull;
		GT6RecipesCompressor.sMaterialItemResolver = GT6RecipeMapPhaseGateTest::brickOrNull;
		GT6RecipesWiremill.sMaterialItemResolver = GT6RecipeMapPhaseGateTest::brickOrNull;
		GT6RecipesBees.sCombResolver = aComb -> Items.BRICK;
		GT6RecipesBees.sFluidResolver = aId -> Fluids.WATER;
		GT6RecipesBees.sMaterialItemResolver = GT6RecipeMapPhaseGateTest::brickOrNull;
		GT6RecipesImplosion.sMaterialItemResolver = GT6RecipeMapPhaseGateTest::brickOrNull;
		GT6RecipesImplosion.sTntResolver = () -> Items.TNT;
		GT6RecipesImplosion.sCircuitResolver = aConfig -> new ItemStack(Items.PAPER);
		GT6RecipesStoneChisel.sStoneItemResolver = aStone -> Items.BRICK;
		GT6RecipesShCL.sMaterialItemResolver = GT6RecipeMapPhaseGateTest::brickOrNull;
		GT6RecipesCokeOven.sOutputItemResolver = aOutput -> Items.BRICK;
		GT6RecipesCokeOven.sInputItemResolver = aRow -> Items.BRICK;
		GT6RecipesCokeOven.sFluidResolver = aId -> Fluids.WATER;
		GT6RecipesWelder.sPlateResolver = GT6RecipeMapPhaseGateTest::brickStackOrNull;
		GT6RecipesWelder.sOutputResolver = aId -> Items.BRICK;
		GT6RecipesWelder.sSelectorResolver = aConfig -> new ItemStack(Items.PAPER);
		GT6RecipesMassfab.sMaterialItemResolver = (OreDictPrefix aPrefix, OreDictMaterial aMaterial) -> Items.IRON_INGOT;
		GT6RecipesMassfab.sMatterFluidResolver = aHalf -> aHalf.equals("charged") ? Fluids.LAVA : Fluids.WATER;
		GT6RecipesFusion.sCircuitResolver = aConfig -> new ItemStack(Items.PAPER);
		GT6RecipesFusion.sFluidResolver = (OreDictMaterial aMaterial, Boolean aMolten) -> aMolten ? Fluids.LAVA : Fluids.WATER;
		GT6RecipesFusion.sMaterialItemResolver = GT6RecipeMapPhaseGateTest::brickOrNull;
		GT6RecipesFusion.sMaterialItemResolver = (OreDictPrefix aPrefix, OreDictMaterial aMaterial) -> Items.BRICK;
		GT6RecipesCanner.sDyeFluidResolver = aIndex -> Fluids.WATER;
		GT6RecipesCanner.sChlorineResolver = () -> Fluids.LAVA;
		GT6RecipesCanner.sEmptyCanResolver = () -> new ItemStack(Items.PAPER);
		GT6RecipesCanner.sSprayPaintResolver = aIndex -> new ItemStack(Items.CLAY_BALL);
		GT6RecipesCanner.sRemoverResolver = () -> new ItemStack(Items.CLAY_BALL);
		GT6RecipesCanner.sFoodCanEmptyResolver = () -> new ItemStack(Items.PAPER);
		GT6RecipesCanner.sRottenCansResolver = aTier -> new ItemStack(Items.CLAY_BALL);
		GT6RecipesCanner.sCookiesCanResolver = () -> new ItemStack(Items.BRICK);
		GT6RecipesCanner.sCfoamFluidResolver = aIndex -> Fluids.FLOWING_LAVA;
		GT6RecipesCanner.sCfoamOwnedFluidResolver = aIndex -> Fluids.FLOWING_WATER;
		GT6RecipesCanner.sFoamSprayResolver = aIndex -> new ItemStack(Items.CLAY_BALL);
		GT6RecipesCanner.sFoamSprayOwnedResolver = aIndex -> new ItemStack(Items.CLAY_BALL);
		GT6RecipesCanner.sCarbonDioxideResolver = () -> Fluids.FLOWING_LAVA;
		GT6RecipesCanner.sLaserGasEmptyResolver = () -> new ItemStack(Items.PAPER);
		GT6RecipesCanner.sLaserGasCo2Resolver = () -> new ItemStack(Items.CLAY_BALL);
		GT6RecipeMaps.reset();
		GT6RecipeMaps.init();
	}

	@AfterEach
	void restoreSeamsAndDropGeneration() {
		for (Runnable tRestore : sSeamRestores) tRestore.run();
		GT6RecipeMaps.reset();
	}

	/** The full census pour, arm by arm (the census is the ledger; see the class doc). */
	private static void pourCensus() throws Exception {
		GT6RecipesDistillery.load();
		GT6RecipesDrying.load();
		GT6RecipesBurnFuels.load();
		GT6RecipesEngineFuels.load();
		GT6RecipesCokeOven.load();
		GT6RecipesOreChain.load();
		GT6RecipesShCL.load();
		GT6RecipesStoneChisel.load();
		GT6RecipesCanner.load();
		GT6RecipeMapJsonLoader.pour(Map.of()); // the ledger arm — the no-op pour
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
	}

	/** The census and the hook ledger must move together (the guard test pins the same ledger against ITS array). */
	@Test
	void pourCensusStaysEqualToTheGenerationHookLedger() throws ClassNotFoundException {
		for (String tLoader : POUR_CENSUS) Class.forName(tLoader);
		assertEquals(POUR_CENSUS.length, GT6RecipeMaps.generationResetHooks().size(),
				"the pour census must cover every hook-ledger loader — a new stock loader joins BOTH "
				+ "the guard test's POISON_CAPABLE_LOADERS and this census, and bumps the snapshot table");
	}

	/** Acceptance ①: the whole registration pour is legal in the OPEN phase (and this snapshot IS the pour). */
	@Test
	void registrationPourStaysLegalAndPhaseStaysOpen() throws Exception {
		pourCensus();
		assertEquals(GT6RecipeMaps.Phase.OPEN, GT6RecipeMaps.phase(), "the pour must not move the phase");
	}

	/** Acceptance ②: after freeze() every addRecipe fails loud with the map name; reset() rewinds the phase (the P18 discipline). */
	@Test
	void latePourThrowsWithTheMapNameAndResetRewindsThePhase() throws Exception {
		pourCensus();
		GT6RecipeMaps.freeze();
		assertEquals(GT6RecipeMaps.Phase.FROZEN, GT6RecipeMaps.phase());

		IllegalStateException tThrown = assertThrows(IllegalStateException.class,
				() -> GT6RecipeMaps.COKE_OVEN.addRecipe(row()),
				"a late pour into a stocked map fails loud");
		assertTrue(tThrown.getMessage().contains("gt.recipe.cokeoven"),
				"the message must carry the map name, got: " + tThrown.getMessage());
		assertTrue(tThrown.getStackTrace().length > 0, "the offending call stack rides the exception");

		// an EMPTY map fails just as loud — the gate is the phase, not the content
		IllegalStateException tEmpty = assertThrows(IllegalStateException.class,
				() -> GT6RecipeMaps.GENERIFIER.addRecipe(row()),
				"a late pour into a declared-empty map fails loud too");
		assertTrue(tEmpty.getMessage().contains("gt.recipe.generifier"), "the empty map's name rides too: " + tEmpty.getMessage());

		// the P18 rewind: a fresh generation registers OPEN — pours are legal again
		GT6RecipeMaps.reset();
		assertEquals(GT6RecipeMaps.Phase.OPEN, GT6RecipeMaps.phase(), "reset() rewinds the phase with the generation");
		GT6RecipesEngineFuels.load();
		assertEquals(7, GT6RecipeMaps.ENGINE_FUELS.mRecipeList.size(), "the fresh generation pours clean (the guard-test pin shape)");
	}

	/** Acceptance ③: the freeze point — freeze preserves the fully-poured counts exactly (the ratchet; bump the table consciously). */
	@Test
	void freezePreservesTheSnapshotRowCounts() throws Exception {
		pourCensus();
		Map<String, Integer> tActual = new LinkedHashMap<>();
		for (Map.Entry<String, RecipeMap> tEntry : snapshotOrder().entrySet()) {
			tActual.put(tEntry.getKey(), tEntry.getValue().mRecipeList.size());
		}
		List<String> tMismatches = new ArrayList<>();
		for (Map.Entry<String, Integer> tExpected : SNAPSHOT.entrySet()) {
			Integer tSeen = tActual.get(tExpected.getKey());
			if (!tExpected.getValue().equals(tSeen)) {
				tMismatches.add(tExpected.getKey() + ": expected " + tExpected.getValue() + " vs actual " + tSeen);
			}
		}
		assertTrue(tMismatches.isEmpty(),
				"the freeze-point snapshot drifted — a card changed a map's registration stock; verify the change and bump "
				+ "SNAPSHOT (the ratchet). Mismatches: " + tMismatches + " — full actual table for the bump: " + tActual);

		GT6RecipeMaps.freeze();
		// the frozen generation is EXACTLY the snapshot: no row lost, none smuggled in
		for (Map.Entry<String, RecipeMap> tEntry : snapshotOrder().entrySet()) {
			assertEquals(SNAPSHOT.get(tEntry.getKey()), Integer.valueOf(tEntry.getValue().mRecipeList.size()),
					tEntry.getKey() + " must be identical across the freeze");
		}
		assertEquals(70, RecipeMap.RECIPE_MAPS.size(), "the freeze-point registry census (the GT6RecipeMapsTest pin shape)");
	}

	/** The JSON reload window: a FROZEN /reload re-pour lands its rows and re-freezes; an OPEN pour owes no re-freeze. */
	@Test
	void theJsonWindowReopensForAReloadAndRefreezes() {
		GT6RecipeMaps.freeze();
		GT6RecipeMapJsonLoader.pour(Map.of(new net.minecraft.resources.ResourceLocation("gt6", "shredder"),
				parse("{\"recipes\": [{\"inputs\": [{\"item\": \"minecraft:andesite\"}], \"outputs\": [{\"item\": \"minecraft:cobblestone\"}], \"duration\": 512, \"eut\": 16}]}")));
		assertEquals(1, GT6RecipeMapJsonLoader.pouredCount("shredder"), "the frozen-reload row landed through the window");
		assertEquals(GT6RecipeMaps.Phase.FROZEN, GT6RecipeMaps.phase(), "the window re-freezes on the way out");

		GT6RecipeMaps.reset(); // an OPEN generation (offline/boot) owes no re-freeze
		GT6RecipeMapJsonLoader.pour(Map.of());
		assertEquals(GT6RecipeMaps.Phase.OPEN, GT6RecipeMaps.phase(), "the window is invisible to an OPEN generation");
	}

	/** The RECIPE_MAPS registry in upstream registration order — the snapshot's walk order. */
	private static Map<String, RecipeMap> snapshotOrder() {
		Map<String, RecipeMap> tOrder = new LinkedHashMap<>();
		tOrder.put("FURNACE", GT6RecipeMaps.FURNACE);
		tOrder.put("COKE_OVEN", GT6RecipeMaps.COKE_OVEN);
		tOrder.put("SHREDDER", GT6RecipeMaps.SHREDDER);
		tOrder.put("CRUSHER", GT6RecipeMaps.CRUSHER);
		tOrder.put("LATHE", GT6RecipeMaps.LATHE);
		tOrder.put("CHISEL", GT6RecipeMaps.CHISEL);
		tOrder.put("ENGINE_FUELS", GT6RecipeMaps.ENGINE_FUELS);
		tOrder.put("FLUIDBED", GT6RecipeMaps.FLUIDBED);
		tOrder.put("BURN", GT6RecipeMaps.BURN);
		tOrder.put("GAS_FUELS", GT6RecipeMaps.GAS_FUELS);
		tOrder.put("DISTILLERY", GT6RecipeMaps.DISTILLERY);
		tOrder.put("DRYING", GT6RecipeMaps.DRYING);
		tOrder.put("CANNER", GT6RecipeMaps.CANNER);
		tOrder.put("MIXER", GT6RecipeMaps.MIXER);
		tOrder.put("SIFTING", GT6RecipeMaps.SIFTING);
		tOrder.put("COMPRESSOR", GT6RecipeMaps.COMPRESSOR);
		tOrder.put("WIREMILL", GT6RecipeMaps.WIREMILL);
		tOrder.put("ROLLING_MILL", GT6RecipeMaps.ROLLING_MILL);
		tOrder.put("BATH", GT6RecipeMaps.BATH);
		tOrder.put("FURNACE_FUEL", GT6RecipeMaps.FURNACE_FUEL);
		tOrder.put("PRESS", GT6RecipeMaps.PRESS);
		tOrder.put("EXTRUDER", GT6RecipeMaps.EXTRUDER);
		tOrder.put("CRUCIBLE_SMELTING", GT6RecipeMaps.CRUCIBLE_SMELTING);
		tOrder.put("CRUCIBLE_ALLOYING", GT6RecipeMaps.CRUCIBLE_ALLOYING);
		tOrder.put("ANVIL", GT6RecipeMaps.ANVIL);
		tOrder.put("ANVIL_BEND", GT6RecipeMaps.ANVIL_BEND);
		tOrder.put("FERMENTER", GT6RecipeMaps.FERMENTER);
		tOrder.put("LOOM", GT6RecipeMaps.LOOM);
		tOrder.put("PRESSURE_WASHER", GT6RecipeMaps.PRESSURE_WASHER);
		tOrder.put("SQUEEZER", GT6RecipeMaps.SQUEEZER);
		tOrder.put("BEDROCK_ORE_LIST", GT6RecipeMaps.BEDROCK_ORE_LIST);
		tOrder.put("CLUSTER_MILL", GT6RecipeMaps.CLUSTER_MILL);
		tOrder.put("ROLL_BENDER", GT6RecipeMaps.ROLL_BENDER);
		tOrder.put("ROLL_FORMER", GT6RecipeMaps.ROLL_FORMER);
		tOrder.put("CENTRIFUGE", GT6RecipeMaps.CENTRIFUGE);
		tOrder.put("SHARPENING", GT6RecipeMaps.SHARPENING);
		tOrder.put("CUTTER", GT6RecipeMaps.CUTTER);
		tOrder.put("BOXINATOR", GT6RecipeMaps.BOXINATOR);
		tOrder.put("UNBOXINATOR", GT6RecipeMaps.UNBOXINATOR);
		tOrder.put("SLUICE", GT6RecipeMaps.SLUICE);
		tOrder.put("AUTOCRAFTER", GT6RecipeMaps.AUTOCRAFTER);
		tOrder.put("STEAM_CRACKING", GT6RecipeMaps.STEAM_CRACKING);
		tOrder.put("CATALYTIC_CRACKING", GT6RecipeMaps.CATALYTIC_CRACKING);
		tOrder.put("COAGULATOR", GT6RecipeMaps.COAGULATOR);
		tOrder.put("CRYO_MIXER", GT6RecipeMaps.CRYO_MIXER);
		tOrder.put("MAGNETIC_SEPARATOR", GT6RecipeMaps.MAGNETIC_SEPARATOR);
		tOrder.put("INJECTOR", GT6RecipeMaps.INJECTOR);
		tOrder.put("LAMINATOR", GT6RecipeMaps.LAMINATOR);
		tOrder.put("AUTOCLAVE", GT6RecipeMaps.AUTOCLAVE);
		tOrder.put("FREEZER", GT6RecipeMaps.FREEZER);
		tOrder.put("POLARIZER", GT6RecipeMaps.POLARIZER);
		tOrder.put("LIGHTNING", GT6RecipeMaps.LIGHTNING);
		tOrder.put("SLICER", GT6RecipeMaps.SLICER);
		tOrder.put("LASER_ENGRAVER", GT6RecipeMaps.LASER_ENGRAVER);
		tOrder.put("WELDER", GT6RecipeMaps.WELDER);
		tOrder.put("ELECTROLYZER", GT6RecipeMaps.ELECTROLYZER);
		tOrder.put("PRINTER", GT6RecipeMaps.PRINTER);
		tOrder.put("SCANNER_VISUALS", GT6RecipeMaps.SCANNER_VISUALS);
		tOrder.put("GENERIFIER", GT6RecipeMaps.GENERIFIER);
		tOrder.put("DISTILLATION_TOWER", GT6RecipeMaps.DISTILLATION_TOWER);
		tOrder.put("CRYO_DISTILLATION_TOWER", GT6RecipeMaps.CRYO_DISTILLATION_TOWER);
		tOrder.put("MELTER", GT6RecipeMaps.MELTER);
		tOrder.put("SMELTER", GT6RecipeMaps.SMELTER);
		tOrder.put("FUELS_HOT", GT6RecipeMaps.FUELS_HOT);
		tOrder.put("ROASTING", GT6RecipeMaps.ROASTING);
		tOrder.put("IMPLOSION", GT6RecipeMaps.IMPLOSION);
		tOrder.put("SCANNER_MOLECULAR", GT6RecipeMaps.SCANNER_MOLECULAR);
		tOrder.put("MASSFAB", GT6RecipeMaps.MASSFAB);
		tOrder.put("REPLICATOR", GT6RecipeMaps.REPLICATOR);
		tOrder.put("FUSION", GT6RecipeMaps.FUSION);
		return tOrder;
	}

	/** Offline JSON parse helper (the Gson face the loader itself uses). */
	private static com.google.gson.JsonElement parse(String aJson) {
		return com.google.gson.JsonParser.parseString(aJson);
	}

	/** A minimal well-formed row for the gate throws-pins (Recipe.java:135, the 8-arg form). */
	private static Recipe row() {
		return new Recipe(true,
				new ItemStack[]{new ItemStack(Items.BRICK)},
				new ItemStack[]{new ItemStack(Items.BRICK)},
				new net.minecraftforge.fluids.FluidStack[0],
				new net.minecraftforge.fluids.FluidStack[0],
				512, 16, 0);
	}
}
