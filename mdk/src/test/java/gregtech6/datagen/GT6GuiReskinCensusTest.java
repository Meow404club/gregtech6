/**
 * Pin h (task gui-reskin-amazawa) — the machine GUI reskin census. Every PNG under
 * {@code assets/gt6/textures/gui/machines/} was reskinned from the amazawa resource
 * pack (Modrinth "tfc-amazawa-light-gui" 1.0.5g, Apache-2.0, author 天沢香, chat
 * authorization shipped at docs/licenses/amazawa-gui-authorization.png — the
 * attribution trio lives in the assets/README.md reskin section). The MANIFEST table
 * below is the single source of truth for that wave: our lowercase target stem
 * (ResourceLocation path legality, ResourceLocation.java:213-232), the PascalCase
 * amazawa source file (matched case-insensitively; anvilbend folds amazawa
 * AnvilBendingBig.png per the assets/README.md Small/Big fold row), the reskinned
 * bytes' sha256, and the canvas size, which equals the replaced upstream size
     * per-file (256x256 on all 73, verified at match time — the sizes pin keeps any
     * future reskin honest the same way). Since 34-viewer-gui-bg the folder also
     * carries the viewer's NEI backdrop plate (nei.png) and the bedrockorelist.png
     * display map — both amazawa redraws, rows here like everything else.
 *
 * <p>Like pin g (GT6TextureCensusTest) this is pure read-only JUnit, no vanilla
 * bootstrap, touching only committed trees. The (176,0) 16x16 spot pin guards the
 * vanilla-Screen-leg progress-arrow overlay, which blits that UV window from the same
 * background PNG (upstream ContainerClientBasicMachine.java:48-65 lineage) — a reskin
 * that drops the arrow strip would silently break every machine GUI's progress bar.</p>
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

class GT6GuiReskinCensusTest {

    /** The reskinned machine GUI folder, under the mdk root. */
    private static final Path GUI_MACHINES =
        Path.of("src", "main", "resources", "assets", "gt6", "textures", "gui", "machines");

    /**
     * The authorization screenshot (attribution trio, decisions.r8-gui-visual-source) —
     * repo-root docs/, a sibling of the mdk root.
     */
    private static final Path LICENSE_SHOT = Path.of("docs", "licenses", "amazawa-gui-authorization.png");

    /** ResourceLocation path legality, lowercase-only form (vanilla 1.20.1 [a-z0-9/._-]). */
    private static final Pattern LEGAL_STEM = Pattern.compile("[a-z0-9_-]+");

    /**
     * One row per surviving PNG: {target stem, amazawa source file, sha256 of the
     * reskinned bytes, width, height}. The purge-era three: the ACT crop-source pair
     * (act-dual-gui) and the nei plate (34-viewer-gui-bg; bedrockorelist retired with
     * its mGUIPath value).
     */
    private static final String[][] MANIFEST = {
        // the three SURVIVORS of machine-gui-composed-purge (the 2026-10-10 user ruling
        // "全部改成拼接，把完整图全部清了"): the two ACT sheets are the parts-pipeline crop
        // sources (crop_gui_parts.py:53-55) and nei.png is the composed viewer plate
        // (GT6RecipeMapViewerMeta PLATE — the 永保留 anchor). The other 72 reskinned
        // machine sheets retired with the mGUIPath values; their historical rows live in
        // git history (the sha grounding for the five part-crop sources rides the
        // GT6TextureCensusTest upstream-source exemption table).
        {"advancedcraftingtable", "advancedcraftingtable.png", "af774ea0631d00b61242ea8d18deaba148c9fc581fd90201c615091381729a5a", "256", "256"},
        {"advancedcraftingtablecharging", "advancedcraftingtablecharging.png", "cd57524d0fcb72efd3519e4a0e9a62fe8e67b7422aff61194985da9a500f198a", "256", "256"},
        {"nei", "NEI.png", "93fda523222f02a0dbc2b405e2226a9454cbaca0e776c65b861352f8d965b4ac", "256", "256"},
    };

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

    private static Path guiMachinesDir() {
        return mdkRoot().resolve(GUI_MACHINES);
    }

    private static String sha256(Path file) throws IOException {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(Files.readAllBytes(file)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * The wave pin: the on-disk gui/machines set is EXACTLY the manifest (bijective — a
     * file added without a row or deleted without one fails here, the pin never passes
     * vacuously), and every file matches its row: lowercase-legal stem, ImageIO canvas
     * size == the replaced upstream size, sha256 == the manifest digest.
     */
    @Test
    void everyReskinnedMachineGuiPngMatchesManifest() throws IOException {
        assertTrue(Files.isRegularFile(mdkRoot().getParent().resolve(LICENSE_SHOT)),
            "the amazawa authorization screenshot is missing — the attribution trio must ship");
        Set<String> onDisk = new HashSet<>();
        try (var walk = Files.list(guiMachinesDir())) {
            walk.filter(p -> p.getFileName().toString().endsWith(".png"))
                .forEach(p -> onDisk.add(p.getFileName().toString().replaceAll("\\.png$", "")));
        }
        Set<String> pinned = new HashSet<>();
        List<String> violations = new ArrayList<>();
        for (String[] row : MANIFEST) {
            String stem = row[0], source = row[1], sha = row[2];
            int w = Integer.parseInt(row[3]), h = Integer.parseInt(row[4]);
            pinned.add(stem);
            if (!LEGAL_STEM.matcher(stem).matches()) {
                violations.add(stem + " — ResourceLocation-illegal stem (from " + source + ")");
                continue;
            }
            Path file = guiMachinesDir().resolve(stem + ".png");
            if (!Files.isRegularFile(file)) {
                violations.add(stem + ".png — missing (reskin row " + source + " not applied)");
                continue;
            }
            BufferedImage img = ImageIO.read(file.toFile());
            if (img.getWidth() != w || img.getHeight() != h) {
                violations.add(stem + ".png — size " + img.getWidth() + "x" + img.getHeight()
                    + " != pinned upstream size " + w + "x" + h);
            }
            String actual = sha256(file);
            if (!actual.equals(sha)) {
                violations.add(stem + ".png — sha256 " + actual + " != manifest " + sha);
            }
        }
        assertEquals(pinned, onDisk,
            "gui/machines is not exactly the reskin manifest — add a row for a new file "
                + "or restore/delete the stray PNG (the composed-purge three)");
        assertTrue(violations.isEmpty(), "reskin manifest violations: " + violations);
        assertEquals(3, MANIFEST.length,
            "the manifest drifted off the composed-purge three — the survivors pin is exact");
    }

    /**
     * The README reskin section must mirror this table row-for-row (target stem,
     * amazawa source, sha256) — this MANIFEST is the single source of truth and the
     * README section is its human-facing copy; a copy edited without the table (or
     * vice versa) fails here instead of quietly forking the ledger.
     */
    @Test
    void readmeReskinSectionMirrorsManifest() throws IOException {
        Path readme = mdkRoot().resolve(Path.of("src", "main", "resources", "assets", "README.md"));
        String section = Files.readString(readme)
            .split("## task gui-reskin-amazawa", 2)[1];
        Set<String> readmeRows = new LinkedHashSet<>();
        java.util.regex.Matcher m = java.util.regex.Pattern
            // the provenance word is part of the copy, not the ledger key (the nanofab
            // row rides the upstream borrow, not the amazawa pack — review-seat hotfix)
            .compile("`gui/machines/([a-z0-9_-]+)\\.png` ← (?:amazawa|upstream) `([^`]+)` sha256 `([0-9a-f]{64})`")
            .matcher(section);
        while (m.find()) {
            readmeRows.add(m.group(1) + "|" + m.group(2) + "|" + m.group(3));
        }
        Set<String> tableRows = new LinkedHashSet<>();
        for (String[] row : MANIFEST) {
            tableRows.add(row[0] + "|" + row[1] + "|" + row[2]);
        }
        assertEquals(tableRows, readmeRows,
            "the README reskin manifest drifted from the pin-h table — the table is the "
                + "single source of truth, fix the copy that is wrong (diff: "
                + simpleDiff(tableRows, readmeRows) + ")");
    }

    private static String simpleDiff(Set<String> expected, Set<String> actual) {
        Set<String> missing = new HashSet<>(expected);
        missing.removeAll(actual);
        Set<String> extra = new HashSet<>(actual);
        extra.removeAll(expected);
        return "readme-missing" + Arrays.toString(missing.toArray())
            + " readme-extra" + Arrays.toString(extra.toArray());
    }


}
