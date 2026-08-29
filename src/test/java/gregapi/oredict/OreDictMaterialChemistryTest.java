/**
 * Tests for task gt-material-model: weighted component chemistry and chemical formula generation
 * (upstream OreDictMaterial.setMoleculeConfiguration :468-525), the setMcfg convenience chain,
 * energetic/atomic stat setters, the ITagDataContainer contract and the put(Object...) dispatch.
 */
package gregapi.oredict;

import static gregapi.data.CS.U;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import gregapi.code.ITagDataContainer;
import gregapi.code.TagData;

public class OreDictMaterialChemistryTest {

	private static final TagData DECOMPOSABLE = TagData.createTagData("COMPOUNDS.DECOMPOSABLE");
	private static final TagData UUM = TagData.createTagData("PROCESSING.UUM_SYNTHESISABLE");
	private static final TagData APPROXIMATE = TagData.createTagData("COMPOUNDS.APPROXIMATE");
	private static final TagData ELEMENT = TagData.createTagData("ATOMIC.ELEMENT");
	private static final TagData HAS_TOOL_STATS = TagData.createTagData("PROPERTIES.HAS_TOOL_STATS");
	private static final TagData NO_ADVANCED_TOOLS = TagData.createTagData("PROPERTIES.NO_ADVANCED_TOOLS");

	/** Detached material: created inside a throwaway registry instance (constructor privatized by gt-material-dataset; upstream semantics preserved: mID -1, no INSTANCE registration). */
	private static OreDictMaterial mat(String aName, String aLocal) {
		return new MaterialRegistry().createMaterial(-1, aName, aLocal);
	}

	private static OreDictMaterial element(String aName, long aProtons, long aNeutrons, long aMelt, long aBoil, double aDensity, String aTooltip) {
		OreDictMaterial m = mat(aName, aName);
		m.setStats(aProtons, aNeutrons, aMelt, aBoil, aDensity);
		if (aTooltip != null) m.tooltip(aTooltip);
		m.put(ELEMENT);
		return m;
	}

	// --- weighted chemistry (upstream :468-492) ---------------------------

	@Test
	public void weightedAveragesOfMoleculeComponents() {
		// Fe-like (27p+33n, mass 60, density 7.5) 2/3 + Co-like (30p+36n, mass 66, density 9.0) 1/3
		OreDictMaterial a = element("Chem A", 27, 33, 1800, 3600, 7.5, null);
		OreDictMaterial b = element("Chem B", 30, 36, 2100, 4200, 9.0, null);
		OreDictMaterial alloy = mat("Chem Alloy", "Chem Alloy");
		alloy.setMcfg(0, a, 2 * U / 3, b, U / 3);

		assertEquals(28, alloy.mProtons);   // (27*2 + 30*1)/3
		assertEquals(28, alloy.mElectrons); // electrons = protons here
		assertEquals(34, alloy.mNeutrons);  // (33*2 + 36*1)/3
		assertEquals(62, alloy.mMass);      // (60*2 + 66*1)/3 = 28+34
		assertEquals(8.0, alloy.mGramPerCubicCentimeter, 1e-9); // (7.5*2 + 9.0*1)/3
		assertEquals(1900, alloy.mMeltingPoint);                // (1800*2 + 2100*1)/3
		assertEquals(3800, alloy.mBoilingPoint);                // (3600*2 + 4200*1)/3
		assertEquals(380000, alloy.mPlasmaPoint);               // (360000*2 + 420000*1)/3
		assertEquals(2, alloy.mComponents.getComponents().size());
		// total amount (2U/3 + U/3) = U -> auto divider 1 (upstream :529-531)
		assertEquals(1, alloy.mComponents.getCommonDivider());
		// with divider 1 the component amounts stay undivided
		assertEquals(2 * U / 3, alloy.mComponents.getComponents().get(0).mAmount);
	}

