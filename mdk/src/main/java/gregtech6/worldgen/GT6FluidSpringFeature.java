package gregtech6.worldgen;

import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

import gregtech6.block.ore.GTBedrockOreBlock;
import gregtech6.registry.GTBlockEntities;

/**
 * The bedrock-spring Feature (task p31-fluid-spring spec ②) — the per-chunk adapter around
 * {@link GT6FluidSpringGenerator}, the {@link GT6BedrockOreFeature} isomorphic shape: the
 * placed feature runs Count(1)+InSquare+BiomeFilter (one attempt per chunk), the work
 * chunk is the REGION CENTER (WorldGenRegion.getCenter(), the /place path falls back to
 * the aimed chunk), and all writes stay inside that chunk by construction (the dome
 * footprint insets from the chunk walls).
 *
 * <p><b>The per-chunk mutual-exclusion seam</b> (the card's key constraint): the static
 * flags GENERATED_NO_BEDROCK_ORE/CAN_GENERATE_BEDROCK_ORE did not port (bedrock card
 * deviation ④), so the ore draw is REPLAYED here on the ore feature's own
 * coordinate-seeded stream ({@link GT6VeinGenerator#veinRandom} over
 * {@code OVERWORLD_DIMENSION_SALT} — the exact prefix GT6BedrockOreFeature.place consumes
 * via {@code drawRows}) and an ore-claimed chunk refuses the spring (WorldgenFluidSpring
 * .java:62 GENERATED_NO_BEDROCK_ORE face). The "矿先泉后" registration order
 * (Loader_Worldgen.java:781) therefore holds WITHOUT relying on biome-modifier
 * application order — whichever feature runs first claims the chunk and the replay
 * refuses the other; the gt6:fluid_springs biome modifier still ships textually AFTER
 * bedrock_ores (the source-order declaration). The spring's own rows ride the
 * {@link GT6Worldgen#SPRING_DIMENSION_SALT} stream (see the generator javadoc).
 *
 * <p><b>The fluid write seam</b> (the GTCEu FluidSproutFeature judgment, verified against
 * tmp/refs/gtceu-modern FluidSproutFeature.java:114-131): the lake cells write the fluid
 * block's SOURCE state and the position is marked for postprocessing
 * ({@code getChunk(pos).markPosForPostprocessing}, ChunkAccess.java:279 — the fluid
 * re-ticks and settles when the chunk completes). ponytail: the GTCEu BulkSectionAccess
 * direct-section writes are a batch-write perf shape irrelevant at the dome's <=1536
 * cells — the in-repo plain setBlock(…, 2) path (the bedrock card, RCON-verified)
 * carries the writes and only the postprocessing half of the seam is load-bearing.
 *
 * <p>Declared deferrals: the surface grass-indicator arm (WorldgenFluidSpring.java:82-103)
 * rides the card spec ③ defer — the dome/lake/shell half (p31) and the infinite-spring
 * nozzle arm (:77-79, task p38-issue5-fluid-spring-nozzle: the GTBlockEntities.FLUID_SPRING
 * block + its GTFluidSpringBlockEntity at the strict-bedrock floor, 1/16 per position at
 * dome layers i &gt; 2) are complete. The shell block is deepslate (the sibling bedrock
 * muffin translation; the
 * upstream OW row picks the Betweenlands deepslate-or-stone fallback, WorldgenFluidSpring
 * .java:69-70 — a compat block this port does not carry). KJS face (card declaration):
 * the 16-row table is datapack JSON (the configured-feature config); the Feature/codec
 * registration is the registry face, out of KJS scope (GT6Features javadoc clause).
 */
public class GT6FluidSpringFeature extends Feature<GTFluidSpringConfig.Table> {

