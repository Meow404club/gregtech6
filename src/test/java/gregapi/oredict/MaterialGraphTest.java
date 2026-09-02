/**
 * Tests for task gt-material-graph: conversion-chain queries and the alloy composition reference
 * graph over the real MT dataset (1273 materials registered via MT.init()).
 *
 * Every chain assertion cites its upstream (GT6 1.7.10) evidence; the query idioms themselves are
 * factored from upstream call sites (UT.java:718/:1047, Loader_OreProcessing.java:311, see
 * MaterialGraph javadoc).
 */
package gregapi.oredict;

import static gregapi.data.CS.U;
import static gregapi.data.CS.U2;
import static gregapi.data.CS.U3;
import static gregapi.data.CS.U4;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregapi.oredict.MaterialGraph.Process;
import gregapi.oredict.configurations.IOreDictConfigurationComponent;

public class MaterialGraphTest {

	@BeforeAll
	public static void initMT() {
		// Other test classes may leave the shared registry closed; MT.init()'s re-init path needs it open.
		MaterialRegistry.INSTANCE.open();
		MT.init();
	}

	private static OreDictMaterial testMaterial(String aName) {
		return MaterialRegistry.INSTANCE.createMaterial(-1, aName, aName);
	}

	// ========================================================================================
	// Single step queries
	// ========================================================================================

	@Test
	public void singleStepIronOreCrushesIntoHematite() {
		// Chain assertion 1: upstream MT.java:1973 "Fe.setOreMultiplier(3).setCrushing(Fe2O3, U)" (ported MT.java:2815).
		assertSame(MT.Fe2O3, MaterialGraph.conversionTarget(MT.Fe, Process.CRUSHING));
		OreDictMaterialStack tStep = MaterialGraph.singleStep(MT.Fe, Process.CRUSHING);
		assertSame(MT.Fe2O3, tStep.mMaterial);
		assertEquals(U, tStep.mAmount);
	}

	@Test
	public void singleStepTungstenOreCrushesIntoScheelite() {
		// Chain assertion 2: upstream MT.java:1976 "W.setOreMultiplier(2).setCrushing(OREMATS.Scheelite, U)" (ported MT.java:2818).
		assertSame(MT.OREMATS.Scheelite, MaterialGraph.conversionTarget(MT.W, Process.CRUSHING));
		assertEquals(U, MaterialGraph.singleStep(MT.W, Process.CRUSHING).mAmount);
	}

	@Test
	public void singleStepChalkSmeltsIntoCalcite() {
		// Chain assertion 3: upstream MT.java:1233 "Chalk ... .setSmelting(CaCO3, 2*U3)" (ported MT.java:2072).
		assertSame(MT.CaCO3, MaterialGraph.conversionTarget(MT.Chalk, Process.SMELTING));
		assertEquals(2 * U3, MaterialGraph.singleStep(MT.Chalk, Process.SMELTING).mAmount);
	}

	@Test
	public void singleStepIceSmeltsIntoWater() {
		// Chain assertion 4: upstream MT.java:1013 "Ice ... .setSmelting(H2O, U)" (ported MT.java:1884).
		assertSame(MT.H2O, MaterialGraph.conversionTarget(MT.Ice, Process.SMELTING));
		assertEquals(U, MaterialGraph.singleStep(MT.Ice, Process.SMELTING).mAmount);
	}

	@Test
	public void singleStepDiamondSmeltsAndBurns() {
		// Chain assertion 5: upstream MT.java:208 diamond factory ".setSmelting(C, 2*U)" + ".setBurning(Ash, U)".
		assertSame(MT.C, MaterialGraph.conversionTarget(MT.Diamond, Process.SMELTING));
		assertEquals(2 * U, MaterialGraph.singleStep(MT.Diamond, Process.SMELTING).mAmount);
		assertSame(MT.Ash, MaterialGraph.conversionTarget(MT.Diamond, Process.BURNING));
		assertEquals(U, MaterialGraph.singleStep(MT.Diamond, Process.BURNING).mAmount);
	}

	@Test
	public void selfReferencingDefaultIsNotAConversion() {
		// Upstream OreDictMaterial.java:284 defaults every mTarget* to "OM.stack(this, U)"; the
		// self-target test "mTargetCrushing.mMaterial == aMaterial" is how upstream detects "no
		// conversion" (LanguageHandler.java:572, OreDictMaterialCondition.java:49 CRUSHING condition).
		// Fe2O3 never gets a setCrushing call, so its crushing target is itself.
		assertNull(MaterialGraph.conversionTarget(MT.Fe2O3, Process.CRUSHING));
		OreDictMaterialStack tStep = MaterialGraph.singleStep(MT.Fe2O3, Process.CRUSHING);
		assertSame(MT.Fe2O3, tStep.mMaterial);
		assertEquals(U, tStep.mAmount);
	}

