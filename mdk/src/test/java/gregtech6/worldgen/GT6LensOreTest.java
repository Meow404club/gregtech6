/**
 * Tests for task c3-lens-ores: the lens companion-ore table (the upstream
 * stone-layer companion rows bound to the 5 marker-stone lenses), the axis gate
 * face, the count formula and the feature keys — the acceptance's offline audit
 * unit (the GT6LargeVeinTest posture).
 *
 * <p>Compile anchors (transcribed independently here, production and test must
 * agree or a conscious decision is forced): Loader_Worldgen.java:225-229 (kimberlite
 * rows), :247-252 (basalt), :288-295 (marble), :359-365 (granite_red), :217-222
 * (komatiite); CS.java:120 (U48 = U/48 ...); StoneLayerOres.java:90 (the
 * {@code nextInt(U) < chance} = 1/N per layer-stone-block face);
 * FeatureSorter.java:52-57 + ChunkGenerator.java:319 (the chain-edge execution
 * order the same-modifier rider relies on).
 *
 * <p>Offline-safe by construction: initMaterials + the vanilla bootstrap bracket only.
 */
package gregtech6.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;
import com.google.gson.JsonObject;

import gregtech6.datagen.GT6WorldgenDatagen;
import gregtech6.datagen.GT6WorldgenDatagen.LensOreRow;
import gregtech6.registry.GTMaterialItems;

class GT6LensOreTest {

