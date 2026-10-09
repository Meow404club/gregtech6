package gregtech6.worldgen;

import java.util.Map;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.data.worldgen.features.TreeFeatures;

import gregtech6.block.stone.StoneVariant;
import gregtech6.registry.GT6SurfaceBlocks;
import gregtech6.worldgen.GT6CenterFeature.Env;
import gregtech6.worldgen.GT6CenterFeature.Sink;

/**
 * The biome display ring (task worldgen-center-biomes) — the {@code worldgen/center/
 * WorldgenCenterBiomes.java:63-282} port: the world-origin ±6-chunk showcase (12x12 chunk
 * window {@code aMinX∈[-96,80]}, :69) where every chunk is claimed by exactly ONE display
 * zone: the fake river cross, four 2x2 corner specials (ice/forest/mesa/swamp) and four
 * quadrant fills (cold taiga/plains/desert/jungle), each zone = one whole-chunk biome
 * overwrite + a hand-built floor column.
 *
 * <p><b>The biome write-path ruling</b> (the card's core research deliverable): the naive
 * route is dead — 1.20.1 {@code LevelChunkSection.biomes} is a {@code PalettedContainerRO}
 * with NO public setter (LevelChunkSection.java:25, both editions; the port audit's
 * objection). Reflection/AT over the private field was rejected: the field is the vanilla
 * palette machinery itself, and every candidate hole (constructor replay, section swap,
 * accessor transformer) buys the same thing the vanilla public face already sells, while
 * adding a mapping-stability liability the port has no other precedent for. The SELECTED
 * path is {@code ChunkAccess#fillBiomesFromNoise(BiomeResolver, Climate.Sampler)} —
 * public on BOTH legs (1.20.1 ChunkAccess.java:425, 1.21.1 :430), the exact face vanilla's
 * own {@code /fillbiome} uses (FillBiomeCommand.java:127), writing every quart cell through
 * {@code LevelChunkSection.fillBiomesFromNoise} (a recreate+refill+reassign of the palette
 * — LevelChunkSection.java:180-192, vanilla-owned and vanilla-tested). Zero reflection,
 * zero AT, zero registry-risk. The resolver is a constant ({@code (x,y,z,s) -> holder})
 * reproducing the upstream {@code Arrays.fill(aChunk.getBiomeArray(), biomeID)} semantics
 * (:71/:75/:79/:83/:103/:115/:140/:162/:195/:213/:227/:244); the sampler is never invoked.
 * Leg delta: NONE — the 1.21.1 field stayed {@code PalettedContainerRO} (LevelChunkSection
 * :25) and the public face survived verbatim, so this is the rare write path with NO
 * stonecutter fork (the candidate ③ "21.1 section API 更宽" turned out moot — nothing
 * narrower existed to widen). Chunk-safety: the Feature runs per-chunk and fills ONLY the
 * WorldGenRegion's center chunk (the region's own chunk — never an out-of-window write);
 * the fill happens at the TOP_LAYER_MODIFICATION decoration pass where the palette is long
 * settled (BIOMES status ran earlier) and nothing re-derives it afterwards — the same
 * ordering vanilla's NoiseBasedChunkGenerator (:87) and /fillbiome rely on.
 *
 * <p><b>The declared block mappings</b> (the 对账 face): BlocksGT.River (the BlockWaterlike
 * visual) → {@code minecraft:water} ×3; BlocksGT.Sands metas 0/1/2 (Black/Basaltic/
 * Granitic Black Sand — BlockSands.java:39-41) → the ported {@code gt6:black_sand} for all
 * three (the meta-0 row is verbatim, 1/2 are the declared stand-in until a GT sands block
 * family lands); BlocksGT.Diggables metas 0/1/2/3/4/5/6 (BlockDiggable.java:47-57) →
 * vanilla mud (the GT6OreBlocks.java:217 ruling) + the diggables-pits parallel-branch
 * quartet + gt6:turf + gt6:nether_red_clay. The clay quartet (metas 1/4/5/6) resolves BY
 * REGISTRY NAME at place time ({@link #diggable}) — the task card said "delivered in your
 * baseline", the audit found it on the unmerged {@code work/worldgen-diggables-pits}
 * branch; a compile-time handle would have welded this card to that merge order, the
 * name-based null-skip lights the ring up whenever it lands, zero further edits (the
 * coordinator-ruled form). MTE 32757+flint / 32757 / 32756 → gt6 surface_rock_flint /
 * surface_rock_stone / surface_stick (the GT6SurfaceBlocks trio). The upstream vanilla
 * WorldGenTrees rows ride the vanilla configured features (oak/birch/spruce/jungle) — the
 * height/provider draws are the feature's own (declared deviation); the draws the upstream
 * took BEFORE each generate call stay consumed where they order other pins (stream
 * fidelity). The 1.7.10 stone metas map onto StoneVariant ordinals (0=STONE, 2=MCOBL —
 * StoneVariant.java:13); vanilla {@code Blocks.stone,1}/{@code gravel,1}/{@code snow_layer}
 * metas are plain modern blocks (1.7.10 damage values with no visual there), the snow
 * layer rides LAYERS meta+1. The EtFu_Dirt conditional (:245) is CUT — no such dependency
 * face exists, the dirt-meta-1 branch runs unconditionally. The upstream
 * BlockRiver.PLACEMENT_ALLOWED toggle (:84/:96) is CUT with its purpose: the gate guarded
 * the special block from foreign placements, moot for vanilla water.
 */
