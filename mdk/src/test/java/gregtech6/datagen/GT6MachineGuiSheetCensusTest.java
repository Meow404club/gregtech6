/**
 * Pure-JUnit census for task machine-gui-composed-purge — the
 * {@code assets/gt6/textures/gui/machines/} sheet set is EXACTLY the three
 * composed-purge survivors, and every survivor still carries its live reference.
 *
 * <p>The ruling (2026-10-10, the user override recorded in the task card and
 * assets/README.md): "全部改成拼接，把完整图全部清了" — this SUPERSEDES the prior
 * zero-deletable census (task machine-gui-sheet-purge, whose 78-name classification
 * table is the input this ruling consumed): the six live GUIs compose from the
 * gui/parts crops (GTBasicMachineScreen renderBg + the MUI theme base — the oven rides
 * the family MUI panel since oven-gui-arrow-align),
 * the {@code RecipeMap.mGUIPath} column and its 67 data-grounded sheets are retired
 * wholesale, and the on-disk set shrinks 78 → 3:</p>
 * <ul>
 *   <li><b>nei</b> — the composed viewer plate (GT6RecipeMapViewerMeta PLATE), the
 *       永保留 anchor;</li>
 *   <li><b>advancedcraftingtable + advancedcraftingtablecharging</b> — the ACT pair,
 *       the crop SOURCES the parts pipeline derives from (mdk/tools/crop_gui_parts.py
 *       :53-55 and the README parts table; their derivation-provenance digests ride the
 *       GT6TextureCensusTest upstream-source exemption table).</li>
 * </ul>
 *
 * <p>The ratchet: the on-disk set must equal this three-name whitelist exactly (a sheet
 * added without a reference fails; a survivor deleted without shrinking the whitelist
 * fails), and each survivor carries its reference needle in its live consumer —
 * {@code gui/machines/nei} in src/main/java, the ACT pair in the crop script. A red here
 * forces the conscious consumer + whitelist + README-manifest move instead of a silent
 * drift. Read-only, no datagen product (ADR-P20 §1.4).</p>
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class GT6MachineGuiSheetCensusTest {

	/** The audited sheets dir (the reskin census' home). */
	private static final Path SHEETS_DIR = Path.of("src", "main", "resources", "assets", "gt6", "textures", "gui", "machines");

	/**
	 * The composed-purge survivors: the viewer plate (the needle face is src/main/java)
	 * and the two ACT crop sources (the needle face is the crop script).
	 */
	private static final Set<String> PLATE = Set.of("nei");
	private static final Set<String> PARTS_CROP_SOURCES = Set.of(
			"advancedcraftingtable", "advancedcraftingtablecharging");

	/** Location of the mdk project root, walking up from the (leg-dependent) test working dir. */
	private static Path mdkRoot() {
		for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
			if (Files.isRegularFile(p.resolve("tools").resolve("gen_textures.py"))) {
				return p;
			}
		}
		throw new AssertionError("mdk root (tools/gen_textures.py) not found upward from "
				+ Path.of("").toAbsolutePath());
	}

	private static Set<String> onDiskStems() throws IOException {
		Set<String> onDisk = new TreeSet<>();
		try (Stream<Path> walk = Files.list(mdkRoot().resolve(SHEETS_DIR))) {
			walk.map(p -> p.getFileName().toString())
					.filter(n -> n.endsWith(".png"))
					.forEach(n -> onDisk.add(n.substring(0, n.length() - ".png".length())));
		}
		return onDisk;
	}

	private static Set<String> whitelist() {
		Set<String> all = new TreeSet<>(PLATE);
		all.addAll(PARTS_CROP_SOURCES);
		return all;
	}

	/**
	 * The set pin: disk == the composed-purge three, bijective (never vacuous), plus the
	 * 永保留 anchor — the nei.png plate (GT6RecipeMapViewerMeta PLATE) may never leave.
	 */
	@Test
	void theOnDiskSheetSetIsExactlyTheComposedPurgeThree() throws IOException {
		Set<String> expected = whitelist();
		assertEquals(3, expected.size(),
				"the whitelist drifted off the composed-purge three — re-run the reference audit "
						+ "before touching this pin");
		assertTrue(expected.containsAll(PLATE) && expected.contains("nei"),
				"the nei.png plate is the 永保留 ruling — it may not leave the whitelist");
		Set<String> onDisk = onDiskStems();
		Set<String> missing = new TreeSet<>(expected);
		missing.removeAll(onDisk);
		Set<String> stray = new TreeSet<>(onDisk);
		stray.removeAll(expected);
		assertTrue(missing.isEmpty() && stray.isEmpty(),
				"gui/machines is not the composed-purge three — a survivor lost ("
						+ missing + ": restore it or shrink this whitelist and the README manifest) "
						+ "or a stray sheet appeared (" + stray + ": give it a live code reference "
						+ "and a class above, or delete it)");
	}

	/**
	 * The reference face: the nei plate still has its {@code gui/machines/nei} needle in
	 * src/main/java (the composed viewer plate), and the ACT pair still has its rows in
	 * the crop script. A red here means a retirement killed the last reference: delete
	 * the sheet, the whitelist row and the README manifest row in the same change.
	 */
	@Test
	void everySurvivorKeepsItsReference() throws IOException {
		String cropScript = Files.readString(mdkRoot().resolve(Path.of("tools", "crop_gui_parts.py")),
				StandardCharsets.UTF_8);
		for (String stem : PARTS_CROP_SOURCES) {
			assertTrue(cropScript.contains("gui/machines/" + stem + ".png"),
					"crop source " + stem + ".png lost its crop_gui_parts.py row — it is no longer "
							+ "a parts-pipeline source; audit and delete the sheet or restore the row");
		}
		List<Path> sources;
		try (Stream<Path> walk = Files.walk(mdkRoot().resolve(Path.of("src", "main", "java")))) {
			sources = walk.filter(p -> p.getFileName().toString().endsWith(".java")).toList();
		}
		assertTrue(sources.size() >= 500, "only " + sources.size()
				+ " main sources walked — the source tree moved, fix the probe before trusting this pin");
		StringBuilder combined = new StringBuilder();
		for (Path p : sources) {
			combined.append(Files.readString(p, StandardCharsets.UTF_8));
		}
		// the delimiter-lookahead needle (the (?=\.png|\"|\|) form the sheet-purge census
		// established) — "nei" alone would false-positive everywhere
		assertTrue(java.util.regex.Pattern.compile("gui/machines/nei(?=\\.png|\"|\\|)").matcher(combined).find(),
				"the nei plate lost its src/main/java reference — it is no longer the composed "
						+ "viewer plate; audit and delete or restore");
	}
}
