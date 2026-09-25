package gregtech6.worldgen.dungeon;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

//? if forge {
import com.mojang.serialization.Codec;
//?} else {
/*import com.mojang.serialization.MapCodec;
//21.1: simpleCodec returns MapCodec (vanilla-mc 1.21.1 BuriedTreasureStructure.java:16).
*///?}

import gregtech6.registry.GT6Structures;
import gregtech6.registry.GTStoneBlocks;

/**
 * The GT6 shelter dungeon STRUCTURE (task p38-dungeon-framework) — the modern
 * {@code Structure} port of upstream {@code gregapi.worldgen.dungeon.WorldgenDungeonGT}
 * (tmp/gt6-1.7.10, 379 lines): the whole N×N grid layout is rolled ONCE at
 * {@link #findGenerationPoint} time and dispatched into per-chunk pieces (the vanilla
 * WoodlandMansionStructure.java:30-42 grid-then-pieces shape); the 1.7.10 cross-chunk
 * setBlock walking dies here because each piece's {@code BoundingBox} clips its
 * {@code postProcess} per chunk natively.
 *
 * <p>Generation conditions, upstream :141-144 mapped:
 * <ul>
 * <li>the 11-chunk grid ({@code |chunk| % (maxSize+4) == 5}) rides the structure_set
 *     {@code random_spread} placement (spacing 11 — the datapack JSON, GT6WorldgenDatagen
 *     bootstrap); the fixed mid-cell offset 5 becomes the salt-uniform offset in
 *     {@code [0, spacing-separation]} (RandomSpreadStructurePlacement.getPotentialStructureChunk
 *     — a documented distribution-equivalent deviation, the p31 decision-level precedent);</li>
 * <li>the 1/100 probability roll runs here on the chunk-seeded random;</li>
 * <li>the spawn exclusion {@code |chunk*16| < 256+maxSize*16} = {@code |chunk| < 16+maxSize}
 *     chunks runs here;</li>
 * <li>the y0-bedrock floor check is DROPPED: its 1.7.10 semantics ("the bedrock floor is
 *     not cave-carved at the chunk center") have no queryable counterpart before chunk
 *     generation, and the 1.18+ bedrock floor at world bottom is structurally intact —
 *     a documented deviation;</li>
 * <li>biomes live in the structure JSON ({@code #minecraft:is_overworld}) and are applied
 *     by {@code findValidGenerationPoint} around the Y20 stub — the upstream overworld-only
 *     dimension gate.</li>
 * </ul>
 *
 * <p>Room dispatch (upstream :258-294): dead-end ROOM cells draw the DEAD_END pool —
 * this card ships ONLY the storage vault ({@code DungeonChunkRoomStorage}, which IS the
 * upstream {@code DungeonChunkRoomVault} shell + the piston-door + the loot chest); the
 * five portal rooms are the mod-专属 never pool (unported by ruling). Non-dead-end ROOM
 * cells draw the ROOMS pool — empty in this card (Workshop/MiningBedrock/Library/Farm are
 * the MTE-gated batch cards), so every such room takes the upstream
 * {@code ROOM_EMPTY} fallback face. The BARRACKS important room (:163 lines) is likewise
 * deferred and takes the empty-room face for now. Corridor3/4 variants (:200/:120) defer
 * with the corridor base covering every connection count.
 *
 * <p>KJS face: the structure/structure_set JSONs are datapack-native; the codec +
 * StructureType/PieceType registration rows are the registry face (declared out of KJS
 * scope, the GT6Features javadoc clause).
 */
public class GT6DungeonStructure extends Structure {

    //? if forge {
    public static final Codec<GT6DungeonStructure> CODEC = simpleCodec(GT6DungeonStructure::new);
    //?} else {
    /*public static final MapCodec<GT6DungeonStructure> CODEC = simpleCodec(GT6DungeonStructure::new);
    //21.1: simpleCodec returns MapCodec (vanilla-mc 1.21.1 BuriedTreasureStructure.java:16 form).
    *///?}

