package gregtech6.worldgen;

import java.util.List;
import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import gregtech6.block.stone.StoneVariant;
import gregtech6.registry.GTStoneBlocks;

/**
 * The nether stone-lens Feature (task p31-nether-lens-end-yield) — the
 * {@link GT6StrataLensFeature} per-chunk adapter re-formed for the 17-stone nether table,
 * carrying TWO upstream deltas:
 *
 * <ul>
 * <li><b>The draw is the independent-row face</b> ({@link GT6LensGenerator#drawIndependentLenses}):
 * upstream nether stones are 17 SEPARATE WorldgenObjects each rolling its own
 * {@code nextInt(200) == 0} per chunk (Loader_Worldgen.java:656 + WorldgenBlob.java:68),
 * not the strata lens' exactly-one weighted pick — so one chunk can grow several stones,
 * and every scan window sees ~17/200 lens hits per rolling chunk (rate fidelity:
 * a 48x48 chunk census holds ~196 lenses, all 17 stones observable).</li>
 * <li><b>The host is netherrack only</b> (WorldgenStone.java:50
 * {@code isReplaceableOreGen(Blocks.netherrack)} — the nether row's host face; the
 * overworld lens' {@code #stone_ore_replaceables} tag has no nether member and the
 * GT-stone-overridable arm is the stone-layer card's domain).</li>
 * </ul>
 *
 * <p><b>The stream</b>: upstream {@code WD.random(World)} XORs the dimension id into the
 * seed (WD.java:547, the prospecting-exploit comment) — this feature passes the nether
 * salt -1 ({@link #dimensionSalt}, the 1.7.10 numeric id), so the nether draws never
 * align with an overworld world's. The coordinate-seeded stream + the pure-geometry
 * {@link GT6LensGenerator#generateSlice} keep the decision-level determinism
 * (decisions.2026-09-18-p31-strata-lens-determinism-acceptance): any two chunks
 * recomputing an origin derive bit-identical slices by construction.
 *
 * <p><b>The window</b> is the strata lens' ±3 chunks (radius 40 + the 16-block center
 * spread: a 4-chunk-away origin's nearest reach is 64-40 = 24 &gt; 15 blocks past the
 * near edge — out of the work chunk; ±3 is exact). The Y writes ride an
 * out-of-build-height guard: a center up to y120 plus the 12 half-height reaches past
 * the 128-tall nether ceiling, where 1.7.10 silently no-op'd and a WorldGenRegion
 * setBlock would crash.
 *
 * <p>KJS face (card declaration): the 17-row table is datapack JSON (the
 * configured-feature config); this class is the registry face, out of KJS scope
 * (GT6Features javadoc clause).
 */
public class GT6NetherLensFeature extends Feature<GTLensConfig.Table> {

    /** The origin scan half-width: radius 40 + the 16-block center spread stays inside ±3 chunks (see class javadoc). */
    public static final int SCAN_RADIUS_CHUNKS = 3;

    /** The nether legacy dimension id (the 1.7.10 provider.dimensionId the upstream seed XORed). */
    public static final long NETHER_DIMENSION_SALT = -1;
    /** The end legacy dimension id (the End large-vein draw salts the same stream). */
    public static final long END_DIMENSION_SALT = 1;
    /** The overworld legacy dimension id — the zero salt, the pre-salt stream verbatim. */
    public static final long OVERWORLD_DIMENSION_SALT = 0;

    public GT6NetherLensFeature() {
        super(GTLensConfig.Table.CODEC);
    }

    /**
     * The legacy numeric dimension id of a level, the {@code WD.random(World)} salt face:
     * overworld 0, nether -1, end 1 (the vanilla dimension keys' legacy ids — any mod
     * dimension salts 0, matching upstream's dim-type switch default arm). The concrete
     * level carries {@code dimension()} (Level.java:862): the natural path hands a
     * WorldGenRegion (getLevel() → the ServerLevel), /place hands the ServerLevel itself.
     */
    public static long dimensionSalt(WorldGenLevel aLevel) {
        Level tConcrete = aLevel instanceof Level tLevel ? tLevel : ((WorldGenRegion) aLevel).getLevel();
        ResourceKey<Level> tDim = tConcrete.dimension();
        if (tDim == Level.NETHER) return NETHER_DIMENSION_SALT;
        if (tDim == Level.END) return END_DIMENSION_SALT;
        return OVERWORLD_DIMENSION_SALT;
    }

