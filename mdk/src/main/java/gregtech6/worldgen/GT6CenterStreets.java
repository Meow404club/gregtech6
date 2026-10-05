package gregtech6.worldgen;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * The Streets geometry (task worldgen-center-nexus) — the {@code WorldgenStreets} port
 * (WorldgenStreets.java, 890 lines) in the GEOMETRIC EQUIVALENCE form the card declared:
 * grid parameters verbatim (the 24-wide band band∈[-16,15], the asphalt lanes at
 * band∈[-12,-2]∪[1,11], the median band∈[-1,0], the rail cadence ((i+2)/4)%2, the plaza
 * span |i|,|j|≤31, the marker towers at ±28..31, the 512-boundary level-marker and
 * interchange cadences, the HEIGHT offsets, the kill-sky height asymmetry 64 vs 32), the
 * write topology chunk-decomposed — each chunk writes the absolute coordinates it owns in
 * the BAND axis (the road cross-section direction; the class javadoc on
 * {@link GT6CenterFeature} carries the decomposition rationale). Every body here is a
 * pure function of absolute coordinates + the {@link GT6CenterFeature.Env}/
 * {@link GT6CenterFeature.Sink} deps — offline-testable.
 *
 * <p>Deviations (all declared): the 4 corner NB marks at y255 (:362/:389) are unreachable
 * in the 3x3 write window — cut; the entity cull (:650/:887) is a no-op (no entities at
 * generation time); the horizontal half-slab walls collapse to bottom slabs (the port
 * slab is the vanilla vertical-only SlabBlock); the dyed CFoam ring slabs map to the
 * 16-dye concrete slab family; sign text uses biome registry paths.
 */
public final class GT6CenterStreets {

    /** The road axis — an X-road runs along Z (band = the x coordinate), a Z-road along X (band = the z coordinate). */
    public enum Axis { X, Z }

    private GT6CenterStreets() {
    }

    /** {@code UT.Code.inside(aMin, aMax, x)} verbatim (:75, UT.java:1730). */
    static boolean inside(int aMin, int aMax, int aValue) {
        return aMin < aMax ? aMin <= aValue && aValue <= aMax : aMax <= aValue && aValue <= aMin;
    }

    /** {@code WD.even(int...)} verbatim (:580 — the count of even coords is even). */
    static boolean even(int... aCoords) {
        int i = 0;
        for (int tCoord : aCoords) if (tCoord % 2 == 0) i++;
        return i % 2 == 0;
    }

    /** The level-marker cadence — the segment crosses a 512 boundary below ({@code aBase>>9 != (aBase-16)>>9}, :530/:767). */
    public static boolean levelMarker(int aBase) {
        return (aBase >> 9) != ((aBase - 16) >> 9);
    }

    /** The interchange cadence — the segment crosses a 512 boundary above ({@code aBase>>9 != (aBase+16)>>9}, :549/:786). */
    public static boolean interchange(int aBase) {
        return (aBase >> 9) != ((aBase + 16) >> 9);
    }

    /** The band-owner test — the decomposition seam: does the owning chunk hold this absolute band coordinate. */
    static boolean owns(int aOwnBandChunk, Axis aAxis, int aX, int aZ) {
        return aOwnBandChunk == Math.floorDiv(aAxis == Axis.X ? aX : aZ, 16);
    }

    // ---------------------------------------------------------------- plaza (:69-356)

    /**
     * The 64x64 intersection city: the owned plaza columns + the owned decorations. The
     * under-plaza beacon pyramid rides the BEACON flag (:305-356). Spawn is the caller's.
     */
    public static void plaza(GT6CenterFeature.Sink aSink, GT6CenterFeature.Env aEnv, int aCx, int aCz,
            boolean aCenterBiomes, boolean aNexus, boolean aTesting, boolean aBeacon) {
        for (int i = Math.max(aCx * 16, -32); i <= Math.min(aCx * 16 + 15, 31); i++) {
            for (int j = Math.max(aCz * 16, -32); j <= Math.min(aCz * 16 + 15, 31); j++) {
                plazaColumn(aSink, i, j, aCenterBiomes, aNexus, aTesting);
            }
        }
        decoratePlaza(aSink, aCx, aCz);
        plazaSigns(aSink, aEnv, aCx, aCz);
        if (aBeacon) plazaBeacon(aSink, aEnv, aCx, aCz);
    }