public final class GT6CenterBiomes {

    /** The display zones, the upstream branch order (:70-277). The {@code *_FILL} rows are the biome-only companion-switch couplings. */
    public enum Zone {
        NONE, RIVER, PLAZA_FILL, NEXUS_FILL, TESTING_FILL,
        ICE, TAIGA, FOREST, PLAINS, MESA, DESERT, SWAMP, JUNGLE
    }

    /** The zone → vanilla biome key paths (the 1.7.10→modern renames: icePlains→snowy_plains, coldTaiga→snowy_taiga, mesa→badlands, swampland→swamp). */
    private static final Map<String, ResourceKey<Biome>> BIOME_KEYS = Map.ofEntries(
            Map.entry("river", Biomes.RIVER),
            Map.entry("plains", Biomes.PLAINS),
            Map.entry("snowy_plains", Biomes.SNOWY_PLAINS),
            Map.entry("snowy_taiga", Biomes.SNOWY_TAIGA),
            Map.entry("forest", Biomes.FOREST),
            Map.entry("badlands", Biomes.BADLANDS),
            Map.entry("desert", Biomes.DESERT),
            Map.entry("swamp", Biomes.SWAMP),
            Map.entry("jungle", Biomes.JUNGLE));

    /** The tree kind → the vanilla configured feature (the upstream WorldGenTrees wood metas 0/1/2/3 — jungle keeps the vines feature, :271-274 the T flag). */
    private static final Map<String, ResourceKey<ConfiguredFeature<?, ?>>> TREE_KEYS = Map.of(
            "oak", TreeFeatures.OAK,
            "birch", TreeFeatures.BIRCH,
            "spruce", TreeFeatures.SPRUCE,
            "jungle", TreeFeatures.JUNGLE_TREE);

    /** The corner offsets (:128-136/:151-154/:206-210/:239-242) — the two per-axis stands. */
    private static final int[] CORNERS = {4, 12};

    private GT6CenterBiomes() {}

    /** {@code aMinX∈[-96,80] && aMinZ∈[-96,80]} (:69) — the 12x12 chunk window, ±6 from the origin. */
    public static boolean inRegion(int aCx, int aCz) {
        return aCx >= -6 && aCx <= 5 && aCz >= -6 && aCz <= 5;
    }

