/**
 * Tests for task p26-crucible-physics-smeltery: the shared crucible physics core over the
 * real MT dataset (the MaterialGraphTest posture: MT.init() + the CRUCIBLE_ALLOY wiring).
 *
 * Table-driven across the two physics forms (arch ruling ②): SMALL = the 1.7.10 small
 * Smeltery constants (MultiTileEntitySmeltery.java:76-78: MAX_AMOUNT 16*U,
 * HEAT_RESISTANCE_BONUS 1.25, GAS_RANGE 3, the :262/:318 explode ceiling 6), LARGE = the
 * card-B consumption face (432*U / 1.10 / 8 / 5) — the SAME functions must answer both.
 *
 * Every assertion cites its upstream line; the arithmetic itself is CruciblePhysics.
 */
package gregapi.util;

import static gregapi.data.CS.U;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.data.MT;
import gregapi.data.TD;
import gregapi.oredict.MaterialGraph;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.util.CruciblePhysics.AlloyResult;
import gregapi.util.CruciblePhysics.Params;

public class CruciblePhysicsTest {

	/** The WEIGHT_AIR gate value (MT.java:111) — materials at/below this density evaporate harmlessly. */
	private static final double WEIGHT_AIR_G_PER_CUBIC_CENTIMETER = 0.0012;

	@BeforeAll
	public static void initMT() {
		// Other test classes may leave the shared registry closed; MT.init()'s re-init path needs it open.
		MaterialRegistry.INSTANCE.open();
		MT.init();
		// the crucible alloy scan walks mAlloyComponentReferences — the postInit wiring
		// (GT_API_Post.java:816-821, the MaterialGraph.applyCrucibleAlloyReferences port)
		MaterialGraph.applyCrucibleAlloyReferences();
	}

	// ====================================================================================
	// the two physics forms (the parameter-face table)
	// ====================================================================================

	private static final Params SMALL = Params.SMALL;
	private static final Params LARGE = Params.LARGE;

	/** The arch-table pin: the small Smeltery face verbatim (MultiTileEntitySmeltery.java:76-78). */
	@Test
	public void smallFormConstants() {
		assertEquals(16 * U, SMALL.maxAmount());
		assertEquals(1.25, SMALL.heatResistanceBonus(), 1e-9);
		assertEquals(6, SMALL.explosionPower());
		assertEquals(3, SMALL.gasRange());
		assertEquals(100, SMALL.kgPerEnergy()); // the :77 KG_PER_ENERGY
	}

	/** The card-B consumption face pinned here so the signature stays stable across B/C. */
	@Test
	public void largeFormConstants() {
		assertEquals(432 * U, LARGE.maxAmount());
		assertEquals(1.10, LARGE.heatResistanceBonus(), 1e-9);
		assertEquals(8, LARGE.explosionPower());
		assertEquals(5, LARGE.gasRange());
		assertEquals(100, LARGE.kgPerEnergy());
	}

	// ====================================================================================
	// tRequiredEnergy = 1 + Σ热质量 / 100 (:301)
	// ====================================================================================

	@Test
	public void requiredEnergyIsOnePlusWeightOverKgPerEnergy() {
		// :301 verbatim shape: 1 + (long)(weight / 100) — one HU heats kgPerEnergy kg by 1 K
		assertEquals(1, CruciblePhysics.requiredEnergy(0, 100)); // the empty crucible still pays 1 HU/K
		assertEquals(1, CruciblePhysics.requiredEnergy(99.99, 100));
		assertEquals(2, CruciblePhysics.requiredEnergy(100, 100));
		assertEquals(3, CruciblePhysics.requiredEnergy(222.222222, 100)); // 2 units of density-1 material
		// the KG_PER_ENERGY constant is shared by both forms — the table only varies maxAmount/bonus/power/range
	}

	// ====================================================================================
	// the thermal mass weighted temperature equilibrium (:330-352)
	// ====================================================================================

	@Test
	public void emptyCrucibleTakesTheIncomingTemperature() {
		// w1 = 0 → units(|Δ|, w2, 0, F) = 0 → the temperature lands exactly on the incoming one
		List<OreDictMaterialStack> tContent = new ArrayList<>();
		List<OreDictMaterialStack> tIncoming = List.of(new OreDictMaterialStack(MT.Water, U));
		CruciblePhysics.AddResult tResult = CruciblePhysics.addStacks(tContent, tIncoming, 300, 500, 0, SMALL);
		assertTrue(tResult.added());
		assertEquals(300, tResult.temperature());
		assertEquals(1, tContent.size());
		assertSame(MT.Water, tContent.get(0).mMaterial);
	}

