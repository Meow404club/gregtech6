package gregtech6.worldgen;

import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import gregtech6.block.stone.StoneVariant;
import gregtech6.registry.GTStoneBlocks;

/**
 * The strata-lens Feature (task p31-strata-lens) — the {@link GT6LargeVeinFeature}
 * isomorphic per-chunk adapter around {@link GT6LensGenerator} (itself the
 * {@link GT6VeinGenerator} core re-formed for the 5-row marker-stone table). Kept
 * class-separated from the math so the offline tests drive it without class-loading
 * vanilla {@code Feature} (the entity-registry clinit trap, GT6LargeVeinTest posture).
 *
 * <p><b>The scheme</b> (the p30-w6 vein research, verdict a, carried over): EVERY chunk,
 * at its single place() call, scans the ±3-chunk origin window, re-derives each origin's
 * lens from the origin-seeded stream, and writes only its own chunk's slice — both sides
 * of a border compute the same lens, no master/owner race, no seam. The window is
 * ±3 chunks because the codec caps the radius at 48 ({@link GTLensConfig#RADIUS_CODEC_CAP})
 * and the center stays inside the origin chunk: reach &lt; 64 blocks = 4 chunks.
 *
 * <p><b>The work chunk</b>: the placed feature runs Count(1)+InSquare+BiomeFilter, one
 * attempt per chunk; the work chunk is the REGION CENTER (WorldGenRegion.getCenter(),
 * WorldGenRegion.java:108 both legs) — the /place command path hands a ServerLevel, not a
 * region, and falls back to the origin's chunk (the same chunk the command aimed at).
 *
 * <p><b>Placement</b> = the blob's host semantics: the lens stone replaces ONLY the
 * vanilla {@code #minecraft:stone_ore_replaceables} family (stone/granite/diorite/
 * andesite — the same tag the L0 blob's TagMatchTest gates, GT6WorldgenDatagen
 * bootstrapConfigured), never air/caves/ores/deepslate — the lens bottom ends naturally
 * at the deepslate band, the modern mNoDeep face (research.p30-w6-deepslate-strata).
 * Multi-origin overlaps resolve deterministically: every position has exactly one writing
 * chunk and that chunk walks the origin window in the pinned order.
 *
 * <p>KJS face (card declaration): the lens table is datapack JSON (the configured-feature
 * config); this class is the registry face, out of KJS scope (GT6Features javadoc clause).
 */
public class GT6StrataLensFeature extends Feature<GTLensConfig.Table> {

    /** The origin scan half-width: radius cap 48 + the 16-block center spread &lt; 4 chunks. */
    public static final int SCAN_RADIUS_CHUNKS = 3;

    public GT6StrataLensFeature() {
        super(GTLensConfig.Table.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<GTLensConfig.Table> aContext) {
        WorldGenLevel tLevel = aContext.level();
        ChunkPos tWork = tLevel instanceof WorldGenRegion ? ((WorldGenRegion) tLevel).getCenter()
                : new ChunkPos(aContext.origin());
        boolean rPlaced = false;
        int tMinX = tWork.getMinBlockX(), tMaxX = tMinX + 15;
        int tMinZ = tWork.getMinBlockZ(), tMaxZ = tMinZ + 15;
        for (int tDX = -SCAN_RADIUS_CHUNKS; tDX <= SCAN_RADIUS_CHUNKS; tDX++) {
            for (int tDZ = -SCAN_RADIUS_CHUNKS; tDZ <= SCAN_RADIUS_CHUNKS; tDZ++) {
                int tOriginX = tWork.x + tDX, tOriginZ = tWork.z + tDZ;
                if (!GT6LensGenerator.isLensOriginCell(tOriginX) || !GT6LensGenerator.isLensOriginCell(tOriginZ)) {
                    continue;
                }
                Random tRandom = GT6VeinGenerator.veinRandom(tLevel.getSeed(), GT6NetherLensFeature.OVERWORLD_DIMENSION_SALT, tOriginX, tOriginZ);
                GTLensConfig tLens = GT6LensGenerator.drawLens(aContext.config(), tRandom);
                if (tLens == null) continue;
                rPlaced |= placeSlice(tLevel, tLens, tRandom, tOriginX << 4, tOriginZ << 4,
                        tMinX, tMaxX, tMinZ, tMaxZ);
            }
        }
        return rPlaced;
    }

    /**
     * One origin's slice against the live level: the host-gated stone sink (the blob's
     * TagMatchTest(stone_ore_replaceables) face as a runtime state check). The stone block
     * resolves per row through the STONE-variant registry handle — an unknown snake row
     * skips without placing (the GTVeinConfig MT.NULL invalid-row posture); at worldgen
     * time the handle is always bound (features only run in a loaded, registered world).
     */
    private boolean placeSlice(WorldGenLevel aLevel, GTLensConfig aLens, Random aRandom, int aOriginMinX,
            int aOriginMinZ, int aClipMinX, int aClipMaxX, int aClipMinZ, int aClipMaxZ) {
        Block tBlock = resolveStone(aLens.stone());
        if (tBlock == null) return false;
        net.minecraft.world.level.block.state.BlockState tState = tBlock.defaultBlockState();
        return GT6LensGenerator.generateSlice(aLens, aRandom, aOriginMinX, aOriginMinZ,
                aClipMinX, aClipMaxX, aClipMinZ, aClipMaxZ, (aX, aY, aZ) -> {
                    BlockPos tPos = new BlockPos(aX, aY, aZ);
                    if (aLevel.getBlockState(tPos).is(BlockTags.STONE_ORE_REPLACEABLES)) {
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