    /** The zone classifier — the upstream if-cascade in branch order (:70 plaza fill, :74 nexus, :78 testing, :82 river cross, :100/:138/:192/:225 the four quadrant fills). */
    public static Zone zone(int aCx, int aCz) {
        if (!inRegion(aCx, aCz)) return Zone.NONE;
        if (GT6CenterFeature.STREETS && GT6CenterFeature.isPlazaChunk(aCx, aCz)) return Zone.PLAZA_FILL; // :70-73
        if (GT6CenterFeature.NEXUS && GT6CenterFeature.isNexusChunk(aCx, aCz)) return Zone.NEXUS_FILL; // :74-77
        if (GT6CenterFeature.TESTING && (aCx == 2 || aCx == 3) && (aCz == -2 || aCz == -3)) return Zone.TESTING_FILL; // :78-81
        if (aCx == -1 || aCx == 0 || aCz == -1 || aCz == 0) return Zone.RIVER; // :82
        if (aCx < 0) {
            if (aCz < 0) {
                if ((aCx == -5 || aCx == -4) && (aCz == -5 || aCz == -4)) return Zone.ICE; // :102
                return Zone.TAIGA; // :115
            }
            if ((aCx == -5 || aCx == -4) && (aCz == 3 || aCz == 4)) return Zone.FOREST; // :139
            return Zone.PLAINS; // :162
        }
        if (aCz < 0) {
            if ((aCx == 3 || aCx == 4) && (aCz == -5 || aCz == -4)) return Zone.MESA; // :194
            return Zone.DESERT; // :213
        }
        if ((aCx == 3 || aCx == 4) && (aCz == 3 || aCz == 4)) return Zone.SWAMP; // :226
        return Zone.JUNGLE; // :244
    }

    /** The zone's vanilla biome key path — icePlains→snowy_plains, coldTaiga→snowy_taiga, mesa→badlands, swampland→swamp (the 1.7.10→modern renames). */
    public static String biome(Zone aZone) {
        return switch (aZone) {
            case RIVER, PLAZA_FILL -> "river"; // :71/:83
            case NEXUS_FILL, TESTING_FILL, PLAINS -> "plains"; // :75/:79/:162
            case ICE -> "snowy_plains"; // :103
            case TAIGA -> "snowy_taiga"; // :115
            case FOREST -> "forest"; // :140
            case MESA -> "badlands"; // :195
            case DESERT -> "desert"; // :213
            case SWAMP -> "swamp"; // :227
            case JUNGLE -> "jungle"; // :244
            default -> "";
        };
    }

    /** The upstream Diggables meta → the gt6 registry path (0 = the vanilla MUD ruling — not a gt6 path, the "minecraft:" marker). */
    public static String diggableName(int aMeta) {
        return switch (aMeta) {
            case 0 -> "minecraft:mud"; // the GT6OreBlocks.java:217 deviation ruling
            case 1 -> "brown_clay"; // the diggables-pits parallel branch registrations
            case 2 -> "turf"; // GT6SurfaceBlocks.TURF
            case 3 -> "nether_red_clay"; // GT6NetherOres (the BlockDiggable meta-3 ruling)
            case 4 -> "yellow_clay";
            case 5 -> "blue_clay";
            case 6 -> "white_clay";
            default -> "";
        };
    }

    /** The meta→block face: 0 rides the vanilla MUD constant, the rest resolve by name (null until the owning registration lands — the null-skip light-up form). */
    public static Block diggable(int aMeta) {
        if (aMeta == 0) return Blocks.MUD;
        String tName = diggableName(aMeta);
        return tName.isEmpty() ? null : byName(tName);
    }

    /** The upstream Sands meta → the gt6 registry path — all three black sands map to the ported {@code black_sand} (the declared stand-in, class javadoc). */
    public static String sandName(int aMeta) {
        return "black_sand";
    }

    /** The river-floor sand quadrant meta (:90) — {@code aCx<0 ? aCz<0 ? 0 : 1 : aCz<0 ? 2 : 0}. */
    public static int sandMeta(int aCx, int aCz) {
        return aCx < 0 ? aCz < 0 ? 0 : 1 : aCz < 0 ? 2 : 0;
    }

