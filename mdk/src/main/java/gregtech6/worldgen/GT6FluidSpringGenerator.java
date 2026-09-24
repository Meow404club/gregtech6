package gregtech6.worldgen;

import java.util.Random;

/**
 * The bedrock-spring deterministic core (task p31-fluid-spring; the nozzle arm landed by
 * task p38-issue5-fluid-spring-nozzle, the indicator arm stays the declared defer) — the
 * pure generation math
 * of {@code WorldgenFluidSpring} (the lake half + the nozzle half of the upstream spring),
 * isolated from the Feature adapter so the offline
 * tests drive it WITHOUT class-loading vanilla {@code Feature} (the GT6BedrockOreGenerator
 * posture).
 *
 * <p><b>The per-chunk mutual-exclusion seam (the card's key constraint, the bedrock
 * deviation ④ handoff)</b>: upstream the spring and the bedrock ore arbitrate ONE bedrock
 * event per chunk through two STATIC FLAGS (WorldgenFluidSpring.java:62-64 — the spring
 * skips when {@code GENERATED_NO_BEDROCK_ORE} was cleared by an ore this chunk, and
 * clears {@code CAN_GENERATE_BEDROCK_ORE} itself; the ore gate reads it back at
 * WorldgenOresBedrock.java:142). The static flags did not port (bedrock card deviation
 * ④). THIS card rebuilds the same semantics self-contained: the bedrock-ore draw is
 * REPLAYED on the ore feature's own coordinate-seeded stream ({@link #oreClaims} —
 * veinRandom(seed, OVERWORLD_DIMENSION_SALT, chunk) + drawRows, the exact prefix the
 * GT6BedrockOreFeature consumes) and a claimed chunk refuses the spring. The "矿先泉后"
 * registration-order semantics (Loader_Worldgen.java:781 "Has to be after Bedrock Ores")
 * becomes execution-order-INDEPENDENT: whichever feature runs first writes its own event
 * and the replay refuses the other — the ore feature needs no seam (it rolls its rows
 * unconditionally, upstream same: the ore only guards against a SPRING that ran first,
 * which the replay direction covers). The spring's own rows ride a SECOND independent
 * stream, salt {@link GT6Worldgen#SPRING_DIMENSION_SALT} (the port-owned synthetic salt —
 * upstream drew the row gates from the shared per-chunk random after the bedrock draws,
 * a stream position the port cannot and need not reproduce, the vein-card strengthening
 * face; the spring-spring claim of :64 becomes first-hit-wins inside one draw, since one
 * Feature instance walks the whole table per chunk).
 *
 * <p><b>The shape</b> (generateDome, WorldgenFluidSpring.java:66-80 verbatim): the chunk
 * centre at the bedrock floor must be bedrock or a bedrock ore (:66-67); then the stepped
 * dome — for layer i = 0..6, footprint inset by i from each chunk wall: the shell pass
 * fills every NON-OPAQUE cell at y = i+1 with the shell block (:73, the cave-seal skin),
 * and for i &gt; 0 the lake pass fills every cell at y = i with the row's fluid block
 * (:75, unconditional). Net: a fluid ziggurat y = 1..6 shrinking 14x14 -> 4x4, sealed by
 * the shell wherever the world was open. The Y mapping is the bedrock card's declared
 * translation: upstream y=0 (the 1.7.10 flat bedrock floor) -> {@link GT6BedrockOreGenerator#BEDROCK_Y}
 * = -64, so the lake rides -63..-58 and the shell -63..-57. NO random draws anywhere in
 * the shape (see the config javadoc) — same seed + same world = same dome.
 */
public final class GT6FluidSpringGenerator {

    /** WorldgenFluidSpring.java:72 {@code for (int i = 0; i <= 6; i++)} — the dome layer count. */
    public static final int DOME_LAYERS = 7;

    /** The chunk-local centre offset of the bedrock-face gate (WorldgenFluidSpring.java:66 aMinX+8). */
    public static final int GATE_OFFSET = 8;

    private GT6FluidSpringGenerator() {
    }

    /** The row validity (the modern dimension-mask face: only overworld rows roll — the offworld rows :789-797 stay dormant). */
    public static boolean valid(GTFluidSpringConfig aRow) {
        return aRow.overworld();
    }

    /**
     * The exclusion replay (WorldgenFluidSpring.java:62 {@code !GENERATED_NO_BEDROCK_ORE}
     * face): re-rolls the bedrock-ore rows on the PASSED stream — the caller builds it
     * with {@link GT6VeinGenerator#veinRandom}(seed, OVERWORLD_DIMENSION_SALT, x, z), the
     * identical stream the GT6BedrockOreFeature draws with, so a chunk the ore claims
     * (any row rolled its 1/P) refuses the spring. Pure — the table comes from the caller
     * (the Feature reads the live gt6:bedrock_ores config; the tests pass the constant).
     */
    public static boolean oreClaims(GTBedrockOreConfig.Table aOreTable, Random aOreRandom) {
        return !GT6BedrockOreGenerator.drawRows(aOreTable, aOreRandom).isEmpty();
    }