	@Test
	public void energeticPointsAreClampedAgainstEachOther() {
		OreDictMaterial a = element("Clamp A", 10, 10, 100, 200, 1.0, null);
		OreDictMaterial b = element("Clamp B", 20, 20, 100, 200, 1.0, null);
		OreDictMaterial alloy = mat("Clamp Alloy", "Clamp Alloy");
		alloy.setMcfg(0, a, U, b, U); // average: melt 100, boil 200, plasma 20000
		assertEquals(100, alloy.mMeltingPoint);
		assertEquals(200, alloy.mBoilingPoint); // Math.max(meltingPoint+1, average 200) -> 200
		assertTrue(alloy.mPlasmaPoint > alloy.mBoilingPoint);
	}

	// --- chemical formula generation (upstream :494-523) ------------------

	@Test
	public void chemicalFormulaWithSubscripts() {
		OreDictMaterial fe = element("Form Fe", 26, 30, 1811, 3134, 7.874, "Fe");
		OreDictMaterial o = element("Form O", 8, 8, 54, 90, 1.141, "O");
		OreDictMaterial alloy = mat("Form Oxide", "Form Oxide");
		alloy.put(DECOMPOSABLE); // gate: containsAny(UUM, DECOMPOSABLE)
		alloy.setMcfg(0, fe, 2 * U, o, U);
		assertEquals("Fe\u2082O", alloy.mTooltipChemical);

		// bigger subscripts spell multi-digit numbers (NUM_SUB, upstream CS.java:169-201)
		OreDictMaterial twelve = mat("Form Twelve", "Form Twelve");
		twelve.put(DECOMPOSABLE);
		twelve.setMcfg(0, fe, 12 * U, o, U);
		assertEquals("Fe\u2081\u2082O", twelve.mTooltipChemical);
	}

	@Test
	public void chemicalFormulaCapsAtSubscript300Plus() {
		// NUM_SUB has 301 entries; index 300 (upstream CS.java:200) is the "₃₀₀₊" marker capping amounts of 300+ Units
		OreDictMaterial fe = element("Form Cap Fe", 26, 30, 1811, 3134, 7.874, "Fe");
		OreDictMaterial alloy = mat("Form Capped", "Form Capped");
		alloy.put(DECOMPOSABLE);
		alloy.setMcfg(0, fe, 400 * U);
		assertEquals("Fe\u2083\u2080\u2080\u208A", alloy.mTooltipChemical);
	}

	@Test
	public void setMcfgDropsNullMaterialComponents() {
		// upstream OM.stack(null, amount) yields null and the NoNulls component list drops it (OM.java:485)
		OreDictMaterial fe = element("Form Null Fe", 26, 30, 1811, 3134, 7.874, "Fe");
		OreDictMaterial alloy = mat("Form Null Dropped", "Form Null Dropped");
		alloy.setMcfg(0, fe, U, null, 5 * U);
		assertEquals(1, alloy.mComponents.getComponents().size());
		assertSame(fe, alloy.mComponents.getComponents().get(0).mMaterial);
	}

	@Test
	public void chemicalFormulaSingleComponentCopiesTooltip() {
		OreDictMaterial fe = element("Form Only", 26, 30, 1811, 3134, 7.874, "Fe");
		OreDictMaterial alloy = mat("Form Copy", "Form Copy");
		alloy.put(DECOMPOSABLE);
		alloy.setMcfg(0, fe, U); // one component, exactly one unit -> copy verbatim
		assertEquals("Fe", alloy.mTooltipChemical);
	}