    public GT6FluidSpringFeature() {
        super(GTFluidSpringConfig.Table.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<GTFluidSpringConfig.Table> aContext) {
        WorldGenLevel tLevel = aContext.level();
        ChunkPos tWork = tLevel instanceof WorldGenRegion ? ((WorldGenRegion) tLevel).getCenter()
                : new ChunkPos(aContext.origin());

        // :62 — the GENERATED_NO_BEDROCK_ORE replay (the mutual-exclusion seam, see class javadoc)
        Random tOreRandom = GT6VeinGenerator.veinRandom(tLevel.getSeed(),
                GT6VeinGenerator.OVERWORLD_DIMENSION_SALT, tWork.x, tWork.z);
        if (GT6FluidSpringGenerator.oreClaims(oreTable(tLevel), tOreRandom)) return false;

        // :62/:64 — the spring's own 1/P rolls, first hit claims the chunk
        Random tSpringRandom = GT6VeinGenerator.veinRandom(tLevel.getSeed(),
                GT6Worldgen.SPRING_DIMENSION_SALT, tWork.x, tWork.z);
        GTFluidSpringConfig tRow = GT6FluidSpringGenerator.drawSpring(aContext.config(), tSpringRandom);
        if (tRow == null) return false;

        BlockState tFluid = fluidState(tRow.blockId());
        if (tFluid == null) return false; // the loud-refusal face for an unresolvable id (no silent water fallback)

        int tBedrockY = Math.max(GT6BedrockOreGenerator.BEDROCK_Y, tLevel.getMinBuildHeight());
        return GT6FluidSpringGenerator.generateDome(tRow, tWork.getMinBlockX(), tWork.getMinBlockZ(),
                tBedrockY, tSpringRandom, levelSink(tLevel, tFluid, tRow));
    }

    /**
     * The live gt6:bedrock_ores table — the replay replays the ACTUAL datapack config
     * (registry face: LevelReader.java:212 registryAccess, WorldGenRegion.java:355; the
     * configured feature IS the {@link GTBedrockOreConfig.Table}; Registry.getOrThrow
     * returns the value directly, the 1.20.1 Registry.java face), so a datapack-edited
     * ore table keeps the exclusion exact.
     */
    private static GTBedrockOreConfig.Table oreTable(WorldGenLevel aLevel) {
        return (GTBedrockOreConfig.Table) aLevel.registryAccess()
                .registryOrThrow(Registries.CONFIGURED_FEATURE)
                .getOrThrow(GT6Worldgen.BEDROCK_ORES_CONFIGURED).config();
    }

    /** The row's resolved fluid source state, or null when the id carries no registered block (the GTFluidSpringBlockEntity.sourceState miss face). */
    private static BlockState fluidState(String aBlockId) {
        return gregtech6.tileentity.misc.GTFluidSpringBlockEntity.sourceState(aBlockId);
    }

    private GT6FluidSpringGenerator.DomeSink levelSink(WorldGenLevel aLevel, BlockState aFluid, GTFluidSpringConfig aRow) {
        return new GT6FluidSpringGenerator.DomeSink() {
            @Override
            public boolean isBedrockFace(int aX, int aZ) {
                Block tBlock = aLevel.getBlockState(new BlockPos(aX, GT6BedrockOreGenerator.BEDROCK_Y, aZ)).getBlock();
                return tBlock == Blocks.BEDROCK || tBlock instanceof GTBedrockOreBlock; // :67 + the idempotent ore face
            }

            @Override
            public boolean isBedrock(int aX, int aZ) {
                Block tBlock = aLevel.getBlockState(new BlockPos(aX, GT6BedrockOreGenerator.BEDROCK_Y, aZ)).getBlock();
                return tBlock == Blocks.BEDROCK; // :77 WD.bedrock — the STRICT floor face (no bedrock-ore nozzles)
            }

            @Override
            public boolean isOpaque(int aX, int aY, int aZ) {
                return aLevel.getBlockState(new BlockPos(aX, aY, aZ)).canOcclude(); // :73 WD.opq — the GT6LargeVeinFeature:128 probe form
            }

            @Override
            public void shell(int aX, int aY, int aZ) {
                aLevel.setBlock(new BlockPos(aX, aY, aZ), Blocks.DEEPSLATE.defaultBlockState(), 2); // :73 the cave-seal skin
            }

            @Override
            public void fluid(int aX, int aY, int aZ, GTFluidSpringConfig aDomeRow) {
                BlockPos tPos = new BlockPos(aX, aY, aZ);
                aLevel.setBlock(tPos, aFluid, 2); // :75 the lake body (the source state)
                aLevel.getChunk(tPos).markPosForPostprocessing(tPos); // the FluidSproutFeature.java:127 settle seam
            }

            @Override
            public void nozzle(int aX, int aY, int aZ) {
                // :78 — the MultiTileEntityFluidSpring placement: the block + its "gt.spring" data
                BlockPos tPos = new BlockPos(aX, aY, aZ);
                aLevel.setBlock(tPos, GTBlockEntities.FLUID_SPRING.get().defaultBlockState(), 2);
                if (aLevel.getBlockEntity(tPos) instanceof gregtech6.tileentity.misc.GTFluidSpringBlockEntity tSpring) {
                    tSpring.setSpring(aRow.blockId(), aRow.springFluid().intValue());
                }
            }
        };
    }
}
