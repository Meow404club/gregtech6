/**
 * Offline guard test for task p20-i18n-zhcn-provider (ADR 2026-09-06-p20-i18n-zhcn-pipeline
 * §1.3): the zh_cn key set is structurally pinned to the en_us key set, the composed domains
 * are pinned ABSENT from zh until their B-wave cards land, and the zh coverage floor is a
 * ratchet. Same posture as GT6EnUsJeiInfoTest:52-64 — a recording LanguageProvider subclass
 * (add is public and non-final) captures every entry addTranslations() would emit, in a bare
 * JVM, no datagen run needed (the generated files themselves are gated by runData + the
 * second-run written:0 check).
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.data.PackOutput;
import net.minecraft.server.Bootstrap;

import gregtech6.registry.GTMaterialItems;

public class GT6LangParityTest {

	/**
	 * The zh_cn coverage floor — a RATCHET: it may only be raised (by an explicit PR changing
	 * this constant, with the new measured key count in the message), never lowered. Initial
	 * value per the task card: material ~1770 + tabs ~110 + tagprefix ~102 + misc ~17.
	 */
	private static final int ZH_KEY_FLOOR = 1800;

	/**
	 * A-wave negative assertions (ADR §1.3): the COMPOSED domains stay absent from zh — those
	 * en keys are pre-installed full strings that the B-wave cards replace with template keys
	 * (p20-i18n-compose-wires then p20-i18n-compose-rows); every card SHRINKS its assertion as
	 * it lands, until the list is empty. Substring form: every entry is anchored by its
	 * {@code block.gt6.}/{@code item.gt6.} namespace so the gt6.tagprefix./gt6.material.
	 * small-unit faces can never false-positive.
	 */
	private static final List<String> COMPOSED_DOMAIN_KEYS = List.of(
		// B1 — p20-i18n-compose-wires: the electric/redstone/laser wire + cable families,
		// the conveyor/robot-arm tier covers
		"block.gt6.wire_",
		"block.gt6.cable_",
		"item.gt6.cover_conveyor_",
		"item.gt6.cover_robot_arm_",
		// B2 — p20-i18n-compose-rows: stone variants + the kinetics / boiler / burning-box /
		// large-boiler / wall / transmitter / machine-tier / attachment rows
		"block.gt6.axle_",
		"block.gt6.steam_engine",
		"block.gt6.steam_boiler_tank_",
		"block.gt6.diesel_engine",
		"block.gt6.burning_box",
		"block.gt6.dense_wall_",
		"block.gt6.large_boiler_",
		"block.gt6.heat_transmitter",
		"block.gt6.dryer_",
		"block.gt6.distillery_",
		"block.gt6.shredder_t",
		"block.gt6.crusher_t",
		"block.gt6.lathe_t",
		"block.gt6.tap_",
		"block.gt6.funnel_");

	/** Stone variants: block.gt6.<stone_snake>.<variant_snake> (17 stones x 16 variants, B2). */
	private static final Pattern COMPOSED_STONE_KEY = Pattern.compile("^block\\.gt6\\.[a-z0-9_]+\\.[a-z0-9_]+$");

	private static Map<String, String> enEntries;
	private static Map<String, String> zhEntries;

	@BeforeAll
	public static void boot() {
		// the walks load Item subclasses (snakeCase) and the full OP/material registries —
		// vanilla bootstrap first (GT6EnUsJeiInfoTest.boot shape; offline-expected throwables)
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// offline-expected
		}
		GTMaterialItems.initMaterials();
	}

	/** Records every add() the provider makes (duplicate key = hard failure, as in the jei test). */
	private static Map<String, String> collect(boolean aZh) {
		Map<String, String> tEntries = new HashMap<>();
		PackOutput tOutput = new PackOutput(Path.of("build", "tmp", aZh ? "gt6zhcn-parity-test" : "gt6enus-parity-test"));
		// the recording subclasses live in this package, so the protected addTranslations() is
		// reachable on their exact type (a LanguageProvider-typed variable would not be)
		if (aZh) {
			new GT6ZhCn(tOutput) {
				@Override
				public void add(String aKey, String aValue) {
					recordNew(tEntries, aKey, aValue);
				}
			}.addTranslations();
		} else {
			new GT6EnUs(tOutput) {
				@Override
				public void add(String aKey, String aValue) {
					recordNew(tEntries, aKey, aValue);
				}
			}.addTranslations();
		}
		return tEntries;
	}

	private static void recordNew(Map<String, String> aEntries, String aKey, String aValue) {
		if (aEntries.put(aKey, aValue) != null) {
			throw new IllegalStateException("Duplicate translation key " + aKey);
		}
	}

	private static Map<String, String> en() {
		if (enEntries == null) enEntries = collect(false);
		return enEntries;
	}

	private static Map<String, String> zh() {
		if (zhEntries == null) zhEntries = collect(true);
		return zhEntries;
	}

	@Test
	public void zhKeysAreASubsetOfEnKeys() {
		List<String> tMissing = new ArrayList<>();
		for (String tKey : zh().keySet()) {
			if (!en().containsKey(tKey)) tMissing.add(tKey);
		}
		assertTrue(tMissing.isEmpty(),
			"zh keys must be a subset of en keys (a TSV join or walk drifted off the en face): " + tMissing);
	}

	@Test
	public void composedDomainsStayAbsentFromZh() {
		List<String> tViolations = new ArrayList<>();
		for (String tKey : zh().keySet()) {
			for (String tDomain : COMPOSED_DOMAIN_KEYS) {
				if (tKey.contains(tDomain)) tViolations.add(tKey + " (B-wave domain " + tDomain + ")");
			}
			if (COMPOSED_STONE_KEY.matcher(tKey).matches()) tViolations.add(tKey + " (B-wave stone variant)");
		}
		assertTrue(tViolations.isEmpty(),
			"zh must not carry pre-installed composed-domain strings before the B-wave template"
			+ " conversion lands (each card shrinks its assertion): " + tViolations);
	}

	@Test
	public void zhCoverageMeetsTheRatchetFloor() {
		int tSize = zh().size();
		assertTrue(tSize >= ZH_KEY_FLOOR,
			"zh key count " + tSize + " fell below the ZH_KEY_FLOOR ratchet " + ZH_KEY_FLOOR
			+ " — the floor only ever goes UP: raise it via an explicit PR constant bump");
	}

	@Test
	public void allZhValuesAreNonBlankTemplatesHaveTheirSlot() {
		zh().forEach((tKey, tValue) -> {
			assertFalse(tValue == null || tValue.isBlank(), "blank zh value for " + tKey);
			if (tKey.startsWith("gt6.tagprefix.")) {
				assertTrue(tValue.contains("%s"),
					"zh prefix template " + tKey + " lost its %s material slot (the runtime fills it)");
			}
		});
	}

	@Test
	public void noHandRowIsOrphaned() {
		// a typo'd hand-row key (or a key whose walk was removed) would otherwise be silently
		// skipped — surface it. review-status direct rows may legitimately stay unemitted.
		List<String> tOrphans = new ArrayList<>();
		for (Map.Entry<String, GT6ZhCn.RefRow> tRow : GT6ZhCn.loadReference().get("direct").entrySet()) {
			if ("hand".equals(tRow.getValue().status()) && !zh().containsKey(tRow.getKey())) {
				tOrphans.add(tRow.getKey());
			}
		}
		assertTrue(tOrphans.isEmpty(), "hand rows never emitted by the zh walks: " + tOrphans);
	}
}
