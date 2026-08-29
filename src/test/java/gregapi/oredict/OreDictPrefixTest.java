/**
 * Tests for task gt-ore-prefix: the OreDictPrefix model (long-prefix-first resolution with
 * negative caching, U-unit amount conversion with the stacksize formula, naming template with
 * the material local-name fallback, byproduct data, prefix registry state machine).
 *
 * The prefix collections are global statics (upstream shape), so this class resets the shared
 * PrefixRegistry in @BeforeAll and registers uniquely-named ("pz" prefixed) fixtures; OPTest
 * does the same with OP.reset()/OP.init(). Class-level isolation holds because JUnit runs the
 * classes one at a time.
 */
package gregapi.oredict;

import static gregapi.data.CS.U;
import static gregapi.data.CS.U2;
import static gregapi.data.CS.U4;
import static gregapi.data.CS.U9;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.code.TagData;

public class OreDictPrefixTest {

	static OreDictPrefix sOre, sOreSand, sOreSandstone, sDust, sDustSmall, sDustTiny, sGem, sGemChipped;

	@BeforeAll
	public static void setUpRegistry() {
		PrefixRegistry.INSTANCE.reset();
		sOre          = OreDictPrefix.createPrefix("pzOre"         ).setLocalItemName(""      , " Ore");
		sOreSand      = OreDictPrefix.createPrefix("pzOreSand"     ).setLocalItemName("Sand " , " Ore");
		sOreSandstone = OreDictPrefix.createPrefix("pzOreSandstone").setLocalItemName("Sandstone ", " Ore");
		sDust         = OreDictPrefix.createPrefix("pzDust"        ).setLocalItemName(""      , " Dust");
		sDustSmall    = OreDictPrefix.createPrefix("pzDustSmall"   ).setLocalItemName("Small Pile of ", " Dust");
		sDustTiny     = OreDictPrefix.createPrefix("pzDustTiny"    ).setLocalItemName("Tiny Pile of " , " Dust");
		sGem          = OreDictPrefix.createPrefix("pzGem"         );
		sGemChipped   = OreDictPrefix.createPrefix("pzGemChipped"  );
	}

	// --- long-prefix-first resolution (OreDictPrefix.get, upstream :120-133) ---

	@Test
	public void longerPrefixesWinOverShorterOnes() {
		assertSame(sOreSandstone, OreDictPrefix.get("pzOreSandstoneFe"), "the 14-char prefix beats the shorter ones it starts with");
		assertSame(sOreSand, OreDictPrefix.get("pzOreSandFe"), "the 9-char prefix beats the 5-char one");
		assertSame(sOre, OreDictPrefix.get("pzOreFe"), "the 5-char prefix is the only match here");
		assertSame(sDustSmall, OreDictPrefix.get("pzDustSmallFe"));
		assertSame(sDustTiny, OreDictPrefix.get("pzDustTinyFe"));
		assertSame(sDust, OreDictPrefix.get("pzDustFe"));
		assertSame(sGemChipped, OreDictPrefix.get("pzGemChippedFe"));
		assertSame(sGem, OreDictPrefix.get("pzGemFe"));
	}

	@Test
	public void parseResultsAreCachedByIdentity() {
		assertSame(OreDictPrefix.get("pzOreSandFe"), OreDictPrefix.get("pzOreSandFe"), "sParsed returns the same instance for the same input");
	}

	@Test
	public void unknownNamesResolveToNullRepeatedly() {
		assertNull(OreDictPrefix.get("pzNoSuchPrefixFe"));
		assertNull(OreDictPrefix.get("pzNoSuchPrefixFe"), "the negative cache must also return null on the second call");
	}

	@Test
	public void resolutionIsCaseSensitive() {
		assertNull(OreDictPrefix.get("PZDUSTFE"), "upstream startsWith matching is exact-case (:126)");
		assertNull(OreDictPrefix.get("pzdustfe"));
	}

