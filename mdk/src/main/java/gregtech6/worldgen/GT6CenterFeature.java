package gregtech6.worldgen;

import java.util.List;
import java.util.Random;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.FluidState;

import net.minecraftforge.registries.RegistryObject;

import gregtech6.block.foam.GT6CFoamFreshBlock;
import gregtech6.registry.GT6ConcreteBlocks;
import gregtech6.registry.GT6FoamBlocks;
import gregtech6.registry.GT6Rails;
import gregtech6.registry.GTStoneBlocks;
import gregtech6.registry.GTStoneBlocks.StoneSpec;

/**
 * The world-origin Center showcase (task worldgen-center-nexus) — the {@code
 * worldgen/center/} trio Nexus/Streets/Beacon port ({@code Loader_Worldgen.java:646-650}
 * rows 2-4; CenterBiomes rides the follow-up card, Testing landed with
 * worldgen-center-testing). ONE Feature
 * ({@code gt6:center}, the single-feature shape of LARGE_VEINS/NETHER_QUARTZ) attached
 * to #minecraft:is_overworld at TOP_LAYER_MODIFICATION (runs after every other pass, so
 * the hand-built terrain overwrites whatever generated before — the upstream
 * GENERATING_SPECIAL suppression face, WorldgenCenterBiomes.reset :63-65, becomes
 * plain overwriting).
 *
 * <p><b>Upstream gates → chunk predicates</b> (all verbatim if-translation, aMinX = chunk
 * min block X): Nexus {@code aMinX==16 && aMinZ==-48} (:57) = chunk (1,-3); Streets roads
 * {@code aMinX∈{-16,0}} X-roads / {@code aMinZ∈{-16,0}} Z-roads (:67/:387) with the gap
 * rows {@code aMinZ∉{-32,16}} (:385) / {@code aMinX∉{-32,16}} (:412) and the 4x4-chunk
 * plaza 64x64 (:69); Beacon {@code (aMinX!=−16&&aMinX!=0)||(...)} (:60 — the second
 * clause {@code aMinZ==−16&&aMinZ==0} is dead code, always false) = columns {-1,0} at
 * every Z.
 *
 * <p><b>Payload decomposition (the declared structural deviation)</b>: upstream one chunk
 * writes the whole 64x64 plaza / 24-wide road band cross-chunk via world.setBlock. Modern
 * FEATURES runs in a 3x3-chunk WorldGenRegion whose out-of-window writes are silently
 * dropped (ChunkStatus.java:133 writeRadiusCutoff — the worldgen-flower-arm audit), so
 * every writer is decomposed into per-chunk OWNED COLUMNS: the geometry stays a pure
 * function of absolute coordinates and each chunk writes the columns it owns
 * ({@code floorDiv(x,16)==cx && floorDiv(z,16)==cz}). Grid parameters (widths, lane
 * offsets, rail cadence, heights, materials) are verbatim; the write topology is
 * chunk-decomposed. Geometric equivalence, not line-for-line.
 *
 * <p><b>Config face</b>: upstream per-WorldgenObject config booleans, all default F
 * ({@code Loader_Worldgen.java:646-650}). The port carries no ForgeConfigSpec (the
 * MaterialTreeDisplay.SHOWN precedent) — the five switches are non-final statics assigned
 * by a future config card, default F faithful. HEIGHT = upstream {@code
 * WD.waterLevel()+4} = 66 (water top 62 in BOTH editions — 1.7.10 WD.waterLevel()==62,
 * 1.20.1 sea level 63 with top water block 62 — the number carries over verbatim).
 *
 * <p><b>Block mapping</b> (the 对账 declaration): BlocksGT.Concrete/CFoam/CFoam-slab →
 * gt6:concrete_{dye} / gt6:cfoam / gt6:cfoam_slab (the dye index order is the upstream
 * CS.DYE_INDEX, GTSprayCanItem.DYE_IDS verbatim; the testing shell carries the dye on
 * the 16-step COLOR property, the c-foam-block-family one-block ruling); BlocksGT.Asphalt → minecraft:
 * black_concrete + white_concrete stripes (GT asphalt block not yet ported); BlocksGT.
 * Glass DYE_INDEX_LightBlue → minecraft:light_blue_stained_glass (the testing ceiling's
 * GlowGlass slab rides the same mapping — vanilla has no glass slab, the full block is
 * the declared stand-in); BlocksGT.RailRoad
 * meta 0/8 → rail_road SHAPE NORTH_SOUTH + POWERED false/true and meta 1/9 → EAST_WEST
 * (the PoweredRail SHAPE property cannot carry the upstream meta-8/9 texture cadence —
 * the powered texture is the declared stand-in, the cadence math stays verbatim);
 * NB → air. The beacon effect pre-configuration rides the PUBLIC BeaconBlockEntity
 * .load(CompoundTag) face (BeaconBlockEntity.java:265 reads Primary/Secondary; Levels
 * recomputes from the pyramid on the first tick — vanilla 1.20.1 never NBT-loads it):
 * the upstream four NBT round-trips (:72-77 form, Streets :313-356 the plaza variant)
 * collapse to one Env.beacon call per beacon. The upstream entity-cull call
 * (WorldgenStreets.java:650/:887) is a no-op here: the modern pipeline generates chunks
 * before entities exist.
 */
public class GT6CenterFeature extends Feature<NoneFeatureConfiguration> {

    /** The five WorldgenObject switches, upstream defaults F (:646-650) — the testing shell rides TESTING since worldgen-center-testing; CenterBiomes stays on the follow-up card. */
    public static boolean CENTER_BIOMES = false;
    public static boolean STREETS = false;
    public static boolean NEXUS = false;
    public static boolean BEACON = false;
    public static boolean TESTING = false;

