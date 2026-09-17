package gregtech6.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The strata-lens deterministic core (task p31-strata-lens) — the {@link GT6VeinGenerator}
 * isomorph: the same origin-grid + origin-seeded-stream + exactly-one-weighted-draw
 * scheme, re-formed for the 5-row marker-stone lens table and the flattened-blob
 * (mountain-scale ellipsoid) shape. Kept vanilla-free and Feature-free exactly like the
 * vein core so the offline tests drive the math headless (the GT6LargeVeinTest posture).
 *
 * <p><b>The grid</b>: the {@link GT6VeinGenerator#ORIGIN_PHASE} verbatim with the modulus
 * widened 3 &gt; 9 — mountain-scale landmarks sit on a 9x9-chunk lattice (one lens origin
 * per 144 chunks), the {@code (cc + PHASE) % 9 == 1} isomorphic form of
 * GT6WorldGenerator.java:97. Every lens origin is also a vein origin (9 is a multiple of
 * 3, 1 % 3 == 1) — a lens host may carry a large vein, the vein's host-skin semantics
 * already map GT stone hosts (GT6OreBlocks.stoneToOreFamilies), upstream's
 * stoneToNormalOres cut ores through stone layers the same way. The stream is
 * {@link GT6VeinGenerator#veinRandom} REUSED verbatim (the WD.java:547-560 seeding — the
 * card spec's "全随机 origin 种子化 = 上游语义加强版": pick AND shape rides the one
 * origin-seeded stream, where upstream's shape draws rode the per-chunk random).
 *
 * <p><b>The shape</b> is a pure function of three drawn parameters (center Y/X/Z, pinned
 * draw order) — a horizontal circle of the row radius carrying a vertical half-extent
 * that rides the circle profile (the flattened blob: full half-height at the center,
 * tapering to zero at the rim). NO per-block draws at all, so any two chunks recomputing
 * an origin derive bit-identical slices by construction — a strictly stronger determinism
 * than the vein's clip-independent-roll discipline. The radius rides the
 * {@link GTLensConfig#RADIUS_CODEC_CAP} = 48, which keeps every lens inside the ±3-chunk
 * scan window of the Feature (radius 48 + a center anywhere in the origin chunk &lt; 64
 * blocks = 4 chunks).
 */
public final class GT6LensGenerator {

    /** The upstream grid phase constant, verbatim (GT6WorldGenerator.java:97 via GT6VeinGenerator). */
    public static final int ORIGIN_PHASE = GT6VeinGenerator.ORIGIN_PHASE;

    /** The lens lattice modulus — the isOriginCell form widened 3 &gt; 9 (one origin per 9x9 chunks). */
    public static final int GRID_MODULUS = 9;

    private GT6LensGenerator() {
    }

    /** The lens grid predicate: the {@code (cc + PHASE) % m == 1} form with the 9-chunk mountain lattice. */
    public static boolean isLensOriginCell(int aChunkCoord) {
        return (aChunkCoord + ORIGIN_PHASE) % GRID_MODULUS == 1;
    }

    /**
     * The weighted draw of exactly one lens row ({@link GT6VeinGenerator#drawVein} shape):
     * sum the rarity of the drawable rows (a dead Y domain — maxY &lt;= minY — is skipped,
     * the vein-table validity face), then the cumulative nextInt(tMaxWeight) countdown.
     * Consumes exactly one draw when any row is drawable.
     */
    public static GTLensConfig drawLens(GTLensConfig.Table aTable, Random aRandom) {
        int tMaxWeight = 0;
        List<GTLensConfig> tList = new ArrayList<>(aTable.lenses().size());
        for (GTLensConfig tLens : aTable.lenses()) {
            if (tLens.maxY() <= tLens.minY()) continue;
            tMaxWeight += tLens.rarity();
            tList.add(tLens);
        }
        if (tMaxWeight <= 0 || tList.isEmpty()) return null;
        int tWeight = aRandom.nextInt(tMaxWeight);
        for (GTLensConfig tLens : tList) {
            tWeight -= tLens.rarity();
            if (tWeight <= 0) return tLens;
        }
        throw new IllegalStateException("GT6 strata-lens draw fell through the weight list"); // unreachable
    }

    /**
     * One origin's slice into the clip rectangle. THE deterministic core: three draws from
     * the passed origin-seeded stream in the pinned order (center Y, center X, center Z —
     * the vein's tMinY-first order in spirit), then the pure-geometry ellipsoid pass.
     * Nothing clip-dependent feeds the stream, so any two chunks recomputing this origin
     * derive identical slices.
     *
     * @param aClipMinX/aClipMaxX/aClipMinZ/aClipMaxZ the write clip (the work chunk's block
     *        bounds from place(); the tests drive full-lens vs per-chunk clips through it)
     */
    public static boolean generateSlice(GTLensConfig aLens, Random aRandom, int aOriginMinX, int aOriginMinZ,
            int aClipMinX, int aClipMaxX, int aClipMinZ, int aClipMaxZ, LensSink aSink) {
        int tCenterY = aLens.minY() + aRandom.nextInt(aLens.maxY() - aLens.minY() + 1); // the Y domain, inclusive
        int tCenterX = aOriginMinX + aRandom.nextInt(16);
        int tCenterZ = aOriginMinZ + aRandom.nextInt(16);
        boolean rPlaced = false;
        int tRadius = aLens.radius(), tHalf = aLens.halfHeight();
        int tMinX = Math.max(aClipMinX, tCenterX - tRadius), tMaxX = Math.min(aClipMaxX, tCenterX + tRadius);
        int tMinZ = Math.max(aClipMinZ, tCenterZ - tRadius), tMaxZ = Math.min(aClipMaxZ, tCenterZ + tRadius);
        for (int tX = tMinX; tX <= tMaxX; tX++) {
            long tDX = tX - tCenterX;
            for (int tZ = tMinZ; tZ <= tMaxZ; tZ++) {
                long tDZ = tZ - tCenterZ;
                double tHoriz = (double) (tDX * tDX + tDZ * tDZ) / ((double) tRadius * tRadius);
                if (tHoriz > 1.0) continue;
                // the flattened-blob face: the column's vertical half-extent rides the circle profile
                int tReach = (int) Math.floor(tHalf * Math.sqrt(1.0 - tHoriz));
                for (int tY = tCenterY - tReach; tY <= tCenterY + tReach; tY++) {
                    aSink.stone(tX, tY, tZ);
                    rPlaced = true;
                }
            }
        }
        return rPlaced;
    }

    /** The placement callback — the level face (GT6StrataLensFeature) and the offline-test face. */
    interface LensSink {
        void stone(int aX, int aY, int aZ);
    }
}