	@Test
	public void chemicalFormulaBracketsNestedComponents() {
		// inner alloys have their own components -> their tooltips get bracketed (upstream :501-505)
		OreDictMaterial x = element("Form X", 1, 0, 100, 200, 1.0, "Xx");
		OreDictMaterial y = element("Form Y", 2, 2, 100, 200, 1.0, "Yy");
		OreDictMaterial inner = mat("Form Inner", "Form Inner");
		inner.put(DECOMPOSABLE);
		inner.setMcfg(0, x, U / 2, y, U / 2); // mComponents != null, 2 undivided components
		inner.tooltip("InnerChem"); // deterministic inner tooltip

		OreDictMaterial outer = mat("Form Outer", "Form Outer");
		outer.put(DECOMPOSABLE);
		outer.setMcfg(0, inner, U);
		// single undivided component with amount U would copy the tooltip... but the inner
		// material has 1 undivided component of amount U, so the bracket rule does not apply.
		// upstream :497-498: size()==1 && amount==U -> plain copy of the tooltip.
		assertEquals("InnerChem", outer.mTooltipChemical);

		OreDictMaterial outer2 = mat("Form Outer2", "Form Outer2");
		outer2.put(DECOMPOSABLE);
		outer2.setMcfg(0, inner, 2 * U); // amount > U -> subscript on the bracketed formula
		assertEquals("(InnerChem)\u2082", outer2.mTooltipChemical);
	}

	@Test
	public void chemicalFormulaApproximateFallsBackToLocalName() {
		OreDictMaterial weird = element("Form Weird", 5, 5, 100, 200, 2.0, null);
		weird.put(APPROXIMATE);
		weird.setLocal("Weird Stuff");
		OreDictMaterial alloy = mat("Form With Approx", "Form With Approx");
		alloy.put(DECOMPOSABLE);
		alloy.setMcfg(0, weird, U);
		// single component amount U copies the tooltip (null here) -> formula stays empty-ish;
		// use two components to reach the fallback branch instead
		OreDictMaterial fe = element("Form Fe2", 26, 30, 1811, 3134, 7.874, "Fe");
		OreDictMaterial alloy2 = mat("Form With Approx2", "Form With Approx2");
		alloy2.put(DECOMPOSABLE);
		alloy2.setMcfg(0, fe, U, weird, U);
		assertEquals("Fe(Weird Stuff)", alloy2.mTooltipChemical);
	}

	@Test
	public void chemicalFormulaGatedByDecomposableOrUUM() {
		OreDictMaterial fe = element("Form Gate Fe", 26, 30, 1811, 3134, 7.874, "Fe");
		OreDictMaterial o = element("Form Gate O", 8, 8, 54, 90, 1.141, "O");
		OreDictMaterial ungated = mat("Form Ungated", "Form Ungated");
		ungated.setMcfg(0, fe, U, o, U);
		assertNull(ungated.mTooltipChemical, "no UUM/DECOMPOSABLE tag -> no formula (upstream :494)");

		ungated.put(UUM);
		ungated.setMcfg(0, fe, U, o, U);
		assertEquals("FeO", ungated.mTooltipChemical);
	}

	// --- energetic setters --------------------------------------------------

	@Test
	public void heatSingleArgumentDoublesBoilAndCentuplesPlasma() {
		OreDictMaterial m = mat("Heat One", "Heat One");
		m.heat(1500);
		assertEquals(1500, m.mMeltingPoint);
		assertEquals(3000, m.mBoilingPoint);
		assertEquals(300000, m.mPlasmaPoint);
	}

	@Test
	public void heatRejectsInvertedPoints() {
		OreDictMaterial m = mat("Heat Bad", "Heat Bad");
		assertThrows(IllegalArgumentException.class, () -> m.heat(3000, 1500));
		assertThrows(IllegalArgumentException.class, () -> m.heat(1500, 2500, 2000));
		m.heat(1500, 2500, 30000);
		assertEquals(30000, m.mPlasmaPoint);
	}

