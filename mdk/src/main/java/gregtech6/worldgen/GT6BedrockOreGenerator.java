package gregtech6.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import gregapi.oredict.OreDictMaterial;
import gregtech6.registry.GT6BedrockOreBlocks;

/**
 * The bedrock-ore deterministic core (task p31-bedrock-ore-worldgen) — the pure
 * generation math of {@code WorldgenOresBedrock} (the third, independent generation
 * form next to the large veins and the strata lenses), isolated from the Feature
 * adapter so the offline tests drive it WITHOUT class-loading vanilla {@code Feature}
 * (the entity-registry clinit trap, the GT6StrataLensTest posture).
 *
 * <p><b>No origin grid</b>: unlike the veins (3-chunk lattice) and the lenses (9-chunk),
 * the upstream bedrock ore rolls PER CHUNK — one {@code WorldgenOresBedrock} object per
 * row, each with its own {@code nextInt(mProbability)} gate (WorldgenOresBedrock.java:142),
 * all inside the single chunk (generateVein :181-230 writes patch/muffin/tails within
 * x/z 1..14 — the research.p30-w6-bedrock-spring single-chunk face). So the deterministic
 * stream is the work chunk's own {@link GT6VeinGenerator#veinRandom} (the WD.java:547-560
 * coordinate seeding — the decision-level determinism the strata card pinned; upstream
 * drew from the shared per-chunk worldgen random, a stream position this port cannot and
 * need not reproduce — the declared strengthening, same face as the vein card).
 *
 * <p><b>The rolls</b> (drawRows): table order, one {@code nextInt(P)==0} per
 * overworld-and-valid row, INDEPENDENT (no exactly-one weighting — one WorldgenObject
 * per row upstream). Two rows hitting one chunk is legal and rare (OW sum ~0.5%/chunk).
 *
 * <p><b>The Y mapping</b> (the declared translation, the research card's modern face):
 * upstream y=0 (the 1.7.10 flat bedrock floor) -> {@link #BEDROCK_Y} = -64 (the 1.20.1
 * flat bedrock floor); the muffin layers y=1..6 -> -63..-58 (the deepslate band); the
 * tails y=7..waterLevel-1 -> -57..62 (the sea level 63 face, WD.waterLevel).
 */
public final class GT6BedrockOreGenerator {

    /** The flat bedrock floor of the modern overworld — the upstream y=0 face (see class javadoc). */
    public static final int BEDROCK_Y = -64;
    /** The muffin occupies the 6 layers above the floor: y 1..6 upstream -> -63..-58 (the deepslate shell band). */
    public static final int MUFFIN_LAYERS = 6;
    /** WorldgenOresBedrock.java:214 {@code WD.waterLevel(aWorld)} — the modern overworld sea level. */
    public static final int SEA_LEVEL = 63;
    /** The tail top face: upstream {@code tY = tD1.length..waterLevel-1} = 7..62 -> -57..62. */
    public static final int TAIL_TOP_Y = SEA_LEVEL - 1;

    /** WorldgenOresBedrock.java:198-199 — the muffin trapezoid, x/z lower/upper bounds per layer, verbatim. */
    public static final int[] MUFFIN_D1 = {5, 4, 2, 1, 0, 2, 5};
    public static final int[] MUFFIN_D2 = {11, 12, 14, 15, 16, 14, 11};

    private GT6BedrockOreGenerator() {
    }

    /** The bedrock-row validity (the modern mID > 0 face: the row's material has registrable bedrock blocks). */
    public static boolean valid(GTBedrockOreConfig aRow) {
        return aRow.overworld() && aRow.material() != null && aRow.material().mID > 0
                && axis().contains(aRow.material());
    }

    private static volatile java.util.Set<OreDictMaterial> sAxis;

    /** The GT6BedrockOreBlocks material axis, resolved lazily (the GT6VeinGenerator.axis form). */
    static java.util.Set<OreDictMaterial> axis() {
        java.util.Set<OreDictMaterial> tAxis = sAxis;
        if (tAxis == null) {
            tAxis = java.util.Set.copyOf(GT6BedrockOreBlocks.materialAxis());
            sAxis = tAxis;
        }
        return tAxis;
    }

    /**
     * The per-chunk row rolls (WorldgenOresBedrock.java:142 gate per row, table order,
     * one stream): every overworld-and-valid row rolls {@code nextInt(P)==0} on the SAME
     * chunk-seeded stream — the hits return in table order (the draw order the shape
     * stream then continues on).
     */
    public static List<GTBedrockOreConfig> drawRows(GTBedrockOreConfig.Table aTable, Random aRandom) {
        List<GTBedrockOreConfig> rHits = new ArrayList<>(1);
        for (GTBedrockOreConfig tRow : aTable.rows()) {
            if (!valid(tRow)) continue;
            if (aRandom.nextInt(tRow.probability()) == 0) rHits.add(tRow);
        }
        return rHits;
    }

