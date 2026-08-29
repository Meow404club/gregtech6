/**
 * Tests for task gt-ore-prefix: the OP prefix dataset (batch registration through the
 * PrefixRegistry state machine, fidelity spot checks against upstream values, familiar-prefix
 * derivation, priority-prefix consistency with OreDictMaterial.mPriorityPrefixIndex).
 */
package gregapi.data;

import static gregapi.data.CS.U;
import static gregapi.data.CS.U2;
import static gregapi.data.CS.U4;
import static gregapi.data.CS.U9;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.code.TagData;
import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictMaterialStack;
import gregapi.oredict.OreDictPrefix;
import gregapi.oredict.PrefixRegistry;

public class OPTest {

	static OreDictPrefix sGemChippedDefaultCondition;

	@BeforeAll
	public static void initOP() {
		OP.reset();
		OP.init();
		sGemChippedDefaultCondition = OreDictPrefix.createPrefix("pzDefaultCondition");
	}

	// --- batch registration and full initialization ---

	@Test
	public void everyPrefixFieldIsAssignedByInit() {
		int count = 0;
		for (Field field : OP.class.getFields()) {
			if (field.getType() != OreDictPrefix.class) continue;
			count++;
			try {
				assertNotNull(field.get(null), "OP." + field.getName() + " must be assigned by OP.init()");
			} catch (IllegalAccessException e) {
				throw new AssertionError(e);
			}
		}
		assertEquals(406, count, "406 of the upstream 453 prefix fields are ported (47 deferred entries: OreDictMaterialCondition families + cascaded dependents)");
	}

	@Test
	public void registryIsOpenAndCollectionsAreFilled() {
		assertTrue(PrefixRegistry.INSTANCE.isOpen());
		assertEquals(421, OreDictPrefix.VALUES.size(), "405 create/unused calls + 16 NEW identical-name prefixes of the 17 listed upstream: 'raw' (oreRaw :133) dedupes against the existing unused(\"raw\") prefix (:531), exactly like upstream");
		assertTrue(OreDictPrefix.sPrefixes.size() >= OreDictPrefix.VALUES.size());
	}

	// --- long-prefix-first resolution against the real dataset ---

	@Test
	public void stoneOreFamilyResolvesLongestFirst() {
		assertSame(OP.oreBasalt, OreDictPrefix.get("oreBasaltFe"), "exact name beats the shorter 'ore' prefix");
		assertSame(OP.oreBasalt, OreDictPrefix.get("oreBasaltIron"));
		assertSame(OP.oreSand, OreDictPrefix.get("oreSandIron"), "oreSand (7) beats ore (3)");
		assertSame(OP.ore, OreDictPrefix.get("oreIron"));
		assertSame(OP.dustTiny, OreDictPrefix.get("dustTinyGold"), "dustTiny (8) beats dust (4)");
		assertSame(OP.dustSmall, OreDictPrefix.get("dustSmallGold"));
		assertSame(OP.dust, OreDictPrefix.get("dustGold"));
		assertSame(OP.gemChipped, OreDictPrefix.get("gemChippedRuby"));
		assertSame(OP.gem, OreDictPrefix.get("gemRuby"));
	}

	@Test
	public void identicalNamesReRegisterToTheirTarget() {
		assertSame(OP.ore, OreDictPrefix.sPrefixes.get("oreGem").mTargetRegistration, "oreGem -> ore (upstream OP.java:55)");
		assertSame(OP.oreDense, OreDictPrefix.sPrefixes.get("denseore").mTargetRegistration, "denseore -> oreDense (upstream :124)");
		assertTrue(OreDictPrefix.sPrefixes.get("oreGem").contains(TagData.createTagData("PREFIX.PREFIX_UNUSED")), "identical names carry PREFIX_UNUSED (OreDictPrefix.java:153)");
	}

	// --- U-unit conversion fidelity against upstream numbers ---

