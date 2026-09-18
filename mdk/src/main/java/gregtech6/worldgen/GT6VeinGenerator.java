package gregtech6.worldgen;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import gregapi.oredict.OreDictMaterial;
import gregtech6.registry.GT6OreBlocks;

/**
 * The large-vein deterministic core (task p30-w6-t3-large-veins) — the pure
 * generation math of the {@code WorldgenOresLarge} algorithm, isolated from the
 * {@link GT6LargeVeinFeature} adapter so the offline tests can drive it WITHOUT
 * class-loading vanilla {@code Feature} (whose clinit drags the entity registry,
 * unbootstrappable headless; the GT6WorldgenDatagenTest offline-safety posture).
 *
 * <p>Everything here is deterministic given (world seed, origin chunk): the WD.java
 * :547-560 stream, the GT6WorldGenerator.java:93-103 weighted exactly-one draw, and
 * the WorldgenOresLarge.java:88-131 slice pass in upstream draw order. See the
 * Feature javadoc for the per-chunk origin-grid scheme and the declared stream
 * hygiene (the indicator draws are unconditional; every writing chunk consumes the
 * identical sequence).
 */
public final class GT6VeinGenerator {

    /** The upstream grid phase constant, verbatim (GT6WorldGenerator.java:97). */
    public static final int ORIGIN_PHASE = 402653184;

    private GT6VeinGenerator() {
    }

    /** The grid predicate: a chunk coordinate is an origin cell (GT6WorldGenerator.java:97 verbatim; the huge positive addend keeps Java % positive). */
    public static boolean isOriginCell(int aChunkCoord) {
        return (aChunkCoord + ORIGIN_PHASE) % 3 == 1;
    }

    /**
     * WD.java:547-560 verbatim (the chunk coords already shifted), with the upstream
     * dimension salt exposed: {@code WD.random(World)} seeds with
     * {@code world.getSeed() ^ world.provider.dimensionId} (WD.java:547 — "to prevent
     * multiple Dimensions from being identical in Ore Generation", the comment verbatim),
     * so the stream takes the dimension id as a salt — overworld 0, nether -1, end 1
     * (the 1.7.10 numeric ids; the same numbers vanilla keeps as its dimension keys'
     * legacy ids). The zero-salt call is the overworld stream BIT-IDENTICAL to the
     * pre-salt form ({@code seed ^ 0 == seed}), so every existing overworld pin holds.
     * Discard the first 50 draws twice around the coord reseed. java.util.Random on
     * both legs (the research determinism ruling).
     */
    public static Random veinRandom(long aWorldSeed, long aDimSalt, int aChunkX, int aChunkZ) {
        Random tRandom = new Random(aWorldSeed ^ aDimSalt);
        for (int i = 0; i < 50; i++) tRandom.nextInt(0x00ffffff);
        // upstream precedence: nextLong() >> 2 + 1L binds as >> (2+1)
        tRandom = new Random(aWorldSeed ^ ((tRandom.nextLong() >> 3) * aChunkX + (tRandom.nextLong() >> 3) * aChunkZ));
        for (int i = 0; i < 50; i++) tRandom.nextInt(0x00ffffff);
        return tRandom;
    }

    /**
     * The weighted draw of exactly one vein (GT6WorldGenerator.java:90-103 verbatim):
     * sum the weights of the drawable rows (overworld && >= 1 valid slot — the mInvalid
     * gate WorldgenObject.java:60 + WorldgenOresLarge.java:85), then the cumulative
     * nextInt(tMaxWeight) countdown. Consumes exactly one draw when any row is drawable.
     */
    public static GTVeinConfig drawVein(List<GTVeinConfig> aTable, Random aRandom) {
        int tMaxWeight = 0;
        List<GTVeinConfig> tList = new ArrayList<>(aTable.size());
        for (GTVeinConfig tVein : aTable) {
            if (!tVein.overworld()) continue;
            if (!valid(tVein.oreTop()) && !valid(tVein.oreBottom()) && !valid(tVein.oreBetween())
                    && !valid(tVein.oreSpread())) continue;
            tMaxWeight += tVein.weight();
            tList.add(tVein);
        }
        if (tMaxWeight <= 0 || tList.isEmpty()) return null;
        int tWeight = aRandom.nextInt(tMaxWeight);
        for (GTVeinConfig tVein : tList) {
            tWeight -= tVein.weight();
            if (tWeight <= 0) return tVein;
        }
        throw new IllegalStateException("GT6 large-vein draw fell through the weight list"); // unreachable
    }