	@Test
	public void burningDefaultsToZeroAmountSelfStack() {
		// Upstream OreDictMaterial.java:292: "mTargetBurning = OM.stack(this, 0)" - the remaining
		// Material when being burned. Materials without setBurning keep the zero-amount self stack.
		OreDictMaterialStack tStep = MaterialGraph.singleStep(MT.Fe, Process.BURNING);
		assertSame(MT.Fe, tStep.mMaterial);
		assertEquals(0, tStep.mAmount);
		assertNull(MaterialGraph.conversionTarget(MT.Fe, Process.BURNING));
	}

	@Test
	public void generifyingChainOfMeteoriticSteels() {
		// Chain assertion 6: upstream MT.java:1739 "MeteoricBlackSteel ... .setGenerifying(BlackSteel)"
		// and :1740 "MeteoricBlueSteel ... .setGenerifying(BlueSteel)"; setGenerifying fixes the
		// amount to U (upstream OreDictMaterial.java:881-887).
		assertSame(MT.BlackSteel, MaterialGraph.conversionTarget(MT.MeteoricBlackSteel, Process.GENERIFYING));
		assertSame(MT.BlueSteel, MaterialGraph.conversionTarget(MT.MeteoricBlueSteel, Process.GENERIFYING));
		assertEquals(U, MaterialGraph.singleStep(MT.MeteoricBlackSteel, Process.GENERIFYING).mAmount);
	}

	@Test
	public void allTwelveProcessFamiliesAreBound() {
		// Upstream OreDictMaterial.java:283-295 (12 forward stacks) / :299-310 (12 reverse sets).
		assertEquals(12, Process.values().length);
		for (Process tProcess : Process.values()) {
			assertNotNull(MaterialGraph.singleStep(MT.Fe, tProcess));
			assertNotNull(MaterialGraph.singleStep(MT.Fe, tProcess).mMaterial);
		}
	}

	// ========================================================================================
	// Chain expansion
	// ========================================================================================

	@Test
	public void ironCrushingChainTerminatesAtSelfLoop() {
		// Fe --CRUSHING--> Fe2O3 (upstream MT.java:1973); Fe2O3 targets itself (default
		// OreDictMaterial.java:284), so the chain is exactly one hop.
		List<OreDictMaterialStack> tPath = MaterialGraph.expandChain(MT.Fe, Process.CRUSHING, 8);
		assertEquals(1, tPath.size());
		assertSame(MT.Fe2O3, tPath.get(0).mMaterial);
		assertEquals(U, tPath.get(0).mAmount);
	}

	@Test
	public void chalkSmeltingChainTerminatesAtSelfLoop() {
		// Chalk --SMELTING--> CaCO3 (upstream MT.java:1233); CaCO3 has no smelting target of its own.
		List<OreDictMaterialStack> tPath = MaterialGraph.expandChain(MT.Chalk, Process.SMELTING, 8);
		assertEquals(1, tPath.size());
		assertSame(MT.CaCO3, tPath.get(0).mMaterial);
		assertEquals(2 * U3, tPath.get(0).mAmount);
	}

	@Test
	public void chainExpansionRespectsDepthLimit() {
		// The depth limit is the hard "no-blowup" bound on top of the visited-set termination rule.
		assertTrue(MaterialGraph.expandChain(MT.Fe, Process.CRUSHING, 0).isEmpty());
		assertEquals(1, MaterialGraph.expandChain(MT.Fe, Process.CRUSHING, 1).size());
		assertThrows(IllegalArgumentException.class, () -> MaterialGraph.expandChain(MT.Fe, Process.CRUSHING, -1));
	}