	@Test
	public void setStatsAndSetStatsElementComputeMass() {
		OreDictMaterial m = mat("Stats A", "Stats A");
		m.setStats(26, 30, 1811, 3134, 7.874);
		assertEquals(26, m.mProtons);
		assertEquals(26, m.mElectrons);
		assertEquals(30, m.mNeutrons);
		assertEquals(56, m.mMass);
		assertEquals(7.874, m.mGramPerCubicCentimeter, 0);
		assertEquals(3134, m.mBoilingPoint);
		assertEquals(313400, m.mPlasmaPoint);

		OreDictMaterial e = mat("Stats Element", "Stats Element");
		e.setStatsElement(1, 1, 0, 0, 0.00009); // hydrogen, isotope mass 0
		assertEquals(1, e.mMass);
		e.setStatsElement(1, 1, 1, 0, 0.00009); // deuterium
		assertEquals(2, e.mMass);
		e.setStatsElement(1, 1, 1, 1, 0.00009); // tritium (additional mass)
		assertEquals(3, e.mMass);

		// copy helpers (:1128-1144)
		OreDictMaterial copy = mat("Stats Copy", "Stats Copy");
		copy.stealStatsElement(m);
		assertEquals(56, copy.mMass);
		copy.stealQuality(m);
		copy.qual(m);
		copy.heat(m);
		assertEquals(3134, copy.mBoilingPoint);
	}

	// --- qual -----------------------------------------------------------------

	@Test
	public void qualSetsToolStatsAndTags() {
		OreDictMaterial m = mat("Qual Mat", "Qual Mat");
		m.qual(2, 8.0, 1000, 5);
		assertEquals(2, m.mToolTypes);
		assertEquals(8.0F, m.mToolSpeed);
		assertEquals(1000, m.mToolDurability);
		assertEquals(5, m.mToolQuality);
		assertTrue(m.contains(HAS_TOOL_STATS));
		assertTrue(m.contains(TagData.createTagData("ITEMGENERATOR.PARTS")));
		assertTrue(m.contains(TagData.createTagData("ITEMGENERATOR.STICKS")));
		assertTrue(m.contains(TagData.createTagData("ITEMGENERATOR.PLATES")));
		assertTrue(m.contains(NO_ADVANCED_TOOLS)); // aType < 3
		m.qual(3, 8.0, 1000, 5); // qual never removes tags; a fresh material proves qual(3) adds none
		assertTrue(m.contains(NO_ADVANCED_TOOLS));
		OreDictMaterial advanced = mat("Qual Advanced", "Qual Advanced");
		advanced.qual(3, 8.0, 1000, 5);
		assertFalse(advanced.contains(NO_ADVANCED_TOOLS));
		m.qual(9, 8.0, 1000, 50); // bind2/bind4 clamps (UT.java:1554/1556)
		assertEquals(3, m.mToolTypes);
		assertEquals(15, m.mToolQuality);
		m.qual(1, 8.0, 0, 5);
		assertEquals(1, m.mToolDurability); // Math.max(1, aDurability)
	}

	// --- processing targets ---------------------------------------------------

	@Test
	public void processingTargetsMaintainBackReferences() {
		OreDictMaterial a = mat("Proc A", "Proc A");
		OreDictMaterial b = mat("Proc B", "Proc B");
		// default: every target points at itself with one unit (upstream :283-295)
		assertEquals(U, a.mTargetCrushing.mAmount);
		assertSame(a, a.mTargetCrushing.mMaterial);
		assertEquals(0, a.mTargetBurning.mAmount);

		a.setCrushing(b, U / 2);
		assertSame(b, a.mTargetCrushing.mMaterial);
		assertEquals(U / 2, a.mTargetCrushing.mAmount);
		assertTrue(b.mTargetedCrushing.contains(a));
		a.setCrushing(a, U); // retarget cleans the old back reference (upstream :783)
		assertFalse(b.mTargetedCrushing.contains(a));

		a.setSmelting(b, U);
		assertTrue(a.contains(TagData.createTagData("PROCESSING.MELTING"))); // upstream :804
		a.setSmelting(b, 0); // amount 0 disables -> no MELTING tag added for a fresh material
		OreDictMaterial fresh = mat("Proc Fresh", "Proc Fresh");
		fresh.setSmelting(b, 0);
		assertFalse(fresh.contains(TagData.createTagData("PROCESSING.MELTING")));

		a.setAllToTheOutputOf(b); // bulk copy (:749-762)
		assertSame(b.mTargetPulver.mMaterial, a.mTargetPulver.mMaterial);
		assertEquals(b.mTargetPulver.mAmount, a.mTargetPulver.mAmount);
		a.setAllToTheOutputOf(b, 1, 2); // scaled copy (:764-778)
		assertEquals(b.mTargetPulver.mAmount / 2, a.mTargetPulver.mAmount);
	}

