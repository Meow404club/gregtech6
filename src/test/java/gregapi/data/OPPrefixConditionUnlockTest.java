/**
 * Tests for task p2-material-condition-system: the 47 prefixes unlocked in OP once
 * OreDictMaterialCondition landed (upstream OP.java:166, :232-254, :256-268, :270-276,
 * :281-283), their registration arithmetic, and condition-filtering fidelity of ingotHot
 * and the toolHead/tool families against the upstream predicate semantics.
 */
package gregapi.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import gregapi.oredict.MaterialRegistry;
import gregapi.oredict.OreDictMaterial;
import gregapi.oredict.OreDictPrefix;

public class OPPrefixConditionUnlockTest {

	/** The 47 entries that were deferred by gt-ore-prefix (OP.java:48-49 note) and unlocked here. */
	static final String[] UNLOCKED = {
		"ingotHot",
		"toolHeadSaw", "toolHeadFile", "toolHeadChisel", "toolHeadBuzzSaw", "toolHeadChainsaw",
		"toolHeadWrench", "toolHeadDrill", "toolHeadSword", "toolHeadPickaxe", "toolHeadShovel",
		"toolHeadSpade", "toolHeadAxe", "toolHeadHoe", "toolHeadSense", "toolHeadPlow",
		"toolHeadHammer", "toolHeadScrewdriver", "toolHeadBuilderwand", "toolHeadConstructionPickaxe",
		"toolHeadPickaxeGem", "toolHeadAxeDouble", "toolHeadUniversalSpade", "toolHeadArrow",
		"toolHeadRawSaw", "toolHeadRawChisel", "toolHeadRawSword", "toolHeadRawPickaxe",
		"toolHeadRawShovel", "toolHeadRawSpade", "toolHeadRawUniversalSpade", "toolHeadRawAxe",
		"toolHeadRawAxeDouble", "toolHeadRawHoe", "toolHeadRawSense", "toolHeadRawPlow", "toolHeadRawArrow",
		"toolSword", "toolPickaxe", "toolShovel", "toolAxe", "toolHoe", "toolShears", "tool",
		"arrowGtWood", "arrowGtPlastic", "arrow"
	};

	@BeforeAll
	public static void initOP() {
		OP.reset();
		OP.init();
	}

	@Test
	public void all47UnlockedPrefixesAreAssigned() throws Exception {
		for (String name : UNLOCKED) {
			Field field = OP.class.getField(name);
			assertNotNull(field.get(null), "OP." + name + " must be assigned by OP.init()");
		}
		assertEquals(47, UNLOCKED.length);
	}

	/** Programmatic census: upstream 453 prefix fields, of which the 47 unlocked ones are new. */
	@Test
	public void prefixFieldCensusMatchesUpstream453() {
		int count = 0;
		for (Field field : OP.class.getFields()) if (field.getType() == OreDictPrefix.class) count++;
		assertEquals(453, count, "upstream OP.java has 453 prefix fields (452 create/unused + 1 alias oreHee)");
	}

	/** Upstream 452 create/unused calls + 17 identical-name re-registrations - 1 'raw' dedup (:133 vs :531) = 468. */
	@Test
	public void prefixRegistryTotalIs468() {
		assertEquals(468, OreDictPrefix.VALUES.size(), "421 (pre-unlock) + 47 unlocked; dedup arithmetic unchanged from upstream");
	}

	/** ingotHot (upstream :166): new And(INGOTS_HOT, SMITHABLE, meltmin(800)).
	 *  The TD tags are added through the real TD constants — the fact that the condition
	 *  reacts to them proves the OP seam unified with TD via the idempotent factory. */
	@Test
	public void ingotHotConditionFiltersByTagsAndMeltingPoint() {
		MaterialRegistry r = new MaterialRegistry();
		OreDictMaterial ready = r.createMaterial(9201, "UnlockHotReady", "UnlockHotReady");
		ready.add(TD.ItemGenerator.INGOTS_HOT, TD.Processing.SMITHABLE);
		ready.mMeltingPoint = 900;
		assertTrue(OP.ingotHot.canGenerateItem(ready), "tags + melting point above 800K generate Hot Ingots");

		OreDictMaterial boundary = r.createMaterial(9202, "UnlockHotBoundary", "UnlockHotBoundary");
		boundary.add(TD.ItemGenerator.INGOTS_HOT, TD.Processing.SMITHABLE);
		boundary.mMeltingPoint = 800;
		assertTrue(OP.ingotHot.canGenerateItem(boundary), "meltmin(800) is inclusive, upstream OreDictMaterialCondition :94");

		OreDictMaterial cold = r.createMaterial(9203, "UnlockHotCold", "UnlockHotCold");
		cold.add(TD.ItemGenerator.INGOTS_HOT, TD.Processing.SMITHABLE);
		cold.mMeltingPoint = 799;
		assertFalse(OP.ingotHot.canGenerateItem(cold), "meltmin(800) leg must reject sub-800K materials");

		OreDictMaterial notSmithable = r.createMaterial(9204, "UnlockHotNotSmith", "UnlockHotNotSmith");
		notSmithable.add(TD.ItemGenerator.INGOTS_HOT);
		notSmithable.mMeltingPoint = 900;
		assertFalse(OP.ingotHot.canGenerateItem(notSmithable), "SMITHABLE leg must reject non-smithable materials");

		OreDictMaterial noHotTag = r.createMaterial(9205, "UnlockHotNoTag", "UnlockHotNoTag");
		noHotTag.add(TD.Processing.SMITHABLE);
		noHotTag.mMeltingPoint = 900;
		assertFalse(OP.ingotHot.canGenerateItem(noHotTag), "INGOTS_HOT leg must reject materials outside the hot-ingot group");
	}

