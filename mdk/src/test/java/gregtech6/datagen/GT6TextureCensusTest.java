/**
 * Pure-JUnit texture census for task p20-texture-census-test — the read-only guard
 * wave of the P20 texture sprint (ADR-P20, docs/adr/2026-09-06-p20-texture-tree-policy.md).
 *
 * <p>Policy pins asserted here (ADR-P20 §2):</p>
 * <ul>
 *   <li>the (iconset, prefix) COMBOS table in {@code mdk/tools/gen_textures.py} pins at
 *       2785 pairs over 40 sets (census 2026-08-30/09-06, research card
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
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
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
     * The machine-wave borrowed families (task p32-hygiene-lang-assets), static-tree
     * texture path prefixes under {@link #TEXTURES_PREFIX}: the three controller colored
     * faces (fusion/implosion/graagg, the attribution backfill) plus the massfab small+
     * large families and the retriever cover pair (already ledgered by their own cards).
     */
    private static final List<String> BORROWED_FAMILY_PREFIXES = List.of(
        "block/fusionreactor_colored_", "block/implosioncompressor_colored_",
        "block/vondagraagg_colored_", "block/massfab_colored_", "block/massfab_overlay_",
        "block/largemassfab_colored_", "block/retrieveritem/");

    /** The wave pin: 50 borrowed controller/cover PNGs + 20 port-generated comb icons. */
    private static final int PINNED_WAVE_TOTAL = 70;

    /**
     * The pinned distinct iconset count over the same table (walked 2026-09-06: 40 sets
     * on the table and on disk; upstream TextureSet.java's 41 total includes the unused
     * "PLASMA" family — the research card's "37" was a stale P3-era figure).
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
     * Pin a: the COMBOS table holds exactly 2785 (iconset, prefix) pairs over 40 sets.
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
        Path itemModels = mdkRoot().resolve(GENERATED_TREE).resolve("assets/gt6/models/item");
        assertTrue(Files.isDirectory(itemModels), "generated item model tree missing: " + itemModels);
        List<String> unresolved = new ArrayList<>();
        int models = unresolvedTextureRefsUnder(itemModels, unresolved);
        assertTrue(models > 0, "no item models walked — the census must never pass vacuously");
        assertTrue(unresolved.isEmpty(), sample("unresolved gt6: texture reference", unresolved));
    }

    /**
     * Pin d2 (task p38-c5-asset-coverage-guard): the same resolution over the generated
     * BLOCK models. The census found pin d blind to the block face — a block model
     * referencing a missing PNG bakes the missing-texture checkerboard into the world,
     * and EFH only validates the datagen call sites, not a ref renamed inside the borrowed
     * tree after the fact. One walk implementation for both faces.
     */
    @Test
    void everyBlockModelTextureReferenceResolves() throws IOException {
        Path blockModels = mdkRoot().resolve(GENERATED_TREE).resolve("assets/gt6/models/block");
        assertTrue(Files.isDirectory(blockModels), "generated block model tree missing: " + blockModels);
        List<String> unresolved = new ArrayList<>();
        int models = unresolvedTextureRefsUnder(blockModels, unresolved);
        assertTrue(models > 0, "no block models walked — the census must never pass vacuously");
        assertTrue(unresolved.isEmpty(), sample("unresolved gt6: block-model texture reference", unresolved));
    }

    /** The shared pin-d walk: collect every unresolved gt6: texture ref under a model tree. */
    private int unresolvedTextureRefsUnder(Path modelsDir, List<String> unresolved) throws IOException {
        Set<String> resolvable = new HashSet<>();
        for (String rel : textureRels(mdkRoot().resolve(STATIC_TREE))) {
            resolvable.add(toResourcePath(rel));
        }
        for (String rel : textureRels(mdkRoot().resolve(GENERATED_TREE))) {
            resolvable.add(toResourcePath(rel));
        }
        int models = 0;
        try (Stream<Path> walk = Files.walk(modelsDir)) {
            for (Path model : walk.filter(p -> p.getFileName().toString().endsWith(".json")).toList()) {
                models++;
                JsonObject textures = JsonParser.parseString(Files.readString(model, StandardCharsets.UTF_8))
                    .getAsJsonObject().getAsJsonObject("textures");
                if (textures == null) {
                    continue; // parent-model, no direct texture refs
                }
                for (var entry : textures.entrySet()) {
                    String ref = entry.getValue().getAsString();
                    if (ref.startsWith("gt6:") && !resolvable.contains(ref)) {
                        unresolved.add(modelsDir.relativize(model) + " [" + entry.getKey() + "] -> " + ref);
                    }
                }
            }
        }
        return models;
    }

    /**
     * Pin e (task p32-hygiene-lang-assets, the M2 script assertion): the P31 new-texture
     * wave carries its assets/README.md attribution with ZERO gaps — every borrowed PNG
     * is named in the ledger AND its actual file bytes hash to a sha256 the ledger
     * records (the name check alone can pass while the prose describes a different file;
     * the digest check alone can pass across machines because upstream ships identical
     * uniform tiles), and the port-generated comb family keeps its declared-art face.
     * The family prefixes pin the wave's scope; a new borrowed family must extend them
     * AND land its README section in the same PR.
     */
    @Test
    void newTextureWaveCarriesFullAttribution() throws Exception {
        String readme = Files.readString(mdkRoot().resolve(STATIC_TREE).resolve("assets/README.md"),
            StandardCharsets.UTF_8);
        List<String> borrowed = new ArrayList<>();
        List<String> combs = new ArrayList<>();
        for (String rel : textureRels(mdkRoot().resolve(STATIC_TREE))) {
            String sub = rel.substring(TEXTURES_PREFIX.length() + 1);
            if (sub.startsWith("item/comb/")) {
                combs.add(rel);
            } else if (BORROWED_FAMILY_PREFIXES.stream().anyMatch(sub::startsWith)) {
                borrowed.add(rel);
            }
        }
        assertEquals(PINNED_WAVE_TOTAL, borrowed.size() + combs.size(),
            "the borrowed texture wave drifted (families added/removed) — extend the pins and the README section together");
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        List<String> violations = new ArrayList<>();
        for (String rel : borrowed) {
            Path png = mdkRoot().resolve(STATIC_TREE).resolve(rel);
            String name = png.getFileName().toString();
            String hex = HexFormat.of().formatHex(sha256.digest(Files.readAllBytes(png)));
            if (!readme.contains(name)) {
                violations.add(rel + " — filename absent from assets/README.md");
            }
            if (!readme.contains(hex)) {
                violations.add(rel + " — file bytes hash to " + hex + ", not grounded in assets/README.md");
            }
        }
        assertTrue(violations.isEmpty(), sample("P31 borrowed texture without attribution", violations));
        assertTrue(readme.contains("item/comb/comb_<name>.png") && combs.size() == 20,
            "the comb family's port-generated-art declaration (the (x20) tint table) is missing or the family drifted");
    }

    /**
     * The declared pin-f exceptions (task p38-c5-asset-coverage-guard): the ledger rows
     * whose digest records an UPSTREAM SOURCE, not a shipped file — the src-over composite
     * sources (basicmachine front/overlay_active pairs, press/laser overlays), the
     * upstream animation frame strips (16x64/16x96/16x128 sheets) and the keypad 0-15
     * sheet, plus the one aggregate row (the 36-file turbine_mains find|sha256sum). These
     * bytes are not vendored, so no tree file can ground them. The anti-rot half of
     * {@link #fullLedgerDigestsGroundToActualAssets()} asserts they stay ungrounded: the
     * moment such a source ships verbatim, the row grounds and must leave this list.
     */
    private static final List<String> UPSTREAM_SOURCE_ONLY_LEDGER_ROWS = List.of(
        "15d955d59a6d1a33a75203672f977b709f3081c2febe16910bad58fa97e085c9",
        "1eba237ba18bd29f2800e326118adba65a39dbf0dca9fb037d3fd6bbc96aea85",
        "20c309d9ff4175bf0f04b4c57efbd58fa1915f1c69907abd3bd91d6d01b70138",
        "2efd256b8f70385118ecff73075b3b20d62ae9c6a6f7e674c0d7253b0c89eb70",
        "3ac3ffc4f17a13b6ffdb9ed2c18f845100315854467acdec0c45b7ca772969d0",
        "40ac47fac76cd859703bde605b0095cb33f55ffa9da9323fe5299f4dec75aee9",
        "48b6811ef825b2be50dcbda29ef5e2a3f707708a499a2d3796561ddb29770185",
        "4a690d5ec4647aafe3277730b26883197f198a5077e62c82c63772f9912d0b8f",
        "4c787010041ea75483b4eedffb17e402c8763de27e2271a4eacccf960972ec71",
        "4d02bba819e7368ae9b8ef5441fca9d586ddc37f4a9b3b553af2802fa2e923c3",
        "4d8ee0b3a3b6d5979caeecb74e7044d79f3e4b9abf3f19e0dc36ffe0ea2d0be2",
        "50577244ea9ba4406d6541eda6e042ec2f284bb1fce673293ae726faf0ab45cc",
        "5c728ec16e0f33fd41d656f32d8f697d9622e324666e909d37ed0af386530594",
        "622d6f2a90b342366a98180f0a4b8a257b4c718908abd2b96deae4f6a9f1a0f1",
        "634a425059a530e1a2279342717cf77e3e1d0ff6eac2933dee0492e2118c62be",
        "6569a89537bc53d51e88c51edc435b8b8cf66d1bc2a040cec6b36d2c0074b736",
        "6af996a09c044b943dc6d0fc69cbe7ac74340e860b952c10429cff28426a5675",
        "6b6100db65c96a4185f1563bdc0657f67f87c9c04f7d76a6fd9ebe8ae6620903",
        "6fc656f9cd0a57f4023da022bb7056a6e05c93ac61f4369e7e785360ce5e7a4b",
        "751b8ced7db40f0a5b284d1763f4b1d03fb4a559f73277da8c389ca7502bb8bf",
        "797d94f589520541dfb74922a86e1f40b48454a3083880e23a78e620504caa48",
        "7987357d4fd1205431cfde328f9e442cc454d178bb87a2d05ca3659aa4d7eec3",
        "822b52fc5f647891438ad6467c58f05135946e6272b4e3826ccd9ffa7ce07ab6",
        "84e4deec9a2653627feb48946343b839d73709d8614f57adf1d3a19001ae12c3",
        "8592fa60fdc687e7585ff8e900f1c6ada9798219172296c12e03fd5389973d99",
        "8a6ac14f9b121618afff00b3bf7372484adbe7a0307848ee9f96188536b3ce45",
        "8b1448355e209cb60c894b42b564302bd12b5f960738c89eb022fca01667694b",
        "920176fe0c003f6f293aab5fc344418356377d273c414a8f5378755e25674891",
        "945780fdf0e036b853ac844fdd9e2fc17bb5740098a6b48786c6f1389391b9a2",
        "a407c167ef8365400016a797e34577a8d9c9b6da6e7049d0b7a280307b7c0731",
        "a7e6ff28615bd73a017a9e59518023b80e823a6178117d58fecfc606053d5ee6",
        "b4025444ec680bc3cbc8f22bd66fb8c4c64294fbbea8ed932c83cbc917bbf23a",
        "b6fd3ae7a44bb1b25a28f70b1ebba42625cda93e796d35ee5be6c8f18f753dd2",
        "bcbdd663b5fe66ea453245f93a89f7987066b42185d43d602dd738f303d83ee3",
        "bd47d5cd7bb3ed1fc17d30c4c405716c46246f359a276c56be8d5f0282ff33f5",
        "c02c6dda70cb39555251d5de281cefecc420c5c83c91f123ac98e402508fc81a",
        "ca114cacd51c1d662cd0ee30eab3cbfeb9df03d5afa5fb09de8bb54916f1e891",
        "cab01fef8674e5611ec99e5a87b0595ec1e4df8379a10b6c9f4806c30550a592",
        "cdbbc989598b9955868e457728dc88f4cca6e317b90e3ba3fc0d117252cc327d",
        "d0d30c15da3f7bcddadbd347a99756e3c59c17870ba00238dc9e771290ee2b3f",
        "da225602274cd7769c7ce7c74e23d29f84cd6e08d77f3a6cedf5cc8f7efa1cc7",
        "e09830ea78842c0b11d1e9f0e9be0fb697859177be7e6ab684363c0fc7ab2b20",
        "e8b1c5126229ad9c05a2ddd03cefa6093f1767133f6181ad3ebbad48ce775bbb",
        "ea63a03b226d86607f2fcf091d36d3118b2c88d5f6f43227319639eb3abc469b",
        "f075bba0be3c71dfc2a9f463b9be8ae717350e1675a4531f38a25b2e1c74358c",
        "f91ced667197de43286fee72872c960ef044defaffc1ed27acac70bb2f738e64",
        "ffb243256575a2e80a6a18cf1aa057241959b745ee02ce50b420a03933d94a6a");

    /**
     * Pin f (task p38-c5-asset-coverage-guard): the FULL sha256 ledger reconciles against
     * the actual assets — every distinct digest recorded anywhere in assets/README.md must
     * ground to the sha256 of at least one PNG on the static ∪ generated face, except the
     * declared {@link #UPSTREAM_SOURCE_ONLY_LEDGER_ROWS} (composite/animation upstream
     * sources not vendored here). This is the whole-ledger extension of pin e's P31-wave
     * digest check: a borrowed PNG whose bytes drift from its ledger row (or a row whose
     * file never landed) now breaks the census instead of the provenance story.
     */
    @Test
    void fullLedgerDigestsGroundToActualAssets() throws Exception {
        Set<String> groundable = new HashSet<>();
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        for (Path tree : List.of(mdkRoot().resolve(STATIC_TREE), mdkRoot().resolve(GENERATED_TREE))) {
            for (String rel : textureRels(tree)) {
                groundable.add(HexFormat.of().formatHex(
                    sha256.digest(Files.readAllBytes(tree.resolve(rel)))));
            }
        }
        String readme = Files.readString(mdkRoot().resolve(STATIC_TREE).resolve("assets/README.md"),
            StandardCharsets.UTF_8);
        Set<String> ledger = new HashSet<>();
        java.util.regex.Matcher ledgerRow = java.util.regex.Pattern.compile("[0-9a-f]{64}").matcher(readme);
        while (ledgerRow.find()) {
            ledger.add(ledgerRow.group());
        }
        assertTrue(ledger.size() >= 1000, "ledger digest count implausibly small: " + ledger.size()
            + " — the README parse broke, the pin must never pass vacuously");
        List<String> ungrounded = new ArrayList<>();
        for (String hex : ledger) {
            if (!groundable.contains(hex) && !UPSTREAM_SOURCE_ONLY_LEDGER_ROWS.contains(hex)) {
                ungrounded.add(hex);
            }
        }
        assertTrue(ungrounded.isEmpty(), sample("ledger digest that grounds to NO shipped PNG "
            + "(update the README row or, for a new upstream-source row, declare it)", ungrounded));
        List<String> healed = new ArrayList<>();
        for (String hex : UPSTREAM_SOURCE_ONLY_LEDGER_ROWS) {
            if (groundable.contains(hex)) {
                healed.add(hex);
            }
        }
        assertTrue(healed.isEmpty(), sample("declared upstream-source row now ships in the tree — "
            + "delete it from UPSTREAM_SOURCE_ONLY_LEDGER_ROWS", healed));
    }

    /** "assets/gt6/textures/item/x.png" -> "gt6:item/x" (the model-side reference form). */
    private static String toResourcePath(String rel) {
        String under = rel.substring(TEXTURES_PREFIX.length() + 1, rel.length() - ".png".length());
        return "gt6:" + under;
    }
}
