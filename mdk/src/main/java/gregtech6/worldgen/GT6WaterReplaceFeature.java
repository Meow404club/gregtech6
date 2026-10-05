package gregtech6.worldgen;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import gregtech6.fluid.GTFluids;

/**
 * The vanilla-water replacement Feature (task worldgen-water-replace) — the chunk-scan
 * adapter over the 3-row {@link GT6WaterReplaceConfig.Table}, the GT6FluidSpringFeature
 * isomorphic shape: one placed feature per chunk, rows iterate in table order.
 *
 * <p><b>THE SELECTION REPORT (card acceptance ① — route A, the feature/biome-modifier
 * JSON route):</b> upstream is a chunk-decorate WorldgenObject (Loader_Worldgen.java
 * :576-578) — the port's one existing worldgen shape is configured/placed feature +
 * biome modifier (GT6WorldgenDatagen), so a Feature at decoration IS the upstream
 * timing. Route B (ChunkEvent.Load) needs first-load flag storage (chunk persistent
 * data) for zero precedent; route C (noise/carver injection) needs vanilla generator
 * patching, foreign to this repo (zero mixins — the GTCEu OreConfigurationMixin shape
 * was explicitly not the port's posture, state research.r6-32-33-worldgen). The step is
 * {@code TOP_LAYER_MODIFICATION} (the last decoration step), the load-bearing choice:
 * <ul>
 * <li>AFTER vegetal — seagrass/kelp gate on strict {@code Blocks.WATER}
 *     (SeagrassFeature.java:30/:37, KelpFeature.java:26/:32): replacing earlier would
 *     sterilize ocean vegetation; after, the plants stand and the scan passes
 *     non-occluding plants while converting the water around them (upstream leaves were
 *     passed the same way, the :64 break only fires on occluding blocks).</li>
 * <li>AFTER freeze_top_layer — frozen-river surface ice is SnowAndFreezeFeature (the
 *     vanilla inline feature at this very step) gated on strict
 *     {@code fluid == Fluids.WATER} (Biome.java:141): replacing earlier would end
 *     frozen-river ice; after, the ice cap sits and the water below converts. Vanilla
 *     inline features precede modifier-appended ones in the same step's list (vanilla
 *     features live in the biome JSON arrays; AddFeaturesBiomeModifier appends), so the
 *     order holds. Frozen-OCEAN ice is the SURFACE system (SurfaceSystem.java:224
 *     frozenOceanExtension, pre-decoration) — untouched either way.</li>
 * </ul>
 *
 * <p><b>The order carrier:</b> the three rows ride ONE configured feature (this class)
 * over the table — the OCEAN→RIVER→SWAMP hard order (Loader_Worldgen.java:575-578) is
 * the table iteration, immune to biome-modifier application order (the fluid-spring
 * replay-seam posture, GT6WorldgenDatagen fluid_springs note). The swamp arm's
 * BlockWaterlike conversion (WorldgenSwamp.java:66) only has meaning because the ocean/
 * river rows ran first — same chunk, same pass, strictly ordered.
 *
 * <p><b>The chunk-gate face:</b> upstream gated per CHUNK over {@code aBiomeNames} (the
 * 16x16 biome name grid): ocean = any ocean biome in the chunk (WorldgenOcean.java:54),
 * river = a river biome AND no ocean biome (WorldgenRiver.java:54 verbatim compound),
 * swamp = any swamp biome (WorldgenSwamp.java:55). The modern face samples the chunk's
 * 16 biome quadrants at the sea-level surface (the 4x4 biome cell grid — one
 * {@code level.getBiome} per quadrant, per chunk, the SnowAndFreezeFeature sample
 * shape); the per-column swamp check (:66 {@code aBiomes[tX][tZ].biomeName}) samples the
 * live column position — and only in the waterlike arm, the only column-granular piece
 * upstream.
 *
 * <p><b>The write seam (the perf face):</b> upstream wrote the first replaced block per
 * column through setBlock and the REST through the storage array (WorldgenOcean.java
 * :71-79 {@code func_150818_a} — no callbacks, no light). The port mirror is
 * {@code LevelChunkSection.setBlockState} — the direct section write: no onPlace (LevelChunk
 * .java:282 fires it unconditionally — a setBlock(2) ocean pass would schedule thousands
 * of fluid ticks per chunk and settle shorelines), no neighbor updates, no heightmap
 * churn (water → water-like, same heightmap classes), no light flip (both sides
 * non-occluding). Consequence, declared: the placed water sits STATIC until disturbed —
 * the upstream BlockOcean.PLACEMENT_ALLOWED/UPDATE_TICK settle gates have no vanilla
 * LiquidBlock counterpart (the port fluids are ForgeFlowingFluid bodies); a disturbed
 * ocean cell flows like any fluid block. The scan stays inside the work chunk by
 * construction (16x16 columns over {@code tWork}), inside the FEATURES-step write radius.
 *
 * <p><b>Declared deviations:</b> the upstream mod-biome rows in the three gate sets
 * (RWG/BiomesOPlenty names) are CUT — no modern identity; BlockRiverAdvanced (the soda
 * experiments block, the fourth BlockWaterlike subclass) is unported and out of the
 * conversion set; the waterlike arm covers the port's three water bodies
 * ({@code gt6:seawater_block}/{@code gt6:riverwater_block}/{@code gt6:waterdirty_block}
 * — the waterdirty member writes same-state, the idempotent tail of the upstream
 * instanceof check); the BlockSwamp hunger/confusion effects (Loader_Blocks.java:137)
 * stay unported (the spring-block posture — effect faces live in the bottle domain).
 * KJS face (card declaration): the 3-row table is datapack JSON (the configured-feature
 * config); the Feature/codec registration is the registry face, out of KJS scope
 * (GT6Features javadoc clause).
 */
