package gregtech6.worldgen;

import java.util.Map;
import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import gregapi.oredict.OreDictMaterial;
import gregtech6.block.surface.GT6SurfaceRockBlock;
import gregtech6.registry.GT6OreBlocks;
import gregtech6.registry.GT6SurfaceBlocks;

/**
 * The L1 large-vein Feature (task p30-w6-t3-large-veins) — the per-chunk adapter around
 * {@link GT6VeinGenerator} (the deterministic WorldgenOresLarge core; kept class-separated
 * so the offline tests drive the math without class-loading vanilla {@code Feature}, whose
 * clinit drags the entity registry that is unbootstrappable headless).
 *
 * <p><b>The scheme</b> (research.p30-w6-vein-boundary, verdict a). Upstream generates each
 * vein ONCE per chunk while scanning a 5x5 chunk neighborhood for 3x3-grid origin cells
 * ({@code ((cc+402653184)%3)==1} on both axes, GT6WorldGenerator.java:95-105) and
 * re-derives the vein at each origin from {@code WD.random(seed^dim, origin)} (:92/:98,
 * WD.java:547-560) — the vein rectangle (origin ± randomSize, WorldgenOresLarge.java:111-112,
 * reach 31/46 blocks) crosses 2-3 chunks while a FEATURES step may only WRITE its center
 * ±1 chunk (1.20.1 ChunkStatus.java:133 writeRadiusCutoff=1; out-of-radius setBlock is a
 * SILENT false, WorldGenRegion.java:251-285), so "the origin chunk writes the whole vein"
 * would silently drop two thirds of it. The modern translation: EVERY chunk, at its single
 * place() call, scans the same 5x5 origin grid, re-derives the full vein from the
 * origin-seeded stream, and writes only its own chunk's slice — both sides of a border
 * compute the same vein, no master/owner race.
 *
 * <p><b>The declared strengthening</b> (research.p30-w6-vein-boundary ③): upstream seeded
 * only the vein PICK and tMinY by the origin while the shape/indicator draws ran on the
 * CURRENT chunk's random (GT6WorldGenerator.java:54 mRandom — its own edges are not
 * bit-consistent across the recomputing chunks). Here the SINGLE stream — pick, tMinY,
 * indicators, shape, in upstream draw order — is origin-seeded, so every recomputing chunk
 * derives bit-identical slices. Two stream-hygiene consequences, both upstream-invisible:
 * the indicator draws are consumed even when the >=64-block ring gate (WorldgenOresLarge
 * .java:94, the GENERATE_STREETS branch; modern has no streets flag, the streets-on branch
 * is the declared default) suppresses placement, and the distance gate (:90) gates the
 * writing chunk BEFORE its draws (upstream verbatim) — in both cases every WRITING chunk
 * still consumes the identical sequence, which is what bit-consistency needs.
 *
 * <p>Placement = the WD.setOre host-skin semantics (WD.java:744-759): the ore block is the
 * host stone's own family from {@link GT6OreBlocks#stoneToOreFamilies()} — vanilla stone
 * hosts get the stone family, deepslate hosts the deepslate family (the
 * research.p30-w6-deepslate-strata plan-a layering y&gt;=8 stone-form / y&lt;0 deepslate-form
 * falls out of the host for free), the 17 GT blob stones their GT family; unmapped hosts
 * (granite/diorite/andesite/tuff, air) are skipped, upstream same. The indicator MTE
 * (32757) is the spec ⑤ declared deviation: the rocks-sticks {@link GT6SurfaceRockBlock}
 * objects stand in, one per valid vein material ({@link GT6SurfaceBlocks} surface_rock_*
 * rows), invalid picks falling back to the default rock exactly like upstream's NBT-less
 * 1/3 arm.
 *
 * <p>KJS face (card declaration): the vein table is datapack JSON (the configured-feature
 * config); this class is the registry face, out of KJS scope (GT6Features javadoc clause).
 */
public class GT6LargeVeinFeature extends Feature<GTVeinConfig.Table> {