    /** One plaza column — the pure per-(i,j) body of the upstream 64x64 loop (:70-98). */
    static void plazaColumn(GT6CenterFeature.Sink aSink, int i, int j, boolean aCenterBiomes, boolean aNexus, boolean aTesting) {
        BlockState tAir = Blocks.AIR.defaultBlockState();
        for (int k = 2; k < 64; k++) aSink.set(i, GT6CenterFeature.HEIGHT + k, j, tAir); // :70
        for (int k = 1; k < GT6CenterFeature.HEIGHT; k++) aSink.set(i, k, j, GT6CenterFeature.state(GT6CenterFeature.concrete(8))); // :71
        aSink.set(i, GT6CenterFeature.HEIGHT - 1, j, GT6CenterFeature.state(GT6CenterFeature.concrete(8))); // :73
        if (i <= -29 || j <= -29 || i >= 28 || j >= 28) { // :74
            if (inside(-12, 11, i) || inside(-12, 11, j)) { // :75 — the road strips through the band
                boolean tEdge = i == -31 || j == -31 || i == 30 || j == 30; // :76 the white rim
                aSink.set(i, GT6CenterFeature.HEIGHT, j, tEdge ? Blocks.WHITE_CONCRETE.defaultBlockState()
                        : Blocks.BLACK_CONCRETE.defaultBlockState()); // the Asphalt mapping
                aSink.set(i, GT6CenterFeature.HEIGHT + 1, j, tAir); // :77
            } else {
                aSink.set(i, GT6CenterFeature.HEIGHT, j, GT6CenterFeature.state(GT6CenterFeature.concrete(8))); // :79
                if (!aCenterBiomes && (i == -32 || j == -32 || i == 31 || j == 31) // :80
                        && (!aNexus || i < 0 || i == 31 || j != -32)
                        && (!aTesting || j > 0 || i != 31 || j == -32)) {
                    for (int k = 1; k <= 5; k++) { // :81-85 the checker wall
                        aSink.set(i, GT6CenterFeature.HEIGHT + k, j, even(i, k, j)
                                ? GT6CenterFeature.state(GT6CenterFeature.cfoam())
                                : GT6CenterFeature.state(GT6CenterFeature.concrete(7)));
                    }
                } else {
                    aSink.set(i, GT6CenterFeature.HEIGHT + 1, j, slab(GT6CenterFeature.cfoamSlab(), SlabType.BOTTOM)); // :87
                }
            }
        } else {
            if (inside(-12, 11, i) && inside(-12, 11, j)) { // :91 — the pavilion field
                aSink.set(i, GT6CenterFeature.HEIGHT, j, GT6CenterFeature.state(GT6CenterFeature.concrete(8))); // :92
                boolean tInner = inside(-11, 10, i) && inside(-11, 10, j); // :93
                aSink.set(i, GT6CenterFeature.HEIGHT + 1, j, tInner
                        ? GT6CenterFeature.state(GT6CenterFeature.concrete(8))
                        : GT6CenterFeature.state(GT6CenterFeature.cfoam()));
            } else {
                aSink.set(i, GT6CenterFeature.HEIGHT, j, Blocks.BLACK_CONCRETE.defaultBlockState()); // :95 the inner roads
                aSink.set(i, GT6CenterFeature.HEIGHT + 1, j, tAir); // :96
            }
        }
    }

    /** The four marker towers (2x2 colored concrete H+1..H+3 + cfoam slabs H+4, :102-117 form) + the slab rings. */
    private static void decoratePlaza(GT6CenterFeature.Sink aSink, int aCx, int aCz) {
        tower(aSink, aCx, aCz, -32, -31, -1, 0, 4); // blue NW (:102-109) — DYE_INDEX_Blue 4
        tower(aSink, aCx, aCz, 30, 31, -1, 0, 1); // red NE (:153-160)
        tower(aSink, aCx, aCz, -1, 0, -32, -31, 11); // yellow south (:204-211)
        tower(aSink, aCx, aCz, -1, 0, 30, 31, 2); // green south-east (:255-262)
        // the white/red slab rings around each tower (:118-123/:169-174/:220-225/:271-276)
        ringSlab(aSink, aCx, aCz, -32, -2, 15); ringSlab(aSink, aCx, aCz, -31, -2, 1);
        ringSlab(aSink, aCx, aCz, -30, -1, 15); ringSlab(aSink, aCx, aCz, -30, 0, 1);
        ringSlab(aSink, aCx, aCz, -31, 1, 15); ringSlab(aSink, aCx, aCz, -32, 1, 1);
        ringSlab(aSink, aCx, aCz, 31, -2, 1); ringSlab(aSink, aCx, aCz, 30, -2, 15);
        ringSlab(aSink, aCx, aCz, 29, -1, 1); ringSlab(aSink, aCx, aCz, 29, 0, 15);
        ringSlab(aSink, aCx, aCz, 30, 1, 1); ringSlab(aSink, aCx, aCz, 31, 1, 15);
        ringSlab(aSink, aCx, aCz, -2, -32, 15); ringSlab(aSink, aCx, aCz, -2, -31, 1);
        ringSlab(aSink, aCx, aCz, -1, -30, 15); ringSlab(aSink, aCx, aCz, 0, -30, 1);
        ringSlab(aSink, aCx, aCz, 1, -31, 15); ringSlab(aSink, aCx, aCz, 1, -32, 1);
        ringSlab(aSink, aCx, aCz, -2, 31, 1); ringSlab(aSink, aCx, aCz, -2, 30, 15);
        ringSlab(aSink, aCx, aCz, -1, 29, 1); ringSlab(aSink, aCx, aCz, 0, 29, 15);
        ringSlab(aSink, aCx, aCz, 1, 30, 1); ringSlab(aSink, aCx, aCz, 1, 31, 15);
    }