	@Test
	public void resetClearsTheNegativeParseCache() {
		assertNull(OreDictPrefix.get("pzAfterResetFe"), "no such prefix yet");
		PrefixRegistry.INSTANCE.reset();
		OreDictPrefix created = OreDictPrefix.createPrefix("pzAfterReset");
		assertSame(created, OreDictPrefix.get("pzAfterResetFe"), "a stale negative cache would still return null here");
		// rebuild the fixtures for the remaining tests
		setUpRegistry();
	}

	// --- constructor invariants (upstream :99-118) ---

	@Test
	public void sortedListIsOrderedByDescendingLength() {
		int previous = Integer.MAX_VALUE;
		for (OreDictPrefix prefix : OreDictPrefix.VALUES_SORTED) {
			assertTrue(prefix.mNameInternal.length() <= previous, "VALUES_SORTED must be non-increasing in name length");
			previous = prefix.mNameInternal.length();
		}
		assertTrue(OreDictPrefix.VALUES_SORTED.containsAll(java.util.Arrays.asList(sOre, sOreSand, sOreSandstone)));
	}

	@Test
	public void registrationOrderIsPreservedInValues() {
		assertTrue(OreDictPrefix.VALUES.indexOf(sOre) < OreDictPrefix.VALUES.indexOf(sOreSandstone), "VALUES keeps insertion order (upstream :105)");
	}

	@Test
	public void createPrefixNormalizesSpacesAndMinuses() {
		OreDictPrefix p = OreDictPrefix.createPrefix("pz Nor-Mal");
		assertEquals("pzNorMal", p.mNameInternal, "spaces and minuses are stripped (:94)");
		assertEquals("pz Nor-Mal", p.mNameLocal, "the local name keeps the original spelling (:102)");
		assertEquals("pz Nor-Mal", p.mNameCategory);
		assertSame(p, OreDictPrefix.createPrefix("pzNorMal"), "same normalized name returns the same prefix (:95-96)");
	}

	@Test
	public void invalidPrefixNamesAreRejected() {
		assertThrows(IllegalArgumentException.class, () -> OreDictPrefix.createPrefix("ab"), "names below 3 chars would break other prefixes (:104)");
		assertThrows(IllegalArgumentException.class, () -> OreDictPrefix.createPrefix("a|b"), "pipe is one of the five invalid characters (:103)");
		assertThrows(IllegalArgumentException.class, () -> OreDictPrefix.createPrefix("a*b"));
		assertThrows(IllegalArgumentException.class, () -> OreDictPrefix.createPrefix("a:b"));
		assertThrows(IllegalArgumentException.class, () -> OreDictPrefix.createPrefix("a.b"));
		assertThrows(IllegalArgumentException.class, () -> OreDictPrefix.createPrefix("a$b"));
	}

	// --- U-unit amount conversion (upstream :158-180) ---

	@Test
	public void materialStatsConvertIntoStacksizes() {
		OreDictPrefix dust = OreDictPrefix.createPrefix("pzStatDust").setMaterialStats(U);
		assertEquals(U, dust.mAmount);
		assertEquals(U, dust.mWeight);
		assertEquals(64, dust.mDefaultStackSize, "amount < 2U keeps stacksize 64 (:162)");
		assertTrue(dust.contains(TagData.createTagData("PREFIX.MATERIAL_BASED")), "setMaterialStats tags MATERIAL_BASED (:159)");

		OreDictPrefix ingotDouble = OreDictPrefix.createPrefix("pzStatDouble").setMaterialStats(U * 2);
		assertEquals(32, ingotDouble.mDefaultStackSize, "64 / (2U / U) = 32 (:162)");

		OreDictPrefix ingotQuadruple = OreDictPrefix.createPrefix("pzStatQuad").setMaterialStats(U * 4);
		assertEquals(16, ingotQuadruple.mDefaultStackSize, "64 / 4 = 16");

		OreDictPrefix plateDense = OreDictPrefix.createPrefix("pzStatDense").setMaterialStats(U * 9);
		assertEquals(7, plateDense.mDefaultStackSize, "64 / 9 = 7, integer division");
	}