	@Test
	public void thermalMassBlendHitsTheExactUpstreamArithmetic() {
		// :333 verbatim: newTemp = aTemperature + sign * units(|own - aTemperature|, w1+w2, w1, F)
		// 1U water = density 1.0 → 111.111.. kg (getWeight = density * 111.111111 * amount / U).
		// w1 = w2 = 1U water: units(200, 222, 111, F) = (200*111)/222 = 100 → 300 + 100 = 400.
		List<OreDictMaterialStack> tContent = new ArrayList<>(List.of(new OreDictMaterialStack(MT.Water, U)));
		List<OreDictMaterialStack> tIncoming = List.of(new OreDictMaterialStack(MT.Water, U));
		CruciblePhysics.AddResult tResult = CruciblePhysics.addStacks(tContent, tIncoming, 300, 500, 0, SMALL);
		assertTrue(tResult.added());
		assertEquals(400, tResult.temperature());
		assertEquals(2 * U, tContent.get(0).mAmount); // the addToList merge
	}

	@Test
	public void capacityGateRejectsOverflow() {
		// :331 — total(content) + total(incoming) > maxAmount refuses the WHOLE addition
		List<OreDictMaterialStack> tContent = new ArrayList<>(List.of(new OreDictMaterialStack(MT.Water, SMALL.maxAmount())));
		List<OreDictMaterialStack> tIncoming = List.of(new OreDictMaterialStack(MT.Water, U));
		CruciblePhysics.AddResult tSmall = CruciblePhysics.addStacks(tContent, tIncoming, 300, 300, 0, SMALL);
		assertFalse(tSmall.added(), "17 units do not fit the 16-unit small smeltery");
		assertEquals(1, tContent.size(), "the refused batch must not partially enter");

		List<OreDictMaterialStack> tBigContent = new ArrayList<>(List.of(new OreDictMaterialStack(MT.Water, SMALL.maxAmount())));
		CruciblePhysics.AddResult tLarge = CruciblePhysics.addStacks(tBigContent, tIncoming, 300, 300, 0, LARGE);
		assertTrue(tLarge.added(), "the same 17 units fit the 432-unit large crucible");
	}

	// ====================================================================================
	// the heat/cool tick (:301-313)
	// ====================================================================================

	@Test
	public void heatTickPaysEnergyAndRaisesTemperature() {
		// :305-309 — conversions = energy / requiredEnergy; each conversion = +1 K, cooldown resets to 100
		double tWeight = 222.222222; // required energy 3
		CruciblePhysics.TickResult tResult = CruciblePhysics.tickHeat(500, 3, 300, tWeight, 0, 100);
		assertEquals(501, tResult.temperature());
		assertEquals(0, tResult.energy());
		assertEquals(100, tResult.cooldown());
	}

	@Test
	public void outOfSupplyCooldownDripsTowardEnvironment() {
		// :311 — after the 100-tick supply window (cooldown expired), every 10 ticks move ONE Kelvin toward env
		CruciblePhysics.TickResult tResult = CruciblePhysics.tickHeat(500, 0, 300, 222.222222, 0, 100);
		assertEquals(499, tResult.temperature(), "cooling down toward the environment");
		assertEquals(10, tResult.cooldown(), "the next drip is 10 ticks away");

		CruciblePhysics.TickResult tWarm = CruciblePhysics.tickHeat(500, 0, 600, 222.222222, 0, 100);
		assertEquals(501, tWarm.temperature(), "a hotter environment warms the crucible back up");

		CruciblePhysics.TickResult tNothing = CruciblePhysics.tickHeat(500, 0, 300, 222.222222, 100, 100);
		assertEquals(500, tNothing.temperature(), "inside the 100-tick supply window nothing cools");
	}

	@Test
	public void temperatureFloorIsMin200Environment() {
		// :313 — the hard clamp applies EVERY tick: temperature >= min(200, env)
		CruciblePhysics.TickResult tFloor = CruciblePhysics.tickHeat(1, 0, 300, 222.222222, 1, 100);
		assertEquals(200, tFloor.temperature(), "env above 200 K: the crucible never sits below 200 K");
		CruciblePhysics.TickResult tColdEnv = CruciblePhysics.tickHeat(150, 0, 150, 222.222222, 1, 100);
		assertEquals(150, tColdEnv.temperature(), "env below 200 K: the floor follows the environment");
		CruciblePhysics.TickResult tHot = CruciblePhysics.tickHeat(500, 0, 4000, 222.222222, 100, 100);
		assertEquals(500, tHot.temperature(), "above the floor nothing clamps");
	}

