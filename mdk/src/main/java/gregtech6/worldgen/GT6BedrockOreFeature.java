package gregtech6.worldgen;

import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import gregapi.oredict.OreDictMaterial;
import gregtech6.block.ore.GTBedrockOreBlock;
import gregtech6.registry.GT6BedrockOreBlocks;
import gregtech6.registry.GT6OreBlocks;
import gregtech6.registry.GT6SurfaceBlocks;

/**
 * The bedrock-ore Feature (task bedrock-ore-worldgen spec ②) — the per-chunk adapter
 * around {@link GT6BedrockOreGenerator}, the {@link GT6StrataLensFeature} isomorphic shape
 * minus the origin grid: the bedrock ore has NO lattice, every chunk rolls its own rows on
 * its own coordinate-seeded stream ({@link GT6VeinGenerator#veinRandom} — the decision-level
 * determinism the strata card pinned; the placed-feature random is NOT used at all).
 *
 * <p><b>The work chunk</b>: the placed feature runs Count(1)+InSquare+BiomeFilter, one
 * attempt per chunk; the work chunk is the REGION CENTER (WorldGenRegion.getCenter(), the
 * strata form) — the /place command path hands a ServerLevel and falls back to the aimed
 * chunk. All writes stay inside that chunk by construction (the patch/muffin bounds are
 * 0..16, the tail walk breaks at the 1..14 ring — the research card's single-chunk face).
 *
 * <p><b>Placement</b> = the WD.setOre/setSmallOre host-skin semantics (WD.java:744-759/
 * :765-780): the muffin/tail ore blocks are the HOST stone's own family from
 * {@link GT6OreBlocks#stoneToOreFamilies()} — the just-written muffin shell is deepslate so
 * the muffin ore rides the deepslate family, the tails ride whatever stone they cross;
 * unmapped hosts (air, caves, the vanilla replaceables granite/diorite/andesite that map to
 * no GT family) are skipped, upstream same. The bedrock-face patch replaces the floor
 * unconditionally within its 6x6 (the upstream placeBlock force, y=0 -> -64).
 *
 * <p><b>The dimension face</b> (task worldgen-nether-bedrock-lava): the nether modifier row
 * ({@code gt6:nether_bedrock_ores} over {@code #minecraft:is_nether}) hangs the SAME placed
 * feature in the nether, and the feature routes itself — {@code dimensionType().hasCeiling()}
 * (the upstream WD.waterLevel {@code hasNoSky} face, WD.java:430) picks the row mask (the
 * seven GEN_NETHER rows :758-764), the NETHER stream salt, the nether's own bedrock floor
 * (y=0, the flat layer) and water line (tails to 30), and the NETHERRACK muffin shell
 * (WorldgenOresBedrock.java:196 "Use Deepslate if available, except in the Nether"; the
 * netherrack host resolves its own ore family, GT6OreBlocks FAMILIES).
 *
 * <p><b>The indicator flowers</b> (task worldgen-flower-arm): every hit row with a flower
 * column runs {@link GT6BedrockOreGenerator#generateFlowers} right after its vein on the
 * same stream (:141-178 order), and the {@code flower} sink callback below does the
 * surface column scan (y 140..62) + place/canSurvive/retract — the findability arm of
 * WorldgenOresBedrock.java:147-179. The rocks arm (MTE 32757) stays the declared OUT.
 *
 * <p>KJS face (card declaration): the 46-row table is datapack JSON (the configured-feature
 * config); this class is the registry face, out of KJS scope (GT6Features javadoc clause).
 */
public class GT6BedrockOreFeature extends Feature<GTBedrockOreConfig.Table> {

