/**
 * Offline guard tests for task issues #29/#34a (GitHub #29b): the generic RM viewer lang faces.
 * Both datagen providers are recorded through same-package recording subclasses (the
 * GT6LangParityTest posture) and the gt6.jei.recipe_map.* / gt6.jei.cost.* domain is pinned:
 * every one of the 75 visible map titles exists in BOTH locales, the en value is the live
 * map's own {@code mNameLocal} (transcription drift fails the gate structurally, not by
 * spot check), and the 16 cost-line keys carry the upstream NEI :680-717 literals verbatim
 * on the en face.
 */
package gregtech6.jei;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.data.PackOutput;
import net.minecraft.server.Bootstrap;

import gregtech6.datagen.GT6EnUs;
import gregtech6.datagen.GT6ZhCn;
import gregtech6.recipes.GT6RecipeMaps;
import gregtech6.recipes.GTRecipesOfflineTestBase;
import gregtech6.recipes.RecipeMap;

class GT6RecipeMapLangTest extends GTRecipesOfflineTestBase {

	private static final String TITLE_PREFIX = GT6RecipeMapViewerMeta.TITLE_KEY_PREFIX;
	private static final List<String> COST_KEYS = List.of(
			GT6RecipeMapViewerMeta.KEY_COSTS, GT6RecipeMapViewerMeta.KEY_USAGE, GT6RecipeMapViewerMeta.KEY_TIER,
			GT6RecipeMapViewerMeta.KEY_TIER_UNSPECIFIED, GT6RecipeMapViewerMeta.KEY_POWER, GT6RecipeMapViewerMeta.KEY_GAIN,
			GT6RecipeMapViewerMeta.KEY_OUTPUT, GT6RecipeMapViewerMeta.KEY_CHANCE, GT6RecipeMapViewerMeta.KEY_CHANCE_EACH,
			GT6RecipeMapViewerMeta.KEY_TIME, GT6RecipeMapViewerMeta.KEY_UNIT_TICKS, GT6RecipeMapViewerMeta.KEY_UNIT_SECS,
			GT6RecipeMapViewerMeta.KEY_UNIT_MINS, GT6RecipeMapViewerMeta.KEY_START, GT6RecipeMapViewerMeta.KEY_TEMPERATURE,
			GT6RecipeMapViewerMeta.NOT_CONSUMED_KEY);

	private static Map<String, String> enEntries;
	private static Map<String, String> zhEntries;

