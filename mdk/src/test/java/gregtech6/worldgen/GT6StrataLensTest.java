/**
 * Tests for task p31-strata-lens: the 5-row marker-stone lens table, the exactly-one
 * origin draw, the codec roundtrip, the ellipsoid shape pin and the per-chunk slice
 * tiling — the acceptance's offline audit unit (the GT6LargeVeinTest posture).
 *
 * <p>Compile anchors (transcribed independently here, production and test must agree
 * or a conscious decision is forced): decisions.2026-09-17-p30-strata-ruling (the option-b
 * ruling: 5 marker stones as mountain-scale lenses, the other 12 keep the blob),
 * research.p30-w6-vein-boundary (the origin-grid deterministic recompute + the
 * origin-seeded stream, the strengthening the lens core inherits wholesale),
 * GT6WorldgenDatagen.STRATA_LENS_TABLE (the clean calibration — the conflict-audit
 * ORE_SIZE lesson: no P30 curve reused).
 *
 * <p>The tiling test IS the offline leg of the cross-chunk bit-consistency acceptance:
 * per-chunk slices over the ±3-chunk window must be pairwise disjoint and must
 * concatenate to the full-lens clip — the lens core is a pure function of three drawn
 * parameters, so the proof is exact (no stream-hygiene subtlety at all).
 *
 * <p>Offline-safe by construction: initMaterials + the vanilla bootstrap bracket only;
 * no vanilla Feature class is touched (the GT6LensGenerator/GTLensConfig pair is
 * Feature-free, the entity-registry clinit trap stays untriggered).
 */
package gregtech6.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.mojang.serialization.JsonOps;

import gregtech6.datagen.GT6WorldgenDatagen;
import gregtech6.registry.GTMaterialItems;
import gregtech6.registry.GTStoneBlocks;
import gregtech6.worldgen.GT6LensGenerator.LensSink;

class GT6StrataLensTest {

    /** The card spec's settled marker-stone list, spec order. */
    private static final List<String> SPEC_ORDER =
            List.of("marble", "basalt", "kimberlite", "granite_red", "komatiite");

    /** The card's fixed RCON probe seed — the same one live. */
    private static final long SEED = 6131000569321125127L;