    public GT6BedrockOreFeature() {
        super(GTBedrockOreConfig.Table.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<GTBedrockOreConfig.Table> aContext) {
        WorldGenLevel tLevel = aContext.level();
        ChunkPos tWork = tLevel instanceof WorldGenRegion ? ((WorldGenRegion) tLevel).getCenter()
                : new ChunkPos(aContext.origin());
        // the dimension face (see the class javadoc): hasCeiling = the nether, the WD.java:430 hasNoSky face
        boolean tNether = tLevel.dimensionType().hasCeiling();
        Random tRandom = GT6VeinGenerator.veinRandom(tLevel.getSeed(),
                tNether ? GT6VeinGenerator.NETHER_DIMENSION_SALT : GT6VeinGenerator.OVERWORLD_DIMENSION_SALT,
                tWork.x, tWork.z);
        List<GTBedrockOreConfig> tHits = GT6BedrockOreGenerator.drawRows(aContext.config(), tRandom, tNether);
        if (tHits.isEmpty()) return false;
        Map<Block, GT6OreBlocks.OreFamily> tHosts = GT6OreBlocks.stoneToOreFamilies();
        int tFloor = Math.max(GT6BedrockOreGenerator.BEDROCK_Y, tLevel.getMinBuildHeight()); // the nether's own y=0 floor
        Block tShell = tNether ? Blocks.NETHERRACK : Blocks.DEEPSLATE; // :196 "except in the Nether"
        GT6BedrockOreGenerator.BedrockSink tSink = levelSink(tLevel, tHosts, tFloor, tShell);
        boolean rPlaced = false;
        for (GTBedrockOreConfig tRow : tHits) {
            boolean tVein = GT6BedrockOreGenerator.generateVein(tRow, tRandom, tWork.getMinBlockX(), tWork.getMinBlockZ(),
                    tLevel.getMinBuildHeight(),
                    tNether ? GT6BedrockOreGenerator.NETHER_TAIL_TOP_Y : GT6BedrockOreGenerator.TAIL_TOP_Y, tSink);
            if (tVein) { // :143 — the indicator loop only runs after a placed vein (:141-178 order)
                GT6BedrockOreGenerator.generateFlowers(tRow, tRandom, tWork.getMinBlockX(), tWork.getMinBlockZ(), tSink);
            }
            rPlaced |= tVein;
        }
        return rPlaced;
    }

    private GT6BedrockOreGenerator.BedrockSink levelSink(WorldGenLevel aLevel, Map<Block, GT6OreBlocks.OreFamily> aHosts,
            int aFloor, Block aShell) {
        return new GT6BedrockOreGenerator.BedrockSink() {
            @Override
            public boolean isBedrockFace(int aX, int aZ) {
                Block tBlock = aLevel.getBlockState(new BlockPos(aX, aFloor, aZ)).getBlock();
                return tBlock == Blocks.BEDROCK || tBlock instanceof GTBedrockOreBlock; // :185 (the idempotent re-place face)
            }

            @Override
            public void bedrockOre(int aX, int aY, int aZ, OreDictMaterial aMaterial, boolean aSmall) {
                Block tBlock = bedrockBlock(aMaterial, aSmall);
                if (tBlock != null) aLevel.setBlock(new BlockPos(aX, aY, aZ), tBlock.defaultBlockState(), 2);
            }

            @Override
            public void shell(int aX, int aY, int aZ) {
                aLevel.setBlock(new BlockPos(aX, aY, aZ), aShell.defaultBlockState(), 2); // :203, the unconditional face (deepslate ow / netherrack nether :196)
            }

            @Override
            public void ore(int aX, int aY, int aZ, OreDictMaterial aMaterial, boolean aSmall) {
                BlockPos tPos = new BlockPos(aX, aY, aZ);
                GT6OreBlocks.OreFamily tFamily = aHosts.get(aLevel.getBlockState(tPos).getBlock()); // WD.java:750/:771
                if (tFamily == null) return;
                var tHandle = GT6OreBlocks.get(tFamily,
                        aSmall ? GT6OreBlocks.FormKind.SMALL : GT6OreBlocks.FormKind.NORMAL, aMaterial);
                if (tHandle == null) return;
                aLevel.setBlock(tPos, tHandle.get().defaultBlockState(), 2);
            }

            /**
             * The indicator-flower column scan (task worldgen-flower-arm; the live face of
             * WorldgenOresBedrock.java:157-172). Walk y 140 down to 63 (:157, the
             * {@link GT6BedrockOreGenerator#FLOWER_MIN_Y}/{@link #FLOWER_MAX_Y} window):
             * liquid/farmland gives up (:159), non-opaque/wood/leaves scans lower (:160),
             * a non-easyReplaceable block above gives up (:161), then the flower tries on
             * any non-dirt contact (:162 — the rocks arm is the declared cut, so no
             * nextInt(4) draw): place, keep it when {@code canSurvive} (:164, the
             * canBlockStay face — A-group dirt/grass vs B-group sand soils via the
             * GT6FlowerBlock split), retract to air otherwise (:165).
             */
            @Override
            public void flower(int aX, int aZ, String aFlower) {
                Block tFlower = flowerBlock(aFlower);
                if (tFlower == null) return; // the unknown-id posture (the MT.NULL row face)
                BlockState tFlowerState = tFlower.defaultBlockState();
                for (int tY = GT6BedrockOreGenerator.FLOWER_MAX_Y; tY > GT6BedrockOreGenerator.FLOWER_MIN_Y; tY--) {
                    BlockPos tPos = new BlockPos(aX, tY, aZ);
                    BlockState tContact = aLevel.getBlockState(tPos);
                    if (!tContact.getFluidState().isEmpty() || tContact.is(Blocks.FARMLAND)) break; // :159
                    if (!tContact.isSolidRender(aLevel, tPos) || tContact.is(BlockTags.LOGS)
                            || tContact.is(BlockTags.LEAVES)) continue; // :160
                    BlockPos tAbove = tPos.above();
                    BlockState tOver = aLevel.getBlockState(tAbove);
                    // :161 WD.easyRep — air/replaceable (snow layer, plants) + the leaves face
                    if (!tOver.isAir() && !tOver.canBeReplaced() && !tOver.is(BlockTags.LEAVES)
                            && !tOver.is(BlockTags.FLOWERS)) break;
                    if (!tContact.is(Blocks.DIRT)) { // :162 — flowers always try, the declared density deviation
                        aLevel.setBlock(tAbove, tFlowerState, 2); // :163
                        if (tFlowerState.canSurvive(aLevel, tAbove)) break; // :164
                        aLevel.setBlock(tAbove, Blocks.AIR.defaultBlockState(), 2); // :165 the retract face
                    }
                    break; // :171
                }
            }
        };
    }

    /** The registered flower block of a GT6SurfaceBlocks.FLOWER_SPECS snake id, or null (the invalid-slot posture). */
    private static Block flowerBlock(String aFlower) {
        for (int i = 0; i < GT6SurfaceBlocks.FLOWER_SPECS.size(); i++) {
            if (GT6SurfaceBlocks.FLOWER_SPECS.get(i).snake().equals(aFlower)) return GT6SurfaceBlocks.FLOWERS.get(i).get();
        }
        return null;
    }

    /** The registered bedrock block of a (material, form) pair, or null (the invalid-row posture). */
    private static Block bedrockBlock(OreDictMaterial aMaterial, boolean aSmall) {
        return GT6BedrockOreBlocks.get(aSmall, aMaterial) == null
                ? null : GT6BedrockOreBlocks.get(aSmall, aMaterial).get();
    }
}