    /** The gt6 block by registry path — null when the registration has not landed (the BLOCK registry is defaulted to air, the miss face reads back air → null). */
    static Block byName(String aPath) {
        Block tBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("gt6", aPath));
        return tBlock == Blocks.AIR ? null : tBlock;
    }

    /** The zone payload — the biome overwrite FIRST (the upstream Arrays.fill precedes the block writes), then the per-zone floor column. */
    public static void build(Sink aSink, Env aEnv, RandomSource aRng, int aCx, int aCz, Zone aZone) {
        aEnv.fillBiome(biome(aZone));
        switch (aZone) {
            case RIVER -> river(aSink, aEnv, aCx, aCz);
            case ICE -> ice(aSink, aRng, aCx, aCz);
            case TAIGA -> taiga(aSink, aEnv, aRng, aCx, aCz);
            case FOREST -> forest(aSink, aEnv, aRng, aCx, aCz);
            case PLAINS -> plains(aSink, aEnv, aRng, aCx, aCz);
            case MESA -> mesa(aSink, aCx, aCz);
            case DESERT -> desert(aSink, aCx, aCz);
            case SWAMP -> swamp(aSink, aEnv, aRng, aCx, aCz);
            case JUNGLE -> jungle(aSink, aEnv, aRng, aCx, aCz);
            default -> {} // the *_FILL rows ride the biome overwrite only (:70-81 early returns)
        }
    }

    // ------------------------------------------------------------- the zones

    /** The fake river cross (:82-98): water ×3 over the quadrant sand, gravel, clay ×2, the stone pillar; the spawn face (:97). */
    private static void river(Sink aSink, Env aEnv, int aCx, int aCz) {
        BlockState tSand = state(byName(sandName(sandMeta(aCx, aCz)))); // :90 the quadrant meta draw
        BlockState tWater = Blocks.WATER.defaultBlockState(); // the BlocksGT.River → WATER mapping (BlockRiver extends BlockWaterlike)
        BlockState tClay = Blocks.CLAY.defaultBlockState(); // :92-93 meta 0
        BlockState tStone = Blocks.STONE.defaultBlockState(); // :94 — the 1.7.10 stone meta 1 carries no visual
        BlockState tAir = Blocks.AIR.defaultBlockState();
        for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
            int tX = aCx * 16 + i, tZ = aCz * 16 + j;
            for (int k = -3; k < 64; k++) aSink.set(tX, GT6CenterFeature.HEIGHT + k, tZ, tAir); // :86
            aSink.set(tX, GT6CenterFeature.HEIGHT - 4, tZ, tWater); // :87
            aSink.set(tX, GT6CenterFeature.HEIGHT - 5, tZ, tWater); // :88
            aSink.set(tX, GT6CenterFeature.HEIGHT - 6, tZ, tWater); // :89
            aSink.set(tX, GT6CenterFeature.HEIGHT - 7, tZ, tSand); // :90
            aSink.set(tX, GT6CenterFeature.HEIGHT - 8, tZ, Blocks.GRAVEL.defaultBlockState()); // :91
            aSink.set(tX, GT6CenterFeature.HEIGHT - 9, tZ, tClay); // :92
            aSink.set(tX, GT6CenterFeature.HEIGHT - 10, tZ, tClay); // :93
            for (int k = 1; k < GT6CenterFeature.HEIGHT - 10; k++) aSink.set(tX, k, tZ, tStone); // :94
        }
        aEnv.spawn(0, GT6CenterFeature.HEIGHT + 5, 0); // :97
    }

    /** The ice 2x2 (:102-113): the ice sheet over packed ice, the greenschist/blueschist pillar. */
    private static void ice(Sink aSink, RandomSource aRng, int aCx, int aCz) {
        for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
            int tX = aCx * 16 + i, tZ = aCz * 16 + j;
            clearSky(aSink, tX, tZ, 1); // :105
            aSink.set(tX, GT6CenterFeature.HEIGHT, tZ, Blocks.ICE.defaultBlockState()); // :106
            for (int k = 1; k <= 5; k++) aSink.set(tX, GT6CenterFeature.HEIGHT - k, tZ, Blocks.PACKED_ICE.defaultBlockState()); // :107-111
            for (int k = 1; k < GT6CenterFeature.HEIGHT - 5; k++) { // :112
                boolean tMossy = aRng.nextBoolean();
                aSink.set(tX, k, tZ, stoneLayer(k < 32 ? "greenschist" : "blueschist", tMossy));
            }
        }
    }

    /** The cold taiga fill (:115-136): snow over podzol (the dirt-meta-2 face), the mossy cobble pillar, the 4 spruce corners. */
    private static void taiga(Sink aSink, Env aEnv, RandomSource aRng, int aCx, int aCz) {
        int tX0 = aCx * 16, tZ0 = aCz * 16;
        for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
            int tX = tX0 + i, tZ = tZ0 + j;
            clearSky(aSink, tX, tZ, 2); // :117 — k=2, the H+1 row stays for the snow
            aSink.set(tX, GT6CenterFeature.HEIGHT + 1, tZ, Blocks.SNOW.defaultBlockState()
                    .setValue(SnowLayerBlock.LAYERS, aRng.nextInt(2) + 1)); // :118 the snow_layer meta 0/1 → LAYERS 1/2
            for (int k = 0; k <= 5; k++) aSink.set(tX, GT6CenterFeature.HEIGHT - k, tZ, Blocks.PODZOL.defaultBlockState()); // :119-124 dirt meta 2
            for (int k = 1; k < GT6CenterFeature.HEIGHT - 5; k++) aSink.set(tX, k, tZ, Blocks.MOSSY_COBBLESTONE.defaultBlockState()); // :125
        }
        for (int i : CORNERS) for (int j : CORNERS) { // :128-136
            aRng.nextInt(2); // the NB-meta draw — the argument evaluation kept for stream fidelity
            aSink.set(tX0 + i, GT6CenterFeature.HEIGHT + 1, tZ0 + j, Blocks.AIR.defaultBlockState()); // :128-131
            aEnv.tree("spruce", tX0 + i, GT6CenterFeature.HEIGHT + 1, tZ0 + j); // :133-136 meta 1
        }
    }

    /** The forest 2x2 (:139-159): grass over dirt, the kimberlite/quartzite pillar, the pumpkin stands, the oak/birch corners. */
    private static void forest(Sink aSink, Env aEnv, RandomSource aRng, int aCx, int aCz) {
        int tX0 = aCx * 16, tZ0 = aCz * 16;
        for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
            int tX = tX0 + i, tZ = tZ0 + j;
            clearSky(aSink, tX, tZ, 1); // :142
            aSink.set(tX, GT6CenterFeature.HEIGHT, tZ, Blocks.GRASS_BLOCK.defaultBlockState()); // :143
            for (int k = 1; k <= 5; k++) aSink.set(tX, GT6CenterFeature.HEIGHT - k, tZ, Blocks.DIRT.defaultBlockState()); // :144-148
            for (int k = 1; k < GT6CenterFeature.HEIGHT - 5; k++) { // :149
                boolean tMossy = aRng.nextBoolean();
                aSink.set(tX, k, tZ, stoneLayer(k < 32 ? "kimberlite" : "quartzite", tMossy));
            }
        }
        for (int i : new int[] {6, 10}) for (int j : new int[] {6, 10}) {
            aSink.set(tX0 + i, GT6CenterFeature.HEIGHT + 1, tZ0 + j, Blocks.PUMPKIN.defaultBlockState()); // :151-154
        }
        aRng.nextInt(3); // :156 the 4+nextInt(3) height draw
        aEnv.tree("oak", tX0 + 4, GT6CenterFeature.HEIGHT + 1, tZ0 + 4); // :156 meta 0
        aRng.nextInt(3); // :157
        aEnv.tree("birch", tX0 + 12, GT6CenterFeature.HEIGHT + 1, tZ0 + 4); // :157 meta 2
        aRng.nextInt(3); // :158
        aEnv.tree("birch", tX0 + 4, GT6CenterFeature.HEIGHT + 1, tZ0 + 12); // :158 meta 2
        aRng.nextInt(3); // :159
        aEnv.tree("oak", tX0 + 12, GT6CenterFeature.HEIGHT + 1, tZ0 + 12); // :159 meta 0
    }

    /** The plains fill (:162-190): THE clay ground — the Diggables band 1/3/4/5/6 under grass+dirt, the limestone/marble pillar, the surface-rock/decoration lottery. */
    private static void plains(Sink aSink, Env aEnv, RandomSource aRng, int aCx, int aCz) {
        int tX0 = aCx * 16, tZ0 = aCz * 16;
        for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
            int tX = tX0 + i, tZ = tZ0 + j;
            clearSky(aSink, tX, tZ, 1); // :164
            aSink.set(tX, GT6CenterFeature.HEIGHT, tZ, Blocks.GRASS_BLOCK.defaultBlockState()); // :165
            aSink.set(tX, GT6CenterFeature.HEIGHT - 1, tZ, Blocks.DIRT.defaultBlockState()); // :166
            aSink.set(tX, GT6CenterFeature.HEIGHT - 2, tZ, state(diggable(1))); // :167 BrownClay
            aSink.set(tX, GT6CenterFeature.HEIGHT - 3, tZ, state(diggable(3))); // :168 RedClay
            aSink.set(tX, GT6CenterFeature.HEIGHT - 4, tZ, state(diggable(4))); // :169 Bentonite
            aSink.set(tX, GT6CenterFeature.HEIGHT - 5, tZ, state(diggable(5))); // :170 Palygorskite
            aSink.set(tX, GT6CenterFeature.HEIGHT - 6, tZ, state(diggable(6))); // :171 Kaolinite
            for (int k = 1; k < GT6CenterFeature.HEIGHT - 6; k++) { // :172
                boolean tMossy = aRng.nextBoolean();
                aSink.set(tX, k, tZ, stoneLayer(k < 32 ? "limestone" : "marble", tMossy));
            }
            decoration(aSink, aRng.nextInt(60), tX, tZ); // :173-188
        }
    }

    /** The per-column decoration lottery (:173-188) — the surface-rock trio, the tallgrass, the flower row. */
    private static void decoration(Sink aSink, int aRoll, int aX, int aZ) {
        int tY = GT6CenterFeature.HEIGHT + 1;
        switch (aRoll) {
            case 0, 1, 2 -> aSink.set(aX, tY, aZ, state(byName("surface_rock_flint"))); // :174-175 the 32757+flint NBT row
            case 3, 4, 5 -> aSink.set(aX, tY, aZ, state(byName("surface_rock_stone"))); // :176 the bare 32757 row
            case 6, 7, 8 -> aSink.set(aX, tY, aZ, state(byName("surface_stick"))); // :176 the 32756 row
            case 9, 10, 11, 12, 13, 14 -> aSink.set(aX, tY, aZ, grassPlant()); // :177 tallgrass meta 1
            case 15 -> aSink.set(aX, tY, aZ, Blocks.DANDELION.defaultBlockState()); // :178 yellow_flower
            case 16 -> aSink.set(aX, tY, aZ, Blocks.POPPY.defaultBlockState()); // :179 red_flower 0
            case 17 -> aSink.set(aX, tY, aZ, Blocks.BLUE_ORCHID.defaultBlockState()); // :180
            case 18 -> aSink.set(aX, tY, aZ, Blocks.ALLIUM.defaultBlockState()); // :181
            case 19 -> aSink.set(aX, tY, aZ, Blocks.AZURE_BLUET.defaultBlockState()); // :182 houstonia
            case 20 -> aSink.set(aX, tY, aZ, Blocks.RED_TULIP.defaultBlockState()); // :183
            case 21 -> aSink.set(aX, tY, aZ, Blocks.ORANGE_TULIP.defaultBlockState()); // :184
            case 22 -> aSink.set(aX, tY, aZ, Blocks.WHITE_TULIP.defaultBlockState()); // :185
            case 23 -> aSink.set(aX, tY, aZ, Blocks.PINK_TULIP.defaultBlockState()); // :186
            case 24 -> aSink.set(aX, tY, aZ, Blocks.OXEYE_DAISY.defaultBlockState()); // :187
            default -> {} // :188 — rolls 25..59 leave bare grass
        }
    }

    /** The 1-block grass plant — the 1.20.3 rename fork (Blocks.GRASS here / SHORT_GRASS on 21.1, the GTSenseItem precedent). */
    private static BlockState grassPlant() {
        //? if forge {
        return Blocks.GRASS.defaultBlockState(); // :177 tallgrass meta 1
        //?} else {
        /*return Blocks.SHORT_GRASS.defaultBlockState();
         *///?}
    }

    /** The mesa 2x2 (:194-211): red sand over terracotta (the hardened_clay face), the 4 three-high cacti. */
    private static void mesa(Sink aSink, int aCx, int aCz) {
        int tX0 = aCx * 16, tZ0 = aCz * 16;
        for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
            int tX = tX0 + i, tZ = tZ0 + j;
            clearSky(aSink, tX, tZ, 1); // :197
            for (int k = 0; k <= 5; k++) aSink.set(tX, GT6CenterFeature.HEIGHT - k, tZ, Blocks.RED_SAND.defaultBlockState()); // :198-203 sand meta 1
            for (int k = 1; k < GT6CenterFeature.HEIGHT - 5; k++) aSink.set(tX, k, tZ, Blocks.TERRACOTTA.defaultBlockState()); // :204 hardened_clay meta 0
        }
        for (int i : CORNERS) for (int j : CORNERS) {
            for (int k = 1; k <= 3; k++) aSink.set(tX0 + i, GT6CenterFeature.HEIGHT + k, tZ0 + j, Blocks.CACTUS.defaultBlockState()); // :206-210
        }
    }

    /** The desert fill (:213-224): sand over sandstone. */
    private static void desert(Sink aSink, int aCx, int aCz) {
        for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
            int tX = aCx * 16 + i, tZ = aCz * 16 + j;
            clearSky(aSink, tX, tZ, 1); // :215
            for (int k = 0; k <= 5; k++) aSink.set(tX, GT6CenterFeature.HEIGHT - k, tZ, Blocks.SAND.defaultBlockState()); // :216-221 sand meta 0
            for (int k = 1; k < GT6CenterFeature.HEIGHT - 5; k++) aSink.set(tX, k, tZ, Blocks.SANDSTONE.defaultBlockState()); // :222
        }
    }

    /** The swamp 2x2 (:226-242): water over mud/mud/turf/turf/turf (the Diggables 0/0/2/2/2 band), the granite pillar, the glowtus lottery, the 4 lily pads. */
    private static void swamp(Sink aSink, Env aEnv, RandomSource aRng, int aCx, int aCz) {
        int tX0 = aCx * 16, tZ0 = aCz * 16;
        BlockState tGlowtus = state(GT6CenterFeature.resolve(GT6SurfaceBlocks.GLOWTUS));
        for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
            int tX = tX0 + i, tZ = tZ0 + j;
            clearSky(aSink, tX, tZ, 1); // :229
            aSink.set(tX, GT6CenterFeature.HEIGHT, tZ, Blocks.WATER.defaultBlockState()); // :230
            aSink.set(tX, GT6CenterFeature.HEIGHT - 1, tZ, state(diggable(0))); // :231 mud
            aSink.set(tX, GT6CenterFeature.HEIGHT - 2, tZ, state(diggable(0))); // :232 mud
            aSink.set(tX, GT6CenterFeature.HEIGHT - 3, tZ, state(diggable(2))); // :233 turf
            aSink.set(tX, GT6CenterFeature.HEIGHT - 4, tZ, state(diggable(2))); // :234 turf
            aSink.set(tX, GT6CenterFeature.HEIGHT - 5, tZ, state(diggable(2))); // :235 turf
            for (int k = 1; k < GT6CenterFeature.HEIGHT - 5; k++) { // :236
                boolean tMossy = aRng.nextBoolean();
                aSink.set(tX, k, tZ, stoneLayer(k < 32 ? "granite_red" : "granite_black", tMossy));
            }
            if (aRng.nextInt(8) == 0) { // :237 the glowtus lottery
                aRng.nextInt(16); // the upstream meta draw — the modern block is meta-less, the draw stays for stream fidelity
                aSink.set(tX, GT6CenterFeature.HEIGHT + 1, tZ, tGlowtus);
            }
        }
        for (int i : CORNERS) for (int j : CORNERS) {
            aSink.set(tX0 + i, GT6CenterFeature.HEIGHT + 1, tZ0 + j, Blocks.LILY_PAD.defaultBlockState()); // :239-242 waterlily
        }
    }

    /** The jungle fill (:244-275): grass over coarse dirt (the dirt-meta-1 else branch — the EtFu conditional CUT), the komatiite/basalt pillar, the melon, the 4 big vine trees. */
    private static void jungle(Sink aSink, Env aEnv, RandomSource aRng, int aCx, int aCz) {
        int tX0 = aCx * 16, tZ0 = aCz * 16;
        for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
            int tX = tX0 + i, tZ = tZ0 + j;
            clearSky(aSink, tX, tZ, 1); // :248/:259
            aSink.set(tX, GT6CenterFeature.HEIGHT, tZ, Blocks.GRASS_BLOCK.defaultBlockState()); // :260
            for (int k = 1; k <= 5; k++) aSink.set(tX, GT6CenterFeature.HEIGHT - k, tZ, Blocks.COARSE_DIRT.defaultBlockState()); // :261-265 dirt meta 1
            for (int k = 1; k < GT6CenterFeature.HEIGHT - 5; k++) { // :266
                boolean tMossy = aRng.nextBoolean();
                aSink.set(tX, k, tZ, stoneLayer(k < 32 ? "komatiite" : "basalt", tMossy));
            }
        }
        aSink.set(tX0 + 6 + aRng.nextInt(4), GT6CenterFeature.HEIGHT + 1, tZ0 + 6 + aRng.nextInt(4),
                Blocks.MELON.defaultBlockState()); // :269 — the two draws, argument order left-to-right
        for (int i : CORNERS) for (int j : CORNERS) {
            aRng.nextInt(3); // the 9+nextInt(3) height draw
            aEnv.tree("jungle", tX0 + i, GT6CenterFeature.HEIGHT + 1, tZ0 + j); // :271-274 meta 3, vines T
        }
    }

    // ------------------------------------------------------------- the column helpers

    /** The NB sky-clear (:86/:105/:117/:142/:164/:197/:215/:229/:248) — k = aFromK..63 at HEIGHT+k. */
    private static void clearSky(Sink aSink, int aX, int aZ, int aFromK) {
        BlockState tAir = Blocks.AIR.defaultBlockState();
        for (int k = aFromK; k < 64; k++) aSink.set(aX, GT6CenterFeature.HEIGHT + k, aZ, tAir);
    }

    /** The GT stone layer draw (:112/:149/:172/:236/:266) — the upstream meta {@code nextBoolean()?2:0} → MCOBL/STONE (StoneVariant ordinals = the 1.7.10 metas). */
    private static BlockState stoneLayer(String aSnake, boolean aMossy) {
        return GT6CenterFeature.state(GT6CenterFeature.stone(aSnake, aMossy ? StoneVariant.MCOBL : StoneVariant.STONE));
    }

    private static BlockState state(Block aBlock) {
        return aBlock == null ? null : aBlock.defaultBlockState();
    }

    // ------------------------------------------------------------- the live faces

    /** The chunk-wide biome overwrite — the write-path ruling (class javadoc) riding ChunkAccess#fillBiomesFromNoise on the region's center chunk. */
    public static void liveFillBiome(WorldGenLevel aLevel, ChunkPos aChunk, String aBiomeName) {
        if (!(aLevel instanceof net.minecraft.server.level.WorldGenRegion tRegion)) return; // the region-less face (e.g. /place) skips the fill — declared
        Holder<Biome> tHolder = aLevel.registryAccess().registryOrThrow(Registries.BIOME)
                .getHolderOrThrow(BIOME_KEYS.get(aBiomeName));
        tRegion.getChunk(aChunk.x, aChunk.z)
                .fillBiomesFromNoise((x, y, z, sampler) -> tHolder, Climate.empty()); // the constant resolver = the Arrays.fill semantics; the vanilla no-op sampler (Climate.java:62) is never invoked
    }

    /** The vanilla tree placement — the upstream WorldGenTrees rows via the configured-feature registry; heights ride the feature's own providers (the declared deviation). */
    public static void liveTree(WorldGenLevel aLevel, ChunkGenerator aGenerator, RandomSource aRng,
            String aKind, int aX, int aY, int aZ) {
        ResourceKey<ConfiguredFeature<?, ?>> tKey = TREE_KEYS.get(aKind);
        if (tKey == null) return;
        aLevel.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .getHolderOrThrow(tKey).value().place(aLevel, aGenerator, aRng, new net.minecraft.core.BlockPos(aX, aY, aZ));
    }
}
