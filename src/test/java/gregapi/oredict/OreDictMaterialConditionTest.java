/**
 * Tests for task p2-material-condition-system: the OreDictMaterialCondition predicates
 * (upstream gregapi/oredict/OreDictMaterialCondition.java:31-126, verbatim port), checked
 * against the upstream predicate semantics including the boundary cases.
 */
package gregapi.oredict;

import static gregapi.data.CS.U;
import static gregapi.data.CS.U9;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import gregapi.code.ICondition;
import gregapi.code.ICondition.And;
import gregapi.code.ICondition.Or;

public class OreDictMaterialConditionTest {

	/** Selfcrush is true by default: the constructor seeds mTargetCrushing = stack(this, U) (OreDictMaterial:146). */
	@Test
	public void selfcrushIsTrueOnDefaultTarget() {
		MaterialRegistry r = new MaterialRegistry();
		OreDictMaterial a = r.createMaterial(9101, "CondSelfCrushA", "CondSelfCrushA");
		assertTrue(OreDictMaterialCondition.selfcrush().isTrue(a), "upstream :49 mTargetCrushing.mMaterial == aMaterial");
	}

	@Test
	public void selfcrushIsFalseAfterRetarget() {
		MaterialRegistry r = new MaterialRegistry();
		OreDictMaterial a = r.createMaterial(9102, "CondSelfCrushB", "CondSelfCrushB");
		OreDictMaterial b = r.createMaterial(9103, "CondSelfCrushC", "CondSelfCrushC");
		a.setCrushing(b, U);
		assertFalse(OreDictMaterialCondition.selfcrush().isTrue(a), "upstream :49 identity fails after setCrushing");
		assertTrue(OreDictMaterialCondition.selfcrush().isTrue(b), "b still crushes to itself");
	}

	/** Fullpulver is an amount check: mTargetPulver.mAmount >= U (upstream :54). */
	@Test
	public void fullpulverBoundaryIsExactlyU() {
		MaterialRegistry r = new MaterialRegistry();
		OreDictMaterial a = r.createMaterial(9104, "CondFullPulver", "CondFullPulver");
		assertTrue(OreDictMaterialCondition.fullpulver().isTrue(a), "default mTargetPulver amount is U");
		a.setPulver(a, U - 1);
		assertFalse(OreDictMaterialCondition.fullpulver().isTrue(a));
		a.setPulver(a, U);
		assertTrue(OreDictMaterialCondition.fullpulver().isTrue(a), ">= U is the inclusive boundary");
		a.setPulver(a, 2 * U);
		assertTrue(OreDictMaterialCondition.fullpulver().isTrue(a));
	}

	@Test
	public void selfforgeFollowsTargetIdentity() {
		MaterialRegistry r = new MaterialRegistry();
		OreDictMaterial a = r.createMaterial(9105, "CondSelfForge", "CondSelfForge");
		OreDictMaterial b = r.createMaterial(9106, "CondSelfForgeB", "CondSelfForgeB");
		assertTrue(OreDictMaterialCondition.selfforge().isTrue(a), "upstream :59");
		a.setForging(b, U);
		assertFalse(OreDictMaterialCondition.selfforge().isTrue(a));
	}

	@Test
	public void fullforgeBoundaryIsExactlyU() {
		MaterialRegistry r = new MaterialRegistry();
		OreDictMaterial a = r.createMaterial(9107, "CondFullForge", "CondFullForge");
		assertTrue(OreDictMaterialCondition.fullforge().isTrue(a), "default amount U, upstream :64");
		a.setForging(a, U9);
		assertFalse(OreDictMaterialCondition.fullforge().isTrue(a));
		a.setForging(a, U);
		assertTrue(OreDictMaterialCondition.fullforge().isTrue(a), ">= U inclusive");
	}

	/** meltmin/meltmax compare mMeltingPoint with an inclusive boundary (upstream :94/:100). */
	@Test
	public void meltminMeltmaxBoundariesAreInclusive() {
		MaterialRegistry r = new MaterialRegistry();
		OreDictMaterial m = r.createMaterial(9108, "CondMelt", "CondMelt");
		m.mMeltingPoint = 800;
		assertTrue(OreDictMaterialCondition.meltmin(800).isTrue(m), "equality passes meltmin, upstream :94 >=");
		assertFalse(OreDictMaterialCondition.meltmin(801).isTrue(m));
		assertTrue(OreDictMaterialCondition.meltmax(800).isTrue(m), "equality passes meltmax, upstream :100 <=");
		assertFalse(OreDictMaterialCondition.meltmax(799).isTrue(m));
		// The ingotHot gate (OP.java:166 meltmin(800)): a default 1000K material passes.
		OreDictMaterial hot = r.createMaterial(9109, "CondMeltHot", "CondMeltHot");
		assertTrue(OreDictMaterialCondition.meltmin(800).isTrue(hot), "default mMeltingPoint = 1000");
	}

	@Test
	public void boilminBoilmaxCompareBoilingPoint() {
		MaterialRegistry r = new MaterialRegistry();
		OreDictMaterial m = r.createMaterial(9110, "CondBoil", "CondBoil");
		m.mBoilingPoint = 3000;
		assertTrue(OreDictMaterialCondition.boilmin(3000).isTrue(m), "upstream :106 >=");
		assertFalse(OreDictMaterialCondition.boilmin(3001).isTrue(m));
		assertTrue(OreDictMaterialCondition.boilmax(3000).isTrue(m), "upstream :112 <=");
		assertFalse(OreDictMaterialCondition.boilmax(2999).isTrue(m));
	}

