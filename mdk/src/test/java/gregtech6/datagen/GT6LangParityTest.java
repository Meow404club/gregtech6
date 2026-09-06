/**
 * Offline guard test for task p20-i18n-zhcn-provider (ADR 2026-09-06-p20-i18n-zhcn-pipeline
 * §1.3): the zh_cn key set is structurally pinned to the en_us key set, the not-yet-composed
 * domains are pinned ABSENT from zh until their B-wave cards land, and the zh coverage floor
 * is a ratchet. Same posture as GT6EnUsJeiInfoTest:52-64 — a recording LanguageProvider
 * subclass (add is public and non-final) captures every entry addTranslations() would emit,
 * in a bare JVM, no datagen run needed (the generated files themselves are gated by runData
 * + the second-run written:0 check).
 *
 * <p>Task p20-i18n-compose-wires FLIPPED the B1 half of the guard: the wire + conveyor/
 * robot-arm domains are now asserted COMPOSED on BOTH sides (no pre-installed full-string
 * key survives on en or zh outside the three atomic exemptions, and the template keys exist
 * with their argument slots in both locales) — the A-wave "absent from zh" form is gone for
 * that domain, the B2 assertions remain.
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.data.PackOutput;
import net.minecraft.server.Bootstrap;

import gregtech6.item.MaterialPrefixItem;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTWireSpecs;

public class GT6LangParityTest {

	/**
	 * The zh_cn coverage floor — a RATCHET: it may only be raised (by an explicit PR changing
	 * this constant, with the new measured key count in the message), never lowered. Initial
	 * value per the task card: material ~1770 + tabs ~110 + tagprefix ~102 + misc ~17.
	 */
	private static final int ZH_KEY_FLOOR = 1800;

	/**
	 * A-wave negative assertions (ADR §1.3): the COMPOSED domains stay absent from zh — those
	 * en keys are pre-installed full strings that the B-wave cards replace with template keys;
	 * every card SHRINKS its assertion as it lands, until the list is empty. B1
	 * (p20-i18n-compose-wires) has landed: its four entries are gone from this list and now
	 * live under {@link #WIRES_COMPOSED_PREFIXES} in the FLIPPED both-sides form. Substring
	 * form: every entry is anchored by its {@code block.gt6.}/{@code item.gt6.} namespace so
	 * the gt6.tagprefix./gt6.material. small-unit faces can never false-positive.
	 */
	private static final List<String> COMPOSED_DOMAIN_KEYS = List.of(
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

	/**
	 * The FLIPPED B1 guard (task p20-i18n-compose-wires): these prefixes must match NO
	 * pre-installed full-string key on EITHER locale side — the names compose at runtime
	 * (GTWireBlock.displayNameOf / the GT6Covers cover templates) from the gt6.wire.* and
	 * gt6.cover.* template keys.
	 */
	private static final List<String> WIRES_COMPOSED_PREFIXES = List.of(
		"block.gt6.wire_",
		"block.gt6.cable_",
		"item.gt6.cover_conveyor_",
		"item.gt6.cover_robot_arm_");

	/**
	 * The B1 atomic exemptions — the material-less forms that keep whole-string keys on both
	 * faces: the two p7 legacy electric blocks (no row identity to compose from) and the
	 * laser family (material-less, Loader:1815 verbatim).
	 */
	private static final Set<String> WIRES_ATOMIC_KEYS = Set.of(
		"block.gt6.wire_electric_1x",
		"block.gt6.wire_electric_2x",
		"block.gt6.wire_laser");

	/** The B1 template keys and their argument-slot counts (display=3: size/material/form; plain=2; the cover templates=1; the form units are slot-less NOUNS the templates consume). */
	private static final Map<String, Integer> WIRES_TEMPLATE_SLOTS = Map.of(
		"gt6.wire.display", 3,
		"gt6.wire.display.plain", 2,
		"gt6.wire.form.wire", 0,
		"gt6.wire.form.cable", 0,
		"gt6.wire.form.wirelamp", 0,
		"gt6.cover.conveyor.display", 1,
		"gt6.cover.robot_arm.display", 1);

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

	/**
	 * The FLIPPED B1 guard (task p20-i18n-compose-wires): the wire + conveyor/robot-arm
	 * domains carry NO pre-installed full-string key on EITHER side — the en strings retired
	 * with the B1 shrink, zh never had them — outside the three atomic exemptions (the two
	 * material-less legacy blocks and the laser family, whose descriptionId keys are the
	 * name face on both locales).
	 */
	@Test
	public void wireConveyorDomainsAreComposedOnBothSides() {
		List<String> tViolations = new ArrayList<>();
		Map<String, Map<String, String>> tSides = Map.of("en_us", en(), "zh_cn", zh());
		tSides.forEach((tLocale, tEntries) -> {
			for (String tKey : tEntries.keySet()) {
				for (String tPrefix : WIRES_COMPOSED_PREFIXES) {
					if (tKey.startsWith(tPrefix) && !WIRES_ATOMIC_KEYS.contains(tKey)) {
						tViolations.add(tLocale + ":" + tKey + " (composed domain " + tPrefix + ")");
					}
				}
			}
		});
		assertTrue(tViolations.isEmpty(),
			"the wire/conveyor domains must compose at runtime: no pre-installed full-string key"
			+ " may survive on either side (atomic exemptions only): " + tViolations);
	}

	/**
	 * The B1 template face: every wire/cover template key exists in BOTH locales with its
	 * exact argument-slot count — the parity subset alone would tolerate a zh template whose
	 * slots drifted from the en compose contract (e.g. a lost size slot would render
	 * "12×锡线缆" as "×锡线缆").
	 */
	@Test
	public void wireTemplatesExistWithTheirSlotsOnBothSides() {
		Map<String, Map<String, String>> tSides = Map.of("en_us", en(), "zh_cn", zh());
		tSides.forEach((tLocale, tEntries) -> WIRES_TEMPLATE_SLOTS.forEach((tKey, tCount) -> {
			String tValue = tEntries.get(tKey);
			assertTrue(tValue != null, tLocale + " is missing the B1 template key " + tKey);
			int tSeen = tValue.split("%s", -1).length - 1;
			assertEquals(tCount, tSeen, tLocale + " template " + tKey + " = \"" + tValue + "\" slot count");
		}));
	}

	/**
	 * The FULL-EXPANSION pin (the review round 1 finding: the slot structure alone let the
	 * en template ship as {@code "%sx %s%s"} with bare-noun form units — every composed name
	 * rendered "1x TinWire" and no test noticed). The template + small-unit faces recorded
	 * above are substituted slot-by-slot (the reviewer's programmatic comparison posture) and
	 * pinned EQUAL to the upstream row strings word for word — the space between the material
	 * and the form RIDES THE EN TEMPLATE ({@code "%sx %s %s"}); zh stays the no-space CJK
	 * shape. The B2 rows domain must repeat this pin shape.
	 */
	@Test
	public void wireTemplatesExpandToTheUpstreamStrings() {
		// electric: "%sx %s %s" over (size, gt6.material.<snake>, form unit)
		assertEquals("1x Tin Wire", expandWire(1, "tin", "gt6.wire.form.wire"));
		assertEquals("12x Tin Cable", expandWire(12, "tin", "gt6.wire.form.cable"));
		assertEquals("16x Tungsten Wire", expandWire(16, "tungsten", "gt6.wire.form.wire"));
		// redstone: the size-less plain template, bare Lumium wire = the Wirelamp unit
		assertEquals("Red Alloy Wire", expandWire(0, "red_alloy", "gt6.wire.form.wire"));
		assertEquals("Lumium Wirelamp", expandWire(0, "lumium", "gt6.wire.form.wirelamp"));
		// the covers: the template carries the parens, the tier rides a literal
		assertEquals("Compact Electric Conveyor (LV)", substitute(en().get("gt6.cover.conveyor.display"), "LV"));
		assertEquals("Compact Robot Arm (PUV1)", substitute(en().get("gt6.cover.robot_arm.display"), "PUV1"));
	}

	/**
	 * The compose material-slot presence pin (review R2 finding): the compose references
	 * {@code gt6.material.<snake>} UNCONDITIONALLY, but tier materials are created with
	 * {@code mID -1} (Superconductor, MT.java:986 {@code tier()}) and never reach the
	 * registration-face material walk (GT6EnUs.addMaterialNames {@code mID < 0} continue) —
	 * the 16 superconductor variants composed the RAW key while every slot-structure and
	 * expansion pin stayed green, because none checked the material KEY FACE. This walks the
	 * whole 626-variant compose domain (620 electric + 6 redstone) against the en recording
	 * face, so a keyface/compose-domain mismatch of this class is structurally red — the B2
	 * rows domain reuses the shape.
	 */
	@Test
	public void everyComposedVariantMaterialKeyIsOnTheEnFace() {
		List<String> tMissing = new ArrayList<>();
		int tChecked = 0;
		for (GTWireSpecs.Variant tVariant : GTWireSpecs.variants()) {
			tChecked++;
			String tKey = "gt6.material." + MaterialPrefixItem.snakeCase(tVariant.row().material().get().mNameInternal);
			if (!en().containsKey(tKey)) tMissing.add(GTWireSpecs.registryName(tVariant) + " -> " + tKey);
		}
		for (GTWireSpecs.Variant tVariant : GTWireSpecs.redstoneVariants()) {
			tChecked++;
			String tKey = "gt6.material." + MaterialPrefixItem.snakeCase(tVariant.row().material().get().mNameInternal);
			if (!en().containsKey(tKey)) tMissing.add(GTWireSpecs.registryName(tVariant) + " -> " + tKey);
		}
		assertEquals(626, tChecked, "the compose domain census (the atomic laser/legacy forms are NOT in scope)");
		assertTrue(tMissing.isEmpty(), "every composed variant's material key must exist on the en face"
			+ " (a missing face renders the RAW key at runtime): " + tMissing);
	}

	/** The wire-name expansion over the en face: aSize 0 = the size-less plain template. */
	private static String expandWire(int aSize, String aMaterialSnake, String aFormKey) {
		String tTemplate = en().get(aSize > 0 ? "gt6.wire.display" : "gt6.wire.display.plain");
		String tMaterial = en().get("gt6.material." + aMaterialSnake);
		assertTrue(tTemplate != null && tMaterial != null, "the expansion face must be recorded");
		return substitute(tTemplate, aSize > 0 ? new String[] {String.valueOf(aSize), tMaterial, en().get(aFormKey)}
			: new String[] {tMaterial, en().get(aFormKey)});
	}

	/** Substitutes the {@code %s} slots in order — the game-side rendering of a fully-translated template face. */
	private static String substitute(String aTemplate, String... aArgs) {
		StringBuilder rBuilder = new StringBuilder();
		int tArg = 0;
		for (int i = 0; i < aTemplate.length(); i++) {
			if (aTemplate.charAt(i) == '%' && i + 1 < aTemplate.length() && aTemplate.charAt(i + 1) == 's') {
				assertTrue(tArg < aArgs.length, "template " + aTemplate + " consumed more slots than provided");
				rBuilder.append(aArgs[tArg++]);
				i++;
			} else {
				rBuilder.append(aTemplate.charAt(i));
			}
		}
		assertTrue(tArg == aArgs.length, "template " + aTemplate + " left " + (aArgs.length - tArg) + " slots unfilled");
		return rBuilder.toString();
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