	@Test
	public void materialAmountsMatchUpstreamValues() {
		assertEquals(U, OP.dust.mAmount, "dust = U (OP.java:154)");
		assertEquals(U4, OP.dustSmall.mAmount, "dustSmall = U4 (:155)");
		assertEquals(U9, OP.dustTiny.mAmount, "dustTiny = U9 (:156)");
		assertEquals(U9, OP.nugget.mAmount, "nugget = U9 (:170)");
		assertEquals(2 * U, OP.ingotDouble.mAmount, "ingotDouble = 2U (:165)");
		assertEquals(9 * U, OP.plateDense.mAmount, "plateDense = 9U (:191)");
	}

	@Test
	public void stacksizeFormulaMatchesUpstream() {
		assertEquals(64, OP.dust.mDefaultStackSize, "U < 2U -> 64");
		assertEquals(32, OP.ingotDouble.mDefaultStackSize, "64 / 2 = 32 (:162)");
		assertEquals(16, OP.ingotQuadruple.mDefaultStackSize, "64 / 4 = 16 (:163)");
		assertEquals(16, OP.plateDense.mDefaultStackSize, "the 64/9 = 7 default is clamped back UP to setMinStacksize(16) (OreDictPrefix :193-196 + OP.java:191)");
		assertEquals(18, OP.scrapGt.mDefaultStackSize, "scrapGt setStacksize(18) (OP.java:188)");
		assertEquals(64, OP.dust.mMinimumStackSize, "dust setMinStacksize(64) (:154)");
		assertEquals(9, OP.dustTiny.mMinimumStackSize, "dustTiny setMinStacksize(9) (:156)");
	}

	@Test
	public void oreStatsAndCrateWeights() {
		assertEquals(-1, OP.oreBlackgranite.mAmount, "ore prefixes have no material amount (OP.java:57)");
		assertEquals(2 * U, OP.oreBlackgranite.mWeight);
		assertEquals(64 * U, OP.oreBedrock.mWeight, "oreBedrock = 64U (:121)");
		assertTrue(OP.oreBlackgranite.contains(TagData.createTagData("PREFIX.STANDARD_ORE")));
		assertEquals(-1, OP.crateGtRaw.mAmount, "crateGtRaw setMaterialStats(-1, U*32) (:332)");
		assertEquals(32 * U, OP.crateGtRaw.mWeight);
		assertTrue(OP.crateGtRaw.contains(TagData.createTagData("PREFIX.STORAGE_BASED")));
		assertTrue(OP.crateGtRaw.contains(TagData.createTagData("NEI.HIDDEN")), "TD.Creative.HIDDEN seam key (:332)");
	}

	// --- naming templates ---

	@Test
	public void namingTemplatesMatchUpstreamPreAndPost() throws Exception {
		OreDictMaterial mat = MaterialRegistry.INSTANCE.createMaterial(9500, "OpNameMat", "Opium");
		assertEquals("Small Pile of Opium Dust", OP.dustSmall.getLocalizedName(mat), "dustSmall pre/post (OP.java:155)");
		assertEquals("Tiny Pile of Opium Dust", OP.dustTiny.getLocalizedName(mat), "dustTiny pre/post (:156)");
		assertEquals("Double Opium Ingot", OP.ingotDouble.getLocalizedName(mat), "ingotDouble pre/post (:165)");
		assertEquals("Opium Ore", OP.ore.getLocalizedName(mat), "ore pre/post (:55)");
		assertEquals("Opium bearing Rock", OP.rockGt.getLocalizedName(mat), "rockGt post ' bearing Rock' (:141)");
	}

	@Test
	public void categoryNamesAreCarriedVerbatim() {
		assertEquals("Ores", OP.ore.mNameCategory);
		assertEquals("Dusts", OP.dust.mNameCategory);
		assertEquals("Ingots", OP.ingot.mNameCategory);
		assertEquals("1/72nd Dusts", OP.dustDiv72.mNameCategory, "odd category names survive verbatim (:157)");
	}

