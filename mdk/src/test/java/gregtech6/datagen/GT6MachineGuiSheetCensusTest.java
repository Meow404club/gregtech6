/**
 * Pure-JUnit census for task machine-gui-sheet-purge — the
 * {@code assets/gt6/textures/gui/machines/} sheet set is EXACTLY the audited
 * reference union, and every sheet in it still has a live code reference.
 *
 * <p>Background (the user ruling recorded at GT6RecipeMapViewerMeta.java:55-58, the
 * "配方页渲染切拼接 UI + 删除独立机器 GUI 贴图" debt): the viewer band retired by task
 * composed-ui-energy-slot-and-parts was the last VIEWER consumption of the standalone
 * machine GUI sheets, and this card audited the remaining consumers before any deletion.
 * The audit verdict (2026-10-10, full grep of {@code gui/machines/} across main sources,
 * JSON resources and tooling): <b>zero deletable sheets</b> — every one of the 78 PNGs on
 * disk is referenced by at least one of three live consumption classes:</p>
 * <ul>
 *   <li><b>render-alive</b> — a screen or MUI panel blits the PNG today:
 *     {@code cokeoven} (GTBasicMachineScreen.backgroundOf ← TileEntityCokeOven, the
 *     GTCokeOvenBlock open + the GTClientMachineListener screen mount),
 *     {@code dryer} (Dryer rows bind GTBasicMachinesMenus.DRYER_MENU, GTMachines.java:524,
 *     the vanilla arm GTBasicMachineBlock.java:410-413),
 *     {@code canner} (the canner rows bind CANNER_MENU, GTMachines.java:654/:1229/:5535/:5554 —
 *     the menu chain is live; the CANNER_MENU screen constructor mount is a separate gap,
 *     kept on the chain-live face), {@code oven} (GTOvenScreen.BACKGROUND, the hardcoded
 *     :25 constant — also the data path of the ROASTING/MICROWAVE/FURNACE-FUEL maps),
 *     {@code distillationtower} + {@code cryodistillationtower} (GTDistillationTowerMUI
 *     .backgroundOf, the tower BEs' recipes().mGUIPath, GT6Distillation.java:325),
 *     {@code bumbliary} + {@code bumbliaryadvanced} (GT6BumbliaryMUI.buildPanel background,
 *     the BE's GT6MuiMachine tryOpen chain, GT6BumbliaryBlockEntity.java:477/:472/:488),
 *     {@code nei} (GT6RecipeMapViewerMeta.PLATE_TEXTURE :608, the composed viewer plate —
 *     the 永保留 ruling);</li>
 *   <li><b>parts-pipeline crop sources</b> — {@code advancedcraftingtable} +
 *     {@code advancedcraftingtablecharging} are the source sheets the composed-UI parts
 *     were cropped from (mdk/tools/crop_gui_parts.py:53-55 and the README parts table);
 *     deleting them orphans the parts provenance/re-derivation face;</li>
 *   <li><b>data-grounded</b> — the other 67 are the {@code RecipeMap.mGUIPath} values
 *     GT6RecipeMaps passes verbatim from the upstream RM.java/FM.java rows (the
 *     upstream-fidelity ledger, pinned per-map by GT6RecipeMapsTest and siblings). They
 *     are grounded-on-disk ENFORCED by
 *     {@code GT6TextureCensusTest.everyRecipeMapGuiPathIsLowercaseLegalAndGroundedOnDisk}
 *     (the chisel exemption is the sole dangling path; the 2026-10-05 hotfix 40ec0d7c8
 *     chose re-grounding over exemption when assembler/nanofab went missing — the
 *     established policy). Their RENDER face is map-dependent: only the maps carried by
 *     the vanilla-leg Hosts above and the tower MUI ever reach a blit; the rest ride the
 *     MUI theme base (GTBasicMachineMUI's sheet arm is the removed half of the
 *     double-layer stack, r11-gui-basicmachine-clean-sample). Deep retirement of these
 *     67 values is a deliberate upstream-ledger policy change, not this card's move —
 *     the per-name class table below is the input for that ruling.</li>
 * </ul>
 *
 * <p>The ratchet (this card's deliverable): the on-disk set must equal this whitelist
 * exactly (a sheet added without a reference and a row here fails; a sheet deleted
 * without shrinking the whitelist fails), and every whitelisted sheet must still carry
 * its reference needle ({@code gui/machines/<name>} with a delimiter lookahead) in
 * {@code src/main/java} — when a future retirement kills a value's last reference this
 * pin goes red and forces the conscious file + whitelist + README-manifest removal
 * instead of a silent orphan. The README reskin manifest face (disk ↔ rows, sha256) is
 * GT6GuiReskinCensusTest's wave pin; the mGUIPath grounding face is GT6TextureCensusTest.
 * Read-only, no datagen product (ADR-P20 §1.4).</p>
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class GT6MachineGuiSheetCensusTest {

    /** The audited sheets dir (the reskin census' home). */
    private static final Path SHEETS_DIR = Path.of("src", "main", "resources", "assets", "gt6", "textures", "gui", "machines");

    /**
     * Render-alive: a screen or panel blits the PNG through a registered/open chain.
     * Evidence pointers live in the class javadoc; the names here are the sheet stems.
     */
    private static final Set<String> RENDER_ALIVE = Set.of(
        "cokeoven", "dryer", "canner", "oven",
        "distillationtower", "cryodistillationtower",
        "bumbliary", "bumbliaryadvanced",
        "nei");

    /** Parts-pipeline crop sources — the needle face is mdk/tools/crop_gui_parts.py, not src/main/java. */
    private static final Set<String> PARTS_CROP_SOURCES = Set.of(
        "advancedcraftingtable", "advancedcraftingtablecharging");

    /**
     * Data-grounded: the RecipeMap.mGUIPath values (upstream RM.java/FM.java rows,
     * lowercased) whose PNG must stay grounded per GT6TextureCensusTest. Alphabetical,
     * one per disk file — a name here without a file (or vice versa) fails the set pin.
     */
    private static final List<String> DATA_GROUNDED = List.of(
        "alloying", "anvil", "anvilbend", "autoclave", "bath", "bedrockorelist",
        "boxinator", "bumblelyzer", "burnmixer", "catalyticcracking", "centrifuge",
        "clustermill", "coagulator", "compressor", "cooker", "crafting", "crafting2by2",
        "crusher", "cryomixer", "crystallisationcrucible", "cutter", "default",
        "distillery", "electrolyzer", "extruder", "fermenter", "freezer", "fusion",
        "generifier", "hammer", "implosioncompressor", "injector", "juicer",
        "laminator", "laserengraver", "lathe", "lightning", "loom",
        "magneticseparator", "massfab", "melter", "mixer", "mortar", "nanofab",
        "plantalyzer", "polarizer", "press", "pressurewasher", "printer",
        "replicator", "roaster", "rollbender", "rollformer", "rollingmill",
        "scannermolecular", "scannervisuals", "sharpener", "shredder", "sifter",
        "slicer", "sluice", "smelter", "squeezer", "steamcracking", "unboxinator",
        "welder", "wiremill");

    private static Set<String> expectedStems() {
        Set<String> all = new TreeSet<>(RENDER_ALIVE);
        all.addAll(PARTS_CROP_SOURCES);
        all.addAll(DATA_GROUNDED);
        return all;
    }

    /** The same upward mdk-root probe the sibling census tests use (tools/gen_textures.py marker). */
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

    /**
     * The set pin: disk == whitelist, bijective (never vacuous), plus the 永保留 anchor —
     * the nei.png plate (GT6RecipeMapViewerMeta.PLATE_TEXTURE) must never leave the set.
     */
    @Test
    void theOnDiskSheetSetIsExactlyTheAuditedWhitelist() throws IOException {
        Set<String> expected = expectedStems();
        assertEquals(78, expected.size(),
            "the whitelist itself drifted off the audited 78 — re-run the reference audit "
                + "before touching this pin");
        assertTrue(expected.containsAll(RENDER_ALIVE) && expected.contains("nei"),
            "the nei.png plate is the 永保留 ruling — it may not leave the whitelist");
        Set<String> onDisk = onDiskStems();
        Set<String> missing = new TreeSet<>(expected);
        missing.removeAll(onDisk);
        Set<String> stray = new TreeSet<>(onDisk);
        stray.removeAll(expected);
        assertTrue(missing.isEmpty() && stray.isEmpty(),
            "gui/machines is not the audited reference union — a whitelisted sheet died ("
                + missing + ": delete its references first, then shrink this whitelist and the "
                + "README reskin manifest) or a stray sheet appeared (" + stray
                + ": give it a live code reference and a class above, or delete it)");
    }

    /**
     * The reference-union face: every whitelisted stem still has its
     * {@code gui/machines/<stem>} needle (delimiter-lookahead so <code>anvil</code> is not
     * satisfied by <code>anvilbend</code>) in a {@code src/main/java} source file — the
     * crop-source pair's needle face is the crop script instead. A red here means a
     * retirement killed the last reference: delete the sheet, shrink the whitelist and
     * the README manifest row in the same change.
     */
    @Test
    void everyWhitelistedSheetKeepsItsCodeReference() throws IOException {
        Set<String> pending = expectedStems();
        pending.removeAll(PARTS_CROP_SOURCES);
        String cropScript = Files.readString(mdkRoot().resolve(Path.of("tools", "crop_gui_parts.py")),
            StandardCharsets.UTF_8);
        for (String stem : PARTS_CROP_SOURCES) {
            assertTrue(cropScript.contains("gui/machines/" + stem + ".png"),
                "crop source " + stem + ".png lost its crop_gui_parts.py row — it is no longer "
                    + "a parts-pipeline source; audit and delete the sheet or restore the row");
        }
        List<String> stems = List.copyOf(pending); // TreeSet — already sorted, index-stable
        List<Path> sources;
        try (Stream<Path> walk = Files.walk(mdkRoot().resolve(Path.of("src", "main", "java")))) {
            sources = walk.filter(p -> p.getFileName().toString().endsWith(".java")).toList();
        }
        assertTrue(sources.size() >= 500, "only " + sources.size()
            + " main sources walked — the source tree moved, fix the probe before trusting this pin");
        // ponytail: single combined-text scan (a few MB) beats per-name file indexing at this set size
        StringBuilder combined = new StringBuilder();
        for (Path p : sources) {
            combined.append(Files.readString(p, StandardCharsets.UTF_8));
        }
        String corpus = combined.toString();
        List<String> unreferenced = new ArrayList<>();
        for (String stem : stems) {
            Pattern needle = Pattern.compile("gui/machines/" + stem + "(?=\\.png|\"|\\|)");
            if (!needle.matcher(corpus).find()) {
                unreferenced.add(stem);
            }
        }
        assertTrue(unreferenced.isEmpty(),
            "sheets whose last gui/machines reference died — delete the PNG, the whitelist row "
                + "and the README manifest row in the same change: " + unreferenced);
    }
}
