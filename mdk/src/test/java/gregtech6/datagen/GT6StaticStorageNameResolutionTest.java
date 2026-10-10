/**
 * Offline name-resolution ratchet for the FULL static-storage family (task
 * shelf-crate-i18n-runtime-probe): every one of the 148 {@link GT6StaticStorages.ROWS}
 * resolves its display name — BOTH locales (en_us, zh_cn) — against the COMMITTED
 * generated lang files through the vanilla {@code Language} path
 * ({@code Language.loadFromJson} + {@code Language.inject}, the exact faces the client
 * LanguageManager rides), asserting the resolved text is the EXPECTED string and never
 * a raw key. The item-class seam is pinned on its SOURCE body (the Forge Item
 * constructor is as offline-unconstructible as the block one — the item name IS the
 * block name in production via the GTComposedNameItem delegation, and the vanilla
 * BlockItem's lack of a getName(ItemStack) override is the reported bug's engine).
 *
 * <p><b>The bug this pins shut.</b> The family registered plain {@code BlockItem}s while
 * the metal shelf/crate rows retired their {@code block.gt6.<path>} keys into the compose
 * templates ({@code gt6.row.metal_{bookshelf,bottlecrate}.display} + the shared
 * {@code gt6.row.mat.*} words). Vanilla BlockItem delegates ONLY the descriptionId
 * (BlockItem.java:186-189) — so a stack of {@code bookshelf_metal_antimony} hovered the
 * raw key {@code block.gt6.bookshelf_metal_antimony} on the client: the
 * "书架和瓶箱，大多数都是没有对应语言键的" report (120 metal items raw, the 20 wooden ones
 * fine on their atomic keys). The fix rides {@link GTComposedNameItem} (the GT6Hoppers:392
 * / GT6MassStorageItem form) at the {@link GT6StaticStorages#itemOf} seam, plus the
 * KIND-scoped compose guard on {@link GT6StaticStorages#displayNameOf} (the former
 * material-scoped block guard resolved a bronze LOCKER through the bottlecrate template
 * on the block face — Jade's name row — while its atomic key sat unused there).
 *
 * <p><b>The ratchet posture.</b> A new row lands in ROWS and is covered automatically; a
 * reverted compose guard turns red on the face-value equality (a locker would drift to
 * the bottlecrate template), a reverted item seam turns red on the {@code instanceof}
 * pin, a retired key turns red on the resolution leak (Language.getOrDefault falls back
 * to the raw key, which contains "gt6.").
 *
 * <p><b>The offline ceiling (the honest link chain).</b> Constructing a
 * {@code GT6StorageBlock} or any {@code Item} offline is impossible — the Forge Block
 * AND Item constructors register intrusive holders into the ALREADY-FROZEN registry
 * ({@code NamespacedWrapper.validateWrite:271} on both, the shelf-crate-2px lesson
 * plus this card's Item.java:61 twin), and the test fork boot completes the freeze
 * before any test class loads. So the row faces are exercised through
 * {@link GT6StaticStorages#displayNameOf} — the single compose point the block
 * {@code getName()} delegates to (a source-linked one-liner) — and the item-class seam
 * through the source-body pin above.
 *
 * <p><b>Offline posture.</b> The fork boot ({@code GT6TestJvmBootListener}) covers the
 * vanilla bootstrap; the Language injection is JVM-global, so the prior instance is
 * saved and restored in {@code @AfterAll} (the neighboring suites keep the default
 * key-echo language).
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.StringDecomposer;

import gregtech6.block.GTComposedNameItem;
import gregtech6.registry.GT6StaticStorages;
import gregtech6.registry.GT6StaticStorages.Kind;
import gregtech6.registry.GT6StaticStorages.StaticRow;

public class GT6StaticStorageNameResolutionTest {

	private static final String BOOKSHELF_TEMPLATE_KEY = "gt6.row.metal_bookshelf.display";
	private static final String BOTTLECRATE_TEMPLATE_KEY = "gt6.row.metal_bottlecrate.display";

	private static Language priorLanguage;
	private static final Map<String, String> EN = new HashMap<>();
	private static final Map<String, String> ZH = new HashMap<>();

	/** Location of the mdk project root (the GT6StoneSlabCraftingJsonTest.mdkRoot shape). */
	private static Path mdkRoot() {
		for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
			if (Files.isRegularFile(p.resolve("tools").resolve("gen_textures.py"))) return p;
		}
		throw new AssertionError("mdk root (tools/gen_textures.py) not found upward from "
				+ Path.of("").toAbsolutePath());
	}

	/** The committed generated face — the files the jar actually ships (Language.loadFromJson, the client parser). */
	private static Map<String, String> loadLocale(String aLocale) throws Exception {
		Path tFile = mdkRoot().resolve("src").resolve("generated").resolve("resources")
				.resolve("assets").resolve("gt6").resolve("lang").resolve(aLocale + ".json");
		Map<String, String> rMap = new HashMap<>();
		try (InputStream tIn = Files.newInputStream(tFile)) {
			Language.loadFromJson(tIn, rMap::put);
		}
		assertFalse(rMap.isEmpty(), "the committed " + aLocale + " lang face is empty: " + tFile);
		return rMap;
	}

	/** The map-backed Language (the ClientLanguage shape — getOrDefault falls back to the KEY, the leak detector). */
	private static Language languageOf(Map<String, String> aMap) {
		return new Language() {
			@Override
			public String getOrDefault(String aKey, String aFallback) {
				return aMap.getOrDefault(aKey, aFallback);
			}

			@Override
			public boolean has(String aKey) {
				return aMap.containsKey(aKey);
			}

			@Override
			public boolean isDefaultRightToLeft() {
				return false;
			}

			@Override
			public FormattedCharSequence getVisualOrder(FormattedText aText) {
				return aSink -> aText.visit(
						(aStyle, aChar) -> StringDecomposer.iterateFormatted(aChar, aStyle, aSink)
								? Optional.empty() : FormattedText.STOP_ITERATION,
						Style.EMPTY).isPresent();
			}
		};
	}

	@BeforeAll
	public static void boot() throws Exception {
		// vanilla bootstrap first (the parity-test shape; the fork boot already covered it,
		// the bracket is the discipline) — the ROWS walk touches Item subclasses
		SharedConstants.tryDetectVersion();
		try {
			Bootstrap.bootStrap();
		} catch (Throwable ignored) {
			// offline-expected
		}
		EN.putAll(loadLocale("en_us"));
		ZH.putAll(loadLocale("zh_cn"));
		priorLanguage = Language.getInstance();
	}

	@AfterAll
	public static void restoreLanguage() {
		Language.inject(priorLanguage);
	}

	/**
	 * The expected display string, derived from the COMPOSE RULE (not from the code): a
	 * metal shelf/crate row resolves its kind template over the row's mat word; every other
	 * row resolves its atomic {@code block.gt6.<path>} key. The live face is asserted
	 * EQUAL to this — a guard revert (a locker leaking the bottlecrate template), a wrong
	 * template ternary or a retired key all drift the live face off it.
	 */
	private static String expectedNameOf(StaticRow aRow, Map<String, String> aMap, String aSide) {
		boolean tComposed = aRow.material() != null
				&& (aRow.kind() == Kind.BOOKSHELF || aRow.kind() == Kind.BOTTLECRATE);
		if (!tComposed) {
			String tKey = "block.gt6." + aRow.path();
			String tValue = aMap.get(tKey);
			assertTrue(tValue != null, aSide + " face is missing the atomic key " + tKey);
			return tValue;
		}
		String tTemplateKey = aRow.kind() == Kind.BOOKSHELF ? BOOKSHELF_TEMPLATE_KEY : BOTTLECRATE_TEMPLATE_KEY;
		String tTemplate = aMap.get(tTemplateKey);
		assertTrue(tTemplate != null, aSide + " face is missing the template " + tTemplateKey);
		assertTrue(tTemplate.indexOf("%s") >= 0 && tTemplate.indexOf("%s") == tTemplate.lastIndexOf("%s"),
				"the template " + tTemplateKey + " drifted off the single-slot form: " + tTemplate);
		String tMatWord = aMap.get("gt6.row.mat." + aRow.material().slug());
		assertTrue(tMatWord != null, aSide + " face is missing the mat word gt6.row.mat." + aRow.material().slug());
		return tTemplate.replace("%s", tMatWord);
	}

	private static void assertRowFace(StaticRow aRow, Map<String, String> aMap, String aSide) {
		String tExpected = expectedNameOf(aRow, aMap, aSide);
		// the name face both the block (Jade's name row) and the item (via the
		// GTComposedNameItem delegation) resolve through
		String tFace = GT6StaticStorages.displayNameOf(aRow).getString();
		assertEquals(tExpected, tFace, aSide + " name face of " + aRow.path());
		assertFalse(tFace.contains("gt6."),
				"a raw key leaked into the " + aSide + " name face of " + aRow.path()
						+ " (a missing lang key resolves the key itself): " + tFace);
	}

	@Test
	public void everyRowNameResolvesOnBothLocales() {
		Language.inject(languageOf(EN));
		for (StaticRow tRow : GT6StaticStorages.ROWS) assertRowFace(tRow, EN, "en");
		Language.inject(languageOf(ZH));
		for (StaticRow tRow : GT6StaticStorages.ROWS) assertRowFace(tRow, ZH, "zh");
	}

	/**
	 * The item seam pin (SOURCE arm — the GT6StorageToolCharParityTest
	 * decoded-arm-into-the-assertion form): {@link GT6StaticStorages#itemOf} must
	 * construct the composed-name item class. The offline runtime pin is unreachable —
	 * the Forge Item constructor ALSO registers an intrusive holder into the frozen
	 * registry (Item.java:61 -> NamespacedWrapper.validateWrite, the same wall the block
	 * ctor hits), so both the correct and the reverted seam throw identically at
	 * construction; the seam's CLASS choice is therefore pinned on the source body. A
	 * revert to a plain BlockItem (which resolves the raw descriptionId instead of the
	 * block name — the reported bug's engine) turns this red. The DELEGATION behavior of
	 * GTComposedNameItem (stack name == block.getName) needs no pin here: it is the
	 * house-proven carrier class the wire/pipe/barrel/mass-storage families already ship.
	 */
	@Test
	public void itemSeamShipsTheComposedNameClass() throws Exception {
		Path tSource = mdkRoot().resolve("src").resolve("main").resolve("java")
				.resolve("gregtech6").resolve("registry").resolve("GT6StaticStorages.java");
		String tText = Files.readString(tSource);
		int tSeam = tText.indexOf("public static Item itemOf(");
		assertTrue(tSeam >= 0, "the item seam method vanished from GT6StaticStorages"
				+ " (the family's one item-class choice point): " + tSource);
		int tBodyEnd = tText.indexOf("\n\tpublic static", tSeam + 1);
		String tBody = tText.substring(tSeam, tBodyEnd < 0 ? tText.length() : tBodyEnd);
		assertTrue(tBody.contains("new GTComposedNameItem"),
				"the item seam no longer ships the composed-name item class (a plain"
						+ " BlockItem resolves the raw descriptionId — the reported bug): " + tBody);
		assertFalse(tBody.contains("BlockItem("),
				"the item seam constructs a raw BlockItem (the raw-key engine): " + tBody);
	}

	/**
	 * The family census (task material-mc-b-storage-mass-shelf walk, the shelf/crate
	 * i18n-runtime-probe pin): 148 rows — the 8 bronze/steel locker/drawer/safe rows, the
	 * 20 wooden shelf/crate rows, the 120 metal shelf/crate rows. A walk-empty ROWS or a
	 * silently dropped ladder fails loud here instead of passing the resolution sweep
	 * vacuously.
	 */
	@Test
	public void rowCensusPins() {
		assertEquals(148, GT6StaticStorages.ROWS.size(), "the static-storage row census");
		Map<Kind, Integer> tByKind = new EnumMap<>(Kind.class);
		int tComposed = 0;
		int tAtomic = 0;
		for (StaticRow tRow : GT6StaticStorages.ROWS) {
			tByKind.merge(tRow.kind(), 1, Integer::sum);
			boolean tComposedFace = tRow.material() != null
					&& (tRow.kind() == Kind.BOOKSHELF || tRow.kind() == Kind.BOTTLECRATE);
			if (tComposedFace) tComposed++; else tAtomic++;
		}
		assertEquals(2, tByKind.get(Kind.LOCKER), "the locker pair");
		assertEquals(2, tByKind.get(Kind.DRAWER), "the drawer pair");
		assertEquals(2, tByKind.get(Kind.SAFE_MECHANICAL), "the mechanical safe pair");
		assertEquals(2, tByKind.get(Kind.SAFE_KEYLOCKED), "the keylocked safe pair");
		assertEquals(70, tByKind.get(Kind.BOOKSHELF), "the bookshelf ladder (10 wood + 60 metal)");
		assertEquals(70, tByKind.get(Kind.BOTTLECRATE), "the bottlecrate ladder (10 wood + 60 metal)");
		assertEquals(120, tComposed, "the metal shelf/crate composed-name rows");
		assertEquals(28, tAtomic, "the atomic-key rows (8 bronze/steel containers + 20 wooden shelf/crate)");
	}

	/**
	 * The template-key shape on BOTH locales (the resolution sweep would catch a missing
	 * key via the leak, but the slot-count pin catches a template whose argument slot
	 * drifted — "Bookshelf (%s)" losing its slot renders "12×" as "×", the parity-test
	 * precedent). The 60 mat words ride the per-row sweep.
	 */
	@Test
	public void composeTemplatesCarryTheirSlotsOnBothLocales() {
		for (Map.Entry<String, Map<String, String>> tSide : Map.of("en", EN, "zh", ZH).entrySet()) {
			for (String tKey : List.of(BOOKSHELF_TEMPLATE_KEY, BOTTLECRATE_TEMPLATE_KEY)) {
				String tValue = tSide.getValue().get(tKey);
				assertTrue(tValue != null, tSide.getKey() + " face is missing " + tKey);
				assertEquals(1, tValue.split("%s", -1).length - 1, tSide.getKey() + " slot count of " + tKey);
			}
			// the mat words: one per slug the metal ladders walk
			for (StaticRow tRow : GT6StaticStorages.ROWS) {
				if (tRow.material() == null) continue;
				assertTrue(tSide.getValue().containsKey("gt6.row.mat." + tRow.material().slug()),
						tSide.getKey() + " face is missing gt6.row.mat." + tRow.material().slug());
			}
		}
	}
}