    /**
     * One origin's slice into the clip rectangle. THE deterministic core: every draw comes
     * from the passed origin-seeded stream in upstream order (:92 tMinY, :97-98 indicator
     * count/columns, :111-112 the four edge offsets, then the per-column rolls in column
     * order with the || short-circuit :114-128); nothing clip-dependent feeds the stream,
     * so any two chunks recomputing this origin derive identical slices (the research
     * bit-consistency condition).
     *
     * @param aClipMinX/aClipMaxX/aClipMinZ/aClipMaxZ the write clip (the work chunk's block
     *        bounds from place(); the tests drive full-vein vs per-chunk clips through it)
     */
    public static boolean generateSlice(GTVeinConfig aVein, Random aRandom, int aOriginMinX, int aOriginMinZ,
            int aClipMinX, int aClipMaxX, int aClipMinZ, int aClipMaxZ, int aWorldMinY, SliceSink aSink) {
        // :90 — the spawn gate, verbatim on the WRITING chunk (every table row carries 0)
        if (aVein.spawnDistance() > 0 && Math.abs(aClipMinX) <= aVein.spawnDistance()
                && Math.abs(aClipMinZ) <= aVein.spawnDistance()) return false;

        // :92 — tMinY (upstream re-seeded an identical WD.random for this draw; on the one
        // origin stream it is simply the next draw)
        int tMinY = aVein.minY() + aRandom.nextInt(aVein.maxY() - aVein.minY() - 5);

        // the four slots, validity-filtered (the per-layer mID > 0 guards :113-126; the
        // modern validity face is the GT6OreBlocks registration universe)
        OreDictMaterial tTop = valid(aVein.oreTop()) ? aVein.oreTop() : null;
        OreDictMaterial tBottom = valid(aVein.oreBottom()) ? aVein.oreBottom() : null;
        OreDictMaterial tBetween = valid(aVein.oreBetween()) ? aVein.oreBetween() : null;
        OreDictMaterial tSpread = valid(aVein.oreSpread()) ? aVein.oreSpread() : null;
        OreDictMaterial[] tSlots = {aVein.oreTop(), aVein.oreBottom(), aVein.oreBetween(), aVein.oreSpread()};

        // :94-108 — the indicator arm. The DRAWS are unconditional (a gate-dependent skip
        // would misalign the shape stream between chunks inside/outside the ring); the
        // >=64 ring gate (streets-on branch) suppresses only the placement.
        boolean rPlaced = false;
        if (aVein.indicator()) {
            boolean tRing = Math.abs(aClipMinX) >= 64 && Math.abs(aClipMaxX) >= 64
                    && Math.abs(aClipMinZ) >= 64 && Math.abs(aClipMaxZ) >= 64;
            for (int i = 0, j = 1 + aRandom.nextInt(3); i < j; i++) {
                int tX = aClipMinX + aRandom.nextInt(16), tZ = aClipMinZ + aRandom.nextInt(16);
                // :104 — 2/3 of the rocks carry one of the four slots (uniform over the four
                // slots, the UT.Code.select shape; an invalid pick falls back at IO time),
                // 1/3 the default rock (the upstream NBT-less arm)
                OreDictMaterial tMat = aRandom.nextInt(3) != 0 ? tSlots[aRandom.nextInt(4)] : null;
                if (tRing) {
                    for (int tY = Math.min(aSink.surfaceHeight(tX, tZ), tMinY + 25); tY >= tMinY - 10 && tY > aWorldMinY; tY--) {
                        int tProbe = aSink.probe(tX, tY, tZ);
                        if (tProbe == SliceSink.PROBE_LIQUID || tProbe == SliceSink.PROBE_OTHER) break; // :101/:103
                        if (tProbe == SliceSink.PROBE_PASS) continue; // :102
                        if (aSink.replaceable(tX, tY + 1, tZ)) { // :104 easyRep
                            aSink.indicatorRock(tX, tY + 1, tZ, tMat);
                            rPlaced = true;
                        }
                        break;
                    }
                }
            }
        }

        // :111-112 — the vein rectangle: origin ± one randomSize offset on each side
        int tCX = aOriginMinX - aRandom.nextInt(aVein.size());
        int tEX = aOriginMinX + 16 + aRandom.nextInt(aVein.size());
        int tCZ = aOriginMinZ - aRandom.nextInt(aVein.size());
        int tEZ = aOriginMinZ + 16 + aRandom.nextInt(aVein.size());
        for (int tX = tCX; tX <= tEX; tX++) for (int tZ = tCZ; tZ <= tEZ; tZ++) {
            boolean tInClip = tX >= aClipMinX && tX <= aClipMaxX && tZ >= aClipMinZ && tZ <= aClipMaxZ;
            // :114/:119/:123/:126 — the column-to-edge distance denominators (z-edges first,
            // x-edges second), integer-divided by the density
            int tDZEdge = Math.max(1, Math.max(Math.abs(tCZ - tZ), Math.abs(tEZ - tZ)) / aVein.density());
            int tDXEdge = Math.max(1, Math.max(Math.abs(tCX - tX), Math.abs(tEX - tX)) / aVein.density());
            if (tBottom != null) for (int tY = tMinY - 1; tY < tMinY + 2; tY++) { // :113-117 Bottom tMinY-1..+1
                if (aRandom.nextInt(tDZEdge) == 0 || aRandom.nextInt(tDXEdge) == 0) {
                    if (tInClip) { aSink.ore(tX, tY, tZ, tBottom); rPlaced = true; }
                }
            }
            if (tTop != null) for (int tY = tMinY + 3; tY < tMinY + 6; tY++) { // :118-122 Top +3..+5
                if (aRandom.nextInt(tDZEdge) == 0 || aRandom.nextInt(tDXEdge) == 0) {
                    if (tInClip) { aSink.ore(tX, tY, tZ, tTop); rPlaced = true; }
                }
            }
            if (tBetween != null && (aRandom.nextInt(tDZEdge) == 0 || aRandom.nextInt(tDXEdge) == 0)) { // :123-125
                int tY = tMinY + 2 + aRandom.nextInt(2); // the y draw rides the pass, upstream shape
                if (tInClip) { aSink.ore(tX, tY, tZ, tBetween); rPlaced = true; }
            }
            if (tSpread != null && (aRandom.nextInt(tDZEdge) == 0 || aRandom.nextInt(tDXEdge) == 0)) { // :126-128
                int tY = tMinY - 1 + aRandom.nextInt(7); // Spread -1..+5
                if (tInClip) { aSink.ore(tX, tY, tZ, tSpread); rPlaced = true; }
            }
        }
        return rPlaced;
    }