	@BeforeAll
	static void bootAndRecord() {
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// offline-expected
		}
		gregtech6.registry.GTMaterialItems.initMaterials();
		enEntries = record(false);
		zhEntries = record(true);
	}

	/** The GT6LangParityTest recording posture — capture every add() without a datagen run. */
	private static Map<String, String> record(boolean aZh) {
		Map<String, String> tEntries = new HashMap<>();
		PackOutput tOutput = new PackOutput(Path.of("build", "tmp", aZh ? "gt6zhicon-test" : "gt6enicon-test"));
		if (aZh) {
			new GT6ZhCn(tOutput) {
				@Override
				public void add(String aKey, String aValue) {
					tEntries.put(aKey, aValue);
				}

				void run() {
					this.addTranslations(); // the call site lives in the subclass body — the cross-package protected seam
				}
			}.run();
		} else {
			new GT6EnUs(tOutput) {
				@Override
				public void add(String aKey, String aValue) {
					tEntries.put(aKey, aValue);
				}

				void run() {
					this.addTranslations();
				}
			}.run();
		}
		return tEntries;
	}

	@Test
	void everyVisibleMapTitleExistsInBothLocalesAndEnIsTheLiveLocalName() {
		GT6RecipeMaps.reset(); // hermetic: retire boot/sibling generations first (task hermetic-pour-tests)
		GT6RecipeMaps.init();
		for (RecipeMap tMap : GT6RecipeMapViewerMeta.visibleMaps()) {
			String tKey = GT6RecipeMapViewerMeta.titleKey(tMap);
			assertEquals(tMap.mNameLocal, enEntries.get(tKey),
					tKey + " en face must be the live map's mNameLocal (transcription drift)");
			String tZh = zhEntries.get(tKey);
			assertNotNull(tZh, tKey + " missing from the zh face — the #29b regression");
			assertFalse(tZh.isBlank(), tKey + " carries a blank zh face");
		}
	}

	@Test
	void zhTitleDomainCoversExactlyTheSameKeys() {
		long tEnTitles = enEntries.keySet().stream().filter(k -> k.startsWith(TITLE_PREFIX)).count();
		long tZhTitles = zhEntries.keySet().stream().filter(k -> k.startsWith(TITLE_PREFIX)).count();
		assertEquals(75, tEnTitles, "the en title domain is the 75-map census (the nanofab joined in recipe-b6b)");
		assertEquals(tEnTitles, tZhTitles, "zh carries exactly the en title domain (zero-debt contract)");
		// the wording-reuse spot pins (the existing zh machine-name faces)
		assertEquals("焦炉", zhEntries.get(TITLE_PREFIX + "cokeoven"));
		assertEquals("粉碎机", zhEntries.get(TITLE_PREFIX + "shredder"));
		assertEquals("液化炉", zhEntries.get(TITLE_PREFIX + "smelter"));
		assertEquals("低温蒸馏塔", zhEntries.get(TITLE_PREFIX + "cryodistillationtower"));
		// the deliberate divergence: squeezer must NOT collide with juicer's 榨汁机
		assertEquals("压榨机", zhEntries.get(TITLE_PREFIX + "squeezer"));
	}

	@Test
	void costKeysCarryTheUpstreamEnLiteralsAndTheZhFaces() {
		assertEquals(16, COST_KEYS.size());
		for (String tKey : COST_KEYS) {
			assertNotNull(enEntries.get(tKey), tKey + " missing from the en face");
			assertNotNull(zhEntries.get(tKey), tKey + " missing from the zh face");
		}
		// the NEI_RecipeMap :680-717 literals, verbatim on the en face
		assertEquals("Costs: %s GU", enEntries.get(GT6RecipeMapViewerMeta.KEY_COSTS));
		assertEquals("Usage: %s GU/t", enEntries.get(GT6RecipeMapViewerMeta.KEY_USAGE));
		assertEquals("Tier: %s GU", enEntries.get(GT6RecipeMapViewerMeta.KEY_TIER));
		assertEquals("Tier: unspecified", enEntries.get(GT6RecipeMapViewerMeta.KEY_TIER_UNSPECIFIED));
		assertEquals("Power: %s", enEntries.get(GT6RecipeMapViewerMeta.KEY_POWER));
		assertEquals("Gain: %s GU", enEntries.get(GT6RecipeMapViewerMeta.KEY_GAIN));
		assertEquals("Output: %s GU/t", enEntries.get(GT6RecipeMapViewerMeta.KEY_OUTPUT));
		assertEquals("Chance: %s", enEntries.get(GT6RecipeMapViewerMeta.KEY_CHANCE));
		assertEquals("Chance: %s each", enEntries.get(GT6RecipeMapViewerMeta.KEY_CHANCE_EACH));
		assertEquals("Time: %s %s", enEntries.get(GT6RecipeMapViewerMeta.KEY_TIME));
		assertEquals("ticks", enEntries.get(GT6RecipeMapViewerMeta.KEY_UNIT_TICKS));
		assertEquals("secs", enEntries.get(GT6RecipeMapViewerMeta.KEY_UNIT_SECS));
		assertEquals("mins", enEntries.get(GT6RecipeMapViewerMeta.KEY_UNIT_MINS));
		assertEquals("Start: %s%s", enEntries.get(GT6RecipeMapViewerMeta.KEY_START));
		assertEquals("Temperature: %s%s", enEntries.get(GT6RecipeMapViewerMeta.KEY_TEMPERATURE));
		// the :671 not-consumed tooltip, upstream verbatim
		assertEquals("Does not get consumed in the process", enEntries.get(GT6RecipeMapViewerMeta.NOT_CONSUMED_KEY));
		assertEquals("该物品不会被消耗", zhEntries.get(GT6RecipeMapViewerMeta.NOT_CONSUMED_KEY));
	}
}