	@Test
	public void alloySimpleAccumulatesCreationRecipesAndTags() {
		OreDictMaterial a = element("Alloy A", 1, 1, 100, 200, 1.0, null);
		OreDictMaterial b = element("Alloy B", 2, 2, 100, 200, 1.0, null);
		OreDictMaterial alloy = mat("Alloy Real", "Alloy Real");
		alloy.setMcfg(0, a, U / 2, b, U / 2);
		alloy.alloyCentrifuge();
		assertTrue(alloy.contains(TagData.createTagData("PROCESSING.CENTRIFUGABLE")));
		assertTrue(alloy.contains(TagData.createTagData("COMPOUNDS.ALLOY")));
		assertTrue(alloy.contains(TagData.createTagData("COMPOUNDS.DECOMPOSABLE")));
		assertTrue(alloy.contains(TagData.createTagData("PROCESSING.CRUCIBLE_ALLOY")));
		assertEquals(1, alloy.mAlloyCreationRecipes.size());
		alloy.alloyElectrolyzer(1200); // with heat overload
		assertEquals(2, alloy.mAlloyCreationRecipes.size());
		assertEquals(1200, alloy.mMeltingPoint);
		// setAloy == setMcfg + alloyCentrifuge (:677-703)
		OreDictMaterial quick = mat("Alloy Quick", "Alloy Quick");
		quick.setAloy(0, a, U / 2, b, U / 2);
		assertEquals(2, quick.mComponents.getComponents().size());
		assertTrue(quick.contains(TagData.createTagData("PROCESSING.CENTRIFUGABLE")));
		// uumMcfg requires the UUM tag on all components (:602-673), else only a warning
		OreDictMaterial uumAlloy = mat("Alloy UUM", "Alloy UUM");
		a.put(UUM);
		b.put(UUM);
		uumAlloy.uumAloy(0, a, U / 2, b, U / 2);
		assertTrue(uumAlloy.contains(UUM));
	}

	// --- tag container (ITagDataContainer) + put dispatch ----------------------

	@Test
	public void tagContainerSemantics() {
		OreDictMaterial m = mat("Tag Mat", "Tag Mat");
		TagData tagA = TagData.createTagData("MODELTEST.TAGA");
		TagData tagB = TagData.createTagData("MODELTEST.TAGB");
		TagData tagC = TagData.createTagData("MODELTEST.TAGC");

		assertTrue(m.put(tagA, tagB) instanceof ITagDataContainer); // fluent, implements the container contract
		assertTrue(m.contains(tagA));
		assertTrue(m.containsAll(tagA, tagB));
		assertTrue(m.containsAll(java.util.Arrays.asList(tagA, tagB)));
		assertFalse(m.containsAll(tagA, tagC));
		assertTrue(m.containsAny(tagC, tagB));
		assertFalse(m.containsAny(tagC));

		assertTrue(m.add(tagC) == m); // ITagDataContainer.add returns this
		assertTrue(m.contains(tagC));
		assertTrue(m.remove(tagC)); // returns whether the tag was there
		assertFalse(m.remove(tagC));

		assertTrue(m.isTrue(m)); // ICondition: identity
		assertFalse(m.isTrue(mat("Tag Other", "Tag Other")));
		assertTrue(m.NOT.isTrue(mat("Tag Other", "Tag Other")));
		assertFalse(m.NOT.isTrue(m));

		m.put(new TagData[] {tagC}, tagB); // convenience array overload (:1489-1493)
		assertTrue(m.contains(tagC));
	}