	// --- familiar prefixes (OP.java:578-600) ---

	@Test
	public void familiarPrefixesDerivedFromTags() {
		assertTrue(OP.dust.mFamiliarPrefixes.contains(OP.dust), "self-familiar");
		assertTrue(OP.dust.mFamiliarPrefixes.contains(OP.dustSmall), "DUST_BASED loop (:591)");
		assertTrue(OP.dust.mFamiliarPrefixes.contains(OP.dustTiny));
		assertTrue(OP.crushed.mFamiliarPrefixes.contains(OP.crushedTiny), "explicit reversal (:578)");
		assertTrue(OP.crushedTiny.mFamiliarPrefixes.contains(OP.crushed));
		assertTrue(OP.ore.mFamiliarPrefixes.contains(OP.blockRaw), "STANDARD_ORE loop adds blockRaw (:585)");
		assertTrue(OP.ingot.mFamiliarPrefixes.contains(OP.nugget), "INGOT_BASED loop (:590)");
		assertFalse(OP.ingot.mFamiliarPrefixes.contains(OP.ingotDouble), "upstream quirk: ingotDouble (:165) does not carry INGOT_BASED, so it is not familiar to ingot");
	}

	// --- conditions and item generation (OreDictPrefix :363-371) ---

	@Test
	public void conditionsDriveGenerationQueries() {
		OreDictMaterial mat = MaterialRegistry.INSTANCE.createMaterial(9501, "OpCondMat", "Condium");
		mat.add(OreDictPrefix.ORES); // ITEMGENERATOR.ORES seam tag
		assertTrue(OP.crushed.canGenerateItem(mat), "crushed condition = ORES (OP.java:135)");
		assertTrue(OP.ore.canGenerateItem(mat), "ore condition = ORES (:55)");
		assertFalse(OP.dustTiny.canGenerateItem(mat), "dustTiny condition = dust -> needs DUSTS/DIRTY_DUSTS (:156)");
		OP.crushed.disableItemGeneration(mat);
		assertFalse(OP.crushed.isGeneratingItem(mat), "blacklist wins in isGeneratingItem");
		assertTrue(OP.crushed.canGenerateItem(mat), "blacklist is invisible to canGenerateItem");
		assertTrue(sGemChippedDefaultCondition.canGenerateItem(mat), "a prefix without setCondition keeps ICondition.TRUE (:76)");
		assertFalse(sGemChippedDefaultCondition.NOT.isTrue(mat), "the NOT condition mirrors the default (:575-576)");
		OreDictPrefix never = OreDictPrefix.createPrefix("pzNeverGenerate").setCondition(gregapi.code.ICondition.FALSE); // like bouleGt (OP.java:183)
		assertFalse(never.canGenerateItem(mat), "ICondition.FALSE blocks generation");
	}

	// --- byproducts model on dataset entries ---

	@Test
	public void byproductModelIsReadyForTheDatasetCard() {
		assertEquals(0, OP.dust.mByProducts.size(), "the MT/ANY byproduct data block (OP.java:639-740) is deferred to the dataset card");
		assertNull(OP.dust.byproduct(0), "byproduct(int) is safe on empty lists (:465)");
		OreDictMaterial mat = MaterialRegistry.INSTANCE.createMaterial(9502, "OpByMat", "Byium");
		OP.dust.mByProducts.add(new OreDictMaterialStack(mat, U9));
		assertSame(OP.dust.mByProducts.get(0).mMaterial, OP.dust.byproduct(0).mMaterial);
		assertEquals(U9, OP.dust.byproduct(0).mAmount);
		OP.dust.mByProducts.clear(); // do not leak into other tests
	}

	// --- mPriorityPrefix consistency (OP.java:627-636 + OreDictMaterial :273-274) ---