    @BeforeAll
    static void boot() {
        // the material system before MT dereferences; the vanilla bootstrap bracket for the
        // codec/JsonOps classes (the GT6LargeVeinTest posture, offline throwables ignored)
        GTMaterialItems.initMaterials();
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
        }
    }

    /** The 22-row census: per-lens counts and order, band sanity, the U-denominator domain. */
    @Test
    void lensOreTableCensusIsPinned() {
        List<LensOreRow> tTable = GT6WorldgenDatagen.LENS_ORE_TABLE;
        assertEquals(22, tTable.size(), "the transcription pin: 22 upstream StoneLayerOres rows over the 5 lenses");
        Map<String, Integer> tPerLens = new LinkedHashMap<>();
        for (LensOreRow tRow : tTable) tPerLens.merge(tRow.lens(), 1, Integer::sum);
        assertEquals(List.of(3, 4, 6, 5, 4), List.copyOf(tPerLens.values()),
                "kimberlite 3 (:225-229) / basalt 4 (:247-252) / marble 6 (:288-295) / granite_red 5 (:359-365) / komatiite 4 (:217-222), upstream order");
        for (LensOreRow tRow : tTable) {
            assertTrue(tRow.minY() >= 0 && tRow.maxY() <= 255,
                    tRow.lens() + " band must stay the verbatim upstream domain");
            assertTrue(tRow.minY() <= tRow.maxY(), tRow.lens() + " band must be ordered");
            assertTrue(tRow.denominator() >= 8 && tRow.denominator() <= 128,
                    tRow.lens() + " chance denominator must be a CS U-token (U8..U128)");
            assertEquals(tRow.maxY() - tRow.minY() + 1, tRow.bandWidth(), "the bandWidth formula");
        }
    }

    /** The verbatim spot pins: one signature row per lens, Y band + denominator + material. */
    @Test
    void signatureRowsAreVerbatim() {
        List<LensOreRow> tTable = GT6WorldgenDatagen.LENS_ORE_TABLE;
        LensOreRow tDiamond = tTable.get(0);
        assertEquals("kimberlite", tDiamond.lens());
        assertEquals(0, tDiamond.minY());
        assertEquals(12, tDiamond.maxY());
        assertEquals(48, tDiamond.denominator());
        assertEquals("Diamond", materialName(tDiamond), ":226 Diamond U48 y0-12 — the kimberlite→diamond bone");
        LensOreRow tChromite = tTable.get(6);
        assertEquals("basalt", tChromite.lens());
        assertEquals(32, tChromite.minY());
        assertEquals(64, tChromite.maxY());
        assertEquals(8, tChromite.denominator());
        assertEquals("Chromite", materialName(tChromite), ":251 Chromite U8 y32-64 — the basalt bone (axis-gated today)");
        LensOreRow tCassiterite = tTable.get(7);
        assertEquals("marble", tCassiterite.lens());
        assertEquals(20, tCassiterite.minY());
        assertEquals(80, tCassiterite.maxY());
        assertEquals(16, tCassiterite.denominator());
        assertEquals("Cassiterite", materialName(tCassiterite), ":289 Cassiterite U16 y20-80");
        LensOreRow tColtan = tTable.get(17);
        assertEquals("granite_red", tColtan.lens());
        assertEquals(20, tColtan.minY());
        assertEquals(50, tColtan.maxY());
        assertEquals(16, tColtan.denominator());
        assertEquals("Coltan", materialName(tColtan), ":364 Coltan U16 y20-50 (the !MD.HBM arm — active on this port)");
        LensOreRow tRedstone = tTable.get(20);
        assertEquals("komatiite", tRedstone.lens());
        assertEquals(0, tRedstone.minY());
        assertEquals(30, tRedstone.maxY());
        assertEquals(8, tRedstone.denominator());
        assertEquals("Redstone", materialName(tRedstone), ":220 Redstone U8 y0-30");
    }

    /**
     * The axis gate face: every generated row's material sits in the GT6OreBlocks
     * material axis (the GT6VeinGenerator.valid precedent) and the generated band
     * keeps table order. The gate is LIVE data, never a frozen drop-list — the
     * axis-extension card (a-ore-axis-extension, B plan ruled) widens the axis
     * and today-gated rows (all four of basalt, kimberlite's two gems, marble's
     * Stannite/Kesterite, granite_red's uranium family, komatiite's magnesite) start
     * generating with zero edits here, so no assertion names a
     * not-currently-generated row: merge-order agnostic against that card
     * (coordinator notice 2026-09-28).
     */
    @Test
    void axisGateFaceIsPinned() {
        List<LensOreRow> tTable = GT6WorldgenDatagen.LENS_ORE_TABLE;
        List<LensOreRow> tGenerated = GT6WorldgenDatagen.lensOreRows();
        int tTableIndex = 0;
        for (LensOreRow tRow : tGenerated) {
            assertTrue(GT6WorldgenDatagen.lensOreValid(tRow),
                    tRow.lens() + " generates, so its material must sit in the axis");
            while (tTableIndex < tTable.size() && tTable.get(tTableIndex) != tRow) tTableIndex++;
            assertTrue(tTableIndex < tTable.size(), "generated rows keep table order");
            tTableIndex++;
        }
        assertEquals(22, tTable.size(), "the transcription still ships the FULL upstream table as data");
    }

    /** The generated (lens, material) pairs: the 9 SPEC rows all generate — every one of
     * them is an axis member today and stays one after the r7-a widening, so this
     * membership form holds under either merge order (the width/roster of the rest of
     * the generated band is the axis's business, not this card's). */
    @Test
    void generatedPairsArePinned() {
        List<String> tPairs = new ArrayList<>();
        for (LensOreRow tRow : GT6WorldgenDatagen.lensOreRows()) tPairs.add(tRow.lens() + "/" + materialName(tRow));
        for (String tSpec : List.of("kimberlite/Diamond",
                "marble/Cassiterite", "marble/Sphalerite", "marble/Chalcopyrite", "marble/Pyrite",
                "granite_red/Coltan",
                "komatiite/Cinnabar", "komatiite/Redstone", "komatiite/Pyrite")) {
            assertTrue(tPairs.contains(tSpec), "the SPEC row must generate: " + tSpec);
        }
    }

    /**
     * The count formula: {@code max(1, round(bandWidth / (N × 1.5)))} — the relative
     * frequencies stay upstream (bandWidth/N), the absolute scale is the declared
     * lens-form compromise (the class javadoc's calibration knob).
     */
    @Test
    void countFormulaIsPinned() {
        for (LensOreRow tRow : GT6WorldgenDatagen.lensOreRows()) {
            int tExpected = Math.max(1, Math.round(tRow.bandWidth() / (tRow.denominator() * 1.5f)));
            assertEquals(tExpected, GT6WorldgenDatagen.lensOreCount(tRow), tRow.lens() + "/" + materialName(tRow));
        }
        // the spot pins across the range: diamond collapses to 1, the wide-band rows climb
        LensOreRow tTable0 = GT6WorldgenDatagen.LENS_ORE_TABLE.get(0);
        assertEquals(1, GT6WorldgenDatagen.lensOreCount(tTable0), "diamond 13/(48×1.5) → the 1 floor");
        LensOreRow tCassiterite = GT6WorldgenDatagen.LENS_ORE_TABLE.get(7);
        assertEquals(3, GT6WorldgenDatagen.lensOreCount(tCassiterite), "cassiterite 61/(16×1.5) → 3");
        LensOreRow tRedstone = GT6WorldgenDatagen.LENS_ORE_TABLE.get(20);
        assertEquals(3, GT6WorldgenDatagen.lensOreCount(tRedstone), "redstone 31/(8×1.5) → 3");
    }

    /** The feature-key face: {@code ore_lens/<lens>_<material-snake>}, configured == placed
     * path, all distinct, the 9 SPEC keys present (the set's width rides the axis, as in
     * generatedPairsArePinned). */
    @Test
    void keyFaceIsPinned() {
        List<String> tPaths = new ArrayList<>();
        for (LensOreRow tRow : GT6WorldgenDatagen.lensOreRows()) {
            String tPath = GT6WorldgenDatagen.lensOreConfiguredKey(tRow).location().getPath();
            assertEquals(tPath, GT6WorldgenDatagen.lensOrePlacedKey(tRow).location().getPath(),
                    "the placed key shares the configured path (the small-ore form)");
            assertTrue(tPaths.add(tPath), "key must be distinct: " + tPath);
        }
        for (String tSpec : List.of("ore_lens/kimberlite_diamond",
                "ore_lens/marble_cassiterite", "ore_lens/marble_sphalerite", "ore_lens/marble_chalcopyrite",
                "ore_lens/marble_pyrite", "ore_lens/granite_red_coltan",
                "ore_lens/komatiite_cinnabar", "ore_lens/komatiite_redstone", "ore_lens/komatiite_pyrite")) {
            assertTrue(tPaths.contains(tSpec), "the SPEC key must exist: " + tSpec);
        }
    }

    /**
     * The generated biome-modifier JSON face (the load-bearing ORDER pin): the
     * strata_lenses modifier's feature list = [strata_lenses, ...companions] — the
     * list order IS the FeatureSorter chain edge that guarantees the ores scan host
     * blocks the lens placed (FeatureSorter.java:52-57). The companion tail mirrors
     * the generated-row set exactly (no size pin: the r7-a axis widening grows both
     * sides together on the next datagen run — merge-order agnostic).
     */
    @Test
    void strataLensModifierShipsLensThenCompanions() throws Exception {
        List<String> tGeneratedKeys = new ArrayList<>();
        for (LensOreRow tRow : GT6WorldgenDatagen.lensOreRows()) {
            tGeneratedKeys.add(GT6WorldgenDatagen.lensOrePlacedKey(tRow).location().getPath());
        }
        for (String tBrand : new String[] {"forge", "neoforge"}) {
            JsonObject tRow = resourceJson("data/gt6/" + tBrand + "/biome_modifier/strata_lenses.json");
            assertTrue(tRow.get("features").isJsonArray(), tBrand + ": the modifier must ship a feature LIST now");
            List<String> tFeatures = new ArrayList<>();
            tRow.get("features").getAsJsonArray().forEach(tElement -> tFeatures.add(tElement.getAsString()));
            assertEquals("gt6:strata_lenses", tFeatures.get(0), tBrand + ": the lens feature rides FIRST");
            assertTrue(tFeatures.contains("gt6:ore_lens/kimberlite_diamond"),
                    tBrand + ": the diamond-pipe bone must ride the modifier");
            List<String> tCompanions = new ArrayList<>(tFeatures.stream()
                    .filter(tFeature -> tFeature.startsWith("gt6:ore_lens/"))
                    .map(tFeature -> tFeature.substring("gt6:".length())).toList());
            assertEquals(tGeneratedKeys, tCompanions,
                    tBrand + ": the modifier's ore_lens tail == the generated row set, table order");
        }
        for (String tPath : new String[] {"ore_lens/kimberlite_diamond", "ore_lens/marble_cassiterite",
                "ore_lens/komatiite_pyrite"}) {
            assertNotNull(resourceJson("data/gt6/worldgen/configured_feature/" + tPath + ".json"));
            assertNotNull(resourceJson("data/gt6/worldgen/placed_feature/" + tPath + ".json"));
        }
    }

    /**
     * The ordinary small-ore band's order pin (task lens-ore-base-order): the band
     * (overworld surface pairs then the deep-band mirrors) rides the strata_lenses
     * modifier as the exact tail AFTER the lens+companion head — the one-modifier list
     * order is the FeatureSorter chain (FeatureSorter.java:52-57 consecutive pairs →
     * ChunkGenerator.java:319), so the band's marble-family target arm always sees lens
     * stone the lens already placed. The standalone ore_small_overworld modifier must
     * stay gone from both brands: its own-modifier landing order was the uncontracted
     * datapack load order, the user-reported stone-base-in-marble bug.
     */
    @Test
    void smallOreBandRidesTheLensChainAfterTheLens() throws Exception {
        List<String> tExpectedSmall = new ArrayList<>();
        for (GTOreWorldgen.Placement tPair : GTOreWorldgen.placementPairs()) {
            if (tPair.dim() == GTOreWorldgen.Dim.OVERWORLD) {
                tExpectedSmall.add(GTOreWorldgen.placedKey(tPair.row(), tPair.dim()).location().toString());
            }
        }
        for (GTOreWorldgen.SmallOreRow tRow : GTOreWorldgen.deepMirrorRows()) {
            tExpectedSmall.add(GTOreWorldgen.deepPlacedKey(tRow).location().toString());
        }
        assertTrue(tExpectedSmall.size() > 0, "the overworld small-ore face is non-empty");
        for (String tBrand : new String[] {"forge", "neoforge"}) {
            JsonObject tRow = resourceJson("data/gt6/" + tBrand + "/biome_modifier/strata_lenses.json");
            List<String> tFeatures = new ArrayList<>();
            tRow.get("features").getAsJsonArray().forEach(tElement -> tFeatures.add(tElement.getAsString()));
            assertEquals("gt6:strata_lenses", tFeatures.get(0), tBrand + ": the lens feature rides FIRST");
            int tFirstSmall = tFeatures.indexOf(tExpectedSmall.get(0));
            assertTrue(tFirstSmall > 0, tBrand + ": the small-ore band is present in the chain");
            assertEquals(tExpectedSmall, tFeatures.subList(tFirstSmall, tFeatures.size()),
                    tBrand + ": the small-ore band is the exact tail, surface-then-deep order");
            // the whole companion head precedes the band: the last ore_lens entry < the band start
            int tLastLens = -1;
            for (int i = 0; i < tFeatures.size(); i++) {
                if (tFeatures.get(i).startsWith("gt6:ore_lens/")) tLastLens = i;
            }
            assertTrue(tLastLens < tFirstSmall, tBrand + ": every companion precedes the small-ore band");
            // the standalone overworld modifier stays gone
            assertNull(GT6LensOreTest.class.getClassLoader()
                    .getResourceAsStream("data/gt6/" + tBrand + "/biome_modifier/ore_small_overworld.json"),
                    tBrand + ": the standalone ore_small_overworld modifier stays deleted");
        }
    }

    private static String materialName(LensOreRow aRow) {
        return GT6WorldgenDatagen.lensOreResolve(aRow).mNameInternal;
    }

    private static JsonObject resourceJson(String aPath) throws Exception {
        try (InputStream tStream = GT6LensOreTest.class.getClassLoader().getResourceAsStream(aPath)) {
            assertNotNull(tStream, aPath + " must ship on the classpath");
            return JsonParser.parseString(new String(tStream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
