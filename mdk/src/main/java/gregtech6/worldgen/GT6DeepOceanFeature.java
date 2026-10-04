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

import gregapi.data.MT;
import gregapi.oredict.OreDictMaterial;
import gregtech6.block.stone.StoneVariant;
import gregtech6.registry.GT6OreBlocks;
import gregtech6.registry.GTStoneBlocks;

/**
 * The deep-ocean prismarine-pylon Feature (task worldgen-deepocean-corals) — the
 * {@code WorldgenDeepOcean} port (Loader_Worldgen.java:580 {@code ocean.prismacorals},
 * generator body WorldgenDeepOcean.java:46-106), a NoneFeatureConfiguration Feature over
 * the {@link GT6WorleyNoise} port (the nether-form shape). One pylon per gated chunk:
 * the coordinate-seeded chunk stream picks the column (i, 30..38, k), the water gate
 * probes it, the 16-cell noise draw picks dark/light/nothing, the pylon steps 7x7 at the
 * base to 1x1 at the tips with a 1/8 ore roll per body.
 *
 * <p>Despite the upstream config name "prismacorals", the upstream never emits corals:
 * the "Corals maybe?" noise cells 8-11 are a {@code return F} arm (WorldgenDeepOcean.java:
 * 54-56) — the shipped payload is stepped PYLONS of BlocksGT.PrismarineDark (cells 12-13)
 * or BlocksGT.PrismarineLight (cells 14-15), each set-block carrying a 1/8 ore roll on
 * the UPPER face only (Garnierite in the darkprismarine family / MnO2 in the
 * lightprismarine family, WorldgenDeepOcean.java:62/:67/:72/:77 and the :85/:90/:95/:100
 * light twins — ores_normal[14]/[13], the 0-based stone-array order). The blocks ride the
 * port's GTStoneBlocks prismarine_light/dark STONE rows and the GT6OreBlocks prismarine
 * families (both registered by the ore-1-mech/stone cards — the coral-block question
 * resolves to NO new blocks; the CUT face is upstream's own: the coral arm never shipped).
 *
 * <p>Determinism: pure coordinate function over (world seed, chunk, dimension salt) —
 * the chunk stream is {@link GT6VeinGenerator#veinRandom} and the noise is the
 * {@link GT6WorleyNoise} port (decisions.2026-09-18-p31-strata-lens-determinism-acceptance).
 *
 * <p>KJS face (card declaration): zero JSON config (upstream constants live in the
 * class); the Feature registration is the registry face (the GT6Features javadoc
 * declaration), the configured/placed/biome-modifier JSONs are the datapack face.
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
        int tMinY = tLevel.getMinBuildHeight(), tMaxY = tLevel.getMaxBuildHeight() - 1;
        // the live WD.set face — chunk-local writes, flag 2 (the nether-form posture); the
        // band guard keeps a /place in a clipped context from throwing
        placePylons(tBaseX + i, j, tBaseZ + k, tRandom, tStone, tOre, (aX, aY, aZ, aBlock) -> {
            if (aY < tMinY || aY > tMaxY) return;
            tLevel.setBlock(new BlockPos(aX, aY, aZ), aBlock.defaultBlockState(), 2);
        });
        return true; // :79/:102 — the ran-face semantics
    }

    /** The payload ore of a pylon: ores_normal[14] Garnierite hosted darkprismarine / ores_normal[13]
     * MnO2 hosted lightprismarine (WorldgenDeepOcean.java:62/:85 — the 0-based 17-stone array order),
     * the family NORMAL form (the placeBlock semantics). Null when the family walk or the
     * registration walk comes up empty (the loud-refusal face). */
    static Block orePayload(boolean aDark) {
        GT6OreBlocks.OreFamily tFamily = family(aDark ? "darkprismarine" : "lightprismarine");
        OreDictMaterial tMaterial = aDark ? MT.OREMATS.Garnierite : MT.MnO2;
        var tHandle = GT6OreBlocks.get(tFamily, GT6OreBlocks.FormKind.NORMAL, tMaterial);
        return tHandle == null ? null : tHandle.get();
    }

    /** The ore family by its upstream internal snake (GT6OreBlocks.FAMILIES walk — no new API surface). */
    private static GT6OreBlocks.OreFamily family(String aSnake) {
        for (GT6OreBlocks.OreFamily tFamily : GT6OreBlocks.FAMILIES) {
            if (tFamily.snake().equals(aSnake)) return tFamily;
        }
        throw new IllegalStateException("no ore family " + aSnake);
    }

    /**
     * The stepped pylon writer (WorldgenDeepOcean.java:59-78 dark / :82-101 light, verbatim
     * shape): four radius bands — the 1x1 column |dy| 8..10, the 3x3 |dy| 5..7, the 5x5
     * |dy| 2..4, the 7x7 |dy| 0..1 — every body sets BOTH faces then rolls ONE
     * {@code nextInt(8)}, the ore replacing the UPPER (+l) set only. 203 bodies per pylon,
     * one draw each (the GT6DeepOceanWorldgenTest replay pin).
     */
    public static void placePylons(int aX, int aY, int aZ, Random aRandom, Block aStone, Block aOre, Sink aOut) {
        band(aX, aY, aZ, 8, 3, 0, aRandom, aStone, aOre, aOut); // :59-63 — the 1x1 top/bottom
        band(aX, aY, aZ, 5, 3, 1, aRandom, aStone, aOre, aOut); // :64-68 — the 3x3 band
        band(aX, aY, aZ, 2, 3, 2, aRandom, aStone, aOre, aOut); // :69-73 — the 5x5 band
        band(aX, aY, aZ, 0, 2, 3, aRandom, aStone, aOre, aOut); // :74-78 — the 7x7 base
    }

    /** One band: {@code aRows} l-values upward from {@code aL}, square radius {@code aRadius}. */
    private static void band(int aX, int aY, int aZ, int aL, int aRows, int aRadius,
            Random aRandom, Block aStone, Block aOre, Sink aOut) {
        for (int tL = aL; tL < aL + aRows; tL++) {
            for (int m = -aRadius; m <= aRadius; m++) {
                for (int n = -aRadius; n <= aRadius; n++) {
                    aOut.put(aX + m, aY + tL, aZ + n, aStone); // WD.set +l (e.g. :60)
                    aOut.put(aX + m, aY - tL, aZ + n, aStone); // WD.set -l (:61)
                    if (aRandom.nextInt(ORE_CHANCE) == 0) aOut.put(aX + m, aY + tL, aZ + n, aOre); // :62
                }
            }
        }
    }
}