    /** The tower/city ground level ({@code WD.waterLevel()+4} — mHeight, :41). */
    public static int HEIGHT = 66;

    /** The window-column matrix (:58, verbatim 16 entries). */
    public static final boolean[] F_WINDOW = {false, true, true, true, true, false, true, true, true, true, false, true, true, true, true, false};

    /** The far-field road tunnel trigger — upstream counts &gt;128 opaque columns over the 32-wide band (:370); the owned-column decomposition halves the scan width, so the threshold halves with it. */
    public static final int TUNNEL_THRESHOLD = 64;

    /** The fixed Nexus stone-draw seed (upstream RNGSUS is shared global state; a local fixed seed keeps the stones branch deterministic). */
    public static final long NEXUS_STONE_SEED = 6131000569321125127L;

    /** The vanilla effect ids for the beacon NBT rows — the network-frozen protocol constants (upstream Potion.moveSpeed=1/digSpeed=3/damageBoost=5/resistance=11/regeneration=10; identical in both editions, so the GTDrinks Holder leg fork is skipped here). */
    public static final int MOVEMENT_SPEED = 1;
    public static final int DIG_SPEED = 3;
    public static final int DAMAGE_BOOST = 5;
    public static final int DAMAGE_RESISTANCE = 11;
    public static final int REGENERATION = 10;

    /** The upstream NBT effect pair for the beacon at band (i, j) — speed/speed at (-1,-1), haste/haste at (-1,0), strength/strength at (0,-1), resistance/regeneration at (0,0) (Beacon :74-110, Streets :313-356). */
    public static int[] beaconEffects(int aI, int aJ) {
        int tPrimary = aI < 0 ? (aJ < 0 ? MOVEMENT_SPEED : DIG_SPEED) : (aJ < 0 ? DAMAGE_BOOST : DAMAGE_RESISTANCE);
        int tSecondary = (aI < 0 || aJ < 0) ? tPrimary : REGENERATION; // :110 the lone mixed pair
        return new int[] {tPrimary, tSecondary};
    }

    public GT6CenterFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    // ---------------------------------------------------------------- gates

    /** {@code aMinX==16 && aMinZ==-48} (:57) — the Nexus tower chunk. */
    public static boolean isNexusChunk(int aCx, int aCz) {
        return aCx == 1 && aCz == -3;
    }

    /** {@code aMinX==-16 || aMinX==0} (:60) — the Beacon columns (the dead second clause javadoc'd on the class). */
    public static boolean isBeaconColumn(int aCx) {
        return aCx == -1 || aCx == 0;
    }

    /** The 4x4-chunk plaza {-2..1}² — the upstream 64x64 write span of the intersection chunk (:69). */
    public static boolean isPlazaChunk(int aCx, int aCz) {
        return aCx >= -2 && aCx <= 1 && aCz >= -2 && aCz <= 1;
    }

    /** X-road chunks: the aMinX∈{-16,0} column, outside the plaza, skipping the gap rows aMinZ∈{-32,16} (:385). */
    public static boolean roadXApplies(int aCx, int aCz) {
        return !isPlazaChunk(aCx, aCz) && (aCx == -1 || aCx == 0) && aCz != -2 && aCz != 1;
    }

    /** Z-road chunks: the aMinZ∈{-16,0} row, outside the plaza, skipping the gap columns aMinX∈{-32,16} (:412). */
    public static boolean roadZApplies(int aCx, int aCz) {
        return !isPlazaChunk(aCx, aCz) && (aCz == -1 || aCz == 0) && aCx != -2 && aCx != 1;
    }

    /**
     * The far-field road mode decision (:361-384): the 128-column blocked scan → tunnel
     * (through-mountain), an infinite-water biome → bridge+kill-sky, otherwise
     * land+kill-sky. The ring rows (:385) are elevated bridges with side walls unless the
     * (follow-up-card) biome ring clears them.
     */
    public enum RoadMode { LAND, TUNNEL, BRIDGE, RING }

    /** The pure decision mapping behind {@link #farRoadMode} (the offline pin seam). */
    public static RoadMode farRoadDecision(boolean aBlocked, boolean aInfiniteWater) {
        if (aBlocked) return RoadMode.TUNNEL; // :371 generateRoadX(F,F,T,F,F)
        if (aInfiniteWater) return RoadMode.BRIDGE; // :381 generateRoadX(F,T,F,T,T)
        return RoadMode.LAND; // :383 generateRoadX(T,T,F,F,T)
    }

    /** The 4-chunk testing box {@code (aMinX!=32&&aMinX!=48)||(aMinZ!=-32&&aMinZ!=-48)} (:67) — chunk-min block coords 32/48 = cx {2,3}, -48/-32 = cz {-3,-2}. Disjoint from the trio (nexus (1,-3), plaza {-2..1}², road bands {-1,0}). */
    public static boolean isTestingChunk(int aCx, int aCz) {
        return (aCx == 2 || aCx == 3) && (aCz == -3 || aCz == -2);
    }