    public GT6DungeonStructure(Structure.StructureSettings aSettings) {
        super(aSettings);
    }

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext aContext) {
        ChunkPos tChunk = aContext.chunkPos();
        // :142 — the spawn-radius exclusion, in chunks (16 + maxSize).
        if (Math.abs(tChunk.x) < GT6DungeonLayout.SPAWN_EXCLUDE_CHUNKS
                || Math.abs(tChunk.z) < GT6DungeonLayout.SPAWN_EXCLUDE_CHUNKS) {
            return Optional.empty();
        }
        // :141 — the 1/100 probability roll on the chunk-seeded random.
        if (aContext.random().nextInt(GT6DungeonLayout.PROBABILITY) != 0) {
            return Optional.empty();
        }
        BlockPos tPosition = new BlockPos(tChunk.getMiddleBlockX(), GT6DungeonLayout.DUNGEON_Y, tChunk.getMiddleBlockZ());
        return Optional.of(new Structure.GenerationStub(tPosition, tBuilder -> generatePieces(tBuilder, aContext)));
    }

    /** The one-shot grid roll and piece dispatch (upstream :150-294). */
    private void generatePieces(StructurePiecesBuilder aBuilder, Structure.GenerationContext aContext) {
        RandomSource tRandom = aContext.random();
        byte[][] tLayout = GT6DungeonLayout.generate(tRandom);
        int tSide = tLayout.length;

        // :152-154 — two random shell stones; :150 — the dungeon accent color.
        String tPrimary = GTStoneBlocks.STONES.get(tRandom.nextInt(GTStoneBlocks.STONES.size())).snake();
        String tSecondary = GTStoneBlocks.STONES.get(tRandom.nextInt(GTStoneBlocks.STONES.size())).snake();
        int tColor = tRandom.nextInt(16);

        // :175-176 — the candidate chunk carries the grid center.
        int tBaseX = (aContext.chunkPos().x - tSide / 2) * 16;
        int tBaseZ = (aContext.chunkPos().z - tSide / 2) * 16;

        // the per-dungeon key roll (task dungeon-keys; WorldgenDungeonGT.java:169-173):
        // five ids, the first = 1 + max(draw, unique tag), the rest DESCENDING. The
        // upstream unique tag was System.nanoTime() (:170 — the anti-collision face);
        // the port tags the dungeon's ORIGIN CHUNK (ChunkPos.asLong — unique per dungeon
        // by construction, so cross-dungeon collisions stay impossible) and keeps the
        // draw seed-deterministic. The roll rides a DERIVED random seeded from that
        // chunk tag — ZERO draws off the structure stream, so the layout/stones/color
        // draws above stay bit-identical (the p38-dungeon-framework scan pins keep
        // their meaning; the p31 decision-level determinism precedent).
        long[] tKeyIds = keyIds(aContext.chunkPos());

        // the per-dead-end hide draws (task dungeon-keys; the Workshop :119-123 face):
        // ONE derived stream consumed SEQUENTIALLY across the dungeon's dead-ends, so
        // each dead-end takes its own draw — a per-piece reseed here would make every
        // storage room of one dungeon draw the SAME index (all five locks racing for
        // one findable key). The upstream next(keys*2) is dual-purpose (:119-120): the
        // value first gates at 50% (>= keys → hide nothing, ported as -1) and a passing
        // value IS the key index (naturally < keys — the tKeyIndex < keys clamp).
        int tDeadEnds = 0;
        for (int i = 1; i < tSide - 1; i++) for (int j = 1; j < tSide - 1; j++) {
            if (tLayout[i][j] == GT6DungeonLayout.ROOM_ID && GT6DungeonLayout.connectionCount(tLayout, i, j) == 1) tDeadEnds++;
        }
        int[] tHideDraws = hideDraws(aContext.chunkPos(), tDeadEnds);
        int tHideCursor = 0;

        for (int i = 1; i < tSide - 1; i++) for (int j = 1; j < tSide - 1; j++) {
            byte tCell = tLayout[i][j];
            if (tCell == 0) continue;

            int tX = tBaseX + i * 16, tZ = tBaseZ + j * 16;
            byte tDoors = GT6DungeonPiece.doorsOf(tLayout, i, j);
            switch (tCell) {
                case GT6DungeonLayout.ENTRANCE -> aBuilder.addPiece(new GT6DungeonPiece(
                        GT6DungeonPiece.Kind.ENTRANCE, box(tX, tZ, entranceTop(aContext, tX, tZ) + 2),
                        tDoors, tPrimary, tSecondary, tColor, 0));
                case GT6DungeonLayout.BARRACKS ->
                    // the barracks room defers (the 163-line bed/crafting interior, MTE-gated);
                    // the cell takes the upstream ROOM_EMPTY fallback face for now.
                    aBuilder.addPiece(new GT6DungeonPiece(
                            GT6DungeonPiece.Kind.ROOM_EMPTY, box(tX, tZ, GT6DungeonLayout.DUNGEON_Y + 8),
                            tDoors, tPrimary, tSecondary, tColor, 0));
                case GT6DungeonLayout.CORRIDOR ->
                    // Corridor3/4 defer; the corridor base covers every connection count.
                    aBuilder.addPiece(new GT6DungeonPiece(
                            GT6DungeonPiece.Kind.CORRIDOR, box(tX, tZ, GT6DungeonLayout.DUNGEON_Y + 8),
                            tDoors, tPrimary, tSecondary, tColor, 0));
                case GT6DungeonLayout.ROOM_ID -> {
                    if (GT6DungeonLayout.connectionCount(tLayout, i, j) == 1) {
                        // the DEAD_END pool draw — this card's pool is exactly the storage
                        // vault (Vault shell + piston doors + loot chests); the five portal
                        // rooms are the mod-专属 never pool (unported by ruling). This
                        // dead-end takes its OWN draw off the shared hide table (the
                        // Workshop :119-120 gate+index face); the keys spread across the
                        // dungeon's storage dead-ends until the rooms-batch card moves the
                        // hiding.
                        aBuilder.addPiece(new GT6DungeonPiece(
                                GT6DungeonPiece.Kind.STORAGE, box(tX, tZ, GT6DungeonLayout.DUNGEON_Y + 8),
                                tDoors, tPrimary, tSecondary, tColor, 0, tKeyIds,
                                tHideDraws[tHideCursor++]));
                    } else {
                        // the ROOMS pool draw — the pool ships empty this card, so the
                        // upstream ROOM_EMPTY fallback face takes the room.
                        aBuilder.addPiece(new GT6DungeonPiece(
                                GT6DungeonPiece.Kind.ROOM_EMPTY, box(tX, tZ, GT6DungeonLayout.DUNGEON_Y + 8),
                                tDoors, tPrimary, tSecondary, tColor, 0));
                    }
                }
                default -> {
                }
            }
        }
    }

    /** The piece box: shell y0..7 at DUNGEON_Y (20..27) + the y8 lamp band (28) + the pillar foundation down to y2 — or, for the entrance, up to the aligned surface cap (+2 for the top ring). */
    private static BoundingBox box(int aX, int aZ, int aTopY) {
        return new BoundingBox(aX, GT6DungeonLayout.DUNGEON_Y - 18, aZ, aX + 15, aTopY, aZ + 15);
    }

    /**
     * The per-dungeon key roll (task dungeon-keys; WorldgenDungeonGT.java:169-173):
     * {@link gregtech6.items.GT6Keys#KEYS_PER_DUNGEON} ids, the first =
     * {@code 1 + max(draw, origin-chunk tag)}, the rest DESCENDING (upstream
     * {@code tKeyIDs[i] = tKeyIDs[i-1] - 1}, :171). The derived random (seeded from the
     * chunk tag itself) keeps the structure stream untouched and the roll deterministic
     * per dungeon position — the offline audit face.
     */
    public static long[] keyIds(ChunkPos aOrigin) {
        RandomSource tKeyRandom = RandomSource.create(aOrigin.toLong() * 0x9E3779B97F4A7C15L);
        long[] rIds = new long[gregtech6.items.GT6Keys.KEYS_PER_DUNGEON];
        rIds[0] = 1 + Math.max(tKeyRandom.nextInt(1000000), aOrigin.toLong());
        for (int i = 1; i < rIds.length; i++) rIds[i] = rIds[i - 1] - 1;
        return rIds;
    }

    /**
     * The per-dead-end hide draws (task dungeon-keys; the Workshop :119-123 face,
     * {@code tKeyIndex = next(keys * 2); if (tKeyIndex < keys) hide(key[tKeyIndex])}):
     * ONE derived stream consumed sequentially — the k-th dead-end of the dungeon gets
     * draw k, so a multi-dead-end dungeon spreads its hiding instead of every room
     * redrawing the same value. The upstream next(keys*2) is dual-purpose (:119-120):
     * the value first gates the hide at 50% (upstream: not taken; port: {@code -1} =
     * hide nothing) and a PASSING value IS the key index (naturally {@code < keys},
     * the {@code tKeyStacks[tKeyIndex]} clamp — never an index into the five-id array
     * unchecked). Pure function of the origin chunk (the offline audit face).
     */
    public static int[] hideDraws(ChunkPos aOrigin, int aDeadEndCount) {
        RandomSource tHideRandom = RandomSource.create(aOrigin.toLong() * 0x9E3779B97F4A7C15L + 0x6B65795FL);
        int[] rDraws = new int[aDeadEndCount];
        for (int i = 0; i < aDeadEndCount; i++) {
            int tKeyIndex = tHideRandom.nextInt(gregtech6.items.GT6Keys.KEYS_PER_DUNGEON * 2);
            rDraws[i] = tKeyIndex < gregtech6.items.GT6Keys.KEYS_PER_DUNGEON ? tKeyIndex : -1;
        }
        return rDraws;
    }

    /**
     * The entrance shaft cap (upstream DungeonChunkEntrance.java:112-121, re-based):
     * the 1.7.10 upward 10×10 air-scan becomes the max WORLD_SURFACE_WG height + 1 over
     * the same x/z 3..12 window (the noise heightmap answers before chunk gen), then the
     * upstream 5-alignment ({@code (tHeight-1) % 5 != 0 → += 5 - (tHeight-1) % 5}).
     */
    static int entranceTop(Structure.GenerationContext aContext, int aX, int aZ) {
        int tTop = 0;
        for (int tX = aX + 3; tX <= aX + 12; tX++) for (int tZ = aZ + 3; tZ <= aZ + 12; tZ++) {
            tTop = Math.max(tTop, aContext.chunkGenerator().getFirstOccupiedHeight(
                    tX, tZ, Heightmap.Types.WORLD_SURFACE_WG, aContext.heightAccessor(), aContext.randomState()) + 1);
        }
        if ((tTop - 1) % 5 != 0) tTop += 5 - ((tTop - 1) % 5);
        return Math.min(tTop, aContext.heightAccessor().getMaxBuildHeight() - 3);
    }

    @Override
    public StructureType<?> type() {
        return GT6Structures.DUNGEON_STRUCTURE_TYPE.get();
    }
}