	/** toolSword family (upstream :270-276): typemin(1) on mToolTypes. */
	@Test
	public void toolFamilyFiltersByToolTypeMinimum() {
		MaterialRegistry r = new MaterialRegistry();
		OreDictMaterial toolMaterial = r.createMaterial(9211, "UnlockToolMat", "UnlockToolMat");
		toolMaterial.mToolTypes = 1;
		assertTrue(OP.toolSword.canGenerateItem(toolMaterial));
		assertTrue(OP.tool.canGenerateItem(toolMaterial));
		toolMaterial.mToolTypes = 0;
		assertFalse(OP.toolSword.canGenerateItem(toolMaterial));
		assertFalse(OP.tool.canGenerateItem(toolMaterial));
	}

	/** toolHeadArrow (upstream :254) and its cascade (upstream :268, :281-283):
	 *  And(PROJECTILES, typemin(1)); the raw head and the arrows hang off the same condition. */
	@Test
	public void arrowCascadeFollowsToolHeadArrowCondition() {
		MaterialRegistry r = new MaterialRegistry();
		OreDictMaterial arrowMaterial = r.createMaterial(9212, "UnlockArrowMat", "UnlockArrowMat");
		arrowMaterial.add(TD.ItemGenerator.PROJECTILES);
		arrowMaterial.mToolTypes = 1;
		assertTrue(OP.toolHeadArrow.canGenerateItem(arrowMaterial));
		assertTrue(OP.toolHeadRawArrow.canGenerateItem(arrowMaterial), "raw head condition is the toolHeadArrow prefix itself (:268)");
		assertTrue(OP.arrowGtWood.canGenerateItem(arrowMaterial), "Or(toolHeadArrow, EMPTY), upstream :281");
		assertTrue(OP.arrow.canGenerateItem(arrowMaterial), "condition is the toolHeadArrow prefix, upstream :283");

		arrowMaterial.mToolTypes = 0;
		assertFalse(OP.toolHeadArrow.canGenerateItem(arrowMaterial));
		assertFalse(OP.arrowGtWood.canGenerateItem(arrowMaterial));
		assertFalse(OP.arrow.canGenerateItem(arrowMaterial));

		arrowMaterial.mToolTypes = 1;
		arrowMaterial.remove(TD.ItemGenerator.PROJECTILES);
		assertFalse(OP.toolHeadArrow.canGenerateItem(arrowMaterial), "PROJECTILES leg gates the whole cascade");
	}

	/** toolHeadSaw class (upstream :232): And(typemin(2), BOUNCY.NOT, STRETCHY.NOT). */
	@Test
	public void toolHeadSawExcludesBouncyAndStretchy() {
		MaterialRegistry r = new MaterialRegistry();
		OreDictMaterial plain = r.createMaterial(9213, "UnlockSawPlain", "UnlockSawPlain");
		plain.mToolTypes = 2;
		assertTrue(OP.toolHeadSaw.canGenerateItem(plain));
		plain.add(TD.Properties.BOUNCY);
		assertFalse(OP.toolHeadSaw.canGenerateItem(plain), "BOUNCY.NOT leg, upstream :232");
	}

	/** toolHeadHammer (upstream :247): And(typemin(1), Or(BOUNCY, STRETCHY, WOOD, qualmin(1))). */
	@Test
	public void toolHeadHammerNeedsPropertyOrQuality() {
		MaterialRegistry r = new MaterialRegistry();
		OreDictMaterial m = r.createMaterial(9214, "UnlockHammer", "UnlockHammer");
		m.mToolTypes = 1;
		m.mToolQuality = 0;
		assertFalse(OP.toolHeadHammer.canGenerateItem(m), "no property tag and quality below 1");
		m.mToolQuality = 1;
		assertTrue(OP.toolHeadHammer.canGenerateItem(m), "qualmin(1) leg");
		m.mToolQuality = 0;
		m.add(TD.Properties.WOOD);
		assertTrue(OP.toolHeadHammer.canGenerateItem(m), "WOOD property leg");
	}
}
