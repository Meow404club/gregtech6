package gregtech6.worldgen;

import java.util.Random;

import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;

/**
 * The Coltan Contention deterministic core (task worldgen-coltan) — the pure generation
 * math of the upstream {@code WorldgenColtan} special generator (the second independent
 * generation form the r12 coverage audit's gap ②), isolated from the Feature adapter so
 * the offline tests drive it WITHOUT class-loading vanilla {@code Feature} (the
 * GT6BedrockOreGenerator posture).
 *
 * <p><b>Why a dedicated Feature and not an ore_small special row</b> (the card's
 * selection ruling): upstream WorldgenColtan is NOT a per-chunk uniform scatter — it
 * derives ONE world-seed-determined contention center from the world seed
 * ({@code seed+5 -> nextGaussian()*1500}, WorldgenColtan.java:54-55), drops a full
 * bedrock vein into exactly that one chunk (:57, the coordinate-selected vein — no
 * probability roll), scatters small ores in every chunk within a 480-block RADIUS of
 * that point (:62-69) and adds normal-form ores within 64 blocks (:72-79). A placed
 * ore_small row is Count+InSquare+HeightRange over EVERY chunk — no vanilla placement
 * modifier expresses "distance to a seed-derived point", so the row translation is
 * structurally impossible (the worldgen-flower-arm codec ruling face: rows translate
 * per-chunk semantics only).
 *
 * <p><b>The flow</b> (WorldgenColtan.java:53-81, in chunk order): center from the world
 * seed (a fixed stream, NOT the chunk random) -> the center chunk runs the shared
 * {@link GT6BedrockOreGenerator#generateVein} shape for Coltan -> the chunk random gates
 * the rings. The Feature adapter (GT6ColtanFeature) mirrors the upstream single-stream
 * order: the vein (when the chunk IS the center) draws first, the scatter continues the
 * same stream.
 *
 * <p><b>Declared deviations</b>: the upstream {@code GENERATING_SPECIAL} debug cut
 * (:58) is not ported (the CS debug posture has no modern carrier); the distance gate
 * computes in {@code long} (:59 upstream squares the int coordinate difference — at
 * coordinates past ~46000 chunks the int product overflows NEGATIVE and the
 * {@code > mRange*mRange} gate PASSES, littering coltan over the whole far field; the
 * long math implements the authored intent — the ring and only the ring); the scatter
 * stream is the dedicated {@link GT6Worldgen#COLTAN_DIMENSION_SALT} coordinate-seeded
 * stream (upstream drew the shared per-chunk random, a stream position this port cannot
 * and need not reproduce — the GT6BedrockOreGenerator declared-strengthening face); the
 * config-file override columns (MinHeight/MaxHeight/Amount/Range) ride no config surface
 * (the constants live here, the GT6NetherQuartzFeature class-constants face).
 */
public final class GT6ColtanGenerator {

    /** The upstream ctor columns verbatim (Loader_Worldgen.java:779): y band 20..40. */
    public static final int MIN_Y = 20;
    public static final int MAX_Y = 40;
    /** The per-ring attempt budget (:779 column 5; both rings draw their own count). */
    public static final int AMOUNT = 32;
    /** The contention ring radius in blocks (:779 column 6, upstream {@code mRange}). */
    public static final int RANGE = 480;

    /** WorldgenColtan.java:55 — the gaussian spread of the contention center. */
    public static final int CENTER_SPREAD = 1500;
    /** WorldgenColtan.java:54 — {@code new Random(aWorld.getSeed()+5)}, the +5. */
    public static final long SEED_SALT = 5;
    /** WorldgenColtan.java:71 — "close to the Bedrock Vein" = 64*64=4096, the large-ore ring. */
    public static final int LARGE_RING_SQ = 4096;

    /** RANGE squared, the small-ore ring gate (:62 {@code mRange*mRange}). */
    public static final int RANGE_SQ = RANGE * RANGE;

    /**
     * The center chunk's bedrock vein row — the :57 static call
     * {@code WorldgenOresBedrock.generateVein(MT.OREMATS.Coltan, ...)}: the FULL vein shape
     * (patch/muffin/tails) reuses {@link GT6BedrockOreGenerator#generateVein} verbatim. The
     * probability column is inert (the chunk was selected by COORDINATE match, the drawRows
     * gate never runs). DECLARED GAP: Coltan has no bedrock-ore blocks (the
     * GT6BedrockOreBlocks axis = the 46-row bedrock table's materials, Coltan is not one)
     * — the bedrock-face patch/forced block no-op through the sink's null-block guard, the
     * muffin shell + muffin/tail ores place in the host skins; a bedrock-axis extension is
     * the registry face, out of this card's files scope. A METHOD on purpose: the material
     * dereference stays at use time (the GTOreWorldgen supplier lesson — no clinit hazard).
     */
    public static GTBedrockOreConfig centerVein() {
        return new GTBedrockOreConfig("ore.special.coltan", MT.OREMATS.Coltan, 1, true);
    }