	@Test
	public void materialStatsWithSeparateWeightKeepAmountMinusOne() {
		OreDictPrefix crate = OreDictPrefix.createPrefix("pzStatCrate").setMaterialStats(-1, U * 32); // upstream OP.java:332 crateGtRaw
		assertEquals(-1, crate.mAmount);
		assertEquals(U * 32, crate.mWeight);
		assertEquals(64, crate.mDefaultStackSize, "mAmount -1 < 2U -> stacksize 64 (:162)");
	}

	@Test
	public void stacksizeBoundariesAreClamped() {
		OreDictPrefix p = OreDictPrefix.createPrefix("pzStatClamp");
		p.setStacksize(1, 1);
		assertEquals(1, p.mMinimumStackSize);
		assertEquals(1, p.mDefaultStackSize);
		p.setStacksize(999);
		assertEquals(64, p.mDefaultStackSize, "bind_ clamps to 64 (upstream UT.java:1547-1549)");
		p.setMinStacksize(999);
		assertEquals(64, p.mMinimumStackSize, "bind_ clamps the minimum to 64 as well (:194)");
		assertEquals(64, p.mDefaultStackSize);
	}

	@Test
	public void oreStatsLeaveAmountUnknownAndTagOre() {
		OreDictPrefix ore = OreDictPrefix.createPrefix("pzStatOre").setOreStats(U * 2);
		assertEquals(-1, ore.mAmount, "ore prefixes carry no material amount (:176)");
		assertEquals(U * 2, ore.mWeight);
		assertTrue(ore.contains(TagData.createTagData("PREFIX.MATERIAL_BASED")));
		assertTrue(ore.contains(TagData.createTagData("PREFIX.UNIFICATABLE")));
		assertTrue(ore.contains(TagData.createTagData("PREFIX.ORE")));
		assertTrue(ore.contains(TagData.createTagData("PREFIX.TOOLTIP_ENCHANTS")));
	}

	// --- naming template (upstream OP.java:49 + LanguageHandler.java:579) ---

	@Test
	public void namingTemplateWrapsTheMaterialName() throws Exception {
		OreDictMaterial mat = new MaterialRegistry().createMaterial(9400, "PzNameMat", "Ironish");
		assertEquals("Small Pile of Ironish Dust", sDustSmall.getLocalizedName(mat), "pre + material + post (LanguageHandler.java:579)");
		assertEquals("Sandstone Ironish Ore", sOreSandstone.getLocalizedName(mat));
		assertEquals("Ironish Ore", sOre.getLocalizedName(mat));
	}

	@Test
	public void materialLocalNameFallsBackToInternalName() throws Exception {
		OreDictMaterial mat = new MaterialRegistry().createMaterial(9401, "PzNameMat2", null);
		mat.setLocal(null); // upstream :330-333 / ported :234-236: null local falls back to the internal name
		assertEquals("Small Pile of PzNameMat2 Dust", sDustSmall.getLocalizedName(mat));
	}

	@Test
	public void setLocalPrefixNameOverridesTheCategoryDefault() {
		OreDictPrefix p = OreDictPrefix.createPrefix("pzNameOverride").setCategoryName("First");
		assertEquals("pzNameOverride", p.mNameLocal, "the constructor seeds mNameLocal from aNameLocal (:102); setCategoryName does not touch it");
		assertEquals("First", p.mNameCategory);
		p.setLocalPrefixName("Second");
		assertEquals("Second", p.mNameLocal, "setLocalPrefixName is the OP.java:49-50 create() path for the display name");
		assertEquals("First", p.mNameCategory, "mNameCategory stays untouched by setLocalPrefixName");
	}

	// --- byproducts (upstream :87-88, :464-466) ---