	@Test
	public void chainExpansionDetectsSyntheticCycle() {
		// PORT-SIDE NEW scenario: no real MT pair forms a conversion cycle, so wire one with
		// synthetic ID=-1 materials via the public setCrushing (upstream OreDictMaterial.java:781-787).
		OreDictMaterial tA = testMaterial("MGGraphCycleA"), tB = testMaterial("MGGraphCycleB");
		tA.setCrushing(tB, U);
		tB.setCrushing(tA, U);
		List<OreDictMaterialStack> tPath = MaterialGraph.expandChain(tA, Process.CRUSHING, 16);
		// A -> B traversed, then B -> A is refused because A was already visited; the closing edge
		// is not appended and the walk stops instead of looping to the depth limit.
		assertEquals(1, tPath.size());
		assertSame(tB, tPath.get(0).mMaterial);
		// The reverse query across the same synthetic edge (upstream UT.java:1047 predicate).
		Map<OreDictMaterial, Long> tTargeting = MaterialGraph.targeting(tB, Process.CRUSHING);
		assertEquals(1, tTargeting.size());
		assertEquals(U, tTargeting.get(tA).longValue());
	}

	@Test
	public void chainExpansionTerminatesAcrossTheWholeDataset() {
		// Termination proof over real data: every registered material x every process family must
		// complete within the bound (visited-set rule 2 catches cycles, rule 1 catches self loops,
		// rule 3 is the hard stop). Also sanity-check that the dataset is the full one from card 3.
		int tMaterials = 0;
		for (OreDictMaterial tMat : MaterialRegistry.INSTANCE.MATERIAL_MAP.values()) {
			if (tMat == null) continue;
			tMaterials++;
			for (Process tProcess : Process.values()) {
				List<OreDictMaterialStack> tPath = MaterialGraph.expandChain(tMat, tProcess, 128);
				assertTrue(tPath.size() <= 128, tMat.mNameInternal + " / " + tProcess + " exceeded the bound");
				for (OreDictMaterialStack tStack : tPath) assertNotNull(tStack.mMaterial);
			}
		}
		assertTrue(tMaterials > 1200, "expected the full MT dataset, got " + tMaterials);
	}

	// ========================================================================================
	// Reverse queries (UT.java:1047 semantics)
	// ========================================================================================

	@Test
	public void reverseQueryIronOreIsExactSingleton() {
		// Chain assertion 7 (reverse): only Fe crushes into Fe2O3 (upstream MT.java:1973), so the
		// UT.java:1047 predicate must yield exactly {Fe: U}.
		Map<OreDictMaterial, Long> tTargeting = MaterialGraph.targeting(MT.Fe2O3, Process.CRUSHING);
		assertEquals(1, tTargeting.size());
		assertEquals(U, tTargeting.get(MT.Fe).longValue());
	}

	@Test
	public void reverseQueryCalciteFindsChalk() {
		// Reverse of upstream MT.java:1233: Chalk --SMELTING--> CaCO3 with 2*U3.
		Map<OreDictMaterial, Long> tTargeting = MaterialGraph.targeting(MT.CaCO3, Process.SMELTING);
		assertEquals(2 * U3, tTargeting.get(MT.Chalk).longValue());
	}

	@Test
	public void reverseQueryExcludesTheSelfEntryOfTheReverseSet() {
		// Upstream OreDictMaterial.java:299-310 initializes every mTargeted* set with the material
		// ITSELF; the "tMat != aMat" guard of UT.java:1047 filters it back out. NULL has no inbound
		// edges at all, so its reverse map must be empty, not {NULL: U}.
		assertTrue(MaterialGraph.targeting(MT.NULL, Process.CRUSHING).isEmpty());
		// Same guard on a real material that is a conversion target: Fe2O3 must not target itself.
		assertFalse(MaterialGraph.targeting(MT.Fe2O3, Process.CRUSHING).containsKey(MT.Fe2O3));
	}

	@Test
	public void reverseQueryExcludesZeroAmountDisabledEdges() {
		// setCrushing(material, 0) disables the process (javadoc upstream OreDictMaterial.java:780);
		// the "tMat.mTargetCrushing.has(aMat)" guard of UT.java:1047 requires a positive amount
		// (OreDictMaterialStack.has, ported :85-87), so a disabled edge never shows up.
		OreDictMaterial tSrc = testMaterial("MGGraphZeroSrc"), tDst = testMaterial("MGGraphZeroDst");
		tSrc.setCrushing(tDst, 0);
		assertFalse(MaterialGraph.targeting(tDst, Process.CRUSHING).containsKey(tSrc));
		assertEquals(0, MaterialGraph.singleStep(tSrc, Process.CRUSHING).mAmount);
	}

