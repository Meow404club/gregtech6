/*
 * Offline pinned tests for task boiler-barometer: the 33 borrowed upstream barometer
 * gauge PNGs (the BI.BAROMETER dial + the 32 BAROMETER_SCALE needle states) exist
 * byte-identical to the README sha256 ledger rows, and the generated atlas JSON carries
 * exactly the one directory source the GTBoilerBarometerModel consumes (the
 * ore-overlay no-model-JSON precedent — the atlas source IS the consumer-side
 * stitching). Pure filesystem/JSON assertions, no bootstrap.
 */
package gregtech6.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class GT6BoilerBarometerDatagenTest {

    /** The README ledger section's entry shape: - `block/barometer/NAME.png` — `HEX` (upstream ...). */
    private static final Pattern LEDGER_ROW =
            Pattern.compile("- `block/barometer/([a-z0-9]+)\\.png` — `([0-9a-f]{64})` \\(upstream `overlays/barometer/\\1\\.png`\\)");

    /** The pinned gauge census: the base dial + the 32 zero-padded needle states. */
    private static final int PINNED_GAUGE_TOTAL = 33;

    /** The mdk project root, walking up from the (leg-dependent) test working dir (mdk/tools anchor). */
    private static Path mdkRoot() {
        for (Path p = Path.of("").toAbsolutePath(); p != null; p = p.getParent()) {
            if (Files.isRegularFile(p.resolve("tools").resolve("gen_textures.py"))) {
                return p;
            }
        }
        throw new AssertionError("mdk root (tools/gen_textures.py) not found upward from "
                + Path.of("").toAbsolutePath());
    }

    /** The static assets tree inside the mdk root. */
    private static Path assetsRoot() {
        return mdkRoot().resolve("src/main/resources/assets");
    }

    private static InputStream resource(String aPath) {
        return GT6BoilerBarometerDatagenTest.class.getClassLoader().getResourceAsStream(aPath);
    }

    /** The ledger rows of the boiler-barometer README section, path-suffix → sha256. */
    private static Map<String, String> ledgerRows() throws Exception {
        Path tReadme = assetsRoot().resolve("README.md");
        Map<String, String> rRows = new LinkedHashMap<>();
        for (String tLine : Files.readAllLines(tReadme, StandardCharsets.UTF_8)) {
            Matcher tMatch = LEDGER_ROW.matcher(tLine.trim());
            if (tMatch.matches()) {
                rRows.put(tMatch.group(1), tMatch.group(2));
            }
        }
        return rRows;
    }

    /** The 33 PNGs exist under the static tree and hash to the README ledger rows. */
    @Test
    void borrowedBarometerGaugePngsMatchTheLedger() throws Exception {
        Map<String, String> tLedger = ledgerRows();
        assertEquals(PINNED_GAUGE_TOTAL, tLedger.size(), "the ledger carries base + 00..31");
        assertTrue(tLedger.containsKey("base"), "the dial row");
        for (int i = 0; i < 32; i++) {
            assertTrue(tLedger.containsKey(String.format("%02d", i)), "the needle row " + i);
        }
        MessageDigest tDigest = MessageDigest.getInstance("SHA-256");
        for (Map.Entry<String, String> tRow : tLedger.entrySet()) {
            Path tPng = assetsRoot().resolve("gt6/textures/block/barometer/" + tRow.getKey() + ".png");
            assertTrue(Files.isRegularFile(tPng), "the borrowed PNG exists: " + tRow.getKey());
            String tHash = HexFormat.of().formatHex(tDigest.digest(Files.readAllBytes(tPng)));
            assertEquals(tRow.getValue(), tHash, "byte-identical to the upstream snapshot: " + tRow.getKey());
        }
    }

    /**
     * The generated atlas carries EXACTLY the one directory source — no 33 single-file
     * duplicates (the DirectoryLister maps the whole folder in one source,
     * GT6Atlases addAtlasSources). Read from DISK (the GT6AssetCoverageGuardTest
     * mdkRoot convention): the classloader lookup resolves this path to the VANILLA
     * atlas inside the neoforge runtime jar first on the neo leg (TransformingClassLoader
     * ordering — the FML probe evidence), so a classpath read asserts the wrong file.
     */
    @Test
    void atlasCarriesExactlyOneBarometerDirectorySource() throws Exception {
        Path tAtlas = mdkRoot().resolve("src/generated/resources/assets/minecraft/atlases/blocks.json");
        assertTrue(Files.isRegularFile(tAtlas), "the generated atlas JSON must be committed: " + tAtlas);
        String tRaw = Files.readString(tAtlas, StandardCharsets.UTF_8);
        JsonObject tAtlasJson = JsonParser.parseString(tRaw).getAsJsonObject();
        int tDirectorySources = 0, tSingleSources = 0;
        for (var tElement : tAtlasJson.getAsJsonArray("sources")) {
            JsonObject tSource = tElement.getAsJsonObject();
            String tType = tSource.has("type") ? tSource.get("type").getAsString() : "";
            String tResource = tSource.has("resource") ? tSource.get("resource").getAsString() : "";
            if (tResource.startsWith("gt6:block/barometer/")) {
                tSingleSources++; // a per-file SingleFile entry would be the 33-row bloat
            }
            // the 1.21.1 leg serialises the directory id un-prefixed ("directory"), the
            // 1.20.1 leg as "minecraft:directory" — same AtlasSourceSerializer, both accepted
            if ((tType.equals("directory") || tType.equals("minecraft:directory"))
                    && "block/barometer".equals(tSource.get("source").getAsString())) {
                tDirectorySources++;
                assertEquals("block/barometer", tSource.get("prefix").getAsString(),
                        "the DirectoryLister prefix maps textures/<source>/<f>.png to <prefix>/<f>");
            }
        }
        assertEquals(1, tDirectorySources, "exactly one directory source");
        assertEquals(0, tSingleSources, "no per-file single sources");
    }

    /** The static texture folder holds exactly the 33 ledger files (no strays). */
    @Test
    void barometerFolderCarriesExactlyTheLedgerFiles() throws Exception {
        List<String> tOnDisk;
        try (var tWalk = Files.list(assetsRoot().resolve("gt6/textures/block/barometer"))) {
            tOnDisk = tWalk.map(p -> p.getFileName().toString()).sorted().toList();
        }
        assertEquals(PINNED_GAUGE_TOTAL, tOnDisk.size(), "33 files, no strays");
        assertTrue(tOnDisk.contains("base.png"));
        assertTrue(tOnDisk.contains("00.png") && tOnDisk.contains("31.png"), "the zero-padded bounds");
    }
}
