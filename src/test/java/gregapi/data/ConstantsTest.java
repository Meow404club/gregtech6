/**
 * Tests for the minimal CS constants (T/F, U/UD unit family) and the minimal TD tag subset,
 * ported from GregTech 6 gregapi/data/CS.java:96-122 and TD.java:462-467,567.
 * The U-family precision contract is documented at CS.java:112-114.
 */

package gregapi.data;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import gregapi.code.TagData;

public class ConstantsTest {
	@Test
	public void booleanShorthands() {
		assertTrue(CS.T);
		assertFalse(CS.F);
	}

	@Test
	public void materialUnitValue() {
		assertEquals(648648000L, CS.U);
	}

	/**
	 * Upstream contract (CS.java:112-114): U is divisible without precision loss by the
	 * documented divisor list. Porting discovery: the family ALSO defines U17/U128/U256/U512,
	 * which are NOT exact divisors (17 is no factor of U; 2^7..2^9 exceed U's 2^6) - upstream
	 * keeps them as plain truncated U/d, and its own precision comment does not list them.
	 * So: U/d must always equal the truncated quotient, and additionally be exact
	 * (U % d == 0, reconstructing U) for every divisor except 17/128/256/512.
	 */
	@Test
	public void unitFamilyDividesUWithoutPrecisionLoss() throws Exception {
		Set<Long> tExactDivisors = new HashSet<>();
		for (Field tField : CS.class.getDeclaredFields()) {
			String tName = tField.getName();
			if (!tName.matches("U\\d+")) continue;
			long tDivisor = Long.parseLong(tName.substring(1));
			long tValue = tField.getLong(null);
			// every U<n> constant is the truncated U/n (upstream source definition, CS.java:120)
			assertEquals(CS.U / tDivisor, tValue, "U" + tDivisor + " must equal U/" + tDivisor);
			if (tDivisor == 17 || tDivisor == 128 || tDivisor == 256 || tDivisor == 512) {
				assertTrue(CS.U % tDivisor != 0, "U must NOT be exactly divisible by " + tDivisor);
			} else {
				assertEquals(0L, CS.U % tDivisor, "U must be exactly divisible by " + tDivisor);
				assertEquals(CS.U, tValue * tDivisor, "U" + tDivisor + " * " + tDivisor + " must reconstruct U");
				tExactDivisors.add(tDivisor);
			}
		}
		// spot-check the documented precision list is really covered (CS.java:112-114;
		// note upstream's comment mentions 21/22/81 but the constant family defines no such fields)
		assertTrue(tExactDivisors.containsAll(java.util.Arrays.asList(2L, 3L, 9L, 16L, 64L, 96L, 144L, 1000L, 1440L)));
	}

	@Test
	public void nuggetIsOneNinthOfAnIngot() {
		// CS.java:118 "For example Nugget = U / 9 as it contains out of 1/9th of an Ingot."
		assertEquals(CS.U / 9, CS.U9);
	}

	@Test
	public void doubleMaterialUnitMatchesU() {
		assertEquals((double)CS.U, CS.UD, 0.0);
	}

	@Test
	public void tdTagSubsetMatchesUpstreamKeys() {
		assertEquals("PROPERTIES.AUTO_BLACKLIST", TD.Properties.AUTO_BLACKLIST.mName);
		assertEquals("PROPERTIES.AUTO_MATERIAL", TD.Properties.AUTO_MATERIAL.mName);
		assertEquals("PROPERTIES.INVALID_MATERIAL", TD.Properties.INVALID_MATERIAL.mName);
		assertEquals("PROPERTIES.UNUSED_MATERIAL", TD.Properties.UNUSED_MATERIAL.mName);
		assertEquals("ITEMGENERATOR.LENSES", TD.ItemGenerator.LENSES.mName);
		// toString is the tag name (TagData.toString)
		assertEquals("PROPERTIES.INVALID_MATERIAL", TD.Properties.INVALID_MATERIAL.toString());
	}

	@Test
	public void tdTagsAreDistinctAndGloballyIdempotent() {
		Set<TagData> tTags = new HashSet<>(java.util.Arrays.asList(
			TD.Properties.AUTO_BLACKLIST, TD.Properties.AUTO_MATERIAL,
			TD.Properties.INVALID_MATERIAL, TD.Properties.UNUSED_MATERIAL, TD.ItemGenerator.LENSES));
		assertEquals(5, tTags.size());
		// cross-class idempotency: creating the same tag name elsewhere yields the same instance
		assertSame(TD.Properties.INVALID_MATERIAL, TagData.createTagData("PROPERTIES.INVALID_MATERIAL"));
		assertSame(TD.ItemGenerator.LENSES, TagData.createTagData("itemgenerator.lenses"));
	}
}