	@Test
	public void reverseQuerySkipsAliasMaterialsViaTargetRegistration() {
		// The "tMat.mTargetRegistration == tMat" guard of UT.java:1047 skips materials whose
		// registration points elsewhere (setRegistration, upstream OreDictMaterial.java:378-383).
		OreDictMaterial tReal = testMaterial("MGGraphAliasReal"), tAlias = testMaterial("MGGraphAlias");
		OreDictMaterial tDst = testMaterial("MGGraphAliasDst");
		tAlias.setRegistration(tReal); // alias.mTargetRegistration now points at tReal, not at itself
		OreDictMaterial tControl = testMaterial("MGGraphAliasControl");
		tAlias.setCrushing(tDst, U);
		tControl.setCrushing(tDst, U);
		Map<OreDictMaterial, Long> tTargeting = MaterialGraph.targeting(tDst, Process.CRUSHING);
		assertFalse(tTargeting.containsKey(tAlias), "alias materials must be skipped");
		assertTrue(tTargeting.containsKey(tControl));
		assertEquals(U, tTargeting.get(tControl).longValue());
	}

	@Test
	public void reverseQueryGenerifying() {
		// Reverse of upstream MT.java:1739: MeteoricBlackSteel --GENERIFYING--> BlackSteel.
		Map<OreDictMaterial, Long> tTargeting = MaterialGraph.targeting(MT.BlackSteel, Process.GENERIFYING);
		assertTrue(tTargeting.containsKey(MT.MeteoricBlackSteel));
		assertEquals(U, tTargeting.get(MT.MeteoricBlackSteel).longValue());
	}

	// ========================================================================================
	// Alloy composition reference graph
	// ========================================================================================

	@Test
	public void bronzeComposition() {
		// Chain assertion 8: upstream MT.java:1705 "Bronze ... .uumAloy( 0, Cu, 3*U, Sn, 1*U)".
		// setMcfg with divider 0 derives the common divider 4 from the total amount
		// (upstream OreDictMaterial.java:527-534). getComponents returns the per-unit amounts
		// (3*U/4 Cu + U/4 Sn), getUndividedComponents the raw recipe amounts.
		assertEquals(4, MaterialGraph.alloyCommonDivider(MT.Bronze));
		List<OreDictMaterialStack> tUndivided = MaterialGraph.alloyUndividedComponents(MT.Bronze);
		assertEquals(2, tUndivided.size());
		assertSame(MT.Cu, tUndivided.get(0).mMaterial);
		assertEquals(3 * U, tUndivided.get(0).mAmount);
		assertSame(MT.Sn, tUndivided.get(1).mMaterial);
		assertEquals(U, tUndivided.get(1).mAmount);
		List<OreDictMaterialStack> tPerUnit = MaterialGraph.alloyComponents(MT.Bronze);
		assertEquals(3 * U / 4, tPerUnit.get(0).mAmount);
		assertEquals(U4, tPerUnit.get(1).mAmount);
	}

	@Test
	public void invarComposition() {
		// Chain assertion 9: upstream MT.java:1754 "Invar ... .uumAloy( 0, WroughtIron, 2*U, Ni, 1*U)"
		// - divider 3, two thirds wrought iron + one third nickel.
		assertEquals(3, MaterialGraph.alloyCommonDivider(MT.Invar));
		List<OreDictMaterialStack> tUndivided = MaterialGraph.alloyUndividedComponents(MT.Invar);
		assertEquals(2, tUndivided.size());
		assertSame(MT.WroughtIron, tUndivided.get(0).mMaterial);
		assertEquals(2 * U, tUndivided.get(0).mAmount);
		assertSame(MT.Ni, tUndivided.get(1).mMaterial);
		assertEquals(U, tUndivided.get(1).mAmount);
	}

	@Test
	public void electrumCompositionMatchesTheInterfaceDocExample() {
		// Chain assertion 10: upstream MT.java:1691 "Electrum ... .uumAloy( 0, Ag, 1*U, Au, 1*U)".
		// This is exactly the example of the getComponents contract (upstream
		// IOreDictConfigurationComponent.java:42-44): Electrum = half a Unit Gold + half a Unit Silver.
		assertEquals(2, MaterialGraph.alloyCommonDivider(MT.Electrum));
		for (OreDictMaterialStack tStack : MaterialGraph.alloyComponents(MT.Electrum)) {
			assertEquals(U2, tStack.mAmount);
			assertTrue(tStack.mMaterial == MT.Ag || tStack.mMaterial == MT.Au);
		}
	}

