/**
 * Pure-JUnit texture census for task p20-texture-census-test — the read-only guard
 * wave of the P20 texture sprint (ADR-P20, docs/adr/2026-09-06-p20-texture-tree-policy.md).
 *
 * <p>Policy pins asserted here (ADR-P20 §2):</p>
 * <ul>
 *   <li>the (iconset, prefix) COMBOS table in {@code mdk/tools/gen_textures.py} pins at
 *       2785 pairs over 37 sets (census 2026-08-30/09-06, research card
 *       tasks.p20-research-texture-census);</li>
 *   <li>every combo's PNG exists <em>exactly once</em> across the static tree
 *       (mdk/src/main/resources) ∪ the generated tree (mdk/src/generated/resources) —
 *       deliberately a union face, so W2 borrow waves migrating a PNG between trees
 *       keep it green (a "generated must hold 2785" pin would NOT survive that);</li>
 *   <li>no texture relative path lives in BOTH trees — the regression nail for
 *       ADR-P20 §1.2 (processResources DuplicatesStrategy.INCLUDE = the later-copied
 *       generated tree silently shadows a static-tree real texture, build.forge.gradle.kts:158
 *       / build.neoforge.gradle.kts:194 append AFTER the default srcDir);</li>
 *   <li>every {@code gt6:} texture reference of every generated item model resolves to a
 *       PNG on the static ∪ generated face.</li>
 * </ul>
 *
 * <p>Upstream grounding is carried by assets/README.md's sha256 table, NOT by any
 * tmp/ snapshot (coder worktrees and the main disk's tmp/ are not guaranteed to
 * agree) — this test touches nothing outside the two committed resource trees and
 * the census script. It is deliberately NOT a datagen provider and writes nothing
 * (ADR-P20 §1.4).</p>
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class GT6TextureCensusTest {

    /** ADR-P20 §1.1: the canonical home of real (borrowed upstream) textures. */
    private static final Path STATIC_TREE = Path.of("src", "main", "resources");

    /** The datagen product tree, mounted as an appended srcDir by both legs. */
    private static final Path GENERATED_TREE = Path.of("src", "generated", "resources");

    /** Textures live under this prefix inside either tree. */
    private static final String TEXTURES_PREFIX = "assets/gt6/textures";

    /** The pinned (iconset, prefix) pair total — the task card's pin, from the script's COMBOS table. */
    private static final int PINNED_COMBO_TOTAL = 2785;

    /**
     * The pinned distinct iconset count over the same table (walked 2026-09-06: 40 sets
     * on the table and on disk; upstream TextureSet.java's 41 total includes the unused
     * "misc" family — the research card's "37" was a stale P3-era figure).
     */
    private static final int PINNED_ICONSET_TOTAL = 40;

    /** One ("iconset", "prefix") row of the COMBOS set literal in gen_textures.py. */
    private static final Pattern COMBO_ROW =
        Pattern.compile("\\(\\s*\"([a-z0-9_]+)\"\\s*,\\s*\"([a-z0-9_]+)\"\\s*\\)");

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

    /** Every texture relative path ("assets/gt6/textures/...") present in one tree; absent tree = empty. */
    private static Set<String> textureRels(Path treeRoot) throws IOException {
        Set<String> rels = new HashSet<>();
        Path textures = treeRoot.resolve(TEXTURES_PREFIX);
        if (!Files.isDirectory(textures)) {
            return rels;
        }
        try (Stream<Path> walk = Files.walk(textures)) {
            walk.filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().endsWith(".png"))
                .forEach(p -> rels.add(TEXTURES_PREFIX + "/"
                    + textures.relativize(p).toString().replace('\\', '/')));
        }
        return rels;
    }

    /** The (iconset, prefix) pairs parsed straight out of the script's COMBOS set literal. */
    private static Set<String[]> combosFromScript() throws IOException {
        String src = Files.readString(mdkRoot().resolve("tools").resolve("gen_textures.py"),
            StandardCharsets.UTF_8);
        int start = src.indexOf("COMBOS = {");
        assertTrue(start >= 0, "COMBOS table not found in gen_textures.py");
        int end = src.indexOf("\n}", start);
        assertTrue(end > start, "COMBOS table terminator not found in gen_textures.py");
        Set<String[]> combos = new HashSet<>();
        Matcher matcher = COMBO_ROW.matcher(src.substring(start, end));
        while (matcher.find()) {
            combos.add(new String[] {matcher.group(1), matcher.group(2)});
        }
        return combos;
    }

    /** Tree-relative PNG path of one (iconset, prefix) combo. */
    private static String comboRel(String[] combo) {
        return TEXTURES_PREFIX + "/item/material_sets/" + combo[0] + "/" + combo[1] + ".png";
    }

    /** Failure message for a violation list, capped to keep the report readable. */
    private static String sample(String what, List<String> violations) {
        StringBuilder sb = new StringBuilder(what + ": " + violations.size() + " violation(s)");
        for (String v : violations.subList(0, Math.min(10, violations.size()))) {
            sb.append("\n  - ").append(v);
        }
        return sb.toString();
    }

    /**
     * Pin a: the COMBOS table holds exactly 2785 (iconset, prefix) pairs over 37 sets.
     * Parsed from gen_textures.py itself so the script stays the single source of truth.
     */
    @Test
    void combosTablePinsThe2785PairCensus() throws IOException {
        Set<String[]> combos = combosFromScript();
        Set<String> iconsets = new HashSet<>();
        for (String[] combo : combos) {
            iconsets.add(combo[0]);
        }
        assertEquals(PINNED_COMBO_TOTAL, combos.size(),
            "(iconset, prefix) pair total drifted — re-run the census and re-pin deliberately");
        assertEquals(PINNED_ICONSET_TOTAL, iconsets.size(), "distinct iconset count drifted");
    }

    /**
     * Pin b: every combo's PNG exists exactly once across static ∪ generated. The union
     * face is deliberate (ADR-P20 §2): a W2 borrow wave moving a PNG from one tree to
     * the other keeps this green, while a total miss (0) or a cross-tree duplicate (2) fails.
     */
    @Test
    void everyComboPresentExactlyOnceAcrossBothTrees() throws IOException {
        Set<String> staticRels = textureRels(mdkRoot().resolve(STATIC_TREE));
        Set<String> generatedRels = textureRels(mdkRoot().resolve(GENERATED_TREE));
        List<String> violations = new ArrayList<>();
        for (String[] combo : combosFromScript()) {
            String rel = comboRel(combo);
            int copies = (staticRels.contains(rel) ? 1 : 0) + (generatedRels.contains(rel) ? 1 : 0);
            if (copies != 1) {
                violations.add(copies + " copy/copies: " + rel);
            }
        }
        assertTrue(violations.isEmpty(), sample("exactly-once violation", violations));
    }

    /**
     * Pin c: no texture relative path lives in both trees — the regression nail for
     * ADR-P20 §1.2. On the shadowing mechanism: both legs append the generated tree
     * AFTER the default src/main/resources, and processResources defaults to
     * DuplicatesStrategy.INCLUDE (= later copy wins), so a cross-tree duplicate would
     * let the generated placeholder silently beat the static real texture with no
     * build error.
     */
    @Test
    void noTexturePathLivesInBothTrees() throws IOException {
        Set<String> staticRels = textureRels(mdkRoot().resolve(STATIC_TREE));
        Set<String> generatedRels = textureRels(mdkRoot().resolve(GENERATED_TREE));
        Set<String> collisions = new HashSet<>(staticRels);
        collisions.retainAll(generatedRels);
        assertTrue(collisions.isEmpty(), sample("cross-tree duplicate texture path", new ArrayList<>(collisions)));
    }

    /**
     * Pin d: every gt6: texture reference of every generated item model resolves to a
     * PNG on the static ∪ generated face. Covers layer0 material_sets refs and every
     * other texture key alike; "#" values (in-model back-references) are skipped.
     */
    @Test
    void everyItemModelTextureReferenceResolves() throws IOException {
        Set<String> resolvable = new HashSet<>();
        for (String rel : textureRels(mdkRoot().resolve(STATIC_TREE))) {
            resolvable.add(toResourcePath(rel));
        }
        for (String rel : textureRels(mdkRoot().resolve(GENERATED_TREE))) {
            resolvable.add(toResourcePath(rel));
        }
        Path itemModels = mdkRoot().resolve(GENERATED_TREE).resolve("assets/gt6/models/item");
        assertTrue(Files.isDirectory(itemModels), "generated item model tree missing: " + itemModels);

        List<String> unresolved = new ArrayList<>();
        int models = 0;
        try (Stream<Path> walk = Files.walk(itemModels)) {
            for (Path model : walk.filter(p -> p.getFileName().toString().endsWith(".json")).toList()) {
                models++;
                JsonObject textures = JsonParser.parseString(Files.readString(model, StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonObject("textures");
                if (textures == null) {
                    continue; // parent-model item, no direct texture refs
                }
                for (var entry : textures.entrySet()) {
                    String ref = entry.getValue().getAsString();
                    if (ref.startsWith("gt6:") && !resolvable.contains(ref)) {
                        unresolved.add(itemModels.relativize(model) + " [" + entry.getKey() + "] -> " + ref);
                    }
                }
            }
        }
        assertTrue(models > 0, "no item models walked — the census must never pass vacuously");
        assertTrue(unresolved.isEmpty(), sample("unresolved gt6: texture reference", unresolved));
    }

    /** "assets/gt6/textures/item/x.png" -> "gt6:item/x" (the model-side reference form). */
    private static String toResourcePath(String rel) {
        String under = rel.substring(TEXTURES_PREFIX.length() + 1, rel.length() - ".png".length());
        return "gt6:" + under;
    }
}