    /**
     * The world's contention center (WorldgenColtan.java:54-55 verbatim):
     * {@code new Random(seed+5)} then TWO gaussian draws scaled by 1500 — deterministic
     * per world seed, independent of chunk coordinates.
     */
    public static int[] center(long aWorldSeed) {
        Random tRandom = new Random(aWorldSeed + SEED_SALT);
        return new int[] {(int)(tRandom.nextGaussian() * CENTER_SPREAD), (int)(tRandom.nextGaussian() * CENTER_SPREAD)};
    }

    /**
     * The center-chunk match (WorldgenColtan.java:57): the center BLOCK coordinate's chunk
     * (the {@code >>4} face) equals the work chunk.
     */
    public static boolean hitsCenter(int aCenterX, int aCenterZ, int aChunkMinX, int aChunkMinZ) {
        return (aCenterX >> 4) == (aChunkMinX >> 4) && (aCenterZ >> 4) == (aChunkMinZ >> 4);
    }

    /**
     * The per-ring attempt count (WorldgenColtan.java:63/:73 verbatim): {@code max(1,
     * mAmount/2 + nextInt(1+mAmount)/2)} — 16..32 attempts at Amount 32.
     */
    public static int scatterCount(Random aRandom, int aAmount) {
        return Math.max(1, aAmount / 2 + aRandom.nextInt(1 + aAmount) / 2);
    }

    /**
     * The ring scatter (WorldgenColtan.java:59-81 verbatim, the long-gate deviation above):
     * out of the 480 ring = nothing; in the ring each iteration draws material FIRST
     * ({@code nextInt(5)}: 0 = Columbite, 1 = Tantalite, else Coltan — the 3:1:1 face) then
     * x/z uniform in-chunk and y uniform in [20, 40); past the 64 ring the loop returns
     * after the small-ore pass, inside it the second loop re-draws its own count and places
     * the normal-form ores.
     *
     * @param aSink the placement callbacks ({@code true} = the :65-67 WD.setSmallOre face,
     *        {@code false} = the :75-77 WD.setOre face)
     * @return false when the chunk is outside the contention ring (nothing placed)
     */
    public static boolean scatter(int aCenterX, int aCenterZ, int aChunkMinX, int aChunkMinZ,
            Random aRandom, ColtanSink aSink) {
        long tDX = (long)aCenterX - aChunkMinX, tDZ = (long)aCenterZ - aChunkMinZ;
        long tDistance = tDX * tDX + tDZ * tDZ; // :59 — long math, the overflow deviation note
        if (tDistance > RANGE_SQ) return false; // :62
        int tYRange = Math.max(1, MAX_Y - MIN_Y); // :65 the nextInt bound
        for (int i = 0, j = scatterCount(aRandom, AMOUNT); i < j; i++) { // :63
            OreDictMaterial tMaterial = material(aRandom.nextInt(5)); // :64 the switch roll FIRST
            aSink.ore(aChunkMinX + aRandom.nextInt(16), MIN_Y + aRandom.nextInt(tYRange),
                    aChunkMinZ + aRandom.nextInt(16), tMaterial, true); // :65-67
        }
        if (tDistance > LARGE_RING_SQ) return true; // :72
        for (int i = 0, j = scatterCount(aRandom, AMOUNT); i < j; i++) { // :73 the re-drawn count
            OreDictMaterial tMaterial = material(aRandom.nextInt(5));
            aSink.ore(aChunkMinX + aRandom.nextInt(16), MIN_Y + aRandom.nextInt(tYRange),
                    aChunkMinZ + aRandom.nextInt(16), tMaterial, false); // :75-77
        }
        return true;
    }

    /** The ring material switch (WorldgenColtan.java:64-68): 0 Columbite, 1 Tantalite, else Coltan. */
    public static OreDictMaterial material(int aRoll) {
        switch (aRoll) {
        case 0: return MT.OREMATS.Columbite;
        case 1: return MT.OREMATS.Tantalite;
        default: return MT.OREMATS.Coltan;
        }
    }

    /** The placement callbacks — the level face (GT6ColtanFeature) and the offline-test face. */
    public interface ColtanSink {
        /** One ore block: {@code aSmall} = the :65-67 small face, else the :75-77 normal face. */
        void ore(int aX, int aY, int aZ, OreDictMaterial aMaterial, boolean aSmall);
    }

    private GT6ColtanGenerator() {
    }
}
