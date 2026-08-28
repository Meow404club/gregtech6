/**
 * Tests for OreDictMaterialStack, ported from GregTech 6
 * gregapi/oredict/OreDictMaterialStack.java:20-103. Assertions mirror upstream semantics,
 * including the documented quirks (negative amount wildcard in equals, stack==material).
 */

package gregapi.oredict;

import static gregapi.data.CS.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import gregapi.data.MT;

public class OreDictMaterialStackTest {
	private static final OreDictMaterial IRON = new OreDictMaterial((short)1, "IRON", "IRON");
	private static final OreDictMaterial COPPER = new OreDictMaterial((short)2, "COPPER", "COPPER");

	@Test
	public void nullMaterialFallsBackToMTNULL() {
		assertSame(MT.NULL, new OreDictMaterialStack(null, U).mMaterial);
		assertEquals(-1, MT.NULL.mID);
		assertEquals("NULL", MT.NULL.mNameInternal);
	}

	@Test
	public void copyAndCloneAreIndependentInstances() {
		OreDictMaterialStack aStack = new OreDictMaterialStack(IRON, U);
		OreDictMaterialStack aCopy = aStack.copy(U2);
		OreDictMaterialStack aClone = aStack.clone();
		assertEquals(U2, aCopy.mAmount);
		assertEquals(U, aClone.mAmount);
		assertSame(IRON, aCopy.mMaterial);
		assertSame(IRON, aClone.mMaterial);
		// copies mutate independently
		aClone.mAmount = U3;
		assertEquals(U, aStack.mAmount);
	}

	@Test
	public void mulDivArithmetic() {
		assertEquals(U, new OreDictMaterialStack(IRON, U9).mul(9).mAmount);
		assertEquals(U9, new OreDictMaterialStack(IRON, U).div(9).mAmount);
		// div truncates like upstream long division
		assertEquals(3, new OreDictMaterialStack(IRON, 10).div(3).mAmount);
		// mul keeps the material, returns a new instance
		OreDictMaterialStack aStack = new OreDictMaterialStack(IRON, U);
		OreDictMaterialStack aMultiplied = aStack.mul(2);
		assertNotSame(aStack, aMultiplied);
		assertSame(IRON, aMultiplied.mMaterial);
		// U2 means U/2 (CS.java:120), so 2x is spelled out here
		assertEquals(2 * U, aMultiplied.mAmount);
		// original is untouched (immutably styled arithmetic)
		assertEquals(U, aStack.mAmount);
	}

	@Test
	public void divupRoundsUp() {
		// UT.Code.divup(n, d) = n/d + (n%d == 0 ? 0 : 1) (UT.java:1697-1699)
		assertEquals(4, new OreDictMaterialStack(IRON, 10).divup(3).mAmount);
		assertEquals(3, new OreDictMaterialStack(IRON, 9).divup(3).mAmount);
		// divup(m, d) applies UT.Code.divup to (m * amount) / d: (3 * U9) / 9 = U/27, exact here
		assertEquals(U / 27, new OreDictMaterialStack(IRON, U9).divup(3, 9).mAmount);
		assertEquals(34, new OreDictMaterialStack(IRON, 100).divup(1, 3).mAmount);
		assertEquals(33, new OreDictMaterialStack(IRON, 100).div(1, 3).mAmount);
	}

	@Test
	public void mulThenDivMatchesSingleDivForm() {
		// div(m, d) == (m * amount) / d; divup(m, d) rounds that quotient up
		OreDictMaterialStack aStack = new OreDictMaterialStack(IRON, 1000);
		assertEquals(aStack.mul(3).div(7).mAmount, aStack.div(3, 7).mAmount);
		assertEquals(aStack.mul(3).divup(7).mAmount, aStack.divup(3, 7).mAmount);
	}

	@Test
	public void weightUsesDensityFormula() {
		OreDictMaterial tSteelLike = new OreDictMaterial((short)3, "TESTSTEEL", "TESTSTEEL");
		tSteelLike.mGramPerCubicCentimeter = 7.8;
		// (g/cm^3 * 111.111111 * amount) / U  (OreDictMaterial.java:1401)
		assertEquals(7.8 * 111.111111, new OreDictMaterialStack(tSteelLike, U).weight(), 1e-9);
		// U2 is U/2 (CS.java:120)
		assertEquals(0.5 * 7.8 * 111.111111, new OreDictMaterialStack(tSteelLike, U2).weight(), 1e-9);
		assertEquals(0.0, new OreDictMaterialStack(tSteelLike, 0).weight(), 0.0);
	}

	@Test
	public void hasRequiresIdentityAndPositiveAmount() {
		assertTrue(new OreDictMaterialStack(IRON, U).has(IRON));
		assertFalse(new OreDictMaterialStack(IRON, U).has(COPPER));
		assertFalse(new OreDictMaterialStack(IRON, 0).has(IRON));
		assertFalse(new OreDictMaterialStack(IRON, -U).has(IRON));
	}

	@Test
	public void equalityMirrorsUpstreamQuirks() {
		OreDictMaterialStack aIronU = new OreDictMaterialStack(IRON, U);
		OreDictMaterialStack aIronU2 = new OreDictMaterialStack(IRON, U);
		assertEquals(aIronU, aIronU2);
		assertEquals(aIronU2, aIronU);
		assertNotEquals(aIronU, new OreDictMaterialStack(COPPER, U));
		// amounts matching by identity of material, and a negative amount is a wildcard (:84)
		assertTrue(aIronU.equals(new OreDictMaterialStack(IRON, -1)));
		assertTrue(new OreDictMaterialStack(IRON, -1).equals(aIronU));
		// a stack equals its own material object (:82 upstream quirk, kept verbatim)
		assertTrue(aIronU.equals(IRON));
		// but not a different material object
		assertFalse(aIronU.equals(COPPER));
		assertFalse(aIronU.equals(null));
		assertFalse(aIronU.equals("IRON"));
		// hashCode delegates to the material (hashCode = mHashID)
		assertEquals(IRON.hashCode(), aIronU.hashCode());
	}

	@Test
	public void toStringIsMaterialMinusAmount() {
		assertEquals("IRON - " + U, new OreDictMaterialStack(IRON, U).toString());
		assertEquals("NULL - 5", new OreDictMaterialStack(null, 5).toString());
	}

	@Test
	public void addToListMergesByMaterialAndSkipsZero() {
		List<OreDictMaterialStack> aList = new ArrayList<>();
		new OreDictMaterialStack(IRON, U9).addToList(aList);
		new OreDictMaterialStack(COPPER, U).addToList(aList);
		new OreDictMaterialStack(IRON, U9).addToList(aList);
		assertEquals(2, aList.size());
		// same material merged into the existing entry (mutated in place, :100)
		assertEquals(2 * U9, aList.get(0).mAmount);
		// different materials stay separate entries
		assertEquals(U, aList.get(1).mAmount);
		// zero amount is ignored entirely (:99)
		new OreDictMaterialStack(IRON, 0).addToList(aList);
		assertEquals(2, aList.size());
		assertEquals(2 * U9, aList.get(0).mAmount);
		// the added entry is a clone, so later mutation of the source does not leak in (:101);
		// COPPER is already in the list, so this source merges into that entry (:100)
		OreDictMaterialStack aSource = new OreDictMaterialStack(COPPER, U2);
		aSource.addToList(aList);
		assertEquals(2, aList.size());
		assertEquals(U + U2, aList.get(1).mAmount);
		aSource.mAmount = 12345;
		assertEquals(U + U2, aList.get(1).mAmount);
	}
}