	// ====================================================================================
	// the temperature limits (:360-362 + :324)
	// ====================================================================================

	@Test
	public void temperatureMaxIsShellMeltingPointTimesBonus() {
		// the parameter-face pair: the same shell answers differently per form
		long tStone = CruciblePhysics.temperatureMax(MT.Stone, SMALL.heatResistanceBonus());
		assertEquals((long)(MT.Stone.mMeltingPoint * 1.25), tStone);
		long tStoneLarge = CruciblePhysics.temperatureMax(MT.Stone, LARGE.heatResistanceBonus());
		assertEquals((long)(MT.Stone.mMeltingPoint * 1.10), tStoneLarge);
		assertTrue(tStone > tStoneLarge, "the small 1.25 bonus tolerates more than the large 1.10 one");
	}

	@Test
	public void meltDownWarningIsWithin100KelvinOfTheCeiling() {
		// :324 — mMeltDown = (mTemperature + 100 > getTemperatureMax)
		assertTrue(CruciblePhysics.isMeltDownWarning(1700, 1799));
		assertFalse(CruciblePhysics.isMeltDownWarning(1700, 1800));
	}

	// ====================================================================================
	// the phase gates (:246-284) — smelting/solidifying/boiling/acid
	// ====================================================================================

	@Test
	public void smeltingGateConvertsDustToMoltenTarget() {
		// :271-273 — crossing the melting point upward converts to mTargetSmelting.
		// Gilded Iron smelts into Fe (MT.java dataset: GildedIron.setSmelting(Fe, U)).
		OreDictMaterial tGilded = MT.GildedIron;
		assertSame(MT.Fe, tGilded.mTargetSmelting.mMaterial);
		List<OreDictMaterialStack> tContent = new ArrayList<>(List.of(new OreDictMaterialStack(tGilded, U)));
		long tHot = Math.max(tGilded.mMeltingPoint, MT.Fe.mMeltingPoint);
		CruciblePhysics.phaseGates(tContent, tHot, tGilded.mMeltingPoint - 1, false, false, SMALL);
		assertEquals(1, tContent.size());
		assertSame(MT.Fe, tContent.get(0).mMaterial, "the gilded iron becomes iron");
		assertEquals(U, tContent.get(0).mAmount);
	}

	@Test
	public void solidifyingGateKeepsTheMetalNotTheDust() {
		// :274-276 — falling below the melting point converts to mTargetSolidifying: molten
		// iron solidifies into iron, it does NOT turn back into hematite dust.
		List<OreDictMaterialStack> tContent = new ArrayList<>(List.of(new OreDictMaterialStack(MT.Fe, U)));
		OreDictMaterial tTarget = MT.Fe.mTargetSolidifying.mMaterial;
		CruciblePhysics.phaseGates(tContent, MT.Fe.mMeltingPoint - 1, MT.Fe.mMeltingPoint, false, false, SMALL);
		assertEquals(1, tContent.size());
		assertSame(tTarget, tContent.get(0).mMaterial, "the solidifying target, never the dust");
		assertEquals(U, tContent.get(0).mAmount);
	}

	@Test
	public void boilingGateEvaporatesAndScoresGasAndFire() {
		// :254-258 — water above its boiling point evaporates with fizz; hot enough boiling
		// points apply gas damage and spawn fire
		List<OreDictMaterialStack> tContent = new ArrayList<>(List.of(new OreDictMaterialStack(MT.Water, U)));
		CruciblePhysics.PhaseOutcome tOutcome = CruciblePhysics.phaseGates(tContent, 5000, 300, false, false, SMALL);
		assertTrue(tContent.isEmpty(), "the water boiled off");
		assertTrue(tOutcome.fizz());
		assertFalse(tOutcome.acidDestroyed());
		assertEquals(0, tOutcome.explosionStrength());
	}

	@Test
	public void explosiveMaterialDetonatesAtTheFormPower() {
		// :259-262 — an EXPLOSIVE boil clears the crucible and explodes at
		// scale(amount, maxAmount, explosionPower, F): a full-capacity dose maxes the scale.
		OreDictMaterial tExplosive = anyMaterialWith(TD.Properties.EXPLOSIVE);
		assertNotNull(tExplosive, "the dataset must carry an explosive material");
		long tBoiling = Math.max(tExplosive.mBoilingPoint, 5000);

		List<OreDictMaterialStack> tSmallContent = new ArrayList<>(List.of(new OreDictMaterialStack(tExplosive, SMALL.maxAmount())));
		CruciblePhysics.PhaseOutcome tSmall = CruciblePhysics.phaseGates(tSmallContent, tBoiling, 300, false, false, SMALL);
		assertTrue(tSmallContent.isEmpty());
		assertEquals(6, tSmall.explosionStrength(), "the small smeltery tops out at power 6");

		List<OreDictMaterialStack> tLargeContent = new ArrayList<>(List.of(new OreDictMaterialStack(tExplosive, LARGE.maxAmount())));
		CruciblePhysics.PhaseOutcome tLarge = CruciblePhysics.phaseGates(tLargeContent, tBoiling, 300, false, false, LARGE);
		assertTrue(tLargeContent.isEmpty());
		assertEquals(8, tLarge.explosionStrength(), "the large crucible tops out at power 8");
	}