    private static void tower(GT6CenterFeature.Sink aSink, int aCx, int aCz, int x1, int x2, int z1, int z2, int aDye) {
        for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++) {
            if (aCx != Math.floorDiv(x, 16) || aCz != Math.floorDiv(z, 16)) continue;
            for (int k = 1; k <= 3; k++) aSink.set(x, GT6CenterFeature.HEIGHT + k, z, GT6CenterFeature.state(GT6CenterFeature.concrete(aDye)));
            aSink.set(x, GT6CenterFeature.HEIGHT + 4, z, slab(GT6CenterFeature.cfoamSlab(), SlabType.BOTTOM));
        }
    }

    private static void ringSlab(GT6CenterFeature.Sink aSink, int aCx, int aCz, int aX, int aZ, int aWhiteRedDye) {
        if (aCx != Math.floorDiv(aX, 16) || aCz != Math.floorDiv(aZ, 16)) return;
        // the upstream white/red slabs ride the dyed CFoam slab meta; the port face is the
        // 16-dye concrete slab family (gt6:concrete_white_slab / _red_slab).
        aSink.set(aX, GT6CenterFeature.HEIGHT + 1, aZ, slab(GT6CenterFeature.concreteSlab(aWhiteRedDye), SlabType.BOTTOM));
    }

    /** The eight biome sign pairs (:124-303) — lines = the 4 biome samples per sign, H+3 far / H+2 near. */
    private static void plazaSigns(GT6CenterFeature.Sink aSink, GT6CenterFeature.Env aEnv, int aCx, int aCz) {
        signPair(aEnv, aCx, aCz, -30, 0, Direction.EAST, false, 95, 0);
        signPair(aEnv, aCx, aCz, -30, -1, Direction.EAST, false, -96, 0);
        signPair(aEnv, aCx, aCz, 29, 0, Direction.WEST, true, 95, 0);
        signPair(aEnv, aCx, aCz, 29, -1, Direction.WEST, true, -96, 0);
        signPair(aEnv, aCx, aCz, 0, -30, Direction.SOUTH, false, 95, 1);
        signPair(aEnv, aCx, aCz, -1, -30, Direction.SOUTH, false, -96, 1);
        signPair(aEnv, aCx, aCz, 0, 29, Direction.NORTH, true, 95, 1);
        signPair(aEnv, aCx, aCz, -1, 29, Direction.NORTH, true, -96, 1);
    }

    /** One sign pair: 4 samples at H+3 (the ±4096..2560 row) + 4 at H+2 (the ±2048..512 row) — the samples ride the SIGNED coordinate verbatim (:126-135 form, no offset). */
    private static void signPair(GT6CenterFeature.Env aEnv, int aCx, int aCz, int aX, int aZ, Direction aFace,
            boolean aPositiveFar, int aFixed, int aAxisSel) {
        if (aCx != Math.floorDiv(aX, 16) || aCz != Math.floorDiv(aZ, 16)) return;
        int tRot = GT6CenterFeature.signRotation(aFace);
        int tSign = aPositiveFar ? 1 : -1;
        aEnv.sign(aX, GT6CenterFeature.HEIGHT + 3, aZ, tRot,
                name(aEnv, aAxisSel, tSign * 4096, aFixed), name(aEnv, aAxisSel, tSign * 3584, aFixed),
                name(aEnv, aAxisSel, tSign * 3072, aFixed), name(aEnv, aAxisSel, tSign * 2560, aFixed));
        aEnv.sign(aX, GT6CenterFeature.HEIGHT + 2, aZ, tRot,
                name(aEnv, aAxisSel, tSign * 2048, aFixed), name(aEnv, aAxisSel, tSign * 1536, aFixed),
                name(aEnv, aAxisSel, tSign * 1024, aFixed), name(aEnv, aAxisSel, tSign * 512, aFixed));
    }

    /** aAxisSel 0 = sample along X at the fixed Z, 1 = along Z at the fixed X. */
    private static String name(GT6CenterFeature.Env aEnv, int aAxisSel, int aVar, int aFixed) {
        return aAxisSel == 0 ? aEnv.biomeName(aVar, aFixed) : aEnv.biomeName(aFixed, aVar);
    }

    /** The under-plaza beacon pyramid (:305-356) — the STREETS variant: iron at H-3..H, beacons at H+1 with the same four NBT effect pairs (:313-356). */
    private static void plazaBeacon(GT6CenterFeature.Sink aSink, GT6CenterFeature.Env aEnv, int aCx, int aCz) {
        int[][] tRings = {{-5, -3}, {-4, -2}, {-3, -1}, {-2, 0}};
        for (int[] tRing : tRings) for (int i = tRing[0]; i < tRing[0] + 10; i++) for (int j = tRing[0]; j < tRing[0] + 10; j++) {
            GT6CenterFeature.put(aSink, aCx, aCz, i, GT6CenterFeature.HEIGHT + tRing[1], j, Blocks.IRON_BLOCK);
        }
        for (int i : new int[] {-1, 0}) for (int j : new int[] {-1, 0}) {
            if (aCx == Math.floorDiv(i, 16) && aCz == Math.floorDiv(j, 16)) {
                aSink.set(i, GT6CenterFeature.HEIGHT + 1, j, Blocks.BEACON.defaultBlockState()); // :313/:324/:335/:346
                int[] tEffects = GT6CenterFeature.beaconEffects(i, j);
                aEnv.beacon(i, GT6CenterFeature.HEIGHT + 1, j, tEffects[0], tEffects[1]);
            }
        }
    }

    // ---------------------------------------------------------------- roads (:418-889)

    /**
     * One road segment: the generateRoadX (:418-652) / generateRoadZ (:655-889) transposed
     * pair fused on the axis parameter. aBase = the along-axis base chunk (the aMinZ of an
     * X-road / the aMinX of a Z-road); aOwnBand = the band-owner chunk ({-1 or 0}). The
     * differing spots stay verbatim: kill-sky height 64 (X) vs 32 (Z), rail metas 0/8 (X)
     * vs 1/9 (Z), marker colors yellow/green vs blue/red, sign labels X:/Z:.
     */
    public static void road(GT6CenterFeature.Sink aSink, GT6CenterFeature.Env aEnv, Axis aAxis, int aBase,
            int aOwnBand, GT6CenterFeature.RoadMode aMode, boolean aSideWalls) {
        boolean tLand = aMode == GT6CenterFeature.RoadMode.LAND;
        boolean tKillSky = aMode == GT6CenterFeature.RoadMode.LAND || aMode == GT6CenterFeature.RoadMode.BRIDGE;
        boolean tTunnel = aMode == GT6CenterFeature.RoadMode.TUNNEL;
        boolean tBridge = aMode == GT6CenterFeature.RoadMode.BRIDGE || aMode == GT6CenterFeature.RoadMode.RING;
        int tH = GT6CenterFeature.HEIGHT;
        int tBandLo = aOwnBand == -1 ? -16 : 0, tBandHi = aOwnBand == -1 ? -1 : 15;
        BlockState tAir = Blocks.AIR.defaultBlockState();
        BlockState tGravel = Blocks.GRAVEL.defaultBlockState();
        BlockState tCobble = Blocks.COBBLESTONE.defaultBlockState();
        BlockState tAsphalt = Blocks.BLACK_CONCRETE.defaultBlockState();
        BlockState tRoadbedConcrete = GT6CenterFeature.state(GT6CenterFeature.concrete(8));
        for (int i = 0; i < 16; i++) {
            int tAlong = aBase * 16 + i;
            for (int tBand = tBandLo; tBand <= tBandHi; tBand++) {
                int tX = aAxis == Axis.X ? tBand : tAlong;
                int tZ = aAxis == Axis.X ? tAlong : tBand;
                if (tLand && Math.abs(tBand) >= 13) { // :421-428 the staggered shoulder fills
                    int tStart = switch (Math.abs(tBand)) { case 13 -> tH + 1; case 14 -> tH; case 15 -> tH - 1; default -> tH - 2; };
                    for (int y = tStart; y > 0; y--) {
                        if (!aEnv.opq(tX, y, tZ)) aSink.set(tX, y, tZ, tGravel); else break;
                    }
                }
                if (tTunnel) { // :430-437
                    if (tBand >= -12 && tBand < 12) aSink.set(tX, tH + 7, tZ, GT6CenterFeature.state(GT6CenterFeature.concrete(15)));
                    if (Math.abs(tBand) == 13) {
                        for (int k = 0; k < 7; k++) {
                            aSink.set(tX, tH + k, tZ, GT6CenterFeature.state(GT6CenterFeature.concrete(k == 3 ? 7 : 15)));
                        }
                    }
                }
                if (tBridge && (tBand == -13 || tBand == 12)) { // :438-443 — the beams ride -13/+12, NOT ±13
                    aSink.set(tX, tH, tZ, GT6CenterFeature.state(GT6CenterFeature.concrete(8)));
                    aSink.set(tX, tH + 1, tZ, GT6CenterFeature.state(GT6CenterFeature.concrete(8)));
                }
                if (tKillSky) { // :444-445/:681-682 — the 64 vs 32 height asymmetry is the verbatim pin
                    int tTop = aAxis == Axis.X ? 64 : 32;
                    if (tBand >= -13 && tBand < 13) for (int k = 2; k < tTop; k++) aSink.set(tX, tH + k, tZ, tAir);
                } else if (tBand >= -12 && tBand < 12) { // :447/:684 the partial clear
                    for (int k = 2; k < 7; k++) aSink.set(tX, tH + k, tZ, tAir);
                }
                if ((tBand >= -12 && tBand < 2) || (tBand >= 1 && tBand < 12)) { // :449-468
                    aSink.set(tX, tH + 1, tZ, tAir);
                    if (tLand) {
                        aSink.set(tX, tH - 2, tZ, tCobble);
                        aSink.set(tX, tH - 1, tZ, tGravel);
                        for (int y = tH - 3; y > 0; y--) {
                            if (!aEnv.opq(tX, y, tZ)) aSink.set(tX, y, tZ, tCobble); else break; // :454
                        }
                    } else {
                        aSink.set(tX, tH - 1, tZ, tRoadbedConcrete);
                    }
                }
                if ((tBand >= -12 && tBand < -1) || (tBand >= 1 && tBand < 12)) { // :470-480/:500-510 the asphalt lanes
                    aSink.set(tX, tH, tZ, tAsphalt);
                }
                if (tBand == -1 || tBand == 0) { // :483-498 the median
                    if (tLand) {
                        aSink.set(tX, tH - 1, tZ, tCobble);
                        aSink.set(tX, tH, tZ, tGravel);
                        aSink.set(tX, tH + 1, tZ, tGravel);
                        for (int y = tH - 2; y > 0; y--) {
                            if (!aEnv.opq(tX, y, tZ)) aSink.set(tX, y, tZ, tCobble); else break; // :489
                        }
                    } else {
                        aSink.set(tX, tH - 1, tZ, tRoadbedConcrete);
                        aSink.set(tX, tH, tZ, tRoadbedConcrete);
                        aSink.set(tX, tH + 1, tZ, tRoadbedConcrete);
                    }
                }
                if (tBand == -12 || tBand == -2 || tBand == 1 || tBand == 11) { // the lane slabs
                    aSink.set(tX, tH + 1, tZ, slab(GT6CenterFeature.cfoamSlab(), SlabType.BOTTOM)); // :470/:480/:500/:510
                }
                if (tBand == -11 || tBand == -3 || tBand == 2 || tBand == 10) { // the plain rails
                    aSink.set(tX, tH + 1, tZ, rail(aAxis, false)); // meta 0 / 1
                }
                if (tBand == -7 || tBand == 6) { // the cadence rails
                    boolean tMarked = ((i + 2) / 4) % 2 == 0; // :475 — i = the along index
                    aSink.set(tX, tH + 1, tZ, rail(aAxis, tMarked)); // meta 8/0 (X) — 9/1 (Z)
                }
            }
        }
        if (tTunnel) { // :513-522 the glowstone row
            for (int tOff : new int[] {1, 6, 9, 14}) for (int tBand : new int[] {-13, 12}) {
                int tX = aAxis == Axis.X ? tBand : aBase * 16 + tOff;
                int tZ = aAxis == Axis.X ? aBase * 16 + tOff : tBand;
                if (owns(aOwnBand, aAxis, tX, tZ)) aSink.set(tX, tH + 3, tZ, Blocks.GLOWSTONE.defaultBlockState());
            }
        }
        if (aSideWalls) sideWalls(aSink, aEnv, aAxis, aBase, aOwnBand); // :523-528
        if (tBridge) bridgeUnderside(aSink, aEnv, aAxis, aBase, aOwnBand); // :592-647 — the deck underside + the two pillar shafts
        if (levelMarker(aBase)) levelMarkerPanel(aSink, aEnv, aAxis, aBase, aOwnBand, tTunnel); // :530-548
        if (interchange(aBase)) interchangeLoop(aSink, aAxis, aBase, aOwnBand, tTunnel); // :549-600
    }

    /**
     * The bridge undercarriage (:592-647 X / the Z mirror): the deck underside at H-2/H-3
     * (along 6..9 × bands -9..-6 and 5..8, LightGray), then per pillar cluster a
     * descending search — while any of the four ground probes (bands {aProbe1,aProbe2} ×
     * along {5,10}) at the level is non-opaque, the leg shaft (bands {leg,leg+1} × along
     * {7,8}, Gray) extends; on all-opaque the 7-row cap (along 6..9 × the 4-band cluster
     * range, LightGray, rows k+3..k-3 guarded) lands and the descent stops.
     */
    private static void bridgeUnderside(GT6CenterFeature.Sink aSink, GT6CenterFeature.Env aEnv, Axis aAxis, int aBase, int aOwnBand) {
        for (int tAlong = 6; tAlong <= 9; tAlong++) for (int tBand : new int[] {-9, -8, -7, -6, 5, 6, 7, 8}) {
            putOwned(aSink, aAxis, aBase, aOwnBand, tBand, tAlong, GT6CenterFeature.HEIGHT - 2, GT6CenterFeature.concrete(7)); // :592-601
            putOwned(aSink, aAxis, aBase, aOwnBand, tBand, tAlong, GT6CenterFeature.HEIGHT - 3, GT6CenterFeature.concrete(7));
        }
        pillar(aSink, aEnv, aAxis, aBase, aOwnBand, -10, -5, -8); // :602-614 the west cluster
        pillar(aSink, aEnv, aAxis, aBase, aOwnBand, 9, 4, 6); // :615-646 the east cluster
    }

    /** One pillar cluster (:602-614 form) — aProbe1/aProbe2 the probe bands, the legs at {aLeg,aLeg+1} × along {7,8}, the cap over the probe band's 4-wide range. */
    private static void pillar(GT6CenterFeature.Sink aSink, GT6CenterFeature.Env aEnv, Axis aAxis, int aBase, int aOwnBand,
            int aProbe1, int aProbe2, int aLeg) {
        int tH = GT6CenterFeature.HEIGHT;
        int tCapLo = aLeg - 1; // the caps span the leg pair ±1 (:592 west -9..-6 over legs -8/-7; east 5..8 over 6/7)
        for (int tK = tH - 4; tK > 0; tK--) {
            boolean tGround = true;
            for (int tBand : new int[] {aProbe1, aProbe2}) for (int tAlong : new int[] {5, 10}) {
                if (!aEnv.opq(coordX(aAxis, aBase, tBand, tAlong), tK, coordZ(aAxis, aBase, tBand, tAlong))) tGround = false;
            }
            if (!tGround) {
                for (int tBand : new int[] {aLeg, aLeg + 1}) for (int tAlong : new int[] {7, 8}) {
                    putOwned(aSink, aAxis, aBase, aOwnBand, tBand, tAlong, tK, GT6CenterFeature.concrete(8)); // the Gray shaft
                }
            } else {
                for (int tAlong = 6; tAlong <= 9; tAlong++) for (int tBand = tCapLo; tBand < tCapLo + 4; tBand++) {
                    for (int tDy = 3; tDy >= -3; tDy--) {
                        if (tK > -tDy) putOwned(aSink, aAxis, aBase, aOwnBand, tBand, tAlong, tK + tDy, GT6CenterFeature.concrete(7));
                    }
                }
                break;
            }
        }
    }

    /** The ownership-guarded (band, along) write on the road axis — the decomposition seam over the transposed pair. */
    private static void putOwned(GT6CenterFeature.Sink aSink, Axis aAxis, int aBase, int aOwnBand, int aBand, int aAlong, int aY, Block aBlock) {
        int tX = coordX(aAxis, aBase, aBand, aAlong), tZ = coordZ(aAxis, aBase, aBand, aAlong);
        if (!owns(aOwnBand, aAxis, tX, tZ)) return;
        aSink.set(tX, aY, tZ, aBlock == null ? null : aBlock.defaultBlockState());
    }

    private static int coordX(Axis aAxis, int aBase, int aBand, int aAlong) {
        return aAxis == Axis.X ? aBand : aBase * 16 + aAlong;
    }

    private static int coordZ(Axis aAxis, int aBase, int aBand, int aAlong) {
        return aAxis == Axis.X ? aBase * 16 + aAlong : aBand;
    }

    /** The rail_road state — the meta mapping: 0/1 → plain straight, 8/9 → the powered-texture stand-in (class javadoc). */
    static BlockState rail(Axis aAxis, boolean aMarked) {
        Block tRail = GT6CenterFeature.roadRail();
        if (tRail == null) return null;
        RailShape tShape = aAxis == Axis.X ? RailShape.NORTH_SOUTH : RailShape.EAST_WEST;
        return tRail.defaultBlockState().setValue(PoweredRailBlock.SHAPE, tShape)
                .setValue(PoweredRailBlock.POWERED, aMarked);
    }

    /** The tunnel-approach side walls (:524-527): the quadrant patches behind the liquid/opaque probes at band ±13/∓14. */
    private static void sideWalls(GT6CenterFeature.Sink aSink, GT6CenterFeature.Env aEnv, Axis aAxis, int aBase, int aOwnBand) {
        int tH = GT6CenterFeature.HEIGHT;
        for (int tHalf = 0; tHalf < 2; tHalf++) {
            for (int tSide = 0; tSide < 2; tSide++) {
                boolean tProbe = false;
                for (int i = tHalf * 8; i < tHalf * 8 + 8; i++) {
                    int tProbeBand = tSide == 0 ? 13 : -14;
                    int tX = aAxis == Axis.X ? tProbeBand : aBase * 16 + i;
                    int tZ = aAxis == Axis.X ? aBase * 16 + i : tProbeBand;
                    if (aEnv.opq(tX, tH + 4, tZ)) { tProbe = true; break; } // :524 the probe
                }
                if (!tProbe) continue;
                int tWallBand = tSide == 0 ? 12 : -13;
                for (int j = tHalf * 8; j < tHalf * 8 + 8; j++) for (int k = 2; k < 6; k++) {
                    int tX = aAxis == Axis.X ? tWallBand : aBase * 16 + j;
                    int tZ = aAxis == Axis.X ? aBase * 16 + j : tWallBand;
                    if (!owns(aOwnBand, aAxis, tX, tZ)) continue;
                    aSink.set(tX, tH + k, tZ, even(tSide, k, j) // :524 the WD.even(0/1, k, j) checker
                            ? GT6CenterFeature.state(GT6CenterFeature.cfoam())
                            : GT6CenterFeature.state(GT6CenterFeature.concrete(7)));
                }
            }
        }
    }

    /** The 512 level-marker panels + the coordinate signs (:530-548 X, :767-785 Z). */
    private static void levelMarkerPanel(GT6CenterFeature.Sink aSink, GT6CenterFeature.Env aEnv, Axis aAxis, int aBase, int aOwnBand, boolean aTunnel) {
        int tH = GT6CenterFeature.HEIGHT;
        int tFrame = aTunnel ? 0 : 15; // tunnel black / open white
        int tFill = aAxis == Axis.X ? (aBase < 0 ? 11 : 2) : (aBase < 0 ? 4 : 1); // yellow/green — blue/red
        for (int tBand : new int[] {-13, 12}) {
            if (aOwnBand != Math.floorDiv(tBand, 16)) continue;
            for (int i = 5; i < 11; i++) for (int j = 1; j < 6; j++) {
                boolean tEdge = i == 5 || i == 10 || j == 1 || j == 5; // :532 the frame/fill pick
                int tX = aAxis == Axis.X ? tBand : aBase * 16 + i;
                int tZ = aAxis == Axis.X ? aBase * 16 + i : tBand;
                aSink.set(tX, tH + j, tZ, GT6CenterFeature.state(GT6CenterFeature.concrete(tEdge ? tFrame : tFill)));
            }
            for (int tOff : new int[] {5, 10}) for (int tK : new int[] {1, 5}) { // :540-547 the corner glowstones
                int tX = aAxis == Axis.X ? tBand : aBase * 16 + tOff;
                int tZ = aAxis == Axis.X ? aBase * 16 + tOff : tBand;
                aSink.set(tX, tH + tK, tZ, Blocks.GLOWSTONE.defaultBlockState());
            }
        }
        // the coordinate signs — lines ["", <fixed label>, <value label>, ""] (:535-538 X, :772-775 Z)
        for (int tBand : new int[] {-12, 11}) {
            if (aOwnBand != Math.floorDiv(tBand, 16)) continue;
            for (int tOff = 7; tOff <= 8; tOff++) {
                int tValue = tOff == 7 ? (aBase - 16) >> 9 : aBase >> 9;
                int tX = aAxis == Axis.X ? tBand : aBase * 16 + tOff;
                int tZ = aAxis == Axis.X ? aBase * 16 + tOff : tBand;
                int tRot;
                String tLine2, tLine3;
                if (aAxis == Axis.X) {
                    tRot = GT6CenterFeature.signRotation(tBand == -12 ? Direction.EAST : Direction.WEST);
                    tLine2 = tBand == -12 ? "X: -1" : "X: 0";
                    tLine3 = "Z: " + tValue;
                } else {
                    tRot = GT6CenterFeature.signRotation(tBand == -12 ? Direction.SOUTH : Direction.NORTH);
                    tLine2 = "X: " + tValue;
                    tLine3 = tBand == -12 ? "Z: -1" : "Z: 0";
                }
                aEnv.sign(tX, tH + 3, tZ, tRot, "", tLine2, tLine3, "");
            }
        }
    }

    /** The interchange lane-merge loops (:549-600 X, :786-837 Z) — corner slabs/NB, the clears, the asphalt connector, tunnel stubs. */
    private static void interchangeLoop(GT6CenterFeature.Sink aSink, Axis aAxis, int aBase, int aOwnBand, boolean aTunnel) {
        int tH = GT6CenterFeature.HEIGHT;
        BlockState tAir = Blocks.AIR.defaultBlockState();
        BlockState tAsphalt = Blocks.BLACK_CONCRETE.defaultBlockState();
        // the corner merge slabs — {band, along, dye | -1 = NB} (:550-557 entry, :570-577 exit)
        int[][] tCorners = {
                {-2, 0, 15}, {-2, 1, 1}, {-2, 2, -1}, {-1, 2, 15}, {0, 2, 1}, {1, 2, -1}, {1, 1, 15}, {1, 0, 1},
                {-2, 15, 1}, {-2, 14, 15}, {-2, 13, -1}, {-1, 13, 1}, {0, 13, 15}, {1, 13, -1}, {1, 14, 1}, {1, 15, 15}};
        for (int[] tC : tCorners) {
            int tX = aAxis == Axis.X ? tC[0] : aBase * 16 + tC[1];
            int tZ = aAxis == Axis.X ? aBase * 16 + tC[1] : tC[0];
            if (!owns(aOwnBand, aAxis, tX, tZ)) continue;
            BlockState tState = tC[2] < 0 ? tAir : slab(GT6CenterFeature.concreteSlab(tC[2]), SlabType.BOTTOM);
            aSink.set(tX, tH + 1, tZ, tState);
        }
        for (int i = 2; i < 14; i++) for (int tBand : new int[] {-1, 0}) { // :559-562 the asphalt connector
            int tX = aAxis == Axis.X ? tBand : aBase * 16 + i;
            int tZ = aAxis == Axis.X ? aBase * 16 + i : tBand;
            if (owns(aOwnBand, aAxis, tX, tZ)) aSink.set(tX, tH, tZ, tAsphalt);
        }
        for (int i = 3; i < 13; i++) for (int tBand = -2; tBand <= 1; tBand++) { // :563-568 the clears
            int tX = aAxis == Axis.X ? tBand : aBase * 16 + i;
            int tZ = aAxis == Axis.X ? aBase * 16 + i : tBand;
            if (owns(aOwnBand, aAxis, tX, tZ)) aSink.set(tX, tH + 1, tZ, tAir);
        }
        if (aTunnel) { // the tunnel stubs (:579-589 boundary chunks, :592-599 mid-segment)
            int[][] tStubs = levelMarker(aBase)
                    ? new int[][] {{0, 0}, {0, 1}, {1, 0}, {1, 1}, {14, 0}, {14, 1}, {15, 0}, {15, 1}}
                    : new int[][] {{7, 0}, {7, 1}, {8, 0}, {8, 1}};
            for (int[] tStub : tStubs) for (int k = 0; k < 7; k++) {
                int tX = aAxis == Axis.X ? tStub[1] : aBase * 16 + tStub[0];
                int tZ = aAxis == Axis.X ? aBase * 16 + tStub[0] : tStub[1];
                if (!owns(aOwnBand, aAxis, tX, tZ)) continue;
                aSink.set(tX, tH + k, tZ, GT6CenterFeature.state(GT6CenterFeature.concrete(k == 3 ? 7 : 15)));
            }
        }
    }

    private static BlockState slab(Block aBlock, SlabType aType) {
        if (aBlock == null) return null;
        BlockState tState = aBlock.defaultBlockState();
        return !tState.hasProperty(SlabBlock.TYPE) ? tState : tState.setValue(SlabBlock.TYPE, aType);
    }
}