    /**
     * The spring row draw (WorldgenFluidSpring.java:62/:64): table order, one
     * {@code nextInt(P)==0} per valid row, FIRST hit wins and is returned — the upstream
     * CAN_GENERATE_BEDROCK_ORE=F claim blocks every later spring row, so at most one
     * spring per chunk (null = no spring this chunk).
     */
    public static GTFluidSpringConfig drawSpring(GTFluidSpringConfig.Table aTable, Random aRandom) {
        for (GTFluidSpringConfig tRow : aTable.rows()) {
            if (!valid(tRow)) continue;
            if (aRandom.nextInt(tRow.probability()) == 0) return tRow;
        }
        return null;
    }

    /**
     * The stepped dome (WorldgenFluidSpring.java:66-80): the bedrock-face gate, then the
     * shell pass (non-opaque cells at y = i+1) and the lake pass (every cell at y = i,
     * i &gt; 0) per layer, footprint inset by the layer index from each chunk wall (local
     * x/z 0..15 — upstream aMinX+i..aMaxX-i), then the NOZZLE arm (:77-79, task
     * p38-issue5-fluid-spring-nozzle): for i &gt; 2, per position, a row with a spring
     * fluid rolls {@code nextInt(16) == 0} on the PASSED stream and a strict-bedrock
     * floor places the nozzle at the bedrock floor (y = aBedrockY, upstream y = 0) —
     * {@link DomeSink#nozzle}. The draws ride the same coordinate-seeded stream the
     * caller owns (the Feature passes the SPRING_DIMENSION_SALT stream after drawSpring;
     * the upstream shared-chunk-random draw is unreproducible, the documented port
     * strengthening) — decision-level determinism holds, and the dome writes themselves
     * stay draw-free. A row without a spring fluid draws NOTHING (the upstream
     * short-circuit order: mSpringFluid != null first).
     *
     * @param aBedrockY the flat bedrock floor ({@link GT6BedrockOreGenerator#BEDROCK_Y},
     *        clamped to the world minimum by the caller)
     * @param aNozzleRandom the coordinate-seeded stream the 1/16 nozzle draws ride
     * @return false when the chunk centre is not a bedrock face (the :67 gate)
     */
    public static boolean generateDome(GTFluidSpringConfig aRow, int aChunkMinX, int aChunkMinZ,
            int aBedrockY, Random aNozzleRandom, DomeSink aSink) {
        // :66-67 — the chunk centre at the bedrock floor must be bedrock (or a bedrock ore)
        if (!aSink.isBedrockFace(aChunkMinX + GATE_OFFSET, aChunkMinZ + GATE_OFFSET)) return false;

        for (int i = 0; i < DOME_LAYERS; i++) {
            for (int tX = i; tX < 16 - i; tX++) for (int tZ = i; tZ < 16 - i; tZ++) {
                // :73 — the shell skin over every non-opaque cell one layer above
                if (!aSink.isOpaque(aChunkMinX + tX, aBedrockY + i + 1, aChunkMinZ + tZ))
                    aSink.shell(aChunkMinX + tX, aBedrockY + i + 1, aChunkMinZ + tZ);

                // :75 — the lake body, unconditional (i > 0 keeps the floor layer fluid-free)
                if (i > 0) aSink.fluid(aChunkMinX + tX, aBedrockY + i, aChunkMinZ + tZ, aRow);

                // :77-79 — the nozzle arm (task p38-issue5-fluid-spring-nozzle): the
                // upstream short-circuit order verbatim (fluid first, then the draw,
                // then the strict WD.bedrock floor read)
                if (aRow.springFluid() != null && i > 2 && aNozzleRandom.nextInt(16) == 0
                        && aSink.isBedrock(aChunkMinX + tX, aChunkMinZ + tZ))
                    aSink.nozzle(aChunkMinX + tX, aBedrockY, aChunkMinZ + tZ);
            }
        }
        return true;
    }

    /** The placement callbacks — the level face (GT6FluidSpringFeature) and the offline-test face. */
    interface DomeSink {
        /** The :67 gate: the chunk-center block at the bedrock floor is bedrock (or a bedrock ore). */
        boolean isBedrockFace(int aX, int aZ);

        /** The :77 gate: the position's floor block is STRICTLY vanilla bedrock (WD.bedrock — the nozzle admits no bedrock-ore floor). */
        boolean isBedrock(int aX, int aZ);

        /** The :73 WD.opq read — the opaque-full-cube check the shell pass seals on. */
        boolean isOpaque(int aX, int aY, int aZ);

        /** The shell skin write (:73 — deepslate, the sibling bedrock muffin translation). */
        void shell(int aX, int aY, int aZ);

        /** The lake body write (:75 — the row's resolved fluid block source state). */
        void fluid(int aX, int aY, int aZ, GTFluidSpringConfig aRow);

        /** The nozzle placement (:78 — the MultiTileEntityFluidSpring port at the bedrock floor). */
        void nozzle(int aX, int aY, int aZ);
    }
}