	@Test
	public void putObjectDispatchBranches() {
		OreDictMaterial m = mat("Put Mat", "Put Mat");
		m.put(1000); // Number -> setFurnaceBurnTime (upstream :1471-1473)
		assertEquals(1000, m.mFurnaceBurnTime);
		m.put(-50);
		assertEquals(0, m.mFurnaceBurnTime); // Math.max(0, aValue)
		m.put((Object)null); // null-safe
		m.put(new Object[] {2000, null}); // arrays are flattened recursively (:1455-1457)
		assertEquals(2000, m.mFurnaceBurnTime);
		m.put(java.util.Arrays.asList(3000)); // Iterable is iterated (:1483)
		assertEquals(3000, m.mFurnaceBurnTime);
	}

	@Test
	public void enchantmentStacksCarryIDAndLevel() {
		OreDictMaterial m = mat("Enchant Mat", "Enchant Mat");
		m.addEnchantmentForTools("minecraft:efficiency", 5);
		m.addEnchantmentForWeapons("minecraft:sharpness", 3);
		m.addEnchantmentForDamage("minecraft:unbreaking", 1); // weapons + ammo (:1209-1213)
		m.addEnchantmentForRanged("minecraft:power", 4);
		m.addEnchantmentForFishing("minecraft:luck_of_the_sea", 3);
		m.addEnchantmentForArmors("minecraft:protection", 4);
		assertEquals(1, m.mEnchantmentTools.size());
		assertEquals("minecraft:efficiency", m.mEnchantmentTools.get(0).mEnchantmentID);
		assertEquals(5, m.mEnchantmentTools.get(0).mLevel);
		assertEquals(2, m.mEnchantmentWeapons.size()); // sharpness + unbreaking via ForDamage
		assertEquals(1, m.mEnchantmentAmmo.size());
		assertEquals("minecraft:unbreaking", m.mEnchantmentAmmo.get(0).mEnchantmentID);
		assertEquals(1, m.mEnchantmentRanged.size());
		assertEquals(1, m.mEnchantmentFishing.size());
		assertEquals(1, m.mEnchantmentArmors.size());
	}

	// --- misc fluent setters -----------------------------------------------------

	@Test
	public void fluentSetters() {
		OreDictMaterial m = mat("Fluent Mat", "Fluent Mat");
		assertSame(m, m.setLocal("Pretty Name"));
		assertEquals("Pretty Name", m.getLocal());
		assertSame(m, m.setLocal(null)); // null falls back to the internal name (:332)
		assertEquals("FluentMat", m.getLocal()); // the constructor is private now, so the name goes through createMaterial's sanitize (upstream :204-206), which strips the space
		assertSame(m, m.setOriginalMod("somemod"));
		assertEquals("somemod", m.mOriginalMod);
		assertSame(m, m.setOriginalMod(null)); // null keeps the old value (upstream :343)
		assertEquals("somemod", m.mOriginalMod);
		assertSame(m, m.tooltip("Custom"));
		assertEquals("Custom", m.mTooltipChemical);
		assertSame(m, m.hide());
		assertTrue(m.mHidden);
		assertSame(m, m.hide(false));
		assertFalse(m.mHidden);
		OreDictMaterial other = mat("Fluent Other", "Fluent Other");
		assertSame(m, m.handle(other));
		assertSame(other, m.mHandleMaterial);
		assertSame(m, m.addSourceOf(other));
		assertTrue(m.mSourceOf.contains(other));
		// upstream defaults from :250 (Technetium) before any stat setter runs
		OreDictMaterial defaults = mat("Fluent Defaults", "Fluent Defaults");
		assertEquals(55, defaults.mNeutrons);
		assertEquals(43, defaults.mProtons);
		assertEquals(98, defaults.mMass);
		assertEquals(1000, defaults.mMeltingPoint);
		assertEquals(3000, defaults.mBoilingPoint);
		assertEquals(10000, defaults.mPlasmaPoint);
		assertEquals(1.0, defaults.mGramPerCubicCentimeter, 0);
	}
}