	@Test
	public void acidMaterialDestroysANonAcidproofCrucible() {
		// :265-270 — an ACID content in a non-acidproof crucible eats everything
		OreDictMaterial tAcid = anyMaterialWith(TD.Properties.ACID);
		assertNotNull(tAcid, "the dataset must carry an acid");
		List<OreDictMaterialStack> tContent = new ArrayList<>(List.of(
				new OreDictMaterialStack(tAcid, U),
				new OreDictMaterialStack(MT.Fe, U)));
		CruciblePhysics.PhaseOutcome tOutcome = CruciblePhysics.phaseGates(tContent, 300, 300, false, false, SMALL);
		assertTrue(tContent.isEmpty(), "the acid destroyed everything");
		assertTrue(tOutcome.acidDestroyed());
		// :265 — an acidproof crucible survives
		List<OreDictMaterialStack> tProofContent = new ArrayList<>(List.of(new OreDictMaterialStack(tAcid, U)));
		CruciblePhysics.PhaseOutcome tProof = CruciblePhysics.phaseGates(tProofContent, 300, 300, false, true, SMALL);
		assertFalse(tProof.acidDestroyed());
		assertEquals(1, tProofContent.size());
	}

	@Test
	public void lighterThanAirMaterialsEvaporate() {
		// :251 — density at/below 0.0012 g/cm³ never accumulates (MT.Air itself drops out
		// through the :249 identity arm WITHOUT fizz, so the test picks another such gas)
		OreDictMaterial tGas = null;
		for (OreDictMaterial tMaterial : MaterialRegistry.INSTANCE.MATERIAL_ARRAY) {
			if (tMaterial == null || tMaterial.mID < 0 || tMaterial == MT.Air || tMaterial == MT.NULL) continue;
			if (tMaterial.mGramPerCubicCentimeter > 0 && tMaterial.mGramPerCubicCentimeter <= WEIGHT_AIR_G_PER_CUBIC_CENTIMETER) {tGas = tMaterial; break;}
		}
		assertNotNull(tGas, "the dataset must carry another lighter-than-air material besides Air");
		List<OreDictMaterialStack> tContent = new ArrayList<>(List.of(new OreDictMaterialStack(tGas, U)));
		CruciblePhysics.PhaseOutcome tOutcome = CruciblePhysics.phaseGates(tContent, 300, 300, false, false, SMALL);
		assertTrue(tContent.isEmpty());
		assertTrue(tOutcome.fizz());
	}

	// ====================================================================================
	// the alloy scan (:186-244) — zero table lookups, all off the material graph
	// ====================================================================================

	@Test
	public void alloyScanFormsInvarFromItsComponents() {
		// Invar's components come off the graph itself (the zero-hardcode discipline);
		// every component molten (tNonMolten gate :208/:221) → the alloy forms.
		OreDictMaterial tInvar = MT.Invar;
		List<OreDictMaterialStack> tComponents = new ArrayList<>(MaterialGraph.alloyUndividedComponents(tInvar));
		assertFalse(tComponents.isEmpty(), "Invar must declare its composition");
		long tCommonDivider = MaterialGraph.alloyCommonDivider(tInvar);
		assertTrue(tCommonDivider > 0);

		long tHot = tInvar.mMeltingPoint;
		for (OreDictMaterialStack tComponent : tComponents) tHot = Math.max(tHot, tComponent.mMaterial.mMeltingPoint);

		List<OreDictMaterialStack> tContent = new ArrayList<>();
		for (OreDictMaterialStack tComponent : tComponents) new OreDictMaterialStack(tComponent.mMaterial, tComponent.mAmount).addToList(tContent);

		AlloyResult tResult = CruciblePhysics.alloyScan(tContent, tHot);
		assertSame(tInvar, tResult.alloy(), "the scan must find Invar");
		// :199/:213 — the needed amounts are WHOLE-UNIT COUNTS (max(1, raw/U)), so the
		// conversions value rides the U scale: Invar = 2U WI + 1U Ni per round → 1 round = U.
		assertEquals(U, tResult.conversions(), "one recipe round at the U scale");

		// the :234-244 consumption half — the components go out, commonDivider × conversions Invar comes in
		// (the zero-amount component shells stay until the next :249 sweep, so the assertions read
		// the positive-amount content)
		CruciblePhysics.applyAlloy(tContent, tResult.alloy(), tResult.conversions());
		assertEquals(tCommonDivider * U, total(tContent), "the Invar output preserves the total mass");
		OreDictMaterialStack tPositive = null;
		for (OreDictMaterialStack tStack : tContent) if (tStack.mAmount > 0) {
			assertNull(tPositive, "exactly one positive stack remains");
			tPositive = tStack;
		}
		assertNotNull(tPositive);
		assertSame(tInvar, tPositive.mMaterial);
	}