    @Override
    public boolean place(FeaturePlaceContext<GTLensConfig.Table> aContext) {
        WorldGenLevel tLevel = aContext.level();
        ChunkPos tWork = tLevel instanceof WorldGenRegion ? ((WorldGenRegion) tLevel).getCenter()
                : new ChunkPos(aContext.origin());
        long tSalt = dimensionSalt(tLevel);
        int tMinBuildY = tLevel.getMinBuildHeight(), tMaxBuildY = tLevel.getMaxBuildHeight() - 1;
        boolean rPlaced = false;
        int tMinX = tWork.getMinBlockX(), tMaxX = tMinX + 15;
        int tMinZ = tWork.getMinBlockZ(), tMaxZ = tMinZ + 15;
        for (int tDX = -SCAN_RADIUS_CHUNKS; tDX <= SCAN_RADIUS_CHUNKS; tDX++) {
            for (int tDZ = -SCAN_RADIUS_CHUNKS; tDZ <= SCAN_RADIUS_CHUNKS; tDZ++) {
                int tOriginX = tWork.x + tDX, tOriginZ = tWork.z + tDZ;
                // EVERY chunk is a potential rolling origin (the upstream per-chunk gate is the
                // 1/200 roll itself, not an origin grid) — the window walks all 7x7 neighbors.
                Random tRandom = GT6VeinGenerator.veinRandom(tLevel.getSeed(), tSalt, tOriginX, tOriginZ);
                List<GTLensConfig> tHits = GT6LensGenerator.drawIndependentLenses(aContext.config(), tRandom);
                for (GTLensConfig tLens : tHits) {
                    rPlaced |= placeSlice(tLevel, tLens, tRandom, tOriginX << 4, tOriginZ << 4,
                            tMinX, tMaxX, tMinZ, tMaxZ, tMinBuildY, tMaxBuildY);
                }
            }
        }
        return rPlaced;
    }

    /**
     * One origin's slice against the live level: the netherrack host gate (the nether row
     * of WorldgenStone.java:50) plus the build-height guard (see class javadoc). An unknown
     * stone snake skips without placing (the GTLensConfig invalid-row posture).
     */
    private boolean placeSlice(WorldGenLevel aLevel, GTLensConfig aLens, Random aRandom, int aOriginMinX,
            int aOriginMinZ, int aClipMinX, int aClipMaxX, int aClipMinZ, int aClipMaxZ,
            int aMinBuildY, int aMaxBuildY) {
        Block tBlock = resolveStone(aLens.stone());
        if (tBlock == null) return false;
        net.minecraft.world.level.block.state.BlockState tState = tBlock.defaultBlockState();
        return GT6LensGenerator.generateSlice(aLens, aRandom, aOriginMinX, aOriginMinZ,
                aClipMinX, aClipMaxX, aClipMinZ, aClipMaxZ, (aX, aY, aZ) -> {
                    if (aY < aMinBuildY || aY > aMaxBuildY) return; // the nether ceiling/bottom clip
                    BlockPos tPos = new BlockPos(aX, aY, aZ);
                    if (aLevel.getBlockState(tPos).getBlock() == Blocks.NETHERRACK) { // WorldgenStone.java:50
                        aLevel.setBlock(tPos, tState, 2);
                    }
                });
    }

    /** The STONE-variant block of a stone snake, or null for an unknown row (no datapack crash face). */
    private Block resolveStone(String aSnake) {
        return GTStoneBlocks.block(aSnake, StoneVariant.STONE) == null
                ? null : GTStoneBlocks.block(aSnake, StoneVariant.STONE).get();
    }
}