    public GT6LargeVeinFeature() {
        super(GTVeinConfig.Table.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<GTVeinConfig.Table> aContext) {
        WorldGenLevel tLevel = aContext.level();
        // the placed-feature call is exactly one per chunk; the InSquare spread keeps the
        // origin inside the work chunk, and /place invocations land the same way
        ChunkPos tWork = new ChunkPos(aContext.origin());
        // the dimension's row set: the End modifier hangs the SAME placed feature on
        // #is_end (its conditions are the has_planet_veins yield gate), so the draw
        // filters ORE_END rows there — the biome probe is the runtime face of the
        // upstream per-dim flag lists (GT6WorldGenerator.java:93 + the :126-127 switch).
        boolean tEndRows = tLevel.getBiome(aContext.origin()).is(net.minecraft.tags.BiomeTags.IS_END);
        Map<Block, GT6OreBlocks.OreFamily> tHosts = GT6OreBlocks.stoneToOreFamilies();
        GT6VeinGenerator.SliceSink tSink = levelSink(tLevel, tHosts);
        boolean rPlaced = false;
        for (int tDX = -2; tDX <= 2; tDX++) for (int tDZ = -2; tDZ <= 2; tDZ++) {
            int tOriginX = tWork.x + tDX, tOriginZ = tWork.z + tDZ;
            if (!GT6VeinGenerator.isOriginCell(tOriginX) || !GT6VeinGenerator.isOriginCell(tOriginZ)) continue;
            Random tRandom = GT6VeinGenerator.veinRandom(tLevel.getSeed(), GT6NetherLensFeature.dimensionSalt(tLevel), tOriginX, tOriginZ);
            GTVeinConfig tVein = GT6VeinGenerator.drawVein(aContext.config().veins(), tRandom, tEndRows);
            if (tVein == null) continue;
            rPlaced |= GT6VeinGenerator.generateSlice(tVein, tRandom, tOriginX << 4, tOriginZ << 4,
                    tWork.getMinBlockX(), tWork.getMinBlockX() + 15, tWork.getMinBlockZ(), tWork.getMinBlockZ() + 15,
                    tLevel.getMinBuildHeight(), tSink);
        }
        return rPlaced;
    }

    private GT6VeinGenerator.SliceSink levelSink(WorldGenLevel aLevel, Map<Block, GT6OreBlocks.OreFamily> aHosts) {
        return new GT6VeinGenerator.SliceSink() {
            @Override
            public void ore(int aX, int aY, int aZ, OreDictMaterial aMaterial) {
                BlockPos tPos = new BlockPos(aX, aY, aZ);
                GT6OreBlocks.OreFamily tFamily = aHosts.get(aLevel.getBlockState(tPos).getBlock()); // WD.java:750
                if (tFamily == null) return;
                Block tBlock = GT6OreBlocks.get(tFamily, GT6OreBlocks.FormKind.NORMAL, aMaterial).get();
                aLevel.setBlock(tPos, tBlock.defaultBlockState(), 2);
            }

            @Override
            public void indicatorRock(int aX, int aY, int aZ, OreDictMaterial aMaterial) {
                Block tBlock = GT6SurfaceBlocks.indicatorRock(aMaterial);
                aLevel.setBlock(new BlockPos(aX, aY, aZ),
                        tBlock.defaultBlockState().setValue(GT6SurfaceRockBlock.FACING, Direction.DOWN), 2);
            }

            @Override
            public int surfaceHeight(int aX, int aZ) {
                return aLevel.getHeight(Heightmap.Types.WORLD_SURFACE, aX, aZ);
            }

            @Override
            public int probe(int aX, int aY, int aZ) {
                BlockPos tPos = new BlockPos(aX, aY, aZ);
                BlockState tState = aLevel.getBlockState(tPos);
                if (!tState.getFluidState().isEmpty()) return PROBE_LIQUID; // :101 — checked BEFORE opacity
                if (tState.isAir() || !tState.canOcclude()) return PROBE_PASS; // :102 isOpaqueCube
                if (tState.is(BlockTags.DIRT) || aHosts.containsKey(tState.getBlock())) return PROBE_SUITABLE;
                return PROBE_OTHER;
            }

            @Override
            public boolean replaceable(int aX, int aY, int aZ) {
                return aLevel.getBlockState(new BlockPos(aX, aY, aZ)).canBeReplaced(); // WD.easyRep
            }
        };
    }
}