    /** The registration-universe validity (the modern mID &gt; 0 face: the slot has registrable ore blocks). */
    static boolean valid(OreDictMaterial aMaterial) {
        return aMaterial != null && aMaterial.mID > 0 && axis().contains(aMaterial);
    }

    private static volatile Set<OreDictMaterial> sAxis;

    /** The GT6OreBlocks material axis, resolved lazily (post-OP.init; identity-deduped by materialAxis itself). */
    static Set<OreDictMaterial> axis() {
        Set<OreDictMaterial> tAxis = sAxis;
        if (tAxis == null) {
            tAxis = Set.copyOf(GT6OreBlocks.materialAxis());
            sAxis = tAxis;
        }
        return tAxis;
    }

    /** The placement callbacks — the level face (GT6LargeVeinFeature) and the offline-test face. */
    interface SliceSink {
        int PROBE_PASS = 0;     // :102 non-opaque — the scan continues down
        int PROBE_LIQUID = 1;   // :101 liquid — the scan breaks
        int PROBE_SUITABLE = 2; // :103 grass/ground/sand/rock — the rock sits here
        int PROBE_OTHER = 3;    // :103 any other opaque — the scan breaks

        void ore(int aX, int aY, int aZ, OreDictMaterial aMaterial);
        void indicatorRock(int aX, int aY, int aZ, OreDictMaterial aMaterial); // null = the default-rock arm
        int surfaceHeight(int aX, int aZ);
        int probe(int aX, int aY, int aZ);
        boolean replaceable(int aX, int aY, int aZ);
    }
}