    /**
     * One vein (generateVein, WorldgenOresBedrock.java:181-230, in-chunk by construction):
     * the bedrock-face gate, the 6x6 patch, the forced center block, the muffin trapezoid,
     * the 5-7 small-ore tails. Every draw comes from the passed chunk-seeded stream in
     * upstream order (patch roll per position -> the forced block's 2 offsets -> muffin
     * roll per position -> the tail count and per-step rolls), so a chunk's decision and
     * shape replay identically.
     *
     * @param aChunkMinX/aChunkMinZ the work chunk's min block coords (all writes stay inside
     *        x/z 0..15 of it; the tail walk's 1..14 bounds are the upstream verbatim gates)
     * @param aSink the placement callbacks (the level face and the offline-test face)
     * @return false when the chunk center is not a bedrock face (the :185 gate)
     */
    public static boolean generateVein(GTBedrockOreConfig aRow, Random aRandom, int aChunkMinX, int aChunkMinZ,
            int aWorldMinY, BedrockSink aSink) {
        // :183-185 — Requires existing Bedrock! (the chunk center's bedrock face)
        if (!aSink.isBedrockFace(aChunkMinX + 8, aChunkMinZ + 8)) return false;
        int tBedrockY = Math.max(BEDROCK_Y, aWorldMinY); // the flat floor, clamped to the world (datagen/other dims)

        // :187-192 — the 6x6 bedrock-face patch: 1/6 large, 2/6 small, 3/6 nothing
        for (int tX = 5; tX < 11; tX++) for (int tZ = 5; tZ < 11; tZ++) {
            switch (aRandom.nextInt(6)) {
                case 0: aSink.bedrockOre(aChunkMinX + tX, tBedrockY, aChunkMinZ + tZ, aRow.material(), false); break;
                case 1: case 2: aSink.bedrockOre(aChunkMinX + tX, tBedrockY, aChunkMinZ + tZ, aRow.material(), true); break;
            }
        }
        // :194 — At least one Ore Block must be there: force a large one in the center area
        aSink.bedrockOre(aChunkMinX + 6 + aRandom.nextInt(4), tBedrockY, aChunkMinZ + 6 + aRandom.nextInt(4),
                aRow.material(), false);

        // :196-211 — the muffin: a deepslate shell over the trapezoid (tD1[tY]..tD2[tY]-1,
        // y 1..6 -> -63..-58), then 1/6 large / 2/6 small / 3/6 plain-shell per position.
        // The shell writes UNCONDITIONALLY (the upstream GENERATED_NO_BEDROCK_ORE first-vein
        // face: see the class javadoc — the static-flag interplay is a spring-card seam).
        for (int tLayer = 1; tLayer < MUFFIN_D1.length; tLayer++) {
            int tY = tBedrockY + tLayer;
            for (int tX = MUFFIN_D1[tLayer]; tX < MUFFIN_D2[tLayer]; tX++) {
                for (int tZ = MUFFIN_D1[tLayer]; tZ < MUFFIN_D2[tLayer]; tZ++) {
                    aSink.shell(aChunkMinX + tX, tY, aChunkMinZ + tZ);
                    switch (aRandom.nextInt(6)) {
                        case 0: aSink.ore(aChunkMinX + tX, tY, aChunkMinZ + tZ, aRow.material(), false); break;
                        case 1: case 2: aSink.ore(aChunkMinX + tX, tY, aChunkMinZ + tZ, aRow.material(), true); break;
                    }
                }
            }
        }

        // :213-225 — the 5-7 random-walk small-ore tails up to just below sea level
        for (int i = 5 + aRandom.nextInt(3); i-- > 0;) {
            int tX = 5 + aRandom.nextInt(6), tZ = 5 + aRandom.nextInt(6);
            for (int tLayer = MUFFIN_D1.length; tLayer + tBedrockY <= TAIL_TOP_Y; tLayer++) {
                switch (aRandom.nextInt(7)) {case 0: tX++; break; case 1: tX--; break; case 2: tZ++; break; case 3: tZ--;}
                if (tX <= 0 || tX >= 15 || tZ <= 0 || tZ >= 15) {
                    aSink.ore(aChunkMinX + tX, tBedrockY + tLayer, aChunkMinZ + tZ, aRow.material(), true);
                    break;
                } else if (aRandom.nextInt(3) != 0) {
                    aSink.ore(aChunkMinX + tX, tBedrockY + tLayer, aChunkMinZ + tZ, aRow.material(), true);
                }
            }
        }
        return true;
    }

    /** The placement callbacks — the level face (GT6BedrockOreFeature) and the offline-test face. */
    interface BedrockSink {
        /** The :185 gate: the chunk-center block at the bedrock floor is bedrock (or a bedrock ore). */
        boolean isBedrockFace(int aX, int aZ);

        /** The patch/forced block: the bedrock face becomes the large/small bedrock ore. */
        void bedrockOre(int aX, int aY, int aZ, OreDictMaterial aMaterial, boolean aSmall);

        /** The muffin shell: the position becomes deepslate (the :196-206 unconditional face). */
        void shell(int aX, int aY, int aZ);

        /** The muffin/tail ore: the position becomes the host stone's large/small ore (WD.setOre/setSmallOre — the host resolves in the sink, the just-written deepslate or the natural stone). */
        void ore(int aX, int aY, int aZ, OreDictMaterial aMaterial, boolean aSmall);
    }
}