	@Test
	public void alloyScanNeedsTheAlloyMolten() {
		// :195 — the candidate alloy below its own melting point never forms
		OreDictMaterial tInvar = MT.Invar;
		List<OreDictMaterialStack> tComponents = MaterialGraph.alloyUndividedComponents(tInvar);
		List<OreDictMaterialStack> tContent = new ArrayList<>();
		for (OreDictMaterialStack tComponent : tComponents) new OreDictMaterialStack(tComponent.mMaterial, tComponent.mAmount).addToList(tContent);
		AlloyResult tCold = CruciblePhysics.alloyScan(tContent, tInvar.mMeltingPoint - 1);
		assertNull(tCold.alloy(), "below the alloy melting point nothing forms");
	}

	@Test
	public void alloyScanToleratesOneNonMoltenComponent() {
		// :203-221 — tNonMolten <= 1: one component below its melting point is tolerated.
		// All but the LAST component are molten; the last one stays cold → exactly one
		// non-molten component regardless of Invar's component count.
		OreDictMaterial tInvar = MT.Invar;
		List<OreDictMaterialStack> tComponents = new ArrayList<>(MaterialGraph.alloyUndividedComponents(tInvar));
		assertTrue(tComponents.size() >= 2, "Invar is a multi-component alloy");
		long tHot = tInvar.mMeltingPoint;
		for (int i = 0; i < tComponents.size() - 1; i++) tHot = Math.max(tHot, tComponents.get(i).mMaterial.mMeltingPoint);
		List<OreDictMaterialStack> tContent = new ArrayList<>();
		for (OreDictMaterialStack tComponent : tComponents) new OreDictMaterialStack(tComponent.mMaterial, tComponent.mAmount).addToList(tContent);
		AlloyResult tResult = CruciblePhysics.alloyScan(tContent, tHot);
		assertSame(tInvar, tResult.alloy(), "exactly one non-molten component is tolerated");
	}

	// ====================================================================================
	// the ore direct-smelt projection (:167-183)
	// ====================================================================================

	@Test
	public void oreDirectSmeltsWithoutCrushing() {
		// :167-183 — an ore melts straight into mTargetCrushing × mOreMultiplier
		OreDictMaterialStack tDirect = CruciblePhysics.oreDirect(MT.Fe, 1);
		assertSame(MaterialGraph.conversionTarget(MT.Fe, MaterialGraph.Process.CRUSHING), tDirect.mMaterial);
		assertEquals(MT.Fe.mTargetCrushing.mAmount * MT.Fe.mOreMultiplier, tDirect.mAmount);
		// the dense-ore form factor doubles (:177-178)
		OreDictMaterialStack tDense = CruciblePhysics.oreDirect(MT.Fe, 2);
		assertEquals(tDirect.mAmount * 2, tDense.mAmount);
	}

	// ====================================================================================
	// helpers
	// ====================================================================================

	private static long total(List<OreDictMaterialStack> aList) {
		long rTotal = 0;
		for (OreDictMaterialStack tStack : aList) rTotal += tStack.mAmount;
		return rTotal;
	}

	/** The first registered material carrying the tag with a real density (the dynamic-dataset discipline). */
	private static OreDictMaterial anyMaterialWith(gregapi.code.TagData aTag) {
		for (OreDictMaterial tMaterial : MaterialRegistry.INSTANCE.MATERIAL_ARRAY) {
			if (tMaterial == null || tMaterial.mID < 0) continue;
			if (tMaterial.contains(aTag) && tMaterial.mGramPerCubicCentimeter > WEIGHT_AIR_G_PER_CUBIC_CENTIMETER) return tMaterial;
		}
		return null;
	}
}
