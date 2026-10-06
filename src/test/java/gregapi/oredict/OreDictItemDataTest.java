/**
 * Tests for OreDictItemData, ported from GregTech 6
 * gregapi/oredict/OreDictItemData.java:20-194. The pins mirror upstream semantics: the
 * aggregation constructor (:101-132 — mTargetReversing re-targeting, same-material summing,
 * amount-descending main/byproduct split with stable ties), the getAllMaterialWeights dual
 * arms (:153-164 — prefix weight vs stored amount), the validity predicate family (:134-142)
 * and copy() deep-copy behavior (:175-182, including the mFurnaceFuel quirk).
 */

package gregapi.oredict;

import static gregapi.data.CS.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class OreDictItemDataTest {

	@BeforeAll
	public static void setUpRegistry() {
		// prefix creation needs an open registry (PrefixRegistry is a global static; fresh batch per class)
		PrefixRegistry.INSTANCE.reset();
	}

	// detached registry per material family, per test (MaterialRegistry.java:53 "instance copies are used by tests")
	private static OreDictMaterial iron()        {return new MaterialRegistry().createMaterial(1, "Iron", "Iron");}
	private static OreDictMaterial copper()      {return new MaterialRegistry().createMaterial(2, "Copper", "Copper");}
	private static OreDictMaterial anyIron()     {return new MaterialRegistry().createMaterial(3, "AnyIron", "AnyIron");}
	private static OreDictPrefix prefix(String aName, long aAmount, long aWeight) {
		return OreDictPrefix.createPrefix(aName).setMaterialStats(aAmount, aWeight);
	}

	// --- prefix-backed constructor (upstream :50-55) ---

	@Test
	public void prefixCtorBindsPrefixStatsAndByproducts() {
		OreDictMaterial tIron = iron(), tCopper = copper();
		OreDictPrefix tDust = prefix("cdxDustA", U, U);
		tDust.mByProducts.add(new OreDictMaterialStack(tCopper, U4));

		OreDictItemData aData = new OreDictItemData(tDust, tIron);
		assertSame(tDust, aData.mPrefix);
		assertEquals(U, aData.mMaterial.mAmount); // at the prefix amount (:52)
		assertSame(tIron, aData.mMaterial.mMaterial);
		assertEquals("cdxDustAIron", aData.mOreDictName); // prefix + material internal names (:53)
		assertEquals("cdxDustAIron", aData.toString()); // cached name (:48, :189)
		assertFalse(aData.mBlackListed); // prefix ctor keeps the default, unlike the stack ctors
		assertEquals(1, aData.mByProducts.length);
		assertSame(tDust.mByProducts.get(0), aData.mByProducts[0]); // byproducts stored by reference (:54)

		// null material arm: no stack, empty name
		OreDictItemData aNoMaterial = new OreDictItemData(tDust, (OreDictMaterial)null);
		assertNull(aNoMaterial.mMaterial);
		assertEquals("", aNoMaterial.mOreDictName);
		assertEquals(1, aNoMaterial.mByProducts.length);
	}

	// --- stack constructor (upstream :57-71) ---

	@Test
	public void stackCtorClonesInputsAndDropsNulls() {
		OreDictMaterial tIron = iron(), tCopper = copper();
		OreDictMaterialStack aInput = new OreDictMaterialStack(tIron, U);

		OreDictItemData aData = new OreDictItemData(aInput, (OreDictMaterialStack[])null);
		assertNull(aData.mPrefix);
		assertEquals("", aData.mOreDictName);
		assertTrue(aData.mBlackListed); // :61
		assertNotSame(aInput, aData.mMaterial); // main is cloned (:59)
		assertEquals(U, aData.mMaterial.mAmount);
		assertEquals(0, aData.mByProducts.length); // null array -> ZL_MS (:62-63)

		// null elements are dropped, the survivors are cloned (:65-70)
		OreDictMaterialStack aBy = new OreDictMaterialStack(tCopper, U4);
		OreDictItemData aData2 = new OreDictItemData(aInput, null, aBy, null);
		assertEquals(1, aData2.mByProducts.length);
		assertNotSame(aBy, aData2.mByProducts[0]);
		assertEquals(U4, aData2.mByProducts[0].mAmount);
		assertNotSame(aInput, aData2.mMaterial);
	}

	// --- convenience overloads and the Collection form (upstream :73-99) ---

	@Test
	public void convenienceCtorOverloads() {
		OreDictMaterial tIron = iron(), tCopper = copper(), tAnyIron = anyIron();

		// :89 material + long amount + material + long byproduct amount
		OreDictItemData aData = new OreDictItemData(tIron, U2, tCopper, U4);
		assertSame(tIron, aData.mMaterial.mMaterial);
		assertEquals(U2, aData.mMaterial.mAmount);
		assertEquals(1, aData.mByProducts.length);
		assertSame(tCopper, aData.mByProducts[0].mMaterial);
		assertEquals(U4, aData.mByProducts[0].mAmount);

		// :93-95 two-byproduct form
		OreDictItemData aData2 = new OreDictItemData(tIron, U, tCopper, U4, tAnyIron, U9);
		assertEquals(2, aData2.mByProducts.length);

		// :97-99 Collection form delegates into the aggregation constructor:
		// iron U2 + U = 3U/2 beats copper U4 + U4 = U/2 beats anyIron U9 (descending order pin)
		OreDictItemData aData3 = new OreDictItemData(Arrays.asList(aData, aData2));
		assertSame(tIron, aData3.mMaterial.mMaterial);
		assertEquals(U2 + U, aData3.mMaterial.mAmount);
		assertEquals(2, aData3.mByProducts.length);
		assertSame(tCopper, aData3.mByProducts[0].mMaterial);
		assertEquals(U4 + U4, aData3.mByProducts[0].mAmount);
		assertSame(tAnyIron, aData3.mByProducts[1].mMaterial);
		assertEquals(U9, aData3.mByProducts[1].mAmount);

		// null arms degrade exactly like upstream: OM.stack nulls are dropped by the :67 filter
		OreDictItemData aData4 = new OreDictItemData(tIron, U2, (OreDictMaterial)null, U4);
		assertEquals(0, aData4.mByProducts.length);
		OreDictItemData aData5 = new OreDictItemData((OreDictMaterial)null, U2, tCopper, U4);
		assertNull(aData5.mMaterial);
		assertEquals(1, aData5.mByProducts.length);
	}

	// --- aggregation constructor (upstream :101-132), acceptance pin 1 ---

	@Test
	public void aggregationReversesTargetsAndSplitsByAmount() {
		OreDictMaterial tIron = iron(), tCopper = copper(), tAnyIron = anyIron();
		tIron.mTargetReversing = tAnyIron; // upstream ANY.java:153 pattern (MT.Fe.mTargetReversing = ANY.Iron)

		// inputs: Iron x U9 (via a small-dust prefix) and Copper x U2 (stack ctor)
		OreDictItemData dataIron = new OreDictItemData(prefix("cdxDustSmallB", U9, U9), tIron);
		OreDictItemData dataCopper = new OreDictItemData(tCopper, U2);

		OreDictItemData rData = new OreDictItemData(dataIron, dataCopper);
		// Copper (U/2) outweighs Iron (U/9) -> Copper is the Main Material, mTargetReversing = itself
		assertSame(tCopper, rData.mMaterial.mMaterial);
		assertEquals(U2, rData.mMaterial.mAmount);
		// the Iron input is reported as its mTargetReversing target (:109) — the reversal is the pin
		assertEquals(1, rData.mByProducts.length);
		assertSame(tAnyIron, rData.mByProducts[0].mMaterial);
		assertEquals(U9, rData.mByProducts[0].mAmount);
		assertNull(rData.mPrefix);
		assertTrue(rData.mBlackListed); // :104

		// same pin on the MAIN arm: Iron x U outweighs its byproduct, main comes out as AnyIron
		OreDictItemData dataIronBig = new OreDictItemData(tIron, U, new OreDictMaterialStack(tCopper, U4));
		OreDictItemData rMain = new OreDictItemData(dataIronBig);
		assertSame(tAnyIron, rMain.mMaterial.mMaterial); // :109
		assertEquals(U, rMain.mMaterial.mAmount);
		assertSame(tCopper, rMain.mByProducts[0].mMaterial); // :110 (Copper reverses to itself)
		assertEquals(U4, rMain.mByProducts[0].mAmount);
	}

	@Test
	public void aggregationSumsSameMaterial() {
		OreDictMaterial tIron = iron(), tCopper = copper();

		// same material from two inputs (different amounts) is summed into one entry (:113-120)
		OreDictItemData rData = new OreDictItemData(
			new OreDictItemData(tIron, U9),
			new OreDictItemData(tIron, U4),
			new OreDictItemData(tCopper, U9));
		assertSame(tIron, rData.mMaterial.mMaterial);
		assertEquals(U9 + U4, rData.mMaterial.mAmount); // U/9 + U/4 summed beats the lone copper U/9
		assertEquals(1, rData.mByProducts.length);
		assertSame(tCopper, rData.mByProducts[0].mMaterial);
		assertEquals(U9, rData.mByProducts[0].mAmount);
	}

	@Test
	public void aggregationTieKeepsInsertionOrderAndIgnoresEmptyInputs() {
		OreDictMaterial tIron = iron(), tCopper = copper();

		// equal amounts compare as 0 and Collections.sort is stable -> first seen wins the main slot (:122)
		OreDictItemData rData = new OreDictItemData(new OreDictItemData(tIron, U), new OreDictItemData(tCopper, U));
		assertSame(tIron, rData.mMaterial.mMaterial);
		assertEquals(1, rData.mByProducts.length);
		assertSame(tCopper, rData.mByProducts[0].mMaterial);

		// null entries (:108) and non-positive amounts (:109-110) are excluded
		OreDictItemData rEmpty = new OreDictItemData(
			null,
			new OreDictItemData(new OreDictMaterialStack(tIron, 0)),
			new OreDictItemData(new OreDictMaterialStack(tCopper, 0), new OreDictMaterialStack(tIron, -U)));
		assertNull(rEmpty.mMaterial);
		assertEquals(0, rEmpty.mByProducts.length);
		assertFalse(rEmpty.validMaterial());
	}

	@Test
	public void aggregationOfNothingYieldsEmptyBlacklistedData() {
		OreDictItemData rData = new OreDictItemData();
		assertNull(rData.mPrefix);
		assertNull(rData.mMaterial); // :124-125
		assertEquals(0, rData.mByProducts.length);
		assertEquals("", rData.mOreDictName);
		assertTrue(rData.mBlackListed);
	}

	// --- getAllMaterialWeights (upstream :153-164), acceptance pin 2 ---

	@Test
	public void getAllMaterialWeightsDualArms() {
		OreDictMaterial tIron = iron(), tCopper = copper();

		// prefix arm (:156-157): the Prefix WEIGHT is reported, not the stored prefix AMOUNT
		OreDictPrefix tPlate = prefix("cdxPlateC", U, U4); // mAmount = U, mWeight = U/4
		OreDictItemData prefixData = new OreDictItemData(tPlate, tIron);
		assertEquals(U, prefixData.mMaterial.mAmount); // stored at the prefix amount
		List<OreDictMaterialStack> rWeights = prefixData.getAllMaterialWeights();
		assertEquals(1, rWeights.size());
		assertSame(tIron, rWeights.get(0).mMaterial);
		assertEquals(U4, rWeights.get(0).mAmount); // :157 — the weight, not the U amount
		assertNotSame(prefixData.mMaterial, rWeights.get(0)); // fresh stack, the stored one is untouched

		// prefix-less arm (:158-159): the stored main stack itself, by reference
		OreDictItemData stackData = new OreDictItemData(new OreDictMaterialStack(tIron, U), new OreDictMaterialStack(tCopper, U4));
		List<OreDictMaterialStack> rWeights2 = stackData.getAllMaterialWeights();
		assertEquals(2, rWeights2.size());
		assertSame(stackData.mMaterial, rWeights2.get(0)); // :159
		assertSame(stackData.mByProducts[0], rWeights2.get(1)); // byproducts appended by reference (:162)

		// invalid material: only the byproducts come through (:155 guard)
		OreDictItemData noMaterial = new OreDictItemData((OreDictMaterialStack)null, new OreDictMaterialStack(tCopper, U4));
		List<OreDictMaterialStack> rWeights3 = noMaterial.getAllMaterialWeights();
		assertEquals(1, rWeights3.size());
		assertSame(tCopper, rWeights3.get(0).mMaterial);
	}

	// --- getAllMaterialStacks / getByProduct (upstream :145-150, :167-169) ---

	@Test
	public void getAllMaterialStacksPassesReferences() {
		OreDictMaterial tIron = iron(), tCopper = copper();
		OreDictItemData aData = new OreDictItemData(new OreDictMaterialStack(tIron, U), new OreDictMaterialStack(tCopper, U4));
		List<OreDictMaterialStack> rStacks = aData.getAllMaterialStacks();
		assertEquals(2, rStacks.size());
		assertSame(aData.mMaterial, rStacks.get(0)); // :147
		assertSame(aData.mByProducts[0], rStacks.get(1)); // :148

		assertTrue(new OreDictItemData((OreDictMaterialStack)null).getAllMaterialStacks().isEmpty());
	}

	@Test
	public void getByProductIndexing() {
		OreDictItemData aData = new OreDictItemData(new OreDictMaterialStack(iron(), U),
			new OreDictMaterialStack(copper(), U4), new OreDictMaterialStack(anyIron(), U9));
		assertSame(aData.mByProducts[0], aData.getByProduct(0));
		assertSame(aData.mByProducts[1], aData.getByProduct(1));
		assertNull(aData.getByProduct(2)); // out of range (:168)
		assertNull(aData.getByProduct(-1));
	}

	// --- copy (upstream :175-184), acceptance pin 3 ---

	@Test
	public void copyDeepClonesPrefixlessData() {
		OreDictMaterial tIron = iron(), tCopper = copper();
		OreDictItemData aData = new OreDictItemData(new OreDictMaterialStack(tIron, U), new OreDictMaterialStack(tCopper, U4));
		assertTrue(aData.mBlackListed); // stack ctor default
		aData.mBlocked = T;
		aData.setUseVanillaDamage(); // :186 chained setter
		aData.setNotFurnaceFuel(); // :187
		assertFalse(aData.mFurnaceFuel);

		OreDictItemData rCopy = aData.copy();
		assertNotSame(aData, rCopy);
		// the copied flag group (:178-180)
		assertTrue(rCopy.mBlackListed);
		assertTrue(rCopy.mBlocked);
		assertTrue(rCopy.mUseVanillaDamage);
		// upstream quirk pinned: mFurnaceFuel is NOT part of copy() (:175-182 has no such line) — the copy resets to T
		assertTrue(rCopy.mFurnaceFuel);

		// deep-copied main material
		assertNotSame(aData.mMaterial, rCopy.mMaterial);
		assertSame(tIron, rCopy.mMaterial.mMaterial);
		assertEquals(U, rCopy.mMaterial.mAmount);
		rCopy.mMaterial.mAmount = 42;
		assertEquals(U, aData.mMaterial.mAmount);

		// deep-copied, array-independent byproducts
		assertNotSame(aData.mByProducts, rCopy.mByProducts);
		assertNotSame(aData.mByProducts[0], rCopy.mByProducts[0]);
		assertEquals(U4, rCopy.mByProducts[0].mAmount);
		rCopy.mByProducts[0].mAmount = 1;
		assertEquals(U4, aData.mByProducts[0].mAmount);

		// static helper (:184)
		assertNull(OreDictItemData.copy(null));
		OreDictItemData rStatic = OreDictItemData.copy(aData);
		assertNotSame(aData, rStatic);
		assertEquals(U4, rStatic.mByProducts[0].mAmount);
	}

	@Test
	public void copyReDerivesPrefixData() {
		OreDictMaterial tIron = iron(), tCopper = copper();
		OreDictPrefix tDust = prefix("cdxDustD", U, U);
		tDust.mByProducts.add(new OreDictMaterialStack(tCopper, U4));

		OreDictItemData aData = new OreDictItemData(tDust, tIron);
		aData.mBlackListed = T; // copied over the prefix ctor's F

		OreDictItemData rCopy = aData.copy();
		assertSame(tDust, rCopy.mPrefix); // prefix-backed copy keeps the prefix (:176 second arm)
		assertEquals("cdxDustDIron", rCopy.mOreDictName); // re-derived from prefix + material (:53)
		assertEquals(U, rCopy.mMaterial.mAmount); // re-derived at the prefix amount
		assertEquals(1, rCopy.mByProducts.length);
		assertSame(tDust.mByProducts.get(0), rCopy.mByProducts[0]); // prefix byproducts by reference (:54)
		assertTrue(rCopy.mBlackListed); // flag survives (:179)
	}

	// --- validity predicate family (upstream :134-142) ---

	@Test
	public void validityPredicateFamily() {
		OreDictMaterial tIron = iron();
		OreDictPrefix tDust = prefix("cdxDustE", U, U);

		// full data: prefix + positive-ID material with a positive amount
		OreDictItemData aFull = new OreDictItemData(tDust, tIron);
		assertTrue(aFull.validPrefix() && aFull.validMaterial() && aFull.validData());
		assertTrue(aFull.nonemptyMaterial() && aFull.listedMaterial() && aFull.fullMaterial());
		assertTrue(aFull.nonemptyData() && aFull.listedData() && aFull.fullData());

		// mID 0 is the "Empty" Material (:139-140): listed but not nonempty
		OreDictMaterial tEmpty = new MaterialRegistry().createMaterial(0, "EmptyM", "EmptyM");
		OreDictItemData aEmpty = new OreDictItemData(prefix("cdxDustF", U, U), tEmpty);
		assertTrue(aEmpty.listedMaterial()); // mID >= 0
		assertFalse(aEmpty.nonemptyMaterial()); // mID > 0 fails
		assertFalse(aEmpty.fullMaterial());
		assertTrue(aEmpty.listedData());
		assertFalse(aEmpty.nonemptyData() || aEmpty.fullData());

		// unregistered material (mID -1): neither listed nor nonempty
		OreDictItemData aInvalid = new OreDictItemData(prefix("cdxDustG", U, U), new MaterialRegistry().createMaterial(-1, "InvalidM", "InvalidM"));
		assertFalse(aInvalid.listedMaterial() || aInvalid.nonemptyMaterial() || aInvalid.fullMaterial());
		assertFalse(aInvalid.listedData() || aInvalid.nonemptyData() || aInvalid.fullData());
		assertTrue(aInvalid.validData()); // prefix + non-null stack is still "valid"

		// prefix without a material: only validPrefix holds
		OreDictItemData aNoMaterial = new OreDictItemData(tDust, (OreDictMaterial)null);
		assertTrue(aNoMaterial.validPrefix());
		assertFalse(aNoMaterial.validMaterial() || aNoMaterial.validData() || aNoMaterial.fullData()
			|| aNoMaterial.listedData() || aNoMaterial.nonemptyData() || aNoMaterial.fullMaterial()
			|| aNoMaterial.listedMaterial() || aNoMaterial.nonemptyMaterial());

		// prefix-less data: every prefix-composed predicate fails
		OreDictItemData aStack = new OreDictItemData(new OreDictMaterialStack(tIron, U));
		assertTrue(aStack.validMaterial());
		assertFalse(aStack.validPrefix());
		assertFalse(aStack.validData() || aStack.fullData() || aStack.listedData() || aStack.nonemptyData());
	}

	// --- setters / toString (upstream :186-189) ---

	@Test
	public void settersChainAndDefaults() {
		OreDictItemData aData = new OreDictItemData(new OreDictMaterialStack(iron(), U));
		assertTrue(aData.mFurnaceFuel); // :38 default T
		assertSame(aData, aData.setNotFurnaceFuel());
		assertFalse(aData.mFurnaceFuel);
		assertFalse(aData.mUseVanillaDamage); // :37 default F
		assertSame(aData, aData.setUseVanillaDamage());
		assertTrue(aData.mUseVanillaDamage);
		assertEquals("", aData.toString()); // stack ctor name (:60, :189)
	}
}
