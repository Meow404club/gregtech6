package gregtech6.worldgen;

import java.util.Random;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import gregtech6.block.stone.StoneVariant;
import gregtech6.registry.GTStoneBlocks;

/**
 * The deep-ocean prismarine-pylon Feature (task worldgen-deepocean-corals) — the
 * {@code WorldgenDeepOcean} port (Loader_Worldgen.java:580 {@code ocean.prismacorals},
 * generator body WorldgenDeepOcean.java:46-106). RED-PIN STUB: the constants and the
 * call shape are pinned by GT6DeepOceanWorldgenTest; {@link #placePylons} lands in the
 * green commit.
 *
 * <p>Despite the upstream config name "prismacorals", the upstream never emits corals:
 * the "Corals maybe?" noise cells 8-11 are a {@code return F} arm (WorldgenDeepOcean.java:
 * 54-56) — the shipped payload is stepped PYLONS of BlocksGT.PrismarineDark (cells 12-13)
 * or BlocksGT.PrismarineLight (cells 14-15), each set-block carrying a 1/8 ore roll on
 * the UPPER face only (Garnierite in the darkprismarine family / MnO2 in the
 * lightprismarine family, WorldgenDeepOcean.java:62/:67/:72/:77 and the :85/:90/:95/:100
 * light twins — ores_normal[14]/[13], the 0-based stone-array order).
 */
public class GT6DeepOceanFeature extends Feature<NoneFeatureConfiguration> {

    /** The column pick {@code i/k = 3 + rnd(9)} (WorldgenDeepOcean.java:48 verbatim). */
    public static final int PICK_MIN = 3, PICK_SPAN = 9;
    /** The height pick {@code j = 30 + rnd(9)} (WorldgenDeepOcean.java:48 verbatim). */
    public static final int J_MIN = 30, J_SPAN = 9;
    /** The noise probe {@code (aMinX+8, 32, aMinZ+8)} (WorldgenDeepOcean.java:50 verbatim). */
    public static final int GATE_PROBE_XZ = 8, GATE_PROBE_Y = 32;
    /** The noise cell count (WorldgenDeepOcean.java:50 — {@code get(..., 16)}). */
    public static final int GATE_CELLS = 16;
    /** The 1/8 ore roll per (l, m, n) body (WorldgenDeepOcean.java:62 et seq). */
    public static final int ORE_CHANCE = 8;

    public GT6DeepOceanFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    /** The chunk-local pylon writer seam (headless-testable; the live sink lands green). */
    public interface Sink {
        void put(int aX, int aY, int aZ, Block aBlock);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> aContext) {
        WorldGenLevel tLevel = aContext.level();
        ChunkPos tWork = tLevel instanceof WorldGenRegion ? ((WorldGenRegion) tLevel).getCenter()
                : new ChunkPos(aContext.origin());
        Random tRandom = GT6VeinGenerator.veinRandom(tLevel.getSeed(),
                GT6VeinGenerator.dimensionSalt(tLevel), tWork.x, tWork.z);
        // the NoiseGenerator(aWorld) face (WorldgenDeepOcean.java:50 — offset 512*dimId):
        // the deep-ocean feature is overworld-only (the IS_DEEP_OCEAN modifier), salt 0 =
        // offset 0 = the upstream overworld stream
        GT6WorleyNoise tNoise = new GT6WorleyNoise(tLevel.getSeed(),
                (int) GT6VeinGenerator.dimensionSalt(tLevel));
        int i = PICK_MIN + tRandom.nextInt(PICK_SPAN), j = J_MIN + tRandom.nextInt(J_SPAN),
                k = PICK_MIN + tRandom.nextInt(PICK_SPAN);
        int tBaseX = tWork.getMinBlockX(), tBaseZ = tWork.getMinBlockZ();
        // :49 — the WD.anywater gate (the port face: the water fluid tag covers the vanilla
        // water column; the water-replace card's GT waters ride the same tag, so the
        // upstream ordering "corals after ocean.seawater" survives the merge)
        if (!tLevel.getFluidState(new BlockPos(tBaseX + i, j, tBaseZ + k)).is(FluidTags.WATER)) return false;
        int tCell = tNoise.get(tBaseX + GATE_PROBE_XZ, GATE_PROBE_Y, tBaseZ + GATE_PROBE_XZ, GATE_CELLS); // :50
        boolean tDark;
        if (tCell == 12 || tCell == 13) tDark = true;       // :57-79 — the dark pylons
        else if (tCell == 14 || tCell == 15) tDark = false; // :80-102 — the light pylons
        else return false;                                   // :51-56 — default/8-11 stay normal
        Block tStone = GTStoneBlocks.block(tDark ? "prismarine_dark" : "prismarine_light",
                StoneVariant.STONE).get();
        // the payload ores: ores_normal[14] Garnierite hosted darkprismarine / ores_normal[13]
        // MnO2 hosted lightprismarine — the family NORMAL form (the placeBlock semantics)
        Block tOre = orePayload(tDark);
        if (tOre == null) return false;
        placePylons(tBaseX + i, j, tBaseZ + k, tRandom, tStone, tOre,
                (aX, aY, aZ, aBlock) -> { /* STUB — the live sink lands green */ });
        return true; // :79/:102 — the ran-face semantics
    }

    /** STUB — returns null green-side; the family walk + material pick is the payload face. */
    static Block orePayload(boolean aDark) {
        return null;
    }

    /** STUB — the stepped-pylon writer (WorldgenDeepOcean.java:59-101) lands green. */
    public static void placePylons(int aX, int aY, int aZ, Random aRandom, Block aStone, Block aOre, Sink aOut) {
    }
}