	@Test
	public void byproductsAreIndexedAccessData() throws Exception {
		OreDictMaterial mat = new MaterialRegistry().createMaterial(9402, "PzByMat", "By");
		OreDictPrefix p = OreDictPrefix.createPrefix("pzByProduct");
		assertNull(p.byproduct(0), "no byproducts yet");
		OreDictMaterialStack stack = new OreDictMaterialStack(mat, U4);
		p.mByProducts.add(stack);
		assertSame(stack, p.byproduct(0));
		assertNull(p.byproduct(1), "out-of-range index returns null instead of throwing (:465)");
		assertNull(p.byproduct(999));
	}

	// --- familiar prefixes (upstream :85-86, :141-147) ---

	@Test
	public void familiarPrefixesContainSelfAndReverse() {
		assertTrue(sDust.mFamiliarPrefixes.contains(sDust), "the constructor adds the prefix itself (:117)");
		OreDictPrefix tiny = OreDictPrefix.createPrefix("pzFamTiny");
		sDust.addFamiliarPrefixWithReversal(tiny);
		assertTrue(sDust.mFamiliarPrefixes.contains(tiny));
		assertTrue(tiny.mFamiliarPrefixes.contains(sDust), "WithReversal is symmetric (:145-147)");
	}

	// --- registration state machine (PrefixRegistry; upstream gate at :93) ---

	@Test
	public void closedRegistryRejectsPrefixCreation() {
		PrefixRegistry registry = PrefixRegistry.INSTANCE;
		assertTrue(registry.isOpen());
		registry.close();
		assertThrows(IllegalStateException.class, () -> OreDictPrefix.createPrefix("pzTooLate"), "upstream: Prefixes have to be initialised in PreInit or earlier! (:93)");
		assertNull(OreDictPrefix.sPrefixes.get("pzTooLate"));
		registry.open();
		assertTrue(registry.isOpen());
	}

	@Test
	public void resetWipesAllCollectionsAndReopens() {
		OreDictPrefix p = OreDictPrefix.createPrefix("pzResetMe");
		assertFalse(OreDictPrefix.sPrefixes.isEmpty());
		PrefixRegistry.INSTANCE.reset();
		assertTrue(OreDictPrefix.sPrefixes.isEmpty());
		assertTrue(OreDictPrefix.VALUES.isEmpty());
		assertTrue(OreDictPrefix.VALUES_SORTED.isEmpty());
		assertTrue(OreDictPrefix.sParsed.isEmpty());
		assertSame(p.getClass(), OreDictPrefix.createPrefix("pzResetMe").getClass(), "after reset the name is free again and can be re-registered");
		setUpRegistry();
	}

	// --- condition plumbing (upstream :363-371, :570-576) ---

	@Test
	public void conditionAndBlacklistDriveItemGeneration() throws Exception {
		MaterialRegistry registry = new MaterialRegistry();
		OreDictMaterial mat = registry.createMaterial(9403, "PzCondMat", "Cond");
		mat.add(OreDictPrefix.ORES); // the seam tag; also proves the idempotent factory shares instances
		assertSame(TagData.createTagData("ITEMGENERATOR.ORES"), OreDictPrefix.ORES);
		OreDictPrefix ore = OreDictPrefix.createPrefix("pzCondOre").setOreStats(U); // condition = ORES (:178)
		assertTrue(ore.canGenerateItem(mat), "material carries the ORES tag, so the TagData condition passes");
		ore.disableItemGeneration(mat);
		assertFalse(ore.isGeneratingItem(mat), "blacklisted materials do not generate (:365)");
		assertTrue(ore.canGenerateItem(mat), "canGenerateItem ignores the blacklist (:370)");
		assertTrue(sGemChipped.canGenerateItem(mat), "a prefix without setCondition keeps ICondition.TRUE (:76)");
		assertFalse(sGemChipped.NOT.isTrue(mat), "the NOT condition mirrors the default (:575-576)");
		OreDictPrefix never = OreDictPrefix.createPrefix("pzNeverGenerate").setCondition(gregapi.code.ICondition.FALSE); // like bouleGt (OP.java:183)
		assertFalse(never.canGenerateItem(mat), "ICondition.FALSE blocks generation");
	}
}