    @BeforeAll
    static void boot() {
        // the material system before GTStoneBlocks.STONES dereferences its suppliers; the
        // vanilla bootstrap bracket for the codec/JsonOps classes (offline throwables ignored)
        GTMaterialItems.initMaterials();
        // the GTOfflineRenderTestBase recipe: the version detect must precede bootStrap — a bare-JVM first boot poisons DataFixers for every later suite in this JVM (the run-order lottery)
        net.minecraft.SharedConstants.tryDetectVersion();
        try {
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {
        }
    }

    /** The 5-row census: spec order, registered stones, sane domains, the radius cap = the scan-safety rail. */
    @Test
    void lensTableCensusIsPinned() {
        List<GTLensConfig> tTable = GT6WorldgenDatagen.STRATA_LENS_TABLE;
        assertEquals(5, tTable.size(), "the card spec pin: exactly the 5 settled marker stones");
        int tWeightSum = 0;
        for (int i = 0; i < SPEC_ORDER.size(); i++) {
            GTLensConfig tLens = tTable.get(i);
            assertEquals(SPEC_ORDER.get(i), tLens.stone(), "row " + i + " must stay in spec order");
            assertTrue(GTStoneBlocks.STONES.stream().anyMatch(tStone -> tStone.snake().equals(tLens.stone())),
                    tLens.stone() + " must be a registered GT stone (registration increment zero)");
            assertTrue(tLens.rarity() > 0, tLens.stone() + " must carry a positive rarity weight");
            assertTrue(tLens.maxY() > tLens.minY(), tLens.stone() + " must leave nextInt(maxY-minY+1) >= 1");
            assertTrue(tLens.halfHeight() >= 1, tLens.stone() + " must carry a positive half-height");
            tWeightSum += tLens.rarity();
        }
        assertEquals(21, tWeightSum, "the draw mass of the 5 rows");
        for (GTLensConfig tLens : tTable) {
            assertTrue(tLens.radius() >= 1 && tLens.radius() <= GTLensConfig.RADIUS_CODEC_CAP,
                    tLens.stone() + " radius must ride the codec cap");
        }
        // the scan-window inequality: radius cap 48 + a center anywhere in the origin
        // chunk (<= 15) stays under 4 chunks, so the Feature's +/-3-chunk window sees
        // every origin whose lens can touch the work chunk
        assertTrue(GTLensConfig.RADIUS_CODEC_CAP + 15 < 16 * (GT6StrataLensFeature.SCAN_RADIUS_CHUNKS + 1),
                "the +/-3-chunk scan window must mathematically cover the max lens reach");
    }

    /** The blob band shrinks to 12 and partitions STONES with the lens band (no double-generation). */
    @Test
    void blobBandShrinksAndPartitionsStones() {
        assertEquals(12, GT6Worldgen.BLOB_STONES.size(), "the option-b ruling: the other 12 stones keep the blob");
        assertEquals(12, GT6Worldgen.CONFIGURED_KEYS.size(), "12 blob configured keys");
        assertEquals(12, GT6Worldgen.PLACED_KEYS.size(), "12 blob placed keys");
        assertEquals(12, GT6WorldgenDatagen.BIOME_MODIFIER_KEYS.size(), "12 blob biome modifiers");
        List<String> tBlob = GT6Worldgen.BLOB_STONES.stream().map(GTStoneBlocks.StoneSpec::snake).toList();
        Set<String> tUnion = new HashSet<>(tBlob);
        assertTrue(tUnion.addAll(GT6Worldgen.LENS_STONE_SNAKES), "the bands must be disjoint (a marker stone in both = double generation)");
        assertEquals(tUnion.size(), tBlob.size() + GT6Worldgen.LENS_STONE_SNAKES.size(), "disjoint partition");
        Set<String> tStones = new HashSet<>();
        GTStoneBlocks.STONES.forEach(tStone -> tStones.add(tStone.snake()));
        assertEquals(tStones, tUnion, "BLOB + LENS must cover the 17-stone universe exactly");
    }

    /** The lattice predicate keeps the phase form with the widened modulus; the draw replays identically. */
    @Test
    void latticeAndDrawReplayIsDeterministic() {
        // the phase form: (cc + 402653184) % 9 == 1 — cells at cc = 4, 13, ... and -5, -14, ...
        assertTrue(GT6LensGenerator.isLensOriginCell(4));
        assertTrue(GT6LensGenerator.isLensOriginCell(13));
        assertTrue(GT6LensGenerator.isLensOriginCell(-5));
        assertFalse(GT6LensGenerator.isLensOriginCell(0));
        assertFalse(GT6LensGenerator.isLensOriginCell(1), "the modulus widened: the vein cell 1 is not a lens cell");
        assertTrue(GT6VeinGenerator.isOriginCell(4), "every lens cell is also a vein cell (9 is a multiple of 3)");
        assertTrue(GT6VeinGenerator.isOriginCell(13), "and the subset is proper-strict: 13 is a vein AND a lens cell (1 of 3 vein cells)");

        GTLensConfig.Table tTable = new GTLensConfig.Table(GT6WorldgenDatagen.STRATA_LENS_TABLE);
        GTLensConfig tFirst = GT6LensGenerator.drawLens(tTable, GT6VeinGenerator.veinRandom(SEED, 0, 4, 4));
        assertNotNull(tFirst, "the exactly-one draw must pick a row (weights sum 21)");
        GTLensConfig tSecond = GT6LensGenerator.drawLens(tTable, GT6VeinGenerator.veinRandom(SEED, 0, 4, 4));
        assertEquals(tFirst, tSecond, "the same (seed, origin) must draw the same row");
        assertTrue(SPEC_ORDER.contains(tFirst.stone()), "the draw must land on a table row");
    }

    /** The same (seed, origin) recomputes the identical slice — the acceptance's determinism face. */
    @Test
    void sliceReplayIsDeterministic() {
        RecordingSink tFirst = sliceForOrigin(SEED, 4, 4);
        RecordingSink tSecond = sliceForOrigin(SEED, 4, 4);
        assertEquals(tFirst.stones, tSecond.stones, "the same (seed, origin) must recompute the identical lens slice");
        assertTrue(tFirst.stones.size() > 0, "a lens origin at this seed actually places (the probe face)");
        // a second lattice cell also places (the grid face, not a one-cell fluke)
        assertTrue(GT6LensGenerator.isLensOriginCell(13));
        assertTrue(sliceForOrigin(SEED, 13, 4).stones.size() > 0, "a second lens origin also places");
    }

    /**
     * THE cross-chunk proof: per-chunk slices over the +/-3-chunk window are disjoint and
     * tile the full-lens clip — two chunks deriving the same lens never double-write and
     * never leave a seam (the acceptance's no-1-column-seam face, offline).
     */
    @Test
    void perChunkSlicesTileTheFullLens() {
        int tOriginX = 4, tOriginZ = 4;
        List<Pos> tFull = sliceForOrigin(SEED, tOriginX, tOriginZ).stones;

        List<Pos> tUnion = new ArrayList<>();
        for (int tWX = tOriginX - GT6StrataLensFeature.SCAN_RADIUS_CHUNKS;
                tWX <= tOriginX + GT6StrataLensFeature.SCAN_RADIUS_CHUNKS; tWX++) {
            for (int tWZ = tOriginZ - GT6StrataLensFeature.SCAN_RADIUS_CHUNKS;
                    tWZ <= tOriginZ + GT6StrataLensFeature.SCAN_RADIUS_CHUNKS; tWZ++) {
                RecordingSink tSlice = new RecordingSink();
                generateAt(SEED, tOriginX, tOriginZ, tSlice, tWX << 4, (tWX << 4) + 15, tWZ << 4, (tWZ << 4) + 15);
                for (Pos tPos : tSlice.stones) {
                    assertFalse(tUnion.contains(tPos), "a block written by two work chunks = a double-write seam");
                    assertTrue(tPos.x >= (tWX << 4) && tPos.x <= ((tWX << 4) + 15), "the slice stayed inside its own chunk (x)");
                    assertTrue(tPos.z >= (tWZ << 4) && tPos.z <= ((tWZ << 4) + 15), "the slice stayed inside its own chunk (z)");
                    tUnion.add(tPos);
                }
            }
        }
        assertEquals(tFull.size(), tUnion.size(), "the chunk clips must cover every full-lens block exactly once");
        List<Pos> tRemaining = new ArrayList<>(tFull);
        tRemaining.removeAll(tUnion);
        assertTrue(tRemaining.isEmpty(), "union content != full clip");
    }

    /**
     * The shape pin (the conflict-audit ORE_SIZE lesson, the clean-calibration proof):
     * the slice IS the flattened-blob ellipsoid of the drawn parameters — every placed
     * block satisfies the inequality and every inequality block in the bounding box is
     * placed (no drift, no leftover curve).
     */
    @Test
    void sliceIsTheCleanEllipsoidOfTheDrawnParameters() {
        int tOriginX = 4, tOriginZ = 4;
        Random tRandom = GT6VeinGenerator.veinRandom(SEED, 0, tOriginX, tOriginZ); // the overworld salt
        GTLensConfig tLens = GT6LensGenerator.drawLens(new GTLensConfig.Table(GT6WorldgenDatagen.STRATA_LENS_TABLE), tRandom);
        int tCenterY = tLens.minY() + tRandom.nextInt(tLens.maxY() - tLens.minY() + 1);
        int tCenterX = (tOriginX << 4) + tRandom.nextInt(16);
        int tCenterZ = (tOriginZ << 4) + tRandom.nextInt(16);

        Set<Pos> tPlaced = new HashSet<>(sliceForOrigin(SEED, tOriginX, tOriginZ).stones);
        Set<Pos> tExpected = new HashSet<>();
        for (int tX = tCenterX - tLens.radius(); tX <= tCenterX + tLens.radius(); tX++) {
            for (int tZ = tCenterZ - tLens.radius(); tZ <= tCenterZ + tLens.radius(); tZ++) {
                double tHoriz = (double) ((long) (tX - tCenterX) * (tX - tCenterX)
                        + (long) (tZ - tCenterZ) * (tZ - tCenterZ)) / ((double) tLens.radius() * tLens.radius());
                if (tHoriz > 1.0) continue;
                int tReach = (int) Math.floor(tLens.halfHeight() * Math.sqrt(1.0 - tHoriz));
                for (int tY = tCenterY - tReach; tY <= tCenterY + tReach; tY++) {
                    tExpected.add(new Pos(tX, tY, tZ));
                }
            }
        }
        assertEquals(tExpected, tPlaced, "the slice must be exactly the ellipsoid of the drawn parameters");
        assertTrue(tExpected.size() >= 100, "the lens is mountain-scale, not a pebble (the drawn shape face)");
    }

    /**
     * The adjacent-chunk origin-set face (the acceptance formulation): two adjacent work
     * chunks each walk their own scan window; the shared origins must derive identical
     * parameters from either walk (the pure-coordinate predicate + the per-origin seeded
     * stream — no walk-order or scan-window state can leak into a slice).
     */
    @Test
    void adjacentChunksDeriveSharedOriginsIdentically() {
        // the windows of (4,4) and (5,4) share the lattice cell (4,4) (cells sit at cc = 4 mod 9)
        Set<String> tFromA = sharedOriginSlices(4, 4, 5, 4);
        Set<String> tFromB = sharedOriginSlices(5, 4, 4, 4);
        assertEquals(tFromA, tFromB, "the shared origins must derive identical slices from either chunk's walk");
        assertFalse(tFromA.isEmpty(), "two adjacent windows must share at least one origin cell");
    }

    /** The full-lens slice fingerprints of the origins shared by two work chunks' scan windows. */
    private static Set<String> sharedOriginSlices(int aWorkAX, int aWorkAZ, int aWorkBX, int aWorkBZ) {
        Set<String> rFingerprints = new HashSet<>();
        for (int tDX = -GT6StrataLensFeature.SCAN_RADIUS_CHUNKS; tDX <= GT6StrataLensFeature.SCAN_RADIUS_CHUNKS; tDX++) {
            for (int tDZ = -GT6StrataLensFeature.SCAN_RADIUS_CHUNKS; tDZ <= GT6StrataLensFeature.SCAN_RADIUS_CHUNKS; tDZ++) {
                int tOriginX = aWorkAX + tDX, tOriginZ = aWorkAZ + tDZ;
                if (!GT6LensGenerator.isLensOriginCell(tOriginX) || !GT6LensGenerator.isLensOriginCell(tOriginZ)) continue;
                if (Math.abs(tOriginX - aWorkBX) > GT6StrataLensFeature.SCAN_RADIUS_CHUNKS
                        || Math.abs(tOriginZ - aWorkBZ) > GT6StrataLensFeature.SCAN_RADIUS_CHUNKS) continue;
                rFingerprints.add(tOriginX + "," + tOriginZ + ":" + sliceForOrigin(SEED, tOriginX, tOriginZ).stones);
            }
        }
        return rFingerprints;
    }

    /** The configured-feature config codec roundtrips through JSON (the datapack face the KJS surface edits). */
    @Test
    void lensTableCodecRoundtripsThroughJson() {
        GTLensConfig.Table tTable = new GTLensConfig.Table(GT6WorldgenDatagen.STRATA_LENS_TABLE);
        var tJson = GTLensConfig.Table.CODEC.encodeStart(JsonOps.INSTANCE, tTable).result().orElseThrow();
        GTLensConfig.Table tBack = GTLensConfig.Table.CODEC.parse(JsonOps.INSTANCE, tJson).result().orElseThrow();
        assertEquals(tTable, tBack, "the codec roundtrip must preserve the whole 5-row table");
        assertEquals(5, tBack.lenses().size());
    }

    // ---------------------------------------------------------------- the harness

    private record Pos(int x, int y, int z) {}

    /** The offline sink: records the stone placements (the pure-geometry calls, host gate excluded). */
    private static final class RecordingSink implements LensSink {
        final List<Pos> stones = new ArrayList<>();

        @Override public void stone(int aX, int aY, int aZ) {
            stones.add(new Pos(aX, aY, aZ));
        }
    }

    /** One full origin pipeline: the WD.random stream (reused verbatim), the weighted draw, the slice into the given clip. */
    private static RecordingSink sliceForOrigin(long aSeed, int aOriginX, int aOriginZ) {
        RecordingSink tSink = new RecordingSink();
        generateAt(aSeed, aOriginX, aOriginZ, tSink, -1_000_000, 1_000_000, -1_000_000, 1_000_000);
        return tSink;
    }

    private static void generateAt(long aSeed, int aOriginX, int aOriginZ, RecordingSink aSink,
            int aClipMinX, int aClipMaxX, int aClipMinZ, int aClipMaxZ) {
        Random tRandom = GT6VeinGenerator.veinRandom(aSeed, 0, aOriginX, aOriginZ); // the overworld salt
        GTLensConfig tLens = GT6LensGenerator.drawLens(new GTLensConfig.Table(GT6WorldgenDatagen.STRATA_LENS_TABLE), tRandom);
        if (tLens == null) return;
        GT6LensGenerator.generateSlice(tLens, tRandom, aOriginX << 4, aOriginZ << 4,
                aClipMinX, aClipMaxX, aClipMinZ, aClipMaxZ, aSink);
    }
}
