/**
 * Pure-JUnit overlay dedupe census for task overlay-texture-dedupe — the anti-expansion
 * guard for the item overlay canonicalization wave.
 *
 * <p>Background (the wave's one-hand evidence): the 4816 {@code *overlay*.png} files under
 * {@code assets/gt6/textures/} collapsed by md5 into 631 content groups; the item-domain
 * families (the p27 material_sets overlay borrows + the item flat iconset overlays + the
 * pocket multitool family) held 2871 same-content duplicates — 2146 of them fully
 * transparent VOID passes (upstream copies the same <CODE>&lt;Prefix&gt;_OVERLAY</CODE> art into
 * every SET folder, TextureSet.java:113-116, and ships byte-different encodings of
 * pixel-empty handles/slots). Each distinct content group now ships ONCE as
 * {@code item/overlay/<semantic>.png} (byte-identical to one former member); every
 * (iconset, prefix) member resolves onto the canonical at datagen time
 * ({@code GT6ItemModels.OVERLAY_CANONICAL}); the singleton overlays keep their paths.</p>
 *
 * <p>Pins asserted here:</p>
 * <ul>
 *   <li>zero same-content duplicates across the whole item overlay family (static ∪
 *       generated) — a re-expanded per-set copy of any deduped group re-trips this
 *       instantly (the "防再展开" nail);</li>
 *   <li>the exact family whitelist count (51 canonicals + 117 singletons = 168) — any
 *       re-expansion or stray overlay file re-pins deliberately;</li>
 *   <li>the canonical directory is EXACTLY the alias table's value set — no private
 *       canonicals outside {@code GT6ItemModels.OVERLAY_CANONICAL}, no alias pointing at
 *       a missing file;</li>
 *   <li>the alias table itself is non-degenerate (thousands of members, every canonical
 *       named under item/overlay/) so this census can never pass vacuously.</li>
 * </ul>
 *
 * <p>Same posture as {@link GT6TextureCensusTest}: read-only over the two committed
 * resource trees, writes nothing.</p>
 *
 * <p>Second batch (task overlay-dedupe-block-leg): the BLOCK domain collapsed its own
 * 2057-file overlay census into 550 content groups — 124 real-art canonicals under
 * {@code block/overlay/}, the 571 pixel-empty members riding the batch-1
 * {@code item/overlay/empty}, 402 unique-content singletons untouched; the pins below
 * hold that end state. NEVER-MERGE exclusion: everything under
 * {@code block/materialicons/} — the 22 SETs x {ore, ore_small, ore_overlay,
 * ore_small_overlay} atlas sources are pinned DISTINCT by GT6OreCensusTest (pixel-empty
 * upstream, but the per-SET slot paths are the identity), and the pipe_side /
 * block_plate_gem arts are the same per-material art-slot class — so the materialicons
 * files sit outside this family predicate by design.</p>
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class GT6OverlayDedupeCensusTest {

    private static final Path STATIC_TREE = Path.of("src", "main", "resources");
    private static final Path GENERATED_TREE = Path.of("src", "generated", "resources");
    private static final String TEXTURES_PREFIX = "assets/gt6/textures";

    /**
     * The pinned item overlay family total: 51 content-group canonicals under
     * {@code item/overlay/} + 117 singletons (unique content, paths unchanged). The
     * canonical split at the wave: empty.png <- 2146 transparent VOID members,
     * pocket_tool.png <- 8, 49 real-art canonicals <- 717 members.
     */
    private static final int PINNED_FAMILY_TOTAL = 168;
    private static final int PINNED_CANONICAL_TOTAL = 51;
    private static final int PINNED_ALIAS_MEMBER_TOTAL = 2855; // +156 casing quartet + 8 pocket multitool (see the rows)

    /** Location of the mdk project root, walking up from the (leg-dependent) test working dir. */
    private static Path mdkRoot() {
        for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
            if (Files.isRegularFile(p.resolve("src/main/resources/assets/README.md"))) return p;
        }
        throw new AssertionError("mdk root (src/main/resources/assets/README.md) not found upward from "
            + Path.of("").toAbsolutePath());
    }

    /** The item overlay family: every PNG under item/ whose name carries "overlay", one tree. */
    private static Set<String> itemOverlayRels(Path mdk, Path treeRoot) throws IOException {
        Set<String> rels = new TreeSet<>();
        Path textures = mdk.resolve(treeRoot).resolve(TEXTURES_PREFIX + "/item");
        if (!Files.isDirectory(textures)) {
            return rels;
        }
        try (Stream<Path> walk = Files.walk(textures)) {
            walk.filter(Files::isRegularFile)
                .filter(p -> {
                    String name = p.getFileName().toString();
                    // the canonicals live under item/overlay/ (semantic names without the
                    // suffix); the singletons carry "overlay" in the filename
                    return name.endsWith(".png")
                        && (name.contains("overlay") || p.getParent().endsWith("overlay"));
                })
                .forEach(p -> rels.add(TEXTURES_PREFIX + "/item/"
                    + textures.relativize(p).toString().replace('\\', '/')));
        }
        return rels;
    }

    // ------------------------------------------------------------------ block domain

    /** The pinned block overlay family total: 124 canonicals + 402 unique singletons (materialicons excluded). */
    private static final int PINNED_BLOCK_FAMILY_TOTAL = 526;
    /** The 124 block content-group canonicals under block/overlay/. */
    private static final int PINNED_BLOCK_CANONICAL_TOTAL = 124;
    /** The block alias member total (1037 same-content + 571 transparent members). */
    private static final int PINNED_BLOCK_ALIAS_MEMBER_TOTAL = 1608;

    /**
     * The block overlay family: every PNG under block/ whose path carries "overlay",
     * OUTSIDE block/materialicons/ (the never-merge per-material art slots, see the
     * class javadoc). One tree.
     */
    private static Set<String> blockOverlayRels(Path mdk, Path treeRoot) throws IOException {
        Set<String> rels = new TreeSet<>();
        Path textures = mdk.resolve(treeRoot).resolve(TEXTURES_PREFIX + "/block");
        if (!Files.isDirectory(textures)) {
            return rels;
        }
        try (Stream<Path> walk = Files.walk(textures)) {
            walk.filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().endsWith(".png"))
                // the RELATIVIZED path decides (the worktree dir itself may carry "overlay")
                .filter(p -> {
                    String rel = TEXTURES_PREFIX + "/block/"
                        + textures.relativize(p).toString().replace('\\', '/');
                    return rel.contains("overlay") && !rel.contains("materialicons");
                })
                .forEach(p -> rels.add(TEXTURES_PREFIX + "/block/"
                    + textures.relativize(p).toString().replace('\\', '/')));
        }
        return rels;
    }

    /**
     * Block pins 1 + 2: the family is exactly the whitelist and content-unique — a
     * re-expanded family face copy (or a resurrected transparent pass) breaks both.
     */
    @Test
    void blockOverlayFamilyIsWhitelistedAndContentUnique() throws Exception {
        Path mdk = mdkRoot();
        Set<String> family = blockOverlayRels(mdk, STATIC_TREE);
        family.addAll(blockOverlayRels(mdk, GENERATED_TREE));
        assertEquals(PINNED_BLOCK_FAMILY_TOTAL, family.size(),
            "block overlay family drifted — re-run the md5 census and re-pin deliberately");

        Map<String, List<String>> byContent = new HashMap<>();
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        for (String rel : family) {
            Path png = mdk.resolve(STATIC_TREE).resolve(rel);
            if (!Files.isRegularFile(png)) {
                png = mdk.resolve(GENERATED_TREE).resolve(rel);
            }
            byContent.computeIfAbsent(
                HexFormat.of().formatHex(sha256.digest(Files.readAllBytes(png))),
                k -> new ArrayList<>()).add(rel);
        }
        List<String> duplicated = byContent.values().stream()
            .filter(l -> l.size() > 1)
            .map(l -> String.join(" == ", l))
            .toList();
        assertTrue(duplicated.isEmpty(), "same-content block overlay files (the dedupe regression): "
            + duplicated.size() + "\n  " + String.join("\n  ", duplicated));
    }

    /**
     * Block pin 3: the alias table is non-degenerate, every canonical sits under
     * block/overlay/ (the empty group under item/overlay/), and block/overlay/ ships
     * EXACTLY the alias value set — no private canonicals, no alias to a missing file.
     */
    @Test
    void blockAliasTableIsNonDegenerateAndMatchesTheCanonicalDirectory() throws IOException {
        Map<String, String> alias = GT6BlockOverlayAliases.ALIAS;
        assertTrue(alias.size() >= PINNED_BLOCK_ALIAS_MEMBER_TOTAL,
            "block alias table implausibly small (" + alias.size() + ")");

        Path mdk = mdkRoot();
        Set<String> expected = new TreeSet<>();
        for (String canonical : Set.copyOf(alias.values())) {
            assertTrue(canonical.startsWith("block/overlay/") || canonical.equals("item/overlay/empty"),
                "block canonical outside block/overlay/: " + canonical);
            expected.add(TEXTURES_PREFIX + "/" + canonical + ".png");
        }
        assertEquals(PINNED_BLOCK_CANONICAL_TOTAL + 1, expected.size(),
            "distinct block canonical count drifted — re-pin with the merge table in assets/README.md");
        assertTrue(Files.isRegularFile(mdk.resolve(STATIC_TREE).resolve(TEXTURES_PREFIX + "/item/overlay/empty.png")),
            "the transparent groups ride the batch-1 item/overlay/empty canonical — it must exist");
        expected.remove(TEXTURES_PREFIX + "/item/overlay/empty.png"); // its home is the item family (pins above)

        Set<String> onDisk = new TreeSet<>();
        for (Path tree : List.of(STATIC_TREE, GENERATED_TREE)) {
            Path dir = mdk.resolve(tree).resolve(TEXTURES_PREFIX + "/block/overlay");
            if (!Files.isDirectory(dir)) {
                continue;
            }
            try (Stream<Path> walk = Files.list(dir)) {
                walk.filter(p -> p.getFileName().toString().endsWith(".png"))
                    .forEach(p -> onDisk.add(TEXTURES_PREFIX + "/block/overlay/"
                        + p.getFileName().toString().replace('\\', '/')));
            }
        }
        Set<String> missing = new TreeSet<>(expected);
        missing.removeAll(onDisk);
        Set<String> extra = new TreeSet<>(onDisk);
        extra.removeAll(expected);
        assertTrue(missing.isEmpty(), "block alias canonical with no file on disk: " + missing);
        assertTrue(extra.isEmpty(), "canonical file outside the block alias table (private canonical): " + extra);
    }

    /**
     * Pin 1 + 2: the whole item overlay family holds exactly the pinned whitelist, and no
     * two of its files share content bytes — a re-expanded per-set copy of any deduped
     * content group (or a stray file) breaks both halves at once.
     */
    @Test
    void itemOverlayFamilyIsWhitelistedAndContentUnique() throws Exception {
        Path mdk = mdkRoot();
        Set<String> family = itemOverlayRels(mdk, STATIC_TREE);
        family.addAll(itemOverlayRels(mdk, GENERATED_TREE));
        assertEquals(PINNED_FAMILY_TOTAL, family.size(),
            "item overlay family drifted — re-run the md5 census and re-pin deliberately "
            + "(a re-expanded dedupe copy is exactly the regression this pin exists for)");

        Map<String, List<String>> byContent = new HashMap<>();
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        for (String rel : family) {
            // one copy per path across the two trees is guaranteed upstream (the census
            // exactly-once/no-cross-tree pins); digest the static face, fall back to generated
            Path png = mdk.resolve(STATIC_TREE).resolve(rel);
            if (!Files.isRegularFile(png)) {
                png = mdk.resolve(GENERATED_TREE).resolve(rel);
            }
            byContent.computeIfAbsent(
                HexFormat.of().formatHex(sha256.digest(Files.readAllBytes(png))),
                k -> new ArrayList<>()).add(rel);
        }
        List<String> duplicated = byContent.values().stream()
            .filter(l -> l.size() > 1)
            .map(l -> String.join(" == ", l))
            .toList();
        assertTrue(duplicated.isEmpty(), "same-content overlay files (the dedupe regression): "
            + duplicated.size() + "\n  " + String.join("\n  ", duplicated));
    }

    /**
     * Pin 3: the canonical directory ships EXACTLY the alias table's value set — every
     * canonical file exists, no private canonicals, no alias pointing outside item/overlay/.
     */
    @Test
    void canonicalDirectoryMatchesTheAliasTableExactly() throws IOException {
        Map<String, String> alias = GT6ItemModels.OVERLAY_CANONICAL;
        assertTrue(alias.size() >= PINNED_ALIAS_MEMBER_TOTAL,
            "alias table implausibly small (" + alias.size() + ") — the census must never pass vacuously");

        Path mdk = mdkRoot();
        Set<String> expected = new TreeSet<>();
        for (String canonical : Set.copyOf(alias.values())) {
            assertTrue(canonical.startsWith("item/overlay/"),
                "canonical outside item/overlay/: " + canonical);
            expected.add(TEXTURES_PREFIX + "/" + canonical + ".png");
        }
        assertEquals(PINNED_CANONICAL_TOTAL, expected.size(),
            "distinct canonical count drifted — re-pin with the merge table in assets/README.md");

        Set<String> onDisk = new TreeSet<>();
        for (Path tree : List.of(STATIC_TREE, GENERATED_TREE)) {
            Path dir = mdk.resolve(tree).resolve(TEXTURES_PREFIX + "/item/overlay");
            if (!Files.isDirectory(dir)) {
                continue;
            }
            try (Stream<Path> walk = Files.list(dir)) {
                walk.filter(p -> p.getFileName().toString().endsWith(".png"))
                    .forEach(p -> onDisk.add(TEXTURES_PREFIX + "/item/overlay/"
                        + p.getFileName().toString().replace('\\', '/')));
            }
        }
        Set<String> missing = new TreeSet<>(expected);
        missing.removeAll(onDisk);
        Set<String> extra = new TreeSet<>(onDisk);
        extra.removeAll(expected);
        assertTrue(missing.isEmpty(), "alias canonical with no file on disk: " + missing);
        assertTrue(extra.isEmpty(), "canonical file outside the alias table (private canonical): " + extra);
    }
}
