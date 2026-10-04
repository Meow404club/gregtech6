package gregtech6.worldgen;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import gregtech6.block.surface.GT6SurfaceRockBlock;
import gregtech6.registry.GT6SurfaceBlocks;

/**
 * The nether rack Feature (task worldgen-racks) — the {@code WorldgenRacks} port
 * (WorldgenRacks.java:46-98, the Loader_Worldgen.java:619 {@code "nether.rocks"} row,
 * the nether-only {@code GEN_NETHER} sister of the overworld {@code WorldgenRocks}
 * band): EVERY gated chunk, SIXTEEN random-column attempts, each scanning DOWN from a
 * random Y window to the first ground contact and placing ONE surface rock carrying the
 * nether loot arm (the 24-case NBT lottery → the per-pair {@link Rack} blocks).
 *
 * <p>Upstream verbatim structure:
 * <ul>
 * <li>the chunk gate {@code aRandom.nextBoolean()} (:54) rides the placed chain as
 *     {@code RarityFilter.onAverageOnceEvery(2)} — the same nextBoolean draw, the
 *     tier-a datapack face;</li>
 * <li>{@code checkForMajorWorldgen} (:54) is the overworld streets/biomes showcase
 *     gate (WorldgenObject.java:70-76) — ALWAYS false in {@code GEN_NETHER}, the dead
 *     clause is CUT;</li>
 * <li>16 attempts per chunk (:57), {@code aMinX + nextInt(16)} column picks (:58);</li>
 * <li>the start draw {@code nextInt(80) + 47} (:59, the vanilla-height arm) — the
 *     1.7.10 nether interior [47, 126]; the {@code WD.bedrock(255) ? 200 : 80}
 *     flat-bedrock arm has no port config face, CUT (the GT6NetherQuartzFeature clamp
 *     posture: the buildable interior is the domain);</li>
 * <li>the 40-deep downward scan (:59) — liquid abort (:61), air keeps scanning (:62),
 *     the first ground-class contact ends the column (:93) with the rack placed above
 *     it when the slot is replaceable (:64 {@code WD.easyRep}).</li>
 * </ul>
 *
 * <p><b>The host gate</b> (upstream :63 material ∈ {grass, ground, sand, rock}):
 * 1.20.1 dropped the state Material face, so the gate is the {@code isFaceSturdy(UP)}
 * surface — the SAME predicate {@link GT6SurfaceRockBlock#canSurvive} demands of the
 * attach face (GTCEu :126-131), i.e. we never place where the rock could not sit.
 * Declared deviation, the GTCEu sturdy-attach class: full non-ground blocks (wood,
 * leaves, glowstone, warts) now qualify as hosts where the material classes skipped
 * them — in the nether this moves a marginal band of racks from "buried" to "placed".
 *
 * <p><b>The loot arms</b> (:65-90, the verbatim {@link #pick} table): the substrate
 * conditionals (nether bricks → debris :3-5, soul sand/soil → gloomstone/quartz :6-11,
 * gravel → flint :16-23) are pick-time — a pure JSON weighted list cannot express
 * them, hence the Feature class. The debris 3:1 rockGt/oreRaw inner draw
 * ({@code nextInt(4)==0 ? oreRaw : rockGt}, :68-71) rides the loot table like the
 * meteorite rock (WorldgenRocks.java:63 precedent). The ported nether-liker hosts
 * (IL.NeLi_SoulSoil/NeLi_Gravel, :72/:82) are the dimension-mod band, OUT (card spec).
 *
 * <p>KJS face (the card declaration): the placement JSONs (configured/placed/biome
 * modifier) are the tier-a datapack surface; the 24-case table is an upstream constant,
 * not a config surface.
 */
public class GT6RacksFeature extends Feature<NoneFeatureConfiguration> {

    /** The draw window floor (:59, the +47). */
    public static final int RACKS_MIN_Y = 47;
    /** The draw window span (:59, the vanilla-height nextInt(80)). */
    public static final int RACKS_SPAN = 80;
    /** The downward scan depth (:59, the {@code tY - 40} floor). */
    public static final int RACKS_DEPTH = 40;
    /** The per-chunk attempt count (:57). */
    public static final int RACKS_ATTEMPTS = 16;

    /**
     * The rack identities (the :66-90 loot arms): seven nether rocks + the shared
     * first-batch flint rock. The debris identity's 3:1 split lives in its loot table.
     */
    public enum Rack {
        NETHER_QUARTZ, GLOWSTONE, ANCIENT_DEBRIS, OBSIDIAN, BASALT, BLACKSTONE, GLOOMSTONE, FLINT
    }