	@Test
	public void nestedAlloyCompositionIsWalkable() {
		// Chain assertion 11: the steel family composes alloys out of alloys -
		// upstream MT.java:1715 "BlueSteel ... .setAloy( 0, SterlingSilver, 1*U, BismuthBronze, 1*U, Steel, 2*U, BlackSteel, 4*U)"
		// (divider 8), :1714 "BlackSteel ... .uumAloy( 0, Ni, 1*U, BlackBronze, 1*U, Steel, 3*U)"
		// (divider 5), :1713 "Steel ... .uumMcfg( 0, WroughtIron, 1*U)".
		List<OreDictMaterialStack> tBlue = MaterialGraph.alloyUndividedComponents(MT.BlueSteel);
		assertEquals(8, MaterialGraph.alloyCommonDivider(MT.BlueSteel));
		assertEquals(4, tBlue.size());
		assertSame(MT.Steel, tBlue.get(2).mMaterial);
		assertEquals(2 * U, tBlue.get(2).mAmount);
		assertSame(MT.BlackSteel, tBlue.get(3).mMaterial);

		List<OreDictMaterialStack> tBlack = MaterialGraph.alloyUndividedComponents(MT.BlackSteel);
		assertSame(MT.BlackBronze, tBlack.get(1).mMaterial);
		assertSame(MT.Steel, tBlack.get(2).mMaterial);

		// The scan-based containment query follows the same nesting: Electrum inside BlackBronze
		// (upstream MT.java:1706 "BlackBronze ... .uumAloy( 0, Cu, 3*U, Electrum, 2*U)").
		assertTrue(MaterialGraph.alloysContaining(MT.Electrum).containsKey(MT.BlackBronze));
		assertTrue(MaterialGraph.alloysContaining(MT.Steel).containsKey(MT.BlueSteel));
		assertTrue(MaterialGraph.alloysContaining(MT.BlackSteel).containsKey(MT.BlueSteel));
	}

	@Test
	public void steelComponentDataWithoutCreationRecipe() {
		// Steel gets mComponents via uumMcfg (upstream MT.java:1713) but no alloySimple() call, so
		// it has components yet NO entry in mAlloyCreationRecipes; the recipe
		// "Steel.addAlloyingRecipe(WroughtIron + Air)" comes from the hand-written block
		// (upstream MT.java:3348, ported MT.java:4043). The two query paths must reflect exactly that.
		assertEquals(1, MaterialGraph.alloyCommonDivider(MT.Steel));
		assertSame(MT.WroughtIron, MaterialGraph.alloyUndividedComponents(MT.Steel).get(0).mMaterial);
		assertTrue(MaterialGraph.alloyCreationRecipes(MT.Steel).size() >= 1);
		// The scan finds Invar (which went through alloySimple) but not Steel's own mComponents.
		assertTrue(MaterialGraph.alloysContaining(MT.WroughtIron).containsKey(MT.Invar));
		// The direct references find Steel (its addAlloyingRecipe ran inside MT.init).
		assertTrue(MaterialGraph.alloyComponentReferences(MT.WroughtIron).contains(MT.Steel));
	}

	@Test
	public void crucibleAlloyWiringCompletesTheReferenceGraph() {
		// Two-tier data, upstream-faithful: alloySimple (:426-429) fills mAlloyCreationRecipes but
		// NOT mAlloyComponentReferences/ALLOYS; the hand-written addAlloyingRecipe block
		// (upstream MT.java:3332-3349, ported :4034-4081) wires Fe/Steel/Bronze(via AnnealedCopper)/...;
		// GT_API_Post.java:816-821 completes the rest at postInit. Invar and Electrum have no
		// hand-written recipe, so before the wiring they are absent from ALLOYS.
		assertFalse(MaterialGraph.isRegisteredAlloy(MT.Invar));
		assertFalse(MaterialGraph.alloyComponentReferences(MT.WroughtIron).contains(MT.Invar));
		// Bronze IS pre-wired (:4067 "Bronze.addAlloyingRecipe(AnnealedCopper 3U + Sn 1U)") but its
		// uumAloy copper content (upstream MT.java:1705) is not referenced yet.
		assertTrue(MaterialGraph.isRegisteredAlloy(MT.Bronze));
		assertFalse(MaterialGraph.alloyComponentReferences(MT.Cu).contains(MT.Bronze));

		int tInvarRecipesBefore = MaterialGraph.alloyCreationRecipes(MT.Invar).size();
		assertTrue(tInvarRecipesBefore >= 1); // alloySimple recorded mComponents at declaration time (upstream :426-429)

		int tWired = MaterialGraph.applyCrucibleAlloyReferences();
		assertTrue(tWired > 0, "the CRUCIBLE_ALLOY pass must wire the uumAloy alloys");

		assertTrue(MaterialGraph.isRegisteredAlloy(MT.Invar));
		assertTrue(MaterialGraph.isRegisteredAlloy(MT.Electrum));
		assertTrue(MaterialGraph.alloyComponentReferences(MT.WroughtIron).contains(MT.Invar));
		assertTrue(MaterialGraph.alloyComponentReferences(MT.Cu).contains(MT.Bronze));
		assertTrue(MaterialGraph.alloyComponentReferences(MT.Ni).contains(MT.Invar));

		// VERBATIM QUIRK (upstream :820 has no dedup guard): the current mComponents instance is
		// appended again even though alloySimple already recorded it at declaration time.
		List<IOreDictConfigurationComponent> tRecipes = MaterialGraph.alloyCreationRecipes(MT.Invar);
		assertEquals(tInvarRecipesBefore + 1, tRecipes.size());
		assertSame(MT.Invar.mComponents, tRecipes.get(tInvarRecipesBefore));

		// And like the upstream loop, calling it again appends yet another copy.
		assertTrue(MaterialGraph.applyCrucibleAlloyReferences() > 0);
		assertEquals(tInvarRecipesBefore + 2, MaterialGraph.alloyCreationRecipes(MT.Invar).size());
		assertSame(MT.Invar.mComponents, MaterialGraph.alloyCreationRecipes(MT.Invar).get(tInvarRecipesBefore + 1));
	}