	@Test
	public void priorityPrefixMappingCoversAllFiveIndexes() {
		OreDictMaterial gemMat = MaterialRegistry.INSTANCE.createMaterial(9511, "OpPriGem", "PriGem");
		OreDictMaterial dustMat = MaterialRegistry.INSTANCE.createMaterial(9512, "OpPriDust", "PriDust");
		OreDictMaterial ingotMat = MaterialRegistry.INSTANCE.createMaterial(9513, "OpPriIngot", "PriIngot");
		OreDictMaterial plateMat = MaterialRegistry.INSTANCE.createMaterial(9514, "OpPriPlate", "PriPlate");
		OreDictMaterial plateGemMat = MaterialRegistry.INSTANCE.createMaterial(9515, "OpPriPlateGem", "PriPlateGem");
		OreDictMaterial noneMat = MaterialRegistry.INSTANCE.createMaterial(9516, "OpPriNone", "PriNone");
		OreDictMaterial exoticMat = MaterialRegistry.INSTANCE.createMaterial(9517, "OpPriExotic", "PriExotic");

		gemMat.mPriorityPrefixIndex = 1;
		dustMat.mPriorityPrefixIndex = 2;
		ingotMat.mPriorityPrefixIndex = 3;
		plateMat.mPriorityPrefixIndex = 4;
		plateGemMat.mPriorityPrefixIndex = 5;
		noneMat.mPriorityPrefixIndex = 0;
		exoticMat.mPriorityPrefixIndex = 7;

		OP.applyPriorityPrefixes();

		assertSame(OP.gem, gemMat.mPriorityPrefix);
		assertSame(OP.dust, dustMat.mPriorityPrefix);
		assertSame(OP.ingot, ingotMat.mPriorityPrefix);
		assertSame(OP.plate, plateMat.mPriorityPrefix);
		assertSame(OP.plateGem, plateGemMat.mPriorityPrefix);
		assertNull(noneMat.mPriorityPrefix, "index 0 maps to no prefix (upstream :628 'case 0: break')");
		assertNull(exoticMat.mPriorityPrefix, "indexes outside 1..5 stay null (:628-635)");

		// pairwise consistency: whenever the index names a prefix family, the field carries that family
		assertEquals(1, gemMat.mPriorityPrefixIndex);
		assertEquals(gemMat.mPriorityPrefixIndex == 1, gemMat.mPriorityPrefix == OP.gem);
		assertEquals(5, plateGemMat.mPriorityPrefixIndex);
		assertEquals(plateGemMat.mPriorityPrefixIndex == 5, plateGemMat.mPriorityPrefix == OP.plateGem);
	}

	// --- reset roundtrip ---

	@Test
	public void resetAndReinitRoundtrip() {
		assertSame(OP.dust, OreDictPrefix.get("dustGold"), "precondition: resolvable while initialized");
		OP.reset();
		assertNull(OP.ore);
		assertTrue(OreDictPrefix.VALUES.isEmpty(), "the underlying registry is wiped as well");
		assertTrue(PrefixRegistry.INSTANCE.isOpen());
		assertNull(OreDictPrefix.get("dustGold"), "the miss now poisons the negative cache (upstream :131)");
		OP.init();
		assertNotNull(OP.ore);
		assertNull(OreDictPrefix.get("dustGold"), "upstream :121-123: sParsed negative caching has no expiry, only reset clears it");
		assertSame(OP.dust, OreDictPrefix.get("dustGoldIron"), "fresh keys resolve against the re-registered dataset");
		assertEquals(U, OP.dust.mAmount);
	}

	// --- field shape: non-final by design (batch registration) ---

	@Test
	public void prefixFieldsAreNonFinalForBatchRegistration() throws Exception {
		Field ore = OP.class.getField("ore");
		assertTrue(Modifier.isStatic(ore.getModifiers()) && !Modifier.isFinal(ore.getModifiers()),
			"upstream declares public static final fields initialized in class-load order (OP.java:54); the port assigns them in reg* batches so the registry can reset");
	}
}