	@Test
	public void plasminPlasmaxComparePlasmaPoint() {
		MaterialRegistry r = new MaterialRegistry();
		OreDictMaterial m = r.createMaterial(9111, "CondPlasma", "CondPlasma");
		m.mPlasmaPoint = 10000;
		assertTrue(OreDictMaterialCondition.plasmin(10000).isTrue(m), "upstream :118 >=");
		assertFalse(OreDictMaterialCondition.plasmin(10001).isTrue(m));
		assertTrue(OreDictMaterialCondition.plasmax(10000).isTrue(m), "upstream :124 <=");
		assertFalse(OreDictMaterialCondition.plasmax(9999).isTrue(m));
	}

	/** qualmin/qualmax compare mToolQuality (upstream :82/:88). Negative tool qualities never
	 *  come out of qual() (UT.Code.bind4 clamps), but the predicate itself is a plain long
	 *  comparison on the raw field, so a negative value is written directly to test the math. */
	@Test
	public void qualminQualmaxIncludeNegativeQualities() {
		MaterialRegistry r = new MaterialRegistry();
		OreDictMaterial good = r.createMaterial(9112, "CondQualGood", "CondQualGood");
		good.mToolQuality = 1;
		assertTrue(OreDictMaterialCondition.qualmin(1).isTrue(good));
		assertFalse(OreDictMaterialCondition.qualmin(2).isTrue(good));
		assertTrue(OreDictMaterialCondition.qualmax(1).isTrue(good));
		assertFalse(OreDictMaterialCondition.qualmax(0).isTrue(good));

		OreDictMaterial bad = r.createMaterial(9113, "CondQualBad", "CondQualBad");
		bad.mToolQuality = -1;
		assertFalse(OreDictMaterialCondition.qualmin(0).isTrue(bad), "negative quality fails qualmin(0)");
		assertTrue(OreDictMaterialCondition.qualmin(-1).isTrue(bad), "negative quality passes its own lower bound");
		assertTrue(OreDictMaterialCondition.qualmax(-1).isTrue(bad));
	}

	/** typemin/typemax compare mToolTypes (upstream :70/:76). The toolHead/tool families use
	 *  typemin(1) and typemin(2) (OP.java:232-276), so the 0..3 byte range is exercised. */
	@Test
	public void typeminTypemaxCoverToolTypeRange() {
		MaterialRegistry r = new MaterialRegistry();
		OreDictMaterial none = r.createMaterial(9114, "CondTypeNone", "CondTypeNone");
		none.mToolTypes = 0;
		assertFalse(OreDictMaterialCondition.typemin(1).isTrue(none), "upstream :70");
		assertTrue(OreDictMaterialCondition.typemax(2).isTrue(none));

		OreDictMaterial mid = r.createMaterial(9115, "CondTypeMid", "CondTypeMid");
		mid.mToolTypes = 2;
		assertTrue(OreDictMaterialCondition.typemin(2).isTrue(mid), "equality passes, saw/file class gate");
		assertTrue(OreDictMaterialCondition.typemin(1).isTrue(mid), "sword class gate");
		assertTrue(OreDictMaterialCondition.typemax(2).isTrue(mid));
		assertFalse(OreDictMaterialCondition.typemin(3).isTrue(mid));

		OreDictMaterial high = r.createMaterial(9116, "CondTypeHigh", "CondTypeHigh");
		high.mToolTypes = 3;
		assertTrue(OreDictMaterialCondition.typemin(1).isTrue(high));
		assertFalse(OreDictMaterialCondition.typemax(2).isTrue(high));
	}

	/** Combined shapes as they occur in OP.java:232-276 (And of typemin with tag conditions,
	 *  Or inside the hammer gate). */
	@Test
	public void combinatorsComposeWithPredicates() {
		MaterialRegistry r = new MaterialRegistry();
		OreDictMaterial m = r.createMaterial(9117, "CondComb", "CondComb");
		m.mToolTypes = 1;
		m.mToolQuality = 0;
		ICondition<OreDictMaterial> swordLike = new And<>(OreDictMaterialCondition.typemin(1));
		assertTrue(swordLike.isTrue(m));
		ICondition<OreDictMaterial> hammerGate = new And<>(OreDictMaterialCondition.typemin(1), new Or<>(OreDictMaterialCondition.qualmin(1)));
		assertFalse(hammerGate.isTrue(m), "hammer needs a property tag or qualmin(1) on top of typemin(1)");
		m.mToolQuality = 1;
		assertTrue(hammerGate.isTrue(m));
	}

	/** Upstream :32-35 return singletons; the point factories return fresh instances (:36-45). */
	@Test
	public void singletonFactoriesReturnSameInstance() {
		assertSame(OreDictMaterialCondition.selfcrush(), OreDictMaterialCondition.selfcrush());
		assertSame(OreDictMaterialCondition.fullpulver(), OreDictMaterialCondition.fullpulver());
		assertSame(OreDictMaterialCondition.selfforge(), OreDictMaterialCondition.selfforge());
		assertSame(OreDictMaterialCondition.fullforge(), OreDictMaterialCondition.fullforge());
		assertNotSame(OreDictMaterialCondition.meltmin(800), OreDictMaterialCondition.meltmin(800));
	}
}