	@Test
	public void nullAndBoundaryInputs() {
		// Query surface is total: null materials and the NULL material yield empty results.
		assertNull(MaterialGraph.singleStep(null, Process.CRUSHING));
		assertNull(MaterialGraph.conversionTarget(null, Process.CRUSHING));
		assertNull(MaterialGraph.conversionTarget(MT.Fe, null));
		assertTrue(MaterialGraph.expandChain(null, Process.CRUSHING, 8).isEmpty());
		assertTrue(MaterialGraph.expandChain(MT.Fe, null, 8).isEmpty());
		assertTrue(MaterialGraph.targeting(null, Process.CRUSHING).isEmpty());
		assertTrue(MaterialGraph.targeting(MT.Fe, null).isEmpty());
		assertTrue(MaterialGraph.alloyComponents(null).isEmpty());
		assertTrue(MaterialGraph.alloyUndividedComponents(null).isEmpty());
		assertEquals(0, MaterialGraph.alloyCommonDivider(null));
		assertTrue(MaterialGraph.alloyComponentReferences(null).isEmpty());
		assertTrue(MaterialGraph.alloysContaining(null).isEmpty());
		assertTrue(MaterialGraph.alloyCreationRecipes(null).isEmpty());
		assertFalse(MaterialGraph.isRegisteredAlloy(null));
		// MT.NULL never gets components or conversions, so every graph query on it is empty.
		assertTrue(MaterialGraph.alloyComponents(MT.NULL).isEmpty());
		assertTrue(MaterialGraph.targeting(MT.NULL, Process.CRUSHING).isEmpty());
		assertTrue(MaterialGraph.expandChain(MT.NULL, Process.CRUSHING, 4).isEmpty());
	}

	@Test
	public void stackEqualityQuirksInGraphContext() {
		// Upstream OreDictMaterialStack.java:82 ("aObject == mMaterial" -> true) and :84 (a negative
		// amount acts as an amount wildcard in equals), ported verbatim at :91/:93. Graph queries
		// deliberately compare via field identity ("mMaterial != aMaterial") and via has()
		// (amount > 0) instead of equals, so these quirks cannot leak into the results.
		OreDictMaterialStack tStack = new OreDictMaterialStack(MT.Fe, U);
		assertTrue(tStack.equals(MT.Fe)); // stack equals its own material object
		assertTrue(new OreDictMaterialStack(MT.Fe, -1).equals(tStack));
		assertTrue(tStack.equals(new OreDictMaterialStack(MT.Fe, -1)));
		// has() stays amount-strict, which is what keeps disabled edges out of targeting().
		assertFalse(new OreDictMaterialStack(MT.Fe, -1).has(MT.Fe));
		assertFalse(tStack.has(MT.Fe2O3));
		// That is also why targeting() keys by OreDictMaterial (upstream UT.java:1047 tMap.put(tMat, ...)),
		// never by stacks: equal-material stacks would collapse into one key.
		assertEquals(tStack.hashCode(), new OreDictMaterialStack(MT.Fe, -1).hashCode());
	}
}