public class GT6WaterReplaceFeature extends Feature<GT6WaterReplaceConfig.Table> {

    public GT6WaterReplaceFeature() {
        super(GT6WaterReplaceConfig.Table.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<GT6WaterReplaceConfig.Table> aContext) {
        WorldGenLevel tLevel = aContext.level();
        ChunkPos tWork = tLevel instanceof WorldGenRegion ? ((WorldGenRegion) tLevel).getCenter()
                : new ChunkPos(aContext.origin());

        // the chunk-granular gates (upstream aBiomeNames) — one 16-quadrant sample at the
        // sea-level surface (WorldgenOcean.java:54 / WorldgenRiver.java:54 / WorldgenSwamp.java:55)
        int tSampleY = tLevel.getSeaLevel() - 1; // 62 overworld — the top water block (WD.waterLevel default)
        boolean tHasOcean = false, tHasRiver = false, tHasSwamp = false;
        for (int tQX = 0; tQX < 4; tQX++) for (int tQZ = 0; tQZ < 4; tQZ++) {
            Holder<Biome> tBiome = tLevel.getBiome(new BlockPos(
                    tWork.getMinBlockX() + tQX * 4 + 2, tSampleY, tWork.getMinBlockZ() + tQZ * 4 + 2));
            if (tBiome.is(BiomeTags.IS_OCEAN)) tHasOcean = true;
            if (tBiome.is(BiomeTags.IS_RIVER)) tHasRiver = true;
            if (isSwampBiome(tBiome)) tHasSwamp = true;
        }

        boolean rAny = false;
        for (GT6WaterReplaceConfig tRow : aContext.config().rows()) {
            if (rowRuns(tRow.gate(), tHasOcean, tHasRiver, tHasSwamp)) rAny |= replace(tLevel, tWork, tRow);
        }
        return rAny;
    }

    /**
     * The per-row gate matrix — the RIVER compound is WorldgenRiver.java:54 verbatim
     * (a river biome AND no ocean biome in the chunk); OCEAN/SWAMP are the plain
     * any-of-their-set gates (WorldgenOcean.java:54 / WorldgenSwamp.java:55).
     * Package-private: the offline gate-matrix pin (GT6WaterReplaceWorldgenTest).
     */
    static boolean rowRuns(GT6WaterReplaceConfig.Gate aGate, boolean aHasOcean, boolean aHasRiver, boolean aHasSwamp) {
        return switch (aGate) {
            case OCEAN -> aHasOcean;
            case RIVER -> aHasRiver && !aHasOcean;
            case SWAMP -> aHasSwamp;
        };
    }

    /** The swamp-biome identity — no vanilla {@code is_swamp} tag exists, the two vanilla swamp-family biomes are pinned (see the config javadoc). */
    private static boolean isSwampBiome(Holder<Biome> aBiome) {
        return aBiome.is(Biomes.SWAMP) || aBiome.is(Biomes.MANGROVE_SWAMP);
    }

    /**
     * One row's chunk scan — the WorldgenOcean.java:58-82 loop verbatim (River/Swamp same
     * shape): 16x16 columns, scanTop down, occluding break, water replace.
     */
    private boolean replace(WorldGenLevel aLevel, ChunkPos aWork, GT6WaterReplaceConfig aRow) {
        // the loud-refusal face for an unresolvable id (the GT6FluidSpringFeature.fluidState face)
        BlockState tTarget = gregtech6.tileentity.misc.GTFluidSpringBlockEntity.sourceState(aRow.blockId());
        if (tTarget == null) return false;

        // the :66 BlockWaterlike identity set — the port's three water bodies (the river/ocean
        // faces the earlier rows placed, plus waterdirty's idempotent self-row)
        List<BlockState> tWaterlike = List.of(
                sourceState(GTFluids.springBlockId("seawater")),
                sourceState(GTFluids.springBlockId("riverwater")),
                sourceState(GTFluids.springBlockId("waterdirty")));

        ChunkAccess tChunk = aLevel.getChunk(new BlockPos(aWork.getMinBlockX(), 0, aWork.getMinBlockZ()));
        int tTop = Math.min(aRow.scanTop(), aLevel.getMaxBuildHeight() - 1);
        int tBottom = aLevel.getMinBuildHeight() + 1; // :60 the tY > 0 floor, the modern build face
        boolean tSwampRow = aRow.gate() == GT6WaterReplaceConfig.Gate.SWAMP;
        boolean rAny = false;
        for (int tX = 0; tX < 16; tX++) for (int tZ = 0; tZ < 16; tZ++) {
            int tWX = aWork.getMinBlockX() + tX, tWZ = aWork.getMinBlockZ() + tZ;
            for (int tY = tTop; tY >= tBottom; tY--) {
                LevelChunkSection tSection = tChunk.getSection(aLevel.getSectionIndex(tY));
                if (tSection == null) continue; // :62 the null-storage skip
                BlockState tState = tSection.getBlockState(tX, tY & 15, tZ);
                if (tState.canOcclude()) break; // :64 isOpaqueCube → break, the surface-body throttle
                if (tState.is(Blocks.WATER)) { // :65 still + flowing — one modern block
                    write(tSection, tX, tY, tZ, tTarget);
                    rAny = true;
                } else if (tSwampRow && isWaterlike(tState, tWaterlike)
                        && isSwampBiome(aLevel.getBiome(new BlockPos(tWX, tY, tWZ)))) { // :66 the per-column arm
                    write(tSection, tX, tY, tZ, tTarget);
                    rAny = true;
                }
            }
        }
        return rAny;
    }

    /** The row's resolved source state, or null (the spring resolver is THE shared face — worldgen and spray resolve identically). */
    private static BlockState sourceState(String aBlockId) {
        return gregtech6.tileentity.misc.GTFluidSpringBlockEntity.sourceState(aBlockId);
    }

    /** The :66 instanceof-BlockWaterlike face over the port's three water bodies (null-safe: unresolvable ids never match). */
    static boolean isWaterlike(BlockState aState, List<BlockState> aWaterlike) {
        for (BlockState tState : aWaterlike) if (tState != null && aState.is(tState.getBlock())) return true;
        return false;
    }

    /** The direct section write — the :78 func_150818_a fast path (see the class javadoc for the onPlace/tick analysis). */
    private static void write(LevelChunkSection aSection, int aX, int aY, int aZ, BlockState aTarget) {
        aSection.setBlockState(aX, aY & 15, aZ, aTarget);
    }
}