    /**
     * The verbatim :65-90 draw table. One {@code nextInt(24)} draw + the three contact
     * probes of the landed host (nether bricks / soul sand-soil / gravel — the port
     * face of the upstream {@code tContact == Blocks.nether_brick} arms; the
     * NeLi/NePl nether-liker probes are the dimension-mod band, OUT).
     */
    public static Rack pick(int aDraw, boolean aBrickContact, boolean aSoulContact, boolean aGravelContact) {
        switch (aDraw) {
        case 0:  return Rack.NETHER_QUARTZ; // :66
        case 1:  return Rack.GLOWSTONE;     // :67
        case 2:  return Rack.ANCIENT_DEBRIS; // :68 — the 3:1 split rides the loot table
        case 3:  return aBrickContact ? Rack.ANCIENT_DEBRIS : Rack.NETHER_QUARTZ; // :69
        case 4:  return aBrickContact ? Rack.ANCIENT_DEBRIS : Rack.GLOWSTONE;     // :70
        case 5:  return aBrickContact ? Rack.ANCIENT_DEBRIS : Rack.OBSIDIAN;      // :71
        case 6:
        case 7:  return aSoulContact ? Rack.GLOOMSTONE : Rack.FLINT; // :72-73
        case 8:
        case 9:
        case 10:
        case 11: return aSoulContact ? Rack.NETHER_QUARTZ : Rack.FLINT; // :74-77
        case 12: return Rack.OBSIDIAN;      // :78
        case 13:
        case 14:
        case 15: return Rack.BASALT;        // :79-81
        default: return aGravelContact ? Rack.FLINT : Rack.BLACKSTONE; // :82-89, the 16..23 arm
        }
    }

    /** The rack block of a draw (the RegistryObject handles resolve at worldgen use time — the FALLEN_LOG_FEATURES lesson). */
    private static Block rockOf(Rack aRack) {
        return switch (aRack) {
        case NETHER_QUARTZ -> GT6SurfaceBlocks.SURFACE_ROCK_NETHER_QUARTZ.get();
        case GLOWSTONE -> GT6SurfaceBlocks.SURFACE_ROCK_GLOWSTONE.get();
        case ANCIENT_DEBRIS -> GT6SurfaceBlocks.SURFACE_ROCK_ANCIENT_DEBRIS.get();
        case OBSIDIAN -> GT6SurfaceBlocks.SURFACE_ROCK_OBSIDIAN.get();
        case BASALT -> GT6SurfaceBlocks.SURFACE_ROCK_BASALT.get();
        case BLACKSTONE -> GT6SurfaceBlocks.SURFACE_ROCK_BLACKSTONE.get();
        case GLOOMSTONE -> GT6SurfaceBlocks.SURFACE_ROCK_GLOOMSTONE.get();
        case FLINT -> GT6SurfaceBlocks.SURFACE_ROCK_FLINT.get();
        };
    }

    public GT6RacksFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> aContext) {
        WorldGenLevel tLevel = aContext.level();
        ChunkPos tWork = tLevel instanceof WorldGenRegion ? ((WorldGenRegion) tLevel).getCenter()
                : new ChunkPos(aContext.origin());
        RandomSource tRandom = aContext.random();
        int tMinBuildY = tLevel.getMinBuildHeight(), tMaxBuildY = tLevel.getMaxBuildHeight() - 1;
        for (int i = 0; i < RACKS_ATTEMPTS; i++) { // :57 — 16 columns per gated chunk
            int tX = tWork.getMinBlockX() + tRandom.nextInt(16), tZ = tWork.getMinBlockZ() + tRandom.nextInt(16); // :58
            int tStart = RACKS_MIN_Y + tRandom.nextInt(RACKS_SPAN); // :59, the vanilla-height arm
            for (int tY = tStart, tH = Math.max(tMinBuildY, tStart - RACKS_DEPTH); tY > tH; tY--) {
                if (tY > tMaxBuildY) continue; // the modded-dim guard (GT6NetherQuartzFeature posture)
                BlockPos tPos = new BlockPos(tX, tY, tZ);
                BlockState tContact = tLevel.getBlockState(tPos);
                if (!tContact.getFluidState().isEmpty()) break; // :61 — the liquid abort
                if (tContact.isAir()) continue; // :62
                // :63 — the ground gate, the modern sturdy face (see the class javadoc)
                if (!tContact.isFaceSturdy(tLevel, tPos, Direction.UP)) continue;
                BlockPos tAbove = tPos.above();
                if (tLevel.getBlockState(tAbove).canBeReplaced()) { // :64 — WD.easyRep
                    boolean tBrick = tContact.is(Blocks.NETHER_BRICKS); // :69-71
                    boolean tSoul = tContact.is(Blocks.SOUL_SAND) || tContact.is(Blocks.SOUL_SOIL); // :72-77
                    boolean tGravel = tContact.is(Blocks.GRAVEL); // :82-89
                    BlockState tRock = rockOf(pick(tRandom.nextInt(24), tBrick, tSoul, tGravel))
                            .defaultBlockState().setValue(GT6SurfaceRockBlock.FACING, Direction.DOWN); // worldgen places DOWN
                    tLevel.setBlock(tAbove, tRock, 2);
                }
                break; // :93 — the first ground contact ends the column either way
            }
        }
        return true; // :96 — the ran-face (the /place command relies on it)
    }
}