    /**
     * The world-origin testing shell (task worldgen-center-testing, upstream
     * worldgen/center/WorldgenTesting.java) — the 4-chunk concrete box at the spawn
     * frontier: the solid gray-concrete pedestal k=1..HEIGHT (:70), the sky clear
     * HEIGHT+2..255 (:71, the 1.7.10 world cap carried verbatim), the gray CFoam floor
     * (:73), the CFoam walls on the box rim (:74-88, LightBlue body + Yellow bands at
     * +3/+13 + Gray cap), the slab ceiling (:89-93) and the west-wall doorway (:96-131,
     * the (2,-2) chunk). The upstream setSpawnLocation(0, H+5, 0) (:374) rides the Env.
     *
     * <p><b>SKIPPED_UPSTREAM — the cheat room</b>: everything the shell encloses is not
     * ported (no port identity) — the MTE furniture band (:133-371: 7133 shelves, 4033
     * chests, 32757/26304 pipes, 32057/32737/32727 ... the full filler arm), the
     * ToolsGT.sMetaTool / IL.Tool_Chunk_Remover / IL.Tool_Cheat / IL.IC2_Debug /
     * IL.TC_Thaumonomicon inventory rows (:186-344, the Tool_* IL same-scope ruling) and
     * the vanilla crafting-table/cauldron/anvil/ender-chest props (:153/:177/:183/:366-371).
     * The port ships the shell only: walls, doorway, interior clearing.
     */
    public static void testing(Sink aSink, int aCx, int aCz) {
        int tX0 = aCx * 16, tZ0 = aCz * 16;
        BlockState tAir = Blocks.AIR.defaultBlockState();
        for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
            int tX = tX0 + i, tZ = tZ0 + j;
            for (int k = 1; k <= HEIGHT; k++) aSink.set(tX, k, tZ, state(concrete(8))); // :70 the solid gray-concrete pedestal (DYE_INDEX_Gray)
            for (int k = HEIGHT + 2; k < 256; k++) aSink.set(tX, k, tZ, tAir); // :71 the sky clear — the 1.7.10 world cap verbatim (the modern 320 above stays untouched)
            aSink.set(tX, HEIGHT + 1, tZ, cfoam(8)); // :73 the gray CFoam floor
            boolean tEdge = (i == 0 && aCx == 2) || (i == 15 && aCx == 3) || (j == 0 && aCz == -3) || (j == 15 && aCz == -2); // :74 the box rim
            if (tEdge) {
                aSink.set(tX, HEIGHT + 2, tZ, cfoam(12)); // :75 LightBlue
                aSink.set(tX, HEIGHT + 3, tZ, cfoam(11)); // :76 Yellow
                for (int k = 4; k <= 12; k++) aSink.set(tX, HEIGHT + k, tZ, cfoam(12)); // :77-85
                aSink.set(tX, HEIGHT + 13, tZ, cfoam(11)); // :86 Yellow
                aSink.set(tX, HEIGHT + 14, tZ, cfoam(12)); // :87
                aSink.set(tX, HEIGHT + 15, tZ, cfoam(8)); // :88 the gray cap
            } else if ((i != 1 && i != 5 && i != 10 && i != 14) && (j != 1 && j != 5 && j != 10 && j != 14)) { // :89
                // :90 the GlowGlass slab LightBlue — vanilla has no glass slab, the full stained
                // glass is the declared stand-in (the nexus BlocksGT.Glass mapping precedent)
                aSink.set(tX, HEIGHT + 15, tZ, Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState());
            } else {
                aSink.set(tX, HEIGHT + 15, tZ, cfoamSlab(7)); // :92 the LightGray CFoam slab cross
            }
        }
        if (aCx == 2 && aCz == -2) testingDoorway(aSink, tX0, tZ0); // :96-131 (aMinX==32 && aMinZ==-32)
    }

    /** The west-wall doorway (:96-131), the (2,-2) chunk only: the 4x3 opening at i=0, j=6..9 (H+2..H+4), the Gray/Yellow CFoam jambs + H+5 lintel and the interior pilasters at i=1. */
    private static void testingDoorway(Sink aSink, int aX0, int aZ0) {
        BlockState tAir = Blocks.AIR.defaultBlockState();
        aSink.set(aX0, HEIGHT + 2, aZ0 + 5, cfoam(8)); // :97
        for (int j = 6; j <= 9; j++) aSink.set(aX0, HEIGHT + 2, aZ0 + j, tAir); // :98-101
        aSink.set(aX0, HEIGHT + 2, aZ0 + 10, cfoam(8)); // :102
        aSink.set(aX0 + 1, HEIGHT + 2, aZ0 + 6, cfoam(8)); // :103
        aSink.set(aX0 + 1, HEIGHT + 2, aZ0 + 9, cfoam(8)); // :104

        aSink.set(aX0, HEIGHT + 3, aZ0 + 5, cfoam(11)); // :106
        for (int j = 6; j <= 9; j++) aSink.set(aX0, HEIGHT + 3, aZ0 + j, tAir); // :107-110
        aSink.set(aX0, HEIGHT + 3, aZ0 + 10, cfoam(11)); // :111
        aSink.set(aX0 + 1, HEIGHT + 3, aZ0 + 6, cfoam(11)); // :112
        aSink.set(aX0 + 1, HEIGHT + 3, aZ0 + 9, cfoam(11)); // :113

        aSink.set(aX0, HEIGHT + 4, aZ0 + 5, cfoam(8)); // :115
        for (int j = 6; j <= 9; j++) {
            aSink.set(aX0, HEIGHT + 4, aZ0 + j, tAir); // :116-119
            aSink.set(aX0 + 1, HEIGHT + 4, aZ0 + j, cfoam(8)); // :120-123 the full pilaster run
        }
        aSink.set(aX0, HEIGHT + 4, aZ0 + 10, cfoam(8)); // :124

        for (int j = 5; j <= 10; j++) aSink.set(aX0, HEIGHT + 5, aZ0 + j, cfoam(8)); // :126-131 the gray lintel
    }

    // ------------------------------------------------------------ the place dispatch

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> aContext) {
        WorldGenLevel tLevel = aContext.level();
        ChunkPos tChunk = tLevel instanceof net.minecraft.server.level.WorldGenRegion tRegion
                ? tRegion.getCenter() : new ChunkPos(aContext.origin());
        return dispatch(liveSink(tLevel), liveEnv(tLevel), tChunk.x, tChunk.z,
                (aCx, aCz, aAxisX) -> farRoadMode(tLevel, aCx, aCz, aAxisX));
    }

    /** The far-field scanner seam — the live impl reads the region (the blocked/water scans), tests inject their own. */
    public interface FarScanner {
        RoadMode scan(int aCx, int aCz, boolean aAxisX);
    }

    /**
     * The chunk dispatch (the test seam): order = the upstream registration order
     * (:646-650 biomes→streets→nexus→beacon→testing; the chunk sets are disjoint across
     * the trio, so order carries no geometry).
     */
    public static boolean dispatch(Sink aSink, Env aEnv, int aCx, int aCz, FarScanner aFar) {
        boolean rPlaced = false;
        if (NEXUS && isNexusChunk(aCx, aCz)) {
            nexus(aSink, aCx, aCz);
            aEnv.spawn(0, HEIGHT + 5, 0); // :336
            rPlaced = true;
        }
        if (BEACON && !STREETS && isBeaconColumn(aCx)) {
            beacon(aSink, aEnv, aCx, aCz);
            aEnv.spawn(0, HEIGHT + 5, 0); // :114
            rPlaced = true;
        }
        if (STREETS) {
            if (isPlazaChunk(aCx, aCz)) {
                GT6CenterStreets.plaza(aSink, aEnv, aCx, aCz, CENTER_BIOMES, NEXUS, TESTING, BEACON);
                aEnv.spawn(0, HEIGHT + 5, 0); // :358
                rPlaced = true;
            } else if (roadXApplies(aCx, aCz)) {
                RoadMode tMode = aCz < -6 || aCz > 5 ? aFar.scan(aCx, aCz, true) : RoadMode.RING;
                GT6CenterStreets.road(aSink, aEnv, GT6CenterStreets.Axis.X, aCz, aCx, tMode, !CENTER_BIOMES);
                rPlaced = true; // :385
            } else if (roadZApplies(aCx, aCz)) {
                RoadMode tMode = aCx < -6 || aCx > 5 ? aFar.scan(aCx, aCz, false) : RoadMode.RING;
                GT6CenterStreets.road(aSink, aEnv, GT6CenterStreets.Axis.Z, aCx, aCz, tMode, !CENTER_BIOMES);
                rPlaced = true; // :412
            }
        }
        if (TESTING) {
            if (isTestingChunk(aCx, aCz)) {
                testing(aSink, aCx, aCz);
                aEnv.spawn(0, HEIGHT + 5, 0); // :374 every box chunk re-asserts the spawn
                rPlaced = true;
            }
        }
        return rPlaced;
    }

    /** The far-field mode with the level reads (the blocked scan :367-374 + the water-biome scan :375-382). */
    private static RoadMode farRoadMode(WorldGenLevel aLevel, int aCx, int aCz, boolean aAxisX) {
        int tOpaque = 0;
        for (int tA = 0; tA < 16; tA++) for (int tB = 0; tB < 16; tB++) {
            int tX = aAxisX ? aCx * 16 + tB : aCx * 16 + tA; // scan along the road band width
            int tZ = aAxisX ? aCz * 16 + tA : aCz * 16 + tB;
            BlockState tState = aLevel.getBlockState(new BlockPos(tX, HEIGHT + 9, tZ));
            FluidState tFluid = tState.getFluidState();
            if (!tFluid.isEmpty() || (tState.canOcclude() && !tState.is(BlockTags.LOGS) && !tState.is(BlockTags.LEAVES))) {
                if (++tOpaque > TUNNEL_THRESHOLD) return RoadMode.TUNNEL; // :370-372
            }
        }
        boolean tWater = false;
        for (int tA = 0; tA < 16 && !tWater; tA++) for (int tB = 0; tB < 16; tB++) {
            int tX = aCx * 16 + tB, tZ = aCz * 16 + tA;
            if (aLevel.getBiome(new BlockPos(tX, 64, tZ)).is(BiomeTags.IS_OCEAN)
                    || aLevel.getBiome(new BlockPos(tX, 64, tZ)).is(BiomeTags.IS_RIVER)) {
                tWater = true; // :380 BIOMES_INFINITE_WATER → the ocean/river tag mapping
                break;
            }
        }
        return farRoadDecision(false, tWater);
    }

    // ---------------------------------------------------------------- deps

    /** The level-reading/writing effects, the seam that keeps the geometry bodies offline-testable. */
    public interface Env {
        /** Upstream WD.opq face (the canOcclude mapping — the water-replace scan precedent). */
        boolean opq(int aX, int aY, int aZ);
        /** The biome display name at the sample point (the upstream biomeName). */
        String biomeName(int aX, int aZ);
        /** The BIOMES_INFINITE_WATER face → the ocean/river tag pair. */
        boolean infiniteWater(int aX, int aZ);
        /** The WD.sign face — a standing sign with the rotation and text lines. */
        void sign(int aX, int aY, int aZ, int aRotation, String... aLines);
        /** The four beacon NBT rows (:69-111 / Streets :313-356) — Primary/Secondary effect ids on the BE at the block. */
        void beacon(int aX, int aY, int aZ, int aPrimary, int aSecondary);
        /** The upstream setSpawnLocation face. */
        void spawn(int aX, int aY, int aZ);
    }

    /** The block-write sink — records every ATTEMPT (null state = the un-resolvable offline GT block), the live impl skips nulls. */
    public interface Sink {
        void set(int aX, int aY, int aZ, @Nullable BlockState aState);
    }

    static Env liveEnv(WorldGenLevel aLevel) {
        return new Env() {
            @Override public boolean opq(int aX, int aY, int aZ) {
                return aLevel.getBlockState(new BlockPos(aX, aY, aZ)).canOcclude();
            }
            @Override public String biomeName(int aX, int aZ) {
                return aLevel.getBiome(new BlockPos(aX, 64, aZ)).unwrapKey()
                        .map(tKey -> tKey.location().getPath()).orElse("unknown");
            }
            @Override public boolean infiniteWater(int aX, int aZ) {
                var tBiome = aLevel.getBiome(new BlockPos(aX, 64, aZ));
                return tBiome.is(BiomeTags.IS_OCEAN) || tBiome.is(BiomeTags.IS_RIVER);
            }
            @Override public void sign(int aX, int aY, int aZ, int aRotation, String... aLines) {
                BlockPos tPos = new BlockPos(aX, aY, aZ);
                aLevel.setBlock(tPos, Blocks.OAK_SIGN.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.StandingSignBlock.ROTATION, aRotation & 15), 2);
                if (aLevel.getBlockEntity(tPos) instanceof SignBlockEntity tSign) {
                    net.minecraft.network.chat.Component[] tText = new net.minecraft.network.chat.Component[4];
                    for (int i = 0; i < 4; i++) tText[i] = net.minecraft.network.chat.Component.literal(i < aLines.length ? aLines[i] : "");
                    tSign.setText(new SignText(tText, tText, net.minecraft.world.item.DyeColor.BLACK, false), true);
                }
            }
            @Override public void spawn(int aX, int aY, int aZ) {
                Level tLevel = aLevel instanceof net.minecraft.server.level.WorldGenRegion tRegion
                        ? tRegion.getLevel() : (aLevel instanceof Level tL ? tL : null);
                if (tLevel instanceof ServerLevel tServer) tServer.setDefaultSpawnPos(new BlockPos(aX, aY, aZ), 0.0F);
            }
            @Override public void beacon(int aX, int aY, int aZ, int aPrimary, int aSecondary) {
                BlockPos tPos = new BlockPos(aX, aY, aZ);
                if (aLevel.getBlockEntity(tPos) instanceof net.minecraft.world.level.block.entity.BeaconBlockEntity tBeacon) {
                    // the upstream NBT round-trip (:72-77) collapses to the public load face —
                    // Primary/Secondary only; Levels recomputes from the pyramid on the first
                    // tick (vanilla never NBT-loads it, both editions).
                    net.minecraft.nbt.CompoundTag tNBT = new net.minecraft.nbt.CompoundTag();
                    tNBT.putInt("Primary", aPrimary);
                    tNBT.putInt("Secondary", aSecondary);
                    //? if forge {
                    tBeacon.load(tNBT); // 1.20.1: the public override (BeaconBlockEntity.java:265)
                    //?} else {
                    /*tBeacon.loadWithComponents(tNBT, aLevel.registryAccess()); // 1.21.1: load went protected
                     *///?}
                }
            }
        };
    }

    static Sink liveSink(WorldGenLevel aLevel) {
        return (aX, aY, aZ, aState) -> {
            if (aState == null) return;
            aLevel.setBlock(new BlockPos(aX, aY, aZ), aState, 2);
        };
    }

    // ---------------------------------------------------------------- blocks (the mapping face)

    /** gt6:concrete_{dye} — the CS.DYE_INDEX order (black 0 .. white 15). */
    public static Block concrete(int aDyeIndex) {
        List<RegistryObject<Block>> tList = GT6ConcreteBlocks.FULL_BLOCKS;
        if (aDyeIndex < 0 || aDyeIndex >= tList.size()) return null;
        return resolve(tList.get(aDyeIndex));
    }

    /** gt6:concrete_{dye}_slab. */
    public static Block concreteSlab(int aDyeIndex) {
        List<RegistryObject<Block>> tList = GT6ConcreteBlocks.SLAB_BLOCKS;
        if (aDyeIndex < 0 || aDyeIndex >= tList.size()) return null;
        return resolve(tList.get(aDyeIndex));
    }

    public static Block cfoam() {
        return resolve(GT6FoamBlocks.CFOAM);
    }

    public static Block cfoamSlab() {
        return resolve(GT6FoamBlocks.CFOAM_SLAB);
    }

    /** gt6:cfoam with the upstream dye — the 16-step COLOR property (GT6CFoamFreshBlock.COLOR, the GTSprayCanItem.DYE_IDS order; gray 8, light_gray 7, yellow 11, light_blue 12). */
    public static BlockState cfoam(int aDyeIndex) {
        Block tBlock = cfoam();
        return tBlock == null ? null : tBlock.defaultBlockState().setValue(GT6CFoamFreshBlock.COLOR, aDyeIndex & 15);
    }

    /** gt6:cfoam_slab with the upstream dye. */
    public static BlockState cfoamSlab(int aDyeIndex) {
        Block tBlock = cfoamSlab();
        return tBlock == null ? null : tBlock.defaultBlockState().setValue(GT6CFoamFreshBlock.COLOR, aDyeIndex & 15);
    }

    /** The RailRoad block (gt6:rail_road, the GT6Rails ROAD_BLOCK row). */
    public static Block roadRail() {
        return resolve(GT6Rails.ROAD_BLOCK);
    }

    public static Block stone(String aSnake, gregtech6.block.stone.StoneVariant aVariant) {
        return resolve(GTStoneBlocks.block(aSnake, aVariant));
    }

    /** The 17-stone list snake list (the upstream BlocksGT.stones walk order — GTStoneBlocks.STONES). */
    public static String stoneSnake(int aIndex) {
        List<StoneSpec> tStones = GTStoneBlocks.STONES;
        return tStones.get(aIndex % tStones.size()).snake();
    }

    public static int stoneCount() {
        return GTStoneBlocks.STONES.size();
    }

    /** The one registry-dependent face — a bound handle resolves, anything else is the offline null (the position-only pin). */
    static Block resolve(RegistryObject<Block> aRo) {
        if (aRo == null) return null;
        //? if forge {
        return aRo.isPresent() ? aRo.get() : null;
        //?} else {
        /*return aRo.isBound() ? aRo.get() : null;
         *///?}
    }

    // ---------------------------------------------------------------- Nexus (:56-338)

    /**
     * The Nexus tower — segment-for-segment faithful (the user-named body). One chunk,
     * self-contained, no level reads. The STREETS branch builds the concrete/foam/glass
     * form; the else branch is the random-stone ruin form (RNGSUS → the fixed-seed local
     * Random, the per-block draws preserved in sequence).
     */
    public static void nexus(Sink aSink, int aCx, int aCz) {
        int tX0 = aCx * 16, tZ0 = aCz * 16;
        boolean tStreets = STREETS;
        Random tRng = tStreets ? null : new Random(NEXUS_STONE_SEED);
        String tStone = tStreets ? null : stoneSnake(tRng.nextInt(stoneCount()));
        for (int i = 0; i < 16; i++) for (int j = 0; j < 16; j++) {
            int tX = tX0 + i, tZ = tZ0 + j;
            for (int k = 2; k < 64; k++) aSink.set(tX, HEIGHT + k, tZ, Blocks.AIR.defaultBlockState()); // :61/:92
            for (int k = 1; k < HEIGHT; k++) aSink.set(tX, k, tZ, tStreets ? state(concrete(8)) : state(stone(tStone, gregtech6.block.stone.StoneVariant.STONE))); // :62/:93
            aSink.set(tX, HEIGHT + 1, tZ, tStreets ? state(cfoam()) : state(stone(tStone, gregtech6.block.stone.StoneVariant.TILES))); // :63/:94
            if (i == 0 || i == 15 || j == 0 || j == 15) {
                aSink.set(tX, HEIGHT + 0, tZ, tStreets ? state(concrete(8)) : stoneState(tRng, tStone)); // :65/:96
                aSink.set(tX, HEIGHT + 2, tZ, tStreets ? state(concrete(7)) : stoneState(tRng, tStone)); // :66/:97
                aSink.set(tX, HEIGHT + 3, tZ, tStreets ? state(concrete(7)) : stoneState(tRng, tStone)); // :67
                aSink.set(tX, HEIGHT + 4, tZ, tStreets ? state(concrete(7)) : stoneState(tRng, tStone)); // :68
                aSink.set(tX, HEIGHT + 5, tZ, tStreets ? state(concrete(7)) : stoneState(tRng, tStone)); // :69
                aSink.set(tX, HEIGHT + 6, tZ, tStreets ? state(concrete(7)) : stoneState(tRng, tStone)); // :70
                aSink.set(tX, HEIGHT + 7, tZ, tStreets ? state(cfoam()) : state(stone(tStone, gregtech6.block.stone.StoneVariant.CHISL))); // :71/:102
                aSink.set(tX, HEIGHT + 8, tZ, tStreets ? state(concrete(7)) : stoneState(tRng, tStone)); // :72
                if (F_WINDOW[i] || F_WINDOW[j]) { // :73/:104
                    for (int k = 9; k <= 11; k++) aSink.set(tX, HEIGHT + k, tZ, tStreets
                            ? Blocks.LIGHT_BLUE_STAINED_GLASS.defaultBlockState()
                            : Blocks.GLASS_PANE.defaultBlockState()); // :74-76/:105-107
                } else {
                    for (int k = 9; k <= 11; k++) aSink.set(tX, HEIGHT + k, tZ, tStreets ? state(concrete(7)) : stoneState(tRng, tStone)); // :78-80/:109-111
                }
                aSink.set(tX, HEIGHT + 12, tZ, tStreets ? state(concrete(7)) : stoneState(tRng, tStone)); // :82/:113
            } else {
                aSink.set(tX, HEIGHT + 0, tZ, Blocks.OBSIDIAN.defaultBlockState()); // :84/:115
            }
            aSink.set(tX, HEIGHT + 13, tZ, tStreets ? state(cfoam()) : state(stone(tStone, gregtech6.block.stone.StoneVariant.STILE))); // :86/:117
        }
        nexusDoorway(aSink, tX0, tZ0, tStreets); // :127-163 (the south gate, j=15/14 cutouts)
        nexusPortalRing(aSink, tX0, tZ0); // :182-206 (the end-portal frame circle)
        nexusGarden(aSink, tX0, tZ0, tStreets); // :210-246 (the black-lamp garden, west lawn)
        nexusLampRing(aSink, tX0, tZ0, tStreets); // :319-334 (the east lawn lamp ring)
        nexusShed(aSink, tX0, tZ0); // :250-315 (the smooth-sandstone shed)
    }

    /** The per-draw rim stone draw (:96 3+RNGSUS.nextInt(3) → BRICK/CRACK/MBRIK). */
    private static BlockState stoneState(@Nullable Random aRng, String aStone) {
        if (aRng == null) return null;
        gregtech6.block.stone.StoneVariant tVariant = switch (aRng.nextInt(3)) {
            case 0 -> gregtech6.block.stone.StoneVariant.BRICK;
            case 1 -> gregtech6.block.stone.StoneVariant.CRACK;
            default -> gregtech6.block.stone.StoneVariant.MBRIK;
        };
        return state(stone(aStone, tVariant));
    }

    /** The j=15/14 south gate cutouts (:127-163): the CFoam posts + NB doors off the streets branch, terracotta off it. */
    private static void nexusDoorway(Sink aSink, int tX0, int tZ0, boolean aStreets) {
        Block tPost = aStreets ? cfoam() : Blocks.GRAY_TERRACOTTA;
        BlockState tPostS = tPost == null ? null : tPost.defaultBlockState();
        BlockState tAir = Blocks.AIR.defaultBlockState();
        for (int k = 2; k <= 4; k++) {
            aSink.set(tX0 + 5, HEIGHT + k, tZ0 + 15, tPostS); aSink.set(tX0 + 10, HEIGHT + k, tZ0 + 15, tPostS);
            for (int i = 6; i <= 9; i++) aSink.set(tX0 + i, HEIGHT + k, tZ0 + 15, tAir);
            aSink.set(tX0 + 6, HEIGHT + k, tZ0 + 14, tPostS); aSink.set(tX0 + 9, HEIGHT + k, tZ0 + 14, tPostS);
        }
        for (int i = 5; i <= 10; i++) aSink.set(tX0 + i, HEIGHT + 5, tZ0 + 15, tPostS); // :145-150
        for (int i = 7; i <= 8; i++) aSink.set(tX0 + i, HEIGHT + 4, tZ0 + 14, tPostS); // :161-162
        // the obsidian doorway (:165-178) — the H+1 sill runs the full 4-wide frame (:165-168)
        BlockState tObs = Blocks.OBSIDIAN.defaultBlockState();
        BlockState tGlow = Blocks.GLOWSTONE.defaultBlockState();
        for (int i = 6; i <= 9; i++) aSink.set(tX0 + i, HEIGHT + 1, tZ0 + 1, tObs);
        for (int k = 2; k <= 4; k++) {
            aSink.set(tX0 + 6, HEIGHT + k, tZ0 + 1, tObs); aSink.set(tX0 + 9, HEIGHT + k, tZ0 + 1, tObs);
        }
        aSink.set(tX0 + 6, HEIGHT + 5, tZ0 + 1, tGlow); aSink.set(tX0 + 9, HEIGHT + 5, tZ0 + 1, tGlow); // :175/:178
        aSink.set(tX0 + 7, HEIGHT + 5, tZ0 + 1, tObs); aSink.set(tX0 + 8, HEIGHT + 5, tZ0 + 1, tObs);
    }

    /** The end-portal frame circle (:182-206): 12 frames around the 3x3 NB interior at (2..4, 2..4), corners glowstone. */
    private static void nexusPortalRing(Sink aSink, int tX0, int tZ0) {
        BlockState tGlow = Blocks.GLOWSTONE.defaultBlockState();
        BlockState tAir = Blocks.AIR.defaultBlockState();
        aSink.set(tX0 + 1, HEIGHT + 1, tZ0 + 1, tGlow); aSink.set(tX0 + 5, HEIGHT + 1, tZ0 + 1, tGlow);
        aSink.set(tX0 + 1, HEIGHT + 1, tZ0 + 5, tGlow); aSink.set(tX0 + 5, HEIGHT + 1, tZ0 + 5, tGlow);
        for (int i = 2; i <= 4; i++) {
            aSink.set(tX0 + i, HEIGHT + 1, tZ0 + 1, frame(Direction.SOUTH)); // meta 0 = +z facing (1.7.10 legacy meta order)
            aSink.set(tX0 + i, HEIGHT + 1, tZ0 + 5, frame(Direction.NORTH)); // meta 2
            aSink.set(tX0 + 1, HEIGHT + 1, tZ0 + i, frame(Direction.EAST)); // meta 3
            aSink.set(tX0 + 5, HEIGHT + 1, tZ0 + i, frame(Direction.WEST)); // meta 1
        }
        for (int i = 2; i <= 4; i++) for (int j = 2; j <= 4; j++) aSink.set(tX0 + i, HEIGHT + 1, tZ0 + j, tAir); // :188-200
    }

    /** The 1.7.10 end-portal-frame meta → the vanilla FACING mapping (legacy order 0=S,1=W,2=N,3=E). */
    public static BlockState frame(Direction aFacing) {
        return Blocks.END_PORTAL_FRAME.defaultBlockState().setValue(EndPortalFrameBlock.FACING, aFacing);
    }

    /** The garden (:210-246): the black-lamp west edge, the grass lawn, the 2x2 water pool at (12..13, 2..3), the flower perimeter at H+2. */
    private static void nexusGarden(Sink aSink, int tX0, int tZ0, boolean aStreets) {
        BlockState tGlow = Blocks.GLOWSTONE.defaultBlockState();
        Block tLamp = aStreets ? cfoam() : Blocks.BLACK_TERRACOTTA;
        BlockState tLampS = tLamp == null ? null : tLamp.defaultBlockState();
        BlockState tGrass = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState tWater = Blocks.WATER.defaultBlockState();
        BlockState tFlower = Blocks.DANDELION.defaultBlockState();
        // the H+1 bed — column 10 = glow/lamp edge (:210-214), 11/14 = lawns (:215-218/:230-233), 12/13 = pools (:220-229)
        for (int j = 1; j <= 5; j++) {
            aSink.set(tX0 + 10, HEIGHT + 1, tZ0 + j, j == 1 || j == 5 ? tGlow : tLampS);
            aSink.set(tX0 + 14, HEIGHT + 1, tZ0 + j, j == 1 || j == 5 ? tGlow : tLampS);
            aSink.set(tX0 + 11, HEIGHT + 1, tZ0 + j, j == 5 ? tLampS : tGrass);
            aSink.set(tX0 + 12, HEIGHT + 1, tZ0 + j, j == 5 ? tLampS : (j == 2 || j == 3) ? tWater : tGrass);
            aSink.set(tX0 + 13, HEIGHT + 1, tZ0 + j, j == 5 ? tLampS : (j == 2 || j == 3) ? tWater : tGrass);
        }
        for (int i = 11; i <= 14; i++) for (int j = 1; j <= 4; j++) {
            if ((i == 12 || i == 13) && (j == 2 || j == 3)) continue; // the pool has no flowers (:221-222/:226-227)
            aSink.set(tX0 + i, HEIGHT + 2, tZ0 + j, tFlower); // :235-246 — the 12-flower perimeter
        }
    }

    /** The east lawn lamp ring (:319-334): four glowstones + black-lamp edges around (10..14, 10..14). */
    private static void nexusLampRing(Sink aSink, int tX0, int tZ0, boolean aStreets) {
        BlockState tGlow = Blocks.GLOWSTONE.defaultBlockState();
        Block tLamp = aStreets ? cfoam() : Blocks.BLACK_TERRACOTTA;
        BlockState tLampS = tLamp == null ? null : tLamp.defaultBlockState();
        aSink.set(tX0 + 10, HEIGHT + 1, tZ0 + 10, tGlow); aSink.set(tX0 + 10, HEIGHT + 1, tZ0 + 14, tGlow);
        aSink.set(tX0 + 14, HEIGHT + 1, tZ0 + 10, tGlow); aSink.set(tX0 + 14, HEIGHT + 1, tZ0 + 14, tGlow); // :319/:323/:327/:331
        for (int i = 11; i <= 13; i++) {
            aSink.set(tX0 + 10, HEIGHT + 1, tZ0 + i, tLampS); aSink.set(tX0 + 14, HEIGHT + 1, tZ0 + i, tLampS); // :320-322/:328-330
            aSink.set(tX0 + i, HEIGHT + 1, tZ0 + 10, tLampS); aSink.set(tX0 + i, HEIGHT + 1, tZ0 + 14, tLampS); // :324-326/:332-334
        }
    }

    /** The smooth-sandstone shed (:250-315): solid base, hollow interior, corner posts smooth→chiseled→glowstone. */
    private static void nexusShed(Sink aSink, int tX0, int tZ0) {
        BlockState tSmooth = Blocks.SMOOTH_SANDSTONE.defaultBlockState(); // meta 2
        BlockState tChiseled = Blocks.CHISELED_SANDSTONE.defaultBlockState(); // meta 1
        BlockState tGlow = Blocks.GLOWSTONE.defaultBlockState();
        BlockState tAir = Blocks.AIR.defaultBlockState();
        for (int i = 1; i <= 5; i++) for (int j = 10; j <= 14; j++) {
            aSink.set(tX0 + i, HEIGHT + 0, tZ0 + j, tSmooth); // :250-274
            boolean tEdge = i == 1 || i == 5 || j == 10 || j == 14;
            aSink.set(tX0 + i, HEIGHT + 1, tZ0 + j, tEdge ? tSmooth : tAir); // :276-300
        }
        for (int i : new int[] {1, 5}) for (int j : new int[] {10, 14}) {
            aSink.set(tX0 + i, HEIGHT + 2, tZ0 + j, tSmooth); // :302-305
            aSink.set(tX0 + i, HEIGHT + 3, tZ0 + j, tChiseled); // :307-310
            aSink.set(tX0 + i, HEIGHT + 4, tZ0 + j, tGlow); // :312-315
        }
    }

    // ---------------------------------------------------------------- Beacon (:58-116)

    /**
     * The beacon pyramid at the world origin (:62-111), decomposed per owned column: the
     * 10/8/6/4-wide iron levels ride the two beacon columns {-1,0} at every Z (the
     * upstream dead-code gate javadoc'd on the class). The four beacons carry the upstream
     * NBT effect pairs via the Env face — speed/speed at (-1,-1), haste/haste at (-1,0),
     * strength/strength at (0,-1), resistance/regeneration at (0,0) (:74-110).
     */
    public static void beacon(Sink aSink, Env aEnv, int aCx, int aCz) {
        for (int i = -5; i < 5; i++) for (int j = -5; j < 5; j++) put(aSink, aCx, aCz, i, HEIGHT + 1, j, Blocks.IRON_BLOCK); // :62
        for (int i = -4; i < 4; i++) for (int j = -4; j < 4; j++) put(aSink, aCx, aCz, i, HEIGHT + 2, j, Blocks.IRON_BLOCK); // :63
        for (int i = -3; i < 3; i++) for (int j = -3; j < 3; j++) put(aSink, aCx, aCz, i, HEIGHT + 3, j, Blocks.IRON_BLOCK); // :64
        for (int i = -2; i < 2; i++) for (int j = -2; j < 2; j++) put(aSink, aCx, aCz, i, HEIGHT + 4, j, Blocks.IRON_BLOCK); // :65
        for (int i : new int[] {-1, 0}) for (int j : new int[] {-1, 0}) {
            if (aCx == Math.floorDiv(i, 16) && aCz == Math.floorDiv(j, 16)) {
                aSink.set(i, HEIGHT + 5, j, Blocks.BEACON.defaultBlockState()); // :69/:80/:91/:102
                int[] tEffects = beaconEffects(i, j);
                aEnv.beacon(i, HEIGHT + 5, j, tEffects[0], tEffects[1]);
            }
        }
    }

    /** The ownership-guarded column write (the decomposition seam). */
    static void put(Sink aSink, int aCx, int aCz, int aX, int aY, int aZ, Block aBlock) {
        if (aCx == Math.floorDiv(aX, 16) && aCz == Math.floorDiv(aZ, 16)) {
            aSink.set(aX, aY, aZ, aBlock == null ? null : aBlock.defaultBlockState());
        }
    }

    static BlockState state(@Nullable Block aBlock) {
        return aBlock == null ? null : aBlock.defaultBlockState();
    }

    /** The standing-sign rotation for a cardinal face (ROTATION 0=S, 4=W, 8=N, 12=E — the WD.sign side mapping). */
    public static int signRotation(Direction aFacing) {
        return switch (aFacing) {
            case SOUTH -> 0;
            case WEST -> 4;
            case NORTH -> 8;
            case EAST -> 12;
            default -> 0;
        };
    }
}
