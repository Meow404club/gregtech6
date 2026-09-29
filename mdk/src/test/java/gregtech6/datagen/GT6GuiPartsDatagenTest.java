/**
 * Pure-JUnit census for the cropped GUI part sprites (task r8-gui-part-crops).
 *
 * <p>Pins asserted here:</p>
 * <ul>
 *   <li>the manifest ({@code mdk/tools/parts_manifest.json}, product of the canonical
 *       cropper {@code mdk/tools/crop_gui_parts.py}) holds exactly the 12 ledger parts
 *       over 6 sha-pinned sources (amazawa pack gregtech domain + its reskinned
 *       minecraft domain only — TFC domain is NOT a permitted source, see
 *       assets/README.md "GUI part crops");</li>
 *   <li>every part PNG is on the static tree ({@code assets/gt6/textures/gui/parts/})
 *       with manifest-exact sha256 + dimensions, is non-empty (real pixels, not a
 *       transparent stub), and is mirrored 1:1 by a {@link GT6GuiParts} constant
 *       (path + dims) — the constants class stays the single consumer-facing source
 *       of truth;</li>
 *   <li>the six committed source fixtures
 *       ({@code src/test/resources/gregtech6/guiparts/source/}) hash-match their
 *       manifest entries, and every crop rect replayed from them is PIXEL-identical
 *       to the shipped part — the version-independent provenance replay (the python
 *       {@code --verify} face is the byte-identical one);</li>
 *   <li>the parts domain is NOT a datagen product — nothing under the generated tree
 *       (runData must never emit there; ADR-P20 cross-tree rules apply as everywhere).</li>
 * </ul>
 *
 * <p>Deliberately NOT a datagen provider and writes nothing (the GT6TextureCensusTest
 * house rule). No registry bootstrap — this test boots no Minecraft registries.</p>
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import gregtech6.gui.machines.GT6GuiParts;
import gregtech6.gui.machines.GT6GuiParts.GuiPart;

class GT6GuiPartsDatagenTest {

    private static final Path STATIC_TREE = Path.of("src", "main", "resources");
    private static final Path GENERATED_TREE = Path.of("src", "generated", "resources");
    private static final String PARTS_PREFIX = "assets/gt6/textures/gui/parts";

    /** The task-card pin: 12 ledger parts over 6 sources. */
    private static final int PINNED_PART_TOTAL = 12;
    private static final int PINNED_SOURCE_TOTAL = 6;

    /** Committed source fixtures (also present, untracked, under tmp/amazawa-census/v105g). */
    private static final List<String> FIXTURE_SOURCES = List.of(
        "Default.png", "Melter.png", "Freezer.png", "Distillery.png",
        "Crafting2By2.png", "widgets.png");

    /** ResourceLocation path legality (vanilla [a-z0-9_.-/]). */
    private static final Pattern LEGAL_PATH = Pattern.compile("[a-z0-9_.\\-/]+");

    private static Path mdkRoot() {
        for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
            if (Files.isRegularFile(p.resolve("tools").resolve("gen_textures.py"))) {
                return p;
            }
        }
        throw new AssertionError("mdk root (tools/gen_textures.py) not found upward from "
            + Path.of("").toAbsolutePath());
    }

    private static String sha256(byte[] data) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        assertNotNull(in, "test resource missing");
        return in.readAllBytes();
    }

    /** One "parts" row of the manifest. */
    private record Crop(String name, String source, int x, int y, int w, int h, String sha256) {}

    private static List<Crop> manifestParts() throws IOException {
        JsonObject manifest = JsonParser.parseString(Files.readString(
            mdkRoot().resolve("tools").resolve("parts_manifest.json"))).getAsJsonObject();
        List<Crop> parts = new ArrayList<>();
        for (var e : manifest.getAsJsonArray("parts")) {
            JsonObject o = e.getAsJsonObject();
            parts.add(new Crop(o.get("name").getAsString(), o.get("source").getAsString(),
                o.get("x").getAsInt(), o.get("y").getAsInt(),
                o.get("w").getAsInt(), o.get("h").getAsInt(), o.get("sha256").getAsString()));
        }
        return parts;
    }

    private static JsonObject manifestSources() throws IOException {
        return JsonParser.parseString(Files.readString(
            mdkRoot().resolve("tools").resolve("parts_manifest.json"))).getAsJsonObject()
            .getAsJsonObject("sources");
    }

    private static Path partPath(String name) {
        return mdkRoot().resolve(STATIC_TREE).resolve(PARTS_PREFIX).resolve(name);
    }

    private static int countOpaque(BufferedImage img) {
        int n = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if ((img.getRGB(x, y) >>> 24) != 0) {
                    n++;
                }
            }
        }
        return n;
    }

    /** Pin a: the manifest holds exactly the 12 unique lowercase-legal parts over 6 sources. */
    @Test
    void manifestPinsTwelveUniqueLowercaseParts() throws IOException {
        List<Crop> parts = manifestParts();
        assertEquals(PINNED_PART_TOTAL, parts.size(), "part total drifted — re-crop and re-pin deliberately");
        assertEquals(PINNED_SOURCE_TOTAL, manifestSources().keySet().size(), "source total drifted");
        Set<String> seen = new HashSet<>();
        for (Crop c : parts) {
            assertTrue(seen.add(c.name()), "duplicate part name: " + c.name());
            assertTrue(LEGAL_PATH.matcher(c.name()).matches(), "illegal ResourceLocation path: " + c.name());
            assertTrue(manifestSources().has(c.source()), "unknown source: " + c.source());
            assertTrue(c.w() > 0 && c.h() > 0, "degenerate rect: " + c.name());
        }
        for (var key : manifestSources().keySet()) {
            String path = manifestSources().getAsJsonObject(key).get("path").getAsString();
            assertTrue(!path.contains("terrafirmacraft"),
                "TFC domain is not a permitted crop source (2026-09-29 ruling): " + path);
        }
    }

    /** Pin b: every part on disk = manifest sha + dims + a mirrored GT6GuiParts constant + real pixels. */
    @Test
    void everyPartOnDiskMatchesManifestShaDimsAndConstants() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Crop c : manifestParts()) {
            Path file = partPath(c.name());
            if (!Files.isRegularFile(file)) {
                violations.add("missing: " + c.name());
                continue;
            }
            byte[] raw = Files.readAllBytes(file);
            if (!sha256(raw).equals(c.sha256())) {
                violations.add("sha drift: " + c.name());
            }
            BufferedImage img = ImageIO.read(file.toFile());
            if (img.getWidth() != c.w() || img.getHeight() != c.h()) {
                violations.add("dims " + img.getWidth() + "x" + img.getHeight() + " != manifest "
                    + c.w() + "x" + c.h() + ": " + c.name());
            }
            if (countOpaque(img) < 16) {
                violations.add("empty/transparent sprite: " + c.name());
            }
        }
        assertTrue(violations.isEmpty(), "part-file violations: " + violations);

        // constants mirror: every manifest part has exactly one constant of the same
        // name + dims, and vice versa (the 1:1 is what makes the class the truth source)
        Set<String> manifestNames = new HashSet<>();
        for (Crop c : manifestParts()) {
            manifestNames.add(c.name());
        }
        Set<String> constantNames = new HashSet<>();
        for (GuiPart p : GT6GuiParts.ALL) {
            constantNames.add(p.fileName());
            Crop c = manifestParts().stream().filter(m -> m.name().equals(p.fileName())).findFirst().orElse(null);
            assertNotNull(c, "GT6GuiParts constant without manifest row: " + p.fileName());
            assertEquals(p.width(), c.w(), "width drift: " + p.fileName());
            assertEquals(p.height(), c.h(), "height drift: " + p.fileName());
            assertEquals("gt6", p.texture().getNamespace(), "namespace drift: " + p.fileName());
            assertEquals("textures/gui/parts/" + p.fileName(), p.texture().getPath(), "path drift: " + p.fileName());
            assertTrue(LEGAL_PATH.matcher(p.texture().getPath()).matches(), "illegal texture path: " + p.fileName());
        }
        assertEquals(manifestNames, constantNames, "GT6GuiParts.ALL does not mirror the manifest 1:1");
    }

    /** Pin c: the panel is the 9-slice source — the border pads are part of the contract. */
    @Test
    void panelBackgroundCarriesTheNineSliceBorders() {
        assertEquals(176, GT6GuiParts.PANEL_BACKGROUND.width());
        assertEquals(166, GT6GuiParts.PANEL_BACKGROUND.height());
        assertEquals(4, GT6GuiParts.PANEL_BACKGROUND.borderLeft());
        assertEquals(4, GT6GuiParts.PANEL_BACKGROUND.borderTop());
        assertEquals(4, GT6GuiParts.PANEL_BACKGROUND.borderRight());
        assertEquals(4, GT6GuiParts.PANEL_BACKGROUND.borderBottom());
    }

    /**
     * Pin d: provenance replay — for every committed source fixture, the fixture
     * hash-matches its manifest entry and every rect cropped from it is PIXEL-identical
     * to the shipped part (byte-identity is the python --verify face; pixels are the
     * PNG-encoder-independent invariant).
     */
    @Test
    void provenanceRectsReplayPixelIdenticalFromCommittedFixtures() throws IOException {
        int replays = 0;
        List<String> violations = new ArrayList<>();
        for (String source : FIXTURE_SOURCES) {
            byte[] fixtureRaw = readAll(GT6GuiPartsDatagenTest.class
                .getResourceAsStream("/gregtech6/guiparts/source/" + source));
            String fixtureSha = sha256(fixtureRaw);
            String manifestSha = manifestSources().getAsJsonObject(source).get("sha256").getAsString();
            assertEquals(manifestSha, fixtureSha, "fixture/source sha mismatch: " + source);

            BufferedImage fixture = ImageIO.read(new java.io.ByteArrayInputStream(fixtureRaw));
            for (Crop c : manifestParts()) {
                if (!c.source().equals(source)) {
                    continue;
                }
                BufferedImage expect = fixture.getSubimage(c.x(), c.y(), c.w(), c.h());
                BufferedImage actual = ImageIO.read(partPath(c.name()).toFile());
                for (int y = 0; y < c.h(); y++) {
                    for (int x = 0; x < c.w(); x++) {
                        if (expect.getRGB(x, y) != actual.getRGB(x, y)) {
                            violations.add("rect replay mismatch at (" + x + "," + y + "): " + c.name()
                                + " <- " + source + " " + c.x() + "," + c.y());
                            y = c.h();
                            break;
                        }
                    }
                }
                replays++;
            }
        }
        assertEquals(PINNED_PART_TOTAL, replays, "every part must replay from a committed fixture");
        assertTrue(violations.isEmpty(), "provenance replay violations: " + violations);
    }

    /** Pin e: the parts domain is a static-tree borrow, never a datagen product. */
    @Test
    void partsNeverLandInTheGeneratedTree() {
        assertTrue(!Files.exists(mdkRoot().resolve(GENERATED_TREE).resolve(PARTS_PREFIX)),
            "gui/parts must stay out of the datagen product tree");
        assertTrue(Files.isDirectory(mdkRoot().resolve(STATIC_TREE).resolve(PARTS_PREFIX)),
            "static parts dir missing");
    }
}
