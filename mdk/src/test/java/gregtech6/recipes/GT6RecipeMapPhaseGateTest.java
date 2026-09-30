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

import gregapi.data.MT;
import gregapi.data.OP;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;
import gregtech6.registry.GTMaterialItems;

/**
 * The RM phase gate (task rm-phase-gate): the whole map generation lives in the
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
			"gregtech6.recipes.GT6RecipesBees", // task machines-bumblelyzer-crucible — the pour also fills the GT6RecipeMapBumblelyzer display stock (the sFakeRecipes list, outside mRecipeList)
			"gregtech6.recipes.GT6RecipesMassfab",
			"gregtech6.recipes.GT6RecipesFusion",
			"gregtech6.recipes.GT6RecipesSlicer", // task slicer-row-domain - the vanilla leather/paper pour joins the ledger
			"gregtech6.recipes.GT6RecipesBake", // task food-bake-recipes - the bake-chain RM pour joins the ledger
			"gregtech6.recipes.maps.GT6RecipeMapBumblelyzer", // task machines-bumblelyzer-crucible — the display-stock reset hook joins the ledger
			"gregtech6.recipes.GT6RecipesReactorRods", // task debt-reactor-c-rods — the 45-row reactor-rod pour joins the ledger
			"gregtech6.recipes.GT6RecipesCrops", // task cbc-5-crop-consumption — the crop consumption pour joins the ledger
			"gregtech6.recipes.GT6RecipesFood", // task food-recipes-t1b — the five-map food band pour joins the ledger (REVIEW FIX seat XVII: the card selftest ran a narrow Food/Bath/Mixer filter and missed the ADR-P18 ledger ratchet)
	};

	/** The freeze-point snapshot: map field name → expected row count after the full census pour. Upstream registration order. */
	private static final Map<String, Integer> SNAPSHOT = new LinkedHashMap<>();
	static {
		SNAPSHOT.put("FURNACE", 0);
		SNAPSHOT.put("COKE_OVEN", 47); // beam-consume-increment: +8 wood-beam rows (Loader_Recipes_Woods.java:197-201, the GT6BeamKind static activation)
		SNAPSHOT.put("SHREDDER", 406); // +45 task casing-machine-register (the shared-layer join, bumped by task squeezer-seed-legs after the two-commit verification: green 353 at f4077ef5d, red 398 at the casing merge 80bd8e605) — the 840 casingMachine-prefix registrations (4 prefixes x 210) light up 45 survivors of the ShCL RECYCLABLE ring gates (pourRecyclableRing, the GT6RecipesShCL:653 walk; this card ran its own registry domain and the recipes-domain ratchet follows here) +8 task cbc-5-crop-consumption — the wool seam white shred + the cropWheat/baleWheat + 4 grain + potato-remains mortarize-shredder rows
		SNAPSHOT.put("CRUSHER", 1643); // +11 task machines-bumblelyzer-crucible — the bouleGt force-table item universe grows the ore-chain crush-back walk (the plate-gem/tiny/boule carriers)
		SNAPSHOT.put("LATHE", 77); // +3 task machines-bumblelyzer-crucible — the same cascade over the lathe rod/wire walk
		SNAPSHOT.put("CHISEL", 36);
		SNAPSHOT.put("ENGINE_FUELS", 7);
		SNAPSHOT.put("FLUIDBED", 0);
		SNAPSHOT.put("BURN", 7);
		SNAPSHOT.put("GAS_FUELS", 0);
		SNAPSHOT.put("DISTILLERY", 8);
		SNAPSHOT.put("DRYING", 48); // +6 task cbc-5-crop-consumption — the fodder + 4 grain-crop + cropWheat drying legs
		SNAPSHOT.put("CANNER", 84); // +5: the laser gas fill family closure (task debt-laser-gas-family, MultiItemTechnological.java:396-403 — neon/argon/krypton/xenon/carbonmonoxide pour; helium skips over the fixture arm) +1: task debt-hene-fluid — the heliumneon blend fluid row landed (MT.java:1024 → the Loader_Fluids.java:660 createGas walk), the :401 fill row pours; helium keeps the offline fixture skip +25: task debt-reactor-c-rods — the 24 reactor-rod fills (:742-744/:746-762/:782-785) + the :789 Tritium unpack pour
		SNAPSHOT.put("MIXER", 56023); // +20 task cbc-5-crop-consumption — (4 grain crops + cropWheat) x the 4-water mash walk +3 task food-bake-recipes — the :141-:143 dough rows (the :140 gemChipped leg dormant) (union seat XVII)
		SNAPSHOT.put("SIFTING", 489); // +212 task debt-ore-purified-edge — the Loader_OreProcessing.java:351 DUST_ORE arm lands: 4 port families (gravel/sand/redsand/mud, the DUST_ORE-tagged prefixes of GT6OreBlocks.FAMILIES) x the material axis, every row resolving under the brick fixture; +52 task a-ore-axis-extension — the axis grew 53 -> 66 (4 x 13); +224 task b-gem-pool-extension — the axis grew 66 -> 122 (4 x 56, + 1: the 489th row is the grass row0)
		// the ONE version-sensitive census: the Compressor walk rides the vanilla item
		// universe, which differs 1.20.1 vs 1.21.1 by 109 compressibles — the per-leg pin
		// (the stonecutter swap-table convention, GTRecipesOfflineTestBase shape)
		//? if forge {
		SNAPSHOT.put("COMPRESSOR", 2631); // +1 task cbc-5-crop-consumption — the cropWheat compact leg (9 wheat -> the hay block, the LoaderItemList:760 alias)
		//?} else {
		/*SNAPSHOT.put("COMPRESSOR", 2518); // +1 task cbc-5-crop-consumption — the cropWheat compact leg (the same +1 as the forge leg).
		// the base 2517 = -4 task machines-bumblelyzer-crucible — the compressor walk derives from the live item universe, which the bouleGt force-table changed (the row-level mechanism rides the exclusion filters over the new gem-plate/tiny/boule items; the ratchet protocol: bump the verified drift, the walk-level accounting is the cutting-domain card's audit face)
		*///?}
		SNAPSHOT.put("WIREMILL", 912);
		SNAPSHOT.put("ROLLING_MILL", 1); // +1 task food-bake-recipes — the :139 dough→flat row
		SNAPSHOT.put("BATH", 1090); // +1 task food-bake-recipes — the :357 fries row
		SNAPSHOT.put("FURNACE_FUEL", 0);
		SNAPSHOT.put("PRESS", 7); // +7 task food-bake-recipes — the :146-:149 loaves + the :152 cylinder trio
		SNAPSHOT.put("EXTRUDER", 966);
		SNAPSHOT.put("CRUCIBLE_SMELTING", 0);
		SNAPSHOT.put("CRUCIBLE_ALLOYING", 0);
		SNAPSHOT.put("ANVIL", 145);
		SNAPSHOT.put("ANVIL_BEND", 27);
		SNAPSHOT.put("FERMENTER", 1716); // +1716 task cbc-5-crop-consumption — the RM.biomass 66-leg walk (RM.java:688-705) over 26 feeders: 16 flour-grain dust/blockDust arms + the 4 fodder items + the 4 grain crops + cropWheat + the baleWheat hay-block alias
		SNAPSHOT.put("LOOM", 0);
		SNAPSHOT.put("PRESSURE_WASHER", 0);
		// task squeezer-seed-legs — the 4 vanilla seed-oil legs (squeezer.json, the cbc-5 leftover (4):
		// the Loader_Recipes_Crops.java:256-305 oredict seed fan folded to wheat/melon/beetroot/pumpkin
		// per the juicer.json rows-4-7 ruling) ride the SAME JSON seam, equally OUT of this offline
		// snapshot walk and pinned per-file by GT6RecipeMapDataSqueezerSeedRowsPourTest.
		SNAPSHOT.put("SQUEEZER", 25); // +5 task cbc-5-crop-consumption — the vanilla-anchored crop juice legs (apple/melon/beet/carrot/potato)
		SNAPSHOT.put("BEDROCK_ORE_LIST", 0);
		SNAPSHOT.put("CLUSTER_MILL", 0);
		SNAPSHOT.put("ROLL_BENDER", 0);
		SNAPSHOT.put("ROLL_FORMER", 0);
		SNAPSHOT.put("CENTRIFUGE", 40); // +20 task debt-reactor-c-rods — the 17 depleted + 3 solid-enriched recycle rows (:764-780/:787-790) join the census pour
		SNAPSHOT.put("SHARPENING", 0);
		// task recipe-data-b2c-cut — the CUTTER JSON row stock (23596 walk rows in cutter.json, the
		// 20-statement x 4-leg handler replay of Loader_Recipes_Handlers.java:633-652) rides the JSON
		// seam, which stays OUT of this offline snapshot walk (the declared snapshot scope: the
		// census arm pours the no-op JSON face) — the stock is pinned per-file by
		// GT6RecipeMapDataB2cCutRowsPourTest (the live-walk cross-check ratchet). Merge order for the
		// parallel B2c cards: weld -> cut — the weld card's pin segment lands first, this one
		// tail-appends after it (the rebase keeps both segments, neither rewrites the other's line).
		// task recipe-data-b2c-sawing — the RM.sawing SCATTER stock (3959 rows in sawing.json, the
		// seven-anchor replay) rides the SAME JSON seam under the second "sawing" key into the SAME
		// CUTTER map (the size-split ruling: cutter.json at 4966700/5242880 bytes), equally OUT of
		// this offline snapshot walk and pinned per-file by GT6RecipeMapDataB2cSawingRowsPourTest.
		// Merge order: roll -> generify -> weld -> cut -> magnet -> sawing — this segment tail-appends
		// after cut's (the rebase keeps every segment, neither rewrites another's line).
		// task sawing-plank-concrete-increment — the ACTIVATION increment over sawing.json (census
		// 3959 -> 4239, +280 = 56 calls x5): the 9 GT6 tree-log plank legs (Woods:169, the gt-tree-planks
		// unlock) + the Woods:192 :66 DEFAULT_BEAM row (5, the beam-blocks-register unlock) + the
		// Vanilla:604-617 IL.Plank-output statics (70) + the BlockMetaType:92 BlockColored/concrete face
		// (160, the concrete-blocks-register unlock) — all under the declared IL.Plank -> minecraft:oak_planks
		// identity mapping (the file head carries the ruling). Equally OUT of this offline snapshot walk
		// (the same "sawing" JSON seam, CUTTER stays 0) and pinned per-file by the same
		// GT6RecipeMapDataB2cSawingRowsPourTest ratchet. Merge order: sawing -> beam-consume-increment
		// (pending branch, its 35 beam rows tail-append the same array) -> this card — the union rebase
		// keeps every segment, neither rewrites another's line (the concrete lang-seam precedent).
		// task b2-residual-maps — the six B2 residual maps (juicer 22 / roasting 15 / lightning 8 /
		// cryomixer 62 / loom 27 / unboxinator 2 file rows) ride the JSON seam, equally OUT of this
		// offline snapshot walk and pinned per-file by GT6RecipeMapDataB2ResidualRowsPourTest (the
		// census ratchet + the loom ANY.Iron/ANY.Steel live-walk recompute). The card also carries
		// the roasting DECLARED CORRECTION: the seated Boudouard rows' gas amounts were rewritten
		// from the 432-family (the liquid-arm U=144 misapplied to the gas accessor) to the
		// gas-native 1000 mB/U face (3000/6000/12000 -> 4000/8000/16000), with the
		// GT6RoastingRowsPourTest pins updated in the same commit (the declared out-of-boundary edit).
		// Merge order: ... -> sawing -> b2-residual — this segment tail-appends after sawing's.
		SNAPSHOT.put("CUTTER", 0);
		SNAPSHOT.put("BOXINATOR", 4); // +4 task food-bake-recipes — the :687/:726/:756 packs + the :360 fries pack (the plateDouble-Paper pair resolves live)
		SNAPSHOT.put("UNBOXINATOR", 4); // +1 task cbc-5-crop-consumption — the :251 baleWheat unpack (the hay block -> 9 wheat, the LoaderItemList:761 Crop_Wheat alias) +3 task food-bake-recipes — the :687/:726/:756 unpacks (union seat XVII)
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
		SNAPSHOT.put("SLICER", 13); // +8 task food-bake-recipes — the 4 cookie doughs + 4 loaf splits // task slicer-row-domain - the vanilla-face rows land (the leather quartet + the paper row); the declared smoke-JSON debt is retired with the card
		SNAPSHOT.put("LASER_ENGRAVER", 0);
		SNAPSHOT.put("WELDER", 30); // +8 task crucible-wall-obtainability — the dedicated crucible-wall rows replay the :1143-1153 row onto the 8 port-side block twins
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
		capture(() -> GT6RecipesSlicer.sSplitBladeResolver, aV -> GT6RecipesSlicer.sSplitBladeResolver = aV);
		capture(() -> GT6RecipesSlicer.sGridBladeResolver, aV -> GT6RecipesSlicer.sGridBladeResolver = aV);
		capture(() -> GT6RecipesSlicer.sTinyPaperResolver, aV -> GT6RecipesSlicer.sTinyPaperResolver = aV);
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
		capture(() -> GT6RecipesReactorRods.sRodResolver, aV -> GT6RecipesReactorRods.sRodResolver = aV);
		capture(() -> GT6RecipesReactorRods.sMaterialResolver, aV -> GT6RecipesReactorRods.sMaterialResolver = aV);
		capture(() -> GT6RecipesReactorRods.sTritiumResolver, aV -> GT6RecipesReactorRods.sTritiumResolver = aV);
		capture(() -> GT6RecipesCrops.sMaterialItemResolver, aV -> GT6RecipesCrops.sMaterialItemResolver = aV);
		capture(() -> GT6RecipesCrops.sIdItemResolver, aV -> GT6RecipesCrops.sIdItemResolver = aV);
		capture(() -> GT6RecipesCrops.sFluidResolver, aV -> GT6RecipesCrops.sFluidResolver = aV);
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
		// debt-laser-gas-family + debt-hene-fluid: the live posture — every family gas
		// resolves its fluid leg (the heliumneon blend row landed), helium keeps the
		// offline fixture skip (the emitter registry lookup yields nothing unbooted)
		GT6RecipesCanner.sLaserGasFluidResolver = aGas -> Fluids.FLOWING_LAVA;
		GT6RecipesCanner.sLaserGasEmptyResolver = () -> new ItemStack(Items.PAPER);
		GT6RecipesCanner.sLaserGasEmitterResolver = aGas -> "helium".equals(aGas) ? ItemStack.EMPTY : new ItemStack(Items.CLAY_BALL);
		// the Slicer fixture face: the two blades + the tiny-paper output ride distinct
		// vanilla stand-ins (the row mechanics only compare identities; the vanilla
		// armor/paper inputs need no fixture)
		GT6RecipesSlicer.sSplitBladeResolver = () -> new ItemStack(Items.BRICK);
		GT6RecipesSlicer.sGridBladeResolver = () -> new ItemStack(Items.CLAY_BALL);
		GT6RecipesSlicer.sTinyPaperResolver = () -> new ItemStack(Items.PAPER);
		// the bake fixture face (task food-bake-recipes): the port-reality universe — the
		// two dormant pairs stay null (the :140 gemChipped Sugar + :360 plateDouble Paper
		// legs ride the skip side), every bound pair rides a distinct vanilla stand-in
		GT6RecipesBake.sMaterialItemResolver = (aPrefix, aMaterial) -> {
			if (aPrefix == OP.gemChipped && aMaterial == MT.Sugar) return null; // the one dormancy (:140)
			return Items.BRICK; // the plateDouble-Paper pair resolves live (the :360 row pours)
		};
		GT6RecipesBake.sBakeItemResolver = aIndex -> Items.PAPER;
		GT6RecipesBake.sMoldItemResolver = aIndex -> Items.IRON_INGOT;
		GT6RecipesBake.sBladeResolver = aIndex -> Items.IRON_SHOVEL;
		GT6RecipesBake.sSelectorResolver = aConfig -> new ItemStack(Items.GOLD_NUGGET);
		GT6RecipesBake.sNamedFluidResolver = aName -> Fluids.WATER;
		// the reactor-rod arms: the gate only counts rows, the identity stand-ins suffice
		// (the material legs are all non-null prefixes, so brickStackOrNull never drops)
		GT6RecipesReactorRods.sRodResolver = aId -> new ItemStack(Items.PAPER);
		GT6RecipesReactorRods.sMaterialResolver = GT6RecipeMapPhaseGateTest::brickStackOrNull;
		GT6RecipesReactorRods.sTritiumResolver = () -> Fluids.LAVA;
		// the crop-consumption face: id/material/fluid seams all resolve (the brick fixture;
		// the row mechanics only compare identities — task cbc-5-crop-consumption)
		GT6RecipesCrops.sMaterialItemResolver = GT6RecipeMapPhaseGateTest::brickOrNull;
		GT6RecipesCrops.sIdItemResolver = aId -> Items.BRICK;
		GT6RecipesCrops.sFluidResolver = aId -> Fluids.WATER;
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
		GT6RecipesSlicer.load();
		GT6RecipesBake.load(); // task food-bake-recipes — the 26-row bake pour (the two dormant pairs stay skip-side) joins the census
		GT6RecipesReactorRods.load(); // task debt-reactor-c-rods — the 45-row pour (25 Canner + 20 Centrifuge) joins the census
		GT6RecipesCrops.load(); // task cbc-5-crop-consumption — the crop consumption pour joins the census
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
		assertEquals(80, RecipeMap.RECIPE_MAPS.size(), "the freeze-point registry census (the GT6RecipeMapsTest pin shape; +2 task machines-bumblelyzer-crucible (the crystallisationcrucible row stock + the bumblelyzer declared-empty) +2 task machines-burner-plantalyzer (the Burner Mixer constants row + the declared-empty Plantalyzer compat map) +5 task rm-six-maps (microwave/cooker/toolhead declared-empty + mortar/hammer whose rows ride the JSON seam — JSON-seam maps stay OUT of the SNAPSHOT walk, the juicer/crystallisationcrucible precedent))");
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
