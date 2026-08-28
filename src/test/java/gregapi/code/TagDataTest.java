/**
 * Tests for the TagData condition system, ported from GregTech 6 gregapi/code/TagData.java,
 * ICondition.java and ITagDataContainer.java. Assertions mirror upstream semantics
 * (identity-by-creation, mTagID-based equality, NOT/combinator behavior).
 */

package gregapi.code;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class TagDataTest {
	private static final TagData TAG_A = TagData.createTagData("TEST.COND.A");
	private static final TagData TAG_B = TagData.createTagData("TEST.COND.B");
	private static final TagData TAG_C = TagData.createTagData("TEST.COND.C");

	@Test
	public void creationIsIdempotentByNameAndNormalizesToUppercase() {
		assertSame(TAG_A, TagData.createTagData("TEST.COND.A"));
		// upstream createTagData(aName) uppercases before the dedupe loop (TagData.java:74-78)
		assertSame(TAG_A, TagData.createTagData("test.cond.a"));
		assertEquals("TEST.COND.A", TAG_A.mName);
	}

	@Test
	public void equalityIsTagIDBasedAndConsistentWithHashCode() {
		assertNotEquals(TAG_A, TAG_B);
		assertNotEquals(TAG_A.mTagID, TAG_B.mTagID);
		assertEquals(TAG_A.mTagID, TAG_A.hashCode());
		assertEquals(TAG_A, TAG_A);
		assertNotEquals(TAG_A, null);
		assertNotEquals(TAG_A, "TEST.COND.A");
	}

	@Test
	public void asListContainsOnlySelf() {
		assertEquals(Arrays.asList(TAG_A), TAG_A.AS_LIST);
		assertThrows(UnsupportedOperationException.class, () -> TAG_A.AS_LIST.add(TAG_B));
	}

	@Test
	public void isTrueChecksContainerMembership() {
		ITagDataContainer.BasicTagDataContainer aContainer = new ITagDataContainer.BasicTagDataContainer();
		assertFalse(TAG_A.isTrue(aContainer));
		aContainer.add(TAG_A);
		assertTrue(TAG_A.isTrue(aContainer));
	}

	@Test
	public void containerAddContainsAllRemove() {
		ITagDataContainer.BasicTagDataContainer aContainer = new ITagDataContainer.BasicTagDataContainer();
		assertSame(aContainer, aContainer.add(TAG_A, TAG_B));
		assertTrue(aContainer.contains(TAG_A));
		assertTrue(aContainer.containsAll(TAG_A, TAG_B));
		assertFalse(aContainer.containsAll(TAG_A, TAG_C));
		assertTrue(aContainer.containsAll(Arrays.asList(TAG_A, TAG_B)));
		// upstream: add(...) with a null element is silently ignored (HashSetNoNulls.add)
		assertSame(aContainer, aContainer.add((TagData)null));
		assertFalse(aContainer.contains(null));
		// remove returns whether the Tag was there before (ITagDataContainer.java:53-55)
		assertTrue(aContainer.remove(TAG_A));
		assertFalse(aContainer.remove(TAG_A));
		assertFalse(aContainer.contains(TAG_A));
		assertTrue(aContainer.contains(TAG_B));
	}

	@Test
	public void notConditionNegatesMembership() {
		ITagDataContainer.BasicTagDataContainer aContainer = new ITagDataContainer.BasicTagDataContainer().add(TAG_A);
		assertFalse(TAG_A.NOT.isTrue(aContainer));
		assertTrue(TAG_B.NOT.isTrue(aContainer));
	}

	@Test
	public void conditionCombinators() {
		ITagDataContainer.BasicTagDataContainer aContainer = new ITagDataContainer.BasicTagDataContainer().add(TAG_A);
		assertTrue(new ICondition.And<>(TAG_A, TAG_B.NOT).isTrue(aContainer));
		assertFalse(new ICondition.And<>(TAG_A, TAG_B).isTrue(aContainer));
		assertTrue(new ICondition.Or<>(TAG_B, TAG_A).isTrue(aContainer));
		assertFalse(new ICondition.Or<>(TAG_B, TAG_C).isTrue(aContainer));
		assertTrue(new ICondition.Nor<>(TAG_B, TAG_C).isTrue(aContainer));
		assertFalse(new ICondition.Nor<>(TAG_B, TAG_A).isTrue(aContainer));
		assertTrue(new ICondition.Nand<>(TAG_B, TAG_C).isTrue(aContainer));
		// NAND is only false when ALL conditions hold: TAG_B is absent, so NAND stays true
		assertTrue(new ICondition.Nand<>(TAG_B, TAG_A).isTrue(aContainer));
		assertFalse(new ICondition.Nand<>(TAG_A, TAG_A).isTrue(aContainer));
		assertTrue(new ICondition.Xor<>(TAG_A, TAG_B).isTrue(aContainer));
		assertFalse(new ICondition.Xor<>(TAG_A, TAG_A).isTrue(aContainer));
		assertTrue(new ICondition.Equal<>(TAG_A, TAG_A).isTrue(aContainer));
		assertFalse(new ICondition.Equal<>(TAG_A, TAG_B).isTrue(aContainer));
		// every combinator exposes its own NOT (ICondition.java:54,70,86,102,118,133)
		assertTrue(new ICondition.And<>(TAG_A, TAG_B).NOT.isTrue(aContainer));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void conditionConstants() {
		assertTrue(((ICondition<Object>)ICondition.TRUE).isTrue(new Object()));
		assertFalse(((ICondition<Object>)ICondition.FALSE).isTrue(new Object()));
		assertTrue(((ICondition<Object>)ICondition.NULL).isTrue(null));
		assertFalse(((ICondition<Object>)ICondition.NOTNULL).isTrue(null));
		assertTrue(((ICondition<Object>)ICondition.NOTNULL).isTrue(new Object()));
	}

	@Test
	public void translatableAndLocalisedNames() {
		assertEquals("gt.td.long.test.cond.a", TAG_A.getTranslatableNameLong());
		assertEquals("gt.td.short.test.cond.a", TAG_A.getTranslatableNameShort());
		// LH stripped: un-localized fallback is the raw name (upstream LH.get(key, mName))
		assertEquals("TEST.COND.A", TAG_A.getLocalisedNameLong());
		assertEquals("TEST.COND.A", TAG_A.getLocalisedNameShort());
		TagData tChat = TagData.createTagData("TEST.COND.CHAT", "Short", "Long", "CHATCODE");
		assertEquals("CHATCODE", tChat.getChatFormat());
		assertEquals("CHATCODELONG", tChat.getLocalisedChatNameLong().replace("TEST.COND.CHAT", "LONG"));
	}

	@Test
	public void tagsRegistryGrowsWithDistinctTags() {
		int tSizeBefore = TagData.TAGS.size();
		TagData tNew = TagData.createTagData("TEST.COND.REGISTRY");
		assertEquals(tSizeBefore + 1, TagData.TAGS.size());
		assertTrue(TagData.TAGS.contains(tNew));
		// idempotent creation must not grow the registry
		assertSame(tNew, TagData.createTagData("TEST.COND.REGISTRY"));
		assertEquals(tSizeBefore + 1, TagData.TAGS.size());
	}

	@Test
	public void toStringIsTheTagName() {
		assertEquals("TEST.COND.A", TAG_A.toString());
	}
}
